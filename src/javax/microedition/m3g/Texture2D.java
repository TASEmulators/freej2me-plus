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

import org.recompile.mobile.Mobile;

public class Texture2D extends Transformable
{

	public static final int FILTER_BASE_LEVEL = 208;
	public static final int FILTER_LINEAR = 209;
	public static final int FILTER_NEAREST = 210;
	public static final int FUNC_ADD = 224;
	public static final int FUNC_BLEND = 225;
	public static final int FUNC_DECAL = 226;
	public static final int FUNC_MODULATE = 227;
	public static final int FUNC_REPLACE = 228;
	public static final int WRAP_CLAMP = 240;
	public static final int WRAP_REPEAT = 241;

	private int blending;
	private int blendColor;
	private int imageFilter;
	private int levelFilter;
	private int wraps;
	private int wrapt;

	private boolean isNPOT;

	private Image2D texImage;

	public Texture2D(Image2D image)
	{
		this.wraps = WRAP_REPEAT;
		this.wrapt = WRAP_REPEAT;
		this.levelFilter = FILTER_BASE_LEVEL;
		this.imageFilter = FILTER_NEAREST;
		this.blending = FUNC_MODULATE;
		this.blendColor = 0x00000000;
		this.isNPOT = false;
		this.setImage(image);
	}

	protected Object3D duplicateImpl()
	{
		Texture2D copy = (Texture2D) super.duplicateImpl();
		copy.blending = this.blending;
		copy.blendColor = this.blendColor;
		copy.imageFilter = this.imageFilter;
		copy.levelFilter = this.levelFilter;
		copy.wraps = this.wraps;
		copy.wrapt = this.wrapt;
		copy.setImage(this.texImage); // Already adds the reference

		return copy;
	}

	public int getBlendColor()
	{
		return this.blendColor;
	}

	public int getBlending()
	{
		return this.blending;
	}

	public Image2D getImage()
	{
		return this.texImage;
	}

	public int getImageFilter()
	{
		return this.imageFilter;
	}

	public int getLevelFilter()
	{
		return this.levelFilter;
	}

	public int getWrappingS()
	{
		return this.wraps;
	}

	public int getWrappingT()
	{
		return this.wrapt;
	}

	public void setBlendColor(int RGB)
	{
		this.blendColor = RGB & 0x00FFFFFF; // Make sure alpha is discarded
	}

	public void setBlending(int func)
	{
		if (func != FUNC_REPLACE &&
			func != FUNC_MODULATE &&
			func != FUNC_DECAL &&
			func != FUNC_BLEND &&
			func != FUNC_ADD)
			{ throw new java.lang.IllegalArgumentException("Invalid texture blending mode"); }

		this.blending = func;
	}

	public void setFiltering(int levelFilter, int imageFilter)
	{
		if ((levelFilter != FILTER_BASE_LEVEL &&
			 levelFilter != FILTER_NEAREST &&
			 levelFilter != FILTER_LINEAR) ||
			(imageFilter != FILTER_NEAREST &&
			 imageFilter != FILTER_LINEAR))
			{ throw new java.lang.IllegalArgumentException("Invalid texture filter mode"); }

		this.levelFilter = levelFilter;
		this.imageFilter = imageFilter;
	}

	public void setImage(Image2D image)
	{
		if (image == null)
			{ throw new java.lang.NullPointerException("Cannot set texture as null image."); }
		if (image.getWidth() > Graphics3D.MAX_TEXTURE_DIMENSION ||
			image.getHeight() > Graphics3D.MAX_TEXTURE_DIMENSION)
			{ throw new java.lang.IllegalArgumentException("Invalid texture size"); }

		if(!isPowerOfTwo(image.getWidth()) || !isPowerOfTwo(image.getHeight()))
		{
			Mobile.log(Mobile.LOG_WARNING, Graphics3D.class.getPackage().getName() + "." + Graphics3D.class.getSimpleName() + ": " + "Texture (" + image.getWidth() + "," + image.getHeight() + ") is NPOT! Might cause render issues.");
			this.isNPOT = true;
		}

		removeReference(this.texImage);
		this.texImage = image;
		addReference(this.texImage);
	}

	public void setWrapping(int wrapS, int wrapT)
	{
		if ((wrapS != WRAP_CLAMP && wrapS != WRAP_REPEAT) ||
			(wrapT != WRAP_CLAMP && wrapT != WRAP_REPEAT))
			{ throw new java.lang.IllegalArgumentException("Invalid texture wrap mode"); }

		this.wraps = wrapS;
		this.wrapt = wrapT;
	}

	private static boolean isPowerOfTwo(int value) { return value > 0 && ((value & (value-1)) == 0); }

	@Override
	void updateProperty(int property, float[] value)
	{
		Mobile.log(Mobile.LOG_WARNING, Graphics3D.class.getPackage().getName() + "." + Graphics3D.class.getSimpleName() + ": " + "AnimTrack updating Texture2D property");
		switch (property)
		{
			case AnimationTrack.COLOR:
				int r = Math.max(0, Math.min(255, (int)(value[0] * 255.0f)));
				int g = Math.max(0, Math.min(255, (int)(value[1] * 255.0f)));
				int b = Math.max(0, Math.min(255, (int)(value[2] * 255.0f)));
				this.blendColor = (r << 16) | (g << 8) | b;
				break;
			default:
				super.updateProperty(property, value);
		}
	}

	boolean animTrackCompatible(AnimationTrack track)
	{
		switch (track.getTargetProperty())
		{
			case AnimationTrack.COLOR:
				return true;
			default:
				return super.animTrackCompatible(track);
		}
	}

	// We make some texture coord optimizations in Graphics3D, NPOT
	// breaks them so we must check whether this texture is NPOT.
	boolean isNPOT() { return this.isNPOT; }
}
