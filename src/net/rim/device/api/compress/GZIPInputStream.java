/*
	This file is part of FreeJ2ME.

	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	FreeJ2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILTY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/
*/
package net.rim.device.api.compress;

import java.io.IOException;
import java.io.InputStream;

// Just run directly on top of java's GZIPInputStream.
public class GZIPInputStream extends java.util.zip.GZIPInputStream
{
	public GZIPInputStream(InputStream inputStream) throws IOException
	{
		this(inputStream, 1024);
	}

	public GZIPInputStream(InputStream inputStream, int workingBufferSize) throws IOException
	{
		// BlackBerry demands a minimum workingBufferSize of 1024.
		super(inputStream, Math.max(1024, workingBufferSize));
	}
}
