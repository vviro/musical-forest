package de.lmu.dbs.ciaa.util;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Filename filter that bases upon postfix/file extension.
 * 
 * @author Thomas Weber
 *
 */
public class PostfixFilenameFilter implements FilenameFilter {

	private String postfix;
	
	public PostfixFilenameFilter(String postfix) {
		this.postfix = postfix;
	}
	
	public boolean accept(File dir, String filename) {
		return filename.endsWith(postfix);
	}

}
