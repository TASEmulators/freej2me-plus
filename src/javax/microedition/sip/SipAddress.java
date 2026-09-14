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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class SipAddress
{
    private String displayName;
    private String scheme;
    private String user;
    private String host;
    private int port = -1;
    private Map<String, String> parameters = new HashMap<String, String>();
    private boolean isSpecialWildcard = false;

    public SipAddress(String address)
    {
        if (address == null) { throw new NullPointerException("Address cannot be null"); }
        if (address.equals("*"))
        {
            isSpecialWildcard = true;
            return;
        }
        parseAddress(address);
    }

    public SipAddress(String displayName, String URI)
    {
        if (URI == null) { throw new NullPointerException("URI cannot be null"); }

        this.displayName = displayName;
        parseAddress(URI);
    }

    private void parseAddress(String address)
    {
        try
        {
            URI uri = new URI(address);
            scheme = uri.getScheme();
            user = uri.getUserInfo();
            host = uri.getHost();
            port = uri.getPort() == -1 ? (scheme.equals("sip") ? 5060 : 5061) : uri.getPort();
            String[] params = uri.getQuery() != null ? uri.getQuery().split("&") : new String[0];
            for (String param : params)
            {
                String[] keyValue = param.split("=", 2);
                if (keyValue.length == 2) { parameters.put(keyValue[0], keyValue[1]); }
                else { parameters.put(keyValue[0], ""); }
            }
        } catch (URISyntaxException e) { throw new IllegalArgumentException("Error parsing address:", e); }
    }

    public String getDisplayName() { return isSpecialWildcard ? null : displayName; }

    public String getHost() { return isSpecialWildcard ? null : host; }

    public String getParameter(String name)
    {
        if (name == null) { throw new NullPointerException("Parameter name cannot be null"); }
        return isSpecialWildcard ? null : parameters.get(name);
    }

    public String[] getParameterNames()
    {
        return isSpecialWildcard ? null : parameters.keySet().toArray(new String[0]);
    }

    public int getPort()
    {
        return isSpecialWildcard ? 0 : (port == -1 ? (scheme.equals("sip") ? 5060 : 5061) : port);
    }

    public String getScheme() { return isSpecialWildcard ? null : scheme; }

    public String getURI()
    {
        return isSpecialWildcard ? "*" : scheme + ":" + user + "@" + host + (port != -1 ? ":" + port : "");
    }

    public String getUser() { return isSpecialWildcard ? null : user; }

    public void removeParameter(String name)
    {
        if (name == null) { throw new NullPointerException("Parameter name cannot be null"); }

        if (!isSpecialWildcard) { parameters.remove(name); }
    }

    public void setDisplayName(String name)
    {
        if (isSpecialWildcard) { throw new IllegalArgumentException("Cannot modify special '*' wildcard"); }

        displayName = name;
    }

    public void setHost(String host)
    {
        if (isSpecialWildcard) { throw new IllegalArgumentException("Cannot modify special '*' wildcard"); }
        if (host == null) { throw new NullPointerException("Host cannot be null"); }

        this.host = host;
    }

    public void setParameter(String name, String value) {
        if (name == null) { throw new NullPointerException("Parameter name cannot be null"); }
        if (isSpecialWildcard) { throw new IllegalArgumentException("Cannot modify special '*' wildcard"); }

        parameters.put(name, value);
    }

    public void setPort(int port)
    {
        if (isSpecialWildcard) { throw new IllegalArgumentException("Cannot modify special '*' wildcard"); }
        if (port < 0 || port > 65535) { throw new IllegalArgumentException("Port must be between 0 and 65535"); }

        this.port = port;
    }

    public void setScheme(String scheme)
    {
        if (isSpecialWildcard) { throw new IllegalArgumentException("Cannot modify special '*' wildcard"); }
        if (scheme == null) { throw new NullPointerException("Scheme cannot be null"); }

        this.scheme = scheme;
    }

    public void setURI(String URI)
    {
        if (URI == null) { throw new NullPointerException("URI cannot be null"); }
        if (isSpecialWildcard) { throw new IllegalArgumentException("Cannot modify special '*' wildcard"); }

        parseAddress(URI);
    }

    public void setUser(String user)
    {
        if (isSpecialWildcard) { throw new IllegalArgumentException("Cannot modify special '*' wildcard"); }

        this.user = user;
    }

    @Override
    public String toString()
    {
        if (isSpecialWildcard) { return "*"; }

        StringBuilder sb = new StringBuilder();

        if (displayName != null && displayName.length() != 0) { sb.append(displayName).append(" <"); }
        sb.append(getURI());

        if (displayName != null && displayName.length() != 0) { sb.append(">"); }

        return sb.toString();
    }
}
