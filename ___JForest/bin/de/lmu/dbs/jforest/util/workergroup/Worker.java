package de.lmu.dbs.jforest.util.workergroup;

/**
 * Abstract worker implementation.
 * 
 * @author Thomas Weber
 *
 */
public abstract class Worker extends ScheduledThread {

	/**
	 * Minimum work index for this worker
	 */
	private int minIndex = -1;
	
	/**
	 * Maximum work index for this worker
	 */
	private int maxIndex = -1;

	/**
	 * Current worker progress
	 */
	private double progress = 0;

	/**
	 * Worker group this worker belongs to
	 */
	private WorkerGroup parent;
	
	/**
	 * Create a new worker.
	 * 
	 * @param parent
	 */
	public Worker(WorkerGroup parent) {
		this.parent = parent;
	}
	
	/**
	 * Has to implement the actual job to do
	 * 
	 * @throws Exception
	 */
	public abstract void work() throws Exception;
	
	/**
	 * Run the worker. Calls the work method that is implemented by the
	 * children of this class.
	 * 
	 */
	public void run() {
		try {
			work();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		setThreadFinished();
		synchronized(parent) {
			parent.notify();
		}
	}

	/**
	 * Returns the progress of the worker in range [0..1]
	 * 
	 * @return
	 */
	public double getProgress() {
		return progress;
	}
	
	/**
	 * Sets the progress in range [0..1]
	 * 
	 * @param p new progress value
	 * @throws Exception 
	 */
	public void setProgress(double p) throws Exception {
		if (p < 0 || p > 1) throw new Exception("Invalid progress value: " + p);
		this.progress = p;
	}
	
	/**
	 * Sets the worker range. This is used by the worker group do distribute
	 * work by index range among its workers, if distribution by range is used.
	 * 
	 * @param min
	 * @param max
	 * @throws Exception
	 */
	public void setRange(int min, int max) throws Exception {
		this.minIndex = min;
		this.maxIndex = max;
		// Perform checks
		getMinIndex();
		getMaxIndex();
	}
	
	/**
	 * If distribution by range is used, this returns the min index of work for this worker.
	 * 
	 * @return
	 * @throws Exception
	 */
	public int getMinIndex() throws Exception {
		if (minIndex < 0) throw new Exception("This worker has no range set, check implementation.");
		return minIndex;
	}

	/**
	 * If distribution by range is used, this returns the max index of work for this worker.
	 * 
	 * @return
	 * @throws Exception
	 */
	public int getMaxIndex() throws Exception {
		if (maxIndex < 0) throw new Exception("This worker has no range set, check implementation.");
		return maxIndex;
	}
}
