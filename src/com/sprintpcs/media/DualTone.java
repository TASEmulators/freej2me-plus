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
package com.sprintpcs.media;

import java.io.ByteArrayOutputStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import org.recompile.mobile.Mobile; 

/* Probably the basic DTMF (Dual-Tone Multi-Frequency) format, or something analogous */
public class DualTone 
{
	private static final double SEMITONE_CONST = 17.31234049066755; // 1/(ln(2^(1/12)))

	private static final int PPQ = 24; // Ticks per quarter note
    private static final int BPM = 120; // Default BPM, not sure if DTFM changes this
    private static final double TICKS_PER_MILLISECOND = PPQ / (BPM * 4.0); // 4 quarter notes in a minute
	
	byte[] sequence;
	int priority, vibration;

	/* Implementation Idea: Dual Tone can just be made a single midi sequence with two tracks, one for each tone sequence. */
	public DualTone(int[] x_frequencies, int[] y_frequencies, int[] durations, int priority, int vibration) 
	{
        try
		{
			this.priority = priority;
			this.vibration = vibration;

            Sequence midiSequence = new Sequence(Sequence.PPQ, PPQ);
            Track trackA = midiSequence.createTrack();
			Track trackB = midiSequence.createTrack();

			// Like siemens' MelodyComposer, use a square wave instrument here.
			trackA.add(new MidiEvent(new ShortMessage(ShortMessage.PROGRAM_CHANGE, 0, 80, 0), 0));
			trackB.add(new MidiEvent(new ShortMessage(ShortMessage.PROGRAM_CHANGE, 0, 80, 0), 0));

			// Start from tick 0, and move onwards afte reading each note pair's duration.
            long currentTick = 0;
			int tmpNote;

			// x_frequencies and y-frequencies are expected to always be the same length, same for durations
            for (int i = 0; i < x_frequencies.length; i++) 
			{
                tmpNote = convertFreqToNote(x_frequencies[i]);

                int durationInMillis = durations[i];
                // Convert duration from milliseconds to ticks
                long durationInTicks = Math.round(durationInMillis * TICKS_PER_MILLISECOND);

				trackA.add(createMidiEvent(ShortMessage.NOTE_ON, tmpNote, 93, currentTick));
				trackA.add(createMidiEvent(ShortMessage.NOTE_OFF, tmpNote, 0, currentTick + durationInTicks));

				tmpNote = convertFreqToNote(y_frequencies[i]);

				trackB.add(createMidiEvent(ShortMessage.NOTE_ON, tmpNote, 93, currentTick));
				trackB.add(createMidiEvent(ShortMessage.NOTE_OFF, tmpNote, 0, currentTick + durationInTicks));

                currentTick += durationInTicks;
            }

			ByteArrayOutputStream output = new ByteArrayOutputStream();
			MidiSystem.write(midiSequence, 1, output);
			sequence = output.toByteArray();
        }
		catch (Exception e) 
		{
            Mobile.log(Mobile.LOG_ERROR, DualTone.class.getPackage().getName() + "." + DualTone.class.getSimpleName() + ": " + " failed to create DualTone:" + e.getMessage());
        }
    }

	public int getPriority() { return priority; }

	public int getVibration() { return vibration; }

	private int convertFreqToNote(int frequency) 
	{
        return (int) (Math.round(Math.log((double) frequency / 8.176) * SEMITONE_CONST)); // Adjust to MIDI note A4 (440 Hz = note 69)
    }

	private MidiEvent createMidiEvent(int type, int note, int velocity, long tick) throws InvalidMidiDataException 
	{
        ShortMessage message = new ShortMessage();
        message.setMessage(type, 0, note, velocity);
        return new MidiEvent(message, tick);
    }
}
