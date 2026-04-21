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
package com.jblend.graphics.j3d;

import java.io.IOException;

public class Figure
{
	protected com.mascotcapsule.micro3d.v3.Figure figure;

	public Figure(byte[] b)
	{
		figure = new com.mascotcapsule.micro3d.v3.Figure(b);
	}

	public Figure(String name) throws IOException
	{
		figure = new com.mascotcapsule.micro3d.v3.Figure(name);
	}

	public final void dispose()
	{
		figure.dispose();
		figure = null;
	}

	public final void setPosture(ActionTable act, int action, int frame)
	{
		figure.setPosture((com.mascotcapsule.micro3d.v3.ActionTable) act, action, frame);
	}


	public final Texture getTexture()
	{
		return (Texture) figure.getTexture();
	}

	public final void setTexture(Texture t) { figure.setTexture((com.mascotcapsule.micro3d.v3.Texture) t); }

	public final void setTexture(Texture[] t)
	{
		com.mascotcapsule.micro3d.v3.Texture[] texs = new com.mascotcapsule.micro3d.v3.Texture[t.length];

		for(int i = 0; i < t.length; i++)
			texs[i] = (com.mascotcapsule.micro3d.v3.Texture) t[i];

		figure.setTexture(texs);
	}

	public final int getNumTextures() { return figure.getNumTextures(); }

	public final void selectTexture(int idx) { figure.selectTexture(idx); }

	public final int getNumPattern() { return figure.getNumPattern(); }

	public final void setPattern(int idx) { figure.setPattern(idx); }

	public final com.mascotcapsule.micro3d.v3.Figure getFigure() { return figure; }
}