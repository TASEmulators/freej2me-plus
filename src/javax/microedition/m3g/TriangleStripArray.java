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

public class TriangleStripArray extends IndexBuffer
{

	TriangleStripArray() { }

	public TriangleStripArray(int[] indices, int[] stripLengths)
	{
		/* Per JSR-184, throw NullPointerException if indices or stripLengths are null */
		if(indices == null || stripLengths == null) { throw new NullPointerException("Tried to construct a TriangleStripArray with incomplete information."); }

		/*
		 * Also per JSR-184, throw IllegalArgumentException if:
		 * stripLengths is empty, any element in stripLengths is less than 3, or indices.length < sum(stripLengths).
		 */
		int totalStripLength = sum(stripLengths);

		if(stripLengths.length == 0 || indices.length < totalStripLength || hasLengthLessThan3(stripLengths))
			{ throw new IllegalArgumentException("Cannot construct TriangleStripArray, incorrect parameters received."); }

		/* also per JSR-184, throw IndexOutOfBoundsException if any element in indices is negative, or greater than 65535. */
		if(hasInvalidIndices(indices, totalStripLength))
			{ throw new IndexOutOfBoundsException("Index provided to TriangleStripArray is out of bounds."); }

		/* Setup the StripArray with explicit indices. */
		this.updateFields(true, indices, stripLengths);
	}

	public TriangleStripArray(int firstIndex, int[] stripLengths)
	{
		/* As per JSR-184, throw NullPointerException if stripLengths == null. */
		if(stripLengths == null) { throw new NullPointerException("Tried to construct TriangleStripArray with null stripLengths."); }

		/* Also per JSR-184, throw IllegalArgumentException if stripLengths.length == 0 or any element in stripLengths is less than 3. */
		if(stripLengths.length == 0 || hasLengthLessThan3(stripLengths))
			{ throw new IllegalArgumentException("Cannot construct TriangleStripArray, incorrect parameters received."); }

		/* Also per JSR-184, throw IndexOutOfBoundsException if any element in indices is negative, or if firstIndex + sum(stripLengths) is greater than 65535. */
		int totalStripLength = sum(stripLengths);

		if(firstIndex < 0 || firstIndex + totalStripLength > 65535)
			{ throw new IndexOutOfBoundsException("Index provided to TriangleStripArray is out of bounds."); }

		/* Setup the StripArray with implicit indices. */
		this.updateFields(false, new int[] { firstIndex }, stripLengths);
	}

	protected Object3D duplicateImpl() { return (TriangleStripArray) super.duplicateImpl(); }

	private void updateFields(boolean isExplicit, int[] indices, int[] stripLengths)
	{
		/* Update the number of indices from the parent by mapping all valid StripLength elements. */
		super.indexCount = calculateIndexCount(stripLengths);

		/* Update parent's indices by copying this object's indices to it. */
		super.indices = new int[this.indexCount];

		int  in_offset = 0;
		int out_offset = 0;

		for (int strip_id = 0; strip_id < stripLengths.length; strip_id++)
		{
			for (int i = 0; i < (stripLengths[strip_id] - 2); i++)
			{
				int x, y, z;
                int abs_index = in_offset + i;
                boolean swap = (i % 2 == 1);

				if (isExplicit)
				{
					x = indices[abs_index + 0];
					y = indices[abs_index + 1];
					z = indices[abs_index + 2];
				}
				else
				{
					x = indices[0] + abs_index + 0;
					y = indices[0] + abs_index + 1;
					z = indices[0] + abs_index + 2;
				}

				// Swap vertices for odd triangles to maintaining face orientation
				// so that Triangle.java doesn't need to do that in the render loop.
				super.indices[out_offset + 0] = x;
				super.indices[out_offset + 1] = swap ? z : y;
				super.indices[out_offset + 2] = swap ? y : z;

				/* Move to the next vertex on the parent object. */
				out_offset += 3;
			}

			/* Move to the next vertex on this StripArray. */
			in_offset += stripLengths[strip_id];
		}
	}

	private int sum(int[] array)
	{
		int total = 0;
		for (int value : array) { total += value; }

		return total;
	}

	private boolean hasLengthLessThan3(int[] array)
	{
		for (int value : array)
		{
			if (value < 3) { return true; }
		}
		return false;
	}

	private boolean hasInvalidIndices(int[] array, int numIndices)
    {
        for (int i = 0; i < numIndices; i++)
        {
            if (array[i] < 0 || array[i] > 65535) { return true; }
        }

        return false;
    }

	private int calculateIndexCount(int[] stripLengths)
	{
		int total = 0;
		for (int length : stripLengths) { total += (length - 2) * 3; }

		return total;
	}
}
