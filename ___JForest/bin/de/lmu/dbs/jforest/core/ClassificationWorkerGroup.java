package de.lmu.dbs.jforest.core;

import java.util.Date;

import de.lmu.dbs.jforest.util.workergroup.ThreadScheduler;
import de.lmu.dbs.jforest.util.workergroup.WorkerGroup;

/**
 * Worker group to manage distributed classifying.
 * 
 * @author Thomas Weber
 *
 */
public class ClassificationWorkerGroup extends WorkerGroup {

	public ClassificationWorkerGroup(ThreadScheduler scheduler, int numOfWork, long waitTime, boolean verbose) {
		super(scheduler, numOfWork, waitTime, verbose);
	}

	@Override
	public void printStats() throws Exception {
		double eta = getEtaMinutes();
		System.out.println(
				timeStampFormatter.format(new Date()) + ": Classification Threads: " + getScheduler().getThreadsActive() + ", Progress: " + getAvgProgressPerc() + "%, ETA: " + (int)eta + " min; " +
				"Heap: " + getHeapMB() + " MB"
		);
	}

}
