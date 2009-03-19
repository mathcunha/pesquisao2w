// $Id: ConnectionTableDemo.java,v 1.2 2007/09/19 07:45:00 belaban Exp $

package org.jgroups.tests;

import org.jgroups.Address;
import org.jgroups.blocks.ConnectionTable;
import org.jgroups.stack.IpAddress;

import java.io.BufferedReader;
import java.io.InputStreamReader;


public class ConnectionTableDemo implements ConnectionTable.Receiver, ConnectionTable.ConnectionListener {
    ConnectionTable ct=null;
    String dst_host=null;
    int dst_port=0;


    public void receive(Address sender, byte[] data, int offset, int length) {
        String s=new String(data, offset, length);
        System.out.println("<-- " + s + " (from " + sender + ')');
    }


    public void connectionOpened(Address peer_addr) {
        System.out.println("** Connection to " + peer_addr + " opened");
    }

    public void connectionClosed(Address peer_addr) {
        System.out.println("** Connection to " + peer_addr + " closed");
    }


    public void start(int local_port, String dst_host, int dst_port,
                      long reaper_interval, long conn_expire_time) throws Exception {
        BufferedReader in;
        String line;
        Address dest;
        byte[]  data;

        if(reaper_interval > 0 || conn_expire_time > 0)
            ct=new ConnectionTable(local_port, reaper_interval, conn_expire_time);
        else
            ct=new ConnectionTable(local_port);
        ct.addConnectionListener(this);
        this.dst_host=dst_host;
        this.dst_port=dst_port;
        ct.setReceiver(this);

        // System.out.println("**local addr is " + ct.getLocalAddress());

        in=new BufferedReader(new InputStreamReader(System.in));
        while(true) {
            try {
                System.out.print("> ");
                System.out.flush();
                line=in.readLine();
                if(line.startsWith("quit".toLowerCase()) ||
                        line.startsWith("exit".toLowerCase()))
                    break;
                if(line.startsWith("conns")) {
                    System.out.println(ct);
                    continue;
                }
                dest=new IpAddress(dst_host, dst_port);
                data=line.getBytes();
                ct.send(dest, data, 0, data.length);
            }
            catch(Exception e) {
                System.err.println(e);
            }
        }
        ct.stop();
    }


    public static void main(String[] args) {
        String host="localhost", tmp;
        int port=6666, local_port=5555;
        long reaper_interval=0;
        long conn_expire_time=0;

        for(int i=0; i < args.length; i++) {
            tmp=args[i];
            if("-local_port".equals(tmp)) {
                local_port=Integer.parseInt(args[++i]);
                continue;
            }
            if("-remote_host".equals(tmp)) {
                host=args[++i];
                continue;
            }
            if("-remote_port".equals(tmp)) {
                port=Integer.parseInt(args[++i]);
                continue;
            }
            if("-reaper_interval".equals(tmp)) {
                reaper_interval=Long.parseLong(args[++i]);
                continue;
            }
            if("-conn_expire_time".equals(tmp)) {
                conn_expire_time=Long.parseLong(args[++i]);
                continue;
            }
            help();
            return;
        }

        try {
            if(reaper_interval > 0 || conn_expire_time > 0) {
                if(reaper_interval <= 0) reaper_interval=60000;
                if(conn_expire_time <= 0) conn_expire_time=300000;
                new ConnectionTableDemo().start(local_port, host, port, reaper_interval, conn_expire_time);
            }
            else {
                new ConnectionTableDemo().start(local_port, host, port, 0, 0);
            }
        }
        catch(Exception ex) {
            System.err.println("ConnectionTableDemo.main(): " + ex);
        }
    }


    static void help() {
        System.out.println("ConnectionTableDemo [-help] [-local_port <port>] [-remote_host <host>] " +
                           "[-remote_port <port>] [-reaper_interval <interval (msecs)>] " +
                           "[-conn_expire_time <time (msecs)>]");
    }


}
