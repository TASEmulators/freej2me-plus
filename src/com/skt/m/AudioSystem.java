package com.skt.m;

import com.skt.m.impl.AudioClipImpl;

import java.io.IOException;

public class AudioSystem {
    public static int getMaxVolume(String format) throws UnsupportedFormatException {
        return 100;
    }

    public static AudioClip getAudioClip(String format) throws UnsupportedFormatException {
        return new AudioClipImpl();
    }

    public static int getVolume(String format) throws UnsupportedFormatException {
        return 100;
    }

    public static void setVolume(String format, int level) throws UnsupportedFormatException {
        //
    }
}
