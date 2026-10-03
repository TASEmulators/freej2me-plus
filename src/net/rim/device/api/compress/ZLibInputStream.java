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
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

// This is really just InflaterInputStream, so run on top of it.
public class ZLibInputStream extends InflaterInputStream
{
	public ZLibInputStream(InputStream inputStream) throws IOException
	{
		this(inputStream, false, 1024);
	}

	public ZLibInputStream(InputStream inputStream, boolean noWrap) throws IOException
	{
		this(inputStream, noWrap, 1024);
	}

	public ZLibInputStream(InputStream inputStream, boolean noWrap, int workingBufferSize) throws IOException
	{
		// Blackberry demands a minimum workingBufferSize of 1024.
		super(inputStream, new Inflater(noWrap), Math.max(1024, workingBufferSize));
	}

	// Close is the special case, as we need to manually end the Inflater instantiated above.
	public void close() throws IOException
	{
		try { super.close(); }
		finally { inf.end(); }
	}
}
