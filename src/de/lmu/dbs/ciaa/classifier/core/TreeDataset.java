package de.lmu.dbs.ciaa.classifier.core;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;

import de.lmu.dbs.ciaa.util.PostfixFilenameFilter;

public abstract class TreeDataset extends Dataset {

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
	protected Object data = null;
	
	/**
	 * Reference data, containing the correct classification
	 */
	protected Object reference = null;
	
	/**
	 * Create new dataset instance.
	 * 
	 * @param spectrumFile
	 * @param midiFile
	 * @param frequencies
	 * @param step the frame width in audio samples 
	 * @throws Exception
	 */
	public TreeDataset(final File dataFile, final File referenceFile) throws Exception {
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
	public abstract int getLength() throws Exception;
	
	/**
	 * Returns the data array.
	 * 
	 * @return
	 * @throws Exception
	 */
	public synchronized Object getData() throws Exception {
		if (!isLoaded()) load();
		return data;
	}

	/**
	 * Returns the reference data.
	 *  
	 * @return
	 * @throws Exception
	 */
	public synchronized Object getReference() throws Exception {
		if (!isLoaded()) load();
		return reference;
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
