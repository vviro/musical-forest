package de.lmu.dbs.ciaa.classifier.musicalforest;

import java.util.List;

import de.lmu.dbs.ciaa.classifier.Dataset;
import de.lmu.dbs.ciaa.classifier.ForestParameters;
import de.lmu.dbs.ciaa.classifier.Sampler;
import de.lmu.dbs.ciaa.classifier.core2d.Node2d;
import de.lmu.dbs.ciaa.classifier.core2d.RandomTree2d;
import de.lmu.dbs.ciaa.classifier.core2d.Tree2d;
import de.lmu.dbs.ciaa.util.Logfile;

/**
 * 2d random tree implementation for f0 detection.
 * 
 * @author Thomas Weber
 *
 */
public class MusicalRandomTree extends RandomTree2d {

	/**
	 * 
	 * @param params
	 * @param num
	 * @param log
	 * @throws Exception
	 */
	public MusicalRandomTree(ForestParameters params, int num, Logfile log) throws Exception {
		super(params, num, log);
	}

	/**
	 * Creates a tree instance for recursion into a new thread. The arguments are just used to transport
	 * the arguments of growRec to the new thread. See method growRec source code.
	 * 
	 * @throws Exception 
	 * 
	 */
	public MusicalRandomTree(ForestParameters params, Tree2d root, Sampler<Dataset> sampler, List<byte[][]> classification, long count, Node2d node, int mode, int depth, int maxDepth, int num, Logfile log) throws Exception {
		super(params, root, sampler, classification, count, node, mode, depth, maxDepth, num, log);
	}

	/**
	 * Creates a blank tree, used as a factory.
	 * 
	 * @throws Exception
	 */
	public MusicalRandomTree() throws Exception {
		super(null, -1, null);
	}

	/**
	 * Returns the number of classification classes.
	 * TODO: Only 2 classes supported now, make multiclass
	 * 
	 * @return
	 */
	public int getNumOfClasses() {
		return 2;
	}

	/**
	 * Returns the present classes at a cartain point. Here: class 0 is
	 * silence, class 1 is f0.
	 * 
	 * @param refdata
	 * @param x
	 * @param y
	 * @return
	 */
	public void reference(boolean[] ret, byte[][] refdata, int x, int y) {
		//boolean[] ret = new boolean[2];
		ret[1] = (refdata[x][y] > 0); // ? 1 : 0;
		ret[0] = !ret[1]; //(refdata[x][y] > 0) ? 0 : 1;
		//return ret;
	}

	/**
	 * Returns a new instance of the tree.
	 * 
	 * @param params
	 * @param root
	 * @param sampler
	 * @param classification
	 * @param count
	 * @param node
	 * @param mode
	 * @param depth
	 * @param maxDepth
	 * @param num
	 * @param log
	 * @return
	 * @throws Exception 
	 */
	@Override
	public RandomTree2d getInstance(ForestParameters params, Tree2d root, Sampler<Dataset> sampler, List<byte[][]> classification, long count, Node2d node, int mode, int depth, int maxDepth, int num, Logfile log) throws Exception {
		return new MusicalRandomTree(params, root, sampler, classification, count, node, mode, depth, maxDepth, num, log);
	}

	/**
	 * Returns a new instance of the tree.
	 * 
	 */
	@Override
	public RandomTree2d getInstance(ForestParameters params, int num, Logfile log) throws Exception {
		return new MusicalRandomTree(params, num, log);
	}
}
