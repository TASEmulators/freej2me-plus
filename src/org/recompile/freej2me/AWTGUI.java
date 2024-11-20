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
	final Menu phoneType = new Menu("Phone Key Layout");

	/* Dialogs for resolution changes, restart notifications, MemStats and info about FreeJ2ME */
	final Dialog resDialog = new Dialog(main , "Set LCD Resolution", true);
	final Dialog aboutDialog = new Dialog(main , "About FreeJ2ME", true);
	final Dialog memStatDialog = new Dialog(main, "FreeJ2ME MemStat", false);
	final Dialog restartRequiredDialog = new Dialog(main, "Restart Required", true);
	final Dialog inputMapDialog = new Dialog(main, "Key Mapping", true);

	final Button closeAbout = new Button("Close");
	final Button applyResChange = new Button("Apply");
	final Button cancelResChange = new Button("Cancel");
	final Button closeNow = new Button("Close FreeJ2ME");
	final Button restartLater = new Button("Restart later");

	/* Log Level menu */
	Menu logLevel = new Menu("Log Level");

	/* Input mapping keys */
	final Button applyInputs = new Button("Apply Inputs");
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
	final CheckboxMenuItem showFPS = new CheckboxMenuItem("Show FPS Counter", false);

	final CheckboxMenuItem stdLayout = new CheckboxMenuItem("J2ME Standard", true);
	final CheckboxMenuItem lgLayout = new CheckboxMenuItem("LG", false);
	final CheckboxMenuItem motorolaLayout = new CheckboxMenuItem("Motorola/SoftBank", false);
	final CheckboxMenuItem motov8Layout = new CheckboxMenuItem("Motorola V8", false);
	final CheckboxMenuItem tripletsLayout = new CheckboxMenuItem("Motorola Triplets", false);
	final CheckboxMenuItem nokiaLayout = new CheckboxMenuItem("Nokia/Sony/Samsung", false);
	final CheckboxMenuItem nokiaKbLayout = new CheckboxMenuItem("Nokia Keyboard", false);
	final CheckboxMenuItem sagemLayout = new CheckboxMenuItem("Sagem", false);
	final CheckboxMenuItem siemensLayout = new CheckboxMenuItem("Siemens", false);
	final CheckboxMenuItem siemensOldLayout = new CheckboxMenuItem("Siemens Old", false);

	final CheckboxMenuItem fpsCapNone = new CheckboxMenuItem("No Limit", true);
	final CheckboxMenuItem fpsCap60 = new CheckboxMenuItem("60 FPS", false);
	final CheckboxMenuItem fpsCap30 = new CheckboxMenuItem("30 FPS", false);
	final CheckboxMenuItem fpsCap15 = new CheckboxMenuItem("15 FPS", false);

	final CheckboxMenuItem noAlphaOnBlankImages = new CheckboxMenuItem("No alpha on blank images");

	final CheckboxMenuItem logDisabled = new CheckboxMenuItem("Disabled", false);
	final CheckboxMenuItem logDebug = new CheckboxMenuItem("Debug", false);
	final CheckboxMenuItem logInfo = new CheckboxMenuItem("Info", false);
	final CheckboxMenuItem logWarning = new CheckboxMenuItem("Warning", false);
	final CheckboxMenuItem logError = new CheckboxMenuItem("Error", false);

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
		applyInputs.setBackground(Color.green);

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

		/* Input mapping dialog: It's a grid, so a few tricks had to be employed to align everything up */
		inputMapDialog.setBackground(Color.white);
		inputMapDialog.setLayout(new GridLayout(0, 3)); /* Get as many rows as needed, as long it still uses only 3 columns */
		inputMapDialog.setSize(240, 320);
		inputMapDialog.setResizable(false);

		inputMapDialog.add(new Label("Map keys by"));
		inputMapDialog.add(new Label("clicking each"));
		inputMapDialog.add(new Label("button below"));

		inputMapDialog.add(new Label(""));
		inputMapDialog.add(applyInputs);
		inputMapDialog.add(new Label(""));

		inputMapDialog.add(new Label("-----------------------"));
		inputMapDialog.add(new Label("-----------------------"));
		inputMapDialog.add(new Label("-----------------------"));

		inputMapDialog.add(inputButtons[0]);
		inputMapDialog.add(new Label(""));
		inputMapDialog.add(inputButtons[1]);

		inputMapDialog.add(new Label(""));
		inputMapDialog.add(inputButtons[2]);
		inputMapDialog.add(new Label(""));

		inputMapDialog.add(inputButtons[3]);
		inputMapDialog.add(inputButtons[4]);
		inputMapDialog.add(inputButtons[5]);

		inputMapDialog.add(new Label(""));
		inputMapDialog.add(inputButtons[6]);
		inputMapDialog.add(new Label(""));

		inputMapDialog.add(new Label(""));
		inputMapDialog.add(new Label(""));
		inputMapDialog.add(new Label(""));
		
		inputMapDialog.add(inputButtons[7]);
		inputMapDialog.add(inputButtons[8]);
		inputMapDialog.add(inputButtons[9]);
		
		inputMapDialog.add(inputButtons[10]);
		inputMapDialog.add(inputButtons[11]);
		inputMapDialog.add(inputButtons[12]);

		inputMapDialog.add(inputButtons[13]);
		inputMapDialog.add(inputButtons[14]);
		inputMapDialog.add(inputButtons[15]);

		inputMapDialog.add(inputButtons[16]);
		inputMapDialog.add(inputButtons[17]);
		inputMapDialog.add(inputButtons[18]);


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
		mapInputs.setActionCommand("MapInputs");
		applyInputs.setActionCommand("ApplyInputs");
		
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
		mapInputs.addActionListener(menuItemListener);
		applyInputs.addActionListener(menuItemListener);

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

				restartRequiredDialog.setLocationRelativeTo(main);
				restartRequiredDialog.setVisible(true);
			}
		});

		showFPS.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(showFPS.getState()){ Mobile.getPlatform().setShowFPS(true); }
				else{ Mobile.getPlatform().setShowFPS(false); }
			}
		});

		noAlphaOnBlankImages.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(noAlphaOnBlankImages.getState()){ config.updateAlphaSpeedHack("on"); hasPendingChange = true; }
				else{ config.updateAlphaSpeedHack("off"); hasPendingChange = true; }

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
					lgLayout.setState(false);
					motorolaLayout.setState(false);
					motov8Layout.setState(false);
					tripletsLayout.setState(false);
					nokiaLayout.setState(false);
					nokiaKbLayout.setState(false);
					sagemLayout.setState(false);
					siemensLayout.setState(false);
					siemensOldLayout.setState(false);
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
					lgLayout.setState(false);
					motorolaLayout.setState(false);
					motov8Layout.setState(false);
					tripletsLayout.setState(false);
					nokiaKbLayout.setState(false);
					sagemLayout.setState(false);
					siemensLayout.setState(false);
					siemensOldLayout.setState(false);
					hasPendingChange = true;
				}
			}
		});

		nokiaKbLayout.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(!nokiaKbLayout.getState()){ nokiaKbLayout.setState(true); }
				if(nokiaKbLayout.getState())
				{ 
					config.updatePhone("NokiaKeyboard");
					stdLayout.setState(false);
					lgLayout.setState(false);
					motorolaLayout.setState(false);
					motov8Layout.setState(false);
					tripletsLayout.setState(false);
					nokiaLayout.setState(false);
					sagemLayout.setState(false);
					siemensLayout.setState(false);
					siemensOldLayout.setState(false);
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
					lgLayout.setState(false);
					motorolaLayout.setState(false);
					motov8Layout.setState(false);
					tripletsLayout.setState(false);
					nokiaLayout.setState(false);
					nokiaKbLayout.setState(false);
					sagemLayout.setState(false);
					siemensOldLayout.setState(false);
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
					lgLayout.setState(false);
					motov8Layout.setState(false);
					tripletsLayout.setState(false);
					nokiaLayout.setState(false);
					nokiaKbLayout.setState(false);
					sagemLayout.setState(false);
					siemensLayout.setState(false);
					siemensOldLayout.setState(false);
					hasPendingChange = true;
				}
			}
		});

		lgLayout.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(!lgLayout.getState()){ lgLayout.setState(true); }
				if(lgLayout.getState())
				{ 
					config.updatePhone("LG");
					stdLayout.setState(false);
					motorolaLayout.setState(false);
					motov8Layout.setState(false);
					tripletsLayout.setState(false);
					nokiaLayout.setState(false);
					nokiaKbLayout.setState(false);
					sagemLayout.setState(false);
					siemensLayout.setState(false);
					siemensOldLayout.setState(false);
					hasPendingChange = true;
				}
			}
		});

		motov8Layout.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(!motov8Layout.getState()){ motov8Layout.setState(true); }
				if(motov8Layout.getState())
				{ 
					config.updatePhone("MotoV8");
					stdLayout.setState(false);
					lgLayout.setState(false);
					motorolaLayout.setState(false);
					tripletsLayout.setState(false);
					nokiaLayout.setState(false);
					nokiaKbLayout.setState(false);
					sagemLayout.setState(false);
					siemensLayout.setState(false);
					siemensOldLayout.setState(false);
					hasPendingChange = true;
				}
			}
		});

		tripletsLayout.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(!tripletsLayout.getState()){ tripletsLayout.setState(true); }
				if(tripletsLayout.getState())
				{ 
					config.updatePhone("MotoTriplets");
					stdLayout.setState(false);
					lgLayout.setState(false);
					motov8Layout.setState(false);
					motorolaLayout.setState(false);
					nokiaLayout.setState(false);
					nokiaKbLayout.setState(false);
					sagemLayout.setState(false);
					siemensLayout.setState(false);
					siemensOldLayout.setState(false);
					hasPendingChange = true;
				}
			}
		});

		sagemLayout.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(!sagemLayout.getState()){ sagemLayout.setState(true); }
				if(sagemLayout.getState())
				{ 
					config.updatePhone("Sagem");
					stdLayout.setState(false);
					lgLayout.setState(false);
					motov8Layout.setState(false);
					motorolaLayout.setState(false);
					nokiaLayout.setState(false);
					nokiaKbLayout.setState(false);
					tripletsLayout.setState(false);
					siemensLayout.setState(false);
					siemensOldLayout.setState(false);
					hasPendingChange = true;
				}
			}
		});

		siemensOldLayout.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(!siemensOldLayout.getState()){ siemensOldLayout.setState(true); }
				if(siemensOldLayout.getState())
				{ 
					config.updatePhone("SiemensOld");
					stdLayout.setState(false);
					lgLayout.setState(false);
					motov8Layout.setState(false);
					motorolaLayout.setState(false);
					nokiaLayout.setState(false);
					nokiaKbLayout.setState(false);
					tripletsLayout.setState(false);
					siemensLayout.setState(false);
					sagemLayout.setState(false);
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

		logDisabled.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(!logDisabled.getState()){ logDisabled.setState(true); }
				if(logDisabled.getState())
				{ 
					logDebug.setState(false);
					logInfo.setState(false);
					logWarning.setState(false);
					logError.setState(false);

					Mobile.logging = false;
				}
			}
		});

		logDebug.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(!logDebug.getState()){ logDebug.setState(true); }
				if(logDebug.getState())
				{ 
					logDisabled.setState(false);
					logInfo.setState(false);
					logWarning.setState(false);
					logError.setState(false);

					Mobile.logging = true; 
					Mobile.minLogLevel = 0;
				}
			}
		});

		logInfo.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(!logInfo.getState()){ logInfo.setState(true); }
				if(logInfo.getState())
				{ 
					logDisabled.setState(false);
					logDebug.setState(false);
					logWarning.setState(false);
					logError.setState(false);

					Mobile.logging = true; 
					Mobile.minLogLevel = 1;
				}
			}
		});

		logWarning.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(!logWarning.getState()){ logWarning.setState(true); }
				if(logWarning.getState())
				{ 
					logDisabled.setState(false);
					logDebug.setState(false);
					logInfo.setState(false);
					logError.setState(false);

					Mobile.logging = true; 
					Mobile.minLogLevel = 2;
				}
			}
		});

		logError.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(!logError.getState()){ logError.setState(true); }
				if(logError.getState())
				{ 
					logDisabled.setState(false);
					logDebug.setState(false);
					logInfo.setState(false);
					logWarning.setState(false);

					Mobile.logging = true; 
					Mobile.minLogLevel = 3;
				}
			}
		});
		


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
		optionMenu.add(showFPS);
		optionMenu.add(phoneType);
		optionMenu.add(fpsCap);
		optionMenu.add(mapInputs);
		optionMenu.add(speedHackMenu);

		logLevel.add(logDisabled);
		logLevel.add(logDebug);
		logLevel.add(logInfo);
		logLevel.add(logWarning);
		logLevel.add(logError);

		logDisabled.setState(false);
		logDebug.setState(false);
		logInfo.setState(true);
		logWarning.setState(false);
		logError.setState(false);

		debugMenu.add(dumpAudioData);
		debugMenu.add(dumpGraphicsData);
		debugMenu.add(showMemoryUsage);
		debugMenu.add(logLevel);

		for(int i = 0; i < config.supportedResolutions.length; i++) { resChoice.add(config.supportedResolutions[i]); }

		phoneType.add(stdLayout);
		phoneType.add(lgLayout);
		phoneType.add(motorolaLayout);
		phoneType.add(tripletsLayout);
		phoneType.add(motov8Layout);
		phoneType.add(nokiaLayout);
		phoneType.add(nokiaKbLayout);
		phoneType.add(sagemLayout);
		phoneType.add(siemensLayout);
		phoneType.add(siemensOldLayout);
		
		fpsCap.add(fpsCapNone);
		fpsCap.add(fpsCap60);
		fpsCap.add(fpsCap30);
		fpsCap.add(fpsCap15);

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
			fpsCapNone.setState(config.settings.get("fps").equals("0"));
			fpsCap15.setState(config.settings.get("fps").equals("15"));
			fpsCap30.setState(config.settings.get("fps").equals("30"));
			fpsCap60.setState(config.settings.get("fps").equals("60"));

			stdLayout.setState(config.settings.get("phone").equals("Standard"));
			nokiaLayout.setState(config.settings.get("phone").equals("Nokia"));
			siemensLayout.setState(config.settings.get("phone").equals("Siemens"));
			siemensOldLayout.setState(config.settings.get("phone").equals("SiemensOld"));
			motov8Layout.setState(config.settings.get("phone").equals("MotoV8"));
			tripletsLayout.setState(config.settings.get("phone").equals("MotoTriplets"));
			sagemLayout.setState(config.settings.get("phone").equals("Sagem"));
			nokiaKbLayout.setState(config.settings.get("phone").equals("NokiaKeyboard"));
			lgLayout.setState(config.settings.get("phone").equals("LG"));
			motorolaLayout.setState(config.settings.get("phone").equals("Motorola"));

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
			}

			else if (a.getActionCommand() == "CancelResChange") { resDialog.setVisible(false); }

			else if(a.getActionCommand() == "CloseFreeJ2ME") { System.exit(0); }

			else if(a.getActionCommand() == "RestartLater") { restartRequiredDialog.setVisible(false); }

			else if(a.getActionCommand() == "MapInputs") { inputMapDialog.setLocation(main.getLocation().x, main.getLocation().y); inputMapDialog.setVisible(true); }

			/* TODO: Flesh out input mappings apply and file saving (preferably per-game, though a global config could also work great) */
			else if(a.getActionCommand() == "ApplyInputs") 
			{
				System.arraycopy(newInputKeycodes, 0, inputKeycodes, 0, inputKeycodes.length);
				inputMapDialog.setVisible(false); 
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
