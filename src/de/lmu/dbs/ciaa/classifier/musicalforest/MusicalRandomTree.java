package de.lmu.dbs.ciaa.classifier.musicalforest;

import java.util.List;

import de.lmu.dbs.ciaa.classifier.core.*;
import de.lmu.dbs.ciaa.classifier.core2d.RandomTree2d;
import de.lmu.dbs.ciaa.util.Logfile;

/**
 * Random tree implementation for f0 detection on 2-dimensional data (FFT/CQT spectrum etc.).
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
		super(params, 2, num, log);
	}

	/**
	 * Creates a tree instance for recursion into a new thread. The arguments are just used to transport
	 * the arguments of growRec to the new thread. See method growRec source code.
	 * 
	 * @throws Exception 
	 * 
	 */
	public MusicalRandomTree(ForestParameters params, RandomTree root, Sampler<Dataset> sampler, List<Classification> classification, long count, Node node, int mode, int depth, int maxDepth) throws Exception {
		super(root, sampler, classification, count, node, mode, depth, maxDepth);
	}

	/**
	 * Creates a blank tree, used as a factory.
	 * 
	 * @throws Exception
	 */
	public MusicalRandomTree() throws Exception {
		super();
	}

	/**
	 * Provides the possibility to add tree specific log output per node.
	 * 
	 * @param pre
	 * @param countClassesLeft
	 * @param countClassesRight
	 * @param winner
	 * @param winnerThreshold
	 * @throws Exception
	 */
	@Override
	public void logAdditional(String pre, long[][][] countClassesLeft, long[][][] countClassesRight, int winner, int winnerThreshold) throws Exception {
		long silenceLeftW = countClassesLeft[winner][winnerThreshold][1]; 
		long noteLeftW = countClassesLeft[winner][winnerThreshold][0];
		long silenceRightW = countClassesRight[winner][winnerThreshold][1]; 
		long noteRightW = countClassesRight[winner][winnerThreshold][0];
		log.write(pre + "Left note: " + noteLeftW + ", silence: " + silenceLeftW + ", sum: " + (silenceLeftW+noteLeftW));
		log.write(pre + "Right note: " + noteRightW + ", silence: " + silenceRightW + ", sum: " + (silenceRightW+noteRightW));
	}

}
