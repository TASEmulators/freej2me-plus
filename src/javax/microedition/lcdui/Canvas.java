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
import org.recompile.mobile.MobilePlatform;
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

	public static final int KEY_SOFT_LEFT = Mobile.NOKIA_SOFT1;
	public static final int KEY_SOFT_RIGHT = Mobile.NOKIA_SOFT2;

	private int barHeight;
	private boolean fullscreen = false;
	private boolean isPainting = false;

	// Those are only used to discern actual soft key command bar inputs below
	private boolean leftSoftPressed = false;
	private boolean rightSoftPressed = false;

	protected Canvas()
	{
		Mobile.log(Mobile.LOG_INFO, Canvas.class.getPackage().getName() + "." + Canvas.class.getSimpleName() + ": " + "Create Canvas:"+width+", "+height);

		barHeight = Font.getDefaultFont().getHeight();
	}

	public int getGameAction(int keyCode) 
	{ 
		int castKey = Mobile.getGameAction(keyCode);

		// We should send those soft keys to handle commands if not fullscreen. As it means the command bar is visible
		if(castKey == KEY_SOFT_LEFT && !fullscreen) 
		{ 
			if(!leftSoftPressed) { keyPressedCommands(castKey); } // Make sure keyReleases aren't registered here
			leftSoftPressed = !leftSoftPressed; 
		}
		else if(castKey == KEY_SOFT_RIGHT && !fullscreen) 
		{
			if(!rightSoftPressed) { keyPressedCommands(castKey); } 
			rightSoftPressed = !rightSoftPressed; 
		}

		return castKey;
	}

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

	public void repaint() { repaint(0, 0, width, height); } // Just a full canvas repaint

	public void repaint(int x, int y, int width, int height)
	{
		Display.LCDUILock.lock();
		try 
		{
			try 
			{
				if (getDisplay().getCurrent() != this || listCommands) { return; }
				
				// TODO: This might be an issue
				if (isPainting) 
				{
					// we need this to avoid stackoverflow
					// but it seems the underlying problem is that when paint calls
					// repaint, we shouldn't even land here...
					Mobile.getDisplay().callSerially(() -> { repaint(x, y, width, height); });
					return;
				}

				graphics.reset();
				isPainting = true;

				try { paint(graphics); }
				catch (Exception e) 
				{
					Mobile.log(Mobile.LOG_WARNING, Canvas.class.getPackage().getName() + "." + Canvas.class.getSimpleName() + ": " + "Exception hit in paint(graphics)" + e.getMessage());
				}
				finally { isPainting = false; }
				
				// Draw command bar whenever the canvas is not fullscreen and there are commands in the bar
				if (!fullscreen && !commands.isEmpty()) { paintCommandsBar(); }

				Mobile.getPlatform().flushGraphics(platformImage, x, y, width, height);
			}
			catch (Exception e) 
			{
				Mobile.log(Mobile.LOG_ERROR, Canvas.class.getPackage().getName() + "." + Canvas.class.getSimpleName() + ": " + "Serious Exception hit in repaint()" + e.getMessage());
				e.printStackTrace();
			}
		} finally { Display.LCDUILock.unlock(); }
	}

	public void serviceRepaints()
	{
		if (Mobile.getDisplay().getCurrent() == this)
		{
			Mobile.getPlatform().flushGraphics(platformImage, 0, 0, width, height);
		}
	}

	public void setFullScreenMode(boolean mode)
	{
		fullscreen = mode;
		if (mode != fullscreen) 
		{
			fullscreen = mode;
			_invalidate();
		}
	}

	public void showNotify() { }

	protected void sizeChanged(int w, int h)
	{
		width = w;
		height = h;
	}

	public void notifySetCurrent() { _invalidate(); }

	public int getHeight() { return height; }

	private void paintCommandsBar() 
	{
		graphics.reset();

		graphics.setFont(Font.getDefaultFont());
		graphics.setColor(Mobile.lcduiBGColor);
		graphics.fillRect(0, height-barHeight, width, barHeight);

		int textCenter;
		int xPos;

		graphics.setColor(Mobile.lcduiTextColor);
		graphics.drawLine(0, height-barHeight, width, height-barHeight);
		graphics.drawLine(width/2, height-barHeight, width/2, height);
		if (!commands.isEmpty())
		{
			String label = commands.size() > 2 ? "Options" : commands.get(0).getLabel();
			textCenter = (graphics.getGraphics2D().getFontMetrics().stringWidth(label))/2;
			xPos = (width / 4) - textCenter;
			graphics.drawString(label, xPos, height-barHeight, Graphics.LEFT);
		}
		if (commands.size() == 2) 
		{
			textCenter = (graphics.getGraphics2D().getFontMetrics().stringWidth(commands.get(1).getLabel()))/2;
			xPos = (3 * width / 4) - textCenter;
			graphics.drawString(commands.get(1).getLabel(), xPos, height-barHeight, Graphics.LEFT);
		}
	}

	public void addCommand(Command cmd)	{ super.addCommand(cmd); }

	public void removeCommand(Command cmd) { super.removeCommand(cmd); }

	protected void render() 
	{
		if (listCommands) { super.render(); } 
		else { repaint(); }
	}

}
