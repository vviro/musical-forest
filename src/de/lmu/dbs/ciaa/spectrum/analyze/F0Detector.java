package de.lmu.dbs.ciaa.spectrum.analyze;

/**
 * Basic f0 detection, based on Anssi Klapuri´s Algorithm. Needs CQT input coefficients.
 * 
 * TODO not optimized at all: performance is bad, threshold 
 *      algorithm is not suitable to detect wrong f0s ...
 *      this class is not used in the regular implementation of the program.
 * 
 * @author Thomas Weber
 *
 */
public class F0Detector {

	/**
	 * Contains the indexes of the natural harmonics, used for convolution
	 */
	private double[] frequencies;
	
	/**
	 * 
	 */
	public F0Detector(final double[] frequencies, final int amountOvertones) {
		this.frequencies = frequencies;
	}
	
	/**
	 * Basic polyphonic f0 detection, based on Anssi Klapuri´s Algorithm.
	 * 
	 * @param in
	 * @param voices number of iterations (number of f0s being detected)
	 * @return
	 */
	public double[] getF0Poly(final double[] in, final int voices) {
		double threshold = 0.4;
		double[] data = new double[in.length];
		for(int i=0; i<in.length; i++) {
			data[i] = in[i];
		}
		double[][] f0s = new double[voices][]; // 0:bin, 1:magnitude
		double max = -Double.MAX_VALUE; //Double.MIN_VALUE;
		for(int i=0; i<voices; i++) {
			f0s[i] = getF0(data);
			if (f0s[i][1] > max) max = f0s[i][1];
			if (f0s[i][0] >= 0) {
				for(int j=0; j<data.length; j++) {
					// remove overtones of f0
					for(int k=1; k<10; k++) {
						int bin = getBin(frequencies[(int)f0s[i][0]]*k);
						if (bin >= 0) data[bin] = 0;
					}
				}
			}
		}
		double[] ret = new double[in.length];
		for(int i=0; i<f0s.length; i++) {
			f0s[i][1]/=max;
			if (f0s[i][0] >= 0 && f0s[i][1] > threshold) ret[(int)f0s[i][0]] = 1;
		}
		return ret;
	}
	
	/**
	 * Basic monophonic f0 detection
	 * 
	 * @param in data array containing the result of peak 
	 *        detection (see DifferentialAnalyzer.getPeak method), normalized to [0,1]
	 * @return {0: the index of f0, or -1 if none, 1: magnitude of the index}
	 */
	public double[] getF0(final double[] in) {
		double threshold = 0.1;
		double[] mag = new double[in.length];
		int bin;
		for(int f=0; f<frequencies.length && frequencies[f] < 4000; f++) {
			//double freq = ;
			for(int i=1; i<10; i++) {
				bin = getBin(frequencies[f]*i);
				if (bin >= 0 && in[f] > threshold) mag[f]+= in[bin];
			}
		}
		double max = -Double.MAX_VALUE;
		int maxBin = -1;
		for(int i=0; i<mag.length; i++) {
			// Get max only
			if(mag[i] > max) {
				max = mag[i];
				maxBin = i;
			}
		}
		double[] ret = new double[2];
		ret[0] = maxBin;
		ret[1] = max;
		return ret;
	}
	
	/**
	 * Returns the bin of a frequency.
	 * 
	 * @param freq
	 * @return
	 */
	private int getBin(final double freq) {
		for(int i=0; i<frequencies.length; i++) {
			if (frequencies[i] >= freq) 
				return i;
		}
		return -1;
	}
}
