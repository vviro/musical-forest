package de.lmu.dbs.ciaa.classifier.features;

import java.util.ArrayList;
import java.util.List;

import de.lmu.dbs.ciaa.classifier.RandomTreeParameters;
import de.lmu.dbs.ciaa.util.RandomUtils;

/**
 * Feature implementation for music analysis.
 * Uses overtone structures. 
 * 
 * @author Thomas Weber
 *
 */
public class FeatureHarmonic2 extends Feature {

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
	//private static final double[] harmonics = {1.0, 2.0, 2.584962500721156, 3.0, 3.3219280948873626, 3.5849625007211565, 3.8073549220576037, 4.0, 4.169925001442312}; // 10
	
	/**
	 * Create feature with random feature parameters.
	 * 
	 */
	public FeatureHarmonic2(final RandomTreeParameters params) {
		this.uX = RandomUtils.randomInt(params.xMin, params.xMax);
		this.uY = (int)(params.binsPerOctave * harmonics[RandomUtils.randomInt(harmonics.length-1)]);
		this.vX = RandomUtils.randomInt(params.xMin, params.xMax);
		this.vY = (int)(params.binsPerOctave * harmonics[RandomUtils.randomInt(harmonics.length-1)]);
		this.threshold = RandomUtils.randomInt(params.thresholdMax);
		//generateHarmonicFactors(20);
	}
	
	/**
	 * Create feature with random threshold, all other parameters are derived from 
	 * another FeatureKinect instance.
	 * 
	 */
	public FeatureHarmonic2(final FeatureHarmonic2 f, final int thresholdMax) {
		this.uX = f.uX;
		this.uY = f.uY;
		this.vX = f.vX;
		this.vY = f.vY;
		this.threshold = RandomUtils.randomInt(thresholdMax);
	}
	
	/**
	 * 
	 */
	public FeatureHarmonic2() {
	}

	/**
	 * Returns num feature parameter instances, each randomly generated.
	 * 
	 * @param num
	 * @return
	 */
	public List<Feature> getRandomFeatureSet(RandomTreeParameters params) {
		List<Feature> ret = new ArrayList<Feature>();
		for(int i=0; i<params.numOfRandomFeatures; i++) {
			FeatureHarmonic2 n = new FeatureHarmonic2(params);
			ret.add(n);
			for(int j=0; j<params.thresholdCandidatesPerFeature-1; j++) {
				ret.add(new FeatureHarmonic2(n, params.thresholdMax));
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
		
		if (u && v) return (data[iuX][iuY] + data[ivX][ivY]) * data[x][y]; 
		if (u) return data[iuX][iuY] * data[x][y];
		if (v) return data[ivX][ivY] * data[x][y];
		return Integer.MIN_VALUE;
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
}