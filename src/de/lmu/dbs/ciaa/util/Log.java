package de.lmu.dbs.ciaa.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Simple static logger.
 * 
 * @author Thomas Weber
 *
 */
public class Log {
	
	/**
	 * Output buffer
	 */
	private static BufferedWriter out = null; 
	
	/**
	 * Date formatter.
	 */
	protected static SimpleDateFormat timeStampFormatter = new SimpleDateFormat("hh:mm:ss");

	/**
	 * Open a log file (append)
	 * 
	 * @param logfile
	 * @throws IOException
	 */
	public static void open(String logfile) throws IOException {
		FileWriter fstream = new FileWriter(logfile, true);
		out = new BufferedWriter(fstream);
	}
	
	/**
	 * Write a line to the log file.
	 * 
	 * @param message
	 * @throws Exception
	 */
	public static void write(String message) throws Exception {
		if (out == null) throw new Exception("Log not open.");
		out.write(timeStampFormatter.format(new Date()) + ": " + message);
	}
	
	/**
	 * Save the log file to disk.
	 * 
	 * @throws Exception
	 */
	public static void close() throws Exception {
		if (out == null) throw new Exception("Log not open.");
		out.flush();
		out.close();
		out = null;
	}
}
