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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Vector;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.ToneControl;
import javax.microedition.media.Control;
import javax.microedition.media.Controllable;
import javax.microedition.media.Manager;

/* audio/mpeg support */
import javazoom.jl.player.MPEGPlayer;

public class PlatformPlayer implements Player
{

	private final byte NUM_CONTROLS = 4;

	private String contentType = "";

	private audioplayer player;

	private int state = Player.UNREALIZED;

	private Vector<PlayerListener> listeners;

	private Control[] controls;

	public PlatformPlayer(InputStream stream, String type)
	{
		listeners = new Vector<PlayerListener>();
		controls = new Control[NUM_CONTROLS];

		contentType = type;

		if(Mobile.sound == false)
		{
			player = new audioplayer();
		}
		else
		{
			if(type.equalsIgnoreCase("audio/mid") || type.equalsIgnoreCase("audio/midi") || type.equalsIgnoreCase("sp-midi") || type.equalsIgnoreCase("audio/spmidi"))
			{
				player = new midiPlayer(stream);
			}
			else if(type.equalsIgnoreCase("audio/x-wav") || type.equalsIgnoreCase("audio/wav"))
			{
				player = new wavPlayer(stream);
			}
			else if(type.equalsIgnoreCase("audio/mpeg") || type.equalsIgnoreCase("audio/mp3"))
			{
				player = new MP3Player(stream);
			}
			else if (type.equalsIgnoreCase("")) /* If the stream doesn't have an accompanying type, try everything we can to try and load it */
			{
				try 
				{
					final byte[] tryStream = new byte[stream.available()];
					readInputStreamData(stream, tryStream, 0, stream.available());

					Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Received no explicit audio type. Trying to load as MIDI, and if it fails, WAV.");
					/* Try loading it as a MIDI file first */
					try { player = new midiPlayer(new ByteArrayInputStream(tryStream)); } 
					catch (Exception e) { }
					
					/* If that doesn't work, try as WAV next, if it still doesn't work, we have no other players to try */
					try { player = new wavPlayer(new ByteArrayInputStream(tryStream)); }
					catch (Exception e)
					{
						Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "No Player For: "+contentType);
						player = new audioplayer();
					}
				}
				catch (IOException e)
				{
					Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Couldn't read input stream: " + e.getMessage());
				}
			}
			else if (type.equalsIgnoreCase("audio/x-tone-seq")) 
			{
				// Very early stages of implementation, needs to be fleshed out
				player = new tonePlayer(stream);
			}
			else /* TODO: Implement a player for amr audio types */
			{
				Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "No Player For: "+contentType);
				player = new audioplayer();
			}
		}
		controls[0] = new volumeControl(this.player);

		/* Midi Player has a few additional controls */
		if(player instanceof midiPlayer) 
		{
			controls[1] = new tempoControl((midiPlayer) this.player);
			controls[2] = new midiControl((midiPlayer) this.player);
			controls[3] = new toneControl((midiPlayer) this.player);
		}
		

		Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "media type: "+type);
	}

	public PlatformPlayer(String locator)
	{
		player = new audioplayer();
		listeners = new Vector<PlayerListener>();
		controls = new Control[3];
		Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Player locator: "+locator);
	}

	public void close()
	{
		if(getState() == Player.CLOSED) { return; }

		try
		{
			player.stop();
			player.deallocate(); /* Call player's deallocate directly, otherwise we'll realize() again */
			state = Player.CLOSED;
			player = null;
			notifyListeners(PlayerListener.CLOSED, null);	
		}
		catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Could not close player: " + e.getMessage()); }
	}

	public int getState() { return state; }

	public void start()
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot call start() on a CLOSED player."); }
		
		if(getState() == Player.REALIZED || getState() == Player.UNREALIZED) { prefetch(); }

		if(getState() == Player.PREFETCHED) { player.start(); }
	}

	public void stop()
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot call stop() on a CLOSED player."); }
		
		if(getState() == Player.STARTED) 
		{ 
			try { player.stop(); }
			catch (Exception e) { }
		}
	}

	public void addPlayerListener(PlayerListener playerListener)
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot add PlayerListener to a CLOSED player"); }
		if(playerListener == null) { return; }

		listeners.add(playerListener);
	}

	public void removePlayerListener(PlayerListener playerListener)
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot remove PlayerListener from a CLOSED player"); }
		if(playerListener == null) { return; }

		listeners.remove(playerListener);
	}

	private void notifyListeners(String event, Object eventData)
	{
		for(int i=0; i<listeners.size(); i++)
		{
			listeners.get(i).playerUpdate(this, event, eventData);
		}
	}

	public void deallocate()
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot deallocate player, it is already CLOSED."); }

		stop();
		player.deallocate();

		/* 
		 * Only set state to REALIZED if we have effectively moved into REALIZED or higher (PREFETCHED, etc), 
		 * as deallocate can be called during the transition from UNREALIZED to REALIZED, and if that happens,
		 * we can't actually set it as REALIZED, it must be kept as UNREALIZED.
		*/
		if(state > Player.UNREALIZED) 
		{
			player.realize();
			state = Player.REALIZED;
		}	
	}

	public String getContentType() 
	{
		if(getState() == Player.UNREALIZED || getState() == Player.CLOSED) { throw new IllegalStateException("Cannot get content type. Player is either CLOSED or UNREALIZED."); }
		
		return contentType; 
	}

	public long getDuration() 
	{ 
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot call getDuration() on a CLOSED player."); }

		if(getState() == Player.REALIZED || getState() == Player.UNREALIZED) { return Player.TIME_UNKNOWN; }
		
		return player.getDuration(); // Maybe not really needed? We should find a jar that actually uses this for something
	}

	public long getMediaTime() 
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot call getMediaTime on a CLOSED player."); }
		
		return player.getMediaTime(); 
	}

	/* Both midi and wav players do little more than just set their state as PREFETCHED here. */
	public void prefetch() 
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot prefetch player, as it is in the CLOSED state."); }
		
		if(getState() == Player.UNREALIZED) { realize(); }
		
		if(getState() == Player.REALIZED) { player.prefetch(); }
	}

	public void realize() 
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot realize player, as it is in the CLOSED state"); }
		
		if(getState() == Player.UNREALIZED) { player.realize(); }
	}

	public void setLoopCount(int count) 
	{
		/* A MIDlet setting 0 explicitly here is illegal */
		if(count == 0) {throw new IllegalStateException("Jar tried to set loop count as 0. ");}
		if(getState() == Player.CLOSED || getState() == Player.STARTED) { throw new IllegalStateException("Jar tried to set loop count on a player in an invalid state. "); }

		player.setLoopCount(count); 
	}

	public long setMediaTime(long now) 
	{
		if(getState() == Player.UNREALIZED || getState() == Player.CLOSED) { throw new IllegalStateException("Cannot set Media Time. Player is either UNREALIZED or CLOSED. "); }

		return player.setMediaTime(now);
	}

	// Controllable interface //

	public Control getControl(String controlType)
	{
		if(getState() == Player.CLOSED || getState() == Player.UNREALIZED) { throw new IllegalStateException("Cannot call getControl(), as the player is either CLOSED or UNREALIZED."); }

		if(controlType.equals("VolumeControl")) { return controls[0]; }
		if(controlType.equals("TempoControl")) { return controls[1]; }
		if(controlType.equals("MIDIControl")) { return controls[2]; }
		if(controlType.equals("ToneControl")) { return controls[3]; }
		if(controlType.equals("javax.microedition.media.control.VolumeControl")) { return controls[0]; }
		if(controlType.equals("javax.microedition.media.control.TempoControl")) { return controls[1]; }
		if(controlType.equals("javax.microedition.media.control.MIDIControl")) { return controls[2]; }
		if(controlType.equals("javax.microedition.media.control.ToneControl")) { return controls[3]; }
		
		return null;
	}

	public Control[] getControls() 
	{ 
		if(getState() == Player.CLOSED || getState() == Player.UNREALIZED) { throw new IllegalStateException("Cannot call getControls(), as the player is either CLOSED or UNREALIZED."); }

		return controls; 
	}

	/* Read 'n' Bytes from the InputStream. Used by IMA ADPCM decoder as well. */
	public static void readInputStreamData(InputStream input, byte[] output, int offset, int nBytes) throws IOException 
	{
		int end = offset + nBytes;
		while(offset < end) 
		{
			int read = input.read(output, offset, end - offset);
			if(read < 0) throw new java.io.EOFException();
			offset += read;
		}
	}

	// Players //

	private class audioplayer
	{
		public void start() {  }
		public void stop() {  }
		public void setLoopCount(int count) {  }
		public long setMediaTime(long now) { return now; }
		public long getMediaTime() { return 0; }
		public boolean isRunning() { return false; }
		public void deallocate() {  }
		public void realize() { }
		public void prefetch() { }
		public long getDuration() { return Player.TIME_UNKNOWN; }

		/* Copy the stream to a local variable, as we need it in order to realize() and prefetch() on midi and wav players */
		protected byte[] copyMediaData(InputStream stream) 
		{
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int bytesRead;
			try 
			{
				while ((bytesRead = stream.read(buffer)) != -1) 
				{
					byteArrayOutputStream.write(buffer, 0, bytesRead);
				}
			} catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Failed to copy audio stream data:" + e.getMessage()); }
			

			return byteArrayOutputStream.toByteArray();
		}
	}

	private class midiPlayer extends audioplayer
	{
		private byte[] stream;
		private Sequencer midi;
		private Sequence midiSequence;

		public midiPlayer(InputStream stream)
		{
			try
			{
				/* Open the midi stream without a receiver, so that we can load up the custom soundfont if available */
				midi = MidiSystem.getSequencer(false);
				this.stream = copyMediaData(stream);
			}
			catch (Exception e) 
			{ 
				Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Couldn't load midi file:" + e.getMessage());
			}
		}

		public void realize() 
		{ 
			try 
			{ 
				midi.open();
				if(Manager.useCustomMidi && Manager.hasLoadedCustomMidi) 
				{
					midi.getTransmitter().setReceiver(Manager.customSynth.getReceiver()); 
				}
				else
				{
					midi.getTransmitter().setReceiver(MidiSystem.getReceiver());
				}

				/* Make a new copy of the media stream, as realize() can be called more than once during the player's lifecycle */
				midiSequence = MidiSystem.getSequence(new ByteArrayInputStream(stream));
				midi.setSequence(midiSequence);
				state = Player.REALIZED; 
			} 
			catch (Exception e) 
			{ 
				Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Could not realize midi stream:" + e.getMessage());
				deallocate();
				state = Player.UNREALIZED; 
			}
		}

		public void prefetch() { state = Player.PREFETCHED; }

		public void start()
		{
			if(getMediaTime() >= getDuration()) { setMediaTime(0); }
			midi.start();

			/* 
			 * We have to listen for END_OF_MEDIA events, or else jars that rely on this
			 * won't work in the expected way.
			 */
			midi.addMetaEventListener(new MetaEventListener() 
			{
				@Override
				public void meta(MetaMessage meta) 
				{
					if (meta.getType() == 0x2F) // 0x2F = END_OF_MEDIA in Sequencer
					{
						notifyListeners(PlayerListener.END_OF_MEDIA, getMediaTime());
						state = Player.PREFETCHED;
					}
				}
			});
			state = Player.STARTED;
			notifyListeners(PlayerListener.STARTED, getMediaTime());
		}

		public void stop()
		{
			midi.stop();
			state = Player.PREFETCHED;
			notifyListeners(PlayerListener.STOPPED, getMediaTime());
		}

		public void deallocate() { midi.close(); }

		public void setLoopCount(int count)
		{
			/* 
			 * Treat cases where an app wants this stream to loop continuously.
			 * Here, count = 1 means it should loop one time, whereas in j2me
			 * it appears that count = 1 means no loop at all, at least based
			 * on Gameloft games that set effects and some music with count = 1
			 */
			if(count == Clip.LOOP_CONTINUOUSLY) { midi.setLoopCount(count); }
			else { midi.setLoopCount(count-1); }
		}

		public long setMediaTime(long now)
		{
			try 
			{
				if(now >= getDuration()) { midi.setMicrosecondPosition(getDuration()); }
				else if(now < 0) { midi.setMicrosecondPosition(0); }
				else { midi.setMicrosecondPosition(now);  }
				midi.open();
			}
			catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Failed to set MIDI position:" + e.getMessage()); }
			
			/* 
			 * MicrosecondPosition doesn't guarantee perfect precision, so return the new
			 * effective position according to the stream.
			 */
			return getMediaTime();
		}

		public long getMediaTime() { return midi.getMicrosecondPosition(); }

		public long getDuration() { return midi.getMicrosecondLength(); }

		public boolean isRunning() { return midi.isRunning(); }
	}

	private class wavPlayer extends audioplayer
	{
		/* PCM WAV variables */
		private byte[] stream;
		private AudioInputStream wavStream;
		private Clip wavClip;
		/* IMA ADPCM WAV variables */
		InputStream decodedStream;
		private int[] wavHeaderData = new int[4];

		public wavPlayer(InputStream stream)
		{
			/*
			 * A wav header is generally 44-bytes long (60 for IMA ADPCM), and it is what we need to read in order 
			 * to get the stream's format, frame size, bit rate, number of channels, etc. which gives us information
			 * on the kind of codec needed to play or decode the incoming stream. The stream needs to be reset
			 * or else PCM files will be loaded without a header and it might cause issues with playback.
			 */
			try 
			{
				stream.mark(60);
				wavHeaderData = WavImaAdpcmDecoder.readHeader(stream);
				stream.reset();
				this.stream = copyMediaData(stream);
			} catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Could not prepare wav stream:" + e.getMessage());}
		}

		public void realize() 
		{ 
			try
			{
				/* We only check for IMA ADPCM at the moment. */
				if(wavHeaderData[0] != 17) /* If it's not IMA ADPCM we don't need to do anything to the stream. */
				{
					/* Same idea as midiPlayer, operate on a new copy of the media stream */
					wavStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(stream));
					wavClip = AudioSystem.getClip();
					wavClip.open(wavStream);
					state = Player.REALIZED;
				}
				else /* But if it is IMA ADPCM, we have to decode it manually. */
				{
					decodedStream = WavImaAdpcmDecoder.decodeImaAdpcm(new ByteArrayInputStream(stream), wavHeaderData);
					wavStream = AudioSystem.getAudioInputStream(decodedStream);
					wavClip = AudioSystem.getClip();
					wavClip.open(wavStream);
					state = Player.REALIZED;
				}
			}
			catch (Exception e) 
			{ 
				Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Couldn't realize wav stream: " + e.getMessage());
				wavClip.close();
			}
		}

		public void prefetch() { state = Player.PREFETCHED; }

		public void start()
		{	
			if(getMediaTime() >= getDuration()) { setMediaTime(0); }
			wavClip.start();

			/* Like for midi, we need to listen for END_OF_MEDIA events here too. */
			wavClip.addLineListener(new LineListener() 
			{
				@Override
				public void update(LineEvent event) 
				{
					if (event.getType() == LineEvent.Type.STOP) 
					{
						notifyListeners(PlayerListener.END_OF_MEDIA, getMediaTime());
						state = Player.PREFETCHED;
					}
				}
			});

			state = Player.STARTED;
			notifyListeners(PlayerListener.STARTED, getMediaTime());
		}

		public void stop()
		{
			wavClip.stop();
			state = Player.PREFETCHED;
			notifyListeners(PlayerListener.STOPPED, getMediaTime());
		}

		public void deallocate() { wavClip = null; }

		public void setLoopCount(int count)
		{
			/* 
			 * Treat cases where an app wants this stream to loop continuously.
			 * Here, count = 1 means it should loop one time, whereas in j2me
			 * it appears that count = 1 means no loop at all, at least based
			 * on Gameloft games that set effects and some music with count = 1
			 */
			if(count == Clip.LOOP_CONTINUOUSLY) { wavClip.loop(count); }
			else { wavClip.loop(count-1); }
		}

		public long setMediaTime(long now)
		{
			if(now >= getDuration()) { wavClip.setMicrosecondPosition(getDuration()); }
			else if(now < 0) { wavClip.setMicrosecondPosition(0); }
			else { wavClip.setMicrosecondPosition(now);  }

			/* 
			 * MicrosecondPosition doesn't guarantee perfect precision, so return the new
			 * effective position according to the stream.
			 */
			return getMediaTime();
		}

		public long getMediaTime() { return wavClip.getMicrosecondPosition(); }

		public long getDuration() { return  wavClip.getMicrosecondLength(); }

		public boolean isRunning() { return wavClip.isRunning(); }
	}

	private class MP3Player extends audioplayer
	{
		private byte[] stream;
		private MPEGPlayer mp3Player;
		private Thread playerThread;

		public MP3Player(InputStream stream)
		{
			try { this.stream = copyMediaData(stream); }
			catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Could not prepare mpeg stream:" + e.getMessage());}
		}

		public void realize() 
		{ 
			try
			{
				mp3Player = new MPEGPlayer(new ByteArrayInputStream(stream), false);
				state = Player.REALIZED;
			}
			catch (Exception e) 
			{ 
				Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Couldn't realize mpeg stream: " + e.getMessage());
				mp3Player.close();
			}
		}

		public void prefetch() { state = Player.PREFETCHED; }

		public void start()
		{	
			if(getMediaTime() >= getDuration()) { setMediaTime(0); }

			try 
			{
				playerThread = new Thread(() -> 
				{ 
					try 
					{ 
						boolean completed = mp3Player.play();
					}
					catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Couldn't start mpeg player:" + e.getMessage()); }
				});

				/* TODO: MPEGPlayer needs some way of notifying listeners about END_OF_MEDIA events */
				/* mp3Player.addLineListener(new LineListener() 
				{
					@Override
					public void update(LineEvent event) 
					{
						if (event.getType() == LineEvent.Type.STOP) 
						{
							notifyListeners(PlayerListener.END_OF_MEDIA, getMediaTime());
							state = Player.PREFETCHED;
						}
					}
				});*/

				playerThread.start();

				state = Player.STARTED;
				notifyListeners(PlayerListener.STARTED, getMediaTime());
			} catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Couldn't start mpeg player:" + e.getMessage()); }
			
		}

		public void stop()
		{
			mp3Player.stop();
			state = Player.PREFETCHED;
			notifyListeners(PlayerListener.STOPPED, getMediaTime());
		}

		public void deallocate() { mp3Player = null; }

		public void setLoopCount(int count)
		{
			/* 
			 * Treat cases where an app wants this stream to loop continuously.
			 * Here, count = 1 means it should loop one time, whereas in j2me
			 * it appears that count = 1 means no loop at all, at least based
			 * on Gameloft games that set effects and some music with count = 1
			 */
			if(count == Clip.LOOP_CONTINUOUSLY) { mp3Player.loop(count); }
			else { mp3Player.loop(count-1); }
		}

		public long setMediaTime(long now)
		{
			if(now >= getDuration()) { mp3Player.setMicrosecondPosition(getDuration()); }
			else if(now < 0) { mp3Player.setMicrosecondPosition(0); }
			else { mp3Player.setMicrosecondPosition(now);  }

			/* 
			 * IN MP3Player's case, we don't deal with microsecond resolution, so return the new
			 * effective position according to the stream.
			 */
			return getMediaTime();
		}

		public long getMediaTime() { return mp3Player.getMicrosecondPosition(); }

		public long getDuration() { return  mp3Player.getDuration(); }

		public boolean isRunning() { return mp3Player.isRunning(); }
	}

	/* Todo: Implement tone playing functionality */
	private class tonePlayer extends audioplayer 
	{
		private InputStream toneStream;
		private int loops = 0;

		public tonePlayer(InputStream stream) { Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Tone Player"); toneStream = stream; }

		public void start() 
		{  
			// Todo implement functionality to play a tone sequence
			state = Player.STARTED;
		}

		public void stop() 
		{ 
			// Todo implement functionality to stop playing a tone sequence
			state = Player.PREFETCHED;
		}

		public void setLoopCount(int count) { if (count > -1) { loops = count; } }

		public boolean isRunning() 
		{ 
			// Todo implementation depends on start()
			return false; 
		}
	}

	// Controls //

	/* midiControl is untested */
	private class midiControl implements javax.microedition.media.control.MIDIControl
	{
		private midiPlayer player;

		/* 
		 * Java, by default, does not directly support any of the query methods
		 * below, which means we must implement them, and track the state of 
		 * everything that they change ourselves.
		 */
		private int[] channelVolume = new int[16]; // For getChannelVolume
		//private int[] programs = new int[16]; // For getProgram

		public midiControl(midiPlayer player) 
		{ 
			this.player = player;
			for(int channel = 0; channel < channelVolume.length; channel++) { channelVolume[channel] = 127; }
		}

		public int[] getBankList(boolean custom) 
		{ 
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: getBankList()");
			return new int[]{}; 
		}

		public int getChannelVolume(int channel) 
		{ 
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: getChannelVolume()");
			return channelVolume[channel];
		}

		public java.lang.String getKeyName(int bank, int prog, int key) 
		{ 
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: getKeyName()");
			return ""; 
		}

		public int[] getProgram(int channel) 
		{ 
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: getProgram()");
			return new int[]{}; 
		}

		public int[] getProgramList(int bank) 
		{ 
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: getProgramList()");
			return new int[]{}; 
		}

		public java.lang.String getProgramName(int bank, int prog) 
		{ 
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: getProgramName()");
			return ""; 
		}

		public boolean isBankQuerySupported() 
		{ 
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: isBankQuerySupported()");
			return false; 
		}

		public int longMidiEvent(byte[] data, int offset, int length) 
		{
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: longMidiEvent()");

			if(data == null || offset < 0 || length < 0) { throw new IllegalArgumentException("MmidiControl: Invalid arguments for shortMidiEvent()"); }

			/* longMidiEvent sends System Exclusive messages hence the SysexMessage data */
			Track[] tracks = player.midiSequence.getTracks();
			try 
			{
				if (data.length >= offset + length && length > 0) 
				{
					MidiEvent event = new MidiEvent(new SysexMessage(data, length), 0);
					tracks[0].add(event); // Add to track 0; adjust as necessary
					return 1; // Return 1 to indicate success
				}
			} catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Failed to send long MIDI event: " + e.getMessage()); }

			return 0; 
		}

		public void setChannelVolume(int channel, int volume) 
		{
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: setChannelVolume()");

			if(channel < 0 || channel > 15 || volume < 0 || volume > 127) {throw new IllegalArgumentException("midiControl: Tried to call setChannelVolume with invalid args");}

			Track[] track = player.midiSequence.getTracks();

			try 
			{
				track[channel].add(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, 7, volume), 0));
				channelVolume[channel] = volume; // Update the stored volume
			}
			catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Midi setChannelVolume failed: " + e.getMessage());}
		}

		public void setProgram(int channel, int bank, int program) 
		{  
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: setProgram()");

			/* Track[] track = player.midiSequence.getTracks();
			try 
			{
				track[channel].add(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, 0, bank), 0));
				track[channel].add(new MidiEvent(new ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, program, 0), 0));

				programs[channel] = program;
			} catch (Exception e) { Mobile.log(PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Failed to set program: " + e.getMessage()); } */
		}

		public void shortMidiEvent(int type, int data1, int data2) 
		{  
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: shortMidiEvent()");

			if(type == 0xF0 || type == 0xF7 || data1 < 0 || data1 > 127 || data2 < 0 || data2 > 127) { throw new IllegalArgumentException("MmidiControl: Invalid arguments for shortMidiEvent()"); }

			Track[] tracks = player.midiSequence.getTracks();

			/* 
			 * This is probably incorrect, but send the shortEvent to the first track of the Sequence,
			 * as i don't think we can send that to the MIDI device itself.
			 */
			try { tracks[0].add(new MidiEvent(new ShortMessage(type, data1, data2), 0)); } 
			catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Failed to send short MIDI event: " + e.getMessage()); }
		}
	}

	private class volumeControl implements javax.microedition.media.control.VolumeControl
	{
		private int level = 100;
		private boolean muted = false;
		private audioplayer player; // Reference to the player this is linked to, or else we won't be able to apply changes

		public volumeControl(audioplayer player) { this.player = player; }

		public int getLevel() 
		{
			/* 
			 * We could return -1 here if the player is in REALIZED state, but that would require
			 * having access to platformPlayer itself, and this seems a bit optional... let's wait
			 * and see if any jar uses this for flow control of any kind.
			 */
			return level; 
		}

		public int setLevel(int level) 
		{
			/* 
			 * EXPERIMENTAL: Not sure if this is the correct approach, but: 
			 * 
			 * Only save the new level value if the stream isn't muted.
			 * 
			 * My logic here is that if this check isn't in place, we would
			 * hit cases where a jar goes into mute (sets level to 0 because of that)
			 * then goes out of mute but is still silent since setMute() will call this
			 * method with 0 as its level, which was the one that was saved.
			*/
			if(!isMuted()) 
			{
				/* Some Digital Chocolate games actually go all the way to level = 120. E.g. Tornado Mania */
				if(level > 100) { this.level = 100; }
				else if(level < 0) { this.level = 0; }
				else { this.level = level; }
			}

			/* 
			 * Checking if the current level is the same as the one received isn't particularly useful for us,
			 * as no exceptions will be thrown, and it's not like we're hurting for cpu cycles here to benefit
			 * from only making effective changes if needed.
			 */

			if(player instanceof midiPlayer) 
			{
				
				midiPlayer sequencer = (midiPlayer) player;
				int midiVolume = isMuted() ? 0: (int) (this.level * 127 / 100);

				/* Set volume for all channels, through Control Change command 7 */
				for (Track track : sequencer.midiSequence.getTracks())
				{
					for (int i = 0; i < 16; i++)
						{
							/* 
							 * This will make it so that volume changes take effect at the start of the midi stream.
							 * As far as i could test, all jars that have volume adjustment seem to start their streams
							 * from the very beginning after the volume is changed, so this is probably safe.
							 */
							try {track.add(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, i, 7, midiVolume), 0)); }
							catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Midi setLevel failed: " + e.getMessage());}
						}
				}
			}
			else if(player instanceof wavPlayer) /* Haven't found a jar that actually makes use of this yet - Scratch that: Shadow Shoot again */
			{
				wavPlayer wav = (wavPlayer) player;

				/* We have to map 0 <= value <= 100 to a clip's range of -80dB to 0dB  */
				float dB = isMuted() ? 0 : -80.0f + ((this.level / 100.0f) * (80.0f));

				FloatControl volumeControl = (FloatControl) wav.wavClip.getControl(FloatControl.Type.MASTER_GAIN);
				volumeControl.setValue(dB);
			}
			else if(player instanceof MP3Player) { ((MP3Player)player).mp3Player.setLevel(level); }

			notifyListeners(PlayerListener.VOLUME_CHANGED, this);
			
			if(isMuted()) { return 0; }

			return this.level; 
		}

		public void setMute(boolean mute) 
		{
			if(mute != isMuted()) 
			{
				muted = mute;

				if(muted) { setLevel(0); }
				else { setLevel(level); }
			}
		}

		public boolean isMuted() { return muted; }
	}

	/* This one hasn't been tested yet, no jar was found at the time it was implemented */
	private class tempoControl implements javax.microedition.media.control.TempoControl
	{
		/* MAX_RATE and MIN_RATE follow the JSR-135 docs guaranteed values, no matter if our player has a wider range */
		private final int MAX_RATE = 300000;
		private final int MIN_RATE = 10000;

		private int tempo = 120000; // Default tempo of 120 BPM in millitempo
		private int rate = 100000; // Default Rate in RateControl

		private midiPlayer player;

		public tempoControl(midiPlayer player) { this.player = player; }

		/* 
		 * According to the docs, getTempo():
		 * 
		 * Gets the current playback tempo. This represents the current state of the sequencer:
		 *	1 - A sequencer may not be initialized before the Player is prefetched. An uninitialized sequencer in this case returns a default tempo of 120 beats per minute.
		 *	2 - After prefetching has finished, the tempo is set to the start tempo of the MIDI sequence (if any).
		 *	3 - During playback, the return value is the current tempo and varies with tempo events in the MIDI file
		 *	4 - A stopped sequence retains the last tempo it had before it was stopped.
		 *	5 - A call to setTempo() changes current tempo until a tempo event in the MIDI file is encountered.
		 *
		 * Of course, only the very basic, case 3 is considered. If needed, and a jar is found to need it, 
		 * we can implement the other cases for better player state handling.
		 */

		public int getTempo() { Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "TempoControl: getTempo()"); return tempo; }

		public int setTempo(int millitempo) 
		{ 
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "TempoControl: setTempo()");
			tempo = millitempo; 
			
			/* 
			 * Here the docs say that the midi should START at this tempo, which
			 * indicates that we probably should add a midiEvent message to its
			 * tracks in order to change their tempo at tick 0, but first we need
			 * to find a jar that uses this, otherwise it's a shot in the dark.
			*/
			player.midi.setTempoInBPM(getEffectiveBPM());

			return tempo; 
		}

		// RateControl interface
		public int getMaxRate() { Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "TempoControl: getMaxRate()"); return MAX_RATE; }

		public int getMinRate() { Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "TempoControl: getMinRate()"); return MIN_RATE; }

		public int getRate() { Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "TempoControl: getRate()"); return rate; }

		public int setRate(int millirate) 
		{ 
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "TempoControl: setRate()");
			rate = millirate; 
			
			/* 
			 * In order to use setTempoFactor to adjust midiplayer's rate,
			 * we need to convert the rate value we currently have (expressed as
			 * integer) to the float range that setTempoFactor accepts.
			 * 
			 * In short, 100000 here means 100% playback rate, while 1.0f means
			 * the same in setTempoFactor, hence the division below.
			 */
			float factor = rate / 100000.0f;

			player.midi.setTempoFactor(factor);

			return rate; 
		}

		/* Used for setTempo, otherwise we won't know which tempo to actually set */
		public float getEffectiveBPM() { return (float) ( (getTempo() * getRate() / 1000.0f) / 100000.0f); }
	}

	/* ToneControl is also totally untested right now, couldn't find a jar for it */
	private class toneControl implements javax.microedition.media.control.ToneControl
	{
		private midiPlayer player;

		public toneControl(midiPlayer player) { this.player = player; }

		public void setSequence(byte[] sequence) 
		{
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "ToneControl: setSequence()"); /* This should show up in the case a jar tries to use it... just so we can find a jar that can test this */
			try 
			{
				if(sequence == null) { throw new IllegalArgumentException("ToneControl: cannot set a null sequence"); }
				/* TODO: We should check if the player state is PREFETCHED or STARTED here, and throw an IllegalStateException if so. Might not be needed. */

				Sequence toneSequence = new Sequence(Sequence.PPQ, 24);
				Track track = toneSequence.createTrack();

				setupSequence(sequence, track);

				player.midi.setSequence(toneSequence);
			} catch (InvalidMidiDataException e) {Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "ToneControl: Can't parse tone sequence: " + e.getMessage());}
		}

		private void setupSequence(byte[] sequence, Track track) 
		{
			if (sequence.length == 0 || sequence[0] != 1) { throw new IllegalArgumentException("ToneControl: Invalid sequence"); }

			/* We start a MIDI sequence setup like this: */
			int index = 1; // the very first index is just the VERSION number, so skip it
			int tempo = 120; // Default MIDI tempo in BPM
			int currentTick = 0; // This is used to keep track of the current tick, used by SetVolume and Tempo events
			int noteVolume = 127; // Start sending notes at the max volume by default

			while (index < sequence.length) 
			{
				byte eventType = sequence[index++];
				if (eventType >= -1 && eventType <= 127) // We found a note (or SILENCE, -1)
				{
					byte note = sequence[index++];
					byte duration = sequence[index++];
					try { addNote(track, note, duration, noteVolume, currentTick); currentTick += duration; }
					catch (InvalidMidiDataException e) {Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "ToneControl: Invalid note: " + e.getMessage());}
					
				}
				else if (eventType == ToneControl.REPEAT) 
				{
					byte numRepeats = sequence[index++];
					byte noteToRepeat = sequence[index++];
					byte repeatNoteDuration = sequence[index++];
					for (int i = 0; i < numRepeats; i++) 
					{ 
						try {addNote(track, noteToRepeat, repeatNoteDuration, noteVolume, currentTick); }
						catch (InvalidMidiDataException e) {Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "ToneControl: Invalid repeated note: " + e.getMessage());}
						currentTick += repeatNoteDuration;
					}
				}
				else if (eventType == ToneControl.SET_VOLUME) 
				{
					noteVolume = sequence[index++];
					try { track.add(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 7, noteVolume), currentTick)); }
					catch (InvalidMidiDataException e) {Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "ToneControl: Invalid SET_VOLUME event: " + e.getMessage());}
				}
				else if(eventType == ToneControl.TEMPO) 
				{
					tempo = sequence[index++];
					int microsecondsPerBeat = 60000000 / (tempo * 4);
					try 
					{
						track.add(new MidiEvent(new MetaMessage(0x51, new byte[]
						{
							(byte)(microsecondsPerBeat >> 16),
							(byte)(microsecondsPerBeat >> 8),
							(byte)(microsecondsPerBeat)
						}, 3), currentTick));
					}
					catch (InvalidMidiDataException e) {Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "ToneControl: Invalid TEMPO event: " + e.getMessage());}
				}
				else if(eventType == ToneControl.RESOLUTION || 
						eventType == ToneControl.BLOCK_START ||
						eventType == ToneControl.BLOCK_END) { /* These events don't need to be added to the midi sequence */ } 
				else { throw new IllegalArgumentException("Unknown event type."); }
			}
		}
		
		private void addNote(Track track, byte note, byte duration, int volume, int tick) throws InvalidMidiDataException 
		{
			int midiNote = note + 60; // Convert to MIDI note (C4 = MIDI 60)
			int noteDuration = duration; // Use duration directly as tick increment
		
			// Note on event with velocity set to currentVolume
			ShortMessage noteOn = new ShortMessage(ShortMessage.NOTE_ON, 0, midiNote, volume);
			track.add(new MidiEvent(noteOn, tick)); // Start immediately
		
			// Note off event
			ShortMessage noteOff = new ShortMessage(ShortMessage.NOTE_OFF, 0, midiNote, tick + noteDuration);
			track.add(new MidiEvent(noteOff, tick + noteDuration)); // End after the duration
		}
	}
}
