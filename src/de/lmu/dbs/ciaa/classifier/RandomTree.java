package de.lmu.dbs.ciaa.classifier;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import de.lmu.dbs.ciaa.classifier.features.*;
import de.lmu.dbs.ciaa.util.Log;
import de.lmu.dbs.ciaa.util.Statistic;

/**
 * Represents one random decision tree, with pixelwise classification.
 * 
 * @author Thomas Weber
 *
 */
public class RandomTree extends Tree {

	/**
	 * Creates a tree instance for recursion into a new thread. The arguments are just used to transport
	 * the arguments of growRec to the new thread. See method growRec source code.
	 * 
	 * @throws Exception 
	 * 
	 */
	public RandomTree(ForestParameters params, Tree root, Sampler<Dataset> sampler, List<byte[][]> classification, Node node, int mode, int depth, int maxDepth) throws Exception {
		this(params, -1);
		this.newThreadRoot = root;
		this.newThreadSampler = sampler;
		this.newThreadClassification = classification;
		this.newThreadNode = node;
		this.newThreadMode = mode;
		this.newThreadDepth = depth;
		this.newThreadMaxDepth = maxDepth;
	}
	
	/**
	 * Creates a tree (main constructor).
	 * 
	 * @throws Exception 
	 * 
	 */
	public RandomTree(ForestParameters params, int num) throws Exception {
		params.check();
		this.params = params;
		this.num = num;
		this.infoGain = new Statistic();
	}
	
	/**
	 * Creates a tree. Blank, for loading classifier data from file.
	 * 
	 */
	public RandomTree() {
	}

	/**
	 * Returns the classification of the tree at a given value in data: data[x][y]
	 * 
	 * @param data
	 * @param x
	 * @param y
	 * @return
	 * @throws Exception
	 */
	public float classify(final byte[][] data, final int x, final int y) throws Exception {
		return classifyRec(data, tree, x, y);
	}
	
	/**
	 * Internal classification method.
	 * 
	 * @param data
	 * @param node current node to process
	 * @param x
	 * @param y
	 * @return
	 * @throws Exception
	 */
	protected float classifyRec(final byte[][] data, final Node node, final int x, final int y) throws Exception {
		if (node.isLeaf()) {
			return node.probabilities[y];
		} else {
			if (node.feature.evaluate(data, x, y) >= node.feature.threshold) {
				return classifyRec(data, node.left, x, y);
			} else {
				return classifyRec(data, node.right, x, y);
			}
		}
	}
	
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
	public void grow(final Sampler<Dataset> sampler, final int maxDepth) throws Exception {
		// Get random value selection initially
		List<byte[][]> classification = new ArrayList<byte[][]>(); // Classification arrays for each dataset in the sampler, same index
		if (params.percentageOfRandomValuesPerFrame < 1.0) {
			// Drop some of the values by classifying them to -1
			int vpf = (int)(params.percentageOfRandomValuesPerFrame * params.frequencies.length);
			long[] array = sampler.get(0).getRandomValuesArray(vpf);
			for(int i=0; i<sampler.getPoolSize(); i++) {
				classification.add(sampler.get(i).selectRandomValues(0, vpf, array));
			}
		} else {
			// Set all zero classification
			for(int i=0; i<sampler.getPoolSize(); i++) {
				classification.add(new byte[sampler.get(i).getSpectrum().length][sampler.get(i).getSpectrum()[0].length]);
			}
		}
		growRec(this, sampler, classification, tree, 0, 0, maxDepth, true);
	}

	/**
	 * Internal: Grows the tree.
	 * 
	 * @param sampler contains the whole data to train the tree.
	 * @param mode 0: root node (no preceeding classification), 1: left, 2: right; -1: out of bag
	 * @param multithreading this is used to disable the threading part, if called from the run method. 
	 *        Otherwise, an infinite loop would happen with multithreading.
	 * @throws Exception 
	 */
	protected void growRec(Tree root, final Sampler<Dataset> sampler, List<byte[][]> classification, final Node node, final int mode, final int depth, final int maxDepth, boolean multithreading) throws Exception {
		if (params.maxNumOfNodeThreads > 0) {
			synchronized (root.forest) { 
				if (multithreading && (root.forest.getThreadsActive() < params.maxNumOfNodeThreads)) {
					// Start an "anonymous" RandomTree instance to calculate this method. Results have to be 
					// watched with the isGrown method of the original RandomTree instance.
					Tree t = new RandomTree(params, root, sampler, classification, node, mode, depth, maxDepth);
					root.incThreadsActive();
					t.start();
					return;
				}
			}
		}
		
		// Debug
		String pre = "T" + root.num + ":   ";
		if (params.logProgress) {
			for(int i=0; i<depth; i++) pre+= "-  ";
			switch (mode) {
				case 0: {
					Log.write(pre + "Root, Depth " + depth);
					break;
				}
				case 1: {
					Log.write(pre + "L, Depth " + depth);
					break;
				}
				case 2: {
					Log.write(pre + "R, Depth " + depth);
					break;
				}
			}
		}

		// Leaf: Calculate probabilities
		if (depth >= maxDepth) {
			node.probabilities = calculateLeaf(sampler, classification, mode, depth);
			return;
		}
		
		// Get random feature parameter sets
		List<Feature> paramSet = params.featureFactory.getRandomFeatureSet(params);

		int numOfFeatures = paramSet.size();

		long[] silenceLeft = new long[paramSet.size()];
		long[] noteLeft = new long[paramSet.size()];
		long[] silenceRight = new long[paramSet.size()];
		long[] noteRight = new long[paramSet.size()];
		
		int poolSize = sampler.getPoolSize();
		//List<RandomTreeWorker> workers = null;
		//if (params.evaluationThreadsLimit >= depth && params.maxNumOfEvaluationThreads > 0) workers = new ArrayList<RandomTreeWorker>();
		for(int i=0; i<poolSize; i++) {

			// Each dataset...load spectral data and midi
			Dataset dataset = sampler.get(i);
			byte[][] data = dataset.getSpectrum();
			byte[][] midi = dataset.getMidi();
			byte[][] cla = classification.get(i);
			
			// get feature results and split data
			for(int x=0; x<data.length; x++) {
				for(int y=0; y<params.frequencies.length; y++) {
					// Each random value from the subframe
					if (mode == cla[x][y]) { // Is that point in the training set for this node?
						for(int k=0; k<numOfFeatures; k++) {
							// Each featureset candidate...
							Feature feature = paramSet.get(k);
							
							if (feature.evaluate(data, x, y) >= feature.threshold) {
								// Left
								if (midi[x][y] > 0) {
									noteLeft[k]++;
								} else {
									silenceLeft[k]++;
								}
							} else {
								// Right
								if (midi[x][y] > 0) {
									noteRight[k]++;
								} else {
									silenceRight[k]++;
								}
							}
						}
					}
				}
			}
		}

		// Calculate shannon entropy for all parameter sets to get the best set
		double[] gain = new double[paramSet.size()];
		long note_ = 0;
		long silence_ = 0;
		for(int i=0; i<paramSet.size(); i++) {
			long note = noteLeft[i] + noteRight[i];
			long silence = silenceLeft[i] + silenceRight[i];
			if (i>0) {
				// TMP check integrity
				if (note_ != note) throw new Exception("Note: " + note_ + " != " + note);
				if (silence_ != silence) throw new Exception("Silence: " + silence_ + " != " + silence);
			}
			silence_ = silence;
			note_ = note;
			double entropyAll = getEntropy(note, silence);
			double entropyLeft = getEntropy(noteLeft[i], silenceLeft[i]);
			double entropyRight = getEntropy(noteRight[i], silenceRight[i]);
			gain[i] = entropyAll - ((double)(noteLeft[i]+silenceLeft[i])/(note+silence))*entropyLeft - ((double)(noteRight[i]+silenceRight[i])/(note+silence))*entropyRight;
			//System.out.println(pre + "Gain " + i + ": " + gain[i] + " thr: " + paramSet.get(i).threshold);
		}
		double max = Double.MIN_VALUE;
		int winner = 0;
		for(int i=0; i<paramSet.size(); i++) {
			if(gain[i] > max) {
				max = gain[i];
				winner = i;
			}
		}
		// See in info gain is sufficient:
		if (gain[winner] >= params.entropyThreshold) {
			// Yes, save feature and continue recursion
			node.feature = paramSet.get(winner);
		} else {
			// No, make leaf and return
			node.probabilities = calculateLeaf(sampler, classification, mode, depth);
			return;
		}
		
		
		// Split values by winner feature for deeper branches
		List<byte[][]> classificationNext = new ArrayList<byte[][]>(sampler.getPoolSize());
		for(int i=0; i<poolSize; i++) {
			Dataset dataset = sampler.get(i);
			byte[][] data = dataset.getSpectrum();
			byte[][] cla = classification.get(i);
			byte[][] claNext = new byte[data.length][params.frequencies.length];
			for(int x=0; x<data.length; x++) {
				for(int y=0; y<params.frequencies.length; y++) {
					if (mode == cla[x][y]) {
						if (node.feature.evaluate(data, x, y) >= node.feature.threshold) {
							claNext[x][y] = 1; // Left
						} else {
							claNext[x][y] = 2; // Right
						}
					}
				}
			}
			classificationNext.add(claNext);
		}
		
		// Debug //////////////////////////////////////////
		root.infoGain.add(gain[winner]);
		if (params.logNodeInfo) {
			Log.write(pre + "Winner: " + winner + "; Information gain: " + gain[winner] + " Threshold: " + paramSet.get(winner).threshold);
			Log.write(pre + "Left: " + silenceLeft[winner] + " + " + noteLeft[winner] + " = " + (silenceLeft[winner]+noteLeft[winner]));
			Log.write(pre + "Right: " + noteRight[winner] + " + " + silenceRight[winner] + " = " + (noteRight[winner]+silenceRight[winner]));
			Log.write(pre + "Amount of counted samples: " + (silenceLeft[winner]+noteLeft[winner]+noteRight[winner]+silenceRight[winner]));
		}
		//////////////////////////////////////////////////
		
		// Recursion to left and right
		node.left = new Node();
		growRec(root, sampler, classificationNext, node.left, 1, depth+1, maxDepth, true);

		node.right = new Node();
		growRec(root, sampler, classificationNext, node.right, 2, depth+1, maxDepth, true);
	}
	
	/**
	 * Calculates leaf probabilities.
	 * 
	 * @param sampler
	 * @param mode
	 * @param depth
	 * @return
	 * @throws Exception
	 */
	protected float[] calculateLeaf(final Sampler<Dataset> sampler, List<byte[][]> classification, final int mode, final int depth) throws Exception {
		float[] ret = new float[params.frequencies.length];
		float maxP = Float.MIN_VALUE;
		// Collect inverse
		for(int i=0; i<sampler.getPoolSize(); i++) {
			Dataset dataset = sampler.get(i);
			byte[][] midi = dataset.getMidi();
			byte[][] cla = classification.get(i);
			
			for(int x=0; x<midi.length; x++) {
				for(int y=0; y<midi[0].length; y++) {
					if (mode == cla[x][y]) { 
						if (midi[x][y] == 0) { // Inverse
							// f0 is (not) present
							ret[y]++;
							if (ret[y] > maxP) maxP = ret[y];
						}
					}
				}
			}
		}
		// Invert back and normalize
		for(int i=0; i<ret.length; i++) {
			ret[i] = (maxP - ret[i]) / maxP; 
		}
		return ret;
	}
	
	/**
	 * Calculates shannon entropy for binary alphabet (two possible values),  
	 * while a and b represent the count of each of the two "letters". 
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static double getEntropy(final long a, final long b) {
		long all = a+b;
		if(all <= 0) return 0;
		double pa = (double)a/all;
		double pb = (double)b/all;
		return - pa * (Math.log(pa)/LOG2) - pb * (Math.log(pb)/LOG2);
	}
	
	/**
	 * Saves the tree to a file.
	 * 
	 * @param file
	 * @throws Exception
	 */
	public void save(final String filename) throws Exception {
		FileOutputStream fout = new FileOutputStream(filename);
		ObjectOutputStream oos = new ObjectOutputStream(fout);   
		oos.writeObject(tree);
		oos.close();
	}
	
	/**
	 * Loads a tree from file and returns it.
	 * 
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	public static RandomTree load(final String filename) throws Exception {
		FileInputStream fin = new FileInputStream(filename);
		ObjectInputStream ois = new ObjectInputStream(fin);
		RandomTree ret = new RandomTree();
		ret.tree = (Node)ois.readObject();
		ois.close();
		return ret;
	}
	
}
