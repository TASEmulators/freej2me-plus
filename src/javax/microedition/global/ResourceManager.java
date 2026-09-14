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
package javax.microedition.global;

import java.util.HashMap;
import java.util.Map;

public class ResourceManager
{
    public static final String DEVICE = "";

    private String baseName;
    private String locale;
    private Map<Integer, Object> resources = new HashMap<Integer, Object>();
    private boolean cachingEnabled = true;

    private ResourceManager(String baseName, String locale)
    {
        this.baseName = baseName;
        this.locale = locale;
    }

    public static ResourceManager getManager(String baseName) throws ResourceException
    {
        if (baseName == null) { throw new NullPointerException("Base name cannot be null"); }
        if (baseName.length() == 0) { return new ResourceManager(DEVICE, getDefaultLocale()); }

        return new ResourceManager(baseName, getDefaultLocale());
    }

    public static ResourceManager getManager(String baseName, String locale) throws ResourceException
    {
        if (baseName == null || locale == null) { throw new NullPointerException("Base name and locale cannot be null"); }

        return new ResourceManager(baseName, locale);
    }

    public static ResourceManager getManager(String baseName, String[] locales) throws ResourceException
    {
        if (baseName == null || locales == null || locales.length == 0)
        {
            throw new NullPointerException("Base name or locales cannot be null or empty");
        }
        for (String locale : locales)
        {
            ResourceManager manager = new ResourceManager(baseName, locale);
            if (manager.hasResources()) { return manager; }
        }
        throw new ResourceException(ResourceException.NO_RESOURCES_FOR_BASE_NAME, "No resources found");
    }

    public String getBaseName() { return baseName; }

    public String getLocale() { return locale; }

    public boolean isCaching() { return cachingEnabled; }

    public Object getResource(int id) throws ResourceException
    {
        validateResourceId(id);
        Object resource = resources.get(id);
        if (resource == null)
        {
            throw new ResourceException(ResourceException.RESOURCE_NOT_FOUND, "Resource not found for ID: " + id);
        }
        return resource;
    }

    public String getString(int id) throws ResourceException
    {
        Object resource = getResource(id);
        if (!(resource instanceof String))
        {
            throw new ResourceException(ResourceException.WRONG_RESOURCE_TYPE, "Resource is not a string");
        }
        return (String) resource;
    }

    public byte[] getData(int id) throws ResourceException
    {
        Object resource = getResource(id);
        if (!(resource instanceof byte[]))
        {
            throw new ResourceException(ResourceException.WRONG_RESOURCE_TYPE, "Resource is not binary data");
        }
        return (byte[]) resource;
    }

    public static String[] getSupportedLocales(String baseName) throws ResourceException
    {
        if (baseName == null) { throw new NullPointerException("Base name cannot be null"); }
        return Formatter.getSupportedLocales();
    }

    public boolean isValidResourceID(int id)
    {
        return id >= 0 && id <= 0x7FFFFFFF && resources.containsKey(id);
    }

    private boolean hasResources() { return !resources.isEmpty(); }

    private void validateResourceId(int id)
    {
        if (!isValidResourceID(id)) { throw new IllegalArgumentException("Invalid resource ID: " + id); }
    }

    private static String getDefaultLocale() { return System.getProperty("microedition.locale"); }
}
