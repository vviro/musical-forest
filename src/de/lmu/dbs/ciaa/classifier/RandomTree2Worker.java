package de.lmu.dbs.ciaa.classifier;

import java.util.List;

import de.lmu.dbs.ciaa.classifier.features.Feature;

/**
 * Takes the part of evaluating the feature function for all candidates
 * and count the results in parallel.
 * 
 * @author Thomas Weber
 *
 */
public class RandomTree2Worker extends Thread {

	private ForestParameters params = null;
	
	private int minIndex = -1;
	
	private int maxIndex = -1;

	private Sampler<Dataset> sampler;
	private List<Feature> paramSet;
	private List<byte[][]> classification;
	private int mode;
	private float[][] thresholds;
	private long[][][] countClassesLeft;
	private long[][][] countClassesRight;
	
	public boolean finished = false;
	
	public RandomTree2Worker(ForestParameters params, int minIndex, int maxIndex, Sampler<Dataset> sampler, List<Feature> paramSet, List<byte[][]> classification, int mode, float[][] thresholds, long[][][] countClassesLeft, long[][][] countClassesRight) {
		this.params = params;
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
			evaluateFeatures();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		finished = true;
	}
	
	public void evaluateFeatures() throws Exception {
		int numOfFeatures = paramSet.size();
		int poolSize = sampler.getPoolSize();
		for(int poolIndex=0; poolIndex<poolSize; poolIndex++) {
			// Each dataset...load spectral data and midi
			TreeDataset dataset = (TreeDataset)sampler.get(poolIndex);
			byte[][] data = dataset.getData();
			byte[][] midi = dataset.getReference();
			byte[][] cla = classification.get(poolIndex);
			
			// get feature results 
			for(int x=0; x<data.length; x++) {
				for(int y=minIndex; y<=maxIndex; y++) {
					// Each random value from the subframe
					if (mode == cla[x][y]) { // Is that point in the training set for this node?
						for(int k=0; k<numOfFeatures; k++) {
							// Each featureset candidate...
							float ev = paramSet.get(k).evaluate(data, x, y);
							for(int g=0; g<params.thresholdCandidatesPerFeature; g++) {
								if (ev >= thresholds[k][g]) {
									// Left
									if (midi[x][y] > 0) {
										countClassesLeft[k][g][0]++;
									} else {
										countClassesLeft[k][g][1]++;
									}
								} else {
									// Right
									if (midi[x][y] > 0) {
										countClassesRight[k][g][0]++;
									} else {
										countClassesRight[k][g][1]++;
									}
								}
							}
						}
					}
				}
			}
		}
	}

}
