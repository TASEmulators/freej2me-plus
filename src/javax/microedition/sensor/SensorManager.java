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
package javax.microedition.sensor;

import org.recompile.mobile.Mobile;

public class SensorManager 
{

    public static void addSensorListener(SensorListener listener, SensorInfo info)
    {
        Mobile.log(Mobile.LOG_WARNING, SensorManager.class.getPackage().getName() + "." + SensorManager.class.getSimpleName() + ": " + "addSensorListener(SensorListener, SensorInfo).");
    }

    public static void addSensorListener(SensorListener listener, String quantity) 
    {
        Mobile.log(Mobile.LOG_WARNING, SensorManager.class.getPackage().getName() + "." + SensorManager.class.getSimpleName() + ": " + "addSensorListener(SensorListener, String).");
    }

    public static SensorInfo[] findSensors(String url) 
    { 
        Mobile.log(Mobile.LOG_WARNING, SensorManager.class.getPackage().getName() + "." + SensorManager.class.getSimpleName() + ": " + "findSensors(String).");
        return new SensorInfo[]{};
    }

    public static SensorInfo[] findSensors(String quantity, String contextType) 
    { 
        Mobile.log(Mobile.LOG_WARNING, SensorManager.class.getPackage().getName() + "." + SensorManager.class.getSimpleName() + ": " + "findSensors(String, String).");
        return new SensorInfo[]{};
    }

    public static void removeSensorListener (SensorListener listener) 
    {
        Mobile.log(Mobile.LOG_WARNING, SensorManager.class.getPackage().getName() + "." + SensorManager.class.getSimpleName() + ": " + "removeSensorListener(SensorListener).");
    }
}