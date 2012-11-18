package de.lmu.dbs.ciaa.classifier.core;

import java.io.Serializable;

/**
 * Node.
 * 
 * @author Thomas Weber
 *
 */
public class Node implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;

	/**
	 * Individual (ascending) node id (just for debugging)
	 */
	public long id = -1;
	
	/**
	 * Next node id
	 */
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
	 * If the node is a leaf, the probabilities for each class are stored here.
	 * 
	 */
	public float[] probabilities = null;

	/**
	 * Object for debugging. Can be used to attach any data to the node, 
	 * i.e. the classification.
	 */
	public Object debugObject;
	
	/**
	 * Create a new node.
	 * 
	 */
	public Node() {
		this.id = nextId;
		nextId++;
	}
	
	/**
	 * Determines whether the node is a leaf or not.
	 * 
	 * @return
	 */
	public boolean isLeaf() {
		return (left == null) && (right == null);
	}
	
	/**
	 * Returns a visualization of all node features of the forest. For debugging purpose.
	 * 
	 * @param data the array to store results (additive)
	 */
	public void visualize(Object data) {
		if (isLeaf()) return;
		feature.visualize((int[][])data);
		left.visualize(data);
		right.visualize(data);
	}
}
