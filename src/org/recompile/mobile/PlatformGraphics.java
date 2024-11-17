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
import java.awt.image.WritableRaster;
import java.awt.geom.AffineTransform;
import java.awt.BasicStroke;

public class PlatformGraphics extends javax.microedition.lcdui.Graphics implements DirectGraphics
{
	protected BufferedImage canvas;
	private WritableRaster raster;
	protected Graphics2D gc;

	protected Color awtColor;

	protected int strokeStyle = SOLID;

	protected Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);

	public PlatformGraphics platformGraphics;
	public PlatformImage platformImage;

	public PlatformGraphics(PlatformImage image)
	{
		canvas = image.getCanvas();
		gc = canvas.createGraphics();
		platformImage = image;

		platformGraphics = this;

		clipX = 0;
		clipY = 0;
		clipWidth = canvas.getWidth();
		clipHeight = canvas.getHeight();

		setColor(0,0,0);
		setStrokeStyle(SOLID);
		gc.setBackground(new Color(0, 0, 0, 0));
		gc.setFont(font.platformFont.awtFont);
	}

	public void reset() //Internal use method, resets the Graphics object to its inital values
	{
		translate(-1 * translateX, -1 * translateY);
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

		BufferedImage sub = canvas.getSubimage(subx, suby, subw, subh);

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
			gc.drawImage(sub, x, y, null);
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "flushGraphics A:"+e.getMessage());
		}
	}

	public void drawRegion(Image image, int subx, int suby, int subw, int subh, int transform, int x, int y, int anchor)
	{
		if (subw <= 0 || subh <= 0) { return; }

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
		raster = temp.getRaster();
		final int[] pixels = new int[width * height];
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

		raster.setDataElements(0, 0, width, height, pixels);

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
			final FontMetrics metrics = gc.getFontMetrics();
			final int strWidth = metrics.stringWidth(str);
			final int strHeight = metrics.getHeight();
			final int ascent = metrics.getAscent();

			x = AnchorX(x, strWidth, anchor);
			y = y + ascent - 1;
			y = AnchorY(y, strHeight, anchor);

			try { gc.drawString(str, x, y); } 
			catch (Exception e) { }
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
		gc.setClip(x, y, width, height);
		Rectangle rect=new Rectangle();
		gc.getClipBounds(rect);
		clipX = (int) rect.getX();
		clipY = (int) rect.getY();
		clipWidth = (int)rect.getWidth();
		clipHeight = (int)rect.getHeight();
	}

	public void clipRect(int x, int y, int width, int height)
	{
		gc.clipRect(x, y, width, height);
		Rectangle rect=new Rectangle();
		gc.getClipBounds(rect);
		clipX = (int) rect.getX();
		clipY = (int) rect.getY();
		clipWidth = (int)rect.getWidth();
		clipHeight = (int)rect.getHeight();
	}

	public int getTranslateX() { return translateY; }
	
	public int getTranslateY() { return translateY; }

	public void translate(int x, int y)
	{
		translateX += x;
		translateY += y;
		gc.translate(x, y);
		Rectangle rect=new Rectangle();
		gc.getClipBounds(rect);
		clipX = (int) rect.getX();
        clipY = (int) rect.getY();
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
		int[] Type1 = {0xFFFFFFFF, 0xFF000000, 0x00FFFFFF, 0x00000000};
		int c = 0;
		int[] data = null;
		BufferedImage temp = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		raster = temp.getRaster();
		switch(format)
		{
			case -1: // TYPE_BYTE_1_GRAY_VERTICAL // used by Monkiki's Castles
				data = new int[width*height];
				int ods = offset / scanlength;
				int oms = offset % scanlength;
				int b = ods % 8; //Bit offset in a byte
				for (int yj = 0; yj < height; yj++)
				{
					int ypos = yj * width;
					int tmp = (ods + yj) / 8 * scanlength+oms;
					for (int xj = 0; xj < width; xj++)
					{
						c = ((pixels[tmp + xj]>>b)&1);
						if(transparencyMask!=null) { c |= (((transparencyMask[tmp + xj]>>b)&1)^1)<<1; }
						data[(yj*width)+xj] = Type1[c];
					}
					b++;
					if(b>7) b=0;
				}
			break;

			case 1: // TYPE_BYTE_1_GRAY // used by Munkiki's Castles
				data = new int[pixels.length*8];

				for(int i=(offset/8); i<pixels.length; i++)
				{
					for(int j=7; j>=0; j--)
					{
						c = ((pixels[i]>>j)&1);
						if(transparencyMask!=null) { c |= (((transparencyMask[i]>>j)&1)^1)<<1; }
						data[(i*8)+(7-j)] = Type1[c];
					}
				}
			break;

			default: Mobile.log(Mobile.LOG_WARNING, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "drawPixels A : Format " + format + " Not Implemented");
		}

		raster.setDataElements(0, 0, width, height, data);
		gc.drawImage(manipulateImage(temp, manipulation), x, y, null);
	}

	public void drawPixels(int[] pixels, boolean transparency, int offset, int scanlength, int x, int y, int width, int height, int manipulation, int format) 
	{
		if (width <= 0 || height <= 0) { return; }
		if (pixels == null) { throw new NullPointerException(); }
		if (offset < 0 || offset >= pixels.length) { throw new ArrayIndexOutOfBoundsException(); }

		if (scanlength > 0) 
		{
			if (offset + scanlength * (height - 1) + width > pixels.length) { throw new ArrayIndexOutOfBoundsException(); }
		} 
		else 
		{
			if (offset + width > pixels.length || offset + scanlength * (height - 1) < 0) { throw new ArrayIndexOutOfBoundsException(); }
		}

		// Create the temporary BufferedImage and get its WritableRaster
		BufferedImage temp = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		raster = temp.getRaster();

		// Prepare pixel data
		final int[] data = new int[width * height];

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

		raster.setDataElements(0, 0, width, height, data);
		gc.drawImage(manipulateImage(temp, manipulation), x, y, null);
	}

	public void drawPixels(short[] pixels, boolean transparency, int offset, int scanlength, int x, int y, int width, int height, int manipulation, int format)
	{
		BufferedImage temp = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		raster = temp.getRaster();

		final int[] data = new int[pixels.length];
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
	
		// Set the pixel data directly into the raster
		raster.setDataElements(0, 0, width, height, data);
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
		Mobile.log(Mobile.LOG_WARNING, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "getPixels A");
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

	private BufferedImage manipulateImage(BufferedImage image, int manipulation)
	{
        /* 
		 * Both DirectGraphics and Sprite's rotations are counter-clockwise, flipping
		 * an image horizontally is done by multiplying its height or width scale
		 * by -1 respectively. Flipping vertically is the same as flipping horizontally, 
		 * and then rotating by 180 degrees.
		*/
		final int HV = DirectGraphics.FLIP_HORIZONTAL | DirectGraphics.FLIP_VERTICAL;
        final int HV90 = DirectGraphics.FLIP_HORIZONTAL | DirectGraphics.FLIP_VERTICAL | DirectGraphics.ROTATE_90;
        final int HV180 = DirectGraphics.FLIP_HORIZONTAL | DirectGraphics.FLIP_VERTICAL | DirectGraphics.ROTATE_180;
        final int HV270 = DirectGraphics.FLIP_HORIZONTAL | DirectGraphics.FLIP_VERTICAL | DirectGraphics.ROTATE_270;
        final int H90 = DirectGraphics.FLIP_HORIZONTAL | DirectGraphics.ROTATE_90;
        final int H180 = DirectGraphics.FLIP_HORIZONTAL | DirectGraphics.ROTATE_180;
        final int H270 = DirectGraphics.FLIP_HORIZONTAL | DirectGraphics.ROTATE_270;
        final int V90 = DirectGraphics.FLIP_VERTICAL | DirectGraphics.ROTATE_90;
        final int V180 = DirectGraphics.FLIP_VERTICAL | DirectGraphics.ROTATE_180;
        final int V270 = DirectGraphics.FLIP_VERTICAL | DirectGraphics.ROTATE_270;
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
                return PlatformImage.transformImage(image, Sprite.TRANS_MIRROR_ROT270);
            case V90:
            case H270:
                return PlatformImage.transformImage(image, Sprite.TRANS_MIRROR_ROT90);
            case 0: /* No Manipulation */
            case HV180:
                break;
            default:
				Mobile.log(Mobile.LOG_WARNING, PlatformGraphics.class.getPackage().getName() + "." + PlatformGraphics.class.getSimpleName() + ": " + "manipulateImage "+manipulation+" not defined");
		}
		return image;
		
	}
}
