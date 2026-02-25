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
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#ifdef __linux__
#include <stdarg.h>
#include <unistd.h>
#include <sys/wait.h>
#elif _WIN32
#include <windows.h>
#include <tlhelp32.h>
#include <io.h>
#endif
#include "freej2me_libretro.h"
#include <file/file_path.h>
#include <retro_miscellaneous.h>

#define NUM_ARGUMENTS 36

const char *slash = path_default_slash();

// These may change to .so and .dll at some point
#ifdef __linux__
const char *freej2meapp = "freej2me-lr.jar";
#elif _WIN32
const char *freej2meapp = "freej2me-lr.jar";
#endif

retro_environment_t Environ;
retro_video_refresh_t Video;
retro_audio_sample_t Audio;
retro_audio_sample_batch_t AudioBatch;
retro_input_poll_t InputPoll;
retro_input_state_t InputState;
static struct retro_rumble_interface rumble;

static struct retro_log_callback logging;
static retro_log_printf_t log_fn;

void retro_set_video_refresh(retro_video_refresh_t fn) { Video = fn; }
void retro_set_audio_sample(retro_audio_sample_t fn) { Audio = fn; }
void retro_set_audio_sample_batch(retro_audio_sample_batch_t fn) { AudioBatch = fn;}

static void fallback_log(enum retro_log_level level, const char *fmt, ...)
{
   (void)level;
   va_list va;
   va_start(va, fmt);
   vfprintf(stderr, fmt, va);
   va_end(va);
}

void retro_set_environment_core_info(retro_environment_t fn)
{
	int core_opt_version = 0;

	/* Logging support */
	if (fn(RETRO_ENVIRONMENT_GET_LOG_INTERFACE, &logging)) { log_fn = logging.log; }
	else { log_fn = fallback_log; }

	/* Checks if the core options version is v2 or v1*/
	if (!Environ(RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION, &core_opt_version)) { core_opt_version = 0; }

	Environ(RETRO_ENVIRONMENT_SET_CONTROLLER_INFO, (void*)ports);
	Environ(RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS, (void*)desc);

	if (core_opt_version >= 2) { Environ(RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2, &core_exposed_options); }
	else if (core_opt_version >= 1) { Environ(RETRO_ENVIRONMENT_SET_CORE_OPTIONS, (void*)core_options); }
	else { Environ(RETRO_ENVIRONMENT_SET_VARIABLES, (void*)vars); }
}

void retro_set_environment(retro_environment_t fn)
{
	Environ = fn;

	retro_set_environment_core_info(fn);
}

void retro_set_input_poll(retro_input_poll_t fn) { InputPoll = fn; }
void retro_set_input_state(retro_input_state_t fn) { InputState = fn; }

/* Global variables */
struct retro_game_geometry Geometry;


bool isRunning();
bool javaOpen(char *cmd, char **params);

#ifdef __linux__
int javaProcess;
int pRead[2];
int pWrite[2];

#elif _WIN32
BOOL succeeded = FALSE;
PROCESS_INFORMATION javaProcess;
STARTUPINFO startInfo;
HANDLE pRead[2];
HANDLE pWrite[2];
#endif

int joypad[PHONE_KEYS]; /* joypad state */
int joypre[PHONE_KEYS]; /* joypad previous state */
unsigned char joyevent[5] = { 0,0,0,0,0 };

int joymouseX = 0;
int joymouseY = 0;
long joymouseTime = 0; /* countdown to show/hide mouse cursor */
long joymouseClickedTime = 0; /* Countdown to show/hide the cursor in the clicked state */
bool joymouseAnalog = false; /* flag - using analog stick for mouse movement */
int mouseLpre = 0; /* old mouse button state */
int rumbleTime = 0; /* Rumble duration calculated based on data received from FreeJ2ME's app */
unsigned short rumbleStrength = 0xFFFF; /* Rumble strength calculated based on data received from FreeJ2ME's app */
bool uses_mouse = true;
bool uses_pointer = false;
bool booted = false;
bool restarting = false;
bool useAnalogAsEntireKeypad = false; // Enhancement for games like Time Crisis Elite which use the keypad's diagonals exclusively, and not 2+4, 6+8, etc.
float analogDeadzone = 0.10f; // Additional Deadzone over libretro for input reads

bool stoppedRunning = false;
bool resetRequested = false;
unsigned int characterEncoding = 0; // Character encoding used by FreeJ2ME's app. Normally starts with UTF-8

unsigned char readBuffer[PIPE_READ_BUFFER_SIZE];

unsigned int frameWidth = MAX_WIDTH;
unsigned int frameHeight = MAX_HEIGHT;
unsigned int frameSize = MAX_WIDTH * MAX_HEIGHT;
unsigned int frameBufferSize = MAX_WIDTH * MAX_HEIGHT * 3;
unsigned int frame[MAX_WIDTH * MAX_HEIGHT];
unsigned char frameBuffer[MAX_WIDTH * MAX_HEIGHT * 3];
unsigned char frameHeader[15]; // This is always frameHeader's length in Libretro.java -1 (we read the status byte separately)
struct retro_game_info gameinfo;

bool frameRequested = false;
bool fast_forwarding = false;
int framesDropped = 0;

float normal_throttle_rate = DEFAULT_FPS;

/* Libretro exposed config variables START */

char *options_update; /* String containing the options updated in check_variables() */
char *systemPath; /* Path of FreeJ2ME's app */
char** params; /* Char matrix containing launch arguments */
unsigned int optstrlen; /* length of the string above */
unsigned long int screenRes[2]; /* {width, height} */
int rotateScreen = 0; /* Allows rotations between 0, 90, 180 and 270 degrees */
int phoneType = 0; /* 0=Standard (Nokia/Sony/Samsung), 1=LG, 2=Motorola/SoftBank, 3=Motorola Triplets... */
int backlightColor = 1; /* 0=Disabled, 1=Green, etc. */
int gameFPS; /* Auto(0), 60, 55, 50, 45, 40, 35, 30, 25, 20, 15, 10 */
int soundEnabled; /* also acts as a boolean */
int customMidi; /* Also acts as a boolean */
int customFont; /* Also acts as a boolean */
int fontOffset = 0; /* -4, -3, -2, -1, 0 (Default), 1, 2, 3, 4 */
int dumpAudioStreams;
int dumpGraphicsData; // Not really used yet
int deleteTemporaryKJXFiles;
int loggingLevel;
int m3gUntextured;
int m3gWireframe;
int mcv3Heap;
int mcv3TimeStats;
int dojaVersion = 200;
/* Variables used to manage the pointer speed when controlled from an analog stick */
int pointerXSpeed = 8;
int pointerYSpeed = 8;
/* Variables containing the on-screen pointer's colors */
unsigned int pointerInnerColor   = 0x000000;
unsigned int pointerOutlineColor = 0xFFFFFF;
unsigned int pointerClickedColor = 0xFFFF00;

/* Speed Hack section */
unsigned int spdHackNoAlpha = 0; // Boolean
unsigned int spdHackM3GHalfRes = 0; // Boolean
unsigned int spdFrameRateUnlock = 0; // Boolean
unsigned int spdHackMCV3HalfRes = 0; // Boolean
unsigned int spdHackMCV3NoLight = 0; // Boolean

/* Compatibility Settings section */
unsigned int compatFantasyZoneFix          = 0; // Boolean
unsigned int compatTransToOriginOnGFXReset = 0; // Boolean
unsigned int compatImmediateRepaintCalls   = 0; // Boolean
unsigned int compatOverridePlatCheck       = 1; // Boolean
unsigned int compatSiemensFriendlyDraw     = 0; // Boolean
unsigned int compatIgnoreVolumeChanges     = 0; // Boolean
unsigned int compatMCV3HorFovFix           = 0; // Boolean

/* Libretro exposed config variables END */

/* First byte is the identifier, next four are width and height */
unsigned char javaRequestFrame[5] = { 0xF, 0, 0, 0, 0 };

/* mouse cursor image */
unsigned int joymouseImage[408] =
{
	0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,0,0,
	0,0,0,0,1,2,2,1,0,0,0,0,0,0,0,0,0,
	0,0,0,0,1,2,2,1,0,0,0,0,0,0,0,0,0,
	0,0,0,0,1,2,2,1,0,0,0,0,0,0,0,0,0,
	0,0,0,0,1,2,2,1,1,1,0,0,0,0,0,0,0,
	0,0,0,0,1,2,2,1,2,2,1,1,1,0,0,0,0,
	0,0,0,0,1,2,2,1,2,2,1,2,2,1,1,0,0,
	0,0,0,0,1,2,2,1,2,2,1,2,2,1,2,1,0,
	1,1,1,0,1,2,2,1,2,2,1,2,2,1,2,2,1,
	1,2,2,1,1,2,2,2,2,2,2,2,2,1,2,2,1,
	1,2,2,2,1,2,2,2,2,2,2,2,2,2,2,2,1,
	0,1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1,
	0,0,1,2,2,2,2,2,2,2,2,2,2,2,2,2,1,
	0,0,1,2,2,2,2,2,2,2,2,2,2,2,2,2,1,
	0,0,0,1,2,2,2,2,2,2,2,2,2,2,2,2,1,
	0,0,0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,
	0,0,0,0,1,2,2,2,2,2,2,2,2,2,2,1,0,
	0,0,0,0,1,2,2,2,2,2,2,2,2,2,2,1,0,
	0,0,0,0,0,1,2,2,2,2,2,2,2,2,1,0,0,
	0,0,0,0,0,1,2,2,2,2,2,2,2,2,1,0,0,
	0,0,0,0,0,1,1,1,1,1,1,1,1,1,1,0,0
};

/* mouse cursor clicked image */
unsigned int joymouseClickedImage[408] =
{
	0,0,1,0,0,0,0,0,0,1,0,0,0,0,0,0,0,
	0,1,3,1,0,0,0,0,1,3,1,0,0,0,0,0,0,
	0,0,1,3,1,0,0,1,3,1,0,0,0,0,0,0,0,
	0,0,0,1,3,1,1,3,1,0,0,0,0,0,0,0,0,
	0,0,1,3,1,2,2,1,3,1,0,0,0,0,0,0,0,
	0,1,3,1,1,2,2,1,1,3,1,0,0,0,0,0,0,
	0,0,1,0,1,2,2,1,0,1,0,0,0,0,0,0,0,
	0,0,0,0,1,2,2,1,1,1,0,0,0,0,0,0,0,
	0,0,0,0,1,2,2,1,2,2,1,1,1,0,0,0,0,
	0,0,0,0,1,2,2,1,2,2,1,2,2,1,1,0,0,
	0,0,0,0,1,2,2,1,2,2,1,2,2,1,2,1,0,
	1,1,1,0,1,2,2,1,2,2,1,2,2,1,2,2,1,
	1,2,2,1,1,2,2,2,2,2,2,2,2,1,2,2,1,
	1,2,2,2,1,2,2,2,2,2,2,2,2,2,2,2,1,
	0,1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1,
	0,0,1,2,2,2,2,2,2,2,2,2,2,2,2,2,1,
	0,0,1,2,2,2,2,2,2,2,2,2,2,2,2,2,1,
	0,0,0,1,2,2,2,2,2,2,2,2,2,2,2,2,1,
	0,0,0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,
	0,0,0,0,1,2,2,2,2,2,2,2,2,2,2,1,0,
	0,0,0,0,1,2,2,2,2,2,2,2,2,2,2,1,0,
	0,0,0,0,0,1,2,2,2,2,2,2,2,2,1,0,0,
	0,0,0,0,0,1,2,2,2,2,2,2,2,2,1,0,0,
	0,0,0,0,0,1,1,1,1,1,1,1,1,1,1,0,0
};

/*
 * Custom functions to read from, and write to, pipes.
 * Those functions are used to simplify the pipe communication
 * code to be platform-independent as all ifdefs will only need
 * to be done here instead of all around the core whenever a
 * pipe write/read is requested.
 */
#ifdef __linux__
void write_to_pipe(int pipe, void *data, int datasize) { if(isRunning()) { write(pipe, data, datasize); } }
int read_from_pipe(int pipe, void *data, int datasize) { return isRunning() ? read(pipe, data, datasize) : -1; }

#elif _WIN32
void write_to_pipe(void* pipe, void *data, int datasize)
{
	if(!isRunning()) { return; }
	BOOL succeeded = FALSE;
	succeeded = WriteFile(
		pipe,               /* pipe handle */
		data,               /* message */
		datasize,           /* message length */
		NULL,               /* bytes written (not needed) */
		NULL);              /* not overlapped */
	if (!succeeded)
	{
		log_fn(RETRO_LOG_WARN, "Failed to write to pipe. Error: %d!\n", GetLastError() );
		Environ(RETRO_ENVIRONMENT_SET_MESSAGE_EXT, (void*)&messages[PIPE_WRITE_FAIL_MSG]);
	}
}

int read_from_pipe(void* pipe, void *data, int datasize)
{
	if(!isRunning()) { return -1; }
	BOOL succeeded = FALSE;
	/*
	 * BytesRead is basically a long unsigned int which is
	 * casted to int later in order to keep compatibility with
	 * the unix origins of this file. The amount of bytes read
	 * doesn't ever go beyond what is allowed on an int, so this
	 * cast can be considered safe.
	 */
	long unsigned int bytesRead = 0;

	succeeded = ReadFile( /* Same args as WriteFile */
		pipe,
		data,
		datasize,
		&bytesRead,
		NULL);
	if (!succeeded)
	{
		log_fn(RETRO_LOG_WARN, "Failed to read from pipe. Error: %d!\n", GetLastError() );
		Environ(RETRO_ENVIRONMENT_SET_MESSAGE_EXT, (void*)&messages[PIPE_READ_FAIL_MSG]);
	}

	return (int) bytesRead;
}
#endif

int freej2me_present(const char *path) 
{
#ifdef _WIN32
    return _access(path, 0) == 0;
#else
    return access(path, F_OK) == 0;
#endif
}

// Fast-forward state tracker
void check_fast_forwarding(void) 
{
	unsigned int multiplier_scaled = 0;

    if (Environ(RETRO_ENVIRONMENT_GET_FASTFORWARDING, &fast_forwarding)) 
	{
        javaRequestFrame[4] = fast_forwarding ? 1 : 0;
    }

	struct retro_throttle_state throttle_state;
	if (Environ(RETRO_ENVIRONMENT_GET_THROTTLE_STATE, &throttle_state))
	{
		if (throttle_state.mode == RETRO_THROTTLE_NONE && throttle_state.rate > 0.0f)
		{
			normal_throttle_rate = throttle_state.rate;
		}

		if (throttle_state.rate > 0.0f && normal_throttle_rate > 0.0f)
		{
			float multiplier = throttle_state.rate / normal_throttle_rate;
			if (multiplier < 0.0f) { multiplier = 0.0f; }
			if (multiplier > 655.35f) { multiplier = 655.35f; }
			multiplier_scaled = (unsigned int)(multiplier * 100.0f + 0.5f);
		}
	}

	javaRequestFrame[1] = (multiplier_scaled >> 8) & 0xFF;
	javaRequestFrame[2] = (multiplier_scaled) & 0xFF;
}

/* Function to check the core's config states in the libretro frontend */
static void check_variables(bool first_time_startup)
{
	struct retro_variable var = {0};


	var.key = "freej2me_resolution";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		char *resChar;
		char str[100];
		snprintf(str, sizeof(str), "%s", var.value);

		resChar = strtok(str, "x");
		if (resChar) { screenRes[0] = strtoul(resChar, NULL, 0); }
		resChar = strtok(NULL, "x");
		if (resChar) { screenRes[1] = strtoul(resChar, NULL, 0); }
	}


	var.key = "freej2me_rotate";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "0"))        { rotateScreen = 0; }
		else if (!strcmp(var.value, "90"))  { rotateScreen = 1; }
		else if (!strcmp(var.value, "180")) { rotateScreen = 2; }
		else if (!strcmp(var.value, "270")) { rotateScreen = 3; }
	}


	var.key = "freej2me_phone";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "Default"))                 { phoneType = 0; }
		else if (!strcmp(var.value, "LG"))                 { phoneType = 1; }
		else if (!strcmp(var.value, "Motorola/SoftBank"))  { phoneType = 2; }
		else if (!strcmp(var.value, "Motorola Triplets"))  { phoneType = 3; }
		else if (!strcmp(var.value, "Motorola V8"))        { phoneType = 4; }
		else if (!strcmp(var.value, "Nokia Full Keyboard")){ phoneType = 5; }
		else if (!strcmp(var.value, "Sagem"))              { phoneType = 6; }
		else if (!strcmp(var.value, "Siemens"))            { phoneType = 7; }
		else if (!strcmp(var.value, "Sharp"))              { phoneType = 8; }
		else if (!strcmp(var.value, "SKT"))                { phoneType = 9; }
		else if (!strcmp(var.value, "KDDI"))               { phoneType = 10; }
	}

	var.key = "freej2me_backlightcolor";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "Disabled"))    { backlightColor = 0; }
		else if (!strcmp(var.value, "Green"))  { backlightColor = 1; }
		else if (!strcmp(var.value, "Cyan"))   { backlightColor = 2; }
		else if (!strcmp(var.value, "Orange")) { backlightColor = 3; }
		else if (!strcmp(var.value, "Violet")) { backlightColor = 4; }
		else if (!strcmp(var.value, "Red"))    { backlightColor = 5; }
	}


	var.key = "freej2me_fps";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "Auto"))    { gameFPS = 0;  }
		else if (!strcmp(var.value, "60")) { gameFPS = 60; }
		else if (!strcmp(var.value, "55")) { gameFPS = 55; }
		else if (!strcmp(var.value, "50")) { gameFPS = 50; }
		else if (!strcmp(var.value, "45")) { gameFPS = 45; }
		else if (!strcmp(var.value, "40")) { gameFPS = 40; }
		else if (!strcmp(var.value, "35")) { gameFPS = 35; }
		else if (!strcmp(var.value, "30")) { gameFPS = 30; }
		else if (!strcmp(var.value, "25")) { gameFPS = 25; }
		else if (!strcmp(var.value, "20")) { gameFPS = 20; }
		else if (!strcmp(var.value, "15")) { gameFPS = 15; }
		else if (!strcmp(var.value, "10")) { gameFPS = 10; }
	}


	var.key = "freej2me_sound";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "off"))     { soundEnabled = 0; }
		else if (!strcmp(var.value, "on")) { soundEnabled = 1; }
	}


	var.key = "freej2me_midifont";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "off"))     { customMidi = 0; }
		else if (!strcmp(var.value, "on")) { customMidi = 1; }
	}

	var.key = "freej2me_textfont";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "off"))     { customFont = 0; }
		else if (!strcmp(var.value, "on")) { customFont = 1; }
	}

	var.key = "freej2me_fontoffset";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "-4"))      { fontOffset = -4; }
		else if (!strcmp(var.value, "-3")) { fontOffset = -3; }
		else if (!strcmp(var.value, "-2")) { fontOffset = -2; }
		else if (!strcmp(var.value, "-1")) { fontOffset = -1; }
		else if (!strcmp(var.value, "0")) { fontOffset = 0; }
		else if (!strcmp(var.value, "1")) { fontOffset = 1; }
		else if (!strcmp(var.value, "2")) { fontOffset = 2; }
		else if (!strcmp(var.value, "3")) { fontOffset = 3; }
		else if (!strcmp(var.value, "4")) { fontOffset = 4; }
	}

	var.key = "freej2me_analogasentirekeypad";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "off"))     { useAnalogAsEntireKeypad = false; }
		else if (!strcmp(var.value, "on")) { useAnalogAsEntireKeypad = true;  }
	}

	var.key = "freej2me_logginglevel";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "0"))      { loggingLevel = 0; }
		else if (!strcmp(var.value, "1")) { loggingLevel = 1; }
		else if (!strcmp(var.value, "2")) { loggingLevel = 2; }
		else if (!strcmp(var.value, "3")) { loggingLevel = 3; }
		else if (!strcmp(var.value, "4")) { loggingLevel = 4; }
	}

	var.key = "freej2me_dumpaudiostreams";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "off"))     { dumpAudioStreams = 0; }
		else if (!strcmp(var.value, "on")) { dumpAudioStreams = 1; }
	}

	var.key = "freej2me_dumpgraphicsdata";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "off"))     { dumpGraphicsData = 0; }
		else if (!strcmp(var.value, "on")) { dumpGraphicsData = 1; }
	}

	var.key = "freej2me_deletetempkjxfiles";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "off"))     { deleteTemporaryKJXFiles = 0; }
		else if (!strcmp(var.value, "on")) { deleteTemporaryKJXFiles = 1; }
	}


	var.key = "freej2me_pointertype";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "Mouse"))
		{
			uses_mouse = true;
			uses_pointer = false;
		}
		else if (!strcmp(var.value, "Touch"))
		{
			uses_mouse = false;
			uses_pointer = true;
		}
		else if (!strcmp(var.value, "None"))
		{
			uses_mouse = false;
			uses_pointer = false;
		}
	}


	var.key = "freej2me_pointerxspeed";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		pointerXSpeed = atoi(var.value);
	}


	var.key = "freej2me_pointeryspeed";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		pointerYSpeed = atoi(var.value);
	}


	var.key = "freej2me_pointerinnercolor";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "White"))       { pointerInnerColor = 0xFFFFFF; }
		else if (!strcmp(var.value, "Red"))    { pointerInnerColor = 0xFF0000; }
		else if (!strcmp(var.value, "Green"))  { pointerInnerColor = 0x00FF00; }
		else if (!strcmp(var.value, "Blue"))   { pointerInnerColor = 0x0000FF; }
		else if (!strcmp(var.value, "Yellow")) { pointerInnerColor = 0xFFFF00; }
		else if (!strcmp(var.value, "Pink"))   { pointerInnerColor = 0xFF00FF; }
		else if (!strcmp(var.value, "Cyan"))   { pointerInnerColor = 0x00FFFF; }
		else if (!strcmp(var.value, "Black"))  { pointerInnerColor = 0x000000; }
	}


	var.key = "freej2me_pointeroutercolor";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "White"))       { pointerOutlineColor = 0xFFFFFF; }
		else if (!strcmp(var.value, "Red"))    { pointerOutlineColor = 0xFF0000; }
		else if (!strcmp(var.value, "Green"))  { pointerOutlineColor = 0x00FF00; }
		else if (!strcmp(var.value, "Blue"))   { pointerOutlineColor = 0x0000FF; }
		else if (!strcmp(var.value, "Yellow")) { pointerOutlineColor = 0xFFFF00; }
		else if (!strcmp(var.value, "Pink"))   { pointerOutlineColor = 0xFF00FF; }
		else if (!strcmp(var.value, "Cyan"))   { pointerOutlineColor = 0x00FFFF; }
		else if (!strcmp(var.value, "Black"))  { pointerOutlineColor = 0x000000; }
	}


	var.key = "freej2me_pointerclickcolor";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "White"))       { pointerClickedColor = 0xFFFFFF; }
		else if (!strcmp(var.value, "Red"))    { pointerClickedColor = 0xFF0000; }
		else if (!strcmp(var.value, "Green"))  { pointerClickedColor = 0x00FF00; }
		else if (!strcmp(var.value, "Blue"))   { pointerClickedColor = 0x0000FF; }
		else if (!strcmp(var.value, "Yellow")) { pointerClickedColor = 0xFFFF00; }
		else if (!strcmp(var.value, "Pink"))   { pointerClickedColor = 0xFF00FF; }
		else if (!strcmp(var.value, "Cyan"))   { pointerClickedColor = 0x00FFFF; }
		else if (!strcmp(var.value, "Black"))  { pointerClickedColor = 0x000000; }
	}

	var.key = "freej2me_spdhacknoalpha";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "off"))       { spdHackNoAlpha = 0; }
		else if (!strcmp(var.value, "on"))   { spdHackNoAlpha = 1; }
	}

	var.key = "freej2me_spdhackm3ghalfres";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "off"))       { spdHackM3GHalfRes = 0; }
		else if (!strcmp(var.value, "on"))   { spdHackM3GHalfRes = 1; }
	}

	var.key = "freej2me_spdhackmcv3halfres";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "off"))       { spdHackMCV3HalfRes = 0; }
		else if (!strcmp(var.value, "on"))   { spdHackMCV3HalfRes = 1; }
	}

	var.key = "freej2me_spdhackmcv3nolight";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "off"))       { spdHackMCV3NoLight = 0; }
		else if (!strcmp(var.value, "on"))   { spdHackMCV3NoLight = 1; }
	}

	var.key = "freej2me_spdhackfpsunlock";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "0"))        { spdFrameRateUnlock = 0; }
		else if (!strcmp(var.value, "1"))   { spdFrameRateUnlock = 1; }
		else if (!strcmp(var.value, "2"))   { spdFrameRateUnlock = 2; }
		else if (!strcmp(var.value, "3"))   { spdFrameRateUnlock = 3; }
	}

	var.key = "freej2me_compatfantasyzonefix";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "off"))       { compatFantasyZoneFix = 0; }
		else if (!strcmp(var.value, "on"))   { compatFantasyZoneFix = 1; }
	}

	var.key = "freej2me_compattranstooriginongfxreset";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "off"))       { compatTransToOriginOnGFXReset = 0; }
		else if (!strcmp(var.value, "on"))   { compatTransToOriginOnGFXReset = 1; }
	}

	var.key = "freej2me_compatimmediaterepaintcalls";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "off"))       { compatImmediateRepaintCalls = 0; }
		else if (!strcmp(var.value, "on"))   { compatImmediateRepaintCalls = 1; }
	}

	var.key = "freej2me_compatoverrideplatcheck";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "off"))       { compatOverridePlatCheck = 0; }
		else if (!strcmp(var.value, "on"))   { compatOverridePlatCheck = 1; }
	}

	var.key = "freej2me_compatsiemensfriendlydraw";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "off"))       { compatSiemensFriendlyDraw = 0; }
		else if (!strcmp(var.value, "on"))   { compatSiemensFriendlyDraw = 1; }
	}

	var.key = "freej2me_compatignorevolumechanges";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "off"))       { compatIgnoreVolumeChanges = 0; }
		else if (!strcmp(var.value, "on"))   { compatIgnoreVolumeChanges = 1; }
	}

	var.key = "freej2me_compatmcv3horfovfix";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "off"))       { compatMCV3HorFovFix = 0; }
		else if (!strcmp(var.value, "on"))   { compatMCV3HorFovFix = 1; }
	}

	var.key = "freej2me_m3grenderuntextured";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "off"))       { m3gUntextured = 0; }
		else if (!strcmp(var.value, "on"))   { m3gUntextured = 1; }
	}

	var.key = "freej2me_m3grenderwireframe";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "off"))       { m3gWireframe = 0; }
		else if (!strcmp(var.value, "on"))   { m3gWireframe = 1; }
	}

	var.key = "freej2me_mcv3showheap";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "off"))       { mcv3Heap = 0; }
		else if (!strcmp(var.value, "on"))   { mcv3Heap = 1; }
	}

	var.key = "freej2me_mcv3showtimestats";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		if (!strcmp(var.value, "off"))       { mcv3TimeStats = 0; }
		else if (!strcmp(var.value, "on"))   { mcv3TimeStats = 1; }
	}

	var.key = "freej2me_dojaversion";
	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
	{
		dojaVersion = strtoul(var.value, NULL, 0);
	}


	/* Prepare a string to pass those core options to the Java app */
	options_update = malloc(sizeof(char) * PIPE_MAX_LEN);

	snprintf(options_update, PIPE_MAX_LEN, "FJ2ME_LR_OPTS:|%lux%lu|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d", screenRes[0], screenRes[1], rotateScreen, 
		phoneType, gameFPS, soundEnabled, customMidi, dumpAudioStreams, loggingLevel, spdHackNoAlpha, backlightColor, compatFantasyZoneFix, 
		compatTransToOriginOnGFXReset, customFont, fontOffset, dumpGraphicsData, deleteTemporaryKJXFiles, m3gUntextured, m3gWireframe, spdFrameRateUnlock, compatImmediateRepaintCalls,
		compatOverridePlatCheck, compatSiemensFriendlyDraw, spdHackM3GHalfRes, dojaVersion, compatIgnoreVolumeChanges, spdHackMCV3HalfRes, spdHackMCV3NoLight, compatMCV3HorFovFix, mcv3Heap, mcv3TimeStats);
	optstrlen = strlen(options_update);

	/* 0xD = 13, which is the special case where the java app will receive the updated configs */
	unsigned char optupdateevent[5] = { 0xD, (optstrlen>>24)&0xFF, (optstrlen>>16)&0xFF, (optstrlen>>8)&0xFF, optstrlen&0xFF };

	/* Sends the event to set Java in core options read mode, then send the string containing those options */
	if(booted) /* Checks if the java app booted first, or else it'll fail to write to pipes as they don't exist yet. */
	{
		write_to_pipe(pWrite[1], optupdateevent, 5);
		write_to_pipe(pWrite[1], options_update, optstrlen);
		log_fn(RETRO_LOG_INFO, "Sent updated options to the Java app.\n");
	}

	free(options_update);
}

static void Keyboard(bool down, unsigned keycode, uint32_t character, uint16_t key_modifiers)
{
	unsigned char event[5] = {down, (keycode>>24)&0xFF, (keycode>>16)&0xFF, (keycode>>8)&0xFF, keycode&0xFF };

	write_to_pipe(pWrite[1], event, 5);
}

void retro_init(void)
{
	/* init buffers, structs */
	memset(frame, 0, frameSize);
	memset(frameBuffer, 0, frameBufferSize);

	/* Check variables and set parameters */
	check_variables(true);
	char resArg[2][4], rotateArg[2], phoneArg[3], fpsArg[3], soundArg[2], midiArg[2], dumpAudioArg[2], logLevelArg[2], spdHackNoAlphaArg[2], backlightArg[2];
	char compatFantasyZoneFixArg[2], compatTransToOriginOnGFXResetArg[2], fontArg[2], offsetArg[3], dumpGFXArg[2], tempKJXArg[2], m3gUntexArg[2], m3gWireArg[2];
	char fpsunlockHack[2], compatImmediateRepaintArg[2], compatOverridePlatCheckArg[2], compatSiemensFriendlyDrawArg[2], spdHackM3GHalfResArg[2], dojaVersionArg[4];
	char compatIgnoreVolumeChangesArg[2], spdHackMCV3HalfResArg[2], spdHackMCV3NoLightArg[2], compatMCV3HorFovFixArg[2], mcv3HeapArg[2], mcv3TimeStatsArg[2];

	sprintf(resArg[0], "%lu", screenRes[0]);
	sprintf(resArg[1], "%lu", screenRes[1]);
	sprintf(rotateArg, "%d", rotateScreen);
	sprintf(phoneArg, "%d", phoneType);
	sprintf(fpsArg, "%d", gameFPS);
	sprintf(soundArg, "%d", soundEnabled);
	sprintf(midiArg, "%d", customMidi);
	sprintf(dumpAudioArg, "%d", dumpAudioStreams);
	sprintf(logLevelArg, "%d", loggingLevel);
	sprintf(spdHackNoAlphaArg, "%d", spdHackNoAlpha);
	sprintf(backlightArg, "%d", backlightColor);
	sprintf(compatFantasyZoneFixArg, "%d", compatFantasyZoneFix);
	sprintf(compatTransToOriginOnGFXResetArg, "%d", compatTransToOriginOnGFXReset);
	sprintf(fontArg, "%d", customFont);
	sprintf(offsetArg, "%d", fontOffset);
	sprintf(dumpGFXArg, "%d", dumpGraphicsData);
	sprintf(tempKJXArg, "%d", deleteTemporaryKJXFiles);
	sprintf(m3gUntexArg, "%d", m3gUntextured);
	sprintf(m3gWireArg, "%d", m3gWireframe);
	sprintf(fpsunlockHack, "%d", spdFrameRateUnlock);
	sprintf(compatImmediateRepaintArg, "%d", compatImmediateRepaintCalls);
	sprintf(compatOverridePlatCheckArg, "%d", compatOverridePlatCheck);
	sprintf(compatSiemensFriendlyDrawArg, "%d", compatSiemensFriendlyDraw);
	sprintf(spdHackM3GHalfResArg, "%d", spdHackM3GHalfRes);
	sprintf(dojaVersionArg, "%d", dojaVersion);
	sprintf(compatIgnoreVolumeChangesArg, "%d", compatIgnoreVolumeChanges);
	sprintf(spdHackMCV3HalfResArg, "%d", spdHackMCV3HalfRes);
	sprintf(spdHackMCV3NoLightArg, "%d", spdHackMCV3NoLight);
	sprintf(compatMCV3HorFovFixArg, "%d", compatMCV3HorFovFix);
	sprintf(mcv3HeapArg, "%d", mcv3Heap);
	sprintf(mcv3TimeStatsArg, "%d", mcv3TimeStats);

	/* We need to clean up any argument memory from the previous launch arguments in order to load up updated ones */
	if (restarting) { log_fn(RETRO_LOG_INFO, "Restarting FreeJ2ME-Plus.\n"); }
	else // System path is not meant to change on restarts
	{
		log_fn(RETRO_LOG_INFO, "Setting up FreeJ2ME-Plus' System Path.\n");
		Environ(RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY, &systemPath);
	}

	/* Check if freej2me's app actually exists and notify the user if it doesn't */
	char *freej2mePath = malloc(sizeof(char) * PIPE_MAX_LEN);
    snprintf(freej2mePath, sizeof(char) * PIPE_MAX_LEN, "%s/%s", systemPath, freej2meapp);

    if (!freej2me_present(freej2mePath)) 
	{
		free(freej2mePath);
		Environ(RETRO_ENVIRONMENT_SET_MESSAGE_EXT, (void*)&messages[SYSTEM_NOT_FOUND_MSG]);
        log_fn(RETRO_LOG_ERROR, "Error: %s does not exist in the system dir.\n", freej2meapp);
		return;
    }

	free(freej2mePath);

	/* Allocate memory for launch arguments */
	params = (char**)malloc(sizeof(char*) * NUM_ARGUMENTS);
#ifdef __linux__
	params[0] = strdup("java");
#elif _WIN32
	params[0] = strdup("javaw");
#endif
	params[1] = strdup("-jar");
	params[2] = strdup(supported_encodings[characterEncoding]);
	params[3] = strdup(freej2meapp);
	params[4] = strdup(resArg[0]);
	params[5] = strdup(resArg[1]);
	params[6] = strdup(rotateArg);
	params[7] = strdup(phoneArg);
	params[8] = strdup(fpsArg);
	params[9] = strdup(soundArg);
	params[10] = strdup(midiArg);
	params[11] = strdup(dumpAudioArg);
	params[12] = strdup(logLevelArg);
	params[13] = strdup(spdHackNoAlphaArg);
	params[14] = strdup(backlightArg);
	params[15] = strdup(compatFantasyZoneFixArg);
	params[16] = strdup(compatTransToOriginOnGFXResetArg);
	params[17] = strdup(fontArg);
	params[18] = strdup(offsetArg);
	params[19] = strdup(dumpGFXArg);
	params[20] = strdup(tempKJXArg);
	params[21] = strdup(m3gUntexArg);
	params[22] = strdup(m3gWireArg);
	params[23] = strdup(fpsunlockHack);
	params[24] = strdup(compatImmediateRepaintArg);
	params[25] = strdup(compatOverridePlatCheckArg);
	params[26] = strdup(compatSiemensFriendlyDrawArg);
	params[27] = strdup(spdHackM3GHalfResArg);
	params[28] = strdup(dojaVersionArg);
	params[29] = strdup(compatIgnoreVolumeChangesArg);
	params[30] = strdup(spdHackMCV3HalfResArg);
	params[31] = strdup(spdHackMCV3NoLightArg);
	params[32] = strdup(compatMCV3HorFovFixArg);
	params[33] = strdup(mcv3HeapArg);
	params[34] = strdup(mcv3TimeStatsArg);
	params[35] = NULL; // Null-terminate the array

	log_fn(RETRO_LOG_INFO, "Preparing to open FreeJ2ME-Plus' Java app.\n");

	booted = javaOpen(params[0], params);

	// Parameters are no longer needed now
	if (params)
	{
		for (int i = 0; params[i] != NULL; i++) { free(params[i]); }
		free(params);
	}

	if(!booted) 
	{ 
		Environ(RETRO_ENVIRONMENT_SET_MESSAGE_EXT, (void*)&messages[COULD_NOT_START_MSG]); 
		return; 
	}

	/* Setup keyboard input */
	struct retro_keyboard_callback kb = { Keyboard };
	Environ(RETRO_ENVIRONMENT_SET_KEYBOARD_CALLBACK, &kb);

	/* Check if joypad supports rumble */
	if (Environ(RETRO_ENVIRONMENT_GET_RUMBLE_INTERFACE, &rumble)) { log_fn(RETRO_LOG_INFO, "Rumble environment supported.\n"); }
	else { log_fn(RETRO_LOG_INFO, "Rumble environment not supported.\n"); }

	log_fn(RETRO_LOG_INFO, "All preparations done and java app is ready. Keyboard callback set.\n");
	Environ(RETRO_ENVIRONMENT_SET_MESSAGE_EXT, (void*)&messages[CORE_HAS_LOADED_MSG]);
}

bool retro_load_game(const struct retro_game_info *info)
{
	int len = 0;

	/* Game info is passed to a global variable to enable restarts */
	gameinfo = *info;
	/* Send savepath to java */
	char *savedir;
	Environ(RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY, &savedir);
	if (savedir[0] == '\0') 
	{
		Environ(RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY, &savedir);
	}

	char savepath[PATH_MAX_LENGTH];
	snprintf(savepath, sizeof(savepath), "%s%sfreej2me%s", savedir, slash, slash);
	len = strlen(savepath);

	unsigned char saveevent[5] = { 0xB, (len>>24)&0xFF, (len>>16)&0xFF, (len>>8)&0xFF, len&0xFF };
	write_to_pipe(pWrite[1], saveevent, 5);
	write_to_pipe(pWrite[1], savepath, len);

	log_fn(RETRO_LOG_INFO, "Savepath: %s.\n", savepath);

	/* Tell java app to load and run game */
	char romPath[PATH_MAX_LENGTH];

#ifdef __linux__
	realpath(info->path, romPath);
#elif _WIN32
	_fullpath(romPath, info->path, PATH_MAX_LENGTH);
#endif

	len = strlen(romPath);
	log_fn(RETRO_LOG_INFO, "Loading freej2me app from %s\n", romPath);

	unsigned char loadevent[5] = { 0xA, (len>>24)&0xFF, (len>>16)&0xFF, (len>>8)&0xFF, len&0xFF };
	write_to_pipe(pWrite[1], loadevent, 5);
	write_to_pipe(pWrite[1], (unsigned char*) romPath, len);

	log_fn(RETRO_LOG_INFO, "Sent app file and save paths to Java app.\n");

	return true;
}

void retro_unload_game(void) { /* FreeJ2ME closes the game by itself */ }

void retro_run(void)
{
	int i = 0;
	int j = 0;
	int t = 0; /* temp */
	int w = 0; /* sent frame width */
	int h = 0; /* sent frame height */
	int r = 0; /* rotation flag */
	int stat = 0;
	int status = 0;
	bool mouseChange = false;
	bool updated_vars = false; /* Used to check if the core's variables were updated */

	// These are only used if useAnalogAsEntireKeypad is enabled.
	bool num1pressed = false, num3pressed = false, num7pressed = false, num9pressed = false;

	if (Environ(RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE, &updated_vars) && updated_vars) { check_variables(false); }

	if(isRunning())
	{
		
		/* request frame */
		if(!frameRequested)
		{
			check_fast_forwarding();
			javaRequestFrame[3] = 0; // Indicate a new frame request
			write_to_pipe(pWrite[1], javaRequestFrame, 5);
			frameRequested = true;
		}

		/* handle joypad */
		for(i=0; i<PHONE_KEYS; i++) { joypre[i] = joypad[i]; }

		InputPoll();

		/* 
		 *                            0    1    2     3     4  5  6   7       8          9     10 11 12 13 14 15 16 17 18,  19
		 * Input array in libretro: [Up, Down, Left, Right, 9, 7, 0, Fire, RightSoft, LeftSoft, 1, 3. *. #, 2, 4, 6, 8, 5, CLR] 
		 */

		joypad[0] = InputState(0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_UP);
		joypad[1] = InputState(0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_DOWN);
		joypad[2] = InputState(0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_LEFT);
		joypad[3] = InputState(0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_RIGHT);

		joypad[6] = InputState(0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_X);
		joypad[7] = InputState(0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_Y);

		joypad[8] = InputState(0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_START);
		joypad[9] = InputState(0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_SELECT);

		if(useAnalogAsEntireKeypad) 
		{
			// These are more sensitive (lower threshold) in order to minimize cases where the 2,4,6,8 inputs are registered before these. 
			// Num 8 & Num 6
			num9pressed = ((InputState(0, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_LEFT, RETRO_DEVICE_ID_ANALOG_Y ) / 32767.0f) > analogDeadzone/2) 
							&& ((InputState(0, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_LEFT, RETRO_DEVICE_ID_ANALOG_X ) / 32767.0f) > analogDeadzone/2);
			// Num 8 & Num 4
			num7pressed = ((InputState(0, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_LEFT, RETRO_DEVICE_ID_ANALOG_Y ) / 32767.0f) > analogDeadzone/2) 
							&& ((InputState(0, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_LEFT, RETRO_DEVICE_ID_ANALOG_X ) / 32767.0f) < -analogDeadzone/2);
			// Num 2 & Num 4
			num1pressed = ((InputState(0, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_LEFT, RETRO_DEVICE_ID_ANALOG_Y ) / 32767.0f) < -analogDeadzone/2)
							&& ((InputState(0, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_LEFT, RETRO_DEVICE_ID_ANALOG_X ) / 32767.0f) < -analogDeadzone/2);
			// Num 2 & Num 6
			num3pressed = ((InputState(0, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_LEFT, RETRO_DEVICE_ID_ANALOG_Y ) / 32767.0f) < -analogDeadzone/2)
							&& ((InputState(0, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_LEFT, RETRO_DEVICE_ID_ANALOG_X ) / 32767.0f) > analogDeadzone/2);

			joypad[4] =  (int) num9pressed;  // num 9
			joypad[5] =  (int) num7pressed;  // num 7
			joypad[10] = (int) num1pressed; // num 1
			joypad[11] = (int) num3pressed; // num 3
		}
		else
		{
			joypad[4] = InputState(0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_A);  // num 9
			joypad[5] = InputState(0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_B);  // num 7
			joypad[10] = InputState(0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_L); // num 1
			joypad[11] = InputState(0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_R); // num 3
		}
		

		joypad[12] = InputState(0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_L2);
		joypad[13] = InputState(0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_R2);

		if(useAnalogAsEntireKeypad) 
		{
			joypad[14] = (int) ((InputState(0, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_LEFT, RETRO_DEVICE_ID_ANALOG_Y ) / 32767.0f) < -analogDeadzone) && !num1pressed && !num3pressed; // Num 2
			joypad[15] = (int) ((InputState(0, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_LEFT, RETRO_DEVICE_ID_ANALOG_X ) / 32767.0f) < -analogDeadzone) && !num1pressed && !num7pressed;  // Num 4
			joypad[16] = (int) ((InputState(0, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_LEFT, RETRO_DEVICE_ID_ANALOG_X ) / 32767.0f) > analogDeadzone) && !num3pressed && !num9pressed;  // Num 6
			joypad[17] = (int) ((InputState(0, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_LEFT, RETRO_DEVICE_ID_ANALOG_Y ) / 32767.0f) > analogDeadzone) && !num9pressed && !num7pressed;  // Num 8
		}
		else 
		{
			joypad[14] = (int) ((InputState(0, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_LEFT, RETRO_DEVICE_ID_ANALOG_Y ) / 32767.0f) < -analogDeadzone); // Num 2
			joypad[15] = (int) ((InputState(0, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_LEFT, RETRO_DEVICE_ID_ANALOG_X ) / 32767.0f) < -analogDeadzone);  // Num 4
			joypad[16] = (int) ((InputState(0, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_LEFT, RETRO_DEVICE_ID_ANALOG_X ) / 32767.0f) > analogDeadzone);  // Num 6
			joypad[17] = (int) ((InputState(0, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_LEFT, RETRO_DEVICE_ID_ANALOG_Y ) / 32767.0f) > analogDeadzone);  // Num 8
		}

		joypad[18] = InputState(0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_L3); // Num 5
		joypad[19] = InputState(0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_R3); // CLR
		
		/* Right analog will control the pointer, freeing the left analog to mirror the D-Pad if needed. */
		int joyRx = InputState(0, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_RIGHT, RETRO_DEVICE_ID_ANALOG_X);
		int joyRy = InputState(0, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_RIGHT, RETRO_DEVICE_ID_ANALOG_Y);

		int mouseX = InputState(0, RETRO_DEVICE_MOUSE, 0, RETRO_DEVICE_ID_MOUSE_X);
		int mouseY = InputState(0, RETRO_DEVICE_MOUSE, 0, RETRO_DEVICE_ID_MOUSE_Y);
		int mouseL = InputState(0, RETRO_DEVICE_MOUSE, 0, RETRO_DEVICE_ID_MOUSE_LEFT);

		int touchX = InputState(0, RETRO_DEVICE_POINTER, 0, RETRO_DEVICE_ID_POINTER_X);
		int touchY = InputState(0, RETRO_DEVICE_POINTER, 0, RETRO_DEVICE_ID_POINTER_Y);
		int touchP = InputState(0, RETRO_DEVICE_POINTER, 0, RETRO_DEVICE_ID_POINTER_PRESSED);

		/* Process rumble events */
		if (rumbleTime > 0 && rumble.set_rumble_state)
		{
			rumble.set_rumble_state(0, RETRO_RUMBLE_STRONG, rumbleStrength);
			rumble.set_rumble_state(0, RETRO_RUMBLE_WEAK,  rumbleStrength);
			rumbleTime -= 1000 / DEFAULT_FPS;
		}
		else
		{
			rumble.set_rumble_state(0, RETRO_RUMBLE_STRONG, 0);
			rumble.set_rumble_state(0, RETRO_RUMBLE_WEAK, 0);
		}

		/* analog right - move joymouse. XSpeed and YSpeed are multipliers set through the frontend */
		joyRx /= 32768 / pointerXSpeed;
		joyRy /= 32768 / pointerYSpeed;
		if(joyRx != 0 || joyRy !=0)
		{
			joymouseAnalog = true;
			/* This means that the mouse pointer will be visible for 30 frames (half a second, since 60fps is a second) */
			joymouseTime = DEFAULT_FPS / 2;
			joymouseX += joyRx<<1;
			joymouseY += joyRy<<1;

			mouseChange = true;
		}

		if(joymouseX<0) { joymouseX=0; }
		if(joymouseY<0) { joymouseY=0; }
		if(joymouseX>frameWidth-17) { joymouseX=frameWidth-17; }
		if(joymouseY>frameHeight)   { joymouseY=frameHeight; }

		if(uses_mouse)
		{
			/* mouse - move joymouse */
			if(mouseX != 0 || mouseY !=0)
			{
				joymouseAnalog = false;
				joymouseTime = DEFAULT_FPS / 2;
				joymouseX += mouseX;
				joymouseY += mouseY;

				mouseChange = true;
			}
			/* mouse - drag */
			if(mouseL>0 && mouseChange)
			{
				joyevent[0] = 6;
				joyevent[1] = (joymouseX >> 8) & 0xFF;
				joyevent[2] = (joymouseX) & 0xFF;
				joyevent[3] = (joymouseY >> 8) & 0xFF;
				joyevent[4] = (joymouseY) & 0xFF;
				write_to_pipe(pWrite[1], joyevent, 5);
			}

			/* mouse - down/up */
			if(mouseLpre != mouseL)
			{
				if(mouseL == 1) { joymouseClickedTime = DEFAULT_FPS * 0.1; }
				joymouseTime = DEFAULT_FPS / 2;
				joyevent[0] = 4 + mouseL;
				joyevent[1] = (joymouseX >> 8) & 0xFF;
				joyevent[2] = (joymouseX) & 0xFF;
				joyevent[3] = (joymouseY >> 8) & 0xFF;
				joyevent[4] = (joymouseY) & 0xFF;
				write_to_pipe(pWrite[1], joyevent, 5);
			}
			mouseLpre = mouseL;
		}


		/* touch event */
		else if(uses_pointer)
		{
			if(touchP!=0)
			{
				touchX = (int)(((float)(touchX + 0x7FFF)) * ((float)frameWidth / (float)0xFFFE));
				touchY = (int)(((float)(touchY + 0x7FFF)) * ((float)frameHeight / (float)0xFFFE));
				joymouseAnalog = false;
				joyevent[0] = 5; /* touch down */
				joyevent[1] = (touchX >> 8) & 0xFF;
				joyevent[2] = (touchX) & 0xFF;
				joyevent[3] = (touchY >> 8) & 0xFF;
				joyevent[4] = (touchY) & 0xFF;
				write_to_pipe(pWrite[1], joyevent, 5);
				joyevent[0] = 4; /* touch up */
				write_to_pipe(pWrite[1], joyevent, 5);
			}
		}


		for(i=0; i<PHONE_KEYS; i++)
		{
			/* joypad - spot the difference, send corresponding keyup/keydown events */
			if(joypad[i]!=joypre[i])
			{
				if(i==18 && joymouseTime>0 && joymouseAnalog)
				{
					/* when mouse is visible, and using analog stick for mouse, L3 / [num 5] clicks */
					if(joypad[i] == 1) { joymouseClickedTime = DEFAULT_FPS * 0.1; }
					joymouseTime = DEFAULT_FPS / 2;
					joyevent[0] = 4+joypad[18];
					joyevent[1] = (joymouseX >> 8) & 0xFF;
					joyevent[2] = (joymouseX) & 0xFF;
					joyevent[3] = (joymouseY >> 8) & 0xFF;
					joyevent[4] = (joymouseY) & 0xFF;
					write_to_pipe(pWrite[1], joyevent, 5);
				}
				else
				{
					joyevent[0] = 2+joypad[i];
					joyevent[1] = 0;
					joyevent[2] = 0;
					joyevent[3] = 0;
					joyevent[4] = i;
					write_to_pipe(pWrite[1], joyevent, 5);
				}
			}

		}

		/*
		 * grab frame
		 * some jars are noisy
		 * wait for start of frame marker 0xFE
		 */
		i=0;
		while(t!=0xFE && isRunning())
		{
			i++;
			status = read_from_pipe(pRead[0], &t, 1);

			if(i>255 && t!=0xFE)
			{
				/* drop frame */
				framesDropped++;
				if(framesDropped>250)
				{
					Environ(RETRO_ENVIRONMENT_SET_MESSAGE_EXT, (void*)&messages[FRAMES_DROPPED_MSG]);
				}
				Video(frame, frameWidth, frameHeight, sizeof(unsigned int) * frameWidth);
				return;
			}
			if(status<0 && errno != EAGAIN)
			{
				Environ(RETRO_ENVIRONMENT_SET_MESSAGE_EXT, (void*)&messages[INVALID_STATUS_MSG]);
				fflush(stdout);
				Video(frame, frameWidth, frameHeight, sizeof(unsigned int) * frameWidth);
				return;
			}
			/*
			if(t!=0xFE)
			{
				if((t<128 && t>31)||t==10) { printf("%c", t); }
				else { printf("%u", t); }
			}
			 */
		}

		

		/* read frame header */
		frameRequested = false;
		framesDropped = 0;
		status = read_from_pipe(pRead[0], frameHeader, 15); // This is always frameHeader's length in Libretro.java -1 (we already read the previous status byte above into the "t" variable)

		if(status>0)
		{
			w = (frameHeader[0]<<8) | (frameHeader[1]);
			h = (frameHeader[2]<<8) | (frameHeader[3]);
			r = (frameHeader[4]);

			/* Read vibration event */
			int preRumbleTime = ( (frameHeader[5]<<24) | (frameHeader[6]<<16) | (frameHeader[7]<<8) | (frameHeader[8]));
			rumbleStrength = ( (frameHeader[9]<<24) | (frameHeader[10]<<16) | (frameHeader[11]<<8) | (frameHeader[12]));

			/* Read if a restart request was sent (normally used to change character encoding) */
			characterEncoding = frameHeader[14];

			// restart request received, honor it
			if(frameHeader[13] == 1) { resetRequested = true; }

			if(preRumbleTime > 0) 
			{ 
				log_fn(RETRO_LOG_INFO, "Received Vibration event of %d ms. Strength is 0x%04X\n", preRumbleTime, rumbleStrength); 
				rumbleTime = preRumbleTime;
			}

			if(r == 1 || r == 3)
			{
				t = w;
				w = h;
				h = t;
			}
			if(frameWidth!=w || frameHeight!=h)
			{
				frameWidth = w;
				frameHeight = h;
				frameSize = w * h;
				frameBufferSize = frameSize * 3;

				/* update geometry */
				Geometry.base_width = w;
				Geometry.base_height = h;
				Geometry.max_width = MAX_WIDTH;
				Geometry.max_height = MAX_HEIGHT;
				Geometry.aspect_ratio = ((float)w / (float)h);
				Environ(RETRO_ENVIRONMENT_SET_GEOMETRY, &Geometry);
			}
		}

		/* read frame */
		status = 0;
		do
		{
			stat = read_from_pipe(pRead[0], readBuffer, PIPE_READ_BUFFER_SIZE);
			if (stat<=0) break;
			for(i=0; i<stat; i++)
			{
				frameBuffer[status + i] = readBuffer[i];
			}
			status += stat;
		} while(status > 0 && status < frameBufferSize);

		if(status>0)
		{
			t = 0;
			if(r == 0)
			{
				/* copy frameBuffer to frame directly */
				for(i=0; i<frameSize; i++)
				{
					frame[i] = (frameBuffer[t]<<16) | (frameBuffer[t+1]<<8) | (frameBuffer[t+2]);
					t+=3;
				}
			}
			else if(r == 1) 
			{
				/* copy frameBuffer to frame rotated 90 degrees */
				for (j = 0; j < frameWidth; j++) 
				{
					for (i = 0; i < frameHeight; i++) 
					{
						frame[(i * frameWidth) + (frameWidth - 1 - j)] = (frameBuffer[t] << 16) | (frameBuffer[t + 1] << 8) | (frameBuffer[t + 2]);
						t += 3;
					}
				}
			}
			else if (r == 2) 
			{
				/* copy frameBuffer to frame rotated 180 degrees */
				for (i = 0; i < frameHeight; i++) 
				{
					for (j = 0; j < frameWidth; j++) 
					{
						frame[(frameHeight - 1 - i) * frameWidth + (frameWidth - 1 - j)] = (frameBuffer[t] << 16) | (frameBuffer[t + 1] << 8) | (frameBuffer[t + 2]);
						t += 3;
					}
				}
			}
			else if(r == 3)
			{
				/* copy frameBuffer to frame rotated 270 degrees */
				for(j=0; j<frameWidth; j++)
				{
					for(i=frameHeight-1; i>=0; i--)
					{
						frame[(i*frameWidth)+j] = (frameBuffer[t]<<16) | (frameBuffer[t+1]<<8) | (frameBuffer[t+2]);
						t+=3;
					}
				}
			}
		}

		/* draw pointer */
		/* check if the clicked pointer needs to be shown */
		if(joymouseClickedTime>0)
		{
			joymouseClickedTime--;

			for(i=0; i<24; i++)
			{
				for (j=0; j<17; j++)
				{
					// the -3 and -5 are so that the tip of the pointer aligns with the position that will be clicked
					t = ((joymouseY + i - 3)*frameWidth)+(joymouseX + j - 6);
					if(t>=0 && t<sizeof(frame))
					{
						switch (joymouseClickedImage[(i*17)+j])
						{
							case 1: frame[t] = pointerOutlineColor; break;
							case 2: frame[t] = pointerInnerColor; break;
							case 3: frame[t] = pointerClickedColor; break;
						}
					}
				}
			}
		}

		/* Otherwise, draw the standard pointer */
		else if(joymouseTime>0)
		{
			joymouseTime--;

			for(i=0; i<24; i++)
			{
				for (j=0; j<17; j++)
				{
					// the -3 and -5 are so that the tip of the pointer aligns with the position that will be clicked
					t = ((joymouseY + i - 3)*frameWidth)+(joymouseX + j - 6);
					if(t>=0 && t<sizeof(frame))
					{
						switch (joymouseImage[(i*17)+j])
						{
							case 1: frame[t] = pointerOutlineColor; break;
							case 2: frame[t] = pointerInnerColor; break;
						}
					}
				}
			}
		}
	}

	/* send frame to libretro irrespective of FreeJ2ME running (for error messages) */
	Video(frame, frameWidth, frameHeight, sizeof(unsigned int) * frameWidth);

	/* 
 	 * I couldn't find a way for the frontend to notify FreeJ2ME's process that it has paused,
 	 * so this is the alternative. What happens is that, for every frame, libretro will ask
 	 * FreeJ2ME's process to resume/continue at the start, and stop/pause at the end. When
 	 * libretro is running, this pause call below doesn't do much (and i couldn't notice any
 	 * additional overhead), but once the frontend pauses to bring up its menu, or at the user's
 	 * request through the pause button, this takes effect, since retro_run() runs for an entire
 	 * frame. This also means that frame advance is kinda supported, although not perfect.
 	 */
	if(resetRequested) { retro_reset(); }
	
	javaRequestFrame[3] = 1; // Indicate that frame was processed
	write_to_pipe(pWrite[1], javaRequestFrame, 5);
	
}

unsigned retro_get_region(void)
{
	return RETRO_REGION_NTSC;
}

void retro_get_system_info(struct retro_system_info *info)
{
	memset(info, 0, sizeof(*info));
	info->library_name = "FreeJ2ME-Plus";
	info->library_version = "1.52";
	info->valid_extensions = "jar|kjx";
	info->need_fullpath = true;
}

void retro_get_system_av_info(struct retro_system_av_info *info)
{
	memset(info, 0, sizeof(*info));
	info->geometry.base_width   = BASE_WIDTH;
	info->geometry.base_height  = BASE_HEIGHT;
	info->geometry.max_width    = MAX_WIDTH;
	info->geometry.max_height   = MAX_HEIGHT;
	info->geometry.aspect_ratio = ((float)BASE_WIDTH) / ((float)BASE_HEIGHT);

	info->timing.fps = DEFAULT_FPS;
	int pixelformat = RETRO_PIXEL_FORMAT_XRGB8888;
	Environ(RETRO_ENVIRONMENT_SET_PIXEL_FORMAT, &pixelformat);
}


void retro_deinit(void)
{
#ifdef __linux__
	kill(javaProcess, SIGKILL);
	wait(NULL);
#elif _WIN32
	HANDLE hProcessSnap;
	PROCESSENTRY32 pe32;
	CloseHandle(pRead[0]);
	CloseHandle(pRead[1]);
	CloseHandle(pWrite[0]);
	CloseHandle(pWrite[1]);	

	/* 
	* Since java on win32 has the "decency" to open a secondary process with another PID
	* that is what runs freej2me's main app, the only way i (with my very limited windows
	* knowledge) can think of to reliably close this is going nuclear: Terminate all javaw 
	* processes related to javaProcess.
	*/
	hProcessSnap = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
	if (hProcessSnap == INVALID_HANDLE_VALUE) { return; }

	pe32.dwSize = sizeof(PROCESSENTRY32);

	if (!Process32First(hProcessSnap, &pe32)) 
	{
		CloseHandle(hProcessSnap);
		return;
	}

	// Iterate through all processes.
	do 
	{
		if (pe32.th32ParentProcessID == javaProcess.dwProcessId) 
		{
			// Open the child process with TERMINATE permission.
			HANDLE hChildProcess = OpenProcess(PROCESS_TERMINATE, FALSE, pe32.th32ProcessID);
			if (hChildProcess) 
			{
				// Terminate the child process.
				TerminateProcess(hChildProcess, 0);
				CloseHandle(hChildProcess);
			}
		}
	} while (Process32Next(hProcessSnap, &pe32));

	// Then terminate the parent process (the one we have the pointer to).
	HANDLE hParentProcess = OpenProcess(PROCESS_TERMINATE, FALSE, javaProcess.dwProcessId);
	if (hParentProcess) 
	{
		TerminateProcess(hParentProcess, 0);
		CloseHandle(hParentProcess);
	}

	CloseHandle(hProcessSnap);
#endif
}

void retro_reset(void)
{
	resetRequested = false;
	restarting = true;
	booted = false;
	retro_deinit();
	retro_init();
	retro_load_game(&gameinfo);
}

/* Stubs */
unsigned int retro_api_version(void) { return RETRO_API_VERSION; }
void *retro_get_memory_data(unsigned id) { return NULL; }
size_t retro_get_memory_size(unsigned id){ return 0; }
size_t retro_serialize_size(void) { return 0; }
bool retro_serialize(void *data, size_t size) { return false; }
bool retro_unserialize(const void *data, size_t size) { return false; }
void retro_cheat_reset(void) {  }
void retro_cheat_set(unsigned index, bool enabled, const char *code) {  }
bool retro_load_game_special(unsigned game_type, const struct retro_game_info *info, size_t num_info) { return false; }
void retro_set_controller_port_device(unsigned port, unsigned device) {  }


/* Java Process */
bool javaOpen(char *cmd, char **params)
{
	if(!restarting)
	{
		log_fn(RETRO_LOG_INFO, "System Path: %s\n", systemPath);

		log_fn(RETRO_LOG_INFO, "Setting up java app's process and pipes...\n");
	}
	else { log_fn(RETRO_LOG_INFO, "Restarting FreeJ2ME.\n"); restarting = false; }
	
	log_fn(RETRO_LOG_INFO, "Opening: %s %s %s %s ...\n", *(params+0), *(params+1), *(params+2), *(params+3));

#ifdef __linux__
	int pid = 0;
	int fd_stdin  = 0;
	int fd_stdout = 1;

	/*
	 * parent <-- 0 --  pRead  <-- 1 --  child
	 * parent  -- 1 --> pWrite  -- 0 --> child
	 */

 	pipe(pRead); /* 0: pRead, 1: pWrite */
	pipe(pWrite);

	pid = fork();

	if(pid==0) /* child */
	{

		dup2(pWrite[0], fd_stdin);  /* read from parent pWrite */
		dup2(pRead[1], fd_stdout);  /* write to parent pRead */

		close(pWrite[1]);
		close(pRead[0]);
		
		chdir(systemPath);

		execvp(cmd, params);

		/* execvp failure! */
	}

	if(pid>0) /* parent */
	{
		close(pRead[1]);
		close(pWrite[0]);
	}

	if(pid<0) /* error */
	{
		Environ(RETRO_ENVIRONMENT_SET_MESSAGE_EXT, (void*)&messages[IMPROPER_CHILDPROC_MSG]);
	}

	javaProcess = pid;

#elif _WIN32
	SECURITY_ATTRIBUTES pipeSec;

	log_fn(RETRO_LOG_INFO, "Setting up java app's process and pipes...\n");

	ZeroMemory( &pipeSec, sizeof(pipeSec) );
	ZeroMemory( &startInfo, sizeof(startInfo) );
	ZeroMemory( &javaProcess, sizeof(javaProcess) );

	pipeSec.nLength = sizeof(pipeSec);
	pipeSec.bInheritHandle = TRUE; /* Needed for IPC between java app and this core */

	log_fn(RETRO_LOG_INFO, "Creating pipes...\n");

	/* read from parent pWrite */
	if(CreatePipe(&pWrite[0], &pWrite[1], &pipeSec, 0))
	{
		if(!DuplicateHandle(
			GetCurrentProcess(),
			pWrite[0],
			GetCurrentProcess(),
			NULL,
			0,
			FALSE,
			DUPLICATE_SAME_ACCESS))
		{
			log_fn(RETRO_LOG_INFO, "Failed to create pWrite pipe... \n");
			Environ(RETRO_ENVIRONMENT_SET_MESSAGE_EXT, (void*)&messages[IMPROPER_CHILDPROC_MSG]);
			CloseHandle(pWrite[0]);
			CloseHandle(pWrite[1]);
			return false;
		}
	}

	/* write to parent pRead */
	if(CreatePipe(&pRead[0], &pRead[1], &pipeSec, 0))
	{
		if(!DuplicateHandle(
			GetCurrentProcess(),
			pRead[1],
			GetCurrentProcess(),
			NULL,
			0,
			FALSE,
			DUPLICATE_SAME_ACCESS))
		{
			log_fn(RETRO_LOG_INFO, "Failed to create pRead pipe... \n");
			Environ(RETRO_ENVIRONMENT_SET_MESSAGE_EXT, (void*)&messages[IMPROPER_CHILDPROC_MSG]);
			CloseHandle(pRead[0]);
			CloseHandle(pRead[1]);
			return false;
		}
	}

	log_fn(RETRO_LOG_INFO, "Created pipes! \n");

	log_fn(RETRO_LOG_INFO, "Trying to create process... \n");
	
	/* Try starting the child process. Windows requires the commandline argument to be a single string. */
	char cmdWin[PATH_MAX_LENGTH];

	snprintf(cmdWin, PATH_MAX_LENGTH, "%s", params[0]); // First argument needs no space separator

	for (int i = 1; i < NUM_ARGUMENTS; i++) 
	{
		if (params[i] != NULL) 
		{
			// Append a space and then the parameter
			snprintf(cmdWin + strlen(cmdWin), PATH_MAX_LENGTH - strlen(cmdWin), " %s", params[i]);
		}
	}

	GetStartupInfo(&startInfo);
	startInfo.dwFlags = STARTF_USESTDHANDLES;
	startInfo.hStdInput = pWrite[0]; // Child's stdin
	startInfo.hStdOutput = pRead[1];  // Child's stdout
	startInfo.hStdError = GetStdHandle(STD_ERROR_HANDLE);

	if(!CreateProcess( NULL, /* Module name */
		cmdWin,              /* Command line args */
		NULL,                /* Process handle not inheritable */
		NULL,                /* Thread handle not inheritable */
		TRUE,                /* Set handle inheritance to TRUE */
		0,                   /* No creation flags */
		NULL,                /* Use parent's environment block */
		systemPath,          /* Use libretro's "system" dir as starting directory */
		&startInfo,          /* Pointer to STARTUPINFO structure */
		&javaProcess ))      /* Pointer to PROCESS_INFORMATION structure */
	{ /* If it fails, this block is executed */
		log_fn(RETRO_LOG_ERROR, "Couldn't create process, error: %lu\n", GetLastError() );
	}

	log_fn(RETRO_LOG_INFO, "Created process! PID=%d \n", javaProcess.dwProcessId);

	// Close handles in parent process
	CloseHandle(pWrite[0]); // Close unused write end for child
	CloseHandle(pRead[1]);  // Close unused read end for child

	CloseHandle(javaProcess.hThread); // Close the thread handle, not needed
#endif

	/* wait for java process to respond */
	int t = 0;
	int status = 0;

	while(status<1 && isRunning())
	{
		status = read_from_pipe(pRead[0], &t, 1);
		if(status<0 && errno != EAGAIN) { Environ(RETRO_ENVIRONMENT_SET_MESSAGE_EXT, (void*)&messages[INVALID_STATUS_MSG]); }
	}

	if(!isRunning()) { return false; }
	else 
	{
		log_fn(RETRO_LOG_INFO, "Core and Java app started! Initializing game data... \n");
		return true;
	}
}


bool isRunning()
{
	if(stoppedRunning) { return false; } // If the app is no longer running, only a core restart can solve it
#ifdef __linux__
	int status;
	if(waitpid(javaProcess, &status, WNOHANG) == 0) { return true; }

	log_fn(RETRO_LOG_INFO, "Java app is not running! Last known PID=%d \n", javaProcess);
#elif _WIN32
	/*
	 * On win32, receiving a WAIT_TIMEOUT signal before timing out means the process is
	 * still running and didn't face any issues that caused it to lock up or crash.
	 */
	if(WaitForSingleObject(javaProcess.hProcess, 0) == WAIT_TIMEOUT) { return true; }

	log_fn(RETRO_LOG_INFO, "Java app is not running! Last known PID=%d \n", javaProcess.dwProcessId);
#endif
	Environ(RETRO_ENVIRONMENT_SET_MESSAGE_EXT, (void*)&messages[CHILDPROC_CLOSED_MSG]);
	stoppedRunning = true;
	return false;
}
