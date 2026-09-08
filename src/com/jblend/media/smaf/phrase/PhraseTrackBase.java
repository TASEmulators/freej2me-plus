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
package com.jblend.media.smaf.phrase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.media.Manager;
import javax.microedition.media.Player;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformPlayer;

public abstract class PhraseTrackBase
{
	public static final int NO_DATA = 1;
	public static final int READY = 2;
	public static final int PLAYING = 3;
	public static final int PAUSED = 5;
	public static final int DEFAULT_VOLUME = 100;
	public static final int DEFAULT_PANPOT = 64;
	private static final int MAX_VOLUME = 127;

	private int ID;
	private boolean paused = false;
	protected Phrase phrase;
	protected AudioPhrase audioPhrase;
	protected Player player;
	protected PhraseTrackListener listener;

	protected PhraseTrack phraseSyncMaster;
	protected com.j_phone.amuse.PhraseTrack jPhoneSyncMaster;
	protected com.vodafone.v10.sound.SoundTrack vodafoneSyncMaster;

	protected List<PhraseTrack> slavePhrases = new ArrayList<PhraseTrack>();
	protected List<com.j_phone.amuse.PhraseTrack> slaveJPhonePhrases = new ArrayList<com.j_phone.amuse.PhraseTrack>();
	protected List<com.vodafone.v10.sound.SoundTrack> slaveVodafonePhrases = new ArrayList<com.vodafone.v10.sound.SoundTrack>();

	private int volume = 100, panpot = 64;

	public PhraseTrackBase(int id) { ID = id; }

	public void play()
	{
		play(1);
		paused = false;
	}

	public void play(int loop)
	{
		if(player == null) { throw new RuntimeException("Cannot play: null player"); }
		if(loop == 0) { loop = -1; } // Loop as 0 means infinite looping here

		player.setLoopCount(loop);
		player.setMediaTime(0); // Play starts from the beginning of the track
		((PlatformPlayer)player).setPhraseListener(listener);
		player.start();
		((PlatformPlayer.volumeControl)player.getControl("VolumeControl")).setPanpot(panpot);
		((PlatformPlayer.volumeControl)player.getControl("VolumeControl")).setLevel(volume);

		// Play any currently set slave phrases
		for(int i = 0; i < slavePhrases.size(); i++)         { slavePhrases.get(i).play(loop); }
		for(int i = 0; i < slaveJPhonePhrases.size(); i++)   { slaveJPhonePhrases.get(i).play(loop); }
		for(int i = 0; i < slaveVodafonePhrases.size(); i++) { slaveVodafonePhrases.get(i).play(loop); }
		paused = false;
	}

	public void stop()
	{
		if(player == null) { throw new RuntimeException("Cannot stop: null player"); }

		player.stop();
		player.setMediaTime(0);
		paused = false;

		// Stop any currently set slave phrases
		for(int i = 0; i < slavePhrases.size(); i++)         { slavePhrases.get(i).stop(); }
		for(int i = 0; i < slaveJPhonePhrases.size(); i++)   { slaveJPhonePhrases.get(i).stop(); }
		for(int i = 0; i < slaveVodafonePhrases.size(); i++) { slaveVodafonePhrases.get(i).stop(); }
	}

	public void pause()
	{
		if(player == null) { throw new RuntimeException("Cannot pause: null player"); }

		player.stop();
		paused = true;

		// Pause any currently set slave phrases
		for(int i = 0; i < slavePhrases.size(); i++)         { slavePhrases.get(i).pause(); }
		for(int i = 0; i < slaveJPhonePhrases.size(); i++)   { slaveJPhonePhrases.get(i).pause(); }
		for(int i = 0; i < slaveVodafonePhrases.size(); i++) { slaveVodafonePhrases.get(i).pause(); }
	}

	public void resume()
	{
		if(player == null) { throw new RuntimeException("Cannot resume: null player"); }

		((PlatformPlayer)player).setPhraseListener(listener);
		player.start();
		((PlatformPlayer.volumeControl)player.getControl("VolumeControl")).setPanpot(panpot);
		((PlatformPlayer.volumeControl)player.getControl("VolumeControl")).setLevel(volume);
		paused = false;

		// Resume any currently set slave phrases
		for(int i = 0; i < slavePhrases.size(); i++)         { slavePhrases.get(i).resume(); }
		for(int i = 0; i < slaveJPhonePhrases.size(); i++)   { slaveJPhonePhrases.get(i).resume(); }
		for(int i = 0; i < slaveVodafonePhrases.size(); i++) { slaveVodafonePhrases.get(i).resume(); }
	}

	public int getState()
	{
		if(paused) { return PAUSED; }
		else if(player == null || player.getState() == Player.CLOSED) { return NO_DATA; }
		else if(player.getState() <= Player.PREFETCHED) { return READY; }
		else { return PLAYING; }
	}

	public void setVolume(int value)
	{
		if(value < 0 || value > MAX_VOLUME) { throw new IllegalArgumentException("Value is out of range"); }

		volume = value;
		if(player != null) { ((PlatformPlayer.volumeControl)player.getControl("VolumeControl")).setLevel(volume); }
	}

	public int getVolume() { return volume; }

	public void setPanpot(int value)
	{
		if(value < 0 || value > 127) { throw new IllegalArgumentException("Value is out of range"); }

		panpot = value;
		if(player != null) { ((PlatformPlayer.volumeControl)player.getControl("VolumeControl")).setPanpot(panpot); }
	}

	public int getPanpot() { return panpot; }

	public void mute(boolean mute)
	{
		if(player != null) { ((PlatformPlayer.volumeControl)player.getControl("VolumeControl")).setMute(mute); }
	}

	public boolean isMute()
	{
		return player == null ? false : ((PlatformPlayer.volumeControl)player.getControl("VolumeControl")).isMuted();
	}

	public int getID() { return ID; }

	public void setEventListener(PhraseTrackListener l) { listener = l; }

	public void setPhrase(Phrase p)
	{
		if(getState() == PLAYING) { throw new RuntimeException("Cannot set Phrase when the player is running"); }
		if(p == null) { throw new NullPointerException("Cannot set a null phrase"); }

		try
		{
			phrase = p;
			player = Manager.createPlayer(new ByteArrayInputStream(phrase.getData()), "");
			player.prefetch();
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_WARNING, PhraseTrackBase.class.getPackage().getName() + "." + PhraseTrackBase.class.getSimpleName() + ": " + "Failed to create Player from phrase data :" + e.getMessage());
			throw new RuntimeException("Failed to create Player from phrase data");
		}
	}

	public void setAudioPhrase(AudioPhrase p)
	{
		if(getState() == PLAYING) { throw new RuntimeException("Cannot set AudioPhrase when the player is running"); }
		if(p == null) { throw new NullPointerException("Cannot set a null AudioPhrase"); }

		try
		{
			audioPhrase = p;
			player = Manager.createPlayer(new ByteArrayInputStream(audioPhrase.getData()), "");
			player.prefetch();
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_WARNING, PhraseTrackBase.class.getPackage().getName() + "." + PhraseTrackBase.class.getSimpleName() + ": " + "Failed to create Player from phrase data :" + e.getMessage());
			throw new RuntimeException("Failed to create Player from phrase data");
		}
	}

	// The player can only contain a Phrase or an AudioPhrase at any given time
	public void removePhrase()
	{
		if(getState() == PLAYING) { throw new RuntimeException("Cannot remove Phrase when the player is running"); }
		if(player != null) { player.close(); }
		this.phrase = null;
	}

	public void removeAudioPhrase()
	{
		if(getState() == PLAYING) { throw new RuntimeException("Cannot remove AudioPhrase when the player is running"); }
		if(player != null) { player.close(); }
		this.audioPhrase = null;
	}


	public void setPhraseSyncMaster(PhraseTrack master)
	{
		if(getState() == PLAYING || phraseSyncMaster != null || !slavePhrases.isEmpty()) { return; }
		phraseSyncMaster = master;
	}

	public void setJPhoneSyncMaster(com.j_phone.amuse.PhraseTrack master)
	{
		if(getState() == PLAYING || jPhoneSyncMaster != null || !slaveJPhonePhrases.isEmpty()) { return; }
		jPhoneSyncMaster = master;
	}

	public void setVodafoneSyncMaster(com.vodafone.v10.sound.SoundTrack master)
	{
		if(getState() == PLAYING || vodafoneSyncMaster != null || !slaveVodafonePhrases.isEmpty()) { return; }
		vodafoneSyncMaster = master;
	}

	public PhraseTrack getPhraseSyncMaster() { return phraseSyncMaster; }

	public com.j_phone.amuse.PhraseTrack getJPhoneSyncMaster() { return jPhoneSyncMaster; }

	public com.vodafone.v10.sound.SoundTrack getVodafoneSyncMaster() { return vodafoneSyncMaster; }

	// J_Phone's extensions
	public boolean isPlaying() { return getState() == PLAYING; }
}
