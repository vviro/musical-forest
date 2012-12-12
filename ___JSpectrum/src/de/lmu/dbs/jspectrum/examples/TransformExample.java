package de.lmu.dbs.jspectrum.examples;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import de.lmu.dbs.jspectrum.*;
import de.lmu.dbs.jspectrum.analyze.*;
import de.lmu.dbs.jspectrum.util.*;

/**
 * Example implementation for spectral transformations (FFT, CQT). Loads a WAV sample and saves its spectrum to a PNG file.
 * While the file names are parsed from the command line, the transformation parameters are hard coded in the main(..) method
 * for sense of simplicity, this is just an example...see parameters inside main method for further details of the 
 * Transformation parameters.
 * 
 * @author Thomas Weber
 *
 */
public class TransformExample {

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

		// CLI Parsing
		OptionSet options = parseArgs(args);

		// Feature switches
		boolean peak = false; // show peaks on top of transformation
		boolean differential = false; // show differential. This option hides the original transformation.
		boolean f0 = false; // show basic f0-detection 

		// PNG parameters
		String imgFile = (String)options.valueOf("out"); // PNG file to create
		int fspread = 1; // Scale factor for freq scale
		double scaleData = 1; // Scale raw transform data visualization color. Use 1 to disable.
		
		// Transformation general parameters 
		String wavFile = (String)options.valueOf("in"); //"../__CIAA/WaveFiles/Test8_Mix.wav"; // WAV file
		Transformations mode = Transformations.CQT; // Transformation to use (FFT or CQT)

		// CQT parameters XML file (see ConstantQTransform javadoc for details...)
		String settingsFile = "transformSettings.xml";

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

			// Load settings
			TransformParameters params = new TransformParameters();
			params.loadParameters(settingsFile);

			// Create buffer location
			File buffLoc = new File(params.cqtKernelBufferLocation);
			if (!buffLoc.exists()) {
				buffLoc.mkdirs();
				m.measure("Created cqt kernel buffer location: " + params.cqtKernelBufferLocation);
			}
			
			// Load sample
			Sample src = new WaveSample(new File(wavFile));
			m.measure("Loaded sample");

			// Init transformation
			Transform transformation = null;
			if (mode == Transformations.FFT) {
				transformation = new FFTransform((double)src.getSampleRate(), fftlen, zoomOutput);	
			} else {
				transformation = new ConstantQTransform((double)src.getSampleRate(), params.fMin, params.fMax, params.binsPerOctave, params.threshold, params.spread, params.divideFFT, params.cqtKernelBufferLocation);
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
			double[][] data = transformation.calculate(mono, params.step, new HammingWindow(transformation.getWindowSize()));
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
			
			// Save PNG image of the results
			ArrayToImage img = new ArrayToImage(data.length, data[0].length, fspread);
			Scale scale = new LogScale(10);
			if (differential)
				out("--> Max. Differential:  " + img.add(dataAbl, Color.YELLOW, scale)); // plain data, scaled with Log10				
			else {
				Color color = new Color((int)(255*scaleData),(int)(150*scaleData),0);
				out("--> Max Data:  " + img.add(data, color, scale)); // plain data, scaled with Log10
			}
			if (peak)
				out("--> Max. Peak: " + img.add(dataPeak, Color.CYAN, scale, 0)); // peak detection data, scaled with Log10
			if (f0)
				out("--> Max. F0:   " + img.add(dataF0, Color.WHITE, null, 0)); // peak detection data, scaled with Log10
			img.save(new File(imgFile));
			m.measure("Saved image to " + imgFile);
	        
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * CLI option parsing.
	 * 
	 * @param args
	 * @return
	 */
	private static OptionSet parseArgs(String[] args) {
		OptionSet options = null;
		try {
			OptionParser parser = new OptionParser() {
				{
					accepts("in", "Input Wave audio file. Bit and sample rates will be detected automatically.").withRequiredArg().required();
	                accepts("out", "Output PNG image file to store the spectral data.").withRequiredArg().required();
	                accepts("help", "Shows this help screen.").forHelp();
				}
			};
			options = parser.parse(args);
			if (options.has("help")) {
				parser.printHelpOn(System.out);
				System.exit(0);
			}
		} catch (OptionException e) {
			System.out.println(e.getMessage() + "; Try --help");
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(2);
		}
		return options;
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
