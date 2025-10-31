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
package org.recompile.mobile;

import javax.microedition.lcdui.Font;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.font.TextAttribute;

import java.io.File;
import java.io.FilenameFilter;

import java.util.Hashtable;
import java.util.Map;

public class PlatformFont
{
	protected static final byte[] fontSizes = 
	{
		 7,  8, 10, 12, // < 128 minimum px dimension
		 9, 11, 13, 14, // < 176 minimum px dimension
		10, 12, 13, 15, // < 220 minimum px dimension
		11, 13, 15, 17, // >= 220 minimum px dimension
	};

	// Helps LCDUI to better adjust for different screen sizes.
	public static final byte[] fontPadding =
	{
		1, // < 128 minimum px dimension
		2, // < 176 minimum px dimension
		2, // < 220 minimum px dimension
		3  // >= 220 minimum px dimension
	};

	protected boolean isLCDUI;
	
	public static byte screenType = -4;
	protected int face, style, size;
	protected int ascent, descent, height;

	protected static com.nttdocomo.ui.Font defaultDoJaFont = null;
	protected static Font defaultFont = null;

	private FontMetrics metrics;
	private static Graphics gc; // Used only to get the FontMetrics object for any created font
	public java.awt.Font awtFont;
	private static final File textfontDir = new File("freej2me_system" + File.separatorChar + "customFont" + File.separatorChar);
	

	public PlatformFont(int face, int style, int size, boolean isLCDUI)
	{
		// Validate font settings first
		if(isLCDUI && face != Font.FACE_SYSTEM && face != Font.FACE_PROPORTIONAL && face != Font.FACE_MONOSPACE
			&& style != Font.STYLE_PLAIN && style != Font.STYLE_ITALIC && style != Font.STYLE_BOLD
			&& size != Font.SIZE_SMALL && size != Font.SIZE_MEDIUM && size != Font.SIZE_LARGE) 
		{
			throw new IllegalArgumentException("Cannot create a LCDUI font with invalid face, style or size. style " + style + " face " + face + " size " + size);
		}

		if(!isLCDUI && face != com.nttdocomo.ui.Font.FACE_SYSTEM && face != com.nttdocomo.ui.Font.FACE_PROPORTIONAL && face != com.nttdocomo.ui.Font.FACE_MONOSPACE
			&& style != com.nttdocomo.ui.Font.STYLE_PLAIN && style != com.nttdocomo.ui.Font.STYLE_ITALIC && style != com.nttdocomo.ui.Font.STYLE_BOLD && style != com.nttdocomo.ui.Font.STYLE_BOLDITALIC
			&& size != com.nttdocomo.ui.Font.SIZE_SMALL && size != com.nttdocomo.ui.Font.SIZE_MEDIUM && size != com.nttdocomo.ui.Font.SIZE_LARGE) 
		{
			throw new IllegalArgumentException("Cannot create a DoJa font with invalid face, style or size. style " + style + " face " + face + " size " + size);
		}

		// Set over-arching font attributes (awtFont is internal and so are its face, size, etc properties)
		this.isLCDUI = isLCDUI;
		this.face = face;
		this.style = style;
		this.size = size;
		

		// Check the custom font path and use the custom font if enabled
		if(!textfontDir.isDirectory()) 
		{
			try 
			{
				textfontDir.mkdirs();
				File dummyFile = new File(textfontDir.getPath() + File.separatorChar + "Put your ttf font here");
				dummyFile.createNewFile();
			}
			catch(Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformFont.class.getPackage().getName() + "." + PlatformFont.class.getSimpleName() + ": " + "Failed to create custom font dir:" + e.getMessage()); }
		}

		/* Get the first ttf font in the directory, if there's any */
		String[] fontfiles = textfontDir.list(new FilenameFilter()
		{
			@Override
			public boolean accept(File f, String textfont) {
				String lowerCaseFont = textfont.toLowerCase();
				return lowerCaseFont.endsWith(".ttf") || 
						lowerCaseFont.endsWith(".otf") || 
						lowerCaseFont.endsWith(".ttc");
			}
		});

		if (Mobile.useCustomTextFont && fontfiles != null && fontfiles.length > 0) // Load a custom font if enabled, and there is one
		{
            try 
			{
                awtFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, new File(textfontDir, fontfiles[0])).deriveFont(isLCDUI ? getStyle() : convertDoJaToLCDUIStyle(getStyle()), getPointSize());
            } 
			catch (Exception e) // If there's an issue loading it, we can still fallback to the default
			{
				Mobile.log(Mobile.LOG_ERROR, PlatformFont.class.getPackage().getName() + "." + PlatformFont.class.getSimpleName() + ": " + "Failed to load custom font:" + e.getMessage());
				// Fallback
				String fontFace = java.awt.Font.SANS_SERIF;
				if((isLCDUI ? getFace() : convertDoJaToLCDUIFace(getFace())) == Font.FACE_MONOSPACE) { fontFace = java.awt.Font.MONOSPACED; }
				else if((isLCDUI ? getFace() : convertDoJaToLCDUIFace(getFace())) == Font.FACE_PROPORTIONAL) { fontFace = java.awt.Font.DIALOG; }

				awtFont = new java.awt.Font(fontFace, isLCDUI ? getStyle() : convertDoJaToLCDUIStyle(getStyle()), getPointSize());
            }
        }
		else if(!Mobile.useCustomTextFont) // If the user is not going to use custom fonts, or there are no custom fonts in the directory, load the defaults
		{
			// We'll use SansSerif for SYSTEM
			String fontFace = java.awt.Font.SANS_SERIF;
			if((isLCDUI ? getFace() : convertDoJaToLCDUIFace(getFace())) == Font.FACE_MONOSPACE) { fontFace = java.awt.Font.MONOSPACED; }
			else if((isLCDUI ? getFace() : convertDoJaToLCDUIFace(getFace())) == Font.FACE_PROPORTIONAL) { fontFace = java.awt.Font.DIALOG; }

			awtFont = new java.awt.Font(fontFace, isLCDUI ? getStyle() : convertDoJaToLCDUIStyle(getStyle()), getPointSize());
		}
		

		// Standard java doesn't handle underlining the same way, so do it here (LCDUI is the only one that supports underlining)
		if(isLCDUI && (getStyle() & Font.STYLE_UNDERLINED) > 0)
		{
			Map<TextAttribute, Object> map = new Hashtable<TextAttribute, Object>(1);
			map.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);

			awtFont = awtFont.deriveFont(map);
		}

		if(gc == null) { gc = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).getGraphics(); }
		gc.setFont(awtFont);
		metrics = gc.getFontMetrics();
		height = gc.getFontMetrics().getHeight();
		ascent = gc.getFontMetrics().getAscent();
		descent = gc.getFontMetrics().getDescent();
	}

	// Common lcdui.Font and nntdocomo.ui.Font methods

	public int stringWidth(String str) 
	{ 
		if(str == null) { throw new NullPointerException("Cannot get stringWidth from a null String"); }

		return metrics.stringWidth(str); 
	}

	public int substringWidth(String str, int offset, int len) 
	{
		if(str == null) { throw new NullPointerException("Cannot get substringWidth of a null String"); }
		if(offset < 0 || len < 0 || (offset+len) > str.length()) {throw new StringIndexOutOfBoundsException("substringWidth tried to access invalid index on received string");}

		return stringWidth(str.substring(offset, offset+len)); 
	}

	public int getFace() { return face; }

	public int getHeight() { return height; }

	public int getAscent() { return ascent; }

	public int getDescent() { return descent; }

	public int getSize() { return size; }

	public int getPointSize() { return convertSize(size); }

	public int getStyle() { return style; }

	// Internal methods for style and sizing
	public int convertDoJaToLCDUIStyle(int doJaStyle) 
	{
		switch(doJaStyle) 
		{
			case com.nttdocomo.ui.Font.STYLE_BOLD: return Font.STYLE_BOLD;
			case com.nttdocomo.ui.Font.STYLE_BOLDITALIC: return Font.STYLE_BOLD | Font.STYLE_ITALIC;
			case com.nttdocomo.ui.Font.STYLE_ITALIC: return Font.STYLE_ITALIC; 
			case com.nttdocomo.ui.Font.STYLE_PLAIN:
			default: return doJaStyle;
		}
	}

	public int convertDoJaToLCDUIFace(int doJaFace) 
	{
		switch(doJaFace) 
		{
			case com.nttdocomo.ui.Font.FACE_MONOSPACE: return Font.FACE_MONOSPACE;
			case com.nttdocomo.ui.Font.FACE_PROPORTIONAL: return Font.FACE_PROPORTIONAL;
			case com.nttdocomo.ui.Font.FACE_SYSTEM: return Font.FACE_SYSTEM; 
			default: return doJaFace;
		}
	}

	public int convertDoJaToLCDUISize(int doJaSize) 
	{
		switch(doJaSize) 
		{
			case com.nttdocomo.ui.Font.SIZE_LARGE: return Font.SIZE_LARGE;
			case com.nttdocomo.ui.Font.SIZE_MEDIUM: return Font.SIZE_MEDIUM;
			case com.nttdocomo.ui.Font.SIZE_SMALL: return Font.SIZE_SMALL; 
			default: return doJaSize;
		}
	}

	public static void setScreenSize(int width, int height)
	{
		final int minSize = Math.min(width, height);
		if (minSize < 128)      { screenType = 0; }
		else if (minSize < 176) { screenType = 1; }
		else if (minSize < 220) { screenType = 2; }
		else                    { screenType = 3; }

		defaultFont = new Font(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
		defaultDoJaFont = new com.nttdocomo.ui.Font(com.nttdocomo.ui.Font.FACE_SYSTEM, com.nttdocomo.ui.Font.STYLE_PLAIN, com.nttdocomo.ui.Font.SIZE_MEDIUM);   
	}

	public static void updateDefaultFont() 
	{
		defaultFont = new Font(defaultFont.face, defaultFont.style, defaultFont.size);
		defaultDoJaFont = new com.nttdocomo.ui.Font(defaultDoJaFont.face, defaultDoJaFont.style, defaultDoJaFont.size);
	}

	private int convertSize(int size)
	{
		if(!isLCDUI) { size = convertDoJaToLCDUISize(size);}
		switch(size)
		{
			case Font.SIZE_LARGE                  : return fontSizes[4*screenType + 3]+Mobile.fontSizeOffset;
			case Font.SIZE_MEDIUM                 : return fontSizes[4*screenType + 2]+Mobile.fontSizeOffset;
			case Font.SIZE_SMALL                  : return fontSizes[4*screenType + 1]+Mobile.fontSizeOffset;
			case com.nttdocomo.ui.Font.SIZE_TINY  : return fontSizes[4*screenType]+Mobile.fontSizeOffset;
			default          : return fontSizes[4*screenType + 1]+Mobile.fontSizeOffset; // Default to medium
		}
	}
}
