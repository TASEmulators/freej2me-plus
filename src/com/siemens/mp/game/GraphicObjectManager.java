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

package com.siemens.mp.game;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformImage;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import java.util.ArrayList;


public class GraphicObjectManager extends com.siemens.mp.misc.NativeMem
{
	private volatile ArrayList<GraphicObject> list = new ArrayList<GraphicObject>();

	public GraphicObjectManager() { }

	public static byte[] createTextureBits(int width, int height, byte[] texture)
	{
		int bitArraySize = (width * height + 7) / 8;
		byte[] bitTexture = new byte[bitArraySize];

		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				int pixelValue = texture[y * width + x] & 0xFF;

				// Calculate the byte and bit position in the bit array
				int byteIndex = (y * width + x) / 8;
				int bitIndex = (y * width + x) % 8;

				if (pixelValue != 0) { bitTexture[byteIndex] |= (1 << (7 - bitIndex)); }
			}
		}

		return bitTexture;
	}


	public void addObject(GraphicObject g) { list.add(g); }

	public void insertObject(GraphicObject g, int pos) { list.add(pos, g); }

	public void deleteObject(GraphicObject g) { list.remove(g); }

	public void deleteObject(int position) { list.remove(position); }


	public GraphicObject getObjectAt(int index) { return list.get(index); }

	public int getObjectPosition(GraphicObject g) { return list.indexOf(g); }


	public void paint(ExtendedImage img, int x, int y) { paint(img.getImage(), x, y); }

	public void paint(Image image, int x, int y)
	{
		// Siemens is so unbelievably jank that apps apparently can make changes
		// to the GraphicObjects whilst they're rendering, so be extra cautious.
		for (int i = 0; i < list.size(); i++)
		{
			if (i < list.size())
			{
				GraphicObject obj = list.get(i);
				if (obj != null && obj.getVisible())
				{
					obj.paint(image.getGraphics(), x, y);
				}
			}
		}
	}
}
