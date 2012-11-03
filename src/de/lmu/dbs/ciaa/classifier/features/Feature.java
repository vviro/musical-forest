package de.lmu.dbs.ciaa.classifier.features;

import java.io.Serializable;
import java.util.List;

import de.lmu.dbs.ciaa.classifier.RandomTreeParameters;

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
	public int threshold;
	
	/**
	 * Returns a list of generated features.
	 * 
	 * @param params the RandomTreeParameters object holding parameters.
	 * @return
	 */
	public abstract List<Feature> getRandomFeatureSet(final RandomTreeParameters params);

	/**
	 * Feature function called to classify values. 
	 * 
	 * @param data data sample
	 * @param x coordinate in data sample
	 * @param y coordinate in data sample
	 * @return
	 * @throws Exception 
	 */
	public abstract int evaluate(final byte[][] data, final int x, final int y) throws Exception;

}
