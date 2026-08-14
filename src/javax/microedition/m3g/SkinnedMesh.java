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
	private final ArrayList<BoneData> bones = new ArrayList<BoneData>();

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

		if (!getTransformTo(bone, data.initialTransform))
		{
			data.initialTransform.setIdentity();
		}

		bones.add(data);
		bone.hasBones = true;
	}

	public void getBoneTransform(Node bone, Transform transform)
	{
		if (bone == null || transform == null)
		{
			throw new NullPointerException("Bone and Transform cannot be null");
		}

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
			if (b.bone == bone)
			{
				count += b.numVertices;
			}
		}

		if (count == 0) return 0;

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
					if (indices != null) indices[idx] = b.firstVertex + i;
					if (weights != null) weights[idx] = b.weight;
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
		if (root == target) return root;
		if (root instanceof Group)
		{
			Group g = (Group) root;
			for (int i = 0; i < g.getChildCount(); i++)
			{
				Node found = findNodeInTree(g.getChild(i), target);
				if (found != null) return found;
			}
		}
		return null;
	}
}
