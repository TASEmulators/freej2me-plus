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
package javax.microedition.io;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import java.util.HashMap;
import java.util.Map;

import org.recompile.mobile.Mobile;

class HttpConnectionImpl implements HttpConnection, com.nttdocomo.io.HttpConnection
{

	Map<String, String> requestProperty = new HashMap<String, String>();
	private String url, requestMethod;

	public HttpConnectionImpl(String url) { this.url = url; }

	public String getURL() { return url; }

	public String getProtocol() { return url.split(":")[0]; }

	public String getHost() { return ""; }

	public String getFile() { return ""; }

	public String getRef() { return ""; }

	public String getQuery() { return ""; }

	public int getPort() { return 80; }

	public String getRequestMethod() { return requestMethod; }

	public void setRequestMethod(String method) { this.requestMethod = method; }

	public String getRequestProperty(String key) { return requestProperty.get(key); }

	public void setRequestProperty(String key, String value) { requestProperty.put(key, value); }

	public void connect() throws java.io.IOException { Mobile.log(Mobile.LOG_WARNING, HttpConnectionImpl.class.getPackage().getName() + "." + HttpConnectionImpl.class.getSimpleName() + ": " + "Http Connection requested: "+ this.url); }

	public int getResponseCode() { return 200; }

	public String getResponseMessage() { return "OK"; }

	public long getExpiration() { return 0; }

	public long getDate() { return 0; }

	public long getLastModified() { return 0; }

	public String getHeaderField(String name) { return null; }

	public int getHeaderFieldInt(String name, int def) { return 0; }

	public long getHeaderFieldDate(String name, long def) { return 0; }

	public String getHeaderField(int n) { return null; }

	public String getHeaderFieldKey(int n) { return null; }

	public void close() { Mobile.log(Mobile.LOG_WARNING, HttpConnectionImpl.class.getPackage().getName() + "." + HttpConnectionImpl.class.getSimpleName() + ": " + "'closing' http connection"); }

	public String getType() { return null; }

	public String getEncoding() { return null; }

	public long getLength() { return 0; }

	public DataInputStream openDataInputStream() throws UnsupportedEncodingException { return new DataInputStream(this.openInputStream()); }

	public InputStream openInputStream() throws UnsupportedEncodingException { return null; }

	public DataOutputStream openDataOutputStream() { return new DataOutputStream(this.openOutputStream()); }

	public OutputStream openOutputStream() { return null; }
}
