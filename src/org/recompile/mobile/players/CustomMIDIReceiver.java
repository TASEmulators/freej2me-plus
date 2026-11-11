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
package org.recompile.mobile.players;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

// We need this in order to achieve proper volume control for MIDI and SMAF
public class CustomMIDIReceiver implements Receiver
{
	private final Receiver target;
	private float volumeMultiplier = 1.0f;

	public CustomMIDIReceiver(Receiver receiver) { this.target = receiver; }

	public void setVolMult(int val) { this.volumeMultiplier = val / 100.0f; }

	public void send(MidiMessage message, long time)
	{
		if (message instanceof ShortMessage)
		{
			ShortMessage sm = (ShortMessage) message;
			
			// Intercept NOTE_ON messages, we'll override the volume (velocity)
			// on them.
			if (sm.getCommand() == ShortMessage.NOTE_ON)
			{
				int originalVol = sm.getData2();

				if(originalVol > 0)
				{
					int scaledVol = (int) (originalVol * volumeMultiplier);
					
					try
					{
						ShortMessage msg = new ShortMessage();
						msg.setMessage(sm.getCommand(), sm.getChannel(), sm.getData1(), scaledVol);
						target.send(msg, time);
						// We already sent the updated message, no need to continue.
						return;
					}
					catch (InvalidMidiDataException e) { e.printStackTrace(); }
				}
			}
		}

		// We don't care for sysex or other messages here, send them as is.
		target.send(message, time);
	}

	public void close() { target.close(); }
}
