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
	String spfile = "";

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

	/* Main M3G submenu */
	final Menu M3GSettings = new Menu("M3G Settings");

	final Menu m3gAAMenu = new Menu("Anti-Aliasing");
	final Menu m3gBilinearMenu = new Menu("Bilinear Filtering");
	final Menu m3gDitheringMenu = new Menu("Dithering");
	final Menu m3gPerspCorrMenu = new Menu("Perspective Correction");
	final Menu m3gPerspCorrFactMenu = new Menu("Perspective Correction Quality");
	final Menu m3gMipmapMenu = new Menu("Mipmapping");

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
	final MenuItem openSpMenuItem = new MenuItem("Open DoJa SP / SP0 File");
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
		new CheckboxMenuItem("Motorola/SoftBank/Sharp", false),
		new CheckboxMenuItem("Motorola V8", false),
		new CheckboxMenuItem("Motorola Triplets", false),
		new CheckboxMenuItem("Motorola A1000", false),
		new CheckboxMenuItem("Nokia Full Keyboard", false),
		new CheckboxMenuItem("Sagem", false),
		new CheckboxMenuItem("Siemens", false),
		new CheckboxMenuItem("SKT", false)
	};
	final String[] layoutValues = {"Standard", "KDDI", "LG", "Motorola", "MotoV8", "MotoTriplets", "MotoA1000", "NokiaKeyboard", "Sagem", "Siemens", "SKT"};

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
		new CheckboxMenuItem("Info", true),
		new CheckboxMenuItem("Warning", false),
		new CheckboxMenuItem("Error", false)
	};
	final String[] logLevelValues = {"0", "1", "2", "3", "4"};

	// Speedhacks
	final CheckboxMenuItem noAlphaOnBlankImages = new CheckboxMenuItem("No alpha on blank images");
	final CheckboxMenuItem MCV3HalfRes = new CheckboxMenuItem("Render MascotCapsuleV3 at Half Res");
	final CheckboxMenuItem MCV3NoLighting = new CheckboxMenuItem("Disable MascotCapsuleV3's lighting");

	// M3G Menu
	final CheckboxMenuItem M3GHalfRes = new CheckboxMenuItem("Halve Resolution");
	final CheckboxMenuItem M3GDisableFog = new CheckboxMenuItem("Disable Fog");
	final CheckboxMenuItem[] m3gAntiAliasValues =
	{
		new CheckboxMenuItem("Always Disabled", false),
		new CheckboxMenuItem("App-Controlled (Default)", true),
		new CheckboxMenuItem("Force-Enabled", false)
	};
	final CheckboxMenuItem[] m3gBilinearValues =
	{
		new CheckboxMenuItem("Always Disabled", false),
		new CheckboxMenuItem("App-Controlled (Default)", true),
		new CheckboxMenuItem("Force-Enabled", false)
	};
	final CheckboxMenuItem[] m3gDitheringValues =
	{
		new CheckboxMenuItem("Always Disabled", false),
		new CheckboxMenuItem("App-Controlled (Default)", true),
		new CheckboxMenuItem("Force-Enabled", false)
	};
	final CheckboxMenuItem[] m3gPerspCorrValues =
	{
		new CheckboxMenuItem("Always Disabled", false),
		new CheckboxMenuItem("App-Controlled (Default)", true),
		new CheckboxMenuItem("Force-Enabled", false)
	};
	final String[] m3gCommonSettingValues = {"off", "app", "on"};

	final CheckboxMenuItem[] m3gPerspCorrFactorValues =
	{
		new CheckboxMenuItem("Extra", false),
		new CheckboxMenuItem("High (Default)", true),
		new CheckboxMenuItem("Average", false),
		new CheckboxMenuItem("Low", false)
	};
	final String[] m3gPFactorSettingValues = {"extra", "high", "medium", "low"};

	final CheckboxMenuItem[] m3gMipmapValues =
	{
		new CheckboxMenuItem("Always Disabled", false),
		new CheckboxMenuItem("App-Controlled (Default)", true),
		new CheckboxMenuItem("Force-Nearest", false),
		new CheckboxMenuItem("Force-Linear", false)
	};
	final String[] m3gMipmapSettingValues = {"off", "app", "nearest", "linear"};

	// Compatibility settings
	final CheckboxMenuItem fantasyZoneFix = new CheckboxMenuItem("Fix for Fantasy Zone 176x208 weird mirroring");
	final CheckboxMenuItem transToOriginOnReset = new CheckboxMenuItem("Translate to origin on gfx reset");
	final CheckboxMenuItem immediateRepaints = new CheckboxMenuItem("Process canvas repaints immediately");
	final CheckboxMenuItem repaintOnSetCurrent = new CheckboxMenuItem("Repaint on Display setCurrent.");
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
		openSpMenuItem.setActionCommand("OpenSp");
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
		openSpMenuItem.addActionListener(menuItemListener);
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
		// Fullscreen is specific to AWTGUI.
		fullScreen.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				if (hasLoadedFile()) { FreeJ2ME.app.toggleFullscreen(); }
				else { fullScreen.setState(FreeJ2ME.isFullscreen); }
			}
		});

		// Per-App Radio Group Settings.
		bindRadioGroup(dojaVersions, dojaVersionValues, "dojaversion", false, new Runnable()
		{
			public void run() { showRestartDialog(); }
		});
		bindRadioGroup(rotations, rotationValues, "rotate");
		bindRadioGroup(layoutOptions, layoutValues, "phone");
		bindRadioGroup(backlightOptions, backlightValues, "backlightcolor");
		bindRadioGroup(fpsOptions, fpsValues, "fps");
		bindRadioGroup(fpsHackOptions, fpsHackValues, "fpshack");
		bindRadioGroup(fontOffsets, fontOffsetValues, "fontoffset");
		bindRadioGroup(m3gAntiAliasValues, m3gCommonSettingValues, "m3gantialiasmode");
		bindRadioGroup(m3gBilinearValues, m3gCommonSettingValues, "m3gbilinearmode");
		bindRadioGroup(m3gDitheringValues, m3gCommonSettingValues, "m3gditheringmode");
		bindRadioGroup(m3gPerspCorrValues, m3gCommonSettingValues, "m3gperspcorrmode");
		bindRadioGroup(m3gPerspCorrFactorValues, m3gPFactorSettingValues, "m3gperspcorrsubfactor");
		bindRadioGroup(m3gMipmapValues, m3gMipmapSettingValues, "m3gmipmapmode");

		// ShowFPS is a bit different in which it calls for setShowFPS().
		bindRadioGroup(fpsCounterPos, showFPSValues, "fpsCounterPosition", true, new Runnable()
		{
			public void run()
			{
				for (int i = 0; i < fpsCounterPos.length; i++)
				{
					if (fpsCounterPos[i].getState())
					{
						Mobile.getPlatform().setShowFPS(showFPSValues[i]);
						break;
					}
				}
			}
		});
		bindRadioGroup(logLevels, logLevelValues, "logLevel", true, null);

		// Per-App Toggleable settings
		setToggle(noAlphaOnBlankImages, "spdhacknoalpha", true);
		setToggle(M3GHalfRes, "spdhackm3ghalfres", false);
		setToggle(M3GDisableFog, "m3gdisablefog", false);
		setToggle(MCV3HalfRes, "spdhackmcv3halfres", true);
		setToggle(MCV3NoLighting, "spdhackmcv3nolighting", true);
		setToggle(fantasyZoneFix, "compatfantasyzonefix", true);
		setToggle(transToOriginOnReset, "compattranstooriginonreset", false);
		setToggle(immediateRepaints, "compatimmediaterepaints", false);
		setToggle(repaintOnSetCurrent, "compatrepaintonsetcurrent", false);
		setToggle(overridePlatChecks, "compatoverrideplatchecks", true);
		setToggle(siemensFriendlyDrawing, "compatsiemensfriendlydrawing", true);
		setToggle(ignoreVolumeChanges, "compatignorevolumechanges", false);
		setToggle(MCV3HorFovFix, "compatmcv3horizfovfix", false);

		// Special System Toggleable Settings. No use trying to commonize those
		// in a "setSysToggle" since they are only two.
		useCustomMidi.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				config.updateSysSetting("soundfont", useCustomMidi.getState() ? "Custom" : "Default");
				hasPendingChange = true;
			}
		});

		useCustomFont.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				config.updateSysSetting("textfont", useCustomFont.getState() ? "Custom" : "Default");
				hasPendingChange = true;
				showRestartDialog();
			}
		});

		// System toggleable settings.
		setSysToggle(enableAudio, "sound");
		setSysToggle(deleteTemporaryKJXFiles, "deleteTempKJXFiles");
		setSysToggle(dumpAudioData, "dumpAudioStreams");
		setSysToggle(dumpGraphicsData, "dumpGraphicsObjects");
		setSysToggle(M3GUntextured, "M3GUntextured");
		setSysToggle(M3GWireframe, "M3GWireframe");
		setSysToggle(MCV3ShowHeapUsage, "MCV3ShowHeapUsage");
		setSysToggle(MCV3ShowTimeMetrics, "MCV3ShowTimeMetrics");

		// Debug Windows are exclusive to AWTGUI.
		showDebugWindows.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				boolean show = showDebugWindows.getState();
				if (show) { updateDialogLocations(main); }
				awtDialogs[2].setVisible(show);
				awtDialogs[5].setVisible(show);
			}
		});
	}

	private void setToggle(final CheckboxMenuItem checkbox, final String settingKey, final boolean requiresRestart)
	{
		checkbox.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				config.updateSetting(settingKey, checkbox.getState() ? "on" : "off");
				hasPendingChange = true;
				if (requiresRestart) { showRestartDialog(); }
			}
		});
	}

	private void setSysToggle(final CheckboxMenuItem checkbox, final String sysSettingKey)
	{
		checkbox.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				boolean state = checkbox.getState();
				config.updateSysSetting(sysSettingKey, state ? "on" : "off");

				if (sysSettingKey.equals("deleteTempKJXFiles"))        { Mobile.deleteTemporaryKJXFiles = state; }
				else if (sysSettingKey.equals("dumpAudioStreams"))     { Mobile.dumpAudioStreams = state; }
				else if (sysSettingKey.equals("dumpGraphicsObjects"))  { Mobile.dumpGraphicsObjects = state; }
				else if (sysSettingKey.equals("M3GUntextured"))        { Mobile.M3GRenderUntexturedPolygons = state; }
				else if (sysSettingKey.equals("M3GWireframe"))         { Mobile.M3GRenderWireframe = state; }
				else if (sysSettingKey.equals("MCV3ShowHeapUsage"))    { Mobile.MCV3ShowHeapUsage = state; }
				else if (sysSettingKey.equals("MCV3ShowTimeMetrics"))  { Mobile.MCV3ShowTimeMetrics = state; }
			}
		});
	}

	private void bindRadioGroup(final CheckboxMenuItem[] options, final String[] values, final String settingKey, final boolean isSysSetting, final Runnable onChange)
	{
		for (int i = 0; i < options.length; i++)
		{
			final int index = i;
			options[index].addItemListener(new ItemListener()
			{
				public void itemStateChanged(ItemEvent e)
				{
					if (!options[index].getState())
					{
						options[index].setState(true);
						return;
					}

					if (isSysSetting) { config.updateSysSetting(settingKey, values[index]); }
					else { config.updateSetting(settingKey, values[index]); }

					// Deselect all other options
					for (int j = 0; j < options.length; j++)
					{
						if (j != index) { options[j].setState(false); }
					}

					hasPendingChange = true;
					if (onChange != null) { onChange.run(); }
				}
			});
		}
	}

	private void bindRadioGroup(CheckboxMenuItem[] options, String[] values, String settingKey)
	{
		bindRadioGroup(options, values, settingKey, false, null);
	}

	private void buildMenuBar()
	{
		closeMenuItem.setEnabled(false);
		restartMenuItem.setEnabled(false);
		pauseRes.setEnabled(false);
		scrShot.setEnabled(false);

		//add menu items to menus
		fileMenu.add(openMenuItem);
		fileMenu.add(openSpMenuItem);
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
		optionMenu.add(M3GSettings);
		optionMenu.add(speedHackMenu);
		optionMenu.add(compatSettingsMenu);

		for(int i = 0; i < m3gAntiAliasValues.length; i++) { m3gAAMenu.add(m3gAntiAliasValues[i]); }
		M3GSettings.add(m3gAAMenu);

		for(int i = 0; i < m3gBilinearValues.length; i++) { m3gBilinearMenu.add(m3gBilinearValues[i]); }
		M3GSettings.add(m3gBilinearMenu);

		for(int i = 0; i < m3gDitheringValues.length; i++) { m3gDitheringMenu.add(m3gDitheringValues[i]); }
		M3GSettings.add(m3gDitheringMenu);

		for(int i = 0; i < m3gPerspCorrValues.length; i++) { m3gPerspCorrMenu.add(m3gPerspCorrValues[i]); }
		M3GSettings.add(m3gPerspCorrMenu);

		for(int i = 0; i < m3gPerspCorrFactorValues.length; i++) { m3gPerspCorrFactMenu.add(m3gPerspCorrFactorValues[i]); }
		M3GSettings.add(m3gPerspCorrFactMenu);

		for(int i = 0; i < m3gMipmapValues.length; i++) { m3gMipmapMenu.add(m3gMipmapValues[i]); }
		M3GSettings.add(m3gMipmapMenu);

		M3GSettings.add(M3GHalfRes);
		M3GSettings.add(M3GDisableFog);

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
		speedHackMenu.add(MCV3HalfRes);
		speedHackMenu.add(MCV3NoLighting);

		compatSettingsMenu.add(fantasyZoneFix);
		compatSettingsMenu.add(transToOriginOnReset);
		compatSettingsMenu.add(immediateRepaints);
		compatSettingsMenu.add(repaintOnSetCurrent);
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
		// These are special checkbox cases that don't use a config on/off
		fullScreen.setState(FreeJ2ME.isFullscreen);
		useCustomMidi.setState("Custom".equals(config.sysSettings.get("soundfont")));
		useCustomFont.setState("Custom".equals(config.sysSettings.get("textfont")));

		// Per-App RadioGroup Settings
		updateRadioGroup(dojaVersions, dojaVersionValues, "dojaversion", false);
		updateRadioGroup(rotations, rotationValues, "rotate", false);
		updateRadioGroup(fpsOptions, fpsValues, "fps", false);
		updateRadioGroup(fpsHackOptions, fpsHackValues, "fpshack", false);
		updateRadioGroup(fontOffsets, fontOffsetValues, "fontoffset", false);
		updateRadioGroup(layoutOptions, layoutValues, "phone", false);
		updateRadioGroup(backlightOptions, backlightValues, "backlightcolor", false);
		updateRadioGroup(m3gAntiAliasValues, m3gCommonSettingValues, "m3gantialiasmode", false);
		updateRadioGroup(m3gBilinearValues, m3gCommonSettingValues, "m3gbilinearmode", false);
		updateRadioGroup(m3gDitheringValues, m3gCommonSettingValues, "m3gditheringmode", false);
		updateRadioGroup(m3gPerspCorrValues, m3gCommonSettingValues, "m3gperspcorrmode", false);
		updateRadioGroup(m3gMipmapValues, m3gMipmapSettingValues, "m3gmipmapmode", false);
		updateRadioGroup(m3gPerspCorrFactorValues, m3gPFactorSettingValues, "m3gperspcorrsubfactor", false);

		// Standard Per-App Toggleable Settings
		updateToggle(M3GHalfRes, "spdhackm3ghalfres");
		updateToggle(M3GDisableFog, "m3gdisablefog");
		updateToggle(noAlphaOnBlankImages, "spdhacknoalpha");
		updateToggle(MCV3HalfRes, "spdhackmcv3halfres");
		updateToggle(MCV3NoLighting, "spdhackmcv3nolighting");
		updateToggle(fantasyZoneFix, "compatfantasyzonefix");
		updateToggle(transToOriginOnReset, "compattranstooriginonreset");
		updateToggle(immediateRepaints, "compatimmediaterepaints");
		updateToggle(repaintOnSetCurrent, "compatrepaintonsetcurrent");
		updateToggle(overridePlatChecks, "compatoverrideplatchecks");
		updateToggle(siemensFriendlyDrawing, "compatsiemensfriendlydrawing");
		updateToggle(ignoreVolumeChanges, "compatignorevolumechanges");
		updateToggle(MCV3HorFovFix, "compatmcv3horizfovfix");
		resChoice.select(config.settings.get("scrwidth") + "x" + config.settings.get("scrheight"));

		// Sys Settings
		updateRadioGroup(logLevels, logLevelValues, "logLevel", true);
		updateRadioGroup(fpsCounterPos, showFPSValues, "fpsCounterPosition", true);
		updateSysToggle(enableAudio, "sound");
		updateSysToggle(dumpGraphicsData, "dumpGraphicsObjects");
		updateSysToggle(dumpAudioData, "dumpAudioStreams");
		updateSysToggle(M3GWireframe, "M3GWireframe");
		updateSysToggle(M3GUntextured, "M3GUntextured");
		updateSysToggle(MCV3ShowHeapUsage, "MCV3ShowHeapUsage");
		updateSysToggle(MCV3ShowTimeMetrics, "MCV3ShowTimeMetrics");
		updateSysToggle(deleteTemporaryKJXFiles, "deleteTempKJXFiles");

		// Sync AWT Keycodes
		System.arraycopy(Config.inputKeycodes, 0, newInputKeycodes, 0, Config.inputKeycodes.length);
		for (int i = 0; i < inputButtons.length; i++)
		{
			inputButtons[i].setLabel(KeyEvent.getKeyText(newInputKeycodes[i]));
		}

		firstLoad = false;
	}

	private void updateRadioGroup(CheckboxMenuItem[] options, String[] values, String settingKey, boolean isSysSetting)
	{
		String currentValue = isSysSetting ? config.sysSettings.get(settingKey) : config.settings.get(settingKey);
		for (int i = 0; i < options.length; i++)
		{
			options[i].setState(values[i].equals(currentValue));
		}
	}

	private void updateToggle(CheckboxMenuItem checkbox, String settingKey)
	{
		checkbox.setState("on".equals(config.settings.get(settingKey)));
	}

	private void updateSysToggle(CheckboxMenuItem checkbox, String sysSettingKey)
	{
		checkbox.setState("on".equals(config.sysSettings.get(sysSettingKey)));
	}

	class UIListener implements ActionListener
	{
		public void actionPerformed(ActionEvent a)
		{
			String command = a.getActionCommand();
			if(command.equals("Open"))
			{
				FileDialog filePicker = new FileDialog(main, "Open JAR / JAD / KJX / MSD File", FileDialog.LOAD);
				String filename;
				filePicker.setFilenameFilter(new FilenameFilter()
				{
					public boolean accept(File dir, String name)
					{
						return name.toLowerCase().endsWith(".jar") ||
								name.toLowerCase().endsWith(".jad") ||
								name.toLowerCase().endsWith(".kjx") ||
								name.toLowerCase().endsWith(".msd");
					}
				});
				filePicker.setVisible(true);

				filename = filePicker.getFile();

				if(filename == null) { Mobile.log(Mobile.LOG_DEBUG, AWTGUI.class.getPackage().getName() + "." + AWTGUI.class.getSimpleName() + ": " + "Main File Loading was cancelled"); }
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
			if(command.equals("OpenSp"))
			{
				FileDialog filePicker = new FileDialog(main, "Open DoJa SP / SP0 File", FileDialog.LOAD);
				String filename;
				filePicker.setFilenameFilter(new FilenameFilter()
				{
					public boolean accept(File dir, String name)
					{
						return name.toLowerCase().endsWith(".sp") ||
								name.toLowerCase().endsWith(".sp0");
					}
				});
				filePicker.setVisible(true);

				filename = filePicker.getFile();

				if(filename == null) { Mobile.log(Mobile.LOG_DEBUG, AWTGUI.class.getPackage().getName() + "." + AWTGUI.class.getSimpleName() + ": " + "SP/SP0 Loading was cancelled"); }
				else
				{
						try
						{
							spfile = new File(filePicker.getDirectory()+filename).toURI().toString();

							Mobile.getPlatform().spFileName = spfile;

							// We already loaded an app? Then we'll need to restart.
							if(hasLoadedFile()) { showRestartDialog(); }
						}
						catch(Exception e) { Mobile.log(Mobile.LOG_DEBUG, AWTGUI.class.getPackage().getName() + "." + AWTGUI.class.getSimpleName() + ": " + "Load error:" + e.getMessage()); }
				}
			}
			else if(command.equals("Close")) { FreeJ2ME.closeApp(); }
			else if(command.equals("Screenshot")) { ScreenShot.takeScreenshot(false); }
			else if(command.equals("PauseResume")) { MobilePlatform.pauseResumeApp(); }
			else if(command.equals("Exit")) { System.exit(0); }
			else if(command.equals("AboutMenu")) { awtDialogs[1].setLocationRelativeTo(main); awtDialogs[1].setVisible(true); }
			else if(command.equals("CloseAboutMenu")) { awtDialogs[1].setVisible(false); }
			else if(command.equals("ChangeResolution")) { awtDialogs[0].setLocationRelativeTo(main); awtDialogs[0].setVisible(true); }
			else if(command.equals("ApplyResChange"))
			{
				if(fileLoaded) /* Only update res if a jar was loaded, or else AWT throws NullPointerException */
				{
					String[] res = resChoice.getItem(resChoice.getSelectedIndex()).split("x");

					config.updateDisplaySize(Integer.parseInt(res[0]), Integer.parseInt(res[1]));
					hasPendingChange = true;
				}
				awtDialogs[0].setVisible(false);
			}
			else if(command.equals("CancelResChange")) { awtDialogs[0].setVisible(false); }
			else if(command.equals("RestartNow")) { Mobile.restartApp(); }
			else if(command.equals("RestartLater")) { awtDialogs[3].setVisible(false); }
			else if(command.equals("MapInputs")) { awtDialogs[4].setVisible(true); }
			else if(command.equals("ApplyInputs"))
			{
				System.arraycopy(newInputKeycodes, 0, Config.inputKeycodes, 0, newInputKeycodes.length);
				config.updateAWTInputs();
				awtDialogs[4].setVisible(false);
			}
			else if(command.equals("CancelInputs")) { awtDialogs[4].setVisible(false); }
			else if(command.equals("ShowPlayer"))
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
