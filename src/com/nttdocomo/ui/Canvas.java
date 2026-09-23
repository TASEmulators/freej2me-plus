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
package com.nttdocomo.ui;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.MobilePlatform;

public abstract class Canvas extends Frame
{
	public Runnable postFlushDraw = new Runnable()
	{
		@Override
		public void run()
		{
			// Draw FrameBuffer right before the commands bar.
			if (labelVisible) { paintCommandsBar(); }
		}
	};

	public Canvas()
	{
		super();

		Mobile.log(Mobile.LOG_INFO, Canvas.class.getPackage().getName() + "." + Canvas.class.getSimpleName() + ": " + "Create I-Appli Canvas:" + width+", "+height);
	}

	public Graphics getGraphics()
	{
		return platformImage.getDoJaGraphics();
	}

	public int getKeypadState() { return getKeypadState(0); }

	public int getKeypadState(int group)
	{
		if (group < 0) { throw new IllegalArgumentException("group cannot be negative"); }

		return MobilePlatform.doJaKeyState;
	}

	public abstract void paint(Graphics g);

	public void processEvent(int type, int param) { }

	public void repaint() { repaint(0, 0, getWidth(), getHeight()); }

	public void repaint(final int x, final int y, final int width, final int height)
	{
		// DO NOT even issue repaints when paused. Lock here too.
		while (Mobile.isPaused)
		{
			try { Thread.sleep(50); }
			catch(Exception e) { }
		}

		if (!isShown() || width <= 0 || height <= 0 || graphics == null) { return; }

		try
		{
			graphics.reset(x, y, width, height);
			paint(graphics);
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, Canvas.class.getPackage().getName() + "." + Canvas.class.getSimpleName() + ": " + "Serious Exception hit in repaint(): " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void paintCommandsBar()
	{
		// The command bar shouldn't influence canvas drawing operations, so it's added directly to the frontBuffer after swapping.
		javax.microedition.lcdui.Graphics graphics = Mobile.getPlatform().getLcdFrontbufferGraphics();

		final int barHeight = Font.getDefaultFont().getHeight();

		// Fade the command bar if there's one second left to hide it
		long fadeStart = 1000000000L;
		float alphaFactor = 0.8f;
		if (MobilePlatform.timeToUnfocus < fadeStart)
		{
			alphaFactor = Math.max(0f, Math.min(0.8f, MobilePlatform.timeToUnfocus / 1000000000.0f));
		}

		javax.microedition.lcdui.LCDUIRenderer.drawCommandBar(graphics, null, null, null, width, height, barHeight, alphaFactor, Mobile.lcduiBGColor, false);

		int baseAlpha = (int)(0xFF * alphaFactor);
		graphics.setAlphaRGB((baseAlpha << 24) | Mobile.lcduiTextColor);

		// Command text drawing
		int textCenter;
		int xPos;

		String label = softLabels[0] != null ? softLabels[0] : "";
		textCenter = (graphics.getGraphics2D().getFontMetrics().stringWidth(label))/2;
		xPos = (Mobile.lcdWidth / 4) - textCenter;
		graphics.drawString(label, xPos, Mobile.lcdHeight-barHeight, Graphics.TOP | Graphics.LEFT);

		label = softLabels[1] != null ? softLabels[1] : "";
		textCenter = (graphics.getGraphics2D().getFontMetrics().stringWidth(label))/2;
		xPos = (3 * Mobile.lcdWidth / 4) + textCenter;
		graphics.drawString(softLabels[1], xPos, Mobile.lcdHeight-barHeight, Graphics.TOP | Graphics.RIGHT);
	}
}
