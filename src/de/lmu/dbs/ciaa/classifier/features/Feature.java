package de.lmu.dbs.ciaa.classifier.features;

import java.io.Serializable;

/**
 * Abstract feature implementation with its parameters.
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
	 * Feature function called to classify tree nodes. 
	 * 
	 * @param data data sample
	 * @param x coordinate in data sample
	 * @param y coordinate in data sample
	 * @param params feature parameters
	 * @return
	 * @throws Exception 
	 */
	public abstract int evaluate(byte[][] data, int x, int y) throws Exception;

}
