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

	private static ArrayList<CharacterCommand> commands = new ArrayList<CharacterCommand>();
	private Image spriteImage;
	private Graphics spriteGraphics;
	private int[] palettes;
	private byte[] patternData;
	private int[] pixels;

	// The "virtual screen" here is just the lcd backbuffer that any Displayable gets when being created

	public SpriteCanvas(int numPalettes, int numPatterns)
    {
		super();
		this.palettes = new int[numPalettes];
		this.patternData = new byte[numPatterns * 64];
		this.pixels = new int[64];
	}

	public void createFrameBuffer(int fw, int fh)
    {
		if(spriteImage != null) { throw new IllegalStateException("FrameBuffer already exists!"); }
		if(fw > getVirtualWidth() || fh > getVirtualHeight()) { throw new IllegalArgumentException("Size is larger than the screen"); }
		Mobile.log(Mobile.LOG_DEBUG, SpriteCanvas.class.getPackage().getName() + "." + SpriteCanvas.class.getSimpleName() + ": " + "creating FrameBuffer: " + fw + " " + fh);

		spriteImage = Image.createImage(fw, fh, 0);
		spriteGraphics = (Graphics) spriteImage.getGraphics();
	}

	public void disposeFrameBuffer() { spriteGraphics = null; spriteImage = null; }

    // Copies from the virtual screen (back buffer) to the currently set FrameBuffer
	public void copyArea(int sx, int sy, int fw, int fh, int tx, int ty)
    {
		if(sx < 0 || sy < 0 || fw > getVirtualWidth() || fh > getVirtualHeight()) { throw new IllegalArgumentException("Invalid position and/or size received"); }
		if(spriteImage == null) { throw new IllegalStateException("FrameBuffer is not ready!"); }
		Mobile.log(Mobile.LOG_DEBUG, SpriteCanvas.class.getPackage().getName() + "." + SpriteCanvas.class.getSimpleName() + ": " + "copyArea: " + sx + " " + sy + " " + fw + " " + fh + " " + tx + " " + ty);
        if(Mobile.getDisplay().getCurrent() != this) { return; }
		graphics.copyToFrameBuffer(spriteImage, sx, sy, fw, fh, tx, ty, 0);
    }

	// TODO: This should copy from an area of the virtual screen into another to allow features like scrolling, but is untested
    public void copyFullScreen(int tx, int ty)
    {
		if(spriteImage == null) { throw new IllegalStateException("FrameBuffer is not ready!"); }
		Mobile.log(Mobile.LOG_WARNING, SpriteCanvas.class.getPackage().getName() + "." + SpriteCanvas.class.getSimpleName() + ": " + "copyFullscreen (Untested): " + tx + " " + ty);
        if(Mobile.getDisplay().getCurrent() != this) { return; }
        graphics.copyArea(0, 0, getVirtualWidth(), getVirtualHeight(), tx, ty);
    }

	public void drawFrameBuffer(final int tx, final int ty)
    {
		if(spriteImage == null) { throw new IllegalStateException("FrameBuffer is not ready!"); }
		Mobile.log(Mobile.LOG_DEBUG, SpriteCanvas.class.getPackage().getName() + "." + SpriteCanvas.class.getSimpleName() + ": " + "drawFrameBuffer: " + tx + " " + ty);
        if(Mobile.getDisplay().getCurrent() != this) { return; }

		// Effectively calls the repaint override below
		repaint();
	}

	/*
	 * This completely overrides lcdui.Canvas' repaint() call just to make sure the sprite FrameBuffer is always drawn AFTER the virtual screen/backbuffer.
	 * Since the documentation states that the backbuffer can be copied to the sprite frameBuffer but never the other way around, this should be safe
	 */
	public void repaint(final int x, final int y, final int width, final int height)
	{
		if (!isShown() || listCommands) { return; }

		if (!isShown() || listCommands) { return; }

		if(!Mobile.compatImmediateRepaints)
		{
			boolean postEvent = false;
			synchronized (paintLock)
			{
				if (!needsRepaint)
				{
					paintX = x;
					paintY = y;
					paintW = width;
					paintH = height;
					needsRepaint = true;
					postEvent = true;
				}
				else
				{
					// Unionize clipping bounds for consecutive repaints, so
					// we don't keep stacking draw operations on top of others.
					int x2 = Math.max(paintX + paintW, x + width);
					int y2 = Math.max(paintY + paintH, y + height);
					paintX = Math.min(paintX, x);
					paintY = Math.min(paintY, y);
					paintW = x2 - paintX;
					paintH = y2 - paintY;
				}
			}
			// Schedule the paint request for later processing
			if(postEvent)
			{
				Mobile.getDisplay().postPaintRequest(new Runnable()
				{
					@Override
					public void run()
					{
						repaintRequest();
						// draw sprite FB
						Mobile.getPlatform().flushGraphics(spriteImage, x, y, spriteImage.getWidth(), spriteImage.getHeight());
					}
				});
			}
		}
		else // Immediately process the paint event
		{
			synchronized (paintLock)
			{
				paintX = x;
				paintY = y;
				paintW = width;
				paintH = height;
				needsRepaint = true;
			}
			repaintRequest();
			// draw sprite FB in immediate repaints mode
			Mobile.getPlatform().flushGraphics(spriteImage, x, y, spriteImage.getWidth(), spriteImage.getHeight());
		}
	}

	public void setPalette(int index, int palette)
	{
		if(index > palettes.length) { throw new ArrayIndexOutOfBoundsException("Received invalid palette index!"); }
		this.palettes[index] = palette | 0xFF000000;
	}

	public void setPattern(int index, byte[] data)
	{
		if(index * 64 > patternData.length) { throw new ArrayIndexOutOfBoundsException("Received invalid pattern index!"); }
		if(data.length != 64) { throw new IllegalArgumentException("Pattern size is not 64!"); }
		System.arraycopy(data, 0, patternData, index * 64, data.length);
	}

	public static short createCharacterCommand(int offset, boolean transparent, int rotation, boolean isUpsideDown, boolean isRightsideLeft, int patternNo)
    {
		int cmd = (offset & 0x7) << 13;

		if (transparent) { cmd |= 0x1000; }
		cmd |= (rotation & 0x3) << 10;
		if (isUpsideDown) { cmd |= 0x200; }
		if (isRightsideLeft) { cmd |= 0x100; }
		cmd |= patternNo & 0xFF;

		return (short) cmd;
	}

    public void drawBackground(short command, short x, short y)
    {
		Mobile.log(Mobile.LOG_DEBUG, SpriteCanvas.class.getPackage().getName() + "." + SpriteCanvas.class.getSimpleName() + ": " + "drawBackground: " + command + " " + x + " " + y);
        if(Mobile.getDisplay().getCurrent() != this) { return; }

        command = (short) (command & 0xFFFF);
		int offset = (command >> 13) & 0x7;
		int rotation = (command >> 10) & 0x3;
		boolean transparent = (command & 0x1000) != 0;
		boolean isUpsideDown = (command & 0x200) != 0;
		boolean isRightsideLeft = (command & 0x100) != 0;
		int patternNo = command & 0xff;

		for (int x1 = 0; x1 < 8; x1++)
        {
			for (int y1 = 0; y1 < 8; y1++)
            {
				int cmd = (isUpsideDown ? 7 - y1 : y1) * 8 + (isRightsideLeft ? 7 - x1 : x1);
				int colorId = patternData[patternNo * 64 + cmd] & 0xFF;

				if (rotation == 1) { cmd = (7 - y1) + x1 * 8; } // 90 degrees
                else if (rotation == 2) { cmd = (7 - y1) * 8 + (7 - x1); } // 180 degrees
                else if (rotation == 3) { cmd = y1 + (7 - x1) * 8; } // 270 degrees
                else { cmd = y1 * 8 + x1; } // No rotation
				pixels[cmd] = transparent && colorId == 0 ? 0xFF000000
						: (palettes[(colorId + (offset * 32)) & 0xFF]);
			}
		}
		// Draws directly onto the screen
		graphics.drawRGB(pixels, 0, 8, x, y, 8, 8, true);
    }

	public void drawSpriteChar(short command, short x, short y)
    {
		Mobile.log(Mobile.LOG_DEBUG, SpriteCanvas.class.getPackage().getName() + "." + SpriteCanvas.class.getSimpleName() + ": " + "drawSpriteChar: " + command + " " + x + " " + y);
        if(Mobile.getDisplay().getCurrent() != this) { return; }

		command = (short) (command & 0xFFFF);
		int offset = (command >> 13) & 0x7;
		int rotation = (command >> 10) & 0x3;
		boolean transparent = (command & 0x1000) != 0;
		boolean isUpsideDown = (command & 0x200) != 0;
		boolean isRightsideLeft = (command & 0x100) != 0;
		int patternNo = command & 0xff;

		for (int x1 = 0; x1 < 8; x1++)
        {
			for (int y1 = 0; y1 < 8; y1++)
            {
				int cmd = (isUpsideDown ? 7 - y1 : y1) * 8 + (isRightsideLeft ? 7 - x1 : x1);
				int colorId = patternData[patternNo * 64 + cmd] & 0xFF;

				if (rotation == 1) { cmd = (7 - y1) + x1 * 8; } // 90 degrees
                else if (rotation == 2) { cmd = (7 - y1) * 8 + (7 - x1); } // 180 degrees
                else if (rotation == 3) { cmd = y1 + (7 - x1) * 8; } // 270 degrees
                else { cmd = y1 * 8 + x1; } // No rotation
				pixels[cmd] = transparent && colorId == 0 ? 0x00000000
						: (palettes[(colorId + (offset * 32)) & 0xFF]);
			}
		}
		// This one draws to the FrameBuffer
		spriteGraphics.drawRGB(pixels, 0, 8, x, y, 8, 8, true);
	}

    public static int getVirtualHeight() { return Mobile.lcdHeight; }

    public static int getVirtualWidth() { return Mobile.lcdWidth; }

	private static class CharacterCommand
    {
		int offset;
		boolean transparent;
		int rotation;
		boolean isUpsideDown;
		boolean isRightsideLeft;
		int patternNo;
	}
}
