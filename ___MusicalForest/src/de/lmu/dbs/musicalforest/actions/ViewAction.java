package de.lmu.dbs.musicalforest.actions;

import java.io.File;

import de.lmu.dbs.jspectrum.util.RuntimeMeasure;
import de.lmu.dbs.musicalforest.Action;
import de.lmu.dbs.musicalforest.classifier.DataMeta;
import de.lmu.dbs.musicalforest.classifier.ForestMeta;

/**
 * Action that outputs data about a forest or dataset collection.
 * 
 * @author Thomas Weber
 *
 */
public class ViewAction extends Action {

	/**
	 * CSV File name to export
	 */
	private String csvFile;
	
	/**
	 * File to put note length distribution CSV
	 */
	private String lenFile;
	
	/**
	 * Traverse target recursively
	 */
	private boolean rec;
	
	/**
	 * Output CSV data as double in [0,1] or as percentage (int)
	 */
	private boolean perc;
	
	/**
	 * 
	 * @param workingFolder
	 * @param dataFolder
	 */
	public ViewAction(String workingFolder, String csvFile, String lenFile, boolean rec, boolean perc) {
		this.workingFolder = workingFolder;
		this.csvFile = csvFile;
		this.lenFile = lenFile;
		this.rec = rec;
		this.perc = perc;
	}
	
	/**
	 * 
	 */
	@Override
	public void process(RuntimeMeasure m) throws Exception {
		File wf = new File(workingFolder);
		checkFolder(wf);
		if (!rec) {
			view(m, wf);
		} else {
			viewRec(m, wf);
		}
	}

	/**
	 * 
	 * @param m
	 * @param base
	 * @throws Exception 
	 */
	private void viewRec(RuntimeMeasure m, File base) throws Exception {
		view(m, base);
		
		File[] lst = base.listFiles();
		for(int i=0; i<lst.length; i++) {
			File l = lst[i];
			if (l.getName().startsWith(".")) continue;
			if (!l.isDirectory()) continue;
			
			viewRec(m, l);
		}		
	}
	
	/**
	 * View one forest
	 * 
	 * @param m
	 * @throws Exception 
	 */
	private void view(RuntimeMeasure m, File base) throws Exception {
		File fmeta = new File(base.getAbsolutePath() + File.separator + FOREST_META_FILENAME);
		File dmeta = new File(base.getAbsolutePath() + File.separator + DATA_META_FILENAME);
		if (fmeta.exists()) {
			m.measure("Forest meta file found: " + fmeta.getAbsolutePath(), true);
			ForestMeta forestMeta = null;
			forestMeta = ForestMeta.load(fmeta.getAbsolutePath(), true);
			m.measure(forestMeta.toString(), true);
			
			if (csvFile != null) {
				writeCSV(forestMeta, fmeta, csvFile, perc, forestMeta.maxDepth);
				m.measure("Saved meta rates to CSV: " + csvFile); 
			}
			if (lenFile != null) {
				writeCSVLen(forestMeta, lenFile, perc);
				m.measure("Saved note length distribution to CSV: " + lenFile);
			}
		}
		if (dmeta.exists()) {
			DataMeta dataMeta = DataMeta.load(dmeta.getAbsolutePath());
			m.measure("Data meta file found: " + dmeta.getAbsolutePath(), true);
			m.measure(dataMeta.toString(), true);
		}
	}
	
}
