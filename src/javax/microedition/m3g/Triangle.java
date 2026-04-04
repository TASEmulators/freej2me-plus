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

class Triangle
{
	// Clipping planes
	private static final float[]  p = new float[] { 0, 0, 0, 0};
	private static final float[] xp = new float[] {-1, 0, 0, 1};
	private static final float[] xn = new float[] { 1, 0, 0, 1};
	private static final float[] yp = new float[] { 0,-1, 0, 1};
	private static final float[] yn = new float[] { 0, 1, 0, 1};
	private static final float[] zp = new float[] { 0, 0,-1, 1};
	private static final float[] zn = new float[] { 0, 0, 1, 1};

	// Let's reuse this when clipping, quite a bit faster than creating ArrayLists each time
	private static final float[] vert = new float[4];

	private final float[] v;
		// xA, yA, zA, wA,
		// xB, yB, zB, wB,
		// xC, yC, zC, wC;
		// 0   1   2   3

	private float[] t;
		// sA, tA, rA, qA,
		// sB, tB, rB, qB,
		// sC, tC, rC, qC;
		// 0   1   2   3

	private static int[] bufIndex;

	private int triangleIndex = 0;

	Triangle(float[] vertices, int triIndex) 
	{ 
		v = vertices;
		triangleIndex = triIndex;
	}

	public static final Triangle[] fromVertAndTris(float[] vert, float[] texc, int[] tris, int[] renderableTriangles, float near, int cullingMode)
	{
		renderableTriangles[0] = 0;
		boolean sharesVertices = false;
		final Triangle[] result = new Triangle[tris.length / 3];

		// Index buffer data. We don't need to make a sub-array copy for each triangle.
		// We can then just track it statically and each triangle's index will take care of the rest
		bufIndex = tris;

		for (int tri_id = 0; tri_id < tris.length / 3; tri_id++) 
		{
			if (tri_id > 0) 
			{
				sharesVertices = (tris[3* tri_id + 0] == tris[3* (tri_id-1) + 1] &&
								tris[3* tri_id + 1] == tris[3* (tri_id-1) + 2]) ||
								(tris[3* tri_id + 1] == tris[3* (tri_id-1) + 0] &&
								tris[3* tri_id + 2] == tris[3* (tri_id-1) + 1]);

				// Swap vertices for triangles if sharing is detected
				if (sharesVertices) 
				{
					// Swap indexA and indexB
					int temp = tris[3 * tri_id + 0];
					tris[3 * tri_id + 0] = tris[3 * tri_id + 1];
					tris[3 * tri_id + 1] = temp;
				}
			}

			result[renderableTriangles[0]] = new Triangle(new float[] // Vertex positions
			{
				vert[4 * tris[3 * tri_id + 0] + 0], // xA
				vert[4 * tris[3 * tri_id + 0] + 1], // yA
				vert[4 * tris[3 * tri_id + 0] + 2], // zA
				vert[4 * tris[3 * tri_id + 0] + 3], // wA
				vert[4 * tris[3 * tri_id + 1] + 0], // xB
				vert[4 * tris[3 * tri_id + 1] + 1], // yB
				vert[4 * tris[3 * tri_id + 1] + 2], // zB
				vert[4 * tris[3 * tri_id + 1] + 3], // wB
				vert[4 * tris[3 * tri_id + 2] + 0], // xC
				vert[4 * tris[3 * tri_id + 2] + 1], // yC
				vert[4 * tris[3 * tri_id + 2] + 2], // zC
				vert[4 * tris[3 * tri_id + 2] + 3]  // wC
			}, 
			tri_id); // Triangle Index

			// Check if this triangle should be rendered or clipped/culled.
			for (int i = 0; i < 3; i++) // Go through vertices A, B and C
			{ 
				if (result[renderableTriangles[0]].v[i * 4 + 3] >= near) // W cannot be smaller than the near plane, otherwise we'll erroneously cull triangles close to the camera
				{ 
					result[renderableTriangles[0]].v[i * 4 + 0] /= result[renderableTriangles[0]].v[i * 4 + 3]; // x / w
					result[renderableTriangles[0]].v[i * 4 + 1] /= result[renderableTriangles[0]].v[i * 4 + 3]; // y / w
					result[renderableTriangles[0]].v[i * 4 + 2] /= result[renderableTriangles[0]].v[i * 4 + 3]; // z / w
				}
			}

			boolean cullTriangle = (cullingMode == PolygonMode.CULL_BACK && result[renderableTriangles[0]].isCounterClockwise()) ||
								(cullingMode == PolygonMode.CULL_FRONT && !result[renderableTriangles[0]].isCounterClockwise());

			if (!cullTriangle)
			{
				// Move visible triangles (not clipped nor culled) to the front of the array
				if(!result[renderableTriangles[0]].clip()) 
				{ 
					// We now have to restore the renderable geometry back to its original coordinates, otherwise rendering will be broken
					for (int i = 0; i < 3; i++) 
					{
						result[renderableTriangles[0]].v[i * 4 + 0] *= result[renderableTriangles[0]].v[i * 4 + 3]; // x * w
						result[renderableTriangles[0]].v[i * 4 + 1] *= result[renderableTriangles[0]].v[i * 4 + 3]; // y * w
						result[renderableTriangles[0]].v[i * 4 + 2] *= result[renderableTriangles[0]].v[i * 4 + 3]; // z * w
					}

					// Triangle will be rendered, so we can now bother with allocating texture coordinates
					result[renderableTriangles[0]].setTexCoords(texc == null ? null : new float[] // Tex Coordinates
					{
						texc[4 * tris[3 * tri_id + 0] + 0], // sA
						texc[4 * tris[3 * tri_id + 0] + 1], // tA
						texc[4 * tris[3 * tri_id + 0] + 2], // rA
						texc[4 * tris[3 * tri_id + 0] + 3], // qA
						texc[4 * tris[3 * tri_id + 1] + 0], // sB
						texc[4 * tris[3 * tri_id + 1] + 1], // tB
						texc[4 * tris[3 * tri_id + 1] + 2], // rB
						texc[4 * tris[3 * tri_id + 1] + 3], // qB
						texc[4 * tris[3 * tri_id + 2] + 0], // sC
						texc[4 * tris[3 * tri_id + 2] + 1], // tC
						texc[4 * tris[3 * tri_id + 2] + 2], // rC
						texc[4 * tris[3 * tri_id + 2] + 3]  // qC
					});

					result[renderableTriangles[0]].project(); 
					renderableTriangles[0]++;
				}
			}
		}

		return result;
	}

	public final float xA() { return v[4 * 0 + 0]; }
	public final float yA() { return v[4 * 0 + 1]; }
	public final float zA() { return v[4 * 0 + 2]; }
	public final float wA() { return v[4 * 0 + 3]; }
	public final float xB() { return v[4 * 1 + 0]; }
	public final float yB() { return v[4 * 1 + 1]; }
	public final float zB() { return v[4 * 1 + 2]; }
	public final float wB() { return v[4 * 1 + 3]; }
	public final float xC() { return v[4 * 2 + 0]; }
	public final float yC() { return v[4 * 2 + 1]; }
	public final float zC() { return v[4 * 2 + 2]; }
	public final float wC() { return v[4 * 2 + 3]; }

	public final float sA() { return t[4 * 0 + 0]; }
	public final float tA() { return t[4 * 0 + 1]; }
	public final float rA() { return t[4 * 0 + 2]; }
	public final float qA() { return t[4 * 0 + 3]; }
	public final float sB() { return t[4 * 1 + 0]; }
	public final float tB() { return t[4 * 1 + 1]; }
	public final float rB() { return t[4 * 1 + 2]; }
	public final float qB() { return t[4 * 1 + 3]; }
	public final float sC() { return t[4 * 2 + 0]; }
	public final float tC() { return t[4 * 2 + 1]; }
	public final float rC() { return t[4 * 2 + 2]; }
	public final float qC() { return t[4 * 2 + 3]; }

	public final int getIndex(int index) { return bufIndex[3 * triangleIndex + index]; }

	public final boolean clip() 
	{		
		if (isValid()) 
		{			
			// Clip against each plane sequentially
			if (clipPlane(xp) != null &&
				clipPlane(xn) != null &&
				clipPlane(yp) != null &&
				clipPlane(yn) != null &&
				clipPlane(zp) != null &&
				clipPlane(zn) != null) 
			{				
				return false; // If it passed all planes, it means it's at least partially visible, don't clip
			}
		}
		
		return true;
	}

	public static final void transform(Triangle[] triangles, int visibleTris, Transform trVert, Transform trTex)
	{
		for (int i = 0; i < visibleTris; i++)
		{
			trVert.transform(triangles[i].v);
			if (triangles[i].t != null && trTex != null) { trTex.transform(triangles[i].t); }
		}
	}

	private final boolean isValid()
	{
		return wA() >= M3GMath.EPSILON || wB() >= M3GMath.EPSILON || wC() >= M3GMath.EPSILON;
	}

	public final void project()
	{
		// Apply perspective division to the triangle, it's going to NDC
		for (int i = 0; i < 3; i++) 
		{
			// Project vertex
			v[4 * i + 0] /= v[4 * i + 3]; // x / w
			v[4 * i + 1] /= v[4 * i + 3]; // y / w
			v[4 * i + 2] /= v[4 * i + 3]; // z / w
			v[4 * i + 3] = 1f;  // Set w to 1

			// Project texture coordinates
			t[4 * i + 0] /= v[4 * i + 3]; // u / w
			t[4 * i + 1] /= v[4 * i + 3]; // v / w
			// Those don't seem necessary
			//t[4 * i + 2] /= v[4 * i + 3]; // r / w
			//t[4 * i + 3] = 1f;  // Set q to 1
		}
	}

	private final Triangle clipPlane(float[] pn)
	{
		pn = M3GMath.div(pn, (float) M3GMath.sqrt(M3GMath.dotProduct(pn, pn)));

		// Test each vertex of the triangle against the clip planes
		for (int i = 0; i < 3; i++) 
		{
			vert[0] = v[4 * i + 0];
			vert[1] = v[4 * i + 1];
			vert[2] = v[4 * i + 2];
			vert[3] = v[4 * i + 3];
			if (M3GMath.dotProduct(pn, vert) - M3GMath.dotProduct(pn, p) >= 0) { return this; } // Partially visible in this plane, move to next plane
		}
			
		return null; // If no vertex is inside, return a null object since the triangle isn't visible
	}

	// Also used by perspective-correction
	public final void setTexCoords(float[] texCoords) 
	{
        if (texCoords != null && texCoords.length != 12) 
		{
            throw new IllegalArgumentException("Each vertex texture coordinate must have 4 elements (s, t, r, q).");
        }
        
		this.t = texCoords;
    }

	public final boolean isCounterClockwise() 
	{
		return (xB() - xA()) * (yC() - yA()) - (xC() - xA()) * (yB() - yA()) < 0; // Clockwise if normal points towards the viewer
	}
}
