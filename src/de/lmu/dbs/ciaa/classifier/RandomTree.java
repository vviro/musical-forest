package de.lmu.dbs.ciaa.classifier;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import de.lmu.dbs.ciaa.classifier.features.*;

/**
 * Represents one random decision tree.
 * 
 * @author Thomas Weber
 *
 */
public class RandomTree {

	/**
	 * The actual tree structure
	 */
	protected Node tree = new Node();
	
	/**
	 * The parameter set used to grow the tree
	 */
	protected RandomTreeParameters params = null;

	/**
	 * Log to base 2 for better performance
	 */
	public static final double LOG2 = Math.log(2);
	
	/**
	 * Creates a tree.
	 * 
	 * @throws Exception 
	 * 
	 */
	public RandomTree(RandomTreeParameters params) throws Exception {
		params.check();
		this.params = params;
	}
	
	/**
	 * Creates a tree.
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
	public long classify(byte[][] data, int x, int y) throws Exception {
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
	protected long classifyRec(byte[][] data, Node node, int x, int y) throws Exception {
		if (node.isLeaf()) {
			//System.out.println("Leaf");
			return node.probabilities[y];
		} else {
			if (node.feature.evaluate(data, x, y) >= node.feature.threshold) {
				//System.out.println("L");
				return classifyRec(data, node.left, x, y);
			} else {
				//System.out.println("R");
				return classifyRec(data, node.right, x, y);
			}
		}
	}
	
	/**
	 * Calculates shannon entropy for binary alphabet (two possible values),  
	 * while a and b represent the count of each of the two "letters". 
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	protected double getEntropy(long a, long b) {
		long all = a+b;
		if(all <= 0) return 0;
		double pa = (double)a/all;
		double pb = (double)b/all;
		return - pa * (Math.log(pa)/LOG2) - pb * (Math.log(pb)/LOG2);
	}
	
	
	/**
	 * Grows the tree.
	 * 
	 * @param sampler contains the whole data to train the tree.
	 * @param mode 0: root node (no preceeding classification), 1: left, 2: right; -1: out of bag
	 * @throws Exception
	 */
	public void grow(final Sampler<Dataset> sampler, final int maxDepth) throws Exception {
		// Get random value selection initially
		if (params.percentageOfRandomValuesPerFrame < 1.0) {
			System.out.println("Generate random values...");
			int vpf = (int)(params.percentageOfRandomValuesPerFrame * params.frequencies.length);
			long[] array = sampler.get(0).getRandomValuesArray(vpf);
			for(int i=0; i<sampler.getPoolSize(); i++) {
				sampler.get(i).selectRandomValues(0, vpf, array);
			}
		}
		growRec(sampler, tree, 0, 0, maxDepth);
	}

	/**
	 * Internal: Grows the tree.
	 * 
	 * @param sampler contains the whole data to train the tree.
	 * @param mode 0: root node (no preceeding classification), 1: left, 2: right; -1: out of bag
	 * @throws Exception 
	 */
	protected void growRec(final Sampler<Dataset> sampler, final Node node, final int mode, final int depth, final int maxDepth) throws Exception {
		/*
		String pre = "";
		for(int i=0; i<depth; i++) pre+= "   ";
		//*/

		if (depth >= maxDepth) {
			// Leaf: Calculate probabilities
			node.probabilities = calculateLeaf(sampler, mode, depth);
			//System.out.println(pre + "Calculated leaf probabilities.");
			return;
		}
		
		// Get random feature parameter sets
		List<Feature> paramSet = FeatureKinect5.getRandomFeatureSet(params);
		int numOfFeatures = paramSet.size();

		long[] silenceLeft = new long[paramSet.size()];
		long[] noteLeft = new long[paramSet.size()];
		long[] silenceRight = new long[paramSet.size()];
		long[] noteRight = new long[paramSet.size()];
		
		int poolSize = sampler.getPoolSize();
		for(int i=0; i<poolSize; i++) {
			// Each dataset...load spectral data and midi
			//System.out.println(pre + "--> Dataset " + i);
			Dataset dataset = sampler.get(i);
			byte[][] data = dataset.getSpectrum();
			byte[][] midi = dataset.getMidi();
			byte[][] cla = dataset.getClassificationArray(depth);
			
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
		//System.out.println(pre + "Compute Entropy...");
		double[] gain = new double[paramSet.size()];
		for(int i=0; i<paramSet.size(); i++) {
			long note = noteLeft[i] + noteRight[i];
			long silence = silenceLeft[i] + silenceRight[i];
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
		node.feature = paramSet.get(winner); 
		
		// Split values by winner feature for deeper branches
		for(int i=0; i<poolSize; i++) {
			//System.out.println("--> Dataset " + i);
			Dataset dataset = sampler.get(i);
			byte[][] data = dataset.getSpectrum();
			byte[][] cla = dataset.getClassificationArray(depth);
			byte[][] claNext = dataset.getClassificationArray(depth+1);
			
			for(int x=0; x<data.length; x++) {
				for(int y=0; y<params.frequencies.length; y++) {
					if (mode == cla[x][y]) {
						if (node.feature.evaluate(data, x, y) >= node.feature.threshold) {
							//dataset.setClassification(depth+1, x, y, 1);
							claNext[x][y] = 1; // Left
						} else {
							//dataset.setClassification(depth+1, x, y, 2);
							claNext[x][y] = 2; // Right
						}
					}
				}
			}
		}
		
		// Debug //////////////////////////////////////////
		/*System.out.println(pre + "Winner: " + winner + "; Information gain: " + gain[winner] + " Threshold: " + paramSet.get(winner).threshold);
		System.out.println(pre + "Left: " + silenceLeft[winner] + " + " + noteLeft[winner] + " = " + silenceLeft[winner]+noteLeft[winner]);
		System.out.println(pre + "Right: " + noteRight[winner] + " + " + silenceRight[winner] + " = " + noteRight[winner]+silenceRight[winner]);
		//*/
		//////////////////////////////////////////////////
		
		// Recursion to left and right
		node.left = new Node();
		//System.out.println(pre + "Recurse left to depth " + (depth+1) + "...");
		growRec(sampler, node.left, 1, depth+1, maxDepth);

		node.right = new Node();
		//System.out.println(pre + "Recurse right to depth " + (depth+1) + "...");
		growRec(sampler, node.right, 2, depth+1, maxDepth);
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
	protected int[] calculateLeaf(Sampler<Dataset> sampler, int mode, int depth) throws Exception {
		int[] ret = new int[params.frequencies.length];
		int maxP = Integer.MIN_VALUE;
		for(int i=0; i<sampler.getPoolSize(); i++) {
			Dataset dataset = sampler.get(i);
			byte[][] midi = dataset.getMidi();
			byte[][] cla = dataset.getClassificationArray(depth);
			
			for(int x=0; x<midi.length; x++) {
				for(int y=0; y<midi[0].length; y++) {
					if (mode == cla[x][y]) { 
						if (midi[x][y] == 0) { ///////////////////////////////// Inverse!
							// f0 is (not) present
							ret[y]++;
							if (ret[y] > maxP) maxP = ret[y];
						}
					}
				}
			}
		}
		/*for(int i=0; i<node.probabilities.length; i++) {
			if (node.probabilities[i] > 0) System.out.println("i: " + node.probabilities[i]);
		}
		//*/
		return ret;
	}
	
	/**
	 * Saves the tree to a file.
	 * 
	 * @param file
	 * @throws Exception
	 */
	public void save(String filename) throws Exception {
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
	public static RandomTree load(String filename) throws Exception {
		FileInputStream fin = new FileInputStream(filename);
		ObjectInputStream ois = new ObjectInputStream(fin);
		RandomTree ret = new RandomTree();
		ret.tree = (Node)ois.readObject();
		ois.close();
		return ret;
	}

}
