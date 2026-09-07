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
package com.nttdocomo.opt.ui.j3d;

import java.io.IOException;
import java.io.InputStream;

public class Figure
{
	protected com.mascotcapsule.micro3d.v3.Figure figure;

	public Figure(byte[] b)
	{
		try { figure = new com.mascotcapsule.micro3d.v3.Figure(b); }
		catch (Exception e) { throw new RuntimeException("Invalid MBAC data", e); }
	}

	// DoJa constructor
	public Figure(InputStream is) throws IOException
	{
		byte[] tmpStream = new byte[is.available()];
		is.read(tmpStream, 0, is.available());

		try { this.figure = new com.mascotcapsule.micro3d.v3.Figure(tmpStream); }
		catch (Exception e) { throw new RuntimeException("Invalid MBAC data", e); }
	}

	public final int getNumPattern() { return figure.getNumPattern(); }

	public final int getNumTextures() { return figure.getNumTextures(); }

	public final Texture getTexture()
	{
		return (Texture) figure.getTexture();
	}

	public void setTexture(Texture texture) {
		if (texture == null) { throw new NullPointerException("Texture cannot be null"); }
		setTexture(new Texture[]{ texture });
	}

	public void setTexture(Texture[] textures)
	{
		if (textures == null) { throw new NullPointerException("Textures cannot be null"); }
		if (textures.length < figure.getNumTextures()) { throw new IllegalArgumentException("Incorrect texture length:" + textures.length + " exp:" + figure.getNumTextures()); }

		com.mascotcapsule.micro3d.v3.Texture[] mcTexs =
			new com.mascotcapsule.micro3d.v3.Texture[textures.length];

		for (int i = 0; i < textures.length; i++)
		{
			if (textures[i] == null) { throw new NullPointerException("Null texture in array."); }
			if (textures[i].isForEnv) { throw new IllegalArgumentException("Texture is for environment."); }
			mcTexs[i] = textures[i];
		}

		figure.setTexture(mcTexs);
	}

	public void setPosture(ActionTable action, int index, int frame)
	{
		if (action == null) { throw new NullPointerException(); }
		if (index < 0 || index >= action.getNumAction()) { throw new IllegalArgumentException(); }

		int max = action.getMaxFrame(index);
		int clampedFrame = java.lang.Math.max(0, java.lang.Math.min(frame, max));

		figure.setPosture(action, index, clampedFrame);
	}

	public void setPattern(int pattern)
	{
		int count = getNumPattern();
		if (count < 32 && (pattern & ~((1 << count) - 1)) != 0)
			{ throw new IllegalArgumentException("Invalid pattern."); }

		figure.setPattern(pattern);
	}

	public final void selectTexture(int idx) { figure.selectTexture(idx); }

	public final com.mascotcapsule.micro3d.v3.Figure getFigure() { return figure; }
}
