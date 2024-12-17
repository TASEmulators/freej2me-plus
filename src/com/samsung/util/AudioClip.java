
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
package com.samsung.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.VolumeControl;

import org.recompile.mobile.Mobile;

public class AudioClip
{
	public static final int TYPE_MMF = 1;
	public static final int TYPE_MP3 = 2;
	public static final int TYPE_MIDI = 3;

	public static final String[] formatMIMEType = {"audio/mmf", "audio/mp3", "audio/midi"};

	private Player player;

	public AudioClip(int clipType, byte[] audioData, int audioOffset, int audioLength)
	{
		if(audioData == null) { throw new NullPointerException("AudioClip: Cannot open player with null Audio data"); }
		if(clipType < 1 || clipType > 3) { throw new IllegalArgumentException("AudioClip: Clip type not recognized");}
		if (audioOffset < 0 || audioLength < 0 || audioOffset + audioLength > audioData.length) 
		{
			throw new ArrayIndexOutOfBoundsException("AudioClip: Cannot create player, tried to access audioData at an invalid position");
		}

		/* Some jars actually try to pass streams with a different clip type from what they should be, so check their header and ignore whatever the jar is passing here. */
		if(audioData[audioOffset+0] == 'M' && audioData[audioOffset+1] == 'M' && audioData[audioOffset+2] == 'M' && audioData[audioOffset+3] == 'D') { clipType = TYPE_MMF; }
		else if(audioData[audioOffset+0] == 'M' && audioData[audioOffset+1] == 'T' && audioData[audioOffset+2] == 'h' && audioData[audioOffset+3] == 'd') { clipType = TYPE_MIDI; }
		else if(audioData[audioOffset+0] == 'I' && audioData[audioOffset+1] == 'D' && audioData[audioOffset+2] == '3' || ((audioData[audioOffset+0] == (byte) 0xFF) && (audioData[audioOffset+1] & 0xE0) == 0xE0)) { clipType = TYPE_MP3; }

		Mobile.log(Mobile.LOG_WARNING, AudioClip.class.getPackage().getName() + "." + AudioClip.class.getSimpleName() + ": " + "Samsung AudioClip (ByteArray)");

		if(clipType == TYPE_MMF) 
		{
			try 
			{
				// TODO: Either convert MMF to MIDI (easier), or try to playback MMF directly (MUCH harder)

				//byte[] midiDat = convertMMFToMIDI(audioData);
				//audioData = midiDat;
				//clipType = TYPE_MIDI;
			} catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, AudioClip.class.getPackage().getName() + "." + AudioClip.class.getSimpleName() + ": " + "AudioClip: Could not convert MMF file to MIDI:" + e.getMessage());}
			
		}

		try 
		{ 
			player = Manager.createPlayer(new ByteArrayInputStream(audioData, audioOffset, audioLength), formatMIMEType[clipType-1]);
			player.prefetch();
		}
		catch (Exception e) {Mobile.log(Mobile.LOG_ERROR, AudioClip.class.getPackage().getName() + "." + AudioClip.class.getSimpleName() + ": " + "AudioClip: Failed to create player:" + e.getMessage()); }
	}

	public AudioClip(int clipType, String filename)
	{
		if(filename == null) { throw new NullPointerException("AudioClip: Cannot open a player with a null file path"); }
		if(clipType < 1 || clipType > 3) { throw new IllegalArgumentException("AudioClip: Clip type not recognized");}

		Mobile.log(Mobile.LOG_WARNING, AudioClip.class.getPackage().getName() + "." + AudioClip.class.getSimpleName() + ": " + "Samsung AudioClip(type, locator) untested");
		
		InputStream stream = Mobile.getPlatform().loader.getResourceAsStream(filename);

		try 
		{ 
			player = Manager.createPlayer(stream, formatMIMEType[clipType-1]);
			player.prefetch();
		}
		catch (Exception e) {Mobile.log(Mobile.LOG_ERROR, AudioClip.class.getPackage().getName() + "." + AudioClip.class.getSimpleName() + ": " + "AudioClip: Failed to create player:" + e.getMessage()); }
	}

	public static boolean isSupported() { return true; }

	public void pause() { player.stop(); }

	public void play(int loop, int volume) 
	{  
		if(loop < 0 || loop > 255 || volume < 0 || volume > 5) { throw new IllegalArgumentException("AudioClip: Cannot play() media, invalid argument provided"); }

		try
		{
			if (player.getState() == Player.STARTED) { player.stop(); }
			player.setMediaTime(0); // play() should always play media from the beginning, like Nokia Sound
			player.setLoopCount((loop == 0 || loop == 255) ? -1 : loop); // Treat 0 loops, and the max allowed value as infinite looping
			((VolumeControl) player.getControl("VolumeControl")).setLevel(volume * 20); // Received volume varies from 1 to 5, so adapt
			player.start();
		}
		catch (Exception e) {Mobile.log(Mobile.LOG_ERROR, AudioClip.class.getPackage().getName() + "." + AudioClip.class.getSimpleName() + ": " + "AudioClip: Failed to play():" + e.getMessage()); }
	}

	public void resume() 
	{
		/* Resume only restarts the player if it is paused, AND its current saved position is not at the end of the media. Otherwise, this results in infinite playback loops */
		if(player.getState() == Player.PREFETCHED && (player.getMediaTime() < player.getDuration())) { player.start(); }
	}

	public void stop() { player.close(); }

}
