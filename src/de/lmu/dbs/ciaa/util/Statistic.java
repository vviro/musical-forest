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

	private int count;
	
	private double heap;
	
	private double max = Double.MIN_VALUE;

	private double min = Double.MAX_VALUE;
	
	private List<Double> entries;
	
	public Statistic() {
		entries = new ArrayList<Double>();
	}
	
	public void add(double value) {
		heap += value;
		count++;
		if (value > max) max = value;
		if (value < min) min = value;
		entries.add(value);
	}
	
	public void add(float value) {
		add((double)value);
	}

	public void add(byte value) {
		add((double)value);
	}

	public void add(short value) {
		add((double)value);
	}

	public void add(int value) {
		add((double)value);
	}

	public void add(long value) {
		add((double)value);
	}
	
	public double getAverage() {
		return heap / count;
	}
	
	public double getMaximum() {
		return max;
	}
	
	public double getMinimum() {
		return min;
	}
	
	@Override
	public String toString() {
		return "Min: " + min + "; Max: " + max + "; Avg: " + getAverage(); 
	}
	
	public String getDistribution(int bins, int maxL) {
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
		//DecimalFormat df2 = new DecimalFormat( "#,###,###,##0.00" );
		for(int i=0; i<s.length; i++) {
			s[i]/=maxS;
			//ret += df2.format((f*i)+min) + ": ";
			ret += ((f*i)+min) + ": ";
			for(int j=0; j<s[i]; j++) ret += "#";
			ret += "\n";
		}
		return ret;
	}
	
}
