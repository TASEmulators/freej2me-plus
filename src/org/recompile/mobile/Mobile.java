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
package org.recompile.mobile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Canvas;
import javax.microedition.m3g.Graphics3D;

import javax.microedition.midlet.MIDlet;

/*

	Mobile

	Provides MobilePlatform access to mobile app

*/

public class Mobile
{
	private static MobilePlatform platform;

	private static Display display;

	private static Graphics3D graphics3d;

	// Enable/disable logging to the console and optionally to a file
	public static boolean logging = true; 
	private static final String LOG_FILE = "freej2me_system" + File.separatorChar + "FreeJ2ME.log";
	public static byte minLogLevel = 1;

	//Log Levels
	public static final byte LOG_DEBUG = 0;
    public static final byte LOG_INFO = 1;
    public static final byte LOG_WARNING = 2;
    public static final byte LOG_ERROR = 3;
    public static final byte LOG_FATAL = 4;

	// Keycode modifiers
	public static boolean lg = false;
	public static boolean motorola = false;
	public static boolean motoV8 = false;
	public static boolean motoTriplets = false;
	public static boolean nokia = false;
	public static boolean nokiaKeyboard = false;
	public static boolean sagem = false;
	public static boolean siemens = false;

	/* 
	 * For AWTGUI, the input array is as follows: [LeftSoft, RightSoft, Up, Left, Fire, Right, Down, 1, 2, 3, 4, 5, 6, 7, 8, 9, *, 0, #] (5 and Fire are made the same)
	 * While on Libretro, it's:                   [Up, Down, Left, Right, 9, 7, 0, Fire, RightSoft, LeftSoft, 1, 3. *. #, 2, 4, 6, 8] (5 isn't even considered, as the core already abstracts it)
	 * Anbu still isn't considered here, but it should match libretro at some point.
	 */
	private static final int[] awtguiKeycodes   = {9, 8, 0, 2, 7, 3, 1, 10, 14, 11, 15, 7, 16, 5, 17, 4, 12, 6, 13};
	//private static final int[] libretroKeycodes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17}; // 5 and Fire are also the same, and this array doesn't have to be used.

	private static final String[] keyArray = {"Up", "Down", "Left", "Right", "9", "7", "0", "Fire/5", "RightSoft", "LeftSoft", "1", "3", "*", "#", "2", "4", "6", "8"};

	// Set whether audio should be enabled or not. Can work around jars that crash FreeJ2ME due to audio
	public static boolean sound = true;

	// Var to track any changes to current Displayable, otherwise the SDL interface won't render new frames
	public static boolean displayUpdated;

	// Vibration support for Libretro and SDL
	public static int vibrationDuration = 0;

	// Support for explicit FPS limit on jars that require it to work properly
	public static int limitFPS = 0;

	//MIDP Canvas keycodes (A.K.A the standard set provided by MIDP)
	public static final int KEY_NUM0  = Canvas.KEY_NUM0;  // 48
	public static final int KEY_NUM1  = Canvas.KEY_NUM1;  // 49
	public static final int KEY_NUM2  = Canvas.KEY_NUM2;  // 50
	public static final int KEY_NUM3  = Canvas.KEY_NUM3;  // 51
	public static final int KEY_NUM4  = Canvas.KEY_NUM4;  // 52
	public static final int KEY_NUM5  = Canvas.KEY_NUM5;  // 53
	public static final int KEY_NUM6  = Canvas.KEY_NUM6;  // 54
	public static final int KEY_NUM7  = Canvas.KEY_NUM7;  // 55
	public static final int KEY_NUM8  = Canvas.KEY_NUM8;  // 56
	public static final int KEY_NUM9  = Canvas.KEY_NUM9;  // 57
	public static final int KEY_STAR  = Canvas.KEY_STAR;  // 42
	public static final int KEY_POUND = Canvas.KEY_POUND; // 35
	public static final int GAME_UP   = Canvas.UP;     // 1
	public static final int GAME_DOWN = Canvas.DOWN;   // 6
	public static final int GAME_LEFT = Canvas.LEFT;   // 2 
	public static final int GAME_RIGHT= Canvas.RIGHT;  // 5 
	public static final int GAME_FIRE = Canvas.FIRE;   // 8
	public static final int GAME_A    = Canvas.GAME_A; // 9
	public static final int GAME_B    = Canvas.GAME_B; // 10
	public static final int GAME_C    = Canvas.GAME_C; // 11
	public static final int GAME_D    = Canvas.GAME_D; // 12

	//Nokia keycodes
	public static final int NOKIA_UP    = -1; // KEY_UP_ARROW = -1;
	public static final int NOKIA_DOWN  = -2; // KEY_DOWN_ARROW = -2;
	public static final int NOKIA_LEFT  = -3; // KEY_LEFT_ARROW = -3;
	public static final int NOKIA_RIGHT = -4; // KEY_RIGHT_ARROW = -4;
	public static final int NOKIA_SOFT1 = -6; // KEY_SOFTKEY1 = -6; (Left Soft)
	public static final int NOKIA_SOFT2 = -7; // KEY_SOFTKEY2 = -7; (Right Soft)
	public static final int NOKIA_SOFT3 = -5; // KEY_SOFTKEY3 = -5; (Fire)
	public static final int NOKIA_END   = -11; // KEY_END = -11;
	public static final int NOKIA_SEND  = -10; // KEY_SEND = -10;

	//Nokia keyboard keycodes
	public static final int NOKIAKB_UP    = -1; // KEY_UP_ARROW = -1;
	public static final int NOKIAKB_DOWN  = -2; // KEY_DOWN_ARROW = -2;
	public static final int NOKIAKB_LEFT  = -3; // KEY_LEFT_ARROW = -3;
	public static final int NOKIAKB_RIGHT = -4; // KEY_RIGHT_ARROW = -4;
	public static final int NOKIAKB_SOFT1 = -6; // KEY_SOFTKEY1 = -6; (Left Soft)
	public static final int NOKIAKB_SOFT2 = -7; // KEY_SOFTKEY2 = -7; (Right Soft)
	public static final int NOKIAKB_SOFT3 = -5; // KEY_SOFTKEY3 = -5; (Fire)
	public static final int NOKIAKB_NUM0  = 109;
	public static final int NOKIAKB_NUM1  = 114;
	public static final int NOKIAKB_NUM2  = 116;
	public static final int NOKIAKB_NUM3  = 121;
	public static final int NOKIAKB_NUM4  = 102;
	public static final int NOKIAKB_NUM5  = 103;
	public static final int NOKIAKB_NUM6  = 104;
	public static final int NOKIAKB_NUM7  = 118;
	public static final int NOKIAKB_NUM8  = 98;
	public static final int NOKIAKB_NUM9  = 110;
	public static final int NOKIAKB_STAR  = 117;
	public static final int NOKIAKB_POUND = 106;

	//Siemens keycodes
	public static final int SIEMENS_UP    = -59;
	public static final int SIEMENS_DOWN  = -60;
	public static final int SIEMENS_LEFT  = -61;
	public static final int SIEMENS_RIGHT = -62;
	public static final int SIEMENS_SOFT1 = -1; 
	public static final int SIEMENS_SOFT2 = -4; 
	public static final int SIEMENS_FIRE = -26; 

	//Motorola E1000/Alcatel/Softbank keycodes
	public static final int MOTOROLA_UP    = -1;
	public static final int MOTOROLA_DOWN  = -6;
	public static final int MOTOROLA_LEFT  = -2;
	public static final int MOTOROLA_RIGHT = -5;
	public static final int MOTOROLA_SOFT1 = -21; 
	public static final int MOTOROLA_SOFT2 = -22; 
	public static final int MOTOROLA_FIRE = -20;

	//Motorola V8 keycodes
	public static final int MOTOV8_UP    = -1;
	public static final int MOTOV8_DOWN  = -2;
	public static final int MOTOV8_LEFT  = -3;
	public static final int MOTOV8_RIGHT = -4;
	public static final int MOTOV8_SOFT1 = -21;
	public static final int MOTOV8_SOFT2 = -22;
	public static final int MOTOV8_FIRE = -5;

	//Motorola Triplets keycodes
	public static final int TRIPLETS_UP    = 1;
	public static final int TRIPLETS_DOWN  = 6;
	public static final int TRIPLETS_LEFT  = 2;
	public static final int TRIPLETS_RIGHT = 5;
	public static final int TRIPLETS_SOFT1 = 21; 
	public static final int TRIPLETS_SOFT2 = 22; 
	public static final int TRIPLETS_FIRE = 20;

	//LG keycodes
	public static final int LG_UP    = -1;
	public static final int LG_DOWN  = -2;
	public static final int LG_LEFT  = -3;
	public static final int LG_RIGHT = -4;
	public static final int LG_SOFT1 = -202; 
	public static final int LG_SOFT2 = -203; 
	public static final int LG_FIRE = -5;

	//Sagem keycodes (just nokia with inverted softkeys)
	public static final int SAGEM_UP    = -1; // KEY_UP_ARROW = -1;
	public static final int SAGEM_DOWN  = -2; // KEY_DOWN_ARROW = -2;
	public static final int SAGEM_LEFT  = -3; // KEY_LEFT_ARROW = -3;
	public static final int SAGEM_RIGHT = -4; // KEY_RIGHT_ARROW = -4;
	public static final int SAGEM_SOFT1 = -7; // KEY_SOFTKEY1 = -7; (Left Soft)
	public static final int SAGEM_SOFT2 = -6; // KEY_SOFTKEY2 = -6; (Right Soft)
	public static final int SAGEM_SOFT3 = -5; // KEY_SOFTKEY3 = -5; (Fire)

	public static MobilePlatform getPlatform() { return platform; }

	public static void setPlatform(MobilePlatform p) { platform = p; }

	public static Display getDisplay() { return display; }

	public static void setDisplay(Display d) { display = d; }

	public static Graphics3D getGraphics3D() { return graphics3d; }

	public static void setGraphics3D(Graphics3D g) { graphics3d = g; }

	public static InputStream getResourceAsStream(Class c, String resource)
	{
		return platform.loader.getMIDletResourceAsStream(resource);
	}

	public static InputStream getMIDletResourceAsStream(String resource)
	{
		return platform.loader.getMIDletResourceAsStream(resource);
	}

	public static final int getMobileKey(int keycode, boolean isLibretro) 
	{
		if(!isLibretro) { keycode = awtguiKeycodes[keycode]; } // Cast the received awt key to the correct value.

		log(Mobile.LOG_DEBUG, Mobile.class.getPackage().getName() + "." + Mobile.class.getSimpleName() + ": " + "KeyPress:" + keyArray[keycode]);

		// These keys are overridden by the modifier variables (comments simulate the Libretro interface with a NS Pro Controller)
		if(lg)
		{
			switch(keycode)
			{
				case 0: return LG_UP; // Up
				case 1: return LG_DOWN; // Down
				case 2: return LG_LEFT; // Left
				case 3: return LG_RIGHT; // Right
				case 7: return LG_FIRE; // Y
				case 8: return LG_SOFT2; // Start
				case 9: return LG_SOFT1; // Select
			}
		}
		if(motorola)
		{
			switch(keycode)
			{
				case 0: return MOTOROLA_UP; // Up
				case 1: return MOTOROLA_DOWN; // Down
				case 2: return MOTOROLA_LEFT; // Left
				case 3: return MOTOROLA_RIGHT; // Right
				case 7: return MOTOROLA_FIRE; // Y
				case 8: return MOTOROLA_SOFT2; // Start
				case 9: return MOTOROLA_SOFT1; // Select
			}
		}
		if(motoTriplets)
		{
			switch(keycode)
			{
				case 0: return TRIPLETS_UP; // Up
				case 1: return TRIPLETS_DOWN; // Down
				case 2: return TRIPLETS_LEFT; // Left
				case 3: return TRIPLETS_RIGHT; // Right
				case 7: return TRIPLETS_FIRE; // Y
				case 8: return TRIPLETS_SOFT2; // Start
				case 9: return TRIPLETS_SOFT1; // Select
			}
		}
		if(motoV8)
		{
			switch(keycode)
			{
				case 0: return MOTOV8_UP; // Up
				case 1: return MOTOV8_DOWN; // Down
				case 2: return MOTOV8_LEFT; // Left
				case 3: return MOTOV8_RIGHT; // Right
				case 7: return MOTOV8_FIRE; // Y
				case 8: return MOTOV8_SOFT2; // Start
				case 9: return MOTOV8_SOFT1; // Select
			}
		}
		if(nokia)
		{
			switch(keycode)
			{
				case 0: return NOKIA_UP; // Up
				case 1: return NOKIA_DOWN; // Down
				case 2: return NOKIA_LEFT; // Left
				case 3: return NOKIA_RIGHT; // Right
				case 7: return NOKIA_SOFT3; // Y
				case 8: return NOKIA_SOFT2; // Start
				case 9: return NOKIA_SOFT1; // Select
			}
		}
		if(nokiaKeyboard)
		{
			switch(keycode)
			{
				case 0: return NOKIAKB_UP; // Up
				case 1: return NOKIAKB_DOWN; // Down
				case 2: return NOKIAKB_LEFT; // Left
				case 3: return NOKIAKB_RIGHT; // Right
				case 4: return NOKIAKB_NUM9; // A
				case 5: return NOKIAKB_NUM7; // B
				case 6: return NOKIAKB_NUM0; // X
				case 7: return NOKIAKB_SOFT3; // Y
				case 8: return NOKIAKB_SOFT2; // Start
				case 9: return NOKIAKB_SOFT1; // Select
				case 10: return NOKIAKB_NUM1; // L
				case 11: return NOKIAKB_NUM3; // R
				case 12: return NOKIAKB_STAR; // L2
				case 13: return NOKIAKB_POUND; // R2
				case 14: return NOKIAKB_NUM2; // Up 
				case 15: return NOKIAKB_NUM4; // Left
				case 16: return NOKIAKB_NUM6; // Right
				case 17: return NOKIAKB_NUM8; // Down
			}
		}
		if(sagem)
		{
			switch(keycode)
			{
				case 0: return SAGEM_UP; // Up
				case 1: return SAGEM_DOWN; // Down
				case 2: return SAGEM_LEFT; // Left
				case 3: return SAGEM_RIGHT; // Right
				case 7: return SAGEM_SOFT3; // Y
				case 8: return SAGEM_SOFT2; // Start
				case 9: return SAGEM_SOFT1; // Select
			}
		}
		if(siemens)
		{
			switch(keycode)
			{
				case 0: return SIEMENS_UP; // Up
				case 1: return SIEMENS_DOWN; // Down
				case 2: return SIEMENS_LEFT; // Left
				case 3: return SIEMENS_RIGHT; // Right
				case 7: return SIEMENS_FIRE; // Y
				case 8: return SIEMENS_SOFT2; // Start
				case 9: return SIEMENS_SOFT1; // Select
			}
		}
		
		// J2ME Canvas standard keycodes, to match against any keys not covered above.
		switch(keycode)
		{
			case 0: return KEY_NUM2; // Up
			case 1: return KEY_NUM8; // Down
			case 2: return KEY_NUM4; // Left
			case 3: return KEY_NUM6; // Right
			case 4: return KEY_NUM9; // A
			case 5: return KEY_NUM7; // B
			case 6: return KEY_NUM0; // X
			case 7: return KEY_NUM5; // Y
			case 8: return NOKIA_SOFT2; // Start
			case 9: return NOKIA_SOFT1; // Select
			case 10: return KEY_NUM1; // L
			case 11: return KEY_NUM3; // R
			case 12: return KEY_STAR; // L2
			case 13: return KEY_POUND; // R2
			case 14: return KEY_NUM2; // Up 
			case 15: return KEY_NUM4; // Left
			case 16: return KEY_NUM6; // Right
			case 17: return KEY_NUM8; // Down
		}

		// If a matching key wasn't found, return 0;
		return 0;
	}

	public static final void log(byte logLevel, String text) 
	{
		if(!logging || (logLevel < minLogLevel)) { return; }

		switch(logLevel) 
		{
			case LOG_DEBUG:
				text = new String("[DEBUG] " + text);
				break;
			case LOG_INFO:
				text = new String("[INFO] " + text);
				break;
			case LOG_WARNING:
				text = new String("[WARNING] " + text);
				break;
			case LOG_ERROR:
				text = new String("[ERROR] " + text);
				break;
			case LOG_FATAL:
				text = new String("[FATAL] " + text);
				break;
		}

		// Log to console only if not libretro, as it won't be seen there anyway
		if(!MIDlet.isLibretro) { System.out.println(text); }

		File logFile = new File(LOG_FILE);

		// Create system dir if not available yet and try writing to the log file
		logFile.getParentFile().mkdirs();

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) 
		{
			writer.write(text);
			writer.newLine();
		} catch (IOException e) { System.out.println("Couldn't write to log file: " + e.getMessage()); e.printStackTrace(); }
	}

	/* Clears old log file at boot. */
	public static final void clearOldLog() 
	{
		File logFile = new File(LOG_FILE);
        if (logFile.exists()) { logFile.delete(); }
	}
}
