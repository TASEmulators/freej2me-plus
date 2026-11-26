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

package net.rim.device.api.animation;

import org.recompile.mobile.Mobile;

// stub class
public class AnimationValue {

	public int getChannelCount() {
		Mobile.log(Mobile.LOG_INFO, "net.rim.device.api.animation.AnimationValue.getChannelCount() is a stub!");
		return 0;
	}
	
	public int getPropertyComponentCount() {
		Mobile.log(Mobile.LOG_INFO, "net.rim.device.api.animation.AnimationValue.getPropertyComponentCount() is a stub!");
		return 0;
	}
	
	public int  getInt(int channel,int index) {
		Mobile.log(Mobile.LOG_INFO, "net.rim.device.api.animation.AnimationValue.getInt(int,int) is a stub!");
		return 0;
	}
	
	public int getInt(int index) {
		Mobile.log(Mobile.LOG_INFO, "net.rim.device.api.animation.AnimationValue.getInt(int) is a stub!");
		return 0;
	}
	
	public void setInt(int index,int value) {
		Mobile.log(Mobile.LOG_INFO, "net.rim.device.api.animation.AnimationValue.setInt(int,int) is a stub!");
	}
}
