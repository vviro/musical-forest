package de.lmu.dbs.ciaa.midi;

import java.io.File;

/**
 * Generator that renders random MIDI files. 
 * 
 * @author Thomas Weber
 *
 */
public class MIDIRandomGenerator {

	/**
	 * Polyphonic or monophonic rendering
	 */
	boolean polyphonic = false;
	
	/**
	 * length of generated MIDI files in milliseconds
	 */
	long length;
	
	/**
	 * BPM speed
	 */
	int bpm;
	
	/**
	 * Number of random notes to be generated (only in polyphonic mode)
	 */
	int notes;
	
	/**
	 * lowest note number
	 */
	int minNote = MIDI.MIN_NOTE_NUMBER;
	
	/**
	 * highest note number
	 */
	int maxNote = MIDI.MAX_NOTE_NUMBER;
	
	/**
	 * shortest note duration in millisecs
	 */
	long minDuration = 100;
	
	/**
	 * longest note duration in millisecs
	 */
	long maxDuration = 500; 
	
	/**
	 * Creates a polyphonic generator instance.
	 * 
	 * @param length of piece of "music", in milliseconds
	 * @param bpm beats per minute
	 * @param numOfNotes number of random notes to be rendered
	 * @throws Exception 
	 */
	public MIDIRandomGenerator(long length, int bpm, int numOfNotes) throws Exception {
		if (length <= 0) {
			throw new Exception("Invalid length: " + length);
		}
		if (bpm <= 0) {
			throw new Exception("Invalid bpm value: " + bpm);
		}
		this.length = length;
		this.bpm = bpm;
		this.notes = numOfNotes;
		this.polyphonic = true;
	}
	
	/**
	 * Creates a monophonic generator instance.
	 * 
	 * @param length of piece of "music", in milliseconds
	 * @param bpm beats per minute
	 * @throws Exception
	 */
	public MIDIRandomGenerator(long length, int bpm) throws Exception {
		if (length <= 0) {
			throw new Exception("Invalid length: " + length);
		}
		if (bpm <= 0) {
			throw new Exception("Invalid bpm value: " + bpm);
		}
		this.length = length;
		this.bpm = bpm;
		this.polyphonic = false;
	}

	/**
	 * Sets the range parameters.
	 * 
	 * @param minNote lowest note number
	 * @param maxNote highest note number
	 * @param minDuration lowest duration (milliseconds)
	 * @param maxDuration highest duration (milliseconds)
	 * @throws Exception 
	 */
	public void setRanges(int minNote, int maxNote, int minDuration, int maxDuration) throws Exception {
		if (minNote < MIDI.MIN_NOTE_NUMBER) {
			throw new Exception("Invalid midi note number: " + minNote);
		}
		if (minNote > maxNote) {
			throw new Exception("minNote has to be lower than maxNote");
		}
		if (maxNote > MIDI.MAX_NOTE_NUMBER) {
			throw new Exception("Invalid midi note number: " + maxNote);
		}
		if (minDuration > maxDuration) {
			throw new Exception("minDuration has to be lower than maxDuration");
		}
		if (minDuration <= 0) {
			throw new Exception("Invalid minDuration: " + minDuration);
		}
		this.minNote = minNote;
		this.maxNote = maxNote;
		this.minDuration = minDuration;
		this.maxDuration = maxDuration;
	}
	
	/**
	 * Generates the files. File names are created like "midiFilePrefix[index]midiFilePostfix"
	 * 
	 * @param numOfFiles number of files to be created
	 * @param midiFilePrefix
	 * @param midiFilePostfix
	 * @throws Exception 
	 */
	public void process(int numOfFiles, String midiFilePrefix, String midiFilePostfix) throws Exception {	
		MIDIAdapter ma = new MIDIAdapter(bpm); 
		long ticks = (long)(length / ma.getTickLength()); // Num of ticks
		for(int it=0; it<numOfFiles; it++) {
			ma = new MIDIAdapter(bpm);
			if (polyphonic) {
				ma.generateRandomNotesPoly(0, ticks, notes, minNote, maxNote, minDuration, maxDuration); // Generate to track 0
			} else {
				notes = ma.generateRandomNotesMono(0, ticks, minNote, maxNote, minDuration, maxDuration); // Generate to track 0
			}
			String filename = midiFilePrefix + it + midiFilePostfix; 
			ma.writeFile(new File(filename));
		}
	}
}
