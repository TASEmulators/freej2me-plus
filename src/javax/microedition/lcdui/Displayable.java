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

public abstract class Displayable
{

	public PlatformImage platformImage;
	public Graphics graphics = null;

	public int width = 0;

	public int height = 0;
	
	protected String title = "";

	public ArrayList<Command> commands = new ArrayList<Command>();

	protected ArrayList<Item> items = new ArrayList<Item>();

	protected CommandListener commandlistener;

	public boolean listCommands = false;
	
	public int currentCommand = 0;

	protected int currentItem = -1;

	public Ticker ticker;

	public Displayable()
	{
		// TODO: see if it's a breaking change and add a compat flag if it is?

		width = MobilePlatform.lcdWidth;
		height = MobilePlatform.lcdHeight;
		platformImage = MobilePlatform.getLcdBackbuffer();
		graphics = platformImage.getMIDPGraphics();
	}

	public void addCommand(Command cmd)
	{
		MobilePlatform.showCommandBar();
		if(commands.contains(cmd)) { return; }
		if(cmd == null) { throw new NullPointerException("Cannot insert a null command"); }
		synchronized(commands) { commands.add(cmd); }
		_invalidate();
	}

	public void removeCommand(Command cmd) 
	{
		MobilePlatform.showCommandBar();
		if(cmd == null) { return; }
		synchronized(commands) { commands.remove(cmd); }
		_invalidate(); 
	}
	
	public int getWidth() { return width; }

	public int getHeight() { return height; }
	
	public String getTitle() { return title; }

	public void setTitle(String text) { title = text; }        

	public boolean isShown() { return Mobile.getDisplay().getCurrent() == this; }

	public Ticker getTicker() { return ticker; }

	public void setTicker(Ticker tick) { ticker = tick; }
	
	public void setCommandListener(CommandListener listener) { commandlistener = listener; }

	protected void sizeChanged(int width, int height) { this.width = width; this.height = height; }

	public void doSizeChanged(int width, int height) { sizeChanged(width, height); }

	public Display getDisplay() { return Mobile.getDisplay(); }

	public ArrayList<Command> getCommands() { return commands; }


	public void keyPressed(int key) { }

	public boolean screenKeyPressed(int key) { return false; } // Ignore, classes like Form and List inherit this, and do their own thing with it.
	public void screenKeyReleased(int key) { }
	public void screenKeyRepeated(int key) { }
	
	public void keyReleased(int key) { }
	public void keyRepeated(int key) { }
	public void pointerDragged(int x, int y) { }
	public void pointerPressed(int x, int y) { }
	public void pointerReleased(int x, int y) { }

	public void notifySetCurrent() { _invalidate(); }

	protected void render()
	{
		if(!isShown()) { return; }

		// LCDUI should work independently of the current graphics translation, so translate back to 0,0 before any drawing and restore at the end
		int restoreX = graphics.getTranslateX(), restoreY = graphics.getTranslateY();
		graphics.translate(-restoreX, -restoreY);

		graphics.setFont(Font.getDefaultFont());

		// Draw Background:
		graphics.setColor(Mobile.lcduiBGColor);
		graphics.fillRect(0,0,width,height);
		graphics.setColor(Mobile.lcduiTextColor);

		String currentTitle = listCommands ? "Options" : title;

		int titlePadding = Font.fontPadding[Font.screenType];
		int titleHeight = Font.getDefaultFont().getHeight() + titlePadding;

		int xPadding = Font.getDefaultFont().getHeight()/5;

		int commandsBarHeight = titleHeight - titlePadding;

		int contentHeight = height - titleHeight - commandsBarHeight - 2;
		
		// Draw Title:
		graphics.drawString(currentTitle, width/2, 0, Graphics.HCENTER);
		graphics.drawLine(0, titleHeight, width, titleHeight);
		graphics.drawLine(0, height-commandsBarHeight, width, height-commandsBarHeight);

		int currentY = titleHeight;
		int textCenter;
		int xPos;

		if (listCommands) // Render Commands
		{
			if(commands.size()>0)
			{
				if(currentCommand<0) { currentCommand = 0; }
				// Draw commands //

				int listPadding = titlePadding;
				int itemHeight = titleHeight;

				int ah = contentHeight; // allowed height
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

			graphics.drawLine(width/2, height-commandsBarHeight, width/2, height);

			textCenter = (graphics.getGraphics2D().getFontMetrics().stringWidth("Okay"))/2;
			xPos = (width / 4) - textCenter;
			graphics.drawString("Okay", xPos, currentY+titlePadding, Graphics.LEFT);

			textCenter = (graphics.getGraphics2D().getFontMetrics().stringWidth("Back"))/2;
			xPos = (3 * width / 4) - textCenter;
			graphics.drawString("Back", xPos, currentY+titlePadding, Graphics.LEFT);
		}
		else // Render Items
		{
			graphics.setClip(0, currentY+titlePadding, width, contentHeight);
			String status = renderScreen(0, currentY+titlePadding, width, contentHeight);

			currentY += contentHeight;

			graphics.setClip(0, 0, graphics.getCanvas().getWidth(), graphics.getCanvas().getHeight());
			graphics.setFont(Font.getDefaultFont());

			Command itemCommand = null;
			if (this instanceof Form) { itemCommand = ((Form)this).getItemCommand(); }

			graphics.setColor(Mobile.lcduiTextColor);
			switch(commands.size())
			{
				case 0: break;
				case 1:
					// Draw a center line on the lower bar, we'll only have two objects there
					graphics.drawLine(width/2, height-commandsBarHeight, width/2, height);

					textCenter = (graphics.getGraphics2D().getFontMetrics().stringWidth(commands.get(0).getLabel()))/2;
					xPos = (width / 4) - textCenter;
					graphics.drawString(commands.get(0).getLabel(), xPos, height-commandsBarHeight+titlePadding, Graphics.LEFT);
					if (status != null)
					{
						textCenter = (graphics.getGraphics2D().getFontMetrics().stringWidth(status))/2;
						xPos = (3* width / 4) - textCenter;
						graphics.drawString(status, xPos, height-commandsBarHeight+titlePadding, Graphics.LEFT);
					}
					
					break;
				case 2:
					
					graphics.drawLine(3 * width / 4, height-commandsBarHeight+titlePadding, 4 * width / 6, height);

					graphics.drawLine(width/4, height-commandsBarHeight+titlePadding, width/3, height);

					graphics.drawString(commands.get(0).getLabel(), xPadding, height-commandsBarHeight+titlePadding, Graphics.LEFT);
					graphics.drawString(commands.get(1).getLabel(), width-xPadding, height-commandsBarHeight+titlePadding, Graphics.RIGHT);

					if (status != null && itemCommand == null)
					{
						graphics.drawString(status, width/2, height-commandsBarHeight+titlePadding, Graphics.HCENTER);
					}
					break;
				default:
					graphics.drawString("Options", xPadding, height-commandsBarHeight+titlePadding, Graphics.LEFT);
			}

			if (itemCommand != null) 
			{
				graphics.drawString(itemCommand.getLabel(), width/2, height-commandsBarHeight+titlePadding, Graphics.HCENTER);
			}
		}

		graphics.translate(restoreX, restoreY);
	
		Mobile.getPlatform().flushGraphics(platformImage, 0, 0, width, height);
	}

	protected String renderScreen(int x, int y, int width, int height) { return null; } // Also inherited by Form, List, etc.

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

	public void doLeftCommand()
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

	public void doRightCommand()
	{
		if(commands.size()>1 && commands.size()<=2)
		{
			doCommand(1);
		}
	}

	public void _invalidate() 
	{
		if (!isShown()) { return; }

		Mobile.getDisplay().postPaintRequest(new Runnable()
		{
			@Override
			public void run() { render(); }
		}); 
	}

}
