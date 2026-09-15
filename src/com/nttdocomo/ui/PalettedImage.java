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
package com.nttdocomo.ui;

import java.io.InputStream;
import org.recompile.mobile.Mobile;

public abstract class PalettedImage extends Image
{
	protected Palette palette;
	protected int transparentIndex = 0;
	protected int colorTableSize = 0;

	// Direct pixel index array matching the canvas dimensions (width * height)
	protected byte[] pixelIndices;

	protected PalettedImage() { }

	protected PalettedImage(int width, int height)
	{
		super(width, height);
		this.colorTableSize = 256; // Max amount of colors, the app must be able to set any valid palette

		// Palette will be set by the app
		this.pixelIndices = new byte[width * height];
		this.palette = new Palette(256);
		this.palette.addImage(this);
	}

	protected PalettedImage(byte[] data)
	{
		super(data, 0, data.length);
		initializeData(data);
	}

	private void initializeData(byte[] data)
	{
		this.palette = extractPalette(data);
		this.colorTableSize = this.palette.getEntryCount();
		this.transparentIndex = extractTransparentIndex(data);
		this.palette.addImage(this);

		// Build the pixel index map from the initial image state
		buildInitialIndexMap();
	}

	public static PalettedImage createPalettedImage(byte[] data)
	{
		return new PalettedImageImpl(data);
	}

	public static PalettedImage createPalettedImage(InputStream in)
	{
		try
		{
			byte[] tmpData = new byte[in.available()];
			in.read(tmpData, 0, tmpData.length);
			return new PalettedImageImpl(tmpData);
		}
		catch (Exception e) { return null; }
	}

	public static PalettedImage createPalettedImage(int width, int height)
	{
		return new PalettedImageImpl(width, height);
	}

	public void changeData(byte[] data)
	{
		setCanvas(createImage(data, 0, data.length).getCanvas());
		if (this.palette != null)
		{
			this.palette.removeImage(this);
		}
		initializeData(data);
		updateAllPaletteEntries();
	}

	public void changeData(InputStream in)
	{
		try
		{
			byte[] tmpData = new byte[in.available()];
			in.read(tmpData, 0, tmpData.length);
			changeData(tmpData);
		}
		catch (Exception e) { }
	}

	public Palette getPalette() { return palette; }

	// This method is only used to set a palette to this image
	public void setPalette(Palette palette)
	{
		if (palette == null)
		{
			throw new NullPointerException("Palette cannot be null.");
		}
		if (palette.getEntryCount() < colorTableSize)
		{
			throw new IllegalArgumentException("New palette has less entries than current palette size.");
		}

		// Remove this image from the current palette before swapping to new one.
		if (this.palette != null) { this.palette.removeImage(this); }

		this.palette = palette; // We also keep a reference to the current palette this image is bound to, for getPalette.

		// Since multiple images may share the same palette, we add this image to the palette's bound image array.
		this.palette.addImage(this);
		updateAllPaletteEntries();
	}

	public int getTransparentColor() { throw new UnsupportedOperationException("getTransparentColor() cannot be called on PalettedImage."); }

	public int getTransparentIndex() { return transparentIndex; }

	public void setTransparentIndex(int index)
	{
		this.transparentIndex = index;
		updateAllPaletteEntries();
	}

	public Graphics getGraphics() { throw new UnsupportedOperationException("getGraphics() is not supported for PalettedImage."); }

	/**
	 * Map each pixel on the canvas to an index in the extracted Palette.
	 */
	// Since we aren't given the relation between the image's pixels and the
	// initial palette on the creation step, we must do it ourselves... so run
	// through the image data and map the current palette's indices to each pixel.
	private void buildInitialIndexMap()
	{
		int w = getWidth();
		int h = getHeight();
		this.pixelIndices = new byte[w * h];

		if (palette == null || w <= 0 || h <= 0) { return; }

		int numColors = palette.getEntryCount();
		int[] paletteColors = new int[numColors];
		for (int i = 0; i < numColors; i++) { paletteColors[i] = palette.getEntry(i) & 0x00FFFFFF; }

		for (int y = 0; y < h; y++)
		{
			for (int x = 0; x < w; x++)
			{
				int pixel = getPixel(x, y);
				int alpha = (pixel >> 24) & 0xFF;
				int rgb = pixel & 0x00FFFFFF;

				if (alpha == 0)
				{
					pixelIndices[y * w + x] = (byte) transparentIndex;
					continue;
				}

				// Match RGB value to nearest/exact palette entry
				int matchedIndex = 0;
				for (int i = 0; i < numColors; i++)
				{
					if (paletteColors[i] == rgb)
					{
						matchedIndex = i;
						break;
					}
				}
				pixelIndices[y * w + x] = (byte) matchedIndex;
			}
		}
	}

	public void updateImagePalette(int index, int newColor)
	{
		if (pixelIndices == null) { return; }

		int w = getWidth();
		int h = getHeight();
		int targetRgb = newColor & 0x00FFFFFF;

		for (int i = 0; i < pixelIndices.length; i++)
		{
			int idx = pixelIndices[i] & 0xFF;
			if (idx == index)
			{
				int x = i % w;
				int y = i / w;

				// setPixel is available for anything that extends PlatformImage.
				if (idx == transparentIndex) { setPixel(x, y, 0x00000000); }
				else { setPixel(x, y, 0xFF000000 | targetRgb); }
			}
		}
	}

	// Similar to updateImagePalette, but runs for the entire palette at once,
	// which is faster than calling a bunch of "updateImagePalette" for each
	// palette index when creating these images.
	public void updateAllPaletteEntries()
	{
		if (pixelIndices == null || palette == null) { return; }

		int w = getWidth();
		int h = getHeight();
		int total = Math.min(w * h, pixelIndices.length);

		for (int i = 0; i < total; i++)
		{
			int x = i % w;
			int y = i / w;
			int idx = pixelIndices[i] & 0xFF;

			if (idx == transparentIndex) { setPixel(x, y, 0x00000000); }
			else if (idx < palette.getEntryCount()) { setPixel(x, y, 0xFF000000 | (palette.getEntry(idx) & 0x00FFFFFF)); }
		}
	}

	public static Palette extractPalette(byte[] imageData)
	{
		if (imageData.length < 4) { throw new IllegalArgumentException("Image data is too short."); }

		// Check for GIF signature. If it's not GIF, it should be a Microsoft BMP signature, and if not, we don't support it
		if (imageData[0] == 'G' && imageData[1] == 'I' && imageData[2] == 'F') { return extractPaletteFromGIF(imageData); }
		else if (imageData[0] == 'B' && imageData[1] == 'M')
		{
			if(Mobile.DoJaVersion < 50) { throw new UIException(UIException.UNSUPPORTED_FORMAT, "Current DoJa version does not support BMP."); }
			return extractPaletteFromBMP(imageData);
		}
		else { throw new UnsupportedOperationException("Unsupported image format."); }
	}

	private static Palette extractPaletteFromGIF(byte[] gifData)
	{
		if ((gifData[10] & 0x80) != 0) // Check if global color table flag is set
		{
			// Then we determine the size of the color table, as not all gifs will use the whole 256 color palette
			int bitsPerPixel = (gifData[10] & 0x07) + 1;
			int colorTableSize = 1 << bitsPerPixel;
			int[] colors = new int[colorTableSize];
			int headerOffset = 13;

			for (int i = 0; i < colorTableSize; i++)
			{
				int r = gifData[headerOffset + (i * 3)] & 0xFF;
				int g = gifData[headerOffset + (i * 3) + 1] & 0xFF;
				int b = gifData[headerOffset + (i * 3) + 2] & 0xFF;
				colors[i] = (r << 16) | (g << 8) | b;
			}
			return new Palette(colors);
		}
		else
		{
			throw new UnsupportedOperationException("No global color table present in GIF.");
		}
	}

	private static Palette extractPaletteFromBMP(byte[] bmpData)
	{
		int colorTableOffset = 54; // The color table starts at offset 54, that is, immediately after the header
		int bitDepth = bmpData[28] & 0xFF; // Bit depth's header position on Windows/MS BMP
		int maxPaletteSize;

		// Determine the max palette size based on the header's retrieved bit depth
		switch (bitDepth)
		{
			case 1: maxPaletteSize = 2; break;
			case 4: maxPaletteSize = 16; break;
			case 8: maxPaletteSize = 256; break;
			// No support for 16/24/32-bit BMP, as we are limited to a 256 color palette (so the max must be 8bits per pixel)
			default: throw new UnsupportedOperationException("Unsupported BMP bit depth: " + bitDepth);
		}

		// Read the number of colors from the BMP header (it can be any value up to 2^bitDepth, or 0 to default)
		int actualPaletteSize = bmpData[46] & 0xFF;
		if (actualPaletteSize == 0) { actualPaletteSize = maxPaletteSize; } // If it's 0, default to 2^bitDepth colors
		actualPaletteSize = Math.min(actualPaletteSize, maxPaletteSize);

		int[] colors = new int[actualPaletteSize];

		// Read the color table
		for (int i = 0; i < actualPaletteSize; i++)
		{
			int b = bmpData[colorTableOffset + (i * 4)] & 0xFF;
			int g = bmpData[colorTableOffset + (i * 4) + 1] & 0xFF;
			int r = bmpData[colorTableOffset + (i * 4) + 2] & 0xFF;
			colors[i] = (r << 16) | (g << 8) | b;
		}

		return new Palette(colors);
	}

	private static int extractTransparentIndex(byte[] gifData)
	{
		if (gifData[0] != 'G' || gifData[1] != 'I' || gifData[2] != 'F') return 0;

		int pos = 13;
		if ((gifData[10] & 0x80) != 0) { pos += (1 << ((gifData[10] & 0x07) + 1)) * 3; }

		while (pos + 4 < gifData.length)
		{
			// See if we reached the Graphics Control Extension (GCF) block, as it contains the transparency index
			if ((gifData[pos] & 0xFF) == 0x21 && (gifData[pos + 1] & 0xFF) == 0xF9)
			{
				if (gifData[pos + 2] == 4 && (gifData[pos + 3] & 0x01) != 0)
				{
					return gifData[pos + 4] & 0xFF;
				}
				break;
			}
			pos++;
		}

		// If there's no transparency, set the index as 0, GIF defaults to -1 but DoJa spec says it must be set to 0?
		return 0;
	}
}
