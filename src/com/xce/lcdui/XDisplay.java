package com.xce.lcdui;

import com.skt.m.Graphics2D;
import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformImage;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class XDisplay {
    public static int width;
    public static int height2;

    public static PlatformImage platformImage;

    public static void refresh(int x, int y, int width, int height) {
        Mobile.getPlatform().limitFps();
        Mobile.getPlatform().flushGraphics(platformImage, x, y, width, height);
    }

    public static void drawImageEx(
            Graphics gfx, Image image,
            int tx, int ty,
            Image srcImage,
            int sx, int sy,
            int sw, int sh,
            int mode) {
        Graphics2D.getGraphics2D(gfx).drawImage(tx, ty, srcImage, sx, sy, sw, sh, mode);
    }
}
