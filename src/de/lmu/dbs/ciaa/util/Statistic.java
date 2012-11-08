package de.lmu.dbs.ciaa.util;

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
	
	public void add(double value) {
		heap += value;
		count++;
		if (value > max) max = value;
		if (value < min) min = value;
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
	
}
