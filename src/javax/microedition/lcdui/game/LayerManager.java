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
import javax.microedition.lcdui.Graphics;

public class LayerManager
{

	private ArrayList<Layer> layerList;

	private int viewWindowX;

	private int viewWindowY;

	private int viewWindowWidth;

	private int viewWindowHeight;


	public LayerManager() 
	{ 
		setViewWindow(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
		this.layerList = new ArrayList<Layer>();
	}

	// This is just an insert() call, but with the last layer pos as the index.
	public void append(Layer layer) { insert(layer, layerList.size()); }

	public Layer getLayerAt(int index) 
	{ 
		if ((index < 0) || (index >= layerList.size()))
			{ throw new IndexOutOfBoundsException("Invalid layer index."); }

		return layerList.get(index);
	}

	public int getSize() { return layerList.size(); }

	public void insert(Layer layer, int index) 
	{
		if(layer == null)
			{ throw new NullPointerException("Cannot insert a null layer"); }

		if ((index < 0) || (index > layerList.size()) ||
			(exist(layer) && (index >= layerList.size()))) 
			{ throw new IndexOutOfBoundsException("Layer index out of range"); }
	
		remove(layer);

		layerList.add(index, layer);
	}

	public void paint(Graphics g, int dx, int dy)
	{
		final int clipX = g.getClipX();
		final int clipY = g.getClipY();
		final int clipWidth = g.getClipWidth();
		final int clipHeight = g.getClipHeight();

		// Translate and set Graphics clip to the current ViewWindow bounds
		g.translate(dx - this.viewWindowX, dy - this.viewWindowY);
		g.clipRect(this.viewWindowX, this.viewWindowY,
			this.viewWindowWidth, this.viewWindowHeight);

		// Paint the given layer (if visible). The layer's index indicates its
		// z-order, meaning that index 0 is at the front of all others, which
		// means we have to draw from {@code layers} to {@code 0}.
		for (int i = layerList.size()-1; i >= 0; i--)
		{
			if (layerList.get(i).isVisible()) { layerList.get(i).paint(g); }
		}
			

		// Restore the original translation and clip bounds after painting
		g.translate(-dx + this.viewWindowX, -dy + this.viewWindowY);
		g.setClip(clipX, clipY, clipWidth, clipHeight);
	}

	public void remove(Layer layer) 
	{ 
		if (layer == null)
			throw new NullPointerException("Cannot remove a null layer.");

		layerList.remove(layer);
	}

	private boolean exist(Layer layer)
	{
		if (layer == null) { return false; }

		for (int i = 0; i < layerList.size(); i++) 
		{
			if (this.layerList.get(i) == layer) { return true; }
		}
			
		return false;
	}

	public void setViewWindow(int x, int y, int width, int height)
	{
		if (width < 0 || height < 0) 
			{ throw new IllegalArgumentException("ViewWindow has invalid width and/or height."); }

		this.viewWindowX = x;
		this.viewWindowY = y;
		this.viewWindowWidth = width;
		this.viewWindowHeight = height;
	}
}
