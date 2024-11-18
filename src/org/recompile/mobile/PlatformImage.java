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

import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.game.Sprite;
import javax.microedition.lcdui.game.GameCanvas;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.geom.AffineTransform;

public class PlatformImage extends javax.microedition.lcdui.Image
{
	private WritableRaster raster;
	protected BufferedImage canvas;
	protected PlatformGraphics gc;

	// transformImage variable that is better off not being allocated every call. Is an array because of raster getDataElements()
	private static final int[] transformPixelData = new int[1];

	public BufferedImage getCanvas()
	{
		return canvas;
	}

	public PlatformGraphics getGraphics()
	{
		return gc;
	}

	protected void createGraphics()
	{
		gc = new PlatformGraphics(this);
		gc.setColor(0x000000);
	}

	public PlatformImage(int Width, int Height)
	{
		// Create blank Image
		width = Width;
		height = Height;

		if(Mobile.noAlphaOnBlankImages) { canvas = new BufferedImage(Width, Height, BufferedImage.TYPE_INT_RGB); }
		else { canvas = new BufferedImage(Width, Height, BufferedImage.TYPE_INT_ARGB); }
		
		createGraphics();

		gc.setColor(0xFFFFFF);
		gc.fillRect(0, 0, width, height);
		gc.setColor(0x000000);

		platformImage = this;
	}

	public PlatformImage(String name)
	{
		// Create Image from resource name
		Mobile.log(Mobile.LOG_DEBUG, PlatformImage.class.getPackage().getName() + "." + PlatformImage.class.getSimpleName() + ": " + "Image From Resource Name");
		BufferedImage temp;

		InputStream stream = Mobile.getPlatform().loader.getMIDletResourceAsStream(name);

		if(stream==null) { throw new NullPointerException("Can't load image from resource, as the returned image is null."); }
		else
		{
			try { temp = ImageIO.read(stream); } 
			catch (IOException e) { throw new IllegalArgumentException("Failed to read image from resource:" + e.getMessage()); }
			
			if(temp == null) { throw new NullPointerException("Couldn't load image from resource: Image is null"); }
			
			width = (int)temp.getWidth();
			height = (int)temp.getHeight();

			canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			createGraphics();

			gc.drawImage2(temp, 0, 0);
		}
		platformImage = this;
	}

	public PlatformImage(InputStream stream)
	{
		// Create Image from InputStream
		Mobile.log(Mobile.LOG_DEBUG, PlatformImage.class.getPackage().getName() + "." + PlatformImage.class.getSimpleName() + ": " + "Image From Stream");
		BufferedImage temp;
		try { temp = ImageIO.read(stream); } 
		catch (IOException e) { throw new IllegalArgumentException("Failed to read image from InputStream:" + e.getMessage()); }
		
		if(temp == null) { throw new NullPointerException("Couldn't load image from InputStream: Image is null"); }

		width = (int)temp.getWidth();
		height = (int)temp.getHeight();

		canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		createGraphics();

		gc.drawImage2(temp, 0, 0);

		platformImage = this;
	}

	public PlatformImage(Image source)
	{
		// Create Image from Image
		if(source == null) { throw new NullPointerException("Couldn't load image: Image is null"); }

		width = source.platformImage.width;
		height = source.platformImage.height;

		canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		createGraphics();

		gc.drawImage2(source.platformImage.getCanvas(), 0, 0);

		platformImage = this;
	}

	public PlatformImage(byte[] imageData, int imageOffset, int imageLength)
	{
		// Create Image from Byte Array Range (Data is PNG, JPG, etc.)
		InputStream stream = new ByteArrayInputStream(imageData, imageOffset, imageLength);

		BufferedImage temp;
		
		try { temp = ImageIO.read(stream); } 
		catch (IOException e) { throw new IllegalArgumentException("Failed to read image from Byte Array." + e.getMessage()); }
		
		if(temp == null) { throw new NullPointerException("Couldn't load image from byte array: Image is null"); }
		
		width = temp.getWidth();
		height = temp.getHeight();

		canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		createGraphics();

		gc.drawImage2(temp, 0, 0);

		platformImage = this;
	}

	public PlatformImage(int[] rgb, int Width, int Height, boolean processAlpha)
	{
		// createRGBImage (Data is ARGB pixel data)
		width = Width;
		height = Height;

		if(width < 1) { width = 1; }
		if(height < 1) { height = 1; }

		canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		createGraphics();

		gc.drawRGB(rgb, 0, width, 0, 0, width, height, true);

		platformImage = this;
	}

	public PlatformImage(Image image, int x, int y, int Width, int Height, int transform)
	{
		// Create Image From Sub-Image, Transformed //
		BufferedImage sub = image.platformImage.canvas.getSubimage(x, y, Width, Height);

		canvas = transformImage(sub, transform);
		createGraphics();

		width = (int)canvas.getWidth();
		height = (int)canvas.getHeight();

		platformImage = this;
	}

	public void getRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height) 
	{	
		// Temporary array to hold the raw pixel data
		int[] tempData = new int[width * height];
		
		raster = canvas.getRaster();
		raster.getDataElements(x, y, width, height, tempData);
	
		// Copy the data into rgbData, taking scanlength into account
		for (int row = 0; row < height; row++) 
		{
			int sourceIndex = row * width;
			int destIndex = offset + row * scanlength;
	
			System.arraycopy(tempData, sourceIndex, rgbData, destIndex, width);
		}
	}

	public int getARGB(int x, int y) { return canvas.getRGB(x, y); }

	public int getPixel(int x, int y)
	{
		int[] rgbData = { 0 };
		canvas.getRGB(x, y, 1, 1, rgbData, 0, 1);
		return rgbData[0];
	}

	public void setPixel(int x, int y, int color)
	{
		canvas.setRGB(x, y, color);
	}

	public static final BufferedImage transformImage(final BufferedImage image, final int transform)
	{
		// Return early if no transform is specified.
		if(transform == Sprite.TRANS_NONE) { return image; }

		final int width = (int)image.getWidth();
		final int height = (int)image.getHeight();

		BufferedImage transimage = null;
		if(transform == Sprite.TRANS_ROT90 || transform == Sprite.TRANS_ROT270 || transform == Sprite.TRANS_MIRROR_ROT90 || transform == Sprite.TRANS_MIRROR_ROT270) 
		{
			transimage = new BufferedImage(height, width, BufferedImage.TYPE_INT_ARGB); // Non-Math.PI rotations require width and height to be swapped
		}
		else { transimage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB); }

		final WritableRaster sourceRaster = image.getRaster();
        final WritableRaster targetRaster = transimage.getRaster();
		
		switch (transform) 
		{
			case Sprite.TRANS_ROT90:
				for (int y = 0; y < height; y++) 
				{
					for (int x = 0; x < width; x++) 
					{		
						sourceRaster.getDataElements(x, y, transformPixelData); // Get pixel from original image
						targetRaster.setDataElements(height - 1 - y, x, transformPixelData); // Set pixel in rotated image
					}
				}
				//dumpImage(image, "");
				//dumpImage(transimage, "_rot90");
				return transimage;

			case Sprite.TRANS_ROT180:
				for (int y = 0; y < height; y++) 
				{
					for (int x = 0; x < width; x++) 
					{
						sourceRaster.getDataElements(x, y, transformPixelData);
						targetRaster.setDataElements(width - 1 - x, height - 1 - y, transformPixelData);
					}
				}
				//dumpImage(image, "");
				//dumpImage(transimage, "_rot180");
				return transimage;
			
			case Sprite.TRANS_ROT270:
				for (int y = 0; y < height; y++) 
				{
					for (int x = 0; x < width; x++) 
					{
						sourceRaster.getDataElements(x, y, transformPixelData); // Get pixel from original image
						targetRaster.setDataElements(y, width - 1 - x, transformPixelData); // Set pixel in rotated image
					}
				}
				//dumpImage(image, "");
				//dumpImage(transimage, "_rot270");
				return transimage;

			case Sprite.TRANS_MIRROR: 
				/*
				 * Even though sorting an entire column would be faster from a pure algorithmic perspective (like processing
				 * a whole row at once is on TRANS_MIRROR_ROT180), image data tends to be stored so that each row is contiguous 
				 * in memory, which makes its access oftentimes MUCH faster than for columns, which could negate the performance 
				 * benefits of column access entirely and then some.
				 * 
				 * This transform is such a case. Making get/setDataElements operate on columns, and eliminating that
				 * inner row loop actually results in far worse performance since columns would be accessed way more often.
				 */
				for (int y = 0; y < height; y++) 
				{	
					for (int x = 0; x < width; x++) 
					{
						sourceRaster.getDataElements(x, y, transformPixelData); // Get the pixel data to be mirrored
						targetRaster.setDataElements(width - 1 - x, y, transformPixelData); // Set each mirrored pixel on the row
					}
				}
				//dumpImage(image, "");
				//dumpImage(transimage, "_mirror");
				return transimage;
				

			case Sprite.TRANS_MIRROR_ROT90:
				for (int y = 0; y < height; y++) 
				{
					for (int x = 0; x < width; x++) 
					{
						sourceRaster.getDataElements(x, y, transformPixelData); // Get the pixel from the original image
						targetRaster.setDataElements(height - 1 - y, width - 1 - x, transformPixelData); // Set the pixel in the target raster
					}
				}		
				
				//dumpImage(image, "");
				//dumpImage(transimage, "_mirror90");
				return transimage;

			case Sprite.TRANS_MIRROR_ROT180: // Basically mirror vertically (an arrow pointing up will then point down)
				final int[] rowData = new int[width * 4]; // Batch array for the entire row processing
				for (int y = 0; y < height; y++) 
				{	
					sourceRaster.getDataElements(0, y, width, 1, rowData); // Get the entire row
					targetRaster.setDataElements(0, height - 1 - y, width, 1, rowData); // Set the row in the target raster at the flipped position
				}
				//dumpImage(image, "");
				//dumpImage(transimage, "_mirror180");
				return transimage;
				

			case Sprite.TRANS_MIRROR_ROT270:
				for (int y = 0; y < height; y++) 
				{
					for (int x = 0; x < width; x++) 
					{
						sourceRaster.getDataElements(x, y, transformPixelData); // Get the pixel from the original image
						int mirroredX = width - 1 - x; // Mirrored x position, so the "y" argument below doesn't become too convoluted
						targetRaster.setDataElements(y, width - 1 - mirroredX, transformPixelData); // Set the pixel in the target raster
					}
				}
				//dumpImage(image, "");
				//dumpImage(transimage, "_mirror270");
				return transimage;
		}

		return image;
	}

	// TODO: Turn this into a setting. Being able to dump image data would be nice.
	public static void dumpImage(BufferedImage image, String append) 
	{
        try 
		{
			String imageMD5 = generateMD5Hash(image);
			String dumpPath = "." + File.separatorChar + "FreeJ2MEDumps" + File.separatorChar + "Image" + File.separatorChar + Mobile.getPlatform().loader.suitename + File.separatorChar;
			File dumpFile = new File(dumpPath);
			
			if (!dumpFile.isDirectory()) { dumpFile.mkdirs(); }
			
			dumpPath = dumpPath + "Image_" + imageMD5 + append + ".png";
			
			dumpFile = new File(dumpPath);
			if(dumpFile.exists()) { return; } // Don't overwrite an image that already exists
            ImageIO.write(image, "png", dumpFile);
            System.out.println("Image saved successfully: " + dumpPath);
        } catch (IOException e) { System.err.println("Error saving image: " + e.getMessage()); }
    }

	private static String generateMD5Hash(BufferedImage image) 
	{
        try {
            // Convert BufferedImage to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos); // Change format as needed
            byte[] imageBytes = baos.toByteArray();

            // Create MD5 hash
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(imageBytes);

            // Convert byte array to hex string
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString(); // Return the MD5 hash as a hex string
        } catch (Exception e) {
            e.printStackTrace(); // Handle exceptions as needed
            return null;
        }
    }
}
