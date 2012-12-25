package de.lmu.dbs.musicalforest.classifier.features;

import java.util.ArrayList;
import java.util.List;

import de.lmu.dbs.jforest.core.ForestParameters;
import de.lmu.dbs.jforest.core2d.Feature2d;
import de.lmu.dbs.jspectrum.util.RandomUtils;
import de.lmu.dbs.musicalforest.util.Harmonics;

/**
 * Feature implementation for music analysis.
 * Uses overtone structures. 
 * 
 * @author Thomas Weber
 *
 */
public class FeatureOnOff2 extends Feature2d {

	private static final long serialVersionUID = 1L;
	
	public int uX;
	public int vX;
	
	public float foreignHarmonicsUpWeight; // [0..1]
	public float foreignHarmonicsDnWeight; // [0..1]
	
	public boolean on;
	
	/**
	 * Create feature with random feature parameters.
	 * 
	 */
	public FeatureOnOff2(final ForestParameters params) {
		on = Math.random() > 0.5;
		
		uX = RandomUtils.randomInt(1, 20);
		vX = RandomUtils.randomInt(1, 20);
		
		foreignHarmonicsUpWeight = (float)Math.random();
		foreignHarmonicsDnWeight = 1.0f - foreignHarmonicsUpWeight;
	}
	
	/**
	 * 
	 */
	public FeatureOnOff2() {
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
			FeatureOnOff2 n = new FeatureOnOff2(params);
			ret.add(n);
		}
		return ret;
	}
	
	public float evaluate(final byte[][] data, final int x, final int y) throws Exception {
		if (on) {
			return evaluateOnset(data, x, y);
		} else {
			return evaluateOffset(data, x, y);
		}
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
	public float evaluateOnset(final byte[][] data, final int x, final int y) throws Exception {
		if (data[x][y] == 0) return -Float.MAX_VALUE;
		
		if (x-uX < 0) return -Float.MAX_VALUE;
		if (x+vX >= data.length) return -Float.MAX_VALUE;
		
		float diff = (data[x][y] - data[x-uX][y]);
		if (diff <= 0) return -Float.MAX_VALUE;
		
		float d2 = (float)diff * data[x][y] * data[x+vX][y];
		
		float harmOwn = 0;
		for(int j=0; j<Harmonics.harmonics.length; j++) {
			int ny = y + Harmonics.harmonics[j];
			if (ny >= data[0].length) break; 
			harmOwn+= (float)(data[x][ny]); 
		}
		//harmOwn *= d2; 
		
		float harmForeignUp = 0;
		for(int j=0; j<Harmonics.harmonics.length; j++) {
			for(int j2=j+1; j2<Harmonics.harmonics.length; j2++) {
				int ny = y + Harmonics.harmonics[j] - Harmonics.harmonics[j2];
				if (ny >= data[0].length) break; 
				if (ny < 0) break; 
				harmForeignUp+= (float)(data[x][ny]); 
			}
		}
		harmForeignUp *= foreignHarmonicsUpWeight; //d2 * foreignHarmonicsUpWeight;
		
		float harmForeignDn = 0;
		for(int j=0; j<Harmonics.harmonics.length; j++) {
			int ny = y - Harmonics.harmonics[j];
			if (ny < 0) break; 
			harmForeignDn+= (float)(data[x][ny]); 
		}
		harmForeignDn *= foreignHarmonicsDnWeight; //d2 * foreignHarmonicsDnWeight;
		
		return d2 * (harmOwn - harmForeignUp - harmForeignDn); 
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
	public float evaluateOffset(final byte[][] data, final int x, final int y) throws Exception {
		//if (data[x][y] == 0) return -Float.MAX_VALUE;
		
		if (x-uX < 0) return -Float.MAX_VALUE;
		if (x+vX >= data.length) return -Float.MAX_VALUE;
		
		float diff = (data[x][y] - data[x+vX][y]);
		if (diff <= 0) return -Float.MAX_VALUE;
		
		float d2 = (float)diff * data[x][y] * data[x-uX][y];
		
		float harmOwn = 0;
		for(int j=0; j<Harmonics.harmonics.length; j++) {
			int ny = y + Harmonics.harmonics[j];
			if (ny >= data[0].length) break; 
			harmOwn+= (float)(data[x][ny]); 
		}
		//harmOwn *= d2; 
		
		float harmForeignUp = 0;
		for(int j=0; j<Harmonics.harmonics.length; j++) {
			for(int j2=j+1; j2<Harmonics.harmonics.length; j2++) {
				int ny = y + Harmonics.harmonics[j] - Harmonics.harmonics[j2];
				if (ny >= data[0].length) break; 
				if (ny < 0) break;
				harmForeignUp+= (float)(data[x][ny]); 
			}
		}
		harmForeignUp *= foreignHarmonicsUpWeight; //d2 * foreignHarmonicsUpWeight;
		
		float harmForeignDn = 0;
		for(int j=0; j<Harmonics.harmonics.length; j++) {
			int ny = y - Harmonics.harmonics[j];
			if (ny < 0) break; 
			harmForeignDn+= (float)(data[x][ny]);
		}
		harmForeignDn *= foreignHarmonicsDnWeight; //d2 * foreignHarmonicsDnWeight;
		
		return d2 * (harmOwn - harmForeignUp - harmForeignDn); 
	}

	/**
	 * Generates relative bin positions for the overtone harmonics.
	 * 
	 * @param amount number of overtones to be created
	 * @param binsPerOctave number of bins per octave in the spectral data
	 * @return
	 *
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
		String ret = "LR: Mode: " + (on ? "onset" : "offset") + ", uX: " + uX + ", vX: " + vX;
		//ret+= "; ownHarmonicsWeight: " + ownHarmonicsWeight; 
		ret+= "; foreignHarmonicsUpWeight: " + foreignHarmonicsUpWeight;
		ret+= "; foreignHarmonicsDnWeight: " + foreignHarmonicsDnWeight;
		return ret;
	}

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
	}

	@Override
	public Feature2d getInstance(ForestParameters params) {
		return new FeatureOnOff2(params);
	}
}