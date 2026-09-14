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

import java.util.ArrayList;
import java.util.List;

public class SkinManager
{

    private final String application;
    private Skin activeSkin;
    private final List<SkinListener> listeners = new ArrayList<SkinListener>();

    // Private constructor to enforce singleton pattern
    private SkinManager(String application) { this.application = application; }

    public static SkinManager getInstance(String application)
    {
        if (application == null || application.length() == 0 || !application.matches("^[^:]+:[^:]+:[^:]+$"))
        {
            throw new IllegalArgumentException("Invalid application format. Must be name:version:vendor.");
        }
        return new SkinManager(application);
    }

    public void activateSkin(String name) throws SkinException
    {
        for (SkinListener listener : listeners) { listener.skinActivated(activeSkin, null); }
    }

    public void addSkinListener(SkinListener listener)
    {
        if (listener != null && !listeners.contains(listener)) { listeners.add(listener); }
    }

    public void createSkin(String skinLocator, String name) throws SkinException, ModifyNotSupportedException
    {
        for (SkinListener listener : listeners) { listener.skinCreated(null); }
    }

    public Skin getActiveSkin() { return activeSkin; }

    public Skin getSkin(String name) throws SkinNotFoundException { return null; }

    public String[] getSkins() { return null; }

    public void removeSkin(String name) throws SkinException
    {
        for (SkinListener listener : listeners) { listener.skinRemoved(name, null); }
    }

    public void removeSkinListener(SkinListener listener) { listeners.remove(listener); }
}
