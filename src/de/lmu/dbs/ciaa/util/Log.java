package de.lmu.dbs.ciaa.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Simple logger.
 * 
 * @author Thomas Weber
 *
 */
public class Log {
	
	/**
	 * Output buffer
	 */
	private BufferedWriter out = null; 
	
	/**
	 * Log file name
	 */
	private String filename;
	
	/**
	 * Date formatter.
	 */
	protected static SimpleDateFormat timeStampFormatter = new SimpleDateFormat("hh:mm:ss");

	/**
	 * Open a new log file
	 * 
	 * @param logfile
	 * @throws IOException
	 */
	public Log(String logfile) throws IOException {
		this(logfile, false);
	}
	
	/**
	 * Open a log file
	 * 
	 * @param logfile
	 * @param append
	 * @throws IOException
	 */
	public Log(String logfile, boolean append) throws IOException {
		if (logfile == null) return;
		FileWriter fstream = new FileWriter(logfile, append);
		this.out = new BufferedWriter(fstream);
		this.filename = logfile;
	}

	/**
	 * Write a line to the log file.
	 * 
	 * @param message
	 * @throws Exception
	 */
	public synchronized void write(String message) throws Exception {
		if (out == null) throw new Exception("Log not open.");
		String msg = timeStampFormatter.format(new Date()) + ": " + message;
		out.write(msg + "\n");
	}
	
	/**
	 * Additionally to write, this also doubles the message to out.
	 * 
	 * @param message
	 * @param out
	 * @throws Exception
	 */
	public synchronized void write(String message, PrintStream out) throws Exception {
		write(message);
		out.println(message);
	}
	
	/**
	 * Saves the stream to here and reopen to append.
	 * 
	 * @throws Exception
	 */
	public synchronized void flush() throws Exception {
		close();
		FileWriter fstream = new FileWriter(filename, true);
		this.out = new BufferedWriter(fstream);
	}
	
	/**
	 * Save the log file to disk.
	 * 
	 * @throws Exception
	 */
	public synchronized void close() throws Exception {
		if (out == null) throw new Exception("Log not open.");
		out.flush();
		out.close();
		out = null;
	}
}
