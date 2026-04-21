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

public class PrimitiveArray 
{

	private final int primitiveType;
	private final int param;
	private final int num;
    private int[] VertexArray, texCoordArray, colorArray, normalArray, spriteArray;

	public PrimitiveArray(int primitiveType, int param, int size) throws IllegalArgumentException
    {
		if(size < 0 || size > 255)
			throw new IllegalArgumentException();

		if(primitiveType < Graphics3D.PRIMITIVE_POINTS || primitiveType > Graphics3D.PRIMITIVE_POINT_SPRITES)
			throw new IllegalArgumentException();

		this.primitiveType = primitiveType;
		this.param = param;
		this.num = size;

		if(primitiveType == Graphics3D.PRIMITIVE_POINT_SPRITES)
			this.VertexArray = new int[3*size];
		else
        	this.VertexArray = new int[3*primitiveType*size];

		if ((param & Graphics3D.PDATA_NORMAL_NONE) == Graphics3D.PDATA_NORMAL_NONE)
			this.normalArray = new int[1];

		else if ((param & Graphics3D.PDATA_NORMAL_PER_FACE) == Graphics3D.PDATA_NORMAL_PER_FACE)
			this.normalArray = new int[3 * size];

		else if ((param & Graphics3D.PDATA_NORMAL_PER_VERTEX) == Graphics3D.PDATA_NORMAL_PER_VERTEX)
			this.normalArray = new int[3 * primitiveType * size];
		

		if ((param & Graphics3D.PDATA_COLOR_NONE) == Graphics3D.PDATA_COLOR_NONE)
			this.colorArray = new int[1];
		
		else if ((param & Graphics3D.PDATA_COLOR_PER_FACE) == Graphics3D.PDATA_COLOR_PER_FACE)
			this.colorArray = new int[3 * size];
		

		if ((param & Graphics3D.PDATA_TEXURE_COORD_NONE) == Graphics3D.PDATA_TEXURE_COORD_NONE)
			this.texCoordArray = new int[1];
		
		else if ((param & Graphics3D.PDATA_TEXURE_COORD) == Graphics3D.PDATA_TEXURE_COORD)
			this.texCoordArray = new int[3 * primitiveType * size];
		

		// This is just a sprite pointer array
		if(primitiveType == Graphics3D.PRIMITIVE_POINT_SPRITES)
			this.spriteArray = new int[size];
	}

	public int getType() { return primitiveType; }

	public int getParam() { return param; }

	public int size() { return num; }

	public int[] getVertexArray() { return VertexArray; }

	public int[] getColorArray() { return colorArray; }

	public int[] getNormalArray() { return normalArray; }

	public int[] getTextureCoordArray() { return texCoordArray; }

	public int[] getPointSpriteArray() { return spriteArray; }
}