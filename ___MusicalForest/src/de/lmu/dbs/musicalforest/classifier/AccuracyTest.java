package de.lmu.dbs.musicalforest.classifier;

import java.io.Serializable;

import gnu.trove.list.TDoubleList;
import de.lmu.dbs.jforest.util.Statistic;

/**
 * Test class to measure accuracy of musical forest.
 * 
 * @author Thomas Weber
 *
 */
public class AccuracyTest implements Serializable  {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Number of notes in reference 
	 */
	public int referenceNotes = 0;
	
	/**
	 * Number of correctly detected notes
	 */
	public int detectedNotesRight = 0;

	/**
	 * Number of wrong detected notes
	 *
	public int undetectedNotes = 0;
	
	/**
	 * Number of detected notes
	 */
	public int detectedThings = 0;
	
	/**
	 * Overall distance of detected notes from their found reference.
	 */
	public Statistic distanceStat = new Statistic();
	
	/**
	 * Window size on x axis (halfed)
	 */
	private int winX;
	
	/**
	 * Window size on y axis (halfed)
	 */
	private int winY;
	
	/**
	 * 
	 * @param winX Window size on x axis
	 * @param winY Window size on y axis
	 */
	public AccuracyTest(int winX, int winY) {
		this.winX = winX/2;
		this.winY = winY/2;
	}
	
	/**
	 * Returns the accuracy of correctly detected notes. 
	 * 
	 * @return [0..1]
	 */
	public double getCorrectDetection() {
		return (double) detectedNotesRight / referenceNotes;
	}
	
	/**
	 * Returns the false detection percentage.
	 * 
	 * @return [0..1] or NaN if no notes were detected
	 */
	public double getFalseDetection() {
		if (detectedThings == 0) return Double.NaN; 
		return (double)(detectedThings - detectedNotesRight) / detectedThings;
	}
	
	/**
	 * Returns a formatted analysis string.
	 * 
	 */
	@Override
	public String toString() {
		return  "    Found notes:       " + getCorrectDetection() + "\n" +
				"    False detection:   " + getFalseDetection() + "\n" +
				"    Distance Stats:    " + distanceStat + "\n" +
				"    Reference notes:   " + referenceNotes + "\n" +
				"    Detected:          " + detectedThings + "\n" +
				"    Detected correct:  " + detectedNotesRight + "\n";
	}
	
	/**
	 * Tests the potential matching between two note arrays.
	 * 
	 * @param modeWeights
	 * @param refOn
	 * @return
	 * @throws Exception 
	 */
	public synchronized void addData(float[][] detected, byte[][] reference) throws Exception {
		//if (detected.length != reference.length) throw new Exception("To test accuracy, the input arrays have to have the same dimensions (X)");
		if (detected[0].length != reference[0].length) throw new Exception("To test accuracy, the input arrays have to have the same dimensions (Y)");

		int maxX = (detected.length > reference.length) ? reference.length : detected.length;
		
		// Found notes
	    for(int x=0; x<maxX; x++) {
	    	for(int y=0; y<reference[0].length; y++) {
	    		if (detected[x][y] > 0) {
	    			detectedThings++;
	    		}
	    		if (reference[x][y] > 0) {
	    			referenceNotes++;
	    			double dist = searchNeighbor(detected, x, y);
	    			if (dist >= 0) {
	    				// Detected a note
	    				detectedNotesRight++;
	    				distanceStat.add(dist);
	    			}
	    		}
	    	}
	    }
	    check();
	}

	/**
	 * Tests the potential matching between two note arrays.
	 * 
	 * @param modeWeights
	 * @param refOn
	 * @return
	 * @throws Exception 
	 */
	public synchronized void addData(byte[][] detected, byte[][] reference) throws Exception {
		//if (detected.length != reference.length) throw new Exception("To test accuracy, the input arrays have to have the same dimensions (X)");
		if (detected[0].length != reference[0].length) throw new Exception("To test accuracy, the input arrays have to have the same dimensions (Y)");

		int maxX = (detected.length > reference.length) ? reference.length : detected.length;
		
		// Found notes
	    for(int x=0; x<maxX; x++) {
	    	for(int y=0; y<reference[0].length; y++) {
	    		if (detected[x][y] > 0) {
	    			detectedThings++;
	    		}
	    		if (reference[x][y] > 0) {
	    			referenceNotes++;
	    			double dist = searchNeighbor(detected, x, y);
	    			if (dist >= 0) {
	    				// Detected a note
	    				detectedNotesRight++;
	    				distanceStat.add(dist);
	    			}
	    		}
	    	}
	    }
	    check();
	}

	/**
	 * Returns the distance to a neighbor.
	 * -1 is returned if no neighbor was found.
	 * 
	 * @param detected
	 * @param x
	 * @param y
	 * @return
	 */
	private double searchNeighbor(float[][] detected, int x, int y) {
	    for(int i=x-winX; i<x+winX && i<detected.length; i++) {
	    	while(i<0) i++;
	    	for(int j=y-winY; j<y+winY && j<detected[0].length; j++) {
	    		while(j<0) j++;
	    		if (detected[i][j] > 0) {
	    			return Math.sqrt((x-i)*(x-i) + (y-j)*(y-j));
	    		}
	    	}
	    }
		return -1;
	}

	/**
	 * Returns the distance to a neighbor.
	 * -1 is returned if no neighbor was found.
	 * 
	 * @param detected
	 * @param x
	 * @param y
	 * @return
	 */
	private double searchNeighbor(byte[][] detected, int x, int y) {
	    for(int i=x-winX; i<x+winX && i<detected.length; i++) {
	    	while(i<0) i++;
	    	for(int j=y-winY; j<y+winY && j<detected[0].length; j++) {
	    		while(j<0) j++;
	    		if (detected[i][j] > 0) {
	    			return Math.sqrt((x-i)*(x-i) + (y-j)*(y-j));
	    		}
	    	}
	    }
		return -1;
	}

	/**
	 * 
	 * @throws Exception
	 */
	private synchronized void check() throws Exception {
		if (detectedNotesRight > detectedThings) throw new Exception("Invalid state: detectedNotesRight = " + detectedNotesRight + " > detectedThings = " + detectedThings);
		double c = getCorrectDetection();
		if (c < 0 || c > 1) throw new Exception("Invalid state: correctDetection = " + c);
		double f = getFalseDetection();
		if (f < 0 || f > 1) throw new Exception("Invalid state: falseDetection = " + f);
	}
	
	/**
	 * Merge the given tests into one.
	 * 
	 * @param tests
	 * @return
	 * @throws Exception 
	 */
	public static AccuracyTest mergeTests(AccuracyTest[] tests) throws Exception {
		AccuracyTest ret = new AccuracyTest(tests[0].winX*2, tests[0].winY*2);
		for(int i=0; i<tests.length; i++) {
			tests[i].check();
			ret.detectedThings+= tests[i].detectedThings;
			ret.detectedNotesRight+= tests[i].detectedNotesRight;
			ret.referenceNotes+= tests[i].referenceNotes;
			TDoubleList e = ret.distanceStat.getEntries();
			for(int j=0; j<e.size(); j++) {
				ret.distanceStat.add(e.get(j));
			}
		}
		ret.check();
		return ret;
	}

}
