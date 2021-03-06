package de.lmu.dbs.jforest.core;

import de.lmu.dbs.jforest.util.FileIO;

/**
 * Dataset base class. Supports basic sampling of samples.
 * Samples can also be multi-dimensional, this just delivers the
 * interface for Samplers.
 * 
 * @author Thomas Weber
 *
 */
public abstract class Dataset {

	/**
	 * Represents the sampling state of all samples in the dataset.
	 * Represents the number of times the sample has been sampled.
	 */
	private int[] samples = null;

	/**
	 * Set a new sampling array, i.e. if loaded from buffer file
	 * 
	 * @param s new array
	 */
	public void setData(int[] s) {
		samples = s;
	}
	
	/**
	 * Initialize the sample array. By default, all
	 * samples are included in the sample.
	 * 
	 * @throws Exception
	 */
	private void init() throws Exception {
		samples = new int[getLength()];	
		includeAll();
	}
	
	/**
	 * Returns the number of samples in the dataset.
	 * 
	 * @return
	 * @throws Exception 
	 */
	public abstract int getLength() throws Exception;
	
	/**
	 * Returns the samples array, containing all indices which are in the current 
	 * selection. 
	 * 
	 * @return
	 * @throws Exception 
	 *
	public int[] getSamples() throws Exception {
		if (samples == null) init();
		return samples;
	}
	
	/**
	 * Replaces the complete samples array.
	 * 
	 * @param s
	 */
	public void replaceIncludedSamples(int[] s) {
		samples = s;
	}
	
	/**
	 * Includes one sample.
	 * 
	 * @param s
	 * @throws Exception 
	 */
	public void includeSample(int s) throws Exception {
		if (samples == null) init();
		samples[s]++; // = 0;
	}
	
	/**
	 * Excludes one sample.
	 * 
	 * @param s
	 * @throws Exception 
	 */
	public void excludeSample(int s) throws Exception {
		if (samples == null) init();
		samples[s] = 0;
	}
	
	/**
	 * Exclude all samples.
	 * 
	 * @throws Exception 
	 */
	public void excludeAll() throws Exception {
		samples = new int[getLength()];
	}
	
	/**
	 * Include all samples.
	 * 
	 * @throws Exception 
	 */
	public void includeAll() throws Exception {
		if (samples == null) init();
		for(int i=0; i<samples.length; i++) {
			includeSample(i);
		}

	}

	/**
	 * Returns if a sample is in the sample collection.
	 * 
	 * @param index
	 * @return
	 * @throws Exception 
	 */
	public boolean isSampled(int index) throws Exception {
		if (samples == null) init();
		return samples[index] > 0;
	}
	
	/**
	 * Returns how often the sample has been included.
	 * 
	 * @param index
	 * @return
	 * @throws Exception
	 */
	public int getSampled(int index) throws Exception {
		if (samples == null) init();
		return samples[index];
	}
	
	/**
	 * Returns a clone of the samples array.
	 * 
	 * @return
	 * @throws Exception 
	 */
	public int[] getSamplesClone() throws Exception {
		if (samples == null) init();
		int[] ret = new int[samples.length];
		for(int i=0; i<samples.length; i++) {
			ret[i] = samples[i];
		}
		return ret;
	}
	
	/**
	 * Returns a clone of the dataset.
	 * 
	 * @return
	 * @throws Exception 
	 */
	public abstract Dataset getClone() throws Exception;

	/**
	 * 
	 * @param filename
	 * @throws Exception 
	 */
	public void saveSampling(String filename) throws Exception {
		FileIO<int[]> io = new FileIO<int[]>();
		io.save(filename, this.samples);
	}
}
