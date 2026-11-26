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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.recompile.mobile.Mobile;

// Just run directly on top of java's GZIPInputStream.
public class GZIPInputStream extends java.util.zip.GZIPInputStream
{
	private ByteArrayInputStream buffer = null;
	
	public GZIPInputStream(InputStream inputStream) throws IOException
	{
		super(inputStream, 1024);
	}

	public GZIPInputStream(InputStream inputStream, int workingBufferSize) throws IOException
	{
		// BlackBerry demands a minimum workingBufferSize of 1024.
		super(inputStream, Math.max(1024, workingBufferSize));
	}
	
    public GZIPInputStream(InputStream in, boolean noHeader) throws IOException {
    	super(in, 1024);
    }
    
	private void checkBuffer() throws IOException
	{
		if (buffer == null) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] tmp = new byte[4096];
			int n;
			while ((n = super.read(tmp, 0, tmp.length)) != -1) {
				out.write(tmp, 0, n);
			}
			buffer = new ByteArrayInputStream(out.toByteArray());
		}
	}
    
	@Override
	public int read() throws IOException
	{
		checkBuffer();
		return buffer.read();
	}

	@Override
	public int read(byte[] b) throws IOException
	{
		checkBuffer();
		return buffer.read(b);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		checkBuffer();
		return buffer.read(b, off, len);
	}

	@Override
	public long skip(long n) throws IOException
	{
		checkBuffer();
		return buffer.skip(n);
	}
	
	@Override
	public int available() throws IOException
	{
		checkBuffer();
		return buffer.available();
	}
	
	@Override
	public void close() throws IOException
	{
		if (buffer != null) {
			buffer.close();
		}
		super.close();
	}
	
	
}
