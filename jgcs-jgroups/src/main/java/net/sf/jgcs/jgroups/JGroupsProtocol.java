
/*
 *
 * JGroups implementation of JGCS - Group Communication Service
 * Copyright (C) 2006 Nuno Carvalho, Universidade de Lisboa
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 * 
 * Contact
 * 	Address:
 * 		LASIGE, Departamento de Informatica, Bloco C6
 * 		Faculdade de Ciencias, Universidade de Lisboa
 * 		Campo Grande, 1749-016 Lisboa
 * 		Portugal
 * 	Email:
 * 		jgcs@lasige.di.fc.ul.pt
 * 
 */
 
package net.sf.jgcs.jgroups;


import java.util.HashMap;
import java.util.logging.Logger;

import net.sf.jgcs.AbstractMultiThreadedPollingProtocol;
import net.sf.jgcs.ClosedSessionException;
import net.sf.jgcs.ControlSession;
import net.sf.jgcs.DataSession;
import net.sf.jgcs.GroupConfiguration;
import net.sf.jgcs.JGCSException;

import org.jgroups.Address;
import org.jgroups.BlockEvent;
import org.jgroups.ChannelClosedException;
import org.jgroups.ChannelException;
import org.jgroups.ChannelNotConnectedException;
import org.jgroups.JChannel;
import org.jgroups.SuspectEvent;
import org.jgroups.TimeoutException;
import org.jgroups.View;
import org.jgroups.stack.IpAddress;

public class JGroupsProtocol extends AbstractMultiThreadedPollingProtocol {
	
	//TODO:private static Logger logger = Logger.getLogger(JGroupsProtocol.class);

	private HashMap<JChannel, JGroupsGroup> groups = new HashMap<JChannel, JGroupsGroup>(); 
	
	public JGroupsProtocol() {
		super();
	}

	public DataSession openDataSession(GroupConfiguration group) throws JGCSException {
		DataSession data=lookupDataSession(group);
		if (data==null) {
			createSessions(group);
			data = lookupDataSession(group);
		}
		return data;
	}

	public ControlSession openControlSession(GroupConfiguration group)
			throws JGCSException {
		ControlSession control = lookupControlSession(group);
		if (control==null){
			createSessions(group);
			control = lookupControlSession(group);
		}
		return control;
	}
	
	private synchronized void createSessions(GroupConfiguration g) throws JGCSException{
		JGroupsGroup group = null;
		if( g instanceof JGroupsGroup)
			group = (JGroupsGroup) g;
		else
			throw new JGCSException("Wrong type of the given Group: "+group.getClass().getName()+
					"should be of type "+JGroupsGroup.class.getName());		
			try {
				JChannel channel = new JChannel(group.getConfigFile());
				groups.put(channel, group);
				JGroupsControlSession cs = new JGroupsControlSession(channel, this);
				JGroupsDataSession ds=null;
				try {
					ds = new JGroupsDataSession(this, channel, group);
				} catch (JGCSException e) {
					throw new JGCSException("Could not create JGroups data session.",e);
				}
				putSessions(group, cs,ds);
				createReader(channel, group);
				
			} catch (ChannelException e) {
				throw new JGCSException("Could not create JGroups channel. "+e);
			}
	}
	
	protected synchronized void removeSessions(GroupConfiguration group) {
		super.removeSessions(group);
	}
	
	protected void connectChannel(JChannel c) throws ClosedSessionException, JGCSException {
		if(!c.isOpen())
			throw new ClosedSessionException("JGroups channel is closed");
		try {
			c.connect(groups.get(c).getGroupName());
			startReader(createReader(c, groups.get(c)));
		} catch (ChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected void disconnectChannel(JChannel channel) {
		channel.disconnect();
	}
	
	
	private ProtocolReader<JChannel> createReader(JChannel channel, JGroupsGroup group) {
		ProtocolReader<JChannel> reader = new ProtocolReader<JChannel>() {
			@Override
			public boolean read() {
				Object msg=null;
				Exception exception = null;
				try {
					msg = getChannel().receive(0);
					/*TODO: if(logger.isDebugEnabled())
						logger.debug(("received message: "+msg));*/
				} catch (ChannelNotConnectedException e) {
					return false;
					//exception = e;
				} catch (ChannelClosedException e) {
					return false;
					//exception = e;
				} catch (TimeoutException e) {
					exception = e;
				}
				if(exception != null){
					JGroupsDataSession data = (JGroupsDataSession) lookupDataSession(getGroup());
					data.notifyExceptionListeners(new JGCSException("Could not deliver message.",exception));
					return true;
				}
				else if(msg == null)
					return true;
				
				if(msg instanceof View){
					JGroupsControlSession control = (JGroupsControlSession) lookupControlSession(getGroup());
					control.jgroupsViewAccepted((View) msg);
				}
				else if(msg instanceof SuspectEvent){
					JGroupsControlSession control = (JGroupsControlSession) lookupControlSession(getGroup());
					control.jgroupsSuspect((Address) ((SuspectEvent)msg).getMember());
				}
				else if(msg instanceof BlockEvent){
					JGroupsControlSession control = (JGroupsControlSession) lookupControlSession(getGroup());
					control.jgroupsBlock();
				}
				else if(msg instanceof org.jgroups.Message){
					JGroupsDataSession data = (JGroupsDataSession) lookupDataSession(getGroup());
					JGroupsMessage message = new JGroupsMessage();
					byte[] jgroupsBuffer = ((org.jgroups.Message) msg).getBuffer();
					if(jgroupsBuffer == null)
						return true;
					byte[] buffer = new byte[jgroupsBuffer.length];
					// FIXME? This makes the gap on the DOA results.
					System.arraycopy(jgroupsBuffer,0,buffer,0,buffer.length);
					message.setPayload(buffer);
					message.setSenderAddress((IpAddress) ((org.jgroups.Message)msg).getSrc());
					Object cookie = data.notifyMessageListeners(message);
					if(cookie != null){
						data.notifyServiceListeners(cookie,new JGroupsService("seto_total_order"));
						data.notifyServiceListeners(cookie,new JGroupsService("regular_total_order"));
						data.notifyServiceListeners(cookie,new JGroupsService("uniform_total_order"));
					}
				}
				else{
					JGroupsDataSession data = (JGroupsDataSession) lookupDataSession(getGroup());
					data.notifyExceptionListeners(new JGCSException("Received unknown message type: "+msg));
					//TODO:logger.debug("received unknown object of type "+msg.getClass().getName()+" on reader.");
				}
				return true;
			}
		};
		reader.setFields(group,channel);
		return reader;
	}

}


