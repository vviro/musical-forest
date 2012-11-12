package de.lmu.dbs.ciaa.classifier;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.lmu.dbs.ciaa.util.Log;

/**
 * Random forest implementation. 
 * 
 * @author Thomas Weber
 *
 */
public class Forest {

	/**
	 * The trees of the forest
	 */
	protected List<Tree> trees;
	
	/**
	 * Parameters for growing the trees in the forest.
	 */
	private ForestParameters params; 
	
	/**
	 * Creates a forest with some trees.
	 * 
	 * @param trees
	 * @throws Exception 
	 */
	public Forest(List<Tree> trees, final ForestParameters params) throws Exception {
		this.params = params;
		// Generate trees
		this.trees = trees; //new ArrayList<RandomTree>();
		for(int i=0; i<trees.size(); i++) {
			trees.get(i).setForest(this);
		}
	}
	
	/**
	 * Creates a blank forest.
	 * 
	 * @param numTrees
	 * @throws Exception 
	 */
	public Forest() {
		this.trees = new ArrayList<Tree>();
	}

	/**
	 * Date formatter for debug output.
	 */
	protected SimpleDateFormat timeStampFormatter = new SimpleDateFormat("hh:mm:ss");
	
	private long startTime;
	
	/**
	 * Grows the forest. 
	 * 
	 * @param sampler data provider
	 * @throws Exception
	 */
	public void grow(final Sampler<Dataset> sampler) throws Exception {
		startTime = System.currentTimeMillis();
		for(int i=0; i<trees.size(); i++) {
			Log.write("Growing tree " + i + " to depth " + params.maxDepth);
			trees.get(i).grow((trees.size() == 1) ? sampler : sampler.getSample(), params.maxDepth);
		}
		if (params.maxNumOfNodeThreads > 0) {
			// Multithreading is active, so wait for the results 
			// TODO: Busy waiting, can be done more effectively, but not critical for this application
			while(true) {
				Thread.sleep(params.threadWaitTime);
				boolean ret = true;
				for(int i=0; i<trees.size(); i++) {
					if (!trees.get(i).isGrown()) {
						ret = false;
						if (!params.debugThreadPolling) break;
					}
				}
				if (params.debugThreadPolling) {
					// Debug output
					System.out.print(timeStampFormatter.format(new Date()) + ": Threads: ");
					for(int i=0; i<trees.size(); i++) {
						System.out.print(trees.get(i).getThreadsActive() + " ");
					}
					/*if (params.maxNumOfEvaluationThreads > 0) {
						System.out.print("  Active worker threads: ");
						for(int i=0; i<trees.size(); i++) {
							System.out.print(trees.get(i).getThreadsActive(1) + " ");
						}
						System.out.println(" (sums: " + getThreadsActive(0) + "/" + getThreadsActive(1) + "); Heap: " + ((int)(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()) / (1024*1024)) + " MB");
					} else {*/
						System.out.println("Heap: " + ((int)(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()) / (1024*1024)) + " MB");
					//}
				}
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
		Log.write("### Forest stats ###", System.out);
		int count = getNodeCount();
		Log.write("Num of nodes: " + count, System.out);
		long delta = System.currentTimeMillis() - startTime;
		Log.write("Time elapsed: " + delta/1000.0 + " sec", System.out);
		Log.write("Avg. time per node: " + (delta/count)/1000.0 + " sec", System.out);
		int poss = (int)Math.pow(2, params.maxDepth);
		TreeAnalyzer ana = new TreeAnalyzer(params);
		for(int i=0; i<trees.size(); i++) {
			Tree t = trees.get(i);
			Log.write("Tree " + i + ": " + ana.getNumOfLeafs(t) +  " leafs of possible " + poss + "; Information gain: " + t.getInfoGain());
			Log.write("Count nodes at depths:\n" + ana.getDepthCountsString(t));
			Log.write("Tree structure:\n" + ana.getTreeVisualization(t));
			Log.write("Distribution of gains upon all nodes in tree " + i + ":\n" + t.getInfoGain().getDistributionString(20, 80));
		}
	}

	/**
	 * Returns the amount of nodes in the forest
	 * 
	 * @return
	 * @throws Exception 
	 */
	public int getNodeCount() throws Exception {
		int ret = 0;
		TreeAnalyzer ana = new TreeAnalyzer(params);
		for(int i=0; i<trees.size(); i++) {
			int[] d = ana.getDepthCounts(trees.get(i));
			for(int j=0; j<d.length; j++) {
				ret+= d[j];
			}
		}
		return ret; 
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
		//ArrayUtils.normalize(dataForest);
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
	public static Forest load(final String filename, final int numTrees) throws Exception {
		Forest f = new Forest();
		for(int i=0; i<numTrees; i++) {
			f.trees.add(RandomTree.load(filename + "_tree" + i)); // TODO Type tree / randomtree
		}
		return f;
	}
	
	/**
	 * Returns a visualization of all node features of the forest. For debugging use.
	 * 
	 * @return
	 * @throws Exception 
	 */
	public int[][] visualize(ForestParameters params) throws Exception {
		int[][] ret = new int[(params.xMax - params.xMin + 1)*2][params.frequencies.length];
		TreeAnalyzer ana = new TreeAnalyzer(params);
		for(int i=0; i<trees.size(); i++) {
			ana.visualize(trees.get(i), ret);
		}
		return ret;
	}
	
	/**
	 * Returns the overall amount of threads being active.
	 * 
	 * @return
	 * @throws Exception 
	 */
	public synchronized int getThreadsActive() throws Exception {
		int ret = 0;
		for(int i=0; i<trees.size(); i++) {
			ret += trees.get(i).getThreadsActive();
		}
		return ret;
	}
}
