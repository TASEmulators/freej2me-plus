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

public class RenderContext3D
{
public RenderContext3D(){}
public void setClipRect(int x,int y,int width,int height){}
public void setScreenCenter(int x,int y){}
public void setToonParam(int threshold,int high,int low){}
public void setSphereTexture(Texture texture){}
public void setLighting(boolean isEnabled){}
public void setSphereMap(boolean isEnabled){}
public void setToonShading(boolean isEnabled){}
public void setTransparent(boolean isEnabled){}
public boolean isLighting(){return false;}
public boolean isSphereMap(){return false;}
public boolean isToonShading(){return false;}
public boolean isTransparent(){return false;}
}
