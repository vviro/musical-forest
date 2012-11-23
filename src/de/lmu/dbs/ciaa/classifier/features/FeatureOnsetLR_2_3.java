package de.lmu.dbs.ciaa.classifier.features;

import java.util.ArrayList;
import java.util.List;

import cern.jet.random.sampling.RandomSampler;

import de.lmu.dbs.ciaa.classifier.core2d.Feature2d;
import de.lmu.dbs.ciaa.classifier.core.ForestParameters;
import de.lmu.dbs.ciaa.util.RandomUtils;

/**
 * Feature implementation for music analysis.
 * Uses overtone structures. 
 * 
 * @author Thomas Weber
 *
 */
public class FeatureOnsetLR_2_3 extends Feature2d {

	private static final long serialVersionUID = 1L;
	
	public float[] harmonicFactors = null;
	public int[] chosenHarmonics = null;
	
	public int numOfOvertones = 10; // TODO -> params
	public float harmonicAmplification = 1.0f; // TODO -> params
	
	/**
	 * Factors for calculation of overtones in log frequency spectra. 
	 * Generated with the method generateHarmonicFactors().
	 */
	//private static final double[] harmonics = {1.0, 2.0 ,2.584962500721156, 3.0, 3.3219280948873626, 3.5849625007211565, 3.8073549220576037, 4.0, 4.169925001442312, 4.321928094887363, 4.459431618637297, 4.584962500721157, 4.700439718141093, 4.807354922057604, 4.906890595608519, 5.0, 5.08746284125034, 5.169925001442312, 5.247927513443585}; // 20
	private static int[] harmonics = null;
	
	public int uX;
	public int vX;
	
	/**
	 * Create feature with random feature parameters.
	 * 
	 */
	public FeatureOnsetLR_2_3(final ForestParameters params) {
		initStatic();
		uX = RandomUtils.randomInt(1, 20);
		vX = RandomUtils.randomInt(1, 20);
		harmonicFactors = new float[numOfOvertones];
		chosenHarmonics = new int[numOfOvertones];
		long[] harms = new long[numOfOvertones];
		RandomSampler.sample(
				numOfOvertones, // n 
				harmonics.length, // N
				numOfOvertones, // count 
				0, // low 
				harms, 
				0, 
				null);
		//ArrayUtils.out(harms);
		for(int i=0; i<harms.length; i++) {
			chosenHarmonics[i] = (int)harms[i];
			harmonicFactors[i] = (float)(Math.random()*harmonicAmplification); // * ((float)(harmonics.length-i)/harmonics.length);
		}
		//this.threshold = Math.random() * getMaxValue();
	}
	
	/**
	 * 
	 */
	public FeatureOnsetLR_2_3() {
		initStatic();
	}

	public void initStatic() {
		if (harmonics == null) generateHarmonics(20, 48.0); // TODO festwert
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
			FeatureOnsetLR_2_3 n = new FeatureOnsetLR_2_3(params);
			ret.add(n);
		}
		return ret;
	}
	
	/**
	 * Feature function called to classify tree nodes.  -> feature5.png, quite good
	 * 
	 * @param data data sample
	 * @param x coordinate in data sample
	 * @param y coordinate in data sample
	 * @return
	 * @throws Exception 
	 */
	public float evaluate(final byte[][] data, final int x, final int y) throws Exception {
		if (data[x][y] == 0) return -Float.MAX_VALUE;
		if (x-uX < 0) return -Float.MAX_VALUE;
		if (x+vX >= data.length) return -Float.MAX_VALUE;
		float d2 = (float)(data[x][y] * (data[x][y] - data[x-uX][y]) * data[x+vX][y]); //data[x][y]; //*data[x][y];
		float ret = d2;
		//float ret = d2 - data[x-uX][y];
		for(int j=0; j<chosenHarmonics.length; j++) {
			int ny =  y + harmonics[chosenHarmonics[j]];
			if (ny >= data[0].length) return ret;
			ret+= d2 * data[x][ny] * harmonicFactors[j];
		}
		return ret;
	}
	
	/**
	 * TODO Festwert
	 */
	@Override
	public float getMaxValue() {
		return 400000; 
	}

	/**
	 * Generates relative bin positions for the overtone harmonics.
	 * 
	 * @param amount number of overtones to be created
	 * @param binsPerOctave number of bins per octave in the spectral data
	 * @return
	 */
	private void generateHarmonics(final int amount, final double binsPerOctave) {
		int[] ret = new int[amount];
		for(int i=1; i<amount; i++) {
			ret[i] = (int)(binsPerOctave * (Math.log(i*2) / Math.log(2)));
		}
		harmonics = ret;
	}

	/**
	 * Returns a visualization of all node features of the forest. For debugging use.
	 * 
	 * @param data the array to store results (additive)
	 */
	public void visualize(Object data2) {
		int[][] data = (int[][])data2;
		int x = data.length/2;
		for(int j=0; j<chosenHarmonics.length; j++) {
			int i = chosenHarmonics[j];
			int ny = harmonics[i];
			if (ny > data[0].length) break;
			data[x][ny]+= harmonicFactors[j]; 
		}
	}

	/**
	 * 
	 */
	public String toString() {
		String ret = "LR: uX: " + uX + ", vX: " + vX + ", Harmonics: {";
		for(int i=0; i<harmonicFactors.length; i++) {
			ret+= chosenHarmonics[i] + ": " + harmonicFactors[i] + ", ";
		}
		return ret + "}";
	}

	/**
	 * Divide border for threshold resolution
	 */
	public float border = 3.0f;
	
	/**
	 * Percentage of thresholds taken from below border
	 */
	public float borderDiv = 0.2f;
	
	/**
	 * Returns a randomly generated threshold candidate for the feature.
	 * 
	 * @return
	 */
	@Override
	public float[] getRandomThresholds(int num) {
		float[] ret = new float[num];
		for(int i=0; i<(int)(num*borderDiv); i++) {
			ret[i] = (float)(Math.random() * border);
		}
		for(int i=(int)(num*borderDiv); i<num; i++) {
			ret[i] = (float)(border + Math.random() * getMaxValue());
		}
		return ret;
	}
	//*/

	@Override
	public Feature2d getInstance(ForestParameters params) {
		return new FeatureOnsetLR_2_3(params);
	}
}