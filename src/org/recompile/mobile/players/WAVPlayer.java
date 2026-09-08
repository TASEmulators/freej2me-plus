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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;

import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;

/* IMA ADPCM WAV support */
import javax.microedition.media.decoders.WAVTools;
import javax.microedition.media.decoders.WAVImaADPCMDecoder;
/* WAV a/u-law support */
import javax.microedition.media.decoders.WAVLawDecoder;

import org.recompile.mobile.Mobile;

public class WAVPlayer extends BasicPlayer implements LineListener
{
	/* PCM WAV variables */
	private byte[] tmpStream;
	public Clip wavClip;
	private int[] wavHeaderData = new int[7];
	private volatile int numLoops = 0;
	private volatile boolean isExplicitStop = false;

	public WAVPlayer(InputStream stream)
	{
		/*
		 * A wav header is generally 44-bytes long (up to 60 for IMA ADPCM), and it is what we need to read in order
		 * to get the stream's format, frame size, bit rate, number of channels, etc. which gives us information
		 * on the kind of codec needed to play or decode the incoming stream. The stream needs to be reset
		 * or else PCM files will be loaded without a header and it might cause issues with playback.
		 */
		try
		{
			stream.mark(stream.available());
			wavHeaderData = WAVTools.readHeader(stream);
			stream.reset();
			stream.skip(wavHeaderData[6]);

			tmpStream = new byte[stream.available()];
			stream.read(tmpStream, 0, stream.available());
		} catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, WAVPlayer.class.getPackage().getName() + "." + WAVPlayer.class.getSimpleName() + ": " + "Could not prepare wav stream:" + e.getMessage()); }
	}

	public void realize() { platform.state = Player.REALIZED; }

	public void prefetch()
	{
		try
		{
			if(wavClip == null)
			{
				wavClip = AudioSystem.getClip();
				/* Like for midi, we need to listen for END_OF_MEDIA events here too. */
				wavClip.addLineListener(this);
			}

			if(!wavClip.isOpen())
			{
				/* Process the wave data */
				if(wavHeaderData[0] == 1) // standard PCM WAV, just upsample it
				{
					wavClip.open(AudioSystem.getAudioInputStream(new ByteArrayInputStream(WAVTools.upsample(tmpStream, wavHeaderData[1], WAVTools.hostSampleRate, (short) wavHeaderData[2], (short) wavHeaderData[4], wavHeaderData[5]))));
				}
				else if(wavHeaderData[0] == 6) // A-Law GSM WAV
				{
					wavClip.open(AudioSystem.getAudioInputStream(new ByteArrayInputStream(WAVLawDecoder.decodeALaw(tmpStream, wavHeaderData))));
				}
				else if(wavHeaderData[0] == 7) // u-Law GSM WAV
				{
					wavClip.open(AudioSystem.getAudioInputStream(new ByteArrayInputStream(WAVLawDecoder.decodeULaw(tmpStream, wavHeaderData))));
				}
				else if(wavHeaderData[0] == 17) // IMA ADPCM
				{
					wavClip.open(AudioSystem.getAudioInputStream(new ByteArrayInputStream(WAVImaADPCMDecoder.decodeImaAdpcm(new ByteArrayInputStream(tmpStream), wavHeaderData))));
				}
				else /* Unknown format. */
				{
					Mobile.log(Mobile.LOG_WARNING, WAVPlayer.class.getPackage().getName() + "." + WAVPlayer.class.getSimpleName() + ": " + "WAV Format is " + wavHeaderData[0] + " (Unsupported).");
				}
			}

			platform.state = Player.PREFETCHED;
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, WAVPlayer.class.getPackage().getName() + "." + WAVPlayer.class.getSimpleName() + ": " + "Couldn't prefetch wav stream: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void start()
	{
		isExplicitStop = false;
		if(getMediaTime() >= getDuration()) { setMediaTime(0); }

		platform.state = Player.STARTED;
		platform.notifyListeners(PlayerListener.STARTED, getMediaTime());

		wavClip.start();
	}

	public void stop()
	{
		isExplicitStop = true;
		wavClip.stop();
		platform.state = Player.PREFETCHED;
		platform.notifyListeners(PlayerListener.STOPPED, getMediaTime());
	}

	public void deallocate()
	{
		final Clip clip = this.wavClip;

		if (clip != null)
		{
			platform.ASYNC_DISPATCHER.submit(new Runnable() {
				@Override
				public void run() {
					try
					{
						clip.removeLineListener(WAVPlayer.this);
						if (clip.isRunning()) { clip.stop(); }
						if (clip.isOpen()) { clip.close(); }
					}
					catch (Exception e) { }
				}
			});
		}
		this.wavClip = null;
	}

	public void close()
	{
		tmpStream = null;
		wavHeaderData = null;
	}

	public void setLoopCount(int count)
	{
		/*
		 * Treat cases where an app wants this stream to loop continuously.
		 * Here, count = 1 means it should loop one time, whereas in j2me
		 * it appears that count = 1 means no loop at all, at least based
		 * on Gameloft games that set effects and some music with count = 1
		 */
		if(count == Clip.LOOP_CONTINUOUSLY) { numLoops = count; }
		else { numLoops = count-1; }
	}

	public long setMediaTime(long now)
	{
		if(now >= getDuration()) { wavClip.setMicrosecondPosition(getDuration()); }
		else if(now < 0) { wavClip.setMicrosecondPosition(0); }
		else { wavClip.setMicrosecondPosition(now);  }

		/*
		 * MicrosecondPosition doesn't guarantee perfect precision, so return the new
		 * effective position according to the stream.
		 */
		return getMediaTime();
	}

	public long getMediaTime() { return wavClip.getMicrosecondPosition(); }

	public long getDuration() { return  wavClip.getMicrosecondLength(); }

	public boolean isRunning() { return wavClip.isRunning(); }

	@Override
	public void update(LineEvent event)
	{
		if (event.getType() == LineEvent.Type.STOP)
		{
			// Do this on a thread, otherwise we risk deadlocking on the Java
			// Sound EDT.
			platform.ASYNC_DISPATCHER.submit(new Runnable()
			{
				@Override
				public void run()
				{
					if (isExplicitStop || wavClip == null) { return; }
					platform.state = Player.PREFETCHED;
					if (numLoops != 0)
					{
						platform.notifyListeners(PlayerListener.LOOPED, getMediaTime());
						if (numLoops > 0) { numLoops--; }
						setMediaTime(0);
						start();
					}
					else
					{
						platform.notifyListeners(PlayerListener.END_OF_MEDIA, getMediaTime());
					}
				}
			});
		}
	}
}
