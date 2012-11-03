package de.lmu.dbs.ciaa.classifier;

import de.lmu.dbs.ciaa.classifier.features.Feature;

/**
 * Contains parameters for random trees and features.
 * 
 * @author Thomas Weber
 *
 */
public class RandomTreeParameters {

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
	}
}
