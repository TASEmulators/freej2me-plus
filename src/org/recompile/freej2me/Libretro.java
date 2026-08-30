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

import java.util.Timer;
import java.util.TimerTask;

import java.io.File;
import java.net.URLDecoder;

public class Libretro
{
	private int lcdWidth;
	private int lcdHeight;
	int[] lcdData;

	private boolean soundEnabled = true;
	private static volatile boolean canPause = false;

	private static final long PAUSE_DELAY_MS = 250;
	private static volatile long lastCoreUpdateTime = System.currentTimeMillis(); // Tracks last core update for pause checks

	private byte[] frameBuffer = new byte[800*800*3];
	private final byte[] frameHeader = new byte[]{(byte)0xFE,
		0, 0, 0, 0, // Display data
		0,          // Rotation enabled
		0, 0, 0, 0, // Vibration duration
		0, 0, 0, 0, // Vibration Strength
		0, 0};      // Restart requested, and encoding requested

	private int mousex;
	private int mousey;

	/*
	 * StringBuilder used to get the updated configs from the libretro core
	 * String[] used to tokenize each setting as its own string.
	 */
	String[] cfgtokens;

	LibretroIO lio;

	public static void main(String args[])
	{
		Mobile.clearOldLog();
		Libretro app = new Libretro(args);
	}

	public Libretro(String args[])
	{
		lcdWidth  = Mobile.lcdWidth;
		lcdHeight = Mobile.lcdHeight;

		/*
		 * Notify the MIDlet class that this version of FreeJ2ME is for Libretro, which disables
		 * the ability to close the jar when a J2ME app requests an exit as this can cause segmentation
		 * faults on libretro frontends and also close the unexpectedly.
		*/
		Mobile.getPlatform().isLibretro = true;

		/*
		 * Checks if the arguments were received from the commandline -> width, height, rotate, phonetype, fps, sound, ...
		 *
		 * NOTE:
		 * Due to differences in how linux and win32 pass their cmd arguments, we can't explictly check for a given size
		 * on the argv array. Linux includes the "java", "-jar" and "path/to/freej2me" into the array while WIN32 doesn't.
		 */
		lcdWidth =  Integer.parseInt(args[0]);
		lcdHeight = Integer.parseInt(args[1]);

		Mobile.rotateDisplay = Integer.parseInt(args[2]) * 90;

		Mobile.kddi = false;
		Mobile.lg = false;
		Mobile.motorola = false;
		Mobile.motoTriplets = false;
		Mobile.motoV8 = false;
		Mobile.motoA1000 = false;
		Mobile.nokiaKeyboard = false;
		Mobile.sagem = false;
		Mobile.siemens = false;
		Mobile.skt = false;

		if(Integer.parseInt(args[3]) == 1)       { Mobile.lg = true;    }
		else if(Integer.parseInt(args[3]) == 2)  { Mobile.motorola = true;  }
		else if(Integer.parseInt(args[3]) == 3)  { Mobile.motoTriplets = true; }
		else if(Integer.parseInt(args[3]) == 4)  { Mobile.motoV8 = true; }
		else if(Integer.parseInt(args[3]) == 5)  { Mobile.motoA1000 = true; }
		else if(Integer.parseInt(args[3]) == 6)  { Mobile.nokiaKeyboard = true; }
		else if(Integer.parseInt(args[3]) == 7)  { Mobile.sagem = true; }
		else if(Integer.parseInt(args[3]) == 8)  { Mobile.siemens = true; }
		else if(Integer.parseInt(args[3]) == 9)  { Mobile.skt = true; }
		else if(Integer.parseInt(args[3]) == 10) { Mobile.kddi = true; }

		Mobile.limitFPS = Integer.parseInt(args[4]);

		soundEnabled = Integer.parseInt(args[5]) != 0;

		Mobile.useCustomMidi = Integer.parseInt(args[6]) != 0;

		/* Dump Audio Streams will not be a per-game FreeJ2ME config, so it will have to be set every time for now */
		Mobile.dumpAudioStreams = Integer.parseInt(args[7]) != 0;

		/* Same for Logging Level */
		Mobile.minLogLevel = (byte) (Integer.parseInt(args[8]));

		/* No Alpha on Blank Images SpeedHack is a per-game config */
		Mobile.noAlphaOnBlankImages = Integer.parseInt(args[9]) != 0;

		/* LCD Backlight Mask color index. */
		Mobile.maskIndex = Integer.parseInt(args[10]);

		/* Compat setting to fix Fantasy Zone 176x208 weird mirroring */
		Mobile.compatFantasyZoneFix = Integer.parseInt(args[11]) != 0;

		/* Compat setting to translate back to the origin whenever graphics object is reset */
		Mobile.compatTranslateToOriginOnReset = Integer.parseInt(args[12]) != 0;

		// Custom font and size
		Mobile.useCustomTextFont = Integer.parseInt(args[13]) != 0;

		Mobile.fontSizeOffset = (byte) Integer.parseInt(args[14]);

		// Unused for now
		Mobile.dumpGraphicsObjects = Integer.parseInt(args[15]) != 0;

		// Dump KJX extracted JAR and JAD
		Mobile.deleteTemporaryKJXFiles = Integer.parseInt(args[16]) != 0;

		// M3G Render only untextured polygons
		Mobile.M3GRenderUntexturedPolygons = Integer.parseInt(args[17]) != 0;

		// M3G Render Wireframe
		Mobile.M3GRenderWireframe = Integer.parseInt(args[18]) != 0;

		/* Framerate Unlock. */
		Mobile.unlockFramerateHack = (byte) Integer.parseInt(args[19]);

		/* Compat setting to process repaints immediately */
		Mobile.compatImmediateRepaints = Integer.parseInt(args[20]) != 0;

		/* Compat setting to override mobile platform checks */
		Mobile.compatOverridePlatformChecks = Integer.parseInt(args[21]) != 0;

		/* Compat setting to translate drawing methods in a siemens-friendly way */
		Mobile.compatSiemensFriendlyDrawing = Integer.parseInt(args[22]) != 0;

		/* Half-Res M3G Rendering SpeedHack is a per-game config */
		Mobile.halfResM3GRaster = Integer.parseInt(args[23]) != 0;

		/* DoJa API Version */
		Mobile.DoJaVersion = Integer.parseInt(args[24]);

		/* Compat setting to ignore volume changes */
		Mobile.compatIgnoreVolumeChanges = Integer.parseInt(args[25]) != 0;

		/* MascotCapsuleV3 Half Res rendering speedhack */
		Mobile.halfResMCV3Raster = Integer.parseInt(args[26]) != 0;

		/* MascotCapsuleV3 no Lighting speedhack */
		Mobile.MCV3NoLighting = Integer.parseInt(args[27]) != 0;

		/* Compat setting to fix Horizontal FOV for MascotCapsuleV3 */
		Mobile.compatMCV3HorizontalFovFix = Integer.parseInt(args[28]) != 0;

		/* MascotCapsuleV3 Show Heap debug setting */
		Mobile.MCV3ShowHeapUsage = Integer.parseInt(args[29]) != 0;

		/* MascotCapsuleV3 Show Heap debug setting */
		Mobile.MCV3ShowTimeMetrics = Integer.parseInt(args[30]) != 0;

		/* M3G Anti Aliasing Mode */
		Mobile.m3gAntiAliasingMode = Integer.parseInt(args[31]);

		/* M3G Bilinear Filter Mode */
		Mobile.m3gBilinearFilterMode = Integer.parseInt(args[32]);

		/* M3G Dithering Mode */
		Mobile.m3gDitheringMode = Integer.parseInt(args[33]);

		/* M3G Perspective Correction Mode */
		Mobile.m3gPerspectiveCorrectionMode = Integer.parseInt(args[34]);

		/* M3G Perspective Correction Subsample Factor */
		Mobile.m3gPerspCorrSubFactor = Integer.parseInt(args[35]);

		/* M3G Mipmapping Mode */
		Mobile.m3gMipmapMode = Integer.parseInt(args[36]);

		/* M3G Disable fog */
		Mobile.m3gDisableFog = Integer.parseInt(args[37]) != 0;


		/* Once it finishes parsing all arguments, it's time to set up freej2me-lr */

		Mobile.setPlatform(new MobilePlatform(lcdWidth, lcdHeight), new Runnable() { public void run() { settingsChanged(); } });
		lcdData = Mobile.getPlatform().getLcdFrontbuffer().getDataBuffer();

		// The painter here is only really used to check for frontend pauses
		Mobile.getPlatform().setPainter(new Runnable()
		{
			public void run()
			{
				updatePauseTimer();
			}
		});

		lio = new LibretroIO();

		lio.start();

		System.out.println("+READY");
		System.out.flush();
	}

	private class LibretroIO
	{
		private Timer keytimer;

		public void start()
		{
			keytimer = new Timer("Libretro-Timer");
			keytimer.schedule(new LibretroTimerTask(), 0, 1);
		}

		private class LibretroTimerTask extends TimerTask
		{
			private int bin;
			private int[] din = new int[5];
			private int count = 0;
			private int code;
			private byte[] buffer;
			private int bytesRead = 0;
			private String path;

			public void run()
			{
				try // to read keys
				{
					while(true)
					{
						bin = System.in.read(); // Blocks until there's data available
						if(bin==-1) { return; }

						//System.out.print(" "+bin);
						din[count] = (int)(bin & 0xFF);
						count++;

						/* Check inputs */
						if (count==5)
						{
							count = 0;
							code = (din[1]<<24) | (din[2]<<16) | (din[3]<<8) | din[4];
							switch(din[0])
							{
								//case 0: // keyboard key up (unused)
								//break;

								//case 1:	// keyboard key down (unused)
								//break;

								case 2:	// joypad key up
									MobilePlatform.pressedKeys[code] = false;
									MobilePlatform.keyReleased(Mobile.getMobileKey(code));
									for(int i = 0; i < MobilePlatform.pressedKeys.length; i++)
									{
										if(MobilePlatform.pressedKeys[i]) { MobilePlatform.keyRepeated(Mobile.getMobileKey(i)); }
									}
								break;

								case 3: // joypad key down
									MobilePlatform.pressedKeys[code] = true;
									MobilePlatform.keyPressed(Mobile.getMobileKey(code));
								break;

								case 4: // mouse up
									mousex = (din[1]<<8) | din[2];
									mousey = (din[3]<<8) | din[4];

									if(Mobile.rotateDisplay == 0)
									{
										MobilePlatform.pointerReleased(mousex, mousey);
									}
									if(Mobile.rotateDisplay == 90)
									{
										MobilePlatform.pointerReleased(mousey, lcdHeight - mousex);
									}
									if(Mobile.rotateDisplay == 180)
									{
										MobilePlatform.pointerReleased(lcdWidth - mousex, lcdHeight - mousey);
									}
									if(Mobile.rotateDisplay == 270)
									{
										MobilePlatform.pointerReleased(lcdWidth-mousey, mousex);
									}
								break;

								case 5: // mouse down
									mousex = (din[1]<<8) | din[2];
									mousey = (din[3]<<8) | din[4];

									if(Mobile.rotateDisplay == 0)
									{
										MobilePlatform.pointerPressed(mousex, mousey);
									}
									if(Mobile.rotateDisplay == 90)
									{
										MobilePlatform.pointerPressed(mousey, lcdHeight - mousex);
									}
									if(Mobile.rotateDisplay == 180)
									{
										MobilePlatform.pointerPressed(lcdWidth - mousex, lcdHeight - mousey);
									}
									if(Mobile.rotateDisplay == 270)
									{
										MobilePlatform.pointerPressed(lcdWidth-mousey, mousex);
									}
								break;

								case 6: // mouse drag
									mousex = (din[1]<<8) | din[2];
									mousey = (din[3]<<8) | din[4];

									if(Mobile.rotateDisplay == 0)
									{
										MobilePlatform.pointerDragged(mousex, mousey);
									}
									if(Mobile.rotateDisplay == 90)
									{
										MobilePlatform.pointerDragged(mousey, lcdHeight - mousex);
									}
									if(Mobile.rotateDisplay == 180)
									{
										MobilePlatform.pointerDragged(lcdWidth - mousex, lcdHeight - mousey);
									}
									if(Mobile.rotateDisplay == 270)
									{
										MobilePlatform.pointerDragged(lcdWidth-mousey, mousex);
									}
								break;

								case 10: // load jar
									buffer = new byte[code];
									bytesRead = System.in.read(buffer);

									path = new String(buffer, 0, bytesRead);

									if(Mobile.getPlatform().load(getFormattedLocation(URLDecoder.decode(path.toString(), Mobile.textEncoding))))
									{
										// Check config

										/* Override configs with the ones passed through commandline */
										Mobile.config.settings.put("scrwidth",  ""+lcdWidth);
										Mobile.config.settings.put("scrheight", ""+lcdHeight);

										Mobile.config.settings.put("rotate", "" + Mobile.rotateDisplay);

										if(Mobile.kddi)               { Mobile.config.settings.put("phone", "KDDI");    }
										else if(Mobile.lg)            { Mobile.config.settings.put("phone", "LG");    }
										else if(Mobile.motorola)      { Mobile.config.settings.put("phone", "Motorola");  }
										else if(Mobile.motoTriplets)  { Mobile.config.settings.put("phone", "MotoTriplets"); }
										else if(Mobile.motoV8)        { Mobile.config.settings.put("phone", "MotoV8"); }
										else if(Mobile.motoA1000)     { Mobile.config.settings.put("phone", "MotoA1000"); }
										else if(Mobile.nokiaKeyboard) { Mobile.config.settings.put("phone", "NokiaKeyboard"); }
										else if(Mobile.sagem)         { Mobile.config.settings.put("phone", "Sagem"); }
										else if(Mobile.siemens)       { Mobile.config.settings.put("phone", "Siemens"); }
										else if(Mobile.skt)           { Mobile.config.settings.put("phone", "SKT"); }
										else                          { Mobile.config.settings.put("phone", "Standard"); }

										Mobile.config.settings.put("sound", soundEnabled ? "on" : "off");

										Mobile.config.settings.put("fps", "" + Mobile.limitFPS);

										Mobile.config.settings.put("soundfont", Mobile.useCustomMidi ? "Custom" : "Default");

										Mobile.config.settings.put("spdhacknoalpha", Mobile.noAlphaOnBlankImages ? "on" : "off");
										Mobile.config.settings.put("spdhackmcv3halfres", Mobile.halfResMCV3Raster ? "on" : "off");
										Mobile.config.settings.put("spdhackmcv3nolighting", Mobile.MCV3NoLighting ? "on" : "off");

										if(Mobile.maskIndex == 0)      { Mobile.config.settings.put("backlightcolor", "Disabled"); }
										else if(Mobile.maskIndex == 1) { Mobile.config.settings.put("backlightcolor", "Green"); }
										else if(Mobile.maskIndex == 2) { Mobile.config.settings.put("backlightcolor", "Cyan"); }
										else if(Mobile.maskIndex == 3) { Mobile.config.settings.put("backlightcolor", "Orange"); }
										else if(Mobile.maskIndex == 4) { Mobile.config.settings.put("backlightcolor", "Violet"); }
										else if(Mobile.maskIndex == 5) { Mobile.config.settings.put("backlightcolor", "Red"); }

										Mobile.config.settings.put("compatfantasyzonefix", Mobile.compatFantasyZoneFix ? "on" : "off");
										Mobile.config.settings.put("compattranstooriginonreset", Mobile.compatTranslateToOriginOnReset ? "on" : "off");
										Mobile.config.settings.put("compatimmediaterepaints", Mobile.compatImmediateRepaints ? "on" : "off");
										Mobile.config.settings.put("compatoverrideplatchecks", Mobile.compatOverridePlatformChecks ? "on" : "off");
										Mobile.config.settings.put("compatsiemensfriendlydrawing", Mobile.compatSiemensFriendlyDrawing ? "on" : "off");
										Mobile.config.settings.put("compatignorevolumechanges", Mobile.compatIgnoreVolumeChanges ? "on" : "off");
										Mobile.config.settings.put("compatmcv3horizfovfix", Mobile.compatMCV3HorizontalFovFix ? "on" : "off");

										Mobile.config.settings.put("textfont", Mobile.useCustomTextFont ? "Custom" : "Default");
										Mobile.config.settings.put("fontoffset", "" + Mobile.fontSizeOffset);

										if(Mobile.unlockFramerateHack == 0)      { Mobile.config.settings.put("fpshack", "Default");  }
										else if(Mobile.unlockFramerateHack == 1) { Mobile.config.settings.put("fpshack", "Safe");  }
										else if(Mobile.unlockFramerateHack == 2) { Mobile.config.settings.put("fpshack", "Extended");  }
										else if(Mobile.unlockFramerateHack == 3) { Mobile.config.settings.put("fpshack", "Aggressive");  }

										Mobile.config.settings.put("dojaversion", "" + Mobile.DoJaVersion);

										Mobile.config.settings.put("spdhackm3ghalfres", Mobile.halfResM3GRaster ? "on" : "off");
										Mobile.config.settings.put("m3gdisablefog", Mobile.m3gDisableFog ? "on" : "off");

										if(Mobile.m3gAntiAliasingMode == 0) { Mobile.config.settings.put("m3gantialiasmode", "off");  }
										else if(Mobile.m3gAntiAliasingMode == 1) { Mobile.config.settings.put("m3gantialiasmode", "app");  }
										else if(Mobile.m3gAntiAliasingMode == 2) { Mobile.config.settings.put("m3gantialiasmode", "on");  }

										if(Mobile.m3gBilinearFilterMode == 0) { Mobile.config.settings.put("m3gbilinearmode", "off");  }
										else if(Mobile.m3gBilinearFilterMode == 1) { Mobile.config.settings.put("m3gbilinearmode", "app");  }
										else if(Mobile.m3gBilinearFilterMode == 2) { Mobile.config.settings.put("m3gbilinearmode", "on");  }

										if(Mobile.m3gDitheringMode == 0) { Mobile.config.settings.put("m3gditheringmode", "off");  }
										else if(Mobile.m3gDitheringMode == 1) { Mobile.config.settings.put("m3gditheringmode", "app");  }
										else if(Mobile.m3gDitheringMode == 2) { Mobile.config.settings.put("m3gditheringmode", "on");  }

										if(Mobile.m3gPerspectiveCorrectionMode == 0) { Mobile.config.settings.put("m3gperspcorrmode", "off");  }
										else if(Mobile.m3gPerspectiveCorrectionMode == 1) { Mobile.config.settings.put("m3gperspcorrmode", "app");  }
										else if(Mobile.m3gPerspectiveCorrectionMode == 2) { Mobile.config.settings.put("m3gperspcorrmode", "on");  }

										if(Mobile.m3gPerspCorrSubFactor == 3) { Mobile.config.settings.put("m3gperspcorrsubfactor", "extra");  }
										else if(Mobile.m3gPerspCorrSubFactor == 7) { Mobile.config.settings.put("m3gperspcorrsubfactor", "high");  }
										else if(Mobile.m3gPerspCorrSubFactor == 15) { Mobile.config.settings.put("m3gperspcorrsubfactor", "medium");  }
										else if(Mobile.m3gPerspCorrSubFactor == 31) { Mobile.config.settings.put("m3gperspcorrsubfactor", "low");  }

										if(Mobile.m3gMipmapMode == 3) { Mobile.config.settings.put("m3gmipmapmode", "linear");  }
										else if(Mobile.m3gMipmapMode == 2) { Mobile.config.settings.put("m3gmipmapmode", "nearest");  }
										else if(Mobile.m3gMipmapMode == 1) { Mobile.config.settings.put("m3gmipmapmode", "app");  }
										else if(Mobile.m3gMipmapMode == 0) { Mobile.config.settings.put("m3gmipmapmode", "off");  }

										// Update system settings

										Mobile.config.sysSettings.put("fpsCounterPosition", "Off"); // Libretro has its own frame counter

										Mobile.config.sysSettings.put("logLevel", "" + Mobile.minLogLevel);

										Mobile.config.sysSettings.put("M3GUntextured", Mobile.M3GRenderUntexturedPolygons ? "on" : "off");
										Mobile.config.sysSettings.put("M3GWireframe", Mobile.M3GRenderWireframe ? "on" : "off");

										Mobile.config.sysSettings.put("MCV3ShowHeapUsage", Mobile.MCV3ShowHeapUsage ? "on" : "off");
										Mobile.config.sysSettings.put("MCV3ShowTimeMetrics", Mobile.MCV3ShowTimeMetrics ? "on" : "off");

										Mobile.config.sysSettings.put("deleteTempKJXFiles", Mobile.deleteTemporaryKJXFiles ? "on" : "off");

										Mobile.config.sysSettings.put("dumpAudioStreams", Mobile.dumpAudioStreams ? "on" : "off");
										Mobile.config.sysSettings.put("dumpGraphicsObjects", Mobile.dumpGraphicsObjects ? "on" : "off");


										if(Mobile.libretroRestartRequested == 1)
										{
											frameHeader[14] = Mobile.libretroRestartRequested;
											frameHeader[15] = Mobile.libretroEncodingRequested;

											System.out.write(frameHeader, 0, 16);

											System.out.write(frameBuffer, 0, lcdData.length*3);
											System.out.flush();
											Thread.sleep(Integer.MAX_VALUE); // Wait for as long as possible until the libretro core kills this
										}

										Mobile.config.saveConfig();
										settingsChanged();

										// Run jar
										Mobile.getPlatform().runJar();
									}
									else
									{
										Mobile.log(Mobile.LOG_ERROR, Libretro.class.getPackage().getName() + "." + Libretro.class.getSimpleName() + ": " + "Couldn't load jar...");
										System.exit(0);
									}
								break;

								case 11: // set save path //
									buffer = new byte[code];
									bytesRead = System.in.read(buffer);

									Mobile.getPlatform().dataPath = new String(buffer, 0, bytesRead);
								break;

								case 13:
									/* Received updated settings from libretro core */
									buffer = new byte[code];
									bytesRead = System.in.read(buffer);

									String cfgvars = new String(buffer, 0, bytesRead);
									/* Tokens: [0]="FJ2ME_LR_OPTS:", [1]=width, [2]=height, [3]=rotate, [4]=phone, [5]=fps, ... */
									cfgtokens = cfgvars.split("[| x]", 0);
									/*
									 * cfgtokens[0] is the string used to indicate that the
									 * received string is a config update. Only useful for debugging,
									 * but better leave it in there as we might make adjustments later.
									 */
									Mobile.config.settings.put("scrwidth",  ""+Integer.parseInt(cfgtokens[1]));
									Mobile.config.settings.put("scrheight", ""+Integer.parseInt(cfgtokens[2]));

									Mobile.config.settings.put("rotate", "" + (Integer.parseInt(cfgtokens[3])*90));

									if(Integer.parseInt(cfgtokens[4])==0)  { Mobile.config.settings.put("phone", "Standard"); }
									if(Integer.parseInt(cfgtokens[4])==1)  { Mobile.config.settings.put("phone", "LG");    }
									if(Integer.parseInt(cfgtokens[4])==2)  { Mobile.config.settings.put("phone", "Motorola");  }
									if(Integer.parseInt(cfgtokens[4])==3)  { Mobile.config.settings.put("phone", "MotoTriplets"); }
									if(Integer.parseInt(cfgtokens[4])==4)  { Mobile.config.settings.put("phone", "MotoV8"); }
									if(Integer.parseInt(cfgtokens[4])==5)  { Mobile.config.settings.put("phone", "MotoA1000"); }
									if(Integer.parseInt(cfgtokens[4])==6)  { Mobile.config.settings.put("phone", "NokiaKeyboard"); }
									if(Integer.parseInt(cfgtokens[4])==7)  { Mobile.config.settings.put("phone", "Sagem"); }
									if(Integer.parseInt(cfgtokens[4])==8)  { Mobile.config.settings.put("phone", "Siemens"); }
									if(Integer.parseInt(cfgtokens[4])==9)  { Mobile.config.settings.put("phone", "SKT"); }
									if(Integer.parseInt(cfgtokens[4])==10) { Mobile.config.settings.put("phone", "KDDI"); }

									Mobile.config.settings.put("fps", ""+ Integer.parseInt(cfgtokens[5]));

									Mobile.config.settings.put("sound", Integer.parseInt(cfgtokens[6]) == 1 ? "on" : "off");

									Mobile.config.settings.put("soundfont", Integer.parseInt(cfgtokens[7]) == 1 ? "Custom" : "Default");

									Mobile.config.sysSettings.put("dumpAudioStreams", Integer.parseInt(cfgtokens[8]) == 1 ? "on" : "off");

									Mobile.config.sysSettings.put("logLevel", "" + Integer.parseInt(cfgtokens[9]));

									Mobile.config.settings.put("spdhacknoalpha", Integer.parseInt(cfgtokens[10]) == 1 ? "on" : "off");

									if(Integer.parseInt(cfgtokens[11])==0) { Mobile.config.settings.put("backlightcolor", "Disabled"); }
									if(Integer.parseInt(cfgtokens[11])==1) { Mobile.config.settings.put("backlightcolor", "Green");    }
									if(Integer.parseInt(cfgtokens[11])==2) { Mobile.config.settings.put("backlightcolor", "Cyan");  }
									if(Integer.parseInt(cfgtokens[11])==3) { Mobile.config.settings.put("backlightcolor", "Orange"); }
									if(Integer.parseInt(cfgtokens[11])==4) { Mobile.config.settings.put("backlightcolor", "Violet"); }
									if(Integer.parseInt(cfgtokens[11])==5) { Mobile.config.settings.put("backlightcolor", "Red"); }

									Mobile.config.settings.put("compatfantasyzonefix", Integer.parseInt(cfgtokens[12]) == 1 ? "on" : "off");

									Mobile.config.settings.put("compattranstooriginonreset", Integer.parseInt(cfgtokens[13]) == 1 ? "on" : "off");

									Mobile.config.settings.put("textfont", Integer.parseInt(cfgtokens[14]) == 1 ? "Custom" : "Default");

									Mobile.config.settings.put("fontoffset", "" + Integer.parseInt(cfgtokens[15]));

									Mobile.config.sysSettings.put("dumpGraphicsObjects", Integer.parseInt(cfgtokens[16]) == 1 ? "on" : "off");

									Mobile.config.sysSettings.put("deleteTempKJXFiles", Integer.parseInt(cfgtokens[17]) == 1 ? "on" : "off");

									Mobile.config.sysSettings.put("M3GUntextured", Integer.parseInt(cfgtokens[18]) == 1 ? "on" : "off");

									Mobile.config.sysSettings.put("M3GWireframe", Integer.parseInt(cfgtokens[19]) == 1 ? "on" : "off");

									if(Integer.parseInt(cfgtokens[20])==0) { Mobile.config.settings.put("fpshack", "Default"); }
									if(Integer.parseInt(cfgtokens[20])==1) { Mobile.config.settings.put("fpshack", "Safe");  }
									if(Integer.parseInt(cfgtokens[20])==2) { Mobile.config.settings.put("fpshack", "Extended");  }
									if(Integer.parseInt(cfgtokens[20])==3) { Mobile.config.settings.put("fpshack", "Aggressive");  }

									Mobile.config.settings.put("compatimmediaterepaints", Integer.parseInt(cfgtokens[21]) == 1 ? "on" : "off");

									Mobile.config.settings.put("compatoverrideplatchecks", Integer.parseInt(cfgtokens[22]) == 1 ? "on" : "off");

									Mobile.config.settings.put("compatsiemensfriendlydrawing", Integer.parseInt(cfgtokens[23]) == 1 ? "on" : "off");

									Mobile.config.settings.put("spdhackm3ghalfres", Integer.parseInt(cfgtokens[24]) == 1 ? "on" : "off");

									Mobile.config.settings.put("dojaversion", "" + Integer.parseInt(cfgtokens[25]));

									Mobile.config.settings.put("compatignorevolumechanges", Integer.parseInt(cfgtokens[26]) == 1 ? "on" : "off");

									Mobile.config.settings.put("spdhackmcv3halfres", Integer.parseInt(cfgtokens[27]) == 1 ? "on" : "off");

									Mobile.config.settings.put("spdhackmcv3nolighting", Integer.parseInt(cfgtokens[28]) == 1 ? "on" : "off");

									Mobile.config.settings.put("compatmcv3horizfovfix", Integer.parseInt(cfgtokens[29]) == 1 ? "on" : "off");

									Mobile.config.settings.put("MCV3ShowHeapUsage", Integer.parseInt(cfgtokens[30]) == 1 ? "on" : "off");

									Mobile.config.settings.put("MCV3ShowTimeMetrics", Integer.parseInt(cfgtokens[31]) == 1 ? "on" : "off");

									if(Integer.parseInt(cfgtokens[32])==0) { Mobile.config.settings.put("m3gantialiasmode", "off"); }
									if(Integer.parseInt(cfgtokens[32])==1) { Mobile.config.settings.put("m3gantialiasmode", "app");  }
									if(Integer.parseInt(cfgtokens[32])==2) { Mobile.config.settings.put("m3gantialiasmode", "on");  }

									if(Integer.parseInt(cfgtokens[33])==0) { Mobile.config.settings.put("m3gbilinearmode", "off"); }
									if(Integer.parseInt(cfgtokens[33])==1) { Mobile.config.settings.put("m3gbilinearmode", "app");  }
									if(Integer.parseInt(cfgtokens[33])==2) { Mobile.config.settings.put("m3gbilinearmode", "on");  }

									if(Integer.parseInt(cfgtokens[34])==0) { Mobile.config.settings.put("m3gditheringmode", "off"); }
									if(Integer.parseInt(cfgtokens[34])==1) { Mobile.config.settings.put("m3gditheringmode", "app");  }
									if(Integer.parseInt(cfgtokens[34])==2) { Mobile.config.settings.put("m3gditheringmode", "on");  }

									if(Integer.parseInt(cfgtokens[35])==0) { Mobile.config.settings.put("m3gperspcorrmode", "off"); }
									if(Integer.parseInt(cfgtokens[35])==1) { Mobile.config.settings.put("m3gperspcorrmode", "app");  }
									if(Integer.parseInt(cfgtokens[35])==2) { Mobile.config.settings.put("m3gperspcorrmode", "on");  }

									if(Integer.parseInt(cfgtokens[36])==3)  { Mobile.config.settings.put("m3gperspcorrsubfactor", "extra"); }
									if(Integer.parseInt(cfgtokens[36])==7)  { Mobile.config.settings.put("m3gperspcorrsubfactor", "high");  }
									if(Integer.parseInt(cfgtokens[36])==15) { Mobile.config.settings.put("m3gperspcorrsubfactor", "medium");  }
									if(Integer.parseInt(cfgtokens[36])==31) { Mobile.config.settings.put("m3gperspcorrsubfactor", "low");  }

									if(Integer.parseInt(cfgtokens[37])==3) { Mobile.config.settings.put("m3gmipmapmode", "linear"); }
									if(Integer.parseInt(cfgtokens[37])==2) { Mobile.config.settings.put("m3gmipmapmode", "nearest");  }
									if(Integer.parseInt(cfgtokens[37])==1) { Mobile.config.settings.put("m3gmipmapmode", "app");  }
									if(Integer.parseInt(cfgtokens[37])==0) { Mobile.config.settings.put("m3gmipmapmode", "off");  }

									Mobile.config.settings.put("m3gdisablefog", Integer.parseInt(cfgtokens[38]) == 1 ? "on" : "off");

									Mobile.config.saveConfig();
									settingsChanged();
								break;

								case 15:
									lastCoreUpdateTime = System.currentTimeMillis();

									int multiplierScaled = (din[1] << 8) | din[2];

									if(din[3] == 1) // Frontend has processed the last sent frame, start counting for pause
									{
										canPause = true;
										break;
									}
									else // The frontend is requesting a new frame
									{
										canPause = false;
										if(Mobile.isPaused) // Resume if it was paused previously
										{
											MobilePlatform.pauseResumeApp();
										}
									}

									// Check if the frontend is fast-forwarding
									if(din[4] == 0)
									{
										MobilePlatform.pressedKeys[20] = false;
									}
									else
									{
										MobilePlatform.pressedKeys[20] = true;
										if(multiplierScaled <= 0) { Mobile.fastForwardMultiplier = 20.0f; }
										else { Mobile.fastForwardMultiplier = multiplierScaled / 100.0f; }
									}

									/* Send Frame to Libretro */
									try
									{
										//frameHeader[0] = (byte)0xFE;
										frameHeader[1] = (byte)((lcdWidth>>8)&0xFF);
										frameHeader[2] = (byte)((lcdWidth)&0xFF);
										frameHeader[3] = (byte)((lcdHeight>>8)&0xFF);
										frameHeader[4] = (byte)((lcdHeight)&0xFF);

										frameHeader[6] = (byte)((Mobile.vibrationDuration>>24) & 0xFF);
										frameHeader[7] = (byte)((Mobile.vibrationDuration>>16) & 0xFF);
										frameHeader[8] = (byte)((Mobile.vibrationDuration>>8) & 0xFF);
										frameHeader[9] = (byte)((Mobile.vibrationDuration) & 0xFF);

										frameHeader[10] = (byte)((Mobile.vibrationStrength>>24) & 0xFF);
										frameHeader[11] = (byte)((Mobile.vibrationStrength>>16) & 0xFF);
										frameHeader[12] = (byte)((Mobile.vibrationStrength>>8) & 0xFF);
										frameHeader[13] = (byte)((Mobile.vibrationStrength) & 0xFF);

										frameHeader[14] = Mobile.libretroRestartRequested;
										frameHeader[15] = Mobile.libretroEncodingRequested;

										System.out.write(frameHeader, 0, 16);

										/* Vibration duration should be set to zero to prevent constant sends of the same data, so update it here */
										Mobile.vibrationDuration = 0;

										/* Send display data to libretro */
										synchronized (Mobile.getPlatform().getLcdFrontbuffer())
										{
											for(int i=0; i<lcdData.length; i++)
											{
												frameBuffer[3*i]   = (byte)((lcdData[i]>>16)&0xFF);
												frameBuffer[3*i+1] = (byte)((lcdData[i]>>8)&0xFF);
												frameBuffer[3*i+2] = (byte)((lcdData[i])&0xFF);
											}

											System.out.write(frameBuffer, 0, lcdData.length*3);
											System.out.flush();
										}
									}
									catch (Exception e)
									{
										Mobile.log(Mobile.LOG_DEBUG, Libretro.class.getPackage().getName() + "." + Libretro.class.getSimpleName() + ": " + "Error sending frame: "+e.getMessage());
										System.exit(0);
									}
									// We are now ready to start monitoring for pauses, the first frame was requested and sent
								break;
							}
							//System.out.flush();
						}
					}
				}
				catch (Exception e) { System.exit(0); }
			}
		} // timer
	} // LibretroIO

	private static void updatePauseTimer()
	{
		if(!canPause) { return; } // Only start counting this after libretro has finished processing the last sent frame
		long currentTime = System.currentTimeMillis();

		// Check if the timer has expired since the last core update, as anything beyond the PAUSE_DELAY_MS delta
		// between core updates means the frontend is pretty much effectively paused as well)
		if (!Mobile.isPaused && (currentTime - lastCoreUpdateTime >= PAUSE_DELAY_MS))
		{
			MobilePlatform.pauseResumeApp(); // Call to pause the app
		}
	}

	private static String getFormattedLocation(String loc)
	{
		if (loc.startsWith("file://") || loc.startsWith("http://") || loc.startsWith("https://"))
			return loc;

		File file = new File(loc);
		if(!file.isFile())
		{
			Mobile.log(Mobile.LOG_ERROR, Libretro.class.getPackage().getName() + "." + Libretro.class.getSimpleName() + ": " + "File '" + loc + "' not found...");
			System.exit(0);
		}

		return file.toURI().toString();
	}

	private void settingsChanged()
	{
		Mobile.updateSettings();

		frameHeader[5] = (byte) (Mobile.rotateDisplay / 90);

		if(lcdWidth != Mobile.lcdWidth || lcdHeight != Mobile.lcdHeight)
		{
			lcdWidth = Mobile.lcdWidth;
			lcdHeight = Mobile.lcdHeight;
			Mobile.getPlatform().resizeLCD(lcdWidth, lcdHeight);
			lcdData = Mobile.getPlatform().getLcdFrontbuffer().getDataBuffer();
		}
	}
}
