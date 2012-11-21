package de.lmu.dbs.ciaa.classifier.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Core feature class.
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
	public abstract Feature getInstance(ForestParameters params);

	/**
	 * Override this to initialize static parts of the feature, optionally, 
	 * for loading from file.
	 * 
	 */
	public void initStatic() {
	}
	
	/**
	 * Returns num feature parameter instances, each randomly generated.
	 * 
	 * @param num
	 * @return
	 */
	public List<Object> getRandomFeatureSet(ForestParameters params) {
		List<Object> ret = new ArrayList<Object>();
		for(int i=0; i<params.numOfRandomFeatures; i++) {
			Feature n = getInstance(params);
			ret.add(n);
		}
		return ret;
	}

	/**
	 * Returns a visualization of all node features of the forest. For debugging use.
	 * 
	 * @param data the array to store results (additive)
	 */
	public abstract void visualize(Object data);

	/**
	 * Returns the maximum evaluate value
	 * 
	 * @return
	 * @throws Exception 
	 */
	public abstract float getMaxValue() throws Exception;
	
	/**
	 * Returns a randomly generated threshold candidate for the feature.
	 * 
	 * @return
	 * @throws Exception 
	 */
	public float[] getRandomThresholds(int num) throws Exception {
		float[] ret = new float[num];
		for(int i=0; i<num; i++) {
			ret[i] = (float)(Math.random() * getMaxValue());
		}
		return ret;
	}

	/**
	 * Override this to get access to the logging point in RandomTree.
	 * 
	 * @param pre
	 * @throws Exception 
	 *
	public void logAdditional(String pre, Logfile log) throws Exception {
	}
	//*/
}
