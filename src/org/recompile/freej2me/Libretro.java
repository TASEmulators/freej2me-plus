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

import java.awt.image.DataBufferInt;
import java.util.Timer;
import java.util.TimerTask;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.microedition.media.Manager;
import javax.microedition.midlet.MIDlet;

public class Libretro
{
	private int lcdWidth;
	private int lcdHeight;

	private Config config;
	private boolean rotateDisplay = false;
	private boolean soundEnabled = true;

	private boolean[] pressedKeys = new boolean[128];

	private byte[] frameBuffer = new byte[800*800*3];
	private final byte[] frameHeader = new byte[]{(byte)0xFE, 0, 0, 0, 0, 0, 0, 0, 0, 0};

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
		Mobile.clearOldLog();
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
			if(!Manager.soundfontDir.isDirectory()) 
			{ 
				Manager.soundfontDir.mkdirs();
				File dummyFile = new File(Manager.soundfontDir.getPath() + File.separatorChar + "Put your sf2 bank here");
				dummyFile.createNewFile();
			}
		}
		catch(IOException e) { Mobile.log(Mobile.LOG_ERROR, Libretro.class.getPackage().getName() + "." + Libretro.class.getSimpleName() + ": " + "Failed to create custom midi info file:" + e.getMessage()); }

		/* 
		 * Checks if the arguments were received from the commandline -> width, height, rotate, phonetype, fps, sound, ...
		 * 
		 * NOTE:
		 * Due to differences in how linux and win32 pass their cmd arguments, we can't explictly check for a given size
		 * on the argv array. Linux includes the "java", "-jar" and "path/to/freej2me" into the array while WIN32 doesn't.
		 */
		lcdWidth =  Integer.parseInt(args[0]);
		lcdHeight = Integer.parseInt(args[1]);

		if(Integer.parseInt(args[2]) == 1) { rotateDisplay = true; }

		if(Integer.parseInt(args[3]) == 1)      { Mobile.lg = true;    }
		else if(Integer.parseInt(args[3]) == 2) { Mobile.motorola = true;  }
		else if(Integer.parseInt(args[3]) == 3) { Mobile.motoTriplets = true; }
		else if(Integer.parseInt(args[3]) == 4) { Mobile.motoV8 = true; }
		else if(Integer.parseInt(args[3]) == 5) { Mobile.nokia = true; }
		else if(Integer.parseInt(args[3]) == 6) { Mobile.nokiaKeyboard = true; }
		else if(Integer.parseInt(args[3]) == 7) { Mobile.sagem = true; }
		else if(Integer.parseInt(args[3]) == 8) { Mobile.siemens = true; }

		Mobile.lg = false;
		Mobile.motorola = false;
		Mobile.motoTriplets = false;
		Mobile.motoV8 = false;
		Mobile.nokia = false;
		Mobile.nokiaKeyboard = false;
		Mobile.sagem = false;
		Mobile.siemens = false;

		Mobile.limitFPS = Integer.parseInt(args[4]);

		if(Integer.parseInt(args[5]) == 0) { soundEnabled = false; }

		if(Integer.parseInt(args[6]) == 1) { Manager.useCustomMidi = true; }

		/* Dump Audio Streams will not be a per-game FreeJ2ME config, so it will have to be set every time for now */
		if(Integer.parseInt(args[7]) == 1) { Manager.dumpAudioStreams = true; }

		/* Same for Logging Level */
		if(Integer.parseInt(args[8]) == 0) { Mobile.logging = false; }
		else { Mobile.logging = true; Mobile.minLogLevel = (byte) (Integer.parseInt(args[8])-1); }

		/* No Alpha on Blank Images SpeedHack is a per-game config */
		if(Integer.parseInt(args[9]) == 0) { Mobile.noAlphaOnBlankImages = false; }
		else { Mobile.noAlphaOnBlankImages = true; }

		/* Once it finishes parsing all arguments, it's time to set up freej2me-lr */

		Mobile.setPlatform(new MobilePlatform(lcdWidth, lcdHeight));

		config = new Config();
		config.onChange = new Runnable() { public void run() { settingsChanged(); } };

		lio = new LibretroIO();

		lio.start();
		
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

										if(rotateDisplay)   { config.settings.put("rotate", "on");  }
										if(!rotateDisplay)  { config.settings.put("rotate", "off"); }

										if(Mobile.lg)                 { config.settings.put("phone", "LG");    }
										else if(Mobile.motorola)      { config.settings.put("phone", "Motorola");  }
										else if(Mobile.motoTriplets)  { config.settings.put("phone", "MotoTriplets"); }
										else if(Mobile.motoV8)        { config.settings.put("phone", "MotoV8"); }
										else if(Mobile.nokia)         { config.settings.put("phone", "Nokia"); }
										else if(Mobile.nokiaKeyboard) { config.settings.put("phone", "NokiaKeyboard"); }
										else if(Mobile.sagem)         { config.settings.put("phone", "Sagem"); }
										else if(Mobile.siemens)       { config.settings.put("phone", "Siemens"); }
										else                          { config.settings.put("phone", "Standard"); }

										if(soundEnabled)   { config.settings.put("sound", "on");  }
										if(!soundEnabled)  { config.settings.put("sound", "off"); }

										config.settings.put("fps", "" + Mobile.limitFPS);

										if(!Manager.useCustomMidi) { config.settings.put("soundfont", "Default"); }
										else                       { config.settings.put("soundfont", "Custom");  }

										if(!Mobile.noAlphaOnBlankImages) { config.settings.put("spdhacknoalpha", "off"); }
										else                             { config.settings.put("spdhacknoalpha", "on"); }

										config.saveConfig();
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

									if(Integer.parseInt(cfgtokens[3])==1) { config.settings.put("rotate", "on");  }
									if(Integer.parseInt(cfgtokens[3])==0) { config.settings.put("rotate", "off"); }

									if(Integer.parseInt(cfgtokens[4])==0) { config.settings.put("phone", "Standard"); }
									if(Integer.parseInt(cfgtokens[4])==1) { config.settings.put("phone", "LG");    }
									if(Integer.parseInt(cfgtokens[4])==2) { config.settings.put("phone", "Motorola");  }
									if(Integer.parseInt(cfgtokens[4])==3) { config.settings.put("phone", "MotoTriplets"); }
									if(Integer.parseInt(cfgtokens[4])==4) { config.settings.put("phone", "MotoV8"); }
									if(Integer.parseInt(cfgtokens[4])==5) { config.settings.put("phone", "Nokia"); }
									if(Integer.parseInt(cfgtokens[4])==6) { config.settings.put("phone", "NokiaKeyboard"); }
									if(Integer.parseInt(cfgtokens[4])==7) { config.settings.put("phone", "Sagem"); }
									if(Integer.parseInt(cfgtokens[4])==8) { config.settings.put("phone", "Siemens"); }

									config.settings.put("fps", ""+cfgtokens[5]);

									if(Integer.parseInt(cfgtokens[6])==1) { config.settings.put("sound", "on");  }
									if(Integer.parseInt(cfgtokens[6])==0) { config.settings.put("sound", "off"); }

									if(Integer.parseInt(cfgtokens[7])==0) { config.settings.put("soundfont", "Default"); }
									if(Integer.parseInt(cfgtokens[7])==1) { config.settings.put("soundfont", "Custom");  }

									if(Integer.parseInt(cfgtokens[8])==1) { Manager.dumpAudioStreams = true;  }
									if(Integer.parseInt(cfgtokens[8])==0) { Manager.dumpAudioStreams = false; }

									if(Integer.parseInt(cfgtokens[9])==0) { Mobile.logging = false;  }
									else { Mobile.logging = true; Mobile.minLogLevel = (byte) (Integer.parseInt(cfgtokens[9])-1); }

									if(Integer.parseInt(cfgtokens[10])==0) { Mobile.noAlphaOnBlankImages = false;  }
									else { Mobile.noAlphaOnBlankImages = true; }

									config.saveConfig();
									settingsChanged();
								break;
								
								case 15:
									/* Send Frame to Libretro */
									try
									{				
										//frameHeader[0] = (byte)0xFE;
										frameHeader[1] = (byte)((lcdWidth>>8)&0xFF);
										frameHeader[2] = (byte)((lcdWidth)&0xFF);
										frameHeader[3] = (byte)((lcdHeight>>8)&0xFF);
										frameHeader[4] = (byte)((lcdHeight)&0xFF);
										//frameHeader[5] = (byte)rotateDysplay; ( seen in settingsChanged() )
										frameHeader[6] = (byte)((Mobile.vibrationDuration>>24) & 0xFF);
										frameHeader[7] = (byte)((Mobile.vibrationDuration>>16) & 0xFF);
										frameHeader[8] = (byte)((Mobile.vibrationDuration>>8) & 0xFF);
										frameHeader[9] = (byte)((Mobile.vibrationDuration) & 0xFF);
										System.out.write(frameHeader, 0, 10);

										/* Vibration duration should be set to zero to prevent constant sends of the same data, so update it here*/
										Mobile.vibrationDuration = 0;

										final int[] data = ((DataBufferInt) Mobile.getPlatform().getLCD().getRaster().getDataBuffer()).getData();

										for(int i=0; i<data.length; i++)
										{
											frameBuffer[3*i]   = (byte)((data[i]>>16)&0xFF);
											frameBuffer[3*i+1] = (byte)((data[i]>>8)&0xFF);
											frameBuffer[3*i+2] = (byte)((data[i])&0xFF);
										}

										System.out.write(frameBuffer, 0, data.length*3);
										System.out.flush();
									}
									catch (Exception e)
									{
										Mobile.log(Mobile.LOG_DEBUG, Libretro.class.getPackage().getName() + "." + Libretro.class.getSimpleName() + ": " + "Error sending frame: "+e.getMessage());
										System.exit(0);
									}
								break;
							}
							Mobile.log(Mobile.LOG_DEBUG, Libretro.class.getPackage().getName() + "." + Libretro.class.getSimpleName() + ": " + " ("+code+") <- Key");
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

		Mobile.limitFPS = Integer.parseInt(config.settings.get("fps"));

		String sound = config.settings.get("sound");
		Mobile.sound = false;
		if(sound.equals("on")) { Mobile.sound = true; }

		String phone = config.settings.get("phone");
		Mobile.lg = false;
		Mobile.motorola = false;
		Mobile.motoTriplets = false;
		Mobile.motoV8 = false;
		Mobile.nokia = false;
		Mobile.nokiaKeyboard = false;
		Mobile.sagem = false;
		Mobile.siemens = false;
		if(phone.equals("LG"))            { Mobile.lg = true;}
		if(phone.equals("Motorola"))      { Mobile.motorola = true;}
		if(phone.equals("MotoTriplets"))  { Mobile.motoTriplets = true;}
		if(phone.equals("MotoV8"))        { Mobile.motoV8 = true;}
		if(phone.equals("Nokia"))         { Mobile.nokia = true;}
		if(phone.equals("NokiaKeyboard")) { Mobile.nokiaKeyboard = true;}
		if(phone.equals("Sagem"))         { Mobile.sagem = true;}
		if(phone.equals("Siemens"))       { Mobile.siemens = true;}

		String rotate = config.settings.get("rotate");
		if(rotate.equals("on")) { rotateDisplay = true; frameHeader[5] = (byte)1; }
		if(rotate.equals("off")) { rotateDisplay = false; frameHeader[5] = (byte)0; }

		String midiSoundfont = config.settings.get("soundfont");
		if(midiSoundfont.equals("Custom"))  { Manager.useCustomMidi = true; }
		else if(midiSoundfont.equals("Default")) { Manager.useCustomMidi = false; }

		String speedHackNoAlpha = config.settings.get("spdhacknoalpha");
		if(speedHackNoAlpha.equals("on")) { Mobile.noAlphaOnBlankImages = true; }
		else if (speedHackNoAlpha.equals("off")) { Mobile.noAlphaOnBlankImages = false; };

		if(lcdWidth != w || lcdHeight != h)
		{
			lcdWidth = w;
			lcdHeight = h;
			Mobile.getPlatform().resizeLCD(w, h);
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

	private int getMobileKeyJoy(int keycode) { return Mobile.getMobileKey(keycode, true); }
}
