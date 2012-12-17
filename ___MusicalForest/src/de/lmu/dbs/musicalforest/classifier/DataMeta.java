package de.lmu.dbs.musicalforest.classifier;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;

import de.lmu.dbs.jspectrum.TransformParameters;
import de.lmu.dbs.jspectrum.util.FileIO;

/**
 * Test data relevant meta data.
 * 
 * @author Thomas Weber
 *
 */
public class DataMeta implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Scaling parameter
	 */
	public double scaleParam;
	
	/**
	 * Audio sample rate of the wav files
	 */
	public double sampleRate;
	
	/**
	 * CQT parameters
	 */
	public TransformParameters transformParams;
	
	/**
	 * This is used to synchronize MIDI and WAV/CQT. This is added to the midi
	 * positions (milliseconds)
	 */
	public long midiOffset = 0;
	
	/**
	 * Create new meta object.
	 * 
	 * @param scaleParam
	 */
	public DataMeta(double scaleParam, double sampleRate, TransformParameters params) {
		this.scaleParam = scaleParam;
		this.sampleRate = sampleRate;
		this.transformParams = params;
	}
	
	/**
	 * Saves the data meta object to file
	 * @throws Exception 
	 * 
	 */
	public void save(String filename) throws Exception {
		FileIO<DataMeta> io = new FileIO<DataMeta>();
		io.save(filename, this);
	}

	/**
	 * Load meta file.
	 * 
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	public static DataMeta load(String filename) throws Exception {
		FileInputStream fin = new FileInputStream(filename);
		ObjectInputStream ois = new ObjectInputStream(fin);
		DataMeta ret = (DataMeta)ois.readObject();
		ois.close();
		ret.check();
		return ret;
	}

	/**
	 * Compares this object to another.
	 * 
	 * @return
	 * @throws Exception 
	 */
	public boolean compareTo(DataMeta m, boolean ignoreCqtKernelBufferLocation) throws Exception {
		if (scaleParam != m.scaleParam) return false;
		if (sampleRate != m.sampleRate) return false;
		if (!transformParams.compareTo(m.transformParams, ignoreCqtKernelBufferLocation)) return false;
		return true;
	}

	/**
	 * Checks integrity. 
	 * 
	 * @throws Exception
	 */
	public void check() throws Exception {
		transformParams.check();
		if (sampleRate < 0) throw new Exception("Invalid sample rate: " + sampleRate);
	}

	/**
	 * 
	 */
	public String toString() {
		return 
			"Training dataset parameters: \n" + 
			"  Log Scaling Parameter: " + scaleParam + "\n" + 
			"  Sample Rate:           " + sampleRate + "\n" + 
			"  MIDI sync offset (ms): " + midiOffset + "\n\n" + 
			transformParams.toString();
	}
}
