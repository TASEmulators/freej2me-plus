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
package com.samsung.util;

import org.recompile.mobile.Mobile;

public final class Vibration
{
	public static boolean isSupported() { return true; }

	public static void start(int duration, int strength) 
	{ 
		if(duration < 0 || strength < 1 || strength > 5) { throw new IllegalArgumentException("Samsung Vibration: Cannot start vibrating due to illegal argument"); }
		Mobile.vibrationDuration = duration; 
		//Mobile.vibrationStrength = (byte) strength; // This doesn't seem to be important, and is ignored according to the documentation
	}

	public static void stop() {  Mobile.vibrationDuration = 0; }
}
