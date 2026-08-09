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

	private boolean hasVertexColors = false;

	final int[] colors = new int[3];

	/* 1/w of each vertex after projection, for perspective-correct texture mapping. */
	final float[] invW = new float[] { 1f, 1f, 1f };

	/* Effective vertex indices of the source triangle (after strip-winding correction). */
	private int idxA, idxB, idxC;

	Triangle(float[] vertices, int ia, int ib, int ic)
	{
		v = vertices;
		idxA = ia; idxB = ib; idxC = ic;
	}

	public static final Triangle[] fromVertAndTris(float[] vert, float[] texc, int[] tris, int[] renderableTriangles, float near, int cullingMode, VertexBuffer vertices)
	{
		renderableTriangles[0] = 0;
		boolean sharesVertices = false;
		/* Near-plane clipping can split a crossing triangle into two. */
		final Triangle[] result = new Triangle[(tris.length / 3) * 2];

		final float[] inV = new float[12];
		final float[] inT = new float[12];
		final float[] outV = new float[16]; /* Up to 4 vertices after clipping against one plane */
		final float[] outT = new float[16];

		int prevA = 0, prevB = 0, prevC = 0; /* effective indices of the previous triangle */
		for (int tri_id = 0; tri_id < tris.length / 3; tri_id++)
		{
			/*
			 * Triangle-strip winding correction: consecutive triangles sharing an edge
			 * alternate their winding, so swap two indices to normalize it. The swap is
			 * done on LOCAL copies, comparing against the previous triangle's EFFECTIVE
			 * (post-swap) indices — writing the swap back into the IndexBuffer's array
			 * would corrupt the mesh cumulatively across renders (the detection would
			 * then run on already-swapped data, flipping different triangles each time).
			 */
			int ia = tris[3 * tri_id + 0], ib = tris[3 * tri_id + 1], ic = tris[3 * tri_id + 2];
			if (tri_id > 0)
			{
				sharesVertices = (ia == prevB && ib == prevC) || (ib == prevA && ic == prevB);

				if (sharesVertices)
				{
					final int temp = ia;
					ia = ib;
					ib = temp;
				}
			}
			prevA = ia; prevB = ib; prevC = ic;

			for (int i = 0; i < 3; i++)
			{
				final int idx = 4 * (i == 0 ? ia : (i == 1 ? ib : ic));
				inV[4*i]   = vert[idx];     inV[4*i+1] = vert[idx + 1];
				inV[4*i+2] = vert[idx + 2]; inV[4*i+3] = vert[idx + 3];
				if (texc != null)
				{
					inT[4*i]   = texc[idx];     inT[4*i+1] = texc[idx + 1];
					inT[4*i+2] = texc[idx + 2]; inT[4*i+3] = texc[idx + 3];
				}
			}

			/*
			 * Clip against the near plane (w >= near) in clip space, interpolating both
			 * positions and texture coordinates. Vertices behind the camera would otherwise
			 * explode to huge screen coordinates after the perspective division.
			 */
			final int outCount = clipNearPlane(inV, inT, texc != null, near, outV, outT);
			if (outCount < 3) { continue; }

			/* Triangulate the resulting polygon (3 or 4 vertices) as a fan. */
			for (int fan = 0; fan + 2 < outCount; fan++)
			{
				final Triangle tri = new Triangle(new float[]
				{
					outV[0], outV[1], outV[2], outV[3],
					outV[4*(fan+1)], outV[4*(fan+1)+1], outV[4*(fan+1)+2], outV[4*(fan+1)+3],
					outV[4*(fan+2)], outV[4*(fan+2)+1], outV[4*(fan+2)+2], outV[4*(fan+2)+3]
				}, ia, ib, ic);

				// Perspective division for culling and visibility checks (all w >= near now)
				for (int i = 0; i < 3; i++)
				{
					tri.v[i * 4 + 0] /= tri.v[i * 4 + 3];
					tri.v[i * 4 + 1] /= tri.v[i * 4 + 3];
					tri.v[i * 4 + 2] /= tri.v[i * 4 + 3];
				}

				final boolean cullTriangle = (cullingMode == PolygonMode.CULL_BACK && tri.isCounterClockwise()) ||
									(cullingMode == PolygonMode.CULL_FRONT && !tri.isCounterClockwise());

				if (cullTriangle) { continue; }
				if (tri.clip()) { continue; }

				// We now have to restore the renderable geometry back to its original coordinates, otherwise rendering will be broken
				for (int i = 0; i < 3; i++)
				{
					tri.v[i * 4 + 0] *= tri.v[i * 4 + 3];
					tri.v[i * 4 + 1] *= tri.v[i * 4 + 3];
					tri.v[i * 4 + 2] *= tri.v[i * 4 + 3];
				}

				tri.setTexCoords(texc == null ? null : new float[]
				{
					outT[0], outT[1], outT[2], outT[3],
					outT[4*(fan+1)], outT[4*(fan+1)+1], outT[4*(fan+1)+2], outT[4*(fan+1)+3],
					outT[4*(fan+2)], outT[4*(fan+2)+1], outT[4*(fan+2)+2], outT[4*(fan+2)+3]
				});

				tri.setVertexColors(vertices);

				tri.project();
				result[renderableTriangles[0]] = tri;
				renderableTriangles[0]++;
			}
		}

		return result;
	}

	/*
	 * Sutherland-Hodgman clip of one triangle against the near plane (w >= near).
	 * Writes the resulting polygon (0, 3 or 4 vertices) into outV/outT and returns
	 * its vertex count. Positions and texture coordinates interpolate linearly in
	 * clip space, which is exact for both.
	 */
	private static int clipNearPlane(float[] inV, float[] inT, boolean hasTex, float near, float[] outV, float[] outT)
	{
		int outCount = 0;
		for (int i = 0; i < 3; i++)
		{
			final int j = (i + 1) % 3;
			final float wi = inV[4*i+3], wj = inV[4*j+3];
			final boolean insideI = wi >= near, insideJ = wj >= near;

			if (insideI)
			{
				System.arraycopy(inV, 4*i, outV, 4*outCount, 4);
				if (hasTex) { System.arraycopy(inT, 4*i, outT, 4*outCount, 4); }
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
				outCount++;
			}
		}
		return outCount;
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

	public final int getIndex(int index) { return index == 0 ? idxA : (index == 1 ? idxB : idxC); }

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
			final float w = v[4 * i + 3];

			/* Keep 1/w around: the rasterizer interpolates s/w, t/w and 1/w linearly in
			 * screen space and divides per-pixel for perspective-correct texturing. */
			invW[i] = (w > M3GMath.EPSILON) ? (1f / w) : 1f;

			// Project vertex
			v[4 * i + 0] /= w; // x / w
			v[4 * i + 1] /= w; // y / w
			v[4 * i + 2] /= w; // z / w
			v[4 * i + 3] = 1f;  // Set w to 1

			// Texture coordinates are stored as s/w and t/w (undone per-pixel in the rasterizer)
			if (t != null)
			{
				t[4 * i + 0] *= invW[i]; // s / w
				t[4 * i + 1] *= invW[i]; // t / w
			}
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

	public final void setTexCoords(float[] texCoords)
	{
		if (texCoords != null && texCoords.length != 12)
		{
			throw new IllegalArgumentException("Each vertex texture coordinate must have 4 elements (s, t, r, q).");
		}

		this.t = texCoords;
	}

    public final void setVertexColors(VertexBuffer vertices)
	{
		if (vertices.getColors() != null)
		{
			final byte[] color_vertex = new byte[4];

			for (int i = 0; i < 3; i++)
			{
				vertices.getColors().get(this.getIndex(i), 1, color_vertex);
				colors[i] = (vertices.getColors().getComponentCount() == 3)
					? (0xFF << 24) | (Byte.toUnsignedInt(color_vertex[0]) << 16) |
					(Byte.toUnsignedInt(color_vertex[1]) << 8) | Byte.toUnsignedInt(color_vertex[2])
					: (Byte.toUnsignedInt(color_vertex[3]) << 24) |
					(Byte.toUnsignedInt(color_vertex[0]) << 16) |
					(Byte.toUnsignedInt(color_vertex[1]) << 8) | Byte.toUnsignedInt(color_vertex[2]);
			}

			this.hasVertexColors = true;
		}
	}

	public final boolean hasVertexColors() { return this.hasVertexColors; }

	public final boolean isCounterClockwise()
	{
		return (xB() - xA()) * (yC() - yA()) - (xC() - xA()) * (yB() - yA()) < 0; // Clockwise if normal points towards the viewer
	}
}
