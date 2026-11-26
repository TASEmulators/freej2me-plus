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

public class DeviceInfo {
	
	public static final int BSTAT_DEAD = -2147483648;
	public static final int BSTAT_CHARGING = 1;
	public static final int BSTAT_AC_CONTACTS = 16;
	public static final int BSTAT_IS_USING_EXTERNAL_POWER = 4;
	public static final int BSTAT_LEVEL_CHANGED = 2;
	public static final int BSTAT_LOW = 268435456;
	public static final int BSTAT_LOW_RATE_CHARGING = 8;
	public static final int BSTAT_NONE 	= 8388608;
	public static final int BSTAT_NO_CAMERA_FLASH = 8192;
	public static final int BSTAT_NO_RADIO  = 16384;
	public static final int BSTAT_NO_TURN_ON = 32768;
	public static final int BSTAT_NO_WLAN = 4096;
	public static final int BSTAT_REVERSED = 4194304;
	public static final int BSTAT_TOO_COLD = 1073741824;
	public static final int BSTAT_TOO_HOT = 536870912;
	public static final int BSTAT_UNKNOWN_BATTERY = 2097152;
	public static final int INVALID_DEVICE_ID = -1;
	
	public static String getDeviceName() {
		return "9360";
	}
	
	public static boolean isSimulator() {
		return false;
	}
	
	public static String getSoftwareVersion() {
		return "7.1.0.342";
		
	}
	
	public static int getOSVersion() {
		return 33557790;
	}
	
	public static String getPlatformVersion() {
		return "5.0.0.419";
	}
	
	public static int getBatteryLevel() {
		return 100;
	}
	
	public static int getDeviceId() {
		return 0x2100000a;
	}
	
	public static boolean isBatteryRemovable() {
		return false;
	}
	
	public static boolean isInHolster() {
		return false;
	}
	
	public static int getBatteryStatus() {
		return BSTAT_CHARGING;
	}
	
	public static int getTotalFlashSize() {
		return 2000000000;
	}
	
	public static long getTotalFlashSizeEx() {
		return 3000000000l;
	}
	
	public static boolean isPasswordEnabled() {
		return false;
	}
	
	public static String getManufacturerName() {
		return "Research In Motion";
	}
	
	public static boolean hasCamera() {
		return false;
	}
	
	public static int getBatteryVoltage() {
		return 5;
	}
	
	public static int getBatteryTemperature() {
		return 10;
	}
	
	public static int getLockTimeout() {
		return 10000;
	}
	
	//Idle Time stubs
	public static boolean canResetIdleTime() {
		return true;
	}
	
	public static long getIdleTime() {
		return 1;
	}
	
}