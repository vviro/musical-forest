package de.lmu.dbs.musicalforest.classifier;

import java.awt.Color;
import java.io.File;
import java.util.List;

import de.lmu.dbs.musicalforest.Action;
import de.lmu.dbs.musicalforest.actions.GenerateDataAction;
import de.lmu.dbs.musicalforest.midi.MIDIAdapter;
import de.lmu.dbs.jforest.core.Dataset;
import de.lmu.dbs.jforest.core2d.TreeDataset2d;
import de.lmu.dbs.jspectrum.util.ArrayToImage;
import de.lmu.dbs.jspectrum.util.ArrayUtils;
import de.lmu.dbs.jspectrum.util.FileIO;

/**
 * Represents one dataset for training/testing. Contains references to 
 * corresponding MIDI and spectrum files of the sample.
 * 
 * @author Thomas Weber
 *
 */
public class OnOffMusicalTreeDataset extends TreeDataset2d {

	/**
	 * Debugging option. See load() method. If true, the first loaded dataset is saved as 
	 * PNG to check MIDI sync issues, then exits the program with code 0.
	 */
	private boolean testSync = false;
	
	/**
	 * IO handler for serialized byte[][] object files
	 */
	private static FileIO<byte[][]> dataIo = new FileIO<byte[][]>();

	/**
	 * Indicates if the spectrum and midi data has been loaded
	 */
	private boolean loaded = false;
	
	/**
	 * Frequencies of the data bins
	 */
	private double[] frequencies;
	
	/**
	 * The frame width in audio samples
	 */
	private int step;
	
	/**
	 * Dont blur reference arrays
	 */
	private boolean noBlur;
	
	/**
	 * Create new dataset instance.
	 * 
	 * @param spectrumFile
	 * @param midiFile
	 * @param frequencies
	 * @param step the frame width in audio samples 
	 * @throws Exception
	 */
	public OnOffMusicalTreeDataset(final File spectrumFile, final File midiFile, final double[] frequencies, final int step) throws Exception {
		this(spectrumFile, midiFile, frequencies, step, false);
	}

	/**
	 * Create new dataset instance.
	 * 
	 * @param spectrumFile
	 * @param midiFile
	 * @param frequencies
	 * @param step the frame width in audio samples 
	 * @throws Exception
	 */
	public OnOffMusicalTreeDataset(final File spectrumFile, final File midiFile, final double[] frequencies, final int step, boolean noBlur) throws Exception {
		super(spectrumFile, midiFile);
		this.frequencies = frequencies;
		this.step = step;
		this.noBlur = noBlur;
	}

	/**
	 * Generates new Datasets for training/testing.
	 * 
	 * param ret the List to add the datasets to
	 * @param spectrumFiles list of data files
	 * @param dataPostfix file extension of data files
	 * @param referenceFolder folder containing the corresponding referenceI files
	 * @param referencePostfix file extension for reference files
	 * @return
	 * @throws Exception
	 */
	public static void loadDatasets(List<Dataset> ret, final String dataFolder, final double[] frequencies, final int step) throws Exception {
		
	}
	/**
	 * Generates new Datasets for training/testing.
	 * 
	 * param ret the List to add the datasets to
	 * @param spectrumFiles list of data files
	 * @param dataPostfix file extension of data files
	 * @param referenceFolder folder containing the corresponding referenceI files
	 * @param referencePostfix file extension for reference files
	 * @return
	 * @throws Exception
	 */
	public static void loadDatasets(List<Dataset> ret, final String dataFolder, final double[] frequencies, final int step, boolean noBlur) throws Exception {
		File dir = new File(dataFolder);
		File[] files = dir.listFiles();
		for(int i=0; i<files.length; i++) {
			File f = files[i];
			if (f.getName() == "." || f.getName() == "..") continue;
			if (f.isFile()) {
				if (f.getName().endsWith(GenerateDataAction.FILE_SUFFIX_CQT)) {
					// Search for corresponding MIDI file
					File ref = new File(f.getAbsolutePath().replace(GenerateDataAction.FILE_SUFFIX_CQT, GenerateDataAction.FILE_SUFFIX_MIDI));
					if (ref.exists() && ref.isFile()) {
						ret.add(new OnOffMusicalTreeDataset(f, ref, frequencies, step, noBlur));
					}
				}
			}
			if (f.isDirectory()) {
				loadDatasets(ret, f.getAbsolutePath(), frequencies, step, noBlur);
			}
		}
	}
	
	/**
	 * Loads the spectral and midi data into memory.
	 *
	 * @throws Exception 
	 * 
	 */
	public synchronized void load() throws Exception {
		if (isLoaded()) return;
		
		// Load meta data
		String metafile = dataFile.getParent() + File.separator + Action.DATA_META_FILENAME;
		DataMeta meta = DataMeta.load(metafile);
		
		// Spectrum
		data = dataIo.load(dataFile.getAbsolutePath());
		
		// MIDI  
		MIDIAdapter ma = new MIDIAdapter(referenceFile);
		long duration = MIDIAdapter.calculateDuration(((byte[][])data).length, step, (double)meta.sampleRate);
		byte[][] midiOn = ma.toDataArray(((byte[][])data).length, duration, frequencies, true);
		ArrayUtils.shiftRight(midiOn, Action.DEFAULT_REFERENCE_SHIFT);
		byte[][] midiOff = ArrayUtils.clone(midiOn);
		
		ArrayUtils.filterLast(midiOff);
		if (!noBlur) ArrayUtils.blur(midiOff, 0);
		
		ArrayUtils.filterFirst(midiOn);
		if (!noBlur) ArrayUtils.blur(midiOn, 0);

		byte[][] ref = new byte[midiOn.length][midiOn[0].length];
		for(int x=0; x<midiOn.length; x++) {
			for(int y=0; y<midiOn[0].length; y++) {
				if (midiOn[x][y] > 0) ref[x][y] = 1;
				if (midiOff[x][y] > 0) ref[x][y] = 2;
			}
		}
		reference = ref;
		
		// Debugging option
		if (testSync) {
			ArrayToImage img = new ArrayToImage(midiOn.length, midiOn[0].length, 1);
			img.add((byte[][])data, Color.WHITE, null);
			img.add((byte[][])reference, Color.RED, null, 0);
			img.save(new File("SyncTest_" + dataFile.getName() + ".png"));
			System.exit(0);
		}
		
		loaded = true;
	}
	
	/**
	 * Determines if the dataset has been loaded.
	 * 
	 * @return
	 */
	public boolean isLoaded() {
		return loaded;
	}

	/**
	 * Returns a clone of the dataset.
	 * 
	 * @return
	 */
	@Override
	public Dataset getClone() throws Exception {
		Dataset ret = new OnOffMusicalTreeDataset(dataFile, referenceFile, frequencies, step);
		int[] cl = getSamplesClone();
		ret.replaceIncludedSamples(cl);
		return ret;
	}
	
	@Override
	public String toString() {
		return "Dataset: MIDI file " + referenceFile.getAbsolutePath() + "; Spectrum file: " + dataFile.getAbsolutePath();
	}

	/**
	 * Returns a part of the spectrum.
	 * 
	 * @param x starting frame
	 * @param frames length of the subspectrum
	 * @return
	 * @throws Exception
	 *
	public synchronized Object getSpectrum(final int x, final int frames) throws Exception {
		if (!isLoaded()) load();
		byte[][] ret = new byte[frames][];
		for (int i=0; i<frames; i++) {
			ret[i] = data[x+i];
		}
		return ret;
	}

	/**
	 * Splits up the spectrum and returns the results.
	 * 
	 * @param frames length of one chunk
	 * @return
	 * @throws Exception 
	 *
	public synchronized byte[][][] divideSpectrum(final int frames) throws Exception {
		if (!isLoaded()) load();
		int chunks = data.length/frames;
		byte[][][] ret = new byte[chunks][][];
		for(int i=0; i<chunks; i++) {
			ret[i] = getSpectrum(i*frames, frames);
		}
		return ret;
	}
	//*/

}
