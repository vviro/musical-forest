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

	private static final long serialVersionUID = 1L;

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
	public float[] probabilities = null; 
	
	/**
	 * Determines whether the node is a leaf or not.
	 * 
	 * @return
	 */
	public boolean isLeaf() {
		return (probabilities != null);
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
