/*
	This file is part of FreeJ2ME.

	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	FreeJ2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILTY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/
*/

package net.rim.device.api.ui;


public abstract class TouchGesture {
	
	public TouchGesture() {}
	
	public static final int CLICK_REPEAT = 13575;

	public static final int DOUBLE_TAP = 3;
	
	public static final int HOVER = 0;
	
	public static final int NAVIGATION_SWIPE = 7;
	
	public static final int PINCH_BEGIN = 4;
	
	public static final int PINCH_END = 6;
	
	public static final int PINCH_UPDATE = 5;
	
	public static final int SWIPE = 13572;

	public static final int SWIPE_EAST = 4;
	
	public static final int SWIPE_NORTH = 1;
	
	public static final int SWIPE_SOUTH = 2;
	
	public static final int SWIPE_WEST = 8;
	
	public static final int TAP = 2;
	
	public int getClickRepeatCount() {return 0;};
	
	public abstract int getEvent();
	
	public int getHoverCount() {return 0;};
	
	public float getPinchMagnitude() {return 0;};
	
	public int getSwipeAngle() {return 0;};
	
	public int getSwipeContentAngle() {return 0;};
	
	public int  getSwipeDirection() {return 0;};
	
	public int  getSwipeMagnitude() {return 0;};
	
	public int  getTapCount() {return 0;};
	

}
