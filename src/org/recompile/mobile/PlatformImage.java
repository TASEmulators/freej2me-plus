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

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import javax.microedition.io.Connector;
import com.nttdocomo.util.ScratchPadConnection;

public class PlatformImage
{
	protected BufferedImage canvas;

	protected int[] dataBuffer;

	private boolean isMutable = false;

	private boolean is2bpp = false; // SIEMENS: False = 1bpp, True = 2bpp

	public BufferedImage getCanvas() { return canvas; }

	public int[] getDataBuffer() { return dataBuffer; }

	public void setCanvas(BufferedImage newCanvas) { canvas = newCanvas; }

	/* 
	 * Previously these reused the same graphics object, but this lead to issues due
	 * to the object potentially not being in its init state.
	 *  
	 * Returning a new Graphics object for each call might use a bit more memory for
	 * jars that request many of them without clearing, but should be very marginal.
	 */
	public Graphics getMIDPGraphics() 
	{ 
		if(!isMutable()) { throw new IllegalStateException("Image is immutable, cannot access Graphics object"); }
		return new Graphics(this);
	}

	/* 
	 * This actually returns an nttdocomo.opt.ui.Graphics2 instance internally, it's an extension of nttdocomo.ui.Graphics
	 * containing all of its funcionality, and some I-Appli such as DoDonPachi try to cast this object into a Graphics2
	 * forcefully, which WILL fail if the object returned here isn't actually a Graphics2.
	 */
	public com.nttdocomo.ui.Graphics getDoJaGraphics() 
	{ 
		if(!isMutable()) { throw new IllegalStateException("Image is immutable, cannot access Graphics object"); }
		return new com.nttdocomo.opt.ui.Graphics2(this); 
	}

	public PlatformImage() { }

	public PlatformImage(int Width, int Height)
	{
		// Create blank Image
		if(Mobile.noAlphaOnBlankImages) { canvas = new BufferedImage(Width, Height, BufferedImage.TYPE_INT_RGB); }
		else { canvas = new BufferedImage(Width, Height, BufferedImage.TYPE_INT_ARGB); }
		dataBuffer = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();
		
		Arrays.fill(dataBuffer, 0xFFFFFFFF);

		isMutable = true;
	}

	public PlatformImage(int Width, int Height, int ARGBcolor)
	{
		// Create Image with specific BG color
		canvas = new BufferedImage(Width, Height, BufferedImage.TYPE_INT_ARGB);
		dataBuffer = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();
		
		Arrays.fill(dataBuffer, ARGBcolor);

		isMutable = true;
	}

	public PlatformImage(String name) throws IOException
	{
		// Create Image from resource name
		
		BufferedImage image;

		InputStream stream = null;
		if(!Mobile.isDoJa) 
		{ 
			Mobile.log(Mobile.LOG_DEBUG, PlatformImage.class.getPackage().getName() + "." + PlatformImage.class.getSimpleName() + ": " + "Image From Resource Name");
			stream = Mobile.getPlatform().loader.getMIDletResourceAsStream(name); 
		}
		else // DoJa often tries to load images from scratchpad when calling its image creation methods
		{
			if (name.startsWith("scratchpad:")) 
			{
				
				try 
				{
					Mobile.log(Mobile.LOG_DEBUG, PlatformImage.class.getPackage().getName() + "." + PlatformImage.class.getSimpleName() + ": " + "DoJa Image From Scratchpad");
					ScratchPadConnection spConn = (ScratchPadConnection) Connector.open(name);
					stream = ((ScratchPadConnection)spConn).openInputStream();
				}
				catch(Exception e) { Mobile.log(Mobile.LOG_DEBUG, PlatformImage.class.getPackage().getName() + "." + PlatformImage.class.getSimpleName() + ": " + "Failed to load DoJa Image From Scratchpad:" + e.getMessage()); }
			}
			else { stream = Mobile.getPlatform().loader.getMIDletResourceAsStream(name); }
		}

		if(stream == null) { throw new IOException("Can't load image from resource, as the returned image is null."); }
		else
		{
			final byte[] rawData;
			try { rawData = PNGUtility.readFully(stream); }
			catch (IOException e) { throw new IOException("Failed to read image from resource:" + e.getMessage()); }

			try { image = ImageIO.read(new ByteArrayInputStream(rawData)); } 
			catch (IOException e) { throw new IOException("Failed to read image from resource:" + e.getMessage()); }
			
			if(image == null) { throw new IOException("Can't load image from resource, as the returned image is null."); }

			// Older JREs ignore a PNG's tRNS chunk on gray/rgb images, so apply the color key ourselves if needed.
			image = PNGUtility.applyTransparentColorKey(image, rawData);

			if(image.getType() == BufferedImage.TYPE_INT_ARGB || image.getType() == BufferedImage.TYPE_INT_RGB) { canvas = image; }
			else 
			{
				canvas = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
				canvas.getGraphics().drawImage(image, 0, 0, null);
			}

			dataBuffer = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();
		}
	}

	public PlatformImage(InputStream stream) throws IOException
	{
		// Create Image from InputStream
		Mobile.log(Mobile.LOG_DEBUG, PlatformImage.class.getPackage().getName() + "." + PlatformImage.class.getSimpleName() + ": " + "Image From Stream");
		BufferedImage image;

		final byte[] rawData;
		try { rawData = PNGUtility.readFully(stream); }
		catch (IOException e) { throw new IOException("Failed to read image from InputStream:" + e.getMessage()); }

		try { image = ImageIO.read(new ByteArrayInputStream(rawData)); } 
		catch (IOException e) { throw new IOException("Failed to read image from InputStream:" + e.getMessage()); }
		
		if(image == null) { throw new IOException("Can't load image from stream."); }

		// Older JREs ignore a PNG's tRNS chunk on gray/rgb images, so apply the color key ourselves if needed.
		image = PNGUtility.applyTransparentColorKey(image, rawData);

		if(image.getType() == BufferedImage.TYPE_INT_ARGB || image.getType() == BufferedImage.TYPE_INT_RGB) { canvas = image; }
		else 
		{
			canvas = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
			canvas.getGraphics().drawImage(image, 0, 0, null);
		}

		dataBuffer = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();
	}

	public PlatformImage(Image source)
	{
		// Create a copy from an LCDUI Image
		if(source == null) { throw new NullPointerException("Can't load image, it is null."); }

		// It's safe to assume that the source image will have the same type as the destination, so instead of drawImage we can just arraycopy the source to the destination
		canvas = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		dataBuffer = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();
		final int[] tempData = ((DataBufferInt) source.getCanvas().getRaster().getDataBuffer()).getData();
		
		System.arraycopy(tempData, 0, dataBuffer, 0, tempData.length);
	}

	public PlatformImage(byte[] imageData, int imageOffset, int imageLength, boolean mutable) // DoJa also uses this one, creates mutable images like DirectGraphics
	{
		// Create Image from Byte Array Range (Data is PNG, JPG, etc.)
		InputStream stream = new ByteArrayInputStream(imageData, imageOffset, imageLength);

		BufferedImage image;
		
		try { image = ImageIO.read(stream); } 
		catch (IOException e) { throw new IllegalArgumentException("Failed to read image from Byte Array." + e.getMessage()); }
		
		if(image == null) { throw new IllegalArgumentException("Can't load image from byte array, as the returned image is null."); }

		// Older JREs ignore a PNG's tRNS chunk on gray/rgb images, so apply the color key ourselves if needed.
		image = PNGUtility.applyTransparentColorKey(image, imageData, imageOffset, imageLength);

		if(image.getType() == BufferedImage.TYPE_INT_ARGB || image.getType() == BufferedImage.TYPE_INT_RGB) { canvas = image; }
		else 
		{
			canvas = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
			canvas.getGraphics().drawImage(image, 0, 0, null);
		}

		dataBuffer = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();
		isMutable = mutable;
	}

	public PlatformImage(int[] rgb, int Width, int Height, boolean processAlpha)
	{
		// createRGBImage (Data is ARGB pixel data)
		canvas = new BufferedImage(Width, Height, BufferedImage.TYPE_INT_ARGB);
		dataBuffer = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();

		for(int j = 0; j < Height; j++) 
		{
			for(int i = 0; i < Width; i++) 
			{
				dataBuffer[j*Width + i] = (processAlpha ? rgb[j*Width + i] : rgb[j*Width + i] | 0xFF000000);
			}
		}
	}

	public PlatformImage(Image image, int x, int y, int Width, int Height, int transform)
	{
		// Create a transformed copy of an image
		BufferedImage sub = new BufferedImage(Width, Height, BufferedImage.TYPE_INT_ARGB);
	
		// Get the raw pixel data from the source image, and the new sub image
		final int[] sourceData = ((DataBufferInt) image.canvas.getRaster().getDataBuffer()).getData();
		final int[] subData = ((DataBufferInt) sub.getRaster().getDataBuffer()).getData();
	
		// Copy pixel data directly to the subimage's databuffer.
		for (int j = 0; j < Height; j++) 
		{
			int sourceRow = (y + j) * image.canvas.getWidth() + x;
			int subRow = j * Width;
	
			// Copy pixel rows from the source image to the new sub-image
			System.arraycopy(sourceData, sourceRow, subData, subRow, Math.min(Width, image.canvas.getWidth() - x));
		}

		canvas = transformImage(sub, transform);
		dataBuffer = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();
	}

	// These constructors and methods are exclusive to DoJa's Image classes
	public PlatformImage(com.nttdocomo.ui.Image source) 
	{
		// Create a copy from a DoJa Image
		if(source == null) { throw new NullPointerException("Can't load image, it is null."); }

		// It's safe to assume that the source image will have the same type as the destination, so instead of drawImage we can just arraycopy the source to the destination
		canvas = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		dataBuffer = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();
		final int[] tempData = ((DataBufferInt) source.getCanvas().getRaster().getDataBuffer()).getData();
		
		System.arraycopy(tempData, 0, dataBuffer, 0, tempData.length);
	}

	public PlatformImage(int Width, int Height, int[] data, int off) 
	{
		// Create DoJa image from int array starting from a given offset
		canvas = new BufferedImage(Width, Height, BufferedImage.TYPE_INT_ARGB);

		dataBuffer = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();
		System.arraycopy(data, off, dataBuffer, 0, Width * Height);
		
		isMutable = true;
	}

	// This only iterates through colors that changed in order to save time (see 'com.nttdocomo.ui.PalettedImage')
	public void updateImagePalette(int[] originalColors, int[] newColors) 
	{
		for(int y = 0; y < getHeight(); y++) 
		{
			for(int x = 0; x < getWidth(); x++) 
			{
				for(int i = 0; i < originalColors.length; i++) 
				{
					if(getPixel(x, y) == originalColors[i]) { setPixel(x, y, newColors[i]); }
				}
			}
		}
	}

	// Siemens methods
	public void set2Bpp(boolean bpp) { this.is2bpp = bpp; }

    public boolean is2Bpp() { return is2bpp; }

	public void setMutable(boolean mutable) { isMutable = mutable; }

	// Common methods
	public int getWidth() { return canvas.getWidth(); }

	public int getHeight() { return canvas.getHeight(); }

	public void getRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height) 
	{
		if (width <= 0 || height <= 0) { return; } // No pixels to copy

		if (rgbData == null) { throw new NullPointerException("Can't use getRGB, as the returned image is null."); }
		if (x < 0 || y < 0 || x + width > canvas.getWidth() || y + height > canvas.getHeight()) 
		{
			throw new IllegalArgumentException("getRGB Requested area exceeds bounds of the image");
		}
		if (Math.abs(scanlength) < width) 
		{
			throw new IllegalArgumentException("scanlength must be >= width");
		}

		// Copy the data into rgbData, taking scanlength into account
		for (int row = 0; row < height; row++) 
		{
			int sourceIndex = (y + row) * canvas.getWidth() + x;
			int destIndex = offset + row * scanlength;
	
			System.arraycopy(dataBuffer, sourceIndex, rgbData, destIndex, width);
		}
	}

	public int getARGB(int x, int y) 
	{ 
		if (x < 0 || y < 0 || x >= canvas.getWidth() || y >= canvas.getHeight()) 
		{
			throw new IllegalArgumentException("Requested area exceeds bounds of the image");
		}
	
		// Get the raw pixel data array directly from the canvas dataBuffer
		return dataBuffer[y * canvas.getWidth() + x];
	}

	public int getPixel(int x, int y) { return getARGB(x, y); }

	public void setPixel(int x, int y, int color)
	{
		if (x < 0 || y < 0 || x >= canvas.getWidth() || y >= canvas.getHeight()) 
		{
			throw new IllegalArgumentException("Requested area exceeds bounds of the image");
		}
	
		// Get the raw pixel data array directly from the canvas
		dataBuffer[y * canvas.getWidth() + x] = color;
	}

	public boolean isMutable() { return isMutable; }

	public static final BufferedImage transformImage(final BufferedImage image, final int transform)
	{
		// Return early if no transform is specified.
		if(transform == Sprite.TRANS_NONE) { return image; }

		final int width = (int)image.getWidth();
		final int height = (int)image.getHeight();

		BufferedImage transimage = null;
		if(transform == Sprite.TRANS_ROT90 || transform == Sprite.TRANS_ROT270 || transform == Sprite.TRANS_MIRROR_ROT90 || transform == Sprite.TRANS_MIRROR_ROT270) 
		{
			transimage = new BufferedImage(height, width, image.getType()); // Non-Math.PI rotations require width and height to be swapped
		}
		else { transimage = new BufferedImage(width, height, image.getType()); }

		// We know the data is of TYPE_INT_ARGB, so just get it directly instead of checking for its type
		final int[] sourceData = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
		final int[] targetData = ((DataBufferInt)transimage.getRaster().getDataBuffer()).getData();
		
		switch (transform) 
		{
			case Sprite.TRANS_ROT90:
				for (int y = 0; y < height; y++) 
				{
					int targetPos = (height - 1 - y);
					for (int x = 0; x < width; x++) 
					{
						targetData[targetPos + x * height] = sourceData[y * width + x];
					}
				}
				//dumpImage(image, null, "");
				//dumpImage(transimage, null, "_rot90");
				break;

			case Sprite.TRANS_ROT180:
				/* 
				 * Since this one also has the effect of mirroring the image horizontally like TRANS_MIRROR alongside a
				 * vertical transformation, we can optimize it by only going up to half of the image's width, making two 
				 * pixel assignments on each inner loop iteration from the image's edges to the center, then checking if 
				 * the width is odd, to just copy the pixel in the middle as it won't change on the transformed image.
				 */
				for (int y = 0; y < height; y++) 
				{
					int targetPos = (height - 1 - y) * width;
					for (int x = 0; x < width / 2; x++) 
					{
						targetData[targetPos + (width - 1 - x)] = sourceData[y * width + x];
						targetData[targetPos + x] = sourceData[y * width + (width - 1 - x)];
					}
					// If image width is odd, copy the middle pixel directly as there's no need to swap anything.
					if (width % 2 != 0) { targetData[targetPos + (width / 2)] = sourceData[y * width + (width / 2)]; }
				}
				//dumpImage(image, null, "");
				//dumpImage(transimage, null, "_rot180");
				break;
			
			case Sprite.TRANS_ROT270:
				for (int y = 0; y < height; y++) 
				{
					for (int x = 0; x < width; x++) 
					{
						targetData[y + (width - 1 - x) * height] = sourceData[y * width + x];
					}
				}
				//dumpImage(image, null, "");
				//dumpImage(transimage, null, "_rot270");
				break;

			case Sprite.TRANS_MIRROR: 
				/*
				* Even though sorting an entire column would be faster from a pure algorithmic perspective (like processing
				* a whole row at once is on TRANS_MIRROR_ROT180), image data tends to be stored so that each row is contiguous 
				* in memory, which makes its access oftentimes MUCH faster than for columns, which could negate the performance 
				* benefits of column access entirely and then some.
				* 
				* This transform is such a case. Making operations on columns, and eliminating that inner row loop actually 
				* results in far worse performance since columns would be accessed way more often. So the next best thing is
				* only working on half of the image's width, just like TRANS_ROT180.
				*/
				for (int y = 0; y < height; y++) 
				{
					int targetRow = y * width;
					for (int x = 0; x < width / 2; x++) 
					{
						targetData[targetRow + (width - 1 - x)] = sourceData[targetRow + x];
						targetData[targetRow + x] = sourceData[targetRow + (width - 1 - x)];
					}
					
					// If image width is odd, copy the middle pixel directly as there's no need to swap anything.
					if (width % 2 != 0) { targetData[targetRow + (width/2)] = sourceData[targetRow + (width/2)]; }
				}
				//dumpImage(image, null, "");
				//dumpImage(transimage, null, "_mirror");
				break;

			case Sprite.TRANS_MIRROR_ROT90:
				for (int y = 0; y < height; y++) 
				{
					int targetRow = height - 1 - y;
					for (int x = 0; x < width; x++) 
					{
						targetData[x * height + targetRow] = sourceData[y * width + (width - 1 - x)];
					}
				}
				//dumpImage(image, null, "");
				//dumpImage(transimage, null, "_mirror90");
				break;

			case Sprite.TRANS_MIRROR_ROT180: // Basically mirror vertically (an arrow pointing up will then point down).
				for (int y = 0; y < height; y++) // Due to this, we copy entire rows at once instead of going pixel by pixel
				{
					System.arraycopy(sourceData, y * width, targetData, (height - 1 - y) * width, width);
				}
				//dumpImage(image, null, "");
				//dumpImage(transimage, null, "_mirror180");
				break;
				
			case Sprite.TRANS_MIRROR_ROT270:
				for (int y = 0; y < height; y++) 
				{
					for (int x = 0; x < width; x++) 
					{
						targetData[(width - 1 - x) * height + y] = sourceData[y * width + (width - 1 - x)];
					}
				}
				//dumpImage(image, null, "");
				//dumpImage(transimage, null, "_mirror270");
				break;
		}

		return transimage;
	}

	public static BufferedImage scaleImage(BufferedImage originalImage, int desiredWidth, int desiredHeight) 
    {

        // Create a new image to draw the scaled version to fit on screen
        BufferedImage scaledImage = new BufferedImage(desiredWidth != 0 ? desiredWidth : originalImage.getWidth(), desiredHeight != 0 ? desiredHeight : originalImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D gc = scaledImage.createGraphics();
        gc.drawImage(originalImage, 0, 0, desiredWidth != 0 ? desiredWidth : originalImage.getWidth(), desiredHeight != 0 ? desiredHeight : originalImage.getHeight(), null);
        gc.dispose();
        
        return scaledImage;
    }

	// TODO: Turn this into a setting. Being able to dump image data would be nice.
	public static void dumpImage(BufferedImage image, String path, String append) 
	{
        try 
		{
			String imageMD5 = generateMD5Hash(image);
			String dumpPath = "." + File.separatorChar + "FreeJ2MEDumps" + File.separatorChar + "Image" + (path != null ? path : File.separatorChar + Mobile.getPlatform().loader.suitename + File.separatorChar);
			File dumpFile = new File(dumpPath);
			
			if (!dumpFile.isDirectory()) { dumpFile.mkdirs(); }
			
			dumpPath = dumpPath + "Image_" + imageMD5 + append + ".png";
			
			dumpFile = new File(dumpPath);
			if(dumpFile.exists()) { return; } // Don't overwrite an image that already exists
            ImageIO.write(image, "png", dumpFile);
            System.out.println("Image saved successfully: " + dumpPath);
        } 
		catch (IOException e) { Mobile.log(Mobile.LOG_ERROR, PlatformImage.class.getPackage().getName() + "." + PlatformImage.class.getSimpleName() + ": " + "Failed to save image file: " + e.getMessage()); }
    }

	private static String generateMD5Hash(BufferedImage image) 
	{
        try 
		{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(imageBytes);

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) { sb.append(String.format("%02x", b)); }

            return sb.toString();
        } 
		catch (Exception e) 
		{
			Mobile.log(Mobile.LOG_ERROR, PlatformImage.class.getPackage().getName() + "." + PlatformImage.class.getSimpleName() + ": " + "Could not generate MD5 Hash for data: " + e.getMessage());
            return null;
        }
    }
}
