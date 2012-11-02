package de.lmu.dbs.ciaa.spectrum.analyze;

/**
 * Some routines to calculate with the (discrete) "differential" of arrays.
 * 
 * @author Thomas Weber
 */
public class DifferentialAnalyzer {

	/**
	 * Calculate the (discrete) differential of in.
	 *  
	 * @param in
	 * @param shiftByMaximum if true, output will be raised by the maximum, to avoid 
	 *        negative values (for evaluation use)
	 * @return
	 */
	public double[] getDifferential(double[] in, boolean shiftByMaximum) {
		double[] ret = new double[in.length];
		double min = Double.MAX_VALUE; 
		for(int i=0; i<in.length-1; i++) {
			ret[i] = in[i+1] - in[i];
			if (shiftByMaximum && ret[i] < min) min = ret[i];
		}
		if (shiftByMaximum) {
			min = Math.abs(min);
			for(int i=0; i<in.length-1; i++) {
				ret[i]+= min;
			}
		}
		return ret;
	}
	
	/**
	 * Calculate peak detection. Basically, this equals the 
	 * discrete differential maxima of the input array.
	 * 
	 * @param in sample array
	 * @param filterThreshold a value to filter small peaks by absulute 
	 *        value. Use a value <= 0.0 if no filtering is wanted.
	 * @return sample array (peaks)
	 */
	public double[] getPeaks(double[] in) {
		double[] ret = new double[in.length];
		int dir = 1;
		for(int i=0; i<in.length-1; i++) {
			//ret[i] = 0;
			if(in[i] > in[i+1] && dir == 1) {
				dir = -1;
				ret[i] = in[i];
			}
			if(in[i] <= in[i+1] && dir == -1) {
				dir = 1;
			}
		}
		return ret;
	}
	
}
