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
import java.util.ArrayList;
import java.util.Vector;

import javax.sound.midi.Instrument;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;

import com.nokia.mid.sound.Sound;
import com.nokia.mid.sound.SoundListener;

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

	protected Vector<PlayerListener> listeners;

	private SoundListener nokiaListener;
	private Sound nokiaSound;

	protected Control[] controls;

	// Manager already sets these two
	public static Synthesizer synthesizer;
	public static Receiver receiver;

	public PlatformPlayer(InputStream stream, String type)
	{
		listeners = new Vector<PlayerListener>();
		controls = new Control[NUM_CONTROLS];

		contentType = type;

		if(Mobile.sound == false) { player = new audioplayer(); }
		else
		{
			// Midi player will also play tones, as these are converted to midi in pretty much all cases at the moment
			if(contentType.toLowerCase().contains("mid") || contentType.toLowerCase().contains("tone")) { player = new midiPlayer(stream); }
			else if(contentType.toLowerCase().contains("wav")) { player = new wavPlayer(stream); }
			else if(contentType.toLowerCase().contains("mp"))  { player = new MP3Player(stream); } // MP1, MP2, MP3, MPEG, etc. No other J2ME format has those two letters in sequence.
			else /* If the stream doesn't have an accompanying type or its a type we don't have an explicit player for, do everything we can to try and load it */
			{
				Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Audio type <'" + contentType + "'> doesn't match any supported ones. Trying to find what it is...");
				try 
				{
					final byte[] data = new byte[stream.available()];
					stream.read(data, 0, stream.available());

					if(data.length >= 4 && data[0] == 'M' && data[1] == 'T' && data[2] == 'h' && data[3] == 'd') 
					{
						Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Format is MIDI!");
						player = new midiPlayer(new ByteArrayInputStream(data));
						contentType = "audio/mid";
					}
					else if (data.length >= 15 && data[8] == 'Q' && data[9] == 'L' && data[10] == 'C' && data[11] == 'M' && data[12] == 'f' && data[13] == 'm' && data[14] == 't') 
					{
						// This is for Qualcomm's QCP format, it has to be checked before wav, because Qualcomm's PureVoice also has RIFF as its first bytes
						Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Format is Qualcomm PureVoice! (not supported yet)");
						player = new audioplayer();
						contentType = "audio/qcp";
					}
					else if(data.length >= 4 && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F') 
					{
						Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Format is WAV!");
						player = new wavPlayer(new ByteArrayInputStream(data));
						contentType = "audio/wav";
					}
					else if(data.length >= 3 && data[0] == 'I' && data[1] == 'D' && data[2] == '3' || ((data[0] == (byte) 0xFF) && (data[1] & 0xE0) == 0xE0)) // Check for MPEG files WITH and WITHOUT the ID3 tag
					{
						Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Format is MPEG!");
						player = new MP3Player(new ByteArrayInputStream(data));
						contentType = "audio/mpeg";
					}
					else if(data.length >= 4 && data[0] == 'M' && data[1] == 'M' && data[2] == 'M' && data[3] == 'D')
					{
						Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Format is SMAF/MMF! (not supported yet)");
						player = new audioplayer();
						contentType = "audio/mmf";
					}
					else if(data.length >= 4 && data[0] == 'm' && data[1] == 'e' && data[2] == 'l' && data[3] == 'o')
					{
						Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Format is MFi! (not supported yet)");
						player = new audioplayer();
						contentType = "audio/mfi";
					}
					else if(data.length >= 6 && data[0] == '#' && data[1] == '!' && data[2] == 'A' && data[3] == 'M' && data[4] == 'R' && data[5] == '\n') 
					{
						Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Format is AMR-NB! (not supported yet)");
						player = new audioplayer();
						contentType = "audio/amr";
					} 
					else if(data.length >= 9 && data[0] == '#' && data[1] == '!' && data[2] == 'A' && data[3] == 'M' && data[4] == 'R' && data[5] == '-' && data[6] == 'W' && data[7] == 'B' && data[8] == '\n') 
					{
						Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Format is AMR-WB! (not supported yet)");
						player = new audioplayer();
						contentType = "audio/amr-wb";
					}
					else /* If none of the formats match, we don't know what this is */
					{
						Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "None of the known formats match the received stream, it won't play!");
						player = new audioplayer();
					}
				}
				catch (IOException e)
				{
					Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Couldn't parse input stream: " + e.getMessage());
				}
			}
		}

		// Set up control interfaces based on player type.
		controls[0] = new volumeControl(this.player); // Midi Player with Tones might not use this

		/* Midi Player has a few additional controls */
		if(player instanceof midiPlayer) 
		{
			/* If we're using midiPlayer to play tones, only set it up with ToneControl. */
			if(contentType.equalsIgnoreCase("audio/x-tone-seq")) { controls[3] = new toneControl((midiPlayer) this.player); }
			else
			{
				controls[1] = new tempoControl((midiPlayer) this.player);
				controls[2] = new midiControl((midiPlayer) this.player);
			}
		}

		Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "media type: " + contentType);
	}

	public PlatformPlayer(String locator)
	{
		if(locator.equals(Manager.TONE_DEVICE_LOCATOR) || locator.equals(Manager.MIDI_DEVICE_LOCATOR)) 
		{
			Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + " Creating MIDI Player for locator: "+locator);
			player = new midiPlayer();
			listeners = new Vector<PlayerListener>();
			controls = new Control[NUM_CONTROLS];
			controls[0] = new volumeControl(this.player); // Midi Player with Tones might not use this
			controls[3] = new toneControl((midiPlayer) this.player);
		} 
		else 
		{
			Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "No player for locator: "+locator);
			player = new audioplayer();
			listeners = new Vector<PlayerListener>();
			controls = new Control[3];
		}
	}

	public void close()
	{
		if(getState() == Player.CLOSED) { return; }

		try
		{
			if(player.isRunning()) { stop(); }
			player.close();
			controls = null;
			player = null;
			state = Player.CLOSED;
			notifyListeners(PlayerListener.CLOSED, null);	
		}
		catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Could not close player: " + e.getMessage()); }
	}

	public int getState() { return state; }

	public void start()
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot call start() on a CLOSED player."); }
		
		if(getState() == Player.UNREALIZED) { realize(); }

		if(getState() == Player.REALIZED) { prefetch(); }

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

	public void setSoundListener(Sound sound, SoundListener listener) // Nokia Sound only has methods to set the Sound Listener, not remove it
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot add SoundListener to an UNINITIALIZED Sound"); }
		if(listener == null) { return; }

		nokiaListener = listener;
		nokiaSound = sound;
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

		if(nokiaListener != null) 
		{
			if(event == PlayerListener.CLOSED) { nokiaListener.soundStateChanged(nokiaSound, Sound.SOUND_UNINITIALIZED); }
			else if(event == PlayerListener.STARTED) { { nokiaListener.soundStateChanged(nokiaSound, Sound.SOUND_PLAYING); } }
			else if(event == PlayerListener.STOPPED || event == PlayerListener.END_OF_MEDIA) { { nokiaListener.soundStateChanged(nokiaSound, Sound.SOUND_STOPPED); } }
		}
	}

	public void deallocate()
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot deallocate player, it is already CLOSED."); }

		if(player.isRunning()) { stop(); }
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
		
		/* 
		 * If the player isn't at least prefetched, there's no way to get media time.
		 * PlatformPlayer does in fact acquire everything needed to play the media on realize(),
		 * however, J2ME docs state that the exclusive and scarce resources (such as an actual
		 * player resources) should only be acquired in prefetch. So let's assume we're working this
		 * way here for now.
		 */
		if(getState() == Player.UNREALIZED || getState() == Player.REALIZED) { return Player.TIME_UNKNOWN; }

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
		if(count == 0) {throw new IllegalStateException("Jar tried to set loop count as 0.");}
		if(getState() == Player.CLOSED || getState() == Player.STARTED) { throw new IllegalStateException("Jar tried to set loop count on a player in an invalid state."); }

		player.setLoopCount(count); 
	}

	public long setMediaTime(long now) 
	{
		if(getState() == Player.UNREALIZED || getState() == Player.CLOSED) { throw new IllegalStateException("Cannot set Media Time. Player is either UNREALIZED or CLOSED."); }

		return player.setMediaTime(now);
	}

	// Controllable interface //

	public Control getControl(String controlType)
	{
		if(getState() == Player.CLOSED || getState() == Player.UNREALIZED) { throw new IllegalStateException("Cannot call getControl(), as the player is either CLOSED or UNREALIZED."); }

		if(controlType.contains("VolumeControl")) { return controls[0]; }
		if(controlType.contains("TempoControl"))  { return controls[1]; }
		if(controlType.contains("MIDIControl"))   { return controls[2]; }
		if(controlType.contains("ToneControl"))   { return controls[3]; }
		
		return null;
	}

	public Control[] getControls() 
	{ 
		if(getState() == Player.CLOSED || getState() == Player.UNREALIZED) { throw new IllegalStateException("Cannot call getControls(), as the player is either CLOSED or UNREALIZED."); }

		return controls; 
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
		public void close() { }
		public void realize() { }
		public void prefetch() { }
		public long getDuration() { return Player.TIME_UNKNOWN; }
	}

	private class midiPlayer extends audioplayer
	{
		private Sequencer midi;
		private Sequence midiSequence;
		private int numLoops = 0;
		private MetaEventListener metaListener = null;

		public midiPlayer() // For when a Locator call (usually for tones) is issued
		{
			Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Midi Player [locator] untested");

			// Create an empty sequence, which should be overriden with whatever setSequence() receives.
			try 
			{ 
				midi = MidiSystem.getSequencer(false);
				midiSequence = new Sequence(Sequence.PPQ, 24); 
			} 
			catch (Exception e) {  Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Couldn't load midi file:" + e.getMessage()); }
		}

		public midiPlayer(InputStream stream) 
		{
			try 
			{ 
				midi = MidiSystem.getSequencer(false);
				midiSequence = MidiSystem.getSequence(stream); 
			} 
			catch (Exception e) 
			{
				Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Couldn't load MIDI file: " + e.getMessage());
			}
		}

		public void realize() 
		{ 
			try 
			{
				midi = MidiSystem.getSequencer(false);
				midi.getTransmitter().setReceiver(PlatformPlayer.receiver);
				midi.open();
				midi.setSequence(midiSequence);

				/* 
				 * We have to listen for END_OF_MEDIA events, or else jars that rely on this
				 * won't work in the expected way.
				 */
				if (metaListener == null) 
				{
					metaListener = new MetaEventListener() 
					{
						@Override
						public void meta(MetaMessage meta) 
						{
							if (meta.getType() == 0x2F) // 0x2F = END_OF_MEDIA in Sequencer
							{
								state = Player.PREFETCHED;
								notifyListeners(PlayerListener.END_OF_MEDIA, getMediaTime());
								if(numLoops != 0) 
								{
									if(numLoops > 0) { numLoops--; } // If numLoops = -1, we're looping indefinitely
									setMediaTime(0);
									start();
								}
							}
						}
					};
					midi.addMetaEventListener(metaListener);
				}

				state = Player.REALIZED;
			}
			catch (Exception e) 
			{
				Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Could not realize midi stream:" + e.getMessage());
				state = Player.UNREALIZED; 
			}
		}

		public void prefetch() { state = Player.PREFETCHED; }

		public void start()
		{
			try 
			{
				// Reload the sequence into the sequencer to prevent MIDI property carryovers
				final long time = getMediaTime(); // We'll reload the sequence, so save it's current position to restore later.
				midi.setSequence(midiSequence);
				if(time >= getDuration()) { setMediaTime(0); } // If mediaTime >= getDuration, we should start playing from the beginning
				else { setMediaTime(time); } // Else, resume from where it stopped

				midi.start();
				state = Player.STARTED;
				notifyListeners(PlayerListener.STARTED, getMediaTime());
			}
			catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Failed to clean MIDI sequencer and start playback:" + e.getMessage()); }
		}

		public void stop()
		{
			midi.stop();
			state = Player.PREFETCHED;
			notifyListeners(PlayerListener.STOPPED, getMediaTime());
		}

		public void deallocate() 
		{ 
			if(metaListener != null) 
			{
				midi.removeMetaEventListener(metaListener);
				metaListener = null;
			}
			midi.close();
		}

		public void close() 
		{
			if (metaListener != null) 
			{
				midi.removeMetaEventListener(metaListener);
				metaListener = null;
			}
			midi.close();
			midiSequence = null;
		}

		public void setLoopCount(int count)
		{
			/* 
			 * Treat cases where an app wants this stream to loop continuously.
			 * Here, count = 1 means it should loop one time, whereas in j2me
			 * it appears that count = 1 means no loop at all, at least based
			 * on Gameloft games that set effects and some music with count = 1
			 */
			if(count == Clip.LOOP_CONTINUOUSLY) { numLoops = count; }
			else { numLoops = count-1; }
		}

		public long setMediaTime(long now)
		{
			try 
			{
				if(now >= getDuration()) { midi.setMicrosecondPosition(getDuration()); }
				else if(now < 0) { midi.setMicrosecondPosition(0); }
				else { midi.setMicrosecondPosition(now);  }
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

		public Sequence getSequence() { return midiSequence; }

		public void setSequence(InputStream sequence) 
		{ 
			try { midiSequence = MidiSystem.getSequence(sequence); }
			catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Failed to set MIDI sequence:" + e.getMessage());  }
		}

		public Sequencer getSequencer() { return midi; }
	}

	private class wavPlayer extends audioplayer
	{
		/* PCM WAV variables */
		private byte[] tmpStream;
		private AudioInputStream wavStream;
		private Clip wavClip;
		private int[] wavHeaderData = new int[4];
		private int numLoops = 0;
		private LineListener lineListener = null;

		public wavPlayer(InputStream stream)
		{
			/*
			 * A wav header is generally 44-bytes long (up to 60 for IMA ADPCM), and it is what we need to read in order 
			 * to get the stream's format, frame size, bit rate, number of channels, etc. which gives us information
			 * on the kind of codec needed to play or decode the incoming stream. The stream needs to be reset
			 * or else PCM files will be loaded without a header and it might cause issues with playback.
			 */
			try 
			{
				stream.mark(60);
				wavHeaderData = WavImaAdpcmDecoder.readHeader(stream);
				stream.reset();

				if(wavHeaderData[0] != 17) /* If it's not IMA ADPCM we don't need to do anything to the stream. Just pass it to a local copy */
				{
					tmpStream = new byte[stream.available()];
					stream.read(tmpStream, 0, stream.available());
				}
				else /* But if it is IMA ADPCM, we have to decode it manually. */
				{
					tmpStream = WavImaAdpcmDecoder.decodeImaAdpcm(stream, wavHeaderData);

					if(Mobile.minLogLevel == Mobile.LOG_DEBUG) /* Print the decoded stream's header for analysis */
					{
						InputStream headerRead = new ByteArrayInputStream(tmpStream);
						WavImaAdpcmDecoder.readHeader(headerRead);
						headerRead = null;
					}
				}
			} catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Could not prepare wav stream:" + e.getMessage());}
		}

		public void realize() 
		{ 
			try
			{
				wavStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(tmpStream));
				wavClip = AudioSystem.getClip();
				wavClip.open(wavStream);
				/* Like for midi, we need to listen for END_OF_MEDIA events here too. */
				if (lineListener == null) 
				{
					lineListener = new LineListener() 
					{
						@Override
						public void update(LineEvent event) 
						{
							if (event.getType() == LineEvent.Type.STOP) 
							{
								state = Player.PREFETCHED;
								notifyListeners(PlayerListener.END_OF_MEDIA, getMediaTime());
								if(numLoops != 0) 
								{
									if(numLoops > 0) { numLoops--; } // If numLoops = -1, we're looping indefinitely
									setMediaTime(0);
									start();
								}
							}
						}
					};
					wavClip.addLineListener(lineListener);
				}

				state = Player.REALIZED; 
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

			state = Player.STARTED;
			notifyListeners(PlayerListener.STARTED, getMediaTime());
		}

		public void stop()
		{
			wavClip.stop();
			state = Player.PREFETCHED;
			notifyListeners(PlayerListener.STOPPED, getMediaTime());
		}

		public void deallocate() 
		{
			if (lineListener != null) 
			{
				wavClip.removeLineListener(lineListener);
				lineListener = null;
			}
			wavClip.close(); 
		}

		public void close() 
		{
			if (lineListener != null) 
			{
				wavClip.removeLineListener(lineListener);
				lineListener = null;
			}
			wavClip.close();
			wavClip = null;
			wavStream = null;
			wavHeaderData = null;
		}

		public void setLoopCount(int count)
		{
			/* 
			 * Treat cases where an app wants this stream to loop continuously.
			 * Here, count = 1 means it should loop one time, whereas in j2me
			 * it appears that count = 1 means no loop at all, at least based
			 * on Gameloft games that set effects and some music with count = 1
			 */
			if(count == Clip.LOOP_CONTINUOUSLY) { numLoops = count; }
			else { numLoops = count-1; }
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
		private byte[] tmpStream;
		private MPEGPlayer mp3Player;
		private Thread playerThread = null;

		public MP3Player(InputStream stream)
		{
			try 
			{
				tmpStream = new byte[stream.available()];
				stream.read(tmpStream, 0, stream.available()); 
			}
			catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Could not prepare mpeg stream:" + e.getMessage());}
		}

		public void realize() 
		{ 
			try
			{
				mp3Player = new MPEGPlayer(new ByteArrayInputStream(tmpStream), false);
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
			/*
			 * The fact these null checks have to be littered on MP3Player gives me a bad feeling...
			 * Maybe it's because it doesn't at all work like wav and midi players, which are
			 * integrated into Java Sound and don't need a thread object to play non-blocking.
			 */
			if(mp3Player == null) { return; }

			try 
			{
				playerThread = new Thread(() -> 
				{
					try 
					{
						if(getMediaTime() >= getDuration()) { setMediaTime(0); }
						mp3Player.play(); // This is thread-blocking, so the code below only executes after this has finished.

						/* 
						 * Check if mp3Player is still valid and exit early, since this thread can be 
						 * interrupted and the player can also be closed abruptly. 
						 */ 
						if (mp3Player == null) { return; }

						if (!Thread.currentThread().isInterrupted()) 
						{
							state = Player.PREFETCHED;
							notifyListeners(PlayerListener.END_OF_MEDIA, getMediaTime());
							if(mp3Player.getLoopCount() != 0) 
							{
								if(mp3Player.getLoopCount() > 0) { mp3Player.decreaseLoopCount(); } // If getLoopCount() = -1, we're looping indefinitely
								mp3Player.reset();
								mp3Player.play();
							}
						}
					}
					catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Couldn't start mpeg player:" + e.getMessage()); }
				});

				playerThread.start();

				state = Player.STARTED;
				notifyListeners(PlayerListener.STARTED, getMediaTime());
			} catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Couldn't start mpeg player:" + e.getMessage()); }
		}

		public void stop()
		{
			if(mp3Player == null) { return; }
			mp3Player.stop();
			if (playerThread != null && playerThread.isAlive()) { playerThread.interrupt(); }

			state = Player.PREFETCHED;
			notifyListeners(PlayerListener.STOPPED, getMediaTime());
		}

		public void deallocate() { } // Prefetch does "nothing" in each internal player so deallocate must also do nothing

		public void close() 
		{
			mp3Player = null;
			tmpStream = null;
			playerThread = null;
		}

		public void setLoopCount(int count)
		{
			if(mp3Player == null) { return; }
			/* 
			 * Treat cases where an app wants this stream to loop continuously.
			 * Here, count = 1 means it should loop one time, whereas in j2me
			 * it appears that count = 1 means no loop at all, at least based
			 * on Gameloft games that set effects and some music with count = 1
			 */
			if(count == Clip.LOOP_CONTINUOUSLY) { mp3Player.setLoopCount(count); }
			else { mp3Player.setLoopCount(count-1); }
		}

		public long setMediaTime(long now)
		{
			if(mp3Player == null) { return 0; }

			if(now >= getDuration()) { mp3Player.setMicrosecondPosition(getDuration()); }
			else if(now < 0) { mp3Player.setMicrosecondPosition(0); }
			else { mp3Player.setMicrosecondPosition(now); }

			/* 
			 * In MP3Player's case, we don't deal with microsecond resolution, so return the new
			 * effective position converted to microseconds.
			 */
			return getMediaTime();
		}

		public long getMediaTime() 
		{ 
			if(mp3Player != null) { return mp3Player.getMicrosecondPosition(); } 
			return 0; 
		}

		public long getDuration() 
		{ 
			if(mp3Player != null) { return mp3Player.getDuration(); } 
			return Player.TIME_UNKNOWN; 
		}

		public boolean isRunning() 
		{ 
			if(mp3Player != null) { return mp3Player.isRunning(); } 
			return false; 
		}
	}

	// Controls //

	/* midiControl is untested */
	public class midiControl implements javax.microedition.media.control.MIDIControl
	{
		private midiPlayer player;

		/* 
		 * Java, by default, does not directly support any of the query methods
		 * below, which means we must implement them, and track the state of 
		 * everything that they change ourselves.
		 */
		private int[] channelVolume = new int[16]; // For getChannelVolume

		public midiControl(midiPlayer player) 
		{ 
			this.player = player;
			for(int channel = 0; channel < channelVolume.length; channel++) { channelVolume[channel] = 127; }
		}

		public int[] getBankList(boolean custom)
		{
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: getBankList() untested");

			if(custom) { Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: getBankList() with custom bank not implemented, returning all banks."); }
		
			// Use a list to collect bank numbers
			ArrayList<Integer> bankList = new ArrayList<>();
		
			try 
			{
				Patch[] patches = player.getSequence().getPatchList();
		
				// Use the current sequence as a source for the patches and its available banks
				for (int i = 0; i < patches.length; i++) { bankList.add(patches[i].getBank()); }
			} catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Error retrieving bank list: " + e.getMessage()); }
		
			// Convert the list to an int array to return
			return bankList.stream().mapToInt(Integer::intValue).toArray();
		}

		public int getChannelVolume(int channel) 
		{ 
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: getChannelVolume() untested");
			return channelVolume[channel];
		}

		public java.lang.String getKeyName(int bank, int prog, int key) 
		{ 
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: getKeyName() not implemented ");
			// And probably will never be, i don't think Java has any concept of this, all the way to Key-Mapped Banks
			return ""; 
		}

		public int[] getProgram(int channel) 
		{
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: getProgram() untested");
		
			final int[] program = new int[2];
		
			// This is VERY costly, and might not even be correct as it relies on getProgramList and getBankList, which themselves are untested.
			try
			{
				MidiChannel[] channels = PlatformPlayer.synthesizer.getChannels();

				if(channel < 0 || channel > channels.length) {throw new IllegalArgumentException("midiControl: Tried to call getProgram with invalid channel");}
		
				int currentProgram = channels[channel].getProgram(); // This returns a {bank, program} pair, so only channel.getProgram() is not enough.
				
				// We got the program mapped to that channel, now to find the corresponding bank for this program
				int[] banks = getBankList(false); // Retrieve the list of available banks
				
				for (int bank : banks) // Iterate through the banks to find the matching program, if there's any at all
				{
					int[] programList = getProgramList(bank); // Get list of programs for the current bank
					
					// Check if the current program exists in this bank
					for (int programNum : programList) 
					{
						if (programNum == currentProgram) // IF it does, we found the {bank,program} pair to be returned
						{ 
							program[0] = bank;
							program[1] = currentProgram;
						}
					}
				}
			} 
			catch (Exception e) 
			{
				Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Error retrieving program for channel: " + e.getMessage());
				return null;
			}
		
			return program;
		}

		public int[] getProgramList(int bank) {

			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: getProgramList()");
		
			ArrayList<Integer> programList = new ArrayList<>();
		
			try 
			{
				Patch[] patches = player.getSequence().getPatchList();
				
				// Iterate through the available patches and collect program numbers for the specified bank
				for (Patch patch : patches) 
				{
					if (patch.getBank() == bank) { programList.add(patch.getProgram()); } // Add the program number for the matching bank
				}
			} catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Error retrieving program list: " + e.getMessage()); }

			return programList.stream().mapToInt(Integer::intValue).toArray();
		}

		public String getProgramName(int bank, int prog)
		{
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: getProgramName()");
		
			// Java doesn't even have a concept of having names for programs, only instruments. So let's return the instrument's name instead.
			try 
			{
				Soundbank soundbank = PlatformPlayer.synthesizer.getDefaultSoundbank();
		
				Instrument[] instruments = soundbank.getInstruments();
				for (Instrument instrument : instruments)
				{
					Patch patch = instrument.getPatch();
					// If a matching bank and program number is found on the instrument's patch, return the name of the patch's instrument
					if (patch.getBank() == bank && patch.getProgram() == prog) { return instrument.getName(); }
				}
			} catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Error retrieving program name: " + e.getMessage()); }
		
			return ""; // Return an empty string if no match is found
		}

		public boolean isBankQuerySupported() 
		{ 
			Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "isBankQuerySupported() requested, returning unsupported.");
			return false; 
		}

		public int longMidiEvent(byte[] data, int offset, int length) {
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: longMidiEvent() untested");
		
			// Validate input parameters
			if (data == null || offset < 0 || length < 0 || offset + length > data.length) { throw new IllegalArgumentException("MidiControl: Invalid arguments for longMidiEvent()"); }
		
			try 
			{
				if (data[offset] == (byte) 0xF0 && data[offset + length - 1] == (byte) 0xF7) // Check if it is a SysEx message
				{
					// Create the SysEx message without the status byte
					byte[] sysExData = new byte[length - 2];
					System.arraycopy(data, offset + 1, sysExData, 0, length - 2); // Exclude the 0xF0 and 0xF7

					// Create the SysexMessage
					SysexMessage sysexMessage = new SysexMessage(0xF0, sysExData, sysExData.length);
					PlatformPlayer.receiver.send(sysexMessage, player.getMediaTime() + 50_000L); // Send the message
				}
				else // If it is not, send data as a series of short messages (probably implemented incorrectly, and being untested only makes things worse)
				{
					for (int i = offset; i < offset + length; i += 3) 
					{
						int msgLength = Math.min(3, length - (i - offset)); // Ensure we don't exceed the shortMessage's length
						ShortMessage shortMessage = new ShortMessage();

						if      (msgLength == 1) { shortMessage.setMessage(data[i] & 0xFF); }  // Send only status byte (is this even useful?)
						else if (msgLength == 2) { shortMessage.setMessage(data[i] & 0xFF, data[i + 1] & 0xFF, 0); } // Status byte + one data byte
						else if (msgLength == 3) { shortMessage.setMessage(data[i] & 0xFF, data[i + 1] & 0xFF, data[i + 2] & 0xFF); } // Full short message

						PlatformPlayer.receiver.send(shortMessage, player.getMediaTime() + 50_000L);
					}
				}
				return length; // Return the number of bytes sent
			} 
			catch (Exception e) 
			{
				Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Error sending long MIDI event: " + e.getMessage());
				return -1; // Return -1 if an error occurred
			}
		}

		public void setChannelVolume(int channel, int volume) 
		{
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: setChannelVolume() untested");

			try 
			{
				MidiChannel[] channels = PlatformPlayer.synthesizer.getChannels();

				if(channel < 0 || channel > channels.length || volume < 0 || volume > 127) {throw new IllegalArgumentException("midiControl: Tried to call setChannelVolume with invalid args");}

				// Set the volume on the MIDI channel
				channelVolume[channel] = volume; // For tracking purposes, whenever getChannelVolume is called.
				channels[channel].controlChange(7, volume);
			}
			catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Midi setChannelVolume failed: " + e.getMessage());}
		}

		public void setProgram(int channel, int bank, int program) 
		{  
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: setProgram() untested");
		
			// Validate input parameters
			if (channel < 0 || channel > 15) { throw new IllegalArgumentException("Channel must be between 0 and 15."); }
			if (program < 0 || program > 127) { throw new IllegalArgumentException("Program must be between 0 and 127."); }
			if (bank < -1 || bank > 16383) { throw new IllegalArgumentException("Bank must be between 0 and 16383, or -1 for default bank."); }
		
			// Send bank change
			shortMidiEvent(CONTROL_CHANGE | channel, CONTROL_BANK_CHANGE_MSB, bank >> 7); // Send MSB (Most Significant Byte)
			shortMidiEvent(CONTROL_CHANGE | channel, CONTROL_BANK_CHANGE_LSB, bank & 0x7F); // Send LSB (Least Significant Byte)
		
			// Send program change
			shortMidiEvent(PROGRAM_CHANGE | channel, program, 0);
		}

		public void shortMidiEvent(int type, int data1, int data2) 
		{  
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: shortMidiEvent() untested");

			if(type < 0x80 || type == 0xF0 || type == 0xF7 || data1 < 0 || data1 > 127 || data2 < 0 || data2 > 127) { throw new IllegalArgumentException("MmidiControl: Invalid arguments for shortMidiEvent()"); }

			try 
			{
				// Create a MIDI message from the type and data values received
				final byte[] message = new byte[3];
				message[0] = (byte) type;
				message[1] = (byte) data1;
				message[2] = (byte) data2;

				ShortMessage midiMessage = new ShortMessage();
				midiMessage.setMessage(type, data1, data2);
		
				// Send the MIDI message to the receiver
				PlatformPlayer.receiver.send(midiMessage, player.getMediaTime() + 50_000L); // Send message after 50ms
			}
			catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Failed to send short MIDI event: " + e.getMessage()); }
		}
	}

	public class volumeControl implements com.siemens.mp.media.control.VolumeControl // Siemens already extends javax here
	{
		private int level = 100;
		private boolean muted = false;
		private audioplayer player; // Reference to the player this is linked to, or else we won't be able to apply changes

		public volumeControl(audioplayer player) { this.player = player; }

		public int getLevel() 
		{
			if(getState() == Player.REALIZED) { return -1; }

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

			if (player instanceof midiPlayer) 
			{
				midiPlayer sequencer = (midiPlayer) player;
				int midiVolume = isMuted() ? 0 : (int) (this.level * 127 / 100); // Convert to MIDI volume range

				ShortMessage volumeMessage = new ShortMessage();			
				// Set volume for all channels through Control Change command 7 (volume)
				for (int channel = 0; channel < 16; channel++) 
				{
					try 
					{
						volumeMessage.setMessage(ShortMessage.CONTROL_CHANGE, channel, 7, midiVolume);
						// Apply volume change 50ms after the current playback time, to give sequencer some time to breathe.
						PlatformPlayer.receiver.send(volumeMessage, sequencer.getMediaTime() + 50_000L);
					} 
					catch (Exception e) 
					{
						Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Midi setLevel failed: " + e.getMessage());
					}
				}
			}
			else if(player instanceof wavPlayer) /* Haven't found a jar that actually makes use of this yet - Scratch that: Shadow Shoot again */
			{
				wavPlayer wav = (wavPlayer) player;

				/* We have to map 0 <= value <= 100 to a clip's range of -20dB to 0dB  */
				float dB = isMuted() ? -80.0f : -20.0f + ((this.level / 100.0f) * (20.0f));

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
	public class tempoControl implements javax.microedition.media.control.TempoControl
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

		public int getTempo() { Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "getTempo()"); return tempo; }

		public int setTempo(int millitempo) 
		{ 
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "setTempo()");
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
		public int getMaxRate() { Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "getMaxRate()"); return MAX_RATE; }

		public int getMinRate() { Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "getMinRate()"); return MIN_RATE; }

		public int getRate() { Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "getRate()"); return rate; }

		public int setRate(int millirate) 
		{ 
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "setRate()");
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

	/* ToneControl is also almost entirely untested right now, couldn't find a jar that uses setSequence() */
	public class toneControl implements com.siemens.mp.media.control.ToneControl // Siemens already extends javax here too
	{
		private midiPlayer player;

		public toneControl(midiPlayer player) { Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Tone Control"); this.player = player; }

		/* 
		 * As far as i can tell, Nokia's OTT/OTA Tones don't use this, which would leave only jars that directly use J2ME's Augmented BNF format, if there are any. 
		 * If such a case is found, setupSequence() should be the one to parse that format into a MIDI sequence. 
		 */
		public void setSequence(byte[] sequence) 
		{
			if(sequence == null) { throw new IllegalArgumentException("ToneControl: cannot set a null sequence"); }

			if(getState() == Player.PREFETCHED || getState() == Player.STARTED) { throw new IllegalStateException("Cannot call setSequence(), as the player is either PREFETCHED or STARTED."); }

			// If what we received is a tone sequence from nokia, siemens, sprint, etc. which is converted to midi beforehand, just send it to the player right away.
			if(sequence[0] == 'M' && sequence[1] == 'T' && sequence[2] == 'h' && sequence[3] == 'd') 
			{
				player.setSequence(new ByteArrayInputStream(sequence));
			}
			else // TODO: For J2ME's ToneControl Augmented BNF tone format, if anything out there even uses it.
			{
				/* This should show up in the case a jar tries to use it... just so we can find a jar that can test this */
				Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "setSequence() for A-BNF Tones not implemented");
				try 
				{
					Sequence toneSequence = new Sequence(Sequence.PPQ, 24);
					Track track = toneSequence.createTrack();

					setupSequence(sequence, track);

					//player.setSequence(toneSequence);
					//player.midi.setSequence(toneSequence);
				} catch (InvalidMidiDataException e) {Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Can't parse tone sequence: " + e.getMessage());}
			}
		}

		private void setupSequence(byte[] sequence, Track track) // This tries to parse the default
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
					catch (InvalidMidiDataException e) {Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Invalid note: " + e.getMessage());}
					
				}
				else if (eventType == ToneControl.REPEAT) 
				{
					byte numRepeats = sequence[index++];
					byte noteToRepeat = sequence[index++];
					byte repeatNoteDuration = sequence[index++];
					for (int i = 0; i < numRepeats; i++) 
					{ 
						try {addNote(track, noteToRepeat, repeatNoteDuration, noteVolume, currentTick); }
						catch (InvalidMidiDataException e) {Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Invalid repeated note: " + e.getMessage());}
						currentTick += repeatNoteDuration;
					}
				}
				else if (eventType == ToneControl.SET_VOLUME) 
				{
					noteVolume = sequence[index++];
					try { track.add(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 7, noteVolume), currentTick)); }
					catch (InvalidMidiDataException e) {Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Invalid SET_VOLUME event: " + e.getMessage());}
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
					catch (InvalidMidiDataException e) {Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Invalid TEMPO event: " + e.getMessage());}
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
