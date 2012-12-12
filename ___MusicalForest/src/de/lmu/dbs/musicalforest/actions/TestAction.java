package de.lmu.dbs.musicalforest.actions;

import java.io.File;

import de.lmu.dbs.jforest.core.Dataset;
import de.lmu.dbs.jforest.core2d.Forest2d;
import de.lmu.dbs.jforest.core2d.TreeDataset2d;
import de.lmu.dbs.jforest.sampler.BootstrapSampler;
import de.lmu.dbs.jforest.util.MeanShift;
import de.lmu.dbs.jspectrum.util.ArrayUtils;
import de.lmu.dbs.jspectrum.util.RuntimeMeasure;
import de.lmu.dbs.musicalforest.Action;
import de.lmu.dbs.musicalforest.classifier.AccuracyTest;
import de.lmu.dbs.musicalforest.classifier.DataMeta;
import de.lmu.dbs.musicalforest.classifier.ForestMeta;
import de.lmu.dbs.musicalforest.classifier.OnOffMusicalRandomTree;
import de.lmu.dbs.musicalforest.util.Harmonics;

/**
 * Generate optimal thresholds for mean shifting
 * 
 * @author Thomas Weber
 *
 */
public class TestAction extends Action {

	private int numOfThreads = -1;
	
	private boolean force;
	
	public TestAction(String workingFolder, String dataFolder, int threads, boolean force) {
		this.dataFolder = dataFolder;
		this.workingFolder = workingFolder;
		this.numOfThreads = threads;
		this.force = force;
	}
	
	@Override
	public void process(RuntimeMeasure m) throws Exception {
		checkFolder(new File(workingFolder));
		checkFolder(new File(dataFolder));

		// Load some things
		ForestMeta meta = loadForestMeta(workingFolder, true);
		DataMeta metad = DataMeta.load(dataFolder + File.separator + DATA_META_FILENAME);
		Harmonics.init(OnOffMusicalRandomTree.NUM_OF_OVERTONES, metad.transformParams.binsPerOctave);
		if (!meta.dataMeta.compareTo(metad, true)) {
			if (force) {
				System.out.println("WARNING: Transform parameters of forest and data do not match");
			} else {
				throw new Exception("Transform parameters of forest and data do not match");
			}
		}

		// Load data
		BootstrapSampler<Dataset> sampler = loadTrainingData(m, meta.dataMeta.transformParams);
		
		// Load forest
		Forest2d forest = loadForest(m);
		
		// Classify all datasets
		float[][][][] classifications = new float[sampler.getPoolSize()][][][];
		for(int i=0; i<sampler.getPoolSize(); i++) {
			TreeDataset2d dataset = (TreeDataset2d)sampler.get(i);
			byte[][] data = (byte[][])dataset.getData();
			System.out.println("Classifying dataset " + (i+1) + "/ " + sampler.getPoolSize() + ":");
			classifications[i] = forest.classify2d(data, numOfThreads, true);
		}
		m.measure("Finished classification of " + sampler.getPoolSize() + " datasets");

		// Mean shifting
		int testRadiusX = TEST_TIME_WINDOW;
		int testRadiusY = meta.dataMeta.transformParams.getBinsPerHalfTone();
		AccuracyTest testOn = new AccuracyTest(testRadiusX, testRadiusY);
		AccuracyTest testOff = new AccuracyTest(testRadiusX, testRadiusY);
		for(int i=0; i<sampler.getPoolSize(); i++) {
			TreeDataset2d dataset = (TreeDataset2d)sampler.get(i);

			// Mean shifting
			float[][] dataForestOnset = new float[classifications[i].length][classifications[i][0].length];
			float[][] dataForestOffset = new float[classifications[i].length][classifications[i][0].length];
			for (int x=0; x<dataForestOnset.length; x++) {
				for (int y=0; y<dataForestOnset[0].length; y++) {
					dataForestOnset[x][y] = classifications[i][x][y][1];
					dataForestOffset[x][y] = classifications[i][x][y][2];
				}
			}
			int msWindow = meta.dataMeta.transformParams.getBinsPerHalfTone() * 2;
			MeanShift ms = new MeanShift(msWindow);
		    ms.process(dataForestOnset, (float)meta.bestOnsetThreshold); 
		    MeanShift msOff = new MeanShift(msWindow);
		    msOff.process(dataForestOffset, (float)meta.bestOffsetThreshold);
		    
		    // Accuracy test
			byte[][] reference = (byte[][])dataset.getReference();
			byte[][] refOn = ArrayUtils.clone(reference);
			ArrayUtils.filterFirst(refOn);
			testOn.addData(ms.modeWeights, refOn);
			byte[][] refOff = reference; 
			ArrayUtils.filterLast(refOff);
		    testOn.addData(msOff.modeWeights, refOff);
		}		
		m.measure("Finished mean shift detection and accuracy testing of " + sampler.getPoolSize() + " datasets");
		
		// Print some stats
		m.measure("############ Results ############", true);
		m.measure(" -> Onset Test: \n" + testOn, true);
		m.measure(" -> Offset Test: \n" + testOff, true);
		
		m.setSilent(false);
		m.finalMessage("Finished testing forest in");
	}
}
