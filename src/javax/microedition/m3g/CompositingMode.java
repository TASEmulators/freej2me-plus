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

import org.recompile.mobile.Mobile;

public class CompositingMode extends Object3D
{
	public static final int ALPHA = 64;
	public static final int ALPHA_ADD = 65;
	public static final int MODULATE = 66;
	public static final int MODULATE_X2 = 67;
	public static final int REPLACE = 68;

	private int blending = REPLACE;
	private float alphaThreshold = 0.0f;
	private boolean alphaWrite = true;
	private boolean depthWrite = true;
	private boolean depthTest = true;
	private float depthOffsetUnits = 0.0f;
	private float depthOffsetFactor = 0.0f;
	private boolean colorWrite = true;


	public CompositingMode() { }

	protected Object3D duplicateImpl()
	{
		CompositingMode copy = (CompositingMode) super.duplicateImpl();
		copy.blending = this.blending;
		copy.alphaThreshold = this.alphaThreshold;
		copy.alphaWrite = this.alphaWrite;
		copy.depthWrite = this.depthWrite;
		copy.depthTest = this.depthTest;
		copy.depthOffsetUnits = this.depthOffsetUnits;
		copy.depthOffsetFactor = this.depthOffsetFactor;
		copy.colorWrite = this.colorWrite;
		return copy;
	}

	public float getAlphaThreshold() { return this.alphaThreshold; }

	public int getBlending() { return this.blending; }

	public float getDepthOffsetFactor() { return this.depthOffsetFactor; }

	public float getDepthOffsetUnits() { return this.depthOffsetUnits; }

	public boolean isAlphaWriteEnabled() { return this.alphaWrite; }

	public boolean isColorWriteEnabled() { return this.colorWrite; }

	public boolean isDepthTestEnabled() { return this.depthTest; }

	public boolean isDepthWriteEnabled() { return this.depthWrite; }

	public void setAlphaThreshold(float threshold)
	{
		if (threshold < 0.0f || 1.0f < threshold)
			{ throw new IllegalArgumentException("Invalid threshold:" + threshold); }

		this.alphaThreshold = threshold;
	}

	public void setAlphaWriteEnable(boolean enable) { this.alphaWrite = enable; }

	public void setBlending(int mode)
	{
		if (mode != ALPHA &&
			mode != ALPHA_ADD &&
			mode != MODULATE &&
			mode != MODULATE_X2 &&
			mode != REPLACE)
			{ throw new IllegalArgumentException("Invalid blend mode:" + mode); }

		this.blending = mode;
	}

	public void setColorWriteEnable(boolean enable) { this.colorWrite = enable; }

	public void setDepthOffset(float factor, float units)
	{
		this.depthOffsetFactor = factor;
		this.depthOffsetUnits = units;
	}

	public void setDepthTestEnable(boolean enable) { this.depthTest = enable; }

	public void setDepthWriteEnable(boolean enable) { this.depthWrite = enable; }

	void updateProperty(int property, float[] value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Graphics3D.class.getPackage().getName() + "." + Graphics3D.class.getSimpleName() + ": " + "AnimTrack updating compositingMode property");
		switch (property)
		{
			case AnimationTrack.ALPHA:
				this.alphaThreshold = M3GMath.max(0.0f, M3GMath.min(1.0f, value[0]));
				break;
			default:
				super.updateProperty(property, value);
		}
	}

	boolean animTrackCompatible(AnimationTrack track)
	{
		switch (track.getTargetProperty())
		{
			case AnimationTrack.ALPHA:
				return true;
			default:
				return super.animTrackCompatible(track);
		}
	}
}
