package de.lmu.dbs.musicalforest.actions;

import java.awt.Color;
import java.io.File;

import de.lmu.dbs.jspectrum.TransformParameters;
import de.lmu.dbs.jspectrum.util.ArrayToImage;
import de.lmu.dbs.jspectrum.util.ArrayUtils;
import de.lmu.dbs.jspectrum.util.LogScale;
import de.lmu.dbs.jspectrum.util.RuntimeMeasure;
import de.lmu.dbs.jspectrum.util.Scale;
import de.lmu.dbs.musicalforest.Action;
import de.lmu.dbs.musicalforest.midi.MIDIAdapter;

/**
 * Action to save CQT spectra, optionally overlayed with MIDI
 * 
 * @author Thomas Weber
 *
 */
public class SpectrumAction extends Action {

	public String audioFile;
	
	public String midiFile;
	
	public double scaleParam;
	
	public SpectrumAction(String audioFile, String settingsFile, String midiFile, double scaleParam) {
		this.audioFile = audioFile;
		this.settingsFile = settingsFile;
		this.midiFile = midiFile;
		this.scaleParam = scaleParam;
	}
	
	@Override
	public void process(RuntimeMeasure m) throws Exception {
		
		TransformParameters tparams = new TransformParameters();
		tparams.loadParameters(settingsFile);
		m.measure("Loaded parameters from " + settingsFile);
		
		Scale scale = null;
		if (scaleParam >= 0) {
			scale = new LogScale(scaleParam); 
		}
		double[][] data = this.transformAudioFile(m, tparams, scale, audioFile);

		ArrayToImage img = new ArrayToImage(data.length, data[0].length); 
		img.add(data, Color.WHITE, null);

		if (midiFile != null) {
			MIDIAdapter ma = new MIDIAdapter(new File(midiFile));
			long duration = MIDIAdapter.calculateDuration(data.length, tparams.step, (double)this.audioSample.getSampleRate());
			byte[][] midi = ma.toDataArray(data.length, duration, tparams.frequencies, true);
			ArrayUtils.shiftRight(midi, DEFAULT_REFERENCE_SHIFT);
			img.add(midi, Color.BLUE, null, 0);
			m.measure("Loaded MIDI file: " + midiFile);
		}
		
		String imgFile = audioFile + ".png";
		img.save(new File(imgFile));
		m.measure("Saved image to " + imgFile);
	}

}
