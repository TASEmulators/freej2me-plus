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

public class ElementFactory
{

    public static Element createColorElement(String feature, String role, int argb) throws ModifyNotSupportedException
    {
        validateParameters(feature, role);
        return null;
    }

    public static Element createElementGroup(String feature, String role, Element[] initialElements)
            throws ElementException, ModifyNotSupportedException
    {
        if (initialElements == null || initialElements.length == 0) { throw new IllegalArgumentException("Initial elements cannot be empty."); }
        validateParameters(feature, role);

        for (Element element : initialElements)
        {
            if (element == null) { throw new NullPointerException("Element cannot be null."); }
            if (!element.getFeature().equals(feature)) { throw new ElementException("Feature mismatch.", feature, role); }
        }

        return null;
    }

    public static Element createFloatElement(String feature, String role, float value) throws ModifyNotSupportedException
    {
        validateParameters(feature, role);
        return null;
    }

    public static Element createFontElement(String feature, String role, String name, int style, int size)
            throws ModifyNotSupportedException
    {
        validateParameters(feature, role);
        if (name == null || name.length() == 0) { throw new IllegalArgumentException("Font name cannot be empty."); }
        if (size < 0) { throw new IllegalArgumentException("Size must be non-negative."); }
        return null;
    }

    public static Element createIntegerElement(String feature, String role, int value) throws ModifyNotSupportedException
    {
        validateParameters(feature, role);
        return null;
    }

    public static Element createMediaElement(String feature, String role, String kind, MediaObject content)
            throws ModifyNotSupportedException
    {
        validateParameters(feature, role);
        if (content == null) { throw new NullPointerException("Media content cannot be null."); }
        if (!isValidMediaKind(kind)) { throw new IllegalArgumentException("Invalid media kind."); }
       return null;
    }

    public static Element createStringElement(String feature, String role, String value) throws ModifyNotSupportedException
    {
        validateParameters(feature, role);
        if (value == null) { throw new NullPointerException("Value cannot be null."); }
        return null;
    }

    private static void validateParameters(String feature, String role)
    {
        if (feature == null || role == null) { throw new NullPointerException("Feature and role cannot be null."); }
        if (feature.length() == 0 || role.length() == 0) { throw new IllegalArgumentException("Feature and role cannot be empty."); }
    }

    private static boolean isValidMediaKind(String kind)
    {
        return kind.equals(Element.KIND_GRAPHIC) || kind.equals(Element.KIND_SOUND) || kind.equals(Element.KIND_VIDEO);
    }
}
