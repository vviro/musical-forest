package de.lmu.dbs.musicalforest.actions;

import java.io.File;

import de.lmu.dbs.jforest.core.Dataset;
import de.lmu.dbs.jforest.core2d.Forest2d;
import de.lmu.dbs.jforest.sampler.BootstrapSampler;
import de.lmu.dbs.jspectrum.util.RuntimeMeasure;
import de.lmu.dbs.musicalforest.Action;
import de.lmu.dbs.musicalforest.classifier.DataMeta;
import de.lmu.dbs.musicalforest.classifier.ForestMeta;
import de.lmu.dbs.musicalforest.classifier.OnOffMusicalRandomTree;
import de.lmu.dbs.musicalforest.optimizer.Optimizer;
import de.lmu.dbs.musicalforest.util.Harmonics;

/**
 * Generate optimal thresholds for mean shifting
 * 
 * @author Thomas Weber
 *
 */
public class UpdateAction extends Action {

	private int numOfThreads = -1;
	
	private int maxDepth;
	
	public UpdateAction(String workingFolder, String dataFolder, int threads, int maxDepth) {
		this.dataFolder = dataFolder;
		this.workingFolder = workingFolder;
		this.numOfThreads = threads;
		this.maxDepth = maxDepth;
	}
	
	@Override
	public void process(RuntimeMeasure m) throws Exception {
		checkFolder(new File(workingFolder));
		checkFolder(new File(dataFolder));

		// Load some things
		ForestMeta meta = loadForestMeta(workingFolder, true);
		DataMeta metad = DataMeta.load(dataFolder + File.separator + DATA_META_FILENAME);
		Harmonics.init(OnOffMusicalRandomTree.NUM_OF_OVERTONES, metad.transformParams.binsPerOctave);
		if (!meta.dataMeta.compareTo(metad, true)) throw new Exception("Transform parameters of forest and data do not match");
		BootstrapSampler<Dataset> sampler = loadTrainingData(m, meta.dataMeta.transformParams);
		
		// Load forest
		Forest2d forest = loadForest(m);

		// Calculate optimal thresholds
		int binsPerHalftone = metad.transformParams.getBinsPerHalfTone();
		Optimizer tc = new Optimizer(THRESHOLD_ANALYSIS_GRANULARITY, TEST_TIME_WINDOW, binsPerHalftone, binsPerHalftone, maxDepth); 
		ForestMeta metaT = tc.optimize(forest, sampler.getData(), numOfThreads);
		
		// Update meta data file
		meta.bestOnsetThreshold = metaT.bestOnsetThreshold;
		meta.bestOffsetThreshold = metaT.bestOffsetThreshold;
		meta.bestOnsetThresholdTest = metaT.bestOnsetThresholdTest;
		meta.bestOffsetThresholdTest = metaT.bestOffsetThresholdTest;
		meta.noteLengthDistribution = metaT.noteLengthDistribution;
		meta.noteLengthAvg = metaT.noteLengthAvg;
		meta.maxDepth = maxDepth;
		String mf = workingFolder + File.separator + FOREST_META_FILENAME;
		meta.save(mf);
		m.measure("Finished threshold calculation, updated thresholds in " + mf);
		
		// Print some stats
		//m.measure("Note length distribution: \n" + meta.getNoteLengthDistributionString(20), true);
		m.measure("Note length average: " + meta.noteLengthAvg, true);
		
		m.measure(" -> Best Onset Test: \n" + meta.bestOnsetThresholdTest, true);
		m.measure(" -> Best Offset Test: \n" + meta.bestOffsetThresholdTest, true);
		m.measure(" -> Best Onset threshold: " + meta.bestOnsetThreshold, true);
		m.measure(" -> Best Offset threshold: " + meta.bestOffsetThreshold, true);
		
		m.setSilent(false);
		m.finalMessage("Finished updating forest in");
	}
}
