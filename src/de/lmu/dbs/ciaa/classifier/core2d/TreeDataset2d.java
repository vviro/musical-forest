package de.lmu.dbs.ciaa.classifier.core2d;

import java.io.File;
import cern.jet.random.sampling.RandomSampler;
import de.lmu.dbs.ciaa.classifier.core.TreeDataset;

/**
 * Abstract dataset class for usage in trained trees.
 * 
 * @author Thomas Weber
 *
 */
public abstract class TreeDataset2d extends TreeDataset {

	public TreeDataset2d(File dataFile, File referenceFile) throws Exception {
		super(dataFile, referenceFile);
	}

	/**
	 * Returns the number of samples in the dataset.
	 * 
	 * @return
	 * @throws Exception 
	 */
	public int getLength() throws Exception {
		if (!isLoaded()) load();
		return ((byte[][])data).length;
	}
	
	/**
	 * Returns the initial classification array for this dataset.
	 * 
	 * @return
	 * @throws Exception
	 */
	public synchronized byte[][] getInitialClassification() throws Exception {
		if (!isLoaded()) load();
		byte[][] dataC = (byte[][])data;
		byte[][] ret = new byte[dataC.length][dataC[0].length];
		for(int f=0; f<ret.length; f++) {
			if (isSampled(f)) {
				for(int g=0; g<ret[0].length; g++) {
					ret[f][g] = -1; // Throw all out of bag
				}
			}
		}
		return ret;
	}
	
	/**
	 * Returns the initial classification array for this dataset.
	 * <br><br>
	 * Selects valuesPerFrame randomly chosen pixels from the dataset.
	 * This is done by classifying the value to -1 (out of bag). 
	 * 
	 * @param valuesPerFrame
	 * @return
	 * @throws Exception 
	 */
	public synchronized byte[][] getInitialClassification(final int valuesPerFrame) throws Exception {
		if (!isLoaded()) load();
		byte[][] dataC = (byte[][])data;
		if (valuesPerFrame >= dataC[0].length) {
			// All sampled frames should be in completely
			return getInitialClassification(); 
		}
		byte[][] ret = new byte[dataC.length][dataC[0].length];
		for(int i=0; i<dataC.length; i++) {
			for(int j=0; j<dataC[0].length; j++) {
				ret[i][j] = -1; // Throw all out of bag 
			}
		}
		// Get sample without replacement
		long[] array = new long[valuesPerFrame*dataC.length];
		RandomSampler.sample(
				valuesPerFrame*dataC.length, // n 
				dataC.length*dataC[0].length, // N
				valuesPerFrame*dataC.length, // count 
				0, // low 
				array, 
				0, 
				null);
		for(int i=0; i<array.length; i++) {
			int y = (int)Math.floor(array[i] / dataC.length);
			if (!isSampled(y)) continue;
			int x = (int)array[i] % dataC.length;
			ret[x][y] = 0; 
		}
		return ret;
	}

}
