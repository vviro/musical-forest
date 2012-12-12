package de.lmu.dbs.ciaa.classifier.core2d;

import java.io.File;
import cern.jet.random.sampling.RandomSampler;
import de.lmu.dbs.ciaa.classifier.core.Classification;
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
	 * Returns the number of samples in the dataset (Width of 2d data matrix).
	 * 
	 * @return
	 * @throws Exception 
	 */
	public int getLength() throws Exception {
		if (!isLoaded()) load();
		return ((byte[][])data).length;
	}
	
	/**
	 * Returns the height of the 2d data matrix.
	 * 
	 * @return
	 * @throws Exception
	 */
	public int getHeight() throws Exception {
		if (!isLoaded()) load();
		return ((byte[][])data)[0].length;
	}
	
	/**
	 * Returns the initial classification array for this dataset.
	 * 
	 * @return
	 * @throws Exception
	 */
	public synchronized Classification getInitialClassification() throws Exception {
		int len = getLength();
		int hei = getHeight();
		int cnt = 0;
		for(int f=0; f<len; f++) {
			if (isSampled(f)) {
				cnt+=hei;
			}
		}
		Classification2d ret = new Classification2d(cnt);
		int index = 0;
		for(int f=0; f<len; f++) {
			if (isSampled(f)) {
				for(int g=0; g<hei; g++) {
					ret.xIndex[index] = f;
					ret.yIndex[index] = g;
					index++;
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
	public synchronized Classification getInitialClassification(int valuesPerFrame) throws Exception {
		if (!isLoaded()) load();
		byte[][] dataC = (byte[][])data;
		if (valuesPerFrame >= dataC[0].length) {
			// All sampled frames should be in completely
			return getInitialClassification(); 
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
		Classification2d ret = new Classification2d(array.length);
		int index = 0;
		for(int i=0; i<array.length; i++) {
			ret.xIndex[index] = (int)array[i] % dataC.length;
			if (!isSampled(ret.xIndex[index])) continue;
			ret.yIndex[index] = (int)Math.floor(array[i] / dataC.length);
			index++;
		}
		return ret;
	}

}
