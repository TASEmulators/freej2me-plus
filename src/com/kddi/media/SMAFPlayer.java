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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.recompile.mobile.PlatformPlayer;

import com.jblend.media.MediaPlayerListener;

public class SMAFPlayer extends MediaPlayer 
{
    private PlatformPlayer _player;

    protected MediaPlayerBox box;
    protected int id;
    protected MediaPlayerListener listener;
    protected int pitch;
    protected Object resource;
    protected int tempo;
    protected int volume;

    protected SMAFPlayer(MediaResource resource, MediaPlayerBox box) 
    { 
        super(resource, box);

        byte[] resourceDat = MediaManager.getResource(resource);
        InputStream stream = new ByteArrayInputStream(resourceDat);
        try {
            this._player = new PlatformPlayer(stream, "");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean canPlay(String dataType) {
        return true;
    }

    protected void dispose() {
    }

    protected boolean disposePlayer() { 
        this._player.close();
        return true;
    }

    public int getPitch() { return pitch;  }

    public int getTempo() { return tempo; }

    public int getVolume() { return volume; }

    public void pause() {
        this._player.stop();
    }

    public void play() {
        this.play(0);
    }

    public void play(int count) {
        this._player.setLoopCount(count);
        this._player.start();
    }

    public void resume() {
        this._player.start();
    }

    public void setPitch(int pitch) { pitch = Math.max(-6, Math.min(pitch, 6)); }

    public void setTempo(int tempo) { tempo = Math.max(85, Math.min(tempo, 115)); }

    public void setVolume(int volume) {  volume = Math.max(0, Math.min(volume, 100)); }

    public void stop() {
        this._player.stop();
    }
}