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

public class AffineTrans
{
	// Fixed-point scaling constant (Q12 format: 4096 = 1.0)
	private static final int ONE = 4096;

	public int m00, m01, m02, m03;
	public int m10, m11, m12, m13;
	public int m20, m21, m22, m23;

	public AffineTrans() { setIdentity(); }

	public AffineTrans(
			int a00, int a01, int a02, int a03,
			int a10, int a11, int a12, int a13,
			int a20, int a21, int a22, int a23)
	{
		setElement(a00, a01, a02, a03, a10, a11, a12, a13, a20, a21, a22, a23);
	}

	public void setIdentity()
	{
		m00 = ONE; m01 = 0;   m02 = 0;   m03 = 0;
		m10 = 0;   m11 = ONE; m12 = 0;   m13 = 0;
		m20 = 0;   m21 = 0;   m22 = ONE; m23 = 0;
	}

	public void setElement(
			int a00, int a01, int a02, int a03,
			int a10, int a11, int a12, int a13,
			int a20, int a21, int a22, int a23)
	{
		this.m00 = a00; this.m01 = a01; this.m02 = a02; this.m03 = a03;
		this.m10 = a10; this.m11 = a11; this.m12 = a12; this.m13 = a13;
		this.m20 = a20; this.m21 = a21; this.m22 = a22; this.m23 = a23;
	}

	public void setElement(int row, int column, int value)
	{
		if (row == 0)
		{
			if (column == 0) m00 = value;
			else if (column == 1) m01 = value;
			else if (column == 2) m02 = value;
			else if (column == 3) m03 = value;
			else throw new IllegalArgumentException("Column index out of bounds: " + column);
		}
		else if (row == 1)
		{
			if (column == 0) m10 = value;
			else if (column == 1) m11 = value;
			else if (column == 2) m12 = value;
			else if (column == 3) m13 = value;
			else throw new IllegalArgumentException("Column index out of bounds: " + column);
		}
		else if (row == 2)
		{
			if (column == 0) m20 = value;
			else if (column == 1) m21 = value;
			else if (column == 2) m22 = value;
			else if (column == 3) m23 = value;
			else throw new IllegalArgumentException("Column index out of bounds: " + column);
		}
		else
		{
			throw new IllegalArgumentException("Row index out of bounds: " + row);
		}
	}

	public void setRow(int row, int x, int y, int z, int w)
	{
		if (row == 0)      { m00 = x; m01 = y; m02 = z; m03 = w; }
		else if (row == 1) { m10 = x; m11 = y; m12 = z; m13 = w; }
		else if (row == 2) { m20 = x; m21 = y; m22 = z; m23 = w; }
		else { throw new IllegalArgumentException("Row index out of bounds: " + row); }
	}

	public void setColumn(int column, int x, int y, int z)
	{
		if (column == 0)      { m00 = x; m10 = y; m20 = z; }
		else if (column == 1) { m01 = x; m11 = y; m21 = z; }
		else if (column == 2) { m02 = x; m12 = y; m22 = z; }
		else if (column == 3) { m03 = x; m13 = y; m23 = z; }
		else { throw new IllegalArgumentException("Column index out of bounds: " + column); }
	}

	public void mul(AffineTrans t)
	{
		mul(this, t);
	}

	public void mul(AffineTrans t1, AffineTrans t2)
	{
		if (t1 == null || t2 == null) { throw new NullPointerException("AffineTrans arguments cannot be null"); }

		// 3x4 * 3x4 Matrix Multiplication in Q12 Fixed-Point
		int r00 = (int) (((long) t1.m00 * t2.m00 + (long) t1.m01 * t2.m10 + (long) t1.m02 * t2.m20) >> 12);
		int r01 = (int) (((long) t1.m00 * t2.m01 + (long) t1.m01 * t2.m11 + (long) t1.m02 * t2.m21) >> 12);
		int r02 = (int) (((long) t1.m00 * t2.m02 + (long) t1.m01 * t2.m12 + (long) t1.m02 * t2.m22) >> 12);
		int r03 = (int) (((long) t1.m00 * t2.m03 + (long) t1.m01 * t2.m13 + (long) t1.m02 * t2.m23) >> 12) + t1.m03;

		int r10 = (int) (((long) t1.m10 * t2.m00 + (long) t1.m11 * t2.m10 + (long) t1.m12 * t2.m20) >> 12);
		int r11 = (int) (((long) t1.m10 * t2.m01 + (long) t1.m11 * t2.m11 + (long) t1.m12 * t2.m21) >> 12);
		int r12 = (int) (((long) t1.m10 * t2.m02 + (long) t1.m11 * t2.m12 + (long) t1.m12 * t2.m22) >> 12);
		int r13 = (int) (((long) t1.m10 * t2.m03 + (long) t1.m11 * t2.m13 + (long) t1.m12 * t2.m23) >> 12) + t1.m13;

		int r20 = (int) (((long) t1.m20 * t2.m00 + (long) t1.m21 * t2.m10 + (long) t1.m22 * t2.m20) >> 12);
		int r21 = (int) (((long) t1.m20 * t2.m01 + (long) t1.m21 * t2.m11 + (long) t1.m22 * t2.m21) >> 12);
		int r22 = (int) (((long) t1.m20 * t2.m02 + (long) t1.m21 * t2.m12 + (long) t1.m22 * t2.m22) >> 12);
		int r23 = (int) (((long) t1.m20 * t2.m03 + (long) t1.m21 * t2.m13 + (long) t1.m22 * t2.m23) >> 12) + t1.m23;

		setElement(r00, r01, r02, r03, r10, r11, r12, r13, r20, r21, r22, r23);
	}

	public void setRotateX(int a)
	{
		double rad = (a * java.lang.Math.PI * 2.0) / 4096.0; // Angle scale: 4096 = 360 deg
		int cos = (int) (java.lang.Math.cos(rad) * ONE);
		int sin = (int) (java.lang.Math.sin(rad) * ONE);

		setIdentity();
		m11 = cos;  m12 = -sin;
		m21 = sin;  m22 = cos;
	}

	public void setRotateY(int a)
	{
		double rad = (a * java.lang.Math.PI * 2.0) / 4096.0;
		int cos = (int) (java.lang.Math.cos(rad) * ONE);
		int sin = (int) (java.lang.Math.sin(rad) * ONE);

		setIdentity();
		m00 = cos;   m02 = sin;
		m20 = -sin;  m22 = cos;
	}

	public void setRotateZ(int a)
	{
		double rad = (a * java.lang.Math.PI * 2.0) / 4096.0;
		int cos = (int) (java.lang.Math.cos(rad) * ONE);
		int sin = (int) (java.lang.Math.sin(rad) * ONE);

		setIdentity();
		m00 = cos;  m01 = -sin;
		m10 = sin;  m11 = cos;
	}

	public void setRotateV(Vector3D v, int a)
	{
		if (v == null) { throw new NullPointerException("Vector3D cannot be null"); }

		double rad = (a * java.lang.Math.PI * 2.0) / 4096.0;
		double cos = java.lang.Math.cos(rad);
		double sin = java.lang.Math.sin(rad);

		// Normalize vector
		double vx = v.x / 4096.0;
		double vy = v.y / 4096.0;
		double vz = v.z / 4096.0;
		double len = java.lang.Math.sqrt(vx * vx + vy * vy + vz * vz);
		if (len != 0) { vx /= len; vy /= len; vz /= len; }

		double omc = 1.0 - cos;

		setIdentity();
		m00 = (int) ((cos + vx * vx * omc) * ONE);
		m01 = (int) ((vx * vy * omc - vz * sin) * ONE);
		m02 = (int) ((vx * vz * omc + vy * sin) * ONE);

		m10 = (int) ((vy * vx * omc + vz * sin) * ONE);
		m11 = (int) ((cos + vy * vy * omc) * ONE);
		m12 = (int) ((vy * vz * omc - vx * sin) * ONE);

		m20 = (int) ((vz * vx * omc - vy * sin) * ONE);
		m21 = (int) ((vz * vy * omc + vx * sin) * ONE);
		m22 = (int) ((cos + vz * vz * omc) * ONE);
	}

	public void lookAt(Vector3D position, Vector3D look, Vector3D up)
	{
		if (position == null || look == null || up == null)
		{
			throw new NullPointerException("Vector arguments cannot be null");
		}

		// Forward = normalize(position - look)
		double fx = (position.x - look.x) / 4096.0;
		double fy = (position.y - look.y) / 4096.0;
		double fz = (position.z - look.z) / 4096.0;
		double flen = java.lang.Math.sqrt(fx * fx + fy * fy + fz * fz);
		if (flen != 0) { fx /= flen; fy /= flen; fz /= flen; }

		// Right = normalize(up x forward)
		double ux = up.x / 4096.0, uy = up.y / 4096.0, uz = up.z / 4096.0;
		double rx = uy * fz - uz * fy;
		double ry = uz * fx - ux * fz;
		double rz = ux * fy - uy * fx;
		double rlen = java.lang.Math.sqrt(rx * rx + ry * ry + rz * rz);
		if (rlen != 0) { rx /= rlen; ry /= rlen; rz /= rlen; }

		// Up = forward x right
		ux = fy * rz - fz * ry;
		uy = fz * rx - fx * rz;
		uz = fx * ry - fy * rx;

		m00 = (int) (rx * ONE); m01 = (int) (ry * ONE); m02 = (int) (rz * ONE);
		m03 = (int) (- (rx * position.x + ry * position.y + rz * position.z));

		m10 = (int) (ux * ONE); m11 = (int) (uy * ONE); m12 = (int) (uz * ONE);
		m13 = (int) (- (ux * position.x + uy * position.y + uz * position.z));

		m20 = (int) (fx * ONE); m21 = (int) (fy * ONE); m22 = (int) (fz * ONE);
		m23 = (int) (- (fx * position.x + fy * position.y + fz * position.z));
	}

	public void transform(Vector3D v, Vector3D result)
	{
		if (v == null || result == null) { throw new NullPointerException("Vector3D arguments cannot be null"); }

		long x = v.x;
		long y = v.y;
		long z = v.z;

		result.x = (int) (((long) m00 * x + (long) m01 * y + (long) m02 * z) >> 12) + m03;
		result.y = (int) (((long) m10 * x + (long) m11 * y + (long) m12 * z) >> 12) + m13;
		result.z = (int) (((long) m20 * x + (long) m21 * y + (long) m22 * z) >> 12) + m23;
	}
}
