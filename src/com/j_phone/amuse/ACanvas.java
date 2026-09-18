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
package com.j_phone.amuse;

import com.jblend.ui.SequenceInterface;
import com.vodafone.v10.graphics.sprite.SpriteCanvas;
import org.recompile.mobile.Mobile;

public abstract class ACanvas extends SpriteCanvas implements SequenceInterface
{

	public ACanvas(int numPalettes, int numPatterns, int fw, int fh)
	{
		super(numPalettes, numPatterns);
		createFrameBuffer(fw, fh);
	}
	public static int getVirtualWidth() { return Mobile.lcdWidth; }

	public static int getVirtualHeight() { return Mobile.lcdHeight; }

	public void scroll(int dx, int dy)
	{
		if (dx == 0 && dy == 0) { return; }

		int vw = getVirtualWidth();
		int vh = getVirtualHeight();

		int sx = (dx > 0) ? 0 : -dx;
		int sy = (dy > 0) ? 0 : -dy;
		int w  = vw - Math.abs(dx);
		int h  = vh - Math.abs(dy);

		if (w > 0 && h > 0)
		{
			int tx = (dx > 0) ? dx : 0;
			int ty = (dy > 0) ? dy : 0;

			copyFullScreen(dx, dy);
		}
	}

	public void flush(int tx, int ty) { drawFrameBuffer(tx, ty); }

	public static short createCharacterCommand(int offset, boolean transparent, int rotation, boolean isUpsideDown, boolean isRightsideLeft, int patternNo)
	{
		return SpriteCanvas.createCharacterCommand(offset, transparent, rotation, isUpsideDown, isRightsideLeft, patternNo);
	}

	public final void sequenceStart() { }

	public final void sequenceStop() { }
}
