package org.jgroups.tests;

import org.jgroups.*;
import org.jgroups.protocols.DISCARD;
import org.jgroups.protocols.pbcast.NAKACK;
import org.jgroups.protocols.pbcast.STABLE;
import org.jgroups.stack.ProtocolStack;
import org.jgroups.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the last message dropped problem in NAKACK (see doc/design/varia2.txt)
 * @author Bela Ban
 * @version $Id: LastMessageDroppedTest.java,v 1.3.2.3 2008/06/11 22:32:44 vlada Exp $
 */
public class LastMessageDroppedTest extends ChannelTestBase {
    JChannel c1, c2;

    protected void setUp() throws Exception {
        super.setUp();
        c1=createChannel();
        c2=createChannel(); 
        modifyStack(c1, c2);
        c1.connect("LastMessageDroppedTest");
        c2.connect("LastMessageDroppedTest");
        View view=c2.getView();
        System.out.println("view = " + view);
        assertEquals("view is " + view, 2, view.size());
    }

    protected void tearDown() throws Exception {
        Util.close(c2, c1);
        super.tearDown();
    }


    public void testLastMessageDropped() throws Exception {
        DISCARD discard=new DISCARD();
        ProtocolStack stack=c1.getProtocolStack();
        stack.insertProtocol(discard, ProtocolStack.BELOW, NAKACK.class);
        c1.setOpt(Channel.LOCAL, false);

        Message m1=new Message(null, null, 1);
        Message m2=new Message(null, null, 2);
        Message m3=new Message(null, null, 3);

        MyReceiver receiver=new MyReceiver();
        c2.setReceiver(receiver);
        c1.send(m1);
        c1.send(m2);
        discard.setDropDownMulticasts(1); // drop the next multicast
        c1.send(m3);
        Util.sleep(100);

        List<Integer> list=receiver.getMsgs();
        for(int i=0; i < 10; i++)  {
            System.out.println("list=" + list);
            if(list.size() == 3)
                break;
            Util.sleep(1000);
        }

        assertEquals("list=" + list, 3, list.size());
    }


    private static void modifyStack(JChannel ... channels) {
        for(JChannel ch: channels) {
            ProtocolStack stack=ch.getProtocolStack();
            STABLE stable=(STABLE)stack.findProtocol(STABLE.class);
            if(stable == null)
                throw new IllegalStateException("STABLE protocol was not found");
            stable.setDesiredAverageGossip(2000);
        }
    }


    private static class MyReceiver extends ReceiverAdapter {
        private final List<Integer> msgs=new ArrayList<Integer>(3);

        public List<Integer> getMsgs() {
            return msgs;
        }

        public void receive(Message msg) {
            // System.out.println("<< " + msg.getObject());
            msgs.add((Integer)msg.getObject());
        }
    }
}
