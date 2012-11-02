package de.lmu.dbs.ciaa.classifier.features;

import java.util.ArrayList;
import java.util.List;

import de.lmu.dbs.ciaa.util.RandomUtils;

/**
 * Feature implementation with Kinect formula.
 * 
 * @author Thomas Weber
 *
 */
public class Feature2 extends Feature {

	private static final long serialVersionUID = 1L;
	
	public int uX;
	public int uY;
	public int vX;
	public int vY;

	/**
	 * Create feature with random feature parameters.
	 * 
	 */
	public Feature2(int xMin, int xMax, int yMin, int yMax, int thresholdMax) {
		this.uX = RandomUtils.randomInt(xMin, xMax);
		this.uY = RandomUtils.randomInt(yMin, yMax);
		this.vX = RandomUtils.randomInt(xMin, xMax);
		this.vY = RandomUtils.randomInt(yMin, yMax);
		this.threshold = RandomUtils.randomInt(thresholdMax);
	}
	
	public Feature2(Feature2 f, int thresholdMax) {
		this.uX = f.uX;
		this.uY = f.uY;
		this.vX = f.vX;
		this.vY = f.vY;
		this.threshold = RandomUtils.randomInt(thresholdMax);
	}
	
	public Feature2() {
	}
	
	/**
	 * Returns num feature parameter instances, each randomly generated.
	 * 
	 * @param num
	 * @return
	 */
	public static List<Feature> getRandomFeatureSet(int num, int xMin, int xMax, int yMin, int yMax, int thresholdMax, int thresholdCandidatesPerFeature) {
		List<Feature> ret = new ArrayList<Feature>();
		for(int i=0; i<num; i++) {
			Feature2 n = new Feature2(xMin, xMax, yMin, yMax, thresholdMax);
			ret.add(n);
			for(int j=0; j<thresholdCandidatesPerFeature-1; j++) {
				ret.add(new Feature2(n, thresholdMax));
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
	 * @param params feature parameters
	 * @return
	 * @throws Exception 
	 */
	public int evaluate(final byte[][] data, final int x, final int y) throws Exception {
		//if (x+uX < 0 || x+uX >= data.length) throw new Exception("u overflow");
		//if (x+vX < 0 || x+vX >= data.length) throw new Exception("v overflow");
		int ret = data[x][y];
		if (y+48 < data[x].length) ret+= data[x][y]*data[x][y+48];
		if (y+48*2 < data[x].length) ret+= data[x][y]*data[x][y+48*2];
		if (y+48*3 < data[x].length) ret+= data[x][y]*data[x][y+48*3];
		if (y+48*4 < data[x].length) ret+= data[x][y]*data[x][y+48*4];
		if (y+48*5 < data[x].length) ret+= data[x][y]*data[x][y+48*5];
		return ret;
		//return data[x][y] + data[x][y+uY] + data[x][y+uY*2] + data[x][y+uY*3] + data[x][y+uY*4] + data[x][y+uY*5];
	}
}