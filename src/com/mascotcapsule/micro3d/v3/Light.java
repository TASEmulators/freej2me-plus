/*
 * MIT License
 * Copyright (c) 2026 Yury Kharchenko
 */

package com.mascotcapsule.micro3d.v3;

public class Light {
	Vector3D direction;
	int dirIntensity;
	int ambIntensity;
	
	public Light() {
		direction = new Vector3D(0, 0, 4096);
		dirIntensity = 4096;
		ambIntensity = 0;
	}
	
	public Light(Vector3D dir, int dirIntensity, int ambIntensity) {
		if (dir == null) throw new NullPointerException();
		
		this.direction = new Vector3D(dir);
		this.dirIntensity = dirIntensity;
		this.ambIntensity = ambIntensity;
	}
	
	public final int getParallelLightIntensity() { return dirIntensity; }
	public final int getDirIntensity() { return dirIntensity; }
	
	public final void setParallelLightIntensity(int p) { dirIntensity = p; }
	public final void setDirIntensity(int p) { dirIntensity = p; }
	
	public final int getAmbientIntensity() { return ambIntensity; }
	public final int getAmbIntensity() { return ambIntensity; }
	
	public final void setAmbientIntensity(int p) { ambIntensity = p; }
	public final void setAmbIntensity(int p) { ambIntensity = p; }
	
	public final Vector3D getParallelLightDirection() { return direction; }
	public final Vector3D getDirection() { return direction; }
	
	public final void setParallelLightDirection(Vector3D v) {
		if (v == null) throw new NullPointerException();
		direction = v;
	}
	
	public final void setDirection(Vector3D v) { setParallelLightDirection(v); }

	// Vendor-specific methods
	public final void setParallelLightDirection(com.jblend.graphics.j3d.Vector3D v) {
		if (v == null) throw new NullPointerException();
		direction = (Vector3D) v;
	}

	public final void setParallelLightDirection(com.motorola.graphics.j3d.Vector3D v) {
		if (v == null) throw new NullPointerException();
		direction = (Vector3D) v;
	}

	public final void setParallelLightDirection(com.vodafone.v10.graphics.j3d.Vector3D v) {
		if (v == null) throw new NullPointerException();
		direction = (Vector3D) v;
	}

	public final void setDirection(com.jblend.graphics.j3d.Vector3D v) { setParallelLightDirection((Vector3D) v); }

	public final void setDirection(com.motorola.graphics.j3d.Vector3D v) { setParallelLightDirection((Vector3D) v); }

	public final void setDirection(com.vodafone.v10.graphics.j3d.Vector3D v) { setParallelLightDirection((Vector3D) v); }
}