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
public class MCFeature1_Static extends MCFeature {

	private static final long serialVersionUID = 1L;
	
	public int uX;
	public int uY;
	public int vX;
	public int vY;

	/**
	 * Create feature with random feature parameters.
	 * 
	 */
	public MCFeature1_Static(final ForestParameters params) {
		this.threshold = RandomUtils.randomInt(params.thresholdMax);
	}
	
	/**
	 * Create feature with random threshold, all other parameters are derived from 
	 * another FeatureKinect instance.
	 * 
	 */
	public MCFeature1_Static(final MCFeature1_Static f, final int thresholdMax) {
		this.threshold = RandomUtils.randomInt(thresholdMax);
	}

	/**
	 * 
	 */
	public MCFeature1_Static() {
	}
	
	/**
	 * Returns num feature parameter instances, each randomly generated.
	 * 
	 * @param num
	 * @return
	 */
	public List<MCFeature> getRandomFeatureSet(final ForestParameters params) {
		List<MCFeature> ret = new ArrayList<MCFeature>();
		for(int i=0; i<params.numOfRandomFeatures; i++) {
			MCFeature1_Static n = new MCFeature1_Static(params);
			ret.add(n);
			/*for(int j=0; j<params.thresholdCandidatesPerFeature-1; j++) {
				ret.add(new MCFeature1(n, params.thresholdMax));
			}*/
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
	public float evaluate(final byte[][] data, final int x, final int y) throws Exception {
		return data[x][y];
	}
	
	/**
	 * Returns a visualization of all node features of the forest. For debugging use.
	 * 
	 * @param data the array to store results (additive)
	 */
	public void visualize(int[][] data) {
		int x = data.length/2;
		int y = 0;
		data[x][y]++;
	}

	@Override
	public float getMaxValue() {
		return (float)(Byte.MAX_VALUE-1);
	}
}