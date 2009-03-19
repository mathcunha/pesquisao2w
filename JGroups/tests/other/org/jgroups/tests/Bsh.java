// $Id: Bsh.java,v 1.8 2005/05/30 16:15:11 belaban Exp $


package org.jgroups.tests;


import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.protocols.BSH;
import org.jgroups.stack.IpAddress;

import java.io.BufferedReader;
import java.io.InputStreamReader;


/**
 * Interactive program to test a unicast channel
 * @author Bela Ban March 16 2003
 */
public class Bsh {
    String   host="localhost";
    int      port=0;
    long     timeout=0;
    String   props=null;
    JChannel ch;


    public void start(String[] args) throws Exception {

        for(int i=0; i < args.length; i++) {
            String tmp=args[i];

            if("-props".equals(tmp)) {
                props=args[++i];
                continue;
            }

            if("-host".equals(tmp)) {
                host=args[++i];
                continue;
            }

            if("-port".equals(tmp)) {
                port=Integer.parseInt(args[++i]);
                continue;
            }

            if("-timeout".equals(tmp)) {
                timeout=Long.parseLong(args[++i]);
                continue;
            }

            help();
            return;
        }


        runClient();
    }

    void runClient() throws Exception {
        IpAddress       addr;
        Message         msg;
        String          line;
        BufferedReader  reader;
        BSH.BshHeader   hdr;

        ch=new JChannel(props);
        ch.connect(null); // unicast channel

        addr=new IpAddress(host, port);
        reader= new BufferedReader(new InputStreamReader(System.in));

        while(true) {
            System.out.print("> ");
            line=reader.readLine();
            if(line.startsWith("quit") || line.startsWith("exit")) {
                ch.close();
                return;
            }
            if(line.startsWith("get")) {
                int i=1;
                while(ch.getNumMessages() > 0) {
                    Object obj=ch.receive(1000);
                    System.out.println("#" + i++ + ": " + print(obj) +
                                       ", obj=" + obj);
                }
                continue;
            }
            if(line.startsWith("destroyInterpreter")) {
                msg=new Message(addr, null, line.getBytes());
                hdr=new BSH.BshHeader(BSH.BshHeader.REQ);
                msg.putHeader("BSH", hdr);
                sendAndReceive(msg, 1000);
                continue;
            }

            msg=new Message(addr, null, line.getBytes());
            hdr=new BSH.BshHeader(BSH.BshHeader.REQ);
            msg.putHeader("BSH", hdr);
            sendAndReceive(msg, timeout);
        }
    }

    Object print(Object obj) {
        if(obj == null)
            return null;

        if(obj instanceof Message)
            return ((Message)obj).getObject();
        else
            return obj;
    }


    void sendAndReceive(Message msg, long timeout) {
        Object  obj, result;
        try {
            ch.send(msg);
            obj=ch.receive(timeout);

            if(obj == null || !(obj instanceof Message)) {
                System.err.println("<-- " + obj);
            }
            else {
                result=((Message)obj).getObject();
                System.out.println("<-- " + result);
            }

            // System.out.println("** " + ch.getNumMessages() + " are waiting");
        }
        catch(Throwable t) {
            System.err.println("Bsh.sendAndReceive(): " + t);
        }
    }

    void help() {
        System.out.println("Bsh [-help] [-props <props>]" +
                           "[-host <host>] [-port <port>] [-timeout <timeout>]");
    }


    public static void main(String[] args) {
        try {
            new Bsh().start(args);
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

}
