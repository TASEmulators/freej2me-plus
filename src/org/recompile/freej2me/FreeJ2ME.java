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

import org.recompile.mobile.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.microedition.media.Manager;

public class FreeJ2ME
{

	private static FreeJ2ME app;
	private Frame main;
	private int lcdWidth;
	private int lcdHeight;
	private int scaleFactor = 1;

	private LCD lcd;

	private int xborder;
	private int yborder;

	private PlatformImage img;

	private Config config;
	private boolean useNokiaControls = false;
	private boolean useSiemensControls = false;
	private boolean useMotorolaControls = false;
	private boolean rotateDisplay = false;
	private int limitFPS = 0;
	private int renderHint = 0;
	
	private AWTGUI awtGUI;

	private boolean[] pressedKeys = new boolean[128];

	public static void main(String args[])
	{
		FreeJ2ME app = new FreeJ2ME(args);
	}

	public FreeJ2ME(String args[])
	{
		main = new Frame("FreeJ2ME");
		main.setSize(350,450);
		/* Set a minimum allowed width and height so the menu bar is visible at all times */
		main.setMinimumSize(new Dimension(240, 240));
		main.setBackground(new Color(0,0,64));
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

		// Setup Device //

		/* 
		 * If the directory for custom soundfonts doesn't exist, create it, no matter if the user
		 * is going to use it or not.
		 */
		try 
		{
			if(!PlatformPlayer.soundfontDir.exists()) 
			{ 
				PlatformPlayer.soundfontDir.mkdirs();
				File dummyFile = new File(PlatformPlayer.soundfontDir + "/Put your sf2 bank here");
				dummyFile.createNewFile();
			}
		}
		catch(IOException e) { System.out.println("Failed to create custom midi info file:" + e.getMessage()); }
		

		lcdWidth = 240;
		lcdHeight = 320;

		if(args.length>=1)
		{
			awtGUI.setJarPath(getFormattedLocation(args[0]));
		}
		if(args.length>=3)
		{
			lcdWidth = Integer.parseInt(args[1]);
			lcdHeight = Integer.parseInt(args[2]);
		}
		if(args.length>=4)
		{
			scaleFactor = Integer.parseInt(args[3]);
		}

		Mobile.setPlatform(new MobilePlatform(lcdWidth, lcdHeight));

		lcd = new LCD();
		lcd.setFocusable(true);

		config = new Config();
		config.onChange = new Runnable() { public void run() { settingsChanged(); } };

		/* Add LCD screen to FreeJ2ME's AWT frame */
		main.add(lcd);

		awtGUI = new AWTGUI(config);
		awtGUI.setMainFrame(main);
		/* Append the awt menu bar into FreeJ2ME's frame */
		main.setMenuBar(awtGUI.getMenuBar());

		Mobile.getPlatform().setPainter(new Runnable()
		{
			public void run()
			{
				/* Set menuBar option states based on loaded config */
				if(awtGUI.hasJustLoaded()) { awtGUI.updateOptions(); }

				/* Only update mem dialog's stats if it is visible */
				if(awtGUI.memStatDialog.isVisible()) { awtGUI.updateMemStatDialog(); }

				/* Whenever AWT GUI notifies that its menu options were changed, update settings */
				if(awtGUI.hasChanged()) { settingsChanged(); awtGUI.clearChanged(); }

				lcd.paint(lcd.getGraphics());
			}
		});

		/* Inputs should only be registered if a jar has been loaded, otherwise AWT will throw NullPointerException */
		lcd.addKeyListener(new KeyListener()
		{
			public void keyPressed(KeyEvent e)
			{
				if(awtGUI.hasLoadedFile())
				{
					int keycode = e.getKeyCode();
					int mobikey = getMobileKey(keycode);
					int mobikeyN = (mobikey + 64) & 0x7F; //Normalized value for indexing the pressedKeys array
					
					switch(keycode) // Handle emulator control keys
					{
						case KeyEvent.VK_PLUS:
						case KeyEvent.VK_ADD:
							scaleFactor++;
							main.setSize(lcdWidth * scaleFactor + xborder, lcdHeight * scaleFactor + yborder);
						break;
						case KeyEvent.VK_MINUS:
						case KeyEvent.VK_SUBTRACT:
							if( scaleFactor > 1 )
							{
								scaleFactor--;
								main.setSize(lcdWidth * scaleFactor + xborder, lcdHeight * scaleFactor + yborder);
							}
						break;
						case KeyEvent.VK_C:
							if(e.isControlDown())
							{
								ScreenShot.takeScreenshot(false);
							}
						break;
					}
					
					if (mobikey == 0) //Ignore events from keys not mapped to a phone keypad key
					{
						return; 
					}
					
					if(config.isRunning)
					{
						config.keyPressed(mobikey);
					}
					else
					{
						if (pressedKeys[mobikeyN] == false)
						{
							//~ System.out.println("keyPressed:  " + Integer.toString(mobikey));
							Mobile.getPlatform().keyPressed(mobikey);
						}
						else
						{
							//~ System.out.println("keyRepeated:  " + Integer.toString(mobikey));
							Mobile.getPlatform().keyRepeated(mobikey);
						}
					}
					pressedKeys[mobikeyN] = true;
				}
			}

			public void keyReleased(KeyEvent e)
			{
				if(awtGUI.hasLoadedFile()) 
				{
					int mobikey = getMobileKey(e.getKeyCode());
					int mobikeyN = (mobikey + 64) & 0x7F; //Normalized value for indexing the pressedKeys array
					
					if (mobikey == 0) //Ignore events from keys not mapped to a phone keypad key
					{
						return; 
					}
					
					pressedKeys[mobikeyN] = false;
					
					if(config.isRunning)
					{
						config.keyReleased(mobikey);
					}
					else
					{
						//~ System.out.println("keyReleased: " + Integer.toString(mobikey));
						Mobile.getPlatform().keyReleased(mobikey);
					}
				}
			}

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
					if(rotateDisplay)
					{
						x = (int)((lcd.ch-(e.getY()-lcd.cy)) * lcd.scaley);
						y = (int)((e.getX()-lcd.cx) * lcd.scalex);
					}

					Mobile.getPlatform().pointerPressed(x, y);
				}
			}

			public void mouseReleased(MouseEvent e)
			{
				if(awtGUI.hasLoadedFile()) 
				{
					int x = (int)((e.getX()-lcd.cx) * lcd.scalex);
					int y = (int)((e.getY()-lcd.cy) * lcd.scaley);

					if(rotateDisplay)
					{
						x = (int)((lcd.ch-(e.getY()-lcd.cy)) * lcd.scaley);
						y = (int)((e.getX()-lcd.cx) * lcd.scalex);
					}

					Mobile.getPlatform().pointerReleased(x, y);
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

					if(rotateDisplay)
					{
						x = (int)((lcd.ch-(e.getY()-lcd.cy)) * lcd.scaley);
						y = (int)((e.getX()-lcd.cx) * lcd.scalex);
					}
					
					Mobile.getPlatform().pointerDragged(x, y); 
				}
			}
		});

		main.addComponentListener(new ComponentAdapter()
		{
			public void componentResized(ComponentEvent e)
			{
				resize();
			}
		});

		main.setVisible(true);
		main.pack();

		resize();
		main.setSize(lcdWidth*scaleFactor+xborder, lcdHeight*scaleFactor+yborder);

		if(args.length<1)
		{
			while(!awtGUI.hasLoadedFile())
			{
				try{ Thread.sleep(1000); }
				catch (InterruptedException e) { }
			}
		}
		if(Mobile.getPlatform().loadJar(awtGUI.getJarPath()))
		{
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

			Mobile.getPlatform().runJar();
		}
		else
		{
			System.out.println("Couldn't load jar...");
		}
	}

	private static String getFormattedLocation(String loc)
	{
		if (loc.startsWith("file://") || loc.startsWith("http://") || loc.startsWith("https://"))
			return loc;

		File file = new File(loc);
		if(! file.isFile())
		{
			System.out.println("File not found...");
			System.exit(0);
		}

		return "file://" + file.getAbsolutePath();
	}

	private void settingsChanged()
	{
		int w = Integer.parseInt(config.settings.get("width"));
		int h = Integer.parseInt(config.settings.get("height"));

		limitFPS = Integer.parseInt(config.settings.get("fps"));
		if(limitFPS>0) { limitFPS = 1000 / limitFPS; }

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
		if(phone.equals("Nokia")) { Mobile.nokia = true; useNokiaControls = true; }
		if(phone.equals("Siemens")) { Mobile.siemens = true; useSiemensControls = true; }
		if(phone.equals("Motorola")) { Mobile.motorola = true; useMotorolaControls = true; }

		String rotate = config.settings.get("rotate");
		if(rotate.equals("on")) { rotateDisplay = true; }
		if(rotate.equals("off")) { rotateDisplay = false; }

		String midiSoundfont = config.settings.get("soundfont");
		if(midiSoundfont.equals("Custom"))  { PlatformPlayer.customMidi = true; }
		if(midiSoundfont.equals("Default")) { PlatformPlayer.customMidi = false; }

		// Create a standard size LCD if not rotated, else invert window's width and height.
		if(!rotateDisplay) 
		{
			lcdWidth = w;
			lcdHeight = h;

			Mobile.getPlatform().resizeLCD(w, h);
			
			resize();
			main.setSize(lcdWidth*scaleFactor+xborder , lcdHeight*scaleFactor+yborder);
		}
		else 
		{
			lcdWidth = h;
			lcdHeight = w;

			Mobile.getPlatform().resizeLCD(w, h);

			resize();
			main.setSize(lcdWidth*scaleFactor+xborder , lcdHeight*scaleFactor+yborder);
		}

		Manager.updatePlayerNum((byte) Integer.parseInt(config.settings.get("maxmidistreams")));
	}

	private int getMobileKey(int keycode)
	{
		if(useNokiaControls)
		{
			if(keycode == awtGUI.inputKeycodes[AWTGUI.UP_ARROW_KEY]) { return Mobile.NOKIA_UP; }
			else if(keycode == awtGUI.inputKeycodes[AWTGUI.DOWN_ARROW_KEY]) { return Mobile.NOKIA_DOWN; }
			else if(keycode == awtGUI.inputKeycodes[AWTGUI.LEFT_ARROW_KEY]) { return Mobile.NOKIA_LEFT; }
			else if(keycode == awtGUI.inputKeycodes[AWTGUI.RIGHT_ARROW_KEY]) { return Mobile.NOKIA_RIGHT; }
			else if(keycode == awtGUI.inputKeycodes[AWTGUI.OK_KEY]) { return Mobile.NOKIA_SOFT3; }
		}

		if(useSiemensControls)
		{
			if(keycode == awtGUI.inputKeycodes[AWTGUI.UP_ARROW_KEY]) { return Mobile.SIEMENS_UP; }
			else if(keycode == awtGUI.inputKeycodes[AWTGUI.DOWN_ARROW_KEY]) { return Mobile.SIEMENS_DOWN; }
			else if(keycode == awtGUI.inputKeycodes[AWTGUI.LEFT_ARROW_KEY]) { return Mobile.SIEMENS_LEFT; }
			else if(keycode == awtGUI.inputKeycodes[AWTGUI.RIGHT_ARROW_KEY]) { return Mobile.SIEMENS_RIGHT; }
			else if(keycode == awtGUI.inputKeycodes[AWTGUI.SOFT_LEFT_KEY]) { return Mobile.SIEMENS_SOFT1; }
			else if(keycode == awtGUI.inputKeycodes[AWTGUI.SOFT_LEFT_KEY]) { return Mobile.SIEMENS_SOFT2; }
			else if(keycode == awtGUI.inputKeycodes[AWTGUI.OK_KEY]) { return Mobile.SIEMENS_FIRE; }
		}

		if(useMotorolaControls)
		{
			if(keycode == awtGUI.inputKeycodes[AWTGUI.UP_ARROW_KEY]) { return Mobile.MOTOROLA_UP; }
			else if(keycode == awtGUI.inputKeycodes[AWTGUI.DOWN_ARROW_KEY]) { return Mobile.MOTOROLA_DOWN; }
			else if(keycode == awtGUI.inputKeycodes[AWTGUI.LEFT_ARROW_KEY]) { return Mobile.MOTOROLA_LEFT; }
			else if(keycode == awtGUI.inputKeycodes[AWTGUI.RIGHT_ARROW_KEY]) { return Mobile.MOTOROLA_RIGHT; }
			else if(keycode == awtGUI.inputKeycodes[AWTGUI.SOFT_LEFT_KEY]) { return Mobile.MOTOROLA_SOFT1; }
			else if(keycode == awtGUI.inputKeycodes[AWTGUI.SOFT_LEFT_KEY]) { return Mobile.MOTOROLA_SOFT2; }
			else if(keycode == awtGUI.inputKeycodes[AWTGUI.OK_KEY]) { return Mobile.MOTOROLA_FIRE; }
		}

		if(keycode == awtGUI.inputKeycodes[AWTGUI.NUMPAD0_KEY]) return Mobile.KEY_NUM0;
		else if(keycode == awtGUI.inputKeycodes[AWTGUI.NUMPAD1_KEY]) return Mobile.KEY_NUM1;
		else if(keycode == awtGUI.inputKeycodes[AWTGUI.NUMPAD2_KEY]) return Mobile.KEY_NUM2;
		else if(keycode == awtGUI.inputKeycodes[AWTGUI.NUMPAD3_KEY]) return Mobile.KEY_NUM3;
		else if(keycode == awtGUI.inputKeycodes[AWTGUI.NUMPAD4_KEY]) return Mobile.KEY_NUM4;
		else if(keycode == awtGUI.inputKeycodes[AWTGUI.NUMPAD5_KEY]) return Mobile.KEY_NUM5;
		else if(keycode == awtGUI.inputKeycodes[AWTGUI.NUMPAD6_KEY]) return Mobile.KEY_NUM6;
		else if(keycode == awtGUI.inputKeycodes[AWTGUI.NUMPAD7_KEY]) return Mobile.KEY_NUM7;
		else if(keycode == awtGUI.inputKeycodes[AWTGUI.NUMPAD8_KEY]) return Mobile.KEY_NUM8;
		else if(keycode == awtGUI.inputKeycodes[AWTGUI.NUMPAD9_KEY]) return Mobile.KEY_NUM9;
		else if(keycode == awtGUI.inputKeycodes[AWTGUI.NUMPAD_ASTERISK_KEY]) return Mobile.KEY_STAR;
		else if(keycode == awtGUI.inputKeycodes[AWTGUI.NUMPAD_POUND_KEY]) return Mobile.KEY_POUND;

		else if(keycode == awtGUI.inputKeycodes[AWTGUI.UP_ARROW_KEY]) return Mobile.KEY_NUM2;
		else if(keycode == awtGUI.inputKeycodes[AWTGUI.DOWN_ARROW_KEY]) return Mobile.KEY_NUM8;
		else if(keycode == awtGUI.inputKeycodes[AWTGUI.LEFT_ARROW_KEY]) return Mobile.KEY_NUM4;
		else if(keycode == awtGUI.inputKeycodes[AWTGUI.RIGHT_ARROW_KEY]) return Mobile.KEY_NUM6;

		else if(keycode == awtGUI.inputKeycodes[AWTGUI.OK_KEY]) return Mobile.KEY_NUM5;

		else if(keycode == awtGUI.inputKeycodes[AWTGUI.SOFT_LEFT_KEY]) return Mobile.NOKIA_SOFT1;
		else if(keycode == awtGUI.inputKeycodes[AWTGUI.SOFT_RIGHT_KEY]) return Mobile.NOKIA_SOFT2;

		return 0;
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

	private class LCD extends Canvas
	{
		public int cx=0;
		public int cy=0;
		public int cw=240;
		public int ch=320;

		public double scalex=1;
		public double scaley=1;

		public void updateScale(int vw, int vh)
		{
			cx = (this.getWidth()-vw)/2;
			cy = (this.getHeight()-vh)/2;
			cw = vw;
			ch = vh;
			scalex = (double)lcdWidth/(double)vw;
			scaley = (double)lcdHeight/(double)vh;
		}

		public void paint(Graphics g)
		{
			try
			{
				Graphics2D cgc = (Graphics2D)this.getGraphics();
				if (config.isRunning)
				{
					if(!rotateDisplay)
					{
						g.drawImage(config.getLCD(), cx, cy, cw, ch, null);
					}
					else
					{
						// If rotated, simply redraw the config menu with different width and height
						g.drawImage(config.getLCD(), cy, cx, cw, ch, null);
					}
				}
				else
				{
					if(!rotateDisplay)
					{
						g.drawImage(Mobile.getPlatform().getLCD(), cx, cy, cw, ch, null);
					}
					else
					{
						// Rotate the FB 90 degrees counterclockwise with an adjusted pivot
						cgc.rotate(Math.toRadians(-90), ch/2, ch/2);
						// Draw the rotated FB with adjusted cy and cx values
						cgc.drawImage(Mobile.getPlatform().getLCD(), 0, cx, ch, cw, null);
					}

					if(limitFPS>0)
					{
						Thread.sleep(limitFPS);
					}
				}
			}
			catch (Exception e)
			{
				System.out.println(e.getMessage());
			}
		}
	}
}
