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

package net.rim.device.api.lcdui;

import javax.microedition.lcdui.CustomItem;
import javax.microedition.media.Controllable;

import net.rim.device.api.ui.TouchEvent;

public abstract class BlackBerryCustomItem extends CustomItem implements Controllable {
	
	public BlackBerryCustomItem(String __a) {
		super(__a);
	}
	
	public void touchEvent(TouchEvent message) {
	}
}
