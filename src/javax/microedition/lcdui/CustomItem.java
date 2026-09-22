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


	protected CustomItem(String label) { setLabel(label); }

	public int getGameAction(int keycode) { return Mobile.getGameAction(keycode); }

	protected final int getInteractionModes() { return KEY_PRESS | KEY_RELEASE | POINTER_PRESS | POINTER_RELEASE | TRAVERSE_HORIZONTAL | TRAVERSE_VERTICAL; }

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

	protected boolean traverse(int dir, int viewportWidth, int viewportHeight, int[] visRect_inout)
	{
		int currentX = visRect_inout[0]; // Current X position
		int currentY = visRect_inout[1]; // Current Y position
		int currentWidth = visRect_inout[2]; // Current width of the focused element
		int currentHeight = visRect_inout[3]; // Current height of the focused element

		// Initial state tracking for traversal
		if (dir == NONE) { return true; } // Indicate traversal is happening within the item, but no direction

		// Track the current position based on direction
		switch (dir)
		{
			case Canvas.UP:
				if (currentY > 0)
				{
					currentY -= 5; // Move up
					visRect_inout[1] = currentY; // Update visible rectangle
					if(currentY < 0) { currentY = 0; }
					return true;
				}
				break;
			case Canvas.DOWN:
				if (currentY + currentHeight < viewportHeight) {
					currentY += 5; // Move down
					if(currentY + currentHeight > viewportHeight) { currentY = viewportHeight; }
					visRect_inout[1] = currentY;
					return true;
				}
				break;
			case Canvas.LEFT:
				if (currentX > 0)
				{
					currentX -= 5; // Move left
					if(currentX < 0) { currentX = 0; }
					visRect_inout[0] = currentX; // Update visible rectangle
					return true; // Traversal occurred
				}
				break;
			case Canvas.RIGHT:
				if (currentX + currentWidth < viewportWidth)
				{
					currentX += 5; // Move right
					if(currentX + currentWidth < viewportWidth) { currentX = viewportWidth; }
					visRect_inout[0] = currentX;
					return true;
				}
				break;
			default:
				return false;
		}

		// If no movement occurred, return false
		return false;
	}

	protected void traverseOut() { repaint(); } // Request a repaint to clear highlights, traversing out of the object

	protected void renderItem(Graphics graphics, int x, int y, int width, int height, boolean isSelected)
	{
		// TODO: Incomplete. Possibly we'd need to save/restore much more

		int color = graphics.getColor();
		graphics.translate(x, y);
		paint((Graphics) graphics, width, height);
		graphics.translate(-x, -y);
		graphics.setColor(color);
	}
}
