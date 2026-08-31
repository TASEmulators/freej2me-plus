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

import java.util.concurrent.atomic.AtomicBoolean;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.MobilePlatform;
import org.recompile.mobile.PlatformImage;

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

	public static final int KEY_SOFT_LEFT = 126;
	public static final int KEY_SOFT_RIGHT = 127;

	// SKT-specific keys. They are actually defined in Canvas like that.
	public static final int KEY_CLR = 8;
	public static final int KEY_COML = 129;
	public static final int KEY_COMC = 130;
	public static final int KEY_COMR = 131;
	public static final int KEY_UP = 141;
	public static final int KEY_LEFT = 142;
	public static final int KEY_RIGHT = 145;
	public static final int KEY_DOWN = 146;
	public static final int KEY_FIRE = 148;
	public static final int KEY_CALL = 190;
	public static final int KEY_END = 191;
	public static final int KEY_FLIP_OPEN = 192;
	public static final int KEY_FLIP_CLOSE = 193;
	public static final int KEY_VOL_UP = 194;
	public static final int KEY_VOL_DOWN = 195;

	private int barHeight;
	private boolean suppressKeyEvents = false; // For GameCanvas
	private boolean fullscreen = false;

	// We now coalesce multiple paint calls into a single one spanning the
	// union of all the areas passed to repaint(int, int, int, int).
	protected final Object paintLock = new Object();
	protected boolean needsRepaint = false;
	protected int paintX, paintY, paintW, paintH;

	protected Canvas()
	{
		Mobile.log(Mobile.LOG_INFO, Canvas.class.getPackage().getName() + "." + Canvas.class.getSimpleName() + ": " + "Create Canvas:"+width+", "+height);

		barHeight = Font.getDefaultFont().getHeight();
	}

	// Constructor called by GameCanvas
	protected Canvas(boolean suppressKeys)
	{
		Mobile.log(Mobile.LOG_INFO, Canvas.class.getPackage().getName() + "." + Canvas.class.getSimpleName() + ": " + "Create Canvas:"+width+", "+height+" suppressKeys:"+suppressKeys);

		barHeight = Font.getDefaultFont().getHeight();

		suppressKeyEvents = suppressKeys;
	}

	public int getGameAction(int keyCode) { return Mobile.getGameAction(keyCode); }

	public int getKeyCode(int gameAction)
	{
		switch(gameAction) // Look on Mobile.java for what these magic numbers mean ("J2ME Canvas standard keycodes")
		{
			case KEY_NUM2:   return Mobile.getMobileKey(14);
			case KEY_NUM8:   return Mobile.getMobileKey(17);
			case KEY_NUM4:   return Mobile.getMobileKey(15);
			case KEY_NUM6:   return Mobile.getMobileKey(16);
			case KEY_NUM5:   return Mobile.getMobileKey(18);
			case UP:         return Mobile.getMobileKey(0);
			case DOWN:       return Mobile.getMobileKey(1);
			case LEFT:       return Mobile.getMobileKey(2);
			case RIGHT:      return Mobile.getMobileKey(3);
			case FIRE:       return Mobile.getMobileKey(7);

			case GAME_A: case KEY_NUM1: return Mobile.getMobileKey(10);
			case GAME_B: case KEY_NUM3: return Mobile.getMobileKey(11);
			case GAME_C: case KEY_NUM7: return Mobile.getMobileKey(5);
			case GAME_D: case KEY_NUM9: return Mobile.getMobileKey(4);

			case KEY_NUM0:  return Mobile.getMobileKey(6);
			case KEY_STAR:  return Mobile.getMobileKey(12);
			case KEY_POUND: return Mobile.getMobileKey(13);
		}
		return 0;
	}

	// Used in SKT lcdui classes so we can put them on top of MIDP ones
	public int SKTToMIDPKey(int sktKey)
	{
		switch(sktKey)
		{
			case KEY_CLR: return 0;
			case KEY_COML: return 0;
			case KEY_COMC: return 0;
			case KEY_COMR: return 0;
			case KEY_UP: return UP;
			case KEY_LEFT: return LEFT;
			case KEY_RIGHT: return RIGHT;
			case KEY_DOWN: return DOWN;
			case KEY_FIRE: return FIRE;
			case KEY_CALL: return 0;
			case KEY_END: return 0;
			case KEY_FLIP_OPEN: return 0;
			case KEY_FLIP_CLOSE: return 0;
			case KEY_VOL_UP: return 0;
			case KEY_VOL_DOWN: return 0;
			default: return 0;
		}
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

	public void repaint(final int x, final int y, final int width, final int height)
	{
		// Also check if repaints are being serviced here, some jars like Garfield's House add repaint calls in a separate thread from that blocked by serviceRepaints
		if (!isShown() || listCommands) { return; }

		if(!Mobile.compatImmediateRepaints)
		{
			boolean postEvent = false;
			synchronized (paintLock)
			{
				if (!needsRepaint)
				{
					paintX = x;
					paintY = y;
					paintW = width;
					paintH = height;
					needsRepaint = true;
					postEvent = true;
				}
				else
				{
					// Unionize clipping bounds for consecutive repaints, so
					// we don't keep stacking draw operations on top of others.
					int x2 = Math.max(paintX + paintW, x + width);
					int y2 = Math.max(paintY + paintH, y + height);
					paintX = Math.min(paintX, x);
					paintY = Math.min(paintY, y);
					paintW = x2 - paintX;
					paintH = y2 - paintY;
				}
			}
			// Schedule the paint request for later processing
			if(postEvent)
			{
				Mobile.getDisplay().postPaintRequest(new Runnable()
				{
					@Override
					public void run()
					{
						repaintRequest();
					}
				});
			}
		}
		else // Immediately process the paint event
		{
			synchronized (paintLock)
			{
				paintX = x;
				paintY = y;
				paintW = width;
				paintH = height;
				needsRepaint = true;
			}
			repaintRequest();
		}
	}

	public void repaintRequest()
	{
		// These can be called from another thread, at any time (even if the canvas is no longer visible),
		// so ignore any repaints in cases where it isn't visible
		if (!isShown() || listCommands) { return; }

		int renderX, renderY, renderW, renderH;

		synchronized (paintLock)
		{
			if (!needsRepaint)
			{
				paintLock.notifyAll();
				return;
			}
			renderX = paintX;
			renderY = paintY;
			renderW = paintW;
			renderH = paintH;
		}

		try
		{
			synchronized(graphics)
			{
				graphics.reset(renderX, renderY, renderW, renderH);
				paint(graphics);
			}
		}
		catch(NullPointerException npe)
		{
			Mobile.log(Mobile.LOG_ERROR, Canvas.class.getPackage().getName() + "." + Canvas.class.getSimpleName() + ": " + "Null Pointer Exception in draw event: " + npe.getMessage());
			npe.printStackTrace();
			//throw new NullPointerException("Null Pointer Exception in draw event");
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, Canvas.class.getPackage().getName() + "." + Canvas.class.getSimpleName() + ": " + "Serious Exception hit in repaint(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			// Unblock any threads waiting in serviceRepaints()
			synchronized (paintLock)
			{
				needsRepaint = false;
				paintLock.notifyAll();
			}
		}

		// Draw command bar whenever the canvas is not fullscreen and there are commands in the bar, and always queue it to draw after the flush
		if (!fullscreen && !commands.isEmpty())
		{
			Mobile.getPlatform().setPostFlushDraw(new Runnable()
			{
				@Override
				public void run() { paintCommandsBar(); }
			});
		}

		Mobile.getPlatform().flushGraphics(platformImage, renderX, renderY, renderW, renderH);
	}

	public void serviceRepaints()
	{
		if (!isShown()) { return; }

		// If it was called directly from the Paint/EDT Thread, process now
		// to avoid possible deadlocks (java threading is finicky for J2ME,
		// so you never know)
		if (Mobile.getDisplay().isEventThread())
		{
			repaintRequest();
			return;
		}

		synchronized (paintLock)
		{
			// Block caller thread until the event thread clears the paint flag
			while (needsRepaint)
			{
				try { paintLock.wait(); }
				catch (InterruptedException e)
					{ Thread.currentThread().interrupt(); }
			}
		}
	}

	public void setFullScreenMode(boolean mode)
	{
		if (mode != fullscreen) { fullscreen = mode; }
	}

	public void showNotify() { }

	protected void sizeChanged(int w, int h)
	{
		width = w;
		height = h;
	}

	public int getHeight()
	{
		if (Mobile.isKDDI) { return (height - ((!fullscreen ) ? barHeight : 0)); }
		return height;
	}

	public boolean getFullScreen() { return fullscreen; }

	private void paintCommandsBar()
	{
		// The command bar shouldn't influence canvas drawing operations, so it's added directly to the frontBuffer after swapping.
		javax.microedition.lcdui.Graphics graphics = Mobile.getPlatform().getLcdFrontbufferGraphics();

		// Fade the command bar if there's one second left to hide it
		long fadeStart = 1000000000L;
		if (MobilePlatform.timeToUnfocus < fadeStart)
		{
			graphics.setAlphaRGB(((byte)(0xFF * Math.max(0, Math.min(1, MobilePlatform.timeToUnfocus / 1000000000.0))) << 24) | Mobile.lcduiBGColor);
			graphics.fillRect(0, Mobile.lcdHeight-barHeight, Mobile.lcdWidth, barHeight);
			graphics.setAlphaRGB(((byte)(0xFF * Math.max(0, Math.min(1, MobilePlatform.timeToUnfocus / 1000000000.0))) << 24) | Mobile.lcduiTextColor);
		}
		else
		{
			graphics.setAlphaRGB((0xFF << 24) | Mobile.lcduiBGColor);
			graphics.fillRect(0, Mobile.lcdHeight-barHeight, Mobile.lcdWidth, barHeight);
			graphics.setAlphaRGB((0xFF << 24) | Mobile.lcduiTextColor);
		}

		graphics.drawLine(0, Mobile.lcdHeight-barHeight, Mobile.lcdWidth, Mobile.lcdHeight-barHeight);
		graphics.drawLine(Mobile.lcdWidth/2, Mobile.lcdHeight-barHeight, Mobile.lcdWidth/2, Mobile.lcdHeight);

		// Command text drawing
		int textCenter;
		int xPos;

		if (!commands.isEmpty())
		{
			String label = commands.size() > 2 ? "Options" : commands.get(0).getLabel();
			textCenter = (graphics.getGraphics2D().getFontMetrics().stringWidth(label))/2;
			xPos = (Mobile.lcdWidth / 4) - textCenter;
			graphics.drawString(label, xPos, Mobile.lcdHeight-barHeight, Graphics.LEFT);

			textCenter = (graphics.getGraphics2D().getFontMetrics().stringWidth(commands.size() > 1 ? commands.get(1).getLabel() : ""))/2;
			xPos = (3 * Mobile.lcdWidth / 4) + textCenter;
			graphics.drawString(commands.size() > 1 ? commands.get(1).getLabel() : "", xPos, Mobile.lcdHeight-barHeight, Graphics.RIGHT);
		}
	}

	public void addCommand(Command cmd)	{ super.addCommand(cmd); }

	public void removeCommand(Command cmd) { super.removeCommand(cmd); }

	protected void render()
	{
		if (listCommands) { super.render(); }
		else { repaint(); }
	}

	public final boolean areKeysSuppressed() { return suppressKeyEvents; }
}
