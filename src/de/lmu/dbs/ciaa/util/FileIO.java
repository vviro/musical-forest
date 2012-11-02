package de.lmu.dbs.ciaa.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Generic tool to store and retrieve objects like arrays in files.
 * 
 * @author Thomas Weber
 *
 * @param <T> the type of object to be stored
 */
public class FileIO<T> {

	/**
	 * Save data to file
	 * 
	 * @param filename
	 * @param data
	 * @throws Exception
	 */
	public void save(String filename, T data) throws Exception {
		FileOutputStream fos = new FileOutputStream(filename);
		ObjectOutputStream out = new ObjectOutputStream(fos);
		out.writeObject(data);
		out.flush();
		out.close();
	}

	/**
	 * Load data from file
	 * 
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public T load(String filename) throws Exception {
		FileInputStream fis = new FileInputStream(filename);
		ObjectInputStream in = new ObjectInputStream(fis);
		T ret = (T)in.readObject();
		in.close();
		return ret;
	}
}
