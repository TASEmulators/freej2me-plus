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
import java.util.HashMap;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

import javax.microedition.media.Manager;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformImage;

public class Config
{
	public boolean isRunning = false;

	private int width;
	private int height;

	private File file;
	private String configPath = "";
	private String configFile = "";

	public final String[] supportedResolutions = {"96x65","101x64","101x80","128x128","130x130","120x160","128x160","132x176","176x208","176x220","220x176","208x208","180x320","320x180","208x320","240x320","320x240","240x400","400x240","240x432","240x480","352x416","360x640","640x360","640x480","480x800","800x480"};

	public Runnable onChange;

	HashMap<String, String> settings = new HashMap<String, String>(4);

	public Config()
	{
		
		width = Mobile.getPlatform().lcdWidth;
		height = Mobile.getPlatform().lcdHeight;

		onChange = new Runnable()
		{
			public void run()
			{
				// placeholder
			}
		};
	}

	public void init()
	{
		String appname = Mobile.getPlatform().loader.suitename;
		configPath = Mobile.getPlatform().dataPath + "./config/"+appname;
		configFile = configPath + "/game.conf";
		// Load Config //
		try
		{
			Files.createDirectories(Paths.get(configPath));
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "Problem Creating Config Path "+configPath);
			Mobile.log(Mobile.LOG_ERROR, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + e.getMessage());
		}

		try // Check Config File
		{
			file = new File(configFile);
			if(!file.exists())
			{
				file.createNewFile();
				settings.put("width", ""+width);
				settings.put("height", ""+height);
				settings.put("sound", "on");
				settings.put("phone", "Standard");
				settings.put("rotate", "off");
				settings.put("fps", "0");
				settings.put("soundfont", "Default");
				settings.put("spdhacknoalpha", "off");
				saveConfig();
			}
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "Problem Opening Config "+configFile);
			Mobile.log(Mobile.LOG_ERROR, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + e.getMessage());
		}

		try // Read Records
		{
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
			String[] parts;
			while((line = reader.readLine())!=null)
			{
				parts = line.split(":");
				if(parts.length==2)
				{
					parts[0] = parts[0].trim();
					parts[1] = parts[1].trim();
					if(parts[0]!="" && parts[1]!="")
					{
						// Compatibility with the deprecated "Nokia" input mapping, which is now the Standard mapping (as Canvas was reworked to not need J2ME's default mappings)
						if(parts[0].equals("phone") && parts[1].equals("Nokia") ) { parts[1] = "Standard"; }
						settings.put(parts[0], parts[1]);
					}
				}
			}
			if(!settings.containsKey("width")) { settings.put("width", ""+width); }
			if(!settings.containsKey("height")) { settings.put("height", ""+height); }
			if(!settings.containsKey("sound")) { settings.put("sound", "on"); }
			if(!settings.containsKey("phone")) { settings.put("phone", "Standard"); }
			if(!settings.containsKey("rotate")) { settings.put("rotate", "off"); }
			if(!settings.containsKey("fps")) { settings.put("fps", "0"); }
			if(!settings.containsKey("soundfont")) { settings.put("soundfont", "Default"); }
			if(!settings.containsKey("spdhacknoalpha")) { settings.put("spdhacknoalpha", "off"); }

		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "Problem Reading Config: "+configFile);
			Mobile.log(Mobile.LOG_ERROR, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + e.getMessage());
		}

	}

	public void saveConfig()
	{
		try
		{
			FileOutputStream fout = new FileOutputStream(file);

			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fout));

			for (String key : settings.keySet())
			{
				writer.write(key+":"+settings.get(key)+"\n");
			}
			writer.close();
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "Problem Opening Config "+configFile);
			Mobile.log(Mobile.LOG_ERROR, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + e.getMessage());
		}
	}

	public void updateDisplaySize(int w, int h)
	{
		settings.put("width", ""+w);
		settings.put("height", ""+h);
		saveConfig();
		onChange.run();
		width = w;
		height = h;
	}

	public void updateSound(String value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "Config: sound "+value);
		settings.put("sound", value);
		saveConfig();
		onChange.run();
	}

	public void updatePhone(String value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "Config: phone "+value);
		settings.put("phone", value);
		saveConfig();
		onChange.run();
	}

	public void updateRotate(String value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "Config: rotate "+value);
		settings.put("rotate", value);
		saveConfig();
		onChange.run();
	}

	public void updateFPS(String value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "Config: fps "+value);
		settings.put("fps", value);
		saveConfig();
		onChange.run();
	}

	public void updateSoundfont(String value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "Config: soundfont "+value);
		settings.put("soundfont", value);
		saveConfig();
		onChange.run();
	}

	public void updateAlphaSpeedHack(String value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "Config: spdhacknoalpha "+value);
		settings.put("spdhacknoalpha", value);
		saveConfig();
		onChange.run();
	}

}
