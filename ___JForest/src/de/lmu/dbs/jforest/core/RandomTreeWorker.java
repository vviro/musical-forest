package de.lmu.dbs.jforest.core;

import java.util.List;

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
public class RandomTreeWorker extends Worker {

	private Sampler<Dataset> sampler;
	private List<Object> paramSet;
	private List<Classification> classification;
	private int mode;
	private Object thresholds;
	private long[][][] countClassesLeft;
	private long[][][] countClassesRight;
	private RandomTree rtree;
	
	public RandomTreeWorker(WorkerGroup parent, RandomTree rtree, Sampler<Dataset> sampler, List<Object> paramSet, List<Classification> classification, int mode, float[][] thresholds, long[][][] countClassesLeft, long[][][] countClassesRight) {
		super(parent);
		
		this.rtree = rtree;
		this.sampler = sampler;
		this.paramSet = paramSet;
		this.classification = classification;
		this.mode = mode;
		this.thresholds = thresholds;
		this.countClassesLeft = countClassesLeft;
		this.countClassesRight = countClassesRight;
	}
	
	@Override
	public void work() throws Exception {
		rtree.evaluateFeatures(this, sampler, this.getMinIndex(), this.getMaxIndex(), paramSet, classification, mode, thresholds, countClassesLeft, countClassesRight);
	}
}
