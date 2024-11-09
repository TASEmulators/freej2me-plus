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
package	javax.microedition.lcdui.game;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import org.recompile.mobile.Mobile;

public class Sprite extends Layer
{

	public static final int TRANS_NONE = 0;
	public static final int TRANS_ROT90 = 5;
	public static final int TRANS_ROT180 = 3;
	public static final int TRANS_ROT270 = 6;
	public static final int TRANS_MIRROR = 2;
	public static final int TRANS_MIRROR_ROT90 = 7;
	public static final int TRANS_MIRROR_ROT180 = 1;
	public static final int TRANS_MIRROR_ROT270 = 4;
	private static final int INVERTED_AXES = 0x4;
	private static final int X_FLIP = 0x2;
	private static final int Y_FLIP = 0x1;
	private static final int ALPHA_BITMASK = 0xff000000;
	private static final int FULLY_OPAQUE_ALPHA = 0xff000000;

	private Image sourceImage;
	private int numberFrames;
	private int[] frameCoordsX;
	private int[] frameCoordsY;
	private int srcFrameWidth;
	private int srcFrameHeight;
	private int[] sequence;
	private int sequenceIndex;
	private boolean customSequenceDefined;
	private int dRefX;
	private int dRefY;
	private int collisionRectX;
	private int collisionRectY;
	private int collisionRectWidth;
	private int collisionRectHeight;
	private int t_currentTransformation;
	private int t_collisionRectX;
	private int t_collisionRectY;
	private int t_collisionRectWidth;
	private int t_collisionRectHeight;


	public Sprite(Image image)
	{
		super(image.getWidth(), image.getHeight());

		initializeFrames(image, image.getWidth(), image.getHeight(), false);
		initCollisionRectBounds();
		setTransform(TRANS_NONE);
	}

	public Sprite(Image image, int frameWidth, int frameHeight)
	{
		super(frameWidth, frameHeight);

		if ((frameWidth < 1 || frameHeight < 1) || ((image.getWidth() % frameWidth) != 0) || ((image.getHeight() % frameHeight) != 0))
			{ throw new IllegalArgumentException(); }

		initializeFrames(image, frameWidth, frameHeight, false);
		initCollisionRectBounds();
		setTransform(TRANS_NONE);
	}

	public Sprite(Sprite s)
	{
		super(s != null ? s.getWidth() : 0, s != null ? s.getHeight() : 0);

		if (s == null) { throw new NullPointerException(); }

		this.sourceImage = Image.createImage(s.sourceImage);
		this.numberFrames = s.numberFrames;
		this.frameCoordsX = new int[this.numberFrames];
		this.frameCoordsY = new int[this.numberFrames];

		System.arraycopy(s.frameCoordsX, 0, this.frameCoordsX, 0, s.getRawFrameCount());
		System.arraycopy(s.frameCoordsY, 0, this.frameCoordsY, 0, s.getRawFrameCount());

		this.x = s.getX();
		this.y = s.getY();

		this.dRefX = s.dRefX;
		this.dRefY = s.dRefY;

		this.collisionRectX = s.collisionRectX;
		this.collisionRectY = s.collisionRectY;
		this.collisionRectWidth = s.collisionRectWidth;
		this.collisionRectHeight = s.collisionRectHeight;

		this.srcFrameWidth = s.srcFrameWidth;
		this.srcFrameHeight = s.srcFrameHeight;

		setTransform(s.t_currentTransformation);
		this.setVisible(s.isVisible());

		this.sequence = new int[s.getFrameSequenceLength()];
		this.setFrameSequence(s.sequence);
		this.setFrame(s.getFrame());

		x = s.getRefPixelX() - getTransformedPos(dRefX, t_currentTransformation, true);
		y = s.getRefPixelY() - getTransformedPos(dRefY, t_currentTransformation, false);
	}

	public int getRefPixelX() { return (this.x + getTransformedPos(dRefX, this.t_currentTransformation, true)); }

	public int getRefPixelY() { return (this.y + getTransformedPos(dRefY, this.t_currentTransformation, false)); }

	public void setFrame(int sequenceIndex)
	{
		if (sequenceIndex < 0 || sequenceIndex >= sequence.length) { throw new IndexOutOfBoundsException(); }
		this.sequenceIndex = sequenceIndex;
	}

	public final int getFrame() { return sequenceIndex; }

	public int getRawFrameCount() { return numberFrames; }

	public int getFrameSequenceLength() { return sequence.length; }

	public void nextFrame() { sequenceIndex = (sequenceIndex + 1) % sequence.length; }

	public void prevFrame()
	{
		if (sequenceIndex == 0) { sequenceIndex = sequence.length - 1; }
		else { sequenceIndex--; }
	}

	@Override
	public final void paint(Graphics g)
	{
		if (g == null) { throw new NullPointerException(); }

		if (visible)
		{
			g.drawRegion(sourceImage,
					frameCoordsX[sequence[sequenceIndex]],
					frameCoordsY[sequence[sequenceIndex]],
					srcFrameWidth,
					srcFrameHeight,
					t_currentTransformation,
					this.x,
					this.y,
					Graphics.TOP | Graphics.LEFT);
		}

	}

	public void setFrameSequence(int sequence[])
	{
		if (sequence == null)
		{
			sequenceIndex = 0;
			customSequenceDefined = false;
			this.sequence = new int[numberFrames];
			for (int i = 0; i < numberFrames; i++) { this.sequence[i] = i; }
			return;
		}

		if (sequence.length < 1) { throw new IllegalArgumentException(); }

		for (int aSequence : sequence) { if (aSequence < 0 || aSequence >= numberFrames) { throw new ArrayIndexOutOfBoundsException(); } }
		customSequenceDefined = true;
		this.sequence = new int[sequence.length];
		System.arraycopy(sequence, 0, this.sequence, 0, sequence.length);
		sequenceIndex = 0;
	}

	public void setImage(Image img, int frameWidth, int frameHeight)
	{
		// if image is null image.getWidth() will throw NullPointerException
		if ((frameWidth < 1 || frameHeight < 1) || ((img.getWidth() % frameWidth) != 0) || ((img.getHeight() % frameHeight) != 0))
			{ throw new IllegalArgumentException();}

		final int noOfFrames = (img.getWidth() / frameWidth) * (img.getHeight() / frameHeight);

		boolean maintainCurFrame = true;

		if (noOfFrames < numberFrames)
		{
			maintainCurFrame = false;
			customSequenceDefined = false;
		}

		if (!((srcFrameWidth == frameWidth) && (srcFrameHeight == frameHeight)))
		{

			int oldX = this.x + getTransformedPos(dRefX, this.t_currentTransformation, true);
			int oldY = this.y + getTransformedPos(dRefY, this.t_currentTransformation, false);

			setWidth(frameWidth);
			setHeight(frameHeight);

			initializeFrames(img, frameWidth, frameHeight, maintainCurFrame);
			initCollisionRectBounds();

			this.x = oldX - getTransformedPos(dRefX, this.t_currentTransformation, true);
			this.y = oldY - getTransformedPos(dRefY, this.t_currentTransformation, false);
			computeTransformedBounds(this.t_currentTransformation);

		}
		else { initializeFrames(img, frameWidth, frameHeight, maintainCurFrame); }
	}

	public void defineCollisionRectangle(int x, int y, int width, int height)
	{
		if (width < 0 || height < 0) { throw new IllegalArgumentException(); }

		collisionRectX = x;
		collisionRectY = y;
		collisionRectWidth = width;
		collisionRectHeight = height;

		setTransform(t_currentTransformation);
	}

	public void setTransform(int transform)
	{
		this.x = this.x + getTransformedPos(dRefX, this.t_currentTransformation, true) - getTransformedPos(dRefX, transform, true);
		this.y = this.y + getTransformedPos(dRefY, this.t_currentTransformation, false) - getTransformedPos(dRefY, transform, false);

		computeTransformedBounds(transform);
		t_currentTransformation = transform;
	}

	/* All CollidesWith methods have been rewritten, but i couldn't find a jar that actually uses them yet, so the debug entry messages will remain in place */
	public final boolean collidesWith(Sprite s, boolean pixelLevel) 
	{
		Mobile.log(Mobile.LOG_WARNING, Sprite.class.getPackage().getName() + "." + Sprite.class.getSimpleName() + ": " + "CollidesWith A");
		if (!(s.visible && this.visible)) { return false; }
	
		Rect thisRect = getCollisionRect(this);
		Rect otherRect = getCollisionRect(s);
	
		if (intersectRect(thisRect, otherRect)) 
			{ return pixelLevel ? pixelCollision(thisRect, otherRect, s.sourceImage, s.t_currentTransformation) : true; }
		
		return false;
	}
	
	public final boolean collidesWith(TiledLayer t, boolean pixelLevel) 
	{
		Mobile.log(Mobile.LOG_WARNING, Sprite.class.getPackage().getName() + "." + Sprite.class.getSimpleName() + ": " + "CollidesWith B");
		if (!(t.visible && this.visible)) { return false; }
	
		Rect thisRect = getCollisionRect(this);
		Rect layerRect = new Rect(t.x, t.y, t.x + t.width, t.y + t.height);
	
		if (!intersectRect(thisRect, layerRect)) { return false; }
	
		int tW = t.getCellWidth();
		int tH = t.getCellHeight();
	
		int startCol = Math.max(0, (thisRect.left - t.x) / tW);
		int endCol = Math.min(t.getColumns() - 1, (thisRect.right - 1 - t.x) / tW);
		int startRow = Math.max(0, (thisRect.top - t.y) / tH);
		int endRow = Math.min(t.getRows() - 1, (thisRect.bottom - 1 - t.y) / tH);
	
		for (int row = startRow; row <= endRow; row++) 
		{
			for (int col = startCol; col <= endCol; col++) 
			{
				if (t.getCell(col, row) != 0) 
				{ 
					if (!pixelLevel || checkTileCollision(thisRect, t, col, row, tW, tH)) { return true; } 
				}
			}
		}
		return false;
	}
	
	public final boolean collidesWith(Image image, int x, int y, boolean pixelLevel) 
	{
		Mobile.log(Mobile.LOG_WARNING, Sprite.class.getPackage().getName() + "." + Sprite.class.getSimpleName() + ": " + "CollidesWith C");
		if (!visible) { return false; }
	
		Rect thisRect = getCollisionRect(this);
		Rect imageRect = new Rect(x, y, x + image.getWidth(), y + image.getHeight());
	
		if (intersectRect(thisRect, imageRect)) 
			{ return pixelLevel ? pixelCollision(thisRect, imageRect, image, Sprite.TRANS_NONE) : true;}
		return false;
	}
	
	private Rect getCollisionRect(Sprite s) 
	{
		int left = s.x + s.t_collisionRectX;
		int top = s.y + s.t_collisionRectY;
		int right = left + s.t_collisionRectWidth;
		int bottom = top + s.t_collisionRectHeight;
		return new Rect(left, top, right, bottom);
	}
	
	private boolean intersectRect(Rect r1, Rect r2)
		{ return !(r2.left >= r1.right || r2.right <= r1.left || r2.top >= r1.bottom || r2.bottom <= r1.top); }
	
	private boolean pixelCollision(Rect r1, Rect r2, Image otherImage, int otherTransformation) 
	{
		int intersectLeft = Math.max(r1.left, r2.left);
		int intersectTop = Math.max(r1.top, r2.top);
		int intersectRight = Math.min(r1.right, r2.right);
		int intersectBottom = Math.min(r1.bottom, r2.bottom);
		int intersectWidth = Math.abs(intersectRight - intersectLeft);
		int intersectHeight = Math.abs(intersectBottom - intersectTop);
	
		int thisImageXOffset = getImageTopLeft(intersectLeft, intersectTop, intersectRight, intersectBottom, true);
		int thisImageYOffset = getImageTopLeft(intersectLeft, intersectTop, intersectRight, intersectBottom, false);
		int otherImageXOffset = getImageTopLeft(intersectLeft, intersectTop, intersectRight, intersectBottom, true);
		int otherImageYOffset = getImageTopLeft(intersectLeft, intersectTop, intersectRight, intersectBottom, false);
	
		return doPixelCollision(thisImageXOffset, thisImageYOffset, otherImageXOffset, otherImageYOffset,
				this.sourceImage, this.t_currentTransformation, otherImage, otherTransformation,
				intersectWidth, intersectHeight);
	}
	
	private boolean checkTileCollision(Rect spriteRect, TiledLayer t, int col, int row, int tW, int tH) 
	{
		int cellLeft = col * tW + t.x;
		int cellTop = row * tH + t.y;
		int cellRight = cellLeft + tW;
		int cellBottom = cellTop + tH;
	
		int intersectLeft = Math.max(spriteRect.left, cellLeft);
		int intersectTop = Math.max(spriteRect.top, cellTop);
		int intersectRight = Math.min(spriteRect.right, cellRight);
		int intersectBottom = Math.min(spriteRect.bottom, cellBottom);
	
		if (intersectLeft < intersectRight && intersectTop < intersectBottom) 
		{
			int intersectWidth = intersectRight - intersectLeft;
			int intersectHeight = intersectBottom - intersectTop;
			int thisImageXOffset = getImageTopLeft(intersectLeft, intersectTop, intersectRight, intersectBottom, true);
			int thisImageYOffset = getImageTopLeft(intersectLeft, intersectTop, intersectRight, intersectBottom, false);
			int tileIndex = t.getCell(col, row);
			int image2XOffset = t.tileSetX[tileIndex] + (intersectLeft - cellLeft);
			int image2YOffset = t.tileSetY[tileIndex] + (intersectTop - cellTop);
	
			return doPixelCollision(thisImageXOffset, thisImageYOffset, image2XOffset, image2YOffset,
					this.sourceImage, this.t_currentTransformation, t.image, TRANS_NONE,
					intersectWidth, intersectHeight);
		}
		return false;
	}

	private void initializeFrames(Image image, int fWidth, int fHeight, boolean maintainCurFrame)
	{
		final int imageW = image.getWidth();
		final int imageH = image.getHeight();

		final int numHorizontalFrames = imageW / fWidth;
		final int numVerticalFrames = imageH / fHeight;

		sourceImage = image;

		srcFrameWidth = fWidth;
		srcFrameHeight = fHeight;

		numberFrames = numHorizontalFrames * numVerticalFrames;

		frameCoordsX = new int[numberFrames];
		frameCoordsY = new int[numberFrames];

		if (!maintainCurFrame) { sequenceIndex = 0; }
		if (!customSequenceDefined) { sequence = new int[numberFrames]; }

		int currentFrame = 0;

		for (int yy = 0; yy < imageH; yy += fHeight)
		{
			for (int xx = 0; xx < imageW; xx += fWidth)
			{

				frameCoordsX[currentFrame] = xx;
				frameCoordsY[currentFrame] = yy;

				if (!customSequenceDefined) { sequence[currentFrame] = currentFrame; }
				currentFrame++;
			}
		}
	}

	private void initCollisionRectBounds()
	{
		collisionRectX = 0;
		collisionRectY = 0;

		collisionRectWidth = this.width;
		collisionRectHeight = this.height;
	}

	private boolean intersectRect(int r1x1, int r1y1, int r1x2, int r1y2, int r2x1, int r2y1, int r2x2, int r2y2)
	{
		if (r2x1 >= r1x2 || r2y1 >= r1y2 || r2x2 <= r1x1 || r2y2 <= r1y1) { return false; }
		else { return true; }
	}

	private static boolean doPixelCollision(int image1XOffset, int image1YOffset, int image2XOffset, int image2YOffset,
		Image image1, int transform1, Image image2, int transform2, int width, int height) 
	{
		Mobile.log(Mobile.LOG_WARNING, Sprite.class.getPackage().getName() + "." + Sprite.class.getSimpleName() + ": " + "TiledLayer: Per-Pixel Collision Check!");

		final int[] argbData1 = getARGBData(image1, image1XOffset, image1YOffset, transform1, width, height);
		final int[] argbData2 = getARGBData(image2, image2XOffset, image2YOffset, transform2, width, height);

		return checkPixelCollision(argbData1, argbData2, width, height);
	}

	private static int[] getARGBData(Image image, int xOffset, int yOffset, int transform, int width, int height) 
	{
		int startY, xIncr, yIncr, numPixels = height * width;
		int[] argbData = new int[numPixels];

		if (0x0 != (transform & INVERTED_AXES)) 
		{
			if (0x0 != (transform & Y_FLIP)) 
			{
				xIncr = -(height);
				startY = numPixels - height;
			} 
			else 
			{
				xIncr = height;
				startY = 0;
			}

			if (0x0 != (transform & X_FLIP)) {
				yIncr = -1;
				startY += (height - 1);
			} else { yIncr = +1; }

			image.getRGB(argbData, 0, height, xOffset, yOffset, height, width);
		} 
		else 
		{
			if (0x0 != (transform & Y_FLIP))
			{
				startY = numPixels - width;
				yIncr = -(width);
			}
			else
			{
				startY = 0;
				yIncr = width;
			}

			if (0x0 != (transform & X_FLIP))
			{
				xIncr = -1;
				startY += (width - 1);
			}
			else { xIncr = +1; }

			image.getRGB(argbData, 0, width, xOffset, yOffset, width, height);
		}

		return argbData;
	}

	private static boolean checkPixelCollision(int[] argbData1, int[] argbData2, int width, int height) 
	{
		for (int i = 0; i < width * height; i++) 
		{
			if (((argbData1[i] & ALPHA_BITMASK) == FULLY_OPAQUE_ALPHA) && ((argbData2[i] & ALPHA_BITMASK) == FULLY_OPAQUE_ALPHA)) { return true; }
		}

		return false;
	}

	private int getImageTopLeft(int x1, int y1, int x2, int y2, boolean isX) 
	{
		int ret = 0;
	
		switch (this.t_currentTransformation)
		{
			case TRANS_NONE:
			case TRANS_MIRROR_ROT180:
				ret = isX ? x1 - this.x : y1 - this.y;
				break;
			case TRANS_MIRROR:
			case TRANS_ROT180:
				ret = isX ? (this.x + this.width) - x2 : (this.y + this.height) - y2;
				break;
			case TRANS_ROT90:
			case TRANS_MIRROR_ROT270:
				ret = isX ? y1 - this.y : (this.x + this.width) - x2;
				break;
			case TRANS_ROT270:
			case TRANS_MIRROR_ROT90:
				ret = isX ? (this.y + this.height) - y2 : x1 - this.x;
				break;
			default:
				return ret;
		}
	
		ret += isX ? frameCoordsX[sequence[sequenceIndex]] : frameCoordsY[sequence[sequenceIndex]];
	
		return ret;
	}

	private void computeTransformedBounds(int transform) 
	{
		Mobile.log(Mobile.LOG_DEBUG, Sprite.class.getPackage().getName() + "." + Sprite.class.getSimpleName() + ": " + "TiledLayer: ComputeTransBounds!");
		switch (transform) 
		{
			case TRANS_NONE:
				t_collisionRectX = collisionRectX;
				t_collisionRectY = collisionRectY;
				break;
	
			case TRANS_MIRROR:
				t_collisionRectX = srcFrameWidth - (collisionRectX + collisionRectWidth);
				t_collisionRectY = collisionRectY;
				break;
	
			case TRANS_MIRROR_ROT180:
				t_collisionRectX = collisionRectX;
				t_collisionRectY = srcFrameHeight - (collisionRectY + collisionRectHeight);
				break;
	
			case TRANS_ROT90:
				t_collisionRectX = srcFrameHeight - (collisionRectHeight + collisionRectY);
				t_collisionRectY = collisionRectX;
				break;
	
			case TRANS_ROT180:
				t_collisionRectX = srcFrameWidth - (collisionRectWidth + collisionRectX);
				t_collisionRectY = srcFrameHeight - (collisionRectHeight + collisionRectY);
				break;
	
			case TRANS_ROT270:
				t_collisionRectX = collisionRectY;
				t_collisionRectY = srcFrameWidth - (collisionRectWidth + collisionRectX);
				break;
	
			case TRANS_MIRROR_ROT90:
				t_collisionRectX = srcFrameHeight - (collisionRectHeight + collisionRectY);
				t_collisionRectY = srcFrameWidth - (collisionRectWidth + collisionRectX);
				break;
	
			case TRANS_MIRROR_ROT270:
				t_collisionRectX = collisionRectY;
				t_collisionRectY = collisionRectX;
				break;
	
			default:
				throw new IllegalArgumentException();
		}
	
		t_collisionRectWidth = (transform % 2 == 0) ? collisionRectWidth : collisionRectHeight;
		t_collisionRectHeight = (transform % 2 == 0) ? collisionRectHeight : collisionRectWidth;
	
		this.width = (transform % 2 == 0) ? srcFrameWidth : srcFrameHeight;
		this.height = (transform % 2 == 0) ? srcFrameHeight : srcFrameWidth;
	}

	private int getTransformedPos(int coord, int transform, boolean isX)
	{
		switch (transform)
		{
			case TRANS_NONE:
				return coord;
			case TRANS_MIRROR:
				return isX ? srcFrameWidth - coord - 1 : coord;
			case TRANS_MIRROR_ROT180:
				return isX ? coord : srcFrameHeight - coord - 1;
			case TRANS_ROT90:
				return isX ? srcFrameHeight - coord - 1 : coord;
			case TRANS_ROT180:
				return isX ? srcFrameWidth - coord - 1 : srcFrameHeight - coord - 1;
			case TRANS_ROT270:
				return isX ? coord : srcFrameWidth - coord - 1;
			case TRANS_MIRROR_ROT90:
				return isX ? srcFrameHeight - coord - 1 : srcFrameWidth - coord - 1;
			case TRANS_MIRROR_ROT270:
				return isX ? coord : srcFrameWidth - coord - 1;
			default:
				return 0;
		}
	}

	private class Rect 
	{
		int left, top, right, bottom;

		Rect(int left, int top, int right, int bottom) 
		{
			this.left = left;
			this.top = top;
			this.right = right;
			this.bottom = bottom;
		}
	}
}