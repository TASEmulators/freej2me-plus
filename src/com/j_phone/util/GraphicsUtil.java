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
package com.j_phone.util;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class GraphicsUtil 
{
	public static final int TRANS_NONE = 0;
	public static final int TRANS_ROT90 = 5;
	public static final int TRANS_ROT180 = 3;
	public static final int TRANS_ROT270 = 6;
	public static final int TRANS_MIRROR = 2;
	public static final int TRANS_MIRROR_ROT90 = 7;
	public static final int TRANS_MIRROR_ROT180 = 1;
	public static final int TRANS_MIRROR_ROT270 = 4;
	public static final int STRETCH_QUALITY_NORMAL = 0;
	public static final int STRETCH_QUALITY_LOW = 1;
	public static final int STRETCH_QUALITY_HIGH = 2;

	public static int getPixel(Graphics g, int x, int y) { return g.getCanvas().getRGB(x, y); }

	public static void setPixel(Graphics g, int x, int y) { g.getCanvas().setRGB(x, y, 0); }

	public static void setPixel(Graphics g, int x, int y, int color) { g.getCanvas().setRGB(x, y, color); }

	public static void drawRegion(Graphics g, Image src, int x_src, int y_src, int width, int height, int transform,
	int x_dest, int y_dest, int anchor) 
	{
		// TODO: No docs on this, but Disney Meteos for SoftBank expects the drawn region to be clipped to fit in bounds
		if (x_src < 0) { x_src = 0; }
		if (y_src < 0) { y_src = 0; }
		if (x_src + width > src.getWidth()) { width = src.getWidth() - x_src; }
		if (y_src + height > src.getHeight()) { height = src.getHeight() - y_src; }
		
		g.drawRegion(src, x_src, y_src, width, height, transform, x_dest, y_dest, anchor);
	}

	public static void drawRegion(Graphics g, Image src, int x_src, int y_src, int width, int height, int transform,
	int x_dest, int y_dest, int width_dest, int height_dest, int anchor,
	int stretch_quality) 
	{
		// TODO: This wasn't found in use yet, but same logic as above probably applies.
		if (x_src < 0) { x_src = 0; }
		if (y_src < 0) { y_src = 0; }
		if (x_src + width > src.getWidth()) { width = src.getWidth() - x_src; }
		if (y_src + height > src.getHeight()) { height = src.getHeight() - y_src; }
		
		g.drawRegion(src, x_src, y_src, width, height, transform, x_dest, y_dest, width_dest, height_dest, anchor, stretch_quality);
	}

	public static void drawPseudoTransparentImage(Graphics g, Image src, int x_dest, int y_dest, int anchor, short mask_pattern, int element_size) 
	{
		g.drawImage(src, x_dest, y_dest, anchor);
	}
}
