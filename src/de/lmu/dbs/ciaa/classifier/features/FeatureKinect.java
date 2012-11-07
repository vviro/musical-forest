package de.lmu.dbs.ciaa.classifier.features;

import java.util.ArrayList;
import java.util.List;

import de.lmu.dbs.ciaa.classifier.ForestParameters;
import de.lmu.dbs.ciaa.util.RandomUtils;

/**
 * Feature implementation with Kinect formula.
 * 
 * @author Thomas Weber
 *
 */
public class FeatureKinect extends Feature {

	private static final long serialVersionUID = 1L;
	
	public int uX;
	public int uY;
	public int vX;
	public int vY;

	/**
	 * Create feature with random feature parameters.
	 * 
	 */
	public FeatureKinect(final ForestParameters params) {
		this.uX = RandomUtils.randomInt(params.xMin, params.xMax);
		this.uY = RandomUtils.randomInt(params.yMin, params.yMax);
		this.vX = RandomUtils.randomInt(params.xMin, params.xMax);
		this.vY = RandomUtils.randomInt(params.yMin, params.yMax);
		this.threshold = RandomUtils.randomInt(params.thresholdMax);
	}
	
	/**
	 * Create feature with random threshold, all other parameters are derived from 
	 * another FeatureKinect instance.
	 * 
	 */
	public FeatureKinect(final FeatureKinect f, final int thresholdMax) {
		this.uX = f.uX;
		this.uY = f.uY;
		this.vX = f.vX;
		this.vY = f.vY;
		this.threshold = RandomUtils.randomInt(thresholdMax);
	}
	
	/**
	 * 
	 */
	public FeatureKinect() {
	}

	/**
	 * Returns num feature parameter instances, each randomly generated.
	 * 
	 * @param num
	 * @return
	 */
	public List<Feature> getRandomFeatureSet(final ForestParameters params) {
		List<Feature> ret = new ArrayList<Feature>();
		for(int i=0; i<params.numOfRandomFeatures; i++) {
			FeatureKinect n = new FeatureKinect(params);
			ret.add(n);
			for(int j=0; j<params.thresholdCandidatesPerFeature-1; j++) {
				ret.add(new FeatureKinect(n, params.thresholdMax));
			}
		}
		return ret;
		
	}
	
	/**
	 * Feature function called to classify tree nodes. 
	 * 
	 * @param data data sample
	 * @param x coordinate in data sample
	 * @param y coordinate in data sample
	 * @return
	 * @throws Exception 
	 */
	public int evaluate(final byte[][] data, final int x, final int y) throws Exception {
		if (data[x][y] <= 0) return 0; //Integer.MIN_VALUE;
		int iuX = x+uX/data[x][y];
		if (iuX < 0 || iuX >= data.length) return 0; //Integer.MIN_VALUE;
		int iuY = y+uY/data[x][y];
		if (iuY < 0 || iuY >= data[0].length) return 0; //Integer.MIN_VALUE;
		int ivX = x+vX/data[x][y];
		if (ivX < 0 || ivX >= data.length) return 0; //Integer.MIN_VALUE;
		int ivY = y+vY/data[x][y];
		if (ivY < 0 || ivY >= data[0].length) return 0; //Integer.MIN_VALUE;
		return data[iuX][iuY] - data[ivX][ivY];
	}
	
	/**
	 * Returns a visualization of all node features of the forest. For debugging use.
	 * 
	 * @param data the array to store results (additive)
	 */
	public void visualize(int[][] data) {
		int x = data.length/2 + uX;
		int y = uY;
		data[x][y]++;
		x = data.length/2 + vX;
		y = vY;
		data[x][y]++;
	}

}