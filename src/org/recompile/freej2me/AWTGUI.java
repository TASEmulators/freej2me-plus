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

import java.awt.BorderLayout;
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
import java.awt.ScrollPane;
import java.awt.TextArea;
import java.awt.MenuBar;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;

import java.util.Arrays;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.MobilePlatform;

public final class AWTGUI 
{
	final String VERSION = "1.52";
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
	final Menu DoJaVersion = new Menu("DoJa API Version");
	final Menu screenRotation = new Menu("Screen Rotation (Ctrl+Alt+R)");
	final Menu backlightColor = new Menu("Backlight Color");
	final Menu fontOffset = new Menu("Font Size Offset");

	public final String[] supportedResolutions = {"96x65","101x64","101x80","128x128","130x130","120x160","128x160","160x128","132x176","208x173","176x208","176x220","220x176","208x208","220x220","180x320","320x180","240x240","240x260","208x320","240x320","320x240","240x400","400x240","320x320","240x432","240x480","360x360","352x416","360x480","360x640","640x360","480x640","640x480","345x800","800x345","480x800","800x480"};

	/* Dialogs for resolution changes, restart notifications, MemStats and info about FreeJ2ME */
	final Dialog[] awtDialogs = 
	{
		new Dialog(main , "Set LCD Resolution", true),
		new Dialog(main , "About FreeJ2ME", true),
		new Dialog(main, "FreeJ2ME MemStat", false),
		new Dialog(main, "Restart Required", true),
		new Dialog(main, "Key Mapping", true),
		new Dialog(main, "Console Log", false),
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

	/* M3G Debug submenu */
	final Menu MCV3Debug = new Menu("MascotCapsuleV3 Debugging");

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
		new Button("NumPad-2"),
		new Button("NumPad-3"),
		new Button("E"),
		new Button("NumPad-0"),
		new Button("R"),
		new Button("A"),
		new Button("Space"),
		new Button("C"),
		new Button("X")
	};

	/* Array of inputs in order to support input remapping */
	public static int inputKeycodes[] = new int[] 
	{
		KeyEvent.VK_Q, KeyEvent.VK_W, 
		KeyEvent.VK_UP, KeyEvent.VK_LEFT, KeyEvent.VK_ENTER, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN, 
		KeyEvent.VK_NUMPAD7, KeyEvent.VK_NUMPAD8, KeyEvent.VK_NUMPAD9, 
		KeyEvent.VK_NUMPAD4, KeyEvent.VK_NUMPAD5, KeyEvent.VK_NUMPAD6, 
		KeyEvent.VK_NUMPAD1, KeyEvent.VK_NUMPAD2, KeyEvent.VK_NUMPAD3, 
		KeyEvent.VK_E, KeyEvent.VK_NUMPAD0, KeyEvent.VK_R, KeyEvent.VK_A,
		KeyEvent.VK_SPACE, KeyEvent.VK_C, KeyEvent.VK_X
	};

	private final int newInputKeycodes[] = Arrays.copyOf(inputKeycodes, inputKeycodes.length);

	final Choice resChoice = new Choice();

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
	final CheckboxMenuItem enableAudio = new CheckboxMenuItem("Enable Audio", false);
	final CheckboxMenuItem useCustomMidi = new CheckboxMenuItem("Use custom midi soundfont", false);
	final CheckboxMenuItem useCustomFont = new CheckboxMenuItem("Use custom text font", false);

	final CheckboxMenuItem[] dojaVersions = 
	{
		new CheckboxMenuItem("DoJa-1.0", false),
		new CheckboxMenuItem("DoJa-2.0 & 1.5 OE", false),
		new CheckboxMenuItem("DoJa-3.0 & 2.5 OE", false),
		new CheckboxMenuItem("DoJa-3.5", false),
		new CheckboxMenuItem("DoJa-4.0", false),
		new CheckboxMenuItem("DoJa-4.1", false),
		new CheckboxMenuItem("DoJa-5.0", false),
		new CheckboxMenuItem("DoJa-5.1", false),
		new CheckboxMenuItem("Star-1.0", false),
		new CheckboxMenuItem("Star-1.1", false),
		new CheckboxMenuItem("Star-1.2", false),
		new CheckboxMenuItem("Star-1.3", false),
		new CheckboxMenuItem("Star-1.5", false),
		new CheckboxMenuItem("Star-2.0", true)
	};
	final String[] dojaVersionValues = {"10", "20", "30", "35", "40", "41", "50", "51", "100", "110", "120", "130", "150", "200"};

	final CheckboxMenuItem[] rotations = 
	{
		new CheckboxMenuItem("No rotation", true),
		new CheckboxMenuItem("90 degrees",  false),
		new CheckboxMenuItem("180 degrees", false),
		new CheckboxMenuItem("270 degrees", false)
	};
	final String[] rotationValues = {"0", "90", "180", "270"};

	final CheckboxMenuItem[] layoutOptions = 
	{
		new CheckboxMenuItem("Default", true),
		new CheckboxMenuItem("KDDI", false),
		new CheckboxMenuItem("LG", false),
		new CheckboxMenuItem("Motorola/SoftBank", false),
		new CheckboxMenuItem("Motorola V8", false),
		new CheckboxMenuItem("Motorola Triplets", false),
		new CheckboxMenuItem("Motorola A1000", false),
		new CheckboxMenuItem("Nokia Full Keyboard", false),
		new CheckboxMenuItem("Sagem", false),
		new CheckboxMenuItem("Sharp", false),
		new CheckboxMenuItem("Siemens", false),
		new CheckboxMenuItem("SKT", false)
	};
	final String[] layoutValues = {"Standard", "KDDI", "LG", "Motorola", "MotoV8", "MotoTriplets", "MotoA1000", "NokiaKeyboard", "Sagem", "Sharp", "Siemens", "SKT"};
	
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
	final CheckboxMenuItem M3GHalfRes = new CheckboxMenuItem("Render M3G at Half Resolution");
	final CheckboxMenuItem MCV3HalfRes = new CheckboxMenuItem("Render MascotCapsuleV3 at Half Res");
	final CheckboxMenuItem MCV3NoLighting = new CheckboxMenuItem("Disable MascotCapsuleV3's lighting");

	// Compatibility settings
	final CheckboxMenuItem fantasyZoneFix = new CheckboxMenuItem("Fix for Fantasy Zone 176x208 weird mirroring");
	final CheckboxMenuItem transToOriginOnReset = new CheckboxMenuItem("Translate to origin on gfx reset");
	final CheckboxMenuItem immediateRepaints = new CheckboxMenuItem("Process canvas repaints immediately");
	final CheckboxMenuItem overridePlatChecks = new CheckboxMenuItem("Override Mobile Platform checks");
	final CheckboxMenuItem siemensFriendlyDrawing = new CheckboxMenuItem("Siemens-friendly drawing methods");
	final CheckboxMenuItem ignoreVolumeChanges = new CheckboxMenuItem("Ignore volume changes");
	final CheckboxMenuItem MCV3HorFovFix = new CheckboxMenuItem("MascotCapsuleV3 Horizontal FOV Fix");

	final CheckboxMenuItem deleteTemporaryKJXFiles = new CheckboxMenuItem("Delete KJX files' temporary JAR/JAD");
	final CheckboxMenuItem dumpAudioData = new CheckboxMenuItem("Dump Audio Streams");
	final CheckboxMenuItem dumpGraphicsData = new CheckboxMenuItem("Dump Graphics Objects");
	final CheckboxMenuItem showDebugWindows = new CheckboxMenuItem("Show Debug Windows");
	
	// M3G Debugging
	final CheckboxMenuItem M3GUntextured = new CheckboxMenuItem("Draw Only Vertex Colors");
	final CheckboxMenuItem M3GWireframe = new CheckboxMenuItem("Wireframe Mode");

	// MascotCapsuleV3 Debugging
	final CheckboxMenuItem MCV3ShowHeapUsage = new CheckboxMenuItem("Show Heap Usage");
	final CheckboxMenuItem MCV3ShowTimeMetrics = new CheckboxMenuItem("Show Time Metrics");

	final TextArea logArea = new TextArea();
	final TextArea memArea = new TextArea();
	final Font dialogFont = new Font(Font.DIALOG, Font.BOLD, 12);

	private StringBuilder debugContent = null;
	private BufferedReader logReader = null;

	public AWTGUI(Config config)
	{
		this.config = config;

		debugContent = new StringBuilder();
		try { logReader = new BufferedReader(new FileReader(Mobile.logFile)); }
		catch(Exception e) { System.out.println("Failed to create log window writer:" + e.getMessage()); }

		resChoice.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
		resChoice.setBackground(FreeJ2ME.freeJ2MEBGColor);
		resChoice.setForeground(Color.ORANGE);

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
		awtDialogs[0].setLayout( new FlowLayout(FlowLayout.CENTER, 60, 3));
		awtDialogs[0].setUndecorated(true);
		awtDialogs[0].setSize(230, 125);
		awtDialogs[0].setResizable(false);
		awtDialogs[0].setLocationRelativeTo(main);
		awtDialogs[0].add(new Label("Select a Resolution from the Dropdown"));
		awtDialogs[0].add(new Label("Then hit 'Apply'!"));
		awtDialogs[0].add(resChoice);
		awtDialogs[0].add(awtButtons[1]);
		awtDialogs[0].add(awtButtons[2]);

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

		awtDialogs[4].add(new Label("CLR:"));
		awtDialogs[4].add(inputButtons[19]);
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
		awtDialogs[4].add(new Label("(Ctrl+Alt+*)"));

		awtDialogs[4].add(new Label(""));
		awtDialogs[4].add(new Label(""));
		awtDialogs[4].add(new Label(""));
		
		awtDialogs[4].add(new Label("Fast-Forward"));
		awtDialogs[4].add(new Label("Screenshot"));
		awtDialogs[4].add(new Label("Pause/Resume"));

		awtDialogs[4].add(inputButtons[20]);
		awtDialogs[4].add(inputButtons[21]);
		awtDialogs[4].add(inputButtons[22]);

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
		awtDialogs[3].setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));  
		awtDialogs[3].setUndecorated(true);
		awtDialogs[3].setSize(230, 80);
		awtDialogs[3].setLocationRelativeTo(main);
		awtDialogs[3].add(new Label("This change requires a restart to apply!"));
		awtDialogs[3].add(awtButtons[3]);
		awtDialogs[3].add(awtButtons[4]);


		// Mem stats window
		memArea.setBackground(FreeJ2ME.freeJ2MEBGColor);
		memArea.setForeground(Color.ORANGE);
        memArea.setEditable(false); // Make the log area read-only

		ScrollPane memScrollPane = new ScrollPane(ScrollPane.SCROLLBARS_NEVER);
        memScrollPane.add(memArea);

		awtDialogs[2].setBackground(FreeJ2ME.freeJ2MEBGColor);
		awtDialogs[2].setForeground(Color.ORANGE);
		awtDialogs[2].setLayout(new BorderLayout());
		awtDialogs[2].setSize(200, 80);
		awtDialogs[2].setFont(dialogFont);
		awtDialogs[2].setResizable(false);
		awtDialogs[2].setUndecorated(true);
		awtDialogs[2].add(memScrollPane, BorderLayout.CENTER);

		// Console Log window
		logArea.setBackground(FreeJ2ME.freeJ2MEBGColor);
		logArea.setForeground(Color.ORANGE);
        logArea.setEditable(false); // Make the log area read-only

		ScrollPane logScrollPane = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
        logScrollPane.add(logArea);
		
		awtDialogs[5].setBackground(FreeJ2ME.freeJ2MEBGColor);
        awtDialogs[5].setForeground(Color.ORANGE);
		awtDialogs[5].setLayout(new BorderLayout());
		awtDialogs[5].setSize(720, 320);
		awtDialogs[5].setFont(dialogFont);
		awtDialogs[5].setLocationRelativeTo(main);
		awtDialogs[5].setResizable(false);
		awtDialogs[5].setUndecorated(true);
        awtDialogs[5].add(logScrollPane, BorderLayout.CENTER);

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

	public void updateDialogLocations(Frame mainFrame) 
	{
		awtDialogs[2].setLocation(mainFrame.getLocation().x+mainFrame.getSize().width, mainFrame.getLocation().y);
		awtDialogs[5].setLocation(mainFrame.getLocation().x+mainFrame.getSize().width, mainFrame.getLocation().y+awtDialogs[2].getHeight());
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

		// DoJa Version
		for(byte i = 0; i < dojaVersions.length; i++) 
		{
			final byte index = i;
			dojaVersions[i].addItemListener(new ItemListener() 
			{
				public void itemStateChanged(ItemEvent e) 
				{
					if(!dojaVersions[index].getState()){ dojaVersions[index].setState(true); }
					if(dojaVersions[index].getState())
					{ 
						config.updateDoJaVersion(dojaVersionValues[index]);
						for(int j = 0; j < dojaVersions.length; j++) 
						{
							if(j != index) { dojaVersions[j].setState(false); }
						}
						hasPendingChange = true;

						showRestartDialog();
					}
				}
			});
		}

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

		M3GHalfRes.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(M3GHalfRes.getState()){ config.updateM3GResSpeedHack("on"); hasPendingChange = true; }
				else{ config.updateM3GResSpeedHack("off"); hasPendingChange = true; }
			}
		});

		MCV3HalfRes.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(MCV3HalfRes.getState()){ config.updateMCV3ResSpeedHack("on"); hasPendingChange = true; }
				else{ config.updateMCV3ResSpeedHack("off"); hasPendingChange = true; }

				showRestartDialog();
			}
		});

		MCV3NoLighting.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(MCV3NoLighting.getState()){ config.updateMCV3NoLightingSpeedHack("on"); hasPendingChange = true; }
				else{ config.updateMCV3NoLightingSpeedHack("off"); hasPendingChange = true; }

				showRestartDialog();
			}
		});

		// Compatibility settings
		fantasyZoneFix.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(fantasyZoneFix.getState()){ config.updateCompatFantasyZoneFix("on"); hasPendingChange = true; }
				else{ config.updateCompatFantasyZoneFix("off"); hasPendingChange = true; }

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

		ignoreVolumeChanges.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				if(ignoreVolumeChanges.getState()){ config.updateCompatIgnoreVolumeChanges("on"); hasPendingChange = true; }
				else{ config.updateCompatIgnoreVolumeChanges("off"); hasPendingChange = true; }
			}
		});

		MCV3HorFovFix.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				if(MCV3HorFovFix.getState()){ config.updateCompatMCV3HorizFovFix("on"); hasPendingChange = true; }
				else{ config.updateCompatMCV3HorizFovFix("off"); hasPendingChange = true; }
			}
		});

		// Screen rotations
		for(byte i = 0; i < rotations.length; i++) 
		{
			final byte index = i;
			rotations[i].addItemListener(new ItemListener() 
			{
				public void itemStateChanged(ItemEvent e) 
				{
					if(!rotations[index].getState()){ rotations[index].setState(true); }
					if(rotations[index].getState())
					{ 
						config.updateRotate(rotationValues[index]);
						for(int j = 0; j < rotations.length; j++) 
						{
							if(j != index) { rotations[j].setState(false); }
						}
						hasPendingChange = true;
					}
				}
			});
		}

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

		MCV3ShowHeapUsage.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(MCV3ShowHeapUsage.getState()) { config.MCV3ShowHeapUsage("on"); Mobile.MCV3ShowHeapUsage = true; }
				else { config.MCV3ShowHeapUsage("off"); Mobile.MCV3ShowHeapUsage = false; }
			}
		});

		MCV3ShowTimeMetrics.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				if(MCV3ShowTimeMetrics.getState()) { config.MCV3ShowTimeMetrics("on"); Mobile.MCV3ShowTimeMetrics = true; }
				else { config.MCV3ShowTimeMetrics("off"); Mobile.MCV3ShowTimeMetrics = false; }
			}
		});

		// These are specific to AWTGUI
		showDebugWindows.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent e) 
			{
				/* Mem stats frame won't be centered on FreeJ2ME's frame, instead, it will sit right by its side, that's why "setLocationRelativeTo(main)" isn't used */
				if(showDebugWindows.getState()) 
				{ 
					updateDialogLocations(main); 
					awtDialogs[2].setVisible(true); 
					awtDialogs[5].setVisible(true); 
				}
				else 
				{ 
					awtDialogs[2].setVisible(false); 
					awtDialogs[5].setVisible(false); 
				}
			}
		});
	}

	private void buildMenuBar() 
	{
		closeMenuItem.setEnabled(false);
		restartMenuItem.setEnabled(false);
		pauseRes.setEnabled(false);
		scrShot.setEnabled(false);

		//add menu items to menus
		fileMenu.add(openMenuItem);
		fileMenu.add(restartMenuItem);
		fileMenu.add(closeMenuItem);
		fileMenu.addSeparator();
		fileMenu.add(scrShot);
		fileMenu.add(pauseRes);
		fileMenu.addSeparator();
		fileMenu.add(showPlayer);
		fileMenu.addSeparator();
		fileMenu.add(aboutMenuItem);
		fileMenu.add(exitMenuItem);

		optionMenu.add(fullScreen);
		optionMenu.add(enableAudio);
		optionMenu.add(useCustomMidi);
		optionMenu.add(useCustomFont);
		optionMenu.add(resChangeMenuItem);
		optionMenu.add(mapInputs);
		optionMenu.add(phoneType);
		optionMenu.add(DoJaVersion);
		optionMenu.add(screenRotation);
		optionMenu.add(backlightColor);
		optionMenu.add(fpsCap);
		optionMenu.add(unlockFPSHack);
		optionMenu.add(fontOffset);
		optionMenu.add(speedHackMenu);
		optionMenu.add(compatSettingsMenu);

		optionMenu.setEnabled(false);

		debugMenu.add(showFPS);
		debugMenu.add(deleteTemporaryKJXFiles);
		debugMenu.add(dumpAudioData);
		debugMenu.add(dumpGraphicsData);
		debugMenu.add(showDebugWindows);
		debugMenu.add(logLevel);
		debugMenu.add(M3GDebug);
		debugMenu.add(MCV3Debug);

		debugMenu.setEnabled(false);
		
		deleteTemporaryKJXFiles.setState(true);

		// Internally log levels are ordered in decreasing verbosity level
		// But UI is ordered by increasing verbosity.
		logLevel.add(logLevels[0]);
		for(int i = logLevels.length-1; i > 0; i--) { logLevel.add(logLevels[i]); }

		logLevels[0].setState(false);
		logLevels[1].setState(false);
		logLevels[2].setState(true);
		logLevels[3].setState(false);
		logLevels[4].setState(false);

		M3GDebug.add(M3GUntextured);
		M3GDebug.add(M3GWireframe);

		MCV3Debug.add(MCV3ShowHeapUsage);
		MCV3Debug.add(MCV3ShowTimeMetrics);

		for(int i = 0; i < supportedResolutions.length; i++) { resChoice.add(supportedResolutions[i]); }
		for(int i = 0; i < dojaVersions.length; i++) { DoJaVersion.add(dojaVersions[i]); }
		for(int i = 0; i < rotations.length; i++) { screenRotation.add(rotations[i]); }
		for(int i = 0; i < layoutOptions.length; i++) { phoneType.add(layoutOptions[i]); }
		for(int i = 0; i < backlightOptions.length; i++) { backlightColor.add(backlightOptions[i]); }
		for(int i = 0; i < fpsOptions.length; i++) { fpsCap.add(fpsOptions[i]); }
		for(int i = 0; i < fpsHackOptions.length; i++) { unlockFPSHack.add(fpsHackOptions[i]); }
		for(int i = 0; i < fpsCounterPos.length; i++) { showFPS.add(fpsCounterPos[i]); }
		for(int i = 0; i < fontOffsets.length; i++) { fontOffset.add(fontOffsets[i]); }

		speedHackMenu.add(noAlphaOnBlankImages);
		speedHackMenu.add(M3GHalfRes);
		speedHackMenu.add(MCV3HalfRes);
		speedHackMenu.add(MCV3NoLighting);

		compatSettingsMenu.add(fantasyZoneFix);
		compatSettingsMenu.add(transToOriginOnReset);
		compatSettingsMenu.add(immediateRepaints);
		compatSettingsMenu.add(overridePlatChecks);
		compatSettingsMenu.add(siemensFriendlyDrawing);
		compatSettingsMenu.add(ignoreVolumeChanges);
		compatSettingsMenu.add(MCV3HorFovFix);

		// add menus to menubar
		menuBar.add(fileMenu);
		menuBar.add(optionMenu);
		menuBar.add(debugMenu);
	}

	public void updateOptions() 
	{
			fullScreen.setState(FreeJ2ME.isFullscreen);
			enableAudio.setState(config.sysSettings.get("sound").equals("on"));
			useCustomMidi.setState(config.sysSettings.get("soundfont").equals("Custom"));
			useCustomFont.setState(config.sysSettings.get("textfont").equals("Custom"));

			for(int i = 0; i < dojaVersions.length; i++) { dojaVersions[i].setState(config.settings.get("dojaversion").equals(dojaVersionValues[i])); }

			for(int i = 0; i < rotations.length; i++) { rotations[i].setState(config.settings.get("rotate").equals(rotationValues[i])); }

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

			M3GHalfRes.setState(config.settings.get("spdhackm3ghalfres").equals("on"));

			MCV3HalfRes.setState(config.settings.get("spdhackmcv3halfres").equals("on"));

			MCV3NoLighting.setState(config.settings.get("spdhackmcv3nolighting").equals("on"));

			fantasyZoneFix.setState(config.settings.get("compatfantasyzonefix").equals("on"));

			transToOriginOnReset.setState(config.settings.get("compattranstooriginonreset").equals("on"));

			immediateRepaints.setState(config.settings.get("compatimmediaterepaints").equals("on"));

			overridePlatChecks.setState(config.settings.get("compatoverrideplatchecks").equals("on"));

			siemensFriendlyDrawing.setState(config.settings.get("compatsiemensfriendlydrawing").equals("on"));

			ignoreVolumeChanges.setState(config.settings.get("compatignorevolumechanges").equals("on"));

			MCV3HorFovFix.setState(config.settings.get("compatmcv3horizfovfix").equals("on"));

			resChoice.select(""+ Integer.parseInt(config.settings.get("scrwidth")) + "x" + ""+ Integer.parseInt(config.settings.get("scrheight")));

			// Sys Settings
			for(int i = 0; i < logLevels.length; i++) { logLevels[i].setState(config.sysSettings.get("logLevel").equals(logLevelValues[i])); }

			for(int i = 0; i < fpsCounterPos.length; i++) { fpsCounterPos[i].setState(config.sysSettings.get("fpsCounterPosition").equals(showFPSValues[i])); }

			dumpGraphicsData.setState(config.sysSettings.get("dumpGraphicsObjects").equals("on"));

			dumpAudioData.setState(config.sysSettings.get("dumpAudioStreams").equals("on"));
			
			M3GWireframe.setState(config.sysSettings.get("M3GWireframe").equals("on"));

			M3GUntextured.setState(config.sysSettings.get("M3GUntextured").equals("on"));

			MCV3ShowHeapUsage.setState(config.sysSettings.get("MCV3ShowHeapUsage").equals("on"));

			MCV3ShowTimeMetrics.setState(config.sysSettings.get("MCV3ShowTimeMetrics").equals("on"));

			deleteTemporaryKJXFiles.setState(config.sysSettings.get("deleteTempKJXFiles").equals("on"));

			// Get saved inputs from system config file.
			System.arraycopy(Config.inputKeycodes, 0, newInputKeycodes, 0, Config.inputKeycodes.length);
			for(int i = 0; i < inputButtons.length; i++) { inputButtons[i].setLabel(KeyEvent.getKeyText(newInputKeycodes[i])); }

			/* We only need to do this call once, when the jar first loads */
			firstLoad = false;
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
				System.arraycopy(newInputKeycodes, 0, Config.inputKeycodes, 0, newInputKeycodes.length);
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
		optionMenu.setEnabled(true);
		debugMenu.setEnabled(true);
		closeMenuItem.setEnabled(true);
		restartMenuItem.setEnabled(true);
		pauseRes.setEnabled(true);
		scrShot.setEnabled(true);

		main.setResizable(true);
	}

	public MenuBar getMenuBar() { return menuBar; }

	public boolean hasChanged() { return hasPendingChange; }

	public void clearChanged() { hasPendingChange = false; }

	public boolean hasLoadedFile() { return fileLoaded; }

	public void setMainFrame(Frame mainFrame) 
	{ 
		main = mainFrame; 
		// So that the console window and memory stats follow the main window around
		main.addComponentListener(new ComponentAdapter() 
		{
            public void componentMoved(ComponentEvent e) { updateDialogLocations(main); }
            public void componentResized(ComponentEvent e) { updateDialogLocations(main); }
        });

		main.setResizable(false);
	}

	public String getJarPath() { return jarfile; }

	public boolean hasJustLoaded() { return firstLoad; }

	public void showRestartDialog()
	{
		awtDialogs[3].setLocationRelativeTo(main);
		awtDialogs[3].setVisible(true);
	}

	public void updateDialogs() 
	{
		String line;
		try
		{
			while ((line = logReader.readLine()) != null) { debugContent.append(line).append("\n"); }
			logArea.setText(new String(debugContent.toString()));
			logArea.setCaretPosition(logArea.getText().length());
		} 
		catch (Exception e) { logArea.append("Error reading log file: " + e.getMessage() + "\n"); }

		memArea.setText
		(
			"Total Mem: " + (Runtime.getRuntime().totalMemory() / 1024) + " KB\n" + 
			"Free Mem : " + (Runtime.getRuntime().freeMemory() / 1024) + " KB\n" + 
			"Used Mem : " + ((Runtime.getRuntime().totalMemory() / 1024) - (Runtime.getRuntime().freeMemory() / 1024)) + " KB\n" + 
			"Max Mem  : " + (Runtime.getRuntime().maxMemory() / 1024) + " KB"
		);
    }
}
