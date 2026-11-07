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
package com.docomostar;

import java.util.Hashtable;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.MobilePlatform;

public abstract class StarApplication
{
	public static final int ACTIVATED_BY_KEY_TOUCHED = -4;
	public static final int ACTIVATED_BY_NATIVE = -3;
	public static final int ACTIVATED_BY_WAKEUP_TIMER = -1;
	public static final int ACTIVATED_BY_WAKEUP_TIMER_DELAYED = -2;
	public static final int ACTIVATED_FROM_STARTED_STATE = 0;

	public static final int LAUNCHED_AFTER_DOWNLOAD = 1;
	public static final int LAUNCHED_AS_MAP_PLATFORM_DIRECTLY = 26;
	public static final int LAUNCHED_BY_BLUETOOTH_REMOTE_DEVICE = 30;
	public static final int LAUNCHED_BY_INVITE_MESSAGE = 25;
	public static final int LAUNCHED_FROM_BML = 21;
	public static final int LAUNCHED_FROM_BROWSER = 5;
	public static final int LAUNCHED_FROM_CONTENTS_PACKAGE = 31;
	public static final int LAUNCHED_FROM_DTV = 17;
	public static final int LAUNCHED_FROM_EXT = 4;
	public static final int LAUNCHED_FROM_FACE = 32;
	public static final int LAUNCHED_FROM_FELICA_ADHOC = 19;
	public static final int LAUNCHED_FROM_FULLAPPLI = 22;
	public static final int LAUNCHED_FROM_ICCARD_USAGE = 33;
	public static final int LAUNCHED_FROM_IMAGE = 29;
	public static final int LAUNCHED_FROM_LAUNCHER = 8;
	public static final int LAUNCHED_FROM_LOCATION_IMAGE = 14;
	public static final int LAUNCHED_FROM_LOCATION_INFO = 13;
	public static final int LAUNCHED_FROM_MAILER = 6;
	public static final int LAUNCHED_FROM_MENU = 0;
	public static final int LAUNCHED_FROM_MENU_FOR_DELETION = 20;
	public static final int LAUNCHED_FROM_MINIAPPLI = 23;
	public static final int LAUNCHED_FROM_PHONEBOOK = 15;
	public static final int LAUNCHED_FROM_SCHEDULER = 28;
	public static final int LAUNCHED_FROM_SELECTED_WORDS = 27;
	public static final int LAUNCHED_FROM_STARAPPLI = 7;
	public static final int LAUNCHED_FROM_TIMER = 2;
	public static final int LAUNCHED_FROM_TORUCA = 18;
	public static final int LAUNCHED_FROM_WIDGETVIEW = 24;
	public static final int LAUNCHED_MSG_RECEIVED = 10;
	public static final int LAUNCHED_MSG_SENT = 11;
	public static final int LAUNCHED_MSG_UNSENT = 12;

	public static final int STAR_FACESTATE_FULL_NORMAL = 1;
	public static final int STAR_FACESTATE_MINI_FOCUSED = 2;
	public static final int STAR_FACESTATE_MINI_SELECTED = 4;
	public static final int STAR_FACESTATE_MINI_UNFOCUSED = 3;

	public static final int STAR_STATE_FULLACTIVE = 1;
	public static final int STAR_STATE_SEMIACTIVE = 2;
	public static final int STAR_STATE_STARTED = 0;

	public static final int STAR_TYPE_FULLAPPLI = 1;
	public static final int STAR_TYPE_MINIAPPLI = 2;

	private static StarApplication instance;
	private static StarApplicationManager manager;
	private StarEventListener listener;
	
	public StarApplication()
	{
		StarApplication.instance = this;
	}

	public abstract void started(int launchType);

	public void activated(int activateInfo)
	{
		
	}

	public void stateChanged(boolean isFullActive)
	{
		
	}

	public final int getAppState()
	{
		return 0;
	}

	public final void suspend()
	{
		MobilePlatform.pauseResumeApp();
	}

	public final void terminate()
	{
		Mobile.log(Mobile.LOG_INFO, StarApplication.class.getPackage().getName() + "." + StarApplication.class.getSimpleName() + ": " + "I-Appli sent termination request");
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) 
        {
            Mobile.log(Mobile.LOG_DEBUG, StarApplication.class.getPackage().getName() + "." + StarApplication.class.getSimpleName() + ": " + element);
        }
        Mobile.getPlatform().drawAppTerminated();
	}

	public final void addEventListener(int starEvent, StarEventListener eventListener)
	{
		this.listener = eventListener;
	}

	public final void addWakeupEvent(int starEvent)
	{
		
	}

	public final void removeWakeupEvent(int starEvent)
	{
		
	}

	public final void clearWakeupEvent()
	{
		
	}

	public final void setWakeupTimer(long duration)
	{
		
	}

	public final long getWakeupTimer()
	{
		return 0L;
	}

	public final int getAppType()
	{
		return 0;
	}

	public final void changeAppType(int type, Hashtable params)
	{
		
	}

	public final int getAppFaceState()
	{
		return 0;
	}

	public static final StarApplication getThisStarApplication()
	{
		return StarApplication.instance;
	}

	public final StarApplicationManager getStarApplicationManager()
	{
		return StarApplication.manager;
	}

	public final void setApplicationManager(StarApplicationManager manager)
	{
		StarApplication.manager = manager;
	}
}
