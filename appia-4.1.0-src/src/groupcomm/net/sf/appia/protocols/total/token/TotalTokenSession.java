/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2006 University of Lisbon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 *
 * Initial developer(s): Alexandre Pinto and Hugo Miranda.
 * Contributor(s): See Appia web page for a list of contributors.
 */

package net.sf.appia.protocols.total.token;

import java.util.LinkedList;
import java.util.ListIterator;

import net.sf.appia.core.AppiaError;
import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.EventQualifier;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.core.message.Message;
import net.sf.appia.protocols.group.LocalState;
import net.sf.appia.protocols.group.ViewState;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.events.Send;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.sync.BlockOk;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;

import java.util.logging.Logger; import java.util.logging.Level;


/**
 * Implementation of a token based total order protocol. 
 * @author Nuno Carvalho
 *
 */
public class TotalTokenSession extends Session implements InitializableSession {

	private static Logger log = Logger.getLogger(TotalTokenSession.class.getName());

	private static final int DEFAULT_NUM_MESSAGES_PER_TOKEN = 10;
    private static final long DEFAULT_SILENT_PERIOD = 500; // miliseconds
	
	private long globalSeqNumber;
	private LinkedList<GroupSendableEvent> pendingMessages, undeliveredMessages;
	private int rankWidthToken, numMessagesPerToken;
	
	private LocalState localState;
	private ViewState viewState;
	private boolean isBlocked;
	
    private boolean sentExplicitToken = false;
    private long silentPeriod = DEFAULT_SILENT_PERIOD;
	
	public TotalTokenSession(Layer layer) {
		super(layer);
		
		pendingMessages = new LinkedList<GroupSendableEvent>();
		undeliveredMessages = new LinkedList<GroupSendableEvent>();
		rankWidthToken = 0;
		numMessagesPerToken = DEFAULT_NUM_MESSAGES_PER_TOKEN;
		isBlocked = true;
	}

      /**
       * Initializes the session using the parameters given in the XML configuration.
       * Possible parameters:
       * <ul>
       * <li><b>num_messages_per_token</b> number of messages (maximum) sent before releasing the token.
       * Default is 10.
       * <li><b>silent_token_period</b> amount of time (miliseconds) to delay the token when there are no 
       * messages being exchanged by the group members. The default is 500ms.
       * </ul>
       * 
       * @param params The parameters given in the XML configuration.
       * @see net.sf.appia.xml.interfaces.InitializableSession#init(SessionProperties)
       */
	public void init(SessionProperties params) {
		if(params.containsKey("num_messages_per_token"))
			numMessagesPerToken = params.getInt("num_messages_per_token");
		if(numMessagesPerToken <= 0)
			numMessagesPerToken = DEFAULT_NUM_MESSAGES_PER_TOKEN;
        if(params.containsKey("silent_token_period"))
            silentPeriod = params.getLong("silent_token_period");
        if(silentPeriod <= 0)
            silentPeriod = DEFAULT_SILENT_PERIOD;
	}

	public void handle(Event event){
		if(event instanceof GroupSendableEvent)
			handleGroupSendable((GroupSendableEvent) event);
        else if (event instanceof TokenTimer)
            handleTokenTimer((TokenTimer)event);
		else if (event instanceof BlockOk)
			handleBlock((BlockOk)event);
		else if (event instanceof View)
			handleView((View)event);
		else if (event instanceof ChannelInit)
			handleChannelInit((ChannelInit)event);
		else if (event instanceof ChannelClose)
			handleChannelClose((ChannelClose)event);
		else
			try {
				event.go();
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
	}

	private void handleTokenTimer(TokenTimer timer) {
        if(iHaveToken() && ! isBlocked)
            sendMessages(timer.getChannel());
    }

    private void handleChannelClose(ChannelClose close) {
		isBlocked = true;
		try {
			close.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}

	private void handleChannelInit(ChannelInit init) {
		try {
			init.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}

	private void handleView(View view) {
		localState = view.ls;
		viewState = view.vs;
		rankWidthToken = 0;
		globalSeqNumber = 0;
		isBlocked = false;
		
		if(Level.FINEST.equals(log.getLevel()))
			log.log(Level.FINEST, "Received new view with "+viewState.addresses.length+" members");
		
		try {
			view.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
		if(iHaveToken())
			sendMessages(view.getChannel());
	}

	private void handleBlock(BlockOk ok) {
		isBlocked = true;
		try {
			ok.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}

	private void handleGroupSendable(GroupSendableEvent event) {
		// do not ensure total ordering of point to point events
		if(event instanceof Send){
			try {
				event.go();
			} catch (AppiaEventException e1) {
				e1.printStackTrace();
			}
			return;
		}

		if(Level.FINEST.equals(log.getLevel()))
			log.log(Level.FINEST, "My rank = "+localState.my_rank+" hasToken = "+rankWidthToken);
		// event from aplication
		if(event.getDir() == Direction.DOWN){
			if(Level.FINEST.equals(log.getLevel()))
				log.log(Level.FINEST, "Received Group Sendable from Appl "+event);
			pendingMessages.addLast(event);
			if(iHaveToken() && !isBlocked){
				sendMessages(event.getChannel());
			}
		}
		// event from network
		else {
			final long seq = event.getMessage().popLong();
			
			if(seq <= globalSeqNumber){
				throw new AppiaError("Received message with seq = "+seq+" was expecting seq = "+(globalSeqNumber+1));
			}
			
			if(seq > (globalSeqNumber + 1)){
				storeUndelivered(event,seq);
				if(Level.FINEST.equals(log.getLevel()))
					log.log(Level.FINEST, "Message out of order. Storing message with seq = "+seq);
				return;
			}
			
			final boolean hasToken = event.getMessage().popBoolean();
			if(Level.FINEST.equals(log.getLevel()))
				log.log(Level.FINEST, "Received Group Sendable from the network with seq = "+seq+" token = "+hasToken);
			
			if (!(event instanceof TokenEvent)) {
				try {
					event.go();
				} catch (AppiaEventException e) {
					e.printStackTrace();
				}
                if(localState.my_rank == 0)
                    sentExplicitToken = false;
			}
			
			if(Level.FINEST.equals(log.getLevel()))
				log.log(Level.FINEST, "Delivering message with seq = "+seq);
			
			// at this point, this is the same as: globalSeqNumber = globalSeqNumber +1
			globalSeqNumber = seq;
			
			if(hasToken)
				rotateToken();
			
			while(!undeliveredMessages.isEmpty()){
				final GroupSendableEvent auxEvent = undeliveredMessages.getFirst();
				final long seqaux = auxEvent.getMessage().peekLong();
				if(seqaux == (globalSeqNumber + 1)){
					undeliveredMessages.removeFirst();
					auxEvent.getMessage().popLong();
					final boolean auxHasToken = auxEvent.getMessage().popBoolean();
					if(!(auxEvent instanceof TokenEvent)){
						try {
							auxEvent.go();
						} catch (AppiaEventException e) {
							e.printStackTrace();
						}
						if(Level.FINEST.equals(log.getLevel()))
							log.log(Level.FINEST, "Delivering stored message with seq = "+seqaux);
					} else if(Level.FINEST.equals(log.getLevel()))
						log.log(Level.FINEST, "Ignored token event with seq = "+seqaux);

					globalSeqNumber = seqaux;
					if(auxHasToken)
						rotateToken();
				}
				else
					break;
			}
			
			if(iHaveToken() && ! isBlocked)
                if(sentExplicitToken && localState.my_rank == 0 && event instanceof TokenEvent
                        && pendingMessages.isEmpty())
                    insertTokenDelay(event.getChannel());
                else
                    sendMessages(event.getChannel());
		}
	}
	
	/*
	 * Support methods
	 */
	
    private void insertTokenDelay(Channel channel){
        if(Level.FINEST.equals(log.getLevel()))
            log.log(Level.FINEST, "##### Inserting delay on the token.");

        try {
            new TokenTimer(this.silentPeriod,channel,Direction.DOWN,this,EventQualifier.ON).go();
            sentExplicitToken = false;
        } catch (AppiaEventException e) {
            if(Level.FINEST.equals(log.getLevel())){
                log.log(Level.FINEST, "Exception when sending the TokenTimer: "+e);
                e.printStackTrace();
            }
        } catch (AppiaException e) {
            if(Level.FINEST.equals(log.getLevel())){
                log.log(Level.FINEST, "Exception when sending the TokenTimer: "+e);
                e.printStackTrace();
            }
        }
    }
    
	private boolean iHaveToken(){
		return (rankWidthToken == localState.my_rank);
	}
	
	private void rotateToken(){
		if(viewState.addresses.length > 1)
            rankWidthToken = ((rankWidthToken+1) == viewState.addresses.length)? 0 : rankWidthToken+1;
	}

	private void sendMessages(Channel channel) {
		if(Level.FINEST.equals(log.getLevel()))
			log.log(Level.FINEST, "I'll try to send some messages");

		final int listSize = pendingMessages.size();
		if(listSize == 0){
		    // only rotates the token when there is more then one member in the group
		    if(viewState.view.length > 1){
		        if(Level.FINEST.equals(log.getLevel()))
		            log.log(Level.FINEST, "I do not have any messages. Rotanting token. My rank is "+localState.my_rank);
		        try {
		            final TokenEvent token = new TokenEvent(channel,Direction.DOWN,this,viewState.group,viewState.id);
		            token.getMessage().pushBoolean(true);
		            token.getMessage().pushLong(++globalSeqNumber);
		            if(localState.my_rank == 0)
		                sentExplicitToken = true;
		            token.go();
		            rotateToken();
		        } catch (AppiaEventException e) {
		            e.printStackTrace();
		        }
		    }
		    return;
		}
        
        if(localState.my_rank == 0)
            sentExplicitToken = false;
		boolean sendToken = false;
        
		for(int i=0; !sendToken; i++){
            // the list size variable should not be updated because this "if" needs always the initial value
            // this only sends the token if it is the last message or
            // if the max messages per token were reached with more then one member on the group
            // With only one member, the messages are sent until the end of the buffer is reached.
			if((i+1) == listSize || (viewState.view.length > 1 && (i+1) ==  numMessagesPerToken))
				sendToken = true;
			final GroupSendableEvent ev = pendingMessages.removeFirst();
			ev.orig = localState.my_rank;
			try {
				// Deliver my message
				final GroupSendableEvent clone = (GroupSendableEvent) ev.cloneEvent();
				clone.setDir(Direction.invert(ev.getDir()));
				clone.setSourceSession(this);
				clone.init();
				clone.go();
			} catch (CloneNotSupportedException e1) {
				e1.printStackTrace();
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
			
			if(Level.FINEST.equals(log.getLevel()))
				log.log(Level.FINEST, "Sending message #"+(globalSeqNumber+1)+" with token = "+sendToken);

            // send the message to the group.
			final Message m = ev.getMessage();			
			m.pushBoolean(sendToken);
			m.pushLong(++globalSeqNumber);
			try {
				ev.go();
				if(sendToken)
					rotateToken();
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}			
		}
	}
	
	  private void storeUndelivered(GroupSendableEvent ev, long seq) {
		  ev.getMessage().pushLong(seq);
		    final ListIterator<GroupSendableEvent> aux=undeliveredMessages.listIterator();
		    while (aux.hasPrevious()) {
		      final SendableEvent evaux=(SendableEvent)aux.previous();
		      final long seqaux= evaux.getMessage().peekLong();
		      if (seqaux == seq) {
		        //debug("Received undelivered message already stored. Discarding new copy.");
		        return;
		      }
		      if (seqaux < seq) {
		        aux.next();
		        aux.add(ev);
		        return;
		      }
		    }
		    undeliveredMessages.addFirst(ev);
		  }


}
