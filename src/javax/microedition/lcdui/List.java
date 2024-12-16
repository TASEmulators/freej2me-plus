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

	public static Command SELECT_COMMAND = new Command("Select", Command.SCREEN, 0);

	protected int currentItem = -1;

	private int fitPolicy = Choice.TEXT_WRAP_ON;

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
			}	
			else
			{
				items.add(new StringItem(stringElements[i], stringElements[i]));
			}
		}
	}

	public int append(String stringPart, Image imagePart)
	{ 
			if(imagePart!=null)
			{
				items.add(new ImageItem(stringPart, imagePart, 0, stringPart));
			}
			else
			{
				items.add(new StringItem(stringPart, stringPart));
			}
			_invalidate();
			return items.size()-1;
	}

	public void delete(int elementNum)
	{
		try { items.remove(elementNum); }
		catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, List.class.getPackage().getName() + "." + List.class.getSimpleName() + ": " + "Failed delete element from list:" + e.getMessage()); }
		_invalidate();
	}

	public void deleteAll() { items.clear(); _invalidate(); }

	public int getFitPolicy() { return fitPolicy; }

	public Font getFont(int elementNum) { return Font.getDefaultFont(); }

	public Image getImage(int elementNum) { return ((ImageItem)(items.get(elementNum))).getImage(); }
	
	public int getSelectedFlags(boolean[] selectedArray_return) { return 0; }

	public int getSelectedIndex() { return currentItem; }

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
				}
				else
				{
					items.add(elementNum, new StringItem(stringPart, stringPart));
				}
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

	public boolean isSelected(int elementNum) { return elementNum==currentItem; }

	// public void removeCommand(Command cmd) {  }

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
		
	public void setSelectCommand(Command command) { SELECT_COMMAND = command; }
 
	public void setSelectedFlags(boolean[] selectedArray) { }

	public void setSelectedIndex(int elementNum, boolean selected)
	{
		if(selected == true)
		{
			currentItem = elementNum;
		}
		else
		{
			currentItem = 0;
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
			commandlistener.commandAction(SELECT_COMMAND, this);
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
