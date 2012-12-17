package de.lmu.dbs.musicalforest.midi;

import java.io.File;

/**
 * Generator that renders random MIDI files. Unused in the standard musical forest program.
 * 
 * @author Thomas Weber
 *
 */
public class MIDIRandomGenerator {

	/**
	 * length of generated MIDI files in milliseconds
	 */
	long length;
	
	/**
	 * BPM speed
	 */
	int bpm;
	
	/**
	 * Max polyphony per group
	 */
	public int harmonicComplexity;
	
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
	 * When creating groups, follow the distributions found out by Youngblood (1958) and Hutchinson (1983), taken from
	 * Carol Krumhansl (Cognitive Foundations of Musical Pitch, Oxford Psychology Series, 2001)
	 */
	public boolean musical = false;
	
	/**
	 * Statistics about intervals generated
	 */
	public double[] intervalStats = null;
	
	/**
	 * 
	 * @param length of piece of "music", in milliseconds
	 * @param bpm beats per minute
	 * @throws Exception
	 */
	public MIDIRandomGenerator(final long length, final int bpm, final int harmonicComplexity, boolean musical) throws Exception {
		if (length <= 0) {
			throw new Exception("Invalid length: " + length);
		}
		if (bpm <= 0) {
			throw new Exception("Invalid bpm value: " + bpm);
		}
		this.length = length;
		this.bpm = bpm;
		this.harmonicComplexity = harmonicComplexity;
		this.musical = musical;
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
	public void setRanges(final int minNote, final int maxNote, final long minDuration, final long maxDuration) throws Exception {
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
	public long[] processNonSequential(final int numOfFiles, final String midiFilePrefix, final String midiFilePostfix, int noteCount) throws Exception {	
		MIDIAdapter ma = new MIDIAdapter(bpm); 
		long ticks = (long)(length / ma.getTickLength()); // Num of ticks
		long[] ret = new long[MIDI.MAX_NOTE_NUMBER - MIDI.MIN_NOTE_NUMBER + 1];
		long[] not = null;
		for(int it=0; it<numOfFiles; it++) {
			ma = new MIDIAdapter(bpm);
			not = ma.generateRandomNonSequentialNoteGroups(0, ticks, noteCount, minNote, maxNote, minDuration, maxDuration, harmonicComplexity, musical);
			String filename = midiFilePrefix + it + midiFilePostfix;
			ma.writeFile(new File(filename));
			System.out.println("Created random MIDI file: " + midiFilePrefix + it + midiFilePostfix);
			for(int j=0; j<not.length; j++) {
				ret[j] += not[j];
			}
		}
		intervalStats = ma.intervalStats;
		return ret;
	}

	/**
	 * Generates the files. File names are created like "midiFilePrefix[index]midiFilePostfix".
	 * Returns the amount of created notes, in a histogram array over MIDI notes.
	 * <br><br>
	 * The generation method used here is sequential. Blocks of notes (random chords) are
	 * produced without overlapping.
	 * 
	 * @param numOfFiles number of files to be created
	 * @param midiFilePrefix
	 * @param midiFilePostfix
	 * @throws Exception 
	 */
	public long[] processSequential(final int numOfFiles, final String midiFilePrefix, final String midiFilePostfix, long minPauseBetween, long maxPauseBetween) throws Exception {
		MIDIAdapter ma = new MIDIAdapter(bpm); 
		long ticks = (long)(length / ma.getTickLength()); // Num of ticks
		long[] ret = new long[MIDI.MAX_NOTE_NUMBER - MIDI.MIN_NOTE_NUMBER + 1];
		long[] not = null;
		for(int it=0; it<numOfFiles; it++) {
			ma = new MIDIAdapter(bpm);
			not = ma.generateRandomSequentialNoteGroups(0, ticks, minNote, maxNote, minDuration, maxDuration, minPauseBetween, maxPauseBetween, harmonicComplexity, musical);
			String filename = midiFilePrefix + it + midiFilePostfix;
			ma.writeFile(new File(filename));
			System.out.println("Created random MIDI file: " + midiFilePrefix + it + midiFilePostfix);
			for(int j=0; j<not.length; j++) {
				ret[j] += not[j];
			}
		}
		intervalStats = ma.intervalStats;
		return ret;
	}
}
