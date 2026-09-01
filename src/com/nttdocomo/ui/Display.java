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
package com.nttdocomo.ui;

import java.util.concurrent.atomic.AtomicReference;
import java.util.LinkedList;
import java.util.Queue;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.MobilePlatform;

public class Display
{
	// Numeric Keys
	public static final int KEY_0 = 0x00;
	public static final int KEY_1 = 0x01;
	public static final int KEY_2 = 0x02;
	public static final int KEY_3 = 0x03;
	public static final int KEY_4 = 0x04;
	public static final int KEY_5 = 0x05;
	public static final int KEY_6 = 0x06;
	public static final int KEY_7 = 0x07;
	public static final int KEY_8 = 0x08;
	public static final int KEY_9 = 0x09;
	public static final int KEY_ASTERISK = 0x0a;
	public static final int KEY_POUND = 0x0b;

	// Directional Keys
	public static final int KEY_UP = 0x11;
	public static final int KEY_DOWN = 0x13;
	public static final int KEY_LEFT = 0x10;
	public static final int KEY_RIGHT = 0x12;
	public static final int KEY_SELECT = 0x14;
	public static final int KEY_LOWER_LEFT = 0x1d;
	public static final int KEY_LOWER_RIGHT = 0x1c;
	public static final int KEY_UPPER_LEFT = 0x1a;
	public static final int KEY_UPPER_RIGHT = 0x1b;

	// Soft Keys
	public static final int KEY_SOFT1 = 0x15;
	public static final int KEY_SOFT2 = 0x16;

	// Camera Keys
	public static final int KEY_CAMERA_SELECT = 0x3b;
	public static final int KEY_CAMERA_ZOOM_IN = 0x39;
	public static final int KEY_CAMERA_ZOOM_OUT = 0x3a;

	// Miscellaneous Keys
	public static final int KEY_CLEAR = 0x20;
	public static final int KEY_GPS = 0x2a;
	public static final int KEY_IAPP = 0x18;
	public static final int KEY_MAIL = 0x21;
	public static final int KEY_MEMO = 0x22;
	public static final int KEY_MY_SELECT = 0x35;
	public static final int KEY_PAGE_DOWN = 0x1f;
	public static final int KEY_PAGE_UP = 0x1e;

	// Roll Keys
	public static final int KEY_ROLL_LEFT = 0x30;
	public static final int KEY_ROLL_RIGHT = 0x31;

	// Sub Keys
	public static final int KEY_SUB1 = 0x32;
	public static final int KEY_SUB2 = 0x33;
	public static final int KEY_SUB3 = 0x34;

	// Events
	public static final int KEY_PRESSED_EVENT = 0;
	public static final int KEY_RELEASED_EVENT = 1;
	public static final int FINGER_MOVED_EVENT = 0x41;
	public static final int POINTER_MOVED_EVENT = 0x40;
	public static final int MEDIA_EVENT = 8;
	public static final int RESET_VM_EVENT = 5;
	public static final int RESUME_VM_EVENT = 4;
	public static final int TIMER_EXPIRED_EVENT = 7;
	public static final int UPDATE_VM_EVENT = 6;

	// Maximum and Minimum constants
	protected static final int MAX_OPTION_KEY = 0x3f;
	protected static final int MIN_OPTION_KEY = 0x1a;
	protected static final int MAX_VENDOR_EVENT = 127;
	protected static final int MIN_VENDOR_EVENT = 64;
	protected static final int MAX_VENDOR_KEY = 127;
	protected static final int MIN_VENDOR_KEY = 64;

	protected static Frame current = null;

	private static final Object eventLock = new Object();
	private static final AtomicReference<Runnable> setCurrentRequest = new AtomicReference<Runnable>();
	private static final AtomicReference<Runnable> paintEvent = new AtomicReference<Runnable>();

	private static Queue<Runnable> inputEvents = new LinkedList<Runnable>();

	private static Thread eventThread;

	static
	{
		eventThread = new Thread(new Runnable()
		{
			@Override
			public void run() { processEvents(); }
		}, "DoJaEventProcessing-Thread");

		eventThread.start();
	}

	public static boolean isEventThread() { return Thread.currentThread() == eventThread; }

	public void postInputEvent(final Runnable r)
	{
		if (r == null) { return; }
		synchronized (eventLock)
		{
			inputEvents.add(r);
			eventLock.notifyAll();
		}
	}

	public void postPaintRequest(final Runnable r)
	{
		if (r == null) { return; }

		synchronized (eventLock)
		{
			paintEvent.set(r);
			eventLock.notifyAll();
		}
	}

	private static void processEvents()
	{
		Runnable call = null;
		while(true)
		{
			/*
			 * MIDP docs don't specify anything exact on when setCurrent should be processed,
			 * it just says it is not guaranteed to happen before the "next event delivery"
			 * so let's do it right before any events.
			 */
			call = setCurrentRequest.getAndSet(null);
			if(call != null) { call.run(); }

			Runnable pendingPaint = null;
			int inputCount = 0;

			synchronized (eventLock)
			{
				// If we have no serial events to process, and no current displayable change, wait.
				while(inputEvents.isEmpty() && paintEvent.get() == null  &&
					setCurrentRequest.get() == null)
				{
					try { eventLock.wait(); }
					catch (Exception e) { }
				}

				pendingPaint = paintEvent.getAndSet(null);

				// For inputs we process only the ones that
				// were queued until this method was called.
				inputCount = inputEvents.size();
			}

			// Inputs go before anything else.
			while(inputCount > 0)
			{
				Runnable inputTask = null;
				synchronized (eventLock)
				{
					inputTask = inputEvents.poll();
				}
				if (inputTask != null) { inputTask.run(); }

				inputCount--;
			}

			// Service paints
			if (pendingPaint != null) { pendingPaint.run(); }
		}
	}

	public static Frame getCurrent()
	{
		synchronized (eventLock) { return current; }
	}

	public static int getHeight() { return MobilePlatform.lcdHeight; }

	public static int getWidth() { return MobilePlatform.lcdWidth; }

	public static boolean isColor() { return true; }

	public static int numColors() { return Integer.MAX_VALUE; }

	public static void setCurrent(final Frame frame)
	{
		if (frame == null) { throw new NullPointerException("Frame cannot be null."); }
		if (frame instanceof Dialog) { throw new IllegalArgumentException("Cannot set a dialog as the current frame."); }

		Runnable runnable = new Runnable()
		{
			@Override
			public void run()
			{
				Frame prev = current;
				if (frame == prev) { return; }

				current = frame;

				// Force repaint or notify initial frame display
				try
				{
					if (current instanceof Canvas)
					{
						((Canvas) current).repaint(0, 0, current.getWidth(), current.getHeight());
					}
				}
				catch (Exception e)
				{
					Mobile.log(Mobile.LOG_ERROR, Display.class.getPackage().getName() + "." + Display.class.getSimpleName() + ": " + "SetCurrent paint block failed: " + e.getMessage());
					e.printStackTrace();
				}

				Mobile.log(Mobile.LOG_DEBUG, Display.class.getPackage().getName() + "." + Display.class.getSimpleName() + ": " + "DoJa Set Current "+current.getWidth()+", "+current.getHeight());
			}
		};

		if (Mobile.compatImmediateRepaints || isEventThread())
		{
			runnable.run();
		}
		else
		{
			setCurrentRequest.set(runnable);
			synchronized (eventLock) { eventLock.notifyAll(); }
		}
	}
}
