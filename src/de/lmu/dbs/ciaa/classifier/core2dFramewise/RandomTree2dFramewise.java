package de.lmu.dbs.ciaa.classifier.core2dFramewise;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.lmu.dbs.ciaa.classifier.core.Dataset;
import de.lmu.dbs.ciaa.classifier.core.ForestParameters;
import de.lmu.dbs.ciaa.classifier.core.Node;
import de.lmu.dbs.ciaa.classifier.core.Sampler;
import de.lmu.dbs.ciaa.classifier.core.RandomTree;
import de.lmu.dbs.ciaa.classifier.core.TreeDataset;
import de.lmu.dbs.ciaa.util.ArrayToImage;
import de.lmu.dbs.ciaa.util.Logfile;
import de.lmu.dbs.ciaa.util.Statistic;

/**
 * Represents one random decision tree. Extend this to create randomized decision
 * trees for various applications.
 * 
 * @author Thomas Weber
 *
 */
public abstract class RandomTree2dFramewise extends RandomTree {

	/**
	 * Creates a tree (main constructor).
	 * 
	 * @throws Exception 
	 * 
	 */
	public RandomTree2dFramewise(ForestParameters params, int numOfClasses, int num, Logfile log) throws Exception {
		super(numOfClasses, num, log);
		params.check();
		this.params = params;
		this.infoGain = new Statistic();
	}
	
	/**
	 * Creates a tree instance for recursion into a new thread. The arguments are just used to transport
	 * the arguments of growRec to the new thread. See method growRec source code.
	 * 
	 * @throws Exception 
	 * 
	 */
	public RandomTree2dFramewise(ForestParameters params, RandomTree root, Sampler<Dataset> sampler, List<Object> classification, long count, Node node, int mode, int depth, int maxDepth, int num, Logfile log) throws Exception {
		this(params, root.numOfClasses, num, log);
		this.newThreadRoot = root;
		this.newThreadSampler = sampler;
		this.newThreadClassification = classification;
		this.newThreadNode = node;
		this.newThreadMode = mode;
		this.newThreadDepth = depth;
		this.newThreadMaxDepth = maxDepth;
		this.newThreadCount = count;
	}
	
	/**
	 * Returns the classification of the tree at a given value in data: data[x][y]
	 * 
	 * @param data
	 * @param x
	 * @param y
	 * @return
	 * @throws Exception
	 */
	public float[] classify(final Object data, final int x) throws Exception {
		return classifyRec((byte[][])data, tree, 0, 0, x);
	}
	
	/**
	 * Internal classification method.
	 * 
	 * @param data
	 * @param node current node to process
	 * @param x
	 * @param y
	 * @return
	 * @throws Exception
	 */
	protected float[] classifyRec(final byte[][] data, final Node node, int mode, int depth, final int x) throws Exception {
		if (node.isLeaf()) {
			return node.probabilities;
		} else {
			if (params.saveNodeClassifications > depth-1 && node.debugObject == null) node.debugObject = new int[data.length]; // TMP
			
			if (((Feature2dFramewise)node.feature).evaluate(data, x) >= node.feature.threshold) {
				if (params.saveNodeClassifications > depth-1) ((int[])node.debugObject)[x] = 1;
				return classifyRec(data, node.left, 1, depth+1, x);
			} else {
				if (params.saveNodeClassifications > depth-1) ((int[])node.debugObject)[x] = 2;
				return classifyRec(data, node.right, 2, depth+1, x);
			}
		}
	}
	
	/**
	 * Build first classification array (from bootstrapping samples and random values per sampled frame)
	 * 
	 * @param sampler
	 * @return
	 * @throws Exception 
	 */
	protected List<Object> getPreClassification(Sampler<Dataset> sampler) throws Exception {
		List<Object> classification = new ArrayList<Object>(); // Classification arrays for each dataset in the sampler, same index as in sampler
		//int vpf = (int)(params.percentageOfRandomValuesPerFrame * params.frequencies.length); // values per frame
		for(int i=0; i<sampler.getPoolSize(); i++) {
			TreeDataset d = (TreeDataset)sampler.get(i);
			byte[] cl = (byte[])d.getInitialClassification();
			classification.add(cl);
		}
		
		// TMP: Save classifications to PNGs
		/*
		for(int i=0; i<sampler.getPoolSize(); i++) {
			System.out.println("Visualize " + i);
			String fname = params.workingFolder + File.separator + "T" + num + "_Index_" + i + "_InitialClassification.png";
			ArrayUtils.toImage(fname, ArrayUtils.positivize(classification.get(i)), Color.YELLOW);
		}
		System.exit(0);
		//*/
		return classification;
	}
	
	/**
	 * Splits the training data set of one node.
	 * 
	 * @param sampler
	 * @param classification
	 * @param mode
	 * @param node
	 * @return
	 * @throws Exception
	 */
	public List<Object> splitValues(Sampler<Dataset> sampler, List<Object> classification, int mode, Node node, long[] counts) throws Exception {
		Feature2dFramewise feature = (Feature2dFramewise)node.feature;
		List<Object> classificationNext = new ArrayList<Object>(sampler.getPoolSize());
		int poolSize = sampler.getPoolSize();
		for(int i=0; i<poolSize; i++) {
			TreeDataset dataset = (TreeDataset)sampler.get(i);
			byte[][] data = (byte[][])dataset.getData();
			byte[] cla = (byte[])classification.get(i);
			byte[] claNext = new byte[data.length];
			for(int x=0; x<data.length; x++) {
				//for(int y=0; y<params.frequencies.length; y++) {
					if (mode == cla[x]) {
						if (feature.evaluate(data, x) >= feature.threshold) {
							claNext[x] = 1; // Left
							counts[0]++;
						} else {
							claNext[x] = 2; // Right
							counts[1]++;
						}
					}
				//}
			}
			classificationNext.add(claNext);
		}
		return classificationNext;
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
	public void evaluateFeatures(Sampler<Dataset> sampler, int minIndex, int maxIndex, List<Object> paramSet, List<Object> classification, int mode, Object thresholds, long[][][] countClassesLeft, long[][][] countClassesRight) throws Exception {
		List<Feature2dFramewise> paramSetC = new ArrayList<Feature2dFramewise>();
		for(int i=0; i<paramSet.size(); i++) {
			paramSetC.add((Feature2dFramewise)paramSet.get(i));
		}
		int numOfFeatures = paramSet.size();
		int poolSize = sampler.getPoolSize();
		float[][] thresholdsC = (float[][])thresholds;
		for(int poolIndex=0; poolIndex<poolSize; poolIndex++) {
			// Each dataset...load data and reference
			TreeDataset dataset = (TreeDataset)sampler.get(poolIndex);
			byte[][] data = (byte[][])dataset.getData();
			byte[][] ref = (byte[][])dataset.getReference();
			byte[] cla = (byte[])classification.get(poolIndex);
			
			// get feature results 
			for(int x=0; x<data.length; x++) {
				//for(int y=minIndex; y<=maxIndex; y++) {
					// Each random value from the subframe
					if (mode == cla[x]) { // Is that point in the training set for this node?
						for(int k=0; k<numOfFeatures; k++) {
							// Each featureset candidate...
							float ev = paramSetC.get(k).evaluate(data, x);
							for(int g=0; g<params.thresholdCandidatesPerFeature; g++) {
								if (ev >= thresholdsC[k][g]) {
									// Left
									countClassesLeft[k][g][getRefClass(ref, x)]++;
								} else {
									// Right
									countClassesRight[k][g][getRefClass(ref, x)]++;
								}
							}
						}
					}
				//}
			}
		}
	}
	
	/**
	 * Calculates leaf probability.
	 * 
	 * @param sampler
	 * @param mode
	 * @param depth
	 * @return
	 * @throws Exception
	 */
	protected float[] calculateLeaf(final Sampler<Dataset> sampler, List<Object> classification, final int mode, final int depth) throws Exception {
		//int numOfClasses = this.getNumOfClasses();
		float[] l = new float[numOfClasses];
		long all = 0;
		// See how much was judged right
		for(int i=0; i<sampler.getPoolSize(); i++) {
			TreeDataset dataset = (TreeDataset)sampler.get(i);
			byte[][] ref = (byte[][])dataset.getReference();
			byte[] cla = (byte[])classification.get(i);
			
			for(int x=0; x<ref.length; x++) {
				//for(int y=0; y<ref[0].length; y++) {
					if (mode == cla[x]) {
						//if (mode == 1) {
							l[getRefClass(ref, x)]++;
							all++;
						//}
						/*
						if (mode == 2) {
							for(int c=0; c<numOfClasses; c++) {
								r[c]+= cl[c]; 
							}
						}
/*						if () {
							// f0 is present
							if (mode == 1) {
								ret++;
							} else {
								not++;
							}
						} else {
							if (mode == 2) {
								ret++;
							} else {
								not++;
							}
						}
*/
					}
				//}
			}
		}
		/*
		if (mode == 1) {
			return ret/(ret+not);
		} else {
			return 1-(ret/(ret+not));
		}
		*/
		for(int c=0; c<numOfClasses; c++) {
			l[c] /= (float)all;
		}
		return l;
	}

	/**
	 * Returns the class of a frame in ref.
	 * 
	 * @param ref
	 * @param x
	 * @return
	 */
	public abstract int getRefClass(byte[][] ref, int x);
	
	/**
	 * Saves the current debugTree for visualization of the nodes decisions 
	 * at the last classification run.
	 * 
	 * @param filename
	 * @throws Exception 
	 */
	public void saveDebugTree() throws Exception {
		saveDebugTreeRec(tree, 0, 0);
	}
	
	/**
	 * Saves a visualization image for each nodes decision in the last classification run.
	 * 
	 * @param node
	 * @param depth
	 * @param mode
	 * @throws Exception
	 */
	private void saveDebugTreeRec(Node node, int depth, int mode) throws Exception {
		if (params.saveNodeClassifications <= depth-1) return;
		if (node.isLeaf())  return;
		
		String nf = params.workingFolder + File.separator + "T" + num + "_Classification_Depth" + depth + "_mode_" + mode + "_id_" + node.id + ".png";
		if (node == null || node.debugObject == null) {
			System.out.println("ERROR: Could not save image, node: " + node + ", debugTree: " + node.debugObject);
			return;
		}
		saveDebugTree(node, nf);
		saveDebugTreeRec(node.left, depth+1, 1);
		saveDebugTreeRec(node.right, depth+1, 2);
	}

	/**
	 * Saves the current debugTree for visualization of the nodes decisions 
	 * at the last classification run.
	 * 
	 * @param filename
	 * @throws Exception 
	 */
	private void saveDebugTree(Node node, String filename) throws Exception {
		int[][] debugTree = (int[][])node.debugObject;
		ArrayToImage img = new ArrayToImage(debugTree.length, debugTree[0].length);
		int[][] l = new int[debugTree.length][debugTree[0].length];
		int[][] r = new int[debugTree.length][debugTree[0].length];
		for (int i=0; i<l.length; i++) {
			for (int j=0; j<l[0].length; j++) {
				if (debugTree[i][j] == 1) l[i][j] = 1;
				if (debugTree[i][j] == 2) r[i][j] = 1;
			}
		}
		img.add(l, new Color(0,255,0), null, 0);
		img.add(r, new Color(255,0,0), null, 0);
		img.save(new File(filename));
	}

}
