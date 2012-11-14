package de.lmu.dbs.ciaa.classifier;

import java.awt.Color;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cern.jet.random.sampling.RandomSampler;

import de.lmu.dbs.ciaa.midi.MIDIAdapter;
import de.lmu.dbs.ciaa.util.FileIO;
import de.lmu.dbs.ciaa.util.PostfixFilenameFilter;
import de.lmu.dbs.ciaa.util.SpectrumToImage;

/**
 * Represents one dataset for training/testing. Contains references to 
 * corresponding MIDI and spectrum files of the sample.
 * 
 * @author Thomas Weber
 *
 */
public class Dataset {

	/**
	 * Spectrum file
	 */
	private File spectrumFile;
	
	/**
	 * Spectral data, loaded from spectrumFile
	 */
	private byte[][] spectrum = null;
	
	/**
	 * Left/Right classification of the spectrum values. One entry of the list is
	 * exclusively used by one recursion depth. Values inside the list elements:
	 * 0: root node; 1: left; 2: right; -1: out of bag
	 * 
	 *
	public List<byte[][]> classification;
	
	/**
	 * Corresponding MIDI file
	 */
	private File midiFile;
	
	/**
	 * MIDI data, derived from midiFile
	 */
	private byte[][] midi = null;
	
	/**
	 * IO handler for serialized byte[][] object files
	 */
	private static FileIO<byte[][]> spectrumIo = new FileIO<byte[][]>();

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
	public Dataset(final File spectrumFile, final File midiFile, final double[] frequencies, final int step) throws Exception {
		if (!spectrumFile.exists() || !spectrumFile.isFile()) {
			throw new Exception("ERROR: " + spectrumFile.getAbsolutePath() + " does not exist or is no file");
		}
		if (!midiFile.exists() || !midiFile.isFile()) {
			throw new Exception("ERROR: " + midiFile.getAbsolutePath() + " does not exist or is no file");
		}
		this.spectrumFile = spectrumFile;
		this.midiFile = midiFile;
		this.frequencies = frequencies;
		this.step = step;
		//this.classification = new ArrayList<byte[][]>();
	}

	/**
	 * Loads the spectral and midi data into memory.
	 * 
	 * @throws Exception 
	 * 
	 */
	public synchronized void load() throws Exception {
		if (loaded) return;
		// Spectrum
		spectrum = spectrumIo.load(spectrumFile.getAbsolutePath()); 
		// MIDI
		MIDIAdapter ma = new MIDIAdapter(midiFile);
		long duration = (long)((double)((spectrum.length+1) * step * 1000) / 44100); // TODO festwert
		midi = ma.toDataArray(spectrum.length, duration, frequencies);
		//ArrayUtils.blur(midi, 0);

		// TMP
		SpectrumToImage img = new SpectrumToImage(spectrum.length, spectrum[0].length, 1);
		img.add(spectrum, Color.WHITE, null);
		img.add(midi, Color.RED, null, 0);
		img.save(new File("testdataResults/forestremote/" + this.spectrumFile.getName() + ".png"));
		// /TMP
		
		loaded = true;
	}
	
	/**
	 * Returns the classification array for a given recursion depth.
	 * 
	 * @param depth
	 * @return
	 *
	public byte[][] getClassificationArray(final int depth) {
		if (classification.size() <= depth) {
			for(int i=0; i<=depth; i++) {
				byte[][] n = new byte[spectrum.length][frequencies.length];
				classification.add(n);
			}
		}
		return classification.get(depth);
	}
	
	/**
	 * Returns a part of the spectrum.
	 * 
	 * @param x starting frame
	 * @param frames length of the subspectrum
	 * @return
	 * @throws Exception
	 */
	public synchronized byte[][] getSpectrum(final int x, final int frames) throws Exception {
		if (!loaded) load();
		byte[][] ret = new byte[frames][];
		for (int i=0; i<frames; i++) {
			ret[i] = spectrum[x+i];
		}
		return ret;
	}

	/**
	 * Splits up the spectrum and returns the results.
	 * 
	 * @param frames length of one chunk
	 * @return
	 * @throws Exception 
	 */
	public synchronized byte[][][] divideSpectrum(final int frames) throws Exception {
		if (!loaded) load();
		int chunks = spectrum.length/frames;
		byte[][][] ret = new byte[chunks][][];
		for(int i=0; i<chunks; i++) {
			ret[i] = getSpectrum(i*frames, frames);
		}
		return ret;
	}
	
	/**
	 * Returns a list of all spectrum coordinates int arrays of length 2 (0:x, 1:y) 
	 * whose spectral values are higher than threshold.
	 * 
	 * @param threshold
	 * @return
	 * @throws Exception 
	 */
	public synchronized List<int[]> getRelevant(final int threshold, final double maxFreq) throws Exception {
		if (!loaded) load();
		List<int[]> ret = new ArrayList<int[]>();
		int maxBin = getFrequencyBin(maxFreq);
		if (maxBin < 0) maxBin = frequencies.length;
		for(int x=0; x<spectrum.length; x++) {
			for(int y=0; y<maxBin; y++) {
				if (spectrum[x][y] > threshold) {
					int[] d = {x,y};
					ret.add(d);
				}
			}
		}
		return ret;
	}
	
	/**
	 * Returns a list of all spectrum coordinates int arrays of length 2 (0:x, 1:y) 
	 * whose spectral values are higher than threshold. Only coordinates inside
	 * a specific frame window are regarded.
	 * 
	 * @param x starting frame
	 * @param frames length of the subspectrum
	 * @param threshold below all values are irrelevant
	 * @param maxFreq maximum frequency that is relevant
	 * @return
	 * @throws Exception 
	 */
	public synchronized List<int[]> getRelevant(final int x, final int frames, final int threshold, final double maxFreq) throws Exception {
		if (!loaded) load();
		List<int[]> ret = new ArrayList<int[]>();
		byte[][] chunk = getSpectrum(x, frames);
		int maxBin = getFrequencyBin(maxFreq);
		if (maxBin < 0) maxBin = frequencies.length;
		for(int i=0; i<chunk.length; i++) {
			for(int y=0; y<maxBin; y++) {
				if (chunk[i][y] >= threshold) {
					int[] d = {i,y};
					ret.add(d);
				}
			}
		}
		return ret;
	}
	
	/**
	 * Gets the bin of a given frequency.
	 * 
	 * @param freq
	 * @return
	 */
	public int getFrequencyBin(final double freq) {
		int f=0;
		while (freq > frequencies[f] && f < frequencies.length-1) {
			f++;
		}
		if (f == frequencies.length-1) {
			// See if the frequency is out of range above the highest frequency. See javadoc.
			return -1; 
		}
		return f-1;
	}

	/**
	 * Returns the spectrum file.
	 * 
	 * @return
	 */
	public File getSpectrumFile() {
		return spectrumFile;
	}
	
	/**
	 * Returns the spectral data.
	 * 
	 * @return
	 * @throws Exception
	 */
	public synchronized byte[][] getSpectrum() throws Exception {
		if (!loaded) load();
		return spectrum;
	}

	/**
	 * Returns the MIDI file.
	 * 
	 * @return
	 */
	public File getMidiFile() {
		return midiFile;
	}
	
	/**
	 * Returns the MIDI data.
	 *  
	 * @return
	 * @throws Exception
	 */
	/*public DoubleMatrix2D getMidi() throws Exception {
		if (!loaded) load();
		return midi;
	}*/
	
	public synchronized byte[][] getMidi() throws Exception {
		if (!loaded) load();
		return midi;
	}

	/**
	 * Selects valuesPerFrame randomly chosen pixels from the dataset.
	 * This is done by classifying the value to -1 (out of bag). Also resets the
	 * classification array.
	 * 
	 * @param depth recursion depth to pick the right classification array
	 * @param valuesPerFrame
	 * @params array here you have to deliver an array to work with. This 
	 *         can reduce memory footprint by reusing the array. Use getRandomValuesArray() to generate it.
	 * @return
	 * @throws Exception 
	 */
	public synchronized byte[][] selectRandomValues(final int depth, final int valuesPerFrame, long[] array) throws Exception {
		if (!loaded) load();
		// Throw all out of bag
		byte[][] ret = new byte[spectrum.length][frequencies.length];
		for(int i=0; i<spectrum.length; i++) {
			for(int j=0; j<frequencies.length; j++) {
				ret[i][j] = -1; 
			}
		}
		// Get sample without replacement
		//long[] sample = new long[valuesPerFrame*spectrum.length]; // This is a memory hot spot! We reuse the array for that reason...
		RandomSampler.sample(
				valuesPerFrame*spectrum.length, // n 
				spectrum.length*frequencies.length, // N
				valuesPerFrame*spectrum.length, // count 
				0, // low 
				array, 
				0, 
				null);
		for(int i=0; i<array.length; i++) {
			int x = (int)array[i] % spectrum.length;
			int y = (int)Math.floor(array[i] / spectrum.length);
			ret[x][y] = 0; 
		}
		return ret;
	}
	
	/**
	 * Returns a working array for selectRandomValues().
	 * 
	 * @return
	 * @throws Exception 
	 */
	public synchronized long[] getRandomValuesArray(final int valuesPerFrame) throws Exception {
		if (!loaded) load();
		return new long[valuesPerFrame*spectrum.length];
	}
	
	/**
	 * Generates Datasets for training/testing.
	 * 
	 * @param spectrumFiles list of spectrum files
	 * @param spectrumPostfix file extension of spectrum files
	 * @param midiFolder folder containing the corresponding MIDI files
	 * @param midiPostfix file extension for MIDI files
	 * @return
	 * @throws Exception
	 */
	public static List<Dataset> loadDatasets(final String spectrumFolder, final String spectrumPostfix, final String midiFolder, final String midiPostfix, final double[] frequencies, final int step) throws Exception {
		List<Dataset> ret = new ArrayList<Dataset>();
		List<File> spectrumFiles = getDirList(spectrumFolder, spectrumPostfix);
		for(int i=0; i<spectrumFiles.size(); i++) {
			String midiFilename = midiFolder + File.separator + spectrumFiles.get(i).getName().replace(spectrumPostfix, midiPostfix); 
			File midiFile = new File(midiFilename);
			ret.add(i, new Dataset(spectrumFiles.get(i), midiFile, frequencies, step));
		}
		return ret;
	}
	
	/**
	 * Returns a list of files from the folder, ending with postfix.
	 * 
	 * @param folder the name of a folder
	 * @param postfix file extension for example
	 * @return
	 */
	public static List<File> getDirList(final String folder, final String postfix) {
		File dir = new File(folder);
		FilenameFilter filter = new PostfixFilenameFilter(postfix);
		return Arrays.asList(dir.listFiles(filter));
	}
	
	/**
	 * Returns the ratio between notes and silence of the midi file.
	 * 
	 * @return
	 * @throws Exception 
	 */
	public synchronized double getRatio() throws Exception {
		if (!loaded) load();
		long notes = 0;
		long silence = 0;
		for(int x=0; x<midi.length; x++) {
			for(int y=0; y<midi[0].length; y++) {
				if (midi[x][y] > 0) {
					notes++;
				} else {
					silence++;
				}
			}
		}
		return (double)notes/silence;
	}
	
	@Override
	public String toString() {
		return "Dataset: MIDI file " + midiFile.getAbsolutePath() + "; Spectrum file: " + spectrumFile.getAbsolutePath();
	}
}
