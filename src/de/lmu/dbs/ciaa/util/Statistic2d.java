package de.lmu.dbs.ciaa.util;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple class to get statistics about something 2-dimensional.
 * 
 * @author Thomas Weber
 *
 */
public class Statistic2d {

	private Statistic var1 = new Statistic();
	private Statistic var2 = new Statistic();
	
	/**
	 * Add a value to the statistic.
	 * 
	 * @param value
	 */
	public void add(double value1, double value2) {
		var1.add(value1);
		var2.add(value2);
	}
	
	/**
	 * Add a value to the statistic.
	 * 
	 * @param value
	 */
	public void add(float value1, float value2) {
		add((double)value1, (double)value2);
	}

	/**
	 * Add a value to the statistic.
	 * 
	 * @param value
	 */
	public void add(byte value1, byte value2) {
		add((double)value1, (double)value2);
	}

	/**
	 * Add a value to the statistic.
	 * 
	 * @param value
	 */
	public void add(short value1, short value2) {
		add((double)value1, (double)value2);
	}

	/**
	 * Add a value to the statistic.
	 * 
	 * @param value
	 */
	public void add(int value1, int value2) {
		add((double)value1, (double)value2);
	}

	/**
	 * Add a value to the statistic.
	 * 
	 * @param value
	 */
	public void add(long value1, long value2) {
		add((double)value1, (double)value2);
	}

	public Statistic getStatistic1() {
		return var1;
	}
	
	public Statistic getStatistic2() {
		return var2;
	}

	/**
	 * Returns a reabable String containing the main statistics about the entries.
	 * 
	 */
	@Override
	public String toString() {
		String ret = "Var1: Min: " + var1.getMinimum() + "; Max: " + var1.getMaximum() + "; Avg: " + var1.getAverage() + "\n";
		ret += "Var2: Min: " + var2.getMinimum() + "; Max: " + var2.getMaximum() + "; Avg: " + var2.getAverage();
		return ret; 
	}
	
	/**
	 * Returns the distribution histogram of entries for console/file output.
	 * 
	 * @param bins number of histogram bins
	 * @param maxL width of histogram in characters
	 * @return
	 */
	public String getDistributionString(int bins, int maxL) {
		List<Double> entries1 = var1.getEntries();
		double min1 = var1.getMinimum();
		double max1 = var1.getMaximum();
		List<Double> entries2 = var2.getEntries();
		String ret = "";
		double[] s = new double[bins];
		double f = (bins-1) / (max1 - min1);
		double maxS = Double.MIN_VALUE;
		for(int i=0; i<entries1.size(); i++) {
			int index = (int)((entries1.get(i)-min1)*f);
			s[index] += entries2.get(i);
			if (s[index] > maxS) maxS = s[index];
		}
		maxS/= (double)maxL;
		DecimalFormat df2 = new DecimalFormat( "#,###,###,##0.00000000" );
		for(int i=0; i<s.length; i++) {
			ret += df2.format((i/f)+min1) + ": ";
			int scaled = (int)(s[i]/maxS);
			for(int j=0; j<scaled; j++) ret += "#";
			ret += " (" + (int)s[i] + ")\n";
		}
		return ret;
	}
	
}
