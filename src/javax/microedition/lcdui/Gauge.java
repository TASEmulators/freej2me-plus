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
import org.recompile.mobile.PlatformGraphics;

public class Gauge extends Item
{

	public static final int CONTINUOUS_IDLE = 0;
	public static final int CONTINUOUS_RUNNING = 2;
	public static final int INCREMENTAL_IDLE = 1;
	public static final int INCREMENTAL_UPDATING = 3;
	public static final int INDEFINITE = -1;


	private boolean interactive;
	private int maxValue;
	private int value;

	public Gauge(String label, boolean isInteractive, int maxvalue, int initialvalue)
	{
		if(isInteractive && maxvalue <= 0) { throw new IllegalArgumentException("Cannot create an interactive gauge with negative or 0 max value"); }
		if(maxvalue < 0 && (!isInteractive && maxvalue != INDEFINITE)) { throw new IllegalArgumentException("Cannot create a gauge with negative or 0 max value, or an non-interactive gauge whose max value is not positive or INDEFINITE"); }
		if(!isInteractive && maxvalue == INDEFINITE && initialvalue != INCREMENTAL_IDLE && initialvalue != CONTINUOUS_RUNNING && initialvalue !=  INCREMENTAL_UPDATING)
			{ throw new IllegalArgumentException("Cannot create non-interactive gauge with indefinite range and a special value " + initialvalue + " that isn't in the range 0-3."); }


		Mobile.log(Mobile.LOG_WARNING, Gauge.class.getPackage().getName() + "." + Gauge.class.getSimpleName() + ": " + "Create Gauge");
		setLabel(label);
		interactive = isInteractive;
		maxValue = maxvalue;
		value = initialvalue;
	}


	public void addCommand(Command cmd) { super.addCommand(cmd); }

	public int getMaxValue() { return maxValue; }

	public int getValue() { return value; }

	public boolean isInteractive() { return interactive; }

	public void setDefaultCommand(Command cmd) { super.setDefaultCommand(cmd); }

	public void setItemCommandListener(ItemCommandListener l) { super.setItemCommandListener(l); }

	public void setMaxValue(int newmax) 
	{
		if(!interactive) 
		{
			if(newmax == INDEFINITE) 
			{
				maxValue = newmax;
				setValue(CONTINUOUS_IDLE);
			}
			else if(newmax > 0) { maxValue = newmax; }
		}
		else if(newmax > maxValue) 
		{
			Mobile.log(Mobile.LOG_WARNING, Gauge.class.getPackage().getName() + "." + Gauge.class.getSimpleName() + ": " + "setMaxValue received value " + newmax + " which is higher or equal to the current max of " + maxValue + ". Keeping its value unchanged.");
		}
		else if(newmax > 0) { maxValue = newmax; }

		_invalidateContents();
	}

	public void setValue(int newvalue) 
	{
		if(interactive || maxValue != INDEFINITE) 
		{
			if(newvalue < 0) 
			{ 
				Mobile.log(Mobile.LOG_WARNING, Gauge.class.getPackage().getName() + "." + Gauge.class.getSimpleName() + ": " + "setValue received value " + newvalue + " which is below the min allowed value of 0. Clamping value to 0.");
				value = 0;
			}
			else if(newvalue > maxValue) 
			{
				Mobile.log(Mobile.LOG_WARNING, Gauge.class.getPackage().getName() + "." + Gauge.class.getSimpleName() + ": " + "setValue received value " + newvalue + " which is beyond the max allowed value of " + maxValue + ". Clamping value to " + maxValue + ".");
				value = maxValue; 
			}
			else { value = newvalue; }
		}
		else 
		{
			if(newvalue != CONTINUOUS_IDLE && newvalue != CONTINUOUS_RUNNING && newvalue != INCREMENTAL_IDLE && newvalue != INCREMENTAL_UPDATING) 
			{
				throw new IllegalArgumentException("Gauge is non-interactive and has indefinite value. Received invalid value update");
			}
			value = newvalue;
		}
		_invalidateContents();
	}

	protected int getContentHeight(int width) { return Font.getDefaultFont().getHeight() + Font.getDefaultFont().getHeight()/5; }

	protected boolean keyPressed(int key) // Gauge extends Item, which receives a converted Canvas key
	{ 
		boolean handled = !interactive;

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

	protected void renderItem(PlatformGraphics graphics, int x, int y, int width, int height) 
	{
		graphics.getGraphics2D().translate(x, y);
		
		int arrowSpacing = _drawArrow(graphics, -1,  value > 0, 0, 0, width, Font.getDefaultFont().getHeight());

		graphics.setColor(Mobile.lcduiTextColor);
		graphics.drawRect(arrowSpacing, 0, width-2*arrowSpacing, Font.getDefaultFont().getHeight());

		int barWidth = maxValue == 0 ? 0 : ((value * (width-2*arrowSpacing))/maxValue);

		graphics.fillRect(arrowSpacing+2, 2, barWidth-3, Font.getDefaultFont().getHeight()-3);
		
		graphics.setColor(Mobile.lcduiStrokeColor); // Gauge Value will be rendered with stroke color instead of "lcduiTextColor" (as the bar is TextColor and the BG is BGColor) 
		
		String text;
		if(maxValue != INDEFINITE) { text = Integer.toString(value) + " (" + String.format("%.0f", (value / (float) maxValue * 100f)) + "%)"; }
		else { text = "? (?%)"; }
		int textWidth = (graphics.getGraphics2D().getFontMetrics().stringWidth(text));
		graphics.drawString(text, (width-textWidth)/2 + 1, 0, Graphics.LEFT); // Using Grapyics' HCENTER doesn't work as expected here, so centering has to be done manually
		
		_drawArrow(graphics, 1,  value < maxValue, 0, 0, width, Font.getDefaultFont().getHeight());
	
		graphics.getGraphics2D().translate(-x, -y);
	}

}
