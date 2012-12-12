package de.lmu.dbs.musicalforest.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * Tools for external calls.
 * 
 * @author Thomas Weber
 *
 */
public class ExtTools {
	
	/**
	 * 
	 */
	private PrintStream out = null;
	
	/**
	 * 
	 * @param out
	 */
	public ExtTools(PrintStream out) {
		this.out = out;
	}
	
	/**
	 * Executes commands and captures the ouput of them.
	 * 
	 * @param cmd
	 * @throws Exception 
	 */
	public void exec(String cmd) throws Exception {
		String line;
		Process p = Runtime.getRuntime().exec(cmd);
		BufferedReader bri = new BufferedReader (new InputStreamReader(p.getInputStream()));
		BufferedReader bre = new BufferedReader (new InputStreamReader(p.getErrorStream()));
		while ((line = bri.readLine()) != null) {
			out.println(line);
		}
		bri.close();
		while ((line = bre.readLine()) != null) {
			out.println(line);
		}
		bre.close();
		p.waitFor();
	}
}
