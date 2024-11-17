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

package com.siemens.mp.game;

import org.recompile.mobile.Mobile;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import javax.microedition.media.Manager;
import javax.microedition.media.Player;


public class Melody extends com.siemens.mp.misc.NativeMem
{
	/*
	 * Ideally a having a player instance per Melody instance would be enough, but thanks
	 * to the stop() method being static, we have this HashMap mess (at least it's the best
	 * i can come up with at the moment, seems to work on all tested Jars including AH-1 SeaBomber
	 * AquaRace)
	 */
	private static Map<Melody, Player> melodyPlayers = new HashMap<>();
    private static Melody currentPlayingMelody;  // Track the current playing melody
    private Player melodyPlayer = null; // Instance-specific player
    private byte[] melody = null; 
    public int len = 0;
    public int bpm = MelodyComposer.BPM;

    public Melody() { }


    public static void stop() 
	{
        if (currentPlayingMelody != null) 
		{
            currentPlayingMelody.stopPlaying(); 
            currentPlayingMelody = null;
        }
    }

    // Non-static method to stop the melody playing
    private void stopPlaying() 
	{
        if (melodyPlayer != null) 
		{
            melodyPlayer.stop();
        }
    }

    public void play() { 
        try {
            // If not already in the map, create a new player
            if (!melodyPlayers.containsKey(this)) 
			{
                melodyPlayer = Manager.createPlayer(new ByteArrayInputStream(melody), "audio/x-mid");
                melodyPlayers.put(this, melodyPlayer);
            }
            melodyPlayers.get(this).start();
            currentPlayingMelody = this; // Set this instance as the currently playing melody
        } 
		catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, Melody.class.getPackage().getName() + "." + Melody.class.getSimpleName() + ": " + " failed to play Melody:" + e.getMessage());}
    }

    public void populateMelody(byte[] data) { melody = data; }
}