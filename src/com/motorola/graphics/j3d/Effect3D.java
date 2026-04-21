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
package com.motorola.graphics.j3d;

public class Effect3D
{
	protected com.mascotcapsule.micro3d.v3.Effect3D effect;

	public Effect3D() { effect = new com.mascotcapsule.micro3d.v3.Effect3D(); }

	public Effect3D(Light light, int shading, boolean isEnableTrans, Texture tex)
	{
		effect = new com.mascotcapsule.micro3d.v3.Effect3D(
			(com.mascotcapsule.micro3d.v3.Light) light, shading,
		isEnableTrans, (com.mascotcapsule.micro3d.v3.Texture) tex);
	}

	public final Light getLight() { return (Light) effect.getLight(); }

	public final void setLight(Light light)
	{ 
		effect.setLight((com.mascotcapsule.micro3d.v3.Light) light);
	}

	public final int getShadingType() { return effect.getShadingType(); }

	public final int getShading() { return effect.getShading(); }

	public final void setShadingType(int shading) { effect.setShadingType(shading); }
	
	public final void setShading(int shading) { effect.setShadingType(shading); }

	public final int getToonThreshold() { return effect.getToonThreshold(); }

	public final int getThreshold() { return effect.getToonThreshold(); }

	public final int getToonHigh() { return effect.getToonHigh(); }

	public final int getThresholdHigh() { return effect.getThresholdHigh(); }

	public final int getToonLow() { return effect.getToonLow(); }

	public final int getThresholdLow() { return effect.getThresholdLow(); }

	public final void setToonParams(int threshold, int high, int low)
	{
		effect.setToonParams(threshold, high, low);
	}
	
	public final void setThreshold(int threshold, int high, int low)
	{
		effect.setThreshold(threshold, high, low);
	}

	public final boolean isTransparency() { return effect.isTransparency(); }

	public final boolean isSemiTransparentEnabled() { return effect.isSemiTransparentEnabled(); }

	public final void setTransparency(boolean isEnable) { effect.setTransparency(isEnable); }

	public final void setSemiTransparentEnabled(boolean isEnable) { effect.setSemiTransparentEnabled(isEnable); }

	public final Texture getSphereTexture() { return (Texture) effect.getSphereTexture(); }

	public final Texture getSphereMap() { return (Texture) effect.getSphereMap(); }

	public final void setSphereTexture(Texture tex)
	{ 
		effect.setSphereTexture((com.mascotcapsule.micro3d.v3.Texture) tex);
	}
	
	public final void setSphereMap(Texture tex)
	{ 
		effect.setSphereMap((com.mascotcapsule.micro3d.v3.Texture) tex);
	}

	public final com.mascotcapsule.micro3d.v3.Effect3D getEffect() { return effect; }
}