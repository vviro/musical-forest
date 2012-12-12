package de.lmu.dbs.musicalforest.actions;

import java.io.File;

import de.lmu.dbs.jforest.core.Dataset;
import de.lmu.dbs.jforest.core.ForestParameters;
import de.lmu.dbs.jforest.sampler.BootstrapSampler;
import de.lmu.dbs.jspectrum.util.RuntimeMeasure;
import de.lmu.dbs.musicalforest.Action;
import de.lmu.dbs.musicalforest.classifier.DataMeta;
import de.lmu.dbs.musicalforest.classifier.ForestMeta;
import de.lmu.dbs.musicalforest.classifier.OnOffMusicalRandomTree;
import de.lmu.dbs.musicalforest.util.Harmonics;

/**
 * This action trains a random musical forest on test data and stores the results in a given working folder.
 * The output of this action can be used to classify audio data with class ClassifyAction.
 * 
 * @author Thomas Weber
 *
 */
public class TrainingAction extends Action {

	/**
	 * 
	 */
	private int maxNumOfEvalThreads;
	
	/**
	 * 
	 */
	private int maxNumOfNodeThreads;
	
	/**
	 * 
	 */
	private int nodeThreadingThreshold;
	
	/**
	 * 
	 * @param workingFolder
	 * @param settingsFile
	 */
	public TrainingAction(String workingFolder, String settingsFile, String dataFolder) {
		this.workingFolder = workingFolder;
		this.settingsFile = settingsFile;
		this.dataFolder = dataFolder;
	}
	
	/**
	 * Sets multithreading parameters (without calling this, no multithreading will happen)
	 * 
	 */
	public void setThreadingParams(int evalThreads, int nodeThreads, int nodeThreshold) {
		this.maxNumOfEvalThreads = evalThreads;
		this.maxNumOfNodeThreads = nodeThreads;
		this.nodeThreadingThreshold = nodeThreshold;
	}
	
	/**
	 * Trains a forest upon test data.
	 * 
	 * @throws Exception 
	 */
	@Override
	public void process(RuntimeMeasure m) throws Exception {
		m.measure("Java Heap size (maximum): " + ((double)Runtime.getRuntime().maxMemory() / (1024*1024)) + " MB");
		checkFolder(new File(dataFolder));

		// Load Prerequisites
		ForestParameters fparams = loadForestParams(m, settingsFile);
		DataMeta dataMeta = DataMeta.load(dataFolder + File.separator + DATA_META_FILENAME);
		BootstrapSampler<Dataset> sampler = loadTrainingData(m, dataMeta.transformParams);
		
		// Create result folder
		createWorkingFolder(m);

		// Grow forest
		Harmonics.init(OnOffMusicalRandomTree.NUM_OF_OVERTONES, dataMeta.transformParams.binsPerOctave);
		growForest(m, fparams, sampler, maxNumOfEvalThreads, maxNumOfNodeThreads, nodeThreadingThreshold);
		
		/*
		Forest2d forest = growForest(m, fparams, sampler, maxNumOfEvalThreads, maxNumOfNodeThreads, nodeThreadingThreshold);
		
		// Calculate optimal thresholds
		int binsPerHalftone = dataMeta.transformParams.getBinsPerHalfTone();
		ThresholdOptimizer tc = new ThresholdOptimizer(THRESHOLD_ANALYSIS_GRANULARITY, TEST_TIME_WINDOW, binsPerHalftone, binsPerHalftone);
		ForestMeta meta = tc.calculateThresholds(forest, sampler.getData(), maxNumOfEvalThreads);
		*/
		
		// Collect and save meta data file
		ForestMeta meta = new ForestMeta();
		meta.forestParams = fparams;
		meta.dataMeta = dataMeta;
		String mf = workingFolder + File.separator + FOREST_META_FILENAME;
		meta.save(mf, true);
		m.measure("Finished generating meta data to " + mf);
		
		/*
		// Print some threhold stats
		m.measure(" -> Acc. Onset Test: \n" + meta.bestOnsetThresholdTest, true);
		m.measure(" -> Acc. Offset Test: \n" + meta.bestOffsetThresholdTest, true);
		m.measure(" -> Best Onset threshold: " + meta.bestOnsetThreshold, true);
		m.measure(" -> Best Offset threshold: " + meta.bestOffsetThreshold, true);
		//*/
		
		m.setSilent(false);
		m.finalMessage("Finished training forest in");
	}
}
