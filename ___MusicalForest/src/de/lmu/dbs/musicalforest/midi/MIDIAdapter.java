package de.lmu.dbs.musicalforest.midi;

import gnu.trove.list.TDoubleList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
	public MIDIAdapter(final double bpm) throws Exception {
		super(bpm);
	}
	
	/**
	 * Creates an empty MIDI adapter.
	 * 
	 * @param bpm
	 * @param tpq
	 * @throws Exception
	 */
	public MIDIAdapter(final double bpm, final int tpq) throws Exception {
		super(bpm, tpq);
	}

	/**
	 * Strips all Bank select and Program change events from all tracks.
	 * 
	 * @param i
	 * @throws Exception 
	 */
	public void stripProgramChanges() throws Exception {
		stripMessages(MIDIMessageHeaders.MESSAGE_BANK_SELECT_MSB, true);
		stripMessages(MIDIMessageHeaders.MESSAGE_BANK_SELECT_LSB, true);
		stripMessages(MIDIMessageHeaders.MESSAGE_PROGRAM_CHANGE, true);
	}

	/**
	 * Strips all messages containing the given message header. Use constants
	 * in MIDIMessageHeaders to fill this. If multiChannel is true, the
	 * second half of the first byte will be ignored, which is the midi
	 * channel number in multichannel messages, i.e. note on/off.
	 * 
	 * @param header
	 * @param multiChannel
	 */
	public void stripMessages(byte[] header, boolean multiChannel) {
		for (int i=0; i<tracks.length; i++) {
			Track track = tracks[i];
			List<MidiEvent> remove = new ArrayList<MidiEvent>();
			// Collect list of events that match the header
			for (int j=0; j<track.size(); j++) {
				MidiEvent event = track.get(j);
				byte[] b = event.getMessage().getMessage();
				if (checkMessageType(b, header, multiChannel)) {
					remove.add(event);
				}
			}
			// Remove these events from the track
			for (int k=0; k<remove.size(); k++) {
				MidiEvent event = remove.get(k);
				track.remove(event);
			}
		}
	}	
	
	/**
	 * Sets all velocities to 127
	 * @throws Exception 
	 * 
	 */
	public void maximizeVelocities(int newVelocity) throws Exception {
		for (int i=0; i<tracks.length; i++) {
			Track track = tracks[i];
			for (int j=0; j<track.size(); j++) {
				MidiEvent event = track.get(j);
				byte[] b = event.getMessage().getMessage();
				if (event.getMessage() instanceof ShortMessage) {
					if (checkMessageType(b, MIDIMessageHeaders.MESSAGE_NOTEON, true)) {
						ShortMessage sm = (ShortMessage)event.getMessage();
						int status = 0x90 | sm.getChannel();
						sm.setMessage(status, sm.getData1(), newVelocity);
					}
				}
			}
		}
	}

	/**
	 * Strips all notes out of the given note number range.
	 * 
	 * @param min
	 * @param max
	 */
	public void limitBandwidth(int min, int max) {
		for (int i=0; i<tracks.length; i++) {
			Track track = tracks[i];
			List<MidiEvent> remove = new ArrayList<MidiEvent>();
			// Collect list of events to remove
			for (int j=0; j<track.size(); j++) {
				MidiEvent event = track.get(j);
				if (event.getMessage() instanceof ShortMessage) {
					ShortMessage sm = (ShortMessage)event.getMessage();
					if (sm.getData1() < 44 || sm.getData1() > max) {
						remove.add(event);
					}
				}
			}
			// Remove these events from the track
			for (int k=0; k<remove.size(); k++) {
				MidiEvent event = remove.get(k);
				track.remove(event);
				//System.out.println("Stripped note out of bounds: " + ArrayUtils.byteArrayToString(event.getMessage().getMessage(), event.getMessage().getLength()));
			}
		}
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
	 * @param duration audio length in milliseconds. See calculateDuration()
	 * @param freqs array holding the bin frequencies of the spectrum to match
	 * @return
	 * @throws Exception 
	 */
	public byte[][] toDataArray(final int frames, int offset, final long duration, final double[] freqs) throws Exception {
		return toDataArray(frames, offset, duration, freqs, false);
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
	 * @param duration audio length in milliseconds. See calculateDuration()
	 * @param freqs array holding the bin frequencies of the spectrum to match
	 * @return
	 * @throws Exception 
	 */
	public byte[][] toDataArray(final int frames, final int offset, final long duration, final double[] freqs, boolean ignoreErrors) throws Exception {
		byte[][] ret = new byte[frames+offset][freqs.length];
		double frameDuration = (double)duration/frames; // millis per frame
		double timePerQuarter = (double)tempoChanges.get(0).getMicrosPerQuarter()/1000.0;
		int offsetFrames = (int)((double)offset/frameDuration);
		
		for (int i=0; i<tracks.length; i++) {
			Track track = tracks[i];
			for (int j=0; j<track.size(); j++) {
				// Search for note on events
				MidiEvent event = track.get(j);
				if (event.getMessage() instanceof ShortMessage) {
					ShortMessage sm = (ShortMessage)event.getMessage();
					if (checkMessageType(sm.getMessage(), MIDIMessageHeaders.MESSAGE_NOTEON, true) && sm.getData2() != 0) {
						int note = sm.getData1();
						// Found note on: search for corresponding note off
						long offTick = -1;
						for (int jj=j; jj<track.size(); jj++) {
							MidiEvent event2 = track.get(jj);
							if (event2.getMessage() instanceof ShortMessage) {
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
						setNote(freqs, ret, note, event.getTick(), offTick, frameDuration, timePerQuarter, offsetFrames, ignoreErrors);
					}
				}
			}
		}
		return ret;
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
	 * @param duration audio length in milliseconds. See calculateDuration()
	 * @param freqs array holding the bin frequencies of the spectrum to match
	 * @return
	 * @throws Exception 
	 *
	public byte[][] toDataArray2(final int frames, final long duration, final double[] freqs, boolean ignoreErrors) throws Exception {
		byte[][] ret = new byte[frames][freqs.length];
		double frameDuration = (double)duration/frames; // millis per frame
		double timePerQuarter = (double)tempoChanges.get(0).getMicrosPerQuarter()/1000.0;
		
		for (int i=0; i<tracks.length; i++) {
			Track track = tracks[i];
			for (int j=0; j<track.size(); j++) {
				// Search for note on events
				MidiEvent event = track.get(j);
				if (event.getMessage() instanceof ShortMessage) {
					ShortMessage sm = (ShortMessage)event.getMessage();
					if (checkMessageType(sm.getMessage(), MIDIMessageHeaders.MESSAGE_NOTEON, true) && sm.getData2() != 0) {
						int note = sm.getData1();
						// Found note on: search for corresponding note off
						long offTick = -1;
						for (int jj=j; jj<track.size(); jj++) {
							MidiEvent event2 = track.get(jj);
							if (event2.getMessage() instanceof ShortMessage) {
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
						setNote(freqs, ret, note, event.getTick(), offTick, frameDuration, timePerQuarter, ignoreErrors);
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
	private void setNote(final double[] freqs, final byte[][] data, final int note, final long tick, final long offTick, final double frameDuration, final double timePerQuarter, int offsetFrames, boolean ignoreErrors) throws Exception {
		int startFrame = getFrame(tick, frameDuration, timePerQuarter) + offsetFrames;
		int endFrame = (offTick >= 0) ? getFrame(offTick, frameDuration, timePerQuarter) : data.length-1;
		endFrame+= offsetFrames;
		if (startFrame >= data.length) {
			if (ignoreErrors) {
				//System.err.println("Warning: MIDI data is too long");
				return;
			}
			throw new Exception("Note could not be mapped, start frame " + startFrame + " out of bounds of data array (length: " + data.length + " frames)");
		}
		if (endFrame >= data.length) {
			if (ignoreErrors) {
				//System.err.println("Warning: MIDI data is too long");
				return;
			}
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
	 * Returns the frequency bin in the data array corresponding to a midi notes frequency,
	 * or -1 if the frequency is out of range.
	 * <br><br>
	 * <b>NOTE:</b> The highest bin is just ignored and frequencies of the bin are "out of range".
	 *              This should not be a problem in practice because f0 is mostly lower than the highest spectrum frequency.
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
	 * 
	 * @param tick midi tick of the note
	 * @param frameDuration audio duration of one frame in milliseconds
	 * @param timePerQuarter milliseconds per quarter note (current tempo)
	 * @return
	 */
	private int getFrame(final long tick, final double frameDuration, final double timePerQuarter) {
		double timePos = (double)(timePerQuarter * tick) / ticksPerQuarter; 
		return (int)(timePos / frameDuration); 
	}

	/**
	 * Claculates the length for rendered MIDI byte array creation. Use this to determine the correct
	 * parameter for toByteArray().
	 * 
	 * @param length
	 * @param step
	 * @param sampleRate
	 * @return
	 */
	public static long calculateDuration(int length, int step, double sampleRate) {
		return (long)((double)((length+1)*step*1000.0)/sampleRate);
	}

	/**
	 * Creates MIDI notes from note-on and note-off arrays. Every value
	 * in these arrays which is greater than zero will be interpreted as note.
	 * Velocity is always set to 127, the extraction of velocities
	 * is not implemented yet.
	 * 
	 * @param ons array containing non-zero values at each note on event
	 * @param offs array containing non-zero values at each note off event
	 * @param millisPerFrame milliseconds per array x step
	 * @param frequencies frequencies array holding the frequency of each y coordinate in the arrays
	 * @throws Exception
	 */
	public long renderFromArrays(float[][] ons, float[][] offs, double millisPerFrame, double[] frequencies, int frequencyWindow, TDoubleList noteLengthDistribution, int avgLength) throws Exception {
		if (ons.length != offs.length) throw new Exception("Note on and off arrays have to have the same size");
		if (ons[0].length != offs[0].length) throw new Exception("Note on and off arrays have to have the same size");
		double timePerQuarter = (double)tempoChanges.get(0).getMicrosPerQuarter()/1000.0;
		double timePerTick = timePerQuarter / ticksPerQuarter;
		int offWindow = frequencyWindow/2;
		long count = 0;
		for(int x=0; x<ons.length; x++) {
			for(int y=0; y<ons[0].length; y++) {
				if (ons[x][y] > 0) {
					// Found note
					double freq = (float)frequencies[y];
					int note = midiRef.getNoteFromFrequency((float)freq);
					int durationTicks = (int)((millisPerFrame * searchOffset(ons, offs, x, y, offWindow, noteLengthDistribution, avgLength)) / timePerTick);
					if (durationTicks > 0) {
						int tick = (int)((millisPerFrame * (x+1)) / timePerTick); 
						int velocity = 127;
						setNote(tick, durationTicks, note, velocity);
						count++;
						//System.out.println("Created MIDI note " + note + ": tick " + tick + ", durationTicks: " + durationTicks);
					}
				}
			}
		}
		return count;
	}

	/**
	 * Search offset for a note onset.
	 * 
	 * @param offs
	 * @param x onset x
	 * @param y onset y
	 * @param offWindow window up and down the y axis to look for note offs
	 * @param millisPerFrame
	 * @param timePerTick
	 * @return
	 */
	private int searchOffset(float[][] ons, float[][] offs, int x, int y, int offWindow, TDoubleList noteLengthDistribution, int avgLength) {
		double[] ratings = new double[noteLengthDistribution.size()];
		int onIndex = -1;
		for(int xo=1; x+xo < offs.length && xo < ratings.length; xo++) {
			if (onIndex > 0) break;
			for(int yo = y-offWindow; yo <= y+offWindow && y+yo < offs[0].length; yo++) {
				if (yo < 0)  yo = 0;
				if (offs[xo+x][yo] > 0) {
					// Found note off: rate it
					ratings[xo] = noteLengthDistribution.get(xo);
				}
				if (ons[xo+x][yo] > 0) {
					// Next onset -> leave
					onIndex = xo - 1;
					break;
				}
			}
		}
		double max = -Double.MAX_VALUE;
		int index = -1;
		for(int i=0; i<ratings.length; i++) {
			if (onIndex >= 0 && i >= onIndex) {
				break;
			}
			if (ratings[i] > 0) {
				if (ratings[i] > max) {
					max = ratings[i];
					index = i;
				}
			}
		}
		if (index < 0) {
			if (onIndex >= 0 && avgLength >= onIndex) {
				return onIndex;
			}
			return avgLength;
		}
		return index;
	}

	/*
	private int searchOffset(float[][] ons, float[][] offs, int x, int y, int offWindow, double millisPerFrame, double timePerTick) {
		for(int xo=1; x+xo < offs.length; xo++) {
			for(int yo = y-offWindow; yo <= y+offWindow && y+yo < offs[0].length; yo++) {
				if (yo < 0)  yo = 0;
				if (offs[xo+x][yo] > 0) {
					// Found note off
					return (int)((xo * millisPerFrame) / timePerTick);
				}
				if (ons[xo+x][yo] > 0) {
					// Next onset -> leave
					return (int)(((xo-1) * millisPerFrame) / timePerTick);
				}
			}
		}
		return -1;
	}
	*/
}
