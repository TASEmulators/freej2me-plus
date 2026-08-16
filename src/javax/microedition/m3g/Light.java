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

public class Light extends Node
{

	public static final int AMBIENT = 128;
	public static final int DIRECTIONAL = 129;
	public static final int OMNI = 130;
	public static final int SPOT = 131;

	private int mode = DIRECTIONAL;
	private int color = 0x00FFFFFF;
	private float intensity = 1.0f;
	private float constant = 1.0f;
	private float linear = 0.0f;
	private float quadratic = 0.0f;
	private float angle = 180.0f;
	private float exponent = 0.0f;

	public Light() { }

	protected Object3D duplicateImpl()
	{
		Light copy = (Light) super.duplicateImpl();
		copy.mode = this.mode;
		copy.color = this.color;
		copy.intensity = this.intensity;
		copy.constant = this.constant;
		copy.linear = this.linear;
		copy.quadratic = this.quadratic;
		copy.angle = this.angle;
		copy.exponent = this.exponent;
		return copy;
	}

	public int getColor() { return this.color; }

	public float getConstantAttenuation() { return this.constant; }

	public float getIntensity() { return this.intensity; }

	public float getLinearAttenuation() { return this.linear; }

	public int getMode() { return this.mode; }

	public float getQuadraticAttenuation() { return this.quadratic; }

	public float getSpotAngle() { return this.angle; }

	public float getSpotExponent() { return this.exponent; }

	public void setAttenuation(float c, float l, float q)
	{
		if (c < 0.0f || l < 0.0f || q < 0.0f || (c == 0.0f && l == 0.0f && q == 0.0f))
			{ throw new IllegalArgumentException("Invalid attenuation coefficients."); }

		this.constant = c;
		this.linear = l;
		this.quadratic = q;
	}

	public void setColor(int RGB) { this.color = RGB & 0x00FFFFFF;; }

	public void setIntensity(float value) { this.intensity = value; }

	public void setMode(int value)
	{
		if (mode < AMBIENT || mode > SPOT)
			{ throw new IllegalArgumentException("Invalid light mode: " + mode); }

		this.mode = value;
	}

	public void setSpotAngle(float theta)
	{
		if ((theta < 0.0f || theta > 90.0f) && theta != 180.0f)
			{ throw new IllegalArgumentException("Spot angle must be either in range of [0, 90], or equal to 180."); }

		this.angle = theta;
	}

	public void setSpotExponent(float exp)
	{
		if (exp < 0.0f || exp > 128.0f)
			{ throw new IllegalArgumentException("Spot exponent must be in range of [0, 128]."); }

		this.exponent = exp;
	}

	void updateProperty(int property, float[] value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Graphics3D.class.getPackage().getName() + "." + Graphics3D.class.getSimpleName() + ": " + "AnimTrack updating light property");
		switch (property)
		{
			case AnimationTrack.COLOR:
				int r = M3GMath.max(0, M3GMath.min(255, (int) (value[0] <= 1.0f ? value[0] * 255.0f : value[0])));
				int g = M3GMath.max(0, M3GMath.min(255, (int) (value[1] <= 1.0f ? value[1] * 255.0f : value[1])));
				int b = M3GMath.max(0, M3GMath.min(255, (int) (value[2] <= 1.0f ? value[2] * 255.0f : value[2])));
				this.color = (r << 16) | (g << 8) | b;
				break;
			case AnimationTrack.INTENSITY:
				intensity = value[0];
				break;
			case AnimationTrack.SPOT_ANGLE:
				angle = M3GMath.max(0.0f, M3GMath.min(90.0f, value[0]));
				break;
			case AnimationTrack.SPOT_EXPONENT:
				exponent = M3GMath.max(0.0f, M3GMath.min(128.0f, value[0]));
				break;
			default:
				super.updateProperty(property, value);
		}
	}

	boolean animTrackCompatible(AnimationTrack track)
	{
		switch (track.getTargetProperty())
		{
			case AnimationTrack.COLOR:
			case AnimationTrack.INTENSITY:
			case AnimationTrack.SPOT_ANGLE:
			case AnimationTrack.SPOT_EXPONENT:
				return true;
			default:
				return super.animTrackCompatible(track);
		}
	}
}
