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

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FilenameFilter;

import java.util.Arrays;

public final class AWTGUI 
{
	/* This is used to indicate to FreeJ2ME that it has to call "settingsChanged()" to apply changes made here */
	private boolean hasPendingChange;

	/* Indicates whether a jar file was loaded successfully */
	private boolean fileLoaded = false;
	private boolean firstLoad = false;

	/* String that points to the jar file that has to be loaded */
	String jarfile = "";

	/* This is meant to be a local reference of FreeJ2ME's main frame */
	private Frame main;

	/* And this is meant to be a local reference of FreeJ2ME's config */
	private Config config;

	/* AWT's main MenuBar */
	final MenuBar menuBar = new MenuBar();

	/* MenuBar's menus */
	Menu fileMenu = new Menu("File");
	Menu optionMenu = new Menu("Settings"); 
	Menu debugMenu = new Menu("Debug");

	/* Sub menus (for now, all of them are located in "Settings") */
	final Menu fpsCap = new Menu("FPS Limit");
	final Menu phoneType = new Menu("Phone Key Layout");
	final Menu midiStreamNum = new Menu("Max MIDI Streams");

	/* Dialogs for resolution changes, restart notifications, MemStats and info about FreeJ2ME */
	final Dialog resDialog = new Dialog(main , "Set LCD Resolution", true);
	final Dialog aboutDialog = new Dialog(main , "About FreeJ2ME", true);
	final Dialog memStatDialog = new Dialog(main, "FreeJ2ME MemStat", false);
	final Dialog restartRequiredDialog = new Dialog(main, "Restart Required", true);

	final Button closeAbout = new Button("Close");
	final Button applyResChange = new Button("Apply");
	final Button cancelResChange = new Button("Cancel");
	final Button closeNow = new Button("Close FreeJ2ME");
	final Button restartLater = new Button("Restart later");

	final Choice resChoice = new Choice();
	final String[] supportedRes = new String[] {"96x65","96x96","104x80","128x128","132x176","128x160","176x208","176x220", 
		"208x208", "240x320", "320x240", "240x400", "352x416", "360x640", "640x360" ,"480x800", "800x480"};

	Label totalMemLabel = new Label("Total Mem: 000000000 KB");
	Label freeMemLabel = new Label("Free Mem : 000000000 KB");
	Label usedMemLabel = new Label("Used Mem : 000000000 KB");
	Label maxMemLabel = new Label("Max Mem  : 000000000 KB");

	/* Items for each of the bar's menus */
	final UIListener menuItemListener = new UIListener();

	final MenuItem aboutMenuItem = new MenuItem("About FreeJ2ME");
	final MenuItem resChangeMenuItem = new MenuItem("Change Phone Resolution");

	final MenuItem openMenuItem = new MenuItem("Open Jar");
	final MenuItem closeMenuItem = new MenuItem("Close Jar (Stub)");
	final MenuItem scrShot = new MenuItem("Take Screenshot");
	final MenuItem exitMenuItem = new MenuItem("Exit FreeJ2ME");

	final CheckboxMenuItem enableAudio = new CheckboxMenuItem("Enable Audio", false);
	final CheckboxMenuItem enableRotation = new CheckboxMenuItem("Rotate Screen", false);
	final CheckboxMenuItem useCustomMidi = new CheckboxMenuItem("Use custom midi soundfont", false);

	final CheckboxMenuItem stdLayout = new CheckboxMenuItem("Standard", true);
	final CheckboxMenuItem nokiaLayout = new CheckboxMenuItem("Nokia", false);
	final CheckboxMenuItem siemensLayout = new CheckboxMenuItem("Siemens", false);
	final CheckboxMenuItem motorolaLayout = new CheckboxMenuItem("Motorola", false);

	final CheckboxMenuItem fpsCapNone = new CheckboxMenuItem("No Limit", true);
	final CheckboxMenuItem fpsCap60 = new CheckboxMenuItem("60 FPS", false);
	final CheckboxMenuItem fpsCap30 = new CheckboxMenuItem("30 FPS", false);
	final CheckboxMenuItem fpsCap15 = new CheckboxMenuItem("15 FPS", false);

	final CheckboxMenuItem midiStreams[] = new CheckboxMenuItem[] {new CheckboxMenuItem("1 Slot", false), new CheckboxMenuItem("2 Slots", false), 
		new CheckboxMenuItem("4 Slots", false), new CheckboxMenuItem("8 Slots", false), new CheckboxMenuItem("16 Slots", false), 
		new CheckboxMenuItem("32 Slots", false), new CheckboxMenuItem("48 Slots", false), new CheckboxMenuItem("64 Slots", false),
		new CheckboxMenuItem("96 Slots", false)};
		
	final byte[] numPlayers = new byte[] {1, 2, 4 ,8, 16, 32, 48, 64, 96}; /* Used to simplify the UpdateOptions() method below */

	final CheckboxMenuItem dumpAudioData = new CheckboxMenuItem("Dump Audio Streams");
	final CheckboxMenuItem dumpGraphicsData = new CheckboxMenuItem("Dump Graphics Objects");
	final CheckboxMenuItem showMemoryUsage = new CheckboxMenuItem("Show VM Memory Usage");
	 

	public AWTGUI(Config config)
	{
		this.config = config;

		resChoice.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
		totalMemLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
		freeMemLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
		usedMemLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
		maxMemLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));

		closeAbout.setBackground(Color.green);
		applyResChange.setBackground(Color.green);
		cancelResChange.setBackground(Color.yellow);
		closeNow.setBackground(Color.green);
		restartLater.setBackground(Color.yellow);

		aboutDialog.setBackground(Color.white);
		aboutDialog.setLayout( new FlowLayout(FlowLayout.CENTER, 200, 0));  
		aboutDialog.setUndecorated(true); /* Whenever a Dialog is undecorated, it's because it's meant to look like an internal menu on FreeJ2ME's main Frame */
		aboutDialog.setSize(230, 175);
		aboutDialog.setResizable(false);
		aboutDialog.setLocationRelativeTo(main);
		aboutDialog.add(new Label("FreeJ2ME - A free J2ME emulator"));
		aboutDialog.add(new Label("--------------------------------"));
		aboutDialog.add(new Label("Project Authors:"));
		aboutDialog.add(new Label("David Richardson (Recompile)"));
		aboutDialog.add(new Label("Saket Dandawate (hex007)"));
		aboutDialog.add(closeAbout);

		resDialog.setBackground(Color.white);
		resDialog.setLayout( new FlowLayout(FlowLayout.CENTER, 60, 5));
		resDialog.setUndecorated(true);
		resDialog.setSize(230, 175);
		resDialog.setResizable(false);
		resDialog.setLocationRelativeTo(main);
		resDialog.add(new Label("Select a Resolution from the Dropdown"));
		resDialog.add(new Label("Then hit 'Apply'!"));
		resDialog.add(resChoice);
		resDialog.add(applyResChange);
		resDialog.add(cancelResChange);


		memStatDialog.setBackground(Color.white);
		memStatDialog.setLayout( new FlowLayout(FlowLayout.LEFT, 5, 0));  
		memStatDialog.setSize(240, 145);
		memStatDialog.setResizable(false);
		memStatDialog.add(totalMemLabel);
		memStatDialog.add(freeMemLabel);
		memStatDialog.add(usedMemLabel);
		memStatDialog.add(maxMemLabel);

		restartRequiredDialog.setBackground(Color.white);
		restartRequiredDialog.setLayout( new FlowLayout(FlowLayout.CENTER, 10, 10));  
		restartRequiredDialog.setUndecorated(true);
		restartRequiredDialog.setSize(230, 175);
		restartRequiredDialog.setLocationRelativeTo(main);
		restartRequiredDialog.add(new Label("This change requires a restart to apply!"));
		restartRequiredDialog.add(closeNow);
		restartRequiredDialog.add(restartLater);
		
		openMenuItem.setActionCommand("Open");
		closeMenuItem.setActionCommand("Close");
		scrShot.setActionCommand("Screenshot");
		exitMenuItem.setActionCommand("Exit");
		aboutMenuItem.setActionCommand("AboutMenu");
		resChangeMenuItem.setActionCommand("ChangeResolution");
		applyResChange.setActionCommand("ApplyResChange");
		cancelResChange.setActionCommand("CancelResChange");
		closeAbout.setActionCommand("CloseAboutMenu");
		closeNow.setActionCommand("CloseFreeJ2ME");
		restartLater.setActionCommand("RestartLater");

		openMenuItem.addActionListener(menuItemListener);
		closeMenuItem.addActionListener(menuItemListener);
		scrShot.addActionListener(menuItemListener);
		exitMenuItem.addActionListener(menuItemListener);
		aboutMenuItem.addActionListener(menuItemListener);
		resChangeMenuItem.addActionListener(menuItemListener);
		applyResChange.addActionListener(menuItemListener);
		cancelResChange.addActionListener(menuItemListener);
		closeAbout.addActionListener(menuItemListener);
		closeNow.addActionListener(menuItemListener);
		restartLater.addActionListener(menuItemListener);

		setActionListeners();

		buildMenuBar();
	}

	private void setActionListeners() 
	{
		enableAudio.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(enableAudio.getState()){ config.updateSound("on"); hasPendingChange = true; }
				else{ config.updateSound("off"); hasPendingChange = true; }
			}
		});

		enableRotation.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(enableRotation.getState()){ config.updateRotate("on"); hasPendingChange = true; }
				else{ config.updateRotate("off"); hasPendingChange = true; }
			}
		});

		useCustomMidi.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(useCustomMidi.getState()){ config.updateSoundfont("on"); hasPendingChange = true; }
				else{ config.updateSoundfont("off"); hasPendingChange = true; }

				restartRequiredDialog.setLocationRelativeTo(main);
				restartRequiredDialog.setVisible(true);
			}
		});

		midiStreams[0].addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(midiStreams[0].getState())
				{
					config.updateMIDIStreams(""+numPlayers[0]); 
					hasPendingChange = true;

					// Uncheck all other checkboxes for midi players
					for(int i = 0; i < midiStreams.length; i++)
					{
						if(midiStreams[i].equals(midiStreams[0])) { continue; }
						midiStreams[i].setState(false);
					}
				
				}

				restartRequiredDialog.setLocationRelativeTo(main);
				restartRequiredDialog.setVisible(true);
			}
		});

		midiStreams[1].addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(midiStreams[1].getState())
				{
					config.updateMIDIStreams(""+numPlayers[1]); 
					hasPendingChange = true;

					// Uncheck all other checkboxes for midi players
					for(int i = 0; i < midiStreams.length; i++)
					{
						if(midiStreams[i].equals(midiStreams[1])) { continue; }
						midiStreams[i].setState(false);
					}
				
				}

				restartRequiredDialog.setLocationRelativeTo(main);
				restartRequiredDialog.setVisible(true);
			}
		});

		midiStreams[2].addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(midiStreams[2].getState())
				{
					config.updateMIDIStreams(""+numPlayers[2]); 
					hasPendingChange = true;

					// Uncheck all other checkboxes for midi players
					for(int i = 0; i < midiStreams.length; i++)
					{
						if(midiStreams[i].equals(midiStreams[2])) { continue; }
						midiStreams[i].setState(false);
					}
				
				}

				restartRequiredDialog.setLocationRelativeTo(main);
				restartRequiredDialog.setVisible(true);
			}
		});

		midiStreams[3].addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(midiStreams[3].getState())
				{
					config.updateMIDIStreams(""+numPlayers[3]); 
					hasPendingChange = true;

					// Uncheck all other checkboxes for midi players
					for(int i = 0; i < midiStreams.length; i++)
					{
						if(midiStreams[i].equals(midiStreams[3])) { continue; }
						midiStreams[i].setState(false);
					}
				
				}

				restartRequiredDialog.setLocationRelativeTo(main);
				restartRequiredDialog.setVisible(true);
			}
		});

		midiStreams[4].addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(midiStreams[4].getState())
				{
					config.updateMIDIStreams(""+numPlayers[4]); 
					hasPendingChange = true;

					// Uncheck all other checkboxes for midi players
					for(int i = 0; i < midiStreams.length; i++)
					{
						if(midiStreams[i].equals(midiStreams[4])) { continue; }
						midiStreams[i].setState(false);
					}
				
				}

				restartRequiredDialog.setLocationRelativeTo(main);
				restartRequiredDialog.setVisible(true);
			}
		});

		midiStreams[5].addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(midiStreams[5].getState())
				{
					config.updateMIDIStreams(""+numPlayers[5]); 
					hasPendingChange = true;

					// Uncheck all other checkboxes for midi players
					for(int i = 0; i < midiStreams.length; i++)
					{
						if(midiStreams[i].equals(midiStreams[5])) { continue; }
						midiStreams[i].setState(false);
					}
				
				}

				restartRequiredDialog.setLocationRelativeTo(main);
				restartRequiredDialog.setVisible(true);
			}
		});

		midiStreams[6].addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(midiStreams[6].getState())
				{
					config.updateMIDIStreams(""+numPlayers[6]); 
					hasPendingChange = true;

					// Uncheck all other checkboxes for midi players
					for(int i = 0; i < midiStreams.length; i++)
					{
						if(midiStreams[i].equals(midiStreams[6])) { continue; }
						midiStreams[i].setState(false);
					}
				
				}

				restartRequiredDialog.setLocationRelativeTo(main);
				restartRequiredDialog.setVisible(true);
			}
		});

		midiStreams[7].addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(midiStreams[7].getState())
				{
					config.updateMIDIStreams(""+numPlayers[7]); 
					hasPendingChange = true;

					// Uncheck all other checkboxes for midi players
					for(int i = 0; i < midiStreams.length; i++)
					{
						if(midiStreams[i].equals(midiStreams[7])) { continue; }
						midiStreams[i].setState(false);
					}
				
				}

				restartRequiredDialog.setLocationRelativeTo(main);
				restartRequiredDialog.setVisible(true);
			}
		});

		midiStreams[8].addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(midiStreams[8].getState())
				{
					config.updateMIDIStreams(""+numPlayers[8]); 
					hasPendingChange = true;

					// Uncheck all other checkboxes for midi players
					for(int i = 0; i < midiStreams.length; i++)
					{
						if(midiStreams[i].equals(midiStreams[8])) { continue; }
						midiStreams[i].setState(false);
					}
				
				}

				restartRequiredDialog.setLocationRelativeTo(main);
				restartRequiredDialog.setVisible(true);
			}
		});


		stdLayout.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(!stdLayout.getState()){ stdLayout.setState(true); }
				if(stdLayout.getState())
				{ 
					config.updatePhone("Standard");
					nokiaLayout.setState(false);
					siemensLayout.setState(false);
					motorolaLayout.setState(false);
					hasPendingChange = true;
				}
			}
		});

		nokiaLayout.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(!nokiaLayout.getState()){ nokiaLayout.setState(true); }
				if(nokiaLayout.getState())
				{ 
					config.updatePhone("Nokia");
					stdLayout.setState(false);
					siemensLayout.setState(false);
					motorolaLayout.setState(false);
					hasPendingChange = true;
				}
			}
		});
		
		siemensLayout.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(!siemensLayout.getState()){ siemensLayout.setState(true); }
				if(siemensLayout.getState())
				{ 
					config.updatePhone("Siemens");
					stdLayout.setState(false);
					nokiaLayout.setState(false);
					motorolaLayout.setState(false);
					hasPendingChange = true;
				}
			}
		});

		motorolaLayout.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(!motorolaLayout.getState()){ motorolaLayout.setState(true); }
				if(motorolaLayout.getState())
				{ 
					config.updatePhone("Motorola");
					stdLayout.setState(false);
					nokiaLayout.setState(false);
					siemensLayout.setState(false);
					hasPendingChange = true;
				}
			}
		});


		fpsCapNone.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(!fpsCapNone.getState()){ fpsCapNone.setState(true); }
				if(fpsCapNone.getState())
				{ 
					config.updateFPS("0"); 
					fpsCap60.setState(false);
					fpsCap30.setState(false);
					fpsCap15.setState(false);
					hasPendingChange = true;
				}
			}
		});

		fpsCap60.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(!fpsCap60.getState()){ fpsCap60.setState(true); }
				if(fpsCap60.getState())
				{ 
					config.updateFPS("60"); 
					fpsCapNone.setState(false);
					fpsCap30.setState(false);
					fpsCap15.setState(false);
					hasPendingChange = true;
				}
			}
		});

		fpsCap30.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(!fpsCap30.getState()){ fpsCap30.setState(true); }
				if(fpsCap30.getState())
				{ 
					config.updateFPS("30"); 
					fpsCapNone.setState(false);
					fpsCap60.setState(false);
					fpsCap15.setState(false);
					hasPendingChange = true;
				}
			}
		});

		fpsCap15.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(!fpsCap15.getState()){ fpsCap15.setState(true); }
				if(fpsCap15.getState())
				{ 
					config.updateFPS("15"); 
					fpsCapNone.setState(false);
					fpsCap60.setState(false);
					fpsCap30.setState(false);
					hasPendingChange = true;
				}
			}
		});


		dumpAudioData.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(dumpAudioData.getState()){ /* TODO */ }
				else{ /* TODO */ }
			}
		});

		dumpGraphicsData.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(dumpGraphicsData.getState()){ /* TODO */ }
				else{ /* TODO */ }
			}
		});

		showMemoryUsage.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				/* Mem stats frame won't be centered on FreeJ2ME's frame, instead, it will sit right by its side, that's why "setLocationRelativeTo(main)" isn't used */
				if(showMemoryUsage.getState()){ memStatDialog.setLocation(main.getLocation().x+main.getSize().width, main.getLocation().y); memStatDialog.setVisible(true); }
				else{ memStatDialog.setVisible(false); }
			}
		});
	}

	private void buildMenuBar() 
	{
		//add menu items to menus
		fileMenu.add(openMenuItem);
		fileMenu.add(closeMenuItem);
		fileMenu.addSeparator();
		fileMenu.add(scrShot);
		fileMenu.addSeparator();
		fileMenu.add(aboutMenuItem);
		fileMenu.add(exitMenuItem);

		optionMenu.add(enableAudio);
		optionMenu.add(enableRotation);
		optionMenu.add(useCustomMidi);
		optionMenu.add(resChangeMenuItem);
		optionMenu.add(phoneType);
		optionMenu.add(fpsCap);
		optionMenu.add(midiStreamNum);

		debugMenu.add(dumpAudioData);
		debugMenu.add(dumpGraphicsData);
		debugMenu.add(showMemoryUsage);

		for(int i = 0; i < supportedRes.length; i++) { resChoice.add(supportedRes[i]); }

		phoneType.add(stdLayout);
		phoneType.add(nokiaLayout);
		phoneType.add(siemensLayout);
		phoneType.add(motorolaLayout);

		fpsCap.add(fpsCapNone);
		fpsCap.add(fpsCap60);
		fpsCap.add(fpsCap30);
		fpsCap.add(fpsCap15);

		for(int i = 0; i < midiStreams.length; i++) { midiStreamNum.add(midiStreams[i]); }
		
		// add menus to menubar
		menuBar.add(fileMenu);
		menuBar.add(optionMenu);
		menuBar.add(debugMenu);
	}

	public void updateOptions() 
	{
			enableAudio.setState(config.settings.get("sound").equals("on"));
			enableRotation.setState(config.settings.get("rotate").equals("on"));
			useCustomMidi.setState(config.settings.get("soundfont").equals("on"));
			fpsCapNone.setState(config.settings.get("fps").equals("0"));
			fpsCap15.setState(config.settings.get("fps").equals("15"));
			fpsCap30.setState(config.settings.get("fps").equals("30"));
			fpsCap60.setState(config.settings.get("fps").equals("60"));

			stdLayout.setState(config.settings.get("phone").equals("Standard"));
			nokiaLayout.setState(config.settings.get("phone").equals("Nokia"));
			siemensLayout.setState(config.settings.get("phone").equals("Siemens"));
			motorolaLayout.setState(config.settings.get("phone").equals("Motorola"));

			resChoice.select(""+ Integer.parseInt(config.settings.get("width")) + "x" + ""+ Integer.parseInt(config.settings.get("height")));
			
			for(int i = 0; i < midiStreams.length; i++) 
			{
				midiStreams[i].setState(config.settings.get("maxmidistreams").equals(""+numPlayers[i]));
			}

			/* We only need to do this call once, when the jar first loads */
			firstLoad = false;
	}

	public void updateMemStatDialog() 
	{
		totalMemLabel.setText(new String("Total Mem: " + (Runtime.getRuntime().totalMemory() / 1024) + " KB"));
		freeMemLabel.setText(new String("Free Mem : " + (Runtime.getRuntime().freeMemory() / 1024) + " KB"));
		usedMemLabel.setText(new String("Used Mem : " + ((Runtime.getRuntime().totalMemory() / 1024) - (Runtime.getRuntime().freeMemory() / 1024)) + " KB"));
		maxMemLabel.setText(new String("Max Mem  : " + (Runtime.getRuntime().maxMemory() / 1024) + " KB"));
	}

	class UIListener implements ActionListener {
		public void actionPerformed(ActionEvent a) {            

			if(a.getActionCommand() == "Open") {
				FileDialog filePicker = new FileDialog(main, "Open JAR File", FileDialog.LOAD);
				String filename;
				filePicker.setFilenameFilter(new FilenameFilter()
				{
					public boolean accept(File dir, String name) 
					{ return name.toLowerCase().endsWith(".jar"); }
				});
				filePicker.setVisible(true);

				filename = filePicker.getFile();
				jarfile = new File(filePicker.getDirectory()+File.separator+filePicker.getFile()).toURI().toString();
				if(filename == null) { System.out.println("JAR Loading was cancelled"); }
				else
				{
					fileLoaded = true;
					firstLoad = true;
				}
			}

			else if(a.getActionCommand() == "Close") 
			{
				try
				{
					/* TODO: Try closing the loaded jar without closing FreeJ2ME */
				}
				catch (Throwable e) { System.out.println("Couldn't close jar"); }
			}

			else if(a.getActionCommand() == "Screenshot") { ScreenShot.takeScreenshot(false); }

			else if(a.getActionCommand() == "Exit") { System.exit(0); }

			else if(a.getActionCommand() == "AboutMenu") { aboutDialog.setLocationRelativeTo(main); aboutDialog.setVisible(true); }

			else if(a.getActionCommand() == "CloseAboutMenu") { aboutDialog.setVisible(false); }

			else if(a.getActionCommand() == "ChangeResolution") { resDialog.setLocationRelativeTo(main); resDialog.setVisible(true); }

			else if(a.getActionCommand() == "ApplyResChange") 
			{
				if(fileLoaded) /* Only update res if a jar was loaded, or else AWT throws NullPointerException */
				{
					String[] res = resChoice.getItem(resChoice.getSelectedIndex()).split("x");

					config.updateDisplaySize(Integer.parseInt(res[0]), Integer.parseInt(res[1]));
					hasPendingChange = true;
				}
				resDialog.setVisible(false);
				restartRequiredDialog.setLocationRelativeTo(main);
				restartRequiredDialog.setVisible(true);
			}

			else if (a.getActionCommand() == "CancelResChange") { resDialog.setVisible(false); }

			else if(a.getActionCommand() == "CloseFreeJ2ME") { System.exit(0); }

			else if(a.getActionCommand() == "RestartLater") { restartRequiredDialog.setVisible(false); }
		}
	}

	public MenuBar getMenuBar() { return menuBar; }

	public boolean hasChanged() { return hasPendingChange; }

	public void clearChanged() { hasPendingChange = false; }

	public boolean hasLoadedFile() { return fileLoaded; }

	public void setMainFrame(Frame main) { this.main = main; }

	public void setJarPath(String path) { jarfile = path;}

	public String getJarPath() { return jarfile; }

	public boolean hasJustLoaded() { return firstLoad; }
}
