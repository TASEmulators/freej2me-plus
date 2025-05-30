package com.xce.io;

import java.nio.charset.Charset;

public class ByteToCharEUC_KR extends ByteToCharConverter {
    private static final Charset charset = Charset.forName("EUC_KR");

    public ByteToCharEUC_KR() {
        super(charset.newDecoder());
    }
}
