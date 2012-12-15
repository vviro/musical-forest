package de.lmu.dbs.musicalforest.actions;

import java.io.File;

import de.lmu.dbs.musicalforest.midi.MIDI;
import de.lmu.dbs.musicalforest.midi.MIDIRandomGenerator;
import de.lmu.dbs.jspectrum.util.RuntimeMeasure;
import de.lmu.dbs.musicalforest.Action;

public class MidiAction extends Action {

	public static final String RANDOM_MIDI_FILENAME_PREFIX = "generated";
	
	private int numOfFiles;
	private boolean groups;
	
	private int lengthMinutes;
	private int bpm;
	private int maxNote;
	private int minNote;
	long maxDuration = 500;
	long minDuration = 130;
	
	// only for random polyphonic mode
	double notesPerSecond = 4;
	
	// only for grouped mode
	int harmonicComplexity = 3; // if gt 0, this indicates the maximum timbrality per frame. Groups of notes will be created. Set to 1 to produce monophonic files.
	long maxPauseBetween = 350;

	public MidiAction(String workingFolder, int numOfFiles, boolean grouped, int lengthMinutes, int bpm, int maxNote, int minNote, double notesPerSecond, int harmonicComplexity, long maxPauseBetween, long maxDuration, long minDuration) {
		this.workingFolder = workingFolder;
		
		this.numOfFiles = numOfFiles;
		this.groups = grouped;
		
		this.lengthMinutes = lengthMinutes;
		this.bpm = bpm;
		this.maxNote = maxNote;
		this.minNote = minNote;
		this.maxDuration = maxDuration;
		this.minDuration = minDuration;
		
		this.notesPerSecond = notesPerSecond;
		
		this.harmonicComplexity = harmonicComplexity;
		this.maxPauseBetween = maxPauseBetween;
	}
	
	@Override
	public void process(RuntimeMeasure m) throws Exception {
		MIDIRandomGenerator g;

		long lengthMillis = 1000 * 60 * lengthMinutes; // file length in ms
		

		if (groups) {
			g = new MIDIRandomGenerator(lengthMillis, bpm, harmonicComplexity, maxPauseBetween);
		} else {
			int numOfNotes = (int)(((double)lengthMillis / 1000) * notesPerSecond); 
			g = new MIDIRandomGenerator(lengthMillis, bpm, numOfNotes);
		}		
		g.maxNote = maxNote;
		g.minNote = minNote;
		g.maxDuration = maxDuration;
		g.minDuration = minDuration;
		
		long[] notes = g.process(numOfFiles, workingFolder + File.separator + RANDOM_MIDI_FILENAME_PREFIX + "_" + (groups ? "grouped" : "poly"), ".mid");
		m.measure("Finished creating " + numOfFiles + " random MIDI files");
		
		System.out.println("Note distribution:");
		long all = 0;
		for (int i=0; i<notes.length; i++) {
			System.out.println("   Note " + (MIDI.MIN_NOTE_NUMBER+i) + ": " + notes[i]);
			all += notes[i];
		}
		m.measure("Note count overall: " + all, true);	
	}
}
