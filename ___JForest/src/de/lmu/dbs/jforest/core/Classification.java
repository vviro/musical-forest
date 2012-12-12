package de.lmu.dbs.jforest.core;

/**
 * Bas class for classification objects. Aclassification object holds information
 * for the tree nodes, particularly which values (coordinates) it should process.
 *  
 * @author Thomas Weber
 *
 */
public abstract class Classification {

	/**
	 * Resets the whole classification.
	 * 
	 */
	public abstract void clear();
	
	/**
	 * Returns the amount of stored values in this classification.
	 * 
	 * @return
	 */
	public abstract int getSize();
}

