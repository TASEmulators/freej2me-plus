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
package org.recompile.freej2me.debug;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.mascotcapsule.micro3d.v3.AffineTrans;
import com.mascotcapsule.micro3d.v3.Effect3D;
import com.mascotcapsule.micro3d.v3.Figure;
import com.mascotcapsule.micro3d.v3.FigureLayout;
import com.mascotcapsule.micro3d.v3.Light;
import com.mascotcapsule.micro3d.v3.Vector3D;

import org.recompile.mobile.PlatformGraphics;
import org.recompile.mobile.PlatformImage;

public class MCv33DPreviewPanel extends JPanel
{
	private static final int DEFAULT_YAW = 2560;
	private static final int DEFAULT_PITCH = 128;
	private static final int DEFAULT_CAMERA_DISTANCE = 768;
	private static final int DEFAULT_MODEL_SCALE = 256;

	private static final int MIN_MODEL_SCALE = 32;
	private static final int MAX_MODEL_SCALE = 2048;
	private static final int MAX_PITCH = 1024;

	private PlatformImage img;
	private PlatformGraphics gfx;

	private int bgColor = 0x000000;

	private final FigureLayout figureLayout = new FigureLayout();
	private final AffineTrans viewTrans = new AffineTrans();
	private final AffineTrans modelTrans = new AffineTrans();
	private final AffineTrans rotTrans = new AffineTrans();
	private final AffineTrans scaleTrans = new AffineTrans();
	private final Effect3D effect = new Effect3D();
	private final Vector3D lightDir = new Vector3D(-3511, 731, 878);
	private final Light light = new Light(lightDir, 8192, 1755);

	private final Vector3D eyeVec = new Vector3D();
	private final Vector3D targetVec = new Vector3D(0, 0, 0);
	private final Vector3D upVec = new Vector3D(0, 4096, 0);

	private int yaw = DEFAULT_YAW;
	private int pitch = DEFAULT_PITCH;
	private int cameraDistance = DEFAULT_CAMERA_DISTANCE;
	private int modelScale = DEFAULT_MODEL_SCALE;

	private int lastMouseX = -1, lastMouseY = -1;
	private Figure lastTargetObj = null;

	public MCv33DPreviewPanel()
	{
		setBackground(java.awt.Color.BLACK);
		effect.setLight(light);
		effect.setShading(Effect3D.NORMAL_SHADING);
		initMouse();
		resetCamera();
	}

	public void resetCamera()
	{
		yaw = DEFAULT_YAW;
		pitch = DEFAULT_PITCH;
		cameraDistance = DEFAULT_CAMERA_DISTANCE;
		modelScale = DEFAULT_MODEL_SCALE;
		targetVec.set(10, -5, 15);
		calcCamBase();
		rerenderLast();
	}

	public void setModelScale(int scale)
	{
		this.modelScale = clamp(scale, MIN_MODEL_SCALE, MAX_MODEL_SCALE);
		setCamTrans();
		rerenderLast();
	}

	private void rerenderLast()
	{
		if (lastTargetObj != null) { renderObject(lastTargetObj); }
		else { clearBackground(); }
	}

	private void initMouse()
	{
		MouseAdapter adapter = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				lastMouseX = e.getX();
				lastMouseY = e.getY();
				if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) { resetCamera(); }
			}

			@Override
			public void mouseDragged(MouseEvent e)
			{
				int dx = e.getX() - lastMouseX;
				int dy = e.getY() - lastMouseY;
				lastMouseX = e.getX();
				lastMouseY = e.getY();

				int mods = e.getModifiersEx();
				if ((mods & MouseEvent.BUTTON1_DOWN_MASK) != 0)
				{
					panCamera(dx, dy);
					rerenderLast();
				}
				else if ((mods & MouseEvent.BUTTON3_DOWN_MASK) != 0)
				{
					rotateObject(dx, dy);
					rerenderLast();
				}
			}

			@Override
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				modelScale = clamp((int) Math.round(modelScale * Math.pow(1.12, -e.getWheelRotation())), MIN_MODEL_SCALE, MAX_MODEL_SCALE);
				setCamTrans();
				rerenderLast();
			}
		};

		addMouseListener(adapter);
		addMouseMotionListener(adapter);
		addMouseWheelListener(adapter);
	}

	private void calcCamBase()
	{
		double yawRad = (yaw / (double) 4096) * Math.PI * 2.0;
		double pitchRad = (pitch / (double) 4096) * Math.PI * 2.0;

		double cosPitch = Math.cos(pitchRad);
		double sinPitch = Math.sin(pitchRad);
		double sinYaw = Math.sin(yawRad);
		double cosYaw = Math.cos(yawRad);

		double ox = cameraDistance * cosPitch * sinYaw;
		double oy = cameraDistance * sinPitch;
		double oz = cameraDistance * cosPitch * cosYaw;

		eyeVec.set(
			targetVec.x + (int) Math.round(ox),
			targetVec.y + (int) Math.round(oy),
			targetVec.z + (int) Math.round(oz)
		);

		setCamTrans();
	}

	private void rotateObject(int dx, int dy)
	{
		yaw = wrapAngle(yaw + dx * 4);
		pitch = clamp(pitch + dy * 4, -MAX_PITCH, MAX_PITCH);
		setCamTrans();
	}
	
	private void panCamera(int dx, int dy)
	{
		double fx = targetVec.x - eyeVec.x;
		double fy = targetVec.y - eyeVec.y;
		double fz = targetVec.z - eyeVec.z;
		double fLen = Math.hypot(fx, Math.hypot(fy, fz));
		if (fLen < 1e-6) { return; }
		fx /= fLen;
		fy /= fLen;
		fz /= fLen;

		double rx = fy * upVec.z - fz * upVec.y;
		double ry = fz * upVec.x - fx * upVec.z;
		double rz = fx * upVec.y - fy * upVec.x;
		double rLen = Math.hypot(rx, Math.hypot(ry, rz));
		if (rLen < 1e-6) { rx = 1.0; ry = 0.0; rz = 0.0; }
		else { rx /= rLen; ry /= rLen; rz /= rLen; }

		double camUpX = ry * fz - rz * fy;
		double camUpY = rz * fx - rx * fz;
		double camUpZ = rx * fy - ry * fx;

		int moveX = (int) Math.round((-dx * rx + dy * camUpX));
		int moveY = (int) Math.round((-dx * ry + dy * camUpY));
		int moveZ = (int) Math.round((-dx * rz + dy * camUpZ));

		targetVec.set(targetVec.x + moveX, targetVec.y + moveY, targetVec.z + moveZ);

		eyeVec.set(eyeVec.x + moveX, eyeVec.y + moveY, eyeVec.z + moveZ);

		setCamTrans();
	}

	private void setCamTrans()
	{
		viewTrans.setIdentity();
		viewTrans.lookAt(eyeVec, targetVec, upVec);

		modelTrans.setIdentity();

		rotTrans.setIdentity();
		rotTrans.rotationY(yaw);
		modelTrans.mul(rotTrans);

		rotTrans.setIdentity();
		rotTrans.rotationX(pitch);
		modelTrans.mul(rotTrans);

		scaleTrans.setIdentity();
		scaleTrans.set(
			modelScale, 0, 0, 0,
			0, modelScale, 0, 0,
			0, 0, modelScale, 0
		);
		modelTrans.mul(scaleTrans);

		viewTrans.mul(modelTrans);
		figureLayout.setAffineTrans(viewTrans);
	}

	public void setPreviewBackground(int bgColor)
	{
		this.bgColor = bgColor;
		rerenderLast();
	}

	private void clearMCv3Target()
	{
		int w = Math.max(1, getWidth());
		int h = Math.max(1, getHeight());

		if (img == null || img.getWidth() != w || img.getHeight() != h)
		{
			img = new PlatformImage(w, h);
			gfx = (PlatformGraphics) img.getMIDPGraphics();
		}

		gfx.setColor(bgColor);
		gfx.fillRect(0, 0, img.getWidth(), img.getHeight());
	}

	public void clearBackground()
	{
		clearMCv3Target();
		repaint();
	}

	public void renderObject(Figure targetObj)
	{
		clearMCv3Target();
		lastTargetObj = targetObj;
		if (targetObj == null)
		{
			repaint();
			return;
		}

		int w = Math.max(1, img.getWidth());
		int h = Math.max(1, img.getHeight());

		figureLayout.setPerspective(1, 32767, 768);
		figureLayout.setCenter(w / 2, h / 2);

		synchronized (gfx)
		{
			try { gfx.drawFigure(targetObj, 0, 0, figureLayout, effect); }
			catch (Exception e) { }
		}

		repaint();
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		g.drawImage(img.getCanvas(), 0, 0, null);
	}

	private static int wrapAngle(int angle) { return angle & 0x0FFF; }

	private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
