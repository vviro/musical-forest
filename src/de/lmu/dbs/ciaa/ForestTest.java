package de.lmu.dbs.ciaa;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.lmu.dbs.ciaa.classifier.*;
import de.lmu.dbs.ciaa.spectrum.ConstantQTransform;
import de.lmu.dbs.ciaa.spectrum.Transform;
import de.lmu.dbs.ciaa.util.*;

/**
 * TODO:
 * 	- Information gain threshold
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
 * 11: 3/6/1.0/20/6
 * 
 * 
 * 
 * - Log vom CQT
 * - node prbabilities: store count (norm)
 * - featuzre generation: obertöne
 * - notenlänge verteilung
 * - Ausreisser bei Featurefunktion: What to do?
 * 
 * - Mehrere verschiedene feaure-typen? 
 * 		- f0-feature
 * 		- noteOn-feature?
 * 
 * 
 * - Multithread? 
 * - lookup table für feature.evaluate?
 * 
 * - profile buffer (google code) für dateiformat, statt serialization
 * - github
 * 
 * 
 * ////////////
 * Ergebnisse
 * - profiling: feature ist nicht mehr hotspot 1, sondern 2 (screenshot profiler)
 * - sparse matrix -> halb so schnell, wenig ersparnis -> raus
 * - Trove: Es gibt keine primitiven listen im programm...also keine Anwendung
 * - Bitwise Classification: Uneffectiv (3x so langsam!); also:  
 * 		-> 2x Boolean array: nicht gut (2.2 mal soviel Laufzeit, heap overflow)
 * 			-> da müss ma nochmal schauen, der Overhead ist nicht so groß, ich glaub es passt so 
 * ///////////
 * 
 * 
 * - testdata: performances vom vladimir
 * 
 * @author Thomas Weber
 *
 */
public class ForestTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		RandomTreeParameters params = new RandomTreeParameters();

		// Forest params
		int forestSize = 1; // Number of trees
		
		// Tree params
		int maxDepth = 4; // 4 Max. tree depth
		params.percentageOfRandomValuesPerFrame = 1.0; // 0.5 Percentage of values per frame to be picked to train the trees
		params.numOfRandomFeatures = 20; // 2 Num of random generated feature sets to train each tree node
		params.thresholdCandidatesPerFeature = 6; // 1 Num of thresholds randomly tried for each random feature set 

		params.xMin = -2;
		params.xMax = 2;
		params.yMin = -150;
		params.yMax = 150;
		params.thresholdMax = 60;
		
		String forestFile = "testdata/forest_oob11"; // file for forest parameters
		boolean load = false;  // Instead of growing it, load the forest from file. If false, the forest will grow and be saved to the file.
		
		// MISC params
		String freqFileName = "testdata/frequencies"; // File holding the spectrum frequencies
		String testFile = "WaveFiles/Test8_Mix.wav";
		
		// OOB Test params
		int step = 256; // Samples per frame
		double fmin = 80.0;
		double fmax = 10000.0;
		double binsPerOctave = 48.0;
		double threshold = 0.05;
		double spread = 1.0;
		double divideFFT = 4.0;
		String cqtBufferLocation = "cqtbuff/";

		
		try {
			// Create profiling tool
			RuntimeMeasure m = new RuntimeMeasure(System.out);

			// Load frequency table (must be common for all samples)
			FileIO<double[]> fio = new FileIO<double[]>();
			params.frequencies = fio.load(freqFileName);
			m.measure("Loaded frequency table from " + freqFileName);

			// Collecting test/training samples
			List<Dataset> initialSet = new ArrayList<Dataset>();
			initialSet.addAll(Dataset.loadDatasets("testdata/mono/cqt", ".cqt", "testdata/mono/midi", ".mid", params.frequencies, step));
			initialSet.addAll(Dataset.loadDatasets("testdata/poly/cqt", ".cqt", "testdata/poly/midi", ".mid", params.frequencies, step));
			
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
			if (!load) {
				// Grow
				forest= new RandomForest(forestSize, params);
				forest.grow(samplers.get(0), maxDepth);
				m.measure("Finished growing random forest");
	
				forest.save(forestFile);
				m.measure("Finished saving forest to file: " + forestFile);

			} else {
				// Load
				forest = RandomForest.load(forestFile, forestSize);
				m.measure("Finished loading forest from file: " + forestFile);
			}
	
			// TMP Test with oob data
			// Load sample
			Sample src = new WaveSample(new File(testFile));
			m.measure("Loaded sample");

			Transform transformation = new ConstantQTransform((double)src.getSampleRate(), fmin, fmax, binsPerOctave, threshold, spread, divideFFT, cqtBufferLocation);
			
			m.measure("Initialized transformation");
			out("--> Window size: " + transformation.getWindowSize());
			
			// Make mono
			int[] mono = src.getLeftBuffer();
			m.measure("Extracted left channel, total samples: " + mono.length);

			// Calculate transformation
			double[][] dataOobD = transformation.calculate(mono, step, new HammingWindow(transformation.getWindowSize()));
			byte[][] dataOob = ArrayUtils.toByteArray(dataOobD);
			m.measure("Finished transformation");
			
			long[][] dataForest = new long[dataOob.length][params.frequencies.length];
			for(int x=0; x<dataOob.length; x++) {
				for(int y=0; y<params.frequencies.length; y++) {
					dataForest[x][y] = forest.classify(dataOob, x, y);
				}
			}
			m.measure("Finished testing forest");
			
			/*
			// TMP: Test with one image
			byte[][] data = sampler.get(1).getSpectrum();
			long[][] dataForest = new long[data.length][params.frequencies.length];
			for(int x=0; x<data.length; x++) {
				for(int y=0; y<params.frequencies.length; y++) {
					dataForest[x][y] = forest.classify(data, x, y);
				}
			}
			m.measure("Finished testing random forest");
*/
			// Save image
			String forestImgFile = forestFile + ".png";
			SpectrumToImage img = new SpectrumToImage(dataForest.length, dataForest[0].length, 1);
			Scale scale = new LogScale(10);
			img.add(dataOobD, new Color(255,150,0), scale);
			img.add(dataForest, Color.GREEN, scale, 0.1);
			img.save(new File(forestImgFile));
			m.measure("Saved image to " + forestImgFile);

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
