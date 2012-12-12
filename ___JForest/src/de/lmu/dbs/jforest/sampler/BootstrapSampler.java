package de.lmu.dbs.jforest.sampler;

import java.util.ArrayList;
import java.util.List;

import de.lmu.dbs.jforest.core.Dataset;
import de.lmu.dbs.jforest.util.RandomUtils;

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
	 * <br><br>
	 * CAUTION: Do not use this on already sampled samplers. The
	 *          previous sampling will be ignored and a new sample
	 *          of all frames will happen.
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
			clone.excludeAll();
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
