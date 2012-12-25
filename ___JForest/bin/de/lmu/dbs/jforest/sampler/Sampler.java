package de.lmu.dbs.jforest.sampler;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.lmu.dbs.jforest.core.Dataset;
import de.lmu.dbs.jforest.util.FileIO;
import de.lmu.dbs.jforest.util.RandomUtils;

/**
 * Generic base class for implementing sampling algorithms.
 * 
 * @author Thomas Weber
 *
 */
public abstract class Sampler<T extends Dataset> {

	/**
	 * All datasets
	 */
	protected List<T> datasets;
	
	/**
	 * Create sampler instance.
	 * 
	 * @param datasets the data pool of type T
	 */
	public Sampler(List<T> datasets) {
		this.datasets = datasets;
	}

	/**
	 * Returns a sample of the datasets.
	 * 
	 * @return
	 * @throws Exception 
	 */
	public abstract Sampler<T> getSample() throws Exception;

	/**
	 * Has to return a new sampler instance.
	 * 
	 * @param data
	 * @return
	 */
	public abstract Sampler<T> newInstance(List<T> data);

	/**
	 * Returns the data pool.
	 * 
	 * @return
	 */
	public List<T> getData() {
		return datasets;
	}
	
	/**
	 * Returns one dataset by index.
	 *  
	 * @param index
	 * @return
	 */
	public T get(final int index) {
		return datasets.get(index);
	}
	
	/**
	 * Returns the size of the data pool.
	 * 
	 * @return
	 */
	public int getPoolSize() {
		return datasets.size();
	}
	
	/**
	 * Returns the amount of different datasets.
	 * 
	 * @return
	 */
	public int getVariety() {
		return getDataWithoutDuplicates().size();
	}
	
	/**
	 * Returns a version of the data pool list without duplicates.
	 * 
	 * @return
	 */
	public List<T> getDataWithoutDuplicates() {
		List<T> ret = new ArrayList<T>();
		for(int i=0; i<datasets.size(); i++) {
			if (!ret.contains(datasets.get(i))) {
				ret.add(datasets.get(i));
			}
		}
		return ret;
	}
	
	/**
	 * Returns (parts) new Samplers that contain the disjunct split of the
	 * dataset pool. Each new Sampler contains the same amount of data (if division
	 * is not even, the last sampler actually has less than the others).
	 * <br><br>
	 * The samples for each Sampler are picked randomly from the dataset list.
	 * If parts is higher than the samplers size, the result will be the
	 * same as if parts were equal to the data pool size, meaning that for each
	 * dataset a new sampler is returned, which holds this single dataset.
	 * 
	 * @param parts number of split samplers to be created
	 * @return
	 */
	public List<Sampler<T>> split(final int parts) {
		int numPerPart = (int)Math.ceil((double)datasets.size() / parts);
		List<Sampler<T>> ret = new ArrayList<Sampler<T>>();
		// Create shuffled clone of datasets list
		List<T> n = new ArrayList<T>(datasets);
		Collections.shuffle(n);
		// Create samplers
		Sampler<T> sampler = null;
		List<T> newList = new ArrayList<T>();
		for(int i=0; i<n.size(); i++) {
			if (newList.size() == numPerPart) {
				// New sampler
				sampler = newInstance(newList);
				ret.add(sampler);
				newList = new ArrayList<T>();
			}
			T nn = n.get(i);
			newList.add(nn);
		}
		// Last sampler
		if (newList.size() > 0) {
			sampler = newInstance(newList);
			ret.add(sampler);
		}
		return ret;
	}
	
	/**
	 * 
	 * @param filename
	 * @throws Exception 
	 */
	public void saveSampling(String filename) throws Exception {
		for (int i=0; i<getPoolSize(); i++) {
			get(i).saveSampling(filename + "dataset_" + i);
		}
	}

	/**
	 * 
	 * @param string
	 * @return
	 * @throws Exception 
	 */
	public Sampler<T> loadDistribution(String filename) throws Exception {
		FileIO<int[]> io = new FileIO<int[]>();
		
		// Clone datasets
		List<T> ret = new ArrayList<T>();
		int size = getPoolSize();
		for(int i=0; i<size; i++) {
			T clone = (T)datasets.get(i).getClone();
			File f = new File(filename + "dataset_" + i);
			if (!f.exists() || !f.isFile()) throw new Exception("No sampling array found: " + f.getAbsolutePath());
			int[] d = io.load(filename + "dataset_" + i);
			clone.setData(d);
			ret.add(clone);
			System.out.println("Loaded sampling from " + f.getName());
		}
		return newInstance(ret);
	}
}
