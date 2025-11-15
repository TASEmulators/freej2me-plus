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

import java.util.concurrent.locks.LockSupport;

import javax.microedition.lcdui.Canvas;

public class MediaPlayerBox extends Canvas implements MediaPlayerInterface 
{
    public static final int BACKGROUND = 0;
    public static final int FOREGROUND = 1;
    public static final int PAUSE = 2;
    public static final int PLAY = 3;
    public static final int RESOURCE_DISPOSED = 4;
    public static final int RESUME = 5;
    public static final int STOP = 6;

    private int state = STOP;
    private int mode = BACKGROUND;
    private MediaResource _resource;
    private MediaPlayer _player;
    private MediaEventListener listener;
    private int volume = 100;
    private int tempo = 100;
    private int pitch = 0;

    public MediaPlayerBox() { this(null, FOREGROUND); }

    public MediaPlayerBox(int flag) { this(null, flag); }

    public MediaPlayerBox(MediaResource resource, int flag) 
    {
        if (flag != FOREGROUND && flag != BACKGROUND) 
        {
            throw new IllegalArgumentException("illegal resource flag (" + flag + ").");
        }

        this.mode = flag;
        this._resource = null;
        this._player = null;

        if (resource != null) { this.setResource(resource); }
    }

    public void addMediaEventListener(MediaEventListener l) { listener = l; }

    public int getAttribute(int attr) { return this._player.getAttribute(attr); }

    protected int getMode() { return this.mode; }

    public int getPitch() { return pitch; }

    protected MediaPlayer getPlayer() { return this._player; }

    public MediaResource getResource() { return this._resource; }

    public int getTempo() { return tempo; }

    public int getVolume() { return volume; }

    public void hide() { }

    protected MediaPlayer instantiatePlayer(MediaResource resource) 
    {
        if (resource.getType() == MediaResource.FORMAT_CMF1 ||
        resource.getType() == MediaResource.SMAF_YAMAHA_MA1 ||
        resource.getType() == MediaResource.SMAF_YAMAHA_MA2 ||
        resource.getType() == MediaResource.SMAF_YAMAHA_MA3 || 
        resource.getType() == MediaResource.SMAF_YAMAHA_MA5)
        {
            this._player = new SMAFPlayer(resource, this);
            return this._player;
        }
        return null;
    }

    protected void paint(javax.microedition.lcdui.Graphics g) { }

    public void pause() { this._player.pause(); }

    public void play() 
    { 
        this._player.addMediaEventListener(listener);
        if(this._player.getState() >= javax.microedition.media.Player.REALIZED) 
        {
            this._player.setPitch(Math.max(-6, Math.min(pitch, 6)));
            this._player.setTempo(Math.max(85, Math.min(tempo, 115)));
            this._player.setVolume(Math.max(0, Math.min(volume, 100)));
        }
        this._player.play(); 

        /* play() should not return until media playback finishes in FOREGROUND mode, but tetris outdoor doesn't like this

        if(mode == FOREGROUND) 
        { 
            while(this._player.getState() == javax.microedition.media.Player.STARTED) { LockSupport.parkNanos(1000000); }
        } 
        */
    }

    public void play(int count) { this._player.play(count); }

    public void removeMediaEventListener(MediaEventListener l) 
    { 
        this._player.removeMediaEventListener(l);
    }

    public void resume() { this._player.resume(); }

    public void setAttribute(int attr, int value) 
    {
        this._player.setAttribute(attr, value);
    }

    protected void setPlayerAttributes() { }

    public void setResource(MediaResource resource) 
    {
        if (this._player != null && this._player.getState() > javax.microedition.media.Player.PREFETCHED) { throw new IllegalStateException("state must be STOP"); }
        if (this._resource != null) 
        {
            throw new IllegalStateException("resource must be unset before setting.");
        }
        if (resource == null) throw new NullPointerException();

        // FIXME: Check the resource can be played.
        this._resource = resource;
        resource.addPlayer(this);
        if (_player != null) { this._player.dispose(); }
        this._player = instantiatePlayer(resource);
        this._player.setResource(resource);
    }

    public void setPitch(int pitch) { this.pitch = pitch; }

    public void setTempo(int tempo) { this.tempo = tempo; }

    public void setVolume(int volume) { this.volume = volume; }

    public void show() 
    { 
        // TODO
    }

    public void stop() 
    {
        if (this._player != null) { this._player.stop(); }
    }

    public void unsetResource(MediaResource resource) 
    {
        if (this._player.getState() > javax.microedition.media.Player.PREFETCHED) { return; }
        resource.removePlayer(this);
        if (_player != null) { this._player.dispose(); }
        this._resource = null;
    }
}