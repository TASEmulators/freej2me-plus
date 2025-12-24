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
	private int currentTransform;

	public Sprite(Image image)
	{
		super(image.getWidth(), image.getHeight());

		initializeFrames(image, image.getWidth(), image.getHeight(), false);
		setTransform(TRANS_NONE);
		defineCollisionRectangle(0, 0, width, height);
	}

	public Sprite(Image image, int frameWidth, int frameHeight)
	{
		super(frameWidth, frameHeight);

		if ((frameWidth < 1 || frameHeight < 1) || ((image.getWidth() % frameWidth) != 0) || ((image.getHeight() % frameHeight) != 0))
			{ throw new IllegalArgumentException(); }

		initializeFrames(image, frameWidth, frameHeight, false);
		setTransform(TRANS_NONE);
		defineCollisionRectangle(0, 0, width, height);
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

		setTransform(s.currentTransform);
		this.setVisible(s.isVisible());

		this.sequence = new int[s.getFrameSequenceLength()];
		this.setFrameSequence(s.sequence);
		this.setFrame(s.getFrame());

		this.setRefPixelPosition(s.getRefPixelX(), s.getRefPixelY());
	}

	public void defineReferencePixel(int x, int y) 
	{
		dRefX = x;
		dRefY = y;
	}

	public void setRefPixelPosition(int x, int y) 
	{
		this.x = x - getTransformedPos(dRefX, dRefY, this.currentTransform, true);
		this.y = y - getTransformedPos(dRefX, dRefY, this.currentTransform, false);
	}

	public int getRefPixelX() { return (this.x + getTransformedPos(dRefX, dRefY, this.currentTransform, true)); }

	public int getRefPixelY() { return (this.y + getTransformedPos(dRefX, dRefY, this.currentTransform, false)); }

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
					currentTransform,
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

			int oldX = this.x + getTransformedPos(dRefX, dRefY, this.currentTransform, true);
			int oldY = this.y + getTransformedPos(dRefX, dRefY, this.currentTransform, false);

			setWidth(frameWidth);
			setHeight(frameHeight);

			initializeFrames(img, frameWidth, frameHeight, maintainCurFrame);
			defineCollisionRectangle(0, 0, width, height);

			this.x = oldX - getTransformedPos(dRefX, dRefY, this.currentTransform, true);
			this.y = oldY - getTransformedPos(dRefX, dRefY, this.currentTransform, false);

			if(this.currentTransform == TRANS_MIRROR_ROT270 ||
				this.currentTransform == TRANS_MIRROR_ROT90 ||
				this.currentTransform == TRANS_ROT270 ||
				this.currentTransform == TRANS_ROT90)
			{
				this.width = srcFrameHeight;
				this.height = srcFrameWidth;
			}
			else
			{
				this.width = srcFrameWidth;
				this.height = srcFrameHeight;
			}
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

		setTransform(currentTransform);
	}

	public void setTransform(int transform)
	{
		this.x = this.x + getTransformedPos(dRefX, dRefY, this.currentTransform, true) - getTransformedPos(dRefX, dRefY, transform, true);
		this.y = this.y + getTransformedPos(dRefX, dRefY, this.currentTransform, false) - getTransformedPos(dRefX, dRefY, transform, false);

		if(transform == TRANS_MIRROR_ROT270 ||
			transform == TRANS_MIRROR_ROT90 ||
			transform == TRANS_ROT270 ||
			transform == TRANS_ROT90)
		{
			this.width = srcFrameHeight;
			this.height = srcFrameWidth;
		}
		else
		{
			this.width = srcFrameWidth;
			this.height = srcFrameHeight;
		}

		currentTransform = transform;
	}

	public final boolean collidesWith(Sprite s, boolean pixelLevel) 
	{
		if (!(s.visible && this.visible)) { return false; }

		int otherLeft = s.x + s.collisionRectX;
		int otherTop = s.y + s.collisionRectY;
		int otherRight = otherLeft + s.collisionRectWidth;
		int otherBottom = otherTop + s.collisionRectHeight;

		int left = this.x + this.collisionRectX;
		int top = this.y + this.collisionRectY;
		int right = left + this.collisionRectWidth;
		int bottom = top + this.collisionRectHeight;

		if (intersects(otherLeft, otherTop, otherRight, otherBottom, left, top, right, bottom)) 
		{
			if (pixelLevel) 
			{
				if (this.collisionRectX < 0) { left = this.x; }
				if (this.collisionRectY < 0) { top = this.y; }
				if ((this.collisionRectX + this.collisionRectWidth) > this.width) { right = this.x + this.width; }
				if ((this.collisionRectY + this.collisionRectHeight) > this.height) { bottom = this.y + this.height; }

				if (s.collisionRectX < 0) { otherLeft = s.x; }
				if (s.collisionRectY < 0) { otherTop = s.y; }
				if ((s.collisionRectX + s.collisionRectWidth) > s.width) { otherRight = s.x + s.width; }
				if ((s.collisionRectY + s.collisionRectHeight) > s.height) { otherBottom = s.y + s.height; }

				if (!intersects(otherLeft, otherTop, otherRight, otherBottom, left, top, right, bottom)) { return false; }

				int intersectLeft = (left < otherLeft) ? otherLeft : left;
				int intersectTop = (top < otherTop) ? otherTop : top;
				int intersectRight = (right < otherRight) ? right : otherRight;
				int intersectBottom = (bottom < otherBottom) ? bottom : otherBottom;
				int intersectWidth = Math.abs(intersectRight - intersectLeft);
				int intersectHeight = Math.abs(intersectBottom - intersectTop);

				int thisImageXOffset = getImageTopLeft(intersectLeft, intersectTop, intersectRight, intersectBottom, true);
				int thisImageYOffset = getImageTopLeft(intersectLeft, intersectTop, intersectRight, intersectBottom, false);
				int otherImageXOffset = s.getImageTopLeft(intersectLeft, intersectTop, intersectRight, intersectBottom, true);
				int otherImageYOffset = s.getImageTopLeft(intersectLeft, intersectTop, intersectRight, intersectBottom, false);

				return checkPixCollision(thisImageXOffset, thisImageYOffset,
						otherImageXOffset, otherImageYOffset,
						this.sourceImage,
						this.currentTransform,
						s.sourceImage,
						s.currentTransform,
						intersectWidth, intersectHeight);
			} 
			else { return true; }
		}
		return false;
	}

	public final boolean collidesWith(TiledLayer t, boolean pixelLevel) 
	{
		if (!(t.visible && this.visible)) { return false; }
		int tLx1 = t.x;
		int tLy1 = t.y;
		int tLx2 = tLx1 + t.width;
		int tLy2 = tLy1 + t.height;

		int sx1 = this.x + this.collisionRectX;
		int sy1 = this.y + this.collisionRectY;
		int sx2 = sx1 + this.collisionRectWidth;
		int sy2 = sy1 + this.collisionRectHeight;

		if (!intersects(tLx1, tLy1, tLx2, tLy2, sx1, sy1, sx2, sy2)) { return false; }

		int tW = t.getCellWidth();
		int tH = t.getCellHeight();

		int tNumCols = t.getColumns();
		int tNumRows = t.getRows();

		int startCol = (sx1 <= tLx1) ? 0 : (sx1 - tLx1) / tW;
		int startRow = (sy1 <= tLy1) ? 0 : (sy1 - tLy1) / tH;
		int endCol = (sx2 < tLx2) ? ((sx2 - 1 - tLx1) / tW) : tNumCols - 1;
		int endRow = (sy2 < tLy2) ? ((sy2 - 1 - tLy1) / tH) : tNumRows - 1;

		if (!pixelLevel) 
		{
			for (int row = startRow; row <= endRow; row++) 
			{
				for (int col = startCol; col <= endCol; col++) 
				{
					if (t.getCell(col, row) != 0) { return true; }
				}
			}
			return false;
		} 
		else 
		{
			if (this.collisionRectX < 0) { sx1 = this.x; }
			if (this.collisionRectY < 0) { sy1 = this.y; }
			if ((this.collisionRectX + this.collisionRectWidth) > this.width) { sx2 = this.x + this.width; }
			if ((this.collisionRectY + this.collisionRectHeight) > this.height) { sy2 = this.y + this.height; }

			if (!intersects(tLx1, tLy1, tLx2, tLy2, sx1, sy1, sx2, sy2)) { return false; }

			startCol = (sx1 <= tLx1) ? 0 : (sx1 - tLx1) / tW;
			startRow = (sy1 <= tLy1) ? 0 : (sy1 - tLy1) / tH;
			endCol = (sx2 < tLx2) ? ((sx2 - 1 - tLx1) / tW) : tNumCols - 1;
			endRow = (sy2 < tLy2) ? ((sy2 - 1 - tLy1) / tH) : tNumRows - 1;

			int cellTop = startRow * tH + tLy1;
			int cellBottom = cellTop + tH;

			int tileIndex;

			for (int row = startRow; row <= endRow; row++, cellTop += tH, cellBottom += tH) 
			{
				int cellLeft = startCol * tW + tLx1;
				int cellRight = cellLeft + tW;

				for (int col = startCol; col <= endCol; col++, cellLeft += tW, cellRight += tW) 
				{
					tileIndex = t.getCell(col, row);

					if (tileIndex != 0) 
					{
						int intersectLeft = (sx1 < cellLeft) ? cellLeft : sx1;
						int intersectTop = (sy1 < cellTop) ? cellTop : sy1;
						int intersectRight = (sx2 < cellRight) ? sx2 : cellRight;
						int intersectBottom = (sy2 < cellBottom) ? sy2 : cellBottom;

						if (intersectLeft > intersectRight || intersectTop > intersectBottom) { return false; }

						int intersectWidth = intersectRight - intersectLeft;
						int intersectHeight = intersectBottom - intersectTop;

						int image1XOffset = getImageTopLeft(intersectLeft, intersectTop, intersectRight, intersectBottom, true);
						int image1YOffset = getImageTopLeft(intersectLeft, intersectTop, intersectRight, intersectBottom, false);
						int image2XOffset = t.tileSetX[tileIndex] + (intersectLeft - cellLeft);
						int image2YOffset = t.tileSetY[tileIndex] + (intersectTop - cellTop);

						if (checkPixCollision(image1XOffset,
								image1YOffset,
								image2XOffset,
								image2YOffset,
								this.sourceImage,
								this.currentTransform,
								t.image,
								TRANS_NONE,
								intersectWidth, intersectHeight)) { return true; }
					}
				}
			}
			return false;
		}
	}

	public final boolean collidesWith(Image image, int x, int y, boolean pixelLevel) 
	{
		if (!(visible)) { return false; }

		int otherLeft = x;
		int otherTop = y;
		int otherRight = x + image.getWidth();
		int otherBottom = y + image.getHeight();

		int left = x + collisionRectX;
		int top = y + collisionRectY;
		int right = left + collisionRectWidth;
		int bottom = top + collisionRectHeight;

		if (intersects(otherLeft, otherTop, otherRight, otherBottom, left, top, right, bottom)) 
		{
			if (pixelLevel) 
			{
				if (this.collisionRectX < 0) { left = this.x; }
				if (this.collisionRectY < 0) { top = this.y; }
				if ((this.collisionRectX + this.collisionRectWidth) > this.width) { right = this.x + this.width; }
				if ((this.collisionRectY + this.collisionRectHeight) > this.height) { bottom = this.y + this.height; }

				if (!intersects(otherLeft, otherTop, otherRight, otherBottom, left, top, right, bottom)) { return false; }

				int intersectLeft = (left < otherLeft) ? otherLeft : left;
				int intersectTop = (top < otherTop) ? otherTop : top;

				int intersectRight = (right < otherRight) ? right : otherRight;
				int intersectBottom = (bottom < otherBottom) ? bottom : otherBottom;

				int intersectWidth = Math.abs(intersectRight - intersectLeft);
				int intersectHeight = Math.abs(intersectBottom - intersectTop);

				int thisImageXOffset = getImageTopLeft(intersectLeft,
						intersectTop,
						intersectRight,
						intersectBottom, true);

				int thisImageYOffset = getImageTopLeft(intersectLeft,
						intersectTop,
						intersectRight,
						intersectBottom, false);

				int otherImageXOffset = intersectLeft - x;
				int otherImageYOffset = intersectTop - y;

				return checkPixCollision(thisImageXOffset, thisImageYOffset,
						otherImageXOffset, otherImageYOffset,
						this.sourceImage,
						this.currentTransform,
						image,
						Sprite.TRANS_NONE,
						intersectWidth, intersectHeight);

			}
			else { return true; }
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

	private boolean intersects(int rect1x1, int rect1y1, int rect1x2,
		int rect1y2, int rect2x1, int rect2y1, int rect2x2, int rect2y2) 
	{
		// If one is to the left of the other = no collision
		if (rect1x2 < rect2x1 || rect1x1 > rect2x2)
			return false;

		// If one is above the other = also no collision
		if (rect1y2 < rect2y1 || rect1y1 > rect2y2)
			return false;

		// If none of the above conditions were met, the two rects do intersect
		return true;
	}

	private static boolean checkPixCollision(int image1XOffset,
		int image1YOffset, int image2XOffset, int image2YOffset,
		Image image1, int transform1, Image image2, int transform2,int width,
		int height) 
	{
		final int[] argbData1 = getARGBData(image1, image1XOffset,
			image1YOffset, width, height);

		final int[] argbData2 = getARGBData(image2, image2XOffset,
			image2YOffset, width, height);

		final int[] data1Pos = getSpriteIncrAndStartPos(transform1, width,
			height);

		final int[] data2Pos = getSpriteIncrAndStartPos(transform2, width,
			height);

		int row, col, x1, x2, alpha1, alpha2;
		for (row = 0; row < height; row++)
		{
			x1 = 0;
			x2 = 0;
			for (col = 0; col < width; col++)
			{
				// If there's an opaque pixel in both image's positions, we
				// have a collision
				alpha1 = (argbData1[(row * data1Pos[2]) + x1] >> 24) & 0xFF;
				alpha2 = (argbData2[(row * data2Pos[2]) + x2] >> 24) & 0xFF;
				if ((alpha1 == 0xFF) && (alpha2 == 0xFF))
					return true;

				x1 += data1Pos[1];
				x2 += data2Pos[1];
			}
		}

		return false;
	}

	private static int[] getSpriteIncrAndStartPos(int transform, int width,
		int height)
	{
		boolean isRot180 = (transform & TRANS_MIRROR_ROT180) != 0;
		boolean isMirrorX = (transform & TRANS_MIRROR) != 0;
		int startYPos, incrX, incrY;
		
		// Is it mirrored vertically?
		if ((transform & (TRANS_MIRROR | TRANS_MIRROR_ROT180)) != 0)
		{
			incrX = isRot180 ? -height : height;
			startYPos = isRot180 ? (width * height) - height : 0;
			incrY = isMirrorX ? -1 : 1;
			if (isMirrorX) { startYPos = height - 1; }
		} 
		else 
		{
			incrY = isRot180 ? -width : width;
			startYPos = isRot180 ? (width * height) - width : 0;
			incrX = isMirrorX ? -1 : 1;
			if (isMirrorX) { startYPos += (width - 1); }
		}
		return new int[] {startYPos, incrX, incrY};
	}

	private static int[] getARGBData(Image image, int xOffset, int yOffset,
		int width, int height) 
	{
		int[] argbData = new int[height * width];

		image.getRGB(argbData, 0, width, xOffset, yOffset, height, width);

		return argbData;
	}

	private int getImageTopLeft(int x1, int y1, int x2, int y2, boolean isX) 
	{
		int ret = 0;
	
		switch (this.currentTransform)
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

	private int getTransformedPos(int coordX, int coordY, int transform, boolean isX)
	{
		switch (transform)
		{
			case TRANS_NONE:
				return isX ? coordX : coordY;
			case TRANS_MIRROR:
				return isX ? srcFrameWidth - coordX - 1 : coordY;
			case TRANS_MIRROR_ROT180:
				return isX ? coordX : srcFrameHeight - coordY - 1;
			case TRANS_ROT90:
				return isX ? srcFrameHeight - coordY - 1 : coordX;
			case TRANS_ROT180:
				return isX ? srcFrameWidth - coordX - 1 : srcFrameHeight - coordY - 1;
			case TRANS_ROT270:
				return isX ? coordY : srcFrameWidth - coordX - 1;
			case TRANS_MIRROR_ROT90:
				return isX ? srcFrameHeight - coordY - 1 : srcFrameWidth - coordX - 1;
			case TRANS_MIRROR_ROT270:
				return isX ? coordY : coordX;
			default:
				return 0;
		}
	}
}