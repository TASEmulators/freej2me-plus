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
package com.jblend.graphics.j3d;

public class FigureLayout
{
	com.mascotcapsule.micro3d.v3.FigureLayout layout;

	public FigureLayout() { this(null, 512, 512, 0, 0); }

	public FigureLayout(AffineTrans trans, int sx, int sy, int cx, int cy)
	{
		layout = new com.mascotcapsule.micro3d.v3.FigureLayout(trans == null ? null : trans.afTrans, sx, sy, cx, cy);
	}

	public final AffineTrans getAffineTrans() {
		return new AffineTrans(layout.getAffineTrans());
	}

	public final int getCenterX() { return layout.getCenterX(); }

	public final int getCenterY() { return layout.getCenterY(); }

	public final int getParallelHeight() { return layout.getParallelHeight(); }

	public final int getParallelWidth() { return layout.getParallelWidth(); }

	public final int getScaleX() { return layout.getScaleX(); }

	public final int getScaleY() { return layout.getScaleY(); }

	public final void selectAffineTrans(int idx) { layout.selectAffineTrans(idx); }

	public final void setAffineTrans(AffineTrans trans) { layout.setAffineTrans(trans.afTrans); }

	public final void setAffineTrans(AffineTrans[] trans) {
		if (trans == null) throw new NullPointerException();
		
		for (int i = 0; i < trans.length; i++)
			if (trans[i] == null) throw new NullPointerException();

		com.mascotcapsule.micro3d.v3.AffineTrans[] afTrans = new com.mascotcapsule.micro3d.v3.AffineTrans[trans.length];

		for (int i = 0; i < trans.length; i++)
			afTrans[i] = trans[i].afTrans;
		
		layout.setAffineTrans(afTrans);
	}

	public final void setAffineTransArray(AffineTrans[] trans) { this.setAffineTrans(trans); }

	public final void setCenter(int cx, int cy) { layout.setCenter(cx, cy); }

	public final void setParallelSize(int w, int h) { layout.setParallelSize(w, h); }

	public final void setPerspective(int zNear, int zFar, int angle) { layout.setPerspective(zNear, zFar, angle); }

	public final void setPerspective(int zNear, int zFar, int width, int height)
	{
		layout.setPerspective(zNear, zFar, width, height);
	}

	public final void setScale(int sx, int sy) { layout.setScale(sx, sy); }

	public final com.mascotcapsule.micro3d.v3.FigureLayout getLayout() { return layout; }
}