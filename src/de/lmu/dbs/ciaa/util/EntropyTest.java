package de.lmu.dbs.ciaa.util;

import java.awt.Color;
import java.io.File;

import de.lmu.dbs.ciaa.classifier.RandomTree;

/**
 * Visualizes entropy function.
 * 
 * @author Thomas Weber
 *
 */
public class EntropyTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		int x = 500;
		int y = 500;
		String outFile = "testdataResults/entropyTest.png";
		
		try {
			double[][] e = new double[x][y];
			for(int i=0; i<x; i++) {
				for(int j=0; j<y; j++) {
					e[i][j] = RandomTree.getEntropy(i, j);
				}
			}
			
			SpectrumToImage imgF = new SpectrumToImage(x, y, 1, 1);
			imgF.add(e, Color.GREEN);
			imgF.save(new File(outFile));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
