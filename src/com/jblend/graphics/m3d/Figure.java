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
package com.jblend.graphics.m3d;

import java.io.IOException;

public class Figure extends ObjectBase
{
	public Figure(byte[] data)
	{
	com.mascotcapsule.micro3d.v3.Figure figure = new com.mascotcapsule.micro3d.v3.Figure(data);
	}

	public Figure(String resource) throws IOException
	{
	com.mascotcapsule.micro3d.v3.Figure figure = new com.mascotcapsule.micro3d.v3.Figure(resource);
	}

	public ObjectBase duplicate(){return null;}

	public ActionTable getActionTable(){return null;}

	public int getNumPattern(){return 0;}

	public void setActionFrame(int frame){}

	public void setActionIndex(int action){}

	public void setActionTable(ActionTable table){}

	public void setPattern(int pattern){}

}
