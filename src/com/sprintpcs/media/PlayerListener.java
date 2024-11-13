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

public interface PlayerListener extends javax.microedition.media.PlayerListener
{ 
    public static final int AUDIO_DEVICE_UNAVAILABLE = 0;
	public static final int END_OF_DATA = 1;
	public static final int ERROR = 2;
	public static final int STARTED = 3;
	public static final int STOPPED = 4;
	public static final int PAUSED = 5;
	public static final int RESUME = 6;
	public static final int PREEMPTED = 7;

	public void playerUpdate(int event, Object eventData);
}