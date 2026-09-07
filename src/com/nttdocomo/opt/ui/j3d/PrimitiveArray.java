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
	private final int size;
	private int[] vertexArray, texCoordArray, colorArray, normalArray, spriteArray;

	public PrimitiveArray(int primitiveType, int param, int size) throws IllegalArgumentException
	{
		if(size <= 0 || size > 255) throw new IllegalArgumentException("Invalid primitive count.");

		this.primitiveType = primitiveType;
		this.param = param;
		this.size = size;

		int verticesPerPrimitive;
		switch (primitiveType)
		{
			case Graphics3D.PRIMITIVE_POINTS:
			case Graphics3D.PRIMITIVE_POINT_SPRITES:
				verticesPerPrimitive = 1;
				break;
			case Graphics3D.PRIMITIVE_LINES:
				verticesPerPrimitive = 2;
				break;
			case Graphics3D.PRIMITIVE_TRIANGLES:
				verticesPerPrimitive = 3;
				break;
			case Graphics3D.PRIMITIVE_QUADS:
				verticesPerPrimitive = 4;
				break;
			default:
				throw new IllegalArgumentException("Invalid primitive type: " + primitiveType);
		}

		this.vertexArray = new int[size * verticesPerPrimitive * 3];

		boolean hasColor = (param & (Graphics3D.COLOR_PER_COMMAND | Graphics3D.COLOR_PER_FACE)) != 0;
		boolean hasTexCoord = (param & Graphics3D.TEXTURE_COORD_PER_VERTEX) != 0;
		if (hasColor && hasTexCoord)
			{ throw new IllegalArgumentException("Cannot combine Color and Texture Coordinates."); }

		 if (primitiveType != Graphics3D.PRIMITIVE_POINT_SPRITES)
		 {
			if ((param & Graphics3D.NORMAL_PER_FACE) != 0) { this.normalArray = new int[size * 3]; }
			else if ((param & Graphics3D.NORMAL_PER_VERTEX) != 0)
				{ this.normalArray = new int[size * verticesPerPrimitive * 3]; }

			if ((param & Graphics3D.COLOR_PER_COMMAND) != 0) { this.colorArray = new int[1]; }
			else if ((param & Graphics3D.COLOR_PER_FACE) != 0) { this.colorArray = new int[size]; }

			if (hasTexCoord)
				{ this.texCoordArray = new int[size * verticesPerPrimitive * 2]; }
		}
		else
		{
			if ((param & Graphics3D.POINT_SPRITE_PER_COMMAND) != 0) { this.spriteArray = new int[8]; }
			else if ((param & Graphics3D.POINT_SPRITE_PER_VERTEX) != 0) { this.spriteArray = new int[size * 8]; }
		}
	}

	public int getType() { return primitiveType; }

	public int getParam() { return param; }

	public int size() { return size; }

	public int[] getVertexArray() { return vertexArray; }

	public int[] getColorArray() { return colorArray; }

	public int[] getNormalArray() { return normalArray; }

	public int[] getTextureCoordArray() { return texCoordArray; }

	public int[] getPointSpriteArray() { return spriteArray; }
}
