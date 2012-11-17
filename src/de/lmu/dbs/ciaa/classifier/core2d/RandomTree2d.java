package de.lmu.dbs.ciaa.classifier.core2d;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import de.lmu.dbs.ciaa.classifier.core.Dataset;
import de.lmu.dbs.ciaa.classifier.core.ForestParameters;
import de.lmu.dbs.ciaa.classifier.core.Node;
import de.lmu.dbs.ciaa.classifier.core.Sampler;
import de.lmu.dbs.ciaa.classifier.core.RandomTree;
import de.lmu.dbs.ciaa.classifier.core.TreeDataset;
import de.lmu.dbs.ciaa.util.Logfile;
import de.lmu.dbs.ciaa.util.Statistic;

/**
 * Represents one random decision tree. Extend this to create randomized decision
 * trees for various applications.
 * 
 * @author Thomas Weber
 *
 */
public abstract class RandomTree2d extends RandomTree {

	/**
	 * Creates a tree (main constructor).
	 * 
	 * @throws Exception 
	 * 
	 */
	public RandomTree2d(ForestParameters params, int numOfClasses, int num, Logfile log) throws Exception {
		super(new Node2d(), numOfClasses, num, log);
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
	public RandomTree2d(ForestParameters params, RandomTree root, Sampler<Dataset> sampler, List<Object> classification, long count, Node node, int mode, int depth, int maxDepth, int num, Logfile log) throws Exception {
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
	public float[] classify(final Object data, final int x, final int y) throws Exception {
		return classifyRec((byte[][])data, (Node2d)tree, 0, 0, x, y);
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
	protected float[] classifyRec(final byte[][] data, final Node2d node, int mode, int depth, final int x, final int y) throws Exception {
		if (node.isLeaf()) {
			return node.probabilities;
		} else {
			if (params.saveNodeClassifications > depth-1 && node.debugTree == null) node.debugTree = new int[data.length][data[0].length]; // TMP
			
			if (node.feature.evaluate(data, x, y) >= node.feature.threshold) {
				if (params.saveNodeClassifications > depth-1) node.debugTree[x][y] = 1;
				return classifyRec(data, (Node2d)node.left, 1, depth+1, x, y);
			} else {
				if (params.saveNodeClassifications > depth-1) node.debugTree[x][y] = 2;
				return classifyRec(data, (Node2d)node.right, 2, depth+1, x, y);
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
		int vpf = (int)(params.percentageOfRandomValuesPerFrame * params.frequencies.length); // values per frame
		for(int i=0; i<sampler.getPoolSize(); i++) {
			TreeDataset d = (TreeDataset)sampler.get(i);
			byte[][] cl = (byte[][])d.getInitialClassification(vpf); // Drop some of the values by classifying them to -1
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
		Node2d node2d = (Node2d)node;
		List<Object> classificationNext = new ArrayList<Object>(sampler.getPoolSize());
		int poolSize = sampler.getPoolSize();
		for(int i=0; i<poolSize; i++) {
			TreeDataset dataset = (TreeDataset)sampler.get(i);
			byte[][] data = (byte[][])dataset.getData();
			byte[][] cla = (byte[][])classification.get(i);
			byte[][] claNext = new byte[data.length][params.frequencies.length];
			for(int x=0; x<data.length; x++) {
				for(int y=0; y<params.frequencies.length; y++) {
					if (mode == cla[x][y]) {
						if (node2d.feature.evaluate(data, x, y) >= node2d.feature.threshold) {
							claNext[x][y] = 1; // Left
							counts[0]++;
						} else {
							claNext[x][y] = 2; // Right
							counts[1]++;
						}
					}
				}
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
		List<Feature2d> paramSetC = new ArrayList<Feature2d>();
		for(int i=0; i<paramSet.size(); i++) {
			paramSetC.add((Feature2d)paramSet.get(i));
		}
		int numOfFeatures = paramSet.size();
		int poolSize = sampler.getPoolSize();
		float[][] thresholdsC = (float[][])thresholds;
		for(int poolIndex=0; poolIndex<poolSize; poolIndex++) {
			// Each dataset...load data and reference
			TreeDataset dataset = (TreeDataset)sampler.get(poolIndex);
			byte[][] data = (byte[][])dataset.getData();
			byte[][] ref = (byte[][])dataset.getReference();
			byte[][] cla = (byte[][])classification.get(poolIndex);
			
			// get feature results 
			for(int x=0; x<data.length; x++) {
				for(int y=minIndex; y<=maxIndex; y++) {
					// Each random value from the subframe
					if (mode == cla[x][y]) { // Is that point in the training set for this node?
						for(int k=0; k<numOfFeatures; k++) {
							// Each featureset candidate...
							float ev = paramSetC.get(k).evaluate(data, x, y);
							for(int g=0; g<params.thresholdCandidatesPerFeature; g++) {
								if (ev >= thresholdsC[k][g]) {
									// Left
									countClassesLeft[k][g][ref[x][y]]++;
								} else {
									// Right
									countClassesRight[k][g][ref[x][y]]++;
								}
							}
						}
					}
				}
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
			byte[][] cla = (byte[][])classification.get(i);
			
			for(int x=0; x<ref.length; x++) {
				for(int y=0; y<ref[0].length; y++) {
					if (mode == cla[x][y]) {
						//if (mode == 1) {
							l[ref[x][y]]++;
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
				}
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
		Node2d node2d = (Node2d)node;
		
		String nf = params.workingFolder + File.separator + "T" + num + "_Classification_Depth" + depth + "_mode_" + mode + "_id_" + node.id + ".png";
		if (node == null || node2d.debugTree == null) {
			System.out.println("ERROR: Could not save image, node: " + node + ", debugTree: " + node2d.debugTree);
			return;
		}
		node2d.saveDebugTree(nf);
		saveDebugTreeRec((Node2d)node.left, depth+1, 1);
		saveDebugTreeRec((Node2d)node.right, depth+1, 2);
	}

	/**
	 * Loads a tree from file and returns it.
	 * 
	 * TODO: Save params with forest
	 * 
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	public void load(final String filename) throws Exception {
		FileInputStream fin = new FileInputStream(filename);
		ObjectInputStream ois = new ObjectInputStream(fin);
		tree = (Node2d)ois.readObject();
		ois.close();
	}

}
