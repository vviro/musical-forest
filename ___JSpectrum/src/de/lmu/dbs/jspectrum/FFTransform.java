package de.lmu.dbs.jspectrum;

import rasmus.interpreter.sampled.util.FFT;

/**
 * Short-Time FFT; Wrapper for the RasmusDSP FFT class (rasmus.interpreter.sampled.util.FFT).
 * 
 * @author Thomas Weber
 *
 */
public class FFTransform implements Transform {

	/**
	 * External FFT processor
	 */
	private FFT fft;
	
	/**
	 * Window Size
	 */
	private int fftlen;
	
	/**
	 * 
	 * @param fftlen
	 */
	public FFTransform(int fftlen) {
		fft = new FFT(fftlen);
	}
	
	/**
    *
    * Take a buff_in of plain audio samples and calculate the FFT coeffs.
    * <br><br>
    * Output format:<br>
    * Indexes i*2: real<br>
    * Indexes i*2+1: imaginary<br>
    *
    * @param buff_in
    * @param buff_out length: windowSize, holds complex 
    *        representation of each coeff (real and imaginary)
    */
	public void calc(double[] buffIn, double[] buffOut) {
		fft.calcReal(buffIn, -1);
		for(int i=0; i<buffIn.length; i++) {
			if (i < buffOut.length) buffOut[i] = buffIn[i];
		}
	}

	/**
     * Take a buff_in of plain audio samples and calculate the FFT coeffs,
     * optimized for ready-to-use output format (non-complex).
     *
     * @param buff_in
     * @param buff_out length: number of frequency bins, holds magnitude of each coeff
     */
	public void calcMagnitude(double[] buffIn, double[] buffOut) {
		fft.calcReal(buffIn, -1);
		for(int i=0; i<buffIn.length; i+=2) {
			if (buffOut.length <= i/2) return;
			buffOut[i/2] = Math.sqrt(buffIn[i]*buffIn[i] + buffIn[i+1]*buffIn[i+1]);
		}
	}

	/**
	 * Returns FFT window size
	 * 
	 * @return
	 */
	public int getWindowSize() {
		return fftlen;
	}

}
