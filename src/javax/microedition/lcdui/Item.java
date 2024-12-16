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

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformGraphics;



public abstract class Item
{

	public static final int BUTTON = 2;
	public static final int HYPERLINK = 1;

	public static final int LAYOUT_DEFAULT = 0;

	public static final int LAYOUT_LEFT = 1;
	public static final int LAYOUT_RIGHT = 2;
	public static final int LAYOUT_CENTER = 3;

	public static final int LAYOUT_TOP = 0x10;
	public static final int LAYOUT_BOTTOM  = 0x20;
	public static final int LAYOUT_VCENTER = 0x30;

	public static final int LAYOUT_NEWLINE_BEFORE = 0x100;
	public static final int LAYOUT_NEWLINE_AFTER = 0x200;

	public static final int LAYOUT_SHRINK = 0x400;
	public static final int LAYOUT_VSHRINK = 0x1000;
	public static final int LAYOUT_EXPAND = 0x800;
	public static final int LAYOUT_VEXPAND = 0x2000;

	public static final int LAYOUT_2 = 0x4000;

	public static final int PLAIN = 0;


	protected Form owner;

	private String label;

	private ArrayList<Command> commands = new ArrayList<>();

	private int layout;

	private Command defaultCommand;

	private ItemCommandListener commandListener;

	private int prefWidth = 64;

	private int prefHeight = 8;

	public Item() { }

	public void addCommand(Command cmd) { commands.add(cmd); }

	public String getLabel() { return label; }

	public int getLayout() { return layout; }

	public int getMinimumHeight() { return 8; }

	public int getMinimumWidth() { return 64; }

	public int getPreferredHeight() { return prefHeight; }

	public int getPreferredWidth() { return prefWidth; }

	public void notifyStateChanged() 
	{ 
		Form owner = getOwner();
		if (owner != null) { owner.itemStateChanged(this); }
	}

	public void removeCommand(Command cmd) { if (cmd == defaultCommand) defaultCommand=null; }

	public void setDefaultCommand(Command cmd) { defaultCommand = cmd; }

	public void setItemCommandListener(ItemCommandListener listener) { commandListener = listener; }

	public ItemCommandListener getItemCommandListener() { return commandListener; }

	public void setLabel(String text) 
	{ 
		label = text;
		invalidate();
	}

	public void setLayout(int value) { layout = value; }

	public void setPreferredSize(int width, int height)
	{
		prefWidth = width;
		prefHeight = height;
	}

	protected void setOwner(Form newOwner) { owner = newOwner; }
	
	protected Form getOwner() { return owner; }

	protected boolean hasLabel() { return label != null && !label.isEmpty(); }

	protected int getContentHeight(int width) { return Font.getDefaultFont().getHeight(); }

	protected int getLabelHeight(int width) 
	{
		if (!hasLabel()) { return 0; }

		// for now we assume one line + bottom padding
		return Font.getDefaultFont().getHeight() + Font.getDefaultFont().getHeight() / 5;
	}

	protected void renderItem(PlatformGraphics graphics, int x, int y, int width, int height) { }

	protected void invalidate() 
	{
		Form owner = getOwner();
		if (owner != null) 
		{
			owner.needsLayout = true;
			owner._invalidate();
		}
	}

	protected void _invalidateContents() 
	{
		Form owner = getOwner();
		if (owner != null) { owner._invalidate(); }
	}

	protected Command _getItemCommand() { return defaultCommand; }

	protected boolean keyPressed(int key) { return false; }

	protected void renderItemLabel(PlatformGraphics graphics, int x, int y, int itemContentWidth) 
	{
		Font oldFont = graphics.getFont();
		graphics.setFont(Font.getDefaultFont());
		graphics.drawString(getLabel(), x, y, 0);
		graphics.setFont(oldFont);
	}

	protected boolean traverse(int dir, int viewportWidth, int viewportHeight, int[] visRect_inout) { return false; }

	protected void traverseOut() { }

	protected int _drawArrow(PlatformGraphics graphics, int dir, boolean active, int x, int y, int width, int height) 
	{
		// zb3: these parameters are for the field, not for the arrow

		int arrowWidth = height/2;
		int arrowMargin = height/15;
		int arrowPadding = height/2;
		int arrowHeight = height/2;

		graphics.setColor(active ? Mobile.lcduiTextColor : Mobile.lcduiBGColor);

		if (dir == -1) 
		{
			graphics.fillPolygon(
				new int[]{x+arrowMargin, x+arrowMargin+arrowWidth, x+arrowMargin+arrowWidth}, 0,
				new int[]{y+height/2, y+height/2-arrowHeight/2, y+height/2+arrowHeight/2}, 0, 3, Mobile.lcduiTextColor
			);
		} 
		else if (dir == 1) 
		{
			graphics.fillPolygon(
				new int[]{x+width-arrowWidth-arrowMargin-1, x+width-arrowMargin-1, x+width-arrowWidth-arrowMargin-1}, 0,
				new int[]{y+height/2-arrowHeight/2, y+height/2, y+height/2+arrowHeight/2}, 0, 3, Mobile.lcduiTextColor
			);
		}

		return arrowWidth+arrowMargin+arrowPadding;
	}

}
