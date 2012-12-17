package de.lmu.dbs.musicalforest;

import java.io.IOException;

import de.lmu.dbs.jspectrum.util.RuntimeMeasure;
import de.lmu.dbs.musicalforest.actions.ClassifyAction;
import de.lmu.dbs.musicalforest.actions.GenerateDataAction;
import de.lmu.dbs.musicalforest.actions.MergeForestsAction;
import de.lmu.dbs.musicalforest.actions.GenerateMidiAction;
import de.lmu.dbs.musicalforest.actions.ModifyAction;
import de.lmu.dbs.musicalforest.actions.SpectrumAction;
import de.lmu.dbs.musicalforest.actions.TestAction;
import de.lmu.dbs.musicalforest.actions.UpdateAction;
import de.lmu.dbs.musicalforest.actions.TrainingAction;
import de.lmu.dbs.musicalforest.actions.ViewAction;
import de.lmu.dbs.musicalforest.classifier.ForestMetaException;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * Main class for Musical Forest application. Contains mainly CLI parsing, the algorithms are 
 * implemented in the Action classes (see package de.lmu.dbs.musicalforest.actions).
 * 
 * @author Thomas Weber
 *
 */
public class MusicalForest {

	/**
	 * Exit code if arguments are incorrect
	 */
	public static final int ARGS_ERROR_EXIT_CODE = 1;

	/**
	 * Exit code for other errors
	 */
	public static final int ERROR_EXIT_CODE = 8;
	
	/**
	 * Dont ouput any messages
	 */
	public boolean silent = false;
	
	/**
	 * Action to be performed
	 */
	public Action action = null;
	
	/**
	 * Creates an application instance.
	 * 
	 * @param args
	 * @throws Exception 
	 */
	public MusicalForest(String[] args) throws Exception {
		parseArgs(args);
	}
	
	/**
	 * Main method for MusicalForest application. Just creates an instance of the main class
	 * and calls the requested action method.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			MusicalForest m = new MusicalForest(args);
			m.process();
		} catch (ForestMetaException e) {
			System.err.println("ERROR: " + e.getMessage());
			System.exit(ERROR_EXIT_CODE);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(ERROR_EXIT_CODE);
		}
	}

	/**
	 * Main processing method of the application. Called by main().
	 * 
	 * @throws Exception
	 */
	public void process() throws Exception {
		// Create profiler
		RuntimeMeasure m = new RuntimeMeasure(System.out);
		m.setSilent(silent);
		// Start action
		action.process(m);
	}
	
	/**
	 * CLI option parsing and checking. Creates the Action instance.
	 * 
	 * @param args
	 * @return
	 * @throws Exception 
	 */
	public void parseArgs(String[] args) throws Exception {
		if (args.length > 0) {
			String a = args[0];
			
			if (a.equals("train")) {
				train(removeFirst(args));
			} else if (a.equals("classify")) {
				classify(removeFirst(args));
			} else if (a.equals("update")) {
				update(removeFirst(args));
			} else if (a.equals("test")) {
				test(removeFirst(args));
			} else if (a.equals("generatedata")) {
				generateData(removeFirst(args));
			} else if (a.equals("generatemidi")) {
				generateMidi(removeFirst(args));
			} else if (a.equals("mergeforests")) {
				mergeForests(removeFirst(args));
			} else if (a.equals("spectrum")) {
				spectrum(removeFirst(args));
			} else if (a.equals("view")) {
				view(removeFirst(args));
			} else if (a.equals("modify")) {
				modify(removeFirst(args));
			} else {
				printHelp(args, args[0]);
				System.exit(ARGS_ERROR_EXIT_CODE);
			}
		} else {
			printHelp(args, null);
			System.exit(ARGS_ERROR_EXIT_CODE);
		}
	}

	/**
	 * 
	 * @param args
	 * @throws IOException
	 */
	private void spectrum(String[] args) throws IOException {
		OptionParser parser = new OptionParser() {
			{
				accepts("help", "Shows this help screen.").forHelp();
				accepts("settings", "XML file containing the CQT transform parameters.").withRequiredArg().required();
				accepts("audio", "Wave audio file to transform.").withRequiredArg().required();
				accepts("midi", "Optional MIDI file. If specified, the MIDI will be overlayed over the spectrum " +
						"to visualize synchronization.").withRequiredArg();
				accepts("scale", "Optional: Parameter for LogScaling. If omitted, no scaling happens.").withRequiredArg();
			}
		};
		OptionSet options = getOptions(args, parser);
		String settingsFile = (String)options.valueOf("settings");
		String audioFile = (String)options.valueOf("audio");
		String midiFile = (String)options.valueOf("midi");
		double scaleParam = -1;
		if (options.has("scale")) scaleParam = Double.parseDouble((String)options.valueOf("scale"));
		action = new SpectrumAction(audioFile, settingsFile, midiFile, scaleParam);
	}

	/**
	 * 
	 * @param args
	 * @throws IOException
	 */
	private void view(String[] args) throws IOException {
		OptionParser parser = new OptionParser() {
			{
				accepts("help", "Shows this help screen.").forHelp();
				accepts("target", "Working folder to examine").withRequiredArg().required();
			}
		};
		OptionSet options = getOptions(args, parser);
		String workingFolder = (String)options.valueOf("target");
		action = new ViewAction(workingFolder);
	}

	/**
	 * 
	 * @param args
	 * @throws Exception 
	 */
	private void modify(String[] args) throws Exception {
		OptionParser parser = new OptionParser() {
			{
				accepts("help", "Shows this help screen.").forHelp();
				accepts("target", "Working folder to examine").withRequiredArg().required();
				accepts("field", "Field name").withRequiredArg().required();
				accepts("value", "New value for field").withRequiredArg().required();
				accepts("mode", "0: ForestMeta; 1: DataMeta; 2: ForestMeta.DataMeta").withRequiredArg().required();
			}
		};
		OptionSet options = getOptions(args, parser);
		String workingFolder = (String)options.valueOf("target");
		String field = (String)options.valueOf("field");
		String value = (String)options.valueOf("value");
		int mode = Integer.parseInt((String)options.valueOf("mode"));
		action = new ModifyAction(workingFolder, mode, field, value);
	}

	/**
	 * 
	 * @param removeArg
	 * @throws IOException 
	 */
	private void mergeForests(String[] args) throws IOException {
		OptionParser parser = new OptionParser() {
			{
				accepts("help", "Shows this help screen.").forHelp();
				accepts("target", "Working folder. The new (merged) forest data will go there.").withRequiredArg().required();
				accepts("source", "This has to be the folder that has to contain all forests to merge. Will be searched recursively.").withRequiredArg().required();
				accepts("force", "Ignore missing or invalid metadata in forests");
			}
		};
		OptionSet options = this.getOptions(args, parser);
		String dataFolder = (String)options.valueOf("source");
		String workingFolder = (String)options.valueOf("target");
		boolean force = options.has("force");
		action = new MergeForestsAction(workingFolder, dataFolder, force);
	}

	/**
	 * 
	 * @param removeArg
	 * @throws Exception 
	 */
	private void generateData(String[] args) throws Exception {
		OptionParser parser = new OptionParser() {
			{
				accepts("help", "Shows this help screen.").forHelp();
				accepts("settings", "This has to be filled with the transform settings xml file. This file will also be copied into the data folder for the later processing.").withRequiredArg().required();
				accepts("target", "This folder will be created and filled with the generated training data sets. " + 
						"If there are any datasets in the folder, nothing will be overwritten, but the new files will be postfixed " + 
						"with an index. This allows you to have files with identical names in the MIDI source folder.").withRequiredArg().required();
				accepts("source", "This folder has to provide the MIDI files to generate test " +
						"data from. The generation process is then done recursively to any subfolder " + 
						"of this argument. If there already are Wave files beneath the MIDI, these will be used instead of rendering. " +
						"This can be important because MIDI to Wave rendering only works with midi2mp3, a proprietary external tool for Mac OS X.").withRequiredArg().required();
				accepts("sf", "SoundFont file to use for rendering audio files from MIDI.").withRequiredArg();
				accepts("scale", "Optional: Parameter for LogScaling. If omitted, no scaling happens.").withRequiredArg();
				accepts("strippc", "Optional: Strip all bank select and program change events from the MIDI data before rendering.");
				accepts("stripctrl", "Optional: Strip all controller messages.");
				accepts("maxvelocity", "Optional: Maximize all velocities to 127.");
			}
		};
		OptionSet options = getOptions(args, parser);
		String targetFolder = (String)options.valueOf("target");
		String sourceFolder = (String)options.valueOf("source");
		String settingsFile = (String)options.valueOf("settings");
		String sf = (String)options.valueOf("sf");
		Boolean stripPC = options.has("strippc");
		Boolean stripControlMessages = options.has("stripctrl");
		Boolean maximizeVelocities = options.has("maxvelocity");
		
		double scaleParam = -1;
		if (options.has("scale")) scaleParam = Double.parseDouble((String)options.valueOf("scale"));
		action = new GenerateDataAction(sourceFolder, targetFolder, sf, settingsFile, stripPC, stripControlMessages, maximizeVelocities, scaleParam);
	}

	/**
	 * 
	 * @param removeArg
	 * @throws Exception 
	 */
	private void generateMidi(String[] args) throws Exception {
		OptionParser parser = new OptionParser() {
			{
				accepts("help", "Shows this help screen.").forHelp();
				accepts("target", "Folder to put the generated MIDI files.").withRequiredArg().required();
				accepts("numfiles", "Number of files to create.").withRequiredArg().required();
				accepts("length", "Length of the created files in minutes.").withRequiredArg().required();
				accepts("sequential", "Without this option, note blocks are randomly spread over the file." + 
						             "Use this flag to generate sequences of random 'chords' without overlapping, " +
						             "or completely monophonic files.");
				accepts("bpm", "MIDI tempo in beats per minute.").withRequiredArg().required();
				accepts("maxnote", "Highest note limit (MIDI note number).").withRequiredArg().required();
				accepts("minnote", "Lowest note limit (MIDI note number).").withRequiredArg().required();
				accepts("maxduration", "Maximum note duration.").withRequiredArg().required();
				accepts("minduration", "Minimum note duration.").withRequiredArg().required();
				accepts("maxvoices", "Maximum number of tones per chord.").withRequiredArg().required();
				accepts("nps", "Mandatory in non-sequential mode: Notes per second to generate.").withRequiredArg();
				accepts("maxpause", "Optional in sequential mode: Maximum silence between chords in milliseconds.").withRequiredArg();
				accepts("minpause", "Optional in sequential mode: Minimum silence between chords in milliseconds. " +
									"Can be negative to produce overlapping, has to be lower than maxpause " +
									"(absolute) to avoid endless loops.").withRequiredArg();
				accepts("musical", "Optional: When creating groups, follow the note interval distributions examined " +
						           "by Youngblood (1958) and Hutchinson (1983), adapted from Carol Krumhansl: " +
						           "Cognitive Foundations of Musical Pitch, Oxford Psychology Series, 2001.");
				accepts("prefix", "File name prefix to use.").withRequiredArg().required();
			}
		};
		OptionSet options = getOptions(args, parser);
		String targetFolder = (String)options.valueOf("target");
		String filePrefix = (String)options.valueOf("prefix");
		int numOfFiles = Integer.parseInt((String)options.valueOf("numfiles"));
		boolean sequential = options.has("sequential");
		boolean musical = options.has("musical");
		int lengthMinutes = Integer.parseInt((String)options.valueOf("length"));
		int bpm = Integer.parseInt((String)options.valueOf("bpm"));
		int maxNote = Integer.parseInt((String)options.valueOf("maxnote"));
		int minNote = Integer.parseInt((String)options.valueOf("minnote"));
		int maxDuration = Integer.parseInt((String)options.valueOf("maxduration"));
		int minDuration = Integer.parseInt((String)options.valueOf("minduration"));
		int voices = Integer.parseInt((String)options.valueOf("maxvoices"));
		
		if (!sequential && !options.has("nps")) {
			System.out.println("ERROR: No note per second value is set in sequential mode, add option -nps");
			System.exit(ARGS_ERROR_EXIT_CODE);
		}
		double nps = 0;
		if (!sequential) nps = Double.parseDouble((String)options.valueOf("nps"));
		
		int maxPauseBetween = 0;
		if (sequential) {
			maxPauseBetween = Integer.parseInt((String)options.valueOf("maxpause"));
		}
		int minPauseBetween = 0;
		if (sequential) {
			minPauseBetween = Integer.parseInt((String)options.valueOf("minpause"));
		}
		
		action = new GenerateMidiAction(targetFolder, filePrefix, numOfFiles, sequential, musical, lengthMinutes, bpm, maxNote, minNote, nps, voices, minPauseBetween, maxPauseBetween, maxDuration, minDuration);
	}

	/**
	 * 
	 * @param args
	 * @throws IOException 
	 */
	private void update(String[] args) throws IOException {
		OptionParser parser = new OptionParser() {
			{
				accepts("help", "Shows this help screen.").forHelp();
				accepts("target", "Working folder containing the forest data.").withRequiredArg().required();
				accepts("data", "Test/reference data folder.").withRequiredArg().required();
				accepts("threads", "Optional: Thread number used to process classification. (Accuracy testing will always run with one thread per dataset)").withRequiredArg();
			}
		};
		
		// Parse arguments
		OptionSet options = getOptions(args, parser);
		String dataFolder = (String)options.valueOf("data");
		String workingFolder = (String)options.valueOf("target");
		int threads = -1;
		if (options.has("threads")) threads = Integer.parseInt((String)options.valueOf("threads"));

		action = new UpdateAction(workingFolder, dataFolder, threads);
	}

	/**
	 * 
	 * @param args
	 * @throws IOException 
	 */
	private void test(String[] args) throws IOException {
		OptionParser parser = new OptionParser() {
			{
				accepts("help", "Shows this help screen.").forHelp();
				accepts("target", "Working folder containing the forest data.").withRequiredArg().required();
				accepts("data", "Test/reference data folder.").withRequiredArg().required();
				accepts("threads", "Optional: Thread number used to process classification. (Accuracy testing will always run with one thread per dataset)").withRequiredArg();
				accepts("force", "Ignore if the given test data has different meta specifications than the forest.");
			}
		};
		
		// Parse arguments
		OptionSet options = getOptions(args, parser);
		String dataFolder = (String)options.valueOf("data");
		String workingFolder = (String)options.valueOf("target");
		int threads = -1;
		if (options.has("threads")) threads = Integer.parseInt((String)options.valueOf("threads"));
		boolean force = options.has("force");
		action = new TestAction(workingFolder, dataFolder, threads, force);
	}

	/**
	 * 
	 * @param args
	 * @throws IOException 
	 */
	private void classify(String[] args) throws IOException {
		OptionParser parser = new OptionParser() {
			{
				accepts("help", "Shows this help screen.").forHelp();
				accepts("target", "Forest folder. This has to contain the forest nodedata files and meta data").withRequiredArg().required();
				accepts("audio", "Audio file to process.").withRequiredArg().required();
				
				accepts("midi", "Optional: Can be filled with a reference MIDI file " + 
						"corresponding to the input audio file to test the accuracy of the classification and/or save an image.").withRequiredArg();
				accepts("image", "Optional: Will save an image with the visualized rendered data beneath the audio file.");
				accepts("onsensitivity", "Optional: Overrides the statistically determined optimal note onset sensitivity threshold (In classification mode).").withRequiredArg();
				accepts("offsensitivity", "Optional: Overrides the statistically determined optimal note offset sensitivity threshold (In classification mode).").withRequiredArg();
				accepts("threads", "Optional: Thread number used to perform the worker threading (in training mode: evaluation threading)").withRequiredArg();
				accepts("silent", "Optional: Dont output any messages.");
			}
		};
		OptionSet options = getOptions(args, parser);
		silent = options.has("silent");
		String classifyInput = (String)options.valueOf("audio");
		String workingFolder = (String)options.valueOf("target");
		String midiFile = (String)options.valueOf("midi");
		boolean image = options.has("image");
		double sensitivityOnset = -1;
		double sensitivityOffset = -1;
		if (options.has("onsensitivity")) sensitivityOnset = Double.parseDouble((String)options.valueOf("onsensitivity"));
		if (options.has("offsensitivity")) sensitivityOffset = Double.parseDouble((String)options.valueOf("offsensitivity"));
		int threads = -1;
		if (options.has("threads")) threads = Integer.parseInt((String)options.valueOf("threads"));
		
		action = new ClassifyAction(classifyInput, workingFolder, midiFile, sensitivityOnset, sensitivityOffset, threads, image);
	}

	/**
	 * 
	 * @param args
	 * @throws IOException 
	 */
	private void train(String[] args) throws IOException {
		OptionParser parser = new OptionParser() {
			{
				accepts("help", "Shows this help screen.").forHelp();
				accepts("target", "Working folder. Will be " + 
						"created and filled with the generated forest data, plus some stats and log files.").withRequiredArg().required();
				accepts("settings", "Forest settings stored in a XML file.").withRequiredArg().required();
				accepts("source", "Training data folder. This folder will be searched recursively, all sets of corresponding MIDI and CQT files " +
						"will be added for training. Also, " +
						"the \"frequencies\" file (which contains the frequencies of each bin in the spectrum) " +
						"must lie in this directory.").withRequiredArg().required();
				
				accepts("threads", "Maximum thread number used for evaluation threading inside nodes.").withRequiredArg();
				accepts("nodethreads", "Maximum number of threads for per-node threading.)").withRequiredArg();
				accepts("nodethreshold", "Amount of data values below which node threading should start. " +
						"Use this in combination with evaluation threading to limit node threading to small nodes " +
						"and, in parallel, continue to evaluate the next big node.").withRequiredArg();
			}
		};
		OptionSet options = getOptions(args, parser);
		String workingFolder = (String)options.valueOf("target");
		String settingsFile = (String)options.valueOf("settings");
		String dataFolder = (String)options.valueOf("source");
		
		action = new TrainingAction(workingFolder, settingsFile, dataFolder);
		
		// Threading params
		int evalThreads = -1;
		if (options.has("threads")) evalThreads = Integer.parseInt((String)options.valueOf("threads"));
		int nodeThreads = -1;
		if (options.has("nodethreads")) nodeThreads = Integer.parseInt((String)options.valueOf("nodethreads")); 
		int nodeThreadingThreshold = -1;
		if (options.has("nodethreshold")) nodeThreadingThreshold = Integer.parseInt((String)options.valueOf("nodethreshold"));
		((TrainingAction)action).setThreadingParams(evalThreads, nodeThreads, nodeThreadingThreshold);
	}

	/**
	 * Parse options.
	 * 
	 * @param args
	 * @param parser
	 * @return
	 */
	private OptionSet getOptions(String[] args, OptionParser parser) {
		// Parse arguments
		OptionSet options = null;
		try {
			options = parser.parse(args);
			if (options.has("help")) {
				parser.printHelpOn(System.out);
				System.exit(ARGS_ERROR_EXIT_CODE);
			}
		} catch (OptionException e) {
			System.out.println(e.getMessage() + "; Try java -jar <jarfile> <action> -help");
			System.exit(ARGS_ERROR_EXIT_CODE);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(ERROR_EXIT_CODE);
		}
		return options;
	}

	/**
	 * Removes one argument from an args array.
	 * 
	 * @param args
	 * @param arg
	 * @return
	 */
	public String[] removeFirst(String[] args) {
		String[] ret = new String[args.length-1];
		for(int j=1; j<args.length; j++) {
			ret[j-1] = args[j];
		}
		return ret;
	}
	
	/**
	 * Prints help on general usage.
	 */
	private void printHelp(String[] args, String var) {
		if (var != null) System.out.println("Invalid action: " + var);
		System.out.println("");
		System.out.println("Usage: java -jar <jarfile> <action> ...<actionparams>...");
		System.out.println("");
		System.out.println("The following actions are available:");
		System.out.println("");
		System.out.println("    train:        Trains a new musical random forest upon a give set of training data.");
		System.out.println("                  The trained tree(s) will be saved in the given working folder (see details).");
		System.out.println("                  After training, use update to calculate optimal thresholds for the forest.");
		System.out.println("");
		System.out.println("    update:       Detect optimal thresholds for a forest and save them along with new ");
		System.out.println("                  generated meta data and accuracy tests for the forest tree files. "); 
//		System.out.println("                  Will be automatically done by the train action, too, but has to be ");
//		System.out.println("                  used after mergeforests and after doing any other changes to the tree ");
//		System.out.println("                  files.");
		System.out.println("");
		System.out.println("    classify:     Classifies an audio file with a loaded forest. Optionally");
		System.out.println("                  tests accuracy against that single file and/or creates an image file");
		System.out.println("                  to visualize the results along with the MIDI reference.");
		System.out.println("");
		System.out.println("    test:         Perform accuracy tests against a give set of training/test data ");
		System.out.println("                  (generated by the generatedata action).");
		System.out.println("");
		System.out.println("    mergeforests: Merge the trees of multiple forests into one new forest.");
		System.out.println("                  After that, you have to perform the update task to find the best");
		System.out.println("                  MeanShift thresholds, do new accuracy tests and update meta data for");
		System.out.println("                  classification.");
		System.out.println("");
		System.out.println("    generatedata: Batch generate training data sets from MIDI and/or Wave Audio files.");
		System.out.println("                  Generates the spectral (.cqt) files from each audio file, these will");
		System.out.println("                  be used by the training action to grow the forest. Additionally,");
		System.out.println("                  an image for checking sync with MIDI is saved for each dataset file.");
		System.out.println("");
		System.out.println("    generatemidi: Generate random MIDI files.");
		System.out.println("");
		System.out.println("    view:         Display all available meta data of a forest or training dataset.");
		System.out.println("");
		System.out.println("    spectrum:     CQT transformation of an audio file into a PNG image, optionally ");
		System.out.println("                  overlayed with a MIDI file for checking synchronisation.");
		System.out.println("");
		System.out.println("Type 'java -jar <jarfile> <action> -help' to see details about each actions parameters.");
		System.out.println("");
	}
}
