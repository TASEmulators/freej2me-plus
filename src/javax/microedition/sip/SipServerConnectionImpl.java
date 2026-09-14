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
package javax.microedition.sip;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class SipServerConnectionImpl extends SipConnectionImpl implements SipServerConnection
{

    private String currentState;
    private Map<String, String> headers;
    private int statusCode;
    private String reasonPhrase;

    public SipServerConnectionImpl()
    {
        super();
        this.headers = new HashMap<String, String>();
    }

    public void initResponse(int code) throws IllegalArgumentException, SipException
    {
        if (currentState.equals("RequestReceived")) { throw new SipException(SipException.INVALID_STATE); }
        if (code < 100 || code > 699) { throw new IllegalArgumentException("Invalid status code"); }

        this.statusCode = code;
        this.reasonPhrase = "OK";

        currentState = "Initialized";
    }

    public void setReasonPhrase(String phrase) throws SipException
    {
        if (currentState != "Initialized") { throw new SipException(SipException.INVALID_STATE); }

        if (phrase == null || phrase.length() == 0) { this.reasonPhrase = ""; }
        else { this.reasonPhrase = phrase; }
    }
}
