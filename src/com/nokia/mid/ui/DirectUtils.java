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
package com.nokia.mid.ui;

import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Graphics;

import org.recompile.mobile.PlatformGraphics;

public class DirectUtils
{

	public static Image createImage(byte[] imageData, int imageOffset, int imageLength)
	{
		return Image.createNokiaImage(imageData, imageOffset, imageLength);
	}

	public static Image createImage(int width, int height, int ARGBcolor)
	{
		Image image = Image.createImage(width, height); // This one already creates a mutable image
		PlatformGraphics gc = (PlatformGraphics)image.getGraphics();
		gc.clearRect(0, 0, width, height);
		gc.setAlphaRGB(ARGBcolor);
		gc.fillRect(0,0, width, height);
		return image;
	}

	public static DirectGraphics getDirectGraphics(javax.microedition.lcdui.Graphics g)
	{
		return (PlatformGraphics)g;
	}

}
