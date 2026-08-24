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
package javax.microedition.m3g;

public class M3GMath
{
	static final float EPSILON = 0.0001f;

	// Faster alternatives to Java's Math library, we don't need the more robust checks.

	private static final float[] preCalcSin = new float[65536];

	static
	{
		for (int i = 0; i < 65536; ++i)
		{
			preCalcSin[i] = (float) Math.sin((float) i * Math.PI * 2.0f / 65536.0f);
		}
	}

	public static float sin(float f)
	{
		return preCalcSin[(int) (f * 10430.378F) & '\uffff'];
	}

	public static float cos(float f)
	{
		return preCalcSin[(int) (f * 10430.378F + 16384.0F) & '\uffff'];
	}

	public static float tan(float a)
	{
		final float cosine = cos(a);
		return cosine != 0.0f ? sin(a) / cosine : Float.POSITIVE_INFINITY;
	}

	// Approximation: acos(a) ~= pi/2 + (ba + ca^3) / (1 + da^2 + ea^4)
	public static float acos(float a)
	{
		float a2 = a * a;
		float a3 = a2 * a;
		float a4 = a2 * a2;
		return (float) (Math.PI / 2.0) +
			((-0.939115566f * a) + (0.921784152f * a3)) /
			(1.0f + (-1.284590624f * a2) + (0.295624144f * a4));
	}

	// Those 'to*' methods are just backported from Java 9
	public static float toRadians(float angdeg) { return angdeg * 0.017453292f; } // angdeg * (Math.PI/180.0f)

	public static float toDegrees(float angrad) { return angrad * 57.29577951f; } // angdeg * (180.0f / Math.PI)

	public static float invSqrt(float x)
	{
		float xhalf = 0.5f * x;
		int i = Float.floatToRawIntBits(x);
		i = 0x5f3759df - (i >> 1);
		x = Float.intBitsToFloat(i);
		return x * (1.5f - (xhalf * x * x));
	}

	public static float sqrt(float x)
	{
		return x * invSqrt(x);
	}

	public static float abs(float value) { return (value < 0) ? -value : value; }
	public static int abs(int value) { return (value < 0) ? -value : value; }

	public static float max(float a, float b) { return (a > b) ? a : b; }
	public static int max(int a, int b) { return a - ((a - b) & ((a - b) >> 31)); }

	public static float min(float a, float b) { return (a < b) ? a : b; }
	public static int min(int a, int b) { return b + ((a - b) & ((a - b) >> 31)); }

	public static double exp(double val)
	{
		final long tmp = (long) (1512775 * val + (1072693248 - 60801));
		return Double.longBitsToDouble(tmp << 32);
	}

	public static float exp(float val)
	{
		final int tmp = (int) (12102203 * val + 1064866805);
		return Float.intBitsToFloat(tmp);
	}

	public static int round(float value)
	{
		if (value > 0) { return (int) (value + 0.5f); }
		else { return (int) (value - 0.5f); }
	}

	public static int floor(float value)
	{
		int i = (int) value;
		return (value < i) ? i - 1 : i;
	}

	// Those are slightly faster than using round() since we know the value will always be positive or negative
	public static int roundPositive(float value) { return (int) (value + 0.5f); }

	public static int roundNegative(float value) { return (int) (value - 0.5f); }

	// Much faster atan2 approximation heavily based on https://gist.github.com/volkansalma/2972237
	public static final float atan2(float y, float x)
	{
		final float abs_y = abs(y) + 1e-10f;
		final float r = (x - Math.copySign(abs_y, x)) / (abs_y + abs(x));
		float angle = (float) (Math.PI / 2) - Math.copySign((float) (Math.PI / 4), x);

		angle += (0.1963f * r * r - 0.9817f) * r;
		return Math.copySign(angle, y); // Negate if y is negative
	}

	// Fast float reciprocal (1 / x) using two Newton-Raphson steps
	public static final float fastReciprocal(float x)
	{
		int i = Float.floatToRawIntBits(x);
		i = 0x7EF127EA - i;
		float y = Float.intBitsToFloat(i);

		float e = 1.0f - x * y;

		return y * (1.0f + e) * (1.0f + e * e);
	}

	// Now we get to stuff specific to M3G

	// Normalize a vector
	public static void normalize(float[] vector)
	{
		float lengthSq = vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2];

		if (lengthSq < EPSILON)
		{
			vector[0] = 0.0f;
			vector[1] = 0.0f;
			vector[2] = 1.0f;
			return;
		}

		float invLength = fastReciprocal(sqrt(lengthSq));
		vector[0] *= invLength;
		vector[1] *= invLength;
		vector[2] *= invLength;
	}

	public static float[] add(float[] a, float[] b)
	{
		for (int i = 0; i < a.length; i++) { a[i] += b[i]; }
		return a;
	}

	public static float[] sub(float[] a, float[] b) { return add(a, neg(b)); }

	public static float[] mul(float[] a, float b)
	{
		for (int i = 0; i < a.length; i++) { a[i] *= b; }
		return a;
	}

	public static float[] div(float[] a, float b) { return mul(a, 1f / b); }

	public static float[] neg(float[] a)
	{
		for (int i = 0; i < a.length; i++) { a[i] *= -1f; }
		return a;
	}

	public static float dotProduct(float[] a, float[] b)
	{
		float sum = 0;
		for (int i = 0; i < a.length; i++) { sum += a[i] * b[i]; }
		return sum;
	}

	public static void scaleVec(float[] vec, float s)
	{
		for (int i = 0; i < vec.length; i++) { vec[i] *= s; }
	}

	// Vector3 / float[3] helpers
	// For Vector3, the following disposition is used:
	// [0] = x
	// [1] = y
	// [2] = z
	public static void lerpVec3(int size, float[] vec, float s, float[] start, float[] end)
	{
		float sCompl = 1.f - s;
		for (int i = 0; i < size; i++) { vec[i] = (sCompl * start[i]) + (s * end[i]); }
	}


	// QVec4 / float[4] helpers
	// For QVec4, the following disposition is used:
	// [0] = x
	// [1] = y
	// [2] = z
	// [3] = w
	public static void mulQuat(float[] q1, float[] q2, float[] result)
	{
		result[0] = q1[3] * q2[0] + q1[0] * q2[3] + q1[1] * q2[2] - q1[2] * q2[1]; // x
		result[1] = q1[3] * q2[1] + q1[1] * q2[3] + q1[2] * q2[0] - q1[0] * q2[2]; // y
		result[2] = q1[3] * q2[2] + q1[2] * q2[3] + q1[0] * q2[1] - q1[1] * q2[0]; // z
		result[3] = q1[3] * q2[3] - q1[0] * q2[0] - q1[1] * q2[1] - q1[2] * q2[2]; // w
	}

	public static float[] normalizeQuat(float[] vec4)
	{
		float norm = (vec4[0] * vec4[0] + vec4[1] * vec4[1] + vec4[2] * vec4[2] + vec4[3] * vec4[3]);

		if (norm > EPSILON)
		{
			norm = (1.0f / sqrt(norm));
			scaleVec(vec4, norm);
		}
		else { return identityQuat(); }

		return vec4;
	}

	public static void slerpQuat(float[] orig, float s, float[] q0, float[] q1)
	{
		float cosTheta = q0[0]*q1[0] + q0[1]*q1[1] + q0[2]*q1[2] + q0[3]*q1[3];
		float q1x = q1[0], q1y = q1[1], q1z = q1[2], q1w = q1[3];

		if (cosTheta < 0.0f)
		{
			cosTheta = -cosTheta;
			q1x = -q1x; q1y = -q1y; q1z = -q1z; q1w = -q1w;
		}

		float s0, s1;
		float oneMinusS = 1.0f - s;

		if (cosTheta < (1.0f - EPSILON))
		{
			float theta = acos(cosTheta);
			float sinTheta = sin(theta);
			s0 = sin(oneMinusS * theta) / sinTheta;
			s1 = sin(s * theta) / sinTheta;
		}
		else
		{
			s0 = oneMinusS;
			s1 = s;
		}

		orig[0] = s0 * q0[0] + s1 * q1x;
		orig[1] = s0 * q0[1] + s1 * q1y;
		orig[2] = s0 * q0[2] + s1 * q1z;
		orig[3] = s0 * q0[3] + s1 * q1w;
	}

	public static float[] identityQuat() { return new float[] { 0.0f, 0.0f, 0.0f, 1.0f }; }

	public static float[] setQuatRotation(float[] srcAxis, float[] targetAxis)
	{
		float[] rot = new float[4];
		float[] cross = new float[3];
		float dot = srcAxis[0] * targetAxis[0] + srcAxis[1] * targetAxis[1] + srcAxis[2] * targetAxis[2];

		cross[0] = srcAxis[1] * targetAxis[2] - srcAxis[2] * targetAxis[1];
		cross[1] = srcAxis[2] * targetAxis[0] - srcAxis[0] * targetAxis[2];
		cross[2] = srcAxis[0] * targetAxis[1] - srcAxis[1] * targetAxis[0];

		float angle = acos(dot);
		float sinHalfAngle = sin(angle / 2);

		rot[0] = cross[0] * sinHalfAngle; // x
		rot[1] = cross[1] * sinHalfAngle; // y
		rot[2] = cross[2] * sinHalfAngle; // z
		rot[3] = cos(angle / 2); // w

		return rot;
	}
}
