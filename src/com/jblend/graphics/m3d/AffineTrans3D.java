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

public class AffineTrans3D
{
public AffineTrans3D(){}
public AffineTrans3D(int[] array){}
public AffineTrans3D(int a00,int a01,int a02,int a03,int a10,int a11,int a12,int a13,int a20,int a21,int a22,int a23){}
public int[] get(){return null;}
public void set(int[] array){}
public void set(int a00,int a01,int a02,int a03,int a10,int a11,int a12,int a13,int a20,int a21,int a22,int a23){}
public void setIdentity(){}
public void multiply(AffineTrans3D t){}
public void multiply(AffineTrans3D t1,AffineTrans3D t2){}
public void rotate(Vector3D axis,int angle){}
public void rotate(Vector3D axis,Point3D pos,int angle){}
public void scale(int s){}
public void scale(int sx,int sy,int sz){}
public void translate(int tx,int ty,int tz){}
public void setRotation(Vector3D axis,int angle){}
public void setScale(int s){}
public void setScale(int sx,int sy,int sz){}
public void setTranslation(int tx, int ty,int tz){}
public Point3D transform(Point3D pos){return null;}
public void lookAt(Point3D position,Vector3D look,Vector3D up){}
}
