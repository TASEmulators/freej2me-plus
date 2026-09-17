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

	private int fbTx, fbTy;

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
		if(fbImage != null) { throw new IllegalStateException("FrameBuffer already exists!"); }
		if(fw > getVirtualWidth() || fh > getVirtualHeight()) { throw new IllegalArgumentException("Size is larger than the screen"); }
		Mobile.log(Mobile.LOG_DEBUG, SpriteCanvas.class.getPackage().getName() + "." + SpriteCanvas.class.getSimpleName() + ": " + "creating FrameBuffer: " + fw + " " + fh);

		fbImage = Image.createImage(fw, fh);
		fbGraphics = (Graphics) fbImage.getGraphics();
	}

	public void disposeFrameBuffer() { fbGraphics = null; fbImage = null; }

	// Copies from the virtual screen (back buffer) to the currently set FrameBuffer
	public void copyArea(int sx, int sy, int fw, int fh, int tx, int ty)
	{
		if(fbImage == null) { throw new IllegalStateException("FrameBuffer is not ready!"); }
		if(sx < 0 || sy < 0 || fw > fbImage.getWidth() || fh > fbImage.getHeight()) { throw new IllegalArgumentException("Invalid position and/or size received"); }

		Mobile.log(Mobile.LOG_DEBUG, SpriteCanvas.class.getPackage().getName() + "." + SpriteCanvas.class.getSimpleName() + ": " + "copyArea: " + sx + " " + sy + " " + fw + " " + fh + " " + tx + " " + ty);
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
		Mobile.log(Mobile.LOG_WARNING, SpriteCanvas.class.getPackage().getName() + "." + SpriteCanvas.class.getSimpleName() + ": " + "copyFullscreen (Untested): " + tx + " " + ty);
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
		Mobile.log(Mobile.LOG_DEBUG, SpriteCanvas.class.getPackage().getName() + "." + SpriteCanvas.class.getSimpleName() + ": " + "drawFrameBuffer: " + tx + " " + ty);
		if(Mobile.getDisplay().getCurrent() != this) { return; }

		fbTx = tx;
		fbTy = ty;
		// Effectively calls the repaintRequest override below, as this draws
		// to the real screen.
		repaint();
	}

	/*
	 * This completely overrides lcdui.Canvas' repaintRequest() call just to make sure the sprite FrameBuffer is always drawn AFTER the virtual screen/backbuffer.
	 * Since the documentation states that the backbuffer can be copied to the sprite frameBuffer but never the other way around, this should be safe
	 */
	public void repaintRequest()
	{
		// These can be called from another thread, at any time (even if the canvas is no longer visible),
		// so ignore any repaints in cases where it isn't visible
		if (!isShown() || listCommands) { return; }

		int renderX, renderY, renderW, renderH;

		synchronized (paintLock)
		{
			if (!needsRepaint)
			{
				paintLock.notifyAll();
				return;
			}
			renderX = paintX;
			renderY = paintY;
			renderW = paintW;
			renderH = paintH;
			needsRepaint = false;
		}

		try
		{
			graphics.reset(renderX, renderY, renderW, renderH);
			paint(graphics);
		}
		catch(NullPointerException npe)
		{
			Mobile.log(Mobile.LOG_ERROR, Canvas.class.getPackage().getName() + "." + Canvas.class.getSimpleName() + ": " + "Null Pointer Exception in draw event: " + npe.getMessage());
			npe.printStackTrace();
			//throw new NullPointerException("Null Pointer Exception in draw event");
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, Canvas.class.getPackage().getName() + "." + Canvas.class.getSimpleName() + ": " + "Serious Exception hit in repaint(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			// Unblock any threads waiting in serviceRepaints()
			synchronized (paintLock)
			{
				paintLock.notifyAll();
			}
		}

		if (!fullscreen && !commands.isEmpty())
		{
			Mobile.getPlatform().setPostFlushDraw(new Runnable()
			{
				@Override
				public void run()
				{
					// Draw FrameBuffer right before the commands bar.
					Mobile.getPlatform().getLcdFrontbufferGraphics().drawImage(fbImage, fbTx, fbTy);
					paintCommandsBar();
				}
			});
		}
		Mobile.getPlatform().flushGraphics(platformImage, renderX, renderY, renderW, renderH);
	}

	public void setPalette(int index, int palette)
	{
		if(index >= palettes.length) { throw new ArrayIndexOutOfBoundsException("Received invalid palette index!"); }
		this.palettes[index] = palette | 0xFF000000;
	}

	public void setPattern(int index, byte[] data)
	{
		if(index * 64 >= patternData.length) { throw new ArrayIndexOutOfBoundsException("Received invalid pattern index!"); }
		if(data.length != 64) { throw new IllegalArgumentException("Pattern size is not 64!"); }
		System.arraycopy(data, 0, patternData, index * 64, data.length);
	}

	public static short createCharacterCommand(int offset, boolean transparent, int rotation, boolean isUpsideDown, boolean isRightsideLeft, int patternNo)
	{
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
		Mobile.log(Mobile.LOG_DEBUG, SpriteCanvas.class.getPackage().getName() + "." + SpriteCanvas.class.getSimpleName() + ": " + "drawBackground: " + command + " " + x + " " + y);
		if(Mobile.getDisplay().getCurrent() != this) { return; }

		command = (short) (command & 0xFFFF);
		int offset = (command >> 13) & 0x7;
		int rotation = (command >> 10) & 0x3;
		boolean transparent = (command & 0x1000) != 0;
		boolean isUpsideDown = (command & 0x200) != 0;
		boolean isRightsideLeft = (command & 0x100) != 0;
		int patternNo = command & 0xFF;

		int patternBase = patternNo * 64;

		for (int x1 = 0; x1 < 8; x1++)
		{
			for (int y1 = 0; y1 < 8; y1++)
			{
				int colorId = patternData[patternBase + (y1 * 8 + x1)] & 0xFF;

				int rx, ry;
				switch (rotation)
				{
					case 1:  rx = 7 - y1; ry = x1; break;
					case 2:  rx = 7 - x1; ry = 7 - y1; break;
					case 3:  rx = y1;     ry = 7 - x1; break;
					default: rx = x1;     ry = y1; break;
				}

				if (isUpsideDown) { ry = 7 - ry; }
				if (isRightsideLeft) { rx = 7 - rx; }

				int destIdx = ry * 8 + rx;

				// Backgrounds ignore transparency entirely.
				pixels[destIdx] = palettes[(colorId + (offset * 32)) & 0xFF];
			}
		}
		// Draws directly onto the virtual screen (back buffer)
		graphics.drawRGB(pixels, 0, 8, x * 8, y * 8, 8, 8, true);
	}

	public void drawSpriteChar(short command, short x, short y)
	{
		Mobile.log(Mobile.LOG_DEBUG, SpriteCanvas.class.getPackage().getName() + "." + SpriteCanvas.class.getSimpleName() + ": " + "drawSpriteChar: " + command + " " + x + " " + y);

		command = (short) (command & 0xFFFF);
		int offset = (command >> 13) & 0x7;
		int rotation = (command >> 10) & 0x3;
		boolean transparent = (command & 0x1000) != 0;
		boolean isUpsideDown = (command & 0x200) != 0;
		boolean isRightsideLeft = (command & 0x100) != 0;
		int patternNo = command & 0xFF;

		int patternBase = patternNo * 64;

		for (int x1 = 0; x1 < 8; x1++)
		{
			for (int y1 = 0; y1 < 8; y1++)
			{
				int colorId = patternData[patternBase + (y1 * 8 + x1)] & 0xFF;

				int rx, ry;
				switch (rotation)
				{
					case 1:  rx = 7 - y1; ry = x1; break;
					case 2:  rx = 7 - x1; ry = 7 - y1; break;
					case 3:  rx = y1;     ry = 7 - x1; break;
					default: rx = x1;     ry = y1; break;
				}

				if (isUpsideDown) { ry = 7 - ry; }
				if (isRightsideLeft) { rx = 7 - rx; }

				int destIdx = ry * 8 + rx;

				// Transparency IS respected on the color ID 0 for Sprites.
				if (transparent && colorId == 0) { pixels[destIdx] = 0x00000000; }
				else { pixels[destIdx] = palettes[(colorId + (offset * 32)) & 0xFF]; }
			}
		}
		// This one draws to the FrameBuffer
		fbGraphics.drawRGB(pixels, 0, 8, x, y, 8, 8, true);
	}

	public static int getVirtualHeight() { return Mobile.lcdHeight; }

	public static int getVirtualWidth() { return Mobile.lcdWidth; }
}
