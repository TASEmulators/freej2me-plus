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
package com.sprintpcs.media;

import java.io.ByteArrayInputStream;

import javax.microedition.lcdui.Display;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;

import org.recompile.mobile.Mobile; 

public class Player 
{
	private static javax.microedition.media.Player player;
	private static int priority;

	public static void play(Clip clip, int repeat) 
    {
        Mobile.log(Mobile.LOG_WARNING, Player.class.getPackage().getName() + "." + Player.class.getSimpleName() + ": " + "Untested");

		if (repeat < -1) { throw new IllegalArgumentException("Invalid repeat value was received"); }

		if (clip.getPriority() < priority) 
        { 
            Mobile.log(Mobile.LOG_WARNING, Player.class.getPackage().getName() + "." + Player.class.getSimpleName() + ": " + "Clip priority check.");
            return; 
        }

		if (player != null) { player.close(); }

        try { player = clip.getPlayer(); }
        catch (Exception e) { Mobile.log(Mobile.LOG_WARNING, Player.class.getPackage().getName() + "." + Player.class.getSimpleName() + ": " + "failed to play media: " + e.getMessage()); }
        

		if (repeat != -1 /* Clip.LOOP_CONTINUOUSLY */) { player.setLoopCount(repeat+1); }
		else { player.setLoopCount(repeat); }
		
		Mobile.vibrationDuration = clip.getVibration();
		
        try { player.start(); } 
        catch (Exception e) { e.printStackTrace(); }
	}

    public static void play(DualTone dTone, int repeat) // I assume the second argument is repeat, there's no documentation for it
    {
		if (repeat < -1) { throw new IllegalArgumentException("Invalid repeat value was received"); }

		if (player != null) { player.close(); }

        try { player = Manager.createPlayer(new ByteArrayInputStream(dTone.sequence), "audio/mid"); }
        catch (Exception e) { Mobile.log(Mobile.LOG_WARNING, Player.class.getPackage().getName() + "." + Player.class.getSimpleName() + ": " + "failed to prepare tone media: " + e.getMessage()); }
        

		if (repeat != -1 /* Clip.LOOP_CONTINUOUSLY */) { player.setLoopCount(repeat+1); }
		else { player.setLoopCount(repeat); }
				
        try {  player.start(); } 
        catch (Exception e) 
        { 
            Mobile.log(Mobile.LOG_WARNING, Player.class.getPackage().getName() + "." + Player.class.getSimpleName() + ": " + "failed to play media: " + e.getMessage());
        }
	}

	public static void stop() {
		if (player != null) {
			player.close();
			player = null;
		}
	}
}