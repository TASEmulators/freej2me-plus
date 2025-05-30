package com.xce.io;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;

public abstract class ByteToCharConverter {
    private final CharsetDecoder decoder;

    private static final byte[] emptyByteArray = new byte[0];

    protected ByteToCharConverter(CharsetDecoder decoder) {
        this.decoder = decoder;
    }

    public int convert(byte[] input, int inStart, int inLength, char[] output, int outStart, int outLength) {
        ByteBuffer bb = ByteBuffer.wrap(input, inStart, inLength);
        CharBuffer cb = CharBuffer.wrap(output, outStart, outLength);
        decoder.decode(bb, cb, false);
        return cb.position() - outStart;
    }

    public int flush(char[] output, int outStart, int outLength) {
        ByteBuffer bb = ByteBuffer.wrap(emptyByteArray);
        CharBuffer cb = CharBuffer.wrap(output, outStart, outLength);
        decoder.decode(bb, cb, true);
        decoder.flush(cb);
        decoder.reset();
        return cb.position() - outStart;
    }
}
