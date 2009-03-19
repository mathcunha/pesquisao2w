package org.jgroups.tests;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.jgroups.Address;
import org.jgroups.MergeView;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.util.Util;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests concurrent startup with state transfer.
 * 
 * @author bela
 * @version $Id: ConcurrentStartupTest.java,v 1.32.2.9 2008/11/21 17:49:31 vlada Exp $
 */
public class ConcurrentStartupTest extends ChannelTestBase {

    private AtomicInteger mod = new AtomicInteger(1);

    public void setUp() throws Exception {
        super.setUp();
        mod.set(1);
        CHANNEL_CONFIG = System.getProperty("channel.conf.flush", "flush-udp.xml");
    }

    public boolean useBlocking() {
        return true;
    }   

    public void testConcurrentStartupState1() {
        concurrentStartupHelper(true,false);
    }

    public void testConcurrentStartupState2() {
        concurrentStartupHelper(true,false);
    }
    
    public void testConcurrentStartupState3() {
        concurrentStartupHelper(true,false);
    }
    
    public void testConcurrentStartupState4() {
        concurrentStartupHelper(true,false);
    }
    
    public void testConcurrentStartupState5() {
        concurrentStartupHelper(true,false);
    }
    
    public void testConcurrentStartupState6() {
        concurrentStartupHelper(true,true);
    }
    
    public void testConcurrentStartupState7() {
        concurrentStartupHelper(true,true);
    }
    
    public void testConcurrentStartupState8() {
    	concurrentStartupHelper(true,true);
    }
    
    public void testConcurrentStartupState9() {
    	concurrentStartupHelper(true,true);
    }
    
    public void testConcurrentStartupState10() {
    	concurrentStartupHelper(true,true);
    }
    /**
     * Tests concurrent startup and message sending directly after joining See
     * doc/design/ConcurrentStartupTest.txt for details. This will only work
     * 100% correctly once we have FLUSH support (JGroups 2.4)
     * 
     * NOTE: This test is not guaranteed to pass at 100% rate until combined
     * join and state transfer using one FLUSH phase is introduced (Jgroups
     * 2.5)[1].
     * 
     * [1] http://jira.jboss.com/jira/browse/JGRP-236
     * 
     * 
     */
    protected void concurrentStartupHelper(boolean useDispatcher, boolean connectAndGetState) {
        String[] names = new String[] { "A", "B", "C", "D" };

        int count = names.length;

        ConcurrentStartupChannel[] channels = new ConcurrentStartupChannel[count];
        try{
            // Create a semaphore and take all its permits
            Semaphore semaphore = new Semaphore(count);
            semaphore.acquire(count);

            // Create activation threads that will block on the semaphore
            for(int i=0;i < count;i++) {

                channels[i]=new ConcurrentStartupChannel(names[i], semaphore, useDispatcher,connectAndGetState);

                // Release one ticket at a time to allow the thread to start
                // working
                channels[i].start();
                semaphore.release(1);                                
            }

            // Make sure everyone is in sync
            blockUntilViewsReceived(channels, 60000);


            for(ConcurrentStartupChannel c: channels) {
                View view=c.getChannel().getView();
                System.out.println("view = " + view);
            }

            // Sleep to ensure the threads get all the semaphore tickets
            Util.sleep(1000);

            // Reacquire the semaphore tickets; when we have them all
            // we know the threads are done
            boolean acquired = semaphore.tryAcquire(count, 20, TimeUnit.SECONDS);
            if(!acquired){
                log.warn("Most likely a bug, analyse the stack below:");
                log.warn(Util.dumpThreads());
            }

            // Sleep to ensure async message arrive
            Util.sleep(1000);

            // do test verification            
            for (ConcurrentStartupChannel channel : channels) {
                log.info(channel.getName() +"=" +channel.getList());  
            }
            for (ConcurrentStartupChannel channel : channels) {
                log.info(channel.getName() +"=" +channel.getModifications());  
            }
            
            
            for (ConcurrentStartupChannel channel : channels) {
                assertEquals(channel.getName() + " should have " + count + " elements", count, channel.getList().size());
            }
            
            for(ConcurrentStartupChannel channel:channels){                
                checkEventStateTransferSequence(channel);
            }
        }catch(Exception ex){
            log.warn("Exception encountered during test", ex);
            fail(ex.getLocalizedMessage());
        }finally{
            for(int i = count-1;i >=0;i--){
                channels[i].cleanup();
                Util.sleep(250);
            }                
        }
    }

    protected int getMod() {       
        return mod.incrementAndGet();
    } 
    
    protected class ConcurrentStartupChannel extends PushChannelApplicationWithSemaphore {
        private final List<Address> l = new LinkedList<Address>();       
        private boolean connectAndGetState;
        private final Map<Integer,Object> mods = new TreeMap<Integer,Object>();       

        public ConcurrentStartupChannel(String name,Semaphore semaphore,boolean useDispatcher, boolean connectAndGetState) throws Exception{
            super(name, semaphore, useDispatcher);
            this.connectAndGetState = connectAndGetState;
        }

        public void useChannel() throws Exception {
        	if (connectAndGetState) {
				channel.connect("test", null, null, 25000);
			} else {
				channel.connect("test");
				channel.getState(null, 20000);
			}
        	
            LinkedList<Address> l =new LinkedList<Address>();
            l.add(channel.getLocalAddress());
            channel.send(null, null, l);
        }

        List<Address> getList() {
            return l;
        }

        Map<Integer,Object> getModifications() {
            return mods;
        }

        public void receive(Message msg) {
            if(msg.getBuffer() == null)
                return;
            List<Address> obj = (List)msg.getObject();
            log.info("-- [#" + getName() + " (" + channel.getLocalAddress() + ")]: received " + obj);
            synchronized(this){
                l.addAll(obj);
                Integer key = new Integer(getMod());
                mods.put(key, obj);
            }
        }

        public void viewAccepted(View new_view) {
            super.viewAccepted(new_view);
            synchronized(this) {
                Integer key=new Integer(getMod());
                mods.put(key, new_view.getVid());

                if(new_view instanceof MergeView) {
                    MergeView mv=(MergeView)new_view;
                    Vector<View> subgroups=mv.getSubgroups();
                    boolean amISubgroupLeader=false;
                    for(View view:subgroups) {
                        Address subCoord=view.getMembers().firstElement();
                        amISubgroupLeader=getLocalAddress().equals(subCoord);
                        if(amISubgroupLeader) {
                            for(View view2:subgroups) {
                                if(!getLocalAddress().equals(view2.getMembers().firstElement())) {
                                    for(Address member:view2.getMembers()) {
                                        Message m=new Message(member, null, (Serializable)l);
                                        try {
                                            channel.send(m);
                                        }
                                        catch(Exception e) {
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        public void setState(byte[] state) {
            super.setState(state);
            try{
                List<Address> tmp = (List) Util.objectFromByteBuffer(state);
                synchronized(this){
                    l.clear();
                    l.addAll(tmp);
                    log.info("-- [#" + getName()
                             + " ("
                             + channel.getLocalAddress()
                             + ")]: state is "
                             + l);
                    Integer key = new Integer(getMod());
                    mods.put(key, tmp);
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        public byte[] getState() {
            super.getState();
            List<Address> tmp = null;
            synchronized(this){
                tmp = new LinkedList<Address>(l);
                try{
                    return Util.objectToByteBuffer(tmp);
                }catch(Exception e){
                    e.printStackTrace();
                    return null;
                }
            }
        }

        public void getState(OutputStream ostream) {
            super.getState(ostream);
            ObjectOutputStream oos = null;
            try{
                oos = new ObjectOutputStream(ostream);
                List<Address> tmp = null;
                synchronized(this){
                    tmp = new LinkedList<Address>(l);
                }
                oos.writeObject(tmp);
                oos.flush();
            }catch(IOException e){
                e.printStackTrace();
            }finally{
                Util.close(oos);
            }
        }

        public void setState(InputStream istream) {
            super.setState(istream);
            ObjectInputStream ois = null;
            try{
                ois = new ObjectInputStream(istream);
                List<Address> tmp = (List) ois.readObject();
                synchronized(this){
                    l.clear();
                    l.addAll(tmp);
                    log.info("-- [#" + getName()
                             + " ("
                             + channel.getLocalAddress()
                             + ")]: state is "
                             + l);
                    Integer key = new Integer(getMod());
                    mods.put(key, tmp);
                }
            }catch(Exception e){
                e.printStackTrace();
            }finally{
                Util.close(ois);
            }
        }
    }

    public static Test suite() {
        return new TestSuite(ConcurrentStartupTest.class);
    }

    public static void main(String[] args) {
        String[] testCaseName = { ConcurrentStartupTest.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }
}
