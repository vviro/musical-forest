package de.lmu.dbs.ciaa.util;

/**
 * Class containing some random utilities
 * 
 * @author Thomas Weber
 *
 */
public class RandomUtils {

	/**
	 * Returns a random integer in range [0, max]
	 * 
	 * @param max
	 * @return
	 */
	public static int randomInt(int max) {
		return randomInt(0, max);
	}
	
	/**
	 * Returns a random integer in range [min, max]
	 * 
	 * @param max
	 * @return
	 */
	public static int randomInt(int min, int max) {
		return min + (int)(Math.random() * ((max - min) + 1));
	}

	/**
	 * Returns a random long integer in range [0, max]
	 * 
	 * @param max
	 * @return
	 */
	public static long randomLong(long max) {
		return randomLong(0, max);
	}
	
	/**
	 * Returns a random long integer in range [min, max]
	 * 
	 * @param max
	 * @return
	 */
	public static long randomLong(long min, long max) {
		return min + (long)(Math.random() * ((max - min) + 1));
	}

}
