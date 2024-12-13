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

import javax.microedition.media.Manager;

import org.recompile.mobile.Mobile;

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
	Menu speedHackMenu = new Menu("SpeedHacks"); 
	Menu debugMenu = new Menu("Debug");

	/* Sub menus (for now, all of them are located in "Settings") */
	final Menu fpsCap = new Menu("FPS Limit");
	final Menu showFPS = new Menu("Show FPS Counter");
	final Menu phoneType = new Menu("Phone Key Layout");

	/* Dialogs for resolution changes, restart notifications, MemStats and info about FreeJ2ME */
	final Dialog[] awtDialogs = 
	{
		new Dialog(main , "Set LCD Resolution", true),
		new Dialog(main , "About FreeJ2ME", true),
		new Dialog(main, "FreeJ2ME MemStat", false),
		new Dialog(main, "Restart Required", true),
		new Dialog(main, "Key Mapping", true),
	};
	
	final Button[] awtButtons = 
	{
		new Button("Close"),
		new Button("Apply"),
		new Button("Cancel"),
		new Button("Close FreeJ2ME"),
		new Button("Restart later"),
		new Button("Apply Inputs")
	};
	

	/* Log Level menu */
	Menu logLevel = new Menu("Log Level");

	/* Input mapping keys */
	final Button inputButtons[] = new Button[] 
	{
		new Button("Q"),
		new Button("W"),
		new Button("Up"),
		new Button("Left"),
		new Button("Enter"),
		new Button("Right"),
		new Button("Down"),
		new Button("NumPad-7"),
		new Button("NumPad-8"),
		new Button("NumPad-9"),
		new Button("NumPad-4"),
		new Button("NumPad-5"),
		new Button("NumPad-6"),
		new Button("NumPad-1"),
		new Button("NumPad=2"),
		new Button("NumPad-3"),
		new Button("E"),
		new Button("NumPad-0"),
		new Button("R"),
	};

	/* Constant fields for key mapping indices (matches the array above) */
	static final byte SOFT_LEFT_KEY = 0;
	static final byte SOFT_RIGHT_KEY = 1;
	static final byte UP_ARROW_KEY = 2;
	static final byte LEFT_ARROW_KEY = 3;
	static final byte OK_KEY = 4;
	static final byte RIGHT_ARROW_KEY = 5;
	static final byte DOWN_ARROW_KEY = 6;
	static final byte NUMPAD1_KEY = 7;
	static final byte NUMPAD2_KEY = 8;
	static final byte NUMPAD3_KEY = 9;
	static final byte NUMPAD4_KEY = 10;
	static final byte NUMPAD5_KEY = 11;
	static final byte NUMPAD6_KEY = 12;
	static final byte NUMPAD7_KEY = 13;
	static final byte NUMPAD8_KEY = 14;
	static final byte NUMPAD9_KEY = 15;
	static final byte NUMPAD_ASTERISK_KEY = 16;
	static final byte NUMPAD0_KEY = 17;
	static final byte NUMPAD_POUND_KEY = 18;

	/* Array of inputs in order to support input remapping */
	int inputKeycodes[] = new int[] { 
		KeyEvent.VK_Q, KeyEvent.VK_W, 
		KeyEvent.VK_UP, KeyEvent.VK_LEFT, KeyEvent.VK_ENTER, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN, 
		KeyEvent.VK_NUMPAD7, KeyEvent.VK_NUMPAD8, KeyEvent.VK_NUMPAD9, 
		KeyEvent.VK_NUMPAD4, KeyEvent.VK_NUMPAD5, KeyEvent.VK_NUMPAD6, 
		KeyEvent.VK_NUMPAD1, KeyEvent.VK_NUMPAD2, KeyEvent.VK_NUMPAD3, 
		KeyEvent.VK_E, KeyEvent.VK_NUMPAD0, KeyEvent.VK_R
	};

	private final int newInputKeycodes[] = Arrays.copyOf(inputKeycodes, inputKeycodes.length);

	final Choice resChoice = new Choice();

	Label totalMemLabel = new Label("Total Mem: 000000000 KB");
	Label freeMemLabel = new Label("Free Mem : 000000000 KB");
	Label usedMemLabel = new Label("Used Mem : 000000000 KB");
	Label maxMemLabel = new Label("Max Mem  : 000000000 KB");

	/* Items for each of the bar's menus */
	final UIListener menuItemListener = new UIListener();

	final MenuItem aboutMenuItem = new MenuItem("About FreeJ2ME");
	final MenuItem resChangeMenuItem = new MenuItem("Change Phone Resolution");

	final MenuItem openMenuItem = new MenuItem("Open JAR/JAD File");
	final MenuItem closeMenuItem = new MenuItem("Close Jar (Stub)");
	final MenuItem scrShot = new MenuItem("Take Screenshot");
	final MenuItem exitMenuItem = new MenuItem("Exit FreeJ2ME");
	final MenuItem mapInputs = new MenuItem("Manage Inputs");

	final CheckboxMenuItem enableAudio = new CheckboxMenuItem("Enable Audio", false);
	final CheckboxMenuItem enableRotation = new CheckboxMenuItem("Rotate Screen", false);
	final CheckboxMenuItem useCustomMidi = new CheckboxMenuItem("Use custom midi soundfont", false);

	final CheckboxMenuItem[] layoutOptions = 
	{
		new CheckboxMenuItem("Default", true),
		new CheckboxMenuItem("LG", false),
		new CheckboxMenuItem("Motorola/SoftBank", false),
		new CheckboxMenuItem("Motorola V8", false),
		new CheckboxMenuItem("Motorola Triplets", false),
		new CheckboxMenuItem("Nokia Full Keyboard", false),
		new CheckboxMenuItem("Sagem", false),
		new CheckboxMenuItem("Siemens", false),
		new CheckboxMenuItem("Siemens Old", false)
	};
	final String[] layoutValues = {"Standard", "LG", "Motorola", "MotoTriplets", "MotoV8", "NokiaKeyboard", "Sagem", "Siemens", "SiemensOld"};
	
	final CheckboxMenuItem[] fpsOptions = 
	{
		new CheckboxMenuItem("No Limit", true),
		new CheckboxMenuItem("60 FPS", false),
		new CheckboxMenuItem("30 FPS", false),
		new CheckboxMenuItem("15 FPS", false)
	};
	final String[] fpsValues = {"0", "60", "30", "15"};

	final CheckboxMenuItem[] fpsCounterPos = 
	{
		new CheckboxMenuItem("Off", true),
		new CheckboxMenuItem("Top Left", false),
		new CheckboxMenuItem("Top Right", false),
		new CheckboxMenuItem("Bottom Left", false),
		new CheckboxMenuItem("Bottom Right", false)
	};
	final String[] showFPSValues = {"Off", "TopLeft", "TopRight", "BottomLeft", "BottomRight"};

	final CheckboxMenuItem[] logLevels = 
	{
		new CheckboxMenuItem("Disabled", false),
		new CheckboxMenuItem("Debug", false),
		new CheckboxMenuItem("Info", false),
		new CheckboxMenuItem("Warning", false),
		new CheckboxMenuItem("Error", false)
	};

	final CheckboxMenuItem noAlphaOnBlankImages = new CheckboxMenuItem("No alpha on blank images");
	
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

		awtButtons[0].setBackground(Color.green);
		awtButtons[1].setBackground(Color.green);
		awtButtons[2].setBackground(Color.yellow);
		awtButtons[3].setBackground(Color.green);
		awtButtons[4].setBackground(Color.yellow);
		awtButtons[5].setBackground(Color.green);

		awtDialogs[1].setBackground(Color.white);
		awtDialogs[1].setLayout( new FlowLayout(FlowLayout.CENTER, 200, 0));  
		awtDialogs[1].setUndecorated(true); /* Whenever a Dialog is undecorated, it's because it's meant to look like an internal menu on FreeJ2ME's main Frame */
		awtDialogs[1].setSize(230, 225);
		awtDialogs[1].setResizable(false);
		awtDialogs[1].setLocationRelativeTo(main);
		awtDialogs[1].add(new Label("FreeJ2ME-Plus - A free J2ME emulator"));
		awtDialogs[1].add(new Label("--------------------------------"));
		awtDialogs[1].add(new Label("Original Project Authors:"));
		awtDialogs[1].add(new Label("David Richardson (Recompile)"));
		awtDialogs[1].add(new Label("Saket Dandawate (hex007)"));
		awtDialogs[1].add(new Label("--------------------------------"));
		awtDialogs[1].add(new Label("Plus Fork Maintainer:"));
		awtDialogs[1].add(new Label("Paulo Sousa (AShiningRay)"));
		awtDialogs[1].add(awtButtons[0]);


		awtDialogs[0].setBackground(Color.white);
		awtDialogs[0].setLayout( new FlowLayout(FlowLayout.CENTER, 60, 5));
		awtDialogs[0].setUndecorated(true);
		awtDialogs[0].setSize(230, 175);
		awtDialogs[0].setResizable(false);
		awtDialogs[0].setLocationRelativeTo(main);
		awtDialogs[0].add(new Label("Select a Resolution from the Dropdown"));
		awtDialogs[0].add(new Label("Then hit 'Apply'!"));
		awtDialogs[0].add(resChoice);
		awtDialogs[0].add(awtButtons[1]);
		awtDialogs[0].add(awtButtons[2]);


		awtDialogs[2].setBackground(Color.white);
		awtDialogs[2].setLayout( new FlowLayout(FlowLayout.LEFT, 5, 0));  
		awtDialogs[2].setSize(240, 145);
		awtDialogs[2].setResizable(false);
		awtDialogs[2].add(totalMemLabel);
		awtDialogs[2].add(freeMemLabel);
		awtDialogs[2].add(usedMemLabel);
		awtDialogs[2].add(maxMemLabel);

		/* Input mapping dialog: It's a grid, so a few tricks had to be employed to align everything up */
		awtDialogs[4].setBackground(Color.white);
		awtDialogs[4].setLayout(new GridLayout(0, 3)); /* Get as many rows as needed, as long it still uses only 3 columns */
		awtDialogs[4].setSize(240, 320);
		awtDialogs[4].setResizable(false);

		awtDialogs[4].add(new Label("Map keys by"));
		awtDialogs[4].add(new Label("clicking each"));
		awtDialogs[4].add(new Label("button below"));

		awtDialogs[4].add(new Label(""));
		awtDialogs[4].add(awtButtons[5]);
		awtDialogs[4].add(new Label(""));

		awtDialogs[4].add(new Label("-----------------------"));
		awtDialogs[4].add(new Label("-----------------------"));
		awtDialogs[4].add(new Label("-----------------------"));

		awtDialogs[4].add(inputButtons[0]);
		awtDialogs[4].add(new Label(""));
		awtDialogs[4].add(inputButtons[1]);

		awtDialogs[4].add(new Label(""));
		awtDialogs[4].add(inputButtons[2]);
		awtDialogs[4].add(new Label(""));

		awtDialogs[4].add(inputButtons[3]);
		awtDialogs[4].add(inputButtons[4]);
		awtDialogs[4].add(inputButtons[5]);

		awtDialogs[4].add(new Label(""));
		awtDialogs[4].add(inputButtons[6]);
		awtDialogs[4].add(new Label(""));

		awtDialogs[4].add(new Label(""));
		awtDialogs[4].add(new Label(""));
		awtDialogs[4].add(new Label(""));
		
		awtDialogs[4].add(inputButtons[7]);
		awtDialogs[4].add(inputButtons[8]);
		awtDialogs[4].add(inputButtons[9]);
		
		awtDialogs[4].add(inputButtons[10]);
		awtDialogs[4].add(inputButtons[11]);
		awtDialogs[4].add(inputButtons[12]);

		awtDialogs[4].add(inputButtons[13]);
		awtDialogs[4].add(inputButtons[14]);
		awtDialogs[4].add(inputButtons[15]);

		awtDialogs[4].add(inputButtons[16]);
		awtDialogs[4].add(inputButtons[17]);
		awtDialogs[4].add(inputButtons[18]);


		awtDialogs[3].setBackground(Color.white);
		awtDialogs[3].setLayout( new FlowLayout(FlowLayout.CENTER, 10, 10));  
		awtDialogs[3].setUndecorated(true);
		awtDialogs[3].setSize(230, 175);
		awtDialogs[3].setLocationRelativeTo(main);
		awtDialogs[3].add(new Label("This change requires a restart to apply!"));
		awtDialogs[3].add(awtButtons[3]);
		awtDialogs[3].add(awtButtons[4]);
		
		openMenuItem.setActionCommand("Open");
		closeMenuItem.setActionCommand("Close");
		scrShot.setActionCommand("Screenshot");
		exitMenuItem.setActionCommand("Exit");
		aboutMenuItem.setActionCommand("AboutMenu");
		resChangeMenuItem.setActionCommand("ChangeResolution");
		awtButtons[1].setActionCommand("ApplyResChange");
		awtButtons[2].setActionCommand("CancelResChange");
		awtButtons[0].setActionCommand("CloseAboutMenu");
		awtButtons[3].setActionCommand("CloseFreeJ2ME");
		awtButtons[4].setActionCommand("RestartLater");
		mapInputs.setActionCommand("MapInputs");
		awtButtons[5].setActionCommand("ApplyInputs");
		
		openMenuItem.addActionListener(menuItemListener);
		closeMenuItem.addActionListener(menuItemListener);
		scrShot.addActionListener(menuItemListener);
		exitMenuItem.addActionListener(menuItemListener);
		aboutMenuItem.addActionListener(menuItemListener);
		resChangeMenuItem.addActionListener(menuItemListener);
		awtButtons[1].addActionListener(menuItemListener);
		awtButtons[2].addActionListener(menuItemListener);
		awtButtons[0].addActionListener(menuItemListener);
		awtButtons[3].addActionListener(menuItemListener);
		awtButtons[4].addActionListener(menuItemListener);
		mapInputs.addActionListener(menuItemListener);
		awtButtons[5].addActionListener(menuItemListener);

		addInputButtonListeners();

		setActionListeners();

		buildMenuBar();
	}

	private void addInputButtonListeners() 
	{
		for(int i = 0; i < inputButtons.length; i++) 
		{
			final int buttonIndex = i;

			/* Add a focus listener to each input mapping button */
            inputButtons[i].addFocusListener(new FocusAdapter() 
			{
                Button focusedButton;
				String lastButtonKey = new String("");
				boolean keySet = false;

				@Override
				public void focusGained(FocusEvent e) 
				{
					{
						keySet = false;
						focusedButton = (Button) e.getComponent();
						lastButtonKey = focusedButton.getLabel();
						focusedButton.setLabel("Waiting...");

						focusedButton.addKeyListener(new KeyAdapter() 
						{
							public void keyPressed(KeyEvent e) 
							{
								focusedButton.setLabel(KeyEvent.getKeyText(e.getKeyCode()));
								keySet = true;
								/* Save the new key's code into the expected index of newInputKeycodes */
								newInputKeycodes[buttonIndex] = e.getKeyCode();
							}
						});
					}
				}

				/* Only used to restore the last key map if the user doesn't map a new one into the button */
				@Override
				public void focusLost(FocusEvent e) { if(!keySet) { focusedButton.setLabel(lastButtonKey); } }
            });
		}
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
				if(useCustomMidi.getState()){ config.updateSoundfont("Custom"); hasPendingChange = true; }
				else{ config.updateSoundfont("Default"); hasPendingChange = true; }

				awtDialogs[3].setLocationRelativeTo(main);
				awtDialogs[3].setVisible(true);
			}
		});

		noAlphaOnBlankImages.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(noAlphaOnBlankImages.getState()){ config.updateAlphaSpeedHack("on"); hasPendingChange = true; }
				else{ config.updateAlphaSpeedHack("off"); hasPendingChange = true; }

				awtDialogs[3].setLocationRelativeTo(main);
				awtDialogs[3].setVisible(true);
			}
		});

		// Layout options
		for(byte i = 0; i < layoutOptions.length; i++) 
		{
			final byte index = i;
			layoutOptions[i].addItemListener(new ItemListener() 
			{
				public void itemStateChanged(ItemEvent e) 
				{
					if(!layoutOptions[index].getState()){ layoutOptions[index].setState(true); }
					if(layoutOptions[index].getState())
					{ 
						config.updatePhone(layoutValues[index]);
						for(int j = 0; j < layoutOptions.length; j++) 
						{
							if(j != index) { layoutOptions[j].setState(false); }
						}
						hasPendingChange = true;
					}
				}
			});
		}

		for(byte i = 0; i < fpsOptions.length; i++) 
		{
			final byte index = i;
			fpsOptions[i].addItemListener(new ItemListener() 
			{
				public void itemStateChanged(ItemEvent e) 
				{
					if(!fpsOptions[index].getState()){ fpsOptions[index].setState(true); }
					if(fpsOptions[index].getState())
					{ 
						config.updateFPS(fpsValues[index]);
						for(int j = 0; j < fpsOptions.length; j++) 
						{
							if(j != index) { fpsOptions[j].setState(false); }
						}
						hasPendingChange = true;
					}
				}
			});
		}

		for(byte i = 0; i < fpsCounterPos.length; i++) 
		{
			final byte index = i;
			fpsCounterPos[i].addItemListener(new ItemListener() 
			{
				public void itemStateChanged(ItemEvent e) 
				{
					if(!fpsCounterPos[index].getState()){ fpsCounterPos[index].setState(true); }
					if(fpsCounterPos[index].getState())
					{ 
						Mobile.getPlatform().setShowFPS(showFPSValues[index]);
						for(int j = 0; j < fpsCounterPos.length; j++) 
						{
							if(j != index) { fpsCounterPos[j].setState(false); }
						}
					}
				}
			});
		}

		for(byte i = 0; i < logLevels.length; i++) 
		{
			final byte index = i;
			logLevels[i].addItemListener(new ItemListener() 
			{
				public void itemStateChanged(ItemEvent e) 
				{
					if(!logLevels[index].getState()){ logLevels[index].setState(true); }
					if(logLevels[index].getState())
					{ 
						Mobile.logging = (index > 0);
						Mobile.minLogLevel = (byte) (index-1); // This can go negative if index = 0, as it won't log anyway.
						for(int j = 0; j < logLevels.length; j++) 
						{
							if(j != index) { logLevels[j].setState(false); }
						}
					}
				}
			});
		}
		
		dumpAudioData.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(dumpAudioData.getState()){ Manager.dumpAudioStreams = true; }
				else{ Manager.dumpAudioStreams = false; }
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
				if(showMemoryUsage.getState()){ awtDialogs[2].setLocation(main.getLocation().x+main.getSize().width, main.getLocation().y); awtDialogs[2].setVisible(true); }
				else{ awtDialogs[2].setVisible(false); }
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
		optionMenu.add(showFPS);
		optionMenu.add(phoneType);
		optionMenu.add(fpsCap);
		optionMenu.add(mapInputs);
		optionMenu.add(speedHackMenu);

		debugMenu.add(dumpAudioData);
		debugMenu.add(dumpGraphicsData);
		debugMenu.add(showMemoryUsage);
		debugMenu.add(logLevel);

		for(int i = 0; i < logLevels.length; i++) { logLevel.add(logLevels[i]); }
		logLevels[0].setState(false);
		logLevels[1].setState(false);
		logLevels[2].setState(true);
		logLevels[3].setState(false);
		logLevels[4].setState(false);

		for(int i = 0; i < config.supportedResolutions.length; i++) { resChoice.add(config.supportedResolutions[i]); }
		for(int i = 0; i < layoutOptions.length; i++) { phoneType.add(layoutOptions[i]); }
		for(int i = 0; i < fpsOptions.length; i++) { fpsCap.add(fpsOptions[i]); }
		for(int i = 0; i < fpsCounterPos.length; i++) { showFPS.add(fpsCounterPos[i]); }

		speedHackMenu.add(noAlphaOnBlankImages);
		
		// add menus to menubar
		menuBar.add(fileMenu);
		menuBar.add(optionMenu);
		menuBar.add(debugMenu);
	}

	public void updateOptions() 
	{
			enableAudio.setState(config.settings.get("sound").equals("on"));
			enableRotation.setState(config.settings.get("rotate").equals("on"));
			useCustomMidi.setState(config.settings.get("soundfont").equals("Custom"));

			for(int i = 0; i < fpsOptions.length; i++) { fpsOptions[i].setState(config.settings.get("fps").equals(fpsValues[i])); }

			for(int i = 0; i < layoutOptions.length; i++) 
			{
				layoutOptions[i].setState(config.settings.get("phone").equals(layoutValues[i]));
			}

			noAlphaOnBlankImages.setState(config.settings.get("spdhacknoalpha").equals("on"));

			resChoice.select(""+ Integer.parseInt(config.settings.get("width")) + "x" + ""+ Integer.parseInt(config.settings.get("height")));

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

	class UIListener implements ActionListener 
	{
		public void actionPerformed(ActionEvent a) 
		{            

			if(a.getActionCommand() == "Open") 
			{
				FileDialog filePicker = new FileDialog(main, "Open JAR/JAD File", FileDialog.LOAD);
				String filename;
				filePicker.setFilenameFilter(new FilenameFilter()
				{
					public boolean accept(File dir, String name) 
					{ return name.toLowerCase().endsWith(".jar") || name.toLowerCase().endsWith(".jad"); }
				});
				filePicker.setVisible(true);

				filename = filePicker.getFile();
				jarfile = new File(filePicker.getDirectory()+File.separator+filePicker.getFile()).toURI().toString();

				if(filename == null) { Mobile.log(Mobile.LOG_DEBUG, AWTGUI.class.getPackage().getName() + "." + AWTGUI.class.getSimpleName() + ": " + "JAR Loading was cancelled"); }
				else { loadJarFile(jarfile, true); }
			}

			else if(a.getActionCommand() == "Close") 
			{
				try
				{
					/* TODO: Try closing the loaded jar without closing FreeJ2ME */
				}
				catch (Throwable e) { Mobile.log(Mobile.LOG_ERROR, AWTGUI.class.getPackage().getName() + "." + AWTGUI.class.getSimpleName() + ": " + "Couldn't close jar"); }
			}

			else if(a.getActionCommand() == "Screenshot") { ScreenShot.takeScreenshot(false); }

			else if(a.getActionCommand() == "Exit") { System.exit(0); }

			else if(a.getActionCommand() == "AboutMenu") { awtDialogs[1].setLocationRelativeTo(main); awtDialogs[1].setVisible(true); }

			else if(a.getActionCommand() == "CloseAboutMenu") { awtDialogs[1].setVisible(false); }

			else if(a.getActionCommand() == "ChangeResolution") { awtDialogs[0].setLocationRelativeTo(main); awtDialogs[0].setVisible(true); }

			else if(a.getActionCommand() == "ApplyResChange") 
			{
				if(fileLoaded) /* Only update res if a jar was loaded, or else AWT throws NullPointerException */
				{
					String[] res = resChoice.getItem(resChoice.getSelectedIndex()).split("x");

					config.updateDisplaySize(Integer.parseInt(res[0]), Integer.parseInt(res[1]));
					hasPendingChange = true;
				}
				awtDialogs[0].setVisible(false);
			}

			else if (a.getActionCommand() == "CancelResChange") { awtDialogs[0].setVisible(false); }

			else if(a.getActionCommand() == "CloseFreeJ2ME") { System.exit(0); }

			else if(a.getActionCommand() == "RestartLater") { awtDialogs[3].setVisible(false); }

			else if(a.getActionCommand() == "MapInputs") { awtDialogs[4].setLocation(main.getLocation().x, main.getLocation().y); awtDialogs[4].setVisible(true); }

			/* TODO: Flesh out input mappings apply and file saving (preferably per-game, though a global config could also work great) */
			else if(a.getActionCommand() == "ApplyInputs") 
			{
				System.arraycopy(newInputKeycodes, 0, inputKeycodes, 0, inputKeycodes.length);
				awtDialogs[4].setVisible(false); 
			}

		}
	}

	public void loadJarFile(String jarpath, boolean firstLoad) 
	{
		jarfile = jarpath;
		fileLoaded = true;
		this.firstLoad = firstLoad;
	}

	public MenuBar getMenuBar() { return menuBar; }

	public boolean hasChanged() { return hasPendingChange; }

	public void clearChanged() { hasPendingChange = false; }

	public boolean hasLoadedFile() { return fileLoaded; }

	public void setMainFrame(Frame main) { this.main = main; }

	public String getJarPath() { return jarfile; }

	public boolean hasJustLoaded() { return firstLoad; }
}
