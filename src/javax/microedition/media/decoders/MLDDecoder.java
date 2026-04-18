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

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.nio.ByteOrder;
import java.util.Arrays;

import javax.sound.midi.InvalidMidiDataException;
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

    private static final byte MIDI_CHANNELS = 16;

    // These are used only for debugging
    private static final String[] formatTypes = {"RESERVED", "0x1: Melody", "0x2: Song"};
    private static final String[] melodyTypes = {"RESERVED", "0x1: Complete Melody", "0x2: Part of Melody"};
    private static final String[] extInfoTypes = 
    {
        "RESERVED",     // %b11110000
        "WAV_DATA",     // %b11110001
        "TEXT_DATA",    // %b11110010
        "PICT_DATA",    // %b11110011
        "ANIM_DATA",    // %b11110100
        "RESERVED",     // %b11110101
        "RESERVED",     // %b11110110
        "RESERVED",     // %b11110111
        "RESERVED",     // %b11111000
        "RESERVED",     // %b11111001
        "RESERVED",     // %b11111010
        "RESERVED",     // %b11111011
        "RESERVED",     // %b11111100
        "RESERVED",     // %b11111101
        "RESERVED",     // %b11111110
        "RESERVED",     // %b11111111
    };

    private static final String[] sourceFromTypes = 
    {
        "Network",
        "Terminal",
        "External",
        "RESERVED"
    };

    private static final int[] timebases = new int[] 
    {
        6,   // %b0000
        12,  // %b0001
        24,  // %b0010
        48,  // %b0011
        96,  // %b0100
        192, // %b0101
        384, // %b0110
        0,   // %b0111 - RESERVED
        15,  // %b1000
        30,  // %b1001
        60,  // %b1010
        120, // %b1011
        240, // %b1100
        480, // %b1101
        960, // %b1110
        0    // %b1111 - RESERVED
    };

    // Create a new sequence and track for the converted MLD file
    private static byte numTracks;
    private static Sequence sequence;
    private static Track track;
    private static boolean noteHas3Bytes;
    private static int curTrack; // This increases by 1 for every "trac" chunk in the same MLD

    private static MLDChannelData[] channelData;

    private static byte[] input;

    private static int decodePos = 0;
    private static int exst = -1;

    private static int[] cuePoints;

    private static int globalTimebase;
    private static int globalTempo;

    // PCM-Specific variables
    public static boolean isPCM = false; // Used by PlatformPlayer to decide whether it'll load the decoded data into a WAVPlayer or MIDIPlayer

    // Structures that hold decoded MLD data
    public static List<InputStream> pcmData = null;
    public static InputStream SequenceData = null;
    public static Map<Integer, Integer> pcmDataPositions = new HashMap<Integer, Integer>();
    public static Map<Integer, Integer> pcmDataVelocities = new HashMap<Integer, Integer>();

    public static synchronized void decodeMLD(byte[] data)
	{
        // Reset any and all static variables to their defaults
        isPCM = false;
        decodePos = 0;
        exst = -1;
        numTracks = 0;
        sequence = null; // Clear the previous MIDI sequence
        noteHas3Bytes = true;
        curTrack = 0;
        globalTimebase = timebases[3];
        globalTempo = 125;

        // Clear previous decoded data objects
        if(pcmData != null) { pcmData.clear(); }
        pcmData = null;
        SequenceData = null;
        pcmData = new ArrayList<InputStream>();
        pcmDataPositions.clear();
        pcmDataVelocities.clear();

        input = data;

        boolean parsingData = true;

        // Start parsing the file.
        decodeHeader(); // melo (file header)

        while(parsingData && decodePos < data.length)
        {
            String chunkID = "" + (char) input[decodePos] + (char) input[decodePos+1] + (char) input[decodePos+2] + (char) input[decodePos+3];

            if (chunkID.equals("adat"))      { decodeADATChunk(); }
            //else if (chunkID.equals("adpm")) { decodeADPMChunk(); } // ADPM is read in ADATChunk above
            else if (chunkID.equals("ainf")) { decodeAINFChunk(); }
            else if (chunkID.equals("auth")) { decodeAUTHChunk(); } 
            else if (chunkID.equals("copy")) { decodeCOPYChunk(); } 
            else if (chunkID.equals("code")) { decodeCODEChunk(); } 
            else if (chunkID.equals("cuep")) { decodeCUEPChunk(); } // TODO: Untested
            else if (chunkID.equals("date")) { decodeDATEChunk(); } 
            else if (chunkID.equals("exst")) { decodeEXSTChunk(); }
            else if (chunkID.equals("note")) { decodeNOTEChunk(); } 
            else if (chunkID.equals("prot")) { decodePROTChunk(); } 
            else if (chunkID.equals("sorc")) { decodeSORCChunk(); } 
            else if (chunkID.equals("supt")) { decodeSUPTChunk(); } 
            else if (chunkID.equals("thrd")) { decodeTHRDChunk(); } // TODO: Properly parse this, right now no 3D positioning is accounted for
            else if (chunkID.equals("titl")) { decodeTITLChunk(); } 
            else if (chunkID.equals("vers")) { decodeVERSChunk(); } 
            else if (chunkID.equals("trac")) { decodeTRACChunk(); } 
            else                             { parsingData = false; } // Assume we reached EOF
        }

        try
        {
            // Convert the resulting sequence to byte array and send to the player.
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MidiSystem.write(sequence, 0, output);
            SequenceData = new ByteArrayInputStream(output.toByteArray());

            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + " MFi parsing and conversion finished, Sequence data size:" + output.size() + " | number of PCM streams:" + pcmData.size());
        }
        catch (Exception e) 
        { 
            Mobile.log(Mobile.LOG_ERROR, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + " couldn't write converted MFi Data:" + e.getMessage()); 
            e.printStackTrace(); 
            SequenceData = null;
            pcmData = null;
        }
	}

    public static void decodeHeader() 
    {
        String fileChunkID = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++]; // "MMMD"
        int fileChunkSize = (input[decodePos++] & 0xFF) << 24 | (input[decodePos++] & 0xFF) << 16 | (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF) - 8;
        
        int headerLength = (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF);
        
        int songType = input[decodePos++] & 0xFF;
        int instruments = input[decodePos++] & 0xFF;
        numTracks = (byte) (input[decodePos++] & 0xFF);

        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"-------------------------- MLD CONTENT HEADER --------------------------");
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"fileChunkID: " + fileChunkID);
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"fileChunkSize: " + fileChunkSize);
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"headerSize: " + headerLength);
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"songType: " + formatTypes[songType]);
        if (songType == 0x0200) 
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
        
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"numTracks: " + (4*numTracks) + (numTracks == 1 ? " (MFi)" : "(MFi 2)"));

        // We now have sufficient data to create a proper MIDI sequence.
        if(sequence == null) 
        { 
            try 
            {
                cuePoints = new int[numTracks];
                channelData = new MLDChannelData[4*numTracks];
                for(int i = 0; i < 4*numTracks; i++) { channelData[i] = new MLDChannelData(); }
                
                // Default timebase for CMF/MFi is 48, we make it the highest possible of 960 for MIDI so we can better round any on-the-fly timebase and tempo adjustments with enough precision
                sequence = new Sequence(Sequence.PPQ, 960);
                track = sequence.createTrack();
            } 
            catch(InvalidMidiDataException ie) { Mobile.log(Mobile.LOG_ERROR, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + " couldn't create MIDI Sequence to convert:" + ie.getMessage()); }
        }

        // OK, we're at the start of the "sorc" chunk, which means the content info chunk (content header) has been left behind
    }

    public static void decodeSORCChunk()
    {
        // We're at the Score Track Chunk, so let's decode the info about the audio data
        String sorc = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++]; // "MTR" actually uses 4 chars, the last one appears to always be 0x01
        int chunkSize = (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF); // length is 16 bit for most subchunks in MLD it seems
        byte sourceType = (byte) (input[decodePos++] & 0xFF);

        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Source info > From: " + sourceFromTypes[sourceType & 0xF7] + " | Has copyright: " + ((sourceType & 0x01) == 1 ? "Yes" : "No"));
    }

    public static void decodeTITLChunk() 
    {
        String chunkName = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++];
        int chunkSize = (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF); 
        byte[] byteData = new byte[chunkSize];
        
        for(int i = 0; i < chunkSize; i++) { byteData[i] = (byte) (input[decodePos++]); }

        try 
        {
            String MLDTrackData = new String(byteData, "Shift_JIS");

            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Title: " + MLDTrackData);
        }
        catch(UnsupportedEncodingException e) { }
    }

    public static void decodeVERSChunk() 
    {
        String chunkName = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++];
        int chunkSize = (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF); 
        byte[] byteData = new byte[chunkSize];
        
        for(int i = 0; i < chunkSize; i++) { byteData[i] = (byte) (input[decodePos++]); }

        String MLDTrackData = new String(byteData);

        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Version: " + MLDTrackData);
    }

    public static void decodeDATEChunk() 
    {
        String chunkName = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++];
        int chunkSize = (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF); 
        byte[] byteData = new byte[chunkSize];
        
        for(int i = 0; i < chunkSize; i++) { byteData[i] = (byte) (input[decodePos++]); }

        String MLDTrackData = new String(byteData);

        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Date: " + MLDTrackData);
    }

    public static void decodeEXSTChunk() 
    {
        String chunkName = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++];
        int chunkSize = (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF); 
        byte[] byteData = new byte[chunkSize];
        
        for(int i = 0; i < chunkSize; i++) { byteData[i] = (byte) (input[decodePos++]); }

        exst = byteData[0] + 0xFF + byteData[1];

        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Exst: " + exst);
    }


    public static void decodeCOPYChunk() 
    {
        String chunkName = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++];
        int chunkSize = (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF); 
        byte[] byteData = new byte[chunkSize];
        
        for(int i = 0; i < chunkSize; i++) { byteData[i] = (byte) (input[decodePos++]); }

        String MLDTrackData = new String(byteData);

        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Copyright: " + MLDTrackData);
    }

    public static void decodeCODEChunk() 
    {
        String chunkName = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++];
        int chunkSize = (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF); 
        byte[] byteData = new byte[chunkSize];
        
        for(int i = 0; i < chunkSize; i++) { byteData[i] = (byte) (input[decodePos++]); }

        String MLDTrackData = new String(byteData);

        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Code: " + MLDTrackData);
    }

    public static void decodeSUPTChunk() 
    {
        String chunkName = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++];
        int chunkSize = (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF); 
        byte[] byteData = new byte[chunkSize];
        
        for(int i = 0; i < chunkSize; i++) { byteData[i] = (byte) (input[decodePos++]); }

        String MLDTrackData = "";
        try { MLDTrackData = new String(byteData, "Shift_JIS"); }
        catch (Exception e) { }

        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"suptval: " + MLDTrackData);
    }

    public static void decodePROTChunk() 
    {
        String chunkName = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++];
        int chunkSize = (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF); 
        byte[] byteData = new byte[chunkSize];
        
        for(int i = 0; i < chunkSize; i++) { byteData[i] = (byte) (input[decodePos++]); }

        String contentProvider = new String(byteData);

        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Content Provider: " + contentProvider);
    }

    public static void decodeNOTEChunk() 
    {
        String chunkName = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++];
        int chunkSize = (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF); 
        int alignbyte = input[decodePos++] & 0xFF; // TODO: Find out if this is ever different from 0x00 (and if the NOTE chunk ever has a chunkSize larger than 2 bytes)
        noteHas3Bytes = (input[decodePos++] & 0xFF) == 0;
        
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Notes are 3 Bytes: " + noteHas3Bytes);
    }

    public static void decodeAUTHChunk() 
    {
        String chunkName = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++];
        int chunkSize = (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF); 
        byte[] byteData = new byte[chunkSize];
        
        for(int i = 0; i < chunkSize; i++) { byteData[i] = (byte) (input[decodePos++]); }

        String author = new String(byteData);

        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Author: " + author);
    }

    public static void decodeTHRDChunk() 
    {
        String chunkName = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++];
        int chunkSize = (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF); 
        byte[] byteData = new byte[chunkSize];
        
        for(int i = 0; i < chunkSize; i++) { byteData[i] = (byte) (input[decodePos++]); }

        Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"-------------------------- 3D POS INFO CHUNK --------------------------");
        Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"3D Positioning data: " + Arrays.toString(byteData));
    }

    public static void decodeCUEPChunk() 
    {
        String chunkName = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++];
        int chunkSize = (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF); 
        byte[] byteData = new byte[chunkSize];
        
        for (int i = 0; i < cuePoints.length; i++) { cuePoints[i] = (input[decodePos++] & 0xFF) << 24 | (input[decodePos++] & 0xFF) << 16 | (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF); }

        Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"-------------------------- 3D POS INFO CHUNK --------------------------");
        Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"3D Positioning data: " + Arrays.toString(byteData));
    }

    public static void decodeAINFChunk() 
    {
        String chunkName = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++];
        int chunkSize = (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF); 
        int numStreams = (input[decodePos++] & 0xFF);
        int hasPCMData = (input[decodePos++] & 0xFF);
        
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"-------------------------- AUDIO INFO CHUNK --------------------------");
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Chunk Size: " + chunkSize);
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Amount of (AD)PCM streams: " + numStreams);
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Has following (AD)PCM streams: " + (hasPCMData == 0 ? "Yes" : "No"));
    }

    public static void decodeADATChunk() 
    {
        String chunkName = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++];
        int chunkSize = ((input[decodePos++] & 0xFF) << 24 | (input[decodePos++] & 0xFF) << 16 | (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF));
        int adpmHeaderLen = (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF);
        
        // adpmHeaderLen contains these two as well
        int dataFormat = (input[decodePos++] & 0xFF);
        int dataAttribute = (input[decodePos++] & 0xFF);

        // adpcmSize needs to subtract adpcmHeaderLen (2 bytes), dataFormat (1),
        // dataAttribute (1), and the entire ADPM header (9) = 13 bytes.
        int adpcmSize = chunkSize - 13;
        
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"-------------------------- AUDIO DATA CHUNK --------------------------");
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Chunk Size: " + chunkSize);
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"ADPCM Data Size: " + adpcmSize);
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"ADPM Header Length (+fmt +attr): " + adpmHeaderLen);
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Data format: " + dataFormat);
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Data attribute: " + dataAttribute);
    
        decodeADPMChunk(adpcmSize);
    }

    public static void decodeADPMChunk(int size) 
    {
        String chunkName = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++];
        int chunkSize = (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF); 
        int sampleRate = (input[decodePos++] & 0xFF);
        int bitDepth = (input[decodePos++] & 0xFF);
        int numChannels = (input[decodePos++] & 0xFF);

        byte[] waveData = new byte[size];

        for(int i = 0; i < size; i++) 
        {
            waveData[i] = input[decodePos++];
        }

        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"-------------------------- ADPCM DATA CHUNK --------------------------");
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Chunk Size: " + chunkSize);
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Sample Rate: " + (sampleRate * 1000) + "Hz");
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Bit Depth: " + bitDepth + " bits"); // This is either 2 or 4 bits
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Channel type: " + ((numChannels & 0x07) == 1 ? "Mono " : "Stereo ") + ((numChannels & 0x08) == 0 ? "Non-Interleaved" : "Interleaved"));

        // TODO: MLD seems to use Yamaha's ADPCM Z (YMZ* chips).
        if(bitDepth == 4) { pcmData.add(new ByteArrayInputStream(WAVYamahaADPCMDecoder.ADPCMZDecode(waveData, sampleRate * 1000, numChannels & 0x07))); }
        else { Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"2-bit depth decoding not supported!"); }
    }

    public static void decodeTRACChunk() 
    {
        String chunkName = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++];
        
        // Gradius Neo Imperial has an MLD file that contains a track chunk with no size (and thus nothing after it), so return immediately in those cases
        if(decodePos == input.length) 
        { 
            Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Track has no data. Skipping");
            return; 
        }
        
        int chunkSize = ((input[decodePos++] & 0xFF) << 24 | (input[decodePos++] & 0xFF) << 16 | (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF));
        
        chunkSize = Math.min(chunkSize, input.length-decodePos); // The final track chunk might report an incorrect chunkSize (happens in some Gradius NEO Imperial files)
        
        byte[] byteData = new byte[chunkSize];
        
        for(int i = 0; i < chunkSize; i++) 
        { 
            if(decodePos == input.length) { break; }
            byteData[i] = (byte) (input[decodePos++]); 
        }
        
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"-------------------------- NEW TRACK CHUNK --------------------------");
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"trackSize: " + chunkSize);
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"trackBytes: " + Arrays.toString(byteData));
    
        try { decodeTRACEvents(byteData); curTrack++; }
        catch(Exception e) 
        {
            Mobile.log(Mobile.LOG_ERROR, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Failed to decode track events: " + e.getMessage() + " ");
            e.printStackTrace();
            throw new NullPointerException();
            //SequenceData = null;
        }
    }

    /* -------------------------------------------------------------------------------------- */
    /*           Lower level decoding functions (sequence event, duration, etc)               */
    /* -------------------------------------------------------------------------------------- */

    public static void decodeTRACEvents(byte[] data) throws InvalidMidiDataException 
    {        
        int offset = 0;
        int totalDuration = 0;
        byte status = 0;

        byte eventChannel = 0;
        int gateTime = 0;
        byte noteNumber = 0;
        byte octaveShift = 0;
        byte velocity = 127; // Assume max velocity for all notes, as 3 byte notes do not have velocity changes
        MidiEvent midiEvent = null;

        int centsAdjustment = 0, pitchBendValue = 0, msb = 0, lsb = 0;

        // TODO: This shouldn't be needed, but some MLD files with PCM streams refuse to play PCM otherwise (they only have gateTime = 0 notes)
        ShortMessage pcmWorkaround = new ShortMessage();
        pcmWorkaround.setMessage(ShortMessage.NOTE_OFF, 0, 0, 0);
        track.add(new MidiEvent(pcmWorkaround, Math.round(1 * getMillisecondsPerTick())));

        while (offset < data.length) 
        {

            totalDuration += Math.round((data[offset++] & 0xFF) * getMillisecondsPerTick()); // Update total duration
            status = (byte) (data[offset++] & 0xFF); // Read status byte
        
            // Check against non-note status messages
            if(status == MLD_EXTA_MSG || status == MLD_EXTB_MSG || status == MLD_EXTC_MSG || status == MLD_SYSEX_MSG) 
            {
                byte eventParam = (byte) (data[offset++] & 0xFF);

                if(eventParam < (byte) 0x80) // ExtA Status Message (in CMF, this appears to be fine-pitch-bend)
                {
                    byte[] exstData = new byte[1 + exst];
                    for(int i = 0; i < 1 + exst; i++) { exstData[i] = data[offset++]; }
                    
                    Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Ext-A event. Data: " + Arrays.toString(exstData));
                }
                else if(eventParam >= (byte) 0x80 && eventParam < (byte) 0xF0) // ExtB Status Message
                {
                    int eventValue = (data[offset++] & 0xFF);
                    byte eventChannelIndex = (byte) ((eventValue >> 6) & 0x03);
                    eventChannel           = (byte) (curTrack * 4 + eventChannelIndex);
                    
                    ShortMessage event = new ShortMessage();

                    if (eventParam >= (byte) 0xC0 && eventParam <= (byte) 0xCF) // TIMEBASE-TEMPO event
                    {
                        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "TIMEBASE TEMPO event! timebase:" + timebases[(eventParam & 0x0F)] + " tempo:" + (eventValue != 0 ? eventValue : globalTempo));
                        
                        globalTimebase = timebases[(eventParam & 0x0F)];
                        globalTempo = eventValue != 0 ? eventValue : globalTempo;
                        continue;
                    }
                    
                    // TODO: Improve these
                    switch (eventParam) 
                    {
                        case (byte) 0x80: // AUDIO CHANNEL VOLUME TODO: Is this where PCM samples are set to be played?
                            Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Adding audio channel volume (PCM) event value " + ((eventValue & 0x3F) * 2) + ", channel " + eventChannel);
                            pcmDataPositions.put(totalDuration, (int) eventChannel);
                            pcmDataVelocities.put(totalDuration, (eventValue & 0x3F) * 2);
                            break;
                        case (byte) 0x81: // Sonic 2 Trial uses this, seems to be ADPCM panpot
                            Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Unsupported audio channel panpot (PCM) event value " + ((eventValue & 0x3F) * 2) + ", channel " + eventChannel);
                            //voice is 2 bits "((eventValue >> 6) & 0x03)", panpot is 6 bits "(eventValue & 0x3F) * 2"
                            //pcmDataPositions.put(totalDuration, (int) eventChannel);
                            //pcmDataVelocities.put(totalDuration, (eventValue & 0x3F) * 2);
                            break;

                        case (byte) 0x90: // Sonic 2 Trial uses this, seems to be 3D positioning 
                            Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Unsupported 3D positioning (PCM) event value " + ((eventValue & 0x3F) * 2) + ", channel " + eventChannel);


                        case (byte) 0xB0: // MASTER_VOLUME
                            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Setting Master Volume to " + (eventValue & 0x7F));
                            for(int i = 0; i < MIDI_CHANNELS; i++) 
                            {
                                event.setMessage(ShortMessage.CONTROL_CHANGE, i, 7, (eventValue & 0x7F));
                                midiEvent = new MidiEvent(event, totalDuration);
                                track.add(midiEvent);
                            }
                            break;

                        case (byte) 0xB1: // MASTER_BALANCE
                            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Setting Master Balance to " + (eventValue & 0x7F));
                            for(int i = 0; i < MIDI_CHANNELS; i++) 
                            {
                                event.setMessage(ShortMessage.CONTROL_CHANGE, i, 10, (eventValue & 0x7F));
                                midiEvent = new MidiEvent(event, totalDuration);
                                track.add(midiEvent);
                            }
                            break;

                        case (byte) 0xB3: // MASTER_TUNE
                            if (eventValue < (byte) 0x34 || eventValue > (byte) 0x4C) 
                            {
                                Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Invalid master tune value: " + eventValue);
                                continue;
                            }

                            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Setting Master Tune to " + eventValue);
                            
                            // Calculate the cents adjustment and the resulting master tune (Based on CMF's table)
                            centsAdjustment = (eventValue - 0x40) * 100;

                            pitchBendValue = (centsAdjustment * 8192) / 1200;
                            pitchBendValue += 8192;

                            pitchBendValue = Math.max(0, Math.min(16383, pitchBendValue));

                            // Split the pitch bend value into LSB and MSB
                            lsb = pitchBendValue & 0x7F;
                            msb = (pitchBendValue >> 7) & 0x7F;

                            // Send the pitch bend message to all channels
                            for (int i = 0; i < MIDI_CHANNELS; i++) 
                            {
                                event.setMessage(ShortMessage.PITCH_BEND, i, lsb, msb);
                                midiEvent = new MidiEvent(event, totalDuration);
                                track.add(midiEvent);
                            }
                            break;

                        case (byte) 0xBA: // Drum bank enable
                            Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "(Unsupported) Adding drum bank enable " + ((eventValue & 1) != 0) + " event " + " to channel " + eventChannel);
                            //event.setMessage(ShortMessage.CONTROL_CHANGE, (eventValue >> 3 & 15), channelData[(eventValue >> 3 & 15)].currentInstrument, 0);
                            //midiEvent = new MidiEvent(event, totalDuration);
                            //track.add(midiEvent);
                            break;

                        case (byte) 0xD0: // CUEPOINT
                            Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Cue point event type (not implemented): " + (eventValue == 0x00 ? "StartPoint" : "EndPoint") + " at time " + totalDuration);
                            
                            // startPoint TODO: Properly implement this, right now it just sets the POSITION POINTER
                            if(eventValue == 0x00) 
                            {
                                event.setMessage(ShortMessage.SONG_POSITION_POINTER, totalDuration & 0x7F, (totalDuration >> 7) & 0x7F);
                                midiEvent = new MidiEvent(event, totalDuration);
                                track.add(midiEvent);
                            }
                            else // endpoint TODO: Might not be correct
                            {
                                event.setMessage(ShortMessage.STOP);
                                midiEvent = new MidiEvent(event, totalDuration);
                                track.add(midiEvent);
                                return;
                            }
                            
                            break;

                        case (byte) 0xD1: // JUMP
                            int destination = (eventValue >> 6) & 0x03;
                            int jumpId = (eventValue >> 4) & 0x03;
                            int noOfJumps = eventValue & 0x0F;
                            
                            Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +
                                    "Parsed jump event (not implemented): " +
                                    "Destination: " + (destination == 0 ? "Destination Point" : "Jump Point") + ", " +
                                    "Jump ID: " + jumpId + ", " +
                                    "Number of Jumps: " + (noOfJumps == 15 ? "Infinity" : noOfJumps));

                            // TODO: Apply this to the MIDI data somehow
                            break;

                        case (byte) 0xDC: // NOP Type 2 (just do nothing)
                            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Parsed NOP Type 2 event");
                            break;

                        case (byte) 0xDD: // Loop Point
                            Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Loop point event not implemented!");
                            break;

                        case (byte) 0xDE: // NOP (just do nothing)
                            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Parsed NOP event");
                            break;

                        case (byte) 0xDF: // END_OF_TRACK
                            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "END_OF_TRACK Reached!");
                            return;
            
                        case (byte) 0xE0: // Program Change
                            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Adding program change number " + (eventValue & 0x3F) + " to channel " + eventChannel);
                            event.setMessage(ShortMessage.PROGRAM_CHANGE, eventChannel, (eventValue & 0x3F), 0);
                            midiEvent = new MidiEvent(event, totalDuration);
                            track.add(midiEvent);
                            break;
                        
                        case (byte) 0xE1: // Bank Change
                            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Adding bank change to channel " + eventChannel + " with value " + (eventValue & 0x3F));
                            
                            if ((eventValue & 0x3F) >= 2 && (eventValue & 0x3F) <= 3) // Bank should map to General MIDI Instrument Patch Map
                            {
                                int bankNumber = (eventValue & 0x3F) - 2; // 0 selects the first 64 patches of the Patch Map, 1 selects the second 64 patches
                                
                                // Bank Select MSB
                                event.setMessage(ShortMessage.CONTROL_CHANGE, eventChannel, 0, bankNumber);
                                midiEvent = new MidiEvent(event, totalDuration);
                                track.add(midiEvent);

                                // Bank Select LSB
                                event.setMessage(ShortMessage.CONTROL_CHANGE, eventChannel, 32, 0); // Assuming LSB is 0 for simplicity
                                midiEvent = new MidiEvent(event, totalDuration);
                                track.add(midiEvent);
                            } 
                            else if ((eventValue & 0x3F) == 63) // Drum bank identical to MIDI Percussion Key Map
                            {
                                event.setMessage(ShortMessage.CONTROL_CHANGE, eventChannel, 0, (eventValue & 0x3F)); // TODO: Bank select for drums (might not be supported in MIDI)
                                midiEvent = new MidiEvent(event, totalDuration);
                                track.add(midiEvent);
                            }
                            // Any other value is basically reserved
                            break;

                        case (byte) 0xE2: // Volume change
                            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Adding volume change " + ((eventValue & 0x3F) * 2) +" to channel " + eventChannel);
                            event.setMessage(ShortMessage.CONTROL_CHANGE, eventChannel, 7, (eventValue & 0x3F) * 2);
                            midiEvent = new MidiEvent(event, totalDuration);
                            track.add(midiEvent);
                            break;

                        case (byte) 0xE3: // Panpot change
                            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Adding panpot value" + ((eventValue & 0x3F) * 2) + " to channel " + eventChannel);
                            event.setMessage(ShortMessage.CONTROL_CHANGE, eventChannel, 10, (eventValue & 0x3F) * 2);
                            midiEvent = new MidiEvent(event, totalDuration);
                            track.add(midiEvent);
                            break;

                        case (byte) 0xE4: // Pitch Bend (Basing off of CMF's Table)
                            
                            if ((eventValue & 0x3F) < 32) { centsAdjustment = -((32 - (eventValue & 0x3F)) * channelData[eventChannel].pitchBendRange) * 100; } 
                            else { centsAdjustment = (((eventValue & 0x3F) - 32) * channelData[eventChannel].pitchBendRange) * 100; }

                            pitchBendValue = (centsAdjustment * 32) / 100;
                            pitchBendValue += 8192;

                            // Separate the pitch bend value into MSB and LSB, as we need to send both separately
                            lsb = pitchBendValue & 0x7F;
                            msb = (pitchBendValue >> 7) & 0x7F;

                            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Adding pitch bend MSB " + msb + " LSB " + lsb + " to channel " + eventChannel);
                            event.setMessage(ShortMessage.PITCH_BEND, eventChannel, lsb, msb);
                            midiEvent = new MidiEvent(event, totalDuration);
                            track.add(midiEvent);
                            break;

                        case (byte) 0xE5: // CHANNEL_ASSIGN
                            Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Channel Assign event not implemented! Voice: " + (eventValue & 0xC0) + " Channel:" + (eventValue & 0x0F));
                            channelData[eventChannel].assignedChannel = (byte) (eventValue & 0x0F);
                            break;

                        case (byte) 0xE6: // Expression change
                            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Adding Expression change event value:" + ((eventValue & 0x3F) * 2) + " to channel" + eventChannel);
                            event.setMessage(ShortMessage.CONTROL_CHANGE, eventChannel, 11, (eventValue & 0x3F) * 2);
                            midiEvent = new MidiEvent(event, totalDuration);
                            track.add(midiEvent);
                            break;

                        case (byte) 0xE7: // PITCH_BEND_RANGE
                            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Adding pitch bend range " + (eventValue & 0x3F) + " to channel " + eventChannel);
                            channelData[eventChannel].pitchBendRange = (byte) (eventValue & 0x3F);
                            break;

                        case (byte) 0xE8: // FINE_PITCH_BEND_A / WAVE_CHANNEL_VOLUME
                            Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Fine Pitch Bend A (Wave volume) not implemented! Value: " + ((eventValue & 0x3F) * 2));
                            event.setMessage(ShortMessage.CONTROL_CHANGE, eventChannel, 7, (eventValue & 0x3F) * 2);
                            midiEvent = new MidiEvent(event, totalDuration);
                            track.add(midiEvent);
                            break;

                        case (byte) 0xE9: // FINE_PITCH_BEND_B / WAVE_CHANNEL_PANPOT
                            Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Fine Pitch Bend B (Wave Panpot) not implemented! Value: " + (eventValue & 0x3F));
                            event.setMessage(ShortMessage.CONTROL_CHANGE, eventChannel, 10, (eventValue & 0x3F) * 2);
                            midiEvent = new MidiEvent(event, totalDuration);
                            track.add(midiEvent);
                            break;
                        
                        case (byte) 0xEA: // MODULATION DEPTH
                            Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Adding modulation depth value " + (eventValue & 0x3F) + "(" + ((eventValue & 0x3F) * 2) + ") to channel " + eventChannel);
                            event.setMessage(ShortMessage.CONTROL_CHANGE, eventChannel, 1, (eventValue & 0x3F) * 2);
                            midiEvent = new MidiEvent(event, totalDuration);
                            track.add(midiEvent);
                            break;

                        case (byte) 0xB9: // PART_CONFIGURATION
                            Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Part Configuration event is reserved!");
                            break;

                        case (byte) 0xBD: // PAUSE
                            Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Adding Pause event to sequence");
                            event.setMessage(ShortMessage.STOP);
                            midiEvent = new MidiEvent(event, totalDuration);
                            track.add(midiEvent);

                        case (byte) 0xBF: // RESET
                            Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Adding Reset event to sequence");
                            event.setMessage(ShortMessage.SYSTEM_RESET);
                            midiEvent = new MidiEvent(event, totalDuration);
                            track.add(midiEvent);
                            break;
                            
                        case (byte) 0xBE: // STOP
                            Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Skipping event type: " + String.format("0x%02X", eventParam));
                            // According to CMF, this sets all notes to off at current duration (which here means just stopping any previously playing notes)
                            for(int i = 0; i < channelData.length; i++) 
                            {
                                ShortMessage noteOff = new ShortMessage();
                                noteOff.setMessage(ShortMessage.NOTE_OFF, i, channelData[i].lastNote, 0);
                                midiEvent = new MidiEvent(noteOff, totalDuration);
                                track.add(midiEvent);
                            }
                            break;
                        
                        // Unknown status bytes
                        case (byte) 0x8C: // Bomberman '08 uses this
                        case (byte) 0x92: // Bomberman '08 uses this
                        default:
                            // Unknown status
                            Mobile.log(Mobile.LOG_WARNING, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Unknown status byte: " + String.format("0x%02X", eventParam));
                            break;
                    }
                }
                else if (eventParam > (byte) 0xEF && eventParam <= (byte) 0xFF) // ExtInfo Status Message, SysEx
                {
                    int length = (data[offset++] & 0xFF) << 8 | (data[offset++] & 0xFF);
                    Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Ext info event. Type: " + extInfoTypes[eventParam & 0x0F]);
                    byte[] evtData = new byte[2+length];
                    evtData[0] = (byte) ((length / 0x100) & 0xff);
                    evtData[1] = (byte) ((length % 0x100) & 0xff);
                    for(int i = 0; i < length; i++) 
                    {
                        evtData[i+2] = (byte) (data[offset++] & 0xFF);
                    }
                }
            }
            else // If it didn't match above, we're dealing with a note message
            {
                byte eventChannelIndex = (byte) ((status >> 6) & 0x03);
                eventChannel = (byte) (curTrack * 4 + eventChannelIndex);
                noteNumber = (byte) ((status & 0x3F) + 45); // Note 15 in CMF/MFi is C4, which means that all note values should be increased by 45 (60 is C4 in midi)
        
                if(offset >= data.length) { return; } // TODO: This shouldn't be needed. Most likely it is the result of an improper parsing of events above
                gateTime = Math.round((data[offset++] & 0xFF) * getMillisecondsPerTick());

                // TODO: On SMAF documentation, gateTime cannot be zero. This indicates either a corrupted file or a parse error... let's assume the same for CMF/MFi
                if (gateTime <= 0) 
                {
                    Mobile.log(Mobile.LOG_ERROR, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "note gateTime value cannot be zero. ");
                    return;
                }

                if(!noteHas3Bytes && offset < data.length) // Notes with 4 bytes have an additional byte for octaveShift and velocity
                {
                    byte additionalByte = (byte) (data[offset++] & 0xFF);
                    velocity = (byte) ((additionalByte >> 2 & 0x3F) * 2);
                    octaveShift = (byte) (additionalByte & 0x03);

                    switch (octaveShift) 
                    {
                        case 0: // No change
                            break; 
                        case 1: // Increase one octave
                            noteNumber += 12;
                            break; 
                        case 2: // Decrease two octaves
                            noteNumber -= 24;
                            break; 
                        case 3: // Decrease one octave
                            noteNumber -= 12;
                            break; 
                    }
                }

                // Make sure the resulting note is still within range. We probably don't need this, but better safe than sorry
                if (noteNumber < 0) { noteNumber = 0; } 
                else if (noteNumber > 127) { noteNumber = 127; }
                
                Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " + "Adding note value " + noteNumber + " to channel " + eventChannel);
                
                ShortMessage noteOn = new ShortMessage();
                noteOn.setMessage(ShortMessage.NOTE_ON, channelData[eventChannel].assignedChannel == -1 ? eventChannel : channelData[eventChannel].assignedChannel, noteNumber, velocity);
                midiEvent = new MidiEvent(noteOn, totalDuration);
                track.add(midiEvent);
                
                channelData[eventChannel].lastNote = noteNumber;

                ShortMessage noteOff = new ShortMessage();
                noteOff.setMessage(ShortMessage.NOTE_OFF, channelData[eventChannel].assignedChannel == -1 ? eventChannel : channelData[eventChannel].assignedChannel, channelData[eventChannel].lastNote, 0);
                midiEvent = new MidiEvent(noteOff, totalDuration+gateTime);
                track.add(midiEvent);
                continue;
            }
        }
        Mobile.log(Mobile.LOG_DEBUG, MLDDecoder.class.getPackage().getName() + "." + MLDDecoder.class.getSimpleName() + ": " +"Sequence finished. Returning.");
    }

    // Doesn't translate directly to MIDI ticks, those run at a faster rate so we can round tempo and timebase adjustments with good enough precision.
    public static final float getMillisecondsPerTick() 
    { 
        float tickMultiplier = 120000.0f / (globalTempo * globalTimebase); // With the default timeBase of 48 and tempo of 125, this should result in a duration of 10ms, and the ratio adjusts based on timebase and tempo
        return tickMultiplier; // Return the multiplier for gateTime and totalDuration, as we can't really change the MIDI sequence's PPQ after creating it
    }
}

class MLDChannelData 
{
    public byte pitchBendRange, lastNote, assignedChannel;

    MLDChannelData() 
    {
        assignedChannel = -1;
        pitchBendRange = 2; // Default according to CMF
        lastNote = 0;
    }
}