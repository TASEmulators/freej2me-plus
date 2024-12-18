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
import org.recompile.mobile.PlatformGraphics;



public class ChoiceGroup extends Item implements Choice
{

	private String label;

    private int type;

	private ArrayList<String> strings = new ArrayList<String>();

	private ArrayList<Image> images = new ArrayList<Image>();

	private int fitPolicy;

	private int selectedIndex = -1;
	private int highlightedIndex = -1;
	private ArrayList<Boolean> selectedElements = new ArrayList<Boolean>();

	public ChoiceGroup(String choiceLabel, int choiceType)
	{
		setLabel(choiceLabel);
		type = choiceType;
	}

	public ChoiceGroup(String choiceLabel, int choiceType, String[] stringElements, Image[] imageElements)
	{
		this(choiceLabel, choiceType);
		for(int i=0; i<stringElements.length; i++) 
		{
			strings.add(stringElements[i]);
			images.add((imageElements != null && i<imageElements.length) ? imageElements[i] : null);
			selectedElements.add(false);
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
		strings.add(stringPart);
		images.add(imagePart);
		selectedElements.add(false);

		if (!strings.isEmpty() && selectedIndex == -1) { selectedIndex = 0; }
		invalidate();

		return strings.size() - 1;
		
	}

	public void delete(int itemNum) 
	{
		strings.remove(itemNum);
		images.remove(itemNum);
		selectedElements.remove(itemNum);

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
		strings.clear(); images.clear(); selectedElements.clear();
		selectedIndex = highlightedIndex = -1;
		invalidate();
	}

	public int getFitPolicy() { return fitPolicy; }

	public Font getFont(int itemNum) 
	{
		Mobile.log(Mobile.LOG_WARNING, Choice.class.getPackage().getName() + "." + Choice.class.getSimpleName() + ": " + "getFont() called.");
		return Font.getDefaultFont();
	}

	public Image getImage(int elementNum) { return images.get(elementNum); }

	public int getSelectedFlags(boolean[] selectedArray_return) 
	{ 
		int numSelected = 0;

		for (int i=0; i<selectedElements.size(); i++) 
		{
			if(selectedElements.get(i) == true) { selectedArray_return[i] = true; numSelected++; }
			else { selectedArray_return[i] = false; }
		}

		return numSelected;
	}

  	public int getSelectedIndex() 
	{ 
		if(type == Choice.POPUP || type == Choice.EXCLUSIVE) { return selectedIndex; }

		return -1;
	}

	public String getString(int elementNum) { return strings.get(elementNum); }

	public void insert(int elementNum, String stringPart, Image imagePart)
	{
		strings.add(elementNum, stringPart);
		images.add(elementNum, imagePart);
		selectedElements.add(elementNum, false);

		if (selectedIndex >= elementNum) { selectedIndex++; }
		if (highlightedIndex >= elementNum) { highlightedIndex++; }
		if (!strings.isEmpty() && selectedIndex == -1) { selectedIndex = 0; }

		invalidate();
	}

	public boolean isSelected(int elementNum) 
	{
		if(type == Choice.EXCLUSIVE) { return elementNum==selectedIndex; }
		return selectedElements.get(elementNum); 
	}

	public void set(int elementNum, String stringPart, Image imagePart)
	{
		strings.set(elementNum, stringPart);
		images.set(elementNum, imagePart);

		_invalidateContents();
	}

	public void setFitPolicy(int policy) { fitPolicy = policy; }

	public void setFont(int itemNum, Font font) 
	{ 
		Mobile.log(Mobile.LOG_WARNING, Choice.class.getPackage().getName() + "." + Choice.class.getSimpleName() + ": " + "setFont() called.");
	}

	public void setSelectedFlags(boolean[] selectedArray) 
	{ 
		for (int i=0; i<selectedArray.length && i<size(); i++) { selectedElements.set(i, selectedArray[i]); }

		_invalidateContents();
	}

	public void setSelectedIndex(int elementNum, boolean selected) 
	{
		if (elementNum < 0 || elementNum >= size()) { return; }
		if (type == Choice.EXCLUSIVE) 
		{
			selectedIndex = elementNum;
			for (int i = 0; i < selectedElements.size(); i++) 
			{
				selectedElements.set(i, false); // Deselect all others
			}
		}
		selectedElements.set(elementNum, selected);
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
			setSelectedIndex(highlightedIndex, !selectedElements.get(highlightedIndex));

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

	protected void renderItem(PlatformGraphics graphics, int x, int y, int width, int height) 
	{
		graphics.getGraphics2D().translate(x, y);
		
		if (type == Choice.POPUP) 
		{
			int arrowSpacing = _drawArrow(graphics, -1,  selectedIndex > 0, 0, 0, width, height);

			graphics.setColor(Mobile.lcduiTextColor);
			graphics.drawString(strings.get(selectedIndex), arrowSpacing, 0, 0);

			_drawArrow(graphics, 1, selectedIndex < size()-1, 0, 0, width, height);
		} 
		else 
		{
			int lineHeight = Font.getDefaultFont().getHeight();
			int tickOffset = lineHeight*4/3;
			int textPadding = lineHeight/5;

			for (int t=0; t<strings.size(); t++) {
				if (type == Choice.MULTIPLE) 
				{
					_drawTick(graphics, t, lineHeight, selectedElements.get(t).booleanValue(), false);					
				} 
				else { _drawTick(graphics, t, lineHeight, t == selectedIndex, true);	}

				if (highlightedIndex == t) 
				{
					graphics.fillRect(tickOffset, t*lineHeight, width-tickOffset, lineHeight);
					graphics.setColor(Mobile.lcduiBGColor);
				}

				if (images.get(t) != null) 
				{
					graphics.drawImage(images.get(t), tickOffset+textPadding, t*lineHeight, 0);
					graphics.drawString(strings.get(t), tickOffset+textPadding+lineHeight, t*lineHeight, 0);
				} 
				else { graphics.drawString(strings.get(t), tickOffset+textPadding, t*lineHeight, 0); }

				graphics.setColor(Mobile.lcduiTextColor);
			}
		}

		graphics.getGraphics2D().translate(-x, -y);
	}

	private void _drawTick(PlatformGraphics graphics, int index, int height, boolean filled, boolean isCircle) 
	{
		int tickMargin = height/2;
		int tickWidth = height/2;
	  
		if (isCircle)
		{
			if (filled) 
			{
				graphics.fillArc(tickMargin, index * height + height/2 - tickWidth/2, tickWidth, tickWidth, 0, 360);
			} 
			else 
			{
				graphics.drawArc(tickMargin, index * height + height/2 - tickWidth/2, tickWidth, tickWidth, 0, 360);
			}
		} 
		else 
		{
			if (filled) 
			{
				graphics.fillRect(tickMargin, index * height + height/2 - tickWidth/2, tickWidth, tickWidth);
			} 
			else 
			{
				graphics.drawRect(tickMargin, index * height + height/2 - tickWidth/2, tickWidth, tickWidth);
			}
		}  
	}

}
