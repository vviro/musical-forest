package de.lmu.dbs.musicalforest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import de.lmu.dbs.jforest.core.Dataset;
import de.lmu.dbs.jforest.core.ForestParameters;
import de.lmu.dbs.jforest.core.RandomTree;
import de.lmu.dbs.jforest.core2d.Forest2d;
import de.lmu.dbs.jforest.core2d.RandomTree2d;
import de.lmu.dbs.jforest.sampler.BootstrapSampler;
import de.lmu.dbs.jforest.sampler.Sampler;
import de.lmu.dbs.jforest.util.Logfile;
import de.lmu.dbs.jspectrum.ShortTimeConstantQTransform;
import de.lmu.dbs.jspectrum.ShortTimeTransform;
import de.lmu.dbs.jspectrum.TransformParameters;
import de.lmu.dbs.jspectrum.util.ArrayUtils;
import de.lmu.dbs.jspectrum.util.HammingWindow;
import de.lmu.dbs.jspectrum.util.LogScale;
import de.lmu.dbs.jspectrum.util.RuntimeMeasure;
import de.lmu.dbs.jspectrum.util.Sample;
import de.lmu.dbs.jspectrum.util.Scale;
import de.lmu.dbs.jspectrum.util.WaveSample;
import de.lmu.dbs.musicalforest.classifier.ForestMeta;
import de.lmu.dbs.musicalforest.classifier.ForestMetaException;
import de.lmu.dbs.musicalforest.classifier.OnOffMusicalRandomTree;
import de.lmu.dbs.musicalforest.classifier.OnOffMusicalTreeDataset;

/**
 * Abstract action class. Implements lots of code shared among the musical
 * forest actions, also most of the constants are kept here. 
 * <br><br>
 * See documentation for details.
 * 
 * @author Thomas Weber
 *
 */
public abstract class Action {

	/**
	 * Name of the metadata file for a forest
	 */
	public static final String FOREST_META_FILENAME = "meta.forest"; 

	/**
	 * File name of the training/test data meta file
	 */
	public static final String DATA_META_FILENAME = "meta.data"; 

	/**
	 * Prefix for forest data files.
	 */
	public static final String NODEDATA_FILE_PREFIX = "nodedata_tree";
	
	/**
	 * file name prefix for the test output files
	 */
	public static final String TEST_LOGFILE_NAME_PREFIX = "foresttest";
	
	/**
	 * Global shift to apply to reference arrays. For synchronization purposes.
	 */
	public static final int DEFAULT_REFERENCE_SHIFT = 3;

	/**
	 * Window size in the time domain to check accuracy. 
	 */
	public static final int TEST_TIME_WINDOW = 10;
	
	/**
	 * Number of threshold values tested in update mode (see class ThresholdAnalyzer)
	 */
	public static final int THRESHOLD_ANALYSIS_GRANULARITY = 20;
	
	/**
	 * Default MIDI tempo used to render classified MIDI files.
	 */
	public static final double DEFAULT_MIDI_TEMPO = 120;
	
	/**
	 * Working folder during training.
	 */
	public String workingFolder = null;

	/**
	 * Folder which contains the training data.
	 */
	public String dataFolder = null;
	
	/**
	 * Settings file for training.
	 */
	public String settingsFile = null;
	
	/**
	 * See method transformAudioFile() documentation
	 */
	public Sample audioSample;
	
	/**
	 * Process the action.
	 * 
	 * @param m
	 * @throws Exception
	 */
	public abstract void process(RuntimeMeasure m) throws Exception;
	
	/**
	 * Load forest parameters from a settings file.
	 *  
	 * @param m
	 * @param settingsFile
	 * @return
	 * @throws Exception
	 */
	public ForestParameters loadForestParams(RuntimeMeasure m, String settingsFile) throws Exception {
		// Load Forest params
		if (settingsFile == null) throw new Exception("No settings file given");
		ForestParameters fparams = new ForestParameters();
		fparams.loadParameters(settingsFile);
		fparams.debugFolder = workingFolder;
		fparams.check();
		m.measure("Loaded forest settings from file " + settingsFile + ", debugging goes to " + workingFolder);
		return fparams;
	}

	/**
	 * Audio source loaded in last call of transformAudioFile()
	 */
	protected int[] audioSrc = null;

	/**
	 * Load and CQT transform an audio (WAV) file. The returned data array is normalized to range [0..1].
	 * After execution, the WavSample instance can be accessed by attribute audioSample.
	 * 
	 * @return
	 * @throws Exception 
	 */
	public double[][] transformAudioFile(RuntimeMeasure m, TransformParameters params, Scale scale, String file) throws Exception {
		// Load sample
		audioSample = new WaveSample(new File(file));
		m.measure("Loaded audio data from file " + file);

		ShortTimeTransform transformation = new ShortTimeConstantQTransform((double)audioSample.getSampleRate(), params.fMin, params.fMax, params.binsPerOctave, params.threshold, params.spread, params.divideFFT, params.cqtKernelBufferLocation);
		m.measure("Initialized transformation");
		
		// Make mono
		audioSrc = audioSample.getMono();
		double len = (double)audioSrc.length / audioSample.getSampleRate();
		m.measure("Mono audio Length: " + len + "sec, " + audioSrc.length + " samples");

		// Calculate transformation
		double[][] data = transformation.calculate(audioSrc, params.step, new HammingWindow(transformation.getWindowSize()));
		ArrayUtils.normalize(data); // Normalize to [0,1]
		if (scale != null) ArrayUtils.scale(data, scale); // Log scale
		if (scale != null) m.measure("Finished transformation and scaling (" + ((LogScale)scale).getWidth() + ")");
		else m.measure("Finished transformation without scaling.");
		
		params.frequencies = transformation.getFrequencies();
		return data;
	}
	
	/**
	 * Load everything needed to perform some forest action. The passed args will be filled with the data.
	 * If null is passed, that particular part is not loaded.
	 * 
	 * @throws Exception 
	 * 
	 */
	public BootstrapSampler<Dataset> loadTrainingData(RuntimeMeasure m, TransformParameters tparams) throws Exception {
		if (dataFolder == null) throw new Exception("No working folder given");
		// Collecting test/training samples and create sampler
		List<Dataset> ret = new ArrayList<Dataset>();
		OnOffMusicalTreeDataset.loadDatasets(ret, dataFolder, tparams.frequencies, tparams.step, true);
		BootstrapSampler<Dataset> ret2 = new BootstrapSampler<Dataset>(ret);
		m.measure("Finished generating " + ret.size() + " initial data samples");
		return ret2;
	}
	
	/**
	 * Creates (and resets) the working folder
	 * 
	 * @param m
	 * @throws IOException 
	 */
	public void createWorkingFolder(RuntimeMeasure m) throws IOException {
		// Create result folder
		File wf = new File(workingFolder);
		if (wf.exists()) FileUtils.deleteDirectory(wf);
		wf.mkdirs();
		m.measure("Created working folder " + workingFolder);
	}
	
	/**
	 * Grows a new forest and saves it in the working folder (to default file name).
	 * 
	 * @param m
	 * @param fparams
	 * @param sampler
	 * @throws Exception
	 */
	public Forest2d growForest(RuntimeMeasure m, ForestParameters fparams, Sampler<Dataset> sampler, int maxNumOfEvalThreads, int maxNumOfNodeThreads, int nodeThreadingThreshold) throws Exception {
		// Grow forest
		Logfile[] treelogs = new Logfile[fparams.forestSize]; 
		List<RandomTree> trees = new ArrayList<RandomTree>();
		for(int i=0; i<fparams.forestSize; i++) {
			treelogs[i] = new Logfile(workingFolder + File.separator + "T" + i + "_Growlog.txt");
			RandomTree t = new OnOffMusicalRandomTree(fparams, i, treelogs[i]);
			trees.add(t);
		}
		Logfile forestlog = new Logfile(workingFolder + File.separator + "Forest_Stats.txt");
		Forest2d forest = new Forest2d(trees, fparams, forestlog, maxNumOfEvalThreads, maxNumOfNodeThreads, nodeThreadingThreshold);
		forest.grow(sampler, workingFolder + File.separator + "bootstrap_");
		m.measure("Finished growing random forest");

		forest.save(workingFolder + File.separator + NODEDATA_FILE_PREFIX);
		m.measure("Finished saving forest to folder: " + workingFolder);

		// Save bootstrapping arrays
		//for(int i=0; i<trees.size(); i++) {
			//trees.get(i).saveBootstrapArray(workingFolder + File.separator + "bootstrap_" + i);
		//}
		
		forest.logStats();
		m.measure("Finished logging forest stats");

		// Close logs
		for(int i=0; i<treelogs.length; i++) {
			treelogs[i].close();
		}
		forestlog.close();
		m.measure("Finished closing logs");
		return forest;
	}

	/**
	 * Grows a new forest and saves it in the working folder (to default file name).
	 * 
	 * @param m
	 * @param fparams
	 * @param sampler
	 * @throws Exception
	 *
	public Forest2d expandForest(RuntimeMeasure m, ForestParameters fparams, Sampler<Dataset> sampler, int maxNumOfEvalThreads, int maxNumOfNodeThreads, int nodeThreadingThreshold) throws Exception {

		// Expand forest
		Logfile[] treelogs = new Logfile[fparams.forestSize]; 
		Logfile forestlog = new Logfile(workingFolder + File.separator + "Forest_Stats.txt");
		List<RandomTree> trees = null;
		RandomTree2d treeFactory = new OnOffMusicalRandomTree(); 
		Forest2d forest = new Forest2d(trees, fparams, forestlog, maxNumOfEvalThreads, maxNumOfNodeThreads, nodeThreadingThreshold);
		forest.load(workingFolder + File.separator + TrainingAction.NODEDATA_FILE_PREFIX, OnOffMusicalRandomTree.NUM_OF_CLASSES, treeFactory);
		forest.grow(sampler);
		m.measure("Finished growing random forest");

		forest.save(workingFolder + File.separator + NODEDATA_FILE_PREFIX);
		m.measure("Finished saving forest to folder: " + workingFolder);

		forest.logStats();
		m.measure("Finished logging forest stats");

		// Close logs
		for(int i=0; i<treelogs.length; i++) {
			treelogs[i].close();
		}
		forestlog.close();
		m.measure("Finished closing logs");
		return forest;
	}

	/**
	 * Load a forest meta file.
	 * 
	 * @param dir
	 * @return
	 * @throws Exception
	 */
	public ForestMeta loadForestMeta(String dir) throws Exception {
		return loadForestMeta(dir, false);
	}
	
	/**
	 * Load a forest meta file.
	 * 
	 * @param dir
	 * @return
	 * @throws Exception
	 */
	public ForestMeta loadForestMeta(String dir, boolean force) throws Exception {
		File meta = new File(dir + File.separator + FOREST_META_FILENAME);
		if (!meta.exists()) throw new ForestMetaException("This forest folder contains no meta information, try performing update action");
		return ForestMeta.load(meta.getAbsolutePath(), force);
	}
	
	/**
	 * Loads the saved forest (from default file name in the working folder).
	 * 
	 * @return
	 * @throws Exception
	 */
	public Forest2d loadForest(RuntimeMeasure m) throws Exception {
		RandomTree2d treeFactory = new OnOffMusicalRandomTree(); 
		Forest2d forest = new Forest2d();
		forest.load(workingFolder + File.separator + NODEDATA_FILE_PREFIX, 3, treeFactory);
		m.measure("Finished loading forest");
		return forest;
	}
	
	/**
	 * Checks if dir exists and is a directory.
	 * 
	 * @param dir
	 * @throws Exception
	 */
	public void checkFolder(File dir) throws Exception {
		if (!dir.exists() || !dir.isDirectory()) throw new Exception(dir.getAbsolutePath() + " does not exist or is no directory");
	}
	
	/**
	 * Writes CSV containing accuracy rates.
	 * @param fmeta 
	 * 
	 * @param fmeta
	 * @param csvFile2
	 * @throws Exception 
	 */
	public void writeCSV(ForestMeta meta, File fmeta, String filename, boolean perc, int maxDepth) throws Exception {
		FileWriter fstream = new FileWriter(filename, true);
		BufferedWriter out = new BufferedWriter(fstream);
		
		String sep = ",";
		String sepLine = "\n";
		
		if (!perc) {
			out.write(
				fmeta.getAbsolutePath() + 
				sep + 
				maxDepth + 
				sep + 
				meta.bestOnsetThresholdTest.getCorrectDetection() + 
				sep + 
				meta.bestOnsetThresholdTest.getFalseDetection() + 
				sep + 
				meta.bestOffsetThresholdTest.getCorrectDetection() + 
				sep + 
				meta.bestOffsetThresholdTest.getFalseDetection() +
				sepLine
			);
		} else {
			out.write(
					fmeta.getAbsolutePath() + 
					sep + 
					maxDepth + 
					sep + 
					Math.round(meta.bestOnsetThresholdTest.getCorrectDetection()*100) + 
					sep + 
					Math.round(meta.bestOnsetThresholdTest.getFalseDetection()*100) + 
					sep + 
					Math.round(meta.bestOffsetThresholdTest.getCorrectDetection()*100) + 
					sep + 
					Math.round(meta.bestOffsetThresholdTest.getFalseDetection()*100) +
					sepLine
				);
		}
		out.close();		
	}

	/**
	 * Writes CSV containing accuracy rates.
	 * @param fmeta 
	 * 
	 * @param fmeta
	 * @param csvFile2
	 * @throws Exception 
	 */
	public void writeCSVLen(ForestMeta meta, String filename, boolean perc) throws Exception {
		FileWriter fstream = new FileWriter(filename, false);
		BufferedWriter out = new BufferedWriter(fstream);
		
		String sep = ",";
		String sepLine = "\n";
		
		for(int i=0; i<meta.noteLengthDistribution.size(); i++) {
			out.write(
					i + 
					sep +
					(perc ? Math.round(meta.noteLengthDistribution.get(i)*100) : meta.noteLengthDistribution.get(i)) + 
					sepLine
			);
		}
		out.close();		
	}

}
