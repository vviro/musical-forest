package de.lmu.dbs.ciaa.util;

import java.awt.Color;
import java.io.File;
import java.nio.ByteBuffer;


/**
 * Some static array utility methods.
 * 
 * @author Thomas Weber
 *
 */
public class ArrayUtils {
	
	/**
	 * Shifts the values in the array to the reight by num places.
	 * 
	 * @param reference
	 * @param num
	 */
	public static void shiftRight(byte[][] data, int num) {
		for(int x=data.length-1; x>=0; x--) {
			if (x-num >= 0) {
				data[x] = data[x-num];
			} else {
				data[x] = new byte[data[0].length];
			}
		}
	}

	/**
	 * Flatten a 2d array to 1d (sum)
	 * 
	 * @param data
	 * @return
	 */
	public static byte[] flatten(byte[][] data) {
		byte[] ret = new byte[data.length];
		for(int x=0; x<data.length; x++) {
			for(int y=0; y<data[0].length; y++) {
				if (data[x][y] > 0) {
					ret[x] = 1;
					break;
				}
			}
		}
		return ret;
	}
	
	/**
	 * 
	 * @param data
	 */
	public static void filterFirst(byte[][] data) {
		for(int y=0; y<data[0].length; y++) {
			boolean on = false; //(data[0][y] > 0);
			for(int x=0; x<data.length; x++) {
				if (on && data[x][y] == 0) {
					on = false;
				}
				if (on) data[x][y] = 0;
				if (data[x][y] > 0) {
					on = true;
				}
			}
		}
	}
	
	/**
	 * Saves a 2-dimensional array to a png file.
	 * 
	 * @param filename
	 * @param data
	 * @param color
	 * @throws Exception 
	 */
	public static void toImage(String filename, byte[][] data, Color color) throws Exception {
		ArrayToImage img = new ArrayToImage(data.length, data[0].length);
		img.add(data, color);
		img.save(new File(filename));
	}
	
	/**
	 * Surrounds each value > threshold with a copy of it.
	 * Used to broaden data arrays, i.e. to enlarge MIDI notes in spectra
	 * to match their original bandwidth.
	 * 
	 * @param data
	 * @param threshold
	 */
	public static void blur(byte[][] data, final int threshold) {
		byte[][] ind = new byte[data.length][data[0].length];
		for(int x=0; x<data.length; x++) {
			for(int y=0; y<data[0].length; y++) {
				if (data[x][y] > threshold) {
					ind[x][y] = 1;
				}
			}
		}
		for(int x=0; x<data.length; x++) {
			for(int y=0; y<data[0].length; y++) {
				if (ind[x][y] > 0) {
					try {
						data[x-1][y] = data[x][y];
					} catch (Exception e) {}
					try {
						data[x+1][y] = data[x][y];
					} catch (Exception e) {}
					try {
						data[x][y-1] = data[x][y];
					} catch (Exception e) {}
					try {
						data[x][y+1] = data[x][y];
					} catch (Exception e) {}
				}
			}
		}
	}
	
	/**
	 * Uses a scaling instance on all array elements. these have all to be in the range [0,1].
	 * 
	 * @param data
	 * @param scale
	 * @throws Exception
	 */
	public static void scale(final double[][] data, final Scale scale) throws Exception {
	    for(int i=0; i<data.length; i++) {
		    for(int j=0; j<data[i].length; j++) {
		    	data[i][j] = scale.apply(data[i][j]);
		    }
		}
	}
	
	/**
	 * Normalizes a matrix to [0,1]
	 * 
	 * @param in
	 */
	public static void normalize(double[][] data) {
	    normalize(data, 1);
	}
	
	/**
	 * Normalizes a matrix to [0,ceil]
	 * 
	 * @param in
	 * @param ceil
	 */
	public static void normalize(double[][] data, final double ceil) {
	    // Get maximum
	    double max = -Double.MAX_VALUE;
	    for(int i=0; i<data.length; i++) {
		    for(int j=0; j<data[i].length; j++) {
		    	if (data[i][j] > max) max = data[i][j];
		    }
		}
	    for(int i=0; i<data.length; i++) {
		    for(int j=0; j<data[i].length; j++) {
		    	data[i][j]/=max;
		    	if (ceil != 1) data[i][j]*=ceil;
		    }
		}
	}
	
	/**
	 * Normalizes a matrix to [0,1]
	 * 
	 * @param in
	 */
	public static void normalize(float[][] data) {
	    normalize(data, 1);
	}
	
	/**
	 * Normalizes a matrix to [0,ceil]
	 * 
	 * @param in
	 * @param ceil
	 */
	public static void normalize(float[][] data, final float ceil) {
	    // Get maximum
	    double max = Float.MIN_VALUE;
	    for(int i=0; i<data.length; i++) {
		    for(int j=0; j<data[i].length; j++) {
		    	if (data[i][j] > max) max = data[i][j];
		    }
		}
	    for(int i=0; i<data.length; i++) {
		    for(int j=0; j<data[i].length; j++) {
		    	data[i][j]/=max;
		    	if (ceil != 1) data[i][j]*=ceil;
		    }
		}
	}

	/**
	 * Returns the maximum in in.
	 * 
	 * @param in
	 * @return
	 */
	public static double getMaximum(final double[] in) {
		double max = Double.MIN_VALUE;
		for(int i=0; i<in.length; i++) {
			if(in[i] > max) max = in[i];
		}
		return max;
	}

	/**
	 * returns the minimum in in.
	 * 
	 * @param in
	 * @return
	 */
	public static double getMinimum(final double[] in) {
		double min = Double.MAX_VALUE;
		for(int i=0; i<in.length; i++) {
			if(in[i] < min) min = in[i];
		}
		return min;
	}

	/**
	 * Returns the maximum in in.
	 * 
	 * @param in
	 * @return
	 */
	public static byte getMaximum(final byte[][] in) {
		byte max = Byte.MIN_VALUE;
		for(int i=0; i<in.length; i++) {
			for(int j=0; j<in[i].length; j++) {
				if(in[i][j] > max) max = in[i][j];
			}
			
		}
		return max;
	}

	/**
	 * returns the minimum in in.
	 * 
	 * @param in
	 * @return
	 */
	public static byte getMinimum(final byte[][] in) {
		byte min = Byte.MAX_VALUE;
		for(int i=0; i<in.length; i++) {
			for(int j=0; j<in[i].length; j++) {
				if(in[i][j] < min) min = in[i][j];
			}
			
		}
		return min;
	}

	/**
	 * Sets all samples below threshold to zero.
	 * 
	 * @param in
	 * @param threshold
	 */
	public static void gate(int[] in, final int threshold) {
		for (int i=0; i<in.length; i++) {
			if (in[i] < threshold) in[i] = 0;
		}
	}
	
	/**
	 * Sets all samples below threshold to zero.
	 * 
	 * @param in
	 * @param threshold
	 */
	public static void gate(double[][] in, final double threshold) {
		for (int i=0; i<in.length; i++) {
			for (int j=0; j<in[i].length; j++) {
				if (in[i][j] < threshold) in[i][j] = 0;
			}
		}
	}

	/**
	 * Converts a byte array of size 8 to a long integer.
	 * 
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public static long byteArrayToLong(final byte[] data) throws Exception {
		if (data == null || data.length != 8) {
			throw new Exception("Byte array to long conversion takes exactly 8 bytes, not " + data.length);
		}
		return (long)(
				(long)(0xff & data[0]) << 56 |
				(long)(0xff & data[1]) << 48 |
				(long)(0xff & data[2]) << 40 |
				(long)(0xff & data[3]) << 32 |
				(long)(0xff & data[4]) << 24 |
				(long)(0xff & data[5]) << 16 |
				(long)(0xff & data[6]) << 8 |
				(long)(0xff & data[7]) << 0
		);
	}
	
	/**
	 * Converts a long integer to a byte array of the given length <= 8. 
	 * If smaller than 8, eventually data loss will occur.
	 * 
	 * @param l
	 * @param arrayLength
	 * @return
	 * @throws Exception 
	 */
	public static byte[] longToByteArray(final long l, final int arrayLength) throws Exception {
		if (arrayLength > 8) {
			throw new Exception("Cannot convert to byte array longer than 8: " + arrayLength);
		}
		if (arrayLength < 1) {
			throw new Exception("Cannot convert to empty byte array");
		}
		byte[] ret = new byte[arrayLength];
		ByteBuffer bb = ByteBuffer.allocate(8);
		byte[] bba = bb.putLong(l).array();
		for(int i=0; i<ret.length; i++) {
			ret[i] = bba[i+8-arrayLength];
		}
		return ret;
	}
	
	/**
	 * Convert byte array to string representation for debug output.
	 * 
	 * @param in
	 * @return bytes as 2 digit hex representation, separated by space.
	 */
	public static String byteArrayToString(final byte[] in) {
		return byteArrayToString(in, in.length);
	}

	/**
	 * Convert byte array to string representation for debug output.
	 * 
	 * @param in
	 * @param len the amount of bytes to be calculated (default is in.length) 
	 * @return bytes as 2 digit hex representation, separated by space.
	 */
	public static String byteArrayToString(final byte[] in, final int len) {
		String ret = "";
		for(int i=0; i<len; i++) {
			ret+=String.format("%02X", in[i]) + " ";
		}
		return ret;
	}
	
	/**
	 * Converts a two-dimensional array to int[][]
	 * 
	 * @param in
	 * @return the int[][] array
	 */
	public static int[][] toIntArray(final double[][] in) {
		int[][] ret = new int[in.length][in[0].length];
	    for(int i=0; i<in.length; i++) {
		    for(int j=0; j<in[i].length; j++) {
		    	ret[i][j] = (int)in[i][j];
		    }
	    }
	    return ret;
	}
	
	/**
	 * Converts a two-dimensional double array to byte[][]
	 * 
	 * @param in
	 * @return the byte[][] array
	 */
	public static byte[][] toByteArray(final double[][] in) {
		byte[][] ret = new byte[in.length][in[0].length];
	    for(int i=0; i<in.length; i++) {
		    for(int j=0; j<in[i].length; j++) {
		    	ret[i][j] = (byte)in[i][j];
		    }
	    }
	    return ret;
	}

	/**
	 * Print array to System.out.
	 * 
	 * @param in
	 */
	public static <T> void out(T[] in) {
		out(in, 0, in.length-1);
	}

	/**
	 * Print array to System.out.
	 * 
	 * @param in
	 * @param start
	 */
	public static <T> void out(T[] in, int start) {
		out(in, start, in.length-1);
	}
	
	/**
	 * Print array to System.out.
	 * 
	 * @param in
	 * @param start
	 * @param end
	 */
	public static <T> void out(T[] in, int start, int end) {
		System.out.println(toString(in, start, end));
	}
	
	/**
	 * Returns a string representation containing the array entries.
	 * 
	 * @param <T>
	 * @param in
	 * @param start
	 * @param end
	 * @return
	 */
	public static <T> String toString(T[] in, int start, int end) {
		String ret = "";
		for(int i=start; i<=end && i<in.length; i++) {
			ret+= i + ": " + in[i] + "\n";
		}
		return ret;
	}
	
	/**
	 * Returns a string representation containing the array entries.
	 * 
	 * @param in
	 * @return
	 */
	public static <T> String toString(T[] in) {
		return toString(in, 0, in.length-1);
	}

	/**
	 * Print array to System.out.
	 * 
	 * @param in
	 */
	public static void out(int[] in) {
		out(in, 0, in.length-1);
	}

	/**
	 * Print array to System.out.
	 * 
	 * @param in
	 * @param start
	 */
	public static void out(int[] in, int start) {
		out(in, start, in.length-1);
	}
	
	/**
	 * Print array to System.out.
	 * 
	 * @param in
	 * @param start
	 * @param end
	 */
	public static void out(int[] in, int start, int end) {
		System.out.println(toString(in, start, end));
	}

	/**
	 * Returns a string representation containing the array entries.
	 * 
	 * @param in
	 * @param start
	 * @param end
	 * @return
	 */
	public static String toString(int[] in, int start, int end) {
		String ret = "";
		for(int i=start; i<=end && i<in.length; i++) {
			ret += i + ": " + in[i] + "\n";
		}
		return ret;
	}
	
	/**
	 * Returns a string representation containing the array entries.
	 * 
	 * @param in
	 * @return
	 */
	public static String toString(int[] in) {
		return toString(in, 0, in.length-1);
	}
	
	/**
	 * Print array to System.out.
	 * 
	 * @param in
	 */
	public static void out(long[] in) {
		out(in, 0, in.length-1);
	}

	/**
	 * Print array to System.out.
	 * 
	 * @param in
	 * @param start
	 */
	public static void out(long[] in, int start) {
		out(in, start, in.length-1);
	}

	/**
	 * Print array to System.out.
	 * 
	 * @param in
	 * @param start
	 * @param end
	 */
	public static void out(long[] in, int start, int end) {
		System.out.println(toString(in,start, end));
	}

	/**
	 * Returns a string representation containing the array entries.
	 * 
	 * @param in
	 * @return
	 */
	public static String toString(long[] in) {
		return toString(in, 0, in.length-1);
	}

	/**
	 * Returns a string representation containing the array entries.
	 * 
	 * @param in
	 * @param start
	 * @param end
	 * @return
	 */
	public static String toString(long[] in, int start, int end) {
		String ret = "";
		for(int i=start; i<=end && i<in.length; i++) {
			ret+= i + ": " + in[i] + "\n";
		}
		return ret;
	}

	/**
	 * Print array to System.out.
	 * 
	 * @param in
	 */
	public static void out(float[] in) {
		out(in, 0, in.length-1);
	}

	/**
	 * Print array to System.out.
	 * 
	 * @param in
	 * @param start
	 */
	public static void out(float[] in, int start) {
		out(in, start, in.length-1);
	}

	/**
	 * Print array to System.out.
	 * 
	 * @param in
	 * @param start
	 * @param end
	 */
	public static void out(float[] in, int start, int end) {
		System.out.println(toString(in, start, end, true));
	}

	/**
	 * Returns a string representation containing the array entries.
	 * 
	 * @param in
	 * @return
	 */
	public static String toString(float[] in, boolean multiline) {
		return toString(in, 0, in.length-1, multiline);
	}

	/**
	 * Returns a string representation containing the array entries.
	 * 
	 * @param in
	 * @param start
	 * @param end
	 * @return
	 */
	public static String toString(float[] in, int start, int end, boolean multiline) {
		String ret = "";
		for(int i=start; i<=end && i<in.length; i++) {
			ret+= i + ": " + in[i] + (multiline ? "\n" : ", ");
		}
		return ret;
	}

	/**
	 * Print array to System.out.
	 * 
	 * @param in
	 */
	public static void out(double[] in) {
		out(in, 0, in.length-1);
	}

	/**
	 * Print array to System.out.
	 * 
	 * @param in
	 * @param start
	 */
	public static void out(double[] in, int start) {
		out(in, start, in.length-1);
	}

	/**
	 * Print array to System.out.
	 * 
	 * @param in
	 * @param start
	 * @param end
	 */
	public static void out(double[] in, int start, int end) {
		System.out.println(toString(in, start, end));
	}

	/**
	 * Returns a string representation containing the array entries.
	 * 
	 * @param in
	 * @return
	 */
	public static String toString(double[] in) {
		return toString(in, 0, in.length-1);
	}

	/**
	 * Returns a string representation containing the array entries.
	 * 
	 * @param in
	 * @param start
	 * @param end
	 * @return
	 */
	public static String toString(double[] in, int start, int end) {
		String ret = "";
		for(int i=start; i<=end && i<in.length; i++) {
			ret+= i + ": " + in[i] + "\n";
		}
		return ret;
	}

	/**
	 * Shifts all values up or down by minimum to get [0,x] range.
	 * 
	 * @param bs
	 * @return
	 */
	public static byte[][] positivize(byte[][] data) {
		byte[][] ret = new byte[data.length][data[0].length];
		byte min = Byte.MAX_VALUE;
		for(int x=0; x<data.length; x++) {
			for(int y=0; y<data[0].length; y++) {
				if (data[x][y] < min) min = data[x][y];
			}
		}
		for(int x=0; x<data.length; x++) {
			for(int y=0; y<data[0].length; y++) {
				ret[x][y] = (byte)(data[x][y] - min);
			}
		}
		return ret;
	}

	/**
	 * Counts the occurrences of value in data.
	 * 
	 * @param data
	 * @param value
	 * @return
	 */
	public static long countValues(byte[][] data, int value) {
		long ret = 0;
		for(int x=0; x<data.length; x++) {
			for(int y=0; y<data[0].length; y++) {
				if (data[x][y] == value) ret++;
			}
		}
		return ret;
	}

}
