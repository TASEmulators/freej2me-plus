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
package javax.microedition.m3g;

import java.util.Hashtable;

import org.recompile.mobile.Mobile;

public class Sprite3D extends Node
{

	private static final int FLIPX = 1;
	private static final int FLIPY = 2;
	private int flip;

	private Image2D image;
	private Appearance appearance;
	private boolean scaled;
	private Texture2D texture;

	private int cropw;
	private int croph;
	private int cropx;
	private int cropy;

	public Sprite3D(boolean isScaled, Image2D img, Appearance a)
	{
		/* As per JSR-184, the image cannot be null, and the crop rectangle initially covers the whole image. */
		if (img == null) { throw new NullPointerException("Cannot create a Sprite3D with a null image."); }
		scaled = isScaled;
		image = img;
		appearance = a;
		cropx = 0;
		cropy = 0;
		cropw = img.getWidth();
		croph = img.getHeight();
	}

	public Appearance getAppearance() { return appearance; }

	public int getCropHeight() { return croph; }

	public int getCropWidth() { return cropw; }

	public int getCropX() { return cropx; }

	public int getCropY() { return cropy; }

	public Image2D getImage() { return image; }

	public boolean isScaled() { return scaled; }

	public void setAppearance(Appearance a) 
	{ 
		removeReference(appearance);
		appearance = a; 
		addReference(appearance);
	}

	public void setCrop(int cropX, int cropY, int width, int height)
	{
		cropx=cropX;
		cropy=cropY;
		cropw=width;
		croph=height;
	}

	public void setImage(Image2D img) 
	{ 
		if(img == null) { throw new NullPointerException("Cannot set null image on a Sprite3D"); }
		removeReference(this.image);
		this.image = img;
		addReference(this.image);
		cropx = 0;
		cropy = 0;
		cropw = img.getWidth();
		croph = img.getHeight();
	}

	@Override
	void updateProperty(int property, float[] value) 
	{
		Mobile.log(Mobile.LOG_WARNING, Graphics3D.class.getPackage().getName() + "." + Graphics3D.class.getSimpleName() + ": " + "AnimTrack updating Sprite3D property");
		switch (property) 
		{
			case AnimationTrack.CROP:
				if (value.length > 2) 
				{
					setCrop((int)value[0], (int)value[1], (int) M3GMath.max(-Graphics3D.MAX_TEXTURE_DIMENSION, M3GMath.min(Graphics3D.MAX_TEXTURE_DIMENSION, value[2])),
							(int) M3GMath.max(-Graphics3D.MAX_TEXTURE_DIMENSION, M3GMath.min(Graphics3D.MAX_TEXTURE_DIMENSION, value[3])));
				} 
				else 
				{
					setCrop((int)value[0], (int)value[1], getCropWidth(), getCropHeight());
				}
			default:
				super.updateProperty(property, value);
		}
	}

	boolean animTrackCompatible(AnimationTrack track) 
	{
		switch (track.getTargetProperty()) 
		{
			case AnimationTrack.CROP:
				return true;
			default:
				return super.animTrackCompatible(track);
		}
	}
}
