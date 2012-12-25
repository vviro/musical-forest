package de.lmu.dbs.jforest.core;

import de.lmu.dbs.jforest.sampler.Sampler;
import de.lmu.dbs.jforest.util.workergroup.Worker;
import de.lmu.dbs.jforest.util.workergroup.WorkerGroup;

/**
 * Takes the part of evaluating the feature function for all candidates
 * and count the results in parallel.
 * 
 * @author Thomas Weber
 *
 */
public class ExpansionWorker extends Worker {

	private Sampler<Dataset> sampler;
	private RandomTree tree;
	private int nodeCount;
	private int progressCount = 0;
	
	public ExpansionWorker(WorkerGroup parent, RandomTree tree, Sampler<Dataset> sampler) {
		super(parent);
		
		this.tree = tree;
		this.sampler = sampler;
		
		this.nodeCount = tree.getNodeCount();
	}
	
	@Override
	public void work() throws Exception {
		System.out.println("Start expansion of tree with " + nodeCount + " nodes...");
		tree.expand(sampler, this);
	}

	public void addProgressNode() throws Exception {
		progressCount++;
		setProgress((double)progressCount / nodeCount);
	}
}
