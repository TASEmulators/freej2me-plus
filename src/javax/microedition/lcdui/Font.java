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

public final class Font
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

	private static final int[] fontSizes = 
	{
		8,  8, 10, // < 128 minimum px dimension
		8, 11, 12, // < 176 minimum px dimension
		9, 12, 14, // < 220 minimum px dimension
		10, 14, 16, // >= 220 minimum px dimension
	};

	private static int screenType = -4;
	private int face;
	private int style;
	private int size;

	private static Font defaultFont = null;

	public PlatformFont platformFont;

	private Font(int fontFace, int fontStyle, int fontSize)
	{
		if(face != FACE_SYSTEM && face != FACE_PROPORTIONAL && face != FACE_MONOSPACE
			&& style != STYLE_PLAIN && style != STYLE_ITALIC && style != STYLE_BOLD
			&& size != SIZE_SMALL && size != SIZE_MEDIUM && size != SIZE_LARGE) 
		{
			throw new IllegalArgumentException("Cannot create a font with invalid face, style or size");
		}

		face = fontFace;
		style = fontStyle;
		size = fontSize;
		platformFont = new PlatformFont(this);
	}

	public static void setScreenSize(int width, int height)
	{
		final int minSize = Math.min(width, height);
		if (minSize < 128) { screenType = 0; }
		else if (minSize < 176) { screenType = 1; }
		else if (minSize < 220) { screenType = 2; }
		else { screenType = 3; }
	}

	public int charsWidth(char[] ch, int offset, int length)
	{
		if(ch == null) { throw new NullPointerException("Cannot do charsWidth() with a null char array"); }
		if(offset < 0 || length < 0 || (offset+length) > ch.length) { throw new ArrayIndexOutOfBoundsException("charsWidth tried to access invalid char array index"); }
		
		String str = new String(ch, offset, length);
		return stringWidth(str);
	}

	public int charWidth(char ch) { return stringWidth(String.valueOf(ch)); }

	public int getBaselinePosition() { return platformFont.getAscent(); }

	public static Font getDefaultFont() 
	{ 
		if (defaultFont == null) { defaultFont = new Font(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM); }
		return defaultFont;
	}

	public int getFace() { return face; }

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

	public int getHeight() { return platformFont.getHeight(); }

	public int getSize() { return size; }

	public int getPointSize() { return convertSize(size); }

	public int getStyle() { return style; }

	public boolean isBold() { return (style & STYLE_BOLD) == STYLE_BOLD; }

	public boolean isItalic() { return (style & STYLE_ITALIC) == STYLE_ITALIC; }

	public boolean isPlain() { return style == STYLE_PLAIN; }

	public boolean isUnderlined() { return (style & STYLE_UNDERLINED) == STYLE_UNDERLINED; }

	public int stringWidth(String str) 
	{
		if(str == null) { throw new NullPointerException("Cannot get stringWidth from a null String"); }

		return platformFont.stringWidth(str); 
	}

	public int substringWidth(String str, int offset, int len) 
	{
		if(str == null) { throw new NullPointerException("Cannot get substringWidth of a null String"); }
		if(offset < 0 || len < 0 || (offset+len) > str.length()) {throw new StringIndexOutOfBoundsException("substringWidth tried to access invalid index on received string");}

		return stringWidth(str.substring(offset, offset+len)); 
	}

	private int convertSize(int size)
	{
		switch(size)
		{
			case SIZE_LARGE  : return fontSizes[3*screenType + 2];
			case SIZE_MEDIUM : return fontSizes[3*screenType + 1];
			case SIZE_SMALL  :
			default          : return fontSizes[3*screenType];
		}
	}
}
