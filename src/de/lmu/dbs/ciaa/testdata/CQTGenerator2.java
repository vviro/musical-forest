package de.lmu.dbs.ciaa.testdata;

import java.awt.Color;
import java.io.File;

import de.lmu.dbs.ciaa.midi.MIDIAdapter;
import de.lmu.dbs.ciaa.util.*;

/**
 * Just generates PNGs if forgotten while executing CQTGenerator
 * 
 * @author Thomas Weber
 *
 */
public class CQTGenerator2 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		boolean savePng = true;
		String folder = "testdata2/";
		File midiFolder = new File(folder); // + "mono/midi/");
		File cqtFolder = new File(folder); // + "mono/cqt/");
		File pngFolder = new File(folder); // + "mono/png/");
		String cqtPostfix = ".cqt";
		String pngPostfix = ".png";
		String midiPostfix = ".mid";
		//String peakPostfix = ".peak";
		String freqFileName = folder + "frequencies";
		double sampleRate = 44100;
		int step = 256; // Samples per frame

		try {
			// Create profiling tool etc.
			RuntimeMeasure m = new RuntimeMeasure(System.out);
			FileIO<byte[][]> io = new FileIO<byte[][]>();
			FileIO<double[]> dio = new FileIO<double[]>();

			File[] srcFiles = midiFolder.listFiles();
			for(int i=0; i<srcFiles.length; i++) {
				File midifile = srcFiles[i];
				if (!midifile.isFile()) continue;
				if (!midifile.getName().endsWith(midiPostfix)) {
					continue;
				}
				File cqtfile = new File(cqtFolder.getAbsolutePath() + File.separator + midifile.getName().replace(midiPostfix, cqtPostfix));
				File pngfile = new File(pngFolder.getAbsolutePath() + File.separator + midifile.getName().replace(midiPostfix, pngPostfix));
				//File peakfile = new File(cqtFolder.getAbsolutePath() + File.separator + midifile.getName().replace(midiPostfix, peakPostfix));
				out("#### Processing file " + i + " ####");

				// Load cqt data
				byte[][] data = io.load(cqtfile.getAbsolutePath());
				//byte[][] dataPeak = io.load(peakfile.getAbsolutePath());
				// Load freq file
				double[] freqs = dio.load(freqFileName);
				m.measure("Loaded spectral data");
				
				// Load MIDI file
				MIDIAdapter midi = new MIDIAdapter(midifile);
				long duration = (long)((double)((data.length+1)*step*1000.0)/sampleRate); // Audio length in milliseconds
				byte[][] midiData = midi.toDataArray(data.length, duration, freqs);
				//ArrayUtils.blur(midiData, 0);
				m.measure("Finished loading and extracting MIDI from " + midifile);

				// Save PNG image of the results
				if (savePng) {
					ArrayToImage img = new ArrayToImage(data.length, data[0].length, 1);
					out("--> Max Data:  " + img.add(data, Color.WHITE, null));
					//out("--> Max Peak:  " + img.add(dataPeak, Color.GREEN, null, 0));
					out("--> Max MIDI:  " + img.add(midiData, Color.RED, null, 0));
					img.save(pngfile);
					m.measure("Saved image to " + pngfile.getName());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
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
