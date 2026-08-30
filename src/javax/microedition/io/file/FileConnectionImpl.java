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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.Vector;

import org.recompile.mobile.Mobile;

public class FileConnectionImpl implements FileConnection
{
	private static final String BASE_DIR = ".";

	private String url;
	private boolean open;
	private File localFile;

	public FileConnectionImpl(String url, int mode) throws IOException
	{
		if (url == null) { throw new IllegalArgumentException("URL cannot be null"); }

		this.url = url;
		this.open = true;
		this.localFile = resolveLocalFile(url);

		Mobile.log(Mobile.LOG_DEBUG, FileConnectionImpl.class.getPackage().getName() +
			"." + FileConnectionImpl.class.getSimpleName() + ": " +
			"FileConnectionImpl: Opened " + url + " -> " + localFile.getAbsolutePath());
	}

	private File resolveLocalFile(String url)
	{
		String path = url;

		if (path.startsWith("file://")) path = path.substring(7);
		if (path.startsWith("localhost/")) path = path.substring(10);
		else if (path.startsWith("/")) path = path.substring(1);

		try { path = URLDecoder.decode(path, "UTF-8"); }
		catch (Exception e) { }

		return new File(BASE_DIR, path);
	}

	public boolean isOpen() { return open; }
	public void close() { this.open = false; }
	public boolean exists() { return localFile.exists(); }
	public boolean isDirectory() { return localFile.isDirectory() || url.endsWith("/"); }
	public boolean canRead() { return localFile.canRead(); }
	public boolean canWrite() { return localFile.canWrite(); }
	public boolean isHidden() { return localFile.isHidden(); }
	public long lastModified() { return localFile.lastModified(); }
	public String getURL() { return url; }

	public long fileSize() throws IOException
	{
		if (isDirectory()) { return -1; }
		if (!localFile.exists()) { throw new IOException("File does not exist"); }
		return localFile.length();
	}

	public String getName()
	{
		String name = localFile.getName();
		if (isDirectory() && !name.endsWith("/")) name += "/";
		return name;
	}

	public String getPath()
	{
		String fullPath = localFile.getPath().replace('\\', '/');
		String relative = "/" + fullPath.replace("./appdb/filesystem/", "");

		int lastSlash = relative.lastIndexOf('/');
		return (lastSlash != -1) ? relative.substring(0, lastSlash + 1) : "/";
	}

	public void create() throws IOException
	{
		if (localFile.exists()) { throw new IOException("File already exists"); }
		File parent = localFile.getParentFile();
		if (parent != null && !parent.exists()) parent.mkdirs();
		if (!localFile.createNewFile()) { throw new IOException("Failed to create file"); }
	}

	public void mkdir() throws IOException
	{
		if (localFile.exists())  { throw new IOException("Directory already exists"); }
		if (!localFile.mkdirs()) { throw new IOException("Failed to create directory"); }
	}

	public void delete() throws IOException
	{
		if (!localFile.exists()) { throw new IOException("File or directory does not exist"); }
		if (!localFile.delete()) { throw new IOException("Failed to delete file or directory"); }
	}

	public void rename(String newName) throws IOException
	{
		if (newName == null) { throw new NullPointerException("New name cannot be null"); }
		if (newName.indexOf('/') != -1 || newName.indexOf('\\') != -1)
		{
			throw new IllegalArgumentException("New name cannot contain path separators");
		}

		File target = new File(localFile.getParentFile(), newName);
		if (target.exists()) { throw new IOException("Target file already exists"); }

		if (localFile.renameTo(target))
		{
			this.localFile = target;
			int lastSlash = url.lastIndexOf('/');
			if (lastSlash != -1) this.url = url.substring(0, lastSlash + 1) + newName;
		}
		else
		{
			throw new IOException("Rename failed");
		}
	}

	public void setFileConnection(String fileName) throws IOException
	{
		if (fileName == null) { throw new NullPointerException("File name cannot be null"); }

		if (fileName.equals(".."))
		{
			File parent = localFile.getParentFile();
			if (parent == null) { throw new IOException("Already at root directory"); }
			localFile = parent;
			int secondLastSlash = url.substring(0, url.length() - 1).lastIndexOf('/');
			if (secondLastSlash != -1) url = url.substring(0, secondLastSlash + 1);
		}
		else
		{
			localFile = new File(localFile, fileName);
			if (!url.endsWith("/")) url += "/";
			url += fileName;
		}
	}

	public InputStream openInputStream()
	{
		try
		{
			return new FileInputStream(localFile);
		}
		catch (IOException e)
		{
			return null;
		}
	}

	public DataInputStream openDataInputStream()
	{
		InputStream in = openInputStream();
		return (in != null) ? new DataInputStream(in) : null;
	}

	public OutputStream openOutputStream()
	{
		return openOutputStream(0);
	}

	public OutputStream openOutputStream(long byteOffset)
	{
		try
		{
			if (!localFile.exists()) create();

			FileOutputStream fos = new FileOutputStream(localFile, byteOffset > 0);
			if (byteOffset > 0)
			{
				RandomAccessFile raf = new RandomAccessFile(localFile, "rw");
				raf.seek(byteOffset);
				raf.close();
			}
			return fos;
		}
		catch (IOException e)
		{
			return null;
		}
	}

	public DataOutputStream openDataOutputStream()
	{
		OutputStream out = openOutputStream();
		return (out != null) ? new DataOutputStream(out) : null;
	}

	public void truncate(long byteOffset) throws IOException
	{
		if (isDirectory()) { throw new IOException("Cannot truncate a directory"); }
		if (byteOffset < 0 || byteOffset > localFile.length()) { throw new IllegalArgumentException("Invalid truncate offset"); }

		RandomAccessFile raf = new RandomAccessFile(localFile, "rw");
		try
		{
			raf.setLength(byteOffset);
		}
		finally
		{
			raf.close();
		}
	}

	public Enumeration list() throws IOException { return list("*", false); }

	public Enumeration list(String filter, boolean includeHidden) throws IOException
	{
		if (!isDirectory()) { throw new IOException("Cannot list non-directory"); }
		if (!localFile.exists()) { throw new IOException("Directory does not exist"); }

		File[] files = localFile.listFiles();
		Vector<String> results = new Vector<String>();

		if (files != null)
		{
			String regexFilter = (filter == null || filter.equals("*")) ? null : filter.replace("*", ".*");

			for (int i = 0; i < files.length; i++)
			{
				File f = files[i];
				if (!includeHidden && f.isHidden()) continue;

				String name = f.getName();
				if (f.isDirectory() && !name.endsWith("/")) name += "/";

				if (regexFilter == null || name.matches(regexFilter))
				{
					results.addElement(name);
				}
			}
		}

		return results.elements();
	}

	public long directorySize(boolean includeSubDirs) throws IOException
	{
		if (!isDirectory()) { throw new IOException("Not a directory"); }
		return calculateDirSize(localFile, includeSubDirs);
	}

	private long calculateDirSize(File dir, boolean recursive)
	{
		long size = 0;
		File[] files = dir.listFiles();
		if (files != null)
		{
			for (int i = 0; i < files.length; i++)
			{
				if (files[i].isFile()) size += files[i].length();
				else if (recursive && files[i].isDirectory()) size += calculateDirSize(files[i], true);
			}
		}
		return size;
	}

	public long availableSize() { return localFile.getFreeSpace(); }
	public long totalSize() { return localFile.getTotalSpace(); }
	public long usedSize() { return localFile.getTotalSpace() - localFile.getFreeSpace(); }

	public void setHidden(boolean hidden) throws IOException {}
	public void setReadable(boolean readable) throws IOException { localFile.setReadable(readable); }
	public void setWritable(boolean writable) throws IOException { localFile.setWritable(writable); }
}
