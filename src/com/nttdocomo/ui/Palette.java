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

import java.util.Vector;
import org.recompile.mobile.Mobile;

public class Palette
{
	private int[] entries;
	private Vector<PalettedImage> boundImages = new Vector<PalettedImage>();

	public Palette(int n)
	{
		if (n <= 0) { throw new IllegalArgumentException("Number of entries must be greater than zero"); }

		entries = new int[Math.min(n, 256)];
		// Spec dictates it should initialize to black
		for (int i = 0; i < entries.length; i++) { entries[i] = 0x000000; }
	}

	public Palette(int[] colors)
	{
		if (colors == null) { throw new NullPointerException("Colors array cannot be null"); }
		if (colors.length == 0) { throw new IllegalArgumentException("Colors array must have at least one entry"); }
		if (colors.length > 256) { throw new IllegalArgumentException("Palette can have a maximum of 256 colors"); }

		// Has to be a copy of the received argument
		entries = new int[colors.length];
		for (int i = 0; i < colors.length; i++) { entries[i] = colors[i] & 0x00FFFFFF; }
	}

	public int getEntry(int index)
	{
		if (index < 0 || index >= entries.length)
		{
			throw new ArrayIndexOutOfBoundsException("Index out of bounds: " + index);
		}
		return entries[index];
	}

	public int getEntryCount() { return entries.length; }

	public void setEntry(int index, int color)
	{
		if (index < 0 || index >= entries.length) { throw new ArrayIndexOutOfBoundsException("Index out of bounds"); }
		if((Mobile.DoJaVersion < 40 && (color < 0x000000 || color > 0xFFFFFF)) ||
			(Mobile.DoJaVersion >= 40 && (color < Integer.MIN_VALUE || color > Integer.MAX_VALUE)))
			{ throw new IllegalArgumentException("Invalid color value: " + String.format("%02X", color)); }

		entries[index] = color & 0x00FFFFFF;

		for (int i = 0; i < boundImages.size(); i++)
		{
			boundImages.get(i).updateImagePalette(index, entries[index]);
		}
	}

	public void addImage(PalettedImage image)
	{
		if (image != null && !boundImages.contains(image)) { boundImages.add(image); }
	}

	public void removeImage(PalettedImage image) { boundImages.remove(image); }
}
