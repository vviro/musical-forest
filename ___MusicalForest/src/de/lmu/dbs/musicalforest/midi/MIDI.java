package de.lmu.dbs.musicalforest.midi;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import midiReference.MidiReference;

import de.lmu.dbs.jspectrum.util.ArrayUtils;
import de.lmu.dbs.jspectrum.util.RandomUtils;

/**
 * MIDI class. Can open MIDI files, or create new ones.
 * 
 * @author Thomas Weber
 *
 */
public class MIDI {

	/**
	 * Midi sequence from midi file
	 */
	protected Sequence sequence;
	
	/**
	 * Tracks from the midi sequence
	 */
	protected Track[] tracks;
	
	/**
	 * Ticks per quarter note
	 */
	protected int ticksPerQuarter;
	
	/**
	 * Contains all tempo changes, in microseconds per quarter note, 
	 * indexed by the tick where the change happens
	 */
	protected List<MIDITempoChange> tempoChanges;

	/**
	 * Default value for tempo change (equals 120 bpm)
	 */
	public static final long DEFAULT_TICKS_PER_QUARTER = 500000l;
	
	/**
	 * Number of microseconds per minute
	 */
	public static final long MICROSECONDS_PER_MINUTE = 60000000l;
	
	/**
	 * Lowest possible MIDI note number (A0)
	 */
	public static final int MIN_NOTE_NUMBER = 21;

	/**
	 * Highest possible MIDI note number (C8)
	 */
	public static final int MAX_NOTE_NUMBER = 108;

	/**
	 * MidiReference implementation (by Grant Muller) to convert midi <-> frequency etc. 
	 */
	protected MidiReference midiRef = MidiReference.getMidiReference();
	
	/**
	 * Creates a MIDI instance from a midi file
	 * 
	 * @param file
	 * @throws Exception
	 */
	public MIDI(final File file) throws Exception {
		sequence = MidiSystem.getSequence(file);
		if (sequence == null) {
			throw new Exception("No sequence data could be extracted from file " + file.getAbsolutePath());
		}
		tracks = sequence.getTracks();
		if (tracks == null) {
			throw new Exception("No tracks found in the MIDI sequence of file " + file.getAbsolutePath());
		}
		if (sequence.getDivisionType() != Sequence.PPQ) {
			throw new Exception("MIDI file " + file.getAbsolutePath() + " contains SMPTE code. Only PPQ is allowed here.");
		}
		this.ticksPerQuarter = sequence.getResolution();
		tempoChanges = getTempoChanges();
	}
	
	/**
	 * Creates an empty MIDI instance to build a new midi file
	 * 
	 * @throws Exception 
	 */
	public MIDI(final double bpm) throws Exception {
		this(bpm, 480);
	}
	
	/**
	 * Creates an empty MIDI instance to build a new midi file
	 * 
	 * @throws Exception 
	 */
	public MIDI(final double bpm, final int tpq) throws Exception {
		// Create sequence and initial track
		this.ticksPerQuarter = tpq;
		sequence = new Sequence(javax.sound.midi.Sequence.PPQ, ticksPerQuarter);
		sequence.createTrack();
		tracks = sequence.getTracks();
		if (tracks == null) {
			throw new Exception("Error creating initial track of MIDI sequence");
		}
		// Set initial tempo at tick 0
		setMetaMessage(0, (byte)0x51, ArrayUtils.longToByteArray(bpmToTpq(bpm), 3));
		tempoChanges = getTempoChanges();
	}
	
	/**
	 * Sets a meta message (tempo change etc., starting with 0xFF) to track 0.
	 * 
	 * @param tick the position in time for the message
	 * @param cmd the command byte (i.e. 0x51 for tempo change)
	 * @param data data array (containing only the data bytes, no status/length etc.)
	 * @throws Exception
	 */
	public void setMetaMessage(final long tick, final byte cmd, final byte[] data) throws Exception {
		setMetaMessage(tick, cmd, data, 0);
	}
	
	/**
	 * Sets a meta message (tempo change etc., starting with 0xFF)
	 * 
	 * @param tick the position in time for the message
	 * @param cmd the command byte (i.e. 0x51 for tempo change)
	 * @param data data array (containing only the data bytes, no status/length etc.)
	 * @param track the track number to add the message
	 * @throws Exception
	 */
	public void setMetaMessage(final long tick, final byte cmd, final byte[] data, final int track) throws Exception {
		if (track >= tracks.length) {
			throw new Exception("MIDI Track " + track + " does not exist");
		}
		Track t = tracks[track];
		MetaMessage msg = new MetaMessage();
		msg.setMessage(cmd, data, data.length);
		MidiEvent event = new MidiEvent(msg, tick);
		t.add(event);
	}
	
	/**
	 * Sets one note in track 0, completely with note off message. 
	 * Note off velocity is set to 0.
	 * 
	 * @param tick start tick
	 * @param duration duration in ticks
	 * @param noteName midi note name, like "c3"
	 * @param velocity velocity of the note
	 * @throws Exception
	 */
	public void setNote(final long tick, final long duration, final String noteName, final int velocity) throws Exception {
		setNote(tick, duration, noteName, velocity, 0);
	}

	/**
	 * Sets one note in a track, completely with note off message. 
	 * Note off velocity is set to 0.
	 * 
	 * @param tick start tick
	 * @param duration duration in ticks
	 * @param noteName midi note name, like "c3"
	 * @param velocity velocity of the note
	 * @param track track to write to
	 * @throws Exception
	 */
	public void setNote(final long tick, final long duration, final String noteName, final int velocity, final int track) throws Exception {
		int note = 0;
		try {
			note = midiRef.getNoteNumber(noteName);
		} catch (NullPointerException e) {
			throw new Exception("Invalid MIDI note name: " + noteName + "; Use Notation like C3, Bb4, F#2 etc.");
		}
		setNote(tick, duration, note, velocity, track);
	}
	
	/**
	 * Sets one note in track 0, completely with note off message.
	 * 
	 * @param tick start tick
	 * @param duration duration in ticks
	 * @param note midi note value
	 * @param velocity velocity of the note
	 * @throws Exception
	 */
	public void setNote(final long tick, final long duration, final int note, final int velocity) throws Exception {
		setNote(tick, duration, note, velocity, 0);
	}

	/**
	 * Sets one note in a track, completely with note off message. 
	 * Note off velocity is set to 0.
	 * 
	 * @param tick start tick
	 * @param duration duration in ticks
	 * @param note midi note value
	 * @param velocity velocity of the note
	 * @param track track to write to
	 * @throws Exception
	 */
	public void setNote(final long tick, final long duration, final int note, final int velocity, final int track) throws Exception {
		if (track >= tracks.length) {
			throw new Exception("MIDI Track " + track + " does not exist");
		}
		if (note < 0 || note > 127) {
			throw new Exception("Invalid MIDI note value: " + note);
		}
		if (velocity < 0 || velocity > 127) {
			throw new Exception("Invalid velocity value: " + velocity);
		}
		if (duration <= 0) {
			throw new Exception("Invalid note duration: " + duration);
		}
		// Note on
		ShortMessage sm = new ShortMessage();
		int status = 0x90 | track;
		sm.setMessage(status, (byte)note, (byte)velocity);
		MidiEvent event = new MidiEvent(sm, tick);
		tracks[track].add(event);
		// Note off
		sm = new ShortMessage();
		status = 0x80 | track;
		sm.setMessage(status, (byte)note, 0);
		event = new MidiEvent(sm, tick + duration);
		tracks[track].add(event);
	}
	
	/**
	 * Extracts all MIDI tempo change events. See attribute tempoChanges.
	 * 
	 * @return
	 * @throws Exception 
	 */
	public List<MIDITempoChange> getTempoChanges() throws Exception {
		List<MIDITempoChange> ret = new ArrayList<MIDITempoChange>();
		for (int i=0; i<tracks.length; i++) {
			Track track = tracks[i];
			for (int j=0; j<track.size(); j++) {
				MidiMessage midiMessage = track.get(j).getMessage();
				byte[] message = midiMessage.getMessage();
				if (checkMessageType(message, MIDIMessageHeaders.MESSAGE_TEMPOCHANGE)) {
					long tpq = getMessageDataAsLong(message, midiMessage.getLength(), MIDIMessageHeaders.MESSAGE_TEMPOCHANGE);
					ret.add(new MIDITempoChange(track.get(j).getTick(), tpq));
				}
			}
		}
		if (ret.size() == 0) {
			// Default speed (120 bpm)
			ret.add(new MIDITempoChange(0, DEFAULT_TICKS_PER_QUARTER));
		}
		return ret;
	}
	
	/**
	 * Returns the tempo in BPM.
	 * 
	 * @return
	 * @throws Exception
	 */
	public double getInitialTempo() throws Exception {
		List<MIDITempoChange> c = getTempoChanges();
		return MIDI.tpqToBpm(c.get(0).getMicrosPerQuarter());
	}
	
	/**
	 * Generates polyphonic random notes. Returns the amount
	 * of created notes, in a histogram array over MIDI notes.
	 * 
	 * @param track the target track for the notes
	 * @param length length of sample in ticks
	 * @param notesCount number of notes to render
	 * @param minNote lowest note
	 * @param maxNote highest note
	 * @param minDuration lowest duration
	 * @param maxDuration highest duration
	 * @throws Exception
	 */
	public long[] generateRandomNotesPoly(final int track, final long length, final int notesCount, final int minNote, final int maxNote, final long minDuration, final long maxDuration) throws Exception {
		long[] ret = new long[MAX_NOTE_NUMBER - MIN_NOTE_NUMBER + 1];
		for(int i=0; i<notesCount; i++) { 
			int note = RandomUtils.randomInt(minNote, maxNote);
			long duration = RandomUtils.randomLong(minDuration, maxDuration);
			long start = RandomUtils.randomLong(length - duration);
			setNote(start, duration, note, 127, track);
			ret[note - MIN_NOTE_NUMBER]++;
		}
		return ret;
	}
	
	/**
	 * Generates polyphonic random note groups. Returns the amount
	 * of created notes, in a histogram array over MIDI notes.
	 * 
	 * @param track the target track for the notes
	 * @param length length of sample in ticks
	 * @param notesCount number of notes to render
	 * @param minNote lowest note
	 * @param maxNote highest note
	 * @param minDuration lowest duration
	 * @param maxDuration highest duration
	 * @throws Exception
	 */
	public long[] generateRandomNoteGroups(final int track, final long length, final int minNote, final int maxNote, final long minDuration, final long maxDuration, final long maxPauseBetween, final int harmonicComplexity) throws Exception {
		if (harmonicComplexity < 1) throw new Exception("Harmonic complexity below min value 1: " + harmonicComplexity);
		long[] ret = new long[MAX_NOTE_NUMBER - MIN_NOTE_NUMBER + 1];
		long pos = 0;
		while(pos < length-1) {
			long duration = RandomUtils.randomLong(minDuration, maxDuration);
			if (pos + duration >= length) duration = length - pos - 1;
			int amt = RandomUtils.randomInt(1, harmonicComplexity);
			int lastNote = -1;
			for(int i=0; i<amt; i++) {
				int note = lastNote;
				while(note == lastNote) {
					note = RandomUtils.randomInt(minNote, maxNote);
				}
				lastNote = note;
				setNote(pos, duration, note, 127, track);
				ret[note - MIN_NOTE_NUMBER]++;
			}
			pos+= duration + RandomUtils.randomLong(maxPauseBetween);
		}
		return ret;
	}
	
	/**
	 * Generates monophonic random notes. Divides the length into random chunks. Returns
	 * the amount of created notes, in a histogram array over MIDI notes.
	 * 
	 * @param track the target track for the notes
	 * @param length length of sample in ticks
	 * @param minNote lowest note
	 * @param maxNote highest note
	 * @param minDuration lowest duration
	 * @param maxDuration highest duration
	 * @throws Exception
	 * @return number of created notes
	 */
	public long[] generateRandomNotesMono(final int track, final long length, final int minNote, final int maxNote, final long minDuration, final long maxDuration, final long maxPauseBetween) throws Exception {
		long[] ret = new long[MAX_NOTE_NUMBER - MIN_NOTE_NUMBER + 1];
		long pos = 0;
		while(pos < length-1) {
			int note = RandomUtils.randomInt(minNote, maxNote);
			long duration = RandomUtils.randomLong(minDuration, maxDuration);
			if (pos + duration >= length) duration = length - pos - 1;
			setNote(pos, duration, note, 127, track);
			pos+= duration + RandomUtils.randomLong(maxPauseBetween);
			ret[note - MIN_NOTE_NUMBER]++;
		}
		return ret;
	}
	/**
	 * Writes the midi data of the instance to a file. MIDI files of type 1 are written exclusively.
	 * 
	 * @param file
	 * @throws IOException
	 */
	public void writeFile(File file) throws IOException {
		MidiSystem.write(sequence, 1, file);
	}
	
	/**
	 * Detects if the event is of a certain type, which is given as its MIDI byte prefix.
	 * 
	 * @param event MIDI event instance (can be extracted i.e. from a Track instance)
	 * @param header i.e. {0xFF, 0x51, 0x03} for tempo change events
	 * @return
	 */
	protected boolean checkMessageType(final byte[] message, final byte[] header) {
		return checkMessageType(message, header, false);
	}

	/**
	 * Detects if the event is of a certain type, which is given as its MIDI byte prefix.
	 * 
	 * @param event MIDI event instance (can be extracted i.e. from a Track instance)
	 * @param header i.e. {0xFF, 0x51, 0x03} for tempo change events
	 * @param multiChannel boolean if the first header byte contains the channel 
	 *                     (only the first half of the first header byte is used)
	 * @return
	 */
	protected boolean checkMessageType(final byte[] message, final byte[] header, final boolean multiChannel) {
		for(int i=0; i<header.length && i<message.length; i++) {
			if (multiChannel && i==0) {
				if (message[i] >> 4 != header[i] >> 4) return false;
			} else {
				if (message[i] != header[i]) return false;
			}
		}
		return true;
	}
	
	/**
	 * Returns the data part of the message as long integer.
	 * 
	 * @param message MIDI message
	 * @param msgLength length of message (can be different form message.length)
	 * @param header header bytes (only the length of the header is really 
	 *               used, values are irrelevant for this method)
	 * @return
	 * @throws Exception 
	 */
	protected long getMessageDataAsLong(final byte[] message, final int msgLength, final byte[] header) throws Exception {
		byte[] bb = new byte[8];
		int ind=7;
		for(int i=msgLength-1; i>=header.length; i--) {
			bb[ind] = message[i];
			ind--;
			if (ind < 0) {
				throw new Exception("Too much bytes to convert to long integer");
			}
		}
		return ArrayUtils.byteArrayToLong(bb);
	}
	
	/**
	 * Returns the length of one tick in milliseconds
	 * <br><br>
	 * TODO: Currently this ignores all tempo change events except for the initial one
	 * 
	 * @return
	 */
	public double getTickLength() {
		double timePerQuarter = (double)tempoChanges.get(0).getMicrosPerQuarter()/1000;
		return (double)timePerQuarter / ticksPerQuarter;
	}

	/**
	 * Conversion from ticks per quarter to beats per minute
	 * 
	 * @param tpq
	 * @return
	 */
	public static double tpqToBpm(final long tpq) {
		return (double)MICROSECONDS_PER_MINUTE / tpq; 
	}
	
	/**
	 * Conversion form beats per minute to ticks per quarter.
	 * 
	 * @param bpm
	 * @return
	 */
	public static long bpmToTpq(final double bpm) {
		return (long)((double)MICROSECONDS_PER_MINUTE / bpm);
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Debugging / Info /////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Print sequence information to a PrintStream for debugging.
	 * 
	 * @param out
	 */
	public void printSequenceInfo(final PrintStream out) {
		out.println("MIDI Sequence info:");
		out.println("--> Length: " + sequence.getTickLength() + " ticks" );
		out.println("--> Duration: " + sequence.getMicrosecondLength() + " microseconds (" + (double)sequence.getMicrosecondLength()/1000000 + " seconds)");
		out.println("--> Ticks per quarter note: " + sequence.getResolution());
		out.println("--> Tempo Changes (" + tempoChanges.size() + "):");
		for(int i=0; i<tempoChanges.size(); i++) {
			MIDITempoChange tc = tempoChanges.get(i);
			out.println("    Tick " + tc.getTick() + ": " + tc.getMicrosPerQuarter() + " microsecs (" + tpqToBpm(tc.getMicrosPerQuarter()) + " bpm)");
		}
	}
	
	/**
	 * Print track information to a PrintStream for debugging.
	 * 
	 * @param out
	 */
	public void printTracks(final PrintStream out) {
		out.println("MIDI Track info:");
		out.println("--> Number of tracks: " + tracks.length);
		for (int i=0; i<tracks.length; i++) {
			Track track = tracks[i];
			out.println("--> Track " + i + " (size: " + track.size() + "):");
			for (int j=0; j<track.size(); j++) {
				// Show bytes
				MidiEvent event = track.get(j);
				System.out.println ("    Tick "+ event.getTick() + ": " + 
						ArrayUtils.byteArrayToString(event.getMessage().getMessage(), event.getMessage().getLength()));
			}
		}
	}
}
