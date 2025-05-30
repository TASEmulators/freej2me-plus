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

import java.awt.Button;
import java.awt.CheckboxMenuItem;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.MenuBar;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;

import java.io.File;
import java.io.FilenameFilter;

import java.util.Arrays;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.MobilePlatform;

public final class AWTGUI 
{
	final String VERSION = "1.48";
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
	final Menu fileMenu = new Menu("File");
	final Menu optionMenu = new Menu("Settings");
	final Menu speedHackMenu = new Menu("SpeedHacks"); 
	final Menu compatSettingsMenu = new Menu("Compatibility Settings"); 
	final Menu debugMenu = new Menu("Debug");

	/* Sub menus (for now, all of them are located in "Settings") */
	final Menu fpsCap = new Menu("FPS Limit");
	final Menu unlockFPSHack = new Menu("Unlock FPS Hack");
	final Menu showFPS = new Menu("Show FPS Counter");
	final Menu phoneType = new Menu("Phone Key Layout");
	final Menu backlightColor = new Menu("Backlight Color");
	final Menu fontOffset = new Menu("Font Size Offset");

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
		new Button("Restart Now"),
		new Button("Restart later"),
		new Button("Apply Inputs"),
		new Button("Cancel")
	};
	

	/* Log Level submenu */
	final Menu logLevel = new Menu("Log Level");

	/* M3G Debug submenu */
	final Menu M3GDebug = new Menu("M3G Debugging");

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
		new Button("Space"),
		new Button("C"),
		new Button("X")
	};

	/* Array of inputs in order to support input remapping */
	int inputKeycodes[] = new int[] { 
		KeyEvent.VK_Q, KeyEvent.VK_W, 
		KeyEvent.VK_UP, KeyEvent.VK_LEFT, KeyEvent.VK_ENTER, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN, 
		KeyEvent.VK_NUMPAD7, KeyEvent.VK_NUMPAD8, KeyEvent.VK_NUMPAD9, 
		KeyEvent.VK_NUMPAD4, KeyEvent.VK_NUMPAD5, KeyEvent.VK_NUMPAD6, 
		KeyEvent.VK_NUMPAD1, KeyEvent.VK_NUMPAD2, KeyEvent.VK_NUMPAD3, 
		KeyEvent.VK_E, KeyEvent.VK_NUMPAD0, KeyEvent.VK_R, KeyEvent.VK_SPACE, KeyEvent.VK_C, KeyEvent.VK_X
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

	final MenuItem openMenuItem = new MenuItem("Open JAR / JAD / KJX / MSD File");
	final MenuItem restartMenuItem = new MenuItem("Restart Running Jar");
	final MenuItem closeMenuItem = new MenuItem("Close Running Jar");
	final MenuItem scrShot = new MenuItem("Take Screenshot (Ctrl+Alt+C)");
	final MenuItem pauseRes = new MenuItem("Pause / Resume (Ctrl+Alt+X)");
	final MenuItem exitMenuItem = new MenuItem("Exit FreeJ2ME");
	final MenuItem mapInputs = new MenuItem("Manage Inputs");

	final MenuItem showPlayer = new MenuItem("J2ME Media Player");

	final CheckboxMenuItem fullScreen = new CheckboxMenuItem("Toggle Fullscreen (Ctrl+Alt+F)");
	final CheckboxMenuItem enableRotation = new CheckboxMenuItem("Rotate Screen (Ctrl+Alt+R)", false);
	final CheckboxMenuItem enableAudio = new CheckboxMenuItem("Enable Audio", false);
	final CheckboxMenuItem useCustomMidi = new CheckboxMenuItem("Use custom midi soundfont", false);
	final CheckboxMenuItem useCustomFont = new CheckboxMenuItem("Use custom text font", false);

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
		new CheckboxMenuItem("Sharp", false),
		new CheckboxMenuItem("SKT", false)
	};
	final String[] layoutValues = {"Standard", "LG", "Motorola", "MotoV8", "MotoTriplets", "NokiaKeyboard", "Sagem", "Siemens", "Sharp", "SKT"};
	
	final CheckboxMenuItem[] backlightOptions = 
	{
		new CheckboxMenuItem("White/Disabled", false),
		new CheckboxMenuItem("Green", true),
		new CheckboxMenuItem("Cyan", false),
		new CheckboxMenuItem("Orange", false),
		new CheckboxMenuItem("Violet", false),
		new CheckboxMenuItem("Red", false)
	};
	final String[] backlightValues = {"Disabled", "Green", "Cyan", "Orange", "Violet", "Red"};

	final CheckboxMenuItem[] fpsOptions = 
	{
		new CheckboxMenuItem("No Limit", true),
		new CheckboxMenuItem("60 FPS", false),
		new CheckboxMenuItem("55 FPS", false),
		new CheckboxMenuItem("50 FPS", false),
		new CheckboxMenuItem("45 FPS", false),
		new CheckboxMenuItem("40 FPS", false),
		new CheckboxMenuItem("35 FPS", false),
		new CheckboxMenuItem("30 FPS", false),
		new CheckboxMenuItem("25 FPS", false),
		new CheckboxMenuItem("20 FPS", false),
		new CheckboxMenuItem("15 FPS", false),
		new CheckboxMenuItem("10 FPS", false)
	};
	final String[] fpsValues = {"0", "60", "55", "50", "45", "40", "35", "30", "25", "20", "15", "10"};

	final CheckboxMenuItem[] fpsHackOptions = 
	{
		new CheckboxMenuItem("Disabled", true),
		new CheckboxMenuItem("Safe", false),
		new CheckboxMenuItem("Extended", false),
		new CheckboxMenuItem("Aggressive", false)
	};
	final String[] fpsHackValues = {"Disabled", "Safe", "Extended", "Aggressive"};

	final CheckboxMenuItem[] fpsCounterPos = 
	{
		new CheckboxMenuItem("Off", true),
		new CheckboxMenuItem("Top Left", false),
		new CheckboxMenuItem("Top Right", false),
		new CheckboxMenuItem("Bottom Left", false),
		new CheckboxMenuItem("Bottom Right", false)
	};
	final String[] showFPSValues = {"Off", "TopLeft", "TopRight", "BottomLeft", "BottomRight"};

	final CheckboxMenuItem[] fontOffsets = 
	{
		new CheckboxMenuItem("-4pt", false),
		new CheckboxMenuItem("-3pt", false),
		new CheckboxMenuItem("-2pt", false),
		new CheckboxMenuItem("-1pt", false),
		new CheckboxMenuItem(" 0pt (Default)", true),
		new CheckboxMenuItem(" 1pt", false),
		new CheckboxMenuItem(" 2pt", false),
		new CheckboxMenuItem(" 3pt", false),
		new CheckboxMenuItem(" 4pt", false)
	};
	final String[] fontOffsetValues = {"-4", "-3", "-2", "-1", "0", "1", "2", "3", "4"};

	final CheckboxMenuItem[] logLevels = 
	{
		new CheckboxMenuItem("Disabled", false),
		new CheckboxMenuItem("Debug", false),
		new CheckboxMenuItem("Info", false),
		new CheckboxMenuItem("Warning", false),
		new CheckboxMenuItem("Error", false)
	};
	final String[] logLevelValues = {"0", "1", "2", "3", "4"};

	// Speedhacks
	final CheckboxMenuItem noAlphaOnBlankImages = new CheckboxMenuItem("No alpha on blank images");
	
	// Compatibility settings
	final CheckboxMenuItem doNotTranslateDrawRGB = new CheckboxMenuItem("Don't translate DrawRGB calls");
	final CheckboxMenuItem transToOriginOnReset = new CheckboxMenuItem("Translate to origin on gfx reset");
	final CheckboxMenuItem immediateRepaints = new CheckboxMenuItem("Process canvas repaints immediately");
	final CheckboxMenuItem overridePlatChecks = new CheckboxMenuItem("Override Mobile Platform checks");
	final CheckboxMenuItem siemensFriendlyDrawing = new CheckboxMenuItem("Siemens-friendly drawing methods");

	final CheckboxMenuItem deleteTemporaryKJXFiles = new CheckboxMenuItem("Delete KJX files' temporary JAR/JAD");
	final CheckboxMenuItem dumpAudioData = new CheckboxMenuItem("Dump Audio Streams");
	final CheckboxMenuItem dumpGraphicsData = new CheckboxMenuItem("Dump Graphics Objects");
	final CheckboxMenuItem showMemoryUsage = new CheckboxMenuItem("Show VM Memory Usage");
	
	// M3G Debugging
	final CheckboxMenuItem M3GUntextured = new CheckboxMenuItem("Draw Only Vertex Colors");
	final CheckboxMenuItem M3GWireframe = new CheckboxMenuItem("Wireframe Mode");


	public AWTGUI(Config config)
	{
		this.config = config;

		resChoice.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
		resChoice.setBackground(FreeJ2ME.freeJ2MEBGColor);
		resChoice.setForeground(Color.ORANGE);

		totalMemLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
		freeMemLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
		usedMemLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
		maxMemLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));

		awtButtons[0].setBackground(FreeJ2ME.freeJ2MEDragColor);
		awtButtons[0].setForeground(Color.ORANGE);

		awtButtons[1].setBackground(FreeJ2ME.freeJ2MEDragColor);
		awtButtons[1].setForeground(Color.ORANGE);

		awtButtons[2].setBackground(FreeJ2ME.freeJ2MEBGColor);
		awtButtons[2].setForeground(Color.ORANGE);
		
		awtButtons[3].setBackground(FreeJ2ME.freeJ2MEDragColor);
		awtButtons[3].setForeground(Color.ORANGE);

		awtButtons[4].setBackground(FreeJ2ME.freeJ2MEBGColor);
		awtButtons[4].setForeground(Color.ORANGE);

		awtDialogs[1].setBackground(FreeJ2ME.freeJ2MEBGColor);
		awtDialogs[1].setForeground(Color.ORANGE);
		awtDialogs[1].setLayout( new FlowLayout(FlowLayout.CENTER, 200, 0));  
		awtDialogs[1].setUndecorated(true); /* Whenever a Dialog is undecorated, it's because it's meant to look like an internal menu on FreeJ2ME's main Frame */
		awtDialogs[1].setSize(230, 235);
		awtDialogs[1].setResizable(false);
		awtDialogs[1].setLocationRelativeTo(main);
		awtDialogs[1].add(new Label("FreeJ2ME-Plus - A free J2ME emulator"));
		awtDialogs[1].add(new Label("Version " + VERSION));
		awtDialogs[1].add(new Label("--------------------------------"));
		awtDialogs[1].add(new Label("Original Project Authors:"));
		awtDialogs[1].add(new Label("David Richardson (Recompile)"));
		awtDialogs[1].add(new Label("Saket Dandawate (hex007)"));
		awtDialogs[1].add(new Label("--------------------------------"));
		awtDialogs[1].add(new Label("Plus Fork Maintainer:"));
		awtDialogs[1].add(new Label("Paulo Sousa (AShiningRay)"));
		awtDialogs[1].add(awtButtons[0]);


		awtDialogs[0].setBackground(FreeJ2ME.freeJ2MEBGColor);
		awtDialogs[0].setForeground(Color.ORANGE);
		awtDialogs[0].setLayout( new FlowLayout(FlowLayout.CENTER, 60, 5));
		awtDialogs[0].setUndecorated(true);
		awtDialogs[0].setSize(230, 125);
		awtDialogs[0].setResizable(false);
		awtDialogs[0].setLocationRelativeTo(main);
		awtDialogs[0].add(new Label("Select a Resolution from the Dropdown"));
		awtDialogs[0].add(new Label("Then hit 'Apply'!"));
		awtDialogs[0].add(resChoice);
		awtDialogs[0].add(awtButtons[1]);
		awtDialogs[0].add(awtButtons[2]);


		awtDialogs[2].setBackground(FreeJ2ME.freeJ2MEBGColor);
		awtDialogs[2].setForeground(Color.ORANGE);
		awtDialogs[2].setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
		awtDialogs[2].setSize(240, 145);
		awtDialogs[2].setResizable(false);
		awtDialogs[2].add(totalMemLabel);
		awtDialogs[2].add(freeMemLabel);
		awtDialogs[2].add(usedMemLabel);
		awtDialogs[2].add(maxMemLabel);

		/* Input mapping dialog: It's a grid, so a few tricks had to be employed to align everything up */
		awtDialogs[4].setBackground(FreeJ2ME.freeJ2MEBGColor);
        awtDialogs[4].setForeground(Color.ORANGE);
		awtDialogs[4].setLayout(new GridLayout(0, 3)); /* Get as many rows as needed, as long it still uses only 3 columns */
		awtDialogs[4].setSize(240, 440);
		awtDialogs[4].setLocationRelativeTo(main);
		awtDialogs[4].setResizable(false);
		

		// Setup input button colors
		awtButtons[5].setBackground(FreeJ2ME.freeJ2MEDragColor);
		awtButtons[5].setForeground(Color.GREEN);

		awtButtons[6].setBackground(FreeJ2ME.freeJ2MEDragColor);
		awtButtons[6].setForeground(Color.RED);

		for(int i = 0; i < inputButtons.length; i++) 
		{ 
			inputButtons[i].setBackground(FreeJ2ME.freeJ2MEDragColor);
			inputButtons[i].setForeground(Color.ORANGE);
		}

		awtDialogs[4].add(new Label("Map keys by"));
		awtDialogs[4].add(new Label("clicking each"));
		awtDialogs[4].add(new Label("button below"));

		awtDialogs[4].add(awtButtons[5]);
		awtDialogs[4].add(new Label(""));
		awtDialogs[4].add(awtButtons[6]);

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

		awtDialogs[4].add(new Label("-----------------------"));
		awtDialogs[4].add(new Label("-----------------------"));
		awtDialogs[4].add(new Label("-----------------------"));

		awtDialogs[4].add(new Label("Hotkeys"));
		awtDialogs[4].add(new Label(""));
		awtDialogs[4].add(new Label("(Ctrl + *)"));

		awtDialogs[4].add(new Label(""));
		awtDialogs[4].add(new Label(""));
		awtDialogs[4].add(new Label(""));
		
		awtDialogs[4].add(new Label("Fast-Forward"));
		awtDialogs[4].add(new Label("Screenshot"));
		awtDialogs[4].add(new Label("Pause/Resume"));

		awtDialogs[4].add(inputButtons[19]);
		awtDialogs[4].add(inputButtons[20]);
		awtDialogs[4].add(inputButtons[21]);

		awtDialogs[4].add(new Label(""));
		awtDialogs[4].add(new Label(""));
		awtDialogs[4].add(new Label(""));
		
		awtDialogs[4].add(new Label("Slowdown"));
		awtDialogs[4].add(new Label("TODO"));
		awtDialogs[4].add(new Label("TODO"));

		awtDialogs[4].add(new Label("TODO"));
		awtDialogs[4].add(new Label(""));
		awtDialogs[4].add(new Label(""));


		awtDialogs[3].setBackground(FreeJ2ME.freeJ2MEBGColor);
		awtDialogs[3].setForeground(Color.ORANGE);
		awtDialogs[3].setLayout( new FlowLayout(FlowLayout.CENTER, 10, 10));  
		awtDialogs[3].setUndecorated(true);
		awtDialogs[3].setSize(230, 80);
		awtDialogs[3].setLocationRelativeTo(main);
		awtDialogs[3].add(new Label("This change requires a restart to apply!"));
		awtDialogs[3].add(awtButtons[3]);
		awtDialogs[3].add(awtButtons[4]);
		
		openMenuItem.setActionCommand("Open");
		restartMenuItem.setActionCommand("RestartNow");
		closeMenuItem.setActionCommand("Close");
		scrShot.setActionCommand("Screenshot");
		pauseRes.setActionCommand("PauseResume");
		exitMenuItem.setActionCommand("Exit");
		aboutMenuItem.setActionCommand("AboutMenu");
		resChangeMenuItem.setActionCommand("ChangeResolution");
		awtButtons[1].setActionCommand("ApplyResChange");
		awtButtons[2].setActionCommand("CancelResChange");
		awtButtons[0].setActionCommand("CloseAboutMenu");
		awtButtons[3].setActionCommand("RestartNow");
		awtButtons[4].setActionCommand("RestartLater");
		mapInputs.setActionCommand("MapInputs");
		awtButtons[5].setActionCommand("ApplyInputs");
		awtButtons[6].setActionCommand("CancelInputs");

		showPlayer.setActionCommand("ShowPlayer");
		
		openMenuItem.addActionListener(menuItemListener);
		restartMenuItem.addActionListener(menuItemListener);
		closeMenuItem.addActionListener(menuItemListener);
		scrShot.addActionListener(menuItemListener);
		pauseRes.addActionListener(menuItemListener);
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
		awtButtons[6].addActionListener(menuItemListener);

		showPlayer.addActionListener(menuItemListener);

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
		fullScreen.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(hasLoadedFile()) { FreeJ2ME.app.toggleFullscreen(); }
				else { fullScreen.setState(FreeJ2ME.isFullscreen); }
			}
		});

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
			}
		});

		useCustomFont.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(useCustomFont.getState()){ config.updateTextFont("Custom"); hasPendingChange = true; }
				else{ config.updateTextFont("Default"); hasPendingChange = true; }

				showRestartDialog();
			}
		});

		// Speedhacks
		noAlphaOnBlankImages.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(noAlphaOnBlankImages.getState()){ config.updateAlphaSpeedHack("on"); hasPendingChange = true; }
				else{ config.updateAlphaSpeedHack("off"); hasPendingChange = true; }

				showRestartDialog();
			}
		});

		// Compatibility settings
		doNotTranslateDrawRGB.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(doNotTranslateDrawRGB.getState()){ config.updateCompatDoNotTranslateDrawRGB("on"); hasPendingChange = true; }
				else{ config.updateCompatDoNotTranslateDrawRGB("off"); hasPendingChange = true; }

				showRestartDialog();
			}
		});

		transToOriginOnReset.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(transToOriginOnReset.getState()){ config.updateCompatTranslateToOriginOnReset("on"); hasPendingChange = true; }
				else{ config.updateCompatTranslateToOriginOnReset("off"); hasPendingChange = true; }
			}
		});

		immediateRepaints.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(immediateRepaints.getState()){ config.updateCompatImmediateRepaints("on"); hasPendingChange = true; }
				else{ config.updateCompatImmediateRepaints("off"); hasPendingChange = true; }
			}
		});

		overridePlatChecks.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				if(overridePlatChecks.getState()){ config.updateCompatOverridePlatformChecks("on"); hasPendingChange = true; }
				else{ config.updateCompatOverridePlatformChecks("off"); hasPendingChange = true; }

				showRestartDialog();
			}
		});

		siemensFriendlyDrawing.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				if(siemensFriendlyDrawing.getState()){ config.updateCompatSiemensFriendlyDrawing("on"); hasPendingChange = true; }
				else{ config.updateCompatSiemensFriendlyDrawing("off"); hasPendingChange = true; }

				showRestartDialog();
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

		for(byte i = 0; i < backlightOptions.length; i++) 
		{
			final byte index = i;
			backlightOptions[i].addItemListener(new ItemListener() 
			{
				public void itemStateChanged(ItemEvent e) 
				{
					if(!backlightOptions[index].getState()){ backlightOptions[index].setState(true); }
					if(backlightOptions[index].getState())
					{ 
						config.updateBacklight(backlightValues[index]);
						for(int j = 0; j < backlightOptions.length; j++) 
						{
							if(j != index) { backlightOptions[j].setState(false); }
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

		for(byte i = 0; i < fpsHackOptions.length; i++) 
		{
			final byte index = i;
			fpsHackOptions[i].addItemListener(new ItemListener() 
			{
				public void itemStateChanged(ItemEvent e) 
				{
					if(!fpsHackOptions[index].getState()){ fpsHackOptions[index].setState(true); }
					if(fpsHackOptions[index].getState())
					{ 
						config.updateFPSHack(fpsHackValues[index]);
						for(int j = 0; j < fpsHackOptions.length; j++) 
						{
							if(j != index) { fpsHackOptions[j].setState(false); }
						}
						hasPendingChange = true;
					}
				}
			});
		}

		for(byte i = 0; i < fontOffsets.length; i++) 
		{
			final byte index = i;
			fontOffsets[i].addItemListener(new ItemListener() 
			{
				public void itemStateChanged(ItemEvent e) 
				{
					if(!fontOffsets[index].getState()){ fontOffsets[index].setState(true); }
					if(fontOffsets[index].getState())
					{ 
						config.updateFontOffset(fontOffsetValues[index]);
						for(int j = 0; j < fontOffsets.length; j++) 
						{
							if(j != index) { fontOffsets[j].setState(false); }
						}
					}
				}
			});
		}

		// Sys settings
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
						config.updatefpsCounterPosition(showFPSValues[index]);
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
						config.updateLogLevel(logLevelValues[index]);
						for(int j = 0; j < logLevels.length; j++) 
						{
							if(j != index) { logLevels[j].setState(false); }
						}
					}
				}
			});
		}
		
		deleteTemporaryKJXFiles.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(deleteTemporaryKJXFiles.getState()) { config.updateDeleteTempKJXFiles("on"); Mobile.deleteTemporaryKJXFiles = true; }
				else { config.updateDeleteTempKJXFiles("off"); Mobile.deleteTemporaryKJXFiles = false; }
			}
		});
		

		dumpAudioData.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(dumpAudioData.getState()) { config.updateDumpAudioStreams("on"); Mobile.dumpAudioStreams = true; }
				else { config.updateDumpAudioStreams("off"); Mobile.dumpAudioStreams = false; }
			}
		});

		dumpGraphicsData.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(dumpGraphicsData.getState()) { config.updateDumpGraphicsObjects("on"); Mobile.dumpGraphicsObjects = true; }
				else { config.updateDumpGraphicsObjects("off"); Mobile.dumpGraphicsObjects = false; }
			}
		});

		M3GUntextured.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(M3GUntextured.getState()) { config.updateM3GUntextured("on"); Mobile.M3GRenderUntexturedPolygons = true; }
				else { config.updateM3GUntextured("off"); Mobile.M3GRenderUntexturedPolygons = false; }
			}
		});

		M3GWireframe.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(M3GWireframe.getState()) { config.updateM3GWireframe("on"); Mobile.M3GRenderWireframe = true; }
				else { config.updateM3GWireframe("off"); Mobile.M3GRenderWireframe = false; }
			}
		});

		// This one is specific to AWTGUI
		showMemoryUsage.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				/* Mem stats frame won't be centered on FreeJ2ME's frame, instead, it will sit right by its side, that's why "setLocationRelativeTo(main)" isn't used */
				if(showMemoryUsage.getState()) { awtDialogs[2].setLocation(main.getLocation().x+main.getSize().width, main.getLocation().y); awtDialogs[2].setVisible(true); }
				else { awtDialogs[2].setVisible(false); }
			}
		});
	}

	private void buildMenuBar() 
	{
		//add menu items to menus
		fileMenu.add(openMenuItem);
		fileMenu.add(restartMenuItem);
		fileMenu.add(closeMenuItem);
		fileMenu.addSeparator();
		fileMenu.add(scrShot);
		fileMenu.add(pauseRes);
		fileMenu.addSeparator();
		fileMenu.add(aboutMenuItem);
		fileMenu.add(exitMenuItem);

		optionMenu.add(fullScreen);
		optionMenu.add(enableRotation);
		optionMenu.add(enableAudio);
		optionMenu.add(useCustomMidi);
		optionMenu.add(useCustomFont);
		optionMenu.add(resChangeMenuItem);
		optionMenu.add(mapInputs);
		optionMenu.add(phoneType);
		optionMenu.add(backlightColor);
		optionMenu.add(fpsCap);
		optionMenu.add(unlockFPSHack);
		optionMenu.add(fontOffset);
		optionMenu.add(speedHackMenu);
		optionMenu.add(compatSettingsMenu);

		debugMenu.add(showPlayer);
		debugMenu.addSeparator();
		debugMenu.add(showFPS);
		debugMenu.add(deleteTemporaryKJXFiles);
		debugMenu.add(dumpAudioData);
		debugMenu.add(dumpGraphicsData);
		debugMenu.add(showMemoryUsage);
		debugMenu.add(logLevel);
		debugMenu.add(M3GDebug);
		
		deleteTemporaryKJXFiles.setState(true);

		for(int i = 0; i < logLevels.length; i++) { logLevel.add(logLevels[i]); }
		logLevels[0].setState(false);
		logLevels[1].setState(false);
		logLevels[2].setState(true);
		logLevels[3].setState(false);
		logLevels[4].setState(false);

		M3GDebug.add(M3GUntextured);
		M3GDebug.add(M3GWireframe);

		for(int i = 0; i < config.supportedResolutions.length; i++) { resChoice.add(config.supportedResolutions[i]); }
		for(int i = 0; i < layoutOptions.length; i++) { phoneType.add(layoutOptions[i]); }
		for(int i = 0; i < backlightOptions.length; i++) { backlightColor.add(backlightOptions[i]); }
		for(int i = 0; i < fpsOptions.length; i++) { fpsCap.add(fpsOptions[i]); }
		for(int i = 0; i < fpsHackOptions.length; i++) { unlockFPSHack.add(fpsHackOptions[i]); }
		for(int i = 0; i < fpsCounterPos.length; i++) { showFPS.add(fpsCounterPos[i]); }
		for(int i = 0; i < fontOffsets.length; i++) { fontOffset.add(fontOffsets[i]); }

		speedHackMenu.add(noAlphaOnBlankImages);

		compatSettingsMenu.add(doNotTranslateDrawRGB);
		compatSettingsMenu.add(transToOriginOnReset);
		compatSettingsMenu.add(immediateRepaints);
		compatSettingsMenu.add(overridePlatChecks);
		compatSettingsMenu.add(siemensFriendlyDrawing);

		// add menus to menubar
		menuBar.add(fileMenu);
		menuBar.add(optionMenu);
		menuBar.add(debugMenu);
	}

	public void updateOptions() 
	{
			fullScreen.setState(FreeJ2ME.isFullscreen);
			enableAudio.setState(config.settings.get("sound").equals("on"));
			enableRotation.setState(config.settings.get("rotate").equals("on"));
			useCustomMidi.setState(config.settings.get("soundfont").equals("Custom"));
			useCustomFont.setState(config.settings.get("textfont").equals("Custom"));

			for(int i = 0; i < fpsOptions.length; i++) { fpsOptions[i].setState(config.settings.get("fps").equals(fpsValues[i])); }

			for(int i = 0; i < fpsHackOptions.length; i++) { fpsHackOptions[i].setState(config.settings.get("fpshack").equals(fpsHackValues[i])); }

			for(int i = 0; i < fontOffsets.length; i++) { fontOffsets[i].setState(config.settings.get("fontoffset").equals(fontOffsetValues[i])); }

			for(int i = 0; i < layoutOptions.length; i++) 
			{
				layoutOptions[i].setState(config.settings.get("phone").equals(layoutValues[i]));
			}

			for(int i = 0; i < backlightOptions.length; i++) 
			{
				backlightOptions[i].setState(config.settings.get("backlightcolor").equals(backlightValues[i]));
			}

			noAlphaOnBlankImages.setState(config.settings.get("spdhacknoalpha").equals("on"));

			doNotTranslateDrawRGB.setState(config.settings.get("compatdonottranslatedrawrgb").equals("on"));

			transToOriginOnReset.setState(config.settings.get("compattranstooriginonreset").equals("on"));

			immediateRepaints.setState(config.settings.get("compatimmediaterepaints").equals("on"));

			overridePlatChecks.setState(config.settings.get("compatoverrideplatchecks").equals("on"));

			siemensFriendlyDrawing.setState(config.settings.get("compatsiemensfriendlydrawing").equals("on"));

			resChoice.select(""+ Integer.parseInt(config.settings.get("scrwidth")) + "x" + ""+ Integer.parseInt(config.settings.get("scrheight")));

			// Sys Settings
			for(int i = 0; i < logLevels.length; i++) { logLevels[i].setState(config.sysSettings.get("logLevel").equals(logLevelValues[i])); }

			for(int i = 0; i < fpsCounterPos.length; i++) { fpsCounterPos[i].setState(config.sysSettings.get("fpsCounterPosition").equals(showFPSValues[i])); }

			dumpGraphicsData.setState(config.sysSettings.get("dumpGraphicsObjects").equals("on"));

			dumpAudioData.setState(config.sysSettings.get("dumpAudioStreams").equals("on"));
			
			M3GWireframe.setState(config.sysSettings.get("M3GWireframe").equals("on"));

			M3GUntextured.setState(config.sysSettings.get("M3GUntextured").equals("on"));

			deleteTemporaryKJXFiles.setState(config.sysSettings.get("deleteTempKJXFiles").equals("on"));

			// Get saved inputs from system config file.
			System.arraycopy(config.inputKeycodes, 0, newInputKeycodes, 0, inputKeycodes.length);
			for(int i = 0; i < inputButtons.length; i++) { inputButtons[i].setLabel(KeyEvent.getKeyText(newInputKeycodes[i])); }

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
				FileDialog filePicker = new FileDialog(main, "Open JAR / JAD / KJX / MSD File", FileDialog.LOAD);
				String filename;
				filePicker.setFilenameFilter(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.toLowerCase().endsWith(".jar") ||
								name.toLowerCase().endsWith(".jad") ||
								name.toLowerCase().endsWith(".kjx") ||
								name.toLowerCase().endsWith(".msd");
					}
				});
				filePicker.setVisible(true);

				filename = filePicker.getFile();
				
				if(filename == null) { Mobile.log(Mobile.LOG_DEBUG, AWTGUI.class.getPackage().getName() + "." + AWTGUI.class.getSimpleName() + ": " + "JAR/JAD Loading was cancelled"); }
				else
				{
						try
						{
							jarfile = new File(filePicker.getDirectory()+filename).toURI().toString();

							if(!hasLoadedFile()) { loadJarFile(jarfile); } // First jar being loaded, load straight away
							else // Otherwise, this requires a restart.
							{
								Mobile.getPlatform().fileName = jarfile;
								showRestartDialog();
							}
						}
						catch(Exception e) { Mobile.log(Mobile.LOG_DEBUG, AWTGUI.class.getPackage().getName() + "." + AWTGUI.class.getSimpleName() + ": " + "Load error:" + e.getMessage()); }
				}
			}

			else if(a.getActionCommand() == "Close") { FreeJ2ME.closeApp(); }

			else if(a.getActionCommand() == "Screenshot") { ScreenShot.takeScreenshot(false); }

			else if(a.getActionCommand() == "PauseResume") { MobilePlatform.pauseResumeApp(); }

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

			else if(a.getActionCommand() == "RestartNow") { Mobile.restartApp(); }

			else if(a.getActionCommand() == "RestartLater") { awtDialogs[3].setVisible(false); }

			else if(a.getActionCommand() == "MapInputs") { awtDialogs[4].setVisible(true); }

			else if(a.getActionCommand() == "ApplyInputs") 
			{
				System.arraycopy(newInputKeycodes, 0, inputKeycodes, 0, inputKeycodes.length);
				config.updateAWTInputs();
				awtDialogs[4].setVisible(false); 
			}

			else if(a.getActionCommand() == "CancelInputs") { awtDialogs[4].setVisible(false); }

			else if(a.getActionCommand() == "ShowPlayer") 
			{ 
				// Create FreeJ2MEPlayer Dialog instance and show it;
				FreeJ2MEPlayer playerDialog = new FreeJ2MEPlayer(main);
				playerDialog.setLocationRelativeTo(main);
				playerDialog.setVisible(true);
			}
		}
	}

	public void loadJarFile(String jarpath)
	{
		jarfile = jarpath;
		fileLoaded = true;
		firstLoad = true;
	}

	public MenuBar getMenuBar() { return menuBar; }

	public boolean hasChanged() { return hasPendingChange; }

	public void clearChanged() { hasPendingChange = false; }

	public boolean hasLoadedFile() { return fileLoaded; }

	public void setMainFrame(Frame main) { this.main = main; }

	public String getJarPath() { return jarfile; }

	public boolean hasJustLoaded() { return firstLoad; }

	public void showRestartDialog()
	{
		awtDialogs[3].setLocationRelativeTo(main);
		awtDialogs[3].setVisible(true);
	}
}
