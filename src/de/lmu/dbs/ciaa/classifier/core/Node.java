package de.lmu.dbs.ciaa.classifier.core;

import java.io.Serializable;


public abstract class Node implements Serializable {

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
	 * Create a new node.
	 * 
	 */
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
		return (left == null) && (right == null);
	}
	
	/**
	 * Returns a visualization of all node features of the forest. For debugging use.
	 * 
	 * @param data the array to store results (additive)
	 */
	public abstract void visualize(Object data);

}
