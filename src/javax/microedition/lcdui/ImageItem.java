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


public class ImageItem extends Item
{

	public static final int LAYOUT_CENTER = 3;
	public static final int LAYOUT_DEFAULT = 0;
	public static final int LAYOUT_LEFT = 1;
	public static final int LAYOUT_NEWLINE_AFTER = 0x200;
	public static final int LAYOUT_NEWLINE_BEFORE = 0x100;
	public static final int LAYOUT_RIGHT = 2;

	private Image image;
	private String altText;
	private int appearance;

	public ImageItem(String label, Image img, int layout, String alt)
	{
		this(label, img, layout, alt, PLAIN);
	}

	public ImageItem(String label, Image img, int layout, String alt, int appearanceMode)
	{
		if(appearanceMode < PLAIN || appearanceMode > BUTTON) { throw new IllegalArgumentException("Invalid appearance mode"); }

		setLabel(label);
		setLayout(layout);
		image = img;
		altText = alt;
		appearance = appearanceMode;

		updatePreferredSize();
	}

	public String getAltText() { return altText; }

	public int getAppearanceMode() { return appearance; }

	public Image getImage() { return image; }

	public void setAltText(String text) { altText = text; }

	public void setImage(Image img) { image = img; }


	protected int getContentWidth()
	{
		return (image != null) ? image.getWidth() : 0;
	}

	protected int getContentHeight(int width)
	{
		return (image != null) ? image.getHeight() : 0;
	}
}
