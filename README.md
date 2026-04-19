
![BannerFinal](https://github.com/user-attachments/assets/ca82914c-e30e-406d-8d2e-487bda6263af)

<h1 align="center"> Current status </h1>

<div align="center">

[![Java CI](https://img.shields.io/github/actions/workflow/status/TASEmulators/freej2me-plus/ant.yml?style=for-the-badge&label=FreeJ2ME-Plus%20Core)](https://github.com/jpcsp/jpcsp/blob/master/.github/workflows/main.yml)
[![Website](https://img.shields.io/website?url=https%3A%2F%2Fjpcsp.org%2F&style=for-the-badge&label=FreeJ2ME-Plus%20Webpage)](https://tasemulators.github.io/freej2me-plus/)
![Java version](https://img.shields.io/badge/Java-6-44cc11?style=for-the-badge&label=Minimum%20Java%20VM)
![License](https://img.shields.io/badge/license-GPLv3-red?style=for-the-badge&label=Project%20License)
![Open Issues](https://img.shields.io/github/issues/TASEmulators/freej2me-plus?style=for-the-badge)
![Last Commit](https://img.shields.io/github/last-commit/TASEmulators/freej2me-plus?style=for-the-badge)

</div>

<h1 align="center"> Links to FreeJ2ME-Plus Builds </h1>

<div align="center">

[![Nightly Releases](https://img.shields.io/github/v/release/TASEmulators/freej2me-plus?label=Bleeding%20Edge%20Builds:&style=for-the-badge)](https://github.com/TASEmulators/freej2me-plus/releases/tag/nightlies)
[![Latest Stable Release](https://img.shields.io/badge/version-v1.52-blue?label=Latest%20Stable%20Release:&style=for-the-badge)](https://github.com/TASEmulators/freej2me-plus/releases/tag/1.52)

</div>

---

# :question: What is it?

### FreeJ2ME-Plus is a J2ME emulator with libretro and AWT frontends, it aims to run on basically anything that can run a Java VM.

### Original authors :
#### - David Richardson [Recompile@retropie]
#### - Saket Dandawate  [Hex@retropie]

### Current maintainer:
#### - Paulo Sousa [AShiningRay]

---

# :bar_chart: Compatibility list

### For a general idea of what can or cannot run, look [HERE](https://tasemulators.github.io/freej2me-plus/)

----
# :gear: :coffee: Building FreeJ2ME-Plus

>**Make sure you have Apache Ant installed and can run it. Then, from the freej2me directory, run the following command (yes, it's that simple):**
>```
> > ant
>```
>**That command will create two different jar files inside `build/`:**
>
>**`freej2me.jar` -> Standalone AWT jar executable, can be double-clicked right away to start**
>
>**`freej2me-lr.jar` -> Libretro executable (has to be placed on the frontend's `system/` folder, since it acts as a BIOS for the libretro core and is what runs J2ME jars)**
>
>### **NOTE: The Libretro jar file needs additional binaries to be compiled before use. Look at the additional steps below if you're going to use it.**

# :gear: :video_game: Building the Libretro core

### Building for Linux:
>**To build the libretro core, be sure you can run the `make` command, then open a terminal in freej2me's folder run the following commands from there:**
>```
># libretro core compilation
> > cd src/libretro
> > make
>```
>**This will build `freej2me_libretro.so` on `src/libretro/`, which is the core libretro will use to interface with `freej2me-lr.jar`.**
>
>**Move it to your libretro frontend's `cores/` folder, with freej2me-lr.jar on `system/` and the frontend should be able to load j2me files afterwards.**
>
> ### **NOTE: The core DOES NOT WORK on containerized/sandboxed environments unless it can call a java runtime that also resides in the same sandbox or container, keep that in mind if you're running a libretro frontend through something like flatpak or snap for example.**
>

<h1> </h1>

### Building for Windows:
>**To build the libretro core for windows, first you'll need mingw, or MSYS2 64. **`This guide uses MSYS2`** as it's easier to set up and works closer to linux syntax.**
>
>**Download MSYS2-x86_64 and install it on your computer. By default it will create a linux-like 'home' folder on C:\msys64\home\ and will put a folder with your username in there. This is where you have to move the freej2me folder to, so: `C:\msys64\home\USERNAME\freej2mefolder` for example.**
>
>**With the folder placed in there you can build the core, open the MSYS2 UCRT64 terminal from your pc's start menu, and run the following commands:**
>```
># Installing 'mingw-w64' and 'make' on msys2
> > pacman -S mingw-w64-ucrt-x86_64-gcc
> > pacman -S make
>
> # libretro core compilation
> > cd freej2mefolder/src/libretro
> > make
>```
>**This will build `freej2me_libretro.dll` on `freej2mefolder/src/libretro/`, which is the core libretro will use to interface with `freej2me-lr.jar`.**
>
>**Move it to your libretro frontend's `cores/` folder, with freej2me-lr.jar on `system/` and the frontend should be able to load j2me files afterwards.**
>
> ### **NOTE: The windows core has been tested on Windows 7, 10 & 11 x64.**
>
----
# :memo: How to use the AWT frontend:

Launching the AWT frontend (freej2me.jar) directly will bring up the standalone GUI, where you can load your application through the `File` menu, or by **dragging and dropping your JAR/JAD/KJX/MSD file onto it**. 

You can also configure many aspects of the runtime, including debug options:

<img width="252" height="390" alt="image" src="https://github.com/user-attachments/assets/64737dd3-eea6-4437-b236-aeb163841f21" />
<img width="252" height="390" alt="image" src="https://github.com/user-attachments/assets/5b105d46-d83b-4d1f-9913-687d281523d4" />
<img width="254" height="390" alt="image" src="https://github.com/user-attachments/assets/95054a08-35fb-4e99-87f2-ba36a9a8629c" />

<img width="968" height="399" alt="image" src="https://github.com/user-attachments/assets/f28042c3-1d7f-47be-8942-0e5a5b2cf2b9" />

<h1> </h1>

Alternatively it can be launched from the command line with the following arguments:

- `fullscreen` :arrow_right: `1 = yes, 0 = no`
- `width` :arrow_right: `self explanatory, it's the virtual LCD's width`
- `height` :arrow_right: `also self explanatory, it's the virtual LCD's height`
- `scale` :arrow_right: `for windowed mode, dictates the scale that FreeJ2ME-Plus' window starts with.` 
  - '2' will make it 2X bigger than the original width and height size for example
  - **Note that using the '+' and '-' keys also let you scale the window by an integer factor of +-1**
- `keyLayout` :arrow_right: `specifies which device key layout should be used when booting up.` These can be:
  - `0  -> Default`
  - `1  -> LG`
  - `2  -> Motorola/Softbank`
  - `3  -> Motorola Triplets`
  - `4  -> Motorola V8`
  - `5  -> Motorola A1000`
  - `6  -> Nokia Keyboard`
  - `7  -> Sagem`
  - `8  -> Siemens`
  - `9  -> Sharp`
  - `10 -> SKT`
  - `11 -> KDDI`
- `framerate` :arrow_right: `sets the maximum FPS applications are allowed to run at.` 
  - Can be any value, although '10' to '60' is the expected ballpark
- `dojaversion` :arrow_right: `sets the DoJa/Star profile for the I-Appli to use.` These can be:
  - `10 -> Default`
  - `20 -> DoJa 2.0 & International 1.5`
  - `30 -> DoJa 3.0 & International 2.5`
  - `35 -> DoJa 3.5`
  - `40 -> DoJa 4.0`
  - `41 -> DoJa 4.1`
  - `50 -> DoJa 5.0`
  - `51 -> DoJa 5.1`
  - `100 -> Star 1.0`
  - `110 -> Star 1.1`
  - `120 -> Star 1.2`
  - `130 -> Star 1.3`
  - `150 -> Star 1.5`
  - `200 -> Star 2.0`

<h1> </h1>

Those are organized and read internally in this manner: `java -jar freej2me.jar 'file:///path/to/midlet.jar' fullscreen width height scale keyLayout framerate dojaversion`

Although all arguments aside from the path are optional to launch FreeJ2ME-Plus with any given app.

### _Notes:_

**When running under Microsoft Windows please do note paths require an additional `/` prefixed. For example, `C:\path\to\midlet.jar` should be passed as `file:////C:\path\to\midlet.jar`**

**FreeJ2ME keeps savedata and config at the working directory it is run from. Currently any settings specified at the config file take precedence over the values passed via command-line.**

---

# :mag: Modules and external dependencies used:

- #### JLayer(MPEG Player): - LGPLv2.1 License, compatible with GPLv3

- #### libsdl4j: zlib License, compatible with GPLv3

- #### ObjectWeb's ASM: BSD 3-Clause License, not directly compatible with GPLv3, but can be used as long as the original license is published alongside GPLv3 (check the 'License' tab)

- #### Libretro's API: MIT License, compatible with GPLv3

- #### Roman Lahin [rmn20](https://github.com/rmn20)'s MascotCapsuleV3 renderer (MascotME): MIT License, compatible with GPLv3

---

# :busts_in_silhouette: How to contribute

### If you're a developer:

  1) Open an Issue
  2) Try solving that issue
  3) Post on the Issue if you have a possible solution
  4) Submit a PR implementing the solution

### If you're an user:

  1) Open an Issue
  2) Explain it in as much detail as you can (FreeJ2ME-Plus version, jar used, md5 hash, as well as the issue with logs and images if possible)
  3) Post a save file close to where the issue manifests, or note the steps required to reproduce it
