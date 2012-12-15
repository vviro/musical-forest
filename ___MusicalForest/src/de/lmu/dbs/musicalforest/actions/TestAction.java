package de.lmu.dbs.musicalforest.actions;

import java.io.File;

import de.lmu.dbs.jforest.core.Dataset;
import de.lmu.dbs.jforest.core.TreeDataset;
import de.lmu.dbs.jforest.core2d.Forest2d;
import de.lmu.dbs.jforest.core2d.TreeDataset2d;
import de.lmu.dbs.jforest.sampler.BootstrapSampler;
import de.lmu.dbs.jforest.util.Logfile;
import de.lmu.dbs.jforest.util.MeanShift;
import de.lmu.dbs.jspectrum.util.ArrayUtils;
import de.lmu.dbs.jspectrum.util.RuntimeMeasure;
import de.lmu.dbs.musicalforest.Action;
import de.lmu.dbs.musicalforest.classifier.AccuracyTest;
import de.lmu.dbs.musicalforest.classifier.DataMeta;
import de.lmu.dbs.musicalforest.classifier.ForestMeta;
import de.lmu.dbs.musicalforest.classifier.OnOffMusicalRandomTree;
import de.lmu.dbs.musicalforest.midi.MIDIAdapter;
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
		AccuracyTest testMidiOn = new AccuracyTest(testRadiusX, testRadiusY);
		AccuracyTest testMidiOff = new AccuracyTest(testRadiusX, testRadiusY);
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
		    
		    // Accuracy test (forest)
			byte[][] reference = (byte[][])dataset.getReference();

			byte[][] refOn = ArrayUtils.clone(reference);
			ArrayUtils.filterFirst(refOn);
			
		    byte[][] refOff = ArrayUtils.clone(reference);
			ArrayUtils.filterLast(refOff);

			testOn.addData(ms.modeWeights, refOn);
			testOff.addData(msOff.modeWeights, refOff);

			// Create MIDI from results
		    MIDIAdapter newMidi = new MIDIAdapter(Action.DEFAULT_MIDI_TEMPO);
		    double millisPerStep = (1000.0 * meta.dataMeta.transformParams.step) / meta.dataMeta.sampleRate;
		    int frequencyWindow = meta.dataMeta.transformParams.getBinsPerHalfTone();
		    newMidi.renderFromArrays(ms.modeWeights, msOff.modeWeights, millisPerStep, meta.dataMeta.transformParams.frequencies, frequencyWindow);

		    // Re-render MIDI to array
		    long duration = MIDIAdapter.calculateDuration(dataForestOnset.length, meta.dataMeta.transformParams.step, meta.dataMeta.sampleRate);
		    byte[][] midi = newMidi.toDataArray(dataForestOnset.length, duration, meta.dataMeta.transformParams.frequencies);
		    
		    byte[][] refMidiOn = ArrayUtils.clone(midi);
			ArrayUtils.filterFirst(refMidiOn);
			
			byte[][] refMidiOff = midi; 
			ArrayUtils.filterLast(refOff);

			testMidiOn.addData(refMidiOn, refOn);
			testMidiOff.addData(refMidiOff, refOff);
		}		
		m.measure("Finished mean shift detection and accuracy testing of " + sampler.getPoolSize() + " datasets");
		
		// Print some stats
		m.measure("############ Results ############", true);
		m.measure(" -> Onset Test: \n" + testOn, true);
		m.measure(" -> Offset Test: \n" + testOff, true);
		m.measure(" -> Onset MIDI Test: \n" + testMidiOn, true);
		m.measure(" -> Offset MIDI Test: \n" + testMidiOff, true);
		
		// Save results to text file in forest folder
		Logfile l = new Logfile(workingFolder + File.separator + TEST_LOGFILE_NAME_PREFIX + "_" + (new File(dataFolder)).getName() + ".txt");
		l.write("Results of testing against dataset " + dataFolder + ":");
		l.write("");
		for (int i=0; i<sampler.getPoolSize(); i++) {
			TreeDataset d = (TreeDataset)sampler.get(i);
			l.write("Dataset " + i + ": " + d.getDataFile());
		}
		l.write("");
		l.write("Forest data: Note onset test results: \n" + testOn);
		l.write("Forest data: Note offset test results: \n" + testOff);
		l.write("MIDI Cycle: Note onset test results: \n" + testMidiOn);
		l.write("MIDI Cycle: Note offset test results: \n" + testMidiOff);
		l.write("");
		l.write("CSV:");
		String c = ", ";
		l.write(testOn.getCorrectDetection() + c + testOn.getFalseDetection() + c + testMidiOn.getCorrectDetection() + c + testMidiOn.getFalseDetection() + c);
		l.write(testOff.getCorrectDetection() + c + testOff.getFalseDetection() + c + testMidiOff.getCorrectDetection() + c + testMidiOff.getFalseDetection() + c);
		l.write("");
		l.write("");
		l.write("");
		l.close();
		m.measure("Saved results to text file: " + l.getFilename());
		
		m.setSilent(false);
		m.finalMessage("Finished testing forest in");
	}
}
