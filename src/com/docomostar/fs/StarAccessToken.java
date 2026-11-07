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
package com.docomostar.fs;

public class StarAccessToken implements AccessToken
{
	public static final int ACCESS_UIM = 0x01;
	public static final int ACCESS_PLATFORM = 0x02;
	public static final int ACCESS_SERIES = 0x04;

	private int access, share;
	public StarAccessToken(int access, int share)
	{
		this.access = access;
		this.share = share;
	}

	public int getAccess()
	{
		return this.access;
	}

	public int getShare()
	{
		return this.share;
	}
}
