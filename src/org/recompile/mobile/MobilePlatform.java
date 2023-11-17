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

import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.InputStream;

import java.awt.event.KeyEvent;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.lcdui.Image;
import javax.microedition.m3g.Graphics3D;

import java.awt.image.BufferedImage;


/*

	Mobile Platform

*/

public class MobilePlatform
{

	private PlatformImage lcd;
	private PlatformGraphics gc;
	public int lcdWidth;
	public int lcdHeight;

	public MIDletLoader loader;
	EventQueue eventQueue;

	public Runnable painter;

	public String dataPath = "";

	public int keyState = 0;

	public MobilePlatform(int width, int height)
	{
		lcdWidth = width;
		lcdHeight = height;

		lcd = new PlatformImage(width, height);
		gc = lcd.getGraphics();

		Mobile.setGraphics3D(new Graphics3D());
		
		eventQueue = new EventQueue(this);

		painter = new Runnable()
		{
			public void run()
			{
				// Placeholder //
			}
		};
	}

	public void startEventQueue() {
		eventQueue.start();
	}

	public void dropQueuedEvents() {
		eventQueue.dropEvents();
	}

	public void resizeLCD(int width, int height)
	{
		lcdWidth = width;
		lcdHeight = height;

		lcd = new PlatformImage(width, height);
		gc = lcd.getGraphics();
	}

	public BufferedImage getLCD()
	{
		return lcd.getCanvas();
	}

	public void setPainter(Runnable r)
	{
		painter = r;
	}

	public void keyPressed(int keycode)
	{
		eventQueue.submit(new PlatformEvent(PlatformEvent.KEY_PRESSED, keycode));
	}

	public void keyReleased(int keycode)
	{
		eventQueue.submit(new PlatformEvent(PlatformEvent.KEY_RELEASED, keycode));
	}

	public void keyRepeated(int keycode)
	{
		eventQueue.submit(new PlatformEvent(PlatformEvent.KEY_REPEATED, keycode));
	}

	public void pointerDragged(int x, int y)
	{
		eventQueue.submit(new PlatformEvent(PlatformEvent.POINTER_DRAGGED, x, y));
	}

	public void pointerPressed(int x, int y)
	{
		eventQueue.submit(new PlatformEvent(PlatformEvent.POINTER_PRESSED, x, y));
	}

	public void pointerReleased(int x, int y)
	{
		eventQueue.submit(new PlatformEvent(PlatformEvent.POINTER_RELEASED, x, y));
	}


	public void doKeyPressed(int keycode)
	{
		updateKeyState(keycode, 1);
		Displayable d;
		if ((d = Mobile.getDisplay().getCurrent()) != null) {
			d.keyPressed(keycode);		
		}
	}

	public void doKeyReleased(int keycode)
	{
		updateKeyState(keycode, 0);
		Displayable d;
		if ((d = Mobile.getDisplay().getCurrent()) != null) {
			d.keyReleased(keycode);
		}
	}

	public void doKeyRepeated(int keycode)
	{
		Displayable d;
		if ((d = Mobile.getDisplay().getCurrent()) != null) {
			d.keyRepeated(keycode);	
		}
	}

	public void doPointerDragged(int x, int y)
	{
		Displayable d;
		if ((d = Mobile.getDisplay().getCurrent()) != null) {
			d.pointerDragged(x, y);	
		}
	}

	public void doPointerPressed(int x, int y)
	{
		Displayable d;
		if ((d = Mobile.getDisplay().getCurrent()) != null) {
			d.pointerPressed(x, y);	
		}
	}

	public void doPointerReleased(int x, int y)
	{
		Displayable d;
		if ((d = Mobile.getDisplay().getCurrent()) != null) {
			d.pointerReleased(x, y);
		}
	}

	private void updateKeyState(int key, int val)
	{
		int mask=0;
		switch (key)
		{
			case Mobile.KEY_NUM2: mask = GameCanvas.UP_PRESSED; break;
			case Mobile.KEY_NUM4: mask = GameCanvas.LEFT_PRESSED; break;
			case Mobile.KEY_NUM6: mask = GameCanvas.RIGHT_PRESSED; break;
			case Mobile.KEY_NUM8: mask = GameCanvas.DOWN_PRESSED; break;
			case Mobile.KEY_NUM5: mask = GameCanvas.FIRE_PRESSED; break;
			case Mobile.KEY_NUM1: mask = GameCanvas.GAME_A_PRESSED; break;
			case Mobile.KEY_NUM3: mask = GameCanvas.GAME_B_PRESSED; break;
			case Mobile.KEY_NUM7: mask = GameCanvas.GAME_C_PRESSED; break;
			case Mobile.KEY_NUM9: mask = GameCanvas.GAME_D_PRESSED; break;
			case Mobile.NOKIA_UP: mask = GameCanvas.UP_PRESSED; break;
			case Mobile.NOKIA_LEFT: mask = GameCanvas.LEFT_PRESSED; break;
			case Mobile.NOKIA_RIGHT: mask = GameCanvas.RIGHT_PRESSED; break;
			case Mobile.NOKIA_DOWN: mask = GameCanvas.DOWN_PRESSED; break;
			case Mobile.NOKIA_SOFT3: mask = GameCanvas.FIRE_PRESSED; break;

		}
		keyState |= mask;
		keyState ^= mask;
		if(val==1) { keyState |= mask; }
	}

/*
	******** Jar Loading ********
*/

	public boolean loadJar(String jarurl)
	{
		try
		{
			URL jar = new URL(jarurl);
			loader = new MIDletLoader(new URL[]{jar});
			return true;
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
			return false;
		}

	}

	public void runJar()
	{
		try
		{
			loader.start();
		}
		catch (Exception e)
		{
			System.out.println("Error Running Jar");
			e.printStackTrace();
		}
	}

/*
	********* Graphics ********
*/

	public void flushGraphics(Image img, int x, int y, int width, int height)
	{
		gc.flushGraphics(img, x, y, width, height);

		painter.run();

		//System.gc();
	}

	public void repaint(Image img, int x, int y, int width, int height)
	{
		gc.flushGraphics(img, x, y, width, height);

		painter.run();

		//System.gc();
	}

	public void setPlatformProperty(String value)
	{
		loader.setProperty("microedition.platform", value);
		System.setProperty("microedition.platform", value);
	}


	static class PlatformEvent
	{
		static final int KEY_PRESSED = 1;
		static final int KEY_REPEATED = 2; 
		static final int KEY_RELEASED = 3;
		static final int POINTER_PRESSED = 4;
		static final int POINTER_DRAGGED = 5;
		static final int POINTER_RELEASED = 6;

		int type;
		int code;
		int code2;

		PlatformEvent(int type, int code)
		{
			this.type = type;
			this.code = code;
		}

		PlatformEvent(int type, int x, int y)
		{
			this.type = type;
			this.code = x;
			this.code2 = y;
		}
	}

	/**
	 * This class exists so we don't block main AWT EventQueue.
	 */
	private static class EventQueue implements Runnable	{
		BlockingQueue<PlatformEvent> queue = new LinkedBlockingQueue<>();
		MobilePlatform platform;
		private volatile Thread thread;

		public EventQueue(MobilePlatform platform) {
			this.platform = platform;
		}

		public void start()	{
			if (thread == null) {
				thread = new Thread(this, "MobilePlatformEventQueue");
				thread.start();
			}
		}

		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					PlatformEvent event = queue.take();
					handleEvent(event);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				} catch (Exception e) {
					System.out.println("exception in event handler: "+e.getMessage());
				}
			}

			thread = null;
		}

		public void submit(PlatformEvent event) {
			queue.offer(event);
		}

		public void dropEvents() {
			while (true) {
				if (queue.poll() == null) {
					break;
				}
			}
		}

		private void handleEvent(PlatformEvent event) {
			if (event.type == PlatformEvent.KEY_PRESSED) {
				platform.doKeyPressed(event.code);
			} else if (event.type == PlatformEvent.KEY_REPEATED) {
				platform.doKeyRepeated(event.code);
			} else if (event.type == PlatformEvent.KEY_RELEASED) {
				platform.doKeyReleased(event.code);
			} else if (event.type == PlatformEvent.POINTER_PRESSED) {
				platform.doPointerPressed(event.code, event.code2);
			} else if (event.type == PlatformEvent.POINTER_DRAGGED) {
				platform.doPointerDragged(event.code, event.code2);
			} else if (event.type == PlatformEvent.POINTER_RELEASED) {
				platform.doPointerReleased(event.code, event.code2);
			}
		}

	}


}
