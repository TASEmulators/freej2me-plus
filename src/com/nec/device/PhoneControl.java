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
package com.nec.device;

import org.recompile.mobile.Mobile;

public final class PhoneControl
{
	public static final int DEV_BACKLIGHT = 0;
	public static final int DEV_VIBRATION = 3;

	public static final int ATTR_BACKLIGHT_OFF = 1;
	public static final int ATTR_BACKLIGHT_ON = 2;

	// What these do is unknown... intensity maybe? Could be patterns too...
	public static final int ATTR_VIBRATION_OFF = 4;
	public static final int ATTR_VIBRATION_1_ON = 5;
	public static final int ATTR_VIBRATION_2_ON = 6;
	public static final int ATTR_VIBRATION_3_ON = 7;

	private static int vibStatus = ATTR_VIBRATION_OFF, ledStatus = ATTR_BACKLIGHT_OFF;

	public static final void setAttribute(int device, int attr) throws IllegalArgumentException
	{
		if(device != DEV_BACKLIGHT && device != DEV_VIBRATION)
			{ throw new IllegalArgumentException("Invalid device"); }

		if(device == DEV_BACKLIGHT)
		{
			switch(attr)
			{
				case ATTR_BACKLIGHT_OFF:
					Mobile.getDisplay().flashBacklight(0);
					break;
				case ATTR_BACKLIGHT_ON:
					Mobile.getDisplay().flashBacklight(Integer.MAX_VALUE);
					break;
				default:
					throw new IllegalArgumentException("Invalid attribute");
			}
			ledStatus = attr;
		}
		else
		{
			switch(attr)
			{
				case ATTR_VIBRATION_OFF:
					Mobile.getDisplay().vibrate(0);
					Mobile.vibrationStrength = 0xFFFF;
					break;
				case ATTR_VIBRATION_1_ON:
					Mobile.getDisplay().vibrate(Integer.MAX_VALUE);
					Mobile.vibrationStrength = 0x5555;
					break;
				case ATTR_VIBRATION_2_ON:
					Mobile.getDisplay().vibrate(Integer.MAX_VALUE);
					Mobile.vibrationStrength = 0xAAAA;
					break;
				case ATTR_VIBRATION_3_ON:
					Mobile.getDisplay().vibrate(Integer.MAX_VALUE);
					Mobile.vibrationStrength = 0xFFFF;
					break;
				default:
					throw new IllegalArgumentException("Invalid attribute");
			}
			vibStatus = attr;
		}
	}

	public static final int getAttribute(int device) throws IllegalArgumentException
	{
		if(device != DEV_BACKLIGHT && device != DEV_VIBRATION)
			{ throw new IllegalArgumentException("Invalid device"); }

		return device == DEV_BACKLIGHT ? ledStatus : vibStatus;
	}

	// Devices are always available here.
	public static final boolean isAvailable(int device) throws IllegalArgumentException
	{
		if(device != DEV_BACKLIGHT && device != DEV_VIBRATION)
			{ throw new IllegalArgumentException("Invalid device"); }

		return true;
	}
}
