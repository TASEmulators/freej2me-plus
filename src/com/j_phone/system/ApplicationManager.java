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
package com.j_phone.system;

import org.recompile.mobile.Mobile;

public class ApplicationManager
{

	private static ApplicationManager instance;

	public static ApplicationManager getInstance()
	{
		if (instance == null) { return instance = new ApplicationManager(); }

		return instance;
	}

	public void setPausedTransitMenu(int n)
	{
		Mobile.log(Mobile.LOG_INFO, ApplicationManager.class.getPackage().getName() + "." + ApplicationManager.class.getSimpleName() + ": " + "Set Paused Transit Menu" + n + ".");
	}

	// Ys - The Oath in Felghana uses this. Apparently just forces an RMS write
	// to disk, which we don't need.
	public void flushRMS()
	{
		Mobile.log(Mobile.LOG_INFO, ApplicationManager.class.getPackage().getName() + "." + ApplicationManager.class.getSimpleName() + ": " + "Flush RMS.");
	}

}
