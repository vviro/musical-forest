package de.lmu.dbs.jspectrum;

import de.lmu.dbs.jspectrum.util.Window;

/**
 * Constant Q Transform. Generates full 3d spectra as data matrices.
 * 
 * @author Thomas Weber
 *
 */
public class ConstantQTransform extends Transform {

	/**
	 * Size of the windows used to assemble the data matrix
	 */
	private int windowSize; 
	
	/**
	 * Number of frequency bands
	 */
	private int bins;
	
	/**
	 * CQT processor (processes one frame)
	 */
	private ShortTimeConstantQTransform stcqt = null;
	
	/**
	 * Create CQT instance.
	 * 
	 * @param sampleRate audio sample rate
	 * @param fmin minimum frequency
	 * @param fmax maximum frequency
	 * @param binsPerOctave bands per octave to examine
	 * @throws Exception
	 */
	public ConstantQTransform(final double sampleRate, final double fmin, final double fmax, final double binsPerOctave) throws Exception {
		this(sampleRate, fmin, fmax, binsPerOctave, 0.001, 1.0, 1.0, null);
	}

	/**
	 * Create CQT instance.
	 * 
	 * @param sampleRate audio sample rate
	 * @param fmin minimum frequency
	 * @param fmax maximum frequency
	 * @param binsPerOctave bands per octave to examine
	 * @param threshold lower numbers are better!
	 * @param spread
	 * @throws Exception
	 */
	public ConstantQTransform(final double sampleRate, final double fmin, final double fmax, final double binsPerOctave, final double threshold, final double spread) throws Exception {
		this(sampleRate, fmin, fmax, binsPerOctave, threshold, spread, 1.0, null);
	}

	/**
	 * Create CQT instance.
	 * 
	 * @param sampleRate audio sample rate
	 * @param fmin minimum frequency
	 * @param fmax maximum frequency
	 * @param binsPerOctave bands per octave to examine
	 * @param threshold lower numbers are better!
	 * @param spread
	 * @param divideFFT divide FFT window to increase performance
	 * @throws Exception
	 */
	public ConstantQTransform(final double sampleRate, final double fmin, final double fmax, final double binsPerOctave, final double threshold, final double spread, final double divideFFT) throws Exception {
		this(sampleRate, fmin, fmax, binsPerOctave, threshold, spread, divideFFT, null);
	}

	/**
	 * Create CQT instance.
	 * 
	 * @param sampleRate audio sample rate
	 * @param fmin minimum frequency
	 * @param fmax maximum frequency
	 * @param binsPerOctave bands per octave to examine
	 * @param threshold lower numbers are better!
	 * @param spread
	 * @param divideFFT divide FFT window to increase performance
	 * @param kernelBufferLocation folder where the kernel buffer files are stored
	 * @throws Exception
	 */
	public ConstantQTransform(final double sampleRate, final double fmin, final double fmax, final double binsPerOctave, final double threshold, final double spread, final double divideFFT, final String kernelBufferLocation) throws Exception {
		stcqt = new ShortTimeConstantQTransform(sampleRate, fmin, fmax, binsPerOctave, threshold, spread, divideFFT, kernelBufferLocation);
		windowSize = stcqt.getWindowSize();
		bins = stcqt.getNumberOfOutputBands();
	}

	/**
	 * Returns a matrix containing cqt data of the given audio sample. The data is stepped through and 
	 * the windowed to get the 2d matrix output representing the spectrum over time.
	 * 
	 * @param samples audio samples 
	 * @param step the amount of samples of one analysis step (each step calculates one window)
	 * @param windowFunction the windowing function used
	 * @return data matrix (shape: [frame][number of frequency bins, filled with magnitude values])
	 */
	@Override
	public double[][] calculate(final int[] samples, final int step, final Window windowFunction) {
		int frames = (int)Math.floor(samples.length / step); 
		double[][] data = new double[frames][bins]; 
		double[] frameBuffer = new double[windowSize];
		int index;
		for(int frame=0; frame<frames; frame++) {
			for(int h=0; h<windowSize; h++) {
				index = frame*step+h-windowSize/2;
				if (index < 0 || index >= samples.length) {
					frameBuffer[h] = 0.0;
				} else {
					frameBuffer[h] = (double)samples[index];
				}
			}
			windowFunction.apply(frameBuffer);
			stcqt.calcMagnitude(frameBuffer, data[frame]);
		}
		return data;
	}

	/**
	 * Returns the frequencies in hertz, corresponding to the second level of the 
	 * data matrix returned by calculate(..).
	 * 
	 * @return 
	 */
	public double[] getFrequencies() {
		return stcqt.getFreqs();
	}
	
	/**
	 * Returns the FFT window size (not the step!).
	 * 
	 * @return
	 */
	@Override
	public int getWindowSize() {
		return windowSize;
	}

	/**
	 * Returns if the kernels were loaded from buffer files or not.
	 * 
	 * @return
	 */
	public boolean isBuffered() {
		return stcqt.isBuffered();
	}

}
