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
import org.recompile.mobile.MobilePlatform;
import org.recompile.mobile.PlatformImage;
import org.recompile.mobile.PlatformGraphics;

public abstract class Displayable
{

	public PlatformImage platformImage;
	public PlatformGraphics graphics = null;

	public int width = 0;

	public int height = 0;
	
	protected String title = "";

	protected ArrayList<Command> commands = new ArrayList<Command>();

	protected ArrayList<Item> items = new ArrayList<Item>();

	protected CommandListener commandlistener;

	protected boolean listCommands = false;
	
	protected int currentCommand = 0;

	protected int currentItem = -1;

	public Ticker ticker;

	private volatile boolean isValidating = false;

	public Displayable()
	{
		width = Mobile.getPlatform().lcdWidth;
		height = Mobile.getPlatform().lcdHeight;
		platformImage = new PlatformImage(width, height);
		graphics = platformImage.getGraphics();
	}

	public void addCommand(Command cmd)
	{ 
		commands.add(cmd);
		_invalidate();
	}

	public void removeCommand(Command cmd) 
	{ 
		commands.remove(cmd);
		_invalidate(); 
	}
	
	public int getWidth() { return width; }

	public int getHeight() { return height; }
	
	public String getTitle() { return title; }

	public void setTitle(String text) { title = text; }        

	public boolean isShown() { return true; }

	public Ticker getTicker() { return ticker; }

	public void setTicker(Ticker tick) { ticker = tick; }
	
	public void setCommandListener(CommandListener listener) { commandlistener = listener; }

	protected void sizeChanged(int width, int height) { this.width = width; this.height = height; }

	public void doSizeChanged(int width, int height) { sizeChanged(width, height); }

	public Display getDisplay() { return Mobile.getDisplay(); }

	public ArrayList<Command> getCommands() { return commands; }


	public void keyPressed(int key) 
	{ 
		if (listCommands) { keyPressedCommands(Mobile.getGameAction(key)); } 
		else 
		{
			boolean handled = screenKeyPressed(Mobile.getGameAction(key));
			if (!handled)
			{
				if (Mobile.getGameAction(key) == Canvas.KEY_SOFT_LEFT || Mobile.getGameAction(key) == Canvas.FIRE 
				|| Mobile.getGameAction(key) == Canvas.KEY_NUM5) 
				{
					doLeftCommand();
				} 
				else if (Mobile.getGameAction(key) == Canvas.KEY_SOFT_RIGHT) 
				{
					doRightCommand();
				}
			}
		}
	}

	public boolean screenKeyPressed(int key) { return false; } // Ignore, classes like Form and List inherit this, and do their own thing with it.
	public void screenKeyReleased(int key) { }
	public void screenKeyRepeated(int key) { }
	
	public void keyReleased(int key) { }
	public void keyRepeated(int key) { }
	public void pointerDragged(int x, int y) { }
	public void pointerPressed(int x, int y) { }
	public void pointerReleased(int x, int y) { }
	public void showNotify() { }
	public void hideNotify() { }

	public void notifySetCurrent() { render(); }

	protected void render()
	{
		graphics.setFont(Font.getDefaultFont());

		// Draw Background:
		graphics.setColor(Mobile.lcduiBGColor);
		graphics.fillRect(0,0,width,height);
		graphics.setColor(Mobile.lcduiTextColor);

		String currentTitle = listCommands ? "Options" : title;

		int titlePadding = Font.getDefaultFont().getHeight() / 10;
		int titleHeight = Font.getDefaultFont().getHeight() + 2*titlePadding;

		int xPadding = Font.getDefaultFont().getHeight()/5;

		int commandsBarHeight = titleHeight - 1;

		int contentHeight = height - titleHeight - commandsBarHeight - 2; // 1px for line
		
		// Draw Title:
		graphics.drawString(currentTitle, width/2, titlePadding, Graphics.HCENTER);
		graphics.drawLine(0, titleHeight, width, titleHeight);
		graphics.drawLine(0, height-commandsBarHeight-1, width, height-commandsBarHeight-1);

		int currentY = titleHeight + 1;
		int textCenter;
		int xPos;

		if (listCommands) // Render Commands
		{
			if(commands.size()>0)
			{
				if(currentCommand<0) { currentCommand = 0; }
				// Draw commands //

				int listPadding = Font.getDefaultFont().getHeight()/5;
				int itemHeight = Font.getDefaultFont().getHeight();

				int ah = contentHeight - 2*listPadding; // allowed height
				int max = (int)Math.floor(ah / itemHeight); // max items per page			
				if(commands.size()<max) { max = commands.size(); }

				int page = 0;
				page = (int)Math.floor(currentCommand/max); // current page
				int first = page * max; // first item to show
				int last = first + max - 1;

				if(last>=commands.size()) { last = commands.size()-1; }
				
				int y = currentY + listPadding;
				for(int i=first; i<=last; i++)
				{	
					if(currentCommand == i)
					{
						graphics.fillRect(0,y,width,itemHeight);
						graphics.setColor(Mobile.lcduiBGColor);
					}
					
					graphics.drawString(commands.get(i).getLabel(), width/2, y, Graphics.HCENTER);
					graphics.setColor(Mobile.lcduiTextColor);

					y += itemHeight;
				}
			}

			currentY += contentHeight;

			graphics.setColor(Mobile.lcduiTextColor);

			graphics.drawLine(width/2, height-commandsBarHeight-1, width/2, height);

			textCenter = (graphics.getGraphics2D().getFontMetrics().stringWidth("Okay"))/2;
			xPos = (width / 4) - textCenter;
			graphics.drawString("Okay", xPos, currentY+titlePadding, Graphics.LEFT);

			textCenter = (graphics.getGraphics2D().getFontMetrics().stringWidth("Back"))/2;
			xPos = (3 * width / 4) - textCenter;
			graphics.drawString("Back", xPos, currentY+titlePadding, Graphics.LEFT);
		}
		else // Render Items
		{
			graphics.setClip(0, currentY, width, contentHeight);
			String status = renderScreen(0, currentY, width, contentHeight);

			currentY += contentHeight;

			graphics.reset();
			graphics.setFont(Font.getDefaultFont());

			Command itemCommand = null;
			if (this instanceof Form) { itemCommand = ((Form)this).getItemCommand(); }

			graphics.setColor(Mobile.lcduiTextColor);
			switch(commands.size())
			{
				case 0: break;
				case 1:
					// Draw a center line on the lower bar, we'll only have two objects there
					graphics.drawLine(width/2, height-commandsBarHeight-1, width/2, height);

					textCenter = (graphics.getGraphics2D().getFontMetrics().stringWidth(commands.get(0).getLabel()))/2;
					xPos = (width / 4) - textCenter;
					graphics.drawString(commands.get(0).getLabel(), xPos, currentY+titlePadding, Graphics.LEFT);
					if (status != null)
					{
						textCenter = (graphics.getGraphics2D().getFontMetrics().stringWidth(status))/2;
						xPos = (3* width / 4) - textCenter;
						graphics.drawString(status, xPos, currentY+titlePadding, Graphics.LEFT);
					}
					
					break;
				case 2:
					
					graphics.drawLine(3 * width / 4, height-commandsBarHeight-1, 4 * width / 6, height);

					graphics.drawLine(width/4, height-commandsBarHeight-1, width/3, height);

					graphics.drawString(commands.get(0).getLabel(), xPadding, currentY+titlePadding, Graphics.LEFT);
					graphics.drawString(commands.get(1).getLabel(), width-xPadding, currentY+titlePadding, Graphics.RIGHT);

					if (status != null && itemCommand == null)
					{
						graphics.drawString(status, width/2, currentY+titlePadding, Graphics.HCENTER);
					}
					break;
				default:
					graphics.drawString("Options", xPadding, currentY+titlePadding, Graphics.LEFT);
			}

			if (itemCommand != null) 
			{
				graphics.drawString(itemCommand.getLabel(), width/2, currentY+titlePadding, Graphics.HCENTER);
			}
		}
	
		if(this.getDisplay().getCurrent() == this)
		{
			Mobile.getPlatform().flushGraphics(platformImage, 0, 0, width, height);
		}
	}

	protected String renderScreen(int x, int y, int width, int height) { return null; } // Also inherited by Form, List, etc.

	protected void keyPressedCommands(int key)
	{
		if(key == Canvas.KEY_NUM2 || key == Canvas.UP) 
		{
			currentCommand--;
			if(currentCommand<0) { currentCommand = commands.size()-1; }
		}
		else if(key == Canvas.KEY_NUM8 || key == Canvas.DOWN) 
		{
			currentCommand++;
			if(currentCommand>=commands.size()) { currentCommand = 0; }
		}
		else if (key == Canvas.KEY_NUM5 || key == Canvas.FIRE || key == Canvas.KEY_SOFT_LEFT) 
		{
			listCommands = false;
			doLeftCommand();
			currentCommand = 0;
		}
		else if (key == Canvas.KEY_SOFT_RIGHT) 
		{
			listCommands = false;
			doRightCommand();
			currentCommand = 0;
		}
		else { return; }

		_invalidate(); 
	}

	protected void doCommand(int index)
	{
		if(index>=0 && commands.size()>index)
		{
			if(commandlistener!=null)
			{
                commandlistener.commandAction(commands.get(index), this);
			}
		}
	}

	protected void doLeftCommand()
	{
		if(commands.size()>2 && !listCommands)
		{
			listCommands = true;
			_invalidate();
		}
		else if(commands.size()>2 && listCommands) 
		{
			doCommand(currentCommand);
			listCommands = false;
		}
		else
		{
			if(commands.size()>0 && commands.size()<=2)
			{
				doCommand(0);
			}
		}
	}

	protected void doRightCommand()
	{
		if(commands.size()>0 && commands.size()<=2)
		{
			doCommand(1);
		}
	}

	protected void _invalidate() 
	{
		// zb3: TODO: consider queuing this
		// the code below ensures this function is not reentrant
		synchronized (Display.LCDUILock)
		{
			if (getDisplay().getCurrent() != this) { return; }

			if (isValidating) 
			{
				Mobile.log(Mobile.LOG_ERROR, Displayable.class.getPackage().getName() + "." + Displayable.class.getSimpleName() + ": " + "Recursive invalidation attempt detected.");
				Thread.dumpStack();
			} 
			else 
			{
				isValidating = true;

				try { render(); } 
				finally { isValidating = false; }
			}
		}
	}
}
