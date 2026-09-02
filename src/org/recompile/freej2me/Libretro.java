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

import java.io.File;
import java.net.URLDecoder;

public class Libretro
{
	private int lcdWidth, lcdHeight;
	int[] lcdData;

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
		/*
		 * Notify the MIDlet class that this version of FreeJ2ME is for Libretro, which disables
		 * the ability to close the jar when a J2ME app requests an exit as this can cause segmentation
		 * faults on libretro frontends and also close them unexpectedly.
		*/
		Mobile.getPlatform().isLibretro = true;

		/*
		 * Checks if the boot-time arguments were received -> width, height
		 *
		 * NOTE:
		 * Due to differences in how linux and win32 pass their cmd arguments,
		 * we can't explicitly check for a given size
		 * on the argv array. Linux includes the "java", "-jar" and
		 * "path/to/freej2me" into the array while WIN32 doesn't.
		 */
		lcdWidth =  Integer.parseInt(args[0]);
		lcdHeight = Integer.parseInt(args[1]);

		/* Once it finishes parsing all arguments, it's time to set up freej2me-lr */

		Mobile.setPlatform(new MobilePlatform(lcdWidth, lcdHeight), new Runnable() { public void run() { settingsChanged(); } });
		lcdData = Mobile.getPlatform().getLcdFrontbuffer().getDataBuffer();

		// The painter here is only really used to check for frontend pauses
		Mobile.getPlatform().setPainter(new Runnable()
		{
			public void run() { updatePauseTimer(); }
		});

		lio = new LibretroIO();

		lio.start();

		System.out.println("+READY");
		System.out.flush();
	}

	private class LibretroIO implements Runnable
	{
		private Thread ioThread;

		public void start()
		{
			ioThread = new Thread(this, "Libretro-IO-Thread");
			ioThread.setDaemon(true);
			ioThread.start();
		}

		@Override
		public void run()
		{
			int bin;
			int[] din = new int[5];
			int count = 0;
			int code;
			byte[] buffer;
			int bytesRead = 0;
			String path;

			try // to read keys
			{
				while(true)
				{
					bin = System.in.read(); // Blocks until there's data available
					if(bin==-1) { return; }

					din[count] = (int)(bin & 0xFF);
					count++;

					/* Check inputs */
					if (count==5)
					{
						count = 0;
						code = (din[1]<<24) | (din[2]<<16) | (din[3]<<8) | din[4];
						switch(din[0])
						{
							case 2:	// joypad key up
								MobilePlatform.pressedKeys[code] = false;
								MobilePlatform.keyReleased(Mobile.getMobileKey(code));
							break;

							case 3: // joypad key down
								MobilePlatform.pressedKeys[code] = true;
								MobilePlatform.keyPressed(Mobile.getMobileKey(code));
							break;

							// Mouse events are all handled similarly
							case 4: // mouse up
							case 5: // mouse down
							case 6: // mouse drag
							{
								int rawX = (din[1] << 8) | din[2];
								int rawY = (din[3] << 8) | din[4];

								int x = rawX, y = rawY;
								switch (Mobile.rotateDisplay)
								{
									case 90:  x = rawY;            y = lcdHeight - rawX; break;
									case 180: x = lcdWidth - rawX; y = lcdHeight - rawY; break;
									case 270: x = lcdWidth - rawY; y = rawX;             break;
								}

								switch (din[0])
								{
									case 4: MobilePlatform.pointerReleased(x, y); break;
									case 5: MobilePlatform.pointerPressed(x, y);  break;
									case 6: MobilePlatform.pointerDragged(x, y);  break;
								}
								break;
							}

							case 10: // load jar
								buffer = new byte[code];
								bytesRead = System.in.read(buffer);

								path = new String(buffer, 0, bytesRead);

								if(Mobile.getPlatform().load(getFormattedLocation(URLDecoder.decode(path, Mobile.textEncoding))))
								{
									if(Mobile.libretroRestartRequested == 1)
									{
										frameHeader[14] = Mobile.libretroRestartRequested;
										frameHeader[15] = Mobile.libretroEncodingRequested;

										System.out.write(frameHeader, 0, 16);

										System.out.write(frameBuffer, 0, lcdData.length*3);
										System.out.flush();
										Thread.sleep(Integer.MAX_VALUE); // Wait for as long as possible until the libretro core kills this
									}
								}
								else
								{
									Mobile.log(Mobile.LOG_ERROR, Libretro.class.getPackage().getName() + "." + Libretro.class.getSimpleName() + ": " + "Couldn't load jar...");
									System.exit(0);
								}
							break;

							case 11: // set save path
								buffer = new byte[code];
								bytesRead = System.in.read(buffer);

								Mobile.getPlatform().dataPath = new String(buffer, 0, bytesRead);
							break;


							case 12: // Received settings from libretro core
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

								Mobile.config.sysSettings.put("sound", Integer.parseInt(cfgtokens[6]) == 1 ? "on" : "off");

								Mobile.config.sysSettings.put("soundfont", Integer.parseInt(cfgtokens[7]) == 1 ? "Custom" : "Default");

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

								Mobile.config.sysSettings.put("textfont", Integer.parseInt(cfgtokens[14]) == 1 ? "Custom" : "Default");

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

								Mobile.config.settings.put("compatrepaintonsetcurrent", Integer.parseInt(cfgtokens[39]) == 1 ? "on" : "off");

								Mobile.config.saveConfig();
								settingsChanged();
							break;

							case 13: // Run jar
								buffer = new byte[code];
								bytesRead = System.in.read(buffer);
								Mobile.getPlatform().runJar();
							break;

							case 15: // Libretro core requested a new frame.
								lastCoreUpdateTime = System.currentTimeMillis();

								// Fire repeats for any currently held keys on
								// every libretro frame tick
								for(int i = 0; i < MobilePlatform.pressedKeys.length; i++)
								{
									if(MobilePlatform.pressedKeys[i]) { MobilePlatform.keyRepeated(Mobile.getMobileKey(i)); }
								}

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
					}
				}
			}
			catch (Exception e) { System.exit(0); }
		} // run()
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
