package de.lmu.dbs.jforest.util;

/**
 * Interface to implement scaling functions which transform from [0,1] to [0,1].
 * 
 * @author Thomas Weber
 *
 */
public interface Scale {

	/**
	 * Apply the scaling function.
	 * 
	 * @param sample range: [0,1]
	 * @return also range between [0,1]
	 * @throws Exception 
	 */
	public double apply(final double sample) throws Exception;
	
}
