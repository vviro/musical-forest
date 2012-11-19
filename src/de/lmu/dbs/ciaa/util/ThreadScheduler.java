package de.lmu.dbs.ciaa.util;

/**
 * Schedules the thread distribution of an application.
 * 
 * @author Thomas Weber
 *
 */
public class ThreadScheduler {

	private int maxThreads;
	
	private int active = 0;
	
	public ThreadScheduler(int maxThreads) {
		this.maxThreads = maxThreads;
	}
	
	public synchronized int getThreadsAvailable() {
		return maxThreads - getThreadsActive();
	}
	
	public synchronized int getThreadsActive() {
		return active; //ManagementFactory.getThreadMXBean().getThreadCount();
	}
	
	public synchronized void incThreadsActive() {
		active++;
	}
	
	public synchronized void decThreadsActive() throws Exception {
		active--;
		if (active < 0) throw new Exception("Num of node threads below zero: " + active);
	}
	
}
