package de.lmu.dbs.ciaa.testdata;

import de.lmu.dbs.ciaa.midi.MIDIRandomGenerator;
import de.lmu.dbs.ciaa.util.RuntimeMeasure;

/**
 * Program that generates random MIDI files, polyphonic or monophonic.
 * <br><br>
 * TODO: Implement command line argument parser...at the moment, all params have to be hard coded.  
 * 
 * @author Thomas Weber
 *
 */
public class MIDIGenerator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MIDIRandomGenerator g;
		
		// Params
		int numOfFiles = 20; 
		boolean polyphonic = true;
		int numOfNotes = 50; // only for polyphonic mode
		long length = 6000; // file length in ms
		int bpm = 120; // beats per minute
		String targetFolder = "testdata/poly/midi/"; // folder for the files (with separator!)
		String targetName = "poly";  // name of the new files
		
		try {
			// Create profiling tool
			RuntimeMeasure m = new RuntimeMeasure(System.out);
			if (polyphonic) {
				g = new MIDIRandomGenerator(length, bpm, numOfNotes);
			} else {
				g = new MIDIRandomGenerator(length, bpm);
			}
			g.process(numOfFiles, targetFolder + targetName, ".mid");
			m.measure("Finished creating " + numOfFiles + " random MIDI files");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Outputs the message to the console 
	 * 
	 * @param message the message
	 */
	@SuppressWarnings("unused")
	private void out(String message) {
		System.out.println(message);
	}
	
}
