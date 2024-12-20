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
    private Player melodyPlayer; // Instance-specific plafyer
    private static Player currentPlayingMelody;

    public Melody(byte[] data) 
    { 
        try 
        {
            melodyPlayer = Manager.createPlayer(new ByteArrayInputStream(data), "audio/x-mid");
            melodyPlayer.prefetch();
            Melody.currentPlayingMelody = melodyPlayer;
        } 
        catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, Melody.class.getPackage().getName() + "." + Melody.class.getSimpleName() + ": " + " failed to create Melody player:" + e.getMessage());}
    }

    public static void stop() 
	{
        if(currentPlayingMelody != null) { currentPlayingMelody.stop(); }
    }

    public void play() { 
        try 
        {
            melodyPlayer.start();
            Melody.currentPlayingMelody = melodyPlayer;
        } 
		catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, Melody.class.getPackage().getName() + "." + Melody.class.getSimpleName() + ": " + " failed to play Melody:" + e.getMessage());}
    }

}