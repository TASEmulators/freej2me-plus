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
import javax.microedition.media.control.ToneControl;

import org.recompile.mobile.Mobile; 

public class Player 
{
    private static javax.microedition.media.Player player;
    private static javax.microedition.media.Player bgPlayer;
    private static PlayerListener listener;
    private static int priority;

	public static void play(Clip clip, int repeat) 
    {
		if (repeat < -1) { throw new IllegalArgumentException("Invalid repeat value was received"); }

		if (clip.getPriority() < priority) 
        { 
            Mobile.log(Mobile.LOG_INFO, Player.class.getPackage().getName() + "." + Player.class.getSimpleName() + ": " + "new Clip priority lower than current media.");
            return; 
        }

        priority = clip.getPriority();

		if (player != null) { player.close(); }

        try { player = clip.getPlayer(); }
        catch (Exception e) { Mobile.log(Mobile.LOG_WARNING, Player.class.getPackage().getName() + "." + Player.class.getSimpleName() + ": " + "failed to prepare Clip media: " + e.getMessage()); }
        

		if (repeat != -1 /* Clip.LOOP_CONTINUOUSLY */) { player.setLoopCount(repeat+1); }
		else { player.setLoopCount(repeat); }
		
		Mobile.vibrationDuration = clip.getVibration();
		
        try 
        { 
            player.start();
            if(listener != null) { listener.playerUpdate(PlayerListener.STARTED, player.getMediaTime()); }
        } 
        catch (Exception e) { Mobile.log(Mobile.LOG_WARNING, Player.class.getPackage().getName() + "." + Player.class.getSimpleName() + ": " + "failed to play Clip media: " + e.getMessage()); }
	}

    public static void play(DualTone dTone, int repeat) // I assume the second argument is repeat, there's no documentation for it
    {
		if (repeat < -1) { throw new IllegalArgumentException("Invalid repeat value was received"); }

        if (dTone.getPriority() < priority) 
        { 
            Mobile.log(Mobile.LOG_INFO, Player.class.getPackage().getName() + "." + Player.class.getSimpleName() + ": " + "new DualTone priority lower than current media.");
            return; 
        }

        priority = dTone.getPriority();

        try 
        {
            if(player == null) { player = Manager.createPlayer(new ByteArrayInputStream(dTone.sequence), "audio/x-tone-seq"); }
            
            else 
            {
                player.deallocate();
				((ToneControl) player.getControl("ToneControl")).setSequence(dTone.sequence);
                // No need to prefetch here, as player.start() will have to happen below.
            }
        }
        catch (Exception e) { Mobile.log(Mobile.LOG_WARNING, Player.class.getPackage().getName() + "." + Player.class.getSimpleName() + ": " + "failed to prepare DualTone media: " + e.getMessage()); }
        
		if (repeat != -1 /* Clip.LOOP_CONTINUOUSLY */) { player.setLoopCount(repeat+1); }
		else { player.setLoopCount(repeat); }

        Mobile.vibrationDuration = dTone.getVibration();
				
        try 
        { 
            player.start();
            if(listener != null) { listener.playerUpdate(PlayerListener.STARTED, player.getMediaTime()); }
        } 
        catch (Exception e) 
        { 
            Mobile.log(Mobile.LOG_WARNING, Player.class.getPackage().getName() + "." + Player.class.getSimpleName() + ": " + "failed to play DualTone media: " + e.getMessage());
        }
	}

    public static void playBackground(Clip clip, int repeat) 
    {
        if (repeat < -1) { throw new IllegalArgumentException("Invalid repeat value was received"); }

		if (bgPlayer != null) { bgPlayer.close(); }

        try { bgPlayer = clip.getPlayer(); }
        catch (Exception e) { Mobile.log(Mobile.LOG_WARNING, Player.class.getPackage().getName() + "." + Player.class.getSimpleName() + ": " + "failed to prepare Clip media: " + e.getMessage()); }
        

		if (repeat != -1 /* Clip.LOOP_CONTINUOUSLY */) { bgPlayer.setLoopCount(repeat+1); }
		else { bgPlayer.setLoopCount(repeat); }
		
		Mobile.vibrationDuration = clip.getVibration();
		
        try 
        { 
            bgPlayer.start();
            if(listener != null) { listener.playerUpdate(PlayerListener.STARTED, bgPlayer.getMediaTime()); }
        } 
        catch (Exception e) { Mobile.log(Mobile.LOG_WARNING, Player.class.getPackage().getName() + "." + Player.class.getSimpleName() + ": " + "failed to play Clip media on Background: " + e.getMessage()); }
    }

    public static void resume() 
    { 
        player.start(); 
        if(listener != null) { listener.playerUpdate(PlayerListener.STARTED, player.getMediaTime()); } 
    }

    public static void addPlayerListener(PlayerListener playerListener) { listener = playerListener; }

    public static void removePlayerListener(PlayerListener playerListener) { listener = null; }

	public static void stop() 
    {
		if (player != null) 
        { 
            player.stop(); 
            if(listener != null) { listener.playerUpdate(PlayerListener.STOPPED, player.getMediaTime()); }
        }
	}
}