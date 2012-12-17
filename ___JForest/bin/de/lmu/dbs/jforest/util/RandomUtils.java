package de.lmu.dbs.jforest.util;

import de.lmu.dbs.jforest.util.ArrayUtils;
import de.lmu.dbs.jforest.util.RandomUtils;

/**
 * Class containing some random utilities
 * 
 * @author Thomas Weber
 *
 */
public class RandomUtils {

	/**
	 * Returns a random integer from a distribution.
	 * The distribution array has to contain the probability density for each if its indexes
	 * One of these indexes will be returned, so output is in range [0..distribution.length].
	 * 
	 * @param distribution
	 * @return
	 */
	public static int randomDistributedInt(double[] distribution) {
		double r = Math.random();
		double sum = 0;
		int index = 0;
		while(sum < r && index < distribution.length) {
			sum+= distribution[index];
			index++;
		}
		return index-1;
	}
	
	public static void testRandomDistributionInt() { 
		
		double[] dist = new double[20];
		for(int i=0; i<dist.length; i++) {
			dist[i] = randomInt(100);
		}
		ArrayUtils.density(dist);
		
		double[] test = new double[dist.length];
		for(int i=0; i<1000000; i++) {
			int r = RandomUtils.randomDistributedInt(dist);
			test[r]++;
		}
		ArrayUtils.out(dist);
		System.out.println("\n");
		ArrayUtils.density(test);
		ArrayUtils.out(test);
		System.out.println("\n");
		double diff = 0;
		for(int i=0; i<test.length; i++) {
			double diff2 = Math.abs(test[i] - dist[i]);
			diff+= diff2;
			System.out.println(i + " diff = " + diff2);
		}
		System.out.println("diff = " + diff);
	}
	
	/**
	 * Returns a random integer in range [0, max]
	 * 
	 * @param max
	 * @return
	 */
	public static int randomInt(final int max) {
		return randomInt(0, max);
	}
	
	/**
	 * Returns a random integer in range [min, max]
	 * 
	 * @param max
	 * @return
	 */
	public static int randomInt(final int min, final int max) {
		return min + (int)(Math.random() * ((max - min) + 1));
	}

	/**
	 * Returns a random long integer in range [0, max]
	 * 
	 * @param max
	 * @return
	 */
	public static long randomLong(final long max) {
		return randomLong(0, max);
	}
	
	/**
	 * Returns a random long integer in range [min, max]
	 * 
	 * @param max
	 * @return
	 */
	public static long randomLong(final long min, final long max) {
		return min + (long)(Math.random() * ((max - min) + 1));
	}

}
