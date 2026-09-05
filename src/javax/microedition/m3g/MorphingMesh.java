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

public class MorphingMesh extends Mesh
{
	private VertexBuffer[] targets;
	private float[] weights;
	private VertexBuffer morphedVertices;

	private MorphingMesh() { }

	public MorphingMesh(VertexBuffer base, VertexBuffer[] targets, IndexBuffer[] submeshes, Appearance[] appearances)
	{
		super(base, submeshes, appearances);
		checkTargets(targets);

		this.targets = new VertexBuffer[targets.length];
		this.weights = new float[targets.length];

		for (int i = 0; i < targets.length; i++)
		{
			this.targets[i] = targets[i];
			addReference(this.targets[i]);
		}
	}

	public MorphingMesh(VertexBuffer base, VertexBuffer[] targets, IndexBuffer submeshes, Appearance appearances)
	{
		super(base, submeshes, appearances);
		checkTargets(targets);

		this.targets = new VertexBuffer[targets.length];
		this.weights = new float[targets.length];

		for (int i = 0; i < targets.length; i++)
		{
			this.targets[i] = targets[i];
			addReference(this.targets[i]);
		}
	}

	protected Object3D duplicateImpl()
	{
		MorphingMesh copy = (MorphingMesh) super.duplicateImpl();

		copy.targets = new VertexBuffer[this.targets.length];
		copy.weights = new float[this.targets.length];

		for (int i = 0; i < this.targets.length; i++)
		{
			copy.targets[i] = this.targets[i];
			copy.weights[i] = this.weights[i];
			copy.addReference(copy.targets[i]);
		}

		/* The morphed buffer is a lazily built cache; the copy builds its own. */
		copy.morphedVertices = null;

		return copy;
	}

	public VertexBuffer getMorphTarget(int index)
	{
		if (index < 0 || index >= targets.length)
		{
			throw new IndexOutOfBoundsException("Morph target index out of bounds: " + index);
		}

		return targets[index];
	}

	public int getMorphTargetCount() { return targets.length; }

	public void setWeights(float[] weights)
	{
		if (weights == null)
		{
			throw new NullPointerException("Weights must not be null");
		}

		if (weights.length < getMorphTargetCount())
		{
			throw new IllegalArgumentException("Number of weights must be greater or equal to getMorphTargetCount()");
		}

		System.arraycopy(weights, 0, this.weights, 0, targets.length);
		this.dirtyBits[1] = true;
	}

	public void getWeights(float[] weights)
	{
		if (weights == null)
		{
			throw new NullPointerException("Weights must not be null");
		}
		if (weights.length < getMorphTargetCount())
		{
			throw new IllegalArgumentException("Number of weights must be greater or equal to getMorphTargetCount()");
		}

		System.arraycopy(this.weights, 0, weights, 0, this.weights.length);
	}

	private void checkTargets(VertexBuffer[] targets)
	{
		if (targets == null) { throw new NullPointerException("MorphingMesh has no Target array"); }
		if (targets.length == 0)
		{
			throw new IllegalArgumentException("Targets array is empty");
		}

		for (int i = 0; i < targets.length; i++)
		{
			/* As per JSR-184, a null element in targets is a NullPointerException. */
			if (targets[i] == null)
			{
				throw new NullPointerException("Morph target at index " + i + " is null");
			}
		}

		/*
		 * As per JSR-184 ("Deferred exceptions"), the requirements that all targets
		 * share the same array set and layout, and that the base is a superset of
		 * them, cannot be enforced until morphing is actually done, that is, when
		 * rendering or picking.
		 */
	}

	/* Attribute selectors for the morphing helpers below. */
	private static final int ATTR_POSITIONS = 0;
	private static final int ATTR_NORMALS = 1;
	private static final int ATTR_COLORS = 2;
	private static final int ATTR_TEXCOORDS = 3;

	@Override
	public VertexBuffer getVertexBuffer()
	{
		VertexBuffer base = super.getVertexBuffer();
		if (targets == null || targets.length == 0 || base == null)
		{
			return base;
		}

		if (morphedVertices == null)
		{
			/*
			 * As per JSR-184 ("Deferred exceptions"), target layout constraints are
			 * validated here, when the resultant mesh is needed for rendering or picking.
			 */
			validateMorphLayout(base);
			createMorphedBuffer(base);
			this.dirtyBits[1] = true;
		}

		if (this.dirtyBits[1])
		{
			this.morphMesh(base);
			this.dirtyBits[1] = false;
		}

		return morphedVertices;
	}

	/*
	 * As per JSR-184, all morph targets must have the same types of arrays with the
	 * same vertex counts, component counts and component sizes, and the base mesh
	 * must be a superset of the targets.
	 */
	private void validateMorphLayout(VertexBuffer base)
	{
		validateArrayFamily(base.getPositions(null), ATTR_POSITIONS, 0);
		validateArrayFamily(base.getNormals(), ATTR_NORMALS, 0);
		validateArrayFamily(base.getColors(), ATTR_COLORS, 0);
		for (int unit = 0; unit < Graphics3D.NUM_TEXTURE_UNITS; unit++)
			{ validateArrayFamily(base.getTexCoords(unit, null), ATTR_TEXCOORDS, unit); }
	}

	private void validateArrayFamily(VertexArray baseArray, int attribute, int unit)
	{
		final VertexArray first = getArray(targets[0], attribute, unit);
		for (int i = 1; i < targets.length; i++)
		{
			final VertexArray other = getArray(targets[i], attribute, unit);
			if ((first == null) != (other == null) || (first != null && !sameLayout(first, other)))
				{ throw new IllegalStateException("Morph targets have different vertex array sets or layouts"); }
		}
		if (first != null && (baseArray == null || !sameLayout(first, baseArray)))
			{ throw new IllegalStateException("Base mesh is not a superset of the morph targets"); }
	}

	private static boolean sameLayout(VertexArray a, VertexArray b)
	{
		return a.getVertexCount() == b.getVertexCount() &&
			a.getComponentCount() == b.getComponentCount() &&
			a.getComponentType() == b.getComponentType();
	}

	private static VertexArray getArray(VertexBuffer buffer, int attribute, int unit)
	{
		switch (attribute)
		{
			case ATTR_POSITIONS: return buffer.getPositions(null);
			case ATTR_NORMALS: return buffer.getNormals();
			case ATTR_COLORS: return buffer.getColors();
			default: return buffer.getTexCoords(unit, null);
		}
	}

	/*
	 * The resultant buffer clones every base array that will be morphed and shares
	 * the rest. As per JSR-184, scale and bias are always taken from the base mesh
	 * as such, and arrays absent from the targets are copied from the base.
	 */
	private void createMorphedBuffer(VertexBuffer base)
	{
		morphedVertices = (VertexBuffer) base.duplicate();

		float[] scaleBias = new float[4];
		VertexArray array = base.getPositions(scaleBias);
		if (array != null && getArray(targets[0], ATTR_POSITIONS, 0) != null)
		{
			morphedVertices.setPositions((VertexArray) array.duplicate(), scaleBias[0],
				new float[] { scaleBias[1], scaleBias[2], scaleBias[3] });
		}

		array = base.getNormals();
		if (array != null && getArray(targets[0], ATTR_NORMALS, 0) != null)
			{ morphedVertices.setNormals((VertexArray) array.duplicate()); }

		array = base.getColors();
		if (array != null && getArray(targets[0], ATTR_COLORS, 0) != null)
			{ morphedVertices.setColors((VertexArray) array.duplicate()); }

		for (int unit = 0; unit < Graphics3D.NUM_TEXTURE_UNITS; unit++)
		{
			array = base.getTexCoords(unit, scaleBias);
			if (array != null && getArray(targets[0], ATTR_TEXCOORDS, unit) != null)
			{
				float[] bias = new float[array.getComponentCount()];
				System.arraycopy(scaleBias, 1, bias, 0, bias.length);
				morphedVertices.setTexCoords(unit, (VertexArray) array.duplicate(), scaleBias[0], bias);
			}
		}
	}

	/*
	 * As per JSR-184, the resultant mesh is R = B + sum[ wi (Ti - B) ], applied to
	 * the VertexBuffer default color and to every array present in the morph targets.
	 */
	private void morphMesh(VertexBuffer base)
	{
		morphDefaultColor(base);
		morphArray(base.getPositions(null), ATTR_POSITIONS, 0, false);
		morphArray(base.getNormals(), ATTR_NORMALS, 0, false);
		/* Colors are unsigned 8-bit values, as rendering reads them via toUnsignedInt. */
		morphArray(base.getColors(), ATTR_COLORS, 0, true);
		for (int unit = 0; unit < Graphics3D.NUM_TEXTURE_UNITS; unit++)
			{ morphArray(base.getTexCoords(unit, null), ATTR_TEXCOORDS, unit, false); }
	}

	private void morphDefaultColor(VertexBuffer base)
	{
		final int baseColor = base.getDefaultColor();
		int result = 0;

		/* Morph each ARGB channel independently and clamp to the unsigned byte range. */
		for (int shift = 0; shift <= 24; shift += 8)
		{
			final int baseComponent = (baseColor >>> shift) & 0xFF;
			float acc = baseComponent;
			for (int t = 0; t < targets.length; t++)
				{ acc += weights[t] * (((targets[t].getDefaultColor() >>> shift) & 0xFF) - baseComponent); }
			result |= M3GMath.max(0, M3GMath.min(255, M3GMath.round(acc))) << shift;
		}
		morphedVertices.setDefaultColor(result);
	}

	private void morphArray(VertexArray baseArray, int attribute, int unit, boolean unsigned)
	{
		if (baseArray == null || getArray(targets[0], attribute, unit) == null) { return; }

		final VertexArray output = getArray(morphedVertices, attribute, unit);
		final int numVertices = baseArray.getVertexCount();
		final int totalElements = numVertices * baseArray.getComponentCount();

		// VertexArray components are either byte or short sized.
		// We must handle both cases separately.
		if (baseArray.getComponentType() == 1)
		{
			byte[] baseRaw = new byte[totalElements];
			byte[][] targetRaw = new byte[targets.length][totalElements];
			byte[] outRaw = new byte[totalElements];
			baseArray.get(0, numVertices, baseRaw);
			for (int t = 0; t < targets.length; t++)
				{ getArray(targets[t], attribute, unit).get(0, numVertices, targetRaw[t]); }

			for (int i = 0; i < totalElements; i++)
			{
				final int baseValue = unsigned ? (baseRaw[i] & 0xFF) : baseRaw[i];
				float acc = baseValue;
				for (int t = 0; t < targets.length; t++)
				{
					final int targetValue = unsigned ? (targetRaw[t][i] & 0xFF) : targetRaw[t][i];
					acc += weights[t] * (targetValue - baseValue);
				}
				final int val = M3GMath.round(acc);
				outRaw[i] = (byte) (unsigned ? M3GMath.max(0, M3GMath.min(255, val))
					: M3GMath.max(-128, M3GMath.min(127, val)));
			}
			output.set(0, numVertices, outRaw);
		}
		else
		{
			short[] baseRaw = new short[totalElements];
			short[][] targetRaw = new short[targets.length][totalElements];
			short[] outRaw = new short[totalElements];
			baseArray.get(0, numVertices, baseRaw);
			for (int t = 0; t < targets.length; t++)
				{ getArray(targets[t], attribute, unit).get(0, numVertices, targetRaw[t]); }

			for (int i = 0; i < totalElements; i++)
			{
				float acc = baseRaw[i];
				for (int t = 0; t < targets.length; t++)
					{ acc += weights[t] * (targetRaw[t][i] - baseRaw[i]); }
				outRaw[i] = (short) M3GMath.max(-32768, M3GMath.min(32767, M3GMath.round(acc)));
			}
			output.set(0, numVertices, outRaw);
		}
	}

	@Override
	public void updateProperty(int property, float[] value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Graphics3D.class.getPackage().getName() + "." + Graphics3D.class.getSimpleName() + ": " + "AnimTrack updating MorphingMesh property");
		switch (property)
		{
			case AnimationTrack.MORPH_WEIGHTS:
				int count = Math.min(targets.length, value.length);
				for (int i = 0; i < count; i++) { weights[i] = value[i]; }
				for (int i = count; i < targets.length; i++) { weights[i] = 0.0f; }
				this.dirtyBits[1] = true;
				break;
			default:
				super.updateProperty(property, value);
		}
	}

	boolean animTrackCompatible(AnimationTrack track)
	{
		switch (track.getTargetProperty())
		{
			case AnimationTrack.MORPH_WEIGHTS:
				return true;
			default:
				return super.animTrackCompatible(track);
		}
	}
}
