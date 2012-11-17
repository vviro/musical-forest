package de.lmu.dbs.ciaa.classifier.core2d;

import de.lmu.dbs.ciaa.classifier.core.Feature;

/**
 * Base class for 2d features.
 * 
 * @author Thomas Weber
 *
 */
public abstract class Feature2d extends Feature {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

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

}
