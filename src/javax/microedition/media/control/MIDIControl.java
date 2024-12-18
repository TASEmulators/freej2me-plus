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
package javax.microedition.media.control;

public interface MIDIControl extends javax.microedition.media.Control
{

	public static final int CONTROL_CHANGE = 176;
	public static final int NOTE_ON = 144;

	static final int PROGRAM_CHANGE = 0xC0;
	static final int CONTROL_BANK_CHANGE_MSB = 0x00;
	static final int CONTROL_BANK_CHANGE_LSB = 0x20;

	public int[] getBankList(boolean custom);

	public int getChannelVolume(int channel);

	public java.lang.String getKeyName(int bank, int prog, int key);

	public int[] getProgram(int channel);

	public int[] getProgramList(int bank);

	public java.lang.String getProgramName(int bank, int prog);

	public boolean isBankQuerySupported();

	public int longMidiEvent(byte[] data, int offset, int length);

	public void setChannelVolume(int channel, int volume);

	public void setProgram(int channel, int bank, int program);

	public void shortMidiEvent(int type, int data1, int data2);

}
