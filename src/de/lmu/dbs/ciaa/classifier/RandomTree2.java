package de.lmu.dbs.ciaa.classifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.lmu.dbs.ciaa.classifier.features.*;
import de.lmu.dbs.ciaa.util.Logfile;
import de.lmu.dbs.ciaa.util.LogScale;
import de.lmu.dbs.ciaa.util.Scale;
import de.lmu.dbs.ciaa.util.Statistic;
import de.lmu.dbs.ciaa.util.Statistic2d;

/**
 * Represents one random decision tree, with pixelwise classification.
 * 
 * @author Thomas Weber
 *
 */
public class RandomTree2 extends Tree {

	private DecimalFormat decimalFormat = new DecimalFormat("#0.000000");
	
	/**
	 * Creates a tree instance for recursion into a new thread. The arguments are just used to transport
	 * the arguments of growRec to the new thread. See method growRec source code.
	 * 
	 * @throws Exception 
	 * 
	 */
	public RandomTree2(ForestParameters params, Tree root, Sampler<Dataset> sampler, List<byte[][]> classification, long count, Node node, int mode, int depth, int maxDepth, int num, Logfile log) throws Exception {
		this(params, num, log);
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
	 * Creates a tree (main constructor).
	 * 
	 * @throws Exception 
	 * 
	 */
	public RandomTree2(ForestParameters params, int num, Logfile log) throws Exception {
		super(num, log);
		params.check();
		this.params = params;
		this.infoGain = new Statistic();
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
	public float classify(final byte[][] data, final int x, final int y) throws Exception {
		return classifyRec(data, tree, 0, 0, x, y);
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
	protected float classifyRec(final byte[][] data, final Node node, int mode, int depth, final int x, final int y) throws Exception {
		if (node.isLeaf()) {
			return node.probability;
		} else {
			if (params.saveNodeClassifications > depth-1 && node.debugTree == null) node.debugTree = new int[data.length][data[0].length]; // TMP
			
			if (node.feature.evaluate(data, x, y) >= node.feature.threshold) {
				if (params.saveNodeClassifications > depth-1) node.debugTree[x][y] = 1;
				return classifyRec(data, node.left, 1, depth+1, x, y);
			} else {
				if (params.saveNodeClassifications > depth-1) node.debugTree[x][y] = 2;
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
	protected List<byte[][]> getPreClassification(Sampler<Dataset> sampler) throws Exception {
		List<byte[][]> classification = new ArrayList<byte[][]>(); // Classification arrays for each dataset in the sampler, same index as in sampler
		int vpf = (int)(params.percentageOfRandomValuesPerFrame * params.frequencies.length); // values per frame
		for(int i=0; i<sampler.getPoolSize(); i++) {
			TreeDataset d = (TreeDataset)sampler.get(i);
			byte[][] cl = d.getInitialClassification(vpf); // Drop some of the values by classifying them to -1
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
	 * Grows the tree. 
	 * <br><br>
	 * Attention: If multithreading is activated, you have to care about 
	 * progress before proceeding with testing etc., i.e. you can use the 
	 * isGrown() method for that purpose.
	 * 
	 * @param sampler contains the whole data to train the tree.
	 * @param mode 0: root node (no preceeding classification), 1: left, 2: right; -1: out of bag
	 * @throws Exception
	 */
	public void grow(final Sampler<Dataset> sampler, final int maxDepth) throws Exception {
		if (log == null) throw new Exception("Tree " + num + " has no logging object");
		
		// List training data files and parameters in log
		logMeta(sampler);
		
		// Preclassify and grow
		List<byte[][]> classification = getPreClassification(sampler);
		System.out.println("Finished pre-classification for tree " + num + ", start growing...");
		growRec(this, sampler, classification, Long.MAX_VALUE, tree, 0, 0, maxDepth, true);
	}

	/**
	 * Internal: Grows the tree.
	 * 
	 * @param sampler contains the whole data to train the tree.
	 * @param mode 0: root node (no preceeding classification), 1: left, 2: right; -1: out of bag
	 * @param multithreading this is used to disable the threading part, if called from the run method. 
	 *        Otherwise, an infinite loop would happen with multithreading.
	 * @throws Exception 
	 */
	protected void growRec(Tree root, final Sampler<Dataset> sampler, List<byte[][]> classification, final long count, final Node node, final int mode, final int depth, final int maxDepth, boolean multithreading) throws Exception {
		if (params.maxNumOfNodeThreads > 0) {
			synchronized (root.forest) { 
				if (multithreading && (root.forest.getThreadsActive() < params.maxNumOfNodeThreads)) {
					// Start an "anonymous" RandomTree instance to calculate this method. Results have to be 
					// watched with the isGrown method of the original RandomTree instance.
					Tree t = new RandomTree2(params, root, sampler, classification, count, node, mode, depth, maxDepth, num, log);
					root.incThreadsActive();
					t.start();
					return;
				}
			}
		}
		
		// TMP
		String pre = "T" + root.num + ":  ";
		for(int i=0; i<depth; i++) pre+="-  ";

		// See if we exceeded max recursion depth
		if (depth >= maxDepth) {
			// Make it a leaf node
			node.probability = calculateLeaf(sampler, classification, mode, depth);
			if (params.logNodeInfo) log.write(pre + " Mode " + mode + " leaf; Probability " + node.probability);
			return;
		}
		
		// Get random feature parameter sets
		List<Feature> paramSet = params.featureFactory.getRandomFeatureSet(params);
		int numOfFeatures = paramSet.size();

		// Generate random thresholds for each feature param set
		float[][] thresholds = new float[numOfFeatures][];
		for(int i=0; i<thresholds.length; i++) {
			thresholds[i] = params.featureFactory.getRandomThresholds(params.thresholdCandidatesPerFeature);
		}

		// Evaluate the features
		int numOfClasses = getNumOfClasses();
		long[][][] countClassesLeft = new long[numOfFeatures][params.thresholdCandidatesPerFeature][numOfClasses];
		long[][][] countClassesRight = new long[numOfFeatures][params.thresholdCandidatesPerFeature][numOfClasses];
		evaluateFeatures(sampler, paramSet, classification, count, mode, thresholds, countClassesLeft, countClassesRight, node, depth);		

		// Calculate info gain upon each combination of feature/threshold 
		double[][] gain = getGainsByEntropy(paramSet, countClassesLeft, countClassesRight);
		
		// Get maximum gain feature/threshold combination
		double max = -Double.MAX_VALUE;
		int winner = 0;
		int winnerThreshold = 0;

		double min = Double.MAX_VALUE; // TMP just used for stats
		Statistic2d gainStat = new Statistic2d(); // TMP gain statistics for gain/threshold diagrams
		
		for(int i=0; i<numOfFeatures; i++) {
			for(int j=0; j<params.thresholdCandidatesPerFeature; j++) {
				if(gain[i][j] > max) {
					max = gain[i][j];
					winner = i;
					winnerThreshold = j;
				}
				if (params.logNodeInfo && gain[i][j] < min) min = gain[i][j]; // TMP just used for stats
				if (params.saveGainThresholdDiagrams > depth) gainStat.add(thresholds[i][j], gain[i][j]); // TMP gain statistics for gain/threshold diagrams
			}
		}
		
		// Debug //////////////////////////////////////////
		root.infoGain.add(gain[winner][winnerThreshold]);
		if (params.logNodeInfo) {
			log.write(pre + "------------------------");
			long silenceLeftW = countClassesLeft[winner][winnerThreshold][1]; 
			long noteLeftW = countClassesLeft[winner][winnerThreshold][0];
			long silenceRightW = countClassesRight[winner][winnerThreshold][1]; 
			long noteRightW = countClassesRight[winner][winnerThreshold][0];
			log.write(pre + "Finished node " + node.id + " at Depth " + depth + ", Mode: " + mode, System.out);
			log.write(pre + "Winner: " + winner + " Thr Index: " + winnerThreshold + "; Information gain: " + decimalFormat.format(gain[winner][winnerThreshold]));
			log.write(pre + "Left note: " + noteLeftW + ", silence: " + silenceLeftW + ", sum: " + (silenceLeftW+noteLeftW));
			log.write(pre + "Right note: " + noteRightW + ", silence: " + silenceRightW + ", sum: " + (silenceRightW+noteRightW));
			log.write(pre + "Gain min: " + decimalFormat.format(min) + ", max: " + decimalFormat.format(max));
			
			for(int i=0; i<thresholds[winner].length; i++) {
				log.write(pre + "Thr. " + i + ": " + decimalFormat.format(thresholds[winner][i]) + ", Gain: " + decimalFormat.format(gain[winner][i]));
			}
			//*/
			float tmin = Float.MAX_VALUE;
			float tmax = -Float.MAX_VALUE;
			for(int i=0; i<thresholds[winner].length; i++) {
				if (thresholds[winner][i] > tmax) tmax = thresholds[winner][i];
				if (thresholds[winner][i] < tmin) tmin = thresholds[winner][i];
			}
			log.write(pre + "Threshold min: " + decimalFormat.format(tmin) + "; max: " + decimalFormat.format(tmax));
			if (thresholds[winner][winnerThreshold] == tmin) log.write(pre + "WARNING: Threshold winner is min: Depth " + depth + ", mode: " + mode + ", thr: " + thresholds[winner][winnerThreshold], System.out);
			if (thresholds[winner][winnerThreshold] == tmax) log.write(pre + "WARNING: Threshold winner is max: Depth " + depth + ", mode: " + mode + ", thr: " + thresholds[winner][winnerThreshold], System.out);
		}
		// Save gain/threshold diagrams for each grown node
		if (params.saveGainThresholdDiagrams > depth) {
			String nf = "T" + num + "_GainDistribution_Depth" + depth + "_mode_" + mode + "_id_" + node.id + ".png";
			Scale s = new LogScale(10);
			gainStat.saveDistributionImage(params.workingFolder + File.separator + nf, 400, 400, s);
		}
		//////////////////////////////////////////////////
		
		// See in info gain is sufficient:
		if (gain[winner][winnerThreshold] > params.entropyThreshold) {
			// Yes, save feature and continue recursion
			node.feature = paramSet.get(winner);
			node.feature.threshold = thresholds[winner][winnerThreshold];
			if (params.logNodeInfo) log.write(pre + "Feature threshold: " + node.feature.threshold + "; Coeffs: " + node.feature);
		} else {
			// No, make leaf and return
			node.probability = calculateLeaf(sampler, classification, mode, depth);
			//node.probabilities = calculateLeaf(sampler, classification, mode, depth);
			if (params.logNodeInfo) log.write(pre + "Mode " + mode + " leaf; Probability " + node.probability);
			return;
		}
		
		// Split values by winner feature for deeper branches
		List<byte[][]> classificationNext = new ArrayList<byte[][]>(sampler.getPoolSize());
		int poolSize = sampler.getPoolSize();
		long countLeft = 0;
		long countRight = 0;
		for(int i=0; i<poolSize; i++) {
			TreeDataset dataset = (TreeDataset)sampler.get(i);
			byte[][] data = dataset.getData();
			byte[][] cla = classification.get(i);
			byte[][] claNext = new byte[data.length][params.frequencies.length];
			for(int x=0; x<data.length; x++) {
				for(int y=0; y<params.frequencies.length; y++) {
					if (mode == cla[x][y]) {
						if (node.feature.evaluate(data, x, y) >= node.feature.threshold) {
							claNext[x][y] = 1; // Left
							countLeft++;
						} else {
							claNext[x][y] = 2; // Right
							countRight++;
						}
					}
				}
			}
			classificationNext.add(claNext);
		}
		
		// Flush log changes to preserve them if crashes happen
		log.flush();
		
		// Recursion to left and right
		node.left = new Node();
		growRec(root, sampler, classificationNext, countLeft, node.left, 1, depth+1, maxDepth, true);

		node.right = new Node();
		growRec(root, sampler, classificationNext, countRight, node.right, 2, depth+1, maxDepth, true);
	}
	
	/**
	 * Returns the number of classification classes.
	 * 
	 * @return
	 */
	protected int getNumOfClasses() {
		return 2;
	}
	
	/**
	 * Evaluates a couple of features with a couple of thresholds. This
	 * is the most CPU intensive part of the tree training algorithm.
	 * 
	 * @param sampler
	 * @param paramSet
	 * @param classification
	 * @param mode
	 * @param thresholds
	 * @param countClassesLeft
	 * @param countClassesRight
	 * @throws Exception
	 */
	protected void evaluateFeatures(Sampler<Dataset> sampler, List<Feature> paramSet, List<byte[][]> classification, long count, int mode, float[][] thresholds, long[][][] countClassesLeft, long[][][] countClassesRight, Node node, int depth) throws Exception {
		if (count < params.minEvalThreadCount) {
			// Not much values, no multithreading
			System.out.println("  [T" + num + ", Id " + node.id + ", Depth " + depth + ": Just " + count + " values, no multithreading]");
			RandomTree2Worker w = new RandomTree2Worker(params, 0, sampler.getPoolSize()-1, sampler, paramSet, classification, mode, thresholds, countClassesLeft, countClassesRight);
			w.evaluateFeatures();
			return;
		}
		
		int poolSize = sampler.getPoolSize();
		RandomTree2Worker[] workers = new RandomTree2Worker[params.numOfWorkerThreadsPerNode];
		int ipw = poolSize / workers.length;
		for(int i=0; i<workers.length; i++) {
			int min = i*ipw;
			int max = min + ipw - 1;
			if (max >= poolSize) max = poolSize-1;
			//System.out.println(min + " to " + max);
			workers[i] = new RandomTree2Worker(params, min, max, sampler, paramSet, classification, mode, thresholds, countClassesLeft, countClassesRight);
			workers[i].start();
		}
		while(true) {
			try {
				Thread.sleep(params.threadWaitTime);
			} catch (InterruptedException e) {
				System.out.println("[Wait interrupted by VM, continuing...]");
			}
			boolean ret = true;
			int cnt = 0;
			for(int i=0; i<workers.length; i++) {
				if (!workers[i].finished) {
					ret = false;
					cnt++;
				}
			}
			if (params.debugThreadPolling) {
				// Debug output
				System.out.print(timeStampFormatter.format(new Date()) + ": T" + num + ", Thrds: " + cnt + ", Id " + node.id + ", Depth " + depth + "; ");
				System.out.println("Heap: " + Math.round((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()) / (1024.0*1024.0)) + " MB");
			}
			if (ret) break;
		}
		System.out.print(timeStampFormatter.format(new Date()) + ": All workers done");
	}
	
	
	/**
	 * Info gain calculation. Uses an error-unfriendly [0,oo] algo. Stabilizes at good thrs.
	 * 
	 * @return
	 */
	protected double[][] getGains1(List<Feature> paramSet, long[][][] countClassesLeft, long[][][] countClassesRight, double noteRatio) {
		double[][] gain = new double[paramSet.size()][params.thresholdCandidatesPerFeature];
		int numOfFeatures = paramSet.size();
		for(int i=0; i<numOfFeatures; i++) {
			for(int j=0; j<params.thresholdCandidatesPerFeature; j++) {
				double leftGain = ((double)countClassesLeft[i][j][0]/noteRatio) / countClassesLeft[i][j][1];
				double rightGain = countClassesRight[i][j][1] / ((double)countClassesRight[i][j][0]/noteRatio);
				gain[i][j] = leftGain * rightGain;
			}
		}
		return gain;
	}

	/**
	 * Info gain calculation. Uses an error-unfriendly [-oo,2] algo. Stabilizes at good thrs.
	 * 
	 * @return
	 */
	protected double[][] getGains2(List<Feature> paramSet, long[][][] countClassesLeft, long[][][] countClassesRight, double noteRatio) {
		double[][] gain = new double[paramSet.size()][params.thresholdCandidatesPerFeature];
		double note = countClassesLeft[0][0][0] + countClassesRight[0][0][0];
		double silence = countClassesLeft[0][0][1] + countClassesRight[0][0][1];
		int numOfFeatures = paramSet.size();
		for(int i=0; i<numOfFeatures; i++) {
			for(int j=0; j<params.thresholdCandidatesPerFeature; j++) {
				double leftGain = (double)countClassesLeft[i][j][0] / note - (double)countClassesLeft[i][j][1] / silence;
				double rightGain= (double)countClassesRight[i][j][1] / silence - (double)countClassesRight[i][j][0] / note;
				gain[i][j] = leftGain + rightGain;
			}
		}
		return gain;
	}

	/**
	 * Info gain calculation upon shannon entropy, Kinect formula.
	 * 
	 */
	protected double[][] getGainsByEntropy(List<Feature> paramSet, long[][][] countClassesLeft, long[][][] countClassesRight) {
		double[][] gain = new double[paramSet.size()][params.thresholdCandidatesPerFeature];
		// Get overall sum of classes
		int numOfClasses = countClassesLeft[0][0].length;
		long[] classes = new long[numOfClasses];
		long all = 0;
		for(int y=0; y<numOfClasses; y++) {
			classes[y] = countClassesLeft[0][0][y] + countClassesRight[0][0][y];
			all += classes[y];
		}
		// Calculate gains
		double entropyAll = getEntropy(classes);
		int numOfFeatures = paramSet.size();
		for(int i=0; i<numOfFeatures; i++) {
			for(int j=0; j<params.thresholdCandidatesPerFeature; j++) {
				double entropyLeft = getEntropy(countClassesLeft[i][j]);
				double entropyRight = getEntropy(countClassesRight[i][j]);
				long amountLeft = 0;
				long amountRight = 0;
				for(int d=0; d<numOfClasses; d++) {
					amountLeft += countClassesLeft[i][j][d];
					amountRight += countClassesRight[i][j][d];
				}
				gain[i][j] = entropyAll - ((double)amountLeft/all)*entropyLeft - ((double)amountRight/all)*entropyRight;
			}
		}
		return gain;
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
	private float calculateLeaf(final Sampler<Dataset> sampler, List<byte[][]> classification, final int mode, final int depth) throws Exception {
		float ret = 0;
		float not = 0;
		// See how much was judged right
		for(int i=0; i<sampler.getPoolSize(); i++) {
			TreeDataset dataset = (TreeDataset)sampler.get(i);
			byte[][] midi = dataset.getReference();
			byte[][] cla = classification.get(i);
			
			for(int x=0; x<midi.length; x++) {
				for(int y=0; y<midi[0].length; y++) {
					if (mode == cla[x][y]) { 
						if (midi[x][y] > 0) {
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
					}
				}
			}
		}
		if (mode == 1) {
			return ret/(ret+not);
		} else {
			return 1-(ret/(ret+not));
		}
	}

	/**
	 * Calculates shannon entropy for binary alphabet (two possible values),  
	 * while a and b represent the count of each of the two "letters". 
	 * 
	 * @param a
	 * @param b
	 * @return
	 *
	public static double getEntropy(final long a, final long b) {
		long all = a+b;
		if(all <= 0) return 0;
		double pa = (double)a/all;
		double pb = (double)b/all;
		return - pa * (Math.log(pa)/LOG2) - pb * (Math.log(pb)/LOG2);
	}

	/**
	 * Calculates shannon entropy. 
	 *  
	 * 
	 * @param counts delivers the counts for each "letter" of the alphabet. 
	 * @return
	 */
	public static double getEntropy(final long[] counts) {
		long all = 0;
		for(int i=0; i<counts.length; i++) {
			all += counts[i];
		}
		if(all <= 0) return 0;
		
		double ret = 0;
		for(int i=0; i<counts.length; i++) {
			if (counts[i] > 0) {
				double p = (double)counts[i]/all;
				ret -= p * (Math.log(p)/LOG2);
			}
		}
		return ret;
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
		if (node == null || node.debugTree == null) {
			System.out.println("ERROR: Could not save image, node: " + node + ", debugTree: " + node.debugTree);
			return;
		}
		node.saveDebugTree(nf);
		saveDebugTreeRec(node.left, depth+1, 1);
		saveDebugTreeRec(node.right, depth+1, 2);
	}

	/**
	 * Write some params to log file.
	 * @throws Exception 
	 * 
	 */
	private void logMeta(Sampler<Dataset> sampler) throws Exception {
		String lst = "";
		for(int o=0; o<sampler.getPoolSize(); o++) {
			TreeDataset d = (TreeDataset)sampler.get(o);
			lst += "Dataset " + o + ": " + d.getDataFile().getAbsolutePath() + " (Reference: " + d.getReferenceFile().getAbsolutePath() + ")\n";
		}
		log.write("Parameters: \n" + params.toString());
		log.write("Training data for tree " + num + ":\n" + lst);
		log.write("\n");
	}
	
	/**
	 * Saves the tree to a file.
	 * 
	 * @param file
	 * @throws Exception
	 */
	public void save(final String filename) throws Exception {
		FileOutputStream fout = new FileOutputStream(filename);
		ObjectOutputStream oos = new ObjectOutputStream(fout);   
		oos.writeObject(tree);
		oos.close();
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
	public static RandomTree2 load(ForestParameters params, final String filename, final int num) throws Exception {
		FileInputStream fin = new FileInputStream(filename);
		ObjectInputStream ois = new ObjectInputStream(fin);
		RandomTree2 ret = new RandomTree2(params, num, null);
		ret.tree = (Node)ois.readObject();
		ois.close();
		return ret;
	}

}
