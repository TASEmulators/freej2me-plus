package com.skt.m;

import com.xce.lcdui.XDisplay;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class Graphics2D {
    private final Graphics g;

    public Graphics2D(Graphics g) {
        this.g = g;
    }

    public static Graphics2D getGraphics2D(Graphics g) {
        return new Graphics2D(g);
    }

    public void drawImage(int tx, int ty, Image src, int sx, int sy, int sw, int sh, int mode) {
        // note: do not implement this on top of Graphics.drawRegion,
        // or it will randomly crash due to out-of-bounds

        // TODO: mode should not be ignored
        // but what does it even mean for non-mono displays?

        g.getGraphics2D().drawImage(
                src.getCanvas(),
                tx, ty, tx + sw, ty + sh,
                sx, sy, sx + sw, sy + sh,
                null
        );
    }

    public static Image captureLCD(int x, int y, int w, int h) {
        Image image = Image.createImage(w, h);
        image.getGraphics().getGraphics2D().drawImage(
                XDisplay.platformImage.getCanvas(),
                0, 0, w, h,
                x, y, x + w, x + h,
                null
        );
        return image;
    }

    public static Image createMaskableImage(int width, int height) {
        return Image.createImage(width, height);
    }

    public void setPixel(int x, int y, int color) {
        g.setPixel(x, y, color);
    }
}
