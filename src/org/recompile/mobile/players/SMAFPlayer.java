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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;

import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformPlayer;

public class SMAFPlayer extends BasicPlayer implements MetaEventListener, LineListener
{
	// For SMAF, the Sequenced data will dictate player events
	public Sequencer midi;
	public Synthesizer synthesizer;
	public Receiver receiver;

	private int synthIdx = -1;
	private boolean synthReserved = false;

	private Sequence midiSequence;
	private boolean hasMidiPlaybackEvents = false;

	private volatile int numLoops = 0;
	private volatile boolean isExplicitStop = false;
	private long curTime = 0;
	private boolean sequencerLoopConfigured = false;

	private long pendingLoopStart = 0;
	private long pendingLoopEnd = -1;
	private int pendingLoopCount = 0;

	// Meanwhile, sampled data will be treated like "additional" instruments
	private boolean isPlaying = false;
	private AudioInputStream[] wavStreams = null;
	public Clip[] wavClips = null;
	private final Object pcmClipLock = new Object();
	final Set<Integer> playedPositions = new HashSet<Integer>();
	private Map<Integer, Integer> pcmPositions, pcmVelocities;

	private ScheduledFuture<?> playbackTask;

	public SMAFPlayer(InputStream midiStream, InputStream[] wavStreams, Map<Integer, Integer> pcmPositions, Map<Integer, Integer> pcmVelocities)
	{
		if (PlatformPlayer.smafExecutor == null)
		{
			synchronized (PlatformPlayer.class)
			{
				if (PlatformPlayer.smafExecutor == null)
				{
					PlatformPlayer.smafExecutor = Executors.newScheduledThreadPool(2);
				}
			}
		}

		try
		{
			midiSequence = MidiSystem.getSequence(midiStream);
			hasMidiPlaybackEvents = hasMidiPlaybackEvents(midiSequence);
			if (wavStreams != null && wavStreams.length > 0)
			{
				this.wavStreams = new AudioInputStream[wavStreams.length];
				this.pcmPositions = pcmPositions;
				this.pcmVelocities = pcmVelocities;
				for (int i = 0; i < wavStreams.length; i++)
				{
					if (wavStreams[i] == null) { continue; }
					this.wavStreams[i] = AudioSystem.getAudioInputStream(wavStreams[i]);
				}
			}
			PlatformPlayer.addPlayerToStack(null, null, null, this);
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, SMAFPlayer.class.getPackage().getName() + "." + SMAFPlayer.class.getSimpleName() + ": " + ": Couldn't load SMAF data: " + e.getMessage());
		}
	}

	@Override
	public void realize() { platform.state = Player.REALIZED; }

	@Override
	public void prefetch()
	{
		try
		{
			if (!hasMidiPlaybackEvents && !hasPcmStreams())
			{
				platform.state = Player.PREFETCHED;
				return;
			}

			configurePlayback();

			synchronized (pcmClipLock)
			{
				if (wavStreams != null && wavClips == null) { wavClips = new Clip[wavStreams.length]; }
			}

			platform.state = Player.PREFETCHED;
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, SMAFPlayer.class.getPackage().getName() + "." + SMAFPlayer.class.getSimpleName() + ": " + ": Could not prefetch: " + e.getMessage());
			platform.state = Player.UNREALIZED;
			e.printStackTrace();
		}
	}

	@Override
	public void start()
	{
		isExplicitStop = false;
		try
		{
			if (!hasMidiPlaybackEvents && !hasPcmStreams())
			{
				platform.state = Player.PREFETCHED;
				platform.notifyListeners(PlayerListener.END_OF_MEDIA, getMediaTime());
				return;
			}

			platform.state = Player.STARTED;
			platform.notifyListeners(PlayerListener.STARTED, getMediaTime());

			if ((hasMidiPlaybackEvents || hasPcmStreams()) && (!synthReserved || midi.getSequence() == null))
			{
				prepareMidiSubsystem();

				synchronized (this.midi)
				{
					if (this.midi.isRunning()) { this.midi.stop(); }

					this.midi.setSequence(midiSequence);
					this.midi.removeMetaEventListener(this);
					this.midi.addMetaEventListener(this);

					if (curTime >= getDuration()) { setMediaTime(0); }
					else { setMediaTime(curTime); }

					this.midi.start();
				}
			}

			isPlaying = true;
			startPcmScheduler();
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, SMAFPlayer.class.getPackage().getName() + "." + SMAFPlayer.class.getSimpleName() + ": " + ": Failed to start SMAF: " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public void stop()
	{
		isExplicitStop = true;
		if (this.midi != null)
		{
			synchronized (this.midi)
			{
				if (this.midi.isRunning()) { this.midi.stop(); }
				this.midi.removeMetaEventListener(this);
			}
		}

		stopPcmScheduler();
		stopPcmClips();
		curTime = getMediaTime();
		releaseMidiSubsystem();

		isPlaying = false;
		platform.state = Player.PREFETCHED;
		platform.notifyListeners(PlayerListener.STOPPED, curTime);
	}

	@Override
	public void deallocate()
	{
		stop();

		synchronized (pcmClipLock)
		{
			if (wavClips != null)
			{
				for (int i = 0; i < wavClips.length; i++)
				{
					if (wavClips[i] != null)
					{
						wavClips[i].stop();
						wavClips[i].close();
						wavClips[i] = null;
					}
				}
				wavClips = null;
			}
		}
		isPlaying = false;
	}

	@Override
	public void close()
	{
		deallocate();
		midiSequence = null;

		if (wavStreams != null)
		{
			for (int i = 0; i < wavStreams.length; i++)
			{
				wavStreams[i] = null;
			}
		}

		isPlaying = false;
	}

	private void prepareMidiSubsystem()
	{
		if (!synthReserved)
		{
			this.synthIdx = Manager.retrieveAvailableSynthIndex();
			this.synthesizer = Manager.exclusiveSynths[synthIdx];
			this.receiver = Manager.exclusiveReceivers[synthIdx];
			this.midi = Manager.exclusiveSequencers[synthIdx];
			this.synthReserved = true;
		}
	}

	private void releaseMidiSubsystem()
	{
		if (synthReserved)
		{
			synchronized (this.midi)
			{
				Manager.releaseSynthIndex(synthIdx);
				synthReserved = false;
				this.midi = null;
				this.synthesizer = null;
				this.receiver = null;
				this.synthIdx = -1;
			}
		}
	}

	private void startPcmScheduler()
	{
		if (wavClips == null || pcmPositions == null || pcmPositions.isEmpty()) { return; }

		stopPcmScheduler(); // Cancel any lingering tasks

		playbackTask = PlatformPlayer.smafExecutor.scheduleAtFixedRate(new Runnable()
		{
			@Override
			public void run()
			{
				if (!isPlaying) { return; }

				int mediaTime = (int) (getMediaTime() / 1000);

				for (Map.Entry<Integer, Integer> entry : pcmPositions.entrySet())
				{
					Integer position = entry.getKey();
					if (position < mediaTime && !playedPositions.contains(position))
					{
						playPcmStream(entry.getValue(), pcmVelocities.get(position));
						playedPositions.add(position);
					}
				}
			}
		}, 0, 5, TimeUnit.MILLISECONDS);
	}

	private void stopPcmScheduler()
	{
		if (playbackTask != null)
		{
			playbackTask.cancel(false);
			playbackTask = null;
		}
	}

	private void playPcmStream(int pcmIndex, int velocity)
	{
		synchronized (pcmClipLock)
		{
			if (wavStreams == null || pcmIndex < 0 || pcmIndex >= wavClips.length || wavStreams[pcmIndex] == null) { return; }

			try
			{
				Clip target = wavClips[pcmIndex];

				if (target == null)
				{
					wavStreams[pcmIndex].reset();
					target = AudioSystem.getClip();
					target.open(wavStreams[pcmIndex]);
					target.addLineListener(this);
					wavClips[pcmIndex] = target;
				}

				// Target ONLY the requested clip rather than iterating and flushing all active clips
				if (target.isRunning()) { target.stop(); }

				FloatControl volumeControl = (FloatControl) target.getControl(FloatControl.Type.MASTER_GAIN);
				float dB = -30.0f + ((velocity / 127.0f) * (30.0f));
				if (dB > 6.0f) { dB = 6.0f; }
				volumeControl.setValue(dB);

				target.setFramePosition(0);
				target.start();
			 }
			catch (Exception e)
			{
				Mobile.log(Mobile.LOG_ERROR, SMAFPlayer.class.getPackage().getName() + "." + SMAFPlayer.class.getSimpleName() + ": Failed playing PCM stream: " + e.getMessage());
			}
		}
	}

	@Override
	public void setLoopCount(int count)
	{
		/*
		 * Treat cases where an app wants this stream to loop continuously.
		 * Here, count = 1 means it should loop one time, whereas in j2me
		 * it appears that count = 1 means no loop at all, at least based
		 * on Gameloft games that set effects and some music with count = 1
		 */
		if (count == Clip.LOOP_CONTINUOUSLY) { numLoops = count; }
		else { numLoops = count - 1; }
	}

	@Override
	public long setMediaTime(long now)
	{
		if (now >= getDuration()) { now = getDuration(); }
		else if (now < 0) { now = 0; }

		curTime = now;

		try
		{
			synchronized (pcmClipLock)
			{
				if (wavClips != null)
				{
					// Clear all played positions so PCM can play again if rewinding.
					playedPositions.clear();
					for (int i = 0; i < wavClips.length; i++)
					{
						if (wavClips[i] != null) { wavClips[i].setMicrosecondPosition(0); }
					}
				}
			}

			if (this.midi != null)
			{
				synchronized (this.midi)
				{
					this.midi.setMicrosecondPosition(now);
				}
			}
		}
		catch (Exception e)
		{
		Mobile.log(Mobile.LOG_ERROR, SMAFPlayer.class.getPackage().getName() + "." + SMAFPlayer.class.getSimpleName() + ": " + ": Failed to set position: " + e.getMessage());
		}

		/*
		 * MicrosecondPosition doesn't guarantee perfect precision, so return the new
		 * effective position according to the stream.
		 */
		return getMediaTime();
	}

	@Override
	public long getMediaTime()
	{
		if (this.midi != null)
		{
			synchronized (this.midi)
			{
				curTime = this.midi.getMicrosecondPosition();
			}
		}
		return curTime;
	}

	@Override
	public long getDuration()
	{
		if (midiSequence != null) { return midiSequence.getMicrosecondLength(); }
		return Player.TIME_UNKNOWN;
	}

	@Override
	public boolean isRunning() { return isPlaying; }

	public Sequence getSequence() { return midiSequence; }

	protected long getSequenceTick()
	{
		if (this.midi != null)
		{
			synchronized (this.midi)
			{
				return this.midi.getTickPosition();
			}
		}
		return 0L;
	}

	public void setSequence(InputStream sequence)
	{
		try
		{
			midiSequence = MidiSystem.getSequence(sequence);
			hasMidiPlaybackEvents = hasMidiPlaybackEvents(midiSequence);
			sequencerLoopConfigured = false;
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, SMAFPlayer.class.getPackage().getName() + "." + SMAFPlayer.class.getSimpleName() + ": " + ": Failed to set MIDI sequence: " + e.getMessage());
		}
	}

	private boolean hasPcmStreams()
	{
		if (wavStreams == null) { return false; }
		for (int i = 0; i < wavStreams.length; i++)
		{
			if (wavStreams[i] != null) { return true; }
		}
		return false;
	}

	private boolean hasMidiPlaybackEvents(Sequence sequence)
	{
		if (sequence == null) { return false; }
		Track[] tracks = sequence.getTracks();
		for (int t = 0; t < tracks.length; t++)
		{
			Track track = tracks[t];
			for (int i = 0; i < track.size(); i++)
			{
				MidiEvent event = track.get(i);
				if (!(event.getMessage() instanceof ShortMessage)) { continue; }
				ShortMessage message = (ShortMessage) event.getMessage();
				if (message.getCommand() == ShortMessage.NOTE_ON && message.getData2() > 0) { return true; }
			}
		}
		return false;
	}

	protected void onMeta(MetaMessage meta) { }

	protected void stopPcmClips()
	{
		synchronized (pcmClipLock)
		{
			if (wavClips == null) { return; }
			for (int i = 0; i < wavClips.length; i++)
			{
				if (wavClips[i] == null) { continue; }
				wavClips[i].stop();
			}
		}
	}

	protected void setLoop(long loopStartTick, long loopEndTick, int repeatCount)
	{
		sequencerLoopConfigured = true;
		pendingLoopStart = loopStartTick;
		pendingLoopEnd = loopEndTick;
		pendingLoopCount = repeatCount < 0 ? Sequencer.LOOP_CONTINUOUSLY : repeatCount;

		if (this.midi != null)
		{
			synchronized (this.midi)
			{
				this.midi.setLoopStartPoint(loopStartTick);
				this.midi.setLoopEndPoint(loopEndTick);
				this.midi.setLoopCount(pendingLoopCount);
			}
		}
	}

	protected void configurePlayback() throws InvalidMidiDataException { }

	@Override
	public void meta(MetaMessage meta)
	{
		onMeta(meta);
		if (meta.getType() == 0x2F) // END_OF_MEDIA
		{
			// Do this on a thread, otherwise we risk deadlocking on the Java
			// Sound EDT.
			platform.ASYNC_DISPATCHER.submit(new Runnable()
			{
				@Override
				public void run()
				{
					if (isExplicitStop || midi == null) { return; }

					stopPcmScheduler();
					curTime = getMediaTime();

					isPlaying = false;
					platform.state = Player.PREFETCHED;

					if (sequencerLoopConfigured)
					{
						releaseMidiSubsystem();
						platform.notifyListeners(PlayerListener.END_OF_MEDIA, curTime);
					}
					else if (numLoops != 0)
					{
						platform.notifyListeners(PlayerListener.LOOPED, curTime);
						if (numLoops > 0) { numLoops--; }
						setMediaTime(0);
						start();
					}
					else
					{
						releaseMidiSubsystem();
						platform.notifyListeners(PlayerListener.END_OF_MEDIA, curTime);
					}
				}
			});
		}
	}

	@Override
	public void update(LineEvent event)
	{
		if (event.getType() == LineEvent.Type.STOP)
		{
			final Clip clip = (Clip) event.getLine();

			// Same as above, don't risk deadlocking on the Sound EDT.
			PlatformPlayer.smafExecutor.submit(new Runnable()
			{
				@Override
				public void run()
				{
					synchronized (pcmClipLock)
					{
						if (wavClips != null)
						{
							for (int i = 0; i < wavClips.length; i++)
							{
								if (wavClips[i] == clip)
								{
									wavClips[i] = null;
									break;
								}
							}
						}
						clip.removeLineListener(SMAFPlayer.this);
						clip.close();
					}
				}
			});
		}
	}
}
