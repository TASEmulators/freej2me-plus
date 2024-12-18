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

import javax.microedition.lcdui.game.Sprite;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformImage;


public class Image
{

	public PlatformImage platformImage;

	public int width;
	public int height;


	public static Image createImage(byte[] imageData, int imageOffset, int imageLength) throws IllegalArgumentException
	{
		Mobile.log(Mobile.LOG_DEBUG, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Create Image from image data ");
		if (imageData == null) {throw new NullPointerException();}
		if (imageOffset + imageLength > imageData.length) {throw new ArrayIndexOutOfBoundsException();}

		return new PlatformImage(imageData, imageOffset, imageLength);
	}

	// The only difference here is that DirectUtils creates a mutable image, not an immutable one.
	public static Image createNokiaImage(byte[] imageData, int imageOffset, int imageLength) throws IllegalArgumentException
	{
		Mobile.log(Mobile.LOG_DEBUG, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Create DirectUtils Image from image data ");
		if (imageData == null) {throw new NullPointerException();}
		if (imageOffset + imageLength > imageData.length) {throw new ArrayIndexOutOfBoundsException();}

		PlatformImage img = new PlatformImage(imageData, imageOffset, imageLength);
		img.setMutable(true);

		return img;
	}

	public static Image createImage(Image source)
	{
		Mobile.log(Mobile.LOG_DEBUG, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Create Image from Image ");
		if (source == null) {throw new NullPointerException();}
		// If the source is immutable, just return it, despite the docs not mentioning it directly
		if (!source.isMutable()) { return source; }

		// Else, create an immutable copy of the received image.
		PlatformImage newSource = new PlatformImage(source);
		newSource.setMutable(false);
		
		return new PlatformImage(source);
	}

	public static Image createImage(Image img, int x, int y, int width, int height, int transform)
	{
		Mobile.log(Mobile.LOG_DEBUG, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Create Image from sub-image " + " img_w:" + Integer.toString(img.getWidth()) + " img_h:" + Integer.toString(img.getHeight()) + " x:" + Integer.toString(x) + " y:" + Integer.toString(y) + " width:" + Integer.toString(width) + " height:" + Integer.toString(height));
		if (img == null) {throw new NullPointerException();}
		if (x+width > img.getWidth() || y+height > img.getHeight()) {throw new IllegalArgumentException();}
		if (width <= 0 || height <= 0) {throw new IllegalArgumentException();}

		if(!img.isMutable() && (x+width == img.getWidth() && y+height == img.getHeight()) && transform == Sprite.TRANS_NONE) { return img; }

		return new PlatformImage(img, x, y, width, height, transform);
	}

	public static Image createImage(InputStream stream) throws IOException, IllegalArgumentException
	{
		Mobile.log(Mobile.LOG_DEBUG, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Create Image stream");
		if (stream == null) {throw new NullPointerException();}
		return new PlatformImage(stream);
	}

	public static Image createImage(int width, int height)
	{
		Mobile.log(Mobile.LOG_DEBUG, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Create Image w,h " + width + ", " + height);
		if (width <= 0 || height <= 0) {throw new IllegalArgumentException();}

		PlatformImage img = new PlatformImage(width, height);
		img.setMutable(true);
		return img;
	}

	public static Image createImage(String name) throws IOException
	{
		Mobile.log(Mobile.LOG_DEBUG, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Create Image " + name);
		if (name == null) {throw new NullPointerException();}
		return new PlatformImage(name);
	}

	public static Image createRGBImage(int[] rgb, int width, int height, boolean processAlpha)
	{
		Mobile.log(Mobile.LOG_DEBUG, Image.class.getPackage().getName() + "." + Image.class.getSimpleName() + ": " + "Create Image RGB " + width + ", " + height);
		if (rgb == null) {throw new NullPointerException();}
		if (width <= 0 || height <= 0) {throw new IllegalArgumentException();}
		if (rgb.length < width * height) {throw new ArrayIndexOutOfBoundsException();}
		return new PlatformImage(rgb, width, height, processAlpha);
	}

	public Graphics getGraphics() { return platformImage.getGraphics(); }

	public int getHeight() { return height; }

	public void getRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height) {  }

	public int getWidth() { return width; }

	public boolean isMutable() { return platformImage.isMutable(); }

}
