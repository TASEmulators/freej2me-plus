/*
 * 11/19/04		1.0 moved to LGPL.
 * 01/12/99		Initial version.	mdm@techie.com
 *-----------------------------------------------------------------------
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *----------------------------------------------------------------------
 */
 
package javazoom.jl.decoder;

/**
 * The <code>Decoder</code> class encapsulates the details of
 * decoding an MPEG audio frame. 
 * 
 * @author	MDM	
 * @version 0.0.7 12/12/99
 * @since	0.0.5
 */
public class Decoder implements DecoderErrors
{
	static private final Params DEFAULT_PARAMS = new Params();
	
	/**
	 * The Bistream from which the MPEG audio frames are read.
	 */
	//private Bitstream				stream;
	
	/**
	 * The Obuffer instance that will receive the decoded
	 * PCM samples.
	 */
	private Obuffer			output;
		
	/**
	 * Synthesis filter for the left channel.
	 */
	private SynthesisFilter			filter1;
	
	/**
	 * Sythesis filter for the right channel.
	 */
	private SynthesisFilter			filter2;	
			
	/**
	 * The decoder used to decode layer III frames.
	 */
	private LayerIIIDecoder			l3decoder;
	private LayerIIDecoder			l2decoder;
	private LayerIDecoder			l1decoder;
	
	private int						outputFrequency;
	private int						outputChannels;
	
	private boolean					initialized;
		
	
	/**
	 * Creates a new <code>Decoder</code> instance with default 
	 * parameters.
	 */
	
	public Decoder()
	{
		this(null);
	}

	/**
	 * Creates a new <code>Decoder</code> instance with default 
	 * parameters.
	 * 
	 * @param params	The <code>Params</code> instance that describes
	 *					the customizable aspects of the decoder.  
	 */
	public Decoder(Params params0)
	{
		if (params0==null)
			params0 = DEFAULT_PARAMS;
	}
	
	/**
	 * Decodes one frame from an MPEG audio bitstream.
	 * 
	 * @param header		The header describing the frame to decode.
	 * @param bitstream		The bistream that provides the bits for te body of the frame. 
	 * 
	 * @return A SampleBuffer containing the decoded samples.
	 */
	public Obuffer decodeFrame(Header header, Bitstream stream)
		throws DecoderException
	{
		if (!initialized)
		{
			initialize(header);
		}
		
		int layer = header.layer();
		
		output.clear_buffer();
		
		FrameDecoder decoder = retrieveDecoder(header, stream, layer);
		
		decoder.decodeFrame();
				
		output.write_buffer(1);
		
		return output;	
	}
	
	/**
	 * Changes the output buffer. This will take effect the next time
	 * decodeFrame() is called. 
	 */
	public void setOutputBuffer(Obuffer out)
	{
		output = out;
	}
	
	/**
	 * Retrieves the sample frequency of the PCM samples output
	 * by this decoder. This typically corresponds to the sample
	 * rate encoded in the MPEG audio stream.
	 * 
	 * @param the sample rate (in Hz) of the samples written to the
	 *		output buffer when decoding. 
	 */
	public int getOutputFrequency()
	{
		return outputFrequency;
	}
	
	/**
	 * Retrieves the number of channels of PCM samples output by
	 * this decoder. This usually corresponds to the number of
	 * channels in the MPEG audio stream, although it may differ.
	 * 
	 * @return The number of output channels in the decoded samples: 1 
	 *		for mono, or 2 for stereo.
	 *		
	 */
	public int getOutputChannels()
	{
		return outputChannels;	
	}
		
	protected DecoderException newDecoderException(int errorcode)
	{
		return new DecoderException(errorcode, null);
	}
	
	protected DecoderException newDecoderException(int errorcode, Throwable throwable)
	{
		return new DecoderException(errorcode, throwable);
	}
	
	protected FrameDecoder retrieveDecoder(Header header, Bitstream stream, int layer)
		throws DecoderException
	{
		FrameDecoder decoder = null;
		
		// REVIEW: allow channel output selection type
		// (LEFT, RIGHT, BOTH, DOWNMIX)
		switch (layer)
		{
		case 3:
			if (l3decoder==null)
			{
				l3decoder = new LayerIIIDecoder(stream, 
					header, filter1, filter2, 
					output, OutputChannels.BOTH_CHANNELS);
			}						
			
			decoder = l3decoder;
			break;
		case 2:
			if (l2decoder==null)
			{
				l2decoder = new LayerIIDecoder();
				l2decoder.create(stream, 
					header, filter1, filter2, 
					output, OutputChannels.BOTH_CHANNELS);				
			}
			decoder = l2decoder;
			break;
		case 1:
			if (l1decoder==null)
			{
				l1decoder = new LayerIDecoder();
				l1decoder.create(stream, 
					header, filter1, filter2, 
					output, OutputChannels.BOTH_CHANNELS);				
			}
			decoder = l1decoder;
			break;
		}
						
		if (decoder==null)
		{
			throw newDecoderException(UNSUPPORTED_LAYER, null);
		}
		
		return decoder;
	}
	
	private void initialize(Header header)
		throws DecoderException
	{
		
		// REVIEW: allow customizable scale factor
		float scalefactor = 32700.0f;
		
		int mode = header.mode();
		int layer = header.layer();
		int channels = mode==Header.SINGLE_CHANNEL ? 1 : 2;

					
		// set up output buffer if not set up by client.
		if (output==null)
			output = new SampleBuffer(header.frequency(), channels);
		
		filter1 = new SynthesisFilter(0, scalefactor);
   		
		// REVIEW: allow mono output for stereo
		if (channels==2) 
			filter2 = new SynthesisFilter(1, scalefactor);

		outputChannels = channels;
		outputFrequency = header.frequency();
		
		initialized = true;
	}
	
	/**
	 * The <code>Params</code> class presents the customizable
	 * aspects of the decoder. 
	 * <p>
	 * Instances of this class are not thread safe. 
	 */
	public static class Params implements Cloneable
	{		
		public Params()
		{			
		}
		
		public Object clone()
		{
			try
			{
				return super.clone();
			}
			catch (CloneNotSupportedException ex)
			{				
				throw new InternalError(this+": "+ex);
			}
		}
	};
}

