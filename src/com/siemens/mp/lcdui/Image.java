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
package com.siemens.mp.lcdui;

import org.recompile.mobile.Mobile;

public class Image extends javax.microedition.lcdui.Image
{
    public static final int COLOR_BMP_8BIT = 5;

    public static Image createImageFromFile(String filename, boolean ScaleToFullScreen) 
    { 
        Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "createImageFromFile(String, boolean) not implemented");
        return null;
    }

    public static Image createImageFromFile(String filename, int ScaleToWidth, int ScaleToHeight) 
    { 
        Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "createImageFromFile(String, int, int) not implemented");
        return null;
    }

    public static int getPixelColor(Image image, int x, int y) 
    { 
        Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "getPixelColor(Image, int, int) not implemented");
        return 0;
    }

    public static void setPixelColor(Image image, int x, int y, int color) 
    { 
        Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "setPixelColor(String, int, int, int) not implemented");
    }

    public static void writeBmpToFile(Image image, String filename) 
    { 
        Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "writeBmpToFile(Image, String) not implemented");
    }
}