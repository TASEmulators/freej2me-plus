package com.skt.m.impl;

import com.skt.m.AudioClip;
import com.skt.m.ResourceAllocException;
import com.skt.m.UnsupportedFormatException;
import com.skt.m.UserStopException;

import java.io.IOException;

public class AudioClipImpl implements AudioClip {
    @Override
    public void open(byte[] data, int offset, int bufferSize) throws UnsupportedFormatException, ResourceAllocException {

    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void play() throws UserStopException, IOException {

    }

    @Override
    public void loop() throws UserStopException, IOException {

    }

    @Override
    public void stop() throws IOException {

    }

    @Override
    public void pause() throws IOException {

    }

    @Override
    public void resume() throws IOException {

    }
}
