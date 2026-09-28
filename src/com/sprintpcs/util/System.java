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
package com.sprintpcs.util;

import org.recompile.mobile.Mobile;

public class System
{
	public static String getSystemState(String s)
	{ 
		Mobile.log(Mobile.LOG_WARNING, System.class.getPackage().getName() + "." + System.class.getSimpleName() + ": " + "getSystemStatenot implemented: " + s);
		return "";
	}

	public static void setExitURI(String s)
	{ 
		Mobile.log(Mobile.LOG_WARNING, System.class.getPackage().getName() + "." + System.class.getSimpleName() + ": " + "setExitURI not implemented: " + s);
		return;
	}

	public static void addSystemListener(SystemEventListener listener)
	{ 
		Mobile.log(Mobile.LOG_WARNING, System.class.getPackage().getName() + "." + System.class.getSimpleName() + ": " + "addSystemListener not implemented. ");
	}

	public static String[] getPropertiesList()
	{
		Mobile.log(Mobile.LOG_WARNING, System.class.getPackage().getName() + "." + System.class.getSimpleName() + ": " + "getPropertiesList not implemented. ");
		return null;
	}

	public static void setSystemSetting(String property, String value)
	{ 
		Mobile.log(Mobile.LOG_WARNING, System.class.getPackage().getName() + "." + System.class.getSimpleName() + ": " + "setSystemSetting not implemented: " + property + " " + value);
	}

	public static String getProtectedProperty(String property)
	{ 
		Mobile.log(Mobile.LOG_WARNING, System.class.getPackage().getName() + "." + System.class.getSimpleName() + ": " + "getProtectedProperty not implemented: " + property);
		return "";
	}

	public static void promptMasterVolume()
	{ 
		Mobile.log(Mobile.LOG_WARNING, System.class.getPackage().getName() + "." + System.class.getSimpleName() + ": " + "promptMasterVolume not implemented. ");
	}

	public static void getTactileFeedback()
	{
		// This method seems to be just for vibration feedback on touch
		com.sprintpcs.media.Vibrator.vibrate(100);
	}
}
