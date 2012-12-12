package de.lmu.dbs.ciaa.util;

/**
 * A Thread that can be scheduled with ThreadScheduler.
 * 
 * @author Thomas Weber
 *
 */
public class ScheduledThread extends Thread {

	private int threadId;
	
	private static int nextThreadId = 0;
	
	private boolean finished = false;
	
	public ScheduledThread() {
		synchronized(this) {
			this.threadId = nextThreadId;
			nextThreadId++;
		}
	}
	
	public int getThreadId() {
		return threadId;
	}
	
	public boolean threadIsFinished() {
		return finished;
	}
	
	public void setThreadFinished() {
		finished = true;
	}
}
