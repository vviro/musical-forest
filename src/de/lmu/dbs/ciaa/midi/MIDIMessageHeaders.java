package de.lmu.dbs.ciaa.midi;

/**
 * This class contains MIDI message headers in byte[] format.
 * 
 * @author Thomas Weber
 *
 */
public class MIDIMessageHeaders {
	
	public static final byte[] MESSAGE_TEMPOCHANGE = {(byte)0xff, (byte)0x51, (byte)0x03};
	public static final byte[] MESSAGE_NOTEON = {(byte)0x90};
	public static final byte[] MESSAGE_NOTEOFF = {(byte)0x80};
	
}
