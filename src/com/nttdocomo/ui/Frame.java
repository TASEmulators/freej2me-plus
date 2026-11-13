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
package com.nttdocomo.ui;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.MobilePlatform;
import org.recompile.mobile.PlatformImage;
import org.recompile.mobile.PlatformGraphics;

public abstract class Frame
{
    public static final int SOFT_KEY_1 = 0;
    public static final int SOFT_KEY_2 = 1;

    public String[] softLabels = {"", ""};

    public PlatformImage platformImage;
	public Graphics graphics = null;
    public boolean labelVisible = true; // Default label visibility to enabled, apps can disable it if needed

	public int width = 0;
	public int height = 0;

    public Frame() 
    { 
        width = Mobile.getPlatform().lcdWidth;
		height = Mobile.getPlatform().lcdHeight;
		platformImage = MobilePlatform.getLcdBackbuffer();
		graphics = platformImage.getDoJaGraphics();
    }

    public final int getHeight()  { return height; }

    public final int getWidth() { return width; }

    public void setBackground(int color) 
    {
        if((Mobile.DoJaVersion < 40 && (color < 0x000000 || color > 0xFFFFFF)) ||
        (Mobile.DoJaVersion >= 40 && (color < Integer.MIN_VALUE || color > Integer.MAX_VALUE)))
        { throw new IllegalArgumentException("Invalid color value."); }

        ((PlatformGraphics)graphics).setDoJaClearColor(color);
    }

    public void setSoftLabel(int key, String label) 
    {
        MobilePlatform.showCommandBar();
        if (key != SOFT_KEY_1 && key != SOFT_KEY_2) { throw new IllegalArgumentException("Invalid soft key number."); }
        softLabels[key] = label;
    }

    public void setSoftLabelVisible(boolean b) { labelVisible = b; }

    public boolean isShown() { return Display.getCurrent() == this; }

}
