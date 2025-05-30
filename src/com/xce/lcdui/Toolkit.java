package com.xce.lcdui;

import org.recompile.mobile.PlatformImage;

import javax.microedition.lcdui.Graphics;

public class Toolkit {
    public static final int FONT_HEIGHT = 12;

    // this is final according to XCE's docs,
    // but we make it non-final, so we can set it externally
    public static /* final */ Graphics graphics;
}
