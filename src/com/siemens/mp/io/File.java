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
package com.siemens.mp.io;

//import java.io.File; // Can't import that here, as this class is also called "File"
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.recompile.mobile.Mobile;

public class File
{
    public static final int INSIDE_STORAGE_PATH = 1;
    public static final int OUTSIDE_STORAGE_PATH = 0;
    public static final String STORAGE_DRIVE = "a:";

    private static final Map<Integer, RandomAccessFile> openFiles = new HashMap<Integer, RandomAccessFile>();
	private static int lastFileDescriptor;

    public File() { }

    public static String buildPath(String fileName)
    {
        Mobile.log(Mobile.LOG_ERROR, File.class.getPackage().getName() + "." + File.class.getSimpleName() + ": " + "buildPath(string) not implemented");
        return fileName;
    }

    public static int checkFileName(String fileName)
    {
        return fileName.indexOf(':') == -1 ? INSIDE_STORAGE_PATH : OUTSIDE_STORAGE_PATH;
    }

    public int close(int fileDescriptor) throws IOException
    {
        RandomAccessFile closeFile = openFiles.get(fileDescriptor);
		if (closeFile != null)
        {
			openFiles.remove(fileDescriptor);
			closeFile.close();
			return fileDescriptor;
		}
        Mobile.log(Mobile.LOG_ERROR, File.class.getPackage().getName() + "." + File.class.getSimpleName() + ": " + "could not close fileDescriptor " + fileDescriptor);
		return -1;
    }

    public static int copy(String source, String dest) throws IOException
    {
        java.io.File sourceFile = findFile(source);
		java.io.File destFile = findFile(dest);
		FileInputStream fis = new FileInputStream(sourceFile);
		try
        {
			FileChannel sourceChannel = fis.getChannel();
			try
            {
				FileOutputStream fos = new FileOutputStream(destFile);
				try
                {
					FileChannel destChannel = fos.getChannel();
					try { destChannel.transferFrom(sourceChannel, 0, sourceChannel.size()); }
                    finally { destChannel.close(); }
				}
                finally { fos.close(); }
			}
            finally { sourceChannel.close(); }
		}
        finally { fis.close(); }

        Mobile.log(Mobile.LOG_ERROR, File.class.getPackage().getName() + "." + File.class.getSimpleName() + ": " + "could not copy data from " + source + " to " + dest);
		return 1;
    }

    public static int debugWrite(String fileName, String infoString) throws IOException
    {
        try
        {
            FileWriter writer = new FileWriter(fileName, true);
            writer.write(infoString + "\n");
            writer.flush();
            writer.close();
            return 1;
        }
        catch (IOException e) { throw new IOException("Failed to write to the file: " + e.getMessage()); }
    }

    public static int delete(String fileName) throws IOException { return findFile(fileName).delete() ? 1 : -1; }

	public static int exists(String fileName) throws IOException { return findFile(fileName).exists() ? 1 : -1; }

    public static boolean isDirectory(String pathName) throws IOException { return findFile(pathName).isDirectory(); }

    public int seek(int fileDescriptor, int seekpos) throws IOException
    {
        RandomAccessFile file = openFiles.get(fileDescriptor);
		if (file == null) { return -1; }

		file.seek(seekpos);
		return (int) file.getFilePointer();
    }

    public int length(int fileDescriptor) throws IOException
    {
        RandomAccessFile file = openFiles.get(fileDescriptor);
		if (file == null) { return -1; }

		return (int) file.length();
    }

    public static String[] list(String pathName) throws IOException
    {
		String[] files = findFile(pathName).list();
		if (files == null) { return new String[0]; }

		Arrays.sort(files);
		return files;
    }

    public int open(String fileName) throws IOException
    {
        Mobile.log(Mobile.LOG_DEBUG, File.class.getPackage().getName() + "." + File.class.getSimpleName() + ": " + "Opening:" + fileName);
        java.io.File file = findFile(fileName);
		RandomAccessFile rfile = new RandomAccessFile(file, "rw");
		openFiles.put(++lastFileDescriptor, rfile);
		return lastFileDescriptor;
    }

    public int read(int fileDescriptor, byte[] buf, int offset, int numBytes) throws IOException
    {
        Mobile.log(Mobile.LOG_DEBUG, File.class.getPackage().getName() + "." + File.class.getSimpleName() + ": " + "Opening:" + fileDescriptor);
        RandomAccessFile file = openFiles.get(fileDescriptor);
		if (file == null) { return -1; }

		return file.read(buf, offset, numBytes);
    }

    public static int rename(String source, String dest) throws IOException { return findFile(source).renameTo(findFile(dest)) ? 1 : -1; }

    public static int spaceAvailable() throws IOException
    {
        Mobile.log(Mobile.LOG_WARNING, File.class.getPackage().getName() + "." + File.class.getSimpleName() + ": " + "spaceAvailable() measurements not implemented");
        return Integer.MAX_VALUE;
    }

    public static void truncate(int fileDescriptor, int size) throws IOException
    {
        RandomAccessFile file = openFiles.get(fileDescriptor);
		if (file == null) { return; }

		file.setLength(size);
    }

    public int write(int fileDescriptor, byte[] buf, int offset, int numBytes) throws IOException
    {
        RandomAccessFile file = openFiles.get(fileDescriptor);
		if (file == null) { return -1; }

		file.write(buf, offset, numBytes);
		return numBytes;
    }


    private static java.io.File findFile(String fileName) throws IOException
    {
        java.io.File file;
		int colon = fileName.indexOf(':');

		if (colon == -1) { file = new java.io.File(Mobile.SIEMENS_DATA_PATH, fileName); }
        else
        {
			fileName = fileName.substring(colon + 2);
			file = new java.io.File(Mobile.SIEMENS_DATA_PATH, fileName);
		}

        // Create siemens dir if not available yet
		file.getParentFile().mkdirs();

        return file;
	}
}
