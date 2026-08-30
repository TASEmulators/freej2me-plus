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
package javax.microedition.io.file;

import java.util.Enumeration;
import java.util.Vector;

public class FileSystemRegistry extends Object
{

	private static final Vector<FileSystemListener> listeners = new Vector<FileSystemListener>();
	private static final String SYSTEM_ROOT = "freej2me_system/";

	// Static immutable Enumeration for the "freej2me_system/" path. If MIDlets
	// are going to have any kind of access to files through here, it'll be on
	// the directory we already created and have permissions for.
	private static final Enumeration<String> SYSTEM_ROOTS = new Enumeration<String>()
	{
		private boolean hasMore = true;

		@Override
		public boolean hasMoreElements() { return hasMore; }

		@Override
		public String nextElement()
		{
			if (!hasMore)
			{
				throw new SecurityException("No more roots available");
			}
			hasMore = false;
			return SYSTEM_ROOT;
		}
	};

	/* Returns true if the fileSystemListener was added. */
	public static boolean addFileSystemListener(FileSystemListener listener) throws SecurityException, NullPointerException
	{
		if (listener == null) { throw new NullPointerException("Listener cannot be null"); }

		synchronized (listeners)
		{
			if (!listeners.contains(listener))
			{
				listeners.addElement(listener);
				return true;
			}
		}
		return false;
	}

	public static Enumeration listRoots() { return SYSTEM_ROOTS; };

	/* Returns true if the fileSystemListener was removed. */
	public static boolean removeFileSystemListener(FileSystemListener listener) throws NullPointerException
	{
		if (listener == null) { throw new NullPointerException("Listener cannot be null"); }

		synchronized (listeners)
		{
			return listeners.removeElement(listener);
		}
	};
}
