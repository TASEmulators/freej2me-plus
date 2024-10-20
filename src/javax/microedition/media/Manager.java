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
package javax.microedition.media;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformPlayer;

public final class Manager
{
	public static final String TONE_DEVICE_LOCATOR = "device://tone";

	/* Custom MIDI variables */
	public static boolean useCustomMidi = false;
	public static boolean hasLoadedCustomMidi = false;
	public static File soundfontDir = new File("freej2me_system" + File.separatorChar + "customMIDI" + File.separatorChar);
	private static Soundbank customSoundfont;
	public static Synthesizer customSynth;
	private static Synthesizer dedicatedTonePlayer = null;
	private static MidiChannel dedicatedToneChannel;
	
	public static boolean dumpAudioStreams = false;

	public static Player createPlayer(InputStream stream, String type) throws IOException, MediaException
	{
		checkCustomMidi();

		if(dumpAudioStreams) 
		{
			stream.mark(1024);
			String streamMD5 = generateMD5Hash(stream, 1024);
			stream.reset();

			// Copy the stream contents into a temporary stream to be saved as file
			final ByteArrayOutputStream streamCopy = new ByteArrayOutputStream();
			final byte[] copyBuffer = new byte[1024];
			int copyLength;
			while ((copyLength = stream.read(copyBuffer)) > -1 ) { streamCopy.write(copyBuffer, 0, copyLength); }
			streamCopy.flush();

			// Make sure the initial stream will still be available for FreeJ2ME
			stream = new ByteArrayInputStream(streamCopy.toByteArray());

			// And save the copy to the specified dir

			OutputStream outStream;
			String dumpPath = "." + File.separatorChar + "FreeJ2MEDumps" + File.separatorChar + "Audio" + File.separatorChar + Mobile.getPlatform().loader.suitename + File.separatorChar;
			File dumpFile = new File(dumpPath);

			if (!dumpFile.isDirectory()) { dumpFile.mkdirs(); }

			if(type.equalsIgnoreCase("audio/mid") || type.equalsIgnoreCase("audio/midi") || type.equalsIgnoreCase("sp-midi") || type.equalsIgnoreCase("audio/spmidi")) 
				{ dumpFile = new File(dumpPath + "Stream_" + streamMD5 + ".mid");}
			else if(type.equalsIgnoreCase("audio/x-wav") || type.equalsIgnoreCase("audio/wav")) { dumpFile = new File(dumpPath + "Stream_" + streamMD5 + ".wav");}
			else if(type.equalsIgnoreCase("audio/mpeg") || type.equalsIgnoreCase("audio/mp3")) { dumpFile = new File(dumpPath + "Stream_" + streamMD5 + ".mp3");}

			outStream = new FileOutputStream(dumpFile);

			streamCopy.writeTo(outStream);
		}

		return new PlatformPlayer(stream, type);
	}

	public static Player createPlayer(String locator) throws MediaException
	{
		checkCustomMidi();
		System.out.println("Create Player "+locator);
		return new PlatformPlayer(locator);
	}
	
	public static String[] getSupportedContentTypes(String protocol)
	{
		//System.out.println("Get Supported Media Content Types");
		return new String[]{"audio/midi", "audio/x-wav", 
		"audio/amr", "audio/mpeg", "audio/x-tone-seq" };
	}
	
	public static String[] getSupportedProtocols(String content_type)
	{
		System.out.println("Get Supported Media Protocols");
		return new String[]{};
	}
	
	public static void playTone(int note, int duration, int volume) throws MediaException
	{
		checkCustomMidi();
		System.out.println("Play Tone"); // Haven't found a jar that uses this method yet, but manual testing shows this already works

		if (note < 0 || note > 127) { throw new IllegalArgumentException("playTone: Note value must be between 0 and 127."); }
        if (duration <= 0) { throw new IllegalArgumentException("playTone: Note duration must be positive and non-zero."); }
        if (volume < 0) { volume = 0; } 
		else if (volume > 100) { volume = 100; }

		if(dedicatedTonePlayer == null) 
		{ 
			try  
			{ 
				dedicatedTonePlayer = MidiSystem.getSynthesizer(); 
				dedicatedTonePlayer.open();
				if(useCustomMidi && hasLoadedCustomMidi) { dedicatedTonePlayer.loadAllInstruments(customSoundfont); }

				dedicatedToneChannel = dedicatedTonePlayer.getChannels()[0]; 
			} 
			catch (MidiUnavailableException e) { System.out.println("playTone: Couldn't open Tone Player: " + e.getMessage()); return;}
		}

        /* 
		 * There's no need to calculate the note frequency as per the MIDP Manager docs,
		 * they are pretty much the note numbers used by Java's Built-in MIDI library. 
		 * Just play the note straight away, mapping the volume from 0-100 to 0-127.
		 */ 
        dedicatedToneChannel.controlChange(7, (volume * 127 / 100) );
        dedicatedToneChannel.noteOn(note, duration); // Make the decay just long enough for the note not to fade shorter than expected

        /* Since it has to be non-blocking, wait for the specified duration in a separate Thread before stopping the note. */
        new Thread(() -> 
		{
            try { Thread.sleep(duration); } 
			catch (InterruptedException e) { System.out.println("playTone: Failed to keep playing note for its specified duration: " + e.getMessage()); }
            dedicatedToneChannel.noteOff(note);
        }).start();
	}

	private static String generateMD5Hash(InputStream stream, int byteCount) 
	{
        try
		{
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] data = new byte[byteCount];
            int bytesRead = stream.read(data, 0, byteCount);

            if (bytesRead != -1) { md.update(data, 0, bytesRead); }

            // Convert MD5 hash to hex string
            StringBuilder md5Sum = new StringBuilder();
            for (byte b : md.digest()) { md5Sum.append(String.format("%02x", b)); }

            return md5Sum.toString();
        } catch (Exception e) { System.out.println("Failed to generate stream MD5:" + e.getMessage()); }

		return null;
    }

	private static final void checkCustomMidi() 
	{
		/* 
		 * Check if the user wants to run a custom MIDI soundfont. Also, there's no harm 
		 * in checking if the directory exists again.
		 */
		if(!useCustomMidi || hasLoadedCustomMidi) { return; }
		
		/* Get the first sf2 soundfont in the directory */
		String[] fontfile = soundfontDir.list(new FilenameFilter()
		{
			@Override
			public boolean accept(File f, String soundfont ) { return soundfont.toLowerCase().endsWith(".sf2"); }
		});

		/* 
		 * Only really set the player to use a custom midi soundfont if there is
		 * at least one inside the directory.
		 */
		if(fontfile != null && fontfile.length > 0) 
		{
			try 
			{
				// Load the first .sf2 font available, if there's none that's valid, don't set any and use JVM's default
				customSoundfont = MidiSystem.getSoundbank(new File(soundfontDir, fontfile[0]));
				customSynth = MidiSystem.getSynthesizer();
				customSynth.open();
				customSynth.loadAllInstruments(customSoundfont);

				hasLoadedCustomMidi = true; // We have now loaded the custom midi soundfont, mark as such so we don't waste time entering here again
			} 
			catch (Exception e) { System.out.println("Manager -> Could not load soundfont: " + e.getMessage());}
		} 
		else { System.out.println("PlatformPlayer: Custom MIDI enabled but there's no soundfont in" + (soundfontDir.getPath() + File.separatorChar)); }
	}
}
