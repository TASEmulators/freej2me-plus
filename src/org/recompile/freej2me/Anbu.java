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

import org.recompile.mobile.Mobile;
import org.recompile.mobile.MobilePlatform;

import io.github.libsdl4j.api.event.SDL_Event;
import io.github.libsdl4j.api.surface.SDL_Surface;
import io.github.libsdl4j.api.video.SDL_Window;
import io.github.libsdl4j.api.joystick.SDL_Joystick;

import static io.github.libsdl4j.api.Sdl.SDL_Init;
import static io.github.libsdl4j.api.SdlSubSystemConst.SDL_INIT_JOYSTICK;
import static io.github.libsdl4j.api.SdlSubSystemConst.SDL_INIT_VIDEO;

import static io.github.libsdl4j.api.render.SdlRender.SDL_CreateRenderer;
import static io.github.libsdl4j.api.render.SdlRender.SDL_RenderPresent;
import static io.github.libsdl4j.api.render.SdlRender.SDL_RenderCopy;
import static io.github.libsdl4j.api.render.SdlRender.SDL_UpdateTexture;
import static io.github.libsdl4j.api.render.SDL_TextureAccess.SDL_TEXTUREACCESS_STREAMING;
import static io.github.libsdl4j.api.render.SdlRender.SDL_CreateTexture;
import static io.github.libsdl4j.api.render.SdlRender.SDL_DestroyRenderer;
import static io.github.libsdl4j.api.render.SdlRender.SDL_DestroyTexture;
import static io.github.libsdl4j.api.surface.SdlSurface.SDL_BlitScaled;
import static io.github.libsdl4j.api.surface.SdlSurface.SDL_CreateRGBSurface;
import static io.github.libsdl4j.api.surface.SdlSurface.SDL_FreeSurface;

import static io.github.libsdl4j.api.video.SdlVideo.SDL_SetWindowSize;
import static io.github.libsdl4j.api.video.SdlVideo.SDL_SetWindowFullscreen;
import static io.github.libsdl4j.api.video.SdlVideo.SDL_CreateWindow;
import static io.github.libsdl4j.api.video.SdlVideo.SDL_GetWindowSurface;
import static io.github.libsdl4j.api.video.SdlVideo.SDL_UpdateWindowSurface;
import static io.github.libsdl4j.api.video.SdlVideoConst.SDL_WINDOWPOS_CENTERED;
import static io.github.libsdl4j.api.video.SDL_WindowFlags.SDL_WINDOW_FULLSCREEN_DESKTOP;
import static io.github.libsdl4j.api.video.SDL_WindowFlags.SDL_WINDOW_SHOWN;
import static io.github.libsdl4j.api.pixels.SDL_PixelFormatEnum.SDL_PIXELFORMAT_RGB888;

import static io.github.libsdl4j.api.event.SdlEvents.SDL_PollEvent;
import static io.github.libsdl4j.api.event.SdlEventsConst.SDL_ENABLE;
import static io.github.libsdl4j.api.event.SdlEventsConst.SDL_PRESSED;
import static io.github.libsdl4j.api.event.SDL_EventType.SDL_JOYDEVICEADDED;
import static io.github.libsdl4j.api.event.SDL_EventType.SDL_JOYDEVICEREMOVED;
import static io.github.libsdl4j.api.event.SDL_EventType.SDL_JOYBUTTONDOWN;
import static io.github.libsdl4j.api.event.SDL_EventType.SDL_JOYBUTTONUP;
import static io.github.libsdl4j.api.event.SDL_EventType.SDL_JOYHATMOTION;
import static io.github.libsdl4j.api.event.SDL_EventType.SDL_KEYDOWN;
import static io.github.libsdl4j.api.event.SDL_EventType.SDL_KEYUP;
import static io.github.libsdl4j.api.event.SDL_EventType.SDL_MOUSEBUTTONDOWN;
import static io.github.libsdl4j.api.event.SDL_EventType.SDL_MOUSEBUTTONUP;
import static io.github.libsdl4j.api.event.SDL_EventType.SDL_MOUSEMOTION;
import static io.github.libsdl4j.api.event.SDL_EventType.SDL_QUIT;

import static io.github.libsdl4j.api.joystick.SdlJoystick.SDL_JoystickOpen;
import static io.github.libsdl4j.api.joystick.SdlJoystick.SDL_JoystickEventState;
import static io.github.libsdl4j.api.joystick.SdlJoystickConst.SDL_HAT_DOWN;
import static io.github.libsdl4j.api.joystick.SdlJoystickConst.SDL_HAT_LEFTDOWN;
import static io.github.libsdl4j.api.joystick.SdlJoystickConst.SDL_HAT_RIGHTDOWN;
import static io.github.libsdl4j.api.joystick.SdlJoystickConst.SDL_HAT_UP;
import static io.github.libsdl4j.api.joystick.SdlJoystickConst.SDL_HAT_LEFTUP;
import static io.github.libsdl4j.api.joystick.SdlJoystickConst.SDL_HAT_RIGHTUP;
import static io.github.libsdl4j.api.joystick.SdlJoystickConst.SDL_HAT_LEFT;
import static io.github.libsdl4j.api.joystick.SdlJoystickConst.SDL_HAT_RIGHT;
import static io.github.libsdl4j.api.joystick.SdlJoystickConst.SDL_HAT_CENTERED;

import static io.github.libsdl4j.api.joystick.SdlJoystick.SDL_JoystickClose;
import static io.github.libsdl4j.api.joystick.SdlJoystick.SDL_JoystickName;
import static io.github.libsdl4j.api.joystick.SdlJoystick.SDL_JoystickRumble;

import static io.github.libsdl4j.api.keycode.SDL_Keycode.*; // We can import all keyboard keycodes here


import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.media.Manager;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
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
	private int[] lcdData;
	private int scaleFactor = 1;
	private boolean isFullscreen = false;
	private boolean SDLInitialized = false;


	// On keyboard, SDL follows AWT's convention
	int inputKeycodes[] = new int[] { 
		SDLK_Q, SDLK_W, 
		SDLK_UP, SDLK_LEFT, SDLK_RETURN, SDLK_RIGHT, SDLK_DOWN, 
		SDLK_KP_7, SDLK_KP_8, SDLK_KP_9, 
		SDLK_KP_4, SDLK_KP_5, SDLK_KP_6, 
		SDLK_KP_1, SDLK_KP_2, SDLK_KP_3, 
		SDLK_E, SDLK_KP_0, SDLK_R
	};

	private static final int[] joypadKeycodes = {0x00, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0xFA, 0xFB, 0xFC, 0xFD};

	SDL_Joystick joy = null;

	public Anbu(String args[])
	{
		Mobile.clearOldLog();
		lcdWidth = Mobile.lcdWidth;
		lcdHeight = Mobile.lcdHeight;

		String file = null;

		if(args.length>=1)
		{
			try { file = getFormattedLocation(URLDecoder.decode(args[0], Mobile.textEncoding)); }
			catch(Exception e) { }
			Mobile.log(Mobile.LOG_DEBUG, Anbu.class.getPackage().getName() + "." + Anbu.class.getSimpleName() + ": " + file);
		}
		if(args.length>=3)
		{
		 	lcdWidth = Integer.parseInt(args[1]);
			lcdHeight = Integer.parseInt(args[2]);
		}
		if(args.length>=4) { scaleFactor = Integer.parseInt(args[3]); }

		Mobile.setPlatform(new MobilePlatform(lcdWidth, lcdHeight), new Runnable() { public void run() { settingsChanged(); } });
		lcdData = ((DataBufferInt) Mobile.getPlatform().getLcdFrontbufferImage().getRaster().getDataBuffer()).getData();

		/* TODO: Anbu/SDL has no way of enabling any settings outside of cmd args yet, a UI and code overhaul might be in order */

		// Set painter right before the jar is loaded
		Mobile.getPlatform().setPainter(new Runnable()
		{
			public synchronized void run()
			{
				if(!SDLInitialized) { return; } // Don't do anything while SDL isn't initialized
				try
				{
					/* Check if vibration commands have to be handled */
					if(Mobile.vibrationDuration != 0) 
					{
						int vib = SDL_JoystickRumble(joy, (short) (Mobile.vibrationStrength & 0xFFFF), (short) (Mobile.vibrationStrength & 0xFFFF), Mobile.vibrationDuration);
						Mobile.vibrationDuration = 0;
					}
					
					sdl.paint();
				}
				catch (Exception e) { }
			}
		});

		if(file != null && Mobile.getPlatform().load(file))
		{
			// Check config

			/* Allows FreeJ2ME to set the width and height passed as cmd arguments. */
			if(args.length>=3)
			{
				lcdWidth = Integer.parseInt(args[1]);
				lcdHeight = Integer.parseInt(args[2]);
				Mobile.config.settings.put("width",  ""+lcdWidth);
				Mobile.config.settings.put("height", ""+lcdHeight);
			}

			Mobile.config.saveConfig();
			settingsChanged();

			// Run jar
			Mobile.getPlatform().runJar();

			// Start SDL
			sdl = new SDL();
			sdl.start(args);
		}
		else
		{
			Mobile.log(Mobile.LOG_ERROR, Anbu.class.getPackage().getName() + "." + Anbu.class.getSimpleName() + ": " + "Couldn't load jar...");
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
			Mobile.log(Mobile.LOG_ERROR, Anbu.class.getPackage().getName() + "." + Anbu.class.getSimpleName() + ": " + "File not found...");
			System.exit(0);
		}

		return file.toURI().toString();
	}

	private class SDL
	{
		protected SDL_Window window;
		protected SDL_Surface surface;
		protected SDL_Surface lcdSurface;

		private int mouseX;
		private int mouseY;
		private boolean mousePressed = false;
		private boolean mouseDragged = false;
		private int dragThreshold = 2; // threshold in pixels

		private boolean resolutionChanged = false;

		private Timer keytimer;
		private TimerTask keytask;

		public synchronized void start(String args[])
		{
			if (SDL_Init(SDL_INIT_VIDEO | SDL_INIT_JOYSTICK) < 0)
			{
				Mobile.log(Mobile.LOG_ERROR, Anbu.class.getPackage().getName() + "." + Anbu.class.getSimpleName() + ": " + "Unable to initialize SDL");
				stop();
			}

			window = SDL_CreateWindow("FreeJ2ME-Plus - SDL", SDL_WINDOWPOS_CENTERED, SDL_WINDOWPOS_CENTERED, lcdWidth*scaleFactor, lcdHeight*scaleFactor, SDL_WINDOW_SHOWN);
			SDL_JoystickEventState(SDL_ENABLE);

			lcdSurface = SDL_CreateRGBSurface(0, lcdWidth, lcdHeight, 32, 0, 0, 0, 0);

			SDLInitialized = true;
		}

		public void stop() { System.exit(0); }

		public void paint()
		{
			/* 
			 * Let's make resolution changes and adjust any relevant objects here, as it's right on the render path 
			 * and it makes sure that the objects will be set correctly before being rendered.
			 */
			if(resolutionChanged) { updateScreen(); }

			// Scale the surface to the windows' size, and blit directly to it. Using a renderer should be faster, but using it incurs a heavy penalty here somehow
			surface = SDL_GetWindowSurface(window);
			if (surface != null)
			{
				lcdSurface.getPixels().write(0, lcdData, 0, lcdData.length);

				SDL_BlitScaled(lcdSurface, null, surface, null);

				SDL_UpdateWindowSurface(window);
			}

			/* 
			 * Normally, input reading should not be tied to the render logic, because that would softlock jars that only
			 * send new render data after an input is registered (Ex: JBenchmark 2 and some other jars that use Form UI).
			 * But for determinism in libTAS, inputs must be pulled synchronously with rendering, because libTAS processes
			 * inputs (and a lot of other things) during frame boundaries, which is when the program calls its render function.
			 */
			processEvents();
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
					else if (key == SDLK_F11) // TODO: Doesn't respect aspect ratio
					{
						if(event.type == SDL_KEYDOWN) 
						{
							//isFullscreen = !isFullscreen;
							//toggleFullscreen();
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

				// TODO: Analog and trigger inputs.

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
					
					MobilePlatform.pointerPressed(mouseX, mouseY);

					//printf("\npress coords-> X: %d | Y: %d", correctedMouseX, correctedMouseY);
				}
				else if(event.type == SDL_MOUSEBUTTONUP) 
				{
					// Capture mouse button release to send to anbu.java
					// calculateCorrectedMousePos(&event);
					mouseX = event.button.x;
					mouseY = event.button.y;
				
					if(mousePressed) 
					{ 
						mousePressed = false;
						mouseDragged = false;
						MobilePlatform.pointerReleased(mouseX, mouseY);
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
						MobilePlatform.pointerDragged(mouseX, mouseY);
					}
				}
			}
		}

		private void keyDown(int key)
		{
			if (key == Integer.MIN_VALUE) { return; }

			if(MobilePlatform.pressedKeys[key] == true) { MobilePlatform.keyRepeated(Mobile.getMobileKey(key)); }
			else 
			{
				MobilePlatform.pressedKeys[key] = true;
				MobilePlatform.keyPressed(Mobile.getMobileKey(key));
			}
			
		}

		private void keyUp(int key)
		{
			if (key == Integer.MIN_VALUE) { return; }

			MobilePlatform.pressedKeys[key] = false;
			MobilePlatform.keyReleased(Mobile.getMobileKey(key));

			for(int i = 0; i < MobilePlatform.pressedKeys.length; i++) 
			{
				if(MobilePlatform.pressedKeys[i]) { MobilePlatform.keyRepeated(Mobile.getMobileKey(i)); }
			}
		}

		private int getMobileKey(int keycode)
		{
			for(int i = 0; i < inputKeycodes.length; i++) 
			{
				if(keycode == inputKeycodes[i]) { return Mobile.convertAWTKeycode(i);}
			}
			return Integer.MIN_VALUE;
		}

		private int getMobileKeyFromButton(int button)
		{
			for(int i = 0; i < joypadKeycodes.length; i++) 
			{
				if(button == joypadKeycodes[i]) { return Mobile.convertSDLKeycode(i);}
			}

			return Integer.MIN_VALUE;
		}
		
		private void addJoystick(int id)
		{
			// assert(id >= 0 && id < SDL_NumJoysticks());

			// open joystick & add to our list
			joy = SDL_JoystickOpen(id);
			Mobile.log(Mobile.LOG_DEBUG, Anbu.class.getPackage().getName() + "." + Anbu.class.getSimpleName() + ": " + "Added Joystick:" + SDL_JoystickName(joy));
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
			SDL_JoystickClose(joy);
			Mobile.log(Mobile.LOG_DEBUG, Anbu.class.getPackage().getName() + "." + Anbu.class.getSimpleName() + ": " + "Joystick Removed!" + SDL_JoystickName(joy));
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
		private synchronized void updateScreen()
		{
			SDL_SetWindowSize(window, lcdWidth*scaleFactor, lcdHeight*scaleFactor);
			SDL_FreeSurface(lcdSurface);
			lcdSurface = SDL_CreateRGBSurface(0, lcdWidth, lcdHeight, 32, 0, 0, 0, 0);
			resolutionChanged = false;
		}

	} // sdl

	void settingsChanged() 
	{
		boolean hasRotated = Mobile.updateSettings();

		if(Mobile.lcdWidth != lcdWidth || Mobile.lcdHeight != lcdHeight || hasRotated) 
		{
			Mobile.getPlatform().resizeLCD(Mobile.lcdWidth, Mobile.lcdHeight);
			lcdData = ((DataBufferInt) Mobile.getPlatform().getLcdFrontbufferImage().getRaster().getDataBuffer()).getData();
			if(!Mobile.rotateDisplay) 
			{
				lcdWidth = Mobile.lcdWidth;
				lcdHeight = Mobile.lcdHeight;
			}
			else 
			{
				lcdWidth = Mobile.lcdHeight;
				lcdHeight = Mobile.lcdWidth;
			}
			sdl.resolutionChanged = true;
		}

	}
}
