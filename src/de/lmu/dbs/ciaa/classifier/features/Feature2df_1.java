package de.lmu.dbs.ciaa.classifier.features;

import java.util.ArrayList;
import java.util.List;

import de.lmu.dbs.ciaa.classifier.core.Feature;
import de.lmu.dbs.ciaa.classifier.core.ForestParameters;
import de.lmu.dbs.ciaa.classifier.core2df.Feature2df;

/**
 * 
 * @author Thomas Weber
 *
 */
public class Feature2df_1 extends Feature2df {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	private ForestParameters params;
	
	/**
	 * Create feature with random feature parameters.
	 * 
	 */
	public Feature2df_1(final ForestParameters params) {
		this.params = params;
	}
	
	/**
	 * 
	 */
	public Feature2df_1() {
	}
	
	/**
	 * 
	 */
	@Override
	public float evaluate(byte[][] data, int x) throws Exception {
		for(int i=0; i<data[x].length; i++) {
			if (data[x][i] > 0) return 1;
		}
		return 0;
	}

	/**
	 * 
	 */
	@Override
	public float getMaxValue() {
		return 1;
	}

	/**
	 * 
	 */
	@Override
	public void visualize(Object data) {
		//data[][];
	}

	@Override
	public Feature getInstance(ForestParameters params) {
		return new Feature2df_1(params);
	}

}
