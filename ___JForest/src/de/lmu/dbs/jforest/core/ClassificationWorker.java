package de.lmu.dbs.jforest.core;

import de.lmu.dbs.jforest.util.workergroup.Worker;
import de.lmu.dbs.jforest.util.workergroup.WorkerGroup;

/**
 * Worker for multithreaded classification.
 * 
 * @author Thomas Weber
 *
 */
public class ClassificationWorker extends Worker {

	private Forest forest;
	
	private byte[][] data;
	
	private float[][][] dataForest;
	
	public ClassificationWorker(WorkerGroup parent, Forest forest, byte[][] data, float[][][] dataForest) {
		super(parent);
		this.forest = forest;
		this.data = data;
		this.dataForest = dataForest;
	}
	
	@Override
	public void work() throws Exception {
		forest.classifyThreaded(this, data, dataForest, getMinIndex(), getMaxIndex());
	}
}
