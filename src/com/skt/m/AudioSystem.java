package com.skt.m;

import java.io.IOException;

public class AudioSystem {
    public static int getMaxVolume(String format) throws UnsupportedFormatException {
        return 100;
    }

    public static AudioClip getAudioClip(String format) throws UnsupportedFormatException {
        return new AudioClip() {
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
        };
    }

    public static int getVolume(String format) throws UnsupportedFormatException {
        return 100;
    }

    public static void setVolume(String format, int level) throws UnsupportedFormatException {
        //
    }
}
