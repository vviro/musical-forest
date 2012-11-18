package de.lmu.dbs.ciaa.classifier.core2d;

import java.util.List;

import de.lmu.dbs.ciaa.classifier.core.Feature;
import de.lmu.dbs.ciaa.classifier.core.ForestParameters;
import de.lmu.dbs.ciaa.util.RandomUtils;

public abstract class FeatureComposite2d extends Feature2d {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Feature2d feature;
	private List<Feature2d> factories;
	
	public abstract List<Feature2d> getFactories();
	
	public FeatureComposite2d() {
		factories = getFactories();
	}
	
	@Override
	public float evaluate(byte[][] data, int x, int y) throws Exception {
		return feature.evaluate(data, x, y);
	}

	@Override
	public float getMaxValue() {
		return feature.getMaxValue();
	}

	@Override
	public void visualize(Object data) {
	}

	@Override
	public Feature getInstance(ForestParameters params) {
		int i = RandomUtils.randomInt(factories.size()-1);
		return factories.get(i).getInstance(params);
	}

}
