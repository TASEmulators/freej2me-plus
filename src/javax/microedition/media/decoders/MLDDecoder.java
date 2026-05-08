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
            if (parts.length < 3)
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
        private static final int DEFAULT_TEMPO = 120;
        private static final int MIN_TEMPO = 20;
        private static final int MAX_TEMPO = 255;
        private static final int DEFAULT_LEVEL = 63;
        private static final int DEFAULT_PAN = 32;
        private static final int DEFAULT_PITCH_COARSE = 32;
        private static final int DEFAULT_PITCH_FINE = 32;
        private static final int DEFAULT_PITCH_RANGE = 2;
        private static final int DEFAULT_MODULATION = 0;
        private static final int[] OCTAVE_TABLE = new int[] { 0, 12, -24, -12 };
        private static final boolean DEFAULT_IDENTITY_OUTPUT_MAP = false;
        private static final boolean DEFAULT_RESERVE_DRUM_OUTPUT_CHANNEL = true;
        private static final int MIDI_DRUM_CHANNEL = 9;
        private static final int DEFAULT_SPECIAL_OUTPUT_MASK = 1 << MIDI_DRUM_CHANNEL;

        private MLDMelodyDecoder()
        {
        }

        static DecodeResult build(List<byte[]> trackPayloads, int declaredTrackCount, int noteExtraBytes, int exstSize)
                throws IOException, InvalidMidiDataException
        {
            int effectiveTrackCount = declaredTrackCount > 0 ? declaredTrackCount : trackPayloads.size();
            DecodePlan plan = prepareDecodePlan(trackPayloads, noteExtraBytes, exstSize, effectiveTrackCount);
            MidiTracks midiTracks = createMidiTracks();
            RenderState renderState = createRenderState(plan.effectiveTrackCount);
            long maxTick = renderEvents(plan, renderState);
            long contentEndTick = finalizeRender(plan, renderState, midiTracks, maxTick);

            return new DecodeResult(
                    midiTracks.sequence,
                    plan.pcmPositions,
                    plan.pcmVelocities,
                    plan.warnings);
        }

        private static DecodePlan prepareDecodePlan(
                List<byte[]> trackPayloads,
                int noteExtraBytes,
                int exstSize,
                int effectiveTrackCount)
                throws IOException
        {
            List<String> warnings = new ArrayList<String>();
            // TRAC bytes are walked once; melody rendering and PCM extraction share these events.
            List<TrackDecodeResult> decodedTracks = decodeTracks(trackPayloads, noteExtraBytes, exstSize);
            List<TrackEvent> orderedEvents = collectOrderedEvents(decodedTracks);
            List<RawTempoPoint> tempoSeeds = collectTempoSeeds(orderedEvents);
            Collections.sort(tempoSeeds, RAW_TEMPO_COMPARATOR);
            List<TempoPoint> tempoPoints = buildTempoPoints(tempoSeeds, warnings);
            TempoMapper tempoMapper = new TempoMapper(tempoPoints);
            LoopInfo loopInfo = determineLoopInfo(decodedTracks, tempoMapper, warnings);
            Map<Integer, Integer> pcmPositions = new HashMap<Integer, Integer>();
            Map<Integer, Integer> pcmVelocities = new HashMap<Integer, Integer>();
            extractPcmTriggers(decodedTracks, tempoPoints, pcmPositions, pcmVelocities, warnings);
            return new DecodePlan(decodedTracks, orderedEvents, tempoPoints, tempoMapper, loopInfo, pcmPositions, pcmVelocities, warnings, effectiveTrackCount);
        }

        private static List<TrackDecodeResult> decodeTracks(List<byte[]> trackPayloads, int noteExtraBytes, int exstSize) throws IOException
        {
            List<TrackDecodeResult> decodedTracks = new ArrayList<TrackDecodeResult>(trackPayloads.size());
            for (int i = 0; i < trackPayloads.size(); i++)
            {
                decodedTracks.add(decodeTrack(i, trackPayloads.get(i), noteExtraBytes, exstSize));
            }
            return decodedTracks;
        }

        private static MidiTracks createMidiTracks() throws InvalidMidiDataException
        {
            Sequence sequence = new Sequence(Sequence.PPQ, MIDI_PPQ);
            Track conductorTrack = sequence.createTrack();
            addTrackName(conductorTrack, "MLD Conductor");

            Track[] channelTracks = new Track[MIDI_CHANNEL_COUNT];
            for (int midiChannel = 0; midiChannel < MIDI_CHANNEL_COUNT; midiChannel++)
            {
                channelTracks[midiChannel] = sequence.createTrack();
                addTrackName(channelTracks[midiChannel], "MLD Channel " + midiChannel);
            }
            return new MidiTracks(sequence, conductorTrack, channelTracks);
        }

        private static RenderState createRenderState(int effectiveTrackCount)
        {
            List<MessageEvent> messageEvents = new ArrayList<MessageEvent>();
            ControlCollector controlCollector = new ControlCollector(messageEvents);
            ChannelState[] channels = createChannelStates();
            int[] partChannelMap = createIdentityPartChannelMap(Math.max(MIDI_CHANNEL_COUNT, effectiveTrackCount * 4));
            OutputChannelLayout outputLayout = OutputChannelLayout.createDefault();
            Map<Integer, ActiveNote> activeNotes = new LinkedHashMap<Integer, ActiveNote>();
            List<Long> stopTicks = new ArrayList<Long>();

            emitInitialMidiDefaults(controlCollector, channels);
            return new RenderState(messageEvents, controlCollector, channels, partChannelMap, outputLayout, activeNotes, stopTicks);
        }

        private static long renderEvents(DecodePlan plan, RenderState renderState)
        {
            long maxTick = 0L;
            for (int i = 0; i < plan.orderedEvents.size(); i++)
            {
                TrackEvent event = plan.orderedEvents.get(i);
                maxTick = Math.max(maxTick, flushExpiredNotes(event.rawTick, renderState.activeNotes, renderState.messageEvents));
                if (event instanceof NoteEvent)
                {
                    maxTick = Math.max(maxTick, handleNoteEvent((NoteEvent) event, plan, renderState));
                }
                else if (event instanceof SystemEvent)
                {
                    maxTick = Math.max(maxTick, handleSystemEvent((SystemEvent) event, plan, renderState));
                }
            }
            return maxTick;
        }

        private static long finalizeRender(DecodePlan plan, RenderState renderState, MidiTracks midiTracks, long maxTick)
                throws InvalidMidiDataException
        {
            maxTick = Math.max(maxTick, flushExpiredNotes(Integer.MAX_VALUE, renderState.activeNotes, renderState.messageEvents));
            for (int i = 0; i < plan.decodedTracks.size(); i++)
            {
                maxTick = Math.max(maxTick, plan.tempoMapper.rawToMidiTick(plan.decodedTracks.get(i).totalRawTicks));
            }

            long contentEndTick = plan.loopInfo.hasLoop ? Math.max(1L, plan.loopInfo.loopEndTick) : Math.max(1L, maxTick);
            emitTempoTrack(plan.tempoPoints, midiTracks.conductorTrack, contentEndTick);
            if (plan.loopInfo.hasLoop)
            {
                addLoopMarker(midiTracks.conductorTrack, plan.loopInfo.loopStartTick, plan.loopInfo.loopEndTick, plan.loopInfo.repeatCount);
            }
            addStopMarkers(midiTracks.conductorTrack, renderState.stopTicks);

            writeChannelEvents(renderState, midiTracks);
            closeTracks(midiTracks, contentEndTick);
            return contentEndTick;
        }

        private static void writeChannelEvents(RenderState renderState, MidiTracks midiTracks) throws InvalidMidiDataException
        {
            int[] outputChannelMap = buildOutputChannelMap(renderState.outputLayout);
            List<MessageEvent> remappedEvents = remapMessageEvents(renderState.messageEvents, outputChannelMap);
            Collections.sort(remappedEvents, MESSAGE_EVENT_COMPARATOR);
            for (int i = 0; i < remappedEvents.size(); i++)
            {
                MessageEvent event = remappedEvents.get(i);
                addShortMessage(midiTracks.channelTracks[event.midiChannel], event.status, event.midiChannel, event.data1, event.data2, event.tick);
            }
        }

        private static void closeTracks(MidiTracks midiTracks, long contentEndTick) throws InvalidMidiDataException
        {
            addEndOfTrack(midiTracks.conductorTrack, contentEndTick + 1L);
            for (int i = 0; i < midiTracks.channelTracks.length; i++)
            {
                addEndOfTrack(midiTracks.channelTracks[i], contentEndTick + 1L);
            }
        }

        private static TrackDecodeResult decodeTrack(int trackIndex, byte[] payload, int noteExtraBytes, int exstSize) throws IOException
        {
            List<TrackEvent> events = new ArrayList<TrackEvent>();
            int offset = 0;
            int rawTick = 0;
            int pendingExtendedDelta = 0;
            int eventIndex = 0;

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
                if (isResourceStatus(status))
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
                        events.add(new ResourceEvent(trackIndex, eventIndex++, delta, rawTick, command, -1, -1, length, true));
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
                        events.add(new ResourceEvent(trackIndex, eventIndex++, delta, rawTick, command, value, part, bodyLength, false));
                    }
                    continue;
                }

                // System status 0xFF carries tempo, loop, global stop, and timing controls.
                if (isSystemStatus(status))
                {
                    if (offset >= payload.length)
                    {
                        throw new IOException("Truncated system event in track " + trackIndex);
                    }

                    int command = payload[offset++] & 0xFF;
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
                    events.add(new SystemEvent(trackIndex, eventIndex++, delta, rawTick, command, value, part, timebase));
                    continue;
                }

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
                        eventIndex++,
                        delta,
                        rawTick,
                        (status >> 6) & 0x03,
                        status & 0x3F,
                        gate,
                        velocity,
                        octaveShift,
                        noteExtraBytes));
            }

            return new TrackDecodeResult(trackIndex, rawTick, events);
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

        private static boolean isResourceStatus(int status)
        {
            return status == (MLD_EXTA_MSG & 0xFF) || status == (MLD_EXTB_MSG & 0xFF) || status == (MLD_EXTC_MSG & 0xFF);
        }

        private static boolean isSystemStatus(int status)
        {
            return status == (MLD_SYSEX_MSG & 0xFF);
        }

        private static List<TrackEvent> collectOrderedEvents(List<TrackDecodeResult> decodedTracks)
        {
            List<TrackEvent> events = new ArrayList<TrackEvent>();
            for (int i = 0; i < decodedTracks.size(); i++)
            {
                events.addAll(decodedTracks.get(i).events);
            }
            Collections.sort(events, TRACK_EVENT_COMPARATOR);
            return events;
        }

        private static List<RawTempoPoint> collectTempoSeeds(List<TrackEvent> orderedEvents)
        {
            List<RawTempoPoint> seeds = new ArrayList<RawTempoPoint>();
            int currentTimebase = DEFAULT_TIMEBASE;
            int currentTempo = DEFAULT_TEMPO;
            for (int i = 0; i < orderedEvents.size(); i++)
            {
                TrackEvent event = orderedEvents.get(i);
                if (!(event instanceof SystemEvent))
                {
                    continue;
                }
                SystemEvent systemEvent = (SystemEvent) event;
                if (!isTempo(systemEvent) && systemEvent.command != 0xBC && systemEvent.command != 0xBF)
                {
                    continue;
                }
                if (seeds.isEmpty() && systemEvent.rawTick > 0)
                {
                    seeds.add(new RawTempoPoint(0, currentTimebase, currentTempo, -1, -1, true));
                }
                if (isTempo(systemEvent))
                {
                    currentTimebase = systemEvent.timebase > 0 ? systemEvent.timebase : currentTimebase;
                    currentTempo = systemEvent.value > 0 ? systemEvent.value : currentTempo;
                }
                else if (systemEvent.command == 0xBC)
                {
                    currentTempo = clamp(MIN_TEMPO, MAX_TEMPO, currentTempo + signedByte(systemEvent.value));
                }
                else if (systemEvent.command == 0xBF)
                {
                    currentTimebase = DEFAULT_TIMEBASE;
                    currentTempo = DEFAULT_TEMPO;
                }
                seeds.add(new RawTempoPoint(
                        systemEvent.rawTick,
                        currentTimebase,
                        currentTempo,
                        systemEvent.trackIndex,
                        systemEvent.eventIndex,
                        false));
            }
            return seeds;
        }

        private static void extractPcmTriggers(
                List<TrackDecodeResult> decodedTracks,
                List<TempoPoint> tempoPoints,
                Map<Integer, Integer> pcmPositions,
                Map<Integer, Integer> pcmVelocities,
                List<String> warnings)
        {
            for (int i = 0; i < decodedTracks.size(); i++)
            {
                TrackDecodeResult track = decodedTracks.get(i);
                for (int j = 0; j < track.events.size(); j++)
                {
                    TrackEvent event = track.events.get(j);
                    if (!(event instanceof ResourceEvent))
                    {
                        continue;
                    }
                    ResourceEvent resourceEvent = (ResourceEvent) event;
                    if (resourceEvent.longEvent)
                    {
                        continue;
                    }
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

        private static List<TempoPoint> buildTempoPoints(List<RawTempoPoint> seeds, List<String> warnings)
        {
            List<TempoPoint> points = new ArrayList<TempoPoint>();
            if (seeds.isEmpty())
            {
                warnings.add("No tempo event observed; inserting synthetic 120 BPM / timebase 48 point.");
                seeds.add(new RawTempoPoint(0, 48, 120, -1, -1, true));
            }
            else if (seeds.get(0).rawTick > 0)
            {
                RawTempoPoint first = seeds.get(0);
                warnings.add("First tempo event does not start at tick 0; inserting synthetic point at origin.");
                seeds.add(0, new RawTempoPoint(0, first.timebase, first.tempo, -1, -1, true));
            }

            long midiTick = 0L;
            int lastRawTick = 0;
            int lastTimebase = seeds.get(0).timebase;
            for (int i = 0; i < seeds.size(); i++)
            {
                RawTempoPoint seed = seeds.get(i);
                int deltaRaw = seed.rawTick - lastRawTick;
                if (deltaRaw < 0)
                {
                    deltaRaw = 0;
                }
                midiTick += ((long) deltaRaw * MIDI_PPQ) / Math.max(1, lastTimebase);
                points.add(new TempoPoint(
                        seed.rawTick,
                        midiTick,
                        seed.timebase,
                        seed.tempo,
                        60000000 / Math.max(1, seed.tempo),
                        seed.synthetic));
                lastRawTick = seed.rawTick;
                lastTimebase = seed.timebase;
            }
            return points;
        }

        private static LoopInfo determineLoopInfo(List<TrackDecodeResult> decodedTracks, TempoMapper mapper, List<String> warnings)
        {
            Integer[] loopStarts = new Integer[4];
            Integer[] loopEnds = new Integer[4];
            int[] repeatCounts = new int[] { 0, 0, 0, 0 };
            List<String> loopWarnings = new ArrayList<String>();

            for (int i = 0; i < decodedTracks.size(); i++)
            {
                TrackDecodeResult track = decodedTracks.get(i);
                for (int j = 0; j < track.events.size(); j++)
                {
                    TrackEvent event = track.events.get(j);
                    if (!(event instanceof SystemEvent))
                    {
                        continue;
                    }
                    SystemEvent systemEvent = (SystemEvent) event;
                    if (systemEvent.command != 0xDD)
                    {
                        continue;
                    }
                    int slot = (systemEvent.value >> 6) & 0x03;
                    int operation = systemEvent.value & 0x03;
                    if (operation == 0x00)
                    {
                        if (loopStarts[slot] == null || systemEvent.rawTick < loopStarts[slot].intValue())
                        {
                            loopStarts[slot] = Integer.valueOf(systemEvent.rawTick);
                        }
                    }
                    else if (operation == 0x01)
                    {
                        if (loopEnds[slot] == null || systemEvent.rawTick < loopEnds[slot].intValue())
                        {
                            loopEnds[slot] = Integer.valueOf(systemEvent.rawTick);
                            int repeat = (systemEvent.value >> 2) & 0x0F;
                            repeatCounts[slot] = repeat == 0 ? -1 : repeat;
                        }
                    }
                }
            }

            int chosenSlot = -1;
            for (int slot = 0; slot < 4; slot++)
            {
                if (loopStarts[slot] != null && loopEnds[slot] != null && loopEnds[slot].intValue() > loopStarts[slot].intValue())
                {
                    if (chosenSlot >= 0)
                    {
                        loopWarnings.add("Multiple loop slots detected; using the lowest numbered complete slot.");
                        break;
                    }
                    chosenSlot = slot;
                }
                else if ((loopStarts[slot] == null) != (loopEnds[slot] == null))
                {
                    loopWarnings.add("Loop slot " + slot + " is incomplete and will be ignored.");
                }
            }

            warnings.addAll(loopWarnings);
            if (chosenSlot < 0)
            {
                return new LoopInfo(false, -1L, -1L, 0);
            }

            int loopStart = loopStarts[chosenSlot].intValue();
            int loopEnd = loopEnds[chosenSlot].intValue();
            if (loopEnd <= loopStart)
            {
                warnings.add("Loop end does not fall after loop start; looping disabled.");
                return new LoopInfo(false, -1L, -1L, 0);
            }

            return new LoopInfo(true, mapper.rawToMidiTick(loopStart), mapper.rawToMidiTick(loopEnd), repeatCounts[chosenSlot]);
        }

        private static long handleNoteEvent(NoteEvent noteEvent, DecodePlan plan, RenderState renderState)
        {
            int partLane = partLaneIndex(noteEvent.trackIndex, noteEvent.voice);
            int logicalChannel = resolvePartChannel(renderState.partChannelMap, partLane);
            if (logicalChannel < 0 || logicalChannel >= MAX_LOGICAL_CHANNELS)
            {
                plan.warnings.add("Skipping note mapped outside logical-channel range: track=" + noteEvent.trackIndex + " voice=" + noteEvent.voice + " -> " + logicalChannel);
                return -1L;
            }
            if (logicalChannel >= renderState.channels.length)
            {
                plan.warnings.add("Skipping note mapped outside channel state range: " + logicalChannel);
                return -1L;
            }

            ChannelState channel = renderState.channels[logicalChannel];
            if (!channel.allowsOrdinaryNotes())
            {
                return -1L;
            }
            observeOutputChannelActivity(renderState.outputLayout, logicalChannel);
            if (logicalChannel >= MIDI_CHANNEL_COUNT)
            {
                plan.warnings.add("Skipping note mapped to logical channel " + logicalChannel + " because the host exposes only 16 MIDI channels.");
                return -1L;
            }

            long midiStartTick = plan.tempoMapper.rawToMidiTick(noteEvent.rawTick);
            emitPatchIfNeeded(renderState.controlCollector, channel, logicalChannel, midiStartTick);

            int noteBase = renderState.outputLayout != null && renderState.outputLayout.isSpecialChannel(logicalChannel) ? 35 : baseMidiNoteForMode(channel.mode);
            int midiNote = clamp(0, 127, noteBase + noteEvent.pitch + octaveOffset(noteEvent.octaveShift));
            int velocity = noteEvent.hasExtraByte() ? clamp(1, 127, noteEvent.velocity * 2) : 126;
            int rawEndTick = noteEvent.rawTick + noteEvent.gate;
            long midiEndTick = normalizeMidiEnd(midiStartTick, plan.tempoMapper.rawToMidiTick(rawEndTick));
            Integer activeKey = Integer.valueOf((logicalChannel << 7) | midiNote);
            ActiveNote active = renderState.activeNotes.get(activeKey);
            if (active != null)
            {
                active.rawEndTick = rawEndTick;
                active.midiEndTick = normalizeMidiEnd(active.midiStartTick, midiEndTick);
                return active.midiEndTick;
            }

            int order = renderState.controlCollector.allocateOrder();
            renderState.messageEvents.add(MessageEvent.noteOn(logicalChannel, midiStartTick, midiNote, velocity, order));
            renderState.activeNotes.put(activeKey, new ActiveNote(logicalChannel, midiNote, rawEndTick, midiEndTick, order, midiStartTick));
            return midiEndTick;
        }

        private static long handleSystemEvent(SystemEvent systemEvent, DecodePlan plan, RenderState renderState)
        {
            if (isTempo(systemEvent))
            {
                return -1L;
            }

            long midiTick = plan.tempoMapper.rawToMidiTick(systemEvent.rawTick);
            if (systemEvent.command == 0xBE)
            {
                return applyGlobalStop(systemEvent, midiTick, renderState, plan.warnings);
            }
            if (systemEvent.command == 0xBF)
            {
                return applySessionReset(midiTick, renderState);
            }
            if (!applyGlobalSystemEvent(systemEvent, midiTick, renderState))
            {
                applyChannelSystemEvent(systemEvent, midiTick, renderState, plan.warnings);
            }
            return midiTick;
        }

        private static boolean applyGlobalSystemEvent(SystemEvent systemEvent, long midiTick, RenderState renderState)
        {
            switch (systemEvent.command)
            {
                case 0xB0:
                    // 0xB0 is the score-track global volume event; maps to MIDI master volume.
                    renderState.controlCollector.emitMasterVolume(midiTick, clamp(0, 127, systemEvent.value));
                    return true;
                case 0xB1:
                    renderState.controlCollector.emitMasterPan(midiTick, clamp(0, 127, systemEvent.value));
                    return true;
                case 0xB3:
                    renderState.controlCollector.emitMasterTune(midiTick, systemEvent.value & 0x7F);
                    return true;
                case 0xBA:
                    applyPatchModeChange(systemEvent.value, midiTick, renderState.controlCollector, renderState.channels);
                    return true;
                case 0xBD:
                    // 0xBD is the system-event master volume; same MIDI mapping as 0xB0.
                    renderState.controlCollector.emitMasterVolume(midiTick, clamp(0, 127, systemEvent.value));
                    return true;
                default:
                    return false;
            }
        }

        private static long applyGlobalStop(SystemEvent systemEvent, long midiTick, RenderState renderState, List<String> warnings)
        {
            if (systemEvent.value != 0)
            {
                warnings.add("Ignoring 0xBE STOP with nonzero value " + systemEvent.value + " at raw tick " + systemEvent.rawTick + ".");
                return midiTick;
            }

            Iterator<Map.Entry<Integer, ActiveNote>> iterator = renderState.activeNotes.entrySet().iterator();
            while (iterator.hasNext())
            {
                ActiveNote active = iterator.next().getValue();
                renderState.messageEvents.add(MessageEvent.noteOff(active.midiChannel, midiTick, active.midiNote, renderState.controlCollector.allocateOrder()));
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
                renderState.messageEvents.add(MessageEvent.noteOff(active.midiChannel, midiTick, active.midiNote, renderState.controlCollector.allocateOrder()));
                iterator.remove();
            }

            renderState.controlCollector.emitAllSoundOff(midiTick);
            recordStopTick(renderState.stopTicks, midiTick);
            resetChannelStates(renderState.channels);
            resetPartChannelMap(renderState.partChannelMap);
            renderState.controlCollector.resetCaches();
            emitInitialMidiDefaults(renderState.controlCollector, renderState.channels, midiTick);
            return midiTick;
        }

        private static void applyChannelSystemEvent(SystemEvent systemEvent, long midiTick, RenderState renderState, List<String> warnings)
        {
            ChannelTarget target;
            switch (systemEvent.command)
            {
                case 0xE0:
                    target = resolveChannelTarget(systemEvent, renderState.channels, renderState.partChannelMap, warnings);
                    applyProgramChange(target, systemEvent.value, midiTick, renderState.controlCollector);
                    break;
                case 0xE1:
                    target = resolveChannelTarget(systemEvent, renderState.channels, renderState.partChannelMap, warnings);
                    applyBankChange(target, systemEvent.value, midiTick, renderState.controlCollector);
                    break;
                case 0xE2:
                    target = resolveChannelTarget(systemEvent, renderState.channels, renderState.partChannelMap, warnings);
                    applyAbsoluteLevel(target, systemEvent.value, midiTick, renderState.controlCollector);
                    break;
                case 0xE3:
                    target = resolveChannelTarget(systemEvent, renderState.channels, renderState.partChannelMap, warnings);
                    applyPan(target, systemEvent.value, midiTick, renderState.controlCollector);
                    break;
                case 0xE4:
                    target = resolveChannelTarget(systemEvent, renderState.channels, renderState.partChannelMap, warnings);
                    applyPitchCoarse(target, systemEvent.value, midiTick, renderState.controlCollector);
                    break;
                case 0xE5:
                    applyVoiceAssignment(systemEvent, renderState.partChannelMap);
                    break;
                case 0xE6:
                    target = resolveChannelTarget(systemEvent, renderState.channels, renderState.partChannelMap, warnings);
                    applyRelativeLevel(target, systemEvent.value, midiTick, renderState.controlCollector);
                    break;
                case 0xE7:
                    target = resolveChannelTarget(systemEvent, renderState.channels, renderState.partChannelMap, warnings);
                    applyPitchRange(target, systemEvent.value, midiTick, renderState.controlCollector);
                    break;
                case 0xE8:
                    target = resolveChannelTarget(systemEvent, renderState.channels, renderState.partChannelMap, warnings);
                    applyPitchFine(target, systemEvent.value, midiTick, renderState.controlCollector);
                    break;
                case 0xE9:
                    target = resolveChannelTarget(systemEvent, renderState.channels, renderState.partChannelMap, warnings);
                    applyFineCache(target, systemEvent.value);
                    break;
                case 0xEA:
                    target = resolveChannelTarget(systemEvent, renderState.channels, renderState.partChannelMap, warnings);
                    applyModulation(target, systemEvent.value, midiTick, renderState.controlCollector);
                    break;
                default:
                    break;
            }
        }

        private static void applyAbsoluteLevel(ChannelTarget target, int value, long midiTick, ControlCollector controlCollector)
        {
            if (target == null)
            {
                return;
            }
            target.channel.level = value & 0x3F;
            target.channel.volumeCache = clamp(0, 127, target.channel.level * 2);
            emitVolumeIfMidiChannel(controlCollector, target, midiTick);
        }

        private static void applyRelativeLevel(ChannelTarget target, int value, long midiTick, ControlCollector controlCollector)
        {
            if (target == null)
            {
                return;
            }
            target.channel.level = clamp(0, 63, target.channel.level + ((value & 0x3F) - 32));
            target.channel.volumeCache = clamp(0, 127, target.channel.level * 2);
            emitVolumeIfMidiChannel(controlCollector, target, midiTick);
        }

        private static void applyPan(ChannelTarget target, int value, long midiTick, ControlCollector controlCollector)
        {
            if (target == null)
            {
                return;
            }
            target.channel.pan = value & 0x3F;
            emitPanIfMidiChannel(controlCollector, target, midiTick);
        }

        private static void applyPitchCoarse(ChannelTarget target, int value, long midiTick, ControlCollector controlCollector)
        {
            if (target == null)
            {
                return;
            }
            target.channel.pitchCoarse = value & 0x3F;
            emitPitchBendIfMidiChannel(controlCollector, target, midiTick);
        }

        private static void applyPitchFine(ChannelTarget target, int value, long midiTick, ControlCollector controlCollector)
        {
            if (target == null)
            {
                return;
            }
            target.channel.pitchFine = value & 0x3F;
            emitPitchBendIfMidiChannel(controlCollector, target, midiTick);
        }

        private static void applyFineCache(ChannelTarget target, int value)
        {
            if (target == null)
            {
                return;
            }
            target.channel.pitchFine = value & 0x3F;
        }

        private static void applyPitchRange(ChannelTarget target, int value, long midiTick, ControlCollector controlCollector)
        {
            if (target == null)
            {
                return;
            }
            int range = value & 0x3F;
            if (range > 24)
            {
                return;
            }
            target.channel.pitchRange = range;
            emitPitchRangeIfMidiChannel(controlCollector, target, midiTick);
        }

        private static void applyModulation(ChannelTarget target, int value, long midiTick, ControlCollector controlCollector)
        {
            if (target == null)
            {
                return;
            }
            target.channel.modulation = value & 0x3F;
            emitModulationIfMidiChannel(controlCollector, target, midiTick);
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
                partChannelMap[partLane] = systemEvent.value & 0x3F;
            }
        }

        private static void applyPatchModeChange(int value, long midiTick, ControlCollector controlCollector, ChannelState[] channels)
        {
            int logicalChannel = (value >> 3) & 0x0F;
            if (!isUsableControlChannel(logicalChannel, channels))
            {
                return;
            }
            ChannelState channel = channels[logicalChannel];
            channel.mode = value & 0x07;
            channel.patchDirty = true;
            if (channel.mode == 1)
            {
                emitPatchIfNeeded(controlCollector, channel, logicalChannel, midiTick);
            }
        }

        private static boolean isUsableControlChannel(int logicalChannel, ChannelState[] channels)
        {
            return logicalChannel >= 0 && logicalChannel < channels.length;
        }

        private static ChannelTarget resolveChannelTarget(
                SystemEvent systemEvent,
                ChannelState[] channels,
                int[] partChannelMap,
                List<String> warnings)
        {
            if (systemEvent.part < 0)
            {
                return null;
            }
            int partLane = partLaneIndex(systemEvent.trackIndex, systemEvent.part);
            int logicalChannel = resolvePartChannel(partChannelMap, partLane);
            if (logicalChannel < 0 || logicalChannel >= MAX_LOGICAL_CHANNELS)
            {
                warnings.add("Skipping control mapped outside logical-channel range: track=" + systemEvent.trackIndex + " part=" + systemEvent.part + " -> " + logicalChannel);
                return null;
            }
            if (!isUsableControlChannel(logicalChannel, channels))
            {
                return null;
            }
            return new ChannelTarget(logicalChannel, channels[logicalChannel]);
        }

        private static void emitInitialMidiDefaults(ControlCollector controlCollector, ChannelState[] channels)
        {
            emitInitialMidiDefaults(controlCollector, channels, 0L);
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

        private static void applyProgramChange(ChannelTarget target, int value, long midiTick, ControlCollector controlCollector)
        {
            if (target == null)
            {
                return;
            }
            target.channel.program = value & 0x3F;
            target.channel.hasProgramEvent = true;
            target.channel.patchDirty = true;
            emitPatchIfNeeded(controlCollector, target.channel, target.logicalChannel, midiTick);
        }

        private static void applyBankChange(ChannelTarget target, int value, long midiTick, ControlCollector controlCollector)
        {
            if (target == null)
            {
                return;
            }
            target.channel.bank = value & 0x3F;
            target.channel.patchDirty = true;
            if (!target.channel.hasProgramEvent)
            {
                return;
            }
            emitPatchIfNeeded(controlCollector, target.channel, target.logicalChannel, midiTick);
        }

        private static void emitVolumeIfMidiChannel(ControlCollector controlCollector, ChannelTarget target, long midiTick)
        {
            if (target.hasMidiChannel())
            {
                controlCollector.emitVolume(target.logicalChannel, midiTick, toMidiVolume(target.channel));
            }
        }

        private static void emitPanIfMidiChannel(ControlCollector controlCollector, ChannelTarget target, long midiTick)
        {
            if (target.hasMidiChannel())
            {
                controlCollector.emitPan(target.logicalChannel, midiTick, toMidiPan(target.channel));
            }
        }

        private static void emitPitchBendIfMidiChannel(ControlCollector controlCollector, ChannelTarget target, long midiTick)
        {
            if (target.hasMidiChannel())
            {
                controlCollector.emitPitchBend(target.logicalChannel, midiTick, computePitchBend(target.channel));
            }
        }

        private static void emitPitchRangeIfMidiChannel(ControlCollector controlCollector, ChannelTarget target, long midiTick)
        {
            if (target.hasMidiChannel())
            {
                controlCollector.emitPitchRange(target.logicalChannel, midiTick, target.channel.pitchRange);
            }
        }

        private static void emitModulationIfMidiChannel(ControlCollector controlCollector, ChannelTarget target, long midiTick)
        {
            if (target.hasMidiChannel())
            {
                controlCollector.emitModulation(target.logicalChannel, midiTick, target.channel.modulation * 2);
            }
        }

        private static void emitPatchIfNeeded(ControlCollector controlCollector, ChannelState channel, int midiChannel, long midiTick)
        {
            if (midiChannel < 0 || midiChannel >= MIDI_CHANNEL_COUNT)
            {
                return;
            }
            if (!channel.allowsOrdinaryNotes())
            {
                return;
            }
            int hostProgram = translateHostProgram(channel);
            if (!channel.patchDirty && channel.patchEmitted && channel.lastProgram == hostProgram)
            {
                return;
            }
            if (channel.patchEmitted && channel.lastProgram == hostProgram)
            {
                channel.patchDirty = false;
                return;
            }

            controlCollector.emitProgramChange(midiChannel, midiTick, hostProgram);
            channel.patchDirty = false;
            channel.patchEmitted = true;
            channel.lastProgram = hostProgram;
        }

        private static int translateHostProgram(ChannelState channel)
        {
            return composeOrdinaryPatchWord(channel.bank, channel.program) & 0x7F;
        }

        private static int composeOrdinaryPatchWord(int bank, int program)
        {
            int low6 = program & 0x3F;
            int high6 = bank & 0x3F;
            if ((high6 & 0x3E) == 0)
            {
                switch (low6)
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
            return low6 | (high6 << 6);
        }

        private static void emitTempoTrack(List<TempoPoint> tempoPoints, Track conductorTrack, long contentEndTick)
                throws InvalidMidiDataException
        {
            TempoPoint active = tempoPoints.get(0);
            addTempoMeta(conductorTrack, active.mpqn, 0L);
            for (int i = 0; i < tempoPoints.size(); i++)
            {
                TempoPoint point = tempoPoints.get(i);
                if (point.midiTick <= 0L || point.midiTick > contentEndTick)
                {
                    continue;
                }
                addTempoMeta(conductorTrack, point.mpqn, point.midiTick);
            }
        }

        private static long flushExpiredNotes(int currentRawTick, Map<Integer, ActiveNote> activeNotes, List<MessageEvent> messageEvents)
        {
            long maxTick = -1L;
            Iterator<Map.Entry<Integer, ActiveNote>> iterator = activeNotes.entrySet().iterator();
            while (iterator.hasNext())
            {
                ActiveNote active = iterator.next().getValue();
                if (active.rawEndTick > currentRawTick)
                {
                    continue;
                }
                messageEvents.add(MessageEvent.noteOff(active.midiChannel, active.midiEndTick, active.midiNote, active.order));
                maxTick = Math.max(maxTick, active.midiEndTick);
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

        private static int partLaneIndex(int trackIndex, int voice)
        {
            return (trackIndex * 4) + voice;
        }

        private static int[] buildOutputChannelMap(OutputChannelLayout outputLayout)
        {
            int[] outputChannelMap = createIdentityPartChannelMap(MIDI_CHANNEL_COUNT);
            if (outputLayout == null || !outputLayout.hasFixedLayout())
            {
                return outputChannelMap;
            }
            if (outputLayout.usesIdentityMap())
            {
                return outputChannelMap;
            }
            int nextMelodicChannel = 0;
            for (int logicalChannel = 0; logicalChannel < MIDI_CHANNEL_COUNT; logicalChannel++)
            {
                if (!outputLayout.isActive(logicalChannel))
                {
                    continue;
                }
                if (outputLayout.isSpecialChannel(logicalChannel))
                {
                    outputChannelMap[logicalChannel] = MIDI_DRUM_CHANNEL;
                    continue;
                }
                outputChannelMap[logicalChannel] = nextMelodicChannel;
                nextMelodicChannel = nextSequentialOutputChannel(nextMelodicChannel, outputLayout.reservesDrumChannel());
            }
            return outputChannelMap;
        }

        private static List<MessageEvent> remapMessageEvents(List<MessageEvent> messageEvents, int[] outputChannelMap)
        {
            List<MessageEvent> remapped = new ArrayList<MessageEvent>(messageEvents.size());
            for (int i = 0; i < messageEvents.size(); i++)
            {
                MessageEvent event = messageEvents.get(i);
                remapped.add(new MessageEvent(
                        remapMidiChannel(event.midiChannel, outputChannelMap),
                        event.tick,
                        event.phase,
                        event.status,
                        event.data1,
                        event.data2,
                        event.order));
            }
            return remapped;
        }

        private static int remapMidiChannel(int logicalChannel, int[] outputChannelMap)
        {
            if (outputChannelMap == null || logicalChannel < 0 || logicalChannel >= outputChannelMap.length)
            {
                return logicalChannel;
            }
            return clamp(0, MIDI_CHANNEL_COUNT - 1, outputChannelMap[logicalChannel]);
        }

        private static void observeOutputChannelActivity(OutputChannelLayout outputLayout, int logicalChannel)
        {
            if (outputLayout != null)
            {
                outputLayout.observeActive(logicalChannel);
            }
        }

        private static int nextSequentialOutputChannel(int current, boolean reserveDrumChannel)
        {
            if (current >= MIDI_CHANNEL_COUNT - 1)
            {
                return MIDI_CHANNEL_COUNT - 1;
            }
            if (reserveDrumChannel && current == (MIDI_DRUM_CHANNEL - 1))
            {
                return current + 2;
            }
            return current + 1;
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
            return clamp(0, 127, channel.volumeCache);
        }

        private static int toMidiPan(ChannelState channel)
        {
            return clamp(0, 127, channel.pan * 2);
        }

        private static int computePitchBend(ChannelState channel)
        {
            return clamp(0, 16383, (8 * (channel.pitchFine + (32 * channel.pitchCoarse))) - 256);
        }

        private static int computeMasterTunePitchBend(int value)
        {
            int centsAdjustment = (value - 0x40) * 100;
            int pitchBendValue = (centsAdjustment * 8192) / 1200;
            pitchBendValue += 8192;
            return clamp(0, 16383, pitchBendValue);
        }

        private static long normalizeMidiEnd(long midiStartTick, long midiEndTick)
        {
            return midiEndTick <= midiStartTick ? (midiStartTick + 1L) : midiEndTick;
        }

        private static int clamp(int min, int max, int value)
        {
            return Math.max(min, Math.min(max, value));
        }

        private static int signedByte(int value)
        {
            int unsigned = value & 0xFF;
            return unsigned < 0x80 ? unsigned : unsigned - 0x100;
        }

        private static int readBe16(byte[] data, int offset)
        {
            return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
        }

        private static int compareInts(int left, int right)
        {
            return Integer.compare(left, right);
        }

        private static int compareLongs(long left, long right)
        {
            return Long.compare(left, right);
        }

        private static void recordStopTick(List<Long> stopTicks, long tick)
        {
            if (!stopTicks.isEmpty() && stopTicks.get(stopTicks.size() - 1).longValue() == tick)
            {
                return;
            }
            stopTicks.add(Long.valueOf(tick));
        }

        private static final class DecodePlan
        {
            final List<TrackDecodeResult> decodedTracks;
            final List<TrackEvent> orderedEvents;
            final List<TempoPoint> tempoPoints;
            final TempoMapper tempoMapper;
            final LoopInfo loopInfo;
            final Map<Integer, Integer> pcmPositions;
            final Map<Integer, Integer> pcmVelocities;
            final List<String> warnings;
            final int effectiveTrackCount;

            DecodePlan(
                    List<TrackDecodeResult> decodedTracks,
                    List<TrackEvent> orderedEvents,
                    List<TempoPoint> tempoPoints,
                    TempoMapper tempoMapper,
                    LoopInfo loopInfo,
                    Map<Integer, Integer> pcmPositions,
                    Map<Integer, Integer> pcmVelocities,
                    List<String> warnings,
                    int effectiveTrackCount)
            {
                this.decodedTracks = decodedTracks;
                this.orderedEvents = orderedEvents;
                this.tempoPoints = tempoPoints;
                this.tempoMapper = tempoMapper;
                this.loopInfo = loopInfo;
                this.pcmPositions = pcmPositions;
                this.pcmVelocities = pcmVelocities;
                this.warnings = warnings;
                this.effectiveTrackCount = effectiveTrackCount;
            }
        }

        private static final class RenderState
        {
            final List<MessageEvent> messageEvents;
            final ControlCollector controlCollector;
            final ChannelState[] channels;
            final int[] partChannelMap;
            final OutputChannelLayout outputLayout;
            final Map<Integer, ActiveNote> activeNotes;
            final List<Long> stopTicks;

            RenderState(
                    List<MessageEvent> messageEvents,
                    ControlCollector controlCollector,
                    ChannelState[] channels,
                    int[] partChannelMap,
                    OutputChannelLayout outputLayout,
                    Map<Integer, ActiveNote> activeNotes,
                    List<Long> stopTicks)
            {
                this.messageEvents = messageEvents;
                this.controlCollector = controlCollector;
                this.channels = channels;
                this.partChannelMap = partChannelMap;
                this.outputLayout = outputLayout;
                this.activeNotes = activeNotes;
                this.stopTicks = stopTicks;
            }
        }

        private static final class MidiTracks
        {
            final Sequence sequence;
            final Track conductorTrack;
            final Track[] channelTracks;

            MidiTracks(Sequence sequence, Track conductorTrack, Track[] channelTracks)
            {
                this.sequence = sequence;
                this.conductorTrack = conductorTrack;
                this.channelTracks = channelTracks;
            }
        }

        static final class DecodeResult
        {
            final Sequence sequence;
            final Map<Integer, Integer> pcmPositions;
            final Map<Integer, Integer> pcmVelocities;
            final List<String> warnings;

            DecodeResult(Sequence sequence, Map<Integer, Integer> pcmPositions, Map<Integer, Integer> pcmVelocities, List<String> warnings)
            {
                this.sequence = sequence;
                this.pcmPositions = new HashMap<Integer, Integer>(pcmPositions);
                this.pcmVelocities = new HashMap<Integer, Integer>(pcmVelocities);
                this.warnings = new ArrayList<String>(warnings);
            }
        }

        private static final class TrackDecodeResult
        {
            final int trackIndex;
            final int totalRawTicks;
            final List<TrackEvent> events;

            TrackDecodeResult(int trackIndex, int totalRawTicks, List<TrackEvent> events)
            {
                this.trackIndex = trackIndex;
                this.totalRawTicks = totalRawTicks;
                this.events = events;
            }
        }

        private static abstract class TrackEvent
        {
            final int trackIndex;
            final int eventIndex;
            final int delta;
            final int rawTick;

            TrackEvent(int trackIndex, int eventIndex, int delta, int rawTick)
            {
                this.trackIndex = trackIndex;
                this.eventIndex = eventIndex;
                this.delta = delta;
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
            final int noteExtraBytes;

            NoteEvent(int trackIndex, int eventIndex, int delta, int rawTick, int voice, int pitch, int gate, int velocity, int octaveShift, int noteExtraBytes)
            {
                super(trackIndex, eventIndex, delta, rawTick);
                this.voice = voice;
                this.pitch = pitch;
                this.gate = gate;
                this.velocity = velocity;
                this.octaveShift = octaveShift;
                this.noteExtraBytes = noteExtraBytes;
            }

            boolean hasExtraByte()
            {
                return noteExtraBytes > 0;
            }
        }

        private static final class SystemEvent extends TrackEvent
        {
            final int command;
            final int value;
            final int part;
            final int timebase;

            SystemEvent(int trackIndex, int eventIndex, int delta, int rawTick, int command, int value, int part, int timebase)
            {
                super(trackIndex, eventIndex, delta, rawTick);
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
            final int bodyLength;
            final boolean longEvent;

            ResourceEvent(int trackIndex, int eventIndex, int delta, int rawTick, int command, int value, int part, int bodyLength, boolean longEvent)
            {
                super(trackIndex, eventIndex, delta, rawTick);
                this.command = command;
                this.value = value;
                this.part = part;
                this.bodyLength = bodyLength;
                this.longEvent = longEvent;
            }
        }

        private static final class RawTempoPoint
        {
            final int rawTick;
            final int timebase;
            final int tempo;
            final int trackIndex;
            final int eventIndex;
            final boolean synthetic;

            RawTempoPoint(int rawTick, int timebase, int tempo, int trackIndex, int eventIndex, boolean synthetic)
            {
                this.rawTick = rawTick;
                this.timebase = timebase;
                this.tempo = tempo;
                this.trackIndex = trackIndex;
                this.eventIndex = eventIndex;
                this.synthetic = synthetic;
            }
        }

        private static final class TempoPoint
        {
            final int rawTick;
            final long midiTick;
            final int timebase;
            final int tempo;
            final int mpqn;
            final boolean synthetic;

            TempoPoint(int rawTick, long midiTick, int timebase, int tempo, int mpqn, boolean synthetic)
            {
                this.rawTick = rawTick;
                this.midiTick = midiTick;
                this.timebase = timebase;
                this.tempo = tempo;
                this.mpqn = mpqn;
                this.synthetic = synthetic;
            }
        }

        private static final class LoopInfo
        {
            final boolean hasLoop;
            final long loopStartTick;
            final long loopEndTick;
            final int repeatCount;

            LoopInfo(boolean hasLoop, long loopStartTick, long loopEndTick, int repeatCount)
            {
                this.hasLoop = hasLoop;
                this.loopStartTick = loopStartTick;
                this.loopEndTick = loopEndTick;
                this.repeatCount = repeatCount;
            }
        }

        private static final class ChannelState
        {
            int mode = 0;
            int bank = 0;
            int program = 0;
            boolean hasProgramEvent = false;
            int level = DEFAULT_LEVEL;
            int volumeCache = DEFAULT_LEVEL * 2;
            int pan = DEFAULT_PAN;
            int pitchCoarse = DEFAULT_PITCH_COARSE;
            int pitchFine = DEFAULT_PITCH_FINE;
            int pitchRange = DEFAULT_PITCH_RANGE;
            int modulation = DEFAULT_MODULATION;
            boolean patchDirty = true;
            boolean patchEmitted = false;
            int lastProgram = -1;

            boolean allowsOrdinaryNotes()
            {
                return mode == 0 || mode == 1;
            }

            void reset()
            {
                mode = 0;
                bank = 0;
                program = 0;
                hasProgramEvent = false;
                level = DEFAULT_LEVEL;
                volumeCache = DEFAULT_LEVEL * 2;
                pan = DEFAULT_PAN;
                pitchCoarse = DEFAULT_PITCH_COARSE;
                pitchFine = DEFAULT_PITCH_FINE;
                pitchRange = DEFAULT_PITCH_RANGE;
                modulation = DEFAULT_MODULATION;
                patchDirty = true;
                patchEmitted = false;
                lastProgram = -1;
            }
        }

        private static final class ActiveNote
        {
            final int midiChannel;
            final int midiNote;
            final int order;
            final long midiStartTick;
            int rawEndTick;
            long midiEndTick;

            ActiveNote(int midiChannel, int midiNote, int rawEndTick, long midiEndTick, int order, long midiStartTick)
            {
                this.midiChannel = midiChannel;
                this.midiNote = midiNote;
                this.rawEndTick = rawEndTick;
                this.midiEndTick = midiEndTick;
                this.order = order;
                this.midiStartTick = midiStartTick;
            }
        }

        private static final class ChannelTarget
        {
            final int logicalChannel;
            final ChannelState channel;

            ChannelTarget(int logicalChannel, ChannelState channel)
            {
                this.logicalChannel = logicalChannel;
                this.channel = channel;
            }

            boolean hasMidiChannel()
            {
                return logicalChannel >= 0 && logicalChannel < MIDI_CHANNEL_COUNT;
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

            void emitControlChange(int midiChannel, long tick, int controller, int value)
            {
                emitDedupedControl(midiChannel, tick, controller, clamp(0, 127, value));
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

            void emitMasterTune(long tick, int value)
            {
                if (value < 0x34 || value > 0x4C)
                {
                    return;
                }
                int pitchBendValue = computeMasterTunePitchBend(value);
                for (int midiChannel = 0; midiChannel < MIDI_CHANNEL_COUNT; midiChannel++)
                {
                    emitPitchBend(midiChannel, tick, pitchBendValue);
                }
            }

            void emitAllSoundOff(long tick)
            {
                for (int midiChannel = 0; midiChannel < MIDI_CHANNEL_COUNT; midiChannel++)
                {
                    emitImmediateControl(midiChannel, tick, 120, 0);
                }
            }

            void emitImmediateControl(int midiChannel, long tick, int controller, int value)
            {
                emit(midiChannel, tick, ShortMessage.CONTROL_CHANGE, controller, clamp(0, 127, value));
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

        private static final class OutputChannelLayout
        {
            private final boolean fixedLayout;
            private final boolean identityMap;
            private final boolean reserveDrumChannel;
            private int activeMask = 0;
            private int specialChannelMask = 0;

            private OutputChannelLayout(boolean fixedLayout, boolean identityMap, boolean reserveDrumChannel, int specialChannelMask)
            {
                this.fixedLayout = fixedLayout;
                this.identityMap = identityMap;
                this.reserveDrumChannel = reserveDrumChannel;
                this.specialChannelMask = specialChannelMask;
            }

            static OutputChannelLayout createDefault()
            {
                return new OutputChannelLayout(true, DEFAULT_IDENTITY_OUTPUT_MAP, DEFAULT_RESERVE_DRUM_OUTPUT_CHANNEL, DEFAULT_SPECIAL_OUTPUT_MASK);
            }

            void observeActive(int logicalChannel)
            {
                if (logicalChannel < 0 || logicalChannel >= MIDI_CHANNEL_COUNT)
                {
                    return;
                }
                activeMask |= (1 << logicalChannel);
            }

            boolean isActive(int logicalChannel)
            {
                return logicalChannel >= 0
                        && logicalChannel < MIDI_CHANNEL_COUNT
                        && ((activeMask >>> logicalChannel) & 1) != 0;
            }

            boolean hasFixedLayout()
            {
                return fixedLayout;
            }

            boolean isSpecialChannel(int logicalChannel)
            {
                return logicalChannel >= 0
                        && logicalChannel < MIDI_CHANNEL_COUNT
                        && ((specialChannelMask >>> logicalChannel) & 1) != 0;
            }

            boolean usesIdentityMap()
            {
                return identityMap;
            }

            boolean reservesDrumChannel()
            {
                return reserveDrumChannel;
            }
        }

        private static final class TempoMapper
        {
            private final List<TempoPoint> tempoPoints;

            TempoMapper(List<TempoPoint> tempoPoints)
            {
                this.tempoPoints = tempoPoints;
            }

            long rawToMidiTick(int rawTick)
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
        }

        private static final Comparator<TrackEvent> TRACK_EVENT_COMPARATOR = new Comparator<TrackEvent>()
        {
            public int compare(TrackEvent left, TrackEvent right)
            {
                int byTick = compareInts(left.rawTick, right.rawTick);
                if (byTick != 0) { return byTick; }

                int byTrack = compareInts(left.trackIndex, right.trackIndex);
                if (byTrack != 0) { return byTrack; }

                return compareInts(left.eventIndex, right.eventIndex);
            }
        };

        private static final Comparator<RawTempoPoint> RAW_TEMPO_COMPARATOR = new Comparator<RawTempoPoint>()
        {
            public int compare(RawTempoPoint left, RawTempoPoint right)
            {
                int byTick = compareInts(left.rawTick, right.rawTick);
                if (byTick != 0) { return byTick; }

                int byTrack = compareInts(left.trackIndex, right.trackIndex);
                if (byTrack != 0) { return byTrack; }

                int byEvent = compareInts(left.eventIndex, right.eventIndex);
                if (byEvent != 0) { return byEvent; }

                if (left.synthetic == right.synthetic)
                {
                    return 0;
                }
                return left.synthetic ? -1 : 1;
            }
        };

        private static final Comparator<MessageEvent> MESSAGE_EVENT_COMPARATOR = new Comparator<MessageEvent>()
        {
            public int compare(MessageEvent left, MessageEvent right)
            {
                int byTick = compareLongs(left.tick, right.tick);
                if (byTick != 0) { return byTick; }

                int byOrder = compareInts(left.order, right.order);
                if (byOrder != 0) { return byOrder; }

                int byPhase = compareInts(left.phase, right.phase);
                if (byPhase != 0) { return byPhase; }
                int byChannel = compareInts(left.midiChannel, right.midiChannel);
                if (byChannel != 0) { return byChannel; }
                int byData1 = compareInts(left.data1, right.data1);
                if (byData1 != 0) { return byData1; }

                return compareInts(left.data2, right.data2);
            }
        };
    }
}