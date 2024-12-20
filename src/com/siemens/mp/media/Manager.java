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
package com.siemens.mp.media;

import java.io.InputStream;

import javax.microedition.media.MediaException;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.SiemensPlatformPlayer;

public class Manager extends javax.microedition.media.Manager 
{ 
    public static com.siemens.mp.media.Player createPlayer(InputStream stream, String type) throws MediaException
	{
		try 
		{
			com.siemens.mp.media.Player player = (com.siemens.mp.media.Player) javax.microedition.media.Manager.createSiemensPlayer(stream, type);
			player.realize(); // Doing realize right after creating because Chessmaster is such a broken game it's not even funny
			return player;
		} 
		catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Failed to create player from stream:" + e.getMessage()); }
		
		return null;
    }

    public static com.siemens.mp.media.Player createPlayer(com.siemens.mp.media.protocol.DataSource source) throws MediaException
	{
		try 
		{ 
			com.siemens.mp.media.Player player = (com.siemens.mp.media.Player) javax.microedition.media.Manager.createSiemensPlayer(source);
			player.realize(); // Doing realize right after creating because Chessmaster is such a broken game it's not even funny
			return player;
		} 
		catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Failed to create player from source:" + e.getMessage()); }
		
		return null;
	}

    public static Player createPlayer(String locator) throws MediaException
	{
		try 
		{ 
			com.siemens.mp.media.Player player = (com.siemens.mp.media.Player) javax.microedition.media.Manager.createSiemensPlayer(locator); 
			player.realize(); // Doing realize right after creating because Chessmaster is such a broken game it's not even funny
			return player;
		} 
		catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, Manager.class.getPackage().getName() + "." + Manager.class.getSimpleName() + ": " + "Failed to create player from locator:" + e.getMessage()); }
		
		return null;
    }

    public static String[] getSupportedContentTypes(String protocol)
	{
		return javax.microedition.media.Manager.getSupportedContentTypes(protocol);
	}
	
	public static String[] getSupportedProtocols(String content_type)
	{
		return javax.microedition.media.Manager.getSupportedProtocols(content_type);
	}
	
	public static void playTone(int note, int duration, int volume) throws MediaException
	{
        try { javax.microedition.media.Manager.playTone(note, duration, volume); }
        catch (Exception e) { }
	}
}