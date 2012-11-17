package de.lmu.dbs.ciaa.classifier.core;

import java.io.Serializable;
import java.util.List;


/**
 * Core feature class.
 * 
 * @author Thomas Weber
 *
 */
public abstract class Feature implements Serializable {

	/**
	 * 
	 */
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
	public abstract List<Object> getRandomFeatureSet(final ForestParameters params);

	/**
	 * Returns a visualization of all node features of the forest. For debugging use.
	 * 
	 * @param data the array to store results (additive)
	 */
	public abstract void visualize(Object data);

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
