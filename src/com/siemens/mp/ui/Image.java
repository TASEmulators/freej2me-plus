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
package com.siemens.mp.ui;

import java.io.IOException;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformImage;

import javax.microedition.lcdui.game.Sprite;

// NativeMem is a complete stub as is, so inherit javax Image directly
public class Image extends javax.microedition.lcdui.Image 
{
    int width, height;
    static javax.microedition.lcdui.Image img;
    public static final int COLOR_BMP_8BIT = 5;

    protected Image() { }

    Image(byte[] imageData) 
    {
        Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Image(byte[]) not implemented");
        // TODO
    }

    // The idea is to handle as much of this on lcdui.Image as possible
    Image(byte[] bytes, int imageWidth, int imageHeight) throws IllegalArgumentException
	{
        Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Image(byte[], width, height) not implemented");
        //TODO: Might be incorrect
		img = javax.microedition.lcdui.Image.createImage(bytes, width, height);
        width = img.getWidth();
        height = img.getHeight();
	}

    Image(byte[] bytes, int imageWidth, int imageHeight, boolean transparent) 
    {
        Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Image(byte[], width, height, boolean transp) not implemented");
        // TODO
		//img = javax.microedition.lcdui.Image.createRGBImage(rgb, width, height, transparent);
        //width = img.getWidth();
        //height = img.getHeight();
    }

    Image(byte[] rgb, int width, int height, int bitmapType)
	{
        Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Image(byte[], width, height, int type) not implemented");
        // TODO: We should verify whether or not Alpha Processing is required here
		//img = javax.microedition.lcdui.Image.createRGBImage(rgb, width, height, true);
	}

    Image(javax.microedition.lcdui.Image image) 
    {
        Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Image(Image) untested");
        img = javax.microedition.lcdui.Image.createImage(image);
        width = img.getWidth();
        height = img.getHeight();
    }

    Image(int imageWidth, int imageHeight)
	{
        Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Image(width, height) untested");
		img = javax.microedition.lcdui.Image.createImage(imageWidth, imageHeight);
	}

    Image(String name, boolean doScale) 
    {
        Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Image(String, boolean) not implemented");
        try 
        {
            if(!doScale) { img = createImageWithoutScaling(name); }
            else 
            {
                img = createImageWithoutScaling(name);
                // Do Scaling
            }
        } catch (IOException e) { }
    }

    // AH-1 SeaBomber Siemens uses this, works.
    public static javax.microedition.lcdui.Image createImageWithoutScaling(String name) throws IOException
    { 
        return javax.microedition.lcdui.Image.createImage(name);
    }

    /* This one is barely documented */
    public static javax.microedition.lcdui.Image getNativeImage(Image img) 
    {
        Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "getNativeImage(image) not implemented"); 
        return img; 
    }

    public static void mirrorImageHorizontally(javax.microedition.lcdui.Image image) 
    {
        img = image;
        img.platformImage = new  org.recompile.mobile.PlatformImage(image, 0, 0, img.getWidth(), img.getHeight(), Sprite.TRANS_MIRROR); // Seems to work in 'Bermuda'
    }

    public static void mirrorImageVertically(javax.microedition.lcdui.Image image) 
    {
        img = image;
        img.platformImage = new  org.recompile.mobile.PlatformImage(image, 0, 0, img.getWidth(), img.getHeight(), Sprite.TRANS_MIRROR_ROT180); // Untested
        Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "mirrorImageVertically(image) untested"); 
    }

	public int getHeight() { return height; }

	public int getWidth() { return width; }

}