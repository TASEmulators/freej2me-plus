/*
Anbu, an interface between FreeJ2ME emulator and SDL2
Authors:
	Anbu        Saket Dandawate (hex @ retropie)
	FreeJ2ME    D. Richardson (recompile @ retropie)
	
To compile : g++ -std=c++11 -lSDL2 -lpthread -o anbu anbu.cpp

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

// SDL interface definitions

#define BYTES 3

// Input Mapping section

#define KEYBOARD_COMMAND 0
#define JOYPAD_COMMAND 1
#define JOYPAD_AXIS_COMMAND 2

#define AXIS_DEADZONE 23000

#define UNMAPPED	17

#define KEY_NUM5	0
#define	KEY_NUM7	1
#define	KEY_NUM9	2
#define	KEY_POUND	3
#define	KEY_SOFT1	4
#define	KEY_NUM0	5
#define	KEY_SOFT2	6
#define KEY_GAMEA	7
#define KEY_GAMEB	8
#define	KEY_NUM1	9
#define	KEY_NUM3	10
#define	KEY_NUM2	11
#define	KEY_NUM8	12
#define	KEY_NUM4	13
#define	KEY_NUM6	14
#define KEY_STAR	15

#define AXIS_MAPPING_OFFSET 11
#define AXIS_MAPPING_NUM sizeof(defaultJoyAxis)/sizeof(int)

const char *keynames[] =
{
	"KEY_NUM5",
	"KEY_NUM7",
	"KEY_NUM9",
	"KEY_POUND",
	"KEY_SOFT1",
	"KEY_NUM0",
	"KEY_SOFT2",
	"KEY_GAMEA",
	"KEY_GAMEB",
	"KEY_NUM1",
	"KEY_NUM3",
	"KEY_NUM2",
	"KEY_NUM8",
	"KEY_NUM4",
	"KEY_NUM6",
	"KEY_NUM0",
	"KEY_STAR",
	"UNMAPPED"
};

// Both default mappings are in regards to the key defines above

// For the keyboard, the idea was to keep as much in the numpad as possible.
// Not the most comfortable setup, but works.
int defaultKeyboardKeys[] = 
{
	1073741917, // Num 5
	1073741913,	// Num 1
	1073741915, // Num 3
	122, // X
	13, // Enter
	99, // C
	8, // Backspace
	97, // A
	115, // S
	1073741919, // Num 7
	1073741921, // Num 9
	1073741906, // Arrow Up
	1073741905, // Arrow Down
	1073741904, // Arrow Left
	1073741903, // Arrow Right
	1073741922, // Num 0
	1073741909 // Num *
};

int defaultJoyAxis[] = 
{
	768, // Left Stick Up
	1280, // Left Stick Down
	0, // Left Stick Left
	512 // Left Stick Right
};

int defaultJoyKeys[] = 
{
	KEY_NUM5,
	KEY_NUM7,
	KEY_NUM9,
	KEY_POUND,
	KEY_SOFT1,
	KEY_NUM0,
	KEY_SOFT2,
	KEY_GAMEA,
	KEY_GAMEB,
	KEY_NUM1,
	KEY_NUM3,
	KEY_NUM2,
	KEY_NUM8,
	KEY_NUM4,
	KEY_NUM6,
	KEY_NUM0,
	KEY_STAR
};