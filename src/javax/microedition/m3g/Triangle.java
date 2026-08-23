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

import java.util.ArrayList;

class Triangle
{
	// Temporary buffer for vertex colors
	private static final byte[] COLOR_VERTEX = new byte[4];

	// Temporary buffer for normals and lighting calculations
	private static final byte[] B_NORM  = new byte[3];
	private static final short[] S_NORM = new short[3];
	private static final float[] N_EYE = new float[4];
	private static final float[] V_EYE = new float[4];
	private static final float[] L_MAT = new float[16];

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

	// Used for sorting triangles front-to-back
	private float sortZ;

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

	public static final Triangle[] fromVertAndTris(
		// Position and texture vertex data
		float[] vert, float[][] texc,
		// Material and shading
		Material material, int shadingMode, boolean twoSide, boolean localCameraLight,
		// Normal data
		float[] eyePos, VertexArray vertNorms, Transform normalMatrix,
		// Lights
		ArrayList<Light> lights, float[] lightEyePos, float[] lightEyeDir,
		// IndexArray, clipping, winding order and perspectiveCorrection
		int[] tris, int[] renderableTriangles, int cullingMode, VertexBuffer vertices,
        boolean polygonClockwise, boolean perspectiveCorrect)
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

			// Do we have vertex colors? If so, prep them here
			if (vertices.getColors() != null)
			{
				for (int i = 0; i < 3; i++)
				{
					vertices.getColors().get(tris[3 * tri_id + i], 1, Triangle.COLOR_VERTEX);
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
			else
			{
				inC[0] = vertices.getDefaultColor();
				inC[1] = vertices.getDefaultColor();
				inC[2] = vertices.getDefaultColor();
			}

			// Is the app using lights? Then calculate per-vertex lighting.
			boolean hasLighting = (vertNorms != null && material != null &&
				lights != null && !lights.isEmpty());
			if (hasLighting)
			{
				calculateLighting(eyePos, vertNorms, normalMatrix, material, shadingMode, twoSide,
					localCameraLight, lights, lightEyePos, lightEyeDir, tris, tri_id, Triangle.inC);
			}

			/*
             * Clip against the homogeneous near plane (z >= -w), interpolating
             * positions, texture coordinates and vertex colors before perspective division.
			 */
            final int outCount = clipNearPlane(Triangle.inV, Triangle.inT, Triangle.inC,
                    hasTex, texc, Triangle.outV, Triangle.outT, Triangle.outC);

			if (outCount < 3) { continue; }

			/* Triangulate the resulting polygon (3 or 4 vertices) as a fan. */
			for (int fan = 0; fan + 2 < outCount; fan++)
			{
				final Triangle tri = Triangle.result[renderableTriangles[0]];
				tri.setVertexCoords(Triangle.outV, fan);

				final boolean isFrontFace = polygonClockwise ? !tri.isCounterClockwise() : tri.isCounterClockwise();

				final boolean cullTriangle = (cullingMode == PolygonMode.CULL_BACK && !isFrontFace) ||
							 (cullingMode == PolygonMode.CULL_FRONT && isFrontFace);

				if (cullTriangle) { continue; }

				tri.setTexCoords(Triangle.outT, fan);
				boolean hasColors = hasLighting || (vertices.getColors() != null);
				tri.setVertexColors(hasColors ? Triangle.outC : null, fan);

				// Calculate the average Z for front-to-back sorting.
				tri.sortZ = (tri.v[2] + tri.v[6] + tri.v[10]) * 0.33333334f;

				tri.project(perspectiveCorrect);

				if (tri.outsideFrustum()) { continue; }

				Triangle.result[renderableTriangles[0]] = tri;
				renderableTriangles[0]++;
			}
		}

		return sortFrontToBack(Triangle.result, renderableTriangles[0]);
	}

	private static final void calculateLighting(
		float[] eyePos, VertexArray vertNorms, Transform normalMatrix,
		Material material, int shadingMode, boolean twoSided, boolean localCameraLight,
		ArrayList<Light> lights, float[] lightEyePos, float[] lightEyeDir,
		int[] tris, int tri_id, int[] outColors)
	{
		// Material Colors
		int matAmbient  = material.getColor(Material.AMBIENT);
		int matDiffuse  = material.getColor(Material.DIFFUSE);
		int matSpecular = material.getColor(Material.SPECULAR);
		int matEmissive = material.getColor(Material.EMISSIVE);
		float shininess = material.getShininess();

		float maR = ((matAmbient >> 16) & 0xFF) / 255.0f, maG = ((matAmbient >> 8) & 0xFF) / 255.0f, maB = (matAmbient & 0xFF) / 255.0f;
		float mdR = ((matDiffuse >> 16) & 0xFF) / 255.0f, mdG = ((matDiffuse >> 8) & 0xFF) / 255.0f, mdB = (matDiffuse & 0xFF) / 255.0f;
		float msR = ((matSpecular >> 16) & 0xFF) / 255.0f, msG = ((matSpecular >> 8) & 0xFF) / 255.0f, msB = (matSpecular & 0xFF) / 255.0f;
		float meR = ((matEmissive >> 16) & 0xFF) / 255.0f, meG = ((matEmissive >> 8) & 0xFF) / 255.0f, meB = (matEmissive & 0xFF) / 255.0f;
		int alpha = (matDiffuse >>> 24);

		boolean vertColorTrackingEnabled = material.isVertexColorTrackingEnabled();

		// Cache the normal matrix into a local reference.
		normalMatrix.get(L_MAT);

		// Flat Shading? We calculate only vertex 2 (C) and copy to others
		int firstVertex = (shadingMode == PolygonMode.SHADE_FLAT) ? 2 : 0;
		for (int v = firstVertex; v <= 2; v++)
		{
			int vertIndex = tris[3 * tri_id + v];

			// Vertex color tracking is enabled? Then the vertex colors replace
			// the material's diffuse and ambient ones.
			if (vertColorTrackingEnabled)
			{
				int vertColor = outColors[v];
				alpha = (vertColor >>> 24);

				final float vR = ((vertColor >> 16) & 0xFF) / 255.0f;
				final float vG = ((vertColor >> 8)  & 0xFF) / 255.0f;
				final float vB = (vertColor         & 0xFF) / 255.0f;

				mdR = vR; mdG = vG; mdB = vB;
				maR = vR; maG = vG; maB = vB;
			}

			// Normals may be stored as either short or byte
			if (vertNorms.getComponentType() == 1)
			{
				vertNorms.get(vertIndex, 1, B_NORM);
				N_EYE[0] = B_NORM[0] / 127.0f;
				N_EYE[1] = B_NORM[1] / 127.0f;
				N_EYE[2] = B_NORM[2] / 127.0f;
			}
			else
			{
				vertNorms.get(vertIndex, 1, S_NORM);
				N_EYE[0] = S_NORM[0] / 32767.0f;
				N_EYE[1] = S_NORM[1] / 32767.0f;
				N_EYE[2] = S_NORM[2] / 32767.0f;
			}

			// Vertex normals must now be multiplied by the normal matrix to
			// reach eye space.
			float nx = N_EYE[0], ny = N_EYE[1], nz = N_EYE[2];
			N_EYE[0] = L_MAT[0] * nx + L_MAT[1] * ny + L_MAT[2] * nz;
			N_EYE[1] = L_MAT[4] * nx + L_MAT[5] * ny + L_MAT[6] * nz;
			N_EYE[2] = L_MAT[8] * nx + L_MAT[9] * ny + L_MAT[10] * nz;

			M3GMath.normalize(N_EYE);

			V_EYE[0] = eyePos[vertIndex * 4];
			V_EYE[1] = eyePos[vertIndex * 4 + 1];
			V_EYE[2] = eyePos[vertIndex * 4 + 2];

			// Emission color is our base here.
			float r = meR, g = meG, b = meB;

			float viewX, viewY, viewZ;

			if(localCameraLight)
			{
				viewX = -V_EYE[0];
				viewY = -V_EYE[1];
				viewZ = -V_EYE[2];
				float viewLen = M3GMath.sqrt(viewX * viewX + viewY * viewY + viewZ * viewZ);
				if (viewLen > M3GMath.EPSILON) { viewX /= viewLen; viewY /= viewLen; viewZ /= viewLen; }
			}
			else
			{
				viewX = 0.0f;
				viewY = 0.0f;
				viewZ = 1.0f;
			}

			for (int l = 0; l < lights.size(); l++)
			{
				Light light = lights.get(l);
				int lMode = light.getMode();
				float lIntensity = light.getIntensity();

				int lColor = light.getColor();
				float lR = (((lColor >> 16) & 0xFF) / 255.0f) * lIntensity;
				float lG = (((lColor >> 8) & 0xFF)  / 255.0f) * lIntensity;
				float lB = ((lColor & 0xFF)         / 255.0f) * lIntensity;

				// Ambient Lights only affect the material's ambient according to M3G.
				if (lMode == Light.AMBIENT)
				{
					r += maR * lR;
					g += maG * lG;
					b += maB * lB;
					continue; // Skip diffuse and specular entirely on this light.
				}

				// Now for directional, omni or spot lights, we calculate diffuse and specular,
				// so we need their direction and attenuation.
				float lightDirX, lightDirY, lightDirZ;
				float attenuation = 1.0f;

				if (lMode == Light.DIRECTIONAL)
				{
					lightDirX = -lightEyeDir[l * 4];
					lightDirY = -lightEyeDir[l * 4 + 1];
					lightDirZ = -lightEyeDir[l * 4 + 2];

					float lLen = M3GMath.sqrt(lightDirX * lightDirX + lightDirY * lightDirY + lightDirZ * lightDirZ);
					if (lLen > M3GMath.EPSILON) { lightDirX /= lLen; lightDirY /= lLen; lightDirZ /= lLen; }
				}
				else
				{
					// Positional lights use distance attenuation
					float lx = lightEyePos[l * 4] - V_EYE[0];
					float ly = lightEyePos[l * 4 + 1] - V_EYE[1];
					float lz = lightEyePos[l * 4 + 2] - V_EYE[2];
					float dist = M3GMath.sqrt(lx * lx + ly * ly + lz * lz);

					if (dist > M3GMath.EPSILON) { lightDirX = lx / dist; lightDirY = ly / dist; lightDirZ = lz / dist; }
					else { lightDirX = 0; lightDirY = 0; lightDirZ = 1; }

					attenuation = M3GMath.fastReciprocal(light.getConstantAttenuation() +
						light.getLinearAttenuation() * dist +
						light.getQuadraticAttenuation() * dist * dist);

					// Additional directional cone attenuation for SPOT lights
					if (lMode == Light.SPOT)
					{
						float sdX = lightEyeDir[l * 4];
						float sdY = lightEyeDir[l * 4 + 1];
						float sdZ = lightEyeDir[l * 4 + 2];

						float spotDot = (lightDirX * sdX + lightDirY * sdY + lightDirZ * sdZ);
						float cutoffCos = M3GMath.cos(M3GMath.toRadians(light.getSpotAngle()));

						if (spotDot >= cutoffCos)
						{
							attenuation *= (float) Math.pow(spotDot, light.getSpotExponent());
						}
						else { attenuation = 0.0f; }
					}
				}

				if (attenuation <= 0.0f) { continue; }

				// Calculate Dot Product between the normal and light (N . L)
				float nDotL = N_EYE[0] * lightDirX + N_EYE[1] * lightDirY + N_EYE[2] * lightDirZ;

				// Handle Two-Sided Materials by flipping normals. TODO: UNTESTED!
				if (twoSided && nDotL < 0.0f)
				{
					nDotL = -nDotL;
					nx = -nx; ny = -ny; nz = -nz;
				}

				if (nDotL > 0.0f)
				{
					// Diffuse lighting
					float diffFactor = nDotL * attenuation;
					r += mdR * lR * diffFactor;
					g += mdG * lG * diffFactor;
					b += mdB * lB * diffFactor;

					// Specular lighting (Gouraud, since we do it per-vertex)
					float hX = lightDirX + viewX, hY = lightDirY + viewY, hZ = lightDirZ + viewZ;
					float hLen = M3GMath.sqrt(hX * hX + hY * hY + hZ * hZ);

					if (hLen > M3GMath.EPSILON)
					{
						hX /= hLen; hY /= hLen; hZ /= hLen;
						float nDotH = N_EYE[0] * hX + N_EYE[1] * hY + N_EYE[2] * hZ;
						if (twoSided && nDotH < 0.0f) { nDotH = -nDotH; }

						if (nDotH > 0.0f)
						{
							float specFactor = (float) Math.pow(nDotH, shininess) * attenuation;
							r += msR * lR * specFactor;
							g += msG * lG * specFactor;
							b += msB * lB * specFactor;
						}
					}
				}
			}

			// We now have the final color for the vertex
			int ir = (int) (M3GMath.min(1.0f, r) * 255.0f);
			int ig = (int) (M3GMath.min(1.0f, g) * 255.0f);
			int ib = (int) (M3GMath.min(1.0f, b) * 255.0f);
			int color = (alpha << 24) | (ir << 16) | (ig << 8) | ib;

			outColors[v] = color;

			// On flat shading we just apply vertex 2's color to the others.
			if (shadingMode == PolygonMode.SHADE_FLAT)
			{
				outColors[0] = color;
				outColors[1] = color;
				break;
			}
		}
	}

	/*
     * Sutherland-Hodgman clip of one triangle against the homogeneous near plane
     * z + w >= 0. This is valid for perspective, parallel and generic projection
     * matrices; camera-space distances are not available for a generic matrix.
	 * Writes the resulting polygon (0, 3 or 4 vertices) into outV/outT and returns
	 * its vertex count. Positions, texture coordinates and vertex colors
	 * interpolate linearly in clip space, which is exact for all.
	 */
    private static final int clipNearPlane(float[] inV, float[][] inT, int[] inC,
                                           boolean hasTex, float[][] texc, float[] outV, float[][] outT, int[] outC)
	{
		int outCount = 0;

		for (int i = 0; i < 3; i++)
		{
			final int j = (i + 1) % 3;
			final float wi = inV[4*i+3], wj = inV[4*j+3];
            final float distanceI = inV[4*i+2] + wi;
            final float distanceJ = inV[4*j+2] + wj;
            final boolean insideI = distanceI >= 0.0f, insideJ = distanceJ >= 0.0f;

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
                final float amt = distanceI / (distanceI - distanceJ);
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

	private static Triangle[] sortFrontToBack(Triangle[] array, int count)
	{
		// No use trying to sort less than 2 triangles
		if (count < 2) { return array; }

	    // Insertion sort should be good enough for most M3G apps, as triangle
		// count is often very low.
	    for (int i = 1; i < count; i++)
	    {
	        Triangle tri = array[i];
	        int j = i - 1;

	        while (j >= 0 && array[j].sortZ < tri.sortZ)
	        {
	            array[j + 1] = array[j];
	            j--;
	        }
	        array[j + 1] = tri;
	    }

		return array;
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

	public final boolean hasVertexColors() { return this.hasVertexColors; }
}
