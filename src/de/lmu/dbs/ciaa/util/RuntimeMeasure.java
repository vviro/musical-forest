package de.lmu.dbs.ciaa.util;

import java.io.PrintStream;

/**
 * Tool to measure the different parts of processing in a program.
 * 
 * @author Thomas Weber
 *
 */
public class RuntimeMeasure {

	/**
	 * Last timestamp. Updated on construction and measure() calls.
	 */
	private long last;
	
	/**
	 * Output stream. If null, messages are ignored.
	 */
	private PrintStream out = null;
	
	/**
	 * Instance without output stream.
	 */
	public RuntimeMeasure() {
		this(null);
	}

	/**
	 * Instance with output stream for message output.
	 * 
	 * @param out
	 */
	public RuntimeMeasure(final PrintStream out) {
		this.last = System.currentTimeMillis();
		this.out = out;
	}
	
	/**
	 * Measures the time between the last call (or construction).
	 * 
	 * @return milliseconds
	 */
	public long measure() {
		long now = System.currentTimeMillis();
		long diff = now - last;
		last = System.currentTimeMillis();
		return diff;
	}

	/**
	 * Measures the time between the last call (or construction).
	 * If out is not null, the message is printed to out, with postfix ": " plus 
	 * the elapsed time since the last call, formatted in seconds.
	 * 
	 * @param message
	 * @return milliseconds
	 */
	public long measure(final String message) {
		long now = System.currentTimeMillis();
		long diff = now - last;
		if (message != null && out != null) {
			out.println(message + ": " + diff/1000.0 + "sec");
		}
		last = System.currentTimeMillis();
		return diff;
	}
}
