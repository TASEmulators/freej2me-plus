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
package org.recompile.freej2me;

import org.recompile.mobile.*;

import java.awt.Image;
import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Graphics2D;

import java.io.File;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder;

public class Anbu
{

	public static void main(String[] args)
	{
		Anbu app = new Anbu(args);
	}

	private SDL sdl;

	private int lcdWidth;
	private int lcdHeight;

	private boolean[] pressedKeys = new boolean[128];

	private Runnable painter;

	public Anbu(String args[])
	{
		lcdWidth = 240;
		lcdHeight = 320;

		if (args.length < 3)
		{
			System.out.println("Insufficient parameters provided");
			return;
		}
		lcdWidth = Integer.parseInt(args[1]);
		lcdHeight = Integer.parseInt(args[2]);

		Mobile.setPlatform(new MobilePlatform(lcdWidth, lcdHeight));

		painter = new Runnable()
		{
			public void run()
			{
				try
				{
					// Send Frame to SDL interface
					int[] data = Mobile.getPlatform().getLCD().getRGB(0, 0, lcdWidth, lcdHeight, null, 0, lcdWidth);
					byte[] frame = new byte[data.length * 3];
					int cb = 0;
					for(int i = 0; i < data.length; i++)
					{
						frame[cb + 0] = (byte)(data[i] >> 16);
						frame[cb + 1] = (byte)(data[i] >> 8);
						frame[cb + 2] = (byte)(data[i]);
						cb += 3;
					}
					sdl.frame.write(frame);
				}
				catch (Exception e) { }
			}
		};

		Mobile.getPlatform().setPainter(painter);

		String file = getFormattedLocation(args[0]);
		System.out.println(file);

		if(Mobile.getPlatform().loadJar(file))
		{
			// Check config

			// Start SDL
			sdl = new SDL();
			sdl.start(args);

			// Run jar
			Mobile.getPlatform().runJar();
		}
		else
		{
			System.out.println("Couldn't load jar...");
			System.exit(0);
		}
	}

	private static String getFormattedLocation(String loc)
	{
		if (loc.startsWith("file://") || loc.startsWith("http://") || loc.startsWith("https://"))
			return loc;

		File file = new File(loc);
		if(!file.isFile())
		{
			System.out.println("File not found...");
			System.exit(0);
		}

		return "file://" + file.getAbsolutePath();
	}

	private class SDL
	{
		private Timer keytimer;
		private TimerTask keytask;

		private Process proc;
		private InputStream keys;
		public OutputStream frame;

		public void start(String args[])
		{
			try
			{
				// Check if we're receiving the MS Windows separator and set up accordingly
				if (File.separatorChar == '\\') { args[0] = System.getenv("USERPROFILE") + "\\freej2me\\bin\\sdl_interface.exe"; } 
				else { args[0] = "/usr/local/bin/sdl_interface"; }

				proc = new ProcessBuilder(args).start();

				keys = proc.getInputStream();
				frame = proc.getOutputStream();

				keytimer = new Timer();
				keytask = new SDLKeyTimerTask();
				keytimer.schedule(keytask, 0, 5);
			}
			catch (Exception e)
			{
				System.out.println("Failed to start sdl_interface");
				System.out.println(e.getMessage());
				System.exit(0);
			}
		}

		public void stop()
		{
			proc.destroy();
			keytimer.cancel();
		}

		private class SDLKeyTimerTask extends TimerTask
		{
			private int bin;
			private byte[] din = new byte[6];
			private int count = 0;
			private int code;
			private int mobikey;
			int mousex = 0, mousey = 0;

			public void run()
			{
				try // to read keys
				{
					while(true)
					{
						if(!proc.isAlive()) 
						{
							System.out.println("SDL interface was closed. Cleaning up...");
							System.exit(0);
						}

						bin = keys.read();
						if(bin==-1) { return; }
						//~ System.out.print(" "+bin);
						din[count] = (byte)(bin & 0xFF);
						count++;
						if (count==5)
						{
							count = 0;
							code = (din[1]<<24) | (din[2]<<16) | (din[3]<<8) | din[4];
							//~ System.out.println(" ("+code+") <- Key");
							System.out.println(din[0] + "|" + din[1] + "|" + din[2] + "|" + din[3] + "|" + din[4] + "|");
							switch(din[0])
							{
								case 0: // keyboard key up
									mobikey = getMobileKey(din[1]); 
									if (mobikey != 0)
									{
										keyUp(mobikey);
									}
									break;

								case 1:  // keyboard key down
									mobikey = getMobileKey(din[1]); 
									if (mobikey != 0)
									{
										keyDown(mobikey);
									}
									break;

								case 16: // joypad key up
									mobikey = getMobileKey(din[1]);
									if (mobikey != 0)
									{
										keyUp(mobikey);
									}
								break;

								case 17: // joypad key down
									mobikey = getMobileKey(din[1]);
									//System.out.println("JoyKey:" + din[1]);
									if (mobikey != 0)
									{
										keyDown(mobikey);
									}

								case 4: // mouse up
									mousex = ((din[1] & 0xFF) << 8) | (din[2] & 0xFF);
									mousey = ((din[3] & 0xFF) << 8) | (din[4] & 0xFF);

									Mobile.getPlatform().pointerReleased(mousex, mousey);
								break;

								case 5: // mouse down
									mousex = ((din[1] & 0xFF) << 8) | (din[2] & 0xFF);
									mousey = ((din[3] & 0xFF) << 8) | (din[4] & 0xFF);

									Mobile.getPlatform().pointerPressed(mousex, mousey);
									//System.out.println("press| pointerX:" + mousex + " | PointerY:" + mousey);
								break;

								case 6: // mouse drag (not implemented yet)
									mousex = ((din[1] & 0xFF) << 8) | (din[2] & 0xFF);
									mousey = ((din[3] & 0xFF) << 8) | (din[4] & 0xFF);

									Mobile.getPlatform().pointerDragged(mousex, mousey);
									//System.out.println("drag | pointerX:" + mousex + " | PointerY:" + mousey);
								break;

								case 127:
									// Received boot settings from sdl interface

									// Only rotation is taken into account for now, and mostly for debugging reasons since the entire work is done in sdl.
									// Anbu.java is mostly acting as the message printer here.
									if((((din[1] & 0xFF) << 8) | (din[2] & 0xFF)) == 270) { System.out.println("Screen rotated!"); }
									
								default: break;
							}
						} 
					}
				}
				catch (Exception e) { }
			}
		} // timer

		private void keyDown(int key)
		{
			int mobikeyN = (key + 64) & 0x7F; //Normalized value for indexing the pressedKeys array
			//if(config.isRunning)
			//{
			//	config.keyPressed(key);
			//}
			//else
			//{
			if (pressedKeys[mobikeyN] == false)
			{
				Mobile.getPlatform().keyPressed(key);
			}
			else
			{
				Mobile.getPlatform().keyRepeated(key);
			}
			//}
			pressedKeys[mobikeyN] = true;
		}

		private void keyUp(int key)
		{
			int mobikeyN = (key + 64) & 0x7F; //Normalized value for indexing the pressedKeys array
			//if(!config.isRunning)
			//{
			Mobile.getPlatform().keyReleased(key);
			//}
			pressedKeys[mobikeyN] = false;
		}

		private int getMobileKey(int keycode)
		{
			switch(keycode)
			{
				// Inputs examples based on a Switch Pro Controller,
				// which is the default setting used by Anbu.cpp
				// Different controllers have different codes.
				// LZ and RZ triggers aren't buttons per se
				// hence, they won't be found here yet.
				case 0x00: return Mobile.KEY_NUM5; // A
				case 0x01: return Mobile.KEY_NUM7; // B
				case 0x02: return Mobile.KEY_NUM9; // X
				case 0x03: return Mobile.KEY_POUND; // Y

				case 0x04: return Mobile.NOKIA_SOFT1; // -/Select
				case 0x05: return Mobile.KEY_NUM0; // Home
				case 0x06: return Mobile.NOKIA_SOFT2; // +/Start

				case 0x07: return Mobile.GAME_A; // Left Analog press
				case 0x08: return Mobile.GAME_B; // Right Analog press

				case 0x09: return Mobile.KEY_NUM1; // L
				case 0x0A: return Mobile.KEY_NUM3; // R

				case 0x0B: return Mobile.KEY_NUM2; // D-Pad Up
				case 0x0C: return Mobile.KEY_NUM8; // D-Pad Down
				case 0x0D: return Mobile.KEY_NUM4; // D-Pad Left
				case 0x0E: return Mobile.KEY_NUM6; // D-Pad Right
				//case 0x0F: return Mobile.GAME_C; // Screenshot, shouldn't really be used here
			}
			return 0;
		}

	} // sdl
}
