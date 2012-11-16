package de.lmu.dbs.ciaa.classifier;

import java.util.List;

import de.lmu.dbs.ciaa.classifier.features.Feature;
import de.lmu.dbs.ciaa.util.Logfile;

public class MusicalRandomTree extends RandomTree {

	public MusicalRandomTree(ForestParameters params, int num, Logfile log) throws Exception {
		super(params, num, log);
	}

	/**
	 * Creates a tree instance for recursion into a new thread. The arguments are just used to transport
	 * the arguments of growRec to the new thread. See method growRec source code.
	 * 
	 * @throws Exception 
	 * 
	 */
	public MusicalRandomTree(ForestParameters params, Tree root, Sampler<Dataset> sampler, List<byte[][]> classification, long count, Node node, int mode, int depth, int maxDepth, int num, Logfile log) throws Exception {
		super(params, root, sampler, classification, count, node, mode, depth, maxDepth, num, log);
	}

	/**
	 * Creates a blank tree, used as a factory.
	 * 
	 * @throws Exception
	 */
	public MusicalRandomTree() throws Exception {
		super(null, -1, null);
	}
	
	/**
	 * This does the actual evaluation work.
	 * 
	 * @param sampler
	 * @param minIndex
	 * @param maxIndex
	 * @param paramSet
	 * @param classification
	 * @param count
	 * @param mode
	 * @param thresholds
	 * @param countClassesLeft
	 * @param countClassesRight
	 * @param node
	 * @param depth
	 * @throws Exception
	 */
	public void evaluateFeatures(Sampler<Dataset> sampler, int minIndex, int maxIndex, List<Feature> paramSet, List<byte[][]> classification, int mode, float[][] thresholds, long[][][] countClassesLeft, long[][][] countClassesRight) throws Exception {
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

	/**
	 * Returns a new instance of the tree.
	 * 
	 * @param params
	 * @param root
	 * @param sampler
	 * @param classification
	 * @param count
	 * @param node
	 * @param mode
	 * @param depth
	 * @param maxDepth
	 * @param num
	 * @param log
	 * @return
	 * @throws Exception 
	 */
	@Override
	public RandomTree getInstance(ForestParameters params, Tree root, Sampler<Dataset> sampler, List<byte[][]> classification, long count, Node node, int mode, int depth, int maxDepth, int num, Logfile log) throws Exception {
		return new MusicalRandomTree(params, root, sampler, classification, count, node, mode, depth, maxDepth, num, log);
	}

	/**
	 * Returns a new instance of the tree.
	 * 
	 */
	@Override
	public RandomTree getInstance(ForestParameters params, int num, Logfile log) throws Exception {
		return new MusicalRandomTree(params, num, log);
	}
}
