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
package com.nttdocomo.opt.ui;

import org.recompile.mobile.Mobile;

public class TransparentImage extends com.nttdocomo.ui.Image
{
	private boolean useTransp = false;
	private int transpColor = 0x00000000;
	
	protected TransparentImage() { }

	private TransparentImage(com.nttdocomo.ui.Image image) { super(image); }

	public static TransparentImage createTransparentImage(com.nttdocomo.ui.Image image)
	{
		// Not sure we need to do anything specific here com.nttdocomo.ui.Image
		// can work with transparency already.
		return new TransparentImage(image);
	}

	public void setTransparentEnabled(boolean b) { this.useTransp = b; }

	public void setTransparentColor(int color)
	{ 
		this.transpColor = color;
		
		// DoJa >= 5.0 uses the transparent color immediately;
		if(Mobile.DoJaVersion >= 50) { setTransparentEnabled(true); }
	}

	public final com.nttdocomo.ui.Graphics getGraphics() { throw new com.nttdocomo.lang.UnsupportedOperationException("TransparentImage does not support setAlpha"); }

	public final int getTransparentColor() { throw new com.nttdocomo.lang.UnsupportedOperationException("TransparentImage does not support getTransparentColor"); }

	public final void setAlpha(int alpha) { throw new com.nttdocomo.lang.UnsupportedOperationException("TransparentImage does not support setAlpha"); }

	public final int getAlpha() { throw new com.nttdocomo.lang.UnsupportedOperationException("TransparentImage does not support getAlpha"); }
}
