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

public class Group extends Node
{
	public Node firstChild;
	public int numNonCullables = 0, numRenderables = 0;

	protected Object3D duplicateImpl()
	{
		Group copy = (Group) super.duplicateImpl();
		copy.firstChild = null;

		// We must Duplicate each child in the circular doubly-linked list
		if (this.firstChild != null)
		{
			Node curr = this.firstChild;
			do
			{
				Node childCopy = (Node) curr.duplicateImpl();
				copy.addChild(childCopy);
				curr = curr.right;
			}
			while (curr != this.firstChild);
		}

		return copy;
	}

	public void addChild(Node child)
	{
		if (child == null) { throw new NullPointerException("child can not be null"); }
		if (child == this) { throw new IllegalArgumentException("can not add self as child"); }
		if (child.getParent() != null) { throw new IllegalArgumentException("child already has parent"); }
		if (isAncestor(child)) { throw new IllegalArgumentException("Cannot add an ancestor as a child"); }

		if (firstChild == null)
		{
			firstChild = child;
			child.left = child;
			child.right = child;
		}
		else
		{
			Node lastChild = firstChild.left;

			lastChild.right = child;
			child.left = lastChild;

			child.right = firstChild;
			firstChild.left = child;
		}

		child.setParent(this);
		addReference(child);
	}

	public Node getChild(int idx)
	{
		if (idx < 0 || idx > getChildCount())
			{ throw new IllegalArgumentException("Negative child index"); }

		if (firstChild == null)
		{
			throw new IndexOutOfBoundsException("Group has no children");
		}

		Node n = firstChild;
		int count = 0;
		do
		{
			if (count == idx)
			{
				return n;
			}
			count++;
			n = n.right;
		}
		while (n != firstChild);

		throw new IndexOutOfBoundsException("Index " + idx + " out of bounds (child count is: " + count + ")");
	}

	public int getChildCount()
	{
		if (firstChild == null)
		{
			return 0;
		}

		int count = 0;
		Node child = firstChild;
		do
		{
			count++;
			child = child.right;
		}
		while (child != firstChild);

		return count;
	}

	public void removeChild(Node child)
	{
		if (child != null && firstChild != null)
		{
			Node n = firstChild;
			do
			{
				if (n == child)
				{
					if (n.right == n) // Only child in the list
					{
						firstChild = null;
					}
					else
					{
						n.right.left = n.left;
						n.left.right = n.right;

						if (firstChild == n)
						{
							firstChild = n.right;
						}
					}

					n.left = null;
					n.right = null;
					n.setParent(null);
					removeReference(child);
					return;
				}
				n = n.right;
			}
			while (n != firstChild);
		}
	}

	@Override
	boolean doAlign(Node ref)
	{
		if (!super.doAlign(ref))
		{
			return false;
		}

		Node child = firstChild;
		if (child != null)
		{
			do
			{
				if (!child.doAlign(ref))
				{
					return false;
				}
				child = child.right;
			}
			while (child != firstChild);
		}
		return true;
	}

	public boolean pick(int scope, float x, float y, Camera camera, RayIntersection ri)
	{
		if (camera == null) { throw new NullPointerException("Camera cannot be null"); }

		// TODO
		return false;
	}

	public boolean pick(int scope, float ox, float oy, float oz, float dx, float dy, float dz, RayIntersection ri)
	{
		if (dx == 0.0f && dy == 0.0f && dz == 0.0f)
			{ throw new IllegalArgumentException("Ray direction vector cannot be zero"); }
		// TODO
		return false;
	}

	private boolean isAncestor(Node potentialChild)
	{
		Node p = this.getParent();
		while (p != null)
		{
			if (p == potentialChild)
			{
				return true;
			}
			p = p.getParent();
		}
		return false;
	}

	int getRenderableCount() { return this.numRenderables; }
	int getNonCullableCount() { return this.numNonCullables; }
}
