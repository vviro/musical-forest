package de.lmu.dbs.jforest.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Core feature class. Extend this to create the specific feature function (see evaluate())
 * for the needs of your forest.
 * 
 * @author Thomas Weber
 *
 */
public abstract class Feature implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Threshold to compare to the feature functions output value.
	 */
	public double threshold;
	
	/**
	 * Factory method.
	 * 
	 * @param params
	 * @return
	 */
	public abstract Feature getInstance(ForestParameters params) throws Exception;

	/**
	 * Returns num feature parameter instances, each randomly generated.
	 * 
	 * @param num
	 * @return
	 */
	public List<Object> getRandomFeatureSet(ForestParameters params) throws Exception {
		List<Object> ret = new ArrayList<Object>();
		for(int i=0; i<params.numOfRandomFeatures; i++) {
			Feature n = getInstance(params);
			ret.add(n);
		}
		return ret;
	}

	/**
	 * Returns a randomly generated threshold candidate for the feature.
	 * 
	 * @return
	 * @throws Exception 
	 */
	public float[] getRandomThresholds(int num) throws Exception {
		float[] ret = new float[num];
		for(int i=0; i<num; i++) {
			ret[i] = (float)(Math.random() - 0.5) * Float.MAX_VALUE;
		}
		return ret;
	}
}
