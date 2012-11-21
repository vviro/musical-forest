package de.lmu.dbs.ciaa;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;

import de.lmu.dbs.ciaa.classifier.core.*;
import de.lmu.dbs.ciaa.classifier.core2d.*;
import de.lmu.dbs.ciaa.classifier.musicalforest.*;
import de.lmu.dbs.ciaa.midi.MIDIAdapter;
import de.lmu.dbs.ciaa.spectrum.ConstantQTransform;
import de.lmu.dbs.ciaa.spectrum.Transform;
import de.lmu.dbs.ciaa.util.*;

/**
 * 
 * @author Thomas Weber
 *
 */
public class ForestDeviationTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String testFile = "WaveFiles/Test8_Mix.wav"; // WAV file used to test the forest. Results are stored in a PNG file called <testFile>.png
		String testReferenceFile = "WaveFiles/MIDIFiles/Test8melody.mid"; // Test MIDI reference file. Has to be musically equal to testFile 
		double imgThreshold = 0.1; // Threshold to filter the normalized forest results in the PNG test output
		
		try {
			System.out.println("Java Heap size (maximum): " + ((double)Runtime.getRuntime().maxMemory() / (1024*1024)) + " MB");
			
			// Create profiling tool
			RuntimeMeasure m = new RuntimeMeasure(System.out);

			// Load sample
			Sample src = new WaveSample(new File(testFile));
			m.measure("Loaded sample");

			Transform transformation = new ConstantQTransform((double)src.getSampleRate(), 80, 8000, 48, 0.05, 1.0, 4.0, "cqtbuff/");
			
			m.measure("Initialized transformation");
			out("-> Window size: " + transformation.getWindowSize());
			
			// Make mono
			int[] mono = src.getLeftBuffer();
			m.measure("Extracted left channel, total samples: " + mono.length);

			// Calculate transformation
			double[][] dataOobDouble = transformation.calculate(mono, 256, new HammingWindow(transformation.getWindowSize()));

			Scale scale = new LogScale(10);
			ArrayUtils.normalize(dataOobDouble); // Normalize to [0,1]
			ArrayUtils.scale(dataOobDouble, scale); // Log scale
			ArrayUtils.normalize(dataOobDouble, (double)Byte.MAX_VALUE-1); // Normalize back to [0,MAX_VALUE] 
			byte[][] data = ArrayUtils.toByteArray(dataOobDouble); // To byte array to use with forest
			m.measure("Finished transformation and scaling");
			
			// Load MIDI
			MIDIAdapter ma = new MIDIAdapter(new File(testReferenceFile));
			long duration = (long)((double)((data.length+1) * 256 * 1000.0) / 44100); // TODO festwerte
			byte[][] reference = ma.toDataArray(data.length, duration, transformation.getFrequencies());
			ArrayUtils.filterFirst(reference);
			int[] noteX = new int[data.length];
			int index = 0;
			for(int x=0; x<data.length; x++) {
				for(int y=0; y<data[0].length; y++) {
					if (reference[x][y] > 0) {
						noteX[index] = x;
						index++;
						break;
					}
				}
			}
			ArrayUtils.blur(reference, 0);
			m.measure("Loaded MIDI reference file: " + testReferenceFile);
			
			// Deviation line(s)
			byte[][] lines = new byte[data.length][data[0].length];
			
			int[] xDeviation = new int[data[0].length];
			for(int y=0; y<data[0].length; y++) {
				xDeviation[y] = (int)(Math.pow(1.4, (float)(data[0].length - y) / 48.0) / 1.0) - 1;
			}
			System.out.println(data[0].length);
			//int x = 342;
			for(int i=0; i<index; i++) {
				for(int y=0; y<data[0].length; y++) {
					lines[noteX[i]][y] = 1;
				}
				for(int y=0; y<data[0].length; y++) {
					if (noteX[i]-xDeviation[y] >= 0) {
						lines[noteX[i]-xDeviation[y]][y] = 2;
					}
				}
			}
			
			// Save image
			String forestImgFile = "testdeviation.png";
			ArrayToImage img = new ArrayToImage(data.length, data[0].length);
			out("-> Max data: " +  + img.add(data, Color.WHITE, null));
			out("-> Max lines: " +  + img.add(lines, Color.RED, null, 0));
			out("-> Max MIDI: " + img.add(reference, Color.BLUE, null, 0));
			img.save(new File(forestImgFile));
			m.measure("Saved image to " + forestImgFile);
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	/**
	 * Outputs the message to the console 
	 * 
	 * @param message the message
	 */
	private static void out(String message) {
		System.out.println(message);
	}
	
}
