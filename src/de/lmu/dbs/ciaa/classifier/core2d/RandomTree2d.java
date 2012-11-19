package de.lmu.dbs.ciaa.classifier.core2d;

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
public class RandomTree2d extends RandomTree {

	/**
	 * Creates a tree (main constructor).
	 * 
	 * @throws Exception 
	 * 
	 */
	public RandomTree2d(ForestParameters params, int num) throws Exception {
		super(params, num);
	}
	
	/**
	 * Creates a tree (main constructor).
	 * 
	 * @throws Exception 
	 * 
	 */
	public RandomTree2d(ForestParameters params, int numOfClasses, int num, Logfile log) throws Exception {
		super(params, numOfClasses, num, log);
		this.infoGain = new Statistic();
	}
	
	/**
	 * Creates a tree instance for recursion into a new thread. The arguments are just used to transport
	 * the arguments of growRec to the new thread. See method growRec source code.
	 * 
	 * @throws Exception 
	 * 
	 */
	public RandomTree2d(RandomTree root, Sampler<Dataset> sampler, List<Object> classification, long count, Node node, int mode, int depth, int maxDepth) throws Exception {
		this(root.params, root.numOfClasses, root.num, root.log);
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
		return classifyRec((byte[][])data, tree, 0, 0, x, y);
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
	protected float[] classifyRec(final byte[][] data, final Node node, int mode, int depth, final int x, final int y) throws Exception {
		if (node.isLeaf()) {
			return node.probabilities;
		} else {
			if (params.saveNodeClassifications > depth-1 && node.debugObject == null) node.debugObject = new int[data.length][data[0].length]; // TMP
			
			if (((Feature2d)node.feature).evaluate(data, x, y) >= node.feature.threshold) {
				if (params.saveNodeClassifications > depth-1) ((int[][])node.debugObject)[x][y] = 1;
				return classifyRec(data, node.left, 1, depth+1, x, y);
			} else {
				if (params.saveNodeClassifications > depth-1) ((int[][])node.debugObject)[x][y] = 2;
				return classifyRec(data, node.right, 2, depth+1, x, y);
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
		Feature2d feature = (Feature2d)node.feature;
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
						if (feature.evaluate(data, x, y) >= feature.threshold) {
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
		int numOfFeatures = paramSet.size();
		Feature2d[] features = new Feature2d[numOfFeatures];
		for(int i=0; i<paramSet.size(); i++) {
			features[i] = (Feature2d)paramSet.get(i);
		}
		float[][] thresholdsArray = (float[][])thresholds;
		int poolSize = sampler.getPoolSize();
		int tcpf = params.thresholdCandidatesPerFeature;
		
		for(int poolIndex=0; poolIndex<poolSize; poolIndex++) {
			// Each dataset...load data and reference
			TreeDataset dataset = (TreeDataset)sampler.get(poolIndex);
			byte[][] data = (byte[][])dataset.getData();
			byte[][] ref = (byte[][])dataset.getReference();
			byte[][] cla = (byte[][])classification.get(poolIndex);
			
			// get feature results 
			for(int x=0; x<data.length; x++) {
				for(int y=minIndex; y<=maxIndex; y++) {
					for(int k=0; k<numOfFeatures; k++) {
						if (mode == cla[x][y]) { // Is that point in the training set for this node?
							float ev = features[k].evaluate(data, x, y);
							for(int g=0; g<tcpf; g++) {
								if (ev >= thresholdsArray[k][g]) {
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
						l[ref[x][y]]++;
						all++;
					}
				}
			}
		}
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
	public RandomTree2d getInstance(RandomTree root, Sampler<Dataset> sampler, List<Object> classification, long count, Node node, int mode, int depth, int maxDepth) throws Exception {
		return new RandomTree2d(root, sampler, classification, count, node, mode, depth, maxDepth);
	}

	/**
	 * Returns a new instance of the tree.
	 * 
	 */
	@Override
	public RandomTree2d getInstance(ForestParameters params, int num) throws Exception {
		return new RandomTree2d(params, num);
	}

}
