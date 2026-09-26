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
package net.rim.device.api.system;

import org.recompile.mobile.Mobile;

public class Sensor
{
	public static final int FLIP = 2;
	public static final int HOLSTER = 1;
	public static final int SLIDE = 4;
	public static final int STATE_FLIP_CLOSED = 1;
	public static final int STATE_FLIP_OPEN = 0;
	public static final int STATE_IN_HOLSTER = 1;
	public static final int STATE_OUT_OF_HOLSTER = 0;
	public static final int STATE_SLIDE_CLOSED = 1;
	public static final int STATE_SLIDE_IN_TRANSITION = 2;
	public static final int STATE_SLIDE_OPEN = 0;

	public static boolean isSupported(int sensorId)
	{
		Mobile.log(Mobile.LOG_WARNING, Sensor.class.getPackage().getName() + "." + Sensor.class.getSimpleName() + ": " + "isSupported not implemented. ID:" + sensorId);
		return false;
	}

	public static int getState(int sensorId)
	{
		Mobile.log(Mobile.LOG_WARNING, Sensor.class.getPackage().getName() + "." + Sensor.class.getSimpleName() + ": " + "getStte not implemented. ID:" + sensorId);
		return 0;
	}

	public static void addListener(Application app, SensorListener listener, int sensors)
	{
		Mobile.log(Mobile.LOG_WARNING, Sensor.class.getPackage().getName() + "." + Sensor.class.getSimpleName() + ": " + "addListener not implemented.");
	}

	public static void removeListener(Application app, SensorListener listener)
	{
		Mobile.log(Mobile.LOG_WARNING, Sensor.class.getPackage().getName() + "." + Sensor.class.getSimpleName() + ": " + "removeListener not implemented.");
	}
}
