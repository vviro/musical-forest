package de.lmu.dbs.ciaa.spectrum;

import java.awt.Color;
import java.io.File;

import de.lmu.dbs.ciaa.midi.MIDIAdapter;
import de.lmu.dbs.ciaa.util.*;

/**
 * Example that compares a spectrum of an audio file to its original MIDI representation.
 * 
 * @author Thomas Weber
 *
 */
public class TransformAndMIDIExample {

	/**
	 * Transformation modes
	 * 
	 * @author Thomas Weber
	 *
	 */
	public static enum Transformations {FFT, CQT};
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// MIDI parameters
		String midiFile = "MIDIFiles/Test8melody.mid"; // MIDI file to overlay
		
		// PNG parameters
		int fspread = 1; // Scale factor for freq scale
		String imgFile = "testimages/ciaa2.png"; // PNG file
		
		// Transformation general parameters 
		String wavFile = "WaveFiles/Test8_Mix.wav"; //args[1]; // WAV file
		Transformations mode = Transformations.CQT; // Transformation to use (FFT or CQT)
		int step = 256; // Samples per frame

		// CQT parameters (see ConstantQTransform javadoc for details...)
		double fmin = 80.0;
		double fmax = 10000.0;
		double binsPerOctave = 48.0;
		double threshold = 0.05;
		double spread = 1.0;
		double divideFFT = 4.0;
		String cqtBufferLocation = "cqtbuff/";

		// FFT parameters (see FFTransform javadoc for details...)
		int fftlen = 16384;
		double zoomOutput = 0.05;
		
		try {
			// Create profiling tool
			RuntimeMeasure m = new RuntimeMeasure(System.out);

			// Load sample
			Sample src = new WaveSample(new File(wavFile));
			m.measure("Loaded sample");

			// Init transformation
			Transform transformation = null;
			if (mode == Transformations.FFT) {
				transformation = new FFTransform((double)src.getSampleRate(), fftlen, zoomOutput);	
			} else {
				transformation = new ConstantQTransform((double)src.getSampleRate(), fmin, fmax, binsPerOctave, threshold, spread, divideFFT, cqtBufferLocation);
			}
			m.measure("Initialized transformation");
			
			// Make mono
			int[] mono = src.getLeftBuffer();
			m.measure("Extracted left channel, total samples: " + mono.length);

			// Calculate transformation
			double[][] data = transformation.calculate(mono, step, new HammingWindow(transformation.getWindowSize()));
			m.measure("Finished transformation. Number of output frames: " + data.length);
			
			// Normalize data
			ArrayUtils.normalize(data);
			m.measure("Finished normalizing transformed data");

			// Load MIDI file
			MIDIAdapter midi = new MIDIAdapter(new File(midiFile));
			long duration = (long)(mono.length*1000.0/src.getSampleRate()); // Audio length in milliseconds
			byte[][] midiData = midi.toDataArray(data.length, duration, transformation.getFrequencies());
			ArrayUtils.blur(midiData, 0);
			m.measure("Finished loading and extracting MIDI from " + midiFile);
			
			// Save PNG image of the results
			SpectrumToImage img = new SpectrumToImage(data.length, data[0].length, fspread);
			Scale scale = new LogScale(10);
			img.add(data, new Color(255,150,0), scale); // plain data, scaled with Log10
			img.add(midiData, new Color(0,255,0), null, 0.5); // midi
			img.save(new File(imgFile));
			m.measure("Saved image to " + imgFile);
	        
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Outputs the message to the console 
	 * 
	 * @param message the message
	 */
	@SuppressWarnings("unused")
	private static void out(String message) {
		System.out.println(message);
	}
	
}
