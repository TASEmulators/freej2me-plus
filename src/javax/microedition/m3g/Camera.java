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

public class Camera extends Node
{
	public static final int GENERIC = 48;
	public static final int PARALLEL = 49;
	public static final int PERSPECTIVE = 50;

	private int projMode; /* Can be set to one of the projection modes above. */
	private float[] projMatrix; /* A 4D Projection Matrix represented as 1D array to allow the direct usage of Transform.get() and Transform.set(). */
	/* Based on JSR-184, the camera object has 4 main parameters: fovy, aspectRatio, near, and far. */
	private float[] params;
	private boolean dirtyMatrix = false;

	public Camera()
	{
		this.projMode = GENERIC;
		this.projMatrix = new float[]
		{
			1, 0, 0, 0,
			0, 1, 0, 0,
			0, 0, 1, 0,
			0, 0, 0, 1
		};
		this.params = new float[4];
	}

	protected Object3D duplicateImpl()
	{
		Camera copy = (Camera) super.duplicateImpl();
		copy.projMode = this.projMode;
		copy.projMatrix = this.projMatrix.clone();
		copy.params = this.params.clone();
		copy.dirtyMatrix = this.dirtyMatrix;
		return copy;
	}


	public int getProjection(float[] params)
	{
		/* As per JSR-184, throw IllegalArgumentException if the received params array has less than 4 positions. */
		if(params != null && params.length < 4)
		 { throw new IllegalArgumentException("Invalid params array. "); }

		if (this.projMode != GENERIC && params != null) { System.arraycopy(this.params, 0, params, 0, 4); }

		return this.projMode;
	}

	public int getProjection(Transform transform)
	{
		if (transform != null)
		{
			/*
			 * As per JSR-184, throw ArithmeticException if the transform matrix cannot be computed because of
			 * illegal perspective or parallel projection parameters.
			 */
			if (this.projMode != GENERIC && this.params[2] == this.params[3])
				{ throw new ArithmeticException("Illegal projection parameters."); }

			/*
			 * The computation of the projection matrix is only done if requested.
			 * To prevent repeating the same computation, the matrix is cached.
			 * This cache is re-set to null by `setParallel` and `setPerspective`.
			 */
			if (dirtyMatrix) { this.computeMatrix(); }
		}
		/* Copies the current projection matrix to the given transform, and returns the current projection mode. */
		transform.set(this.projMatrix);
		return this.projMode;
	}

	public void setGeneric(Transform transform)
	{
		/* As per JSR-184, throw NullPointerException if the received transform is null. */
		if(transform == null) { throw new NullPointerException("Tried to set GENERIC projection mode without providing a transform."); }

		/* Copies the current camera projection matrix to the given transform, and sets projection mode to GENERIC. */
		transform.get(this.projMatrix);
		this.projMode = GENERIC;
		this.dirtyMatrix = false;
	}

	public void setParallel(float fovy, float aspectRatio, float near, float far)
	{
		/* As per JSR-18, throw IllegalArgumentException if height or aspectRatio <= 0. */
		if(fovy <= 0 || aspectRatio <= 0)
			{ throw new IllegalArgumentException("Tried to set parallel projection with negative FOV or aspect ratio."); }

		/* Clears the Projection Matrix (it has to be computed again), sets the mode to PARALLEL projection, and sets the camera parameters. */
		this.projMode = PARALLEL;
		this.params[0] = fovy;
		this.params[1] = aspectRatio;
		this.params[2] = near;
		this.params[3] = far;
		this.dirtyMatrix = true;
	}

	public void setPerspective(float fovy, float aspectRatio, float near, float far)
	{
		/* As per JSR-184, throw IllegalArgumentException if any of the arguments is <= 0, or fovy > 180. */
		if(fovy <= 0 || 180 <= fovy || aspectRatio <= 0 || near <= 0 || far <= 0)
			{ throw new IllegalArgumentException("Tried to set perspective projection with invalid parameters."); }

		/* Clears the Projection Matrix (it has to be computed again), sets the mode to PERSPECTIVE projection, and sets the camera parameters. */
		this.projMode = PERSPECTIVE;
		this.params[0] = fovy;
		this.params[1] = aspectRatio;
		this.params[2] = near;
		this.params[3] = far;
		this.dirtyMatrix = true;
	}

	private void computeMatrix()
	{
		if (this.projMode == GENERIC)
		{
			this.dirtyMatrix = false;
			return;
		}

		float fovy = this.params[0];
		float aspectRatio = this.params[1];
		float near = this.params[2];
		float far = this.params[3];

		float h, w, d, b;

		if (this.projMode == PARALLEL)
		{
			h = fovy;
			w = aspectRatio * h;
			d = far - near;
			b = near + far;

			this.projMatrix[0]  = 2.0f / w;  this.projMatrix[1]  = 0.0f;     this.projMatrix[2]  = 0.0f;       this.projMatrix[3]  = 0.0f;
			this.projMatrix[4]  = 0.0f;      this.projMatrix[5]  = 2.0f / h; this.projMatrix[6]  = 0.0f;       this.projMatrix[7]  = 0.0f;
			this.projMatrix[8]  = 0.0f;      this.projMatrix[9]  = 0.0f;     this.projMatrix[10] = -2.0f / d;  this.projMatrix[11] = -b / d;
			this.projMatrix[12] = 0.0f;      this.projMatrix[13] = 0.0f;     this.projMatrix[14] = 0.0f;       this.projMatrix[15] = 1.0f;
		}
		else if (this.projMode == PERSPECTIVE)
		{
			h = M3GMath.tan(M3GMath.toRadians(fovy) / 2.0f);
			w = aspectRatio * h;
			d = far - near;
			b = near + far;

			this.projMatrix[0]  = 1.0f / w;  this.projMatrix[1]  = 0.0f;     this.projMatrix[2]  = 0.0f;       this.projMatrix[3]  = 0.0f;
			this.projMatrix[4]  = 0.0f;      this.projMatrix[5]  = 1.0f / h; this.projMatrix[6]  = 0.0f;       this.projMatrix[7]  = 0.0f;
			this.projMatrix[8]  = 0.0f;      this.projMatrix[9]  = 0.0f;     this.projMatrix[10] = -b / d;     this.projMatrix[11] = (-2.0f * near * far) / d;
			this.projMatrix[12] = 0.0f;      this.projMatrix[13] = 0.0f;     this.projMatrix[14] = -1.0f;      this.projMatrix[15] = 0.0f;
		}

		this.dirtyMatrix = false;
	}

	void updateProperty(int property, float[] value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Graphics3D.class.getPackage().getName() + "." + Graphics3D.class.getSimpleName() + ": " + "AnimTrack updating camera property");
		switch (property)
		{
			case AnimationTrack.FAR_DISTANCE:
				params[3] = (projMode == PERSPECTIVE) ? M3GMath.max(0.0001f, value[0]) : value[0];
				break;
			case AnimationTrack.FIELD_OF_VIEW:
				params[0] = (projMode == PERSPECTIVE) ? M3GMath.max(0.0001f, M3GMath.min(179.999f, value[0])) : M3GMath.max(0.0001f, value[0]);
				break;
			case AnimationTrack.NEAR_DISTANCE:
				params[2] = (projMode == PERSPECTIVE) ? M3GMath.max(0.0001f, value[0]) : value[0];
				break;
			default:
				super.updateProperty(property, value);
		}

		if (projMode != GENERIC) { this.dirtyMatrix = true; }
	}

	boolean animTrackCompatible(AnimationTrack track)
	{
		switch (track.getTargetProperty())
		{
			case AnimationTrack.FAR_DISTANCE:
			case AnimationTrack.FIELD_OF_VIEW:
			case AnimationTrack.NEAR_DISTANCE:
				return true;
			default:
				return super.animTrackCompatible(track);
		}
	}
}
