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
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JMenuBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;

import java.util.ArrayList;
import java.util.Arrays;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.MobilePlatform;
import org.recompile.freej2me.gamepad.GamepadReader;
import org.recompile.freej2me.gamepad.LinuxGamepadReader;

public final class FJGUI
{
	final String VERSION = "1.52";
	/* This is used to indicate to FreeJ2ME that it has to call "settingsChanged()" to apply changes made here */
	private boolean hasPendingChange;

	/* Indicates whether a jar file was loaded successfully */
	private boolean fileLoaded = false;
	private boolean firstLoad = true;
	private boolean allowRestartDialog = false;

	/* String that points to the jar file that has to be loaded */
	String jarfile = "";
	String spfile = "";

	/* This is meant to be a local reference of FreeJ2ME's main frame */
	private JFrame main;

	/* And this is meant to be a local reference of FreeJ2ME's config */
	private Config config;

	/* AWT's main JMenuBar */
	final JMenuBar menuBar = new JMenuBar();

	/* JMenuBar's menus */
	final JMenu fileMenu = new JMenu("File");
	final JMenu optionMenu = new JMenu("Settings");
	final JMenu speedHackMenu = new JMenu("SpeedHacks");
	final JMenu debugMenu = new JMenu("Debug");

	/* Sub JMenus (for now, all of them are located in "Settings") */
	final JMenu fpsCap = new JMenu("FPS Limit");
	final JMenu unlockFPSHack = new JMenu("Unlock FPS Hack");
	final JMenu showFPS = new JMenu("Show FPS Counter");
	final JMenu phoneType = new JMenu("Phone Key Layout");
	final JMenu DoJaVersion = new JMenu("DoJa API Version");
	final JMenu screenRotation = new JMenu("Screen Rotation (Ctrl+Alt+R)");
	final JMenu backlightColor = new JMenu("Backlight Color");
	final JMenu fontOffset = new JMenu("Font Size Offset");

	public final String[] supportedResolutions = {"96x65","101x64","101x80","128x128","130x130","120x160","128x160","160x128","132x176","208x173","176x208","176x220","220x176","208x208","220x220","180x320","320x180","240x240","240x260","208x320","240x320","320x240","240x400","400x240","320x320","240x432","240x480","360x360","352x416","360x480","360x640","640x360","480x640","640x480","345x800","800x345","480x800","800x480"};

	/* JDialogs for resolution changes, restart notifications, MemStats and info about FreeJ2ME */
	final JDialog[] swingDialogs =
	{
		new JDialog(main , "Set LCD Resolution", true),
		new JDialog(main , "About FreeJ2ME", true),
		new JDialog(main, "FreeJ2ME MemStat", false),
		new JDialog(main, "Restart Required", true),
		new JDialog(main, "Key Mapping", true),
		new JDialog(main, "Console Log", false),
		new JDialog(main, "Compatibility Settings", false),
	};

	final JButton[] swingButtons =
	{
		new JButton("Close"),
		new JButton("Apply"),
		new JButton("Cancel"),
		new JButton("Restart Now"),
		new JButton("Restart later"),
		new JButton("Apply"),
		new JButton("Cancel"),
		new JButton("Close Dialog"),
		new JButton("Keyboard"), // Toggles input layouts on the mapping screen
		new JButton("Refresh") // Gamepad refresh button
	};

	/* Log Level submenu */
	final JMenu logLevel = new JMenu("Log Level");

	/* Main M3G submenu */
	final JMenu M3GSettings = new JMenu("M3G Settings");

	final JMenu m3gAAMenu = new JMenu("Anti-Aliasing");
	final JMenu m3gBilinearMenu = new JMenu("Bilinear Filtering");
	final JMenu m3gDitheringMenu = new JMenu("Dithering");
	final JMenu m3gPerspCorrMenu = new JMenu("Perspective Correction");
	final JMenu m3gPerspCorrFactMenu = new JMenu("Perspective Correction Quality");
	final JMenu m3gMipmapMenu = new JMenu("Mipmapping");

	/* M3G Debug submenu */
	final JMenu M3GDebug = new JMenu("M3G Debugging");

	/* M3G Debug submenu */
	final JMenu MCV3Debug = new JMenu("MascotCapsuleV3 Debugging");

	/* Input mapping keys */
	final JButton inputButtons[] = new JButton[]
	{
		new JButton("Q"),
		new JButton("W"),
		new JButton("Up"),
		new JButton("Left"),
		new JButton("Enter"),
		new JButton("Right"),
		new JButton("Down"),
		new JButton("NumPad-7"),
		new JButton("NumPad-8"),
		new JButton("NumPad-9"),
		new JButton("NumPad-4"),
		new JButton("NumPad-5"),
		new JButton("NumPad-6"),
		new JButton("NumPad-1"),
		new JButton("NumPad-2"),
		new JButton("NumPad-3"),
		new JButton("E"),
		new JButton("NumPad-0"),
		new JButton("R"),
		new JButton("A"),
		new JButton("Space"),
		new JButton("C"),
		new JButton("X")
	};

	final JButton gamepadButtons[] = new JButton[]
	{
		new JButton(""),
		new JButton(""),
		new JButton(""),
		new JButton(""),
		new JButton(""),
		new JButton(""),
		new JButton(""),
		new JButton(""),
		new JButton(""),
		new JButton(""),
		new JButton(""),
		new JButton(""),
		new JButton(""),
		new JButton(""),
		new JButton(""),
		new JButton(""),
		new JButton(""),
		new JButton(""),
		new JButton(""),
		new JButton(""),
		new JButton(""),
		new JButton(""),
		new JButton("")
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

	/* Gamepad inputs. Zeroed by default as these vary per joystick. */
	public static int gamepadKeycodes[] = new int[]
	{
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0
	};

	public static String gamepadKeyNames[] = new String[]
	{
		"", "", "", "", "", "", "", "", "", "", "", "", "", "",
		"", "", "", "", "", "", "", "", ""
	};

	// Container for input mappings that allows swapping between keyboard and gamepad
	final CardLayout inputLayout = new CardLayout();
	final JPanel inputPanel = new JPanel(inputLayout);
	final JTextArea gamepadName = new JTextArea("Pad: None");
	private static GamepadReader gamepadReader = null;
	private static Thread gamepadThread = null;

	final JComboBox resChoice = new JComboBox();

	/* Items for each of the bar's JMenus */
	final UIListener menuItemListener = new UIListener(this);

	final JMenuItem aboutMenuItem = new JMenuItem("About FreeJ2ME");
	final JMenuItem resChangeMenuItem = new JMenuItem("Change Phone Resolution");

	final JMenuItem openMenuItem = new JMenuItem("Open JAR / JAD / KJX / MSD File");
	final JMenuItem openSpMenuItem = new JMenuItem("Open DoJa SP / SP0 File");
	final JMenuItem restartMenuItem = new JMenuItem("Restart Running Jar");
	final JMenuItem closeMenuItem = new JMenuItem("Close Running Jar");
	final JMenuItem scrShot = new JMenuItem("Take Screenshot (Ctrl+Alt+C)");
	final JMenuItem pauseRes = new JMenuItem("Pause / Resume (Ctrl+Alt+X)");
	final JMenuItem exitMenuItem = new JMenuItem("Exit FreeJ2ME");
	final JMenuItem mapInputs = new JMenuItem("Manage Inputs");
	final JMenuItem compatSettingsMenu = new JMenuItem("Compatibility Settings");

	final JMenuItem showPlayer = new JMenuItem("J2ME Media Player");

	final JCheckBoxMenuItem fullScreen = new JCheckBoxMenuItem("Toggle Fullscreen (Ctrl+Alt+F)", false);
	final JCheckBoxMenuItem enableAudio = new JCheckBoxMenuItem("Enable Audio", true);
	final JCheckBoxMenuItem useCustomMidi = new JCheckBoxMenuItem("Use custom midi soundfont", false);
	final JCheckBoxMenuItem useCustomFont = new JCheckBoxMenuItem("Use custom text font", false);

	final JCheckBoxMenuItem[] dojaVersions =
	{
		new JCheckBoxMenuItem("DoJa-1.0", false),
		new JCheckBoxMenuItem("DoJa-2.0 & 1.5 OE", false),
		new JCheckBoxMenuItem("DoJa-3.0 & 2.5 OE", false),
		new JCheckBoxMenuItem("DoJa-3.5", false),
		new JCheckBoxMenuItem("DoJa-4.0", false),
		new JCheckBoxMenuItem("DoJa-4.1", false),
		new JCheckBoxMenuItem("DoJa-5.0", false),
		new JCheckBoxMenuItem("DoJa-5.1", false),
		new JCheckBoxMenuItem("Star-1.0", false),
		new JCheckBoxMenuItem("Star-1.1", false),
		new JCheckBoxMenuItem("Star-1.2", false),
		new JCheckBoxMenuItem("Star-1.3", false),
		new JCheckBoxMenuItem("Star-1.5", false),
		new JCheckBoxMenuItem("Star-2.0", true)
	};
	final String[] dojaVersionValues = {"10", "20", "30", "35", "40", "41", "50", "51", "100", "110", "120", "130", "150", "200"};

	final JCheckBoxMenuItem[] rotations =
	{
		new JCheckBoxMenuItem("No rotation", true),
		new JCheckBoxMenuItem("90 degrees",  false),
		new JCheckBoxMenuItem("180 degrees", false),
		new JCheckBoxMenuItem("270 degrees", false)
	};
	final String[] rotationValues = {"0", "90", "180", "270"};

	final JCheckBoxMenuItem[] layoutOptions =
	{
		new JCheckBoxMenuItem("Default", true),
		new JCheckBoxMenuItem("KDDI", false),
		new JCheckBoxMenuItem("LG", false),
		new JCheckBoxMenuItem("Motorola/SoftBank/Sharp", false),
		new JCheckBoxMenuItem("Motorola V8", false),
		new JCheckBoxMenuItem("Motorola Triplets", false),
		new JCheckBoxMenuItem("Motorola A1000", false),
		new JCheckBoxMenuItem("Nokia Full Keyboard", false),
		new JCheckBoxMenuItem("Sagem", false),
		new JCheckBoxMenuItem("Siemens", false),
		new JCheckBoxMenuItem("SKT", false)
	};
	final String[] layoutValues = {"Standard", "KDDI", "LG", "Motorola", "MotoV8", "MotoTriplets", "MotoA1000", "NokiaKeyboard", "Sagem", "Siemens", "SKT"};

	final JCheckBoxMenuItem[] backlightOptions =
	{
		new JCheckBoxMenuItem("White/Disabled", true),
		new JCheckBoxMenuItem("Green", false),
		new JCheckBoxMenuItem("Cyan", false),
		new JCheckBoxMenuItem("Orange", false),
		new JCheckBoxMenuItem("Violet", false),
		new JCheckBoxMenuItem("Red", false)
	};
	final String[] backlightValues = {"Disabled", "Green", "Cyan", "Orange", "Violet", "Red"};

	final JCheckBoxMenuItem[] fpsOptions =
	{
		new JCheckBoxMenuItem("No Limit", false),
		new JCheckBoxMenuItem("60 FPS", true),
		new JCheckBoxMenuItem("55 FPS", false),
		new JCheckBoxMenuItem("50 FPS", false),
		new JCheckBoxMenuItem("45 FPS", false),
		new JCheckBoxMenuItem("40 FPS", false),
		new JCheckBoxMenuItem("35 FPS", false),
		new JCheckBoxMenuItem("30 FPS", false),
		new JCheckBoxMenuItem("25 FPS", false),
		new JCheckBoxMenuItem("20 FPS", false),
		new JCheckBoxMenuItem("15 FPS", false),
		new JCheckBoxMenuItem("10 FPS", false)
	};
	final String[] fpsValues = {"0", "60", "55", "50", "45", "40", "35", "30", "25", "20", "15", "10"};

	final JCheckBoxMenuItem[] fpsHackOptions =
	{
		new JCheckBoxMenuItem("Disabled", true),
		new JCheckBoxMenuItem("Safe", false),
		new JCheckBoxMenuItem("Extended", false),
		new JCheckBoxMenuItem("Aggressive", false)
	};
	final String[] fpsHackValues = {"Disabled", "Safe", "Extended", "Aggressive"};

	final JCheckBoxMenuItem[] fpsCounterPos =
	{
		new JCheckBoxMenuItem("Off", true),
		new JCheckBoxMenuItem("Top Left", false),
		new JCheckBoxMenuItem("Top Right", false),
		new JCheckBoxMenuItem("Bottom Left", false),
		new JCheckBoxMenuItem("Bottom Right", false)
	};
	final String[] showFPSValues = {"Off", "TopLeft", "TopRight", "BottomLeft", "BottomRight"};

	final JCheckBoxMenuItem[] fontOffsets =
	{
		new JCheckBoxMenuItem("-4pt", false),
		new JCheckBoxMenuItem("-3pt", false),
		new JCheckBoxMenuItem("-2pt", false),
		new JCheckBoxMenuItem("-1pt", false),
		new JCheckBoxMenuItem(" 0pt (Default)", true),
		new JCheckBoxMenuItem(" 1pt", false),
		new JCheckBoxMenuItem(" 2pt", false),
		new JCheckBoxMenuItem(" 3pt", false),
		new JCheckBoxMenuItem(" 4pt", false)
	};
	final String[] fontOffsetValues = {"-4", "-3", "-2", "-1", "0", "1", "2", "3", "4"};

	final JCheckBoxMenuItem[] logLevels =
	{
		new JCheckBoxMenuItem("Disabled", false),
		new JCheckBoxMenuItem("Debug", false),
		new JCheckBoxMenuItem("Info", true),
		new JCheckBoxMenuItem("Warning", false),
		new JCheckBoxMenuItem("Error", false)
	};
	final String[] logLevelValues = {"0", "1", "2", "3", "4"};

	// Speedhacks
	final JCheckBoxMenuItem noAlphaOnBlankImages = new JCheckBoxMenuItem("No alpha on blank images", false);
	final JCheckBoxMenuItem MCV3HalfRes = new JCheckBoxMenuItem("Render MascotCapsuleV3 at Half Res", false);
	final JCheckBoxMenuItem MCV3NoLighting = new JCheckBoxMenuItem("Disable MascotCapsuleV3's lighting", false);

	// M3G JMenu
	final JCheckBoxMenuItem M3GHalfRes = new JCheckBoxMenuItem("Halve Resolution", false);
	final JCheckBoxMenuItem M3GDisableFog = new JCheckBoxMenuItem("Disable Fog", false);
	final JCheckBoxMenuItem[] m3gAntiAliasValues =
	{
		new JCheckBoxMenuItem("Always Disabled", false),
		new JCheckBoxMenuItem("App-Controlled (Default)", true),
		new JCheckBoxMenuItem("Force-Enabled", false)
	};
	final JCheckBoxMenuItem[] m3gBilinearValues =
	{
		new JCheckBoxMenuItem("Always Disabled", false),
		new JCheckBoxMenuItem("App-Controlled (Default)", true),
		new JCheckBoxMenuItem("Force-Enabled", false)
	};
	final JCheckBoxMenuItem[] m3gDitheringValues =
	{
		new JCheckBoxMenuItem("Always Disabled", false),
		new JCheckBoxMenuItem("App-Controlled (Default)", true),
		new JCheckBoxMenuItem("Force-Enabled", false)
	};
	final JCheckBoxMenuItem[] m3gPerspCorrValues =
	{
		new JCheckBoxMenuItem("Always Disabled", false),
		new JCheckBoxMenuItem("App-Controlled (Default)", true),
		new JCheckBoxMenuItem("Force-Enabled", false)
	};
	final String[] m3gCommonSettingValues = {"off", "app", "on"};

	final JCheckBoxMenuItem[] m3gPerspCorrFactorValues =
	{
		new JCheckBoxMenuItem("Extra", false),
		new JCheckBoxMenuItem("High (Default)", true),
		new JCheckBoxMenuItem("Average", false),
		new JCheckBoxMenuItem("Low", false)
	};
	final String[] m3gPFactorSettingValues = {"extra", "high", "medium", "low"};

	final JCheckBoxMenuItem[] m3gMipmapValues =
	{
		new JCheckBoxMenuItem("Always Disabled", false),
		new JCheckBoxMenuItem("App-Controlled (Default)", true),
		new JCheckBoxMenuItem("Force-Nearest", false),
		new JCheckBoxMenuItem("Force-Linear", false)
	};
	final String[] m3gMipmapSettingValues = {"off", "app", "nearest", "linear"};

	// Compatibility settings
	final JCheckBox fantasyZoneFix = new JCheckBox("Fix for Fantasy Zone 176x208 weird mirroring", false);
	final JCheckBox transToOriginOnReset = new JCheckBox("Translate to origin on gfx reset", false);
	final JCheckBox immediateRepaints = new JCheckBox("Process canvas repaints immediately", false);
	final JCheckBox repaintOnSetCurrent = new JCheckBox("Repaint on Display setCurrent.", false);
	final JCheckBox overridePlatChecks = new JCheckBox("Override Mobile Platform checks", true);
	final JCheckBox siemensFriendlyDrawing = new JCheckBox("Siemens-friendly drawing methods", false);
	final JCheckBox ignoreVolumeChanges = new JCheckBox("Ignore volume changes", false);
	final JCheckBox MCV3HorFovFix = new JCheckBox("MascotCapsuleV3 Horizontal FOV Fix", false);

	final JCheckBoxMenuItem deleteTemporaryKJXFiles = new JCheckBoxMenuItem("Delete KJX files' temporary JAR/JAD", true);
	final JCheckBoxMenuItem dumpAudioData = new JCheckBoxMenuItem("Dump Audio Streams", false);
	final JCheckBoxMenuItem dumpGraphicsData = new JCheckBoxMenuItem("Dump Graphics Objects", false);
	final JCheckBoxMenuItem showDebugWindows = new JCheckBoxMenuItem("Show Debug Windows", false);

	// M3G Debugging
	final JCheckBoxMenuItem M3GUntextured = new JCheckBoxMenuItem("Draw Only Vertex Colors", false);
	final JCheckBoxMenuItem M3GWireframe = new JCheckBoxMenuItem("Wireframe Mode", false);

	// MascotCapsuleV3 Debugging
	final JCheckBoxMenuItem MCV3ShowHeapUsage = new JCheckBoxMenuItem("Show Heap Usage", false);
	final JCheckBoxMenuItem MCV3ShowTimeMetrics = new JCheckBoxMenuItem("Show Time Metrics", false);

	final JTextArea logArea = new JTextArea();
	final JTextArea memArea = new JTextArea();
	final Font dialogFont = new Font(Font.DIALOG, Font.BOLD, 12);

	private StringBuilder debugContent = null;
	private BufferedReader logReader = null;

	public FJGUI(Config config)
	{

		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (Exception e) { System.out.println("Failed to set Look & Feel: " + e.getMessage()); }
		this.config = config;

		debugContent = new StringBuilder();
		try { logReader = new BufferedReader(new FileReader(Mobile.logFile)); }
		catch(Exception e) { System.out.println("Failed to create log window writer:" + e.getMessage()); }

		// Flatten those buttons, their default look is pretty ugly.
		for(int i = 0; i < swingButtons.length; i++) { flattenButton(swingButtons[i]); }

		// Input buttons get a smaller font and tighter insets as well.
		for(int i = 0; i < inputButtons.length; i++)
		{
			inputButtons[i].setMargin(new Insets(0, 0, 0, 0));
			inputButtons[i].setFont(new Font(Font.DIALOG, Font.BOLD, 10));
			flattenButton(inputButtons[i]);

			gamepadButtons[i].setMargin(new Insets(0, 0, 0, 0));
			gamepadButtons[i].setFont(new Font(Font.DIALOG, Font.BOLD, 10));
			flattenButton(gamepadButtons[i]);
		}
		// Same for the gamepad refresh button
		swingButtons[9].setMargin(new Insets(0, 0, 0, 0));
		swingButtons[9].setFont(new Font(Font.DIALOG, Font.BOLD, 10));
		flattenButton(swingButtons[9]);

		// The gamepad name area on the input map menu must only span 1 row.
		gamepadName.setRows(1);
		gamepadName.setEditable(false);
		gamepadName.setForeground(Color.BLACK);
		gamepadName.setBackground(new Color(238, 238, 238));
		gamepadName.setFont(new Font(Font.DIALOG, Font.BOLD, 10));

		swingDialogs[1].setLayout( new FlowLayout(FlowLayout.CENTER, 200, 0));
		swingDialogs[1].setUndecorated(true); /* Whenever a JDialog is undecorated, it's because it's meant to look like an internal menu on FreeJ2ME's main JFrame */
		swingDialogs[1].setSize(230, 235);
		swingDialogs[1].setResizable(false);
		swingDialogs[1].add(new JLabel("FreeJ2ME-Plus - A free J2ME emulator"));
		swingDialogs[1].add(new JLabel("Version " + VERSION));
		swingDialogs[1].add(new JLabel("--------------------------------"));
		swingDialogs[1].add(new JLabel("Original Project Authors:"));
		swingDialogs[1].add(new JLabel("David Richardson (Recompile)"));
		swingDialogs[1].add(new JLabel("Saket Dandawate (hex007)"));
		swingDialogs[1].add(new JLabel("--------------------------------"));
		swingDialogs[1].add(new JLabel("Plus Fork Maintainer:"));
		swingDialogs[1].add(new JLabel("Paulo Sousa (AShiningRay)"));
		swingDialogs[1].add(swingButtons[0]);

		swingButtons[1].setForeground(Color.BLUE);
		swingButtons[2].setForeground(Color.RED);

		resChoice.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
		resChoice.setPreferredSize(new java.awt.Dimension(105, 30));

		swingDialogs[0].getContentPane().removeAll();
		swingDialogs[0].setLayout(new BoxLayout(swingDialogs[0].getContentPane(), BoxLayout.Y_AXIS));
		swingDialogs[0].setUndecorated(true);
		swingDialogs[0].setBackground(new Color(238, 238, 238, 160));
		swingDialogs[0].setSize(250, 125);
		swingDialogs[0].setResizable(false);

		JLabel label1 = new JLabel("Select a Resolution from the Dropdown");
		JLabel label2 = new JLabel("and then press the 'Apply' button!");
		label1.setAlignmentX(Component.CENTER_ALIGNMENT);
		label2.setAlignmentX(Component.CENTER_ALIGNMENT);

		resChoice.setAlignmentX(Component.CENTER_ALIGNMENT);
		Dimension resChoiceSize = new Dimension(100, 30);
		resChoice.setPreferredSize(resChoiceSize);
		resChoice.setMaximumSize(resChoiceSize);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
		buttonPanel.setOpaque(false);
		buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonPanel.add(swingButtons[1]);
		buttonPanel.add(swingButtons[2]);

		swingDialogs[0].add(Box.createVerticalStrut(8));
		swingDialogs[0].add(label1);
		swingDialogs[0].add(label2);
		swingDialogs[0].add(Box.createVerticalStrut(8));
		swingDialogs[0].add(resChoice);
		swingDialogs[0].add(Box.createVerticalStrut(10));
		swingDialogs[0].add(buttonPanel);

		// Setup the key mapping dialog, a separate method for this is much
		// cleaner since it's quite a big menu and now there are two layouts
		// (keyboard and gamepad)
		setupKeyMappingDialog();

		// Restart Required Dialog
		swingDialogs[3].setLayout(new FlowLayout(FlowLayout.CENTER, 5, 10));
		swingDialogs[3].setUndecorated(true);
		swingDialogs[3].setBackground(new Color(238, 238, 238, 160));
		swingDialogs[3].setSize(240, 80);
		swingDialogs[3].add(new JLabel("This change requires a restart to apply!"));

		swingButtons[3].setForeground(Color.BLUE);
		swingButtons[4].setForeground(Color.RED);

		swingDialogs[3].add(swingButtons[3]);
		swingDialogs[3].add(swingButtons[4]);


		// Mem stats window
		memArea.setEditable(false); // Make the log area read-only

		JScrollPane memScrollPane = new JScrollPane(memArea);
		memScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

		swingDialogs[2].setLayout(new BorderLayout());
		swingDialogs[2].setUndecorated(true);
		swingDialogs[2].setSize(200, 80);
		swingDialogs[2].setFont(dialogFont);
		swingDialogs[2].setResizable(false);

		swingDialogs[2].add(memScrollPane, BorderLayout.CENTER);

		// Console Log window
		logArea.setEditable(false); // Make the log area read-only

		JScrollPane logScrollPane = new JScrollPane(logArea);
		logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		swingDialogs[5].setLayout(new BorderLayout());
		swingDialogs[5].setUndecorated(true);
		swingDialogs[5].setSize(600, 320);
		swingDialogs[5].setFont(dialogFont);
		swingDialogs[5].setResizable(false);
		swingDialogs[5].add(logScrollPane, BorderLayout.CENTER);

		// Compatibility settings are a dialog now, for much better readability
		swingDialogs[6].getContentPane().removeAll();
		swingDialogs[6].setLayout(new BorderLayout());
		swingDialogs[6].setSize(640, 480);
		swingDialogs[6].setResizable(false);
		swingDialogs[6].setUndecorated(true);

		JPanel headerPanel = new JPanel();
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
		headerPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

		JLabel titleLabel = new JLabel("Compatibility Settings");
		titleLabel.setFont(new Font("Dialog", Font.BOLD, 16));
		titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		JLabel descLabel = new JLabel("FreeJ2ME-Plus settings that help some apps work better at the expense of breaking others.");
		descLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
		descLabel.setForeground(Color.DARK_GRAY);
		descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		headerPanel.add(titleLabel);
		headerPanel.add(Box.createVerticalStrut(4));
		headerPanel.add(descLabel);

		// 2-Column Grid Panel, packs enough horizontal information without
		// cluttering too much (and 1 column would make this a long list)
		JPanel gridPanel = new JPanel(new GridLayout(0, 2, 4, 4));
		gridPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		// Add setting entries into the settings' grid
		gridPanel.add(createSettingCard(fantasyZoneFix, "Fix for Fantasy Zone 176x208 mirroring",
			"Fantasy Zone 176x208's MIDP version goes entirely out of spec with its mirroring operation, and is broken even on real devices. This setting fixes it at the expense of breaking other applications that use the same draw path for S40 and match the expected behavior."));
		gridPanel.add(createSettingCard(transToOriginOnReset, "Translate to origin on Graphics reset",
			"Some apps rely on the graphics object being translated to the origin before every draw as opposed to managing that state themselves. This compatibility setting helps with that, and any case where the drawn area keeps moving in any given direction for unknown reasons."));
		gridPanel.add(createSettingCard(immediateRepaints, "Process Canvas repaints immediately",
			"By default, J2ME expects canvas repaints to be queued up, and applications can either request serviceRepaints() or use serial calls to synchronize rendering. However, this setting may help cases where an app is freezing by deadlocking on event sync."));
		gridPanel.add(createSettingCard(repaintOnSetCurrent, "Repaint on MIDP Display setCurrent",
			"By default, J2ME never explicitly makes a Canvas repaint itself when it is brought to the screen (set as current), the apps should do so when appropriate. This setting forces repaints to happen in that case, fixing apps that would get stuck in a blank or black screen at boot."));
		gridPanel.add(createSettingCard(overridePlatChecks, "Override Mobile Platform checks",
			"Some applications check against specific platform strings (such as 'Nokia', 'Siemens S60'). This setting overrides any platform strings by FreeJ2ME's own to allow apps that check against specific devices to boot. This option helps far more than breaks, so it's on by default."));
		gridPanel.add(createSettingCard(siemensFriendlyDrawing, "Siemens-friendly drawing methods",
			"MIDP-Compliant J2ME drawing operations do not need to check for negative translation values in order to draw images properly. However, some Siemens apps like STCC (Swedish Touring Car Championship) won't work properly with the default behavior. This option works around it, but will break other apps."));
		gridPanel.add(createSettingCard(ignoreVolumeChanges, "Ignore volume change requests",
			"Media playback is probably the J2ME subsystem whose implementation and utilization varies the most by vendor. Some applications go as far as setting volume changes to streams they already stopped beforehand, which can cause playback issues. Enabling this option helps work around this."));
		gridPanel.add(createSettingCard(MCV3HorFovFix, "MascotCapsuleV3 Horizontal FOV Fix",
			"Might help games meant for portrait resolutions work better in landscape resolutions. Usually not needed unless you're running a game that uses this 3D engine, and is forcing a different resolution than the app's expected one."));

		// Wrap grid in container so it doesn't stretch items infinitely
		JPanel containerPanel = new JPanel(new BorderLayout());
		containerPanel.add(gridPanel, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(containerPanel);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.getVerticalScrollBar().setUnitIncrement(12);
		scrollPane.setBorder(null);

		JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
		closePanel.add(swingButtons[7]);

		swingDialogs[6].add(headerPanel, BorderLayout.NORTH);
		swingDialogs[6].add(scrollPane, BorderLayout.CENTER);
		swingDialogs[6].add(closePanel, BorderLayout.SOUTH);

		// Setup actions
		openMenuItem.setActionCommand("Open");
		openSpMenuItem.setActionCommand("OpenSp");
		restartMenuItem.setActionCommand("RestartNow");
		closeMenuItem.setActionCommand("Close");
		scrShot.setActionCommand("Screenshot");
		pauseRes.setActionCommand("PauseResume");
		exitMenuItem.setActionCommand("Exit");
		aboutMenuItem.setActionCommand("AboutMenu");
		resChangeMenuItem.setActionCommand("ChangeResolution");
		swingButtons[1].setActionCommand("ApplyResChange");
		swingButtons[2].setActionCommand("CancelResChange");
		swingButtons[0].setActionCommand("CloseAboutMenu");
		swingButtons[3].setActionCommand("RestartNow");
		swingButtons[4].setActionCommand("RestartLater");
		mapInputs.setActionCommand("MapInputs");
		compatSettingsMenu.setActionCommand("CompatSettings");
		swingButtons[5].setActionCommand("ApplyInputs");
		swingButtons[6].setActionCommand("CancelInputs");
		swingButtons[7].setActionCommand("CloseCompat");

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
		swingButtons[1].addActionListener(menuItemListener);
		swingButtons[2].addActionListener(menuItemListener);
		swingButtons[0].addActionListener(menuItemListener);
		swingButtons[3].addActionListener(menuItemListener);
		swingButtons[4].addActionListener(menuItemListener);
		mapInputs.addActionListener(menuItemListener);
		compatSettingsMenu.addActionListener(menuItemListener);
		swingButtons[5].addActionListener(menuItemListener);
		swingButtons[6].addActionListener(menuItemListener);
		swingButtons[7].addActionListener(menuItemListener);
		showPlayer.addActionListener(menuItemListener);

		addInputButtonListeners(false);
		addInputButtonListeners(true);

		buildMenuBar();

		setActionListeners();
	}

	public static void flattenButton(JButton button)
	{
		button.setContentAreaFilled(false);        // Removes the default gradient fill
		button.setOpaque(true);                    // Allows background color to fill cleanly
		button.setBackground(new Color(220, 220, 220)); // Sets a solid flat background
	}

	public void setupKeyMappingDialog()
	{
		JDialog dialog = swingDialogs[4];
		dialog.getContentPane().removeAll();
		dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));
		dialog.setSize(280, 410);
		dialog.setResizable(false);

		// Header Label
		JLabel headerLabel = new JLabel("Click any button below to map keys", SwingConstants.CENTER);
		headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		headerLabel.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));

		// Action Panel (Apply, Cancel + Mode Toggle)
		JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
		swingButtons[5].setForeground(Color.BLUE); // Apply
		swingButtons[6].setForeground(Color.RED);  // Cancel

		// Input Layout Toggle action
		swingButtons[8].setActionCommand("ToggleInputLayout");
		swingButtons[8].addActionListener(menuItemListener);

		actionPanel.add(swingButtons[5]);
		actionPanel.add(swingButtons[6]);
		actionPanel.add(swingButtons[8]);

		// Add both input layouts into the card layout
		inputPanel.add(createInputPanel(inputButtons, false), "KEYBOARD");
		inputPanel.add(createInputPanel(gamepadButtons, true), "GAMEPAD");

		// Assemble the input dialog itself
		dialog.add(headerLabel);
		dialog.add(actionPanel);
		dialog.add(Box.createVerticalStrut(8));
		dialog.add(new JSeparator(JSeparator.HORIZONTAL));
		dialog.add(Box.createVerticalStrut(8));

		dialog.add(inputPanel);

		dialog.invalidate();
		dialog.pack();
		dialog.repaint();
	}

	// Rather than duplicating code, the input map panel goes through this
	// method to build both the Keyboard and Gamepad layouts.
	private JPanel createInputPanel(JButton[] buttons, boolean isGamepad)
	{
		JPanel container = new JPanel();
		container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

		// Gamepad Mode has an extra row with device name and a "Refresh" button
		if (isGamepad)
		{
			// Create a horizontal row panel
			JPanel devicePanel = new JPanel(new BorderLayout(4, 0));

			// Align the extra row's width with the main panel, otherwise it'll
			// clip into the window's edges.
			devicePanel.setMaximumSize(new Dimension(260, 24));

			// Gamepad device refresh action.
			swingButtons[9].setActionCommand("RefreshGamepads");
			swingButtons[9].addActionListener(menuItemListener);

			// Click it so that FreeJ2ME+ loads with a gamepad already present
			// if available (makes debugging this easier, and also improves UX)
			swingButtons[9].doClick();

			devicePanel.add(gamepadName, BorderLayout.CENTER);
			devicePanel.add(swingButtons[9], BorderLayout.EAST);

			container.add(devicePanel);
			container.add(Box.createVerticalStrut(4)); // Gap before phone grid
		}

		// Phone Grid
		JPanel phonePanel = new JPanel(new GridLayout(0, 3, 3, 3));
		phonePanel.setMaximumSize(new Dimension(260, 240));

		phonePanel.add(buttons[0]); // Soft Left (Q)
		phonePanel.add(new JLabel(""));
		phonePanel.add(buttons[1]); // Soft Right (W)

		phonePanel.add(new JLabel(""));
		phonePanel.add(buttons[2]); // Up
		phonePanel.add(new JLabel(""));

		phonePanel.add(buttons[3]); // Left
		phonePanel.add(buttons[4]); // Enter/OK
		phonePanel.add(buttons[5]); // Right

		phonePanel.add(new JLabel(""));
		phonePanel.add(buttons[6]); // Down
		phonePanel.add(new JLabel(""));

		phonePanel.add(new JLabel("CLR KEY:", SwingConstants.RIGHT));
		phonePanel.add(buttons[19]); // Clear (A)
		phonePanel.add(new JLabel(""));

		if(!isGamepad)
		{
			phonePanel.add(new JLabel(""));
			phonePanel.add(new JLabel(""));
			phonePanel.add(new JLabel(""));
		}

		// Numpad keys
		phonePanel.add(buttons[7]);  phonePanel.add(buttons[8]);  phonePanel.add(buttons[9]);
		phonePanel.add(buttons[10]); phonePanel.add(buttons[11]); phonePanel.add(buttons[12]);
		phonePanel.add(buttons[13]); phonePanel.add(buttons[14]); phonePanel.add(buttons[15]);
		phonePanel.add(buttons[16]); phonePanel.add(buttons[17]); phonePanel.add(buttons[18]);

		// Hotkey section
		JPanel hotkeyHeader = new JPanel(new FlowLayout(FlowLayout.CENTER));
		hotkeyHeader.add(new JLabel("Hotkeys"));
		hotkeyHeader.add(new JLabel("(Ctrl+Alt+*)"));

		JPanel hotkeyGrid = new JPanel(new GridLayout(2, 3, 2, 2));
		hotkeyGrid.setMaximumSize(new Dimension(260, 50));

		hotkeyGrid.add(new JLabel("Fast-Fwd", SwingConstants.CENTER));
		hotkeyGrid.add(new JLabel("Screenshot", SwingConstants.CENTER));
		hotkeyGrid.add(new JLabel("(Un)Pause", SwingConstants.CENTER));

		hotkeyGrid.add(buttons[20]);
		hotkeyGrid.add(buttons[21]);
		hotkeyGrid.add(buttons[22]);

		// Stack sub-panels
		container.add(phonePanel);
		container.add(Box.createVerticalStrut(8));
		container.add(new JSeparator(JSeparator.HORIZONTAL));
		container.add(Box.createVerticalStrut(5));
		container.add(hotkeyHeader);
		container.add(hotkeyGrid);

		return container;
	}

	public void updateDialogLocations(JFrame mainFrame)
	{
		swingDialogs[2].setLocation(mainFrame.getLocation().x+mainFrame.getSize().width, mainFrame.getLocation().y);
		swingDialogs[5].setLocation(mainFrame.getLocation().x+mainFrame.getSize().width, mainFrame.getLocation().y+swingDialogs[2].getHeight());
	}

	private void addInputButtonListeners(final boolean isGamepad)
	{
		JButton[] buttons = isGamepad ? this.gamepadButtons : this.inputButtons;
		for(int i = 0; i < buttons.length; i++)
		{
			final int buttonIndex = i;

			/* Add a focus listener to each input mapping button */
			buttons[i].addFocusListener(new FocusAdapter()
			{
				JButton focusedButton;
				String lastButtonKey = new String("");
				boolean keySet = false;

				@Override
				public void focusGained(FocusEvent e)
				{
					keySet = false;
					focusedButton = (JButton) e.getComponent();
					lastButtonKey = focusedButton.getText();
					focusedButton.setText("Waiting...");

					if(isGamepad)
					{
						// Gamepads work a bit differently from standard Swing
						// inputs in which they're not immediately available
						// or set right at boot, so if the reader is null, just
						// revert back to the prior key state.
						if (FJGUI.gamepadReader != null)
						{
							// Register listener on the gamepad event thread.
							// These must run on the EDT, so we dispatch there.
							FJGUI.gamepadReader.setInputListener(new GamepadReader.GamepadInputListener()
							{
								@Override
								public void onInputDetected(final String inputName, final int inputCode)
								{
									SwingUtilities.invokeLater(new Runnable()
									{
										@Override
										public void run()
										{
											focusedButton.setText(inputName);
											keySet = true;
											gamepadKeycodes[buttonIndex] = inputCode;
											gamepadKeyNames[buttonIndex] = inputName;

											// Processed the key, so remove listener.
											FJGUI.gamepadReader.setInputListener(null);
										}
									});
								}
							});
						}
					}
					else // Keyboard inputs
					{
						focusedButton.addKeyListener(new KeyAdapter()
						{
							public void keyPressed(KeyEvent e)
							{
								focusedButton.setText(KeyEvent.getKeyText(e.getKeyCode()));
								keySet = true;
								/* Save the new key's code into the expected index of InputKeycodes */
								inputKeycodes[buttonIndex] = e.getKeyCode();
							}
						});
					}
				}

				/* Only used to restore the last key map if the user doesn't map a new one into the button */
				@Override
				public void focusLost(FocusEvent e)
				{
					if (isGamepad && FJGUI.gamepadReader != null)
					{
						// Just remove the gamepad input listener.
						FJGUI.gamepadReader.setInputListener(null);
					}

					if(!keySet)
					{
						focusedButton.setText(lastButtonKey);
					}
				}
			});
		}
	}

	private void setActionListeners()
	{
		// Fullscreen is specific to FJGUI.
		fullScreen.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				if (hasLoadedFile()) { FreeJ2ME.app.toggleFullscreen(); }
				else { fullScreen.setSelected(FreeJ2ME.isFullscreen); }
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
					if (fpsCounterPos[i].isSelected())
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
		setToggle(immediateRepaints, "compatimmediaterepaints", true);
		setToggle(repaintOnSetCurrent, "compatrepaintonsetcurrent", true);
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
				config.updateSysSetting("soundfont", useCustomMidi.isSelected() ? "Custom" : "Default");
				hasPendingChange = true;
			}
		});

		useCustomFont.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				config.updateSysSetting("textfont", useCustomFont.isSelected() ? "Custom" : "Default");
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

		// Debug Windows are exclusive to FJGUI.
		showDebugWindows.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				boolean show = showDebugWindows.isSelected();
				if (show) { updateDialogLocations(main); }
				swingDialogs[2].setVisible(show);
				swingDialogs[5].setVisible(show);
			}
		});
	}

	private void setToggle(final AbstractButton checkbox, final String settingKey, final boolean requiresRestart)
	{
		checkbox.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				// Only act when selected or deselected (prevents double executions)
				boolean selected = checkbox.isSelected();
				config.updateSetting(settingKey, selected ? "on" : "off");
				hasPendingChange = true;
				if (requiresRestart) { showRestartDialog(); }
			}
		});
	}

	private void setSysToggle(final JCheckBoxMenuItem checkbox, final String sysSettingKey)
	{
		checkbox.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				boolean state = checkbox.isSelected();
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

	private void bindRadioGroup(final JCheckBoxMenuItem[] options, final String[] values, final String settingKey, final boolean isSysSetting, final Runnable onChange)
	{
		for (int i = 0; i < options.length; i++)
		{
			final int index = i;
			options[index].addItemListener(new ItemListener()
			{
				public void itemStateChanged(ItemEvent e)
				{
					// Swing fires events for DESELECTED and SELECTED.
					// Process ONLY when an option becomes SELECTED.
					if (e.getStateChange() == ItemEvent.SELECTED)
					{
						if (isSysSetting) { config.updateSysSetting(settingKey, values[index]); }
						else { config.updateSetting(settingKey, values[index]); }

						hasPendingChange = true;
						if (onChange != null) { onChange.run(); }
					}
				}
			});
		}
	}

	private void bindRadioGroup(JCheckBoxMenuItem[] options, String[] values, String settingKey)
	{
		bindRadioGroup(options, values, settingKey, false, null);
	}

	@SuppressWarnings("unchecked")
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
		optionMenu.add(compatSettingsMenu);
		optionMenu.add(phoneType);
		optionMenu.add(DoJaVersion);
		optionMenu.add(screenRotation);
		optionMenu.add(backlightColor);
		optionMenu.add(fpsCap);
		optionMenu.add(unlockFPSHack);
		optionMenu.add(fontOffset);
		optionMenu.add(M3GSettings);
		optionMenu.add(speedHackMenu);

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

		deleteTemporaryKJXFiles.setSelected(true);

		// Internally log levels are ordered in decreasing verbosity level
		// But UI is ordered by increasing verbosity.
		logLevel.add(logLevels[0]);
		for(int i = logLevels.length-1; i > 0; i--) { logLevel.add(logLevels[i]); }

		M3GDebug.add(M3GUntextured);
		M3GDebug.add(M3GWireframe);

		MCV3Debug.add(MCV3ShowHeapUsage);
		MCV3Debug.add(MCV3ShowTimeMetrics);


		for(int i = 0; i < supportedResolutions.length; i++) { resChoice.addItem(supportedResolutions[i]); }
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

		// add all menus to menubar
		menuBar.add(fileMenu);
		menuBar.add(optionMenu);
		menuBar.add(debugMenu);
	}

	// Each compatibility setting is a card containing name and description
	// for better readability.
	private JPanel createSettingCard(final AbstractButton checkBox, String title, String description)
	{
		final JPanel card = new JPanel(new BorderLayout(3, 0));

		// Checkboxes themselves are not interactible, we have the whole card
		// act as the checkbox for better UX.
		checkBox.setText("");
		checkBox.setOpaque(false);
		checkBox.setEnabled(false);

		// Forward mouse events from the checkbox directly to the card ifself
		checkBox.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				card.dispatchEvent(SwingUtilities.convertMouseEvent(checkBox, e, card));
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				card.dispatchEvent(SwingUtilities.convertMouseEvent(checkBox, e, card));
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				card.dispatchEvent(SwingUtilities.convertMouseEvent(checkBox, e, card));
			}
		});

		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createEtchedBorder(),
			BorderFactory.createEmptyBorder(3, 3, 3, 3)
		));

		// Allow clicking the entire card panel to toggle the checkbox, so that
		// users don't need to aim for the potentially minuscule checkbox.
		card.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				checkBox.setSelected(!checkBox.isSelected());
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				card.setBackground(new Color(225, 230, 240));
				card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				// Reset to default card color, and mouse cursor
				card.setBackground(UIManager.getColor("Panel.background"));
				card.setCursor(Cursor.getDefaultCursor());
			}
		});

		JPanel textPanel = new JPanel();
		textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
		textPanel.setOpaque(false);

		JLabel titleLabel = new JLabel(title);
		titleLabel.setFont(new Font("Dialog", Font.BOLD, 12));

		JLabel descLabel = new JLabel("<html><body style='width: 210px;'>" + description + "</body></html>");
		descLabel.setFont(new Font("Dialog", Font.BOLD, 10));
		descLabel.setForeground(Color.GRAY);

		textPanel.add(titleLabel);
		textPanel.add(Box.createVerticalStrut(5));
		textPanel.add(descLabel);

		card.add(checkBox, BorderLayout.WEST);
		card.add(textPanel, BorderLayout.CENTER);

		return card;
	}

	public void updateOptions()
	{
		// These are special checkbox cases that don't use a config on/off
		fullScreen.setSelected(FreeJ2ME.isFullscreen);
		useCustomMidi.setSelected("Custom".equals(config.sysSettings.get("soundfont")));
		useCustomFont.setSelected("Custom".equals(config.sysSettings.get("textfont")));

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
		resChoice.setSelectedItem(config.settings.get("scrwidth") + "x" + config.settings.get("scrheight"));

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
		System.arraycopy(Config.inputKeycodes, 0, inputKeycodes, 0, Config.inputKeycodes.length);
		System.arraycopy(Config.gamepadKeycodes, 0, gamepadKeycodes, 0, Config.gamepadKeycodes.length);
		System.arraycopy(Config.gamepadKeyNames, 0, gamepadKeyNames, 0, Config.gamepadKeyNames.length);
		for (int i = 0; i < inputButtons.length; i++)
		{
			inputButtons[i].setText(KeyEvent.getKeyText(inputKeycodes[i]));
			gamepadButtons[i].setText(gamepadKeyNames[i]);
		}

		firstLoad = false;
	}

	private void updateRadioGroup(JCheckBoxMenuItem[] options, String[] values, String settingKey, boolean isSysSetting)
	{
		String currentValue = isSysSetting ? config.sysSettings.get(settingKey) : config.settings.get(settingKey);
		for (int i = 0; i < options.length; i++)
		{
			options[i].setSelected(values[i].equals(currentValue));
		}
	}

	private void updateToggle(AbstractButton checkbox, String settingKey)
	{
		checkbox.setSelected("on".equals(config.settings.get(settingKey)));
	}

	private void updateSysToggle(JCheckBoxMenuItem checkbox, String sysSettingKey)
	{
		checkbox.setSelected("on".equals(config.sysSettings.get(sysSettingKey)));
	}

	class UIListener implements ActionListener
	{
		FJGUI gui = null;

		public UIListener(FJGUI gui) { this.gui = gui; }

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

				if(filename == null) { Mobile.log(Mobile.LOG_DEBUG, FJGUI.class.getPackage().getName() + "." + FJGUI.class.getSimpleName() + ": " + "Main File Loading was cancelled"); }
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
						catch(Exception e) { Mobile.log(Mobile.LOG_DEBUG, FJGUI.class.getPackage().getName() + "." + FJGUI.class.getSimpleName() + ": " + "Load error:" + e.getMessage()); }
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

				if(filename == null) { Mobile.log(Mobile.LOG_DEBUG, FJGUI.class.getPackage().getName() + "." + FJGUI.class.getSimpleName() + ": " + "SP/SP0 Loading was cancelled"); }
				else
				{
						try
						{
							spfile = new File(filePicker.getDirectory()+filename).toURI().toString();

							Mobile.getPlatform().spFileName = spfile;

							// We already loaded an app? Then we'll need to restart.
							if(hasLoadedFile()) { showRestartDialog(); }
						}
						catch(Exception e) { Mobile.log(Mobile.LOG_DEBUG, FJGUI.class.getPackage().getName() + "." + FJGUI.class.getSimpleName() + ": " + "Load error:" + e.getMessage()); }
				}
			}
			else if(command.equals("Close")) { FreeJ2ME.closeApp(); }
			else if(command.equals("Screenshot")) { ScreenShot.takeScreenshot(false); }
			else if(command.equals("PauseResume")) { MobilePlatform.pauseResumeApp(); }
			else if(command.equals("Exit")) { System.exit(0); }
			else if(command.equals("AboutMenu")) { swingDialogs[1].setLocationRelativeTo(main); swingDialogs[1].setVisible(true); }
			else if(command.equals("CloseAboutMenu")) { swingDialogs[1].setVisible(false); }
			else if(command.equals("CloseCompat")) { swingDialogs[6].setVisible(false); }
			else if(command.equals("ChangeResolution")) { swingDialogs[0].setLocationRelativeTo(main); swingDialogs[0].setVisible(true); }
			else if(command.equals("ApplyResChange"))
			{
				if(fileLoaded) /* Only update res if a jar was loaded, or else AWT throws NullPointerException */
				{
					String[] res = ((String)resChoice.getSelectedItem()).split("x");

					config.updateDisplaySize(Integer.parseInt(res[0]), Integer.parseInt(res[1]));
					hasPendingChange = true;
				}
				swingDialogs[0].setVisible(false);
			}
			else if(command.equals("CancelResChange")) { swingDialogs[0].setVisible(false); }
			else if(command.equals("RestartNow")) { Mobile.restartApp(); }
			else if(command.equals("RestartLater")) { swingDialogs[3].setVisible(false); }
			else if(command.equals("MapInputs")) { swingDialogs[4].setVisible(true); }
			else if(command.equals("CompatSettings")) { swingDialogs[6].setLocationRelativeTo(main); swingDialogs[6].setVisible(true); }
			else if(command.equals("ApplyInputs"))
			{
				System.arraycopy(inputKeycodes, 0, Config.inputKeycodes, 0, inputKeycodes.length);
				System.arraycopy(gamepadKeycodes, 0, Config.gamepadKeycodes, 0, gamepadKeycodes.length);
				System.arraycopy(gamepadKeyNames, 0, Config.gamepadKeyNames, 0, gamepadKeyNames.length);
				config.updateAWTInputs();
				swingDialogs[4].setVisible(false);
			}
			else if(command.equals("CancelInputs")) { swingDialogs[4].setVisible(false); }
			else if(command.equals("ToggleInputLayout"))
			{
				if ("Keyboard".equals(swingButtons[8].getText()))
				{
					inputLayout.show(inputPanel, "GAMEPAD");
					swingButtons[8].setText("Gamepad");
				}
				else
				{
					inputLayout.show(inputPanel, "KEYBOARD");
					swingButtons[8].setText("Keyboard");
				}
			}
			else if(command.equals("RefreshGamepads"))
			{
				ArrayList<String> availableDevices = GamepadReader.getAvailableDevices();

				// If this passes, we have a reader for this platform.
				if (availableDevices != null)
				{
					String firstDevice = availableDevices.get(0);

					// Instant OS-agnostic reader instantiation to resolve the human-readable name
					String os = System.getProperty("os.name").toLowerCase();

					// We already have a reader running? Stop it before creating another
					if (FJGUI.gamepadThread != null && FJGUI.gamepadThread.isAlive())
			        {
			            if (FJGUI.gamepadReader != null) { FJGUI.gamepadReader.stop(); }

			            FJGUI.gamepadThread.interrupt();

						// Wait for the thread a bit, so it can end normally.
			            try { FJGUI.gamepadThread.join(500); }
			            catch (InterruptedException e)  { Thread.currentThread().interrupt(); }
			        }

					if (os.contains("linux")) { FJGUI.gamepadReader = new LinuxGamepadReader(firstDevice, gui); }
					// else if (os.contains("win")) { FJGUI.gamepadReader = new WindowsGamepadReader(firstDevice, gui); }
					// else if (os.contains("mac")) { FJGUI.gamepadReader = new MacGamepadReader(firstDevice, gui); }

					gamepadName.setText("Pad: " + FJGUI.gamepadReader.getDeviceName());

					// Process gamepad inputs on a separate thread.
					FJGUI.gamepadThread = new Thread(FJGUI.gamepadReader, "GamepadThread");
					FJGUI.gamepadThread.setDaemon(true);
					FJGUI.gamepadThread.start();
				}
				else
				{
					// Close any active readers, we have no devices now.
					if (FJGUI.gamepadReader != null)
					{
					    FJGUI.gamepadReader.stop();
					    FJGUI.gamepadReader = null;
					}
					gamepadName.setText("Pad: None");
				}
			}
			else if(command.equals("ShowPlayer"))
			{
				// Create FreeJ2MEPlayer JDialog instance and show it;
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

	public JMenuBar getJMenuBar() { return menuBar; }

	public boolean hasChanged() { return hasPendingChange; }

	public void clearChanged()
	{
		allowRestartDialog = true;
		hasPendingChange = false;
	}

	public boolean hasLoadedFile() { return fileLoaded; }

	public void setMainFrame(JFrame mainFrame)
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
		// If we're still in the init stage, ignore changes that call this up.
		if(!this.allowRestartDialog) { return; }
		swingDialogs[3].setLocationRelativeTo(main);
		swingDialogs[3].setVisible(true);
	}

	public void updateDialogs()
	{
		try
		{
			// Read only new incoming lines from the stream, that way, we can
			// just append new text rather than calling upon setText() to nuke
			// any currently selected text.
			StringBuilder newLines = new StringBuilder();
			String line;
			while ((line = logReader.readLine()) != null)
			{
				newLines.append(line).append("\n");
			}

			// Only update the UI if there are actual new log messages, improves
			// performance a bit.
			if (newLines.length() > 0)
			{
				// Make sure the selection and caret are kept track of
				int selStart = logArea.getSelectionStart();
				int selEnd = logArea.getSelectionEnd();
				boolean hasSelection = (selStart != selEnd);

				logArea.append(newLines.toString());

				if (hasSelection)
				{
					logArea.setSelectionStart(selStart);
					logArea.setSelectionEnd(selEnd);
				}
				else { logArea.setCaretPosition(logArea.getDocument().getLength()); }
			}
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
