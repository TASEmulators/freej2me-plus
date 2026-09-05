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
import java.io.InputStream;
import java.util.ArrayList;

import org.recompile.freej2me.FJGUI;
import org.recompile.mobile.Mobile;
import org.recompile.mobile.MobilePlatform;

public class WindowsGamepadReader extends GamepadReader
{
	// The exe IS REQUIRED to be in the same directory as the jar.
	private static final String WIN_32_PAD = "Win32Pad.exe";

	private Process win32PadProcess;
	private InputStream in;

	// This one is used to kill the input reader process when FreeJ2ME-Plus
	// closes, otherwise it will be dangling there doing nothing on the OS.
	private Thread shutdownHook;

	public WindowsGamepadReader(String devicePath, FJGUI gui)
	{
		super(devicePath, resolveDeviceName(devicePath), gui);
	}

	private static String resolveDeviceName(String path)
	{
		// Not sure DirectInput has a way to read the device
		// name like linux does on /dev/input and SysFS, so we
		// just append the path (which is just "0") to a fixed string.
		return "DInput Gamepad " + path;
	}

	public static ArrayList<String> getAvailableDevices()
	{
		File win32Pad = getFromParentDir();

		// No exe on directory? No devices either so return early.
		if (!win32Pad.exists() || win32Pad.length() == 0) { return null; }

		ArrayList<String> devices = new ArrayList<String>();
		devices.add("0");
		return devices;
	}

	@Override
	public void run()
	{
		try
		{
			File win32Pad = getFromParentDir();

			if (!win32Pad.exists() || win32Pad.length() == 0)
			{
				System.err.println("[Gamepad] ERROR: Required helper is missing: " + win32Pad.getAbsolutePath());
				System.err.println("[Gamepad] Please check if '" + WIN_32_PAD + "' is in the same directory as this jar.");
				return;
			}

			// Launch helper process with device target ID as argument (we only use ID 0)
			ProcessBuilder pb = new ProcessBuilder(win32Pad.getAbsolutePath(), "0");

			pb.redirectErrorStream(true);
			win32PadProcess = pb.start();

			// Needing to reference this object into a final object just so it
			// can be ran on a thread... nice.
			final WindowsGamepadReader self = this;
			shutdownHook = new Thread(new Runnable()
			{
				public void run() { self.stop(); }
			});
			Runtime.getRuntime().addShutdownHook(shutdownHook);

			// Direct binary input stream from C stdout
			in = win32PadProcess.getInputStream();
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

				if (bytesRead < 8 || !running) { break; }

				short value = (short) ((buffer[4] & 0xFF) | ((buffer[5] & 0xFF) << 8));
				int type = buffer[6] & 0xFF;
				int number = buffer[7] & 0xFF;

				boolean isInit = (type & 0x80) != 0;
				type &= ~0x80;

				GamepadInputListener listen = this.listener;

				if (type == TYPE_BUTTON && !isInit)
				{
					String buttonName = "Button-" + number;

					if (listen != null) { listen.onInputDetected(buttonName, number); }
					else
					{
						if (value == 1) { MobilePlatform.keyPressed(Mobile.getMobileKey(this.getKey(number))); }
						else { MobilePlatform.keyReleased(Mobile.getMobileKey(this.getKey(number))); }
					}
				}
				else if (type == TYPE_AXIS && !isInit)
				{
					// Axis 16 and 17 are the D-Pad in DInput, we cannot have
					// deadzones on those.
					int deadzone = (number == 16 || number == 17) ? 0 : 12000;
					String axisName = (value > 0 ? "+Axis-" : "-Axis-") + number;
					int posCode = 100 + (number * 2) + 1;
					int negCode = 100 + (number * 2);
					int axisVal = value > 0 ? posCode : negCode;

					if (listen != null && Math.abs(value) > deadzone)
					{
						listen.onInputDetected(axisName, axisVal);
					}
					else
					{
						if (Math.abs(value) > deadzone)
						{
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
		}
		catch (Exception e)
		{
			// That exception will be caught when we're closing the process,
			// so only log the error if this happens when we're not actually
			// closing it.
			if (running)
			{
				System.err.println("[Gamepad] Windows Input stream error: " + e.getMessage());
			}
		}
		finally
		{
			stop();
			System.out.println("[Gamepad] Input reader stopped for device " + devicePath);
		}
	}

	// Resolves the parent directory of FreeJ2ME-Plus' jar, so that we don't
	// have issues loading the gamepad exe in situation where the commandline
	// is at a different directory when launching the jar.
	private static File getFromParentDir()
	{
		try
		{
			File jarDir = new File(WindowsGamepadReader.class.getProtectionDomain().
				getCodeSource().getLocation().toURI()).getParentFile();
			return new File(jarDir, WIN_32_PAD);
		}
		catch (Exception e)
		{
			// Fallback to working directory if URI resolution fails
			return new File(WIN_32_PAD);
		}
	}

	public void stop()
	{
		super.stop();

		// Deregister the hook during usual refresh button presses to avoid
		// leaving useless triggers laying around... that often causes all sorts
		// of weird issues.
		if (shutdownHook != null)
		{
			try { Runtime.getRuntime().removeShutdownHook(shutdownHook); }
			catch (Exception e) { }
			shutdownHook = null;
		}

		if (win32PadProcess != null)
		{
			try
			{
				// Ask windows to nuke the process.
				win32PadProcess.destroy();

				// Close remaining process pipes so OS handles are dropped and
				// we can close this process gracefully.
				try { win32PadProcess.getOutputStream().close(); }
				catch (Exception e) { }

				try { win32PadProcess.getInputStream().close(); }
				catch (Exception e) { }

				try { win32PadProcess.getErrorStream().close(); }
				catch (Exception e) { }
			}
			catch (Exception e) { }
			finally { win32PadProcess = null; }
		}

		// Close the gamepad input stream so we don't get stuck on blocking IO.
		if (in != null)
		{
			try { in.close(); }
			catch (Exception ignored) { }
			in = null;
		}
	}
}
