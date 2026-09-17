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

import java.util.HashMap;
import java.util.Map;

import org.recompile.mobile.Mobile;

public class PhrasePlayerBase
{
	protected static PhrasePlayer phrasePlayer;

	protected int trackCount = 0;
	protected int audioTrackCount = 0;
	protected int jPhoneTrackCount = 0;
	protected int vodafoneTrackCount = 0;

	protected Map<Integer, PhraseTrack> usedTracks = new HashMap<Integer, PhraseTrack>();
	protected Map<Integer, AudioPhraseTrack> usedAudioTracks = new HashMap<Integer, AudioPhraseTrack>();
	protected Map<Integer, com.j_phone.amuse.PhraseTrack> usedJPhoneTracks = new HashMap<Integer, com.j_phone.amuse.PhraseTrack>();
	protected Map<Integer, com.vodafone.v10.sound.SoundTrack> usedVodafoneTracks = new HashMap<Integer, com.vodafone.v10.sound.SoundTrack>();

	public static PhrasePlayer setup()
	{
		phrasePlayer = new PhrasePlayer();
		return phrasePlayer;
	}

	public void disposePlayer()
	{
		Mobile.log(Mobile.LOG_DEBUG, PhrasePlayer.class.getPackage().getName() + "." + PhrasePlayer.class.getSimpleName() + ": " + "disposePlayer");
		phrasePlayer.dispose();
		phrasePlayer = null;
	}

	public PhraseTrack getPhraseTrack()
	{
		if(phrasePlayer == null || trackCount > getTrackCount()) { throw new IllegalStateException("Cannot get a new track"); }
		Mobile.log(Mobile.LOG_DEBUG, PhrasePlayer.class.getPackage().getName() + "." + PhrasePlayer.class.getSimpleName() + ": " + "getTrack");

		// Try reusing a track that has been disposed first
		for(int i = 0; i < usedTracks.size(); i++)
		{
			if(usedTracks.get(i) == null)
			{
				usedTracks.put(i, new PhraseTrack(i));
				return usedTracks.get(i);
			}
		}

		usedTracks.put(trackCount, new PhraseTrack(trackCount));
		trackCount += 1;

		return usedTracks.get(trackCount-1);
	}

	public AudioPhraseTrack getAudioTrack()
	{
		if(phrasePlayer == null || audioTrackCount > getAudioTrackCount()) { throw new IllegalStateException("Cannot get a new track"); }
		Mobile.log(Mobile.LOG_DEBUG, PhrasePlayer.class.getPackage().getName() + "." + PhrasePlayer.class.getSimpleName() + ": " + "getAudioTrack");

		// Try reusing a track that has been disposed first
		for(int i = 0; i < usedAudioTracks.size(); i++)
		{
			if(usedAudioTracks.get(i) == null)
			{
				usedAudioTracks.put(i, new AudioPhraseTrack(i));
				return usedAudioTracks.get(i);
			}
		}

		usedAudioTracks.put(audioTrackCount, new AudioPhraseTrack(audioTrackCount));
		audioTrackCount += 1;

		return usedAudioTracks.get(audioTrackCount-1);
	}

	public com.j_phone.amuse.PhraseTrack getJPhoneTrack()
	{
		if(phrasePlayer == null || jPhoneTrackCount > getTrackCount()) { throw new IllegalStateException("Cannot get a new track"); }
		Mobile.log(Mobile.LOG_DEBUG, PhrasePlayer.class.getPackage().getName() + "." + PhrasePlayer.class.getSimpleName() + ": " + "getTrack");

		// Try reusing a track that has been disposed first
		for(int i = 0; i < usedJPhoneTracks.size(); i++)
		{
			if(usedJPhoneTracks.get(i) == null)
			{
				usedJPhoneTracks.put(i, new com.j_phone.amuse.PhraseTrack(i));
				return usedJPhoneTracks.get(i);
			}
		}

		usedJPhoneTracks.put(jPhoneTrackCount, new com.j_phone.amuse.PhraseTrack(jPhoneTrackCount));
		jPhoneTrackCount += 1;

		return usedJPhoneTracks.get(jPhoneTrackCount-1);
	}

	public com.vodafone.v10.sound.SoundTrack getVodafoneTrack()
	{
		if(phrasePlayer == null || vodafoneTrackCount > getTrackCount()) { throw new IllegalStateException("Cannot get a new track"); }
		Mobile.log(Mobile.LOG_DEBUG, PhrasePlayer.class.getPackage().getName() + "." + PhrasePlayer.class.getSimpleName() + ": " + "getTrack");

		// Try reusing a track that has been disposed first
		for(int i = 0; i < usedVodafoneTracks.size(); i++)
		{
			if(usedVodafoneTracks.get(i) == null)
			{
				usedVodafoneTracks.put(i, new com.vodafone.v10.sound.SoundTrack(i));
				return usedVodafoneTracks.get(i);
			}
		}

		usedVodafoneTracks.put(vodafoneTrackCount, new com.vodafone.v10.sound.SoundTrack(vodafoneTrackCount));
		vodafoneTrackCount += 1;

		return usedVodafoneTracks.get(vodafoneTrackCount-1);
	}

	public int getTrackCount() // This is for Sequenced/MIDI data
	{
		if(phrasePlayer == null) { throw new IllegalStateException("PhrasePlayer has been destroyed"); }
		return 16;
	}

	public int getAudioTrackCount() // This is often used for PCM/Sampled data
	{
		if(phrasePlayer == null) { throw new IllegalStateException("PhrasePlayer has been destroyed"); }
		return 32;
	}

	public PhraseTrack getPhraseTrack(int track)
	{
		if(phrasePlayer == null || track > getTrackCount()) { throw new IllegalStateException("Cannot get track"); }
		if(usedTracks.get(track) != null) { throw new IllegalStateException("Requested AudioTrack is already in use"); }

		Mobile.log(Mobile.LOG_DEBUG, PhrasePlayer.class.getPackage().getName() + "." + PhrasePlayer.class.getSimpleName() + ": " + "getAudioTrack(I)");

		trackCount++;
		usedTracks.put(track, new PhraseTrack(track));
		return usedTracks.get(track);
	}

	public AudioPhraseTrack getAudioTrack(int track)
	{
		if(phrasePlayer == null || track > getAudioTrackCount()) { throw new IllegalStateException("Cannot get track"); }
		if(usedAudioTracks.get(track) != null) { throw new IllegalStateException("Requested AudioTrack is already in use"); }

		Mobile.log(Mobile.LOG_DEBUG, PhrasePlayer.class.getPackage().getName() + "." + PhrasePlayer.class.getSimpleName() + ": " + "getTrack(I)");

		usedAudioTracks.put(track, new AudioPhraseTrack(track));
		return usedAudioTracks.get(track);
	}

	public com.j_phone.amuse.PhraseTrack getJPhoneTrack(int track)
	{
		if(phrasePlayer == null || track > getTrackCount()) { throw new IllegalStateException("Cannot get track"); }
		if(usedJPhoneTracks.get(track) != null) { throw new IllegalStateException("Requested AudioTrack is already in use"); }

		Mobile.log(Mobile.LOG_DEBUG, PhrasePlayer.class.getPackage().getName() + "." + PhrasePlayer.class.getSimpleName() + ": " + "getAudioTrack(I)");

		jPhoneTrackCount++;
		usedJPhoneTracks.put(track, new com.j_phone.amuse.PhraseTrack(track));
		return usedJPhoneTracks.get(track);
	}

	public com.vodafone.v10.sound.SoundTrack getVodafoneTrack(int track)
	{
		if(phrasePlayer == null || track > getTrackCount()) { throw new IllegalStateException("Cannot get track"); }
		if(usedVodafoneTracks.get(track) != null) { throw new IllegalStateException("Requested AudioTrack is already in use"); }

		Mobile.log(Mobile.LOG_DEBUG, PhrasePlayer.class.getPackage().getName() + "." + PhrasePlayer.class.getSimpleName() + ": " + "getAudioTrack(I)");

		vodafoneTrackCount++;
		usedVodafoneTracks.put(track, new com.vodafone.v10.sound.SoundTrack(track));
		return usedVodafoneTracks.get(track);
	}

	public void disposePhraseTrack(PhraseTrack t)
	{
		if(t == null) { throw new NullPointerException("Cannot dispose a null track"); }
		Mobile.log(Mobile.LOG_DEBUG, PhrasePlayer.class.getPackage().getName() + "." + PhrasePlayer.class.getSimpleName() + ": " + "disposeTrack");

		if(usedTracks.containsValue(t))
		{
			for (Map.Entry<Integer, PhraseTrack> entry : usedTracks.entrySet())
			{
				if (entry.getValue() != null && entry.getValue().equals(t))
				{
					usedTracks.put(entry.getKey(), null);
					break;
				}
			}
		}
	}

	public void disposeAudioTrack(AudioPhraseTrack t)
	{
		if(t == null) { throw new NullPointerException("Cannot dispose a null track"); }
		Mobile.log(Mobile.LOG_DEBUG, PhrasePlayer.class.getPackage().getName() + "." + PhrasePlayer.class.getSimpleName() + ": " + "disposeAudioTrack");

		if(usedAudioTracks.containsValue(t))
		{
			for (Map.Entry<Integer, AudioPhraseTrack> entry : usedAudioTracks.entrySet())
			{
				if (entry.getValue() != null && entry.getValue().equals(t))
				{
					usedAudioTracks.put(entry.getKey(), null);
					break;
				}
			}
		}
	}

	public void disposeJPhoneTrack(com.j_phone.amuse.PhraseTrack t)
	{
		if(t == null) { throw new NullPointerException("Cannot dispose a null track"); }
		Mobile.log(Mobile.LOG_DEBUG, PhrasePlayer.class.getPackage().getName() + "." + PhrasePlayer.class.getSimpleName() + ": " + "disposeTrack");

		if(usedJPhoneTracks.containsValue(t))
		{
			for (Map.Entry<Integer, com.j_phone.amuse.PhraseTrack> entry : usedJPhoneTracks.entrySet())
			{
				if (entry.getValue() != null && entry.getValue().equals(t))
				{
					usedJPhoneTracks.put(entry.getKey(), null);
					break;
				}
			}
		}
	}

	public void disposeVodafoneTrack(com.vodafone.v10.sound.SoundTrack t)
	{
		if(t == null) { throw new NullPointerException("Cannot dispose a null track"); }
		Mobile.log(Mobile.LOG_DEBUG, PhrasePlayer.class.getPackage().getName() + "." + PhrasePlayer.class.getSimpleName() + ": " + "disposeTrack");

		if(usedVodafoneTracks.containsValue(t))
		{
			for (Map.Entry<Integer, com.vodafone.v10.sound.SoundTrack> entry : usedVodafoneTracks.entrySet())
			{
				if (entry.getValue() != null && entry.getValue().equals(t))
				{
					usedVodafoneTracks.put(entry.getKey(), null);
					break;
				}
			}
		}
	}

	public void kill()
	{
		Mobile.log(Mobile.LOG_DEBUG, PhrasePlayer.class.getPackage().getName() + "." + PhrasePlayer.class.getSimpleName() + ": " + "kill");

		// Close/Unset all AudioPhraseTracks
		for(int i = 0; i < usedAudioTracks.size(); i++)
		{
			if(usedAudioTracks.get(i) != null) { usedAudioTracks.get(i).removeAudioPhrase(); }
		}

		// Close/Unset all PhraseTracks
		for(int i = 0; i < usedTracks.size(); i++)
		{
			if(usedTracks.get(i) != null) { usedTracks.get(i).removePhrase(); }
		}

		// Close/Unset all j_phone PhraseTracks
		for(int i = 0; i < usedJPhoneTracks.size(); i++)
		{
			if(usedJPhoneTracks.get(i) != null) { usedJPhoneTracks.get(i).removePhrase(); }
		}

		// Close/Unset all vodafone SoundTracks
		for(int i = 0; i < usedVodafoneTracks.size(); i++)
		{
			if(usedVodafoneTracks.get(i) != null) { usedVodafoneTracks.get(i).removePhrase(); }
		}
	}

	public void pause()
	{
		Mobile.log(Mobile.LOG_DEBUG, PhrasePlayer.class.getPackage().getName() + "." + PhrasePlayer.class.getSimpleName() + ": " + "pause");

		// Pause all AudioPhraseTracks
		for(int i = 0; i < usedAudioTracks.size(); i++)
		{
			if(usedAudioTracks.get(i) != null) { usedAudioTracks.get(i).pause(); }
		}

		// Pause all PhraseTracks
		for(int i = 0; i < usedTracks.size(); i++)
		{
			if(usedTracks.get(i) != null) { usedTracks.get(i).pause(); }
		}

		// Pause all j_phone PhraseTracks
		for(int i = 0; i < usedJPhoneTracks.size(); i++)
		{
			if(usedJPhoneTracks.get(i) != null) { usedJPhoneTracks.get(i).pause(); }
		}

		// Pause all vodafone SoundTracks
		for(int i = 0; i < usedVodafoneTracks.size(); i++)
		{
			if(usedVodafoneTracks.get(i) != null) { usedVodafoneTracks.get(i).pause(); }
		}
	}

	public void resume()
	{
		Mobile.log(Mobile.LOG_DEBUG, PhrasePlayer.class.getPackage().getName() + "." + PhrasePlayer.class.getSimpleName() + ": " + "resume");

		// Resume all AudioPhraseTracks
		for(int i = 0; i < usedAudioTracks.size(); i++)
		{
			if(usedAudioTracks.get(i) != null) { usedAudioTracks.get(i).resume(); }
		}

		// Resume all PhraseTracks
		for(int i = 0; i < usedTracks.size(); i++)
		{
			if(usedTracks.get(i) != null) { usedTracks.get(i).resume(); }
		}

		// Resume all j_phone PhraseTracks
		for(int i = 0; i < usedJPhoneTracks.size(); i++)
		{
			if(usedJPhoneTracks.get(i) != null) { usedJPhoneTracks.get(i).resume(); }
		}

		// Resume all vodafone SoundTracks
		for(int i = 0; i < usedVodafoneTracks.size(); i++)
		{
			if(usedVodafoneTracks.get(i) != null) { usedVodafoneTracks.get(i).resume(); }
		}
	}

	public void dispose()
	{
		kill();

		usedAudioTracks.clear();
		usedTracks.clear();
		usedJPhoneTracks.clear();
		usedVodafoneTracks.clear();
		trackCount = 0;
		audioTrackCount = 0;
	}

	// TODO: J_Phone's extensions
	public com.j_phone.amuse.PhraseTrack getTrackPair()
	{
		return null;
	}

	public com.j_phone.amuse.PhraseTrack getTrackPair(int paramInt)
	{
		return null;
	}
}
