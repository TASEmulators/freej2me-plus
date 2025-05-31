package org.recompile.freej2me.imageio;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.Iterator;

public class LbmReader extends ImageReader {
    private static final ColorSpace COLORSPACE_SRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);

    private static final ImageTypeSpecifier TYPE_SRGB_ALPHA = ImageTypeSpecifier.createInterleaved(COLORSPACE_SRGB, new int[]{0, 1, 2, 3}, DataBuffer.TYPE_BYTE, true, false);

    private ImageInputStream imageInput;

    private LbmHeaderData header;

    protected LbmReader(ImageReaderSpi originatingProvider) {
        super(originatingProvider);
    }

    @Override
    public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
        super.setInput(input, seekForwardOnly, ignoreMetadata);

        header = null;
        this.imageInput = (ImageInputStream) input;
    }

    private void readHeader() throws IOException {
        if (header != null) return;

        imageInput.setByteOrder(ByteOrder.LITTLE_ENDIAN);

        byte[] magic = new byte[4];
        imageInput.readFully(magic);
        if (magic[0] != 'L' || magic[1] != 'B' || magic[2] != 'M' || magic[3] != 'P') {
            throw new IOException("Not an LBMP file");
        }

        int bitDepth = imageInput.readInt();
        int width = imageInput.readInt();
        int height = imageInput.readInt();
        int bytesPerPlane = imageInput.readInt();
        boolean enableAlpha = imageInput.readInt() != 0;

        header = new LbmHeaderData(bitDepth, width, height, bytesPerPlane, enableAlpha);
    }

    @Override
    public int getNumImages(boolean allowSearch) throws IOException {
        return 1;
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        readHeader();
        return header.width;
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        readHeader();
        return header.height;
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        readHeader();
        return Collections.singleton(TYPE_SRGB_ALPHA).iterator();
    }

    @Override
    public IIOMetadata getStreamMetadata() throws IOException {
        return null;
    }

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
        return null;
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        readHeader();

        int width = getWidth(imageIndex);
        int height = getHeight(imageIndex);

        BufferedImage destination = getDestination(param, getImageTypes(imageIndex), width, height);

        Rectangle srcRegion = new Rectangle();
        Rectangle destRegion = new Rectangle();
        computeRegions(param, width, height, destination, srcRegion, destRegion);

        WritableRaster raster = destination.getRaster().createWritableChild(destRegion.x, destRegion.y, destRegion.width, destRegion.height, 0, 0, null);

        if (header.bitDepth == 2) {
            int numScanlines = (height + 7) / 8;
            if (width * numScanlines != header.bytesPerPlane) {
                throw new IIOException("Wrong number of bytes per plane");
            }

            byte[] highPlaneData = new byte[header.bytesPerPlane];
            byte[] lowPlaneData = new byte[header.bytesPerPlane];
            byte[] alphaPlaneData = header.enableAlpha ? new byte[header.bytesPerPlane] : null;

            imageInput.readFully(highPlaneData);
            imageInput.readFully(lowPlaneData);
            if (header.enableAlpha) {
                imageInput.readFully(alphaPlaneData);
            }

            byte[] resultPixelData = new byte[destRegion.width * destRegion.height * 4];

            for (int y = 0; y < srcRegion.height; y++) {
                int srcY = y + srcRegion.y;

                for (int x = 0; x < srcRegion.width; x++) {
                    int srcX = x + srcRegion.x;

                    int byteIndex = srcX + (srcY >> 3) * width;
                    int bitIndex = srcY & 7;

                    int alpha = 0xFF;
                    if (alphaPlaneData != null) {
                        if (((alphaPlaneData[byteIndex] >> bitIndex) & 1) == 1) {
                            alpha = 0;
                        }
                    }

                    int color = ~((((highPlaneData[byteIndex] >>> bitIndex) & 1) * 0xF0)
                            | (((lowPlaneData[byteIndex] >>> bitIndex) & 1) * 0x0F));

                    resultPixelData[(x + y * destRegion.width) * 4] = (byte) color;
                    resultPixelData[(x + y * destRegion.width) * 4 + 1] = (byte) color;
                    resultPixelData[(x + y * destRegion.width) * 4 + 2] = (byte) color;
                    resultPixelData[(x + y * destRegion.width) * 4 + 3] = (byte) alpha;
                }

                if (abortRequested()) {
                    processReadAborted();
                    break;
                }
            }

            raster.setDataElements(0, 0, destRegion.width, destRegion.height, resultPixelData);
        } else {
            if (width * height != header.bytesPerPlane) {
                throw new IIOException("Wrong number of bytes per plane");
            }

            int numScanlines = (height + 7) / 8;

            byte[] rgbPlaneData = new byte[header.bytesPerPlane];
            byte[] alphaPlaneData = header.enableAlpha ? new byte[numScanlines * width] : null;

            imageInput.readFully(rgbPlaneData);
            if (header.enableAlpha) {
                imageInput.readFully(alphaPlaneData);
            }

            byte[] resultPixelData = new byte[destRegion.width * destRegion.height * 4];

            for (int y = 0; y < srcRegion.height; y++) {
                int srcY = y + srcRegion.y;

                for (int x = 0; x < srcRegion.width; x++) {
                    int srcX = x + srcRegion.x;

                    int byteIndex = srcX + srcY * width;

                    int color = rgbPlaneData[byteIndex];
                    int red = (color >>> 5) & 7;
                    int green = (color >>> 2) & 7;
                    int blue = color & 3;

                    int aByteIndex = srcX + (srcY >> 3) * width;
                    int bitIndex = srcY & 7;

                    int alpha = 0xFF;
                    if (alphaPlaneData != null) {
                        if (((alphaPlaneData[aByteIndex] >> bitIndex) & 1) == 1) {
                            alpha = 0;
                        }
                    }

                    resultPixelData[(x + y * destRegion.width) * 4] = (byte) (red * 36 + (red >>> 1));
                    resultPixelData[(x + y * destRegion.width) * 4 + 1] = (byte) (green * 36 + (green >>> 1));
                    resultPixelData[(x + y * destRegion.width) * 4 + 2] = (byte) (blue * 85);
                    resultPixelData[(x + y * destRegion.width) * 4 + 3] = (byte) alpha;
                }

                if (abortRequested()) {
                    processReadAborted();
                    break;
                }
            }

            raster.setDataElements(0, 0, destRegion.width, destRegion.height, resultPixelData);
        }

        return destination;
    }
}
