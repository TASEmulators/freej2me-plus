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
package com.nec.graphics;

import javax.microedition.lcdui.Canvas;

public abstract class NxCanvas extends Canvas
{
	// These map directly to Nokia keys so no extra code is really needed.
	public static final int NX_KEY_UP = -1;
	public static final int NX_KEY_DOWN = -2;
	public static final int NX_KEY_LEFT = -3;
	public static final int NX_KEY_RIGHT = -4;
	public static final int NX_KEY_FIRE = -5;
}
