package de.lmu.dbs.ciaa.classifier.features;

import java.util.ArrayList;
import java.util.List;

import de.lmu.dbs.ciaa.classifier.RandomTreeParameters;
import de.lmu.dbs.ciaa.util.RandomUtils;

/**
 * Feature implementation with Kinect formula.
 * 
 * @author Thomas Weber
 *
 */
public class FeatureKinect5 extends Feature {

	private static final long serialVersionUID = 1L;
	
	public int uX;
	public int uY;
	public int vX;
	public int vY;

	/**
	 * Create feature with random feature parameters.
	 * 
	 */
	public FeatureKinect5(int xMin, int xMax, int yMin, int yMax, int thresholdMax) {
		this.uX = RandomUtils.randomInt(xMin, xMax);
		this.uY = RandomUtils.randomInt(yMin, yMax);
		this.vX = RandomUtils.randomInt(xMin, xMax);
		this.vY = RandomUtils.randomInt(yMin, yMax);
		this.threshold = RandomUtils.randomInt(thresholdMax);
	}
	
	/**
	 * Create feature with random threshold, all other parameters are derived from 
	 * another FeatureKinect instance.
	 * 
	 */
	public FeatureKinect5(FeatureKinect5 f, int thresholdMax) {
		this.uX = f.uX;
		this.uY = f.uY;
		this.vX = f.vX;
		this.vY = f.vY;
		this.threshold = RandomUtils.randomInt(thresholdMax);
	}

	/**
	 * Returns num feature parameter instances, each randomly generated.
	 * 
	 * @param num
	 * @return
	 */
	public static List<Feature> getRandomFeatureSet(RandomTreeParameters params) {
		List<Feature> ret = new ArrayList<Feature>();
		for(int i=0; i<params.numOfRandomFeatures; i++) {
			FeatureKinect5 n = new FeatureKinect5(params.xMin, params.xMax, params.yMin, params.yMax, params.thresholdMax);
			ret.add(n);
			for(int j=0; j<params.thresholdCandidatesPerFeature-1; j++) {
				ret.add(new FeatureKinect5(n, params.thresholdMax));
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
		//if (data[x][y] <= 0) return 0; //Integer.MIN_VALUE;
		int iuX = x+uX; ///data[x][y];
		if (iuX < 0 || iuX >= data.length) return 0; //Integer.MIN_VALUE;
		int iuY = y+uY; ///data[x][y];
		if (iuY < 0 || iuY >= data[0].length) return 0; //Integer.MIN_VALUE;
		int ivX = x+vX; ///data[x][y];
		if (ivX < 0 || ivX >= data.length) return 0; //Integer.MIN_VALUE;
		int ivY = y+vY; ///data[x][y];
		if (ivY < 0 || ivY >= data[0].length) return 0; //Integer.MIN_VALUE;
		return data[x][y] + data[iuX][iuY] + data[ivX][ivY];
	}
}