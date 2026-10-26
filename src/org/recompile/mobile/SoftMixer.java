/*
	This file is part of FreeJ2ME.

	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	FreeJ2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/
*/
package org.recompile.mobile;

import java.lang.reflect.Method;
import java.util.Map;

import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

/*
 * All the sound through one line: the MIDI synthesizers, the WAV and SMAF
 * clips and the MP3 streams each open an audio line of their own, which a
 * sound device that takes one stream at a time (the FunKey S, whose kernel
 * can't run ALSA's software mixing) refuses after the first. With
 * -Dfreej2me.softMixer=true they get their lines from Gervill's software mixer
 * instead (com.sun.media.sound.SoftMixingMixer, in every OpenJDK but not
 * registered as a mixer), which mixes them in Java into one line on the
 * device. Off by default: nothing changes without the property.
 *
 * -Dfreej2me.softMixer.format="22050 Hz, 16 bit, mono" sets the line's format
 * (the mixer mixes at it; default 44100 Hz, 16 bit, stereo). The JDK's
 * package isn't exported: run with --add-exports java.desktop/com.sun.media.sound=ALL-UNNAMED
 * on Java 9 and later (nothing is needed on 8).
 */
public final class SoftMixer
{
	private static final boolean enabled = Boolean.getBoolean("freej2me.softMixer");
	private static Mixer mixer;
	private static boolean failed;

	private SoftMixer() { }

	public static boolean isEnabled() { return enabled; }

	/* The mixer, open on the device, or null when it isn't wanted or couldn't open. */
	public static synchronized Mixer get()
	{
		if (!enabled || failed) { return null; }
		if (mixer != null) { return mixer; }
		try
		{
			AudioFormat format = parseFormat(System.getProperty("freej2me.softMixer.format", "44100 Hz, 16 bit, stereo"));
			SourceDataLine line = AudioSystem.getSourceDataLine(format);
			Mixer m = (Mixer) Class.forName("com.sun.media.sound.SoftMixingMixer").getConstructor().newInstance();
			// open(SourceDataLine): the mixer's format and output are the line's;
			// its open() would look for a 48 kHz line and resample everything up.
			m.getClass().getMethod("open", SourceDataLine.class).invoke(m, line);
			mixer = m;
			Mobile.log(Mobile.LOG_INFO, SoftMixer.class.getPackage().getName() + "." + SoftMixer.class.getSimpleName() + ": " + "software mixer open at " + format);
		}
		catch (Throwable t)
		{
			failed = true;
			Mobile.log(Mobile.LOG_ERROR, SoftMixer.class.getPackage().getName() + "." + SoftMixer.class.getSimpleName() + ": " + "couldn't open the software mixer, lines open on the device directly: " + t);
		}
		return mixer;
	}

	/* A clip, from the mixer or (without it) the system. */
	public static Clip getClip() throws LineUnavailableException
	{
		Mixer m = get();
		if (m == null) { return AudioSystem.getClip(); }
		return (Clip) m.getLine(new DataLine.Info(Clip.class, new AudioFormat(44100f, 16, 2, true, false)));
	}

	/* A line for streamed sound, from the mixer or (without it) the system. */
	public static SourceDataLine getSourceDataLine(AudioFormat format) throws LineUnavailableException
	{
		Mixer m = get();
		if (m == null) { return AudioSystem.getSourceDataLine(format); }
		return (SourceDataLine) m.getLine(new DataLine.Info(SourceDataLine.class, format));
	}

	/*
	 * Opens a synthesizer, on a line of the mixer when there is one (Gervill's
	 * AudioSynthesizer.open(SourceDataLine, Map), through reflection: the class
	 * isn't exported), else as usual.
	 */
	public static void openSynthesizer(Synthesizer synth) throws MidiUnavailableException
	{
		Mixer m = get();
		if (m == null) { synth.open(); return; }
		try
		{
			// The synthesizer renders at its own rate (its settings) but with the
			// mixer's channels: the mixer drops a stereo line's right channel when
			// it is mono. No jitter correction (a thread of its own): the mixer
			// paces the line.
			AudioFormat synthFormat = (AudioFormat) synth.getClass().getMethod("getFormat").invoke(synth);
			AudioFormat mixerFormat = (AudioFormat) m.getClass().getMethod("getFormat").invoke(m);
			AudioFormat format = new AudioFormat(synthFormat.getSampleRate(), 16, mixerFormat.getChannels(), true, false);
			SourceDataLine line = (SourceDataLine) m.getLine(new DataLine.Info(SourceDataLine.class, format));
			Map<String, Object> info = new java.util.HashMap<String, Object>();
			info.put("jitter correction", Boolean.FALSE);
			Method open = synth.getClass().getMethod("open", SourceDataLine.class, Map.class);
			open.invoke(synth, line, info);
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_WARNING, SoftMixer.class.getPackage().getName() + "." + SoftMixer.class.getSimpleName() + ": " + "couldn't open the synthesizer on the software mixer, opening it on the device: " + e);
			synth.open();
		}
	}

	/*
	 * "22050 Hz, 16 bit, mono" and the like (as Gervill reads its "format"
	 * property); underscores count as spaces, for command lines that can't
	 * carry them.
	 */
	static AudioFormat parseFormat(String s)
	{
		float rate = 44100f;
		int bits = 16, channels = 2;
		String prev = "";
		for (String token : s.toLowerCase().split("[,_ ]+"))
		{
			if (token.equals("mono")) { channels = 1; }
			else if (token.equals("stereo")) { channels = 2; }
			else if (token.startsWith("channel")) { channels = Integer.parseInt(prev); }
			else if (token.equals("bit")) { bits = Integer.parseInt(prev); }
			else if (token.equals("hz")) { rate = Float.parseFloat(prev); }
			prev = token;
		}
		return new AudioFormat(rate, bits, channels, true, false);
	}
}
