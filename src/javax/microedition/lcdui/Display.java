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

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Displayable;
import javax.microedition.midlet.MIDlet;

import javax.microedition.lcdui.Image;

import java.util.Vector;
import java.util.Timer;
import java.util.TimerTask;

import java.util.concurrent.locks.ReentrantLock;

import org.recompile.mobile.Mobile;

public class Display
{
	// when fps is limited, this needs to be a fair lock
	// otherwise key events might not get dispatched at proper time
	public static final ReentrantLock LCDUILock = new ReentrantLock(true);

	// this can only be used from a dedicated thread which MAY NOT hold lcduilock
	public static final Object calloutLock = new Object();

	public static final int LIST_ELEMENT = 1;
	public static final int CHOICE_GROUP_ELEMENT = 2;
	public static final int ALERT = 3;
	public static final int COLOR_BACKGROUND = 0;
	public static final int COLOR_FOREGROUND = 1;
	public static final int COLOR_HIGHLIGHTED_BACKGROUND = 2;
	public static final int COLOR_HIGHLIGHTED_FOREGROUND = 3;
	public static final int COLOR_BORDER = 4;
	public static final int COLOR_HIGHLIGHTED_BORDER = 5;

	private Displayable current;

	private static Display display;

	public Vector<Runnable> serialCalls;

	private Timer timer;

	private SerialCallTimerTask timertask;

	private boolean isSettingCurrent = false;

	private Thread flashThread;

	public Display()
	{
		display = this;

		Mobile.setDisplay(this);

		serialCalls = new Vector<Runnable>(16);
		timer = new Timer();
		timertask = new SerialCallTimerTask();
		timer.schedule(timertask, 0, 17);
	}

	public void callSerially(Runnable r)
	{
		LCDUILock.lock();
		try { serialCalls.add(r); } 
		finally { LCDUILock.unlock(); }
	}
	private class SerialCallTimerTask extends TimerTask
	{
		public void run()
		{
			if(!serialCalls.isEmpty())
			{
				try
				{
					synchronized (calloutLock)
					{
						LCDUILock.lock();
						try 
						{
							Runnable call = serialCalls.get(0);
							serialCalls.removeElementAt(0);
							call.run();
						} finally { LCDUILock.unlock(); }
					}
				}
				catch (Exception e) { }
			}
		}
	}

	public boolean flashBacklight(int duration) 
	{
		try 
		{
			if (flashThread != null && flashThread.isAlive()) 
			{
				flashThread.interrupt();
				Mobile.renderLCDMask = false;
			}
			flashThread = new Thread(() -> 
			{
				Mobile.renderLCDMask = true;
				try { Thread.sleep((duration == Integer.MAX_VALUE) ? Long.MAX_VALUE : duration); } // If backlight is Int MAX_VALUE, that means it should stay on.
				catch(Exception e) {}
				Mobile.renderLCDMask = false;
			});
			flashThread.start();
		}
		catch(Exception e) { Mobile.log(Mobile.LOG_ERROR, Display.class.getPackage().getName() + "." + Display.class.getSimpleName() + ": " + "Failed to flash Backlight: "+ e.getMessage()); }
		return true;
	}

	public int getBestImageHeight(int imageType)
	{
		switch(imageType)
		{
			case LIST_ELEMENT: return Mobile.getPlatform().lcdHeight / 8;
			case CHOICE_GROUP_ELEMENT: return Mobile.getPlatform().lcdHeight / 8;
			case ALERT: return Mobile.getPlatform().lcdHeight;
		}
		return Mobile.getPlatform().lcdHeight;
	}

	public int getBestImageWidth(int imageType) { return Mobile.getPlatform().lcdWidth; }

	public int getBorderStyle(boolean highlighted) { return 0; }

	public int getColor(int colorSpecifier)
	{
		switch(colorSpecifier)
		{
			case COLOR_BACKGROUND: return 0;
			case COLOR_FOREGROUND: return 0xFFFFFF;
			case COLOR_HIGHLIGHTED_BACKGROUND: return 0xFFFFFF;
			case COLOR_HIGHLIGHTED_FOREGROUND: return 0;
			case COLOR_BORDER: return 0x808080;
			case COLOR_HIGHLIGHTED_BORDER: return 0xFFFFFF;
		}
		return 0;
	}

	public Displayable getCurrent() { return current; }

	public static Display getDisplay(MIDlet m) { return display; }

	public boolean isColor() { return true; }

	public int numAlphaLevels() { return 256; }

	public int numColors() { return 16777216; }

	public void setCurrent(Displayable next)
	{
		if (next == null) { return; }

		LCDUILock.lock();
		try 
		{		
			if(current == next || isSettingCurrent) { return; }
			try
			{
				isSettingCurrent = true;
				try 
				{
					if (current != null) { current.hideNotify(); }
					Mobile.getPlatform().keyState = 0; // reset keystate
					next.showNotify();
				} 
				finally { isSettingCurrent = false; }
				if(next instanceof Alert) { ((Alert) next).setNextScreen(current); }
				current = next;
				current.notifySetCurrent();
				Mobile.getPlatform().flushGraphics(current.platformImage, 0,0, current.width, current.height);
				Mobile.log(Mobile.LOG_DEBUG, Display.class.getPackage().getName() + "." + Display.class.getSimpleName() + ": " + "Set Current "+current.width+", "+current.height);
			}
			catch (Exception e)
			{
				Mobile.log(Mobile.LOG_ERROR, Display.class.getPackage().getName() + "." + Display.class.getSimpleName() + ": " + "Problem with setCurrent(next)");
				e.printStackTrace();
			}
		} 
		finally 
		{ 
			LCDUILock.unlock();
			Mobile.displayUpdated = true;
		}
	}

	public void setCurrent(Alert alert, Displayable next)
	{
		try
		{
			setCurrent(alert);
			alert.setNextScreen(next);
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, Display.class.getPackage().getName() + "." + Display.class.getSimpleName() + ": " + "Problem with setCurrent(alert, next)");
			e.printStackTrace();
		}
	}

	public void setCurrentItem(Item item) 
	{
		Form form = item.getOwner();
		if (form != null) 
		{
			if (form != current) { setCurrent(form); }
			form.focusItem(item);
		}
	}

	public boolean vibrate(int duration)
	{
		Mobile.vibrationDuration = duration;
		Mobile.log(Mobile.LOG_DEBUG, Display.class.getPackage().getName() + "." + Display.class.getSimpleName() + ": " + "Vibrate");
		return true;
	}

}
