
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

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import net.sf.jgcs.ClosedSessionException;
import net.sf.jgcs.JGCSException;
import net.sf.jgcs.NotJoinedException;
import net.sf.jgcs.membership.AbstractBlockSession;

import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelException;
import org.jgroups.ChannelListener;
import org.jgroups.JChannel;
import org.jgroups.View;
import org.jgroups.stack.IpAddress;

public class JGroupsControlSession extends AbstractBlockSession implements ChannelListener {

	//TODO:private static Logger logger = Logger.getLogger(JGroupsControlSession.class);

	private JChannel channel;
	private boolean isChannelBlocked;
	private JGroupsProtocol protocol;
	private volatile boolean shunned;

	private Object membership_lock = new Object();

	public JGroupsControlSession(JChannel ch, JGroupsProtocol protocol) {
		super();
		this.channel = ch;
		this.protocol = protocol;
		this.channel.addChannelListener(this);
		this.channel.setOpt(JChannel.AUTO_RECONNECT, false);
	}

	public void blockOk() throws JGCSException{
		channel.blockOk();		
	}

	public boolean isBlocked() {
		return isChannelBlocked;
	}

	public void join() throws ClosedSessionException, JGCSException {
		protocol.connectChannel(channel);
		synchronized (membership_lock) {
			while (!isJoined()) {
				try {
					membership_lock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void leave() throws ClosedSessionException, JGCSException {
		protocol.disconnectChannel(channel);
		setMembership(null);
	}

	public SocketAddress getLocalAddress() {
		if (channel == null)
			return null;
		IpAddress myjgaddr = (IpAddress) channel.getLocalAddress();
		return new InetSocketAddress(myjgaddr.getIpAddress(),myjgaddr.getPort());
	}

	// listeners of JGroups
	
	public void jgroupsViewAccepted(View new_view) {
	    //JGroups BugWorkAround: Do Not Remove This String Representation Of The View 
		new_view.toString();
		isChannelBlocked = false;
		JGroupsMembership currentMembership = null;
		try {
			currentMembership = (JGroupsMembership) getMembership();
		} catch (NotJoinedException e) {
			// this means that is the first view
		}
		JGroupsMembership incomingMembership = null;
		if(currentMembership == null){
			// if this is the first view
			incomingMembership = new JGroupsMembership(new_view,channel);
			incomingMembership.setJoined(incomingMembership.getMembershipList());
			notifyAndSetMembership(incomingMembership);
		}
		else{
			incomingMembership = new JGroupsMembership(new_view,channel);
			List<SocketAddress> newMembersList = newMembers(currentMembership,incomingMembership);
			List<SocketAddress> oldMembersList = oldMembers(currentMembership,incomingMembership);
			

			// get the members marked as suspect that are not in the current view
			List<SocketAddress> failed = new ArrayList<SocketAddress>();
			for (SocketAddress s : currentMembership.getFailedToNextView()) {
				if (incomingMembership.getMemberRank(s) == -1) {
					failed.add(s);
				}
			}
			
			incomingMembership.setFailed(failed);
			incomingMembership.setLeaved(oldMembersList);
			incomingMembership.setJoined(newMembersList);
			
			//TODO:
			/*if(logger.isDebugEnabled())
				logger.debug("Old: "+oldMembersList+" -> new: "+newMembersList);*/
			
			// notify membership listener
			notifyAndSetMembership(incomingMembership);
			
			// notify control listener
			for (SocketAddress peer : newMembersList) {
				notifyJoin(peer);
			}
			for (SocketAddress peer : incomingMembership.getLeavedMembers()) {
				notifyLeave(peer);
			}
			for (SocketAddress peer : incomingMembership.getFailedMembers()) {
				notifyFailed(peer);
			}
		}
		
		synchronized (membership_lock) {
			membership_lock.notifyAll();	
		}
		
		
		/*TODO
		if(logger.isDebugEnabled())
			logger.debug("View Accepted! currentMembership: "+currentMembership);
			*/
	}

	private List<SocketAddress> 
	newMembers(JGroupsMembership oldMembership, JGroupsMembership newMembership){
		
		List<SocketAddress> newMembersList = new ArrayList<SocketAddress>();
		for(SocketAddress peer : newMembership.getMembershipList())
			if(!oldMembership.getMembershipList().contains(peer))
				newMembersList.add(peer);
		return newMembersList;
	}
	
	private List<SocketAddress> 
	oldMembers(JGroupsMembership oldMembership, JGroupsMembership newMembership){
		
		List<SocketAddress> oldMembersList = new ArrayList<SocketAddress>();
		for(SocketAddress peer : oldMembership.getMembershipList())
			if(!newMembership.getMembershipList().contains(peer) && 
					!oldMembership.getFailedMembers().contains(peer))
				oldMembersList.add(peer);
		return oldMembersList;
	}

	public void jgroupsSuspect(Address suspected_mbr) {
/*TODO		if(logger.isDebugEnabled())
			logger.debug("suspected member "+suspected_mbr);*/
		IpAddress suspected = (IpAddress) suspected_mbr;
		SocketAddress peer = new InetSocketAddress(suspected.getIpAddress(),suspected.getPort());
		JGroupsMembership m = null;
		try {
			m = (JGroupsMembership) getMembership();
		} catch (NotJoinedException e) {
			//TODO:logger.warn("Received notification of suspected member, but I'm not joined.",e);
			return;
		}
		m.addToFailed(peer);
	}

	public void jgroupsBlock() {
		isChannelBlocked = true;
		notifyBlock();
		/*TODO: if(logger.isDebugEnabled())
			logger.debug("received block");*/
	}

	public boolean isJoined() {
		try {
			return getMembership() != null;
		} catch (NotJoinedException e) {
			return false;
		}
	}
	
	
	/* ***************** jgroups channel listener ******************** */
	
	public void channelConnected(Channel arg0) {
		shunned = false;
		//TODO:logger.debug("jgroups channel connected event");
	}

	public void channelDisconnected(Channel arg0) {
		setMembership(null);
		shunned = false;
		//TODO:logger.debug("jgroups channel disconnected event");
	}

	public void channelReconnected(Address arg0) {
		shunned = false;
		//TODO:logger.debug("jgroups channel reconnected event");
	}

	public void channelShunned() {
		setMembership(null);
		shunned = true;
		//TODO:logger.debug("jgroups channel shunned event");
	}

	public void channelClosed(final Channel arg0) {
		setMembership(null);
		//TODO:logger.debug("jgroups channel closed event");
		new Thread(new Runnable() {
			public void run() {
				if (shunned) {
					try {
						// this is really necessary
						Thread.sleep(5000);
					} catch (InterruptedException e) { e.printStackTrace(); }
					
					try {
						channel.open();
					} catch (ChannelException e) {
						//TODOlogger.error("error reopening channel", e);
					}
					notifyRemoved();
				}
			}
		}, "JGCS_JGROUPS_CLOSER_THREAD").start();
	}
}
