package net.sf.jgcs.jgroups;

import javax.management.MBeanServer;

import org.jgroups.JChannel;

import net.sf.jgcs.ControlSession;
import net.sf.jgcs.DataSession;
import net.sf.jgcs.Protocol;
import net.sf.jgcs.Service;

public class JGroupsJmxConfigurator implements net.sf.jgcs.jmx.JmxConfigurator {

	public void register(	DataSession dataSession,
							ControlSession controlSession,
							Service service,
							Protocol protocol,
							MBeanServer server, 
							String domain) throws Exception {

		domain = domain.replace(':', '-');
		
		JGroupsDataSession jgroupsDataSession = (JGroupsDataSession)dataSession;
		JChannel channel = jgroupsDataSession.getChannel(service);
        org.jgroups.jmx.JmxConfigurator.registerChannel(channel, server,domain,channel.getClusterName(),true);
		
	}

}
