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

public class TextBox extends Screen
{

	private String text;
	private int max;
	private int constraints;
	private String mode;
	private int caretPosition;
	private int padding;
	private int margin;


	public TextBox(String Title, String value, int maxSize, int Constraints)
	{
		title = Title;
		text = value;
		max = maxSize;
		constraints = Constraints;


		padding = Font.getDefaultFont().getHeight() / 5;
		margin = Font.getDefaultFont().getHeight() / 5;
	}

	public void delete(int offset, int length)
	{
		text = text.substring(0, offset) + text.substring(offset+length);
		if (caretPosition > text.length()) {
			caretPosition = text.length();
		}
		_invalidate();
	}

	public int getCaretPosition() { return caretPosition; }

	public int getChars(char[] data)
	{
		for(int i=0; i<text.length(); i++)
		{
			data[i] = text.charAt(i);
		}
		return text.length();
	}

	public int getConstraints() { return constraints; }

	public int getMaxSize() { return max; }

	public String getString() { return text; }

	public void insert(char[] data, int offset, int length, int position)
	{
		StringBuilder out = new StringBuilder();
		out.append(text, 0, position);
		out.append(data, offset, length);
		out.append(text.substring(position));
		text = out.toString();

		_invalidate();
	}

	public void insert(String src, int position)
	{
		StringBuilder out = new StringBuilder();
		out.append(text, 0, position);
		out.append(src);
		out.append(text.substring(position));
		text = out.toString();

		_invalidate();
	}

	public void setChars(char[] data, int offset, int length)
	{
		StringBuilder out = new StringBuilder();
		out.append(data, offset, length);
		text = out.toString();
		caretPosition = text.length();
		_invalidate();
	}

	public void setConstraints(int Constraints) { constraints = Constraints;  }

	public void setInitialInputMode(String characterSubset) { mode = characterSubset; }

	public int setMaxSize(int maxSize) { max = maxSize; return max; }

	public void setString(String value) 
	{ 
		text = value;
		caretPosition = text.length();
		_invalidate();
	}

	public void setTicker(Ticker tick) { ticker = tick; }

	public void setTitle(String s) { title = s; }

	public int size() { return text.length(); }

	public boolean screenKeyPressed(int key) 
	{
		boolean handled = true;

		Mobile.log(Mobile.LOG_WARNING, TextBox.class.getPackage().getName() + "." + TextBox.class.getSimpleName() + ": " + "TextBox keyPress handling not fully implemented!");

		// TODO: Flesh this out, as right now it does little more than move the caret on joypads
		if (key == Canvas.DOWN && caretPosition > 0) // Down acts as Backspace
		{
			text = text.substring(0, caretPosition-1) + text.substring(caretPosition);
			caretPosition--;
		}
		else if (key == Canvas.UP && caretPosition < text.length()) // Up works as a "delete" key
		{ 
			text = text.substring(0, caretPosition) + text.substring(caretPosition+1); 
		}
		else if (key == Canvas.LEFT && caretPosition > 0) { caretPosition--; } 
		else if (key == Canvas.RIGHT && caretPosition < text.length()) { caretPosition++; } 
		
		/* Pulled from zb3's fork. But we shouldn't rely on keyboard here
		else if (e.getKeyChar() > ' ' && e.getKeyChar() < 0x7f) 
		{
			char chr = e.getKeyChar();
			boolean ok = true;

			if (constraints == TextField.NUMERIC && !((chr >= '0' && chr <= '9') || chr == '-')) {
				ok = false;
			} else if (constraints == TextField.DECIMAL && !((chr >= '0' && chr <= '9') || chr == '-' || chr == '.' || chr == ',')) {
				ok = false;
			}

			if (ok) {
				text = text.substring(0, caretPosition) + String.valueOf(chr) + text.substring(caretPosition);
				caretPosition++;
			} else {
				handled = false;
			}
		} 
		*/
		else {
			handled = false;
		}
		
		if (handled) {
			_invalidate();
		}

		return handled;
	}

	protected String renderScreen(int x, int y, int width, int height) 
	{
		graphics.getGraphics2D().translate(x, y);

		graphics.setColor(Mobile.lcduiTextColor);
		graphics.drawRect(margin, margin, width-2*margin, Font.getDefaultFont().getHeight()+2*padding);

		graphics.drawString(text, margin+padding, margin+padding, 0);

		int cwidth = Font.getDefaultFont().stringWidth(text.substring(0, caretPosition));

		graphics.drawRect(margin+padding+cwidth, margin+padding, 0, Font.getDefaultFont().getHeight());
		
		graphics.getGraphics2D().translate(-x, -y);
		return null;
	}

 }
