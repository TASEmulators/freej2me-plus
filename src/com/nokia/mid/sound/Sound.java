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
package com.nokia.mid.sound;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.ToneControl;
import javax.sound.midi.MidiUnavailableException;

import javax.microedition.media.decoders.NokiaOTTDecoder;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformPlayer;

/* Using references from http://www.j2megame.org/j2meapi/Nokia_UI_API_1_1/com/nokia/mid/sound/Sound.html */
public class Sound
{
	public static final int FORMAT_TONE = 1;
	public static final int FORMAT_WAV = 5;
	public static final int SOUND_PLAYING = 0;
	public static final int SOUND_STOPPED = 1;
	public static final int SOUND_UNINITIALIZED = 3;

	public static final byte TONE_MAX_VOLUME = 127;

	/*
	 * There's a freq table in: https://github.com/SymbianSource/oss.FCL.sf.app.JRT/blob/0822c2dcfb807a245ec84ab06006b59df7aedab6/javauis/nokiasound/javasrc/com/nokia/mid/sound/Sound.java
	 * 
	 * But using this single tone frequency multiplier has the same end result when converting, 
	 * and is far easier to understand throughout the code.
	 * It's also provided by the J2ME Docs: https://docs.oracle.com/javame/config/cldc/ref-impl/midp2.0/jsr118/javax/microedition/media/control/ToneControl.html
	 */
	private static final double SEMITONE_CONST = 17.31234049066755; // 1/(ln(2^(1/12)))

	private static ArrayList<Sound> players = new ArrayList<Sound>();

	private Player player;

	private static boolean isPrevPlayerTone = false;

	private int gain = 255;

	private SoundListener listener;

	public Sound(byte[] data, int type) { init(data, type); }
	
	public Sound(int freq, long duration) { init(freq, duration); }

	public static int getConcurrentSoundCount(int type) { return 1; }

	public int getState() 
	{
		if(player == null) { return SOUND_UNINITIALIZED; }
		
		int state = player.getState();

		switch (state)
		{
			case Player.STARTED:
				return SOUND_PLAYING;
			case Player.PREFETCHED:
			case Player.REALIZED:
				return SOUND_STOPPED;
			case Player.UNREALIZED:
			case Player.CLOSED:
			default:
				return SOUND_UNINITIALIZED;
		}
	}

	public static int[] getSupportedFormats() { return new int[]{FORMAT_TONE, FORMAT_WAV}; }

	public void init(byte[] data, int type) 
	{
		if(type != FORMAT_TONE && type != FORMAT_WAV) { throw new IllegalArgumentException("Cannot init player with unsupported format"); }
		if(data == null) { throw new NullPointerException("Cannot init player with null data"); }

		try 
		{
			if (type == FORMAT_TONE) 
			{
				try 
				{
					if(Mobile.dumpAudioStreams) { Manager.dumpAudioStream(new ByteArrayInputStream(data), "audio/x-tone-seq"); } // Dump original OTA as well
					if(player == null || !isPrevPlayerTone)  // check for null because release() can be called after all.
					{
						if(player != null) { release(); }
						player = Manager.createPlayer(new ByteArrayInputStream(NokiaOTTDecoder.convertToMidi(data)), "audio/x-tone-seq"); // This will dump the converted file if the setting is enabled
						isPrevPlayerTone = true;
					}
					else
					{
						player.stop();
						player.deallocate();
						((ToneControl) player.getControl("ToneControl")).setSequence(NokiaOTTDecoder.convertToMidi(data));
						if(Mobile.dumpAudioStreams) { Manager.dumpAudioStream(new ByteArrayInputStream(NokiaOTTDecoder.convertToMidi(data)), "audio/x-tone-seq"); } // Here we have to dump the stream manually, as setSequence is a fast way to swap short tone sequences
					}
					player.prefetch();
					if(!players.contains(this)) { players.add(this); }
				}
				catch (MidiUnavailableException e) { Mobile.log(Mobile.LOG_ERROR, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + " couldn't create Tone player:" + e.getMessage()); }
			}
			else if (type == FORMAT_WAV) 
			{
				if (player != null) { release(); }
				String format;
				if(data[0] == 'M' && data[1] == 'T' && data[2] == 'h' && data[3] == 'd') { format = "audio/mid"; }
				else if(data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F') { format = "audio/wav"; }
				else // One of the versions of PAC-MAN for Nokia S40v1 actually does this, seems like it's always OTT in those cases
				{ 
					Mobile.log(Mobile.LOG_WARNING, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + " couldn't find what format this is. Passing as FORMAT_TONE."); 
					try 
					{
						data = NokiaOTTDecoder.convertToMidi(data);
					}
					catch(MidiUnavailableException e) { Mobile.log(Mobile.LOG_ERROR, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + " Failed to convert unknown data to MIDI. Maybe isn't OTT:" + e.getMessage()); }
					format = "audio/x-tone-seq";
				}

				player = Manager.createPlayer(new ByteArrayInputStream(data), format);
				player.prefetch();
				isPrevPlayerTone = false;
				if(!players.contains(this)) { players.add(this); }
			}
			else { throw new IllegalArgumentException("Nokia Sound: Invalid audio format: " + type); }
		}
		catch (MediaException exception) { } catch (IOException exception) { }
	}

	public void init(int freq, long duration) 
	{
		if(duration <= 0 || convertFreqToNote(freq) > 127 || convertFreqToNote(freq) < 0) { throw new IllegalArgumentException("Cannot init tone with invalid parameters"); }
		
		Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Nokia Sound: Single Note:" + freq + " for:" + duration);

		try 
		{ 
			release();
			Manager.playTone(convertFreqToNote(freq), (int) duration, TONE_MAX_VOLUME); 
		}
		catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Nokia Sound: Could not play tone:" + e.getMessage()); }
	}

	public void play(int loop) 
	{
		if(player == null || getState() == SOUND_UNINITIALIZED) { return; }
		if(getState() == SOUND_PLAYING) { player.stop(); }
		if(loop < 0) { throw new IllegalArgumentException("Cannot play media, invalid loop value received"); }
		else if(loop == 0) { loop = -1; }

		// We only support one player running at a time here, so stop any currently running ones before starting
		for(int i = 0; i < players.size(); i++) 
		{
			if(players.get(i).getState() == SOUND_PLAYING) { players.get(i).stop(); }
		}
		
		if(((PlatformPlayer)player).nokiaListener != listener) { ((PlatformPlayer) player).setSoundListener(this, listener); }
		((PlatformPlayer.volumeControl)player.getControl("VolumeControl")).setLevel((int) (gain / 255f * 100f));
		player.setLoopCount(loop);
		player.setMediaTime(0); // A play call always makes the media play from the beginning.
		player.start();
	}

	// Nokia UI API 1.1 states that this should be a deallocate() call, but since we always recreate the player on init, we can use close()
	public void release() 
	{ 
		if(player != null) 
		{
			player.close();
			player = null;
			players.remove(this);
		}
	}

	public void resume() 
	{
		if(player == null || getState() == SOUND_UNINITIALIZED || getState() == SOUND_PLAYING) { return; }

		if(((PlatformPlayer)player).nokiaListener != listener) { ((PlatformPlayer) player).setSoundListener(this, listener); }
		((PlatformPlayer.volumeControl)player.getControl("VolumeControl")).setLevel((int) (gain / 255f * 100f));
		player.start(); 
	}

	public void setGain(int gain) 
	{ 
		// Gain goes from 0 to 255, while setLevel works from 0 to 100
		this.gain = gain;
	}

	public int getGain() 
	{ 
		return this.gain;
	}

	public void setSoundListener(SoundListener soundListener) { this.listener = soundListener; }

	public void stop() 
	{ 
		if(player == null || getState() == Sound.SOUND_STOPPED || getState() == Sound.SOUND_UNINITIALIZED) { return; }

		if(((PlatformPlayer)player).nokiaListener != listener) { ((PlatformPlayer) player).setSoundListener(this, listener); }
		player.stop(); 
	}

	// This is the same conversion used in Sprintpcs' DualTone implementation., as it also uses this constant.
	public static int convertFreqToNote(int freq) { return (int) Math.max(Math.round(Math.log((double) freq / 8.176) * SEMITONE_CONST), 0); }
}
