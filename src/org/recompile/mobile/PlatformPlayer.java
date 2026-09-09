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
package org.recompile.mobile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import javax.sound.midi.Instrument;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Patch;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;
import javax.sound.sampled.FloatControl;

import com.nokia.mid.sound.Sound;
import com.nokia.mid.sound.SoundListener;

import javax.microedition.media.Control;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.TimeBase;
import javax.microedition.media.control.ToneControl;

/* SMAF decoding support */
import javax.microedition.media.decoders.SMAFDecoder;
/* MLD decoding support */
import javax.microedition.media.decoders.MLDDecoder;
/* Ericsson Melody support */
import javax.microedition.media.decoders.EMSMelodyDecoder;

/* Import all supported player implementations. */
import org.recompile.mobile.players.*;

public class PlatformPlayer implements Player
{
	// DoJa REALLY stresses this out. Akumajou Densetsu keeps more than 64 MLD
	// files in memory.
	private static final BasicPlayer[] sequencePlayers = new BasicPlayer[128];

	// For player disposal handling, so we don't clog the Sound EDT.
	public static final ExecutorService ASYNC_DISPATCHER =
		Executors.newSingleThreadExecutor();

	private final byte NUM_CONTROLS = 4;

	public String contentType = "";

	private BasicPlayer player;

	public int state = Player.UNREALIZED;

	protected Vector<PlayerListener> listeners;
	protected Vector<com.siemens.mp.media.PlayerListener> siemensListeners;
	protected Vector<com.kddi.media.MediaEventListener> kddiListeners;

	private com.kddi.media.MediaPlayerBox kddiPlayerBox;

	public SoundListener nokiaListener;
	private Sound nokiaSound;

	private com.nttdocomo.ui.MediaListener doJaListener;
	private com.nttdocomo.ui.AudioPresenter doJaPresenter;

	private com.jblend.media.smaf.phrase.PhraseTrackListener phraseListener;

	protected boolean disableControls = false; // For when a given audio format is not supported
	protected Control[] controls;

	protected PlatformPlayer() { }

	public PlatformPlayer(InputStream stream, String type)
	{
		listeners = new Vector<PlayerListener>();
		siemensListeners = new Vector<com.siemens.mp.media.PlayerListener>();
		kddiListeners = new Vector<com.kddi.media.MediaEventListener>();
		controls = new Control[NUM_CONTROLS];

		contentType = type;

		if(Mobile.sound == false) { player = new BasicPlayer(); }
		else
		{
			// Midi player will also play tones, as these are converted to midi in pretty much all cases at the moment
			if(contentType.toLowerCase().contains("mid") || contentType.toLowerCase().contains("tone")) { player = new MIDIPlayer(stream); }
			else if(contentType.toLowerCase().contains("wav")) { player = new WAVPlayer(stream); }
			else if(contentType.toLowerCase().contains("mp"))  { player = new MP3Player(stream); } // MP1, MP2, MP3, MPEG, etc. No other J2ME format has those two letters in sequence.
			else /* If the stream doesn't have an accompanying type or its a type we don't have an explicit player for, do everything we can to try and load it */
			{
				try
				{
					final byte[] data = new byte[stream.available()];
					stream.read(data, 0, stream.available());

					if(data.length >= 4 && data[0] == 'M' && data[1] == 'T' && data[2] == 'h' && data[3] == 'd')
					{
						Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Format is MIDI!");
						contentType = "audio/midi";
						player = new MIDIPlayer(new ByteArrayInputStream(data));
					}
					else if (data.length >= 15 && data[8] == 'Q' && data[9] == 'L' && data[10] == 'C' && data[11] == 'M' && data[12] == 'f' && data[13] == 'm' && data[14] == 't')
					{
						// This is for Qualcomm's QCP format, it has to be checked before wav, because Qualcomm's PureVoice also has RIFF as its first bytes
						Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Format is Qualcomm PureVoice! (not supported yet)");
						contentType = "audio/qcp (stub)";
						player = new BasicPlayer();
						disableControls = true;
					}
					else if(data.length >= 4 && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F')
					{
						Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Format is WAV!");
						contentType = "audio/wav";
						player = new WAVPlayer(new ByteArrayInputStream(data));
					}
					else if(data.length >= 3 && data[0] == 'I' && data[1] == 'D' && data[2] == '3' || ((data[0] == (byte) 0xFF) && (data[1] & 0xE0) == 0xE0)) // Check for MPEG files WITH and WITHOUT the ID3 tag
					{
						Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Format is MPEG!");
						contentType = "audio/mpeg";
						player = new MP3Player(new ByteArrayInputStream(data));
					}
					else if((data.length >= 4 && data[0] == 'M' && data[1] == 'M' && data[2] == 'M' && data[3] == 'D') ||
							(data.length >= 6 && data[2] == 'M' && data[3] == 'M' && data[4] == 'M' && data[5] == 'D') // SMAF files go so insanely off-spec it isn't even funny
					)
					{
						Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Format is SMAF/MMF! (not fully supported yet)");
						contentType = "audio/mmf";
						SMAFDecoder.decodeSMAF(data);
						if(SMAFDecoder.SequenceData != null || SMAFDecoder.pcmData != null)
						{
							if(Mobile.dumpAudioStreams)
							{
								if(SMAFDecoder.SequenceData != null) { SMAFDecoder.SequenceData = Manager.dumpAudioStream(SMAFDecoder.SequenceData, contentType); }
								if(SMAFDecoder.pcmData != null)
								{
									for(int i = 0; i < SMAFDecoder.pcmData.size(); i++)
									{
										if(SMAFDecoder.pcmData.get(i) == null) { continue; }
										SMAFDecoder.pcmData.set(i, Manager.dumpAudioStream(SMAFDecoder.pcmData.get(i), contentType));
									}
								}
							}
							player = new SMAFPlayer(SMAFDecoder.SequenceData, SMAFDecoder.pcmData.toArray(new InputStream[0]));
						}
						else { player = new BasicPlayer(); disableControls = true; } // Somehow the SMAF decoder failed, retrieve a stub player

					}
					else if((data.length >= 4 && data[0] == 'm' && data[1] == 'e' && data[2] == 'l' && data[3] == 'o')
							|| (data.length >= 4 && data[0] == 'c' && data[1] == 'm' && data[2] == 'i' && data[3] == 'd'))
					{
						if(data.length >= 4 && data[0] == 'm' && data[1] == 'e' && data[2] == 'l' && data[3] == 'o')
						{
							Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Format is MLD/MFi! (not fully supported yet)");
							contentType = "audio/x-mld";
						}
						else if (data.length >= 4 && data[0] == 'c' && data[1] == 'm' && data[2] == 'i' && data[3] == 'd')
						{
							Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Format is CMF! (not fully supported yet)");
							contentType = "audio/x-cmf";
						}
						MLDDecoder.decodeMLD(data);
						if(MLDDecoder.SequenceData != null || MLDDecoder.pcmData != null)
						{
							if(Mobile.dumpAudioStreams)
							{
								if(MLDDecoder.SequenceData != null) { MLDDecoder.SequenceData = Manager.dumpAudioStream(MLDDecoder.SequenceData, contentType); }
								if(MLDDecoder.pcmData != null)
								{
									for(int i = 0; i < MLDDecoder.pcmData.size(); i++)
									{
										MLDDecoder.pcmData.set(i, Manager.dumpAudioStream(MLDDecoder.pcmData.get(i), contentType));
									}
								}
							}
							player = new MLDPlayer(MLDDecoder.SequenceData, MLDDecoder.pcmData.toArray(new InputStream[0]));
						}
						else { player = new BasicPlayer(); disableControls = true; } // Somehow the MLD decoder failed, retrieve a stub player
					}
					else if(data.length >= 4 && data[0] == 'B' && data[1] == 'E' && data[2] == 'G' && data[3] == 'I' && data[4] == 'N' && data[5] == ':' && data[6] == 'I' && data[7] == 'M')
					{
						Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Format is EMS iMelody! (not fully supported yet)");
						contentType = "audio/x-imy";
						player = new MIDIPlayer(EMSMelodyDecoder.decodeMelody(data));
					}
					else if(data.length >= 4 && data[0] == 'B' && data[1] == 'E' && data[2] == 'G' && data[3] == 'I' && data[4] == 'N' && data[5] == ':' && data[6] == 'E' && data[7] == 'M')
					{
						Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Format is EMS eMelody! (not fully supported yet)");
						contentType = "audio/x-emy";
						player = new MIDIPlayer(EMSMelodyDecoder.decodeMelody(data));
					}
					else if(data.length >= 6 && data[0] == '#' && data[1] == '!' && data[2] == 'A' && data[3] == 'M' && data[4] == 'R' && data[5] == '\n')
					{
						Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Format is AMR-NB! (not supported yet)");
						contentType = "audio/amr (stub)";
						player = new BasicPlayer();
						disableControls = true;
					}
					else if(data.length >= 9 && data[0] == '#' && data[1] == '!' && data[2] == 'A' && data[3] == 'M' && data[4] == 'R' && data[5] == '-' && data[6] == 'W' && data[7] == 'B' && data[8] == '\n')
					{
						Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Format is AMR-WB! (not supported yet)");
						contentType = "audio/amr-wb (stub)";
						player = new BasicPlayer();
						disableControls = true;
					}
					else /* If none of the formats match, we don't know what this is */
					{
						Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "None of the known formats match the received stream, it won't play!");
						player = new BasicPlayer();
						contentType = "unknown";
						disableControls = true;
					}
				}
				catch (IOException e)
				{
					Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Couldn't parse input stream: " + e.getMessage());
				}
			}
		}

		// Set up control interfaces based on player type.
		controls[0] = new volumeControl(this.player);

		player.setPlatform(this);

		/* MIDI Player has a few additional controls */
		if(player instanceof MIDIPlayer || player instanceof SMAFPlayer)
		{
			/* If we're using MIDIPlayer to play tones, only set it up with ToneControl. */
			if(contentType.equalsIgnoreCase("audio/x-tone-seq")) { controls[3] = new toneControl((MIDIPlayer) this.player); }
			else if(player instanceof MIDIPlayer) { controls[2] = new midiControl((MIDIPlayer) this.player); }

			// Tempo control is available for both midi and smaf players
			controls[1] = new tempoControl(this.player);
		}

		Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "media type: " + contentType);
	}

	public PlatformPlayer(String locator)
	{
		if(locator.equals(Manager.TONE_DEVICE_LOCATOR) || locator.equals(Manager.MIDI_DEVICE_LOCATOR))
		{
			Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + " Creating MIDI Player for locator: "+locator);
			player = new MIDIPlayer();
			listeners = new Vector<PlayerListener>();
			siemensListeners = new Vector<com.siemens.mp.media.PlayerListener>();
			kddiListeners = new Vector<com.kddi.media.MediaEventListener>();
			controls = new Control[NUM_CONTROLS];
			controls[0] = new volumeControl(this.player); // Midi Player with Tones might not use this
			controls[3] = new toneControl((MIDIPlayer) this.player);
		}
		else
		{
			Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "No player for locator: "+locator);
			player = new BasicPlayer();
			listeners = new Vector<PlayerListener>();
			siemensListeners = new Vector<com.siemens.mp.media.PlayerListener>();
			kddiListeners = new Vector<com.kddi.media.MediaEventListener>();
			controls = new Control[3];
		}
	}

	public void close()
	{
		if(getState() == Player.CLOSED) { return; }

		try
		{
			if(isRunning()) { stop(); }
			player.deallocate();
			player.close();
			controls = null;
			player = null;
			state = Player.CLOSED;
			notifyListeners(PlayerListener.CLOSED, null);
		}
		catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Could not close player: " + e.getMessage()); e.printStackTrace(); }
	}

	public int getState() { return state; }

	public void start()
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot call start() on a CLOSED player."); }

		if(getState() == Player.UNREALIZED) { realize(); }

		if(getState() == Player.REALIZED) { prefetch(); }

		if(getState() == Player.PREFETCHED) { player.start(); }
	}

	public void stop()
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot call stop() on a CLOSED player."); }

		if(getState() == Player.STARTED)
		{
			try { player.stop(); }
			catch (Exception e) { }
		}
	}

	public void addPlayerListener(PlayerListener playerListener)
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot add PlayerListener to a CLOSED player"); }
		if(playerListener == null) { return; }

		listeners.add(playerListener);
	}

	public void setSoundListener(Sound sound, SoundListener listener) // Nokia Sound only has methods to set the Sound Listener, not remove it
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot add SoundListener to an UNINITIALIZED Sound"); }
		if(listener == null) { return; }

		nokiaListener = listener;
		nokiaSound = sound;
	}

	public void removePlayerListener(PlayerListener playerListener)
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot remove PlayerListener from a CLOSED player"); }
		if(playerListener == null) { return; }

		listeners.remove(playerListener);
	}

	// Siemens listeners
	public void addPlayerListener(com.siemens.mp.media.PlayerListener playerListener)
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot remove PlayerListener from a CLOSED player"); }
		if(playerListener == null) { return; }

		siemensListeners.add(playerListener);
	}

	public void removePlayerListener(com.siemens.mp.media.PlayerListener playerListener)
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot remove PlayerListener from a CLOSED player"); }
		if(playerListener == null) { return; }

		siemensListeners.remove(playerListener);
	}

	// KDDI listeners
	public void addMediaPlayerBox(com.kddi.media.MediaPlayerBox box) { this.kddiPlayerBox = box; }

	public void addPlayerListener(com.kddi.media.MediaEventListener playerListener)
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot remove PlayerListener from a CLOSED player"); }
		if(playerListener == null) { return; }

		kddiListeners.add(playerListener);
	}

	public void removePlayerListener(com.kddi.media.MediaEventListener playerListener)
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot remove PlayerListener from a CLOSED player"); }
		if(playerListener == null) { return; }

		kddiListeners.remove(playerListener);
	}

	//DoJa listeners
	public void setDoJaListener(com.nttdocomo.ui.MediaListener listener, com.nttdocomo.ui.AudioPresenter presenter) // DoJa behaves akin to Nokia in how it sets listeners
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot add SoundListener to an UNINITIALIZED Sound"); }
		if(listener == null) { return; }

		doJaListener = listener;
		doJaPresenter = presenter;
	}

	// JBlend/Vodafone/KDDI, etc Phrase listeners
	public void setPhraseListener(com.jblend.media.smaf.phrase.PhraseTrackListener listener)
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot add SoundListener to an UNINITIALIZED Sound"); }

		if(listener == null) { return; }

		phraseListener = listener;
	}

	public void notifyListeners(String event, Object eventData)
	{
		// Paused events will never happen for these three

		for(int i=0; i<listeners.size(); i++)
		{
			if(event == PlayerListener.LOOPED) { event = PlayerListener.END_OF_MEDIA; }
			listeners.get(i).playerUpdate(this, event, eventData);
		}

		for(int i=0; i < siemensListeners.size(); i++)
		{
			if(event == PlayerListener.LOOPED) { event = PlayerListener.END_OF_MEDIA; }
			siemensListeners.get(i).playerUpdate((com.siemens.mp.media.Player) this, event, eventData);
		}

		if(nokiaListener != null)
		{
			if(event == PlayerListener.CLOSED) { nokiaListener.soundStateChanged(nokiaSound, Sound.SOUND_UNINITIALIZED); }
			else if(event == PlayerListener.STARTED) { nokiaListener.soundStateChanged(nokiaSound, Sound.SOUND_PLAYING); }
			else if(event == PlayerListener.STOPPED || event == PlayerListener.END_OF_MEDIA || event == PlayerListener.LOOPED) { nokiaListener.soundStateChanged(nokiaSound, Sound.SOUND_STOPPED); }
		}

		// These are more robust and have additional events

		if(kddiListeners.size() > 0)
		{
			int kddiEvent = 0;
			int kddiData = getState() == Player.CLOSED ? 0 : (int) getMediaTime(); // This is an optional integer argument according to KDDI docs, but let's send the current media time nonetheless
			if(event == PlayerListener.STARTED && kddiData == 0) { kddiEvent = com.kddi.media.MediaPlayerBox.PLAY; }
			if(event == PlayerListener.STARTED && kddiData != 0) { kddiEvent = com.kddi.media.MediaPlayerBox.RESUME; }
			else if(event == PlayerListener.CLOSED) { kddiEvent = com.kddi.media.MediaPlayerBox.STOP; }
			else if(event == PlayerListener.STOPPED) { kddiEvent = com.kddi.media.MediaPlayerBox.PAUSE; }
			else if(event == PlayerListener.END_OF_MEDIA || event == PlayerListener.LOOPED) { kddiEvent = com.kddi.media.MediaPlayerBox.PAUSE; }

			for(int i=0; i < kddiListeners.size(); i++)
			{
				kddiListeners.get(i).stateChanged(this.kddiPlayerBox, kddiEvent, kddiData);
			}
		}

		if(doJaListener != null)
		{
			int doJaData = getState() == Player.CLOSED ? 0 : (int) getMediaTime();
			if(event == PlayerListener.CLOSED) { doJaListener.mediaAction(doJaPresenter, com.nttdocomo.ui.AudioPresenter.AUDIO_STOPPED, doJaData); }
			else if(event == PlayerListener.STARTED && doJaData == 0) { doJaListener.mediaAction(doJaPresenter, com.nttdocomo.ui.AudioPresenter.AUDIO_PLAYING, doJaData); }
			else if(event == PlayerListener.STARTED && doJaData != 0) { doJaListener.mediaAction(doJaPresenter, com.nttdocomo.ui.AudioPresenter.AUDIO_RESTARTED, doJaData); }
			else if(event == PlayerListener.STOPPED)  { doJaListener.mediaAction(doJaPresenter, com.nttdocomo.ui.AudioPresenter.AUDIO_PAUSED, doJaData); }
			else if(event == PlayerListener.END_OF_MEDIA) { doJaListener.mediaAction(doJaPresenter, com.nttdocomo.ui.AudioPresenter.AUDIO_COMPLETE, doJaData); }
			else if(event == PlayerListener.LOOPED) { doJaListener.mediaAction(doJaPresenter, com.nttdocomo.ui.AudioPresenter.AUDIO_LOOPED, doJaData); }
		}

		if(phraseListener != null)
		{
			// Phrase listeners don't have events types for STOP and START, but they do have quite a few USER_* events though...
			//if(event == PlayerListener.CLOSED) { phraseListener.mediaAction(com.nttdocomo.ui.AudioPresenter.AUDIO_STOPPED); }
			//else if(event == PlayerListener.STARTED) { phraseListener.mediaAction(com.nttdocomo.ui.AudioPresenter.EV); }
			if(event == PlayerListener.STOPPED)  { phraseListener.eventOccurred(com.jblend.media.smaf.phrase.PhraseEventType.EV_PAUSE); }
			else if(event == PlayerListener.END_OF_MEDIA) { phraseListener.eventOccurred(com.jblend.media.smaf.phrase.PhraseEventType.EV_END); }
			else if(event == PlayerListener.LOOPED) { phraseListener.eventOccurred(com.jblend.media.smaf.phrase.PhraseEventType.EV_LOOP); }
		}
	}

	public void deallocate()
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot deallocate player, it is already CLOSED."); }

		if(getState() >= Player.REALIZED && isRunning()) { stop(); }
		player.deallocate();

		/*
		 * Only set state to REALIZED if we have effectively moved into REALIZED or higher (PREFETCHED, etc),
		 * as deallocate can be called during the transition from UNREALIZED to REALIZED, and if that happens,
		 * we can't actually set it as REALIZED, it must be kept as UNREALIZED.
		 */
		if(state > Player.UNREALIZED)
		{
			player.realize();
			state = Player.REALIZED;
		}
	}

	public String getContentType()
	{
		if(getState() == Player.UNREALIZED || getState() == Player.CLOSED) { throw new IllegalStateException("Cannot get content type. Player is either CLOSED or UNREALIZED."); }

		return contentType;
	}

	public long getDuration()
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot call getDuration() on a CLOSED player."); }

		if(getState() == Player.REALIZED || getState() == Player.UNREALIZED) { return Player.TIME_UNKNOWN; }

		/*
		* MLD conversion may unroll one extra loop iteration to stabilize the MIDI state.
		* Using the sequencer duration here would cause the Media Player to display that extra iteration.
		*/

		return player instanceof MLDPlayer ? ((MLDPlayer) player).displayDuration() : player.getDuration(); // Maybe not really needed? We should find a jar that actually uses this for something
	}

	public long getMediaTime()
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot call getMediaTime on a CLOSED player."); }

		/*
		 * If the player isn't at least prefetched, there's no way to get media time.
		 * PlatformPlayer does in fact acquire everything needed to play the media on realize(),
		 * however, J2ME docs state that the exclusive and scarce resources (such as an actual
		 * player resources) should only be acquired in prefetch. So let's assume we're working this
		 * way here for now.
		 */
		if(getState() == Player.UNREALIZED || getState() == Player.REALIZED) { return Player.TIME_UNKNOWN; }

		// Keep MLD progress on the same source timeline as the displayed duration above.
		return player instanceof MLDPlayer ? ((MLDPlayer) player).displayTime() : player.getMediaTime();
	}

	/* Both midi and wav players do little more than just set their state as PREFETCHED here. */
	public void prefetch()
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot prefetch player, as it is in the CLOSED state."); }

		if(getState() == Player.UNREALIZED) { realize(); }

		if(getState() == Player.REALIZED) { player.prefetch(); }
	}

	public void realize()
	{
		if(getState() == Player.CLOSED) { throw new IllegalStateException("Cannot realize player, as it is in the CLOSED state"); }

		if(getState() == Player.UNREALIZED) { player.realize(); }
	}

	public void setLoopCount(int count)
	{
		/* A MIDlet setting 0 explicitly here is illegal */
		if(count == 0) {throw new IllegalStateException("Jar tried to set loop count as 0.");}
		if(getState() == Player.CLOSED || getState() == Player.STARTED) { throw new IllegalStateException("Jar tried to set loop count on a player in an invalid state."); }

		player.setLoopCount(count);
	}

	public long setMediaTime(long now)
	{
		if(getState() == Player.UNREALIZED || getState() == Player.CLOSED) { throw new IllegalStateException("Cannot set Media Time. Player is either UNREALIZED or CLOSED."); }

		return player.setMediaTime(now);
	}

	public boolean isRunning() { return getState() >= Player.PREFETCHED ? player.isRunning() : false; }

	// Controllable interface //

	public Control getControl(String controlType)
	{
		if(disableControls) { return controls[0]; }
		if(getState() == Player.CLOSED || getState() == Player.UNREALIZED) { throw new IllegalStateException("Cannot call getControl(), as the player is either CLOSED or UNREALIZED."); }

		if(controlType.contains("VolumeControl")) { return controls[0]; }
		if(controlType.contains("TempoControl"))  { return controls[1]; }
		if(controlType.contains("MIDIControl"))   { return controls[2]; }
		if(controlType.contains("ToneControl"))   { return controls[3]; }

		return null;
	}

	public Control[] getControls()
	{
		if(disableControls) { return controls; }
		if(getState() == Player.CLOSED || getState() == Player.UNREALIZED) { throw new IllegalStateException("Cannot call getControls(), as the player is either CLOSED or UNREALIZED."); }

		return controls;
	}

	public static void addPlayerToStack(MIDIPlayer midplayer, WAVPlayer wavplayer, MP3Player mpegPlayer, SMAFPlayer smafPlayer)
	{
		if(midplayer != null)
		{
			for(int i = 0; i < sequencePlayers.length; i++)
			{
				if(sequencePlayers[i] == null || sequencePlayers[i].getSequence() == null)
				{
					sequencePlayers[i] = midplayer;
					return;
				}
			}
			// All players are occupied, find the first stopped one to be replaced
			for(int i = 0; i < sequencePlayers.length; i++)
			{
				if(sequencePlayers[i] != null && !sequencePlayers[i].isRunning())
				{
					sequencePlayers[i].deallocate();
					sequencePlayers[i].close();
					sequencePlayers[i] = midplayer;
					Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Overriding a previously allocated midi player. Either the jar requires more than " + sequencePlayers.length + " midi at the same time, or it's not closing media properly.");
					return;
				}
			}
		}
		else if(smafPlayer != null)
		{
			for(int i = 0; i < sequencePlayers.length; i++)
			{
				if(sequencePlayers[i] == null || sequencePlayers[i].getSequence() == null)
				{
					sequencePlayers[i] = smafPlayer;
					return;
				}
			}
			// All players are occupied, find the first stopped one to be replaced
			for(int i = 0; i < sequencePlayers.length; i++)
			{
				if(sequencePlayers[i] != null && !sequencePlayers[i].isRunning())
				{
					sequencePlayers[i].deallocate();
					sequencePlayers[i].close();
					sequencePlayers[i] = smafPlayer;
					Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Overriding a previously allocated midi player. Either the jar requires more than " + sequencePlayers.length + " midi at the same time, or it's not closing media properly.");
					return;
				}
			}
		}
		else if(wavplayer != null)
		{
			return;
		}
	}

	public TimeBase getTimeBase()
	{
		Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "getTimeBase() not implemented.");
		return null;
	}

	public void setTimeBase(TimeBase master)
	{
		Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "setTimeBase(TimeBase) not implemented.");
	}

	// Controls //

	/* midiControl is untested */
	public class midiControl implements javax.microedition.media.control.MIDIControl
	{
		private MIDIPlayer player;

		/*
		 * Java, by default, does not directly support any of the query methods
		 * below, which means we must implement them, and track the state of
		 * everything that they change ourselves.
		 */
		private int[] channelVolume = new int[16]; // For getChannelVolume

		public midiControl(MIDIPlayer player)
		{
			this.player = player;
			for(int channel = 0; channel < channelVolume.length; channel++) { channelVolume[channel] = 127; }
		}

		public int[] getBankList(boolean custom)
		{
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: getBankList() untested");

			if(custom) { Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: getBankList() with custom bank not implemented, returning all banks."); }

			// Use a list to collect bank numbers
			ArrayList<Integer> bankList = new ArrayList<Integer>();

			try
			{
				Patch[] patches = player.getSequence().getPatchList();

				// Use the current sequence as a source for the patches and its available banks
				for (int i = 0; i < patches.length; i++) { bankList.add(patches[i].getBank()); }
			} catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Error retrieving bank list: " + e.getMessage()); }

			// Return the array of banks
			int[] bankArray = new int[bankList.size()];
			for (int i = 0; i < bankList.size(); i++) { bankArray[i] = bankList.get(i); }

			return bankArray;
		}

		public int getChannelVolume(int channel)
		{
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: getChannelVolume() untested");
			return channelVolume[channel];
		}

		public java.lang.String getKeyName(int bank, int prog, int key)
		{
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: getKeyName() not implemented ");
			// And probably will never be, i don't think Java has any concept of this, all the way to Key-Mapped Banks
			return "";
		}

		public int[] getProgram(int channel)
		{
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: getProgram() untested");

			final int[] program = new int[2];

			// This is VERY costly, and might not even be correct as it relies on getProgramList and getBankList, which themselves are untested.
			try
			{
				MidiChannel midiChannels[] = player.synthesizer.getChannels();
				if(channel < 0 || channel > midiChannels.length) {throw new IllegalArgumentException("midiControl: Tried to call getProgram with invalid channel");}

				int currentProgram = midiChannels[channel].getProgram(); // This returns a {bank, program} pair, so only channel.getProgram() is not enough.

				// We got the program mapped to that channel, now to find the corresponding bank for this program
				int[] banks = getBankList(false); // Retrieve the list of available banks

				for (int bank : banks) // Iterate through the banks to find the matching program, if there's any at all
				{
					int[] programList = getProgramList(bank); // Get list of programs for the current bank

					// Check if the current program exists in this bank
					for (int programNum : programList)
					{
						if (programNum == currentProgram) // IF it does, we found the {bank,program} pair to be returned
						{
							program[0] = bank;
							program[1] = currentProgram;
						}
					}
				}
			}
			catch (Exception e)
			{
				Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Error retrieving program for channel: " + e.getMessage());
				return null;
			}

			return program;
		}

		public int[] getProgramList(int bank) {

			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: getProgramList()");

			ArrayList<Integer> programList = new ArrayList<Integer>();

			try
			{
				Patch[] patches = player.getSequence().getPatchList();

				// Iterate through the available patches and collect program numbers for the specified bank
				for (Patch patch : patches)
				{
					if (patch.getBank() == bank) { programList.add(patch.getProgram()); } // Add the program number for the matching bank
				}
			} catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Error retrieving program list: " + e.getMessage()); }

			// Return the array of programs
			int[] programArray = new int[programList.size()];
			for (int i = 0; i < programList.size(); i++) { programArray[i] = programList.get(i); }

			return programArray;
		}

		public String getProgramName(int bank, int prog)
		{
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: getProgramName()");

			// Java doesn't even have a concept of having names for programs, only instruments. So let's return the instrument's name instead.
			try
			{
				Soundbank soundbank = player.synthesizer.getDefaultSoundbank();

				Instrument[] instruments = soundbank.getInstruments();
				for (Instrument instrument : instruments)
				{
					Patch patch = instrument.getPatch();
					// If a matching bank and program number is found on the instrument's patch, return the name of the patch's instrument
					if (patch.getBank() == bank && patch.getProgram() == prog) { return instrument.getName(); }
				}
			} catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Error retrieving program name: " + e.getMessage()); }

			return ""; // Return an empty string if no match is found
		}

		public boolean isBankQuerySupported()
		{
			Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "isBankQuerySupported() requested, returning unsupported.");
			return false;
		}

		public int longMidiEvent(byte[] data, int offset, int length) {
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: longMidiEvent() untested");

			// Validate input parameters
			if (data == null || offset < 0 || length < 0 || offset + length > data.length) { throw new IllegalArgumentException("MidiControl: Invalid arguments for longMidiEvent()"); }

			try
			{
				if (data[offset] == (byte) 0xF0 && data[offset + length - 1] == (byte) 0xF7) // Check if it is a SysEx message
				{
					// Create the SysEx message without the status byte
					byte[] sysExData = new byte[length - 2];
					System.arraycopy(data, offset + 1, sysExData, 0, length - 2); // Exclude the 0xF0 and 0xF7

					// Create the SysexMessage
					SysexMessage sysexMessage = new SysexMessage(0xF0, sysExData, sysExData.length);
					player.receiver.send(sysexMessage, player.getMediaTime() + 50000L); // Send the message
				}
				else // If it is not, send data as a series of short messages (probably implemented incorrectly, and being untested only makes things worse)
				{
					for (int i = offset; i < offset + length; i += 3)
					{
						int msgLength = Math.min(3, length - (i - offset)); // Ensure we don't exceed the shortMessage's length
						ShortMessage shortMessage = new ShortMessage();

						if      (msgLength == 1) { shortMessage.setMessage(data[i] & 0xFF); }  // Send only status byte (is this even useful?)
						else if (msgLength == 2) { shortMessage.setMessage(data[i] & 0xFF, data[i + 1] & 0xFF, 0); } // Status byte + one data byte
						else if (msgLength == 3) { shortMessage.setMessage(data[i] & 0xFF, data[i + 1] & 0xFF, data[i + 2] & 0xFF); } // Full short message

						player.receiver.send(shortMessage, player.getMediaTime() + 50000L);
					}
				}
				return length; // Return the number of bytes sent
			}
			catch (Exception e)
			{
				Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Error sending long MIDI event: " + e.getMessage());
				return -1; // Return -1 if an error occurred
			}
		}

		public void setChannelVolume(int channel, int volume)
		{
			Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: setChannelVolume() untested");

			try
			{
				MidiChannel midiChannels[] = player.synthesizer.getChannels();
				if(channel < 0 || channel > midiChannels.length || volume < 0 || volume > 127) {throw new IllegalArgumentException("midiControl: Tried to call setChannelVolume with invalid args");}

				// Set the volume on the MIDI channel
				channelVolume[channel] = volume; // For tracking purposes, whenever getChannelVolume is called.
				midiChannels[channel].controlChange(7, volume);
			}
			catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Midi setChannelVolume failed: " + e.getMessage());}
		}

		public void setProgram(int channel, int bank, int program)
		{
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: setProgram() untested");

			// Validate input parameters
			if (channel < 0 || channel > 15) { throw new IllegalArgumentException("Channel must be between 0 and 15."); }
			if (program < 0 || program > 127) { throw new IllegalArgumentException("Program must be between 0 and 127."); }
			if (bank < -1 || bank > 16383) { throw new IllegalArgumentException("Bank must be between 0 and 16383, or -1 for default bank."); }

			// Send bank change
			shortMidiEvent(CONTROL_CHANGE | channel, CONTROL_BANK_CHANGE_MSB, bank >> 7); // Send MSB (Most Significant Byte)
			shortMidiEvent(CONTROL_CHANGE | channel, CONTROL_BANK_CHANGE_LSB, bank & 0x7F); // Send LSB (Least Significant Byte)

			// Send program change
			shortMidiEvent(PROGRAM_CHANGE | channel, program, 0);
		}

		public void shortMidiEvent(int type, int data1, int data2)
		{
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "midiControl: shortMidiEvent() untested");

			if(type < 0x80 || type == 0xF0 || type == 0xF7 || data1 < 0 || data1 > 127 || data2 < 0 || data2 > 127) { throw new IllegalArgumentException("midiControl: Invalid arguments for shortMidiEvent()"); }

			try
			{
				// Create a MIDI message from the type and data values received
				final byte[] message = new byte[3];
				message[0] = (byte) type;
				message[1] = (byte) data1;
				message[2] = (byte) data2;

				ShortMessage midiMessage = new ShortMessage();
				midiMessage.setMessage(type, data1, data2);

				// Send the MIDI message to the receiver
				player.receiver.send(midiMessage, player.getMediaTime() + 50000L); // Send message after 50ms
			}
			catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Failed to send short MIDI event: " + e.getMessage()); }
		}
	}

	public class volumeControl implements com.siemens.mp.media.control.VolumeControl // Siemens already extends javax here
	{
		private boolean muted = false;
		private BasicPlayer player; // Reference to the player this is linked to, or else we won't be able to apply changes
		private byte volume = 100;
		private int panValue = 64; // Center panning

		// MIDI Volume Sysex message
		private byte[] volumeSysEx = new byte[]
		{
			(byte) 0xF0, // SysEx indicator
			(byte) 0x7F, (byte) 0x7F,
			(byte) 0x04, (byte) 0x01, // GM-Compatible device and manufacturer ID
			0, // Lower 7 volume bits, ignored for java's volume which goes from 0 to 127
			volume, // Volume value
			(byte) 0xF7  // End of Sysex
		};
		SysexMessage sysexMessage = new SysexMessage();

		public volumeControl(BasicPlayer player) { this.player = player; }

		public int getLevel()
		{
			if(getState() == Player.REALIZED) { return -1; }

			return volume;
		}

		public int setLevel(int level)
		{
			/* Some Digital Chocolate games actually go all the way to level = 120. E.g. Tornado Mania */
			if(level > 100) { level = 100; }
			else if(level < 0) { level = 0; }

			if(level == getLevel() || Mobile.compatIgnoreVolumeChanges || player == null) { return getLevel(); }

			try
			{
				if (player instanceof MIDIPlayer)
				{
					if(((MIDIPlayer)player).synthesizer == null) { return getLevel(); } // Only make changes if the midi subsystem for this player is available

					volumeSysEx[6] = isMuted() ? 0 : (byte) (level * 127 / 100); // Convert to MIDI volume range
					sysexMessage.setMessage(volumeSysEx, volumeSysEx.length);
					((MIDIPlayer)player).receiver.send(sysexMessage, -1); // Send the volume change message
				}
				else if(player instanceof WAVPlayer)
				{
					if(((WAVPlayer)player).wavClip == null) { return getLevel(); } // Only make changes if the wav clip for this player is available
					WAVPlayer wav = (WAVPlayer) player;

					/* We have to map 0 <= value <= 100 to a clip's range of -30dB to 0dB  */
					float dB = isMuted() ? -80.0f : -30.0f + ((level / 100.0f) * (30.0f));

					FloatControl volumeControl = (FloatControl) wav.wavClip.getControl(FloatControl.Type.MASTER_GAIN);
					volumeControl.setValue(dB);
				}
				else if(player instanceof SMAFPlayer) // SMAF is a mix of midi and WAVPlayer, so it pretty much borrows from both here
				{
					if(((SMAFPlayer)player).synthesizer != null) // MIDI portion of SMAF
					{
						volumeSysEx[6] = isMuted() ? 0 : (byte) (level * 127 / 100);
						sysexMessage.setMessage(volumeSysEx, volumeSysEx.length);
						((SMAFPlayer)player).receiver.send(sysexMessage, -1); // Send the volume change message
					}

					if(((SMAFPlayer) player).wavClips != null) // WAV portion of SMAF
					{
						FloatControl volumeControl;
						float dB = isMuted() ? -80.0f : -40.0f + ((level / 100.0f) * (40.0f));

						for(int i = 0; i < ((SMAFPlayer) player).wavClips.length; i++)
						{
							if(((SMAFPlayer) player).wavClips[i] == null) { continue; }
							volumeControl = (FloatControl) ((SMAFPlayer) player).wavClips[i].getControl(FloatControl.Type.MASTER_GAIN);
							volumeControl.setValue(dB);
						}
					}
				}
				else if(player instanceof MP3Player && ((MP3Player)player).mp3Player != null)
					{ ((MP3Player)player).mp3Player.setLevel(level); }
			}
			catch(Exception e)
			{
				Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "failed to set volume: " + e.getMessage());
				e.printStackTrace();
			}

			volume = (byte) level;
			notifyListeners(PlayerListener.VOLUME_CHANGED, this); // Notify that the volume state changed

			return getLevel();
		}

		public void setMute(boolean mute)
		{
			if(mute != isMuted())
			{
				muted = mute;

				setLevel(volume);
				notifyListeners(PlayerListener.VOLUME_CHANGED, this);
			}
		}

		public boolean isMuted() { return muted; }

		// These two are only really used for SMAF
		public void setPanpot(int panning)
		{
			if (player instanceof SMAFPlayer)
			{
				SMAFPlayer sequencer = (SMAFPlayer) player;

				MidiChannel midiChannels[] = sequencer.synthesizer.getChannels();

				if(sequencer.isRunning())
				{
					for (int channel = 0; channel < midiChannels.length; channel++)
					{
						midiChannels[channel].controlChange(10, panning);
					}
				}
				panValue = panning;
			}
		}

		public int getPanpot() { return player instanceof SMAFPlayer ? panValue : 64; }
	}

	/* This one hasn't been tested yet, no jar was found at the time it was implemented */
	public class tempoControl implements javax.microedition.media.control.TempoControl
	{
		/* MAX_RATE and MIN_RATE follow the JSR-135 docs guaranteed values, no matter if our player has a wider range */
		private final int MAX_RATE = 300000;
		private final int MIN_RATE = 10000;

		private int tempo = 120000; // Default tempo of 120 BPM in millitempo
		private int rate = 100000; // Default Rate in RateControl

		private BasicPlayer player;

		public tempoControl(BasicPlayer player) { this.player = player; }

		/*
		 * According to the docs, getTempo():
		 *
		 * Gets the current playback tempo. This represents the current state of the sequencer:
		 *	1 - A sequencer may not be initialized before the Player is prefetched. An uninitialized sequencer in this case returns a default tempo of 120 beats per minute.
		 *	2 - After prefetching has finished, the tempo is set to the start tempo of the MIDI sequence (if any).
		 *	3 - During playback, the return value is the current tempo and varies with tempo events in the MIDI file
		 *	4 - A stopped sequence retains the last tempo it had before it was stopped.
		 *	5 - A call to setTempo() changes current tempo until a tempo event in the MIDI file is encountered.
		 *
		 * Of course, only the very basic, case 3 is considered. If needed, and a jar is found to need it,
		 * we can implement the other cases for better player state handling.
		 */

		public int getTempo() { Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "getTempo()"); return tempo; }

		public int setTempo(int millitempo)
		{
			Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "setTempo()");
			tempo = millitempo;

			/*
			 * Here the docs say that the midi should START at this tempo, which
			 * indicates that we probably should add a midiEvent message to itsf
			 * tracks in order to change their tempo at tick 0, but first we need
			 * to find a jar that uses this, otherwise it's a shot in the dark.
			 */
			if(player instanceof MIDIPlayer) { ((MIDIPlayer)player).midi.setTempoInBPM(getEffectiveBPM()); }
			else if(player instanceof SMAFPlayer) { ((SMAFPlayer)player).midi.setTempoInBPM(getEffectiveBPM()); }

			return tempo;
		}

		// RateControl interface
		public int getMaxRate() { Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "getMaxRate()"); return MAX_RATE; }

		public int getMinRate() { Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "getMinRate()"); return MIN_RATE; }

		public int getRate() { Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "getRate()"); return rate; }

		public int setRate(int millirate)
		{
			if(player == null) { return millirate; }
			rate = millirate;

			/*
			 * In order to use setTempoFactor to adjust MIDIPlayer's rate,
			 * we need to convert the rate value we currently have (expressed as
			 * integer) to the float range that setTempoFactor accepts.
			 *
			 * In short, 100000 here means 100% playback rate, while 1.0f means
			 * the same in setTempoFactor, hence the division below.
			 */
			float factor = rate / 100000.0f;

			if(player instanceof MIDIPlayer) { ((MIDIPlayer)player).midi.setTempoFactor(factor); }
			else if(player instanceof SMAFPlayer) { ((SMAFPlayer)player).midi.setTempoFactor(factor); }

			return rate;
		}

		/* Used for setTempo, otherwise we won't know which tempo to actually set */
		public float getEffectiveBPM() { return (float) ( (getTempo() * getRate() / 1000.0f) / 100000.0f); }
	}

	/* ToneControl is also almost entirely untested right now, couldn't find a jar that uses setSequence() */
	public class toneControl implements com.siemens.mp.media.control.ToneControl // Siemens already extends javax here too
	{
		private MIDIPlayer player;

		public toneControl(MIDIPlayer player) { Mobile.log(Mobile.LOG_DEBUG, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Tone Control"); this.player = player; }

		/*
		 * As far as i can tell, Nokia's OTT/OTA Tones don't use this, which would leave only jars that directly use J2ME's Augmented BNF format, if there are any.
		 * If such a case is found, setupSequence() should be the one to parse that format into a MIDI sequence.
		 */
		public void setSequence(byte[] sequence)
		{
			if(sequence == null) { throw new IllegalArgumentException("ToneControl: cannot set a null sequence"); }

			if(getState() == Player.PREFETCHED || getState() == Player.STARTED) { throw new IllegalStateException("Cannot call setSequence(), as the player is either PREFETCHED or STARTED."); }

			// If what we received is a tone sequence from nokia, siemens, sprint, etc. which is converted to midi beforehand, just send it to the player right away.
			if(sequence[0] == 'M' && sequence[1] == 'T' && sequence[2] == 'h' && sequence[3] == 'd')
			{
				player.setSequence(new ByteArrayInputStream(sequence));
			}
			else // TODO: For J2ME's ToneControl Augmented BNF tone format, if anything out there even uses it.
			{
				/* This should show up in the case a jar tries to use it... just so we can find a jar that can test this */
				Mobile.log(Mobile.LOG_WARNING, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "setSequence() for A-BNF Tones not implemented");
				try
				{
					Sequence toneSequence = new Sequence(Sequence.PPQ, 24);
					Track track = toneSequence.createTrack();

					setupSequence(sequence, track);

					//player.setSequence(toneSequence);
					//player.midi.setSequence(toneSequence);
				} catch (InvalidMidiDataException e) {Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Can't parse tone sequence: " + e.getMessage());}
			}
		}

		private void setupSequence(byte[] sequence, Track track) // This tries to parse the default
		{
			if (sequence.length == 0 || sequence[0] != 1) { throw new IllegalArgumentException("ToneControl: Invalid sequence"); }

			/* We start a MIDI sequence setup like this: */
			int index = 1; // the very first index is just the VERSION number, so skip it
			int tempo = 120; // Default MIDI tempo in BPM
			int currentTick = 0; // This is used to keep track of the current tick, used by SetVolume and Tempo events
			int noteVolume = 127; // Start sending notes at the max volume by default

			while (index < sequence.length)
			{
				byte eventType = sequence[index++];
				if (eventType >= -1 && eventType <= 127) // We found a note (or SILENCE, -1)
				{
					byte note = sequence[index++];
					byte duration = sequence[index++];
					try { addNote(track, note, duration, noteVolume, currentTick); currentTick += duration; }
					catch (InvalidMidiDataException e) {Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Invalid note: " + e.getMessage());}

				}
				else if (eventType == ToneControl.REPEAT)
				{
					byte numRepeats = sequence[index++];
					byte noteToRepeat = sequence[index++];
					byte repeatNoteDuration = sequence[index++];
					for (int i = 0; i < numRepeats; i++)
					{
						try {addNote(track, noteToRepeat, repeatNoteDuration, noteVolume, currentTick); }
						catch (InvalidMidiDataException e) {Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Invalid repeated note: " + e.getMessage());}
						currentTick += repeatNoteDuration;
					}
				}
				else if (eventType == ToneControl.SET_VOLUME)
				{
					noteVolume = sequence[index++];
					try
					{
						ShortMessage volumeMessage = new ShortMessage();
						volumeMessage.setMessage(ShortMessage.CONTROL_CHANGE, 0, 7, noteVolume);
						track.add(new MidiEvent(volumeMessage, currentTick));
					}
					catch (InvalidMidiDataException e) {Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Invalid SET_VOLUME event: " + e.getMessage());}
				}
				else if(eventType == ToneControl.TEMPO)
				{
					tempo = sequence[index++];
					int microsecondsPerBeat = 60000000 / (tempo * 4);
					try
					{
						track.add(new MidiEvent(new MetaMessage(0x51, new byte[]
						{
							(byte)(microsecondsPerBeat >> 16),
							(byte)(microsecondsPerBeat >> 8),
							(byte)(microsecondsPerBeat)
						}, 3), currentTick));
					}
					catch (InvalidMidiDataException e) {Mobile.log(Mobile.LOG_ERROR, PlatformPlayer.class.getPackage().getName() + "." + PlatformPlayer.class.getSimpleName() + ": " + "Invalid TEMPO event: " + e.getMessage());}
				}
				else if(eventType == ToneControl.RESOLUTION ||
						eventType == ToneControl.BLOCK_START ||
						eventType == ToneControl.BLOCK_END) { /* These events don't need to be added to the midi sequence */ }
				else { throw new IllegalArgumentException("Unknown event type."); }
			}
		}

		private void addNote(Track track, byte note, byte duration, int volume, int tick) throws InvalidMidiDataException
		{
			int midiNote = note + 60; // Convert to MIDI note (C4 = MIDI 60)
			int noteDuration = duration; // Use duration directly as tick increment

			// Note on event with velocity set to currentVolume
			ShortMessage noteOn = new ShortMessage(ShortMessage.NOTE_ON, 0, midiNote, volume);
			track.add(new MidiEvent(noteOn, tick)); // Start immediately

			// Note off event
			ShortMessage noteOff = new ShortMessage(ShortMessage.NOTE_OFF, 0, midiNote, tick + noteDuration);
			track.add(new MidiEvent(noteOff, tick + noteDuration)); // End after the duration
		}
	}
}
