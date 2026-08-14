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

		copy.weights = (float[]) this.weights.clone();
		copy.targets = new VertexBuffer[this.targets.length];

		for (int i = 0; i < this.targets.length; i++)
		{
			copy.targets[i] = this.targets[i];
			copy.addReference(copy.targets[i]);
		}

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

		VertexBuffer baseBuffer = getVertexBuffer();
		int baseVertexCount = (baseBuffer != null) ? baseBuffer.getVertexCount() : -1;

		for (int i = 0; i < targets.length; i++)
		{
			if (targets[i] == null)
			{
				throw new IllegalArgumentException("Morph target at index " + i + " is null");
			}
			if (targets[i].getVertexCount() == 0)
			{
				throw new IllegalArgumentException("Morph target at index " + i + " has no vertices");
			}
			if (baseVertexCount != -1 && targets[i].getVertexCount() != baseVertexCount)
			{
				throw new IllegalArgumentException("Morth target count and vertex count differ.");
			}
		}
	}

	@Override
	public void updateProperty(int property, float[] value)
	{
		Mobile.log(Mobile.LOG_WARNING, Graphics3D.class.getPackage().getName() + "." + Graphics3D.class.getSimpleName() + ": " + "AnimTrack updating morphingMesh property");
		switch (property)
		{
			case AnimationTrack.MORPH_WEIGHTS:
				int count = Math.min(targets.length, value.length);
				for (int i = 0; i < count; i++) { weights[i] = value[i]; }
				for (int i = count; i < targets.length; i++) { weights[i] = 0.0f; }
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
