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

#include <stdio.h>
#include <iostream>
#include <assert.h>
#include <map>
#include "anbu.hpp"

#ifdef _WIN32
#include <windows.h>
#include <string>
#else
#include <pthread.h>
#endif

#include <SDL2/SDL.h>

#define DEADZONE 23000
#define BYTES 3

using namespace std;

#ifdef _WIN32
DWORD t_capturing;
#else
pthread_t t_capturing;
bool capturing = true;

int angle = 0;
int sourceWidth = 0, sourceHeight = 0;
int displayWidth = 0, displayHeight = 0;
double windowScale = 1;
double overlayScale = 1;
int lastTime = 0;

SDL_Renderer *mRenderer;
SDL_Texture *mBackground;
SDL_Texture *mOverlay;
SDL_Texture *mTexture;
SDL_Window *mWindow;

// Mouse pointer coordinates
int mouseX = 0;
int mouseY = 0;
int correctedMouseX = 0;
int correctedMouseY = 0;
bool mousePressed = false;
bool mouseDragged = false;
unsigned int dragThreshold = 2; // threshold in pixels

// SDL2-specific settings
bool getScreenShot = false;
bool isFullscreen = false;
string interpol = "nearest";

std::map<SDL_JoystickID, SDL_Joystick*> mJoysticks;
std::map<SDL_JoystickID, int*> mPrevAxisValues;

//SDL2 keyboard and joystick mapping abstractions.
std::map<int, int> mJoyMappings;
std::map<int, int> mKeyboardMappings;
bool remappingKeys = false;

string getHelp()
{
	string help = "Anbu 0.8.5, an interface between FreeJ2ME emulator and SDL2.\n\
Usage: anbu width height [OPTION]...\n\
\n\
Options:\n\
  -i type               Interpolation to use {'nearest', 'linear', 'best'}\n\
  -r rotation           Rotate incoming frame if needed [0 = no rotation, 1= 270 degrees]\n\
  -s scale              Additional scaling [2, 3, etc] of FreeJ2ME's window\n\
  -c R G B              Background color in RGB format [0-255] for each channel\n\
  -f fullscreen         Sets if Anbu should start in fullscreen [1] or not [0]\n\
\n\
Authors:\n\
  Anbu:                 Saket Dandawate / hex\n\
  FreeJ2ME:             D. Richardson / recompile\n\
\n\
Licensed under GPL3. Free for Non-Commercial usage.";
	return help;
}

/**************************************************** Input / Output Handlers */
int findJoypadFunction(const int key) {
    for (const auto& keyFunction : mJoyMappings) {
        if (keyFunction.second == key) {
            return keyFunction.first;
        }
    }
    return UNMAPPED;
}

int findKeyboardFunction(const int key) {
    for (const auto& keyFunction : mKeyboardMappings) {
        if (keyFunction.second == key) {
            return keyFunction.first;
        }
    }
    return UNMAPPED;
}

void addJoystick(int id)
{
	assert(id >= 0 && id < SDL_NumJoysticks());

	// open joystick & add to our list
	SDL_Joystick* joy = SDL_JoystickOpen(id);
	assert(joy);

	// add it to our list so we can close it again later
	SDL_JoystickID joyId = SDL_JoystickInstanceID(joy);
	mJoysticks[joyId] = joy;

	// set up the prevAxisValues
	int numAxes = SDL_JoystickNumAxes(joy);
	mPrevAxisValues[joyId] = new int[numAxes];
	std::fill(mPrevAxisValues[joyId], mPrevAxisValues[joyId] + numAxes, 0);
}

void removeJoystick(SDL_JoystickID joyId)
{
	assert(joyId != -1);
	// delete old prevAxisValues
	auto axisIt = mPrevAxisValues.find(joyId);
	delete[] axisIt->second;
	mPrevAxisValues.erase(axisIt);

	// close the joystick
	auto joyIt = mJoysticks.find(joyId);
	if(joyIt != mJoysticks.end())
	{
		SDL_JoystickClose(joyIt->second);
		mJoysticks.erase(joyIt);
	}
}

void initKeys(bool fromConfig) 
{
	// There are only 17 emulated functions right now
	for(int key = 0; key < 17; key++) 
	{
		if(fromConfig) 
		{
			
		}
		else 
		{
			mKeyboardMappings.insert({key, defaultKeyboardKeys[key]});
			mJoyMappings.insert({key, defaultJoyKeys[key]});
		}
	}
}

// Remap joypad and keyboard keys by assigning the pressed key to the current
// function being displayed.
void remapJoypadKeys(int joyKeyIdx, int keyFunction)
{
	mJoyMappings.find(joyKeyIdx)->second = keyFunction;
}

void remapKeyboardKeys(int keyIdx, int keyFunction)
{

	mKeyboardMappings.find(keyIdx)->second = keyFunction;
}

void sendKey(int key, bool pressed, bool joystick, bool mouse)
{
	unsigned char bytes[6];
	if(key == 127) {
		// We're passing commandline data to anbu.java
		bytes[0] = (key);
		bytes[1] = (char) (angle >> 8) & 0xFF;
		bytes[2] = (char) (angle) & 0xFF;
	}
	else if(mouse) {
		bytes[0] = (mouse << 2) | pressed;
		bytes[1] = (char) (correctedMouseX >> 8) & 0xFF;
		bytes[2] = (char) (correctedMouseX) & 0xFF;
		bytes[3] = (char) (correctedMouseY >> 8) & 0xFF;
		bytes[4] = (char) (correctedMouseY) & 0xFF;

		if(mouseDragged) { bytes[0] = 6; }
	}
	else {
		bytes[0] = (char) (joystick << 4) | pressed;

		if(joystick) { bytes[1] = (char) findJoypadFunction(key); }
		else { bytes[1] = (char) findKeyboardFunction(key); }
		
		bytes[2] = (char) 0;
		bytes[3] = (char) 0;
		bytes[4] = (char) 0;
	}
	fwrite(&bytes, sizeof(char), 5, stdout);
}

bool sendQuitEvent()
{
	SDL_Event* quit = new SDL_Event();
	quit->type = SDL_QUIT;
	SDL_PushEvent(quit);
	return true;
}

/********************************************************** Utility Functions */
void calculateCorrectedMousePos(SDL_Event *event) 
{
	mouseX = event->button.x;
	mouseY = event->button.y;

	// If the screen is rotated, apply a coordinate transformation to keep mouse coords consistent
	if(angle == 270) 
	{
		correctedMouseX = sourceWidth - ((sourceWidth * mouseY) / displayHeight); 
		correctedMouseY = (sourceHeight * mouseX) / displayWidth; 
	}
	else 
	{
		correctedMouseX = (sourceWidth * mouseX) / displayWidth;
		correctedMouseY = (sourceHeight * mouseY) / displayHeight;
	}
}

void calculateDisplaySizeAndAspectRatio(SDL_DisplayMode *dispMode)
{
	double frameAspectRatio;
	if (angle == 270) { frameAspectRatio = (double)sourceHeight / sourceWidth; } 
	else { frameAspectRatio = (double)sourceWidth / sourceHeight; }

    if (frameAspectRatio > 1.0)
    {
        displayWidth = dispMode->h * frameAspectRatio;
        displayHeight = dispMode->h;
    }
    else
    {
        displayWidth = dispMode->w;
        displayHeight = dispMode->w / frameAspectRatio;
    }
}

void toggleFullscreen() 
{
	// Get the desktop display mode
    SDL_DisplayMode dispMode;
    SDL_GetDesktopDisplayMode(0, &dispMode);

	double frameAspectRatio = (double) sourceWidth/sourceHeight;

    // Calculate the desired width and height based on the frame's aspect ratio
    calculateDisplaySizeAndAspectRatio(&dispMode);

    // Resize the window to the desired width and height
    SDL_SetWindowSize(mWindow, dispMode.w, dispMode.h);
	SDL_SetWindowDisplayMode(mWindow, &dispMode);

	if(isFullscreen){ SDL_SetWindowFullscreen(mWindow, SDL_WINDOW_FULLSCREEN_DESKTOP); }
	else
	{ 
		SDL_SetWindowFullscreen(mWindow, SDL_WINDOW_SHOWN);
		if(angle != 270) { SDL_SetWindowSize(mWindow, sourceWidth*windowScale, sourceHeight*windowScale); }
		else { SDL_SetWindowSize(mWindow, sourceHeight*windowScale, sourceWidth*windowScale); }
	}
}

void loadDisplayDimensions()
{
	SDL_DisplayMode dispMode;
	
	SDL_GetDesktopDisplayMode(0, &dispMode);

	// We need to account for aspect ratio here, since mouse coordinates are in
	// respect to the window coordinates and not FreeJ2ME's destination rect.
	calculateDisplaySizeAndAspectRatio(&dispMode);
}

SDL_Rect getDestinationRect()
{
	double scale;
	switch (angle)
	{
	case 0:
	case 180:
		scale = min( (double) displayWidth/sourceWidth, (double) displayHeight/sourceHeight );
		break;
	case 90:
	case 270:
		scale = min( (double) displayWidth/sourceHeight, (double) displayHeight/sourceWidth );
		break;
	default:
		double angleR = std::acos(-1) * angle / 180;
		double boundW = fabs(cos(angleR) * sourceWidth) + fabs(sin(angleR) * sourceHeight);
		double boundH = fabs(sin(angleR) * sourceWidth) + fabs(cos(angleR) * sourceHeight);
		scale = min(displayWidth / boundW, displayHeight / boundH);
		break;
	}

	int w = sourceWidth * scale, h = sourceHeight * scale;
	return { (displayWidth - w )/2, (displayHeight - h)/2, w, h };
}

bool updateFrame(size_t numChars, unsigned char* buffer, FILE* input = stdin)
{
	int read_count = fread(buffer, sizeof(char), numChars, input);
	return read_count == numChars;
}

void drawFrame(unsigned char *frame, size_t pitch, SDL_Rect& dest, int angle, int interFrame = 16)
{
	// Cutoff rendering at 60fps
	if (SDL_GetTicks() - lastTime < interFrame) {
		return;
	}

	lastTime = SDL_GetTicks();

	SDL_RenderClear(mRenderer);
    SDL_UpdateTexture(mTexture, NULL, frame, pitch);
    SDL_RenderCopyEx(mRenderer, mBackground, NULL, NULL, 0, NULL, SDL_FLIP_NONE);
    SDL_RenderCopyEx(mRenderer, mTexture, NULL, &dest, angle, NULL, SDL_FLIP_NONE);
    SDL_RenderCopyEx(mRenderer, mOverlay, NULL, &dest, angle, NULL, SDL_FLIP_NONE);
    SDL_RenderPresent(mRenderer);

	if(getScreenShot)
	{
		time_t currentTime = time(NULL);
		struct tm* localTime = localtime(&currentTime);
		int width, height;
		SDL_GetWindowSize(mWindow, &width, &height);

		// Format the datetime string
		char datetimeString[20];
		snprintf(datetimeString, sizeof(datetimeString), "%d-%02d-%02d %02d:%02d:%02d",
					localTime->tm_year + 1900, localTime->tm_mon + 1, localTime->tm_mday,
					localTime->tm_hour, localTime->tm_min, localTime->tm_sec);

		// Save ScreenShot
		SDL_Surface* surface = SDL_CreateRGBSurface(0, width, height, 32, 0, 0, 0, 0);
		SDL_RenderReadPixels(mRenderer, NULL, SDL_PIXELFORMAT_ARGB8888, surface->pixels, surface->pitch);
		SDL_SaveBMP(surface, strcat(datetimeString, ".bmp"));
		SDL_FreeSurface(surface);

		getScreenShot = false;
	}
}

void loadOverlay(SDL_Rect &rect)
{
	int psize =  overlayScale * rect.w / sourceWidth;
	int size = rect.w * rect.h * 4;
	unsigned char *bytes = new unsigned char[size];

	for (int h = 0; h < rect.h; h++)
		for (int w = 0; w < rect.w; w++)
		{
			int c = (h * rect.w + w) * 4;
			bytes[c] = 0;
			bytes[c+1] = 0;
			bytes[c+2] = 0;
			bytes[c+3] = w % psize == 0 || h % psize == 0 ? 64 : 0;
		}

	mOverlay = SDL_CreateTexture(mRenderer, SDL_PIXELFORMAT_ARGB8888, SDL_TEXTUREACCESS_STATIC, rect.w, rect.h);
	SDL_SetTextureBlendMode(mOverlay, SDL_BLENDMODE_BLEND);
	SDL_UpdateTexture(mOverlay, NULL, bytes, rect.w * sizeof(unsigned char) * 4);
	delete[] bytes;
}

/******************************************************** Processing Function */
void init(Uint8 r = 0, Uint8 g = 0, Uint8 b = 0)
{
	if (sourceWidth == 0 || sourceHeight == 0)
	{
		cerr << "anbu: Neither width nor height parameters can be 0." << endl;
		exit(1);
	}

	if (SDL_Init(SDL_INIT_VIDEO | SDL_INIT_JOYSTICK) < 0 )
	{
		cerr << "Unable to initialize SDL" << endl;
		exit(1);
	}

	loadDisplayDimensions();

	if(false) // This will check for the presence of a SDL-specific config file for input mappings and other game-independent stuff
	{
		
	}
	else // If there's no config, initialize the default keys
	{
		initKeys(false);
	}

	// Clear screen and draw coloured Background
	if(angle == 270) { SDL_CreateWindowAndRenderer(sourceHeight*windowScale, sourceWidth*windowScale, SDL_WINDOW_SHOWN, &mWindow, &mRenderer); }
	else { SDL_CreateWindowAndRenderer(sourceWidth*windowScale, sourceHeight*windowScale, SDL_WINDOW_SHOWN, &mWindow, &mRenderer); }
	if(isFullscreen) { toggleFullscreen(); }
	SDL_SetWindowTitle(mWindow, "FreeJ2ME - SDL");
	SDL_SetRenderDrawColor(mRenderer, r, g, b, 255);
	SDL_RenderClear(mRenderer);
	SDL_RenderPresent(mRenderer);

	// Set scaling properties
	SDL_SetHint(SDL_HINT_RENDER_SCALE_QUALITY, interpol.c_str());
	SDL_RenderSetLogicalSize(mRenderer, displayWidth, displayHeight);
}

#ifdef _WIN32
DWORD WINAPI startStreaming()
{
#else
void startStreaming()
{
#endif
	SDL_Rect dest = getDestinationRect();

	loadOverlay(dest);

	size_t pitch = sourceWidth * sizeof(char) * BYTES;
	size_t numChars = sourceWidth * sourceHeight * BYTES;
	unsigned char* frame = new unsigned char[numChars];

	// Create a mTexture where drawing can take place. Streaming for constant updates.
	mTexture = SDL_CreateTexture(mRenderer, SDL_PIXELFORMAT_RGB24, SDL_TEXTUREACCESS_STREAMING, sourceWidth, sourceHeight);

	while (capturing && updateFrame(numChars, frame) || !sendQuitEvent())
		drawFrame(frame, pitch, dest, angle);

	SDL_DestroyTexture(mTexture);
	delete[] frame;

#ifdef _WIN32
	return 0;
#else
	pthread_exit(NULL);
#endif
}

void startCapturing()
{
	int key;
	SDL_JoystickEventState(SDL_ENABLE);

	while (capturing)
	{
		SDL_Event event;
		if (SDL_WaitEvent(&event))
		{
			if(event.type == SDL_QUIT) 
			{
				capturing = false;
				continue;
			}
			else if(event.type == SDL_KEYDOWN || event.type == SDL_KEYUP) 
			{
				key = event.key.keysym.sym;
				if (key == SDLK_F4) 
				{
					key = -1;
					capturing = false;
				}
				else if (key == SDLK_F8 && event.type == SDL_KEYDOWN)
				{
					getScreenShot = true;
				}
				else if (key == SDLK_F11 && event.type == SDL_KEYDOWN) 
				{
					isFullscreen = !isFullscreen;
					toggleFullscreen();
				}
				else if(key == SDLK_KP_PLUS && event.type == SDL_KEYDOWN) 
				{
					windowScale += 1;

					if(angle == 270) 
					{
						SDL_SetWindowSize(mWindow, sourceHeight*windowScale,
                       		sourceWidth*windowScale);
					}
					else 
					{ 
						SDL_SetWindowSize(mWindow, sourceWidth*windowScale,
                       		sourceHeight*windowScale);
					}
					
				}
				else if(key == SDLK_KP_MINUS && event.type == SDL_KEYDOWN) 
				{
					if(windowScale > 1) 
					{
						windowScale -= 1;
						if(angle == 270) 
						{
							SDL_SetWindowSize(mWindow, sourceHeight*windowScale,
								sourceWidth*windowScale);
						}
						else 
						{ 
							SDL_SetWindowSize(mWindow, sourceWidth*windowScale,
								sourceHeight*windowScale);
						}
					}
				}

				//printf("Key:%d. Down:%s | cast:%s\n", key, event.key.state == SDL_PRESSED ? "true" : "false", keynames[findKeyboardFunction(key)]);// findKeyboardFunction(key));
				sendKey(key, event.key.state == SDL_PRESSED, false, false);
			}

			else if(event.type == SDL_JOYBUTTONDOWN || event.type == SDL_JOYBUTTONUP) 
			{
				if(!remappingKeys) 
				{
					key = event.jbutton.button;
					sendKey(key, event.jbutton.state == SDL_PRESSED, true, false);
					//printf("JoyKey:%d. Down:%s | cast:%s\n", key, event.type == SDL_JOYBUTTONDOWN ? "true" : "false",  mJoyMappings.find(key)->second);
				}
				else 
				{
					if(event.type == SDL_JOYBUTTONDOWN) 
					{

					}
				}
			}

			else if(event.type == SDL_JOYAXISMOTION) 
			{
				// jaxis.value => -32768 to 32767
				int normValue;
				if(abs(event.jaxis.value) <= DEADZONE)
					normValue = 0;
				else
					if(event.jaxis.value > 0)
						normValue = 1;
					else
						normValue = -1;

				if(abs(normValue) != abs(mPrevAxisValues[event.jaxis.which][event.jaxis.axis]))
				{
					if(!remappingKeys) 
					{
						key = 3 * event.jaxis.axis + normValue + 1;
						sendKey(key << 8, normValue != 0, true, false);
						//printf("JoyAxis:%d. Centered:%s\n", key<<8, normValue == 0 ? "true" : "false");
					}
					else 
					{
						// TODO: Allow Remapping joy axis
						if(normValue == 0) 
						{

						}
					}
				}
				mPrevAxisValues[event.jaxis.which][event.jaxis.axis] = normValue;
			}

			else if(event.type == SDL_JOYDEVICEADDED) { addJoystick(event.jdevice.which); }
				
			else if(event.type == SDL_JOYDEVICEREMOVED) { removeJoystick(event.jdevice.which); }
				
			// Mouse keys (any mouse button click is valid)
			else if(event.type == SDL_MOUSEBUTTONDOWN) 
			{
				// Capture mouse button click to send to anbu.java	
				calculateCorrectedMousePos(&event);

				mousePressed = true;
				sendKey(0, true, false, true);
				//printf("\npress coords-> X: %d | Y: %d", correctedMouseX, correctedMouseY);
			}
			else if(event.type == SDL_MOUSEBUTTONUP) 
			{
				// Capture mouse button release to send to anbu.java
				calculateCorrectedMousePos(&event);

				if(mousePressed) 
				{ 
					mousePressed = false;
					mouseDragged = false;
					sendKey(0, false, false, true);
				}
			}
			else if(event.type == SDL_MOUSEMOTION) 
			{
				// Check if a drag event is ocurring
				if(mousePressed && (abs(event.button.x - mouseX) * abs(event.button.y - mouseY)) > dragThreshold)
				{ 
					mouseDragged = true;
					calculateCorrectedMousePos(&event);
					
					//printf("\ndrag coords-> X: %d | Y: %d", correctedMouseX, correctedMouseY);
					sendKey(6, false, false, true); 
				}
			}
			
			else if(event.type == SDL_WINDOWEVENT_SIZE_CHANGED) 
			{
				// if the window was resized, get the new size to correct mouse coordinates;
				displayWidth = event.window.data1 * sourceWidth / event.window.data2;
				displayHeight = event.window.data2 * sourceHeight / event.window.data1;
			}
			fflush(stdout);
		}
	}
}

/*********************************************************************** Main */
int main(int argc, char* argv[])
{
	int c = 0;
	Uint8 r = 44, g = 62, b = 80; // Midnight Blue

	while (++c < argc)
	{
		if ( argc < 3 || string("--help") == argv[c] || string("-h") == argv[c]) {
			cout << getHelp() << endl;
			return 0;
		} else if (c == 1) {
			sourceWidth = atoi(argv[c]);
			sourceHeight = atoi(argv[++c]);
		} else if (c > 2 && string("-r") == argv[c] && argc > c + 1) {
			if(atoi(argv[++c]) == 1) { angle = 270; } // cmd rotation argument
		} else if (c > 2 && string("-i") == argv[c] && argc > c + 1) {
			interpol = argv[++c];
		} else if (c > 2 && string("-f") == argv[c] && argc > c + 1) {
			isFullscreen = atoi(argv[++c]);
		} else if (c > 2 && string("-s") == argv[c] && argc > c + 1) {
			windowScale = atoi(argv[++c]);
		} else if (c > 2 && string("-c") == argv[c] && argc > c + 3) {
			r = atoi(argv[++c]);
			g = atoi(argv[++c]);
			b = atoi(argv[++c]);
		}
	}

	init(r, g, b);
	bool initialCursorState = SDL_ShowCursor(0) == 1;

#ifdef _WIN32
	HANDLE hThreadCapturing;
	if ((hThreadCapturing = CreateThread(NULL, 0, &startStreaming, NULL, 0, &t_capturing)) == NULL) {
		std::cerr << "Unable to start thread, exiting ..." << endl;
		SDL_Quit();
		return 1;
	}
#else
	if (pthread_create(&t_capturing, 0, &startStreaming, NULL))
	{
		std::cerr << "Unable to start thread, exiting ..." << endl;
		SDL_Quit();
		return 1;
	}
#endif

	startStreaming();
#ifdef _WIN32
    WaitForSingleObject(hThreadCapturing, INFINITE);
	CloseHandle(hThreadCapturing);
#else
	pthread_join(t_capturing, NULL);
	SDL_ShowCursor(SDL_ENABLE);
	SDL_Quit();
	return 0;
}
