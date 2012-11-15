package de.lmu.dbs.ciaa;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;

import de.lmu.dbs.ciaa.classifier.*;
import de.lmu.dbs.ciaa.spectrum.ConstantQTransform;
import de.lmu.dbs.ciaa.spectrum.Transform;
import de.lmu.dbs.ciaa.util.*;

/**
 * TODO *************************************************************************************
 * 
 * Test on server features:
 * 	3b
 * 	5
 * 
 * Code:
 * 		- make forest generic
 * 			- extend RandomTree
 * 			- Sampler type -> ?
 * 		- logdatei per tree
 * 		- gain distribution diagrams: ins feature versetzen, weil abh. von verteilung
 * 		- protocol buffer (google code) für dateiformat, statt serialization
 *
 * 		-> Forests: 
 * 			- reimplement bootstrapping framewise
 * 			- automate test to get values instead of images only (s. Breiman)		
 * 
 * - Mehr rekursion wird schlechter ab dep 4 oder so
 * 		- es klassifizieren sich beide seiten zur selben klasse...verhndern?
 * 		- vielleicht ist das bei vielen klassen besser...also erweiterung des kinect?
 * 
 * Optimierung:
 * 		- Make the application distributable
 * 
 * Testdaten:
 * 		- Performances von Vladimir
 * 
 * Forest:
 * 		Feature:
 * 			- notenlänge verteilung
 * 		Allgemein:
 * 			- Mehrere verschiedene feaure-typen?
 * 				- f0-feature
 * 				- noteOn-feature (basiert auf fuzzy attacks)
 * 				- ...?
 * 			- oder alternieren zw. daten und peaks (bei Featuregenerierung)?
 * 			- oder mehrere Wälder per problem?
 * 
 * 
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
		String logFile = "growlog.txt"; // File for logging tree grow events (not application logging)
		
		// Debug params (all others are loaded from settings.xml)
		String copyToDir = "testdataResults/lastrun"; // used to pick up the results of a test run by scripts. The contents of the working folder are being copied there. 
		String featureImgFile = "featuresOverview.png"; // This file is saved along the tree node data files. It contains a visualization of the created tree´s nodes features.
		//String treeImgFile = "featuresOverviewY.png"; // This file is saved along the tree node data files. It contains a visualization of the created tree´s nodes features.
		String testFile = "WaveFiles/Test8_Mix.wav"; // WAV file used to test the forest. Results are stored in a PNG file called <testFile>.png
		double imgThreshold = 0.5; // Threshold to filter the normalized forest results in the PNG test output
		
		try {
			System.out.println("Java Heap size (maximum): " + ((double)Runtime.getRuntime().maxMemory() / (1024*1024)) + " MB");
			
			// Create profiling tool
			RuntimeMeasure m = new RuntimeMeasure(System.out);

			// Load settings
			Settings settings = new Settings(settingsFile);
			ForestParameters params = settings.getForestParameters();
			m.measure("Loaded settings from XML file");
			
			// Create result folder
			File resultDir = new File(params.workingFolder);
			if (!params.loadForest) {
				FileUtils.deleteDirectory(new File(params.workingFolder));
				resultDir.mkdirs();
				Log.open(params.workingFolder + File.separator + logFile);
				m.measure("Created target folder and opened log");
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
			initialSet.addAll(Dataset.loadDatasets("testdata2", ".peak", "testdata2", ".mid", params.frequencies, params.step));
			
			// Create initial bootstrapping sampler
			BootstrapSampler<Dataset> sampler = new BootstrapSampler<Dataset>(initialSet);
			m.measure("Finished generating " + initialSet.size() + " initial data samples");

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
				List<Tree> trees = new ArrayList<Tree>();
				for(int i=0; i<params.forestSize; i++) {
					trees.add(new RandomTree(params, i));
				}
				forest = new Forest(trees, params);
				//forest.grow(samplers.get(0));
				forest.grow(sampler);
				m.measure("Finished growing random forest");

				forest.save(params.workingFolder + File.separator + params.nodedataFilePrefix);
				m.measure("Finished saving forest to file: " + params.workingFolder);

				forest.logTreeStats();
				m.measure("Finished logging forest stats");
	
			} else {
				// Load
				forest = Forest.load(params, params.workingFolder + File.separator + params.nodedataFilePrefix, params.forestSize);
				m.measure("Finished loading forest from folder: " + params.workingFolder);
			}
			//System.exit(0);
			
			// TMP Test with oob data
			// Load sample
			Sample src = new WaveSample(new File(testFile));
			m.measure("Loaded sample");

			Transform transformation = new ConstantQTransform((double)src.getSampleRate(), params.fMin, params.fMax, params.binsPerOctave, params.threshold, params.spread, params.divideFFT, params.cqtBufferLocation);
			
			m.measure("Initialized transformation");
			out("-> Window size: " + transformation.getWindowSize());
			
			// Make mono
			int[] mono = src.getLeftBuffer();
			m.measure("Extracted left channel, total samples: " + mono.length);

			// Calculate transformation
			double[][] dataOobD = transformation.calculate(mono, params.step, new HammingWindow(transformation.getWindowSize()));
			Scale scale = new LogScale(10);
			ArrayUtils.normalize(dataOobD); // Normalize to [0,1]
			ArrayUtils.scale(dataOobD, scale); // Log scale
			ArrayUtils.normalize(dataOobD, (double)Byte.MAX_VALUE-1); // Normalize back to [0,MAX_VALUE] 
			byte[][] dataOob = ArrayUtils.toByteArray(dataOobD); // To byte array to use with forest
			m.measure("Finished transformation and scaling");
			
			// Test classification
			float[][] dataForest = forest.classify(dataOob); 
			m.measure("Finished testing forest");
			
			// Save node images
			for(int i=0; i<forest.getTrees().size(); i++) {
				RandomTree t = (RandomTree)forest.getTrees().get(i);
				t.saveDebugTree();
			}
			m.measure("Saved node visualization images");
			
			// Save image
			String forestImgFile = params.workingFolder + File.separator + (new File(testFile)).getName() + ".png";
			SpectrumToImage img = new SpectrumToImage(dataForest.length, dataForest[0].length);
			out("-> Max data: " +  + img.add(dataOobD, Color.WHITE, null));
			out("-> Max forest: " + img.add(dataForest, Color.RED, null, imgThreshold));
			img.save(new File(forestImgFile));
			m.measure("Saved image to " + forestImgFile);
			
			// Visualize features
			int[][] featuresVisualization = forest.visualize(params);
			int[][] featuresVisualizationGrid = new int[featuresVisualization.length][featuresVisualization[0].length];
			int x0 = -params.xMin;
			for(int i=0; i<featuresVisualizationGrid[0].length; i++) {
				featuresVisualizationGrid[x0*2][i] = 1;
			}
			String featuresFile = params.workingFolder + File.separator + featureImgFile;
			SpectrumToImage imgF = new SpectrumToImage(featuresVisualization.length, featuresVisualization[0].length, 2, 20);
			imgF.add(featuresVisualizationGrid, Color.BLUE, null, 0);
			out("-> Maximum feature point overlay: " + imgF.add(featuresVisualization, Color.WHITE, null, 0));
			imgF.save(new File(featuresFile));
			m.measure("Saved feature visualization to " + featuresFile);

			Log.close();
			m.measure("Saved log file");
			
			// Debug: copy generated working folder to a location where it can be easier accessed by scripts
			FileUtils.deleteDirectory(new File(copyToDir));
			//FileUtils.copyFile(new File(settingsFile), new File(params.workingFolder + File.separator + settingsFile));
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
