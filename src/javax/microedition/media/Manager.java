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
package javax.microedition.media;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformPlayer;

public final class Manager
{
	public static final String TONE_DEVICE_LOCATOR = "device://tone";

	/* Default max amount of players in FreeJ2ME's config  */
	public static PlatformPlayer mediaPlayers[] = new PlatformPlayer[32];
	public static byte mediaPlayersIndex = 0;

	/* Midi Caching for better performance on certain VMs like OpenJDK 8 with jars that constantly load a similar set of streams. */
	private static Map<String, Byte> mediaCache = new HashMap<>();
	
	public static boolean dumpAudioStreams = false;

	public static Player createPlayer(InputStream stream, String type) throws IOException, MediaException
	{
		stream.mark(1024);
		String streamMD5 = generateMD5Hash(stream, 1024);
		stream.reset();

		if(dumpAudioStreams) 
		{
			// Copy the stream contents into a temporary stream to be saved as file
			final ByteArrayOutputStream streamCopy = new ByteArrayOutputStream();
			final byte[] copyBuffer = new byte[1024];
			int copyLength;
			while ((copyLength = stream.read(copyBuffer)) > -1 ) { streamCopy.write(copyBuffer, 0, copyLength); }
			streamCopy.flush();

			// Make sure the initial stream will still be available for FreeJ2ME
			stream = new ByteArrayInputStream(streamCopy.toByteArray());

			// And save the copy to the specified dir

			OutputStream outStream;
			String dumpPath = "." + File.separatorChar + "FreeJ2MEDumps" + File.separatorChar + "Audio" + File.separatorChar + Mobile.getPlatform().loader.suitename + File.separatorChar;
			File dumpFile = new File(dumpPath);

			if (!dumpFile.isDirectory()) { dumpFile.mkdirs(); }

			if(type.equalsIgnoreCase("audio/mid") || type.equalsIgnoreCase("audio/midi") || type.equalsIgnoreCase("sp-midi") || type.equalsIgnoreCase("audio/spmidi")) 
				{ dumpFile = new File(dumpPath + "Stream_" + streamMD5 + ".mid");}
			else if(type.equalsIgnoreCase("audio/x-wav") || type.equalsIgnoreCase("audio/wav")) { dumpFile = new File(dumpPath + "Stream_" + streamMD5 + ".wav");}
			else if(type.equalsIgnoreCase("audio/mpeg") || type.equalsIgnoreCase("audio/mp3")) { dumpFile = new File(dumpPath + "Stream_" + streamMD5 + ".mp3");}

			outStream = new FileOutputStream(dumpFile);

			streamCopy.writeTo(outStream);
		}

		/* If we currently have this stream's player cached, return it instantly to avoid creating a new player and its overhead */
		if (mediaCache.containsKey(streamMD5))
		{
			/* 
			 * We're basically "loading up" a new player as far as the MIDlet is concerned, 
			 * so make it seem as such by doing the following steps before returning it to the MIDlet:
			 * 1 - Stopping the player if it's not stopped (this will probably be removed once media playback loop is more mature)
			 * 2 - Setting the media playback time back to the start.
			 * 3 - Setting its state to PREFETCHED for good measure. 
			 */
			mediaPlayers[mediaCache.get(streamMD5)].stop();
			mediaPlayers[mediaCache.get(streamMD5)].setMediaTime(0);
			mediaPlayers[mediaCache.get(streamMD5)].prefetch();
			return mediaPlayers[mediaCache.get(streamMD5)]; 
		}

		// Otherwise, let's create and cache a new one.

		// If the index is out of bounds, we reached the end of our cache, go back to the start to find a position to free
		if(mediaPlayersIndex >= mediaPlayers.length) { mediaPlayersIndex = 0; }

		// Run through the entire cache index to find a suitable position to slot the new player in.
		for(; mediaPlayersIndex < mediaPlayers.length; mediaPlayersIndex++) 
		{
			if(mediaPlayers[mediaPlayersIndex] == null) { break; } /* A null position means we can use it right away */

			/* Otherwise, we prefer deallocating a position if it is not playing (running). */
			else if(mediaPlayers[mediaPlayersIndex] != null && mediaPlayers[mediaPlayersIndex].getState() == Player.PREFETCHED)
			{ 
				mediaPlayers[mediaPlayersIndex].cacheDeallocate();
				mediaCache.values().remove(mediaPlayersIndex);
				break;
			}
			/* If we ever reach this one, it's because all the other slots are used, and are playing. Deallocate the last cache position as a last resort. */
			else if(mediaPlayersIndex == mediaPlayers.length-1)
			{
				mediaPlayers[mediaPlayersIndex].cacheDeallocate();
				mediaCache.values().remove(mediaPlayersIndex);
				break;
			}
		}

		mediaPlayers[mediaPlayersIndex] = new PlatformPlayer(stream, type);
		mediaCache.put(streamMD5, mediaPlayersIndex);
		mediaPlayersIndex++;

		return mediaPlayers[mediaCache.get(streamMD5)];
	}

	public static Player createPlayer(String locator) throws MediaException
	{
		System.out.println("Create Player "+locator);
		return new PlatformPlayer(locator);
	}
	
	public static String[] getSupportedContentTypes(String protocol)
	{
		//System.out.println("Get Supported Media Content Types");
		return new String[]{"audio/midi", "audio/x-wav", 
		"audio/amr", "audio/mpeg"};
	}
	
	public static String[] getSupportedProtocols(String content_type)
	{
		System.out.println("Get Supported Media Protocols");
		return new String[]{};
	}
	
	public static void playTone(int note, int duration, int volume)
	{
		System.out.println("Play Tone");
	}

	public static void updatePlayerNum(byte num) 
	{
		mediaPlayers = new PlatformPlayer[num];
	}

	private static String generateMD5Hash(InputStream stream, int byteCount) 
	{
        try
		{
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] data = new byte[byteCount];
            int bytesRead = stream.read(data, 0, byteCount);

            if (bytesRead != -1) { md.update(data, 0, bytesRead); }

            // Convert MD5 hash to hex string
            StringBuilder md5Sum = new StringBuilder();
            for (byte b : md.digest()) { md5Sum.append(String.format("%02x", b)); }

            return md5Sum.toString();
        } catch (Exception e) { System.out.println("Failed to generate stream MD5:" + e.getMessage()); }

		return null;
    }
}
