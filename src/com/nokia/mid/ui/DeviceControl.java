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
package com.nokia.mid.ui;

import javax.microedition.lcdui.Display;

import org.recompile.mobile.Mobile;

public class DeviceControl
{
	public static void flashLights(long duration) { Mobile.getDisplay().flashBacklight((int)duration); }

	public static void setLights(int num, int level) 
	{ 
		if(level == 0)       { Mobile.renderLCDMask = false; }
		else                 { Mobile.renderLCDMask = true;  }
	}

	public static void startVibra(int freq, long duration) 
	{
		if(freq == 0) { return; } // No need to vibrate if the strength will be zero.
		if(freq < 0 || freq > 100) { throw new IllegalArgumentException("Cannot startVibra(), freq value is out of bounds"); }
		
		Mobile.vibrationDuration = (int) duration;
		Mobile.vibrationStrength = (int) ((freq / 100.0) * 0xFFFF); // Map from 0-100 to 0x0000-0xFFFF
	}

	public static void stopVibra() 
	{ 
		Mobile.vibrationDuration = 0; 
		Mobile.vibrationStrength = 0xFFFF;
	}
}
