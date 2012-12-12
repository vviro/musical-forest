package de.lmu.dbs.musicalforest.midi;

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
	public static final byte[] MESSAGE_BANK_SELECT_MSB = {(byte)0xB0, (byte)0x00};
	public static final byte[] MESSAGE_BANK_SELECT_LSB = {(byte)0xB0, (byte)0x20};
	public static final byte[] MESSAGE_PROGRAM_CHANGE = {(byte)0xC0};
	public static final byte[] MESSAGE_CONTROLLER = {(byte)0xB0};
}
