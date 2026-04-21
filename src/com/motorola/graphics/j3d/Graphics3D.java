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
package com.motorola.graphics.j3d;

public interface Graphics3D
{
	public int COMMAND_AFFINE_INDEX = -2030043136;
	public int COMMAND_AMBIENT_LIGHT = -1610612736;
	public int COMMAND_ATTRIBUTE = -2097152000;
	public int COMMAND_CENTER = -2063597568;
	public int COMMAND_CLIP = -2080374784;
	public int COMMAND_DIRECTION_LIGHT = -1593835520;
	public int COMMAND_END = -2147483648;
	public int COMMAND_FLUSH = -2113929216;
	public int COMMAND_LIST_VERSION_1_0 = -33554431;
	public int COMMAND_NOP = -2130706432;
	public int COMMAND_PARALLEL_SCALE = -1879048192;
	public int COMMAND_PARALLEL_SIZE = -1862270976;
	public int COMMAND_PERSPECTIVE_FOV = -1845493760;
	public int COMMAND_PERSPECTIVE_WH = -1828716544;
	public int COMMAND_TEXTURE_INDEX = -2046820352;
	public int COMMAND_THRESHOLD = -1358954496;
	public int ENV_ATTR_LIGHTING = 1;
	public int ENV_ATTR_SEMI_TRANSPARENT = 8;
	public int ENV_ATTR_SPHERE_MAP = 2;
	public int ENV_ATTR_TOON_SHADING = 4;
	public int PATTR_BLEND_ADD = 64;
	public int PATTR_BLEND_HALF = 32;
	public int PATTR_BLEND_NORMAL = 0;
	public int PATTR_BLEND_SUB = 96;
	public int PATTR_COLORKEY = 16;
	public int PATTR_LIGHTING = 1;
	public int PATTR_SPHERE_MAP = 2;
	public int PDATA_COLOR_NONE = 0;
	public int PDATA_COLOR_PER_COMMAND = 1024;
	public int PDATA_COLOR_PER_FACE = 2048;
	public int PDATA_NORMAL_NONE = 0;
	public int PDATA_NORMAL_PER_FACE = 512;
	public int PDATA_NORMAL_PER_VERTEX = 768;
	public int PDATA_POINT_SPRITE_PARAMS_PER_CMD = 4096;
	public int PDATA_POINT_SPRITE_PARAMS_PER_FACE = 8192;
	public int PDATA_POINT_SPRITE_PARAMS_PER_VERTEX = 12288;
	public int PDATA_TEXURE_COORD = 12288;
	public int PDATA_TEXURE_COORD_NONE = 0;
	public int POINT_SPRITE_LOCAL_SIZE = 0;
	public int POINT_SPRITE_NO_PERS = 2;
	public int POINT_SPRITE_PERSPECTIVE = 0;
	public int POINT_SPRITE_PIXEL_SIZE = 1;
	public int PRIMITIVE_LINES = 33554432;
	public int PRIMITIVE_POINT_SPRITES = 83886080;
	public int PRIMITIVE_POINTS = 16777216;
	public int PRIMITIVE_QUADS = 67108864;
	public int PRIMITIVE_TRIANGLES = 50331648;

	public void drawCommandList(Texture texture, int x, int y, FigureLayout layout, Effect3D effect,
		int[] commandlist);

	public void drawCommandList(Texture[] textures, int x, int y, FigureLayout layout, Effect3D effect,
		int[] commandlist);

	public void drawFigure(Figure figure, int x, int y, FigureLayout layout, Effect3D effect);

	public void flush();

	public void renderFigure(Figure figure, int x, int y, FigureLayout layout, Effect3D effect);

	public void renderPrimitives(Texture texture, int x, int y, FigureLayout layout, Effect3D effect,
		int command, int numPrimitives, int[] vertexCoords, int[] normals, int[] textureCoords, int[] colors);
}