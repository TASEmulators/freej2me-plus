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

	Image sourceImage;

	int numberFrames;

	int[] frameCoordsX;

	int[] frameCoordsY;

	int srcFrameWidth;

	int srcFrameHeight;

	int[] sequence;

	private int sequenceIndex;

	private boolean customSequenceDefined;

	int dRefX;

	int dRefY;

	int collisionRectX;

	int collisionRectY;

	int collisionRectWidth;

	int collisionRectHeight;

	int t_currentTransformation;

	int t_collisionRectX;

	int t_collisionRectY;

	int t_collisionRectWidth;

	int t_collisionRectHeight;


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

		x = s.getRefPixelX() - getTransformedPtX(dRefX, dRefY, t_currentTransformation);
		y = s.getRefPixelY() - getTransformedPtY(dRefX, dRefY, t_currentTransformation);
	}

	public int getRefPixelX() { return (this.x + getTransformedPtX(dRefX, dRefY, this.t_currentTransformation)); }

	public int getRefPixelY() { return (this.y + getTransformedPtY(dRefX, dRefY, this.t_currentTransformation)); }

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

			int oldX = this.x + getTransformedPtX(dRefX, dRefY, this.t_currentTransformation);
			int oldY = this.y + getTransformedPtY(dRefX, dRefY, this.t_currentTransformation);

			setWidth(frameWidth);
			setHeight(frameHeight);

			initializeFrames(img, frameWidth, frameHeight, maintainCurFrame);

			initCollisionRectBounds();

			this.x = oldX - getTransformedPtX(dRefX, dRefY, this.t_currentTransformation);

			this.y = oldY - getTransformedPtY(dRefX, dRefY, this.t_currentTransformation);

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

		this.x = this.x + getTransformedPtX(dRefX, dRefY, this.t_currentTransformation) - getTransformedPtX(dRefX, dRefY, transform);

		this.y = this.y + getTransformedPtY(dRefX, dRefY, this.t_currentTransformation) - getTransformedPtY(dRefX, dRefY, transform);

		computeTransformedBounds(transform);

		t_currentTransformation = transform;
	}

	public final boolean collidesWith(Sprite s, boolean pixelLevel)
	{

		if (!(s.visible && this.visible)) { return false; }

		int otherLeft = s.x + s.t_collisionRectX;
		int otherTop = s.y + s.t_collisionRectY;
		int otherRight = otherLeft + s.t_collisionRectWidth;
		int otherBottom = otherTop + s.t_collisionRectHeight;

		int left = this.x + this.t_collisionRectX;
		int top = this.y + this.t_collisionRectY;
		int right = left + this.t_collisionRectWidth;
		int bottom = top + this.t_collisionRectHeight;

		if (intersectRect(otherLeft, otherTop, otherRight, otherBottom, left, top, right, bottom))
		{

			if (pixelLevel)
			{

				if (this.t_collisionRectX < 0) { left = this.x; }
				if (this.t_collisionRectY < 0) { top = this.y; }
				if ((this.t_collisionRectX + this.t_collisionRectWidth) > this.width) { right = this.x + this.width; }
				if ((this.t_collisionRectY + this.t_collisionRectHeight) > this.height) { bottom = this.y + this.height; }

				if (s.t_collisionRectX < 0) { otherLeft = s.x; }
				if (s.t_collisionRectY < 0) { otherTop = s.y; }
				if ((s.t_collisionRectX + s.t_collisionRectWidth) > s.width) { otherRight = s.x + s.width; }
				if ((s.t_collisionRectY + s.t_collisionRectHeight) > s.height) { otherBottom = s.y + s.height; }

				if (!intersectRect(otherLeft, otherTop, otherRight, otherBottom, left, top, right, bottom)) { return false; }

				final int intersectLeft = (left < otherLeft) ? otherLeft : left;
				final int intersectTop = (top < otherTop) ? otherTop : top;

				final int intersectRight = (right < otherRight) ? right : otherRight;
				final int intersectBottom = (bottom < otherBottom) ? bottom : otherBottom;

				final int intersectWidth = Math.abs(intersectRight - intersectLeft);
				final int intersectHeight = Math.abs(intersectBottom - intersectTop);

				final int thisImageXOffset = getImageTopLeftX(intersectLeft,
						intersectTop,
						intersectRight,
						intersectBottom);

				final int thisImageYOffset = getImageTopLeftY(intersectLeft,
						intersectTop,
						intersectRight,
						intersectBottom);

				final int otherImageXOffset = s.getImageTopLeftX(intersectLeft,
						intersectTop,
						intersectRight,
						intersectBottom);

				final int otherImageYOffset = s.getImageTopLeftY(intersectLeft,
						intersectTop,
						intersectRight,
						intersectBottom);

				return doPixelCollision(thisImageXOffset, thisImageYOffset,
						otherImageXOffset, otherImageYOffset,
						this.sourceImage,
						this.t_currentTransformation,
						s.sourceImage,
						s.t_currentTransformation,
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

		int tW = t.getCellWidth();
		int tH = t.getCellHeight();

		int sx1 = this.x + this.t_collisionRectX;
		int sy1 = this.y + this.t_collisionRectY;
		int sx2 = sx1 + this.t_collisionRectWidth;
		int sy2 = sy1 + this.t_collisionRectHeight;

		int tNumCols = t.getColumns();
		int tNumRows = t.getRows();

		int startCol = 0;
		int endCol = 0;
		int startRow = 0;
		int endRow = 0;

		if (!intersectRect(tLx1, tLy1, tLx2, tLy2, sx1, sy1, sx2, sy2)) { return false; }

		startCol = (sx1 <= tLx1) ? 0 : (sx1 - tLx1) / tW;
		startRow = (sy1 <= tLy1) ? 0 : (sy1 - tLy1) / tH;

		endCol = (sx2 < tLx2) ? ((sx2 - 1 - tLx1) / tW) : tNumCols - 1;
		endRow = (sy2 < tLy2) ? ((sy2 - 1 - tLy1) / tH) : tNumRows - 1;

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
			if (this.t_collisionRectX < 0) { sx1 = this.x; }
			if (this.t_collisionRectY < 0) { sy1 = this.y; }
			if ((this.t_collisionRectX + this.t_collisionRectWidth) > this.width) { sx2 = this.x + this.width; }
			if ((this.t_collisionRectY + this.t_collisionRectHeight) > this.height) { sy2 = this.y + this.height; }

			if (!intersectRect(tLx1, tLy1, tLx2, tLy2, sx1, sy1, sx2, sy2)) { return (false); }

			startCol = (sx1 <= tLx1) ? 0 : (sx1 - tLx1) / tW;
			startRow = (sy1 <= tLy1) ? 0 : (sy1 - tLy1) / tH;

			endCol = (sx2 < tLx2) ? ((sx2 - 1 - tLx1) / tW) : tNumCols - 1;
			endRow = (sy2 < tLy2) ? ((sy2 - 1 - tLy1) / tH) : tNumRows - 1;

			int cellTop = startRow * tH + tLy1;
			int cellBottom = cellTop + tH;

			int tileIndex;;

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

						if (intersectLeft > intersectRight)
						{
							int temp = intersectRight;
							intersectRight = intersectLeft;
							intersectLeft = temp;
						}

						if (intersectTop > intersectBottom)
						{
							int temp = intersectBottom;
							intersectBottom = intersectTop;
							intersectTop = temp;
						}

						int intersectWidth = intersectRight - intersectLeft;
						int intersectHeight = intersectBottom - intersectTop;

						int image1XOffset = getImageTopLeftX(intersectLeft,
								intersectTop,
								intersectRight,
								intersectBottom);

						int image1YOffset = getImageTopLeftY(intersectLeft,
								intersectTop,
								intersectRight,
								intersectBottom);

						int image2XOffset = t.tileSetX[tileIndex] +
								(intersectLeft - cellLeft);
						int image2YOffset = t.tileSetY[tileIndex] +
								(intersectTop - cellTop);

						if (doPixelCollision(image1XOffset,
								image1YOffset,
								image2XOffset,
								image2YOffset,
								this.sourceImage,
								this.t_currentTransformation,
								t.image,
								TRANS_NONE,
								intersectWidth, intersectHeight))
								{ return true; }
					}
				}
			}

			return false;
		}

	}

	public final boolean collidesWith(Image image, int x,
									  int y, boolean pixelLevel)
	{

		if (!(visible)) { return false; }

		int otherLeft = x;
		int otherTop = y;
		int otherRight = x + image.getWidth();
		int otherBottom = y + image.getHeight();

		int left = x + t_collisionRectX;
		int top = y + t_collisionRectY;
		int right = left + t_collisionRectWidth;
		int bottom = top + t_collisionRectHeight;

		// first check if the collision rectangles of the two sprites intersect
		if (intersectRect(otherLeft, otherTop, otherRight, otherBottom, left, top, right, bottom))
		{
			if (pixelLevel)
			{
				if (this.t_collisionRectX < 0) { left = this.x; }
				if (this.t_collisionRectY < 0) { top = this.y; }
				if ((this.t_collisionRectX + this.t_collisionRectWidth) > this.width) { right = this.x + this.width; }
				if ((this.t_collisionRectY + this.t_collisionRectHeight) > this.height) { bottom = this.y + this.height; }

				if (!intersectRect(otherLeft, otherTop, otherRight, otherBottom, left, top, right, bottom))
				{ return false; }

				final int intersectLeft = (left < otherLeft) ? otherLeft : left;
				final int intersectTop = (top < otherTop) ? otherTop : top;

				final int intersectRight = (right < otherRight)
						? right : otherRight;
				final int intersectBottom = (bottom < otherBottom)
						? bottom : otherBottom;

				final int intersectWidth = Math.abs(intersectRight - intersectLeft);
				final int intersectHeight = Math.abs(intersectBottom - intersectTop);

				final int thisImageXOffset = getImageTopLeftX(intersectLeft,
						intersectTop,
						intersectRight,
						intersectBottom);

				final int thisImageYOffset = getImageTopLeftY(intersectLeft,
						intersectTop,
						intersectRight,
						intersectBottom);

				final int otherImageXOffset = intersectLeft - x;
				final int otherImageYOffset = intersectTop - y;

				return doPixelCollision(thisImageXOffset, thisImageYOffset,
						otherImageXOffset, otherImageYOffset,
						this.sourceImage,
						this.t_currentTransformation,
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

				if (!customSequenceDefined)
				{
					sequence[currentFrame] = currentFrame;
				}
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

	private static boolean doPixelCollision(int image1XOffset,
											int image1YOffset,
											int image2XOffset,
											int image2YOffset,
											Image image1, int transform1,
											Image image2, int transform2,
											int width, int height)
	{

		int startY1;
		int xIncr1, yIncr1;

		int startY2;
		int xIncr2, yIncr2;

		int numPixels = height * width;

		final int[] argbData1 = new int[numPixels];
		final int[] argbData2 = new int[numPixels];

		if (0x0 != (transform1 & INVERTED_AXES))
		{

			if (0x0 != (transform1 & Y_FLIP))
			{
				xIncr1 = -(height);
				startY1 = numPixels - height;
			}
			else
			{
				xIncr1 = height;
				startY1 = 0;
			}

			if (0x0 != (transform1 & X_FLIP))
			{
				yIncr1 = -1;
				startY1 += (height - 1);
			}
			else { yIncr1 = +1; }

			image1.getRGB(argbData1, 0, height, image1XOffset, image1YOffset, height, width);

		}
		else
		{
			if (0x0 != (transform1 & Y_FLIP))
			{
				startY1 = numPixels - width;
				yIncr1 = -(width);
			}
			else
			{
				startY1 = 0;
				yIncr1 = width;
			}

			if (0x0 != (transform1 & X_FLIP))
			{
				xIncr1 = -1;
				startY1 += (width - 1);
			}
			else { xIncr1 = +1; }

			image1.getRGB(argbData1, 0, width, image1XOffset, image1YOffset, width, height);
		}

		if (0x0 != (transform2 & INVERTED_AXES))
		{
			if (0x0 != (transform2 & Y_FLIP))
			{
				xIncr2 = -(height);
				startY2 = numPixels - height;
			}
			else
			{
				xIncr2 = height;
				startY2 = 0;
			}

			if (0x0 != (transform2 & X_FLIP))
			{
				yIncr2 = -1;
				startY2 += height - 1;
			}
			else { yIncr2 = +1; }

			image2.getRGB(argbData2, 0, height,image2XOffset, image2YOffset, height, width);

		}
		else
		{

			if (0x0 != (transform2 & Y_FLIP))
			{
				startY2 = numPixels - width;
				yIncr2 = -(width);
			}
			else
			{
				startY2 = 0;
				yIncr2 = +width;
			}

			if (0x0 != (transform2 & X_FLIP))
			{
				xIncr2 = -1;
				startY2 += (width - 1);
			}
			else { xIncr2 = +1; }

			image2.getRGB(argbData2, 0, width, image2XOffset, image2YOffset, width, height);
		}

		int x1, x2;
		int xLocalBegin1, xLocalBegin2;

		int numIterRows;
		int numIterColumns;

		for (numIterRows = 0, xLocalBegin1 = startY1, xLocalBegin2 = startY2; numIterRows < height;
			 xLocalBegin1 += yIncr1, xLocalBegin2 += yIncr2, numIterRows++)
		{

			for (numIterColumns = 0, x1 = xLocalBegin1, x2 = xLocalBegin2; numIterColumns < width; x1 += xIncr1, x2 += xIncr2, numIterColumns++)
			{
				if (((argbData1[x1] & ALPHA_BITMASK) == FULLY_OPAQUE_ALPHA) && ((argbData2[x2] & ALPHA_BITMASK) == FULLY_OPAQUE_ALPHA))
					{ return true; }
			}
		}

		return false;
	}

	private int getImageTopLeftX(int x1, int y1, int x2, int y2)
	{
		int retX = 0;

		switch (this.t_currentTransformation)
		{

			case TRANS_NONE:
			case TRANS_MIRROR_ROT180:
				retX = x1 - this.x;
				break;

			case TRANS_MIRROR:
			case TRANS_ROT180:
				retX = (this.x + this.width) - x2;
				break;

			case TRANS_ROT90:
			case TRANS_MIRROR_ROT270:
				retX = y1 - this.y;
				break;

			case TRANS_ROT270:
			case TRANS_MIRROR_ROT90:
				retX = (this.y + this.height) - y2;
				break;

			default:
				return retX;
		}

		retX += frameCoordsX[sequence[sequenceIndex]];

		return retX;
	}

	private int getImageTopLeftY(int x1, int y1, int x2, int y2)
	{
		int retY = 0;
		switch (this.t_currentTransformation)
		{

			case TRANS_NONE:
			case TRANS_MIRROR:
				retY = y1 - this.y;
				break;

			case TRANS_ROT180:
			case TRANS_MIRROR_ROT180:
				retY = (this.y + this.height) - y2;
				break;

			case TRANS_ROT270:
			case TRANS_MIRROR_ROT270:
				retY = x1 - this.x;
				break;

			case TRANS_ROT90:
			case TRANS_MIRROR_ROT90:
				retY = (this.x + this.width) - x2;
				break;

			default:
				return retY;
		}

		retY += frameCoordsY[sequence[sequenceIndex]];

		return retY;
	}

	private void computeTransformedBounds(int transform)
	{
		switch (transform)
		{

			case TRANS_NONE:

				t_collisionRectX = collisionRectX;
				t_collisionRectY = collisionRectY;
				t_collisionRectWidth = collisionRectWidth;
				t_collisionRectHeight = collisionRectHeight;
				this.width = srcFrameWidth;
				this.height = srcFrameHeight;

				break;

			case TRANS_MIRROR:

				t_collisionRectX = srcFrameWidth - (collisionRectX + collisionRectWidth);

				t_collisionRectY = collisionRectY;
				t_collisionRectWidth = collisionRectWidth;
				t_collisionRectHeight = collisionRectHeight;

				this.width = srcFrameWidth;
				this.height = srcFrameHeight;

				break;

			case TRANS_MIRROR_ROT180:

				t_collisionRectY = srcFrameHeight -
						(collisionRectY + collisionRectHeight);

				t_collisionRectX = collisionRectX;
				t_collisionRectWidth = collisionRectWidth;
				t_collisionRectHeight = collisionRectHeight;

				this.width = srcFrameWidth;
				this.height = srcFrameHeight;

				break;

			case TRANS_ROT90:

				t_collisionRectX = srcFrameHeight -
						(collisionRectHeight + collisionRectY);
				t_collisionRectY = collisionRectX;

				t_collisionRectHeight = collisionRectWidth;
				t_collisionRectWidth = collisionRectHeight;

				this.width = srcFrameHeight;
				this.height = srcFrameWidth;

				break;

			case TRANS_ROT180:

				t_collisionRectX = srcFrameWidth - (collisionRectWidth +
						collisionRectX);
				t_collisionRectY = srcFrameHeight - (collisionRectHeight +
						collisionRectY);

				t_collisionRectWidth = collisionRectWidth;
				t_collisionRectHeight = collisionRectHeight;

				this.width = srcFrameWidth;
				this.height = srcFrameHeight;

				break;

			case TRANS_ROT270:

				t_collisionRectX = collisionRectY;
				t_collisionRectY = srcFrameWidth - (collisionRectWidth +
						collisionRectX);

				t_collisionRectHeight = collisionRectWidth;
				t_collisionRectWidth = collisionRectHeight;

				this.width = srcFrameHeight;
				this.height = srcFrameWidth;

				break;

			case TRANS_MIRROR_ROT90:

				t_collisionRectX = srcFrameHeight - (collisionRectHeight +
						collisionRectY);
				t_collisionRectY = srcFrameWidth - (collisionRectWidth +
						collisionRectX);

				t_collisionRectHeight = collisionRectWidth;
				t_collisionRectWidth = collisionRectHeight;

				this.width = srcFrameHeight;
				this.height = srcFrameWidth;

				break;

			case TRANS_MIRROR_ROT270:

				t_collisionRectY = collisionRectX;
				t_collisionRectX = collisionRectY;

				t_collisionRectHeight = collisionRectWidth;
				t_collisionRectWidth = collisionRectHeight;

				this.width = srcFrameHeight;
				this.height = srcFrameWidth;
				break;

			default: throw new IllegalArgumentException();
		}
	}

	private int getTransformedPtX(int x, int y, int transform)
	{
		int t_x = 0;
		switch (transform)
		{

			case TRANS_NONE:
				t_x = x;
				break;
			case TRANS_MIRROR:
				t_x = srcFrameWidth - x - 1;
				break;
			case TRANS_MIRROR_ROT180:
				t_x = x;
				break;
			case TRANS_ROT90:
				t_x = srcFrameHeight - y - 1;
				break;
			case TRANS_ROT180:
				t_x = srcFrameWidth - x - 1;
				break;
			case TRANS_ROT270:
				t_x = y;
				break;
			case TRANS_MIRROR_ROT90:
				t_x = srcFrameHeight - y - 1;
				break;
			case TRANS_MIRROR_ROT270:
				t_x = y;
				break;
			default:
				break;
		}
		return t_x;
	}

	private int getTransformedPtY(int x, int y, int transform)
	{
		int t_y = 0;
		switch (transform)
		{

			case TRANS_NONE:
				t_y = y;
				break;
			case TRANS_MIRROR:
				t_y = y;
				break;
			case TRANS_MIRROR_ROT180:
				t_y = srcFrameHeight - y - 1;
				break;
			case TRANS_ROT90:
				t_y = x;
				break;
			case TRANS_ROT180:
				t_y = srcFrameHeight - y - 1;
				break;
			case TRANS_ROT270:
				t_y = srcFrameWidth - x - 1;
				break;
			case TRANS_MIRROR_ROT90:
				t_y = srcFrameWidth - x - 1;
				break;
			case TRANS_MIRROR_ROT270:
				t_y = x;
				break;
			default:
				break;
		}
		return t_y;
	}

}