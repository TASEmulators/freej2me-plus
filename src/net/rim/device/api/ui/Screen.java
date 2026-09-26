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

import org.recompile.mobile.Mobile;

// TODO: Implement this later. It's a massive class and i don't think it's
// worth it right now.
public abstract class Screen
{
	public static final long DEFAULT_CLOSE = 131072L;
	public static final long DEFAULT_MENU = 65536L;
	public static final long NO_SYSTEM_MENU_ITEMS = 262144L;

	// Actual constructor and methods.
	public void setTrackballSensitivityXOffset(int trackballSensitivityXOffset)
	{
		Mobile.log(Mobile.LOG_WARNING, Screen.class.getPackage().getName() + "." + Screen.class.getSimpleName() + ": " + "setTrackballSensitivityXOffset not implemented. Val:" + trackballSensitivityXOffset);
	}

	public void setTrackballSensitivityYOffset(int trackballSensitivityYOffset)
	{
		Mobile.log(Mobile.LOG_WARNING, Screen.class.getPackage().getName() + "." + Screen.class.getSimpleName() + ": " + "setTrackballSensitivityYOffset not implemented. Val:" + trackballSensitivityYOffset);
	}
}
