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
import java.io.IOException;

import java.util.Arrays;

import org.recompile.mobile.Mobile;

public final class WAVImaADPCMDecoder
{

	/* Information about this audio format: https://wiki.multimedia.cx/index.php/IMA_ADPCM */

	/* 
	 * Variables to hold the previously decoded sample and step used, per channel (if needed) 
	 * "NOTE: Arrays that won't be reassigned (but its values can still change) and variables 
	 * that won't be changed are marked as final throughout the code to identify that they
	 * are not meant to be changed at any point, and to also optimize the decoder's
	 * execution as much as possible since FreeJ2ME has a habit of freezing when adpcm samples 
	 * are being decoded. So far only seems to happen in Java 8 and on my more limited devices."
	 *     - @AShiningRay
	 */
	private static final byte LEFTCHANNEL = 0;
	private static final byte RIGHTCHANNEL = 1;

	private static final byte IMAHEADERSIZE = 60;
	
	private static final byte[] ima_step_index_table = 
	{
		-1, -1, -1, -1, 2, 4, 6, 8, 
		-1, -1, -1, -1, 2, 4, 6, 8
	};
	
	private static final short[] ima_step_size_table = 
	{
		7, 8, 9, 10, 11, 12, 13, 14, 16, 17,
		19, 21, 23, 25, 28, 31, 34, 37, 41, 45,
		50, 55, 60, 66, 73, 80, 88, 97, 107, 118,
		130, 143, 157, 173, 190, 209, 230, 253, 279, 307,
		337, 371, 408, 449, 494, 544, 598, 658, 724, 796,
		876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066,
		2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358,
		5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899,
		15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767
	};

	private static final short[] predictedSample = {0, 0};
	private static final byte[] tableIndex = {0, 0};

	/* 
	 * This method will decode IMA WAV ADPCM into linear PCM_S16LE.
	 */
	public static final byte[] decodeADPCM(final byte[] input, final int inputSize, final short numChannels, final int frameSize)
	{
		byte adpcmSample;
		byte curChannel;
		int inputIndex = 0, outputIndex = 0;
		short decodedSample;

		if(numChannels == 2) { Mobile.log(Mobile.LOG_WARNING, WAVImaADPCMDecoder.class.getPackage().getName() + "." + WAVImaADPCMDecoder.class.getSimpleName() + ": " + "Stereo IMA ADPCM decoding is untested.");  }

		/* 
		 * Make sure that the output size is 4 times as big as input's due to IMA ADPCM being able to pack 4 bytes of standard PCM 
		 * data into a single one, with 44 additional bytes in place to accomodate for the new header that will be created afterwards.
		 *
		 * For this reason, make it so the initial size for output is 4 times the size of the ADPCM input + the space required for the
		 * header it will have.
		 */
		final byte[] output = new byte[inputSize * 4];

		/* Initialize the predictor's sample and step values. */
		predictedSample[LEFTCHANNEL] = 0;
		tableIndex[LEFTCHANNEL] = 0;

		if(numChannels == 2) 
		{
			predictedSample[RIGHTCHANNEL] = 0;
			tableIndex[RIGHTCHANNEL] = 0;
		}

		while (inputIndex < inputSize) 
		{
			// If we don't have enough bytes left to do another stereo run here, return (or else we risk an OOB access and the whole stream gets invalidated). TODO: Check if this is expected behavior.
			if(numChannels == 2 && (inputSize - inputIndex) < 16) 
			{
				Mobile.log(Mobile.LOG_WARNING, WAVImaADPCMDecoder.class.getPackage().getName() + "." + WAVImaADPCMDecoder.class.getSimpleName() + ": " + "Remaining Bytes:" + (inputSize-inputIndex) + " < 16, cannot decode the last few stereo samples. Adding silence instead."); 
			 	break;
			} 

			/* Check if the decoder is at the beginning of a new chunk to see if the preamble needs to be read. */
			if (inputIndex % frameSize == 0)
			{
				/* 
				 * For each 4 bits used in IMA ADPCM, 16 must be used for PCM so adjust 
				 * indices and sizes accordingly. Byte 3 is reserved and has no practical 
				 * use for us.
				 */

				/* Bytes 0 and 1 describe the chunk's initial predictor value (little-endian), clamp it even in case of issues such as to try and preserve the decoded stream's quality. */
				predictedSample[LEFTCHANNEL] = (short) Math.max(Short.MIN_VALUE, Math.min(((input[inputIndex])) | ((input[inputIndex+1]) << 8), Short.MAX_VALUE));
				/* Byte 2 is the chunk's initial index on the step_size_table. Clamp as well */
				tableIndex[LEFTCHANNEL] = (byte) Math.max(0, Math.min(input[inputIndex+2], 88));
				inputIndex += 4;
				
				if (numChannels == 2) /* If we're dealing with stereo IMA ADPCM: */
				{
					predictedSample[RIGHTCHANNEL] = (short) Math.max(Short.MIN_VALUE, Math.min(((input[inputIndex])) | ((input[inputIndex+1]) << 8), Short.MAX_VALUE));
					tableIndex[RIGHTCHANNEL] = (byte) Math.max(0, Math.min(input[inputIndex+2], 88));
					inputIndex += 4;
				}
			}

			/* 
			 * In the very rare cases where some j2me app might use stereo IMA ADPCM, 
			 * we should decode each audio channel. 
			 * 
			 * If the format is stereo, it is assumed to be interleaved, which means that
			 * the stream will have a left channel sample followed by a right channel sample,
			 * followed by a left... and so on. In ADPCM those samples are setup so that 4 bytes
			 * from the left channel are followed by 4 bytes of the right channel.
			 * 
			 * https://wiki.multimedia.cx/index.php/Microsoft_IMA_ADPCM.
			 */
			if (numChannels == 2) 
			{
				/* 
				 * So in the case it's a stereo stream, decode 8 nibbles from both left and right channels, interleaving
				 * them in the resulting PCM stream.
				 */
				for (byte i = 0; i < 8; i++) 
				{
					if(i < 4) { curChannel = LEFTCHANNEL; }
					else      { curChannel = RIGHTCHANNEL; }

					adpcmSample = (byte) (input[inputIndex] & 0x0f);
					decodedSample = decodeSample(curChannel, adpcmSample);
					output[outputIndex + ((i & 3) << 3) + (curChannel << 1)] = (byte) decodedSample;
					output[outputIndex + ((i & 3) << 3) + (curChannel << 1) + 1] = (byte) (decodedSample >> 8);

					adpcmSample = (byte) ((input[inputIndex] >> 4) & 0x0f);
					decodedSample = decodeSample(curChannel, adpcmSample);
					output[outputIndex + ((i & 3) << 3) + (curChannel << 1) + 4] = (byte) decodedSample;
					output[outputIndex + ((i & 3) << 3) + (curChannel << 1) + 5] = (byte) (decodedSample >> 8);
					inputIndex++;
				}
				outputIndex += 32;
			}
			else
			{
				/* 
				 * If it's mono, just decode nibbles from ADPCM into PCM data sequentially, there's no sample 
				 * interleaving to worry about, much less multiple channels, so we only use channel 0.
				 * 
				 * Decode the entire block here and only get out of the loop for preamble reads, or
				 * if we reached the end of the stream, because we don't really need
				 * to keep going back up to check all those if cases for every sample. 
				 */
				while(inputIndex % frameSize != 0 && inputIndex < inputSize) 
				{
					adpcmSample = (byte)(input[inputIndex] & 0x0f);
					decodedSample = decodeSample(LEFTCHANNEL, adpcmSample);
					output[outputIndex++] = (byte) decodedSample;
					output[outputIndex++] = (byte) (decodedSample >> 8);

					adpcmSample = (byte)((input[inputIndex] >> 4) & 0x0f);
					decodedSample = decodeSample(LEFTCHANNEL, adpcmSample);
					output[outputIndex++] = (byte) decodedSample;
					output[outputIndex++] = (byte) (decodedSample >> 8);
					inputIndex++;
				}
			}
		}

		return output;
	}

	/* This method will decode a single IMA ADPCM sample to linear PCM_S16LE sample. */
	private static final short decodeSample(final byte channel, final byte adpcmSample)
	{
		/* 
		 * This decode procedure is mostly based on the following document:
		 * https://www.cs.columbia.edu/~hgs/audio/dvi/IMA_ADPCM.pdf
		 */

		/* 
		 * Get the step size from the last table index saved for this channel, to be used when decoding 
		 * the new given sample. 
		 */
		final int stepSize = ima_step_size_table[tableIndex[channel]];
		
		/* 
		 * This follows the first optimization of the original IMA ADPCM diff calculation formula
		 * found in https://wiki.multimedia.cx/index.php/IMA_ADPCM ()
		 */
    	int diff = ((stepSize * (adpcmSample & 0x07)) + (stepSize >> 1)) >> 2;

		// Negate if the sign bit is set 
		if ((adpcmSample & 8) != 0) { diff = -diff; }

		/* 
		 * Clamps the value of decodedSample to that of a short data type. At this point, the decoded 
		 * sample should already fit nicely into a short type value range as per columbia's doc.
		 */
		predictedSample[channel] = (short) Math.max(Short.MIN_VALUE, Math.min(predictedSample[channel] + diff, Short.MAX_VALUE));

		/* Basically columbia doc's "calculate stepsize" snippet */
		tableIndex[channel] += ima_step_index_table[adpcmSample];
		tableIndex[channel] = (byte) Math.max(0, Math.min(tableIndex[channel], 88));

		return predictedSample[channel];
	}

	/* Decode the received IMA WAV ADPCM stream into a signed PCM16LE byte array, then return it to PlatformPlayer. */
	public static final byte[] decodeImaAdpcm(final InputStream stream, final int[] wavHeaderData) throws IOException
	{
		// wavHeaderData[5] contains the correct data length specified by the IMA header that may not match the file's size due to padding and alignment, which is why we use it instead of stream.available().
		final byte[] input = new byte[wavHeaderData[5]];
		WAVTools.readInputStreamData(stream, input, 0, wavHeaderData[5]);

		byte[] decodedData = decodeADPCM(input, wavHeaderData[5], (short) wavHeaderData[2], wavHeaderData[3]);

		return WAVTools.upsample(decodedData, wavHeaderData[1], WAVTools.hostSampleRate, (short) wavHeaderData[2], (short) 16, decodedData.length);
	}
}
