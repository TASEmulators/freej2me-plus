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

public class AnimationTrack extends Object3D
{

	public static final int ALPHA = 256;
	public static final int AMBIENT_COLOR = 257;
	public static final int COLOR = 258;
	public static final int CROP = 259;
	public static final int DENSITY = 260;
	public static final int DIFFUSE_COLOR = 261;
	public static final int EMISSIVE_COLOR = 262;
	public static final int FAR_DISTANCE = 263;
	public static final int FIELD_OF_VIEW = 264;
	public static final int INTENSITY = 265;
	public static final int MORPH_WEIGHTS = 266;
	public static final int NEAR_DISTANCE = 267;
	public static final int ORIENTATION = 268;
	public static final int PICKABILITY = 269;
	public static final int SCALE = 270;
	public static final int SHININESS = 271;
	public static final int SPECULAR_COLOR = 272;
	public static final int SPOT_ANGLE = 273;
	public static final int SPOT_EXPONENT = 274;
	public static final int TRANSLATION = 275;
	public static final int VISIBILITY = 276;


	public KeyframeSequence sequence;
	public int property;
	private AnimationController controller;

	private float[] sample;

	public AnimationTrack(KeyframeSequence sequence, int property)
	{
		if (sequence == null) { throw new NullPointerException("Sequence must not be null"); }
		if ((property < ALPHA) || (property > VISIBILITY)) { throw new IllegalArgumentException("Unknown property"); }
		if (!isCompatible(sequence.getComponentCount(), property)) { throw new IllegalArgumentException("Sequence is not compatible with property"); }
		this.sequence = sequence;
		this.property = property;
		addReference(this.sequence);
	}

	protected Object3D duplicateImpl()
	{
		AnimationTrack copy = (AnimationTrack) super.duplicateImpl();
		copy.sequence = this.sequence;
		copy.property = this.property;
		copy.setController(this.controller);

		addReference(copy.sequence);
		return copy;
	}

	public void getContribution(int time, float[] accumSamples, float[] weight, int[] validity)
	{
		if (this.controller == null || !controller.isActive(time))
		{
			weight[0] = 0;
			int timeToAct = (controller != null) ? controller.timeToActivation(time) : Integer.MAX_VALUE;
			validity[0] = M3GMath.max(1, timeToAct);
			return;
		}

		weight[0] = controller.getWeight();

		if (weight[0] <= 0.0f)
		{
			validity[0] = Integer.MAX_VALUE;
			return;
		}

		int sampleLength = sequence.getComponentCount();

		if (this.sample == null || this.sample.length < sampleLength)
			{ this.sample = new float[sampleLength]; }

		float speed = controller.getSpeed();
		float sampleTime = controller.getPosition(time);
		validity[0] = sequence.getSample(sampleTime, this.sample);

		int worldValidity;
		if (speed == 0.0f) { worldValidity = Integer.MAX_VALUE; }
		else { worldValidity = (int) M3GMath.roundPositive(validity[0] / M3GMath.abs(speed)); }

		int timeToDeact = controller.timeToDeactivation(time);
		validity[0] = M3GMath.max(1, M3GMath.min(worldValidity, timeToDeact));

		for (int i = 0; i < sampleLength; i++) { accumSamples[i] += this.sample[i]; }
	}

	public AnimationController getController() { return controller; }

	public void setController(AnimationController controller)
	{
		removeReference(this.controller);
		this.controller = controller;
		addReference(this.controller);
	}

	public int getTargetProperty() { return property; }

	public KeyframeSequence getKeyframeSequence() { return sequence; }

	private boolean isCompatible(int components, int property)
	{
		switch (property)
		{
			case ALPHA:
			case DENSITY:
			case FAR_DISTANCE:
			case FIELD_OF_VIEW:
			case INTENSITY:
			case NEAR_DISTANCE:
			case PICKABILITY:
			case SHININESS:
			case SPOT_ANGLE:
			case SPOT_EXPONENT:
			case VISIBILITY:
				return components == 1;
			case CROP:
				return components == 2 || components == 4;
			case AMBIENT_COLOR:
			case COLOR:
			case DIFFUSE_COLOR:
			case EMISSIVE_COLOR:
			case SPECULAR_COLOR:
			case TRANSLATION:
				return components == 3;
			case SCALE:
				return components == 1 || components == 3;
			case ORIENTATION:
				return components == 4;
			case MORPH_WEIGHTS:
				return components > 0;
			default:
				return false; // Shouldn't occur
		}
	}
}
