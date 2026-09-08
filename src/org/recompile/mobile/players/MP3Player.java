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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;

/* audio/mpeg support */
import javazoom.jl.player.MPEGPlayer;

import org.recompile.mobile.Mobile;

public class MP3Player extends BasicPlayer
{
	private byte[] tmpStream;
	public MPEGPlayer mp3Player;
	private Thread playerThread = null;
	private volatile boolean mp3PlayerRunning = false;
	private int numLoops = 0;

	public MP3Player(InputStream stream)
	{
		try
		{
			tmpStream = new byte[stream.available()];
			stream.read(tmpStream, 0, stream.available());
		}
		catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, MP3Player.class.getPackage().getName() + "." + MP3Player.class.getSimpleName() + ": " + "Could not prepare mpeg stream:" + e.getMessage());}
	}

	public void realize() { platform.state =  Player.REALIZED; }

	public void prefetch()
	{
		try
		{
			mp3Player = new MPEGPlayer(new ByteArrayInputStream(tmpStream), false);
			platform.state =  Player.PREFETCHED;
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, MP3Player.class.getPackage().getName() + "." + MP3Player.class.getSimpleName() + ": " + "Couldn't prefetch mpeg stream: " + e.getMessage());
			mp3Player.close();
		}
	}

	public void start()
	{
		try
		{
			playerThread = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						mp3PlayerRunning = true;
						while(mp3PlayerRunning)
						{
							if(getMediaTime() >= getDuration()) { setMediaTime(0); }
							else { setMediaTime(getMediaTime()); } // Resume from when last stopped
							mp3Player.play(); // This is thread-blocking, so the code below only executes after this has finished.

							/*
							* Check if mp3Player is still valid and exit early, since this thread can be
							* interrupted and the player can also be closed abruptly.
							*/
							if (mp3Player == null || !mp3PlayerRunning)  { return; }

							if (!Thread.currentThread().isInterrupted())
							{
								platform.state =  Player.PREFETCHED;
								platform.notifyListeners(PlayerListener.END_OF_MEDIA, getMediaTime());
								if(numLoops != 0)
								{
									if(numLoops > 0) { numLoops--; } // If numLoops = -1, we're looping indefinitely
									mp3Player.reset();
									mp3Player.play();
								}
								mp3Player.reset();
								mp3PlayerRunning = false;
							}
						}
					}
					catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, MP3Player.class.getPackage().getName() + "." + MP3Player.class.getSimpleName() + ": " + "Mpeg player runtime error:" + e.getMessage()); }
				}
			});

			platform.state =  Player.STARTED;
			platform.notifyListeners(PlayerListener.STARTED, getMediaTime());

			playerThread.start();
		} catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, MP3Player.class.getPackage().getName() + "." + MP3Player.class.getSimpleName() + ": " + "Couldn't start mpeg player:" + e.getMessage()); }
	}

	public void stop()
	{
		mp3Player.stop();
		mp3PlayerRunning = false;
		platform.state =  Player.PREFETCHED;
		platform.notifyListeners(PlayerListener.STOPPED, getMediaTime());
	}

	public void deallocate()
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				if(mp3Player != null) { mp3Player.close(); }
				mp3Player = null;
			}
		}).start();
	}

	public void close()
	{
		tmpStream = null;
		playerThread = null;
	}

	public void setLoopCount(int count)
	{
		/*
		 * Treat cases where an app wants this stream to loop continuously.
		 * Here, count = 1 means it should loop one time, whereas in j2me
		 * it appears that count = 1 means no loop at all, at least based
		 * on Gameloft games that set effects and some music with count = 1
		 */
		if(count == javax.sound.sampled.Clip.LOOP_CONTINUOUSLY) { numLoops = count; }
		else { numLoops = count-1; }
	}

	public long setMediaTime(long now)
	{
		if(now >= getDuration()) { mp3Player.setMicrosecondPosition(getDuration()); }
		else if(now < 0) { mp3Player.setMicrosecondPosition(0); }
		else { mp3Player.setMicrosecondPosition(now); }

		/*
		 * In MP3Player's case, we don't deal with microsecond resolution, so return the new
		 * effective position converted to microseconds.
		 */
		return getMediaTime();
	}

	public long getMediaTime() { return mp3Player.getMicrosecondPosition(); }

	public long getDuration() { return mp3Player.getDuration(); }

	public boolean isRunning() { return mp3Player.isRunning(); }
}
