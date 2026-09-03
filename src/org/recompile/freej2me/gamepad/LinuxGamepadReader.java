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
			File[] files = inputDir.listFiles();
			if (files != null)
			{
				for (File file : files)
				{
					if (file.getName().matches("js\\d+")) {
						devices.add(file.getAbsolutePath());
					}
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
			System.err.println("[Gamepad] Device not found: " + devicePath);
			return;
		}

		FileInputStream in = null;
		try
		{
			in = new FileInputStream(joystickFile);
			byte[] buffer = new byte[8];
			System.out.println("[Gamepad] Connected: " + deviceName + " (" + devicePath + ")");

			while (running)
			{
				int bytesRead = 0;
				while (bytesRead < 8 && running)
				{
					int r = in.read(buffer, bytesRead, 8 - bytesRead);
					if (r == -1) { break; }
					bytesRead += r;
				}

				if (bytesRead < 8) { break; }

				short value = (short) ((buffer[4] & 0xFF) | ((buffer[5] & 0xFF) << 8));
				int type = buffer[6] & 0xFF;
				int number = buffer[7] & 0xFF;

				boolean isInit = (type & 0x80) != 0;
				type &= ~0x80;

				GamepadInputListener listen = this.listener;

				if (type == TYPE_BUTTON && !isInit)
				{
					String buttonName = "Button-" + number;

					//System.out.println(deviceName + " -> " + buttonName + ": " + (value == 1 ? "PRESSED" : "RELEASED"));

					// For remapping
					if (listen != null) { listen.onInputDetected(buttonName, number); }

					if (value == 1) { MobilePlatform.keyPressed(Mobile.getMobileKey(this.getKey(number))); }
					else { MobilePlatform.keyReleased(Mobile.getMobileKey(this.getKey(number))); }
				}
				else if (type == TYPE_AXIS && !isInit)
				{
					// TODO: Make deadzone adjustable? This should be fine
					// since we only need digital presses, not analog intensity.
					int deadzone = 12000;
					String axisName = (value > 0 ? "+Axis-" : "-Axis-") + number;
					int posCode = 100 + (number * 2) + 1;
					int negCode = 100 + (number * 2);
					int axisVal = value > 0 ? posCode : negCode;

					// For remapping
					if (listen != null && Math.abs(value) > deadzone)
					{
						listen.onInputDetected(axisName, axisVal);
					}

					//System.out.println(deviceName + " -> " + axisName + ": " + value);

					if (Math.abs(value) > deadzone)
					{
						// Release the opposite direction here. I had some
						// issues where quick flicks failed to result in a release.
						int oppositeCode = value > 0 ? negCode : posCode;
						MobilePlatform.keyReleased(Mobile.getMobileKey(this.getKey(oppositeCode)));
						MobilePlatform.keyPressed(Mobile.getMobileKey(this.getKey(axisVal)));
					}
					else
					{
						MobilePlatform.keyReleased(Mobile.getMobileKey(this.getKey(posCode)));
						MobilePlatform.keyReleased(Mobile.getMobileKey(this.getKey(negCode)));
					}
				}
			}
		}
		catch (Exception e) { System.err.println("[Gamepad] Input stream disconnected: " + e.getMessage()); }
		finally { System.out.println("[Gamepad] Input reader stopped for " + devicePath); }
	}
}
