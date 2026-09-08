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
package org.recompile.mobile.players;

import java.io.InputStream;

import javax.sound.midi.Sequence;

import javax.microedition.media.Player;

import org.recompile.mobile.PlatformPlayer;

public class BasicPlayer
{
	PlatformPlayer platform = null;

	public void start() {  }
	public void stop() {  }
	public void setLoopCount(int count) {  }
	public long setMediaTime(long now) { return now; }
	public long getMediaTime() { return 0; }
	public boolean isRunning() { return false; }
	public void deallocate() {  }
	public void close() { }
	public void realize() { }
	public void prefetch() { }
	public long getDuration() { return Player.TIME_UNKNOWN; }

	public void setPlatform(PlatformPlayer p) { this.platform = p; }

	// For sequence players
	public Sequence getSequence() { return null; }
}
