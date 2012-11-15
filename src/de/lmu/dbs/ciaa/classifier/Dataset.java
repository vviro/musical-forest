package de.lmu.dbs.ciaa.classifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Dataset base class. Supports some sampling basics.
 * 
 * @author Thomas Weber
 *
 */
public abstract class Dataset {

	/**
	 * Indices of sampled frames for this dataset. Size 0 means all samples are in.
	 */
	private List<Integer> samples = new ArrayList<Integer>();
	
	/**
	 * Returns the number of samples in the dataset.
	 * 
	 * @return
	 * @throws Exception 
	 */
	protected abstract int getLength() throws Exception;
	
	/**
	 * Returns the samples array, containing all indices which are in the current 
	 * selection. Size 0 means all samples are in.
	 * 
	 * @return
	 */
	protected List<Integer> getSamples() {
		return samples;
	}
	
	/**
	 * Sets the samples array.
	 * 
	 * @param s
	 */
	protected void includeSamples(List<Integer> s) {
		samples = s;
	}
	
	/**
	 * Includes one sample.
	 * 
	 * @param s
	 * @throws Exception 
	 */
	protected void includeSample(int s) throws Exception {
		samples.add(s);
	}
	
	/**
	 * Returns if a sample is in the sample collection.
	 * 
	 * @param index
	 * @return
	 */
	protected boolean isSampled(int index) {
		if (samples.size() == 0) return true;
		for(int z=0; z<samples.size(); z++) {
			if (samples.get(z).intValue() == index) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns a clone of the dataset list.
	 * 
	 * @return
	 * @throws Exception 
	 */
	public List<Integer> getSamplesClone() throws Exception {
		List<Integer> ret = new ArrayList<Integer>();
		for(int i=0; i<samples.size(); i++) {
			ret.add(new Integer(samples.get(i).intValue()));
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
