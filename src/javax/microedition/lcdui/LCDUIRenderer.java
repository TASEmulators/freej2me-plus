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

// Class responsible for rendering LCDUI elements with a consistent style.
// Said style is basically Swing's Metal look and feel.
public class LCDUIRenderer
{
	// Standard gray colors used for the gradient bases and lines
	private static final int WHITE = 255;
	private static final int LIGHTER_GRAY = 228;
	private static final int BASE_GRAY = 192;
	private static final int DARKER_GRAY = 128;

	public static final int SCROLLBAR_W = 4;

	static final Font commandFont = new Font(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL);
	static final Font tickerFont = new Font(Font.FACE_MONOSPACE, Font.STYLE_ITALIC, Font.SIZE_SMALL);

	public static void drawTitleBar(Graphics graphics, int width, String title, int titleHeight, int bgCol)
	{
		int bgR = (bgCol >> 16) & 0xFF;
		int bgG = (bgCol >> 8) & 0xFF;
		int bgB = bgCol & 0xFF;

		// Title bar gradient: starts gray at top, blends to lcduiBGColor at bottom
		for (int py = 0; py < titleHeight; py++)
		{
			float ratio = (titleHeight <= 1) ? 0f : (float) py / (titleHeight - 1);
			int r = (int)(BASE_GRAY + ratio * (bgR - BASE_GRAY));
			int g = (int)(BASE_GRAY + ratio * (bgG - BASE_GRAY));
			int b = (int)(BASE_GRAY + ratio * (bgB - BASE_GRAY));

			graphics.setColor(r, g, b);
			graphics.drawLine(0, py, width, py);
		}

		// Title bottom border line
		graphics.setColor(DARKER_GRAY, DARKER_GRAY, DARKER_GRAY);
		graphics.drawLine(0, titleHeight, width, titleHeight);
		graphics.setColor(Mobile.lcduiTextColor);
		graphics.drawString(title != null ? title : "", width/2, -2, Graphics.TOP | Graphics.HCENTER);
	}

	public static void drawCommandBar(Graphics graphics, String status, ArrayList<Command> commands, Command itemCommand,
		int width, int totalHeight, int barHeight, float alphaFactor, int bgColor, boolean threeItems)
	{
		Font tmpFont = graphics.getFont();
		graphics.setFont(commandFont);
		int baseAlpha = (int)(0xFF * alphaFactor);
		int bgR = (bgColor >> 16) & 0xFF;
		int bgG = (bgColor >> 8) & 0xFF;
		int bgB = bgColor & 0xFF;

		int startY = totalHeight - barHeight;

		// Command bar gradient: starts lcduiBGColor at top, blends to gray at bottom
		for (int py = 0; py < barHeight; py++)
		{
			float ratio = (barHeight <= 1) ? 0f : (float) py / (barHeight - 1);
			int r = (int)(bgR + ratio * (BASE_GRAY - bgR));
			int g = (int)(bgG + ratio * (BASE_GRAY - bgG));
			int b = (int)(bgB + ratio * (BASE_GRAY - bgB));

			int rowColor = (baseAlpha << 24) | (r << 16) | (g << 8) | b;
			graphics.setAlphaRGB(rowColor);
			graphics.drawLine(0, startY + py, width, startY + py);
		}

		// Command bar top border
		graphics.setAlphaRGB((baseAlpha << 24) | (DARKER_GRAY << 16) | (DARKER_GRAY << 8) | DARKER_GRAY);
		graphics.drawLine(0, startY, width, startY);

		// If three items, V-shaped separator, otherwise only a vertical separator
		// at the center of the bar.
		if (threeItems)
		{
			// Define exact symmetric top split points (1/3 and 2/3 of the screen)
			int topX1 = width / 3 - 2;
			int topX2 = (2 * width) / 3 + 2;

			int slant = 6;
			if (slant > width / 10) {
				slant = Math.max(2, width / 10);
			}

			// Both lines share the exact same parallel angle:
			// Left separator: shifts right from topX1
			// Right separator: mirrors topX2
			graphics.drawLine(topX1, startY, topX1 + slant, totalHeight);
			graphics.drawLine(topX2, startY, topX2 - slant, totalHeight);
		}
		else { graphics.drawLine(width / 2, startY, width / 2, totalHeight); }

		// Command text drawing
		graphics.setAlphaRGB((baseAlpha << 24) | Mobile.lcduiTextColor);
		if (commands == null || commands.isEmpty()) { return; }

		int size = commands.size();
		if (size == 1)
		{
			int textCenter = (graphics.getGraphics2D().getFontMetrics().stringWidth(commands.get(0).getLabel())) / 2;
			graphics.drawString(commands.get(0).getLabel(),
				(threeItems ? (width / 6) : (width / 4)) - textCenter, startY, Graphics.TOP | Graphics.LEFT);
			if (status != null)
			{
				textCenter = (graphics.getGraphics2D().getFontMetrics().stringWidth(status)) / 2;
				graphics.drawString(status != null ? status : "", (3 * width / 4) - textCenter, startY, Graphics.TOP | Graphics.LEFT);
			}
		}
		else if (size == 2)
		{
			int textCenter = (graphics.getGraphics2D().getFontMetrics().stringWidth(commands.get(0).getLabel())) / 2;
			graphics.drawString(commands.get(0).getLabel(),
				(threeItems ? (width / 6) : (width / 4)) - textCenter, startY, Graphics.TOP | Graphics.LEFT);

			textCenter = (graphics.getGraphics2D().getFontMetrics().stringWidth(commands.get(1).getLabel())) / 2;
			graphics.drawString(commands.get(1).getLabel(),
				(threeItems ? (5 * width / 6) : (3 * width / 4)) + textCenter, startY, Graphics.TOP | Graphics.RIGHT);

			if (status != null && itemCommand == null)
			{
				graphics.drawString(status != null ? status : "", width / 2, startY, Graphics.TOP | Graphics.HCENTER);
			}
		}
		else
		{
			// Options view, opens the command list.
			int textCenter = (graphics.getGraphics2D().getFontMetrics().stringWidth("Options")) / 2;
			graphics.drawString("Options", (threeItems ? (width / 6) : (width / 4)) - textCenter, startY, Graphics.TOP | Graphics.LEFT);
		}

		if (itemCommand != null)
		{
			graphics.drawString(itemCommand.getLabel(), width / 2, startY, Graphics.TOP | Graphics.HCENTER);
		}

		graphics.setFont(tmpFont);
	}

	public static void drawItem(Graphics graphics, int index, String label, Image image, int x, int y, int width, int height,
		boolean selected, boolean checked, int choiceType, boolean isInner, int layoutFlag)
	{
		int horizontalInset = 4; // 2px margin on each side so items don't kiss the screen edges
		int drawX = x + horizontalInset;
		int drawWidth = width - (horizontalInset * 2);
		int lineHeight = Font.getDefaultFont().getHeight();

		int halfH = height / 2;
		if (halfH < 1) { halfH = 1; }

		if (selected)
		{
			// Protruding / Raised button gradient for selected item
			for (int py = 0; py < height; py++)
			{
				float distFromCenter = Math.abs(py - halfH) / (float) halfH;
				if (distFromCenter > 1f) { distFromCenter = 1f; }

				int val = (int)(BASE_GRAY + (255 - BASE_GRAY) * (1.0f - distFromCenter));
				graphics.setColor(val, val, val);
				graphics.drawLine(drawX, y + py, drawX + drawWidth, y + py);
			}

			graphics.setColor(DARKER_GRAY, DARKER_GRAY, DARKER_GRAY);
			graphics.drawRect(drawX, y, drawWidth, height);
		}
		else if(!isInner)
		{
			// Punched-in / Recessed gradient for unselected
			for (int py = 0; py < height; py++)
			{
				float distFromCenter = Math.abs(py - halfH) / (float) halfH;
				if (distFromCenter > 1f) { distFromCenter = 1f; }

				int val = (int)(BASE_GRAY + (LIGHTER_GRAY - BASE_GRAY) * distFromCenter);
				graphics.setColor(val, val, val);
				graphics.drawLine(drawX, y + py, drawX + drawWidth, y + py);
			}
		}

		if(choiceType == Choice.MULTIPLE) { drawTick(graphics, index, lineHeight, checked, false); }
		else if(choiceType == Choice.EXCLUSIVE) { drawTick(graphics, index, lineHeight, checked, true); }
		else if (choiceType == Choice.POPUP)
		{
			final int arrowWidth = Font.getDefaultFont().getHeight() / 2;
			final int arrowMargin = Font.getDefaultFont().getHeight() / 15;
			final int arrowPadding = Font.getDefaultFont().getHeight() / 2;
			final int arrowSpacing = arrowWidth + arrowMargin + arrowPadding;

			graphics.drawString("<", x + arrowSpacing - 1, y, Graphics.TOP | Graphics.RIGHT);
			graphics.drawString(">", x + arrowSpacing + (width - 2 * arrowSpacing) + 2, y, Graphics.TOP | Graphics.LEFT);
			graphics.drawString(label != null ? label : "", width / 2, y, Graphics.TOP | Graphics.HCENTER);
			return;
		}

		// Now to text rendering
		if(selected) { graphics.setColor(Mobile.lcduiTextColor); }
		else { graphics.setColor(DARKER_GRAY, DARKER_GRAY, DARKER_GRAY); }

		int tickOffset = (choiceType == Choice.IMPLICIT ? 0 : (lineHeight * 4 / 3));
		int contentX = (choiceType == Choice.IMPLICIT) ? drawX : (x + tickOffset);

		int hAlign = layoutFlag & 0x3;  // LAYOUT_LEFT (1), LAYOUT_RIGHT (2), LAYOUT_CENTER (3)
		int vAlign = layoutFlag & 0x30; // LAYOUT_TOP (0x10), LAYOUT_BOTTOM (0x20), LAYOUT_VCENTER (0x30)

		if (image != null)
		{
			int imgWidth = image.getWidth();
			int imgHeight = image.getHeight();
			int imgX = contentX;

			if (choiceType == Choice.IMPLICIT)
			{
				if (hAlign == Item.LAYOUT_RIGHT) { imgX = drawX + drawWidth - imgWidth; }
				else if (hAlign == Item.LAYOUT_CENTER) { imgX = drawX + (drawWidth - imgWidth) / 2; }
				else { imgX = drawX; }
			}

			int imgY = y + (height - imgHeight) / 2; // Default to vertical center
			if (vAlign == Item.LAYOUT_TOP) { imgY = y; }
			else if (vAlign == Item.LAYOUT_BOTTOM) { imgY = y + height - imgHeight; }
			if (imgY < y) { imgY = y; }

			graphics.drawImage(image, imgX, imgY, 0);

			if (choiceType != Choice.IMPLICIT || hAlign == Item.LAYOUT_LEFT) { contentX += imgWidth + 4; }
		}

		if (choiceType == Choice.IMPLICIT && image == null)
		{
			if (hAlign == Item.LAYOUT_RIGHT)
			{
				graphics.drawString(label != null ? label : "", drawX + drawWidth - 3, y, Graphics.TOP | Graphics.RIGHT);
			}
			else if (hAlign == Item.LAYOUT_CENTER)
			{
				graphics.drawString(label != null ? label : "", x + (width / 2), y, Graphics.TOP | Graphics.HCENTER);
			}
			else
			{
				graphics.drawString(label != null ? label : "", drawX + 3, y, Graphics.TOP | Graphics.LEFT);
			}
		}
		else
		{
			// If explicitly right-aligned or center-aligned via layout flags:
			if (choiceType == Choice.IMPLICIT && hAlign == Item.LAYOUT_RIGHT)
			{
				graphics.drawString(label != null ? label : "", drawX + drawWidth - 3, y, Graphics.TOP | Graphics.RIGHT);
			}
			else if (choiceType == Choice.IMPLICIT && hAlign == Item.LAYOUT_CENTER)
			{
				graphics.drawString(label != null ? label : "", x + (width / 2), y, Graphics.TOP | Graphics.HCENTER);
			}
			else
			{
				graphics.drawString(label != null ? label : "", contentX + 3, y, Graphics.TOP | Graphics.LEFT);
			}
		}

		graphics.setColor(Mobile.lcduiTextColor);
	}

	public static void fillBackground(Graphics graphics, int width, int height)
	{
		graphics.setColor(LIGHTER_GRAY, LIGHTER_GRAY, LIGHTER_GRAY);
		graphics.fillRect(0,0,width,height);
		graphics.setColor(Mobile.lcduiTextColor);
	}

	public static void drawScrollIndicator(Graphics graphics, int ax, int ay, boolean pointUp)
	{
		graphics.setColor(DARKER_GRAY, DARKER_GRAY, DARKER_GRAY);

		if(pointUp) // Draw a tiny 5x3 upward-pointing pixel triangle
		{
			graphics.drawLine(ax,     ay + 2, ax + 4, ay + 2);
			graphics.drawLine(ax + 1, ay + 1, ax + 3, ay + 1);
			graphics.drawLine(ax + 2, ay,     ax + 2, ay    );
		}
		else // Same, but downward
		{
			graphics.drawLine(ax + 2, ay,     ax + 2, ay    );
			graphics.drawLine(ax + 1, ay - 1, ax + 3, ay - 1);
			graphics.drawLine(ax,     ay - 2, ax + 4, ay - 2);
		}
		graphics.setColor(Mobile.lcduiTextColor);
	}

	public static void drawScrollBar(Graphics graphics, int x, int y, int height)
	{
		graphics.setColor(DARKER_GRAY, DARKER_GRAY, DARKER_GRAY);
		graphics.fillRoundRect(x - SCROLLBAR_W, y, SCROLLBAR_W, height, 4, 4);
		graphics.setColor(Mobile.lcduiTextColor);
	}

	public static void drawTick(Graphics graphics, int index, int height, boolean filled, boolean isCircle)
	{
		int tickWidth = Math.max(10, (height * 3) / 5);
		int tickX = height / 4 + 3;
		int tickY = index * height + (height - tickWidth) / 2;

		if (isCircle)
		{
			graphics.setColor(DARKER_GRAY, DARKER_GRAY, DARKER_GRAY);
			graphics.drawArc(tickX, tickY, tickWidth, tickWidth, 0, 360);

			if (filled)
			{
				int radius = tickWidth / 2 - 1;
				int centerX = tickX + tickWidth / 2;
				int centerY = tickY + tickWidth / 2;

				// Render a smooth 3D shaded sphere using concentric filled arcs from edge to center
				for (int r = radius; r >= 1; r--)
				{
					float ratio = (float) r / radius; // 1 at edge, 0 at center
					int val = (int)(DARKER_GRAY + (WHITE - DARKER_GRAY) * (1.0f - ratio));
					graphics.setColor(val, val, val);
					graphics.fillArc(centerX - r, centerY - r, r * 2, r * 2, 0, 360);
				}

				// Crisp outer ring for the inner selected indicator
				graphics.setColor(DARKER_GRAY, DARKER_GRAY, DARKER_GRAY);
			}
		}
		else
		{
			graphics.setColor(DARKER_GRAY, DARKER_GRAY, DARKER_GRAY);
			graphics.drawRect(tickX, tickY, tickWidth, tickWidth);

			if (filled)
			{
				int innerSize = tickWidth - 4;
				int innerX = tickX + 2;
				int innerY = tickY + 2;
				int halfH = innerSize / 2;
				if (halfH < 1) { halfH = 1; }

				for (int py = 0; py <= innerSize; py++)
				{
					float distFromCenter = Math.abs(py - halfH) / (float) halfH;
					if (distFromCenter > 1f) { distFromCenter = 1f; }

					int val = (int)(DARKER_GRAY + (WHITE - DARKER_GRAY) * (1.0f - distFromCenter));
					graphics.setColor(val, val, val);
					graphics.drawLine(innerX, innerY + py, innerX + innerSize, innerY + py);
				}
				graphics.setColor(DARKER_GRAY, DARKER_GRAY, DARKER_GRAY);
				graphics.drawRect(innerX, innerY, innerSize, innerSize);
			}
		}
		graphics.setColor(Mobile.lcduiTextColor);
	}

	public static void drawGauge(Graphics graphics, int x, int y, int width, int height, int value, int maxValue, boolean interactive, int animationOffset, boolean selected)
	{
		graphics.translate(x, y);

		final int arrowWidth = Font.getDefaultFont().getHeight() / 2;
		final int arrowMargin = Font.getDefaultFont().getHeight() / 15;
		final int arrowPadding = Font.getDefaultFont().getHeight() / 2;
		final int arrowSpacing = arrowWidth + arrowMargin + arrowPadding;

		int barWidthTotal = width - (interactive ? 2 * arrowSpacing : arrowSpacing);
		int barX = interactive ? arrowSpacing : arrowSpacing / 2;
		int barHeight = Font.getDefaultFont().getHeight();

		if (interactive)
		{
			graphics.setColor(DARKER_GRAY, DARKER_GRAY, DARKER_GRAY);
			graphics.drawString("<", arrowSpacing - 3, (barHeight - Font.getDefaultFont().getHeight()) / 2, Graphics.TOP | Graphics.RIGHT);
			graphics.drawString(">", arrowSpacing + barWidthTotal + 3, (barHeight - Font.getDefaultFont().getHeight()) / 2, Graphics.TOP | Graphics.LEFT);
			graphics.setColor(Mobile.lcduiTextColor);
		}

		// Draw recessed track background with rounded corners
		int arc = 4;
		for (int py = 1; py < barHeight; py++)
		{
			float ratio = (barHeight <= 1) ? 0f : (float) py / (barHeight - 1);
			int val = (int)(BASE_GRAY + (LIGHTER_GRAY - BASE_GRAY) * Math.abs(ratio - 0.5f) * 2f);
			graphics.setColor(val, val, val);
			graphics.drawLine(barX + 1, py, barX + barWidthTotal - 1, py);
		}
		graphics.setColor(DARKER_GRAY, DARKER_GRAY, DARKER_GRAY);
		graphics.drawRoundRect(barX, 0, barWidthTotal, barHeight, arc, arc);

		// Draw filled portion if definite (with a gradient of course)
		if (maxValue != Gauge.INDEFINITE && maxValue > 0)
		{
			int fillW = (int)((value / (float) maxValue) * (barWidthTotal - 4));
			if (fillW < 0) { fillW = 0; }
			if (fillW > barWidthTotal - 4) { fillW = barWidthTotal - 4; }

			if (fillW > 0)
			{
				int halfH = (barHeight - 4) / 2;
				if (halfH < 1) { halfH = 1; }
				for (int py = 3; py < barHeight - 2; py++)
				{
					float distFromCenter = Math.abs((py - 2) - halfH) / (float) halfH;
					if (distFromCenter > 1f) { distFromCenter = 1f; }

					int val = (int)(DARKER_GRAY + (WHITE - DARKER_GRAY) * (1.0f - distFromCenter));
					graphics.setColor(val, val, val);
					graphics.drawLine(barX + 2, py, barX + 1 + fillW, py);
				}
				graphics.setColor(DARKER_GRAY, DARKER_GRAY, DARKER_GRAY);
				graphics.drawRoundRect(barX + 2, 2, fillW, barHeight - 4, arc - 2, arc - 2);
			}
		}
		else if (maxValue == Gauge.INDEFINITE) // INDEFINITE type is animated.
		{
			// Sliding block takes up 25% of the track
			int blockW = barWidthTotal / 4;
			int startX = barX + 2 + animationOffset;
			int fillW = blockW;

			// Clip block bounds inside the track
			if (startX < barX + 2) { startX = barX + 2; }
			if (startX + fillW > barX + barWidthTotal - 1) { fillW = (barX + barWidthTotal - 1) - startX; }

			if (fillW > 0)
			{
				int halfH = (barHeight - 4) / 2;
				if (halfH < 1) { halfH = 1; }
				for (int py = 3; py < barHeight - 2; py++)
				{
					float distFromCenter = Math.abs((py - 2) - halfH) / (float) halfH;
					if (distFromCenter > 1f) { distFromCenter = 1f; }

					int val = (int)(DARKER_GRAY + (WHITE - DARKER_GRAY) * (1.0f - distFromCenter));
					graphics.setColor(val, val, val);
					graphics.drawLine(startX, py, startX + fillW, py);
				}
				graphics.setColor(DARKER_GRAY, DARKER_GRAY, DARKER_GRAY);
				graphics.drawRoundRect(startX, 2, fillW, barHeight - 4, arc - 2, arc - 2);
			}
		}

		// Draw centered text label over the bar
		String text;
		if (maxValue != Gauge.INDEFINITE)
		{
			text = value + " (" + String.format("%.0f", (value / (float) maxValue * 100f)) + "%)";
		}
		else { text = "? (?%)"; }

		if(selected) { graphics.setColor(Mobile.lcduiTextColor); }
		else { graphics.setColor(DARKER_GRAY, DARKER_GRAY, DARKER_GRAY); }
		int textWidth = graphics.getGraphics2D().getFontMetrics().stringWidth(text);
		graphics.drawString(text, barX + (barWidthTotal - textWidth) / 2, (barHeight - Font.getDefaultFont().getHeight()) / 2, Graphics.TOP | Graphics.LEFT);

		graphics.translate(-x, -y);
	}

	public static void drawTicker(Graphics graphics, int width, String text, int tickerHeight, int y, int tickerX, int bgCol)
	{
		int bgR = (bgCol >> 16) & 0xFF;
		int bgG = (bgCol >> 8) & 0xFF;
		int bgB = bgCol & 0xFF;

		for (int py = 0; py < tickerHeight; py++)
		{
			float ratio = (tickerHeight <= 1) ? 0f : (float) py / (tickerHeight - 1);
			int r = (int)(BASE_GRAY + ratio * (bgR - BASE_GRAY));
			int g = (int)(BASE_GRAY + ratio * (bgG - BASE_GRAY));
			int b = (int)(BASE_GRAY + ratio * (bgB - BASE_GRAY));

			graphics.setColor(r, g, b);
			graphics.drawLine(0, y + py, width, y + py);
		}

		// Dotted border frame around the ticker so we get better UI separation
		graphics.setColor(DARKER_GRAY, DARKER_GRAY, DARKER_GRAY);
		int tmpStyle = graphics.getStrokeStyle();
		graphics.setStrokeStyle(Graphics.DOTTED);
		graphics.drawLine(0, y, width, y);
		graphics.setStrokeStyle(tmpStyle);

		graphics.setColor(Mobile.lcduiTextColor);
		Font tmpFont = graphics.getFont();
		graphics.setFont(tickerFont);
		graphics.drawString(text != null ? text : "", tickerX, y + (tickerHeight - Font.getDefaultFont().getHeight()) / 2, Graphics.TOP | Graphics.LEFT);
		graphics.setFont(tmpFont);
	}

	public static void drawEditableField(Graphics graphics, int x, int y, int width, int height,
		String text, int caretPosition, String explicitCaretChar,
		char[][] charSetHint, int max, int margin, int padding, boolean selected, boolean hasHint)
	{
		graphics.translate(x, y - 2);

		int textColor = selected ? Mobile.lcduiTextColor :
			(0xFF << 24) | (DARKER_GRAY << 16) | (DARKER_GRAY << 8) | DARKER_GRAY;

		int fontH = Font.getDefaultFont().getHeight();
		int fieldHeight = fontH + (padding * 3);
		int fieldWidth = width - 1 - (margin * 2);
		int halfH = fieldHeight / 2;
		if (halfH < 1) { halfH = 1; }

		// Punched-in / Recessed gradient background
		for (int py = 0; py < fieldHeight; py++)
		{
			float distFromCenter = Math.abs(py - halfH) / (float) halfH;
			if (distFromCenter > 1f) { distFromCenter = 1f; }

			int val = (int)(BASE_GRAY + (LIGHTER_GRAY - BASE_GRAY) * distFromCenter);
			graphics.setColor(val, val, val);
			graphics.drawLine(margin, py, margin + fieldWidth, py);
		}

		// Top inner shadow line & outer border frame
		graphics.setColor(DARKER_GRAY, DARKER_GRAY, DARKER_GRAY);
		graphics.drawLine(margin, 0, margin + fieldWidth, 0);
		graphics.drawRect(margin, 0, fieldWidth, fieldHeight);

		int textY = margin + padding;

		// Replace line breaks, they aren't visible by default.
		String formattedText = text.replace('\n', '↳');

		if (caretPosition < 0) { caretPosition = 0; }
		if (caretPosition > formattedText.length()) { caretPosition = formattedText.length(); }

		// Draw the existing text before the caret
		graphics.setColor(textColor);
		if (caretPosition > 0)
		{
			graphics.drawChars(formattedText.substring(0, caretPosition).toCharArray(), 0, caretPosition, margin + padding, textY, 0);
		}

		int caretWidth = Font.getDefaultFont().stringWidth(formattedText.substring(0, caretPosition));

		// Fill the background for the character to be inserted (at the caret position)
		String caretChar = explicitCaretChar;
		if (caretChar == null) {
			caretChar = (caretPosition < formattedText.length()) ? String.valueOf(formattedText.charAt(caretPosition)) : " ";
		}
		int caretCharWidth = Font.getDefaultFont().stringWidth(caretChar);

		graphics.setColor(textColor);
		graphics.fillRect(margin + padding + caretWidth, textY+1, caretCharWidth, fontH-1);

		graphics.setColor(Mobile.lcduiBGColor);
		graphics.drawString(caretChar, margin + padding + caretWidth, textY, 0);

		// Draw the remaining text after the caret
		int remainWidth = 0;
		graphics.setColor(textColor);
		if(formattedText.length() - (caretPosition + 1) > 0)
		{
			graphics.drawChars(formattedText.substring(caretPosition + 1).toCharArray(), 0, formattedText.length() - (caretPosition + 1), margin + padding + caretWidth + caretCharWidth, textY, 0);
			remainWidth = Font.getDefaultFont().stringWidth(formattedText.substring(caretPosition + 1));
		}

		// Draw indicators to show whether more text is allowed or not
		if(hasHint)
		{
			String indicator = (formattedText.length() < max) ? "⨁" : "⨂";
			graphics.setColor(formattedText.length() < max ? 0x00BB00 : 0x770000);
			graphics.drawString(indicator, margin + padding + caretWidth + caretCharWidth + remainWidth, textY, 0);
		}

		// Draw navigation arrows using original margin-based spacing so they clear the text
		graphics.setColor(textColor);
		graphics.drawString("^", margin + padding + caretWidth + caretCharWidth / 2 - 3, margin - fontH / 3, 0);
		graphics.drawString("v", margin + padding + caretWidth + caretCharWidth / 2 - 3, margin + fontH, 0);

		// Render the characterSet hint inside the right side of the text field box
		if(hasHint && charSetHint != null && charSetHint.length > 0)
		{
			String hintText = new String(charSetHint[0]);
			int hintWidth = Font.getDefaultFont().stringWidth(hintText);
			int hintX = margin + fieldWidth - hintWidth - padding;
			int hintY = textY;

			graphics.setColor(textColor);
			graphics.fillRoundRect(hintX, hintY, hintWidth + 1, fontH, 6, 6);

			graphics.setColor(Mobile.lcduiBGColor);
			graphics.drawString(hintText, hintX, hintY, 0);
		}

		graphics.setColor(Mobile.lcduiTextColor);
		graphics.translate(-x, -y+2);
	}

	public static void drawTextField(Graphics graphics, int x, int y, int width, int height,
		String text, int caretPosition, char[][] charSet, int charSetIdx, int selectedCharIndex,
		char[][] charSetHint, int max, int margin, int padding, boolean selected, boolean hasHint)
	{
		String caretChar = (charSet != null && charSet.length > charSetIdx && charSet[charSetIdx].length > selectedCharIndex)
			? ((charSet[charSetIdx][selectedCharIndex] == '\n') ? "↳" : String.valueOf(charSet[charSetIdx][selectedCharIndex]))
			: null;

		char[][] hintToPass = (charSetHint != null && charSetHint.length > charSetIdx) ? new char[][] { charSetHint[charSetIdx] } : charSetHint;

		drawEditableField(graphics, x, y, width, height, text, caretPosition, caretChar, hintToPass, max, margin, padding, selected, hasHint);
	}
}
