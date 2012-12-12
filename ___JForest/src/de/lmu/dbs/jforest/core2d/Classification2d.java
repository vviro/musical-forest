package de.lmu.dbs.jforest.core2d;

import de.lmu.dbs.jforest.core.Classification;

/**
 * Classification object for 2d data.
 * 
 * @author Thomas Weber
 *
 */
public class Classification2d extends Classification {

	public int[] xIndex;
	
	public int[] yIndex;
	
	public Classification2d(int size) {
		xIndex = new int[size];
		yIndex = new int[size];
	}
	
	@Override
	public void clear() {
		xIndex = null;
		yIndex = null;
	}

	@Override
	public int getSize() {
		return xIndex.length;
	}

	/**
	 * Generates a byte array with the classified pixels (x/y) set to 1.
	 * 
	 * @param w
	 * @param h
	 * @return
	 */
	public byte[][] toByteArray(int w, int h) {
		byte[][] ret = new byte[w][h];
		for (int i=0; i<xIndex.length; i++) {
			ret[xIndex[i]][yIndex[i]] = 1;
		}
		return ret;
	}
}
