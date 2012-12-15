package de.lmu.dbs.musicalforest.actions;

import java.awt.Color;
import java.io.File;

import midiReference.MidiReference;

import org.apache.commons.io.FileUtils;

import de.lmu.dbs.jspectrum.ConstantQTransform;
import de.lmu.dbs.jspectrum.Transform;
import de.lmu.dbs.jspectrum.TransformParameters;
import de.lmu.dbs.jspectrum.util.ArrayToImage;
import de.lmu.dbs.jspectrum.util.ArrayUtils;
import de.lmu.dbs.jspectrum.util.FileIO;
import de.lmu.dbs.jspectrum.util.HammingWindow;
import de.lmu.dbs.jspectrum.util.LogScale;
import de.lmu.dbs.jspectrum.util.RuntimeMeasure;
import de.lmu.dbs.jspectrum.util.Sample;
import de.lmu.dbs.jspectrum.util.WaveSample;
import de.lmu.dbs.jspectrum.util.Window;
import de.lmu.dbs.musicalforest.Action;
import de.lmu.dbs.musicalforest.classifier.DataMeta;
import de.lmu.dbs.musicalforest.midi.MIDIAdapter;
import de.lmu.dbs.musicalforest.midi.MIDIMessageHeaders;
import de.lmu.dbs.musicalforest.util.ExtTools;

/**
 * Generates data sets for training musical forests.
 * 
 * For generating WAV files from MIDI with SoundFonts, an external tool is used: 
 * http://www.audiosoftstore.com/midi-to-mp3-converter-terminal.html
 * 
 * @author Thomas Weber
 *
 */
public class GenerateDataAction extends Action {

	/**
	 * This folder will be created inside the data folder and filled with the audio and
	 * image files. This is done because the training algorithm actually doesnt need these,
	 * they are just stored for evaluation purposes.
	 */
	public static final String AUDIO_FOLDER_NAME = "Audio";
	
	/**
	 * Path to the MIDI to Wave converter (external) program.
	 */
	public static final String MIDI_2_WAV_CONVERTER = "midi2mp3/midi2mp3";
	
	/**
	 * Suffix for CQT spectrum files.
	 */
	public static final String FILE_SUFFIX_CQT = ".cqt";

	/**
	 * Suffix for CQT spectrum files.
	 */
	public static final String FILE_SUFFIX_MIDI = ".mid";

	/**
	 * Suffix for spectrum image files.
	 */
	public static final String FILE_SUFFIX_IMAGE = ".png";
	
	/**
	 * Suffix for wave audio files.
	 */
	public static final String FILE_SUFFIX_AUDIO = ".wav";

	/**
	 * MIDI source folder. All contained MIDI files (lower case extension .mid, .MID or others are ignored!)
	 * will be parsed and WAV files will be created (if not existent beneath the MIDI file), then, CQT transformations are generated
	 * to .cqt files beneath the MIDI files. Finally, PNG images are created to visualize
	 * the created dataset.
	 */
	public String sourceFolder = null;
	
	/**
	 * Path to the SF file that contains the sound font to use 
	 * for rendering WAV from MIDI.
	 */
	public String soundFont = null;
	
	/**
	 * Strip program change and bank select messages from the MIDI reference
	 */
	public boolean stripPC = false;
	
	/**
	 * Settings file for transformation.
	 */
	public String transformParamsFile = null;
	
	/**
	 * Tool for executing external commands
	 */
	private ExtTools extTools = new ExtTools(System.out);
	
	/**
	 * Transformation instance that does the CQT.
	 */
	private Transform transformation = null; 

	/**
	 * Transformation window function
	 */
	private Window transformationWindow;
	
	/**
	 * Scaling object used to scale the spectral data before saving.
	 */
	private LogScale scale = null;
	
	/**
	 * File IO for spectrum files.
	 */
	private FileIO<byte[][]> cqtIo = new FileIO<byte[][]>();

	private boolean stripControlMessages;
	
	private boolean maximizeVelocities;
	
	private TransformParameters params;
	
	/**
	 * 
	 * @param dataFolder
	 * @throws Exception 
	 */
	public GenerateDataAction(String midiFolder, String dataFolder, String soundFont, String transformParamsFile, boolean stripPC, boolean stripControlMessages, boolean maximizeVelocities, double scaleParam) throws Exception {
		this.dataFolder = dataFolder;
		this.sourceFolder = midiFolder;
		this.soundFont = soundFont;
		this.stripPC = stripPC;
		this.transformParamsFile = transformParamsFile;
		this.stripControlMessages = stripControlMessages;
		this.maximizeVelocities = maximizeVelocities;
		if (scaleParam > 0) {
			this.scale = new LogScale(scaleParam);
		}
	}

	/**
	 * Process action.
	 * 
	 */
	@Override
	public void process(RuntimeMeasure m) throws Exception {
		File df = new File(dataFolder);
		File mf = new File(sourceFolder);
		
		// Checks
		File tsf = new File(transformParamsFile);
		if (!tsf.exists()) {
			System.err.println("Transformation settings file does not exist: " + transformParamsFile);
			System.exit(12);
		}
		/*File sff = new File(soundFont);
		if (!sff.exists()) {
			System.err.println("Sound font file does not exist: " + soundFont);
			System.exit(11);
		}*/
		if (!mf.exists() || !mf.isDirectory()) {
			System.err.println("MIDI source folder " + sourceFolder + " does not exist or is not a directory.");
			System.exit(9);
		}
		if (df.isFile()) {
			System.err.println("Data folder " + dataFolder + " is a file, please remove it or specify another folder.");
			System.exit(10);
		}
		
		// Create data folder
		if (!df.exists()) {
			df.mkdirs();
		}
		
		// Copy settings file and parse it
		params = new TransformParameters();
		params.loadParameters(transformParamsFile);
		//FileUtils.copyFile(new File(transformParamsFile), new File(dataFolder + File.separator + TRANSFORM_SETTINGS_FILENAME));

		// Create folder for wave and image files
		File waveDir = new File(df.getAbsolutePath() + File.separator + AUDIO_FOLDER_NAME);
		
		// Get MIDI range
		MidiReference midiRef = MidiReference.getMidiReference();
		int minNote = midiRef.getNoteFromFrequency((float)params.fMin) + 1; 
		int maxNote = midiRef.getNoteFromFrequency((float)params.fMax) - 1;
		m.measure(" --> Min MIDI note: " + minNote, true);
		m.measure(" --> Max MIDI note: " + maxNote, true);

		// Process
		int num = generateDatasets(m, params, mf, df, waveDir, minNote, maxNote);
		
		m.setSilent(false);
		m.finalMessage("Finished generating " + num + " training datasets");
	}
	
	/**
	 * 
	 * @param sampleRate
	 * @return
	 * @throws Exception
	 */
	public Transform getTransformation(double sampleRate) throws Exception {
		if (transformation == null) {
			// Init transformation
			transformation = new ConstantQTransform(sampleRate, params.fMin, params.fMax, params.binsPerOctave, params.threshold, params.spread, params.divideFFT, params.cqtKernelBufferLocation);
			transformationWindow = new HammingWindow(transformation.getWindowSize());
			// Save meta file
			params.frequencies = transformation.getFrequencies();
			params.check();
			double scaleParam = (scale != null) ? scale.getWidth() : 0;
			DataMeta meta = new DataMeta(scaleParam, sampleRate, params);
			String filename = dataFolder + File.separator + DATA_META_FILENAME;
			meta.save(filename);
		}
		return transformation;
	}
	
	/**
	 * Travel the directory recursively and generate test datasets from each 
	 * MIDI file found on the way.
	 * 
	 */
	private int generateDatasets(RuntimeMeasure m, TransformParameters params, final File midiFolderFile, final File dataFolderFile, final File audioFolderFile, int minNote, int maxNote) throws Exception {
		File[] files = midiFolderFile.listFiles();
		int ret = 0;
		for(int i=0; i<files.length; i++) {
			File f = files[i];
			if (f.getName() == "." || f.getName() == "..") continue;
			if (f.isFile()) {
				if (generateDataset(m, params, f, dataFolderFile, audioFolderFile, minNote, maxNote)) {
					ret++;
				}
			}
			if (f.isDirectory()) {
				ret += generateDatasets(m, params, f, dataFolderFile, audioFolderFile, minNote, maxNote);
			}
		}
		return ret;
	}

	/**
	 * Generate a test set from one MIDI file.
	 * 
	 * @param m
	 * @param midiFileSrc File to generate from (MIDI)
	 * @param audioFolderFile folder to store evaluation visualizations
	 * @return true if rendering was successful
	 * @throws Exception 
	 */
	private boolean generateDataset(RuntimeMeasure m, TransformParameters params, File midiFileSrc, File dataFolderFile, File audioFolderFile, int minNote, int maxNote) throws Exception {
		if (!midiFileSrc.getName().endsWith(FILE_SUFFIX_MIDI)) return false;
		String basename = midiFileSrc.getName().replace(FILE_SUFFIX_MIDI, "");

		// Load MIDI file and save it to data folder
		File midiFile = new File(dataFolderFile.getAbsolutePath() + File.separator + basename + FILE_SUFFIX_MIDI);
		File wavRef = new File(sourceFolder + File.separator + basename + FILE_SUFFIX_AUDIO);
		
		int i=2;
		String bn2 = null;
		while (midiFile.exists()) {
			bn2 = basename + "_" + i;
			midiFile = new File(dataFolderFile.getAbsolutePath() + File.separator + bn2 + FILE_SUFFIX_MIDI);
			i++;
		}
		if (bn2 != null) {
			basename = bn2;
		}
		
		MIDIAdapter midiSrc = new MIDIAdapter(midiFileSrc);
		midiSrc.limitBandwidth(minNote, maxNote);
		
		if (!wavRef.exists()) {
			// Only alter MIDI data if the wav file will be rendered, too
			if (stripPC) {
				m.measure("Stripping program changes...", true);
				midiSrc.stripProgramChanges();
			}
			if (stripControlMessages) {
				m.measure("Stripping controller messages...", true);
				midiSrc.stripMessages(MIDIMessageHeaders.MESSAGE_CONTROLLER, true);
			}
			if (maximizeVelocities) {
				m.measure("Maximizing velocity...", true);
				midiSrc.maximizeVelocities(127);
			}
		}
		midiSrc.writeFile(midiFile);
		m.measure(" --> Copied MIDI reference to " + midiFile.getName());
		
		// Render WAV file with external tool (to audio folder)
		File wavFile = new File(audioFolderFile.getAbsolutePath() + File.separator + basename + FILE_SUFFIX_AUDIO);
		if (wavRef.exists()) {
			FileUtils.copyFile(wavRef, wavFile);
			m.measure(" --> Copied WAV file to " + wavFile.getName());
		} else {
			extTools.exec(MIDI_2_WAV_CONVERTER + " " + midiFile.getAbsolutePath() + " -o " + audioFolderFile.getAbsolutePath() + " -e wave -sf " + soundFont);
			File wavFileGen = new File(audioFolderFile.getAbsolutePath() + File.separator + midiFile.getName() + FILE_SUFFIX_AUDIO);
			FileUtils.moveFile(wavFileGen, wavFile);
			if (!wavFile.exists() || !wavFile.isFile()) throw new Exception("Error generating audio file: " + wavFile.getAbsolutePath());
			m.measure(" --> Rendered WAV file to " + wavFile.getName());
		}
		
		// Constant Q Transform
		Sample src = new WaveSample(wavFile);
		int[] mono = src.getMono();
		double[][] data = getTransformation((double)src.getSampleRate()).calculate(mono, params.step, transformationWindow);
		if (scale != null) {
			ArrayUtils.normalize(data);
			ArrayUtils.scale(data, scale);
			m.measure("Scaled CQT data by " + scale.toString(), true);
		}
		ArrayUtils.normalize(data, (double)Byte.MAX_VALUE);
		byte[][] byteData = ArrayUtils.toByteArray(data);
		File cqtFile = new File(dataFolderFile.getAbsolutePath() + File.separator + basename + FILE_SUFFIX_CQT);
		cqtIo.save(cqtFile.getAbsolutePath(), byteData);
		if (!cqtFile.exists() || !cqtFile.isFile()) throw new Exception("Error generating CQT file: " + cqtFile.getAbsolutePath());
		m.measure(" --> Saved CQT to file " + cqtFile.getName());
		
		// Save additional images containing the spectrum and MIDI visually for evaluation (to eval folder)
		long duration = MIDIAdapter.calculateDuration(data.length, params.step, (double)src.getSampleRate()); // Audio length in milliseconds
		byte[][] midiData = midiSrc.toDataArray(data.length, duration, params.frequencies, true);
		ArrayUtils.shiftRight(midiData, DEFAULT_REFERENCE_SHIFT);
		File imgFile = new File(audioFolderFile.getAbsolutePath() + File.separator + basename + FILE_SUFFIX_IMAGE);
		ArrayToImage img = new ArrayToImage(data.length, data[0].length, 1);
		img.add(byteData, Color.WHITE, null);
		img.add(midiData, Color.GREEN, null, 0);
		img.save(imgFile);
		if (!imgFile.exists() || !imgFile.isFile()) throw new Exception("Error generating visualization file: " + imgFile.getAbsolutePath());
		m.measure(" --> Saved image to " + imgFile.getName());
		
		m.measure(" -----> Generated Dataset from MIDI file " + midiFileSrc.getName(), true);
		return true;
	}

}
