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
import org.recompile.mobile.PlatformFont;

public class Font extends PlatformFont
{
	public static final int FACE_MONOSPACE = 32;
	public static final int FACE_PROPORTIONAL = 64;
	public static final int FACE_SYSTEM = 0;

	public static final int FONT_INPUT_TEXT = 1;
	public static final int FONT_STATIC_TEXT = 0;

	public static final int SIZE_LARGE = 16;
	public static final int SIZE_MEDIUM = 0;
	public static final int SIZE_SMALL = 8;

	public static final int STYLE_BOLD = 1;
	public static final int STYLE_ITALIC = 2;
	public static final int STYLE_PLAIN = 0;
	public static final int STYLE_UNDERLINED = 4;

	public Font(int face, int style, int size)
	{
		super(face, style, size, true);
	}

	public int charsWidth(char[] ch, int offset, int length)
	{
		if(ch == null) { throw new NullPointerException("Cannot do charsWidth() with a null char array"); }
		if(offset < 0 || length < 0 || (offset+length) > ch.length) { throw new ArrayIndexOutOfBoundsException("charsWidth tried to access invalid char array index"); }

		String str = new String(ch, offset, length);
		return stringWidth(str);
	}

	public int charWidth(char ch) { return stringWidth(String.valueOf(ch)); }

	public int getBaselinePosition() { return getAscent(); }

	public static Font getDefaultFont() { return defaultFont; }

	public static Font getFont(int fontSpecifier)
	{
		if(fontSpecifier != FONT_INPUT_TEXT && fontSpecifier != FONT_STATIC_TEXT) { throw new IllegalArgumentException("Cannot get font with an invalid specifier"); }

		return defaultFont;
	}

	public static Font getFont(int face, int style, int size)
	{
		if(face != FACE_SYSTEM && face != FACE_PROPORTIONAL && face != FACE_MONOSPACE
			&& style != STYLE_PLAIN && style != STYLE_ITALIC && style != STYLE_BOLD
			&& size != SIZE_SMALL && size != SIZE_MEDIUM && size != SIZE_LARGE)
		{
			throw new IllegalArgumentException("Cannot get a font with invalid face, style or size");
		}

		return new Font(face, style, size);
	}

	public boolean isBold() { return (style & STYLE_BOLD) == STYLE_BOLD; }

	public boolean isItalic() { return (style & STYLE_ITALIC) == STYLE_ITALIC; }

	public boolean isPlain() { return style == STYLE_PLAIN; }

	public boolean isUnderlined() { return (style & STYLE_UNDERLINED) == STYLE_UNDERLINED; }
}
