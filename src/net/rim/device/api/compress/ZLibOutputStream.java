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
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

// This is really just DeflaterOutputStream, so run on top of it.
public class ZLibOutputStream extends DeflaterOutputStream
{
	public static final int COMPRESSION_NONE = 0;
	public static final int COMPRESSION_BEST = 9;
	public static final int MIN_LOG2_WINDOW_LENGTH = 8;
	public static final int MAX_LOG2_WINDOW_LENGTH = 15;

	public ZLibOutputStream(OutputStream outputStream) throws IOException
	{
		this(outputStream, false, MAX_LOG2_WINDOW_LENGTH, COMPRESSION_NONE);
	}

	public ZLibOutputStream(OutputStream outputStream, boolean noWrap) throws IOException
	{
		this(outputStream, noWrap, MAX_LOG2_WINDOW_LENGTH, COMPRESSION_NONE);
	}

	public ZLibOutputStream(OutputStream outputStream, boolean noWrap, int maxLog2WindowLength) throws IOException
	{
		this(outputStream, noWrap, maxLog2WindowLength, COMPRESSION_NONE);
	}

	public ZLibOutputStream(OutputStream outputStream, boolean noWrap, int maxLog2WindowLength, int compressionValue) throws IOException
	{
		super(outputStream, new Deflater(compressionValue, noWrap));
	}

	// Close is the special case, as we need to manually end the Deflater instantiated above.
	public void close() throws IOException
	{
		try { super.close(); }
		finally { def.end(); }
	}
}
