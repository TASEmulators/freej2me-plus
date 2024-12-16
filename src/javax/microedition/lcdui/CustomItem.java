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
import org.recompile.mobile.PlatformGraphics;

public abstract class CustomItem extends Item
{
	protected static final int KEY_PRESS = 4;
	protected static final int KEY_RELEASE = 8;
	protected static final int KEY_REPEAT = 0x10;
	protected static final int NONE = 0x00;
	protected static final int POINTER_DRAG = 0x80;
	protected static final int POINTER_PRESS = 0x20;
	protected static final int POINTER_RELEASE = 0x40;
	protected static final int TRAVERSE_HORIZONTAL = 1;
	protected static final int TRAVERSE_VERTICAL = 2;


	protected CustomItem(String label)
	{
		Mobile.log(Mobile.LOG_WARNING, CustomItem.class.getPackage().getName() + "." + CustomItem.class.getSimpleName() + ": " + "CustomItem created. LCDUI support is incomplete");
		setLabel(label);
	}

	public int getGameAction(int keycode) { return 0; }

	protected final int getInteractionModes() { return 0xFC; }

	protected abstract int getMinContentHeight();

	protected abstract int getMinContentWidth();

	protected abstract int getPrefContentHeight(int width);

	protected abstract int getPrefContentWidth(int height);

	protected void hideNotify() { }

	protected final void invalidate() { super.invalidate(); }

	protected boolean keyPressed(int keyCode) { return false; }

	protected void keyReleased(int keyCode) { }

	protected void keyRepeated(int keyCode) { }

	protected abstract void paint(Graphics g, int w, int h);

	protected void pointerDragged(int x, int y) { }

	protected void pointerPressed(int x, int y) { }

	protected void pointerReleased(int x, int y) { }

	protected final void repaint() { this._invalidateContents(); }

	protected final void repaint(int x, int y, int w, int h) { this._invalidateContents(); }

	protected void showNotify() { }

	protected void sizeChanged(int w, int h) { }

	protected int getContentHeight(int width) { return this.getPrefContentHeight(width); }

	protected boolean traverse(int dir, int viewportWidth, int viewportHeight, int[] visRect_inout) { return true; }

	protected void traverseOut() { }

	protected void renderItem(PlatformGraphics graphics, int x, int y, int width, int height) 
	{
		// TODO: Incomplete. Possibly we'd need to save/restore much more

		int color = graphics.getColor();
		graphics.getGraphics2D().translate(x, y);
		paint(graphics, width, height);
		graphics.getGraphics2D().translate(-x, -y);
		graphics.setColor(color);
	}

}
