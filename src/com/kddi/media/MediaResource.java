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
package com.kddi.media;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.recompile.mobile.Mobile;

public class MediaResource extends java.lang.Object 
{
	public static final String SMAF_YAMAHA_MA1 = "dev4anm";
    public static final String SMAF_YAMAHA_MA2 = "devmfan";
    public static final String SMAF_YAMAHA_MA3 = "devm39z";
    public static final String SMAF_YAMAHA_MA5 = "devm53z";

	private String _type;

    public MediaResource(byte[] resource, java.lang.String disposition) {
		this.initialize(resource, disposition);
	}

    public MediaResource(java.lang.String url) throws java.io.IOException {
		System.out.println(url);
		try {
			byte[] dat = Mobile.getPlatform().loader.getMIDletResourceAsByteArray(url);
			this.initialize(dat, "devm39z");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initialize(byte[] resource, String disposition) {
		this._type = disposition;
		MediaManager.putResource(this, resource);
	}

    public void dispose() {
		MediaManager.removeResource(this);
	}

    public MediaPlayerBox[] getPlayer() {
		return MediaManager.getMediaPlayerBoxes(this);
	}

    public java.lang.String getType() {
		return this._type;
	}

    public java.lang.String toString() {
		return super.toString();
	}
}