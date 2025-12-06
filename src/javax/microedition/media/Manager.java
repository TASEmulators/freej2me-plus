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
import java.util.concurrent.locks.LockSupport;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.microedition.media.protocol.DataSource;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformPlayer;
import org.recompile.mobile.JavaxPlatformPlayer;
import org.recompile.mobile.SiemensPlatformPlayer;

public class Manager
{
	public static final String TONE_DEVICE_LOCATOR = "device://tone";
	public static final String MIDI_DEVICE_LOCATOR = "device://midi";

	/* Custom MIDI variables */
	private static File soundfontDir = new File("freej2me_system" + File.separatorChar + "customMIDI" + File.separatorChar);
	private static Soundbank customSoundfont;
	private static Soundbank defaultSoundbank = null;
	
	public static final int NUM_EXCLUSIVE_SYNTHS = 4;
	public static final Synthesizer[] exclusiveSynths = new Synthesizer[NUM_EXCLUSIVE_SYNTHS];
	public static final boolean[] synthIdxInUse = new boolean[] { false, false, false, false };
	public static Synthesizer toneSynth = null;
	public static Receiver toneReceiver = null;
	public static Sequencer toneSequencer = null;
	private static MidiChannel toneChannel;
	private static Thread toneThread;

	public static synchronized Player createPlayer(InputStream stream, String type) throws IOException, MediaException
	{
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
		if(locator == null) { throw new IllegalArgumentException("Cannot create a player with a null locator"); }

		InputStream stream = Mobile.getMIDletResourceAsStream(locator);

		if(Mobile.dumpAudioStreams && !locator.equals(Manager.TONE_DEVICE_LOCATOR) && !locator.equals(Manager.MIDI_DEVICE_LOCATOR)) 
		{
			stream = dumpAudioStream(stream, locator); // Using the locator, we can try find out what this is by parsing the file extension
		}

		Mobile.log(Mobile.LOG_WARNING, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Create Player "+locator);
		if(!locator.equals(Manager.TONE_DEVICE_LOCATOR) && !locator.equals(Manager.MIDI_DEVICE_LOCATOR)) { return new JavaxPlatformPlayer(stream, ""); } // Empty type, let PlatformPlayer handle it
		else { return new JavaxPlatformPlayer(locator); } // If it's a dedicated locator, PlatformPlayer can handle it directly
	}

	public static Player createPlayer(DataSource data) throws MediaException
	{
		if(data == null) { throw new IllegalArgumentException("Cannot create a player with a null DataSource"); }

		return createPlayer(data.getLocator());
	}

	// Siemens-Specific Manager stuff

	public static Player createSiemensPlayer(com.siemens.mp.media.protocol.DataSource source) throws MediaException
	{
		if(source == null) { throw new IllegalArgumentException("Cannot create a player with a null DataSource"); }

		Mobile.log(Mobile.LOG_WARNING, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Create Player DataSource (Siemens)");

		return new SiemensPlatformPlayer(source.getLocator());
	}

	public static synchronized Player createSiemensPlayer(InputStream stream, String type) throws IOException, MediaException
	{
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
		if(locator == null) { throw new IllegalArgumentException("Cannot create a player with a null locator"); }

		InputStream stream = Mobile.getPlatform().loader.getResourceAsStream(locator);

		if(Mobile.dumpAudioStreams && !locator.equals(Manager.TONE_DEVICE_LOCATOR) && !locator.equals(Manager.MIDI_DEVICE_LOCATOR)) 
		{
			dumpAudioStream(stream, locator); // Using the locator, we can try find out what this is by parsing the file extension
		}

		Mobile.log(Mobile.LOG_DEBUG, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Create Player "+locator);
		if(!locator.equals(Manager.TONE_DEVICE_LOCATOR) && !locator.equals(Manager.MIDI_DEVICE_LOCATOR)) { return new SiemensPlatformPlayer(stream, ""); } // Empty type, let PlatformPlayer handle it
		else { return new SiemensPlatformPlayer(locator); } // If it's a dedicated locator, PlatformPlayer can handle it directly
	}


	// Manager's helper functions
	public static String[] getSupportedContentTypes(String protocol)
	{
		Mobile.log(Mobile.LOG_DEBUG, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Get Supported Media Content Types");
		return new String[]{"audio/midi", "audio/x-wav", "audio/x-mld",
			"audio/amr", "audio/mpeg", "audio/x-tone-seq", "audio/mmf",
			"audio/x-imy", "audio/basic" };
	}
	
	public static String[] getSupportedProtocols(String content_type)
	{
		Mobile.log(Mobile.LOG_WARNING, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Get Supported Media Protocols");
		return new String[]{ TONE_DEVICE_LOCATOR, MIDI_DEVICE_LOCATOR };
	}
	
	public static void playTone(final int note, int duration, int volume) throws MediaException
	{
		if(Mobile.sound == false) { return; }
		
		Mobile.log(Mobile.LOG_DEBUG, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Play Tone");

		if (note < 0 || note > 127) { throw new IllegalArgumentException("playTone: Note value must be between 0 and 127."); }
		if (duration <= 0) { throw new IllegalArgumentException("playTone: Note duration must be positive and non-zero."); }
		if (volume < 0) { volume = 0; } 
		else if (volume > 100) { volume = 100; }

		final int restoreBankMSB = toneChannel.getController(0);    // Bank MSB
        final int restoreBankLSB = toneChannel.getController(32);   // Bank LSB
        final int restoreInstrument = toneChannel.getProgram();     // Current instrument

		toneChannel.controlChange(0, 1);   // Bank change MSB (Bank 1)
		toneChannel.controlChange(32, 0);  // Bank change LSB
		toneChannel.programChange(80);     // Set it to use the square wave instrument, just so all tones formats are using the same

		if(toneThread != null && toneThread.isAlive()) { toneThread.interrupt(); } // Interrupt the currently playing tone if one is playing

		// Notes that are too short can't even be heard in FreeJ2ME (some Karma Studios games use 10ms for sound, which is barely enough time for the media to start playing). A reasonable minimum duration is 50ms.
		if(duration < 50) { Mobile.log(Mobile.LOG_DEBUG, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Tone duration too short (" + duration + " ms), changing to the 50ms min."); }
		final int effectiveDuration = (duration < 50 ? 50 : duration);

		final byte restoreVolume = (byte) toneChannel.getController(7); // Save previous volume to restore later, as this synth is shared by everything that uses MIDI

		/* 
		 * There's no need to calculate the note frequency as per the MIDP Manager docs,
		 * they are pretty much the note numbers used by Java's Built-in MIDI library. 
		 * Just play the note straight away, mapping the volume from 0-100 to 0-127.
		 */ 
		toneChannel.controlChange(7, volume * 127 / 100);
		toneChannel.noteOn(note, effectiveDuration); // Make the decay just long enough for the note not to fade shorter than expected

		/* Since it has to be non-blocking, wait for the specified duration in a separate Thread before stopping the note. */
		toneThread = new Thread(new Runnable() 
		{
			@Override
			public void run() 
			{
				try 
				{ 
					Thread.sleep(effectiveDuration);
					toneChannel.noteOff(note);
					toneChannel.controlChange(0,  restoreBankMSB);
					toneChannel.controlChange(32, restoreBankLSB);
					toneChannel.programChange(restoreInstrument);
					toneChannel.controlChange(7, restoreVolume);
				} 
				catch (InterruptedException e) 
				{
					toneChannel.noteOff(note);
					toneChannel.controlChange(0,  restoreBankMSB);
					toneChannel.controlChange(32, restoreBankLSB);
					toneChannel.programChange(restoreInstrument);
					toneChannel.controlChange(7, restoreVolume);
					return; 
				} // The only reason for this to be interrupted is if a new tone is requested
			}
		});
		toneThread.start();
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
			else if(type.toLowerCase().contains("mld")) { dumpFile = new File(dumpPath + "Stream_" + streamMD5 + ".mld"); }
			else if(type.toLowerCase().contains("mmf")) 
			{
				stream.mark(4);
				byte[] data = new byte[4]; 
				stream.read(data);
				stream.reset();
				if((data[0] == 'M' && data[1] == 'T' && data[2] == 'h' && data[3] == 'd') ) { dumpFile = new File(dumpPath + "Stream_" + streamMD5 + "_Decoded.mid"); } // It's a sequence SMAF, converted to MIDI
				else if((data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F') ) { dumpFile = new File(dumpPath + "Stream_" + streamMD5 + "_Decoded.wav"); } // It's a PCM SMAF, converted to WAV
				else { dumpFile = new File(dumpPath + "Stream_" + streamMD5 + ".mmf"); } // Original Yamaha SMAF file
			}
			else
			{
				stream.mark(4);
				byte[] data = new byte[4]; 
				stream.read(data);
				stream.reset();
				if((data[0] == 'M' && data[1] == 'T' && data[2] == 'h' && data[3] == 'd') ) { dumpFile = new File(dumpPath + "Stream_" + streamMD5 + "_Decoded.mid"); } // Tones are converted to midi, save them as "decoded"
				else if(data.length >= 4 && data[0] == 'M' && data[1] == 'M' && data[2] == 'M' && data[3] == 'D') { dumpFile = new File(dumpPath + "Stream_" + streamMD5 + ".mmf"); } // Yamaha SMAF
				else { dumpFile = new File(dumpPath + "Stream_" + streamMD5 + ".ota"); } // Nokia OTA
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
		 * If the directory for custom soundfonts doesn't exist, create it, no matter if the user
		 * is going to use it or not.
		 */
		if(!soundfontDir.isDirectory()) 
		{
			try 
			{
				soundfontDir.mkdirs();
				File dummyFile = new File(soundfontDir.getPath() + File.separatorChar + "place sf2 file or gm for early java 6 here");
				dummyFile.createNewFile();
			}
			catch(IOException e) { Mobile.log(Mobile.LOG_ERROR, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Failed to create custom midi dir:" + e.getMessage()); }
		}
		
		/* Get the first sf2 or gm soundfont in the directory */
		String[] fontfile = soundfontDir.list(new FilenameFilter() 
		{
			@Override
			public boolean accept(File f, String soundfont) 
			{
				String lowerCaseFont = soundfont.toLowerCase();
				return lowerCaseFont.endsWith(".sf2") || lowerCaseFont.endsWith(".gm");
			}
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
			} 
			catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Could not load soundfont into synth: " + e.getMessage());}
		}
		else if (!Mobile.useCustomMidi) { customSoundfont = defaultSoundbank; }
		else 
		{ 
			Mobile.log(Mobile.LOG_WARNING, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Custom MIDI enabled but there's no soundfont in: " + (soundfontDir.getPath() + File.separatorChar)); 
			customSoundfont = defaultSoundbank;
		}
	}

	public static void changeCustomMidi() 
	{
		try 
		{
			boolean wasPlaying = false;
			checkCustomMidi();

			// Maybe we went through here before, make sure to stop the sequencers before changing soundfont
			if (toneSequencer != null && toneSequencer.isRunning()) 
			{
				toneSequencer.stop();
				wasPlaying = true;
			}

			for(int i = 0; i < NUM_EXCLUSIVE_SYNTHS; i++) 
			{
				if(exclusiveSynths[i] != null) { exclusiveSynths[i].loadAllInstruments(Manager.getCustomSoundfont()); }
			}
			
			// Restart the sequencer if needed
			if (toneSequencer != null && wasPlaying) 
			{
				toneSequencer.start();
				wasPlaying = false;
			}
		}
		catch (Exception e) {Mobile.log(Mobile.LOG_WARNING, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Failed to change MIDI soundfont: " + e.getMessage()); }
	}

	public static Synthesizer prepareSynthesizer() throws MidiUnavailableException
	{
		Synthesizer synth = MidiSystem.getSynthesizer(); 
		synth.open();

		defaultSoundbank = synth.getDefaultSoundbank();

		changeCustomMidi();

		synth.loadAllInstruments(customSoundfont);

		return synth;
	}

	public static int retrieveAvailableSynthIndex() 
	{
		for(int i = 0; i < NUM_EXCLUSIVE_SYNTHS; i++) 
		{
			if(synthIdxInUse[i] == false) { return i; }
		}

		return NUM_EXCLUSIVE_SYNTHS-1; // We have no option but to reuse a synth here, as the four exclusive ones are all in use
	}

	public static Soundbank getCustomSoundfont() { return customSoundfont; }

	public static void prepareMediaEngine() 
	{
		try  
		{
			for(int i = 0; i < NUM_EXCLUSIVE_SYNTHS; i++) 
			{
				exclusiveSynths[i] = prepareSynthesizer();
			}
			toneSynth = exclusiveSynths[NUM_EXCLUSIVE_SYNTHS-1]; // Get the last synth of PlatformPlayer
			toneReceiver = toneSynth.getReceiver();	
			toneChannel = toneSynth.getChannels()[15]; // Also get the last channel of the last synth, to minimize chances of this causing issues with other MIDI streams

			toneSequencer = MidiSystem.getSequencer(false);
			toneSequencer.getTransmitter().setReceiver(toneReceiver);
			toneSequencer.open();

			Mobile.log(Mobile.LOG_DEBUG, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Synthesizer for sequenced and tone data is ready.");
		} 
		catch (MidiUnavailableException e) { Mobile.log(Mobile.LOG_ERROR, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Couldn't open Tone Player: " + e.getMessage()); }
	}
}
