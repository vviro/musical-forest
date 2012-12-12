package de.lmu.dbs.musicalforest.classifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;

import de.lmu.dbs.jforest.core.Forest;
import de.lmu.dbs.jforest.core.ForestParameters;
import de.lmu.dbs.musicalforest.Action;


/**
 * This class encapsulates forest metadata.
 * 
 * @author Thomas Weber
 *
 */
public class ForestMeta implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Best threshold calculated for note onset mean shifting
	 */
	public double bestOnsetThreshold;
	
	/**
	 * Best threshold calculated for note onset mean shifting
	 */
	public double bestOffsetThreshold;
	
	/**
	 * Accuracy test at best threshold (note on)
	 */
	public AccuracyTest bestOnsetThresholdTest;
	
	/**
	 * Accuracy test at best threshold (note off)
	 */
	public AccuracyTest bestOffsetThresholdTest;
	
	/**
	 * Meta data of the training datasets used to grow the forest
	 */
	public DataMeta dataMeta;
	
	/**
	 * Forest parameters used for growing the trees in the forest.
	 * Only relevant in training mode, if a forest is merged manually,
	 * this is usually null.
	 */
	public ForestParameters forestParams;
	
	/**
	 * Hash value for the tree files beneath the meta file.
	 */
	private String hash;
	
	/**
	 * 
	 * @param bestThreshold
	 * @param bestThresholdTest
	 */
	public ForestMeta(double bestOnsetThreshold, AccuracyTest bestOnsetThresholdTest, double bestOffsetThreshold, AccuracyTest bestOffsetThresholdTest, DataMeta dataMeta) {
		this.bestOnsetThreshold = bestOnsetThreshold;
		this.bestOffsetThreshold = bestOffsetThreshold;
		this.bestOnsetThresholdTest = bestOnsetThresholdTest;
		this.bestOffsetThresholdTest = bestOffsetThresholdTest;
		this.dataMeta = dataMeta;
	}

	/**
	 * 
	 */
	public ForestMeta() {
	}
	
	/**
	 * Checks integrity.
	 * 
	 * @throws Exception
	 */
	public void check() throws Exception {
		if (dataMeta == null) throw new Exception("Data meta object is null");
		dataMeta.check();
		if (forestParams != null) forestParams.check();
		if (bestOnsetThresholdTest == null) throw new Exception("No onset test (null)");
		if (bestOffsetThresholdTest == null) throw new Exception("No offset test (null)");
		if (bestOnsetThreshold < 0 || bestOnsetThreshold > 1) throw new Exception("Invalid onset threshold: " + bestOnsetThreshold);
		if (bestOffsetThreshold < 0 || bestOffsetThreshold > 1) throw new Exception("Invalid offset threshold: " + bestOffsetThreshold);
	}
	
	/**
	 * Save instance to file.
	 * 
	 * @param filename
	 * @throws Exception 
	 */
	public void save(String filename) throws Exception {
		save(filename, false);
	}
	
	/**
	 * Save instance to file.
	 * 
	 * @param filename
	 * @throws Exception 
	 */
	public void save(String filename, boolean noHashAndChecks) throws Exception {
		if (!noHashAndChecks) {
			hash = calcHash((new File(filename)).getParentFile());
			check();
		} else {
			hash = null;
		}
		FileOutputStream fout = new FileOutputStream(filename);
		ObjectOutputStream oos = new ObjectOutputStream(fout);   
		oos.writeObject(this);
		oos.close();
	}
	
	/**
	 * Load meta file.
	 * 
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	public static ForestMeta load(String filename) throws Exception {
		return load(filename, false);
	}

	/**
	 * Load meta file.
	 * 
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	public static ForestMeta load(String filename, boolean force) throws Exception {
		FileInputStream fin = new FileInputStream(filename);
		ObjectInputStream ois = new ObjectInputStream(fin);
		ForestMeta ret = (ForestMeta)ois.readObject();
		ois.close();

		File hf = new File(filename).getParentFile();
		if (!force) {
			ret.check();
			if (!ret.checkHash(hf)) throw new ForestMetaException("Forest meta data in " + hf.getAbsolutePath() + " needs to be updated, try running update action");
			//if (!force) throw new ForestMetaException("Forest meta data in " + hf.getAbsolutePath() + "needs to be updated, try running update action: " + ret.hash);
			//else System.err.println("WARNING: Forest meta data in " + hf.getAbsolutePath() + " does not match tree files: " + ret.hash);
		}
		return ret;
	}
	
	/**
	 * Returns a readable representation of the meta data.
	 * 
	 */
	public String getAccuracyString() {
		return "Best onset threshold: " + this.bestOnsetThreshold + "; Best offset threshold: " + this.bestOffsetThreshold + 
			   "Best onset test: " + this.bestOnsetThresholdTest + "; Best offset test: " + this.bestOffsetThresholdTest;
	}

	/**
	 * Compares this object to another. (Only data params have to match)
	 * 
	 * @return
	 * @throws Exception 
	 */
	public boolean compareTo(ForestMeta fm, boolean ignoreCqtKernelBufferLocation) throws Exception {
		return dataMeta.compareTo(fm.dataMeta, ignoreCqtKernelBufferLocation);
	}
	
	/**
	 * Checks if the stored hash value is valid
	 * @throws Exception 
	 */
	private boolean checkHash(File dir) throws Exception {
		return (hash != null && hash.equals(calcHash(dir)));
	}
	
	/**
	 * Calculates a hash value over the tree files present 
	 * in the directory and stores it in an attribute.
	 * @throws Exception 
	 * 
	 */
	private static String calcHash(File dir) throws Exception {
		List<File> lst = Forest.getTreeList(dir.getAbsolutePath() + File.separator + Action.NODEDATA_FILE_PREFIX);
		String bs = "";
		for(int i=0; i<lst.size(); i++) {
			bs+= lst.get(i).getAbsolutePath() + getFileHash(lst.get(i));
		}
		String h = DigestUtils.md5Hex(bs);
		return h;
	}
	
	/**
	 * Returns the hash value of a file´s content
	 * 
	 * @param file
	 * @return
	 * @throws Exception 
	 */
	private static String getFileHash(File file) throws Exception {
		FileInputStream fin = new FileInputStream(file);
		return DigestUtils.md5Hex(fin);
	}

	/**
	 * 
	 */
	public String toString() {
		String ret = "Forest meta data: \n";
		ret+= "  Tree File Hash:                      " + hash + "\n";
		ret+= "  Best Onset Threshold:                " + bestOnsetThreshold + "\n";
		ret+= "  Best Offset Threshold:               " + bestOffsetThreshold + "\n";
		ret+= "  Best Onset Threshold Accuracy Test: \n" + bestOnsetThresholdTest + "";
		ret+= "  Best Offset Threshold Accuracy Test: \n" + bestOffsetThresholdTest + "\n";
		if (forestParams != null) ret+= forestParams.toString() + "\n";
		ret+= dataMeta.toString() + "\n";
		return ret; 
	}

}
