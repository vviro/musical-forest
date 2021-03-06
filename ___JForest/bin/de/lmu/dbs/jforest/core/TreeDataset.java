package de.lmu.dbs.jforest.core;

import java.io.File;

/**
 * Dataset base class for random trees.
 * 
 * @author Thomas Weber
 *
 */
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
	 * Loads the spectral and midi data into memory.
	 * 
	 * @throws Exception 
	 * 
	 */
	public abstract void load() throws Exception;
	
	/**
	 * Returns the initial classification array for this dataset.
	 * 
	 * @return
	 * @throws Exception
	 */
	public abstract Classification getInitialClassification() throws Exception;

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
	public abstract Classification getInitialClassification(final int valuesPerFrame) throws Exception;

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
	 * Returns the data object.
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
