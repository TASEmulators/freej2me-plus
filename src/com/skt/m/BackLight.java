package com.skt.m;

import org.recompile.mobile.Mobile;

public class BackLight {
    public static int getColor() {
        return Mobile.lcdMaskColors[Mobile.maskIndex];
    }

    public static int getColorNum() { return 1; }

    public static int[] getColors() { return new int[]{0xFFFFFFFF}; }

    public static void on(int timeout) {
        Mobile.maskIndex = 6;
        Mobile.getDisplay().flashBacklight(timeout);
    }

    public static void off() {
        Mobile.getDisplay().flashBacklight(0);
    }

    public static void setColor(int color) {
        Mobile.lcdMaskColors[6] = 0xFFFFFFFF;
    }
}
