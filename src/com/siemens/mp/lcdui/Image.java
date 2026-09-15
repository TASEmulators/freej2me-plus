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

import java.io.IOException;

import org.recompile.mobile.Mobile;

public class Image extends javax.microedition.lcdui.Image
{
    public static final int COLOR_BMP_8BIT = 5;

    public static Image createImageFromFile(String filename, boolean ScaleToFullScreen) throws IOException
    {
        Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "createImageFromFile(String, boolean) untested");
        Image img = (Image) createImage(filename);
        if(ScaleToFullScreen) { img.setCanvas(scaleImage(img.getCanvas(), Mobile.getDisplay().getCurrent().getHeight(), Mobile.getDisplay().getCurrent().getHeight())); }
        return img;
    }

    public static Image createImageFromFile(String filename, int ScaleToWidth, int ScaleToHeight) throws IOException
    {
        Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "createImageFromFile(String, int, int) untested");
        Image img = (Image) createImage(filename);
        img.setCanvas(scaleImage(img.getCanvas(), ScaleToWidth, ScaleToHeight));
        return img;
    }

    public static int getPixelColor(javax.microedition.lcdui.Image image, int x, int y)
    {
        Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "getPixelColor(Image, int, int) untested");
        return image.getPixel(x, y);
    }

    // Works in Bubble Boost
    public static void setPixelColor(javax.microedition.lcdui.Image image, int x, int y, int color)
    {
        image.setPixel(x, y, color);
    }

    public static void writeBmpToFile(javax.microedition.lcdui.Image image, String filename) throws IOException
    {
        Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "writeBmpToFile(Image, String) untested");
        Image.dumpImage(image.getCanvas(), filename, "");
    }

    public static javax.microedition.lcdui.Image createTransparentImageFromMask(javax.microedition.lcdui.Image image, javax.microedition.lcdui.Image mask)
    {
		int width = image.getWidth();
		int height = image.getHeight();
		int[] imagePixels = new int[width * height];
		int[] maskPixels = new int[width * height];

		image.getRGB(imagePixels, 0, width, 0, 0, width, height);
		mask.getRGB(maskPixels, 0, width, 0, 0, width, height);

		for (int y = 0; y < height; y++)
        {
			for (int x = 0; x < width; x++)
            {
				if (maskPixels[y * width + x] == 0xFFFFFFFF)
                {
					imagePixels[y * width + x] = 0;
				}
			}
		}
		return javax.microedition.lcdui.Image.createRGBImage(imagePixels, width, height, true);
	}
}
