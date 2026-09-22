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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.recompile.mobile.Mobile;

public class DateField extends Item
{

	public static final int DATE = 1;
	public static final int DATE_TIME = 3;
	public static final int TIME = 2;

	private Date dateValue;
	private int inputMode;
	private TimeZone timeZone;

	private String text = "1970/01/01-00:00:00:000";
	SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss:SSS");
	private int caretPosition = 0;
	private int padding;
	private int margin;
	private boolean highlighted;

	public DateField(String label, int mode) { this(label, mode, null); }

	public DateField(String label, int mode, TimeZone timeZone)
	{
		setLabel(label);
		setInputMode(mode);
		this.timeZone = (timeZone != null) ? timeZone : TimeZone.getDefault();
		text = formatDate();

		// these can't be static because of Font.getDefaultFont().getHeight()
		padding = Font.getDefaultFont().getHeight() / 3;
		margin = Font.getDefaultFont().getHeight() / 5;
	}

	public Date getDate()
	{
		if(dateValue == null) { return dateValue; }

		text = formatDate();
		Calendar calendar = Calendar.getInstance(timeZone);
		calendar.setTime(dateValue);

		if (inputMode == TIME)
		{
			// We only need the time data, so set date to zero epoch
			calendar.set(Calendar.YEAR, 1970);
			calendar.set(Calendar.MONTH, Calendar.JANUARY);
			calendar.set(Calendar.DAY_OF_MONTH, 1);
		}
		else if (inputMode == DATE)
		{
			// We only need date data, so set time to zero
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
		}
		// Else, return both date and time

		return calendar.getTime();
	}

	public int getInputMode() { return inputMode; }

	public void setDate(Date date)
	{
		if (date == null) { dateValue = date; return; }

		Calendar calendar = Calendar.getInstance(timeZone);
		calendar.setTime(date);

		if (inputMode == TIME)
		{
			if (calendar.get(Calendar.YEAR) != 1970 || calendar.get(Calendar.MONTH) != Calendar.JANUARY ||
				calendar.get(Calendar.DAY_OF_MONTH) != 1) { dateValue = null; }
			else { dateValue = date; }
		}
		else if (inputMode == DATE)
		{
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			dateValue = calendar.getTime();
		}
		else if (inputMode == DATE_TIME) { dateValue = date; }

		text = formatDate();
	}

	public void setInputMode(int mode)
	{
		if (mode != DATE && mode != TIME && mode != DATE_TIME) { throw new IllegalArgumentException("Invalid input mode"); }
		inputMode = mode;
	}


	// LCDUI rendering and navigation
	protected int getContentHeight(int width) { return Font.getDefaultFont().getHeight() + padding*2 + 2*margin; /* padding */ }

	protected boolean keyPressed(int key)
	{
		boolean handled = true, changed = true;

		if (key == Canvas.DOWN) // Cycle down through the current numeric value set
		{
			int curVal = Integer.parseInt("" + text.charAt(caretPosition)) - 1;
			curVal = checkRange(curVal);
			text = text.substring(0, caretPosition) + Character.forDigit(curVal, 10) + text.substring(caretPosition + 1);
		}
		else if (key == Canvas.UP) // Cycle up through the current numeric value set
		{
			int curVal = Integer.parseInt("" + text.charAt(caretPosition)) + 1;
			curVal = checkRange(curVal);
			text = text.substring(0, caretPosition) + Character.forDigit(curVal, 10) + text.substring(caretPosition + 1);
		}
		else if (key == Canvas.LEFT && caretPosition > 0) // Move back one char
		{
			caretPosition--;
			if (caretPosition > 0 && (text.charAt(caretPosition) == ':' || text.charAt(caretPosition) == '-' || text.charAt(caretPosition) == '/'))
			{
				caretPosition--;
			}
			if (caretPosition < 0) { caretPosition = 0; }
			changed = true;
		}
		else if (key == Canvas.RIGHT && caretPosition < text.length()-1) // Move forward one char
		{
			caretPosition++;
			if(text.charAt(caretPosition) == ':' || text.charAt(caretPosition) == '-' || text.charAt(caretPosition) == '/') { caretPosition++; }
			changed = true;
		}
		else { handled = false; changed = false; }

		if (changed) { notifyStateChanged(); }

		if (handled) { _invalidateContents(); }

		return handled;
	}

	// TODO: I wonder if skt has an "XDateField" or something like that... leaving those here just in case
	public void externalKeyPressed(int key) { keyPressed(key); }
	protected void externalRenderItem(Graphics graphics, int x, int y, int width, int height, boolean isSelected) { renderItem(graphics, x, y, width, height, isSelected); }

	protected void renderItem(Graphics graphics, int x, int y, int width, int height, boolean isSelected)
	{
		if (inputMode == TIME && caretPosition < 11) { caretPosition = 11; }
		if (inputMode == DATE && caretPosition > 9) { caretPosition = 9; }

		String relevantText;
		int offset = 0;

		if (inputMode == DATE)
		{
			int dash = text.indexOf('-');
			relevantText = text.substring(0, dash != -1 ? dash : text.length());
		}
		else if (inputMode == TIME)
		{
			offset = text.indexOf('-') + 1;
			relevantText = text.substring(offset > 0 ? offset : 0);
		}
		else // DATE_TIME
		{
			relevantText = text;
		}

		int localCaretPos = Math.max(0, Math.min(caretPosition - offset, relevantText.length() - 1));

		LCDUIRenderer.drawEditableField(graphics, x, y, width, height,
			relevantText, localCaretPos, null, null, 0, margin, padding, isSelected, false);
	}

	protected boolean traverse(int dir, int viewportWidth, int viewportHeight, int[] visRect_inout)
	{
		if (!highlighted)
		{
			highlighted = true;
			_invalidateContents();
		}

		return false;
	}

	protected void traverseOut()
	{
		if (highlighted)
		{
			highlighted = false;
			_invalidateContents();
		}
	}

	public String formatDate()
	{
		try { dateValue = format.parse(text); }
		catch (Exception e) { } // Should never happen

		Calendar calendar = Calendar.getInstance(timeZone);
		calendar.setTime(dateValue);

		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH) + 1; // Months are 0-based
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		int second = calendar.get(Calendar.SECOND);
		int milli = calendar.get(Calendar.MILLISECOND);

		StringBuilder formattedDate = new StringBuilder();

		if (inputMode == DATE)           { formattedDate.append(String.format("%04d/%02d/%02d-00:00:00:000", year, month, day)); }
		else if (inputMode == TIME)      { formattedDate.append(String.format("1970/01/01-%02d:%02d:%02d:%03d", hour, minute, second, milli)); }
		else if (inputMode == DATE_TIME) { formattedDate.append(String.format("%04d/%02d/%02d-%02d:%02d:%02d:%03d", year, month, day, hour, minute, second, milli)); }

		return formattedDate.toString();
	}

	public int checkRange(int val)
	{
		int month = Integer.parseInt(text.substring(5, 7));

		switch (caretPosition)
		{
			case 5: // Month tens
				if (val <= 0)
				{
					if(Integer.parseInt("" + text.charAt(6)) < 1) // Correct month units if they're out of range
					{
						text = text.substring(0, 6) + Character.forDigit(1, 10) + text.substring(7);
					}
					// Reset day to 01
					text = text.substring(0, 8) + Character.forDigit(0, 10) + Character.forDigit(1, 10) + text.substring(10);
					return 0;
				}
				if (val >= 1)
				{
					if(Integer.parseInt("" + text.charAt(6)) > 2) // Correct month units if they're out of range
					{
						text = text.substring(0, 6) + Character.forDigit(2, 10) + text.substring(7);
					}
					// Reset day to 01
					text = text.substring(0, 8) + Character.forDigit(0, 10) + Character.forDigit(1, 10) + text.substring(10);
					return 1;
				}
				break;

			case 6: // Month units
				if(Integer.parseInt("" + text.charAt(5)) == 1)
				{
					if (val < 0) { return 0; }
					if (val > 2) { return 2; }
				}
				else
				{
					if (val < 1) { return 1; }
					if (val > 9) { return 9; }
				}
				break;

			case 8: // Day tens
				if(month == 2) // February is the only with less than 30 days
				{
					if (val <= 0)
					{
						if(Integer.parseInt("" + text.charAt(9)) < 1) // Correct day units if they're out of range
						{
							text = text.substring(0, 9) + Character.forDigit(1, 10) + text.substring(10);
						}
						return 0;
					}
					if (val >= 2)
					{
						if(Integer.parseInt("" + text.charAt(9)) > getMaxDayUnitInMonth(month)) // Correct day units if they're out of range
						{
							text = text.substring(0, 9) + Character.forDigit(getMaxDayUnitInMonth(month), 10) + text.substring(10);
						}
						return 2;
					}
				}
				else
				{
					if (val <= 0)
					{
						if(Integer.parseInt("" + text.charAt(9)) < 1) // Correct day units if they're out of range
						{
							text = text.substring(0, 9) + Character.forDigit(1, 10) + text.substring(10);
						}
						return 0;
					}
					if (val >= 3)
					{
						if(Integer.parseInt("" + text.charAt(9)) > getMaxDayUnitInMonth(month)) // Correct day units if they're out of range
						{
							text = text.substring(0, 9) + Character.forDigit(getMaxDayUnitInMonth(month), 10) + text.substring(10);
						}
						return 3;
					}
				}
				break;

			case 9: // Day units
				if(Integer.parseInt("" + text.charAt(8)) > 0)
				{
					if(month == 2)
					{
						if(Integer.parseInt("" + text.charAt(8)) == 2)
						{
							if(val > getMaxDayUnitInMonth(month))
							{
								return getMaxDayUnitInMonth(month);
							}
							if(val < 0) { return 0; }
						}
						else
						{
							if(val < 0) { return 0; }
							if(val > 9) { return 9; }
						}
					}
					else
					{
						if(Integer.parseInt("" + text.charAt(8)) == 3)
						{
							if(val > getMaxDayUnitInMonth(month))
							{
								return getMaxDayUnitInMonth(month);
							}
							if(val < 0) { return 0; }
						}
						else
						{
							if(val < 0) { return 0; }
							if(val > 9) { return 9; }
						}
					}
				}
				else
				{
					if(val < 1) { return 1; }
					if(val > 9) { return 9; }
				}
				break;

			case 11: // Hour tens
				if (val < 0) { return 0; }
				if (val >= 2)
				{
					if(Integer.parseInt("" + text.charAt(12)) > 3) // Correct hour units if they're out of range
					{
						text = text.substring(0, 12) + Character.forDigit(3, 10) + text.substring(13);
					}
					return 2;
				}
				break;

			case 12: // Hour units place
				if(Integer.parseInt("" + text.charAt(11)) == 2)
				{
					if (val < 0) { return 0; }
					if (val > 3) { return 3; }
				}
				else
				{
					if (val < 0) { return 0; }
					if (val > 9) { return 9; }
				}
				break;

			case 14: // Minute tens
			case 17: // second tens
				if (val < 0) { return 0; }
				if (val > 5) { return 5; }
				break;

			case 0:	// Year field
			case 1:
			case 2:
			case 3:
			case 15: // Minute units
			case 18: // Second units
			case 20: // Milliseconds
			case 21:
			case 22:
				if (val < 0) { return 0; }
				if (val > 9) { return 9; }
				break;

			default:
				throw new IllegalArgumentException("Invalid position.");
		}

		return val;
	}

	private int getMaxDayUnitInMonth(int month)
	{
		int year = Integer.parseInt(text.substring(0, 4));
		switch (month)
		{
			case 1:
			case 3:
			case 5:
			case 7:
			case 8:
			case 10:
			case 12:
				return 1;
			case 4:
			case 6:
			case 9:
			case 11:
				return 0;
			case 2:
				// Check for leap year
				if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) { return 9; }
				else { return 8; }
			default:
				return 0; // Shouldn't happen
		}
	}
}
