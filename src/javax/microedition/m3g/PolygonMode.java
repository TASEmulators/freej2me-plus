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

public class PolygonMode extends Object3D
{

	public static final int CULL_BACK = 160;
	public static final int CULL_FRONT = 161;
	public static final int CULL_NONE = 162;
	public static final int SHADE_FLAT = 164;
	public static final int SHADE_SMOOTH = 165;
	public static final int WINDING_CCW = 168;
	public static final int WINDING_CW = 169;

	private int culling;
	private int shading;
	private int winding;
	private boolean twoSidedLighting;
	private boolean localCameraLighting;
	private boolean perspectiveCorrection;

	public PolygonMode()
	{
		this.culling = CULL_BACK;
		this.winding = WINDING_CCW;
		this.shading = SHADE_SMOOTH;
		this.twoSidedLighting = false;
		this.localCameraLighting = false;
		this.perspectiveCorrection = false;
	}

	protected Object3D duplicateImpl()
	{
		PolygonMode copy = (PolygonMode) super.duplicateImpl();
		copy.culling = this.culling;
		copy.shading = this.shading;
		copy.winding = this.winding;
		copy.twoSidedLighting = this.twoSidedLighting;
		copy.localCameraLighting = this.localCameraLighting;
		copy.perspectiveCorrection = this.perspectiveCorrection;
		return copy;
	}

	public int getCulling() { return this.culling; }

	public int getShading() { return this.shading; }

	public int getWinding() { return this.winding; }

	public boolean isLocalCameraLightingEnabled() { return this.localCameraLighting; }

	public boolean isPerspectiveCorrectionEnabled() { return this.perspectiveCorrection; }

	public boolean isTwoSidedLightingEnabled() { return this.twoSidedLighting; }

	public void setCulling(int mode)
	{
		if (mode != CULL_BACK &&
			mode != CULL_FRONT &&
			mode != CULL_NONE)
			throw new java.lang.IllegalArgumentException("Invalid cull mode:" + mode);

		this.culling = mode;
	}

	public void setLocalCameraLightingEnable(boolean enable) { this.localCameraLighting = enable; }

	public void setPerspectiveCorrectionEnable(boolean enable) { this.perspectiveCorrection = enable; }

	public void setShading(int mode)
	{
		if (mode != SHADE_FLAT &&
			mode != SHADE_SMOOTH)
			throw new java.lang.IllegalArgumentException("Invalid shading:" + mode);

		this.shading = mode;
	}

	public void setTwoSidedLightingEnable(boolean enable) { this.twoSidedLighting = enable; }

	public void setWinding(int mode)
	{
		if (mode != WINDING_CCW &&
			mode != WINDING_CW)
			throw new java.lang.IllegalArgumentException("Invalid winding:" + mode);

		this.winding = mode;
	}
}
