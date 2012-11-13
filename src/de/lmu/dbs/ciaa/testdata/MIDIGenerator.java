package de.lmu.dbs.ciaa.testdata;

import de.lmu.dbs.ciaa.midi.MIDI;
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
		int numOfFiles = 1; 
		boolean polyphonic = true;
		long length = 1000 * 60 * 10; // file length in ms
		int bpm = 120; // beats per minute
		String targetFolder = "testdata/poly/"; // folder for the files (with separator!)
		String targetName = "random_poly";  // name of the new files
		int minNote = 44;
		int maxNote = 100;
		
		// only for polyphonic mode
		double notesPerSecond = 4;
		long maxDurationPoly = 500;
		long minDurationPoly = 200;
		
		// only for monophonic mode
		long maxPauseBetween = 350;
		long maxDuration = 500;
		long minDuration = 130;
		
		try {
			// Create profiling tool
			RuntimeMeasure m = new RuntimeMeasure(System.out);
			if (polyphonic) {
				int numOfNotes = (int)(((double)length / 1000) * notesPerSecond); 
				g = new MIDIRandomGenerator(length, bpm, numOfNotes);
				g.maxDuration = maxDurationPoly;
				g.minDuration = minDurationPoly;
			} else {
				g = new MIDIRandomGenerator(length, bpm, maxPauseBetween);
				g.maxDuration = maxDuration;
				g.minDuration = minDuration;
			}
			g.maxNote = maxNote;
			g.minNote = minNote;
			long[] notes = g.process(numOfFiles, targetFolder + targetName, ".mid");
			m.measure("Finished creating " + numOfFiles + " random MIDI files");
			
			System.out.println("Note distribution:");
			long all = 0;
			for (int i=0; i<notes.length; i++) {
				System.out.println("   Note " + (MIDI.MIN_NOTE_NUMBER+i) + ": " + notes[i]);
				all += notes[i];
			}
			System.out.println("Note count overall: " + all);

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
