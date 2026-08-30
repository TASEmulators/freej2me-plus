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
package javax.microedition.io;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.microedition.io.file.FileConnectionImpl;
import javax.wireless.messaging.MessageConnectionImpl;

import org.recompile.mobile.Mobile;

public class Connector
{

	public static final int READ = 1;
	public static final int READ_WRITE = 3;
	public static final int WRITE = 2;

	private static OutputStream output = null;

	public static InputStream openInputStream(String name) throws IOException
	{
		if (name.startsWith("scratchpad:"))
		{
			return new com.nttdocomo.util.ScratchPadConnection(name).openInputStream();
		}
		if (name.startsWith("file://"))
        {
            InputConnection conn = (InputConnection) open(name, READ, false);
            return conn.openInputStream();
        }
		return new InputConnectionImpl(name).openInputStream();
	}

	public static DataInputStream openDataInputStream(String name) throws IOException
	{
		if (name.startsWith("scratchpad:"))
		{
			return new com.nttdocomo.util.ScratchPadConnection(name).openDataInputStream();
		}
		if (name.startsWith("file://"))
        {
            InputConnection conn = (InputConnection) open(name, READ, false);
            return conn.openDataInputStream();
        }
		return new InputConnectionImpl(name).openDataInputStream();
	}

	public static Connection open(String name) throws IOException { return open(name, 3, false); }

	public static Connection open(String name, int mode) throws IOException { return open(name, mode, false); }

	public static Connection open(String name, int mode, boolean timeouts) throws IOException
	{

		if (name.startsWith("scratchpad:"))
		{
			return new com.nttdocomo.util.ScratchPadConnection(name);
		}

		if (name.startsWith("resource:")) // older Siemens phones?
		{
			return new InputConnectionImpl(name);
		}

		if (name.startsWith("http://") || name.startsWith("https://") || name.startsWith("socket://")) { return new HttpConnectionImpl(name); }

		// JSR-75 File Connection API
        if (name.startsWith("file://"))
        {
            return new FileConnectionImpl(name, mode);
        }

		if(Mobile.usingMessagingAPI)
		{
			return new MessageConnectionImpl(name);
		}

		throw new ConnectionNotFoundException("Unsupported protocol: " + name);
	}

	public static DataOutputStream openDataOutputStream(String name) throws IOException
	{
		if (name.startsWith("scratchpad:"))
		{
			return new com.nttdocomo.util.ScratchPadConnection(name).openDataOutputStream();
		}
		if (name.startsWith("file://"))
        {
            OutputConnection conn = (OutputConnection) open(name, WRITE, false);
            return conn.openDataOutputStream();
        }
		return new DataOutputStream(output);
	}

	public static OutputStream openOutputStream(String name) throws IOException
	{
		if (name.startsWith("scratchpad:"))
		{
			return new com.nttdocomo.util.ScratchPadConnection(name).openOutputStream();
		}
		if (name.startsWith("file://"))
        {
            OutputConnection conn = (OutputConnection) open(name, WRITE, false);
            return conn.openOutputStream();
        }
		return output;
	}

}
