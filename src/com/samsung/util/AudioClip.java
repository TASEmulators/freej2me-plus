
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

		/* Some jars actually try to pass MMF streams with a different type, so check its header to see if this needs to be handled. */
		if(audioData[0] == 'M' && audioData[1] == 'M' && audioData[2] == 'M' && audioData[3] == 'D') { clipType = TYPE_MMF; }

		System.out.println("Samsung AudioClip (ByteArray)");

		if(clipType == TYPE_MMF) 
		{
			try 
			{
				// TODO: Either convert MMF to MIDI (easier), or try to playback MMF directly (MUCH harder)

				//byte[] midiDat = convertMMFToMIDI(audioData);
				//audioData = midiDat;
				//clipType = TYPE_MIDI;
			} catch (Exception e) { System.out.println("AudioClip: Could not convert MMF file to MIDI:" + e.getMessage());}
			
		}

		try { player = Manager.createPlayer(new ByteArrayInputStream(audioData, audioOffset, audioLength), formatMIMEType[clipType-1]); }
		catch (Exception e) {System.out.println("AudioClip: Failed to create player:" + e.getMessage()); }
	}

	public AudioClip(int clipType, String filename)
	{
		if(filename == null) { throw new NullPointerException("AudioClip: Cannot open a player with a null file path"); }
		if(clipType < 1 || clipType > 3) { throw new IllegalArgumentException("AudioClip: Clip type not recognized");}

		System.out.println("Samsung AudioClip (locator)");
		
		InputStream stream = null; // TODO: Actually load this stream from the locator provided

		try { player = Manager.createPlayer(stream, formatMIMEType[clipType-1]); }
		catch (Exception e) {System.out.println("AudioClip: Failed to create player:" + e.getMessage()); }
	}

	public static boolean isSupported() { return true; }

	public void pause() { player.stop(); }

	public void play(int loop, int volume) 
	{  
		if(loop < 0 || loop > 255 || volume < 0 || volume > 5) { throw new IllegalArgumentException("AudioClip: Cannot play() media, invalid argument provided"); }

		try
		{
			if (loop == 0) { loop = -1; }
			if (player.getState() == Player.STARTED) { player.stop(); }
			player.setLoopCount(loop);
			((VolumeControl) player.getControl("VolumeControl")).setLevel(volume * 20); // Received volume varies from 1 to 5, so adapt
			player.start();
		}
		catch (Exception e) {System.out.println("AudioClip: Failed to play():" + e.getMessage()); }
	}

	public void resume() { player.start(); }

	public void stop() { player.close(); }

}
