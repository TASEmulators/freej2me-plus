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

	// Array of 2 commands that forces select types on the left soft key, and cancel on the right.
	private ArrayList<Command> swapped = new ArrayList<Command>(2);

	protected ArrayList<Item> items = new ArrayList<Item>();

	protected CommandListener commandlistener;

	public boolean listCommands = false;

	public int currentCommand = 0;

	protected int currentItem = -1;

	public Ticker ticker;

	public Displayable()
	{
		width = MobilePlatform.lcdWidth;
		height = MobilePlatform.lcdHeight;
		platformImage = MobilePlatform.getLcdBackbuffer();
		graphics = platformImage.getMIDPGraphics();
	}

	public void addCommand(Command cmd)
	{
		MobilePlatform.showCommandBar();

		if(cmd == null) { throw new NullPointerException("Cannot insert a null command"); }
		if(commands.contains(cmd)) { return; }
		synchronized(commands) { commands.add(cmd); }
		_invalidate();
	}

	public void removeCommand(Command cmd)
	{
		MobilePlatform.showCommandBar();
		if(cmd == null || !commands.contains(cmd)) { return; }
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

		// Draw Background:
		LCDUIRenderer.fillBackground(graphics, width, height);

		String currentTitle = listCommands ? "Options" : title;
		int titlePadding = Font.fontPadding[Font.screenType];
		int titleHeight = Font.getDefaultFont().getHeight() - 1;
		int commandsBarHeight = LCDUIRenderer.commandFont.getHeight() - 2;

		// Ticker reserves some space of its own, drawn right above the command bar.
		Ticker ticker = getTicker();
		int tickerHeight = 0;
		if(ticker != null)
		{
			ticker.advanceOffset();
			tickerHeight = LCDUIRenderer.commandFont.getHeight();
		}
		int contentHeight = height - titleHeight - commandsBarHeight - 1;
		int currentY = titleHeight;

		// Draw Title:
		LCDUIRenderer.drawTitleBar(graphics, width, title, titleHeight, Mobile.lcduiBGColor);

		Command itemCommand = (this instanceof Form) ? ((Form)this).getItemCommand() : null;

		// If we aren't listing commands, set the clip region to be between the
		// title bar and the commands bar, as we'll render the items right now.
		graphics.setClip(0, currentY + titlePadding, width, contentHeight);
		String status = listCommands ? null : renderScreen(0, currentY+titlePadding, width, contentHeight);
		graphics.setClip(0, 0, graphics.getCanvas().getWidth(), graphics.getCanvas().getHeight());

		// Draw the ticker
		if (ticker != null)
		{
			int tickerY = height - commandsBarHeight - tickerHeight+2;
			LCDUIRenderer.drawTicker(graphics, width, ticker.getString(), tickerHeight, tickerY, ticker.getScrollOffset(), Mobile.lcduiBGColor);
		}
		// Then draw te command bar
		boolean isThreeItems = (!listCommands && commands.size() == 2);

		// Standardize OK/SELECT/etc commands on the left soft key, and
		// CANCEL/EXIT/STOP/etc ones on the right soft key whenever we have two
		// commands.
		ArrayList<Command> displayCommands = commands;
		if (!listCommands && commands.size() == 2)
		{
			Command c0 = commands.get(0);
			Command c1 = commands.get(1);
			if (isBackCommand(c0) && !isBackCommand(c1))
			{
				swapped.clear();
				swapped.add(c1);
				swapped.add(c0);
				displayCommands = swapped;
			}
		}

		LCDUIRenderer.drawCommandBar(graphics, status, !listCommands ? displayCommands : null,
			itemCommand, width, height, commandsBarHeight, 1.0f,
		Mobile.lcduiBGColor, isThreeItems);

		if (listCommands) // Render Commands
		{
			if(!commands.isEmpty())
			{
				if(currentCommand < 0) { currentCommand = 0; }

				int itemHeight = titleHeight;
				int max = Math.min(commands.size(), (int)Math.floor(contentHeight / itemHeight));
				int page = (int)Math.floor(currentCommand / max);
				int first = page * max;
				int last = Math.min(first + max - 1, commands.size() - 1);

				boolean hasUp = (first > 0);
				boolean hasDown = (last < commands.size() - 1);

				int vGap = 2; // Add a small space between items
				// Apply a small vertical offset to avoid overlapping onto the
				// scroll indicator arrows. 3 pixels looks good here.
				int y = currentY + titlePadding + 3;
				for(int i = first; i <= last; i++)
				{
					if(commands.get(i).getCommandType() == Command.BACK)
					{
						// If the user navigated onto the hidden back command, move it
						// forward or wrap safely if already at the end of the command
						// list so it doesn't get stuck on an invisible slot.
						if(currentCommand == i)
						{
							currentCommand = (i + 1 < commands.size()) ? i + 1 : i - 1;
							if(currentCommand < 0) { currentCommand = 0; }
						}
						continue;
					}
					boolean isSelected = (currentCommand == i);
					LCDUIRenderer.drawItem(graphics, i, commands.get(i).getLabel(), null, 0, y, width, itemHeight - vGap, isSelected, false, Choice.IMPLICIT, false, Item.LAYOUT_CENTER);
					y += itemHeight;
				}

				// Down Indicator (if there are items hidden above)
				if (hasUp)
				{
					LCDUIRenderer.drawScrollIndicator(graphics, width / 2 - 2, currentY + 1, true);
				}

				// Down Indicator (if there are items hidden below)
				if (hasDown)
				{
					LCDUIRenderer.drawScrollIndicator(graphics, width / 2 - 2, currentY + contentHeight + 1, false);
				}
			}

			currentY += contentHeight;
			graphics.setColor(Mobile.lcduiTextColor);

			int startY = height - commandsBarHeight;

			int textCenter = (graphics.getGraphics2D().getFontMetrics().stringWidth("Okay"))/2;
			graphics.drawString("Okay", (width / 4) - textCenter, startY, 0);

			if(hasBackCommand())
			{
				textCenter = (graphics.getGraphics2D().getFontMetrics().stringWidth("Back"))/2;
				graphics.drawString("Back", (3 * width / 4) - textCenter, startY, 0);
			}
		}

		graphics.translate(restoreX, restoreY);
		Mobile.getPlatform().flushGraphics(platformImage, 0, 0, width, height);

		// Re-render this screen at set intervals, with the lower limit being
		// dictated by the FPS limit.
		Mobile.getDisplay().postPaintRequest(new Runnable()
		{
			@Override
			public void run() { render(); }
		});
	}

	protected String renderScreen(int x, int y, int width, int height) { return null; } // Also inherited by Form, List, etc.

	protected void doCommand(int index)
	{
		if(index>=0 && commands.size()>index)
		{
			if(commandlistener!=null)
			{
				commandlistener.commandAction(commands.get(index), this);
				_invalidate();
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
		else if(commands.size()>0 && commands.size()<=2)
		{
			doCommand(getLeftCommandIndex());
			currentCommand = 0;
		}
	}

	public void doRightCommand()
	{
		if(commands.size()>1 && commands.size()<=2)
		{
			doCommand(getRightCommandIndex());
			currentCommand = 0;
		}
		else if(commands.size() > 2)
		{
			for(int i = 0; i < commands.size(); i++)
			{
				if(isBackCommand(commands.get(i))) // Find the back command
				{
					doCommand(i);
					currentCommand = 0;
					return;
				}
			}
		}
	}

	public boolean hasBackCommand()
	{
		for(int i = 0; i < commands.size(); i++)
		{
			if(isBackCommand(commands.get(i))) { return true; }
		}

		return false;
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

	private boolean isBackCommand(Command cmd)
	{
		if (cmd == null) { return false; }
		int type = cmd.getCommandType();
		return type == Command.BACK || type == Command.CANCEL || type == Command.STOP || type == Command.EXIT;
	}

	private int getLeftCommandIndex()
	{
		if (commands.size() == 2)
		{
			Command c0 = commands.get(0);
			Command c1 = commands.get(1);
			if (isBackCommand(c0) && !isBackCommand(c1)) {
				return 1; // c1 is the primary command, place/execute on left
			}
		}
		return 0;
	}

	private int getRightCommandIndex()
	{
		if (commands.size() == 2)
		{
			Command c0 = commands.get(0);
			Command c1 = commands.get(1);
			if (isBackCommand(c0) && !isBackCommand(c1)) {
				return 0; // c0 is the back command, place/execute on right
			}
		}
		return 1;
	}
}
