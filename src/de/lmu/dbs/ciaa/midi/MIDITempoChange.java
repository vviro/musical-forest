package de.lmu.dbs.ciaa.midi;

/**
 * Represents a MIDI tempo change.
 * 
 * @author Thomas Weber
 *
 */
public class MIDITempoChange {

	/**
	 * Microseconds per quarter note
	 */
	private long microsPerQuarter;
	
	/**
	 * MIDI tick timestamp
	 */
	private long tick;
	
	/**
	 * 
	 * @param tick
	 * @param microsPerQuarter
	 */
	public MIDITempoChange(long tick, long microsPerQuarter) {
		this.tick = tick;
		this.microsPerQuarter = microsPerQuarter;
	}
	
	/**
	 * Get Microseconds per quarter note
	 * 
	 * @return
	 */
	public long getMicrosPerQuarter() {
		return microsPerQuarter;
	}

	/**
	 * Set Microseconds per quarter note
	 * 
	 * @param microsPerQuarter
	 */
	public void setMicrosPerQuarter(long microsPerQuarter) {
		this.microsPerQuarter = microsPerQuarter;
	}

	/**
	 * Get MIDI tick timestamp
	 * 
	 * @return
	 */
	public long getTick() {
		return tick;
	}

	/**
	 * Set MIDI tick timestamp
	 * 
	 * @param tick
	 */
	public void setTick(long tick) {
		this.tick = tick;
	}
}
