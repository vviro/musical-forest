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
public class RandomTreeWorkerGroup extends WorkerGroup {

	private Node node;
	
	private long count;
	
	private int num;
	
	private int depth;
	
	private Forest forest;
	
	private RandomTree tree;
	
	public RandomTreeWorkerGroup(RandomTree tree, ThreadScheduler scheduler, int numOfWork, boolean verbose, Forest forest, long count, int num, Node node, int depth) {
		super(scheduler, numOfWork, 4000, verbose);
		this.node = node;
		this.count = count;
		this.num = num;
		this.depth = depth;
		this.forest = forest;
		this.tree = tree;
	}

	@Override
	public void printStats() throws Exception {
		// General stats
		String countS = (count == Long.MAX_VALUE) ? "all" : count+"";
		int nt = forest.nodeScheduler.getThreadsActive();
		int et = forest.evalScheduler.getThreadsActive();
		
		double perc = getAvgProgress();
		double all = getOverallProgress(perc, depth);
		double eta = getEtaMinutes(all, forest.getStartTime());
		String etaString = ", " + (int)(all*100) + "% (ETA: " + (int)eta + " min)";
		if (count < (double)tree.getInitialCount() * 0.8) {
			// Dont show estimations because they are not very precise on smaller nodes
			etaString = "";
		}
		
		System.out.println(
				timeStampFormatter.format(new Date()) + ": T" + num + ", Thrds: " + et + " + " + nt + ", Node " + node.id + " (" + (int)(perc*100) + "%), Depth " + depth + ", Values: " + countS + "" + etaString + "; " + 
				"Heap: " + getHeapMB() + " MB"
		);
	}

	/**
	 * Estimates the overall progress.
	 * 
	 * @param perc
	 * @param depth2
	 * @return
	 */
	private double getOverallProgress(double perc, int depth2) {
		int md = forest.getParams().maxDepth;
		double ret = (double)(depth2) + perc;
		return ret / md;
	}
	
}
