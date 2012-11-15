package de.lmu.dbs.ciaa.classifier;

import java.text.SimpleDateFormat;
import java.util.List;

import de.lmu.dbs.ciaa.util.Statistic;

/**
 * Base class for trees.
 * 
 * @author Thomas Weber
 *
 */
public abstract class Tree extends Thread {

	/**
	 * Statistic about the trees information gain.
	 */
	public Statistic infoGain;
	
	/**
	 * Index of the tree in its forest.
	 */
	protected int num = -1;
	
	/**
	 * Reference to the parent forest (only set in root tree instances, not in threaded recursion).
	 */
	protected Forest forest;
	
	/**
	 * The actual tree structure
	 */
	protected Node tree = new Node();
	
	/**
	 * The parameter set used to grow the tree
	 */
	protected ForestParameters params = null;

	/**
	 * Log to base 2 for better performance
	 */
	public static final double LOG2 = Math.log(2);
	
	/**
	 * Number of active node threads in this tree, excluding the calling thread.
	 */
	protected int nodeThreadsActive = 0;
	
	/**
	 * Number of active evaluation worker threads in this tree, excluding the calling thread.
	 */
	protected int evaluationThreadsActive = 0;
	
	/**
	 * The attributes with prefix "newThread" are used to transport parameters
	 * to new Threads (see growRec source code)
	 */
	protected Sampler<Dataset> newThreadSampler;

	/**
	 * The attributes with prefix "newThread" are used to transport parameters
	 * to new Threads (see growRec source code)
	 */
	protected List<byte[][]> newThreadClassification;

	/**
	 * Root tree instance, used for multithreading to watch active threads.
	 */
	protected Tree newThreadRoot;
	
	/**
	 * The attributes with prefix "newThread" are used to transport parameters
	 * to new Threads (see growRec source code)
	 */
	protected Node newThreadNode;

	/**
	 * The attributes with prefix "newThread" are used to transport parameters
	 * to new Threads (see growRec source code)
	 */
	protected int newThreadMode;
	
	/**
	 * The attributes with prefix "newThread" are used to transport parameters
	 * to new Threads (see growRec source code)
	 */
	protected int newThreadDepth;
	
	/**
	 * The attributes with prefix "newThread" are used to transport parameters
	 * to new Threads (see growRec source code)
	 */
	protected int newThreadMaxDepth;
	
	/**
	 * Date formatter for debug output.
	 */
	protected SimpleDateFormat timeStampFormatter = new SimpleDateFormat("hh:mm:ss");
	
	/**
	 * Returns the classification of the tree at a given value in data: data[x][y]
	 * 
	 * @param data
	 * @param x
	 * @param y
	 * @return
	 * @throws Exception
	 */
	public abstract float classify(final byte[][] data, final int x, final int y) throws Exception;
		
	/**
	 * Grows the tree. 
	 * <br><br>
	 * Attention: If multithreading is activated, you have to care about 
	 * progress before proceeding with testing etc., i.e. you can use the 
	 * isGrown() method for that purpose.
	 * 
	 * @param sampler contains the whole data to train the tree.
	 * @param mode 0: root node (no preceeding classification), 1: left, 2: right; -1: out of bag
	 * @throws Exception
	 */
	public abstract void grow(final Sampler<Dataset> sampler, final int maxDepth) throws Exception;

	/**
	 * Internal: Grows the tree.
	 * 
	 * @param sampler contains the whole data to train the tree.
	 * @param mode 0: root node (no preceeding classification), 1: left, 2: right; -1: out of bag
	 * @param multithreading this is used to disable the threading part, if called from the run method. 
	 *        Otherwise, an infinite loop would happen with multithreading.
	 * @throws Exception 
	 */
	protected abstract void growRec(Tree root, final Sampler<Dataset> sampler, List<byte[][]> classification, final Node node, final int mode, final int depth, final int maxDepth, boolean multithreading) throws Exception;

	/**
	 * Saves the tree to a file.
	 * 
	 * @param file
	 * @throws Exception
	 */
	public abstract void save(final String filename) throws Exception;
	
	/**
	 * Returns if the tree is grown. For multithreading mode.
	 * 
	 * @return
	 */
	public boolean isGrown() {
		return (nodeThreadsActive == 0) && (evaluationThreadsActive == 0);
	}

	/**
	 * Sets a forest as parent.
	 * 
	 * @param forest
	 */
	public void setForest(Forest forest) {
		this.forest = forest;
	}
	
	/**
	 * Wraps the growRec() method for multithreaded tree growing, using the 
	 * instance attributes postfixed with "newThread".
	 * Represents an "anonymous" RandomTree instance to wrap the growRec method. 
	 * Results have to be watched with the isGrown method of the original RandomTree instance.
	 * 
	 */
	public void run() {
		try {
			if (params.debugThreadForking) System.out.println("T" + newThreadRoot.num + ": --> Forking new thread at depth " + newThreadDepth);
			
			growRec(newThreadRoot, newThreadSampler, newThreadClassification, newThreadNode, newThreadMode, newThreadDepth, newThreadMaxDepth, false);
			newThreadRoot.decThreadsActive();
			
			if (params.debugThreadForking) System.out.println("T" + newThreadRoot.num + ": <-- Thread at depth " + newThreadDepth + " released.");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	/**
	 * Returns the amount of active threads for this tree.
	 * 
	 * @return
	 * @throws Exception 
	 */
	public synchronized int getThreadsActive() throws Exception {
		return nodeThreadsActive;
	}
	
	/**
	 * Decreases the thread counter for this tree.
	 * 
	 * @throws Exception
	 */
	protected synchronized void incThreadsActive() throws Exception {
		nodeThreadsActive++;
		if (nodeThreadsActive > params.maxNumOfNodeThreads) throw new Exception("Thread amount above maximum of " + params.maxNumOfNodeThreads + ": " + nodeThreadsActive);
	}

	/**
	 * Increases the thread counter for this tree.
	 * 
	 * @throws Exception
	 */
	protected synchronized void decThreadsActive() throws Exception {
		nodeThreadsActive--;
		if (nodeThreadsActive < 0) throw new Exception("Thread amount below zero: " + nodeThreadsActive);
	}

	/**
	 * Returns a info gain statistic instance.
	 * 
	 * @return
	 */
	public Statistic getInfoGain() {
		return infoGain;
	}

	/**
	 * Returns the root node of the tree.
	 * 
	 * @return
	 */
	public Node getRootNode() {
		return tree;
	}

}
