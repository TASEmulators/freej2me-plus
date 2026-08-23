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
package javax.microedition.m3g;

import org.recompile.mobile.Mobile;

public class Image2D extends Object3D
{

	public static final int ALPHA = 96;
	public static final int LUMINANCE = 97;
	public static final int LUMINANCE_ALPHA = 98;
	public static final int RGB = 99;
	public static final int RGBA = 100;


	private byte[] image;
	private int width;
	private int height;
	private int format;
	private int bpp;
	private boolean mutable;

	public static final String[] formatNames = {"ALPHA", "LUMINANCE", "LUMINANCE_ALPHA", "RGB", "RGBA"};

	public Image2D(int format, int w, int h)
	{
		/* As per JSR-184, throw IllegalArgumentException if format or dimensions are invalid. */
		validateFormat(format);
		validateDimensions(w, h);

		this.mutable = true;
		this.width = w;
		this.height = h;
		this.format = format;
		setBpp();
		this.image = new byte[w * h * this.bpp];
	}

	public Image2D(int format, int w, int h, byte[] image)
	{
		/* As per JSR-184, throw NullPointerException if the received image is null. */
		if (image == null) { throw new NullPointerException("Tried to construct Image2D with null image. "); }

		/* Also per JSR-184, throw IllegalArgumentException if format or dimensions are invalid. */
		validateFormat(format);
		validateDimensions(w, h);

		this.format = format;
		setBpp();

		int len = w * h * this.bpp;
		if (image.length < len)
		{
			throw new IllegalArgumentException("Image byte array too small. Expected size: " + len + ", actual size: " + image.length);
		}

		Mobile.log(Mobile.LOG_DEBUG, Image2D.class.getPackage().getName() + "." + Image2D.class.getSimpleName() + ": " +  "M3G Byte Image Format: " + formatNames[format-96]);

		this.mutable = false;
		this.width = w;
		this.height = h;

		this.image = image;
	}

	public Image2D(int format, int w, int h, byte[] image, byte[] palette)
	{
		/* As per JSR-184, throw NullPointerException if the received image or palette are null. */
		if (image == null) { throw new NullPointerException("Tried to construct Image2D with null image. "); }
		if (palette == null) { throw new NullPointerException("Image Palette array cannot be null."); }

		/* Also per JSR-184, throw IllegalArgumentException if format or dimensions are invalid. */
		validateFormat(format);
		validateDimensions(w, h);

		/*
		 * Also per JSR-184, throw IllegalArgumentException if (palette.length < 256*C) && ((palette.length % C) != 0),
		 * where C is the number of color components (for instance, 3 for RGB).
		 */
		this.format = format;
		setBpp();

		if (palette.length < 256 * this.bpp && (palette.length % this.bpp) != 0)
			{ throw new IllegalArgumentException("Illegal palette length: " + palette.length); }

		Mobile.log(Mobile.LOG_DEBUG, Image2D.class.getPackage().getName() + "." + Image2D.class.getSimpleName() + ": " +  "M3G Paletted Image Format: " + formatNames[format-96] + " indices len: " + image.length + " palette len:" + palette.length);

		this.mutable = false;
		this.width = w;
		this.height = h;

		// We now start to copy the received "image" comprised of palette indices, as well as the palette colors themselves.
		this.image = new byte[image.length * this.bpp];

		for(int i = 0; i < image.length; i++)
		{
			/*
			 * Due to that, we get its data by reading the received image[] multiplied by bpp. Also, those values
			 * are unsigned (as there will be 256 entries in the palette), while java treats its native types
			 * as signed. So we are required to do that bitwise AND operation to make them unsigned when reading
			*/
			int pIdx = (image[i] & 0xFF) * this.bpp;
			int offset = i * this.bpp;
			// The pallete will be 256 entries multiplied by the format's amount of bytes per pixel
			for (int k = 0; k < bpp; k++) { this.image[offset + k] = palette[pIdx + k]; }
		}
	}

	public Image2D(int format, Object image)
	{
		/* As per JSR-184, throw NullPointerException if the received image is null. */
		if (image == null) { throw new NullPointerException("Tried to construct Image2D with null image. "); }

		/* Also per JSR-184, throw IllegalArgumentException if format is not one of the constants. */
		validateFormat(format);

		/* Also per JSR-184, throw IllegalArgumentException if image is not a valid instance of the supported Image classes. */
		if (!(image instanceof javax.microedition.lcdui.Image) && !(image instanceof java.awt.Image))
			{ throw new IllegalArgumentException("The image object received is not appropriate to this implementation."); }

		Mobile.log(Mobile.LOG_DEBUG, Image2D.class.getPackage().getName() + "." + Image2D.class.getSimpleName() + ": " +  "M3G Image Format:" + formatNames[format-96]);

		javax.microedition.lcdui.Image img = (javax.microedition.lcdui.Image) image;
		this.mutable = false;
		this.width = img.getWidth();
		this.height = img.getHeight();
		this.format = format;
		setBpp();

		this.image = new byte[this.width * this.height * this.bpp];

		int[] argb = new int[this.width * this.height];
		img.getRGB(argb, 0, this.width, 0, 0, this.width, this.height);

		int idx = 0;
		for (int pixel : argb)
		{
			int a = (pixel >> 24) & 0xFF;
			int r = (pixel >> 16) & 0xFF;
			int g = (pixel >> 8) & 0xFF;
			int b = pixel & 0xFF;

			switch (this.format)
			{
				case ALPHA:
					this.image[idx++] = (byte) a;
					break;
				case LUMINANCE:
					this.image[idx++] = (byte) ((r + g + b) / 3);
					break;
				case LUMINANCE_ALPHA:
					this.image[idx++] = (byte) ((r + g + b) / 3);
					this.image[idx++] = (byte) a;
					break;
				case RGB:
					this.image[idx++] = (byte) r;
					this.image[idx++] = (byte) g;
					this.image[idx++] = (byte) b;
					break;
				case RGBA:
					this.image[idx++] = (byte) r;
					this.image[idx++] = (byte) g;
					this.image[idx++] = (byte) b;
					this.image[idx++] = (byte) a;
					break;
			}
		}
	}

	protected Object3D duplicateImpl()
	{
		Image2D copy = (Image2D) super.duplicateImpl();
		copy.image = this.image == null ? null : (byte[]) this.image.clone();
		copy.width = this.width;
		copy.height = this.height;
		copy.format = this.format;
		copy.bpp = this.bpp;
		copy.mutable = this.mutable;
		return copy;
	}


	public int getFormat() { return this.format; }

	public int getHeight() { return this.height; }

	public int getWidth() { return this.width; }

	public boolean isMutable() { return this.mutable; }

	public void set(int x, int y, int w, int h, byte[] image)
	{
		/* As per JSR-184, throw...
		 * NullPointerException if the received image is null.
		 * IllegalStateException if this Image2D object is immutable.
		 * IllegalStateException if x < 0 or y < 0 or width <= 0 or height <= 0
		 * IllegalStateException if image.length < (width * height * bpp)
		 */
		if (image == null) { throw new java.lang.NullPointerException("Received null image."); }
		if (!this.mutable) { throw new java.lang.IllegalStateException("This Image2D object is not mutable."); }
		if (x < 0 || y < 0 || w <= 0 || h <= 0 || (x + w) > this.width || (y + h) > this.height)
			{ throw new java.lang.IllegalArgumentException("Tried to set image with invalid parameters."); }

		if (image.length < w * h * this.bpp)
		{
			throw new IllegalArgumentException("Source image cannot smaller than specified region");
		}

		for (int row = 0; row < h; row++)
		{
			int src = row * w * this.bpp;
			int dest = ((y + row) * this.width + x) * this.bpp;
			System.arraycopy(image, src, this.image, dest, w * this.bpp);
		}
	}

	// We do not handle OOB x and y positions here, Graphics3D does that in the clear/render loops
	final int getPixel(int x, int y)
	{
		int offset = this.bpp * (this.width * y + x);

		switch (this.format)
		{
			case ALPHA:
				return ((this.image[offset] & 0xFF) << 24);
			case LUMINANCE:
				int lum = this.image[offset] & 0xFF;
				return 0xFF000000 | (lum << 16) | (lum << 8) | lum;
			case LUMINANCE_ALPHA:
				int laLum = this.image[offset] & 0xFF;
				int laAlpha = this.image[offset + 1] & 0xFF;
				return (laAlpha << 24) | (laLum << 16) | (laLum << 8) | laLum;
			case RGB:
				return 0xFF000000
					| ((this.image[offset] & 0xFF) << 16)
					| ((this.image[offset + 1] & 0xFF) << 8)
					| (this.image[offset + 2] & 0xFF);
			case RGBA:
				return ((this.image[offset + 3] & 0xFF) << 24)
					| ((this.image[offset] & 0xFF) << 16)
					| ((this.image[offset + 1] & 0xFF) << 8)
					| (this.image[offset + 2] & 0xFF);
			default:
				return 0;
		}
	}

	private static final void validateFormat(int format)
	{
		if (format < ALPHA || format > RGBA)
		{
			throw new IllegalArgumentException("Invalid image format: " + format);
		}
	}

	private static final void validateDimensions(int w, int h)
	{
		if (w <= 0 || h <= 0)
		{
			throw new IllegalArgumentException("Width and height must be > 0");
		}
	}

	private final void setBpp()
	{
		switch (this.format)
		{
			case ALPHA:
				this.bpp = 1;
				break;
			case LUMINANCE:
				this.bpp = 1;
				break;
			case LUMINANCE_ALPHA:
				this.bpp = 2;
				break;
			case RGB:
				this.bpp = 3;
				break;
			case RGBA:
				this.bpp = 4;
				break;
			default:
				this.bpp = 0;
		}
	}

	static final boolean isPowerOfTwo(int value) { return value > 0 && ((value & (value-1)) == 0); }
}
