package de.lmu.dbs.jforest.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import de.lmu.dbs.jforest.sampler.Sampler;
import de.lmu.dbs.jforest.util.Logfile;
import de.lmu.dbs.jforest.util.Statistic;
import de.lmu.dbs.jforest.util.Statistic2d;
import de.lmu.dbs.jforest.util.ArrayUtils;
import de.lmu.dbs.jforest.util.LogScale;
import de.lmu.dbs.jforest.util.Scale;
import de.lmu.dbs.jforest.util.workergroup.ScheduledThread;

/**
 * Base class for trees.
 * 
 * @author Thomas Weber
 *
 */
public abstract class RandomTree extends ScheduledThread {

	/**
	 * Set at begin of growing: initial amount of values to classify
	 */
	private long initialCount = 0;
	
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
	public RandomTree(int numOfClasses, int num) throws Exception {
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
		this(numOfClasses, num);
		this.params = params;
		params.check();
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
	public abstract RandomTree getInstance(int numOfClasses, int num) throws Exception;

	/**
	 * Returns the classification of the tree at a given value in data: data[x][y]
	 * 
	 * @param data
	 * @param x
	 * @param y
	 * @return
	 * @throws Exception
	 */
	public abstract float[] classify(final Object data, final int x, final int y, int maxDepth) throws Exception;
		
	/**
	 * Build first classification array (from bootstrapping samples and random values per sampled frame)
	 * 
	 * @param sampler
	 * @return
	 * @throws Exception 
	 */
	protected abstract List<Classification> getPreClassification(Sampler<Dataset> sampler) throws Exception;

	/**
	 * Build first classification array (from bootstrapping samples and random values per sampled frame)
	 * 
	 * @param sampler
	 * @return
	 * @throws Exception 
	 */
	protected abstract List<Classification> getPreClassification(Sampler<Dataset> sampler, double vpf) throws Exception;

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
	public abstract void evaluateFeatures(RandomTreeWorker worker, Sampler<Dataset> sampler, int minIndex, int maxIndex, List<Object> paramSet, List<Classification> classification, int mode, Object thresholds, long[][][] countClassesLeft, long[][][] countClassesRight) throws Exception;

	/**
	 * Expands the forest so that every node has probabiliy arrays. Useful for generating stats.
	 * 
	 * @param sampler
	 * @throws Exception 
	 */
	public void expand(Sampler<Dataset> sampler, ExpansionWorker w) throws Exception {
		// Preclassify
		List<Classification> classification = getPreClassification(sampler, 1.0);
		System.out.println("Finished pre-classification for tree " + num + ", start expanding...");
		w.setProgress(0.01);
		
		initialCount = 0;
		for(int i=0; i<classification.size(); i++) {
			initialCount+= classification.get(i).getSize();
		}

		expandRec(sampler, classification, initialCount, tree, 0, 0, w);
	}

	/**
	 * 
	 * @param sampler
	 * @param classification
	 * @param initialCount
	 * @param node
	 * @param mode
	 * @param depth
	 * @throws Exception 
	 */
	private void expandRec(Sampler<Dataset> sampler, List<Classification> classification, long initialCount, Node node, int mode, int depth, ExpansionWorker w) throws Exception {
		w.addProgressNode();
		if (node.isLeaf()) {
			//System.out.println("Leaf at depth " + depth + ", node " + node.id + ": Stopped expansion");
			return;
		}
		
		//System.out.println("Expanding node " + node.id + " at depth " + depth + "; Heap: " + getHeapMB() + "MB");
		
		// Get probs for all nodes
		node.probabilities = calculateLeaf(sampler, classification, mode, depth);

		// Split values
		long[] counts = new long[2];
		List<Classification> classificationNextL = new ArrayList<Classification>();
		List<Classification> classificationNextR = new ArrayList<Classification>();
		splitValues(sampler, classification, classificationNextL, classificationNextR, mode, node, counts);
		
		// Shred classifications for garbage collector before recursion
		for(int c=0; c<classification.size(); c++) {
			classification.get(c).clear();
		}
		classification.clear(); 
		classification = null;
		
		//System.out.println("Finished expanding node " + node.id + " at depth " + depth);
		
		// Recursion to left and right
		expandRec(sampler, classificationNextL, counts[0], node.left, 1, depth+1, w);
		expandRec(sampler, classificationNextR, counts[1], node.right, 2, depth+1, w);
	}

	/**
	 * Returns the JVM heap size in megabytes.
	 * 
	 * @return
	 */
	public int getHeapMB() {
		return (int)Math.round((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()) / (1024.0*1024.0));
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
		List<Classification> classification = getPreClassification(sampler);
		System.out.println("Finished pre-classification for tree " + num + ", start growing...");

		initialCount = 0;
		for(int i=0; i<classification.size(); i++) {
			initialCount+= classification.get(i).getSize();
		}
		growRec(this, sampler, classification, initialCount, tree, 0, 0, maxDepth, true);
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

		// Start an "anonymous" RandomTree instance to calculate this method. Results have to be 
		// watched with the isGrown method of the original RandomTree instance.
		if (count < root.forest.nodeThreadingThreshold && root.forest.nodeScheduler.getMaxThreads() > 0) {
			synchronized (root.forest.nodeScheduler) { 
				if (multithreading && (root.forest.nodeScheduler.getThreadsAvailable() > 0)) {
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
		long[][][] countClassesLeft = new long[numOfFeatures][params.thresholdCandidatesPerFeature][numOfClasses];
		long[][][] countClassesRight = new long[numOfFeatures][params.thresholdCandidatesPerFeature][numOfClasses];
		evaluateFeaturesThreaded(root, sampler, paramSet, classification, count, mode, thresholds, countClassesLeft, countClassesRight, node, depth);		

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
		
		// Log //////////////////////////////////////////
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
			logExit(pre, countClassesLeft, countClassesRight, winner, winnerThreshold);
			
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
			String nf = params.debugFolder + File.separator + "T" + num + "_GainDistribution_Depth" + depth + "_mode_" + mode + "_id_" + node.id + ".png";
			Scale s = new LogScale(10); // Log to make better visibility of dark points
			gainStat.saveDistributionImage(nf, 400, 400, s);
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
	protected void evaluateFeaturesThreaded(RandomTree root, Sampler<Dataset> sampler, List<Object> paramSet, List<Classification> classification, long count, int mode, float[][] thresholds, long[][][] countClassesLeft, long[][][] countClassesRight, Node node, int depth) throws Exception {
		int numWork = getNumOfWork(sampler, paramSet, classification); //paramSet.size(); //sampler.getPoolSize();
		
		if (root.forest.evalScheduler.getMaxThreads() <= 1) {
			// Eval threads are disabled -> simply calculate it
			evaluateFeatures(null, sampler, 0, numWork-1, paramSet, classification, mode, thresholds, countClassesLeft, countClassesRight);
			return;
		}
		
		if (newThreadRoot != null) { 
			// This node is in node threading mode -> simply calculate it
			evaluateFeatures(null, sampler, 0, numWork-1, paramSet, classification, mode, thresholds, countClassesLeft, countClassesRight);
			return;
		}

		// Create and run evaluation worker group
		synchronized(root.forest.evalScheduler) {
			RandomTreeWorkerGroup group = new RandomTreeWorkerGroup(this, root.forest.evalScheduler, numWork, true, root.forest, count, num, node, depth);
			int threadNum = root.forest.evalScheduler.getThreadsAvailable();
			for(int i=0; i<threadNum; i++) {
				RandomTreeWorker worker = new RandomTreeWorker(group, this, sampler, paramSet, classification, mode, thresholds, countClassesLeft, countClassesRight);
				group.add(worker);
			}
			group.runGroup();
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
	public static double getBinaryEntropy(final long a, final long b) {
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
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		setThreadFinished();
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
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	public void load(final String filename) throws Exception {
		FileInputStream fin = new FileInputStream(filename);
		ObjectInputStream ois = new ObjectInputStream(fin);
		tree = (Node)ois.readObject();
		ois.close();
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
	 * 
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
	public void logExit(String pre, long[][][] countClassesLeft, long[][][] countClassesRight, int winner, int winnerThreshold) throws Exception {
	}

	/**
	 * Returns the initial amount of samples used to grow the tree.
	 * 
	 * @return
	 */
	public long getInitialCount() {
		return initialCount;
	}
	
	/**
	 * Returns the amount of nodes in the forest
	 * 
	 * @return
	 * @throws Exception 
	 */
	public int getNodeCount() {
		return getNodeCountRec(tree); 
	}

	/**
	 * Internal
	 * 
	 * @param node
	 * @param counts
	 * @param depth
	 */
	private int getNodeCountRec(Node node) {
		int ret = 1;
		if (!node.isLeaf()) {
			ret += getNodeCountRec(node.left);
			ret += getNodeCountRec(node.right);
		}
		return ret;
	}
	

}
