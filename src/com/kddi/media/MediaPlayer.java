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
package com.kddi.media;

public abstract class MediaPlayer implements MediaPlayerInterface 
{
    protected int id;
    protected MediaResource resource;
    protected static final int INVALID_PLAYER = -1;

    protected MediaPlayer(MediaResource resource, MediaPlayerBox box) {}

    public void addMediaEventListener(MediaEventListener l) { }

    public static boolean canPlay(java.lang.String dataType) { return false; }

    protected abstract void dispose();

    protected abstract boolean disposePlayer();

    public int getAttribute(int attr) { return 0; }

    public int getPitch() { return 0; }

    public MediaResource getResource() { return null; }

    public int getTempo() { return 0; }

    public int getVolume() { return 0; }

    public void hide() {}

    protected void notifyEvent(MediaPlayer player, int type, int option) {}

    protected void paint(javax.microedition.lcdui.Graphics g) {}

    public void pause() {}

    public void play() {}

    public void play(int count) {}

    public void removeMediaEventListener(MediaEventListener l) {}

    public void resume() {}

    public void setAttribute(int attr, int value) {}

    public void setPitch(int pitch) {}

    public void setResource(MediaResource resource) {}

    public void setTempo(int tempo) {}

    public void setVolume(int volume) {}

    public void show() {}

    public void stop() {}

    public void unsetResource(MediaResource resource) {}

    protected void validateId() {}

    /* Non-KDDI, but helps state management */
    public int getState() { return 0; }
}