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

import java.util.List;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.MobilePlatform;

public class Alert extends Screen
{

	public static final Command DISMISS_COMMAND = new Command("OK", Command.OK, 0);

	public static final int FOREVER = -2;


	private String message;

	private Image image;

	private List<String> lines;
	private int lineSpacing;
	private int margin;
	private int scrollbarWidth;
	private int scrollY = 0;
	private int scrollHeight = 0;
	private int clientHeight;
	private boolean needsLayout = true;

	private AlertType type;

	private int timeout = FOREVER;

	private Gauge indicator;

	private Displayable nextScreen = null;


	public Alert(String title) 
	{
		setTitle(title);
		setTimeout(getDefaultTimeout());
		setType(new AlertType());

		addCommand(Alert.DISMISS_COMMAND);

		setCommandListener(defaultListener);

		lineSpacing = 1;
		scrollbarWidth = 4;
		margin = Font.getDefaultFont().getHeight() / 4;
	}

	public Alert(String title, String alertText, Image alertImage, AlertType alertType)
	{
		setTitle(title);
		setString(alertText);
		setImage(alertImage);
		setType(alertType);

		setTimeout(getDefaultTimeout());

		addCommand(Alert.DISMISS_COMMAND);

		setCommandListener(defaultListener);

		lineSpacing = 1;
		scrollbarWidth = 4;
		margin = Font.getDefaultFont().getHeight() / 4;
	}

	public int getDefaultTimeout() { return Alert.FOREVER; }

	public int getTimeout() { return timeout; }

	public void setTimeout(int time) { timeout = time; }

	public AlertType getType() { return type; }

	public void setType(AlertType t) { type = t; }

	public String getString() { return message; }

	public void setString(String text)
	{
		Mobile.log(Mobile.LOG_WARNING, Alert.class.getPackage().getName() + "." + Alert.class.getSimpleName() + ": " + text);
		message = text;
		needsLayout = true;
	}

	public Image getImage() { return image; }

	public void setImage(Image img) { image = img; }

	public void setIndicator(Gauge gauge) { indicator = gauge; }

	public Gauge getIndicator() { return indicator; }

	public void addCommand(Command cmd)
	{
		super.addCommand(cmd);

		if (getCommands().size() == 2)
		{
			super.removeCommand(Alert.DISMISS_COMMAND);
		}

	}

	public void removeCommand(Command cmd)
	{
		if (getCommands().size() > 1)
		{
			super.removeCommand(cmd);
		}
	}

	public void setCommandListener(CommandListener listener)
	{
		if (listener == null)
		{
			listener = defaultListener;
		}
		super.setCommandListener(listener);
	}

	public CommandListener defaultListener = new CommandListener()
	{
		public void commandAction(Command cmd, Displayable next)
		{
			Mobile.getDisplay().setCurrent(nextScreen);
		}
	};

	public void setNextScreen(Displayable next) { nextScreen = next; }
	
	public String renderScreen(int x, int y, int width, int height) {
		clientHeight = height;

		if (message == null) {
			return null;
		}
		if (needsLayout) {
			lines = StringItem.wrapText(message, width - 2*margin - scrollbarWidth, Font.getDefaultFont());
			needsLayout = false;
			if (lines.isEmpty()) {
				return "";
			}

			scrollHeight = (lines.size()*Font.getDefaultFont().getHeight() + (lines.size()-1)*lineSpacing) + 2*margin;
			scrollY = 0;
		}

		if (lines.isEmpty()) {
			return "";
		}

		graphics.setColor(Mobile.lcduiTextColor);
		for(int l=0;l<lines.size();l++) {
			int ystart = margin + l*Font.getDefaultFont().getHeight() + (l > 0 ? (l-1)*lineSpacing : 0);
			int yend = ystart + Font.getDefaultFont().getHeight();

			if (yend < scrollY || ystart >= scrollY+height) {
				continue;
			}

			graphics.drawString(
				lines.get(l),
				x + margin,
				y + ystart - scrollY,
				Graphics.LEFT);
		}
		
		double fact = (double)height/scrollHeight;
		int yscrollStart = (int)Math.round(scrollY * fact);
		int yscrollHeight = (int)Math.min(height, Math.round(height * fact));
	
		if (height < scrollHeight)
		{
			graphics.setColor(Mobile.lcduiBGColor);
			graphics.fillRect(x + width - scrollbarWidth, y+yscrollStart, scrollbarWidth, yscrollHeight);
		}
		
		return null;
	}

	public boolean screenKeyPressed(int key) 
	{
		if (needsLayout || lines.isEmpty() || scrollHeight <= clientHeight) 
		{
			return false;
		}

		boolean handled = true;
		int scrollAmount = clientHeight/4;
		int maxScroll = scrollHeight - clientHeight;

		// TODO: Check if this is functional. Inputs should be abstracted here to work on all key layouts, Displayable is what does this
		if ((key == Canvas.UP || key == Canvas.KEY_NUM2) && scrollY > 0) 
		{
			scrollY = Math.max(0, scrollY - scrollAmount);
		} 
		else if ((key == Canvas.DOWN || key == Canvas.KEY_NUM8) && scrollY < maxScroll) 
		{
			scrollY = Math.min(maxScroll, scrollY + scrollAmount);
		}
		else if((key == Canvas.FIRE || key == Canvas.KEY_NUM5))
		{
			defaultListener.commandAction(DISMISS_COMMAND, nextScreen);
		}

		if (handled) { _invalidate(); }

		return handled;
	}
}
