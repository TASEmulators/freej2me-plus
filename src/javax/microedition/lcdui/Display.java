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

import java.util.concurrent.atomic.AtomicReference;
import java.util.LinkedList;
import java.util.Queue;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.MobilePlatform;

public class Display
{
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

	private static final Queue<Runnable> serializedEvents = new LinkedList<Runnable>(), inputEvents = new LinkedList<Runnable>();

	private static final AtomicReference<Runnable> setCurrentRequest = new AtomicReference<Runnable>(), paintEvent = new AtomicReference<Runnable>();

	private Thread flashThread;

	public Display() 
	{ 
		new Thread(new Runnable() 
		{
			@Override
			public void run() { processEvents(); }
		}, "EventProcessing-Thread").start();
	}

	// MIDlet serial call queue methods
	public void callSerially(final Runnable r) 
	{ 
		synchronized (serializedEvents) 
		{ 
			serializedEvents.add(r); 
			serializedEvents.notify(); 
		}
	}

	// Paint events should be serialized as well (if the event queue is empty, we need to notify the event processing thread to resume)
    public void postPaintRequest(final Runnable r) 
	{ 
		synchronized (serializedEvents) 
		{
			paintEvent.set(r);
			serializedEvents.notify();
		}
	}

	/* 
	 * Input events should be added in a separate thread since FreeJ2ME-Plus is the one issuing them, different from paint events which only the app will request
	 * Not doing this in a thread has the potential to freeze apps like Konami's Rush and Attack and Ratatouille.
	 * 
	 * Note that this also synchronizes on the serial queue, as input events will be processed in the same thread that serial calls and repaints are, to approximate
	 * the spec's need for them all to be serialized in respect to each other (except inputs can't be truly serialized into serializedEvents because apps like
	 * Heroes Lore: Wind of Soltia spam the queue very hard and make input processing completely unreliable)
	 */
	public void postInputEvent(final Runnable r) 
	{ 
		new Thread(new Runnable() 
		{
			@Override
			public void run() 
			{
				synchronized(inputEvents) { inputEvents.add(r); }
				synchronized (serializedEvents) 
				{
					serializedEvents.notify();
				}
			}
		}).start();
	}

	private void processEvents() 
	{
		Runnable call = null;
		while(true) 
		{
			/* 
			 * MIDP docs don't specify anything exact on when setCurrent should be processed, it just says it is not guaranteed to happen before the "next event delivery"
			 * so let's do it right before any events.
			 */
			call = setCurrentRequest.getAndSet(null);
			if(call != null) { call.run(); }

			synchronized (serializedEvents) 
			{
				while(serializedEvents.isEmpty() && inputEvents.isEmpty() && paintEvent.get() == null  && setCurrentRequest.get() == null) // If we have no serial events to process, and no current displayable change, wait.
				{
					try { serializedEvents.wait(); }
					catch (Exception e) { }
				}

				// Run paint event in sync with the serial queue, always before the serial call's run()
				call = paintEvent.getAndSet(null);
				if(call != null) { call.run(); }
				
				call = serializedEvents.poll(); 
				if(call != null) 
				{ 
					try { call.run(); }
					catch(Exception e) 
					{ 
						Mobile.log(Mobile.LOG_WARNING, Display.class.getPackage().getName() + "." + Display.class.getSimpleName() + ": " + "Failed to run callSerially event: "+ e.getMessage() + " retrying after a 1000ms delay."); 
						try { Thread.sleep(1000); }
						catch(Exception ex) { }
						call.run();
					}
				}
			}

			// Process all pending inputs added since the previous thread loop, after any previously pending events were processed.
			synchronized(inputEvents) 
			{ 
				while(!inputEvents.isEmpty()) 
				{ 
					call = inputEvents.poll();
					if(call != null) { call.run(); }
				}
			}
		}
	}

	public void processPaintsNow() // Used by Canvas.serviceRepaints() to force repaints to be serviced
	{
		Runnable call = paintEvent.getAndSet(null);
		if(call != null) { call.run(); } // Only run Paint Events
		else { return; }
	}

	public boolean flashBacklight(final int duration) 
	{
		try 
		{
			if (flashThread != null && flashThread.isAlive()) 
			{
				flashThread.interrupt();
				Mobile.renderLCDMask = false;
			}
			flashThread = new Thread(new Runnable()
			{
				@Override
				public void run() 
				{
					Mobile.renderLCDMask = true;
					try { Thread.sleep((duration == Integer.MAX_VALUE) ? Long.MAX_VALUE : duration); } // If backlight is Int MAX_VALUE, that means it should stay on.
					catch(Exception e) {}
					Mobile.renderLCDMask = false;
				}
			});
			flashThread.start();
		}
		catch(Exception e) { Mobile.log(Mobile.LOG_ERROR, Display.class.getPackage().getName() + "." + Display.class.getSimpleName() + ": " + "Failed to flash Backlight: "+ e.getMessage()); }
		return true;
	}

	public boolean vodafoneFlashBacklight(final int duration, final int offDuration, final int reps) 
	{
		try 
		{
			if (flashThread != null && flashThread.isAlive()) 
			{
				flashThread.interrupt();
				Mobile.renderLCDMask = false;
			}
			flashThread = new Thread(new Runnable()
			{
				@Override
				public void run() 
				{
					for(int i = 0; i < reps; i++) 
					{
						Mobile.renderLCDMask = true;
						try { Thread.sleep(duration);}
						catch(Exception e) {}

						Mobile.renderLCDMask = false;
						try { Thread.sleep(offDuration); }
						catch(Exception e) {}
					}
				}
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

	public int getBorderStyle(boolean highlighted) 
	{ 
		if(highlighted) { return Graphics.SOLID; }
		else { return Graphics.DOTTED; }
	}

	public int getColor(int colorSpecifier)
	{
		switch(colorSpecifier)
		{
			case COLOR_BACKGROUND: return Mobile.lcduiBGColor;
			case COLOR_FOREGROUND: return Mobile.lcduiTextColor;
			case COLOR_HIGHLIGHTED_BACKGROUND: return Mobile.lcduiTextColor;
			case COLOR_HIGHLIGHTED_FOREGROUND: return Mobile.lcduiBGColor;
			case COLOR_BORDER: return Mobile.lcduiStrokeColor;
			case COLOR_HIGHLIGHTED_BORDER: return Mobile.lcduiBGColor;
		}
		return 0;
	}

	public Displayable getCurrent() { return current; }

	public static Display getDisplay(MIDlet m) 
	{
		if(m == null) { throw new NullPointerException("Cannot get a unique Display for a null MIDlet"); } 
		
		return Mobile.getDisplay(); 
	}

	public boolean isColor() { return true; }

	public int numAlphaLevels() { return 256; }

	public int numColors() { return Integer.MAX_VALUE; }

	public void setCurrent(final Displayable next)
	{
		setCurrentRequest.set(new Runnable()
		{
			@Override
			public void run() 
			{
				Displayable prev = current;
				if (next == null || current == next) { return; }

				// Alert and Canvas preparation
				try 
				{
					if(next instanceof Alert) { ((Alert) next).setNextScreen(current); }
					
					// Call upon showNotify right before the new canvas is made visible
					if (next instanceof Canvas) { ((Canvas) next).showNotify(); }
				}
				catch (Exception e)
				{
					Mobile.log(Mobile.LOG_ERROR, Display.class.getPackage().getName() + "." + Display.class.getSimpleName() + ": " + "SetCurrent Alert/Canvas preparation block failed: " + e.getMessage());
					e.printStackTrace();
				}

				// displayable setup and hideNotify
				try 
				{
					// Make the new displayable effective
					current = next;

					// Call upon hideNotify right after removing the previous canvas from the display
					if (prev instanceof Canvas) { ((Canvas) prev).hideNotify(); }
				}
				catch (Exception e)
				{
					Mobile.log(Mobile.LOG_ERROR, Display.class.getPackage().getName() + "." + Display.class.getSimpleName() + ": " + "SetCurrent displayable setup and hideNotify block failed: " + e.getMessage());
					e.printStackTrace();
				}

				// Paint displayable block
				try 
				{
					// Some jars call upon a canvas repaint() once they're ready. If the canvas still hasn't been shown at this time, wait a bit longer before forcing a repaint
					if(current instanceof Canvas && !((Canvas) current).hasBeenDrawnAfterSet())
					{ 
						int maxWait = 66; // Wait for a max of 66ms (~15fps interval), i don't want to start littering FreeJ2ME-Plus with compatibility flags

						while(!((Canvas) current).hasBeenDrawnAfterSet() && maxWait > 0) 
						{
							Thread.sleep(1);
							maxWait--;
						}

						// Still wasn't shown by the application itself? Force it to be
						if(!((Canvas) current).hasBeenDrawnAfterSet()) { ((Canvas) current).repaint(0, 0, current.getWidth(), current.getHeight()); }
					}
					else { current.notifySetCurrent(); } // Displayables other than canvas have drawing dictated entirely by FreeJ2ME, so always force a draw to happen on setCurrent
				}
				catch (Exception e)
				{
					Mobile.log(Mobile.LOG_ERROR, Display.class.getPackage().getName() + "." + Display.class.getSimpleName() + ": " + "SetCurrent paint block failed: " + e.getMessage());
					e.printStackTrace();
				}

				Mobile.log(Mobile.LOG_DEBUG, Display.class.getPackage().getName() + "." + Display.class.getSimpleName() + ": " + "Set Current "+current.width+", "+current.height);
			}
		});
		if(Mobile.compatImmediateRepaints) { setCurrentRequest.getAndSet(null).run(); }
		else { synchronized(serializedEvents) { serializedEvents.notify(); } }
	}

	public void setCurrent(final Alert alert, final Displayable next)
	{
		setCurrentRequest.set(new Runnable()
		{
			@Override
			public void run() 
			{
				if(alert == null || next == null) { throw new NullPointerException("Cannot pass a null alert or next displayable into setCurrent(Alert, Displayable)"); }
				if(next instanceof Alert) { throw new IllegalArgumentException("Cannot pass an alert as the next screen of another alert in setCurrent(Alert, Displayable)"); }

				try
				{
					alert.setNextScreen(next);

					current = alert;
					current.notifySetCurrent();

					Mobile.log(Mobile.LOG_DEBUG, Display.class.getPackage().getName() + "." + Display.class.getSimpleName() + ": " + "Set Current Alert "+current.width+", "+current.height);	
				}
				catch (Exception e)
				{
					Mobile.log(Mobile.LOG_ERROR, Display.class.getPackage().getName() + "." + Display.class.getSimpleName() + ": " + "Problem with setCurrent(alert, next)");
					e.printStackTrace();
				}
			}
		});
		if(Mobile.compatImmediateRepaints) { setCurrentRequest.getAndSet(null).run(); }
		else { synchronized(serializedEvents) { serializedEvents.notify(); } }
	}

	public void setCurrentItem(final Item item) 
	{
		setCurrentRequest.set(new Runnable()
		{
			@Override
			public void run() 
			{
				Form form = item.getOwner();
				if (form != null) 
				{
					if (form != current) { setCurrent(form); }
					form.focusItem(item);
				}
			}
		});
		if(Mobile.compatImmediateRepaints) { setCurrentRequest.getAndSet(null).run(); }
		else { synchronized(serializedEvents) { serializedEvents.notify(); } }
	}

	public boolean vibrate(int duration)
	{
		Mobile.vibrationDuration = duration;
		Mobile.log(Mobile.LOG_DEBUG, Display.class.getPackage().getName() + "." + Display.class.getSimpleName() + ": " + "Vibrate");
		return true;
	}
}
