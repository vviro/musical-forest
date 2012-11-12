package de.lmu.dbs.ciaa.classifier;

import java.io.Serializable;

import de.lmu.dbs.ciaa.classifier.features.Feature;

/**
 * A node in the decision tree.
 * 
 * @author Thomas Weber
 *
 */
public class Node implements Serializable {

	private static final long serialVersionUID = 2L;

	/**
	 * Individual (ascending) node id (just for debugging)
	 */
	public long id = -1;
	
	public static long nextId = 0;
	
	/**
	 * The feature which classifies the node branches
	 */
	public Feature feature;
	
	/**
	 * Left branch node
	 */
	public Node left = null;
	
	/**
	 * Right branch node
	 */
	public Node right = null;
	
	/**
	 * If the node is a leaf, here the probabilities for each frequency are stored
	 */
	//public float[] probabilities = null; 
	
	public float probability = 0;
	
	public Node() {
		synchronized(this) {
			this.id = nextId;
			nextId++;
		}
	}
	
	/**
	 * Determines whether the node is a leaf or not.
	 * 
	 * @return
	 */
	public boolean isLeaf() {
		//return (probabilities != null);
		return (left == null) && (right == null);
	}
	
	/**
	 * Returns a visualization of all node features of the forest. For debugging use.
	 * 
	 * @param data the array to store results (additive)
	 */
	public void visualize(int[][] data) {
		if (isLeaf()) return;
		feature.visualize(data);
		left.visualize(data);
		right.visualize(data);
	}
}
