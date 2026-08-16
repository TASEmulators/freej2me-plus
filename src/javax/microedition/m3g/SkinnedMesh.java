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
	private final ArrayList<BoneData> bones = new ArrayList<BoneData>();
	private boolean initBindSet;

	// Used to store bone information for vertex transforms.
	private static class BoneData
	{
		Node bone;
		int weight;
		int firstVertex;
		int numVertices;
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

		copy.removeReference(this.skeleton);
		Group copySkeleton = (Group) this.skeleton.duplicate();
		copy.skeleton = copySkeleton;
		copy.skeleton.setParent(copy);
		copy.initBindSet = false;
		copy.addReference(copySkeleton);

		for (BoneData b : this.bones)
		{
			Node clonedBone = findNodeInTree(copySkeleton, b.bone);
			if (clonedBone != null)
			{
				BoneData copyBone = new BoneData(clonedBone, b.weight, b.firstVertex, b.numVertices);
				copyBone.initialTransform.set(b.initialTransform);
				copy.bones.add(copyBone);
			}
		}

		return copy;
	}

	public void addTransform(Node bone, int weight, int firstVertex, int numVertices)
	{
		if (bone == null) { throw new NullPointerException("Bone node cannot be null"); }
		if (weight <= 0) { throw new IllegalArgumentException("Weight must be positive"); }
		if (numVertices <= 0) { throw new IllegalArgumentException("NumVertices must be positive"); }

		VertexBuffer vbuf = getVertexBuffer();
		int maxVertices = (vbuf != null) ? vbuf.getVertexCount() : 65535;

		if (firstVertex < 0 || (firstVertex + numVertices) > maxVertices)
		{
			throw new IndexOutOfBoundsException("Vertex range [" + firstVertex + ", " + (firstVertex + numVertices) + "] out of bounds (max: " + maxVertices + ")");
		}

		if (!isChildOf(this.skeleton, bone) && bone != this.skeleton)
		{
			throw new IllegalArgumentException("Bone node must be part of the skeleton group hierarchy");
		}

		BoneData data = new BoneData(bone, weight, firstVertex, numVertices);

		this.dirtyBits[1] = true;
		this.initBindSet = false;
		bones.add(data);
		bone.hasBones = true;
	}

	public void getBoneTransform(Node bone, Transform transform)
	{
		if (bone == null || transform == null)
		{
			throw new NullPointerException("Bone and Transform cannot be null");
		}

		initBindPoses();

		for (BoneData b : bones)
		{
			if (b.bone == bone)
			{
				transform.set(b.initialTransform);
				return;
			}
		}
		throw new IllegalArgumentException("Node is not a bone in this SkinnedMesh");
	}

	public int getBoneVertices(Node bone, int[] indices, float[] weights)
	{
		if (bone == null) { throw new NullPointerException("Bone node cannot be null"); }

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
					if (weights != null) { weights[idx] = b.weight; }
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
		if (skeleton.getParent() != null) { throw new IllegalArgumentException("Skeleton already has a parent"); }
	}

	private static Node findNodeInTree(Node root, Node target)
	{
		if (root == target) { return root; }

		if (root instanceof Group)
		{
			Group g = (Group) root;
			for (int i = 0; i < g.getChildCount(); i++)
			{
				Node found = findNodeInTree(g.getChild(i), target);
				if (found != null) { return found; }
			}
		}

		return null;
	}

	@Override
	public VertexBuffer getVertexBuffer()
	{
		// TODO: Not working properly yet, tested in Solid Weapon 2 3D.
		VertexBuffer base = super.getVertexBuffer();
		if (bones.isEmpty() || base == null)
		{
			return base;
		}

		initBindPoses();

		if (skinnedVertices == null)
		{
			skinnedVertices = (VertexBuffer) base.duplicate();

			// DEEP COPY position array so base is never mutated
			float[] scaleBias = new float[4];
			VertexArray basePositions = base.getPositions(scaleBias);
			if (basePositions != null)
			{
				VertexArray clonedPositions = (VertexArray) basePositions.duplicate();
				skinnedVertices.setPositions(clonedPositions, scaleBias[0],
					new float[] { scaleBias[1], scaleBias[2], scaleBias[3] });
			}
		}

		// TODO: Try checking against dirtyBits[1] later, it should be true
		// whenever the underlying transformable changes.
		applySkinning(base);

		return skinnedVertices;
	}

	private void initBindPoses()
	{
		if (this.initBindSet) { return; }

		Transform boneToMesh = new Transform();
		for (int i = 0; i < bones.size(); i++)
		{
			BoneData bd = (BoneData) bones.get(i);
			if (bd.bone.getTransformTo(this, boneToMesh))
            {
                bd.initialTransform.set(boneToMesh);
                bd.initialTransform.invert();
            }
            else { bd.initialTransform.setIdentity(); }
		}
		this.initBindSet = true;
	}

	private void applySkinning(VertexBuffer base)
    {
    	// TODO: Vertex Normals and Colors
        float[] scaleBias = new float[4];
        VertexArray basePositions = base.getPositions(scaleBias);
        VertexArray blendedPositions = skinnedVertices.getPositions(null);

        if (basePositions == null || blendedPositions == null) { return; }

        int numVertices = basePositions.getVertexCount();
        int components = basePositions.getComponentCount();
        int totalElements = numVertices * components;

        float scale = scaleBias[0];
        float invScale = (scale != 0.0f) ? (1.0f / scale) : 1.0f;
        float biasX = scaleBias[1], biasY = scaleBias[2], biasZ = scaleBias[3];

        int numBones = bones.size();
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

            // 2. Multiply by Mesh -> Bone reference pose (Inverse Bind Pose)
            finalSkinning.set(boneToMesh);
            finalSkinning.postMultiply(data.initialTransform);
            finalSkinning.get(skinningMatrices[b]);
        }

        int componentType = basePositions.getComponentType();
        float[] floatPos = new float[totalElements];

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

        float[] accumulatedX = new float[numVertices];
        float[] accumulatedY = new float[numVertices];
        float[] accumulatedZ = new float[numVertices];
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

                accumulatedX[vIdx] += (m[0] * x + m[4] * y + m[8] * z + m[12]) * w;
                accumulatedY[vIdx] += (m[1] * x + m[5] * y + m[9] * z + m[13]) * w;
                if (components > 2)
                {
                    accumulatedZ[vIdx] += (m[2] * x + m[6] * y + m[10] * z + m[14]) * w;
                }
                weightSums[vIdx] += w;
            }
        }

        if (componentType == 1) // byte write-back
        {
            byte[] outRaw = new byte[totalElements];
            for (int i = 0; i < numVertices; i++)
            {
                int offset = i * components;
                float totalW = weightSums[i];
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

                outRaw[offset]     = (byte) M3GMath.max(-128, M3GMath.min(127, M3GMath.round((finalX - biasX) * invScale)));
                outRaw[offset + 1] = (byte) M3GMath.max(-128, M3GMath.min(127, M3GMath.round((finalY - biasY) * invScale)));
                if (components > 2)
                {
                    outRaw[offset + 2] = (byte) M3GMath.max(-128, M3GMath.min(127, M3GMath.round((finalZ - biasZ) * invScale)));
                }
            }
            blendedPositions.set(0, numVertices, outRaw);
        }
        else if (componentType == 2) // short write-back
        {
            short[] outRaw = new short[totalElements];
            for (int i = 0; i < numVertices; i++)
            {
                int offset = i * components;
                float totalW = weightSums[i];
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

                outRaw[offset]     = (short) M3GMath.max(-32768, M3GMath.min(32767, M3GMath.round((finalX - biasX) * invScale)));
                outRaw[offset + 1] = (short) M3GMath.max(-32768, M3GMath.min(32767, M3GMath.round((finalY - biasY) * invScale)));
                if (components > 2)
                {
                    outRaw[offset + 2] = (short) M3GMath.max(-32768, M3GMath.min(32767, M3GMath.round((finalZ - biasZ) * invScale)));
                }
            }
            blendedPositions.set(0, numVertices, outRaw);
        }
    }
}
