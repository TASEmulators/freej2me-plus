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
package com.nttdocomo.ui;

public final class TextBox extends Component implements Interactable
{
    public static final int ALPHA = 1;
    public static final int DISPLAY_ANY = 0;
    public static final int DISPLAY_PASSWORD = 1;
    public static final int KANA = 2;
    public static final int NUMBER = 0;
    public static final int SIMPLIFIED_HANZI = 4;
    public static final int TRADITIONAL_HANZI = 3;

    private String text;
    private com.nttdocomo.lang.XString xText;
    private int columns;
    private int rows;
    private int mode;
    private boolean editable;
    private boolean enabled;

    private Font font = Font.getDefaultFont();

    public TextBox(String text, int columns, int rows, int mode)
    {
        if (columns < 0 || rows < 0) { throw new IllegalArgumentException("Columns and rows must be non-negative"); }
        if (mode < 0 || mode > 4) { throw new IllegalArgumentException("Illegal mode value"); }

        this.text = (text != null) ? text : "";
        this.columns = columns;
        this.rows = rows;
        this.mode = mode;
        this.editable = true;
        this.enabled = true;
    }

    public void setEnabled(boolean b) { this.enabled = b; }

    public void requestFocus() { }

    public void setText(String text)  { this.text = (text != null) ? text : ""; }

    public void setText(com.nttdocomo.lang.XString xText)  { this.xText = (xText != null) ? xText : null; }

    public String getText() { return text; }

    public com.nttdocomo.lang.XString getXText() { return xText; }

    public void setInputMode(int mode)
    {
        if (mode < 0 || mode > 4) { throw new IllegalArgumentException("Illegal mode value"); }

        this.mode = mode;
    }

    public void setFont(Font font) { this.font = font; }

    public void setEditable(boolean b) { this.editable = b; }
}
