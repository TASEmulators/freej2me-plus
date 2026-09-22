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

import java.util.ArrayList;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.MobilePlatform;

public class ChoiceGroup extends Item implements Choice
{

	private String label;

	private int type;

	private ArrayList<String> strings = new ArrayList<String>();
	private ArrayList<Font> fonts = new ArrayList<Font>();
	private ArrayList<Image> images = new ArrayList<Image>();

	private int fitPolicy = Choice.TEXT_WRAP_DEFAULT;

	private int selectedIndex = -1;
	private int highlightedIndex = -1;
	private ArrayList<Boolean> selectedItems = new ArrayList<Boolean>();

	public ChoiceGroup(String choiceLabel, int choiceType)
	{
		if(choiceType != Choice.EXCLUSIVE && choiceType != Choice.MULTIPLE && choiceType != Choice.POPUP)
			{ throw new IllegalArgumentException("Invalid choice type for choice group"); }

		setLabel(choiceLabel);
		type = choiceType;
	}

	public ChoiceGroup(String choiceLabel, int choiceType, String[] stringElements, Image[] imageElements)
	{
		this(choiceLabel, choiceType);
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

	ChoiceGroup(String choiceLabel, int choiceType, boolean validateChoiceType) { this(choiceLabel, choiceType); }

	ChoiceGroup(String choiceLabel, int choiceType, String[] stringElements, Image[] imageElements, boolean validateChoiceType)
	{
		this(choiceLabel, choiceType, stringElements, imageElements);
	}

	public int append(String stringPart, Image imagePart)
	{
		if(stringPart == null) { throw new NullPointerException("String cannot be null"); }

		strings.add(stringPart);
		fonts.add(Font.getDefaultFont());
		images.add(imagePart);
		selectedItems.add(false);

		if (!strings.isEmpty() && selectedIndex == -1) { selectedIndex = 0; }
		invalidate();

		return size() - 1;
	}

	public void delete(int itemNum)
	{
		if(itemNum < 0 || itemNum >= size()) { throw new IndexOutOfBoundsException("Invalid item index"); }

		strings.remove(itemNum);
		fonts.remove(itemNum);
		images.remove(itemNum);
		selectedItems.remove(itemNum);

		if (strings.isEmpty()) { selectedIndex = highlightedIndex = -1; }
		else
		{
			if (selectedIndex > itemNum) { selectedIndex--; }
			if (highlightedIndex > itemNum) { highlightedIndex--; }
		}

		invalidate();
	}

	public void deleteAll()
	{
		strings.clear(); images.clear(); selectedItems.clear();
		fonts.clear();
		selectedIndex = highlightedIndex = -1;
		invalidate();
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
		if(type == Choice.POPUP || type == Choice.EXCLUSIVE) { return selectedIndex; }

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
		if (highlightedIndex >= itemNum) { highlightedIndex++; }
		if (!strings.isEmpty() && selectedIndex == -1) { selectedIndex = 0; }

		invalidate();
	}

	public boolean isSelected(int itemNum)
	{
		if(itemNum < 0 || itemNum >= size()) { throw new IndexOutOfBoundsException("Invalid item index"); }

		if(type == Choice.EXCLUSIVE) { return itemNum==selectedIndex; }
		return selectedItems.get(itemNum);
	}

	public void set(int itemNum, String stringPart, Image imagePart)
	{
		if(itemNum < 0 || itemNum >= size()) { throw new IndexOutOfBoundsException("Invalid item index"); }
		if(stringPart == null) { throw new NullPointerException("String cannot be null"); }

		strings.set(itemNum, stringPart);
		images.set(itemNum, imagePart);

		_invalidateContents();
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

	public void setSelectedFlags(boolean[] selectedArray)
	{
		if(selectedArray.length < size()) { throw new IllegalArgumentException("Return array too small"); }
		if(selectedArray == null) { throw new NullPointerException("Return array cannot be null"); }

		for (int i=0; i<selectedArray.length && i<size(); i++) { selectedItems.set(i, selectedArray[i]); }

		_invalidateContents();
	}

	public void setSelectedIndex(int itemNum, boolean selected)
	{
		if (itemNum < 0 || itemNum >= size()) { throw new IndexOutOfBoundsException("Invalid element index"); }
		if (type == Choice.EXCLUSIVE || type == Choice.POPUP)
		{
			if(!selected) { return; }
			selectedIndex = itemNum;
			for (int i = 0; i < selectedItems.size(); i++)
			{
				selectedItems.set(i, false); // Deselect all others
			}
		}
		selectedItems.set(itemNum, selected);
		_invalidateContents();
	}

	public int size() { return strings.size(); }


	protected boolean traverse(int dir, int viewportWidth, int viewportHeight, int[] visRect_inout)
	{
		if (type == Choice.POPUP) { return false; }

		// intial traverse
		if (highlightedIndex == -1)
		{
			if (!strings.isEmpty())
			{
				highlightedIndex = dir == Canvas.UP ? strings.size() - 1 : 0;
				return true;
			}
			else { return false; }
		}
		else
		{
			if (dir == Canvas.UP && highlightedIndex > 0) { highlightedIndex--; }
			else if (dir == Canvas.DOWN && highlightedIndex < size()-1) { highlightedIndex++; }
			else { return false; }

			visRect_inout[1] = Font.getDefaultFont().getHeight() * highlightedIndex;
			visRect_inout[3] = Font.getDefaultFont().getHeight();

			_invalidateContents();
			return true;
		}
	}

	protected void traverseOut()
	{
		if (highlightedIndex != -1)
		{
			highlightedIndex = -1;
			_invalidateContents();
		}
	}


	protected boolean keyPressed(int key)
	{
		boolean handled = true;

		if (type == Choice.POPUP)
		{
			if ((key == Canvas.LEFT || key == Canvas.KEY_NUM4) && selectedIndex > 0)
			{
				selectedIndex--;
			}
			else if ((key == Canvas.RIGHT || key == Canvas.KEY_NUM6) && selectedIndex < size()-1)
			{
				selectedIndex++;
			}
			else { handled = false; }
		}
		else if ((key == Canvas.KEY_NUM5 || key == Canvas.FIRE) && highlightedIndex != -1)
		{
			if (type == Choice.EXCLUSIVE) { selectedIndex = highlightedIndex; }
			setSelectedIndex(highlightedIndex, !selectedItems.get(highlightedIndex));

			handled = true;
		}
		else { handled = false; }

		if (handled)
		{
			notifyStateChanged();
			_invalidateContents();
		}

		return handled;
	}


	protected int getContentHeight(int width)
	{
		if (type == Choice.POPUP) { return Font.getDefaultFont().getHeight() + (Font.getDefaultFont().getHeight() / 6); }
		else { return size() * Font.getDefaultFont().getHeight() + (Font.getDefaultFont().getHeight() / 6); }
	}

	protected void renderItem(Graphics graphics, int x, int y, int width, int height, boolean isSelected)
	{
		graphics.translate(x, y);

		int lineHeight = Font.getDefaultFont().getHeight();

		if (type == Choice.POPUP)
		{
			// Popup mode renders as a single button showing the currently selected item
			String text = (strings.size() > 0 && selectedIndex >= 0 && selectedIndex < strings.size()) ? strings.get(selectedIndex) : "";
			Image img = (images != null && selectedIndex >= 0 && selectedIndex < images.size()) ? images.get(selectedIndex) : null;

			LCDUIRenderer.drawItem(graphics, 0, text, img, 0, 0, width, lineHeight, isSelected, false, type, true, getLayout());
		}
		else
		{
			for (int t = 0; t < strings.size(); t++)
			{
				String text = strings.get(t);
				Image img = (images != null && t < images.size()) ? images.get(t) : null;

				// Determine checkbox / radio button checked status
				boolean checked = false;
				if (type == Choice.MULTIPLE) { checked = selectedItems.get(t).booleanValue(); }
				else if (type == Choice.EXCLUSIVE) { checked = (t == selectedIndex); }

				// Only the currently focused/highlighted row gets the visual "selected" gradient
				boolean rowSelected = isSelected && (highlightedIndex == t);

				// Compute unique vertical offset for each row inside the ChoiceGroup
				int itemY = t * lineHeight;

				LCDUIRenderer.drawItem(graphics, t, text, img, 0, itemY, width, lineHeight, rowSelected, checked, type, true, getLayout());
			}
		}

		graphics.translate(-x, -y);
	}

}
