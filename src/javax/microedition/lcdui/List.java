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
import org.recompile.mobile.PlatformImage;

import java.util.ArrayList;

public class List extends Screen implements Choice
{

	public static Command SELECT_COMMAND = new Command("", Command.SCREEN, 0);

	protected int selectedIndex = -1;

	private int fitPolicy = Choice.TEXT_WRAP_DEFAULT;

	private ArrayList<String> strings = new ArrayList<String>();
	private ArrayList<Font> fonts = new ArrayList<Font>();
	private ArrayList<Image> images = new ArrayList<Image>();

	private ArrayList<Boolean> selectedItems = new ArrayList<Boolean>();

	private int type;

	public List(String title, int listType)
	{
		if(listType != Choice.EXCLUSIVE && listType != Choice.MULTIPLE && listType != Choice.IMPLICIT)
			{ throw new IllegalArgumentException("Invalid choice type for choice group"); }

		setTitle(title);
		type = listType;
	}

	public List(String title, int listType, String[] stringElements, Image[] imageElements)
	{
		this(title, listType);
		if(stringElements == null) { throw new NullPointerException("String array cannot be null");}
		if(imageElements != null && imageElements.length != stringElements.length)
			{ throw new IllegalArgumentException("Element array size mismatch"); }

		for(int i=0; i<stringElements.length; i++)
		{
			if(stringElements[i] == null) { throw new NullPointerException("Null element in string array"); }

			strings.add(stringElements[i]);
			fonts.add(Font.getDefaultFont());
			images.add((imageElements != null && i<imageElements.length) ? imageElements[i] : null);
			selectedItems.add(false);
		}

		if (!strings.isEmpty()) { selectedIndex = 0; }
	}

	public int append(String stringPart, Image imagePart)
	{
		if(stringPart == null) { throw new NullPointerException("String cannot be null"); }

		strings.add(stringPart);
		fonts.add(Font.getDefaultFont());
		images.add(imagePart);
		selectedItems.add(false);

		if (!strings.isEmpty() && selectedIndex == -1) { selectedIndex = 0; }
		_invalidate();

		return size() - 1;
	}

	public void delete(int itemNum)
	{
		if(itemNum < 0 || itemNum >= size()) { throw new IndexOutOfBoundsException("Invalid item index"); }

		strings.remove(itemNum);
		fonts.remove(itemNum);
		images.remove(itemNum);
		selectedItems.remove(itemNum);

		if (selectedIndex > itemNum) { selectedIndex--; }

		_invalidate();
	}

	public void deleteAll()
	{
		strings.clear(); images.clear(); selectedItems.clear();
		fonts.clear();
		selectedIndex = -1;
		_invalidate();
	}

	public int getFitPolicy() { return fitPolicy; }

	public Font getFont(int itemNum)
	{
		if(itemNum < 0 || itemNum >= size()) { throw new IndexOutOfBoundsException("Invalid item index"); }

		return fonts.get(itemNum);
	}

	public Image getImage(int itemNum)
	{
		if(itemNum < 0 || itemNum >= size()) { throw new IndexOutOfBoundsException("Invalid item index"); }

		return images.get(itemNum);
	}

	public int getSelectedFlags(boolean[] selectedArray_return)
	{
		if(selectedArray_return.length < size()) { throw new IllegalArgumentException("Return array too small"); }
		if(selectedArray_return == null) { throw new NullPointerException("Return array cannot be null"); }

		int numSelected = 0;

		for (int i=0; i<selectedItems.size(); i++)
		{
			if(selectedItems.get(i) == true) { selectedArray_return[i] = true; numSelected++; }
			else { selectedArray_return[i] = false; }
		}

		return numSelected;
	}

	public int getSelectedIndex()
	{
		if(type == Choice.IMPLICIT || type == Choice.EXCLUSIVE) { return selectedIndex; }

		return -1;
	}

	public String getString(int itemNum)
	{
		if(itemNum < 0 || itemNum >= size()) { throw new IndexOutOfBoundsException("Invalid item index"); }

		return strings.get(itemNum);
	}

	public void insert(int itemNum, String stringPart, Image imagePart)
	{
		if(itemNum < 0 || itemNum >= size()) { throw new IndexOutOfBoundsException("Invalid item index"); }
		if(stringPart == null) { throw new NullPointerException("String cannot be null"); }

		strings.add(itemNum, stringPart);
		fonts.add(itemNum, Font.getDefaultFont());
		images.add(itemNum, imagePart);
		selectedItems.add(itemNum, false);

		if (selectedIndex >= itemNum) { selectedIndex++; }
		if (!strings.isEmpty() && selectedIndex == -1) { selectedIndex = 0; }

		_invalidate();
	}

	public boolean isSelected(int itemNum)
	{
		if(itemNum < 0 || itemNum >= size()) { throw new IndexOutOfBoundsException("Invalid item index"); }

		if(type == Choice.IMPLICIT || type == Choice.EXCLUSIVE) { return itemNum==selectedIndex; }
		return selectedItems.get(itemNum);
	}

	@Override
	public void removeCommand(Command cmd)
	{
		super.removeCommand(cmd);
		if(cmd == SELECT_COMMAND) { setSelectCommand(null); }
		_invalidate();
	}

	public void set(int itemNum, String stringPart, Image imagePart)
	{
		if(itemNum < 0 || itemNum >= size()) { throw new IndexOutOfBoundsException("Invalid item index"); }
		if(stringPart == null) { throw new NullPointerException("String cannot be null"); }

		strings.set(itemNum, stringPart);
		images.set(itemNum, imagePart);
		_invalidate();
	}

	public void setFitPolicy(int policy)
	{
		if(policy < Choice.TEXT_WRAP_DEFAULT || policy > Choice.TEXT_WRAP_OFF) { throw new IllegalArgumentException("Invalid policy"); }
		fitPolicy = policy;
	}

	public void setFont(int itemNum, Font font)
	{
		if(itemNum < 0 || itemNum >= size()) { throw new IndexOutOfBoundsException("Invalid item index"); }
		fonts.set(itemNum, font != null ? font :  Font.getDefaultFont());
	}

	public void setSelectCommand(Command command)
	{
		super.removeCommand(SELECT_COMMAND);
		SELECT_COMMAND = command != null ? command : new Command("", Command.SCREEN, 0);
	}

	public void setSelectedFlags(boolean[] selectedArray)
	{
		if(selectedArray.length < size()) { throw new IllegalArgumentException("Return array too small"); }
		if(selectedArray == null) { throw new NullPointerException("Return array cannot be null"); }

		for (int i=0; i<selectedArray.length && i<size(); i++) { selectedItems.set(i, selectedArray[i]); }

		_invalidate();
	}

	public void setSelectedIndex(int itemNum, boolean selected)
	{
		if (itemNum < 0 || itemNum >= size()) { throw new IndexOutOfBoundsException("Invalid element index"); }
		if (type == Choice.EXCLUSIVE)
		{
			if(!selected) { return; }
			selectedIndex = itemNum;
			for (int i = 0; i < selectedItems.size(); i++)
			{
				selectedItems.set(i, false); // Deselect all others
			}
		}
		selectedItems.set(itemNum, selected);
		_invalidate();
	}

	public int size() { return strings.size(); }

	/*
		Draw list, handle input
	*/

	public boolean screenKeyPressed(int key)
	{
		if(size()<1) { return false; }
		boolean handled = true;

		if (key == Canvas.UP || key == Canvas.KEY_NUM2) { selectedIndex--; }
		else if (key == Canvas.DOWN || key == Canvas.KEY_NUM8) { selectedIndex++; }
		else if (key == Canvas.FIRE || key == Canvas.KEY_NUM5) { doDefaultCommand(); }
		else { handled = false; }

		if (selectedIndex>=size()) { selectedIndex=0; }
		if (selectedIndex<0) { selectedIndex = size()-1; }

		if (handled) { _invalidate(); }

		return handled;
	}

	protected void doDefaultCommand()
	{
		if(commandlistener!=null)
		{
			if(type == Choice.IMPLICIT) { commandlistener.commandAction(SELECT_COMMAND, this); }
			else
			{
				setSelectedIndex(selectedIndex, !selectedItems.get(selectedIndex));
			}
		}
	}

	public String renderScreen(int x, int y, int width, int height)
	{
		if (size() == 0) { return null; }

		if(selectedIndex < 0) { selectedIndex = 0; }

		int itemPadding = Font.fontPadding[Font.screenType];
		int itemHeight = Font.getDefaultFont().getHeight();
		int imagePadding = Font.getDefaultFont().getHeight()/4;

		int ah = height - itemPadding; // allowed height
		int max = Math.max(1, (int) Math.floor(ah / (itemHeight))); // max items per page (minimum of 1)

		if(size() < max) { max = size(); }

		int page = 0;
		page = (int)Math.floor(selectedIndex/max); // current page
		int first = page * max; // first item to show
		int last = first + max - 1;

		if(last >= size()) { last = size()-1; }

		boolean hasUp = (first > 0);
		boolean hasDown = (last < size() - 1);

		y += itemPadding;
		int startY = y;

		for(int i = first; i<=last; i++)
		{
			int vGap = 1; // Add a small space between items
			boolean isSelected = (selectedIndex == i);
			boolean checked = false;
			if (type == Choice.MULTIPLE) { checked = selectedItems.get(i).booleanValue(); }
			else if (type == Choice.EXCLUSIVE) { checked = (i == selectedIndex); }

			LCDUIRenderer.drawItem(graphics, i, strings.get(i), images.get(i), 0, y, width, itemHeight - vGap, isSelected, checked, type, false, Item.LAYOUT_DEFAULT);

			y += itemHeight;
		}

		// Down Indicator (if there are items hidden above)
		if (hasUp)
		{
			LCDUIRenderer.drawScrollIndicator(graphics, width / 2 - 2, startY + 1, true);
		}

		// Down Indicator (if there are items hidden below)
		if (hasDown)
		{
			LCDUIRenderer.drawScrollIndicator(graphics, width / 2 - 2, y + 1, false);
		}

		return (selectedIndex+1)+"/"+size();
	}
}
