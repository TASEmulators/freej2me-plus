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

public class Fog extends Object3D
{

	public static final int	EXPONENTIAL = 80;
	public static final int LINEAR = 81;

	private float near = 0.0f;
	private float far = 1.0f;
	private int mode = LINEAR;
	private int color = 0x00000000;
	private float density = 1.0f;

	public Fog() { }

	protected Object3D duplicateImpl()
	{
		Fog copy = (Fog) super.duplicateImpl();
		copy.near = this.near;
		copy.far = this.far;
		copy.mode = this.mode;
		copy.color = this.color;
		copy.density = this.density;
		return copy;
	}

	public int getColor() { return this.color; }

	public float getDensity() { return this.density; }

	public float getFarDistance() { return this.far; }

	public int getMode() { return this.mode; }

	public float getNearDistance() { return this.near; }

	public void setColor(int RGB) { this.color = RGB & 0x00FFFFFF; }

	public void setDensity(float value)
	{
		if(value < 0.0f) { throw new IllegalArgumentException("Invalid density value"); }
		this.density = value;
	}

	public void setLinear(float Near, float Far)
	{
		this.near = Near;
		this.far = Far;
	}

	public void setMode(int value)
	{
		if(value != LINEAR && value != EXPONENTIAL)
			{ throw new IllegalArgumentException("Fog only supports LINEAR and EXPONENTIAL types"); }
		this.mode = value;
	}

	void updateProperty(int property, float[] value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Graphics3D.class.getPackage().getName() + "." + Graphics3D.class.getSimpleName() + ": " + "AnimTrack updating Fog property");
		switch (property)
		{
			case AnimationTrack.COLOR:
				int r = M3GMath.max(0, M3GMath.min(255, (int) (value[0] <= 1.0f ? value[0] * 255.0f : value[0])));
				int g = M3GMath.max(0, M3GMath.min(255, (int) (value[1] <= 1.0f ? value[1] * 255.0f : value[1])));
				int b = M3GMath.max(0, M3GMath.min(255, (int) (value[2] <= 1.0f ? value[2] * 255.0f : value[2])));
				this.color = (r << 16) | (g << 8) | b;
				break;
			case AnimationTrack.DENSITY:
				this.density = M3GMath.max(0.0f, value[0]);
				break;
			case AnimationTrack.FAR_DISTANCE:
				far = value[0];
				break;
			case AnimationTrack.NEAR_DISTANCE:
				near = value[0];
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
			case AnimationTrack.DENSITY:
			case AnimationTrack.FAR_DISTANCE:
			case AnimationTrack.NEAR_DISTANCE:
				return true;
			default:
				return super.animTrackCompatible(track);
		}
	}
}
