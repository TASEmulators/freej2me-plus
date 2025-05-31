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

/*
	FreeJ2ME - AWT
*/

import org.recompile.mobile.Mobile;
import org.recompile.mobile.MobilePlatform;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

public class FreeJ2ME
{

	public static FreeJ2ME app;
	protected Frame main;
	private int lcdWidth;
	private int lcdHeight;
	private int scaleFactor = 1;

	private static final String extInputFilePath = "FreeJ2MEExternalKeyEvents.txt";
	private static HashMap<String, Integer> extEventsMap = new HashMap<String, Integer>();

	public static final Color freeJ2MEBGColor = new Color(0,0,64);
	public static final Color freeJ2MEDragColor = new Color(55, 55, 125);

	public static boolean isFullscreen = false;

	private LCD lcd;

	private int xborder;
	private int yborder;
	
	// AWT GUI
	private AWTGUI awtGUI;

	public static void main(String args[])
	{
		Mobile.clearOldLog();
		FreeJ2ME.app = new FreeJ2ME(args);

		// After FreeJ2ME is properly opened, start the external input thread
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				while (true)
				{
					checkExtInputFile();
					try { Thread.sleep(4); } // External inputs poll at a 250fps rate, more than fast enough for just about everything
					catch (InterruptedException e) { }
				}
			}
		}, "ExternalInputs-Thread").start();
	}

	private static void checkExtInputFile()
	{
			File extFile = new File("freej2me_system/"+extInputFilePath);

			// If File doesn't exist on the system dir, check if this is the web/CheerpJ frontend
			if(!extFile.exists()) { extFile = new File("/str/"+extInputFilePath); }

            if (extFile.exists()) { readFile(extFile.getPath()); }
    }

    private static void readFile(String filePath)
	{
        HashMap<String, Integer> newEventsMap = new HashMap<String, Integer>();

        try
		{
			BufferedReader br = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = br.readLine()) != null)
			{
                String[] parts = line.split(":");
                if (parts.length == 2)
				{
                    String key = parts[0].trim();
                    int value = Integer.parseInt(parts[1].trim());
                    newEventsMap.put(key, value);
                }
            }
			br.close();
        } catch (IOException e) { e.printStackTrace(); }

		// No changes to the file, so no external input changes either, return early (so that these don't override any internal events).
		if(extEventsMap.equals(newEventsMap)) { return; }

		extEventsMap = newEventsMap;

		// Parse external inputs:
		for (Map.Entry<String, Integer> entry : newEventsMap.entrySet())
		{
			String key = entry.getKey();
			Integer value = entry.getValue();
			switch(key.hashCode())
			{
				case 0x6B30:  // 0 - k0
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD0, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD0, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 0x6B31: // 1 - k1
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD1, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD1, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 0x6B32: // 2 (8 in keyboard numpad) - k2
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD8, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD8, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 0x6B33:
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD3, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD3, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 0x6B34:
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD4, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD4, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 0x6B35:
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD5, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD5, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 0x6B36:
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD6, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD6, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 0x6B37:
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD7, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD7, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 0x6B38:
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD2, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD2, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 0x6B39: // k9
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD9, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD9, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 0x6B2A: // k*
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_E, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_E, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 0x6B23: // k#
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_R, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_R, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 0x6B75: // Up - ku
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 0x6B64: // Down - kd
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 0x6B6C: // Left - kl
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 0x6B72: // Right - kr
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 0x6B63: // Fire - kc
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 0x6C73: // leftSoft - ls
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_Q, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_Q, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 0x7273: // rightSoft - rs
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_W, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_W, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 0x6666: // Fast-Forward - ff
					if(value == 1 && !Mobile.isFastForwarding) { Mobile.isFastForwarding = true; app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_SPACE, KeyEvent.CHAR_UNDEFINED), true); }
					else if(value == 0 && Mobile.isFastForwarding) { Mobile.isFastForwarding = false; app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_SPACE, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 0x726F: // Rotation - ro
					if(value == 1 && !Mobile.rotateDisplay)
					{
						Mobile.config.settings.put("rotate",  "on");
						app.settingsChanged();
					}
					else if(value == 0 && Mobile.rotateDisplay)
					{
						Mobile.config.settings.put("rotate",  "off");
						app.settingsChanged();
					}
					break;
				case 0x7061: // Pause - pa
					if(value == 1 && !Mobile.isPaused) { Mobile.isPaused = true; app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_X, KeyEvent.CHAR_UNDEFINED), true); }
					else if(value == 0 && Mobile.isPaused) { Mobile.isPaused = false; app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_X, KeyEvent.CHAR_UNDEFINED)); }
					break;
			}
		}
    }

	public static void closeApp()
	{
		try
		{
            String java = System.getProperty("java.home") + "/bin/java";
            String classPath = System.getProperty("java.class.path");

            String[] commands = new String[] { java, "-Dfile.encoding="+Mobile.textEncoding, "-cp", classPath, FreeJ2ME.class.getName() };

            // Start a new instance
            ProcessBuilder processBuilder = new ProcessBuilder(commands);
            processBuilder.start();

            // Exit the current instance
            System.exit(0);
        }
		catch (IOException e) { e.printStackTrace(); }
	}

	public FreeJ2ME(String args[])
	{
		// Setup Device //
		boolean fullscreenAtStartup = false;
		if(args.length>=2)
		{
			fullscreenAtStartup = (Integer.parseInt(args[1]) == 1);
		}

		if(args.length>=4)
		{
			Mobile.lcdWidth = Integer.parseInt(args[2]);
			Mobile.lcdHeight = Integer.parseInt(args[3]);
		}
		if(args.length>=5)
		{
			scaleFactor = Integer.parseInt(args[4]);
		}

		lcdWidth = Mobile.lcdWidth;
		lcdHeight = Mobile.lcdHeight;

		Mobile.setPlatform(new MobilePlatform(lcdWidth, lcdHeight), new Runnable() { public void run() { settingsChanged(); } });

		lcd = new LCD();
		lcd.setFocusable(true);

		awtGUI = new AWTGUI(Mobile.config);

		constructFreeJ2MEGUI();

		if(args.length>=1) // Only now we can load the jar passed as argument
		{
			try { awtGUI.loadJarFile(getFormattedLocation(URLDecoder.decode(args[0], Mobile.textEncoding))); }
			catch(Exception e) { }
		}

		/* Inputs should only be registered if a jar has been loaded, otherwise AWT will throw NullPointerException */
		lcd.addKeyListener(new KeyListener()
		{
			public void keyPressed(KeyEvent e) { pressKey(e, false); }

			public void keyReleased(KeyEvent e) { releaseKey(e); }

			public void keyTyped(KeyEvent e) { }

		});

		lcd.addMouseListener(new MouseListener()
		{

			public void mousePressed(MouseEvent e)
			{
				if(awtGUI.hasLoadedFile()) 
				{
					int x = (int)((e.getX()-lcd.cx) * lcd.scalex);
					int y = (int)((e.getY()-lcd.cy) * lcd.scaley);

					// Adjust the pointer coords if the screen is rotated, same for mouseReleased
					if(Mobile.rotateDisplay)
					{
						x = (int)((lcd.ch-(e.getY()-lcd.cy)) * lcd.scaley);
						y = (int)((e.getX()-lcd.cx) * lcd.scalex);
					}

					MobilePlatform.pointerPressed(x, y);
				}
			}

			public void mouseReleased(MouseEvent e)
			{
				if(awtGUI.hasLoadedFile()) 
				{
					int x = (int)((e.getX()-lcd.cx) * lcd.scalex);
					int y = (int)((e.getY()-lcd.cy) * lcd.scaley);

					if(Mobile.rotateDisplay)
					{
						x = (int)((lcd.ch-(e.getY()-lcd.cy)) * lcd.scaley);
						y = (int)((e.getX()-lcd.cx) * lcd.scalex);
					}

					MobilePlatform.pointerReleased(x, y);
				}
			}

			public void mouseExited(MouseEvent e) { }
			public void mouseEntered(MouseEvent e) { }
			public void mouseClicked(MouseEvent e) { }

		});

		lcd.addMouseMotionListener(new MouseMotionAdapter() 
		{
			public void mouseDragged(MouseEvent e)
			{
				if(awtGUI.hasLoadedFile()) 
				{
					int x = (int)((e.getX()-lcd.cx) * lcd.scalex);
					int y = (int)((e.getY()-lcd.cy) * lcd.scaley);

					if(Mobile.rotateDisplay)
					{
						x = (int)((lcd.ch-(e.getY()-lcd.cy)) * lcd.scaley);
						y = (int)((e.getX()-lcd.cx) * lcd.scalex);
					}
					
					MobilePlatform.pointerDragged(x, y);
				}
			}
		});

		displayGUI();

		// Set painter right before the jar is loaded
		Mobile.getPlatform().setPainter(new Runnable()
		{
			public void run()
			{
				/* Set menuBar option states based on loaded config */
				if(awtGUI.hasJustLoaded()) { awtGUI.updateOptions(); }

				/* Only update mem dialog's stats if it is visible */
				if(awtGUI.awtDialogs[2].isVisible()) { awtGUI.updateMemStatDialog(); }

				/* Whenever AWT GUI notifies that its menu options were changed, update settings */
				if(awtGUI.hasChanged()) { settingsChanged(); awtGUI.clearChanged(); }

				lcd.repaint();
			}
		});

		if(args.length<1)
		{
			while(!awtGUI.hasLoadedFile())
			{
				try{ Thread.sleep(1000); }
				catch (InterruptedException e) { }
			}
		}
		if(Mobile.getPlatform().load(awtGUI.getJarPath()))
		{
			/* Allows FreeJ2ME to set the width and height passed as cmd arguments. */
			if(args.length>=4)
			{
				lcdWidth = Integer.parseInt(args[1]);
				lcdHeight = Integer.parseInt(args[2]);
				Mobile.config.settings.put("width",  ""+lcdWidth);
				Mobile.config.settings.put("height", ""+lcdHeight);
			}

			if(args.length>=6)
			{
				if(Integer.parseInt(args[5]) == 0) { Mobile.config.settings.put("phone",  "Standard"); }
				if(Integer.parseInt(args[5]) == 1) { Mobile.config.settings.put("phone",  "LG"); }
				if(Integer.parseInt(args[5]) == 2) { Mobile.config.settings.put("phone",  "Motorola"); }
				if(Integer.parseInt(args[5]) == 3) { Mobile.config.settings.put("phone",  "MotoTriplets"); }
				if(Integer.parseInt(args[5]) == 4) { Mobile.config.settings.put("phone",  "MotoV8"); }
				if(Integer.parseInt(args[5]) == 5) { Mobile.config.settings.put("phone",  "NokiaKeyboard"); }
				if(Integer.parseInt(args[5]) == 6) { Mobile.config.settings.put("phone",  "Sagem"); }
				if(Integer.parseInt(args[5]) == 7) { Mobile.config.settings.put("phone",  "Siemens"); }
				if(Integer.parseInt(args[5]) == 8) { Mobile.config.settings.put("phone",  "Sharp"); }
			}

			if(args.length>=7)
			{
				Mobile.config.settings.put("fps", ""+Integer.parseInt(args[6])+"");
			}

			settingsChanged();

			Mobile.getPlatform().runJar();
		}
		else
		{
			Mobile.log(Mobile.LOG_ERROR, FreeJ2ME.class.getPackage().getName() + "." + FreeJ2ME.class.getSimpleName() + ": " + "Couldn't load jar...");
		}

		// Go fullscreen as soon as the jar is loaded from the commandline path above
		if(fullscreenAtStartup) { toggleFullscreen(); }
	}

	protected void pressKey(KeyEvent e, boolean ignoreModifiers)
	{
		if(awtGUI.hasLoadedFile())
		{
			int keycode = e.getKeyCode();
			int mobikey = getMobileKey(keycode);

			switch(keycode) // Handle emulator control keys
			{
				case KeyEvent.VK_EQUALS:
				case KeyEvent.VK_ADD:
					if(!isFullscreen)
					{
						scaleFactor++;
						main.setSize(lcdWidth * scaleFactor + xborder, lcdHeight * scaleFactor + yborder);
					}
				break;
				case KeyEvent.VK_MINUS:
				case KeyEvent.VK_SUBTRACT:
					if(scaleFactor > 1 && !isFullscreen)
					{
						scaleFactor--;
						main.setSize(lcdWidth * scaleFactor + xborder, lcdHeight * scaleFactor + yborder);
					}
				break;
				case KeyEvent.VK_F:
					if(e.isAltDown() && e.isControlDown())
					{
						toggleFullscreen();
					}
				break;
				case KeyEvent.VK_R: // Toggle rotation
					if(e.isAltDown() && e.isControlDown())
					{
						Mobile.config.settings.put("rotate",  (Mobile.rotateDisplay ? "off" : "on"));
						settingsChanged();
					}
				break;
			}

			if (mobikey == Integer.MIN_VALUE) // Ignore events from keys not mapped to a phone keypad key (AWTGUI does use 0, so this can't mirror libretro)
			{
				return;
			}

			if (MobilePlatform.pressedKeys[mobikey] == false)
			{
				if(mobikey < 19) // Anything over 19 are special keys (fast-forward, etc)
				{
					MobilePlatform.pressedKeys[mobikey] = true;
					MobilePlatform.keyPressed(Mobile.getMobileKey(mobikey));
				}
				else
				{
					if((e.isAltDown() && e.isControlDown()) || ignoreModifiers)
					{
						MobilePlatform.pressedKeys[mobikey] = true;
					}
				}
			}
			else
			{
				if(mobikey < 19) { MobilePlatform.keyRepeated(Mobile.getMobileKey(mobikey)); }
			}
		}
	}

	protected void releaseKey(KeyEvent e)
	{
		if(awtGUI.hasLoadedFile())
		{
			int mobikey = getMobileKey(e.getKeyCode());

			if (mobikey == Integer.MIN_VALUE) // Ignore events from keys not mapped to a phone keypad key (AWTGUI does use 0, so this can't mirror libretro)
			{
				return;
			}

			// Figures we must only release if the key is pressed. This vastly simplifies external input event handling
			if(MobilePlatform.pressedKeys[mobikey])
			{
				MobilePlatform.pressedKeys[mobikey] = false;
				MobilePlatform.keyReleased(Mobile.getMobileKey(mobikey));

				if(mobikey == 20) { ScreenShot.takeScreenshot(false); }
				else if(mobikey == 21) { MobilePlatform.pauseResumeApp(); }

				for(int i = 0; i < MobilePlatform.pressedKeys.length; i++)
				{
					if(MobilePlatform.pressedKeys[i]) { MobilePlatform.keyRepeated(Mobile.getMobileKey(i)); }
				}
			}
		}
	}

	private static String getFormattedLocation(String loc)
	{
		if (loc.startsWith("file://") || loc.startsWith("http://") || loc.startsWith("https://"))
			return loc;

		File file = new File(loc);
		if(!file.isFile())
		{
			Mobile.log(Mobile.LOG_ERROR, FreeJ2ME.class.getPackage().getName() + "." + FreeJ2ME.class.getSimpleName() + ": " + "File not found...");
			System.exit(0);
		}

		return file.toURI().toString();
	}

	private void settingsChanged()
	{
		boolean hasRotated = Mobile.updateSettings();

		// Create a standard size LCD if not rotated, else invert window's width and height.
		if(Mobile.lcdWidth != lcdWidth || Mobile.lcdHeight != lcdHeight || hasRotated) 
		{
			Mobile.getPlatform().resizeLCD(Mobile.lcdWidth, Mobile.lcdHeight);

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
			resize();
			if(!isFullscreen) { main.setSize(lcdWidth*scaleFactor+xborder , lcdHeight*scaleFactor+yborder); }
			lcd.clearScreen();
		}
		
		awtGUI.updateOptions();
	}

	private int getMobileKey(int keycode)
	{
		for(int i = 0; i < awtGUI.inputKeycodes.length; i++) 
		{
			if(keycode == awtGUI.inputKeycodes[i]) { return Mobile.convertAWTKeycode(i);}
		}
		return Integer.MIN_VALUE;
	}

	private void resize()
	{
		xborder = main.getInsets().left+main.getInsets().right;
		yborder = main.getInsets().top+main.getInsets().bottom;

		double vw = (main.getWidth()-xborder)*1;
		double vh = (main.getHeight()-yborder)*1;

		double nw = lcdWidth;
		double nh = lcdHeight;

		nw = vw;
		nh = nw*((double)lcdHeight/(double)lcdWidth);

		if(nh>vh)
		{
			nh = vh;
			nw = nh*((double)lcdWidth/(double)lcdHeight);
		}

		lcd.updateScale((int)nw, (int)nh);
	}

	public void toggleFullscreen() 
	{
        isFullscreen = !isFullscreen;
		main.dispose();
		constructFreeJ2MEGUI();
		displayGUI();
    }

	private void constructFreeJ2MEGUI()
	{
		main = new Frame("FreeJ2ME-Plus");

		if (isFullscreen) 
		{
            main.setUndecorated(true);
            main.setSize(Toolkit.getDefaultToolkit().getScreenSize());
        } 
		else 
		{
            main.setSize(350, 450);
            main.setMinimumSize(new Dimension(240, 240));
			main.setLocationRelativeTo(null); // Center window on screen
        }

		main.setBackground(Color.BLACK);
		
		try
		{
			main.setIconImage(ImageIO.read(main.getClass().getResourceAsStream("/org/recompile/icon.png")));	
		}
		catch (Exception e) { }

		main.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				System.exit(0);
			}
		});

		/* Add LCD screen to FreeJ2ME's AWT frame */
		main.add(lcd);

		awtGUI.setMainFrame(main);
		/* Append the awt menu bar into FreeJ2ME's frame */
		if(!isFullscreen) { main.setMenuBar(awtGUI.getMenuBar()); }
	}

	private void displayGUI() 
	{
		main.addComponentListener(new ComponentAdapter()
		{
			public void componentResized(ComponentEvent e) { resize(); }
		});

		main.setVisible(true);
		main.pack();
		resize();
		if(!isFullscreen) { main.setSize(lcdWidth*scaleFactor+xborder, lcdHeight*scaleFactor+yborder); }

		awtGUI.updateMemStatDialog();
	}

	private class LCD extends Canvas
	{
		private boolean showDragMessage = false, fileSupported = true;
		public int cx=0;
		public int cy=0;
		public int cw=240;
		public int ch=320;

		public double scalex=1;
		public double scaley=1;

		public LCD() { setDropTarget(); }

		public void updateScale(int vw, int vh)
		{
			cx = (this.getWidth()-vw)/2;
			cy = (this.getHeight()-vh)/2;
			cw = vw;
			ch = vh;
			scalex = (double)lcdWidth/(double)vw;
			scaley = (double)lcdHeight/(double)vh;
		}

		@Override
        public void update(Graphics g) {
            // Use paint method directly to avoid flicker
            paint(g);
        }

		// Used to clear the entire framebuffer when rotated in fullscreen to remove garbage pixels
		public void clearScreen() 
		{
			((Graphics2D) this.getGraphics()).clearRect(0, 0, getWidth(), getHeight());
		}

		public void paint(Graphics g)
		{
			if(!showDragMessage) 
			{
				if (!Mobile.rotateDisplay) {
					g.drawImage(Mobile.getPlatform().getLcdFrontbufferImage(), cx, cy, cw, ch, null);
				} else {
					// Rotate the FB 90 degrees counterclockwise with an adjusted pivot
					((Graphics2D) g).rotate(Math.toRadians(-90), ch/2, ch/2);
					// Draw the rotated FB with adjusted cy and cx values
					g.drawImage(Mobile.getPlatform().getLcdFrontbufferImage(), 0, cx, ch, cw, null);
				}
				
				if(Mobile.isPaused)
				{
					g.setColor(new Color(0, 0, 64, 160));
					g.fillRect(0, 0, getWidth(), getHeight());
					g.setFont(new Font("Dialog", Font.BOLD, cw/5));
					g.setColor(Color.ORANGE);
					String message = "PAUSED!";
					FontMetrics metrics = g.getFontMetrics();
					int x = (getWidth() - metrics.stringWidth(message)) / 2;
					int y = (getHeight() + metrics.getAscent()) / 2;
					g.drawString(message, x, y);
				}
				else if (MobilePlatform.pressedKeys[19]) // Check if fast-forward is active
				{
					g.setFont(new Font("Dialog", Font.BOLD, cw/2));
					g.setColor(Color.ORANGE);
					String fastForwardIndicator = "»";
					FontMetrics ffMetrics = g.getFontMetrics();
					int ffX = (getWidth() - ffMetrics.stringWidth(fastForwardIndicator)) / 2;
					int ffY = (getHeight() + ffMetrics.getAscent()) / 2;
					g.drawString(fastForwardIndicator, ffX, ffY);
				}
			}
			else 
			{
				g.setColor(freeJ2MEDragColor);
				g.fillRect(cx, cy, cw, ch);
				g.setFont(new Font("Dialog", Font.BOLD, 20));
				g.setColor(fileSupported ? Color.ORANGE : Color.RED);
				String message = fileSupported ? ">> DROP HERE <<" : "INVALID FILE TYPE!!!";
				FontMetrics metrics = g.getFontMetrics();
				int x = (getWidth() - metrics.stringWidth(message)) / 2;
				int y = (getHeight() / 2);
				g.drawString(message, x, y);
			}
		}

		private void setDropTarget() 
		{
			new DropTarget(this, new DropTargetListener() 
			{
				@Override
				@SuppressWarnings("unchecked")
				public void dragEnter(DropTargetDragEvent dtde) 
				{
					try
					{
						if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
						{
							// Get the files being dragged
							Transferable transferable = dtde.getTransferable();
							java.util.List<File> files = (java.util.List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

							// Check if the file is supported
							boolean fileSupported = false;
							for (File file : files)
							{
								if (isSupportedFile(file.getName()))
								{
									fileSupported = true;
									break;
								}
							}
						}
					} catch (Exception e) { e.printStackTrace(); }

					showDragMessage = true;
					repaint();
				}
	
				@Override
				public void dragOver(DropTargetDragEvent dtde) { }
	
				@Override
				public void dropActionChanged(DropTargetDragEvent dtde) { }
	
				@Override
				public void dragExit(DropTargetEvent dte) 
				{
					showDragMessage = false;
					repaint();
				}
	
				@Override
				@SuppressWarnings("unchecked")
				public void drop(DropTargetDropEvent dtde) 
				{
					try 
					{
						dtde.acceptDrop(DnDConstants.ACTION_COPY);
						Transferable transferable = dtde.getTransferable();
						if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) 
						{
							java.util.List<File> files = (java.util.List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
							if (!files.isEmpty() && fileSupported)
							{
								// Load the dropped file
								if(!awtGUI.hasLoadedFile()) { awtGUI.loadJarFile(files.get(0).toURI().toString()); }
								else // Ask for a restart if a jar is already running
								{
									Mobile.getPlatform().fileName = files.get(0).toURI().toString();
									awtGUI.showRestartDialog();
								}
							}
						}
					} 
					catch (Exception e) { System.out.println("Exception caught in Drag and Drop:" + e.getMessage()); } 
					finally 
					{
						dtde.dropComplete(true);
						showDragMessage = false;
						repaint();
					}
				}
			});
		}

		private boolean isSupportedFile(String fileName) 
		{
			// Check for supported extensions with drag and drop
			return fileName.toLowerCase().endsWith(".jar") ||
				   fileName.toLowerCase().endsWith(".jad") ||
				   fileName.toLowerCase().endsWith(".kjx");
		}
	}
}
