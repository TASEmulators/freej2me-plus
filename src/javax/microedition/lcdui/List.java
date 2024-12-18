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
import org.recompile.mobile.PlatformGraphics;

import java.util.ArrayList;

public class List extends Screen implements Choice
{

	public static Command SELECT_COMMAND = new Command("", Command.SCREEN, 0);

	protected int currentItem = -1;

	private int fitPolicy = Choice.TEXT_WRAP_ON;

	private ArrayList<Boolean> selectedItems = new ArrayList<Boolean>();

	private int type;

	public List(String title, int listType)
	{
		setTitle(title);
		type = listType;
	}

	public List(String title, int listType, String[] stringElements, Image[] imageElements)
	{
		this(title, listType);

		for(int i=0; i<stringElements.length; i++)
		{
			if(imageElements!=null)
			{
				items.add(new ImageItem(stringElements[i], imageElements[i], 0, stringElements[i]));
				selectedItems.add(false);
			}	
			else
			{
				items.add(new StringItem(stringElements[i], stringElements[i]));
				selectedItems.add(false);
			}
		}
	}

	public int append(String stringPart, Image imagePart)
	{ 
			if(imagePart!=null)
			{
				items.add(new ImageItem(stringPart, imagePart, 0, stringPart));
				selectedItems.add(false);
			}
			else
			{
				items.add(new StringItem(stringPart, stringPart));
				selectedItems.add(false);
			}
			_invalidate();
			return items.size()-1;
	}

	public void delete(int elementNum)
	{
		try { items.remove(elementNum); selectedItems.remove(elementNum); }
		catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, List.class.getPackage().getName() + "." + List.class.getSimpleName() + ": " + "Failed delete element from list:" + e.getMessage()); }
		_invalidate();
	}

	public void deleteAll() { items.clear(); selectedItems.clear(); _invalidate(); }

	public int getFitPolicy() { return fitPolicy; }

	public Font getFont(int elementNum) { return Font.getDefaultFont(); }

	public Image getImage(int elementNum) { return ((ImageItem)(items.get(elementNum))).getImage(); }
	
	public int getSelectedFlags(boolean[] selectedArray_return) 
	{
		if(type == Choice.IMPLICIT) { return 0; }
		int numSelected = 0;
		for(int i = 0; i < selectedItems.size(); i++) 
		{
			if(selectedItems.get(i) == true) { selectedArray_return[i] = true; numSelected++; }
			else { selectedArray_return[i] = false; }
		}
		return numSelected;
	}

	public int getSelectedIndex() 
	{
		if(type == Choice.IMPLICIT || type == Choice.EXCLUSIVE) { return currentItem; }

		return -1; 
	}

	public String getString(int elementNum)
	{
		Item item = items.get(elementNum);

		if (item instanceof StringItem) { return ((StringItem)item).getText(); }
		else { return item.getLabel(); }
	}

	public void insert(int elementNum, String stringPart, Image imagePart)
	{
		if(elementNum<items.size() && elementNum>=0)
		{
			try
			{
				if(imagePart!=null)
				{
					items.add(elementNum, new ImageItem(stringPart, imagePart, 0, stringPart));
					selectedItems.add(elementNum, false);
				}
				else
				{
					items.add(elementNum, new StringItem(stringPart, stringPart));
					selectedItems.add(elementNum, false);
				}
				if (currentItem >= elementNum) { currentItem++; }
				_invalidate();
			}
			catch(Exception e)
			{
				append(stringPart, imagePart);
			}
		}
		else
		{
			append(stringPart, imagePart);
		}
	}

	public boolean isSelected(int elementNum) 
	{ 
		if(type == Choice.IMPLICIT) { return elementNum==currentItem; }

		return selectedItems.get(elementNum); 
	}

	@Override
	public void removeCommand(Command cmd) 
	{
		if(cmd == SELECT_COMMAND) { setSelectCommand(null); }
		commands.remove(cmd);
		_invalidate(); 
	}

	public void set(int elementNum, String stringPart, Image imagePart)
	{
		if(imagePart!=null)
		{
			items.set(elementNum, new ImageItem(stringPart, imagePart, 0, stringPart));
		}
		else
		{
			items.set(elementNum, new StringItem(stringPart, stringPart));
		}
	}
	
	public void setFitPolicy(int fitpolicy) { fitPolicy = fitpolicy; }

	public void setFont(int elementNum, Font font) { }
		
	public void setSelectCommand(Command command) 
	{ 
		SELECT_COMMAND = command;
		commands.add(command);
	}
 
	public void setSelectedFlags(boolean[] selectedArray) 
	{ 
		for(int i = 0; i < selectedArray.length; i++) 
		{
			setSelectedIndex(i, selectedArray[i]);
		}
	}

	public void setSelectedIndex(int elementNum, boolean selected)
	{
		if(type == Choice.IMPLICIT) 
		{
			if(selected == true) { currentItem = elementNum; }
			else { currentItem = 0; }
			_invalidate();
			return;
		}

		selectedItems.set(elementNum, selected);

		if(type == Choice.EXCLUSIVE) // Deselect everyone else
		{
			for(int i = 0; i < selectedItems.size(); i++) 
			{
				if(i != elementNum) { selectedItems.set(elementNum, false); }
			}
		}

		_invalidate();
	}

	//void setTicker(Ticker ticker)
	
	//void setTitle(String s)

	public int size() { return items.size(); }

	/*
		Draw list, handle input
	*/

	public boolean screenKeyPressed(int key)
	{
		if(items.size()<1) { return false; }
		boolean handled = true;

		if (key == Canvas.UP || key == Canvas.KEY_NUM2) { currentItem--; } 
		else if (key == Canvas.DOWN || key == Canvas.KEY_NUM8) { currentItem++; }
		else if (key == Canvas.FIRE || key == Canvas.KEY_NUM5) { doDefaultCommand(); }
		else { handled = false; }

		if (currentItem>=items.size()) { currentItem=0; }
		if (currentItem<0) { currentItem = items.size()-1; }

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
				setSelectedIndex(currentItem, !selectedItems.get(currentItem));
			}
		}
	}

	public String renderScreen(int x, int y, int width, int height)
	{
		if(items.size()>0)
		{
			if(currentItem<0) { currentItem = 0; }

			int listPadding = Font.getDefaultFont().getHeight()/5;
			int itemHeight = Font.getDefaultFont().getHeight();
			int imagePadding = Font.getDefaultFont().getHeight()/4;

			int ah = height - 2*listPadding; // allowed height
			int max = Math.max(1, (int) Math.floor(ah / itemHeight)); // max items per page (minimum of 1)

			if(items.size()<max) { max = items.size(); }
		
			int page = 0;
			page = (int)Math.floor(currentItem/max); // current page
			int first = page * max; // first item to show
			int last = first + max - 1;

			if(last>=items.size()) { last = items.size()-1; }
			
			y += listPadding;
			for(int i=first; i<=last; i++)
			{
				if(currentItem == i)
				{
					// Don't touch the edges of the screen, or the border from another item for better spacing
					graphics.fillRect(x+2, y+1, width-4, itemHeight-1);
					graphics.setColor(Mobile.lcduiBGColor);
				}

				if(type == Choice.MULTIPLE) 
				{
					graphics.drawRect(x+5, y+2, itemHeight-4, itemHeight-4);
					if(selectedItems.get(i) == true) 
					{
						graphics.fillRect(x+7, y+4, itemHeight-7, itemHeight-7);
					}
				}
				else if(type == Choice.EXCLUSIVE) // Exclusive will have Radio-Button selectors
				{
					graphics.drawRoundRect(x+5, y+2, itemHeight-4, itemHeight-4, itemHeight-4, itemHeight-4);
					if(selectedItems.get(i) == true) 
					{
						graphics.fillRoundRect(x+7, y+4, itemHeight-8, itemHeight-8, itemHeight-8, itemHeight-8);
					}
				}
				

				graphics.drawString(items.get(i).getLabel(), width/2, y, Graphics.HCENTER);

				if(items.get(i) instanceof StringItem)
				{
					graphics.drawString(((StringItem)items.get(i)).getText(), x+width/2, y, Graphics.HCENTER);
				}

				graphics.setColor(Mobile.lcduiTextColor);
				if(items.get(i) instanceof ImageItem)
				{
					graphics.drawImage(((ImageItem)items.get(i)).getImage(), x+imagePadding, y, 0);
				}
				
				y += itemHeight;
			}
		}

		return ""+(currentItem+1)+" of "+items.size();
	}
}
