package de.lmu.dbs.musicalforest.actions;

import java.io.File;

import de.lmu.dbs.jforest.core.Dataset;
import de.lmu.dbs.jforest.core2d.Forest2d;
import de.lmu.dbs.jforest.core2d.RandomTree2d;
import de.lmu.dbs.jforest.sampler.BootstrapSampler;
import de.lmu.dbs.jspectrum.util.RuntimeMeasure;
import de.lmu.dbs.musicalforest.Action;
import de.lmu.dbs.musicalforest.classifier.DataMeta;
import de.lmu.dbs.musicalforest.classifier.ForestMeta;
import de.lmu.dbs.musicalforest.classifier.OnOffMusicalRandomTree;
import de.lmu.dbs.musicalforest.util.Harmonics;

/**
 * 
 * @author Thomas Weber
 *
 */
public class ExpandAction extends Action {

	/**
	 * 
	 *
	private int numThreads;
	
	/**
	 * Source forest 
	 */
	private String sourceFolder;
	
	/**
	 * 
	 * @param workingFolder
	 * @param settingsFile
	 */
	public ExpandAction(String workingFolder, String dataFolder, String sourceFolder, int numThreads) {
		this.workingFolder = workingFolder;
		this.dataFolder = dataFolder;
		//this.numThreads = numThreads; 
		this.sourceFolder = sourceFolder;
	}
	
	/**
	 * 
	 * @throws Exception 
	 */
	@Override
	public void process(RuntimeMeasure m) throws Exception {
		m.measure("Java Heap size (maximum): " + ((double)Runtime.getRuntime().maxMemory() / (1024*1024)) + " MB");
		checkFolder(new File(dataFolder));
		checkFolder(new File(sourceFolder));

		// Load Prerequisites
		DataMeta dataMeta = DataMeta.load(dataFolder + File.separator + DATA_META_FILENAME);
		BootstrapSampler<Dataset> sampler = loadTrainingData(m, dataMeta.transformParams);
		
		// Create result folder
		createWorkingFolder(m);

		// Load forest
		RandomTree2d treeFactory = new OnOffMusicalRandomTree(); 
		Forest2d forest = new Forest2d();
		forest.load(sourceFolder + File.separator + TrainingAction.NODEDATA_FILE_PREFIX, OnOffMusicalRandomTree.NUM_OF_CLASSES, treeFactory);
		m.measure("Loaded forest from " + sourceFolder);
		
		// Collect and save meta data file
		ForestMeta meta = loadForestMeta(sourceFolder, true);
		String mf = workingFolder + File.separator + FOREST_META_FILENAME;
		meta.save(mf, true);
		m.measure("Finished copying meta data to " + mf);
		
		// Expand forest
		Harmonics.init(OnOffMusicalRandomTree.NUM_OF_OVERTONES, dataMeta.transformParams.binsPerOctave);
		forest.expand(sampler, sourceFolder + File.separator + "bootstrap_");
		
		// Save expanded forest
		forest.save(workingFolder + File.separator + NODEDATA_FILE_PREFIX);
		m.measure("Finished saving forest to folder: " + workingFolder, true);		
		
		m.setSilent(false);
		m.finalMessage("Finished expanding forest in");
	}
}
