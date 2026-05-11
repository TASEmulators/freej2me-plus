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
package com.nttdocomo.opt.ui;

import com.nttdocomo.ui.Image;
import com.nttdocomo.ui.impls.ImageImpl;
import org.recompile.mobile.Mobile;

public class Graphics2 extends com.nttdocomo.ui.Graphics
{ 
	public static final int CM_NORMAL = 0;
	public static final int CM_ZOOM = 256;
	public static final int OP_REPL = 0;
	public static final int OP_ADD = 1;
	public static final int OP_SUB = 2;

	private static final String[] operations = {"OP_REPL", "OP_ADD", "OP_SUB"};
	protected int coordMode = CM_NORMAL;

	public Graphics2(org.recompile.mobile.PlatformImage image) { super(image); }

	public void setCoordinateMode(int mode) { coordMode = mode; }

	public void setRenderMode(int operator, int srcRatio, int dstRatio) 
	{
		if (operator < 0 || operator > 2)   { throw new IllegalArgumentException("Invalid operator: " + operator); }
		if (srcRatio < 0 || srcRatio > 255) { throw new IllegalArgumentException("Invalid srcRatio: " + srcRatio); }
		if (dstRatio < 0 || dstRatio > 255) { throw new IllegalArgumentException("Invalid dstRatio: " + dstRatio); }
	
		this.renderMode = operator;
		this.srcRatio = srcRatio;
		this.dstRatio = dstRatio;
	}

	public static int getIntermediateColor(int color1, int color2, int ratio) 
	{
		if (ratio < 0 || ratio > 255) { throw new IllegalArgumentException("Invalid ratio: " + ratio); }

		int red1 = (color1 >> 16) & 0xFF;
		int green1 = (color1 >> 8) & 0xFF;
		int blue1 = color1 & 0xFF;

		int red2 = (color2 >> 16) & 0xFF;
		int green2 = (color2 >> 8) & 0xFF;
		int blue2 = color2 & 0xFF;

		int newRed = clamp((255 - ratio) * red1 + ratio * red2) / 255;
		int newGreen = clamp((255 - ratio) * green1 + ratio * green2) / 255;
		int newBlue = clamp((255 - ratio) * blue1 + ratio * blue2) / 255;

		return (0xFF << 24) | (newRed << 16) | (newGreen << 8) | newBlue;
	}

	public void drawImage(com.nttdocomo.ui.Image image, com.nttdocomo.opt.ui.j3d.AffineTrans at) 
	{
		Mobile.log(Mobile.LOG_WARNING, Graphics2.class.getPackage().getName() + "." + Graphics2.class.getSimpleName() + ": " + "drawImage A");
	}

	public void drawImage(com.nttdocomo.ui.Image image, com.nttdocomo.opt.ui.j3d.AffineTrans at, int sx, int sy, int width, int height) 
	{
		Mobile.log(Mobile.LOG_WARNING, Graphics2.class.getPackage().getName() + "." + Graphics2.class.getSimpleName() + ": " + "drawImage B");
	}

	// For fixed-width fonts, this is equivalent to calling Graphics#drawString after extracting digit characters from "(an infinite blank string) + (a string representation of value)" from the right
	public void drawNumber(int x, int y, int value, int digit) 
	{
		if (digit <= 0) { throw new IllegalArgumentException("Digit must be greater than 0: " + digit); }

		String numberString = Integer.toString(value);
		
		if (value < 0) { numberString = "-" + numberString.substring(1); }

		StringBuilder paddedString = new StringBuilder();
		
		int paddingLength = digit - numberString.length();
		if (paddingLength > 0) 
		{
			for (int i = 0; i < paddingLength; i++) { paddedString.append(' '); }
		}
		paddedString.append(numberString);

		drawString(paddedString.toString(), x, y);
	}

	public Image getImage(int x, int y, int width, int height) 
	{
		// Validate the width and height
		if (width <= 0 || height <= 0) { throw new IllegalArgumentException("Width and height must be greater than 0: width=" + width + ", height=" + height); }

		int adjustedX = Math.max(x, 0);
		int adjustedY = Math.max(y, 0);
		int adjustedWidth = Math.min(width, canvasWidth - adjustedX);
		int adjustedHeight = Math.min(height, canvasHeight - adjustedY);
		if (adjustedWidth <= 0 || adjustedHeight <= 0) { throw new IllegalArgumentException("Adjusted Width and height must be greater than 0: width=" + adjustedWidth + ", height=" + adjustedHeight); }

		Image subArea = new ImageImpl(adjustedWidth, adjustedHeight);
		copyToFrameBuffer(subArea, adjustedX, adjustedY, adjustedWidth, adjustedHeight, 0, 0, 0);
		return subArea;
	}

	// For a display device with a vertical synchronization rate of exactly 60 times per second, 1000000/60 = 16667 is returned
	public int getSyncUnlockInterval() { return 16667; }

	// Do we actually need to implement this? Return 'interval' for now
	public int syncUnlock(int interval) { /* TODO */ return interval; }
}