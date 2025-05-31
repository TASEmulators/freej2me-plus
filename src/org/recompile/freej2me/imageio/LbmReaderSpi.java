package org.recompile.freej2me.imageio;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Locale;

public class LbmReaderSpi extends ImageReaderSpi {
    public LbmReaderSpi() {
        super(
                "FreeJ2ME",
                "1.0",
                new String[]{"LBM", "LBMP"},
                new String[]{"lbm"},
                new String[]{"image/x-xce-lbmp"},
                "org.recompile.freej2me.imageio.LbmReader",
                new Class[]{ImageInputStream.class}, // inputTypes,
                null,// writerSpiNames,
                false,// supportsStandardStreamMetadataFormat,
                null,// nativeStreamMetadataFormatName,
                null,// nativeStreamMetadataFormatClassName,
                null,// extraStreamMetadataFormatNames,
                null,// extraStreamMetadataFormatClassNames,
                false,// supportsStandardImageMetadataFormat,
                null,// nativeImageMetadataFormatName,
                null,// nativeImageMetadataFormatClassName,
                null,// extraImageMetadataFormatNames,
                null// extraImageMetadataFormatClassNames
        );
    }

    @Override
    public boolean canDecodeInput(Object source) throws IOException {
        if (!(source instanceof ImageInputStream)) return false;

        ImageInputStream stream = (ImageInputStream) source;

        try {
            stream.mark();
            byte[] header = new byte[4];
            stream.readFully(header);
            if (header[0] == 'L' && header[1] == 'B' && header[2] == 'M' && header[3] == 'P') {
                return true;
            }
        } catch (EOFException e) {
            return false;
        } finally {
            stream.reset();
        }
        return false;
    }

    @Override
    public ImageReader createReaderInstance(Object extension) throws IOException {
        return new LbmReader(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "XCE LBM format image reader";
    }
}
