package de.lmu.dbs.musicalforest.threshold;

import java.util.Date;

import de.lmu.dbs.jforest.util.workergroup.ThreadScheduler;
import de.lmu.dbs.jforest.util.workergroup.WorkerGroup;

public class ThresholdWorkerGroup extends WorkerGroup {

	public ThresholdWorkerGroup(ThreadScheduler scheduler, long waitTime, boolean verbose) {
		super(scheduler, waitTime, verbose);
	}

	@Override
	public void printStats() throws Exception {
		double eta = getEtaMinutes();
		System.out.println(
				timeStampFormatter.format(new Date()) + ": " + getScheduler().getThreadsActive() + " Threads searching best MS threshold: " + getAvgProgressPerc() + "%, ETA: " + (int)eta + " min; " +
				"Heap: " + getHeapMB() + " MB"
		);
	}

}
