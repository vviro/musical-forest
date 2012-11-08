package de.lmu.dbs.ciaa.classifier;

import java.util.List;

import de.lmu.dbs.ciaa.classifier.features.Feature;

/**
 * 
 * 
 * @author tom
 *
 */
public class RandomTreeWorker extends Thread {

	protected List<Feature> paramSet = null;
	
	protected Sampler<Dataset> sampler = null;
	
	protected List<byte[][]> classification = null;
	
	protected int mode;
	
	protected RandomTree root;
	
	protected long[][] result = null;
	
	public RandomTreeWorker(RandomTree root, List<Feature> paramSet, Sampler<Dataset> sampler, List<byte[][]> classification, int mode) {
		this.root = root;
		this.paramSet = paramSet;
		this.sampler = sampler;
		this.classification = classification;
		this.mode = mode;
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
			result = work();
			root.decThreadsActive();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	/**
	 * Is the worker finished and are the results ready to fetch?
	 * 
	 * @return
	 */
	public boolean isDone() {
		return (result != null);
	}
	
	/**
	 * Calculates the main work load for a random tree.
	 * 
	 * @return
	 * @throws Exception
	 */
	public long[][] work() throws Exception {
		int numOfFeatures = paramSet.size();

		long[] silenceLeft = new long[paramSet.size()];
		long[] noteLeft = new long[paramSet.size()];
		long[] silenceRight = new long[paramSet.size()];
		long[] noteRight = new long[paramSet.size()];
		
		int poolSize = sampler.getPoolSize();
		for(int i=0; i<poolSize; i++) {
			// Each dataset...load spectral data and midi
			Dataset dataset = sampler.get(i);
			byte[][] data = dataset.getSpectrum();
			byte[][] midi = dataset.getMidi();
			byte[][] cla = classification.get(i);
			
			// get feature results and split data
			for(int x=0; x<data.length; x++) {
				for(int y=0; y<data[0].length; y++) {
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
		
		long[][] ret = new long[4][];
		ret[0] = silenceLeft;
		ret[1] = noteLeft;
		ret[2] = silenceRight;
		ret[3] = noteRight;
		return ret;
	}

}
