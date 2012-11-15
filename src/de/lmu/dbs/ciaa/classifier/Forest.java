package de.lmu.dbs.ciaa.classifier;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.lmu.dbs.ciaa.util.Logfile;

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
	 * Log file for the forest
	 */
	private Logfile log = null;
	
	/**
	 * Creates a forest with some trees.
	 * 
	 * @param trees
	 * @throws Exception 
	 */
	public Forest(List<Tree> trees, final ForestParameters params, Logfile log) throws Exception {
		this.params = params;
		this.trees = trees;
		this.log = log;
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
	public Forest(ForestParameters params) {
		this.trees = new ArrayList<Tree>();
		this.params = params;
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
		if (trees.size() < 1) throw new Exception("No trees to grow");
		startTime = System.currentTimeMillis();
		
		for(int i=0; i<trees.size(); i++) {
			System.out.println("Growing tree " + i + " to depth " + params.maxDepth);
			trees.get(i).grow((trees.size() == 1) ? sampler : sampler.getSample(), params.maxDepth);
		}
		if (params.maxNumOfNodeThreads > 0) {
			// Multithreading is active, so wait for the results 
			// TODO: Busy waiting, can be done more effectively, but not critical for this application
			while(true) {
				try {
					Thread.sleep(params.threadWaitTime);
				} catch (InterruptedException e) {
					System.out.println("[Wait interrupted by VM, continuing...]");
				}
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
					System.out.println("Heap: " + ((int)(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()) / (1024*1024)) + " MB");
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
	public void logStats() throws Exception {
		log.write("");
		log.write("### Tree stats ###", System.out);
		int count = getNodeCount();
		log.write("Num of nodes: " + count, System.out);
		long delta = System.currentTimeMillis() - startTime;
		log.write("Time elapsed: " + delta/1000.0 + " sec", System.out);
		log.write("Avg. time per node: " + (delta/count)/1000.0 + " sec", System.out);
		int poss = (int)Math.pow(2, params.maxDepth);
		TreeAnalyzer ana = new TreeAnalyzer(params);
		for(int i=0; i<trees.size(); i++) {
			Tree t = trees.get(i);
			log.write("Tree " + i + ": " + ana.getNumOfLeafs(t) +  " leafs of possible " + poss + "; Information gain: " + t.getInfoGain());
			log.write("Count nodes at depths:\n" + ana.getDepthCountsString(t));
			log.write("Tree structure:\n" + ana.getTreeVisualization(t));
			log.write("Distribution of gains upon all nodes in tree " + i + ":\n" + t.getInfoGain().getDistributionString(20, 80));
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
	public static Forest load(final ForestParameters params, final String filename, final int numTrees) throws Exception {
		Forest f = new Forest(params);
		for(int i=0; i<numTrees; i++) {
			f.trees.add(RandomTree.load(params, filename + "_tree" + i, i)); // TODO Type tree / randomtree
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

	/**
	 * Returns the tree list.
	 * 
	 * @return
	 */
	public List<Tree> getTrees() {
		return trees;
	}

}
