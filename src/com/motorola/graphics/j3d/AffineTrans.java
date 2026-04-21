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

public class AffineTrans 
{
	// This might be an issue. MCV3 shouldn't have made these public. No easy way around these when moving across
	// all the different packages that implement this.
	
	public int m00, m01, m02, m03;
	public int m10, m11, m12, m13;
	public int m20, m21, m22, m23;

	protected com.mascotcapsule.micro3d.v3.AffineTrans afTrans;

	public AffineTrans() { afTrans = new com.mascotcapsule.micro3d.v3.AffineTrans(); }

	public AffineTrans(int[][] a) 
	{
		afTrans = new com.mascotcapsule.micro3d.v3.AffineTrans(a);
	}

	AffineTrans(com.mascotcapsule.micro3d.v3.AffineTrans trans) { afTrans = trans; }

	public AffineTrans(int m00, int m01, int m02, int m03,
			int m10, int m11, int m12, int m13,
			int m20, int m21, int m22, int m23)
	{
		afTrans = new com.mascotcapsule.micro3d.v3.AffineTrans(m00, m01, m02, m03, m10, m11, m12, m13,
			m20, m21, m22, m23);
	}

	public final void get(int[] a) { afTrans.get(a, 0); }

	public final void get(int[] a, int offset) { afTrans.get(a, offset); }

	public final void lookAt(Vector3D pos, Vector3D look, Vector3D up) 
	{
		afTrans.lookAt((com.mascotcapsule.micro3d.v3.Vector3D) pos,
			(com.mascotcapsule.micro3d.v3.Vector3D) look,
			(com.mascotcapsule.micro3d.v3.Vector3D)up);
	}

	public final void mul(AffineTrans a) { afTrans.mul(this.afTrans, a.afTrans); }

	public final void mul(AffineTrans a1, AffineTrans a2) { afTrans.mul(a1.afTrans, a2.afTrans); }

	public final void multiply(AffineTrans a) { afTrans.mul(this.afTrans, a.afTrans); }

	public final void multiply(AffineTrans a1, AffineTrans a2) { afTrans.mul(a1.afTrans, a2.afTrans); }

	public final void rotationV(Vector3D v, int r)
	{ 
		afTrans.setRotation((com.mascotcapsule.micro3d.v3.Vector3D) v, r);
	}

	public final void rotationX(int r) { afTrans.rotationX(r); }

	public final void rotationY(int r) { afTrans.rotationY(r); }

	public final void rotationZ(int r) { afTrans.rotationZ(r); }

	public final void set(AffineTrans a) { afTrans.set(a.afTrans); }

	public final void set(int[] a) { afTrans.set(a, 0); }

	public final void set(int[][] a) { afTrans.set(a); }

	public final void set(int[] a, int offset) { afTrans.set(a, offset); }

	public final void set(
			int m00, int m01, int m02, int m03,
			int m10, int m11, int m12, int m13,
			int m20, int m21, int m22, int m23)
	{
		afTrans.set(m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23);
	}

	public final void setIdentity() { afTrans.setIdentity(); }

	public final void setRotation(Vector3D v, int r)
	{ 
		afTrans.setRotation((com.mascotcapsule.micro3d.v3.Vector3D) v, r);
	}

	public final void setRotationX(int r) { afTrans.setRotationX(r); }

	public final void setRotationY(int r) { afTrans.setRotationY(r); }

	public final void setRotationZ(int r) { afTrans.setRotationZ(r); }

	public final void setViewTrans(Vector3D pos, Vector3D look, Vector3D up)
	{ 
		afTrans.lookAt((com.mascotcapsule.micro3d.v3.Vector3D) pos,
			(com.mascotcapsule.micro3d.v3.Vector3D) look,
			(com.mascotcapsule.micro3d.v3.Vector3D) up);
	}

	public final Vector3D transform(Vector3D v)
	{ 
		return (Vector3D) afTrans.transform((com.mascotcapsule.micro3d.v3.Vector3D) v);
	}

	public final Vector3D transPoint(Vector3D v)
	{ 
		return (Vector3D) afTrans.transPoint((com.mascotcapsule.micro3d.v3.Vector3D) v);
	}
}