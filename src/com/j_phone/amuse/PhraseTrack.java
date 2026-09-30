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
package com.j_phone.amuse;

public class PhraseTrack extends com.jblend.media.smaf.phrase.PhraseTrackBase
{

	private Phrase phrase;
	private PhraseTrackListener listener;

    public PhraseTrack(int id) { super(id); }

    public int getID() { return super.getID(); }

	public Phrase getPhrase() { return phrase; }

    public int getState() { return super.getState(); }

	public void setPhrase(Phrase p) 
    {
		this.phrase = p;
		super.setPhrase(phrase.getPhraseImpl());
	}

	public boolean isPlaying() { return super.getState() == PLAYING; }

    public boolean isMute() { return super.isMute(); }

	public void setVolume(int value) { super.setVolume(value); }

    public void mute(boolean mute) { super.mute(mute); }

    public void setPanpot(int value) { super.setPanpot(value); }

	public int getPanpot() { return super.getPanpot(); }

	public void stop() { super.stop(); }

    public void play() { play(1); }

	public void play(int loop) 
    { 
        super.setEventListener((com.jblend.media.smaf.phrase.PhraseTrackListener) listener);
        super.play(loop); 
    }

    public void pause() { super.pause(); }

    public void resume() { super.resume(); }

	public void removePhrase() 
    { 
        this.phrase = null; 
        super.removePhrase();
    }

    public PhraseTrack getSyncMaster() { return getJPhoneSyncMaster(); }

    public void setSubjectTo(PhraseTrack master) 
	{ 
		if(master != null) // Add sync relation
		{
			setJPhoneSyncMaster(master);
			master.slaveJPhonePhrases.add(this);
		}
		else if(getJPhoneSyncMaster() != null) // Clear sync relation
		{
			getJPhoneSyncMaster().slaveJPhonePhrases.remove(this);
			setJPhoneSyncMaster(master);
		}
	}

	public void setEventListener(PhraseTrackListener l) { this.listener = l; }
}
