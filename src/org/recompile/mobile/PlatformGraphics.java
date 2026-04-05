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
package org.recompile.mobile;

import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.game.Sprite;

import com.nokia.mid.ui.DirectGraphics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import com.nttdocomo.ui.UIException;

public abstract class PlatformGraphics implements DirectGraphics, com.nttdocomo.opt.ui.j3d.Graphics3D
{

	// Gaussian blur kernel (7x7) for Motorola's FunLights
	protected static final byte[] gaussianKernel = 
	{
		1,  2,  3,  2,  1, 0, 0,
		2,  5,  8,  5,  2, 0, 0,
		3,  8, 12,  8,  3, 0, 0,
		2,  5,  8,  5,  2, 0, 0,
		1,  2,  3,  2,  1, 0, 0,
		0,  0,  0,  0,  0, 0, 0,
		0,  0,  0,  0,  0, 0, 0
	};
	
	public static final int BASELINE = 64;
	public static final int BOTTOM   = 32;
	public static final int DOTTED   = 1;
	public static final int HCENTER  = 1;
	public static final int LEFT     = 4;
	public static final int RIGHT    = 8;
	public static final int SOLID    = 0;
	public static final int TOP      = 16;
	public static final int VCENTER  = 2;

	/* 
	 * DirectGraphics rotations are counter-clockwise compared to MIDP's clockwise, flipping
	 * an image horizontally is done by multiplying its height or width scale
	 * by -1 respectively. Flipping vertically is the same as flipping horizontally, 
	 * and then rotating by 180 degrees.
	 */
	private static final short HV    = DirectGraphics.FLIP_HORIZONTAL | DirectGraphics.FLIP_VERTICAL;
	private static final short HV90  = DirectGraphics.FLIP_HORIZONTAL | DirectGraphics.FLIP_VERTICAL | DirectGraphics.ROTATE_90;
	private static final short HV180 = DirectGraphics.FLIP_HORIZONTAL | DirectGraphics.FLIP_VERTICAL | DirectGraphics.ROTATE_180;
	private static final short HV270 = DirectGraphics.FLIP_HORIZONTAL | DirectGraphics.FLIP_VERTICAL | DirectGraphics.ROTATE_270;
	private static final short H90   = DirectGraphics.FLIP_HORIZONTAL | DirectGraphics.ROTATE_90;
	private static final short H180  = DirectGraphics.FLIP_HORIZONTAL | DirectGraphics.ROTATE_180;
	private static final short H270  = DirectGraphics.FLIP_HORIZONTAL | DirectGraphics.ROTATE_270;
	private static final short V90   = DirectGraphics.FLIP_VERTICAL | DirectGraphics.ROTATE_90;
	private static final short V180  = DirectGraphics.FLIP_VERTICAL | DirectGraphics.ROTATE_180;
	private static final short V270  = DirectGraphics.FLIP_VERTICAL | DirectGraphics.ROTATE_270;

	/* 
	 * DoJa Constants
	 */

	// Colors
	public static final int BLACK   = 0;    // (0x00, 0x00, 0x00)
	public static final int BLUE    = 1;    // (0x00, 0x00, 0xff)
	public static final int LIME    = 2;    // (0x00, 0xff, 0x00)
	public static final int AQUA    = 3;    // (0x00, 0xff, 0xff)
	public static final int RED     = 4;    // (0xff, 0x00, 0x00)
	public static final int FUCHSIA = 5;    // (0xff, 0x00, 0xff)
	public static final int YELLOW  = 6;    // (0xff, 0xff, 0x00)
	public static final int WHITE   = 7;    // (0xff, 0xff, 0xff)
	public static final int GRAY    = 8;    // (0x80, 0x80, 0x80)
	public static final int NAVY    = 9;    // (0x00, 0x00, 0x80)
	public static final int GREEN   = 10;   // (0x00, 0x80, 0x00)
	public static final int TEAL    = 11;   // (0x00, 0x80, 0x80)
	public static final int MAROON  = 12;   // (0x80, 0x00, 0x00)
	public static final int PURPLE  = 13;   // (0x80, 0x00, 0x80)
	public static final int OLIVE   = 14;   // (0x80, 0x80, 0x00)
	public static final int SILVER  = 15;   // (0xc0, 0xc0, 0xc0)

	// flip modes
	public static final int FLIP_NONE = 0;
	public static final int FLIP_HORIZONTAL = 1;
	public static final int FLIP_VERTICAL = 2;
	public static final int FLIP_ROTATE = 3;
	public static final int FLIP_ROTATE_LEFT = 4;
	public static final int FLIP_ROTATE_RIGHT = 5;
	public static final int FLIP_ROTATE_RIGHT_HORIZONTAL = 6;
	public static final int FLIP_ROTATE_RIGHT_VERTICAL = 7;

	// com.nttdocomo.opt.ui.Graphics2 variables
	protected int renderMode = com.nttdocomo.opt.ui.Graphics2.OP_REPL;
	protected int srcRatio = 255, dstRatio = 255;

	// FPS Counter variables
	private static int frameCount = 0;
	private static long lastFpsTime = System.nanoTime();
	private static int fps = 0;

	// Scale factor
	private static final int GAUSSIAN_SCALE_FACTOR = 159;

	// Graphics context variables
	protected BufferedImage canvas;
	protected Graphics2D gc;
	protected int canvasWidth;
	protected int canvasHeight;
	protected int[] canvasData;
	protected PlatformImage baseImage;
	protected boolean fastBlit;

	protected int translateX = 0;
	protected int translateY = 0;

	protected int resetTransX = 0;
	protected int resetTransY = 0;
	private boolean firstReset = true;

	protected int color = 0xFF000000;
	protected Font font = Font.getDefaultFont();
	protected com.nttdocomo.ui.Font dojaFont = com.nttdocomo.ui.Font.getDefaultFont();
	protected int strokeStyle = SOLID;

	protected int dojaLockCount = 0;
	protected int dojaflipMode = 0;
	protected boolean usePictoColor = false;
	protected boolean contextDisposed = false;

	private static final String fastForwardIndicator = "⮞⮞";
	private static final String pauseIndicator = "PAUSED!";

	private static final Font HUDFont = new Font(Font.FACE_MONOSPACE, Font.STYLE_BOLD, Font.SIZE_LARGE);

	public PlatformGraphics(PlatformImage image)
	{
		this.baseImage = image;
		canvas = image.getCanvas();
		gc = canvas.createGraphics();

		canvasWidth = canvas.getWidth();
		canvasHeight = canvas.getHeight();

		canvasData = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();

		setClip(0, 0, canvasWidth, canvasHeight);
		gc.setFont(font.awtFont);
		setColor(color);

		gc.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	}

	public void reset() // Internal use method, resets the Graphics object to its inital values
	{
		reset(0, 0, canvasWidth, canvasHeight);
	}
	
	public void reset(int clipx, int clipy, int clipw, int cliph) // Internal use method, resets the Graphics object to its inital values
	{
		if(firstReset) // Save the translation state prior to the very first graphics reset, so it can be restored later (Jars may use this to set their fixed drawing position)
		{
			resetTransX = getTranslateX();
			resetTransY = getTranslateY();
			firstReset = false;
		}
		if(!Mobile.compatTranslateToOriginOnReset) { setOrigin(resetTransX, resetTransY); }
		else { setOrigin(0, 0); }

		setClip(clipx, clipy, clipw, cliph);
		setColor(0,0,0);
		setFont(Font.getDefaultFont());
		setStrokeStyle(SOLID);
	}

	public Graphics2D getGraphics2D() { return gc; }

	public BufferedImage getCanvas() { return canvas; }

	public int[] getFrameBuffer() { return canvasData; }

	public void clearRect(int x, int y, int width, int height)
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		int tmpcolor = color;
		setColor(0xFF000000); // Clear to default background color
		fillRect(x, y, width, height);
		setColor(tmpcolor);
	}

	public void copyArea(int x_src, int y_src, int width, int height, int x_dest, int y_dest, int anchor) 
	{
		x_src += getTranslateX();
		y_src += getTranslateY();

		x_dest = AnchorX(x_dest, width, anchor);
		y_dest = AnchorY(y_dest, height, anchor);

		// Check if the source area is within bounds before doing any draw operations
		if (x_src < 0 || y_src < 0 || 
			x_src + width > canvasWidth || 
			y_src + height > canvasHeight) {
			throw new IllegalArgumentException("Source area exceeds the bounds of the graphics object.");
		}

			/* 
			 * A neat trick here is that we don't need to check for types, as the copied
			 * subregion will always have the same data type as the original canvas it
			 * was copied from, be it INT_RGB, INT_ARGB, etc.
			 */
		final int[] subPixels = new int[width * height];

		for (int j = 0; j < height; j++) 
		{
			for (int i = 0; i < width; i++) 
			{
				subPixels[j * width + i] = canvasData[(y_src + j) * canvas.getWidth() + (x_src + i)];
			}
		}
	
		for (int j = 0; j < height; j++) 
		{
			for (int i = 0; i < width; i++) 
			{
				// The image data CAN go out of the destination bounds, we just can't draw it whenever it does.
				if (x_dest + i >= 0 && y_dest + j >= 0 && 
					x_dest + i < canvas.getWidth() && 
					y_dest + j < canvas.getHeight()) 
				{
					canvasData[(y_dest + j) * canvas.getWidth() + (x_dest + i)] = subPixels[j * width + i];
				}
			}
		}
	}

	// Basically same as copyArea, but copies from one image to another, instead of operating on the same image
	public void copyToFrameBuffer(BufferedImage frameBuffer, int x_src, int y_src, int width, int height, int x_dest, int y_dest, int anchor) 
	{
		if (frameBuffer == null) { return; }

		x_dest = AnchorX(x_dest, width, anchor);
		y_dest = AnchorY(y_dest, height, anchor);

		x_src += getTranslateX();
		y_src += getTranslateY();

		if (x_src < 0 || y_src < 0 || 
			x_src + width > canvasWidth || 
			y_src + height > canvasHeight) {
			throw new IllegalArgumentException("Source area exceeds the bounds of the graphics object.");
		}

		final int[] fbPixels = ((DataBufferInt) frameBuffer.getRaster().getDataBuffer()).getData();

		final int[] subPixels = new int[width * height];

		for (int j = 0; j < height; j++) 
		{
			for (int i = 0; i < width; i++) 
			{
				subPixels[j * width + i] = canvasData[(y_src + j) * canvas.getWidth() + (x_src + i)];
			}
		}

		for (int j = 0; j < height; j++) 
		{
			for (int i = 0; i < width; i++) 
			{
				// The image data CAN go out of the destination bounds, we just can't draw it whenever it does.
				if (x_dest + i >= 0 && y_dest + j >= 0 && 
					x_dest + i < frameBuffer.getWidth() && 
					y_dest + j < frameBuffer.getHeight()) 
				{
					fbPixels[(y_dest + j) * frameBuffer.getWidth() + (x_dest + i)] = subPixels[j * width + i];
				}
			}
		}
	}

	public void copyToFrameBuffer(Image frameBuffer, int x_src, int y_src, int width, int height, int x_dest, int y_dest, int anchor) 
	{
		copyToFrameBuffer(frameBuffer.getCanvas(), x_src, y_src, width, height, x_dest, y_dest, anchor);
	}

	public void copyToFrameBuffer(com.nttdocomo.ui.Image frameBuffer, int x_src, int y_src, int width, int height, int x_dest, int y_dest, int anchor) 
	{
		copyToFrameBuffer(frameBuffer.getCanvas(), x_src, y_src, width, height, x_dest, y_dest, anchor);
	}

	public void drawChar(char character, int x, int y, int anchor)
	{
		drawString(Character.toString(character), x, y, anchor);
	}

	public void drawChars(char[] data, int offset, int length, int x, int y, int anchor)
	{
		char[] str = new char[length];
		for(int i=offset; i<offset+length; i++)
		{
			if(i>=0 && i<data.length)
			{
				str[i-offset] = data[i];
			}
		}	
		drawString(new String(str), x, y, anchor);
	}

	public void drawImage(Image image, int x, int y, int anchor)
	{
		try
		{
			x = AnchorX(x, image.getWidth(), anchor);
			y = AnchorY(y, image.getHeight(), anchor);

			drawRGB(image.getDataBuffer(), 0, image.getWidth(), x, y, image.getWidth(), image.getHeight(), true);
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "drawImage :"+e.getMessage());
		}
	}

	public void drawImage(Image image, int x, int y)
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }
		
		drawImage(image, x, y, 0);
	}

	public void flushGraphics(PlatformImage image, int x, int y, int width, int height)
	{
		// called by MobilePlatform.flushGraphics/repaint

		try
		{
			fastBlit = (/*!Mobile.renderLCDMask || */ Mobile.maskIndex == 0) && !Mobile.funLightsEnabled;
			
			if(fastBlit && image.getDataBuffer() == canvasData) 
			{ 
				if(!MobilePlatform.showFPS.equals("Off")) { showFPS(); }
				return; // No need to copy anything, they're already the same
			} 
			if(fastBlit && x == 0 && y == 0 && width == canvasWidth && height == canvasHeight) 
			{ 
				/* 
				 * If the area to be drawn is the whole canvas, and no special treatment
				 * has to be done to the image, we can copy the whole image data into the FrontBuffer
				 * at once and return early. 
				 * 
				 * The canvas is always positive-sized and positioned at (0,0), so we don't even 
				 * need to do any of the checks below.
				 */
				System.arraycopy(image.getDataBuffer(), 0, canvasData, 0, canvasWidth*canvasHeight);
				if(!MobilePlatform.showFPS.equals("Off")) { showFPS(); }
				return; 
			}

			/* 
			 * We don't need to check for clipping or translation here, the frontBuffer 
			 * is always at (0,0) and has a clip region equal to the canvas dimensions.
			 * 
			 * A simple check against the image bounds is enough
			 */ 
			if(x < 0) { x = 0; }
			if(y < 0) { y = 0; }
			if(width + x > canvasWidth)   { width = canvasWidth - x; }
			if(height + y > canvasHeight) { height = canvasHeight - y; }

			int[] overlayData = null;

			// This one is rather costly, as it has to draw overlays on the corners of the screen with gaussian filtering applied.
			if(Mobile.funLightsEnabled)
			{
				overlayData = new int[width * height];
				drawFunLights(overlayData, width, height);
			}
		
			int destRowIndex, srcRowIndex, i, j;
			// Render the resulting image
			for (j = y; j < y + height; j++) 
			{
				// If there's no masking or overlay needed, we can copy a whole row at once, which is faster
				if(fastBlit)
				{
					destRowIndex = j * canvasWidth + x;
					srcRowIndex = j * image.getWidth() + x;
					System.arraycopy(image.getDataBuffer(), srcRowIndex, canvasData, destRowIndex, width);
				}
				else
				{
					destRowIndex = j * canvasWidth;
					srcRowIndex = j * image.getWidth();
					
					for (i = x; i < x + width; i++) 
					{
						// Only apply the backlight mask if Display, nokia's DeviceControl, or others request it for backlight effects.
						canvasData[destRowIndex + i] = image.getDataBuffer()[srcRowIndex + i] & Mobile.lcdMaskColors[Mobile.maskIndex]; //(Mobile.renderLCDMask ? Mobile.lcdMaskColors[Mobile.maskIndex] : 0xFFFFFFFF);

						// If funLights overlay is requested by the game, apply its pixels to the screen area
						if(Mobile.funLightsEnabled) { canvasData[destRowIndex + i] = blendPixels(overlayData[srcRowIndex + i], canvasData[destRowIndex + i]); }
					}
				}
			}

			if(!MobilePlatform.showFPS.equals("Off")) { showFPS(); }
		}
		catch (Exception e)
		{
			// Games can try to render offscreen even at the correct resolution, so this makes more sense as a debug log
			Mobile.log(Mobile.LOG_DEBUG, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "flushGraphics A:"+e.getMessage());
		}
	}

	public void drawRegion(Image image, int subx, int suby, int subw, int subh, int transform, int x, int y, int anchor)
	{
		if(subw == 0 || subh == 0) { return; }

		if (image == null) { throw new NullPointerException("Source image cannot be null"); }

		if (subx < 0 || suby < 0 || subx + subw > image.getCanvas().getWidth() || suby + subh > image.getCanvas().getHeight()) 
		{
			throw new IllegalArgumentException("Source region is out of bounds");
		}

		if(Mobile.compatSiemensFriendlyDrawing) 
		{
			if(getTranslateX() < 0) { x -= getTranslateX(); }
			if(getTranslateY() < 0) { y -= getTranslateY(); }
		}

		try
		{	
			if(transform == 0)
			{
				x = AnchorX(x, subw, anchor);
				y = AnchorY(y, subh, anchor);				
				drawRGB(image.getDataBuffer(), subx + (suby * image.getWidth()), image.getWidth(), x, y, subw, subh, true);
			}
			else
			{
				PlatformImage sub = new PlatformImage(image, subx, suby, subw, subh, transform);
				x = AnchorX(x, sub.getWidth(), anchor);
				y = AnchorY(y, sub.getHeight(), anchor);
				drawRGB(sub.getDataBuffer(), 0, sub.getWidth(), x, y, sub.getWidth(), sub.getHeight(), true);
			}
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "drawRegion A (x:"+x+" y:"+y+" w:"+subw+" h:"+subh+"):"+e.getMessage());
		}
	}

	public void drawRegion(Image image, int subx, int suby, int subw, int subh, int transform, int x, int y, int width_dest, int height_dest, int anchor, int stretch_quality) 
	{
		if(subw == 0 || subh == 0) { return; }

		Mobile.log(Mobile.LOG_WARNING, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "drawRegion B is untested!");

		try
		{
			if(transform == 0)
			{
				x = AnchorX(x, subw, anchor);
				y = AnchorY(y, subh, anchor);
				gc.drawImage(image.getCanvas(), x, y, x + width_dest, y + height_dest, subx, suby, subx + subw, suby + subh, null);
			}
			else
			{
				PlatformImage sub = new PlatformImage(image, subx, suby, subw, subh, transform);
				x = AnchorX(x, sub.getWidth(), anchor);
				y = AnchorY(y, sub.getHeight(), anchor);
				gc.drawImage(sub.getCanvas(), x, y, x + width_dest, y + height_dest, subx, suby, subx + subw, suby + subh, null);
			}
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "drawRegion B failed:"+e.getMessage());
		}
	}

	public void drawRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height, boolean processAlpha) 
	{
		if(width == 0 || height == 0) { return; }
		if (rgbData == null) { throw new NullPointerException("RGB Data array is null"); }
		if (offset < 0 || offset >= rgbData.length) { throw new ArrayIndexOutOfBoundsException("Invalid offset for RGB Data"); }
	
		if (scanlength > 0) 
		{
			if (offset + scanlength * (height - 1) + width > rgbData.length) 
			{
				throw new ArrayIndexOutOfBoundsException("DrawRGB Area is out of bounds (len" + rgbData.length + " max" + (offset + scanlength * (height - 1) + width)  + " scanlength " + scanlength + " offset " + offset + ")");
			}
		} 
		else 
		{
			if (offset + width > rgbData.length || offset + scanlength * (height - 1) < 0) 
			{
				throw new ArrayIndexOutOfBoundsException("DrawRGB Area is out of bounds (scanlength " + scanlength + ")");
			}
		}
		
		x += translateX;
		y += translateY;
	
		final int clipX = (getClipX() + translateX < 0) ? 0 : (getClipX() + translateX);
		final int clipY = (getClipY() + translateY < 0) ? 0 : (getClipY() + translateY);
		final int clipWidth = (getClipWidth() + getClipX() + translateX > canvasWidth) ? canvasWidth : (getClipWidth() + getClipX() + translateX);
		final int clipHeight = (getClipHeight() + getClipY() + translateY > canvasHeight) ? canvasHeight : (getClipHeight() + getClipY() + translateY);

		if(y + height > clipHeight) { height = clipHeight - y; }
		if(x + width > clipWidth)   { width = clipWidth - x; }

		/* If width or height ended up as zero, we can exit early */
		if(width == 0 || height == 0) { return; }
		
		final int icache = (x > clipX) ? 0 : (clipX - x);
		final int jcache = (y > clipY) ? 0 : (clipY - y);

		int rowOffset, destRow, j, i;
		// The array's x and y positions start from either 0 or the first valid drawable position, as the offset is what dictates where the data should start being read from
		for (j = jcache; j < height; j++) // This ternary only runs once, so there's no need to cache
		{
			rowOffset = offset + (j * scanlength);
			destRow = (y + j) * canvasWidth;
	
			for (i = icache; i < width; i++)
			{
				if (!processAlpha || (rgbData[rowOffset + i] >> 24 & 0xFF) == 255) { canvasData[destRow + x + i] = rgbData[rowOffset + i] | 0xFF000000; } // Set pixel as fully opaque
				else { canvasData[destRow + x + i] = blendPixels(rgbData[rowOffset + i], canvasData[destRow + x + i]); } // Handle alpha blending
			}
		}
	}

	public void drawLine(int x1, int y1, int x2, int y2) 
	{ 
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		x1 += translateX;
		x2 += translateX;
		y1 += translateY;
		y2 += translateY;

		final int clipX = (getClipX() + translateX < 0) ? 0 : (getClipX() + translateX);
		final int clipY = (getClipY() + translateY < 0) ? 0 : (getClipY() + translateY);
		final int clipWidth = (getClipWidth() + getClipX() + translateX > canvasWidth) ? canvasWidth : (getClipWidth() + getClipX() + translateX);
		final int clipHeight = (getClipHeight() + getClipY() + translateY > canvasHeight) ? canvasHeight : (getClipHeight() + getClipY() + translateY);

		int dx = Math.abs(x2 - x1);
    	int dy = Math.abs(y2 - y1);

		// This is basically a slightly modified bresenham algorithm

		int sx = (x1 < x2) ? 1 : -1; // Step in x direction
		int sy = (y1 < y2) ? 1 : -1; // Step in y direction
		int err = dx - dy; // Error value

		int curPixel = 0; // Used only for DOTTED style lines
		while(true) 
		{
			// Paint the pixel if the stroke style is dotted and the current position matches, or if it's just plain solid
			if(x1 >= clipX && x1 < clipWidth && y1 >= clipY && y1 < clipHeight && 
			((strokeStyle == DOTTED && curPixel % 4 <= 1) || strokeStyle == SOLID)) 
			{
				if(!Mobile.isDoJa && getAlphaComponent() == 255) { canvasData[y1*canvasWidth+x1] = getColor(); }
				else 
				{ 
					canvasData[y1*canvasWidth+x1] = blendPixels(getColor(), canvasData[y1*canvasWidth+x1]);
				}
			}

			if (x1 == x2 && y1 == y2) { break; } // Line is now fully drawn, so jump out

			int err2 = err * 2;
			if (err2 > -dy) 
			{
				err -= dy; 
				x1 += sx;
			}
			if (err2 < dx) 
			{
				err += dx; 
				y1 += sy;
			}
			curPixel++;
		}
	}

	public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle)
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		// Java's coordinate system has positive angles moving counter-clockwise
		arcAngle = -arcAngle;
		startAngle = -startAngle;

		x += translateX;
		y += translateY;

		final int clipX = (getClipX() + translateX < 0) ? 0 : (getClipX() + translateX);
		final int clipY = (getClipY() + translateY < 0) ? 0 : (getClipY() + translateY);
		final int clipWidth = (getClipWidth() + getClipX() + translateX > canvasWidth) ? canvasWidth : (getClipWidth() + getClipX() + translateX);
		final int clipHeight = (getClipHeight() + getClipY() + translateY > canvasHeight) ? canvasHeight : (getClipHeight() + getClipY() + translateY);

		int curPixel = 0; // Used only for DOTTED style lines

		/* 
			* This works similarly to Bresenham's midpoint circle algorithm. "steps" dictates how many
			* iterations are used to draw the circle. A bigger value will result in the same pixels 
			* being hit more times (and wasted cycles since they'll be discarded later) but will
			* guarantee a perfectly filled outline, whereas a small value will result in gaps
			* appearing in the circle since less points will be sampled. The current value is
			* a good balance between filling all positions on all kinds of shapes while hitting as 
			* few pixels as possible.
			*/
		
		final float centerX = x + width / 2.0f;
		final float centerY = y + height / 2.0f;
		final float radiusX = width / 2.0f;
		final float radiusY = height / 2.0f;
		final float startAngleRad = fastToRadians(startAngle);
		final float endAngleRad = (fastToRadians(startAngle + arcAngle) - fastToRadians(startAngle));
		int steps = (int) (Math.abs(arcAngle * ((width + height) / 2) / 50.0f));
		
		int firstFillX = (int) Math.round((centerX) + radiusX * Math.cos(startAngleRad));
		int firstFillY = (int) Math.round((centerY) + radiusY * Math.sin(startAngleRad)); 
		int lastFillX = -1;
		int lastFillY = -1;

		if((firstFillX >= clipX && firstFillX < clipWidth && firstFillY >= clipY && firstFillY < clipHeight) && 
			((strokeStyle == DOTTED && curPixel % 4 <= 1) || strokeStyle == SOLID))
		{
			if(!Mobile.isDoJa && getAlphaComponent() == 255) { canvasData[(firstFillY * canvasWidth) + firstFillX] = getColor(); }
			else 
			{ 
				canvasData[(firstFillY * canvasWidth) + firstFillX] = blendPixels(getColor(), canvasData[(firstFillY * canvasWidth) + firstFillX]); 
			}
			curPixel++;
		}

		/* First pixel was already drawn, so start from step 1 */
		for (int i = 1; i < steps; i++) 
		{
			float angle = startAngleRad + (i * endAngleRad / steps);
			
			int fillX = (int) Math.round((centerX) + radiusX * Math.cos(angle));
			int fillY = (int) Math.round((centerY) + radiusY * Math.sin(angle));
			
			// Make sure we don't paint the same pixel more than once
			if((lastFillX == fillX && lastFillY == fillY) || (firstFillX == fillX && firstFillY == fillY)) { continue; }
			lastFillX = fillX;
			lastFillY = fillY;

			if((fillX >= clipX && fillX < clipWidth && fillY >= clipY && fillY < clipHeight) && 
			((strokeStyle == DOTTED && curPixel % 4 <= 1) || strokeStyle == SOLID))
			{
				if(!Mobile.isDoJa && getAlphaComponent() == 255) { canvasData[(fillY * canvasWidth) + fillX] = getColor(); }
				else 
				{ 
					canvasData[(fillY * canvasWidth) + fillX] = blendPixels(getColor(), canvasData[(fillY * canvasWidth) + fillX]); 
				}
			}
			curPixel++;
		}
	}

	public void drawRect(int x, int y, int width, int height)
	{
		if(width < 0 || height < 0) { return; }
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }
		
		width+=1;
		height+=1;
		x += translateX;
		y += translateY;

		final int clipX = (getClipX() + translateX < 0) ? 0 : (getClipX() + translateX);
		final int clipY = (getClipY() + translateY < 0) ? 0 : (getClipY() + translateY);
		final int clipWidth = (getClipWidth() + getClipX() + translateX > canvasWidth) ? canvasWidth : (getClipWidth() + getClipX() + translateX);
		final int clipHeight = (getClipHeight() + getClipY() + translateY > canvasHeight) ? canvasHeight : (getClipHeight() + getClipY() + translateY);
		
		for (int j = 0; j < height; j++) 
		{
			for (int i = 0; i < width;) 
			{
				// Paint the pixel if the border style is dotted and the current position matches, or if it's plain solid
				if((x+i) >= clipX && (y+j) >= clipY && (x+i) < clipWidth && (y+j) < clipHeight && 
				((strokeStyle == DOTTED && ((j == 0 && i % 4 <= 1) || (i == 0 && j % 4 <= 1) || 
				(j == height-1 && i % 4 <= 1) || (i == width-1 && j % 4 <= 1)))
				|| strokeStyle == SOLID))
				{
					if(!Mobile.isDoJa && getAlphaComponent() == 255) { canvasData[((y + j) * canvasWidth) + (x + i)] = getColor(); }
					else 
					{
						canvasData[((y + j) * canvasWidth) + (x + i)] = blendPixels(getColor(), canvasData[((y + j) * canvasWidth) + (x + i)]);
					}
				}
				
				// We must only draw borders, otherwise this becomes fillRect
				if(j == 0 || j == height-1 || width == 1) { i++; }
				else { i += width-1; }
			}
		}
	}

	public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight)
	{
		if(width < 0 || height < 0) { return; }
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		arcWidth = Math.abs(arcWidth);
		arcHeight = Math.abs(arcHeight);

		// We'll be doing only even arc widths and heights, otherwise the borders will look off (java's Graphics allow odd width/heights though)
		if(arcWidth  %2 != 0) { arcWidth++; }
		if(arcHeight %2 != 0) { arcHeight++; }

		if(arcWidth >= width) { arcWidth = width-1; }
		if(arcHeight >= height) { arcHeight = height-1; }
				
		// Fill the main rectangle area
		drawLine(x + (arcWidth/2)+1, y, x+width-(arcWidth/2)-2, y); // Top line
		drawLine(x + (arcWidth/2)+1, y+height, x+width-(arcWidth/2)-2, y+height); // Bottom line
		drawLine(x, y+(arcHeight/2)+1, x, y+height-(arcHeight/2)-2); // Left line
		drawLine(x+width, y+(arcHeight/2)+1, x+width, y+height-(arcHeight/2)-2); // Right line

		// Fill rounded corners
		drawArc(x, y, arcWidth, arcHeight, 90, 90); // Top-left corner
		drawArc(x + width - arcWidth - 1, y, arcWidth, arcHeight, 0, 90); // Top-right corner
		drawArc(x, y + height - arcHeight - 1, arcWidth, arcHeight, 180, 90); // Bottom-left corner
		drawArc(x + width - arcWidth - 1, y + height - arcHeight - 1, arcWidth, arcHeight, 270, 90); // Bottom-right corner
	}

	// Patch: Line break support (May affect other games)
	public void drawString(String str, int x, int y, int anchor)
	{
		if(str == null || str.length() == 0) { return; }
		if(str.indexOf('\n') < 0 && str.indexOf('\r') < 0)
		{
			drawStringSingleLine(str, x, y, anchor);
			return;
		}

		int lineHeight = 0;
		if(Mobile.isDoJa) { lineHeight = dojaFont.getHeight(); }
		else { lineHeight = font.getHeight(); }

		int lineStart = 0;
		int lineIndex = 0;
		for(int i = 0; i <= str.length(); i++)
		{
			boolean end = (i == str.length());
			char ch = end ? 0 : str.charAt(i);
			if(end || ch == '\n' || ch == '\r')
			{
				String line = str.substring(lineStart, i);
				drawStringSingleLine(line, x, y + (lineIndex * lineHeight), anchor);
				lineIndex++;
				if(!end && ch == '\r' && i + 1 < str.length() && str.charAt(i + 1) == '\n') { i++; }
				lineStart = i + 1;
			}
		}
	}

	private void drawStringSingleLine(String str, int x, int y, int anchor)
	{
		if(str != null && str.length() > 0)
		{
			int ascent = 0;
			int height = 0;

			if(Mobile.isDoJa) 
			{
				x = AnchorX(x, dojaFont.stringWidth(str), anchor);
				ascent = dojaFont.getAscent();
				height = dojaFont.getHeight();
			}
			else 
			{
				x = AnchorX(x, font.stringWidth(str), anchor);
				ascent = font.getBaselinePosition();
				height = font.getHeight();
			}

			y += ascent;
			
			if((anchor & Graphics.VCENTER)>0) { y = y+height/2; }
			if((anchor & Graphics.BOTTOM)>0) { y = y-height; }
			if((anchor & Graphics.BASELINE)>0) { y = y-ascent; }

			gc.drawString(str, x, y);
		}
	}

	public void drawSubstring(String str, int offset, int len, int x, int y, int anchor)
	{
		if (str.length() >= offset + len)
		{
			drawString(str.substring(offset, offset+len), x, y, anchor);
		}
	}

	public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) 
	{
		if (contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		// Java's coordinate system has positive angles moving counter-clockwise
		arcAngle = -arcAngle;
		startAngle = -startAngle;

		x += translateX;
		y += translateY;

		final boolean hasAlpha = getAlphaComponent() < 255;
		int filledZ = 0;
		byte[] filledPixels = null;

		/** 
		 * Only allocate the alpha buffer if the color isn't opaque. Noticeably
		 * improves performance for opaque arcs. 8 pixels of information are packed
		 * in a single boolean/byte, noticeably reducing memory usage.
		 */
		if(hasAlpha) { filledPixels = new byte[width * height / 8 + 1]; }

		final int clipX = (getClipX() + translateX < 0) ? 0 : (getClipX() + translateX);
		final int clipY = (getClipY() + translateY < 0) ? 0 : (getClipY() + translateY);
		final int clipWidth = (getClipWidth() + getClipX() + translateX > canvasWidth) ? canvasWidth : (getClipWidth() + getClipX() + translateX);
		final int clipHeight = (getClipHeight() + getClipY() + translateY > canvasHeight) ? canvasHeight : (getClipHeight() + getClipY() + translateY);

		final float centerX = x + width / 2.0f;
		final float centerY = y + height / 2.0f;
		final float radiusX = width / 2.0f;
		final float radiusY = height / 2.0f;
		final float startAngleRad = fastToRadians(startAngle);
		final float endAngleRad = (fastToRadians(startAngle + arcAngle) - fastToRadians(startAngle));
		float maxRadius = Math.max(radiusX, radiusY);

		int steps = (int) (Math.abs(arcAngle * ((width + height) / 2) / 50.0f));

		for (int i = 0; i < steps; i++) 
		{
			float angle = startAngleRad + (i * endAngleRad / steps);

			for (float j = 0; j < maxRadius; j++) 
			{
				int innerX = (int) Math.round(centerX + radiusX * Math.cos(angle) * (j / maxRadius));
				int innerY = (int) Math.round(centerY + radiusY * Math.sin(angle) * (j / maxRadius));
				filledZ = ((innerY-y) * width + innerX-x);

				if (innerX >= clipX && innerX < clipWidth && innerY >= clipY && innerY < clipHeight && 
					innerX-x >= 0 && innerY-y >=0 && (hasAlpha ? (filledPixels[filledZ >> 3] & (1 << (7 - filledZ & 7))) == 0 : true)) 
				{
					if (!hasAlpha) { canvasData[(innerY * canvasWidth) + innerX] = getColor(); } 
					else 
					{
						filledPixels[filledZ >> 3] |= (1 << (7 - filledZ & 7));
						canvasData[(innerY * canvasWidth) + innerX] = blendPixels(getColor(), canvasData[(innerY * canvasWidth) + innerX]);
					}
				}
			}
		}
	}


	public void fillRect(int x, int y, int width, int height)
	{
		if(width < 0 || height < 0) { return; }
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		x += translateX;
		y += translateY;

		final int clipX = (getClipX() + translateX < 0) ? 0 : (getClipX() + translateX);
		final int clipY = (getClipY() + translateY < 0) ? 0 : (getClipY() + translateY);
		final int clipWidth = (getClipWidth() + getClipX() + translateX > canvasWidth) ? canvasWidth : (getClipWidth() + getClipX() + translateX);
		final int clipHeight = (getClipHeight() + getClipY() + translateY > canvasHeight) ? canvasHeight : (getClipHeight() + getClipY() + translateY);

		if(y + height > clipHeight) { height = clipHeight - y; }
		if(x + width > clipWidth)   { width = clipWidth - x; }

		/* If width or height ended up as zero, we can exit early */
		if(width == 0 || height == 0) { return; }
		
		final int icache = (x > clipX) ? 0 : (clipX - x);
		final int jcache = (y > clipY) ? 0 : (clipY - y);
		
		for (int j = jcache; j < height; j++) 
		{
			for (int i = icache; i < width; i++) 
			{
				if(!Mobile.isDoJa && getAlphaComponent() == 255) { canvasData[((y+j) * canvasWidth) + x+i] = getColor(); }
				else { canvasData[((y+j) * canvasWidth) + x+i] = blendPixels(getColor(), canvasData[((y+j) * canvasWidth) + x+i]); }
			}
		}
	}

	public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) 
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		arcWidth = Math.abs(arcWidth);
		arcHeight = Math.abs(arcHeight);
		if(arcWidth == 0 && arcHeight == 0) 
		{ 
			fillRect(x, y, arcWidth, arcHeight); 
			return; 
		}

		// We'll be doing only even arc widths and heights, otherwise the borders will look off (java's Graphics allow odd width/heights though)
		if(arcWidth  %2 != 0) { arcWidth++; }
		if(arcHeight %2 != 0) { arcHeight++; }

		if(arcWidth >= width) { arcWidth = width-1; }
		if(arcHeight >= height) { arcHeight = height-1; }
		
		// Fill the main rectangle area
		fillRect(x + (arcWidth/2)+1, y, width - arcWidth - 2, height); // Middle part
		fillRect(x, y + (arcHeight/2)+1, (arcWidth/2)+1, height - arcHeight - 2); // Left Side part
		fillRect(x + (width - (arcWidth/2))-1, y + (arcHeight/2)+1, (arcWidth/2)+1, height - arcHeight - 2); // Right Side part

		// Fill rounded corners
		fillArc(x, y, arcWidth, arcHeight, 90, 90); // Top-left corner
		fillArc(x + width - arcWidth - 1, y, arcWidth, arcHeight, 0, 90); // Top-right corner
		fillArc(x, y + height - arcHeight - 1, arcWidth, arcHeight, 180, 90); // Bottom-left corner
		fillArc(x + width - arcWidth - 1, y + height - arcHeight - 1, arcWidth, arcHeight, 270, 90); // Bottom-right corner
	}

	public void setColor(int rgb)
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		if(!Mobile.isDoJa || (Mobile.isDoJa && Mobile.DoJaVersion < 40)) 
		{
			setColor((rgb>>16) & 0xFF, (rgb>>8) & 0xFF, rgb & 0xFF);
		}
		else // DoJa 4.0 and above support transparency here
		{
			setAlphaRGB(rgb);
		}
	}

	public void setColor(int r, int g, int b)
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }
		
		color = (0xFF << 24) | (r<<16) | (g<<8) | b; // Alpha is ignored below, we set it just so the color variable is accurate
		gc.setColor(new Color(color));
	}

	public void setGrayScale(int value) { setColor(value, value, value); }

	public int getGrayScale() 
	{
		// calculate this based on a simplified perceived color brightness formula from W3C: https://www.w3.org/TR/AERT/#color-contrast
		return (int) (0.299 * getRedComponent() + 0.587 * getGreenComponent() + 0.114 * getBlueComponent());
	}

	public int getRedComponent() { return (color >> 16) & 0xFF; }

	public int getGreenComponent() { return (color >> 8) & 0xFF; }

	public int getBlueComponent() { return color & 0xFF; }

	public int getColor() { return color; }

	public int getDisplayColor(int color) { return color; }

	public Font getFont() { return font; }

	public void setStrokeStyle(int stroke) 
	{
		if(stroke != strokeStyle) { strokeStyle = stroke; } // We set the stroke when actually drawing in draw* operations
	}

	public int getStrokeStyle() { return strokeStyle;}

	public void setFont(Font font)
	{
		if(font == null) { font = Font.getDefaultFont(); }
		this.font = font;
		gc.setFont(font.awtFont);
	}

	public void setClip(int x, int y, int width, int height)
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }
		
		if(!Mobile.isDoJa) { gc.setClip(x, y, width, height); }
		else { gc.setClip(x-getTranslateX(), y-getTranslateY(), width, height); }
	}

	public void clipRect(int x, int y, int width, int height)
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }
		
		gc.clipRect(x, y, width, height);
	}

	public int getTranslateX() { return translateX; }
	
	public int getTranslateY() { return translateY; }

	public int getClipHeight() { return gc.getClipBounds().height; }

	public int getClipWidth() { return gc.getClipBounds().width; }

	public int getClipX() { return gc.getClipBounds().x; }

	public int getClipY() { return gc.getClipBounds().y; }

	public void translate(int x, int y)
	{
		translateX += x;
		translateY += y;
		gc.translate(x, y);
	}

	private int AnchorX(int x, int width, int anchor)
	{
		int xout = x;
		if((anchor & HCENTER)>0) { xout = x-(width/2); }
		if((anchor & RIGHT)>0) { xout = x-width; }
		if((anchor & LEFT)>0) { xout = x; }
		return xout;
	}

	private int AnchorY(int y, int height, int anchor)
	{
		int yout = y;
		if((anchor & VCENTER)>0) { yout = y-(height/2); }
		if((anchor & TOP)>0) { yout = y; }
		if((anchor & BOTTOM)>0) { yout = y-height; }
		if((anchor & BASELINE)>0) { yout = y+height; }
		return yout;
	}

	public void setAlphaRGB(int ARGB)
	{
		color = ARGB;
		gc.setColor(new Color(color, true));
	}

	/*
		****************************
			Nokia Direct Graphics
		****************************
	*/
	// http://www.j2megame.org/j2meapi/Nokia_UI_API_1_1/com/nokia/mid/ui/DirectGraphics.html

	

	public int getNativePixelFormat() { return 0; } // Don't explicitly set any native format for color, let the jar send in whatever it has and we'll convert.

	public int getAlphaComponent() { return (color >> 24 & 0xFF); }

	public void setARGBColor(int argbColor) { setAlphaRGB(argbColor); }

	public void drawImage(javax.microedition.lcdui.Image img, int x, int y, int anchor, int manipulation)
	{
		if(Mobile.compatFantasyZoneFix) 
		{
			setClip(getClipX()-getTranslateX(), getClipY()-getTranslateY(), getClipWidth(), getClipHeight());
		}
		
		BufferedImage image = manipulateImage(img.getCanvas(), manipulation);
		x = AnchorX(x, image.getWidth(), anchor);
		y = AnchorY(y, image.getHeight(), anchor);

		int[] imgData = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
		drawRGB(imgData, 0, image.getWidth(), x, y, image.getWidth(), image.getHeight(), true);

		if(Mobile.compatFantasyZoneFix) 
		{
			setClip(getClipX()-getTranslateX(), getClipY()-getTranslateY(), getClipWidth(), getClipHeight());
		}
	}

	public void drawPixels(byte[] pixels, byte[] transparencyMask, int offset, int scanlength, int x, int y, int width, int height, int manipulation, int format)
	{
		if (width < 0 || height < 0) { throw new IllegalArgumentException("drawPixels(byte) received negative width or height"); }
		if (pixels == null) { throw new NullPointerException("drawPixels(byte) received a null pixel array"); }
		if (offset < 0 || offset >= (pixels.length * 8)) { throw new ArrayIndexOutOfBoundsException("drawPixels(byte) index out of bounds:" + width + " * " + height + "| pixels len:" + (pixels.length * 8) + "| offset:" + offset); }

		if(width == 0 || height == 0) { return; }

		int c = 0;
		int a = 0xFF;
		BufferedImage temp = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);// Nokia DirectGraphics states that image width and height CAN be zero.
		int[] data = ((DataBufferInt) temp.getRaster().getDataBuffer()).getData();
		int bit;

		switch (format) 
		{
			// NOTE on gray scales: The higher the __pixels value on a given
			// position, the darker the pixel. This is so that the output
			// matches actual nokia devices like a 3310, 3410, etc. Which is
			// why (n - c) is used in color scaling. Alpha is scaled normally.
			case DirectGraphics.TYPE_BYTE_1_GRAY_VERTICAL:
				// Bit offset, GRAY_VERTICAL packs 8 vertical pixels in a byte.
				bit = (offset / scanlength) % 8;
				for (int yj = 0; yj < height; yj++) 
				{
					int ypos = yj * width;
					int tmp = ((offset / scanlength) + yj) / 8 * 
						scanlength + (offset % scanlength);
					for (int xj = 0; xj < width; xj++) 
					{
						// Ignore if accessing out of bounds
						if(tmp + xj >= pixels.length)
							continue;

						c = ((pixels[tmp + xj] >> bit) & 1);

						if (transparencyMask != null) 
						{
							a = ((transparencyMask[tmp + xj] >> bit) & 1) << 1;
						
							a *= 255;
						}

						c = (1 - c) * 255;
						
						data[ypos + xj] = (a << 24) | (c << 16) | (c << 8) | c;
					}
					bit++;
					if (bit > 7) 
						bit = 0;
				}
				break;
	
			case DirectGraphics.TYPE_BYTE_1_GRAY: 
				bit = 7 - offset % 8;
				for (int yj = 0; yj < height; yj++) 
				{
					int line = offset + yj * scanlength;
					int ypos = yj * width;
					for (int xj = 0; xj < width; xj++) 
					{
						if((line + xj) / 8 >= pixels.length)
							continue;
						
						c = ((pixels[(line + xj) / 8] >> bit) & 1);

						if (transparencyMask != null) 
						{
							a = ((transparencyMask[(line + xj) / 8] >> bit) 
								& 1) << 1;

							a *= 255;
						}

						c = (1 - c) * 255;

						data[ypos + xj] = (a << 24) | (c << 16) | (c << 8) | c;

						bit--;
						if (bit < 0) 
							bit = 7;
					}
					bit -= (scanlength - width) % 8;
					if (bit < 0) 
						bit = 8 + bit;
				}
				break;

			/**
			 * Note that the following types were not found in use on any J2ME
			 * apps yet:
			 * 
			 * TYPE_BYTE_2_GRAY
			 * TYPE_BYTE_332_RGB
			 * TYPE_BYTE_4_GRAY
			 * TYPE_BYTE_8_GRAY
			 * 
			 * On the upside, their packing mode is simpler, being from left
			 * to right and BYTE_2 has 4 pixels in a byte, BYTE_4 has 2 pixels
			 * in a byte, and BYTE_8 + BYTE_332 are one pixel per byte, so no
			 * need for per-bit manipulations.
			 */
			case DirectGraphics.TYPE_BYTE_2_GRAY:
				for (int yj = 0; yj < height; yj++) 
				{
					int line = offset + yj * scanlength;
					int ypos = yj * width;

					for (int xj = 0; xj < width; xj++) 
					{
						if ((line + xj / 4) >= pixels.length) 
							continue;

						c = (pixels[line + xj / 4] >> (6 - (2 * (xj % 4))) 
							& 0x03);
						if (transparencyMask != null) 
						{
							a = (transparencyMask[line + xj / 4] >> (6 - 
								(2 * (xj % 4))) & 0x03);
							
							a *= 85;
						}

						c = (3 - c) * 85;

						data[ypos + xj] = (a << 24) | (c << 16) | (c << 8) | c; 
					}
				}
				break;

			case DirectGraphics.TYPE_BYTE_332_RGB: 
				for (int yj = 0; yj < height; yj++) 
				{
					int line = offset + yj * scanlength;
					int ypos = yj * width;

					for (int xj = 0; xj < width; xj++) 
					{
						if ((line + xj) >= pixels.length) 
							continue;

						/* We have 3 bytes for red and green, 2 for blue */
						c = pixels[line + xj] & 0xFF;
						int r = (c >> 5) & 0x07;
						int g = (c >> 2) & 0x07;
						int b = (c & 0x03);

						/* 
						 * Thus we have to expand them to 8 bits for 888_RGB.
						 * This one is a bit more complex than the one for
						 * BYTE_4 and BYTE_8 types, due to 3 bits not mapping
						 * perfectly to the 0x00-0xFF range with a single mul
						 * operation.
						 */
						r = (r * 255) / 7;
						g = (g * 255) / 7;
						b *= 85;

						/* 
						 * If a transparencyMask is available, it will have
						 * a full 8 bits of alpha information on each position,
						 * since the transparencyMask's alpha data has to be as 
						 * wide as the color/gray data for a given pixel on all 
						 * byte types.
						 */
						if (transparencyMask != null) 
							a = transparencyMask[line + xj] & 0xFF;

						data[ypos + xj] = (a << 24) | (r << 16) | (g << 8) | b;
					}
				}
				break;

			case DirectGraphics.TYPE_BYTE_4_GRAY: 
				for (int yj = 0; yj < height; yj++) 
				{
					int line = offset + yj * scanlength;
					int ypos = yj * width;

					for (int xj = 0; xj < width; xj++) 
					{
						if ((line + xj / 2) >= pixels.length) 
							continue;

						c = (pixels[line + xj / 2] >> (4 * (1 - (xj % 2))) 
							& 0x0F);
						if (transparencyMask != null)
						{
							a = (transparencyMask[line + xj / 2] >> (4 * (1 - 
								(xj % 2))) & 0x0F);

							a *= 17;
						}
						
						c = (15 - c) * 17;
						
						data[ypos + xj] = (a << 24) | (c << 16) | (c << 8) | c;
					}
				}
				break;

			case DirectGraphics.TYPE_BYTE_8_GRAY: 
				for (int yj = 0; yj < height; yj++) 
				{
					int line = offset + yj * scanlength;
					int ypos = yj * width;

					for (int xj = 0; xj < width; xj++) 
					{
						if ((line + xj) >= pixels.length) 
							continue;

						c = 255 - (pixels[line + xj] & 0xFF);
						
						if(transparencyMask != null)
							a = transparencyMask[line + xj] & 0xFF;

						data[ypos + xj] = (a << 24) | (c << 16) | (c << 8) | c;
					}
				}
				break;
			default:
				throw new IllegalArgumentException("Unsupported format: " + format);
		}

		temp = manipulateImage(temp, manipulation);
		data = ((DataBufferInt) temp.getRaster().getDataBuffer()).getData();

		drawRGB(data, 0, temp.getWidth(), x, y, temp.getWidth(), temp.getHeight(), true);
	}

	public void drawPixels(int[] pixels, boolean transparency, int offset, int scanlength, int x, int y, int width, int height, int manipulation, int format) 
	{
		if (width < 0 || height < 0) { throw new IllegalArgumentException("drawPixels(int) received negative width or height"); }
		if (pixels == null) { throw new NullPointerException("drawPixels(int) received a null pixel array"); }
		if (offset < 0 || offset >= pixels.length) { throw new ArrayIndexOutOfBoundsException("drawPixels(int) index out of bounds:" + width + " * " + height + "| len:" + pixels.length); }

		if(width == 0 || height == 0) { return; }

		// Create the temporary BufferedImage and get its DataBuffer to manipulate it directly.
		BufferedImage temp = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		int[] data = ((DataBufferInt) temp.getRaster().getDataBuffer()).getData();

		for (int row = 0; row < height; row++) 
		{
			int srcIndex = offset + row * scanlength;
			for (int col = 0; col < width; col++) 
			{
				if(srcIndex + col >= pixels.length) { continue; } // Ignore if accessing out of bounds
				if (!transparency) { pixels[srcIndex + col] |= 0xFF000000; } // Set alpha to 255
				data[row * width + col] = pixels[srcIndex + col];
			}
		}

		temp = manipulateImage(temp, manipulation);
		data = ((DataBufferInt) temp.getRaster().getDataBuffer()).getData();

		drawRGB(data, 0, temp.getWidth(), x, y, temp.getWidth(), temp.getHeight(), transparency);
	}

	public void drawPixels(short[] pixels, boolean transparency, int offset, int scanlength, int x, int y, int width, int height, int manipulation, int format)
	{
		if (width < 0 || height < 0) { throw new IllegalArgumentException("drawPixels(short) received negative width or height"); }
		if (pixels == null) { throw new NullPointerException("drawPixels(short) received a null pixel array"); }
		if (offset < 0 || offset >= pixels.length) { throw new ArrayIndexOutOfBoundsException("drawPixels(short) index out of bounds:" + width + " * " + height + "| len:" + pixels.length); }

		if(width == 0 || height == 0) { return; }
		
		BufferedImage temp = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    	int[] data = ((DataBufferInt) temp.getRaster().getDataBuffer()).getData();
		
		// Prepare the pixel data
		for (int row = 0; row < height; row++) 
		{
			int srcIndex = offset + row * scanlength;
			for (int col = 0; col < width; col++) 
			{
				if(srcIndex + col >= pixels.length) { continue; } // Ignore if accessing out of bounds
				data[row * width + col] = pixelToColor(pixels[srcIndex + col], format);
				if (!transparency) { data[row * width + col] |= 0xFF000000; } // Set alpha to 255
			}
		}

		temp = manipulateImage(temp, manipulation);
		data = ((DataBufferInt) temp.getRaster().getDataBuffer()).getData();

		drawRGB(data, 0, temp.getWidth(), x, y, temp.getWidth(), temp.getHeight(), transparency);
	}

	public void drawPolygon(int[] xPoints, int xOffset, int[] yPoints, int yOffset, int nPoints, int argbColor)
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		int temp = color;

		// Drawing a Polygon means basically drawing the edges (lines) between each pair of vertices
		setAlphaRGB(argbColor);
		for(int i=0; i < nPoints; i++)
		{
			if(i == nPoints-1) { drawLine(xPoints[xOffset+i], yPoints[yOffset+i], xPoints[xOffset], yPoints[yOffset]); }
			else { drawLine(xPoints[xOffset+i], yPoints[yOffset+i], xPoints[xOffset+i+1], yPoints[yOffset+i+1]); }
		}
		setAlphaRGB(temp);
	}

	public void drawTriangle(int x1, int y1, int x2, int y2, int x3, int y3)
	{
		drawTriangle(x1, y1, x2, y2, x3, y3, getColor());
	}

	public void drawTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int argbColor)
	{
		int temp = color;
		setAlphaRGB(argbColor);
		drawLine(x1, y1, x2, y2);
		drawLine(x2, y2, x3, y3);
		drawLine(x3, y3, x1, y1);
		setAlphaRGB(temp);
	}

	public void fillPolygon(int[] xPoints, int xOffset, int[] yPoints, int yOffset, int nPoints, int argbColor) 
	{
		if (contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		if (nPoints < 3) { return; }

		final int clipX = (getClipX() + translateX < 0) ? 0 : (getClipX() + translateX);
		final int clipY = (getClipY() + translateY < 0) ? 0 : (getClipY() + translateY);
		final int clipWidth = (getClipWidth() + getClipX() + translateX > canvasWidth) ? canvasWidth : (getClipWidth() + getClipX() + translateX);
		final int clipHeight = (getClipHeight() + getClipY() + translateY > canvasHeight) ? canvasHeight : (getClipHeight() + getClipY() + translateY);

		/* 
		 * Filling polygons is done through the canonical Scan Line fill algorithm. It works
		 * just like its description: Find the yMax and yMin of the polygon, calculate the intersections
		 * between each edge, sort intersections by increasing X coordinate, then fill from top to bottom.
		 */
		int ymin = Integer.MAX_VALUE;
		int ymax = Integer.MIN_VALUE;
		for (int i = 0; i < nPoints; i++) 
		{
			if (yPoints[i+yOffset] < ymin) { ymin = yPoints[i+yOffset]; }
			if (yPoints[i+yOffset] > ymax) { ymax = yPoints[i+yOffset]; }
		}

		if(ymin+translateY < clipY) { ymin = clipY-translateY; }
		if(ymax+translateY >= clipHeight) { ymax = clipHeight-translateY; }

		final int[] intersections = new int[nPoints]; 
        int intersectionCount = 0;

		for (int y = ymin; y < ymax; y++) 
		{
			intersectionCount = 0;
			for (int i = 0; i < nPoints; i++) 
			{
				int j = (i + 1) % nPoints;

				if ((yPoints[i + yOffset] <= y && yPoints[j + yOffset] > y) || (yPoints[j + yOffset] <= y && yPoints[i + yOffset] > y)) 
				{
					int dy = yPoints[j + yOffset] - yPoints[i + yOffset];
					if (dy != 0) 
					{
						int x = xPoints[i + xOffset] * dy + (y - yPoints[i + yOffset]) * (xPoints[j + xOffset] - xPoints[i + xOffset]);
						x /= dy;
						intersections[intersectionCount++] = x;
					}
				}
			}

			for (int i = 0; i < intersectionCount - 1; i++) 
			{
				for (int j = 0; j < intersectionCount - 1 - i; j++) 
				{
					if (intersections[j] > intersections[j + 1]) 
					{
						int temp = intersections[j];
						intersections[j] = intersections[j + 1];
						intersections[j + 1] = temp;
					}
				}
			}

			for (int i = 0; i < intersectionCount; i += 2) 
			{
				if (i + 1 < intersectionCount) 
				{
					int xStart = intersections[i] + translateX;
					int xEnd = intersections[i + 1] + translateX;
					if(xStart < clipX) { xStart = clipX; }
					if(xEnd > clipWidth) { xEnd = clipWidth; }
					for (int x = xStart; x < xEnd; x++) 
					{ 
						if(((argbColor >> 24) & 0xFF) == 255) { canvasData[(y+translateY)*canvasWidth+x] = argbColor; }
						else 
						{
							canvasData[(y+translateY)*canvasWidth+x] = blendPixels(argbColor, canvasData[(y+translateY)*canvasWidth+x]);
						}
					}
				}
			}
		}
	}

	public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3)
	{
		fillTriangle(x1, y1, x2, y2, x3, y3, getColor());
	}

	public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int argbColor)
	{
		fillPolygon(new int[]{x1, x2, x3}, 0, new int[] {y1, y2, y3}, 0, 3, argbColor);
	}

	public void getPixels(byte[] pixels, byte[] transparencyMask, int offset, int scanlength, int x, int y, int width, int height, int format)
	{
		if (pixels == null) { throw new NullPointerException("Byte array cannot be null");}
		
		x += getTranslateX();
		y += getTranslateY();

		if (x < 0|| y < 0 || height < 0 || width < 0)
		{ 
			throw new IllegalArgumentException("Invalid width,height,x or y"); 
		}
		
		if (x < 0 || y < 0 || width * height > pixels.length)  
		{
			throw new ArrayIndexOutOfBoundsException("Requested copy area exceeds bounds of the image");
		}

		// Copy only the area that's on screen.
		if(x+width >= canvasWidth) { width = canvasWidth-x; }
		if(y+height >= canvasHeight) { height = canvasHeight-y; }
	
		switch (format) 
		{
			case DirectGraphics.TYPE_BYTE_1_GRAY_VERTICAL:
				for (int row = 0; row < height; row++) 
				{
					for (int col = 0; col < width; col++) 
					{
						int pixelIndex = (y + row) * canvasWidth + (x + col);
						int pixelValue = canvasData[pixelIndex];

						// Store pixel value as a bit in the pixels array
						int byteIndex = (offset + row) * scanlength + (col / 8);
						int bitIndex = col % 8;

						// Set the bit in the retrieved byte to the expected value.
						pixels[byteIndex] |= ((pixelValue & 0xFF) != 0 ? 0 : 1) << (7 - bitIndex);
						if(transparencyMask != null) { transparencyMask[byteIndex] |= ((pixelValue & 0xFF000000) != 0 ? 0 : 1) << (7 - bitIndex); }
					}
				}
				break;

			case DirectGraphics.TYPE_BYTE_1_GRAY:
				for (int row = 0; row < height; row++) 
				{
					for (int col = 0; col < width; col++) 
					{
						int pixelIndex = (y + row) * canvasWidth + (x + col);
						int pixelValue = canvasData[pixelIndex];
						int byteIndex = (offset / 8) + ((row * width + col) / 8);
						int bitIndex = (row * width + col) % 8;

						pixels[byteIndex] |= ((pixelValue & 0xFF) != 0 ? 0 : 1) << (7 - bitIndex);
						if(transparencyMask != null) { transparencyMask[byteIndex] |= ((pixelValue & 0xFF000000) != 0 ? 0 : 1) << (7 - bitIndex); }
					}
				}
				break;

			case DirectGraphics.TYPE_BYTE_2_GRAY:
				for (int row = 0; row < height; row++) 
				{
					for (int col = 0; col < width; col++) 
					{
						int pixelIndex = (y + row) * canvasWidth + (x + col);
						int pixelValue = canvasData[pixelIndex];

						int byteIndex = (offset + row) * scanlength + (col / 4);
						int pixelPos = col % 4;

						int grayValue = (pixelValue & 0xFF);
						int c = grayValue / 85;

						pixels[byteIndex] |= c << (6 - (2 * pixelPos));
						if (transparencyMask != null) 
						{
							int alphaValue = (pixelValue >> 24) & 0xFF;
							int a = alphaValue / 85;
							transparencyMask[byteIndex] |= a << (6 - (2 * pixelPos));
						}
					}
				}
				break;

			case DirectGraphics.TYPE_BYTE_332_RGB:
				for (int row = 0; row < height; row++) 
				{
					for (int col = 0; col < width; col++) 
					{
						int pixelIndex = (y + row) * canvasWidth + (x + col);
						int pixelValue = canvasData[pixelIndex];

						int byteIndex = (offset + row) * scanlength + col;
						int r = (pixelValue >> 16) & 0xFF;
						int g = (pixelValue >> 8) & 0xFF;
						int b = pixelValue & 0xFF;

						int rgb = ((r * 7 / 255) << 5) | ((g * 7 / 255) << 2) | (b * 3 / 255);
						
						pixels[byteIndex] = (byte) rgb;
						if (transparencyMask != null) 
						{
							int alphaValue = (pixelValue >> 24) & 0xFF;
							transparencyMask[byteIndex] = (byte) alphaValue;
						}
					}
				}
				break;

			case DirectGraphics.TYPE_BYTE_4_GRAY:
				for (int row = 0; row < height; row++) 
				{
					for (int col = 0; col < width; col++) 
					{
						int pixelIndex = (y + row) * canvasWidth + (x + col);
						int pixelValue = canvasData[pixelIndex];

						int byteIndex = (offset + row) * scanlength + (col / 2);
						int pixelPos = col % 2;

						int grayValue = (pixelValue & 0xFF);
						int c = grayValue / 17;

						pixels[byteIndex] |= c << (4 * (1 - pixelPos));
						if (transparencyMask != null) 
						{
							int alphaValue = (pixelValue >> 24) & 0xFF;
							int a = alphaValue / 17;
							transparencyMask[byteIndex] |= a << (4 * (1 - pixelPos));
						}
					}
				}
				break;
			
			case DirectGraphics.TYPE_BYTE_8_GRAY:
				for (int row = 0; row < height; row++) 
				{
					for (int col = 0; col < width; col++) 
					{
						int pixelIndex = (y + row) * canvasWidth + (x + col);
						int pixelValue = canvasData[pixelIndex];

						int byteIndex = (offset + row) * scanlength + col;
						int grayValue = (pixelValue & 0xFF);
						pixels[byteIndex] = (byte) grayValue;

						if (transparencyMask != null) 
						{
							int alphaValue = (pixelValue >> 24) & 0xFF;
							transparencyMask[byteIndex] = (byte) alphaValue;
						}
					}
				}
				break;
			default:
				throw new IllegalArgumentException("Unsupported format: " + format);
		}
	}

	public void getPixels(int[] pixels, int offset, int scanlength, int x, int y, int width, int height, int format)
	{
		if (pixels == null) { throw new NullPointerException("int array cannot be null"); }
		
		x += getTranslateX();
		y += getTranslateY();

		if (x < 0|| y < 0 || height < 0 || width < 0)
		{ 
			throw new IllegalArgumentException("Invalid width,height,x or y"); 
		}
		
		if (x < 0 || y < 0 || width * height > pixels.length)  
		{
			throw new ArrayIndexOutOfBoundsException("Requested copy area exceeds bounds of the image");
		}

		// Copy only the area that's on screen.
		if(x+width >= canvasWidth) { width = canvasWidth-x; }
		if(y+height >= canvasHeight) { height = canvasHeight-y; }

		for (int row = 0; row < height; row++) 
		{
			for(int col = 0; col < width; col++) 
			{
				int canvasPixel = canvasData[col + x + (row + y) * canvasWidth];
				int pixelIndex = offset + col + (row * scanlength);
				// getPixels(short[]) explains why blending is done here
				pixels[pixelIndex] = blendPixels(canvasPixel, pixels[pixelIndex]);
			}
		}
	}

	public void getPixels(short[] pixels, int offset, int scanlength, int x, int y, int width, int height, int format)
	{
		if (pixels == null) { throw new NullPointerException("short array cannot be null"); }
		
		x += getTranslateX();
		y += getTranslateY();

		if (x < 0|| y < 0 || height < 0 || width < 0)
		{ 
			throw new IllegalArgumentException("Invalid width,height,x or y"); 
		}
		
		if (x < 0 || y < 0 || width * height > pixels.length)  
		{
			throw new ArrayIndexOutOfBoundsException("Requested copy area exceeds bounds of the image");
		}

		// Copy only the area that's on screen.
		if(x+width >= canvasWidth) { width = canvasWidth-x; }
		if(y+height >= canvasHeight) { height = canvasHeight-y; }
		
		for(int row=0; row<height; row++)
		{
			for (int col=0; col<width; col++)
			{
				int canvasPixel = canvasData[col + x + (row + y) * canvasWidth];
				int pixelIndex = offset + col + (row * scanlength);
				// We have to alpha blend this, Lemmings is a game that reuses the same short[] array for drawing terrain here
				// If we just add the canvas pixel directly to it, the transparency will override anything previously in the array pos
				pixels[pixelIndex] = colorToShortPixel(blendPixels(canvasPixel, pixelToColor(pixels[pixelIndex], format)), format);
			}
		}
	}

	private int pixelToColor(short c, int format) 
	{
		int a = 0xFF;
		int r = 0;
		int g = 0;
		int b = 0;

		/* 
		 * Here we cast to USHORT_4444_ARGB if the game just tries sending the pixels with the 
		 * "default" short pixel format FreeJ2ME "accepts" (it doesn't expose any of the valid ones
		 * as a way to try and make the game send pixels in their native format. Works for karma studios games.) 
		 */
		if(format == 0) { format = DirectGraphics.TYPE_USHORT_4444_ARGB; }
	
		switch (format) 
		{
			case DirectGraphics.TYPE_USHORT_1555_ARGB:
				a = ((c >> 15) & 0x01) * 0xFF; // just 1 bit for alpha
				r = (c >> 10) & 0x1F; 
				g = (c >> 5) & 0x1F; 
				b = c & 0x1F;
				r = (r << 3) | (r >> 2);
				g = (g << 3) | (g >> 2);
				b = (b << 3) | (b >> 2);
				break;
			case DirectGraphics.TYPE_USHORT_444_RGB:
				r = (c >> 8) & 0xF; 
				g = (c >> 4) & 0xF; 
				b = c & 0xF;
				r = (r << 4) | r;
				g = (g << 4) | g;
				b = (b << 4) | b;
				break;
			case DirectGraphics.TYPE_USHORT_4444_ARGB:
				a = (c >> 12) & 0xF; 
				r = (c >> 8) & 0xF; 
				g = (c >> 4) & 0xF; 
				b = c & 0xF;
				a = (a << 4) | a;
				r = (r << 4) | r;
				g = (g << 4) | g;
				b = (b << 4) | b;
				break;
			case DirectGraphics.TYPE_USHORT_555_RGB:
				r = (c >> 10) & 0x1F; 
				g = (c >> 5) & 0x1F; 
				b = c & 0x1F;
				r = (r << 3) | (r >> 2);
				g = (g << 3) | (g >> 2);
				b = (b << 3) | (b >> 2);
				break;
			case DirectGraphics.TYPE_USHORT_565_RGB:
				r = (c >> 11) & 0x1F; 
				g = (c >> 5) & 0x3F; 
				b = c & 0x1F;
				r = (r << 3) | (r >> 2);
				g = (g << 2) | (g >> 4);
				b = (b << 3) | (b >> 2);
				break;
			default:
				throw new IllegalArgumentException("Unsupported format: " + format);
		}
	
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private short colorToShortPixel(int c, int format) 
	{
		int a, r, g, b;

		/* 
		 * Here we cast to USHORT_4444_ARGB if the game just tries sending the pixels with the 
		 * "default" short pixel format FreeJ2ME "accepts" (it doesn't expose any of the valid ones
		 * as a way to try and make the game send pixels in their native format. Works for karma studios games.) 
		 */
		if(format == 0) { format = DirectGraphics.TYPE_USHORT_4444_ARGB; }
	
		switch (format) 
		{
			case DirectGraphics.TYPE_USHORT_1555_ARGB:
				a = (c >>> 31) & 0x1;
				r = (c >> 19) & 0x1F;
				g = (c >> 11) & 0x1F;
				b = (c >> 3) & 0x1F;
				return (short) ((a << 15) | (r << 10) | (g << 5) | b);
			case DirectGraphics.TYPE_USHORT_444_RGB:
				r = (c >> 20) & 0xF;
				g = (c >> 12) & 0xF;
				b = (c >> 4) & 0xF;
				return (short) ((r << 8) | (g << 4) | b);
			case DirectGraphics.TYPE_USHORT_4444_ARGB:
				a = (c >>> 28) & 0xF;
				r = (c >> 20) & 0xF;
				g = (c >> 12) & 0xF;
				b = (c >> 4) & 0xF;
				return (short) ((a << 12) | (r << 8) | (g << 4) | b);
			case DirectGraphics.TYPE_USHORT_555_RGB:
				r = (c >> 19) & 0x1F;
				g = (c >> 11) & 0x1F;
				b = (c >> 3) & 0x1F;
				return (short) ((r << 10) | (g << 5) | b);
			case DirectGraphics.TYPE_USHORT_565_RGB:
				r = (c >> 19) & 0x1F;
				g = (c >> 10) & 0x3F;
				b = (c >> 3) & 0x1F;
				return (short) ((r << 11) | (g << 5) | b);
			default:
				throw new IllegalArgumentException("Unsupported format: " + format);
		}
	}

	private static final BufferedImage manipulateImage(final BufferedImage image, final int manipulation)
	{
		// Return early if there's no manipulation to be done
		if(manipulation == 0 || manipulation == HV180) { return image; }
		
		switch(manipulation)
		{
			case V180:
			case FLIP_HORIZONTAL:
            case DirectGraphics.FLIP_HORIZONTAL:
                return PlatformImage.transformImage(image, Sprite.TRANS_MIRROR);
            case H180:
			case FLIP_VERTICAL:
            case DirectGraphics.FLIP_VERTICAL:
                return PlatformImage.transformImage(image, Sprite.TRANS_MIRROR_ROT180);
			case HV270:
			case FLIP_ROTATE_LEFT:
            case DirectGraphics.ROTATE_90:
                return PlatformImage.transformImage(image, Sprite.TRANS_ROT270);
			case HV:
			case FLIP_ROTATE:
            case DirectGraphics.ROTATE_180:
                return PlatformImage.transformImage(image, Sprite.TRANS_ROT180);
			case HV90:
			case FLIP_ROTATE_RIGHT:
            case DirectGraphics.ROTATE_270:
                return PlatformImage.transformImage(image, Sprite.TRANS_ROT90);
            case V270:
            case H90:
			case FLIP_ROTATE_RIGHT_VERTICAL:
                return PlatformImage.transformImage(image, Sprite.TRANS_MIRROR_ROT90);
            case V90:
            case H270:
			case FLIP_ROTATE_RIGHT_HORIZONTAL:
                return PlatformImage.transformImage(image, Sprite.TRANS_MIRROR_ROT270);
            default:
				Mobile.log(Mobile.LOG_WARNING, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "manipulateImage "+manipulation+" not defined");
		}

		return image;
	}

	// Used everywhere alpha blending might be needed, be it getPixels, flushGraphics, etc.
	private final int blendPixels(final int srcPixel, final int destPixel) 
	{
		final int srcAlpha = (srcPixel >> 24) & 0xFF; // Source alpha
		int newRed = 0, newGreen = 0, newBlue = 0;

		if(srcAlpha == 0) { return destPixel; } // No blending needed in any of the cases below, return early

		switch (renderMode) 
		{
			case com.nttdocomo.opt.ui.Graphics2.OP_REPL: // Also used by MIDP, which does this operation by default (SRC_OVER)
				if(srcAlpha == 255) { return srcPixel; }
				else // Blending is needed
				{
					final int destAlpha = (destPixel >> 24) & 0xFF;

					final int invSrcAlpha = (255 - srcAlpha);

					final int newAlpha = (srcAlpha + destAlpha > 255) ? 255 : (srcAlpha + destAlpha);

					newRed = ((((srcPixel >> 16) & 0xFF) * srcAlpha) + (((destPixel >> 16) & 0xFF) * invSrcAlpha)) / 255;
					newGreen =  ((((srcPixel >> 8) & 0xFF) * srcAlpha) + (((destPixel >> 8) & 0xFF) * invSrcAlpha)) / 255;
					newBlue = (((srcPixel & 0xFF) * srcAlpha) + ((destPixel & 0xFF) * invSrcAlpha)) / 255;

					return (newAlpha << 24) | (newRed << 16) | (newGreen << 8) | newBlue;
				}
			// ADD and SUB never take alpha into consideration for RGB values
			case com.nttdocomo.opt.ui.Graphics2.OP_ADD:
				newRed   = clamp(((destPixel >> 16) & 0xFF) * dstRatio / 255 + ((srcPixel >> 16) & 0xFF) * srcRatio / 255);
				newGreen = clamp(((destPixel >> 8) & 0xFF) * dstRatio / 255 + ((srcPixel >> 8) & 0xFF) * srcRatio / 255);
				newBlue  = clamp((destPixel & 0xFF) * dstRatio / 255 + (srcPixel & 0xFF) * srcRatio / 255);
				return (0xFF << 24) | (newRed << 16) | (newGreen << 8) | newBlue;
			case com.nttdocomo.opt.ui.Graphics2.OP_SUB:
				newRed   = clamp(((destPixel >> 16) & 0xFF) * dstRatio / 255 - ((srcPixel >> 16) & 0xFF) * srcRatio / 255);
				newGreen = clamp(((destPixel >> 8) & 0xFF) * dstRatio / 255 - ((srcPixel >> 8) & 0xFF) * srcRatio / 255);
				newBlue  = clamp((destPixel & 0xFF) * dstRatio / 255 - (srcPixel & 0xFF) * srcRatio / 255);
				return (0xFF << 24) | (newRed << 16) | (newGreen << 8) | newBlue;
			
			default:
				return srcPixel;
		}
	}

	/*
		****************************
			Motorola FunLights
		****************************
	*/
	public void drawFunLights(int[] pixelData, int width, int height) 
	{		
		// Set pixels for the fun lights directly
		for (int y = 0; y < height; y++) 
		{
			for (int x = 0; x < width; x++) 
			{
				if (x < width / 2 && y >= height - Mobile.funLightRegionSize / 2) // Navigation Keypad Region (Bottom-Left)
				{
					if (y < height) pixelData[y * width + x] = Mobile.funLightRegionColor[2]; // funLightColorNav
				} 
				else if (x >= width / 2 && y >= height - Mobile.funLightRegionSize / 2) // Numeric Keypad Region (Bottom-Right)
				{
					if (y < height) pixelData[y * width + x] = Mobile.funLightRegionColor[3];
				} 
				else if (x < (Mobile.funLightRegionSize / 2) -2) // Left Sideband Region
				{
					pixelData[y * width + x] = Mobile.funLightRegionColor[4];
				} 
				else if (x >= width - Mobile.funLightRegionSize / 2) // Right Sideband Region
				{
					pixelData[y * width + x] = Mobile.funLightRegionColor[4];
				}
			}
		}
	
		// Now apply a Gaussian blur using direct pixel manipulation
		applyGaussianBlur(pixelData, width, height);
	}
	
	private void applyGaussianBlur(int[] pixels, int width, int height) 
	{
		final int[] result = new int[pixels.length];

		final int kernelSize = 7;
		final int kernelRadius = kernelSize / 2;
	
		// Horizontal blur
		for (int y = 0; y < height; y++) 
		{
			for (int x = 0; x < width; x++) 
			{
				if(x > Mobile.funLightRegionSize - kernelRadius && x < width - Mobile.funLightRegionSize + kernelRadius && y < height - Mobile.funLightRegionSize + kernelRadius) { continue; }

				float r = 0, g = 0, b = 0, a = 0;
				float weightSum = 0;
	
				for (int kx = -kernelRadius; kx <= kernelRadius; kx++) 
				{
					int pixelX = x + kx;
	
					if (pixelX >= 0 && pixelX < width) 
					{
						int pixelColor = pixels[y * width + pixelX];
						float kernelWeight = (float) gaussianKernel[kx + kernelRadius] / GAUSSIAN_SCALE_FACTOR;

						r += ((pixelColor >> 16) & 0xff) * kernelWeight;
						g += ((pixelColor >> 8) & 0xff) * kernelWeight;
						b += (pixelColor & 0xff) * kernelWeight;
						a += ((pixelColor >> 24) & 0xff) * kernelWeight;
						weightSum += kernelWeight;
					}
				}
	
				int newAlpha = (a / weightSum < 255) ? (int)(a / weightSum) : 255;
				int newRed =   (r / weightSum < 255) ? (int)(r / weightSum) : 255;
				int newGreen = (g / weightSum < 255) ? (int)(g / weightSum) : 255;
				int newBlue =  (b / weightSum < 255) ? (int)(b / weightSum) : 255;
	
				result[y * width + x] = (newAlpha << 24) | (newRed << 16) | (newGreen << 8) | newBlue;
			}
		}
	
		// vertical blur
		for (int x = 0; x < width; x++) 
		{
			for (int y = 0; y < height; y++) 
			{
				if(x > Mobile.funLightRegionSize - kernelRadius && x < width - Mobile.funLightRegionSize + kernelRadius && y < height - Mobile.funLightRegionSize + kernelRadius) { continue; }

				float r = 0, g = 0, b = 0, a = 0;
				float weightSum = 0;
	
				for (int ky = -kernelRadius; ky <= kernelRadius; ky++) 
				{
					int pixelY = y + ky;
	
					if (pixelY >= 0 && pixelY < height) 
					{
						int pixelColor = result[pixelY * width + x];
						float kernelWeight = (float) gaussianKernel[ky + kernelRadius] / GAUSSIAN_SCALE_FACTOR;

						r += ((pixelColor >> 16) & 0xff) * kernelWeight;
						g += ((pixelColor >> 8) & 0xff) * kernelWeight;
						b += (pixelColor & 0xff) * kernelWeight;
						a += ((pixelColor >> 24) & 0xff) * kernelWeight;
						weightSum += kernelWeight;
					}
				}
	
				int newAlpha = (a / weightSum < 255) ? (int)(a / weightSum) : 255;
				int newRed =   (r / weightSum < 255) ? (int)(r / weightSum) : 255;
				int newGreen = (g / weightSum < 255) ? (int)(g / weightSum) : 255;
				int newBlue =  (b / weightSum < 255) ? (int)(b / weightSum) : 255;
	
				result[y * width + x] = (newAlpha << 24) | (newRed << 16) | (newGreen << 8) | newBlue;
			}
		}
		System.arraycopy(result, 0, pixels, 0, pixels.length);
	}

	/*
		****************************
			DoJa Graphics
		****************************
	*/

	public void dispose() 
	{ 
		contextDisposed = true;
		canvasData = null;
		baseImage = null;
		canvas = null;
		gc.dispose();
	}

	// This has to create a copy of the current graphics context, translation, clip, etc included
	public com.nttdocomo.ui.Graphics copy() 
	{ 
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		com.nttdocomo.ui.Graphics newGc = new com.nttdocomo.ui.Graphics(this.baseImage);

		newGc.translate(getTranslateX(), getTranslateY());
		newGc.setClip(getClipX(), getClipY(), getClipWidth(), getClipHeight());
		newGc.setColor(color);
		newGc.setStrokeStyle(getStrokeStyle());

		return newGc;
	}

	public void copyArea(int x, int y, int width, int height, int dx, int dy) 
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		copyArea(x, y, width, height, dx, dy, 0);
	}

	// Text appears to be rendered with BASELINE anchoring, at least, it's what most DoJa jars seem to like better
	public void drawChars(char[] data, int x, int y, int offset, int length)
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		if(data == null) { throw new NullPointerException("Null char array received"); }
		if(offset < 0 || length < 0 || offset+length >= data.length) { throw new StringIndexOutOfBoundsException("invalid length and/or position received"); }
		drawChars(data, offset, length, x, y, BASELINE);
	}

	public void drawString(String str, int x, int y)
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }
		if(str == null) { throw new NullPointerException("Null string received"); }
		
		if(str.length() > 0) { drawString(str, x, y, BASELINE); }
	}

	public void drawImage(com.nttdocomo.ui.Image image, int[] matrix) 
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }
		try 
		{
			float[] fmatrix = new float[matrix.length];
			for (int i = 0; i < matrix.length; i++) { fmatrix[i] = (float) matrix[i]; }
			AffineTransform transform = new AffineTransform(fmatrix);

			gc.setTransform(transform);
			drawScaledImage(image, 0, 0, image.getWidth(), image.getHeight(), 0, 0, image.getWidth(), image.getHeight());
		} 
		catch (Exception e) 
		{
			Mobile.log(Mobile.LOG_ERROR, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "drawImage with matrix: " + e.getMessage());
		}
	}

	public void drawImage(com.nttdocomo.ui.Image image, int[] matrix, int sx, int sy, int width, int height) 
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }
		try 
		{
			float[] fmatrix = new float[matrix.length];
			for (int i = 0; i < matrix.length; i++) { fmatrix[i] = (float) matrix[i]; }
			AffineTransform transform = new AffineTransform(fmatrix);

			gc.setTransform(transform);
			drawScaledImage(image, sx, sy, width, height, sx, sy, width, height);
		} 
		catch (Exception e) 
		{
			Mobile.log(Mobile.LOG_ERROR, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "drawImage with matrix and part: " + e.getMessage());
		}
	}

	public void drawImage(com.nttdocomo.ui.Image image, int x, int y) 
	{
		drawScaledImage(image, x, y, image.getWidth(), image.getHeight(), 0, 0, image.getWidth(), image.getHeight());
	}

	public void drawImage(com.nttdocomo.ui.Image image, int dx, int dy, int sx, int sy, int width, int height) 
	{
		drawScaledImage(image, dx, dy, width, height, sx, sy, width, height);
	}

	private int[] adjustCoordinates(int imageWidth, int imageHeight, int sx, int sy, int width, int height, int transform) 
	{
		/* These swap width and height */
		if(transform == FLIP_ROTATE_LEFT || transform == FLIP_ROTATE_RIGHT ||
			transform == FLIP_ROTATE_RIGHT_HORIZONTAL || transform == FLIP_ROTATE_RIGHT_VERTICAL)
		{
			int tempHeight = height;
			height = width;
			width = height;
		}

		switch (transform) 
		{
			case FLIP_HORIZONTAL:
				sx = imageWidth - sx - width; 
				break;

			case FLIP_VERTICAL:
				sy = imageHeight - sy - height; 
				break;

			case FLIP_ROTATE_RIGHT: // 90 degree rotation to the right (clockwise 90)
				int tempX = sx;
				sx = imageHeight - sy - width; 
				sy = tempX;
				break;

			case FLIP_ROTATE_LEFT: // 90 degree rotation to the left (clockwise 270)
				int tempY = sy;
				sy = imageWidth - sx - height; 
				sx = tempY;
				break;

			case FLIP_ROTATE: // 180 degree rotation
				sx = imageWidth - sx - width; 
				sy = imageHeight - sy - height;
				break;

			case FLIP_ROTATE_RIGHT_VERTICAL: // Same as a mirrored 90 degree clockwise rotation
				Mobile.log(Mobile.LOG_WARNING, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "DoJa FLIP_ROTATE_RIGHT_VERTICAL untested");
				tempY = sy;
				sy = imageWidth - sx - height;
				sx = imageHeight - tempY - width;
				break;

			case FLIP_ROTATE_RIGHT_HORIZONTAL: // Same as a mirrored 270 degree clockwise rotation
				Mobile.log(Mobile.LOG_WARNING, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "DoJa FLIP_ROTATE_RIGHT_HORIZONTAL untested");
				tempX = sx;
				sx = sy;
				sy = tempX;
				break;
		}

		// Return adjusted coordinates via reference parameters
    	return new int[]{sx, sy, width, height};
	}

	public void setOrigin(int x, int y) 
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }
		
		translate(x-translateX, y-translateY); // Reset from previous translation
	}

	public void clearClip() 
	{ 
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }
		
		setClip(0, 0, canvasWidth, canvasHeight); 
	}

	public void setFont(com.nttdocomo.ui.Font dojaFont) 
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		if(dojaFont == null) { dojaFont = com.nttdocomo.ui.Font.getDefaultFont(); }
		this.dojaFont = dojaFont;
		gc.setFont(dojaFont.awtFont);
	}

	public void lock() 
	{ 
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		dojaLockCount++; 
	}

    public void unlock(boolean forced)
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		dojaLockCount = forced ? 0 : dojaLockCount-1;
		
		if (dojaLockCount == 0 && com.nttdocomo.ui.Display.getCurrent() instanceof com.nttdocomo.ui.Canvas) 
		{
			((com.nttdocomo.ui.Canvas) com.nttdocomo.ui.Display.getCurrent()).repaint();
		}
    }

	public static int getColorOfRGB(int r, int g, int b) 
	{
		return getColorOfRGB(r, g, b, Mobile.DoJaVersion >= 40 ? 255 : 0);
	}

	public static int getColorOfRGB(int r, int g, int b, int a) 
	{
		if (a < 0 || a > 255 || r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) { throw new IllegalArgumentException("RGB values must be between 0 and 255"); }

		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	public static int getColorOfName(int name) 
	{
		int alpha = Mobile.DoJaVersion >= 40 ? 0xFF000000 : 0x00000000;
		switch (name) 
		{
			case BLACK:     return 0x00000000 | alpha; // (0x00, 0x00, 0x00)
			case BLUE:      return 0x000000FF | alpha; // (0x00, 0x00, 0xff)
			case LIME:      return 0x0000FF00 | alpha; // (0x00, 0xff, 0x00)
			case AQUA:      return 0x0000FFFF | alpha; // (0x00, 0xff, 0xff)
			case RED:       return 0x00FF0000 | alpha; // (0xff, 0x00, 0x00)
			case FUCHSIA:   return 0x00FF00FF | alpha; // (0xff, 0x00, 0xff)
			case YELLOW:    return 0x00FFFF00 | alpha; // (0xff, 0xff, 0x00)
			case WHITE:     return 0x00FFFFFF | alpha; // (0xff, 0xff, 0xff)
			case GRAY:      return 0x00808080 | alpha; // (0x80, 0x80, 0x80)
			case NAVY:      return 0x00000080 | alpha; // (0x00, 0x00, 0x80)
			case GREEN:     return 0x00008000 | alpha; // (0x00, 0x80, 0x00)
			case TEAL:      return 0x00008080 | alpha; // (0x00, 0x80, 0x80)
			case MAROON:    return 0x00800000 | alpha; // (0x80, 0x00, 0x00)
			case PURPLE:    return 0x00800080 | alpha; // (0x80, 0x00, 0x80)
			case OLIVE:     return 0x00808000 | alpha; // (0x80, 0x80, 0x00)
			case SILVER:    return 0x00C0C0C0 | alpha; // (0xc0, 0xc0, 0xc0)
			default: throw new IllegalArgumentException("Illegal color name: " + name);
		}
	}

	public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) 
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		for (int i = 0; i < nPoints - 1; i++) 
		{
			drawLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
		}
	}

	public void drawPolyline(int[] xPoints, int[] yPoints, int offset, int count) 
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		for (int i = offset; i < offset + count - 1; i++) 
		{
			drawLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
		}
	}

	// Those Polygon methods are used by Gang Bullets 2 and Dragon Ball RPG
	public void fillPolygon(final int[] xPoints, final int[] yPoints, final int numPoints) 
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		fillPolygon(xPoints, 0, yPoints, 0, numPoints, (0xFF << 24) | getColor());
	}

	public void fillPolygon(final int[] xPoints, final int[] yPoints, final int offset, final int numPoints) 
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		fillPolygon(xPoints, offset, yPoints, offset, numPoints, (0xFF << 24) | getColor());
	}

	// Haven't found those in use, but if there's fillPolygon for DoJa, there must be drawPolygon too
	public void drawPolygon(final int[] xPoints, final int[] yPoints, final int numPoints) 
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		drawPolygon(xPoints, 0, yPoints, 0, numPoints, (0xFF << 24) | getColor());
	}

	public void drawPolygon(final int[] xPoints, final int[] yPoints, final int offset, final int numPoints) 
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		drawPolygon(xPoints, offset, yPoints, offset, numPoints, (0xFF << 24) | getColor());
	}

	public void drawScaledImage(com.nttdocomo.ui.Image image, int dx, int dy, int width, int height, int sx, int sy, int swidth, int sheight) 
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		try 
		{
			if (dojaflipMode != FLIP_NONE) 
			{
				int[] adjustedCoordinates = adjustCoordinates(image.getCanvas().getWidth(), image.getCanvas().getHeight(), sx, sy, width, height, dojaflipMode);
				sx      = adjustedCoordinates[0];
				sy      = adjustedCoordinates[1];
				swidth  = adjustedCoordinates[2];
				sheight = adjustedCoordinates[3];

				width  = adjustedCoordinates[2];
				height = adjustedCoordinates[3];
			}

			dx+=translateX;
			dy+=translateY;

			final int clipX = (getClipX() + translateX < 0) ? 0 : (getClipX() + translateX);
			final int clipY = (getClipY() + translateY < 0) ? 0 : (getClipY() + translateY);
			final int clipWidth = (getClipWidth() + getClipX() + translateX > canvasWidth) ? canvasWidth : (getClipWidth() + getClipX() + translateX);
			final int clipHeight = (getClipHeight() + getClipY() + translateY > canvasHeight) ? canvasHeight : (getClipHeight() + getClipY() + translateY);

			BufferedImage newImg = manipulateImage(image.getCanvas(), dojaflipMode);
			int[] imgData = ((DataBufferInt) newImg.getRaster().getDataBuffer()).getData();
			
			swidth = Math.min(swidth, image.getWidth());
			sheight = Math.min(sheight, image.getHeight());
			width = Math.min(width, image.getWidth());
			height = Math.min(height, image.getHeight());

			for (int j = dy; j < dy + height; j++) 
			{
				int srcY = sy + (j - dy) * sheight / height;
				for (int i = dx; i < dx + width; i++) 
				{ 
					int srcX = sx + (i - dx) * swidth / width;

					if (srcX >= sx && srcX < sx + swidth && srcY >= sy && srcY < sy + sheight && srcY * image.getWidth() + srcX >= 0 &&
					srcY * image.getWidth() + srcX < imgData.length-1 && j * canvasWidth + i >= 0 && j * canvasWidth + i < canvasData.length-1
					&& i < clipWidth && i >= clipX && j < clipHeight && j >= clipY) 
					{
						setPixel(i, j, blendPixels(imgData[srcY * image.getWidth() + srcX], canvasData[j * canvasWidth + i]));
					}
				}
			}
			//gc.drawImage(manipulateImage(image.getCanvas(), dojaflipMode), dx, dy, dx + width, dy + height, sx, sy, sx + swidth, sy + sheight, null);
		}
		catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "drawScaledImage: " + e.getMessage()); }
	}

	public void drawSpriteSet(com.nttdocomo.ui.SpriteSet sprites) 
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		Mobile.log(Mobile.LOG_WARNING, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "drawSpriteSet is untested ");

		for (com.nttdocomo.ui.Sprite sprite : sprites.getSprites())  // TODO: Support flip modes
		{
			drawRGB(sprite.getImage().getDataBuffer(), 0, sprite.getImage().getWidth(), sprite.getX(), sprite.getY(), sprite.getImage().getWidth(), sprite.getImage().getHeight(), true);
		}
	}

	public void drawImageMap(com.nttdocomo.ui.ImageMap map, int x, int y) 
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		Mobile.log(Mobile.LOG_WARNING, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "drawImageMap is untested ");

		map.setWindowLocation(x, y);
		
		map.draw((com.nttdocomo.ui.Graphics) this);
	}

	public void setFlipMode(int mode) 
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		switch(mode) 
		{
			case FLIP_HORIZONTAL:
			case FLIP_NONE:
			case FLIP_VERTICAL:
			case FLIP_ROTATE:
			case FLIP_ROTATE_LEFT:
			case FLIP_ROTATE_RIGHT:
			case FLIP_ROTATE_RIGHT_HORIZONTAL:
			case FLIP_ROTATE_RIGHT_VERTICAL:
				dojaflipMode = mode;
				break;
			default:
				throw new IllegalArgumentException("Invalid flip mode received: " + mode);
		}
	}

	// These are used in some DoJa versions of Gradius, like Gradius II
	public int getPixel(int x, int y) { return getRGBPixel(x, y); }

	public int getRGBPixel(int x, int y) 
	{ 
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }
		return canvasData[y*canvasWidth+x]; 
	}

	// These aren't documented, but some DoJa jars use them (space Manbow uses setRGBPixel right at the menu for example)
	// They don't seem all too different from lcdui Image's set/getPixel(s) as far as logic goes
	public void setPixel(int x, int y) 
	{ 
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }
		
		canvasData[y*canvasWidth+x] = getColor();
	}

	public void setPixel(int x, int y, int color) 
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }
		int restorecolor = getColor();
		setAlphaRGB(color);
		setPixel(x, y);
		setAlphaRGB(restorecolor);
	}

	public void setRGBPixel(int x, int y, int color) { setPixel(x, y, color); }

	// Used by Galaga for Mobage, doesn't seem correct yet
	public int[] getPixels(int x, int y, int width, int height, int[] array, int offset) 
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }
		if(array == null) { throw new NullPointerException("Null data array received"); }
		if(width < 0 || height < 0) { throw new IllegalArgumentException("Invalid value for width or height"); }
		if(offset < 0 || (offset + width*height) > array.length || (offset + width*height) < 0) { throw new ArrayIndexOutOfBoundsException("Requested range is out of bounds"); }
		
		getPixels(array, offset, width, x, y, width, height, DirectGraphics.TYPE_INT_8888_ARGB);
		return array;
	}

	public void setPixels(int x, int y, int width, int height, int[] array, int offset) 
	{
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }
		if(array == null) { throw new NullPointerException("Null data array received"); }
		if(width < 0 || height < 0) { throw new IllegalArgumentException("Invalid value for width or height"); }
		if(offset < 0 || (offset + width*height) > array.length || (offset + width*height) < 0) { throw new ArrayIndexOutOfBoundsException("Requested range is out of bounds"); }
		drawRGB(array, offset, width, x, y, width, height, Mobile.DoJaVersion >= 40);
	}

	// Not really found in use yet, but if there's get/setRGBPixel, there should be a get/setRGBPixels too.
	public void setRGBPixels(int x, int y, int width, int height, int[] array, int offset) 
	{
		setPixels(x, y, width, height, array, offset);
	}

	public int[] getRGBPixels(int x, int y, int width, int height, int[] array, int offset) 
	{
		return getPixels(x, y, width, height, array, offset);
	}

	public void setPictoColorEnabled(boolean b) 
	{ 
		if(contextDisposed) { throw new UIException(UIException.ILLEGAL_STATE, "This graphics context has been disposed"); }

		usePictoColor = b; 
	}

	// TODO: Different MascotCapsuleV3's Graphics3D implementations

	// DoJa's com.nttdocomo.opt.ui.j3d
	public void setViewTrans(com.nttdocomo.opt.ui.j3d.AffineTrans paramAffineTrans) 
	{

	}
	
	public void setScreenScale(int paramInt1, int paramInt2) 
	{

	}
	
	public void setScreenCenter(int paramInt1, int paramInt2) 
	{

	}
	
	public void drawFigure(com.nttdocomo.opt.ui.j3d.Figure paramFigure) 
	{

	}
	
	public void setSphereTexture(com.nttdocomo.opt.ui.j3d.Texture paramTexture) 
	{

	}
	
	public void enableLight(boolean paramBoolean) 
	{

	}
	
	public void enableSphereMap(boolean paramBoolean) 
	{

	}
	
	public void setAmbientLight(int paramInt) 
	{ 

	}
	
	public void setDirectionLight(com.nttdocomo.opt.ui.j3d.Vector3D paramVector3D, int paramInt) 
	{

	}
	
	public void enableSemiTransparent(boolean paramBoolean) 
	{

	}
	
	public void setClipRect3D(int paramInt1, int paramInt2, int paramInt3, int paramInt4) 
	{

	}

	public void setPerspective(int n1, int n2, int n3) 
	{

	}

	public void executeCommandList(int[] a) 
	{

	}

	public void renderPrimitives(com.nttdocomo.opt.ui.j3d.PrimitiveArray arr, int num) 
	{

	}

	public void flush() 
	{
		
	}

	// FPS COUNTER


	// For now, the logic here works by updating the framerate counter every second
	public final void showFPS() 
	{
		frameCount++;
		if (System.nanoTime() - lastFpsTime >= 1000000000)
		{ 
			fps = frameCount; 
			frameCount = 0; 
			lastFpsTime = System.nanoTime(); 
		}

		String fpsText = "FPS: " + fps;
		int scaledWidth = getFont().stringWidth(fpsText);
		int scaledHeight = getFont().getBaselinePosition();
		
		if(MobilePlatform.showFPS.equals("TopLeft"))          { setOrigin(2, 2); }
		else if(MobilePlatform.showFPS.equals("TopRight"))    { setOrigin(MobilePlatform.lcdWidth-scaledWidth-2, 2); }
		else if(MobilePlatform.showFPS.equals("BottomLeft"))  { setOrigin(2, MobilePlatform.lcdHeight-scaledHeight-2 - (MobilePlatform.focusCommandBar ? font.getHeight() : 0)); }
		else if(MobilePlatform.showFPS.equals("BottomRight")) { setOrigin(MobilePlatform.lcdWidth-scaledWidth-2, MobilePlatform.lcdHeight-scaledHeight-2 - (MobilePlatform.focusCommandBar ? font.getHeight() : 0)); }

		// Set the overlay background and draw
		setARGBColor(0x96000069); // BG is a semi-transparent dark blue
		fillRoundRect(0, 2, scaledWidth, scaledHeight, 4, 4); // Cut a bit off from the height so that the counter is slimmer. We're not using chars that go below baseline like 'f' or 'q'
		// Set the font color and draw it
		setAlphaRGB(0xFFFFAF00); // Text color is orange
		drawRoundRect(0, 2, scaledWidth, scaledHeight, 4, 4);
		drawString(fpsText, 0, 0, TOP | LEFT);
		setOrigin(0, 0);
		setColor(0, 0, 0);
	}

	public final void drawFastForwardIndicator()
	{
		int tmpColor = getColor();
		Font tmpFont = getFont();
		setAlphaRGB(0x90000000);
		gc.fillRect(0, 0, canvasWidth, canvasHeight);
		setFont(HUDFont);
		setColor(0xFFFFAF00);
		int x = (canvasWidth - HUDFont.stringWidth(fastForwardIndicator)) / 2;
		gc.drawString(fastForwardIndicator, x, HUDFont.getHeight());
		setColor(tmpColor);
		setFont(tmpFont);
	}

	public final void drawPauseIndicator()
	{
		int tmpColor = getColor();
		Font tmpFont = getFont();
		setAlphaRGB(0x90000000);
		gc.fillRect(0, 0, canvasWidth, canvasHeight);
		setFont(HUDFont);
		setColor(0xFFFFAF00);
		int x = (canvasWidth - HUDFont.stringWidth(pauseIndicator)) / 2;
		gc.drawString(pauseIndicator, x, HUDFont.getHeight());
		setColor(tmpColor);
		setFont(tmpFont);
	}

	// Helper methods
	protected static final int clamp(int value) { return Math.max(0, Math.min(255, value)); }

	protected static final float fastToRadians(float angdeg) { return angdeg * 0.017453292f; }
}
