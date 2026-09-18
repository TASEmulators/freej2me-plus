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

public class PrimitiveArray extends ObjectBase
{
public PrimitiveArray(int type, int num, int param){}
public PrimitiveArray(Primitive[] primitive){}
public ObjectBase duplicate(){return null;}
public int getType(){return 0;}
public int getParam(){return 0;}
public int numPrimitive(){return 0;}
public int[] getVertexArray(){return null;}
public int[] getNormalArray(){return null;}
public int[] getColorArray(){return null;}
public int[] getTexCoordArray(){return null;}
}


