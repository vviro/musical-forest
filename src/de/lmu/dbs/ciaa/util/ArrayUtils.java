package de.lmu.dbs.ciaa.util;

import java.nio.ByteBuffer;

/**
 * Some static array utility methods.
 * 
 * @author Thomas Weber
 *
 */
public class ArrayUtils {
	
	/**
	 * Uses a scaling instance on all array elements. these have all to be in the range [0,1].
	 * 
	 * @param data
	 * @param scale
	 * @throws Exception
	 */
	public static void scale(double[][] data, Scale scale) throws Exception {
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
	public static void normalize(double[][] data, double ceil) {
	    // Get maximum
	    double max = Double.MIN_VALUE;
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
	public static void gate(int[] in, int threshold) {
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
	public static void gate(double[][] in, double threshold) {
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
	public static long byteArrayToLong(byte[] data) throws Exception {
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
	public static byte[] longToByteArray(long l, int arrayLength) throws Exception {
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
	public static String byteArrayToString(byte[] in) {
		return byteArrayToString(in, in.length);
	}

	/**
	 * Convert byte array to string representation for debug output.
	 * 
	 * @param in
	 * @param len the amount of bytes to be calculated (default is in.length) 
	 * @return bytes as 2 digit hex representation, separated by space.
	 */
	public static String byteArrayToString(byte[] in, int len) {
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
	public static int[][] toIntArray(double[][] in) {
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
	public static byte[][] toByteArray(double[][] in) {
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
		for(int i=start; i<=end && i<in.length; i++) {
			System.out.println(i + ": " + in[i]);
		}
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
		for(int i=start; i<=end && i<in.length; i++) {
			System.out.println(i + ": " + in[i]);
		}
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
		for(int i=start; i<=end && i<in.length; i++) {
			System.out.println(i + ": " + in[i]);
		}
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
		for(int i=start; i<=end && i<in.length; i++) {
			System.out.println(i + ": " + in[i]);
		}
	}
}
