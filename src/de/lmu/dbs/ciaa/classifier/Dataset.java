package de.lmu.dbs.ciaa.classifier;

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
	 * -1 means the sample is not in the sample, >= 0 the opposite.
	 */
	private int[] samples = null;

	/**
	 * Initialize the sample array. By default, all
	 * samples are included in the sample.
	 * 
	 * @throws Exception
	 */
	protected void init() throws Exception {
		samples = new int[getLength()];		
	}
	
	/**
	 * Returns the number of samples in the dataset.
	 * 
	 * @return
	 * @throws Exception 
	 */
	protected abstract int getLength() throws Exception;
	
	/**
	 * Returns the samples array, containing all indices which are in the current 
	 * selection. 
	 * 
	 * @return
	 * @throws Exception 
	 */
	protected int[] getSamples() throws Exception {
		if (samples == null) init();
		return samples;
	}
	
	/**
	 * Replaces the complete samples array.
	 * 
	 * @param s
	 */
	protected void replaceIncludedSamples(int[] s) {
		samples = s;
	}
	
	/**
	 * Includes one sample.
	 * 
	 * @param s
	 * @throws Exception 
	 */
	protected void includeSample(int s) throws Exception {
		if (samples == null) init();
		samples[s] = 0;
	}
	
	/**
	 * Excludes one sample.
	 * 
	 * @param s
	 * @throws Exception 
	 */
	protected void excludeSample(int s) throws Exception {
		if (samples == null) init();
		samples[s] = -1;
	}
	
	/**
	 * Exclude all samples.
	 * 
	 * @throws Exception 
	 */
	protected void excludeAll() throws Exception {
		if (samples == null) init();
		for(int i=0; i<samples.length; i++) {
			samples[i] = -1;
		}
	}
	
	/**
	 * Include all samples.
	 * 
	 * @throws Exception 
	 */
	protected void includeAll() throws Exception {
		samples = new int[getLength()];
	}

	/**
	 * Returns if a sample is in the sample collection.
	 * 
	 * @param index
	 * @return
	 * @throws Exception 
	 */
	protected boolean isSampled(int index) throws Exception {
		if (samples == null) init();
		return samples[index] >= 0;
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
}
