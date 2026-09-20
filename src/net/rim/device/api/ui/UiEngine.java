/*
	This file is part of FreeJ2ME.

	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	FreeJ2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILTY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/
*/
package net.rim.device.api.ui;


public interface UiEngine
{
	public static final int GLOBAL_MODAL = 1;
	public static final int GLOBAL_QUEUE = 2;
	public static final int GLOBAL_SHOW_LOWER = 4;

	void dismissStatus(Screen screen);

	Screen getActiveScreen();

	int getScreenCount();

	boolean isPaintingSuspended();

	void popScreen(Screen screen);

	void pushGlobalScreen(Screen screen, int priority, boolean inputRequired);

	void pushGlobalScreen(Screen screen, int priority, int flags);

	void pushModalScreen(Screen screen);

	void pushScreen(Screen screen);

	void queueStatus(Screen screen, int priority, boolean inputRequired);

	void relayout();

	void repaint();

	void suspendPainting(boolean suspend);

	void updateDisplay();
}
