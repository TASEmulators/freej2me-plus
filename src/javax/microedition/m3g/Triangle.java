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
	// Temporary buffer for vertex colors
	private static final byte[] COLOR_VERTEX = new byte[4];

	// Temporary buffer for input and output vertices/texCoords/vertex colors.
	private static final int[] inC = new int[3];
	private static final int[] outC = new int[4];
	private static final float[] inV = new float[12];
	private static final float[][] inT = new float[Graphics3D.NUM_TEXTURE_UNITS][12];
	private static final float[] outV = new float[16];
	private static final float[][] outT = new float[Graphics3D.NUM_TEXTURE_UNITS][16];

	// Output array of triangles. Allows us to reuse the memory block allocated for triangle
	// data without needing to GC it every render pass (it'll still reallocate if the triangle count increases)
	private static Triangle[] result;

	private boolean hasVertexColors = false;

	/* Vertex indices of the source triangle. */
	private int[] idx;

	private final int[] colors = new int[3];

	/* 1/w of each vertex after projection, for perspective-correct texture mapping. */
	private final float[] invW = new float[] { 1f, 1f, 1f };

	private final float[] v = new float[12];
		// xA, yA, zA, wA,
		// xB, yB, zB, wB,
		// xC, yC, zC, wC;
		// 0   1   2   3

	private final float[][] t = new float[Graphics3D.NUM_TEXTURE_UNITS][12];
		// For each texture unit:
		// [sA, tA, rA, qA,
		// sB, tB, rB, qB,
		// sC, tC, rC, qC];
		// 0   1   2   3

	Triangle() { }

	public static final Triangle[] fromVertAndTris(float[] vert, float[][] texc, int[] tris, int[] renderableTriangles,
		float near, int cullingMode, VertexBuffer vertices, boolean polygonClockwise, boolean perspectiveCorrect)
	{
		renderableTriangles[0] = 0;
		final int totalTris = tris.length / 3;
		boolean hasTex = false;
		for(int i = 0; i < Graphics3D.NUM_TEXTURE_UNITS; i++)
		{
			if(texc[i] != null) { hasTex = true; break; }
		}

		// Only allocate a new triangle array if it doesn't exist, or cannot fit the incoming mesh.
		// Near-plane clipping can split a crossing triangle into two, hence the `* 2`, as
		// the worst case here is a single triangle that takes the whole screen and is clipped to 2.
		if(Triangle.result == null || totalTris * 2 > Triangle.result.length)
		{
			// Let's start off by copying the references of the old array to the
			// new one. Saves having to reallocate all objects again whenever
			// the size increases, as we can just reuse the same references.
			final int oldLen = (Triangle.result == null) ? 0 : Triangle.result.length;

			Triangle[] newRef = new Triangle[totalTris * 2];
			if (oldLen > 0) { System.arraycopy(Triangle.result, 0, newRef, 0, oldLen); }

			for (int i = oldLen; i < totalTris * 2; i++) {newRef[i] = new Triangle(); }
			Triangle.result = newRef;
		}

		for (int tri_id = 0; tri_id < tris.length / 3; tri_id++)
		{
			for (int i = 0; i < 3; i++)
			{
				final int idx = 4 * tris[3 * tri_id + i];
				Triangle.inV[4*i]   = vert[idx];     Triangle.inV[4*i+1] = vert[idx + 1];
				Triangle.inV[4*i+2] = vert[idx + 2]; Triangle.inV[4*i+3] = vert[idx + 3];

				for (int u = 0; u < Graphics3D.NUM_TEXTURE_UNITS; u++)
                {
                    if (texc[u] != null)
                    {
                        Triangle.inT[u][4*i]   = texc[u][idx];     Triangle.inT[u][4*i+1] = texc[u][idx + 1];
                        Triangle.inT[u][4*i+2] = texc[u][idx + 2]; Triangle.inT[u][4*i+3] = texc[u][idx + 3];
                    }
                }
			}

			/*
			 * Clip against the near plane (w >= near) in clip space, interpolating both
			 * positions, texture coordinates and vertex colors. Vertices behind the
			 * camera would otherwise explode to huge coordinates after perspective division.
			 */
			final int outCount = clipNearPlane(Triangle.inV, Triangle.inT, Triangle.inC, tris, tri_id,
				vertices, hasTex, texc, near, Triangle.outV, Triangle.outT, Triangle.outC);

			if (outCount < 3) { continue; }

			/* Triangulate the resulting polygon (3 or 4 vertices) as a fan. */
			for (int fan = 0; fan + 2 < outCount; fan++)
			{
				final Triangle tri = Triangle.result[renderableTriangles[0]];
				tri.setVertexCoords(Triangle.outV, fan);
				tri.setTexCoords(Triangle.outT, fan);
				tri.setVertexColors(vertices.getColors() == null ? null : Triangle.outC, fan);
				tri.setVertexIndices(tris);

				final boolean isFrontFace = polygonClockwise ? !tri.isCounterClockwise() : tri.isCounterClockwise();

				final boolean cullTriangle = (cullingMode == PolygonMode.CULL_BACK && !isFrontFace) ||
							 (cullingMode == PolygonMode.CULL_FRONT && isFrontFace);

				if (cullTriangle) { continue; }

				tri.project(perspectiveCorrect);

				if (tri.outsideFrustum()) { continue; }

				Triangle.result[renderableTriangles[0]] = tri;
				renderableTriangles[0]++;
			}
		}

		return Triangle.result;
	}

	/*
	 * Sutherland-Hodgman clip of one triangle against the near plane (w >= near).
	 * Writes the resulting polygon (0, 3 or 4 vertices) into outV/outT and returns
	 * its vertex count. Positions, texture coordinates and vertex colors
	 * interpolate linearly in clip space, which is exact for all.
	 */
	private static int clipNearPlane(float[] inV, float[][] inT, int[] inC, int[] indices, int tri_id,
		VertexBuffer vertices, boolean hasTex, float texc[][], float near, float[] outV, float[][] outT, int[] outC)
	{
		int outCount = 0;

		// Do we have vertex colors? If so, prep them here
		if (vertices.getColors() != null)
		{
			for (int i = 0; i < 3; i++)
			{
				vertices.getColors().get(indices[3 * tri_id + i], 1, Triangle.COLOR_VERTEX);
				inC[i] = (vertices.getColors().getComponentCount() == 3) ?
					(0xFF << 24) | (Byte.toUnsignedInt(Triangle.COLOR_VERTEX[0]) << 16) |
					(Byte.toUnsignedInt(Triangle.COLOR_VERTEX[1]) << 8) |
					Byte.toUnsignedInt(Triangle.COLOR_VERTEX[2]) :
					(Byte.toUnsignedInt(Triangle.COLOR_VERTEX[3]) << 24) |
					(Byte.toUnsignedInt(Triangle.COLOR_VERTEX[0]) << 16) |
					(Byte.toUnsignedInt(Triangle.COLOR_VERTEX[1]) << 8) |
					Byte.toUnsignedInt(Triangle.COLOR_VERTEX[2]);
			}
		}

		for (int i = 0; i < 3; i++)
		{
			final int j = (i + 1) % 3;
			final float wi = inV[4*i+3], wj = inV[4*j+3];
			final boolean insideI = wi >= near, insideJ = wj >= near;

			if (insideI)
			{
				System.arraycopy(inV, 4*i, outV, 4*outCount, 4);
				if(hasTex)
				{
					for (int u = 0; u < Graphics3D.NUM_TEXTURE_UNITS; u++)
	                {
	                    if (texc[u] != null) { System.arraycopy(inT[u], 4*i, outT[u], 4*outCount, 4); }
	                }
				}

				if (inC != null) { outC[outCount] = inC[i]; }
				outCount++;
			}
			if (insideI != insideJ)
			{
				final float amt = (near - wi) / (wj - wi);
				for (int c = 0; c < 4; c++)
				{
					outV[4*outCount + c] = inV[4*i + c] + amt * (inV[4*j + c] - inV[4*i + c]);
					if (hasTex)
					{
						for (int u = 0; u < Graphics3D.NUM_TEXTURE_UNITS; u++)
	                    {
	                        if (texc[u] != null)
	                        {
	                            outT[u][4*outCount + c] = inT[u][4*i + c] + amt * (inT[u][4*j + c] - inT[u][4*i + c]);
	                        }
	                    }
					}
				}

				if (inC != null)
				{
					final int cA = inC[i], cB = inC[j];
					final int alpha = (int) (amt * 256f);

					final int rbA = cA & 0x00FF00FF, rbB = cB & 0x00FF00FF;
					final int agA = (cA >>> 8) & 0x00FF00FF, agB = (cB >>> 8) & 0x00FF00FF;

					final int rb = (rbA + (((rbB - rbA) * alpha) >> 8)) & 0x00FF00FF;
                	final int ag = (agA + (((agB - agA) * alpha) >> 8)) & 0x00FF00FF;

					outC[outCount] = rb | (ag << 8);
				}
				outCount++;
			}
		}
		return outCount;
	}

	public final boolean outsideFrustum()
	{
		return (v[0] < -1f && v[4] < -1f && v[8] < -1f) ||
			(v[0] >  1f && v[4] >  1f && v[8] >  1f) ||
			(v[1] < -1f && v[5] < -1f && v[9] < -1f) ||
			(v[1] >  1f && v[5] >  1f && v[9] >  1f) ||
			(v[2] < -1f && v[6] < -1f && v[10] < -1f) ||
			(v[2] >  1f && v[6] >  1f && v[10] >  1f);
	}

	public static final void transform(Triangle[] triangles, int visibleTris, Transform trVert, Transform[] trTex)
	{
		for (int i = 0; i < visibleTris; i++)
		{
			trVert.transform(triangles[i].v);

			for(int u = 0; u < Graphics3D.NUM_TEXTURE_UNITS; u++)
			{
				if (trTex != null)
	            {
	                // Each trTex transform is bound to a texture unit, so it is
					// safe to use it as a check to see if we have these coords.
	                trTex[u].transform(triangles[i].t[u]);
	            }
			}
		}
	}

	public final void project(boolean perspectiveCorrect)
	{
		// Apply perspective division to the triangle, it's going to NDC
		for (int i = 0; i < 3; i++)
		{
			final float w = v[4 * i + 3];

			/* Keep 1/w around: the rasterizer interpolates s/w, t/w and 1/w linearly in
			 * screen space and divides per-pixel for perspective-correct texturing. */
			invW[i] = (w > M3GMath.EPSILON) ? (1f / w) : 1f;

			// Project vertex
			v[4 * i + 0] /= w; // x / w
			v[4 * i + 1] /= w; // y / w
			v[4 * i + 2] /= w; // z / w
			v[4 * i + 3] = 1f;  // Set w to 1

			// Texture coordinates are stored as s/w and t/w if
			// perspective correction is enabled (undone per-pixel in rasterizer)
			if (perspectiveCorrect)
			{
				for (int u = 0; u < Graphics3D.NUM_TEXTURE_UNITS; u++)
                {
                	if(t[u] == null) { continue; }
                    t[u][4 * i + 0] *= invW[i]; // s / w
                    t[u][4 * i + 1] *= invW[i]; // t / w
                }
			}
		}
	}

	public final boolean isCounterClockwise()
	{
		float ax = v[0], ay = v[1], aw = v[3];
		float bx = v[4], by = v[5], bw = v[7];
		float cx = v[8], cy = v[9], cw = v[11];

		// Usually counterClockWise would be <= 0.0, but we're in Clip space
		// here where Y is the inverse of NDC, so invert to > 0.0;
		return ((bx * aw - ax * bw) * (cy * aw - ay * cw) -
			(by * aw - ay * bw) * (cx * aw - ax * cw)) > 0.0f;
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

	public final float sA(int unit) { return t[unit][4 * 0 + 0]; }
	public final float tA(int unit) { return t[unit][4 * 0 + 1]; }
	public final float rA(int unit) { return t[unit][4 * 0 + 2]; }
	public final float qA(int unit) { return t[unit][4 * 0 + 3]; }
	public final float sB(int unit) { return t[unit][4 * 1 + 0]; }
	public final float tB(int unit) { return t[unit][4 * 1 + 1]; }
	public final float rB(int unit) { return t[unit][4 * 1 + 2]; }
	public final float qB(int unit) { return t[unit][4 * 1 + 3]; }
	public final float sC(int unit) { return t[unit][4 * 2 + 0]; }
	public final float tC(int unit) { return t[unit][4 * 2 + 1]; }
	public final float rC(int unit) { return t[unit][4 * 2 + 2]; }
	public final float qC(int unit) { return t[unit][4 * 2 + 3]; }

	public final float iwA() { return invW[0]; }
	public final float iwB() { return invW[1]; }
	public final float iwC() { return invW[2]; }

	public final int colorA() { return colors[0]; }
	public final int colorB() { return colors[1]; }
	public final int colorC() { return colors[2]; }

	public final int getIndex(int index) { return idx[index]; }

	// This one is for memory reuse, so `this.t` is expected to be allocated by now.
	public final void setTexCoords(float[][] tCoords, int fan)
	{
		final int f1 = 4 * (fan + 1);
		final int f2 = 4 * (fan + 2);

		for (int i = 0; i < Graphics3D.NUM_TEXTURE_UNITS; i++)
        {
            if (tCoords[i] == null) { continue; }
            System.arraycopy(tCoords[i], 0,  t[i], 0, 4);
            System.arraycopy(tCoords[i], f1, t[i], 4, 4);
            System.arraycopy(tCoords[i], f2, t[i], 8, 4);
        }
	}

	// This one is also for memory reuse, so `this.v` is expected to be allocated by now.
	public final void setVertexCoords(float[] vCoords, int fan)
	{
		final int f1 = 4 * (fan + 1);
		final int f2 = 4 * (fan + 2);

		System.arraycopy(vCoords, 0,  v, 0, 4);
		System.arraycopy(vCoords, f1, v, 4, 4);
		System.arraycopy(vCoords, f2, v, 8, 4);
	}

	// This one is also for memory reuse, so `this.colors` is expected to be allocated by now.
	public final void setVertexColors(int[] vColors, int fan)
	{
		this.hasVertexColors = (vColors != null);
		if (vColors == null) { return; }
		this.colors[0] = vColors[0];
		this.colors[1] = vColors[fan + 1];
		this.colors[2] = vColors[fan + 2];
	}

	// This one is also for memory reuse, so `this.idx` is expected to be allocated by now.
	public final void setVertexIndices(int[] indices) { this.idx = indices; }

	public final boolean hasVertexColors() { return this.hasVertexColors; }
}
