package de.lmu.dbs.ciaa.classifier.features;

import java.awt.Color;
import java.io.File;

import de.lmu.dbs.ciaa.classifier.core.ForestParameters;
import de.lmu.dbs.ciaa.spectrum.*;
import de.lmu.dbs.ciaa.spectrum.analyze.DifferentialAnalyzer;
import de.lmu.dbs.ciaa.spectrum.analyze.F0Detector;
import de.lmu.dbs.ciaa.util.ArrayUtils;
import de.lmu.dbs.ciaa.util.HammingWindow;
import de.lmu.dbs.ciaa.util.LogScale;
import de.lmu.dbs.ciaa.util.RuntimeMeasure;
import de.lmu.dbs.ciaa.util.Sample;
import de.lmu.dbs.ciaa.util.Scale;
import de.lmu.dbs.ciaa.util.ArrayToImage;
import de.lmu.dbs.ciaa.util.WaveSample;

/**
 * Example implementation for spectral transformations (FFT, CQT). Loads a WAV sample and saves its spectrum to a PNG file.
 * See parameters inside main method for details.
 * 
 * @author Thomas Weber
 *
 */
public class FeatureVisualizer {

	/**
	 * Transformation modes
	 * 
	 * @author Thomas Weber
	 *
	 */
	public static enum Transformations {FFT, CQT};
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// Feature switches
		boolean peak = true; // show peaks on top of transformation
		boolean differential = false; // show differential. This option hides the original transformation.
		boolean f0 = false; // show basic f0-detection 

		// PNG parameters
		String imgFile = "testimages/feature9.png"; // PNG file
		int fspread = 1; // Scale factor for freq scale
		//double scaleData = 1; // Scale raw transform data visualization color. Use 1 to disable.
		
		// Transformation general parameters 
		String wavFile = "testdata/mono/wav/mono3.wav";//WaveFiles/Test8_Mix.wav"; //args[1]; // WAV file
		Transformations mode = Transformations.CQT; // Transformation to use (FFT or CQT)
		int step = 256; // Samples per frame

		// CQT parameters (see ConstantQTransform javadoc for details...)
		double fmin = 80.0;
		double fmax = 10000.0;
		double binsPerOctave = 48.0;
		double threshold = 0.05;
		double spread = 1.0;
		double divideFFT = 4.0;
		String cqtBufferLocation = "cqtbuff/";

		// FFT parameters (see FFTransform javadoc for details...)
		int fftlen = 16384;
		double zoomOutput = 0.05;
		
		// Signal processing options (for additional analyzers, deactivated by default)
		int gateInputThreshold = 0; // Gate input signal before transform. Set 0 to disable.
		boolean limit = false; // Limit coefficients after transform to maxCoeff
		double maxCoeff = 1000; // Maximum coefficient for limiting, if limiting is switched on
		boolean normalize = false; // Normalize after transform 
		int f0voices = 5; // Max voices(iterations) of f0 detection

		try {
			// Create profiling tool
			RuntimeMeasure m = new RuntimeMeasure(System.out);

			// Load sample
			Sample src = new WaveSample(new File(wavFile));
			m.measure("Loaded sample");

			// Init transformation
			Transform transformation = null;
			if (mode == Transformations.FFT) {
				transformation = new FFTransform((double)src.getSampleRate(), fftlen, zoomOutput);	
			} else {
				transformation = new ConstantQTransform((double)src.getSampleRate(), fmin, fmax, binsPerOctave, threshold, spread, divideFFT, cqtBufferLocation);
			}
			
			m.measure("Initialized transformation");
			out("--> Window size: " + transformation.getWindowSize());
			
			// Make mono
			int[] mono = src.getLeftBuffer();
			m.measure("Extracted left channel, total samples: " + mono.length);

			// Gate
			if (gateInputThreshold > 0) {
				ArrayUtils.gate(mono, gateInputThreshold);
				m.measure("Gated input signal below " + gateInputThreshold);
			}
			
			// Calculate transformation
			double[][] data = transformation.calculate(mono, step, new HammingWindow(transformation.getWindowSize()));
			m.measure("Finished transformation");
			
			// Limit
			if (limit) {
				int limited = transformation.limit(data, maxCoeff);
				m.measure("Limited data to max. " + maxCoeff + "; Interpolated " + limited + " samples");
			}

			// Normalize
			if (normalize) {
				ArrayUtils.normalize(data);
				m.measure("Normalized transformation data");
			}

			//ArrayUtils.gate(data, 2.0/127.0);
			
			// Overtone peak detection
			double[][] dataPeak = new double[data.length][];
			DifferentialAnalyzer p = new DifferentialAnalyzer();
			if (peak || f0) {
				for(int i=0; i<data.length; i++) {
					dataPeak[i] = p.getPeaks(data[i]);
				}
				m.measure("Finished peak detection");
			}
			
			// Differential
			double[][] dataAbl = new double[data.length][];
			if (differential) {
				for(int i=0; i<data.length; i++) {
					dataAbl[i] = p.getDifferential(data[i], true);
				}
				m.measure("Finished differential transform");
			}
			
			// basic f0 detection
			double[][] dataF0 = new double[data.length][];
			if (f0) {
				ArrayUtils.normalize(dataPeak);
				F0Detector f0d = new F0Detector(transformation.getFrequencies(), 20);
				for(int i=0; i<data.length; i++) {
					dataF0[i] = f0d.getF0Poly(dataPeak[i], f0voices);
				}
				m.measure("Finished basic f0 detection");
			}
			
			Scale scale = new LogScale(10);
			ArrayUtils.normalize(data);
			ArrayUtils.scale(data, scale);
			
			ForestParameters pa = new ForestParameters();
			pa.xMin = 0;
			pa.xMax = 0;
			//pa.yMin = -10;
			//pa.yMax = 10;
			FeatureHarmonic5 f = new FeatureHarmonic5(pa);
			//float[] hf = {(float) 0.0, (float) 0.0, (float) 9.903322, (float) 0.0, (float) 0.0, (float) 0.0, (float) 2.6849601, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 6.452658, (float) 0.0, (float) 5.2687445, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0};
			//f.harmonicFactors = hf;
			
			/*ArrayUtils.normalize(data); // Normalize to [0,1]
			Scale scale = new LogScale(10);
			ArrayUtils.scale(data, scale); // Log scale
			*/
			ArrayUtils.normalize(data, (double)Byte.MAX_VALUE-1);
			byte[][] byteData = ArrayUtils.toByteArray(data);
			float[][] fData = new float[data.length][data[0].length];
			for(int x=0; x<data.length; x++) {
				for(int y=0; y<data[x].length; y++) {
					fData[x][y] = (f.evaluate(byteData, x, y) >= 1900865.2) ? 1 : 0;
				}
			}
/*
			ArrayUtils.normalize(fData, Byte.MAX_VALUE-1);
			byteData = new byte[data.length][data[0].length];
			for(int x=0; x<data.length; x++) {
				for(int y=0; y<data[x].length; y++) {
					byteData[x][y] = (byte)fData[x][y];
					if (byteData[x][y] < 0 ) System.out.println("Minus!! " + byteData[x][y] + " at " + x + "/" + y);
				}
			}
			float[][] fData2 = new float[data.length][data[0].length];
			for(int x=0; x<data.length; x++) {
				for(int y=0; y<data[x].length; y++) {
					fData2[x][y] = f.evaluate(byteData, x, y);
				}
			}
			fData = fData2;
			//*/

			/*
			float[][] fDataMax = new float[data.length][data[0].length];
			for(int x=0; x<data.length; x++) {
				float max = Float.MIN_VALUE;
				int maxi = -1;
				for(int y=0; y<data[x].length; y++) {
					if (fData[x][y] > max) {
						max = fData[x][y];
						maxi = y;
					}
				}
				if (maxi >= 0) fDataMax[x][maxi] = fData[x][maxi];
			}
			*/
			// Save PNG image of the results
			ArrayToImage img = new ArrayToImage(data.length, data[0].length, fspread);
			//Color color = new Color((int)(255*scaleData),(int)(150*scaleData),0);
			img.add(byteData, Color.WHITE);
			out("Max: " + img.add(fData, Color.GREEN, null, 0));
			//out("Max: " + img.add(fDataMax, Color.RED, scale, 0.1));
			img.save(new File(imgFile));
			m.measure("Saved image to " + imgFile);
	        
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Outputs the message to the console 
	 * 
	 * @param message the message
	 */
	private static void out(String message) {
		System.out.println(message);
	}
	
}
