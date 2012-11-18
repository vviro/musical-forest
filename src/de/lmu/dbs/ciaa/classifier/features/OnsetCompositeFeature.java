package de.lmu.dbs.ciaa.classifier.features;

import java.util.ArrayList;
import java.util.List;

import de.lmu.dbs.ciaa.classifier.core2d.Feature2d;
import de.lmu.dbs.ciaa.classifier.core2d.FeatureComposite2d;
import de.lmu.dbs.ciaa.classifier.features.*;

public class OnsetCompositeFeature extends FeatureComposite2d {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public List<Feature2d> getFactories() {
		List<Feature2d> ret = new ArrayList<Feature2d>();
		ret.add(new FeatureOnsetLR());
		ret.add(new FeatureOnsetBlur());
		return ret;
	}

}
