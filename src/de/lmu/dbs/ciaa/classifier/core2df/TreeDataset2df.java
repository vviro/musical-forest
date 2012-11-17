package de.lmu.dbs.ciaa.classifier.core2df;

import java.io.File;
import de.lmu.dbs.ciaa.classifier.core.TreeDataset;

/**
 * Abstract dataset class for usage in trained trees.
 * 
 * @author Thomas Weber
 *
 */
public abstract class TreeDataset2df extends TreeDataset {

	public TreeDataset2df(File dataFile, File referenceFile) throws Exception {
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
	public synchronized Object getInitialClassification() throws Exception {
		if (!isLoaded()) load();
		byte[][] dataC = (byte[][])data;
		byte[] ret = new byte[dataC.length];
		for(int f=0; f<ret.length; f++) {
			if (!isSampled(f)) {
				ret[f] = -1; // Throw all out of bag
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
	public synchronized Object getInitialClassification(final int valuesPerFrame) throws Exception {
		throw new Exception("Values per frame are not allowed in 2df");
	}

}
