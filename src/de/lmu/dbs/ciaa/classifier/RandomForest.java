package de.lmu.dbs.ciaa.classifier;

import java.util.ArrayList;
import java.util.List;

import de.lmu.dbs.ciaa.util.ArrayUtils;

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
	protected List<RandomTree> trees;
	
	/**
	 * Parameters for growing the trees in the forest.
	 */
	private ForestParameters params; 
	
	/**
	 * Creates a forest with numTrees trees.
	 * 
	 * @param numTrees
	 * @throws Exception 
	 */
	public RandomForest(final int numTrees, final ForestParameters params) throws Exception {
		this.params = params;
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
	public void grow(final Sampler<Dataset> sampler, final int maxDepth) throws Exception {
		for(int i=0; i<trees.size(); i++) {
			System.out.println("Growing tree " + i + " to depth " + maxDepth);
			trees.get(i).grow(sampler.getSample(), maxDepth);
		}
		if (params.threadDepth >= 0) {
			// Multithreading is active, so wait for the results 
			// TODO: Busy waiting, can be more effectively, but not critical for this application)
			while(true) {
				Thread.sleep(params.threadWaitTime);
				boolean ret = true;
				for(int i=0; i<trees.size(); i++) {
					if (params.debugThreadPolling) System.out.println("--> Active threads in tree " + i + ": " + trees.get(i).getThreadsActive());
					if (!trees.get(i).isGrown()) {
						ret = false;
						if (!params.debugThreadPolling) break;
					}
				}
				if (ret) break;
			}
		}
	}

	/**
	 * Returns the probability for event(s) on value x/y.
	 * 
	 * @param data
	 * @param x
	 * @param y
	 * @return
	 * @throws Exception 
	 */
	public float classify(final byte[][] data, final int x, final int y) throws Exception {
		float ret = 0;
		for(int i=0; i<trees.size(); i++) {
			ret += trees.get(i).classify(data, x, y);
		}
		return ret / trees.size();
	}
	
	/**
	 * Classifies a whole 2d array of data values and returns the results, normalized to [0,1].
	 * 
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public float[][] classify(byte[][] data) throws Exception {
		float[][] dataForest = new float[data.length][data[0].length];
		for(int x=0; x<data.length; x++) {
			for(int y=0; y<data[0].length; y++) {
				dataForest[x][y] = classify(data, x, y);
			}
		}
		ArrayUtils.normalize(dataForest);
		return dataForest;
	}
	
	/**
	 * Saves the trained forest parameters to disk. Each tree is stored in one file.
	 * 
	 * @param file
	 * @throws Exception 
	 */
	public void save(final String filename) throws Exception {
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
	public static RandomForest load(final String filename, final int numTrees) throws Exception {
		RandomForest f = new RandomForest();
		for(int i=0; i<numTrees; i++) {
			f.trees.add(RandomTree.load(filename + "_tree" + i));
		}
		return f;
	}
	
	/**
	 * Returns a visualization of all node features of the forest. For debugging use.
	 * 
	 * @return
	 */
	public int[][] visualize(ForestParameters params) {
		int[][] ret = new int[(params.xMax - params.xMin)*2][params.frequencies.length];
		for(int i=0; i<trees.size(); i++) {
			trees.get(i).visualize(ret);
		}
		return ret;
	}
}
