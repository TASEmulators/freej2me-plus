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
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.lcdui.game.Sprite;

import com.nokia.mid.ui.DirectGraphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.DataBufferInt;
import java.awt.image.Kernel;

import com.nttdocomo.ui.UIException;

public abstract class PlatformGraphics implements DirectGraphics
{
	protected BufferedImage canvas;
	protected Graphics2D gc;
	protected int[] canvasData;
	protected int[] imgPixels;
	protected PlatformImage baseImage, lastImage;

	protected Color awtColor;

	// Gaussian blur kernel (7x7) for Motorola's FunLights
	protected static final float[] gaussianKernel = 
	{
		1f / 159,  2f / 159,  3f / 159,  2f / 159,  1f / 159, 0, 0,
		2f / 159,  5f / 159,  8f / 159,  5f / 159,  2f / 159, 0, 0,
		3f / 159,  8f / 159, 12f / 159,  8f / 159,  3f / 159, 0, 0,
		2f / 159,  5f / 159,  8f / 159,  5f / 159,  2f / 159, 0, 0,
		1f / 159,  2f / 159,  3f / 159,  2f / 159,  1f / 159, 0, 0,
		0,         0,         0,         0,         0,        0, 0,
		0,         0,         0,         0,         0,        0, 0
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


	protected int translateX = 0;
	protected int translateY = 0;

	protected int resetTransX = 0;
	protected int resetTransY = 0;
	private boolean firstReset = true;

	protected int color = 0xFFFFFF;
	protected Font font = Font.getDefaultFont();
	protected com.nttdocomo.ui.Font dojaFont = com.nttdocomo.ui.Font.getDefaultFont();
	protected int strokeStyle = SOLID;

	protected int dojaLockCount = 0;
	protected int dojaflipMode = 0;
	protected boolean usePictoColor = false;
	protected boolean contextDisposed = false;

	/* 
	 * Both DirectGraphics and Sprite's rotations are counter-clockwise, flipping
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

	public PlatformGraphics(PlatformImage image)
	{
		this.baseImage = image;
		canvas = image.getCanvas();
		gc = canvas.createGraphics();

		canvasData = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();

		gc.setClip(0, 0, canvas.getWidth(), canvas.getHeight());

		setColor(0,0,0);
		setStrokeStyle(SOLID);
		gc.setBackground(new Color(0, 0, 0, 0));
		gc.setFont(font.platformFont.awtFont);

		// Assuming we ever decide to implement configurable Java Graphics rendering options (2D smoothing, AA, etc), they should be applied here

		// Example: Enable font AA (GASP uses font resource information to apply AA when appropriate)
        //gc.getGraphics2D().setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
		gc.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	}

	public void reset() //Internal use method, resets the Graphics object to its inital values
	{
		reset(0, 0, canvas.getWidth(), canvas.getHeight());
	}
	
	public void reset(int clipx, int clipy, int clipw, int cliph) //Internal use method, resets the Graphics object to its inital values
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

	public void clearRect(int x, int y, int width, int height)
	{
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }

		gc.clearRect(x, y, width, height);
	}

	public void copyArea(int x_src, int y_src, int width, int height, int x_dest, int y_dest, int anchor) 
	{
		if (width <= 0 || height <= 0) { return; }
	
		x_dest = AnchorX(x_dest, width, anchor);
		y_dest = AnchorY(y_dest, height, anchor);
	
		x_src += getTranslateX();
		y_src += getTranslateY();
	
		// Check if the source area is within bounds before doing any draw operations
		if (x_src < 0 || y_src < 0 || 
			x_src + width > canvas.getWidth() || 
			y_src + height > canvas.getHeight()) {
			throw new IllegalArgumentException("Source area exceeds the bounds of the graphics object.");
		}
	
		/* 
		 * A neat trick here is that we don't need to check for types, as the copied
		 * subregion will always have the same data type as the original canvas it
		 * was copied from, be it INT_RGB, INT_ARGB, etc.
		 */
		// Create a data buffer to hold the copied pixel area
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

	public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle)
	{
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }

		if (width < 0 || height < 0) { return; }
		gc.drawArc(x, y, width, height, startAngle, arcAngle);
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

			gc.drawImage(image.getCanvas(), x, y, null);
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "drawImage A:"+e.getMessage());
		}
	}

	public void drawImage(Image image, int x, int y)
	{
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }
		try
		{
			gc.drawImage(image.getCanvas(), x, y, null);
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "drawImage B:"+e.getMessage());
		}
	}

	public void flushGraphics(PlatformImage image, int x, int y, int width, int height)
	{
		// called by MobilePlatform.flushGraphics/repaint

		// Ensure image's width and height are still positive
		if (width <= 0 || height <= 0) { return; }

		try
		{
			final int startX = Math.max(getClipX() - x, 0);
			final int endX = Math.min(getClipX() + getClipWidth() - x, width);
			final int startY = Math.max(getClipY() - y, 0);
			final int endY = Math.min(getClipY() + getClipHeight() - y, height);
			final int canvasWidth = canvas.getWidth();
			final int canvasHeight = canvas.getHeight();
			final int imageWidth = image.getWidth();
			int[] overlayData = null;

			// Only spend time reallocating this if we really are drawing from a different image than the last (speeds things up a bit)
			if(image != lastImage)
			{
				imgPixels = ((DataBufferInt) image.getCanvas().getRaster().getDataBuffer()).getData();
				lastImage = image;
			}

			boolean fastBlit = !Mobile.renderLCDMask && Mobile.maskIndex != 0 && !Mobile.funLightsEnabled;
			if (fastBlit && imgPixels == canvasData) {
				return;
			}

			// This one is rather costly, as it has to draw overlays on the corners of the screen with gaussian filtering applied.
			if(Mobile.funLightsEnabled)
			{
				overlayData = new int[width * height];
				drawFunLights(overlayData, width, height);
			}
		
			int destRowIndex, srcRowIndex, destIndex, srcIndex, srcAlpha, existingPixel, destAlpha, newAlpha, newRed, newGreen, newBlue;
			// Render the resulting image
			for (int j = startY + y; j < endY + y; j++) 
			{
				// If there's no masking or overlay needed, we can copy a whole row at once, which is faster
				if(fastBlit)
				{
					destRowIndex = j * canvasWidth + startX + x;
            		srcRowIndex = j * imageWidth + startX + x;
					System.arraycopy(imgPixels, srcRowIndex, canvasData, destRowIndex, endX - startX);
				}
				else
				{
					for (int i = startX + x; i < endX + x; i++) 
					{
						destIndex = j * canvasWidth + i;
						srcIndex = j * imageWidth + i;

						// Only apply the backlight mask if Display, nokia's DeviceControl, or others request it for backlight effects.
						canvasData[destIndex] = imgPixels[srcIndex] & (Mobile.renderLCDMask ? Mobile.lcdMaskColors[Mobile.maskIndex] : 0xFFFFFFFF);

						// If funLights overlay is requested by the game, apply its pixels to the screen area
						if(Mobile.funLightsEnabled) 
						{
							srcAlpha = (overlayData[srcIndex] >> 24) & 0xFF; // Source alpha
							existingPixel = canvasData[destIndex]; // Current pixel in the canvas
							destAlpha = (existingPixel >> 24) & 0xFF;
		
							// Blend alpha and color values using the srcOver alpha compositing method
							newAlpha = Math.min(255, srcAlpha + destAlpha);
							newRed = (((overlayData[srcIndex] >> 16) & 0xFF) * srcAlpha + ((existingPixel >> 16) & 0xFF) * (255 - srcAlpha)) / newAlpha;
							newGreen = (((overlayData[srcIndex] >> 8) & 0xFF) * srcAlpha + ((existingPixel >> 8) & 0xFF) * (255 - srcAlpha)) / newAlpha;
							newBlue = ((overlayData[srcIndex] & 0xFF) * srcAlpha + (existingPixel & 0xFF) * (255 - srcAlpha)) / newAlpha;

							canvasData[destIndex] = (newAlpha << 24) | (newRed << 16) | (newGreen << 8) | newBlue;
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			// Games can try to render offscreen even at the correct resolution, so this makes more sense as a debug log
			Mobile.log(Mobile.LOG_DEBUG, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "flushGraphics A:"+e.getMessage());
		}
	}

	public void drawRegion(Image image, int subx, int suby, int subw, int subh, int transform, int x, int y, int anchor)
	{
		if (subw <= 0 || subh <= 0) { return; }

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
				gc.drawImage(image.getCanvas(), x, y, x + subw, y + subh, subx, suby, subx + subw, suby + subh, null);
			}
			else
			{
				PlatformImage sub = new PlatformImage(image, subx, suby, subw, subh, transform);
				x = AnchorX(x, sub.getWidth(), anchor);
				y = AnchorY(y, sub.getHeight(), anchor);
				gc.drawImage(sub.getCanvas(), x, y, null);
			}
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "drawRegion A (x:"+x+" y:"+y+" w:"+subw+" h:"+subh+"):"+e.getMessage());
		}
	}

	public void drawRegion(Image image, int subx, int suby, int subw, int subh, int transform, int x, int y, int width_dest, int height_dest, int anchor, int stretch_quality) 
	{
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
		if (width <= 0 || height <= 0) { return; }
		if (rgbData == null) { throw new NullPointerException(); }
		if (offset < 0 || offset >= rgbData.length) { throw new ArrayIndexOutOfBoundsException(); }
	
		if (scanlength > 0) 
		{
			if (offset + scanlength * (height - 1) + width > rgbData.length) 
			{
				throw new ArrayIndexOutOfBoundsException("DrawRGB Area is out of bounds (scanlength " + scanlength + ")");
			}
		} else 
		{
			if (offset + width > rgbData.length || offset + scanlength * (height - 1) < 0) 
			{
				throw new ArrayIndexOutOfBoundsException("DrawRGB Area is out of bounds (scanlength " + scanlength + ")");
			}
		}
	
		if(!Mobile.compatDoNotTranslateDrawRGB) 
		{
			x += getTranslateX();
			y += getTranslateY();
		}
	
		final int canvasWidth = canvas.getWidth();
		final int clipX = Math.max(getClipX(), 0);
		final int clipY = Math.max(getClipY(), 0);
		final int clipWidth = Math.min(getClipWidth()+clipX, canvasWidth);
		final int clipHeight = Math.min(getClipHeight()+clipY, canvas.getHeight());
	
		int rowOffset, destRow, pixelIndex, destIndex, pixel, srcAlpha, existingPixel, destAlpha, newAlpha, newRed, newGreen, newBlue;
		// Directly manipulate the canvasData
		for (int j = 0; j < height; j++) // The array's x and y positions start from 0, as the offset is what dictates where the data should start being read from
		{
			if ((y + j - getTranslateY()) < clipY || (y + j - getTranslateY()) >= clipHeight || (y + j) >= canvas.getHeight()) { continue; }
			rowOffset = offset + (j * scanlength); // Calculate the starting index for the current row
			destRow = (y + j) * canvasWidth;
	
			for (int i = 0; i < width; i++)
			{
				if ((x + i - getTranslateX()) < clipX || (x + i - getTranslateX()) >= clipWidth || (x + i) >= canvasWidth) { continue; }

				pixelIndex = rowOffset + i; // Source index in rgbData
				destIndex = destRow + x + i;

				pixel = rgbData[pixelIndex];
				if (!processAlpha) { canvasData[destIndex] = pixel | 0xFF000000; } // Set pixel as fully opaque
				else // Handle alpha blending
				{
					srcAlpha = (pixel >> 24) & 0xFF; // Source alpha
					if(srcAlpha == 255) { canvasData[destIndex] = pixel; }
					else if (srcAlpha > 0)
					{
						existingPixel = canvasData[destIndex]; // Current pixel in the canvas
						destAlpha = (existingPixel >> 24) & 0xFF;
	
						// Blend alpha and color values using the srcOver alpha compositing method
						newAlpha = Math.min(255, srcAlpha + destAlpha);
						newRed = (((pixel >> 16) & 0xFF) * srcAlpha + ((existingPixel >> 16) & 0xFF) * (255 - srcAlpha)) / newAlpha;
						newGreen = (((pixel >> 8) & 0xFF) * srcAlpha + ((existingPixel >> 8) & 0xFF) * (255 - srcAlpha)) / newAlpha;
						newBlue = ((pixel & 0xFF) * srcAlpha + (existingPixel & 0xFF) * (255 - srcAlpha)) / newAlpha;
	
						// Store the new pixel back in canvasData
						canvasData[destIndex] = (newAlpha << 24) | (newRed << 16) | (newGreen << 8) | newBlue;
					}
				}
			}
		}
	}

	public void drawLine(int x1, int y1, int x2, int y2) 
	{ 
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }
		
		gc.drawLine(x1, y1, x2, y2); 
	}

	public void drawRect(int x, int y, int width, int height)
	{
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }
		if (width < 0 || height < 0) { return; }

		gc.drawRect(x, y, width, height);
	}

	public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight)
	{
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }

		if (width < 0 || height < 0) { return; }

		gc.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
	}

	public void drawString(String str, int x, int y, int anchor)
	{
		if(str!=null)
		{
			x = AnchorX(x, gc.getFontMetrics().stringWidth(str), anchor);
			int ascent = gc.getFontMetrics().getAscent();
			int height = gc.getFontMetrics().getHeight();

			y += ascent - 1;
			
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
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }

		if (width <= 0 || height <= 0) { return; }

		gc.fillArc(x, y, width, height, startAngle, arcAngle);
	}

	public void fillRect(int x, int y, int width, int height)
	{
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }

		if (width <= 0 || height <= 0) { return; }

		gc.fillRect(x, y, width, height);
	}

	public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight)
	{
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }

		if (width < 0 || height < 0) { return; }

		gc.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
	}

	public void setColor(int rgb)
	{
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }

		setColor((rgb>>16) & 0xFF, (rgb>>8) & 0xFF, rgb & 0xFF);
	}

	public void setColor(int r, int g, int b)
	{
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }

		color = (r<<16) + (g<<8) + b;
		awtColor = new Color(r, g, b);
		gc.setColor(awtColor);
	}

	public void setGrayScale(int value) { setColor(value, value, value); }

	public int getGrayScale() 
	{
		int r = gc.getColor().getRed();
		int g = gc.getColor().getGreen();
		int b = gc.getColor().getBlue();

		return 0x4CB2 * r + 0x9691 * g + 0x1D3E * b >> 16;
	}

	public int getRedComponent() { return gc.getColor().getRed(); }

	public int getGreenComponent() { return gc.getColor().getGreen(); }

	public int getBlueComponent() { return gc.getColor().getBlue(); }

	public int getColor() 
	{
		return (gc.getColor().getRed() << 16) | (gc.getColor().getGreen() << 8) | gc.getColor().getBlue();
	}

	public int getDisplayColor(int color) { return color; }

	public Font getFont() { return font; }

	public void setStrokeStyle(int stroke) 
	{
		if (strokeStyle == DOTTED) 
		{
			float[] dotPattern = {2.0f, 2.0f}; // Dot of length 2 px, followed by 2 px of gap
			BasicStroke dottedStroke = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dotPattern, 0.0f);
			
			gc.setStroke(dottedStroke); 
		} 
		else { gc.setStroke(new BasicStroke(1.0f)); } // Solid stroke with width of 2 px

		strokeStyle = stroke;
	}

	public int getStrokeStyle() { return strokeStyle;}

	public void setFont(Font font)
	{
		if(font == null) { font = Font.getDefaultFont(); }
		this.font = font;
		gc.setFont(font.platformFont.awtFont);
	}

	public void setClip(int x, int y, int width, int height)
	{
		if(!Mobile.isDoJa) { gc.setClip(x, y, width, height); }
		else { gc.setClip(x-getTranslateX(), y-getTranslateY(), width, height); }
	}

	public void clipRect(int x, int y, int width, int height)
	{
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }

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
		gc.setColor(new Color(ARGB, true));
	}

	/*
		****************************
			Nokia Direct Graphics
		****************************
	*/
	// http://www.j2megame.org/j2meapi/Nokia_UI_API_1_1/com/nokia/mid/ui/DirectGraphics.html

	private int colorAlpha;

	public int getNativePixelFormat() { return 0; } // Don't explicitly set any native format for color, let the jar send in whatever it has and we'll convert.

	public int getAlphaComponent() { return colorAlpha; }

	public void setARGBColor(int argbColor)
	{
		colorAlpha = (argbColor>>>24) & 0xFF;
		setAlphaRGB(argbColor);
	}

	public void drawImage(javax.microedition.lcdui.Image img, int x, int y, int anchor, int manipulation)
	{
		setClip(getClipX()-getTranslateX(), getClipY()-getTranslateY(), getClipWidth(), getClipHeight());

		BufferedImage image = manipulateImage(img.getCanvas(), manipulation);
		x = AnchorX(x, image.getWidth(), anchor);
		y = AnchorY(y, image.getHeight(), anchor);
		gc.drawImage(image, x, y, null);

		// Restore clip back to original value
		setClip(getClipX()+getTranslateX(), getClipY()+getTranslateY(), getClipWidth(), getClipHeight());
	}

	public void drawPixels(byte[] pixels, byte[] transparencyMask, int offset, int scanlength, int x, int y, int width, int height, int manipulation, int format)
	{
		if (width == 0 || height == 0) { return; }
		if (width < 0 || height < 0) { throw new IllegalArgumentException("drawPixels(byte) received negative width or height"); }
		if (pixels == null) { throw new NullPointerException("drawPixels(byte) received a null pixel array"); }
		if (offset < 0 || offset >= (pixels.length * 8)) { throw new ArrayIndexOutOfBoundsException("drawPixels(byte) index out of bounds:" + width + " * " + height + "| pixels len:" + (pixels.length * 8) + "| offset:" + offset); }

		int[] Type1 = {0xFFFFFFFF, 0xFF000000, 0x00FFFFFF, 0x00000000};
		int c = 0;
		BufferedImage temp = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);// Nokia DirectGraphics states that image width and height CAN be zero.
		int[] data = ((DataBufferInt) temp.getRaster().getDataBuffer()).getData();

		switch (format) 
		{
			case DirectGraphics.TYPE_BYTE_1_GRAY_VERTICAL: // TYPE_BYTE_1_GRAY_VERTICAL - Used by Munkiki's Castles
				int ods = offset / scanlength;
				int oms = offset % scanlength;
				int b = ods % 8; // Bit offset in a byte, since GRAY_VERTICAL is packing 8 vertical pixel bits in a byte.
				for (int yj = 0; yj < height; yj++) 
				{
					int ypos = yj * width;
					int tmp = (ods + yj) / 8 * scanlength + oms;
					for (int xj = 0; xj < width; xj++) 
					{
						if(tmp + xj >= pixels.length) { continue; } // Ignore if accessing out of bounds
						c = ((pixels[tmp + xj] >> b) & 1);
						if (transparencyMask != null) 
						{
							c |= (((transparencyMask[tmp + xj] >> b) & 1) ^ 1) << 1; // Apply transparency mask
						}
						data[ypos + xj] = Type1[c]; // Set pixel directly in the DataBuffer also removing the need for setDataElements
					}
					b++;
					if (b > 7) b = 0;
				}
				break;
	
			case DirectGraphics.TYPE_BYTE_1_GRAY: // TYPE_BYTE_1_GRAY - Also used by Munkiki's Castles
				b = 7 - offset % 8;
				for (int yj = 0; yj < height; yj++) 
				{
					int line = offset + yj * scanlength;
					int ypos = yj * width;
					for (int xj = 0; xj < width; xj++) 
					{
						if((line + xj) / 8 >= pixels.length) { continue; } // Ignore if accessing out of bounds
						c = ((pixels[(line + xj) / 8] >> b) & 1);
						if (transparencyMask != null) 
						{
							c |= (((transparencyMask[(line + xj) / 8] >> b) & 1) ^ 1) << 1; // Apply transparency mask
						}
						data[ypos + xj] = Type1[c];
						b--;
						if (b < 0) b = 7;
					}
					b = b - (scanlength - width) % 8;
					if (b < 0) b = 8 + b;
				}
				break;

			default: Mobile.log(Mobile.LOG_WARNING, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "drawPixels A : Format " + format + " Not Implemented");
		}

		temp = manipulateImage(temp, manipulation);
		data = ((DataBufferInt) temp.getRaster().getDataBuffer()).getData();

		drawRGB(data, 0, temp.getWidth(), x, y, temp.getWidth(), temp.getHeight(), true);
	}

	public void drawPixels(int[] pixels, boolean transparency, int offset, int scanlength, int x, int y, int width, int height, int manipulation, int format) 
	{
		if (width == 0 || height == 0) { return; }
		if (width < 0 || height < 0) { throw new IllegalArgumentException("drawPixels(int) received negative width or height"); }
		if (pixels == null) { throw new NullPointerException("drawPixels(int) received a null pixel array"); }
		if (offset < 0 || offset >= pixels.length) { throw new ArrayIndexOutOfBoundsException("drawPixels(int) index out of bounds:" + width + " * " + height + "| len:" + pixels.length); }

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
		if (width == 0 || height == 0) { return; }
		if (width < 0 || height < 0) { throw new IllegalArgumentException("drawPixels(short) received negative width or height"); }
		if (pixels == null) { throw new NullPointerException("drawPixels(short) received a null pixel array"); }
		if (offset < 0 || offset >= pixels.length) { throw new ArrayIndexOutOfBoundsException("drawPixels(short) index out of bounds:" + width + " * " + height + "| len:" + pixels.length); }

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
				if (!transparency) { data[row * width + col] = (data[row * width + col] & 0x00FFFFFF) | 0xFF000000; } // Set alpha to 255
			}
		}

		temp = manipulateImage(temp, manipulation);
		data = ((DataBufferInt) temp.getRaster().getDataBuffer()).getData();

		drawRGB(data, 0, temp.getWidth(), x, y, temp.getWidth(), temp.getHeight(), transparency);
	}

	public void drawPolygon(int[] xPoints, int xOffset, int[] yPoints, int yOffset, int nPoints, int argbColor)
	{
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }

		int temp = color;
		int[] x = new int[nPoints];
		int[] y = new int[nPoints];

		setAlphaRGB(argbColor);

		for(int i=0; i<nPoints; i++)
		{
			x[i] = xPoints[xOffset+i];
			y[i] = yPoints[yOffset+i];
		}
		gc.drawPolygon(x, y, nPoints);
		setColor(temp);
	}

	public void drawTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int argbColor)
	{
		int temp = color;
		setAlphaRGB(argbColor);
		gc.drawPolygon(new int[]{x1,x2,x3}, new int[]{y1,y2,y3}, 3);
		setColor(temp);
	}

	public void fillPolygon(int[] xPoints, int xOffset, int[] yPoints, int yOffset, int nPoints, int argbColor)
	{
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }

		int temp = color;
		int[] x = new int[nPoints];
		int[] y = new int[nPoints];

		setAlphaRGB(argbColor);

		for(int i=0; i<nPoints; i++)
		{
			x[i] = xPoints[xOffset+i];
			y[i] = yPoints[yOffset+i];
		}
		gc.fillPolygon(x, y, nPoints);
		setColor(temp);
	}

	public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3)
	{
		gc.fillPolygon(new int[]{x1,x2,x3}, new int[]{y1,y2,y3}, 3);
	}

	public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int argbColor)
	{
		int temp = color;
		setAlphaRGB(argbColor);
		gc.fillPolygon(new int[]{x1,x2,x3}, new int[]{y1,y2,y3}, 3);
		setColor(temp);
	}

	public void getPixels(byte[] pixels, byte[] transparencyMask, int offset, int scanlength, int x, int y, int width, int height, int format)
	{
		if (width <= 0 || height <= 0) { return; } // We have no pixels to copy
		if (pixels == null) { throw new NullPointerException("Byte array cannot be null");}
		if (x < 0 || y < 0 || x + width > canvas.getWidth() || y + height > canvas.getHeight()) 
		{
			throw new IllegalArgumentException("Requested copy area exceeds bounds of the image");
		}
		if (Math.abs(scanlength) < width) { throw new IllegalArgumentException("scanlength must be >= width");}
	
		// Just like DrawPixels(byte), we only handle BYTE_1_GRAY_VERTICAL and BYTE_1_GRAY yet
		switch (format) 
		{
			case DirectGraphics.TYPE_BYTE_1_GRAY_VERTICAL:
				for (int row = 0; row < height; row++) 
				{
					for (int col = 0; col < width; col++) 
					{
						int pixelIndex = (y + row) * canvas.getWidth() + (x + col);
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

			case DirectGraphics.TYPE_BYTE_1_GRAY: // Pretty similar to the one above
				for (int row = 0; row < height; row++) 
				{
					for (int col = 0; col < width; col++) 
					{
						int pixelIndex = (y + row) * canvas.getWidth() + (x + col);
						int pixelValue = canvasData[pixelIndex];
						int byteIndex = (offset / 8) + ((row * width + col) / 8);
						int bitIndex = (row * width + col) % 8;

						pixels[byteIndex] |= ((pixelValue & 0xFF) != 0 ? 0 : 1) << (7 - bitIndex);
						if(transparencyMask != null) { transparencyMask[byteIndex] |= ((pixelValue & 0xFF000000) != 0 ? 0 : 1) << (7 - bitIndex); }
					}
				}
				break;

			default: Mobile.log(Mobile.LOG_WARNING, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "getPixels A : Format " + format + " Not Implemented");
		}
	}

	public void getPixels(int[] pixels, int offset, int scanlength, int x, int y, int width, int height, int format)
	{
		if (width <= 0 || height <= 0) { return; } // We have no pixels to copy
		if (pixels == null) { throw new NullPointerException("int array cannot be null"); }
		if (x < 0 || y < 0 || x + width > canvas.getWidth() || y + height > canvas.getHeight()) 
		{
			throw new IllegalArgumentException("Requested copy area exceeds bounds of the image");
		}
		
		canvas.getRGB(x, y, width, height, pixels, offset, scanlength);
	}

	public void getPixels(short[] pixels, int offset, int scanlength, int x, int y, int width, int height, int format)
	{
		if (width <= 0 || height <= 0) { return; } // We have no pixels to copy
		if (pixels == null) { throw new NullPointerException("short array cannot be null"); }
		if (x < 0 || y < 0 || x + width > canvas.getWidth() || y + height > canvas.getHeight()) 
		{
			throw new IllegalArgumentException("Requested copy area exceeds bounds of the image");
		}
		
		for(int row=0; row<height; row++)
		{
			for (int col=0; col<width; col++)
			{
				int pixelIndex = offset + col + (row * scanlength);
				pixels[pixelIndex] = colorToShortPixel(canvasData[col+x + (row+y)*canvas.getWidth()], format);
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
		int out = 0;

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
				out = (a << 15) | (r << 10) | (g << 5) | b;
				break;
			case DirectGraphics.TYPE_USHORT_444_RGB:
				r = (c >> 20) & 0xF;
				g = (c >> 12) & 0xF;
				b = (c >> 4) & 0xF;
				out = (r << 8) | (g << 4) | b;
				break;
			case DirectGraphics.TYPE_USHORT_4444_ARGB:
				a = (c >>> 28) & 0xF;
				r = (c >> 20) & 0xF;
				g = (c >> 12) & 0xF;
				b = (c >> 4) & 0xF;
				out = (a << 12) | (r << 8) | (g << 4) | b;
				break;
			case DirectGraphics.TYPE_USHORT_555_RGB:
				r = (c >> 19) & 0x1F;
				g = (c >> 11) & 0x1F;
				b = (c >> 3) & 0x1F;
				out = (r << 10) | (g << 5) | b;
				break;
			case DirectGraphics.TYPE_USHORT_565_RGB:
				r = (c >> 19) & 0x1F;
				g = (c >> 10) & 0x3F;
				b = (c >> 3) & 0x1F;
				out = (r << 11) | (g << 5) | b;
				break;
			default:
				throw new IllegalArgumentException("Unsupported format: " + format);
		}
		return (short) out;
	}

	private static final BufferedImage manipulateImage(final BufferedImage image, final int manipulation)
	{
		// Return early if there's no manipulation to be done
		if(manipulation == 0 || manipulation == HV180) { return image; }
		
		switch(manipulation)
		{
			case V180:
            case DirectGraphics.FLIP_HORIZONTAL:
                return PlatformImage.transformImage(image, Sprite.TRANS_MIRROR);
            case H180:
            case DirectGraphics.FLIP_VERTICAL:
                return PlatformImage.transformImage(image, Sprite.TRANS_MIRROR_ROT180);
			case HV270:
            case DirectGraphics.ROTATE_90:
                return PlatformImage.transformImage(image, Sprite.TRANS_ROT270);
			case HV:
            case DirectGraphics.ROTATE_180:
                return PlatformImage.transformImage(image, Sprite.TRANS_ROT180);
			case HV90:
            case DirectGraphics.ROTATE_270:
                return PlatformImage.transformImage(image, Sprite.TRANS_ROT90);
            case V270:
            case H90:
                return PlatformImage.transformImage(image, Sprite.TRANS_MIRROR_ROT90);
            case V90:
            case H270:
                return PlatformImage.transformImage(image, Sprite.TRANS_MIRROR_ROT270);
            default:
				Mobile.log(Mobile.LOG_WARNING, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "manipulateImage "+manipulation+" not defined");
		}

		return image;
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
						float kernelWeight = gaussianKernel[kx + kernelRadius];

						r += ((pixelColor >> 16) & 0xff) * kernelWeight;
						g += ((pixelColor >> 8) & 0xff) * kernelWeight;
						b += (pixelColor & 0xff) * kernelWeight;
						a += ((pixelColor >> 24) & 0xff) * kernelWeight;
						weightSum += kernelWeight;
					}
				}
	
				int newAlpha = Math.min(255, (int)(a / weightSum));
				int newRed = Math.min(255, (int)(r / weightSum));
				int newGreen = Math.min(255, (int)(g / weightSum));
				int newBlue = Math.min(255, (int)(b / weightSum));
	
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
						float kernelWeight = gaussianKernel[ky + kernelRadius];

						r += ((pixelColor >> 16) & 0xff) * kernelWeight;
						g += ((pixelColor >> 8) & 0xff) * kernelWeight;
						b += (pixelColor & 0xff) * kernelWeight;
						a += ((pixelColor >> 24) & 0xff) * kernelWeight;
						weightSum += kernelWeight;
					}
				}
	
				int newAlpha = Math.min(255, (int)(a / weightSum));
				int newRed = Math.min(255, (int)(r / weightSum));
				int newGreen = Math.min(255, (int)(g / weightSum));
				int newBlue = Math.min(255, (int)(b / weightSum));
	
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
		imgPixels = null;
		baseImage = null;
		lastImage = null;
		canvas = null;
		gc.dispose();
	}

	// This has to create a copy of the current graphics context, translation, clip, etc included
	public com.nttdocomo.ui.Graphics copy() 
	{ 
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }

		com.nttdocomo.ui.Graphics newGc = new com.nttdocomo.ui.Graphics(this.baseImage);

		newGc.translate(getTranslateX(), getTranslateY());
		newGc.setClip(getClipX(), getClipY(), getClipWidth(), getClipHeight());
		newGc.setARGBColor(gc.getColor().getRGB());
		newGc.setStrokeStyle(getStrokeStyle());

		return newGc;
	}

	public void copyArea(int x, int y, int width, int height, int dx, int dy) 
	{
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }

		copyArea(x, y, width, height, dx, dy, 0);
	}

	// Text appears to be rendered with BOTTOM LEFT anchoring, at least, it's what most DoJa jars seem to like better
	public void drawChars(char[] data, int x, int y, int offset, int length)
	{
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }

		if(data == null) { throw new NullPointerException("Null char array received"); }
		if(offset < 0 || length < 0 || offset+length >= data.length) { throw new StringIndexOutOfBoundsException("invalid length and/or position received"); }
		drawChars(data, offset, length, x, y, BOTTOM | LEFT);
	}

	public void drawString(String str, int x, int y)
	{
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }
		if(str!=null) { drawString(str, x, y, BOTTOM | LEFT); }
		else { throw new NullPointerException("Null string received"); }
	}

	public void drawImage(com.nttdocomo.ui.Image image, int[] matrix) 
	{
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }
		try 
		{
			float[] fmatrix = new float[matrix.length];
			System.arraycopy(matrix, 0, fmatrix, 0, matrix.length);
			AffineTransform transform = new AffineTransform(fmatrix);

			gc.setTransform(transform);
			gc.drawImage(image.getCanvas(), 0, 0, null);
		} 
		catch (Exception e) 
		{
			Mobile.log(Mobile.LOG_ERROR, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "drawImage with matrix: " + e.getMessage());
		}
	}

	public void drawImage(com.nttdocomo.ui.Image image, int[] matrix, int sx, int sy, int width, int height) 
	{
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }
		try 
		{
			float[] fmatrix = new float[matrix.length];
			System.arraycopy(matrix, 0, fmatrix, 0, matrix.length);
			AffineTransform transform = new AffineTransform(fmatrix);

			gc.setTransform(transform);
			gc.drawImage(image.getCanvas(), sx, sy, sx + width, sy + height, null);
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
		switch (transform) 
		{
			case FLIP_HORIZONTAL:
				sx = imageWidth - sx - width; 
				break;

			case FLIP_VERTICAL:
				sy = imageHeight - sy - height; 
				break;

			// TODO: From here on out, all cases are untested
			case FLIP_ROTATE_RIGHT:
				int tempX = sx;
				sx = sy;
				sy = imageHeight - tempX - height; 
				int tempHeight = height;
				height = width; 
				width = tempHeight;
				break;

			case FLIP_ROTATE_LEFT:
				int tempY = sy;
				sy = imageWidth - sx - width; 
				sx = tempY;
				tempHeight = height;
				height = width; 
				width = tempHeight; 
				break;

			case FLIP_ROTATE:
				sx = imageWidth - sx - width; 
				sy = imageHeight - sy - height;
				break;

			case FLIP_ROTATE_RIGHT_VERTICAL:
				sx = imageWidth - sx - width;
				int temp = sy;
				sy = sx; 
				sx = imageHeight - temp - height; 
				break;

			case FLIP_ROTATE_RIGHT_HORIZONTAL:
				sy = imageHeight - sy - height; 
				temp = sx;
				sx = imageHeight - temp - width; 
				break;
		}

		// Return adjusted coordinates via reference parameters
    	return new int[]{sx, sy, width, height};
	}

	public void setOrigin(int x, int y) 
	{
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }
		
		translate(x-getTranslateX(), y-getTranslateY()); // Reset from previous translation
	}

	public void clearClip() 
	{ 
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }
		
		setClip(0, 0, canvas.getWidth(), canvas.getHeight()); 
	}

	public void setFont(com.nttdocomo.ui.Font dojaFont) 
	{
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }

		if(dojaFont == null) { dojaFont = com.nttdocomo.ui.Font.getDefaultFont(); }
		this.dojaFont = dojaFont;
		gc.setFont(dojaFont.platformFont.awtFont);
	}

	public void lock() 
	{ 
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }

		dojaLockCount++; 
	}

    public void unlock(boolean forced)
	{
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }

		dojaLockCount = forced ? 0 : dojaLockCount-1;
		
		if (dojaLockCount == 0 && com.nttdocomo.ui.Display.getCurrent() instanceof com.nttdocomo.ui.Canvas) 
		{
			((com.nttdocomo.ui.Canvas) com.nttdocomo.ui.Display.getCurrent()).repaint();
		}
    }

	public static int getColorOfRGB(int r, int g, int b) 
	{
		if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) { throw new IllegalArgumentException("RGB values must be between 0 and 255"); }

		// TODO: DoJa 4.0 allows negative values, so we should use 0xFF for alpha
		return (0 << 24) | (r << 16) | (g << 8) | b;
	}

	public static int getColorOfRGB(int a, int r, int g, int b) 
	{
		if (a < 0 || a > 255 || r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) { throw new IllegalArgumentException("RGB values must be between 0 and 255"); }

		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	public static int getColorOfName(int name) 
	{
		// TODO: DoJa 4.0 has transparency, so we should make this mask 0xFF000000 in such cases
		int alpha = 0x0000000;
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
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }

		for (int i = 0; i < nPoints - 1; i++) 
		{
			drawLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
		}
	}

	public void drawPolyline(int[] xPoints, int[] yPoints, int offset, int count) 
	{
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }

		for (int i = offset; i < offset + count - 1; i++) 
		{
			drawLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
		}
	}

	public void drawScaledImage(com.nttdocomo.ui.Image image, int dx, int dy, int width, int height, int sx, int sy, int swidth, int sheight) 
	{
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }

		try 
		{
			if (dojaflipMode != FLIP_NONE) 
			{
				int[] adjustedCoordinates = adjustCoordinates(image.getCanvas().getWidth(), image.getCanvas().getHeight(), sx, sy, width, height, dojaflipMode);
				sx     = adjustedCoordinates[0];
				sy     = adjustedCoordinates[1];
				width  = adjustedCoordinates[2];
				height = adjustedCoordinates[3];
			}
			gc.drawImage(manipulateDoJaImage(image.getCanvas(), dojaflipMode), dx, dy, dx + width, dy + height, sx, sy, sx + swidth, sy + sheight, null);
		}
		catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "drawScaledImage: " + e.getMessage()); }
	}

	public void drawSpriteSet(com.nttdocomo.ui.SpriteSet sprites) 
	{
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }

		for (com.nttdocomo.ui.Sprite sprite : sprites.getSprites())  // TODO: Support flip modes
		{
			gc.drawImage(sprite.getImage().getCanvas(), sprite.getX(), sprite.getY(), null);
		}
	}

	public void drawImageMap(com.nttdocomo.ui.ImageMap map, int x, int y) 
	{
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }

		map.setWindowLocation(x, y);
		
		map.draw((com.nttdocomo.ui.Graphics) this);
	}

	public void setFlipMode(int mode) 
	{
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }

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
	public int getPixel(int x, int y) { return this.getRGBPixel(x, y); }

	public int getRGBPixel(int x, int y) { return canvasData[y*canvas.getWidth()+x]; }

	// These aren't documented, but some DoJa jars use them (space Manbow uses setRGBPixel right at the menu for example)
	// They don't seem all too different from lcdui Image's set/getPixel(s) as far as logic goes
	public void setPixel(int x, int y) { canvasData[y*canvas.getWidth()+x] = getColor(); }

	public void setPixel(int x, int y, int color) 
	{
		int restorecolor = getColor();
		setColor(color);
		setPixel(x, y);
		setColor(restorecolor);
	}

	public void setRGBPixel(int x, int y, int color) { setPixel(x, y, color); }

	public void setPictoColorEnabled(boolean b) 
	{ 
		if(contextDisposed) { throw new UIException(1, "This graphics context has been disposed"); }

		usePictoColor = b; 
	}

	private static final BufferedImage manipulateDoJaImage(final BufferedImage image, final int manipulation)
	{
		// Return early if there's no manipulation to be done
		if(manipulation == FLIP_NONE) { return image; }
		
		switch(manipulation)
		{
			case FLIP_HORIZONTAL:
                return PlatformImage.transformImage(image, Sprite.TRANS_MIRROR);
			case FLIP_VERTICAL:
                return PlatformImage.transformImage(image, Sprite.TRANS_MIRROR_ROT180);
			case FLIP_ROTATE_RIGHT:
                return PlatformImage.transformImage(image, Sprite.TRANS_ROT270);
			case FLIP_ROTATE:
                return PlatformImage.transformImage(image, Sprite.TRANS_ROT180);
			case FLIP_ROTATE_LEFT:
                return PlatformImage.transformImage(image, Sprite.TRANS_ROT90);
			case FLIP_ROTATE_RIGHT_VERTICAL:
                return PlatformImage.transformImage(image, Sprite.TRANS_MIRROR_ROT90);
			case FLIP_ROTATE_RIGHT_HORIZONTAL:
                return PlatformImage.transformImage(image, Sprite.TRANS_MIRROR_ROT270);
            default:
				Mobile.log(Mobile.LOG_WARNING, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "manipulateImage "+manipulation+" not defined");
		}

		return image;
	}
}
