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
package org.recompile.mobile;

import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.game.GameCanvas;

/*
	Mobile Platform
*/

public class MobilePlatform
{

	private static PlatformImage lcdFrontbuffer;
	private PlatformGraphics gcFrontbuffer;

	private static PlatformImage lcd;
	private PlatformGraphics gc;

	public static int lcdWidth;
	public static int lcdHeight;

	// Frame Limit Variables
	private long lastRenderTime = System.nanoTime();
	private long requiredFrametime = 0;
	private long elapsedTime = 0;
	private long sleepTime = 0;

	// Whether the user has toggled the ShowFPS option
	public static String showFPS = "Off";

	// Canvas command bar focus
	public static boolean focusCommandBar = true;
	public static long timeToUnfocus = 3000000000L; // Command bar is visible for 3 seconds

	public static boolean isLibretro = false;
	public static boolean appTerminated = false;

	public MIDletLoader loader;
	public static Displayable displayable;

	public String dataPath = "";
	public static String fileName = null;
	private static String kjxJadFileName = null; // Static so that we can delete the extracted jar and jad files if needed

	public volatile static int keyState = 0;
	public volatile static int vodafoneKeyState = 0;
	public volatile static int doJaKeyState = 0;

	// MobilePlatform will handle the input repeats as well
	public static boolean[] pressedKeys = new boolean[23];

	public static Runnable painter, postDraw;

	public MobilePlatform(int width, int height)
	{
		boolean isUsingValidEncoding = false;

		// Check whether we're using any of the valid encodings before starting the jar, otherwise we'll be defaulting to ISO_8859_1
		for (String encoding : Mobile.supportedEncodings)
		{
			if (encoding.equals(System.getProperty("file.encoding"))) { isUsingValidEncoding = true; }
		}

		if(!isUsingValidEncoding)
		{
			Mobile.textEncoding = Mobile.supportedEncodings[Mobile.ISO_8859_1];
			checkFileEncoding();
		}

		resizeLCD(width, height);

		painter = new Runnable()
		{
			public void run()
			{
				// Placeholder //
			}
		};

	}

	public void resizeLCD(int width, int height)
	{
		// No need to waste time here if the screen dimensions haven't changed (screen was just rotated for example)
		if(lcdWidth == width && lcdHeight == height) { return; }

		lcdWidth = width;
		lcdHeight = height;

		org.recompile.mobile.PlatformFont.setScreenSize(width, height);

		lcdFrontbuffer = new PlatformImage(width, height);
		lcd = new PlatformImage(width, height);


        gcFrontbuffer = lcdFrontbuffer.getMIDPGraphics();


		/*
		 * Try to have the jar scale as well. If this doesn't work,
		 * a simple restart is all it takes, just like before.
		 */

		if (!Mobile.isDoJa)
		{
			gc = lcd.getMIDPGraphics();
			com.xce.lcdui.XDisplay.width = width;
			com.xce.lcdui.XDisplay.height2 = height;
			com.xce.lcdui.XDisplay.platformImage = lcd;
			com.xce.lcdui.Toolkit.graphics = (Graphics) gc;

			if (Mobile.getDisplay() != null && Mobile.getDisplay().getCurrent() != null)
			{
				Mobile.getDisplay().getCurrent().doSizeChanged(width, height);
				Mobile.getDisplay().getCurrent().platformImage = lcd;
				Mobile.getDisplay().getCurrent().graphics = (Graphics) gc;
			}
		}
		else if(Mobile.isDoJa && com.nttdocomo.ui.Display.getCurrent() != null) // Doja's current Frames (Displayables) are static
		{
			com.nttdocomo.ui.Display.getCurrent().platformImage = lcd;
			com.nttdocomo.ui.Display.getCurrent().graphics = lcd.getDoJaGraphics();
		}

	}

	public static PlatformImage getLcdBackbuffer() { return lcd; }

	public PlatformImage getLcdFrontbuffer() { return lcdFrontbuffer; }

	public Graphics getLcdFrontbufferGraphics() { return (Graphics) gcFrontbuffer; }

	public void setPainter(Runnable r) { painter = r; }

	public static void pauseResumeApp()
	{
		if(!Mobile.isDoJa)
		{
			displayable = Mobile.getDisplay().getCurrent();
			if (!(displayable instanceof Canvas)) { return; }

			if(!Mobile.isPaused)
			{
				((Canvas) displayable).hideNotify();

				try { Mobile.midlet.callPauseApp(); }
				catch (Exception e) { e.printStackTrace(); }

				Mobile.isPaused = true;

				painter.run();
			}
			else
			{
				Mobile.isPaused = false;

				((Canvas) displayable).showNotify();

				try { Mobile.midlet.callStartApp(); }
				catch (Exception e) { e.printStackTrace(); }

				painter.run();
			}
		}
		else
		{
			// TODO: DoJa pause/resume
		}
	}

	public static void keyPressed(final int keycode)
	{
		if(appTerminated) { return; }

		if(!MIDletLoader.MIDletSelected) { MIDletLoader.keyPress(Mobile.getGameAction(keycode)); }
		else if (!Mobile.isPaused)
		{
			updateKeyState(Mobile.getGameAction(keycode), true);
			updateVodafoneKeyState(Mobile.getCanvasAction(keycode), true);
			updateDoJaKeyState(Mobile.getCanvasAction(keycode), true);
			if (!Mobile.isDoJa && Mobile.getDisplay() != null && (displayable = Mobile.getDisplay().getCurrent()) != null)
			{
				Mobile.getDisplay().postInputEvent(new Runnable()
				{
					@Override
					public void run()
					{
						if(!handleCommands(Mobile.getCanvasAction(keycode)))
						{
							if(displayable instanceof Canvas && !((Canvas) displayable).areKeysSuppressed()) { displayable.keyPressed(keycode); }
						}
					}
				});
			}
		}
	}

	public static void keyReleased(final int keycode)
	{
		if(appTerminated) { return; }

		if(!Mobile.isPaused && MIDletLoader.MIDletSelected)
		{
			updateKeyState(Mobile.getGameAction(keycode), false);
			updateVodafoneKeyState(Mobile.getCanvasAction(keycode), false);
			updateDoJaKeyState(Mobile.getCanvasAction(keycode), false);
			if (!Mobile.isDoJa && Mobile.getDisplay() != null && (displayable = Mobile.getDisplay().getCurrent()) != null && MIDletLoader.MIDletSelected)
			{
				Mobile.getDisplay().postInputEvent(new Runnable()
				{
					@Override
					public void run()
					{
						if(displayable instanceof Canvas && !((Canvas) displayable).areKeysSuppressed()) { displayable.keyReleased(keycode); }
					}
				});
			}
		}
	}

	public static void keyRepeated(final int keycode)
	{
		if(appTerminated) { return; }

		if (!Mobile.isPaused && MIDletLoader.MIDletSelected && !Mobile.isDoJa && Mobile.getDisplay() != null && (displayable = Mobile.getDisplay().getCurrent()) != null)
		{
			Mobile.getDisplay().postInputEvent(new Runnable()
			{
				@Override
				public void run()
				{
					if(!handleCommands(Mobile.getCanvasAction(keycode)))
					{
						if(displayable instanceof Canvas && !((Canvas) displayable).areKeysSuppressed()) { displayable.keyRepeated(keycode); }
					}
				}
			});
		}
		// TODO: DoJa
	}

	public static void pointerDragged(final int x, final int y)
	{
		if(appTerminated) { return; }

		if (!Mobile.isPaused && MIDletLoader.MIDletSelected && !Mobile.isDoJa && Mobile.getDisplay() != null && (displayable = Mobile.getDisplay().getCurrent()) != null)
		{
			Mobile.getDisplay().postInputEvent(new Runnable()
			{
				@Override
				public void run() { displayable.pointerDragged(x, y); }
			});
		}
		// TODO: DoJa
	}

	public static void pointerPressed(final int x, final int y)
	{
		if(appTerminated) { return; }

		if (!Mobile.isPaused && MIDletLoader.MIDletSelected && !Mobile.isDoJa && Mobile.getDisplay() != null && (displayable = Mobile.getDisplay().getCurrent()) != null)
		{
			Mobile.getDisplay().postInputEvent(new Runnable()
			{
				@Override
				public void run() { displayable.pointerPressed(x, y); }
			});
		}
		else if(Mobile.isDoJa)
		{
			com.nttdocomo.opt.ui.PointingDevice.setX(x);
			com.nttdocomo.opt.ui.PointingDevice.setY(y);
		}

		// TODO: DoJa
	}

	public static void pointerReleased(final int x, final int y)
	{
		if(appTerminated) { return; }

		if (!Mobile.isPaused && MIDletLoader.MIDletSelected && !Mobile.isDoJa && Mobile.getDisplay() != null && (displayable = Mobile.getDisplay().getCurrent()) != null)
		{
			Mobile.getDisplay().postInputEvent(new Runnable()
			{
				@Override
				public void run() { displayable.pointerReleased(x, y); }
			});
		}
		else if(Mobile.isDoJa)
		{
			com.nttdocomo.opt.ui.PointingDevice.setX(-1);
			com.nttdocomo.opt.ui.PointingDevice.setY(-1);
		}
		// TODO: DoJa
	}

	private static void updateKeyState(int key, boolean pressed)
	{
		int mask=0;
		switch (key)
		{
			case Canvas.KEY_NUM2: mask = GameCanvas.UP_PRESSED;     break;
			case Canvas.KEY_NUM4: mask = GameCanvas.LEFT_PRESSED;   break;
			case Canvas.KEY_NUM6: mask = GameCanvas.RIGHT_PRESSED;  break;
			case Canvas.KEY_NUM8: mask = GameCanvas.DOWN_PRESSED;   break;
			case Canvas.KEY_NUM5: mask = GameCanvas.FIRE_PRESSED;   break;
			case Canvas.GAME_A:   mask = GameCanvas.GAME_A_PRESSED; break;
			case Canvas.GAME_B:   mask = GameCanvas.GAME_B_PRESSED; break;
			case Canvas.GAME_C:   mask = GameCanvas.GAME_C_PRESSED; break;
			case Canvas.GAME_D:   mask = GameCanvas.GAME_D_PRESSED; break;
			case Canvas.UP:       mask = GameCanvas.UP_PRESSED;     break;
			case Canvas.LEFT:     mask = GameCanvas.LEFT_PRESSED;   break;
			case Canvas.RIGHT:    mask = GameCanvas.RIGHT_PRESSED;  break;
			case Canvas.DOWN:     mask = GameCanvas.DOWN_PRESSED;   break;
			case Canvas.FIRE:     mask = GameCanvas.FIRE_PRESSED;   break;
		}
		if(pressed) { keyState |= mask; }
		else { keyState ^= mask; }
	}

	// Original implementation by Yury Kharchenko (J2ME-Loader)
	private static void updateVodafoneKeyState(int key, boolean pressed)
	{
		int mask=0;
		switch (key)
		{
			case Canvas.UP:
				mask = 1 << 12; // 12 Up
				break;
			case Canvas.LEFT:
				mask = 1 << 13; // 13 Left
				break;
			case Canvas.RIGHT:
				mask = 1 << 14; // 14 Right
				break;
			case Canvas.DOWN:
				mask = 1 << 15; // 15 Down
				break;
			case Canvas.FIRE:
				mask = 1 << 16; // 16 Select
				break;
			case Canvas.KEY_NUM0:
				mask = 1; //  0 0
				break;
			case Canvas.KEY_NUM1:
				mask = 1 << 1; //  1 1
				break;
			case Canvas.KEY_NUM2:
				mask = 1 << 2; //  2 2
				break;
			case Canvas.KEY_NUM3:
				mask = 1 << 3; //  3 3
				break;
			case Canvas.KEY_NUM4:
				mask = 1 << 4; //  4 4
				break;
			case Canvas.KEY_NUM5:
				mask = 1 << 5; //  5 5
				break;
			case Canvas.KEY_NUM6:
				mask = 1 << 6; //  6 6
				break;
			case Canvas.KEY_NUM7:
				mask = 1 << 7; //  7 7
				break;
			case Canvas.KEY_NUM8:
				mask = 1 << 8; //  8 8
				break;
			case Canvas.KEY_NUM9:
				mask = 1 << 9; //  9 9
				break;
			case Canvas.KEY_STAR:
				mask = 1 << 10; // 10 *
				break;
			case Canvas.KEY_POUND:
				mask = 1 << 11; // 11 #
				break;
			case Canvas.KEY_SOFT_LEFT:
				mask = 1 << 17; // 17 Softkey 1
				break;
			case Canvas.KEY_SOFT_RIGHT:
				mask = 1 << 18; // 18 Softkey 2
				break;
			default:
				mask = 0;
		}
		if(pressed) { vodafoneKeyState |= mask; }
		else { vodafoneKeyState ^= mask; }
	}

	// For a reference of these shift values, look into com.nttdocomo.ui.Display
	private static void updateDoJaKeyState(int key, boolean pressed)
	{
		int mask = 0, eventKey = 0;
		switch (key)
		{
			case Canvas.UP:
				mask = 1 << 0x11;
				eventKey = com.nttdocomo.ui.Display.KEY_UP;
				break;
			case Canvas.LEFT:
				mask = 1 << 0x10;
				eventKey = com.nttdocomo.ui.Display.KEY_LEFT;
				break;
			case Canvas.RIGHT:
				mask = 1 << 0x12;
				eventKey = com.nttdocomo.ui.Display.KEY_RIGHT;
				break;
			case Canvas.DOWN:
				mask = 1 << 0x13;
				eventKey = com.nttdocomo.ui.Display.KEY_DOWN;
				break;
			case Canvas.FIRE: // Doubles as KDDI_CLR
				mask = 1 << 0x14;
				eventKey = com.nttdocomo.ui.Display.KEY_SELECT;
				break;
			case Canvas.KEY_NUM0:
				mask = 1;
				eventKey = com.nttdocomo.ui.Display.KEY_0;
				break;
			case Canvas.KEY_NUM1:
				mask = 1 << 1;
				eventKey = com.nttdocomo.ui.Display.KEY_1;
				break;
			case Canvas.KEY_NUM2:
				mask = 1 << 2;
				eventKey = com.nttdocomo.ui.Display.KEY_2;
				break;
			case Canvas.KEY_NUM3:
				mask = 1 << 3;
				eventKey = com.nttdocomo.ui.Display.KEY_3;
				break;
			case Canvas.KEY_NUM4:
				mask = 1 << 4;
				eventKey = com.nttdocomo.ui.Display.KEY_4;
				break;
			case Canvas.KEY_NUM5:
				mask = 1 << 5;
				eventKey = com.nttdocomo.ui.Display.KEY_5;
				break;
			case Canvas.KEY_NUM6:
				mask = 1 << 6;
				eventKey = com.nttdocomo.ui.Display.KEY_6;
				break;
			case Canvas.KEY_NUM7:
				mask = 1 << 7;
				eventKey = com.nttdocomo.ui.Display.KEY_7;
				break;
			case Canvas.KEY_NUM8:
				mask = 1 << 8;
				eventKey = com.nttdocomo.ui.Display.KEY_8;
				break;
			case Canvas.KEY_NUM9:
				mask = 1 << 9;
				eventKey = com.nttdocomo.ui.Display.KEY_9;
				break;
			case Canvas.KEY_STAR:
				mask = 1 << 0x0a;
				eventKey = com.nttdocomo.ui.Display.KEY_ASTERISK;
				break;
			case Canvas.KEY_POUND:
				mask = 1 << 0x0b;
				eventKey = com.nttdocomo.ui.Display.KEY_POUND;
				break;
			case Canvas.KEY_SOFT_LEFT:
				mask = 1 << 0x15;
				eventKey = com.nttdocomo.ui.Display.KEY_SOFT1;
				break;
			case Canvas.KEY_SOFT_RIGHT:
				mask = 1 << 0x16;
				eventKey = com.nttdocomo.ui.Display.KEY_SOFT2;
				break;
			// TODO: This case might not be needed for DoJa at all
			//case Mobile.KDDI_CLR:
			//	mask = 1 << 0x20;
			//	eventKey = com.nttdocomo.ui.Display.KEY_CLEAR;
			//	break;
			default:
				mask = 0;
		}

		boolean canvasPresent = (com.nttdocomo.ui.Display.getCurrent() != null && com.nttdocomo.ui.Display.getCurrent() instanceof com.nttdocomo.ui.Canvas);

		if(pressed)
		{
			doJaKeyState |= mask;
			if(canvasPresent)
			{
				((com.nttdocomo.ui.Canvas)com.nttdocomo.ui.Display.getCurrent()).processEvent(com.nttdocomo.ui.Display.KEY_PRESSED_EVENT, eventKey);
			}
		}
		else // Send the released event BEFORE changing the mask (or else this will always send 0 as the key value)
		{
			if(canvasPresent)
			{
				((com.nttdocomo.ui.Canvas)com.nttdocomo.ui.Display.getCurrent()).processEvent(com.nttdocomo.ui.Display.KEY_RELEASED_EVENT, eventKey);
			}
			doJaKeyState ^= mask;
		}
	}

	// MIDP Spec dictates that only Canvas (and CustomItem) keys should be serialized, so i'll assume that these commands don't need to as they're usually meant for other LCDUI displayables
	private static boolean handleCommands(int key)
	{
		boolean canvasFullscreen = false, notCanvas = true; // Default to false, as all other displayables can show commands at all times

		if(displayable instanceof Canvas)
		{
			canvasFullscreen = ((Canvas)displayable).getFullScreen();
			notCanvas = false;
		}

		if((!canvasFullscreen && !displayable.commands.isEmpty()) || notCanvas)
		{
			if (displayable.listCommands)
			{
				if(key == Canvas.KEY_NUM2 || key == Canvas.UP)
				{
					displayable.currentCommand--;
					if(displayable.currentCommand<0) { displayable.currentCommand = displayable.commands.size()-1; }
					displayable._invalidate();
					return true;
				}
				else if(key == Canvas.KEY_NUM8 || key == Canvas.DOWN)
				{
					displayable.currentCommand++;
					if(displayable.currentCommand>=displayable.commands.size()) { displayable.currentCommand = 0; }
					displayable._invalidate();
					return true;
				}
				else if (key == Canvas.KEY_SOFT_LEFT) // Left and Right soft commands do not need an explicit invalidate call
				{
					showCommandBar();
					displayable.doLeftCommand();
					return true;
				}
				else if (key == Canvas.KEY_SOFT_RIGHT)
				{
					showCommandBar();
					displayable.doRightCommand();
					return true;
				}
			}
			else
			{
				boolean handled = displayable.screenKeyPressed(key);
				if (!handled)
				{
					if (key == Canvas.KEY_SOFT_LEFT)
					{
						showCommandBar();
						displayable.doLeftCommand();
						return true;
					}
					else if (key == Canvas.KEY_SOFT_RIGHT)
					{
						showCommandBar();
						displayable.doRightCommand();
						return true;
					}
				}
			}
		}
		return false;
	}

/*
	******** Jar/Jad Loading ********
*/

	public boolean load(String fileName)
	{
        Map<String, String> descriptorProperties = new HashMap<String, String>();

		/*
		 * Java treats "!/" sequences as a pointer to a file inside a jar, which will cause
		 * issues with MIDletLoader, so convert exclamations beforehand to not confuse it.
		 */
		fileName = fileName.replaceAll("!", "%21");
		this.fileName = fileName;

		if(fileName.toLowerCase().contains(".kjx")) // KDDI KJX parser, originally from J2ME-Loader by @ohayoyogi
		{
			try
			{
				File testDir = new File(Mobile.tempKJXDir);
				if(!testDir.isDirectory())
				{
					try
					{
						testDir.mkdirs();
					}
					catch(Exception e) { Mobile.log(Mobile.LOG_ERROR, MobilePlatform.class.getPackage().getName() + "." + MobilePlatform.class.getSimpleName() + ": " + "Failed to create KDDI temp dir:" + e.getMessage()); }
				}

				File kjxFile = new File(new URI(fileName));
				File tmpfile = null;

				InputStream inputStream = new FileInputStream(kjxFile);
				DataInputStream dis = new DataInputStream(inputStream);
				byte[] magic = new byte[3];
				dis.read(magic, 0, 3);
				if (!Arrays.equals(magic, "KJX".getBytes()))
				{
					throw new Exception("KJX Header string does not match: " + new String(magic));
				}

				byte startJadPos = dis.readByte();
				byte lenKjxFileName = dis.readByte();
				dis.skipBytes(lenKjxFileName);
				int lenJadFileContent = dis.readUnsignedShort();
				byte lenJadFileName = dis.readByte();
				byte[] jadFileName = new byte[lenJadFileName];
				dis.read(jadFileName, 0, lenJadFileName);
				kjxJadFileName = new String(jadFileName);

				int bufSize = 2048;
				byte[] buf = new byte[bufSize];

				// Write jad and parse its descriptors
				tmpfile = new File(Mobile.tempKJXDir, kjxJadFileName);
				try
				{
					FileOutputStream fos = new FileOutputStream(tmpfile);
					int restSize = lenJadFileContent;
					while(restSize > 0)
					{
						int readSize = dis.read(buf, 0, Math.min(restSize, bufSize));
						fos.write(buf, 0, readSize);
						restSize -= readSize;
					}
					fos.close();
				}
				catch(Exception e)
				{
					Mobile.log(Mobile.LOG_ERROR, MobilePlatform.class.getPackage().getName() + "." + MobilePlatform.class.getSimpleName() + ": " + "Failed to prepare kjx jad data: " + e.getMessage());
					return false;
				}

				try
				{
					InputStream targetStream = new FileInputStream(tmpfile);
					MIDletLoader.parseDescriptorInto(targetStream, descriptorProperties);
					targetStream.close();
				}
				catch (IOException e)
				{
					Mobile.log(Mobile.LOG_ERROR, MobilePlatform.class.getPackage().getName() + "." + MobilePlatform.class.getSimpleName() + ": " + "Failed to load kjx jad data: " + e.getMessage());
					return false;
				}

				// Write jar
				tmpfile = new File(Mobile.tempKJXDir, kjxJadFileName.substring(0, kjxJadFileName.length() -4) + ".jar");
				try
				{
					FileOutputStream fos = new FileOutputStream(tmpfile);
					int length = 0;
					while((length = dis.read(buf)) > 0) { fos.write(buf, 0, length); }
					fos.close();
				}
				catch(Exception e)
				{
					Mobile.log(Mobile.LOG_ERROR, MobilePlatform.class.getPackage().getName() + "." + MobilePlatform.class.getSimpleName() + ": " + "Failed to load kjx jar data: " + e.getMessage());
					return false;
				}

				// Send dumped jar path to loader
				URL jar = tmpfile.toURI().toURL();
				loader = new MIDletLoader(jar, descriptorProperties);
				if (Mobile.isDoJa)
				{
					Mobile.textEncoding = "Shift_JIS";
					MobilePlatform.checkFileEncoding();
				}
				Mobile.config.init(loader.suitename);

				return true;
			}
			catch (Exception e) { Mobile.log(Mobile.LOG_INFO, MobilePlatform.class.getPackage().getName() + "." + MobilePlatform.class.getSimpleName() + ": " + "Couldn't load KJX file:" + e.getMessage()); return false; }
		}
		else // If it's not KJX, it's JAD/MSD or JAR
		{
			/*
			 * If loading a jar directly, check if an accompanying jad/msd with the same name
			 * is present in the directory, to load any platform properties from there.
			 */
			if (fileName.toLowerCase().contains(".jar"))
			{
				try
				{
					// Create a File object for the directory containing the JAR file
					final File jarFile = new File(new URI(fileName));
					final File jarDirectory = jarFile.getParentFile();

					// Check for accompanying JAD or MSD files
					if (jarDirectory != null && jarDirectory.isDirectory())
					{
						FilenameFilter filter = new FilenameFilter()
						{
							public boolean accept(File dir, String name)
							{
								return name.equalsIgnoreCase(jarFile.getName().replace(".jar", ".jad")) ||
									name.equalsIgnoreCase(jarFile.getName().replace(".jar", ".msd"));
							}
						};

						File[] files = jarDirectory.listFiles(filter);

						if (files != null)
						{
							for (File file : files)
							{
								if (file.exists() && !file.isDirectory())
								{
									if (file.getName().toLowerCase().endsWith(".jad"))
									{
										Mobile.log(Mobile.LOG_INFO, MobilePlatform.class.getPackage().getName() + "." + MobilePlatform.class.getSimpleName() + ": " + "Accompanying JAD found! Parsing additional MIDlet properties.");
										fileName = file.toURI().toString();
									}
									else if (file.getName().toLowerCase().endsWith(".msd")) // We assume there will never be a jad and a msd for the same app in the directory
									{
										Mobile.log(Mobile.LOG_INFO, MobilePlatform.class.getPackage().getName() + "." + MobilePlatform.class.getSimpleName() + ": " + "Accompanying MSD found! Parsing additional MIDlet properties.");
										fileName = file.toURI().toString();
									}
									break;
								}
							}
						}
					}
				}
				catch (Exception e) { Mobile.log(Mobile.LOG_INFO, MobilePlatform.class.getPackage().getName() + "." + MobilePlatform.class.getSimpleName() + ": " + "Couldn't check for accompanying JAD/MSD:" + e.getMessage()); }
			}

			boolean isMsd = fileName.toLowerCase().endsWith(".msd");
			boolean isJad = fileName.toLowerCase().endsWith(".jad");

			if (isJad || isMsd)
			{
				if (isMsd)
				{
					Mobile.isSKT = true;
					Mobile.textEncoding = "EUC_KR";
					MobilePlatform.checkFileEncoding();
				}

				String preparedFileName = fileName.replace("file:", "").trim();
				try { preparedFileName = URLDecoder.decode(preparedFileName, Mobile.textEncoding); }
				catch (Exception e)
				{
					System.err.println("Error decoding file name: " + e.getMessage());
					return false;
				}

				InputStream targetStream = null;
				try
				{
					targetStream = new FileInputStream(preparedFileName);
					try { MIDletLoader.parseDescriptorInto(targetStream, descriptorProperties); }
					finally { targetStream.close(); }
				}
				catch (IOException e)
				{
					Mobile.log(Mobile.LOG_ERROR, MobilePlatform.class.getPackage().getName() + "." + MobilePlatform.class.getSimpleName() + ": " + "Failed to load descriptor data: " + e.getMessage());
					return false;
				}

				// JAD/MSD file was parsed, so get the jar path and load it next

				// String jarUrl = descriptorProperties.getOrDefault("MIDlet-Jar-URL", preparedFileName.replace(".jad", ".jar"));

				// We will not support downloading jars from the internet on the fly,
				// unless there is a very good reason to do so.
				// Also, unless the jad has a URI for loading the jar, ignore the path as well

				// Just try getting the jar in the same directory as the jad in those cases.
				fileName = fileName.substring(0, fileName.lastIndexOf('.')) + ".jar";
			}

			try
			{
				URL jar = new URL(fileName);
				loader = new MIDletLoader(jar, descriptorProperties);
				if (Mobile.isDoJa)
				{
					Mobile.textEncoding = Mobile.supportedEncodings[Mobile.SHIFT_JIS];
					MobilePlatform.checkFileEncoding();
				}
				Mobile.config.init(loader.suitename);

				return true;
			}
			catch (Exception e)
			{
				Mobile.log(Mobile.LOG_ERROR, MobilePlatform.class.getPackage().getName() + "." + MobilePlatform.class.getSimpleName() + ": " + "Failed to load Jar: " + e.getMessage());
				e.printStackTrace();
				return false;
			}
		}
    }

	public void runJar()
	{
		try
		{
			if(Mobile.deleteTemporaryKJXFiles && kjxJadFileName != null)
			{
				File tmpfile = new File(Mobile.tempKJXDir, kjxJadFileName.substring(0, kjxJadFileName.length() -4) + ".jar");
				tmpfile.delete(); // Delete the temporary jar file
				tmpfile = new File(Mobile.tempKJXDir, kjxJadFileName);
				tmpfile.delete(); // Delete the temporary jad file
			}

			/*
			 * Load up everything needed to play sound before the jar opens to minimize ingame stutters
			 * this basically just loads up the synthesizers, as they're the biggest troublemakers.
			 */
			javax.microedition.media.Manager.prepareMediaEngine();

			loader.start();
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, MobilePlatform.class.getPackage().getName() + "." + MobilePlatform.class.getSimpleName() + ": " + "Error Running Jar");
			e.printStackTrace();
		}
	}

	public static void checkFileEncoding()
	{
		if(!System.getProperty("file.encoding").equals(Mobile.textEncoding))
		{
			Mobile.log(Mobile.LOG_INFO, MobilePlatform.class.getPackage().getName() + "." + MobilePlatform.class.getSimpleName() + ": " + "different encoding: " + System.getProperty("file.encoding") + " while it should be " + Mobile.textEncoding + ". Restarting freeJ2ME to apply new encoding");
			Mobile.restartApp();
		}
	}

/*
	********* Graphics ********
*/

	public final void flushGraphics(PlatformImage img, int x, int y, int width, int height)
	{
		if(!Mobile.isPaused && !appTerminated)
		{
			/*
			 * This must be Synchronized so a frontend reading the frontbuffer
			 * (e.g. the libretro pipe serializer) never observes a half-flushed
			 * frame (tearing).
			 */
			synchronized (lcdFrontbuffer)
			{
				gcFrontbuffer.flushGraphics(img, x, y, width, height);
			}
			if(postDraw != null) { postDraw.run(); postDraw = null; }
			painter.run();

			if(focusCommandBar)
			{
				timeToUnfocus -= (System.nanoTime()-lastRenderTime);
				if(timeToUnfocus <= 0) { focusCommandBar = false; }
			}

			Mobile.getPlatform().limitFps();
		}
	}

	public final void drawAppTerminated()
	{
		if(!isLibretro)
		{
			gcFrontbuffer.setColor(0, 0, 64);
			gcFrontbuffer.fillRect(0, 0, lcdWidth, lcdHeight);
			gcFrontbuffer.setColor(0xFFFFAF00);
			gcFrontbuffer.drawString("APP TERMINATED!", lcdWidth/2, lcdHeight/10, org.recompile.mobile.PlatformGraphics.HCENTER);
			gcFrontbuffer.drawString("Open a new one", lcdWidth/2, lcdHeight/10 + javax.microedition.lcdui.Font.getDefaultFont().getHeight()+2, org.recompile.mobile.PlatformGraphics.HCENTER);
			gcFrontbuffer.drawString("through Drag-Drop", lcdWidth/2, lcdHeight/10 + 2*javax.microedition.lcdui.Font.getDefaultFont().getHeight()+2, org.recompile.mobile.PlatformGraphics.HCENTER);
			gcFrontbuffer.drawString("or 'File->Open'.", lcdWidth/2, lcdHeight/10 + 3*javax.microedition.lcdui.Font.getDefaultFont().getHeight()+2, org.recompile.mobile.PlatformGraphics.HCENTER);
		}
		else
		{
			gcFrontbuffer.setColor(0,0,0);
			gcFrontbuffer.fillRect(0, 0, lcdWidth, lcdHeight);
		}

		appTerminated = true;

		painter.run();
	}

	public void limitFps()
	{
		if(Mobile.limitFPS == 0 || pressedKeys[20]) { lastRenderTime = System.nanoTime(); return; }

		requiredFrametime = 1000000000 / Mobile.limitFPS;
		elapsedTime = System.nanoTime() - lastRenderTime;
		sleepTime = (requiredFrametime - elapsedTime); // Sleep time in nanoseconds

		if (sleepTime > 0) { LockSupport.parkNanos(sleepTime); }

		lastRenderTime = System.nanoTime();
	}

	public void setShowFPS(String show) { showFPS = show; }

	public static void showCommandBar()
	{
		focusCommandBar = true;
		timeToUnfocus = 3000000000L;
	}

	// LCDUI command bar and other "overlay" renders:

	public void setPostFlushDraw(Runnable r) { postDraw = r; }
}
