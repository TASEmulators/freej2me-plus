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
	private boolean servicing = false;
	private boolean firstDrawn = false;

	private AtomicBoolean pendingRepaint = new AtomicBoolean(false);

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
			case Mobile.KEY_NUM2:   return Mobile.getMobileKey(14);
			case Mobile.KEY_NUM8:   return Mobile.getMobileKey(17);
			case Mobile.KEY_NUM4:   return Mobile.getMobileKey(15);
			case Mobile.KEY_NUM6:   return Mobile.getMobileKey(16);
			case Mobile.KEY_NUM5:   return Mobile.getMobileKey(18);
			case Mobile.GAME_UP:    return Mobile.getMobileKey(0);
			case Mobile.GAME_DOWN:  return Mobile.getMobileKey(1);
			case Mobile.GAME_LEFT:  return Mobile.getMobileKey(2);
			case Mobile.GAME_RIGHT: return Mobile.getMobileKey(3);
			case Mobile.GAME_FIRE:  return Mobile.getMobileKey(7);
	
			// GAME_A through D don't show up in documentation at all.
			case Mobile.GAME_A: case Mobile.KEY_NUM1: return Mobile.getMobileKey(10);
			case Mobile.GAME_B: case Mobile.KEY_NUM3: return Mobile.getMobileKey(11);
			case Mobile.GAME_C: case Mobile.KEY_NUM7: return Mobile.getMobileKey(5);
			case Mobile.GAME_D: case Mobile.KEY_NUM9: return Mobile.getMobileKey(4);

			case Mobile.KEY_NUM0:  return Mobile.getMobileKey(6);
			case Mobile.KEY_STAR:  return Mobile.getMobileKey(12);
			case Mobile.KEY_POUND: return Mobile.getMobileKey(13);
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

	public void repaint(final int x, final int y, final int width, final int height)
	{
		// Also check if repaints are being serviced here, some jars like Garfield's House add repaint calls in a separate thread from that blocked by serviceRepaints
		if (!isShown() || listCommands || servicing) { return; }

		if(!Mobile.compatImmediateRepaints) 
		{
			Mobile.getDisplay().postPaintRequest(new Runnable() 
			{
				@Override
				public void run() 
				{ 
					repaintRequest(x, y, width, height); 
					pendingRepaint.set(false);
				}
			}); 
			pendingRepaint.set(true);
		}
		else // Immediately process the paint event
		{
			repaintRequest(x, y, width, height); 
		}
	}

	public void repaintRequest(final int x, final int y, final int width, final int height) 
	{
		// These can be called from another thread, at any time (even if the canvas is no longer visible), so ignore any repaints in cases where it isn't visible
		if (!isShown() || listCommands) { return; }

		firstDrawn = true; // So that setCurrent knows whether this canvas has been shown by the application before forcing a repaint of its own (we don't need to finalize the paint call)

		try 
		{
			synchronized(graphics) 
			{
				graphics.reset(x, y, width, height); 
				paint(graphics); 
			}
		}
		catch(NullPointerException npe) 
		{
			throw new NullPointerException("Null Pointer Exception in draw event");
		}
		catch (Exception e) 
		{
			Mobile.log(Mobile.LOG_ERROR, Canvas.class.getPackage().getName() + "." + Canvas.class.getSimpleName() + ": " + "Serious Exception hit in repaint(): " + e.getMessage());
			e.printStackTrace();
		}

		// Draw command bar whenever the canvas is not fullscreen and there are commands in the bar
		if (!fullscreen && !commands.isEmpty()) { paintCommandsBar(); }

		Mobile.getPlatform().flushGraphics(platformImage, x, y, width, (!fullscreen && !commands.isEmpty()) ? height+barHeight : height); // Extend draw area if commands are visible
		Mobile.getPlatform().limitFps();
	}

	public void serviceRepaints() 
	{
		if(!isShown() || !pendingRepaint.get()) { return; }
	
		servicing = true;
		if(!MobilePlatform.pressedKeys[19]) // If the fast-forward key is pressed, ignore the waiting and force a repaint immediately
		{
			// serviceRepaints has to force pending repaints to happen, so initially wait until they have time to be serviced normally, or multiple retries were attempted and unsuccessful
			for(byte waitTime = 0; waitTime < 16; waitTime++) 
			{
				if(pendingRepaint.get()) 
				{
					try { Thread.sleep(1); } // Worst case scenario, this will sleep for a total of 16ms before serviceRepaints forces repaints to happen (60fps min force-refresh)
					catch (Exception e) { }
				}
				else { break; } // Good, the pending repaint was serviced, break out of the loop
			}
		}

		// If Repaints weren't serviced in a timely manner above, the alternative is to force them to happen
		Mobile.getDisplay().processPaintsNow(); 
		servicing = false;
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
		if (Mobile.isKDDI) {
			return (height - ((!fullscreen ) ? barHeight : 0));
		}
		return height;
	}

	public boolean getFullScreen() { return fullscreen; }

	private void paintCommandsBar() 
	{
		// LCDUI should work independently of the current graphics translation, so translate back to 0,0 before any drawing and restore at the end
		int restoreX = graphics.getTranslateX(), restoreY = graphics.getTranslateY();
		int clipX = graphics.getClipX(), clipY = graphics.getClipY(), clipW = graphics.getClipWidth(), clipH = graphics.getClipHeight();

		graphics.setOrigin(0, 0);
		graphics.clearClip();

		// Fade the command bar if there's one second left to hide it
		long fadeStart = 1000000000L;
		if (MobilePlatform.timeToUnfocus < fadeStart) 
		{
			graphics.setAlphaRGB(((byte)(0xFF * Math.max(0, Math.min(1, MobilePlatform.timeToUnfocus / 1000000000.0))) << 24) | Mobile.lcduiBGColor);
		} 
		else { graphics.setAlphaRGB((0xFF << 24) | Mobile.lcduiBGColor); }
		
		graphics.fillRect(0, height-barHeight, width, barHeight);

		if (MobilePlatform.timeToUnfocus < fadeStart) 
		{
			graphics.setAlphaRGB(((byte)(0xFF * Math.max(0, Math.min(1, MobilePlatform.timeToUnfocus / 1000000000.0))) << 24) | Mobile.lcduiTextColor);
		} 
		else { graphics.setAlphaRGB((0xFF << 24) | Mobile.lcduiTextColor); }
		graphics.drawLine(0, height-barHeight, width, height-barHeight);
		graphics.drawLine(width/2, height-barHeight, width/2, height);
		
		// Command text drawing
		int textCenter;
		int xPos;

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

		graphics.setOrigin(restoreX, restoreY);
		graphics.setClip(clipX, clipY, clipW, clipH);
	}

	public void addCommand(Command cmd)	{ super.addCommand(cmd); }

	public void removeCommand(Command cmd) { super.removeCommand(cmd); }

	protected void render() 
	{
		if (listCommands) { super.render(); } 
		else { repaint(); }
	}

	public final boolean hasBeenDrawnAfterSet() { return firstDrawn; }

	public final boolean areKeysSuppressed() { return suppressKeyEvents; }
}
