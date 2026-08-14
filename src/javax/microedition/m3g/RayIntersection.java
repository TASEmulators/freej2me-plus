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

public class RayIntersection
{
	private Node intersected = null;
	private float distance = 0.f;
	private int submeshIndex = 0;
	private float[] textureS = new float[Graphics3D.getTextureUnitCount()];
	private float[] textureT = new float[Graphics3D.getTextureUnitCount()];
	private float[] normal = new float[3];
	private float[] ray = new float[6];

	public RayIntersection()
	{
		normal[2] = 1.f;
		ray[5] = 1.f;
	}

	public Node getIntersected() { return this.intersected; }

	public float getDistance() { return this.distance; }

	public int getSubmeshIndex() { return this.submeshIndex; }

	public float getTextureS(int index)
	{
		if (index < 0 || index >= textureS.length)
			{ throw new IndexOutOfBoundsException("Invalid texture unit"); }

		return this.textureS[index];
	}

	public float getTextureT(int index)
	{
		if (index < 0 || index >= textureS.length)
			{ throw new IndexOutOfBoundsException("Invalid texture unit"); }

		return this.textureT[index];
	}

	public float getNormalX() { return this.normal[0]; }

	public float getNormalY() { return this.normal[1]; }

	public float getNormalZ() { return this.normal[2]; }

	public void getRay(float[] ray)
	{
		if (ray == null) { throw new NullPointerException("Ray cannot be null"); }

		if (ray.length < 6) { throw new IllegalArgumentException("Invalid Ray size"); }

		System.arraycopy(this.ray, 0, ray, 0, 6);
	}

	void set(Node node, float distance, int submeshIndex, float[] ray, float[] normal, float[] texS, float[] texT)
	{
		this.intersected = node;
		this.distance = distance;
		this.submeshIndex = submeshIndex;

		if (ray != null) { System.arraycopy(ray, 0, this.ray, 0, 6); }

		if (normal != null) { System.arraycopy(normal, 0, this.normal, 0, 3); }

		if (texS != null)
		{
			int len = Math.min(texS.length, this.textureS.length);
			System.arraycopy(texS, 0, this.textureS, 0, len);
		}

		if (texT != null)
		{
			int len = Math.min(texT.length, this.textureT.length);
			System.arraycopy(texT, 0, this.textureT, 0, len);
		}
	}
}
