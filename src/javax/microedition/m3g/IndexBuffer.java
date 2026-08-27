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

public abstract class IndexBuffer extends Object3D
{
	protected int indexCount;
	protected int[] indices;

	protected Object3D duplicateImpl()
	{
		IndexBuffer copy = (IndexBuffer) super.duplicateImpl();
		copy.indices = this.indices == null ? null : (int[]) this.indices.clone();
		copy.indexCount = this.indexCount;
		return copy;
	}

	public int getIndexCount() { return this.indexCount; }

	public void getIndices(int[] indices)
	{
		/* As per JSR-184, throw NullPointerException if the received indices is null. */
		if(indices == null) {throw new NullPointerException("Tried to get buffer's vertex indices without providing the actual indices."); }

		/* Also per JSR-184, throw IllegalArgumentException if indices.length < getIndexCount. */
		if (indices.length < getIndexCount())
			{ throw new IllegalArgumentException("Tried to return vertex indices to an array that's smaller than the object's number of indices."); }

		if (getIndexCount() > 0 && this.indices != null) { System.arraycopy(this.indices, 0, indices, 0, getIndexCount()); }
	}

	// Faster alternative to the above that doesn't copy the index array, used by Graphics3D's render()
	public int[] getIndexArray() { return indices; }
}
