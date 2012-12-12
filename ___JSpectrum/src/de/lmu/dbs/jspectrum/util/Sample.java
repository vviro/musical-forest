package de.lmu.dbs.jspectrum.util;

import java.io.PrintStream;

/**
 * Core class for analysis of audio samples. The samples have to be loaded with the init method,
 * by passing the in an int[] array with corresponding bit depth (indicating the value of the fullscale
 * sample) and the sample rate (samples per second).
 * 
 * @author Thomas Weber
 *
 */
public class Sample {

	/**
	 * Sample window used to determine the noise floor of the file
	 */
	public static final int NOISE_FLOOR_WINDOW = 4410; 
	
	/**
	 * Headroom above noise floor to calculate with
	 */
	public static final double NOISE_FLOOR_HEADROOM = 6.0;
	
	/**
	 * Minimum Frequency (for Loudness metering)
	 */
	public static final int LOUDNESS_MINFREQ = 20;
	
	/**
	 * Here the data is stored in memory after instance creation
	 */
	protected int[] buffer = null; // data array
	
	/**
	 * dB FullScale sample value for faster access
	 */
	protected int sampleFullscale;
	
	/**
	 * Sample rate for faster access
	 */
	private long sampleRate;
	
	/**
	 * Audio dynamics in bits
	 */
	protected int bitDepth;

	/**
	 * Initializes the instance with a given data array
	 * 
	 * @param bitDepth
	 * @param sampleRate
	 * @param data
	 */
	public void init(int bitDepth, long sampleRate, int[] data) {
		this.bitDepth = bitDepth;
		this.sampleRate = sampleRate;
		this.buffer = data;
		this.sampleFullscale = getFullScale();
	}
	
	/**
	 * Checks integrity of the buffer data. Returns false if the data is inconsistent, true if everything
	 * is all right.
	 * 
	 * @return
	 */
	public boolean check() {
		for(int i=0; i<this.buffer.length; i++) {
			if (this.buffer[i] > this.sampleFullscale) return false;
		}
		return true;
	}
	
	/**
	 * Returns the maximum sample value (positive)
	 * 
	 * @return
	 */
	public int getFullScale() {
		return (int)Math.pow(2, this.bitDepth);
	}
	
	/**
	 * Calculates sample value -> decibels. Attention: sample 0 will return -Infinity. Use sample=1 
	 * for an approximation to the lowest possible value.
	 * 
	 * @param sample
	 * @return
	 */
	public double toDecibels(int sample) {
		return (sample == 0) 
			? (20 * Math.log10(1/sampleFullscale)) // Fake but better readable and not really relevant here
			: (20 * Math.log10(((double)Math.abs(sample))/sampleFullscale));
	}
	
	/**
	 * Calculates decibels -> sample value
	 * 
	 * @param decibels
	 * @return
	 */
	public int toSample(double decibels) {
		return (int)Math.pow(10, decibels/20 + Math.log10(sampleFullscale));
	}
	
	/**
	 * Adds some decibels to a sample value and returns the result.
	 * 
	 * @param sample
	 * @param db
	 * @return
	 */
	public int addDecibels(int sample, double db) {
		return toSample(toDecibels(sample) + db);
	}
	
	/** 
	 * Gets the minimum and maximum samples for both channels.
	 * 
	 * @return An int[] array: Lmin | Lmax | Rmin | Rmax
	 * @throws Exception
	 */
	public int[] getMinMax() throws Exception {
		if (buffer == null) throw new Exception("Buffer not filled yet.");
		int[] ret = new int[4]; // Lmin, Lmax, Rmin, Rmax
		ret[0] = Integer.MAX_VALUE;
		ret[1] = Integer.MIN_VALUE;
		ret[2] = Integer.MAX_VALUE;
		ret[3] = Integer.MIN_VALUE;
		for (int s=0 ; s<buffer.length; s+=2) {
			if (buffer[s] < ret[0]) ret[0] = buffer[s];
			if (buffer[s] > ret[1]) ret[1] = buffer[s];
			if (buffer[s+1] < ret[2]) ret[2] = buffer[s+1];
			if (buffer[s+1] > ret[3]) ret[3] = buffer[s+1];
		}
		return ret;
	}
	
	/** 
	 * Gets the absolute minimum and maximum samples for both channels.
	 * 
	 * @return An int[] array: Lmin | Lmax | Rmin | Rmax
	 * @throws Exception
	 */
	public int[] getMinMaxAbsolute() throws Exception {
		if (buffer == null) throw new Exception("Buffer not filled yet.");
		int[] ret = new int[4]; // Lmin, Lmax, Rmin, Rmax
		ret[0] = Integer.MAX_VALUE;
		ret[1] = 0;
		ret[2] = Integer.MAX_VALUE;
		ret[3] = 0;
		for (int s=0 ; s<buffer.length; s+=2) {
			if (Math.abs(buffer[s]) < ret[0]) ret[0] = Math.abs(buffer[s]);
			if (Math.abs(buffer[s]) > ret[1]) ret[1] = Math.abs(buffer[s]);
			if (Math.abs(buffer[s+1]) < ret[2]) ret[2] = Math.abs(buffer[s+1]);
			if (Math.abs(buffer[s+1]) > ret[3]) ret[3] = Math.abs(buffer[s+1]);
		}
		return ret;
	}
	
	/**
	 * Returns the noise floor in sample value. You can define the start sample index,
	 * which defines whether left or right channel is used. The buffer is examined from 
	 * start and in window length.
	 * 
	 * @param start use 0 for left and 1 for right channel
	 * @param window the width of the silence part in samples
	 * @return
	 */
	public int getNoiseFloor(int start, int window) {
		int floor = Integer.MIN_VALUE;
		for (int i=start; i<window; i+=2) {
			if (Math.abs(buffer[i]) > floor) floor = Math.abs(buffer[i]);
		}
		return floor;
	}

	/**
	 * Returns the average loudness in db value at the sample index.
	 * If the value cannot be calculated, -1 or -2 is returned. (-1: Left side too small, -2: right side too small)
	 * 
	 * @param index
	 * @param minFreq in Hertz
	 * @return
	 */
	public double getLoudnessDb(int index, int minFreq) {
		return toDecibels(getLoudness(index, minFreq)) + 10.0;
	}

	/**
	 * Returns the average loudness in sample value at the sample index.
	 * If the value cannot be calculated, -1 or -2 is returned. (-1: Left side too small, -2: right side too small)
	 * 
	 * @param index
	 * @param minFreq in Hertz
	 * @return
	 */
	public int getLoudness(int index, int minFreq) {
		int windowSize = (int)((getSampleRate()/minFreq)/2);
		int start = index - windowSize;
		if (start < 0) return -1;
		int end = index + windowSize;
		if (end >= this.buffer.length) return -1;
		
		int acc = 0;
		int s = 0;
		for(int i = start; i <= end; i+=2) {
			acc += Math.abs(buffer[i]);
			s++;
		}
		
		return acc/s;
	}

	/**
	 * Returns the (absolute) index of the first occurrence of a higher sample value than floor, starting at sample i.
	 * If no peak has been found, -1 will be returned.
	 * 
	 * @param i
	 * @param floor
	 * @return
	 */
	public int getFirstPeak(boolean left, int start, int floor) {
		int right = left ? 0 : 1;
		for (int i=start*2+right; i<buffer.length; i+=2) {
			if (Math.abs(buffer[i]) > floor) {
				return i/2;
			}
		}
		return -1;
	}
	
	/**
	 * Returns the (absolute) index of the first occurrence of a lower sample value than ceil, starting at sample i, and lasting for minimum of last samples.
	 * If no low has been found, -1 will be returned.
	 * 
	 * @param i
	 * @param floor
	 * @param last
	 * @return
	 */
	public int getFirstBelow(boolean left, int start, int ceil, int last) {
		int right = left ? 0 : 1;
		int acc = 0;
		for (int i=start*2+right; i<buffer.length; i+=2) {
			if (Math.abs(buffer[i]) <= ceil) {
				acc++;
			} else acc = 0;
			if (acc > last) return i/2-last;
		}
		return -1;
	}
	
	/**
	 * Returns a buffer array with just the left side of the file.
	 * 
	 * @return
	 */
	public int[] getLeftBuffer() {
		int[] ret = new int[buffer.length/2];
		int b = 0;
		for(int i=0; i<buffer.length/2; i++) {
			ret[i] = buffer[b];
			b+=2;
		}
		return ret;
	}

	/**
	 * Returns a buffer array with just the right side of the file.
	 * 
	 * @return
	 */
	public int[] getRightBuffer() {
		int[] ret = new int[buffer.length/2];
		int b = 0;
		for(int i=1; i<buffer.length/2; i++) {
			ret[i] = buffer[b];
			b+=2;
		}
		return ret;
	}
	
	/**
	 * Returns a buffer array with "mono-fied" data
	 * 
	 * @return
	 */
	public int[] getMono() {
		int[] ret = new int[buffer.length/2];
		int b = 0;
		for(int i=0; i<buffer.length/2; i++) {
			ret[i] = (buffer[b] + buffer[b+1]) / 2;
			b+=2;
		}
		return ret;
	}

	/* ***************************************************************************************************************************** */ 
	/* ***************************************************************************************************************************** */ 
	
	/**
	 * Prints info for the loaded data on the specified PrintStream instance.
	 * 
	 * @param out
	 * @throws Exception
	 */
	public void printInfo(PrintStream out) throws Exception {
		int[] minmax = getMinMax();
		out.println("Signed Maxima:");
		out.println("Left:  Min: " + minmax[0] + ", Max: " + minmax[1]);
		out.println("Right: Min: " + minmax[2] + ", Max: " + minmax[3]);
		minmax = getMinMaxAbsolute();
		out.println("Absolute Maxima:");
		out.println("Left:  Min: " + minmax[0] + ", Max: " + minmax[1]);
		out.println("Right: Min: " + minmax[2] + ", Max: " + minmax[3]);
		out.println("Noise Floor:");
		out.println("Right: " + getNoiseFloor(0, NOISE_FLOOR_WINDOW) + ", " + getNoiseFloor(1, NOISE_FLOOR_WINDOW));
		//out.println("Noise Floor +6dB:");
		//out.println("Right: " + addDecibels(getNoiseFloor(0, NOISE_FLOOR_WINDOW), NOISE_FLOOR_HEADROOM) + ", " + addDecibels(getNoiseFloor(1, NOISE_FLOOR_WINDOW), NOISE_FLOOR_HEADROOM));
		//out.println("FullScale sample value: " + getFullScale() + "; Minimum dB (approx): " + toDecibels(1));
	}

	public long getSampleRate() {
		return sampleRate;
	}
	
	/**
	 * Puts all audio data into a file for debugging purposes only.
	 * 
	 * @param filename
	 * @throws Exception
	 *
	public void debugToFile(String filename) throws Exception {
		debugToFile(filename, 0, buffer.length);
	}

	/**
	 * Puts selected audio data into a file for debugging purposes only.
	 * 
	 * @param filename
	 * @param minSample
	 * @param maxSample
	 * @throws Exception
	 *
	public void debugToFile(String filename, int minSample, int maxSample) throws Exception {
		if (buffer == null) throw new Exception("Buffer not filled yet.");
		FileWriter outFile = new FileWriter(filename);
		PrintWriter out = new PrintWriter(outFile);
		int fi = Integer.toString(sampleFullscale).length();
		// Header line
		String str = "Smpl: " 
				+ fillString("Left", fi-6) 
				+ " "
				+ fillString("Right", fi)
				+ " dB: "
				+ fillString("Left", 20-5)
				+ " "
				+ fillString("Right", 20)
				+ " Freq: "
				+ fillString("L/R", 7)
				+ fillString(" Loudness: ", 21);
		out.println(str);
		for (int i=0; i< str.length(); i++) out.print("-");
		out.println();
		for (int i = minSample; i<maxSample; i+=2) {
			if (i >= buffer.length) break;
			// LR Sample values
			out.print(
					//"Smpl: " + 
					fillString(Integer.toString(buffer[i]), fi) + " " + fillString(Integer.toString(buffer[i+1]), fi)
			);
			// LR loudness (dBFS)
			out.print(//" dB: " 
					fillString(Integer.toString((int)toDecibels(buffer[i])), 5)
					+ fillString(getBar((int)toDecibels(buffer[i]), 15), 15)
					+ " " 
					+ fillString(Integer.toString((int)toDecibels(buffer[i+1])), 5)
					+ fillString(getBar((int)toDecibels(buffer[i+1]), 15), 15)
			);
			// Frequency
			out.print(
					//" Freq: " 
					fillString(Integer.toString((int)detectPitch(i)), 6)
					+ " " 
					+ fillString(Integer.toString((int)detectPitch(i+1)), 6)
			);
			out.print(
					// Loud
					fillString(Double.toString(this.getLoudnessDb(i, LOUDNESS_MINFREQ)), 10)
					+ "/"
					+ fillString(Double.toString(this.getLoudnessDb(i+1, LOUDNESS_MINFREQ)), 10)
			);
			// Close line
			out.println();
		}
		out.close();
	}
	
	/**
	 * Puts selected audio data into a file for debugging purposes only. Small output (just Samples and Loudness)
	 * 
	 * @param filename
	 * @param minSample
	 * @param maxSample
	 * @throws Exception
	 *
	public void debugToFileSmall(String filename, int minSample, int maxSample) throws Exception {
		if (buffer == null) throw new Exception("Buffer not filled yet.");
		//AudioAnalyser ana = new AudioAnalyser();
		//int detectionFrame = 100; // double this value, it means to left and right sides 
		FileWriter outFile = new FileWriter(filename);
		PrintWriter out = new PrintWriter(outFile);
		int fi = Integer.toString(sampleFullscale).length();
		// Header line
		String str = "Smpl: " 
				+ fillString("Left", fi-6) 
				+ " "
				+ fillString("Right", fi)
				+ fillString(" Loudness: ", 21);
				//+ " "
				//+ fillString("Right", 6);
		out.println(str);
		for (int i=0; i< str.length(); i++) out.print("-");
		out.println();
		for (int i = minSample; i<maxSample; i+=2) {
			if (i >= buffer.length) break;
			// LR Sample values
			out.print(
					//"Smpl: " + 
					fillString(Integer.toString(buffer[i]), fi) + " " + fillString(Integer.toString(buffer[i+1]), fi)
			);
			// LR loudness (dBFS)
			out.print(
					// Loud
					fillString(Double.toString(this.getLoudnessDb(i, LOUDNESS_MINFREQ)), 10)
					+ "/"
					+ fillString(Double.toString(this.getLoudnessDb(i+1, LOUDNESS_MINFREQ)), 10)
			);
			// Close line
			out.println();
		}
		out.close();
	}

	/**
	 * Returns an excerpt of the buffer
	 * 
	 * @param min
	 * @param max
	 * @return
	 *
	public int[] getExcerpt(int min, int max) {
		int[] ret = new int[max-min+1];
		int index = 0;
		for (int i=min; i<=max; i++) {
			ret[index++] = buffer[i];
		}
		return ret;
	}
	
	/**
	 * Helper for debugToFile to generate loudness bars
	 * 
	 * @param value
	 * @param width
	 * @return
	 *
	protected String getBar(int value, int width) {
		double val = (-toDecibels(1) + value)/-toDecibels(1);
		String ret = "";
		for (int i=0; i<width; i++) {
			if (i<val*width) {
				ret += "#";
			} else {
				ret += ".";
			}
		}
		return ret;
	}

	/**
	 * Fills up a string to a given length with spaces
	 * 
	 * @param str
	 * @param places
	 * @return
	 *
	protected String fillString(String str, int places) {
		return fillString(str, places, " ");
	}
	
	/**
	 * Fills up a string to a given length with specified character or string
	 * 
	 * @param str
	 * @param places
	 * @param fillChar
	 * @return
	 *
	protected String fillString(String str, int places, String fillChar) {
		String ret = str;
		while(ret.length() < places) {
			ret += fillChar;
		}
		return ret;
	}
	
	//*/
}
