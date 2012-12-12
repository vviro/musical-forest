package de.lmu.dbs.musicalforest.actions;

import java.io.File;

import org.apache.commons.io.FileUtils;

import de.lmu.dbs.jspectrum.util.RuntimeMeasure;
import de.lmu.dbs.musicalforest.Action;
import de.lmu.dbs.musicalforest.classifier.ForestMeta;
import de.lmu.dbs.musicalforest.classifier.ForestMetaException;

/**
 * Action that merges multiple forests into one. You have to optimize 
 * thresholds after that, using "threshold" action!
 * 
 * @author Thomas Weber
 *
 */
public class MergeForestsAction extends Action {

	/**
	 * Ignore warnings about missing meta files
	 */
	private boolean force;
	
	/**
	 * Internal index
	 */
	private int nextIndex = 0;
	
	/**
	 * Has the metadata already been copied?
	 */
	private boolean metaCollected;
	
	/**
	 * 
	 */
	private ForestMeta referenceMeta;

	/**
	 * 
	 * @param workingFolder
	 * @param dataFolder
	 */
	public MergeForestsAction(String workingFolder, String dataFolder, boolean force) {
		this.dataFolder = dataFolder;
		this.workingFolder = workingFolder;
		this.force = force;
	}
	
	/**
	 * 
	 */
	@Override
	public void process(RuntimeMeasure m) throws Exception {
		File df = new File(dataFolder);
		checkFolder(df);
		checkFolder(new File(workingFolder));
		
		nextIndex = 0;
		metaCollected = false;
		referenceMeta = null;
		int num = travelFolder(m, df);

		if (nextIndex != num) throw new Exception("Corrupt state: number of merged trees: " + num + " != nextIndex: " + nextIndex);
		
		m.setSilent(false);
		m.finalMessage("Finished merging " + num + " trees in");
	}

	/**
	 * 
	 * @param m
	 * @param dataFolder
	 */
	private int travelFolder(RuntimeMeasure m, File dir) throws Exception {
		m.measure("Travelling directory: " + dir.getAbsolutePath());
		File[] files = dir.listFiles();
		int ret = 0;
		for(int i=0; i<files.length; i++) {
			File f = files[i];
			if (f.getName() == "." || f.getName() == "..") continue;
			if (f.isFile()) {
				if (f.getName().startsWith(NODEDATA_FILE_PREFIX)) {
					int index = -1;
					try {
						index = Integer.parseInt(f.getName().substring(NODEDATA_FILE_PREFIX.length()));
					} catch (Exception e) {
						index = -2;
					}
					if (index >= 0) {
						collectTree(m, f);
						ret++;
					}
				}
			}
			if (f.isDirectory()) {
				ret += travelFolder(m, f);
			}
		}
		return ret;
	}

	/**
	 * Collects one tree to the working folder
	 * 
	 * @param m
	 * @param f
	 */
	private void collectTree(RuntimeMeasure m, File f) throws Exception {
		m.measure("Collecting tree " + nextIndex + " from " + f.getAbsolutePath());

		File dest = new File(workingFolder + File.separator + NODEDATA_FILE_PREFIX + nextIndex);
		FileUtils.copyFile(f, dest);
		nextIndex++;
		
		// Check meta integrity
		File meta = new File(f.getParent() + File.separator + FOREST_META_FILENAME);
		File metaTarget = new File(workingFolder + File.separator + FOREST_META_FILENAME);

		if (!meta.exists()) {
			if (force) {
				System.err.println("WARNING: Forest in " + f.getParent() + " has no meta data");
			} else {
				throw new ForestMetaException("Forest in " + f.getParent() + " has no meta data");
			}
		}
		ForestMeta metaObject = loadForestMeta(f.getParent(), force);
		
		// Collect metadata on first
		if (!metaCollected) {
			referenceMeta = metaObject;
			referenceMeta.forestParams = null;
			referenceMeta.save(metaTarget.getAbsolutePath(), true);
			metaCollected = true;
		} else {
			// Compare settings
			if (metaObject != null && !referenceMeta.compareTo(metaObject, true)) {
				if (force) {
					System.err.println("WARNING: The forest in " + f.getParent() + " contains transformation settings not compatible with the others, remove it from the data folder");
				} else {
					throw new ForestMetaException("The forest in " + f.getParent() + " contains transformation settings not compatible with the others, remove it from the data folder");
				}
			}
		}
	}

}
