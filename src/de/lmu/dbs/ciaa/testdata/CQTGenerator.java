package de.lmu.dbs.ciaa.testdata;

import java.awt.Color;
import java.io.File;

import de.lmu.dbs.ciaa.spectrum.*;
import de.lmu.dbs.ciaa.spectrum.analyze.DifferentialAnalyzer;
import de.lmu.dbs.ciaa.util.*;

/**
 * Batch creation of cqt raw files and/or png files from wav files.
 * 
 * @author Thomas Weber
 *
 */
public class CQTGenerator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		boolean saveCqt = true;
		boolean savePng = true;
		boolean savePeak = true;
		File srcFolder = new File("testdata/poly/wav/");
		File cqtFolder = new File("testdata/poly/cqt/");
		File pngFolder = new File("testdata/poly/png/");
		String cqtPostfix = ".cqt";
		String peakPostfix = ".peak";
		String pngPostfix = ".png";
		String wavPostfix = ".wav";
		String freqFileName = "testdata/frequencies";
		Scale scale = new LogScale(10);
		int step = 256; // Samples per frame

		// CQT parameters (see ConstantQTransform javadoc for details...)
		double fmin = 80.0;
		double fmax = 10000.0;
		double binsPerOctave = 48.0;
		double threshold = 0.05;
		double spread = 1.0;
		double divideFFT = 4.0;
		String cqtBufferLocation = "cqtbuff/";

		try {
			// Create profiling tool etc.
			RuntimeMeasure m = new RuntimeMeasure(System.out);
			if (!saveCqt && !savePng) throw new Exception("Nothing to save");
			FileIO<byte[][]> io = new FileIO<byte[][]>();

			File[] srcFiles = srcFolder.listFiles();
			Transform transformation = null; 
			double sampleRate = 0;
			boolean freqSaved = false;
			for(int i=0; i<srcFiles.length; i++) {
				File wavfile = srcFiles[i];
				if (!wavfile.isFile()) continue;
				if (!wavfile.getName().endsWith(wavPostfix)) {
					continue;
				}
				File cqtfile = new File(cqtFolder.getAbsolutePath() + File.separator + wavfile.getName().replace(wavPostfix, cqtPostfix));
				File pngfile = new File(pngFolder.getAbsolutePath() + File.separator + wavfile.getName().replace(wavPostfix, pngPostfix));
				File peakfile = new File(cqtFolder.getAbsolutePath() + File.separator + wavfile.getName().replace(wavPostfix, peakPostfix));
				out("#### Processing file " + i + " ####");
				
				// Load sample
				Sample src = new WaveSample(wavfile);
				m.measure("Loaded sample from " + wavfile.getName());
				
				// Init transformation
				if ((double)src.getSampleRate() != sampleRate || transformation == null) {
					sampleRate = (double)src.getSampleRate();
					transformation = new ConstantQTransform(sampleRate, fmin, fmax, binsPerOctave, threshold, spread, divideFFT, cqtBufferLocation);
					m.measure("Initialized transformation");
				}
				// Save frequency array (only once)
				if (!freqSaved) {
					FileIO<double[]> fio = new FileIO<double[]>();
					fio.save(freqFileName, transformation.getFrequencies());
					freqSaved = true;
					m.measure("Saved frequency table to " + freqFileName);
				}

				// Make mono
				int[] mono = src.getLeftBuffer(); // TODO sum channels
				m.measure("Extracted left channel, total samples: " + mono.length);

				// Calculate transformation
				double[][] data = transformation.calculate(mono, step, new HammingWindow(transformation.getWindowSize()));
				m.measure("Finished transformation");

				// peak detection
				double[][] dataPeak = new double[data.length][];
				DifferentialAnalyzer p = new DifferentialAnalyzer();
				for(int j=0; j<data.length; j++) {
					dataPeak[j] = p.getPeaks(data[j]);
				}
				m.measure("Finished peak detection");

				// Normalize and scale
				if (scale != null) {
					ArrayUtils.normalize(data); 
					ArrayUtils.scale(data, scale);
				}
				
				ArrayUtils.normalize(data, (double)Byte.MAX_VALUE);
				byte[][] byteData = ArrayUtils.toByteArray(data);
				ArrayUtils.normalize(dataPeak, (double)Byte.MAX_VALUE);
				byte[][] peakData = ArrayUtils.toByteArray(dataPeak);
				m.measure("Prepared transformation data");

				if (saveCqt) {
					io.save(cqtfile.getAbsolutePath(), byteData);
					m.measure("Saved raw cqt data to " + cqtfile.getName());
				}

				if (savePeak) {
					io.save(peakfile.getAbsolutePath(), peakData);
					m.measure("Saved raw cqt data to " + peakfile.getName());
				}

				// Save PNG image of the results
				if (savePng) {
					SpectrumToImage img = new SpectrumToImage(data.length, data[0].length, 1);
					out("--> Max Data:  " + img.add(byteData, Color.WHITE, null));
					out("--> Max Peak:  " + img.add(peakData, Color.GREEN, null, 0));
					img.save(pngfile);
					m.measure("Saved image to " + pngfile.getName());
				}
			}
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
