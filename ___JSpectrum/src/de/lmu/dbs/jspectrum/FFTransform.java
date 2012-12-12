package de.lmu.dbs.jspectrum;

import de.lmu.dbs.jspectrum.util.Window;

/**
 * FFT Transformation.
 * 
 * @author Thomas Weber
 *
 */
public class FFTransform extends Transform {

	/**
	 * FFT size
	 */
	private int fftlen;
	
	/**
	 * 
	 */
	private ShortTimeFFTransform stfft; 
	
	/**
	 * zooms in towards the low frequencies if > 1.0
	 */
	private double zoomOutput = 1.0;

	/**
	 * Audio sample rate (to calculate frequencies)
	 */
	private double sampleRate;
	
	/**
	 * FFT Transformation
	 * 
	 * @param sampleRate audio sample rate (to calculate frequencies)
	 * @param fftlen FFT window size
	 * @param zoomOutput zooms in towards the low frequencies if > 1.0
	 */
	public FFTransform(double sampleRate, int fftlen, double zoomOutput) {
		this.fftlen = fftlen;
		this.zoomOutput = zoomOutput;
		this.sampleRate = sampleRate;
		stfft = new ShortTimeFFTransform(fftlen);
	}

	/**
	 * Returns a matrix containing fft data of the given audio sample. The data is stepped through and 
	 * the windowed to get the 2d matrix output representing the spectrum over time.
	 * 
	 * @param samples audio samples 
	 * @param step the amount of samples of one analysis step (each step calculates one window)
	 * @param windowFunction the windowing function used
	 * @return data matrix (shape: [frame][fftlen, filled with magnitude values])
	 */
	@Override
	public double[][] calculate(int[] samples, int step, Window windowFunction) {
		int frames = (samples.length) / step - 1; 
		double[][] data = new double[frames][(int)(fftlen*zoomOutput/2)]; 
		double[] frameBuffer = new double[fftlen];
		int beg;
		for(int frame=0; frame<frames; frame++) {
			beg = frame*step;
			for(int h=0; h<fftlen; h++) {
				if (beg < samples.length) frameBuffer[h] = (double)samples[beg];
				beg++;
			}
			windowFunction.apply(frameBuffer);
			stfft.calcMagnitude(frameBuffer, data[frame]);
		}
		return data;
	}

	/**
	 * Returns the FFT window size (not the step!).
	 * 
	 * @return
	 */
	@Override
	public int getWindowSize() {
		return fftlen;
	}

	/**
	 * Returns the frequencies in hertz, corresponding to the second level of the 
	 * data matrix returned by calculate(..).
	 * <br><br>
	 * <b>WARNING:</b> Untested for FFT!
	 * 
	 * @return 
	 */
	@Override
	public double[] getFrequencies() {
		double[] ret = new double[fftlen];
		double fmax = sampleRate/2; // Nyquist frequency
		double step = fmax/fftlen;
		for(int i=0; i<ret.length; i++) {
			ret[i] = i*step;
		}
		return ret;
	}
}
