package de.lmu.dbs.musicalforest.actions;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

import de.lmu.dbs.jspectrum.util.RuntimeMeasure;
import de.lmu.dbs.musicalforest.Action;
import de.lmu.dbs.musicalforest.classifier.DataMeta;
import de.lmu.dbs.musicalforest.classifier.ForestMeta;

/**
 * Change meta data fields
 * <br><br>
 * TODO: Not all fields are included yet 
 * 
 * @author Thmas Weber
 *
 */
public class ModifyAction extends Action {

	/**
	 * Field name of field to change
	 */
	private String field;
	
	/**
	 * New field value
	 */
	private String value;
	
	/**
	 * 0: ForestMeta; 1: DataMeta
	 */
	private int mode;
	
	/**
	 * 
	 * @param workingFolder
	 * @param dataFolder
	 * @throws Exception 
	 */
	public ModifyAction(String workingFolder, int mode, String field, String value) throws Exception {
		this.workingFolder = workingFolder;
		this.field = field;
		this.value = value;
		this.mode = mode;
		if (mode < 0 || mode > 2) throw new Exception("Invalid mode: " + mode);
	}
	
	/**
	 * 
	 */
	@Override
	public void process(RuntimeMeasure m) throws Exception {
		checkFolder(new File(workingFolder));

		if (mode == 0) {
			// Forest data root
			File fmeta = new File(workingFolder + File.separator + FOREST_META_FILENAME);
			if (fmeta.exists()) {
				m.measure("Forest meta file found: " + fmeta.getAbsolutePath(), true);
				ForestMeta forestMeta = ForestMeta.load(fmeta.getAbsolutePath(), true);
				setAttribute(forestMeta, field, value);
				forestMeta.save(fmeta.getAbsolutePath(), true);
				m.measure(forestMeta.toString(), true);
			} else throw new Exception("Forest meta file not found in " + workingFolder);
		}
		if (mode == 2) {
			// Forest data dataMeta
			File fmeta = new File(workingFolder + File.separator + FOREST_META_FILENAME);
			if (fmeta.exists()) {
				m.measure("Forest meta file found: " + fmeta.getAbsolutePath(), true);
				ForestMeta forestMeta = ForestMeta.load(fmeta.getAbsolutePath(), true);
				setAttribute(forestMeta.dataMeta, field, value);
				forestMeta.save(fmeta.getAbsolutePath(), true);
				m.measure(forestMeta.toString(), true);
			} else throw new Exception("Forest meta file not found in " + workingFolder);
		}
		if (mode == 1) {
			// Data
			File dmeta = new File(workingFolder + File.separator + DATA_META_FILENAME);
			if (dmeta.exists()) {
				DataMeta dataMeta = DataMeta.load(dmeta.getAbsolutePath());
				m.measure("Data meta file found: " + dmeta.getAbsolutePath(), true);
				setAttribute(dataMeta, field, value);
				dataMeta.save(dmeta.getAbsolutePath());
				m.measure(dataMeta.toString(), true);
			} else throw new Exception("Data meta file not found in " + workingFolder);
		}
	}

	/**
	 * Sets a field of an object.
	 * 
	 * @param o
	 * @param field2
	 * @param value2
	 * @throws NoSuchFieldException 
	 * @throws SecurityException 
	 */
	private void setAttribute(Object o, String field2, String value2) throws Exception {
		Class<?> c = o.getClass();
		Field f = c.getDeclaredField(field2);
		Type t = f.getGenericType();
		f.setAccessible(true);
		
		if (t.toString().equals("double")) {
			f.set(o, Double.parseDouble(value2));
		}
		if (t.toString().equals("float")) {
			f.set(o, Float.parseFloat(value2));
		}
		if (t.toString().equals("long")) {
			f.set(o, Long.parseLong(value2));
		}
		if (t.toString().equals("int")) {
			f.set(o, Integer.parseInt(value2));
		}
		if (t.toString().equals("boolean")) {
			f.set(o, Boolean.parseBoolean(value2));
		}
		if (t.toString().equals("String")) {
			f.set(o, value2);
		}
	}
}
