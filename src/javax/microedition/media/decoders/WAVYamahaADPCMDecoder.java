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

public final class WAVYamahaADPCMDecoder 
{

    // Code adapted from https://github.com/superctr/adpcm, licensed as Public Domain
    private static final int[] ADPCMA_STEP_TABLE = 
    {
        16, 17, 19, 21, 23, 25, 28, 31,
        34, 37, 41, 45, 50, 55, 60, 66,
        73, 80, 88, 97, 107, 118, 130, 143,
        157, 173, 190, 209, 230, 253, 279, 307,
        337, 371, 408, 449, 494, 544, 598, 658,
        724, 796, 876, 963, 1060, 1166, 1282, 1411, 1552
    };

    private static final int[] ADPCMB_STEP_TABLE = 
    {
        57, 57, 57, 57, 77, 102, 128, 153
    };

    private static final int[] DELTA_TABLE = 
    {
        1, 3, 5, 7, 9, 11, 13, 15, -1, -3, -5, -7, -9, -11, -13, -15
    };

    private static final int[] ADJUST_TABLE = 
    {
        -1, -1, -1, -1, 2, 5, 7, 9
    };

    private static final int[] ADPCMZ_STEP_TABLE = 
    {
        230, 230, 230, 230, 307, 409, 512, 614
    };

    private static final int clamp(int x, int low, int high) { return (x > high) ? high : (x < low) ? low : x; }

    private static int stepSize, delta, out, adjustedStep, sign, diff, newval, nstep;

    private static final int ADPCMAStep(int step, int[] history, int[] stepHist) 
    {
        stepSize = ADPCMA_STEP_TABLE[stepHist[0]];
        delta = (DELTA_TABLE[step & 15] * stepSize) >> 3;
        out = (history[0] + delta) & 0xFFF; // No saturation
        //out |= (out & 0x800) != 0 ? ~0xFFF : 0;
        history[0] = out;
        adjustedStep = clamp(stepHist[0] + ADJUST_TABLE[step & 7], 0, 48);
        stepHist[0] = adjustedStep;
        return out;
    }

    private static final int ADPCMBStep(int step, int[] history, int[] stepSize) 
    {
        sign = step & 8;
        delta = step & 7;
        diff = ((1 + (delta << 1)) * stepSize[0]) >> 3;
        newval = history[0] + (sign > 0 ? -clamp(diff, 0, 32767) : clamp(diff, 0, 32767));
        nstep = ADPCMB_STEP_TABLE[delta] * stepSize[0] >> 6;

        stepSize[0] = clamp(nstep, 1280, 32767); // Seems to work better on a wide sample of PCM SMAF data
        //stepSize[0] = clamp(nstep, 127, 24576); // Original code's step clamping
        history[0] = newval = clamp(newval, -32768, 32767);

        return newval;
    }

    private static final int ADPCMZStep(int step, int[] history, int[] stepSize)
    {
        sign = step & 8;
        delta = step & 7;
        diff = ((1 + (delta << 1)) * stepSize[0]) >> 3;
        newval = history[0] + (sign > 0 ? -clamp(diff, 0, 32767) : clamp(diff, 0, 32767));
        nstep = ADPCMZ_STEP_TABLE[delta] * stepSize[0] >> 8;
        stepSize[0] = clamp(nstep, 127, 24576);
        history[0] = newval = clamp(newval, -32768, 32767);
        return newval;
    }

    public static final byte[] ADPCMADecode(byte[] buffer, int originalSampleRate, int numChannels) 
    {
        int[] history    = {0};
        int[] stepHist   = {0}; 
        byte[] outBuffer = new byte[buffer.length * 4]; // 4 bytes for each input byte (yamaha and ima adpcm go from 4 bits to 16)

        int outputIndex = 0, step = 0, decodedSample = 0;

        for (int i = 0; i < buffer.length; i++) 
        {
            // lower nibble
            step = (buffer[i] & 0x0F);
            decodedSample = ADPCMAStep(step, history, stepHist) << 4;
            outBuffer[outputIndex++] = (byte) (decodedSample & 0xFF);        // LSB
            outBuffer[outputIndex++] = (byte) ((decodedSample >> 8) & 0xFF); // MSB

            // upper nibble
            step = (buffer[i] >> 4) & 0x0F;
            decodedSample = ADPCMAStep(step, history, stepHist) << 4;
            outBuffer[outputIndex++] = (byte) (decodedSample & 0xFF);        // LSB
            outBuffer[outputIndex++] = (byte) ((decodedSample >> 8) & 0xFF); // MSB
        }

        return WAVTools.upsample(outBuffer, originalSampleRate, WAVTools.hostSampleRate, (short) numChannels, (short) 16, outBuffer.length);
    }

    public static final byte[] ADPCMBDecode(byte[] buffer, int originalSampleRate, int numChannels) 
    {
        int[] history    = {0};
        int[] stepSize   = {127};
        byte[] outBuffer = new byte[buffer.length * 4]; // 4 bytes per input byte

        int outputIndex = 0, step = 0, decodedSample = 0;

        for (int i = 0; i < buffer.length; i++) 
        {
            // lower nibble
            step = (buffer[i] & 0x0F);
            decodedSample = ADPCMBStep(step, history, stepSize);
            outBuffer[outputIndex++] = (byte) (decodedSample & 0xFF);        // LSB
            outBuffer[outputIndex++] = (byte) ((decodedSample >> 8) & 0xFF); // MSB

            // upper nibble
            step = (buffer[i] >> 4) & 0x0F;
            decodedSample = ADPCMBStep(step, history, stepSize);
            outBuffer[outputIndex++] = (byte) (decodedSample & 0xFF);        // LSB
            outBuffer[outputIndex++] = (byte) ((decodedSample >> 8) & 0xFF); // MSB
        }

        return WAVTools.upsample(outBuffer, originalSampleRate, WAVTools.hostSampleRate, (short) numChannels, (short) 16, outBuffer.length);
    }

    public static final byte[] ADPCMZDecode(byte[] buffer, int originalSampleRate, int numChannels) 
    {
        int[] history    = {0};
        int[] stepSize   = {127};
        byte[] outBuffer = new byte[buffer.length * 4]; // 4 bytes per input byte

        int outputIndex = 0, step = 0, decodedSample = 0;
        
        // TODO: The Yamaha YMZ/AICA chips perform low-pass filtering (not implemented at all)
        // and interpolation (done by upsample) to smooth out the resulting audio waves.

        for (int i = 0; i < buffer.length; i++) 
        {
            // Closest we have to "low-pass" right now is that
            // (sample * 224 / 255) operation to reduce the wave's amplitude.

            // lower nibble
            step = (buffer[i] & 0x0F);
            decodedSample = ADPCMZStep(step, history, stepSize) * 224 / 255;
            outBuffer[outputIndex++] = (byte) (decodedSample & 0xFF);        // LSB
            outBuffer[outputIndex++] = (byte) ((decodedSample >> 8) & 0xFF); // MSB

            // upper nibble
            step = (buffer[i] >> 4) & 0x0F;
            decodedSample = ADPCMZStep(step, history, stepSize) * 224 / 255;
            outBuffer[outputIndex++] = (byte) (decodedSample & 0xFF);        // LSB
            outBuffer[outputIndex++] = (byte) ((decodedSample >> 8) & 0xFF); // MSB
        }

        return WAVTools.upsample(outBuffer, originalSampleRate, WAVTools.hostSampleRate, (short) numChannels, (short) 16, outBuffer.length);
    }
}