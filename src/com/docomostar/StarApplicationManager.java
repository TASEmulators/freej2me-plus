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

import java.util.HashMap;

import org.recompile.mobile.Mobile;

public final class StarApplicationManager
{
	public static final int SUSPEND_BY_NATIVE = 1;
	public static final int SUSPEND_BY_STARAPPLI = 2;
	public static final int SUSPEND_PACKETIN = 0x0100;
	public static final int SUSPEND_CALL_OUT = 0x0200;
	public static final int SUSPEND_CALL_IN = 0x0400;
	public static final int SUSPEND_MAIL_SEND = 0x0800;
	public static final int SUSPEND_MAIL_RECEIVE = 0x1000;
	public static final int SUSPEND_MESSAGE_RECEIVE = 0x2000;
	public static final int SUSPEND_SCHEDULE_NOTIFY = 0x4000;
	public static final int SUSPEND_MULTITASK_APPLICATION = 0x8000;

	public static HashMap<String, String> properties;
	public static String[] appParam;
	
	public StarApplicationManager()
	{
		if(properties.containsKey("SPsize")) { Mobile.scratchPadSizes = properties.get("SPsize").split(","); }
        if(properties.containsKey("AppParam")) { appParam = properties.get("AppParam").split("\\s+"); }
	}

	public static void initAppProperties(HashMap<String, String> initProperties)
	{
		properties = initProperties;
	}

	public static String[] getArgs()
	{
		if(appParam != null) { return appParam.clone(); }
        else { return new String[] {""}; }
	}

	public static String getParameter(String key)
	{
		Mobile.log(Mobile.LOG_INFO, StarApplicationManager.class.getPackage().getName() + "." + StarApplicationManager.class.getSimpleName() + ": " + "getParameter:" + key);
		
		return null;
	}

	public static String getSourceURL() { return "dojanetstub://" + properties.get("PackageURL"); }

	public boolean allowAudioOutput()
	{
		return true;
	}

	public void clearMoved()
	{
		
	}

	//public ScheduleDate getLaunchTime(int index)
	//{
	//	return null;
	//}

	public StarEventObject getPushedEvent(int starEvent)
	{
		return null;
	}

	public int getSuspendInfo()
	{
		return 0;
	}

	public boolean isAllowedAudioOutput()
	{
		return true;
	}

	public boolean isMoved()
	{
		return false;
	}

	public boolean isMovedFromOtherTerminal()
	{
		return false;
	}

	//public void setLaunchTime(int index, ScheduleDate date)
	//{
	//
	//}

	public void upgrade()
	{
		
	}

	//public void addRemoteLauncher(RemoteDevice launcher)
	//{
	//	
	//}

	public String[] getRemoteLauncherList()
	{
		return null;
	}

	//public boolean removeRemoteLauncher(RemoteDevice launcher)
	//{
	//	return false;
	//}
}
