package de.lmu.dbs.musicalforest.threshold;

import de.lmu.dbs.jforest.core2d.Forest2d;
import de.lmu.dbs.jforest.core2d.TreeDataset2d;
import de.lmu.dbs.jforest.util.workergroup.Worker;
import de.lmu.dbs.jforest.util.workergroup.WorkerGroup;
import de.lmu.dbs.musicalforest.classifier.AccuracyTest;

public class ThresholdWorker extends Worker {

	protected ThresholdOptimizer optimizer;
	
	private Forest2d forest;
	
	private TreeDataset2d dataset;
	
	private AccuracyTest[][] testsOnset;
	
	private AccuracyTest[][] testsOffset;
	
	private int num;
	
	private float[][][] classification;
	
	public ThresholdWorker(WorkerGroup parent, ThresholdOptimizer optimizer, Forest2d forest, TreeDataset2d dataset, float[][][] classification, AccuracyTest[][] testsOnset, AccuracyTest[][] testsOffset, int num) {
		super(parent);
		this.optimizer = optimizer;
		this.forest = forest;
		this.dataset = dataset;
		this.classification = classification;
		this.testsOnset = testsOnset;
		this.testsOffset = testsOffset;
		this.num = num;
	}
	
	@Override
	public void work() throws Exception {
		optimizer.processThreaded(this, forest, dataset, classification, testsOnset, testsOffset, num);
	}
}
