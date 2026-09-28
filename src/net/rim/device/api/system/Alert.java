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

public final class Alert
{
	public static final int ALERT_ERROR_BAD_DATA = 2;
	public static final int ALERT_ERROR_BAD_STATE = 3;
	public static final int ALERT_ERROR_FILESYSTEM_FULL = 4;
	public static final int ALERT_ERROR_UNKNOWN = 1;
	public static final int ALERT_OK = 0;

	public static void enablePWMSync(boolean enable)
	{
		Mobile.log(Mobile.LOG_WARNING, Alert.class.getPackage().getName() + "." + Alert.class.getSimpleName() + ": " + "enablePWMSync not implemented.");
	}

	public static int getVolume() { return 100; }

	public static boolean isADPCMSupported() { return true; }

	public static boolean isAudioSupported() { return true; }

	public static boolean isBuzzerSupported() { return true; }

	public static boolean isMIDISupported() { return true; }

	public static boolean isVibrateSupported() { return true; }

	public static void mute(boolean newMuteState)
	{
		Mobile.log(Mobile.LOG_WARNING, Alert.class.getPackage().getName() + "." + Alert.class.getSimpleName() + ": " + "mute not implemented.");
	}

	public static void playBuzzer(short[] tune, int volume)
	{
		Mobile.log(Mobile.LOG_WARNING, Alert.class.getPackage().getName() + "." + Alert.class.getSimpleName() + ": " + "playBuzzer not implemented.");
	}

	@Deprecated
	public static void setADPCMVolume(int volume)
	{
		Mobile.log(Mobile.LOG_WARNING, Alert.class.getPackage().getName() + "." + Alert.class.getSimpleName() + ": " + "setADPCMVolume not implemented.");
	}

	public static void setBuzzerVolume(int volume)
	{
		Mobile.log(Mobile.LOG_WARNING, Alert.class.getPackage().getName() + "." + Alert.class.getSimpleName() + ": " + "setBuzzerVolume not implemented.");
	}

	public static void setVolume(int volume)
	{
		Mobile.log(Mobile.LOG_WARNING, Alert.class.getPackage().getName() + "." + Alert.class.getSimpleName() + ": " + "setVolume not implemented.");
	}

	@Deprecated
	public static int startADPCM(byte[] tune, boolean interruptable)
	{
		Mobile.log(Mobile.LOG_WARNING, Alert.class.getPackage().getName() + "." + Alert.class.getSimpleName() + ": " + "startADPCM not implemented.");
		return ALERT_OK;
	}

	public static void startAudio(short[] tune, int volume)
	{
		Mobile.log(Mobile.LOG_WARNING, Alert.class.getPackage().getName() + "." + Alert.class.getSimpleName() + ": " + "startAudio not implemented.");
	}

	public static void startBuzzer(short[] tune, int volume)
	{
		Mobile.log(Mobile.LOG_WARNING, Alert.class.getPackage().getName() + "." + Alert.class.getSimpleName() + ": " + "startBuzzer A not implemented.");
	}

	public static void startBuzzer(short[] tune, int volume, boolean interruptable)
	{
		Mobile.log(Mobile.LOG_WARNING, Alert.class.getPackage().getName() + "." + Alert.class.getSimpleName() + ": " + "startBuzzer B not implemented.");
	}

	public static int startMIDI(byte[] tune, boolean interruptable)
	{
		Mobile.log(Mobile.LOG_WARNING, Alert.class.getPackage().getName() + "." + Alert.class.getSimpleName() + ": " + "startMIDI not implemented.");
		return ALERT_OK;
	}

	public static void startVibrate(int duration)
	{
		Mobile.vibrationDuration = duration;
	}

	public static void stopADPCM()
	{
		Mobile.log(Mobile.LOG_WARNING, Alert.class.getPackage().getName() + "." + Alert.class.getSimpleName() + ": " + "stopADPCM not implemented.");
	}

	public static void stopAudio()
	{
		Mobile.log(Mobile.LOG_WARNING, Alert.class.getPackage().getName() + "." + Alert.class.getSimpleName() + ": " + "stopAudio not implemented.");
	}

	public static void stopBuzzer()
	{
		Mobile.log(Mobile.LOG_WARNING, Alert.class.getPackage().getName() + "." + Alert.class.getSimpleName() + ": " + "stopBuzzer not implemented.");
	}

	public static void stopMIDI()
	{
		Mobile.log(Mobile.LOG_WARNING, Alert.class.getPackage().getName() + "." + Alert.class.getSimpleName() + ": " + "stopMIDI not implemented.");
	}

	public static void stopVibrate()
	{
		Mobile.vibrationDuration = 0;
	}
}
