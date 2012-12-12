package de.lmu.dbs.jforest.core2d;

import java.util.List;

import de.lmu.dbs.jforest.core.Feature;
import de.lmu.dbs.jforest.util.RandomUtils;
import de.lmu.dbs.jforest.core.ForestParameters;

/**
 * Feature that can serve as a factory for randomly altered other features.
 * Every randomly generated feature instance will be of random type, selected
 * from the factories set with the factories List.
 * 
 * @author Thomas Weber
 *
 */
public abstract class FeatureComposite2d extends Feature2d {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private List<Feature2d> factories;
	
	public abstract List<Feature2d> getFactories();
	
	public FeatureComposite2d() {
		factories = getFactories();
	}
	
	@Override
	public float evaluate(byte[][] data, int x, int y) throws Exception {
		throw new Exception(this.getClass().getName() + ": This is just a factory feature, dont use it to classify.");
	}

	@Override
	public Feature getInstance(ForestParameters params) throws Exception {
		int i = RandomUtils.randomInt(factories.size()-1);
		return factories.get(i).getInstance(params);
	}
}
