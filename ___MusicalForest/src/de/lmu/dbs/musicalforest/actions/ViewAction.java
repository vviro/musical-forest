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
	 * 
	 * @param workingFolder
	 * @param dataFolder
	 */
	public ViewAction(String workingFolder) {
		this.workingFolder = workingFolder;
	}
	
	/**
	 * 
	 */
	@Override
	public void process(RuntimeMeasure m) throws Exception {
		checkFolder(new File(workingFolder));

		File fmeta = new File(workingFolder + File.separator + FOREST_META_FILENAME);
		File dmeta = new File(workingFolder + File.separator + DATA_META_FILENAME);
		if (fmeta.exists()) {
			m.measure("Forest meta file found: " + fmeta.getAbsolutePath(), true);
			ForestMeta forestMeta = null;
			forestMeta = ForestMeta.load(fmeta.getAbsolutePath(), true);
			m.measure(forestMeta.toString(), true);
		}
		if (dmeta.exists()) {
			DataMeta dataMeta = DataMeta.load(dmeta.getAbsolutePath());
			m.measure("Data meta file found: " + dmeta.getAbsolutePath(), true);
			m.measure(dataMeta.toString(), true);
		}
	}
}
