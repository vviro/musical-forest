package de.lmu.dbs.ciaa.classifier.core2d;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.lmu.dbs.ciaa.classifier.Dataset;
import de.lmu.dbs.ciaa.classifier.ForestParameters;
import de.lmu.dbs.ciaa.classifier.Sampler;
import de.lmu.dbs.ciaa.classifier.features.*;
import de.lmu.dbs.ciaa.util.ArrayUtils;
import de.lmu.dbs.ciaa.util.Logfile;
import de.lmu.dbs.ciaa.util.LogScale;
import de.lmu.dbs.ciaa.util.Scale;
import de.lmu.dbs.ciaa.util.Statistic;
import de.lmu.dbs.ciaa.util.Statistic2d;

/**
 * Represents one random decision tree. Extend this to create randomized decision
 * trees for various applications.
 * 
 * @author Thomas Weber
 *
 */
public abstract class RandomTree2d extends Tree2d {

	private DecimalFormat decimalFormat = new DecimalFormat("#0.000000");
	
	/**
	 * Creates a tree (main constructor).
	 * 
	 * @throws Exception 
	 * 
	 */
	public RandomTree2d(ForestParameters params, int numOfClasses, int num, Logfile log) throws Exception {
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
	public RandomTree2d(ForestParameters params, Tree2d root, Sampler<Dataset> sampler, List<byte[][]> classification, long count, Node2d node, int mode, int depth, int maxDepth, int num, Logfile log) throws Exception {
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
	public abstract RandomTree2d getInstance(ForestParameters params, Tree2d root, Sampler<Dataset> sampler, List<byte[][]> classification, long count, Node2d node, int mode, int depth, int maxDepth, int num, Logfile log) throws Exception;
	
	/**
	 * Returns a new instance of the tree.
	 * 
	 * @param params
	 * @param num
	 * @param log
	 * @return
	 * @throws Exception
	 */
	public abstract RandomTree2d getInstance(ForestParameters params, int num, Logfile log) throws Exception;

	/**
	 * Provides the possibility to add tree specific log output per node.
	 * 
	 * @param pre
	 * @param countClassesLeft
	 * @param countClassesRight
	 * @param winner
	 * @param winnerThreshold
	 * @throws Exception
	 */
	public abstract void logAdditional(String pre, long[][][] countClassesLeft, long[][][] countClassesRight, int winner, int winnerThreshold) throws Exception;
	
	/**
	 * Returns the classification of the tree at a given value in data: data[x][y]
	 * 
	 * @param data
	 * @param x
	 * @param y
	 * @return
	 * @throws Exception
	 */
	public float[] classify(final byte[][] data, final int x, final int y) throws Exception {
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
	protected float[] classifyRec(final byte[][] data, final Node2d node, int mode, int depth, final int x, final int y) throws Exception {
		if (node.isLeaf()) {
			return node.probabilities;
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
			TreeDataset2d d = (TreeDataset2d)sampler.get(i);
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
	protected void growRec(Tree2d root, final Sampler<Dataset> sampler, List<byte[][]> classification, final long count, final Node2d node, final int mode, final int depth, final int maxDepth, boolean multithreading) throws Exception {
		if (params.maxNumOfNodeThreads > 0) {
			synchronized (root.forest) { 
				if (multithreading && (root.forest.getThreadsActive() < params.maxNumOfNodeThreads)) {
					// Start an "anonymous" RandomTree instance to calculate this method. Results have to be 
					// watched with the isGrown method of the original RandomTree instance.
					Tree2d t = getInstance(params, root, sampler, classification, count, node, mode, depth, maxDepth, num, log);
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
			node.probabilities = calculateLeaf(sampler, classification, mode, depth);
			if (params.logNodeInfo) log.write(pre + " Mode " + mode + " leaf; Probabilities " + ArrayUtils.toString(node.probabilities, true));
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
		//int numOfClasses = getNumOfClasses();
		long[][][] countClassesLeft = new long[numOfFeatures][params.thresholdCandidatesPerFeature][numOfClasses];
		long[][][] countClassesRight = new long[numOfFeatures][params.thresholdCandidatesPerFeature][numOfClasses];
		evaluateFeaturesThreads(sampler, paramSet, classification, count, mode, thresholds, countClassesLeft, countClassesRight, node, depth);		

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
			// General node info
			log.write(pre + "------------------------");
			log.write(pre + "Finished node " + node.id + " at Depth " + depth + ", Mode: " + mode, System.out);
			log.write(pre + "Winner: " + winner + " Thr Index: " + winnerThreshold + "; Information gain: " + decimalFormat.format(gain[winner][winnerThreshold]));
			log.write(pre + "Gain min: " + decimalFormat.format(min) + ", max: " + decimalFormat.format(max));
			float tmin = Float.MAX_VALUE;
			float tmax = -Float.MAX_VALUE;
			for(int i=0; i<thresholds[winner].length; i++) {
				if (thresholds[winner][i] > tmax) tmax = thresholds[winner][i];
				if (thresholds[winner][i] < tmin) tmin = thresholds[winner][i];
			}
			log.write(pre + "Threshold min: " + decimalFormat.format(tmin) + "; max: " + decimalFormat.format(tmax));
			if (thresholds[winner][winnerThreshold] == tmin) log.write(pre + "WARNING: Threshold winner is min: Depth " + depth + ", mode: " + mode + ", thr: " + thresholds[winner][winnerThreshold], System.out);
			if (thresholds[winner][winnerThreshold] == tmax) log.write(pre + "WARNING: Threshold winner is max: Depth " + depth + ", mode: " + mode + ", thr: " + thresholds[winner][winnerThreshold], System.out);

			// Tree specific log entries
			logAdditional(pre, countClassesLeft, countClassesRight, winner, winnerThreshold);

			// Thresholds
			for(int i=0; i<thresholds[winner].length; i++) {
				log.write(pre + "Thr. " + i + ": " + decimalFormat.format(thresholds[winner][i]) + ", Gain: " + decimalFormat.format(gain[winner][i]));
			}
			//*/
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
			node.probabilities = calculateLeaf(sampler, classification, mode, depth);
			//node.probabilities = calculateLeaf(sampler, classification, mode, depth);
			if (params.logNodeInfo) log.write(pre + "Mode " + mode + " leaf; Probabilities: " + ArrayUtils.toString(node.probabilities, true));
			return;
		}
		
		// Split values by winner feature for deeper branches
		List<byte[][]> classificationNext = new ArrayList<byte[][]>(sampler.getPoolSize());
		int poolSize = sampler.getPoolSize();
		long countLeft = 0;
		long countRight = 0;
		for(int i=0; i<poolSize; i++) {
			TreeDataset2d dataset = (TreeDataset2d)sampler.get(i);
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
		node.left = new Node2d();
		growRec(root, sampler, classificationNext, countLeft, node.left, 1, depth+1, maxDepth, true);

		node.right = new Node2d();
		growRec(root, sampler, classificationNext, countRight, node.right, 2, depth+1, maxDepth, true);
	}
	
	/**
	 * Evaluates a couple of features with a couple of thresholds. This
	 * is the most CPU intensive part of the tree training algorithm.
	 * This method just controls the thread behaviour of feature evaluation.
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
	protected void evaluateFeaturesThreads(Sampler<Dataset> sampler, List<Feature> paramSet, List<byte[][]> classification, long count, int mode, float[][] thresholds, long[][][] countClassesLeft, long[][][] countClassesRight, Node2d node, int depth) throws Exception {
		int numWork = params.frequencies.length; //paramSet.size(); //sampler.getPoolSize();
		
		if (!params.enableEvaluationThreads) {
			System.out.println("T" + num + ", Id " + node.id + ", Depth " + depth + ": Calculate evaluations (not multithreaded)");
			evaluateFeatures(sampler, 0, numWork-1, paramSet, classification, mode, thresholds, countClassesLeft, countClassesRight);
			return;
		}
		
		if (count < params.minEvalThreadCount) {
			// Not much values, no multithreading
			System.out.println("  [T" + num + ", Id " + node.id + ", Depth " + depth + ": Just " + count + " values, no multithreading]");
			evaluateFeatures(sampler, 0, numWork-1, paramSet, classification, mode, thresholds, countClassesLeft, countClassesRight);
			return;
		}
		
		// Create workers for frequency bands
		RandomTreeWorker2d[] workers = new RandomTreeWorker2d[params.numOfWorkerThreadsPerNode];
		int ipw = numWork / workers.length;
		for(int i=0; i<workers.length; i++) {
			int min = i*ipw;
			int max = min + ipw - 1;
			if (max >= numWork) max = numWork-1;
			//System.out.println(min + " to " + max);
			workers[i] = new RandomTreeWorker2d(this, min, max, sampler, paramSet, classification, mode, thresholds, countClassesLeft, countClassesRight);
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
		System.out.println(timeStampFormatter.format(new Date()) + ": T" + num + ": All workers done");
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
			// Each dataset...load data and reference
			TreeDataset2d dataset = (TreeDataset2d)sampler.get(poolIndex);
			byte[][] data = dataset.getData();
			byte[][] ref = dataset.getReference();
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
	 * Info gain calculation. Uses an error-unfriendly [0,oo] algo. Stabilizes at good thrs.
	 * 
	 * @return
	 *
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
	 *
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
	private float[] calculateLeaf(final Sampler<Dataset> sampler, List<byte[][]> classification, final int mode, final int depth) throws Exception {
		//int numOfClasses = this.getNumOfClasses();
		float[] l = new float[numOfClasses];
		long all = 0;
		// See how much was judged right
		for(int i=0; i<sampler.getPoolSize(); i++) {
			TreeDataset2d dataset = (TreeDataset2d)sampler.get(i);
			byte[][] ref = dataset.getReference();
			byte[][] cla = classification.get(i);
			
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
	private void saveDebugTreeRec(Node2d node, int depth, int mode) throws Exception {
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
			TreeDataset2d d = (TreeDataset2d)sampler.get(o);
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
	public void load(final String filename) throws Exception {
		FileInputStream fin = new FileInputStream(filename);
		ObjectInputStream ois = new ObjectInputStream(fin);
		tree = (Node2d)ois.readObject();
		ois.close();
	}

}
