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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.Arrays;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import org.recompile.mobile.Mobile;

// Decoder for NTT DoCoMo's MLD/MFi format, closely related to CMF: https://web.archive.org/web/20220912152735/https://datatracker.ietf.org/doc/html/draft-atarius-cmf-00
public final class MLDDecoder
{

    private static final byte MLD_EXTA_MSG  = (byte) 0x3F;
    private static final byte MLD_EXTB_MSG  = (byte) 0x7F;
    private static final byte MLD_EXTC_MSG  = (byte) 0xBF;
    private static final byte MLD_SYSEX_MSG = (byte) 0xFF;

    // These are used only for debugging
    private static final String[] formatTypes = {"RESERVED", "0x1: Melody", "0x2: Song"};
    private static final String[] melodyTypes = {"RESERVED", "0x1: Complete Melody", "0x2: Part of Melody"};
    private static final String[] sourceFromTypes = 
    {
        "Network",
        "Terminal",
        "External",
        "RESERVED"
    };

    // PCM-Specific variables
    public static boolean isPCM = false; // Used by PlatformPlayer to decide whether it'll load the decoded data into a WAVPlayer or MIDIPlayer

    // Structures that hold decoded MLD data
    public static List<InputStream> pcmData = null;
    public static InputStream SequenceData = null;
    public static Map<Integer, Integer> pcmDataPositions = new HashMap<Integer, Integer>();
    public static Map<Integer, Integer> pcmDataVelocities = new HashMap<Integer, Integer>();

    private static PlaybackTimeline playbackTimeline = new PlaybackTimeline();

    public static PlaybackTimeline getPlaybackTimeline() { return playbackTimeline; }

    /*
     * DD loops can make the converted MIDI longer than the source. This keeps the
     * displayed time on the original MLD timeline. A new instance is made for each file,
     * so an existing player keeps its own timeline when another MLD is decoded.
     */
    public static final class PlaybackTimeline
    {
        private List<MLDMelodyDecoder.TempoPoint> tempoPoints = Collections.emptyList();
        private List<Integer> rewindRawTicks = Collections.emptyList();
        private List<Integer> rewindSourceRawTicks = Collections.emptyList();
        private int endRawTick;
        private int parserCycleStartRawTick = -1;
        private int parserCycleEndRawTick = -1;
        private int sequenceLoopStartRawTick = -1;
        private int displayEndRawTick = -1;
        private long displayDurationMicros = -1L;

        private PlaybackTimeline() { }

        public long displayDuration(long fallback)
        {
            return displayDurationMicros > 0 ? displayDurationMicros : fallback;
        }

        public long displayTime(long midiTick, int resolution, long fallback)
        {
            int sourceRawTick = sourceRawTick(midiTick, resolution);
            if(sourceRawTick < 0) { return fallback; }
            return (displayDurationMicros * sourceRawTick) / endRawTick;
        }

        private void setSourceProgress(
                List<Integer> rewindRawTicks,
                List<Integer> rewindSourceRawTicks,
                int endRawTick,
                int parserCycleStartRawTick,
                int parserCycleEndRawTick,
                int sequenceLoopStartRawTick,
                int displayEndRawTick)
        {
            this.rewindRawTicks = rewindRawTicks;
            this.rewindSourceRawTicks = rewindSourceRawTicks;
            this.endRawTick = Math.max(0, endRawTick);
            this.parserCycleStartRawTick = parserCycleStartRawTick;
            this.parserCycleEndRawTick = parserCycleEndRawTick;
            this.sequenceLoopStartRawTick = sequenceLoopStartRawTick;
            this.displayEndRawTick = displayEndRawTick;
        }

        private int sourceRawTick(long midiTick, int resolution)
        {
            if(displayDurationMicros <= 0 || endRawTick <= 0 || rewindRawTicks.isEmpty() || tempoPoints.isEmpty()) { return -1; }

            int rawTick = rawTickAt(midiTick, resolution);
            int parserCycleRawTicks = parserCycleEndRawTick - parserCycleStartRawTick;
            if(sequenceLoopStartRawTick >= 0 && parserCycleRawTicks > 0 && rawTick >= sequenceLoopStartRawTick)
            {
                rawTick = parserCycleStartRawTick
                        + (rawTick - sequenceLoopStartRawTick) % parserCycleRawTicks;
            }

            int sourceRawTick = rawTick;
            for(int i = 0; i < rewindRawTicks.size(); i++)
            {
                if(rewindRawTicks.get(i).intValue() > rawTick) { break; }
                sourceRawTick = rawTick + rewindSourceRawTicks.get(i).intValue() - rewindRawTicks.get(i).intValue();
            }
            return Math.max(0, Math.min(endRawTick, sourceRawTick));
        }

        private int rawTickAt(long midiTick, int resolution)
        {
            MLDMelodyDecoder.TempoPoint point = tempoPoints.get(0);
            for(int i = 1; i < tempoPoints.size() && tempoPoints.get(i).midiTick <= midiTick; i++) { point = tempoPoints.get(i); }
            long deltaMidiTick = Math.max(0L, midiTick - point.midiTick);
            long rawTick = point.rawTick + (deltaMidiTick * point.timebase) / Math.max(1, resolution);
            return rawTick > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) rawTick;
        }
    }

    private static final class DecodeState
    {
        byte[] input;
        int decodePos;
        byte numTracks;
        Sequence sequence;
        int noteExtraBytes;
        List<byte[]> melodyTrackData;
        int exst;
        int[] cuePoints;

        DecodeState(byte[] data)
        {
            this.input = data;
            this.decodePos = 0;
            this.exst = -1;
            this.numTracks = 0;
            this.sequence = null;
            this.noteExtraBytes = 0;
            this.melodyTrackData = new ArrayList<byte[]>();
            this.cuePoints = new int[0];
        }

        String readChunkId()
        {
            return "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++];
        }

        int readChunkSize16()
        {
            return (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF);
        }

        int readChunkSize32()
        {
            return (input[decodePos++] & 0xFF) << 24 | (input[decodePos++] & 0xFF) << 16 | (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF);
        }
    }

    public static synchronized void decodeMLD(byte[] data)
	{
        isPCM = false;
        pcmData = new ArrayList<InputStream>();
        SequenceData = null;
        pcmDataPositions.clear();
        pcmDataVelocities.clear();
        playbackTimeline = new PlaybackTimeline();

        DecodeState state = new DecodeState(data);

        boolean parsingData = true;

        // Start parsing the file.
        decodeHeader(state); // melo (file header)

        while(parsingData && state.decodePos < data.length)
        {
            String chunkID = "" + (char) state.input[state.decodePos] + (char) state.input[state.decodePos+1] + (char) state.input[state.decodePos+2] + (char) state.input[state.decodePos+3];

            if (chunkID.equals("adat"))      { decodeADATChunk(state); }
            //else if (chunkID.equals("adpm")) { decodeADPMChunk(state, 0); } // ADPM is read in ADATChunk above
            else if (chunkID.equals("ainf")) { decodeAINFChunk(state); }
            else if (chunkID.equals("auth")) { decodeAUTHChunk(state); } 
            else if (chunkID.equals("copy")) { decodeCOPYChunk(state); } 
            else if (chunkID.equals("code")) { decodeCODEChunk(state); } 
            else if (chunkID.equals("cuep")) { decodeCUEPChunk(state); } // TODO: Untested
            else if (chunkID.equals("date")) { decodeDATEChunk(state); } 
            else if (chunkID.equals("exst")) { decodeEXSTChunk(state); }
            else if (chunkID.equals("note")) { decodeNOTEChunk(state); } 
            else if (chunkID.equals("prot")) { decodePROTChunk(state); } 
            else if (chunkID.equals("sorc")) { decodeSORCChunk(state); } 
            else if (chunkID.equals("supt")) { decodeSUPTChunk(state); } 
            else if (chunkID.equals("thrd")) { decodeTHRDChunk(state); } // TODO: Properly parse this, right now no 3D positioning is accounted for
            else if (chunkID.equals("titl")) { decodeTITLChunk(state); } 
            else if (chunkID.equals("vers")) { decodeVERSChunk(state); } 
            else if (chunkID.equals("trac")) { decodeTRACChunk(state); } 
            else                             { parsingData = false; } // Assume we reached EOF
        }

        buildMelodySequence(state);

        try
        {
            // Convert the resulting sequence to byte array and send to the player.
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            int midiFileType = resolveMidiFileType(state.sequence);
            MidiSystem.write(state.sequence, midiFileType, output);
            SequenceData = new ByteArrayInputStream(output.toByteArray());

            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + " MFi parsing and conversion finished, MIDI file type:" + midiFileType + " Sequence data size:" + output.size() + " | number of PCM streams:" + pcmData.size());
        }
        catch (Exception e) 
        { 
            Mobile.log(Mobile.LOG_ERROR, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + " couldn't write converted MFi Data:" + e.getMessage()); 
            e.printStackTrace(); 
            SequenceData = null;
            pcmData = null;
        }
	}

    private static int resolveMidiFileType(Sequence midiSequence)
    {
        int[] supportedTypes = MidiSystem.getMidiFileTypes(midiSequence);

        for(int i = 0; i < supportedTypes.length; i++)
        {
            if(supportedTypes[i] == 1) { return 1; }
        }

        if(supportedTypes.length > 0) { return supportedTypes[0]; }

        throw new IllegalStateException("No supported MIDI file type for generated sequence.");
    }

    private static void buildMelodySequence(DecodeState state)
    {
        try
        {
            if(state.melodyTrackData == null || state.melodyTrackData.isEmpty())
            {
                return;
            }

            MLDMelodyDecoder.DecodeResult decodeResult = MLDMelodyDecoder.build(state.melodyTrackData, state.numTracks, state.noteExtraBytes, Math.max(state.exst, 0));
            state.sequence = decodeResult.sequence;
            playbackTimeline = decodeResult.playbackTimeline;
            pcmDataPositions.putAll(decodeResult.pcmPositions);
            pcmDataVelocities.putAll(decodeResult.pcmVelocities);

            for(int i = 0; i < decodeResult.warnings.size(); i++)
            {
                Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + decodeResult.warnings.get(i));
            }
        }
        catch(Exception e)
        {
            Mobile.log(Mobile.LOG_ERROR, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + " couldn't build melody sequence:" + e.getMessage());
            e.printStackTrace();
            state.sequence = null;
        }
    }

    private static void decodeHeader(DecodeState state) 
    {
        String fileChunkID = state.readChunkId(); // "melo"
        int fileChunkSize = state.readChunkSize32() - 8;
        
        int headerLength = (state.input[state.decodePos++] & 0xFF) << 8 | (state.input[state.decodePos++] & 0xFF);
        
        int songType = state.input[state.decodePos++] & 0xFF;
        int instruments = state.input[state.decodePos++] & 0xFF;
        state.numTracks = (byte) (state.input[state.decodePos++] & 0xFF);

        if(!"melo".equals(fileChunkID))
        {
            throw new IllegalArgumentException("Unexpected MLD header: " + fileChunkID);
        }

        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"-------------------------- MLD CONTENT HEADER --------------------------");
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"fileChunkID: " + fileChunkID);
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"fileChunkSize: " + fileChunkSize);
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"headerSize: " + headerLength);
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"songType: " + formatTypes[songType]);
        if (songType == 0x02)
        {
            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"contentClass has music events: " + ((instruments & 0x01) != 0));
            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"contentClass has pcm data: " + ((instruments & 0x02) != 0));
            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"contentClass has text data: " + ((instruments & 0x04) != 0));
            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"contentClass has image data: " + ((instruments & 0x08) != 0));
            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"contentClass has fem vocals: " + ((instruments & 0x10) != 0));
            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"contentClass has male vocals: " + ((instruments & 0x20) != 0));
            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"contentClass has other vocals: " + ((instruments & 0x40) != 0));
        } 
        else 
        {
            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"melody type: " + melodyTypes[instruments]);
        }
        
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"numTracks: " + state.numTracks + (state.numTracks == 1 ? " (MFi)" : " (MFi 2)"));

        // We now have sufficient data to create a proper MIDI sequence.
        try 
        {
            state.cuePoints = new int[state.numTracks];
            
            // Default timebase for CMF/MFi is 48, we keep an empty placeholder sequence here and replace it after melody compilation
            state.sequence = new Sequence(Sequence.PPQ, 960);
        } 
        catch(InvalidMidiDataException ie) { Mobile.log(Mobile.LOG_ERROR, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + " couldn't create MIDI Sequence to convert:" + ie.getMessage()); }

        // OK, we're at the start of the "sorc" chunk, which means the content info chunk (content header) has been left behind
    }

    private static void decodeSORCChunk(DecodeState state)
    {
        // We're at the Score Track Chunk, so let's decode the info about the audio data
        state.readChunkId(); // chunk ID already checked by caller ("sorc")
        int chunkSize = state.readChunkSize16(); // length is 16 bit for most subchunks in MLD it seems
        byte sourceType = (byte) (state.input[state.decodePos++] & 0xFF);

        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Source info > From: " + sourceFromTypes[sourceType & 0xF7] + " | Has copyright: " + ((sourceType & 0x01) == 1 ? "Yes" : "No"));
    }

    private static void decodeTITLChunk(DecodeState state) 
    {
        state.readChunkId();
        int chunkSize = state.readChunkSize16();
        byte[] byteData = new byte[chunkSize];
        
        for(int i = 0; i < chunkSize; i++) { byteData[i] = (byte) (state.input[state.decodePos++]); }

        try 
        {
            String MLDTrackData = new String(byteData, "Shift_JIS");

            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Title: " + MLDTrackData);
        }
        catch(UnsupportedEncodingException e) { }
    }

    private static void decodeVERSChunk(DecodeState state) 
    {
        state.readChunkId();
        int chunkSize = state.readChunkSize16();
        byte[] byteData = new byte[chunkSize];
        
        for(int i = 0; i < chunkSize; i++) { byteData[i] = (byte) (state.input[state.decodePos++]); }

        String MLDTrackData = new String(byteData);

        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Version: " + MLDTrackData);
    }

    private static void decodeDATEChunk(DecodeState state) 
    {
        state.readChunkId();
        int chunkSize = state.readChunkSize16();
        byte[] byteData = new byte[chunkSize];
        
        for(int i = 0; i < chunkSize; i++) { byteData[i] = (byte) (state.input[state.decodePos++]); }

        String MLDTrackData = new String(byteData);

        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Date: " + MLDTrackData);
    }

    private static void decodeEXSTChunk(DecodeState state) 
    {
        state.readChunkId();
        int chunkSize = state.readChunkSize16();
        byte[] byteData = new byte[chunkSize];
        
        for(int i = 0; i < chunkSize; i++) { byteData[i] = (byte) (state.input[state.decodePos++]); }

        state.exst = chunkSize >= 2 ? (((byteData[0] & 0xFF) << 8) | (byteData[1] & 0xFF)) : 0;

        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Exst: " + state.exst);
    }


    private static void decodeCOPYChunk(DecodeState state) 
    {
        state.readChunkId();
        int chunkSize = state.readChunkSize16();
        byte[] byteData = new byte[chunkSize];
        
        for(int i = 0; i < chunkSize; i++) { byteData[i] = (byte) (state.input[state.decodePos++]); }

        String MLDTrackData = new String(byteData);

        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Copyright: " + MLDTrackData);
    }

    private static void decodeCODEChunk(DecodeState state) 
    {
        state.readChunkId();
        int chunkSize = state.readChunkSize16();
        byte[] byteData = new byte[chunkSize];
        
        for(int i = 0; i < chunkSize; i++) { byteData[i] = (byte) (state.input[state.decodePos++]); }

        String MLDTrackData = new String(byteData);

        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Code: " + MLDTrackData);
    }

    private static void decodeSUPTChunk(DecodeState state) 
    {
        state.readChunkId();
        int chunkSize = state.readChunkSize16();
        byte[] byteData = new byte[chunkSize];
        
        for(int i = 0; i < chunkSize; i++) { byteData[i] = (byte) (state.input[state.decodePos++]); }

        String MLDTrackData = "";
        try { MLDTrackData = new String(byteData, "Shift_JIS"); }
        catch (Exception e) { }

        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"suptval: " + MLDTrackData);
    }

    private static void decodePROTChunk(DecodeState state) 
    {
        state.readChunkId();
        int chunkSize = state.readChunkSize16();
        byte[] byteData = new byte[chunkSize];
        
        for(int i = 0; i < chunkSize; i++) { byteData[i] = (byte) (state.input[state.decodePos++]); }

        String contentProvider = new String(byteData);

        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Content Provider: " + contentProvider);
    }

    private static void decodeNOTEChunk(DecodeState state) 
    {
        state.readChunkId();
        int chunkSize = state.readChunkSize16();
        int alignbyte = state.input[state.decodePos++] & 0xFF; // TODO: Find out if this is ever different from 0x00 (and if the NOTE chunk ever has a chunkSize larger than 2 bytes)
        int extraByteCount = state.input[state.decodePos++] & 0xFF;

        state.noteExtraBytes = (alignbyte << 8) | extraByteCount;
        
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Note extra bytes: " + state.noteExtraBytes);
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Notes are 3 Bytes: " + (state.noteExtraBytes == 0));
    }

    private static void decodeAUTHChunk(DecodeState state) 
    {
        state.readChunkId();
        int chunkSize = state.readChunkSize16();
        byte[] byteData = new byte[chunkSize];
        
        for(int i = 0; i < chunkSize; i++) { byteData[i] = (byte) (state.input[state.decodePos++]); }

        String author = new String(byteData);

        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Author: " + author);
    }

    private static void decodeTHRDChunk(DecodeState state) 
    {
        state.readChunkId();
        int chunkSize = state.readChunkSize16();
        byte[] byteData = new byte[chunkSize];
        
        for(int i = 0; i < chunkSize; i++) { byteData[i] = (byte) (state.input[state.decodePos++]); }

        Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"-------------------------- 3D POS INFO CHUNK --------------------------");
        Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"3D Positioning data: " + Arrays.toString(byteData));
    }

    private static void decodeCUEPChunk(DecodeState state) 
    {
        state.readChunkId();
        int chunkSize = state.readChunkSize16();
        for (int i = 0; i < state.cuePoints.length; i++) { state.cuePoints[i] = (state.input[state.decodePos++] & 0xFF) << 24 | (state.input[state.decodePos++] & 0xFF) << 16 | (state.input[state.decodePos++] & 0xFF) << 8 | (state.input[state.decodePos++] & 0xFF); }

        Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"-------------------------- CUEPOINT INFO CHUNK --------------------------");
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Chunk Size: " + chunkSize);
        Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Cue points: " + Arrays.toString(state.cuePoints));
    }

    private static void decodeAINFChunk(DecodeState state) 
    {
        state.readChunkId();
        int chunkSize = state.readChunkSize16();
        int numStreams = (state.input[state.decodePos++] & 0xFF);
        int hasPCMData = (state.input[state.decodePos++] & 0xFF);
        
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"-------------------------- AUDIO INFO CHUNK --------------------------");
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Chunk Size: " + chunkSize);
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Amount of (AD)PCM streams: " + numStreams);
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Has following (AD)PCM streams: " + (hasPCMData == 0 ? "Yes" : "No"));
    }

    private static void decodeADATChunk(DecodeState state) 
    {
        state.readChunkId();
        int chunkSize = state.readChunkSize32();
        int adpmHeaderLen = state.readChunkSize16();
        
        // adpmHeaderLen contains these two as well
        int dataFormat = (state.input[state.decodePos++] & 0xFF);
        int dataAttribute = (state.input[state.decodePos++] & 0xFF);

        // adpcmSize needs to subtract adpcmHeaderLen (2 bytes), dataFormat (1),
        // dataAttribute (1), and the entire ADPM header (9) = 13 bytes.
        int adpcmSize = chunkSize - 13;
        
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"-------------------------- AUDIO DATA CHUNK --------------------------");
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Chunk Size: " + chunkSize);
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"ADPCM Data Size: " + adpcmSize);
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"ADPM Header Length (+fmt +attr): " + adpmHeaderLen);
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Data format: " + dataFormat);
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Data attribute: " + dataAttribute);
    
        decodeADPMChunk(state, adpcmSize);
    }

    private static void decodeADPMChunk(DecodeState state, int size) 
    {
        state.readChunkId();
        int chunkSize = state.readChunkSize16();
        int sampleRate = (state.input[state.decodePos++] & 0xFF);
        int bitDepth = (state.input[state.decodePos++] & 0xFF);
        int numChannels = (state.input[state.decodePos++] & 0xFF);

        byte[] waveData = new byte[size];

        for(int i = 0; i < size; i++) 
        {
            waveData[i] = state.input[state.decodePos++];
        }

        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"-------------------------- ADPCM DATA CHUNK --------------------------");
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Chunk Size: " + chunkSize);
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Sample Rate: " + (sampleRate * 1000) + "Hz");
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Bit Depth: " + bitDepth + " bits"); // This is either 2 or 4 bits
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Channel type: " + ((numChannels & 0x07) == 1 ? "Mono " : "Stereo ") + ((numChannels & 0x08) == 0 ? "Non-Interleaved" : "Interleaved"));

        if(bitDepth == 4) 
        {
            pcmData.add(new ByteArrayInputStream(WAVYamahaADPCMDecoder.ADPCMZDecode(waveData, sampleRate * 1000, (numChannels & 0x07))));
        }
    }

    private static void decodeTRACChunk(DecodeState state) 
    {
        state.readChunkId();
        
        // Gradius Neo Imperial has an MLD file that contains a track chunk with no size (and thus nothing after it), so return immediately in those cases
        if(state.decodePos == state.input.length) 
        { 
            Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Track has no data. Skipping");
            return; 
        }
        
        int chunkSize = state.readChunkSize32();
        
        chunkSize = Math.min(chunkSize, state.input.length - state.decodePos); // The final track chunk might report an incorrect chunkSize (happens in some Gradius NEO Imperial files)
        
        byte[] byteData = new byte[chunkSize];
        
        for(int i = 0; i < chunkSize; i++) 
        { 
            if(state.decodePos == state.input.length) { break; }
            byteData[i] = (byte) (state.input[state.decodePos++]); 
        }

        state.melodyTrackData.add(byteData);
    }

    /* -------------------------------------------------------------------------------------- */
    /*           Lower level decoding functions (sequence event, duration, etc)               */
    /* -------------------------------------------------------------------------------------- */

    public static final class MLDSequenceMarker
    {
        public static final int META_MARKER_TYPE = 0x06;
        private static final String LOOP_MARKER_PREFIX = "MLD_LOOP:";
        private static final String STOP_MARKER_PREFIX = "MLD_STOP:";

        private MLDSequenceMarker()
        {
        }

        public static byte[] encodeStopMarker(long tick)
        {
            return encodeAscii(STOP_MARKER_PREFIX + tick);
        }

        public static byte[] encodeLoopMarker(long loopStartTick, long loopEndTick, int repeatCount)
        {
            return encodeAscii(LOOP_MARKER_PREFIX + loopStartTick + ":" + loopEndTick + ":" + repeatCount);
        }

        public static String decodeMarker(MetaMessage meta)
        {
            if (meta == null || meta.getType() != META_MARKER_TYPE)
            {
                return null;
            }
            try
            {
                return new String(meta.getData(), "US-ASCII");
            }
            catch (UnsupportedEncodingException e)
            {
                return new String(meta.getData());
            }
        }

        public static boolean isStopMarker(String marker)
        {
            return marker != null && marker.startsWith(STOP_MARKER_PREFIX);
        }

        public static boolean isLoopMarker(String marker)
        {
            return marker != null && marker.startsWith(LOOP_MARKER_PREFIX);
        }

        public static LoopMarker parseLoopMarker(String marker)
        {
            if (!isLoopMarker(marker))
            {
                return null;
            }
            String[] parts = marker.substring(LOOP_MARKER_PREFIX.length()).split(":");
            if (parts.length != 3)
            {
                return null;
            }
            try
            {
                return new LoopMarker(Long.parseLong(parts[0]), Long.parseLong(parts[1]), Integer.parseInt(parts[2]));
            }
            catch (NumberFormatException e)
            {
                return null;
            }
        }

        private static byte[] encodeAscii(String marker)
        {
            try
            {
                return marker.getBytes("US-ASCII");
            }
            catch (UnsupportedEncodingException e)
            {
                return marker.getBytes();
            }
        }

        public static final class LoopMarker
        {
            public final long loopStartTick;
            public final long loopEndTick;
            public final int repeatCount;

            private LoopMarker(long loopStartTick, long loopEndTick, int repeatCount)
            {
                this.loopStartTick = loopStartTick;
                this.loopEndTick = loopEndTick;
                this.repeatCount = repeatCount;
            }
        }
    }

    public static final class MLDMelodyDecoder
    {
        private static final int MIDI_CHANNEL_COUNT = 16;
        private static final int MAX_LOGICAL_CHANNELS = 64;
        private static final int MIDI_PPQ = 1920;
        private static final int DEFAULT_TIMEBASE = 48;
        private static final int DEFAULT_TEMPO = 125;
        private static final int MIN_TEMPO = 20;
        private static final int MAX_TEMPO = 255;
        private static final int DEFAULT_MASTER_VOLUME = 100;
        private static final int DEFAULT_LEVEL = 63;
        private static final int DEFAULT_PAN = 32;
        private static final int DEFAULT_PITCH_COARSE = 32;
        private static final int DEFAULT_PITCH_FINE = 32;
        private static final int DEFAULT_PITCH_RANGE = 2;
        private static final int DEFAULT_MODULATION = 0;
        private static final int[] OCTAVE_TABLE = new int[] { 0, 12, -24, -12 };
        private static final int MIDI_DRUM_CHANNEL = 9;

        private MLDMelodyDecoder()
        {
        }

        static DecodeResult build(List<byte[]> trackPayloads, int declaredTrackCount, int noteExtraBytes, int exstSize)
                throws IOException, InvalidMidiDataException
        {
            int effectiveTrackCount = declaredTrackCount > 0 ? declaredTrackCount : trackPayloads.size();
            List<String> warnings = new ArrayList<String>();
            List<List<TrackEvent>> decodedTracks = new ArrayList<List<TrackEvent>>(trackPayloads.size());
            for (int i = 0; i < trackPayloads.size(); i++)
            {
                decodedTracks.add(decodeTrack(i, trackPayloads.get(i), noteExtraBytes, exstSize));
            }

            PlaybackTimeline timeline = new PlaybackTimeline();
            ScheduledExecution execution = scheduleEvents(decodedTracks, effectiveTrackCount, warnings, timeline);
            List<TempoPoint> tempoPoints = buildTempoPoints(execution.events, warnings);
            timeline.tempoPoints = tempoPoints;
            if (timeline.displayEndRawTick > 0)
            {
                long displayEndTick = rawToMidiTick(tempoPoints, timeline.displayEndRawTick);
                timeline.displayDurationMicros = midiTickToMicroseconds(tempoPoints, displayEndTick);
            }
            Map<Integer, Integer> pcmPositions = new HashMap<Integer, Integer>();
            Map<Integer, Integer> pcmVelocities = new HashMap<Integer, Integer>();
            extractPcmTriggers(decodedTracks, tempoPoints, pcmPositions, pcmVelocities, warnings);

            Sequence sequence = new Sequence(Sequence.PPQ, MIDI_PPQ);
            Track conductorTrack = sequence.createTrack();
            addTrackName(conductorTrack, "MLD Conductor");
            Track[] channelTracks = new Track[MIDI_CHANNEL_COUNT];
            for (int midiChannel = 0; midiChannel < MIDI_CHANNEL_COUNT; midiChannel++)
            {
                channelTracks[midiChannel] = sequence.createTrack();
                addTrackName(channelTracks[midiChannel], "MLD Channel " + midiChannel);
            }

            List<MessageEvent> messageEvents = new ArrayList<MessageEvent>();
            ControlCollector controlCollector = new ControlCollector(messageEvents);
            ChannelState[] channels = createChannelStates();
            int[] partChannelMap = createIdentityPartChannelMap(Math.max(MIDI_CHANNEL_COUNT, effectiveTrackCount * 4));
            RenderState renderState = new RenderState(
                    messageEvents,
                    controlCollector,
                    channels,
                    partChannelMap,
                    new LinkedHashMap<Integer, ActiveNote>(),
                    new ArrayList<Long>());
            emitInitialMidiDefaults(controlCollector, channels, 0L);

            long maxTick = 0L;
            for (int i = 0; i < execution.events.size(); i++)
            {
                TrackEvent event = execution.events.get(i);
                maxTick = Math.max(maxTick, flushExpiredNotes(event.rawTick, renderState.activeNotes, messageEvents));
                if (event instanceof NoteEvent)
                {
                    maxTick = Math.max(maxTick, handleNoteEvent((NoteEvent) event, tempoPoints, warnings, renderState));
                }
                else if (event instanceof SystemEvent)
                {
                    maxTick = Math.max(maxTick, handleSystemEvent((SystemEvent) event, tempoPoints, warnings, renderState));
                }
            }
            maxTick = Math.max(maxTick, flushExpiredNotes(Integer.MAX_VALUE, renderState.activeNotes, messageEvents));
            maxTick = Math.max(maxTick, rawToMidiTick(tempoPoints, execution.endRawTick));

            boolean hasLoop = execution.hasLoop();
            long loopStartTick = hasLoop ? rawToMidiTick(tempoPoints, execution.loopStartRawTick) : -1L;
            long loopEndTick = hasLoop ? rawToMidiTick(tempoPoints, execution.loopEndRawTick) : -1L;
            long contentEndTick = hasLoop ? Math.max(1L, loopEndTick) : Math.max(1L, maxTick);
            emitTempoTrack(tempoPoints, conductorTrack, contentEndTick);
            if (hasLoop)
            {
                addLoopMarker(conductorTrack, loopStartTick, loopEndTick, -1);
            }
            addStopMarkers(conductorTrack, renderState.stopTicks);

            int[] outputChannelMap = buildOutputChannelMap(renderState.activeOutputMask);
            Collections.sort(messageEvents, MESSAGE_EVENT_COMPARATOR);
            for (int i = 0; i < messageEvents.size(); i++)
            {
                MessageEvent event = messageEvents.get(i);
                if (hasLoop && event.tick > contentEndTick) { continue; }
                int midiChannel = outputChannelMap[event.midiChannel];
                addShortMessage(channelTracks[midiChannel], event.status, midiChannel, event.data1, event.data2, event.tick);
            }

            addEndOfTrack(conductorTrack, contentEndTick + 1L);
            for (int i = 0; i < channelTracks.length; i++)
            {
                addEndOfTrack(channelTracks[i], contentEndTick + 1L);
            }
            return new DecodeResult(sequence, timeline, pcmPositions, pcmVelocities, warnings);
        }

        private static List<TrackEvent> decodeTrack(int trackIndex, byte[] payload, int noteExtraBytes, int exstSize) throws IOException
        {
            List<TrackEvent> events = new ArrayList<TrackEvent>();
            int offset = 0;
            int rawTick = 0;
            int pendingExtendedDelta = 0;

            while (offset < payload.length)
            {
                if (offset + 2 > payload.length)
                {
                    throw new IOException("Truncated event in track " + trackIndex + " at 0x" + Integer.toHexString(offset));
                }

                int delta = (payload[offset] & 0xFF) + pendingExtendedDelta;
                pendingExtendedDelta = 0;
                int status = payload[offset + 1] & 0xFF;
                offset += 2;
                rawTick += delta;

                // Resource statuses 0x3F/0x7F/0xBF share the same command/body framing.
                if (status == (MLD_EXTA_MSG & 0xFF) || status == (MLD_EXTB_MSG & 0xFF) || status == (MLD_EXTC_MSG & 0xFF))
                {
                    if (offset >= payload.length)
                    {
                        throw new IOException("Truncated resource event in track " + trackIndex);
                    }

                    int command = payload[offset++] & 0xFF;
                    if (command >= 0xF0)
                    {
                        // Long resource events carry an opaque big-endian length-prefixed body.
                        if (offset + 2 > payload.length)
                        {
                            throw new IOException("Truncated long resource event in track " + trackIndex);
                        }
                        int length = readBe16(payload, offset);
                        offset += 2;
                        if (offset + length > payload.length)
                        {
                            throw new IOException("Resource payload overruns track " + trackIndex);
                        }
                        offset += length;
                        events.add(new ResourceEvent(trackIndex, rawTick, command, -1, -1));
                    }
                    else
                    {
                        int bodyLength = bodyLengthForResourceCommand(command, exstSize);
                        if (offset + bodyLength > payload.length)
                        {
                            throw new IOException("Truncated resource body in track " + trackIndex);
                        }
                        int value = bodyLength > 0 ? payload[offset] & 0xFF : -1;
                        int part = value >= 0 && command >= 0x80 && command < 0xF0 ? ((value >> 6) & 0x03) : -1;
                        offset += bodyLength;
                        events.add(new ResourceEvent(trackIndex, rawTick, command, value, part));
                    }
                    continue;
                }

                // System status 0xFF carries tempo, loop, global stop, and timing controls.
                if (status == (MLD_SYSEX_MSG & 0xFF))
                {
                    if (offset >= payload.length)
                    {
                        throw new IOException("Truncated system event in track " + trackIndex);
                    }

                    int command = payload[offset++] & 0xFF;
                    if (command < 0x80)
                    {
                        int bodyLength = 1 + Math.max(0, exstSize);
                        if (offset + bodyLength > payload.length)
                        {
                            throw new IOException("Truncated short system envelope in track " + trackIndex);
                        }
                        offset += bodyLength;
                        events.add(new SystemEvent(trackIndex, rawTick, command, -1, -1, -1));
                        continue;
                    }

                    if (command >= 0xF0)
                    {
                        if (offset + 2 > payload.length)
                        {
                            throw new IOException("Truncated machine-dependent event in track " + trackIndex);
                        }
                        int length = readBe16(payload, offset);
                        offset += 2;
                        if (offset + length > payload.length)
                        {
                            throw new IOException("Machine-dependent payload overruns track " + trackIndex);
                        }
                        offset += length;
                        events.add(new SystemEvent(trackIndex, rawTick, command, -1, -1, -1));
                        continue;
                    }

                    if (offset >= payload.length)
                    {
                        throw new IOException("Truncated system event value in track " + trackIndex);
                    }

                    int value = payload[offset++] & 0xFF;
                    if (command == 0xDC)
                    {
                        // NOP type 2 extends the next delta by supplying its high byte.
                        pendingExtendedDelta = value << 8;
                    }
                    int part = (command >= 0xE0 && command <= 0xEF) ? ((value >> 6) & 0x03) : -1;
                    int timebase = (command >= 0xC0 && command <= 0xCF) ? timebaseFor(command & 0x0F) : -1;
                    events.add(new SystemEvent(trackIndex, rawTick, command, value, part, timebase));
                    if (command == 0xDF)
                    {
                        break;
                    }
                    continue;
                }

                // Any remaining status is a note: the high two bits select the voice and
                // the low six bits are the pitch. The first extra byte adds velocity and octave.
                if (offset >= payload.length)
                {
                    throw new IOException("Truncated note event in track " + trackIndex);
                }

                int gate = payload[offset++] & 0xFF;
                int velocity = 63;
                int octaveShift = 0;
                if (noteExtraBytes > 0)
                {
                    if (offset >= payload.length)
                    {
                        throw new IOException("Truncated note attributes in track " + trackIndex);
                    }
                    int attr = payload[offset++] & 0xFF;
                    velocity = (attr >> 2) & 0x3F;
                    octaveShift = attr & 0x03;
                    int skip = noteExtraBytes - 1;
                    if (offset + skip > payload.length)
                    {
                        throw new IOException("Truncated note extra bytes in track " + trackIndex);
                    }
                    offset += skip;
                }

                events.add(new NoteEvent(
                        trackIndex,
                        rawTick,
                        (status >> 6) & 0x03,
                        status & 0x3F,
                        gate,
                        velocity,
                        octaveShift,
                        noteExtraBytes > 0));
            }

            return events;
        }

        private static int bodyLengthForResourceCommand(int command, int exstSize)
        {
            switch (command)
            {
                case 0x80:
                case 0x81:
                case 0x90:
                    // PCM resource commands use one packed part/value byte.
                    return 1;
                default:
                    if (command < 0x80)
                    {
                        // Short EXTA-style resource bodies include the configured EXST bytes.
                        return 1 + Math.max(0, exstSize);
                    }
                    return 1;
            }
        }

        private static ScheduledExecution scheduleEvents(
                List<List<TrackEvent>> decodedTracks,
                int effectiveTrackCount,
                List<String> warnings,
                PlaybackTimeline timeline)
        {
            // A DD loop saves and restores every track, not just track 0 where its command lives.
            TrackCursor[] cursors = new TrackCursor[decodedTracks.size()];
            for (int i = 0; i < decodedTracks.size(); i++)
            {
                cursors[i] = new TrackCursor(i, decodedTracks.get(i));
            }

            LoopSlotState[] slots = new LoopSlotState[4];
            for (int i = 0; i < slots.length; i++)
            {
                slots[i] = new LoopSlotState();
            }

            List<TrackEvent> executed = new ArrayList<TrackEvent>();
            List<Integer> rewindRawTicks = new ArrayList<Integer>();
            List<Integer> rewindSourceRawTicks = new ArrayList<Integer>();
            int sourceEndRawTick = sourceEndRawTick(decodedTracks);
            // Remember where repeated parser positions were first seen once an endless DD loop starts.
            Map<String, long[]> seenSchedulerStates = null;
            long currentTick = 0L;
            int displayEndRawTick = -1;

            while (true)
            {
                int currentRawTick = checkedRawTick(currentTick);
                if (seenSchedulerStates != null)
                {
                    String key = schedulerStateKey(cursors, slots, currentTick);
                    long[] previous = seenSchedulerStates.get(key);
                    if (previous != null)
                    {
                        if (currentTick <= previous[0])
                        {
                            throw new IllegalStateException("Native DD scheduler repeated without advancing time.");
                        }

                        LoopSlotState infinite = null;
                        for (int i = 0; i < slots.length; i++)
                        {
                            if (slots[i].active && slots[i].remaining < 0)
                            {
                                infinite = slots[i];
                                break;
                            }
                        }
                        if (infinite == null)
                        {
                            throw new IllegalStateException("Native DD scheduler repeated without an active infinite loop.");
                        }

                        warnings.add("Native DD parser cycle detected at raw tick " + previous[0] + " to " + currentTick + ".");
                        ScheduledExecution execution = stabilizeInfiniteLoop(
                                executed,
                                (int) previous[1],
                                checkedRawTick(previous[0]),
                                currentRawTick,
                                effectiveTrackCount,
                                warnings);
                        timeline.setSourceProgress(
                                rewindRawTicks,
                                rewindSourceRawTicks,
                                infinite.sourceLoopEndSourceRawTick,
                                checkedRawTick(previous[0]),
                                currentRawTick,
                                execution.loopStartRawTick,
                                displayEndRawTick);
                        return execution;
                    }
                    seenSchedulerStates.put(key, new long[] { currentTick, executed.size() });
                }

                int nextCursor = nextTrackCursor(cursors);
                if (nextCursor < 0)
                {
                    timeline.setSourceProgress(rewindRawTicks, rewindSourceRawTicks, sourceEndRawTick, -1, -1, -1, -1);
                    return new ScheduledExecution(executed, -1, -1, checkedRawTick(currentTick));
                }

                TrackCursor cursor = cursors[nextCursor];
                currentTick = cursor.dueTick;
                int eventRawTick = checkedRawTick(currentTick);
                TrackEvent source = cursor.current();
                TrackEvent event = copyTrackEventAt(source, eventRawTick);
                executed.add(event);

                if (event instanceof SystemEvent)
                {
                    SystemEvent systemEvent = (SystemEvent) event;
                    if (systemEvent.command == 0xDD && systemEvent.trackIndex == 0)
                    {
                        int slot = (systemEvent.value >> 6) & 0x03;
                        int operation = systemEvent.value & 0x03;
                        if (operation == 0)
                        {
                            // Save the position after the start command so a rewind does not run it again.
                            advanceTrackCursor(cursor, currentTick);
                            slots[slot].capture(cursors, currentTick, source.rawTick);
                            continue;
                        }
                        if (operation == 1 && slots[slot].valid)
                        {
                            LoopSlotState state = slots[slot];
                            if (state.startRawTick == currentTick)
                            {
                                // The native player treats a zero-length DD loop as the end of all tracks.
                                warnings.add("Zero-duration DD loop end marks every native track parser context done at raw tick " + currentTick + ".");
                                for (int i = 0; i < cursors.length; i++)
                                {
                                    cursors[i].finish();
                                }
                                timeline.setSourceProgress(rewindRawTicks, rewindSourceRawTicks, sourceEndRawTick, -1, -1, -1, -1);
                                return new ScheduledExecution(executed, -1, -1, checkedRawTick(currentTick));
                            }

                            if (!state.active)
                            {
                                // The repeat nibble counts extra passes; zero means repeat forever.
                                int repeat = (systemEvent.value >> 2) & 0x0F;
                                state.remaining = repeat == 0 ? -1 : repeat;
                                state.active = true;
                                if (repeat == 0)
                                {
                                    state.sourceLoopEndSourceRawTick = source.rawTick;
                                    displayEndRawTick = eventRawTick;
                                    if (seenSchedulerStates == null)
                                    {
                                        // Cycle detection is needed only after an infinite DD loop starts.
                                        seenSchedulerStates = new LinkedHashMap<String, long[]>();
                                    }
                                }
                            }
                            else if (state.remaining > 0)
                            {
                                state.remaining--;
                            }

                            if (state.remaining == 0)
                            {
                                state.active = false;
                                state.valid = false;
                                advanceTrackCursor(cursor, currentTick);
                            }
                            else
                            {
                                rewindRawTicks.add(Integer.valueOf(eventRawTick));
                                rewindSourceRawTicks.add(Integer.valueOf(state.sourceStartRawTick));
                                state.restore(cursors, currentTick);
                            }
                            continue;
                        }
                    }
                }

                advanceTrackCursor(cursor, currentTick);
            }
        }

        private static int sourceEndRawTick(List<List<TrackEvent>> decodedTracks)
        {
            int endRawTick = 0;
            for (int i = 0; i < decodedTracks.size(); i++)
            {
                List<TrackEvent> track = decodedTracks.get(i);
                if (!track.isEmpty())
                {
                    endRawTick = Math.max(endRawTick, track.get(track.size() - 1).rawTick);
                }
            }
            return endRawTick;
        }

        private static ScheduledExecution stabilizeInfiniteLoop(
                List<TrackEvent> executed,
                int repeatStartIndex,
                int parserCycleStartRawTick,
                int parserCycleEndRawTick,
                int effectiveTrackCount,
                List<String> warnings)
        {
            int cycleRawTicks = parserCycleEndRawTick - parserCycleStartRawTick;
            if (cycleRawTicks <= 0)
            {
                throw new IllegalStateException("Native DD parser cycle must advance raw time.");
            }

            // The same parser position only tells us which events repeat. Replay the cycle
            // until tempo, channels, and active notes also return to the same state.
            LoopSimulationState state = new LoopSimulationState(effectiveTrackCount);
            for (int i = 0; i < repeatStartIndex; i++)
            {
                state.process(executed.get(i));
            }
            state.flushExpired(parserCycleStartRawTick);

            int repeatEndIndex = executed.size();
            Map<String, Integer> seenSemanticStates = new LinkedHashMap<String, Integer>();
            seenSemanticStates.put(state.semanticStateKey(parserCycleStartRawTick), Integer.valueOf(parserCycleStartRawTick));
            for (int i = repeatStartIndex; i < repeatEndIndex; i++)
            {
                state.process(executed.get(i));
            }

            int boundary = parserCycleEndRawTick;
            state.flushExpired(boundary);
            // Only then can the MIDI sequencer loop without changing the next pass.
            while (true)
            {
                String stateKey = state.semanticStateKey(boundary);
                Integer previous = seenSemanticStates.get(stateKey);
                if (previous != null)
                {
                    warnings.add("Native DD semantic loop stabilized at raw tick " + previous + " to " + boundary + ".");
                    return new ScheduledExecution(executed, previous.intValue(), boundary, boundary);
                }
                seenSemanticStates.put(stateKey, Integer.valueOf(boundary));
                appendParserCycle(executed, repeatStartIndex, repeatEndIndex, parserCycleStartRawTick, boundary, state);
                boundary = checkedRawTick((long) boundary + cycleRawTicks);
                state.flushExpired(boundary);
            }
        }

        private static void appendParserCycle(
                List<TrackEvent> executed,
                int repeatStartIndex,
                int repeatEndIndex,
                int parserCycleStartRawTick,
                int cycleStartRawTick,
                LoopSimulationState state)
        {
            for (int i = repeatStartIndex; i < repeatEndIndex; i++)
            {
                TrackEvent source = executed.get(i);
                int phase = source.rawTick - parserCycleStartRawTick;
                TrackEvent event = copyTrackEventAt(source, checkedRawTick((long) cycleStartRawTick + phase));
                executed.add(event);
                state.process(event);
            }
        }

        // Ignore absolute time here: after a rewind, the same relative positions should compare equal.
        private static String schedulerStateKey(TrackCursor[] cursors, LoopSlotState[] slots, long currentTick)
        {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < cursors.length; i++)
            {
                TrackCursor cursor = cursors[i];
                result.append(cursor.index).append(',').append(cursor.done ? 1 : 0).append(',')
                        .append(cursor.done ? Long.MAX_VALUE : cursor.dueTick - currentTick).append(';');
            }
            result.append('|');
            for (int i = 0; i < slots.length; i++)
            {
                LoopSlotState slot = slots[i];
                result.append(slot.valid ? 1 : 0).append(',').append(slot.active ? 1 : 0).append(',')
                        .append(slot.remaining < 0 ? -1 : slot.remaining).append(',')
                        .append(slot.valid && slot.startRawTick == currentTick ? 1 : 0).append(':');
                if (slot.indices != null)
                {
                    for (int j = 0; j < slot.indices.length; j++)
                    {
                        result.append(slot.indices[j]).append(',')
                                .append(slot.done[j] ? 1 : 0).append(',')
                                .append(slot.remainingTicks[j]).append(';');
                    }
                }
                result.append('|');
            }
            return result.toString();
        }

        private static int nextTrackCursor(TrackCursor[] cursors)
        {
            // Tracks count down together; lower track numbers go first when events share a tick.
            int selected = -1;
            for (int i = 0; i < cursors.length; i++)
            {
                TrackCursor cursor = cursors[i];
                if (cursor.done)
                {
                    continue;
                }
                if (selected < 0
                        || cursor.dueTick < cursors[selected].dueTick
                        || (cursor.dueTick == cursors[selected].dueTick && cursor.trackIndex < cursors[selected].trackIndex))
                {
                    selected = i;
                }
            }
            return selected;
        }

        private static void advanceTrackCursor(TrackCursor cursor, long currentTick)
        {
            int previousIndex = cursor.index;
            cursor.index++;
            if (cursor.index >= cursor.events.size())
            {
                cursor.finish();
                return;
            }

            TrackEvent previous = cursor.events.get(previousIndex);
            TrackEvent next = cursor.events.get(cursor.index);
            int sourceDelta = next.rawTick - previous.rawTick;
            if (sourceDelta < 0)
            {
                throw new IllegalArgumentException("Decoded track raw ticks are not monotonic at track " + cursor.trackIndex + ".");
            }
            cursor.dueTick = checkedRawTick(currentTick + sourceDelta);
        }

        private static TrackEvent copyTrackEventAt(TrackEvent source, int rawTick)
        {
            if (source instanceof NoteEvent)
            {
                NoteEvent event = (NoteEvent) source;
                return new NoteEvent(
                        event.trackIndex,
                        rawTick,
                        event.voice,
                        event.pitch,
                        event.gate,
                        event.velocity,
                        event.octaveShift,
                        event.hasExtraByte);
            }
            if (source instanceof SystemEvent)
            {
                SystemEvent event = (SystemEvent) source;
                return new SystemEvent(
                        event.trackIndex,
                        rawTick,
                        event.command,
                        event.value,
                        event.part,
                        event.timebase);
            }
            if (source instanceof ResourceEvent)
            {
                ResourceEvent event = (ResourceEvent) source;
                return new ResourceEvent(
                        event.trackIndex,
                        rawTick,
                        event.command,
                        event.value,
                        event.part);
            }
            throw new IllegalArgumentException("Unsupported decoded event type: " + source.getClass().getName());
        }

        private static int checkedRawTick(long value)
        {
            if (value < 0L || value > Integer.MAX_VALUE)
            {
                throw new IllegalArgumentException("Native DD execution exceeds supported raw-tick range.");
            }
            return (int) value;
        }

        private static List<TempoPoint> buildTempoPoints(List<TrackEvent> orderedEvents, List<String> warnings)
        {
            // Each raw interval uses the timebase that was active when that interval began.
            // Keep every change so notes, PCM triggers, and displayed time share one clock.
            List<TempoPoint> points = new ArrayList<TempoPoint>();
            int currentTimebase = DEFAULT_TIMEBASE;
            int currentTempo = DEFAULT_TEMPO;
            int lastRawTick = 0;
            long midiTick = 0L;
            boolean foundTempo = false;

            for (int i = 0; i < orderedEvents.size(); i++)
            {
                TrackEvent event = orderedEvents.get(i);
                if (!(event instanceof SystemEvent))
                {
                    continue;
                }
                SystemEvent systemEvent = (SystemEvent) event;
                if (systemEvent.trackIndex != 0
                        || (!isTempo(systemEvent) && systemEvent.command != 0xBC && systemEvent.command != 0xBF)
                        || (systemEvent.command == 0xBC && (systemEvent.value < 0 || systemEvent.value >= 0x80)))
                {
                    continue;
                }

                if (!foundTempo && systemEvent.rawTick > 0)
                {
                    points.add(new TempoPoint(0, 0L, currentTimebase, currentTempo));
                }
                foundTempo = true;

                int deltaRaw = systemEvent.rawTick - lastRawTick;
                if (deltaRaw < 0)
                {
                    deltaRaw = 0;
                }
                midiTick += ((long) deltaRaw * MIDI_PPQ) / Math.max(1, currentTimebase);

                if (isTempo(systemEvent))
                {
                    currentTimebase = systemEvent.timebase;
                    currentTempo = clamp(MIN_TEMPO, MAX_TEMPO, systemEvent.value);
                }
                else if (systemEvent.command == 0xBC)
                {
                    currentTempo = clamp(MIN_TEMPO, MAX_TEMPO, currentTempo + systemEvent.value - 0x40);
                }
                else if (systemEvent.command == 0xBF)
                {
                    currentTempo = DEFAULT_TEMPO;
                }

                TempoPoint point = new TempoPoint(
                        systemEvent.rawTick,
                        midiTick,
                        currentTimebase,
                        currentTempo);
                // With no time between writes at the same tick, only the last value affects what follows.
                if (!points.isEmpty() && points.get(points.size() - 1).rawTick == systemEvent.rawTick)
                {
                    points.set(points.size() - 1, point);
                }
                else
                {
                    points.add(point);
                }
                lastRawTick = systemEvent.rawTick;
            }

            if (!foundTempo)
            {
                warnings.add("No tempo event observed; inserting native default 125 BPM / timebase 48 point.");
                points.add(new TempoPoint(0, 0L, DEFAULT_TIMEBASE, DEFAULT_TEMPO));
            }
            return points;
        }

        private static void extractPcmTriggers(
                List<List<TrackEvent>> decodedTracks,
                List<TempoPoint> tempoPoints,
                Map<Integer, Integer> pcmPositions,
                Map<Integer, Integer> pcmVelocities,
                List<String> warnings)
        {
            for (int i = 0; i < decodedTracks.size(); i++)
            {
                List<TrackEvent> track = decodedTracks.get(i);
                for (int j = 0; j < track.size(); j++)
                {
                    TrackEvent event = track.get(j);
                    if (!(event instanceof ResourceEvent))
                    {
                        continue;
                    }
                    ResourceEvent resourceEvent = (ResourceEvent) event;
                    if (resourceEvent.command == 0x80)
                    {
                        // PCM playback still consumes millisecond keys, but timing comes from the unified tempo map.
                        int position = rawToMilliseconds(resourceEvent.rawTick, tempoPoints);
                        int channel = partLaneIndex(resourceEvent.trackIndex, resourceEvent.part);
                        int velocity = clamp(0, 127, (resourceEvent.value & 0x3F) * 2);
                        pcmPositions.put(Integer.valueOf(position), Integer.valueOf(channel));
                        pcmVelocities.put(Integer.valueOf(position), Integer.valueOf(velocity));
                    }
                    else if (resourceEvent.command == 0x81)
                    {
                        warnings.add("Unsupported audio channel panpot (PCM) event at raw tick " + resourceEvent.rawTick + ".");
                    }
                    else if (resourceEvent.command == 0x90)
                    {
                        warnings.add("Unsupported 3D positioning (PCM) event at raw tick " + resourceEvent.rawTick + ".");
                    }
                }
            }
        }

        private static int rawToMilliseconds(int rawTick, List<TempoPoint> tempoPoints)
        {
            // Use the same tempo/timebase breakpoints as MIDI rendering to avoid PCM drift.
            TempoPoint current = tempoPoints.get(0);
            long micros = 0L;
            int currentRawTick = 0;
            for (int i = 0; i < tempoPoints.size(); i++)
            {
                TempoPoint point = tempoPoints.get(i);
                if (point.rawTick > rawTick)
                {
                    break;
                }
                long deltaRaw = point.rawTick - currentRawTick;
                micros += (deltaRaw * 60000000L) / Math.max(1, current.tempo * current.timebase);
                current = point;
                currentRawTick = point.rawTick;
            }
            long remainingRaw = rawTick - currentRawTick;
            micros += (remainingRaw * 60000000L) / Math.max(1, current.tempo * current.timebase);
            return (int) ((micros + 500L) / 1000L);
        }

        private static long handleNoteEvent(NoteEvent noteEvent, List<TempoPoint> tempoPoints, List<String> warnings, RenderState renderState)
        {
            int partLane = partLaneIndex(noteEvent.trackIndex, noteEvent.voice);
            int logicalChannel = resolvePartChannel(renderState.partChannelMap, partLane);
            if (logicalChannel < 0 || logicalChannel >= MAX_LOGICAL_CHANNELS)
            {
                warnings.add("Skipping note mapped outside logical-channel range: track=" + noteEvent.trackIndex + " voice=" + noteEvent.voice + " -> " + logicalChannel);
                return -1L;
            }
            if (logicalChannel >= renderState.channels.length)
            {
                warnings.add("Skipping note mapped outside channel state range: " + logicalChannel);
                return -1L;
            }

            ChannelState channel = renderState.channels[logicalChannel];
            boolean sounding = channel.allowsOrdinaryNoteOn() && logicalChannel < MIDI_CHANNEL_COUNT;
            long midiStartTick = rawToMidiTick(tempoPoints, noteEvent.rawTick);
            if (sounding)
            {
                renderState.activeOutputMask |= (1 << logicalChannel);
                emitPatchIfNeeded(renderState.controlCollector, channel, logicalChannel, midiStartTick);
            }
            else if (logicalChannel >= MIDI_CHANNEL_COUNT)
            {
                warnings.add("Skipping note mapped to logical channel " + logicalChannel + " because the host exposes only 16 MIDI channels.");
            }

            int pitchOffset = noteEvent.pitch + octaveOffset(noteEvent.octaveShift);
            int nativeNote = baseMidiNoteForMode(channel.mode) + pitchOffset;
            int noteBase = sounding && logicalChannel == MIDI_DRUM_CHANNEL ? 35 : baseMidiNoteForMode(channel.mode);
            int midiNote = clamp(0, 127, noteBase + pitchOffset);
            int velocity = noteEvent.hasExtraByte ? clamp(1, 127, noteEvent.velocity * 2) : 126;
            int rawEndTick = noteEvent.rawTick + noteEvent.gate;
            long midiEndTick = rawToMidiTick(tempoPoints, rawEndTick);
            // Gate identity follows the native pitch, even when the drum channel emits a
            // different MIDI note. A matching active note is extended instead of retriggered.
            Integer activeKey = Integer.valueOf((logicalChannel << 8) | (nativeNote & 0xFF));
            ActiveNote active = renderState.activeNotes.get(activeKey);
            if (active != null)
            {
                active.rawEndTick = rawEndTick;
                active.midiEndTick = midiEndTick;
                if (active.sounding)
                {
                    return normalizeMidiEnd(active.midiStartTick, midiEndTick);
                }
                return active.midiChannel < MIDI_CHANNEL_COUNT ? midiEndTick : -1L;
            }

            int order = renderState.controlCollector.allocateOrder();
            if (sounding)
            {
                renderState.messageEvents.add(MessageEvent.noteOn(logicalChannel, midiStartTick, midiNote, velocity, order));
            }
            renderState.activeNotes.put(activeKey, new ActiveNote(logicalChannel, midiNote, rawEndTick, midiEndTick, order, midiStartTick, sounding));
            if (sounding)
            {
                return normalizeMidiEnd(midiStartTick, midiEndTick);
            }
            return logicalChannel < MIDI_CHANNEL_COUNT ? midiEndTick : -1L;
        }

        private static long handleSystemEvent(SystemEvent systemEvent, List<TempoPoint> tempoPoints, List<String> warnings, RenderState renderState)
        {
            if (isTempo(systemEvent))
            {
                return -1L;
            }

            long midiTick = rawToMidiTick(tempoPoints, systemEvent.rawTick);
            switch (systemEvent.command)
            {
                case 0xB0:
                    if (acceptTrackZero7Bit(systemEvent))
                    {
                        renderState.masterVolume = systemEvent.value;
                        renderState.controlCollector.emitMasterVolume(midiTick, renderState.masterVolume);
                    }
                    break;
                case 0xB1:
                    if (acceptTrackZero7Bit(systemEvent))
                    {
                        renderState.controlCollector.emitMasterPan(midiTick, systemEvent.value);
                    }
                    break;
                case 0xBA:
                    if (acceptTrackZero7Bit(systemEvent))
                    {
                        int logicalChannel = (systemEvent.value >> 3) & 0x0F;
                        ChannelState channel = renderState.channels[logicalChannel];
                        channel.mode = systemEvent.value & 0x07;
                        if (channel.mode == 1)
                        {
                            applyNativePatchHelperState(channel);
                            channel.patchDirty = true;
                            emitPatchIfNeeded(renderState.controlCollector, channel, logicalChannel, midiTick);
                        }
                    }
                    break;
                case 0xBD:
                    if (acceptTrackZero7Bit(systemEvent))
                    {
                        renderState.masterVolume = clamp(0, 127, renderState.masterVolume + systemEvent.value - 0x40);
                        renderState.controlCollector.emitMasterVolume(midiTick, renderState.masterVolume);
                    }
                    break;
                case 0xBE:
                    return applyGlobalStop(systemEvent, midiTick, renderState, warnings);
                case 0xBF:
                    return systemEvent.trackIndex == 0 ? applySessionReset(midiTick, renderState) : midiTick;
                default:
                    applyChannelSystemEvent(systemEvent, midiTick, renderState, warnings);
                    break;
            }
            return midiTick;
        }

        private static long applyGlobalStop(SystemEvent systemEvent, long midiTick, RenderState renderState, List<String> warnings)
        {
            if (systemEvent.trackIndex != 0)
            {
                return midiTick;
            }
            if (systemEvent.value != 0)
            {
                warnings.add("Ignoring 0xBE STOP with nonzero value " + systemEvent.value + " at raw tick " + systemEvent.rawTick + ".");
                return midiTick;
            }

            Iterator<Map.Entry<Integer, ActiveNote>> iterator = renderState.activeNotes.entrySet().iterator();
            while (iterator.hasNext())
            {
                ActiveNote active = iterator.next().getValue();
                if (active.sounding)
                {
                    renderState.messageEvents.add(MessageEvent.noteOff(active.midiChannel, midiTick, active.midiNote, renderState.controlCollector.allocateOrder()));
                }
                iterator.remove();
            }

            renderState.controlCollector.emitAllSoundOff(midiTick);
            recordStopTick(renderState.stopTicks, midiTick);
            return midiTick;
        }

        private static long applySessionReset(long midiTick, RenderState renderState)
        {
            Iterator<Map.Entry<Integer, ActiveNote>> iterator = renderState.activeNotes.entrySet().iterator();
            while (iterator.hasNext())
            {
                ActiveNote active = iterator.next().getValue();
                if (active.sounding)
                {
                    renderState.messageEvents.add(MessageEvent.noteOff(active.midiChannel, midiTick, active.midiNote, renderState.controlCollector.allocateOrder()));
                }
                iterator.remove();
            }

            renderState.controlCollector.emitAllSoundOff(midiTick);
            recordStopTick(renderState.stopTicks, midiTick);
            resetChannelStates(renderState.channels);
            resetPartChannelMap(renderState.partChannelMap);
            renderState.masterVolume = DEFAULT_MASTER_VOLUME;
            renderState.controlCollector.resetCaches();
            emitInitialMidiDefaults(renderState.controlCollector, renderState.channels, midiTick);
            return midiTick;
        }

        private static void applyChannelSystemEvent(SystemEvent systemEvent, long midiTick, RenderState renderState, List<String> warnings)
        {
            if (systemEvent.command == 0xE5)
            {
                applyVoiceAssignment(systemEvent, renderState.partChannelMap);
                return;
            }

            if (systemEvent.part < 0)
            {
                return;
            }
            int partLane = partLaneIndex(systemEvent.trackIndex, systemEvent.part);
            int logicalChannel = resolvePartChannel(renderState.partChannelMap, partLane);
            if (logicalChannel < 0 || logicalChannel >= renderState.channels.length)
            {
                warnings.add("Skipping control mapped outside logical-channel range: track=" + systemEvent.trackIndex + " part=" + systemEvent.part + " -> " + logicalChannel);
                return;
            }

            ChannelState channel = renderState.channels[logicalChannel];
            ControlCollector controlCollector = renderState.controlCollector;
            boolean hasMidiChannel = logicalChannel < MIDI_CHANNEL_COUNT;
            applyChannelSemanticState(systemEvent, channel);
            switch (systemEvent.command)
            {
                case 0xE0:
                    channel.patchDirty = true;
                    emitPatchIfNeeded(controlCollector, channel, logicalChannel, midiTick);
                    break;
                case 0xE1:
                    if (channel.mode == 1)
                    {
                        channel.patchDirty = true;
                        if (channel.hasProgramEvent)
                        {
                            emitPatchIfNeeded(controlCollector, channel, logicalChannel, midiTick);
                        }
                    }
                    break;
                case 0xE2:
                case 0xE6:
                    if (hasMidiChannel) { controlCollector.emitVolume(logicalChannel, midiTick, toMidiVolume(channel)); }
                    break;
                case 0xE3:
                    if (hasMidiChannel) { controlCollector.emitPan(logicalChannel, midiTick, toMidiPan(channel)); }
                    break;
                case 0xE4:
                    if (hasMidiChannel)
                    {
                        if (channel.pitchRangeDirty)
                        {
                            controlCollector.emitPitchRange(logicalChannel, midiTick, channel.pitchRange);
                            channel.pitchRangeDirty = false;
                        }
                        controlCollector.emitPitchBend(logicalChannel, midiTick, computePitchBend(channel));
                    }
                    break;
                case 0xE7:
                    if ((systemEvent.value & 0x3F) <= 24) { channel.pitchRangeDirty = true; }
                    break;
                case 0xE8:
                    if (hasMidiChannel)
                    {
                        if (channel.pitchRangeDirty)
                        {
                            controlCollector.emitPitchRange(logicalChannel, midiTick, channel.pitchRange);
                            channel.pitchRangeDirty = false;
                        }
                        controlCollector.emitPitchBend(logicalChannel, midiTick, computePitchBend(channel));
                    }
                    break;
                case 0xEA:
                    if (hasMidiChannel) { controlCollector.emitModulation(logicalChannel, midiTick, channel.modulation * 2); }
                    break;
                default:
                    break;
            }
        }

        private static void applyVoiceAssignment(SystemEvent systemEvent, int[] partChannelMap)
        {
            if (systemEvent.part < 0)
            {
                return;
            }
            int partLane = partLaneIndex(systemEvent.trackIndex, systemEvent.part);
            if (partLane >= 0 && partLane < partChannelMap.length)
            {
                // E5 may select any of 64 logical channels. Values above 15 keep state but
                // cannot produce sound through a standard MIDI output.
                partChannelMap[partLane] = systemEvent.value & 0x3F;
            }
        }

        private static void applyNativePatchHelperState(ChannelState channel)
        {
            channel.noteOnSuppressed = channel.mode != 0 && channel.mode != 1;
        }

        private static void applyChannelSemanticState(SystemEvent event, ChannelState channel)
        {
            switch (event.command)
            {
                case 0xE0:
                    channel.program = event.value & 0x3F;
                    channel.hasProgramEvent = true;
                    applyNativePatchHelperState(channel);
                    break;
                case 0xE1:
                    channel.bank = event.value & 0x3F;
                    if (channel.mode == 1) { applyNativePatchHelperState(channel); }
                    break;
                case 0xE2:
                    channel.level = event.value & 0x3F;
                    break;
                case 0xE3:
                    channel.pan = event.value & 0x3F;
                    break;
                case 0xE4:
                    channel.pitchCoarse = event.value & 0x3F;
                    break;
                case 0xE6:
                    channel.level = clamp(0, 63, channel.level + ((event.value & 0x3F) - 32));
                    break;
                case 0xE7:
                    if ((event.value & 0x3F) <= 24) { channel.pitchRange = event.value & 0x3F; }
                    break;
                case 0xE8:
                case 0xE9:
                    channel.pitchFine = event.value & 0x3F;
                    break;
                case 0xEA:
                    channel.modulation = event.value & 0x3F;
                    break;
                default:
                    break;
            }
        }

        private static void emitInitialMidiDefaults(ControlCollector controlCollector, ChannelState[] channels, long midiTick)
        {
            for (int midiChannel = 0; midiChannel < MIDI_CHANNEL_COUNT; midiChannel++)
            {
                ChannelState channel = channels[midiChannel];
                controlCollector.emitVolume(midiChannel, midiTick, toMidiVolume(channel));
                controlCollector.emitPan(midiChannel, midiTick, toMidiPan(channel));
                controlCollector.emitPitchRange(midiChannel, midiTick, channel.pitchRange);
                controlCollector.emitPitchBend(midiChannel, midiTick, computePitchBend(channel));
                controlCollector.emitModulation(midiChannel, midiTick, channel.modulation * 2);
            }
        }

        private static void emitPatchIfNeeded(ControlCollector controlCollector, ChannelState channel, int midiChannel, long midiTick)
        {
            if (midiChannel < 0 || midiChannel >= MIDI_CHANNEL_COUNT)
            {
                return;
            }
            if (channel.mode != 0 && channel.mode != 1)
            {
                return;
            }
            int hostProgram = translateHostProgram(channel);
            if (!channel.patchDirty && channel.lastProgram >= 0)
            {
                return;
            }
            if (channel.lastProgram == hostProgram)
            {
                channel.patchDirty = false;
                return;
            }

            controlCollector.emitProgramChange(midiChannel, midiTick, hostProgram);
            channel.patchDirty = false;
            channel.lastProgram = hostProgram;
        }

        private static int translateHostProgram(ChannelState channel)
        {
            int program = channel.program & 0x3F;
            int bank = channel.bank & 0x3F;
            if ((bank & 0x3E) == 0)
            {
                switch (program)
                {
                    case 0: return 0;
                    case 1: return 9;
                    case 2: return 16;
                    case 3: return 24;
                    case 4: return 13;
                    case 5: return 74;
                    default: break;
                }
            }
            return (program | (bank << 6)) & 0x7F;
        }

        private static void emitTempoTrack(List<TempoPoint> tempoPoints, Track conductorTrack, long contentEndTick)
                throws InvalidMidiDataException
        {
            TempoPoint active = tempoPoints.get(0);
            addTempoMeta(conductorTrack, 60000000 / Math.max(1, active.tempo), 0L);
            for (int i = 0; i < tempoPoints.size(); i++)
            {
                TempoPoint point = tempoPoints.get(i);
                if (point.midiTick <= 0L || point.midiTick > contentEndTick)
                {
                    continue;
                }
                addTempoMeta(conductorTrack, 60000000 / Math.max(1, point.tempo), point.midiTick);
            }
        }

        private static long flushExpiredNotes(int currentRawTick, Map<Integer, ActiveNote> activeNotes, List<MessageEvent> messageEvents)
        {
            // Native gates expire before another event at the same raw tick is handled.
            long maxTick = -1L;
            Iterator<Map.Entry<Integer, ActiveNote>> iterator = activeNotes.entrySet().iterator();
            while (iterator.hasNext())
            {
                ActiveNote active = iterator.next().getValue();
                if (active.rawEndTick > currentRawTick)
                {
                    continue;
                }
                if (active.sounding)
                {
                    long midiEndTick = normalizeMidiEnd(active.midiStartTick, active.midiEndTick);
                    messageEvents.add(MessageEvent.noteOff(active.midiChannel, midiEndTick, active.midiNote, active.order));
                    maxTick = Math.max(maxTick, midiEndTick);
                }
                else if (active.midiChannel < MIDI_CHANNEL_COUNT)
                {
                    maxTick = Math.max(maxTick, active.midiEndTick);
                }
                iterator.remove();
            }
            return maxTick;
        }

        private static void addStopMarkers(Track conductorTrack, List<Long> stopTicks)
                throws InvalidMidiDataException
        {
            for (int i = 0; i < stopTicks.size(); i++)
            {
                long tick = stopTicks.get(i);
                // 0xBE must also reach the player so active PCM clips are stopped.
                byte[] data = MLDSequenceMarker.encodeStopMarker(tick);
                MetaMessage meta = new MetaMessage();
                meta.setMessage(MLDSequenceMarker.META_MARKER_TYPE, data, data.length);
                conductorTrack.add(new MidiEvent(meta, tick));
            }
        }

        private static void addTrackName(Track track, String name) throws InvalidMidiDataException
        {
            MetaMessage meta = new MetaMessage();
            byte[] data = name.getBytes();
            meta.setMessage(0x03, data, data.length);
            track.add(new MidiEvent(meta, 0L));
        }

        private static void addTempoMeta(Track track, int mpqn, long tick) throws InvalidMidiDataException
        {
            byte[] data = new byte[] {
                    (byte) ((mpqn >>> 16) & 0xFF),
                    (byte) ((mpqn >>> 8) & 0xFF),
                    (byte) (mpqn & 0xFF)
            };
            MetaMessage meta = new MetaMessage();
            meta.setMessage(0x51, data, data.length);
            track.add(new MidiEvent(meta, tick));
        }

        private static void addLoopMarker(Track track, long loopStartTick, long loopEndTick, int repeatCount)
                throws InvalidMidiDataException
        {
            byte[] data = MLDSequenceMarker.encodeLoopMarker(loopStartTick, loopEndTick, repeatCount);
            MetaMessage meta = new MetaMessage();
            meta.setMessage(MLDSequenceMarker.META_MARKER_TYPE, data, data.length);
            track.add(new MidiEvent(meta, 0L));
        }

        private static void addShortMessage(Track track, int status, int channel, int data1, int data2, long tick)
                throws InvalidMidiDataException
        {
            ShortMessage message = new ShortMessage();
            message.setMessage(status, channel, data1, data2);
            track.add(new MidiEvent(message, tick));
        }

        private static void addEndOfTrack(Track track, long tick) throws InvalidMidiDataException
        {
            MetaMessage meta = new MetaMessage();
            meta.setMessage(0x2F, new byte[0], 0);
            track.add(new MidiEvent(meta, tick));
        }

        private static ChannelState[] createChannelStates()
        {
            ChannelState[] channels = new ChannelState[MAX_LOGICAL_CHANNELS];
            for (int i = 0; i < channels.length; i++)
            {
                channels[i] = new ChannelState();
            }
            return channels;
        }

        private static void resetChannelStates(ChannelState[] channels)
        {
            for (int i = 0; i < channels.length; i++)
            {
                channels[i].reset();
            }
        }

        private static int[] createIdentityPartChannelMap(int length)
        {
            int[] map = new int[length];
            resetPartChannelMap(map);
            return map;
        }

        private static void resetPartChannelMap(int[] map)
        {
            for (int i = 0; i < map.length; i++)
            {
                map[i] = i;
            }
        }

        private static int resolvePartChannel(int[] partChannelMap, int partLane)
        {
            if (partLane < 0)
            {
                return -1;
            }
            if (partLane >= partChannelMap.length)
            {
                return partLane;
            }
            return partChannelMap[partLane];
        }

        private static boolean isTempo(SystemEvent systemEvent)
        {
            return systemEvent.command >= 0xC0 && systemEvent.command <= 0xCF && systemEvent.timebase > 0;
        }

        private static boolean acceptTrackZero7Bit(SystemEvent systemEvent)
        {
            return systemEvent.trackIndex == 0 && systemEvent.value >= 0 && systemEvent.value < 0x80;
        }

        private static int partLaneIndex(int trackIndex, int voice)
        {
            return (trackIndex * 4) + voice;
        }

        private static int[] buildOutputChannelMap(int activeOutputMask)
        {
            // Keep MIDI channel 10 for drums and pack the active melodic channels around it.
            int[] outputChannelMap = createIdentityPartChannelMap(MIDI_CHANNEL_COUNT);
            int nextMelodicChannel = 0;
            for (int logicalChannel = 0; logicalChannel < MIDI_CHANNEL_COUNT; logicalChannel++)
            {
                if (((activeOutputMask >>> logicalChannel) & 1) == 0)
                {
                    continue;
                }
                if (logicalChannel == MIDI_DRUM_CHANNEL)
                {
                    outputChannelMap[logicalChannel] = MIDI_DRUM_CHANNEL;
                    continue;
                }
                outputChannelMap[logicalChannel] = nextMelodicChannel;
                if (nextMelodicChannel == MIDI_DRUM_CHANNEL - 1)
                {
                    nextMelodicChannel += 2;
                }
                else if (nextMelodicChannel < MIDI_CHANNEL_COUNT - 1)
                {
                    nextMelodicChannel++;
                }
            }
            return outputChannelMap;
        }

        private static int timebaseFor(int selector)
        {
            switch (selector)
            {
                case 0x0: return 6;
                case 0x1: return 12;
                case 0x2: return 24;
                case 0x3: return 48;
                case 0x4: return 96;
                case 0x5: return 192;
                case 0x6: return 384;
                case 0x8: return 15;
                case 0x9: return 30;
                case 0xA: return 60;
                case 0xB: return 120;
                case 0xC: return 240;
                case 0xD: return 480;
                case 0xE: return 960;
                default: return -1;
            }
        }

        private static int baseMidiNoteForMode(int mode)
        {
            return mode == 1 ? 35 : 45;
        }

        private static int octaveOffset(int octaveShift)
        {
            if (octaveShift < 0 || octaveShift >= OCTAVE_TABLE.length) { return 0; }
            return OCTAVE_TABLE[octaveShift];
        }

        private static int toMidiVolume(ChannelState channel)
        {
            return clamp(0, 127, channel.level * 2);
        }

        private static int toMidiPan(ChannelState channel)
        {
            return clamp(0, 127, channel.pan * 2);
        }

        private static int computePitchBend(ChannelState channel)
        {
            return clamp(0, 16383, (8 * (channel.pitchFine + (32 * channel.pitchCoarse))) - 256);
        }

        private static long normalizeMidiEnd(long midiStartTick, long midiEndTick)
        {
            return midiEndTick <= midiStartTick ? (midiStartTick + 1L) : midiEndTick;
        }

        private static int clamp(int min, int max, int value)
        {
            return Math.max(min, Math.min(max, value));
        }

        private static int readBe16(byte[] data, int offset)
        {
            return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
        }

        private static void recordStopTick(List<Long> stopTicks, long tick)
        {
            if (!stopTicks.isEmpty() && stopTicks.get(stopTicks.size() - 1).longValue() == tick)
            {
                return;
            }
            stopTicks.add(Long.valueOf(tick));
        }

        private static final class RenderState
        {
            final List<MessageEvent> messageEvents;
            final ControlCollector controlCollector;
            final ChannelState[] channels;
            final int[] partChannelMap;
            int activeOutputMask = 0;
            final Map<Integer, ActiveNote> activeNotes;
            final List<Long> stopTicks;
            int masterVolume = DEFAULT_MASTER_VOLUME;

            RenderState(
                    List<MessageEvent> messageEvents,
                    ControlCollector controlCollector,
                    ChannelState[] channels,
                    int[] partChannelMap,
                    Map<Integer, ActiveNote> activeNotes,
                    List<Long> stopTicks)
            {
                this.messageEvents = messageEvents;
                this.controlCollector = controlCollector;
                this.channels = channels;
                this.partChannelMap = partChannelMap;
                this.activeNotes = activeNotes;
                this.stopTicks = stopTicks;
            }
        }

        static final class DecodeResult
        {
            final Sequence sequence;
            final PlaybackTimeline playbackTimeline;
            final Map<Integer, Integer> pcmPositions;
            final Map<Integer, Integer> pcmVelocities;
            final List<String> warnings;

            DecodeResult(Sequence sequence, PlaybackTimeline playbackTimeline, Map<Integer, Integer> pcmPositions, Map<Integer, Integer> pcmVelocities, List<String> warnings)
            {
                this.sequence = sequence;
                this.playbackTimeline = playbackTimeline;
                this.pcmPositions = new HashMap<Integer, Integer>(pcmPositions);
                this.pcmVelocities = new HashMap<Integer, Integer>(pcmVelocities);
                this.warnings = new ArrayList<String>(warnings);
            }
        }

        private static final class ScheduledExecution
        {
            final List<TrackEvent> events;
            final int loopStartRawTick;
            final int loopEndRawTick;
            final int endRawTick;

            ScheduledExecution(List<TrackEvent> events, int loopStartRawTick, int loopEndRawTick, int endRawTick)
            {
                this.events = events;
                this.loopStartRawTick = loopStartRawTick;
                this.loopEndRawTick = loopEndRawTick;
                this.endRawTick = endRawTick;
            }

            boolean hasLoop()
            {
                return loopStartRawTick >= 0 && loopEndRawTick > loopStartRawTick;
            }
        }

        private static final class TrackCursor
        {
            final int trackIndex;
            final List<TrackEvent> events;
            int index;
            long dueTick;
            boolean done;

            TrackCursor(int trackIndex, List<TrackEvent> events)
            {
                this.trackIndex = trackIndex;
                this.events = events;
                this.index = 0;
                this.done = events.isEmpty();
                this.dueTick = done ? Long.MAX_VALUE : events.get(0).rawTick;
            }

            TrackEvent current()
            {
                return events.get(index);
            }

            void finish()
            {
                done = true;
                dueTick = Long.MAX_VALUE;
                index = events.size();
            }
        }

        private static final class LoopSlotState
        {
            boolean valid;
            boolean active;
            int remaining;
            long startRawTick;
            int sourceStartRawTick;
            int sourceLoopEndSourceRawTick;
            int[] indices;
            long[] remainingTicks;
            boolean[] done;

            void capture(TrackCursor[] cursors, long currentTick, int sourceRawTick)
            {
                valid = true;
                startRawTick = currentTick;
                sourceStartRawTick = sourceRawTick;
                sourceLoopEndSourceRawTick = -1;
                indices = new int[cursors.length];
                remainingTicks = new long[cursors.length];
                done = new boolean[cursors.length];
                for (int i = 0; i < cursors.length; i++)
                {
                    TrackCursor cursor = cursors[i];
                    indices[i] = cursor.index;
                    done[i] = cursor.done;
                    remainingTicks[i] = cursor.done ? Long.MAX_VALUE : Math.max(0L, cursor.dueTick - currentTick);
                }
            }

            void restore(TrackCursor[] cursors, long currentTick)
            {
                if (!valid || indices == null)
                {
                    return;
                }
                for (int i = 0; i < cursors.length; i++)
                {
                    TrackCursor cursor = cursors[i];
                    cursor.index = indices[i];
                    cursor.done = done[i];
                    cursor.dueTick = cursor.done ? Long.MAX_VALUE : currentTick + remainingTicks[i];
                }
            }
        }

        // Parser positions can repeat while tempo or channel state is still changing. Mirror
        // the audible state here so the chosen MIDI loop comes back to the same sound.
        private static final class LoopSimulationState
        {
            final ChannelState[] channels = createChannelStates();
            final int[] partChannelMap;
            final Map<Integer, LoopActiveNote> activeNotes = new LinkedHashMap<Integer, LoopActiveNote>();
            int timebase = DEFAULT_TIMEBASE;
            int tempo = DEFAULT_TEMPO;
            int masterVolume = DEFAULT_MASTER_VOLUME;

            LoopSimulationState(int effectiveTrackCount)
            {
                partChannelMap = createIdentityPartChannelMap(Math.max(MIDI_CHANNEL_COUNT, effectiveTrackCount * 4));
            }

            void process(TrackEvent event)
            {
                flushExpired(event.rawTick);
                if (event instanceof NoteEvent)
                {
                    processNote((NoteEvent) event);
                }
                else if (event instanceof SystemEvent)
                {
                    processSystem((SystemEvent) event);
                }
            }

            void flushExpired(int currentRawTick)
            {
                Iterator<Map.Entry<Integer, LoopActiveNote>> iterator = activeNotes.entrySet().iterator();
                while (iterator.hasNext())
                {
                    if (iterator.next().getValue().rawEndTick <= currentRawTick)
                    {
                        iterator.remove();
                    }
                }
            }

            // Compare this only at the end of a complete repeated pass.
            String semanticStateKey(int rawOrigin)
            {
                StringBuilder result = new StringBuilder();
                result.append(timebase).append(',').append(tempo).append(',').append(masterVolume).append('|');
                for (int i = 0; i < partChannelMap.length; i++)
                {
                    result.append(partChannelMap[i]).append(',');
                }
                result.append('|');
                for (int i = 0; i < channels.length; i++)
                {
                    ChannelState channel = channels[i];
                    result.append(channel.mode).append(',').append(channel.bank).append(',').append(channel.program).append(',')
                            .append(channel.hasProgramEvent ? 1 : 0).append(',').append(channel.level).append(',')
                            .append(channel.pan).append(',').append(channel.pitchCoarse).append(',').append(channel.pitchFine).append(',')
                            .append(channel.pitchRange).append(',').append(channel.modulation).append(',')
                            .append(channel.noteOnSuppressed ? 1 : 0).append(';');
                }
                result.append('|');
                for (Map.Entry<Integer, LoopActiveNote> entry : activeNotes.entrySet())
                {
                    LoopActiveNote note = entry.getValue();
                    result.append(entry.getKey()).append(':').append(note.rawEndTick - rawOrigin).append(',')
                            .append(note.sounding ? 1 : 0).append(',').append(note.midiNote).append(';');
                }
                return result.toString();
            }

            private void processNote(NoteEvent event)
            {
                int partLane = partLaneIndex(event.trackIndex, event.voice);
                int logicalChannel = resolvePartChannel(partChannelMap, partLane);
                if (logicalChannel < 0 || logicalChannel >= channels.length)
                {
                    return;
                }

                ChannelState channel = channels[logicalChannel];
                int pitchOffset = event.pitch + octaveOffset(event.octaveShift);
                int nativeNote = baseMidiNoteForMode(channel.mode) + pitchOffset;
                Integer key = Integer.valueOf((logicalChannel << 8) | (nativeNote & 0xFF));
                int rawEndTick = event.rawTick + event.gate;
                LoopActiveNote active = activeNotes.remove(key);
                if (active != null)
                {
                    active.rawEndTick = rawEndTick;
                    activeNotes.put(key, active);
                    return;
                }

                boolean sounding = channel.allowsOrdinaryNoteOn() && logicalChannel < MIDI_CHANNEL_COUNT;
                int noteBase = sounding && logicalChannel == MIDI_DRUM_CHANNEL ? 35 : baseMidiNoteForMode(channel.mode);
                int midiNote = clamp(0, 127, noteBase + pitchOffset);
                activeNotes.put(key, new LoopActiveNote(rawEndTick, sounding, midiNote));
            }

            private void processSystem(SystemEvent event)
            {
                if (event.trackIndex == 0)
                {
                    if (isTempo(event))
                    {
                        timebase = event.timebase;
                        tempo = clamp(MIN_TEMPO, MAX_TEMPO, event.value);
                    }
                    else if (event.command == 0xBC && event.value < 0x80)
                    {
                        tempo = clamp(MIN_TEMPO, MAX_TEMPO, tempo + event.value - 0x40);
                    }
                    else if (event.command == 0xBF)
                    {
                        tempo = DEFAULT_TEMPO;
                    }
                }

                switch (event.command)
                {
                    case 0xB0:
                        if (acceptTrackZero7Bit(event)) { masterVolume = event.value; }
                        return;
                    case 0xBD:
                        if (acceptTrackZero7Bit(event)) { masterVolume = clamp(0, 127, masterVolume + event.value - 0x40); }
                        return;
                    case 0xBA:
                        if (acceptTrackZero7Bit(event))
                        {
                            ChannelState channel = channels[(event.value >> 3) & 0x0F];
                            channel.mode = event.value & 0x07;
                            if (channel.mode == 1) { applyNativePatchHelperState(channel); }
                        }
                        return;
                    case 0xBE:
                        if (event.trackIndex == 0 && event.value == 0) { activeNotes.clear(); }
                        return;
                    case 0xBF:
                        if (event.trackIndex == 0)
                        {
                            activeNotes.clear();
                            resetChannelStates(channels);
                            resetPartChannelMap(partChannelMap);
                            masterVolume = DEFAULT_MASTER_VOLUME;
                        }
                        return;
                    case 0xE5:
                        applyVoiceAssignment(event, partChannelMap);
                        return;
                    default:
                        break;
                }

                if (event.part < 0)
                {
                    return;
                }
                int partLane = partLaneIndex(event.trackIndex, event.part);
                int logicalChannel = resolvePartChannel(partChannelMap, partLane);
                if (logicalChannel < 0 || logicalChannel >= channels.length)
                {
                    return;
                }
                applyChannelSemanticState(event, channels[logicalChannel]);
            }
        }

        private static final class LoopActiveNote
        {
            int rawEndTick;
            final boolean sounding;
            final int midiNote;

            LoopActiveNote(int rawEndTick, boolean sounding, int midiNote)
            {
                this.rawEndTick = rawEndTick;
                this.sounding = sounding;
                this.midiNote = midiNote;
            }
        }

        private static abstract class TrackEvent
        {
            final int trackIndex;
            final int rawTick;

            TrackEvent(int trackIndex, int rawTick)
            {
                this.trackIndex = trackIndex;
                this.rawTick = rawTick;
            }
        }

        private static final class NoteEvent extends TrackEvent
        {
            final int voice;
            final int pitch;
            final int gate;
            final int velocity;
            final int octaveShift;
            final boolean hasExtraByte;

            NoteEvent(int trackIndex, int rawTick, int voice, int pitch, int gate, int velocity, int octaveShift, boolean hasExtraByte)
            {
                super(trackIndex, rawTick);
                this.voice = voice;
                this.pitch = pitch;
                this.gate = gate;
                this.velocity = velocity;
                this.octaveShift = octaveShift;
                this.hasExtraByte = hasExtraByte;
            }
        }

        private static final class SystemEvent extends TrackEvent
        {
            final int command;
            final int value;
            final int part;
            final int timebase;

            SystemEvent(int trackIndex, int rawTick, int command, int value, int part, int timebase)
            {
                super(trackIndex, rawTick);
                this.command = command;
                this.value = value;
                this.part = part;
                this.timebase = timebase;
            }
        }

        private static final class ResourceEvent extends TrackEvent
        {
            final int command;
            final int value;
            final int part;

            ResourceEvent(int trackIndex, int rawTick, int command, int value, int part)
            {
                super(trackIndex, rawTick);
                this.command = command;
                this.value = value;
                this.part = part;
            }
        }

        private static final class TempoPoint
        {
            final int rawTick;
            final long midiTick;
            final int timebase;
            final int tempo;

            TempoPoint(int rawTick, long midiTick, int timebase, int tempo)
            {
                this.rawTick = rawTick;
                this.midiTick = midiTick;
                this.timebase = timebase;
                this.tempo = tempo;
            }
        }

        private static final class ChannelState
        {
            int mode = 0;
            int bank = 0;
            int program = 0;
            boolean hasProgramEvent = false;
            int level = DEFAULT_LEVEL;
            int pan = DEFAULT_PAN;
            int pitchCoarse = DEFAULT_PITCH_COARSE;
            int pitchFine = DEFAULT_PITCH_FINE;
            int pitchRange = DEFAULT_PITCH_RANGE;
            int modulation = DEFAULT_MODULATION;
            boolean patchDirty = true;
            boolean pitchRangeDirty = false;
            boolean noteOnSuppressed = false;
            int lastProgram = -1;

            boolean allowsOrdinaryNoteOn()
            {
                return !noteOnSuppressed;
            }

            void reset()
            {
                mode = 0;
                bank = 0;
                program = 0;
                hasProgramEvent = false;
                level = DEFAULT_LEVEL;
                pan = DEFAULT_PAN;
                pitchCoarse = DEFAULT_PITCH_COARSE;
                pitchFine = DEFAULT_PITCH_FINE;
                pitchRange = DEFAULT_PITCH_RANGE;
                modulation = DEFAULT_MODULATION;
                patchDirty = true;
                pitchRangeDirty = false;
                noteOnSuppressed = false;
                lastProgram = -1;
            }
        }

        private static final class ActiveNote
        {
            final int midiChannel;
            final int midiNote;
            final int order;
            final long midiStartTick;
            final boolean sounding;
            int rawEndTick;
            long midiEndTick;

            ActiveNote(int midiChannel, int midiNote, int rawEndTick, long midiEndTick, int order, long midiStartTick, boolean sounding)
            {
                this.midiChannel = midiChannel;
                this.midiNote = midiNote;
                this.rawEndTick = rawEndTick;
                this.midiEndTick = midiEndTick;
                this.order = order;
                this.midiStartTick = midiStartTick;
                this.sounding = sounding;
            }
        }

        private static final class ControlCollector
        {
            private final List<MessageEvent> messageEvents;
            private final Map<Integer, Integer> lastControlValues = new LinkedHashMap<Integer, Integer>();
            private final Map<Integer, Integer> lastPitchBendValues = new LinkedHashMap<Integer, Integer>();
            private int nextOrder = 0;

            ControlCollector(List<MessageEvent> messageEvents)
            {
                this.messageEvents = messageEvents;
            }

            int allocateOrder()
            {
                return nextOrder++;
            }

            void resetCaches()
            {
                lastControlValues.clear();
                lastPitchBendValues.clear();
            }

            void emitProgramChange(int midiChannel, long tick, int program)
            {
                emit(midiChannel, tick, ShortMessage.PROGRAM_CHANGE, clamp(0, 127, program), 0);
            }

            void emitVolume(int midiChannel, long tick, int value)
            {
                emitDedupedControl(midiChannel, tick, 7, clamp(0, 127, value));
            }

            void emitPan(int midiChannel, long tick, int value)
            {
                emitDedupedControl(midiChannel, tick, 10, clamp(0, 127, value));
            }

            void emitPitchRange(int midiChannel, long tick, int range)
            {
                emit(midiChannel, tick, ShortMessage.CONTROL_CHANGE, 101, 0);
                emit(midiChannel, tick, ShortMessage.CONTROL_CHANGE, 100, 0);
                emit(midiChannel, tick, ShortMessage.CONTROL_CHANGE, 6, clamp(0, 127, range));
                emit(midiChannel, tick, ShortMessage.CONTROL_CHANGE, 38, 0);
            }

            void emitPitchBend(int midiChannel, long tick, int bendValue)
            {
                int clamped = clamp(0, 16383, bendValue);
                Integer key = Integer.valueOf(midiChannel);
                Integer lastValue = lastPitchBendValues.get(key);
                if (lastValue != null && lastValue.intValue() == clamped)
                {
                    return;
                }
                lastPitchBendValues.put(key, Integer.valueOf(clamped));
                emit(midiChannel, tick, ShortMessage.PITCH_BEND, clamped & 0x7F, (clamped >> 7) & 0x7F);
            }

            void emitModulation(int midiChannel, long tick, int value)
            {
                emitDedupedControl(midiChannel, tick, 1, clamp(0, 127, value));
            }

            void emitMasterVolume(long tick, int value)
            {
                for (int midiChannel = 0; midiChannel < MIDI_CHANNEL_COUNT; midiChannel++)
                {
                    emitVolume(midiChannel, tick, value);
                }
            }

            void emitMasterPan(long tick, int value)
            {
                for (int midiChannel = 0; midiChannel < MIDI_CHANNEL_COUNT; midiChannel++)
                {
                    emitPan(midiChannel, tick, value);
                }
            }

            void emitAllSoundOff(long tick)
            {
                for (int midiChannel = 0; midiChannel < MIDI_CHANNEL_COUNT; midiChannel++)
                {
                    emit(midiChannel, tick, ShortMessage.CONTROL_CHANGE, 120, 0);
                }
            }

            private void emitDedupedControl(int midiChannel, long tick, int controller, int value)
            {
                Integer key = Integer.valueOf((midiChannel << 8) | (controller & 0x7F));
                Integer lastValue = lastControlValues.get(key);
                if (lastValue != null && lastValue.intValue() == value)
                {
                    return;
                }
                lastControlValues.put(key, Integer.valueOf(value));
                emit(midiChannel, tick, ShortMessage.CONTROL_CHANGE, controller, value);
            }

            private void emit(int midiChannel, long tick, int status, int data1, int data2)
            {
                messageEvents.add(MessageEvent.control(midiChannel, tick, status, data1, data2, allocateOrder()));
            }
        }

        private static final class MessageEvent
        {
            static final int PHASE_NOTE_OFF = 0;
            static final int PHASE_CONTROL = 1;
            static final int PHASE_NOTE_ON = 2;

            final int midiChannel;
            final long tick;
            final int phase;
            final int status;
            final int data1;
            final int data2;
            final int order;

            private MessageEvent(int midiChannel, long tick, int phase, int status, int data1, int data2, int order)
            {
                this.midiChannel = midiChannel;
                this.tick = tick;
                this.phase = phase;
                this.status = status;
                this.data1 = data1;
                this.data2 = data2;
                this.order = order;
            }

            static MessageEvent control(int midiChannel, long tick, int status, int data1, int data2, int order)
            {
                return new MessageEvent(midiChannel, tick, PHASE_CONTROL, status, data1, data2, order);
            }

            static MessageEvent noteOff(int midiChannel, long tick, int midiNote, int order)
            {
                return new MessageEvent(midiChannel, tick, PHASE_NOTE_OFF, ShortMessage.NOTE_OFF, midiNote, 0, order);
            }

            static MessageEvent noteOn(int midiChannel, long tick, int midiNote, int velocity, int order)
            {
                return new MessageEvent(midiChannel, tick, PHASE_NOTE_ON, ShortMessage.NOTE_ON, midiNote, velocity, order);
            }
        }

        private static long rawToMidiTick(List<TempoPoint> tempoPoints, int rawTick)
        {
            TempoPoint current = tempoPoints.get(0);
            for (int i = 1; i < tempoPoints.size(); i++)
            {
                TempoPoint next = tempoPoints.get(i);
                if (next.rawTick > rawTick)
                {
                    break;
                }
                current = next;
            }
            return current.midiTick + (((long) rawTick - current.rawTick) * MIDI_PPQ) / Math.max(1, current.timebase);
        }

        private static long midiTickToMicroseconds(List<TempoPoint> tempoPoints, long midiTick)
        {
            TempoPoint current = tempoPoints.get(0);
            long currentTick = 0L;
            long micros = 0L;
            for (int i = 1; i < tempoPoints.size(); i++)
            {
                TempoPoint next = tempoPoints.get(i);
                if (next.midiTick > midiTick)
                {
                    break;
                }
                long deltaTick = next.midiTick - currentTick;
                micros += (deltaTick * (60000000L / Math.max(1, current.tempo))) / MIDI_PPQ;
                currentTick = next.midiTick;
                current = next;
            }
            long deltaTick = midiTick - currentTick;
            return micros + (deltaTick * (60000000L / Math.max(1, current.tempo))) / MIDI_PPQ;
        }

        private static final Comparator<MessageEvent> MESSAGE_EVENT_COMPARATOR = new Comparator<MessageEvent>()
        {
            public int compare(MessageEvent left, MessageEvent right)
            {
                if (left.tick != right.tick) { return left.tick < right.tick ? -1 : 1; }
                if (left.order != right.order) { return left.order < right.order ? -1 : 1; }
                return left.phase < right.phase ? -1 : (left.phase == right.phase ? 0 : 1);
            }
        };
    }
}