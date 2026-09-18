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

public class FixedPoint
{
	private int value;

	public FixedPoint() { this.value = 0; }

	public FixedPoint(int value) { this.value = value; }

	public void setValue(int value) { this.value = value; }

	public int getValue() { return this.value; }

	public int getInteger() { return this.value / 65536; }

	public int getDecimal() { return this.value % 65536; }


	public FixedPoint add(FixedPoint n) { return add(n.value); }

	public FixedPoint add(int n)
	{
		long res = (long) this.value + (long) n;
		this.value = (int) res;
		return this;
	}

	public FixedPoint subtract(FixedPoint n) { return subtract(n.value); }

	public FixedPoint subtract(int n)
	{
		long res = (long) this.value - (long) n;
		this.value = (int) res;
		return this;
	}

	public FixedPoint multiply(FixedPoint n) { return multiply(n.value); }

	public FixedPoint multiply(int n)
	{
		long prod = ((long) this.value * (long) n) >> 16;
		this.value = (int) prod;
		return this;
	}

	public FixedPoint divide(FixedPoint n) { return divide(n.value); }

	public FixedPoint divide(int n)
	{
		if (n == 0) { throw new ArithmeticException("FixedPoint division by zero"); }
		long div = (((long) this.value) << 16) / n;
		this.value = (int) div;
		return this;
	}

	public FixedPoint pow()
	{
		long prod = ((long) this.value * (long) this.value) >> 16;
		this.value = (int) prod;
		return this;
	}

	public FixedPoint inverse()
	{
		if (this.value == 0) { throw new ArithmeticException("inverse division by zero"); }
		long inv = (1L << 32) / (long) this.value;
		this.value = (int) inv;
		return this;
	}


	public FixedPoint sin(FixedPoint r)
	{
		double rad = r.value / 65536.0;
		this.value = (int) Math.round(Math.sin(rad) * 65536.0);
		return this;
	}

	public FixedPoint cos(FixedPoint r)
	{
		double rad = r.value / 65536.0;
		this.value = (int) Math.round(Math.cos(rad) * 65536.0);
		return this;
	}

	public FixedPoint tan(FixedPoint r)
	{
		double rad = r.value / 65536.0;
		this.value = (int) Math.round(Math.tan(rad) * 65536.0);
		return this;
	}

	public FixedPoint asin(FixedPoint v)
	{
		double d = v.value / 65536.0;
		if (d < -1.0 || d > 1.0)
		{
			throw new ArithmeticException("asin domain error: " + d);
		}
		this.value = (int) Math.round(Math.asin(d) * 65536.0);
		return this;
	}

	public FixedPoint acos(FixedPoint v)
	{
		double d = v.value / 65536.0;
		if (d < -1.0 || d > 1.0) { throw new ArithmeticException("acos domain error: " + d); }
		this.value = (int) Math.round(Math.acos(d) * 65536.0);
		return this;
	}

	public FixedPoint atan(FixedPoint v)
	{
		double d = v.value / 65536.0;
		this.value = (int) Math.round(Math.atan(d) * 65536.0);
		return this;
	}

	public FixedPoint sqrt()
	{
		if (this.value < 0) { throw new ArithmeticException("sqrt negative value"); }
		double d = this.value / 65536.0;
		this.value = (int) Math.round(Math.sqrt(d) * 65536.0);
		return this;
	}


	public boolean isInfinite() { return this.value == Integer.MAX_VALUE || this.value == Integer.MIN_VALUE; }

	public FixedPoint clone() { return new FixedPoint(this.value); }

	public double toDouble() { return this.value / 65536.0; }

	public static FixedPoint getPI() { return new FixedPoint((int) Math.round(Math.PI * 65536.0)); }

	public static FixedPoint getMaximum() { return new FixedPoint(Integer.MAX_VALUE); }

	public static FixedPoint getMinimum() { return new FixedPoint(Integer.MIN_VALUE); }
}
