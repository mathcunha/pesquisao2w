package net.sf.jgcs.jmx;

import javax.management.MBeanServer;

import net.sf.jgcs.ControlSession;
import net.sf.jgcs.DataSession;
import net.sf.jgcs.Protocol;
import net.sf.jgcs.Service;

public interface JmxConfigurator {

	public void register(	DataSession dataSession, 
							ControlSession controlSession, 
							Service service,
							Protocol protocol,
							MBeanServer server, 
							String domain) throws Exception;
	
}
