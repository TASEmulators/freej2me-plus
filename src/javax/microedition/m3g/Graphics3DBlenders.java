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

// Class containing specialized blenders for Graphics3D blending modes. With this,
// we can set the blend logic to use only once per render call, and execute them
// without any sort of branching on when drawing each pixel.
class Graphics3DBlenders
{
	interface CompositingBlender { int blend(int bg, int fg, int alpha); }

	static class CompositingBlenders
	{
		// This one was turned into a faster path where we don't even allocate
		// this in memory and just apply the source pixel directly in the method
		// itself, also saving a method call in the process.
		//static final CompositingBlender REPLACE = new CompositingBlender()
		//{
		//	public int blend(int bg, int fg, int alpha) { return fg; }
		//};

		static final CompositingBlender ALPHA = new CompositingBlender()
		{
			public int blend(int bg, int fg, int alpha)
			{
				if (alpha <= 0)   { return bg; }
				if (alpha >= 255) { return fg; }

				int invA = 255 - alpha;

				int bgRB = bg & 0x00FF00FF;
				int fgRB = fg & 0x00FF00FF;
				int outRB = ((fgRB * alpha + bgRB * invA) >> 8) & 0x00FF00FF;

				int bgAG = (bg >>> 8) & 0x00FF00FF;
				int fgAG = (fg >>> 8) & 0x00FF00FF;
				int outAG = ((fgAG * alpha + bgAG * invA) >> 8) & 0x00FF00FF;

				return outRB | (outAG << 8);
			}
		};

		static final CompositingBlender ALPHA_ADD = new CompositingBlender()
		{
			public int blend(int bg, int fg, int alpha)
			{
				if (alpha == 0) { return bg; }

				int fgRB = fg & 0x00FF00FF;
				int addRB = ((fgRB * alpha) >> 8) & 0x00FF00FF;

				int fgG = fg & 0x0000FF00;
				int addG = ((fgG * alpha) >> 8) & 0x0000FF00;

				int bgA = bg >>> 24;
				int addA = (alpha * (255 - bgA)) >> 8;

				int sumRB = (bg & 0x00FF00FF) + addRB;
				int sumG  = (bg & 0x0000FF00) + addG;
				int sumA  = bgA + addA;

				int overflowRB = sumRB & 0x01000100;
				int maskRB = (overflowRB - (overflowRB >> 8));
				int outRB = (sumRB | maskRB) & 0x00FF00FF;

				int overflowG = sumG & 0x00010000;
				int maskG = overflowG - (overflowG >> 8);
				int outG = (sumG | maskG) & 0x0000FF00;

				int outA = sumA | -(sumA >> 8);

				return ((outA & 0xFF) << 24) | outRB | outG;
			}
		};

		static final CompositingBlender MODULATE = new CompositingBlender()
		{
			public int blend(int bg, int fg, int alpha)
			{
				int bgRB = bg & 0x00FF00FF;
				int fgRB = fg & 0x00FF00FF;

				int r = (((bgRB >> 16) * (fgRB >> 16)) >> 8) & 0xFF;
				int b = (((bgRB & 0xFF) * (fgRB & 0xFF)) >> 8) & 0xFF;

				int bgAG = (bg >>> 8) & 0x00FF00FF;
				int fgAG = (fg >>> 8) & 0x00FF00FF;
				int a = (((bgAG >> 16) * (fgAG >> 16)) >> 8) & 0xFF;
				int g = (((bgAG & 0xFF) * (fgAG & 0xFF)) >> 8) & 0xFF;

				return (a << 24) | (r << 16) | (g << 8) | b;
			}
		};

		static final CompositingBlender MODULATE_X2 = new CompositingBlender()
		{
			public int blend(int bg, int fg, int alpha)
			{
				int bgA = bg >>> 24, bgR = (bg >> 16) & 0xFF, bgG = (bg >> 8) & 0xFF, bgB = bg & 0xFF;
				int fgA = fg >>> 24, fgR = (fg >> 16) & 0xFF, fgG = (fg >> 8) & 0xFF, fgB = fg & 0xFF;

				int outR = (fgR * bgR) >> 7;
				int outG = (fgG * bgG) >> 7;
				int outB = (fgB * bgB) >> 7;
				int outA = (fgA * bgA) >> 7;

				outR = (outR | -(outR >> 8)) & 0xFF;
				outG = (outG | -(outG >> 8)) & 0xFF;
				outB = (outB | -(outB >> 8)) & 0xFF;
				outA = (outA | -(outA >> 8)) & 0xFF;

				return (outA << 24) | (outR << 16) | (outG << 8) | outB;
			}
		};

		static final CompositingBlender PASSTHROUGH = new CompositingBlender()
		{
			public int blend(int bg, int fg, int alpha) { return bg; }
		};
	}

	// Texturing blend modes

	interface TextureBlender { int blend(int bg, int fg, int texBlendColor); }

	static class TextureBlenders
	{
		static final TextureBlender PASSTHROUGH = new TextureBlender()
		{
			public int blend(int bg, int fg, int texBlendColor) { return bg; }
		};

		// This one was turned into a faster path where we don't even allocate
		// this in memory and just apply the texture pixel directly in the method
		// itself, saving a method call in the process.
		//static final TextureBlender REPLACE_FG = new TextureBlender() {
		//    public int blend(int bg, int fg, int texBlendColor) { return fg; }
		//};

		static final TextureBlender REPLACE_ALPHA = new TextureBlender()
		{
			public int blend(int bg, int fg, int texBlendColor)
			{
				return (fg & 0xFF000000) | (bg & 0x00FFFFFF);
			}
		};

		static final TextureBlender ADD_RGB = new TextureBlender()
		{
			public int blend(int bg, int fg, int texBlendColor)
			{
				int sumRB = (bg & 0x00FF00FF) + (fg & 0x00FF00FF);
				int overRB = (sumRB & 0x01000100) - ((sumRB & 0x01000100) >>> 8);
				int outRB = (sumRB | overRB) & 0x00FF00FF;

				int sumG = (bg & 0x0000FF00) + (fg & 0x0000FF00);
				int overG = (sumG & 0x00010000) - ((sumG & 0x00010000) >>> 8);
				int outG = (sumG | overG) & 0x0000FF00;

				return (bg & 0xFF000000) | outG | outRB;
			}
		};

		static final TextureBlender ADD_RGBA = new TextureBlender()
		{
			public int blend(int bg, int fg, int texBlendColor)
			{
				int outA = (((bg >>> 24) * (fg >>> 24)) + 128) >> 8;

				int sumRB = (bg & 0x00FF00FF) + (fg & 0x00FF00FF);
				int overRB = (sumRB & 0x01000100) - ((sumRB & 0x01000100) >>> 8);
				int outRB = (sumRB | overRB) & 0x00FF00FF;

				int sumG = (bg & 0x0000FF00) + (fg & 0x0000FF00);
				int overG = (sumG & 0x00010000) - ((sumG & 0x00010000) >>> 8);
				int outG = (sumG | overG) & 0x0000FF00;

				return (outA << 24) | outG | outRB;
			}
		};

		static final TextureBlender ALPHA_MUL = new TextureBlender()
		{
			public int blend(int bg, int fg, int texBlendColor)
			{
				int outA = (((bg >>> 24) * (fg >>> 24)) + 128) >> 8;
				return (outA << 24) | (bg & 0x00FFFFFF);
			}
		};

		static final TextureBlender BLEND_RGBA = new TextureBlender()
		{
			public int blend(int bg, int fg, int texBlendColor)
			{
				int outA = (((bg >>> 24) * (fg >>> 24)) + 128) >> 8;

				int fR = (bg >> 16) & 0xFF, cR = (texBlendColor >> 16) & 0xFF, tR = (fg >> 16) & 0xFF;
				int fG = (bg >>  8) & 0xFF, cG = (texBlendColor >>  8) & 0xFF, tG = (fg >>  8) & 0xFF;
				int fB =  bg        & 0xFF, cB =  texBlendColor        & 0xFF, tB =  fg        & 0xFF;

				int outR = (fR + (((cR - fR) * tR + 128) >> 8)) & 0xFF;
				int outG = (fG + (((cG - fG) * tG + 128) >> 8)) & 0xFF;
				int outB = (fB + (((cB - fB) * tB + 128) >> 8)) & 0xFF;

				return (outA << 24) | (outR << 16) | (outG << 8) | outB;
			}
		};

		static final TextureBlender BLEND_RGB = new TextureBlender()
		{
			public int blend(int bg, int fg, int texBlendColor)
			{
				int fR = (bg >> 16) & 0xFF, cR = (texBlendColor >> 16) & 0xFF, tR = (fg >> 16) & 0xFF;
				int fG = (bg >>  8) & 0xFF, cG = (texBlendColor >>  8) & 0xFF, tG = (fg >>  8) & 0xFF;
				int fB =  bg        & 0xFF, cB =  texBlendColor        & 0xFF, tB =  fg        & 0xFF;

				int outR = fR + (((cR - fR) * tR + 128) >> 8);
				int outG = fG + (((cG - fG) * tG + 128) >> 8);
				int outB = fB + (((cB - fB) * tB + 128) >> 8);

				return (bg & 0xFF000000) | (outR << 16) | (outG << 8) | outB;
			}
		};

		static final TextureBlender DECAL_RGBA = new TextureBlender()
		{
			public int blend(int bg, int fg, int texBlendColor)
			{
				int tA = fg >>> 24;

				int fRB = bg & 0x00FF00FF, tRB = fg & 0x00FF00FF;
				int outRB = (fRB + ((((tRB - fRB) * tA) >> 8) & 0x00FF00FF)) & 0x00FF00FF;

				int fAG = (bg >>> 8) & 0x00FF00FF, tAG = (fg >>> 8) & 0x00FF00FF;
				int outAG = (fAG + ((((tAG - fAG) * tA) >> 8) & 0x00FF00FF)) & 0x00FF00FF;

				return (bg & 0xFF000000) | ((outRB | (outAG << 8)) & 0x00FFFFFF);
			}
		};

		static final TextureBlender MODULATE_RGBA = new TextureBlender()
		{
			public int blend(int bg, int fg, int texBlendColor)
			{
				int outR = (((bg >> 16) & 0xFF) * ((fg >> 16) & 0xFF) + 128) >> 8;
				int outG = (((bg >>  8) & 0xFF) * ((fg >>  8) & 0xFF) + 128) >> 8;
				int outB = (( bg        & 0xFF) * ( fg        & 0xFF) + 128) >> 8;
				int outA = ((bg >>> 24) * (fg >>> 24) + (bg >>> 24)) >> 8;

				return (outA << 24) | (outR << 16) | (outG << 8) | outB;
			}
		};

		static final TextureBlender MODULATE_RGB = new TextureBlender()
		{
			public int blend(int bg, int fg, int texBlendColor)
			{
				int outR = ((bg >> 16) & 0xFF) * ((fg >> 16) & 0xFF);
				int outG = ((bg >>  8) & 0xFF) * ((fg >>  8) & 0xFF);
				int outB = ( bg        & 0xFF) * ( fg        & 0xFF);

				return (bg & 0xFF000000)
					 | ((outR & 0xFF00) << 8)
					 |  (outG & 0xFF00)
					 |  (outB >> 8);
			}
		};
	}
}
