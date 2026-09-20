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
package net.rim.device.api.system;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.microedition.io.file.FileSystemListener;

import net.rim.device.api.ui.UiApplication;

import org.recompile.mobile.Mobile;

public class Application
{
	private static UiApplication instance = null;
	private static final Object globalEventLock = new Object();

	private KeyListener keyListener = null;
	private boolean acceptingKeyUp = true;

	private GlobalEventListener globalEventListener;
	private HolsterListener holsterListener;
	private SystemListener systemListener;
	private TrackwheelListener trackwheelListener;

	private Vector<AlertListener> alertListeners = new Vector<AlertListener>();
	private Vector<AudioListener> audioListeners = new Vector<AudioListener>();
	private Vector<FileSystemListener> fileSystemListeners = new Vector<FileSystemListener> ();
	//private Vector fileSystemJournalListeners = new Vector();
	private Vector<IOPortListener> ioPortListeners = new Vector<IOPortListener>();
	private Vector<RadioListener> radioListeners = new Vector<RadioListener>();
	private Vector<RealtimeClockListener> realtimeClockListeners = new Vector<RealtimeClockListener>();
	private Vector<PeripheralListener> peripheralListeners = new Vector<PeripheralListener>();
	private Vector<Object> mediaActionHandlers = new Vector<Object>();

	private boolean eventThreadEntered = false;
	private Thread eventThread = null;
	private boolean acceptEvents = true;
	private boolean isForegroundState = true;
	private int processId;
	private Object serviceMode;

	// Timer for handling repeated or delayed invokeLater requests
	private static final Timer backgroundTimer = new Timer(true);

	public static Application getApplication()
	{
		instance = new UiApplication();
		return instance;
	}

	protected boolean acceptsForeground() { return false; }

	public boolean shouldAppearInApplicationSwitcher() { return acceptsForeground(); }

	public boolean isInTouchCompatibilityMode()
	{
		return false;
	}

	public void activate() { }

	public void deactivate() { }

	public final boolean isForeground() { return isForegroundState; }

	public final void requestBackground()
	{
		if (isForegroundState)
		{
			isForegroundState = false;
			deactivate();
		}
	}

	public final void requestForeground()
	{
		if (!isForegroundState)
		{
			isForegroundState = true;
			activate();
		}
	}

	public boolean requestClose()
	{
		try
		{
			Mobile.midlet.requestDestroy(true);
			return true;
		}
		catch (Exception e) { return false; } // Failed to destroy MIDlet.
	}

	public boolean isAlive() { return Mobile.midlet != null; }

	public final int getProcessId() { return processId; }


	public Object getServiceMode() { return serviceMode; }

	public final void setServiceMode(Object serviceMode) { setServiceModeImpl(serviceMode); }

	protected void setServiceModeImpl(Object serviceMode) { this.serviceMode = serviceMode; }

	// Default behavior here is to just reject all suggestions
	public boolean suggestServiceMode(Object serviceMode) { return false;  }


	public static Object getEventLock()
	{
		Application app = getApplication();
		return app.getAppEventLock();
	}

	public final Object getAppEventLock() { return globalEventLock; }

	public void enterEventDispatcher()
	{
		synchronized (this)
		{
			if (eventThreadEntered) { throw new IllegalStateException("Event thread has already been entered."); }
			eventThreadEntered = true;
			eventThread = Thread.currentThread();
		}

		try
		{
			while (eventThreadEntered) { Thread.sleep(50); }
		}
		catch (InterruptedException e) { Thread.currentThread().interrupt(); }
	}

	public final void setAcceptEvents(boolean on) { this.acceptEvents = on; }

	public boolean hasEventThread() { return eventThreadEntered; }

	public boolean isEventThread() { return Thread.currentThread() == eventThread; }

	public static final boolean isEventDispatchThread()
	{
		Application app = getApplication();
		return app.isEventThread();
	}

	public final boolean isHandlingEvents() { return eventThreadEntered; }

	public final void invokeLater(Runnable runnable)
	{
		if (runnable == null) { throw new NullPointerException("Received null runnable"); }
		if (!acceptEvents || !hasEventThread()) { return; }
		new Thread(runnable, "BB-Event-Dispatcher").start();
	}

	public final int invokeLater(final Runnable runnable, long time, boolean repeat)
	{
		if (runnable == null) { throw new NullPointerException("Received null runnable"); }
		if (time <= 0) { throw new IllegalArgumentException("Invalid invoke time"); }
		if (!acceptEvents || !hasEventThread()) { return -1; }

		TimerTask task = new TimerTask()
		{
			public void run()
			{
				if (acceptEvents) { runnable.run(); }
			}
		};

		if (repeat) { backgroundTimer.scheduleAtFixedRate(task, time, time); }
		else { backgroundTimer.schedule(task, time); }

		return task.hashCode();
	}

	public final void cancelInvokeLater(int id)
	{
		// TODO
	}

	public final void invokeAndWait(Runnable runnable)
	{
		if (runnable == null) { throw new NullPointerException("Received null runnable"); }
		if (isEventDispatchThread()) { runnable.run(); }
		else
		{
			synchronized (globalEventLock) { runnable.run(); }
		}
	}


	public final void enableKeyUpEvents(boolean enable) { this.acceptingKeyUp = enable; }

	public final boolean acceptsKeyUpEvents() { return acceptingKeyUp; }

	public void addKeyListener(KeyListener listener)
	{
		if (listener == null) { throw new NullPointerException("Received null listener."); }
		this.keyListener = listener;
	}

	public void addKeyListener(KeyListener listener, boolean excludeFromUnhandledGlobalKeyEvents)
	{
		if (listener == null) { throw new NullPointerException("Received null listener."); }
		this.keyListener = listener;
	}

	public void removeKeyListener(KeyListener listener)
	{
		if (this.keyListener == listener) { this.keyListener = null; }
	}

	public boolean addMediaActionHandler(Object handler)
	{
		if (handler == null) { throw new NullPointerException("Null handler received"); }
		if (!mediaActionHandlers.contains(handler))
		{
			mediaActionHandlers.addElement(handler);
			return true;
		}
		return false;
	}

	public void removeMediaActionHandler(Object handler)
	{
		if (handler != null) { mediaActionHandlers.removeElement(handler); }
	}


	public void addTrackwheelListener(TrackwheelListener listener)
	{
		if (listener == null) { throw new NullPointerException("Received null listener."); }
		this.trackwheelListener = listener;
	}

	public void removeTrackwheelListener(TrackwheelListener listener)
	{
		if (this.trackwheelListener == listener) { this.trackwheelListener = null; }
	}

	public void addSystemListener(SystemListener listener)
	{
		if (listener == null) { throw new NullPointerException("Received null listener."); }
		this.systemListener = listener;
	}

	public void removeSystemListener(SystemListener listener)
	{
		if (this.systemListener == listener) { this.systemListener = null; }
	}

	public void addGlobalEventListener(GlobalEventListener listener)
	{
		if (listener == null) { throw new NullPointerException("Received null listener."); }
		this.globalEventListener = listener;
	}

	public void removeGlobalEventListener(GlobalEventListener listener)
	{
		if (this.globalEventListener == listener) { this.globalEventListener = null; }
	}

	public void addHolsterListener(HolsterListener listener)
	{
		if (listener == null) { throw new NullPointerException("Received null listener."); }
		this.holsterListener = listener;
	}

	public void removeHolsterListener(HolsterListener listener)
	{
		if (this.holsterListener == listener) { this.holsterListener = null; }
	}

	public void addRadioListener(RadioListener listener)
	{
		if (listener == null) { throw new NullPointerException("Received null listener."); }
		radioListeners.addElement(listener);
	}

	public void addRadioListener(int WAFFilter, RadioListener listener)
	{
		if (listener == null) { throw new NullPointerException("Received null listener."); }
		radioListeners.addElement(listener);
	}

	public void removeRadioListener(RadioListener listener)
	{
		radioListeners.removeElement(listener);
	}

	public void addIOPortListener(IOPortListener listener)
	{
		if (listener == null) { throw new NullPointerException("Received null listener."); }
		ioPortListeners.addElement(listener);
	}

	public void removeIOPortListener(IOPortListener listener)
	{
		ioPortListeners.removeElement(listener);
	}

	public void addFileSystemListener(FileSystemListener listener)
	{
		if (listener == null) { throw new NullPointerException("Received null listener."); }
		fileSystemListeners.addElement(listener);
	}

	public void removeFileSystemListener(FileSystemListener listener)
	{
		fileSystemListeners.removeElement(listener);
	}

	/*
	public void addFileSystemJournalListener(FileSystemJournalListener listener)
	{
		if (listener == null) { throw new NullPointerException("Received null listener."); }
		fileSystemJournalListeners.addElement(listener);
	}

	public void removeFileSystemJournalListener(FileSystemJournalListener listener)
	{
		fileSystemJournalListeners.removeElement(listener);
	}
	*/

	public void addRealtimeClockListener(RealtimeClockListener listener)
	{
		if (listener == null) { throw new NullPointerException("Received null listener."); }
		realtimeClockListeners.addElement(listener);
	}

	public void removeRealtimeClockListener(RealtimeClockListener listener)
	{
		realtimeClockListeners.removeElement(listener);
	}

	public void addPeripheralListener(PeripheralListener listener)
	{
		if (listener == null) { throw new NullPointerException("Received null listener."); }
		peripheralListeners.addElement(listener);
	}

	public void removePeripheralListener(PeripheralListener listener)
	{
		peripheralListeners.removeElement(listener);
	}

	public void addAlertListener(AlertListener listener)
	{
		if (listener == null) { throw new NullPointerException("Received null listener."); }
		alertListeners.addElement(listener);
	}

	public void removeAlertListener(AlertListener listener)
	{
		alertListeners.removeElement(listener);
	}
}
