package com.skt.m;

import java.io.IOException;

public interface AudioClip {
    void open(byte[] data, int offset, int bufferSize)
            throws UnsupportedFormatException, ResourceAllocException;

    void close() throws IOException;

    void play() throws UserStopException, IOException;

    void loop() throws UserStopException, IOException;

    void stop() throws IOException;

    void pause() throws IOException;

    void resume() throws IOException;
}
