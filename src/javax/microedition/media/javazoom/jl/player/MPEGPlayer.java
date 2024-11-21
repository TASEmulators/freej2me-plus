	/*
	* 11/19/04		1.0 moved to LGPL.
	* 29/01/00		Initial version. mdm@techie.com
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

	package javazoom.jl.player;

	import java.io.*;

	import javazoom.jl.decoder.Bitstream;
	import javazoom.jl.decoder.BitstreamException;
	import javazoom.jl.decoder.Decoder;
	import javazoom.jl.decoder.Header;
	import javazoom.jl.decoder.JavaLayerException;
	import javazoom.jl.decoder.SampleBuffer;

	import javax.microedition.media.Player;
	import org.recompile.mobile.Mobile;

	/**
	 * The <code>Player</code> class implements a simple player for playback
	* of an MPEG audio stream.
	*
	* @author	Mat McGowan
	* @since	0.0.8
	*/

	// REVIEW: the audio device should not be opened until the
	// first MPEG audio frame has been decoded. 
	public class MPEGPlayer
	{
		/**
		 * The current frame number.
		*/
		private int frame = 0;

		/**
		 * The MPEG audio bitstream.
		*/
		// javac blank final bug.
		/*final*/ private Bitstream		bitstream;

		/**
		 * The MPEG audio decoder.
		*/
		/*final*/ private Decoder		decoder;

		/**
		 * The AudioDevice the audio samples are written to.
		*/
		private AudioDevice	audio;

		/**
		 * Has the player been closed?
		*/
		private boolean		closed = false;

		/**
		 * Has the player played back all frames from the stream?
		*/
		private boolean		complete = false;

		private int			lastPosition = 0;

		private boolean paused;

		private int bitrate;

		private byte[] data;
		private int dataIndex;
		public boolean isBuffered;
		private InputStream dataStream;

		private int positionOffset;

		private int vol;
		private boolean reset;
		private int loopCount;

		/**
		 * Creates a new <code>Player</code> instance.
		*/
		public MPEGPlayer(InputStream stream, boolean buffer) throws JavaLayerException, IOException { this(stream, null, buffer); }

		public MPEGPlayer(InputStream stream, AudioDevice device, boolean buffer) throws JavaLayerException, IOException {
			if (stream instanceof ByteArrayInputStream) { buffer = true; }
			if (buffer) 
			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream(stream.available());
				byte[] b = new byte[2048];
				int r;
				while ((r = stream.read(b)) != -1) { baos.write(b, 0, r); }
				data = baos.toByteArray();
				stream.close();
				stream = dataStream = new InputStream() 
				{
					public int read() throws IOException 
					{
						if (dataIndex == data.length) return -1;
						return data[dataIndex++];
					}

					public int read(byte[] b, int i, int len) throws IOException 
					{
						if (len > data.length - dataIndex) { len = data.length - dataIndex; }
						if (len == 0) return -1;
						System.arraycopy(data, dataIndex, b, i, len);
						dataIndex += len;
						return len;
					}
				};
			}
			isBuffered = buffer;
			vol = 100;
			this.lastPosition = 0;
			bitstream = new Bitstream(stream);
			decoder = new Decoder();
			if (device!=null)
			{
				audio = device;
			}
			else
			{
				FactoryRegistry r = FactoryRegistry.systemRegistry();
				audio = r.createAudioDevice();
			}
			audio.open(decoder);
		}

		public boolean play() throws JavaLayerException, InterruptedException { return play(Integer.MAX_VALUE); }

		/**
		 * Plays a number of MPEG audio frames.
		*
		* @param frames	The number of frames to play.
		* @return	true if the last frame was played, or false if there are
		*			more frames.
		*/
		public boolean play(int frames) throws JavaLayerException, InterruptedException
		{
			paused = false;
			boolean ret = true;

			while (frames-- > 0 && ret)
			{
				if(paused) { return false; }
				if(closed) 
				{
					ret = false;
					break;
				}

				if (reset) 
				{
					Mobile.log(Mobile.LOG_WARNING, MPEGPlayer.class.getPackage().getName() + "." + MPEGPlayer.class.getSimpleName() + ": " + "play locked");
					synchronized (dataStream) { dataStream.wait(); }
					Mobile.log(Mobile.LOG_WARNING, MPEGPlayer.class.getPackage().getName() + "." + MPEGPlayer.class.getSimpleName() + ": " + "play unlocked");
				}
				//int i = audio.getPosition();
				try { ret = decodeFrame(); } 
				catch (JavaLayerException e) { if (!reset) throw e; }

				//int j = audio.getPosition() - i;
				//	Mobile.log(Mobile.LOG_DEBUG, MPEGPlayer.class.getPackage().getName() + "." + MPEGPlayer.class.getSimpleName() + ": " + "fps: " + ((1000-j)/j) + " ("+j+"ms)");
			}
			if (!ret)
			{
				// last frame, ensure all data flushed to the audio device.
				AudioDevice out = audio;
				if (out!=null)
				{
					out.flush();
					synchronized (this)
					{
						/* Only set complete flag if not entering a new loop */
						complete = (!closed);
					}
				}
			}
			paused = true;
			return ret;
		}

		public void reset() 
		{
			if (!isBuffered) { return; }

			Mobile.log(Mobile.LOG_DEBUG, MPEGPlayer.class.getPackage().getName() + "." + MPEGPlayer.class.getSimpleName() + ": " + "reset");
			dataIndex = positionOffset = 0;
			reset = true;
			try 
			{
				if (this.bitstream != null) 
				{
					this.bitstream.close();
					this.bitstream = null;
				}
				this.bitstream = new Bitstream(dataStream);
				decoder = new Decoder();
				if (audio != null) 
				{
					audio.close();
					audio = null;
				}
				FactoryRegistry r = FactoryRegistry.systemRegistry();
				audio = r.createAudioDevice();
				audio.open(decoder);
				closed = false;
				paused = false;
			} catch (Exception e) { e.printStackTrace(); }
			reset = false;

			synchronized (dataStream) { dataStream.notifyAll(); }
			Mobile.log(Mobile.LOG_DEBUG, MPEGPlayer.class.getPackage().getName() + "." + MPEGPlayer.class.getSimpleName() + ": " + "reset done");
		}

		public int getBitrate() {
			if(bitrate == 0) {
				try {
					bitrate = bitstream.readFrame().bitrate();
				} catch (Exception ignored) {}
			}
			return bitrate;
		}

		/**
		 * Cloases this player. Any audio currently playing is stopped
		* immediately.
		*/
		public synchronized void close()
		{
			this.closed = true;
			AudioDevice a;
			if ((a = this.audio) != null) {
				audio = null;
				a.close();
				this.lastPosition = a.getPosition();
				try {
					this.bitstream.close();
					this.data = null;
				} catch (BitstreamException ignored) {}
			}
		}

		/**
		 * Returns the completed status of this player.
		*
		* @return	true if all available MPEG audio frames have been
		*			decoded, or false otherwise.
		*/
		public synchronized boolean isComplete() { return complete; }

		/**
		 * Retrieves the position in milliseconds of the current audio
		* sample being played. This method delegates to the <code>
		* AudioDevice</code> that is used by this player to sound
		* the decoded audio samples.
		*/
		public int getPosition()
		{
			int position = lastPosition;

			AudioDevice out = audio;
			if (out!=null)
			{
				position = out.getPosition();
			}
			return position + positionOffset;
		}

		/**
		 * Decodes a single frame.
		*
		* @return true if there are no more frames to decode, false otherwise.
		*/
		protected boolean decodeFrame() throws JavaLayerException
		{
			try
			{
				AudioDevice out = audio;
				if (out==null) { return false; }

				Header h = bitstream.readFrame();

				if (h==null) { return false; }
				bitrate = h.bitrate();

				// sample buffer set when decoder constructed
				SampleBuffer output = (SampleBuffer)decoder.decodeFrame(h, bitstream);

				synchronized (this)
				{
					out = audio;
					if (out!=null)
					{
						out.setVolume(vol);
						out.write(output.getBuffer(), 0, output.getBufferLength());
					}
				}

				bitstream.closeFrame();
			}
			catch (RuntimeException ex) { throw new JavaLayerException("Exception decoding audio frame", ex); }
			
			return true;
		}

		/**
		 * skips over a single frame
		* @return false	if there are no more frames to decode, true otherwise.
		*/
		protected boolean skipFrame() throws JavaLayerException
		{
			Header h = bitstream.readFrame();
			if (h == null) return false;
			bitstream.closeFrame();
			return true;
		}

		public float framesPerSecond() { return framesPerSecond(bitstream.header); }

		public static float framesPerSecond(Header h) 
		{
			if(h.frequency() == 0) { return 0; }
			float o = 1;
			switch(h.frequency()) 
			{
			case 44100:
				o = 38f + 1 / 3f;
				break;
			case 32000:
				o = 27.6f;
				break;
			case 48000:
				o = 42f;
				break;
			default:
				int i = h.frequency();
				o = 38.5f;
				o = o / 44100;
				o = o * i;
			}
			return o;
		}


		public boolean skip(final int skip, Header h) throws JavaLayerException
		{
			if(h == null) { h = bitstream.header; }
			bitrate = h.bitrate();
			positionOffset = skip;
			float o = framesPerSecond(h);
			int offset = (int)((skip / 1000D) * o);
			Mobile.log(Mobile.LOG_DEBUG, MPEGPlayer.class.getPackage().getName() + "." + MPEGPlayer.class.getSimpleName() + ": " + "skip: "+ skip + "ms" + " d: " + o + " off: "+ offset);
			boolean ret = true;
			int initial = offset;
			while (offset-- > 0 && ret) {
				ret = skipFrame();
				try {
					double i = initial;
					double d = (i-offset)/i;
					int l = (int)(d*skip);
					//if(listener != null && offset % 2 == 1) listener.positionChanged(l);
				} catch (ArithmeticException ignored) {}
			}
			return ret;
		}

		public int getFrame() { return frame; }

		public void setLevel(int n) 
		{
			vol = n;
			if(audio != null) audio.setVolume(n);
		}

		public void stop() { paused = true; }

		public Bitstream bitstream() { return bitstream; }

	/* 
	 * This player works in milliseconds. setMicrosecondPosition actually sets in milliseconds, but 
	 * since getMicrosecondPosition converts to microseconds, the jar will get the resulting microsecond
	 * position, aligning to the j2me docs.
	 */
	public void setMicrosecondPosition(long position) 
	{
		try
		{
			Header hdr = bitstream.readFrame();
			reset();
			skip((int) (position / 1000L), hdr);
		} catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, MPEGPlayer.class.getPackage().getName() + "." + MPEGPlayer.class.getSimpleName() + ": " + "MPEGPlayer: Failed to set microsecond position:" + e.getMessage());}
	}

	/* getPosition returns in milliseconds, so multiplying by 1000 gives us the result in microseconds */
	public long getMicrosecondPosition() { return getPosition() * 1000L; }

	public long getDuration() 
	{ 
		double duration = 0;

		try { duration = (double) ((data.length * 8 * 1_000D) / getBitrate()); } 
		catch (Exception e){ Mobile.log(Mobile.LOG_ERROR, MPEGPlayer.class.getPackage().getName() + "." + MPEGPlayer.class.getSimpleName() + ": " + "Couldn't get duration:" + e.getMessage()); return Player.TIME_UNKNOWN;}
		
		return (long) duration; 
	}

	public void setLoopCount(int count) 
	{
		if(count == -1) { loopCount = Integer.MAX_VALUE; } // Loop "indefinitely"
		else { loopCount = count; }
	}

	public boolean isRunning() 
	{
		if(!paused && !closed && !reset) { return true; }
		else { return false; }
	}

	// Looping is handled by PlatformPlayer
	public int getLoopCount() { return loopCount; }

	public void decreaseLoopCount() { loopCount--; }
}
