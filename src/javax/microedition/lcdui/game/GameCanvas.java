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
package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.MobilePlatform;
import org.recompile.mobile.PlatformImage;

public abstract class GameCanvas extends Canvas
{
	public static final int UP_PRESSED = 1 << Canvas.UP;
	public static final int DOWN_PRESSED = 1 << Canvas.DOWN;
	public static final int LEFT_PRESSED = 1 << Canvas.LEFT;
	public static final int RIGHT_PRESSED = 1 << Canvas.RIGHT;
	public static final int FIRE_PRESSED = 1 << Canvas.FIRE;
	public static final int GAME_A_PRESSED = 1 << Canvas.GAME_A;
	public static final int GAME_B_PRESSED = 1 << Canvas.GAME_B;
	public static final int GAME_C_PRESSED = 1 << Canvas.GAME_C;
	public static final int GAME_D_PRESSED = 1 << Canvas.GAME_D;

	private Image buffer = null;

	protected GameCanvas(boolean suppressKeyEvents)
	{
		super(suppressKeyEvents);

		// Only create the off-screen buffer if the application really wants to use it
		if(getWidth() > 0 && getHeight() > 0) { buffer = Image.createImage(getWidth(), getHeight()); }
	}

	protected Graphics getGraphics() 
	{
		Graphics g = buffer.getGraphics();
		g.reset(); 
		return g;
	}

	public void paint(Graphics g) { g.drawImage(buffer, 0, 0, Graphics.LEFT | Graphics.TOP); }

	public void flushGraphics(int x, int y, int width, int height)
	{
		if (width <= 0 || height <= 0 || !isShown()) { return; }

		if (x < 0) { width += x; x = 0; }
		if (y < 0) { height += y; y = 0; }
		if (x + width > this.width) { width = this.width - x; }
		if (y + height > this.height) { height = this.height - y; }

		// If nothing will be drawn, don't even waste a call...
		if (width <= 0 || height <= 0 || x >= this.width || y >= this.height) { return; }

		Mobile.getPlatform().flushGraphics(buffer, x, y, width, height);
	}

	public void flushGraphics() { flushGraphics(0, 0, getWidth(), getHeight()); }

	public int getKeyStates() { return isShown() ? MobilePlatform.keyState : 0; }

	@Override
	public void doSizeChanged(int w, int h) { super.sizeChanged(w, h); }
}
