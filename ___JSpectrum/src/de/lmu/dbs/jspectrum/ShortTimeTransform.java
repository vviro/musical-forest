package de.lmu.dbs.jspectrum;

import de.lmu.dbs.jspectrum.util.Window;

/**
 * Base class for time-domain -> time/freq-domain transformations
 * 
 * @author Thomas Weber
 *
 */
public abstract class ShortTimeTransform {

	/**
	 * Calculate the transformation
	 * 
	 * @param samples input data (time domain)
	 * @param step samples per frame
	 * @param windowFunction windowing function object 
	 * @return
	 */
	public abstract double[][] calculate(final int[] samples, final int step, final Window windowFunction);

	/**
	 * Return window size of transformation 
	 * 
	 * @return
	 */
	public abstract int getWindowSize();

	/**
	 * Returns the frequencies in hertz, corresponding to the second level of the 
	 * data matrix returned by calculate(..).
	 * 
	 * @return 
	 */
	public abstract double[] getFrequencies();

	/**
	 * Adds zeroes at the beginning of in.
	 * 
	 * @param in
	 * @param samples amount of zeroes to add
	 * @return
	 */
	public int[] addLeadingZeroes(final int[] in, final int samples) {
		int[] ret = new int[in.length + samples];
		for(int i=0; i<in.length; i++) {
			ret[i+samples] = in[i];
		}
		return ret;
	}
	
	/**
	 * Everything in the array above max will be interpolated between 
	 * the next samples in frequency axis.
	 * 
	 * @param in data array
	 * @param max maximum permitted value
	 * @return amount of interpolated samples
	 */
	public int limit(final double[][] in, final double max) {
		int ret = 0;
		for(int i=0; i<in.length; i++) {
			for(int j=0; j<in[i].length; j++) {
				if (in[i][j] > max) {
					interpolate(in[i], j, max);
					ret++;
				}
			}
		}
		return ret;
	}

	/**
	 * Search left and right of in[index] and interpolate index between the nearest found samples < max.
	 * 
	 * @param in
	 * @param index
	 * @param max
	 */
	private void interpolate(double[] in, int index, double max) {
		int l = index;
		int h = index;
		while (in[l] > max && l>0) l--;
		while (in[h] > max && h<in.length-1) h++;
		double low = (in[l] < max) ? in[l] : max;
		double high = (in[h] < max) ? in[h] : max;
		in[index] = (low + high) / 2; 
	}
	
	/**
	 * Normalize the frames to [0,1] independently per frame.
	 * 
	 * @param in data array
	 */
	public void normalizePerFrame(double[][] in) {
		for(int i=0; i<in.length; i++) {
			double max = Double.MIN_VALUE;
			for(int j=0; j<in[i].length; j++) {
				if (in[i][j] > max) max = in[i][j];
			}
			for(int j=0; j<in[i].length; j++) {
				in[i][j]/=max;
			}
		}
	}
}
