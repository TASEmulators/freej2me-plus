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

public class Vector3D
{
	public int x;
	public int y;
	public int z;

	private static final int UNIT_LENGTH = 4096;

	public Vector3D()
	{
		this.x = 0;
		this.y = 0;
		this.z = 0;
	}

	public Vector3D(int x, int y, int z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void normalize()
	{
		double len = java.lang.Math.sqrt((double) x * x + (double) y * y + (double) z * z);

		if (len == 0.0)
		{
			throw new ArithmeticException("Cannot normalize a zero vector");
		}

		this.x = (int) java.lang.Math.round((this.x / len) * UNIT_LENGTH);
		this.y = (int) java.lang.Math.round((this.y / len) * UNIT_LENGTH);
		this.z = (int) java.lang.Math.round((this.z / len) * UNIT_LENGTH);
	}

	public int dot(Vector3D v)
	{
		return dot(this, v);
	}

	public static int dot(Vector3D v1, Vector3D v2)
	{
		if (v1 == null || v2 == null)
		{
			throw new NullPointerException("Vector3D arguments cannot be null");
		}

		return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
	}

	public void cross(Vector3D v)
	{
		cross(this, v);
	}

	public void cross(Vector3D u, Vector3D v)
	{
		if (u == null || v == null)
		{
			throw new NullPointerException("Vector3D arguments cannot be null");
		}

		int resX = u.y * v.z - u.z * v.y;
		int resY = u.z * v.x - u.x * v.z;
		int resZ = u.x * v.y - u.y * v.x;

		this.x = resX;
		this.y = resY;
		this.z = resZ;
	}
}
