package de.lmu.dbs.ciaa.classifier;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.lmu.dbs.ciaa.util.ArrayUtils;
import de.lmu.dbs.ciaa.util.Log;

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
			this.trees.add(new RandomTreeAnalyzable(params, this, i));
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
	 * Date formatter for debug output.
	 */
	protected SimpleDateFormat timeStampFormatter = new SimpleDateFormat("hh:mm:ss");
	
	/**
	 * Grows the forest. 
	 * 
	 * @param sampler data provider
	 * @throws Exception
	 */
	public void grow(final Sampler<Dataset> sampler) throws Exception {
		for(int i=0; i<trees.size(); i++) {
			Log.write("Growing tree " + i + " to depth " + params.maxDepth);
			trees.get(i).grow(sampler.getSample(), params.maxDepth);
		}
		if (params.maxNumOfNodeThreads > 0) {
			// Multithreading is active, so wait for the results 
			// TODO: Busy waiting, can be done more effectively, but not critical for this application
			while(true) {
				Thread.sleep(params.threadWaitTime);
				if (params.debugThreadPolling) System.out.print(timeStampFormatter.format(new Date()) + ": Active threads (node/eval): ");
				boolean ret = true;
				for(int i=0; i<trees.size(); i++) {
					if (params.debugThreadPolling) System.out.print(trees.get(i).getThreadsActive(0) + "/" + trees.get(i).getThreadsActive(1) + " ");
					if (!trees.get(i).isGrown()) {
						ret = false;
						if (!params.debugThreadPolling) break;
					}
				}
				if (params.debugThreadPolling) System.out.println(" (sum: " + this.getThreadsActive(0) + "/" + this.getThreadsActive(1) + "); Heap: " + ((int)(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()) / (1024*1024)) + " MB");
				if (ret) break;
			}
		}
	}
	
	/**
	 * Debug tree stats to Log.
	 * 
	 * @throws Exception 
	 * 
	 */
	public void logTreeStats() throws Exception {
		Log.write("");
		Log.write("### Forest stats ###");
		int poss = (int)Math.pow(2, params.maxDepth);
		for(int i=0; i<trees.size(); i++) {
			RandomTreeAnalyzable t = (RandomTreeAnalyzable)trees.get(i);
			Log.write("Tree " + i + ": " + t.getNumOfLeafs() +  " leafs of possible " + poss + "; Information gain: " + t.infoGain);
			Log.write("Count nodes at depths:\n" + t.getDepthCountsString());
			//Log.write("Tree structure:\n" + t.getTreeVisualization());
			Log.write("Distribution of gains:\n" + t.infoGain.getDistributionString(20, 80));
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
		int[][] ret = new int[(params.xMax - params.xMin + 1)*2][params.frequencies.length];
		for(int i=0; i<trees.size(); i++) {
			((RandomTreeAnalyzable)trees.get(i)).visualize(ret);
		}
		return ret;
	}
	
	/**
	 * Returns the overall amount of threads being active.
	 * 
	 * @param mode: 0: node, 1: evaluation
	 * @return
	 * @throws Exception 
	 */
	public synchronized int getThreadsActive(int mode) throws Exception {
		int ret = 0;
		for(int i=0; i<trees.size(); i++) {
			ret += trees.get(i).getThreadsActive(mode);
		}
		return ret;
	}
}
