package de.lmu.dbs.ciaa;

import java.awt.Color;
import java.io.File;

import de.lmu.dbs.ciaa.classifier.core.*;
import de.lmu.dbs.ciaa.classifier.core2d.*;
import de.lmu.dbs.ciaa.classifier.musicalforest.*;
import de.lmu.dbs.ciaa.midi.MIDIAdapter;
import de.lmu.dbs.ciaa.spectrum.ConstantQTransform;
import de.lmu.dbs.ciaa.spectrum.Transform;
import de.lmu.dbs.ciaa.util.*;

/**
 * 
 * @author Thomas Weber
 *
 */
public class ForestLoadedTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// XML file for program configuration
		String settingsFile = "settingsLoaded.xml";
		
		String testFile = "WaveFiles/Test8_Mix.wav"; // WAV file used to test the forest. Results are stored in a PNG file called <testFile>.png
		String testReferenceFile = "WaveFiles/MIDIFiles/Test8melody.mid"; // Test MIDI reference file. Has to be musically equal to testFile 
		double imgThreshold = 0.2; // Threshold to filter the normalized forest results in the PNG test output
		
		try {
			System.out.println("Java Heap size (maximum): " + ((double)Runtime.getRuntime().maxMemory() / (1024*1024)) + " MB");
			
			// Create profiling tool
			RuntimeMeasure m = new RuntimeMeasure(System.out);

			// Load settings
			Settings settings = new Settings(settingsFile);
			ForestParameters params = settings.getForestParameters();
			m.measure("Loaded settings from XML file");
			
			// Load frequency table (must be common for all samples)
			FileIO<double[]> fio = new FileIO<double[]>();
			String freqFileName = params.frequencyTableFile;
			params.frequencies = fio.load(freqFileName);
			m.measure("Loaded frequency table from " + freqFileName);

			// Grow forest with training part of data
			Forest forest;
			RandomTree2d treeFactory = new MusicalRandomTree(); 
			forest = Forest.load(params, params.workingFolder + File.separator + params.nodedataFilePrefix, 2, treeFactory);
			m.measure("Finished loading forest from folder: " + params.workingFolder);
			
			// Load sample
			Sample src = new WaveSample(new File(testFile));
			m.measure("Loaded sample");

			Transform transformation = new ConstantQTransform((double)src.getSampleRate(), params.fMin, params.fMax, params.binsPerOctave, params.threshold, params.spread, params.divideFFT, params.cqtBufferLocation);
			m.measure("Initialized transformation");
			
			// Make mono
			int[] mono = src.getLeftBuffer();
			m.measure("Extracted left channel, total samples: " + mono.length);

			// Calculate transformation
			double[][] data = transformation.calculate(mono, params.step, new HammingWindow(transformation.getWindowSize()));
			Scale scale = new LogScale(10);
			ArrayUtils.normalize(data); // Normalize to [0,1]
			ArrayUtils.scale(data, scale); // Log scale
			ArrayUtils.normalize(data, (double)Byte.MAX_VALUE-1); // Normalize back to [0,MAX_VALUE] 
			byte[][] byteData = ArrayUtils.toByteArray(data); // To byte array to use with forest
			m.measure("Finished transformation and scaling");
			
			// Test classification
			float[][][] dataForestCl = forest.classify2d(byteData);
			float[][] dataForest = new float[dataForestCl.length][dataForestCl[0].length];
			for (int x=0; x<dataForest.length; x++) {
				for (int y=0; y<dataForest[0].length; y++) {
					dataForest[x][y] = dataForestCl[x][y][1];
				}
			}
			m.measure("Finished running forest");
			
			// Load MIDI
			MIDIAdapter ma = new MIDIAdapter(new File(testReferenceFile));
			long duration = (long)((double)((byteData.length+1) * params.step * 1000.0) / 44100); // TODO festwerte
			byte[][] reference = ma.toDataArray(byteData.length, duration, params.frequencies);
			ArrayUtils.filterFirst(reference);
			ArrayUtils.blur(reference, 0);
			m.measure("Loaded MIDI reference file: " + testReferenceFile);
			
			// Error rates
			int levels = 10;
			long[] right1 = new long[levels];
			long[] right2 = new long[levels];
			long[] wrong1 = new long[levels];
			long[] wrong2 = new long[levels];
			long amtR = 0;
			for(int i=0; i<byteData.length; i++) {
				for(int j=0; j<byteData[0].length; j++) {
					for (int k=1; k<=levels; k++) {
						if (reference[i][j] > 0) {
							if (k==1) amtR++;
							if (dataForest[i][j] > (float)k/levels) {
								right1[k-1]++;
							} else {
								wrong1[k-1]++;
							}
						} else {
							if (dataForest[i][j] > (float)k/levels) {
								wrong2[k-1]++;
							} else {
								right2[k-1]++;
							}
						}
					}
				}
			}
			String sfile = params.workingFolder + File.separator + "run_statistics.txt";
			Logfile l = new Logfile(sfile);
			long amt = byteData.length*byteData[0].length;
			l.write("Amount of tested values: " + amt);
			l.write("Amount of notes (reference): " + amtR);
			for (int k=0; k<levels; k++) {
				double rda = (double)(right1[k]+right2[k]) / amt;
				double wda = (double)(wrong1[k]+wrong2[k]) / amt;
				double rdaN = (double)right1[k]/amtR;
				double rdaS = (double)right2[k]/(amt - amtR);
				l.write("Thr. " + (float)k/levels + ": Classified correct: " + (right1[k] + right2[k]) + ", not detected: " + wrong1[k] + ", falsely detected: " + wrong2[k]);
				l.write("     Percentages general: right/all: " + rda + ", wrong/all: " + wda);
				l.write("                   notes: right/all: " + rdaN);
				l.write("                 silence: right/all: " + rdaS);
			}
			l.close();
			m.measure("Finished evaluation run to file " + sfile + ": ");

			// Save node images
			/*
			for(int i=0; i<forest.getTrees().size(); i++) {
				RandomTree2d t = (RandomTree2d)forest.getTrees().get(i);
				t.saveDebugTree();
			}
			m.measure("Saved node classification images");
			//*/
			
			// Save image
			String forestImgFile = params.workingFolder + File.separator + (new File(testFile)).getName() + ".png";
			ArrayToImage img = new ArrayToImage(dataForest.length, dataForest[0].length);
			out("-> Max data: " +  + img.add(data, Color.WHITE, null));
			out("-> Max forest: " + img.add(dataForest, Color.RED, null, imgThreshold));
			//out("-> Max MIDI: " + img.add(reference, Color.BLUE, null, 0));
			img.save(new File(forestImgFile));
			m.measure("Saved image to " + forestImgFile);
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
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
