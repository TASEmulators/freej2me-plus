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
	// 1.0f / 255.0f, to prevent a bunch of divisions in lighting calculations
	private static final float INVDIV = 0.003921569f;

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

	// 1/w of each vertex after projection, for perspective-correct texturing.
	private final float[] invW = new float[] { 1f, 1f, 1f };

	private final float[] v = new float[12];
		// xA, yA, zA, wA,
		// xB, yB, zB, wB,
		// xC, yC, zC, wC;
		// 0   1   2   3

	private float[][] t = new float[Graphics3D.ACTIVE_TEXTURE_UNITS][6];
		// For each texture unit:
		// [sA, tA,
		// sB, tB,
		// sC, tC,];
		// 0   1
		// We have no use for the `r` and `q` coordinates.

	Triangle() { }

	public static final Triangle[] fromVertAndTris(
		// Position and texture vertex data
		float[] vert, float[][] texc,
		// Material and shading
		Material material, int shadingMode, boolean twoSide, boolean localCameraLight,
		// Normal data
		float[] eyePos, VertexArray vertNorms, Transform normalMatrix,
		// Lights
		ArrayList<Light> lights, float[] lightEyePos, float[] lightEyeDir, int curScope,
		// IndexArray, clipping, winding order and perspectiveCorrection
		int[] tris, int[] renderableTriangles, int cullingMode, VertexBuffer vertices,
		boolean polygonClockwise, boolean perspectiveCorrect)
	{
		renderableTriangles[0] = 0;
		final int totalTris = tris.length / 3;
		boolean hasTex = texc[0] != null;
		// Is the app using lights? Set up to calculate per-vertex lighting.
		boolean hasLighting = (vertNorms != null && material != null &&
			lights != null && !lights.isEmpty());
		boolean hasColors = hasLighting || (vertices.getColors() != null);

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

				for (int u = 0; u < Graphics3D.ACTIVE_TEXTURE_UNITS; u++)
				{
					if (texc[u] != null)
					{
						Triangle.inT[u][4*i]   = texc[u][idx];     Triangle.inT[u][4*i+1] = texc[u][idx + 1];
						Triangle.inT[u][4*i+2] = texc[u][idx + 2]; Triangle.inT[u][4*i+3] = texc[u][idx + 3];
					}
				}
			}

			final boolean isFrontFace = polygonClockwise ? !isCounterClockwise() : isCounterClockwise();

			final boolean cullTriangle = (cullingMode == PolygonMode.CULL_BACK && !isFrontFace) ||
						 (cullingMode == PolygonMode.CULL_FRONT && isFrontFace);

			if (cullTriangle || outsideFrustum()) { continue; }

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

			if (hasLighting)
			{
				calculateLighting(eyePos, vertNorms, normalMatrix, material, shadingMode, twoSide,
					localCameraLight, lights, lightEyePos, lightEyeDir, curScope, tris, tri_id, Triangle.inC);
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
				tri.setTexCoords(Triangle.outT, fan);
				tri.setVertexColors(hasColors ? Triangle.outC : null, fan);

				// Calculate the average Z for front-to-back sorting.
				tri.sortZ = (tri.v[2] + tri.v[6] + tri.v[10]) * 0.33333334f;

				tri.project(perspectiveCorrect);

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
		int curScope, int[] tris, int tri_id, int[] outColors)
	{
		// Material Colors
		int matAmbient  = material.getColor(Material.AMBIENT);
		int matDiffuse  = material.getColor(Material.DIFFUSE);
		int matSpecular = material.getColor(Material.SPECULAR);
		int matEmissive = material.getColor(Material.EMISSIVE);
		float shininess = material.getShininess();

		float maR = ((matAmbient >> 16) & 0xFF) * INVDIV, maG = ((matAmbient >> 8) & 0xFF) * INVDIV, maB = (matAmbient & 0xFF) * INVDIV;
		float mdR = ((matDiffuse >> 16) & 0xFF) * INVDIV, mdG = ((matDiffuse >> 8) & 0xFF) * INVDIV, mdB = (matDiffuse & 0xFF) * INVDIV;
		float msR = ((matSpecular >> 16) & 0xFF) * INVDIV, msG = ((matSpecular >> 8) & 0xFF) * INVDIV, msB = (matSpecular & 0xFF) * INVDIV;
		float meR = ((matEmissive >> 16) & 0xFF) * INVDIV, meG = ((matEmissive >> 8) & 0xFF) * INVDIV, meB = (matEmissive & 0xFF) * INVDIV;
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

				final float vR = ((vertColor >> 16) & 0xFF) * INVDIV;
				final float vG = ((vertColor >> 8)  & 0xFF) * INVDIV;
				final float vB = (vertColor         & 0xFF) * INVDIV;

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

				// Skip lights that aren't set to render or are at a different scope.
				if (!light.isRenderingEnabled() || (light.getScope() & curScope) == 0)
				{
					continue;
				}

				int lMode = light.getMode();
				float lIntensity = light.getIntensity();

				int lColor = light.getColor();
				float lR = (((lColor >> 16) & 0xFF) * INVDIV) * lIntensity;
				float lG = (((lColor >> 8) & 0xFF)  * INVDIV) * lIntensity;
				float lB = ((lColor & 0xFF)         * INVDIV) * lIntensity;

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

						// Negate these light directions, as lightDir points from vertex to
						// light and the spotlights's sd* variables point from
						// light to scene. Evaluating them without any negation resulted
						// in a negative dot product that just got it culled right below.
						float spotDot = (-lightDirX * sdX + -lightDirY * sdY + -lightDirZ * sdZ);
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
				if (twoSided)
				{
					// Dot product between transformed normal and eye-to-vertex direction.
					// Are they negative? Flip the eye normals so we can light the other side.
					float nDotV = N_EYE[0] * viewX + N_EYE[1] * viewY + N_EYE[2] * viewZ;
					if (nDotV < 0.0f)
					{
						N_EYE[0] = -N_EYE[0];
						N_EYE[1] = -N_EYE[1];
						N_EYE[2] = -N_EYE[2];
					}
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
					for (int u = 0; u < Graphics3D.ACTIVE_TEXTURE_UNITS; u++)
					{
						if (texc[u] != null) { System.arraycopy(inT[u], 4*i, outT[u], 4*outCount, 4); }
					}
				}

				outC[outCount] = inC[i];
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
						for (int u = 0; u < Graphics3D.ACTIVE_TEXTURE_UNITS; u++)
						{
							if (texc[u] != null)
							{
								outT[u][4*outCount + c] = inT[u][4*i + c] + amt * (inT[u][4*j + c] - inT[u][4*i + c]);
							}
						}
					}
				}

				final int cA = inC[i], cB = inC[j];
				final int alpha = (int) (amt * 256f);

				final int rbA = cA & 0x00FF00FF, rbB = cB & 0x00FF00FF;
				final int agA = (cA >>> 8) & 0x00FF00FF, agB = (cB >>> 8) & 0x00FF00FF;

				final int rb = (rbA + (((rbB - rbA) * alpha) >> 8)) & 0x00FF00FF;
				final int ag = (agA + (((agB - agA) * alpha) >> 8)) & 0x00FF00FF;

				outC[outCount] = rb | (ag << 8);
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

	private static final boolean outsideFrustum()
	{
		final float w0 = inV[3],  w1 = inV[7],  w2 = inV[11];

		if (inV[0] < -w0 && inV[4] < -w1 && inV[8] < -w2)  { return true; }
		if (inV[0] >  w0 && inV[4] >  w1 && inV[8] >  w2)  { return true; }

		if (inV[1] < -w0 && inV[5] < -w1 && inV[9] < -w2)  { return true; }
		if (inV[1] >  w0 && inV[5] >  w1 && inV[9] >  w2)  { return true; }

		if (inV[2] < -w0 && inV[6] < -w1 && inV[10] < -w2) { return true; }
		if (inV[2] >  w0 && inV[6] >  w1 && inV[10] > w2)  { return true; }

		return false;
	}

	public static final void transform(Triangle[] triangles, int visibleTris, Transform trVert, Transform[] trTex)
	{
		for (int i = 0; i < visibleTris; i++)
		{
			trVert.transform(triangles[i].v);

			for(int u = 0; u < Graphics3D.ACTIVE_TEXTURE_UNITS; u++)
			{
				if (trTex != null)
				{
					// Each trTex transform is bound to a texture unit, so it is
					// safe to use it as a check to see if we have these coords.
					trTex[u].transformTexCoords(triangles[i].t[u]);
				}
			}
		}
	}

	public final void project(boolean perspectiveCorrect)
	{
		// Apply perspective division to the triangle, it's going to NDC
		for (int i = 0; i < 3; i++)
		{
			int baseIdx = 4 * i;
			// It is faster to calculate the reciprocal of w (1/w) and just
			// multiply vertices and texture coordinates by it, than it is to
			// constantly divide them by W here.
			invW[i] = M3GMath.fastReciprocal(v[baseIdx + 3]);

			// Project vertex
			v[baseIdx + 0] *= invW[i]; // x / w
			v[baseIdx + 1] *= invW[i]; // y / w
			v[baseIdx + 2] *= invW[i]; // z / w
			v[baseIdx + 3] = 1.0f;  // Set w to 1

			// Texture coordinates are stored as s/w and t/w if
			// perspective correction is enabled (undone per-pixel in rasterizer)
			if (perspectiveCorrect)
			{
				for (int u = 0; u < Graphics3D.ACTIVE_TEXTURE_UNITS; u++)
				{
					t[u][2 * i + 0] *= invW[i]; // s / w
					t[u][2 * i + 1] *= invW[i]; // t / w
				}
			}
		}
	}

	private static final boolean isCounterClockwise()
	{
		float ax = inV[0], ay = inV[1], aw = inV[3];
		float bx = inV[4], by = inV[5], bw = inV[7];
		float cx = inV[8], cy = inV[9], cw = inV[11];

		// Usually counterClockWise would be <= 0.0, but we're in Clip space
		// here where Y is the inverse of NDC, so invert to > 0.0;
		return ax * (by * cw - cy * bw) + bx * (cy * aw - ay * cw)
			+ cx * (ay * bw - by * aw) > 0.0f;
	}

	public final float xA() { return v[0]; }
	public final float yA() { return v[1]; }
	public final float zA() { return v[2]; }
	public final float wA() { return v[3]; }
	public final float xB() { return v[4]; }
	public final float yB() { return v[5]; }
	public final float zB() { return v[6]; }
	public final float wB() { return v[7]; }
	public final float xC() { return v[8]; }
	public final float yC() { return v[9]; }
	public final float zC() { return v[10]; }
	public final float wC() { return v[11]; }

	public final float sA(int unit) { return t[unit][0]; }
	public final float tA(int unit) { return t[unit][1]; }
	public final float sB(int unit) { return t[unit][2]; }
	public final float tB(int unit) { return t[unit][3]; }
	public final float sC(int unit) { return t[unit][4]; }
	public final float tC(int unit) { return t[unit][5]; }

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

		// The number of active texture units MAY have increased since this
		// triangle was created, check here and resize properly..
		if(Graphics3D.ACTIVE_TEXTURE_UNITS > this.t.length / 6)
			{ this.t = new float[Graphics3D.ACTIVE_TEXTURE_UNITS][6]; }

		for (int i = 0; i < Graphics3D.ACTIVE_TEXTURE_UNITS; i++)
		{
			if (tCoords[i] == null) { continue; }
			t[i][0] = tCoords[i][0];  t[i][1] = tCoords[i][1];
			t[i][2] = tCoords[i][f1]; t[i][3] = tCoords[i][f1 + 1];
			t[i][4] = tCoords[i][f2]; t[i][5] = tCoords[i][f2 + 1];
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
