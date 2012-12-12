package de.lmu.dbs.ciaa.classifier.features;

import cern.jet.random.sampling.RandomSampler;

import de.lmu.dbs.ciaa.classifier.core.ForestParameters;
import de.lmu.dbs.ciaa.classifier.core2d.Feature2d;
import de.lmu.dbs.ciaa.util.RandomUtils;

/**
 * Feature implementation for music analysis.
 * Uses overtone structures. 
 * 
 * @author Thomas Weber
 *
 */
public class FeatureHarmonic5_vari extends Feature2d {

	private static final long serialVersionUID = 1L;
	
	public float[] harmonicFactors = null;
	public int[] chosenHarmonics = null;
	
	public int numOfOvertones = 5; // TODO -> params
	public float harmonicAmplification = 10; // TODO -> params
	
	/**
	 * Factors for calculation of overtones in log frequency spectra. 
	 * Generated with the method generateHarmonicFactors().
	 */
	//private static final double[] harmonics = {1.0, 2.0 ,2.584962500721156, 3.0, 3.3219280948873626, 3.5849625007211565, 3.8073549220576037, 4.0, 4.169925001442312, 4.321928094887363, 4.459431618637297, 4.584962500721157, 4.700439718141093, 4.807354922057604, 4.906890595608519, 5.0, 5.08746284125034, 5.169925001442312, 5.247927513443585}; // 20
	private static int[] harmonics = null;
	
	/**
	 * Create feature with random feature parameters.
	 * 
	 */
	public FeatureHarmonic5_vari(final ForestParameters params) {
		if (harmonics == null) generateHarmonics(20, 48.0); // TODO festwert
		long[] harms = new long[numOfOvertones];
		
		RandomSampler.sample(
				numOfOvertones, // n 
				harmonics.length, // N
				numOfOvertones, // count 
				0, // low 
				harms, 
				0, 
				null);
		
		int amt = RandomUtils.randomInt(1, numOfOvertones);
		harmonicFactors = new float[amt];
		chosenHarmonics = new int[amt];
		for(int i=0; i<amt; i++) {
			chosenHarmonics[i] = (int)harms[i];
			harmonicFactors[i] = (float)(Math.random()*harmonicAmplification * ((float)(harmonics.length-i)/harmonics.length));
		}
		//this.threshold = Math.random() * getMaxValue();
	}
	
	/**
	 * Create feature with random threshold, all other parameters are derived from 
	 * another FeatureKinect instance.
	 * 
	 *
	public FeatureHarmonic5(final FeatureHarmonic5 f) {
		this.harmonicFactors = f.harmonicFactors;
		this.threshold = Math.random() * getMaxValue();
	}
	
	/**
	 * 
	 */
	public FeatureHarmonic5_vari() {
	}

	/**
	 * Feature function called to classify tree nodes. 
	 * 
	 * @param data data sample
	 * @param x coordinate in data sample
	 * @param y coordinate in data sample
	 * @return
	 * @throws Exception 
	 *
	public float evaluate(final byte[][] data, final int x, final int y) throws Exception {
		double ret = data[x][y]*data[x][y]; 
		//return (int)ret*100;
		for(int i=0; i<harmonics.length; i++) {//harmonics.length; i++) {
			int ny =  y + (int)(48.0*harmonics[i]); // TODO binsPerOctave
			if (ny >= data[0].length) return (int)(ret*100);
			ret+= (data[x][ny] * harmonicFactors[i]) * data[x][y]; 
		}
		return (int)(ret*100);
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
		if (data[x][y] == 0) return 0;
		float d2 = data[x][y]*data[x][y];
		float ret = 0;
		for(int j=0; j<chosenHarmonics.length; j++) {
			int ny =  y + harmonics[chosenHarmonics[j]];
			if (ny >= data[0].length) return ret;
			if (data[x][ny] > 0) {
				ret+= d2*data[x][ny]*harmonicFactors[j];
			}
		}
		return ret;
	}
	
	/**
	 * 
	 *
	public float evaluate(final byte[][] data, final int x, final int y) throws Exception {
		double d2 = (data[x][y] > 10) ? 1 : 0;
		if (d2 == 0) return 0;
		double ret = 0;
		for(int i=0; i<harmonics.length; i++) {//harmonics.length; i++) {
			int ny =  y + (int)(48.0*harmonics[i]); // TODO binsPerOctave
			if (ny >= data[0].length) return (int)(ret*100);
			if (data[x][ny] > 20) {
				ret+= d2; //*data[x][ny]; //*harmonicFactors[i];
			}
			//ret+= (data[x][ny] * harmonicFactors[i]) * data[x][y]; 
		}
		return (int)(ret*100);
	}
	
	/**
	 * 
	 *
	public float evaluate(final byte[][] data, final int x, final int y) throws Exception {
		double d2 = Math.log1p((double)data[x][y]);
		double ret = 0;
		for(int i=0; i<harmonics.length; i++) {//harmonics.length; i++) {
			int ny =  y + (int)(48.0*harmonics[i]); // TODO binsPerOctave
			if (ny >= data[0].length) return (int)(ret*100);
			//if (data[x][ny] >= data[x][y]/10) {
			ret+= d2*d2*data[x][ny]; //*harmonicFactors[i];
			//}
			//ret+= (data[x][ny] * harmonicFactors[i]) * data[x][y]; 
		}
		return (int)(ret*100);
	}

	/**
	 * TODO Festwert
	 */
	public float getMaxValue() {
		return 4000000; //(float)((Byte.MAX_VALUE-1)*(Byte.MAX_VALUE-1));
		//return (float)((Byte.MAX_VALUE-1)*(Byte.MAX_VALUE-1)*(Byte.MAX_VALUE-1) * (harmonics.length + 1) * 10);
	}

	/**
	 * Generates factors for the overtone harmonics and outputs the array
	 * to the console. Paste this output into the java code then.
	 * <br><br>
	 * This is not used in the regular program.
	 * 
	 * @param amount number of overtones to be created
	 * @return
	 *
	@SuppressWarnings("unused")
	private void generateHarmonicFactors_SysOut(final int amount) {
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
	
	public String toString() {
		String ret = "{";
		for(int i=0; i<harmonicFactors.length; i++) {
			ret+= chosenHarmonics[i] + ": " + harmonicFactors[i] + ", ";
		}
		return ret + "}";
	}

	/**
	 * Lambda parameter for exponential distribution of randomly 
	 * created threshold candidates
	 *
	public double lambda = 0.005;

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

	@Override
	public Feature2d getInstance(ForestParameters params) {
		return new FeatureHarmonic5_vari(params);
	}
}