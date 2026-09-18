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

import java.io.InputStream;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import java.util.Collections;
import java.util.ArrayList;

import org.recompile.freej2me.FJGUI;
import org.recompile.mobile.Mobile;
import org.recompile.mobile.MobilePlatform;

public abstract class GamepadReader implements Runnable
{
	protected static final byte TYPE_BUTTON = 1;
	protected static final byte TYPE_AXIS = 2;

	// We don't need analog input. We just treat them as digital inputs.
	protected static final int AXIS_PRESS_THRESHOLD   = 16000;

	// For key repeat events
	private static Thread repeatThread;
	private static volatile boolean repeatRunning = false;

	protected final String devicePath;
	protected final String deviceName;
	protected final FJGUI gui;
	protected int activeAxis = -1; // -1 means no axis is active right now
	protected volatile boolean running = true;
	protected InputStream in;
	protected volatile int lastPressedKey = -1;

	// listener for input remapping support
	protected volatile GamepadInputListener listener;

	public interface GamepadInputListener
	{
		void onInputDetected(String inputName, int inputCode);
	}

	public GamepadReader(String devicePath, String deviceName, FJGUI gui)
	{
		this.devicePath = devicePath;
		this.deviceName = deviceName;
		this.gui = gui;
		startKeyRepeatThread();
	}

	public static ArrayList<String> getAvailableDevices()
	{
		String os = System.getProperty("os.name").toLowerCase();

		// We only support gamepads on Linux (Unix) and Windows right now.
		if (os.contains("linux")) { return LinuxGamepadReader.getAvailableDevices(); }
		else if (os.contains("win")) { return WindowsGamepadReader.getAvailableDevices(); }
		else if (os.contains("mac")) { return MacGamepadReader.getAvailableDevices(); }

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

	// Reader implementations must call this (and handle IOExceptions)
	protected int handleInput(ReadableByteChannel channel, ByteBuffer buffer) throws IOException
	{
		buffer.clear();
		while (buffer.hasRemaining() && running)
		{
			int r = channel.read(buffer);
			if (r == -1) { break; }
		}

		if (buffer.position() < 8) { return -1; }

		buffer.flip();

		int time = buffer.getInt();
		short value = buffer.getShort();
		int type = buffer.get() & 0xFF;
		int number = buffer.get() & 0xFF;

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
				if(keyIndex == Integer.MIN_VALUE) { return 0; }

				if (value == 1)
				{
					if(!MobilePlatform.pressedKeys[keyIndex])
					{
						MobilePlatform.pressedKeys[keyIndex] = true;
						lastPressedKey = keyIndex;
						MobilePlatform.keyPressed(Mobile.getMobileKey(keyIndex));
					}
				}
				else
				{
					MobilePlatform.pressedKeys[keyIndex] = false;
					// Find any other pressed key to repeat
					if (lastPressedKey == keyIndex) { lastPressedKey = findPressedKey(); }
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

				if(axisKeyIndex == Integer.MIN_VALUE && opsKeyIndex == Integer.MIN_VALUE) { return 0; }

				if (Math.abs(value) > ((number == 16 || number == 17) ? 0 : AXIS_PRESS_THRESHOLD))
				{
					if (opsKeyIndex != Integer.MIN_VALUE && MobilePlatform.pressedKeys[opsKeyIndex])
					{
						MobilePlatform.pressedKeys[opsKeyIndex] = false;
						if (lastPressedKey == opsKeyIndex) { lastPressedKey = findPressedKey(); }
						MobilePlatform.keyReleased(Mobile.getMobileKey(opsKeyIndex));
					}

					if(axisKeyIndex != Integer.MIN_VALUE && !MobilePlatform.pressedKeys[axisKeyIndex])
					{
						MobilePlatform.pressedKeys[axisKeyIndex] = true;
						lastPressedKey = axisKeyIndex;
						MobilePlatform.keyPressed(Mobile.getMobileKey(axisKeyIndex));
					}
				}
				else
				{
					if (axisKeyIndex != Integer.MIN_VALUE && MobilePlatform.pressedKeys[axisKeyIndex])
					{
						MobilePlatform.pressedKeys[axisKeyIndex] = false;
						if (lastPressedKey == axisKeyIndex) { lastPressedKey = findPressedKey(); }
						MobilePlatform.keyReleased(Mobile.getMobileKey(axisKeyIndex));
					}
					if (opsKeyIndex != Integer.MIN_VALUE && MobilePlatform.pressedKeys[opsKeyIndex])
					{
						MobilePlatform.pressedKeys[opsKeyIndex] = false;
						if (lastPressedKey == opsKeyIndex) { lastPressedKey = findPressedKey(); }
						MobilePlatform.keyReleased(Mobile.getMobileKey(opsKeyIndex));
					}
				}
			}
		}

		return 0;
	}

	public boolean isRunning() { return running; }

	private synchronized void startKeyRepeatThread()
	{
		if (repeatRunning) { return; }
		repeatRunning = true;

		repeatThread = new Thread(new Runnable()
		{
			public void run()
			 {
				final long START_DELAY = 350;
				int currentActiveKey = -1;
				long keyPressTime = 0;

				while (repeatRunning)
				{
					long time = System.currentTimeMillis();

					if (lastPressedKey != -1 && MobilePlatform.pressedKeys[lastPressedKey])
					{
						if (lastPressedKey != currentActiveKey)
						{
							currentActiveKey = lastPressedKey;
							keyPressTime = time;
						}
						else
						{
							if (time - keyPressTime >= START_DELAY)
							{
								MobilePlatform.keyRepeated(Mobile.getMobileKey(currentActiveKey));
							}
						}
					}
					else { currentActiveKey = -1; }

					try { Thread.sleep(33); } // Repeat keys at a 30 fps interval, close to AWT
					catch (InterruptedException e) { break; }
				}
			 }
		}, "Gamepad-KeyRepeatThread");

		repeatThread.setDaemon(true);
		repeatThread.start();
	}

	// When a key is released, this is called to find if any other key is currently pressed for repeats
	protected static int findPressedKey()
	{
		for (int i = 0; i < MobilePlatform.pressedKeys.length; i++)
		{
			if (MobilePlatform.pressedKeys[i]) { return i; }
		}
		return -1;
	}

	public void stop()
	{
		this.running = false;

		// Stop the key repeat thread as well
		synchronized (GamepadReader.class)
		{
			repeatRunning = false;
			if (repeatThread != null)
			{
				repeatThread.interrupt();
				repeatThread = null;
			}
		}
	}
}
