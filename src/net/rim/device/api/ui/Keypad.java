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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.recompile.mobile.Mobile;

public class Keypad
{
	public static final long GUID_KEYPAD_CHANGED = -3769281743063593175L;
	public static final int HW_LAYOUT_32 = 364669234;
	public static final int HW_LAYOUT_39 = 364669241;
	public static final int HW_LAYOUT_HANDW_RECOGNITION = 213682245;
	public static final int HW_LAYOUT_ITUT = 230263636;
	public static final int HW_LAYOUT_LEGACY = 295594807;
	public static final int HW_LAYOUT_PHONE = 179602501;
	public static final int HW_LAYOUT_REDUCED = 364346180;
	public static final int HW_LAYOUT_REDUCED_24 = 364341300;
	public static final int HW_LAYOUT_TOUCHSCREEN_12 = 414738226;
	public static final int HW_LAYOUT_TOUCHSCREEN_12A = 412510273;
	public static final int HW_LAYOUT_TOUCHSCREEN_12C = 412510275;
	public static final int HW_LAYOUT_TOUCHSCREEN_12H = 412510280;
	public static final int HW_LAYOUT_TOUCHSCREEN_20J = 412575306;
	public static final int HW_LAYOUT_TOUCHSCREEN_20JA = 842025537;
	public static final int HW_LAYOUT_TOUCHSCREEN_20K = 412575307;
	public static final int HW_LAYOUT_TOUCHSCREEN_20N = 412575310;
	public static final int HW_LAYOUT_TOUCHSCREEN_24 = 414738484;
	public static final int HW_LAYOUT_TOUCHSCREEN_29 = 414738489;
	public static final int HW_LAYOUT_TOUCHSCREEN_29T = 412577620;
	public static final int HW_LAYOUT_TOUCHSCREEN_35H = 412642120;
	public static final int HW_LAYOUT_TOUCHSCREEN_35J = 412642122;
	public static final int HW_LAYOUT_TOUCHSCREEN_37A = 412642625;
	public static final int HW_LAYOUT_TOUCHSCREEN_37B = 412642626;
	public static final int KEY_ALT = 257;
	public static final int KEY_APPLICATION = 21;
	public static final int KEY_BACKLIGHT = 259;
	public static final int KEY_BACKSPACE = 8;
	public static final int KEY_BACKWARD = 4101;
	public static final int KEY_CAMERA_FOCUS = 211;
	public static final int KEY_CONVENIENCE_1 = 9;
	public static final int KEY_CONVENIENCE_2 = 21;
	public static final int KEY_DELETE = 27;
	public static final int KEY_END = 8;
	public static final int KEY_ENTER = 0;
	public static final int KEY_ESCAPE = 27;
	public static final int KEY_FORWARD = 4100;
	public static final int KEY_LOCK = 4099;
	public static final int KEY_MENU = 4098;
	public static final int KEY_MIDDLE = 9;
	public static final int KEY_NEXT = 20;
	public static final int KEY_RIGHT_3 = 95;
	public static final int KEY_SEND = 7;
	public static final int KEY_SHIFT_LEFT = 258;
	public static final int KEY_SHIFT_RIGHT = 256;
	public static final int KEY_SHIFT_X = 261;
	public static final int KEY_SPACE = 32;
	public static final int KEY_SPEAKERPHONE = 273;
	public static final int KEY_VOLUME_DOWN = 4097;
	public static final int KEY_VOLUME_UP = 4096;

	// TODO: Actual functionality for this.
	private static int hardwareLayout = HW_LAYOUT_39;

	// Actual constructor and methods.
	public static char getAltedChar(char ch)
	{
		Mobile.log(Mobile.LOG_WARNING, Keypad.class.getPackage().getName() + "." + Keypad.class.getSimpleName() + ": " + "getAltedChar not implemented.");
		return ' ';
	}

	public static Locale getLocale()
	{
		Mobile.log(Mobile.LOG_WARNING, Keypad.class.getPackage().getName() + "." + Keypad.class.getSimpleName() + ": " + "getUnaltedChar not implemented.");
		return null;
	}

	public static char getUnaltedChar(char ch)
	{
		Mobile.log(Mobile.LOG_WARNING, Keypad.class.getPackage().getName() + "." + Keypad.class.getSimpleName() + ": " + "getUnaltedChar not implemented.");
		return ' ';
	}

	public static char map(int keycode) 
	{
		Mobile.log(Mobile.LOG_WARNING, Keypad.class.getPackage().getName() + "." + Keypad.class.getSimpleName() + ": " + "map A not implemented. Key:" + keycode);
		return ' ';
	}

	public static char map(int key, int status)
	{
		Mobile.log(Mobile.LOG_WARNING, Keypad.class.getPackage().getName() + "." + Keypad.class.getSimpleName() + ": " + "map B not implemented. Key:" + key + " " + status);
		return ' ';
	}

	public static void getKeyChars(int key, int status, StringBuffer result)
	{
		if (result == null) { return; }
		
		Mobile.log(Mobile.LOG_WARNING, Keypad.class.getPackage().getName() + "." + Keypad.class.getSimpleName() + ": " + "getKeyChars A not implemented.");
	}

	public static void getKeyChars(int keycode, StringBuffer result)
	{
		if (result == null) { return; }

		Mobile.log(Mobile.LOG_WARNING, Keypad.class.getPackage().getName() + "." + Keypad.class.getSimpleName() + ": " + "getKeyChars B not implemented.");
	}

	public static int key(int keycode)
	{ 
		Mobile.log(Mobile.LOG_WARNING, Keypad.class.getPackage().getName() + "." + Keypad.class.getSimpleName() + ": " + "getKeyChars B not implemented. Key:" + keycode);
		return keycode & 0xFFFF;
	}

	public static int getHardwareLayout() { return hardwareLayout; }

	public static int getKeyCode(char ch, int status)
	{
		Mobile.log(Mobile.LOG_WARNING, Keypad.class.getPackage().getName() + "." + Keypad.class.getSimpleName() + ": " + "getKeyCode not implemented.");
		return 0;
	}

	public static int status(int keycode) { return (keycode >> 16) & 0xFFFF; }
	
	public static boolean isOnKeypad(char ch)
	{
		Mobile.log(Mobile.LOG_WARNING, Keypad.class.getPackage().getName() + "." + Keypad.class.getSimpleName() + ": " + "isOnKeypad not implemented.");
		return false;
	}

	public static boolean isValidKeyCode(int keycode)
	{
		Mobile.log(Mobile.LOG_WARNING, Keypad.class.getPackage().getName() + "." + Keypad.class.getSimpleName() + ": " + "isValidKeyCode not implemented.");
		return false;
	}

	public static int keycode(char scancode, int status)
	{
		// Do direct composition of scancode (lower 16 bits) and
		// status (upper 16 bits).
		return ((status & 0xFFFF) << 16) | (scancode & 0xFFFF);
	}

	public static boolean hasSendEndKeys() { return true; }

	public static boolean hasCurrencyKey() { return false; }

	public static boolean hasMuteKey() { return false; }

	public static boolean hasMediaKeys() { return false; }
}
