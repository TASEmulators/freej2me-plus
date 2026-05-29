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
package javax.microedition.media.decoders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import org.recompile.mobile.Mobile;

public class NokiaOTTDecoder 
{
	/* Note style defaults. */
	public static final int NATURAL_STYLE = 0;
	public static final int CONTINUOUS_STYLE = 1;
	public static final int STACCATO_STYLE = 2;

	/*
	 * There's a freq table in: https://github.com/SymbianSource/oss.FCL.sf.app.JRT/blob/0822c2dcfb807a245ec84ab06006b59df7aedab6/javauis/nokiasound/javasrc/com/nokia/mid/sound/Sound.java
	 * 
	 * But using this single tone frequency multiplier has the same end result when converting, 
	 * and is far easier to understand throughout the code.
	 * It's also provided by the J2ME Docs: https://docs.oracle.com/javame/config/cldc/ref-impl/midp2.0/jsr118/javax/microedition/media/control/ToneControl.html
	 */
	private static final double SEMITONE_CONST = 17.31234049066755; // 1/(ln(2^(1/12)))

	private static int parsePos = 0; // Used exclusively as a marker for OTA/OTT Parsing
	private static byte curBitPos = 0; // Current bit position in the current parsePos
	private static byte[] data;
	private static float noteScale = 1f; // Default scale of 880Hz
	private static int noteStyle = NATURAL_STYLE; // The default style is NATURAL
	private static int curTick = 0; // To keep track of the current midi note tick, or else all notes will play at the same time.

	/* These are used for pattern specifiers that reuse a prior pattern */
	private static int lastPatternPos = 0;
	private static byte lastPatternBitPos = 0;
	private static int lastPatternLen = 0;
	private static int restorePatternPos = 0;
	private static byte restorePatternBitPos = 0;

	// This one is used for debugging.
	private static final String[] noteStrings = new String[] {"Pause", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "H", "Reserved", "Reserved", "Reserved"};


    public static int convertFreqToNote(int freq) { return (int) (Math.round(Math.log((double) freq / 8.176) * SEMITONE_CONST)); }

	public static synchronized byte[] convertToMidi(byte[] data) throws MidiUnavailableException, IOException  // Start by parsing the OTT Header
	{
		try 
		{
			parsePos = 0; // Reset the parsePos counter
			curBitPos = 0; // Reset current bit position
			noteScale = 1f; // Reset scale as well
			noteStyle = NATURAL_STYLE; // Reset note style too
			curTick = 0; // Also move curTick to the beginning
			NokiaOTTDecoder.data = data; // Save the byte array's reference for reading

			// Create a new sequence and track for the converted tone
			Sequence sequence = new Sequence(Sequence.PPQ, 24);
			Track track = sequence.createTrack();
			ShortMessage bankMSB = new ShortMessage();
			ShortMessage bankLSB = new ShortMessage();
			ShortMessage programChange = new ShortMessage();

			bankMSB.setMessage(ShortMessage.CONTROL_CHANGE, 0, 0, 1); // Bank change MSB (Bank 1)
			bankLSB.setMessage(ShortMessage.CONTROL_CHANGE, 0, 32, 0); // Bank change LSB
			programChange.setMessage(ShortMessage.PROGRAM_CHANGE, 0, 80, 0); // 80 is the Square Wave / Lead 1 instrument, which we'll use to get closer to what this should sound like

			track.add(new MidiEvent(bankMSB, 0));
			track.add(new MidiEvent(bankLSB, 1));
			track.add(new MidiEvent(programChange, 0));
		
			// Validate command length
			int commandLength = readBits(8); // Command Length is 8 bits, so get them from the bit array.
			Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Command length: " + commandLength);
		
			for (int i = 0; i < commandLength; i++) 
			{
				int commandType = readBits(8); // Check command type (first 7 bits + filler bit which is always 0)
		
				switch (commandType) 
				{
					case 0x4A: // Ringing tone programming
						Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Ringing tone programming detected.");
						parseRingingTone(track);
						i++; // We processed another command inside ringingTone
						break;
					case 0x44: // Unicode
						Mobile.log(Mobile.LOG_WARNING, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Unicode detected.");
						parseUnicode();
						break;
					case 0x3A: // Sound
						Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Sound detected.");
						parseSound(track);
						break;
					case 0xA: // Cancel command, the specification says nothing about what it really does... Does any actual OTT/OTA ringtone use this?
						if(readBits(7) == 0x05)
						{
							parseUnicode();
						}
						else 
						{
							throw new IllegalArgumentException("Invalid cancel command specifier");
						}
						Mobile.log(Mobile.LOG_WARNING, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Cancel command detected. Returning.");
						break;
					case 0x0: // This should happen at the end of every parsing procedure.
						Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "End of ringtone programming!");
						break;
					default: // If this is the case, we can't parse the header, so just return outright
						Mobile.log(Mobile.LOG_ERROR, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Unknown command type: " + Integer.toBinaryString(commandType));
						break;
				}
			}
		
			// Convert the resulting sequence to byte array and send to the player.
			try 
			{
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				MidiSystem.write(sequence, 0, output);
				return output.toByteArray();
			}
			catch (IOException e) { Mobile.log(Mobile.LOG_ERROR, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + " couldn't write converted Tone Sequence:" + e.getMessage()); return null;}
		} 
		catch(InvalidMidiDataException e) { Mobile.log(Mobile.LOG_ERROR, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + " couldn't convert Tone Sequence:" + e.getMessage()); return null;}
	}

	private static void parseRingingTone(Track track) 
	{
		/* 
		 * If we found a <ringing-tone-programming> string, that means that up next
		 * it's either a <unicode> or a <sound> bit string
		 */
		int nextCheck = readBits(7);
		
		if(nextCheck == 0x1D) 
		{ 
			Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Detected Sound!" );
			parseSound(track);
		} 
		else if(nextCheck == 0x22) 
		{
			// We must read a unicode, which will be placed before any Sound
			parseUnicode();
		}
		else
		{
			throw new IllegalArgumentException("Invalid set of bits for ringing-tone-programming");
		}
	}

	// A unicode is defined in the spec as a 16-bit UCS-2 encoded char
	private static void parseUnicode()
	{
		short unicode = (short) readBits(16);
		Mobile.log(Mobile.LOG_WARNING, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Unicode:" + unicode);
	}

	private static void parseSound(Track track) 
	{
		// Read song type
		int songType = readBits(3); // 3 bits are used to represent the song type

		switch (songType) 
		{
			case 0x1: // Basic song type
				Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Basic Song Detected!");
				parseBasicSong(track);
				break;
			case 0x2: // Temporary song type
				Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Temporary Song Detected!");
				parseTemporarySong(track);
				break;

			// These are all 'reserved for future extension' in the latest
			// spec revision i have (v3.0.0), likely never actually used.
			case 0x3: // MIDI song type
				Mobile.log(Mobile.LOG_WARNING, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "MIDI Song Detected!");
				throw new IllegalArgumentException("Unsupported song format");
			case 0x4: // Digitized song type
				Mobile.log(Mobile.LOG_WARNING, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Digitized Song Detected!");
				throw new IllegalArgumentException("Unsupported song format");
			case 0x5: // Polyphonic song type
				Mobile.log(Mobile.LOG_WARNING, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Polyphonic Song Detected!");
				throw new IllegalArgumentException("Unsupported song format");
			default:
				Mobile.log(Mobile.LOG_ERROR, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Unknown song type: " + Integer.toBinaryString(songType));
				throw new IllegalArgumentException("Unsupported song format");
		}
	}

	private static void parseBasicSong(Track track) 
	{
		// Read title length (upper 4 bits)
		int titleLength = readBits(4);

		StringBuilder title = new StringBuilder();
		for (int i = 0; i < titleLength; i++) 
		{
			char character = (char) readBits(8);
			title.append(character);
		}
		Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Title Length:" + titleLength + " | Basic Song Title: " + title.toString());
		
		// Read song sequence length
		parseTemporarySong(track);
	}
	
	private static void parseTemporarySong(Track track) 
	{
		// Read song sequence length
		int songSequenceLength = readBits(8); // Read the number of patterns
		Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Song Sequence Length: " + songSequenceLength);
	
		// Parse each song pattern
		for (int i = 0; i < songSequenceLength; i++) { parseSongPattern(track); }
	}
	
	private static void parseSongPattern(Track track) 
	{
		// Read the pattern header
		int patternHeader = readBits(3); // 3 bits for Pattern Header's beginning (000) 
		int patternId = readBits(2); // 2 bits for pattern ID
		int loopValue = readBits(4); // 4 bits for loop value

		Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Pattern Header - ID: " + patternHeader + ", Pattern ID: " + patternId + ", Loop Value: " + loopValue);
	
		if(loopValue == 0xF) { Mobile.log(Mobile.LOG_WARNING, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "OTA/OTT Tone Infinite Loop parsing is not implemented. Parsing pattern without loop..."); loopValue = 0; }

		// Marker for the current pattern start position, as we'll re-read it as many times as there are loops.
		int loopParsePosMark = parsePos; 
		byte loopCurBitPos = curBitPos;

		while(loopValue >= 0) // LoopValue == 0 still means the pattern has to be entirely parsed at least one time.
		{
			parsePos = loopParsePosMark;
			curBitPos = loopCurBitPos;

			// Read the pattern specifier
			int patternSpecifier = readBits(8);
			
			// For specifier 0b00000000, we must reuse the prior pattern
			if (patternSpecifier == 0x0)
			{
				Mobile.log(Mobile.LOG_WARNING, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Using already-defined pattern.");
				
				// Well restore back to this position after reusing a pattern
				restorePatternPos = parsePos;
				restorePatternBitPos = curBitPos;

				// Parse/Loop the last known pattern
				while(loopValue >= 0) 
				{
					Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "New pattern length: " + patternSpecifier);
			
					parsePos = lastPatternPos;
					curBitPos = lastPatternBitPos;

					for (int j = 0; j < lastPatternLen; j++) { parsePatternInstruction(track); }
					loopValue--;
				}

				// We can now continue reading the next bits
				parsePos = restorePatternPos;
				curBitPos = restorePatternBitPos;

				// We already looped internally, so we can return outright
				return;
			}

			// This means we have a new pattern length
			else 
			{
				Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "New pattern length: " + patternSpecifier);
				int numberOfInstructions = patternSpecifier; // The number of instructions to read
		
				lastPatternLen = numberOfInstructions;
				lastPatternPos = parsePos;
				lastPatternBitPos = curBitPos;

				// Reset note Style and Scale, otherwise they will carry over from the last pattern (which is incorrect despite the Smart Message API not disclosing it)
				noteStyle = NATURAL_STYLE;
				noteScale = 1f;

				for (int j = 0; j < numberOfInstructions; j++) { parsePatternInstruction(track); }
			}
			loopValue--; // We completed a loop, so decrease the counter.
		}
		
	}
	
	private static void parsePatternInstruction(Track track) 
	{
		// Read the instruction type (could be a note, scale, style, tempo, or volume)
		int instructionType = readBits(3); // 3 bits for instruction ID

		Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "instructionType: " + instructionType);

		switch (instructionType) 
		{
			case 0x1: // Note Instruction
				parseNoteInstruction(track);
				break;
			case 0x2: // Scale Instruction
				parseScaleInstruction();
				break;
			case 0x3: // Style Instruction
				parseStyleInstruction();
				break;
			case 0x4: // Tempo Instruction
				parseTempoInstruction(track);
				break;
			case 0x5: // Volume Instruction
				parseVolumeInstruction(track);
				break;
			default:
				Mobile.log(Mobile.LOG_ERROR, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Unknown instruction type: " + Integer.toBinaryString(instructionType));
				break;
		}
	}
	
	private static void parseNoteInstruction(Track track) 
	{
		int noteValue = readBits(4); // 4 bits for note value
		int noteDuration = readBits(3); // 3 bits for duration
		int durationSpecifier = readBits(2); // Read next byte for duration specifier
	
		// Convert note value to MIDI note number (C4 = 60)
		int midiNote = convertNoteValueToMidi(noteValue);
		
		// Calculate duration in ticks (depends on MIDI PPQ and duration settings)
		int ticks = convertDurationToTicks(noteDuration, durationSpecifier);

		Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "noteDuration: " + noteDuration + "| durationSpecifier: " + durationSpecifier);
	
		// Create MIDI events for the note, accounting for the current Note Style.
		try 
		{
			if(midiNote != -1) 
			{
				ShortMessage noteOn = new ShortMessage();
				ShortMessage noteOff = new ShortMessage();
				
				noteOn.setMessage(ShortMessage.NOTE_ON, 0, midiNote, 93);
				track.add(new MidiEvent(noteOn, curTick));

				if(noteStyle == STACCATO_STYLE) // STACCATO has shorter notes with longer rest by making NOTE_OFF end way before the next note's NOTE_ON
				{
					noteOff.setMessage(ShortMessage.NOTE_OFF, 0, midiNote, 0);
					track.add(new MidiEvent(noteOff, curTick + (ticks / 2)));
				}
				else if (noteStyle == CONTINUOUS_STYLE) // Notes flow into each other
				{
					noteOff.setMessage(ShortMessage.NOTE_OFF, 0, midiNote, 0);
					track.add(new MidiEvent(noteOff, curTick + ticks));
				}
				else // NATURAL adds notes with a small rest between them.
				{
					noteOff.setMessage(ShortMessage.NOTE_OFF, 0, midiNote, 0);
					track.add(new MidiEvent(noteOff, curTick + (ticks * 20 / 21)));
				}
			}
			
			curTick += ticks;
		}
		catch (InvalidMidiDataException e) { Mobile.log(Mobile.LOG_ERROR, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Couldn't parse note instruction:" + e.getMessage()); }
	}

	private static void parseScaleInstruction() 
	{
		int scaleValue = readBits(2); // 2 bits are used for scale value

		switch (scaleValue) 
		{
			case 0x0:
				Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Scale-1: A = 440 Hz");
				noteScale = 0.5f;
				break;
			case 0x1:
				Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Scale-2: A = 880 Hz (default)");
				noteScale = 1f;
				break;
			case 0x2:
				Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Scale-3: A = 1.76 kHz");
				noteScale = 2f;
				break;
			case 0x3:
				Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Scale-4: A = 3.52 kHz");
				noteScale = 4f;
				break;
			default:
				Mobile.log(Mobile.LOG_ERROR, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Unknown scale value");
				break;
		}
	}
	
	private static void parseStyleInstruction() 
	{
		int styleValue = readBits(2); // 2 bits for style value

		switch (styleValue) 
		{
			case 0x0:
				Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Natural Style (rest between notes)");
				noteStyle = NATURAL_STYLE;
				break;
			case 0x1:
				Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Continuous Style (no rest between notes)");
				noteStyle = CONTINUOUS_STYLE;
				break;
			case 0x2:
				Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Staccato Style (shorter notes)");
				noteStyle = STACCATO_STYLE;
				break;
			case 0x3:
				Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "RESERVED");
				break;
			default:
				Mobile.log(Mobile.LOG_ERROR, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Unknown style value");
				break;
		}
	}
	
	private static void parseTempoInstruction(Track track) 
	{
		int bpmValue = readBits(5); // 5 bits for BPM
		int bpm = 0;

		// Map the binary value to actual BPM values based on the table provided by Smart Messaging v2.1.0/v3.0.0
		switch (bpmValue) 
		{
			case 0x00: bpm = 25; break;
			case 0x01: bpm = 28; break;
			case 0x02: bpm = 31; break;
			case 0x03: bpm = 35; break;
			case 0x04: bpm = 40; break;
			case 0x05: bpm = 45; break;
			case 0x06: bpm = 50; break;
			case 0x07: bpm = 56; break;
			case 0x08: bpm = 63; break;
			case 0x09: bpm = 70; break;
			case 0x0A: bpm = 80; break;
			case 0x0B: bpm = 90; break;
			case 0x0C: bpm = 100; break;
			case 0x0D: bpm = 112; break;
			case 0x0E: bpm = 125; break;
			case 0x0F: bpm = 140; break;
			case 0x10: bpm = 160; break;
			case 0x11: bpm = 180; break;
			case 0x12: bpm = 200; break;
			case 0x13: bpm = 225; break;
			case 0x14: bpm = 250; break;
			case 0x15: bpm = 285; break;
			case 0x16: bpm = 320; break;
			case 0x17: bpm = 355; break;
			case 0x18: bpm = 400; break;
			case 0x19: bpm = 450; break;
			case 0x1A: bpm = 500; break;
			case 0x1B: bpm = 565; break;
			case 0x1C: bpm = 635; break;
			case 0x1D: bpm = 715; break;
			case 0x1E: bpm = 800; break;
			case 0x1F: bpm = 900; break;
			default: Mobile.log(Mobile.LOG_ERROR, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Unknown BPM value");
		}

		int microsecondsPerBeat = 60000000 / bpm; 
		try 
		{
			MetaMessage metaMessage = new MetaMessage();
			metaMessage.setMessage(0x51, new byte[] 
			{
				(byte) (microsecondsPerBeat >> 16),
				(byte) (microsecondsPerBeat >> 8),
				(byte) (microsecondsPerBeat)
			}, 3);
			track.add(new MidiEvent(metaMessage, curTick)); // Add BPM change event at the current tick pos
			Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Tempo Instruction - BPM: " + bpm);
		}
		catch (InvalidMidiDataException e) { Mobile.log(Mobile.LOG_ERROR, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Error adding BPM event:" + e.getMessage()); }
	}
	
	private static void parseVolumeInstruction(Track track) 
	{
		int volumeValue = readBits(4); // 4 bits for volume level
		int midiVolume = 0; // Initialize MIDI volume

		// Approximately map the parsed volume value range (0-15) to the usual MIDI range (0-127)
		switch (volumeValue) 
		{
			case 0x0: // tone-off
				midiVolume = 0;
				break;
			case 0x1:
				midiVolume = 48;
				break;
			case 0x2:
				midiVolume = 56;
				break;
			case 0x3:
				midiVolume = 64;
				break;
			case 0x4:
				midiVolume = 72;
				break;
			case 0x5:
				midiVolume = 80;
				break;
			case 0x6:
				midiVolume = 88;
				break;
			case 0x7: // This is the default volume level (7)
				midiVolume = 92;
				break;
			case 0x8:
				midiVolume = 100;
				break;
			case 0x9:
				midiVolume = 104;
				break;
			case 0xA:
				midiVolume = 108;
				break;
			case 0xB:
				midiVolume = 112;
				break;
			case 0xC:
				midiVolume = 116;
				break;
			case 0xD:
				midiVolume = 120;
				break;
			case 0xE:
				midiVolume = 124;
				break;
			case 0xF:
			default:
				midiVolume = 127;
				break;
		}
		Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Volume Instruction: " + volumeValue);

		// Add a MIDI volume change event into the current tick position.
		try 
		{ 
			ShortMessage volumeEvent = new ShortMessage();
			volumeEvent.setMessage(ShortMessage.CONTROL_CHANGE, 0, 7, midiVolume);
			track.add(new MidiEvent(volumeEvent, curTick));
		} 
		catch (InvalidMidiDataException e) { Mobile.log(Mobile.LOG_ERROR, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Error on volume change event:" + e.getMessage()); }
	}
	
	private static int convertNoteValueToMidi(int noteValue) 
	{
		int baseFrequency = 0; // To hold the base frequency of the note

		// Get the base frequency from the frequency table starting from C1
		switch (noteValue) 
		{
			case 0x0: 
			Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Parsed Pause note. "); 
			return -1; // Pause (no MIDI note)
			case 0x1: baseFrequency = 523; break;// C1
			case 0x2: baseFrequency = 554; break;// C#1 (D1b)
			case 0x3: baseFrequency = 587; break;// D1
			case 0x4: baseFrequency = 622; break;// D#1 (E1b, so on)
			case 0x5: baseFrequency = 659; break;// E1
			case 0x6: baseFrequency = 698; break;// F1
			case 0x7: baseFrequency = 740; break;// F#1
			case 0x8: baseFrequency = 784; break;// G1
			case 0x9: baseFrequency = 831; break;// G#1
			case 0xA: baseFrequency = 880; break;// A1
			case 0xB: baseFrequency = 932; break;// A#1
			case 0xC: baseFrequency = 988; break;// B(or H)1
			default:
			Mobile.log(Mobile.LOG_WARNING, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Parsed Note: " + noteStrings[noteValue] + ". Returning a pause instead."); 
			return -1; // Invalid note, but CaveCab tries to add notes with reserved values. Let's just return a pause instead of causing issues for midi playback.
		}

		/* 
		 * Convert the frequency back to a MIDI note using the current note scale factor. 
		 * 
		 * In short: 
		 * Scale-1 (0.5):           C1 -> C0
		 * Scale-2 (1.0 - default): C1 = C1
		 * Scale-3 (2.0):           C1 -> C2
		 * Scale-4 (4.0):           C1 -> C3
		*/
		int noteFromFreq = convertFreqToNote((int) (baseFrequency * noteScale));

		if(Mobile.minLogLevel == Mobile.LOG_DEBUG) // Let's only spend time with this calculation if we really need to print it for debug
		{
			int octave = (int) Math.floor(Math.log(noteScale) / Math.log(2));
			if(octave < 0) { octave = 0; }

			Mobile.log(Mobile.LOG_DEBUG, NokiaOTTDecoder.class.getPackage().getName() + "." + NokiaOTTDecoder.class.getSimpleName() + ": " + "Parsed Note: " + noteStrings[noteValue] + (octave+1) + " | Converted to Midi:" + noteFromFreq);
		}

		return noteFromFreq;
	}
	
	private static int convertDurationToTicks(int noteDuration, int durationSpecifier) 
	{
		// Base duration in ticks (e.g., Quarter Note = 24 ticks)
		int baseTicks = 24;
		switch (noteDuration) 
		{
			case 0x0: baseTicks *= 4; break; // Full note
			case 0x1: baseTicks *= 2; break; // 1/2 note
			case 0x3: baseTicks /= 2; break; // 1/8 note
			case 0x4: baseTicks /= 4; break; // 1/16 note
			case 0x5: baseTicks /= 8; break; // 1/32 note
			case 0x2:                        // 1/4 note (default)
			default: break;                    // Default to 1/4 if reserved
		}

		// Adjust ticks based on duration specifier
		switch (durationSpecifier) 
		{
			case 0x1: // Dotted note
				baseTicks = baseTicks * 15 / 10; // Increase duration by 50%
				break;
			case 0x2: // Double dotted note
				baseTicks = baseTicks * 175 / 100; // Increase duration by 75%
				break;
			case 0x3: // 2/3 length
				baseTicks = baseTicks * 2 / 3; // Reduce duration to about 2/3
				break;
			case 0x0: // No special duration specifier
			default:   // This case should not happen but just ignore any duration changes if it does
				break;
		}

		return baseTicks;
	}

	private static int readBits(int numBits) 
	{
		int value = 0;
		for (int i = 0; i < numBits; i++) 
		{
			if (parsePos >= data.length) 
			{
				return 0;
			}

			value <<= 1; 
			value |= (data[parsePos] & (1 << (7 - curBitPos))) != 0 ? 1 : 0;
			curBitPos++;

			// If the current bit value is 8, we reached a new byte, wrap to 0
			if (curBitPos == 8)
			{
				curBitPos = 0;
				parsePos++;
			}
		}
		return value;
	}
}