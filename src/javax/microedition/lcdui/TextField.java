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
import org.recompile.mobile.PlatformGraphics;

public class TextField extends Item
{
	public static final int ANY = 0;
	public static final int CONSTRAINT_MASK = 0xFFFF;
	public static final int DECIMAL = 5;
	public static final int EMAILADDR = 1;
	public static final int INITIAL_CAPS_SENTENCE = 0x200000;
	public static final int INITIAL_CAPS_WORD = 0x100000;
	public static final int NON_PREDICTIVE = 0x80000;
	public static final int NUMERIC = 2;
	public static final int PASSWORD = 0x10000;
	public static final int PHONENUMBER = 3;
	public static final int SENSITIVE = 0x40000;
	public static final int UNEDITABLE = 0x20000;
	public static final int URL = 4;


	private String text;
	private int max;
	private int constraints;
	private int caretPosition = 0;
	private String mode;
	private int padding;
	private int margin;
	private boolean hilighted;

	public TextField(String label, String value, int maxSize, int Constraints)
	{
		setLabel(label);
		text = value == null ? "" : value;
		max = maxSize;
		constraints = Constraints;

		// these can't be static because of Font.getDefaultFont().getHeight()
		padding = Font.getDefaultFont().getHeight() / 5; 
		margin = Font.getDefaultFont().getHeight() / 5;
	}

	void delete(int offset, int length)
	{
		text = text.substring(0, offset) + text.substring(offset+length);
		if (caretPosition > text.length()) { caretPosition = text.length(); }
		_invalidateContents();
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

		_invalidateContents();
	}

	public void insert(String src, int position)
	{
		StringBuilder out = new StringBuilder();
		out.append(text, 0, position);
		out.append(src);
		out.append(text.substring(position));
		text = out.toString();

		_invalidateContents();
	}

	public void setChars(char[] data, int offset, int length)
	{
		if (data == null) {
			setString("");
			return;
		}

		StringBuilder out = new StringBuilder();
		out.append(data, offset, length);
		text = out.toString();
		caretPosition = text.length();
		_invalidateContents();
	}

	public void setConstraints(int Constraints) { 
		constraints = Constraints;
		_invalidateContents(); // because it might change arrows visibility
	}

	public void setInitialInputMode(String characterSubset) { mode = characterSubset; }

	public int setMaxSize(int maxSize) { max = maxSize; return max; }

	public void setString(String value) 
	{
		if (value == null) { value = ""; }
		
		text = value;
		caretPosition = text.length();
		_invalidateContents();
	}

	public int size() { return text.length(); }

	protected int getContentHeight(int width) { return Font.getDefaultFont().getHeight() + padding*2 + 2*margin; /* padding */ }

	protected boolean keyPressed(int key) 
	{
		boolean handled = true, changed = false;

		Mobile.log(Mobile.LOG_WARNING, TextField.class.getPackage().getName() + "." + TextField.class.getSimpleName() + ": " + "TextField keyPress handling not fully implemented!");

		if (key == Canvas.DOWN && caretPosition > 0) // Same as TextBox, DOWN works as Backspace
		{
			text = text.substring(0, caretPosition-1) + text.substring(caretPosition);
			caretPosition--;
			changed = true;
		} 
		else if (key == Canvas.UP && caretPosition < text.length()) // Also just as TextBox, UP works as Delete
		{
			text = text.substring(0, caretPosition) + text.substring(caretPosition+1);
			changed = true;
		} 
		else if (constraints != NUMERIC && key == Canvas.LEFT && caretPosition > 0) { caretPosition--; } 
		else if (constraints != NUMERIC && key == Canvas.RIGHT && caretPosition < text.length()) { caretPosition++; } 
		else if (constraints == NUMERIC && key == Canvas.LEFT) 
		{
			int value = 0;

			try {
				value = Integer.parseInt(text);
			} catch(Exception exc) {}

			value--;

			text = Integer.toString(value);
			changed = true;
		} 
		else if (constraints == NUMERIC && key == Canvas.RIGHT) 
		{
			int value = 0;

			try { value = Integer.parseInt(text); } 
			catch(Exception exc) {}

			value++;

			text = Integer.toString(value);
			changed = true;
		} 
		/* Same as TextBox here too
		else if (e.getKeyChar() > ' ' && e.getKeyChar() < 0x7f) 
		{
			char chr = e.getKeyChar();
			boolean ok = true;

			if (constraints == NUMERIC && !((chr >= '0' && chr <= '9') || chr == '-')) { ok = false; } 
			else if (constraints == DECIMAL && !((chr >= '0' && chr <= '9') || chr == '-' || chr == '.' || chr == ',')) { ok = false; }

			if (ok) 
			{
				text = text.substring(0, caretPosition) + String.valueOf(chr) + text.substring(caretPosition);
				caretPosition++;
				changed = true;
			} 
			else { handled = false; }
		} 
		*/
		else { handled = false; }

		if (changed) { notifyStateChanged(); }
		
		if (handled) { _invalidateContents(); }

		return handled;
	}

	protected void renderItem(PlatformGraphics graphics, int x, int y, int width, int height) 
	{
		graphics.getGraphics2D().translate(x, y);

		int arrowSpacing = 0;

		if (constraints == NUMERIC) 
		{
			arrowSpacing = _drawArrow(graphics, -1,  true, 0, margin+padding, width, Font.getDefaultFont().getHeight());
		}

		graphics.setColor(Mobile.lcduiTextColor);
		graphics.drawRect(arrowSpacing+margin, margin, width-2*arrowSpacing-2*margin, Font.getDefaultFont().getHeight()+2*padding);

		graphics.drawString(text, arrowSpacing+margin+padding, margin+padding, 0);

		int cwidth = Font.getDefaultFont().stringWidth(text.substring(0, caretPosition));

		if (hilighted) 
		{
			graphics.drawRect(arrowSpacing+margin+padding+cwidth, margin+padding, 0, Font.getDefaultFont().getHeight());
		}

		if (constraints == NUMERIC) {
			_drawArrow(graphics, 1,  true, 0, margin+padding, width, Font.getDefaultFont().getHeight());
		}
	
		
		graphics.getGraphics2D().translate(-x, -y);
	}

	protected boolean traverse(int dir, int viewportWidth, int viewportHeight, int[] visRect_inout) 
	{
		if (!hilighted) 
		{
			hilighted = true;
			_invalidateContents();
		}
		
		return false;
	}

	protected void traverseOut() 
	{ 
		if (hilighted) 
		{
			hilighted = false;
			_invalidateContents();
		}
	}
}
