package de.lmu.dbs.ciaa.util;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple class to get statistics about something.
 * 
 * @author Thomas Weber
 *
 */
public class Statistic {

	/**
	 * Number of entries
	 */
	private int count;
	
	/**
	 * Sum of all entries;
	 */
	private double sum;
	
	/**
	 * Maximum of all entries
	 */
	private double max = Double.MIN_VALUE;

	/**
	 * Minimum of all entries
	 */
	private double min = Double.MAX_VALUE;
	
	/**
	 * List containing all entries
	 */
	private List<Double> entries;
	
	/**
	 * Create a statistic for one value´s distribution.
	 * 
	 */
	public Statistic() {
		entries = new ArrayList<Double>();
	}
	
	/**
	 * Add a value to the statistic.
	 * 
	 * @param value
	 */
	public void add(double value) {
		sum += value;
		count++;
		if (value > max) max = value;
		if (value < min) min = value;
		entries.add(value);
	}
	
	/**
	 * Add a value to the statistic.
	 * 
	 * @param value
	 */
	public void add(float value) {
		add((double)value);
	}

	/**
	 * Add a value to the statistic.
	 * 
	 * @param value
	 */
	public void add(byte value) {
		add((double)value);
	}

	/**
	 * Add a value to the statistic.
	 * 
	 * @param value
	 */
	public void add(short value) {
		add((double)value);
	}

	/**
	 * Add a value to the statistic.
	 * 
	 * @param value
	 */
	public void add(int value) {
		add((double)value);
	}

	/**
	 * Add a value to the statistic.
	 * 
	 * @param value
	 */
	public void add(long value) {
		add((double)value);
	}
	
	/**
	 * Returns the arithmetic average over all values.
	 * 
	 * @return
	 */
	public double getAverage() {
		return sum / count;
	}
	
	/**
	 * Returns the value of the maximum of all entries.
	 * 
	 * @return
	 */
	public double getMaximum() {
		return max;
	}
	
	/**
	 * Returns the value of the minimum of all entries.
	 * 
	 * @return
	 */
	public double getMinimum() {
		return min;
	}
	
	/**
	 * Returns a reabable String containing the main statistics about the entries.
	 * 
	 */
	@Override
	public String toString() {
		return "Min: " + min + "; Max: " + max + "; Avg: " + getAverage(); 
	}
	
	/**
	 * Returns the distribution histogram of entries for console/file output.
	 * 
	 * @param bins number of histogram bins
	 * @param maxL width of histogram in characters
	 * @return
	 */
	public String getDistributionString(int bins, int maxL) {
		String ret = "";
		double[] s = new double[bins];
		double f = (bins-1) / (max - min);
		double maxS = Double.MIN_VALUE;
		for(int i=0; i<entries.size(); i++) {
			int index = (int)((entries.get(i)-min)*f);
			s[index] ++;
			if (s[index] > maxS) maxS = s[index];
		}
		maxS/= (double)maxL;
		DecimalFormat df2 = new DecimalFormat( "#,###,###,##0.00000000" );
		for(int i=0; i<s.length; i++) {
			ret += df2.format((i/f)+min) + ": ";
			int scaled = (int)(s[i]/maxS);
			for(int j=0; j<scaled; j++) ret += "#";
			ret += " (" + (int)s[i] + ")\n";
		}
		return ret;
	}
	
	/**
	 * Returns the entries list.
	 * 
	 * @return
	 */
	public List<Double> getEntries() {
		return entries;
	}
	
}
