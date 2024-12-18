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
package com.motorola.multimedia;

import java.util.TimerTask;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformPlayer;

import com.motorola.funlight.FunLight;

public class Vibrator extends TimerTask implements Runnable
{
    public static final int MAX_VIBRATE_TIME = Integer.MAX_VALUE; // Should be 5 minutes, but we can actually go beyond
    public static final int MIN_PAUSE_TIME = 2000; // 2 seconds
    public static final int VIBRATE_2SHORT = 1;
    public static final int VIBRATE_LONG = 2;
    public static final int VIBRATE_PULSE = 3;
    public static final int VIBRATE_SHORT = 4;
    public static final int VIBRATE_SHORT_LONG = 5;
    public static final int VIBRATE_SILENT = 0;

    private static int currentVibrateTone = VIBRATE_SHORT; // Default tone will be "Short"
    private static Thread vibrationThread;

    @Override
    public void run() 
    { 
        Mobile.log(Mobile.LOG_WARNING, Vibrator.class.getPackage().getName() + "." + Vibrator.class.getSimpleName() + ": " + " run() ");
    }

    public static void setVibrateTone(int tone) 
    {
        if (tone < 0 || tone > 5) { throw new IllegalArgumentException("Invalid tone value"); }
        currentVibrateTone = tone;
    }

    public static void vibrateFor(int timeInMs) 
    {
        if (timeInMs < 0 || timeInMs > MAX_VIBRATE_TIME) 
        {
            throw new IllegalArgumentException("timeInMs must be between 0 and MAX_VIBRATE_TIME");
        }
        setupVibration(timeInMs, timeInMs, 0);
    }

    public static void vibratePeriodically(int timeInMs) 
    {
        if (timeInMs < MIN_PAUSE_TIME || timeInMs > MAX_VIBRATE_TIME) 
        {
            throw new IllegalArgumentException("timeInMs must be between MIN_PAUSE_TIME and MAX_VIBRATE_TIME");
        }
        setupVibration(MAX_VIBRATE_TIME, timeInMs, timeInMs);
    }

    public static void vibratePeriodically(int timeInMs, int timeOffInMs) 
    {
        if (timeInMs < MIN_PAUSE_TIME || timeInMs > MAX_VIBRATE_TIME) 
        {
            throw new IllegalArgumentException("timeInMs must be between MIN_PAUSE_TIME and MAX_VIBRATE_TIME");
        }
        setupVibration(MAX_VIBRATE_TIME, timeInMs, timeOffInMs);
    }

    public static void vibratorOff() 
    {
        if (vibrationThread != null) 
        {
            vibrationThread.interrupt();
            try { vibrationThread.join(); } 
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            vibrationThread = null;
        }

        Mobile.getDisplay().vibrate(0);
    }

    public static void vibratorOn() 
    {
        setupVibration(MAX_VIBRATE_TIME, MAX_VIBRATE_TIME, 0);
    }

    public static void setupVibration(int duration, int vibrateDuration, int pauseDuration) 
    {
        Mobile.log(Mobile.LOG_WARNING, Vibrator.class.getPackage().getName() + "." + Vibrator.class.getSimpleName() + ": " + " Vibration (Untested): " + duration + "| " + vibrateDuration + " |" + pauseDuration);

        try 
        {
            vibratorOff();

            vibrationThread = new Thread(() -> 
            {
                long stopTime = System.currentTimeMillis() + duration;

                while(System.currentTimeMillis() <= stopTime) 
                {
                    if((System.currentTimeMillis() - stopTime + duration) % (vibrateDuration + pauseDuration) < vibrateDuration) 
                    {
                        // If we are in the vibrateDuration time slice, allow vibrations
                        try 
                        {
                            switch (currentVibrateTone) 
                            {
                                case VIBRATE_SHORT: // Short vibration
                                    Mobile.getDisplay().vibrate(150); 
                                    Thread.sleep(MIN_PAUSE_TIME+150);
                                    break;
                                case VIBRATE_LONG: // Long vibration
                                    Mobile.getDisplay().vibrate(1000); 
                                    Thread.sleep(MIN_PAUSE_TIME+1000);
                                    break;
                                case VIBRATE_2SHORT: // Two short vibrations with pause
                                    Mobile.getDisplay().vibrate(150); 
                                    Thread.sleep(650);
                                    Mobile.getDisplay().vibrate(150); 
                                    Thread.sleep(MIN_PAUSE_TIME+150);
                                    break;
                                case VIBRATE_SHORT_LONG:
                                    Mobile.getDisplay().vibrate(150); 
                                    Thread.sleep(650);
                                    Mobile.getDisplay().vibrate(850); 
                                    Thread.sleep(MIN_PAUSE_TIME+850);
                                    break;
                                case VIBRATE_SILENT:
                                    vibratorOff(); // No vibration
                                    break;
                                default:
                                    throw new IllegalArgumentException("Unknown vibration pattern");
                            }
                        }
                        catch (Exception e) { }
                    } 
                    else // Else, stop vibrating
                    {
                        Mobile.getDisplay().vibrate(0);
                    }
                }
            });
            vibrationThread.start();
        } catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, Vibrator.class.getPackage().getName() + "." + Vibrator.class.getSimpleName() + ": " + "Couldn't start vibrator:" + e.getMessage()); }
    }
}