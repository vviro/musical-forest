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
	public float getMaxValue() throws Exception {
		throw new Exception(this.getClass().getName() + ": This is just a factory feature, dont use it to classify.");
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
