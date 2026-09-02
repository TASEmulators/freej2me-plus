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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;

import java.util.Arrays;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.MobilePlatform;

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
	final JMenu compatSettingsMenu = new JMenu("Compatibility Settings");
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
	};

	final JButton[] swingButtons =
	{
		new JButton("Close"),
		new JButton("Apply"),
		new JButton("Cancel"),
		new JButton("Restart Now"),
		new JButton("Restart later"),
		new JButton("Apply"),
		new JButton("Cancel")
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

	final JComboBox resChoice = new JComboBox();

	/* Items for each of the bar's JMenus */
	final UIListener menuItemListener = new UIListener();

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
	final JCheckBoxMenuItem fantasyZoneFix = new JCheckBoxMenuItem("Fix for Fantasy Zone 176x208 weird mirroring", false);
	final JCheckBoxMenuItem transToOriginOnReset = new JCheckBoxMenuItem("Translate to origin on gfx reset", false);
	final JCheckBoxMenuItem immediateRepaints = new JCheckBoxMenuItem("Process canvas repaints immediately", false);
	final JCheckBoxMenuItem repaintOnSetCurrent = new JCheckBoxMenuItem("Repaint on Display setCurrent.", false);
	final JCheckBoxMenuItem overridePlatChecks = new JCheckBoxMenuItem("Override Mobile Platform checks", true);
	final JCheckBoxMenuItem siemensFriendlyDrawing = new JCheckBoxMenuItem("Siemens-friendly drawing methods", false);
	final JCheckBoxMenuItem ignoreVolumeChanges = new JCheckBoxMenuItem("Ignore volume changes", false);
	final JCheckBoxMenuItem MCV3HorFovFix = new JCheckBoxMenuItem("MascotCapsuleV3 Horizontal FOV Fix", false);

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
		for(int i = 0; i < inputButtons.length; i++)
		{
			// inputButtons get a smaller font and tighter padding too.
			inputButtons[i].setMargin(new Insets(1, 1, 1, 1));
			//inputButtons[i].setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
			flattenButton(inputButtons[i]);
		}

		swingDialogs[1].setLayout( new FlowLayout(FlowLayout.CENTER, 200, 0));
		swingDialogs[1].setUndecorated(true); /* Whenever a JDialog is undecorated, it's because it's meant to look like an internal menu on FreeJ2ME's main JFrame */
		swingDialogs[1].setSize(230, 235);
		swingDialogs[1].setResizable(false);
		swingDialogs[1].setLocationRelativeTo(main);
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
		swingDialogs[0].setLocationRelativeTo(main);

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

		/* Input mapping dialog: It's a grid, so a few tricks had to be employed to align everything up */
		swingDialogs[4].getContentPane().removeAll();
		swingDialogs[4].setLayout(new BoxLayout(swingDialogs[4].getContentPane(), BoxLayout.Y_AXIS));
		swingDialogs[4].setSize(280, 480);
		swingDialogs[4].setResizable(false);
		swingDialogs[4].setLocationRelativeTo(main);

		// Header (apply and cancel buttons)
		JLabel headerLabel = new JLabel("Click any button below to map keys", SwingConstants.CENTER);
		headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		headerLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

		JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
		swingButtons[5].setForeground(Color.BLUE); // Apply
		swingButtons[6].setForeground(Color.RED);  // Cancel
		actionPanel.add(swingButtons[5]);
		actionPanel.add(swingButtons[6]); // Assuming Cancel or swingButtons[6]

		JPanel phonePanel = new JPanel(new GridLayout(0, 3, 3, 3));
		phonePanel.setMaximumSize(new Dimension(260, 240));

		phonePanel.add(inputButtons[0]); // Soft Left (Q)
		phonePanel.add(new JLabel(""));
		phonePanel.add(inputButtons[1]); // Soft Right (W)

		phonePanel.add(new JLabel(""));
		phonePanel.add(inputButtons[2]); // Up
		phonePanel.add(new JLabel(""));

		phonePanel.add(inputButtons[3]); // Left
		phonePanel.add(inputButtons[4]); // Enter/OK
		phonePanel.add(inputButtons[5]); // Right

		phonePanel.add(new JLabel(""));
		phonePanel.add(inputButtons[6]); // Down
		phonePanel.add(new JLabel(""));

		phonePanel.add(new JLabel("CLR KEY:", SwingConstants.RIGHT));
		phonePanel.add(inputButtons[19]); // Clear (A)
		phonePanel.add(new JLabel(""));

		phonePanel.add(new JLabel(""));
		phonePanel.add(new JLabel(""));
		phonePanel.add(new JLabel(""));

		// Numpad keys
		phonePanel.add(inputButtons[7]);  phonePanel.add(inputButtons[8]);  phonePanel.add(inputButtons[9]);
		phonePanel.add(inputButtons[10]); phonePanel.add(inputButtons[11]); phonePanel.add(inputButtons[12]);
		phonePanel.add(inputButtons[13]); phonePanel.add(inputButtons[14]); phonePanel.add(inputButtons[15]);
		phonePanel.add(inputButtons[16]); phonePanel.add(inputButtons[17]); phonePanel.add(inputButtons[18]);

		JPanel hotkeyHeader = new JPanel(new FlowLayout(FlowLayout.CENTER));
		hotkeyHeader.add(new JLabel("Hotkeys"));
		hotkeyHeader.add(new JLabel("(Ctrl+Alt+*)"));

		JPanel hotkeyGrid = new JPanel(new GridLayout(2, 2, 2, 2));
		hotkeyGrid.setMaximumSize(new Dimension(300, 80));

		hotkeyGrid.add(new JLabel("Fast-Forward", SwingConstants.CENTER));
		hotkeyGrid.add(new JLabel("Screenshot", SwingConstants.CENTER));
		hotkeyGrid.add(new JLabel("(Un)Pause", SwingConstants.CENTER));

		hotkeyGrid.add(inputButtons[20]);
		hotkeyGrid.add(inputButtons[21]);
		hotkeyGrid.add(inputButtons[22]);

		// Build the input menu itself now.
		swingDialogs[4].add(headerLabel);
		swingDialogs[4].add(actionPanel);
		swingDialogs[4].add(Box.createVerticalStrut(8));
		swingDialogs[4].add(new JSeparator(JSeparator.HORIZONTAL));
		swingDialogs[4].add(Box.createVerticalStrut(8));

		swingDialogs[4].add(phonePanel);

		swingDialogs[4].add(Box.createVerticalStrut(8));
		swingDialogs[4].add(new JSeparator(JSeparator.HORIZONTAL));
		swingDialogs[4].add(Box.createVerticalStrut(5));

		swingDialogs[4].add(hotkeyHeader);
		swingDialogs[4].add(hotkeyGrid);
		swingDialogs[4].add(Box.createVerticalStrut(10));


		// Restart Required Dialog
		swingDialogs[3].setLayout(new FlowLayout(FlowLayout.CENTER, 5, 10));
		swingDialogs[3].setUndecorated(true);
		swingDialogs[3].setBackground(new Color(238, 238, 238, 160));
		swingDialogs[3].setSize(240, 80);
		swingDialogs[3].setLocationRelativeTo(main);
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
		swingDialogs[5].setLocationRelativeTo(main);
		swingDialogs[5].setResizable(false);
		swingDialogs[5].add(logScrollPane, BorderLayout.CENTER);

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
		swingButtons[5].setActionCommand("ApplyInputs");
		swingButtons[6].setActionCommand("CancelInputs");

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
		swingButtons[5].addActionListener(menuItemListener);
		swingButtons[6].addActionListener(menuItemListener);

		showPlayer.addActionListener(menuItemListener);

		addInputButtonListeners();

		buildMenuBar();

		setActionListeners();
	}

	public static void flattenButton(JButton button)
	{
	    button.setContentAreaFilled(false);        // Removes the default gradient fill
	    button.setOpaque(true);                    // Allows background color to fill cleanly
	    button.setBackground(new Color(220, 220, 220)); // Sets a solid flat background
	}

	public void updateDialogLocations(JFrame mainFrame)
	{
		swingDialogs[2].setLocation(mainFrame.getLocation().x+mainFrame.getSize().width, mainFrame.getLocation().y);
		swingDialogs[5].setLocation(mainFrame.getLocation().x+mainFrame.getSize().width, mainFrame.getLocation().y+swingDialogs[2].getHeight());
	}

	private void addInputButtonListeners()
	{
		for(int i = 0; i < inputButtons.length; i++)
		{
			final int buttonIndex = i;

			/* Add a focus listener to each input mapping button */
			inputButtons[i].addFocusListener(new FocusAdapter()
			{
				JButton focusedButton;
				String lastButtonKey = new String("");
				boolean keySet = false;

				@Override
				public void focusGained(FocusEvent e)
				{
					{
						keySet = false;
						focusedButton = (JButton) e.getComponent();
						lastButtonKey = focusedButton.getText();
						focusedButton.setText("Waiting...");

						focusedButton.addKeyListener(new KeyAdapter()
						{
							public void keyPressed(KeyEvent e)
							{
								focusedButton.setText(KeyEvent.getKeyText(e.getKeyCode()));
								keySet = true;
								/* Save the new key's code into the expected index of newInputKeycodes */
								newInputKeycodes[buttonIndex] = e.getKeyCode();
							}
						});
					}
				}

				/* Only used to restore the last key map if the user doesn't map a new one into the button */
				@Override
				public void focusLost(FocusEvent e) { if(!keySet) { focusedButton.setText(lastButtonKey); } }
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

	private void setToggle(final JCheckBoxMenuItem checkbox, final String settingKey, final boolean requiresRestart)
	{
	    checkbox.addItemListener(new ItemListener()
	    {
	        public void itemStateChanged(ItemEvent e)
	        {
	            // Only act when selected or deselected (prevents double executions)
	            boolean selected = checkbox.isSelected(); // Swing method
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
	            boolean state = checkbox.isSelected(); // Swing method
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
		System.arraycopy(Config.inputKeycodes, 0, newInputKeycodes, 0, Config.inputKeycodes.length);
		for (int i = 0; i < inputButtons.length; i++)
		{
			inputButtons[i].setText(KeyEvent.getKeyText(newInputKeycodes[i]));
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

	private void updateToggle(JCheckBoxMenuItem checkbox, String settingKey)
	{
		checkbox.setSelected("on".equals(config.settings.get(settingKey)));
	}

	private void updateSysToggle(JCheckBoxMenuItem checkbox, String sysSettingKey)
	{
		checkbox.setSelected("on".equals(config.sysSettings.get(sysSettingKey)));
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
			else if(command.equals("ApplyInputs"))
			{
				System.arraycopy(newInputKeycodes, 0, Config.inputKeycodes, 0, newInputKeycodes.length);
				config.updateAWTInputs();
				swingDialogs[4].setVisible(false);
			}
			else if(command.equals("CancelInputs")) { swingDialogs[4].setVisible(false); }
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
