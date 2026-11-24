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
	private PlatformImage img;
	private PlatformGraphics gfx;

	private int bgColor = 0x000000;

	private final FigureLayout figureLayout = new FigureLayout();
	private final AffineTrans viewTrans = new AffineTrans();
	private final Effect3D effect = new Effect3D();
	private final Vector3D lightDir = new Vector3D(0, 0, 0);
	private final Light light = new Light(lightDir, 4096, 1024);

	private final Vector3D eyeVec = new Vector3D();
	private final Vector3D targetVec = new Vector3D();
	private final Vector3D upVec = new Vector3D(0, 4096, 0);

	private int lastMouseX = -1, lastMouseY = -1;
	private Figure lastTargetObj = null;

	public MCv33DPreviewPanel()
	{
		setBackground(java.awt.Color.BLACK);
		effect.setLight(light);
		effect.setShading(Effect3D.NORMAL_SHADING);
		initMouse();
	}

	public void resetCamera()
	{
		// TODO
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
					// TODO: Camera orbiting
					rerenderLast();
				}
				else if ((mods & MouseEvent.BUTTON3_DOWN_MASK) != 0)
				{
					panCamera(dx, dy);
					rerenderLast();
				}
			}

			@Override
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				// TODO: Change camera distance
				rerenderLast();
			}
		};

		addMouseListener(adapter);
		addMouseMotionListener(adapter);
		addMouseWheelListener(adapter);
	}

	private void calcCamBase()
	{
		// TODO
	}

	private void panCamera(int dx, int dy)
	{
		// TODO
	}

	private void setCamTrans()
	{
		// TODO
	}

	public void setPreviewBackground(int bgColor)
	{
		this.bgColor = bgColor;
		clearBackground();
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

		synchronized (gfx)
		{
			// TODO
		}

		repaint();
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		g.drawImage(img.getCanvas(), 0, 0, null);
	}
}
