/*
	This file is part of FreeJ2ME.

	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	FreeJ2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILTY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/
*/
package net.rim.device.api.ui;

import java.util.Vector;

import net.rim.device.api.system.Application;

// Should be abstract, but i'm not bothering with that...
public class UiApplication extends Application implements UiEngine
{
	// Screen management stack
	private Vector<Screen> screens = new Vector<Screen>();
	private boolean paintingSuspended = false;

	public static final UiApplication getUiApplication()
	{
		// We know this will always return a UiApplication.
		return (UiApplication) Application.getApplication();
	}


	// Left to implement later.
	public final UiEngineInstance getUiEngineInstance() { return null; }

	public final void suspendPainting(boolean suspend)
	{
		if (suspend && paintingSuspended) { throw new IllegalStateException("Painting is already suspended."); }
		if (!suspend && !paintingSuspended)
		{
			throw new IllegalStateException("Painting is not currently suspended.");
		}
		paintingSuspended = suspend;
	}

	public final boolean isPaintingSuspended() { return paintingSuspended; }

	public final void updateDisplay()
	{
		if (!paintingSuspended) { repaint(); }
	}

	public final int getScreenCount()
	{
		synchronized (getEventLock()) { return screens.size(); }
	}

	public final Screen getActiveScreen()
	{
		synchronized (getEventLock())
		{
			if (screens.isEmpty()) { return null; }

			return (Screen) screens.lastElement();
		}
	}

	public final void pushScreen(Screen screen)
	{
		if (screen == null) { throw new NullPointerException("Screen cannot be null."); }

		synchronized (getEventLock())
		{
			if (screens.contains(screen)) { throw new IllegalArgumentException("Screen is already on the stack."); }

			screens.addElement(screen);
			if (!paintingSuspended) { repaint(); }
		}
	}

	public final void popScreen(Screen screen)
	{
		if (screen == null) { throw new NullPointerException("Screen cannot be null."); }

		synchronized (getEventLock())
		{
			if (!screens.contains(screen))
			{
				throw new IllegalArgumentException("Screen is not on the stack.");
			}

			screens.removeElement(screen);

			if (!paintingSuspended) { repaint(); }
		}
	}

	public final void pushModalScreen(Screen screen)
	{
		if (screen == null)
		{
			throw new NullPointerException("Screen cannot be null.");
		}
		if (!isEventDispatchThread())
		{
			throw new IllegalStateException("Modal screens must be pushed on the event thread.");
		}

		pushScreen(screen);

		try
		{
			while (screens.contains(screen) && isAlive()) { Thread.sleep(50); }
		}
		catch (InterruptedException e) { Thread.currentThread().interrupt(); }
	}

	public final void pushGlobalScreen(Screen screen, int priority, int flags)
	{
		if (screen == null) { throw new NullPointerException("Screen cannot be null."); }

		pushScreen(screen);
	}

	public final void pushGlobalScreen(Screen screen, int priority, boolean inputRequired)
	{
		pushGlobalScreen(screen, priority, 0);
	}

	public final void queueStatus(Screen screen, int priority, boolean inputRequired)
	{
		pushGlobalScreen(screen, priority, 0);
	}

	public final void dismissStatus(Screen screen) { popScreen(screen); }

	public final void repaint()
	{
		if (paintingSuspended) { return; }

		Screen active = getActiveScreen();

		if (active != null)
		{
			// TODO: Paint
		}
	}

	public final void relayout()
	{
		synchronized (getEventLock())
		{
			// Relayout all screens on stack
			repaint();
		}
	}
}
