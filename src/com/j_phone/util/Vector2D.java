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
package com.j_phone.util;

public class Vector2D
{
	private FixedPoint x;
	private FixedPoint y;

	public Vector2D()
	{
		this.x = new FixedPoint(0);
		this.y = new FixedPoint(0);
	}

	public Vector2D(FixedPoint x, FixedPoint y)
	{
		this.x = x;
		this.y = y;
	}

	public Vector2D(int x, int y)
	{
		this.x = new FixedPoint(x);
		this.y = new FixedPoint(y);
	}

	public void add(int x, int y)
	{
		this.x = this.x.add(x);
		this.y = this.y.add(y);
	}

	public void add(Vector2D vector)
	{
		this.x = this.x.add(vector.x);
		this.y = this.y.add(vector.y);
	}

	public Vector2D clone() { return new Vector2D(this.x, this.y); }

	public FixedPoint getX() { return x; }

	public FixedPoint getY() { return y; }

	public static FixedPoint innerProduct(Vector2D v1, Vector2D v2)
	{
		FixedPoint productX = v1.getX().multiply(v2.getX());
		FixedPoint productY = v1.getY().multiply(v2.getY());
		return productX.add(productY);
	}

	public void normalize()
	{
		FixedPoint magnitude = this.sqrt();
		if (!magnitude.isInfinite() && magnitude.getInteger() != 0)
		{
			this.x = this.x.divide(magnitude);
			this.y = this.y.divide(magnitude);
		}
	}


	public static FixedPoint outerProduct(Vector2D v1, Vector2D v2)
	{
		return v1.getX().multiply(v2.getY()).subtract(v1.getY().multiply(v2.getX()));
	}

	public void setValue(FixedPoint x, FixedPoint y)
	{
		this.x = x;
		this.y = y;
	}

	public void setValue(int x, int y)
	{
		this.x = new FixedPoint(x);
		this.y = new FixedPoint(y);
	}

	public void subtract(int x, int y)
	{
		this.x = this.x.subtract(x);
		this.y = this.y.subtract(y);
	}

	public void subtract(Vector2D vector)
	{
		this.x = this.x.subtract(vector.x);
		this.y = this.y.subtract(vector.y);
	}

	public FixedPoint sqrt()
	{
		FixedPoint squaredMagnitude = this.x.multiply(this.x).add(this.y.multiply(this.y));
		return squaredMagnitude.sqrt();
	}
}
