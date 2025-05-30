package com.xce.io;

import java.io.IOException;
import java.io.OutputStream;

public class FileOutputStream extends OutputStream {
    private final java.io.FileOutputStream stream;

    public FileOutputStream(String name) throws IOException {
        // System.out.println("open for writing " + name);

        this.stream = new java.io.FileOutputStream(XFile.convertFilePath(name));
    }

    @Override
    public void write(int b) throws IOException {
        stream.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        stream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        stream.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

    @Override
    public void flush() throws IOException {
        stream.flush();
    }
}
