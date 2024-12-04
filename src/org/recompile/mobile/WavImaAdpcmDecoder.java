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
package org.recompile.mobile;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.recompile.mobile.Mobile;

public final class WavImaAdpcmDecoder
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

	private static final byte PCMHEADERSIZE = 44;
	private static final byte PCMPREAMBLESIZE = 16;
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
		int inputIndex = 0, outputIndex = PCMHEADERSIZE; /* Give some space for the header by starting from byte 44. */
		short decodedSample;

		/* 
		 * Make sure that the output size is 4 times as big as input's due to IMA ADPCM being able to pack 4 bytes of standard PCM 
		 * data into a single one, with 44 additional bytes in place to accomodate for the new header that will be created afterwards.
		 *
		 * Also, calculate the final expected output size right here instead of decreasing the output size on each preamble read,
		 * checking if the final size doesn't match what was initially expected (if there's at least one preamble, it won't)
		 * and then copying everything into a new byte array, which is too costly and can be cut entirely by just passing the 
		 * correct output size right at the start. This is done because preamble data should not be added into the decoded stream,
		 * and each preamble is 4*numChannels bytes long on IMA ADPCM, which means it would take 16*numChannels bytes on the decoded stream 
		 * for each preamble added.
		 */
		final byte[] output = new byte[(inputSize * 4) + PCMHEADERSIZE - ((PCMPREAMBLESIZE*numChannels) * (int) java.lang.Math.floor(inputSize / frameSize))];

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
				Mobile.log(Mobile.LOG_WARNING, WavImaAdpcmDecoder.class.getPackage().getName() + "." + WavImaAdpcmDecoder.class.getSimpleName() + ": " + "Remaining Bytes:" + (inputSize-inputIndex) + " < 16, cannot decode the last few stereo samples. Adding silence instead."); 
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

				// Check byte 2 of the preamble beforehand and notify if there's any problem. TODO: On valid streams, there shouldn't be any problem, but this is triggered sometimes. Have to find out why.
				if((input[inputIndex + 2]) > 88 || (input[inputIndex + 2]) < 0) 
				{ 
					Mobile.log(Mobile.LOG_WARNING, WavImaAdpcmDecoder.class.getPackage().getName() + "." + WavImaAdpcmDecoder.class.getSimpleName() + ": " + "Malformed block header (Mono/L-Channel). Decoded stream might sound bad.");
				}
				/* Bytes 0 and 1 describe the chunk's initial predictor value (little-endian), clamp it even in case of issues such as to try and preserve the decoded stream's quality. */
				predictedSample[LEFTCHANNEL] = (short) Math.max(Short.MIN_VALUE, Math.min(((input[inputIndex])) | ((input[inputIndex+1]) << 8), Short.MAX_VALUE));
				/* Byte 2 is the chunk's initial index on the step_size_table. Clamp as well */
				tableIndex[LEFTCHANNEL] = (byte) Math.max(0, Math.min(input[inputIndex+2], 88));
				inputIndex += 4;
				
				if (numChannels == 2) /* If we're dealing with stereo IMA ADPCM: */
				{
					if((input[inputIndex + 2]) > 88 || (input[inputIndex + 2]) < 0) 
					{ 
						Mobile.log(Mobile.LOG_WARNING, WavImaAdpcmDecoder.class.getPackage().getName() + "." + WavImaAdpcmDecoder.class.getSimpleName() + ": " + "Malformed block header (R-Channel). Decoded stream might sound bad.");
					}
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

		// TODO: This is maybe inconsequential, but the decoder is leaving a bit of garbage at the end of the output. This is a workaround for it.
		for(int i = 0; i < 512; i++) { output[output.length-i-1] = (byte) 0; }

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
	
	/*
	 * Since the header is always expected to be positioned right at the start
	 * of a byte array, read it to determine the WAV type.
	 * 
	 * Optionally it also returns some information about the audio format to help build a 
	 * new header for the decoded stream.
	*/
	public static final int[] readHeader(InputStream input) throws IOException 
	{
		/*
			The header of a WAV (RIFF) file is 44 bytes long and has the following format:

			CHAR[4] "RIFF" header
			UINT32  Size of the file (chunkSize).
			  CHAR[4] "WAVE" format
				CHAR[4] "fmt " header
				UINT32  SubChunkSize (examples: 12 for PCM unsigned 8-bit )
				  UINT16 AudioFormat (ex: 1 [PCM], 17 [IMA ADPCM] )
				  UINT16 NumChannels
				  UINT32 SampleRate
				  UINT32 BytesPerSec (samplerate*frame size)
				  UINT16 frameSize or blockAlign (256 on some gameloft games)
				  UINT16 BitsPerSample (gameloft games appear to use 4)
				CHAR[4] "data" header
				UINT32 Length of sample data.
				<Sample data>

			-- IMA ADPCM introduces the following before "data" header, and after BitsPerSample:
			UINT16 ByteExtraData
			UINT16 ExtraData
			CHAR[4] "fact" header
			UINT32 SubChunk2Size
			UINT32 NumOfSamples
		*/

		String riff = readInputStreamASCII(input, 4); // 0 - 4
		int dataSize = readInputStreamInt32(input);  // 4 - 8
		String format = readInputStreamASCII(input, 4);  // 8 - 12
		String fmt = readInputStreamASCII(input, 4);  // 12 - 16
		int chunkSize = readInputStreamInt32(input);  // 16 - 20
		short audioFormat = (short) readInputStreamInt16(input);  // 20 - 22
		short audioChannels = (short) readInputStreamInt16(input);  // 22 - 24
		int sampleRate = readInputStreamInt32(input);  // 24 - 28
		int bytesPerSec = readInputStreamInt32(input); // 28 - 32
		short frameSize = (short) readInputStreamInt16(input); // 32 - 34
		short bitsPerSample = (short) readInputStreamInt16(input); // 34 - 36
		
		// These are conditionally read depending on IMA ADPCM
		short ByteExtraData = 0;
		short ExtraData = 0;
		String factHeader = "";
		int SubChunk2Size = 0;
		int numOfSamples = 0;

		if(audioFormat == 0x11) 
		{
			ByteExtraData = (short) readInputStreamInt16(input); // 36 - 38 -- On IMA ADPCM
			ExtraData = (short) readInputStreamInt16(input); // 38 - 40 -- On IMA ADPCM
			factHeader = readInputStreamASCII(input, 4);  // 40 - 44 -- On IMA ADPCM
			SubChunk2Size = readInputStreamInt32(input);  // 44 - 48 -- On IMA ADPCM
			numOfSamples = readInputStreamInt32(input);  // 48 - 52 -- On IMA ADPCM
		}

		String dataHeader = readInputStreamASCII(input, 4);  // 36 - 40 -- On PCM WAV, 52-56 On IMA ADPCM
		int dataLen = readInputStreamInt32(input); // 40 - 44 -- On PCM WAV, 56-60 On IMA ADPCM

		Mobile.log(Mobile.LOG_DEBUG, WavImaAdpcmDecoder.class.getPackage().getName() + "." + WavImaAdpcmDecoder.class.getSimpleName() + ": " + (audioFormat == 0x11 ? "IMA ADPCM" : "PCM") + " WAV HEADER_START");

		Mobile.log(Mobile.LOG_DEBUG, WavImaAdpcmDecoder.class.getPackage().getName() + "." + WavImaAdpcmDecoder.class.getSimpleName() + ": " + riff);
		Mobile.log(Mobile.LOG_DEBUG, WavImaAdpcmDecoder.class.getPackage().getName() + "." + WavImaAdpcmDecoder.class.getSimpleName() + ": " + "FileSize:" + dataSize);
		Mobile.log(Mobile.LOG_DEBUG, WavImaAdpcmDecoder.class.getPackage().getName() + "." + WavImaAdpcmDecoder.class.getSimpleName() + ": " + "Format: " + format);

		Mobile.log(Mobile.LOG_DEBUG, WavImaAdpcmDecoder.class.getPackage().getName() + "." + WavImaAdpcmDecoder.class.getSimpleName() + ": " + "---'" + fmt + "' header---");
		Mobile.log(Mobile.LOG_DEBUG, WavImaAdpcmDecoder.class.getPackage().getName() + "." + WavImaAdpcmDecoder.class.getSimpleName() + ": " + "Header ChunkSize:" + Integer.toString(chunkSize));
		Mobile.log(Mobile.LOG_DEBUG, WavImaAdpcmDecoder.class.getPackage().getName() + "." + WavImaAdpcmDecoder.class.getSimpleName() + ": " + "AudioFormat: " + Integer.toString(audioFormat));
		Mobile.log(Mobile.LOG_DEBUG, WavImaAdpcmDecoder.class.getPackage().getName() + "." + WavImaAdpcmDecoder.class.getSimpleName() + ": " + "AudioChannels:" + Integer.toString(audioChannels));
		Mobile.log(Mobile.LOG_DEBUG, WavImaAdpcmDecoder.class.getPackage().getName() + "." + WavImaAdpcmDecoder.class.getSimpleName() + ": " + "SampleRate:" + Integer.toString(sampleRate));
		Mobile.log(Mobile.LOG_DEBUG, WavImaAdpcmDecoder.class.getPackage().getName() + "." + WavImaAdpcmDecoder.class.getSimpleName() + ": " + "BytesPerSec:" + Integer.toString(bytesPerSec));
		Mobile.log(Mobile.LOG_DEBUG, WavImaAdpcmDecoder.class.getPackage().getName() + "." + WavImaAdpcmDecoder.class.getSimpleName() + ": " + "FrameSize:" + Integer.toString(frameSize));
		Mobile.log(Mobile.LOG_DEBUG, WavImaAdpcmDecoder.class.getPackage().getName() + "." + WavImaAdpcmDecoder.class.getSimpleName() + ": " + "BitsPerSample:" + Integer.toString(bitsPerSample));
		
		if(audioFormat == 0x11) 
		{
			Mobile.log(Mobile.LOG_DEBUG, WavImaAdpcmDecoder.class.getPackage().getName() + "." + WavImaAdpcmDecoder.class.getSimpleName() + ": " + "ByteExtraData:" + Integer.toString(ByteExtraData));
			Mobile.log(Mobile.LOG_DEBUG, WavImaAdpcmDecoder.class.getPackage().getName() + "." + WavImaAdpcmDecoder.class.getSimpleName() + ": " + "ExtraData:" + Integer.toString(ExtraData));
			Mobile.log(Mobile.LOG_DEBUG, WavImaAdpcmDecoder.class.getPackage().getName() + "." + WavImaAdpcmDecoder.class.getSimpleName() + ": " + "---'" + factHeader +"' header---");
			Mobile.log(Mobile.LOG_DEBUG, WavImaAdpcmDecoder.class.getPackage().getName() + "." + WavImaAdpcmDecoder.class.getSimpleName() + ": " + "SubChunk2Size:" + Integer.toString(SubChunk2Size));
			Mobile.log(Mobile.LOG_DEBUG, WavImaAdpcmDecoder.class.getPackage().getName() + "." + WavImaAdpcmDecoder.class.getSimpleName() + ": " + "numOfSamples:" + Integer.toString(numOfSamples));
		}
		
		Mobile.log(Mobile.LOG_DEBUG, WavImaAdpcmDecoder.class.getPackage().getName() + "." + WavImaAdpcmDecoder.class.getSimpleName() + ": " + "---'" + dataHeader +"' header---");
		Mobile.log(Mobile.LOG_DEBUG, WavImaAdpcmDecoder.class.getPackage().getName() + "." + WavImaAdpcmDecoder.class.getSimpleName() + ": " + "SampleDataLength:" + Integer.toString(dataLen));

		Mobile.log(Mobile.LOG_DEBUG, WavImaAdpcmDecoder.class.getPackage().getName() + "." + WavImaAdpcmDecoder.class.getSimpleName() + ": " + "WAV HEADER_END");
		
		/* 
		 * We need the audio format to check if it's ADPCM or PCM, and the file's 
		 * dataSize, SampleRate and audioChannels to decode ADPCM and build a new header. 
		 */
		return new int[] {audioFormat, sampleRate, audioChannels, frameSize};
	}

	/* Read a 16-bit little-endian unsigned integer from input.*/
	private static final int readInputStreamInt16(InputStream input) throws IOException 
	{ return ( input.read() & 0xFF ) | ( ( input.read() & 0xFF ) << 8 ); }

	/* Read a 32-bit little-endian signed integer from input.*/
	private static final int readInputStreamInt32(InputStream input) throws IOException 
	{
		return ( input.read() & 0xFF ) | ( ( input.read() & 0xFF ) << 8 )
			| ( ( input.read() & 0xFF ) << 16 ) | ( ( input.read() & 0xFF ) << 24 );
	}

	/* Return a String containing 'n' Characters of ASCII/ISO-8859-1 text from input. */
	private static final String readInputStreamASCII(InputStream input, int nChars) throws IOException 
	{
		byte[] chars = new byte[nChars];
		readInputStreamData(input, chars, 0, nChars);
		return new String(chars, "ISO-8859-1");
	}

	/* Read 'n' Bytes from the InputStream starting from the specified offset into the output array. */
	private static final void readInputStreamData(InputStream input, byte[] output, int offset, int nBytes) throws IOException 
	{
		int end = offset + nBytes;
		while(offset < end) 
		{
			int read = input.read(output, offset, end - offset);
			if(read < 0) throw new java.io.EOFException();
			offset += read;
		}
	}

	/* 
	 * Builds a WAV header that describes the decoded ADPCM file on the first 44 bytes. 
	 * Data: little-endian, 16-bit, signed, same sample rate and channels as source IMA ADPCM.
	 */
	private static final void buildHeader(byte[] buffer, final short numChannels, final int sampleRate) 
	{ 
		final short bitsPerSample = 16;   /* 16-bit PCM */
		final short audioFormat = 1;      /* WAV linear PCM */
		final int subChunkSize = 16;      /* Fixed size for Wav Linear PCM */
		final int chunk = 0x52494646;     /* 'RIFF' */ 
		final int format = 0x57415645;    /* 'WAVE' */ 
		final int subChunk1 = 0x666d7420; /* 'fmt ' */ 
		final int subChunk2 = 0x64617461; /* 'data' */ 

		/* 
		 * We'll have 16 bits per sample, so each sample has 2 bytes, with that we just divide
		 * the size of the byte buffer (minus the header) by (bitsPerSample/8), then multiply by the amount 
		 * of channels.
		*/
		final int samplesPerChannel = (buffer.length - 44) / ((bitsPerSample / 8) * numChannels);

		/* 
		 * Frame size is fairly standard, and PCM's fixed sample size makes it so the frameSize is either 2 bytes 
		 * for mono, or 4 bytes for stereo.
		 */
		final short frameSize = (short) (numChannels * (bitsPerSample / 8));

		/* 
		 * Previously only took into account mono streams. And since we know the framesize and
		 * the amount of samples per channel, in a format that has a fixed amount of bits per sample,
		 * we can account for multiple audio channels on sampleDataLength with a simpler calculus:
		 */
		final int sampleDataLength = (samplesPerChannel * numChannels) * frameSize;

		/* 
		 * Represents how many bytes are streamed per second. With all of the data above, it's trivial to
		 * calculate by getting the sample rate, the amount of channels and bytes per sample (bitsPerSample / 8)
		 */
		final int bytesPerSec = sampleRate * numChannels * (bitsPerSample / 8);
		
		/* NOTE: ChunkSize includes the header, so sampleDataLength + 44, which is the byte size of our header */

		/* ChunkID */
		ByteBuffer.wrap(buffer, 0, 4).order(ByteOrder.BIG_ENDIAN).putInt(chunk);
		/* ChunkSize (or File size, "RIFF" and "WAVE" fiels from the header are not considered) */
		ByteBuffer.wrap(buffer, 4, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(sampleDataLength + 36);
		/* Format (WAVE) */
		ByteBuffer.wrap(buffer, 8, 4).order(ByteOrder.BIG_ENDIAN).putInt(format);
		/* SubchunkID (fmt) */
		ByteBuffer.wrap(buffer, 12, 4).order(ByteOrder.BIG_ENDIAN).putInt(subChunk1);
		/* SubchunkSize (or format chunk size) */
		ByteBuffer.wrap(buffer, 16, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(subChunkSize);
		/* Audioformat */
		ByteBuffer.wrap(buffer, 20, 2).order(ByteOrder.LITTLE_ENDIAN).putShort(audioFormat);
		/* NumChannels (will be the same as source ADPCM) */
		ByteBuffer.wrap(buffer, 22, 2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) numChannels);
		/* SampleRate (will be the same as source ADPCM) */
		ByteBuffer.wrap(buffer, 24, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(sampleRate);
		/* ByteRate (BytesPerSec) */
		ByteBuffer.wrap(buffer, 28, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(bytesPerSec);
		/* BlockAlign (Frame Size) */
		ByteBuffer.wrap(buffer, 32, 2).order(ByteOrder.LITTLE_ENDIAN).putShort(frameSize);
		/* BitsPerSample */
		ByteBuffer.wrap(buffer, 34, 2).order(ByteOrder.LITTLE_ENDIAN).putShort(bitsPerSample);
		/* Subchunk2ID (data) */
		ByteBuffer.wrap(buffer, 36, 4).order(ByteOrder.BIG_ENDIAN).putInt(subChunk2);
		/* Subchunk2 Size (sampledata length) */
		ByteBuffer.wrap(buffer, 40, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(sampleDataLength);
	}

	/* Decode the received IMA WAV ADPCM stream into a signed PCM16LE byte array, then return it to PlatformPlayer. */
	public static final byte[] decodeImaAdpcm(final InputStream stream, final int[] wavHeaderData) throws IOException
	{
		/* Remove the header from the stream, we shouldn't "decode" it as if it was a sample */
		stream.skip(IMAHEADERSIZE);

		final byte[] input = new byte[stream.available()];
		readInputStreamData(stream, input, 0, stream.available());

		final byte[] output = decodeADPCM(input, input.length, (short) wavHeaderData[2], wavHeaderData[3]);
		buildHeader(output, (short) wavHeaderData[2], wavHeaderData[1]); /* Builds a new header for the decoded stream. */

		return output;
	}
}
