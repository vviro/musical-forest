package de.lmu.dbs.ciaa.classifier.core2d;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;

import cern.jet.random.sampling.RandomSampler;
import de.lmu.dbs.ciaa.classifier.Dataset;
import de.lmu.dbs.ciaa.util.PostfixFilenameFilter;

/**
 * Abstract dataset class for usage in trained trees.
 * 
 * @author Thomas Weber
 *
 */
public abstract class TreeDataset2d extends Dataset {

	/**
	 * Spectrum file
	 */
	protected File dataFile;
	
	/**
	 * Corresponding MIDI file
	 */
	protected File referenceFile;
	
	/**
	 * Data to classify 
	 */
	protected byte[][] data = null;
	
	/**
	 * Reference data, containing the correct classification
	 */
	protected byte[][] reference = null;
	
	/**
	 * Create new dataset instance.
	 * 
	 * @param spectrumFile
	 * @param midiFile
	 * @param frequencies
	 * @param step the frame width in audio samples 
	 * @throws Exception
	 */
	public TreeDataset2d(final File dataFile, final File referenceFile) throws Exception {
		if (!dataFile.exists() || !dataFile.isFile()) {
			throw new Exception("ERROR: " + dataFile.getAbsolutePath() + " does not exist or is no file");
		}
		if (!referenceFile.exists() || !referenceFile.isFile()) {
			throw new Exception("ERROR: " + referenceFile.getAbsolutePath() + " does not exist or is no file");
		}
		this.dataFile = dataFile;
		this.referenceFile = referenceFile;
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
	 * Loads the spectral and midi data into memory.
	 * 
	 * @throws Exception 
	 * 
	 */
	public abstract void load() throws Exception;
	
	/**
	 * Determines if the dataset has been loaded.
	 * 
	 * @return
	 */
	public abstract boolean isLoaded();

	/**
	 * Returns the number of samples in the dataset.
	 * 
	 * @return
	 * @throws Exception 
	 */
	public int getLength() throws Exception {
		if (!isLoaded()) load();
		return data.length;
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
	 */
	public synchronized byte[][][] divideSpectrum(final int frames) throws Exception {
		if (!isLoaded()) load();
		int chunks = data.length/frames;
		byte[][][] ret = new byte[chunks][][];
		for(int i=0; i<chunks; i++) {
			ret[i] = getSpectrum(i*frames, frames);
		}
		return ret;
	}
	
	/**
	 * Returns the data array.
	 * 
	 * @return
	 * @throws Exception
	 */
	public synchronized byte[][] getData() throws Exception {
		if (!isLoaded()) load();
		return data;
	}

	/**
	 * Returns the reference data.
	 *  
	 * @return
	 * @throws Exception
	 */
	public synchronized byte[][] getReference() throws Exception {
		if (!isLoaded()) load();
		return reference;
	}
	
	/**
	 * Returns the initial classification array for this dataset.
	 * 
	 * @return
	 * @throws Exception
	 */
	public synchronized byte[][] getInitialClassification() throws Exception {
		if (!isLoaded()) load();
		byte[][] ret = new byte[data.length][data[0].length];
		for(int f=0; f<ret.length; f++) {
			if (isSampled(f)) {
				for(int g=0; g<ret[0].length; g++) {
					ret[f][g] = -1; // Throw all out of bag
				}
			}
		}
		return ret;
	}
	
	/**
	 * Returns the initial classification array for this dataset.
	 * <br><br>
	 * Selects valuesPerFrame randomly chosen pixels from the dataset.
	 * This is done by classifying the value to -1 (out of bag). 
	 * 
	 * @param valuesPerFrame
	 * @return
	 * @throws Exception 
	 */
	public synchronized byte[][] getInitialClassification(final int valuesPerFrame) throws Exception {
		if (!isLoaded()) load();
		if (valuesPerFrame >= data[0].length) {
			// All sampled frames should be in completely
			return getInitialClassification(); 
		}
		byte[][] ret = new byte[data.length][data[0].length];
		for(int i=0; i<data.length; i++) {
			for(int j=0; j<data[0].length; j++) {
				ret[i][j] = -1; // Throw all out of bag 
			}
		}
		// Get sample without replacement
		long[] array = new long[valuesPerFrame*data.length];
		RandomSampler.sample(
				valuesPerFrame*data.length, // n 
				data.length*data[0].length, // N
				valuesPerFrame*data.length, // count 
				0, // low 
				array, 
				0, 
				null);
		for(int i=0; i<array.length; i++) {
			int y = (int)Math.floor(array[i] / data.length);
			if (!isSampled(y)) continue;
			int x = (int)array[i] % data.length;
			ret[x][y] = 0; 
		}
		return ret;
	}

	public File getDataFile() {
		return dataFile;
	}

	public File getReferenceFile() {
		return referenceFile;
	}

	@Override
	public String toString() {
		return "Dataset: Reference file " + referenceFile.getAbsolutePath() + "; Data file: " + dataFile.getAbsolutePath();
	}

}
