package de.lmu.dbs.jforest.util.workergroup;

/**
 * A Thread that can be scheduled with ThreadScheduler.
 * 
 * @author Thomas Weber
 *
 */
public class ScheduledThread extends Thread {

	/**
	 * Thread id
	 */
	private int threadId;
	
	/**
	 * Id of the next thread
	 */
	private static int nextThreadId = 0;
	
	/**
	 * Finished?
	 */
	private boolean finished = false;
	
	/**
	 * 
	 */
	public ScheduledThread() {
		synchronized(this) {
			this.threadId = nextThreadId;
			nextThreadId++;
		}
	}
	
	public int getThreadId() {
		return threadId;
	}

	/**
	 * Returns if the thread finished calculation.
	 * 
	 * @return
	 */
	public boolean threadIsFinished() {
		return finished;
	}
	
	/**
	 * Sets the thread to finished state. Must be called by the subclasses at the end of calculation.
	 * 
	 */
	public void setThreadFinished() {
		finished = true;
	}
}
