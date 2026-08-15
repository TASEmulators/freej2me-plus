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
	private static final float[] inT = new float[12];
	private static final float[] outV = new float[16];
	private static final float[] outT = new float[16];

	// Let's reuse this when clipping, quite a bit faster than creating ArrayLists each time
	private static final float[] clipVert = new float[4];

	// Output array of triangles. Allows us to reuse the memory block allocated for triangle
	// data without needing to GC it every render pass (it'll still reallocate if the triangle count increases)
	private static Triangle[] result;

	private boolean hasVertexColors = false;

	/* Triangle id on the indices array. */
	private int triangleID;

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

	private final float[] t = new float[12];
		// sA, tA, rA, qA,
		// sB, tB, rB, qB,
		// sC, tC, rC, qC;
		// 0   1   2   3

	Triangle() { }

	public static final Triangle[] fromVertAndTris(float[] vert, float[] texc, int[] tris, int[] renderableTriangles,
		float near, int cullingMode, VertexBuffer vertices, boolean polygonClockwise, boolean perspectiveCorrect)
	{
		renderableTriangles[0] = 0;
		final int totalTris = tris.length / 3;

		// Only allocate a new triangle array if it doesn't exist, or cannot fit the incoming mesh.
		// Near-plane clipping can split a crossing triangle into two, hence the `* 2`.
		if(Triangle.result == null || totalTris * 2 > Triangle.result.length)
			{ Triangle.result = new Triangle[totalTris * 2]; }

		for (int tri_id = 0; tri_id < tris.length / 3; tri_id++)
		{
			for (int i = 0; i < 3; i++)
			{
				final int idx = 4 * tris[3 * tri_id + i];
				Triangle.inV[4*i]   = vert[idx];     Triangle.inV[4*i+1] = vert[idx + 1];
				Triangle.inV[4*i+2] = vert[idx + 2]; Triangle.inV[4*i+3] = vert[idx + 3];

				if (texc != null)
				{
					Triangle.inT[4*i]   = texc[idx];     Triangle.inT[4*i+1] = texc[idx + 1];
					Triangle.inT[4*i+2] = texc[idx + 2]; Triangle.inT[4*i+3] = texc[idx + 3];
				}
			}

			/*
			 * Clip against the near plane (w >= near) in clip space, interpolating both
			 * positions, texture coordinates and vertex colors. Vertices behind the
			 * camera would otherwise explode to huge coordinates after perspective division.
			 */
			final int outCount = clipNearPlane(Triangle.inV, Triangle.inT, Triangle.inC, tris, tri_id, vertices,
				texc != null, near, Triangle.outV, Triangle.outT, Triangle.outC);

			if (outCount < 3) { continue; }

			/* Triangulate the resulting polygon (3 or 4 vertices) as a fan. */
			for (int fan = 0; fan + 2 < outCount; fan++)
			{
				// Create a new triangle if we don't already have one available on the static array.
				// This saves a bunch of temporary memory allocations once the array is built and no longer increases
				// in size.
				if(Triangle.result[renderableTriangles[0]] == null)
					{ Triangle.result[renderableTriangles[0]] = new Triangle(); }

				final Triangle tri = Triangle.result[renderableTriangles[0]];
				tri.setVertexCoords(Triangle.outV, fan);
				tri.setTexCoords(texc == null ? null : Triangle.outT, fan);
				tri.setVertexColors(vertices.getColors() == null ? null : Triangle.outC, fan);
				tri.setVertexIndices(tris, tri_id);

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
	private static int clipNearPlane(float[] inV, float[] inT, int[] inC, int[] indices, int tri_id,
		VertexBuffer vertices, boolean hasTex, float near, float[] outV, float[] outT, int[] outC)
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
				if (hasTex) { System.arraycopy(inT, 4*i, outT, 4*outCount, 4); }
				if (inC != null) { outC[outCount] = inC[i]; }
				outCount++;
			}
			if (insideI != insideJ)
			{
				final float amt = (near - wi) / (wj - wi);
				for (int c = 0; c < 4; c++)
				{
					outV[4*outCount + c] = inV[4*i + c] + amt * (inV[4*j + c] - inV[4*i + c]);
					if (hasTex) { outT[4*outCount + c] = inT[4*i + c] + amt * (inT[4*j + c] - inT[4*i + c]); }
				}

				if (inC != null)
				{
					final int cA = inC[i];
				    final int cB = inC[j];

				    final int alpha = (int) (amt * 256f);

				    final int rbA = cA & 0x00FF00FF;
				    final int rbB = cB & 0x00FF00FF;

				    final int agA = (cA >>> 8) & 0x00FF00FF;
				    final int agB = (cB >>> 8) & 0x00FF00FF;

				    final int rb = rbA + (((rbB - rbA) * alpha) >> 8) & 0x00FF00FF;
				    final int ag = agA + (((agB - agA) * alpha) >> 8) & 0x00FF00FF;

				    outC[outCount] = (ag << 8) | rb;
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

	public static final void transform(Triangle[] triangles, int visibleTris, Transform trVert, Transform trTex)
	{
		for (int i = 0; i < visibleTris; i++)
		{
			trVert.transform(triangles[i].v);
			if (triangles[i].t != null && trTex != null) { trTex.transform(triangles[i].t); }
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
			if (t != null && perspectiveCorrect)
			{
				t[4 * i + 0] *= invW[i]; // s / w
				t[4 * i + 1] *= invW[i]; // t / w
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

	public final float iwA() { return invW[0]; }
	public final float iwB() { return invW[1]; }
	public final float iwC() { return invW[2]; }

	public final int colorA() { return colors[0]; }
	public final int colorB() { return colors[1]; }
	public final int colorC() { return colors[2]; }

	public final int getIndex(int index) { return idx[index]; }

	// This one is for memory reuse, so `this.t` is expected to be allocated by now.
	public final void setTexCoords(float[] tCoords, int fan)
	{
		if (tCoords == null) { return; }

		final int f1 = 4 * (fan + 1);
		final int f2 = 4 * (fan + 2);

		System.arraycopy(tCoords, 0,  t, 0, 4);
		System.arraycopy(tCoords, f1, t, 4, 4);
		System.arraycopy(tCoords, f2, t, 8, 4);
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
	public final void setVertexIndices(int[] indices, int tri_id)
	{
		this.idx = indices;
		this.triangleID = tri_id;
	}

	public final boolean hasVertexColors() { return this.hasVertexColors; }
}
