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

import java.util.Vector;
import org.recompile.mobile.Mobile;

public abstract class Node extends Transformable
{
	public static final int NONE  = 144;
	public static final int ORIGIN  = 145;
	public static final int X_AXIS  = 146;
	public static final int Y_AXIS  = 147;
	public static final int Z_AXIS  = 148;

	public Node parent = null;
	public Node left;
	public Node right;

	private Node yRef = null;
	private Node zRef = null;
	private int yTarget = NONE;
	private int zTarget = NONE;
	private float alphaFactor = 1.0f;
	private boolean picking = true;
	private boolean rendering = true;
	private int scope = -1;

	boolean hasRenderables = false;
	boolean hasBones = false;
	boolean[] dirtyBits = new boolean[2]; // {renderablesBit, bonesBit}

	protected Object3D duplicateImpl()
	{
		Node copy = (Node) super.duplicateImpl();
		copy.parent = null;
		copy.left = null;
		copy.right = null;
		copy.yRef = null;
		copy.zRef = null;
		copy.yTarget = NONE;
		copy.zTarget = NONE;
		copy.dirtyBits = new boolean[]{ false, false };
		return copy;
	}

	boolean doAlign(Node ref) { return computeAlignment(ref == null ? this : ref); }

	public final void align(Node reference)
	{
		Mobile.log(Mobile.LOG_WARNING, Node.class.getPackage().getName() + "." + Node.class.getSimpleName() + ": " + "Node Alignment requested (untested)");
		if (reference != null && (this.getRootNode() != reference.getRootNode())) { throw new IllegalArgumentException(); }

		doAlign(reference == null ? this : reference);
	}

	boolean computeAlignment(Node refNode)
	{
		if (zTarget == NONE && yTarget == NONE) { return true; }

		Node root = getRootNode();
		if (zRef != null && (isChildOf(this, zRef) || zRef.getRootNode() != root)) { return false; }
		if (yRef != null && (isChildOf(this, yRef) || yRef.getRootNode() != root)) { return false; }

		if (zTarget != NONE)
		{
			Node targetNode = (zRef != null) ? zRef : refNode;
			if (targetNode == this) { return false; }
			if (!computeAlignmentRotation(new float[]{0, 0, 1}, targetNode, zTarget, NONE)) { return false; }
		}

		if (yTarget != NONE)
		{
			Node targetNode = (yRef != null) ? yRef : refNode;
			if (targetNode == this) { return false; }
			if (!computeAlignmentRotation(new float[]{0, 1, 0}, targetNode, yTarget, (zTarget != NONE) ? Z_AXIS : NONE)) { return false; }
		}

		return true;
	}

	private boolean computeAlignmentRotation(float[] srcAxis, Node targetNode, int targetAxisName, int constraint)
	{
		if (this.parent == null) return true;

		Transform transform = new Transform();
		if (!targetNode.getTransformTo(this.parent, transform)) return false;

		float[] transformMatrix = new float[16];
		float[] orientation = new float[4];
		float[] targetAxis = new float[4];

		getOrientation(orientation);

		float[] translation = new float[3];
		getTranslation(translation);
		transform.postTranslate(transformMatrix[12], transformMatrix[13], transformMatrix[14]);

		if (constraint != NONE)
		{
			float[] rot = new float[]{ orientation[0], orientation[1], orientation[2], -orientation[3] };
			transform.preRotate(rot[0], rot[1], rot[2], rot[3]);
		}

		transform.get(transformMatrix);
		transformAlignmentTarget(targetAxisName, transformMatrix, targetAxis);

		if (constraint == Z_AXIS)
		{
			float norm = targetAxis[0] * targetAxis[0] + targetAxis[1] * targetAxis[1];
			if (norm < 1.0e-5f) return true;
			norm = 1.0f / M3GMath.sqrt(norm);
			targetAxis[0] *= norm;
			targetAxis[1] *= norm;
			targetAxis[2] = 0.0f;
		}
		else
		{
			float norm = targetAxis[0] * targetAxis[0] + targetAxis[1] * targetAxis[1] + targetAxis[2] * targetAxis[2];
			if (norm > 1.0e-5f)
			{
				norm = 1.0f / M3GMath.sqrt(norm);
				targetAxis[0] *= norm;
				targetAxis[1] *= norm;
				targetAxis[2] *= norm;
			}
		}

		float[] rot = M3GMath.setQuatRotation(srcAxis, targetAxis);

		if (constraint != NONE)
		{
			float[] newOrientation = new float[4];
			M3GMath.mulQuat(orientation, rot, newOrientation);
			System.arraycopy(newOrientation, 0, orientation, 0, 4);
		}
		else
		{
			System.arraycopy(rot, 0, orientation, 0, 4);
		}

		setOrientation(orientation[0], orientation[1], orientation[2], orientation[3]);
		return true;
	}

	static void transformAlignmentTarget(int target, float[] transform, float[] out)
	{
		out[0] = 0; out[1] = 0; out[2] = 0; out[3] = 0;

		switch (target)
		{
			case ORIGIN: out[3] = 1.0f; break;
			case X_AXIS: out[0] = 1.0f; break;
			case Y_AXIS: out[1] = 1.0f; break;
			case Z_AXIS: out[2] = 1.0f; break;
		}

		float x = transform[0] * out[0] + transform[1] * out[1] + transform[2] * out[2] + transform[3] * out[3];
		float y = transform[4] * out[0] + transform[5] * out[1] + transform[6] * out[2] + transform[7] * out[3];
		float z = transform[8] * out[0] + transform[9] * out[1] + transform[10] * out[2] + transform[11] * out[3];

		out[0] = x;
		out[1] = y;
		out[2] = z;
		out[3] = 0;
	}

	static boolean isChildOf(Node parent, Node child)
	{
		for (Node n = child; n != null; n = n.parent)
		{
			if (n.parent == parent) { return true; }
		}

		return false;
	}

	/*
	 * Returns the node in dstRoot occupying the same relative position as
	 * srcNode in srcRoot, assuming both trees have an identical structure.
	 * Used to rewire intra-tree references (e.g. bones, the active camera)
	 * after duplicating a scene graph branch. Returns null if srcNode is
	 * not a descendant of srcRoot.
	 */
	static Node matchingNode(Node srcRoot, Node srcNode, Node dstRoot)
	{
		if (srcNode == srcRoot) { return dstRoot; }

		// Compute the depth of srcNode relative to the tree root.
		int depth = 0;
		for (Node n = srcNode; n != srcRoot; n = n.getParent())
		{
			if (n == null) { return null; }
			depth++;
		}

		// Collect the child indices from the root down to the node.
		int[] indices = new int[depth];
		int i = depth;
		for (Node n = srcNode; n != srcRoot; )
		{
			Node parent = n.getParent();
			if (!(parent instanceof Group)) { return null; }

			Group g = (Group) parent;
			int idx = -1;
			int children = g.getChildCount();
			for (int c = 0; c < children; c++)
			{
				if (g.getChild(c) == n) { idx = c; break; }
			}
			if (idx < 0) { return null; }

			indices[--i] = idx;
			n = parent;
		}

		// Walk the same path in the destination tree.
		Node dst = dstRoot;
		for (i = 0; i < indices.length; i++)
		{
			if (!(dst instanceof Group) || indices[i] >= ((Group) dst).getChildCount()) { return null; }
			dst = ((Group) dst).getChild(indices[i]);
		}

		return dst;
	}

	public Node getAlignmentReference(int axis)
	{
		if(axis == Y_AXIS) { return this.yRef; }
		else if(axis == Z_AXIS) { return this.zRef; }

		/* If it's not Y_AXIS or Z_AXIS, throw IllegalArgumentException as per JSR-184. */
		throw new IllegalArgumentException("Tried requesting alignment reference on invalid axis.");
	}

	public int getAlignmentTarget(int axis)
	{
		if(axis == Y_AXIS) { return this.yTarget; }
		else if(axis == Z_AXIS) { return this.zTarget; }

		/* If it's not Y_AXIS or Z_AXIS, throw IllegalArgumentException as per JSR-184. */
		throw new IllegalArgumentException("Tried requesting alignment target on invalid axis.");
	}

	public float getAlphaFactor() { return this.alphaFactor; }

	public Node getParent() { return this.parent; }

	/* Mostly used so we can find whether a child node*/
	public Node getRootNode()
	{
		Node root = this;
		while (root.parent != null)
		{
			root = root.parent;
		}
		return root;
	}

	public int getScope() { return this.scope; }

	public boolean getTransformTo(Node target, Transform transform)
	{
		if (target == null) { throw new NullPointerException("Target node cannot be null"); }
		if (transform == null) { throw new NullPointerException("Transform object cannot be null"); }

		if (target == this)
		{
			transform.setIdentity();
			return true;
		}

		Vector<Node> pathThis = new Vector<Node>();
		for (Node n = this; n != null; n = n.parent)
			{ pathThis.addElement(n); }

		Vector<Node> pathTarget = new Vector<Node>();
		for (Node n = target; n != null; n = n.parent)
			{ pathTarget.addElement(n); }

		// Check this and target share the same root.
		if (pathThis.elementAt(pathThis.size() - 1) != pathTarget.elementAt(pathTarget.size() - 1))
			{ return false; }

		int i = pathThis.size() - 1;
		int j = pathTarget.size() - 1;
		while (i >= 0 && j >= 0 && pathThis.elementAt(i) == pathTarget.elementAt(j))
		{
			i--;
			j--;
		}
		int lcaIndexThis = i + 1;
		int lcaIndexTarget = j + 1;

		// We accumulate transforms from this node all the way to the
		// lowest ancestor common to both nodes
		Transform thisToRoot = new Transform();
		Transform temp = new Transform();
		for (int k = lcaIndexThis - 1; k >= 0; k--)
		{
			Node n = (Node) pathThis.elementAt(k);
			n.getCompositeTransform(temp);
			thisToRoot.postMultiply(temp);
		}

		// Same for the target
		Transform targetToRoot = new Transform();
		for (int k = lcaIndexTarget - 1; k >= 0; k--)
		{
			Node n = (Node) pathTarget.elementAt(k);
			n.getCompositeTransform(temp);
			targetToRoot.postMultiply(temp);
		}

		// Will throw exception if matrix is not invertible
		targetToRoot.invert();

		targetToRoot.postMultiply(thisToRoot);
		transform.set(targetToRoot);
		return true;
	}

	public boolean isPickingEnabled() { return this.picking; }

	public boolean isRenderingEnabled() { return this.rendering; }

	public void setAlignment(Node zRef, int zTarget, Node yRef, int yTarget)
	{
		/*
		 * As per JSR-184, throw IllegalArgumentException if:
		 * yTarget or zTarget is not one of the symbolic constants listed above
		 * (zRef == yRef) && (zTarget == yTarget != NONE)
		 * zRef or yRef is this Node.
		 */
		if ((zTarget != NONE && zTarget != X_AXIS && zTarget != Y_AXIS && zTarget != Z_AXIS && zTarget != ORIGIN) ||
			(yTarget != NONE && yTarget != X_AXIS && yTarget != Y_AXIS && yTarget != Z_AXIS && yTarget != ORIGIN))
			{ throw new IllegalArgumentException("Node target axis is invalid."); }
		if (zRef != null && yRef != null && zRef == yRef && zTarget != NONE && yTarget != NONE)
			{ throw new IllegalArgumentException("Tried to align with two references having the same axis."); }
		if (zRef == this || yRef == this)
			{ throw new IllegalArgumentException("Tried to use this node as one of the reference nodes."); }

		this.zRef = (zTarget != NONE) ? zRef : null;
		this.yRef = (yTarget != NONE) ? yRef : null;
		this.zTarget = zTarget;
		this.yTarget = yTarget;
	}

	public void setAlphaFactor(float alphaFactor)
	{
		/* As per JSR-184, throw IllegalArgumentException if factor < 0 or factor > 1.0.*/
		if (alphaFactor < 0 || alphaFactor > 1)
			{ throw new IllegalArgumentException("Tried to set AlphaFactor with out of range value."); }

		this.alphaFactor = alphaFactor;
	}

	public void setPickingEnable(boolean enable) { this.picking = enable; }

	public void setRenderingEnable(boolean enable) { this.rendering = enable; }

	public void setScope(int scope) { this.scope = scope; }

	void setParent(Node parent)
	{
		int nonCullableChange = getNonCullableCount();
		int renderableChange = getRenderableCount();

		if (this.parent != null)
		{
			this.parent.updateNodeCounters(-nonCullableChange, -renderableChange);
			if (renderableChange != 0)
			{
				this.parent.invalidateNode(new boolean[]{ true, true });
			}
		}

		this.parent = parent;

		if (parent != null)
		{
			boolean[] flags = new boolean[]{ renderableChange != 0, hasBones };
			parent.updateNodeCounters(nonCullableChange, renderableChange);
			parent.invalidateNode(flags);
		}
	}

	void invalidateNode(boolean[] flags)
	{
		Node node = this;
		while (node != null && (node.dirtyBits[0] != flags[0] || node.dirtyBits[1] != flags[1]))
		{
			System.arraycopy(flags, 0, node.dirtyBits, 0, 2);
			node = node.parent;
		}
	}

	void updateNodeCounters(int nonCullableChange, int renderableChange)
	{
		boolean hasRenderables = (renderableChange > 0);
		Node node = this;
		while (node != null)
		{
			if (node instanceof Group || node instanceof World)
			{
				((Group) node).numNonCullables += nonCullableChange;
				((Group) node).numRenderables += renderableChange;
				hasRenderables = ((Group) node).numRenderables > 0;
			}
			node.hasRenderables = hasRenderables;
			node = node.parent;
		}
	}

	// Getters for renderable/cullable count in Group objects.
	int getRenderableCount() { return 0; }
	int getNonCullableCount() { return 0; }

	@Override
	void updateProperty(int property, float[] value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Node.class.getPackage().getName() + "." + Node.class.getSimpleName() + ": " + "AnimTrack updating Node property");
		switch (property)
		{
			case AnimationTrack.ALPHA:
				setAlphaFactor(M3GMath.max(0.0f, M3GMath.min(1.0f, value[0])));
				break;
			case AnimationTrack.PICKABILITY:
				setPickingEnable(value[0] >= 0.5f);
				break;
			case AnimationTrack.VISIBILITY:
				setRenderingEnable(value[0] >= 0.5f);
				this.dirtyBits[0] = true;
				break;
			default:
				super.updateProperty(property, value);
		}
	}


	boolean animTrackCompatible(AnimationTrack track)
	{
		switch (track.getTargetProperty())
		{
			case AnimationTrack.ALPHA:
			case AnimationTrack.VISIBILITY:
			case AnimationTrack.PICKABILITY:
				return true;
			default:
				return super.animTrackCompatible(track);
		}
	}
}
