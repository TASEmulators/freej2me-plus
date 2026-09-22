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
package com.xce.lcdui;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.Graphics;

import org.recompile.mobile.Mobile;

public class XTextField
{
    private int maxSize;
    private boolean focus;
    private int cursorPos = 0;

    private int x, y, width, height;

    private TextField textField;
    private Canvas canvas;
    private Graphics g;

    public XTextField(String text, int maxSize, int constraints, Canvas canvas)
    {
        this.canvas = canvas;

        this.textField = new TextField("",text,maxSize,constraints);
    }

    public void setText(String s) { textField.setString(s); }

    public String getText() { return textField.getString(); }

    public void keyPressed(int keyCode)
    {
        textField.externalKeyPressed(canvas.SKTToMIDPKey(keyCode));
        repaint();
    }

    public void keyReleased(int keyCode)
    {
    }

    public void keyRepeated(int keyCode)
    {
        textField.externalKeyPressed(canvas.SKTToMIDPKey(keyCode));
        repaint();
    }

    public void paint(Graphics g)
    {
        textField.externalRenderItem(g, x, y, width, height, this.focus);
    }

    public void repaint() { canvas.repaint(); }

    public void setMaxSize(int maxSize)
    {
        this.maxSize = maxSize;

        textField.setMaxSize(maxSize);
    }

    public int getMaxSize() { return this.maxSize; }

    public void setBounds(int x, int y, int width, int height)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean hasFocus()
    {
        Mobile.log(Mobile.LOG_WARNING, XTextField.class.getPackage().getName() + "." + XTextField.class.getSimpleName() + ": " + "hasFocus:" + this.focus);
        return this.focus;
    }

    public void setFocus(boolean focus)
    {
        Mobile.log(Mobile.LOG_WARNING, XTextField.class.getPackage().getName() + "." + XTextField.class.getSimpleName() + ": " + "setFocus:" + focus);
        this.focus = focus;
    }

    public void inputChar(char key)
    {
        Mobile.log(Mobile.LOG_WARNING, XTextField.class.getPackage().getName() + "." + XTextField.class.getSimpleName() + ": " + "inputChar " + key);
        textField.insert(new String(new char[]{key}), textField.getCaretPosition());
    }
}
