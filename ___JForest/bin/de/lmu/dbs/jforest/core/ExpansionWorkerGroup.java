package de.lmu.dbs.jforest.core;

import java.util.Date;

import de.lmu.dbs.jforest.util.workergroup.ThreadScheduler;
import de.lmu.dbs.jforest.util.workergroup.WorkerGroup;

/**
 * Worker group for evaluation threading in RandomTree
 * 
 * @author Thomas Weber
 *
 */
public class ExpansionWorkerGroup extends WorkerGroup {

	public ExpansionWorkerGroup(ThreadScheduler scheduler, int numOfWork, boolean verbose) {
		super(scheduler, numOfWork, 4000, verbose);
	}

	@Override
	public void printStats() throws Exception {
		// General stats
		double perc = getAvgProgress();
		//double eta = 0; //getEtaMinutes(all, forest.getStartTime());
		//String etaString = ""; //", " + (int)(all*100) + "ETA: " + (int)eta + " min";

		System.out.println(
				timeStampFormatter.format(new Date()) + ": Expanded " + (int)(perc*100) + "%, Threads: " + this.getScheduler().getThreadsActive() + "; " + 
				"Heap: " + getHeapMB() + " MB"
		);
	}

}
