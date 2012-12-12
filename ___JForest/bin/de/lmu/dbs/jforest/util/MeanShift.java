package de.lmu.dbs.jforest.util;

/**
 * Local maximum finding algorithm based on mean shift. Also segments the data array.
 * 
 * @author Thomas Weber
 *
 */
public class MeanShift {
	
	/**
	 * Window size for segmentation
	 */
	private int windowSize;
	
	/**
	 * 
	 * @param windowSize
	 */
	public MeanShift(int windowSize) {
		this.windowSize = windowSize/2;
	}
	
	/**
	 * Segmentation of the input array. Is filled by process(). Contains the mode numbers of each coordinate.
	 */
	public int[][] segmentation;
	
	/**
	 * Contains the found modes, represented by the segmentation numbers (mode numbers).
	 */
	public int[][] modes; // TODO: List?
	
	/**
	 * Contains the found modes, but represented by the amount of pixels that led to the mode.
	 */
	public float[][] modeWeights; // TODO: List?
	
	/**
	 * Process mean shifting.
	 * 
	 * @param data
	 * @param threshold
	 */
	public void process(float[][] data, float threshold) {
	
		modes = new int[data.length][data[0].length];
		modeWeights = new float[data.length][data[0].length];
		segmentation = new int[data.length][data[0].length];
		
		int nextModeId = 1;
		// Iterate all pixels
		for(int dx=0; dx<data.length; dx++) {
			for(int dy=0; dy<data[0].length; dy++) {
				if (data[dx][dy] >= threshold) {
					int x = dx;
					int y = dy;

					// Segment this pixel
					int iter = 0;
					double diff;
					boolean count = true;
					do {
						float avg = -Float.MAX_VALUE;
						int meanX = -1;
						int meanY = -1;
						int ch = 0; 
						// Search maximum in the current window
						for(int nx=x-windowSize; nx<x+windowSize; nx++) {
							if (nx >= 0 && nx < data.length) {
								for(int ny=y-windowSize; ny<y+windowSize; ny++) {
									if (ny >= 0 && ny < data[0].length) {
										if (data[nx][ny] >= threshold) {
											if (data[nx][ny] > avg) {
												avg = data[nx][ny];
												meanX = nx;
												meanY = ny;
												ch++;
											}
										}
									}
								}
							}
						}
						if (ch <= 1) {
							// All values equal (no maximum) -> just ignore this pixel (see below)
							count = false;
						}
						diff = Math.sqrt((x-meanX)*(x-meanX) + (y-meanY)*(y-meanY)); // How far have we gone in this iteration?
						x = meanX;
						y = meanY;
						iter++;
					} while (count && diff > 0.5f && iter < 100);
					
					if (count) {
						if (modes[x][y] == 0) {
							modes[x][y] = nextModeId;
							nextModeId++;
						}
						modeWeights[x][y]++;
						segmentation[dx][dy] = modes[x][y];
					}
				}
			}
		}
	}
}