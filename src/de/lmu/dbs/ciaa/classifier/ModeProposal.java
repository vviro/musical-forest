package de.lmu.dbs.ciaa.classifier;

/**
 * Proposal of reference positions in classifier output arrays.
 * 
 * @author Thomas Weber
 *
 */
public class ModeProposal {

	/**
	 * Propose.
	 * 
	 * @param data
	 * @return
	 */
	public float[][] propose(float[][] data) {
		float[][] ret = new float[data.length][data[0].length];
		for(int x=0; x<data.length; x++) {
			for(int y=0; y<data[0].length; y++) {		
				ret[x][y] = propose(data, x, y);
			}
		}
		return ret;
	}
	
	/**
	 * 
	 * @param data
	 * @param x
	 * @param y
	 * @return
	 */
	public float propose(float[][] data, int x, int y) {
		return 0;
	}

}
