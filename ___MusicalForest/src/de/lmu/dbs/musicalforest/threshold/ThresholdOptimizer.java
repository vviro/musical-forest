package de.lmu.dbs.musicalforest.threshold;

import java.text.SimpleDateFormat;
import java.util.List;

import de.lmu.dbs.jforest.core.Dataset;
import de.lmu.dbs.jforest.core.Forest;
import de.lmu.dbs.jforest.core2d.Forest2d;
import de.lmu.dbs.jforest.core2d.TreeDataset2d;
import de.lmu.dbs.jforest.util.MeanShift;
import de.lmu.dbs.jforest.util.workergroup.ThreadScheduler;
import de.lmu.dbs.jforest.util.workergroup.Worker;
import de.lmu.dbs.jspectrum.util.ArrayUtils;
import de.lmu.dbs.musicalforest.Action;
import de.lmu.dbs.musicalforest.classifier.AccuracyTest;
import de.lmu.dbs.musicalforest.classifier.ForestMeta;
import de.lmu.dbs.musicalforest.midi.MIDIAdapter;

/**
 * Class for detection of optimal thresholds for musical forest.
 * 
 * @author Thomas Weber
 *
 */
public class ThresholdOptimizer {

	/**
	 * Date formatter for debug output.
	 */
	protected SimpleDateFormat timeStampFormatter = new SimpleDateFormat("hh:mm:ss");

	/**
	 * 
	 */
	public ThreadScheduler threadScheduler;
	
	/**
	 * Number of slices to divide [0..1]
	 */
	private int granularity;
	
	/**
	 * X radius of accuracy tests
	 */
	private int testRadiusX;

	/**
	 * Y radius of accuracy tests
	 */
	private int testRadiusY;
	
	/**
	 * MeanShift window size in pixels
	 */
	private int meanShiftWindow;
	
	/**
	 * Meta data from the forest
	 */
	private ForestMeta meta;

	/**
	 * 
	 * @param granularity
	 * @param testRadiusX
	 * @param testRadiusY
	 * @param meanShiftWindow
	 */
	public ThresholdOptimizer(int granularity, int testRadiusX, int testRadiusY, int meanShiftWindow, ForestMeta meta) {
		this.granularity = granularity;
		this.testRadiusX = testRadiusX;
		this.testRadiusY = testRadiusY;
		this.meanShiftWindow = meanShiftWindow;
		this.meta = meta;
	}
	
	/**
	 * 
	 * @param forest
	 * @param datasets
	 * @throws Exception
	 */
	public ForestMeta optimize(Forest2d forest, List<Dataset> datasets, int numOfClassifyingThreads) throws Exception {
		// Init arrays
		AccuracyTest[][] testsOnset = new AccuracyTest[granularity][granularity];
		for(int i=0; i<testsOnset.length; i++) {
			for(int j=0; j<testsOnset[0].length; j++) {
				testsOnset[i][j] = new AccuracyTest(testRadiusX, testRadiusY);
			}
		}
		AccuracyTest[][] testsOffset = new AccuracyTest[granularity][granularity];
		for(int i=0; i<testsOffset.length; i++) {
			for(int j=0; j<testsOffset[0].length; j++) {
				testsOffset[i][j] = new AccuracyTest(testRadiusX, testRadiusY);
			}
		}
		
		// Classify all datasets
		float[][][][] classifications = new float[datasets.size()][][][];
		for(int i=0; i<datasets.size(); i++) {
			TreeDataset2d dataset = (TreeDataset2d)datasets.get(i);
			byte[][] data = (byte[][])dataset.getData();
			System.out.println("Classifying dataset " + (i+1) + "/ " + datasets.size() + ":");
			classifications[i] = forest.classify2d(data, numOfClassifyingThreads, true);
		}
		
		// Multithreaded thresholds search
		System.out.println("Search best thresholds...");
		threadScheduler = new ThreadScheduler(datasets.size());
		synchronized(threadScheduler) {
			ThresholdWorkerGroup group = new ThresholdWorkerGroup(threadScheduler, Forest.THREAD_POLLING_INTERVAL, true);
			for(int i=0; i<datasets.size(); i++) {
				TreeDataset2d dataset = (TreeDataset2d)datasets.get(i);
				ThresholdWorker worker = new ThresholdWorker(group, this, forest, dataset, classifications[i], testsOnset, testsOffset, i);
				group.add(worker);
			}
			group.runGroup();
		}

		// Find best note on threshold
		double max = -Double.MAX_VALUE;
		int bestOnsetIndex = -1;
		int bestOffsetIndex = -1;
		for(int i=0; i<granularity; i++) {
			for(int j=0; j<granularity; j++) {
				double fdOn = testsOnset[i][j].getFalseDetection();
				if (fdOn == Double.NaN) fdOn = 0;
				double tOn = testsOnset[i][j].getCorrectDetection() - fdOn;

				double fdOff = testsOffset[i][j].getFalseDetection();
				if (fdOff == Double.NaN) fdOff = 0;
				double tOff = testsOffset[i][j].getCorrectDetection() - fdOff;

				double t = tOn + tOff;
				
				if (t > max) {
					max = t;
					bestOnsetIndex = i;
					bestOffsetIndex = j;
				}
			}
		}
		
		// Return meta object
		double bestOnsetThreshold = (double)bestOnsetIndex / granularity;
		double bestOffsetThreshold = (double)bestOffsetIndex / granularity;
		AccuracyTest bestOnsetTest = testsOnset[bestOnsetIndex][bestOffsetIndex];
		AccuracyTest bestOffsetTest = testsOffset[bestOnsetIndex][bestOffsetIndex];
		return new ForestMeta(bestOnsetThreshold, bestOnsetTest, bestOffsetThreshold, bestOffsetTest, null);

/*		// Average results from different datasets
		double acc = 0;
		for (int i=0; i<datasets.size(); i++) {
			acc+= bestOnsetThresholds[i];
		}
		double bestOnsetThreshold = acc / datasets.size();
		int biOnset = (int)(bestOnsetThreshold * granularity);

		acc = 0;
		for (int i=0; i<datasets.size(); i++) {
			acc+= bestOffsetThresholds[i];
		}
		double bestOffsetThreshold = acc / datasets.size();
		int biOffset = (int)(bestOffsetThreshold * granularity);
		
		// Return meta object
		int biOnset = (int)(bestOnsetThreshold * granularity);
		int biOffset = (int)(bestOffsetThreshold * granularity);
		AccuracyTest bestOnsetTest = testsOnset[biOnset][biOffset];
		AccuracyTest bestOffsetTest = testsOffset[biOnset][biOffset];
		return new ForestMeta(bestOnsetThreshold, bestOnsetTest, bestOffsetThreshold, bestOffsetTest, null);
 */	
	}
	
	/**
	 * Called by the worker threads to process one dataset.
	 * 
	 * @param forest
	 * @param dataset
	 * @throws Exception
	 */
	public void processThreaded(Worker worker, Forest2d forest, TreeDataset2d dataset, float[][][] classification, AccuracyTest[][] testsOnset, AccuracyTest[][] testsOffset, int num) throws Exception {
		byte[][] reference = (byte[][])dataset.getReference();
		
		// Split ons and offs from classification output
		float[][] dataForestOnset = new float[classification.length][classification[0].length];
		float[][] dataForestOffset = new float[classification.length][classification[0].length];
		for (int x=0; x<dataForestOnset.length; x++) {
			for (int y=0; y<dataForestOnset[0].length; y++) {
				dataForestOnset[x][y] = classification[x][y][1];
				dataForestOffset[x][y] = classification[x][y][2];
			}
		}

		// Separate ons and offs in classification output
		byte[][] refOn = ArrayUtils.clone(reference);
		for (int x=0; x<reference.length; x++) {
			for (int y=0; y<reference[0].length; y++) {
				if (refOn[x][y] != 1) refOn[x][y] = 0; 
			}
		}
		byte[][] refOff = reference; 
		for (int x=0; x<reference.length; x++) {
			for (int y=0; y<reference[0].length; y++) {
				if (refOff[x][y] != 2) refOff[x][y] = 0; 
			}
		}

		for(int i=0; i<granularity; i++) {
			// Mean Shift
			double fThreshold = (double)i/granularity;
			MeanShift ms = new MeanShift(meanShiftWindow);
			ms.process(dataForestOnset, (float)fThreshold); 

			for(int j=0; j<granularity; j++) {
			    
			    // For each onset, find best offset threshold
				double fThresholdOff = (double)j/granularity;
			    MeanShift msOff = new MeanShift(meanShiftWindow);
			    msOff.process(dataForestOffset, (float)fThresholdOff); 

			    // Generate MIDI from Mean Shift results
			    MIDIAdapter newMidi = new MIDIAdapter(Action.DEFAULT_MIDI_TEMPO);
			    double millisPerStep = (1000.0 * meta.dataMeta.transformParams.step) / meta.dataMeta.sampleRate;
			    int frequencyWindow = meta.dataMeta.transformParams.getBinsPerHalfTone();
			    newMidi.renderFromArrays(ms.modeWeights, msOff.modeWeights, millisPerStep, meta.dataMeta.transformParams.frequencies, frequencyWindow);

			    // Re-render MIDI to array
			    long duration = MIDIAdapter.calculateDuration(dataForestOnset.length, meta.dataMeta.transformParams.step, meta.dataMeta.sampleRate);
			    byte[][] midi = newMidi.toDataArray(dataForestOnset.length, duration, meta.dataMeta.transformParams.frequencies);
			    
			    byte[][] refMidiOn = ArrayUtils.clone(midi);
				ArrayUtils.filterFirst(refMidiOn);
		
			    byte[][] refMidiOff = ArrayUtils.clone(midi);
				ArrayUtils.filterLast(refMidiOff);

			    // Test MIDI results
			    testsOnset[i][j].addData(refMidiOn, refOn);
			    testsOffset[i][j].addData(refMidiOff, refOff);
			}
			worker.setProgress(fThreshold);
		}
		
		
		//thresholdsOnset[num] = (double)bestOnsetIndex / granularity;
		//thresholdsOffset[num] = (double)bestOffsetIndex / granularity;
		
		worker.setProgress(1.0);
	}

	/**
	 * 
	 * @param tests
	 * @param thresholds
	 * @param dataForest
	 * @param reference
	 * @param num
	 * @throws Exception
	 *
	private void calculate(AccuracyTest[] tests, double[] thresholds, float[][] dataForest, byte[][] reference, int num) throws Exception {
		for(int i=0; i<granularity; i++) {

			// Mean Shift
			double fThreshold = (double)i/granularity;
		    MeanShift ms = new MeanShift(meanShiftWindow);
		    ms.process(dataForest, (float)fThreshold); 

			// Generate MIDI from Mean Shift
		    MIDIAdapter newMidi = new MIDIAdapter(Action.DEFAULT_MIDI_TEMPO);
		    double millisPerStep = (1000.0 * meta.dataMeta.transformParams.step) / meta.dataMeta.sampleRate;
		    int frequencyWindow = meta.dataMeta.transformParams.getBinsPerHalfTone();
		    newMidi.renderFromArrays(ms.modeWeights, msOff.modeWeights, millisPerStep, meta.dataMeta.transformParams.frequencies, frequencyWindow);
		    tests[i].addData(ms.modeWeights, reference);
		}
		
		// Find best note on threshold
		double max = -Double.MAX_VALUE;
		int maxIndex = -1;
		for(int i=0; i<granularity; i++) {
			double fd = tests[i].getFalseDetection();
			if (fd == Double.NaN) fd = 0;
			double t = tests[i].getCorrectDetection() - fd;
			if (t > max) {
				max = t;
				maxIndex = i;
			}
		}
		
		// Determine best index
		int bestIndex = maxIndex; 
		thresholds[num] = (double)bestIndex / granularity;
	}
	//*/
}
