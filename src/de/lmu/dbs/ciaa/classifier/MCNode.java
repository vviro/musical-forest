package de.lmu.dbs.ciaa.classifier;

import java.awt.Color;
import java.io.File;
import java.io.Serializable;

import de.lmu.dbs.ciaa.classifier.features.MCFeature;
import de.lmu.dbs.ciaa.util.SpectrumToImage;

/**
 * A node in the decision tree.
 * 
 * @author Thomas Weber
 *
 */
public class MCNode implements Serializable {
	
	private static final long serialVersionUID = 2L;

	/**
	 * Stores the last classification done by this node. For debugging.
	 */
	public double[][] debugTree = null;

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
	public MCFeature feature;
	
	/**
	 * Left branch node
	 */
	public MCNode left = null;
	
	/**
	 * Right branch node
	 */
	public MCNode right = null;
	
	/**
	 * If the node is a leaf, here the probabilities for each frequency are stored
	 */
	public float[] probabilities = null; 
	//public float probability = 0;
	
	public MCNode() {
		synchronized(this) {
			this.id = nextId;
			nextId++;
		}
	}
	
	/**
	 * Saves the current debugTree for visualization of the nodes decisions 
	 * at the last classification run.
	 * 
	 * @param filename
	 * @throws Exception 
	 */
	public void saveDebugTree(String filename) throws Exception {
		SpectrumToImage img = new SpectrumToImage(debugTree.length, debugTree[0].length);
		img.add(debugTree, Color.YELLOW);
		img.save(new File(filename));
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
