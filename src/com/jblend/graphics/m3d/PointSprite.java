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

public class PointSprite extends ObjectBase
{
    public static int DISABLE_PERSPECTIVE;
    public static int ENABLE_SCREEN_SIZE;
    public PointSprite(){}
	public ObjectBase duplicate(){return null;}
	public Point3D getVertex(){return null;}
    public TexCoord[] getTexCoord(){return null;}
    public int getWidth(){return 0;}
    public int getHeight(){return 0;}
    public void setSize(int width,int height){}
    public int getAngle(){return 0;}
    public void setAngle(int angle){}
    public int getParam(){return 0;}
    public void setParam(int param){}
}
