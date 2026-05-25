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

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import org.recompile.mobile.Mobile;

public final class EMSiMelodyDecoder
{

    private static final String[] styleString = 
    {
        "S0 (Natural Style)",
        "S1 (Continuous Style)",
        "S2 (Staccato Style)"
    };

    private static final int PPQ = 24;

    private static int volume;
    private static int tempo;
    private static int style;
    private static int curTick;

    private static int decodePos;
    private static byte[] input;

    private static Sequence sequence;
    private static Track track;
    private static List<MidiEvent> currentEvents = new ArrayList<MidiEvent>(); // Store current events (for block repeats)
    public static Map<Integer, Integer> vibePositions = new HashMap<Integer, Integer>();
    public static Map<Integer, Integer> ledPositions = new HashMap<Integer, Integer>();
    public static Map<Integer, Integer> backlightPositions = new HashMap<Integer, Integer>();

    public static final InputStream decodeiMelody(byte[] inputData) 
    {

        decodePos = 0;
        input = inputData;

        curTick = 0;
        style = 0;      // Default style is natural style
        volume = 7 * 8; // Default volume is 7 if not specified (but we convert it to MIDI)
        tempo = 120;    // Default tempo is 120 if not defined
        currentEvents.clear();
        vibePositions.clear();
        ledPositions.clear();
        backlightPositions.clear();

        String headerStart, version, format, name = null, composer = null, copyright = null;
        StringBuilder nextString = new StringBuilder();

        // File start
        while(!(Character.toUpperCase((char) input[decodePos]) == 'V' && Character.toUpperCase((char) input[decodePos+1]) == 'E')) { nextString.append((char) (input[decodePos++] & 0xFF)); }
        headerStart = nextString.toString().replaceAll("[\\r\\n ]", "");
        nextString.delete(0, nextString.capacity());

        // Version
        while(!(Character.toUpperCase((char) input[decodePos]) == 'F' && Character.toUpperCase((char) input[decodePos+1]) == 'O')) { nextString.append((char) (input[decodePos++] & 0xFF)); }
        version = nextString.toString().replaceAll("[\\r\\n ]", "");
        nextString.delete(0, nextString.capacity());

        // Format
        while (!((Character.toUpperCase((char) input[decodePos]) == 'N' && Character.toUpperCase((char) input[decodePos + 1]) == 'A' && Character.toUpperCase((char) input[decodePos + 4]) == ':') ||
                (Character.toUpperCase((char) input[decodePos]) == 'C' && Character.toUpperCase((char) input[decodePos + 1]) == 'O' && Character.toUpperCase((char) input[decodePos + 8]) == ':') ||
                (Character.toUpperCase((char) input[decodePos]) == 'C' && Character.toUpperCase((char) input[decodePos + 1]) == 'O' && Character.toUpperCase((char) input[decodePos + 9]) == ':') ||
                (Character.toUpperCase((char) input[decodePos]) == 'B' && Character.toUpperCase((char) input[decodePos + 1]) == 'E' && Character.toUpperCase((char) input[decodePos + 4]) == ':') ||
                (Character.toUpperCase((char) input[decodePos]) == 'S' && Character.toUpperCase((char) input[decodePos + 1]) == 'T' && Character.toUpperCase((char) input[decodePos + 5]) == ':') ||
                (Character.toUpperCase((char) input[decodePos]) == 'V' && Character.toUpperCase((char) input[decodePos + 1]) == 'O' && Character.toUpperCase((char) input[decodePos + 6]) == ':') ||
                (Character.toUpperCase((char) input[decodePos]) == 'M' && Character.toUpperCase((char) input[decodePos + 1]) == 'E' && Character.toUpperCase((char) input[decodePos + 6]) == ':'))) 
        {
            nextString.append((char) (input[decodePos++] & 0xFF));
        }
        format = nextString.toString().replaceAll("[\\r\\n ]", "");
        nextString.setLength(0);

        // Get any optional data fields before we reach the melody field (the left side of each is case insensitive, and fields are stacked, as in, the Beat field never comes before the name field when both are present)
        while (!(Character.toUpperCase((char) input[decodePos]) == 'M' && Character.toUpperCase((char) input[decodePos + 1]) == 'E' && Character.toUpperCase((char) input[decodePos + 6]) == ':')) 
        {
            if (Character.toUpperCase((char) input[decodePos]) == 'N' && Character.toUpperCase((char) input[decodePos + 1]) == 'A' && Character.toUpperCase((char) input[decodePos + 4]) == ':') // Name field
            { 
                while (!(Character.toUpperCase((char) input[decodePos]) == 'C' && Character.toUpperCase((char) input[decodePos + 1]) == 'O' && Character.toUpperCase((char) input[decodePos + 9]) == ':') &&
                    !(Character.toUpperCase((char) input[decodePos]) == 'C' && Character.toUpperCase((char) input[decodePos + 1]) == 'O' && Character.toUpperCase((char) input[decodePos + 8]) == ':') &&
                    !(Character.toUpperCase((char) input[decodePos]) == 'B' && Character.toUpperCase((char) input[decodePos + 1]) == 'E' && Character.toUpperCase((char) input[decodePos + 4]) == ':') &&
                    !(Character.toUpperCase((char) input[decodePos]) == 'S' && Character.toUpperCase((char) input[decodePos + 1]) == 'T' && Character.toUpperCase((char) input[decodePos + 5]) == ':') &&
                    !(Character.toUpperCase((char) input[decodePos]) == 'V' && Character.toUpperCase((char) input[decodePos + 1]) == 'O' && Character.toUpperCase((char) input[decodePos + 6]) == ':') &&
                    !(Character.toUpperCase((char) input[decodePos]) == 'M' && Character.toUpperCase((char) input[decodePos + 1]) == 'E' && Character.toUpperCase((char) input[decodePos + 6]) == ':')) 
                {
                    nextString.append((char) (input[decodePos++] & 0xFF));
                }
                name = nextString.toString().replaceAll("[\\r\\n ]", "");
            } 
            else if (Character.toUpperCase((char) input[decodePos]) == 'C' && Character.toUpperCase((char) input[decodePos + 1]) == 'O' && Character.toUpperCase((char) input[decodePos + 8]) == ':') // Composer field
            { 
                while (!(Character.toUpperCase((char) input[decodePos]) == 'C' && Character.toUpperCase((char) input[decodePos + 1]) == 'O' && Character.toUpperCase((char) input[decodePos + 9]) == ':') &&
                    !(Character.toUpperCase((char) input[decodePos]) == 'B' && Character.toUpperCase((char) input[decodePos + 1]) == 'E' && Character.toUpperCase((char) input[decodePos + 4]) == ':') &&
                    !(Character.toUpperCase((char) input[decodePos]) == 'S' && Character.toUpperCase((char) input[decodePos + 1]) == 'T' && Character.toUpperCase((char) input[decodePos + 5]) == ':') &&
                    !(Character.toUpperCase((char) input[decodePos]) == 'V' && Character.toUpperCase((char) input[decodePos + 1]) == 'O' && Character.toUpperCase((char) input[decodePos + 6]) == ':') &&
                    !(Character.toUpperCase((char) input[decodePos]) == 'M' && Character.toUpperCase((char) input[decodePos + 1]) == 'E' && Character.toUpperCase((char) input[decodePos + 6]) == ':')) 
                {
                    nextString.append((char) (input[decodePos++] & 0xFF));
                }
                composer = nextString.toString().replaceAll("[\\r\\n ]", "");
            } 
            else if (Character.toUpperCase((char) input[decodePos]) == 'B' && Character.toUpperCase((char) input[decodePos + 1]) == 'E' && Character.toUpperCase((char) input[decodePos + 4]) == ':') // Beat field
            { 
                while (!(Character.toUpperCase((char) input[decodePos]) == 'C' && Character.toUpperCase((char) input[decodePos + 1]) == 'O' && Character.toUpperCase((char) input[decodePos + 9]) == ':') &&
                    !(Character.toUpperCase((char) input[decodePos]) == 'S' && Character.toUpperCase((char) input[decodePos + 1]) == 'T' && Character.toUpperCase((char) input[decodePos + 5]) == ':') &&
                    !(Character.toUpperCase((char) input[decodePos]) == 'V' && Character.toUpperCase((char) input[decodePos + 1]) == 'O' && Character.toUpperCase((char) input[decodePos + 6]) == ':') &&
                    !(Character.toUpperCase((char) input[decodePos]) == 'M' && Character.toUpperCase((char) input[decodePos + 1]) == 'E' && Character.toUpperCase((char) input[decodePos + 6]) == ':')) 
                {
                    nextString.append((char) (input[decodePos++] & 0xFF));
                }
                tempo = Integer.parseInt(nextString.toString().replaceAll("[\\r\\n ]", "").split(":")[1]);
            } 
            else if (Character.toUpperCase((char) input[decodePos]) == 'S' && Character.toUpperCase((char) input[decodePos + 1]) == 'T' && Character.toUpperCase((char) input[decodePos + 5]) == ':') // Style field
            { 
                while (!(Character.toUpperCase((char) input[decodePos]) == 'C' && Character.toUpperCase((char) input[decodePos + 1]) == 'O' && Character.toUpperCase((char) input[decodePos + 9]) == ':') &&
                    !(Character.toUpperCase((char) input[decodePos]) == 'V' && Character.toUpperCase((char) input[decodePos + 1]) == 'O' && Character.toUpperCase((char) input[decodePos + 6]) == ':') &&
                    !(Character.toUpperCase((char) input[decodePos]) == 'M' && Character.toUpperCase((char) input[decodePos + 1]) == 'E' && Character.toUpperCase((char) input[decodePos + 6]) == ':')) 
                {
                    nextString.append((char) (input[decodePos++] & 0xFF));
                }
                style = Integer.parseInt(nextString.toString().replaceAll("[\\r\\n S]", "").split(":")[1]);
            } 
            else if (Character.toUpperCase((char) input[decodePos]) == 'V' && Character.toUpperCase((char) input[decodePos + 1]) == 'O' && Character.toUpperCase((char) input[decodePos + 6]) == ':') // Volume field
            { 
                while (!(Character.toUpperCase((char) input[decodePos]) == 'C' && Character.toUpperCase((char) input[decodePos + 1]) == 'O' && Character.toUpperCase((char) input[decodePos + 9]) == ':') &&
                    !(Character.toUpperCase((char) input[decodePos]) == 'M' && Character.toUpperCase((char) input[decodePos + 1]) == 'E' && Character.toUpperCase((char) input[decodePos + 6]) == ':')) 
                {
                    nextString.append((char) (input[decodePos++] & 0xFF));
                }
                volume = Integer.parseInt(nextString.toString().replaceAll("[\\r\\n V]", "").split(":")[1]) * 8;
            } 
            else if (Character.toUpperCase((char) input[decodePos]) == 'C' && Character.toUpperCase((char) input[decodePos + 1]) == 'O' && Character.toUpperCase((char) input[decodePos + 9]) == ':') // Copyright field
            {
                while (!(Character.toUpperCase((char) input[decodePos]) == 'M' && Character.toUpperCase((char) input[decodePos + 1]) == 'E' && Character.toUpperCase((char) input[decodePos + 6]) == ':')) 
                {
                    nextString.append((char) (input[decodePos++] & 0xFF));
                }
                copyright = nextString.toString().replaceAll("[\\r\\n ]", "");;
            }

            nextString.setLength(0);
        }

        Mobile.log(Mobile.LOG_DEBUG, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ": " + "----------------EMS Header----------------");
        Mobile.log(Mobile.LOG_DEBUG, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ":     " + headerStart);
        Mobile.log(Mobile.LOG_DEBUG, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ":     " + version);
        Mobile.log(Mobile.LOG_DEBUG, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ":     " + format);
        
        if(name != null)      { Mobile.log(Mobile.LOG_DEBUG, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ":     " + name); }
        if(composer != null)  { Mobile.log(Mobile.LOG_DEBUG, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ":     " + composer); }
        Mobile.log(Mobile.LOG_DEBUG, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ":     BEAT:" + tempo);
        Mobile.log(Mobile.LOG_DEBUG, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ":     " + styleString[style]);
        Mobile.log(Mobile.LOG_DEBUG, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ":     VOLUME:" + volume);
        if(copyright != null) { Mobile.log(Mobile.LOG_DEBUG, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ":     " + copyright); }
        
        return decodeMelodyData();
    }

    public static final InputStream decodeMelodyData() 
    {
        try 
        {
            sequence = new Sequence(Sequence.PPQ, PPQ);
            track = sequence.createTrack();
            StringBuilder nextString = new StringBuilder();

            Mobile.log(Mobile.LOG_DEBUG, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ": " + "-----------------EMS Decoding-----------------");

            // Set up tempo, volume and instrument beforehand

            int microsecondsPerQuarterNote = 60000000 / tempo;
            MetaMessage tempoEvent = new MetaMessage();
            tempoEvent.setMessage(0x51, new byte[]
            {
                (byte) (microsecondsPerQuarterNote >> 16),
                (byte) (microsecondsPerQuarterNote >> 8),
                (byte) (microsecondsPerQuarterNote)
            }, 3);
            
            track.add(new MidiEvent(tempoEvent, 0));

            ShortMessage volumeEvent = new ShortMessage();
            volumeEvent.setMessage(ShortMessage.CONTROL_CHANGE, 0, 7, volume);
            track.add(new MidiEvent(volumeEvent, 0));

            ShortMessage bankMSB = new ShortMessage();
            ShortMessage bankLSB = new ShortMessage();
            ShortMessage programChange = new ShortMessage();

            bankMSB.setMessage(ShortMessage.CONTROL_CHANGE, 0, 0, 1); // Bank change MSB (Bank 1)
            bankLSB.setMessage(ShortMessage.CONTROL_CHANGE, 0, 32, 0); // Bank change LSB
            programChange.setMessage(ShortMessage.PROGRAM_CHANGE, 0, 80, 0); // 80 is the Square Wave / Lead 1 instrument, which we'll use to get closer to what this should sound like

            track.add(new MidiEvent(bankMSB, 0));
            track.add(new MidiEvent(bankLSB, 1));
            track.add(new MidiEvent(programChange, 0));

            // Parse the melody data itself
            while(!((char) input[decodePos] == 'E' && (char) input[decodePos+1] == 'N' && (char) input[decodePos+2] == 'D')) 
                { nextString.append((char) (input[decodePos++] & 0xFF)); }

            String melodyString = nextString.toString().replaceAll("[\\r\\n ()MELODY:]", "");
            
            nextString.setLength(0);

            decodeBlock(melodyString, track);

            while(decodePos < input.length) { nextString.append((char) (input[decodePos++] & 0xFF)); }
            String melodyEndString = nextString.toString().replaceAll("[\\r\\n ]", "");

            // This should always match END:MELODY
            Mobile.log(Mobile.LOG_DEBUG, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ": " + "REACHED " + melodyEndString);

            // Everything's finished, send the converted stream to the player
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MidiSystem.write(sequence, 0, output);

            return new ByteArrayInputStream(output.toByteArray());
        }
        catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ": " + "Failed to decode EMS iMelody:" + e.getMessage()); e.printStackTrace(); }
        return null;
    }

    private static void decodeBlock(String melodyString, Track track) throws InvalidMidiDataException
    {
        int octave = 4;
        int noteDuration = 0;
        int noteModifier = 0; // Note modifier for flat and sharp notes.
        int noteValue = 0;

        for (int i = 0; i < melodyString.length(); i++) 
        {
            char currentChar = melodyString.charAt(i);            

            // Handle note volume/velocity modifier
            if (currentChar == 'V') 
            {
                if (i + 1 < melodyString.length() && melodyString.charAt(i + 1) == '+') // V+ increases volume by a step
                {
                    volume = Math.min(volume + 8, 127);
                    Mobile.log(Mobile.LOG_DEBUG, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ": " + "Volume changed to:" + volume);
                    i++;
                } 
                else if (i + 1 < melodyString.length() && melodyString.charAt(i + 1) == '-') 
                {
                    volume = Math.max(volume - 8, 0);
                    Mobile.log(Mobile.LOG_DEBUG, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ": " + "Volume changed to:" + volume);
                    i++;
                }
                continue;
            }

            // Silence event (appears to not be affected by Style)
            if (currentChar == 'r') 
            {
                noteDuration = Character.getNumericValue(melodyString.charAt(++i));

                noteDuration = getDurationInTicks(noteDuration);

                if(melodyString.charAt(i+1) == '.') { noteDuration *= 1.5; i++; } // dotted note
                else if(melodyString.charAt(i+1) == ';') { noteDuration *= 1.75; i++; } // double dotted note
                else if(melodyString.charAt(i+1) == ':') { noteDuration = (int) Math.round(noteDuration * (2.0 / 3.0)); i++; } // 2/3 length note

                Mobile.log(Mobile.LOG_DEBUG, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ": " + "Adding rest event from time " + curTick + " to " + (curTick+noteDuration));

                ShortMessage noteOn = new ShortMessage();
                ShortMessage noteOff = new ShortMessage();

                noteOn.setMessage(ShortMessage.NOTE_ON, 0, noteValue, 0);
                track.add(new MidiEvent(noteOn, curTick));
                
                noteOff.setMessage(ShortMessage.NOTE_OFF, 0, 0, 0);
                track.add(new MidiEvent(noteOff, curTick + noteDuration));
                
                currentEvents.add(new MidiEvent(noteOn, curTick));
                currentEvents.add(new MidiEvent(noteOff, curTick + noteDuration));

                curTick += noteDuration;

                continue;
            }

            // Flat note -> Decrease next Midi note value by 1
            if(currentChar == '&') 
            { 
                Mobile.log(Mobile.LOG_DEBUG, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ": " + "Flat note modifier parsed");
                noteModifier = -1; 
                continue; 
            }

            // Sharp note -> Increase next Midi note value by 1
            if(currentChar == '#') 
            { 
                Mobile.log(Mobile.LOG_DEBUG, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ": " + "Sharp note modifier parsed");
                noteModifier = +1; 
                continue; 
            }

            // Note octave change
            if (currentChar == '*') 
            {
                octave = Character.getNumericValue(melodyString.charAt(++i));
                Mobile.log(Mobile.LOG_DEBUG, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ": " + "Octave changed to:" + octave);
                continue;
            }

            // TODO: Handle those other events like vibration, led and backlight, for now, we just skip them
            if (melodyString.startsWith("ledon", i)) 
            {
                Mobile.log(Mobile.LOG_WARNING, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ": " + "LED On event not implemented");
                ledPositions.put(curTick, Integer.MAX_VALUE);
                i += 4;
                continue;
            }
            if (melodyString.startsWith("ledoff", i)) 
            {
                Mobile.log(Mobile.LOG_WARNING, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ": " + "LED Off event not implemented");
                ledPositions.put(curTick, 0);
                i += 5;
                continue;
            }
            if (melodyString.startsWith("vibeon", i)) 
            {
                Mobile.log(Mobile.LOG_WARNING, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ": " + "Vibration On event not implemented");
                vibePositions.put(curTick, Integer.MAX_VALUE);
                i += 5;
                continue;
            }
            if (melodyString.startsWith("vibeoff", i)) 
            {
                Mobile.log(Mobile.LOG_WARNING, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ": " + "Vibration Off event not implemented");
                vibePositions.put(curTick, 0);
                i += 6;
                continue;
            }
            if (melodyString.startsWith("backon", i)) 
            {
                Mobile.log(Mobile.LOG_WARNING, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ": " + "Backlight On event not implemented");
                backlightPositions.put(curTick, Integer.MAX_VALUE);
                i += 5;
                continue;
            }
            if (melodyString.startsWith("backoff", i)) 
            {
                Mobile.log(Mobile.LOG_WARNING, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ": " + "Backlight Off event not implemented");
                backlightPositions.put(curTick, 0);
                i += 6;
                continue;
            }

            // Actual note specifier
            if (isNoteCharacter(currentChar)) 
            {
                noteValue = getNoteValue(currentChar, octave);
                noteDuration = Character.getNumericValue(melodyString.charAt(++i)); // Get duration
                noteDuration = getDurationInTicks(noteDuration);

                int restDuration = 0;

                // If there are still more characters to be read, we can check if the next char is a
                // note duration specifier (dotted, double dotted or 2/3 length).
                if(i+1 < melodyString.length())
                {
                    if(melodyString.charAt(i+1) == '.') { noteDuration *= 1.5; i++; } // dotted note
                    else if(melodyString.charAt(i+1) == ';') { noteDuration *= 1.75; i++; } // double dotted note
                    else if(melodyString.charAt(i+1) == ':') { noteDuration = (int) Math.round(noteDuration * (2.0 / 3.0)); i++; } // 2/3 length note
                }

                switch(style)
                {
                    case 0: // Natural Style, 20:1 ratio of note:rest
                        restDuration = noteDuration / 20;
                        noteDuration *= (20 / 21);
                        break;

                    case 1: // Continuous style, no rest.
                        break;

                    case 2: // Staccato Style, 1:1 ratio of note:rest
                        noteDuration /= 2;    
                        restDuration = noteDuration;
                }
                
                Mobile.log(Mobile.LOG_DEBUG, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ": " + "Adding note:" + (noteModifier > 0 ? "#" : (noteModifier < 0 ? "&" : "")) + currentChar + octave + " with duration " + noteDuration + " and velocity " + volume + " from time " + curTick + " to " + (curTick+noteDuration));
                noteValue += noteModifier;

                ShortMessage noteOn = new ShortMessage();
                ShortMessage noteOff = new ShortMessage();

                noteOn.setMessage(ShortMessage.NOTE_ON, 0, noteValue, volume);
                track.add(new MidiEvent(noteOn, curTick));
                
                noteOff.setMessage(ShortMessage.NOTE_OFF, 0, noteValue, 0);
                track.add(new MidiEvent(noteOff, curTick + noteDuration));
                
                currentEvents.add(new MidiEvent(noteOn, curTick));

                curTick += noteDuration + restDuration;

                currentEvents.add(new MidiEvent(noteOff, curTick - 50));

                
                
                noteModifier = 0; // Restore the note modifier
                continue;
            }

            if(currentChar == '@') // '@' is a special character denoting how many times this note/led/vibe/back event block should repeat
            {   
                int numRepeats = Character.getNumericValue(melodyString.charAt(++i));
                Mobile.log(Mobile.LOG_DEBUG, EMSiMelodyDecoder.class.getPackage().getName() + "." + EMSiMelodyDecoder.class.getSimpleName() + ": " + (numRepeats == 0 ? "Infinite (unsupported, capped to 255)" : numRepeats-1) + " block repeats requested!");
                
                if(numRepeats == 0) { numRepeats = 255; } // 0 means infinite looping of a block, but that's not feasible in MIDI i think
                for(int rep = 1; rep < numRepeats; rep++) 
                { 
                    curTick += tempo/4; // Add a small gap between repeats
                    int curTickIncrement = 0;
                    for (MidiEvent event : currentEvents) 
                    {
                        MidiEvent newEvent = new MidiEvent(event.getMessage(), curTick + event.getTick());
                        track.add(newEvent);
                        curTickIncrement = (int) newEvent.getTick();
                    }
                    curTick = curTickIncrement;
                }
                currentEvents.clear(); // Clear the repeated block's events
                continue;
            }

            // If nothing above matched the current character, skip it
            i++;
        }
    }

    private static boolean isNoteCharacter(char c)  { return "cdefgab".indexOf(c) >= 0; }

    private static int getDurationInTicks(int durationValue) 
    {
        // Calculate duration in ticks based on the duration value
        switch (durationValue) 
        {
            case 0: return PPQ * 4; // Full-note
            case 1: return PPQ * 2; // 1/2-note
            case 2: return PPQ;     // 1/4-note
            case 3: return PPQ / 2; // 1/8-note
            case 4: return PPQ / 4; // 1/16-note
            case 5: return PPQ / 8; // 1/32-note
            default: return 0; // Invalid duration
        }
    }

    private static int getNoteValue(char note, int octave) 
    {
        int baseNote = 0;

        switch (note) 
        {
            case 'c': baseNote = 12; break;
            case 'd': baseNote = 14; break; // D4
            case 'e': baseNote = 16; break; // E4
            case 'f': baseNote = 17; break; // F4
            case 'g': baseNote = 19; break; // G4
            case 'a': baseNote = 21; break; // A4
            case 'b': baseNote = 23; break; // B4
            default: baseNote = 40; // Invalid note, default to silence
        };
        return baseNote + octave * 12;
    }
}