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

public abstract class ObjectBase
{
 public static final int BLEND_NORMAL = 0;
 public static final int BLEND_HALF = 0;
 public static final int BLEND_ADD = 0;
 public static final int BLEND_SUB = 0;
 public ObjectBase duplicate(){return null;}
 public void setLighting(boolean enable){}
 public void setSphereMap(boolean enable){}
 public void setColorKey(boolean enable){}
 public void setBlendMode(int param){}
 public void setTransform(AffineTrans3D t){}
 public void setTexture(Texture texture){}
 public void setTexture(Texture[] textures){}
 public Texture[] getTexture(){return null;}
}
