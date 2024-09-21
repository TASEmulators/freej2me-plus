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
package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class TiledLayer extends Layer 
{

	protected Image image;
	private int rows;
	private int cols;
	private int tileHeight;
	private int tileWidth;
	
	private int numberOfTiles;
	int[] tileSetX;
	int[] tileSetY;
	private int[] animatedTiles;
	private int animatedTileCount = 0;

	private int[][] tiles;

	public TiledLayer(int colsw, int rowsh, Image baseimage, int tileWidth, int tileHeight) 
	{
		super(colsw < 1 || tileWidth < 1 ? -1 : colsw * tileWidth, rowsh < 1 || tileHeight < 1 ? -1 : rowsh * tileHeight);

		if (((baseimage.getWidth() % tileWidth) != 0) || ((baseimage.getHeight() % tileHeight) != 0)) { throw new IllegalArgumentException(); }
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		this.cols = colsw;
		this.rows = rowsh;

		x = 0;
		y = 0;
		width = tileWidth*cols;
		height = tileHeight*rows;

		tiles = new int[rowsh][colsw];

		final int noOfFrames = (baseimage.getWidth() / tileWidth) * (baseimage.getHeight() / tileHeight);
		createStaticSet(baseimage, noOfFrames + 1, tileWidth, tileHeight, true);
	}

	public int createAnimatedTile(int staticTileIndex) 
	{
		if (staticTileIndex < 0 || staticTileIndex >= numberOfTiles) { throw new IndexOutOfBoundsException(); }

		if (animatedTiles == null) 
		{
			animatedTiles = new int[4];
			animatedTileCount = 1;
		} 
		else if (animatedTileCount == animatedTiles.length) 
		{
			// AnimatedTiles limit has been reached, we will need to increase its size
			int newAnimatedTiles[] = new int[animatedTiles.length * 2];
			System.arraycopy(animatedTiles, 0, newAnimatedTiles, 0, animatedTiles.length);
			animatedTiles = newAnimatedTiles;
		}

		animatedTiles[animatedTileCount] = staticTileIndex;
		animatedTileCount++;
		return (-(animatedTileCount - 1));
	}

	public void setAnimatedTile(int animatedTileIndex, int staticTileIndex) 
	{
		if (staticTileIndex < 0 || staticTileIndex >= numberOfTiles) { throw new IndexOutOfBoundsException(); }
		
		animatedTileIndex = -animatedTileIndex;
		if (animatedTiles == null || animatedTileIndex <= 0 || animatedTileIndex >= animatedTileCount) { throw new IndexOutOfBoundsException(); }

		animatedTiles[animatedTileIndex] = staticTileIndex;
	}

	public int getAnimatedTile(int animatedTileIndex) 
	{
		animatedTileIndex = -animatedTileIndex;
		if (animatedTiles == null || animatedTileIndex <= 0 || animatedTileIndex >= animatedTileCount) { throw new IndexOutOfBoundsException(); }

		return animatedTiles[animatedTileIndex];
	}

	public void setCell(int col, int row, int tileIndex) 
	{
		if (col < 0 || col >= this.cols || row < 0 || row >= this.rows) { throw new IndexOutOfBoundsException(); }

		if (tileIndex > 0) { if (tileIndex >= numberOfTiles) { throw new IndexOutOfBoundsException(); } } 
		else if (tileIndex < 0) { if (animatedTiles == null || (-tileIndex) >= animatedTileCount) { throw new IndexOutOfBoundsException(); } }

		tiles[row][col] = tileIndex;
	}

	public int getCell(int col, int row) 
	{
		if (col < 0 || col >= this.cols || row < 0 || row >= this.rows) { throw new IndexOutOfBoundsException(); }
		return tiles[row][col];
	}

	public void fillCells(int col, int row, int numCols, int numRows, int tileIndex) 
	{
		if (numCols < 0 || numRows < 0) { throw new IllegalArgumentException(); }

		if (col < 0 || col >= this.cols || row < 0 || row >= this.rows || col + numCols > this.cols || row + numRows > this.rows)  { throw new IndexOutOfBoundsException(); }

		if (tileIndex > 0) { if (tileIndex >= numberOfTiles) { throw new IndexOutOfBoundsException(); } } 
		else if (tileIndex < 0) { if (animatedTiles == null || (-tileIndex) >= animatedTileCount) { throw new IndexOutOfBoundsException(); } }

		for (int rowCount = row; rowCount < row + numRows; rowCount++) 
		{
			for (int columnCount = col; columnCount < col + numCols; columnCount++) { tiles[rowCount][columnCount] = tileIndex; }
		}
	}

	public final int getCellWidth() { return tileWidth; }

	public final int getCellHeight() { return tileHeight; }

	public final int getColumns() { return cols; }

	public final int getRows() { return rows; }

	public void setStaticTileSet(Image baseimage, int tileWidth, int tileHeight) 
	{
		if (tileWidth < 1 || tileHeight < 1 || ((baseimage.getWidth() % tileWidth) != 0) || ((baseimage.getHeight() % tileHeight) != 0)) 
		{
			throw new IllegalArgumentException();
		}
		setWidth(cols * tileWidth);
		setHeight(rows * tileHeight);

		int noOfFrames = (baseimage.getWidth() / tileWidth) * (baseimage.getHeight() / tileHeight);

		// the zero index is left empty for transparent tiles
		// so it is passed in createStaticSet as noOfFrames + 1
		if (noOfFrames >= (numberOfTiles - 1)) { createStaticSet(baseimage, noOfFrames + 1, tileWidth, tileHeight, true); } 
		else { createStaticSet(baseimage, noOfFrames + 1, tileWidth, tileHeight, false); }
	}

	@Override
	public final void paint(Graphics g) 
	{
		if (g == null) { throw new NullPointerException(); }

		if (visible) 
		{
			int startColumn = 0;
			int endColumn = this.cols;
			int startRow = 0;
			int endRow = this.rows;

			// calculate the number of columns left of the clip
			int number = (g.getClipX() - this.x) / tileWidth;
			if (number > 0) { startColumn = number; }

			// calculate the number of columns right of the clip
			int endX = this.x + (this.cols * tileWidth);
			int endClipX = g.getClipX() + g.getClipWidth();
			number = (endX - endClipX) / tileWidth;
			if (number > 0) { endColumn -= number; }

			// calculate the number of rows above the clip
			number = (g.getClipY() - this.y) / tileHeight;
			if (number > 0) { startRow = number; }

			// calculate the number of rows below the clip
			int endY = this.y + (this.rows * tileHeight);
			int endClipY = g.getClipY() + g.getClipHeight();
			number = (endY - endClipY) / tileHeight;
			if (number > 0) { endRow -= number; }

			// paint all visible cells
			int tileIndex = 0;

			// y-coordinate
			int ty = this.y + (startRow * tileHeight);
			for (int row = startRow; row < endRow; row++, ty += tileHeight) 
			{

				// reset the x-coordinate at the beginning of every row
				// x-coordinate to draw tile into
				int tx = this.x + (startColumn * tileWidth);
				for (int column = startColumn; column < endColumn; column++, tx += tileWidth) 
				{

					tileIndex = tiles[row][column];
					// check the indices
					// if animated get the corresponding
					// static index from animatedTiles table

					// tileIndex = 0 is a transparent tile
					if (tileIndex == 0) { continue; } 
					else if (tileIndex < 0) { tileIndex = getAnimatedTile(tileIndex); }

					g.drawRegion(image, tileSetX[tileIndex], tileSetY[tileIndex], tileWidth, tileHeight, Sprite.TRANS_NONE, tx, ty, Graphics.TOP | Graphics.LEFT);
				}
			}
		}
	}

	private void createStaticSet(Image baseimage, int noOfFrames, int tileWidth, int tileHeight, boolean maintainIndices) 
	{
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;

		final int imageW = baseimage.getWidth();
		final int imageH = baseimage.getHeight();

		this.image = baseimage;

		numberOfTiles = noOfFrames;
		tileSetX = new int[numberOfTiles];
		tileSetY = new int[numberOfTiles];

		if (!maintainIndices) 
		{
			// populate tile matrix, all the indices are 0 to begin with
			for (rows = 0; rows < tiles.length; rows++) 
			{
				int totalCols = tiles[rows].length;
				for (cols = 0; cols < totalCols; cols++) { tiles[rows][cols] = 0; }
			}
			// delete animated tiles
			animatedTiles = null;
		}

		int currentTile = 1;

		for (int locY = 0; locY < imageH; locY += tileHeight) 
		{
			for (int locX = 0; locX < imageW; locX += tileWidth) 
			{

				tileSetX[currentTile] = locX;
				tileSetY[currentTile] = locY;

				currentTile++;
			}
		}
	}
}
