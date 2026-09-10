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

	int[] image;
	int width;
	int height;
	int widthShift = 0;
	private int format;
	private boolean mutable;
	boolean isPOT = false;

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
		this.isPOT = isPowerOfTwo(w) && isPowerOfTwo(h);
		this.widthShift = Integer.numberOfTrailingZeros(w);
		this.image = new int[w * h];

	}

	public Image2D(int format, int w, int h, byte[] image)
	{
		/* As per JSR-184, throw NullPointerException if the received image is null. */
		if (image == null) { throw new NullPointerException("Tried to construct Image2D with null image. "); }

		/* Also per JSR-184, throw IllegalArgumentException if format or dimensions are invalid. */
		validateFormat(format);
		validateDimensions(w, h);

		this.format = format;
		int bpp = getBpp();
		int len = w * h * bpp;
		if (image.length < len)
		{
			throw new IllegalArgumentException("Image byte array too small. Expected size: " + len + ", actual size: " + image.length);
		}

		Mobile.log(Mobile.LOG_DEBUG, Image2D.class.getPackage().getName() + "." + Image2D.class.getSimpleName() + ": " +  "M3G Byte Image Format: " + formatNames[format-96]);

		this.isPOT = isPowerOfTwo(w) && isPowerOfTwo(h);
		this.widthShift = Integer.numberOfTrailingZeros(w);
		this.mutable = false;
		this.width = w;
		this.height = h;

		this.image = new int[w * h];
		toARGB(image, format, bpp, this.image);
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
		this.isPOT = isPowerOfTwo(w) && isPowerOfTwo(h);
		this.widthShift = Integer.numberOfTrailingZeros(w);

		int bpp = getBpp();
		if (palette.length < 256 * bpp && (palette.length % bpp) != 0)
			{ throw new IllegalArgumentException("Illegal palette length: " + palette.length); }

		Mobile.log(Mobile.LOG_DEBUG, Image2D.class.getPackage().getName() + "." + Image2D.class.getSimpleName() + ": " +  "M3G Paletted Image Format: " + formatNames[format-96] + " indices len: " + image.length + " palette len:" + palette.length);

		this.mutable = false;
		this.width = w;
		this.height = h;

		/*
		 * Some exporters (e.g. Firemint's, seen in NFS Most Wanted) write RGBA palettes
		 * where the alpha byte of EVERY entry is 0 - a leftover "reserved" byte rather
		 * than real transparency data. Taken literally, every texel would be fully
		 * transparent and the whole mesh invisible, which is clearly never intended
		 * (a fully-transparent texture is useless). Detect that degenerate case and
		 * treat such palettes as fully opaque. Palettes with any non-zero alpha entry
		 * (i.e. real cutout/translucency data) are left untouched.
		 * TODO: A hack, might cause issues. Should be revisited
		 */
		boolean forceOpaque = false;
		if (this.format == RGBA)
		{
			forceOpaque = true;
			for (int p = 3; p < palette.length; p += 4)
			{
				if (palette[p] != 0) { forceOpaque = false; break; }
			}
		}

		// We now start to copy the received "image" comprised of palette indices, as well as the palette colors themselves.
		this.image = new int[w * h];

		palettedToARGB(image, palette, format, bpp, forceOpaque, this.image);
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
		this.isPOT = isPowerOfTwo(this.width) && isPowerOfTwo(this.height);
		this.widthShift = Integer.numberOfTrailingZeros(this.width);

		this.image = new int[this.width * this.height];

		img.getRGB(this.image, 0, this.width, 0, 0, this.width, this.height);

		int idx = 0;
		for (int i = 0; i < this.width * this.height; i++)
		{
			int pixel = this.image[i];
			int a = (pixel >> 24) & 0xFF;
			int r = (pixel >> 16) & 0xFF;
			int g = (pixel >> 8) & 0xFF;
			int b = pixel & 0xFF;

			switch (this.format)
			{
				case ALPHA:
					this.image[i] = (a << 24);
					break;
				case LUMINANCE:
					int lum = (r + g + b) / 3;
					this.image[i] = 0xFF000000 | (lum << 16) | (lum << 8) | lum;
					break;
				case LUMINANCE_ALPHA:
					int laLum = (r + g + b) / 3;
					this.image[i] = (a << 24) | (laLum << 16) | (laLum << 8) | laLum;
					break;
				case RGB:
					this.image[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
					break;
				case RGBA:
					this.image[i] = (a << 24) | (r << 16) | (g << 8) | b;
					break;
			}
		}
	}

	protected Object3D duplicateImpl()
	{
		Image2D copy = (Image2D) super.duplicateImpl();
		copy.image = this.image == null ? null : (int[]) this.image.clone();
		copy.width = this.width;
		copy.height = this.height;
		copy.format = this.format;
		copy.mutable = this.mutable;
		copy.isPOT = this.isPOT;
		copy.widthShift = this.widthShift;
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

		int bpp = getBpp();
		if (image.length < w * h * bpp)
		{
			throw new IllegalArgumentException("Source image cannot smaller than specified region");
		}

		int srcOffset = 0;
		for (int row = 0; row < h; row++)
		{
			int destRowOffset = (y + row) * this.width + x;
			for (int col = 0; col < w; col++)
			{
				this.image[destRowOffset + col] = pixelToARGB(image, srcOffset, this.format);
				srcOffset += bpp;
			}
		}
	}

	// We do not handle OOB x and y positions here, Graphics3D does that in the clear/render loops
	final int getPixel(int x, int y)
	{
		return this.image[this.isPOT ? (y << this.widthShift) + x : (y * this.width) + x];
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

	static final boolean isPowerOfTwo(int value) { return value > 0 && ((value & (value-1)) == 0); }

	// Used for mipmap generation and render to image.
	void setPixel(int x, int y, int argb)
	{
		this.image[this.isPOT ? (y << this.widthShift) + x : (y * this.width) + x] = argb;
	}

	private final byte getBpp()
	{
		switch (this.format)
		{
			case ALPHA:
			case LUMINANCE:
				return 1;
			case LUMINANCE_ALPHA:
				return 2;
			case RGB:
				return 3;
			case RGBA:
				return 4;
			default:
				return 0;
		}
	}

	private static void toARGB(byte[] src, int format, int bpp, int[] dest)
	{
		int totalPixels = dest.length;
		int srcOffset = 0;

		for (int i = 0; i < totalPixels; i++)
		{
			dest[i] = pixelToARGB(src, srcOffset, format);
			srcOffset += bpp;
		}
	}

	private static void palettedToARGB(byte[] srcIndices, byte[] palette, int format, int bpp, boolean forceOpaque, int[] dest)
	{
		int totalPixels = dest.length;

		for (int i = 0; i < totalPixels; i++)
		{
			int pIdx = (srcIndices[i] & 0xFF) * bpp;
			int argb = pixelToARGB(palette, pIdx, format);
			if (forceOpaque) { argb |= 0xFF000000; }
			dest[i] = argb;
		}
	}

	private static int pixelToARGB(byte[] data, int offset, int format)
	{
		switch (format)
		{
			case ALPHA:
				return ((data[offset] & 0xFF) << 24);
			case LUMINANCE:
				int lum = data[offset] & 0xFF;
				return 0xFF000000 | (lum << 16) | (lum << 8) | lum;
			case LUMINANCE_ALPHA:
				int laLum = data[offset] & 0xFF;
				int laAlpha = data[offset + 1] & 0xFF;
				return (laAlpha << 24) | (laLum << 16) | (laLum << 8) | laLum;
			case RGB:
				return 0xFF000000 | ((data[offset] & 0xFF) << 16) | ((data[offset + 1] & 0xFF) << 8) | (data[offset + 2] & 0xFF);
			case RGBA:
				return ((data[offset + 3] & 0xFF) << 24) | ((data[offset] & 0xFF) << 16) | ((data[offset + 1] & 0xFF) << 8) | (data[offset + 2] & 0xFF);
			default:
				return 0;
		}
	}
}
