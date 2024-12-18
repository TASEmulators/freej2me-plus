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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.DataBufferInt;
import java.awt.image.Kernel;

public class PlatformGraphics extends javax.microedition.lcdui.Graphics implements DirectGraphics
{
	protected BufferedImage canvas;
	protected Graphics2D gc;

	protected Color awtColor;

	// Gaussian blur kernel (7x7) for Motorola's FunLights
	final float[] gaussianKernel = 
	{
		1f / 159,  2f / 159,  3f / 159,  2f / 159,  1f / 159, 0, 0,
		2f / 159,  5f / 159,  8f / 159,  5f / 159,  2f / 159, 0, 0,
		3f / 159,  8f / 159, 12f / 159,  8f / 159,  3f / 159, 0, 0,
		2f / 159,  5f / 159,  8f / 159,  5f / 159,  2f / 159, 0, 0,
		1f / 159,  2f / 159,  3f / 159,  2f / 159,  1f / 159, 0, 0,
		0,         0,         0,         0,         0,         0, 0,
		0,         0,         0,         0,         0,         0, 0
	};

	/* 
	 * Both DirectGraphics and Sprite's rotations are counter-clockwise, flipping
	 * an image horizontally is done by multiplying its height or width scale
	 * by -1 respectively. Flipping vertically is the same as flipping horizontally, 
	 * and then rotating by 180 degrees.
	 */
	private static final int HV = DirectGraphics.FLIP_HORIZONTAL | DirectGraphics.FLIP_VERTICAL;
	private static final int HV90 = DirectGraphics.FLIP_HORIZONTAL | DirectGraphics.FLIP_VERTICAL | DirectGraphics.ROTATE_90;
	private static final int HV180 = DirectGraphics.FLIP_HORIZONTAL | DirectGraphics.FLIP_VERTICAL | DirectGraphics.ROTATE_180;
	private static final int HV270 = DirectGraphics.FLIP_HORIZONTAL | DirectGraphics.FLIP_VERTICAL | DirectGraphics.ROTATE_270;
	private static final int H90 = DirectGraphics.FLIP_HORIZONTAL | DirectGraphics.ROTATE_90;
	private static final int H180 = DirectGraphics.FLIP_HORIZONTAL | DirectGraphics.ROTATE_180;
	private static final int H270 = DirectGraphics.FLIP_HORIZONTAL | DirectGraphics.ROTATE_270;
	private static final int V90 = DirectGraphics.FLIP_VERTICAL | DirectGraphics.ROTATE_90;
	private static final int V180 = DirectGraphics.FLIP_VERTICAL | DirectGraphics.ROTATE_180;
	private static final int V270 = DirectGraphics.FLIP_VERTICAL | DirectGraphics.ROTATE_270;


	public PlatformGraphics(PlatformImage image)
	{
		canvas = image.getCanvas();
		gc = canvas.createGraphics();

		platformGraphics = this;

		clip = new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight());
		gc.getClipBounds(clip);

		setColor(0,0,0);
		setStrokeStyle(SOLID);
		gc.setBackground(new Color(0, 0, 0, 0));
		gc.setFont(font.platformFont.awtFont);
	}

	public void reset() //Internal use method, resets the Graphics object to its inital values
	{
		translate(-translateX, -translateY);
		translateX = translateY = 0;
		setClip(0, 0, canvas.getWidth(), canvas.getHeight());
		setColor(0,0,0);
		setFont(Font.getDefaultFont());
		setStrokeStyle(SOLID);
	}

	public Graphics2D getGraphics2D() { return gc; }

	public BufferedImage getCanvas() { return canvas; }

	public void clearRect(int x, int y, int width, int height)
	{
		gc.clearRect(x, y, width, height);
	}

	public void copyArea(int subx, int suby, int subw, int subh, int x, int y, int anchor)
	{
		if (subw <= 0 || subh <= 0) { return; }

		x = AnchorX(x, subw, anchor);
		y = AnchorY(y, subh, anchor);

		BufferedImage sub = new BufferedImage(subw, subh, BufferedImage.TYPE_INT_ARGB);

		// Copy the pixels from the source area to the new image. getSubImage() makes both images contain the same data reference
		for (int i = 0; i < subw; i++) 
		{
			for (int j = 0; j < subh; j++) 
			{
				sub.setRGB(i, j, canvas.getRGB(subx + i, suby + j));
			}
		}

		gc.drawImage(sub, x, y, null);
	}

	public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle)
	{
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
			int imgWidth = image.getWidth();
			int imgHeight = image.getHeight();

			x = AnchorX(x, imgWidth, anchor);
			y = AnchorY(y, imgHeight, anchor);

			gc.drawImage(image.platformImage.getCanvas(), x, y, null);
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "drawImage A:"+e.getMessage());
		}
	}

	public void drawImage(Image image, int x, int y)
	{
		try
		{
			gc.drawImage(image.platformImage.getCanvas(), x, y, null);
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "drawImage B:"+e.getMessage());
		}
	}

	public void drawImage2(Image image, int x, int y) // Internal use method called by PlatformImage
	{
		gc.drawImage(image.platformImage.getCanvas(), x, y, null);
	}
	public void drawImage2(BufferedImage image, int x, int y) // Internal use method called by PlatformImage
	{
		gc.drawImage(image, x, y, null);
	}

	public void flushGraphics(Image image, int x, int y, int width, int height)
	{
		// called by MobilePlatform.flushGraphics/repaint
		try
		{
			BufferedImage sub = image.platformImage.getCanvas().getSubimage(x, y, width, height);

			// Apply the backlight mask if Display, nokia's DeviceControl, or others request it for backlight effects.
			if(Mobile.renderLCDMask)
			{
				final int[] pixels = ((DataBufferInt) sub.getRaster().getDataBuffer()).getData();
				for(int i = 0; i < pixels.length; i++) { pixels[i] = pixels[i] & Mobile.lcdMaskColors[Mobile.maskIndex]; }
			}

			// This one is rather costly, as it has to draw overlays on the corners of the screen with gaussian filtering applied.
			if(Mobile.funLightsEnabled)
			{
				// Create an overlay image for the fun lights
				BufferedImage overlayImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

				drawFunLights(overlayImage, width, height);

				gc.drawImage(sub, x, y, null);
				gc.drawImage(overlayImage, x, y, null);
			} 
			else { gc.drawImage(sub, x, y, null); }
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

		if (subx < 0 || suby < 0 || subx + subw > image.platformImage.getCanvas().getWidth() 
					|| suby + subh > image.platformImage.getCanvas().getHeight()) 
		{
			throw new IllegalArgumentException("Source region exceeds image bounds.");
		}

		try
		{
			if(transform == 0)
			{
				BufferedImage sub = image.platformImage.getCanvas().getSubimage(subx, suby, subw, subh);
				x = AnchorX(x, subw, anchor);
				y = AnchorY(y, subh, anchor);
				gc.drawImage(sub, x, y, null);
			}
			else
			{
				PlatformImage sub = new PlatformImage(image, subx, suby, subw, subh, transform);
				x = AnchorX(x, sub.width, anchor);
				y = AnchorY(y, sub.height, anchor);
				gc.drawImage(sub.getCanvas(), x, y, null);
			}
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "drawRegion A (x:"+x+" y:"+y+" w:"+subw+" h:"+subh+"):"+e.getMessage());
		}
	}

	public void drawRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height, boolean processAlpha) 
	{
		if (width <= 0 || height <= 0) { return; }
		if (rgbData == null) { throw new NullPointerException(); }
		if (offset < 0 || offset >= rgbData.length) { throw new ArrayIndexOutOfBoundsException(); }
		
		if (scanlength > 0) 
		{
			if (offset + scanlength * (height - 1) + width > rgbData.length) { throw new ArrayIndexOutOfBoundsException(); }
		}
		else 
		{
			if (offset + width > rgbData.length || offset + scanlength * (height - 1) < 0) { throw new ArrayIndexOutOfBoundsException(); }
		}

		BufferedImage temp = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		// Let's get the target pixel array directly from the image's DataBuffer, saving us an array copy and the overhead of setDataElements().
    	final int[] pixels = ((DataBufferInt) temp.getRaster().getDataBuffer()).getData();
		int s, d, pixel;

		for (int i = 0; i < height; i++) 
		{
			s = offset + i * scanlength;
			d = i * width;
			for (int j = 0; j < width; j++) 
			{
				pixel = rgbData[s++];
				if (!processAlpha) { pixel = (pixel & 0x00FFFFFF) | 0xFF000000; } // Set alpha to 255
				pixels[d + j] = pixel; // Store the pixel
			}
		}

		gc.drawImage(temp, x, y, null);
	}

	public void drawLine(int x1, int y1, int x2, int y2) { gc.drawLine(x1, y1, x2, y2); }

	public void drawRect(int x, int y, int width, int height)
	{
		if (width < 0 || height < 0) { return; }

		gc.drawRect(x, y, width, height);
	}

	public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight)
	{
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
		if (width <= 0 || height <= 0) { return; }

		gc.fillArc(x, y, width, height, startAngle, arcAngle);
	}

	public void fillRect(int x, int y, int width, int height)
	{
		if (width <= 0 || height <= 0) { return; }

		gc.fillRect(x, y, width, height);
	}

	public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight)
	{
		if (width < 0 || height < 0) { return; }

		gc.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
	}

	public void setColor(int rgb)
	{
		setColor((rgb>>16) & 0xFF, (rgb>>8) & 0xFF, rgb & 0xFF);
	}

	public void setColor(int r, int g, int b)
	{
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
		super.setFont(font);
		gc.setFont(font.platformFont.awtFont);
	}

	public void setClip(int x, int y, int width, int height)
	{
		clip.setBounds(x, y, width, height);
		gc.setClip(clip);
	}

	public void clipRect(int x, int y, int width, int height)
	{
		Rectangle newClip = new Rectangle(x, y, width, height);
        clip = clip.intersection(newClip);
		gc.setClip(clip);
	}

	public int getTranslateX() { return translateX; }
	
	public int getTranslateY() { return translateY; }

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
		return xout;
	}

	private int AnchorY(int y, int height, int anchor)
	{
		int yout = y;
		if((anchor & BOTTOM)>0) { yout = y-height; }
		if((anchor & VCENTER)>0) { yout = y-(height/2); }
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

	public int getNativePixelFormat() { return DirectGraphics.TYPE_INT_8888_ARGB; }

	public int getAlphaComponent() { return colorAlpha; }

	public void setARGBColor(int argbColor)
	{
		colorAlpha = (argbColor>>>24) & 0xFF;
		setColor(argbColor);
	}

	public void drawImage(javax.microedition.lcdui.Image img, int x, int y, int anchor, int manipulation)
	{
		BufferedImage image = manipulateImage(img.platformImage.getCanvas(), manipulation);
		x = AnchorX(x, image.getWidth(), anchor);
		y = AnchorY(y, image.getHeight(), anchor);
		drawImage2(image, x, y);
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
		final int[] data = ((DataBufferInt) temp.getRaster().getDataBuffer()).getData();

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

		gc.drawImage(manipulateImage(temp, manipulation), x, y, null);
	}

	public void drawPixels(int[] pixels, boolean transparency, int offset, int scanlength, int x, int y, int width, int height, int manipulation, int format) 
	{
		if (width == 0 || height == 0) { return; }
		if (width < 0 || height < 0) { throw new IllegalArgumentException("drawPixels(int) received negative width or height"); }
		if (pixels == null) { throw new NullPointerException("drawPixels(int) received a null pixel array"); }
		if (offset < 0 || offset >= pixels.length) { throw new ArrayIndexOutOfBoundsException("drawPixels(int) index out of bounds:" + width + " * " + height + "| len:" + pixels.length); }

		if (scanlength > 0) 
		{
			if (offset + scanlength * (height - 1) + width > pixels.length) { throw new ArrayIndexOutOfBoundsException(); }
		} 
		else 
		{
			if (offset + width > pixels.length || offset + scanlength * (height - 1) < 0) { throw new ArrayIndexOutOfBoundsException(); }
		}

		// Create the temporary BufferedImage and get its DataBuffer to manipulate it directly.
		BufferedImage temp = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		int[] data = ((DataBufferInt) temp.getRaster().getDataBuffer()).getData();

		for (int row = 0; row < height; row++) 
		{
			int srcIndex = offset + row * scanlength;
			for (int col = 0; col < width; col++) 
			{
				int pixel = pixels[srcIndex + col];
				if (!transparency) { pixel = (pixel & 0x00FFFFFF) | 0xFF000000; } // Set alpha to 255
				data[row * width + col] = pixel;
			}
		}

		gc.drawImage(manipulateImage(temp, manipulation), x, y, null);
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
			for (int col = 0; col < width; col++) 
			{
				int index = offset + row * scanlength + col;
				data[row * width + col] = pixelToColor(pixels[index], format);
				if (!transparency) { data[row * width + col] &= 0x00FFFFFF; } // Clear the alpha channel
			}
		}
	
		gc.drawImage(manipulateImage(temp, manipulation), x, y, null);
	}

	public void drawPolygon(int[] xPoints, int xOffset, int[] yPoints, int yOffset, int nPoints, int argbColor)
	{
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
	
		// Temporary canvas data array to read raw pixel data from.
		int[] canvasData = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();

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
		canvas.getRGB(x, y, width, height, pixels, offset, scanlength);
	}

	public void getPixels(short[] pixels, int offset, int scanlength, int x, int y, int width, int height, int format)
	{
		int i = offset;
		for(int row=0; row<height; row++)
		{
			for (int col=0; col<width; col++)
			{
				pixels[i] = colorToShortPixel(canvas.getRGB(col+x, row+y), format);
				i++;
			}
		}
	}

	private int pixelToColor(short c, int format)
	{
		int a = 0xFF;
		int r = 0;
		int g = 0;
		int b = 0;
		switch(format)
		{
			case DirectGraphics.TYPE_USHORT_1555_ARGB:
				a = ((c>>15) & 0x01)*0xFF;
				r = (c>>10) & 0x1F; g = (c>>5) & 0x1F; b = c & 0x1F;
				r = (r<<3)|(r>>2); g = (g<<3)|(g>>2); b = (b<<3)|(b>>2);
				break;
			case DirectGraphics.TYPE_USHORT_444_RGB:
				r = (c>>8) & 0xF; g = (c>>4) & 0xF; b = c & 0xF;
				r = (r<<4)|r; g = (g<<4)|g; b = (b<<4)|b;
				break;
			case DirectGraphics.TYPE_USHORT_4444_ARGB:
				a = (c>>12) & 0xF; r = (c>>8) & 0xF; g = (c>>4) & 0xF; b = c & 0xF;
				a = (a<<4)|a; r = (r<<4)|r; g = (g<<4)|g; b = (b<<4)|b;
				break;
			case DirectGraphics.TYPE_USHORT_555_RGB:
				r = (c>>10) & 0x1F; g = (c>>5) & 0x1F; b = c & 0x1F;
				r = (r<<3)|(r>>2); g = (g<<3)|(g>>2); b = (b<<3)|(b>>2);
				break;
			case DirectGraphics.TYPE_USHORT_565_RGB:
				r = (c>>11) & 0x1F; g = (c>>5) & 0x3F; b = c & 0x1F;
				r = (r<<3)|(r>>2); g = (g<<2)|(g>>4); b = (b<<3)|(b>>2);
				break;
		}
		return (a<<24) | (r<<16) | (g<<8) | b;
	}

	private short colorToShortPixel(int c, int format)
	{
		int a = 0;
		int r = 0;
		int g = 0;
		int b = 0;
		int out = 0;
		switch(format)
		{
			case DirectGraphics.TYPE_USHORT_1555_ARGB:
				a=c>>>31; r=((c>>19)&0x1F); g=((c>>11)&0x1F); b=((c>>3)&0x1F);
				out=(a<<15)|(r<<10)|(g<<5)|b;
				break;
			case DirectGraphics.TYPE_USHORT_444_RGB:
				r=((c>>20)&0xF); g=((c>>12)&0xF); b=((c>>4)&0xF);
				out=(r<<8)|(g<<4)|b;
				break;
			case DirectGraphics.TYPE_USHORT_4444_ARGB:
				a=((c>>>28)&0xF); r=((c>>20)&0xF); g=((c>>12)&0xF); b=((c>>4)&0xF);
				out=(a<<12)|(r<<8)|(g<<4)|b;
				break;
			case DirectGraphics.TYPE_USHORT_555_RGB:
				r=((c>>19)&0x1F); g=((c>>11)&0x1F); b=((c>>3)&0x1F);
				out=(r<<10)|(g<<5)|b;
				break;
			case DirectGraphics.TYPE_USHORT_565_RGB:
				r=((c>>19)&0x1F); g=((c>>10)&0x3F); b=((c>>3)&0x1F);
				out=(r<<11)|(g<<5)|b;
				break;
		}
		return (short)out;
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
	public void drawFunLights(BufferedImage overlayImage, int width, int height) 
	{
		int[] pixelData = ((DataBufferInt)overlayImage.getRaster().getDataBuffer()).getData();
		
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
		applyGaussianBlur(overlayImage, width, height);
	}
	
	private void applyGaussianBlur(BufferedImage image, int width, int height) 
	{
		final int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
		final int[] result = new int[pixels.length];
	
		int kernelSize = 7;
		int kernelRadius = kernelSize / 2;
	
		for (int y = 0; y < height; y++) 
		{
			for (int x = 0; x < width; x++) 
			{
				float r = 0, g = 0, b = 0, a = 0;
				float weightSum = 0;
	
				for (int kx = -kernelRadius; kx <= kernelRadius; kx++) 
				{
					int pixelX = x + kx;
	
					if (pixelX >= 0 && pixelX < width) 
					{
						int pixelColor = pixels[y * width + pixelX];
						int alpha = (pixelColor >> 24) & 0xff;
						int red = (pixelColor >> 16) & 0xff;
						int green = (pixelColor >> 8) & 0xff;
						int blue = pixelColor & 0xff;
	
						int premultipliedRed = (red * alpha) / 255;
						int premultipliedGreen = (green * alpha) / 255;
						int premultipliedBlue = (blue * alpha) / 255;
	
						float kernelWeight = gaussianKernel[kx + kernelRadius];
						r += premultipliedRed * kernelWeight;
						g += premultipliedGreen * kernelWeight;
						b += premultipliedBlue * kernelWeight;
						a += alpha * kernelWeight;
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
	
		for (int x = 0; x < width; x++) 
		{
			for (int y = 0; y < height; y++) 
			{
				float r = 0, g = 0, b = 0, a = 0;
				float weightSum = 0;
	
				for (int ky = -kernelRadius; ky <= kernelRadius; ky++) 
				{
					int pixelY = y + ky;
	
					if (pixelY >= 0 && pixelY < height) 
					{
						int pixelColor = result[pixelY * width + x];
						int alpha = (pixelColor >> 24) & 0xff;
						int red = (pixelColor >> 16) & 0xff;
						int green = (pixelColor >> 8) & 0xff;
						int blue = pixelColor & 0xff;
	
						float kernelWeight = gaussianKernel[ky + kernelRadius];
						r += red * kernelWeight;
						g += green * kernelWeight;
						b += blue * kernelWeight;
						a += alpha * kernelWeight;
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
}
