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

import org.recompile.freej2me.FreeJ2ME;
import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformPlayer;
import org.recompile.mobile.JavaxPlatformPlayer;
import org.recompile.mobile.SiemensPlatformPlayer;

public class Manager
{
	public static final String TONE_DEVICE_LOCATOR = "device://tone";
	public static final String MIDI_DEVICE_LOCATOR = "device://midi";

	/* Custom MIDI variables */
	public static boolean hasLoadedSynth = false;
	public static boolean hasLoadedToneSynth = false;
	public static File soundfontDir = new File("freej2me_system" + File.separatorChar + "customMIDI" + File.separatorChar);
	private static Soundbank customSoundfont;
	public static Synthesizer mainSynth;
	private static Synthesizer dedicatedTonePlayer = null;
	private static MidiChannel dedicatedToneChannel;

	public static synchronized Player createPlayer(InputStream stream, String type) throws IOException, MediaException
	{
		checkCustomMidi();

		if (stream == null) { throw new IllegalArgumentException("Cannot create a player since the received stream is null"); }
		/* 
		 * NOTE: If type is null, we can either try to determine the type, or throw a MediaException. Some jars do use exceptions
		 * here as part of the game logic (Sonic Spinball K800i uses the exception above in order to load its streams properly), so
		 * only a lot of testing will be able to determine which is preferable, or if we'd need a config toggle to alternate both.
		 */

		if(Mobile.dumpAudioStreams) { stream = dumpAudioStream(stream, type); }

		return new JavaxPlatformPlayer(stream, type);
	}

	public static Player createPlayer(String locator) throws MediaException
	{
		checkCustomMidi();

		if(locator == null) { throw new IllegalArgumentException("Cannot create a player with a null locator"); }

		InputStream stream = Mobile.getPlatform().loader.getResourceAsStream(locator);

		if(Mobile.dumpAudioStreams && !locator.equals(Manager.TONE_DEVICE_LOCATOR) && !locator.equals(Manager.MIDI_DEVICE_LOCATOR)) 
		{
			dumpAudioStream(stream, locator); // Using the locator, we can try find out what this is by parsing the file extension
		}

		Mobile.log(Mobile.LOG_WARNING, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Create Player "+locator);
		if(!locator.equals(Manager.TONE_DEVICE_LOCATOR) && !locator.equals(Manager.MIDI_DEVICE_LOCATOR)) { return new JavaxPlatformPlayer(stream, ""); } // Empty type, let PlatformPlayer handle it
		else { return new JavaxPlatformPlayer(locator); } // If it's a dedicated locator, PlatformPlayer can handle it directly
	}

	// Siemens-Specific Manager stuff

	public static Player createSiemensPlayer(com.siemens.mp.media.protocol.DataSource source) throws MediaException
	{
		checkCustomMidi();

		if(source == null) { throw new IllegalArgumentException("Cannot create a player with a null DataSource"); }

		Mobile.log(Mobile.LOG_WARNING, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Create Player DataSource");

		return null; // mew SiemensPlatformPlayer(TODO)
	}

	public static synchronized Player createSiemensPlayer(InputStream stream, String type) throws IOException, MediaException
	{
		checkCustomMidi();

		if (stream == null) { throw new IllegalArgumentException("Cannot create a player since the received stream is null"); }
		/* 
		 * NOTE: If type is null, we can either try to determine the type, or throw a MediaException. Some jars do use exceptions
		 * here as part of the game logic (Sonic Spinball K800i uses the exception above in order to load its streams properly), so
		 * only a lot of testing will be able to determine which is preferable, or if we'd need a config toggle to alternate both.
		 */

		if(Mobile.dumpAudioStreams) { stream = dumpAudioStream(stream, type); }

		return new SiemensPlatformPlayer(stream, type);
	}

	public static Player createSiemensPlayer(String locator) throws MediaException
	{
		checkCustomMidi();

		if(locator == null) { throw new IllegalArgumentException("Cannot create a player with a null locator"); }

		InputStream stream = Mobile.getPlatform().loader.getResourceAsStream(locator);

		if(Mobile.dumpAudioStreams && !locator.equals(Manager.TONE_DEVICE_LOCATOR) && !locator.equals(Manager.MIDI_DEVICE_LOCATOR)) 
		{
			dumpAudioStream(stream, locator); // Using the locator, we can try find out what this is by parsing the file extension
		}

		Mobile.log(Mobile.LOG_WARNING, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Create Player "+locator);
		if(!locator.equals(Manager.TONE_DEVICE_LOCATOR) && !locator.equals(Manager.MIDI_DEVICE_LOCATOR)) { return new SiemensPlatformPlayer(stream, ""); } // Empty type, let PlatformPlayer handle it
		else { return new SiemensPlatformPlayer(locator); } // If it's a dedicated locator, PlatformPlayer can handle it directly
	}


	// Manager's helper functions
	
	public static String[] getSupportedContentTypes(String protocol)
	{
		Mobile.log(Mobile.LOG_DEBUG, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Get Supported Media Content Types");
		return new String[]{"audio/midi", "audio/x-wav", 
		"audio/amr", "audio/mpeg", "audio/x-tone-seq" };
	}
	
	public static String[] getSupportedProtocols(String content_type)
	{
		Mobile.log(Mobile.LOG_WARNING, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Get Supported Media Protocols");
		return new String[]{};
	}
	
	public static void playTone(int note, int duration, int volume) throws MediaException
	{
		checkCustomMidi();
		Mobile.log(Mobile.LOG_DEBUG, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Play Tone");

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
				if(Mobile.useCustomMidi && !hasLoadedToneSynth) { dedicatedTonePlayer.loadAllInstruments(customSoundfont); hasLoadedToneSynth = true; }

				dedicatedToneChannel = dedicatedTonePlayer.getChannels()[0]; 
			} 
			catch (MidiUnavailableException e) { Mobile.log(Mobile.LOG_ERROR, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Couldn't open Tone Player: " + e.getMessage()); return;}
		}

		dedicatedToneChannel.programChange(80); // Set it to use the square wave instrument, just so all tones formats are using the same
		/* 
		 * There's no need to calculate the note frequency as per the MIDP Manager docs,
		 * they are pretty much the note numbers used by Java's Built-in MIDI library. 
		 * Just play the note straight away, mapping the volume from 0-100 to 0-127.
		 */ 
		dedicatedToneChannel.controlChange(7, volume * 127 / 100);
		dedicatedToneChannel.noteOn(note, duration); // Make the decay just long enough for the note not to fade shorter than expected

		/* Since it has to be non-blocking, wait for the specified duration in a separate Thread before stopping the note. */
		new Thread(() -> 
		{
            try { Thread.sleep(duration); } 
			catch (InterruptedException e) { Mobile.log(Mobile.LOG_ERROR, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Failed to keep playing note for its specified duration: " + e.getMessage()); }
            dedicatedToneChannel.noteOff(note);
        }).start();
	}

	public static final InputStream dumpAudioStream(InputStream stream, String type) 
	{
		try 
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

			// TODO: Locators will break this sometimes, since file names might also contain those substrings.
			if(type.toLowerCase().contains("mid") )     { dumpFile = new File(dumpPath + "Stream_" + streamMD5 + ".mid"); }
			else if(type.toLowerCase().contains("wav")) { dumpFile = new File(dumpPath + "Stream_" + streamMD5 + ".wav"); }
			else if(type.toLowerCase().contains("mp"))  { dumpFile = new File(dumpPath + "Stream_" + streamMD5 + ".mp3"); }
			else if(type.equalsIgnoreCase("audio/x-tone-seq")) 
			{
				stream.mark(4);
				byte[] data = new byte[4]; 
				stream.read(data);
				if((data[0] == 'M' && data[1] == 'T' && data[2] == 'h' && data[3] == 'd') ) { dumpFile = new File(dumpPath + "Stream_" + streamMD5 + "_Decoded.mid"); } // Tone Seq is converted to midi, save it as "decoded"
				else { dumpFile = new File(dumpPath + "Stream_" + streamMD5 + ".ota"); } // Only nokia OTA tones are going to be dumped in their original form right now. Dual Tone and MelodyComposer have no format of their own.
				stream.reset();
			}

			outStream = new FileOutputStream(dumpFile);

			streamCopy.writeTo(outStream);
		}
		catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Failed to dump media: " + e.getMessage()); }

		return stream;
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
        } catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Failed to generate stream MD5:" + e.getMessage()); }

		return null;
    }

	private static final void checkCustomMidi() 
	{
		/* 
		 * Check if the user wants to run a custom MIDI soundfont. Also, there's no harm 
		 * in checking if the directory exists again. If it has already been loaded, jsut return.
		 */
		if(hasLoadedSynth) { return; }

		/* 
		 * If the directory for custom soundfonts doesn't exist, create it, no matter if the user
		 * is going to use it or not.
		 */
		if(!soundfontDir.isDirectory()) 
		{
			try 
			{
				soundfontDir.mkdirs();
				File dummyFile = new File(soundfontDir.getPath() + File.separatorChar + "Put your sf2 bank here");
				dummyFile.createNewFile();
			}
			catch(IOException e) { Mobile.log(Mobile.LOG_ERROR, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Failed to create custom midi dir:" + e.getMessage()); }
		}
		
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
		if(Mobile.useCustomMidi && fontfile != null && fontfile.length > 0) 
		{
			try 
			{
				// Load the first .sf2 font available, if there's none that's valid, don't set any and use JVM's default
				customSoundfont = MidiSystem.getSoundbank(new File(soundfontDir, fontfile[0]));
				mainSynth = MidiSystem.getSynthesizer();
				mainSynth.open();
				mainSynth.loadAllInstruments(customSoundfont);

				PlatformPlayer.synthesizer = mainSynth;
				PlatformPlayer.receiver = mainSynth.getReceiver();

				hasLoadedSynth = true; // We have now loaded the custom midi soundfont, mark as such so we don't waste time entering here again
			} 
			catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Could not load soundfont into synth: " + e.getMessage());}
		}
		else if (!Mobile.useCustomMidi) 
		{
			try 
			{
				mainSynth = MidiSystem.getSynthesizer();
				mainSynth.open();

				PlatformPlayer.synthesizer = mainSynth;
				PlatformPlayer.receiver = mainSynth.getReceiver();

				hasLoadedSynth = true;
			}
			catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Could not load default synth: " + e.getMessage());}
		}
		else 
		{ 
			Mobile.log(Mobile.LOG_WARNING, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Custom MIDI enabled but there's no soundfont in" + (soundfontDir.getPath() + File.separatorChar)); 

			try 
			{
				mainSynth = MidiSystem.getSynthesizer();
				mainSynth.open();

				PlatformPlayer.synthesizer = mainSynth;
				PlatformPlayer.receiver = mainSynth.getReceiver();

				hasLoadedSynth = true;
			}
			catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Could not load default synth: " + e.getMessage());}
		}
	}
}
