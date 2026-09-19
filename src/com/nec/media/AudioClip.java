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
package com.nec.media;

public abstract interface AudioClip
{
	public abstract void addAudioListener(AudioListener paramAudioListener);

	public abstract void play() throws IllegalStateException;

	public abstract void stop() throws IllegalStateException;

	public abstract int getLapsedTime() throws IllegalStateException;

	public abstract int getTime() throws IllegalStateException;

	public abstract int getChannel() throws IllegalStateException;

	public abstract int getTempo() throws IllegalStateException;

	public abstract void setLoopCount(int paramInt) throws IllegalArgumentException, IllegalStateException;

	public abstract AudioListener getAudioListener();
}
