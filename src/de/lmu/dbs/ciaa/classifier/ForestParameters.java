package de.lmu.dbs.ciaa.classifier;

import de.lmu.dbs.ciaa.classifier.features.Feature;

/**
 * Contains parameters for growing random trees and features. In normal 
 * operation (classify), this is meaningless.
 * 
 * @author Thomas Weber
 *
 */
public class ForestParameters {

	/**
	 * Folder where the forest can save its node data. Can also be used to store test results etc.
	 */
	public String workingFolder = null;
	
	/**
	 * Prefix for the node data files (one will be created for each tree)
	 */
	public String nodedataFilePrefix = null;
	
	/**
	 * Path to the frequency table file, usually located beneath the test data folders. The file 
	 * holds a table of frequencies corresponding to the bins of the spectral transformation output.
	 */
	public String frequencyTableFile = null;
	
	/**
	 * Load the forest from the node data in the working folder (must be grown first).
	 */
	public boolean loadForest = false;
	
	/**
	 * Number of trees in the forest
	 */
	public int forestSize = -1;
	
	/**
	 * Max depth of tree
	 */
	public int maxDepth = -1;
	
	/**
	 * Level of Multithreading. The amount of threads per tree calculates to 2^threadDepth. 
	 * -1 does no multithreading at all, zero will create one thread per tree.
	 */
	public int threadDepth = -2;
	
	/**
	 * Milliseconds interval to check growth status in the forest.
	 */
	public long threadWaitTime = -1;
	
	/**
	 * For each data frame, this represents the percentage of the values to be picked initially
	 * by the training algorithm of the trees.
	 */
	public double percentageOfRandomValuesPerFrame = -1;
	
	/**
	 * This is just an array holding the frequencies corresponding to the spectral data bins
	 */
	public double[] frequencies = null;
	
	/**
	 * This is the number of randomly generated feature parameter sets for each node in training.
	 */
	public int numOfRandomFeatures = -1;

	/**
	 * Minimal x deviation of the feature function
	 */
	public int xMin = -5;
	
	/**
	 * Maximal x deviation of the feature function
	 */
	public int xMax = 5;
	
	/**
	 * Minimal y deviation of the feature function
	 */
	public int yMin = -150;
	
	/**
	 * Maximal y deviation of the feature function
	 */
	public int yMax = 150;
	
	/**
	 * For randomly picked threshold values, this is the maximum
	 */
	public int thresholdMax = 60;
	
	/**
	 * This is the number of randomly picked threshold candidates generated for each feature parameter set
	 */
	public int thresholdCandidatesPerFeature = -1;
	
	/**
	 * Number of frequency bins per octave. Has to be equal to the CQT equivalent parameter.
	 */
	public double binsPerOctave = -1;
	
	/**
	 * Min frequency of spectral data. Has to be equal to the CQT equivalent parameter.
	 */
	public double fMin = -1;

	/**
	 * Max frequency of spectral data. Has to be equal to the CQT equivalent parameter.
	 */
	public double fMax = -1;

	/**
	 * This is a plain feature instance that later is used to generate the training 
	 * features with the getRandomFeatureSet method. It is never used for classification
	 * itself, just as a factory.
	 */
	public Feature featureFactory = null;
	
	/**
	 * Samples per frame, used to interpret training data and for testing
	 */
	public int step = -1;
	
	/**
	 * See CQT Transform
	 */
	public double threshold = -1;
	
	/**
	 * See CQT Transform
	 */
	public double spread = -1;
	
	/**
	 * See CQT Transform
	 */
	public double divideFFT = -1;
	
	/**
	 * Buffer location (folder) for cqt kernels. Must be writable by the program.
	 */
	public String cqtBufferLocation = null;
	
	/**
	 * Show little progress info while growing.
	 */
	public boolean logProgress = false;
	
	/**
	 * Show more details for each node growed on the console.
	 */
	public boolean logNodeInfo = false;
	
	/**
	 * Show a message every time a new thread is created.
	 */
	public boolean debugThreadForking = false;
	
	/**
	 * Show thread polling status information.
	 */
	public boolean debugThreadPolling = false;
	
	/**
	 * Checks value integrity.
	 * 
	 * @return
	 * @throws Exception 
	 */
	public void check() throws Exception {
		if (percentageOfRandomValuesPerFrame < 0 || percentageOfRandomValuesPerFrame > 1) throw new Exception("Invalid value of percentageOfRandomValuesPerFrame: " + percentageOfRandomValuesPerFrame + " (must be in range [0,1])");
		if (numOfRandomFeatures < 1) throw new Exception("Invalid numOfRandomFeatures, must be >= 1: " + numOfRandomFeatures);
		if (thresholdCandidatesPerFeature < 1) throw new Exception("Invalid thresholdCandidatesPerFeature, must be >= 1: " + thresholdCandidatesPerFeature);
		if (frequencies == null || frequencies.length < 1) throw new Exception("Frequency array is null or contains no elements");
		if (binsPerOctave < 1) throw new Exception("You have to set the number of bins per octave");
		if (fMin < 1) throw new Exception("You have to set the minimum frequency of spectral data");
		if (fMax < 1) throw new Exception("You have to set the maximum frequency of spectral data");
		if (forestSize < 1) throw new Exception("Forest must have at least one tree: " + forestSize);
		if (maxDepth < 1) throw new Exception("Maximum tree depth has to be at least 1: " + maxDepth);
		if (threadDepth < -1) throw new Exception("Illegal thread depth value: " + threadDepth);
		if (threadWaitTime < 10) throw new Exception("Thread wait time too short: " + threadWaitTime);
		if (step < 1) throw new Exception("Frame step in samples too low: " + step);
		if (threshold < 0) throw new Exception("CQT transformation threshold is too low: " + threshold);
		if (spread < 0) throw new Exception("CQT transformation spread is too low: " + spread);
		if (divideFFT <= 0) throw new Exception("CQT divideFFT parameter has to be greater than zero: " + divideFFT);
		if (cqtBufferLocation == null) throw new Exception("No cqt kernel buffer folder is set");
	}
}
