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
import org.recompile.mobile.PlatformGraphics;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import com.siemens.mp.misc.NativeMem;

public class ExtendedImage extends com.siemens.mp.misc.NativeMem
{
	private int[] palette1Bpp = { 0xFFFFFFFF, 0xFF000000 };
	private int[] palette2Bpp = { 0x00FFFFFF, 0xFFFFFFFF, 0xFF000000, 0xFF000000 };

	private Image image;

	private PlatformGraphics gc;

	private int width;

	private int height;

	public ExtendedImage(Image img)
	{
		image = Image.createImage(img);
		width = image.getWidth();
		height = image.getHeight();
		image.setMutable(true);
		gc = image.getGraphics();
	}

	public Image getImage() { return image; }

	public int getPixel(int x, int y)
	{
		if (x < 0 || y < 0 || x >= width || y >= height) { return 0; }

		int pixelValue = image.getPixel(x, y);

		if (image.is2Bpp())
		{
			// 0 = Transparent, 1 = White, 2 & 3 = Black
			int alpha = (pixelValue >> 24) & 0xFF;
			if (alpha == 0) { return 0; } // Transparent

			int rgb = pixelValue & 0x00FFFFFF;
			if (rgb == 0x00FFFFFF) return 1; // White

			return 2; // Black
		}
		else
		{
			int rgb = pixelValue & 0x00FFFFFF;
			return (rgb == 0x00FFFFFF) ? 0 : 1; // 0 = white, 1 = black
		}
	}

	public void setPixel(int x, int y, byte color)
	{
		if (x < 0 || y < 0 || x >= width || y >= height) { return; }

		if(image.is2Bpp()) { image.setPixel(x, y, palette2Bpp[color & 0x3]); }
		else { image.setPixel(x, y, palette1Bpp[color & 0x1]); }
	}

	public void getPixelBytes(byte[] pixels, int x, int y, int width, int height)
	{
		if ((x & 7) != 0 || (width & 7) != 0)
		{
			throw new IllegalArgumentException("x and width must be multiples of " + (image.is2Bpp() ? 4 : 8));
		}

		if (pixels == null) { throw new NullPointerException("Pixels byte array cannot be null."); }

		boolean is2bpp = image.is2Bpp();
		int pixelsPerByte = is2bpp ? 4 : 8;

		for (int j = 0; j < height; j++)
		{
			for (int i = 0; i < width; i++)
			{
				int currX = x + i;
				int currY = y + j;

				int pixelColor = getPixel(currX, currY);
				int bitOffset = j * width + i;
				int byteIdx = bitOffset / pixelsPerByte;
				int bitIdx = bitOffset % pixelsPerByte;

				if (byteIdx >= pixels.length) { continue; }

				if (bitIdx == 0) { pixels[byteIdx] = 0; }

				if (is2bpp)
				{
					int shift = 6 - (bitIdx * 2);
					pixels[byteIdx] |= (byte) ((pixelColor & 0x03) << shift);
				}
				else
				{
					int shift = 7 - bitIdx;
					if ((pixelColor & 0x01) != 0) { pixels[byteIdx] |= (byte) (1 << shift); }
				}
			}
		}
	}

	public void setPixels(byte[] pixels, int x, int y, int width, int height)
	{
		if ((x & 7) != 0 || (width & 7) != 0)
		{
			throw new IllegalArgumentException("x and width must be multiples of 8.");
		}

		if (pixels == null) { return; }

		boolean is2bpp = image.is2Bpp();
		int pixelsPerByte = is2bpp ? 4 : 8;

		/*
		 * Some Siemens jars use negative coordinates, and in those cases, it appears to be so that the
		 * data can be retrieved from an area of the byte array that would normally be outside the screen
		 */
		int startX = Math.max(0, x);
		int startY = Math.max(0, y);
		int endX = Math.min(x + width, this.width);
		int endY = Math.min(y + height, this.height);

		for (int currY = startY; currY < endY; currY++)
		{
			int srcY = currY - y;
			for (int currX = startX; currX < endX; currX++)
			{
				int srcX = currX - x;
				int bitOffset = srcY * width + srcX;
				int byteIdx = bitOffset / pixelsPerByte;
				int bitIdx = bitOffset % pixelsPerByte;

				if (byteIdx >= pixels.length) continue;

				byte color;
				if (is2bpp)
				{
					int shift = 6 - (bitIdx * 2);
					color = (byte) ((pixels[byteIdx] >> shift) & 0x03);
				}
				else
				{
					int shift = 7 - bitIdx;
					color = (byte) ((pixels[byteIdx] >> shift) & 0x01);
				}

				setPixel(currX, currY, color);
			}
		}
	}

	public void clear(byte color)
	{
		if(image.is2Bpp()) { gc.setColor(palette2Bpp[color & 0x3]); }
		else { gc.setColor(palette1Bpp[color & 0x1]); }
		gc.fillRect(0, 0, width, height);
		gc.setColor(0xFFFFFFFF);
	}

	// We don't flush the image directly to the front buffer with flushGraphics,
	// instead we just draw onto the current Displayable and wait for a Canvas
	// repaint to show it on screen.
	public void blitToScreen(int x, int y)
	{
		Graphics screenGfx = Mobile.getDisplay().getCurrent().platformImage.getMIDPGraphics();
		screenGfx.drawImage(image, x, y, 0);
	}
}
