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

import org.recompile.mobile.Mobile;

public class Ticker
{
	int scrollOffset = Mobile.getPlatform().lcdWidth;
	private String text;

	public Ticker(String str) { setString(str); }

	public String getString() { return text; }

	public void setString(String str)
	{
		if(str == null) { throw new NullPointerException("String cannot be null"); }
		text = str;
	}

	public int getScrollOffset() { return scrollOffset; }

	public void advanceOffset()
	{
		scrollOffset -= Mobile.limitFPS == 0 ? 1 : 60/Mobile.limitFPS;
		if(scrollOffset < -LCDUIRenderer.tickerFont.stringWidth(text)) { scrollOffset = Mobile.getPlatform().lcdWidth; }
	}
}
