package de.lmu.dbs.ciaa.midi;

import java.io.File;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * Class that adapts MIDI to the machine learning algorithms data format.
 * 
 * @author Thomas Weber
 *
 */
public class MIDIAdapter extends MIDI {

	/**
	 * Initializes a midi file.
	 * 
	 * @param file
	 * @throws Exception
	 */
	public MIDIAdapter(final File file) throws Exception {
		super(file);
	}
	
	/**
	 * Creates an empty MIDI adapter.
	 * 
	 * @param bpm beats per minute
	 * @throws Exception
	 */
	public MIDIAdapter(final int bpm) throws Exception {
		super(bpm);
	}

	/**
	 * Converts the midi data to an array that can be used in direct  
	 * comparison with spectral data.<br>
	 * <br>
	 * Returns an int[frames][freqs.length], where events are encoded:<br>
	 * 		0: Nothing<br>
	 * 		1: A note is ringing<br>
	 * 
	 * @param frames number of time frames used in spectrum to match
	 * @param duration audio length in milliseconds
	 * @param freqs array holding the bin frequencies of the spectrum to match
	 * @return
	 * @throws Exception 
	 */
	public byte[][] toDataArray(final int frames, final long duration, final double[] freqs) throws Exception {
		byte[][] ret = new byte[frames][freqs.length];
		double frameDuration = (double)duration/frames; // millis per frame
		double timePerQuarter = (double)tempoChanges.get(0).getMicrosPerQuarter()/1000;
		
		for (int i=0; i<tracks.length; i++) {
			Track track = tracks[i];
			for (int j=0; j<track.size(); j++) {
				// Search for note on events
				MidiEvent event = track.get(j);
				if (event.getMessage() instanceof ShortMessage) {//.getClass().toString().contains("ShortMessage")) { // Ignore all irrelevant messages // TODO memory hotspot (toString)
					ShortMessage sm = (ShortMessage)event.getMessage();
					if (checkMessageType(sm.getMessage(), MIDIMessageHeaders.MESSAGE_NOTEON, true) && sm.getData2() != 0) {
						int note = sm.getData1(); //getNoteFromMessage(message);
						// Found note on: search for corresponding note off
						long offTick = -1;
						for (int jj=j; jj<track.size(); jj++) {
							MidiEvent event2 = track.get(jj);
							if (event2.getMessage() instanceof ShortMessage) { //.getClass().toString().contains("ShortMessage")) { // Ignore all irrelevant messages
								ShortMessage sm2 = (ShortMessage)event2.getMessage();
								if (sm2.getData1() == note 
									&& (checkMessageType(sm2.getMessage(), MIDIMessageHeaders.MESSAGE_NOTEOFF, true) 
										|| (checkMessageType(sm2.getMessage(), MIDIMessageHeaders.MESSAGE_NOTEON, true) 
											&& sm2.getData2() == 0
											)
										)
									) {
									// Found note off
									offTick = event2.getTick();
									break;
								}
							}
						}
						// Write data
						setNote(freqs, ret, note, event.getTick(), offTick, frameDuration, timePerQuarter);
					}
				}
			}
		}
		return ret;
	}
	
	/**
	 * Converts the midi data to a sparse array that can be used in direct  
	 * comparison with spectral data.<br>
	 * <br>
	 * Returns an int[frames][freqs.length], where events are encoded:<br>
	 * 		0: Nothing<br>
	 * 		1: A note is ringing<br>
	 * 
	 * @param frames number of time frames used in spectrum to match
	 * @param duration audio length in milliseconds
	 * @param freqs array holding the bin frequencies of the spectrum to match
	 * @return
	 * @throws Exception 
	 *
	public DoubleMatrix2D toSparseDataArray(int frames, long duration, double[] freqs) throws Exception {
		DoubleMatrix2D ret = new SparseDoubleMatrix2D(freqs.length, frames);
		double frameDuration = (double)duration/frames; // millis per frame
		double timePerQuarter = (double)tempoChanges.get(0).getMicrosPerQuarter()/1000;
		
		for (int i=0; i<tracks.length; i++) {
			Track track = tracks[i];
			for (int j=0; j<track.size(); j++) {
				// Search for note on events
				MidiEvent event = track.get(j);
				if (event.getMessage() instanceof ShortMessage) {//if (event.getMessage().getClass().toString().contains("ShortMessage")) { // Ignore all irrelevant messages TODO this is a memory hot spot (Class.toString)
					ShortMessage sm = (ShortMessage)event.getMessage();
					if (checkMessageType(sm.getMessage(), MIDIMessageHeaders.MESSAGE_NOTEON, true) && sm.getData2() != 0) {
						int note = sm.getData1(); //getNoteFromMessage(message);
						// Found note on: search for corresponding note off
						long offTick = -1;
						for (int jj=j; jj<track.size(); jj++) {
							MidiEvent event2 = track.get(jj);
							if (event2.getMessage() instanceof ShortMessage) {//if (event2.getMessage().getClass().toString().contains("ShortMessage")) { // Ignore all irrelevant messages
								ShortMessage sm2 = (ShortMessage)event2.getMessage();
								if (sm2.getData1() == note 
									&& (checkMessageType(sm2.getMessage(), MIDIMessageHeaders.MESSAGE_NOTEOFF, true) 
										|| (checkMessageType(sm2.getMessage(), MIDIMessageHeaders.MESSAGE_NOTEON, true) 
											&& sm2.getData2() == 0
											)
										)
									) {
									// Found note off
									offTick = event2.getTick();
									break;
								}
							}
						}
						// Write data
						setNote(freqs, ret, note, event.getTick(), offTick, frameDuration, timePerQuarter);
					}
				}
			}
		}
		return ret;
	}

	/**
	 * Helper method for toDataArray(). Transfers one note to the data array.
	 * 
	 * @param freqs frequencies of bins
	 * @param data output array
	 * @param note the note to paint
	 * @param tick the note on midi tick
	 * @param offTick the note off midi tick (if < 0: endless note)
	 * @param frameDuration duration of one data frame in milliseconds
	 * @param timePerQuarter milliseconds per quarter note (current tempo)
	 * @throws Exception 
	 */
	private void setNote(final double[] freqs, final byte[][] data, final int note, final long tick, final long offTick, final double frameDuration, final double timePerQuarter) throws Exception {
		int startFrame = getFrame(tick, frameDuration, timePerQuarter);
		int endFrame = (offTick >= 0) ? getFrame(offTick, frameDuration, timePerQuarter) : data.length-1;
		if (startFrame >= data.length) {
			throw new Exception("Note could not be mapped, start frame " + startFrame + " out of bounds of data array (length: " + data.length + " frames)");
		}
		if (endFrame >= data.length) {
			throw new Exception("Note could not be mapped, end frame " + endFrame + " out of bounds of data array (length: " + data.length + " frames)");
		}
		int bin = getNoteBin(freqs, note);
		if (bin < 0) return;
		// set data
		for(int i=startFrame; i<endFrame; i++) {
			data[i][bin] = 1;
		}
	}
	
	/**
	 * Helper method for toDataArray(). Transfers one note to the sparse data array.
	 * 
	 * @param freqs frequencies of bins
	 * @param data output array
	 * @param note the note to paint
	 * @param tick the note on midi tick
	 * @param offTick the note off midi tick (if < 0: endless note)
	 * @param frameDuration duration of one data frame in milliseconds
	 * @param timePerQuarter milliseconds per quarter note (current tempo)
	 * @throws Exception 
	 *
	private void setNote(double[] freqs, DoubleMatrix2D data, int note, long tick, long offTick, double frameDuration, double timePerQuarter) throws Exception {
		int startFrame = getFrame(tick, frameDuration, timePerQuarter);
		int endFrame = (offTick >= 0) ? getFrame(offTick, frameDuration, timePerQuarter) : data.columns()-1;
		if (startFrame >= data.columns()) {
			throw new Exception("Note could not be mapped, start frame " + startFrame + " out of bounds of data array (length: " + data.columns() + " frames)");
		}
		if (endFrame >= data.columns()) {
			throw new Exception("Note could not be mapped, end frame " + endFrame + " out of bounds of data array (length: " + data.columns() + " frames)");
		}
		int bin = getNoteBin(freqs, note);
		if (bin < 0) return;
		// set data
		for(int i=startFrame; i<endFrame; i++) {
			data.set(bin, i, 1);
		}
	}

	/**
	 * Returns the frequency bin in the data array corresponding to a midi notes frequency,
	 * or -1 if the frequency is out of range.
	 * <br><br>
	 * TODO: At the moment, the highest bin is just ignored and frequencies of the bin are "out of range".
	 *       This should not be a problem in practice because f0 is mostly lower than highest spectrum frequency.
	 * 
	 * @param freqs array of frequencies corresponding to the spectrum bins
	 * @param note midi note identifier
	 * @return
	 */
	public int getNoteBin(final double[] freqs, final int note) {
		double freq = (double)midiRef.getNoteFrequency(note);
		int f=0;
		while (freq > freqs[f] && f < freqs.length-1) {
			f++;
		}
		if (f == freqs.length-1) {
			// See if the frequency is out of range above the highest frequency. See javadoc.
			return -1; 
		}
		return f-1;
	}
	
	/**
	 * Returns the corresponding frame for a midi tick timestamp.
	 * <br><br>
	 * TODO: Currently this ignores all tempo change events except for the initial one
	 * 
	 * @param tick midi tick of the note
	 * @param frameDuration audio duration of one frame in milliseconds
	 * @param timePerQuarter milliseconds per quarter note (current tempo)
	 * @return
	 */
	private int getFrame(final long tick, final double frameDuration, final double timePerQuarter) {
		double timePos = (double)timePerQuarter * tick / ticksPerQuarter; 
		return (int)(timePos / frameDuration); 
	}
}
