package com.xce.io;

import java.io.IOException;
import java.io.InputStream;

public class FileInputStream extends InputStream {
    private final XFile file;

    public FileInputStream(XFile file) {
        this.file = file;
    }

    public FileInputStream(String name) throws IOException {
        this.file = new XFile(name, XFile.READ);
    }

    @Override
    public int read() throws IOException {
        byte[] tmp = new byte[1];
        if (file.read(tmp, 0, 1) <= 0) {
            return -1;
        }
        return tmp[0] & 0xFF;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return file.read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return file.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        if (n > Integer.MAX_VALUE) {
            throw new IOException("cannot skip that far");
        }
        return file.seek((int) n, XFile.SEEK_CUR);
    }

    @Override
    public int available() throws IOException {
        return file.available();
    }

    @Override
    public void close() throws IOException {
        file.close();
    }

    private long mark = -1;

    @Override
    public void mark(int readlimit) {
        try {
            mark = file.tell();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reset() throws IOException {
        file.seek((int) mark, XFile.SEEK_SET);
        mark = -1;
    }

    @Override
    public boolean markSupported() {
        return true;
    }
}
