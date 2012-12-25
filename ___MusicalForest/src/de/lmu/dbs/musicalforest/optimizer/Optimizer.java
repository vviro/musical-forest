package de.lmu.dbs.musicalforest.optimizer;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.text.SimpleDateFormat;
import java.util.List;

import de.lmu.dbs.jforest.core.Dataset;
import de.lmu.dbs.jforest.core.Forest;
import de.lmu.dbs.jforest.core.TreeDataset;
import de.lmu.dbs.jforest.core2d.Forest2d;
import de.lmu.dbs.jforest.core2d.TreeDataset2d;
import de.lmu.dbs.jforest.util.MeanShift;
import de.lmu.dbs.jforest.util.workergroup.ThreadScheduler;
import de.lmu.dbs.jforest.util.workergroup.Worker;
import de.lmu.dbs.jforest.util.ArrayUtils;
import de.lmu.dbs.musicalforest.classifier.AccuracyTest;
import de.lmu.dbs.musicalforest.classifier.ForestMeta;

/**
 * Class for detection of optimal thresholds for musical forest.
 * 
 * @author Thomas Weber
 *
 */
public class Optimizer {

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
	
	private int maxDepth;
	
	/**
	 * Meta data from the forest
	 *
	private ForestMeta meta;

	/**
	 * 
	 * @param granularity
	 * @param testRadiusX
	 * @param testRadiusY
	 * @param meanShiftWindow
	 */
	public Optimizer(int granularity, int testRadiusX, int testRadiusY, int meanShiftWindow, int maxDepth) {
		this.granularity = granularity;
		this.testRadiusX = testRadiusX;
		this.testRadiusY = testRadiusY;
		this.meanShiftWindow = meanShiftWindow;
		this.maxDepth = maxDepth;
	}
	
	/**
	 * 
	 * @param forest
	 * @param datasets
	 * @throws Exception
	 */
	public ForestMeta optimize(Forest2d forest, List<Dataset> datasets, int numOfClassifyingThreads) throws Exception {
		// Init arrays
		AccuracyTest[] testsOnset = new AccuracyTest[granularity];
		for(int i=0; i<testsOnset.length; i++) {
			testsOnset[i] = new AccuracyTest(testRadiusX, testRadiusY);
		}
		AccuracyTest[] testsOffset = new AccuracyTest[granularity];
		for(int i=0; i<testsOffset.length; i++) {
			testsOffset[i] = new AccuracyTest(testRadiusX, testRadiusY);
		}
		
		// Classify all datasets
		float[][][][] classifications = new float[datasets.size()][][][];
		for(int i=0; i<datasets.size(); i++) {
			TreeDataset2d dataset = (TreeDataset2d)datasets.get(i);
			byte[][] data = (byte[][])dataset.getData();
			System.out.println("Classifying dataset " + (i+1) + "/ " + datasets.size() + ":");
			classifications[i] = forest.classify2d(data, numOfClassifyingThreads, true, maxDepth);
		}
		
		// Multithreaded thresholds search
		System.out.println("Search best thresholds...");
		threadScheduler = new ThreadScheduler(datasets.size());
		synchronized(threadScheduler) {
			OptimizerWorkerGroup group = new OptimizerWorkerGroup(threadScheduler, Forest.THREAD_POLLING_INTERVAL, true);
			for(int i=0; i<datasets.size(); i++) {
				TreeDataset2d dataset = (TreeDataset2d)datasets.get(i);
				OptimizerWorker worker = new OptimizerWorker(group, this, forest, dataset, classifications[i], testsOnset, testsOffset, i);
				group.add(worker);
			}
			group.runGroup();
		}

		// Find best note on threshold
		double max = -Double.MAX_VALUE;
		int bestOnsetIndex = -1;
		for(int i=0; i<granularity; i++) {
			double fd = testsOnset[i].getFalseDetection();
			if (fd == Double.NaN) fd = 0;
			double t = testsOnset[i].getCorrectDetection() - fd;

			if (t > max) {
				max = t;
				bestOnsetIndex = i;
			}
		}
		
		// Find best note off threshold
		max = -Double.MAX_VALUE;
		int bestOffsetIndex = -1;
		for(int i=0; i<granularity; i++) {
			double fd = testsOffset[i].getFalseDetection();
			if (fd == Double.NaN) fd = 0;
			double t = testsOffset[i].getCorrectDetection() - fd;

			if (t > max) {
				max = t;
				bestOffsetIndex = i;
			}
		}

		// Get note length stats
		TDoubleList noteStats = getNoteLengthDistribution(datasets);
		int nlavg = ArrayUtils.getMedianIndex(noteStats.toArray());
		
		// Return meta object
		double bestOnsetThreshold = (double)bestOnsetIndex / granularity;
		double bestOffsetThreshold = (double)bestOffsetIndex / granularity;
		AccuracyTest bestOnsetTest = testsOnset[bestOnsetIndex];
		AccuracyTest bestOffsetTest = testsOffset[bestOffsetIndex];
		return new ForestMeta(bestOnsetThreshold, bestOnsetTest, bestOffsetThreshold, bestOffsetTest, null, noteStats, nlavg);
	}
	
	/**
	 * Called by the worker threads to process one dataset.
	 * 
	 * @param forest
	 * @param dataset
	 * @throws Exception
	 */
	public void processThreaded(Worker worker, Forest2d forest, TreeDataset2d dataset, float[][][] classification, AccuracyTest[] testsOnset, AccuracyTest[] testsOffset, int num) throws Exception {
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
		byte[][] refOff = ArrayUtils.clone(reference); 
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

		    // For each onset, find best offset threshold
		    MeanShift msOff = new MeanShift(meanShiftWindow);
		    msOff.process(dataForestOffset, (float)fThreshold); 

		    testsOnset[i].addData(ms.modeWeights, refOn);
		    testsOffset[i].addData(msOff.modeWeights, refOff);
		    
			worker.setProgress(fThreshold);
		}
		worker.setProgress(1.0);
	}

	/**
	 * Returns the note length distribution among the test data sets.
	 * 
	 * @param datasets
	 * @return
	 * @throws Exception 
	 */
	private TDoubleList getNoteLengthDistribution(List<Dataset> datasets) throws Exception {
		TDoubleList ret = new TDoubleArrayList();
		double max = -Double.MAX_VALUE;
		for(int i=0; i<datasets.size(); i++) {
			TreeDataset dataset = (TreeDataset)datasets.get(i);
			byte[][] ref = (byte[][])dataset.getReference();
			for(int x=0; x<ref.length; x++) {
				for(int y=0; y<ref[0].length; y++) {
					if (ref[x][y] == 1) {
						int dx = 0;
						while(x+dx < ref.length && ref[x+dx][y] != (byte)2) {
							dx++;
						}
						//System.out.println("note: " + x + "/" + y + " dx " + dx);
						while (ret.size() <= dx) {
							ret.add(0); 
						}
						ret.set(dx, ret.get(dx) + 1);
						if (ret.get(dx) > max) max = ret.get(dx);
					}
				}
			}
		}
		ArrayUtils.interpolate(ret);
		ArrayUtils.density(ret);
		// Normalize
		for(int i=0; i<ret.size(); i++) {
			ret.set(i, ret.get(i) / max);
		}
		
		return ret;
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
