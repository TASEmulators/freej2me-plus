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
package org.recompile.freej2me;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import org.recompile.mobile.Mobile;

public class Config
{
	public boolean isRunning = false;

	private int width;
	private int height;

	private File cFile;
	private String configPath = "";
	private String configFile = "";

	private File sFile;
	private final String systemPath = "freej2me_system/";
	private final String systemFile = systemPath + "freej2me.conf";

	public static int inputKeycodes[] = new int[]
	{
		81,  // Q Key
		87,  // W Key
		38,  // Arrow Up
		37,  // Arrow Left
		10,  // Enter Key
		39,  // Arrow Right
		40,  // Arrow Down
		103, // Numpad_7
		104, // Numpad_8
		105, // Numpad_9
		100, // Numpad_4
		101, // Numpad_5
		102, // Numpad_6
		97,  // Numpad_1
		98,  // Numpad_2
		99,  // Numpad_3
		69,  // E Key
		96,  // Numpad_0
		82,  // R Key
		65,  // A key
		32,  // Space Key (for AWT fast-forward)
		67,  // C Key (for AWT screenshots)
		88   // X Key (for AWT Pause/Resume)
	};

	// Array with key names that match to each array index above.
	private static final String[] KEY_NAMES = {
		"input_LeftSoft", "input_RightSoft", "input_ArrowUp", "input_ArrowLeft",
		"input_Fire", "input_ArrowRight", "input_ArrowDown", "input_Num7",
		"input_Num8", "input_Num9", "input_Num4", "input_Num5",
		"input_Num6", "input_Num1", "input_Num2", "input_Num3",
		"input_Star", "input_Num0", "input_Pound", "input_CLR",
		"input_FastForward", "input_Screenshot", "input_PauseResume"
	};

	public Runnable onChange;

	public HashMap<String, String> settings = new HashMap<String, String>(48);
	public HashMap<String, String> sysSettings = new HashMap<String, String>(16);

	public Config()
	{
		width = Mobile.getPlatform().lcdWidth;
		height = Mobile.getPlatform().lcdHeight;

		onChange = new Runnable() { public void run() {} };
	}

	public void init(String appname)
	{
		try
		{
			// For ISO-8859-1 encodings, we'll use UTF-8 for save paths, helps
			// with chinese and special characters (Mirror RecordStore.java)
			configPath = new String((Mobile.getPlatform().dataPath +
				"./config/" + appname)
					.getBytes(System.getProperty("file.encoding")),
					System.getProperty("file.encoding").equals(
					Mobile.supportedEncodings[Mobile.ISO_8859_1]) ? "UTF-8" :
					Mobile.textEncoding);

			configFile = configPath + "/game.conf";
		}
		catch (UnsupportedEncodingException e) { }

		// Load Config //
		try
		{
			File configDir = new File(configPath);
			if (!configDir.exists()) { configDir.mkdirs(); }

			File sysDir = new File(systemPath);
			if (!sysDir.exists()) { sysDir.mkdirs(); }
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, Config.class.getPackage().getName() +
				"." + Config.class.getSimpleName() + ": " +
				"Problem Creating Config Path (" + configPath + ") :" + e.getMessage());
		}

		// Check Config Files
		try
		{
			cFile = new File(configFile);

			// Load default settings if they do not exist yet
			if(!cFile.exists())
			{
				settings.put("scrwidth", ""+width);
				settings.put("scrheight", ""+height);
				settings.put("phone", "Standard");
				settings.put("backlightcolor", "Disabled");
				settings.put("rotate", "0");
				settings.put("fps", "60");
				settings.put("fontoffset", "0");
				settings.put("spdhacknoalpha", "off");
				settings.put("compatfantasyzonefix", "off");
				settings.put("compattranstooriginonreset", "off");
				settings.put("compatimmediaterepaints", "off");
				settings.put("compatrepaintonsetcurrent", "off");
				settings.put("compatoverrideplatchecks", "on");
				settings.put("compatsiemensfriendlydrawing", "off");
				settings.put("compatignorevolumechanges", "off");
				settings.put("compatmcv3horizfovfix", "off");
				settings.put("fpshack", "Disabled");
				settings.put("spdhackm3ghalfres", "off");
				settings.put("m3gantialiasmode", "app");
				settings.put("m3gbilinearmode", "app");
				settings.put("m3gditheringmode", "app");
				settings.put("m3gperspcorrmode", "app");
				settings.put("m3gmipmapmode", "app");
				settings.put("m3gperspcorrsubfactor", "high");
				settings.put("m3gdisablefog", "off");
				settings.put("spdhackmcv3halfres", "off");
				settings.put("spdhackmcv3nolighting", "off");
				settings.put("dojaversion", "200");
				saveConfig();
			}

			// Same for system settings
			sFile = new File(systemFile);
			if(!sFile.exists())
			{
				sysSettings.put("fpsCounterPosition", "Off");
				sysSettings.put("logLevel", "2");
				sysSettings.put("M3GWireframe", "off");
				sysSettings.put("M3GUntextured", "off");
				sysSettings.put("MCV3ShowTimeMetrics", "off");
				sysSettings.put("MCV3ShowHeapUsage", "off");
				sysSettings.put("deleteTempKJXFiles", "on");
				sysSettings.put("dumpAudioStreams", "off");
				sysSettings.put("dumpGraphicsObjects", "off");
				sysSettings.put("sound", "on");
				sysSettings.put("soundfont", "Default");
				sysSettings.put("textfont", "Default");
				// AWT Inputs
				updateAWTInputs();
				saveSystemConfig();
			}
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "Problem Opening Config (" + configFile + ") :" + e.getMessage());
		}

		readConfig(cFile, settings);
		cleanAppSettings();

		readConfig(sFile, sysSettings);
		cleanSystemSettings();
	}

	private void cleanAppSettings()
	{
		settings.remove("compatcliprectongfxreset");
		settings.remove("width");
		settings.remove("height");
		settings.remove("compatignoregccalls");
		settings.remove("compatnonfatalnullimage");
		settings.remove("compatdonottranslatedrawrgb");
		settings.remove("sound");
		settings.remove("soundfont");
		settings.remove("textfont");
		settings.remove("spdhackm3gdisablebilinear");

		if ("on".equals(settings.get("rotate"))) settings.put("rotate", "270");
		else if ("off".equals(settings.get("rotate"))) settings.put("rotate", "0");

		if ("Sharp".equals(settings.get("phone"))) settings.put("phone", "MotoTriplets");

		if (!settings.containsKey("scrwidth")) { settings.put("scrwidth", "" + Mobile.lcdWidth); }
		if (!settings.containsKey("scrheight")) { settings.put("scrheight", "" + Mobile.lcdHeight); }
		if (!settings.containsKey("phone")) { settings.put("phone", "Standard"); }
		if (!settings.containsKey("rotate")) { settings.put("rotate", "" + Mobile.rotateDisplay); }
		if (!settings.containsKey("fps")) { settings.put("fps", "" + Mobile.limitFPS); }
		if (!settings.containsKey("fontoffset")) { settings.put("fontoffset", "" + Mobile.fontSizeOffset); }
		if (!settings.containsKey("spdhacknoalpha")) { settings.put("spdhacknoalpha", Mobile.noAlphaOnBlankImages ? "on" : "off"); }
		if (!settings.containsKey("dojaversion")) { settings.put("dojaversion", "" + Mobile.DoJaVersion); }
	}

	private void cleanSystemSettings()
	{
		if (!sysSettings.containsKey("fpsCounterPosition")) { sysSettings.put("fpsCounterPosition", "Off"); }
		if (!sysSettings.containsKey("logLevel")) { sysSettings.put("logLevel", "2"); }
		if (!sysSettings.containsKey("soundfont")) { sysSettings.put("soundfont", "Default"); }
		if (!sysSettings.containsKey("textfont")) { sysSettings.put("textfont", Mobile.useCustomTextFont ? "Custom" : "Default"); }
		if (!sysSettings.containsKey("sound")) { sysSettings.put("sound", Mobile.sound ? "on" : "off"); }

		for (int i = 0; i < KEY_NAMES.length; i++)
		{
			if (!sysSettings.containsKey(KEY_NAMES[i]))
			{
				sysSettings.put(KEY_NAMES[i], String.valueOf(inputKeycodes[i]));
			}
			else
			{
				try
				{
					inputKeycodes[i] = Integer.parseInt(sysSettings.get(KEY_NAMES[i]));
				}
				catch (NumberFormatException e) { }
			}
		}
	}

	private void readConfig(File file, HashMap<String, String> targetMap)
	{
		if (!file.exists()) { return; }

		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "ISO_8859_1"));
			String line;
			while ((line = reader.readLine()) != null)
			{
				String[] parts = line.split(":", 2);
				if (parts.length == 2)
				{
					String key = parts[0].trim();
					String val = parts[1].trim();
					if (!key.isEmpty() && !val.isEmpty())
					{
						targetMap.put(key, val);
					}
				}
			}
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, Config.class.getPackage().getName() +
				"." + Config.class.getSimpleName() + ": " +
				"Problem Reading Config (" + configFile + ") : " + e.getMessage());
		}
		finally
		{
			if (reader != null)
			{
				try { reader.close(); }
				catch (Exception e) { }
			}
		}
	}

	public void saveConfig() { saveFile(cFile, settings); }

	public void saveSystemConfig() { saveFile(sFile, sysSettings); }

	private void saveFile(File file, HashMap<String, String> map)
	{
		if(file == null) { return; }

		List<String> sortedKeys = new ArrayList<String>(map.keySet());
		Collections.sort(sortedKeys);

		BufferedWriter writer = null;
		try
		{
			writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(file), "ISO_8859_1"));
			for (String key : sortedKeys)
				{ writer.write(key + ":" + map.get(key) + "\n"); }
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, Config.class.getPackage().getName() +
				"." + Config.class.getSimpleName() + ": " + "Problem saving (" +
				file.getPath() + ") config: " + e.getMessage());
		}
		finally
		{
			if (writer != null)
			{
				try { writer.close(); }
				catch (Exception e) { }
			}
		}
	}

	public void updateDisplaySize(int w, int h)
	{
		settings.put("scrwidth", ""+w);
		settings.put("scrheight", ""+h);
		saveConfig();
		onChange.run();
		width = w;
		height = h;
	}

	// Per-app settings
	public void updateSetting(String key, String value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." +
			Config.class.getSimpleName() + ": " + "Config: "+ key + " : " + value);

		settings.put(key, value);
		saveConfig();
		onChange.run();
	}

	// System settings
	public void updateSysSetting(String key, String value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." +
			Config.class.getSimpleName() + ": " + "SysConfig: "+ key + " : " + value);

		sysSettings.put(key, value);
		saveSystemConfig();
		onChange.run();
	}

	public void updateAWTInputs()
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." +
			Config.class.getSimpleName() + ": " + "Updating inputs on System file");

		for (int i = 0; i < KEY_NAMES.length; i++)
		{
			sysSettings.put(KEY_NAMES[i], String.valueOf(inputKeycodes[i]));
		}
		saveSystemConfig();
		onChange.run();
	}
}
