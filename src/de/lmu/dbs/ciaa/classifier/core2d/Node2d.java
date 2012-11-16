package de.lmu.dbs.ciaa.classifier.core2d;

import java.awt.Color;
import java.io.File;
import java.io.Serializable;

import de.lmu.dbs.ciaa.classifier.features.Feature;
import de.lmu.dbs.ciaa.util.ArrayToImage;

/**
 * A node in the decision tree.
 * 
 * @author Thomas Weber
 *
 */
public class Node2d implements Serializable {
	
	private static final long serialVersionUID = 2L;

	/**
	 * Stores the last classification done by this node. For debugging.
	 */
	public int[][] debugTree = null;

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
	public Node2d left = null;
	
	/**
	 * Right branch node
	 */
	public Node2d right = null;
	
	/**
	 * If the node is a leaf, the probabilities for each class are stored here.
	 * 
	 */
	public float[] probabilities = null;
	
	/**
	 * Probability 
	 */
	//public float probability = 0;
	
	public Node2d() {
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
		ArrayToImage img = new ArrayToImage(debugTree.length, debugTree[0].length);
		int[][] l = new int[debugTree.length][debugTree[0].length];
		int[][] r = new int[debugTree.length][debugTree[0].length];
		for (int i=0; i<l.length; i++) {
			for (int j=0; j<l[0].length; j++) {
				if (debugTree[i][j] == 1) l[i][j] = 1;
				if (debugTree[i][j] == 2) r[i][j] = 1;
			}
		}
		img.add(l, new Color(0,255,0), null, 0);
		img.add(r, new Color(255,0,0), null, 0);
		img.save(new File(filename));
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
	public void visualize(int[][] data) {
		if (isLeaf()) return;
		feature.visualize(data);
		left.visualize(data);
		right.visualize(data);
	}
}
