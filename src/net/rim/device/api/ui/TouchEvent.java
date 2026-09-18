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


public abstract class TouchEvent {

	public TouchEvent() {}
	
	public static final int CANCEL = 0;

	public static final int CLICK = 13573;
	
	public static final int DOWN = 13569;
	
	public static final int GESTURE = 1;
	
	public static final int MOVE = 13571;
	
	public static final int UNCLICK = 13574;
	
	public static final int UP = 13570;
	
	public abstract int getEvent();
	
	public abstract TouchGesture getGesture();
	
	public abstract int getGlobalX(int touch);
	
	public abstract int getGlobalY(int touch);
	
	public abstract void getMovePoints(int touch, int[] x, int[] y, int[] time);
	
	public abstract int getMovePointsSize();
	
	public abstract int getTime();
	
	public abstract int getX(int touch);
	
	public abstract int getY(int touch);
	
	public abstract boolean isValid();

}
