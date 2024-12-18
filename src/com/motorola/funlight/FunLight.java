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
package com.motorola.funlight;

import org.recompile.mobile.Mobile;

public class FunLight 
{
	public static int BLACK = 0x00000000;
	public static int BLANK = 0;
	public static int BLUE = 0x000000FF;
	public static int CYAN = 0x0000FFFF;
	public static int GREEN = 0x0000FF00;
	public static int IGNORED = 2;
	public static int MAGENTA = 0x00FF00FF;
	public static int OFF = 0x00000000;
	public static int ON = 0x00FFFFFF;
	public static int QUEUED = 1;
	public static int RED = 0x00FF0000;
	public static int SUCCESS = 0;
	public static int WHITE = 0x00FFFFFF;
	public static int YELLOW = 0x00FFFF00;

    private static DeviceRegion[] deviceRegions = 
    {
        new DeviceRegion(0), // Blank Region
        new DeviceRegion(1)  // Display Region (this array position is the same on all devices that support it, it seems)
        // TODO: Implement those other Regions in some way, maybe by lighting the corners of the screen or something, as most jars light up the main display fully white
        // new DeviceRegion(2) // Navigation Keypad
        // new DeviceRegion(3) // Numeric Keypad
        // new DeviceRegion(4) // Sidebands
    };

    private static int[] availableRegions = {0, 1};

	public static int getControl() { return QUEUED; } // Calls to the Blank Region will always return QUEUED, even if others succeed

	public static Region getRegion(int ID) 
    {
        if(ID < deviceRegions.length) { return deviceRegions[ID]; }
		return deviceRegions[0];
	}

	public static Region[] getRegions() { return deviceRegions; }

	public static int[] getRegionsIDs() { return availableRegions; }

	public static void releaseControl() { }

	public static int setColor(byte red, byte green, byte blue) 
    {
        for(int i = 0; i < deviceRegions.length; i++) { deviceRegions[i].setColor(red, green, blue);}
		return QUEUED;
	}

	public static int setColor(int color) 
    {
        for(int i = 0; i < deviceRegions.length; i++) { deviceRegions[i].setColor(color);}
		return QUEUED;
	}

	static class DeviceRegion implements Region 
    {
        // This should be 0x0RGB, but since we're not aiming to run FreeJ2ME on an actual FunLights device, we can use 0x00RRGGBB normally
        private int color = 0xFFFFFFFE; // Initial value to indicate that it hasn't been set yet
        private int ID;

        public DeviceRegion(int ID) 
        { 
            Mobile.log(Mobile.LOG_DEBUG, FunLight.class.getPackage().getName() + "." + FunLight.class.getSimpleName() + ": " + " New Light Region: " + ID);
            this.ID = ID; 
        }

		public int getColor() throws FunLightException
        { 
            if(ID != 0 && color == 0xFFFFFFFE) { throw new FunLightException(); }
            return color; 
        }

		
		public int getControl() { return ID == 0 ? QUEUED : SUCCESS; }

		
		public int getID() { return this.ID; }

		
		public void releaseControl() { }

		
		public int setColor(byte red, byte green, byte blue) 
        {
            this.color = (red<<16) + (green<<8) + blue;
            if(this.ID == 1) // ID == 1 means it's the Display Region
            {
                Mobile.lcdMaskColors[6] = (0xFF << 24) + this.color; // Alpha is always fully opaque when handling actual backlights
                Mobile.maskIndex = 6;
                Mobile.getDisplay().flashBacklight(Integer.MAX_VALUE);
            }
			return ID == 0 ? QUEUED : SUCCESS;
		}

		
		public int setColor(int color) 
        {
            this.color = color;
            if(this.ID == 1) 
            {
                Mobile.lcdMaskColors[6] = (0xFF << 24) + this.color;
                Mobile.maskIndex = 6;
                Mobile.getDisplay().flashBacklight(Integer.MAX_VALUE);
            }
			return ID == 0 ? QUEUED : SUCCESS;
		}
	}
}