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

		FileInputStream in = null;
		try
		{
			in = new FileInputStream(joystickFile);
			byte[] buffer = new byte[8];
			Mobile.log(Mobile.LOG_INFO, GamepadReader.class.getPackage().getName() + "." + GamepadReader.class.getSimpleName() + ": " + "[Gamepad] Connected: " + deviceName + " (" + devicePath + ")");

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
					else
					{
						int keyIndex = this.getKey(number);

						// Min value means this button is not mapped. Return.
						if(keyIndex == Integer.MIN_VALUE) { continue; }

						if (value == 1)
						{
							if(!MobilePlatform.pressedKeys[keyIndex])
							{
								MobilePlatform.pressedKeys[keyIndex] = true;
								MobilePlatform.keyPressed(Mobile.getMobileKey(keyIndex));
							}
							else { MobilePlatform.keyRepeated(Mobile.getMobileKey(keyIndex)); }
						}
						else
						{
							MobilePlatform.pressedKeys[keyIndex] = false;
							MobilePlatform.keyReleased(Mobile.getMobileKey(keyIndex));
						}
					}
				}
				else if (type == TYPE_AXIS && !isInit)
				{
					String axisName = (value > 0 ? "+Axis-" : "-Axis-") + number;
					int posCode = 100 + (number * 2) + 1;
					int negCode = 100 + (number * 2);
					int axisVal = value > 0 ? posCode : negCode;

					if (listen != null && Math.abs(value) > ((number == 16 || number == 17) ? 0 : AXIS_PRESS_THRESHOLD))
					{
						listen.onInputDetected(axisName, axisVal);
					}
					else
					{
						int axisKeyIndex = this.getKey(axisVal);
						int opsKeyIndex = this.getKey(value > 0 ? negCode : posCode);

						if(axisKeyIndex == Integer.MIN_VALUE && opsKeyIndex == Integer.MIN_VALUE) { continue; }

						if (Math.abs(value) > ((number == 16 || number == 17) ? 0 : AXIS_PRESS_THRESHOLD))
						{
							if (opsKeyIndex != Integer.MIN_VALUE && MobilePlatform.pressedKeys[opsKeyIndex])
							{
								MobilePlatform.pressedKeys[opsKeyIndex] = false;
								MobilePlatform.keyReleased(Mobile.getMobileKey(opsKeyIndex));
							}

							if(axisKeyIndex != Integer.MIN_VALUE && !MobilePlatform.pressedKeys[axisKeyIndex])
							{
								MobilePlatform.pressedKeys[axisKeyIndex] = true;
								MobilePlatform.keyPressed(Mobile.getMobileKey(axisKeyIndex));
							}
							else if (axisKeyIndex != Integer.MIN_VALUE) { MobilePlatform.keyRepeated(Mobile.getMobileKey(axisKeyIndex)); }
						}
						else
						{
							if (axisKeyIndex != Integer.MIN_VALUE && MobilePlatform.pressedKeys[axisKeyIndex])
							{
								MobilePlatform.pressedKeys[axisKeyIndex] = false;
								MobilePlatform.keyReleased(Mobile.getMobileKey(axisKeyIndex));
							}
							if (opsKeyIndex != Integer.MIN_VALUE && MobilePlatform.pressedKeys[opsKeyIndex])
							{
								MobilePlatform.pressedKeys[opsKeyIndex] = false;
								MobilePlatform.keyReleased(Mobile.getMobileKey(opsKeyIndex));
							}
						}
					}
				}
			}
		}
		catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, GamepadReader.class.getPackage().getName() + "." + GamepadReader.class.getSimpleName() + ": " + "[Gamepad] Input stream disconnected: " + e.getMessage()); }
		finally { Mobile.log(Mobile.LOG_INFO, GamepadReader.class.getPackage().getName() + "." + GamepadReader.class.getSimpleName() + ": " + "[Gamepad] Input reader stopped for " + devicePath); }
	}
}
