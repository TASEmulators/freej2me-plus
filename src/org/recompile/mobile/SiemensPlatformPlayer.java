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

import java.io.InputStream;

import javax.microedition.media.Control;
import javax.microedition.media.Player;

public class SiemensPlatformPlayer extends PlatformPlayer implements com.siemens.mp.media.Player 
{
	public SiemensPlatformPlayer(InputStream stream, String type) { super(stream, type); }

	public SiemensPlatformPlayer(String locator) { super(locator); }

	public void addPlayerListener(com.siemens.mp.media.PlayerListener playerListener) 
	{
		super.listeners.add(playerListener);
	}

	public void removePlayerListener(com.siemens.mp.media.PlayerListener playerListener) 
	{
		super.listeners.remove(playerListener);
	}

    // Controllable interface, these two differ from javax on com.mp.siemens.media //

    @Override
	public com.siemens.mp.media.Control getControl(String controlType)
	{
		if(getState() == Player.CLOSED || getState() == Player.UNREALIZED) { throw new IllegalStateException("Cannot call getControl(), as the player is either CLOSED or UNREALIZED."); }
     
        try 
        {
            if(controlType.contains("VolumeControl")) { return (com.siemens.mp.media.Control) super.controls[0]; }
            if(controlType.contains("ToneControl"))   { return (com.siemens.mp.media.Control) super.controls[3]; }
        }
        catch (Exception e) { System.out.println("lmao " + e.getMessage());}
		
		return null;
	}

    @Override
	public com.siemens.mp.media.Control[] getControls() 
	{ 
		if(getState() == Player.CLOSED || getState() == Player.UNREALIZED) { throw new IllegalStateException("Cannot call getControls(), as the player is either CLOSED or UNREALIZED."); }

		return (com.siemens.mp.media.Control[]) super.controls; 
	}
}