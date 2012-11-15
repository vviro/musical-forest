package de.lmu.dbs.ciaa.classifier;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.lmu.dbs.ciaa.midi.MIDIAdapter;
import de.lmu.dbs.ciaa.util.FileIO;

/**
 * Represents one dataset for training/testing. Contains references to 
 * corresponding MIDI and spectrum files of the sample.
 * 
 * @author Thomas Weber
 *
 */
public class MusicalTreeDataset extends TreeDataset {

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
	public MusicalTreeDataset(final File spectrumFile, final File midiFile, final double[] frequencies, final int step) throws Exception {
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
	public static List<MusicalTreeDataset> loadDatasets(final String dataFolder, final String dataPostfix, final String referenceFolder, final String referencePostfix, final double[] frequencies, final int step) throws Exception {
		List<MusicalTreeDataset> ret = new ArrayList<MusicalTreeDataset>();
		List<File> spectrumFiles = getDirList(dataFolder, dataPostfix);
		for(int i=0; i<spectrumFiles.size(); i++) {
			String midiFilename = referenceFolder + File.separator + spectrumFiles.get(i).getName().replace(dataPostfix, referencePostfix); 
			File midiFile = new File(midiFilename);
			ret.add(i, new MusicalTreeDataset(spectrumFiles.get(i), midiFile, frequencies, step));
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
		long duration = (long)((double)((data.length+1) * step * 1000.0) / 44100); // TODO festwerte
		reference = ma.toDataArray(data.length, duration, frequencies);
		//ArrayUtils.blur(midi, 0);

		/*
		// TMP
		SpectrumToImage img = new SpectrumToImage(spectrum.length, spectrum[0].length, 1);
		img.add(spectrum, Color.WHITE, null);
		img.add(midi, Color.RED, null, 0);
		img.save(new File("testdataResults/forestremote/" + this.spectrumFile.getName() + ".png"));
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
		Dataset ret = new MusicalTreeDataset(dataFile, referenceFile, frequencies, step);
		if (getSamples().size() > 0) {
			List<Integer> cl = getSamplesClone();
			ret.includeSamples(cl);
		}
		return ret;
	}
	
	@Override
	public String toString() {
		return "Dataset: MIDI file " + referenceFile.getAbsolutePath() + "; Spectrum file: " + dataFile.getAbsolutePath();
	}
}
