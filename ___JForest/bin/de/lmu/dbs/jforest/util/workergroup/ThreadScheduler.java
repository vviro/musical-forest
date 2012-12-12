package de.lmu.dbs.jforest.util.workergroup;

import java.util.ArrayList;
import java.util.List;

import de.lmu.dbs.jforest.util.workergroup.ScheduledThread;

/**
 * Schedules the thread distribution of an application.
 * 
 * @author Thomas Weber
 *
 */
public class ThreadScheduler {

	/**
	 * Maximum number of threads in this scheduler
	 */
	private int maxThreads;
	
	/**
	 * Maximum of active threads in the runtime of this scheduler
	 */
	private int peakUsage = 0;
	
	/**
	 * Threads
	 */
	private List<ScheduledThread> threads = new ArrayList<ScheduledThread>();
	
	/**
	 * 
	 * @param maxThreads
	 */
	public ThreadScheduler(int maxThreads) {
		this.maxThreads = maxThreads;
	}
	
	/**
	 * Sets the maximum of threads.
	 * 
	 * @param max
	 */
	public void setMaxThreads(int max) {
		maxThreads = max;
	}
	
	/**
	 * Returns the amount of threads left in the pool.
	 * 
	 * @return
	 * @throws Exception
	 */
	public synchronized int getThreadsAvailable() throws Exception {
		return maxThreads - getThreadsActive();
	}
	
	/**
	 * Starts a thread.
	 * 
	 * @param t
	 * @throws Exception
	 */
	public synchronized void startThread(ScheduledThread t) throws Exception {
		if (getThreadsAvailable() <= 0) throw new Exception("No more threads available to start " + t);
		threads.add(t);
		t.start();
	}

	/**
	 * Returns the number of threads at work.
	 * 
	 * @return
	 * @throws Exception
	 */
	public synchronized int getThreadsActive() throws Exception {
		int ret = 0;
		for(int i=0; i<threads.size(); i++) {
			ret+= threads.get(i).threadIsFinished() ? 0 : 1;
		}
		if (ret > maxThreads) throw new Exception("Too much threads active: " + ret);
		if (ret > peakUsage) peakUsage = ret;
		return ret;
	}
	
	/**
	 * Returns the peak number of threads active.
	 * 
	 * @return
	 */
	public int getPeakUsage() {
		return peakUsage;
	}

	/**
	 * Returns the thread pool size.
	 * 
	 * @return
	 */
	public int getMaxThreads() {
		return maxThreads;
	}
}
