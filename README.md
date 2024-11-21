# freej2me

![Java CI](https://github.com/TASEmulators/freej2me-plus/actions/workflows/ant.yml/badge.svg)
![Libretro Cores](https://github.com/TASEmulators/freej2me-plus/actions/workflows/libretro.yml/badge.svg)

A free J2ME emulator with libretro, awt and sdl2 frontends.

Original authors :
- David Richardson [Recompile@retropie]
- Saket Dandawate  [Hex@retropie]

---

## Controls

* `Q` and `W` for left and right softkeys.
* Arrow keys for nav, unless phone is set to "Standard", when arrow keys become 2, 4, 6, and 8.
* Numbers work as expected, the number pad is inverted (123 swap with 789, like a phone)
* `E` and `R` are alternatives to `*` and `#`.
* Enter functions as the Fire key or `5` on "Standard" mode
* ESC brings up the settings menu
* In the AWT frontend (freej2me.jar) `Ctrl+C` takes a screenshot and `+`/`-` can be used to control the window scaling factor

Click [here](KEYMAP.md) for information about more keybindings

## Links
Latest build:

  Java: https://nightly.link/TASEmulators/freej2me-plus/workflows/ant/devel

  Libretro cores: https://nightly.link/TASEmulators/freej2me-plus/workflows/libretro/devel

  Screenshots: https://imgur.com/a/2vAeC

  Compatibility List: https://tasemulators.github.io/freej2me-plus/

----
**FreeJ2ME Jar Compilation:**

>From the root directory, running the following commands:
>```
> > cd freej2me/
> > ant
>```
> Will create three different jar files inside `build/`:
>
> `freej2me.jar` -> Standalone AWT jar executable, currently the main standalone flavor
> 
> `freej2me-lr.jar` -> Libretro executable (has to be placed on the frontend's `system/` folder, since it acts as a BIOS for the libretro core and runs J2ME jars)
>
>`freej2me-sdl.jar` -> Jar executable meant to be used in conjunction with SDL2 for libtas and joystick support, will likely become the de-facto standalone at some point
>
>The Libretro jar file needs additional binaries to be compiled before use. Look at the additional steps below if you're going to use it.

**Building the Libretro core**

> **For linux:**
>To build the libretro core, open a terminal in freej2me's folder run the following commands from there:
>```
># libretro core compilation
> > cd src/libretro
> > make
>```
>This will build `freej2me_libretro.so` on `src/libretro/`, which is the core libretro will use to interface with `freej2me-lr.jar`.
>
>Move it to your libretro frontend's `cores/` folder, with freej2me-lr.jar on `system/` and the frontend should be able to load j2me files afterwards.
>
>NOTE: The core DOES NOT WORK on containerized/sandboxed environments unless it can call a java runtime that also resides in the same sandbox or container, keep that in mind if you're running a libretro frontend through something like flatpak or snap for example.
>

> **For windows:**
>To build the libretro core for windows, first you'll need mingw, or MSYS2 64. **`This guide uses MSYS2`** as it's easier to set up and works closer to linux syntax.
>
> Download MSYS2-x86_64 and install it on your computer. By default it will create a linux-like 'home' folder on C:\msys64\home\ and will put a folder with your username in there. This is where you have to move the freej2me folder to, so: `C:\msys64\home\USERNAME\freej2mefolder` for example.
>
> With the folder placed in there you can build the core, open the MSYS2 UCRT64 terminal from your pc's start menu, and run the following commands:
>```
> # Installing 'mingw-w64' and 'make' on msys2
> > pacman -S mingw-w64-ucrt-x86_64-gcc
> > pacman -S make
>
> # libretro core compilation
> > cd freej2mefolder/src/libretro
> > make
>```
>This will build `freej2me_libretro.dll` on `freej2mefolder/src/libretro/`, which is the core libretro will use to interface with `freej2me-lr.jar`.
>
>Move it to your libretro frontend's `cores/` folder, with freej2me-lr.jar on `system/` and the frontend should be able to load j2me files afterwards.
>
>NOTE: The windows core has been tested on Windows 10 & 11 x64.

----
**Usage (applies to AWT and SDL):**

Launching the AWT frontend (freej2me.jar) will bring up a filepicker to select the MIDlet to run.

Alternatively it can be launched from the command line: `java -jar freej2me.jar 'file:///path/to/midlet.jar' [width] [height] [scale]`
Where _width_, _height_ (dimensions of the simulated screen) and _scale_ (initial scale factor of the window) are optional arguments.

The SDL2 frontend (freej2me-sdl.jar) accepts the same command-line arguments format, aside from the _scale_ option which is unavailable. **NOTE**: This flavor requires libSDL 2.24.0-1 or newer in order to even launch. Make sure you have it installed in your system, or placed alongside the jar for it to load.

When running under Microsoft Windows please do note paths require an additional `/` prefixed. For example, `C:\path\to\midlet.jar` should be passed as `file:///C:\path\to\midlet.jar`

Special note for Windows: It is recommended to use Adoptium's [OpenJDK JRE](https://adoptium.net/temurin/releases/?os=windows&arch=x64&package=jre), instead of Oracle JRE. Late versions of Oracle introduced a bootstrapper javaw.exe, which will leave the actual javaw.exe process behind once the game is closed in RetroArch. Unfortunately, there is no good way for the core to determine which javaw is the _actual_ one, so you will either need to edit your system env to add the actual javaw.exe's path and delete the `javapath` one, or simply use Adoptium's JRE which does not have this issue.

FreeJ2ME keeps savedata and config at the working directory it is run from. Currently any resolution specified at the config file takes precedence over the values passed via command-line.

---

## Modules and external dependencies used:

### JLayer(MPEG Player): - LGPLv2.1 License, compatible with GPLv3

### libsdl4j: zlib License, compatible with GPLv3

### ObjectWeb's ASM: BSD 3-Clause License, not directly compatible with GPLv3, but can be used as long as the original license is published alongside GPLv3 (check the 'License' tab)

### Libretro's API: MIT License, compatible with GPLv3

# How to contribute as a developer:
  1) Open an Issue
  2) Try solving that issue
  3) Post on the Issue if you have a possible solution
  4) Submit a PR implementing the solution

**If you are not a developer, just open an issue normally.**
