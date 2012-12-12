package de.lmu.dbs.jforest.core;

import java.io.Serializable;

import org.jdom2.Element;

import de.lmu.dbs.jforest.core.Feature;
import de.lmu.dbs.jforest.util.ParamLoader;

/**
 * Contains parameters for growing random trees and features. In normal 
 * operation (classify), this is meaningless.
 * 
 * @author Thomas Weber
 *
 */
public class ForestParameters extends ParamLoader implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public boolean logWinnerThresholdCandidates;
	
	/**
	 * 
	 */
	public boolean logFeatureCandidates;
	
	/**
	 * Number of trees in the forest during training
	 */
	public int forestSize = -1;
	
	/**
	 * Max depth of tree growing
	 */
	public int maxDepth = -1;
	
	/**
	 * For each data frame, this represents the percentage of the values to be picked initially
	 * by the training algorithm of the trees.
	 */
	public double percentageOfRandomValuesPerFrame = -1;
	
	/**
	 * This is the number of randomly generated feature parameter sets for each node in training.
	 */
	public int numOfRandomFeatures = -1;

	/**
	 * This is the number of randomly picked threshold candidates generated for each feature parameter set
	 */
	public int thresholdCandidatesPerFeature = -1;
	
	/**
	 * This is a plain feature instance that later is used to generate the training 
	 * features with the getRandomFeatureSet method. It is never used for classification
	 * itself, just as a factory.
	 */
	public Feature featureFactory = null;
	
	/**
	 * Threshold for information gain of the winner feature in growing a node. If info gain is 
	 * below entropyThreshold, the node will become a leaf. Values below zero will disable the
	 * threshold and calculate all nodes down to maxDepth. 
	 */
	public double entropyThreshold = -1;
	
	/**
	 * Show little progress info while growing.
	 */
	public boolean logProgress = false;
	
	/**
	 * Show more details for each node growed on the console.
	 */
	public boolean logNodeInfo = false;
	
	/**
	 * Save a gains/thresholds diagram in a png file for each node´s winner feature, up to the given depth. 
	 * Use -1 to save all nodes.
	 */
	public int saveGainThresholdDiagrams = 0;
	
	/**
	 * Save classifications for each node after clasification run, up to the given depth
	 * Use -1 to save all nodes.
	 */
	public int saveNodeClassifications = 0;
	
	/**
	 * Folder where the extra debugging file go. You have to set this manually before starting training.
	 */
	public String debugFolder = "";
	
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
		if (forestSize < 1) throw new Exception("Forest must have at least one tree: " + forestSize);
		if (maxDepth < 1) throw new Exception("Maximum tree depth has to be at least 1: " + maxDepth);
	}
	
	/**
	 * Generates a parameter set for forest growing. You have to set some of
	 * the params manually...see class documentation of RandomTreeParameters.
	 * 
	 * @return
	 * @throws Exception 
	 */
	public void loadParameters(String configFile) throws Exception {
		Element root = parseConfigFile(configFile, null);
		Element general = root.getChild("General");
		Element forest = root.getChild("Forest");

		// General
		logProgress = Boolean.parseBoolean(general.getAttributeValue("logProgress"));
		logNodeInfo = Boolean.parseBoolean(general.getAttributeValue("logNodeInfo"));
		logFeatureCandidates = Boolean.parseBoolean(general.getAttributeValue("logFeatureCandidates"));
		logWinnerThresholdCandidates = Boolean.parseBoolean(general.getAttributeValue("logWinnerThresholdCandidates"));
		saveGainThresholdDiagrams = Integer.parseInt(general.getAttributeValue("saveGainThresholdDiagrams"));
		saveNodeClassifications = Integer.parseInt(general.getAttributeValue("saveNodeClassifications"));
		
		// Forest
		forestSize = Integer.parseInt(forest.getAttributeValue("forestSize"));
		maxDepth = Integer.parseInt(forest.getAttributeValue("maxDepth"));
		percentageOfRandomValuesPerFrame = Double.parseDouble(forest.getAttributeValue("percentageOfRandomValuesPerFrame"));
		numOfRandomFeatures = Integer.parseInt(forest.getAttributeValue("numOfRandomFeatures"));
		entropyThreshold = Double.parseDouble(forest.getAttributeValue("entropyThreshold"));
		thresholdCandidatesPerFeature = Integer.parseInt(forest.getAttributeValue("thresholdCandidatesPerFeature"));

		String clsName = forest.getAttributeValue("featureFactoryClass");
		featureFactory = (Feature)Class.forName(clsName).getConstructor().newInstance();
	}
	
	/**
	 * Packs most parameters into a readable string.
	 * 
	 * @return
	 */
	public String toString() {
		String ret = "Training forest parameters: \n";
		ret+= "  Forest size at training:               " + forestSize + "\n";
		ret+= "  Depth:                                 " + maxDepth + "\n";
		ret+= "  Percentage of random values per frame: " + percentageOfRandomValuesPerFrame + "\n";
		ret+= "  Number of random features candidates:  " + numOfRandomFeatures + "\n";
		ret+= "  Threshold candidates per candidate:    " + thresholdCandidatesPerFeature + "\n";
		ret+= "  Used feature implementation:           " + featureFactory.getClass().getName() + "\n";
		ret+= "  Entropy threshold:                     " + entropyThreshold + "\n";
		return ret;
	}
}
