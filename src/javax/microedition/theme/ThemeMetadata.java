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

import java.util.Date;
import java.util.Hashtable;

public final class ThemeMetadata {

    private final Hashtable<String, String> titles;
    private final Hashtable<String, String> descriptions;
    private final String defaultLocale;
    private final String author;
    private final Date created;
    private final Date modified;
    private final MediaObject icon;

    public ThemeMetadata(Hashtable<String, String> titles, Hashtable<String, String> descriptions,
                         String defaultLocale, String author, Date created, Date modified, MediaObject icon)
    {
        if (titles == null || descriptions == null || defaultLocale == null || author == null ||
            created == null || modified == null || titles.isEmpty() || descriptions.isEmpty() ||
            !titles.containsKey(defaultLocale))
        {
            throw new IllegalArgumentException("Invalid parameters for ThemeMetadata");
        }

        this.titles = titles;
        this.descriptions = descriptions;
        this.defaultLocale = defaultLocale;
        this.author = author;
        this.created = created;
        this.modified = modified;
        this.icon = icon;
    }

    public String getAuthor() { return author; }

    public Date getCreated() { return created; }

    public String getDefaultLocale() { return defaultLocale; }

    public String getDescription(String locale)
    {
        if (locale == null) { throw new NullPointerException("Locale cannot be null"); }

        String desc = (String) descriptions.get(locale);
        return (desc != null) ? desc : (String) descriptions.get(defaultLocale);
    }

    public MediaObject getIcon() { return icon; }

    public Date getModified() { return modified; }

    public String getTitle(String locale)
    {
        if (locale == null) { throw new NullPointerException("Locale cannot be null"); }

        String title = (String) titles.get(locale);
        return (title != null) ? title : (String) titles.get(defaultLocale);
    }
}
