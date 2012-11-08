package de.lmu.dbs.ciaa.classifier.features;

import java.util.ArrayList;
import java.util.List;

import de.lmu.dbs.ciaa.classifier.ForestParameters;
import de.lmu.dbs.ciaa.util.RandomUtils;

/**
 * Feature implementation for music analysis.
 * Uses overtone structures. 
 * 
 * @author Thomas Weber
 *
 */
public class FeatureHarmonic3 extends Feature {

	private static final long serialVersionUID = 1L;
	
	public int uX;
	public int uY;
	public int vX;
	public int vY;

	/**
	 * Factors for calculation of overtones in log frequency spectra. 
	 * Can be generated with the method generateHarmonicFactors().
	 */
	private static final double[] harmonics = {1.0, 2.0, 2.584962500721156, 3.0, 3.3219280948873626, 3.5849625007211565, 3.8073549220576037, 4.0, 4.169925001442312, 4.321928094887363, 4.459431618637297, 4.584962500721157, 4.700439718141093, 4.807354922057604, 4.906890595608519, 5.0, 5.08746284125034, 5.169925001442312, 5.247927513443585}; // 20
	//private static final double[] harmonics = {1.0, 2.0, 2.584962500721156, 3.0, 3.3219280948873626, 3.5849625007211565, 3.8073549220576037, 4.0, 4.169925001442312, 4.321928094887363, 4.459431618637297, 4.584962500721157, 4.700439718141093, 4.807354922057604, 4.906890595608519, 5.0, 5.08746284125034, 5.169925001442312, 5.247927513443585, 5.321928094887363, 5.392317422778761, 5.459431618637297, 5.523561956057013, 5.584962500721156, 5.643856189774724, 5.700439718141093, 5.754887502163469, 5.807354922057605, 5.857980995127572, 5.906890595608519, 5.954196310386876, 6.0, 6.044394119358453, 6.08746284125034, 6.129283016944967, 6.169925001442312, 6.209453365628949, 6.247927513443586, 6.285402218862249, 6.321928094887362, 6.357552004618085, 6.39231742277876, 6.426264754702098, 6.459431618637298, 6.491853096329675, 6.523561956057013, 6.554588851677638, 6.584962500721156, 6.614709844115209}; // 50
	//private static final double[] harmonics = {1.0, 2.0, 2.584962500721156, 3.0, 3.3219280948873626, 3.5849625007211565, 3.8073549220576037, 4.0, 4.169925001442312}; // 10
	
	/**
	 * Create feature with random feature parameters.
	 * 
	 */
	public FeatureHarmonic3(final ForestParameters params) {
		if (params.xMin < params.xMax) {
			this.uX = RandomUtils.randomInt(params.xMin, params.xMax);
		} else {
			this.uX = 0;
		}
		if (params.xMin < params.xMax) {
			this.vX = RandomUtils.randomInt(params.xMin, params.xMax);
		} else {
			this.vX = 0;
		}
		this.uY = (int)(params.binsPerOctave * harmonics[RandomUtils.randomInt(harmonics.length-1)]);
		this.vY = (int)(params.binsPerOctave * harmonics[RandomUtils.randomInt(harmonics.length-1)]);
		this.threshold = RandomUtils.randomInt(getMaxValue());
		//generateHarmonicFactors(50);
	}
	
	/**
	 * Create feature with random threshold, all other parameters are derived from 
	 * another FeatureKinect instance.
	 * 
	 */
	public FeatureHarmonic3(final FeatureHarmonic3 f) {
		this.uX = f.uX;
		this.uY = f.uY;
		this.vX = f.vX;
		this.vY = f.vY;
		this.threshold = RandomUtils.randomInt(getMaxValue());
	}
	
	/**
	 * 
	 */
	public FeatureHarmonic3() {
	}

	/**
	 * Returns num feature parameter instances, each randomly generated.
	 * 
	 * @param num
	 * @return
	 */
	public List<Feature> getRandomFeatureSet(ForestParameters params) {
		List<Feature> ret = new ArrayList<Feature>();
		for(int i=0; i<params.numOfRandomFeatures; i++) {
			FeatureHarmonic3 n = new FeatureHarmonic3(params);
			ret.add(n);
			for(int j=0; j<params.thresholdCandidatesPerFeature-1; j++) {
				ret.add(new FeatureHarmonic3(n));
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
		// If out of range on time axis, the evaluation is useless, so return a minimal value
		int iuX = x+uX;
		if (iuX < 0 || iuX >= data.length) return Integer.MIN_VALUE;
		int ivX = x+vX;
		if (ivX < 0 || ivX >= data.length) return Integer.MIN_VALUE;

		boolean u = false;
		boolean v = false;
		int iuY = y+uY;
		if (iuY >= 0 && iuY < data[0].length) u = true; 
		int ivY = y+vY;
		if (ivY >= 0 && ivY < data[0].length) v = true; 
		
		if (u && v) return data[iuX][iuY] + data[ivX][ivY] + data[x][y]; 
		if (u) return data[iuX][iuY] + data[x][y];
		if (v) return data[ivX][ivY] + data[x][y];
		return Integer.MIN_VALUE; // All out of range
	}
	
	@Override
	public int getMaxValue() {
		return (Byte.MAX_VALUE-1) * 3;
	}

	/**
	 * Generates factors for the overtone harmonics and outputs the array
	 * to the console. Paste this output into the java code then.
	 * <br><br>
	 * This is not used in the regular program.
	 * 
	 * @param amount number of overtones to be created
	 * @return
	 */
	@SuppressWarnings("unused")
	private void generateHarmonicFactors(final int amount) {
		double[] ret = new double[amount];
		System.out.print("{");
		for(int i=1; i<amount; i++) {
			ret[i] = Math.log(i*2) / Math.log(2);
			System.out.print(ret[i]+", ");
		}
		System.out.println("}");
		System.exit(0);
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