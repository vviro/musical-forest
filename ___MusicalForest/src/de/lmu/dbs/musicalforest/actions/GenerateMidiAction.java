package de.lmu.dbs.musicalforest.actions;

import java.io.File;

import de.lmu.dbs.musicalforest.midi.MIDIRandomGenerator;
import de.lmu.dbs.jspectrum.util.RuntimeMeasure;
import de.lmu.dbs.musicalforest.Action;

/**
 * Random Generator for generation of MIDI files with random note distributions.
 * 
 * @author Thomas Weber
 *
 */
public class GenerateMidiAction extends Action {

	/**
	 * Number of files to create
	 */
	private int numOfFiles;
	
	/**
	 * Use grouped mode (generate random chords)
	 */
	private boolean sequential;

	/**
	 * Length of one file
	 */
	private int lengthMinutes;
	
	/**
	 * MIDI tempo
	 */
	private int bpm;
	
	/**
	 * Max MIDI note
	 */
	private int maxNote;
	
	/**
	 * Min MIDI note
	 */
	private int minNote;
	
	/**
	 * Max note duration
	 */
	long maxDuration = 500;
	
	/**
	 * Min note duration
	 */
	long minDuration = 130;
	
	/**
	 * Only for random polyphonic mode: Notes per second to be generated
	 */
	double notesPerSecond = 4;

	/**
	 * Only for grouped mode: Max voices per generated random chord.
	 * If gt 0, this indicates the maximum timbrality per frame. Groups of notes will be created. Set to 1 to produce monophonic files.
	 */
	int harmonicComplexity = 3;
	
	/**
	 * Only for grouped mode: Max pause between notes
	 */
	long maxPauseBetween = 0;

	/**
	 * Only for grouped mode: Min pause between notes
	 */
	long minPauseBetween = 0;
	
	/**
	 * When creating groups, follow the distributions found out by Youngblood (1958) and Hutchinson (1983), taken from
	 * Carol Krumhansl (Cognitive Foundations of Musical Pitch, Oxford Psychology Series, 2001)
	 */
	public boolean musical = false;
	
	/**
	 * File name prefix to use
	 */
	public String filePrefix = null;
	
	/**
	 * 
	 * @param workingFolder
	 * @param numOfFiles
	 * @param grouped
	 * @param lengthMinutes
	 * @param bpm
	 * @param maxNote
	 * @param minNote
	 * @param notesPerSecond
	 * @param harmonicComplexity
	 * @param maxPauseBetween
	 * @param maxDuration
	 * @param minDuration
	 */
	public GenerateMidiAction(String workingFolder, String filePrefix, int numOfFiles, boolean sequential, boolean musical, int lengthMinutes, int bpm, int maxNote, int minNote, double notesPerSecond, int harmonicComplexity, long minPauseBetween, long maxPauseBetween, long maxDuration, long minDuration) {
		this.workingFolder = workingFolder;
		this.filePrefix = filePrefix;
		
		this.numOfFiles = numOfFiles;
		this.sequential = sequential;
		this.musical = musical;
		
		this.lengthMinutes = lengthMinutes;
		this.bpm = bpm;
		this.maxNote = maxNote;
		this.minNote = minNote;
		this.maxDuration = maxDuration;
		this.minDuration = minDuration;
		
		this.notesPerSecond = notesPerSecond;
		
		this.harmonicComplexity = harmonicComplexity;
		this.minPauseBetween = minPauseBetween;
		this.maxPauseBetween = maxPauseBetween;
	}
	
	/**
	 * 
	 */
	@Override
	public void process(RuntimeMeasure m) throws Exception {
		File wf = new File(workingFolder);
		if (!wf.exists()) {
			wf.mkdirs();
			m.measure("Created target folder: " + workingFolder, true);
		}
		
		long lengthMillis = 1000 * 60 * lengthMinutes; // file length in ms
		int noteCount = (int)(((double)lengthMillis / 1000) * notesPerSecond); 
		
		MIDIRandomGenerator g = new MIDIRandomGenerator(lengthMillis, bpm, harmonicComplexity, musical);
		g.setRanges(minNote, maxNote, minDuration, maxDuration);
		
		long[] notes;
		if (this.sequential) {
			notes = g.processSequential(numOfFiles, workingFolder + File.separator + filePrefix, ".mid", minPauseBetween, maxPauseBetween);
		} else {
			notes = g.processNonSequential(numOfFiles, workingFolder + File.separator + filePrefix, ".mid", noteCount);
		}
		m.measure("Finished creating " + numOfFiles + " random MIDI files");

		// Distribution stats
		System.out.println("Note distribution:");
		for(int i=0; i<g.intervalStats.length; i++) {
			System.out.println(i + ": " + g.intervalStats[i]);
		}
		
		// Count notes
		long all = 0;
		for (int i=0; i<notes.length; i++) {
			//System.out.println("   Note " + (MIDI.MIN_NOTE_NUMBER+i) + ": " + notes[i]);
			all += notes[i];
		}
		m.measure("Note count overall: " + all, true);	
	}
}
