package de.lmu.dbs.ciaa.util;

/**
 * Logarithmic scaling function
 * 
 * @author Thomas Weber
 *
 */
public class LogScale implements Scale {

	/**
	 * Width of the area of log that is used
	 */
	double width;
	
	/**
	 * Precalculated factor to be sure that 1 maps to 1
	 */
	double factor;

	/**
	 * Logarithmic scaling with plain log to base 10.
	 * 
	 */
	public LogScale() {
		this(1);
	}
	/**
	 * Logarithmic scaling. Takes the interval between [1,1+width] of the logarithm on base 10 as core function.
	 *  
	 * @param width
	 */
	public LogScale(final double width) {
		this.width = width;
		this.factor = 1/Math.log10(1+width);
	}
	
	/**
	 * Apply the scaling function.
	 * 
	 * @param sample range: [0,1]
	 * @return also range between [0,1]
	 * @throws Exception 
	 */
	public double apply(final double sample) throws Exception {
		if (sample > 1 || sample < 0) {
			throw new Exception("Value has to be in range [0,1]");
		}
		return Math.log10(sample*width+1)*factor;
	}
	
}
