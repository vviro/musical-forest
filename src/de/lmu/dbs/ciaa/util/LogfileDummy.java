package de.lmu.dbs.ciaa.util;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Logger dummy (does nothing, except if Printstream output is wanted, see write(message, out))
 * 
 * @author Thomas Weber
 *
 */
public class LogfileDummy extends Logfile {
	
	public LogfileDummy(String logfile) throws IOException {
		super(null);
	}

	/**
	 * Write a line to the log file.
	 * 
	 * @param message
	 * @throws Exception
	 */
	@Override
	public void write(String message) throws Exception {
	}
	
	/**
	 * Additionally to write, this also doubles the message to out.
	 * 
	 * @param message
	 * @param out
	 * @throws Exception
	 */
	@Override
	public void write(String message, PrintStream out) throws Exception {
		out.println(message);
	}
	
	/**
	 * Saves the stream to here and reopen to append.
	 * 
	 * @throws Exception
	 */
	@Override
	public void flush() throws Exception {
	}
	
	/**
	 * Save the log file to disk.
	 * 
	 * @throws Exception
	 */
	@Override
	public void close() throws Exception {
	}
}
