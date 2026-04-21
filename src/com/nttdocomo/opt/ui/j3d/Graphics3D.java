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

public interface Graphics3D
{

    public static final int COMMAND_LIST_VERSION_1_0 = 0xFE000001;
	public static final int COMMAND_END = 0x80000000;
	public static final int COMMAND_NOP = 0x81000000;
	public static final int COMMAND_FLUSH = 0x82000000;
	public static final int COMMAND_ATTRIBUTE = 0x83000000;
	public static final int COMMAND_CLIP = 0x84000000;
	public static final int COMMAND_CENTER = 0x85000000;
	public static final int COMMAND_TEXTURE_INDEX = 0x86000000;
	public static final int COMMAND_AFFINE_INDEX = 0x87000000;
	public static final int COMMAND_PARALLEL_SCALE = 0x90000000;
	public static final int COMMAND_PARALLEL_SIZE = 0x91000000;
	public static final int COMMAND_PERSPECTIVE_FOV = 0x92000000;
	public static final int COMMAND_PERSPECTIVE_WH = 0x93000000;
	public static final int COMMAND_AMBIENT_LIGHT = 0xa0000000;
	public static final int COMMAND_DIRECTION_LIGHT = 0xa1000000;
	public static final int COMMAND_THRESHOLD = 0xaf000000;

	public static final int ENV_ATTR_LIGHTING = 1;
	public static final int ENV_ATTR_SPHERE_MAP = 2;
	public static final int ENV_ATTR_TOON_SHADING = 4;
	public static final int ENV_ATTR_SEMI_TRANSPARENT = 8;

	public static final int PATTR_LIGHTING = 0x01;
	public static final int PATTR_SPHERE_MAP = 0x02;
	public static final int PATTR_COLORKEY = 0x10;
	public static final int PATTR_BLEND_NORMAL = 0x00;
	public static final int PATTR_BLEND_HALF = 0x20;
	public static final int PATTR_BLEND_ADD = 0x40;
	public static final int PATTR_BLEND_SUB = 0x60;

	public static final int PDATA_NORMAL_NONE = 0x0000;
	public static final int PDATA_NORMAL_PER_FACE = 0x0200;
	public static final int PDATA_NORMAL_PER_VERTEX = 0x0300;
	public static final int PDATA_COLOR_NONE = 0x0000;
	public static final int PDATA_COLOR_PER_COMMAND = 0x0400;
	public static final int PDATA_COLOR_PER_FACE = 0x0800;
	public static final int PDATA_TEXURE_COORD_NONE = 0x0000;
	public static final int PDATA_TEXURE_COORD = 0x3000;
	public static final int PDATA_POINT_SPRITE_PARAMS_PER_CMD = 0x1000;
	public static final int PDATA_POINT_SPRITE_PARAMS_PER_FACE = 0x2000;
	public static final int PDATA_POINT_SPRITE_PARAMS_PER_VERTEX = 0x3000;

	public static final int POINT_SPRITE_LOCAL_SIZE = 0;
	public static final int POINT_SPRITE_PIXEL_SIZE = 1;
	public static final int POINT_SPRITE_PERSPECTIVE = 0;
	public static final int POINT_SPRITE_NO_PERS = 2;

    public static final int PRIMITIVE_POINTS = 1;
	public static final int PRIMITIVE_LINES = 2;
	public static final int PRIMITIVE_TRIANGLES = 3;
	public static final int PRIMITIVE_QUADS = 4;
	public static final int PRIMITIVE_POINT_SPRITES = 5;

    public void drawFigure(Figure figure);
    
    public void enableLight(boolean b);
    
    public void enableSemiTransparent(boolean b);
    
    public void enableSphereMap(boolean b);
    
    public void enableToonShader(boolean b);
    
    public void executeCommandList(int[] commandlist);
    
    public void flush();
    
    public void renderFigure(Figure figure);
    
    public void renderPrimitives(PrimitiveArray primitives, int attr);
    
    public void renderPrimitives(PrimitiveArray primitives, int offset, int length, int attr);
    
    public void setAmbientLight(int intensity);
    
    public void setClipRect3D(int x, int y, int width, int height);
    
    public void setDirectionLight(Vector3D direction, int intensity);
    
    public void setPerspective(int zNear, int zFar, int angle);
    
    public void setPerspective(int zNear, int zFar, int width, int height);
    
    public void setPrimitiveTexture(int index);
    
    public void setPrimitiveTextureArray(Texture texture);
    
    public void setPrimitiveTextureArray(Texture[] textures);
    
    public void setScreenCenter(int cx, int cy);
    
    public void setScreenScale(int sx, int sy);
    
    public void setScreenView(int width, int height);
    
    public void setSphereTexture(Texture texture);
    
    public void setToonParam(int threshold, int high, int low);
    
    public void setViewTrans(AffineTrans at);
    
    public void setViewTrans(int index);
    
    public void setViewTransArray(AffineTrans[] ats);
}