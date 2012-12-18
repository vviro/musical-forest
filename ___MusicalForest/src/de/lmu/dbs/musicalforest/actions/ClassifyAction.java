package de.lmu.dbs.musicalforest.actions;

import java.awt.Color;
import java.io.File;

import de.lmu.dbs.jforest.util.MeanShift;
import de.lmu.dbs.jforest.core2d.Forest2d;
import de.lmu.dbs.jforest.core2d.RandomTree2d;
import de.lmu.dbs.jspectrum.util.ArrayToImage;
import de.lmu.dbs.jspectrum.util.ArrayUtils;
import de.lmu.dbs.jspectrum.util.LogScale;
import de.lmu.dbs.jspectrum.util.RuntimeMeasure;
import de.lmu.dbs.jspectrum.util.Scale;
import de.lmu.dbs.musicalforest.Action;
import de.lmu.dbs.musicalforest.classifier.AccuracyTest;
import de.lmu.dbs.musicalforest.classifier.ForestMeta;
import de.lmu.dbs.musicalforest.classifier.OnOffMusicalRandomTree;
import de.lmu.dbs.musicalforest.midi.MIDIAdapter;
import de.lmu.dbs.musicalforest.util.Harmonics;

/**
 * This action classifies an audio file with the musical forest.
 * 
 * @author Thomas Weber
 *
 */
public class ClassifyAction extends Action {

	/**
	 * Classification input file (WAV), will be read.
	 */
	public String classifyInput = null;
	
	/**
	 * Folder where all files are saved to.
	 */
	public String workingFolder = null;
	
	/**
	 * Save extra PNG including MIDI source
	 */
	public String midiFile = null;
	
	/**
	 * If this is not -1, it will override the threshold determined by the training.
	 */
	public double overrideThresholdOnset = -1;
	
	/**
	 * If this is not -1, it will override the threshold determined by the training.
	 */
	public double overrideThresholdOffset = -1;

	/**
	 * Number of threads to classify with
	 */
	public int numberOfThreads = -1;
	
	/**
	 * Save visualization image?
	 */
	public boolean saveImage;
	
	/**
	 * 
	 * @param in
	 * @param outImg
	 */
	public ClassifyAction(String in, String workingFolder, String midiFile, double overrideThresholdOnset, double overrideThresholdOffset, int numberOfThreads, boolean saveImage) {
		this.classifyInput = in;
		this.workingFolder = workingFolder;
		this.midiFile = midiFile;
		this.overrideThresholdOnset = overrideThresholdOnset;
		this.overrideThresholdOffset = overrideThresholdOffset;
		this.numberOfThreads = numberOfThreads;
		this.saveImage = saveImage;
	}
	
	/**
	 * Classify audio file with a loaded forest.
	 * 
	 */
	@Override
	public void process(RuntimeMeasure m) throws Exception {
		checkFolder(new File(workingFolder));
		
		ForestMeta meta = loadForestMeta(workingFolder);

		// Transform and scale audio file
		Scale scale = null;
		if (meta.dataMeta.scaleParam > 0) scale = new LogScale(meta.dataMeta.scaleParam);
		double[][] data = this.transformAudioFile(m, meta.dataMeta.transformParams, scale, classifyInput);
		ArrayUtils.normalize(data, (double)Byte.MAX_VALUE-1);  
		byte[][] byteData = ArrayUtils.toByteArray(data); 

		// Load forest
		RandomTree2d treeFactory = new OnOffMusicalRandomTree(); 
		Forest2d forest = new Forest2d();
		forest.load(workingFolder + File.separator + TrainingAction.NODEDATA_FILE_PREFIX, OnOffMusicalRandomTree.NUM_OF_CLASSES, treeFactory);
		m.measure("Loaded forest from " + workingFolder);
		
		// Classify CQT array with forest
		Harmonics.init(OnOffMusicalRandomTree.NUM_OF_OVERTONES, meta.dataMeta.transformParams.binsPerOctave);
		float[][][] dataForestCl = forest.classify2d(byteData, numberOfThreads, true);
		m.measure("Finished classification");
		
		// Convert format of forest output
		float[][] dataForest = new float[dataForestCl.length][dataForestCl[0].length];
		float[][] dataForestOff = new float[dataForestCl.length][dataForestCl[0].length];
		for (int x=0; x<dataForest.length; x++) {
			for (int y=0; y<dataForest[0].length; y++) {
				dataForest[x][y] = dataForestCl[x][y][1];
				dataForestOff[x][y] = dataForestCl[x][y][2];
			}
		}

		// Set meanshift threshold
		double fThreshold = meta.bestOnsetThreshold; 
		double fThresholdOff = meta.bestOffsetThreshold;
		if (this.overrideThresholdOnset > -1) fThreshold = this.overrideThresholdOnset;
		if (this.overrideThresholdOffset > -1) fThresholdOff = this.overrideThresholdOffset;
		m.measure(" --> Threshold onset: " + fThreshold, true);
		m.measure(" --> Threshold offset: " + fThresholdOff, true);

		// Find local modes using mean shift
		int msWindow = meta.dataMeta.transformParams.getBinsPerHalfTone() * 2;
		MeanShift ms = new MeanShift(msWindow);
	    ms.process(dataForest, (float)fThreshold); 
	    MeanShift msOff = new MeanShift(msWindow);
	    msOff.process(dataForestOff, (float)fThresholdOff); 
	    m.measure("Finished mean shifting");

	    // Extract notes from forest output and save new MIDI file
	    File newMidiFile = new File(workingFolder + File.separator + (new File(classifyInput)).getName() + ".mid");
	    MIDIAdapter newMidi = new MIDIAdapter(DEFAULT_MIDI_TEMPO);
	    double millisPerStep = (1000.0 * meta.dataMeta.transformParams.step) / meta.dataMeta.sampleRate;
	    int frequencyWindow = meta.dataMeta.transformParams.getBinsPerHalfTone();
	    m.measure("Extracted " + newMidi.renderFromArrays(ms.modeWeights, msOff.modeWeights, millisPerStep, meta.dataMeta.transformParams.frequencies, frequencyWindow, meta.noteLengthDistribution, (int)meta.noteLengthAvg) + " MIDI notes from classification output");
	    newMidi.writeFile(newMidiFile);
	    m.measure("Finished generating MIDI into file " + newMidiFile.getAbsolutePath());
	    
	    byte[][] reference = null;
	    byte[][] newMidiData = null;
	    byte[][] refMidiOn = null;
	    byte[][] refMidiOff = null;
	    byte[][] refOn = null; 
	    byte[][] refOff = null; 
		if (midiFile != null) {
		    // Load MIDI file
			MIDIAdapter ma = new MIDIAdapter(new File(midiFile));
			long duration = MIDIAdapter.calculateDuration(data.length, meta.dataMeta.transformParams.step, meta.dataMeta.sampleRate);
			reference = ma.toDataArray(data.length, 0, duration, meta.dataMeta.transformParams.frequencies, true);
			ArrayUtils.shiftRight(reference, DEFAULT_REFERENCE_SHIFT);
			m.measure("Loaded MIDI reference file: " + midiFile);
			
			// Test accuracy (forest)
			int testRadiusX = TEST_TIME_WINDOW;
			int testRadiusY = meta.dataMeta.transformParams.getBinsPerHalfTone();
			
			refOn = ArrayUtils.clone(reference);
			ArrayUtils.filterFirst(refOn);
			AccuracyTest testOns = new AccuracyTest(testRadiusX, testRadiusY);
			testOns.addData(ms.modeWeights, refOn);
	
			refOff = ArrayUtils.clone(reference);
			ArrayUtils.filterLast(refOff);
		    AccuracyTest testOffs = new AccuracyTest(testRadiusX, testRadiusY);
		    testOffs.addData(msOff.modeWeights, refOff);
		    
		    m.measure(" --> Note On Test:  \n" + testOns, true);
		    m.measure(" --> Note Off Test: \n" + testOffs, true);

		    // Test accuracy (MIDI)
			duration = MIDIAdapter.calculateDuration(data.length, meta.dataMeta.transformParams.step, meta.dataMeta.sampleRate);
		    newMidiData = newMidi.toDataArray(data.length, 0, duration, meta.dataMeta.transformParams.frequencies, true);
			
		    refMidiOn = ArrayUtils.clone(newMidiData);
			ArrayUtils.filterFirst(refMidiOn);
			AccuracyTest testMidiOns = new AccuracyTest(testRadiusX, testRadiusY);
			testMidiOns.addData(refMidiOn, refOn);
	
		    refMidiOff = ArrayUtils.clone(newMidiData);
			ArrayUtils.filterLast(refMidiOff);
			AccuracyTest testMidiOffs = new AccuracyTest(testRadiusX, testRadiusY);
			testMidiOffs.addData(refMidiOff, refOff);

		    m.measure(" --> MIDI Note On Test:  \n" + testMidiOns, true);
		    m.measure(" --> MIDI Note Off Test: \n" + testMidiOffs, true);

		    m.measure("Finished accuracy tests");
		}
	    if (saveImage) { 
		    // Prepare image output
		    for(int x=0; x<ms.modeWeights.length; x++) {
		    	for(int y=0; y<ms.modeWeights[0].length; y++) {
		    		if (ms.modeWeights[x][y] > 0) {
		    			ms.modeWeights[x][y] = 1;
		    		} else {
		    			ms.modeWeights[x][y] = 0;
		    		}
		    	}
		    }
		    for(int x=0; x<msOff.modeWeights.length; x++) {
		    	for(int y=0; y<msOff.modeWeights[0].length; y++) {
		    		if (msOff.modeWeights[x][y] > 0) {
		    			msOff.modeWeights[x][y] = 1;
		    		} else {
		    			msOff.modeWeights[x][y] = 0;
		    		}
		    	}
		    }
		    
		    ArrayUtils.blur(ms.modeWeights, 0);
		    ArrayUtils.blur(ms.modeWeights, 0);
		    ArrayUtils.blur(ms.modeWeights, 0);
		    ArrayUtils.blur(msOff.modeWeights, 0);
		    ArrayUtils.blur(msOff.modeWeights, 0);
		    ArrayUtils.blur(msOff.modeWeights, 0);

		    ArrayUtils.blur(reference, 0);
		    
		    // Save image
			String forestImgFile = workingFolder + File.separator + (new File(classifyInput)).getName() + ".png";
			ArrayToImage img = new ArrayToImage(dataForest.length, dataForest[0].length);
			System.out.println("-> Max data: " +  + img.add(data, Color.WHITE, null));
			//System.out.println("-> Max forest: " + img.add(dataForest, Color.RED, null, fThreshold));
			//System.out.println("-> Max forestOff: " + img.add(dataForestOff, Color.GREEN, null, fThresholdOff));
			//System.out.println("-> Max segmentation: " + img.addClassified(ms.segmentation));
			System.out.println("-> Max modes: " + img.add(ms.modeWeights, Color.RED, null, 0));
			//System.out.println("-> Max segmentation off: " + img.addClassified(msOff.segmentation));
			System.out.println("-> Max modes off: " + img.add(msOff.modeWeights, Color.YELLOW, null, 0));
			if (reference != null) {
				System.out.println("-> Max MIDI: " + img.add(reference, Color.BLUE, null, 0)); // TODO aufräumen
			}
			if (newMidiData != null) { 
				System.out.println("-> Max new MIDI: " + img.add(newMidiData, Color.GREEN, null, 0));
			}
			img.save(new File(forestImgFile));
			m.measure("Saved image to " + forestImgFile);
	    }

	    m.setSilent(false);
		m.finalMessage("Finished classification in");
	}

	/**
	 * Classify audio file with a loaded forest.
	 * 
	 *
	@Override
	public void process(RuntimeMeasure m) throws Exception {
		checkFolder(new File(workingFolder));
		
		ForestMeta meta = loadForestMeta(workingFolder);

		// Transform and scale audio file
		Scale scale = null;
		if (meta.dataMeta.scaleParam > 0) scale = new LogScale(meta.dataMeta.scaleParam);
		double[][] data = this.transformAudioFile(m, meta.dataMeta.transformParams, scale, classifyInput);
		ArrayUtils.normalize(data, (double)Byte.MAX_VALUE-1);  
		byte[][] byteData = ArrayUtils.toByteArray(data); 

		// Load forest
		RandomTree2d treeFactory = new OnOffMusicalRandomTree(); 
		Forest2d forest = new Forest2d();
		forest.load(workingFolder + File.separator + TrainingAction.NODEDATA_FILE_PREFIX, OnOffMusicalRandomTree.NUM_OF_CLASSES, treeFactory);
		m.measure("Loaded forest from " + workingFolder);
		
		// Classify CQT array with forest
		Harmonics.init(OnOffMusicalRandomTree.NUM_OF_OVERTONES, meta.dataMeta.transformParams.binsPerOctave);
		float[][][] dataForestCl = forest.classify2d(byteData, numberOfThreads, true);
		m.measure("Finished classification");
		
		// Convert format of forest output
		float[][] dataForest = new float[dataForestCl.length][dataForestCl[0].length];
		//float[][] dataForestOff = new float[dataForestCl.length][dataForestCl[0].length];
		for (int x=0; x<dataForest.length; x++) {
			for (int y=0; y<dataForest[0].length; y++) {
				dataForest[x][y] = dataForestCl[x][y][1];
				//dataForestOff[x][y] = dataForestCl[x][y][2];
			}
		}

		// Set meanshift threshold
		double fThreshold = meta.bestOnsetThreshold; 
		//double fThresholdOff = meta.bestOffsetThreshold;
		if (this.overrideThresholdOnset > -1) fThreshold = this.overrideThresholdOnset;
		//if (this.overrideThresholdOffset > -1) fThresholdOff = this.overrideThresholdOffset;
		m.measure(" --> Threshold onset: " + fThreshold, true);
		//m.measure(" --> Threshold offset: " + fThresholdOff, true);

		// Find local modes using mean shift
		int msWindow = meta.dataMeta.transformParams.getBinsPerHalfTone() * 2;
		MeanShift ms = new MeanShift(msWindow);
	    ms.process(dataForest, (float)fThreshold); 
	    //MeanShift msOff = new MeanShift(msWindow);
	    //msOff.process(dataForestOff, (float)fThresholdOff); 
	    //m.measure("Finished mean shifting");

	    // Klapuri f0 detection
		// Overtone peak detection
		double[][] dataPeak = new double[data.length][];
		DifferentialAnalyzer p = new DifferentialAnalyzer();
		for(int i=0; i<data.length; i++) {
			dataPeak[i] = p.getPeaks(data[i]);
		}
		m.measure("Finished peak detection");
		
		// Basic f0 detection
		double[][] dataF0 = new double[data.length][];
		ArrayUtils.normalize(dataPeak);
		F0Detector f0d = new F0Detector(meta.dataMeta.transformParams.frequencies, 20);
		for(int i=0; i<data.length; i++) {
			dataF0[i] = f0d.getF0Poly(dataPeak[i], 5);
		}
		m.measure("Finished basic f0 detection");
	    
	    // Extract notes from forest output and save new MIDI file
	    /*File newMidiFile = new File(workingFolder + File.separator + (new File(classifyInput)).getName() + ".mid");
	    MIDIAdapter newMidi = new MIDIAdapter(DEFAULT_MIDI_TEMPO);
	    double millisPerStep = (1000.0 * meta.dataMeta.transformParams.step) / meta.dataMeta.sampleRate;
	    int frequencyWindow = meta.dataMeta.transformParams.getBinsPerHalfTone();
	    m.measure("Extracted " + newMidi.renderFromArrays(ms.modeWeights, msOff.modeWeights, millisPerStep, meta.dataMeta.transformParams.frequencies, frequencyWindow) + " MIDI notes from classification output");
	    newMidi.writeFile(newMidiFile);
	    m.measure("Finished generating MIDI into file " + newMidiFile.getAbsolutePath());
	    *
	    byte[][] reference = null;
	    byte[][] newMidiData = null;
	    byte[][] refMidiOn = null;
	    byte[][] refMidiOff = null;
	    byte[][] refOn = null; 
	    byte[][] refOff = null; 
		if (midiFile != null) {
		    // Load MIDI file
			MIDIAdapter ma = new MIDIAdapter(new File(midiFile));
			long duration = MIDIAdapter.calculateDuration(data.length, meta.dataMeta.transformParams.step, meta.dataMeta.sampleRate);
			reference = ma.toDataArray(data.length, duration, meta.dataMeta.transformParams.frequencies, true);
			ArrayUtils.shiftRight(reference, DEFAULT_REFERENCE_SHIFT);
			m.measure("Loaded MIDI reference file: " + midiFile);
			
			// Test accuracy (forest)
			int testRadiusX = TEST_TIME_WINDOW;
			int testRadiusY = meta.dataMeta.transformParams.getBinsPerHalfTone();
			
			refOn = ArrayUtils.clone(reference);
			ArrayUtils.filterFirst(refOn);
			AccuracyTest testOns = new AccuracyTest(testRadiusX, testRadiusY);
			testOns.addData(ms.modeWeights, refOn);
	
			/*refOff = ArrayUtils.clone(reference);
			ArrayUtils.filterLast(refOff);
		    AccuracyTest testOffs = new AccuracyTest(testRadiusX, testRadiusY);
		    testOffs.addData(msOff.modeWeights, refOff);*
		    
		    m.measure(" --> Note On Test:  \n" + testOns, true);
		    //m.measure(" --> Note Off Test: \n" + testOffs, true);

		    // Test accuracy (MIDI)
		    /*
			duration = MIDIAdapter.calculateDuration(data.length, meta.dataMeta.transformParams.step, meta.dataMeta.sampleRate);
		    newMidiData = newMidi.toDataArray(data.length, duration, meta.dataMeta.transformParams.frequencies, true);
			
		    refMidiOn = ArrayUtils.clone(newMidiData);
			ArrayUtils.filterFirst(refMidiOn);
			AccuracyTest testMidiOns = new AccuracyTest(testRadiusX, testRadiusY);
			testMidiOns.addData(refMidiOn, refOn);
	
		    refMidiOff = ArrayUtils.clone(newMidiData);
			ArrayUtils.filterLast(refMidiOff);
			AccuracyTest testMidiOffs = new AccuracyTest(testRadiusX, testRadiusY);
			testMidiOffs.addData(refMidiOff, refOff);

		    m.measure(" --> MIDI Note On Test:  \n" + testMidiOns, true);
		    m.measure(" --> MIDI Note Off Test: \n" + testMidiOffs, true);
		    m.measure("Finished accuracy tests");
		     *
		}
	    if (saveImage) { 
		    // Prepare image output
		    for(int x=0; x<ms.modeWeights.length; x++) {
		    	for(int y=0; y<ms.modeWeights[0].length; y++) {
		    		if (ms.modeWeights[x][y] > 0) {
		    			ms.modeWeights[x][y] = 1;
		    		} else {
		    			ms.modeWeights[x][y] = 0;
		    		}
		    	}
		    }
		    /*
		    for(int x=0; x<msOff.modeWeights.length; x++) {
		    	for(int y=0; y<msOff.modeWeights[0].length; y++) {
		    		if (msOff.modeWeights[x][y] > 0) {
		    			msOff.modeWeights[x][y] = 1;
		    		} else {
		    			msOff.modeWeights[x][y] = 0;
		    		}
		    	}
		    }*
		    ArrayUtils.blur(ms.modeWeights, 0);
		    ArrayUtils.blur(ms.modeWeights, 0);
		    ArrayUtils.blur(ms.modeWeights, 0);
		    /*ArrayUtils.blur(msOff.modeWeights, 0);
		    ArrayUtils.blur(msOff.modeWeights, 0);
		    ArrayUtils.blur(msOff.modeWeights, 0);
//*
		    ArrayUtils.blur(reference, 0);
		    
		    // Save image
			String forestImgFile = workingFolder + File.separator + (new File(classifyInput)).getName() + ".png";
			ArrayToImage img = new ArrayToImage(dataForest.length, dataForest[0].length);
			System.out.println("-> Max data: " +  + img.add(data, Color.WHITE, null));
			//System.out.println("-> Max forest: " + img.add(dataForest, Color.RED, null, fThreshold));
			//System.out.println("-> Max forestOff: " + img.add(dataForestOff, Color.GREEN, null, fThresholdOff));
			//System.out.println("-> Max segmentation: " + img.addClassified(ms.segmentation));
			System.out.println("-> Max modes: " + img.add(ms.modeWeights, Color.RED, null, 0));
			//System.out.println("-> Max segmentation off: " + img.addClassified(msOff.segmentation));
			//System.out.println("-> Max modes off: " + img.add(msOff.modeWeights, Color.YELLOW, null, 0));
			if (reference != null) {
				System.out.println("-> Max MIDI: " + img.add(reference, Color.BLUE, null, 0)); // TODO aufräumen
			}
			if (newMidiData != null) { 
				//System.out.println("-> Max new MIDI: " + img.add(newMidiData, Color.GREEN, null, 0));
				//System.out.println("-> Max new MIDI: " + img.add(refMidiOn, Color.GREEN, null, 0));
				//System.out.println("-> Max new MIDI: " + img.add(refMidiOff, Color.red, null, 0));
			}
			//System.out.println(img.add(refMidiOn, Color.red, null, 0));
			//System.out.println(img.add(refOn, Color.blue, null, 0));
			System.out.println(img.add(dataF0, Color.GREEN, null, 0));
			img.save(new File(forestImgFile));
			m.measure("Saved image to " + forestImgFile);
	    }

	    m.setSilent(false);
		m.finalMessage("Finished classification in");
	}
	//*/
}
