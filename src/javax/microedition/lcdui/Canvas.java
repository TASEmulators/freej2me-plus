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
package javax.microedition.lcdui;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformImage;
import org.recompile.mobile.PlatformGraphics;

public abstract class Canvas extends Displayable
{
	public static final int UP = 1;
	public static final int LEFT = 2;
	public static final int RIGHT = 5;
	public static final int DOWN = 6;
	public static final int FIRE = 8;

	public static final int GAME_A = 9;
	public static final int GAME_B = 10;
	public static final int GAME_C = 11;
	public static final int GAME_D = 12;

	public static final int KEY_NUM0 = 48;
	public static final int KEY_NUM1 = 49;
	public static final int KEY_NUM2 = 50;
	public static final int KEY_NUM3 = 51;
	public static final int KEY_NUM4 = 52;
	public static final int KEY_NUM5 = 53;
	public static final int KEY_NUM6 = 54;
	public static final int KEY_NUM7 = 55;
	public static final int KEY_NUM8 = 56;
	public static final int KEY_NUM9 = 57;
	public static final int KEY_STAR = 42;
	public static final int KEY_POUND = 35;


	protected Canvas()
	{
		width = Mobile.getPlatform().lcdWidth;
		height = Mobile.getPlatform().lcdHeight;

		Mobile.log(Mobile.LOG_INFO, Canvas.class.getPackage().getName() + "." + Canvas.class.getSimpleName() + ": " + "Create Canvas:"+width+", "+height);

		platformImage = new PlatformImage(width, height);

		Mobile.getPlatform().canvas = this;
	}

	public int getGameAction(int keyCode) { return Mobile.getGameAction(keyCode); }

	public int getKeyCode(int gameAction)
	{
		switch(gameAction) // Look on Mobile.java for what these magic numbers mean ("J2ME Canvas standard keycodes")
		{
			case Mobile.KEY_NUM2:   return Mobile.getMobileKey(14, true);
			case Mobile.KEY_NUM8:   return Mobile.getMobileKey(17, true);
			case Mobile.KEY_NUM4:   return Mobile.getMobileKey(15, true);
			case Mobile.KEY_NUM6:   return Mobile.getMobileKey(16, true);
			case Mobile.KEY_NUM5:   return Mobile.getMobileKey(18, true);
			case Mobile.GAME_UP:    return Mobile.getMobileKey(0, true);
			case Mobile.GAME_DOWN:  return Mobile.getMobileKey(1, true);
			case Mobile.GAME_LEFT:  return Mobile.getMobileKey(2, true);
			case Mobile.GAME_RIGHT: return Mobile.getMobileKey(3, true);
			case Mobile.GAME_FIRE:  return Mobile.getMobileKey(7, true);
	
			// GAME_A through D don't show up in documentation at all.
			case Mobile.GAME_A: case Mobile.KEY_NUM1: return Mobile.getMobileKey(10, true);
			case Mobile.GAME_B: case Mobile.KEY_NUM3: return Mobile.getMobileKey(11, true);
			case Mobile.GAME_C: case Mobile.KEY_NUM7: return Mobile.getMobileKey(5, true);
			case Mobile.GAME_D: case Mobile.KEY_NUM9: return Mobile.getMobileKey(4, true);

			case Mobile.KEY_NUM0:  return Mobile.getMobileKey(6, true);
			case Mobile.KEY_STAR:  return Mobile.getMobileKey(12, true);
			case Mobile.KEY_POUND: return Mobile.getMobileKey(13, true);
		}
		return 0;
	}

	public String getKeyName(int keyCode)
	{
		if(keyCode<0) { keyCode=0-keyCode; }
		switch(keyCode)
		{
			case 1: return "UP";
			case 2: return "DOWN";
			case 5: return "LEFT";
			case 6: return "RIGHT";
			case 8: return "FIRE";
			case 9: return "A";
			case 10: return "B";
			case 11: return "C";
			case 12: return "D";
			case 48: return "0";
			case 49: return "1";
			case 50: return "2";
			case 51: return "3";
			case 52: return "4";
			case 53: return "5";
			case 54: return "6";
			case 55: return "7";
			case 56: return "8";
			case 57: return "9";
			case 42: return "*";
			case 35: return "#";
		}
		return "-";
	}

	public boolean hasPointerEvents() { return true; }

	public boolean hasPointerMotionEvents() { return false; }

	public boolean hasRepeatEvents() { return true; }

	public void hideNotify() { }

	public boolean isDoubleBuffered() { return true; }

	public void keyPressed(int keyCode) { }

	public void keyReleased(int keyCode) { }

	public void keyRepeated(int keyCode) { }

	protected abstract void paint(Graphics g);

	public void pointerDragged(int x, int y) { }

	public void pointerPressed(int x, int y) { }

	public void pointerReleased(int x, int y) { }

	public void repaint()
	{
		Display.LCDUILock.lock();
		try {
			PlatformGraphics graphics;
			try
			{
				graphics = platformImage.getGraphics();
				graphics.reset();
				paint(graphics);
				if(Mobile.getDisplay().getCurrent() == this)
				{
					Mobile.getPlatform().repaint(platformImage, 0, 0, width, height);
				}
			}
			catch (Exception e)
			{
				System.out.print("Canvas repaint(): "+e.getMessage());
				e.printStackTrace();
			}
		} finally {
			Display.LCDUILock.unlock();
		}	
	}

	public void repaint(int x, int y, int width, int height)
	{
		Display.LCDUILock.lock();
		try {
			PlatformGraphics graphics = platformImage.getGraphics();
			graphics.reset();
			paint(graphics);
			if(Mobile.getDisplay().getCurrent() == this)
			{
				Mobile.getPlatform().repaint(platformImage, x, y, width, height);
			}
		} finally {
			Display.LCDUILock.unlock();
		}
	}

	public void serviceRepaints()
	{
		if (Mobile.getDisplay().getCurrent() == this)
		{
			Mobile.getPlatform().repaint(platformImage, 0, 0, width, height);
		}
	}

	public void setFullScreenMode(boolean mode)
	{
		//System.out.print("Set Canvas Full Screen Mode ");
		fullScreen = mode;
		if(fullScreen)
		{
			width = Mobile.getPlatform().lcdWidth;
			height = Mobile.getPlatform().lcdHeight;
		}
	}

	public void showNotify() { }

	protected void sizeChanged(int w, int h)
	{
		width = w;
		height = h;
	}

	public void doSizeChanged(int w, int h) { sizeChanged(w, h); }

	public void notifySetCurrent() { repaint(); }

}
