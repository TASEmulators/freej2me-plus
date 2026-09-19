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

import javax.microedition.media.Player;

import org.recompile.mobile.PlatformPlayer;

class AudioClipImpl extends com.samsung.util.AudioClip implements AudioClip
{
	private static AudioClipImpl playingClip = null;

	private AudioListener listener;
	private int numLoops = 1;

	public AudioClipImpl(byte[] data)
	{
		// We don't care about the clip type here, Sammy's AudioClip figures
		// that out for us.
		super(0, data, 0, data.length);
	}

	public void addAudioListener(AudioListener listener)
	{
		this.listener = listener;
		if(player != null) { ((PlatformPlayer) player).setNecListener(listener, this); }
	}

	public void play() throws IllegalStateException
	{
		if(player.getState() < Player.PREFETCHED) { throw new IllegalStateException("Player not initialized."); }
		if(((PlatformPlayer)player).necListener != listener) { ((PlatformPlayer) player).setNecListener(listener, this); }

		super.play(numLoops, 4); // Max volume

		// Looks like only one clip must play at a time. Pairetsu has streams
		// playing concurrently as soon as you get ingame otherwise.
		if(playingClip != null && playingClip != this) { playingClip.stop(); }
		playingClip = this;
	}

	public void stop() throws IllegalStateException
	{
		if(player.getState() < Player.PREFETCHED) { throw new IllegalStateException("Player not initialized."); }

		super.stop();
	}

	// Apparently time is in milliseconds here, not microseconds.
	public int getLapsedTime() throws IllegalStateException
	{
		if(player.getState() < Player.PREFETCHED) { throw new IllegalStateException("Player not initialized."); }

		return (int) (player.getMediaTime() / 1000);
	}

	public int getTime() throws IllegalStateException
	{
		if(player.getState() < Player.PREFETCHED) { throw new IllegalStateException("Player not initialized."); }

		return (int) (player.getDuration() / 1000);
	}

	public int getChannel() throws IllegalStateException
	{
		if(player.getState() < Player.PREFETCHED) { throw new IllegalStateException("Player not initialized."); }
		// TODO: What is the purpose of this?
		return 0;
	}

	public int getTempo() throws IllegalStateException
	{
		if(player.getState() < Player.PREFETCHED) { throw new IllegalStateException("Player not initialized."); }
		// TODO: Tempo in what notation? MIDP's default getTempo?
		return ((PlatformPlayer.tempoControl)player.getControl("TempoControl")).getTempo();
	}

	public void setLoopCount(int loops) throws IllegalArgumentException, IllegalStateException
	{
		if(player.getState() < Player.PREFETCHED) { throw new IllegalStateException("Player not initialized."); }
		// What is a valid loop range here? Samsung's 0-255 range?
		if(loops < 0 || loops > 255) { throw new IllegalArgumentException("Invalid loop value: " + loops + ". Range is 0-255."); }

		this.numLoops = (loops == 255 || loops == 0) ? -1 : loops; // Treat 0 and 255 loops as infinite looping;
		player.setLoopCount(this.numLoops);
	}

	public AudioListener getAudioListener() { return listener; }
}
