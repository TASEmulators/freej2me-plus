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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;

import java.util.ArrayList;

import org.recompile.freej2me.FJGUI;
import org.recompile.mobile.Mobile;
import org.recompile.mobile.MobilePlatform;

public class LinuxGamepadReader extends GamepadReader
{
	public LinuxGamepadReader(String devicePath, FJGUI gui)
	{
		super(devicePath, resolveDeviceName(devicePath), gui);
	}

	// We can get the device's name from SysFS, those are always located in
	// "/sys/class/input/js*/device/name"
	private static String resolveDeviceName(String path)
	{
		String fileName = new File(path).getName();
		File sysNameFile = new File("/sys/class/input/" + fileName + "/device/name");

		if (sysNameFile.exists() && sysNameFile.canRead())
		{
			try
			{
				FileInputStream fis = new FileInputStream(sysNameFile);
				byte[] data = new byte[256];
				int read = fis.read(data);
				if (read > 0) { return new String(data, 0, read).trim(); }
			}
			catch (Exception e) { }
		}
		return "Unknown Controller (" + fileName + ")";
	}

	// We use /dev/input here at the moment. Evdev would be more modern, but
	// that one requires permissions, and the only advantage would be Gyro
	// and a few other more advanced features.
	public static ArrayList<String> getAvailableDevices()
	{
		ArrayList<String> devices = new ArrayList<String>();
		File inputDir = new File("/dev/input");

		if (inputDir.exists() && inputDir.isDirectory())
		{
			for (int i = 0; i < 32; i++)
			{
				File jsDevice = new File(inputDir, "js" + i);
				if (jsDevice.exists())
				{
					devices.add(jsDevice.getAbsolutePath());
				}
			}
		}
		return devices.isEmpty() ? null : devices;
	}

	@Override
	public void run()
	{
		File joystickFile = new File(devicePath);
		if (!joystickFile.exists())
		{
			Mobile.log(Mobile.LOG_ERROR, GamepadReader.class.getPackage().getName() + "." + GamepadReader.class.getSimpleName() + ": " + "[Gamepad] Device not found: " + devicePath);
			return;
		}

		in = null;
		FileChannel channel = null;
		try
		{
			in = new FileInputStream(joystickFile);
			channel = ((FileInputStream)in).getChannel();
			ByteBuffer buffer = ByteBuffer.allocate(8);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			Mobile.log(Mobile.LOG_INFO, GamepadReader.class.getPackage().getName() + "." + GamepadReader.class.getSimpleName() + ": " + "[Gamepad] Connected: " + deviceName + " (" + devicePath + ")");

			while (running)
			{
				if(handleInput(channel, buffer) == -1) { break; }
			}
		}
		catch (ClosedByInterruptException ce) { Mobile.log(Mobile.LOG_INFO, GamepadReader.class.getPackage().getName() + "." + GamepadReader.class.getSimpleName() + ": " + "[Gamepad] Input stream closed for refresh."); }
		catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, GamepadReader.class.getPackage().getName() + "." + GamepadReader.class.getSimpleName() + ": " + "[Gamepad] Input stream disconnected: " + e.getMessage()); }
		finally
		{
			if (channel != null)
			{
				try { channel.close(); }
				catch (Exception e) { }
			}
			if (in != null)
			{
				try { in.close(); }
				catch (Exception e) { }
				in = null;
			}
			stop();
			Mobile.log(Mobile.LOG_INFO, GamepadReader.class.getPackage().getName() + "." + GamepadReader.class.getSimpleName() + ": " + "[Gamepad] Input reader stopped for " + devicePath);
		}
	}
}
