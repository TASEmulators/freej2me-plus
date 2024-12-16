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
import org.recompile.mobile.MobilePlatform;
import org.recompile.mobile.PlatformImage;
import org.recompile.mobile.PlatformGraphics;

import java.util.ArrayList;
import java.awt.Rectangle;


public class Form extends Screen
{

	public ItemStateListener listener;

	int focusedItem = 0;
	boolean focusedItemNeedsTraverse = false;
	boolean needsLayout = true;
	int scrollY = 0;
	int itemContentWidth;
	Rectangle[] itemBounds = null;
	int clientHeight;
	int scrollHeight = 0;

	public Form(String title) { setTitle(title); }

	public Form(String title, Item[] itemarray)
	{
		setTitle(title);

		for (int i=0; i<itemarray.length; i++)
		{ doInsert(i, itemarray[i], false); }
	}


	public int append(Image img) { return doInsert(items.size(), new ImageItem("", img , ImageItem.LAYOUT_DEFAULT, ""), false); }

	public int append(Item item) { return doInsert(items.size(), item, false); }

	public int append(String str) { return doInsert(items.size(), new StringItem("", str), false); }

	public void insert(int itemNum, Item item) { doInsert(items.size(), item, false);  }

	public void set(int itemNum, Item item) { doInsert(items.size(), item, true);  }

	int doInsert(int index, Item item, boolean replace) 
	{
		if (replace && index < items.size()) 
		{
			Item oldItem = items.get(index);
			if (oldItem != null) { oldItem.setOwner(null); }
			items.set(index, item);
		} 
		else { items.add(index, item); }
		
		item.setOwner(this);
		needsLayout = true;
		if (items.size() == 1) { focusedItemNeedsTraverse = true; }
		_invalidate();
		return index;
	}

	public void delete(int itemNum) 
	{
		Item oldItem = items.get(itemNum);
		oldItem.traverseOut();
		oldItem.setOwner(null);
		items.remove(itemNum);
		needsLayout = true;
		if (focusedItem >= items.size() && !items.isEmpty()) 
		{
			focusedItem = items.size()-1;
			focusedItemNeedsTraverse = true;
		} 
		else if (items.isEmpty()) 
		{
			focusedItem = 0;
			focusedItemNeedsTraverse = false;
		}
		_invalidate();
	}

	public void deleteAll() 
	{
		for (Item item: items) 
		{
			item.traverseOut();
			item.setOwner(null);
		}
		items.clear();
		needsLayout = true;
		focusedItemNeedsTraverse = false;
		focusedItem = 0;
		_invalidate();
	}

	public Item get(int itemNum) { return items.get(itemNum); }

	public int getHeight() { return 128; }

	public int getWidth() { return 64; }

	public void setItemStateListener(ItemStateListener iListener) { listener = iListener; }

	protected void itemStateChanged(Item item) 
	{
		if (listener != null) { listener.itemStateChanged(item); }
	}

	public int size() { return items.size(); }

	/*
		Draw form, handle input
	*/

	public boolean screenKeyPressed(int key)
	{
		if(items.size()<1 || needsLayout || focusedItemNeedsTraverse) { return false; }

		boolean handled = true, shouldInvalidate = false;

		Item item = getFocusedItem();
		if (item != null) { handled = item.keyPressed(key); }

		if (!handled) 
		{
			if (key == Canvas.UP || key == Canvas.KEY_NUM2 || key == Canvas.DOWN || key == Canvas.KEY_NUM8) 
			{
				// first see if internal traversal should be attempted
				boolean traversed = false;
				traversed = doTraverseItem(focusedItem, key);
			
				// we assume that traversed returning false for a limit doesn't imply traverseOut yet
				// and if we've trafersed internally, there's nothing left

				if (!traversed) {
					// check if we should scroll

					int reasonablePadding = 10;
					int scrollAmount = clientHeight/4;

					Rectangle reasonableViewport = new Rectangle(0, scrollY+reasonablePadding, width, clientHeight-reasonablePadding);
					int traverseDir = 0;

					if (key == Canvas.UP || key == Canvas.KEY_NUM2) 
					{
						if (focusedItem > 0 && itemBounds[focusedItem-1].intersects(reasonableViewport)) 
						{
							// focusedItem--;
							traverseDir = -1;
						} else if (scrollY > 0) 
						{
							scrollY = Math.max(0, scrollY - scrollAmount);
							shouldInvalidate = true;
						}
					} 
					else 
					{
						int maxScroll = scrollHeight - clientHeight;

						if (focusedItem < items.size()-1 && itemBounds[focusedItem+1].intersects(reasonableViewport)) {
							// focusedItem++;
							traverseDir = 1;
						} else if (scrollY < maxScroll) {
							scrollY = Math.min(maxScroll, scrollY + scrollAmount);
							shouldInvalidate = true;
						}
					}

					if (traverseDir != 0) 
					{
						if (!focusedItemNeedsTraverse && getFocusedItem() != null) { getFocusedItem().traverseOut(); }

						focusedItem += traverseDir;

						// do the initial traverse, ignoring results
						doTraverseItem(focusedItem, traverseDir > 0 ? Canvas.DOWN : Canvas.UP);

						scrollForRegion(itemBounds[focusedItem].y, itemBounds[focusedItem].height);
						shouldInvalidate = true;
					}
				}
				handled = true;
			}
			
		}
		
		if (shouldInvalidate) { _invalidate(); }

		return handled;
	}

	private void computeLayout(PlatformGraphics gc, int height)
	{
		this.clientHeight = height;
		scrollY = 0;
		focusedItem = 0;
		focusedItemNeedsTraverse = !items.isEmpty();
		itemBounds = new Rectangle[items.size()];

		int spaceBetweenItems = 2;
		int scrollbarWidth = 4;
		int padding = 5;

		int currentY = padding;

		int itemX = padding;

		
		itemContentWidth = width-scrollbarWidth-2*padding;


		for (int i=0; i<items.size(); i++)
		{
			if (i > 0)
			{
				currentY += spaceBetweenItems;
			}
			int itemHeight = getItemHeight(items.get(i), itemContentWidth);

			itemBounds[i] = new Rectangle(itemX, currentY, itemContentWidth, itemHeight);
			currentY += itemHeight;
		}

		currentY += padding;
		scrollHeight = currentY;	
	}

	private int getItemHeight(Item item, int width)
	{
		int height = item.getContentHeight(width) + item.getLabelHeight(width);
		return height;
	}

	protected Item getFocusedItem() 
	{
		if (items.isEmpty()) { return null; }

		return items.get(focusedItem);
	}

	protected void focusItem(Item item)
	{
		if (needsLayout) { return; } // zb3: hmm

		int index = items.indexOf(item); // zb3: this might not be by reference
		if (index == -1) { return; }

		Item previousItem = getFocusedItem();
		if (previousItem != null && previousItem != item) 
		{
			previousItem.traverseOut();
			
			focusedItem = index;
			focusedItemNeedsTraverse = true;
		}

		scrollForRegion(itemBounds[index].y, itemBounds[index].height);

		render();
	}

	private void scrollForRegion(int y, int height) 
	{
		/*
		 * zb3: current position is this.scrollY
		 * screen height is this.clientHeight
		 * 
		 * our goal is to change scrollY by a minimum amount such that:
		 * - if height <= this.clientHeight, the whole region should be visible
		 * - otherwise, the closer edge (top or bottom) is at the edge of our screen
		 */

		if (height <= this.clientHeight) 
		{
			int topInvisible = Math.max(0, scrollY - y);
			int bottomInvisible = Math.max(0, (y + height) - (scrollY + clientHeight));

			if (topInvisible == 0 && bottomInvisible == 0) { return; }

			if (topInvisible > bottomInvisible) { scrollY -= topInvisible; } 
			else { scrollY += bottomInvisible; }
		} 
		else 
		{
			int topDistance = Math.abs(y - this.scrollY);
			int bottomDistance = Math.abs((y + height) - (this.scrollY + this.clientHeight));

			if (topDistance < bottomDistance) { this.scrollY = y; } 
			else { this.scrollY = y + height - this.clientHeight; }
		}
	}

	protected Command getItemCommand() 
	{
		Item focusedItem = getFocusedItem();
		if (focusedItem != null) { return focusedItem._getItemCommand(); }
		return null;
	}

	public boolean doTraverseItem(int itemIdx, int dir) 
	{
		Item item = items.get(itemIdx);
		int itemLabelHeight = item.getLabelHeight(itemBounds[itemIdx].width);

		int itemStartY = itemBounds[itemIdx].y + itemLabelHeight;

		int visRectStart = Math.max(scrollY, itemStartY);
		int visRectEnd = Math.min(scrollY + clientHeight, itemBounds[itemIdx].y + itemBounds[itemIdx].height);

		int[] visRect = new int[]{0, visRectStart - itemStartY, itemBounds[itemIdx].width,  Math.max(0, visRectEnd - visRectStart)};

		boolean continueTraversing = item.traverse(dir, itemBounds[itemIdx].width, clientHeight, visRect);

		if (continueTraversing) { scrollForRegion(visRect[1] + itemStartY, visRect[3]); }

		return continueTraversing;
	}

	public String renderScreen(int x, int y, int width, int height)
	{
		String ret = null;

		if (needsLayout)
		{
			computeLayout(graphics, height);
			needsLayout = false;
		}

		if (focusedItemNeedsTraverse) 
		{
			// zb3: initial traverse occurs here and we ignore the return value
			// if traverse returns false then it doesn't mean skip traversing
			// otherwise no itemcommands could be supported without traversing.
			focusedItemNeedsTraverse = false;
			doTraverseItem(focusedItem, CustomItem.NONE);
		}

		if(items.size()>0)
		{
			int scrollbarWidth = 4;

			Rectangle viewport = new Rectangle(0, scrollY, width, height);

			for (int t=0;t<items.size();t++)
			{
				Item item = items.get(t);

				if (!viewport.intersects(itemBounds[t]))
				continue;

				int thisX = x + itemBounds[t].x;
				int thisY = y + itemBounds[t].y - scrollY;

				if (t == focusedItem && items.size() > 1)
				{
					graphics.setColor(Mobile.lcduiBGColor);
					// drawRect needs size - 1
					graphics.drawRect(thisX - 3, thisY - 3, itemBounds[t].width + 6 - 1, itemBounds[t].height + 6 - 1);
					graphics.setColor(Mobile.lcduiTextColor);
				}

				if (item.hasLabel()) {
					item.renderItemLabel(graphics, thisX, thisY, itemContentWidth);
					thisY += item.getLabelHeight(itemContentWidth);
				}

				// paint...

				int itemHeight = item.getContentHeight(itemContentWidth);

				if(item instanceof ImageItem)
				{
					graphics.drawImage(((ImageItem)item).getImage(), (width/2)-((ImageItem)item).getImage().getWidth()/2, thisY, 0); // Draw all ImageItems centered for now
				}
				else { item.renderItem(graphics, thisX, thisY, itemContentWidth, itemHeight); }
				
				thisY += itemHeight;
			}

			double fact = (double)height/scrollHeight;
			int yscrollStart = (int)Math.round(scrollY * fact);
			int yscrollHeight = (int)Math.min(height, Math.round(height * fact));
		
			if (height < scrollHeight)
			{
				graphics.setColor(Mobile.lcduiBGColor);
				graphics.fillRect(x + width - scrollbarWidth, y+yscrollStart, scrollbarWidth, yscrollHeight);
			}
		
			graphics.reset();

			ret = (focusedItem+1)+" of "+items.size();
		}

		return ret;
	}

}
