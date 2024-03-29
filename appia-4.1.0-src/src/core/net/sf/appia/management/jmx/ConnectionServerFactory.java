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
 /**
 * Title:        Appia<p>
 * Description:  Protocol development and composition framework<p>
 * Copyright:    Copyright (c) Nuno Carvalho and Luis Rodrigues<p>
 * Company:      F.C.U.L.<p>
 * @author Nuno Carvalho and Luis Rodrigues
 * @version 1.0
 */

package net.sf.appia.management.jmx;

import java.lang.management.ManagementFactory;
import java.util.Hashtable;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import net.sf.appia.core.Channel;
import net.sf.appia.management.AppiaManagementException;

import java.util.logging.Logger; import java.util.logging.Level;

/**
 * This class defines a ConnectionServerFactory
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class ConnectionServerFactory {

    private static Logger log = Logger.getLogger(ConnectionServerFactory.class.getName());

    private static final Hashtable<JMXConfiguration,ConnectionServerFactory> FACTORIES = 
        new Hashtable<JMXConfiguration,ConnectionServerFactory>();
    private MBeanServer mbeanServer = null;
    private Hashtable<Channel,Object> managedChannels = null;
    
    private ConnectionServerFactory(JMXConfiguration config) throws AppiaManagementException {
        managedChannels = new Hashtable<Channel,Object>();
        createMBeanServer(config);
    }
    
    /**
     * Gets the factory instance for a given configuration. Creates the factory if it does not exist.
     * @param config the given configuration.
     * @return the factory instance.
     * @throws AppiaManagementException
     */
    public static ConnectionServerFactory getInstance(JMXConfiguration config) throws AppiaManagementException{
        ConnectionServerFactory factory = (ConnectionServerFactory) FACTORIES.get(config);
        if(factory == null){
            factory = new ConnectionServerFactory(config);
            FACTORIES.put(config,factory);
        }
        return factory;
    }

    private void createMBeanServer(JMXConfiguration config) throws AppiaManagementException{
        if(mbeanServer != null)
            return;
        
        log.info("Creating MBean server for this Appia instance.");
        // The default MBeanServer
        mbeanServer = ManagementFactory.getPlatformMBeanServer();
    }
    
    /**
     * Registers a MBean for the Channel.
     * @param channel the managed channel
     * @param mbean the mbean to register.
     * @throws AppiaManagementException
     */
    public void registerMBean(Channel channel, Object mbean)
    throws AppiaManagementException {
        try {
            mbeanServer.registerMBean(mbean, new ObjectName(Channel.class.getName()+":name="+channel.getChannelID()));
            managedChannels.put(channel,mbean);
        } catch (InstanceAlreadyExistsException e) {
            throw new AppiaManagementException(e);
        } catch (NotCompliantMBeanException e) {
            throw new AppiaManagementException(e);
        } catch (MalformedObjectNameException e) {
            throw new AppiaManagementException(e);
        } catch (MBeanRegistrationException e) {
            throw new AppiaManagementException(e);
        }
    }

    /**
     * Unregisters the MBean of the Channel.
     * @param channel the managed channel
     * @return the channel manager
     * @throws AppiaManagementException
     */
    public Object unregisterMBean(Channel channel)
    throws AppiaManagementException {
        try {
            mbeanServer.unregisterMBean(new ObjectName(Channel.class.getName()+":name="+channel.getChannelID()));
            return managedChannels.remove(channel);
        } catch (InstanceNotFoundException e) {
            throw new AppiaManagementException(e);
        } catch (MBeanRegistrationException e) {
            throw new AppiaManagementException(e);
        } catch (MalformedObjectNameException e) {
            throw new AppiaManagementException(e);
        }
    }

}
