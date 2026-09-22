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
package javax.microedition.lcdui;

import java.util.ArrayList;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformImage;

public class Gauge extends Item
{

	public static final int CONTINUOUS_IDLE = 0;
	public static final int CONTINUOUS_RUNNING = 2;
	public static final int INCREMENTAL_IDLE = 1;
	public static final int INCREMENTAL_UPDATING = 3;
	public static final int INDEFINITE = -1;

	private int animationOffset = 0;
	private int animationDirection = 1; // 1 for moving right, -1 for left

	private boolean interactive;
	private int maxValue;
	private int value;

	public Gauge(String label, boolean isInteractive, int maxvalue, int initialvalue)
	{
		if(isInteractive && maxvalue <= 0) { throw new IllegalArgumentException("Cannot create an interactive gauge with negative or 0 max value"); }
		if(maxvalue < 0 && (!isInteractive && maxvalue != INDEFINITE)) { throw new IllegalArgumentException("Cannot create a gauge with negative or 0 max value, or an non-interactive gauge whose max value is not positive or INDEFINITE"); }
		if(!isInteractive && maxvalue == INDEFINITE && initialvalue != INCREMENTAL_IDLE && initialvalue != CONTINUOUS_RUNNING && initialvalue !=  INCREMENTAL_UPDATING)
			{ throw new IllegalArgumentException("Cannot create non-interactive gauge with indefinite range and a special value " + initialvalue + " that isn't in the range 0-3."); }

		setLabel(label);
		interactive = isInteractive;

		setMaxValue(maxvalue);
		setValue(initialvalue);
	}


	public void addCommand(Command cmd) { super.addCommand(cmd); }

	public int getMaxValue() { return maxValue; }

	public int getValue() { return value; }

	public boolean isInteractive() { return interactive; }

	public void setDefaultCommand(Command cmd) { super.setDefaultCommand(cmd); }

	public void setItemCommandListener(ItemCommandListener l) { super.setItemCommandListener(l); }

	public void setMaxValue(int newmax)
	{
		if (interactive)
		{
			if (newmax <= 0)
			{
				throw new IllegalArgumentException("Interactive gauge max value must be greater than zero");
			}
		}
		else
		{
			if (newmax <= 0 && newmax != INDEFINITE)
			{
				throw new IllegalArgumentException("Non-interactive gauge max value must be > 0 or INDEFINITE");
			}
		}

		boolean hadDefiniteRange = (maxValue != INDEFINITE);

		if (newmax > 0)
		{
			if (hadDefiniteRange)
			{
				maxValue = newmax;
				if (value > newmax) { value = newmax; }
			}
			else
			{
				maxValue = newmax;
				value = 0;
			}
		}
		else
		{
			if (hadDefiniteRange)
			{
				maxValue = newmax;
				value = CONTINUOUS_IDLE;
			}
		}

		_invalidateContents();
	}


	public void setValue(int newvalue)
	{
		if(interactive || maxValue != INDEFINITE)
		{
			if(newvalue > maxValue) { newvalue = maxValue; }
			if(newvalue < 0) { newvalue = 0; }
		}
		else
		{
			if(newvalue != CONTINUOUS_IDLE && newvalue != CONTINUOUS_RUNNING && newvalue != INCREMENTAL_IDLE && newvalue != INCREMENTAL_UPDATING)
			{
				throw new IllegalArgumentException("Gauge is non-interactive and has indefinite value. Received invalid value update");
			}
		}

		value = newvalue;
		_invalidateContents();
	}

	protected int getContentHeight(int width) { return Font.getDefaultFont().getHeight() + Font.getDefaultFont().getHeight()/5; }

	protected boolean keyPressed(int key) // Gauge extends Item, which receives a converted Canvas key
	{
		boolean handled = false;

		if(interactive)
		{
			if ((key == Canvas.LEFT || key == Canvas.KEY_NUM4) && value > 0) { setValue(value-1); handled = true; }
			else if ((key == Canvas.RIGHT || key == Canvas.KEY_NUM6) && value < maxValue) { setValue(value+1); handled = true; }

			if (handled)
			{
				notifyStateChanged();
				_invalidateContents();
			}
		}

		return handled;
	}


	public boolean isAnimating()
	{
		// Only animate if it's non-interactive, indefinite, and in a running/updating state
		return !interactive && maxValue == INDEFINITE &&
			   (value == CONTINUOUS_RUNNING || value == INCREMENTAL_UPDATING);
	}

	public void advanceAnimation(int barWidth)
	{
		if (!isAnimating()) { return; }

		animationOffset += animationDirection * (Mobile.limitFPS == 0 ? 1 : 60/Mobile.limitFPS);

		// Bounce back and forth inside the gauge bar's bounds. Segment width is
		// synced with LCDUIRenderer's drawGauge() code, so a change here must
		// also be done there and vice-versa.
		int segmentWidth = barWidth / 3;
		if (animationOffset + segmentWidth >= barWidth)
		{
			animationOffset = barWidth - segmentWidth;
			animationDirection = -1;
		}
		else if (animationOffset < 0)
		{
			animationOffset = 0;
			animationDirection = 1;
		}
	}

	public int getAnimationOffset() { return animationOffset; }

	protected void renderItem(Graphics graphics, int x, int y, int width, int height, boolean isSelected)
	{
		LCDUIRenderer.drawGauge(graphics, x, y, width, height, value, maxValue, interactive, animationOffset, isSelected);
	}
}
