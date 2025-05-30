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

import java.awt.image.DataBufferInt;
import java.util.Timer;
import java.util.TimerTask;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

import javax.microedition.midlet.MIDlet;

public class Libretro
{
	private int lcdWidth;
	private int lcdHeight;
	int[] lcdData;

	private boolean soundEnabled = true;

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

		if(Integer.parseInt(args[2]) == 0) { Mobile.rotateDisplay = false; }
		else { Mobile.rotateDisplay = true; }

		Mobile.lg = false;
		Mobile.motorola = false;
		Mobile.motoTriplets = false;
		Mobile.motoV8 = false;
		Mobile.nokiaKeyboard = false;
		Mobile.sagem = false;
		Mobile.siemens = false;
		Mobile.sharp = false;
		Mobile.skt = false;

		if(Integer.parseInt(args[3]) == 1)      { Mobile.lg = true;    }
		else if(Integer.parseInt(args[3]) == 2) { Mobile.motorola = true;  }
		else if(Integer.parseInt(args[3]) == 3) { Mobile.motoTriplets = true; }
		else if(Integer.parseInt(args[3]) == 4) { Mobile.motoV8 = true; }
		else if(Integer.parseInt(args[3]) == 5) { Mobile.nokiaKeyboard = true; }
		else if(Integer.parseInt(args[3]) == 6) { Mobile.sagem = true; }
		else if(Integer.parseInt(args[3]) == 7) { Mobile.siemens = true; }
		else if(Integer.parseInt(args[3]) == 8) { Mobile.sharp = true; }
		else if(Integer.parseInt(args[3]) == 9) { Mobile.skt = true; }

		Mobile.limitFPS = Integer.parseInt(args[4]);

		if(Integer.parseInt(args[5]) == 0) { soundEnabled = false; }
		else { soundEnabled = true; }

		if(Integer.parseInt(args[6]) == 0) { Mobile.useCustomMidi = false; }
		else { Mobile.useCustomMidi = true; }

		/* Dump Audio Streams will not be a per-game FreeJ2ME config, so it will have to be set every time for now */
		if(Integer.parseInt(args[7]) == 0) { Mobile.dumpAudioStreams = false; }
		else { Mobile.dumpAudioStreams = true; }

		/* Same for Logging Level */
		Mobile.minLogLevel = (byte) (Integer.parseInt(args[8]));

		/* No Alpha on Blank Images SpeedHack is a per-game config */
		if(Integer.parseInt(args[9]) == 0) { Mobile.noAlphaOnBlankImages = false; }
		else { Mobile.noAlphaOnBlankImages = true; }

		/* LCD Backlight Mask color index. */
		Mobile.maskIndex = Integer.parseInt(args[10]);

		/* Compat setting to not translate drawRGB calls */
		if(Integer.parseInt(args[11]) == 0) { Mobile.compatDoNotTranslateDrawRGB = false; }
		else { Mobile.compatDoNotTranslateDrawRGB = true; }

		/* Compat setting to translate back to the origin whenever graphics object is reset */
		if(Integer.parseInt(args[12]) == 0) { Mobile.compatTranslateToOriginOnReset = false; }
		else { Mobile.compatTranslateToOriginOnReset = true; }

		// Custom font and size
		if(Integer.parseInt(args[13]) == 0) { Mobile.useCustomTextFont = false; }
		else { Mobile.useCustomTextFont = true; }

		Mobile.fontSizeOffset = (byte) Integer.parseInt(args[14]);

		// Unused for now
		if(Integer.parseInt(args[15]) == 0) { Mobile.dumpGraphicsObjects = false; }
		else { Mobile.dumpGraphicsObjects = true; }

		// Dump KJX extracted JAR and JAD
		if(Integer.parseInt(args[16]) == 0) { Mobile.deleteTemporaryKJXFiles = false; }
		else { Mobile.deleteTemporaryKJXFiles = true; }

		// M3G Render only untextured polygons
		if(Integer.parseInt(args[17]) == 0) { Mobile.M3GRenderUntexturedPolygons = false; }
		else { Mobile.M3GRenderUntexturedPolygons = true; }

		// M3G Render Wireframe
		if(Integer.parseInt(args[18]) == 0) { Mobile.M3GRenderWireframe = false; }
		else { Mobile.M3GRenderWireframe = true; }

		/* Framerate Unlock. */
		Mobile.unlockFramerateHack = (byte) Integer.parseInt(args[19]);

		/* Compat setting to process repaints immediately */
		if(Integer.parseInt(args[20]) == 0) { Mobile.compatImmediateRepaints = false; }
		else { Mobile.compatImmediateRepaints = true; }

		/* Compat setting to override mobile platform checks */
		if(Integer.parseInt(args[21]) == 0) { Mobile.compatOverridePlatformChecks = false; }
		else { Mobile.compatOverridePlatformChecks = true; }

		/* Compat setting to translate drawing methods in a siemens-friendly way */
		if(Integer.parseInt(args[22]) == 0) { Mobile.compatSiemensFriendlyDrawing = false; }
		else { Mobile.compatSiemensFriendlyDrawing = true; }


		/* Once it finishes parsing all arguments, it's time to set up freej2me-lr */

		Mobile.setPlatform(new MobilePlatform(lcdWidth, lcdHeight), new Runnable() { public void run() { settingsChanged(); } });
		lcdData = ((DataBufferInt) Mobile.getPlatform().getLCD().getRaster().getDataBuffer()).getData();

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
			keytimer = new Timer();
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
						bin = System.in.read();
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
									
									if(!Mobile.rotateDisplay)
									{
										MobilePlatform.pointerReleased(mousex, mousey);
									}
									else
									{
										MobilePlatform.pointerReleased(lcdWidth-mousey, mousex);
									}
								break;

								case 5: // mouse down
									mousex = (din[1]<<8) | din[2];
									mousey = (din[3]<<8) | din[4];

									if(!Mobile.rotateDisplay)
									{
										MobilePlatform.pointerPressed(mousex, mousey);
									}
									else
									{
										MobilePlatform.pointerPressed(lcdWidth-mousey, mousex);
									}
								break;

								case 6: // mouse drag
									mousex = (din[1]<<8) | din[2];
									mousey = (din[3]<<8) | din[4];

									if(!Mobile.rotateDisplay)
									{
										MobilePlatform.pointerPressed(mousex, mousey);
									}
									else
									{
										MobilePlatform.pointerPressed(lcdWidth-mousey, mousex);
									}
								break;

								case 10: // load jar
									buffer = new byte[code];
									bytesRead = System.in.read(buffer);

									path = new String(buffer, 0, bytesRead, "UTF-8");

									if(Mobile.getPlatform().load(getFormattedLocation(URLDecoder.decode(path.toString(), Mobile.textEncoding))))
									{
										// Check config

										/* Override configs with the ones passed through commandline */
										Mobile.config.settings.put("scrwidth",  ""+lcdWidth);
										Mobile.config.settings.put("scrheight", ""+lcdHeight);

										if(Mobile.rotateDisplay)   { Mobile.config.settings.put("rotate", "on");  }
										if(!Mobile.rotateDisplay)  { Mobile.config.settings.put("rotate", "off"); }

										if(Mobile.lg)                 { Mobile.config.settings.put("phone", "LG");    }
										else if(Mobile.motorola)      { Mobile.config.settings.put("phone", "Motorola");  }
										else if(Mobile.motoTriplets)  { Mobile.config.settings.put("phone", "MotoTriplets"); }
										else if(Mobile.motoV8)        { Mobile.config.settings.put("phone", "MotoV8"); }
										else if(Mobile.nokiaKeyboard) { Mobile.config.settings.put("phone", "NokiaKeyboard"); }
										else if(Mobile.sagem)         { Mobile.config.settings.put("phone", "Sagem"); }
										else if(Mobile.siemens)       { Mobile.config.settings.put("phone", "Siemens"); }
										else if(Mobile.sharp)         { Mobile.config.settings.put("phone", "Sharp"); }
										else if(Mobile.skt)           { Mobile.config.settings.put("phone", "SKT"); }
										else                          { Mobile.config.settings.put("phone", "Standard"); }

										if(soundEnabled)   { Mobile.config.settings.put("sound", "on");  }
										if(!soundEnabled)  { Mobile.config.settings.put("sound", "off"); }

										Mobile.config.settings.put("fps", "" + Mobile.limitFPS);

										if(!Mobile.useCustomMidi)  { Mobile.config.settings.put("soundfont", "Default"); }
										else                       { Mobile.config.settings.put("soundfont", "Custom");  }

										if(!Mobile.noAlphaOnBlankImages) { Mobile.config.settings.put("spdhacknoalpha", "off"); }
										else                             { Mobile.config.settings.put("spdhacknoalpha", "on"); }

										if(Mobile.maskIndex == 0)      { Mobile.config.settings.put("backlightcolor", "Disabled"); }
										else if(Mobile.maskIndex == 1) { Mobile.config.settings.put("backlightcolor", "Green"); }
										else if(Mobile.maskIndex == 2) { Mobile.config.settings.put("backlightcolor", "Cyan"); }
										else if(Mobile.maskIndex == 3) { Mobile.config.settings.put("backlightcolor", "Orange"); }
										else if(Mobile.maskIndex == 4) { Mobile.config.settings.put("backlightcolor", "Violet"); }
										else if(Mobile.maskIndex == 5) { Mobile.config.settings.put("backlightcolor", "Red"); }

										if(!Mobile.compatDoNotTranslateDrawRGB) { Mobile.config.settings.put("compatdonottranslatedrawrgb", "off"); }
										else                                 { Mobile.config.settings.put("compatdonottranslatedrawrgb", "on"); }

										if(!Mobile.compatTranslateToOriginOnReset) { Mobile.config.settings.put("compattranstooriginonreset", "off"); }
										else                                       { Mobile.config.settings.put("compattranstooriginonreset", "on"); }

										if(!Mobile.compatImmediateRepaints) { Mobile.config.settings.put("compatimmediaterepaints", "off"); }
										else                                { Mobile.config.settings.put("compatimmediaterepaints", "on"); }

										if(!Mobile.compatOverridePlatformChecks) { Mobile.config.settings.put("compatoverrideplatchecks", "off"); }
										else                                     { Mobile.config.settings.put("compatoverrideplatchecks", "on"); }

										if(!Mobile.compatSiemensFriendlyDrawing) { Mobile.config.settings.put("compatsiemensfriendlydrawing", "off"); }
										else                                     { Mobile.config.settings.put("compatsiemensfriendlydrawing", "on"); }

										if(!Mobile.useCustomTextFont)  { Mobile.config.settings.put("textfont", "Default"); }
										else                           { Mobile.config.settings.put("textfont", "Custom");  }

										Mobile.config.settings.put("fontoffset", "" + Mobile.fontSizeOffset);

										if(Mobile.unlockFramerateHack == 0)      { Mobile.config.settings.put("fpshack", "Default");  }
										else if(Mobile.unlockFramerateHack == 1) { Mobile.config.settings.put("fpshack", "Safe");  }
										else if(Mobile.unlockFramerateHack == 2) { Mobile.config.settings.put("fpshack", "Extended");  }
										else if(Mobile.unlockFramerateHack == 3) { Mobile.config.settings.put("fpshack", "Aggressive");  }

										// Update system settings

										Mobile.config.sysSettings.put("fpsCounterPosition", "Off"); // Libretro has its own frame counter

										Mobile.config.sysSettings.put("logLevel", "" + Mobile.minLogLevel);

										if(!Mobile.M3GRenderUntexturedPolygons)  { Mobile.config.sysSettings.put("M3GUntextured", "off"); }
										else                                     { Mobile.config.sysSettings.put("M3GUntextured", "on");  }

										if(!Mobile.M3GRenderWireframe)  { Mobile.config.sysSettings.put("M3GWireframe", "off"); }
										else                            { Mobile.config.sysSettings.put("M3GWireframe", "on");  }

										if(!Mobile.deleteTemporaryKJXFiles)  { Mobile.config.sysSettings.put("deleteTempKJXFiles", "off"); }
										else                                 { Mobile.config.sysSettings.put("deleteTempKJXFiles", "on");  }

										if(!Mobile.dumpAudioStreams)  { Mobile.config.sysSettings.put("dumpAudioStreams", "off"); }
										else                          { Mobile.config.sysSettings.put("dumpAudioStreams", "on");  }

										if(!Mobile.dumpGraphicsObjects)  { Mobile.config.sysSettings.put("dumpGraphicsObjects", "off"); }
										else                             { Mobile.config.sysSettings.put("dumpGraphicsObjects", "on");  }

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
									
									Mobile.libretroStarted = true;
								break;

								case 11: // set save path //
									buffer = new byte[code];
									bytesRead = System.in.read(buffer);

									Mobile.getPlatform().dataPath = new String(buffer, 0, bytesRead, "UTF-8");
								break;

								case 13:
									/* Received updated settings from libretro core */
									buffer = new byte[code];
									bytesRead = System.in.read(buffer);
									
									String cfgvars = new String(buffer, 0, bytesRead, "UTF-8");
									/* Tokens: [0]="FJ2ME_LR_OPTS:", [1]=width, [2]=height, [3]=rotate, [4]=phone, [5]=fps, ... */
									cfgtokens = cfgvars.split("[| x]", 0);
									/* 
									 * cfgtokens[0] is the string used to indicate that the 
									 * received string is a config update. Only useful for debugging, 
									 * but better leave it in there as we might make adjustments later.
									 */
									Mobile.config.settings.put("scrwidth",  ""+Integer.parseInt(cfgtokens[1]));
									Mobile.config.settings.put("scrheight", ""+Integer.parseInt(cfgtokens[2]));

									if(Integer.parseInt(cfgtokens[3])==1) { Mobile.config.settings.put("rotate", "on");  }
									if(Integer.parseInt(cfgtokens[3])==0) { Mobile.config.settings.put("rotate", "off"); }

									if(Integer.parseInt(cfgtokens[4])==0) { Mobile.config.settings.put("phone", "Standard"); }
									if(Integer.parseInt(cfgtokens[4])==1) { Mobile.config.settings.put("phone", "LG");    }
									if(Integer.parseInt(cfgtokens[4])==2) { Mobile.config.settings.put("phone", "Motorola");  }
									if(Integer.parseInt(cfgtokens[4])==3) { Mobile.config.settings.put("phone", "MotoTriplets"); }
									if(Integer.parseInt(cfgtokens[4])==4) { Mobile.config.settings.put("phone", "MotoV8"); }
									if(Integer.parseInt(cfgtokens[4])==5) { Mobile.config.settings.put("phone", "NokiaKeyboard"); }
									if(Integer.parseInt(cfgtokens[4])==6) { Mobile.config.settings.put("phone", "Sagem"); }
									if(Integer.parseInt(cfgtokens[4])==7) { Mobile.config.settings.put("phone", "Siemens"); }
									if(Integer.parseInt(cfgtokens[4])==8) { Mobile.config.settings.put("phone", "Sharp"); }
									if(Integer.parseInt(cfgtokens[4])==9) { Mobile.config.settings.put("phone", "SKT"); }

									Mobile.config.settings.put("fps", ""+cfgtokens[5]);

									if(Integer.parseInt(cfgtokens[6])==1) { Mobile.config.settings.put("sound", "on");  }
									if(Integer.parseInt(cfgtokens[6])==0) { Mobile.config.settings.put("sound", "off"); }

									if(Integer.parseInt(cfgtokens[7])==0) { Mobile.config.settings.put("soundfont", "Default"); }
									if(Integer.parseInt(cfgtokens[7])==1) { Mobile.config.settings.put("soundfont", "Custom");  }

									if(Integer.parseInt(cfgtokens[8])==1) { Mobile.config.sysSettings.put("dumpAudioStreams", "on");  }
									if(Integer.parseInt(cfgtokens[8])==0) { Mobile.config.sysSettings.put("dumpAudioStreams", "off"); }

									Mobile.config.sysSettings.put("dumpAudioStreams", "" + Integer.parseInt(cfgtokens[9]));

									if(Integer.parseInt(cfgtokens[10])==0) { Mobile.config.settings.put("spdhacknoalpha", "off");  }
									else { Mobile.config.settings.put("spdhacknoalpha", "on"); }

									if(Integer.parseInt(cfgtokens[11])==0) { Mobile.config.settings.put("backlightcolor", "Disabled"); }
									if(Integer.parseInt(cfgtokens[11])==1) { Mobile.config.settings.put("backlightcolor", "Green");    }
									if(Integer.parseInt(cfgtokens[11])==2) { Mobile.config.settings.put("backlightcolor", "Cyan");  }
									if(Integer.parseInt(cfgtokens[11])==3) { Mobile.config.settings.put("backlightcolor", "Orange"); }
									if(Integer.parseInt(cfgtokens[11])==4) { Mobile.config.settings.put("backlightcolor", "Violet"); }
									if(Integer.parseInt(cfgtokens[11])==5) { Mobile.config.settings.put("backlightcolor", "Red"); }

									if(Integer.parseInt(cfgtokens[12])==0) { Mobile.config.settings.put("compatdonottranslatedrawrgb", "off");  }
									else { Mobile.config.settings.put("compatdonottranslatedrawrgb", "on"); }

									if(Integer.parseInt(cfgtokens[13])==0) { Mobile.config.settings.put("compattranstooriginonreset", "off");  }
									else { Mobile.config.settings.put("compattranstooriginonreset", "on"); }

									if(Integer.parseInt(cfgtokens[14])==0) { Mobile.config.settings.put("textfont", "Default"); }
									if(Integer.parseInt(cfgtokens[14])==1) { Mobile.config.settings.put("textfont", "Custom");  }

									Mobile.config.settings.put("fontoffset", "" + Integer.parseInt(cfgtokens[15]));

									if(Integer.parseInt(cfgtokens[16])==1) { Mobile.config.sysSettings.put("dumpGraphicsObjects", "on");  }
									if(Integer.parseInt(cfgtokens[16])==0) { Mobile.config.sysSettings.put("dumpGraphicsObjects", "off"); }

									if(Integer.parseInt(cfgtokens[17])==1) { Mobile.config.sysSettings.put("deleteTempKJXFiles", "on");  }
									if(Integer.parseInt(cfgtokens[17])==0) { Mobile.config.sysSettings.put("deleteTempKJXFiles", "off"); }

									if(Integer.parseInt(cfgtokens[18])==1) { Mobile.config.sysSettings.put("M3GUntextured", "on");  }
									if(Integer.parseInt(cfgtokens[18])==0) { Mobile.config.sysSettings.put("M3GUntextured", "off"); }

									if(Integer.parseInt(cfgtokens[19])==1) { Mobile.config.sysSettings.put("M3GWireframe", "on");  }
									if(Integer.parseInt(cfgtokens[19])==0) { Mobile.config.sysSettings.put("M3GWireframe", "off"); }

									if(Integer.parseInt(cfgtokens[20])==0) { Mobile.config.settings.put("fpshack", "Default"); }
									if(Integer.parseInt(cfgtokens[20])==1) { Mobile.config.settings.put("fpshack", "Safe");  }
									if(Integer.parseInt(cfgtokens[20])==2) { Mobile.config.settings.put("fpshack", "Extended");  }
									if(Integer.parseInt(cfgtokens[20])==3) { Mobile.config.settings.put("fpshack", "Aggressive");  }

									if(Integer.parseInt(cfgtokens[21])==0) { Mobile.config.settings.put("compatimmediaterepaints", "off");  }
									else { Mobile.config.settings.put("compatimmediaterepaints", "on"); }

									if(Integer.parseInt(cfgtokens[22])==0) { Mobile.config.settings.put("compatoverrideplatchecks", "off");  }
									else { Mobile.config.settings.put("compatoverrideplatchecks", "on"); }

									if(Integer.parseInt(cfgtokens[23])==0) { Mobile.config.settings.put("compatsiemensfriendlydrawing", "off");  }
									else { Mobile.config.settings.put("compatsiemensfriendlydrawing", "on"); }


									Mobile.config.saveConfig();
									settingsChanged();
								break;

								case 15:

									// Check if the frontend is fast-forwarding
									if(din[4] == 0) { MobilePlatform.pressedKeys[19] = false; }
									else { MobilePlatform.pressedKeys[19] = true; }

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
										for(int i=0; i<lcdData.length; i++)
										{
											frameBuffer[3*i]   = (byte)((lcdData[i]>>16)&0xFF);
											frameBuffer[3*i+1] = (byte)((lcdData[i]>>8)&0xFF);
											frameBuffer[3*i+2] = (byte)((lcdData[i])&0xFF);
										}

										System.out.write(frameBuffer, 0, lcdData.length*3);
										System.out.flush();
									}
									catch (Exception e)
									{
										Mobile.log(Mobile.LOG_DEBUG, Libretro.class.getPackage().getName() + "." + Libretro.class.getSimpleName() + ": " + "Error sending frame: "+e.getMessage());
										System.exit(0);
									}
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

		if(Mobile.rotateDisplay == true) { frameHeader[5] = (byte)1; } 
		else                             { frameHeader[5] = (byte)0; }
		
		if(lcdWidth != Mobile.lcdWidth || lcdHeight != Mobile.lcdHeight)
		{
			lcdWidth = Mobile.lcdWidth;
			lcdHeight = Mobile.lcdHeight;
			Mobile.getPlatform().resizeLCD(Mobile.lcdWidth, Mobile.lcdHeight);
			lcdData = ((DataBufferInt) Mobile.getPlatform().getLCD().getRaster().getDataBuffer()).getData();
		}
	}

}
