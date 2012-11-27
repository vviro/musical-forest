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
public class FeatureOnsetLR_2_7 extends Feature2d {

	private static final long serialVersionUID = 1L;
	
	//public float[] harmonicFactors = null;
	public int[] chosenHarmonics = null;
	public int[] chosenHarmonicsRev = null;
	
	public int numOfOvertones = 16; // TODO -> params
	public int numOfOvertonesRev = 16; // TODO -> params
	//public int maxNumOfOvertones = 16; // TODO -> params
	//public float harmonicAmplification = 1.0f; // TODO -> params
	
	//public float harmonicThreshold;
	
	/**
	 * Factors for calculation of overtones in log frequency spectra. 
	 * Generated with the method generateHarmonicFactors().
	 */
	private static int[] harmonics = null;
	
	public int uX;
	public int vX;
	
	//public float weighting;
	
	/**
	 * Create feature with random feature parameters.
	 * 
	 */
	public FeatureOnsetLR_2_7(final ForestParameters params) {
		initStatic();
		
		uX = RandomUtils.randomInt(1, 20);
		vX = RandomUtils.randomInt(1, 20);
		
		//weighting = (float)Math.random() - 0.5f;
		//harmonicThreshold = (float)Math.random() / 2 + 0.5f;
		//numOfOvertones = maxNumOfOvertones; //RandomUtils.randomInt(1, maxNumOfOvertones);
		//harmonicFactors = new float[numOfOvertones];
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
		for(int i=0; i<harms.length; i++) {
			chosenHarmonics[i] = (int)harms[i];
			//harmonicFactors[i] = (float)(Math.random()*harmonicAmplification); // * ((float)(harmonics.length-i)/harmonics.length);
		}
		
		chosenHarmonicsRev = new int[numOfOvertones];
		harms = new long[numOfOvertones];
		RandomSampler.sample(
				numOfOvertones, // n 
				harmonics.length, // N
				numOfOvertones, // count 
				0, // low 
				harms, 
				0, 
				null);
		for(int i=0; i<harms.length; i++) {
			chosenHarmonicsRev[i] = (int)harms[i];
			//harmonicFactors[i] = (float)(Math.random()*harmonicAmplification); // * ((float)(harmonics.length-i)/harmonics.length);
		}
	}
	
	/**
	 * 
	 */
	public FeatureOnsetLR_2_7() {
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
			FeatureOnsetLR_2_7 n = new FeatureOnsetLR_2_7(params);
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
		float diff = (data[x][y] - data[x-uX][y]);
		if (diff <= 0) return -Float.MAX_VALUE;
		float d2 = (float)diff * data[x][y] * data[x+vX][y];
		float ret = 0; //d2;
		for(int j=0; j<chosenHarmonics.length; j++) {
			int ny = y + harmonics[chosenHarmonics[j]];
			if (ny >= data[0].length) break; 
			ret+= d2 * (float)(data[x][ny]); 
		}
		for(int j=0; j<chosenHarmonicsRev.length; j++) {
			int ny = y - harmonics[chosenHarmonicsRev[j]];
			if (ny < 0) break; 
			ret-= d2 * (float)(data[x][ny]); 
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
		//float[] r = new float[amount];
		for(int i=0; i<amount; i++) {
			ret[i] = (int)(binsPerOctave * (Math.log((i+2)*2) / Math.log(2) - 1));
			//r[i] = (float)(Math.log((i+2)*2) / Math.log(2)) - 1;
		}
		//ArrayUtils.out(ret);
		//System.exit(0);
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
			//data[x][ny]+= harmonicFactors[j]; 
		}
	}

	/**
	 * 
	 */
	public String toString() {
		String ret = "LR: uX: " + uX + ", vX: " + vX + ", Harmonics (num: " + numOfOvertones + "): {";
		for(int i=0; i<chosenHarmonics.length; i++) {
			ret+= chosenHarmonics[i] + ", ";
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
		return new FeatureOnsetLR_2_7(params);
	}
}