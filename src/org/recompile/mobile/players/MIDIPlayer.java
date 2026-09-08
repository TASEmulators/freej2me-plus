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

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;

import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;

/* Patcher for MIDI files with running status bytes */
import javax.microedition.media.decoders.MIDIPatcher;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformPlayer;

public class MIDIPlayer extends BasicPlayer implements MetaEventListener
{
	public Sequencer midi;
	private Sequence midiSequence;
	public Synthesizer synthesizer;

	private int synthIdx = -1;
	private boolean synthReserved = false;
	public Receiver receiver;

	private Transmitter transmitter;
	private volatile int numLoops = 0;
	private volatile boolean isExplicitStop = false;
	private long curTime = 0;

	public MIDIPlayer() // For when a Locator call (usually for tones) is issued
	{
		Mobile.log(Mobile.LOG_WARNING, MIDIPlayer.class.getPackage().getName() + "." + MIDIPlayer.class.getSimpleName() + ": " + "Midi Player [locator] untested");

		// Create an empty sequence, which should be overriden with whatever setSequence() receives.
		try
		{
			midiSequence = new Sequence(Sequence.PPQ, 24);
			PlatformPlayer.addPlayerToStack(this, null, null, null);
		}
		catch (Exception e) {  Mobile.log(Mobile.LOG_ERROR, MIDIPlayer.class.getPackage().getName() + "." + MIDIPlayer.class.getSimpleName() + ": " + "Couldn't load midi file:" + e.getMessage()); }
	}

	public MIDIPlayer(InputStream stream)
	{
		byte[] midiData = null;
		try
		{
			midiData = new byte[stream.available()];
			stream.read(midiData, 0, stream.available());

			midiSequence = MidiSystem.getSequence(new ByteArrayInputStream(midiData));
			PlatformPlayer.addPlayerToStack(this, null, null, null);
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_WARNING, MIDIPlayer.class.getPackage().getName() + "." + MIDIPlayer.class.getSimpleName() + ": " + "Couldn't load MIDI file: " + e.getMessage() + ". Trying to patch running status bytes...");
			try
			{
				midiSequence = MidiSystem.getSequence(MIDIPatcher.patchMIDIFile(midiData));
				Mobile.log(Mobile.LOG_INFO, MIDIPlayer.class.getPackage().getName() + "." + MIDIPlayer.class.getSimpleName() + ": " + "MIDI patching succeeded!");
			}
			catch(Exception ie) { Mobile.log(Mobile.LOG_ERROR, MIDIPlayer.class.getPackage().getName() + "." + MIDIPlayer.class.getSimpleName() + ": " + "Couldn't patch MIDI file: " + ie.getMessage() + ". Defaulting midi data to null"); e.printStackTrace(); }
		}
	}

	@Override
	public void realize() { platform.state = Player.REALIZED; }

	@Override
	public void prefetch() { platform.state = Player.PREFETCHED; }

	@Override
	public void start()
	{
		isExplicitStop = false;
		try
		{
			if (!synthReserved || midi == null || midi.getSequence() == null) { prepareMidiSubsystem(); }

			platform.state = Player.STARTED;
			platform.notifyListeners(PlayerListener.STARTED, getMediaTime());

			synchronized (this.midi)
			{
				if (this.midi.isRunning()) { this.midi.stop(); }

				this.midi.setSequence(midiSequence);
				this.midi.removeMetaEventListener(this);
				this.midi.addMetaEventListener(this);

				// If mediaTime >= getDuration, we should start playing from the beginning
				if(curTime >= getDuration()) { setMediaTime(0); }
				else { setMediaTime(curTime); } // Else, resume from where it stopped

				this.midi.start();
			}
		}
		catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, MIDIPlayer.class.getPackage().getName() + "." + MIDIPlayer.class.getSimpleName() + ": " + "Failed to clean MIDI sequencer and start playback:" + e.getMessage()); e.printStackTrace(); }
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
		curTime = getMediaTime();
		releaseMidiSubsystem();
		platform.state = Player.PREFETCHED;
		platform.notifyListeners(PlayerListener.STOPPED, getMediaTime());
	}

	@Override
	public void deallocate() { stop(); }

	@Override
	public void close() { midiSequence = null; }

	@Override
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

	@Override
	public long setMediaTime(long now)
	{
		if (now >= getDuration()) { now = getDuration(); }
		else if (now < 0) { now = 0; }

		curTime = now;

		if(this.midi != null)
		{
			synchronized(this.midi)
			{
				try { this.midi.setMicrosecondPosition(now); }
				catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, MIDIPlayer.class.getPackage().getName() + "." + MIDIPlayer.class.getSimpleName() + ": " + "Failed to set MIDI position:" + e.getMessage()); }
			}
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
		if (midiSequence != null)
		{
			return midiSequence.getMicrosecondLength();
		}
		return Player.TIME_UNKNOWN;
	}

	@Override
	public boolean isRunning()
	{
		if (this.midi != null)
		{
			synchronized (this.midi)
			{
				return this.midi.isRunning();
			}
		}
		return false;
	}

	public Sequence getSequence() { return midiSequence; }

	public void setSequence(InputStream sequence)
	{
		try { midiSequence = MidiSystem.getSequence(sequence); }
		catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, MIDIPlayer.class.getPackage().getName() + "." + MIDIPlayer.class.getSimpleName() + ": " + "Failed to set MIDI sequence:" + e.getMessage());  }
		finally
		{
			curTime = 0;
			try { if (sequence != null) sequence.close(); }
			catch (Exception e) { }
		}
	}

	private void prepareMidiSubsystem() throws MidiUnavailableException, InvalidMidiDataException
	{
		if(!synthReserved)
		{
			this.synthIdx = Manager.retrieveAvailableSynthIndex();
			this.synthesizer = Manager.exclusiveSynths[synthIdx];
			this.receiver = Manager.exclusiveReceivers[synthIdx];
			this.midi = Manager.exclusiveSequencers[synthIdx];
			transmitter = this.midi.getTransmitter();
			transmitter.setReceiver(receiver);
			synthReserved = true;
		}
	}

	private void releaseMidiSubsystem()
	{
		if (synthReserved)
		{
			synchronized (this.midi)
			{
				if (this.transmitter != null)
				{
					this.transmitter.close();
					this.transmitter = null;
				}
				Manager.releaseSynthIndex(synthIdx);
				synthReserved = false;
				this.midi = null;
				this.synthesizer = null;
				this.receiver = null;
				this.synthIdx = -1;
			}
		}
	}

	@Override
	public void meta(MetaMessage meta)
	{
		if (meta.getType() == 0x2F) // 0x2F = END_OF_MEDIA in Sequencer
		{
			// Do this on a thread, otherwise we risk deadlocking on the Java
			// Sound EDT.
			platform.ASYNC_DISPATCHER.submit(new Runnable()
			{
				@Override
				public void run()
				{
					if (isExplicitStop || midi == null) { return; }

					platform.state = Player.PREFETCHED;
					curTime = getMediaTime();

					if (numLoops != 0)
					{
						platform.notifyListeners(PlayerListener.LOOPED, getMediaTime());
						if (numLoops > 0) { numLoops--; } // If numLoops = -1, we're looping indefinitely
						setMediaTime(0);
						start();
					}
					else
					{
						releaseMidiSubsystem();
						platform.notifyListeners(PlayerListener.END_OF_MEDIA, getMediaTime());
					}
				}
			});
		}
	}
}
