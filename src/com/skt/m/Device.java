package com.skt.m;

import org.recompile.mobile.Mobile;

public class Device {
    private static boolean backlightEnabled = true;

    public static void setBacklightEnabled(boolean flag) {
        backlightEnabled = flag;

        /*if (flag) {
            Mobile.getDisplay().flashBacklight(Integer.MAX_VALUE);
        } else {
            Mobile.getDisplay().flashBacklight(0);
        }*/
    }

    public static boolean isBacklightEnabled() {
        return backlightEnabled;
    }

    public static boolean isKeyToneEnabled() {
        return false;
    }

    public static void setKeyToneEnabled(boolean flag) {
    }

    public static void enableRestoreLCD(boolean flag) {
    }

    public static void setColorMode(int mode) {
    }

    public static void setKeyRepeatTime(int delay, int interval) {
    }

    public static void invokeWapBrowser(String url) {
        /*
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }*/
    }
}
