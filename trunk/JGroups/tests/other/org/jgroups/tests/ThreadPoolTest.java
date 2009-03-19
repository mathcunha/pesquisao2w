// $Id: ThreadPoolTest.java,v 1.2 2004/03/30 06:47:34 belaban Exp $

package org.jgroups.tests;


import org.jgroups.util.ReusableThread;
import org.jgroups.util.ThreadPool;
import org.jgroups.util.Util;


public class ThreadPoolTest {

    static class MyThread extends Thread {
	int num=0;
	public MyThread(int num) {this.num=num;}
	
	public void run() {
	    long sleep_time=(long)(Math.random() * 1000);
	    //System.out.print("Thread #" + num + ": sleeping " + sleep_time + ":");
	    Util.sleep(sleep_time);
	    //System.out.println(" -- done");
	}
    }

    

    public static void main(String[] args) {
	ThreadPool      pool=new ThreadPool(5);
	ReusableThread  t;
	MyThread        my=new MyThread(1);
	int             i=0;

	while(true) {
	    t=pool.getThread();
	    my.num=i++;

	    if(t != null) {
		System.out.println("Assigning task");
		t.assignTask(my);
		Util.sleep(100);
	    }
	    else {
		System.out.println("Waiting a bit for threads to become available...");
		Util.sleep(1000);
	    }

	}
    }



}
