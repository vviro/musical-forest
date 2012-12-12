package de.lmu.dbs.jforest.util.workergroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages workers to to one distributed job multithreaded.
 * 
 * @author Thomas Weber
 *
 */
public abstract class WorkerGroup {
	
	/**
	 * Number of work to distribute. Set to 0 or less to disable this functionality.
	 */
	private int numOfWork = -1;
	
	/**
	 * Thread scheduler to use
	 */
	private ThreadScheduler scheduler;
	
	/**
	 * Worker thread instances
	 */
	private List<Worker> workers = new ArrayList<Worker>();
	
	/**
	 * Milliseconds waiting time between monitor polls
	 */
	private long waitTime;
	
	/**
	 * Show messages
	 */
	private boolean verbose;

	/**
	 * Date formatter for debug output.
	 */
	protected SimpleDateFormat timeStampFormatter = new SimpleDateFormat("hh:mm:ss");

	/**
	 * Start time, can be used for ETA calculation
	 */
	protected long startTime;
	
	/**
	 * Constructor for thread groups which do not distribute work by index. Verbose mode is disabled.
	 * 
	 * @param scheduler
	 * @param waitTime
	 */
	public WorkerGroup(ThreadScheduler scheduler, long waitTime) {
		this.startTime = System.currentTimeMillis();
		this.scheduler = scheduler;
		this.waitTime = waitTime;
	}

	/**
	 * Constructor for thread groups which do not distribute work by index.
	 * 
	 * @param scheduler
	 * @param waitTime
	 * @param verbose
	 */
	public WorkerGroup(ThreadScheduler scheduler, long waitTime, boolean verbose) {
		this(scheduler, waitTime);
		this.verbose = verbose;
	}
	
	/**
	 * Constructor for thread groups which do distribute work by index.
	 * 
	 * @param scheduler
	 * @param numOfWork
	 * @param waitTime
	 * @param verbose
	 */
	public WorkerGroup(ThreadScheduler scheduler, int numOfWork, long waitTime, boolean verbose) {
		this(scheduler, waitTime, verbose);
		this.numOfWork = numOfWork;
	}

	/**
	 * Add a new Worker
	 * 
	 * @param w
	 */
	public void add(Worker w) {
		workers.add(w);
	}

	/**
	 * Implement this to integrate your own stat output.
	 * 
	 * @throws Exception 
	 */
	public abstract void printStats() throws Exception;
	
	/**
	 * Run the group and observe/wait. It is advised to synchronize this with the scheduler.
	 * 
	 * @return
	 * @throws Exception 
	 */
	public void runGroup() throws Exception {
		if (numOfWork > 0 && workers.size() > numOfWork) throw new Exception("Too few work (" + numOfWork + ") for " + workers.size() + " threads, check parameters");
		if (workers.size() < 1) throw new Exception("Thread number invalid: " + workers.size());
		
		double ipw = (double)numOfWork / workers.size();
		for(int i=0; i<workers.size(); i++) {
			if (numOfWork > 0) {
				int min = (int)(i*ipw);
				int max = (int)((i+1)*ipw - 1);
				if (i == workers.size()-1 || max >= numOfWork) max = numOfWork-1;
				//System.out.println(min + " to " + max);
				workers.get(i).setRange(min, max);
			}
			scheduler.startThread(workers.get(i));
		}
		check();
		
		// Wait for the worker threads
		long lastTime = 0;
		while(true) {
			synchronized(this) {
				try {
					if (verbose) {
						wait(waitTime);
					} else {
						wait();
					}
				} catch (InterruptedException e) {
					if (verbose) System.out.println("[Wait interrupted by VM, continuing...]");
				}
			}
			
			if (verbose && (System.currentTimeMillis() - lastTime > waitTime)) {
				lastTime = System.currentTimeMillis();
				printStats();
			}
			
			if (isFinished()) break;
		}
	}
	
	/**
	 * Check workers integrity.
	 * 
	 * @throws Exception 
	 */
	public void check() throws Exception {
		if (numOfWork > 0) {
			int nw = 0;
			for(int i=0; i<workers.size(); i++) {
				if (i<workers.size()-1 && workers.get(i).getMaxIndex() != workers.get(i+1).getMinIndex() - 1) throw new Exception("Invalid ranges: " + i + ".max: " + workers.get(i).getMaxIndex() + "; " + (i+1) + ".min: " + workers.get(i+1).getMaxIndex());
				nw+= (workers.get(i).getMaxIndex() - workers.get(i).getMinIndex()) + 1;
			}
			if (nw != numOfWork) throw new Exception("Invalid work distribution: " + nw + " of " + numOfWork);
		}
	}
	
	/**
	 * Returns true if all workers hav finished working
	 * 
	 * @return
	 */
	public boolean isFinished() {
		for(int i=0; i<workers.size(); i++) {
			if (!workers.get(i).threadIsFinished()) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Returns the average progress in range [0..100]
	 * 
	 * @return
	 */
	public int getAvgProgressPerc() {
		return (int)(getAvgProgress()*100);
	}

	/**
	 * Returns the average progress in range [0..1]
	 * 
	 * @return
	 */
	public double getAvgProgress() {
		double avgProgress = 0;
		for(int i=0; i<workers.size(); i++) {
			avgProgress+= workers.get(i).getProgress();
		}
		avgProgress/= workers.size();
		return avgProgress;
	}
	
	/**
	 * Returns the used thread scheduler
	 * 
	 * @return
	 */
	public ThreadScheduler getScheduler() {
		return scheduler;
	}
	
	/**
	 * Returns the JVM heap size in megabytes.
	 * 
	 * @return
	 */
	public int getHeapMB() {
		return (int)Math.round((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()) / (1024.0*1024.0));
	}
	
	/**
	 * Estimates the time for growing the forest.
	 * 
	 * @param progress in range [0,1]
	 * @param startTimeMillis
	 * @return
	 */
	public double getEtaMinutes() {
		double progress = getAvgProgress();
		return getEtaMinutes(progress, startTime);
	}
	
	/**
	 * Estimates the time for growing the forest.
	 * 
	 * @param progress in range [0,1]
	 * @param startTimeMillis
	 * @return
	 */
	public double getEtaMinutes(double progress, long startTimeMillis) {
		double delta = (double)(System.currentTimeMillis() - startTimeMillis);
		double allTime = delta / progress;
		return (allTime - delta) / (1000 * 60);
	}
}
