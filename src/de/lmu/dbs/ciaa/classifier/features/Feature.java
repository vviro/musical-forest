package de.lmu.dbs.ciaa.classifier.features;

import java.io.Serializable;
import java.util.List;

import de.lmu.dbs.ciaa.classifier.ForestParameters;

/**
 * Abstract feature implementation with its parameters and feature evaluation function,
 * also the subclasses have to implement a factory method.
 * 
 * @author Thomas Weber
 *
 */
public abstract class Feature implements Serializable {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Threshold to compare to the feature functions output value.
	 */
	public double threshold;
	
	/**
	 * Returns a list of generated features.
	 * 
	 * @param params the RandomTreeParameters object holding parameters.
	 * @return
	 */
	public abstract List<Feature> getRandomFeatureSet(final ForestParameters params);

	/**
	 * Feature function called to classify values. 
	 * 
	 * @param data data sample
	 * @param x coordinate in data sample
	 * @param y coordinate in data sample
	 * @return
	 * @throws Exception 
	 */
	public abstract float evaluate(final byte[][] data, final int x, final int y) throws Exception;

	/**
	 * Returns a visualization of all node features of the forest. For debugging use.
	 * 
	 * @param data the array to store results (additive)
	 */
	public abstract void visualize(int[][] data);

	/**
	 * Returns the maximum evaluate value
	 * 
	 * @return
	 */
	public abstract float getMaxValue();
	
	/**
	 * Returns a randomly generated threshold candidate for the feature.
	 * 
	 * @return
	 */
	public float[] getRandomThresholds(int num) {
		float[] ret = new float[num];
		for(int i=0; i<num; i++) {
			ret[i] = (float)(Math.random() * getMaxValue());
		}
		return ret;
	}
}
