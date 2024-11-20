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
package com.immersion;

import org.recompile.mobile.Mobile;

public class VibeTonz 
{

	int device;

    public VibeTonz() 
    {
		Mobile.log(Mobile.LOG_WARNING, VibeTonz.class.getPackage().getName() + "." + VibeTonz.class.getSimpleName() + ": " + "VibeTonz not implemented. ");
    }

	public void Initialize() { Mobile.log(Mobile.LOG_WARNING, VibeTonz.class.getPackage().getName() + "." + VibeTonz.class.getSimpleName() + ": " + "Initialize not implemented. "); }

	public void Terminate() { Mobile.log(Mobile.LOG_WARNING, VibeTonz.class.getPackage().getName() + "." + VibeTonz.class.getSimpleName() + ": " + "Terminate not implemented. "); }

    public int OpenDevice(int i) 
    {
		Mobile.log(Mobile.LOG_WARNING, VibeTonz.class.getPackage().getName() + "." + VibeTonz.class.getSimpleName() + ": " + "OpenDevice not implemented. ");
		device = i;
		return device;
	}
	
	/* Pure guesswork here */
    public void PlayIVTEffect(int device, int effect, int duration) 
	{ 
		Mobile.log(Mobile.LOG_WARNING, VibeTonz.class.getPackage().getName() + "." + VibeTonz.class.getSimpleName() + ": " + "PlayIVTEffect not implemented. ");
		Mobile.log(Mobile.LOG_WARNING, VibeTonz.class.getPackage().getName() + "." + VibeTonz.class.getSimpleName() + ": " + "Values: " + device + ", " + effect + ", " + duration);
	}

    public void SetDevicePropertyString(int device, int arg2, String propString) 
	{
		Mobile.log(Mobile.LOG_WARNING, VibeTonz.class.getPackage().getName() + "." + VibeTonz.class.getSimpleName() + ": " + "SetDevicePropertyString not implemented. ");
		Mobile.log(Mobile.LOG_WARNING, VibeTonz.class.getPackage().getName() + "." + VibeTonz.class.getSimpleName() + ": " + "Values: " + device + ", " + arg2 + ", " + propString);
	}

    public void ModifyPlayingMagSweepEffect(int device, int arg2, int arg3, int arg4, int arg5, int arg6, int arg7, int arg8, int arg9) 
	{ 
		Mobile.log(Mobile.LOG_WARNING, VibeTonz.class.getPackage().getName() + "." + VibeTonz.class.getSimpleName() + ": " + "ModifyPlayingMagSweepEffect not implemented. ");
		Mobile.log(Mobile.LOG_WARNING, VibeTonz.class.getPackage().getName() + "." + VibeTonz.class.getSimpleName() + ": " + "Values: " + device + ", " + arg2 + ", " + arg3 + ", " + arg4 + ", " + arg5 + ", " + arg6 + ", " + arg7 + ", " + arg8 + ", " + arg9);
	}

    public void ModifyPlayingPeriodicEffect(int device, int arg2, int arg3, int arg4, int arg5, int arg6, int arg7, int arg8, int arg9, int arg10) 
	{ 
		Mobile.log(Mobile.LOG_WARNING, VibeTonz.class.getPackage().getName() + "." + VibeTonz.class.getSimpleName() + ": " + "ModifyPlayingPeriodicEffect not implemented. ");
		Mobile.log(Mobile.LOG_WARNING, VibeTonz.class.getPackage().getName() + "." + VibeTonz.class.getSimpleName() + ": " + "Values: " + device + ", " + arg2 + ", " + arg3 + ", " + arg4 + ", " + arg5 + ", " + arg6 + ", " + arg7 + ", " + arg8 + ", " + arg9 + ", " + arg10);
	}

	public int PlayPeriodicEffect(int device, int arg2, int arg3, int arg4, int arg5, int arg6, int arg7, int arg8, int arg9) 
	{
		Mobile.log(Mobile.LOG_WARNING, VibeTonz.class.getPackage().getName() + "." + VibeTonz.class.getSimpleName() + ": " + "ModifyPlayingPeriodicEffect not implemented. ");
		Mobile.log(Mobile.LOG_WARNING, VibeTonz.class.getPackage().getName() + "." + VibeTonz.class.getSimpleName() + ": " + "Values: " + device + ", " + arg2 + ", " + arg3 + ", " + arg4 + ", " + arg5 + ", " + arg6 + ", " + arg7 + ", " + arg8 + ", " + arg9);

        return 0;
    }

    public void StopAllPlayingEffects(int device) { Mobile.log(Mobile.LOG_WARNING, VibeTonz.class.getPackage().getName() + "." + VibeTonz.class.getSimpleName() + ": " + "StopAllPlayingEffects not implemented. "); }

    public int CloseDevice(int device) { Mobile.log(Mobile.LOG_WARNING, VibeTonz.class.getPackage().getName() + "." + VibeTonz.class.getSimpleName() + ": " + "CloseDevice not implemented. "); return 0; }

    public int OpenIVTMemoryFile(byte[] IVT) { Mobile.log(Mobile.LOG_WARNING, VibeTonz.class.getPackage().getName() + "." + VibeTonz.class.getSimpleName() + ": " + "OpenIVTMemoryFile not implemented. "); return 0; }

}