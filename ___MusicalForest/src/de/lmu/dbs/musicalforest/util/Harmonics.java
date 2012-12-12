package de.lmu.dbs.musicalforest.util;

/**
 * Static class to use for working with musical harmonics.
 * 
 * @author Thomas Weber
 *
 */
public class Harmonics {

	/**
	 * Factors for calculation of overtones in log frequency spectra. 
	 * Generated with the method generateHarmonicFactors().
	 */
	public static int[] harmonics = null;

	/**
	 * Initialize the harmonics array.
	 * 
	 * @param numOfOvertones
	 * @param binsPerOctave
	 */
	public static void init(int numOfOvertones, double binsPerOctave) {
		if (harmonics == null) generateHarmonics(numOfOvertones, binsPerOctave);
	}
	
	/**
	 * Generates relative bin positions for the overtone harmonics.
	 * 
	 * @param amount number of overtones to be created
	 * @param binsPerOctave number of bins per octave in the spectral data
	 * @return
	 */
	private static void generateHarmonics(final int amount, final double binsPerOctave) {
		int[] ret = new int[amount];
		for(int i=0; i<amount; i++) {
			ret[i] = (int)(binsPerOctave * (Math.log((i+2)*2) / Math.log(2) - 1));
		}
		harmonics = ret;
	}
}
