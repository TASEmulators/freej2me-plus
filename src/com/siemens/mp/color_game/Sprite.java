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

// ***************************************************************************
//  This is just a copy of javax.microedition.lcdui.game.Sprite
//  This is because com.siemens.mp.color_game.Sprite needs to
//  extend com.siemens.mp.color_game.Layer
// ***************************************************************************

package com.siemens.mp.color_game;

import java.util.Vector;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class Sprite	extends	javax.microedition.lcdui.game.Sprite 
{ 
	public Sprite(Image image) { super(image); }

	public Sprite(Image image, int frameWidth, int frameHeight) { super(image, frameWidth, frameHeight); }

	public Sprite(Sprite s) { super(s); }
}
