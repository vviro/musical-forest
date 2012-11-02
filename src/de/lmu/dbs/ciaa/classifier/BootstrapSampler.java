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
public class BootstrapSampler<T> extends Sampler<T> {

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
	 * but contains randomly chosen data sets (with replacement). 
	 * <br><br>
	 * The sets are represented by object references, so they should not be modified after sampling.
	 * 
	 * @return bootstrap sample
	 */
	@Override
	public Sampler<T> getSample() {
		int size = getPoolSize();
		List<T> ret = new ArrayList<T>();
		for(int i=0; i<size; i++) {
			ret.add(i, datasets.get(RandomUtils.randomInt(size-1)));
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
