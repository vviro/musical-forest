package de.lmu.dbs.jspectrum;

import java.io.Serializable;

import org.jdom2.Element;

import de.lmu.dbs.jspectrum.util.ParamLoader;

/**
 * Transformation parameters, mainly for CQT.
 * 
 * @author Thomas Weber
 *
 */
public class TransformParameters extends ParamLoader implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * This is just an array holding the frequencies corresponding to the spectral data bins
	 */
	public double[] frequencies = null;

	/**
	 * Number of frequency bins per octave. Has to be equal to the CQT equivalent parameter.
	 */
	public double binsPerOctave = -1;
	
	/**
	 * Min frequency of spectral data. Has to be equal to the CQT equivalent parameter.
	 */
	public double fMin = -1;

	/**
	 * Max frequency of spectral data. Has to be equal to the CQT equivalent parameter.
	 */
	public double fMax = -1;

	/**
	 * Samples per frame, used to interpret training data and for testing
	 */
	public int step = -1;
	
	/**
	 * See CQT Transform
	 */
	public double threshold = -1;
	
	/**
	 * See CQT Transform
	 */
	public double spread = -1;
	
	/**
	 * See CQT Transform
	 */
	public double divideFFT = -1;
	
	/**
	 * Buffer location (folder) for cqt kernels. Must be writable by the program.
	 */
	public String cqtKernelBufferLocation = null;
	
	/**
	 * Calculates the number of frequency bins per half tone in CQT.
	 * Result will be rounded up.
	 * 
	 * @return
	 */
	public int getBinsPerHalfTone() {
		double ret = binsPerOctave / 12.0;
		return (int)Math.ceil(ret);
	}
	
	/**
	 * Checks value integrity.
	 * 
	 * @return
	 * @throws Exception 
	 */
	public void check() throws Exception {
		if (frequencies == null || frequencies.length < 1) throw new Exception("Frequency array is null or contains no elements");
		if (binsPerOctave < 1) throw new Exception("You have to set the number of bins per octave");
		if (fMin < 1) throw new Exception("You have to set the minimum frequency of spectral data");
		if (fMax < 1) throw new Exception("You have to set the maximum frequency of spectral data");
		if (step < 1) throw new Exception("Frame step in samples too low: " + step);
		if (threshold < 0) throw new Exception("CQT transformation threshold is too low: " + threshold);
		if (spread < 0) throw new Exception("CQT transformation spread is too low: " + spread);
		if (divideFFT <= 0) throw new Exception("CQT divideFFT parameter has to be greater than zero: " + divideFFT);
		if (cqtKernelBufferLocation == null) throw new Exception("No cqt kernel buffer folder is set");
	}
	
	/**
	 * Generates a parameter set for forest growing. You have to set some of
	 * the params manually...see class documentation of RandomTreeParameters.
	 * 
	 * @return
	 * @throws Exception 
	 */
	public void loadParameters(String configFile) throws Exception {
		Element root = parseConfigFile(configFile, null);
		Element transform = root.getChild("Transformation");

		binsPerOctave = Double.parseDouble(transform.getAttributeValue("binsPerOctave"));
		fMin = Double.parseDouble(transform.getAttributeValue("fMin"));
		fMax = Double.parseDouble(transform.getAttributeValue("fMax"));
		step = Integer.parseInt(transform.getAttributeValue("step"));
		threshold = Double.parseDouble(transform.getAttributeValue("threshold"));
		spread = Double.parseDouble(transform.getAttributeValue("spread"));
		divideFFT = Double.parseDouble(transform.getAttributeValue("divideFFT"));
		cqtKernelBufferLocation = transform.getAttributeValue("cqtKernelBufferLocation");
	}
	
	/**
	 * Compare this to another param set. 
	 * 
	 * @param other
	 * @return true if identical
	 * @throws Exception 
	 */
	public boolean compareTo(TransformParameters other, boolean ignoreCqtKernelBufferLocation) throws Exception {
		if (binsPerOctave != other.binsPerOctave) return false;
		if (fMin != other.fMin) return false;
		if (fMax != other.fMax) return false;
		if (step != other.step) return false;
		if (threshold != other.threshold) return false;
		if (spread != other.spread) return false;
		if (divideFFT != other.divideFFT) return false;
		if (divideFFT != other.divideFFT) return false;
		if (!ignoreCqtKernelBufferLocation && !cqtKernelBufferLocation.equals(other.divideFFT)) return false;
		if (frequencies.length != other.frequencies.length) throw new Exception("Corrupt transform parameters file");
		for (int i=0; i<frequencies.length; i++) {
			if (frequencies[i] != other.frequencies[i]) return false;
		}
		return true;
	}
	
	/**
	 * 
	 */
	public String toString() {
		String ret = "Spectral transformation parameters: \n";
		//ret+= "  Bin frequencies array:   " + ArrayUtils.toString(frequencies) + "\n";
		ret+= "  Num of bins:             " + frequencies.length + "\n";
		ret+= "  binsPerOctave:           " + binsPerOctave + "\n";
		ret+= "  fMin:                    " + fMin + "\n";
		ret+= "  fMax:                    " + fMax + "\n";
		ret+= "  step:                    " + step + "\n";
		ret+= "  FFT threshold:           " + threshold + "\n";
		ret+= "  FFT spread:              " + spread + "\n";
		ret+= "  divideFFT:               " + divideFFT + "\n";
		ret+= "  cqtKernelBufferLocation: " + cqtKernelBufferLocation + "\n";
		return ret;
	}
}
