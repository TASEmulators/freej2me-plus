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

import org.recompile.mobile.Mobile;

public class Connector
{

	public static final int READ = 1;
	public static final int READ_WRITE = 3;
	public static final int WRITE = 2;

	private static OutputStream output = null;
	
	public static InputStream openInputStream(String name)
	{
		if(name.startsWith("resource:")) // older Siemens phones?
		{
			return Mobile.getPlatform().loader.getMIDletResourceAsSiemensStream(name.substring(9).replaceAll("\\\\", "/"));
		}
		else
		{
			//return Mobile.getPlatform().loader.getMIDletResourceAsStream(name); // possible
			Mobile.log(Mobile.LOG_WARNING, Connector.class.getPackage().getName() + "." + Connector.class.getSimpleName() + ": " + "Faked InputStream for "+name); // just in case //
			return new fakeIS();
		}
	}


	public static DataInputStream openDataInputStream(String name)
	{
		Mobile.log(Mobile.LOG_WARNING, Connector.class.getPackage().getName() + "." + Connector.class.getSimpleName() + ": " + "Faked DataInputStream: "+name);
		return new DataInputStream(new fakeIS());
	}

	public static Connection open(String name) { return null; }

	public static Connection open(String name, int mode) { return null; }

	public static Connection open(String name, int mode, boolean timeouts) { return null; }

	public static DataOutputStream openDataOutputStream(String name) { return new DataOutputStream(output); }

	public static OutputStream openOutputStream(String name) { return output; }

	// fake inputstream 
	private static class fakeIS extends InputStream
	{
		public int avaliable() { return 0; }

		public void close() { }

		public void mark() { }

		public boolean markSupported() { return false; }

		public int read() { return 0; }

		public int read(byte[] b) { return 0; }
		
		public int read(byte[] b, int off, int len) { return 0; }

		public void reset() { }

		public long skip(long n) { return (long)0; }
	}

}
