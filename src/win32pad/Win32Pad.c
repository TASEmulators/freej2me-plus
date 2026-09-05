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

// THIS IS AN ACTUAL WIN32 DEFINE, I KNOW IT LOOKS LIKE A MEME
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <mmsystem.h>

#define abs(x) ((x) < 0 ? -(x) : (x))

// Manual XInput gamepad event definitions so we can avoid explicitly including
// xinput.h for better compatibility.
typedef struct
{
	WORD wButtons;
	BYTE bLeftTrigger;
	BYTE bRightTrigger;
	SHORT sThumbLX;
	SHORT sThumbLY;
	SHORT sThumbRX;
	SHORT sThumbRY;
	DWORD dwReserved;
} XINPUT_GAMEPAD_EX;

typedef struct
{
	DWORD dwPacketNumber;
	XINPUT_GAMEPAD_EX Gamepad;
} XINPUT_STATE_EX;

typedef DWORD (WINAPI *XInputGetState_t)(DWORD dwUserIndex, XINPUT_STATE_EX* pState);

// XInput static variables for tracking. It works differently from DirectInput,
// so if we don't track these statically, the result will be XInput mashing
// inputs instead of just holding.
static XInputGetState_t pfnXInputGetState = NULL;
static _Bool g_hasXInput = FALSE;
static SHORT lastLX = 0, lastLY = 0, lastRX = 0, lastRY = 0;
static BYTE lastLT = 0, lastRT = 0;
static DWORD lastXInputButtons = 0;

// Attempt to dynamically load XInput on Windows versions that support it. Tries
// from newest to oldest dlls.
static void try_init_xinput(void)
{
	HMODULE hXInput = LoadLibraryA("xinput1_4.dll");
	if (!hXInput) hXInput = LoadLibraryA("xinput1_3.dll");
	if (!hXInput) hXInput = LoadLibraryA("xinput9_1_0.dll");

	if (hXInput)
	{
		pfnXInputGetState = (XInputGetState_t)GetProcAddress(hXInput, "XInputGetState");
		if (pfnXInputGetState) { g_hasXInput = TRUE; }
	}
}



static void write_data(HANDLE hStdout, short val, unsigned char type, unsigned char num)
{
	// DirectInput uses 8-byte data packets for communication (so it's not
	// too different from how gamepads are handled on linux).
	unsigned char data[8];

	// Bytes 0-3 are dummy timestamps
	data[0] = 0;
	data[1] = 0;
	data[2] = 0;
	data[3] = 0;
	data[4] = (unsigned char)(val & 0xFF);
	data[5] = (unsigned char)((val >> 8) & 0xFF);
	data[6] = type;
	data[7] = num;

	// Bytes written, used for checking if all the data was sent below
	// and exit if not (as the pipe was broken).
	DWORD written = 0;

	// Exit immediately if stdout parent stream breaks.
	if (!WriteFile(hStdout, data, 8, &written, NULL) || written != 8)
		{ ExitProcess(0); }

	FlushFileBuffers(hStdout);
}

// argc/argv won't work as normal due to the CRT entry point being dropped...
// we need to read those from GetCommandLineA instead.
static int get_device_id(void)
{
	const char* cmd = GetCommandLineA();
	if (!cmd) { return 0; } // No argument? Use Device ID 0.

	// Fast-forward past the executable path
	_Bool in_quotes = FALSE;
	while (*cmd)
	{
		if (*cmd == '"') { in_quotes = !in_quotes; }
		else if (*cmd == ' ' && !in_quotes)
		{
			while (*cmd == ' ') cmd++;
			break;
		}
		cmd++;
	}

	int res = 0;
	while (*cmd >= '0' && *cmd <= '9')
	{
		res = res * 10 + (*cmd - '0');
		cmd++;
	}
	return res;
}

// Screw MinGW CRT init, we're not adding useless dependencies.
void __main(void) { }

__attribute__((force_align_arg_pointer))
int main(int argc, char* argv[])
{
	// We'll need the stdin handle to monitor its state in order to exit
	// gracefully.
	HANDLE hStdin = GetStdHandle(STD_INPUT_HANDLE);

	// stdOut is the communication pipe itself.
	HANDLE hStdout = GetStdHandle(STD_OUTPUT_HANDLE);

	// Check if stdin is a console stream (launched by the user), which is wrong
	if (GetFileType(hStdin) == FILE_TYPE_CHAR)
	{
		const char msg[] = "Win32Pad - DirectInput Gamepad Reader for FreeJ2ME-Plus.\r\n\n"
			"If you can read this, you're using it wrong. This tool is launched automatically by the jar app and depends on it.\r\n";
		DWORD written;
		WriteFile(hStdout, msg, sizeof(msg) - 1, &written, NULL);
		ExitProcess(1);
	}

	int deviceId = get_device_id();

	try_init_xinput();

	JOYINFOEX info = {0};
	info.dwSize = sizeof(JOYINFOEX);
	info.dwFlags = JOY_RETURNALL;
	XINPUT_STATE_EX xState = {0};

	DWORD lastButtons = 0;
	DWORD lastX = 32768;
	DWORD lastY = 32768;
	DWORD lastZ = 32768;
	DWORD lastR = 32768;
	DWORD lastPOV = 65535;

	while (1)
	{
		// first things first: We check if Java closed the stdin pipe stream due
		// to an app exit or a controller refresh command. If those happen, the
		// stdin handle will be broken so we can close this process right away.
		DWORD avail = 0;
		if (!PeekNamedPipe(hStdin, NULL, 0, NULL, &avail, NULL) && GetLastError() == ERROR_BROKEN_PIPE)
		{
			ExitProcess(0);
		}

		_Bool handledByXInput = FALSE;

		// Try using XInput First Device IDs only go from 0 to 3
		if (g_hasXInput && deviceId < 4)
		{
			if (pfnXInputGetState(deviceId, &xState) == 0)
			{
				handledByXInput = TRUE;

				// Buttons (except D-Pad, those are special cases since they
				// must match DirectInput's values on output)
				DWORD buttons = xState.Gamepad.wButtons;
                DWORD changed = buttons ^ lastXInputButtons;
				if (changed != 0)
				{
					for (int i = 4; i < 16; i++)
					{
						if (changed & (1 << i))
						{
							short state = (buttons & (1 << i)) ? 1 : 0;
							write_data(hStdout, state, 0x01, (unsigned char)i);
						}
					}
				}
				lastXInputButtons = buttons;

				// Left Analog Stick
				SHORT currentLX = xState.Gamepad.sThumbLX;
				if (abs(currentLX - lastLX) > 800)
				{
					write_data(hStdout, currentLX, 0x02, 0);
					lastLX = currentLX;
				}

				// Y is inverted compared to DInput
				SHORT currentLY = (SHORT)-xState.Gamepad.sThumbLY;
				if (abs(currentLY - lastLY) > 800)
				{
					write_data(hStdout, currentLY, 0x02, 1);
					lastLY = currentLY;
				}

				// Right Analog Stick
				SHORT currentRX = xState.Gamepad.sThumbRX;
				if (abs(currentRX - lastRX) > 800)
				{
					write_data(hStdout, currentRX, 0x02, 2);
					lastRX = currentRX;
				}

				SHORT currentRY = (SHORT)-xState.Gamepad.sThumbRY;
				if (abs(currentRY - lastRY) > 800)
				{
					write_data(hStdout, currentRY, 0x02, 3);
					lastRY = currentRY;
				}

				// Left Trigger. bLeftTrigger goes from 0 to 255, so we map it
				// to a short range to match DirectInput
				BYTE currentLT = xState.Gamepad.bLeftTrigger;
				if (abs((int)currentLT - (int)lastLT) > 10)
				{
				    short normLT = (short)(((int)currentLT * 32767) / 255);
				    write_data(hStdout, normLT, 0x02, 4);
				    lastLT = currentLT;
				}

				// Right Trigger, same idea as above.
				BYTE currentRT = xState.Gamepad.bRightTrigger;
				if (abs((int)currentRT - (int)lastRT) > 10)
				{
				    short normRT = (short)(((int)currentRT * 32767) / 255);
				    write_data(hStdout, normRT, 0x02, 5);
				    lastRT = currentRT;
				}

				// XInput has D-Pad as actual buttons, but since the first
				// implementation was based around DInput, we make this match
				// how DInput works.
				short hatX = 0, hatY = 0;
				if (buttons & 0x0001) { hatY = -32767; } // UP
				if (buttons & 0x0002) { hatY = 32767;  } // DOWN
				if (buttons & 0x0004) { hatX = -32767; } // LEFT
				if (buttons & 0x0008) { hatX = 32767;  }  // RIGHT

				static short lastHatX = 0, lastHatY = 0;
				if (hatX != lastHatX)
				{
					write_data(hStdout, hatX, 0x02, 16);
					lastHatX = hatX;
				}
				if (hatY != lastHatY)
				{
					write_data(hStdout, hatY, 0x02, 17);
					lastHatY = hatY;
				}
			}
		}

		// Fallback to DirectInput if XInput is not available.
		if (!handledByXInput && joyGetPosEx(deviceId, &info) == JOYERR_NOERROR)
		{
			// Check the pad's button states.
			DWORD changed = info.dwButtons ^ lastButtons;
			if (changed != 0)
			{
				for (int i = 0; i < 16; i++)
				{
					if (changed & (1 << i))
					{
						short state = (info.dwButtons & (1 << i)) ? 1 : 0;
						write_data(hStdout, state, 0x01, (unsigned char)i); // Type 0x01 = Button
					}
				}
				lastButtons = info.dwButtons;
			}

			// Check for the left stick's X/Y axis
			if (abs((int)info.dwXpos - (int)lastX) > 800)
			{
				short normX = (short)(info.dwXpos - 32768);
				write_data(hStdout, normX, 0x02, 0); // Type 0x02 = Axis 0
				lastX = info.dwXpos;
			}

			if (abs((int)info.dwYpos - (int)lastY) > 800)
			{
				short normY = (short)(info.dwYpos - 32768);
				write_data(hStdout, normY, 0x02, 1); // Type 0x02 = Axis 1
				lastY = info.dwYpos;
			}

			// Now the right stick's
			if (abs((int)info.dwZpos - (int)lastZ) > 800)
			{
				short normZ = (short)(info.dwZpos - 32768);
				write_data(hStdout, normZ, 0x02, 2); // Axis 2 (X)
				lastZ = info.dwZpos;
			}

			if (abs((int)info.dwRpos - (int)lastR) > 800)
			{
				short normR = (short)(info.dwRpos - 32768);
				write_data(hStdout, normR, 0x02, 3); // Axis 3 (Y)
				lastR = info.dwRpos;
			}

			// Now the D-Pad (POV Hat)
			if (info.dwPOV != lastPOV)
			{
				short hatX = 0, hatY = 0;

				if (info.dwPOV != 65535)
				{
					// DirectInput processes angles in hundredths of degrees
					// (0 is Up, 9000 is Right, 18000 is Down, 27000 is Left)
					//
					// NOTE: D-Pad diagonals need <= and >= to be used, as they
					// sit in the exact boundaries between one key and the other.
					if (info.dwPOV >= 31500 || info.dwPOV <= 4500)       { hatY = -32767; } // Up
					else if (info.dwPOV >= 13500 && info.dwPOV <= 22500) { hatY = 32767;  } // Down

					if (info.dwPOV >= 4500 && info.dwPOV <= 13500)       { hatX = 32767;  } // Right
					else if (info.dwPOV >= 22500 && info.dwPOV <= 31500) { hatX = -32767; } // Left
				}

				write_data(hStdout, hatX, 0x02, 16); // Axis 16 (Hat X)
				write_data(hStdout, hatY, 0x02, 17); // Axis 17 (Hat Y)
				lastPOV = info.dwPOV;
			}
		}

		// About 200Hz polling rate. Should be good enough for anything J2ME.
		Sleep(5);
	}

	return 0;
}
