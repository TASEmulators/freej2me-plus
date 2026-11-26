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
package com.nokia.mid.ui.gestures;

public class GestureInteractiveZone
{
	public static final int GESTURE_TAP = 0x1;
	public static final int GESTURE_LONG_PRESS = 0x2;
	public static final int GESTURE_DRAG = 0x4;
	public static final int GESTURE_DROP = 0x8;
	public static final int GESTURE_FLICK = 0x10;
	public static final int GESTURE_LONG_PRESS_REPEATED = 0x20;
	public static final int GESTURE_PINCH = 0x40;
	public static final int GESTURE_DOUBLE_TAP = 0x80;
	public static final int GESTURE_RECOGNITION_START = 0x4000;
	public static final int GESTURE_RECOGNITION_END = 0x8000;
	public static final int GESTURE_ALL = GESTURE_TAP
		| GESTURE_LONG_PRESS
		| GESTURE_DRAG
		| GESTURE_DROP
		| GESTURE_FLICK
		| GESTURE_LONG_PRESS_REPEATED
		| GESTURE_PINCH
		| GESTURE_DOUBLE_TAP
		| GESTURE_RECOGNITION_START
		| GESTURE_RECOGNITION_END;

	public GestureInteractiveZone(int gestures)
	{ 
		
	}

	public GestureInteractiveZone(int gestures, int timeInterval)
	{ 
		
	}

	public void setRectangle(int x, int y, int width, int height)
	{ 
		
	}

	public void setLongPressTimeInterval(int timeInterval)
	{ 
		
	}

	public int getGestures()
	{
		return 0;
	}

	public void setGestures(int gestures)
	{
		
	}

	public int getX()
	{
		return 0;
	}

	public int getY()
	{
		return 0;
	}

	public int getWidth()
	{
		return 0;
	}

	public int getHeight()
	{
		return 0;
	}

	public int getLongPressTimeInterval()
	{
		return 0;
	}

	public static boolean isSupported(int gestureEventIdentity)
	{
		return false;
	}
}
