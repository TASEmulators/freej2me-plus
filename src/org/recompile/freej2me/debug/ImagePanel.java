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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformImage;

class ImagePanel extends JPanel
{
	private static final int ITEM_SPACE = 8;
	private static final int DRAG_THRESHOLD = 4;
	private static final Color SELECTION_FILL = new Color(255, 255, 255, 64);

	private ArrayList<WeakReference<PlatformImage>> images;
	private ArrayList<WeakReference<PlatformImage>> entryImages = new ArrayList<WeakReference<PlatformImage>>();
	private ArrayList<Rectangle> entryBounds = new ArrayList<Rectangle>();
	private int lastWrapWidth = -1;
	private WeakReference<PlatformImage> selectedImage;
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
				JViewport viewport = getViewport();
				if (viewport == null) { return; }

				dragStartScreen = e.getLocationOnScreen();
				dragStartViewPos = viewport.getViewPosition();
				dragged = false;
			}

			public void mouseDragged(MouseEvent e)
			{
				if (dragStartScreen == null || dragStartViewPos == null) { return; }
				JViewport viewport = getViewport();
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

	private JViewport getViewport()
	{
		Container parent = getParent();
		return (parent instanceof JViewport) ? (JViewport) parent : null;
	}

	private void handleImageClick(Point pt)
	{
		WeakReference<PlatformImage> hit = null;

		for (int i = entryBounds.size() - 1; i >= 0; i--)
		{
			Rectangle r = entryBounds.get(i);
			if (pt.x >= r.x - 1 && pt.x <= r.x + r.width + 1 &&
				pt.y >= r.y - 1 && pt.y <= r.y + r.height + 1)
			{
				hit = entryImages.get(i);
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

	public WeakReference<PlatformImage> getSelectedImage() { return selectedImage; }

	public void setImages(ArrayList<WeakReference<PlatformImage>> images)
	{
		this.images = images;
		if (selectedImage != null && (images == null || !images.contains(selectedImage)))
		{
			selectedImage = null;
		}
		refreshLayout(true);
	}

	private int getWrapWidth()
	{
		JViewport viewport = getViewport();
		if (viewport != null)
		{
			int vw = viewport.getExtentSize().width;
			if (vw > 0) { return Math.max(vw, 120); }
		}
		return Math.max(getWidth(), 220);
	}

	private int scaleDimension(int base) { return Math.max(1, (int) Math.round(base * scale)); }

	private void refreshLayout(boolean force)
	{
		int wrapWidth = getWrapWidth();
		if (!force && wrapWidth == lastWrapWidth) { return; }
		lastWrapWidth = wrapWidth;

		ArrayList<WeakReference<PlatformImage>> nextImages = new ArrayList<WeakReference<PlatformImage>>();
		ArrayList<Rectangle> nextBounds = new ArrayList<Rectangle>();

		int x = ITEM_SPACE;
		int y = ITEM_SPACE;
		int rowHeight = 0;
		int contentRight = 100;

		if (images != null)
		{
			for (WeakReference<PlatformImage> imgRef : images)
			{
				PlatformImage img = ObjectMemoryViewerDialog.getReference(imgRef);
				if (img == null) { continue; }
				BufferedImage canvas = img.getCanvas();

				int w = scaleDimension(canvas.getWidth());
				int h = scaleDimension(canvas.getHeight());

				if (x > ITEM_SPACE && (x + w + ITEM_SPACE) > wrapWidth)
				{
					y += rowHeight + ITEM_SPACE;
					x = ITEM_SPACE;
					rowHeight = 0;
				}

				nextImages.add(imgRef);
				nextBounds.add(new Rectangle(x, y, w, h));

				x += w + ITEM_SPACE;
				rowHeight = Math.max(rowHeight, h);
				contentRight = Math.max(contentRight, x);
			}
		}

		entryImages = nextImages;
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

			WeakReference<PlatformImage> imgRef = entryImages.get(i);
			PlatformImage img = ObjectMemoryViewerDialog.getReference(imgRef);
			if (img == null) { continue; }
			BufferedImage canvas = img.getCanvas();

			g.drawImage(canvas, r.x, r.y, r.width, r.height, null);

			boolean isSelected = (imgRef == selectedImage);
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
