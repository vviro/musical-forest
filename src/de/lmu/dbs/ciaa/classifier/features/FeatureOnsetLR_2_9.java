package de.lmu.dbs.ciaa.classifier.features;

import java.util.ArrayList;
import java.util.List;

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
public class FeatureOnsetLR_2_9 extends Feature2d {

	private static final long serialVersionUID = 1L;
	
	public static final int numOfOvertones = 20;
	
	/**
	 * Factors for calculation of overtones in log frequency spectra. 
	 * Generated with the method generateHarmonicFactors().
	 */
	private static int[] harmonics = null;
	
	public int uX;
	public int vX;
	
	//public float ownHarmonicsWeight; // [0..1]
	public float foreignHarmonicsWeight; // [0..1]
	
	/**
	 * Create feature with random feature parameters.
	 * 
	 */
	public FeatureOnsetLR_2_9(final ForestParameters params) {
		initStatic();
		
		uX = 3; //RandomUtils.randomInt(1, 20);
		vX = 14; //RandomUtils.randomInt(1, 20);
		
		//ownHarmonicsWeight = 1.0f; //(float)Math.random();
		foreignHarmonicsWeight = (float)Math.random();
	}
	
	/**
	 * 
	 */
	public FeatureOnsetLR_2_9() {
		initStatic();
	}

	public void initStatic() {
		if (harmonics == null) generateHarmonics(numOfOvertones, 48.0); // TODO festwert
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
			FeatureOnsetLR_2_9 n = new FeatureOnsetLR_2_9(params);
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
		
		float harmOwn = 0;
		for(int j=0; j<harmonics.length; j++) {
			int ny = y + harmonics[j];
			if (ny >= data[0].length) break; 
			harmOwn+= (float)(data[x][ny]); 
		}
		harmOwn *= d2; // * ownHarmonicsWeight;
		
		float harmForeign = 0;
		for(int j=0; j<harmonics.length; j++) {
			int ny = y - harmonics[j];
			if (ny < 0) break; 
			harmForeign+= (float)(data[x][ny]); 
		}
		harmForeign *= d2 * foreignHarmonicsWeight;
		
		return harmOwn - harmForeign; 
	}
	
	/**
	 * TODO Festwert
	 *
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
		for(int i=0; i<amount; i++) {
			ret[i] = (int)(binsPerOctave * (Math.log((i+2)*2) / Math.log(2) - 1));
		}
		harmonics = ret;
	}

	/**
	 * Returns a visualization of all node features of the forest. For debugging use.
	 * 
	 * @param data the array to store results (additive)
	 */
	public void visualize(Object data2) {
		/*
		int[][] data = (int[][])data2;
		int x = data.length/2;
		for(int j=0; j<chosenHarmonics.length; j++) {
			int i = chosenHarmonics[j];
			int ny = harmonics[i];
			if (ny > data[0].length) break;
			data[x][ny]+= harmonicFactors[j]; 
		}
		*/
	}

	/**
	 * 
	 */
	public String toString() {
		String ret = "LR: uX: " + uX + ", vX: " + vX;
		//ret+= "; ownHarmonicsWeight: " + ownHarmonicsWeight; 
		ret+= "; foreignHarmonicsWeight: " + foreignHarmonicsWeight;
		return ret;
	}

	/**
	 * Divide border for threshold resolution
	 *
	public float border = 3.0f;
	
	/**
	 * Percentage of thresholds taken from below border
	 *
	public float borderDiv = 0.2f;
	
	/**
	 * Returns a randomly generated threshold candidate for the feature.
	 * 
	 * @return
	 */
	@Override
	public float[] getRandomThresholds(int num) {
		float[] ret = new float[num];
		long max = 4000000; //(Byte.MAX_VALUE * Byte.MAX_VALUE * Byte.MAX_VALUE) * harmonics.length * 2;
		for(int i=0; i<num; i++) {
			ret[i] = (float)(Math.random() - 0.5) * max ;
		}
		return ret;
		/*
		float[] ret = new float[num];
		for(int i=0; i<(int)(num*borderDiv); i++) {
			ret[i] = (float)(Math.random() * border);
		}
		for(int i=(int)(num*borderDiv); i<num; i++) {
			ret[i] = (float)(border + Math.random() * getMaxValue());
		}
		return ret;
		*/
	}

	@Override
	public Feature2d getInstance(ForestParameters params) {
		return new FeatureOnsetLR_2_9(params);
	}
}