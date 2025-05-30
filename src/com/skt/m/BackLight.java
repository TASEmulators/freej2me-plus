package com.skt.m;

import org.recompile.mobile.Mobile;

public class BackLight {
    public static void on(int timeout) { Mobile.getDisplay().flashBacklight(Integer.MAX_VALUE); }

    public static void off() { Mobile.getDisplay().flashBacklight(0); }
}
