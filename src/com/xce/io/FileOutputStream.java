package com.xce.io;

import java.io.IOException;
import java.io.OutputStream;

public class FileOutputStream extends OutputStream {
    private final XFile file;

    public FileOutputStream(XFile f) throws IOException {
        this.file = f;
    }

    public FileOutputStream(String name) throws IOException {
        this.file = new XFile(name, XFile.WRITE);
    }

    @Override
    public void write(int b) throws IOException {
        file.write(new byte[]{(byte) b}, 0, 1);
    }

    @Override
    public void write(byte[] b) throws IOException {
        file.write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        file.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
        file.close();
    }

    @Override
    public void flush() throws IOException {
        file.flush();
    }
}
