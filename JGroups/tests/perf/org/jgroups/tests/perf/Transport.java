package org.jgroups.tests.perf;

import java.util.Properties;
import java.util.Map;

/**
 * Generic transport abstraction for all different transports (JGroups, JMS, UDP, TCP). The lifecycle is
 * <ol>
 * <li>Create an instance of the transport (using the empty constructor)
 * <li>Call <code>create()</code>
 * <li>Possibly call <code>setReceiver()</code>
 * <li>Call <code>start()</code>
 * <li>Call <code>send()</code>
 * <li>Call <code>stop()</stop>
 * <li>Call <code>destroy()</code> (alternatively call <code>start()</code> again)
 * </ol>
 * @author Bela Ban Jan 22
 * @author 2004
 * @version $Id: Transport.java,v 1.4 2006/12/19 08:51:47 belaban Exp $
 */
public interface Transport {

    /** Create the transport */
    void create(Properties properties) throws Exception;

    /** Get the local address (= endpoint) of this transport. Guaranteed to be called <em>after</em>
     *  <code>create()</code>, possibly even later (after <code>start()</code>) */
    Object getLocalAddress();

    /** Start the transport */
    void start() throws Exception;

    /** Stop the transport */
    void stop();

    /** Destroy the transport. Transport cannot be reused after this call, but a new instance has to be created */
    void destroy();

    /** Set the receiver */
    void setReceiver(Receiver r);

    Map dumpStats();

    /**
     * Send a message
     * @param destination A destination. If null, send a message to all members
     * @param payload A buffer to be sent
     * @throws Exception
     */
    void send(Object destination, byte[] payload, boolean oob) throws Exception;
}
