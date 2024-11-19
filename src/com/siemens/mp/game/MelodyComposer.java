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

package com.siemens.mp.game;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import org.recompile.mobile.Mobile;


public class MelodyComposer
{
	public static final short MAX_NOTES = 32767;

	public static final int TONE_C0 = 0;
	public static final int TONE_CIS0 = 1;
	public static final int TONE_D0 = 2;
	public static final int TONE_DIS0 = 3;
	public static final int TONE_E0 = 4;
	public static final int TONE_F0 = 5;
	public static final int TONE_FIS0 = 6;
	public static final int TONE_G0 = 7;
	public static final int TONE_GIS0 = 8;
	public static final int TONE_A0 = 9;
	public static final int TONE_AIS0 = 10;
	public static final int TONE_H0 = 11;
	public static final int TONE_C1 = 12;
	public static final int TONE_CIS1 = 13;
	public static final int TONE_D1 = 14;
	public static final int TONE_DIS1 = 15;
	public static final int TONE_E1 = 16;
	public static final int TONE_F1 = 17;
	public static final int TONE_FIS1 = 18;
	public static final int TONE_G1 = 19;
	public static final int TONE_GIS1 = 20;
	public static final int TONE_A1 = 21;
	public static final int TONE_AIS1 = 22;
	public static final int TONE_H1 = 23;
	public static final int TONE_C2 = 24;
	public static final int TONE_CIS2 = 25;
	public static final int TONE_D2 = 26;
	public static final int TONE_DIS2 = 27;
	public static final int TONE_E2 = 28;
	public static final int TONE_F2 = 29;
	public static final int TONE_FIS2 = 30;
	public static final int TONE_G2 = 31;
	public static final int TONE_GIS2 = 32;
	public static final int TONE_A2 = 33;
	public static final int TONE_AIS2 = 34;
	public static final int TONE_H2 = 35;
	public static final int TONE_C3 = 36;
	public static final int TONE_CIS3 = 37;
	public static final int TONE_D3 = 38;
	public static final int TONE_DIS3 = 39;
	public static final int TONE_E3 = 40;
	public static final int TONE_F3 = 41;
	public static final int TONE_FIS3 = 42;
	public static final int TONE_G3 = 43;
	public static final int TONE_GIS3 = 44;
	public static final int TONE_A3 = 45;
	public static final int TONE_AIS3 = 46;
	public static final int TONE_H3 = 47;
	public static final int TONE_C4 = 48;
	public static final int TONE_CIS4 = 49;
	public static final int TONE_D4 = 50;
	public static final int TONE_DIS4 = 51;
	public static final int TONE_E4 = 52;
	public static final int TONE_F4 = 53;
	public static final int TONE_FIS4 = 54;
	public static final int TONE_G4 = 55;
	public static final int TONE_GIS4 = 56;
	public static final int TONE_A4 = 57;
	public static final int TONE_PAUSE = 58;
	public static final int NO_TONE = 59;
	public static final int TONE_STOP = 60;
	public static final int TONE_REPEAT = 61;
	public static final int TONE_REPEV = 62;
	public static final int TONE_REPON = 63;
	public static final int TONE_MARK = 64;
	public static final int TONE_REPEAT_MARK = 65;
	public static final int TONE_REPEV_MARK = 66;
	public static final int TONE_REPON_MARK = 67;
	public static final int TONELENGTH_1_1 = 0;
	public static final int TONELENGTH_1_2 = 1;
	public static final int TONELENGTH_1_4 = 2;
	public static final int TONELENGTH_1_8 = 3;
	public static final int TONELENGTH_1_16 = 4;
	public static final int TONELENGTH_1_32 = 5;
	public static final int TONELENGTH_1_64 = 6;
	public static final int TONELENGTH_DOTTED_1_1 = 7;
	public static final int TONELENGTH_DOTTED_1_2 = 8;
	public static final int TONELENGTH_DOTTED_1_4 = 9;
	public static final int TONELENGTH_DOTTED_1_8 = 10;
	public static final int TONELENGTH_DOTTED_1_16 = 11;
	public static final int TONELENGTH_DOTTED_1_32 = 12;
	public static final int TONELENGTH_DOTTED_1_64 = 13;
	
	public static final int BPM = 60;

	private Sequence tmpSequence;
	private Track tmpTrack;
	private Melody curMelody;
	private int curTick = 0;
	private int lastMark = 0; // Used for TONE_MARK

	private final int[] tmpNoteArray = new int[MAX_NOTES*2]; // This will hold notes AND their length, hence why it's 2 * MAX_NOTES
	private int tmpNoteArrayIdx = 0;

	/* Jars are expected to call this before appending notes or doing anything else, so prepare an empty melody (sequence and track) */
	public MelodyComposer() 
	{
		curMelody = new Melody();
		setBPM(BPM); // This will create a new track with the default BPM
		curMelody.len = 0;
		
	}

	// No need for tmpNoteArray here, the notes array is what we'll use to determine repeats and such
	public MelodyComposer(int[] notes, int bpm) 
	{ 
		curMelody = new Melody();
		setBPM(bpm); // This will create a new track with the received BPM
		curMelody.bpm = bpm;

		/* The notes array is a pair of [note, length] */
		for (int i = 0; i < notes.length; i += 2) 
		{
			if(notes[i] == TONE_REPEAT) 
			{
				Mobile.log(Mobile.LOG_DEBUG, MelodyComposer.class.getPackage().getName() + "." + MelodyComposer.class.getSimpleName() + ": " + "TONE_REPEAT!");
				for(int rep = 0; rep < notes[i+1]; rep++) // Repeat N times from the beginning until the current position
				{
					for(int repindex= 0; repindex < i; repindex += 2) 
					{
						if(notes[repindex] != TONE_REPEAT && 
							notes[repindex] != TONE_MARK && 
							notes[repindex] != TONE_REPEAT_MARK &&
							notes[repindex] != TONE_REPEV &&
							notes[repindex] != TONE_REPEV_MARK &&
							notes[repindex] != TONE_REPON &&
							notes[repindex] != TONE_REPON_MARK) // Only append actual notes, or else this would become an infinite loop
						{
							appendNote(notes[repindex], notes[repindex+1]);
						}
					}
				}
			}
			else if (notes[i] == TONE_REPEAT_MARK) 
			{
				Mobile.log(Mobile.LOG_DEBUG, MelodyComposer.class.getPackage().getName() + "." + MelodyComposer.class.getSimpleName() + ": " + "TONE_REPEAT_MARK!");
				for(int rep = 0; rep < notes[i+1]; rep++) // Repeat N times from the beginning until the last mark
				{
					for(int repindex= lastMark; repindex < i; repindex += 2) 
					{
						if(notes[repindex] != TONE_REPEAT && 
							notes[repindex] != TONE_MARK && 
							notes[repindex] != TONE_REPEAT_MARK &&
							notes[repindex] != TONE_REPEV &&
							notes[repindex] != TONE_REPEV_MARK &&
							notes[repindex] != TONE_REPON &&
							notes[repindex] != TONE_REPON_MARK) // Only append actual notes, or else this would become an infinite loop
						{
							appendNote(notes[repindex], notes[repindex+1]);
						}
					}
				}
			}

			else if(notes[i] == TONE_MARK) { lastMark = i; } // This should only set a mark

			else { appendNote(notes[i], notes[i + 1]); }	
		
			/* 
			 * TONE_REPEV, TONE_REPEV_MARK, TONE_REPON, TONE_REPON_MARK not handled, as handling 
			 * them would require a different playback method involving a state machine that uses Manager.playTone()
			 * to play notes. Would be a decent rewrite, since that would have to also work with PlayerListener and other Player 
			 * structures
			 */
		}

		/* Due to that, the real count of notes has to be half of the array's length */
		curMelody.len = notes.length/2;
	}
	
	public void appendNote(int note, int length) 
	{
		Mobile.log(Mobile.LOG_DEBUG, MelodyComposer.class.getPackage().getName() + "." + MelodyComposer.class.getSimpleName() + ": " + "Adding note-length:" + note + " - " + length);
		if (note == TONE_PAUSE || note == NO_TONE) // Assume TONE_PAUSE and NO_TONE are the same for now, couldn't find an example of NO_TONE being used yet to confirm. 
		{
			final int pauseTicks = convertLengthToTicks(length);
			curTick += pauseTicks; // Just advance the current tick count to "pause" by the given length (at least it's what jars seem to use this for on their code)
			return;
		}
		if(note == TONE_REPEAT) 
			{
				Mobile.log(Mobile.LOG_INFO, MelodyComposer.class.getPackage().getName() + "." + MelodyComposer.class.getSimpleName() + ": " + "TONE_REPEAT!");
				for(int rep = 0; rep < length; rep++) // Repeat N times from the beginning until the current position
				{
					for(int repindex= 0; repindex <= tmpNoteArrayIdx; repindex += 2) // <= because we aren't operating directly on the received array like in MelodyComposer()
					{
						if(tmpNoteArray[repindex] != TONE_REPEAT && 
							tmpNoteArray[repindex] != TONE_MARK && 
							tmpNoteArray[repindex] != TONE_REPEAT_MARK &&
							tmpNoteArray[repindex] != TONE_REPEV &&
							tmpNoteArray[repindex] != TONE_REPEV_MARK &&
							tmpNoteArray[repindex] != TONE_REPON &&
							tmpNoteArray[repindex] != TONE_REPON_MARK) // Only append actual notes, or else this would become an infinite loop
						{
							appendNote(tmpNoteArray[repindex], tmpNoteArray[repindex+1]);
						}
					}
				}
			}
			else if (note == TONE_REPEAT_MARK) 
			{
				Mobile.log(Mobile.LOG_DEBUG, MelodyComposer.class.getPackage().getName() + "." + MelodyComposer.class.getSimpleName() + ": " + "TONE_REPEAT_MARK!");
				for(int rep = 0; rep < length; rep++) // Repeat N times from the beginning until the last mark
				{
					for(int repindex = lastMark; repindex <= tmpNoteArrayIdx; repindex += 2) 
					{
						if(tmpNoteArray[repindex] != TONE_REPEAT && 
							tmpNoteArray[repindex] != TONE_MARK && 
							tmpNoteArray[repindex] != TONE_REPEAT_MARK &&
							tmpNoteArray[repindex] != TONE_REPEV &&
							tmpNoteArray[repindex] != TONE_REPEV_MARK &&
							tmpNoteArray[repindex] != TONE_REPON &&
							tmpNoteArray[repindex] != TONE_REPON_MARK) // Only append actual notes, or else this would become an infinite loop
						{
							appendNote(tmpNoteArray[repindex], tmpNoteArray[repindex+1]);
						}
					}
				}
			}
		else if(note == TONE_MARK) { lastMark = tmpNoteArrayIdx; }
		else 
		{
			try 
			{
				tmpNoteArray[tmpNoteArrayIdx * 2] = note;
				tmpNoteArray[tmpNoteArrayIdx * 2 + 1] = convertLengthToTicks(length);

				tmpTrack.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, 0, mapToMidi(tmpNoteArray[tmpNoteArrayIdx * 2]), 93), curTick));
				tmpTrack.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, mapToMidi(tmpNoteArray[tmpNoteArrayIdx * 2]), 0), curTick+tmpNoteArray[tmpNoteArrayIdx * 2 + 1]));
				curTick += tmpNoteArray[tmpNoteArrayIdx * 2 + 1];
				tmpNoteArrayIdx++;
				curMelody.len++;
			} 
			catch (InvalidMidiDataException e) 
			{
				Mobile.log(Mobile.LOG_ERROR, MelodyComposer.class.getPackage().getName() + "." + MelodyComposer.class.getSimpleName() + ": " + " couldn't add Melody note" + note  + ":" + e.getMessage());
			}
		}
	}

	private int convertLengthToTicks(int length)
	{
        int ticksPerQuarterNote = 60000 / curMelody.bpm; // Adjust as necessary
        switch (length) 
		{
            case TONELENGTH_1_1: return ticksPerQuarterNote;
            case TONELENGTH_1_2: return ticksPerQuarterNote / 2;
            case TONELENGTH_1_4: return ticksPerQuarterNote / 4;
            case TONELENGTH_1_8: return ticksPerQuarterNote / 8;
            case TONELENGTH_1_16: return ticksPerQuarterNote / 16;
			case TONELENGTH_1_32: return ticksPerQuarterNote / 32;
			case TONELENGTH_1_64: return ticksPerQuarterNote / 64;
			case TONELENGTH_DOTTED_1_1: return (ticksPerQuarterNote * 3) / 2;
			case TONELENGTH_DOTTED_1_2: return (ticksPerQuarterNote * 3) / 4;
			case TONELENGTH_DOTTED_1_4: return (ticksPerQuarterNote * 3) / 8;
			case TONELENGTH_DOTTED_1_8: return (ticksPerQuarterNote * 3) / 16;
			case TONELENGTH_DOTTED_1_16: return (ticksPerQuarterNote * 3) / 32;
			case TONELENGTH_DOTTED_1_32: return (ticksPerQuarterNote * 3) / 64;
			case TONELENGTH_DOTTED_1_64: return (ticksPerQuarterNote * 3) / 128;
            default: return 0;
        }
    }

	private int mapToMidi(int tone) 
	{
		switch (tone) 
		{
			case TONE_C0: return 12;  // C0
			case TONE_CIS0: return 13; // C#0
			case TONE_D0: return 14;   // D0
			case TONE_DIS0: return 15; // D#0
			case TONE_E0: return 16;   // E0
			case TONE_F0: return 17;   // F0
			case TONE_FIS0: return 18; // F#0
			case TONE_G0: return 19;   // G0
			case TONE_GIS0: return 20; // G#0
			case TONE_A0: return 21;   // A0
			case TONE_AIS0: return 22; // A#0
			case TONE_H0: return 23;   // B0
			case TONE_C1: return 24;   // C1
			case TONE_CIS1: return 25; // C#1
			case TONE_D1: return 26;   // D1
			case TONE_DIS1: return 27; // D#1
			case TONE_E1: return 28;   // E1
			case TONE_F1: return 29;   // F1
			case TONE_FIS1: return 30; // F#1
			case TONE_G1: return 31;   // G1
			case TONE_GIS1: return 32; // G#1
			case TONE_A1: return 33;   // A1
			case TONE_AIS1: return 34; // A#1
			case TONE_H1: return 35;   // B1
			case TONE_C2: return 36;   // C2
			case TONE_CIS2: return 37; // C#2
			case TONE_D2: return 38;   // D2
			case TONE_DIS2: return 39; // D#2
			case TONE_E2: return 40;   // E2
			case TONE_F2: return 41;   // F2
			case TONE_FIS2: return 42; // F#2
			case TONE_G2: return 43;   // G2
			case TONE_GIS2: return 44; // G#2
			case TONE_A2: return 45;   // A2
			case TONE_AIS2: return 46; // A#2
			case TONE_H2: return 47;   // B2
			case TONE_C3: return 48;   // C3
			case TONE_CIS3: return 49; // C#3
			case TONE_D3: return 50;   // D3
			case TONE_DIS3: return 51; // D#3
			case TONE_E3: return 52;   // E3
			case TONE_F3: return 53;   // F3
			case TONE_FIS3: return 54; // F#3
			case TONE_G3: return 55;   // G3
			case TONE_GIS3: return 56; // G#3
			case TONE_A3: return 57;   // A3
			case TONE_AIS3: return 58; // A#3
			case TONE_H3: return 59;   // B3
			case TONE_C4: return 60;   // C4
			case TONE_CIS4: return 61; // C#4
			case TONE_D4: return 62;   // D4
			case TONE_DIS4: return 63; // D#4
			case TONE_E4: return 64;   // E4
			case TONE_F4: return 65;   // F4
			case TONE_FIS4: return 66; // F#4
			case TONE_G4: return 67;   // G4
			case TONE_GIS4: return 68; // G#4
			case TONE_A4: return 69;   // A4
			case NO_TONE: return 0;    // no tone (silence)
			default: return -1;        // unknown tone
		}
	}

	public Melody getMelody() 
	{
		curMelody.populateMelody(convertMelody(tmpSequence)); // Send MIDI melody converted to a byte array
		return curMelody;
	}

	/* Let's assume a max of 32767 notes for now, should be more than enough */
	public static int maxLength() { return (int) MAX_NOTES; }

	/* This should return the current melody's tone count */
	public int length() { return curMelody.len; }

	/* Here, we can basically create a new melody from scratch with default BPM */
	public void resetMelody()
	{ 
		Mobile.log(Mobile.LOG_DEBUG, MelodyComposer.class.getPackage().getName() + "." + MelodyComposer.class.getSimpleName() + ": " + "Reset Melody!");
		setBPM(BPM);
		curMelody = new Melody();
		curTick = 0;
		lastMark = 0;
		tmpNoteArrayIdx = 0;
	}

	/* This is always called after a Melody() constructor or resetMelody() so creating a new sequence and track should be safe */
	public void setBPM(int bpm) 
	{  
        try 
		{ 
			curMelody.bpm = bpm; 
			tmpSequence = new Sequence(Sequence.PPQ, 60000 / bpm);
			tmpTrack = tmpSequence.createTrack();
			tmpTrack.add(new MidiEvent(new ShortMessage(ShortMessage.PROGRAM_CHANGE, 0, 80, 0), 0)); // 80 is the Square Wave / Lead 1 instrument, which we'll use to get closer to what this should sound like
		} 
		catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, MelodyComposer.class.getPackage().getName() + "." + MelodyComposer.class.getSimpleName() + ": " + " couldn't create new Melody with BPM:" + e.getMessage()); }
	}

	private byte[] convertMelody(Sequence melodySequence) 
	{
		try 
		{
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			MidiSystem.write(melodySequence, 1, output);
			return output.toByteArray();
		}
		catch (IOException e) { Mobile.log(Mobile.LOG_ERROR, MelodyComposer.class.getPackage().getName() + "." + MelodyComposer.class.getSimpleName() + ": " + " couldn't write Melody:" + e.getMessage()); return null;}
	}
}