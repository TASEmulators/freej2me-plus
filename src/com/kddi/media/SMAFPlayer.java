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

    protected int id, pitch = 0, tempo = 100, volume = 100;
    protected MediaPlayerListener listener;
    protected MediaResource resource;

    protected SMAFPlayer(MediaResource resource, MediaPlayerBox box) 
    { 
        super(resource, box);

        InputStream stream = new ByteArrayInputStream(resource.getData());

        try 
        { 
            this._player = new PlatformPlayer(stream, "");
            this._player.prefetch();
            this._player.addMediaPlayerBox(box); // So that listener events know which box they're being fired from
        } 
        catch (Exception e) { e.printStackTrace(); } 
        finally 
        {
            try { stream.close(); } 
            catch (Exception e) { e.printStackTrace(); }
        }
    }

    public static boolean canPlay(String dataType) { return true; }

    protected void dispose() 
    { 
        this._player.notifyListeners("deviceUnavailable", 0);
        this._player.close(); 
    }

    protected boolean disposePlayer() 
    {
        dispose();
        return true;
    }

    public void pause() { this._player.stop(); }

    public void play() { this.play(1); }

    public void play(int count) 
    {
        this._player.setMediaTime(0);
        this._player.setLoopCount(count);      
        ((PlatformPlayer.volumeControl)this._player.getControl("VolumeControl")).setLevel(volume);
        ((PlatformPlayer.tempoControl)this._player.getControl("TempoControl")).setRate(tempo*1000); // javax' tempoControl operates in the thousands for rate
        this._player.start();
    }

    public void resume() { this._player.start(); }

    public void stop() 
    {
        this._player.stop();
        this._player.setMediaTime(0);
    }

    public int getPitch() { return 0;  }

    public int getTempo() { return ((PlatformPlayer.tempoControl)this._player.getControl("TempoControl")).getRate() / 1000; }

    public int getVolume() 
    { 
        return ((PlatformPlayer.volumeControl)this._player.getControl("VolumeControl")).getLevel();
    }

    // TODO: Pitch and Tempo changes, not sure if they're even used in KDDI, their Java run was rather short-lived
    public void setPitch(int pitch) 
    { 
        this.pitch = Math.max(-6, Math.min(pitch, 6)); 
    }

    public void setTempo(int tempo) 
    { 
        this.tempo = Math.max(85, Math.min(tempo, 115)); 
    }

    public void setVolume(int volume) 
    {  
        this.volume = volume;
    }

    public void addMediaEventListener(MediaEventListener l) { this._player.addPlayerListener(l); }

    public void removeMediaEventListener(MediaEventListener l) { this._player.removePlayerListener(l); }

    public int getState() { return _player.getState(); }
}
