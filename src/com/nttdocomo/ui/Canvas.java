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

	private final Runnable paintTask = new Runnable()
	{
		@Override
		public void run()
		{
			repaintRequest();
		}
	};

	private final Object paintLock = new Object();
	private boolean needsRepaint = false;
	private int paintX, paintY, paintW, paintH;

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
		if (!isShown()) { return; }

		if (!Mobile.compatImmediateRepaints)
		{
			if (!IApplication.display.isEventThread())
			{
				synchronized (paintLock)
				{
					paintX = x; paintY = y; paintW = width; paintH = height;
					needsRepaint = true;
				}
				repaintRequest(); // Runs synchronously; no thread-boundary lock contention
				return;
			}

			boolean postRequired = false;

			synchronized (paintLock)
			{
				if (!needsRepaint)
				{
					paintX = x;
					paintY = y;
					paintW = width;
					paintH = height;
					needsRepaint = true;
					postRequired = true;
				}
				else
				{
					// Unionize clipping bounds for consecutive repaints, so
					// we don't keep stacking draw operations on top of others.
					int x2 = Math.max(paintX + paintW, x + width);
					int y2 = Math.max(paintY + paintH, y + height);
					paintX = Math.min(paintX, x);
					paintY = Math.min(paintY, y);
					paintW = x2 - paintX;
					paintH = y2 - paintY;
				}
			}

			if (postRequired)
			{
				IApplication.display.postPaintRequest(paintTask);
			}
		}
		else
		{
			synchronized (paintLock)
			{
				paintX = x;
				paintY = y;
				paintW = width;
				paintH = height;
				needsRepaint = true;
			}
			repaintRequest();
		}
	}

	private void repaintRequest()
	{
		if (!isShown()) { return; }

		int rX, rY, rW, rH;

		synchronized (paintLock)
		{
			if (!needsRepaint) { return; }
			rX = paintX;
			rY = paintY;
			rW = paintW;
			rH = paintH;
			needsRepaint = false;
		}

		try
		{
			graphics.reset(rX, rY, rW, rH);
			paint(graphics);
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, Canvas.class.getPackage().getName() + "." + Canvas.class.getSimpleName() + ": " + "Serious Exception hit in repaint(): " + e.getMessage());
			e.printStackTrace();
		}

		if (labelVisible)
		{
			Mobile.getPlatform().setPostFlushDraw(new Runnable()
			{
				@Override
				public void run() { paintCommandsBar(); }
			});
		}

		Mobile.getPlatform().flushGraphics(platformImage, rX, rY, rW, rH);
	}

	private void paintCommandsBar()
	{
		// The command bar shouldn't influence canvas drawing operations, so it's added directly to the frontBuffer after swapping.
		javax.microedition.lcdui.Graphics graphics = Mobile.getPlatform().getLcdFrontbufferGraphics();

		final int barHeight = Font.getDefaultFont().getHeight();
		// Fade the command bar if there's one second left to hide it
		long fadeStart = 1000000000L;
		if (MobilePlatform.timeToUnfocus < fadeStart)
		{
			graphics.setAlphaRGB(((byte)(0xFF * Math.max(0, Math.min(1, MobilePlatform.timeToUnfocus / 1000000000.0))) << 24) | Mobile.lcduiBGColor);
			graphics.fillRect(0, Mobile.lcdHeight-barHeight, Mobile.lcdWidth, barHeight);
			graphics.setAlphaRGB(((byte)(0xFF * Math.max(0, Math.min(1, MobilePlatform.timeToUnfocus / 1000000000.0))) << 24) | Mobile.lcduiTextColor);
		}
		else
		{
			graphics.setAlphaRGB((0xFF << 24) | Mobile.lcduiBGColor);
			graphics.fillRect(0, Mobile.lcdHeight-barHeight, Mobile.lcdWidth, barHeight);
			graphics.setAlphaRGB((0xFF << 24) | Mobile.lcduiTextColor);
		}

		graphics.drawLine(0, Mobile.lcdHeight-barHeight, Mobile.lcdWidth, Mobile.lcdHeight-barHeight);
		graphics.drawLine(Mobile.lcdWidth/2, Mobile.lcdHeight-barHeight, Mobile.lcdWidth/2, Mobile.lcdHeight);

		// Command text drawing
		int textCenter;
		int xPos;

		String label = softLabels[0] != null ? softLabels[0] : "";
		textCenter = (graphics.getGraphics2D().getFontMetrics().stringWidth(label))/2;
		xPos = (Mobile.lcdWidth / 4) - textCenter;
		graphics.drawString(label, xPos, Mobile.lcdHeight-barHeight, Graphics.LEFT);

		label = softLabels[1] != null ? softLabels[1] : "";
		textCenter = (graphics.getGraphics2D().getFontMetrics().stringWidth(label))/2;
		xPos = (3 * Mobile.lcdWidth / 4) + textCenter;
		graphics.drawString(softLabels[1], xPos, Mobile.lcdHeight-barHeight, Graphics.RIGHT);
	}
}
