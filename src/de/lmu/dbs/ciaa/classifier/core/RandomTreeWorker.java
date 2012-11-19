package de.lmu.dbs.ciaa.classifier.core;

import java.util.List;

/**
 * Takes the part of evaluating the feature function for all candidates
 * and count the results in parallel.
 * 
 * @author Thomas Weber
 *
 */
public class RandomTreeWorker extends Thread {

	private int minIndex = -1;
	
	private int maxIndex = -1;

	private Sampler<Dataset> sampler;
	private List<Object> paramSet;
	private List<Object> classification;
	private int mode;
	private Object thresholds;
	private long[][][] countClassesLeft;
	private long[][][] countClassesRight;
	private RandomTree parent;
	
	public boolean finished = false;
	
	public RandomTreeWorker(RandomTree parent, int minIndex, int maxIndex, Sampler<Dataset> sampler, List<Object> paramSet, List<Object> classification, int mode, float[][] thresholds, long[][][] countClassesLeft, long[][][] countClassesRight) {
		this.parent = parent;
		
		this.minIndex = minIndex;
		this.maxIndex = maxIndex;
		this.sampler = sampler;
		this.paramSet = paramSet;
		this.classification = classification;
		this.mode = mode;
		this.thresholds = thresholds;
		this.countClassesLeft = countClassesLeft;
		this.countClassesRight = countClassesRight;
	}
	
	public void run() {
		try {
			parent.evaluateFeatures(sampler, minIndex, maxIndex, paramSet, classification, mode, thresholds, countClassesLeft, countClassesRight);
			parent.forest.scheduler.decThreadsActive();
			System.out.println("Finished evaluation thread");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		finished = true;
	}
	
}
