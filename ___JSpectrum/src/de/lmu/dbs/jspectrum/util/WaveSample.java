package de.lmu.dbs.jspectrum.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import de.lmu.dbs.jspectrum.util.WavFile.WavFile;
import de.lmu.dbs.jspectrum.util.WavFile.WavFileException;

/**
 * This is a variant of Sample that can load the data directly from any WAV file. All properties (bit
 * depth, sample rate) are detected automatically by the WavFile class.
 * <br><br>
 * Uses the Wav file IO class by A.Greensted from http://www.labbookpages.co.uk
 * 
 * @author Thomas Weber
 *
 */
public class WaveSample extends Sample {

	/**
	 * The WAV file to be read
	 */
	protected File file = null;
	
	/**
	 * The WavFile instance to read the wave file
	 */
	protected WavFile wavFile = null;

	/**
	 * Create a Sample instance. Immediately loads the data from the specified file.
	 * 
	 * @param file
	 * @throws Exception
	 */
	public WaveSample(File file) throws Exception {
		super();
		init(file);
	}

	/**
	 * Initialize instance with an audio file. Loads the file into memory.
	 * 
	 * @param file
	 * @throws Exception 
	 */
	public void init(File file) throws Exception {
		this.file = file;
		this.wavFile = openFile(this.file);
		init(this.wavFile.getValidBits(), this.wavFile.getSampleRate(), readData(this.wavFile));
	}
	
	/**
	 * Returns a WavFile instance for file.
	 * 
	 * @param file
	 * @return the WavFile instance
	 * @throws IOException
	 * @throws WavFileException
	 */
	protected WavFile openFile(File file) throws IOException, WavFileException {
		WavFile wf = WavFile.openWavFile(this.file);
		return wf;
	}
	
	/**
	 * Reads samples from a WavFile instance and returns them.
	 * 
	 * @param wf
	 * @return the samples
	 * @throws Exception
	 */
	protected int[] readData(WavFile wf) throws Exception {
		if (wf == null) throw new Exception("No valid WavFile instance is given.");
		int numChannels = wf.getNumChannels();
		int bufSize = 256;
		int[] buf = new int[bufSize * numChannels];
		int[] ret = new int[((int) wf.getNumFrames()) * numChannels];
		int framesRead;  // num of frames read by one attempt 
		int framesTotal = 0;  // total num of frames read
		do {
			framesRead = wf.readFrames(buf, bufSize);
		    for (int s=0 ; s<framesRead * numChannels; s++) {
		    	ret[framesTotal*numChannels + s] = buf[s]; // transport data
		    }
		    framesTotal += framesRead;
		} while (framesRead != 0);
		wf.close();
		return ret;
	}

	/**
	 * Prints info for the loaded audio file on the specified PrintStream instance.
	 * 
	 * @param out
	 * @throws Exception
	 */
	public void printInfo(PrintStream out) throws Exception {
		if (wavFile == null) throw new Exception("No valid WavFile instance is given.");
		wavFile.display(out);
		out.println("Num of Stereo Frames: " + wavFile.getNumFrames());
		super.printInfo(out);
	}
	
}
