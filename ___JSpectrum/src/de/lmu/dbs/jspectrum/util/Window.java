package de.lmu.dbs.jspectrum.util;

/**
 * Interface for windowing functions.
 * 
 * @author Thomas Weber
 *
 */
public interface Window {

	/**
	 * Applies a window function to data. The function has to scale the data by factors in between [0:1].
	 * 
	 * @param data
	 */
    public void apply(double[] data);
	
}
