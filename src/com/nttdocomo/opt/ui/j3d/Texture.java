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

public class Texture extends com.mascotcapsule.micro3d.v3.Texture
{
	public Texture()
	{
		super();
	}

	public Texture(byte[] b, boolean forEnv)
	{
		super(b, !forEnv);
	}

	public Texture(String name, boolean forEnv) throws IOException
	{
		super(name, !forEnv);
	}

	public Texture(InputStream is, boolean forEnv) throws IOException
	{
		super(is, !forEnv);
	}
}