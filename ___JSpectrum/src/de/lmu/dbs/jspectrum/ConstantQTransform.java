package de.lmu.dbs.jspectrum;

/**
 * Copyright (c) 2006, Karl Helgason
 * 
 * 2007/1/8 modified by p.j.leonard
 * 2012 modified by Thomas Weber
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.File;

import de.lmu.dbs.jspectrum.util.FileIO;

import rasmus.interpreter.sampled.util.FFT;

/**
 * Implementation of Short Time Constant Q Transform
 * <br><br>
 * References:
 * <br><br>
 * Judith C. Brown, <br>
 * Calculation of a constant Q spectral transform, J. Acoust. Soc. Am., 89(1):
 * 425-434, 1991. <br>
 * see http://www.wellesley.edu/Physics/brown/pubs/cq1stPaper.pdf
 * <br><br>
 * Judith C. Brown and MillerS. Puckette,<br>
 * An efficient algorithm for the calculation of a constant Q transform, J.<br>
 * Acoust. Soc. Am., Vol. 92, No. 5, November 1992<br>
 * see http://www.wellesley.edu/Physics/brown/pubs/effalgV92P2698-P2701.pdf<br>
 * <br><br>
 * Benjamin Blankertz,<br>
 * The Constant Q Transform<br>
 * see http://wwwmath1.uni-muenster.de/logik/org/staff/blankertz/constQ/constQ.pdf
 * <br><br>
 * mods by p.j.leonard<br>
 * 0 - default centered quantized freq.<br>
 * 1 - <br>
 * 3 - j centered samples <br>
 * 4 - original <br>
 * <br><br>
 * Mods by T.Weber: <br>
 * - code cleaning, javadoc etc.<br>
 * - bug fix for imaginary parts in calc output<br>
 * - performance optimization in calc function(s)
 * - added calcMagnitude function, optimized for ready-to-use output format (non-complex)<br>
 * - added divideFFT parameter to lower FFT window (to increase performance)<br>
 * - buffer generated kernels to file(s) (if bufferLocation folder path is given)<br>
 * <br><br>
 * @author Karl Helgason, P. J. Leonard, Thomas Weber
 */
public class ConstantQTransform implements Transform {

	/**
	 * Constant Q
	 */
	private double q; 

	/**
	 * Number of output bands
	 */
	private int k; 

	/**
	 * FFT size
	 */
	private int fftlen; 

	/**
	 * Frequencies of bins
	 */
	private double[] freqs;

	/**
	 * Kernels
	 */
	private double[][] qKernel;

	/**
	 * Kernel indices
	 */
	private int[][] qKernel_indexes;

	/**
	 * FFT calculator object
	 */
	private FFT fft;

	/**
	 * Sample rate of audio
	 */
	private double sampleRate = 44100;

	/**
	 * Min frequency
	 */
	private double minFreq = 100;

	/**
	 * Max frequency 
	 */
	private double maxFreq = 3000;

	/**
	 * Number of bins per octave
	 */
	private double binsPerOctave = 12;

	/**
	 * Lower number, better quality !!! 0 = best
	 */
	private double threshold = 0.001;

	/**
	 *
	 */
	private double spread = 1.0;

	/**
	 * divide FFT length by this
	 */
	private double divideFFT = 1.0; 
	
	/**
	 * Directory for buffer files
	 */
	private String kernelBufferLocation = null; 
	
	private boolean buffered = false;
	
	public static final String BUFFER_PREFIX = "cqt_";
	public static final String BUFFER_POSTFIX_KERNELS = "_kernels";
	public static final String BUFFER_POSTFIX_INDEX = "_index";
	public static final String BUFFER_POSTFIX_FREQS = "_freqs";
	
	/**
	 * 
	 * 
	 * @param sampleRate
	 * @param minFreq
	 * @param maxFreq
	 * @param binsPerOctave
	 * @throws Exception
	 */
	public ConstantQTransform(double sampleRate, double minFreq, double maxFreq,
			double binsPerOctave) throws Exception {
		this.sampleRate = sampleRate;
		this.minFreq = minFreq;
		this.maxFreq = maxFreq;
		this.binsPerOctave = binsPerOctave;
		init();
	}

	/**
	 * 
	 * 
	 * @param sampleRate
	 * @param minFreq
	 * @param maxFreq
	 * @param binsPerOctave
	 * @param threshold
	 * @param spread
	 * @throws Exception
	 */
	public ConstantQTransform(double sampleRate, double minFreq, double maxFreq,
			double binsPerOctave, double threshold,double spread) throws Exception {
		this.sampleRate = sampleRate;
		this.minFreq = minFreq;
		this.maxFreq = maxFreq;
		this.binsPerOctave = binsPerOctave;
		this.threshold = threshold;
		this.spread = spread;
		init();
	}

	/**
	 * 
	 * @param sampleRate
	 * @param minFreq
	 * @param maxFreq
	 * @param binsPerOctave
	 * @param threshold
	 * @param spread
	 * @param divideFFT
	 * @throws Exception
	 */
	public ConstantQTransform(double sampleRate, double minFreq, double maxFreq,
			double binsPerOctave, double threshold,double spread, double divideFFT) throws Exception {
		this.sampleRate = sampleRate;
		this.minFreq = minFreq;
		this.maxFreq = maxFreq;
		this.binsPerOctave = binsPerOctave;
		this.threshold = threshold;
		this.spread = spread;
		this.divideFFT = divideFFT;
		init();
	}
	
	/**
	 * Create instance with kernel file buffering.
	 * 
	 * @param sampleRate
	 * @param minFreq
	 * @param maxFreq
	 * @param binsPerOctave
	 * @param threshold
	 * @param spread
	 * @param divideFFT
	 * @param kernelBufferLocation
	 * @throws Exception
	 */
	public ConstantQTransform(double sampleRate, double minFreq, double maxFreq,
			double binsPerOctave, double threshold,double spread, double divideFFT, String kernelBufferLocation) throws Exception {
		this.sampleRate = sampleRate;
		this.minFreq = minFreq;
		this.maxFreq = maxFreq;
		this.binsPerOctave = binsPerOctave;
		this.threshold = threshold;
		this.spread = spread;
		this.divideFFT = divideFFT;
		this.kernelBufferLocation = kernelBufferLocation;
		init();
	}

	/**
	 * Generate general kernel data. Buffered to file, if wanted.
	 * 
	 * @throws Exception
	 */
	private void init() throws Exception {
		// Calculate Constant Q
		q = 1.0 / (Math.pow(2, 1.0 / binsPerOctave) - 1.0) / spread;
		// Calculate number of output bins
		k = (int) Math.ceil(binsPerOctave * Math.log(maxFreq / minFreq)
				/ Math.log(2));
		// Calculate length of FFT
		double calc_fftlen = Math.ceil(q * sampleRate / minFreq);
		fftlen = (int)(Math.pow(2, Math.ceil(Math.log(calc_fftlen)
				/ Math.log(2)))/this.divideFFT);
		// Create FFT object
		fft = new FFT(fftlen);
		// Load buffer
		if (loadBuffer()) {
			buffered = true;
			return;
		}
		qKernel = new double[k][];
		qKernel_indexes = new int[k][];
		freqs = new double[k];
		// Calculate Constant Q kernels
		double[] temp = new double[fftlen * 2];
		double[] ctemp = new double[fftlen * 2];
		int[] cindexes = new int[fftlen];
		for (int i = 0; i < k; i++) {
			double[] sKernel = temp;
			// Calculate the frequency of current bin
			freqs[i] = minFreq * Math.pow(2, i / binsPerOctave);
			double len = q * sampleRate / freqs[i];
			for (int j = 0; j < fftlen / 2; j++) {
				double aa;
				aa = (double) (j + 0.5) / len;
				if (aa < .5) {
					double a = 2.0 * Math.PI * aa;
					double window = 0.5 * (1.0 + Math.cos(a)); // Hanning
					window /= len;
					// Calculate kernel
					double x = 2.0 * Math.PI * freqs[i] * (j + 0.5D) / sampleRate;
					sKernel[fftlen + j * 2] = window * Math.cos(x);
					sKernel[fftlen + j * 2 + 1] = window * Math.sin(x);
				} else {
					sKernel[fftlen + j * 2] = 0.0;
					sKernel[fftlen + j * 2 + 1] = 0.0;
				}
			}
			// reflect to genereate first half
			int halfway = fftlen / 2;
			for (int j = 0; j < halfway; j++) {
				int i1 = halfway - j - 1;
				int i2 = halfway + j;
				sKernel[i1 * 2] = sKernel[2 * i2];
				sKernel[i1 * 2 + 1] = -sKernel[2 * i2 + 1];
			}
			// Perform FFT on kernel
			fft.calc(sKernel, -1);
			// Remove all zeros from kernel to improve performance
			double[] cKernel = ctemp;
			int k = 0;
			for (int j = 0, j2 = sKernel.length - 2; j < sKernel.length / 2; j += 2, j2 -= 2) {
				double absval = Math.sqrt(sKernel[j] * sKernel[j]
						+ sKernel[j + 1] * sKernel[j + 1]);
				absval += Math.sqrt(sKernel[j2] * sKernel[j2] + sKernel[j2 + 1]
						* sKernel[j2 + 1]);
				if (absval > threshold) {
					cindexes[k] = j;
					cKernel[2 * k] = sKernel[j] + sKernel[j2];
					cKernel[2 * k + 1] = sKernel[j + 1] + sKernel[j2 + 1];
					k++;
				}
			}
			sKernel = new double[k * 2];
			int[] indexes = new int[k];
			for (int j = 0; j < k * 2; j++)
				sKernel[j] = cKernel[j];
			for (int j = 0; j < k; j++)
				indexes[j] = cindexes[j];
			// Normalize fft output
			for (int j = 0; j < sKernel.length; j++)
				sKernel[j] /= fftlen;
			// Perform complex conjugate on sKernel
			for (int j = 1; j < sKernel.length; j += 2)
				sKernel[j] = -sKernel[j];
			qKernel_indexes[i] = indexes;
			qKernel[i] = sKernel;
			// Write buffer
			writeBuffer();
		}
	}

	/**
     *
     * Take a buff_in of plain audio samples and calculate the constant Q coeffs.
     * <br><br>
     * Output format:<br>
     * Indexes i*2: real<br>
     * Indexes i*2+1: imaginary<br>
     *
     * @param buff_in
     * @param buff_out length: 2*(number of frequency bins), holds 
     *        complex representation of each bin (real and imaginary)
     */
	public void calc(double[] buff_in, double[] buff_out) {
		fft.calcReal(buff_in, -1);
		double t_r, t_i;
		double[] kernel;
		int[] indexes;
		int jj;
		for (int i = 0; i < qKernel.length; i++) {
			kernel = qKernel[i];
			indexes = qKernel_indexes[i];
			t_r = 0;
			t_i = 0;
			for (int j = 0, l = 0; j < kernel.length; j += 2, l++) {
				jj = indexes[l];
				// COMPLEX: T += B * K
				t_r += buff_in[jj] * kernel[j] - buff_in[jj + 1] * kernel[j + 1];
				t_i += buff_in[jj] * kernel[j + 1] + buff_in[jj + 1] * kernel[j];
			}
			buff_out[i * 2] = t_r;
			buff_out[i * 2 + 1] = t_i;
		}
	}

	/**
     * Take a buff_in of plain audio samples and calculate the constant Q coeffs,
     * optimized for ready-to-use output format (non-complex).
     *
     * @param buff_in
     * @param buff_out length: number of frequency bins, holds magnitude of each bin
     */
	public void calcMagnitude(double[] buff_in, double[] buff_out) {
		fft.calcReal(buff_in, -1);
		double t_r, t_i;
		double[] kernel;
		int[] indexes;
		int jj;
		for (int i = 0; i < qKernel.length; i++) {
			kernel = qKernel[i];
			indexes = qKernel_indexes[i];
			t_r = 0;
			t_i = 0;
			for (int j = 0, l = 0; j < kernel.length; j += 2, l++) {
				jj = indexes[l];
				// COMPLEX: T += B * K
				t_r += buff_in[jj] * kernel[j] - buff_in[jj + 1] * kernel[j + 1];
				t_i += buff_in[jj] * kernel[j + 1] + buff_in[jj + 1] * kernel[j];
			}
			buff_out[i] = Math.sqrt(t_r*t_r + t_i*t_i);
		}
	}

	/**
	 * Saves current kernels to a kernel buffer file.
	 * 
	 * @throws Exception
	 */
	private void writeBuffer() throws Exception {
		if (kernelBufferLocation != null) {
			String fname = getBufferName();
			FileIO<double[][]> dio = new FileIO<double[][]>();
			dio.save(fname+BUFFER_POSTFIX_KERNELS, this.qKernel);
			FileIO<int[][]> iio = new FileIO<int[][]>();
			iio.save(fname+BUFFER_POSTFIX_INDEX, this.qKernel_indexes);
			FileIO<double[]> fio = new FileIO<double[]>();
			fio.save(fname+BUFFER_POSTFIX_FREQS, this.freqs);
		}
		
	}
	
	/**
	 * Retrieve kernel buffer data if exists.
	 * 
	 * @return if buffer has been loaded successfully
	 * @throws Exception
	 */
	private boolean loadBuffer() throws Exception {
		if (kernelBufferLocation != null) {
			String fname = getBufferName();
			if (bufferExists(fname)) {
				FileIO<double[][]> dio = new FileIO<double[][]>();
				this.qKernel = dio.load(fname+BUFFER_POSTFIX_KERNELS);
				FileIO<int[][]> iio = new FileIO<int[][]>();
				this.qKernel_indexes = iio.load(fname+BUFFER_POSTFIX_INDEX);
				FileIO<double[]> fio = new FileIO<double[]>();
				this.freqs = fio.load(fname+BUFFER_POSTFIX_FREQS);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Detects the existence of buffered data for this configuration
	 * 
	 * @param fname
	 * @return
	 */
	private boolean bufferExists(final String fname) {
		return (new File(fname+BUFFER_POSTFIX_KERNELS)).exists() 
		    && (new File(fname+BUFFER_POSTFIX_INDEX)).exists() 
		    && (new File(fname+BUFFER_POSTFIX_FREQS)).exists();
	}
	
	/**
	 * Returns the corresponding buffer file name.
	 * 
	 * @return
	 */
	private String getBufferName() {
		String fname = kernelBufferLocation + BUFFER_PREFIX;
		fname+= sampleRate + "_";
		fname+= minFreq + "_";
		fname+= maxFreq + "_";
		fname+= binsPerOctave + "_";
		fname+= threshold + "_";
		fname+= spread + "_";
		fname+= divideFFT;
		return fname;
	}
	
	/**
	 * Returns the FFT calculator instance
	 * 
	 * @return
	 */
	public FFT getFFT() {
		return fft;
	}

	/**
	 * returns an array with the bin requencies
	 * 
	 * @return
	 */
	public double[] getFreqs() {
		return freqs;
	}

	/**
	 * Returns the FFT window length
	 * 
	 * @return
	 */
	public int getWindowSize() {
		return fftlen;
	}

	/**
	 * Returs the number of output bands
	 * 
	 * @return
	 */
	public int getNumberOfOutputBands() {
		return freqs.length;
	}

	/**
	 * Returns if the kernels were loaded from buffer files or not.
	 * 
	 * @return
	 */
	public boolean isBuffered() {
		return buffered;
	}
}
