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

public class Mesh extends Node
{
	private VertexBuffer vertices;
	private IndexBuffer[] submeshes;
	private Appearance[] appearances;

	protected Mesh() { }

	public Mesh(VertexBuffer vertices, IndexBuffer submesh, Appearance appearance)
	{
		if ((vertices == null) || (submesh == null)) { throw new NullPointerException("Cannot create mesh due to a null element"); }

		this.vertices = vertices;
		this.submeshes = new IndexBuffer[]{submesh};
		this.appearances = new Appearance[]{appearance};

		// Appearance can be null here, so only add the reference if it isn't
		if (this.appearances[0] != null) { addReference(this.appearances[0]); }
		addReference(this.vertices);
		addReference(this.submeshes[0]);
	}

	public Mesh(VertexBuffer vertices, IndexBuffer[] submeshes, Appearance[] appearances)
	{
		if ((vertices == null) || (submeshes == null))
		{
			throw new NullPointerException("Cannot create mesh due to a null element");
		}

		if (submeshes.length == 0)
		{
			throw new IllegalArgumentException("Submesh array cannot be empty");
		}

		if (appearances != null && appearances.length < submeshes.length)
		{
			throw new IllegalArgumentException("Appearances array has invalid length.");
		}

		this.vertices = vertices;
		this.submeshes = new IndexBuffer[submeshes.length];
		this.appearances = new Appearance[submeshes.length];

		addReference(this.vertices);

		for (int i = 0; i < submeshes.length; i++)
		{
			if (submeshes[i] == null) { throw new NullPointerException("Submesh " + i + " is null."); }

			this.submeshes[i] = submeshes[i];
			addReference(this.submeshes[i]);

			if (appearances != null && appearances[i] != null)
			{
				this.appearances[i] = appearances[i];
				addReference(this.appearances[i]);
			}
		}
	}

	protected Object3D duplicateImpl()
	{
		Mesh copy = (Mesh) super.duplicateImpl();

		copy.vertices = this.vertices;
		if (copy.vertices != null) { copy.addReference(copy.vertices); }

		copy.submeshes = new IndexBuffer[this.submeshes.length];
		for (int i = 0; i < this.submeshes.length; i++)
		{
			copy.submeshes[i] = this.submeshes[i];
			if (copy.submeshes[i] != null) { copy.addReference(copy.submeshes[i]); }
		}

		copy.appearances = new Appearance[this.appearances.length];
		for (int i = 0; i < this.appearances.length; i++)
		{
			copy.appearances[i] = this.appearances[i];
			if (copy.appearances[i] != null) { copy.addReference(copy.appearances[i]); }
		}

		return copy;
	}

	public Appearance getAppearance(int index)
	{
		if (index < 0 || index >= submeshes.length) { throw new IndexOutOfBoundsException("Cannot get invalid appearance index"); }

		return this.appearances[index];
	}

	public IndexBuffer getIndexBuffer(int index)
	{
		if (index < 0 || index >= submeshes.length) { throw new IndexOutOfBoundsException("Cannot get invalid index buffer index"); }

		return this.submeshes[index];
	}

	public int getSubmeshCount() { return this.submeshes.length; }

	public VertexBuffer getVertexBuffer() { return this.vertices; }

	public void setAppearance(int index, Appearance appearance)
	{
		if (index < 0 || index >= submeshes.length) { throw new IndexOutOfBoundsException("Cannot set to invalid appearance index"); }
		removeReference(this.appearances[index]);
		this.appearances[index] = appearance;
		addReference(this.appearances[index]);
	}
}
