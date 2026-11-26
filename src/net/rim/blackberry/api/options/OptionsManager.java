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

package net.rim.blackberry.api.options;

import org.recompile.mobile.Mobile;

//stub class
public class OptionsManager {

	public static void registerOptionsProvider(OptionsProvider optionsProvider) {
		Mobile.log(Mobile.LOG_INFO, "net.rim.blackberry.api.options.OptionsManager.registerOptionsProvider(OptionsProvider) is a stub!");
	}
	
	public static void registerOptionsProvider(ExtendedOptionsProvider p) {
		Mobile.log(Mobile.LOG_INFO, "net.rim.blackberry.api.options.OptionsManager.registerOptionsProvider(ExtendedOptionsProvider) is a stub!");
	}
}
