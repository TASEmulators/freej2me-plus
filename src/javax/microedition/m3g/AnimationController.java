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

public class AnimationController extends Object3D
{

	private int activationTime = 0;
	private int deactivationTime = 0;
	private float speed = 1.0f;
	private int refWorldTime = 0;
	private float refSequenceTime = 0;
	private float weight = 1.0f;

	protected Object3D duplicateImpl()
	{
		AnimationController copy = (AnimationController) super.duplicateImpl();
		copy.activationTime = this.activationTime;
		copy.deactivationTime = this.deactivationTime;
		copy.speed = this.speed;
		copy.refWorldTime = this.refWorldTime;
		copy.refSequenceTime = this.refSequenceTime;
		copy.weight = this.weight;
		return copy;
	}

	public boolean isActive(int worldTime)
	{
		// start == end? Then the controller is active at all world times
		if (activationTime == deactivationTime) { return true; }

		return (worldTime >= activationTime && worldTime < deactivationTime);
	}

	public int timeToActivation(int worldTime)
	{
		if (worldTime < activationTime) { return activationTime - worldTime; }
		else if (worldTime <= deactivationTime) { return 0; }

		return Integer.MAX_VALUE;
	}

	public int timeToDeactivation(int worldTime)
	{
		if (worldTime < deactivationTime) { return deactivationTime - worldTime; }

		return Integer.MAX_VALUE;
	}

	public void setActiveInterval(int start, int end)
	{
		if (start > end)
			throw new IllegalArgumentException("Start time must be inferior to end time");

		activationTime = start;
		deactivationTime = end;
	}

	public int getActiveIntervalStart() { return activationTime; }

	public int getActiveIntervalEnd() { return deactivationTime; }

	public void setSpeed(float speed, int worldTime)
	{
		this.refSequenceTime = getPosition(worldTime);
		this.refWorldTime = worldTime;
		this.speed = speed;
	}

	public float getSpeed() { return speed; }

	public void setPosition(float sequenceTime, int worldTime)
	{
		this.refSequenceTime = sequenceTime;
		this.refWorldTime = worldTime;
	}

	public float getPosition(int worldTime)
	{
		return (refSequenceTime + (speed * (float) (worldTime - refWorldTime)));
	}

	public int getRefWorldTime() { return refWorldTime; }

	public void setWeight(float weight)
	{
		if (weight < 0)
			throw new IllegalArgumentException("Weight must be positive or zero");
		this.weight = weight;
	}

	public float getWeight() { return weight; }
}
