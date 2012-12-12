package de.lmu.dbs.ciaa.classifier.musicalforest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.lmu.dbs.ciaa.classifier.core.Dataset;
import de.lmu.dbs.ciaa.classifier.core2d.TreeDataset2d;
import de.lmu.dbs.ciaa.midi.MIDIAdapter;
import de.lmu.dbs.ciaa.util.ArrayUtils;
import de.lmu.dbs.ciaa.util.FileIO;

/**
 * Represents one dataset for training/testing. Contains references to 
 * corresponding MIDI and spectrum files of the sample.
 * 
 * @author Thomas Weber
 *
 */
public class OnsetMusicalTreeDataset extends TreeDataset2d {

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
	 * Create new dataset instance.
	 * 
	 * @param spectrumFile
	 * @param midiFile
	 * @param frequencies
	 * @param step the frame width in audio samples 
	 * @throws Exception
	 */
	public OnsetMusicalTreeDataset(final File spectrumFile, final File midiFile, final double[] frequencies, final int step) throws Exception {
		super(spectrumFile, midiFile);
		this.frequencies = frequencies;
		this.step = step;
	}

	/**
	 * Generates Datasets for training/testing.
	 * 
	 * @param spectrumFiles list of data files
	 * @param dataPostfix file extension of data files
	 * @param referenceFolder folder containing the corresponding referenceI files
	 * @param referencePostfix file extension for reference files
	 * @return
	 * @throws Exception
	 */
	public static List<OnsetMusicalTreeDataset> loadDatasets(final String dataFolder, final String dataPostfix, final String referenceFolder, final String referencePostfix, final double[] frequencies, final int step) throws Exception {
		List<OnsetMusicalTreeDataset> ret = new ArrayList<OnsetMusicalTreeDataset>();
		List<File> spectrumFiles = getDirList(dataFolder, dataPostfix);
		for(int i=0; i<spectrumFiles.size(); i++) {
			String midiFilename = referenceFolder + File.separator + spectrumFiles.get(i).getName().replace(dataPostfix, referencePostfix); 
			File midiFile = new File(midiFilename);
			ret.add(i, new OnsetMusicalTreeDataset(spectrumFiles.get(i), midiFile, frequencies, step));
		}
		return ret;
	}
	
	/**
	 * Loads the spectral and midi data into memory.
	 * 
	 * @throws Exception 
	 * 
	 */
	public synchronized void load() throws Exception {
		if (isLoaded()) return;
		// Spectrum
		data = dataIo.load(dataFile.getAbsolutePath()); 
		// MIDI
		MIDIAdapter ma = new MIDIAdapter(referenceFile);
		byte[][] dataC = (byte[][])data;
		long duration = (long)((double)((dataC.length+1) * step * 1000.0) / 44100); // TODO festwerte
		reference = ma.toDataArray(dataC.length, duration, frequencies);
		
		ArrayUtils.filterFirst((byte[][])reference);
		ArrayUtils.blur((byte[][])reference, 0);
		ArrayUtils.shiftRight((byte[][])reference, 3);

		// TMP 
		/*
		ArrayToImage img = new ArrayToImage(dataC.length, dataC[0].length, 1);
		img.add(dataC, Color.WHITE, null);
		img.add((byte[][])reference, Color.RED, null, 0);
		img.save(new File("testdataResults/forestremote_mi7/" + dataFile.getName() + ".png"));
		System.exit(0);
		// /TMP */
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
		Dataset ret = new OnsetMusicalTreeDataset(dataFile, referenceFile, frequencies, step);
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