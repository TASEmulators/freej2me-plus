/*
	This file is part of FreeJ2ME.

	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	FreeJ2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/
*/
package javax.microedition.theme;

import java.io.IOException;
import java.io.InputStream;

public class MediaObject extends Object
{

    private final String mediaType;
    private final byte[] data;

    public MediaObject(String mediaType, byte[] data)
    {
        if (mediaType == null || data == null) { throw new NullPointerException("mediaType and data cannot be null"); }
        if (mediaType.length() == 0 || !isValidMediaType(mediaType)) { throw new IllegalArgumentException("Invalid media type"); }
        if (data.length == 0) { throw new IllegalArgumentException("Data array cannot be empty"); }

        this.mediaType = mediaType;
        this.data = data;
    }

    public MediaObject(String mediaType, InputStream in) throws IOException
    {
        if (mediaType == null || in == null) { throw new NullPointerException("mediaType and input stream cannot be null"); }
        if (mediaType.length() == 0) { throw new IllegalArgumentException("mediaType cannot be empty"); }

        this.mediaType = mediaType;
        this.data = new byte[in.available()];
        in.read(data, 0, in.available());
    }

    public byte[] getData() { return data; }

    public String getMediaType() { return mediaType; }

    private boolean isValidMediaType(String mediaType)
    {
        String[] parts = mediaType.split("/");
        return parts.length == 2 && parts[0].length() != 0 && parts[1].length() != 0;
    }
}
