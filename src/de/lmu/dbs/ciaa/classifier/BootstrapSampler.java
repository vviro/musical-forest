package de.lmu.dbs.ciaa.classifier;

import java.util.ArrayList;
import java.util.List;

import de.lmu.dbs.ciaa.util.RandomUtils;

/**
 * Generic Sampler that implements the statistical bootstrapping method.
 * 
 * @author Thomas Weber
 *
 */
public class BootstrapSampler<T extends Dataset> extends Sampler<T> {

	/**
	 * Creates a bootstrapping sampler instance.
	 * 
	 * @param datasets the data pool
	 */
	public BootstrapSampler(List<T> datasets) {
		super(datasets);
	}

	/**
	 * Returns a sampler, which has the same size as the data pool
	 * but contains randomly chosen datasets (with replacement). 
	 * 
	 * @return bootstrap sample
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	@Override
	public synchronized Sampler<T> getSample() throws Exception {
		// Clone datasets
		List<T> ret = new ArrayList<T>();
		int size = getPoolSize();
		for(int i=0; i<size; i++) {
			T clone = (T)datasets.get(i).getClone();
			// Sample frames in datasets
			int len = clone.getLength();
			for(int j=0; j<len; j++) {
				clone.includeSample(RandomUtils.randomInt(len-1));
			}
			ret.add(clone);
		}
		return new BootstrapSampler<T>(ret);
	}

	/**
	 * Returns a new BootstrapSampler.
	 * 
	 * @param data
	 * @return
	 */
	@Override
	public Sampler<T> newInstance(List<T> data) {
		return new BootstrapSampler<T>(data);
	}
}
