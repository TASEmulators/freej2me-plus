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
package com.nttdocomo.ui;

import org.recompile.mobile.Mobile;

import javax.microedition.media.Manager;

public class PhoneSystem 
{
    // Area Information Attributes
    public static final int ATTR_AREAINFO_COMMUNICATING = 5;
    public static final int ATTR_AREAINFO_FOMA = 0;
    public static final int ATTR_AREAINFO_HSDPA = 1;
    public static final int ATTR_AREAINFO_OUTSIDE = 2;
    public static final int ATTR_AREAINFO_ROAMINGOUT = 3;
    public static final int ATTR_AREAINFO_SELFMODE = 4;
    public static final int ATTR_AREAINFO_UNKNOWN = 99;

    // Backlight Attributes
    public static final int ATTR_BACKLIGHT_OFF = 0;
    public static final int ATTR_BACKLIGHT_ON = 1;

    // Battery Attributes
    public static final int ATTR_BATTERY_CHARGING = 2;
    public static final int ATTR_BATTERY_FULL = 1;
    public static final int ATTR_BATTERY_PARTIAL = 0;

    // Folding State Attributes
    public static final int ATTR_FOLDING_CLOSE = 0;
    public static final int ATTR_FOLDING_OPEN = 1;

    // Mail Reception Status Attributes
    public static final int ATTR_MAIL_AT_CENTER = 2;
    public static final int ATTR_MAIL_NONE = 0;
    public static final int ATTR_MAIL_RECEIVED = 1;

    // Manner Mode Attributes
    public static final int ATTR_MANNER_OFF = 0;
    public static final int ATTR_MANNER_ON = 1;

    // Message Reception Status Attributes
    public static final int ATTR_MESSAGE_AT_CENTER = 2;
    public static final int ATTR_MESSAGE_NONE = 0;
    public static final int ATTR_MESSAGE_RECEIVED = 1;

    // Screen Visibility Attributes
    public static final int ATTR_SCREEN_INVISIBLE = 0;
    public static final int ATTR_SCREEN_VISIBLE = 1;

    // Service Area Attributes (Deprecated)
    public static final int ATTR_SERVICEAREA_INSIDE = 11; // Deprecated
    public static final int ATTR_SERVICEAREA_OUTSIDE = 12; // Deprecated

    // Surround Sound Attributes
    public static final int ATTR_SURROUND_OFF = 0;
    public static final int ATTR_SURROUND_ON = 1;

    // Vibrator Attributes
    public static final int ATTR_VIBRATOR_OFF = 0;
    public static final int ATTR_VIBRATOR_ON = 1;

    // Device Attributes
    public static final int DEV_BACKLIGHT = 0;
    public static final int DEV_VIBRATOR = 1;
    public static final int DEV_FOLDING = 2;
    public static final int DEV_MAILBOX = 3;
    public static final int DEV_MESSAGEBOX = 4;
    public static final int DEV_BATTERY = 5;
    public static final int DEV_SERVICEAREA = 6;
    public static final int DEV_MANNER = 7;
    public static final int DEV_KEYPAD = 8;
    public static final int DEV_SCREEN_VISIBLE = 9;
    public static final int DEV_AUDIO_SURROUND = 10;
    public static final int DEV_AREAINFO = 11;

    public static final int DEV_VENDOR = 135; // Wai Wai World requests this
    public static final int DEV_VENDOR2 = 142; // Super Bomberman G WIDE requests this

    // Vendor and optional attribute ranges
	public static final int MAX_OPTION_ATTR = 255;
	public static final int MAX_VENDOR_ATTR = 127;
	public static final int MIN_OPTION_ATTR = 128;
	public static final int MIN_VENDOR_ATTR = 64;

    // System sound cues
    public static final int SOUND_INFO = 0;
    public static final int SOUND_WARNING = 1;
    public static final int SOUND_ERROR = 2;
    public static final int SOUND_ALARM = 3;
    public static final int SOUND_CONFIRM = 4;

    // Theme setting types
    public static final int THEME_AV_CALL_IN = 5;
    public static final int THEME_AV_CALLING = 7;
    public static final int THEME_CALL_IN = 2;
    public static final int THEME_CALL_OUT = 1;
    public static final int THEME_CHAT_RECEIVED = 6;
    public static final int THEME_MESSAGE_RECEIVE = 4;
    public static final int THEME_MESSAGE_SEND = 3;
    public static final int THEME_STANDBY = 0;
   

    private static int[] attributes = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    protected PhoneSystem() { }

    public static void setAttribute(int attr, int value) 
    {
        if (!isValidAttribute(attr, value)) { throw new IllegalArgumentException("Invalid attribute or value."); }

        Mobile.log(Mobile.LOG_DEBUG, PhoneSystem.class.getPackage().getName() + "." + PhoneSystem.class.getSimpleName() + ": " + "I-Appli set attr " + attr + " value to:" + value);

        if(attr != DEV_VENDOR && attr != DEV_VENDOR2) { attributes[attr] = value; }
    }

    public static int getAttribute(int attr) 
    {
        if (!isValidAttribute(attr, 0)) { throw new IllegalArgumentException("Invalid attribute to get:" + attr); }

        if(attr == DEV_VENDOR || attr == DEV_VENDOR2) { return 0; }
        return attributes[attr];
    }

    public static void playSound(int type) 
    {
        if (type < SOUND_INFO || type > SOUND_CONFIRM) { throw new IllegalArgumentException("Invalid sound type."); }

        try 
        { 
            DoJaSoundPlayer player = new DoJaSoundPlayer(type);
            Thread soundThread = new Thread(player, "DoJa-PlaySound");
            soundThread.start();
        }
        catch (Exception e) { }
    }

    private static boolean isValidAttribute(int attr, int value) 
    {
        switch (attr) 
        {
            case DEV_BACKLIGHT:
                return value == ATTR_BACKLIGHT_OFF || value == ATTR_BACKLIGHT_ON;
            case DEV_VIBRATOR:
                return value == ATTR_VIBRATOR_OFF || value == ATTR_VIBRATOR_ON;
            case DEV_BATTERY:
                return value == ATTR_BATTERY_CHARGING || value == ATTR_BATTERY_FULL || value == ATTR_BATTERY_PARTIAL;
            case DEV_FOLDING:
                return value == ATTR_FOLDING_CLOSE || value == ATTR_FOLDING_OPEN;
            case DEV_MAILBOX:
                return value == ATTR_MAIL_NONE || value == ATTR_MAIL_RECEIVED || value == ATTR_MAIL_AT_CENTER;
            case DEV_AUDIO_SURROUND:
                return value == ATTR_SURROUND_OFF || value == ATTR_SURROUND_ON;
            case DEV_AREAINFO:
                return value == ATTR_AREAINFO_COMMUNICATING || value == ATTR_AREAINFO_FOMA ||
                    value == ATTR_AREAINFO_HSDPA || value == ATTR_AREAINFO_OUTSIDE ||
                    value == ATTR_AREAINFO_ROAMINGOUT || value == ATTR_AREAINFO_SELFMODE ||
                    value == ATTR_AREAINFO_UNKNOWN;
            case DEV_KEYPAD:
                return true; // TODO
            case DEV_VENDOR:
            case DEV_VENDOR2:
                return true;
        }
        return false;
    }

    public static final boolean isAvailable(final int n) { return true; }
}

class DoJaSoundPlayer implements Runnable 
{
    private final int type;

    public DoJaSoundPlayer(int type) { this.type = type; }

    public final void run()
    {
        try 
        {
            switch (type) 
            {
                case PhoneSystem.SOUND_INFO:
                    Manager.playTone(0, 50, 127);
                    Thread.sleep(50);
                    Manager.playTone(88, 90, 127);
                    Thread.sleep(60);
                    Manager.playTone(90, 90, 127);
                    Thread.sleep(60);
                    Manager.playTone(96, 180, 127);
                    break;
            
                case PhoneSystem.SOUND_WARNING:
                    Manager.playTone(0, 50, 127);
                    Thread.sleep(50);
                    Manager.playTone(86, 100, 127);
                    Thread.sleep(70);
                    Manager.playTone(83, 100, 127);
                    Thread.sleep(70);
                    Manager.playTone(86, 100, 127);
                    Thread.sleep(70);
                    Manager.playTone(84, 100, 127);
                    break;

                case PhoneSystem.SOUND_ERROR:
                    Manager.playTone(0, 50, 127);
                    Thread.sleep(50);
                    Manager.playTone(64, 120, 127);
                    Manager.playTone(72, 150, 127);
                    Thread.sleep(90);
                    Manager.playTone(64, 120, 127);
                    Thread.sleep(60);
                    Manager.playTone(62, 150, 127);
                    Thread.sleep(60);
                    Manager.playTone(59, 150, 127);
                    Thread.sleep(60);
                    Manager.playTone(57, 200, 127);
                    break;

                case PhoneSystem.SOUND_ALARM:
                    Manager.playTone(0, 50, 127);
                    Thread.sleep(50);
                    Manager.playTone(88, 300, 127);
                    Thread.sleep(250);
                    Manager.playTone(82, 300, 127);
                    Thread.sleep(250);
                    Manager.playTone(88, 300, 127);
                    Thread.sleep(250);
                    Manager.playTone(82, 350, 127);
                    break;

                case PhoneSystem.SOUND_CONFIRM:
                    Manager.playTone(0, 50, 127);
                    Thread.sleep(50);
                    Manager.playTone(84, 50, 127);
                    Thread.sleep(50);
                    Manager.playTone(91, 50, 127);
                    break;

                default:
                    break;
            }
        }
        catch (Exception e) { Mobile.log(Mobile.LOG_DEBUG, PhoneSystem.class.getPackage().getName() + "." + PhoneSystem.class.getSimpleName() + ": " + "Failed to play sound " + e.getMessage()); }
    }
}