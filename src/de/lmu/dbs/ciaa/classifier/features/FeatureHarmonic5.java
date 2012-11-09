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
public class FeatureHarmonic5 extends Feature {

	private static final long serialVersionUID = 1L;
	
	public float[] harmonicFactors;
	
	/**
	 * Factors for calculation of overtones in log frequency spectra. 
	 * Can be generated with the method generateHarmonicFactors().
	 */
	private static final double[] harmonics = {2.584962500721156, 3.0, 3.3219280948873626, 3.5849625007211565, 3.8073549220576037, 4.0, 4.169925001442312, 4.321928094887363, 4.459431618637297, 4.584962500721157, 4.700439718141093, 4.807354922057604, 4.906890595608519, 5.0, 5.08746284125034, 5.169925001442312, 5.247927513443585}; // 20
	
	/**
	 * Create feature with random feature parameters.
	 * 
	 */
	public FeatureHarmonic5(final ForestParameters params) {
		harmonicFactors = new float[harmonics.length];
		for(int i=0; i<harmonics.length; i++) {
			harmonicFactors[i] = (float)Math.random() * (i/harmonics.length);
		}
		this.threshold = Math.random() * getMaxValue();
		//generateHarmonicFactors(50);
	}
	
	/**
	 * Create feature with random threshold, all other parameters are derived from 
	 * another FeatureKinect instance.
	 * 
	 */
	public FeatureHarmonic5(final FeatureHarmonic5 f) {
		this.harmonicFactors = f.harmonicFactors;
		this.threshold = Math.random() * getMaxValue();
	}
	
	/**
	 * 
	 */
	public FeatureHarmonic5() {
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
			FeatureHarmonic5 n = new FeatureHarmonic5(params);
			ret.add(n);
			for(int j=0; j<params.thresholdCandidatesPerFeature-1; j++) {
				ret.add(new FeatureHarmonic5(n));
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
	public float evaluate(final byte[][] data, final int x, final int y) throws Exception {
		double ret = data[x][y];
		for(int i=0; i<harmonics.length; i++) {
			int ny =  + (int)(48*harmonics[i]); // TODO binsPerOctave
			if (ny > data[0].length) return (int)ret;
			ret+= data[x][ny] * harmonicFactors[i]; 
		}
		return (int)(ret*100);
	}
	
	@Override
	public float getMaxValue() {
		return (float)((Byte.MAX_VALUE-1) * harmonics.length);
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
		int x = data.length/2;
		for(int i=0; i<harmonics.length; i++) {
			int ny =  + (int)(48*harmonics[i]);
			if (ny > data[0].length) break;
			data[x][ny]+= harmonicFactors[i]*100; // TODO binsPerOctave
		}
	}

}