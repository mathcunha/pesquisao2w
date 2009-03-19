package org.jgroups.jmx.protocols;

import org.jgroups.jmx.ProtocolMBean;

import java.util.Enumeration;
import java.util.Date;

/**
 * @author Bela Ban
 * @version $Id: FD_SOCKMBean.java,v 1.2 2007/08/30 10:41:41 belaban Exp $
 */
public interface FD_SOCKMBean extends ProtocolMBean {
    String getLocalAddress();
    String getMembers();
    String getPingableMembers();
    String getPingDest();
    int getNumSuspectEventsGenerated();
    String printSuspectHistory();
    String printCache();
}
