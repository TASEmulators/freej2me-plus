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
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

import javax.microedition.media.Manager;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformImage;

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

	public final String[] supportedResolutions = {"96x65","101x64","101x80","128x128","130x130","120x160","128x160","132x176","176x208","176x220","220x176","208x208","180x320","320x180", "240x240", "208x320","240x320","320x240","240x400","400x240","240x432","240x480","360x360","352x416","360x640","640x360","640x480","480x800","800x480"};

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

	public Runnable onChange;

	public HashMap<String, String> settings = new HashMap<String, String>(4);
	public HashMap<String, String> sysSettings = new HashMap<String, String>(4);

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

	public void init(String appname)
	{
        try
        {
            // For ISO-8859-1 encodings, we'll use UTF-8 for save paths, helps with chinese and special characters (Mirror RecordStore.java)
            configPath = new String((Mobile.getPlatform().dataPath + "./config/" + appname).getBytes(System.getProperty("file.encoding")), System.getProperty("file.encoding").equals(Mobile.supportedEncodings[Mobile.ISO_8859_1]) ? "UTF-8" : Mobile.textEncoding);
			configFile = configPath + "/game.conf";
        }
        catch (UnsupportedEncodingException e) { }
		// Load Config //
		try
		{
			File configDir = new File(configPath);
			if (!configDir.exists()) { configDir.mkdirs(); }

			configDir = new File(systemPath);
			if (!configDir.exists()) { configDir.mkdirs(); }
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "Problem Creating Config Path "+configPath);
			Mobile.log(Mobile.LOG_ERROR, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + e.getMessage());
		}

		try // Check Config File
		{
			cFile = new File(configFile);
			if(!cFile.exists())
			{
				cFile.createNewFile();
				settings.put("scrwidth", ""+width);
				settings.put("scrheight", ""+height);
				settings.put("sound", "on");
				settings.put("phone", "Standard");
				settings.put("backlightcolor", "Disabled");
				settings.put("rotate", "0");
				settings.put("fps", "0");
				settings.put("soundfont", "Default");
				settings.put("textfont", "Default");
				settings.put("fontoffset", "0");
				settings.put("spdhacknoalpha", "off");
				settings.put("compatfantasyzonefix", "off");
				settings.put("compattranstooriginonreset", "off");
				settings.put("compatimmediaterepaints", "off");
				settings.put("compatoverrideplatchecks", "on");
				settings.put("compatsiemensfriendlydrawing", "off");
				settings.put("fpshack", "Disabled");
				settings.put("spdhackm3ghalfres", "off");
				settings.put("dojaversion", "200");
				saveConfig();
			}

			sFile = new File(systemFile);
			if(!sFile.exists())
			{
				sFile.createNewFile();
				sysSettings.put("fpsCounterPosition", "Off");
				sysSettings.put("logLevel", "2");
				sysSettings.put("M3GWireframe", "off");
				sysSettings.put("M3GUntextured", "off");
				sysSettings.put("deleteTempKJXFiles", "on");
				sysSettings.put("dumpAudioStreams", "off");
				sysSettings.put("dumpGraphicsObjects", "off");
				// AWT Inputs
				updateAWTInputs();
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
			BufferedReader reader = new BufferedReader(new FileReader(cFile));
			String line;
			String[] parts;
			while((line = reader.readLine())!=null)
			{
				parts = line.split(":");
				if(parts.length==2)
				{
					parts[0] = parts[0].trim();
					parts[1] = parts[1].trim();
					if(parts[0]!="" && parts[1]!="") { settings.put(parts[0], parts[1]); }
				}
			}
			// Remove now invalid settings
			if(settings.containsKey("compatcliprectongfxreset")) { settings.remove("compatcliprectongfxreset"); }
			if(settings.containsKey("width")) { settings.remove("width"); }
			if(settings.containsKey("height")) { settings.remove("height"); }
			if(settings.containsKey("compatignoregccalls")) { settings.remove("compatignoregccalls"); } // These are now ignored by default, after some bug fixes to lcdui canvas
			if(settings.containsKey("compatnonfatalnullimage")) { settings.remove("compatnonfatalnullimage"); } // No longer needed
			if(settings.containsKey("compatdonottranslatedrawrgb")) { settings.remove("compatdonottranslatedrawrgb"); } // No longer needed
			if(settings.containsKey("rotate")) // Compatibility with older, more limited rotation toggle
			{ 
				if(settings.get("rotate").equals("on")) { settings.put("rotate", "270"); }
				else if(settings.get("rotate").equals("off")) { settings.put("rotate", "0"); }
			}

			// Add any missing settings
			if(!settings.containsKey("scrwidth")) { settings.put("scrwidth", ""+width); }
			if(!settings.containsKey("scrheight")) { settings.put("scrheight", ""+height); }
			if(!settings.containsKey("sound")) { settings.put("sound", "on"); }
			if(!settings.containsKey("phone")) { settings.put("phone", "Standard"); }
			if(!settings.containsKey("backlightcolor")) { settings.put("backlightcolor", "Disabled"); }
			if(!settings.containsKey("rotate")) { settings.put("rotate", "0"); }
			if(!settings.containsKey("fps")) { settings.put("fps", "0"); }
			if(!settings.containsKey("soundfont")) { settings.put("soundfont", "Default"); }
			if(!settings.containsKey("textfont")) { settings.put("textfont", "Default"); }
			if(!settings.containsKey("fontoffset")) { settings.put("fontoffset", "0"); }
			if(!settings.containsKey("spdhacknoalpha")) { settings.put("spdhacknoalpha", "off"); }
			if(!settings.containsKey("compatfantasyzonefix")) { settings.put("compatfantasyzonefix", "off"); }
			if(!settings.containsKey("compattranstooriginonreset")) { settings.put("compattranstooriginonreset", "off"); }
			if(!settings.containsKey("compatimmediaterepaints")) { settings.put("compatimmediaterepaints", "off"); }
			if(!settings.containsKey("compatoverrideplatchecks")) { settings.put("compatoverrideplatchecks", "on"); }
			if(!settings.containsKey("compatsiemensfriendlydrawing")) { settings.put("compatsiemensfriendlydrawing", "off"); }
			if(!settings.containsKey("fpshack")) { settings.put("fpshack", "Disabled"); }
			if(!settings.containsKey("spdhackm3ghalfres")) { settings.put("spdhackm3ghalfres", "off"); }
			if(!settings.containsKey("dojaversion")) { settings.put("dojaversion", "200"); }

			// System settings
			reader = new BufferedReader(new FileReader(sFile));
			while((line = reader.readLine())!=null)
			{
				parts = line.split(":");
				if(parts.length==2)
				{
					parts[0] = parts[0].trim();
					parts[1] = parts[1].trim();
					if(parts[0]!="" && parts[1]!="") { sysSettings.put(parts[0], parts[1]); }
				}
			}

			if(!sysSettings.containsKey("fpsCounterPosition")) { sysSettings.put("fpsCounterPosition", "Off"); }
			if(!sysSettings.containsKey("logLevel")) { sysSettings.put("logLevel", "2"); }
			if(!sysSettings.containsKey("M3GWireframe")) { sysSettings.put("M3GWireframe", "off"); }
			if(!sysSettings.containsKey("M3GUntextured")) { sysSettings.put("M3GUntextured", "off"); }
			if(!sysSettings.containsKey("deleteTempKJXFiles")) { sysSettings.put("deleteTempKJXFiles", "on"); }
			if(!sysSettings.containsKey("dumpAudioStreams")) { sysSettings.put("dumpAudioStreams", "off"); }
			if(!sysSettings.containsKey("dumpGraphicsObjects")) { sysSettings.put("dumpGraphicsObjects", "off"); }
			// AWT Inputs
			if(!sysSettings.containsKey("input_LeftSoft"))    { sysSettings.put("input_LeftSoft", ""     + inputKeycodes[0]); }
			if(!sysSettings.containsKey("input_RightSoft"))   { sysSettings.put("input_RightSoft", ""    + inputKeycodes[1]); }
			if(!sysSettings.containsKey("input_ArrowUp"))     { sysSettings.put("input_ArrowUp", ""      + inputKeycodes[2]); }
			if(!sysSettings.containsKey("input_ArrowLeft"))   { sysSettings.put("input_ArrowLeft", ""    + inputKeycodes[3]); }
			if(!sysSettings.containsKey("input_Fire"))        { sysSettings.put("input_Fire", ""         + inputKeycodes[4]); }
			if(!sysSettings.containsKey("input_ArrowRight"))  { sysSettings.put("input_ArrowRight", ""   + inputKeycodes[5]); }
			if(!sysSettings.containsKey("input_ArrowDown"))   { sysSettings.put("input_ArrowDown", ""    + inputKeycodes[6]); }
			if(!sysSettings.containsKey("input_Num7"))        { sysSettings.put("input_Num7", ""         + inputKeycodes[7]); }
			if(!sysSettings.containsKey("input_Num8"))        { sysSettings.put("input_Num8", ""         + inputKeycodes[8]); }
			if(!sysSettings.containsKey("input_Num9"))        { sysSettings.put("input_Num9", ""         + inputKeycodes[9]); }
			if(!sysSettings.containsKey("input_Num4"))        { sysSettings.put("input_Num4", ""         + inputKeycodes[10]); }
			if(!sysSettings.containsKey("input_Num5"))        { sysSettings.put("input_Num5", ""         + inputKeycodes[11]); }
			if(!sysSettings.containsKey("input_Num6"))        { sysSettings.put("input_Num6", ""         + inputKeycodes[12]); }
			if(!sysSettings.containsKey("input_Num1"))        { sysSettings.put("input_Num1", ""         + inputKeycodes[13]); }
			if(!sysSettings.containsKey("input_Num2"))        { sysSettings.put("input_Num2", ""         + inputKeycodes[14]); }
			if(!sysSettings.containsKey("input_Num3"))        { sysSettings.put("input_Num3", ""         + inputKeycodes[15]); }
			if(!sysSettings.containsKey("input_Star"))        { sysSettings.put("input_Star", ""         + inputKeycodes[16]); }
			if(!sysSettings.containsKey("input_Num0"))        { sysSettings.put("input_Num0", ""         + inputKeycodes[17]); }
			if(!sysSettings.containsKey("input_Pound"))       { sysSettings.put("input_Pound", ""        + inputKeycodes[18]); }
			if(!sysSettings.containsKey("input_CLR"))         { sysSettings.put("input_CLR", ""          + inputKeycodes[19]); }
			if(!sysSettings.containsKey("input_FastForward")) { sysSettings.put("input_FastForward", ""  + inputKeycodes[20]); }
			if(!sysSettings.containsKey("input_Screenshot"))  { sysSettings.put("input_Screenshot", ""   + inputKeycodes[21]); }
			if(!sysSettings.containsKey("input_PauseResume")) { sysSettings.put("input_PauseResume", ""  + inputKeycodes[22]); }

			inputKeycodes[0] = Integer.parseInt(sysSettings.get("input_LeftSoft"));
			inputKeycodes[1] = Integer.parseInt(sysSettings.get("input_RightSoft"));
			inputKeycodes[2] = Integer.parseInt(sysSettings.get("input_ArrowUp"));
			inputKeycodes[3] = Integer.parseInt(sysSettings.get("input_ArrowLeft"));
			inputKeycodes[4] = Integer.parseInt(sysSettings.get("input_Fire"));
			inputKeycodes[5] = Integer.parseInt(sysSettings.get("input_ArrowRight"));
			inputKeycodes[6] = Integer.parseInt(sysSettings.get("input_ArrowDown"));
			inputKeycodes[7] = Integer.parseInt(sysSettings.get("input_Num7"));
			inputKeycodes[8] = Integer.parseInt(sysSettings.get("input_Num8"));
			inputKeycodes[9] = Integer.parseInt(sysSettings.get("input_Num9"));
			inputKeycodes[10] = Integer.parseInt(sysSettings.get("input_Num4"));
			inputKeycodes[11] = Integer.parseInt(sysSettings.get("input_Num5"));
			inputKeycodes[12] = Integer.parseInt(sysSettings.get("input_Num6"));
			inputKeycodes[13] = Integer.parseInt(sysSettings.get("input_Num1"));
			inputKeycodes[14] = Integer.parseInt(sysSettings.get("input_Num2"));
			inputKeycodes[15] = Integer.parseInt(sysSettings.get("input_Num3"));
			inputKeycodes[16] = Integer.parseInt(sysSettings.get("input_Star"));
			inputKeycodes[17] = Integer.parseInt(sysSettings.get("input_Num0"));
			inputKeycodes[18] = Integer.parseInt(sysSettings.get("input_Pound"));
			inputKeycodes[19] = Integer.parseInt(sysSettings.get("input_CLR"));
			inputKeycodes[20] = Integer.parseInt(sysSettings.get("input_FastForward"));
			inputKeycodes[21] = Integer.parseInt(sysSettings.get("input_Screenshot"));
			inputKeycodes[22] = Integer.parseInt(sysSettings.get("input_PauseResume"));

			onChange.run();
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
			FileOutputStream fout = new FileOutputStream(cFile);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fout));

			// Sort the config keys alphabetically before saving
			List<String> sortedKeys = new ArrayList<String>(settings.keySet());
        	Collections.sort(sortedKeys);

			for (String key : sortedKeys)
			{
				writer.write(key+":"+settings.get(key)+"\n");
			}
			writer.close();
			fout.close();

			/* Save system file (if it has been created already), also sorted alphabetically */
			if(sFile != null) 
			{
				sortedKeys = new ArrayList<String>(sysSettings.keySet());
				Collections.sort(sortedKeys);

				fout = new FileOutputStream(sFile);
				writer = new BufferedWriter(new OutputStreamWriter(fout));

				for (String key : sortedKeys)
				{
					writer.write(key+":"+sysSettings.get(key)+"\n");
				}
				writer.close();
				fout.close();
			}
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "Problem saving configs: " + e.getMessage());
			Mobile.log(Mobile.LOG_ERROR, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + e.getMessage());
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

	public void updateTextFont(String value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "Config: textfont "+value);
		settings.put("textfont", value);
		saveConfig();
		onChange.run();
	}

	public void updateFontOffset(String value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "Config: fontoffset "+value);
		settings.put("fontoffset", value);
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

	public void updateM3GResSpeedHack(String value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "Config: spdhackm3ghalfres "+value);
		settings.put("spdhackm3ghalfres", value);
		saveConfig();
		onChange.run();
	}

	public void updateDoJaVersion(String value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "Config: dojaversion "+value);
		settings.put("dojaversion", value);
		saveConfig();
		onChange.run();
	}

	public void updateCompatFantasyZoneFix(String value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "Config: compatfantasyzonefix "+value);
		settings.put("compatfantasyzonefix", value);
		saveConfig();
		onChange.run();
	}

	public void updateCompatTranslateToOriginOnReset(String value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "Config: compattranstooriginonreset "+value);
		settings.put("compattranstooriginonreset", value);
		saveConfig();
		onChange.run();
	}

	public void updateCompatImmediateRepaints(String value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "Config: compatimmediaterepaints "+value);
		settings.put("compatimmediaterepaints", value);
		saveConfig();
		onChange.run();
	}

	public void updateCompatOverridePlatformChecks(String value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "Config: compatoverrideplatchecks "+value);
		settings.put("compatoverrideplatchecks", value);
		saveConfig();
		onChange.run();
	}

	public void updateCompatSiemensFriendlyDrawing(String value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "Config: compatsiemensfriendlydrawing "+value);
		settings.put("compatsiemensfriendlydrawing", value);
		saveConfig();
		onChange.run();
	}

	public void updateFPSHack(String value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "Config: fpshack "+value);
		settings.put("fpshack", value);
		saveConfig();
		onChange.run();
	}

	public void updateBacklight(String value) 
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "Config: backlightcolor "+value);
		settings.put("backlightcolor", value);
		saveConfig();
		onChange.run();
	}


	// System settings

	public void updatefpsCounterPosition(String value) 
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "SysConfig: fpsCounterPosition "+value);
		sysSettings.put("fpsCounterPosition", value);
		saveConfig();
		onChange.run();
	}

	public void updateLogLevel(String value) 
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "SysConfig: logLevel "+value);
		sysSettings.put("logLevel", value);
		saveConfig();
		onChange.run();
	}

	public void updateM3GWireframe(String value) 
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "SysConfig: M3GWireframe "+value);
		sysSettings.put("M3GWireframe", value);
		saveConfig();
		onChange.run();
	}

	public void updateM3GUntextured(String value) 
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "SysConfig: M3GUntextured "+value);
		sysSettings.put("M3GUntextured", value);
		saveConfig();
		onChange.run();
	}

	public void updateDeleteTempKJXFiles(String value) 
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "SysConfig: deleteTempKJXFiles "+value);
		sysSettings.put("deleteTempKJXFiles", value);
		saveConfig();
		onChange.run();
	}

	public void updateDumpAudioStreams(String value) 
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "SysConfig: dumpAudioStreams "+value);
		sysSettings.put("dumpAudioStreams", value);
		saveConfig();
		onChange.run();
	}

	public void updateDumpGraphicsObjects(String value) 
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "SysConfig: dumpGraphicsObjects "+value);
		sysSettings.put("dumpGraphicsObjects", value);
		saveConfig();
		onChange.run();
	}

	public void updateAWTInputs() 
	{
		Mobile.log(Mobile.LOG_DEBUG, Config.class.getPackage().getName() + "." + Config.class.getSimpleName() + ": " + "Updating inputs on System file");
		sysSettings.put("input_LeftSoft", ""     + inputKeycodes[0]);
		sysSettings.put("input_RightSoft", ""    + inputKeycodes[1]);
		sysSettings.put("input_ArrowUp", ""      + inputKeycodes[2]);
		sysSettings.put("input_ArrowLeft", ""    + inputKeycodes[3]);
		sysSettings.put("input_Fire", ""         + inputKeycodes[4]);
		sysSettings.put("input_ArrowRight", ""   + inputKeycodes[5]);
		sysSettings.put("input_ArrowDown", ""    + inputKeycodes[6]);
		sysSettings.put("input_Num7", ""         + inputKeycodes[7]);
		sysSettings.put("input_Num8", ""         + inputKeycodes[8]);
		sysSettings.put("input_Num9", ""         + inputKeycodes[9]);
		sysSettings.put("input_Num4", ""         + inputKeycodes[10]);
		sysSettings.put("input_Num5", ""         + inputKeycodes[11]);
		sysSettings.put("input_Num6", ""         + inputKeycodes[12]);
		sysSettings.put("input_Num1", ""         + inputKeycodes[13]);
		sysSettings.put("input_Num2", ""         + inputKeycodes[14]);
		sysSettings.put("input_Num3", ""         + inputKeycodes[15]);
		sysSettings.put("input_Star", ""         + inputKeycodes[16]);
		sysSettings.put("input_Num0", ""         + inputKeycodes[17]);
		sysSettings.put("input_Pound", ""        + inputKeycodes[18]);
		sysSettings.put("input_CLR", ""          + inputKeycodes[19]);
		sysSettings.put("input_FastForward", ""  + inputKeycodes[20]);
		sysSettings.put("input_Screenshot",  ""  + inputKeycodes[21]);
		sysSettings.put("input_PauseResume", ""  + inputKeycodes[22]);
		saveConfig();
		onChange.run();
	}

}
