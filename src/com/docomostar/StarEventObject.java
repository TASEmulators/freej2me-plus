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
package com.docomostar;

public abstract class StarEventObject
{
	public static final int STAR_AUDIO_OUTPUT_PATH_CHANGED = 14;
	public static final int STAR_AVPLAYER_NOTIFIED = 13;
	public static final int STAR_BLUETOOTH_CONNECT_REQUEST_RECEIVED = 17;
	public static final int STAR_BLUETOOTH_OPERATION_RECEIVED = 16;
	public static final int STAR_CALLED_BY_DTV = 9;
	public static final int STAR_CLOCK_UPDATED = 6;
	public static final int STAR_FACE_STATECHANGE_MINI_FOCUSED = 3;
	public static final int STAR_FACE_STATECHANGE_MINI_SELECTED = 5;
	public static final int STAR_FACE_STATECHANGE_MINI_UNFOCUSED = 4;
	public static final int STAR_FELICA_ADHOC_REQUEST_RECEIVED = 12;
	public static final int STAR_FELICA_PUSHED = 11;
	public static final int STAR_FRAME_CHANGED = 7;
	public static final int STAR_INVITED = 10;
	public static final int STAR_MESSAGE_FOLDER_CHANGED = 8;
	public static final int STAR_STATECHANGE_CLAM_CLOSE = 2;
	public static final int STAR_STATECHANGE_CLAM_OPEN = 1;
	public static final int STAR_WALKING_NAVIGATION_STOPPED = 15;

	private int type;
	
	protected StarEventObject(int eventType)
	{
		this.type = eventType;
	}

	public int getType()
	{
		return this.type;
	}
}
