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

public class KeyframeSequence extends Object3D
{

	public static final int CONSTANT = 192;
	public static final int LINEAR = 176;
	public static final int LOOP = 193;
	public static final int SLERP = 177;
	public static final int SPLINE = 178;
	public static final int SQUAD = 179;
	public static final int STEP = 180;


	private int duration;
	private int intType;
	private int keyframes;
	private int repeat = CONSTANT;
	private int rangeFirst;
	private int rangeLast;
	private int componentCount;
	private int nextKeyframe;
	private boolean dirty = true;

	private float[][] keyFrames;
	private float[][] inTangents;
	private float[][] outTangents;
	private int[] keyFrameTimes;
	private float[][] a;
	private float[][] b;

	private final float[] tempQ0 = new float[4];
	private final float[] tempQ1 = new float[4];
	private final float[] tempS0 = new float[4];
	private final float[] tempS1 = new float[4];

	public KeyframeSequence(int numKeyframes, int numComponents, int interpolation)
	{
		if (numKeyframes < 1 || numComponents < 1)
		{
			throw new IllegalArgumentException("Number of keyframes and components must be >= 1");
		}

		switch (interpolation)
		{
			case SLERP:
			case SQUAD:
				if (numComponents != 4)
					throw new IllegalArgumentException("SLERP and SQUAD require 4 components");
				if (interpolation == SQUAD) {
					a = new float[numKeyframes][4];
					b = new float[numKeyframes][4];
				}
				break;
			case STEP:
			case LINEAR:
				break;
			case SPLINE:
				inTangents = new float[numKeyframes][numComponents];
				outTangents = new float[numKeyframes][numComponents];
				break;
			default:
				throw new IllegalArgumentException("Unknown interpolation mode");
		}

		this.keyframes = numKeyframes;
		this.componentCount = numComponents;
		this.intType = interpolation;

		keyFrames = new float[numKeyframes][numComponents];
		keyFrameTimes = new int[numKeyframes];
		rangeFirst = 0;
		rangeLast = numKeyframes - 1;
	}

	protected Object3D duplicateImpl()
	{
		KeyframeSequence copy = (KeyframeSequence) super.duplicateImpl();

		copy.duration = this.duration;
		copy.intType = this.intType;
		copy.keyframes = this.keyframes;
		copy.repeat = this.repeat;
		copy.rangeFirst = this.rangeFirst;
		copy.rangeLast = this.rangeLast;
		copy.componentCount = this.componentCount;
		copy.dirty = true;

		copy.keyFrames = new float[keyframes][componentCount];
		for (int i = 0; i < keyframes; i++) {
			System.arraycopy(this.keyFrames[i], 0, copy.keyFrames[i], 0, componentCount);
		}

		copy.keyFrameTimes = this.keyFrameTimes.clone();

		if (inTangents != null) {
			copy.inTangents = new float[keyframes][componentCount];
			copy.outTangents = new float[keyframes][componentCount];
		}

		if (a != null) {
			copy.a = new float[keyframes][4];
			copy.b = new float[keyframes][4];
		}

		return copy;
	}

	public int getComponentCount() { return this.componentCount; }

	public int getDuration() { return duration; }

	public int getInterpolationType() { return intType; }

	public int getKeyframeCount() { return keyframes; }

	public int getRepeatMode() { return repeat; }

	public int getValidRangeFirst() { return rangeFirst; }

	public int getValidRangeLast() { return rangeLast; }

	public void setDuration(int value)
	{
		if (value <= 0) {
			throw new IllegalArgumentException("Duration must be positive");
		}
		this.duration = value;
		this.dirty = true;
	}

	public void setRepeatMode(int mode) {
		if (mode != CONSTANT && mode != LOOP) {
			throw new IllegalArgumentException("Invalid repeat mode");
		}
		this.repeat = mode;
	}

	public void setValidRange(int first, int last)
	{
		if (first < 0 || first >= keyframes || last < 0 || last >= keyframes || first > last)
		{
			throw new IllegalArgumentException("Invalid keyframe range");
		}
		this.rangeFirst = first;
		this.rangeLast = last;
		this.dirty = true;
	}

	public void setKeyframe(int index, int time, float[] value)
	{
		if (value == null) { throw new NullPointerException("Value vector must not be null"); }
		if (index < 0 || index >= keyframes) { throw new IndexOutOfBoundsException(); }
		if (value.length < componentCount || time < 0) { throw new IllegalArgumentException(); }

		System.arraycopy(value, 0, keyFrames[index], 0, componentCount);
		keyFrameTimes[index] = time;

		if (intType == SLERP || intType == SQUAD)
		{
			M3GMath.normalizeQuat(keyFrames[index]);
		}
		dirty = true;
	}

	public int getKeyframe(int index, float[] value)
	{
		if (index < 0 || index >= keyframes) { throw new IndexOutOfBoundsException(); }
		if (value != null && value.length < componentCount) { throw new IllegalArgumentException(); }

		if (value != null) {
			System.arraycopy(keyFrames[index], 0, value, 0, componentCount);
		}

		return keyFrameTimes[index];
	}

	public int getSample(float time, float[] sample)
	{
		if (sample == null || sample.length < componentCount) {
			throw new IllegalArgumentException("Sample array is null or too small");
		}

		if (repeat == LOOP && duration > 0)
		{
			time = time % duration;
			if (time < 0.0f) { time += duration; }
		}

		if (time < keyFrameTimes[rangeFirst])
		{
			System.arraycopy(keyFrames[rangeFirst], 0, sample, 0, componentCount);
			return M3GMath.roundPositive(keyFrameTimes[rangeFirst] - time);
		}
		else if (time >= keyFrameTimes[rangeLast])
		{
			System.arraycopy(keyFrames[rangeLast], 0, sample, 0, componentCount);
			return Integer.MAX_VALUE;
		}

		// Find surrounding keyframes
		int start = nextKeyframe;
		if (keyFrameTimes[start] > time || start < rangeFirst || start >= rangeLast)
			{ start = rangeFirst; }

		while (start < rangeLast && keyFrameTimes[start + 1] <= time) { start++; }

		nextKeyframe = start;
		int end = start + 1;

		int dt = keyFrameTimes[end] - keyFrameTimes[start];
		if (dt == 0 || time == keyFrameTimes[start] || intType == STEP)
		{
			System.arraycopy(keyFrames[start], 0, sample, 0, componentCount);
			return (intType == STEP) ? M3GMath.roundPositive(keyFrameTimes[end] - time) : 1;
		}

		float s = (time - (float) keyFrameTimes[start]) / (float) dt;

		switch (intType)
		{
			case LINEAR:
				M3GMath.lerpVec3(componentCount, sample, s, keyFrames[start], keyFrames[end]);
				break;

			case SLERP:
				M3GMath.slerpQuat(sample, s, keyFrames[start], keyFrames[end]);
				break;

			case SPLINE:
				float s2 = s * s;
				float s3 = s2 * s;
				float h00 = 2 * s3 - 3 * s2 + 1;
				float h10 = s3 - 2 * s2 + s;
				float h01 = -2 * s3 + 3 * s2;
				float h11 = s3 - s2;

				float[] p0 = keyFrames[start];
				float[] p1 = keyFrames[end];
				float[] t0 = outTangents[start];
				float[] t1 = inTangents[end];

				for (int i = 0; i < componentCount; i++) {
					sample[i] = h00 * p0[i] + h10 * t0[i] + h01 * p1[i] + h11 * t1[i];
				}
				break;

			case SQUAD:
				M3GMath.slerpQuat(tempQ0, s, keyFrames[start], keyFrames[end]);
				M3GMath.slerpQuat(tempQ1, s, a[start], b[end]);
				M3GMath.slerpQuat(sample, 2.0f * s * (1.0f - s), tempQ0, tempQ1);
				break;
		}

		return 1;
	}
}
