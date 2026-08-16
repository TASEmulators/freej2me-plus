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

public class Material extends Object3D
{
	public static final int AMBIENT = 1024;
	public static final int DIFFUSE = 2048;
	public static final int EMISSIVE = 4096;
	public static final int SPECULAR = 8192;

	private int ambientColor = 0x00333333;
	private int diffuseColor = 0xFFCCCCCC;
	private int emissiveColor = 0x00000000;
	private int specularColor = 0x00000000;
	private float shininess = 0.0f;
	private boolean tracking = false;

	public Material() { }

	protected Object3D duplicateImpl()
	{
		Material copy = (Material) super.duplicateImpl();
		copy.ambientColor = this.ambientColor;
		copy.diffuseColor = this.diffuseColor;
		copy.emissiveColor = this.emissiveColor;
		copy.specularColor = this.specularColor;
		copy.shininess = this.shininess;
		copy.tracking = this.tracking;
		return copy;
	}

	public int getColor(int target)
	{
		/* As per JSR-184, throw IllegalArgumentException if target has a value other than AMBIENT, DIFFUSSE, EMISSIVE or SPECULAR. */
		if (target != AMBIENT && target != DIFFUSE && target != EMISSIVE && target != SPECULAR)
			{ throw new IllegalArgumentException("Tried to get invalid color component from material."); }

		switch(target)
		{
			case DIFFUSE: return this.diffuseColor;
			case EMISSIVE: return this.emissiveColor;
			case SPECULAR: return this.specularColor;
			case AMBIENT:
			default: return this.ambientColor;
		}
	}

	public float getShininess() { return this.shininess; }

	public boolean isVertexColorTrackingEnabled() { return this.tracking; }

	public void setColor(int target, int ARGB)
	{
		/* As per JSR-184, throw IllegalArgumentException if target has a value other than an inclusive OR of one or more of AMBIENT, DIFFUSE, EMISSIVE, SPECULAR. */
		if (target == 0 || (target & ~(AMBIENT | DIFFUSE | EMISSIVE | SPECULAR)) != 0)
			{throw new IllegalArgumentException("Trying to set material color on invalid material component."); }


		if ((target & AMBIENT)  != 0) { this.ambientColor = ARGB & 0x00FFFFFF;  }
		if ((target & DIFFUSE)  != 0) { this.diffuseColor = ARGB;  }
		if ((target & EMISSIVE) != 0) { this.emissiveColor = ARGB & 0x00FFFFFF; }
		if ((target & SPECULAR) != 0) { this.specularColor = ARGB & 0x00FFFFFF; }
	}

	public void setShininess(float shininess)
	{
		/* As per JSR-184, throw IllegalArgumentException if shininess > 128 or < 0. */
		if(shininess < 0f || shininess > 128.0f) { throw new IllegalArgumentException("Material received invalid shininess value:" + shininess); }

		this.shininess = shininess;
	}

	public void setVertexColorTrackingEnable(boolean enable) { this.tracking = enable; }

	void updateProperty(int property, float[] value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Graphics3D.class.getPackage().getName() + "." + Graphics3D.class.getSimpleName() + ": " + "AnimTrack updating material property");

		switch (property)
		{
			case AnimationTrack.ALPHA:
				int a = M3GMath.max(0, M3GMath.min(255, (int) (value[0] <= 1.0f ? value[0] * 255.0f : value[0])));
				this.diffuseColor = (a << 24) | (this.diffuseColor & 0x00FFFFFF);
				break;
			case AnimationTrack.AMBIENT_COLOR:
				this.ambientColor = parseRGB(value);
				break;
			case AnimationTrack.DIFFUSE_COLOR:
				this.diffuseColor = (this.diffuseColor & 0xFF000000) | parseRGB(value);
				break;
			case AnimationTrack.EMISSIVE_COLOR:
				this.emissiveColor = parseRGB(value);
				break;
			case AnimationTrack.SPECULAR_COLOR:
				this.specularColor = parseRGB(value);
				break;
			case AnimationTrack.SHININESS:
				this.shininess = M3GMath.max(0.0f, M3GMath.min(128.0f, value[0]));
				break;
			default:
				super.updateProperty(property, value);
		}
	}

	boolean animTrackCompatible(AnimationTrack track)
	{
		switch (track.getTargetProperty()) {
			case AnimationTrack.ALPHA:
			case AnimationTrack.AMBIENT_COLOR:
			case AnimationTrack.DIFFUSE_COLOR:
			case AnimationTrack.EMISSIVE_COLOR:
			case AnimationTrack.SHININESS:
			case AnimationTrack.SPECULAR_COLOR:
				return true;
			default:
				return super.animTrackCompatible(track);
		}
	}

	private static int parseRGB(float[] value)
	{
		int r = M3GMath.max(0, M3GMath.min(255, (int) (value[0] <= 1.0f ? value[0] * 255.0f : value[0])));
		int g = M3GMath.max(0, M3GMath.min(255, (int) (value[1] <= 1.0f ? value[1] * 255.0f : value[1])));
		int b = M3GMath.max(0, M3GMath.min(255, (int) (value[2] <= 1.0f ? value[2] * 255.0f : value[2])));
		return (r << 16) | (g << 8) | b;
	}
}
