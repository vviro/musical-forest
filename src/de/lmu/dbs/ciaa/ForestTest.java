package de.lmu.dbs.ciaa;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;

import de.lmu.dbs.ciaa.classifier.core.*;
import de.lmu.dbs.ciaa.classifier.core2d.*;
import de.lmu.dbs.ciaa.classifier.musicalforest.*;
import de.lmu.dbs.ciaa.midi.MIDIAdapter;
import de.lmu.dbs.ciaa.spectrum.ConstantQTransform;
import de.lmu.dbs.ciaa.spectrum.Transform;
import de.lmu.dbs.ciaa.util.*;

/**
 * TODO *************************************************************************************
 *
 * 		- mp3 testen (cqt)
 * 		- Ableitung
 * 		- Github -> eigenes Repo
 *
 * - Saubere Tests nach Breiman
 *
 * - Testdaten: Andere Instrumente? Noisy data?
 *
 * - Features: Klapuri?
 * 
 * - Kapitel 3.4 in Kinect paper!
 * 
 * Testdaten:
 * 		- Performances von Vladimir
 * 
 * Code:
 * 		- protocol buffer (google code) für dateiformat, statt serialization
 * 			-> erst wenn parameter fix sind
 * 
 * @author Thomas Weber
 *
 */
public class ForestTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// XML file for program configuration
		String settingsFile = "settings.xml";
		
		// Debug params (all others are loaded from settings.xml)
		String copyToDir = "testdataResults/lastrun"; // used to pick up the results of a test run by scripts. The contents of the working folder are being copied there. 
		//String featureImgFile = "featuresOverview.png"; // This file is saved along the tree node data files. It contains a visualization of the created tree´s nodes features.
		//String treeImgFile = "featuresOverviewY.png"; // This file is saved along the tree node data files. It contains a visualization of the created tree´s nodes features.
		String testFile = "testdata2/random_grouped0.cqt"; //WaveFiles/Test8_Mix.wav"; // WAV file used to test the forest. Results are stored in a PNG file called <testFile>.png
		String testReferenceFile = "testdata2/random_grouped0.mid"; //WaveFiles/MIDIFiles/Test8melody.mid"; // Test MIDI reference file. Has to be musically equal to testFile 
		double imgThreshold = 0.1; // Threshold to filter the normalized forest results in the PNG test output
		
		try {
			System.out.println("Java Heap size (maximum): " + ((double)Runtime.getRuntime().maxMemory() / (1024*1024)) + " MB");
			FileUtils.deleteDirectory(new File(copyToDir));
			
			// Create profiling tool
			RuntimeMeasure m = new RuntimeMeasure(System.out);

			// Load settings
			Settings settings = new Settings(settingsFile);
			ForestParameters params = settings.getForestParameters();
			if (args.length > 0) {
				params.workingFolder = args[0];
				System.out.println("Overriding working folder: " + params.workingFolder);
			}
			m.measure("Loaded settings from XML file");
			
			// Create result folder
			File resultDir = new File(params.workingFolder);
			if (!params.loadForest) {
				File wf = new File(params.workingFolder);
				if (wf.exists()) FileUtils.deleteDirectory(wf);
				resultDir.mkdirs();
				FileUtils.copyFile(new File(settingsFile), new File(params.workingFolder + File.separator + settingsFile)); // TMP
				m.measure("Created working folder");
			}
			
			// Load frequency table (must be common for all samples)
			FileIO<double[]> fio = new FileIO<double[]>();
			String freqFileName = params.frequencyTableFile;
			params.frequencies = fio.load(freqFileName);
			m.measure("Loaded frequency table from " + freqFileName);

			// Collecting test/training samples
			List<Dataset> initialSet = new ArrayList<Dataset>();
			//initialSet.addAll(Dataset.loadDatasets("testdata/mono/cqt", ".cqt", "testdata/mono/midi", ".mid", params.frequencies, params.step));
			//initialSet.addAll(Dataset.loadDatasets("testdata/poly/cqt", ".cqt", "testdata/poly/midi", ".mid", params.frequencies, params.step));
			initialSet.addAll(OnsetMusicalTreeDataset.loadDatasets("testdata2", ".cqt", "testdata2", ".mid", params.frequencies, params.step));
			
			// Create initial bootstrapping sampler
			BootstrapSampler<Dataset> sampler = new BootstrapSampler<Dataset>(initialSet);
			m.measure("Finished generating " + initialSet.size() + " initial data samples");

			/*long c = 0;
			for(int i=0; i<sampler.getPoolSize(); i++) {
				c += ArrayUtils.countValues(((TreeDataset)sampler.get(i)).getData(), 0);
			}
			System.out.println("Amount of zeroes in spectral data: " + c);
			*/
			
			// Split data into training and testing parts (1:1)
			/*List<Sampler<Dataset>> samplers = sampler.split(2);
			out("Initial sampler size: " + sampler.getPoolSize());
			out("Training sampler size: " + samplers.get(0).getPoolSize());
			out("Testing sampler size: " + samplers.get(1).getPoolSize());
			m.measure("Finished splitting training and test data");
			*/
			
			// Grow forest with training part of data
			Forest forest;
			if (!params.loadForest) {
				// Grow
				Logfile[] treelogs = new Logfile[params.forestSize]; 
				List<RandomTree> trees = new ArrayList<RandomTree>();
				for(int i=0; i<params.forestSize; i++) {
					treelogs[i] = new Logfile(params.workingFolder + File.separator + "T" + i + "_Growlog.txt");
					trees.add(new MusicalRandomTree(params, i, treelogs[i]));
				}
				Logfile forestlog = new Logfile(params.workingFolder + File.separator + "ForestStats.txt");
				forest = new Forest(trees, params, forestlog);
				//forest.grow(samplers.get(0));
				forest.grow(sampler);
				m.measure("Finished growing random forest");

				forest.save(params.workingFolder + File.separator + params.nodedataFilePrefix);
				m.measure("Finished saving forest to file: " + params.workingFolder);

				forest.logStats();
				m.measure("Finished logging forest stats");

				// Close logs
				for(int i=0; i<treelogs.length; i++) {
					treelogs[i].close();
				}
				forestlog.close();
				m.measure("Finished closing logs");
	
			} else {
				// Load
				RandomTree2d treeFactory = new MusicalRandomTree(); 
				forest = Forest.load(params, params.workingFolder + File.separator + params.nodedataFilePrefix, 2, treeFactory);
				m.measure("Finished loading forest from folder: " + params.workingFolder);
			}
			//System.exit(0);
			
			// TMP Test with oob data
			// Load sample
			/*
			Sample src = new WaveSample(new File(testFile));
			m.measure("Loaded sample");

			Transform transformation = new ConstantQTransform((double)src.getSampleRate(), params.fMin, params.fMax, params.binsPerOctave, params.threshold, params.spread, params.divideFFT, params.cqtBufferLocation);
			
			m.measure("Initialized transformation");
			out("-> Window size: " + transformation.getWindowSize());
			
			// Make mono
			int[] mono = src.getLeftBuffer();
			m.measure("Extracted left channel, total samples: " + mono.length);

			// Calculate transformation
			double[][] dataOobDouble = transformation.calculate(mono, params.step, new HammingWindow(transformation.getWindowSize()));
			//*/
			FileIO<byte[][]> bio = new FileIO<byte[][]>();
			double[][] dataOobDouble = ArrayUtils.toDoubleArray(bio.load(testFile));
			double[][] dataPeak = dataOobDouble; /*new double[dataOobDouble.length][];

			DifferentialAnalyzer p = new DifferentialAnalyzer();
			for(int i=0; i<dataOobDouble.length; i++) {
				dataPeak[i] = p.getPeaks(dataOobDouble[i]);
			}
			Scale scale = new LogScale(10);
			ArrayUtils.normalize(dataPeak); // Normalize to [0,1]
			ArrayUtils.scale(dataPeak, scale); // Log scale
			ArrayUtils.normalize(dataPeak, (double)Byte.MAX_VALUE-1); // Normalize back to [0,MAX_VALUE] 
			//*/
			byte[][] dataOob = ArrayUtils.toByteArray(dataPeak); // To byte array to use with forest
			m.measure("Finished transformation and scaling");
			
			// Test classification
			float[][][] dataForestCl = forest.classify2d(dataOob);
			float[][] dataForest = new float[dataForestCl.length][dataForestCl[0].length];
			for (int x=0; x<dataForest.length; x++) {
				for (int y=0; y<dataForest[0].length; y++) {
					dataForest[x][y] = dataForestCl[x][y][1];
				}
			}
			m.measure("Finished running forest");
			
			// Load MIDI
			MIDIAdapter ma = new MIDIAdapter(new File(testReferenceFile));
			long duration = (long)((double)((dataOob.length+1) * params.step * 1000.0) / 44100); // TODO festwerte
			byte[][] reference = ma.toDataArray(dataOob.length, duration, params.frequencies);
			ArrayUtils.filterFirst(reference);
			ArrayUtils.blur(reference, 0);
			ArrayUtils.shiftRight(reference, 3);
			m.measure("Loaded MIDI reference file: " + testReferenceFile);
			
			// Error rates
			int levels = 10;
			long[] right1 = new long[levels];
			long[] right2 = new long[levels];
			long[] wrong1 = new long[levels];
			long[] wrong2 = new long[levels];
			long amtR = 0;
			for(int i=0; i<dataOob.length; i++) {
				for(int j=0; j<dataOob[0].length; j++) {
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
			long amt = dataOob.length*dataOob[0].length;
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
			for(int i=0; i<forest.getTrees().size(); i++) {
				RandomTree2d t = (RandomTree2d)forest.getTrees().get(i);
				t.saveDebugTree();
			}
			m.measure("Saved node classification images");
			
			// Save image
			String forestImgFile = params.workingFolder + File.separator + (new File(testFile)).getName() + ".png";
			ArrayToImage img = new ArrayToImage(1500, dataForest[0].length); //dataForest.length, dataForest[0].length);
			out("-> Max data: " +  + img.add(dataOobDouble, Color.WHITE, null));
			out("-> Max forest: " + img.add(dataForest, Color.RED, null, imgThreshold));
			out("-> Max MIDI: " + img.add(reference, Color.BLUE, null, 0));
			img.save(new File(forestImgFile));
			m.measure("Saved image to " + forestImgFile);
			
			// Visualize features
			/*
			int[][] featuresVisualization = forest.visualize(params);
			int[][] featuresVisualizationGrid = new int[featuresVisualization.length][featuresVisualization[0].length];
			int x0 = -params.xMin;
			for(int i=0; i<featuresVisualizationGrid[0].length; i++) {
				featuresVisualizationGrid[x0*2][i] = 1;
			}
			String featuresFile = params.workingFolder + File.separator + featureImgFile;
			ArrayToImage imgF = new ArrayToImage(featuresVisualization.length, featuresVisualization[0].length, 2, 20);
			imgF.add(featuresVisualizationGrid, Color.BLUE, null, 0);
			out("-> Maximum feature point overlay: " + imgF.add(featuresVisualization, Color.WHITE, null, 0));
			imgF.save(new File(featuresFile));
			m.measure("Saved feature visualization to " + featuresFile);
*/
			// Debug: copy generated working folder to a location where it can be easier accessed by scripts
			FileUtils.copyDirectory(new File(params.workingFolder), new File(copyToDir));
			m.measure("Copied results to " + copyToDir);
			
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
