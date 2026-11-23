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
package org.recompile.freej2me.debug;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import javax.microedition.m3g.Image2D;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformImage;

class ImagePanel extends JPanel
{
	private static final int ITEM_SPACE = 8;
	private static final int DRAG_THRESHOLD = 4;
	private static final Color SELECTION_FILL = new Color(255, 255, 255, 64);

	private ArrayList<WeakReference<?>> sourceRefs = new ArrayList<WeakReference<?>>();
	private ArrayList<BufferedImage> sourceCanvases = new ArrayList<BufferedImage>();

	private ArrayList<WeakReference<?>> activeRefs = new ArrayList<WeakReference<?>>();
	private ArrayList<BufferedImage> activeCanvases = new ArrayList<BufferedImage>();
	private ArrayList<Rectangle> entryBounds = new ArrayList<Rectangle>();

	private int lastWrapWidth = -1;
	private WeakReference<?> selectedImage;
	private final ImageSelectionListener selectionListener;
	private float scale = 1.0f;

	private Point dragStartScreen;
	private Point dragStartViewPos;
	private final Point reusableViewPos = new Point();
	private boolean dragged;

	public ImagePanel(ImageSelectionListener listener)
	{
		this.selectionListener = listener;
		setBackground(Color.DARK_GRAY);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		MouseAdapter panelPanner = new MouseAdapter()
		{
			public void mousePressed(MouseEvent e)
			{
				if (!SwingUtilities.isLeftMouseButton(e)) { return; }
				JViewport viewport = (JViewport) getParent();
				if (viewport == null) { return; }

				dragStartScreen = e.getLocationOnScreen();
				dragStartViewPos = viewport.getViewPosition();
				dragged = false;
			}

			public void mouseDragged(MouseEvent e)
			{
				if (dragStartScreen == null || dragStartViewPos == null) { return; }
				JViewport viewport = (JViewport) getParent();
				if (viewport == null) { return; }

				Point currentScreen = e.getLocationOnScreen();
				int dx = currentScreen.x - dragStartScreen.x;
				int dy = currentScreen.y - dragStartScreen.y;

				if (!dragged && (Math.abs(dx) > DRAG_THRESHOLD || Math.abs(dy) > DRAG_THRESHOLD))
				{
					dragged = true;
					setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
				}

				if (dragged)
				{
					Dimension extent = viewport.getExtentSize();
					Dimension viewSize = viewport.getViewSize();

					int maxX = Math.max(0, viewSize.width - extent.width);
					int maxY = Math.max(0, viewSize.height - extent.height);

					int nextX = Math.max(0, Math.min(maxX, dragStartViewPos.x - dx));
					int nextY = Math.max(0, Math.min(maxY, dragStartViewPos.y - dy));

					reusableViewPos.setLocation(nextX, nextY);
					viewport.setViewPosition(reusableViewPos);
				}
			}

			public void mouseReleased(MouseEvent e)
			{
				boolean clicked = SwingUtilities.isLeftMouseButton(e) && !dragged;

				dragStartScreen = null;
				dragStartViewPos = null;
				dragged = false;
				setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

				if (clicked) { handleImageClick(e.getPoint()); }
			}
		};

		addMouseListener(panelPanner);
		addMouseMotionListener(panelPanner);
	}

	private void handleImageClick(Point pt)
	{
		WeakReference<?> hit = null;

		for (int i = entryBounds.size() - 1; i >= 0; i--)
		{
			Rectangle r = entryBounds.get(i);
			if (pt.x >= r.x - 1 && pt.x <= r.x + r.width + 1 &&
				pt.y >= r.y - 1 && pt.y <= r.y + r.height + 1)
			{
				hit = activeRefs.get(i);
				break;
			}
		}

		selectedImage = hit;
		if (selectionListener != null) { selectionListener.onImageSelected(selectedImage); }
		repaint();
	}

	public void setScale(float scale)
	{
		if (this.scale == scale) { return; }
		this.scale = scale;
		refreshLayout(true);
	}

	public WeakReference<?> getSelectedImage() { return selectedImage; }

	public void setImages(ArrayList<WeakReference<PlatformImage>> images)
	{
		ArrayList<WeakReference<?>> nextRefs = new ArrayList<WeakReference<?>>();
		ArrayList<BufferedImage> nextCanvases = new ArrayList<BufferedImage>();

		if (images != null)
		{
			for (WeakReference<PlatformImage> ref : images)
			{
				PlatformImage img = ObjectMemoryViewerDialog.getReference(ref);
				if (img == null) { continue; }
				BufferedImage canvas = img.getCanvas();
				if (canvas != null)
				{
					nextRefs.add(ref);
					nextCanvases.add(canvas);
				}
			}
		}

		applyImgs(nextRefs, nextCanvases);
	}

	public void setImage2Ds(ArrayList<WeakReference<Image2D>> images)
	{
		ArrayList<WeakReference<?>> nextRefs = new ArrayList<WeakReference<?>>();
		ArrayList<BufferedImage> nextCanvases = new ArrayList<BufferedImage>();

		if (images != null)
		{
			for (WeakReference<Image2D> ref : images)
			{
				Image2D img = ObjectMemoryViewerDialog.getReference(ref);
				if (img == null) { continue; }
				BufferedImage canvas = createBufferedImage(img);
				if (canvas != null)
				{
					nextRefs.add(ref);
					nextCanvases.add(canvas);
				}
			}
		}

		applyImgs(nextRefs, nextCanvases);
	}

	private BufferedImage createBufferedImage(Image2D img)
	{
		int width = img.getWidth();
		int height = img.getHeight();
		int[] pixels = img.getPixels(); // replace with your actual pixel-array getter
		if (width <= 0 || height <= 0 || pixels == null) { return null; }

		BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		int[] dst = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
		System.arraycopy(pixels, 0, dst, 0, Math.min(pixels.length, dst.length));
		return bi;
	}

	private void applyImgs(ArrayList<WeakReference<?>> nextRefs, ArrayList<BufferedImage> nextCanvases)
	{
		this.sourceRefs = nextRefs;
		this.sourceCanvases = nextCanvases;

		if (selectedImage != null && !nextRefs.contains(selectedImage)) { selectedImage = null; }

		refreshLayout(true);
	}

	private int getWrapWidth()
	{
		JViewport viewport = (JViewport) getParent();
		if (viewport != null)
		{
			int vw = viewport.getExtentSize().width;
			if (vw > 0) { return (int) (vw * 1.33f); }
		}
		return getWidth();
	}

	private int scaleDimension(int base) { return Math.max(1, (int) Math.round(base * scale)); }

	private void refreshLayout(boolean force)
	{
		int wrapWidth = getWrapWidth();
		if (!force && wrapWidth == lastWrapWidth) { return; }
		lastWrapWidth = wrapWidth;

		ArrayList<WeakReference<?>> nextActiveRefs = new ArrayList<WeakReference<?>>();
		ArrayList<BufferedImage> nextActiveCanvases = new ArrayList<BufferedImage>();
		ArrayList<Rectangle> nextBounds = new ArrayList<Rectangle>();

		int x = ITEM_SPACE;
		int y = ITEM_SPACE;
		int rowHeight = 0;
		int contentRight = 100;

		for (int i = 0; i < sourceRefs.size(); i++)
		{
			WeakReference<?> ref = sourceRefs.get(i);
			if (ref.get() == null) { continue; }

			BufferedImage canvas = sourceCanvases.get(i);
			int w = scaleDimension(canvas.getWidth());
			int h = scaleDimension(canvas.getHeight());

			if (x > ITEM_SPACE && (x + w + ITEM_SPACE) > wrapWidth)
			{
				y += rowHeight + ITEM_SPACE;
				x = ITEM_SPACE;
				rowHeight = 0;
			}

			nextActiveRefs.add(ref);
			nextActiveCanvases.add(canvas);
			nextBounds.add(new Rectangle(x, y, w, h));

			x += w + ITEM_SPACE;
			rowHeight = Math.max(rowHeight, h);
			contentRight = Math.max(contentRight, x);
		}

		activeRefs = nextActiveRefs;
		activeCanvases = nextActiveCanvases;
		entryBounds = nextBounds;
		setPreferredSize(new Dimension(contentRight, Math.max(y + rowHeight + ITEM_SPACE, 300)));
		revalidate();
		repaint();
	}

	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		refreshLayout(false);

		Rectangle clip = g.getClipBounds();

		for (int i = 0; i < entryBounds.size(); i++)
		{
			Rectangle r = entryBounds.get(i);
			if (clip != null && !r.intersects(clip.x - 2, clip.y - 2, clip.width + 4, clip.height + 4)) { continue; }

			WeakReference<?> ref = activeRefs.get(i);
			if (ref.get() == null) { continue; }

			g.drawImage(activeCanvases.get(i), r.x, r.y, r.width, r.height, null);

			boolean isSelected = (ref == selectedImage);
			g.setColor(isSelected ? Color.CYAN : Color.ORANGE);
			g.drawRect(r.x - 1, r.y - 1, r.width + 1, r.height + 1);
			if (isSelected)
			{
				g.drawRect(r.x - 2, r.y - 2, r.width + 3, r.height + 3);
				g.setColor(SELECTION_FILL);
				g.fillRect(r.x, r.y, r.width, r.height);
			}
		}
	}
}
