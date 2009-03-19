
package org.jgroups.tests;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.TimeoutException;
import org.jgroups.stack.GossipRouter;
import org.jgroups.util.Promise;

/**
 * Test designed to make sure the TUNNEL doesn't lock the client and the GossipRouter
 * under heavy load.
 *
 * @author Ovidiu Feodorov <ovidiu@feodorov.com>
 * @version $Revision: 1.11.4.2 $
 * @see TUNNELDeadLockTest#testStress
 */
public class TUNNELDeadLockTest extends TestCase {
    private JChannel channel;
    private Promise promise;
    private int receivedCnt;

    // the total number of the messages pumped down the channel
    private int msgCount=20000;
    // the message payload size (in bytes);
    private int payloadSize=32;
    // the time (in ms) the main thread waits for all the messages to arrive,
    // before declaring the test failed.
    private int mainTimeout=60000;

    private GossipRouter router=null;


    public TUNNELDeadLockTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        promise=new Promise();
        router=new GossipRouter();
        router.start();
    }

    public void tearDown() throws Exception {

        super.tearDown();

        // I prefer to close down the channel inside the test itself, for the
        // reason that the channel might be brought in an uncloseable state by
        // the test.

        // TO_DO: no elegant way to stop the Router threads and clean-up
        //        resources. Use the Router administrative interface, when
        //        available.

        channel=null;
        promise.reset();
        promise=null;
        router.stop();
    }



    /**
     * Pushes messages down the channel as fast as possible. Sometimes this
     * manages to bring the channel and the Router into deadlock. On the
     * machine I run it usually happens after 700 - 1000 messages and I
     * suspect that this number it is related to the socket buffer size.
     * (the comments are written when I didn't solve the bug yet). <br>
     * <p/>
     * The number of messages sent can be controlled with msgCount.
     * The time (in ms) the main threads wait for the all messages to come can
     * be controlled with mainTimeout. If this time passes and the test
     * doesn't see all the messages, it declares itself failed.
     */
    public void testStress() throws Exception {
        String props="tunnel.xml";
        channel=new JChannel(props);
        channel.connect("agroup");

        // receiver thread
        new Thread(new Runnable() {
            public void run() {
                try {
                    while(true) {
                        if(channel == null)
                            return;
                        Object o=channel.receive(10000);
                        if(o instanceof Message) {
                            receivedCnt++;
                            if(receivedCnt % 2000 == 0)
                                System.out.println("-- received " + receivedCnt);
                            if(receivedCnt == msgCount) {
                                // let the main thread know I got all msgs
                                promise.setResult(new Object());
                                return;
                            }
                        }
                    }
                }
                catch(TimeoutException e) {
                    System.err.println("Timeout receiving from the channel. " + receivedCnt +
                            " msgs received so far.");
                }
                catch(Exception e) {
                    System.err.println("Error receiving data");
                    e.printStackTrace();
                }
            }
        }).start();

        // stress send messages - the sender thread
        new Thread(new Runnable() {
            public void run() {
                try {
                    for(int i=0; i < msgCount; i++) {
                        channel.send(null, null, new byte[payloadSize]);
                        if(i % 2000 == 0)
                            System.out.println("-- sent " + i);
                    }
                }
                catch(Exception e) {
                    System.err.println("Error sending data over ...");
                    e.printStackTrace();
                }
            }
        }).start();


        // wait for all the messages to come; if I don't see all of them in
        // mainTimeout ms, I fail the test

        Object result=promise.getResult(mainTimeout);
        if(result == null) {
            String msg=
                    "The channel has failed to send/receive " + msgCount + " messages " +
                    "possibly because of the channel deadlock or too short " +
                    "timeout (currently " + mainTimeout + " ms). " + receivedCnt +
                    " messages received so far.";
            fail(msg);
        }

        // don't close it in tearDown() because it hangs forever for a failed
        // test.
        channel.close();
    }


    public static Test suite() {
        return new TestSuite(TUNNELDeadLockTest.class);
    }




}
