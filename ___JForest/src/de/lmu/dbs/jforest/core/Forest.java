package de.lmu.dbs.jforest.core;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.lmu.dbs.jforest.sampler.Sampler;
import de.lmu.dbs.jforest.util.Logfile;
import de.lmu.dbs.jforest.util.TreeAnalyzer;
import de.lmu.dbs.jforest.util.workergroup.ThreadScheduler;
import de.lmu.dbs.jforest.util.workergroup.Worker;

/**
 * Abstract core implementation of a random decision forest inspired by the implementation of Microsoft Kinect 
 * body part recognition system. The design is generic so this implementation can serve for many different classification purposes.
 * <br><br>
 * <b>Dependencies:</b>
 * <ul>
 * 		<li>Colt (sparse matrix, random algorithms)</li>
 *		<li>JDom 2.0.2 (for XML parsing)</li>
 * 		<li>Trove 3.0.3</li>
 * </ul> 
 * 
 * @author Thomas Weber
 *
 */
public abstract class Forest {
	
	/**
	 * Time delay between thread monitor outs (in millisecs).
	 */
	public static final long THREAD_POLLING_INTERVAL = 4000;

	/**
	 * The trees of the forest
	 */
	protected List<RandomTree> trees;
	
	/**
	 * Parameters for growing the trees in the forest.
	 */
	private ForestParameters params; 

	/**
	 * Log file for the forest
	 */
	private Logfile log = null;
	
	/**
	 * Thread scheduler handling node threading
	 */
	public ThreadScheduler nodeScheduler;

	/**
	 * Thread scheduler handling evaluation threading
	 */
	public ThreadScheduler evalScheduler;
	
	/**
	 * Threshold below which the algorithm switches into node threading. If not set, node threading
	 * will be disabled.
	 * 
	 */
	public int nodeThreadingThreshold = -1;
	
	/**
	 * Date formatter for debug output.
	 */
	protected SimpleDateFormat timeStampFormatter = new SimpleDateFormat("hh:mm:ss");
	
	/**
	 * Grow starting time
	 */
	private long startTime;
	
	/**
	 * Creates a forest with some trees.
	 * 
	 * @param trees
	 * @throws Exception 
	 */
	public Forest(List<RandomTree> trees, final ForestParameters params, Logfile log, int maxNumOfEvalThreads, int maxNumOfNodeThreads, int nodeThreadingThreshold) throws Exception {
		this();
		this.params = params;
		this.trees = trees;
		this.log = log;
		this.nodeScheduler = new ThreadScheduler(maxNumOfNodeThreads);
		this.evalScheduler = new ThreadScheduler(maxNumOfEvalThreads);
		this.nodeThreadingThreshold = nodeThreadingThreshold;
		for(int i=0; i<trees.size(); i++) {
			trees.get(i).setForest(this);
		}
		check();
	}
	
	/**
	 * Creates a blank forest.
	 * 
	 * @param numTrees
	 * @throws Exception 
	 * @throws Exception 
	 */
	public Forest() throws Exception {
		this.trees = new ArrayList<RandomTree>();
	}

	/**
	 * Grows the forest. 
	 * 
	 * @param sampler data provider
	 * @throws Exception
	 */
	public void grow(final Sampler<Dataset> sampler) throws Exception {
		
		for(int i=0; i<trees.size(); i++) {
			System.out.println("Growing tree " + i + " to depth " + params.maxDepth);
			startTime = System.currentTimeMillis();

			trees.get(i).grow(sampler.getSample(), params.maxDepth);
			//trees.get(i).grow((trees.size() == 1) ? sampler : sampler.getSample(), params.maxDepth);
		}
		if (nodeScheduler.getMaxThreads() > 0) {
			// Multithreading is active, so wait for the results
			
			/*
			 * Annotation: One could wait and notify instead of Thread.sleep, but at this point this
			 * is not necessary because this will only be used once at the end of training.
			 */
			
			// Allow the node scheduler to use the (higher) amount of eval threads
			nodeScheduler.setMaxThreads(evalScheduler.getMaxThreads());
			
			System.out.println("Finished main growing procedure, waiting for running node threads...");
			while(true) {
				if (nodeScheduler.getThreadsActive() == 0 && evalScheduler.getThreadsActive() == 0) break;

				System.out.println(
						timeStampFormatter.format(new Date()) + ": Waiting for " + nodeScheduler.getThreadsActive() + " node threads; " + 
						"Heap: " + Math.round((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()) / (1024.0*1024.0)) + " MB"
				);

				try {
					Thread.sleep(4000);
				} catch (InterruptedException e) {
					System.out.println("[Wait interrupted by VM, continuing...]");
				}
			}
			System.out.println("Finished growing forest.");
		}
	}
	
	/**
	 * Classifies a whole 2d array of data values and returns the results, normalized to [0,1].
	 * 
	 * @param data
	 * @return
	 * @throws Exception
	 *
	public float[][][] classify2dM(Object data) throws Exception {
		byte[][][] dataC = (byte[][][])data;
		float[][][] dataForest = new float[dataC.length][dataC[0].length][];
		for(int x=0; x<dataC.length; x++) {
			for(int y=0; y<dataC[0].length; y++) {
				dataForest[x][y] = classify(data, x, y);
			}
		}
		return dataForest;
	}
	
	/**
	 * Returns the probability for event(s) on value x/y. Generic version, usable for any forests.
	 * 
	 * @param data
	 * @param x
	 * @param y
	 * @return
	 * @throws Exception 
	 */
	public float[] classify(final Object data, final int x, final int y) throws Exception {
		int numOfClasses = trees.get(0).getNumOfClasses();
		float[] ret = new float[numOfClasses];
		for(int i=0; i<trees.size(); i++) {
			float[] cl = trees.get(i).classify(data, x, y);
			for(int c=0; c<numOfClasses; c++) {
				ret[c] += cl[c]; 
			}
		}
		for(int c=0; c<numOfClasses; c++) {
			ret[c] /= (float)trees.size();
		}
		return ret;
	}
	
	/**
	 * Core function for multithreaded classifying.
	 * 
	 * @param data
	 * @param dataForest
	 * @throws Exception 
	 */
	public void classifyThreaded(Worker worker, byte[][] data, float[][][] dataForest, int start, int end) throws Exception {
		for(int x=0; x<data.length; x++) {
			if (worker != null && x%20 == 0) worker.setProgress((double)x/data.length);
			for(int y=start; y<=end; y++) {
				dataForest[x][y] = classify(data, x, y);
			}
		}
	}
	
	/**
	 * Saves the trained forest parameters to disk. Each tree is stored in one file.
	 * 
	 * @param file
	 * @throws Exception 
	 */
	public void save(final String filename) throws Exception {
		for(int i=0; i<trees.size(); i++) {
			trees.get(i).save(filename + i);
		}
	}
	
	/**
	 * Loads a trained forest from file. Each tree is stored in one file.
	 * 
	 * @param path forest data is represented by path + tree index (starting from 0). Loading
	 *        breaks when an index file does not exist.
	 * @return
	 * @throws Exception
	 */
	public void load(final String path, final int numOfClasses, RandomTree factory) throws Exception {
		int i=0;
		System.out.print("Loading trees: ");
		while(true) {
			String fn = path + i;
			if (!(new File(fn)).exists()) break;
			RandomTree tr = factory.getInstance(numOfClasses, i);
			tr.load(fn);
			trees.add(tr);
			System.out.print(i + ", ");
			i++;
		}
		System.out.print("checking, ");
		check();
		System.out.println("done");
	}
	
	/**
	 * Returns the tree files in a directory.
	 * 
	 * @param path forest data is represented by path + tree index (starting from 0). Loading
	 *        breaks when an index file does not exist.
	 * @return
	 * @throws Exception
	 */
	public static List<File> getTreeList(String path) throws Exception {
		int i=0;
		List<File> ret = new ArrayList<File>();
		while(true) {
			File f = new File(path + i);
			if (!f.exists() || !f.isFile()) break;
			ret.add(f);
			i++;
		}
		return ret;
	}
	
	/**
	 * Returns the tree list.
	 * 
	 * @return
	 */
	public List<RandomTree> getTrees() {
		return trees;
	}

	/**
	 * Checks some integrity.
	 * 
	 * @throws Exception
	 */
	protected void check() throws Exception {
		if (trees.size() == 0) throw new Exception("No trees in forest");
		int numOfClasses = -1;
		for(int i=0; i<trees.size(); i++) {
			RandomTree t = (RandomTree)trees.get(i);
			if (numOfClasses < 0) numOfClasses = t.getNumOfClasses();
			if (numOfClasses != t.getNumOfClasses()) throw new Exception("All trees in the forest have to base on the same number of classes");
		}
	}
	
	/**
	 * Save tree stats to Log.
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
			RandomTree t = trees.get(i);
			log.write("Tree " + i + ": " + ana.getNumOfLeafs(t) +  " leafs of possible " + poss + "; Information gain: " + t.getInfoGain());
			log.write("Count nodes at depths:\n" + ana.getDepthCountsString(t));
			//log.write("Tree structure:\n" + ana.getTreeVisualization(t));
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
	 * Returns the main log instance of the forest.
	 * 
	 * @return
	 */
	public Logfile getLog() {
		return log;
	}
	
	/**
	 * Returns the parameters object.
	 * 
	 * @return
	 */
	public ForestParameters getParams() {
		return params;
	}

	/**
	 * Returns the start time of growing.
	 * 
	 * @return
	 */
	public long getStartTime() {
		return startTime;
	}

}
