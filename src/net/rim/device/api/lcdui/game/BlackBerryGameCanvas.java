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

package net.rim.device.api.lcdui.game;

import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.media.Controllable;
import javax.microedition.media.Control;
import net.rim.device.api.lcdui.control.DirectionControl;
import net.rim.device.api.lcdui.control.MediaBehaviourControl;
import net.rim.device.api.lcdui.control.VirtualKeyboardControl;
import net.rim.device.api.ui.TouchEvent;
import org.recompile.mobile.Mobile;

public abstract class BlackBerryGameCanvas extends GameCanvas implements Controllable
{
	protected Control[] controls;
	
	public BlackBerryGameCanvas(boolean suppressKeyEvents)
	{ 
		super(suppressKeyEvents);
		controls = new Control[3];
		controls[0] = new directionControl();
		controls[1] = new mediaBehaviourControl();
		controls[2] = new virtualKeyboardControl();
	}

	public Control getControl(String controlType)
	{
		if(controlType.contains("DirectionControl"))       { return this.controls[0]; }
		if(controlType.contains("MediaBehaviourControl"))  { return this.controls[1]; }
		if(controlType.contains("VirtualKeyboardControl")) { return this.controls[2]; }

		return null;
	}

	public Control[] getControls()
	{
		return this.controls;
	}

	public void touchEvent(TouchEvent message)
	{
		// TODO
	}

	public class directionControl implements DirectionControl
	{
		private int directions = 0;
		
		public int getOrientation()
		{
			if(Mobile.getPlatform().lcdWidth == Mobile.getPlatform().lcdHeight) { return DirectionControl.ORIENTATION_SQUARE; }
			else if(Mobile.getPlatform().lcdWidth > Mobile.getPlatform().lcdHeight) { return DirectionControl.ORIENTATION_LANDSCAPE; }

			return DirectionControl.ORIENTATION_PORTRAIT;
		}
    
		public void setAcceptableScreenDirections(int directions) { this.directions = directions; }
	}

	public class mediaBehaviourControl implements MediaBehaviourControl
	{
		private boolean mediaEnabled = Mobile.midlet.getAppProperty("RIM-MIDlet-MediaPlayerModeEnabled") != null;
		
		public void setMediaPlayerModeEnabled(boolean enabled) { this.mediaEnabled = enabled; }
		
		public boolean isMediaPlayerModeEnabled() { return this.mediaEnabled; }
	}

	public class virtualKeyboardControl implements VirtualKeyboardControl
	{
		private int kbVisibility = VirtualKeyboardControl.KEYBOARD_HIDE;
		
		public void setKeyboardVisibility(int visibility) { this.kbVisibility = visibility; }
		
		public int getKeyboardVisibility() { return this.kbVisibility; }
	}
}
