package de.lmu.dbs.ciaa.classifier.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.lmu.dbs.ciaa.util.ArrayUtils;
import de.lmu.dbs.ciaa.util.LogScale;
import de.lmu.dbs.ciaa.util.Logfile;
import de.lmu.dbs.ciaa.util.Scale;
import de.lmu.dbs.ciaa.util.ScheduledThread;
import de.lmu.dbs.ciaa.util.Statistic;
import de.lmu.dbs.ciaa.util.Statistic2d;

/**
 * Base class for trees.
 * 
 * @author Thomas Weber
 *
 */
public abstract class RandomTree extends ScheduledThread {

	/**
	 * Number of classes to divide
	 */
	public int numOfClasses = -1;
	
	/**
	 * Log file for training the tree.
	 */
	public Logfile log = null;
	
	/**
	 * Statistic about the trees information gain.
	 */
	public Statistic infoGain = null;
	
	/**
	 * Index of the tree in its forest.
	 */
	public int num = -1;
	
	/**
	 * Reference to the parent forest (only set in root tree instances, not in threaded recursion).
	 */
	public Forest forest = null;
	
	/**
	 * The actual tree structure
	 */
	protected Node tree = new Node();
	
	/**
	 * The parameter set used to grow the tree
	 */
	public ForestParameters params = null;

	/**
	 * Log to base 2 for better performance
	 */
	public static final double LOG2 = Math.log(2);
	
	/**
	 * Date formatter for debug output.
	 */
	protected SimpleDateFormat timeStampFormatter = new SimpleDateFormat("hh:mm:ss");

	/**
	 * Number formatter for debug output.
	 */
	protected DecimalFormat decimalFormat = new DecimalFormat("#0.000000");
	
	/**
	 * The attributes with prefix "newThread" are used to transport parameters
	 * to new Threads (see growRec source code)
	 */
	protected Sampler<Dataset> newThreadSampler;

	/**
	 * The attributes with prefix "newThread" are used to transport parameters
	 * to new Threads (see growRec source code)
	 */
	protected List<Classification> newThreadClassification;

	/**
	 * The attributes with prefix "newThread" are used to transport parameters
	 * to new Threads (see growRec source code)
	 */
	protected RandomTree newThreadRoot;
	
	/**
	 * The attributes with prefix "newThread" are used to transport parameters
	 * to new Threads (see growRec source code)
	 */
	protected Node newThreadNode;
	
	/**
	 * The attributes with prefix "newThread" are used to transport parameters
	 * to new Threads (see growRec source code)
	 */
	protected int newThreadMode;

	/**
	 * The attributes with prefix "newThread" are used to transport parameters
	 * to new Threads (see growRec source code)
	 */
	protected int newThreadDepth;
	
	/**
	 * The attributes with prefix "newThread" are used to transport parameters
	 * to new Threads (see growRec source code)
	 */
	protected int newThreadMaxDepth;
	
	/**
	 * The attributes with prefix "newThread" are used to transport parameters
	 * to new Threads (see growRec source code)
	 */
	protected long newThreadCount;
	
	/**
	 * Create a tree (as factory).
	 * 
	 * @param num
	 * @param log
	 * @throws Exception 
	 */
	public RandomTree() {
	}

	/**
	 * Create a tree (for loading).
	 * 
	 * @param num
	 * @param log
	 * @throws Exception 
	 */
	public RandomTree(ForestParameters params, int numOfClasses, int num) throws Exception {
		params.check();
		this.params = params;
		this.num = num;
		this.numOfClasses = numOfClasses;
	}

	/**
	 * Create a tree.
	 * 
	 * @param num
	 * @param log
	 * @throws Exception 
	 */
	public RandomTree(ForestParameters params, int numOfClasses, int num, Logfile log) throws Exception {
		this(params, numOfClasses, num);
		this.log = log;
	}

	/**
	 * Returns the amount of jobs to split between the evaluation threads.
	 * 
	 * @return
	 */
	public abstract int getNumOfWork(Sampler<Dataset> sampler, List<Object> paramSet, List<Classification> classification);
	
	/**
	 * Calculates leaf probability.
	 * 
	 * @param sampler
	 * @param mode
	 * @param depth
	 * @return
	 * @throws Exception
	 */
	protected abstract float[] calculateLeaf(final Sampler<Dataset> sampler, List<Classification> classification, final int mode, final int depth) throws Exception;

	/**
	 * Splits the training data set of a node.
	 * 
	 * @param sampler
	 * @param classification
	 * @param mode
	 * @param node
	 * @return
	 * @throws Exception
	 */
	public abstract void splitValues(Sampler<Dataset> sampler, List<Classification> classification, List<Classification> classificationLeft, List<Classification> classificationRight, int mode, Node node, long[] counts) throws Exception;

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
	public abstract RandomTree getInstance(RandomTree root, Sampler<Dataset> sampler, List<Classification> classification, long count, Node node, int mode, int depth, int maxDepth) throws Exception;
	
	/**
	 * Returns a new instance of the tree.
	 * 
	 * @param params
	 * @param num
	 * @param log
	 * @return
	 * @throws Exception
	 */
	public abstract RandomTree getInstance(ForestParameters params, int numOfClasses, int num) throws Exception;

	/**
	 * Returns the classification of the tree at a given value in data: data[x][y]
	 * 
	 * @param data
	 * @param x
	 * @param y
	 * @return
	 * @throws Exception
	 */
	public abstract float[] classify(final Object data, final int x, final int y) throws Exception;
		
	/**
	 * Build first classification array (from bootstrapping samples and random values per sampled frame)
	 * 
	 * @param sampler
	 * @return
	 * @throws Exception 
	 */
	protected abstract List<Classification> getPreClassification(Sampler<Dataset> sampler) throws Exception;

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
	public abstract void evaluateFeatures(Sampler<Dataset> sampler, int minIndex, int maxIndex, List<Object> paramSet, List<Classification> classification, int mode, Object thresholds, long[][][] countClassesLeft, long[][][] countClassesRight) throws Exception;

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
		List<Classification> classification = getPreClassification(sampler);
		System.out.println("Finished pre-classification for tree " + num + ", start growing...");

		long count = 0;
		for(int i=0; i<classification.size(); i++) {
			count+= classification.get(i).getSize();
		}
		
		growRec(this, sampler, classification, count, tree, 0, 0, maxDepth, true);
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
	protected void growRec(RandomTree root, final Sampler<Dataset> sampler, List<Classification> classification, final long count, final Node node, final int mode, final int depth, final int maxDepth, boolean multithreading) throws Exception {
		// TMP
		String pre = "T" + root.num + ":  ";
		for(int i=0; i<depth; i++) pre+="-  ";

		// See if we exceeded max recursion depth
		if (depth >= maxDepth) {
			// Make it a leaf node
			node.probabilities = calculateLeaf(sampler, classification, mode, depth);
			if (params.logNodeInfo) log.write(pre + "Reached max depth, Leaf probabilities " + ArrayUtils.toString(node.probabilities, false));
			return;
		}

		if (count < params.nodeThreadingThreshold && params.maxNumOfNodeThreads > 0) {
				
				synchronized (root.forest.nodeScheduler) { 
					if (multithreading && (root.forest.nodeScheduler.getThreadsAvailable() > 0)) {
						// Start an "anonymous" RandomTree instance to calculate this method. Results have to be 
						// watched with the isGrown method of the original RandomTree instance.
						RandomTree t = getInstance(root, sampler, classification, count, node, mode, depth, maxDepth);
						root.forest.nodeScheduler.startThread(t);
						return;
					}
				}
			}
		
		// Get random feature parameter sets
		List<Object> paramSet = params.featureFactory.getRandomFeatureSet(params);
		int numOfFeatures = paramSet.size();

		// Generate random thresholds for each feature param set
		float[][] thresholds = new float[numOfFeatures][];
		for(int i=0; i<thresholds.length; i++) {
			thresholds[i] = ((Feature)paramSet.get(i)).getRandomThresholds(params.thresholdCandidatesPerFeature);
		}

		// Evaluate the features
		//int numOfClasses = getNumOfClasses();
		long[][][] countClassesLeft = new long[numOfFeatures][params.thresholdCandidatesPerFeature][numOfClasses];
		long[][][] countClassesRight = new long[numOfFeatures][params.thresholdCandidatesPerFeature][numOfClasses];
		evaluateFeaturesThreads(root, sampler, paramSet, classification, count, mode, thresholds, countClassesLeft, countClassesRight, node, depth);		

		// Calculate info gain upon each combination of feature/threshold 
		double[][] gain = getGainsByEntropy(paramSet.size(), countClassesLeft, countClassesRight);
		
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
			log.write(pre + "Finished node " + node.id + " at Depth " + depth + ", Mode: " + mode); //, System.out);
			log.write(pre + "Winner: " + winner + " Thr Index: " + winnerThreshold + "; Information gain: " + decimalFormat.format(gain[winner][winnerThreshold]));
			log.write(pre + "Gain min: " + decimalFormat.format(min) + ", max: " + decimalFormat.format(max));
			float tmin = Float.MAX_VALUE;
			float tmax = -Float.MAX_VALUE;
			for(int i=0; i<thresholds[winner].length; i++) {
				if (thresholds[winner][i] > tmax) tmax = thresholds[winner][i];
				if (thresholds[winner][i] < tmin) tmin = thresholds[winner][i];
			}
			log.write(pre + "Threshold min: " + decimalFormat.format(tmin) + "; max: " + decimalFormat.format(tmax));
			if (thresholds[winner][winnerThreshold] == tmin) log.write(pre + "WARNING: Threshold winner is min: Depth " + depth + ", mode: " + mode + ", thr: " + thresholds[winner][winnerThreshold]);
			if (thresholds[winner][winnerThreshold] == tmax) log.write(pre + "WARNING: Threshold winner is max: Depth " + depth + ", mode: " + mode + ", thr: " + thresholds[winner][winnerThreshold]);

			// Application specific log entries
			logAdditional(pre, countClassesLeft, countClassesRight, winner, winnerThreshold);
			
			// Feature specific log entries
			//node.feature.logAdditional(pre, log);

			// Log all feature candidates
			if (params.logFeatureCandidates) {
				for(int i=0; i<paramSet.size(); i++) {
					// Get max info gain of this feature candidate
					double maxFG = -Double.MAX_VALUE;
					for(int j=0; j<gain[i].length; j++) {
						if (gain[i][j] > maxFG) maxFG = gain[i][j];
					}
					log.write(pre + "Feature " + i + ": " + paramSet.get(i) + "; max info gain: " + maxFG);
				}
			}
			
			// Log all tested thresholds of the winner feature
			if (params.logWinnerThresholdCandidates) {
				for(int i=0; i<thresholds[winner].length; i++) {
					log.write(pre + "Thr. " + i + ": " + decimalFormat.format(thresholds[winner][i]) + ", Gain: " + decimalFormat.format(gain[winner][i]));
				}
			}
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
			// Yes, save best feature
			node.feature = (Feature)paramSet.get(winner);
			node.feature.threshold = thresholds[winner][winnerThreshold];
			if (params.logNodeInfo) log.write(pre + "Feature threshold: " + node.feature.threshold + "; Class: " + node.feature.getClass().getName() + ", Coeffs: " + node.feature);
		} else {
			// No, make this node a leaf and return
			node.probabilities = calculateLeaf(sampler, classification, mode, depth);
			if (params.logNodeInfo) log.write(pre + "Info gain insufficient, Leaf probabilities: " + ArrayUtils.toString(node.probabilities, false));
			return;
		}
		
		// Split values by winner feature for deeper branches
		long[] counts = new long[2];
		List<Classification> classificationNextL = new ArrayList<Classification>();
		List<Classification> classificationNextR = new ArrayList<Classification>();
		splitValues(sampler, classification, classificationNextL, classificationNextR, mode, node, counts);
		
		// If one side has 0 values to classify, make this node a leaf and return
		if (counts[0] == 0 || counts[1] == 0) {
			node.probabilities = calculateLeaf(sampler, classification, mode, depth);
			if (params.logNodeInfo) log.write(pre + "One side zero -> leaf; Probabilities: " + ArrayUtils.toString(node.probabilities, true));
			log.flush();
			return;
		}
		
		// Flush log file changes to disk to preserve them if crashes happen
		log.flush();

		// Shred classifications for garbage collector before recursion
		for(int c=0; c<classification.size(); c++) {
			classification.get(c).clear();
		}
		classification.clear(); 
		classification = null;
		
		// Recursion to left and right
		node.left = new Node();
		growRec(root, sampler, classificationNextL, counts[0], node.left, 1, depth+1, maxDepth, true);
		node.right = new Node();
		growRec(root, sampler, classificationNextR, counts[1], node.right, 2, depth+1, maxDepth, true);
	}

	/**
	 * Evaluates a couple of features with a couple of thresholds. 
	 * This method just controls the thread behaviour of feature evaluation.
	 * The CPU-intensive calculation takes place in abstract method evaluateFeatures().
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
	protected void evaluateFeaturesThreads(RandomTree root, Sampler<Dataset> sampler, List<Object> paramSet, List<Classification> classification, long count, int mode, float[][] thresholds, long[][][] countClassesLeft, long[][][] countClassesRight, Node node, int depth) throws Exception {
		int numWork = getNumOfWork(sampler, paramSet, classification); //paramSet.size(); //sampler.getPoolSize();
		
		if (!params.enableEvaluationThreads) {
			// Eval threads are disabled -> simply calculate it
			evaluateFeatures(sampler, 0, numWork-1, paramSet, classification, mode, thresholds, countClassesLeft, countClassesRight);
			return;
		}
		
		if (newThreadRoot != null) { // || count < params.evalThreadingThreshold) {
			evaluateFeatures(sampler, 0, numWork-1, paramSet, classification, mode, thresholds, countClassesLeft, countClassesRight);
			return;
		}
		
		/*if (count < params.nodeThreadingThreshold) {
			System.out.println("--> WARNING: Eval threads below node threading threshold: " + count);
		}*/
		
		// Create worker threads for groups of frequency bands and start them
		RandomTreeWorker[] workers = null;
		synchronized (root.forest.evalScheduler) {
			int threadNum = root.forest.evalScheduler.getThreadsAvailable();
			if (threadNum > 1) {
				workers = new RandomTreeWorker[threadNum];
				double ipw = (double)numWork / workers.length;
				for(int i=0; i<workers.length; i++) {
					int min = (int)(i*ipw);
					int max = (int)((i+1)*ipw - 1);
					if (i == workers.length-1 || max >= numWork) max = numWork-1;
					//System.out.println(min + " to " + max);
					workers[i] = new RandomTreeWorker(this, min, max, sampler, paramSet, classification, mode, thresholds, countClassesLeft, countClassesRight);
					root.forest.evalScheduler.startThread(workers[i]);
				}
				//System.out.println("Started " + workers.length + " eval threads for " + count + " values");
			} else {
				evaluateFeatures(sampler, 0, numWork-1, paramSet, classification, mode, thresholds, countClassesLeft, countClassesRight);
				return;
			}
		}
		
		// Wait for the worker threads
		long lastTime = 0;
		while(true) {
			synchronized(this) {
				try {
					wait(params.threadWaitTime);
				} catch (InterruptedException e) {
					System.out.println("[Wait interrupted by VM, continuing...]");
				}
			}
			
			if (params.debugThreadPolling && (System.currentTimeMillis() - lastTime > params.threadWaitTime)) {
				lastTime = System.currentTimeMillis();
				// General stats
				String countS = (count == Long.MAX_VALUE) ? "all" : count+"";
				int nt = root.forest.nodeScheduler.getThreadsActive();
				int et = root.forest.evalScheduler.getThreadsActive();
				System.out.println(
						timeStampFormatter.format(new Date()) + ": T" + num + ", Thrds: " + et + " + " + nt + " (" + root.forest.nodeScheduler.getMax() + ") = " + (nt+et) + ", Node " + node.id + ", Depth " + depth + ", Values: " + countS + "; " + 
						"Heap: " + Math.round((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()) / (1024.0*1024.0)) + " MB"
				);
			}
			
			boolean ret = true;
			for(int i=0; i<workers.length; i++) {
				if (!workers[i].threadIsFinished()) {
					ret = false;
					break;
				}
			}
			if (ret) break;
		}
	}
	
	/**
	 * Info gain calculation upon shannon entropy, Kinect formula.
	 * 
	 */
	protected double[][] getGainsByEntropy(int paramSetSize, long[][][] countClassesLeft, long[][][] countClassesRight) {
		double[][] gain = new double[paramSetSize][params.thresholdCandidatesPerFeature];
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
		int numOfFeatures = paramSetSize;
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
	 * Calculates shannon entropy for a binary alphabet (two possible values),  
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
	 * Wraps the growRec() method for multithreaded tree growing, using the 
	 * instance attributes postfixed with "newThread".
	 * Represents an "anonymous" RandomTree instance to wrap the growRec method. 
	 * Results have to be watched with the isGrown method of the original (root) RandomTree instance.
	 * 
	 */
	public void run() {
		try {
			growRec(newThreadRoot, newThreadSampler, newThreadClassification, newThreadCount, newThreadNode, newThreadMode, newThreadDepth, newThreadMaxDepth, false);
			//newThreadRoot.forest.scheduler.decThreadsActive();
			//if (newThreadRoot.params.debugNodeThreads) System.out.println("    T" + num + ": Finished node thread, depth: " + newThreadDepth + ", active: " + newThreadRoot.forest.scheduler.getThreadsActive() + ", values: " + newThreadCount);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		setThreadFinished();
		//System.out.println("Finished node thread id " + getThreadId() + " at depth " + newThreadDepth);
	}
	
	/**
	 * Returns the info gain statistic instance.
	 * 
	 * @return
	 */
	public Statistic getInfoGain() {
		return infoGain;
	}

	/**
	 * Returns the root node of the tree.
	 * 
	 * @return
	 */
	public Node getRootNode() {
		return tree;
	}

	/**
	 * Returns the number of classes to divide.
	 * 
	 * @return
	 */
	public int getNumOfClasses() {
		return numOfClasses;
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
		tree = (Node)ois.readObject();
		tree.feature.initStatic();
		ois.close();
	}

	/**
	 * Returns if the tree is grown.
	 * 
	 * @return
	 *
	public boolean isGrown() {
		return (nodeThreadsActive == 0) && (evaluationThreadsActive == 0);
	}

	/**
	 * Sets a forest as parent.
	 * 
	 * @param forest
	 */
	public void setForest(Forest forest) {
		this.forest = forest;
	}
	
	/**
	 * Write some growing params to the tree log file.
	 * @throws Exception 
	 * 
	 */
	protected void logMeta(Sampler<Dataset> sampler) throws Exception {
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
	 * Provides the possibility to add tree specific log output per node.
	 * Override this method to insert your individual log entries for each node during training.
	 * 
	 * @param pre
	 * @param countClassesLeft
	 * @param countClassesRight
	 * @param winner
	 * @param winnerThreshold
	 * @throws Exception
	 */
	public void logAdditional(String pre, long[][][] countClassesLeft, long[][][] countClassesRight, int winner, int winnerThreshold) throws Exception {
	}
	
}
