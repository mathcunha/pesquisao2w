package org.jgroups.tests;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.jgroups.*;
import org.jgroups.blocks.GroupRequest;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.mux.MuxChannel;
import org.jgroups.protocols.DISCARD;
import org.jgroups.protocols.FD;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;
import org.jgroups.util.Rsp;
import org.jgroups.util.RspList;
import org.jgroups.util.Util;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Semaphore;

/**
 * Tests shunning of a channel
 * 
 * @author vlada
 * @version $Id: ShunTest.java,v 1.1.2.9 2008/10/30 15:03:49 belaban Exp $
 */
public class ShunTest extends ChannelTestBase {
    JChannel c1, c2;
    RpcDispatcher disp1, disp2;

    public void setUp() throws Exception {
        super.setUp();        
        CHANNEL_CONFIG = System.getProperty("channel.conf.flush", "flush-udp.xml");
    }

    protected void tearDown() throws Exception {
        if(disp2 != null)
            disp2.stop();
        if(c2 != null)
            c2.close();
        if(disp1 != null)
            disp1.stop();
        if(c1 != null)
            c1.close();
        super.tearDown();
    }

    public boolean useBlocking() {
        return true;
    }

    public void testShunning() {
        connectAndShun(2,false);
    }
   
    
    public static long getCurrentTime() {
        return System.currentTimeMillis();
    }

    /**
     * Tests the case where (members A and B) member B is shunned, excluded by A , then closes and reopens the channel.
     * After B has rejoined, it invokes an RPC and it should get valid return values from both A and B.
     * @throws Exception
     */
    public void testTwoMembersShun() throws Exception {
        View view;
        CHANNEL_CONFIG = System.getProperty("channel.conf.flush", "udp.xml");
        c1=createChannel();
        c1.setOpt(Channel.AUTO_GETSTATE, false);
        c1.addChannelListener(new BelasChannelListener("C1"));
        c2=createChannel();
        c2.setOpt(Channel.AUTO_GETSTATE, false);
        c2.addChannelListener(new BelasChannelListener("C2"));
        disp1=new RpcDispatcher(c1, null, new BelasReceiver("C1"), this);
        disp2=new RpcDispatcher(c2, null, new BelasReceiver("C2"), this);
        c1.connect("demo");
        c2.connect("demo");
        assertEquals(2, c1.getView().size());

        RspList rsps=disp2.callRemoteMethods(null, "getCurrentTime", null, (Class[])null, GroupRequest.GET_ALL, 10000);
        System.out.println(">> rsps:\n" + rsps);
        assertEquals(2, rsps.size());

        ProtocolStack stack=c1.getProtocolStack();
        stack.removeProtocol("VERIFY_SUSPECT");
        Protocol transport=stack.getTransport();
        System.out.println(">> suspecting C2:");
        transport.up(new Event(Event.SUSPECT, c2.getLocalAddress()));

        System.out.println(">> shunning C2:");
        
        //always up EXIT on underlying JChannel as that is what happens
        //in real scenarios
        
        if(c2 instanceof MuxChannel) {
            ((MuxChannel)c2).getChannel().up(new Event(Event.EXIT));
        }
        else {
            c2.up(new Event(Event.EXIT));
        }

        Util.sleep(1000); // give the closer thread time to close the channel
        System.out.println("waiting for C2 to come back");
        int count=1;
        while(true) {
            view=c2.getView();
            // System.out.println("<< C2.view: " + view);
            if((view != null && view.size() >= 2) || count >= 10)
                break;
            count++;
            Util.sleep(1000);
        }
        view=c2.getView();
        System.out.println(">>> view is " + view + " <<<< (should have 2 members)");
        assertEquals(2, view.size());

        Util.sleep(1000);
        System.out.println("invoking RPC on shunned member");
        rsps=disp2.callRemoteMethods(null, "getCurrentTime", null, (Class[])null, GroupRequest.GET_ALL, 10000);
        System.out.println(">> rsps:\n" + rsps);
        assertEquals(2, rsps.size());
        for(Map.Entry<Address, Rsp> entry: rsps.entrySet()) {
            Rsp rsp=entry.getValue();
            assertFalse(rsp.wasSuspected());
            assertTrue(rsp.wasReceived());
        }

        c1.setReceiver(null);
        c2.setReceiver(null);
        c1.clearChannelListeners();
        c2.clearChannelListeners();
    }

    protected void connectAndShun(int shunChannelIndex, boolean useDispatcher) {
        String[] names = new String[] { "A", "B", "C", "D" };

        int count = names.length;

        ShunChannel[] channels = new ShunChannel[count];
        try{
            // Create a semaphore and take all its permits
            Semaphore semaphore = new Semaphore(count);
            semaphore.acquire(count);

            // Create activation threads that will block on the semaphore
            for(int i = 0;i < count;i++){               
               channels[i] = new ShunChannel(names[i],
                                             semaphore,
                                             useDispatcher);  
                              

               JChannel c = (JChannel) channels[i].getChannel();
               if(c instanceof MuxChannel){
                   c = ((MuxChannel)c).getChannel();
               }
               c.addChannelListener(new MyChannelListener(channels));
               // Release one ticket at a time to allow the thread to start
               // working
               channels[i].start();                 
               semaphore.release(1);
               //sleep at least a second and max second and a half
               Util.sleep(2000);                                                                                                          
            }           

            // block until we all have a valid view
            blockUntilViewsReceived(channels, 60000);

            ShunChannel shun = channels[shunChannelIndex];
            log.info("Start shun attempt");
            addDiscardProtocol((JChannel)shun.getChannel());               
            
            //allow shunning to kick in
            Util.sleep(20000);
            
            //and then block until we all have a valid view or fail with timeout
                blockUntilViewsReceived(channels, 60000);

        }catch(Exception ex){
            log.warn("Exception encountered during test", ex);
            fail(ex.getLocalizedMessage());
        }finally{
            for(ShunChannel channel:channels){
                channel.cleanup();
                Util.sleep(2000); 
            }
        }
    }
    
    private static void modifyStack(JChannel ch) {
        ProtocolStack stack=ch.getProtocolStack();

        try {
            ch.getProtocolStack().removeProtocol("VERIFY_SUSPECT");
        } catch (Exception e) {           
            e.printStackTrace();
        }
        FD fd=(FD)stack.findProtocol("FD");
        if(fd != null) {    
            fd.setMaxTries(3);
            fd.setTimeout(1000);
        }       
    }
    
    private static void addDiscardProtocol(JChannel ch) throws Exception {
        ProtocolStack stack=ch.getProtocolStack();
        Protocol transport=stack.getTransport();
        DISCARD discard=new DISCARD();
        Properties props = new Properties();
        props.setProperty("up", "1.0");
        discard.setProperties(props);
        discard.setProtocolStack(ch.getProtocolStack());
        discard.start();
        stack.insertProtocol(discard, ProtocolStack.ABOVE, transport.getName());
    }    
    
    private static class MyChannelListener extends ChannelListenerAdapter{
        ShunChannel[] channels;
        Channel channel;

        public MyChannelListener(ShunChannel[] channels) {
            super();
            this.channels = channels;
        }
        
        public void channelConnected(Channel channel) {
            this.channel = channel;
        }

        public void channelReconnected(Address addr) {    
            System.out.println("Channel reconnected , new address is " + addr);
        }

        public void channelShunned() {
            System.out.println("Shunned channel is " + channel.getLocalAddress());
            System.out.println("Removing discard ");
            for (ShunChannel ch : channels) {
                JChannel c = (JChannel)ch.getChannel();
                try {
                    if(c.getProtocolStack().findProtocol("DISCARD") != null)
                        c.getProtocolStack().removeProtocol("DISCARD");
                } catch (Exception e) {                    
                    e.printStackTrace();
                    c.close();
                }               
            }            
        }       
    }
    
    private static class BelasChannelListener extends ChannelListenerAdapter {
        final String name;

        public BelasChannelListener(String name) {
            this.name=name;
        }

        public void channelClosed(Channel channel) {
            System.out.println("[" + name + "] channelClosed()");
        }

        public void channelConnected(Channel channel) {
            System.out.println("[" + name + "] channelConnected()");
        }

        public void channelDisconnected(Channel channel) {
            System.out.println("[" + name + "] channelDisconnected()");
        }

        public void channelReconnected(Address addr) {
            System.out.println("[" + name + "] channelReconnected(" + addr + ")");
        }

        public void channelShunned() {
            System.out.println("[" + name + "] channelShunned()");
        }
    }

    private static class BelasReceiver extends ReceiverAdapter {
        final String name;

        public BelasReceiver(String name) {
            this.name=name;
        }

        public void viewAccepted(View new_view) {
            System.out.println("[" + name + "] new_view = " + new_view);
        }
    }

    protected class ShunChannel extends PushChannelApplicationWithSemaphore {
        
        public ShunChannel(String name,Semaphore semaphore,boolean useDispatcher) throws Exception{
            super(name, semaphore, useDispatcher);
            modifyStack((JChannel)channel);
        }

        public void useChannel() throws Exception {           
            channel.connect("test");
            channel.getState(null,5000);
            channel.send(null, null, channel.getLocalAddress());            
        }     

       
        public void setState(byte[] state) {
            super.setState(state);            
        }

        public byte[] getState() {
            super.getState(); 
            return new byte[]{'j','g','r','o','u','p','s'};
        }

        public void getState(OutputStream ostream) {
            super.getState(ostream);            
        }

        public void setState(InputStream istream) {
            super.setState(istream);
        }
    }

    public static Test suite() {
        return new TestSuite(ShunTest.class);
    }

    public static void main(String[] args) {
        String[] testCaseName = { ShunTest.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }
}
