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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;

/*
 * Helper used to work around a long standing limitation of the JDK's own PNG decoder.
 *
 * The 'tRNS' chunk is an ancillary chunk, so a decoder is technically allowed to ignore it.
 * ImageIO's PNGImageReader does honor it for *indexed* (color type 3) images, but for
 * plain grayscale (color type 0) and truecolor RGB (color type 2) images it used to simply
 * drop the chunk, returning a fully opaque image. That was only addressed in JDK 11
 * (JDK-6788458), which means that anyone running FreeJ2ME on a Java 8 runtime, which is
 * still very common for j2me emulation, gets opaque images where the jar clearly asked
 * for a transparent color key.
 *
 * Real handsets (and other emulators, like KEmulator) do apply tRNS in these cases, so
 * whenever the JDK hands us back an image with no alpha for a PNG that does declare a
 * tRNS chunk, we apply the color key ourselves.
 *
 * Sonic 1 Part 1 (Low-End)'s "backicon" sprite is exactly this case: a 26x22, 8bpp
 * grayscale PNG whose tRNS chunk marks the value 0xFF (white) as fully transparent. On a
 * JRE without the JDK 11 fix the arrow ends up drawn over a white box (see issue #250).
 */
public final class PNGUtility
{
	private static final byte[] PNG_SIGNATURE = { (byte)0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' };

	private static final int PNG_COLOR_GRAY = 0;
	private static final int PNG_COLOR_RGB  = 2;

	private PNGUtility() { }

	/*
	 * Reads the whole stream into memory. PNG needs to be inspected as a whole in order to
	 * find its tRNS chunk, and the streams handed to us by the jars are usually just a few
	 * KiB worth of sprite data anyway.
	 */
	public static byte[] readFully(final InputStream stream) throws IOException
	{
		final ByteArrayOutputStream buffer = new ByteArrayOutputStream(Math.max(32, stream.available()));
		final byte[] chunk = new byte[8192];
		int read;

		while((read = stream.read(chunk, 0, chunk.length)) != -1) { buffer.write(chunk, 0, read); }

		return buffer.toByteArray();
	}

	/*
	 * Applies a PNG's tRNS color key to an already decoded image, but only if the decoder
	 * did not do it for us. Returns the image untouched whenever there's nothing to do, so
	 * this is a no-op on runtimes that already behave correctly (JDK 11 and newer).
	 */
	public static BufferedImage applyTransparentColorKey(final BufferedImage image, final byte[] data, final int offset, final int length)
	{
		/* The decoder already gave us an alpha channel, nothing needs patching. */
		if(image == null || image.getColorModel().hasAlpha()) { return image; }

		if(!isPNG(data, offset, length)) { return image; }

		final int colorType = data[offset + 25] & 0xFF;

		/* Indexed PNGs are correctly handled by every JDK version, and gray/rgb + alpha types already carry alpha. */
		if(colorType != PNG_COLOR_GRAY && colorType != PNG_COLOR_RGB) { return image; }

		final int[] transparentSamples = findTRNS(data, offset, length, colorType);
		if(transparentSamples == null) { return image; }

		try { return keyOut(image, transparentSamples); }
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_WARNING, PNGUtility.class.getPackage().getName() + "." + PNGUtility.class.getSimpleName() + ": " + "Failed to apply PNG tRNS color key: " + e.getMessage());
			return image;
		}
	}

	public static BufferedImage applyTransparentColorKey(final BufferedImage image, final byte[] data)
	{
		return applyTransparentColorKey(image, data, 0, data == null ? 0 : data.length);
	}

	private static boolean isPNG(final byte[] data, final int offset, final int length)
	{
		/* Signature (8) + length (4) + "IHDR" (4) + 13 bytes of IHDR payload. */
		if(data == null || length < 8 + 8 + 13) { return false; }

		for(int i = 0; i < PNG_SIGNATURE.length; i++)
		{
			if(data[offset + i] != PNG_SIGNATURE[i]) { return false; }
		}

		return data[offset + 12] == 'I' && data[offset + 13] == 'H' && data[offset + 14] == 'D' && data[offset + 15] == 'R';
	}

	/*
	 * Walks the PNG chunk list looking for tRNS, returning the transparent sample values as
	 * they are stored in the file (one sample for grayscale, three for truecolor), or null
	 * if the chunk isn't present/is malformed.
	 */
	private static int[] findTRNS(final byte[] data, final int offset, final int length, final int colorType)
	{
		int pos = 8; // Skip the PNG signature.

		while(pos + 8 <= length)
		{
			final int chunkLength = readInt(data, offset + pos);
			final int typePos = offset + pos + 4;

			/* Guard against bogus lengths so a corrupt file can't send us out of bounds. */
			if(chunkLength < 0 || pos + 12L + chunkLength > length) { return null; }

			if(data[typePos] == 't' && data[typePos + 1] == 'R' && data[typePos + 2] == 'N' && data[typePos + 3] == 'S')
			{
				final int dataPos = typePos + 4;

				if(colorType == PNG_COLOR_GRAY && chunkLength >= 2)
				{
					return new int[] { ((data[dataPos] & 0xFF) << 8) | (data[dataPos + 1] & 0xFF) };
				}
				else if(colorType == PNG_COLOR_RGB && chunkLength >= 6)
				{
					return new int[]
					{
						((data[dataPos]     & 0xFF) << 8) | (data[dataPos + 1] & 0xFF),
						((data[dataPos + 2] & 0xFF) << 8) | (data[dataPos + 3] & 0xFF),
						((data[dataPos + 4] & 0xFF) << 8) | (data[dataPos + 5] & 0xFF)
					};
				}

				return null;
			}

			/* IDAT marks the start of the pixel data, and tRNS must appear before it. */
			if(data[typePos] == 'I' && data[typePos + 1] == 'D' && data[typePos + 2] == 'A' && data[typePos + 3] == 'T') { return null; }

			pos += 12 + chunkLength; // length + type + payload + crc
		}

		return null;
	}

	/*
	 * Rebuilds the image as ARGB, zeroing the alpha of every pixel matching the color key.
	 *
	 * Matching is done on the *raster samples* rather than on getRGB(), because grayscale
	 * PNGs are decoded into a gray color space and getRGB() would apply a color space
	 * conversion, so the sRGB value would no longer match the raw value stored in tRNS.
	 * Raster samples, on the other hand, are the PNG's own sample values, in the file's own
	 * bit depth, which is exactly how tRNS is specified.
	 */
	private static BufferedImage keyOut(final BufferedImage image, final int[] transparentSamples)
	{
		final int width = image.getWidth();
		final int height = image.getHeight();
		final Raster raster = image.getRaster();
		final int bands = raster.getNumBands();

		/* We can only key out colors if the raster actually has the samples tRNS refers to. */
		if(bands < transparentSamples.length) { return image; }

		final BufferedImage keyed = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		/* Let Java2D do the color conversion, so colors stay exactly as they'd otherwise be. */
		keyed.createGraphics().drawImage(image, 0, 0, null);

		/*
		 * tRNS values are always stored as 16-bit, but they only ever use the lower bits that
		 * fit the image's bit depth, so comparing against the raster samples directly works for
		 * every depth from 1 to 16 bpp.
		 */
		final int[] samples = new int[bands];

		for(int y = 0; y < height; y++)
		{
			for(int x = 0; x < width; x++)
			{
				raster.getPixel(x, y, samples);

				boolean transparent = true;
				for(int i = 0; i < transparentSamples.length; i++)
				{
					if(samples[i] != transparentSamples[i]) { transparent = false; break; }
				}

				if(transparent) { keyed.setRGB(x, y, 0x00000000); }
			}
		}

		return keyed;
	}

	private static int readInt(final byte[] data, final int pos)
	{
		return ((data[pos] & 0xFF) << 24) | ((data[pos + 1] & 0xFF) << 16) | ((data[pos + 2] & 0xFF) << 8) | (data[pos + 3] & 0xFF);
	}
}
