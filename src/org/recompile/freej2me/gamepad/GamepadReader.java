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

import java.util.Collections;
import java.util.ArrayList;

import org.recompile.freej2me.FJGUI;
import org.recompile.mobile.Mobile;

public abstract class GamepadReader implements Runnable
{
	protected static final byte TYPE_BUTTON = 1;
	protected static final byte TYPE_AXIS = 2;

	protected final String devicePath;
	protected final String deviceName;
	protected final FJGUI gui;
	protected volatile boolean running = true;

	// listener for input remapping support
	protected volatile GamepadInputListener listener;

	@FunctionalInterface
	public interface GamepadInputListener
	{
		void onInputDetected(String inputName, int inputCode);
	}

	public GamepadReader(String devicePath, String deviceName, FJGUI gui)
	{
		this.devicePath = devicePath;
		this.deviceName = deviceName;
		this.gui = gui;
	}

	public static ArrayList<String> getAvailableDevices()
	{
		String os = System.getProperty("os.name").toLowerCase();

		// We only support gamepads on Linux (Unix) right now.
		if (os.contains("linux")) { return LinuxGamepadReader.getAvailableDevices(); }
		else if (os.contains("win"))
		{
			// TODO: WindowsGamepadReader.getAvailableDevices();
			return null;
		}
		else if (os.contains("mac"))
		{
			// TODO: MacGamepadReader.getAvailableDevices();
			return null;
		}

		return null;
	}

	public String getDeviceName() { return deviceName; }
	public String getDevicePath() { return devicePath; }

	public void setInputListener(GamepadInputListener listener)
	{
		this.listener = listener;
	}

	protected int getKey(int keycode)
	{
		for(int i = 0; i < gui.gamepadKeycodes.length; i++)
		{
			if(keycode == gui.gamepadKeycodes[i]) { return Mobile.convertAWTKeycode(i);}
		}
		return Integer.MIN_VALUE;
	}

	public void stop() { this.running = false; }
}
