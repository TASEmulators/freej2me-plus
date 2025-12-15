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
import java.util.List;

import org.recompile.mobile.Mobile;

public class StringItem extends Item
{

	private String text;
	private int appearance;
	protected List<String> lines;
	protected int lineSpacing;
	protected int height = 0;
	private int buttonMargin;
	private int buttonPadding;


	public StringItem(String label, String textvalue)
	{
		setLabel(label);
		text = (textvalue == null ? "" : textvalue);
		buttonMargin = Font.getDefaultFont().getHeight() / 5;
		buttonPadding = Font.getDefaultFont().getHeight() / 3;
	}

	public StringItem(String label, String textvalue, int appearanceMode)
	{
		this(label, textvalue);
		appearance = appearanceMode;
	}

	public int getAppearanceMode() { return appearance; }

	public Font getFont() { return Font.getDefaultFont(); }

	public String getText() { return text; }

	public void setFont(Font newfont) { }

	public void setText(String textvalue) 
	{ 
		text = (textvalue == null ? "" : textvalue);
		height = 0; 
		this._invalidateContents(); 
	}

	protected boolean keyPressed(int key) 
	{ 
		if (key == Canvas.FIRE || key == Canvas.KEY_NUM5 || key == Canvas.KEY_SOFT_LEFT) 
		{
			doDefaultCommand();
		}

		// We don't need to set handled to true here
		return false;
	}

	protected int getContentHeight(int width) 
	{
		if (appearance == Item.BUTTON)
		{
			height = Font.getDefaultFont().getHeight() + 2*buttonMargin + 2*buttonPadding;
		} 
		else if (height == 0 && text != null && !text.isEmpty()) 
		{
			lines = wrapText(text, width, Font.getDefaultFont());
			lineSpacing = 1;

			height = lines.size() > 0 ? (lines.size()*Font.getDefaultFont().getHeight() + (lines.size()-1)*lineSpacing) : 0;
		} 
		else if ((text == null || text.isEmpty()) && lines == null) { lines = new ArrayList<String>(); }

		return height;
	}

	public static List<String> wrapText(String text, int width, Font font) 
	{
		String[] lines = text.split("\n", -1);
		List<String> wrappedLines = new ArrayList<String>();
	
		for (String line : lines) 
		{
			StringBuilder wrappedLine = new StringBuilder();
	
			for (int i = 0; i < line.length(); i++) 
			{
				String word = String.valueOf(line.charAt(i));
				String wrapCandidate = wrappedLine.length() == 0 ? word : wrappedLine + word;
				int wrapCandidateWidth = font.stringWidth(wrapCandidate);
	
				if (wrapCandidateWidth > width) 
				{
					if (wrappedLine.length() > 0) { wrappedLines.add(wrappedLine.toString()); }
					wrappedLine = new StringBuilder(word);
				} 
				else { wrappedLine.append(word); }
			}
	
			if (wrappedLine.length() > 0) { wrappedLines.add(wrappedLine.toString()); }
		}
	
		return wrappedLines;
	}

	protected void renderItem(Graphics graphics, int x, int y, int width, int height) 
	{
		if (appearance == Item.BUTTON)
		{
			graphics.setColor(Mobile.lcduiBGColor);
			graphics.fillRect(x+buttonMargin, y+buttonMargin, width-2*buttonMargin, height-2*buttonMargin);

			graphics.setColor(Mobile.lcduiStrokeColor);
			graphics.drawRect(x+buttonMargin, y+buttonMargin, width-2*buttonMargin, height-2*buttonMargin);

			graphics.setColor(Mobile.lcduiTextColor);
			graphics.drawString(text, x+buttonMargin+buttonPadding, y+buttonMargin+buttonPadding, 0);
		} 
		else
		{
			graphics.setColor(Mobile.lcduiTextColor);
			for(int l=0;l<lines.size();l++) 
			{
				graphics.drawString( lines.get(l), x,
					y + l*Font.getDefaultFont().getHeight() + (l > 0 ? (l-1)*lineSpacing : 0),
					Graphics.LEFT);
			}
		}

	}

}
