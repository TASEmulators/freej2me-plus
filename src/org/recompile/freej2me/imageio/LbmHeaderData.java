package org.recompile.freej2me.imageio;

public class LbmHeaderData {
    public final int bitDepth;

    public final int width;
    public final int height;

    public final int bytesPerPlane;
    public final boolean enableAlpha;

    public LbmHeaderData(int bitDepth, int width, int height, int bytesPerPlane, boolean enableAlpha) {
        this.bitDepth = bitDepth;
        this.width = width;
        this.height = height;
        this.bytesPerPlane = bytesPerPlane;
        this.enableAlpha = enableAlpha;
    }
}
