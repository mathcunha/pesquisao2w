package org.jgroups.tests;

import junit.framework.TestSuite;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.blocks.GroupRequest;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.protocols.TP;
import org.jgroups.stack.ProtocolStack;
import org.jgroups.util.Rsp;
import org.jgroups.util.RspList;
import org.jgroups.util.Util;

import java.util.Properties;


/**
 * Tests return values from MessageDispatcher.castMessage()
 * @author Bela Ban
 * @version $Id: MessageDispatcherUnitTest.java,v 1.5.4.4 2008/04/29 21:16:42 vlada Exp $
 */
public class MessageDispatcherUnitTest extends ChannelTestBase {
    MessageDispatcher disp, disp2;
    JChannel ch, ch2;    


    public MessageDispatcherUnitTest(String name) {
        super(name);
    }


    protected void setUp() throws Exception {
        super.setUp();
        ch=createChannel();
        disableBundling(ch);
        disp=new MessageDispatcher(ch, null, null, null);
        ch.connect("x");
    }




    protected void tearDown() throws Exception {
        disp.stop();
        ch.close();
        if(ch2 != null) {
            disp2.stop();
            ch2.close();
        }
        Util.sleep(500);
        super.tearDown();
    }


    public void testNullMessageToSelf() {
        MyHandler handler=new MyHandler(null);
        disp.setRequestHandler(handler);
        RspList rsps=disp.castMessage(null, new Message(), GroupRequest.GET_ALL, 0);
        System.out.println("rsps:\n" + rsps);
        assertNotNull(rsps);
        assertEquals(1, rsps.size());
        Object obj=rsps.getFirst();
        assertNull(obj);
    }


    public void test200ByteMessageToSelf() {
        sendMessage(200);
    }


    public void test2000ByteMessageToSelf() {
        sendMessage(2000);
    }

    public void test20000ByteMessageToSelf() {
        sendMessage(20000);
    }


    public void testNullMessageToAll() throws Exception {
        disp.setRequestHandler(new MyHandler(null));

        ch2=createChannel();
        disableBundling(ch2);
        long stop,start=System.currentTimeMillis();
        disp2=new MessageDispatcher(ch2, null, null, new MyHandler(null));
        stop=System.currentTimeMillis();
        ch2.connect("x");
        assertEquals(2, ch2.getView().size());
        System.out.println("view: " + ch2.getView());

        System.out.println("casting message");
        start=System.currentTimeMillis();
        RspList rsps=disp.castMessage(null, new Message(), GroupRequest.GET_ALL, 0);
        stop=System.currentTimeMillis();
        System.out.println("rsps:\n" + rsps);
        System.out.println("call took " + (stop-start) + " ms");
        assertNotNull(rsps);
        assertEquals(2, rsps.size());
        Rsp rsp=rsps.get(ch.getLocalAddress());
        assertNotNull(rsp);
        Object ret=rsp.getValue();
        assertNull(ret);


        rsp=rsps.get(ch2.getLocalAddress());
        assertNotNull(rsp);
        ret=rsp.getValue();
        assertNull(ret);
    }

    public void test200ByteMessageToAll() throws Exception {
        sendMessageToBothChannels(200);
    }

    public void test2000ByteMessageToAll() throws Exception {
        sendMessageToBothChannels(2000);
    }

    public void test20000ByteMessageToAll() throws Exception {
        sendMessageToBothChannels(20000);
    }
    
    private void sendMessage(int size) {
        long start, stop;
        MyHandler handler=new MyHandler(new byte[size]);
        disp.setRequestHandler(handler);
        start=System.currentTimeMillis();
        RspList rsps=disp.castMessage(null, new Message(), GroupRequest.GET_ALL, 0);
        stop=System.currentTimeMillis();
        System.out.println("rsps:\n" + rsps);
        System.out.println("call took " + (stop-start) + " ms");
        assertNotNull(rsps);
        assertEquals(1, rsps.size());
        byte[] buf=(byte[])rsps.getFirst();
        assertNotNull(buf);
        assertEquals(size, buf.length);
    }



    private void sendMessageToBothChannels(int size) throws Exception {
        long start, stop;
        disp.setRequestHandler(new MyHandler(new byte[size]));

        ch2=createChannel();
        disableBundling(ch2);
        disp2=new MessageDispatcher(ch2, null, null, new MyHandler(new byte[size]));
        ch2.connect("x");
        assertEquals(2, ch2.getView().size());

        System.out.println("casting message");
        start=System.currentTimeMillis();
        RspList rsps=disp.castMessage(null, new Message(), GroupRequest.GET_ALL, 0);
        stop=System.currentTimeMillis();
        System.out.println("rsps:\n" + rsps);
        System.out.println("call took " + (stop-start) + " ms");
        assertNotNull(rsps);
        assertEquals(2, rsps.size());
        Rsp rsp=rsps.get(ch.getLocalAddress());
        assertNotNull(rsp);
        byte[] ret=(byte[])rsp.getValue();
        assertEquals(size, ret.length);

        rsp=rsps.get(ch2.getLocalAddress());
        assertNotNull(rsp);
        ret=(byte[])rsp.getValue();
        assertEquals(size, ret.length);
    }


    private void disableBundling(JChannel ch) {
        ProtocolStack stack=ch.getProtocolStack();
        TP transport = (TP)stack.findProtocol(TP.class);       
        if(transport != null) {
            Properties tmp=new Properties();
            tmp.setProperty("enable_bundling", "false");
            transport.setProperties(tmp);
        }
    }


    public static junit.framework.Test suite() {
        return new TestSuite(MessageDispatcherUnitTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }


    private static class MyHandler implements RequestHandler {
        byte[] retval=null;


        public MyHandler(byte[] retval) {
            this.retval=retval;
        }

        public Object handle(Message msg) {
            return retval;
        }
    }
}
