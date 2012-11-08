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
 * Generated data: ************************************************************************
 * 
 * Forests:
 * 0: 1/4/0.3/1/1 - Feature bei Ausreisser: Exceptions (Ignore bzw. rec. Right)
 * 1: 1/6/1.0/10/4 - Feature bei Ausreisser: MIN_VALUE
 * 2: 1/6/1.0/10/4 - Feature bei Ausreisser: 0
 * 2b: 1/6/1.0/10/4 - Feature bei Ausreisser: 0
 * 2b: 1/6/1.0/10/4 - Feature bei Ausreisser: 0
 * 3: 1/6/1.0/20/5 - Feature bei Ausreisser: 0
 * 4: 3/6/0.8/10/4 - Feature bei Ausreisser: 0
 * 5: 1/6/1.0/10/4 - No Inverse
 * 
 * Oob:
 * 0: 1/6/1.0/10/4
 * 1: -"- mit FeatureKinect2
 * 2: -"- mit FeatureKinect4
 * 3: -"- mit FeatureKinect4 und data[x][y]/3
 * 4: "-" mit FeatureKinect , ausserdem x=0, y in [0,150]
 * 5: "-" mit FeatureKinect4 und data[x][y]/3, ausserdem x=0, y in [0,150]
 * 6: 1/6/0.5/2/2 mit FeatureKinect5, -2/2/-150/150  --> 22sec growing, 40MB at growing
 * 7: 1/6/1.0/2/2 mit sparse matrix für midi, profiled und etwas optimiert
 * 8: 1/6/1.0/2/2 wieder ohne sparse
 * 9: "-" mit bool classification -> gescheitert
 * 10: 1/6/1.0/20/6 ohne bool, nur so
 * 11: ?
 * 12: 1/4/1.0/20/6 mit Harmonic feature
 * 13: "-" (20 statt 10 harmonics)
 * 14: "-" Ausreisser besser behandelt, normalisierte probabilities
 * 15: "-" FeatureHarmonic2
 * 16: 1/6/1.0/20/6 FeatureHarmonic2
 * 17: 1/4/1.0/20/6 FeatureHarmonic2
 * 18: "-" max
 * 19: "-" (add im feature) max
 * 21: "-" mit neuen Testdaten (Log) nur mono
 * 22: "-" mit neuen Testdaten (Log) auch poly, simple feature
 * 23: "-" additives harmonic feature
 * 24: 5/4/1.0/5/5 FeatureHarmonic2
 * 25: 1/4/1.0/10/5 FeatureHarmonic2 -2/2
 * 
 * In Folders: (forestX)
 * 
 * 1: 1/4/1.0/10/5 FeatureHarmonic2 -2/2
 * 2: 2/4/0.2/5/5 Thread test                 !!!!!!!!!!! a-Version! -- d Version is for testing threds
 * 
 * Remote:
 * testdeploy: 5/6/0.8/20/5/Harmonic3 0/0
 * 
 * TODO *************************************************************************************
 * 
 * Random generator is sick?
 * 
 * Optimierung:
 * 		- Make the application distributable
 * 		- lookup table für feature.evaluate? (später)
 * 
 * Testdaten:
 * 		- Performances von Vladimir
 * 
 * Forest:
 *		- Visualize Forest Nodes (Features)! Siehe Test 25, alles dieselben Parameter! Was ist am besten? 
 * 		Feature:
 * 			- u und v auf gleicher frequenz (probieren)
 * 			- harmonics über eine verteilung verteilen? -> weil je weiter oben desto besser das ergebnis
 * 			- peaks als zweite infoquelle für u/v-Punkte?
 * 			- notenlänge verteilung
 * 		Allgemein:
 * 			- Entropy: Wird wirklich der maximale Infogehalt ermittelt?
 * 				-> Mit aktuelleren Features nochmal testen
 * 			- Information gain threshold?
 * 			- Mehrere verschiedene feaure-typen?
 * 				- f0-feature
 * 				- noteOn-feature (basiert auf fuzzy attacks)
 * 				- ...?
 * 			- oder alternieren zw. daten und peaks (bei Featuregenerierung)?
 * 			- oder mehrere Wälder per problem?
 * 
 * Application Design:
 * 		- protocol buffer (google code) für dateiformat, statt serialization
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
			}
			Log.open(params.workingFolder + File.separator + logFile);
			m.measure("Created target folder and opened log");
			
			// Load frequency table (must be common for all samples)
			FileIO<double[]> fio = new FileIO<double[]>();
			String freqFileName = params.frequencyTableFile;
			params.frequencies = fio.load(freqFileName);
			m.measure("Loaded frequency table from " + freqFileName);

			// Collecting test/training samples
			List<Dataset> initialSet = new ArrayList<Dataset>();
			initialSet.addAll(Dataset.loadDatasets("testdata/mono/cqt", ".cqt", "testdata/mono/midi", ".mid", params.frequencies, params.step));
			initialSet.addAll(Dataset.loadDatasets("testdata/poly/cqt", ".cqt", "testdata/poly/midi", ".mid", params.frequencies, params.step));
			
			// Create initial bootstrapping sampler
			BootstrapSampler<Dataset> sampler = new BootstrapSampler<Dataset>(initialSet);
			m.measure("Finished generating " + initialSet.size() + " initial data samples");

			// Split data into training and testing parts (1:1)
			List<Sampler<Dataset>> samplers = sampler.split(2);
			out("Initial sampler size: " + sampler.getPoolSize());
			out("Training sampler size: " + samplers.get(0).getPoolSize());
			out("Testing sampler size: " + samplers.get(1).getPoolSize());
			m.measure("Finished splitting training and test data");
			
			// Grow forest with training part of data
			RandomForest forest;
			if (!params.loadForest) {
				// Grow
				forest= new RandomForest(params.forestSize, params);
				forest.grow(samplers.get(0), params.maxDepth);
				
				m.measure("Finished growing random forest");
	
				forest.save(params.workingFolder + File.separator + params.nodedataFilePrefix);
				m.measure("Finished saving forest to file: " + params.workingFolder);

			} else {
				// Load
				forest = RandomForest.load(params.workingFolder + File.separator + params.nodedataFilePrefix, params.forestSize);
				m.measure("Finished loading forest from folder: " + params.workingFolder);
			}
	
			// TMP Test with oob data
			// Load sample
			Sample src = new WaveSample(new File(testFile));
			m.measure("Loaded sample");

			Transform transformation = new ConstantQTransform((double)src.getSampleRate(), params.fMin, params.fMax, params.binsPerOctave, params.threshold, params.spread, params.divideFFT, params.cqtBufferLocation);
			
			m.measure("Initialized transformation");
			out("--> Window size: " + transformation.getWindowSize());
			
			// Make mono
			int[] mono = src.getLeftBuffer();
			m.measure("Extracted left channel, total samples: " + mono.length);

			// Calculate transformation
			double[][] dataOobD = transformation.calculate(mono, params.step, new HammingWindow(transformation.getWindowSize()));
			Scale scale = new LogScale(10);
			ArrayUtils.normalize(dataOobD); // Normalize to [0,1]
			ArrayUtils.scale(dataOobD, scale); // Log scale
			ArrayUtils.normalize(dataOobD, (double)Byte.MAX_VALUE); // Normalize back to [0,MAX_VALUE] 
			byte[][] dataOob = ArrayUtils.toByteArray(dataOobD); // To byte array to use with forest
			m.measure("Finished transformation and scaling");
			
			// Test classification
			float[][] dataForest = forest.classify(dataOob); 
			m.measure("Finished testing forest");
			
			// Save image
			String forestImgFile = params.workingFolder + File.separator + (new File(testFile)).getName() + ".png";
			SpectrumToImage img = new SpectrumToImage(dataForest.length, dataForest[0].length);
			img.add(dataOobD, new Color(255,150,0), null);
			img.add(dataForest, Color.GREEN, null, imgThreshold);
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
			out("Maximum feature point overlay: " + imgF.add(featuresVisualization, Color.WHITE, null, 0));
			imgF.save(new File(featuresFile));
			m.measure("Saved feature visualization to " + featuresFile);

			// Debug: copy generated working folder to a location where it can be easier accessed by scripts
			FileUtils.deleteDirectory(new File(copyToDir));
			FileUtils.copyDirectory(new File(params.workingFolder), new File(copyToDir));
			m.measure("Copied results to " + copyToDir);
			
			Log.close();
			m.measure("Saved log file");
			
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
