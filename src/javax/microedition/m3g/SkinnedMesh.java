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

public class SkinnedMesh extends Mesh
{
	public Group skeleton;

	private VertexBuffer skinnedVertices;
	private ArrayList<BoneData> bones = new ArrayList<BoneData>();

	// Used to store bone information for vertex transforms.
	private static class BoneData
	{
		Node bone;
		int weight;
		int firstVertex;
		int numVertices;

		// At-rest transformation, set as this SkinnedMesh.getTransformTo(bone)
		// when the bone is added via addTransform(), as mandated by JSR-184.
		Transform initialTransform = new Transform();

		BoneData(Node bone, int weight, int firstVertex, int numVertices)
		{
			this.bone = bone;
			this.weight = weight;
			this.firstVertex = firstVertex;
			this.numVertices = numVertices;
		}
	}

	public SkinnedMesh(VertexBuffer vertices, IndexBuffer[] submeshes, Appearance[] appearances, Group skeleton)
	{
		super(vertices, submeshes, appearances);
		checkSkeleton(skeleton);
		this.skeleton = skeleton;
		this.skeleton.setParent(this);
		addReference(this.skeleton);
	}

	public SkinnedMesh(VertexBuffer vertices, IndexBuffer submeshes, Appearance appearances, Group skeleton)
	{
		super(vertices, submeshes, appearances);
		checkSkeleton(skeleton);
		this.skeleton = skeleton;
		this.skeleton.setParent(this);
		addReference(this.skeleton);
	}

	protected SkinnedMesh() { }

	protected Object3D duplicateImpl()
	{
		SkinnedMesh copy = (SkinnedMesh) super.duplicateImpl();

		Group copySkeleton = (Group) this.skeleton.duplicate();
		copy.skeleton = copySkeleton;
		copy.skeleton.setParent(copy);
		copy.addReference(copySkeleton);

		// Bones must point to the duplicated skeleton nodes, which appear in
		// the same order as in the source skeleton. The at-rest transforms are
		// kept as-is: they were captured by addTransform() and must not change.
		copy.bones = new ArrayList<BoneData>(this.bones.size());
		for (BoneData b : this.bones)
		{
			Node copyBone = Node.matchingNode(this.skeleton, b.bone, copySkeleton);
			if (copyBone != null)
			{
				BoneData copyData = new BoneData(copyBone, b.weight, b.firstVertex, b.numVertices);
				copyData.initialTransform.set(b.initialTransform);
				copy.bones.add(copyData);
			}
		}

		// Skinned vertices are rebuilt lazily from the base VertexBuffer.
		copy.skinnedVertices = null;

		return copy;
	}

	public void addTransform(Node bone, int weight, int firstVertex, int numVertices)
	{
		if (bone == null) { throw new NullPointerException("Bone node cannot be null"); }
		if (weight <= 0) { throw new IllegalArgumentException("Weight must be positive"); }
		if (numVertices <= 0) { throw new IllegalArgumentException("NumVertices must be positive"); }
		if (firstVertex < 0 || firstVertex + numVertices > 65535)
		{
			throw new IndexOutOfBoundsException("Vertex range [" + firstVertex + ", " + (firstVertex + numVertices) + ") out of bounds (max: 65535)");
		}

		if (!isChildOf(this.skeleton, bone) && bone != this.skeleton)
		{
			throw new IllegalArgumentException("Bone node must be part of the skeleton group hierarchy");
		}

		BoneData data = new BoneData(bone, weight, firstVertex, numVertices);

		// JSR-184: the at-rest transformation of the bone is captured here, as
		// the transformation from this SkinnedMesh to the bone, at the time of
		// the addTransform() call. getTransformTo() throws ArithmeticException
		// by itself if that transformation cannot be computed.
		if (!this.getTransformTo(bone, data.initialTransform))
		{
			throw new ArithmeticException("The at-rest transformation cannot be computed for the given bone");
		}

		this.dirtyBits[1] = true;
		bones.add(data);
		bone.hasBones = true;
	}

	public void getBoneTransform(Node bone, Transform transform)
	{
		if (bone == null) { throw new NullPointerException("Bone node cannot be null"); }
		if (transform == null) { throw new NullPointerException("Transform cannot be null"); }

		if (!isChildOf(this.skeleton, bone) && bone != this.skeleton)
		{
			throw new IllegalArgumentException("Node is not in the skeleton group of this SkinnedMesh");
		}

		for (BoneData b : bones)
		{
			if (b.bone == bone)
			{
				transform.set(b.initialTransform);
				return;
			}
		}

		// Per JSR-184, the returned transformation for a node that is in the
		// skeleton group but has no vertices associated with it is undefined.
		transform.setIdentity();
	}

	public int getBoneVertices(Node bone, int[] indices, float[] weights)
	{
		if (bone == null) { throw new NullPointerException("Bone node cannot be null"); }

		if (!isChildOf(this.skeleton, bone) && bone != this.skeleton)
		{
			throw new IllegalArgumentException("Node is not in the skeleton group of this SkinnedMesh");
		}

		int count = 0;
		for (BoneData b : bones)
		{
			if (b.bone == bone) { count += b.numVertices; }
		}

		if (count == 0) { return 0; }

		if (indices != null && indices.length < count)
		{
			throw new IllegalArgumentException("Indices array length too small (needed " + count + ")");
		}
		if (weights != null && weights.length < count)
		{
			throw new IllegalArgumentException("Weights array length too small (needed " + count + ")");
		}

		int idx = 0;
		for (BoneData b : bones)
		{
			if (b.bone == bone)
			{
				for (int i = 0; i < b.numVertices; i++)
				{
					if (indices != null) { indices[idx] = b.firstVertex + i; }

					// Per JSR-184, the returned weights are normalized such that
					// the weights of all bones affecting a vertex sum to one.
					if (weights != null) { weights[idx] = b.weight / totalWeightForVertex(b.firstVertex + i); }
					idx++;
				}
			}
		}

		return count;
	}

	public Group getSkeleton() { return skeleton; }

	private void checkSkeleton(Group skeleton)
	{
		if (skeleton == null) { throw new NullPointerException("Skeleton cannot be null"); }
		if (skeleton instanceof World) { throw new IllegalArgumentException("Skeleton cannot be a World node"); }
		if (skeleton.getParent() != null) { throw new IllegalArgumentException("Skeleton already has a parent"); }
	}

	// Sum of the weights of every bone affecting the given vertex.
	private float totalWeightForVertex(int vertex)
	{
		float total = 0.0f;
		for (BoneData b : bones)
		{
			if (vertex >= b.firstVertex && vertex < b.firstVertex + b.numVertices)
			{
				total += b.weight;
			}
		}
		return total;
	}

	@Override
	public VertexBuffer getVertexBuffer()
	{
		VertexBuffer base = super.getVertexBuffer();
		if (bones.isEmpty() || base == null)
		{
			return base;
		}

		if (skinnedVertices == null)
		{
			skinnedVertices = (VertexBuffer) base.duplicate();

			// DEEP COPY position and normal arrays so the base is never mutated
			float[] scaleBias = new float[4];
			VertexArray basePositions = base.getPositions(scaleBias);
			if (basePositions != null)
			{
				VertexArray clonedPositions = (VertexArray) basePositions.duplicate();
				skinnedVertices.setPositions(clonedPositions, scaleBias[0],
					new float[] { scaleBias[1], scaleBias[2], scaleBias[3] });
			}

			VertexArray baseNormals = base.getNormals();
			if (baseNormals != null)
			{
				skinnedVertices.setNormals((VertexArray) baseNormals.duplicate());
			}
		}

		// TODO: Try checking against dirtyBits[1] later, it should be true
		// whenever the underlying transformable changes.
		applySkinning(base);

		return skinnedVertices;
	}

	private void applySkinning(VertexBuffer base)
	{
		float[] scaleBias = new float[4];
		VertexArray basePositions = base.getPositions(scaleBias);
		if (basePositions == null) { return; }

		VertexArray blendedPositions = skinnedVertices.getPositions(null);
		if (blendedPositions == null) { return; }

		VertexArray baseNormals = base.getNormals();
		VertexArray blendedNormals = (baseNormals != null) ? skinnedVertices.getNormals() : null;

		int numVertices = basePositions.getVertexCount();
		int components = basePositions.getComponentCount();
		int totalElements = numVertices * components;

		int numBones = bones.size();

		// JSR-184 deferred validation: a bone's vertex range cannot be validated
		// at addTransform() time since the vertex buffer can change at any time.
		// Throw when the mesh is actually needed, that is, when rendering.
		for (int b = 0; b < numBones; b++)
		{
			BoneData data = bones.get(b);
			if (data.firstVertex + data.numVertices > numVertices)
			{
				throw new IllegalStateException("Bone vertex range exceeds the vertex buffer size");
			}
		}

		float scale = scaleBias[0];
		float biasX = scaleBias[1], biasY = scaleBias[2], biasZ = scaleBias[3];

		// Compute the skinning matrix of each bone, M * B, where M is the current
		// transformation from the bone to this SkinnedMesh and B is the at-rest
		// transformation from this SkinnedMesh to the bone, captured by addTransform().
		float[][] skinningMatrices = new float[numBones][16];
		Transform boneToMesh = new Transform();
		Transform finalSkinning = new Transform();

		for (int b = 0; b < numBones; b++)
		{
			BoneData data = bones.get(b);

			// 1. Current Bone -> Mesh in animated pose
			if (!data.bone.getTransformTo(this, boneToMesh))
			{
				boneToMesh.setIdentity();
			}

			// 2. Multiply by Mesh -> Bone reference pose (at-rest transform)
			finalSkinning.set(boneToMesh);
			finalSkinning.postMultiply(data.initialTransform);
			finalSkinning.get(skinningMatrices[b]);
		}

		int componentType = basePositions.getComponentType();
		float[] floatPos = new float[totalElements];
		float[] floatNormals = null;

		if (componentType == 1) // byte
		{
			byte[] rawBytes = new byte[totalElements];
			basePositions.get(0, numVertices, rawBytes);
			for (int i = 0; i < totalElements; i++) floatPos[i] = rawBytes[i];
		}
		else if (componentType == 2) // short
		{
			short[] rawShorts = new short[totalElements];
			basePositions.get(0, numVertices, rawShorts);
			for (int i = 0; i < totalElements; i++) floatPos[i] = rawShorts[i];
		}

		float[] accumulatedZ = new float[numVertices];

		if (baseNormals != null && blendedNormals != null)
		{
			floatNormals = new float[totalElements];

			if (baseNormals.getComponentType() == 1) // byte
			{
				byte[] rawBytes = new byte[totalElements];
				baseNormals.get(0, numVertices, rawBytes);
				for (int i = 0; i < totalElements; i++) floatNormals[i] = rawBytes[i];
			}
			else if (baseNormals.getComponentType() == 2) // short
			{
				short[] rawShorts = new short[totalElements];
				baseNormals.get(0, numVertices, rawShorts);
				for (int i = 0; i < totalElements; i++) floatNormals[i] = rawShorts[i];
			}
		}

		float[] accumulatedX = new float[numVertices];
		float[] accumulatedY = new float[numVertices];
		float[] accumulatedNormalsX = (floatNormals != null) ? new float[numVertices] : null;
		float[] accumulatedNormalsY = (floatNormals != null) ? new float[numVertices] : null;
		float[] accumulatedNormalsZ = (floatNormals != null) ? new float[numVertices] : null;
		float[] weightSums = new float[numVertices];

		for (int b = 0; b < numBones; b++)
		{
			BoneData bd = bones.get(b);
			float[] m = skinningMatrices[b];
			float w = (float) bd.weight;

			for (int i = 0; i < bd.numVertices; i++)
			{
				int vIdx = bd.firstVertex + i;
				int offset = vIdx * components;

				// De-quantize position to object space units
				float x = floatPos[offset] * scale + biasX;
				float y = floatPos[offset + 1] * scale + biasY;
				float z = (components > 2) ? (floatPos[offset + 2] * scale + biasZ) : 0.0f;

				// Transform.get() returns the matrix in row-major order, with
				// the translation in elements 3, 7 and 11.
				accumulatedX[vIdx] += (m[0] * x + m[1] * y + m[2] * z + m[3]) * w;
				accumulatedY[vIdx] += (m[4] * x + m[5] * y + m[6] * z + m[7]) * w;
				accumulatedZ[vIdx] += (m[8] * x + m[9] * y + m[10] * z + m[11]) * w;
				weightSums[vIdx] += w;

				if (floatNormals != null)
				{
					float nx = floatNormals[offset];
					float ny = floatNormals[offset + 1];
					float nz = floatNormals[offset + 2];

					// Normals only get the rotation and scaling parts of the matrix
					accumulatedNormalsX[vIdx] += (m[0] * nx + m[1] * ny + m[2] * nz) * w;
					accumulatedNormalsY[vIdx] += (m[4] * nx + m[5] * ny + m[6] * nz) * w;
					accumulatedNormalsZ[vIdx] += (m[8] * nx + m[9] * ny + m[10] * nz) * w;
				}
			}
		}

		float invScale = (scale != 0.0f) ? (1.0f / scale) : 1.0f;
		float[] finalPos = new float[totalElements];
		float[] finalNormals = (floatNormals != null) ? new float[totalElements] : null;

		for (int i = 0; i < numVertices; i++)
		{
			int offset = i * components;
			float totalW = weightSums[i];

			// Vertices with no associated bones are kept at their original
			// positions, that is, in the SkinnedMesh coordinate system.
			float finalX, finalY, finalZ;
			if (totalW > 0.0001f)
			{
				float invW = 1.0f / totalW;
				finalX = accumulatedX[i] * invW;
				finalY = accumulatedY[i] * invW;
				finalZ = accumulatedZ[i] * invW;
			}
			else
			{
				finalX = floatPos[offset] * scale + biasX;
				finalY = floatPos[offset + 1] * scale + biasY;
				finalZ = (components > 2) ? (floatPos[offset + 2] * scale + biasZ) : 0.0f;
			}

			finalPos[offset]     = (finalX - biasX) * invScale;
			finalPos[offset + 1] = (finalY - biasY) * invScale;
			if (components > 2)
			{
				finalPos[offset + 2] = (finalZ - biasZ) * invScale;
			}

			if (floatNormals != null)
			{
				float nx, ny, nz;
				if (totalW > 0.0001f)
				{
					// Blend, then restore the original normal's length so that
					// the result stays well conditioned for quantization.
					float lenSq = accumulatedNormalsX[i] * accumulatedNormalsX[i]
						+ accumulatedNormalsY[i] * accumulatedNormalsY[i]
						+ accumulatedNormalsZ[i] * accumulatedNormalsZ[i];

					if (lenSq > 0.0001f)
					{
						nx = accumulatedNormalsX[i] * M3GMath.invSqrt(lenSq);
						ny = accumulatedNormalsY[i] * M3GMath.invSqrt(lenSq);
						nz = accumulatedNormalsZ[i] * M3GMath.invSqrt(lenSq);

						float onx = floatNormals[offset];
						float ony = floatNormals[offset + 1];
						float onz = floatNormals[offset + 2];
						float originalLen = M3GMath.sqrt(onx * onx + ony * ony + onz * onz);

						nx *= originalLen;
						ny *= originalLen;
						nz *= originalLen;
					}
					else
					{
						nx = floatNormals[offset];
						ny = floatNormals[offset + 1];
						nz = floatNormals[offset + 2];
					}
				}
				else
				{
					nx = floatNormals[offset];
					ny = floatNormals[offset + 1];
					nz = floatNormals[offset + 2];
				}

				finalNormals[offset]     = nx;
				finalNormals[offset + 1] = ny;
				finalNormals[offset + 2] = nz;
			}
		}

		// Quantize and write back the final vertex attributes
		if (componentType == 1) // byte write-back
		{
			byte[] outRaw = new byte[totalElements];
			for (int i = 0; i < totalElements; i++)
			{
				outRaw[i] = (byte) M3GMath.max(-128, M3GMath.min(127, M3GMath.round(finalPos[i])));
			}
			blendedPositions.set(0, numVertices, outRaw);
		}
		else if (componentType == 2) // short write-back
		{
			short[] outRaw = new short[totalElements];
			for (int i = 0; i < totalElements; i++)
			{
				outRaw[i] = (short) M3GMath.max(-32768, M3GMath.min(32767, M3GMath.round(finalPos[i])));
			}
			blendedPositions.set(0, numVertices, outRaw);
		}

		if (finalNormals != null)
		{
			if (blendedNormals.getComponentType() == 1) // byte write-back
			{
				byte[] outRaw = new byte[totalElements];
				for (int i = 0; i < totalElements; i++)
				{
					outRaw[i] = (byte) M3GMath.max(-128, M3GMath.min(127, M3GMath.round(finalNormals[i])));
				}
				blendedNormals.set(0, numVertices, outRaw);
			}
			else // short write-back
			{
				short[] outRaw = new short[totalElements];
				for (int i = 0; i < totalElements; i++)
				{
					outRaw[i] = (short) M3GMath.max(-32768, M3GMath.min(32767, M3GMath.round(finalNormals[i])));
				}
				blendedNormals.set(0, numVertices, outRaw);
			}
		}
	}
}
