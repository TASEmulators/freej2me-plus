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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder;

import javax.microedition.media.Manager;

public class Anbu
{

	public static void main(String[] args)
	{
		Anbu app = new Anbu(args);
	}

	private SDL sdl;

	private int lcdWidth;
	private int lcdHeight;

	private Config config;
	private boolean useNokiaControls = false;
	private boolean useSiemensControls = false;
	private boolean useMotorolaControls = false;
	private boolean rotateDisplay = false;

	// Frame Limit Variables
	private int limitFPS = 0;
	private long lastRenderTime = 0;
	private long requiredFrametime = 0;
	private long elapsedTime = 0;
	private long sleepTime = 0;

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

		/* 
		 * If the directory for custom soundfonts doesn't exist, create it, no matter if the user
		 * is going to use it or not.
		 */
		try 
		{
			if(!Manager.soundfontDir.isDirectory()) 
			{ 
				Manager.soundfontDir.mkdirs();
				File dummyFile = new File(Manager.soundfontDir.getPath() + File.separatorChar + "Put your sf2 bank here");
				dummyFile.createNewFile();
			}
		}
		catch(IOException e) { System.out.println("Failed to create custom midi info file:" + e.getMessage()); }

		/* TODO: Anbu has no way of enabling "Dump Audio Streams", a UI rewrite might be in order */

		config = new Config();
		config.onChange = new Runnable() { public void run() { settingsChanged(); } };

		painter = new Runnable()
		{
			public void run()
			{
				try
				{
					int[] data;

					// Send Frame to SDL interface
					data = Mobile.getPlatform().getLCD().getRGB(0, 0, lcdWidth, lcdHeight, null, 0, lcdWidth);
					byte[] frame = new byte[data.length * 3];
					int cb = 0;

					for(int i = 0; i < data.length; i++)
					{
						frame[cb + 0] = (byte)(data[i] >> 16);
						frame[cb + 1] = (byte)(data[i] >> 8);
						frame[cb + 2] = (byte)(data[i]);
						cb += 3;
					}

					if(limitFPS>0)
					{
						requiredFrametime = 1000 / limitFPS;
						elapsedTime = System.currentTimeMillis() - lastRenderTime;
						sleepTime = requiredFrametime - elapsedTime;

						if (sleepTime > 0) { Thread.sleep(sleepTime); }

						if(limitFPS>0) { lastRenderTime = System.currentTimeMillis(); }

						sdl.frame.write(frame);
					} else { sdl.frame.write(frame); }
				}
				catch (Exception e) { }
			}
		};

		Mobile.getPlatform().setPainter(painter);

		Mobile.getPlatform().startEventQueue();

		String file = getFormattedLocation(args[0]);
		System.out.println(file);

		if(Mobile.getPlatform().loadJar(file))
		{
			// Check config
			config.init();

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
			System.exit(0);
		}

		private class SDLKeyTimerTask extends TimerTask
		{
			private int bin;
			private byte[] din = new byte[6];
			private int count = 0;
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
							stop();
						}

						bin = keys.read();
						if(bin==-1) { return; }
						//~ System.out.print(" "+bin);
						din[count] = (byte)(bin & 0xFF);
						count++;
						if (count==5)
						{
							count = 0;
							//System.out.println(din[0] + "|" + din[1] + "|" + din[2] + "|" + din[3] + "|" + din[4] + "|");
							switch(din[0])
							{
								case 0: // keyboard key up
								case 16: // joypad key up
									mobikey = getMobileKey(din[1]); 
									if (mobikey != 0) { keyUp(mobikey); }
								break;

								case 1:  // keyboard key down
								case 17: // joypad key down
									mobikey = getMobileKey(din[1]);
									//System.out.println("JoyKey:" + din[1]);
									if (mobikey != 0) { keyDown(mobikey); }
								break;

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
			if (pressedKeys[mobikeyN] == false)
			{
				Mobile.getPlatform().keyPressed(key);
			}
			else
			{
				Mobile.getPlatform().keyRepeated(key);
			}
			pressedKeys[mobikeyN] = true;
		}

		private void keyUp(int key)
		{
			int mobikeyN = (key + 64) & 0x7F; //Normalized value for indexing the pressedKeys array
			
			Mobile.getPlatform().keyReleased(key);
			pressedKeys[mobikeyN] = false;
		}

		private int getMobileKey(int keycode)
		{
			// Input examples based on a Switch Pro Controller,
			// which is the default setting used by Anbu.cpp
			// Different controllers have different codes.
			// LZ and RZ triggers aren't buttons per se
			// hence, they won't be found here yet.

			// Note: Keyboard mappings are also translated to these
			// if(keycode == 0x00) { return Mobile.KEY_NUM5; } // A - See below
			if(keycode == 0x01) return Mobile.KEY_NUM7; // B
			if(keycode == 0x02) return Mobile.KEY_NUM9; // X
			if(keycode == 0x03) return Mobile.KEY_POUND; // Y

			
			if(keycode == 0x05) return Mobile.KEY_NUM0; // Home
			

			if(keycode == 0x07) return Mobile.GAME_A; // Left Analog press
			if(keycode == 0x08) return Mobile.GAME_B; // Right Analog press

			if(keycode == 0x09) return Mobile.KEY_NUM1; // L
			if(keycode == 0x0A) return Mobile.KEY_NUM3; // R

			// These keys are overridden by the "useXControls" variables
			if(useNokiaControls) 
			{
				if(keycode == 0x00) { return Mobile.NOKIA_SOFT3; } // A
				if(keycode == 0x04) { return Mobile.NOKIA_SOFT1; } // -/Select
				if(keycode == 0x06) { return Mobile.NOKIA_SOFT2; } // +/Start
				if(keycode == 0x0B) { return Mobile.NOKIA_UP; }    // D-Pad Up
				if(keycode == 0x0C) { return Mobile.NOKIA_DOWN; }  // D-Pad Down
				if(keycode == 0x0D) { return Mobile.NOKIA_LEFT; }  // D-Pad Left
				if(keycode == 0x0E) { return Mobile.NOKIA_RIGHT; } // D-Pad Right
			}
			else if(useSiemensControls) 
			{
				if(keycode == 0x00) { return Mobile.SIEMENS_FIRE; }
				if(keycode == 0x04) { return Mobile.SIEMENS_SOFT1; }
				if(keycode == 0x06) { return Mobile.SIEMENS_SOFT2; }
				if(keycode == 0x0B) { return Mobile.SIEMENS_UP; }
				if(keycode == 0x0C) { return Mobile.SIEMENS_DOWN; }
				if(keycode == 0x0D) { return Mobile.SIEMENS_LEFT; }
				if(keycode == 0x0E) { return Mobile.SIEMENS_RIGHT; }
			}
			else if(useMotorolaControls) 
			{
				if(keycode == 0x00) { return Mobile.MOTOROLA_FIRE; }
				if(keycode == 0x04) { return Mobile.MOTOROLA_SOFT1; }
				if(keycode == 0x06) { return Mobile.MOTOROLA_SOFT2; }
				if(keycode == 0x0B) { return Mobile.MOTOROLA_UP; }
				if(keycode == 0x0C) { return Mobile.MOTOROLA_DOWN; }
				if(keycode == 0x0D) { return Mobile.MOTOROLA_LEFT; }
				if(keycode == 0x0E) { return Mobile.MOTOROLA_RIGHT; }
			}
			else // Standard keycodes
			{
				if(keycode == 0x00) { return Mobile.KEY_NUM5; }
				if(keycode == 0x04) { return Mobile.NOKIA_SOFT1; }
				if(keycode == 0x06) { return Mobile.NOKIA_SOFT2; }
				if(keycode == 0x0B) { return Mobile.KEY_NUM2; }
				if(keycode == 0x0C) { return Mobile.KEY_NUM8; }
				if(keycode == 0x0D) { return Mobile.KEY_NUM4; }
				if(keycode == 0x0E) { return Mobile.KEY_NUM6; }
			}
			
			//if(keycode == 0x0F) return Mobile.GAME_C; // Screenshot, shouldn't really be used here

			//if(keycode == 0x1B) { config.start(); } // ESC, special key to bring up the config menu

			return 0;
		}

	} // sdl

	void settingsChanged() 
	{
		limitFPS = Integer.parseInt(config.settings.get("fps"));

		String sound = config.settings.get("sound");
		Mobile.sound = false;
		if(sound.equals("on")) { Mobile.sound = true; }

		String phone = config.settings.get("phone");
		useNokiaControls = false;
		useSiemensControls = false;
		useMotorolaControls = false;
		Mobile.nokia = false;
		Mobile.siemens = false;
		Mobile.motorola = false;
		Mobile.sonyEricsson = false;
		if(phone.equals("Nokia")) { Mobile.nokia = true; useNokiaControls = true; }
		if(phone.equals("Siemens")) { Mobile.siemens = true; useSiemensControls = true; }
		if(phone.equals("Motorola")) { Mobile.motorola = true; useMotorolaControls = true; }
		if(phone.equals("SonyEricsson")) { Mobile.sonyEricsson = true; useNokiaControls = true; }

		// We should send this one over to the sdl interface.
		String rotate = config.settings.get("rotate");
		if(rotate.equals("on")) { rotateDisplay = true; }
		if(rotate.equals("off")) { rotateDisplay = false; }

		String midiSoundfont = config.settings.get("soundfont");
		if(midiSoundfont.equals("Custom"))  { Manager.useCustomMidi = true; }
		else if(midiSoundfont.equals("Default")) { Manager.useCustomMidi = false; }

		if (Mobile.nokia) { System.setProperty("microedition.platform", "Nokia6233/05.10"); } 
		else if (Mobile.sonyEricsson) 
		{
			System.setProperty("microedition.platform", "SonyEricssonK750/JAVASDK");
			System.setProperty("com.sonyericsson.imei", "IMEI 00460101-501594-5-00");
		} else if (Mobile.siemens) 
		{
			System.setProperty("com.siemens.OSVersion", "11");
			System.setProperty("com.siemens.IMEI", "000000000000000");
		}

		// Screen width and height won't be updated here, it breaks sdl_interface's frame streaming
		// as it will be expecting a given size for the frame, and we don't pass the updated size
		// to it (yet)
	}
}
