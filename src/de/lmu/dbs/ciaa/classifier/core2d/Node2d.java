package de.lmu.dbs.ciaa.classifier.core2d;

import java.awt.Color;
import java.io.File;

import de.lmu.dbs.ciaa.classifier.core.Node;
import de.lmu.dbs.ciaa.util.ArrayToImage;

/**
 * 2d node.
 * 
 * @author Thomas Weber
 *
 */
public class Node2d extends Node {

	/**
	 * The feature which classifies the node branches
	 */
	public Feature2d feature;
	
	/**
	 * Stores the last classification done by this node. For debugging.
	 */
	public int[][] debugTree = null;

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
	 * Returns a visualization of all node features of the forest. For debugging use.
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
