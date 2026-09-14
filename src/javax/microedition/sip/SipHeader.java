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

import java.util.HashMap;
import java.util.Map;

public class SipHeader
{

    private String name;
    private String headerValue;
    private final Map<String, String> parameters = new HashMap<String, String>();

    public SipHeader(String name, String headerValue)
    {
        if (name == null) { throw new NullPointerException("Header name cannot be null"); }
        if (name.trim().length() == 0 || headerValue == null) { throw new IllegalArgumentException("Invalid header name or value"); }

        this.name = name.trim();
        this.headerValue = headerValue != null ? headerValue.trim() : "";
        parseHeaderValue();
    }

    private void parseHeaderValue()
    {
        int paramStart = headerValue.indexOf(';');
        if (paramStart != -1)
        {
            String valuePart = headerValue.substring(0, paramStart).trim();
            this.headerValue = valuePart;
            String[] paramPairs = headerValue.substring(paramStart + 1).split(";");
            for (String pair : paramPairs)
            {
                String[] parts = pair.split("=", 2);
                String key = parts[0].trim();
                String value = parts.length > 1 ? parts[1].trim() : "";
                parameters.put(key, value);
            }
        }
    }

    public String getHeaderValue()
    {
        return headerValue + (parameters.isEmpty() ? "" : ";" + getParametersAsString());
    }

    public String getName() { return name; }

    public String getParameter(String name) { return parameters.get(name); }

    public String[] getParameterNames() { return parameters.keySet().toArray(new String[0]); }

    public String getValue() { return headerValue; }

    public void removeParameter(String name) { parameters.remove(name); }

    public void setName(String name)
    {
        if (name == null) { throw new NullPointerException("Header name cannot be null"); }

        this.name = name.trim();
    }

    public void setParameter(String name, String value)
    {
        if (name == null) { throw new NullPointerException("Parameter name cannot be null"); }

        parameters.put(name.trim(), value != null ? value.trim() : "");
    }

    public void setValue(String value)
    {
        if (value != null && value.contains(";")) { throw new IllegalArgumentException("Header value cannot include parameters"); }

        this.headerValue = value != null ? value.trim() : "";
    }

    private String getParametersAsString()
    {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : parameters.entrySet())
        {
            sb.append(entry.getKey()).append("=");
            sb.append(entry.getValue()).append("; ");
        }
        return sb.toString().trim();
    }
}
