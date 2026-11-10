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
package com.nttdocomo.util;

import java.io.IOException;
import java.io.OutputStream;

import org.recompile.mobile.Mobile;

public class ScratchPadOutputStream extends OutputStream
{
	byte[] data;
	int pos, len, spIndex;

	ScratchPadOutputStream(byte[] data, int pos, int len, int spIndex) 
	{ 
		this.data = data;
		this.pos = pos;
		this.len = len+pos;
		this.spIndex = spIndex;
	}

	public void close() throws IOException 
	{ 
		try 
		{ 
			ScratchPadConnection.writeScratchPad(spIndex, data);
			super.close();
		}
		finally
		{
			data = null;
			Mobile.log(Mobile.LOG_DEBUG, ScratchPadOutputStream.class.getPackage().getName() + "." + ScratchPadOutputStream.class.getSimpleName() + ": " + " Closed scratchPad data");
		}
	}
		
	public void flush() throws IOException 
	{ 
		super.flush();
		Mobile.log(Mobile.LOG_DEBUG, ScratchPadOutputStream.class.getPackage().getName() + "." + ScratchPadOutputStream.class.getSimpleName() + ": " + " Flushed scratchPad data");
	}

	public void write(byte[] b)
	{
		if (b == null) { throw new NullPointerException("Input byte array cannot be null."); }
		write(b, 0, b.length);
	}

	public void write(byte[] b, int off, int length) 
	{
		Mobile.log(Mobile.LOG_DEBUG, ScratchPadOutputStream.class.getPackage().getName() + "." + ScratchPadOutputStream.class.getSimpleName() + ": " + " Writing byte array from data index " + pos + " to " + len);
		if (b == null) { throw new NullPointerException("Input byte array cannot be null."); }
		if (off < 0 || length < 0 || off >= b.length) { throw new IndexOutOfBoundsException("Invalid offset or length."); }

		int availableSpace = len - pos;
		int bytesToWrite = Math.min(length, availableSpace);

		// Only write data that doesn't go out of the ScratchPad's bounds, as OOB data is ignored
		if (bytesToWrite > 0) 
		{
			System.arraycopy(b, off, data, pos, bytesToWrite);
			pos += bytesToWrite;
		}
	}

	public void write(int b)
	{
		Mobile.log(Mobile.LOG_DEBUG, ScratchPadOutputStream.class.getPackage().getName() + "." + ScratchPadOutputStream.class.getSimpleName() + ": " + " Writing byte value " + (byte) (b & 0xFF) + " to data index " + pos);
		if (pos >= data.length) { return; }

		data[pos] = (byte) (b & 0xFF);
		pos++;
	}
}
