package de.lmu.dbs.ciaa.util;

import static java.lang.Math.PI;
import static java.lang.Math.cos;

/**
 * Hamming window class.
 * <br><br>
 * Adapted from Tagtraum Industries Inc. Jipes WindowFunctions&Hamming class
 * 
 * @author Thomas Weber, Hendrik Schreiber
 *
 */
public class HammingWindow implements Window {

	/**
	 * Double of PI
	 */
    private static final double DOUBLE_PI = 2.0 * PI;
    
    /**
     * Precalculated factors
     */
    private double[] scales;
    
    /**
     * Create hamming window object.
     * 
     * @param length size of the window
     */
    public HammingWindow(final int length) {
        this.scales = new double[length];
        final int lengthMinus1 = length - 1;
        for (int n = 0; n < length; n++) {
            final double cosArg = (DOUBLE_PI * n) / lengthMinus1;
            scales[n] = (double) (0.54 - 0.46 * cos(cosArg));
        }
    }

    /**
     * Applies the window to data array.
     * 
     * @param data input data
     * @return the results (same size as input)
     */
    public void apply(double[] data) {
        if (data.length != scales.length) 
        	throw new IllegalArgumentException("Data length must equal scales length.");
        for (int i=0; i<data.length; i++) {
        	data[i]*= scales[i];
        }
    }
}
