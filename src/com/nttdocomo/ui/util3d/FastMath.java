
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
package com.nttdocomo.ui.util3d;

// TODO: This is supposed to be "fast", but i don't think we really need to make low-level optimizations since modern devices are MUCH faster
public class FastMath 
{

    public static float abs(float a) 
    {
        if (Float.isNaN(a) || Float.isInfinite(a)) { throw new IllegalArgumentException("Value must be finite."); }
        return a < 0 ? -a : a;
    }

    public static float add(float x, float y) 
    {
        if (Float.isNaN(x) || Float.isNaN(y)) { throw new IllegalArgumentException("Values must be finite."); }
        return x + y;
    }

    public static float sub(float x, float y) { return x - y; }

    public static float mul(float x, float y) 
    {
        if (Float.isNaN(x) || Float.isNaN(y)) { throw new IllegalArgumentException("Values must be finite."); }
        return x * y;
    }

    public static float div(float x, float y) 
    {
        if (Float.isNaN(x) || Float.isNaN(y)) { throw new IllegalArgumentException("Values must be finite."); }
        if (y == 0) { throw new ArithmeticException("Division by zero."); }
        return x / y;
    }

    public static float sqrt(float x) 
    {
        if (Float.isNaN(x) || x < 0) { throw new IllegalArgumentException("Value must be non-negative."); }
        return (float) Math.sqrt(x);
    }

    public static float sin(float a) 
    {
        if (Float.isNaN(a)) { throw new IllegalArgumentException("Value must be finite."); }
        return (float) Math.sin(Math.toRadians(a));
    }

    public static float cos(float a) 
    {
        if (Float.isNaN(a)) { throw new IllegalArgumentException("Value must be finite."); }
        return (float) Math.cos(Math.toRadians(a));
    }

    public static float tan(float a) 
    {
        if (Float.isNaN(a)) { throw new IllegalArgumentException("Value must be finite."); }
        return (float) Math.tan(Math.toRadians(a));
    }

    public static float asin(float a) 
    {
        if (Float.isNaN(a) || a < -1 || a > 1) { throw new IllegalArgumentException("Value must be in the range [-1, 1]."); }
        return (float) Math.toDegrees(Math.asin(a));
    }

    public static float acos(float a) 
    {
        if (Float.isNaN(a) || a < -1 || a > 1) { throw new IllegalArgumentException("Value must be in the range [-1, 1]."); }
        return (float) Math.toDegrees(Math.acos(a));
    }

    public static float atan(float a) 
    {
        if (Float.isNaN(a)) { throw new IllegalArgumentException("Value must be finite."); }
        return (float) Math.toDegrees(Math.atan(a));
    }

    public static float atan2(float a, float b) 
    {
        if (Float.isNaN(a) || Float.isNaN(b) || Float.isInfinite(a) || Float.isInfinite(b)) { throw new IllegalArgumentException("Values must be finite."); }
        if (a == 0 && b == 0) { return Float.NaN; }
        if (a == 0) { return 90.0f; }
        if (b == 0) { return 0.0f; }

        float angle = (float) Math.toDegrees(Math.atan2(b, a));
        return angle < 0 ? angle + 180.0f : angle;
    }

    public static int floatToInnerInt(float v) 
    {
        if (Float.isNaN(v) || Float.isInfinite(v)) { throw new IllegalArgumentException("Value must be finite."); }
        return Float.floatToIntBits(v);
    }

    public static float innerIntToFloat(int v) { return Float.intBitsToFloat(v); }
}
