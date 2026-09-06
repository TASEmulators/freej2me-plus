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
package org.recompile.freej2me.gamepad;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import org.recompile.freej2me.FJGUI;
import org.recompile.mobile.Mobile;
import org.recompile.mobile.MobilePlatform;

// This class is a complete stub at the moment.
public class MacGamepadReader extends GamepadReader
{
	public MacGamepadReader(String devicePath, FJGUI gui)
	{
		super(devicePath, resolveDeviceName(devicePath), gui);
	}

	private static String resolveDeviceName(String path) { return ""; }

	public static ArrayList<String> getAvailableDevices() { return null; }

	@Override
	public void run() { return; }
}
