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

import javax.microedition.lcdui.game.Sprite;

// NativeMem is a complete stub as is, so inherit javax Image directly, as it's what many games like as return value here
public class Image extends javax.microedition.lcdui.Image 
{
	public static final int COLOR_BMP_8BIT = 5;

	protected Image() { }

	Image(byte[] imageData) 
	{
		//TODO: Does it have to be mutable?
		super(imageData, 0, imageData.length, true);
		Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Image(byte[]) untested");
	}

	// The idea is to handle as much of this on lcdui.Image as possible
	Image(byte[] bytes, int imageWidth, int imageHeight) throws IllegalArgumentException
	{
		//TODO: Might be incorrect
		super(bytes, 0, imageWidth * imageHeight, true);
		Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Image(byte[], width, height) untested");
	}

	Image(byte[] bytes, int imageWidth, int imageHeight, boolean transparent) 
	{
		// TODO: May be incorrect
		super(imageWidth, imageHeight);
		Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Image(byte[], width, height, boolean transp) untested");
		if(transparent) { this.setCanvas(createTransparentImageFromBitmap(bytes, imageWidth, imageHeight).getCanvas()); }
		else { this.setCanvas(createImageFromBitmap(bytes, imageWidth, imageHeight).getCanvas()); }
	}

	Image(byte[] rgb, int width, int height, int bitmapType) throws IOException
	{
		super(width, height);
		Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Image(byte[], width, height, int type) untested");
		// TODO: We should verify whether or not Alpha Processing is required here
		this.setCanvas(createRGBImage(rgb, width, height, bitmapType).getCanvas());
	}

	Image(javax.microedition.lcdui.Image image) 
	{
		super(image);
		Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Image(Image) untested");
	}

	Image(int imageWidth, int imageHeight)
	{
		super(imageWidth, imageHeight);
		Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Image(width, height) untested");
	}

	Image(String name, boolean doScale) throws IOException
	{
		super(name);
		if(!doScale) 
		{ 
			Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Image(String, boolean) without scaling untested");
		}
		else 
		{
			Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Image(String, boolean) with scaling untested");
			this.setCanvas(javax.microedition.lcdui.Image.scaleImage(this.getCanvas(), Mobile.getDisplay().getCurrent().getWidth(), Mobile.getDisplay().getCurrent().getHeight()));
		}
	}

	// AH-1 SeaBomber Siemens uses this, works.
	public static javax.microedition.lcdui.Image createImageWithoutScaling(String name) throws IOException
	{ 
		return javax.microedition.lcdui.Image.createImage(name);
	}

	// Karma Studios' Hoverball uses this in one of its versions, works.
	public static javax.microedition.lcdui.Image createRGBImage(byte[] imageData, int imageWidth, int imageHeight, int BitmapType) throws ArrayIndexOutOfBoundsException, IOException
	{
		if (imageWidth <= 0 || imageHeight <= 0) { throw new ArrayIndexOutOfBoundsException("Width and height must be greater than zero."); }
		if (imageData.length < imageWidth * imageHeight) { throw new ArrayIndexOutOfBoundsException("Image data is not sufficient for the specified dimensions."); }

		final int[] imgData = new int[imageWidth * imageHeight];
		boolean hasAlpha = false;
		
		for (int y = 0; y < imageHeight; y++) 
		{
			for (int x = 0; x < imageWidth; x++) 
			{
				int index = y * imageWidth + x;
				byte pixel = imageData[index];

				if (pixel == (byte) 0xC0) { imgData[x + y * imageWidth] = 0x00000000; hasAlpha = true; } // Special byte code: Fully transparent pixel 
				else 
				{
					int red = (pixel >> 5) & 0x07;   // Upper 3 bits for red
					int green = (pixel >> 2) & 0x07; // Next 3 bits for green
					int blue = pixel & 0x03;         // Lower 2 bits for blue

					// Since this isn't full 8-bit values (0-255 range), we have to scale them properly
					red = (red * 255) / 7;
					green = (green * 255) / 7;
					blue = (blue * 255) / 3;

					int rgb = (0xFF << 24) | (red << 16) | (green << 8) | blue;

					imgData[x + y * imageWidth] = rgb;
				}
			}
		}
		return javax.microedition.lcdui.Image.createRGBImage(imgData, imageWidth, imageHeight, hasAlpha);
	}
	
	public static javax.microedition.lcdui.Image createTransparentImageFromBitmap(byte[] bytes, int width, int height) 
	{
		if (width <= 0 || height <= 0) { throw new IllegalArgumentException("Width and height must be greater than zero."); }
	
		javax.microedition.lcdui.Image image = javax.microedition.lcdui.Image.createImage(width, height);
		int[] rgbData = image.getDataBuffer();
	
		for (int j = 0; j < height; j++) 
		{
			for (int i = 0; i < width; i++) 
			{
				int byteIndex = (j * width + i) / 4;
				int bitIndex = (j * width + i) % 4;
	
				// Extract the 2bpp value
				int value = (bytes[byteIndex] >> (6 - bitIndex * 2)) & 0x03;
	
				// Set the color based on the 2bpp value
				switch (value) 
				{
					case 0: // Transparent
						rgbData[j * width + i] = 0x00FFFFFF; // Transparent
						break;
					case 1: // White
						rgbData[j * width + i] = 0xFFFFFFFF;
						break;
					case 2: // Black
					case 3: // Black
						rgbData[j * width + i] = 0xFF000000; // Black
						break;
				}
			}
		}
	
		image.set2Bpp(true);
		return image;
	}

	/* This one is barely documented */
	public static javax.microedition.lcdui.Image getNativeImage(Image img) 
	{
		Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "getNativeImage(image) not implemented"); 
		return img; 
	}

	public static void mirrorImageHorizontally(javax.microedition.lcdui.Image image) 
	{
		image = javax.microedition.lcdui.Image.createImage(image, 0, 0,
			image.getWidth(), image.getHeight(), Sprite.TRANS_MIRROR);
	}

	public static void mirrorImageVertically(javax.microedition.lcdui.Image image) 
	{
		image = javax.microedition.lcdui.Image.createImage(image, 0, 0,
			image.getWidth(), image.getHeight(), Sprite.TRANS_MIRROR_ROT180);
		Mobile.log(Mobile.LOG_WARNING, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "mirrorImageVertically(image) untested"); 
	}
}
