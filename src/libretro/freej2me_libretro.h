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
#include "libretro.h"

#define PIPE_READ_BUFFER_SIZE 32767
#define DEFAULT_FPS 60
#define BASE_WIDTH 320
#define BASE_HEIGHT 240
#define MAX_WIDTH 800
#define MAX_HEIGHT 800

/* Used as a limit to the string of core option updates to be sent to the Java app */
#define PIPE_MAX_LEN 255

// The max amount of phone keys currently supported (might increase since KDDI and SKT/SK-VM phones tend to have more)
#define PHONE_KEYS 20

static const char *supported_encodings[] = 
{
    "-Dfile.encoding=ISO_8859_1",
    "-Dfile.encoding=Shift_JIS",
    "-Dfile.encoding=EUC_KR"
};

/* Input mapping variables and descriptions */
static const struct retro_controller_description port_1[] =
{
    { "Joypad",    RETRO_DEVICE_JOYPAD },
    { "Keyboard",  RETRO_DEVICE_KEYBOARD },
    { "Empty",     RETRO_DEVICE_NONE },
    { 0 },
};

/* No use having more than one input port on this core */
static const struct retro_controller_info ports[] =
{
    { port_1, 2 },
    { 0 },
};


#define FRAMES_DROPPED_MSG 0
#define INVALID_STATUS_MSG 1
#define IMPROPER_CHILDPROC_MSG 2
#define SYSTEM_NOT_FOUND_MSG 3
#define COULD_NOT_START_MSG 4
#define UNEXPECTED_CLOS_MSG 5
#define PIPE_WRITE_FAIL_MSG 6
#define PIPE_READ_FAIL_MSG 7
#define CHILDPROC_CLOSED_MSG 8
#define CORE_HAS_LOADED_MSG 9

static const struct retro_message_ext messages[] =
{
    /* Message string to be displayed/logged */
    {"Too many frames dropped! Please restart the core.", 5000, 3, RETRO_LOG_ERROR, RETRO_MESSAGE_TARGET_ALL, RETRO_MESSAGE_TYPE_NOTIFICATION, 0},
    {"Invalid status received! Please restart the core.", 5000, 3, RETRO_LOG_ERROR, RETRO_MESSAGE_TARGET_ALL, RETRO_MESSAGE_TYPE_NOTIFICATION, 0},
    {"FreeJ2ME failed to setup pipes for communication!!! \nPlease restart the core.", 15000, 3, RETRO_LOG_ERROR, RETRO_MESSAGE_TARGET_ALL, RETRO_MESSAGE_TYPE_NOTIFICATION, 0},
#ifdef __linux__
    {"FreeJ2ME system files not found! \nMake sure > freej2me-lr.jar < is in the 'system' dir.", 15000, 3, RETRO_LOG_ERROR, RETRO_MESSAGE_TARGET_ALL, RETRO_MESSAGE_TYPE_NOTIFICATION, 0},
#elif _WIN32
    {"FreeJ2ME system files not found! \nMake sure > freej2me-lr.jar < is in the 'system' dir.", 15000, 3, RETRO_LOG_ERROR, RETRO_MESSAGE_TARGET_ALL, RETRO_MESSAGE_TYPE_NOTIFICATION, 0},
#endif
    {"FreeJ2ME could not start! \nMake sure that you have Java 6 or newer installed.", 15000, 3, RETRO_LOG_ERROR, RETRO_MESSAGE_TARGET_ALL, RETRO_MESSAGE_TYPE_NOTIFICATION, 0},
    {"FreeJ2ME closed unexpectedly!!! \nPlease restart the core.", 15000, 3, RETRO_LOG_ERROR, RETRO_MESSAGE_TARGET_ALL, RETRO_MESSAGE_TYPE_NOTIFICATION, 0},
    {"Pipe Write failed! Might be trivial, if you notice issues, please restart.", 5000, 3, RETRO_LOG_WARN, RETRO_MESSAGE_TARGET_ALL, RETRO_MESSAGE_TYPE_NOTIFICATION, 0},
    {"Pipe Read failed! Might be trivial, if you notice issues, please restart.", 5000, 3, RETRO_LOG_WARN, RETRO_MESSAGE_TARGET_ALL, RETRO_MESSAGE_TYPE_NOTIFICATION, 0},
    {"FreeJ2ME not running! Either the game crashed, or it was closed.", 5000, 3, RETRO_LOG_WARN, RETRO_MESSAGE_TARGET_ALL, RETRO_MESSAGE_TYPE_NOTIFICATION, 0},
    {"FreeJ2ME child process loaded successfully!", 3000, 1, RETRO_LOG_INFO, RETRO_MESSAGE_TARGET_OSD, RETRO_MESSAGE_TYPE_NOTIFICATION, 0},
    {"", 0, 0, 0, 0, 0, 0}
};

/* This is responsible for exposing the joypad input mappings to the frontend */
static const struct retro_input_descriptor desc[] =
{
    { 0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_LEFT,                                     "Arrow Left" },
    { 0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_UP,	                                      "Arrow Up" },
    { 0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_DOWN,                                     "Arrow Down" },
    { 0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_RIGHT,                                    "Arrow Right" },
    { 0, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_LEFT, RETRO_DEVICE_ID_ANALOG_X,           "Num 4(-), Num 6(+) : " },
    { 0, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_LEFT, RETRO_DEVICE_ID_ANALOG_Y,           "Num 2(-), Num 8(+) : " },
    { 0, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_RIGHT, RETRO_DEVICE_ID_ANALOG_X,          "Pointer Horizontal Move"},
    { 0, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_RIGHT, RETRO_DEVICE_ID_ANALOG_Y,          "Pointer Vertical Move"},
    { 0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_B,                                        "Num 7" },
    { 0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_A,                                        "Num 9" },
    { 0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_X,                                        "Num 0" },
    { 0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_Y,                                        "OK/Fire" },
    { 0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_L,                                        "Num 1" },
    { 0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_R,                                        "Num 3" },
    { 0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_R2,                                       "Num #" },
    { 0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_L2,                                       "Num *" },
    { 0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_L3,                                       "Num 5/Pointer Press" },
    { 0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_SELECT,                                   "Left Softkey" },
    { 0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_START,                                    "Right Softkey" },
    { 0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_R3,                                       "CLR" },

    { 0 },
};

/* TODO: Default keyboard input mappings, likely mirroring FreeJ2ME Standalone's defaults */

/* Categories for frontends that support config version 2 */
struct retro_core_option_v2_category option_categories[] =
{
    {
        "system_settings",
        "System",
        "Options related to FreeJ2ME's internal phone emulation such as screen resolution, rotation and game FPS limit."
    },
    {
        "advanced_settings",
        "Advanced Settings",
        "Options related to FreeJ2ME's libretro core, such as the on-screen pointer type and speed, as well as logging."
    },
    {
        "speed_hacks",
        "Speed Hacks",
        "Options that can increase FreeJ2ME or app's performance in exchange for lower compatibility by going out of J2ME specifications."
    },
    {
        "compat_settings",
        "Compatibility Settings",
        "Options that help some specific games run, but that may break others."
    },
    {
        "m3g_debug",
        "M3G Debug Settings",
        "Debug settings related to FreeJ2ME's M3G rendering implementation."
    },
    {
        "mcv3_debug",
        "MascotCapsuleV3 Debug Settings",
        "Debug settings related to FreeJ2ME's MascotCapsuleV3 renderer."
    },
};

/* Core config options if running on a frontend with support for config version 2 */
struct retro_core_option_v2_definition core_options[] =
{
    {
        "freej2me_resolution",
        "System > Phone Resolution (Core Restart may be required)",
        "Phone Resolution (Core Restart may be required)",
        "Not all J2ME games run at the same screen resolution. If the game's window is too small, or has sections of it cut off, try increasing or decreasing the internal screen resolution. Some games also break when the screen size is updated while it's running, so in those cases, a restart is required.",
        "Not all J2ME games run at the same screen resolution. If the game's window is too small, or has sections of it cut off, try increasing or decreasing the internal screen resolution. Some games also break when the screen size is updated while it's running, so in those cases, a restart is required.",
        "system_settings",
        {
            { "96x65",     NULL },
            { "101x64",    NULL },
            { "101x80",    NULL },
            { "128x128",   NULL },
            { "130x130",   NULL },
            { "120x160",   NULL },
            { "128x160",   NULL },
            { "132x176",   NULL },
            { "208x173",   NULL },
            { "176x208",   NULL },
            { "176x220",   NULL },
            { "220x176",   NULL },
            { "208x208",   NULL },
            { "180x320",   NULL },
            { "320x180",   NULL },
            { "240x240",   NULL },
            { "208x320",   NULL },
            { "240x320",   NULL },
            { "320x240",   NULL },
            { "240x400",   NULL },
            { "400x240",   NULL },
            { "240x432",   NULL },
            { "240x480",   NULL },
            { "360x360",   NULL },
            { "352x416",   NULL },
            { "360x640",   NULL },
            { "640x360",   NULL },
            { "640x480",   NULL },
            { "345x800",   NULL },
            { "800x345",   NULL },
            { "480x800",   NULL },
            { "800x480",   NULL },
            { NULL, NULL },
        },
        "240x320"
    },
    {
        "freej2me_dojaversion",
        "System > DoJa API Version",
        "DoJa API Version",
        "DoCoMo's Java VM implementation is separated into a set of different APIs with some breaking changes between major versions. This setting allows you to set a specific version that might fix any transparency, audio and gameplay issues on the DoJa/Star app you are running.",
        "DoCoMo's Java VM implementation is separated into a set of different APIs with some breaking changes between major versions. This setting allows you to set a specific version that might fix any transparency, audio and gameplay issues on the DoJa/Star app you are running.",
        "system_settings",
        {
            { "10"   "DoJa-1.0" },
            { "20",  "DoJa-2.0 & 1.5 OE" },
            { "30",  "DoJa-3.0 & 2.5 OE" },
            { "35",  "DoJa-3.5" },
            { "40",  "DoJa-4.0" },
            { "41",  "DoJa-4.1" },
            { "50",  "DoJa-5.0" },
            { "51",  "DoJa-5.1" },
            { "100", "Star-1.0" },
            { "110", "Star-1.1" },
            { "120", "Star-1.2" },
            { "130", "Star-1.3" },
            { "150", "Star-1.5" },
            { "200", "Star-2.0" },
            { NULL, NULL },
        },
        "200"
    },
    {
        "freej2me_rotate",
        "System > Rotate Screen",
        "Rotate Screen",
        "For applications that expect the screen to be rotated, this option allows you to set the rotation in 90-degree steps. 270 degrees is the most commonly used",
        "For applications that expect the screen to be rotated, this option allows you to set the rotation in 90-degree steps. 270 degrees is the most commonly used",
        "system_settings",
        {
            { "0",   "Disabled" },
            { "90",  "90 degrees"  },
            { "180", "180 degrees"  },
            { "270", "270 degrees"  },
            { NULL, NULL },
        },
        "0"
    },
    {
        "freej2me_phone",
        "System > Phone Key Layout",
        "Phone Key Layout",
        "Due to the different mobile phone manufacturers on the J2ME space, it's usual to have some games expecting a certain phone's key layout like Nokia's for example. If a game is not responding to the inputs correctly, try changing this option.",
        "Due to the different mobile phone manufacturers on the J2ME space, it's usual to have some games expecting a certain phone's key layout like Nokia's for example. If a game is not responding to the inputs correctly, try changing this option.",
        "system_settings",
        {
            { "Default",             NULL },
            { "KDDI",                NULL },
            { "LG",                  NULL },
            { "Motorola/SoftBank",   NULL },
            { "Motorola Triplets",   NULL },
            { "Motorola V8",         NULL },
            { "Nokia Full Keyboard", NULL },
            { "Sagem",               NULL },
            { "Sharp",               NULL },
            { "Siemens",             NULL },
            { "SKT",                 NULL },
            { NULL, NULL },
        },
        "Default"
    },
    {
        "freej2me_backlightcolor",
        "System > LCD Backlight Color",
        "LCD Backlight Color",
        "Mostly used for monochrome games, where they request the screen to be lit/unlit for additional effects. This option allows you to select a color for the backlight to mimic some of these devices, like Green (Nokia 3410), Cyan (Nokia 6310i), Orange (Siemens C55), etc. If the game you're running has colored graphics and requests screen backlight anyway, or you don't like those backlight effects, disable this option.",
        "Mostly used for monochrome games, where they request the screen to be lit/unlit for additional effects. This option allows you to select a color for the backlight to mimic some of these devices, like Green (Nokia 3410), Cyan (Nokia 6310i), Orange (Siemens C55), etc. If the game you're running has colored graphics and requests screen backlight anyway, or you don't like those backlight effects, disable this option.",
        "system_settings",
        {
            { "Disabled", NULL },
            { "Green",    NULL },
            { "Cyan",     NULL },
            { "Orange",   NULL },
            { "Violet",   NULL },
            { "Red",      NULL },
            { NULL, NULL },
        },
        "Green"
    },
    {
        "freej2me_fps",
        "System > Game FPS Limit",
        "Game FPS Limit",
        "The J2ME platform allows a great deal of freedom when dealing with synchronization, so while many games are locked to a certain framerate internally, others allow for variable framerates when uncapped at the cost of higher CPU usage, and some even run faster than intended when they get over a certain FPS threshold. Use the option that best suits the game at hand.",
        "The J2ME platform allows a great deal of freedom when dealing with synchronization, so while many games are locked to a certain framerate internally, others allow for variable framerates when uncapped at the cost of higher CPU usage, and some even run faster than intended when they get over a certain FPS threshold. Use the option that best suits the game at hand.",
        "system_settings",
        {
            { "Auto", "Disabled" },
            { "60",   "60 FPS"   },
            { "55",   "55 FPS"   },
            { "50",   "50 FPS"   },
            { "45",   "45 FPS"   },
            { "40",   "40 FPS"   },
            { "35",   "35 FPS"   },
            { "30",   "30 FPS"   },
            { "25",   "25 FPS"   },
            { "20",   "20 FPS"   },
            { "15",   "15 FPS"   },
            { "10",   "10 FPS"   },
            { NULL, NULL },
        },
        "Auto"
    },
    {
        "freej2me_sound",
        "System > Virtual Phone Sound (Core Restart required)",
        "Virtual Phone Sound (Core Restart required)",
        "Enables or disables the virtual phone's ability to load and play audio samples/tones. Some games require support for codecs not yet implemented, or have issues that can be worked around by disabling audio in FreeJ2ME. If a game doesn't run or has issues during longer sessions, try disabling this option.",
        "Enables or disables the virtual phone's ability to load and play audio samples/tones. Some games require support for codecs not yet implemented, or have issues that can be worked around by disabling audio in FreeJ2ME. If a game doesn't run or has issues during longer sessions, try disabling this option.",
        "system_settings",
        {
            { "on",  "On"  },
            { "off", "Off" },
            { NULL, NULL },
        },
        "on"
    },
    {
        "freej2me_midifont",
        "System > MIDI Soundfont",
        "MIDI Soundfont",
        "Selects which kind of MIDI soundfont to use. 'Default' uses the soundfont bundled with the system or Java VM, while 'Custom' allows you to place a custom soundfont on '<freej2me-lr.jar folder>/freej2me_system/customMIDI' and use it on J2ME apps to simulate a specific phone or improve MIDI sound quality. WARNING: Big soundfonts greatly increase the emulator's RAM footprint and processing requirements, while smaller ones can actually help it perform better.",
        "Selects which kind of MIDI soundfont to use. 'Default' uses the soundfont bundled with the system or Java VM, while 'Custom' allows you to place a custom soundfont on '<freej2me-lr.jar folder>/freej2me_system/customMIDI' and use it on J2ME apps to simulate a specific phone or improve MIDI sound quality. WARNING: Big soundfonts greatly increase the emulator's RAM footprint and processing requirements, while smaller ones can actually help it perform better.",
        "system_settings",
        {
            { "off", "Default" },
            { "on",  "Custom" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_textfont",
        "System > Text Font",
        "Text Font",
        "Selects whether you want to use a custom text font or not. 'Default' uses the font bundled with the system or Java VM, while 'Custom' allows you to place a custom font on '<freej2me-lr.jar folder>/freej2me_system/customFont' and use it on J2ME apps to simulate a specific phone's font family. Do note that some fonts may end up being too large or too small to fit in some screen sizes, so you might need to adjust the size offset.",
        "Selects whether you want to use a custom text font or not. 'Default' uses the font bundled with the system or Java VM, while 'Custom' allows you to place a custom font on '<freej2me-lr.jar folder>/freej2me_system/customFont' and use it on J2ME apps to simulate a specific phone's font family. Do note that some fonts may end up being too large or too small to fit in some screen sizes, so you might need to adjust the size offset.",
        "system_settings",
        {
            { "off", "Default" },
            { "on",  "Custom" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_fontoffset",
        "System > Font Size Offset",
        "Font Size Offset",
        "Adjust the offset used for font sizing in order to make text bigger or smaller. Also helps with custom fonts that might be too big or small by default.",
        "Adjust the offset used for font sizing in order to make text bigger or smaller. Also helps with custom fonts that might be too big or small by default.",
        "system_settings",
        {
            { "-4", "-4 pt" },
            { "-3", "-3 pt" },
            { "-2", "-2 pt" },
            { "-1", "-1 pt" },
            { "0", " 0 pt (Default)" },
            { "1", " 1 pt" },
            { "2", " 2 pt" },
            { "3", " 3 pt" },
            { "4", " 4 pt" },
            { NULL, NULL },
        },
        "0"
    },
    {
        "freej2me_analogasentirekeypad",
        "System > Use Analog As Entire Keypad",
        "Use Analog As Entire Keypad",
        "A few games like Time Crisis Elite and Rayman Raving Rabbids can benefit from having the analog serve as the entire keypad for smoother gameplay (in TC Elite's case, with num 5 as pressing the analog too). If you have a game that appears to benefit from this by using the diagonal keypad keys instead of allowing for num2 and num4 to be pressed simultaneously for the same effect for example, try enabling it.",
        "A few games like Time Crisis Elite and Rayman Raving Rabbids can benefit from having the analog serve as the entire keypad for smoother gameplay (in TC Elite's case, with num 5 as pressing the analog too). If you have a game that appears to benefit from this by using the diagonal keypad keys instead of allowing for num2 and num4 to be pressed simultaneously for the same effect for example, try enabling it.",
        "system_settings",
        {
            { "off", "Disabled" },
            { "on",  "Enabled" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_logginglevel",
        "Advanced Settings > Logging Level",
        "Logging Level",
        "When enabled, this option allows FreeJ2ME to log messages of the specified level and higher into 'freej2me_system/FreeJ2ME.log' to facilitate debugging",
        "When enabled, this option allows FreeJ2ME to log messages of the specified level and higher into 'freej2me_system/FreeJ2ME.log' to facilitate debugging",
        "advanced_settings",
        {
            { "0",  "Disable"           },
            { "1",  "Debug"             },
            { "2",  "Info"              },
            { "3",  "Warning"           },
            { "4",  "Error"             },
            { NULL, NULL },
        },
        "0"
    },
    {
        "freej2me_dumpaudiostreams",
        "Advanced Settings > Dump Audio Streams",
        "Dump Audio Streams",
        "This option allows FreeJ2ME to dump incoming Audio Data into $SYSTEM/FreeJ2MEDumps/Audio/appname/*, mostly useful for debugging",
        "This option allows FreeJ2ME to dump incoming Audio Data into $SYSTEM/FreeJ2MEDumps/Audio/appname/*, mostly useful for debugging",
        "advanced_settings",
        {
            { "off",  "Disable"            },
            { "on",  "Enable"              },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_dumpgraphicsdata",
        "Advanced Settings > Dump Graphics Data (Stub)",
        "Dump Graphics Data (Stub)",
        "This option allows FreeJ2ME to dump incoming Graphics Data into $SYSTEM/FreeJ2MEDumps/Graphics/appname/*, mostly useful for debugging",
        "This option allows FreeJ2ME to dump incoming Graphics Data into $SYSTEM/FreeJ2MEDumps/Audio/appname/*, mostly useful for debugging",
        "advanced_settings",
        {
            { "off",  "Disable"            },
            { "on",  "Enable"              },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_deletetempkjxfiles",
        "Advanced Settings > Delete KJX files' temporary JAR/JAD",
        "Delete KJX files' temporary JAR/JAD",
        "Disabling this option allows FreeJ2ME to keep the decompiled JAR and JAD files from a KDDI KJX container in $SYSTEM/FreeJ2MEDumps/KDDI/, useful if you want to archive those files outside their KJX container or try running them somewhere that doesn't handle KJX files",
        "Disabling this option allows FreeJ2ME to keep the decompiled JAR and JAD files from a KDDI KJX container in $SYSTEM/FreeJ2MEDumps/KDDI/, useful if you want to archive those files outside their KJX container or try running them somewhere that doesn't handle KJX files",
        "advanced_settings",
        {
            { "off",  "Disable"            },
            { "on",  "Enable"              },
            { NULL, NULL },
        },
        "on"
    },
    {
        "freej2me_pointertype",
        "Advanced Settings > Pointer Type",
        "Pointer Type",
        "This option sets the type of pointer used by FreeJ2ME, can be set to use a Mouse, a Touchscreen or neither. Please note that only Mouse supports drag and drop motions",
        "This option sets the type of pointer used by FreeJ2ME, can be set to use a Mouse, a Touchscreen or neither. Please note that only Mouse supports drag and drop motions",
        "advanced_settings",
        {
            { "Mouse",  "Mouse"                    },
            { "Touch",  "Touchscreen"              },
            { "None",   "No Pointer/Joypad Analog" },
            { NULL, NULL },
        },
        "Mouse"
    },
    {
        "freej2me_pointerxspeed",
        "Advanced Settings > Pointer X Speed",
        "Pointer X Speed",
        "This option sets the horizontal speed of the on-screen pointer when controlled by a joypad's analog stick.",
        "This option sets the horizontal speed of the on-screen pointer when controlled by a joypad's analog stick.",
        "advanced_settings",
        {
            { "2",  "Slow"    },
            { "4",  "Normal"  },
            { "8",  "Fast"    },
            { "16", "Faster"  },
            { NULL, NULL },
        },
        "4"
    },
    {
        "freej2me_pointeryspeed",
        "Advanced Settings > Pointer Y Speed",
        "Pointer Y Speed",
        "This option sets the vertical speed of the on-screen pointer when controlled by a joypad's analog stick.",
        "This option sets the vertical speed of the on-screen pointer when controlled by a joypad's analog stick.",
        "advanced_settings",
        {
            { "2",  "Slow"    },
            { "4",  "Normal"  },
            { "8",  "Fast"    },
            { "16", "Faster"  },
            { NULL, NULL },
        },
        "4"
    },
    {
        "freej2me_pointerinnercolor",
        "Advanced Settings > Pointer Inner Color",
        "Pointer Inner Color",
        "This option sets the on-screen pointer's inner color.",
        "This option sets the on-screen pointer's inner color.",
        "advanced_settings",
        {
            { "Black",  "Black"            },
            { "Red",    "Red"              },
            { "Green",  "Green"            },
            { "Blue",   "Blue"             },
            { "Yellow", "Yellow"           },
            { "Pink",   "Pink"             },
            { "Cyan",   "Cyan"             },
            { "White",  "White (Default)"  },
            { NULL, NULL },
        },
        "White"
    },
    {
        "freej2me_pointeroutercolor",
        "Advanced Settings > Pointer Outline Color",
        "Pointer Outline Color",
        "This option sets the on-screen pointer's outline color.",
        "This option sets the on-screen pointer's outline color.",
        "advanced_settings",
        {
            { "Black",  "Black (Default)"  },
            { "Red",    "Red"              },
            { "Green",  "Green"            },
            { "Blue",   "Blue"             },
            { "Yellow", "Yellow"           },
            { "Pink",   "Pink"             },
            { "Cyan",   "Cyan"             },
            { "White",  "White"            },
            { NULL, NULL },
        },
        "Black"
    },
    {
        "freej2me_pointerclickcolor",
        "Advanced Settings > Pointer Click Indicator Color",
        "Pointer Click Indicator Color",
        "This option sets the on-screen pointer's click indicator color.",
        "This option sets the on-screen pointer's click indicator color.",
        "advanced_settings",
        {
            { "Black",  "Black"            },
            { "Red",    "Red"              },
            { "Green",  "Green"            },
            { "Blue",   "Blue"             },
            { "Yellow", "Yellow (Default)" },
            { "Pink",   "Pink"             },
            { "Cyan",   "Cyan"             },
            { "White",  "White"            },
            { NULL, NULL },
        },
        "Yellow"
    },
    {
        "freej2me_spdhacknoalpha",
        "Speed Hacks > No Alpha on Blank Images (Restart Required)",
        "No Alpha on Blank Images (Restart Required)",
        "J2ME dictates that all images, including fully blank ones, have to be created with an alpha channel, and this includes the virtual phone's LCD screen. However, FreeJ2ME can create those without an alpha channel instead, cutting back on alpha processing for those images that usually are always fully painted with no transparency. Provides a measurable performance boost depending on the app with little to no side effects",
        "J2ME dictates that all images, including fully blank ones, have to be created with an alpha channel, and this includes the virtual phone's LCD screen. However, FreeJ2ME can create those without an alpha channel instead, cutting back on alpha processing for those images that usually are always fully painted with no transparency. Provides a measurable performance boost depending on the app with little to no side effects",
        "speed_hacks",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_spdhackm3ghalfres",
        "Speed Hacks > Render M3G at Half Resolution",
        "Render M3G at Half Resolution",
        "FreeJ2ME-Plus uses a software renderer for M3G (Mobile 3D Graphics), which can be intensive in more complex applications and higher phone resolutions. Use this if your cpu cannot keep up with full resolution rendering.",
        "FreeJ2ME-Plus uses a software renderer for M3G (Mobile 3D Graphics), which can be intensive in more complex applications and higher phone resolutions. Use this if your cpu cannot keep up with full resolution rendering.",
        "speed_hacks",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_spdhackmcv3halfres",
        "Speed Hacks > Render MascotCapsuleV3 at Half Resolution",
        "Render MascotCapsuleV3 at Half Resolution",
        "FreeJ2ME-Plus also uses a software renderer for MascotCapsuleV3 (courtesy of Roman Lahin, @rmn20), which can be intensive in more complex applications and higher phone resolutions. Use this if your cpu cannot keep up with full resolution rendering.",
        "FreeJ2ME-Plus also uses a software renderer for MascotCapsuleV3 (courtesy of Roman Lahin, @rmn20), which can be intensive in more complex applications and higher phone resolutions. Use this if your cpu cannot keep up with full resolution rendering.",
        "speed_hacks",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_spdhackmcv3nolight",
        "Speed Hacks > Disable MascotCapsuleV3 lighting",
        "Disable MascotCapsuleV3 lighting",
        "FreeJ2ME-Plus allows disabling all lighting operations on its MCV3 renderer. Helps games that use complex lighting setups, otherwise, doesn't have much of a performance impact.",
        "FreeJ2ME-Plus allows disabling all lighting operations on its MCV3 renderer. Helps games that use complex lighting setups, otherwise, doesn't have much of a performance impact.",
        "speed_hacks",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_spdhackfpsunlock",
        "Speed Hacks > Framerate Unlock Hack",
        "Framerate Unlock Hack",
        "Hijacks calls to Java methods normally used for delays and synchronization in order to increase the app's internal framerate. Higher aggressiveness levels increase the scope and type of calls intercepted. 'Safe' tackles only sleep() calls that reside in the same function of a rendering call, 'Extended' extends it to all sleep() calls, and 'Aggressive' goes beyond and hijacks system calls used for timing as well. Works best when the FPS limiter is set to anything other than 'Auto'.",
        "Hijacks calls to Java methods normally used for delays and synchronization in order to increase the app's internal framerate. Higher aggressiveness levels increase the scope and type of calls intercepted. 'Safe' tackles only sleep() calls that reside in the same function of a rendering call, 'Extended' extends it to all sleep() calls, and 'Aggressive' goes beyond and hijacks system calls used for timing as well. Works best when the FPS limiter is set to anything other than 'Auto'.",
        "speed_hacks",
        {
            { "0",  "Disabled (Default)"    },
            { "1",  "Safe"                  },
            { "2",  "Extended"              },
            { "3",  "Aggressive"            },
            { NULL, NULL },
        },
        "0"
    },
    {
        "freej2me_compatfantasyzonefix",
        "Compatibility Settings > Fix for Fantasy Zone 176x208 weird mirroring",
        "Fix for Fantasy Zone 176x208 weird mirroring",
        "Fantasy Zone 176x208's MIDP version goes entirely out of spec with its mirroring operation. It's broken on every other emulator out there and even on actual devices that aren't some Nokia S40 devices. This setting fixes it at the expense of breaking other applications that use the same draw path for S40.",
        "Fantasy Zone 176x208's MIDP version goes entirely out of spec with its mirroring operation. It's broken on every other emulator out there and even on actual devices that aren't some Nokia S40 devices. This setting fixes it at the expense of breaking other applications that use the same draw path for S40.",
        "compat_settings",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_compattranstooriginongfxreset",
        "Compatibility Settings > Translate to origin on gfx reset",
        "Translate to origin on gfx reset",
        "Some apps like Fantasy Zone's 128x128 version rely on the graphics object being translated to the origin before every draw, this compatibility setting helps with that, and any case where the drawn area keeps moving in any given direction for no reason.",
        "Some apps like Fantasy Zone's 128x128 version rely on the graphics object being translated to the origin before every draw, this compatibility setting helps with that, and any case where the drawn area keeps moving in any given direction for no reason.",
        "compat_settings",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_compatimmediaterepaintcalls",
        "Compatibility Settings > Process canvas repaint calls immediately",
        "Process canvas repaint calls immediately",
        "By default, J2ME expects canvas repaints to be queued up, and applications can either request serviceRepaints() or use serial calls to synchronize rendering. However, some apps might cause deadlocks by improper usage of the repaint queue and in turn, freeze. This setting may help cases where an app is freezing for no apparent reason.",
        "By default, J2ME expects canvas repaints to be queued up, and applications can either request serviceRepaints() or use serial calls to synchronize rendering. However, some apps might cause deadlocks by improper usage of the repaint queue and in turn, freeze. This setting may help cases where an app is freezing for no apparent reason.",
        "compat_settings",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_compatoverrideplatcheck",
        "Compatibility Settings > Override Mobile Platform checks",
        "Override Mobile Platform checks",
        "Some applications check against specific platform strings (such as 'Nokia', 'Siemens S60'), whenever this happens, FreeJ2ME's platform string doesn't match what they expect so they refuse to run. This setting overrides any platform strings by FreeJ2ME's own. This option helps far more than breaks, so it's on by default",
        "Some applications check against specific platform strings (such as 'Nokia', 'Siemens S60'), whenever this happens, FreeJ2ME's platform string doesn't match what they expect so they refuse to run. This setting overrides any platform strings by FreeJ2ME's own. This option helps far more than breaks, so it's on by default",
        "compat_settings",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "on"
    },
    {
        "freej2me_compatsiemensfriendlydraw",
        "Compatibility Settings > Siemens-friendly drawing methods",
        "Siemens-friendly drawing methods",
        "MIDP-Compliant J2ME drawing operations do no need to check for negative translation values in order to draw images properly. However, some Siemens apps like STCC (Swedish Touring Car Championship) won't work properly with the default behavior. This option tries to correct translations in a way that is closer to what Siemens' VM probably does drawing. Note that enabling this will break jars that use negative translations but are tailored for the J2ME specification.",
        "MIDP-Compliant J2ME drawing operations do no need to check for negative translation values in order to draw images properly. However, some Siemens apps like STCC (Swedish Touring Car Championship) won't work properly with the default behavior. This option tries to correct translations in a way that is closer to what Siemens' VM probably does drawing. Note that enabling this will break jars that use negative translations but are tailored for the J2ME specification.",
        "compat_settings",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_compatignorevolumechanges",
        "Compatibility Settings > Ignore volume changes",
        "Ignore volume changes",
        "Media playback is probably the J2ME subsystem whose implementation and utilization varies the most by vendor. Some applications go as far as setting volume changes to streams they already stopped beforehand, which can cause playback issues on other media that's currently playing. Sonic 2's MIDP versions are some such cases... enabling this option helps them.",
        "Media playback is probably the J2ME subsystem whose implementation and utilization varies the most by vendor. Some applications go as far as setting volume changes to streams they already stopped beforehand, which can cause playback issues on other media that's currently playing. Sonic 2's MIDP versions are some such cases... enabling this option helps them.",
        "compat_settings",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_compatmcv3horfovfix",
        "Compatibility Settings > MascotCapsuleV3 Horizontal FOV Fix",
        "MascotCapsuleV3 Horizontal FOV Fix",
        "Might help games meant for portrait resolutions work better in landscape resolutions.",
        "Might help games meant for portrait resolutions work better in landscape resolutions.",
        "compat_settings",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_m3grenderuntextured",
        "M3G Debug Settings > Draw only vertex colors",
        "Draw only vertex colors",
        "Enabling this makes M3G render only vertex colored, untextured polygons. Useful for debugging blending and vertex coloring seams.",
        "Enabling this makes M3G render only vertex colored, untextured polygons. Useful for debugging blending and vertex coloring seams.",
        "m3g_debug",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_m3grenderwireframe",
        "M3G Debug Settings > Draw Wireframe",
        "Draw Wireframe",
        "Enabling this makes M3G render only wireframes. Useful for debugging triangle clipping and culling.",
        "Enabling this makes M3G render only wireframes. Useful for debugging triangle clipping and culling.",
        "m3g_debug",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_mcv3showheap",
        "MascotCapsuleV3 Debug Settings > Show Heap Usage",
        "Show Heap Usage",
        "Shows how much Heap is being used by MascotCapsuleV3's renderer.",
        "Shows how much Heap is being used by MascotCapsuleV3's renderer.",
        "mcv3_debug",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_mcv3showtimestats",
        "MascotCapsuleV3 Debug Settings > Show Time Stats",
        "Show Time Stats",
        "Shows frametime statistics for the most important blocks of MascotCapsuleV3's renderer.",
        "Shows frametime statistics for the most important blocks of MascotCapsuleV3's renderer.",
        "mcv3_debug",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    { NULL, NULL, NULL, NULL, NULL, NULL, {{0}}, NULL },
};

/* Core Options v2 struct that allows us to categorize the core options on frontends that support config version 2 */
struct retro_core_options_v2 core_exposed_options =
{
    option_categories,
    core_options
};


/* ---------------------------------- config version 1 properties below ---------------------------------- */

/* Core config options if running on a frontend with support for config version 1 */
struct retro_core_option_definition core_options_v1 [] =
{
    {
        "freej2me_resolution",
        "Phone Resolution (Core Restart may be required)",
        "Not all J2ME games run at the same screen resolution. If the game's window is too small, or has sections of it cut off, try increasing or decreasing the internal screen resolution. Some games also break when the screen size is updated while it's running, so in those cases, a restart is required.",
        {
            { "96x65",     NULL },
            { "101x64",    NULL },
            { "101x80",    NULL },
            { "128x128",   NULL },
            { "130x130",   NULL },
            { "120x160",   NULL },
            { "128x160",   NULL },
            { "132x176",   NULL },
            { "208x173",   NULL },
            { "176x208",   NULL },
            { "176x220",   NULL },
            { "220x176",   NULL },
            { "208x208",   NULL },
            { "180x320",   NULL },
            { "320x180",   NULL },
            { "240x240",   NULL },
            { "208x320",   NULL },
            { "240x320",   NULL },
            { "320x240",   NULL },
            { "240x400",   NULL },
            { "400x240",   NULL },
            { "240x432",   NULL },
            { "240x480",   NULL },
            { "360x360",   NULL },
            { "352x416",   NULL },
            { "360x640",   NULL },
            { "640x360",   NULL },
            { "640x480",   NULL },
            { "345x800",   NULL },
            { "800x345",   NULL },
            { "480x800",   NULL },
            { "800x480",   NULL },
            { NULL, NULL },
        },
        "240x320"
    },
    {
        "freej2me_dojaversion",
        "DoJa API Version",
        "DoCoMo's Java VM implementation is separated into a set of different APIs with some breaking changes between major versions. This setting allows you to set a specific version that might fix any transparency, audio and gameplay issues on the DoJa/Star app you are running.",
        {
            { "10"   "DoJa-1.0" },
            { "20",  "DoJa-2.0 & 1.5 OE" },
            { "30",  "DoJa-3.0 & 2.5 OE" },
            { "35",  "DoJa-3.5" },
            { "40",  "DoJa-4.0" },
            { "41",  "DoJa-4.1" },
            { "50",  "DoJa-5.0" },
            { "51",  "DoJa-5.1" },
            { "100", "Star-1.0" },
            { "110", "Star-1.1" },
            { "120", "Star-1.2" },
            { "130", "Star-1.3" },
            { "150", "Star-1.5" },
            { "200", "Star-2.0" },
            { NULL, NULL },
        },
        "200"
    },
    {
        "freej2me_rotate",
        "Rotate Screen",
        "For applications that expect the screen to be rotated, this option allows you to set the rotation in 90-degree steps. 270 degrees is the most commonly used",
        {
            { "0",   "Disabled" },
            { "90",  "90 degrees"  },
            { "180", "180 degrees"  },
            { "270", "270 degrees"  },
            { NULL, NULL },
        },
        "0"
    },
    {
        "freej2me_phone",
        "Phone Key Layout",
        "Due to the different mobile phone manufacturers on the J2ME space, it's usual to have some games expecting a certain phone's key layout like Nokia's for example. If a game is not responding to the inputs correctly, try changing this option.",
        {
            { "Default",             NULL },
            { "KDDI",                NULL },
            { "LG",                  NULL },
            { "Motorola/SoftBank",   NULL },
            { "Motorola Triplets",   NULL },
            { "Motorola V8",         NULL },
            { "Nokia Full Keyboard", NULL },
            { "Sagem",               NULL },
            { "Sharp",               NULL },
            { "Siemens",             NULL },
            { "SKT",                 NULL },
            { NULL, NULL },
        },
        "Default"
    },
    {
        "freej2me_backlightcolor",
        "LCD Backlight Color",
        "Mostly used for monochrome games, where they request the screen to be lit/unlit for additional effects. This option allows you to select a color for the backlight to mimic some of these devices, like Green (Nokia 3410), Cyan (Nokia 6310i), Orange (Siemens C55), etc. If the game you're running has colored graphics and requests screen backlight anyway, or you don't like those backlight effects, disable this option.",
        {
            { "Disabled", NULL },
            { "Green",    NULL },
            { "Cyan",     NULL },
            { "Orange",   NULL },
            { "Violet",   NULL },
            { "Red",      NULL },
            { NULL, NULL },
        },
        "Green"
    },
    {
        "freej2me_fps",
        "Game FPS Limit",
        "The J2ME platform allows a great deal of freedom when dealing with synchronization, so while many games are locked to a certain framerate internally, others allow for variable framerates when uncapped at the cost of higher CPU usage, and some even run faster than intended when they get over a certain FPS threshold. Use the option that best suits the game at hand.",
        {
            { "Auto", "Disabled" },
            { "60",   "60 FPS"   },
            { "55",   "55 FPS"   },
            { "50",   "50 FPS"   },
            { "45",   "45 FPS"   },
            { "40",   "40 FPS"   },
            { "35",   "35 FPS"   },
            { "30",   "30 FPS"   },
            { "25",   "25 FPS"   },
            { "20",   "20 FPS"   },
            { "15",   "15 FPS"   },
            { "10",   "10 FPS"   },
            { NULL, NULL },
        },
        "Auto"
    },
    {
        "freej2me_sound",
        "Virtual Phone Sound (Core Restart required)",
        "Enables or disables the virtual phone's ability to load and play audio samples/tones. Some games require support for codecs not yet implemented, or have issues that can be worked around by disabling audio in FreeJ2ME (ID Software games such as DOOM II RPG having memory leaks with MIDI samples being one example). If a game doesn't run or has issues during longer sessions, try disabling this option.",
        {
            { "on",  "On"  },
            { "off", "Off" },
            { NULL, NULL },
        },
        "on"
    },
    {
        "freej2me_midifont",
        "MIDI Soundfont",
        "Selects which kind of MIDI soundfont to use. 'Default' uses the soundfont bundled with the system or Java VM, while 'Custom' allows you to place a custom soundfont on '<freej2me-lr.jar folder>/freej2me_system/customMIDI' and use it on J2ME apps to simulate a specific phone or improve MIDI sound quality. WARNING: Big soundfonts greatly increase the emulator's RAM footprint and processing requirements, while smaller ones can actually help it perform better.",
        {
            { "off", "Default" },
            { "on",  "Custom" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_textfont",
        "Text Font",
        "Selects whether you want to use a custom text font or not. 'Default' uses the font bundled with the system or Java VM, while 'Custom' allows you to place a custom font on '<freej2me-lr.jar folder>/freej2me_system/customFont' and use it on J2ME apps to simulate a specific phone's font family. Do note that some fonts may end up being too large or too small to fit in some screen sizes, so you might need to adjust the size offset.",
        {
            { "off", "Default" },
            { "on",  "Custom" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_fontoffset",
        "Font Size Offset",
        "Adjust the offset used for font sizing in order to make text bigger or smaller. Also helps with custom fonts that might be too big or small by default.",
        {
            { "-4", "-4 pt" },
            { "-3", "-3 pt" },
            { "-2", "-2 pt" },
            { "-1", "-1 pt" },
            { "0", " 0 pt (Default)" },
            { "1", " 1 pt" },
            { "2", " 2 pt" },
            { "3", " 3 pt" },
            { "4", " 4 pt" },
            { NULL, NULL },
        },
        "0"
    },
    {
        "freej2me_analogasentirekeypad",
        "Use Analog As Entire Keypad",
        "A few games like Time Crisis Elite and Rayman Raving Rabbids can benefit from having the analog serve as the entire keypad for smoother gameplay (in TC Elite's case, with num 5 as pressing the analog too). If you have a game that appears to benefit from this by using the diagonal keypad keys instead of allowing for num2 and num4 to be pressed simultaneously for the same effect for example, try enabling it.",
        {
            { "off", "Disabled" },
            { "on",  "Enabled" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_logginglevel",
        "Logging Level",
        "When enabled, this option allows FreeJ2ME to log messages of the specified level and higher into 'freej2me_system/FreeJ2ME.log' to facilitate debugging",
        {
            { "0",  "Disable"           },
            { "1",  "Debug"             },
            { "2",  "Info"              },
            { "3",  "Warning"           },
            { "4",  "Error"             },
            { NULL, NULL },
        },
        "0"
    },
    {
        "freej2me_dumpaudiostreams",
        "Dump Audio Streams",
        "This option allows FreeJ2ME to dump incoming Audio Data into $SYSTEM/FreeJ2MEDumps/Audio/appname/*, mostly useful for debugging",
        {
            { "off",  "Disable"            },
            { "on",  "Enable"              },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_dumpgraphicsdata",
        "Dump Graphics Data (Stub)",
        "This option allows FreeJ2ME to dump incoming Graphics Data into $SYSTEM/FreeJ2MEDumps/Audio/appname/*, mostly useful for debugging",
        {
            { "off",  "Disable"            },
            { "on",  "Enable"              },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_deletetempkjxfiles",
        "Delete KJX files' temporary JAR/JAD",
        "Disabling this option allows FreeJ2ME to keep the decompiled JAR and JAD files from a KDDI KJX container in $SYSTEM/FreeJ2MEDumps/KDDI/, useful if you want to archive those files outside their KJX container or try running them somewhere that doesn't handle KJX files",
        {
            { "off",  "Disable"            },
            { "on",  "Enable"              },
            { NULL, NULL },
        },
        "on"
    },
    {
        "freej2me_pointertype",
        "Pointer Type",
        "This option sets the type of pointer used by FreeJ2ME, can be set to use a Mouse, a Touchscreen or neither. Please note that only Mouse supports drag and drop motions",
        {
            { "Mouse",  "Mouse"                    },
            { "Touch",  "Touchscreen"              },
            { "None",   "No Pointer/Joypad Analog" },
            { NULL, NULL },
        },
        "Mouse"
    },
    {
        "freej2me_pointerxspeed",
        "Pointer X Speed",
        "This option sets the horizontal speed of the on-screen pointer when controlled by a joypad's analog stick.",
        {
            { "2",  "Slow"    },
            { "4",  "Normal"  },
            { "8",  "Fast"    },
            { "16", "Faster"  },
            { NULL, NULL },
        },
        "4"
    },
    {
        "freej2me_pointeryspeed",
        "Pointer Y Speed",
        "This option sets the vertical speed of the on-screen pointer when controlled by a joypad's analog stick.",
        {
            { "2",  "Slow"    },
            { "4",  "Normal"  },
            { "8",  "Fast"    },
            { "16", "Faster"  },
            { NULL, NULL },
        },
        "4"
    },
    {
        "freej2me_pointerinnercolor",
        "Pointer Inner Color",
        "This option sets the on-screen pointer's inner color.",
        {
            { "Black",  "Black"            },
            { "Red",    "Red"              },
            { "Green",  "Green"            },
            { "Blue",   "Blue"             },
            { "Yellow", "Yellow"           },
            { "Pink",   "Pink"             },
            { "Cyan",   "Cyan"             },
            { "White",  "White (Default)"  },
            { NULL, NULL },
        },
        "White"
    },
    {
        "freej2me_pointeroutercolor",
        "Pointer Outline Color",
        "This option sets the on-screen pointer's outline color.",
        {
            { "Black",  "Black (Default)"  },
            { "Red",    "Red"              },
            { "Green",  "Green"            },
            { "Blue",   "Blue"             },
            { "Yellow", "Yellow"           },
            { "Pink",   "Pink"             },
            { "Cyan",   "Cyan"             },
            { "White",  "White"            },
            { NULL, NULL },
        },
        "Black"
    },
    {
        "freej2me_pointerclickcolor",
        "Pointer Click Indicator Color",
        "This option sets the on-screen pointer's click indicator color.",
        {
            { "Black",  "Black"            },
            { "Red",    "Red"              },
            { "Green",  "Green"            },
            { "Blue",   "Blue"             },
            { "Yellow", "Yellow (Default)" },
            { "Pink",   "Pink"             },
            { "Cyan",   "Cyan"             },
            { "White",  "White"            },
            { NULL, NULL },
        },
        "Yellow"
    },
    {
        "freej2me_spdhacknoalpha",
        "No Alpha on Blank Images (Restart Required)",
        "J2ME dictates that all images, including fully blank ones, have to be created with an alpha channel, and this includes the virtual phone's LCD screen. However, FreeJ2ME can create those without an alpha channel instead, cutting back on alpha processing for those images that usually are always fully painted with no transparency. Provides a measurable performance boost depending on the app with little to no side effects",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_spdhackm3ghalfres",
        "Render M3G at Half Resolution",
        "FreeJ2ME-Plus uses a software renderer for M3G (Mobile 3D Graphics), which can be intensive in more complex applications and higher phone resolutions. Use this if your cpu cannot keep up with full resolution rendering.",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_spdhackmcv3halfres",
        "Render MascotCapsuleV3 at Half Resolution",
        "FreeJ2ME-Plus also uses a software renderer for MascotCapsuleV3 (courtesy of Roman Lahin, @rmn20), which can be intensive in more complex applications and higher phone resolutions. Use this if your cpu cannot keep up with full resolution rendering.",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_spdhackmcv3nolight",
        "Disable MascotCapsuleV3 lighting",
        "FreeJ2ME-Plus allows disabling all lighting operations on its MCV3 renderer. Helps games that use complex lighting setups, otherwise, doesn't have much of a performance impact.",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_spdhackfpsunlock",
        "Framerate Unlock Hack",
        "Hijacks calls to Java methods normally used for delays and synchronization in order to increase the app's internal framerate. Higher aggressiveness levels increase the scope and type of calls intercepted. 'Safe' tackles only sleep() calls that reside in the same function of a rendering call, 'Extended' extends it to all sleep() calls, and 'Aggressive' goes beyond and hijacks system calls used for timing as well. Works best when the FPS limiter is set to anything other than 'Auto'.",
        {
            { "0",  "Disabled (Default)"    },
            { "1",  "Safe"                  },
            { "2",  "Extended"              },
            { "3",  "Aggressive"            },
            { NULL, NULL },
        },
        "0"
    },
    {
        "freej2me_compatfantasyzonefix",
        "Fix for Fantasy Zone 176x208 weird mirroring",
        "Fantasy Zone 176x208's MIDP version goes entirely out of spec with its mirroring operation. It's broken on every other emulator out there and even on actual devices that aren't some Nokia S40 devices. This setting fixes it at the expense of breaking other applications that use the same draw path for S40.",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_compattranstooriginongfxreset",
        "Translate to origin on gfx reset",
        "Some apps like Fantasy Zone's 128x128 version rely on the graphics object being translated to the origin before every draw, this compatibility setting helps with that, and any case where the drawn area keeps moving in any given direction for no reason.",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_compatimmediaterepaintcalls",
        "Process canvas repaint calls immediately",
        "By default, J2ME expects canvas repaints to be queued up, and applications can either request serviceRepaints() or use serial calls to synchronize rendering. However, some apps might cause deadlocks by improper usage of the repaint queue and in turn, freeze. This setting may help cases where an app is freezing for no apparent reason.",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_compatoverrideplatcheck",
        "Override Mobile Platform checks",
        "Some applications check against specific platform strings (such as 'Nokia', 'Siemens S60'), whenever this happens, FreeJ2ME's platform string doesn't match what they expect so they refuse to run. This setting overrides any platform strings by FreeJ2ME's own. This option helps far more than breaks, so it's on by default",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "on"
    },
    {
        "freej2me_compatsiemensfriendlydraw",
        "Siemens-friendly drawing methods",
        "MIDP-Compliant J2ME drawing operations do no need to check for negative translation values in order to draw images properly. However, some Siemens apps like STCC (Swedish Touring Car Championship) won't work properly with the default behavior. This option tries to correct translations in a way that is closer to what Siemens' VM probably does drawing. Note that enabling this will break jars that use negative translations but are tailored for the J2ME specification.",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_compatignorevolumechanges",
        "Ignore volume changes",
        "Media playback is probably the J2ME subsystem whose implementation and utilization varies the most by vendor. Some applications go as far as setting volume changes to streams they already stopped beforehand, which can cause playback issues on other media that's currently playing. Sonic 2's MIDP versions are some such cases... enabling this option helps them.",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_compatmcv3horfovfix",
        "MascotCapsuleV3 Horizontal FOV Fix",
        "Might help games meant for portrait resolutions work better in landscape resolutions.",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_m3grenderuntextured",
        "Draw only vertex colors",
        "Enabling this makes M3G render only vertex colored, untextured polygons. Useful for debugging blending and vertex coloring seams.",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_m3grenderwireframe",
        "Draw Wireframe",
        "Enabling this makes M3G render only wireframes. Useful for debugging triangle clipping and culling.",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_mcv3showheap",
        "Show Heap Usage",
        "Shows how much Heap is being used by MascotCapsuleV3's renderer.",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    {
        "freej2me_mcv3showtimestats",
        "Show Time Stats",
        "Shows frametime statistics for the most important blocks of MascotCapsuleV3's renderer.",
        {
            { "on",  "Enabled"            },
            { "off", "Disabled (Default)" },
            { NULL, NULL },
        },
        "off"
    },
    { NULL, NULL, NULL, {{0}}, NULL },
};


/* ---------------------------------- properties for frontends without support for CORE_OPTIONS below ---------------------------------- */

/* Core config variables if running on a legacy frontend without support for CORE_OPTIONS */
static const struct retro_variable vars[] =
{
    { /* Screen Resolution */
        "freej2me_resolution",
        "Phone Resolution (Core Restart may be required); 240x320|96x65|101x64|101x80|128x128|130x130|120x160|128x160|132x176|208x173|176x208|176x220|220x176|208x208|180x320|320x180|240x240|208x320|320x240|240x400|400x240|240x432|240x480|360x360|352x416|360x640|640x360|640x480|345x800|800x345|480x800|800x480"
    },
    { /* DoJa API Version */
        "freej2me_dojaversion",
        "DoJa API Version; 200|10|20|30|35|40|41|50|51|100|110|120|130|150",
    },
    { /* Screen Rotation */
        "freej2me_rotate",
        "Rotate Screen; 0|90|180|270" 
    },
    { /* Phone Control Type */
        "freej2me_phone",
        "Phone Key Layout; Default|KDDI|LG|Motorola/SoftBank|Motorola Triplets|Motorola V8|Nokia Full Keyboard|Sagem|Sharp|Siemens|SKT"
    },
    { /* LCD Backlight Color */
        "freej2me_backlightcolor",
        "LCD Backlight Color; Green|Disabled|Cyan|Orange|Violet|Red"
    },
    { /* Game FPS limit */
        "freej2me_fps",
        "Game FPS Limit; Auto|60|55|50|45|40|35|30|25|20|15|10" 
    },
    { /* Virtual Phone Sound */
        "freej2me_sound",
        "Virtual Phone Sound (Core Restart required); on|off"
    },
    { /* MIDI Soundfont */
        "freej2me_midifont",
        "MIDI Soundfont; off|on"
    },
    { /* Custom Text Font */
        "freej2me_textfont",
        "Text Font; off|on"
    },
    { /* Custom Text Font */
        "freej2me_fontoffset",
        "Font Size Offset; 0|-4|-3|-2|-1|1|2|3|4"
    },
    { /* Use Analog As Entire Keypad */
        "freej2me_analogasentirekeypad",
        "Use Analog As Entire Keypad; off|on"
    },
    { /* Logging Level */
        "freej2me_logginglevel",
        "Dump Audio Streams; 0|1|2|3|4"
    },
    { /* Dump Audio Streams */
        "freej2me_dumpaudiostreams",
        "Dump Audio Streams; off|on"
    },
    { /* Dump Graphics Streams */
        "freej2me_dumpgraphicsdata",
        "Dump Graphics Data (Stub); off|on",
    },
    { /* Dump KJX files' temporary JAR/JAD */
        "freej2me_deletetempkjxfiles",
        "Delete KJX files' temporary JAR/JAD; on|off",
    },
    { /* Pointer Type */
        "freej2me_pointertype",
        "Pointer Type; Mouse|Touch|None"
    },
    { /* Screen Pointer X Speed */
        "freej2me_pointerxspeed",
        "Pointer X Speed; 4|1|2|8|16"
    },
    { /* Screen Pointer Y Speed */
        "freej2me_pointeryspeed",
        "Pointer Y Speed; 4|1|2|8|16"
    },
    { /* Pointer's inner color */
        "freej2me_pointerinnercolor",
        "Pointer Inner Color; White|Red|Green|Blue|Yellow|Pink|Cyan|Black"
    },
    { /* Pointer's outline color */
        "freej2me_pointeroutercolor",
        "Pointer Outline Color; Black|Red|Green|Blue|Yellow|Pink|Cyan|White"
    },
    { /* Pointer's click indicator color */
        "freej2me_pointerclickcolor",
        "Pointer Click Indicator Color; Yellow|Black|Red|Green|Blue|Pink|Cyan|White"
    },
    { /* No Alpha on Blank Images speed hack */
        "freej2me_spdhacknoalpha",
        "No Alpha on Blank Images(SpeedHack); off|on"
    },
    { /* Render M3G at Half Resolution speed hack */
        "freej2me_spdhackm3ghalfres",
        "Render M3G at Half Resolution(SpeedHack); off|on",
    },
    { /* Render MascotCapsuleV3 at Half Resolution */
        "freej2me_spdhackmcv3halfres",
        "Render MascotCapsuleV3 at Half Resolution(SpeedHack); off|on",
    },
    { /* Disable MascotCapsuleV3 lighting */
        "freej2me_spdhackmcv3nolight",
        "Disable MascotCapsuleV3 lighting(SpeedHack); off|on",
    },
    { /* Framerate Unlock Hack */
        "freej2me_spdhackfpsunlock",
        "Framerate Unlock Hack; 0|1|2|3"
    },
    { /* Fix for Fantasy Zone 176x208 setting */
        "freej2me_compatfantasyzonefix",
        "Fix for Fantasy Zone 176x208 weird mirroring; off|on"
    },
    { /* Translate to origin on gfx reset setting */
        "freej2me_compattranstooriginongfxreset",
        "Translate to origin on gfx reset; off|on"
    },
    { /* Process canvas repaint calls immediately */
        "freej2me_compatimmediaterepaintcalls",
        "Process canvas repaint calls immediately; off|on"
    },
    { /* Override Mobile Platform checks */
        "freej2me_compatoverrideplatcheck",
        "Override Mobile Platform checks; on|off",
    },
    { /* Siemens-friendly drawing methods */
        "freej2me_compatsiemensfriendlydraw",
        "Siemens-friendly drawing methods; off|on",
    },
    { /* Ignore volume changes */
        "freej2me_compatignorevolumechanges",
        "Ignore volume changes; off|on",
    },
    { /* MascotCapsuleV3 Horizontal FOV Fix */
        "freej2me_compatmcv3horfovfix",
        "MascotCapsuleV3 Horizontal FOV Fix; off|on",
    },
    { /* M3G draw only vertex colors */
        "freej2me_m3grenderuntextured",
        "M3G Draw only vertex colors; off|on"
    },
    { /* M3G draw wireframe */
        "freej2me_m3grenderwireframe",
        "M3G Draw Wireframe; off|on"
    },
    { /* MascotCapsuleV3 Show Heap Usage */
        "freej2me_mcv3showheap",
        "MascotCapsuleV3 Show Heap Usage; off|on"
    },
    { /* MascotCapsuleV3 Show Time Stats */
        "freej2me_mcv3showtimestats",
        "MascotCapsuleV3 Show Time Stats; off|on"
    },
    { NULL, NULL },
};
