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

import org.recompile.mobile.Mobile;

public class VertexBuffer extends Object3D
{

	// The `fixed` field represents whether or not the vertex count (`length`)
	// of this `VertexBuffer` has been determined.
	//
	// The first `VertexArray` added to this `VertexBuffer` makes
	// it "fixed", and the `length` will be set to the number
	// of vertices in the `VertexArray`.
	//
	// Once the `VertexBuffer` is fixed, it only accepts `VertexArray`s
	// with exactly `length` vertices.
	private boolean fixed;
	private int length;
	private int defaultColor;
	private VertexArray positions;
	private VertexArray normals;
	private VertexArray colors;
	private VertexArray[] texCoords;

	private float positionScale;
	private float[] positionBias;
	private float[] texCoordScale;
	private float[][] texCoordBias;
	// colorScale =   1/255
	// colorBias  = 128/255


	public VertexBuffer()
	{
		this.fixed = false;
		this.length = 0;
		this.defaultColor = 0xffffffff;
		this.positions = null;
		this.normals = null;
		this.colors = null;
		this.positionScale = 1.0f;
		this.positionBias = new float[3];
		this.texCoords = new VertexArray[Graphics3D.NUM_TEXTURE_UNITS];
		this.texCoordScale = new float[Graphics3D.NUM_TEXTURE_UNITS];
		this.texCoordBias = new float[Graphics3D.NUM_TEXTURE_UNITS][0];
	}

	protected Object3D duplicateImpl()
	{
		VertexBuffer copy = (VertexBuffer) super.duplicateImpl();

		copy.positions = this.positions;
        copy.normals = this.normals;
        copy.colors = this.colors;

		copy.texCoords = (VertexArray[]) this.texCoords.clone();
		copy.positionBias = (float[]) this.positionBias.clone();
		copy.texCoordBias = new float[this.texCoordBias.length][];
		copy.texCoordScale = (float[]) this.texCoordScale.clone();

		// We may have some texture units unset, so check for nulls before cloning.
		for(int i = 0; i < texCoordBias.length; i++)
		{
			if (this.texCoordBias[i] != null)
	        {
	            copy.texCoordBias[i] = (float[]) this.texCoordBias[i].clone();
	        }
		}

		if (copy.positions != null) { copy.addReference(copy.positions); }
		if (copy.normals != null) { copy.addReference(copy.normals); }
		if (copy.colors != null) { copy.addReference(copy.colors); }
		if (copy.texCoords != null)
		{
			for (int i = 0; i < Graphics3D.NUM_TEXTURE_UNITS; i++)
			{
            	if (copy.texCoords[i] != null) { copy.addReference(copy.texCoords[i]); }
			}
		}

		return copy;
	}

	public VertexArray getColors() { return this.colors; }

	public int getDefaultColor() { return this.defaultColor; }

	public VertexArray getNormals() { return this.normals; }

	public VertexArray getPositions(float[] scaleBias)
	{
		if (scaleBias != null)
		{
			/* As per JSR-184, throw IllegalArgumentException if (scaleBias != null) && (scaleBias.length < 4). */
			if(scaleBias.length < 4) { throw new IllegalArgumentException("ScaleBias has invalid length (less than 4)."); }

			scaleBias[0] = this.positionScale;
			System.arraycopy(this.positionBias, 0, scaleBias, 1, 3);
		}

		return this.positions;
	}

	public VertexArray getTexCoords(int index, float[] scaleBias)
	{
		/* As per JSR-184, throw IndexOutOfBoundsException if index != [0,N] where N is the implementation specific maximum texturing unit index*/
		if (index < 0 || index >= Graphics3D.NUM_TEXTURE_UNITS)
			{ throw new IndexOutOfBoundsException("Tried to access invalid texture unit index."); }

		if (scaleBias != null && this.texCoords[index] != null)
		{
			int components = this.texCoords[index].getComponentCount();
			/* Also per JSR-184, throw IllegalArgumentException if (scaleBias != null) && (scaleBias.length < texCoords.getComponentCount+1). */
			if (scaleBias.length < components + 1)
				{ throw new IllegalArgumentException("Invalid scaleBias length."); }

			scaleBias[0] = this.texCoordScale[index];
			System.arraycopy(this.texCoordBias[index], 0, scaleBias, 1, components);
		}

		return this.texCoords[index];
	}

	public int getVertexCount() { return this.length; }

	public void setColors(VertexArray colors)
	{
		if (colors != null)
		{
			/*
			 * As per JSR-184, throw IllegalArgumentException if:
			 * (colors != null) && (colors.getComponentType != 1)
			 * (colors != null) && (colors.getComponentCount != {3,4})
			 * (colors != null) && (colors.getVertexCount != getVertexCount) && (at least one other VertexArray is set)
			 */
			if (colors.getComponentType() != 1 || colors.getComponentCount() < 3 || 4 < colors.getComponentCount() || (this.fixed && colors.getVertexCount() != this.length))
				{ throw new IllegalArgumentException("Trying to set colors with invalid context."); }

			removeReference(this.colors);
			this.updateLength(colors.getVertexCount());
			this.colors = colors;
			addReference(this.colors);
		}
		else
		{
			removeReference(this.colors);
			this.colors = colors;
			this.checkUnfix();
		}
	}

	public void setDefaultColor(int ARGB) { this.defaultColor = ARGB; }

	public void setNormals(VertexArray normals)
	{
		if (normals != null)
		{
			/*
			 * As per JSR-184, throw IllegalArgumentException if:
			 * (normals != null) && (normals.getComponentCount != 3)
			 * (normals != null) && (normals.getVertexCount != getVertexCount) && (at least one other VertexArray is set)
			 */
			if (normals.getComponentCount() != 3 || (this.fixed && normals.getVertexCount() != this.length))
				{ throw new IllegalArgumentException("Trying to set colors with invalid context."); }

			removeReference(this.normals);
			this.updateLength(normals.getVertexCount());
			this.normals = normals;
			addReference(this.normals);
		}
		else
		{
			removeReference(this.normals);
			this.normals = normals;
			this.checkUnfix();
		}
	}

	public void setPositions(VertexArray positions, float scale, float[] bias)
	{
		if (positions != null)
		{
			/*
			 * As per JSR-184, throw IllegalArgumentException if:
			 * (positions != null) && (positions.getComponentCount != 3
			 * (positions != null) && (positions.getVertexCount != getVertexCount) && (at least one other VertexArray is set)
			 * (positions != null) && (bias != null) && (bias.length < 3)
			 */
			if (positions.getComponentCount() != 3 || (this.fixed && positions.getVertexCount() != this.length) || (bias != null && bias.length < 3))
				{ throw new IllegalArgumentException("Trying to set positions with invalid context."); }

			if (bias == null) { bias = new float[3]; }

			removeReference(this.positions);
			this.updateLength(positions.getVertexCount());
			this.positions = positions;
			addReference(this.positions);
			this.positionScale = scale;
			if (bias != null) { System.arraycopy(bias, 0, this.positionBias, 0, 3); }
		}
		else
		{
			removeReference(this.positions);
			this.positions = positions;
			this.positionScale = 1.0f; // Maybe we don't need to reset it here?
			this.positionBias[0] = 0.0f;
			this.positionBias[1] = 0.0f;
			this.positionBias[2] = 0.0f;
			this.checkUnfix();
		}
	}

	public void setTexCoords(int index, VertexArray texCoords, float scale, float[] bias)
	{
		/* As per JSR-184, throw IndexOutOfBoundsException if if index != [0,N] where N is the implementation specific maximum texturing unit index. */
		if (index < 0 || index >= Graphics3D.NUM_TEXTURE_UNITS)
			{ throw new IndexOutOfBoundsException("Tried to access invalid texture unit index."); }

		if (texCoords != null)
		{
			int componentCount = texCoords.getComponentCount();

			/*
			 * Also per JSR-184, throw IllegalArgumentException if:
			 * (texCoords != null) && (texCoords.getComponentCount != {2,3})
			 * (texCoords != null) && (texCoords.getVertexCount != getVertexCount) && (at least one other VertexArray is set)
			 * (texCoords != null) && (bias != null) && (bias.length < texCoords.getComponentCount)
			 */
			if (componentCount < 2 || 3 < componentCount || (this.fixed && texCoords.getVertexCount() != this.length) || (bias != null && bias.length < componentCount))
				{ throw new IllegalArgumentException("Trying to set Texture Coordinates with invalid context."); }

			if (bias == null) { bias = new float[componentCount]; }

			removeReference(this.texCoords[index]);
			this.updateLength(texCoords.getVertexCount());
			this.texCoords[index] = texCoords;
			addReference(this.texCoords[index]);

			this.texCoordScale[index] = scale;
			this.texCoordBias[index] = new float[componentCount];
			if (bias != null) { System.arraycopy(bias, 0, this.texCoordBias[index], 0, componentCount); }
		}
		else
		{
			removeReference(this.texCoords[index]);
			this.texCoords[index] = texCoords;
			this.texCoordScale[index] = 1.0f; // Maybe we don't need to reset it here?
			this.texCoordBias[index] = null;
			this.checkUnfix();
		}
	}

	private void updateLength(int length)
	{
		if (!this.fixed)
		{
			this.fixed = true;
			this.length = length;
		}
	}

	// This VertexBuffer may be fully reset at some point, and when every
	// VertexArray in it is null, it can get a new fixed size.
	private void checkUnfix()
    {
        if (this.positions != null || this.normals != null || this.colors != null) { return; }
        for (VertexArray texCoord : this.texCoords)
        {
            if (texCoord != null) { return; }
        }

        // All VertexArrays are null, we can work with a new length once new data comes in
        this.fixed = false;
        this.length = 0;
    }

	@Override
	void updateProperty(int property, float[] value)
	{
		Mobile.log(Mobile.LOG_WARNING, Graphics3D.class.getPackage().getName() + "." + Graphics3D.class.getSimpleName() + ": " + "AnimTrack updating VertexBuffer property");
		switch (property)
		{
			case AnimationTrack.ALPHA:
				int alpha = M3GMath.min(255, M3GMath.max(0, M3GMath.roundPositive(value[0] * 255.0f)));
				defaultColor = (defaultColor & 0x00FFFFFF) | (alpha << 24);
				break;
			case AnimationTrack.COLOR:
				int r = M3GMath.min(255, M3GMath.max(0, M3GMath.roundPositive(value[0] * 255.0f)));
				int g = M3GMath.min(255, M3GMath.max(0, M3GMath.roundPositive(value[1] * 255.0f)));
				int b = M3GMath.min(255, M3GMath.max(0, M3GMath.roundPositive(value[2] * 255.0f)));
				defaultColor = (defaultColor & 0xFF000000) | (r << 16) | (g << 8) | b;
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
			case AnimationTrack.COLOR:
				return true;
			default:
				return super.animTrackCompatible(track);
		}
	}
}
