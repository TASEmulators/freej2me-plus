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
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.midlet.MIDlet;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.microedition.media.Manager;

public class Libretro
{
	private int lcdWidth;
	private int lcdHeight;

	private Runnable painter;

	private BufferedImage surface;
	private Graphics2D gc;

	private Config config;
	private boolean useNokiaControls = false;
	private boolean useSiemensControls = false;
	private boolean useMotorolaControls = false;
	private boolean halveCanvasRes = false;
	private boolean rotateDisplay = false;
	private boolean soundEnabled = true;

	// Frame Limit Variables
	private int limitFPS = 0;
	private long lastRenderTime = 0;
	private long requiredFrametime = 0;
	private long elapsedTime = 0;
	private long sleepTime = 0;

	private int maxmidistreams = 32;

	private boolean[] pressedKeys = new boolean[128];

	private byte[] frameBuffer = new byte[800*800*3];
	private byte[] RGBframeBuffer = new byte[800*800*3];
	private byte[] frameHeader = new byte[]{(byte)0xFE, 0, 0, 0, 0, 0};

	private int mousex;
	private int mousey;

	/* 
	 * StringBuilder used to get the updated configs from the libretro core
	 * String[] used to tokenize each setting as its own string.
	 */
	private StringBuilder cfgs;
	String[] cfgtokens;

	LibretroIO lio;

	public static void main(String args[])
	{
		Libretro app = new Libretro(args);
	}

	public Libretro(String args[])
	{
		lcdWidth  = 240;
		lcdHeight = 320;

		/* 
		 * Notify the MIDlet class that this version of FreeJ2ME is for Libretro, which disables 
		 * the ability to close the jar when a J2ME app requests an exit as this can cause segmentation
		 * faults on frontends and also close the unexpectedly.
		*/
		MIDlet.isLibretro = true;

		/* 
		 * If the directory for custom soundfonts doesn't exist, create it, no matter if the user
		 * is going to use it or not.
		 */
		try 
		{
			if(!PlatformPlayer.soundfontDir.isDirectory()) 
			{ 
				PlatformPlayer.soundfontDir.mkdirs();
				File dummyFile = new File(PlatformPlayer.soundfontDir.getPath() + File.separatorChar + "Put your sf2 bank here");
				dummyFile.createNewFile();
			}
		}
		catch(IOException e) { System.out.println("Failed to create custom midi info file:" + e.getMessage()); }

		/* 
		 * Checks if the arguments were received from the commandline -> width, height, rotate, phonetype, fps, sound, ...
		 * 
		 * NOTE:
		 * Due to differences in how linux and win32 pass their cmd arguments, we can't explictly check for a given size
		 * on the argv array. Linux includes the "java", "-jar" and "path/to/freej2me" into the array while WIN32 doesn't.
		 */
		lcdWidth =  Integer.parseInt(args[0]);
		lcdHeight = Integer.parseInt(args[1]);

		if(Integer.parseInt(args[2]) == 1) { halveCanvasRes = true; }

		if(Integer.parseInt(args[3]) == 1) { rotateDisplay = true; }

		if(Integer.parseInt(args[4]) == 1)      { useNokiaControls = true; Mobile.nokia = true;        }
		else if(Integer.parseInt(args[4]) == 2) { useSiemensControls = true; Mobile.siemens = true;    }
		else if(Integer.parseInt(args[4]) == 3) { useMotorolaControls = true; Mobile.motorola = true;  }
		else if(Integer.parseInt(args[4]) == 4) { useNokiaControls = true; Mobile.sonyEricsson = true; }

		limitFPS = Integer.parseInt(args[5]);

		if(Integer.parseInt(args[6]) == 0) { soundEnabled = false; }

		if(Integer.parseInt(args[7]) == 1) { PlatformPlayer.customMidi = true; }
		
		maxmidistreams = Integer.parseInt(args[8]);
		Manager.updatePlayerNum((byte) maxmidistreams);

		/* Dump Audio Streams will not be a per-game FreeJ2ME config, so it will have to be set every time for now */
		if(Integer.parseInt(args[9]) == 1) { Manager.dumpAudioStreams = true; }

		/* Once it finishes parsing all arguments, it's time to set up freej2me-lr */

		surface = new BufferedImage(lcdWidth, lcdHeight, BufferedImage.TYPE_3BYTE_BGR); // libretro display
		gc = (Graphics2D)surface.getGraphics();

		Mobile.setPlatform(new MobilePlatform(lcdWidth, lcdHeight));

		config = new Config();
		config.onChange = new Runnable() { public void run() { settingsChanged(); } };

		lio = new LibretroIO();

		lio.start();

		painter = new Runnable()
		{
			public void run()
			{
				try
				{
					if(limitFPS>0)
					{
						requiredFrametime = 1000 / limitFPS;
						elapsedTime = System.currentTimeMillis() - lastRenderTime;
						sleepTime = requiredFrametime - elapsedTime;

						if (sleepTime > 0) { Thread.sleep(sleepTime); }

						gc.drawImage(Mobile.getPlatform().getLCD(), 0, 0, lcdWidth, lcdHeight, null);

						lastRenderTime = System.currentTimeMillis();
					} 
					else { gc.drawImage(Mobile.getPlatform().getLCD(), 0, 0, lcdWidth, lcdHeight, null); }
				}
				catch (Exception e) { }
			}
		};
		
		Mobile.getPlatform().setPainter(painter);

		Mobile.getPlatform().startEventQueue();

		System.out.println("+READY");
		System.out.flush();
	}

	private class LibretroIO
	{
		private Timer keytimer;
		private TimerTask keytask;

		public void start()
		{
			keytimer = new Timer();
			keytask = new LibretroTimerTask();
			keytimer.schedule(keytask, 0, 1);
		}

		public void stop()
		{
			keytimer.cancel();
		}

		private class LibretroTimerTask extends TimerTask
		{
			private int bin;
			private int[] din = new int[5];
			private int count = 0;
			private int code;
			private int mobikey;
			private StringBuilder path;
			private URL url;

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
						if (count==5)
						{
							count = 0;
							code = (din[1]<<24) | (din[2]<<16) | (din[3]<<8) | din[4];
							switch(din[0])
							{
								case 0: // keyboard key up (unused)
								break;

								case 1:	// keyboard key down (unused)
								break;

								case 2:	// joypad key up
									mobikey = getMobileKeyJoy(code);
									if (mobikey != 0)
									{
										keyUp(mobikey);
									}
								break;

								case 3: // joypad key down
									mobikey = getMobileKeyJoy(code);
									if (mobikey != 0)
									{
										keyDown(mobikey);
									}
								break;

								case 4: // mouse up
									mousex = (din[1]<<8) | din[2];
									mousey = (din[3]<<8) | din[4];
									if(!rotateDisplay)
									{
										Mobile.getPlatform().pointerReleased(mousex, mousey);
									}
									else
									{
										Mobile.getPlatform().pointerReleased(lcdWidth-mousey, mousex);
									}
								break;

								case 5: // mouse down
									mousex = (din[1]<<8) | din[2];
									mousey = (din[3]<<8) | din[4];
									if(!rotateDisplay)
									{
										Mobile.getPlatform().pointerPressed(mousex, mousey);
									}
									else
									{
										Mobile.getPlatform().pointerPressed(lcdWidth-mousey, mousex);
									}
								break;

								case 6: // mouse drag
									mousex = (din[1]<<8) | din[2];
									mousey = (din[3]<<8) | din[4];
									if(!rotateDisplay)
									{
										Mobile.getPlatform().pointerDragged(mousex, mousey);
									}
									else
									{
										Mobile.getPlatform().pointerDragged(lcdWidth-mousey, mousex);
									}
								break;

								case 10: // load jar
									path = new StringBuilder();
									for(int i=0; i<code; i++)
									{
										bin = System.in.read();
										path.append((char)bin);
									}
									url = (new File(path.toString())).toURI().toURL();
									if(Mobile.getPlatform().loadJar(url.toString()))
									{
										// Check config
										config.init();

										/* Override configs with the ones passed through commandline */
										config.settings.put("width",  ""+lcdWidth);
										config.settings.put("height", ""+lcdHeight);

										if(halveCanvasRes)   { config.settings.put("halveCanvasRes", "on");  }
										if(!halveCanvasRes)  { config.settings.put("halveCanvasRes", "off"); }

										if(rotateDisplay)   { config.settings.put("rotate", "on");  }
										if(!rotateDisplay)  { config.settings.put("rotate", "off"); }

										if(useNokiaControls)         { config.settings.put("phone", "Nokia");    }
										else if(useSiemensControls)  { config.settings.put("phone", "Siemens");  }
										else if(useMotorolaControls) { config.settings.put("phone", "Motorola"); }
										else if(Mobile.sonyEricsson) { config.settings.put("phone", "SonyEricsson"); }
										else                         { config.settings.put("phone", "Standard"); }

										if(soundEnabled)   { config.settings.put("sound", "on");  }
										if(!soundEnabled)  { config.settings.put("sound", "off"); }

										config.settings.put("fps", ""+limitFPS);

										if(!PlatformPlayer.customMidi) { config.settings.put("soundfont", "Default"); }
										else                           { config.settings.put("soundfont", "Custom");  }

										config.settings.put("maxmidistreams", ""+maxmidistreams);
										

										config.saveConfig();
										settingsChanged();

										// Run jar
										Mobile.getPlatform().runJar();
									}
									else
									{
										System.out.println("Couldn't load jar...");
										System.exit(0);
									}
								break;

								case 11: // set save path //
									path = new StringBuilder();
									for(int i=0; i<code; i++)
									{
										bin = System.in.read();
										path.append((char)bin);
									}
									Mobile.getPlatform().dataPath = path.toString();
								break;

								case 13:
									/* Received updated settings from libretro core */
									cfgs = new StringBuilder();
									for(int i=0; i<code; i++)
									{
										bin = System.in.read();
										cfgs.append((char)bin);
									}
									String cfgvars = cfgs.toString();
									/* Tokens: [0]="FJ2ME_LR_OPTS:", [1]=width, [2]=height, [3]=rotate, [4]=phone, [5]=fps, ... */
									cfgtokens = cfgvars.split("[| x]", 0);
									/* 
									 * cfgtokens[0] is the string used to indicate that the 
									 * received string is a config update. Only useful for debugging, 
									 * but better leave it in there as we might make adjustments later.
									 */
									config.settings.put("width",  ""+Integer.parseInt(cfgtokens[1]));
									config.settings.put("height", ""+Integer.parseInt(cfgtokens[2]));

									if(Integer.parseInt(cfgtokens[3])==1) { config.settings.put("halveCanvasRes", "on");  }
									if(Integer.parseInt(cfgtokens[3])==0) { config.settings.put("halveCanvasRes", "off"); }

									if(Integer.parseInt(cfgtokens[4])==1) { config.settings.put("rotate", "on");  }
									if(Integer.parseInt(cfgtokens[4])==0) { config.settings.put("rotate", "off"); }

									if(Integer.parseInt(cfgtokens[5])==0) { config.settings.put("phone", "Standard"); }
									if(Integer.parseInt(cfgtokens[5])==1) { config.settings.put("phone", "Nokia");    }
									if(Integer.parseInt(cfgtokens[5])==2) { config.settings.put("phone", "Siemens");  }
									if(Integer.parseInt(cfgtokens[5])==3) { config.settings.put("phone", "Motorola"); }

									config.settings.put("fps", ""+cfgtokens[6]);

									if(Integer.parseInt(cfgtokens[7])==1) { config.settings.put("sound", "on");  }
									if(Integer.parseInt(cfgtokens[7])==0) { config.settings.put("sound", "off"); }

									if(Integer.parseInt(cfgtokens[8])==0) { config.settings.put("soundfont", "Default"); }
									if(Integer.parseInt(cfgtokens[8])==1) { config.settings.put("soundfont", "Custom");  }

									if(Integer.parseInt(cfgtokens[9])==0) { config.settings.put("maxmidistreams", "1");}
									if(Integer.parseInt(cfgtokens[9])==1) { config.settings.put("maxmidistreams", "2");}
									if(Integer.parseInt(cfgtokens[9])==2) { config.settings.put("maxmidistreams", "4");}
									if(Integer.parseInt(cfgtokens[9])==3) { config.settings.put("maxmidistreams", "8");}
									if(Integer.parseInt(cfgtokens[9])==4) { config.settings.put("maxmidistreams", "16");}
									if(Integer.parseInt(cfgtokens[9])==5) { config.settings.put("maxmidistreams", "32");}
									if(Integer.parseInt(cfgtokens[9])==6) { config.settings.put("maxmidistreams", "48");}
									if(Integer.parseInt(cfgtokens[9])==7) { config.settings.put("maxmidistreams", "64");}
									if(Integer.parseInt(cfgtokens[9])==8) { config.settings.put("maxmidistreams", "96");}

									if(Integer.parseInt(cfgtokens[10])==1) { Manager.dumpAudioStreams = true;  }
									if(Integer.parseInt(cfgtokens[10])==0) { Manager.dumpAudioStreams = false; }

									config.saveConfig();
									settingsChanged();
								break;
								
								case 15:
									/* Send Frame to Libretro */
									try
									{
										frameBuffer = ((DataBufferByte) surface.getRaster().getDataBuffer()).getData();

										final int bufferLength = frameBuffer.length;

										/* 
										 * Convert BGR into RGB. Has a negligible performance impact compared to not doing this at all
										 * and sending the BGR array straight to libretro... and is faster than using getRGB().
										 * 
										 * Copying from the original BGR array to a separate RGB array uses a bit more memory, but
										 * works correctly compared to just swapping the channels on the orignal array, where they
										 * still unknowingly end up incorrect from time to time. Runtime performance is pretty much
										 * the same for both methods.
										 */
										for(int i=0; i<bufferLength; i+=3)
										{
											RGBframeBuffer[i]   = frameBuffer[i+2]; // [R]GB = BG[R]
											RGBframeBuffer[i+1] = frameBuffer[i+1];
											RGBframeBuffer[i+2] = frameBuffer[i]; // RG[B] = [B]GR
										}

										//frameHeader[0] = (byte)0xFE;
										frameHeader[1] = (byte)((lcdWidth>>8)&0xFF);
										frameHeader[2] = (byte)((lcdWidth)&0xFF);
										frameHeader[3] = (byte)((lcdHeight>>8)&0xFF);
										frameHeader[4] = (byte)((lcdHeight)&0xFF);

										System.out.write(frameHeader, 0, 6);
										System.out.write(RGBframeBuffer, 0, bufferLength);
										System.out.flush();
									}
									catch (Exception e)
									{
										System.out.print("Error sending frame: "+e.getMessage());
										System.exit(0);
									}
								break;
							}
							//System.out.println(" ("+code+") <- Key");
							//System.out.flush();
						}
					}
				}
				catch (Exception e) { System.exit(0); }
			}
		} // timer
	} // LibretroIO

	private void settingsChanged()
	{
		int w = Integer.parseInt(config.settings.get("width"));
		int h = Integer.parseInt(config.settings.get("height"));

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

		String rotate = config.settings.get("rotate");
		if(rotate.equals("on")) { rotateDisplay = true; frameHeader[5] = (byte)1; }
		if(rotate.equals("off")) { rotateDisplay = false; frameHeader[5] = (byte)0; }

		String midiSoundfont = config.settings.get("soundfont");
		if(midiSoundfont.equals("Custom"))  { PlatformPlayer.customMidi = true; }
		else if(midiSoundfont.equals("Default")) { PlatformPlayer.customMidi = false; }

		halveCanvasRes = false;
		if(config.settings.get("halveCanvasRes").equals("on")) 
		{ 
			halveCanvasRes = true;
			w /= 2;
			h /= 2;
		}

		if(lcdWidth != w || lcdHeight != h)
		{
			lcdWidth = w;
			lcdHeight = h;
			Mobile.getPlatform().resizeLCD(w, h);
			surface = new BufferedImage(lcdWidth, lcdHeight, BufferedImage.TYPE_3BYTE_BGR); // libretro display
			gc = (Graphics2D)surface.getGraphics();
		}

		Manager.updatePlayerNum((byte) Integer.parseInt(config.settings.get("maxmidistreams")));

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

	private int getMobileKeyJoy(int keycode)
	{
		// Input mappings that are expected to be the same on all control modes
		switch(keycode)
		{
			case 4: return Mobile.KEY_NUM9; // A
			case 5: return Mobile.KEY_NUM7; // B
			case 6: return Mobile.KEY_NUM0; // X
			case 10: return Mobile.KEY_NUM1; // L
			case 11: return Mobile.KEY_NUM3; // R
			case 12: return Mobile.KEY_STAR; // L2
			case 13: return Mobile.KEY_POUND; // R2
		}

		// These keys are overridden by the "useXControls" variables
		if(useNokiaControls)
		{
			switch(keycode)
			{
				case 0: return Mobile.NOKIA_UP; // Up
				case 1: return Mobile.NOKIA_DOWN; // Down
				case 2: return Mobile.NOKIA_LEFT; // Left
				case 3: return Mobile.NOKIA_RIGHT; // Right
				case 7: return Mobile.NOKIA_SOFT3; // Y
				case 8: return Mobile.NOKIA_SOFT2; // Start
				case 9: return Mobile.NOKIA_SOFT1; // Select
			}
		}
		if(useSiemensControls)
		{
			switch(keycode)
			{
				case 0: return Mobile.SIEMENS_UP; // Up
				case 1: return Mobile.SIEMENS_DOWN; // Down
				case 2: return Mobile.SIEMENS_LEFT; // Left
				case 3: return Mobile.SIEMENS_RIGHT; // Right
				case 7: return Mobile.SIEMENS_FIRE; // Y
				case 8: return Mobile.SIEMENS_SOFT2; // Start
				case 9: return Mobile.SIEMENS_SOFT1; // Select
			}
		}
		if(useMotorolaControls)
		{
			switch(keycode)
			{
				case 0: return Mobile.MOTOROLA_UP; // Up
				case 1: return Mobile.MOTOROLA_DOWN; // Down
				case 2: return Mobile.MOTOROLA_LEFT; // Left
				case 3: return Mobile.MOTOROLA_RIGHT; // Right
				case 7: return Mobile.MOTOROLA_FIRE; // Y
				case 8: return Mobile.MOTOROLA_SOFT2; // Start
				case 9: return Mobile.MOTOROLA_SOFT1; // Select
			}
		}
		else // Standard keycodes
		{
			switch(keycode)
			{
				case 0: return Mobile.KEY_NUM2; // Up
				case 1: return Mobile.KEY_NUM8; // Down
				case 2: return Mobile.KEY_NUM4; // Left
				case 3: return Mobile.KEY_NUM6; // Right
				case 7: return Mobile.KEY_NUM5; // Y
				case 8: return Mobile.NOKIA_SOFT2; // Start
				case 9: return Mobile.NOKIA_SOFT1; // Select
			}
		}

		return Mobile.KEY_NUM5;
	}
}
