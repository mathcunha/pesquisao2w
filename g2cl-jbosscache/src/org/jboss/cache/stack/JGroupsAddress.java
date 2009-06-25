package org.jboss.cache.stack;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.jgroups.stack.IpAddress;

public class JGroupsAddress extends IpAddress {
	protected InetSocketAddress inetSocketAddress;
	
	public JGroupsAddress(InetAddress i, int p) {
		super(i,p);
	}

	public void setupSocketAddress(){
		inetSocketAddress = new InetSocketAddress(getIpAddress(), getPort());
	}
	
	public InetSocketAddress getSocketAddress(){
		return inetSocketAddress;
	}
}
