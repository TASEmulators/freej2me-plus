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

public final class WAVLawDecoder
{

	/* 
	 * This method will decode u-Law 8-bit PCM wav into linear PCM_S16LE.
	 */
	public static final byte[] decodeALaw(final byte[] input, final int[] wavHeaderData)
	{
		// Decoding based on https://www.ti.com/lit/an/spra163a/spra163a.pdf.

		// A-Law is also 1/2 compression, so each 8-bit sample is decompressed
		// to a 16-bit PCM one.
		final byte[] output = new byte[input.length * 2];

		boolean isNegative = false;
		int decodedSample;
        int step;
        int position;
		byte aLawSample;
        
		for (int i = 0; i < input.length; i++) 
		{

			// Most of the logic here is pretty similar for u-law.
			aLawSample = input[i];

			// a-law code has its even bits inverted for transmission, so we
			// need to invert them back first and foremost. 
			aLawSample = (byte) (aLawSample ^ 0x55);

			// Get state of the most significant (sign) bit, as it indicates
			// whether the decoded sample should be positive or negative.
			isNegative = ((aLawSample & 0x80) != 0);

			// We have to invert the sign bit again if the sample is negative
			if (isNegative) { aLawSample &= ~(1 << 7); }

			// We now get the a-step (mantissa) as well as the channel and
			// shift exponent to use in the decoding formula.
			step = (aLawSample & 0x0F);
			position = (aLawSample & 0xF0) >> 4;

			// Based on 'Equation 25' from the PDF.
			if (position != 4)
				decodedSample = (2 * step + 33) * (1 << position - 32) << 2;
			else
				decodedSample = (2 * step + 33) * (1 << position) << 2;

			// Instead of multiplying by the sign, we invert it here instead
			if(isNegative)
				decodedSample = -decodedSample;
			
            output[i * 2] = (byte) (decodedSample & 0xFF);
			output[i * 2 + 1] = (byte) ((decodedSample >> 8) & 0xFF);
        }

	return WAVTools.upsample(output, wavHeaderData[1], WAVTools.hostSampleRate,
							(short) wavHeaderData[2], (short) 16, output.length);
	}

	/* 
	 * This method will decode u-Law 8-bit PCM wav into linear PCM_S16LE.
	 */
	public static final byte[] decodeULaw(final byte[] input, final int[] wavHeaderData)
	{
		// Decoding based on https://www.ti.com/lit/an/spra163a/spra163a.pdf.

		// u-Law is 1/2 compression, so each 8-bit sample is decompressed to
		// a 16-bit PCM one.
		final byte[] output = new byte[input.length * 2];

		boolean isNegative = false;
		int decodedSample;
        int step;
        int position;
		byte uLawSample;

		for (int i = 0; i < input.length; i++) 
		{
			uLawSample = input[i];

			// u-law code is inverted for transmission, so we need to invert
			// the sample back first and foremost. 
			uLawSample = (byte) ~uLawSample;

			// Get state of the most significant (sign) bit, as it indicates
			// whether the decoded sample should be positive or negative.
			isNegative = ((uLawSample & 0x80) != 0);

			// We have to invert the sign bit again if the sample is negative
			if (isNegative) { uLawSample &= ~(1 << 7); }

			// We now get the u-step (mantissa) as well as the channel and
			// shift exponent to use in the decoding formula.
			step = (uLawSample & 0x0F);
			position = (uLawSample & 0xF0) >> 4;

			// This is 'Equation 17' from the PDF above.
			decodedSample = ((2 * step + 33) * (1 << position - 33)) << 3;

			// Instead of multiplying by the sign, we invert it here instead
			if(isNegative)
				decodedSample = -decodedSample;
			
            output[i * 2] = (byte) (decodedSample & 0xFF);
			output[i * 2 + 1] = (byte) ((decodedSample >> 8) & 0xFF);
        }

		return WAVTools.upsample(output, wavHeaderData[1], WAVTools.hostSampleRate, (short) wavHeaderData[2], (short) 16, output.length);
	}
}