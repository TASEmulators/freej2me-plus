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

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.ClassLoader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.nttdocomo.ui.IApplication;

import javax.microedition.content.Registry;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Canvas;

public class MIDletLoader extends URLClassLoader
{

	public String icon;
	public static String[] name = new String[9];
	private String[] className = new String[9];

	public static URL baseUrl;
	private static JarFile jarFile;
	private static List<JarEntry> jarEntries = new ArrayList<JarEntry>();

	public String suitename;
	public String vendorname;

	private Class<?> mainClass;
	private MIDlet midletInst;
	private IApplication IAppliInst;
	
	private Registry reg;

	private HashMap<String, String> properties = new HashMap<String, String>(32);

	// For the multi-midlet selection screen
	private PlatformImage platformImage;
	private Graphics graphics;
	private static byte selectedMidlet = 0;
	public static boolean MIDletSelected = false;

	private static final String[] knownModelStrings =
	{
		"nokia", "samsung", "siemens", "sharp", "sonyericsson", "sony ericsson", "motorola", "sagem", "lg",
		"c65", "cv65", "cx65", "sx1", "n80", "e61", "n73", "n95", "gx10", "gx15", "gx20", "gx25", "gx30", "k300",
		"k500", "s700", "k800", "k850" // TODO: Add more devices if any jar has sudden issues booting
	};

	private static final String[] supportedLocales =
	{
		"en-US", // English (United States)
		"en-UK", // English (United Kingdom)
		"en",    // Broader English fallback (all other locales have one too)
		"xx",    // Some jars fallback to this for english (Bounce Tales, Nature Park)
		"fr-FR", // French (France)
		"fr",
		"de-DE", // German (Germany)
		"de",
		"es-ES", // Spanish (Spain)
		"es",
		"it-IT", // Italian (Italy)
		"it",
		"ja-JP", // Japanese (Japan)
		"ja",
		"zh-CN", // Chinese (Simplified)
		"zh-TW", // Chinese (Traditional)
		"zh",
		"ko-KR", // Korean (South Korea)
		"ko",
		"pt-PT", // Portuguese (Portugal)
		"pt-BR", // Portuguese (Brazil)
		"pt",
		"ru-RU", // Russian (Russia)
		"ru",
		"ar-AE", // Arabic (UAE)
		"ar",
		"hi-IN", // Hindi (India)
		"hi",
		"tr-TR", // Turkish (Turkey)
		"tr"
	};


	public MIDletLoader(URL url, Map<String, String> descriptorProperties)
	{
		super(new URL[] {url} );

		try 
		{
			String jarName = new File(url.getFile()).getName().replace('.', '_');
			suitename = jarName;
			File file = new File(url.toURI());
            jarFile = new JarFile(file);
			loadJarEntries();
			baseUrl = url;
		} 
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Failed to parse jar:" + e.getMessage());
			e.printStackTrace();
		}

		System.setProperty("audio.samplerates", "8000 11025 12000 16000 22050 24000 32000 44100 48000");
		System.setProperty("audio3d.simultaneouslocations", "8");
		System.setProperty("bluetooth.api.version", "1.0");
		System.setProperty("camera.orientations", "devcam0:outwards devcam1:inwards");
		System.setProperty("camera.resolutions", "devcam0:640x480 devcam1:640x480");
		System.setProperty("com.siemens.IMEI", "000000000005152");
		System.setProperty("com.siemens.OSVersion", "11");
		System.setProperty("com.sonyericsson.imei", "IMEI9 00460101-501594-5-00");
		System.setProperty("device.imei", "000000000000000");
		System.setProperty("microedition.amms.version", "1.1");
		System.setProperty("microedition.broadcast.supports.filecache", "true");
		System.setProperty("microedition.broadcast.supports.overlay", "true");
		System.setProperty("microedition.broadcast.supports.purchasing", "true");
		System.setProperty("microedition.broadcast.supports.timedrecording", "true");
		System.setProperty("microedition.broadcast.version", "1.0");
		System.setProperty("microedition.configuration", "CLDC-1.1");
		System.setProperty("microedition.encoding", System.getProperty("file.encoding"));
		System.setProperty("microedition.io.file.FileConnection.version", "1.0");
		System.setProperty("microedition.jtwi.version", "1.0");
		System.setProperty("microedition.locale", "en-US");
		System.setProperty("microedition.media.version", "1.1");
		System.setProperty("microedition.m3g.version", "1.1");
		System.setProperty("microedition.pim.version", "1.0");
		System.setProperty("microedition.sensor.version", "1.0");
		System.setProperty("microedition.platform", "FreeJ2ME-Plus, a Cross-Platform J2ME Emulator.");
		System.setProperty("microedition.profiles", "MIDP-2.0");
		System.setProperty("supports.audio.capture", "true");
		System.setProperty("supports.mediacapabilities", "music audio3d imageencoding imagepostprocessing camera tuner");
		System.setProperty("supports.mixing", "true");
		System.setProperty("supports.recording", "true");
		System.setProperty("supports.video.capture", "true");
		System.setProperty("tuner.modulations", "am fm");
		System.setProperty("wireless.messaging.mms.mmsc", "http://abc.stubfreej2meplus.net");
		System.setProperty("wireless.messaging.sms.smsc", "+8613800010000");
		System.setProperty("wireless.messaging.version", "1.0");

		// SKT stuff
		System.setProperty("com.xce.wipi.version", "1.0.0");
		System.setProperty("m.SK_VM", "20");
		System.setProperty("m.VENDER", "LG");
		System.setProperty("m.MODEL", "11");
		System.setProperty("m.CARRIER", "SKT");
		System.setProperty("m.COLOR", "5");
		System.setProperty("m.MIN", "0000000000");
		System.setProperty("MIN", "0000000000"); // legacy SK-VM 1.0.x property

		// Integrate properties retrieved from JAD file, if any.
		properties.putAll(descriptorProperties);

		try { loadManifest(); }
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Can't Read Manifest!");
		}

		properties.put("audio.samplerates", "8000 11025 12000 16000 22050 24000 32000 44100 48000");
		properties.put("audio3d.simultaneouslocations", "8");
		properties.put("bluetooth.api.version", "1.0");
		properties.put("camera.orientations", "devcam0:outwards devcam1:inwards");
		properties.put("camera.resolutions", "devcam0:640x480 devcam1:640x480");
		properties.put("com.siemens.IMEI", "000000000005152");
		properties.put("com.siemens.OSVersion", "11");
		properties.put("com.sonyericsson.imei", "IMEI9 00460101-501594-5-00");
		properties.put("device.imei", "000000000000000");
		properties.put("microedition.amms.version", "1.1");
		properties.put("microedition.broadcast.supports.filecache", "true");
		properties.put("microedition.broadcast.supports.overlay", "true");
		properties.put("microedition.broadcast.supports.purchasing", "true");
		properties.put("microedition.broadcast.supports.timedrecording", "true");
		properties.put("microedition.broadcast.version", "1.0");
		properties.put("microedition.configuration", "CLDC-1.1");
		properties.put("microedition.encoding", System.getProperty("file.encoding"));
		properties.put("microedition.io.file.FileConnection.version", "1.0");
		properties.put("microedition.jtwi.version", "1.0");
		properties.put("microedition.locale", "en-US");
		properties.put("microedition.media.version", "1.1");
		properties.put("microedition.m3g.version", "1.1");
		properties.put("microedition.platform", "FreeJ2ME-Plus, a Cross-Platform J2ME Emulator.");
		properties.put("microedition.profiles", "MIDP-2.0");
		properties.put("microedition.pim.version", "1.0");
		properties.put("microedition.sensor.version", "1.0");
		properties.put("supports.audio.capture", "true");
		properties.put("supports.mediacapabilities", "music audio3d imageencoding imagepostprocessing camera tuner");
		properties.put("supports.mixing", "true");
		properties.put("supports.recording", "true");
		properties.put("supports.video.capture", "true");
		properties.put("tuner.modulations", "am fm");
		properties.put("wireless.messaging.mms.mmsc", "http://abc.stubfreej2meplus.net");
		properties.put("wireless.messaging.sms.smsc", "+8613800010000");
		properties.put("wireless.messaging.version", "1.0");

		if (className[0] == null) { className[0] = findMainClassInJar(url); }
	}

	public static String findMainClassInJar(URL url) 
	{
		// we search for a class file containing "startApp" 
		// note this is just an approximation, but it often works
		// the class might be abstract though..
		for (JarEntry entry : jarEntries) 
		{
            if (entry.getName().endsWith(".class")) 
			{
                String className = entry.getName().replace('/', '.').replace(".class", "");
				try 
				{
					if (hasStartApp(className, jarFile.getInputStream(entry))) { return className; }
				}
                catch (IOException e) { e.printStackTrace(); }
            }
        }		
        return null;
    }

	private void loadJarEntries() 
	{
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) 
		{
			JarEntry entry = entries.nextElement();
			jarEntries.add(entry);
		}
    }

	private static boolean hasStartApp(String className, InputStream is) 
	{
		byte[] pattern = "startApp".getBytes();
		try 
		{
			byte[] classBytes = readBytes(is);
			for (int i = 0; i < classBytes.length - pattern.length; i++) 
			{
				int j = 0;
				for (j = 0; j < pattern.length; j++) 
				{
					if (classBytes[i+j] != pattern[j]) { break; }
				}
				if (j == pattern.length) { return true; }
			}
		} catch (IOException e) { e.printStackTrace(); }
  		finally 
		{
			try { is.close(); } 
			catch (IOException e) { e.printStackTrace(); }
		} 
        return false;
    }

	private static byte[] readBytes(InputStream is) throws IOException 
	{
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = is.read(data, 0, data.length)) != -1) { buffer.write(data, 0, nRead); }
        return buffer.toByteArray();
    }

	public void start() throws MIDletStateChangeException
	{
		Method start = null;

		try
		{
			if(className[1] != null) // More than one element, bring up the selection menu
			{
				platformImage = new PlatformImage(Mobile.lcdWidth, Mobile.lcdHeight);
				graphics = platformImage.getMIDPGraphics();

				while(!MIDletSelected) { render(); }
				// keyPress() will run until MIDletSelected becomes true
			}

			// If there's only one midlet, load it straight away
			MIDletSelected = true;
			if(className[selectedMidlet] != null) 
			{
				mainClass = loadClass(className[selectedMidlet]);

				Constructor constructor;
				constructor = mainClass.getConstructor();
				constructor.setAccessible(true);

				if(!Mobile.isDoJa) 
				{
					MIDlet.initAppProperties(properties);
					midletInst = (MIDlet)constructor.newInstance();
				}
				else 
				{
					IApplication.initAppProperties(properties);
					IAppliInst = (IApplication) constructor.newInstance();
				}
				
			}
			
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Problem Constructing " + name + " class: " +className);
			Mobile.log(Mobile.LOG_ERROR, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Reason: "+e.getMessage());
			e.printStackTrace();
			return;
		}

		try
		{
			while (start == null)
			{
				try
				{
					if (mainClass.getName().endsWith("ALW1")) 
					{ 
						Mobile.log(Mobile.LOG_WARNING, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "ALW1 wrapper class detected! Attempting to bypass...");
						start = mainClass.getDeclaredMethod("startRealApp");
					}
					else { start = Mobile.isDoJa ? mainClass.getDeclaredMethod("start") : mainClass.getDeclaredMethod("startApp"); }
					start.setAccessible(true);
				}
				catch (NoSuchMethodException e)
				{
					mainClass = mainClass.getSuperclass();
					if (mainClass == null || mainClass == MIDlet.class || mainClass == IApplication.class) { throw e; }

					mainClass = loadClass(mainClass.getName(), true);
				}
			}
			start.invoke(Mobile.isDoJa ? IAppliInst : midletInst);
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Can't invoke startApp Method: " + e.getMessage());
			e.printStackTrace();
			return;
		}
	}

	public static void parseDescriptorInto(InputStream is, Map<String, String> keyValueMap) {
		parseDescriptorInto(is, keyValueMap, StandardCharsets.UTF_8);
	}

	public static void parseDescriptorInto(InputStream is, Map<String, String> keyValueMap, Charset charset)
	{
		boolean hasMIDlet = false;
        String currentKey = null;
        StringBuilder currentValue = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, charset)))
		{
            String line;
            while ((line = br.readLine()) != null) 
			{
				if (line.trim().isEmpty()) { continue; }
                if (line.startsWith(" ")) { currentValue.append(line, 1, line.length()); } 
				else 
				{
                    if (currentKey != null) 
					{
						if (currentKey.contains("MIDlet-")) { hasMIDlet = true; }
						// Only add a new key-value pair if the key doesn't already exist (set by the JAD file)
                        if(!keyValueMap.containsKey(currentKey)) 
						{
							keyValueMap.put(currentKey, currentValue.toString().trim());  
						}
						else { Mobile.log(Mobile.LOG_DEBUG, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "properties already contain " + currentKey + "! Maintaining current value: " + keyValueMap.get(currentKey)); }
                        currentValue.setLength(0);
                    }

                    int colonIndex = line.indexOf(':');

                    if (colonIndex != -1) 
					{
                        currentKey = line.substring(0, colonIndex).trim();
                        currentValue.append(line.substring(colonIndex + 1).trim());
                    }
                }
            }
            if (currentKey != null) 
			{
				if(!keyValueMap.containsKey(currentKey)) { keyValueMap.put(currentKey, currentValue.toString().trim());  }
				else { Mobile.log(Mobile.LOG_DEBUG, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "properties already contain " + currentKey + "! Maintaining current value: " + keyValueMap.get(currentKey)); }
			}

			if(keyValueMap.containsKey("MIDlet-1")) { hasMIDlet = true; }

			// If no MIDlet was found above, we'll try loading this jar as a DoJa file, which has an accompanying .jam descriptor (this is fine because if a jad is present, it's loaded before this method is even called)
			Mobile.isDoJa = !hasMIDlet;
			br.close();
        }
		catch (IOException e) 
		{
            Mobile.log(Mobile.LOG_ERROR, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Failed to parse descriptor:" + e.getMessage());
        }
    }

	// TODO: Convert to Jam descriptor parsing
	public static void parseJamDescriptorInto(InputStream is, Map<String, String> keyValueMap) 
	{
		Mobile.log(Mobile.LOG_DEBUG, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Parsing .JAM...");
		String currentKey = null;
		StringBuilder currentValue = new StringBuilder();
	
		try
		{
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "Shift_JIS")); // DoJa uses the Shift_JIS charset in its descriptor
			String line;
			while ((line = br.readLine()) != null) 
			{
				if (line.trim().isEmpty()) { continue; }
	
				if (line.startsWith(" ")) { currentValue.append(line.trim()); } 
				else 
				{
					// If there's a current valid key, store the value
					if (currentKey != null) 
					{
						keyValueMap.put(currentKey, currentValue.toString().trim());
						Mobile.log(Mobile.LOG_DEBUG, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Adding prop:" + currentKey + " (" + currentKey + ") val:" + currentValue.toString().trim());
						currentValue.setLength(0);
					}
	
					// Split on '=' to get the key and value (standard MIDlet manifests use ":" as the separator)
					int equalsIndex = line.indexOf('=');
					if (equalsIndex != -1) 
					{
						currentKey = line.substring(0, equalsIndex).trim();
						currentValue.append(line.substring(equalsIndex + 1).trim());
					}
				}
			}
	
			// Store the last key-value pair if it exists
			if (currentKey != null) 
			{
				keyValueMap.put(currentKey, currentValue.toString().trim());

				Mobile.log(Mobile.LOG_DEBUG, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Adding prop:" + currentKey + " (" + currentKey + ") val:" + currentValue.toString().trim());
			}

			br.close();
		}
		catch (IOException e) { Mobile.log(Mobile.LOG_ERROR, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Failed to parse descriptor:" + e.getMessage()); }
	}

	private void loadManifest()
	{
		String resource = "META-INF/MANIFEST.MF";
		URL url = findResource(resource);
		if (url == null) 
		{
			resource = "META-INF/MANIFEST.FM";
			url = findResource(resource);
		}

		if(url != null) // Standard MIDlet manifest is present (at least i assume so)
		{
			try { parseDescriptorInto(url.openStream(), properties); }
			catch (Exception e) 
			{
				Mobile.log(Mobile.LOG_ERROR, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Can't Read Jar Manifest!");
				e.printStackTrace();
			}
		}
		else { Mobile.isDoJa = properties.containsKey("MIDlet-1") ? false : true; } // Else we assume it as DoJa if a JAD file wasn't found, or if it was found but it, like the manifest, doesn't have the MIDlet token

		if(Mobile.isDoJa) // No manifest found in the jar, or the manifest doesn't have a midlet specified. Maybe it's a DoJa file that has an accompanying .jam?
		{
			Mobile.log(Mobile.LOG_WARNING, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "JAR Manifest file not found or lacks MIDlet entry! Checking if it's a DoJa File");
			
			String jamURLString = baseUrl.toString().replace(".jar", ".jam");
			Mobile.log(Mobile.LOG_DEBUG, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Path:" + jamURLString);

			try
			{
				URI jamURI = new URI(jamURLString);
				File jamFile = new File(jamURI);
				if(jamFile.exists()) 
				{
					Mobile.log(Mobile.LOG_INFO, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "JAM File Found!");
					URL jamURL = jamURI.toURL();
					parseJamDescriptorInto(jamURL.openStream(), properties);
				}
			} 
			catch (Exception e) 
			{
				Mobile.log(Mobile.LOG_WARNING, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Could not parse .jam file:" + e.getMessage());
			}
		}

		if(!Mobile.isDoJa) 
		{
			for(int i = 0; i < 9; i++) // Support loading up to 9 midlets, though i doubt any jar will have more than a few.
			{
				if (properties.containsKey("MIDlet-" + (i+1) )) // Starts from MIDlet-1
				{
					String val = properties.get("MIDlet-" + (i+1) );
					String[] parts = val.split(",");
					int argLength = parts.length; // No need for an int here, at max we have 3 arguments

					if (argLength == 3) 
					{
						name[i] = parts[0].trim();
						if(i == 0) { icon = parts[1].trim(); }
						
						if (className[i] == null) { className[i] = parts[2].trim(); }
						
						if(i == 0) 
						{ 
							suitename = name[i];
							suitename = suitename.replace(":","");
						} 
						
					}
					else if(argLength == 2) // A comma is missing, MUST be between the midlet name and icon path, otherwise there's no way to fix here (manifest has to be edited manually)
					{
						String[] newParts = parts[0].split("/", 2); // Split ONLY at the first occurrence of "/"

						name[i] = newParts[0].trim();
						if(i == 0) { icon = "/" + newParts[1].trim(); }

						if (className[i] == null) { className[i] = parts[1].trim(); }

						if(i == 0) 
						{ 
							suitename = name[i];
							suitename = suitename.replace(":","");
						} 
					}

					vendorname = properties.get("MIDlet-Vendor");
					Mobile.log(Mobile.LOG_INFO, "Loading MIDlet: " + name[i] +" | Main Class: " + className[i]);

					reg = new Registry(className[i]);
				}
				else 
				{ 
					name[i] = null;
					className[i] = null;
				}
			}
		}
		else // So far i've only seen single iAppli DoJa
		{
			name[0] = properties.get("AppName");
			icon = properties.get("AppIcon");
			className[0] = properties.get("AppClass");

			suitename = name[0];
			suitename = suitename.replace(":","");

			vendorname = "Keitai-DoJa"; // Used to generate the basename for Scratchpad RMS copies

			Mobile.log(Mobile.LOG_INFO, "Loading I-Appli: " + name[0] +" | Main Class: " + className[0]);
			reg = new Registry(className[0]);
		}
	}


	public InputStream getResourceAsStream(String resource)
	{
		URL url;
		Mobile.log(Mobile.LOG_DEBUG, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Loading Resource: " + resource);

		if(resource.startsWith("/"))
		{
			resource = resource.substring(1);

			if(resource.startsWith("/"))
			{
				resource = resource.substring(1);
			}
		}

		try
		{
			url = findResource(resource);
			// Read all bytes, return ByteArrayInputStream //
			InputStream stream = url.openStream();

			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int count=0;
			byte[] data = new byte[4096];
			while (count!=-1)
			{
				count = stream.read(data);
				if(count!=-1) { buffer.write(data, 0, count); }
			}
			return new ByteArrayInputStream(buffer.toByteArray());
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + resource + " Not Found");
			return super.getResourceAsStream(resource);
		}
	}


	public URL getResource(String resource)
	{
		if(resource.startsWith("/"))
		{
			resource = resource.substring(1);
			if(resource.startsWith("/"))
			{
				resource = resource.substring(1);
			}
		}
		try
		{
			URL url = findResource(resource);
			return url;
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + resource + " Not Found");
			return super.getResource(resource);
		}
	}

	@Override
    public URL findResource(String name) {
        // First, try to find the resource with the original, case-sensitive name
        URL resource = super.findResource(name);
        if (resource != null) {
            return resource;
        }

        // For each URL, check if it is a JAR file and perform a case-insensitive search
        for (URL url : getURLs()) {
            resource = findResourceInJar(url, name);
            if (resource != null) {
                return resource;
            }
        }

        // If not found, return null
        return null;
    }

    private URL findResourceInJar(URL jarUrl, String resourceName) 
	{
		for (JarEntry entry : jarEntries)
		{
			String entryName = entry.getName();
			if (entryName.equalsIgnoreCase(resourceName)) 
			{				
				try 
				{
					URI jarEntryURI = new URI("jar:" + jarUrl.toExternalForm() + "!/" + entryName);
					return jarEntryURI.toURL(); 
				}
				catch(Exception e) { Mobile.log(Mobile.LOG_ERROR, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Couldn't load resource from jar: " + e.getMessage()); e.printStackTrace(); }
			}
		}

		if (resourceName.contains(System.getProperty("microedition.locale")))
		{
			Mobile.log(Mobile.LOG_DEBUG, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Requested resource appears to be a language file. Checking for alternatives...");

			// Search for any language files matching the list of supported locales
			for (String locale : supportedLocales)
			{
				String fallbackResourceName = resourceName.replace(System.getProperty("microedition.locale"), locale);
				for (JarEntry entry : jarEntries)
				{
					String entryName = entry.getName();
					if (entryName.equalsIgnoreCase(fallbackResourceName))
					{
						try
						{
							URI jarEntryURI = new URI("jar:" + jarUrl.toExternalForm() + "!/" + entryName);
							return jarEntryURI.toURL();
						}
						catch (Exception e)
						{
							Mobile.log(Mobile.LOG_ERROR, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Couldn't load fallback resource from jar: " + e.getMessage());
							e.printStackTrace();
						}
					}
				}
			}
		}

		Mobile.log(Mobile.LOG_ERROR, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Couldn't find resource '" + resourceName + "' in jar: " + jarUrl);

        return null;
    }

	/*
		********  loadClass Modifies Methods with ObjectWeb ASM  ********
		Replaces java.lang.Class.getResourceAsStream calls with calls
		to Mobile.getResourceAsStream which calls
		MIDletLoader.getResourceAsStream(class, string)
	*/

	public InputStream getMIDletResourceAsStream(String resource)
	{
		Mobile.log(Mobile.LOG_DEBUG, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Get Resource As Stream: "+resource + " path:" + className[selectedMidlet]);

		boolean isSiemens = false;
		// Remove the "resource:" token that some jars pass into this method. FreeJ2ME doesn't need it.
		if(resource.contains("resource:")) 
		{ 
			resource = resource.replaceAll("resource:", "");
			if(!Mobile.isDoJa) { isSiemens = true;  }
		}

		// If the resource has more than one slash in sequence, remove all of them (the check below will correct it back to one slash)
		while (resource.startsWith("//")) { resource = resource.substring(1); }

		// Replace unsupported slashes
		resource = resource.replace("\\", "/");

		if(!resource.startsWith("/") && getResource(resource) == null) // Relative path, try to parse where the main class is in the jar, as the resource will be alongside it.
		{
			Mobile.log(Mobile.LOG_DEBUG, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Initial path returned null data. Treating as relative path...");
			// Change "." occurrences to "/" to give us the path to the class, and by consequence, the resource's position relative to it
			String resourcePath = className[selectedMidlet].replace(".", "/");
    
			// If we really are in a subdir
			if(resourcePath.contains("/") && !resource.contains(resourcePath)) 
			{
				// Remove the class name from the resolved path
				resourcePath = resourcePath.substring(0, resourcePath.lastIndexOf('/')) + "/"; 

				// And there we have it, just append the resource at the end of it, IF the resource doesn't already have it.
				if(!resource.startsWith(resourcePath)) { resource = resourcePath + resource; }
				else { resource = "/" + resource; }
			}
			else { resource = "/" + resource; } // If not, just append the directory slash
		}

		URL url = getResource(resource);

		// Read all bytes, return ByteArrayInputStream //
		try
		{
			InputStream stream = url.openStream();
			
			// zb3: why not return a stream? or a bufferedinputstream for marks?
			
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int count=0;
			byte[] data = new byte[4096];
			while (count!=-1)
			{
				count = stream.read(data);
				if(count!=-1) { buffer.write(data, 0, count); }
			}
			
			if(!isSiemens) { return new ByteArrayInputStream(buffer.toByteArray()); }
			else { return new SiemensInputStream(buffer.toByteArray()); }
		}
		catch (Exception e)
		{
			return super.getResourceAsStream(resource);
		}
	}

	public byte[] getMIDletResourceAsByteArray(String resource)
	{
		Mobile.log(Mobile.LOG_DEBUG, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Get Resource as Byte Array: "+resource);

		// Remove the "resource:" token that some jars pass into this method. FreeJ2ME doesn't need it.
		if(resource.contains("resource:")) { resource = resource.replaceAll("resource:", ""); }

		// If the resource has more than one slash in sequence, remove all of them (the check below will correct it back to one slash)
		while (resource.startsWith("//")) { resource = resource.substring(1); }

		// Replace unsupported slashes
		resource = resource.replace("\\", "/");

		if(!resource.startsWith("/") && getResource(resource) == null) // Relative path, try to parse where the main class is in the jar, as the resource will be alongside it.
		{
			Mobile.log(Mobile.LOG_DEBUG, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Initial path returned null data. Treating as relative path...");
			// Change "." occurrences to "/" to give us the path to the class, and by consequence, the resource's position relative to it
			String resourcePath = className[selectedMidlet].replace(".", "/");
    
			// If we really are in a subdir
			if(resourcePath.contains("/") && !resource.contains(resourcePath)) 
			{
				// Remove the class name from the resolved path
				resourcePath = resourcePath.substring(0, resourcePath.lastIndexOf('/')) + "/"; 

				// And there we have it, just append the resource at the end of it, IF the resource doesn't already have it.
				if(!resource.startsWith(resourcePath)) { resource = resourcePath + resource; }
				else { resource = "/" + resource; }
			}
			else { resource = "/" + resource; } // If not, just append the directory slash
		}

		URL url = getResource(resource);

		try
		{
			InputStream stream = url.openStream();

			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int count=0;
			byte[] data = new byte[4096];
			while (count!=-1)
			{
				count = stream.read(data);
				if(count!=-1) { buffer.write(data, 0, count); }
			}
			return buffer.toByteArray();
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + resource + " Not Found");
			return new byte[0];
		}
	}

	public byte[] getIAppliScratchPadAsByteArray(String resource)
	{
		Mobile.log(Mobile.LOG_DEBUG, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Get ScratchPad As Byte Array: "+ resource);

		// Remove the "resource:" token that some jars pass into this method. FreeJ2ME doesn't need it.
		if(resource.contains("scratchpad:")) { resource = resource.replaceAll("scratchpad:", ""); }

		// If the resource has more than one slash in sequence, remove all of them (the check below will correct it back to one slash)
		while (resource.startsWith("//")) { resource = resource.substring(1); }

		// Replace unsupported slashes
		resource = resource.replace("\\", "/");

		if(!resource.startsWith("/") && getResource(resource) == null) // Relative path, try to parse where the main class is in the jar, as the resource will be alongside it.
		{
			Mobile.log(Mobile.LOG_DEBUG, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Initial path returned no scratchpad data. Treating as relative path...");
			// Change "." occurrences to "/" to give us the path to the class, and by consequence, the resource's position relative to it
			String resourcePath = className[selectedMidlet].replace(".", "/");
    
			// If we really are in a subdir
			if(resourcePath.contains("/")) 
			{
				// Remove the class name from the resolved path
				resourcePath = resourcePath.substring(0, resourcePath.lastIndexOf('/')) + "/"; 
			
				// And there we have it, just append the resource at the end of it, IF the resource doesn't already have it.
				if(!resource.startsWith(resourcePath)) { resource = resourcePath + resource; }
				else { resource = "/" + resource; }
			}
			else { resource = "/" + resource; } // If not, just append the directory slash
		}

		// We basically ignore everything done above at the moment
		resource = baseUrl.toString().replace(".jar", ".sp");
		
		// TODO: Improve ScratchPad parsing

		// Read all bytes, return ByteArrayInputStream //
		try
		{
			InputStream stream = new FileInputStream(new File(new URI(resource)));
			
			// zb3: why not return a stream? or a bufferedinputstream for marks?
			
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int count=0;
			byte[] data = new byte[4096];
			while (count!=-1)
			{
				count = stream.read(data);
				if(count!=-1) { buffer.write(data, 0, count); }
			}
			return buffer.toByteArray();
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + resource + " Not Found");
			return new byte[0];
		}
	}


	public Class loadClass(String name) throws ClassNotFoundException
	{
		InputStream stream;
		String resource;
		byte[] code;

		Mobile.log(Mobile.LOG_DEBUG, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Load Class "+name);

		checkAPIUsage(name);

		// zb3: this needs to be improved as this won't transform games
		// like hypothetical com.nokia.tictactoe
		if(
			name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("com.nokia") ||
			name.startsWith("com.mascotcapsule") || name.startsWith("com.samsung") || name.startsWith("sun.") ||
			name.startsWith("com.siemens") || name.startsWith("org.recompile") || name.startsWith("jdk.") ||
			name.startsWith("com.vodafone.") || name.startsWith("com.jblend.") || name.startsWith("com.motorola.") ||
			name.startsWith("com.sprintpcs.") || name.startsWith("com.bmc.") || name.startsWith("com.immersion.") || 
			name.startsWith("com.j_phone.") || name.startsWith("com.kddi.") || name.startsWith("com.pantech.") ||
			name.startsWith("mmpp.") || name.startsWith("com.velox.") || name.startsWith("com.nttdocomo.") ||
			name.startsWith("org.xml.") || name.startsWith("org.w3c.") || name.startsWith("javacard.") ||
			name.startsWith("com.sonyericsson") || name.startsWith("com.xce.") || name.startsWith("com.skt.")
			)
		{

			// Change encoding based on vendor (Only DoJa, J_Phone and KDDI at the moment, MIDP already defaults to "ISO_8859_1")
			if(name.startsWith("com.kddi.") || name.startsWith("com.j_phone."))
			{
				Mobile.isKDDI = true;
				Mobile.textEncoding = "Shift_JIS";
				MobilePlatform.checkFileEncoding();
			}
			else if(name.startsWith("com.nttdocomo."))
			{
				Mobile.isDoJa = true;
				Mobile.textEncoding = "Shift_JIS";
				MobilePlatform.checkFileEncoding();
			}
			return loadClass(name, true);
		}

		try
		{
			Mobile.log(Mobile.LOG_DEBUG, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Instrumenting Class "+name);
			resource = name.replace(".", "/") + ".class";
			stream = super.getResourceAsStream(resource);
			code = instrument(stream);
			return defineClass(name, code, 0, code.length);
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Error Adapting Class "+name);
			Mobile.log(Mobile.LOG_ERROR, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + e.toString());
			return null;
		}

	}

	// TODO: This should be fleshed out to parse more classes that can affect the type of Connector returned, etc.
	public void checkAPIUsage(String name) 
	{
		// We need to discern these messaging packages in order to return valid "Connection" classes for SMS
		if (name.contains("javax.wireless.messaging")) { Mobile.usingMessagingAPI = true; }
	}

	// Used to build the RMS json, not much else
	public String getProperty(String key) 
	{ 
		if(properties.containsKey(key)) { return properties.get(key); }
		else { return ""; }
	}


/* **************************************************************
 * Special Siemens Stuff
 * ************************************************************** */

	private class SiemensInputStream extends InputStream
	{
		private ByteArrayInputStream iostream;

		public SiemensInputStream(byte[] data)
		{
			iostream = new ByteArrayInputStream(data);
		}

		public int read()
		{
			int t = iostream.read();
			if (t == -1) { return 0; }
			return t;
		}
		public int read(byte[] b, int off, int len)
		{
			int t = iostream.read(b, off, len);
			if (t == -1) { return 0; }
			return t;
		}
	}


/* ************************************************************** 
 * Instrumentation
 * ************************************************************** */

	private byte[] instrument(InputStream stream) throws Exception
	{
		ClassReader reader = new ClassReader(stream);
		ClassWriter writer = new ClassWriter(0);
		ClassVisitor visitor = new ASMVisitor(writer);
		reader.accept(visitor, ClassReader.SKIP_DEBUG);
		return writer.toByteArray();
	}

	private class ASMVisitor extends ClassAdapter
	{
		private String superName;

		public ASMVisitor(ClassVisitor visitor)
		{
			super(visitor);
		}

		public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces)
		{
			this.superName = superName;

			super.visit(version, access, name, signature, superName, interfaces);
		}

		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) 
		{
			return super.visitField(access, name, desc, signature, value);
		}

		public MethodVisitor visitMethod(int access, String name, final String desc, final String signature, final String[] exceptions)
		{

			// Override invalid Thread methods
			if ("java/lang/Thread".equals(superName))
			{ 
				if("suspend".equals(name) || "stop".equals(name) || "resume".equals(name)) 
				{
					Mobile.log(Mobile.LOG_DEBUG, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "MIDlet tried to override Java's Thread method: " + name + "... patched!");
					name = "_" + name; 
				}
			}

			// Try bypassing ALW1 wrapper
			if (desc.equals("()V") && name.equals("startRealApp") && access == Opcodes.ACC_PRIVATE) 
			{
				Mobile.log(Mobile.LOG_WARNING, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "MIDlet uses an ALW1 ad/demo wrapper... trying to patch...");
				access = Opcodes.ACC_PUBLIC;
			}

			MethodVisitor visitor = super.visitMethod(access, name, desc, signature, exceptions);

			if ((access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC &&
					"(Ljavax/microedition/midlet/MIDlet;)Z".equals(desc)
			) {
				// TODO: better heuristic?
				// SKT security check bypass
				visitor = new ASMSecureUtilWorkaroundMethodVisitor(visitor);
			} else {
				visitor = new ASMMethodVisitor(visitor);
			}
			
			return visitor;
		}

		private class ASMSecureUtilWorkaroundMethodVisitor extends MethodAdapter {
			private final MethodVisitor target;

			public ASMSecureUtilWorkaroundMethodVisitor(MethodVisitor target) {
                super(target);

				this.target = target;
			}

			@Override
			public void visitCode() {
				target.visitCode();
				target.visitInsn(Opcodes.ICONST_1);
				target.visitInsn(Opcodes.IRETURN);
				target.visitMaxs(1, 0);
				target.visitEnd();
			}
		}

		private class ASMMethodVisitor extends MethodAdapter implements Opcodes
		{
			private boolean methodHasScreenDraw = false, foundPlatformCheck = false;

			public ASMMethodVisitor(MethodVisitor visitor)
			{
				super(visitor);
			}

			public void visitMethodInsn(int opcode, String owner, String name, String desc)
			{
				if (opcode == Opcodes.INVOKEVIRTUAL && ("repaint".equals(name) || "serviceRepaints".equals(name)) || "flushGraphics".equals(name)) { methodHasScreenDraw = true; }

				// This one overrides Thread.sleep calls with a call to the "MIDletEnhancements" class, which allows nullifying all sleeps (Unlock FPS hack)
				if (opcode == Opcodes.INVOKESTATIC && "java/lang/Thread".equals(owner) && "sleep".equals(name) && "(J)V".equals(desc)) 
				{
					if(methodHasScreenDraw) { visitMethodInsn(Opcodes.INVOKESTATIC, "org/recompile/mobile/MIDletEnhancements", "drawSleep", "(J)V"); } // Safer sleep override
					else { visitMethodInsn(Opcodes.INVOKESTATIC, "org/recompile/mobile/MIDletEnhancements", "sleep", "(J)V"); } // Extended sleep override, more useful for fast-forwarding
				}
				else if (opcode == Opcodes.INVOKESTATIC && "java/lang/System".equals(owner) && "currentTimeMillis".equals(name))
				{
					// More agressive unlock FPS hack, but mostly useful for "Fast-Forward"
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/recompile/mobile/MIDletEnhancements", "currentTimeMillis", "()J");
				}
				else if (opcode == Opcodes.INVOKESTATIC && "java/lang/System".equals(owner) && "nanoTime".equals(name)) 
				{
					// Same as currentTimeMillis override above
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/recompile/mobile/MIDletEnhancements", "nanoTime", "()J");
				}
				else if(opcode == Opcodes.INVOKESTATIC && "java/lang/Thread".equals(owner) && "yield".equals(name) && "()V".equals(desc)) 
				{
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/recompile/mobile/MIDletEnhancements", "yieldOverride", "()V");
				}
				else if (opcode == Opcodes.INVOKESTATIC && "java/lang/System".equals(owner) && "gc".equals(name)) // Ignore System.gc() calls
				{
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/recompile/mobile/MIDletEnhancements", "noGC", "()V");
				}
				else if (opcode == INVOKEVIRTUAL && name.equals("getResourceAsStream") && owner.equals("java/lang/Class"))
				{
					mv.visitMethodInsn(INVOKESTATIC, "org/recompile/mobile/Mobile", name, "(Ljava/lang/Class;Ljava/lang/String;)Ljava/io/InputStream;");
				}
				else
				{
					mv.visitMethodInsn(opcode, owner, name, desc);
				}
			}

			public void visitFieldInsn(int opcode, String owner, String name, String desc) 
			{
				super.visitFieldInsn(opcode, owner, name, desc);
			}

			public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) 
			{
				super.visitLocalVariable(name, desc, signature, start, end, index);
			}

			@Override
			public void visitLdcInsn(Object value)
			{
				// The loaded app might be going for a check against a specific phone model, prepare to override it
				if (value instanceof String && ((String)value).toLowerCase().contains("microedition.platform"))
				{
					if(Mobile.compatOverridePlatformChecks) { foundPlatformCheck = true; }
				}

				if (foundPlatformCheck && value instanceof String)
				{
					for (String keyword : knownModelStrings)
					{
						if (((String)value).toLowerCase().contains(keyword)) // It is going for the check, replace the device string with FreeJ2ME-Plus'
						{
							Mobile.log(Mobile.LOG_WARNING, MIDletLoader.class.getPackage().getName() + "." + MIDletLoader.class.getSimpleName() + ": " + "Found explicit platform '" + value +  "' model check... overriding.");
							String replacementModel = "FreeJ2ME-Plus, a Cross-Platform J2ME Emulator.";
							value = replacementModel.substring(0, Math.min(((String)value).length(), replacementModel.length()));
							break;
						}
					}
				}
				super.visitLdcInsn(value);
			}

			private static final boolean ENABLE_EXCEPTION_DEBUG = false;
			private final HashSet<Label> catchLabels = new HashSet<>();

			@Override
			public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
				super.visitTryCatchBlock(start, end, handler, type);

				if (ENABLE_EXCEPTION_DEBUG) {
					catchLabels.add(handler);
				}
			}

			@Override
			public void visitLabel(Label label) {
				super.visitLabel(label);

				if (ENABLE_EXCEPTION_DEBUG) {
					if (catchLabels.contains(label)) {
						super.visitInsn(Opcodes.DUP);
						super.visitMethodInsn(
								Opcodes.INVOKEVIRTUAL,
								"java/lang/Throwable",
								"printStackTrace",
								"()V"
						);
					}
				}
			}
		}
	}


	/* ************************************************************** 
	* Multi-Midlet selection menu
	* ************************************************************** */
	public static void keyPress(int key)
	{

		if (key == Canvas.UP || key == Canvas.KEY_NUM2) 
		{ 
			selectedMidlet--;
			if(selectedMidlet < 0) { selectedMidlet++; }
		} 
		else if (key == Canvas.DOWN || key == Canvas.KEY_NUM8) 
		{ 
			selectedMidlet++;
			if(selectedMidlet > name.length || name[selectedMidlet] == null) { selectedMidlet--; }
		}
		else if (key == Canvas.FIRE || key == Canvas.KEY_NUM5) { MIDletSelected = true; }
	}

	protected void render() 
	{
		int numMidlets = 0;
		
		for(int i = 0; i < name.length; i++) 
		{
			if(name[i] != null) { numMidlets++; } // Check how many actual midlets we currently have
		}

		graphics.setFont(Font.getDefaultFont());
	
		// Draw Background
		graphics.setColor(Mobile.lcduiBGColor);
		graphics.fillRect(0, 0, Mobile.lcdWidth, Mobile.lcdHeight);
		graphics.setColor(Mobile.lcduiTextColor);
	
		// Render top bar with the indicator text
		String currentTitle = "Select the MIDlet to run";
		int titlePadding = Font.fontPadding[Font.screenType];
		int titleHeight = Font.getDefaultFont().getHeight() + titlePadding;
		graphics.drawString(currentTitle, Mobile.lcdWidth / 2, 0, Graphics.HCENTER);
		graphics.drawLine(0, titleHeight, Mobile.lcdWidth, titleHeight);
	
		// Render bottom bar with the function hint
		int bottomBarHeight = titleHeight - titlePadding;
		graphics.drawLine(0, Mobile.lcdHeight - bottomBarHeight + titlePadding, Mobile.lcdWidth, Mobile.lcdHeight - bottomBarHeight + titlePadding);
		graphics.drawString("5/OK = Sel.|v^ = Move", Mobile.lcdWidth / 2, Mobile.lcdHeight + (2 * titlePadding) - titleHeight, Graphics.HCENTER);
	
		// Render items in the middle
		int currentY = titleHeight + (2 * titlePadding);
		int itemHeight = Font.getDefaultFont().getHeight() - titlePadding;
	
		// Calculate the number of visible items
		int visibleItems = (Mobile.lcdHeight - titleHeight - (2 * titlePadding) - bottomBarHeight) / itemHeight;
		int firstVisibleItem = Math.max(0, selectedMidlet - visibleItems / 2);
		int lastVisibleItem = Math.min(numMidlets - 1, firstVisibleItem + visibleItems - 1);
	
		// Render MIDlet items
		for (int i = firstVisibleItem; i <= lastVisibleItem; i++) 
		{
			if (name[i] != null) 
			{
				// Highlight the selected MIDlet
				if (i == selectedMidlet) 
				{
					graphics.setColor(Mobile.lcduiTextColor);
					graphics.fillRect(0, currentY, Mobile.lcdWidth, itemHeight);
					graphics.setColor(Mobile.lcduiBGColor);
				} 
				else { graphics.setColor(Mobile.lcduiTextColor); }
	
				graphics.drawString(name[i], Mobile.lcdWidth / 2, currentY, Graphics.HCENTER);
				currentY += itemHeight;
			}
		}
	
		// Draw scrollbar if necessary
		if (numMidlets > visibleItems) 
		{
			int scrollbarWidth = 3+titlePadding;
			int scrollableHeight = Mobile.lcdHeight - titleHeight - bottomBarHeight;
			int scrollbarHeight = (int) ((double) visibleItems / numMidlets * scrollableHeight);
			int scrollbarY = (int) (((double) firstVisibleItem / (numMidlets - visibleItems)) * (scrollableHeight - scrollbarHeight));
	
			// Ensure scrollbar is within the scrollable area's bounds
			scrollbarY = Math.min(scrollbarY, scrollableHeight - scrollbarHeight);
	
			graphics.setColor(Mobile.lcduiStrokeColor); // Scrollbar color
			graphics.fillRect(Mobile.lcdWidth - scrollbarWidth, titleHeight + (2 * titlePadding) + scrollbarY, scrollbarWidth, scrollbarHeight);
		}
	
		Mobile.getPlatform().flushGraphics(platformImage, 0, 0, Mobile.lcdWidth, Mobile.lcdHeight);
	}
}
