package de.lmu.dbs.ciaa.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Schedules the thread distribution of an application.
 * 
 * @author Thomas Weber
 *
 */
public class ThreadScheduler {

	private int maxThreads;
	
	private int max = 0;
	
	private List<ScheduledThread> threads = new ArrayList<ScheduledThread>();
	
	public ThreadScheduler(int maxThreads) {
		this.maxThreads = maxThreads;
	}
	
	public void setMaxThreads(int max) {
		maxThreads = max;
	}
	
	public synchronized int getThreadsAvailable() throws Exception {
		return maxThreads - getThreadsActive();
	}
	
	public synchronized void startThread(ScheduledThread t) throws Exception {
		if (getThreadsAvailable() <= 0) throw new Exception("No more threads available to start " + t);
		threads.add(t);
		t.start();
	}

	public synchronized int getThreadsActive() throws Exception {
		int ret = 0;
		for(int i=0; i<threads.size(); i++) {
			ret+= threads.get(i).threadIsFinished() ? 0 : 1;
		}
		if (ret > maxThreads) throw new Exception("Too much threads active: " + ret);
		if (ret > max) max = ret;
		return ret;
	}
	
	public int getMax() {
		return max;
	}
}
