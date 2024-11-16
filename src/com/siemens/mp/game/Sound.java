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

package com.siemens.mp.game;

import org.recompile.mobile.Mobile;
import javax.microedition.media.Manager;

public class Sound
{
	private static final double SEMITONE_CONST = 17.31234049066755;

	public static void playTone(int frequency, int time) 
	{ 
		try { Manager.playTone(convertFreqToNote(frequency) , time, 100); }
		catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Failed to play tone."); }
	}

	private static int convertFreqToNote(int frequency) { return (int) (Math.round(Math.log((double) frequency / 8.176) * SEMITONE_CONST)); }
}