package de.lmu.dbs.ciaa.classifier.features;

import java.util.ArrayList;
import java.util.List;

import de.lmu.dbs.ciaa.classifier.ForestParameters;

/**
 * Feature implementation for music analysis.
 * Uses overtone structures. 
 * 
 * @author Thomas Weber
 *
 */
public class FeatureHarmonic5 extends Feature {

	private static final long serialVersionUID = 1L;
	
	public double[] harmonicFactors = {5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
	
	/**
	 * Factors for calculation of overtones in log frequency spectra. 
	 * Can be generated with the method generateHarmonicFactors().
	 */
	private static final double[] harmonics = {1.0, 2.0 ,2.584962500721156, 3.0, 3.3219280948873626, 3.5849625007211565, 3.8073549220576037, 4.0, 4.169925001442312, 4.321928094887363, 4.459431618637297, 4.584962500721157, 4.700439718141093, 4.807354922057604, 4.906890595608519, 5.0, 5.08746284125034, 5.169925001442312, 5.247927513443585}; // 20
	
	/**
	 * Create feature with random feature parameters.
	 * 
	 */
	public FeatureHarmonic5(final ForestParameters params) {
		//harmonicFactors = new double[harmonics.length];
		for(int i=0; i<harmonics.length; i++) {
			//harmonicFactors[i] = (float)Math.random(); //*((float)(harmonics.length-i)/harmonics.length); //(float)Math.random() * (i/harmonics.length);
			//harmonicFactors[i] = (Math.random() > 0.7) ? (float)Math.random()*10 : 0; //*((float)(harmonics.length-i)/harmonics.length); //(float)Math.random() * (i/harmonics.length);
			//System.out.println(i + ": " + harmonicFactors[i]);
		}
		//this.threshold = Math.random() * getMaxValue();
		//generateHarmonicFactors(50);
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
			/*for(int j=0; j<params.thresholdCandidatesPerFeature-1; j++) {
				ret.add(new FeatureHarmonic5(n));
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
		double d2 = data[x][y]*data[x][y];
		double ret = 0;
		//if (true) return 0;
		for(int i=0; i<harmonics.length; i++) {//harmonics.length; i++) {
			int ny =  y + (int)(48.0*harmonics[i]); // TODO binsPerOctave
			if (ny >= data[0].length) return (int)ret;
			//if (data[x][ny] >= data[x][y]/10) {
			ret+= d2*data[x][ny]*harmonicFactors[i];
			//}
			//ret+= (data[x][ny] * harmonicFactors[i]) * data[x][y]; 
		}
		return (int)ret;
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
	@Override
	public float getMaxValue() {
		return 100000; //(float)((Byte.MAX_VALUE-1)*(Byte.MAX_VALUE-1));
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
	
	public String toString() {
		String ret = "{";
		for(int i=0; i<harmonicFactors.length; i++) {
			ret+= harmonicFactors[i] + ", ";
		}
		return ret + "}";
	}

}