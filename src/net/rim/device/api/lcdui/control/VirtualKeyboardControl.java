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


public interface VirtualKeyboardControl extends Control {
	
	public static final int KEYBOARD_HIDE = 0;
	
	public static final int KEYBOARD_HIDE_FORCE = 2;
	
	public static final int KEYBOARD_IGNORE = 4;
	
	public static final int KEYBOARD_RESTORE = 5;
	
	public static final int KEYBOARD_SHOW = 1;
	
	public static final int KEYBOARD_SHOW_FORCE = 3;
	
	public void setKeyboardVisibility(int visibility);
	
	public int getKeyboardVisibility();

}
