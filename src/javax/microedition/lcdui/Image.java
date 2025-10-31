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
package javax.microedition.lcdui;

import java.io.IOException;
import java.io.InputStream;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformImage;


public class Image extends PlatformImage
{

	protected Image() { super(); }

	protected Image(int Width, int Height) { super(Width, Height); }

	protected Image(String name) throws IOException { super(name); }

	protected Image(InputStream stream) throws IOException { super(stream); }
	
	protected Image(int Width, int Height, int ARGBcolor) { super(Width, Height, ARGBcolor); }

	protected Image(Image source) { super(source); }

	protected Image(byte[] imageData, int imageOffset, int imageLength, boolean mutable) { super(imageData, imageOffset, imageLength, mutable); }

	protected Image(int[] rgb, int Width, int Height, boolean processAlpha) { super(rgb, Width, Height, processAlpha); }

	protected Image(Image image, int x, int y, int Width, int Height, int transform) { super(image, x, y, Width, Height, transform); } 

	// TODO: This will create mutable images for both MIDP (shouldn't) and Nokia DirectGraphics (should)
	public static Image createImage(byte[] imageData, int imageOffset, int imageLength) throws IllegalArgumentException
	{
		Mobile.log(Mobile.LOG_DEBUG, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Create Image from image data ");
		if (imageData == null) { throw new NullPointerException("Image data cannot be null"); }
		if (imageOffset + imageLength > imageData.length) { throw new ArrayIndexOutOfBoundsException("Access range goes out of Image data's bounds"); }

		return new Image(imageData, imageOffset, imageLength, false);
	}

	public static Image createImage(byte[] imageData, int imageOffset, int imageLength, boolean mutable) throws IllegalArgumentException
	{
		Mobile.log(Mobile.LOG_DEBUG, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Create Image from image data with mutable flag ");
		if (imageData == null) { throw new NullPointerException("Image data cannot be null"); }
		if (imageOffset + imageLength > imageData.length) { throw new ArrayIndexOutOfBoundsException("Access range goes out of Image data's bounds"); }

		return new Image(imageData, imageOffset, imageLength, mutable);
	}

	public static Image createImage(Image source)
	{
		Mobile.log(Mobile.LOG_DEBUG, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Create Image from Image ");
		if (source == null) {throw new NullPointerException();}
		// If the source is immutable, just return it, despite the docs not mentioning it directly
		if (!source.isMutable()) { return source; }

		// Else, create an immutable copy of the received image.
		return new Image(source);
	}

	public static Image createImage(Image img, int x, int y, int width, int height, int transform)
	{
		Mobile.log(Mobile.LOG_DEBUG, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Create Image from sub-image " + " img_w:" + img.getWidth() + " img_h:" + img.getHeight() + " x:" + x + " y:" + y + " width:" + width + " height:" + height + " trans:" + transform);
		if (img == null) { throw new NullPointerException("Source image cannot be null"); }
		if (x+width > img.getWidth() || y+height > img.getHeight()) { throw new IllegalArgumentException("Access range goes out of Image data's bounds"); }
		if (width <= 0 || height <= 0) { throw new IllegalArgumentException("Invalid width or height value for image"); }

		return new Image(img, x, y, width, height, transform);
	}

	public static Image createImage(InputStream stream) throws IOException, IllegalArgumentException
	{
		Mobile.log(Mobile.LOG_DEBUG, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Create Image stream");
		if (stream == null) { throw new NullPointerException("Image stream cannot be null"); }
		return new Image(stream);
	}

	public static Image createImage(int width, int height)
	{
		Mobile.log(Mobile.LOG_DEBUG, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Create Image w,h " + width + ", " + height);
		if (width <= 0 || height <= 0) { throw new IllegalArgumentException("Invalid width or height value for image"); }

		return new Image(width, height);
	}

	public static Image createImage(int width, int height, int ARGBcolor)
	{
		Mobile.log(Mobile.LOG_DEBUG, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Create Image w,h,color " + width + ", " + height  + ", " + ARGBcolor);
		if (width <= 0 || height <= 0) { throw new IllegalArgumentException("Invalid width or height value for image"); }

		return new Image(width, height, ARGBcolor);
	}

	public static Image createImage(String name) throws IOException
	{
		Mobile.log(Mobile.LOG_DEBUG, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Create Image " + name);
		if (name == null) { throw new NullPointerException("Image name cannot be null"); }
		return new Image(name);
	}

	public static Image createRGBImage(int[] rgb, int width, int height, boolean processAlpha)
	{
		Mobile.log(Mobile.LOG_DEBUG, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Create Image RGB " + width + ", " + height);
		if (rgb == null) { throw new NullPointerException("RGB array cannot be null"); }
		if (width <= 0 || height <= 0) { throw new IllegalArgumentException("Invalid width or height value for image"); }
		if (width * height > rgb.length) { throw new ArrayIndexOutOfBoundsException("Access range goes out of Image data's bounds"); }
		return new Image(rgb, width, height, processAlpha);
	}

	public Graphics getGraphics() { return getMIDPGraphics(); }

	// Some early siemens jars like Mine in Line, for whatever reason, expect some siemens.mp.ui Image methods to be here.

	public static Image createImageFromBitmap(byte[] imageData, int imageWidth, int imageHeight) 
	{
		if (imageWidth <= 0 || imageHeight <= 0) { throw new IllegalArgumentException("Width and height must be greater than zero.");}

		Image image = Image.createImage(imageWidth, imageHeight);
		int[] rgbData = image.getDataBuffer();

		for (int j = 0; j < imageHeight; j++) 
		{
			for (int i = 0; i < imageWidth; i++) 
			{
				int byteIndex = (j * imageWidth + i) / 8;
				int bitIndex = (j * imageWidth + i) % 8;

				int pixelValue = (imageData[byteIndex] >> (7 - bitIndex)) & 0x01;

				// Set the color based on the pixel value, 1 for black and 0 for white
				rgbData[j * imageWidth + i] = (pixelValue == 1) ? 0xFF000000 : 0xFFFFFFFF; // Black or White
			}
		}

		image.set2Bpp(false);
		return image;
	}
}
