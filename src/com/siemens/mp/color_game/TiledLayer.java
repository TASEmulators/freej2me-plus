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
package com.siemens.mp.color_game;

import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.Layer;
import javax.microedition.lcdui.game.Sprite;
import javax.microedition.lcdui.Graphics;

import org.recompile.mobile.Mobile;

import java.util.ArrayList;
import java.util.Arrays;

public class TiledLayer extends Layer 
{

	protected Image image;
	private int rows;
	private int cols;
	private int tileHeight;
	private int tileWidth;

	// Rendering checks
	private int clipX;
	private int clipY;
	private int clipWidth;
	private int clipHeight;

	private int startColumn;
	private int endColumn;
	private int startRow;
	private int endRow;

	private int tileIndex;
	private int tx, ty, row, column;
	
	private int numberOfTiles;
	protected int[] tileSetX;
	protected int[] tileSetY;
	private ArrayList<Integer> animatedTiles;
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
			animatedTiles = new ArrayList<Integer>();
			animatedTileCount = 0;
		} 

		animatedTiles.add(staticTileIndex);
		animatedTileCount++;

		return -animatedTileCount;
	}

	public void setAnimatedTile(int animatedTileIndex, int staticTileIndex) 
	{
		if (staticTileIndex < 0 || staticTileIndex >= numberOfTiles) { throw new IndexOutOfBoundsException(); }
		
		animatedTileIndex = -animatedTileIndex;
		if (animatedTiles == null || animatedTileIndex <= 0 || animatedTileIndex >= animatedTileCount) { throw new IndexOutOfBoundsException(); }

		animatedTiles.set(animatedTileIndex, staticTileIndex);
	}

	public int getAnimatedTile(int animatedTileIndex) 
	{
		animatedTileIndex = -animatedTileIndex;
		if (animatedTiles == null || animatedTileIndex <= 0 || animatedTileIndex >= animatedTileCount) { throw new IndexOutOfBoundsException(); }

		return animatedTiles.get(animatedTileIndex);
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

		for (int rowCount = row; rowCount < row + numRows; rowCount++) { Arrays.fill(tiles[rowCount], col, col + numCols, tileIndex); }
	}

	public final int getCellWidth() { return tileWidth; }

	public final int getCellHeight() { return tileHeight; }

	public final int getColumns() { return cols; }

	public final int getRows() { return rows; }

	public void setStaticTileSet(Image baseimage, int tileWidth, int tileHeight) 
	{
		if (tileWidth < 1 || tileHeight < 1 || ((baseimage.getWidth() % tileWidth) != 0) || ((baseimage.getHeight() % tileHeight) != 0)) 
			{ throw new IllegalArgumentException(); }

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
	
		if (!visible) { return; }
	
		// Drawing is restricted to target's clip rect bounds
		clipX = g.getClipX();
		clipY = g.getClipY();
		clipWidth = g.getClipWidth();
		clipHeight = g.getClipHeight();
	
		startColumn = Math.max(0, (clipX - this.x) / tileWidth);
		endColumn = Math.min(this.cols, (clipX + clipWidth - this.x + tileWidth - 1) / tileWidth);
		startRow = Math.max(0, (clipY - this.y) / tileHeight);
		endRow = Math.min(this.rows, (clipY + clipHeight - this.y + tileHeight - 1) / tileHeight);
	
		for (row = startRow; row < endRow; row++) 
		{
			ty = y + (row * tileHeight);
			for (column = startColumn; column < endColumn; column++) 
			{
				tileIndex = tiles[row][column];
	
				if (tileIndex == 0) { continue; } // Skip the transparent tile
				if (tileIndex < 0) { tileIndex = getAnimatedTile(tileIndex); }
	
				tx = x + (column * tileWidth);
				g.drawRegion(image, tileSetX[tileIndex], tileSetY[tileIndex], tileWidth, tileHeight, Sprite.TRANS_NONE, tx, ty, Graphics.TOP | Graphics.LEFT);
			}
		}
	}

	private void createStaticSet(Image baseImage, int noOfFrames, int tileWidth, int tileHeight, boolean maintainIndices) 
	{
		Mobile.log(Mobile.LOG_DEBUG, TiledLayer.class.getPackage().getName() + "." + TiledLayer.class.getSimpleName() + ": " + "Created StaticTileSet!");
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
	
		final int imageW = baseImage.getWidth();
		final int imageH = baseImage.getHeight();
	
		this.image = baseImage;
		this.numberOfTiles = noOfFrames;
		this.tileSetX = new int[numberOfTiles];
		this.tileSetY = new int[numberOfTiles];
	
		if (!maintainIndices) 
		{
			/* 
			 * Since we don't have to maintain Indices, initialize the
			 * TileMatrix such as all indices will be zero, then delete any
			 * animated tiles.
			 */
			for (int row = 0; row < tiles.length; row++) { Arrays.fill(tiles[row], 0); }
			animatedTiles.clear();
			animatedTiles = null;
		}
	
		// Now we can start actually adding tiles to the tile matrix.
		int currentTile = 1;
	
		for (int y = 0; y < imageH; y += tileHeight) 
		{
			for (int x = 0; x < imageW; x += tileWidth) 
			{
				tileSetX[currentTile] = x;
				tileSetY[currentTile] = y;
				currentTile++;
			}
		}
	}
}
