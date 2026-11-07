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

import java.util.Arrays;
import java.util.HashMap;

import org.recompile.mobile.MobilePlatform;
import org.recompile.mobile.Mobile;

public abstract class IApplication
{

	public static final int LAUNCH_AS_LAUNCHER = 4;
	public static final int LAUNCH_BROWSER = 1;
	public static final int LAUNCH_BROWSER_SUSPEND = 13;
	public static final int LAUNCH_DTV = 12;
	public static final int LAUNCH_IAPPLI = 3;
	public static final int LAUNCH_MAIL_LAST_INCOMING = 10;
	public static final int LAUNCH_MAIL_RECEIVED = 7;
	public static final int LAUNCH_MAIL_SENT = 8;
	public static final int LAUNCH_MAIL_UNSENT = 9;
	public static final int LAUNCH_MAILMENU = 5;
	public static final int LAUNCH_SCHEDULER = 6;
	public static final int LAUNCH_VERSIONUP = 2;
	
	public static final int LAUNCHED_AFTER_DOWNLOAD = 1;
	public static final int LAUNCHED_AS_CONCIERGE = 3;
	public static final int LAUNCHED_AS_ILET = 9;
	public static final int LAUNCHED_FROM_BML = 21;
	public static final int LAUNCHED_FROM_BROWSER = 5;
	public static final int LAUNCHED_FROM_DTV = 17;
	public static final int LAUNCHED_FROM_EXT = 4;
	public static final int LAUNCHED_FROM_FELICA_ADHOC = Integer.MIN_VALUE; // This one doesn't have a specific value
	public static final int LAUNCHED_FROM_IAPPLI = 7;
	public static final int LAUNCHED_FROM_LAUNCHER = 8;
	public static final int LAUNCHED_FROM_LOCATION_IMAGE = 14;
	public static final int LAUNCHED_FROM_LOCATION_INFO = 13;
	public static final int LAUNCHED_FROM_MAILER = 6;
	public static final int LAUNCHED_FROM_MENU = 0;
	public static final int LAUNCHED_FROM_MENU_FOR_DELETION = 20;
	public static final int LAUNCHED_FROM_PHONEBOOK = 15;
	public static final int LAUNCHED_FROM_TIMER = 2;
	public static final int LAUNCHED_FROM_TORUCA = 18;
	public static final int LAUNCHED_MSG_RECEIVED = 10;
	public static final int LAUNCHED_MSG_SENT = 11;
	public static final int LAUNCHED_MSG_UNSENT = 12;

	private static IApplication instance;
	public static HashMap<String, String> properties;
	public static String[] appParam;

	public static Display display = new Display();
	
	protected IApplication()
	{
		Mobile.log(Mobile.LOG_INFO, IApplication.class.getPackage().getName() + "." + IApplication.class.getSimpleName() + ": " + "Create DoJa IApplication");
		IApplication.instance = this;
		if(properties.containsKey("SPsize")) { Mobile.scratchPadSizes = properties.get("SPsize").split(","); }
		if(properties.containsKey("AppParam")) { appParam = properties.get("AppParam").split("\\s+"); }

		Mobile.log(Mobile.LOG_INFO, IApplication.class.getPackage().getName() + "." + IApplication.class.getSimpleName() + ": " + "arguments:" + Arrays.toString(appParam));
	}

	public final String[] getArgs()  
	{ 
		if(appParam != null) { return appParam.clone(); }
		else { return new String[] {""}; }
	}

	public int getLaunchType() { return LAUNCHED_FROM_MENU; }

	public static void initAppProperties(HashMap<String, String> initProperties)
	{
		properties = initProperties;
	}

	public String getSourceURL() { return "dojanetstub://" + properties.get("PackageURL"); }

	public static final IApplication getCurrentApp() { return IApplication.instance; }

	public String getParameter(String name)
	{
		Mobile.log(Mobile.LOG_INFO, IApplication.class.getPackage().getName() + "." + IApplication.class.getSimpleName() + ": " + "getParameter:" + name);
		
		return null;
	}

	public abstract void start();

	public void resume()
	{
		if(Mobile.isHidden) { MobilePlatform.hideRestoreApp(); }
	}

	public final void terminate() 
	{ 
		Mobile.log(Mobile.LOG_INFO, IApplication.class.getPackage().getName() + "." + IApplication.class.getSimpleName() + ": " + "I-Appli sent termination request");
		for (StackTraceElement element : Thread.currentThread().getStackTrace()) 
		{
			Mobile.log(Mobile.LOG_DEBUG, IApplication.class.getPackage().getName() + "." + IApplication.class.getSimpleName() + ": " + element);
		}
		Mobile.getPlatform().drawAppTerminated();
	}

	public final void launch(int target, String[] arguments) 
	{ 
		Mobile.log(Mobile.LOG_WARNING, IApplication.class.getPackage().getName() + "." + IApplication.class.getSimpleName() + ": " + "launch(int, String[]) not implemented");
	}
}
