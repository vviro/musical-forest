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
	public RandomTree(ForestParameters params, Tree root, Sampler<Dataset> sampler, List<byte[][]> classification, Node node, int mode, int depth, int maxDepth, double noteRatio) throws Exception {
		this(params, -1);
		this.newThreadRoot = root;
		this.newThreadSampler = sampler;
		this.newThreadClassification = classification;
		this.newThreadNode = node;
		this.newThreadMode = mode;
		this.newThreadDepth = depth;
		this.newThreadMaxDepth = maxDepth;
		this.newThreadNoteRatio = noteRatio;
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
		return classifyRec(data, tree, 0, x, y);
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
	protected float classifyRec(final byte[][] data, final Node node, int mode , final int x, final int y) throws Exception {
		if (node.isLeaf()) {
			return node.probability; //ies[y]; //(mode==1) ? data[x][y] : 0; //
		} else {
			if (node.feature.evaluate(data, x, y) >= node.feature.threshold) {
				return classifyRec(data, node.left, 1, x, y);
			} else {
				return classifyRec(data, node.right, 2, x, y);
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
		double noteRatio = 0;
		for(int i=0; i<sampler.getPoolSize(); i++) {
			Dataset d = sampler.get(i);
			noteRatio += d.getRatio();
		}
		noteRatio/=sampler.getPoolSize();
		growRec(this, sampler, classification, tree, 0, 0, maxDepth, true, noteRatio);
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
	protected void growRec(Tree root, final Sampler<Dataset> sampler, List<byte[][]> classification, final Node node, final int mode, final int depth, final int maxDepth, boolean multithreading, double noteRatio) throws Exception {
		if (params.maxNumOfNodeThreads > 0) {
			synchronized (root.forest) { 
				if (multithreading && (root.forest.getThreadsActive() < params.maxNumOfNodeThreads)) {
					// Start an "anonymous" RandomTree instance to calculate this method. Results have to be 
					// watched with the isGrown method of the original RandomTree instance.
					Tree t = new RandomTree(params, root, sampler, classification, node, mode, depth, maxDepth, noteRatio);
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
			//node.probabilities = calculateLeaf(sampler, classification, mode, depth);
			node.probability = calculateLeaf2(sampler, classification, mode, depth);
			if (params.logNodeInfo) Log.write(pre + " Mode " + mode + " leaf; Probability " + node.probability);
			return;
		}
		
		// Get random feature parameter sets
		List<Feature> paramSet = params.featureFactory.getRandomFeatureSet(params);
		int numOfFeatures = paramSet.size();

		// Generate random thresholds for each feature param set
		float[][] thresholds = new float[numOfFeatures][params.thresholdCandidatesPerFeature];
		for(int i=0; i<thresholds.length; i++) {
			for(int k=0; k<params.thresholdCandidatesPerFeature; k++) {
				thresholds[i][k] = (float)(Math.random() * params.featureFactory.getMaxValue());
			}
		}
		

		long[][] silenceLeft = new long[numOfFeatures][params.thresholdCandidatesPerFeature];
		long[][] noteLeft = new long[numOfFeatures][params.thresholdCandidatesPerFeature];
		long[][] silenceRight = new long[numOfFeatures][params.thresholdCandidatesPerFeature];
		long[][] noteRight = new long[numOfFeatures][params.thresholdCandidatesPerFeature];
		
		int poolSize = sampler.getPoolSize();
		//List<RandomTreeWorker> workers = null;
		//if (params.evaluationThreadsLimit >= depth && params.maxNumOfEvaluationThreads > 0) workers = new ArrayList<RandomTreeWorker>();
		for(int i=0; i<poolSize; i++) {

			// Each dataset...load spectral data and midi
			Dataset dataset = sampler.get(i);
			byte[][] data = dataset.getSpectrum();
			byte[][] midi = dataset.getMidi();
			byte[][] cla = classification.get(i);
			
			// get feature results 
			for(int x=0; x<data.length; x++) {
				for(int y=0; y<params.frequencies.length; y++) {
					// Each random value from the subframe
					if (mode == cla[x][y]) { // Is that point in the training set for this node?
						for(int k=0; k<numOfFeatures; k++) {
							// Each featureset candidate...
							Feature feature = paramSet.get(k);
							float ev = feature.evaluate(data, x, y);
							for(int g=0; g<params.thresholdCandidatesPerFeature; g++) {
								if (ev >= thresholds[k][g]) {
									// Left
									if (midi[x][y] > 0) {
										noteLeft[k][g]++;
									} else {
										silenceLeft[k][g]++;
									}
								} else {
									// Right
									if (midi[x][y] > 0) {
										noteRight[k][g]++;
									} else {
										silenceRight[k][g]++;
									}
								}
							}
						}
					}
				}
			}
		}

		// Calculate shannon entropy for all parameter sets to get the best set
		double[][] gain = new double[paramSet.size()][params.thresholdCandidatesPerFeature];
		for(int i=0; i<numOfFeatures; i++) {
			for(int j=0; j<params.thresholdCandidatesPerFeature; j++) {
				long note = noteLeft[i][j] + noteRight[i][j];
				long silence = silenceLeft[i][j] + silenceRight[i][j];
				double entropyAll = getEntropy((long)(note/noteRatio), silence);
				double entropyLeft = getEntropy((long)(noteLeft[i][j]/noteRatio), silenceLeft[i][j]);
				double entropyRight = getEntropy((long)(noteRight[i][j]/noteRatio), silenceRight[i][j]);
				gain[i][j] = entropyAll - ((double)(noteLeft[i][j]+silenceLeft[i][j])/(note+silence))*entropyLeft - ((double)(noteRight[i][j]+silenceRight[i][j])/(note+silence))*entropyRight;
				//System.out.println(pre + "Gain " + i + ": " + gain[i] + " thr: " + paramSet.get(i).threshold);
				// */
				/*
				long right = noteLeft[i][j] + silenceRight[i][j];
				long wrong = silenceLeft[i][j] + noteRight[i][j];
				double entropyAll = getEntropy(right, wrong);
				double entropyLeft = getEntropy(noteLeft[i][j], silenceLeft[i][j]);
				double entropyRight = getEntropy(noteRight[i][j], silenceRight[i][j]);
				gain[i][j] = entropyAll - ((double)(noteLeft[i][j]+silenceLeft[i][j])/(wrong+right))*entropyLeft - ((double)(noteRight[i][j]+silenceRight[i][j])/(wrong+right))*entropyRight;
				//System.out.println(pre + "Gain " + i + ": " + gain[i] + " thr: " + paramSet.get(i).threshold);
				 * */
			}
		}
		double max = Double.MIN_VALUE;
		int winner = 0;
		int winnerThreshold = 0;
		for(int i=0; i<numOfFeatures; i++) {
			for(int j=0; j<params.thresholdCandidatesPerFeature; j++) {
				if(gain[i][j] > max) {
					max = gain[i][j];
					winner = i;
					winnerThreshold = j;
				}
			}
		}

		// Debug //////////////////////////////////////////
		root.infoGain.add(gain[winner][winnerThreshold]);
		if (params.logNodeInfo) {
			long silenceLeftW = silenceLeft[winner][winnerThreshold]; 
			long noteLeftW = noteLeft[winner][winnerThreshold];
			double noteLeftWCorr = noteLeftW/noteRatio;
			long silenceRightW = silenceRight[winner][winnerThreshold]; 
			long noteRightW = noteRight[winner][winnerThreshold];
			double noteRightWCorr = noteRightW/noteRatio;
			long allW = silenceLeftW + noteLeftW + noteRightW + silenceRightW;
			Log.write(pre + "Winner: " + winner + "; Information gain: " + gain[winner][winnerThreshold] + " Threshold: " + thresholds[winner][winnerThreshold]);
			Log.write(pre + "Left note: " + noteLeftW + " (corr.: " + noteLeftWCorr + "), silence: " + silenceLeftW + ", sum: " + (silenceLeftW+noteLeftW) + ", n/s(corr): " + (noteLeftWCorr/silenceLeftW));
			Log.write(pre + "Right note: " + noteRightW + " (corr.: " + noteRightWCorr + "), silence: " + silenceRightW + ", sum: " + (silenceRightW+noteRightW) + ", n/s(corr): " + (noteRightWCorr/silenceRightW));
			Log.write(pre + "Amount of counted samples: " + allW);
			//String thr = "";
			float tmin = Float.MAX_VALUE;
			float tmax = Float.MIN_VALUE;
			for(int i=0; i<thresholds[winner].length; i++) {
				//thr += thresholds[winner][i] + ", ";
				if (thresholds[winner][i] > tmax) tmax = thresholds[winner][i];
				if (thresholds[winner][i] < tmin) tmin = thresholds[winner][i];
			}
			Log.write(pre + "Threshold min: " + tmin + "; max: " + tmax);
			//Log.write(pre + "Tested winner thresholds: " + thr);
		}
		//////////////////////////////////////////////////
		
		// See in info gain is sufficient:
		if (gain[winner][winnerThreshold] >= params.entropyThreshold) {
			// Yes, save feature and continue recursion
			node.feature = paramSet.get(winner);
			node.feature.threshold = thresholds[winner][winnerThreshold];
			if (params.logNodeInfo) Log.write(pre + "Feature threshold: " + node.feature.threshold + "; Coeffs: " + node.feature);
		} else {
			// No, make leaf and return
			//node.probabilities = calculateLeaf(sampler, classification, mode, depth);
			node.probability = calculateLeaf2(sampler, classification, mode, depth);
			if (params.logNodeInfo) Log.write(pre + "Mode " + mode + " leaf; Probability " + node.probability);
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
		
		// Recursion to left and right
		node.left = new Node();
		growRec(root, sampler, classificationNext, node.left, 1, depth+1, maxDepth, true, noteRatio);

		node.right = new Node();
		growRec(root, sampler, classificationNext, node.right, 2, depth+1, maxDepth, true, noteRatio);
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
	 * Calculates leaf probability.
	 * 
	 * @param sampler
	 * @param mode
	 * @param depth
	 * @return
	 * @throws Exception
	 */
	protected float calculateLeaf2(final Sampler<Dataset> sampler, List<byte[][]> classification, final int mode, final int depth) throws Exception {
		float ret = 0;
		float not = 0;
		// See how much was judged right
		for(int i=0; i<sampler.getPoolSize(); i++) {
			Dataset dataset = sampler.get(i);
			byte[][] midi = dataset.getMidi();
			byte[][] cla = classification.get(i);
			
			for(int x=0; x<midi.length; x++) {
				for(int y=0; y<midi[0].length; y++) {
					if (mode == cla[x][y]) { 
						if (midi[x][y] > 0) {
							// f0 is present
							if (mode == 1) {
								ret++;
							} else {
								not++;
							}
						} else {
							if (mode == 2) {
								ret++;
							} else {
								not++;
							}
						}
					}
				}
			}
		}
		/*
		// Invert back and normalize
		for(int i=0; i<ret.length; i++) {
			ret[i] = (maxP - ret[i]) / maxP; 
		}
		return ret;
		*/
		return ret/(ret+not);
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
