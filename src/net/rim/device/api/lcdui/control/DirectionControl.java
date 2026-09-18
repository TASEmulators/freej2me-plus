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

package net.rim.device.api.lcdui.control;

import javax.microedition.media.Control;


public interface DirectionControl extends Control{
	
	public static final int DIRECTION_EAST = 2;
	
	public static final int DIRECTION_LANDSCAPE = 16;
	
	public static final int DIRECTION_NORTH = 1;
	
	public static final int DIRECTION_PORTRAIT = 32;
	
	public static final int DIRECTION_WEST = 8;
	
	public static final int ORIENTATION_LANDSCAPE = 16;
	
	public static final int ORIENTATION_PORTRAIT = 32;
	
	public static final int ORIENTATION_SQUARE = 0;
	
	public int getOrientation();
    
	public void setAcceptableScreenDirections(int directions);
}
