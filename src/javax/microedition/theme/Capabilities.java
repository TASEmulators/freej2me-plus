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

public class Capabilities
{

    public static String[] getSupportedKinds(String feature, String role)
    {
        if (feature == null || role == null) { throw new NullPointerException(); }
        if (feature.length() == 0 || role.length() == 0) { throw new IllegalArgumentException(); }

        return null;
    }

    public static String[] getSupportedMediaTypes(String feature, String role, String kind) throws ElementNotFoundException
    {
        if (feature == null || role == null || kind == null) { throw new NullPointerException(); }
        if (feature.length() == 0 || role.length() == 0 || kind.length() == 0) { throw new IllegalArgumentException(); }

        return null;
    }

    public static boolean isBaseElementSupported(String feature, String role)
    {
        if (feature == null || role == null) { throw new NullPointerException(); }
        if (feature.length() == 0 || role.length() == 0) { throw new IllegalArgumentException(); }

        return false;
    }

    public static boolean isCustomizationPolicyEnabled() { return false; }

    public static boolean isElementCustomizable(String feature, String role) throws ElementNotFoundException
    {
        if (feature == null || role == null) { throw new NullPointerException(); }
        if (feature.length() == 0 || role.length() == 0) { throw new IllegalArgumentException(); }

        return false;
    }
}
