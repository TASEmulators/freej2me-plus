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

import java.util.Map;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

/* MLD decoding support */
import javax.microedition.media.decoders.MLDDecoder;

import org.recompile.mobile.Mobile;

/*
 * MLD conversion may unroll a loop to stabilize its MIDI state. PlaybackTimeline maps
 * the longer sequencer timeline back to the original MLD progress.
 */

public class MLDPlayer extends SMAFPlayer
{
	private final MLDDecoder.PlaybackTimeline timeline = MLDDecoder.getPlaybackTimeline();

	public MLDPlayer(InputStream midiStream, InputStream[] wavStreams, Map<Integer, Integer> pcmPositions, Map<Integer, Integer> pcmVelocities)
	{
		super(midiStream, wavStreams, pcmPositions, pcmVelocities);
	}

	protected void onMeta(MetaMessage meta)
	{
		String marker = MLDDecoder.MLDSequenceMarker.decodeMarker(meta);
		if(!MLDDecoder.MLDSequenceMarker.isStopMarker(marker)) { return; }

		stopPcmClips();
	}

	protected void configurePlayback() throws InvalidMidiDataException
	{
		MLDDecoder.MLDSequenceMarker.LoopMarker loopInfo = findLoopMarker(getSequence());
		if(loopInfo == null) { return; }

		setLoop(loopInfo.loopStartTick, loopInfo.loopEndTick, loopInfo.repeatCount);
	}

	public long displayDuration()
	{
		return timeline.displayDuration(getDuration());
	}

	public long displayTime()
	{
		return timeline.displayTime(getSequenceTick(), getSequence().getResolution(), super.getMediaTime());
	}

	MLDDecoder.MLDSequenceMarker.LoopMarker findLoopMarker(Sequence sequence)
	{
		if(sequence == null) { return null; }

		Track[] tracks = sequence.getTracks();
		for(int t = 0; t < tracks.length; t++)
		{
			Track track = tracks[t];
			for(int i = 0; i < track.size(); i++)
			{
				MidiEvent event = track.get(i);
				if(!(event.getMessage() instanceof MetaMessage)) { continue; }

				String marker = MLDDecoder.MLDSequenceMarker.decodeMarker((MetaMessage) event.getMessage());
				if(!MLDDecoder.MLDSequenceMarker.isLoopMarker(marker)) { continue; }

				MLDDecoder.MLDSequenceMarker.LoopMarker loopMarker = MLDDecoder.MLDSequenceMarker.parseLoopMarker(marker);
				if(loopMarker == null)
				{
					Mobile.log(Mobile.LOG_WARNING, MLDPlayer.class.getPackage().getName() + "." + MLDPlayer.class.getSimpleName() + ": " + "Invalid embedded MLD loop marker: " + marker);
					continue;
				}
				return loopMarker;
			}
		}

		return null;
	}
}
