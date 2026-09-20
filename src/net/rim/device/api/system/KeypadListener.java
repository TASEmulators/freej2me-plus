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
package net.rim.device.api.system;

public interface KeypadListener
{
	public static final int STATUS_ALT = 1;
	public static final int STATUS_SHIFT = 2;
	public static final int STATUS_CAPS_LOCK = 4;
	public static final int STATUS_KEY_HELD_WHILE_ROLLING = 8;
	public static final int STATUS_ALT_LOCK = 16;
	public static final int STATUS_SHIFT_LEFT = 32;
	public static final int STATUS_SHIFT_RIGHT = 64;
	public static final int STATUS_NOT_FROM_KEYPAD = 32768;
	public static final int STATUS_TRACKWHEEL = 1073741824;
	public static final int STATUS_FOUR_WAY = 536870912;
}
