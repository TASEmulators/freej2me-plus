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
import java.io.IOException;

import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;

import org.recompile.mobile.Mobile; 

public class Clip 
{
	private String contentType;
	private int priority;
	private int vibration;
	private byte[] stream;
	private String locator;

	public Clip(String locator, String contentType, int priority, int vibration) throws IOException 
    {
		this.locator = locator;
		this.contentType = contentType;
		this.priority = priority;
		this.vibration = vibration;
	}

	public Clip(byte[] stream, String contentType, int priority, int vibration) throws IOException 
    {
		this.stream = stream;
		this.contentType = contentType;
		this.priority = priority;
		this.vibration = vibration;
	}

	public String getContentType() { return contentType; }

	public String getStateString() { return "Inactive"; }

	protected Player getPlayer() throws MediaException
    {
		Player player = null;
		try 
        {
			if (stream != null) { player = Manager.createPlayer(new ByteArrayInputStream(stream), contentType); } 
            else 
            {
                // JAMDAT's Solitaire Deluxe prepends "resource:" before the actual resource location, also is the only sprint jar i've seen do this (and load data with locators)
                if(locator.contains(":"))
                {
                    int lastIndex = locator.lastIndexOf(":");
                    
                    return Manager.createPlayer(Mobile.getResourceAsStream(null, locator.substring(lastIndex + 1)), contentType); 
                }
		    }
        }
        catch (IOException e) { Mobile.log(Mobile.LOG_WARNING, Clip.class.getPackage().getName() + "." + Clip.class.getSimpleName() + ": " + "failed to getPlayer: " + e.getMessage()); } 
        
		return player;
	}

	protected int getPriority() { return priority; }

	protected int getVibration() { return vibration; }
}