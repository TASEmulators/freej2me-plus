package com.xce.io;

import org.recompile.mobile.Mobile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class XFile {
    public static final int STDSTREAM = 0;
    public static final int NORMAL = 1;
    public static final int DIRECTORY = 2;
    public static final int FILE_JAR = 3;

    public static final int STDIN = 0;
    public static final int STDOUT = 1;
    public static final int STDERR = 2;

    public static final int SEEK_SET = 0;
    public static final int SEEK_CUR = 1;
    public static final int SEEK_END = 2;

    public static final int READ = 1;
    public static final int WRITE = 2;
    public static final int READ_WRITE = 3;
    public static final int READ_DIRECTORY = 4;
    public static final int READ_RESOURCE = 8;

    private InputStream inputStream;
    private RandomAccessFile raf;

    public XFile(String name, int mode) throws IOException {
        if (mode == READ_RESOURCE) {
            inputStream = Mobile.getMIDletResourceAsStream(name);
        } else {
            String modeStr;
            if (mode == READ) {
                modeStr = "r";
            } else if (mode == WRITE) {
                modeStr = "rw";
            } else if (mode == READ_WRITE) {
                modeStr = "rw";
            } else {
                throw new IllegalArgumentException("invalid XFile mode " + mode);
            }
            raf = new RandomAccessFile(convertFilePath(name), modeStr);
        }
    }

    public int available() throws IOException {
        if (raf != null) {
            return (int) (raf.length() - raf.getFilePointer());
        } else {
            return inputStream.available();
        }
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (raf != null) {
            return raf.read(b, off, len);
        } else {
            return inputStream.read(b, off, len);
        }
    }

    public int write(byte[] b, int off, int len) throws IOException {
        if (raf != null) {
            raf.write(b, off, len);
            return len;
        } else {
            throw new IOException("InputStream XFile does not support writing");
        }
    }

    public long tell() throws IOException {
        if (raf != null) {
            return raf.getFilePointer();
        } else {
            throw new IOException("InputStream XFile does not support seeking");
        }
    }

    public int seek(int n, int whence) throws IOException {
        if (raf != null) {
            if (whence == SEEK_SET) {
                raf.seek(n);
            } else if (whence == SEEK_CUR) {
                raf.seek(raf.getFilePointer() + n);
            } else if (whence == SEEK_END) {
                raf.seek(raf.length() - n);
            }
            return 0;
        } else {
            throw new IOException("InputStream XFile does not support seeking");
        }
    }

    public void flush() throws IOException {

    }

    public void close() throws IOException {
        if (raf != null) {
            raf.close();
        } else {
            inputStream.close();
        }
    }

    static File convertFilePath(String path) {
        String basePath = Mobile.XCE_DATA_PATH + Mobile.getPlatform().loader.suitename;
        boolean ignored = new File(basePath).mkdirs();
        return new File(basePath, path);
    }

    public static boolean exists(String name) throws IOException {
        return convertFilePath(name).exists();
    }

    public static int filesize(String name) throws IOException {
        return (int) convertFilePath(name).length();
    }

    public static int unlink(String name) throws IOException {
        return convertFilePath(name).delete() ? 0 : -1;
    }

    public String readdir() throws IOException {
        Mobile.log(
                Mobile.LOG_ERROR,
                XFile.class.getPackage().getName() + "." + XFile.class.getSimpleName() + ": " + "readdir() not implemented"
        );
        throw new IOException();
    }

    public static int fsused() {
        return 0;
    }

    public static int fsavail() {
        return 0x10000;
    }
}
