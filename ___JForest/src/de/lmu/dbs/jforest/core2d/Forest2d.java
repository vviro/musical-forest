package de.lmu.dbs.jforest.core2d;

import java.util.List;

import de.lmu.dbs.jforest.core.ClassificationWorker;
import de.lmu.dbs.jforest.core.ClassificationWorkerGroup;
import de.lmu.dbs.jforest.core.Forest;
import de.lmu.dbs.jforest.core.ForestParameters;
import de.lmu.dbs.jforest.core.RandomTree;
import de.lmu.dbs.jforest.util.Logfile;
import de.lmu.dbs.jforest.util.workergroup.ThreadScheduler;

/**
 * Forest for 2-dimensional data structures.
 * 
 * @author Thomas Weber
 *
 */
public class Forest2d extends Forest {

	/**
	 * 
	 * @throws Exception
	 */
	public Forest2d() throws Exception {
		super();
	}
	
	/**
	 * 
	 * @param trees
	 * @param params
	 * @param log
	 * @param maxNumOfEvalThreads
	 * @param maxNumOfNodeThreads
	 * @param nodeThreadingThreshold
	 * @throws Exception
	 */
	public Forest2d(List<RandomTree> trees, ForestParameters params, Logfile log, int maxNumOfEvalThreads, int maxNumOfNodeThreads, int nodeThreadingThreshold) throws Exception {
		super(trees, params, log, maxNumOfEvalThreads, maxNumOfNodeThreads, nodeThreadingThreshold);
	}

	/**
	 * Classifies a whole 2d array of data values and returns the results, normalized to [0,1].
	 * 
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public float[][][] classify2d(Object data) throws Exception {
		return classify2d(data, 1, false, -1);
	}
	
	/**
	 * Classifies a whole 2d array of data values and returns the results, normalized to [0,1],
	 * multithreaded if numOfThreads > 1.
	 * 
	 * @param dataO
	 * @return
	 * @throws Exception
	 */
	public float[][][] classify2d(Object dataO, int numOfThreads, boolean verbose, int maxDepth) throws Exception {
		check();	
		byte[][] data = (byte[][])dataO;
		float[][][] dataForest = new float[data.length][data[0].length][];
		int numOfWork = data[0].length;

		// No multithreading
		if (numOfThreads <= 1) {
			if (verbose) System.out.println("No multithreading in classification, too few threads: " + numOfThreads);
			classifyThreaded(null, data, dataForest, 0, numOfWork-1, maxDepth);
			return dataForest;
		}
		
		// Checks
		if (numOfWork < numOfThreads) throw new Exception("Too few work for classification threads: " + data.length);
		
		// Multithreading
		ThreadScheduler ts = new ThreadScheduler(numOfThreads);
		synchronized(ts) {
			ClassificationWorkerGroup group = new ClassificationWorkerGroup(ts, numOfWork, THREAD_POLLING_INTERVAL, true);
			for(int i=0; i<numOfThreads; i++) {
				ClassificationWorker worker = new ClassificationWorker(group, this, data, dataForest, maxDepth);
				group.add(worker);
			}
			group.runGroup();
		}

		return dataForest;
	}
}
