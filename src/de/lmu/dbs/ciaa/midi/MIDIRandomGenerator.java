package de.lmu.dbs.ciaa.midi;

import java.io.File;

import de.lmu.dbs.ciaa.util.ArrayUtils;

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
	boolean groups = false;
	
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
	public int notes;
	
	/**
	 * lowest note number
	 */
	public int minNote = MIDI.MIN_NOTE_NUMBER;
	
	/**
	 * highest note number
	 */
	public int maxNote = MIDI.MAX_NOTE_NUMBER;
	
	/**
	 * shortest note duration in millisecs
	 */
	public long minDuration = 100;
	
	/**
	 * longest note duration in millisecs
	 */
	public long maxDuration = 500;
	
	/**
	 * Maximum random pause length between notes (monophonic)
	 */
	public long maxPauseBetween;
	
	public int harmonicComplexity;
	
	/**
	 * Creates a polyphonic generator instance.
	 * 
	 * @param length of piece of "music", in milliseconds
	 * @param bpm beats per minute
	 * @param numOfNotes number of random notes to be rendered
	 * @throws Exception 
	 */
	public MIDIRandomGenerator(final long length, final int bpm, final int numOfNotes) throws Exception {
		if (length <= 0) {
			throw new Exception("Invalid length: " + length);
		}
		if (bpm <= 0) {
			throw new Exception("Invalid bpm value: " + bpm);
		}
		this.length = length;
		this.bpm = bpm;
		this.notes = numOfNotes;
		this.groups = false;
	}
	
	/**
	 * Creates a monophonic generator instance.
	 * 
	 * @param length of piece of "music", in milliseconds
	 * @param bpm beats per minute
	 * @throws Exception
	 */
	public MIDIRandomGenerator(final long length, final int bpm, final int harmonicComplexity, final long maxPauseBetween) throws Exception {
		if (length <= 0) {
			throw new Exception("Invalid length: " + length);
		}
		if (bpm <= 0) {
			throw new Exception("Invalid bpm value: " + bpm);
		}
		this.length = length;
		this.bpm = bpm;
		this.groups = true;
		this.maxPauseBetween = maxPauseBetween;
		this.harmonicComplexity = harmonicComplexity;
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
	public void setRanges(final int minNote, final int maxNote, final int minDuration, final int maxDuration) throws Exception {
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
	 * Generates the files. File names are created like "midiFilePrefix[index]midiFilePostfix".
	 * Returns the amount of created notes, in a histogram array over MIDI notes.
	 * 
	 * @param numOfFiles number of files to be created
	 * @param midiFilePrefix
	 * @param midiFilePostfix
	 * @throws Exception 
	 */
	public long[] process(final int numOfFiles, final String midiFilePrefix, final String midiFilePostfix) throws Exception {	
		MIDIAdapter ma = new MIDIAdapter(bpm); 
		long ticks = (long)(length / ma.getTickLength()); // Num of ticks
		long[] ret = new long[MIDI.MAX_NOTE_NUMBER - MIDI.MIN_NOTE_NUMBER + 1];
		long[] not = null;
		for(int it=0; it<numOfFiles; it++) {
			ma = new MIDIAdapter(bpm);
			if (groups) {
				not = ma.generateRandomNoteGroups(0, ticks, minNote, maxNote, minDuration, maxDuration, maxPauseBetween, harmonicComplexity); // Generate to track 0
			} else {
				not = ma.generateRandomNotesPoly(0, ticks, notes, minNote, maxNote, minDuration, maxDuration); // Generate to track 0
			}
			String filename = midiFilePrefix + it + midiFilePostfix;
			ma.writeFile(new File(filename));
			System.out.println("Created random MIDI file: " + midiFilePrefix + it + midiFilePostfix);
			for(int j=0; j<not.length; j++) {
				ret[j] += not[j];
			}
		}
		return ret;
	}
}
