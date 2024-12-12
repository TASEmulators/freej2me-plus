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
package com.nokia.mid.sound;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.ToneControl;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformPlayer;

/* Using references from http://www.j2megame.org/j2meapi/Nokia_UI_API_1_1/com/nokia/mid/sound/Sound.html */
public class Sound
{
	public static final int FORMAT_TONE = 1;
	public static final int FORMAT_WAV = 5;
	public static final int SOUND_PLAYING = 0;
	public static final int SOUND_STOPPED = 1;
	public static final int SOUND_UNINITIALIZED = 3;

	public static final byte TONE_MAX_VOLUME = 127;

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

	private Player player;

	private static int parsePos = 0; // Used exclusively as a marker for OTA/OTT Parsing
	private static boolean[] toneBitArray;
	private static float noteScale = 1f; // Default scale of 880Hz
	private static int noteStyle = NATURAL_STYLE; // The default style is NATURAL
	private static int curTick = 0; // To keep track of the current midi note tick, or else all notes will play at the same time.

	private static boolean isPrevPlayerTone = false;

	// This one is used for debugging.
	private static final String[] noteStrings = new String[] {"Pause", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "H", "Reserved", "Reserved", "Reserved"};


	public Sound(byte[] data, int type) { init(data, type); }
	
	public Sound(int freq, long duration) { init(freq, duration); }

	public static int getConcurrentSoundCount(int type) { return 1; }

	public int getState() 
	{ 
		int state = player.getState();

		switch (state)
		{
			case Player.STARTED:
				return SOUND_PLAYING;
			case Player.PREFETCHED:
			case Player.REALIZED:
				return SOUND_STOPPED;
			case Player.UNREALIZED:
			case Player.CLOSED:
			default:
				return SOUND_UNINITIALIZED;
		}
	}

	public static int[] getSupportedFormats() { return new int[]{FORMAT_TONE, FORMAT_WAV}; }

	public void init(byte[] data, int type) 
	{
		try 
		{
			if (type == FORMAT_TONE) 
			{
				try 
				{
					if(player == null || !isPrevPlayerTone)  // check for null because release() can be called after all.
					{
						if(Manager.dumpAudioStreams) { Manager.dumpAudioStream(new ByteArrayInputStream(data), "audio/x-tone-seq"); } // Dump original OTA as well
						player = Manager.createPlayer(new ByteArrayInputStream(convertToMidi(data)), "audio/x-tone-seq"); 
						isPrevPlayerTone = true; 
					}
					else
					{
						player.deallocate();
						((ToneControl) player.getControl("ToneControl")).setSequence(convertToMidi(data));
					}
					player.prefetch();
				}
				catch (MidiUnavailableException e) { Mobile.log(Mobile.LOG_ERROR, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + " couldn't create Tone player:" + e.getMessage()); }
			}
			else if (type == FORMAT_WAV) 
			{
				if (player != null) { player.close(); }
				String format;
				if(data[0] == 'M' && data[1] == 'T' && data[2] == 'h' && data[3] == 'd') { format = "audio/mid"; }
				else if(data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F') { format = "audio/wav"; }
				else { Mobile.log(Mobile.LOG_WARNING, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + " couldn't find what format this is. Passing as FORMAT_WAV."); format = "audio/wav";}

				player = Manager.createPlayer(new ByteArrayInputStream(data), format);
				player.realize();
				player.prefetch();
				isPrevPlayerTone = false;
			}
			else { throw new IllegalArgumentException("Nokia Sound: Invalid audio format: " + type); }
		}
		catch (MediaException exception) { } catch (IOException exception) { }
	}

	/* 
	 * Haven't found a jar using this yet, but forcing it through the one above does indicate that it works even if incorrectly
	 * Also, based on the j2megame source, this is just javax.microedition.media.Manager.playTone() on MIDP 2.0
	 */
	public void init(int freq, long duration) 
	{ 
		Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Nokia Sound: Single Note:" + freq);

		try { Manager.playTone(convertFreqToNote(freq), (int) duration, TONE_MAX_VOLUME);  }
		catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Nokia Sound: Could not play tone:" + e.getMessage()); }
	}

	public void play(int loop) 
	{
		if(getState() == SOUND_PLAYING) { player.stop(); }
		if(getState() == SOUND_UNINITIALIZED) { return; }
		if(loop < 0) { throw new IllegalArgumentException("Cannot play media, invalid loop value received"); }
		else if(loop == 0) { loop = -1; }

		player.setLoopCount(loop);
		player.setMediaTime(0); // A play call always makes the media play from the beginning.
		player.start();
	}

	public void release() { player.close(); }

	public void resume() 
	{
		if(getState() == SOUND_UNINITIALIZED || getState() == SOUND_PLAYING) { return; }
		player.start(); 
	}

	public void setGain(int gain) 
	{ 
		// Gain goes from 0 to 255, while setLevel works from 0 to 100
		((PlatformPlayer.volumeControl)player.getControl("VolumeControl")).setLevel((int) (gain / 255 * 100));
	}

	public int getGain() 
	{ 
		return (int) ((((PlatformPlayer.volumeControl)player.getControl("VolumeControl")).getLevel() / 100) * 255);
	}

	public void setSoundListener(SoundListener soundListener) { ((PlatformPlayer) player).setSoundListener(this, soundListener); }

	public void stop() { player.stop(); }

	/* From here on out, will be only methods to decode Nokia's OTT/OTA format into MIDI. */

	// This is the same conversion used in Sprintpcs' DualTone implementation., as it also uses this constant.
	public static int convertFreqToNote(int freq) { return (int) (Math.round(Math.log((double) freq / 8.176) * SEMITONE_CONST)); }

	public static byte[] convertToMidi(byte[] data) throws MidiUnavailableException, IOException  // Start by parsing the OTT Header
	{
		try 
		{
			parsePos = 0; // Reset the parsePos counter
			noteScale = 1f; // Reset scale as well
			noteStyle = NATURAL_STYLE; // Reset note style too
			curTick = 0; // Also move curTick to the beginning
			toneBitArray = new boolean[data.length * 8]; 

			// Convert the byte array into a bit array for much easier manipulation and reading
			for (int i = 0; i < data.length; i++)
			{
				for (int j = 0; j < 8; j++) 
				{
					toneBitArray[i * 8 + j] = (data[i] & (1 << (7 - j))) != 0;
				}
			}

			// Create a new sequence and track for the converted tone
			Sequence sequence = new Sequence(Sequence.PPQ, 24);
			Track track = sequence.createTrack();
			track.add(new MidiEvent(new ShortMessage(ShortMessage.PROGRAM_CHANGE, 0, 80, 0), 0)); // 80 is the Square Wave / Lead 1 instrument, which we'll use to get closer to what this should sound like
		
			// Validate command length
			int commandLength = readBits(8); // Command Length is 8 bits, so get them from the bit array.
			Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Command length: " + commandLength);
		
			for (int i = 0; i < commandLength; i++) 
			{
				if(toneBitArray.length - parsePos - 8 <= 0) { Mobile.log(Mobile.LOG_WARNING, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "OTT tried to read beyond bounds. Returning stream early. "); break; }
				int commandType = readBits(8); // Check command type (first 7 bits + filler bit which is always 0)
		
				switch (commandType) {
					case 0b01001010: // Ringing tone programming
						Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Ringing tone programming detected.");
						parseRingingTone(track);
						break;
					case 0b01000100: // Unicode (not handled yet, and should have nothing appended into the media track)
						Mobile.log(Mobile.LOG_WARNING, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Unicode detected.");
						parseUnicode();
						break;
					case 0b00111010: // Sound
						Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Sound detected.");
						parseSound(track);
						break;
					case 0b00001010: // Cancel command, Does any actual OTT/OTA ringtone use this?
						Mobile.log(Mobile.LOG_WARNING, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Cancel command detected.");
						break;
					case 0b00000000: // This should happen at the end of every parsing procedure.
						Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "End of ringtone programming!");
						break;
					default: // If this is the case, we can't parse the header, so just return outright
						Mobile.log(Mobile.LOG_ERROR, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Unknown command type: " + Integer.toBinaryString(commandType));
						break;
				}
			}
		
			// Convert the resulting sequence to byte array and send to the player.
			try 
			{
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				MidiSystem.write(sequence, 1, output);
				return output.toByteArray();
			}
			catch (IOException e) { Mobile.log(Mobile.LOG_ERROR, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + " couldn't write converted Tone Sequence:" + e.getMessage()); return null;}
		} 
		catch(InvalidMidiDataException e) { Mobile.log(Mobile.LOG_ERROR, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + " couldn't convert Tone Sequence:" + e.getMessage()); return null;}
	}

	private static void parseRingingTone(Track track) 
	{
		/* 
		 * If we found a <ringing-tone-programming> string, that means that up next
		 * it's either a <unicode> or a <sound> bit string
		 */
		int nextCheck = readBits(7);
		
		if(nextCheck == 0b0011101) 
		{ 
			Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Detected Sound!" );
			parseSound(track);
		} 
		else if(nextCheck == 0b0100010) 
		{
			// Ideally, at this point this check should resolve to a <cancel-command-specifier>
			Mobile.log(Mobile.LOG_WARNING, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Detected Unicode!" );
			parseUnicode();
		}
	}

	// Let's just ignore unicode decoding at all for now, this shouldn't be part of a ringtone
	private static void parseUnicode() { }

	private static void parseSound(Track track) 
	{
		// Read song type
		int songType = readBits(3); // 3 bits are used to represent the song type

		switch (songType) 
		{
			case 0b001: // Basic song type
				Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Basic Song Detected!");
				parseBasicSong(track);
				break;
			case 0b010: // Temporary song type
				Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Temporary Song Detected!");
				parseTemporarySong(track);
				break;
			case 0b011: // MIDI song type
				Mobile.log(Mobile.LOG_WARNING, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "MIDI Song Detected!");
				parseMidiSong(track);
				break;
			case 0b100: // Digitized song type
				Mobile.log(Mobile.LOG_WARNING, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Digitized Song Detected!");
				parseDigitizedSong(track);
				break;
			case 0b101: // Polyphonic song type
				Mobile.log(Mobile.LOG_WARNING, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Polyphonic Song Detected!");
				parsePolyphonicSong(track);
				break;
			default:
				Mobile.log(Mobile.LOG_ERROR, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Unknown song type: " + Integer.toBinaryString(songType));
				break;
		}
	}

	private static void parseBasicSong(Track track) 
	{
		// Read title length
		int titleLength = readBits(4); // Upper 4 bits
	
		StringBuilder title = new StringBuilder();
		for (int i = 0; i < titleLength; i++) 
		{
			char character = (char) readBits(8);
			title.append(character);
		}
		Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Title Length:" + titleLength + " | Basic Song Title: " + title.toString());
		
		// Read song sequence length
		int songSequenceLength = readBits(8); // Read the number of patterns
		Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Basic Song Sequence Length: " + songSequenceLength);
	
		// Parse each song pattern
		for (int i = 0; i < songSequenceLength; i++) { parseSongPattern(track); }
	}
	
	// Implement similar methods for parseTemporarySong, parseMidiSong, parseDigitizedSong, and parsePolyphonicSong
	private static void parseTemporarySong(Track track) 
	{
		// Read song sequence length
		int songSequenceLength = readBits(8); // Read the number of patterns
		Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Song Sequence Length: " + songSequenceLength);
	
		// Parse each song pattern
		for (int i = 0; i < songSequenceLength; i++) { parseSongPattern(track); }
	}
	
	private static void parseMidiSong(Track track) { /* MIDI song parsing logic, Stubbed */ }
	
	private static void parseDigitizedSong(Track track) { /* Digitized song parsing logic, Stubbed */ }
	
	private static void parsePolyphonicSong(Track track) { /* Polyphonic song parsing logic, Stubbed */ }
	
	private static void parseSongPattern(Track track) 
	{
		// Read the pattern header
		int patternHeader = readBits(3); // 3 bits for Pattern Header's beginning
		int patternId = readBits(2); // 2 bits for pattern ID
		int loopValue = readBits(4); // 4 bits for loop value

		Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Pattern Header - ID: " + patternHeader + ", Pattern ID: " + patternId + ", Loop Value: " + loopValue);
	
		if(loopValue == 0b1111) { Mobile.log(Mobile.LOG_WARNING, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "OTA/OTT Tone Infinite Loop parsing is not implemented. Parsing pattern without loop..."); loopValue = 0; }

		int loopParsePosMark = parsePos; // Marker for the current pattern start position, as we'll re-read it as many times as there are loops, to simulate looping parts of a track on MIDI.

		while(loopValue >= 0) // LoopValue == 0 still means the pattern has to be entirely parsed at least one time.
		{
			parsePos = loopParsePosMark;

			// Read the pattern specifier
			int patternSpecifier = readBits(8);
			if (patternSpecifier == 0b00000000) { Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Using already-defined pattern."); } 
			else 
			{
				// This means we have a new pattern length
				Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "New pattern length: " + patternSpecifier);
				int numberOfInstructions = patternSpecifier; // The number of instructions to read
		
				// Reset note Style and Scale, otherwise it'll carry over from the last pattern (which is incorrect despite the Smart Message API not disclosing it)
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

		Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "instructionType: " + instructionType);

		switch (instructionType) 
		{
			case 0b000: // Pattern Header ID
				Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "New pattern found. Backtracking to read it.");
				parsePos -= 3; // Parse Song Pattern will parse the pattern from the beginning, which also includes the 3 bits just read 
				return;
			case 0b001: // Note Instruction
				parseNoteInstruction(track);
				break;
			case 0b010: // Scale Instruction
				parseScaleInstruction();
				break;
			case 0b011: // Style Instruction
				parseStyleInstruction();
				break;
			case 0b100: // Tempo Instruction
				parseTempoInstruction(track);
				break;
			case 0b101: // Volume Instruction
				parseVolumeInstruction(track);
				break;
			default:
				Mobile.log(Mobile.LOG_ERROR, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Unknown instruction type: " + Integer.toBinaryString(instructionType));
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

		Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "noteDuration: " + noteDuration + "| durationSpecifier: " + durationSpecifier);
	
		// Create MIDI events for the note, accounting for the current Note Style.
		try 
		{
			if(noteStyle == STACCATO_STYLE) // Simulate shorter notes for a subtle staccato effect by making NOTE_OFF end before the next note's NOTE_ON
			{
				track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, 0, midiNote, 93), curTick)); // NOTE_ON
				track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, midiNote, 0), curTick + (int) (ticks * 0.70f) )); // NOTE_OFF
			}
			else if (noteStyle == CONTINUOUS_STYLE) // Try to add a small overlap between notes to connect them a bit better, making NOTE_OFF go a bit beyond the next note's NOTE_ON
			{
				track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, 0, midiNote, 93), curTick)); // NOTE_ON
				track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, midiNote, 0), curTick+ (int) (ticks * 1.1f) )); // NOTE_OFF
			}
			else // NATURAL just adds notes as is.
			{
				track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, 0, midiNote, 93), curTick)); // NOTE_ON
				track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, midiNote, 0), curTick+ticks)); // NOTE_OFF
			}
			
			curTick += ticks;
		}
		catch (InvalidMidiDataException e) { Mobile.log(Mobile.LOG_ERROR, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Couldn't parse note instruction:" + e.getMessage()); }
	}

	private static void parseScaleInstruction() 
	{
		int scaleValue = readBits(2); // 2 bits are used for scale value

		switch (scaleValue) 
		{
			case 0b00:
				Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Scale-1: A = 440 Hz");
				noteScale = 0.5f;
				break;
			case 0b01:
				Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Scale-2: A = 880 Hz (default)");
				noteScale = 1f;
				break;
			case 0b10:
				Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Scale-3: A = 1.76 kHz");
				noteScale = 2f;
				break;
			case 0b11:
				Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Scale-4: A = 3.52 kHz");
				noteScale = 4f;
				break;
			default:
				Mobile.log(Mobile.LOG_ERROR, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Unknown scale value");
				break;
		}
	}
	
	private static void parseStyleInstruction() 
	{
		int styleValue = readBits(2); // 2 bits for style value

		switch (styleValue) 
		{
			case 0b00:
				Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Natural Style (rest between notes)");
				noteStyle = NATURAL_STYLE;
				break;
			case 0b01:
				Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Continuous Style (no rest between notes)");
				noteStyle = CONTINUOUS_STYLE;
				break;
			case 0b10:
				Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Staccato Style (shorter notes)");
				noteStyle = STACCATO_STYLE;
				break;
			case 0b11:
				Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "RESERVED");
				break;
			default:
				Mobile.log(Mobile.LOG_ERROR, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Unknown style value");
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
			case 0b00000: bpm = 25; break;
			case 0b00001: bpm = 28; break;
			case 0b00010: bpm = 31; break;
			case 0b00011: bpm = 35; break;
			case 0b00100: bpm = 40; break;
			case 0b00101: bpm = 45; break;
			case 0b00110: bpm = 50; break;
			case 0b00111: bpm = 56; break;
			case 0b01000: bpm = 63; break;
			case 0b01001: bpm = 70; break;
			case 0b01010: bpm = 80; break;
			case 0b01011: bpm = 90; break;
			case 0b01100: bpm = 100; break;
			case 0b01101: bpm = 112; break;
			case 0b01110: bpm = 125; break;
			case 0b01111: bpm = 140; break;
			case 0b10000: bpm = 160; break;
			case 0b10001: bpm = 180; break;
			case 0b10010: bpm = 200; break;
			case 0b10011: bpm = 225; break;
			case 0b10100: bpm = 250; break;
			case 0b10101: bpm = 285; break;
			case 0b10110: bpm = 320; break;
			case 0b10111: bpm = 355; break;
			case 0b11000: bpm = 400; break;
			case 0b11001: bpm = 450; break;
			case 0b11010: bpm = 500; break;
			case 0b11011: bpm = 565; break;
			case 0b11100: bpm = 635; break;
			case 0b11101: bpm = 715; break;
			case 0b11110: bpm = 800; break;
			case 0b11111: bpm = 900; break;
			default: Mobile.log(Mobile.LOG_ERROR, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Unknown BPM value");
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
			Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Tempo Instruction - BPM: " + bpm);
		}
		catch (InvalidMidiDataException e) { Mobile.log(Mobile.LOG_ERROR, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Error adding BPM event:" + e.getMessage()); }
	}
	
	private static void parseVolumeInstruction(Track track) 
	{
		int volumeValue = readBits(4); // 4 bits for volume level
		int midiVolume = 0; // Initialize MIDI volume

		// Approximately map the parsed volume value range (0-15) to the usual MIDI range (0-127)
		switch (volumeValue) 
		{
			case 0b0000: // tone-off
				midiVolume = 0;
				break;
			case 0b0001:
				midiVolume = 48;
				break;
			case 0b0010:
				midiVolume = 56;
				break;
			case 0b0011:
				midiVolume = 64;
				break;
			case 0b0100:
				midiVolume = 72;
				break;
			case 0b0101:
				midiVolume = 80;
				break;
			case 0b0110:
				midiVolume = 88;
				break;
			case 0b0111: // This is the default volume level (7)
				midiVolume = 92;
				break;
			case 0b1000:
				midiVolume = 100;
				break;
			case 0b1001:
				midiVolume = 104;
				break;
			case 0b1010:
				midiVolume = 108;
				break;
			case 0b1011:
				midiVolume = 112;
				break;
			case 0b1100:
				midiVolume = 116;
				break;
			case 0b1101:
				midiVolume = 120;
				break;
			case 0b1110:
				midiVolume = 124;
				break;
			case 0b1111:
			default:
				midiVolume = 127;
				break;
		}
		Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Volume Instruction: " + volumeValue);

		// Add a MIDI volume change event into the current tick position.
		try { track.add(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 7, midiVolume), curTick)); } 
		catch (InvalidMidiDataException e) { Mobile.log(Mobile.LOG_ERROR, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Error on volume change event:" + e.getMessage()); }
	}
	
	private static int convertNoteValueToMidi(int noteValue) 
	{
		int baseFrequency = 0; // To hold the base frequency of the note

		// Get the base frequency from the frequency table starting from C1
		switch (noteValue) 
		{
			case 0b0000: return 0; // Pause (no MIDI note)
			case 0b0001: baseFrequency = 523; break;// C1
			case 0b0010: baseFrequency = 554; break;// C#1 (D1b)
			case 0b0011: baseFrequency = 587; break;// D1
			case 0b0100: baseFrequency = 622; break;// D#1 (E1b, so on)
			case 0b0101: baseFrequency = 659; break;// E1
			case 0b0110: baseFrequency = 698; break;// F1
			case 0b0111: baseFrequency = 740; break;// F#1
			case 0b1000: baseFrequency = 784; break;// G1
			case 0b1001: baseFrequency = 831; break;// G#1
			case 0b1010: baseFrequency = 880; break;// A1
			case 0b1011: baseFrequency = 932; break;// A#1
			case 0b1100: baseFrequency = 988; break;// B(or H)1
			default:
			Mobile.log(Mobile.LOG_WARNING, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Parsed Note: " + noteStrings[noteValue] + ". Returning a pause instead."); 
			return 0; // Invalid note, but CaveCab tries to add notes with reserved values. Let's just return a pause instead of causing issues for midi playback.
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

			Mobile.log(Mobile.LOG_DEBUG, Sound.class.getPackage().getName() + "." + Sound.class.getSimpleName() + ": " + "Parsed Note: " + noteStrings[noteValue] + octave + " | Converted to Midi:" + noteFromFreq);
		}

		return noteFromFreq;
	}
	
	private static int convertDurationToTicks(int noteDuration, int durationSpecifier) 
	{
		// Base duration in ticks (e.g., Quarter Note = 24 ticks)
		int baseTicks = 24;
		switch (noteDuration) 
		{
			case 0b000: baseTicks *= 4; break; // Full note
			case 0b001: baseTicks *= 2; break; // 1/2 note
			case 0b011: baseTicks /= 2; break; // 1/8 note
			case 0b100: baseTicks /= 4; break; // 1/16 note
			case 0b101: baseTicks /= 8; break; // 1/32 note
			case 0b010:                        // 1/4 note (default)
			default: break;                    // Default to 1/4 if reserved
		}

		// Adjust ticks based on duration specifier
		switch (durationSpecifier) 
		{
			case 0b01: // Dotted note
				baseTicks = (int) (baseTicks * 1.5); // Increase duration by 50%
				break;
			case 0b10: // Double dotted note
				baseTicks = (int) (baseTicks * 1.75); // Increase duration by 75%
				break;
			case 0b11: // 2/3 length
				baseTicks = (int) (baseTicks * (2.0 / 3.0)); // Reduce duration to about 2/3
				break;
			case 0b00: // No special duration specifier
			default:   // This case should not happen but just ignore any duration changes if it does
				break;
		}

		return baseTicks;
	}

	// Helper function to read a given number of bits from the bitArray. 
	private static int readBits(int numBits)
	{
		int value = 0;
		for (int i = 0; i < numBits; i++) 
		{
			value <<= 1;
			value |= toneBitArray[parsePos++] ? 1 : 0; // Increment the current parser position by the number of bits read
		}
		return value;
	}
}
