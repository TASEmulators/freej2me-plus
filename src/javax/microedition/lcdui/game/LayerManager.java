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

import java.util.ArrayList;

import java.awt.Shape;

import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Graphics;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformGraphics;


public class LayerManager
{

	private int layers;

	private Layer component[] = new Layer[4];

	protected int x;
	protected int y;
	protected int width;
	protected int height;


	public LayerManager() { setViewWindow(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE); }

	// This is just an insert() call, but with the last layer pos as the index.
	public void append(Layer l) { insert(l, layers); }

	public Layer getLayerAt(int index) 
	{ 
		if ((index < 0) || (index >= layers)) { throw new IndexOutOfBoundsException(); }

		return component[index];
	}

	public int getSize() { return layers; }

	public void insert(Layer l, int index) 
	{ 
		if ((index < 0) || (index > layers) || (exist(l) && (index >= layers))) { throw new IndexOutOfBoundsException(); }
	
		remove(l);

		if (layers == component.length)
		{
			Layer newcomponents[] = new Layer[layers + 4];
			System.arraycopy(component, 0, newcomponents, 0, layers);
			System.arraycopy(component, index, newcomponents, index + 1, layers - index);
			component = newcomponents;
		}
		else { System.arraycopy(component, index, component, index + 1, layers - index); }

		component[index] = l;
		layers++;
	}

	public void paint(Graphics g, int xdest, int ydest)
	{
		int cx = g.getClipX();
		int cy = g.getClipY();
		int cw = g.getClipWidth();
		int ch = g.getClipHeight();

		g.translate(xdest - x, ydest - y);
		// set the clip to view window
		g.clipRect(x, y, width, height);

		for (int i = layers-1; i >= 0; i--)
		{
			Layer comp = component[i];
			if (component[i].visible) { component[i].paint(g); }
		}

		g.translate(-xdest + x, -ydest + y);
		g.setClip(cx, cy, cw, ch);
	}

	public void remove(Layer l) 
	{ 
		if (l == null) { throw new NullPointerException(); }

		for (int i = layers-1; i >= 0; i--)
		{
			if (component[i] == l) 
			{
				System.arraycopy(component, i + 1, component, i, layers - i - 1);
				component[--layers] = null;
			}
		}
	}

	private boolean exist(Layer l)
	{
		if (l == null) { return false; }

		for (int i = layers; --i >= 0; )
		{
			if (component[i] == l) { return true; }
		}
		return false;
	}

	public void setViewWindow(int wx, int wy, int wwidth, int wheight)
	{
		if (width < 0 || height < 0) { throw new IllegalArgumentException(); }

		x = wx;
		y = wy;
		width = wwidth;
		height = wheight;
	}

}
