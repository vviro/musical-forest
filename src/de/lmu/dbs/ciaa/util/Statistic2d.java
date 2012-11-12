package de.lmu.dbs.ciaa.util;

import java.awt.Color;
import java.io.File;
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
	 * Saves a visualization of the 2d distribution to a png file.
	 * 
	 * @param bins number of histogram bins
	 * @param maxL width of histogram in characters
	 * @return
	 * @throws Exception 
	 */
	public void saveDistributionImage(String filename, int width, int height) throws Exception {
		long[][] data = new long[width][height];
		byte[][] grid = new byte[width][height];
		List<Double> entries1 = var1.getEntries();
		List<Double> entries2 = var2.getEntries();
		double m1 = var1.getMinimum();
		double m2 = var2.getMinimum();
		double xfactor = (double)width / entries1.size();
		double yfactor = (double)height / entries2.size();
		for(int i=0; i<entries1.size(); i++) {
			int x = (int)((entries1.get(i)-m1) * xfactor);
			int y = (int)((entries2.get(i)-m2) * yfactor);
			data[x][y]++;
		}
		int gx = (int)((-m1) * xfactor);
		int gy = (int)((-m2) * yfactor);
		for(int i=0; i<width; i++) {
			grid[i][gy] = 1;
		}
		for(int i=0; i<height; i++) {
			grid[gx][i] = 1;
		}
		SpectrumToImage img = new SpectrumToImage(width, height);
		img.add(grid, Color.BLUE, null, 0);
		img.add(data, Color.WHITE);
		img.save(new File(filename));
	}
}
