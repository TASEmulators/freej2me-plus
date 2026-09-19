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
package com.nec.media;

import java.io.InputStream;
import java.io.IOException;

import org.recompile.mobile.Mobile;

public final class Media
{
	public static final AudioClip getAudioClip(String location) throws IllegalArgumentException
	{
		try
		{
			InputStream stream = Mobile.getPlatform().loader.getResourceAsStream(location);
			byte[] data = new byte[stream.available()];
			stream.read(data);
			return new AudioClipImpl(data);
		}
		catch (IOException e) { throw new IllegalArgumentException("Invalid data: " + location); }
	}
}
