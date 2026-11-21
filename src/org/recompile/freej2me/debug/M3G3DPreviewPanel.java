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

import javax.microedition.m3g.Camera;
import javax.microedition.m3g.Graphics3D;
import javax.microedition.m3g.M3GMath;
import javax.microedition.m3g.Node;
import javax.microedition.m3g.Object3D;
import javax.microedition.m3g.Transform;
import javax.microedition.m3g.World;

import org.recompile.mobile.PlatformImage;

public class M3G3DPreviewPanel extends JPanel
{
	private BufferedImage previewBuffer;
	private PlatformImage img;
	private javax.microedition.lcdui.Graphics gfx;

	private boolean wireframe = false, untextured = false, drawHidden = true, mipmapping = false;
	private int bgColor = 0x000000;
	private final Graphics3D renderer = Graphics3D.getInstance();

	private float yaw = 0.0f;
	private float pitch = 15.0f;
	private float distance = 25.0f;
	private float panX = 0.0f, panY = 0.0f, panZ = 0.0f;

	private final Camera previewCam = new Camera();
	private final Transform camTransform = new Transform();
	private final Transform identityTransform = new Transform();
	private final Transform prevCamTrans = new Transform();
	private final float[] camMatrix = new float[16];
	private final float[] forward = new float[3];
	private final float[] right = new float[3];
	private final float[] up = new float[3];
	private int lastCamW = -1, lastCamH = -1;

	private int lastMouseX = -1, lastMouseY = -1;
	private Object3D lastTargetObj = null;

	public M3G3DPreviewPanel() { initMouse(); }

	// Base camera setup looks slightly down, and is a 25 units away from the
	// origin, so that most geometry is visible right away.
	public void resetCamera()
	{
		yaw = 0.0f; pitch = 15.0f; distance = 25.0f;
		panX = 0.0f; panY = 0.0f; panZ = 0.0f;
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
			public void mousePressed(MouseEvent e)
			{
				lastMouseX = e.getX(); lastMouseY = e.getY();
				if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) { resetCamera(); }
			}

			public void mouseDragged(MouseEvent e)
			{
				int dx = e.getX() - lastMouseX, dy = e.getY() - lastMouseY;
				lastMouseX = e.getX(); lastMouseY = e.getY();

				int mods = e.getModifiersEx();
				if ((mods & MouseEvent.BUTTON1_DOWN_MASK) != 0)
				{
					yaw -= dx * 0.6f;
					pitch = Math.max(-89.0f, Math.min(89.0f, pitch + dy * 0.6f));
					rerenderLast();
				}
				else if ((mods & MouseEvent.BUTTON3_DOWN_MASK) != 0)
				{
					panCamera(dx, dy);
					rerenderLast();
				}
			}

			public void mouseWheelMoved(MouseWheelEvent e)
			{
				distance = Math.max(0.5f, Math.min(8192.0f, distance * (float) Math.pow(0.9, -e.getPreciseWheelRotation())));
				rerenderLast();
			}
		};

		addMouseListener(adapter);
		addMouseMotionListener(adapter);
		addMouseWheelListener(adapter);
	}

	private void calcCamBase()
	{
		float yawRad = M3GMath.toRadians(yaw);
		float pitchRad = M3GMath.toRadians(pitch);

		float cosPitch = (float) M3GMath.cos(pitchRad);
		float zx = (float) M3GMath.sin(yawRad) * cosPitch;
		float zy = (float) M3GMath.sin(pitchRad);
		float zz = (float) M3GMath.cos(yawRad) * cosPitch;

		forward[0] = zx; forward[1] = zy; forward[2] = zz;

		float rx = zz, ry = 0.0f, rz = -zx;
		float rLen = (float) M3GMath.sqrt(rx * rx + ry * ry + rz * rz);

		if (rLen > 1e-5f) { rx /= rLen; ry /= rLen; rz /= rLen; }
		right[0] = rx; right[1] = ry; right[2] = rz;

		up[0] = zy * rz - zz * ry; up[1] = zz * rx - zx * rz; up[2] = zx * ry - zy * rx;
	}

	private void panCamera(int dx, int dy)
	{
		calcCamBase();
		float panScale = M3GMath.max(0.002f, distance * 0.0025f);
		panX += (-right[0] * dx + up[0] * dy) * panScale;
		panY += (-right[1] * dx + up[1] * dy) * panScale;
		panZ += (-right[2] * dx + up[2] * dy) * panScale;
	}

	private Transform setCamTrans()
	{
		calcCamBase();

		float ox = distance * forward[0], oy = distance * forward[1], oz = distance * forward[2];
		float eyeX = panX + ox, eyeY = panY + oy, eyeZ = panZ + oz;

		float xx = right[0], xy = right[1], xz = right[2];
		float yx = up[0],    yy = up[1],    yz = up[2];
		float zx = forward[0], zy = forward[1], zz = forward[2];

		camMatrix[0] = xx; camMatrix[1] = yx; camMatrix[2] = zx; camMatrix[3] = eyeX;
		camMatrix[4] = xy; camMatrix[5] = yy; camMatrix[6] = zy; camMatrix[7] = eyeY;
		camMatrix[8] = xz; camMatrix[9] = yz; camMatrix[10] = zz; camMatrix[11] = eyeZ;
		camMatrix[12] = 0f; camMatrix[13] = 0f; camMatrix[14] = 0f; camMatrix[15] = 1f;

		camTransform.set(camMatrix);
		return camTransform;
	}

	public void setPreviewBackground(int bgColor)
	{
		this.bgColor = bgColor;
		clearBackground();
	}

	public void setWireframe(boolean wireframe) { this.wireframe = wireframe; }

	public void setUntextured(boolean untextured) { this.untextured = untextured; }

	public void setDrawHidden(boolean drawHidden) { this.drawHidden = drawHidden; }

	public void setMipmapping(boolean mipmapping) { this.mipmapping = mipmapping; }

	private void clearM3GTarget()
	{
		int w = Math.max(1, getWidth()), h = Math.max(1, getHeight());

		if (previewBuffer == null || previewBuffer.getWidth() != w || previewBuffer.getHeight() != h)
		{
			previewBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			img = new PlatformImage(w, h);
		}

		gfx = img.getMIDPGraphics();
		gfx.setColor(bgColor);
		gfx.fillRect(0, 0, img.getWidth(), img.getHeight());
	}

	private void paintToScreen()
	{
		if (previewBuffer != null && img != null && img.getCanvas() != null)
		{
			Graphics2D g2d = previewBuffer.createGraphics();
			g2d.drawImage(img.getCanvas(), 0, 0, null);
			g2d.dispose();
		}
		repaint();
	}

	public void clearBackground()
	{
		clearM3GTarget();
		paintToScreen();
	}

	public void renderObject(Object3D targetObj)
	{
		clearM3GTarget();
		lastTargetObj = targetObj;
		if (targetObj == null)
		{
			paintToScreen();
			return;
		}

		int w = Math.max(1, img.getWidth());
		int h = Math.max(1, img.getHeight());

		synchronized (renderer)
		{
			Object prevTarget = renderer.getTarget();
			Camera prevCamera = renderer.getCamera(prevCamTrans);
			boolean prevDepthBuffer = renderer.isDepthBufferEnabled();
			int prevRenderHints = renderer.getRenderHints();

			renderer.releaseTarget();
			renderer.bindTarget(gfx);

			if (w != lastCamW || h != lastCamH)
			{
				previewCam.setPerspective(75.0f, (float) w / (float) h, 0.5f, 16384.0f);
				lastCamW = w; lastCamH = h;
			}
			setCamTrans();

			renderer.wireframeDebugFlag = this.wireframe;
			renderer.untexturedDebugFlag = this.untextured;
			renderer.drawHiddenNodesDebugFlag = this.drawHidden;
			renderer.forceMipmappingDebugFlag = this.mipmapping;
			try
			{
				if (targetObj instanceof World)
				{
					renderer.setCamera(previewCam, camTransform);
					renderer.render((World) targetObj, false);
				}
				else if (targetObj instanceof Node)
				{
					renderer.setCamera(previewCam, camTransform);
					renderer.render((Node) targetObj, identityTransform);
				}
			}
			catch (Exception e) { }
			finally
			{
				// Render step is over, restore control to the app, and reset all debug flags.
				renderer.wireframeDebugFlag = false;
				renderer.untexturedDebugFlag = false;
				renderer.drawHiddenNodesDebugFlag = false;
				renderer.forceMipmappingDebugFlag = false;
				renderer.releaseTarget();
				renderer.setCamera(prevCamera, prevCamTrans);
				if (prevTarget != null) { renderer.bindTarget(prevTarget, prevDepthBuffer, prevRenderHints); }
			}
		}

		paintToScreen();
	}

	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		g.drawImage(previewBuffer, 0, 0, getWidth(), getHeight(), null);
	}
}
