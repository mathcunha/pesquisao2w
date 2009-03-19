package org.jgroups.tests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.protocols.DISCARD_PAYLOAD;
import org.jgroups.stack.ProtocolStack;
import org.jgroups.util.Util;

import java.util.LinkedList;
import java.util.List;

/**
 * Tests the UNICAST protocol for OOB msgs, tests http://jira.jboss.com/jira/browse/JGRP-377
 * @author Bela Ban
 * @version $Id: UNICAST_OOB_Test.java,v 1.4 2006/12/13 12:02:42 belaban Exp $
 */
public class UNICAST_OOB_Test extends TestCase {
    JChannel ch1, ch2;
    final String props="udp.xml";

    public UNICAST_OOB_Test(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        ch1=new JChannel(props);
        ch2=new JChannel(props);
    }

    public void tearDown() throws Exception {
        if(ch1 != null)
            ch1.close();
        if(ch2 != null)
            ch2.close();
        super.tearDown();
    }


    public void testRegularMessages() throws Exception {
        sendMessages(false);
    }

    public void testOutOfBandMessages() throws Exception {
        sendMessages(true);
    }


    /**
     */
    private void sendMessages(boolean oob) throws Exception {
        DISCARD_PAYLOAD prot1=new DISCARD_PAYLOAD();
        MyReceiver receiver=new MyReceiver();
        ch2.setReceiver(receiver);

        // the second channel will discard the unicast messages with seqno #3 two times, the let them pass
        ch2.getProtocolStack().insertProtocol(prot1, ProtocolStack.BELOW, "UNICAST");

        ch1.connect("x");
        ch2.connect("x");
        assertEquals(2, ch2.getView().getMembers().size());

        Address dest=ch2.getLocalAddress();
        for(int i=1; i <=5; i++) {
            Message msg=new Message(dest, null, new Long(i));
            if(i == 4 && oob)
                msg.setFlag(Message.OOB);
            System.out.println("-- sending message #" + i);
            ch1.send(msg);
            Util.sleep(100);
        }

        Util.sleep(5000); // wait until retransmission of seqno #3 happens, so that 4 and 5 are received as well

        List seqnos=receiver.getSeqnos();
        System.out.println("sequence numbers: " + seqnos);

        // expected sequence is: 1 2 4 3 5 ! Reason: 4 is sent OOB,  does *not* wait until 3 has been retransmitted !!
        Long[] expected_seqnos=oob?
                new Long[]{new Long(1), new Long(2), new Long(4), new Long(3), new Long(5)} : // OOB
                new Long[]{new Long(1), new Long(2), new Long(3), new Long(4), new Long(5)};  // regular
        for(int i=0; i < expected_seqnos.length; i++) {
            Long expected_seqno=expected_seqnos[i];
            Long received_seqno=(Long)seqnos.get(i);
            assertEquals(expected_seqno,  received_seqno);
        }
    }




    public static class MyReceiver extends ReceiverAdapter {
        /** List<Long> of unicast sequence numbers */
        List seqnos=new LinkedList();

        public MyReceiver() {
        }

        public List getSeqnos() {
            return seqnos;
        }

        public void receive(Message msg) {
            if(msg != null) {
                Long num=(Long)msg.getObject();
                seqnos.add(num);
            }
        }
    }

    public static Test suite() {
        return new TestSuite(UNICAST_OOB_Test.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(UNICAST_OOB_Test.suite());
    }
}
