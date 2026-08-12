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
	private VertexArray colors = null;
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
		this.texCoords = new VertexArray[Graphics3D.NUM_TEXTURE_UNITS];
		this.texCoordScale = new float[Graphics3D.NUM_TEXTURE_UNITS];
		this.texCoordBias = new float[Graphics3D.NUM_TEXTURE_UNITS][0];
	}

	protected Object3D duplicateImpl()
	{
		VertexBuffer copy = (VertexBuffer) super.duplicateImpl();

		copy.positionBias = (float[]) positionBias.clone();
		copy.texCoords = (VertexArray[]) texCoords.clone();
		copy.texCoordBias = new float[texCoordBias.length][];
		for(int i = 0; i < texCoordBias.length; i++) { copy.texCoordBias[i] = (float[]) texCoordBias[i].clone(); }
		copy.texCoordScale = (float[]) texCoordScale.clone();

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
			for (int i = 0; i < 3; i++)
				scaleBias[i + 1] = this.positionBias[i];
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
			/* Also per JSR-184, throw IllegalArgumentException if (scaleBias != null) && (scaleBias.length < texCoords.getComponentCount+1). */
			if (scaleBias.length < this.texCoords[index].getComponentCount() + 1)
				{ throw new IllegalArgumentException("Invalid scaleBias length."); }

			scaleBias[0] = this.texCoordScale[index];
			for (int i = 0; i < this.texCoords[index].getComponentCount(); i++)
				{ scaleBias[i + 1] = this.texCoordBias[index][i]; }
		}
		return this.texCoords[index];
	}

	public int getVertexCount() { return this.length; }

	public void setColors(VertexArray colors)
	{
		if (colors == null) { this.colors = null; }
		else
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
	}

	public void setDefaultColor(int ARGB) { this.defaultColor = ARGB; }

	public void setNormals(VertexArray normals)
	{
		if (normals == null) { this.normals = null; }
		else
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
	}

	public void setPositions(VertexArray positions, float scale, float[] bias)
	{
		if (positions == null) { this.positions = null; }
		else
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
			this.positionBias = bias.clone();

		}
	}

	public void setTexCoords(int index, VertexArray texCoords, float scale, float[] bias)
	{
		/* As per JSR-184, throw IndexOutOfBoundsException if if index != [0,N] where N is the implementation specific maximum texturing unit index. */
		if (index < 0 || index >= Graphics3D.NUM_TEXTURE_UNITS)
			{ throw new IndexOutOfBoundsException("Tried to access invalid texture unit index."); }

		if (texCoords == null) { this.texCoords[index] = null; }
		else
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
			this.texCoordBias[index] = bias.clone();
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

	@Override
	void updateProperty(int property, float[] value)
	{
		Mobile.log(Mobile.LOG_WARNING, Graphics3D.class.getPackage().getName() + "." + Graphics3D.class.getSimpleName() + ": " + "AnimTrack updating VertexBuffer property");
		switch (property)
		{
			case AnimationTrack.ALPHA:
				defaultColor = (defaultColor | 0xFF000000) & ((int) value[0] << 24);
				break;
			case AnimationTrack.COLOR:
				defaultColor = (defaultColor | 0x00FFFFFF) & (int) value[0] >> 16 & (int) value[1] >> 8 & (int) value[2];
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
