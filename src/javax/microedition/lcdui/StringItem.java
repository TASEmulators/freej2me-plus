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
	protected int lineSpacing = 1;
	protected int cachedHeight = -1; // Use -1 or a tracked lastWidth to detect changes
	protected int lastMeasuredWidth = -1;
	private int buttonMargin;
	private int buttonPadding;


	public StringItem(String label, String textvalue)
	{
		this(label, textvalue, PLAIN);
	}

	public StringItem(String label, String textvalue, int appearanceMode)
	{
		if(appearanceMode < PLAIN || appearanceMode > BUTTON) { throw new IllegalArgumentException("Invalid appearance mode"); }
		setLabel(label);
		text = (textvalue == null ? "" : textvalue);
		buttonMargin = font.getHeight() / 5;
		buttonPadding = font.getHeight() / 3;

		appearance = appearanceMode;
	}

	public int getAppearanceMode() { return appearance; }

	public Font getFont() { return font; }

	public String getText() { return text; }

	public void setFont(Font newfont)
	{
		font = newfont != null ? newfont : Font.getDefaultFont();
		cachedHeight = -1;
		lastMeasuredWidth = -1;
		this._invalidateContents();
	}

	public void setText(String textvalue)
	{
		text = (textvalue == null ? "" : textvalue);
		cachedHeight = -1; // Invalidate cached height/lines
		lastMeasuredWidth = -1;
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
			return font.getHeight() + 2 * buttonMargin + 2 * buttonPadding;
		}

		if (text == null || text.length() == 0)
		{
			lines = new ArrayList<String>();
			return 0;
		}

		// Re-wrap the text only if width changed or the cache was invalidated
		if (cachedHeight == -1 || lastMeasuredWidth != width)
		{
			lastMeasuredWidth = width;
			lines = wrapText(text, width, font);
			cachedHeight = lines.size() > 0 ? (lines.size() * font.getHeight() + (lines.size() - 1) * lineSpacing) : 0;
		}

		return cachedHeight;
	}

	public static List<String> wrapText(String text, int width, Font font)
	{
		List<String> wrappedLines = new ArrayList<String>();
		if (text == null) { return wrappedLines; }

		for (String paragraph : text.split("\n", -1))
		{
			if (paragraph.length() == 0)
			{
				wrappedLines.add("");
				continue;
			}

			String currentLine = "";
			for (String word : paragraph.split(" "))
			{
				String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;

				if (font.stringWidth(candidate) <= width) { currentLine = candidate; }
				else
				{
					if (!currentLine.isEmpty()) { wrappedLines.add(currentLine); }

					// If a single word is wider than the whole screen, break it by character
					if (font.stringWidth(word) > width)
					{
						currentLine = "";
						for (int i = 0; i < word.length(); i++)
						{
							char ch = word.charAt(i);
							String charCandidate = currentLine + ch;
							if (font.stringWidth(charCandidate) > width)
							{
								wrappedLines.add(currentLine);
								currentLine = String.valueOf(ch);
							}
							else { currentLine = charCandidate; }
						}
					}
					else { currentLine = word; }
				}
			}
			wrappedLines.add(currentLine);
		}

		return wrappedLines;
	}

	protected void renderItem(Graphics graphics, int x, int y, int width, int height, boolean isSelected)
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
		else if (appearance == Item.HYPERLINK)
		{
			Font tmpFont = graphics.getFont();

			// Hyperlinks will be underlined and italic
			graphics.setFont(new Font(tmpFont.getFace(), tmpFont.getStyle() |
				Font.STYLE_UNDERLINED | Font.STYLE_ITALIC, tmpFont.getSize()));

			graphics.setColor(0, 102, 204);

			for (int l = 0; l < lines.size(); l++)
			{
				graphics.drawString(lines.get(l), x,
					y + l * font.getHeight() + (l > 0 ? (l - 1) * lineSpacing : 0), 0);
			}

			graphics.setFont(tmpFont);
			graphics.setColor(Mobile.lcduiTextColor);
		}
		else
		{
			graphics.setColor(Mobile.lcduiTextColor);
			for(int l=0;l<lines.size();l++)
			{
				graphics.drawString( lines.get(l), x,
					y + l*font.getHeight() + (l > 0 ? (l-1)*lineSpacing : 0), 0);
			}
		}
	}
}
