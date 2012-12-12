package de.lmu.dbs.jspectrum.util;

import java.io.PrintStream;

/**
 * Tool to measure the different parts of processing in a program.
 * 
 * @author Thomas Weber
 *
 */
public class RuntimeMeasure {

	/**
	 * Suppress output completely.
	 */
	private boolean silent = false;
	
	/**
	 * Start time
	 */
	private long start;
	
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
		this.start = last;
	}

	/**
	 * Switch the silent flag.
	 * 
	 * @param value
	 */
	public void setSilent(boolean value) {
		silent = value;
	}
	
	/**
	 * Measures the time between the last call (or construction).
	 * This version of measure(..) should be used when the method is called
	 * very often.
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
		return measure(message, false);
	}

	/**
	 * Measures the time between the last call (or construction).
	 * If out is not null, the message is printed to out, with postfix ": " plus 
	 * the elapsed time since the last call, formatted in seconds.
	 * 
	 * @param message
	 * @param dontShowTime dont show the time since last call (just return the value)
	 * @return milliseconds
	 */
	public long measure(final String message, boolean dontShowTime) {
		long now = System.currentTimeMillis();
		long diff = now - last;
		if (message != null && out != null && !silent) {
			if (dontShowTime) {
				out.println(message);
			} else {
				out.println(message + ": " + diff/1000.0 + "sec");
			}
		}
		last = System.currentTimeMillis();
		return diff;
	}

	/**
	 * Outputs a message and appends the total execution time since
	 * instance creation.
	 * 
	 * @param message
	 * @return milliseconds
	 */
	public long finalMessage(String message) {
		long now = System.currentTimeMillis();
		long diff = now - start;
		if (message != null && out != null && !silent) {
			out.println(message + ": " + diff/1000.0 + "sec");
		}
		last = System.currentTimeMillis();
		return diff;
	}
}
