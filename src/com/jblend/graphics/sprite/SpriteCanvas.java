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
package com.jblend.graphics.sprite;

import java.util.ArrayList;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformImage;

public abstract class SpriteCanvas extends Canvas
{
	private Image fbImage;
	private Graphics fbGraphics;
	private int[] palettes;
	private byte[] patternData;
	private int[] pixels;

	private int fbTx = 0, fbTy = 0;

	// The "virtual screen" here is just the lcd backbuffer that any Displayable gets when being created

	public SpriteCanvas(int numPalettes, int numPatterns)
	{
		super();
		this.palettes = new int[numPalettes];
		this.patternData = new byte[numPatterns * 64];
		this.pixels = new int[64];

		// The post flush draw for SpriteCanvas MUST include the framebuffer draw as well.
		postFlushDraw = new Runnable()
		{
			@Override
			public void run()
			{
				// Draw FrameBuffer right before the commands bar.
				Mobile.getPlatform().getLcdFrontbufferGraphics().drawImage(fbImage, fbTx, fbTy);
				if (!fullscreen && !commands.isEmpty()) { paintCommandsBar(); }
			}
		};
	}

	public void createFrameBuffer(int fw, int fh)
	{
		if(fbImage != null) { throw new IllegalStateException("FrameBuffer already exists!"); }
		if(fw > getVirtualWidth() || fh > getVirtualHeight()) { throw new IllegalArgumentException("Size is larger than the screen"); }

		fbImage = Image.createImage(fw, fh);
		fbGraphics = (Graphics) fbImage.getGraphics();
	}

	public void disposeFrameBuffer() { fbGraphics = null; fbImage = null; }

	// Copies from the virtual screen (back buffer) to the currently set FrameBuffer
	public void copyArea(int sx, int sy, int fw, int fh, int tx, int ty)
	{
		if(fbImage == null) { throw new IllegalStateException("FrameBuffer is not ready!"); }
		if(sx < 0 || sy < 0 || fw > fbImage.getWidth() || fh > fbImage.getHeight()) { throw new IllegalArgumentException("Invalid position and/or size received"); }

		if(Mobile.getDisplay().getCurrent() != this) { return; }

		// Areas that go out of bounds must be discarded.
		if(sx + fw > getVirtualWidth()) { fw = getVirtualWidth() - sx; }
		if(sy + fh > getVirtualHeight()) { fh = getVirtualHeight() - sy; }

		// Nothing to draw.
		if(fw <= 0 || fh <= 0) { return; }

		graphics.copyToFrameBuffer(fbImage, sx, sy, fw, fh, tx, ty, 0);
	}

	// TODO: This should copy from an area of the virtual screen into another to allow features like scrolling, but is untested
	public void copyFullScreen(int tx, int ty)
	{
		if(fbImage == null) { throw new IllegalStateException("FrameBuffer is not ready!"); }
		if(Mobile.getDisplay().getCurrent() != this) { return; }

		// Stuff that goes out of screen is just discarded
		int sx = (tx < 0) ? -tx : 0;
		int sy = (ty < 0) ? -ty : 0;
		int width  = getVirtualWidth() - Math.abs(tx);
		int height = getVirtualHeight() - Math.abs(ty);

		if (width > 0 && height > 0) { graphics.copyArea(sx, sy, width, height, tx, ty); }
	}

	public void drawFrameBuffer(final int tx, final int ty)
	{
		if(fbImage == null) { throw new IllegalStateException("FrameBuffer is not ready!"); }
		if(Mobile.getDisplay().getCurrent() != this) { return; }

		fbTx = tx;
		fbTy = ty;

		// Effectively calls the repaintRequest override below, as this draws
		// to the real screen.
		repaint();
	}

	public void setPalette(int index, int palette)
	{
		if(index >= palettes.length) { throw new ArrayIndexOutOfBoundsException("Received invalid palette index!"); }
		this.palettes[index] = palette | 0xFF000000;
	}

	public void setPattern(int index, byte[] data)
	{
		if(index * 64 >= patternData.length) { throw new ArrayIndexOutOfBoundsException("Received invalid pattern index!"); }
		if(data == null || data.length != 64) { throw new IllegalArgumentException("Pattern size is not 64!"); }
		System.arraycopy(data, 0, patternData, index * 64, data.length);
	}

	public static short createCharacterCommand(int offset, boolean transparent, int rotation, boolean isUpsideDown, boolean isRightsideLeft, int patternNo)
	{
		if (offset < 0 || offset > 7) { throw new IllegalArgumentException("Offset must be between 0 and 7"); }
		if (rotation < 0 || rotation > 3) { throw new IllegalArgumentException("Rotation must be between 0 and 3"); }
		if (patternNo < 0 || patternNo > 255) { throw new IllegalArgumentException("Pattern number must be between 0 and 255"); }

		int cmd = (offset & 0x07) << 13;
		if (transparent) { cmd |= 0x1000; }
		cmd |= (rotation & 0x03) << 10;
		if (isUpsideDown) { cmd |= 0x200; }
		if (isRightsideLeft) { cmd |= 0x100; }
		cmd |= patternNo & 0xFF;

		return (short) cmd;
	}

	public void drawBackground(short command, short x, short y)
	{
		if(Mobile.getDisplay().getCurrent() != this) { return; }

		command = (short) (command & 0xFFFF);
		int patternNo = command & 0xFF;
		int patternBase = patternNo * 64;

		if (patternBase >= patternData.length) { throw new IllegalArgumentException("Pattern index out of bounds: " + patternNo); }

		int offset = (command >> 13) & 0x7;
		int rotation = (command >> 10) & 0x3;
		boolean transparent = (command & 0x1000) != 0;
		boolean isUpsideDown = (command & 0x200) != 0;
		boolean isRightsideLeft = (command & 0x100) != 0;

		for (int y1 = 0; y1 < 8; y1++)
		{
			for (int x1 = 0; x1 < 8; x1++)
			{
				int sx = isRightsideLeft ? (7 - x1) : x1;
				int sy = isUpsideDown ? (7 - y1) : y1;

				int rx, ry;
				switch (rotation)
				{
					case 1:  rx = 7 - sy; ry = sx; break;
					case 2:  rx = 7 - sx; ry = 7 - sy; break;
					case 3:  rx = sy;     ry = 7 - sx; break;
					default: rx = sx;     ry = sy; break;
				}

				int colorId = patternData[patternBase + (y1 * 8 + x1)] & 0xFF;

				pixels[ry * 8 + rx] = palettes[(colorId + (offset * 32)) & 0xFF];
			}
		}
		// Draws directly onto the virtual screen (back buffer)
		graphics.drawRGB(pixels, 0, 8, x * 8, y * 8, 8, 8, true);
	}

	public void drawSpriteChar(short command, short x, short y)
	{
		if (fbGraphics == null) { throw new IllegalStateException("Frame buffer has not been created yet!"); }

		command = (short) (command & 0xFFFF);
		int patternNo = command & 0xFF;
		int patternBase = patternNo * 64;

		if (patternBase >= patternData.length) { throw new IllegalArgumentException("Pattern index out of bounds: " + patternNo); }

		int offset = (command >> 13) & 0x7;
		int rotation = (command >> 10) & 0x3;
		boolean transparent = (command & 0x1000) != 0;
		boolean isUpsideDown = (command & 0x200) != 0;
		boolean isRightsideLeft = (command & 0x100) != 0;

		for (int y1 = 0; y1 < 8; y1++)
		{
			for (int x1 = 0; x1 < 8; x1++)
			{
				int sx = isRightsideLeft ? (7 - x1) : x1;
				int sy = isUpsideDown ? (7 - y1) : y1;

				int rx, ry;
				switch (rotation)
				{
					case 1:  rx = 7 - sy; ry = sx; break;
					case 2:  rx = 7 - sx; ry = 7 - sy; break;
					case 3:  rx = sy;     ry = 7 - sx; break;
					default: rx = sx;     ry = sy; break;
				}

				int colorId = patternData[patternBase + (y1 * 8 + x1)] & 0xFF;

				// Transparency IS respected on color ID 0 for Sprites.
				if (transparent && colorId == 0) { pixels[ry * 8 + rx] = 0x00000000; }
				else { pixels[ry * 8 + rx] = palettes[(colorId + (offset * 32)) & 0xFF]; }
			}
		}
		// This one draws to the FrameBuffer
		fbGraphics.drawRGB(pixels, 0, 8, x, y, 8, 8, true);
	}

	public static int getVirtualHeight() { return Mobile.lcdHeight; }

	public static int getVirtualWidth() { return Mobile.lcdWidth; }
}
