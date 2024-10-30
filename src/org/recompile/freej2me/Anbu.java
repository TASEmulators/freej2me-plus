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

import io.github.libsdl4j.api.event.SDL_Event;
import io.github.libsdl4j.api.render.SDL_Renderer;
import io.github.libsdl4j.api.video.SDL_Window;
import io.github.libsdl4j.api.render.SDL_Texture;
import io.github.libsdl4j.api.joystick.SDL_Joystick;
import io.github.libsdl4j.api.joystick.SDL_JoystickID;

import static io.github.libsdl4j.api.Sdl.SDL_Init;
import static io.github.libsdl4j.api.SdlSubSystemConst.SDL_INIT_VIDEO;
import static io.github.libsdl4j.api.SdlSubSystemConst.SDL_INIT_JOYSTICK;

import static io.github.libsdl4j.api.render.SdlRender.SDL_CreateRenderer;
import static io.github.libsdl4j.api.render.SdlRender.SDL_RenderClear;
import static io.github.libsdl4j.api.render.SdlRender.SDL_RenderPresent;
import static io.github.libsdl4j.api.render.SdlRender.SDL_RenderCopy;
import static io.github.libsdl4j.api.render.SdlRender.SDL_UpdateTexture;
import static io.github.libsdl4j.api.render.SDL_TextureAccess.SDL_TEXTUREACCESS_STREAMING;
import static io.github.libsdl4j.api.render.SdlRender.SDL_CreateTexture;
import static io.github.libsdl4j.api.render.SdlRender.SDL_DestroyRenderer;
import static io.github.libsdl4j.api.render.SdlRender.SDL_DestroyTexture;

import static io.github.libsdl4j.api.video.SdlVideo.*;
import static io.github.libsdl4j.api.video.SdlVideo.*;
import static io.github.libsdl4j.api.video.SdlVideoConst.*;
import static io.github.libsdl4j.api.video.SDL_WindowFlags.*;
import static io.github.libsdl4j.api.pixels.SDL_PixelFormatEnum.SDL_PIXELFORMAT_RGB888;

import static io.github.libsdl4j.api.event.SdlEvents.SDL_PollEvent;
import static io.github.libsdl4j.api.event.SDL_EventType.*;
import static io.github.libsdl4j.api.event.SdlEventsConst.SDL_PRESSED;
import static io.github.libsdl4j.api.event.SdlEventsConst.SDL_ENABLE;

import static io.github.libsdl4j.api.keycode.SDL_Keycode.*;

import static io.github.libsdl4j.api.joystick.SdlJoystick.SDL_JoystickOpen;
import static io.github.libsdl4j.api.joystick.SdlJoystick.SDL_JoystickEventState;
import static io.github.libsdl4j.api.joystick.SdlJoystickConst.*;
import static io.github.libsdl4j.api.joystick.SdlJoystick.*;

import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;

import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.media.Manager;

import com.sun.jna.Memory;

public class Anbu
{

	public static void main(String[] args)
	{
		Anbu app = new Anbu(args);
	}

	private SDL sdl;

	private int lcdWidth;
	private int lcdHeight;
	private int scaleFactor = 1;
	private boolean isFullscreen = false;
	private boolean SDLInitialized = false;

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

	SDL_Joystick joy = null;

	private boolean[] pressedKeys = new boolean[128];

	private Runnable painter;

	public Anbu(String args[])
	{
		lcdWidth = 240;
		lcdHeight = 320;

		String file = null;

		if(args.length>=1)
		{
			file = getFormattedLocation(args[0]);
			System.out.println(file);
		}
		if(args.length>=3)
		{
		 	lcdWidth = Integer.parseInt(args[1]);
			lcdHeight = Integer.parseInt(args[2]);
		}
		if(args.length>=4) { scaleFactor = Integer.parseInt(args[3]); }

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

		/* TODO: Anbu/SDL has no way of enabling any settings outside of cmd args yet, a UI and code overhaul might be in order */

		config = new Config();
		config.onChange = new Runnable() { public void run() { settingsChanged(); } };

		painter = new Runnable()
		{
			public void run()
			{
				try
				{
					/* Check if vibration commands have to be handled */
					if(Mobile.vibrationDuration != 0) 
					{
						int vib = SDL_JoystickRumble(joy, (short) 0xFFFF, (short) 0xFFFF, Mobile.vibrationDuration);
						Mobile.vibrationDuration = 0;
					}
					
					if(limitFPS>0)
					{
						requiredFrametime = 1000 / limitFPS;
						elapsedTime = System.currentTimeMillis() - lastRenderTime;
						sleepTime = requiredFrametime - elapsedTime;

						if (sleepTime > 0) { Thread.sleep(sleepTime); }

						sdl.paint();

						lastRenderTime = System.currentTimeMillis();

					} else { sdl.paint(); }
				}
				catch (Exception e) { }
			}
		};

		Mobile.getPlatform().setPainter(painter);

		Mobile.getPlatform().startEventQueue();

		if(file != null && Mobile.getPlatform().loadJar(file))
		{
			// Check config
			config.init();

			/* Allows FreeJ2ME to set the width and height passed as cmd arguments. */
			if(args.length>=3)
			{
				lcdWidth = Integer.parseInt(args[1]);
				lcdHeight = Integer.parseInt(args[2]);
				config.settings.put("width",  ""+lcdWidth);
				config.settings.put("height", ""+lcdHeight);
			}

			settingsChanged();

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
		protected SDL_Renderer renderer;
		protected SDL_Window window;
		protected SDL_Texture texture;

		protected Memory pixels;

		private int mouseX;
		private int mouseY;
		private boolean mousePressed = false;
		private boolean mouseDragged = false;
		private int dragThreshold = 2; // threshold in pixels

		private boolean resolutionChanged = false;

		private Timer keytimer;
		private TimerTask keytask;

		public void start(String args[])
		{
			if (SDL_Init(SDL_INIT_VIDEO | SDL_INIT_JOYSTICK) < 0 )
			{
				System.out.println("Unable to initialize SDL");
				stop();
			}

			window = SDL_CreateWindow("FreeJ2ME-Plus - SDL", SDL_WINDOWPOS_CENTERED, SDL_WINDOWPOS_CENTERED, lcdWidth*scaleFactor, lcdHeight*scaleFactor, SDL_WINDOW_SHOWN);
			
			// Create a renderer, and a texture where drawing can take place, streaming for constant updates.
			renderer = SDL_CreateRenderer(window, -1, 0);
			texture = SDL_CreateTexture(renderer, SDL_PIXELFORMAT_RGB888, SDL_TEXTUREACCESS_STREAMING, lcdWidth, lcdHeight);
			pixels = new Memory(lcdWidth * lcdHeight * 4);

			SDL_JoystickEventState(SDL_ENABLE);

			SDLInitialized = true;
		}

		public void stop()
		{
			SDL_DestroyTexture(texture);
			System.exit(0);
		}

		public void paint()
		{
			/* 
			 * Normally, input reading should not be tied to the render logic, because that would softlock jars that only
			 * send new render data after an input is registered (Ex: JBenchmark 2 and some other jars that use Form UI).
			 * But for determinism in libTAS, inputs must be pulled synchronously with rendering, because libTAS processes
			 * inputs (and a lot of other things) during frame boundaries, which is when the program calls its render function.
			 */
			processEvents();

			/* 
			 * Let's make resolution changes and adjust any relevant objects here, as it's right on the render path 
			 * and it makes sure that the objects will be set correctly before being rendered.
			 */
			if(resolutionChanged || Mobile.displayUpdated) { updateScreen(); }

			/* 
			 * Like on libretro, access the image's DataBuffer directly instead of using BufferedImage's getRGB() method,
			 * which is slower. The only difference is that here the data has to be saved as Integers instead of Bytes,
			 * since the image we copy from is from TYPE_INT_ARGB, and not TYPE_3BYTE_BGR.
			 */
			final int[] data = ((DataBufferInt) Mobile.getPlatform().getLCD().getRaster().getDataBuffer()).getData();
            pixels.write(0, data, 0, data.length);

			//SDL_RenderClear(renderer); // We don't need RenderClear since we always repaint the whole screen.
			SDL_UpdateTexture(texture, null, pixels, lcdWidth * 4);
			SDL_RenderCopy(renderer, texture, null, null);
			SDL_RenderPresent(renderer);
		}

		public void processEvents()
		{
			int key;
			int mobikey;

			SDL_Event event = new SDL_Event();
			while (SDL_PollEvent(event) != 0)
			{
				if(event.type == SDL_QUIT)
				{
					stop();
				}
				else if(event.type == SDL_KEYDOWN || event.type == SDL_KEYUP) 
				{
					key = event.key.keysym.sym;
					if (key == SDLK_F4) 
					{
						stop();
					}
					else if (key == SDLK_F8)
					{
						//if(event.type == SDL_KEYDOWN) { Screenshot.takeScreenshot = true; }
						continue;
					}
					else if (key == SDLK_F11) 
					{
						if(event.type == SDL_KEYDOWN) 
						{
							isFullscreen = !isFullscreen;
							toggleFullscreen();
						}
						continue;
					}
					else if(key == SDLK_KP_PLUS) 
					{
						if(event.type == SDL_KEYDOWN) 
						{
							scaleFactor++;
							resolutionChanged = true;
						}
						continue;
					}
					else if(key == SDLK_KP_MINUS) 
					{
						if(scaleFactor > 1 && event.type == SDL_KEYDOWN)
						{
							scaleFactor--;
							resolutionChanged = true;
						}
						continue;
					}

					//printf("Key:%d. Down:%s | cast:%s\n", key, event.key.state == SDL_PRESSED ? "true" : "false", keynames[findInputMappedFunction(key,  KEYBOARD_COMMAND)]);
					
					mobikey = getMobileKey(key);

					if (event.key.state == SDL_PRESSED)
					{
						keyDown(mobikey);
					}
					else
					{
						keyUp(mobikey);						
					}
				}

				else if(event.type == SDL_JOYBUTTONDOWN || event.type == SDL_JOYBUTTONUP) 
				{
					mobikey = getMobileKeyFromButton(event.jbutton.button);
					
					if (event.jbutton.state == SDL_PRESSED)
					{
						keyDown(mobikey);
					}
					else
					{
						keyUp(mobikey);						
					}
					// printf("JoyKey:%d. Down:%s | cast:%s\n", key, event.type == SDL_JOYBUTTONDOWN ? "true" : "false",  keynames[findInputMappedFunction(key, JOYPAD_COMMAND)]);
				}

				else if(event.type == SDL_JOYHATMOTION) 
				{
					if (event.jhat.value == SDL_HAT_LEFTUP)
					{
						keyDown(getMobileKey(SDLK_LEFT));
						keyDown(getMobileKey(SDLK_UP));
						keyUp(getMobileKey(SDLK_RIGHT));
						keyUp(getMobileKey(SDLK_DOWN));
					}
					if (event.jhat.value == SDL_HAT_UP)
					{
						keyUp(getMobileKey(SDLK_LEFT));
						keyDown(getMobileKey(SDLK_UP));
						keyUp(getMobileKey(SDLK_RIGHT));
						keyUp(getMobileKey(SDLK_DOWN));
					}
					if (event.jhat.value == SDL_HAT_RIGHTUP)
					{
						keyUp(getMobileKey(SDLK_LEFT));
						keyDown(getMobileKey(SDLK_UP));
						keyDown(getMobileKey(SDLK_RIGHT));
						keyUp(getMobileKey(SDLK_DOWN));
					}
					if (event.jhat.value == SDL_HAT_LEFT)
					{
						keyDown(getMobileKey(SDLK_LEFT));
						keyUp(getMobileKey(SDLK_UP));
						keyUp(getMobileKey(SDLK_RIGHT));
						keyUp(getMobileKey(SDLK_DOWN));
					}
					if (event.jhat.value == SDL_HAT_LEFTDOWN)
					{
						keyDown(getMobileKey(SDLK_LEFT));
						keyUp(getMobileKey(SDLK_UP));
						keyUp(getMobileKey(SDLK_RIGHT));
						keyDown(getMobileKey(SDLK_DOWN));
					}
					if (event.jhat.value == SDL_HAT_CENTERED)
					{
						keyUp(getMobileKey(SDLK_LEFT));
						keyUp(getMobileKey(SDLK_UP));
						keyUp(getMobileKey(SDLK_RIGHT));
						keyUp(getMobileKey(SDLK_DOWN));
					}
					if (event.jhat.value == SDL_HAT_DOWN)
					{
						keyUp(getMobileKey(SDLK_LEFT));
						keyUp(getMobileKey(SDLK_UP));
						keyUp(getMobileKey(SDLK_RIGHT));
						keyDown(getMobileKey(SDLK_DOWN));
					}
					if (event.jhat.value == SDL_HAT_RIGHTDOWN)
					{
						keyUp(getMobileKey(SDLK_LEFT));
						keyUp(getMobileKey(SDLK_UP));
						keyDown(getMobileKey(SDLK_RIGHT));
						keyDown(getMobileKey(SDLK_DOWN));
					}
					if (event.jhat.value == SDL_HAT_RIGHT)
					{
						keyUp(getMobileKey(SDLK_LEFT));
						keyUp(getMobileKey(SDLK_UP));
						keyDown(getMobileKey(SDLK_RIGHT));
						keyUp(getMobileKey(SDLK_DOWN));
					}
				}
				
				// else if(event.type == SDL_JOYAXISMOTION) 
				// {
				//	 // jaxis.value => -32768 to 32767
				//	 int normValue;
				//	 if(abs(event.jaxis.value) <= AXIS_DEADZONE) { normValue = 0; }
				//	 else 
				//	 {
				//		 if(event.jaxis.value > 0) { normValue = 1; }
				//		 else { normValue = -1; }
				//	 }
				// 
				//	 if(abs(normValue) != abs(mPrevAxisValues[event.jaxis.which][event.jaxis.axis]))
				//	 {
				//		 key = 3 * event.jaxis.axis + normValue + 1;
				// 
				//		 // If the axis is centered, send the last command but in the "keyUp" event to prevent it from being stuck in "keyDown" mode
				//		 if(normValue == 0) { sendKey(findInputMappedFunction(lastAxisKey<<8,  JOYPAD_AXIS_COMMAND), normValue != 0, true, false); }
				//		 else 
				//		 {
				//			 lastAxisKey = key;
				//			 sendKey(findInputMappedFunction(key<<8,  JOYPAD_AXIS_COMMAND), normValue != 0, true, false);
				//		 }
				// 
				//		 //printf("JoyAxis:%d. Centered:%s | cast:%s\n", key<<8, normValue == 0 ? "true" : "false", keynames[findInputMappedFunction(key<<8,  JOYPAD_AXIS_COMMAND)]);
				//	 }
				//	 mPrevAxisValues[event.jaxis.which][event.jaxis.axis] = normValue;
				// }
				
				else if(event.type == SDL_JOYDEVICEADDED) { addJoystick(event.jdevice.which); }
				
				else if(event.type == SDL_JOYDEVICEREMOVED) { removeJoystick(event.jdevice.which); }
				
				// Mouse keys (any mouse button click is valid)
				else if(event.type == SDL_MOUSEBUTTONDOWN) 
				{
					// Capture mouse button click to send to anbu.java	
					// calculateCorrectedMousePos(&event);
				
					mousePressed = true;
					mouseX = event.button.x;
					mouseY = event.button.y;
					
					Mobile.getPlatform().pointerPressed(event.button.x, event.button.y);

					//printf("\npress coords-> X: %d | Y: %d", correctedMouseX, correctedMouseY);
				}
				else if(event.type == SDL_MOUSEBUTTONUP) 
				{
					// Capture mouse button release to send to anbu.java
					// calculateCorrectedMousePos(&event);
				
					if(mousePressed) 
					{ 
						mousePressed = false;
						mouseDragged = false;
						Mobile.getPlatform().pointerReleased(event.button.x, event.button.y);
					}
				}
				else if(event.type == SDL_MOUSEMOTION) 
				{
					// Check if a drag event is ocurring
					if(mousePressed && (Math.abs(event.button.x - mouseX) * Math.abs(event.button.y - mouseY)) > dragThreshold)
					{ 
						mouseDragged = true;
						mouseX = event.button.x;
						mouseY = event.button.y;
						// calculateCorrectedMousePos(&event);
				
						//printf("\ndrag coords-> X: %d | Y: %d", correctedMouseX, correctedMouseY);
						Mobile.getPlatform().pointerDragged(mouseX, mouseY);
					}
				}
			}
		}

		private void keyDown(int key)
		{
			if (key == 0)
			{
				return;
			}
			
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
			if (key == 0)
			{
				return;
			}

			int mobikeyN = (key + 64) & 0x7F; //Normalized value for indexing the pressedKeys array
			if (pressedKeys[mobikeyN] == true)
			{
				Mobile.getPlatform().keyReleased(key);					
			}
			pressedKeys[mobikeyN] = false;
		}

		private int getMobileKey(int keycode)
		{		
			if(keycode == SDLK_KP_1) return Mobile.KEY_NUM7; // B
			if(keycode == SDLK_KP_3) return Mobile.KEY_NUM9; // X
			if(keycode == SDLK_X) return Mobile.KEY_POUND; // Y

			
			if(keycode == SDLK_C) return Mobile.KEY_NUM0; // Home
			

			if(keycode == SDLK_A) return Mobile.GAME_A; // Left Analog press
			if(keycode == SDLK_S) return Mobile.GAME_B; // Right Analog press

			if(keycode == SDLK_KP_7) return Mobile.KEY_NUM1; // L
			if(keycode == SDLK_KP_9) return Mobile.KEY_NUM3; // R

			// These keys are overridden by the "useXControls" variables
			if(useNokiaControls) 
			{
				if(keycode == SDLK_KP_5) { return Mobile.NOKIA_SOFT3; } // A
				if(keycode == SDLK_RETURN) { return Mobile.NOKIA_SOFT1; } // -/Select
				if(keycode == SDLK_BACKSPACE) { return Mobile.NOKIA_SOFT2; } // +/Start
				if(keycode == SDLK_UP) { return Mobile.NOKIA_UP; }	// D-Pad Up
				if(keycode == SDLK_DOWN) { return Mobile.NOKIA_DOWN; }  // D-Pad Down
				if(keycode == SDLK_LEFT) { return Mobile.NOKIA_LEFT; }  // D-Pad Left
				if(keycode == SDLK_RIGHT) { return Mobile.NOKIA_RIGHT; } // D-Pad Right
			}
			else if(useSiemensControls) 
			{
				if(keycode == SDLK_KP_5) { return Mobile.SIEMENS_FIRE; }
				if(keycode == SDLK_RETURN) { return Mobile.SIEMENS_SOFT1; }
				if(keycode == SDLK_BACKSPACE) { return Mobile.SIEMENS_SOFT2; }
				if(keycode == SDLK_UP) { return Mobile.SIEMENS_UP; }
				if(keycode == SDLK_DOWN) { return Mobile.SIEMENS_DOWN; }
				if(keycode == SDLK_LEFT) { return Mobile.SIEMENS_LEFT; }
				if(keycode == SDLK_RIGHT) { return Mobile.SIEMENS_RIGHT; }
			}
			else if(useMotorolaControls) 
			{
				if(keycode == SDLK_KP_5) { return Mobile.MOTOROLA_FIRE; }
				if(keycode == SDLK_RETURN) { return Mobile.MOTOROLA_SOFT1; }
				if(keycode == SDLK_BACKSPACE) { return Mobile.MOTOROLA_SOFT2; }
				if(keycode == SDLK_UP) { return Mobile.MOTOROLA_UP; }
				if(keycode == SDLK_DOWN) { return Mobile.MOTOROLA_DOWN; }
				if(keycode == SDLK_LEFT) { return Mobile.MOTOROLA_LEFT; }
				if(keycode == SDLK_RIGHT) { return Mobile.MOTOROLA_RIGHT; }
			}
			else // Standard keycodes
			{
				if(keycode == SDLK_KP_5) { return Mobile.KEY_NUM5; }
				if(keycode == SDLK_RETURN) { return Mobile.NOKIA_SOFT1; }
				if(keycode == SDLK_BACKSPACE) { return Mobile.NOKIA_SOFT2; }
				if(keycode == SDLK_UP) { return Mobile.KEY_NUM2; }
				if(keycode == SDLK_DOWN) { return Mobile.KEY_NUM8; }
				if(keycode == SDLK_LEFT) { return Mobile.KEY_NUM4; }
				if(keycode == SDLK_RIGHT) { return Mobile.KEY_NUM6; }
			}	

			return 0;
		}

		private int getMobileKeyFromButton(int button)
		{
			if(button == 0x00) { return Mobile.KEY_NUM5; } // A - See below			
			if(button == 0x01) return Mobile.KEY_NUM7; // B
			if(button == 0x02) return Mobile.KEY_NUM9; // X
			if(button == 0x03) return Mobile.KEY_POUND; // Y
			if(button == 0x05) return Mobile.KEY_NUM0; // Home
			if(button == 0x07) return Mobile.GAME_A; // Left Analog press
			if(button == 0x08) return Mobile.GAME_B; // Right Analog press
			if(button == 0x09) return Mobile.KEY_NUM1; // L
			if(button == 0x0A) return Mobile.KEY_NUM3; // R

			if(button == 0x0F) return Mobile.GAME_C; // Screenshot, shouldn't really be used here

			// These keys are overridden by the "useXControls" variables
			if(useNokiaControls) 
			{
				if(button == 0x00) { return Mobile.NOKIA_SOFT3; } // A
				if(button == 0x04) { return Mobile.NOKIA_SOFT1; } // -/Select
				if(button == 0x06) { return Mobile.NOKIA_SOFT2; } // +/Start
				if(button == 0x0B) { return Mobile.NOKIA_UP; }	// D-Pad Up
				if(button == 0x0C) { return Mobile.NOKIA_DOWN; }  // D-Pad Down
				if(button == 0x0D) { return Mobile.NOKIA_LEFT; }  // D-Pad Left
				if(button == 0x0E) { return Mobile.NOKIA_RIGHT; } // D-Pad Right
			}
			else if(useSiemensControls) 
			{
				if(button == 0x00) { return Mobile.SIEMENS_FIRE; }
				if(button == 0x04) { return Mobile.SIEMENS_SOFT1; }
				if(button == 0x06) { return Mobile.SIEMENS_SOFT2; }
				if(button == 0x0B) { return Mobile.SIEMENS_UP; }
				if(button == 0x0C) { return Mobile.SIEMENS_DOWN; }
				if(button == 0x0D) { return Mobile.SIEMENS_LEFT; }
				if(button == 0x0E) { return Mobile.SIEMENS_RIGHT; }
			}
			else if(useMotorolaControls) 
			{
				if(button == 0x00) { return Mobile.MOTOROLA_FIRE; }
				if(button == 0x04) { return Mobile.MOTOROLA_SOFT1; }
				if(button == 0x06) { return Mobile.MOTOROLA_SOFT2; }
				if(button == 0x0B) { return Mobile.MOTOROLA_UP; }
				if(button == 0x0C) { return Mobile.MOTOROLA_DOWN; }
				if(button == 0x0D) { return Mobile.MOTOROLA_LEFT; }
				if(button == 0x0E) { return Mobile.MOTOROLA_RIGHT; }
			}
			else // Standard keycodes
			{
				if(button == 0x00) { return Mobile.KEY_NUM5; }
				if(button == 0x04) { return Mobile.NOKIA_SOFT1; }
				if(button == 0x06) { return Mobile.NOKIA_SOFT2; }
				if(button == 0x0B) { return Mobile.KEY_NUM2; }
				if(button == 0x0C) { return Mobile.KEY_NUM8; }
				if(button == 0x0D) { return Mobile.KEY_NUM4; }
				if(button == 0x0E) { return Mobile.KEY_NUM6; }
			}
			
			return 0;
		}
		
		private void addJoystick(int id)
		{
			// assert(id >= 0 && id < SDL_NumJoysticks());

			// open joystick & add to our list
			//System.out.println("Joystick Detected!" + SDL_JoystickName(joy));
			joy = SDL_JoystickOpen(id);
			//System.out.println("Added Joystick:" + SDL_JoystickName(joy));
			// assert(joy);

			// add it to our list so we can close it again later
			// SDL_JoystickID joyId = SDL_JoystickInstanceID(joy);
			// mJoysticks[joyId] = joy;

			// set up the prevAxisValues
			// int numAxes = SDL_JoystickNumAxes(joy);
			// mPrevAxisValues[joyId] = new int[numAxes];
			// std::fill(mPrevAxisValues[joyId], mPrevAxisValues[joyId] + numAxes, 0);
		}

		private void removeJoystick(int joyId)
		{
			//System.out.println("Removing Joystick:" + SDL_JoystickName(joy));
			SDL_JoystickClose(joy);
			//System.out.println("Joystick Removed!" + SDL_JoystickName(joy));
			// assert(joyId != -1);
			// delete old prevAxisValues
			// auto axisIt = mPrevAxisValues.find(joyId);
			// delete[] axisIt->second;
			// mPrevAxisValues.erase(axisIt);
			// 
			// // close the joystick
			// auto joyIt = mJoysticks.find(joyId);
			// if(joyIt != mJoysticks.end())
			// {
			// 	SDL_JoystickClose(joyIt->second);
			// 	mJoysticks.erase(joyIt);
			// }
		}

		void toggleFullscreen() 
		{
			if(isFullscreen){ SDL_SetWindowFullscreen(window, SDL_WINDOW_FULLSCREEN_DESKTOP); }
			else
			{ 
				SDL_SetWindowFullscreen(window, SDL_WINDOW_SHOWN);
				resolutionChanged = true;
			}
		}

		/* 
		 * Whenever FreeJ2ME updates its current displayable, or the user resizes the screen, this must be called to
		 * to update the renderer and make sure SDL will render to the new window size correctly.
		 */
		private void updateScreen() 
		{
			SDL_SetWindowSize(window, lcdWidth*scaleFactor , lcdHeight*scaleFactor);
			SDL_DestroyRenderer(renderer);
			renderer = SDL_CreateRenderer(window, -1, 0);
			texture = SDL_CreateTexture(renderer, SDL_PIXELFORMAT_RGB888, SDL_TEXTUREACCESS_STREAMING, lcdWidth, lcdHeight);
			pixels = new Memory(lcdWidth * lcdHeight * 4); 
			resolutionChanged = false;
			Mobile.displayUpdated = false;
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
	}
}
