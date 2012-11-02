package de.lmu.dbs.ciaa.classifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Random forest implementation. 
 * 
 * @author Thomas Weber
 *
 */
public class RandomForest {

	/**
	 * The trees of the forest
	 */
	private List<RandomTree> trees;
	
	/**
	 * Creates a forest with numTrees trees.
	 * 
	 * @param numTrees
	 * @throws Exception 
	 */
	public RandomForest(int numTrees, RandomTreeParameters params) throws Exception {
		// Generate trees
		this.trees = new ArrayList<RandomTree>();
		for(int i=0; i<numTrees; i++) {
			this.trees.add(new RandomTree(params));
		}
	}
	
	/**
	 * Creates a blank forest.
	 * 
	 * @param numTrees
	 * @throws Exception 
	 */
	public RandomForest() {
		this.trees = new ArrayList<RandomTree>();
	}

	/**
	 * Grows the forest.
	 * 
	 * @param sampler data provider
	 * @throws Exception
	 */
	public void grow(Sampler<Dataset> sampler, int maxDepth) throws Exception {
		for(int i=0; i<trees.size(); i++) {
			trees.get(i).grow(sampler.getSample(), maxDepth);
		}
	}

	/**
	 * Returns the probability for event(s) on pixel x/y.
	 * (Probabilities are not normalized TODO)
	 * 
	 * @param data
	 * @param x
	 * @param y
	 * @return
	 * @throws Exception 
	 */
	public long classify(byte[][] data, int x, int y) throws Exception {
		long ret = 0;
		for(int i=0; i<trees.size(); i++) {
			ret += trees.get(i).classify(data, x, y);
		}
		return ret;
	}
	
	/**
	 * Saves the trained forest parameters to disk. Each tree is stored in one file.
	 * 
	 * @param file
	 * @throws Exception 
	 */
	public void save(String filename) throws Exception {
		for(int i=0; i<trees.size(); i++) {
			trees.get(i).save(filename + "_tree" + i);
		}
	}
	
	/**
	 * Loads a trained forest from file. Each tree is stored in one file.
	 * 
	 * @param filename
	 * @param numTrees
	 * @return
	 * @throws Exception
	 */
	public static RandomForest load(String filename, int numTrees) throws Exception {
		RandomForest f = new RandomForest();
		for(int i=0; i<numTrees; i++) {
			f.trees.add(RandomTree.load(filename + "_tree" + i));
		}
		return f;
	}
}
