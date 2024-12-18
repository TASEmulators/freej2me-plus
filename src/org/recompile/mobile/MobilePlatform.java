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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
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

	// Frame Limit Variables
	private long lastRenderTime = System.nanoTime();
	private long requiredFrametime = 0;
	private long elapsedTime = 0;
	private long sleepTime = 0;

	// Whether the user has toggled the ShowFPS option
	private final int OVERLAY_WIDTH = 80;
	private final int OVERLAY_HEIGHT = 20;
	private String showFPS = "Off";
	private int frameCount = 0;
	private long lastFpsTime = System.nanoTime();
    private int fps = 0;

	public static boolean isLibretro = false;
	public static boolean isSDL = false;

	public MIDletLoader loader;
	private EventQueue eventQueue;
	public static Displayable displayable;

	public Runnable painter;

	public String dataPath = "";

	public volatile int keyState = 0;

	public MobilePlatform(int width, int height)
	{
		resizeLCD(width, height);

		Mobile.setGraphics3D(new Graphics3D());
		
		eventQueue = new EventQueue(this);

		painter = new Runnable()
		{
			public void run()
			{
				// Placeholder //
			}
		};

		// SDL is the only one that needs this, since its runnable ties input and render logic for TAS support
		if(isSDL)
		{
			final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

			service.scheduleAtFixedRate(() -> 
			{
				// If 100ms have passed and a new painter run did not happen, force it to happen
				if(lastRenderTime - System.nanoTime() < -100_000_000)
				{
					painter.run();
				}
			}, 100_000_000, 100_000_000, TimeUnit.NANOSECONDS); // run 20 times per second
		}
		
	}

	public void startEventQueue() { eventQueue.start(); }

	public void dropQueuedEvents() { eventQueue.dropEvents(); }

	public void resizeLCD(int width, int height)
	{
		// No need to waste time here if the screen dimensions haven't changed (screen was just rotated for example)
		if(lcdWidth == width && lcdHeight == height) { return; }

		lcdWidth = width;
		lcdHeight = height;
		Font.setScreenSize(width, height);

		lcd = new PlatformImage(width, height);
		gc = lcd.getGraphics();

		/* 
		 * Try to have the jar scale as well. If this doesn't work,
		 * a simple restart is all it takes, just like before.
		 */
		if(Mobile.getDisplay() != null) 
		{ 
			Mobile.getDisplay().getCurrent().doSizeChanged(width, height);
			Mobile.getDisplay().getCurrent().platformImage = lcd; 
			Mobile.getDisplay().getCurrent().graphics = gc; 
		}
	}

	public BufferedImage getLCD() { return lcd.getCanvas(); }

	public void setPainter(Runnable r) { painter = r; }

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
		updateKeyState(Mobile.getGameAction(keycode), 1);
		if ((displayable = Mobile.getDisplay().getCurrent()) != null) { displayable.keyPressed(keycode); }
	}

	public void doKeyReleased(int keycode)
	{
		updateKeyState(Mobile.getGameAction(keycode), 0);
		if ((displayable = Mobile.getDisplay().getCurrent()) != null) { displayable.keyReleased(keycode); }
	}

	public void doKeyRepeated(int keycode)
	{
		if ((displayable = Mobile.getDisplay().getCurrent()) != null) { displayable.keyRepeated(keycode); }
	}

	public void doPointerDragged(int x, int y)
	{
		if ((displayable = Mobile.getDisplay().getCurrent()) != null) { displayable.pointerDragged(x, y); }
	}

	public void doPointerPressed(int x, int y)
	{
		if ((displayable = Mobile.getDisplay().getCurrent()) != null) { displayable.pointerPressed(x, y); }
	}

	public void doPointerReleased(int x, int y)
	{
		if ((displayable = Mobile.getDisplay().getCurrent()) != null) { displayable.pointerReleased(x, y); }
	}

	private void updateKeyState(int key, int val)
	{
		int mask=0;
		switch (key)
		{
			case Canvas.KEY_NUM2: mask = GameCanvas.UP_PRESSED;     break;
			case Canvas.KEY_NUM4: mask = GameCanvas.LEFT_PRESSED;   break;
			case Canvas.KEY_NUM6: mask = GameCanvas.RIGHT_PRESSED;  break;
			case Canvas.KEY_NUM8: mask = GameCanvas.DOWN_PRESSED;   break;
			case Canvas.KEY_NUM5: mask = GameCanvas.FIRE_PRESSED;   break;
			case Canvas.KEY_NUM1: mask = GameCanvas.GAME_A_PRESSED; break;
			case Canvas.KEY_NUM3: mask = GameCanvas.GAME_B_PRESSED; break;
			case Canvas.KEY_NUM7: mask = GameCanvas.GAME_C_PRESSED; break;
			case Canvas.KEY_NUM9: mask = GameCanvas.GAME_D_PRESSED; break;
			case Canvas.UP:       mask = GameCanvas.UP_PRESSED;     break;
			case Canvas.LEFT:     mask = GameCanvas.LEFT_PRESSED;   break;
			case Canvas.RIGHT:    mask = GameCanvas.RIGHT_PRESSED;  break;
			case Canvas.DOWN:     mask = GameCanvas.DOWN_PRESSED;   break;
			case Canvas.FIRE:     mask = GameCanvas.FIRE_PRESSED;   break;
		}
		keyState |= mask;
		keyState ^= mask;
		if(val==1) { keyState |= mask; }
	}

/*
	******** Jar/Jad Loading ********
*/

	public boolean load(String fileName) 
	{
        Map<String, String> descriptorProperties = new HashMap<>();

		/*
		 * If loading a jar directly, check if an accompanying jad with the same name 
		 * is present in the directory, to load any platform properties from there.
		 */
		if(fileName.toLowerCase().contains(".jar")) 
		{
			try 
			{
				File checkJad = new File(new URI(fileName.replace(".jar", ".jad")));
				if(checkJad.exists() && !checkJad.isDirectory()) 
				{
					Mobile.log(Mobile.LOG_INFO, MobilePlatform.class.getPackage().getName() + "." + MobilePlatform.class.getSimpleName() + ": " + "Accompanying JAD found! Parsing additional MIDlet properties.");
					fileName = fileName.replace(".jar", ".jad"); 
				}
			} catch (Exception e) { Mobile.log(Mobile.LOG_INFO, MobilePlatform.class.getPackage().getName() + "." + MobilePlatform.class.getSimpleName() + ": " + "Couldn't check for accompanying JAD:" + e.getMessage()); }
		}
		
        boolean isJad = fileName.toLowerCase().endsWith(".jad");

        if (isJad) 
		{
            String preparedFileName = fileName.substring(fileName.lastIndexOf(":") + 1).trim();
            try { preparedFileName = URLDecoder.decode(preparedFileName, StandardCharsets.UTF_8.name()); } 
			catch (Exception e) 
			{
                System.err.println("Error decoding file name: " + e.getMessage());
                return false;
            }

            try (InputStream targetStream = new FileInputStream(preparedFileName)) { MIDletLoader.parseDescriptorInto(targetStream, descriptorProperties); } 
			catch (IOException e) 
			{
                Mobile.log(Mobile.LOG_ERROR, MobilePlatform.class.getPackage().getName() + "." + MobilePlatform.class.getSimpleName() + ": " + "Failed to load Jad: " + e.getMessage());
                return false;
            }

            String jarUrl = descriptorProperties.getOrDefault("MIDlet-Jar-URL", preparedFileName.replace(".jad", ".jar"));

            // We will not support downloading jars from the internet on the fly, unless there is a very good reason to do so. Also, unless the jad has a URI for loading the jar, ignore the path as well
            if (jarUrl.toLowerCase().contains("http:") || jarUrl.toLowerCase().contains("https:") || !jarUrl.toLowerCase().contains("file:")) 
				{ jarUrl = fileName.replace(".jad", ".jar"); } // Just try getting the jar in the same directory as the jad in those cases.

            fileName = jarUrl;
        }

        try 
		{
            URL jar = new URL(fileName);
            loader = new MIDletLoader(new URL[]{jar}, descriptorProperties);
            return true;
        } 
		catch (Exception e) 
		{
            Mobile.log(Mobile.LOG_ERROR, MobilePlatform.class.getPackage().getName() + "." + MobilePlatform.class.getSimpleName() + ": " + "Failed to load Jar: " + e.getMessage());
            return false;
        }
    }

	public void runJar()
	{
		try { loader.start(); }
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, MobilePlatform.class.getPackage().getName() + "." + MobilePlatform.class.getSimpleName() + ": " + "Error Running Jar");
			e.printStackTrace();
		}
	}

/*
	********* Graphics ********
*/

	public final void flushGraphics(Image img, int x, int y, int width, int height)
	{
		limitFps();
		gc.flushGraphics(img, x, y, width, height);
		
		if(!showFPS.equals("Off")) { showFPS();}
		painter.run();

		//System.gc();
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
	private static class EventQueue implements Runnable	
	{
		BlockingQueue<PlatformEvent> queue = new LinkedBlockingQueue<>();
		MobilePlatform platform;
		private volatile Thread thread;

		public EventQueue(MobilePlatform platform) { this.platform = platform; }

		public void start()	{
			if (thread == null) 
			{
				thread = new Thread(this, "MobilePlatformEventQueue");
				thread.start();
			}
		}

		public void run() {
			while (!Thread.currentThread().isInterrupted()) 
			{
				try 
				{
					PlatformEvent event = queue.take();
					handleEvent(event);
				}
				catch (InterruptedException e) 
				{
					Thread.currentThread().interrupt();
					break;
				} catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, MobilePlatform.class.getPackage().getName() + "." + MobilePlatform.class.getSimpleName() + ": " + "exception in event handler: "+e.getMessage()); }
			}

			thread = null;
		}

		public void submit(PlatformEvent event) { queue.offer(event); }

		public void dropEvents() 
		{
			while (true) 
			{
				if (queue.poll() == null) { break; }
			}
		}

		private void handleEvent(PlatformEvent event) 
		{
			if (event.type == PlatformEvent.KEY_PRESSED) {platform.doKeyPressed(event.code); }
			else if (event.type == PlatformEvent.KEY_REPEATED) { platform.doKeyRepeated(event.code); }
			else if (event.type == PlatformEvent.KEY_RELEASED) { platform.doKeyReleased(event.code); }
			else if (event.type == PlatformEvent.POINTER_PRESSED) { platform.doPointerPressed(event.code, event.code2); }
			else if (event.type == PlatformEvent.POINTER_DRAGGED) { platform.doPointerDragged(event.code, event.code2); }
			else if (event.type == PlatformEvent.POINTER_RELEASED) { platform.doPointerReleased(event.code, event.code2); }
		}

	}

	private void limitFps() 
	{
		if(Mobile.limitFPS == 0) { lastRenderTime = System.nanoTime(); return; }

		requiredFrametime = 1_000_000_000 / Mobile.limitFPS;
		elapsedTime = System.nanoTime() - lastRenderTime;
		sleepTime = (requiredFrametime - elapsedTime); // Sleep time in nanoseconds

		/* 
		 * TODO: Framerate still deviates a little from the intended lock 
		 * 
		 * Possible solution: Some kind of calibration mechanism to nudge the
		 * actual lock closer to the user's display refresh rate.
		 */
		if (sleepTime > 0) { LockSupport.parkNanos(sleepTime); }

		lastRenderTime = System.nanoTime();
	}

	// For now, the logic here works by updating the framerate counter every second
	private final void showFPS() {
		frameCount++;
	
		if (System.nanoTime() - lastFpsTime >= 1_000_000_000) { 
			fps = frameCount; 
			frameCount = 0; 
			lastFpsTime = System.nanoTime(); 
		}

		BufferedImage overlayImage = new BufferedImage(OVERLAY_WIDTH, OVERLAY_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D overlayGraphics = overlayImage.createGraphics();

		// Enable font AA for better text quality (GASP uses font resource information to apply AA when appropriate)
        gc.getGraphics2D().setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		gc.getGraphics2D().setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		
		// Set the overlay background
		overlayGraphics.setColor(new Color(0, 0, 105, 150)); // BG is a semi-transparent dark blue
		overlayGraphics.fillRect(0, 0, OVERLAY_WIDTH, OVERLAY_HEIGHT);
	
		// Adjust the font size
		int fontSize = 21; // Base font size
		overlayGraphics.setFont(overlayGraphics.getFont().deriveFont((float) fontSize));
		overlayGraphics.setColor(new Color(255, 175, 0, 255)); // Text color is orange
	
		// Draw the FPS text
		String fpsText = "FPS: " + fps;
		overlayGraphics.drawString(fpsText, 3, 17);
	
		overlayGraphics.dispose(); // Clean up graphics
	
		// Scale the overlay image to fit the screen
		double scale = Math.min(lcdWidth, lcdHeight);

		int scaledWidth = 0;
		if(scale < 100) { scaledWidth = (int) (lcdWidth / 2.5);}
		if(scale > 100) { scaledWidth = (int) (lcdWidth / 3);}
		if(scale > 200) { scaledWidth = (int) (lcdWidth / 4);}
		if(scale > 300) { scaledWidth = (int) (lcdWidth / 5);}
		if(scale > 400) { scaledWidth = (int) (lcdWidth / 6);}
		int scaledHeight = (int) (scaledWidth / 4);
	
		// Draw the scaled overlay image onto the jar's main screen.
		if(showFPS.equals("TopLeft"))          { gc.getGraphics2D().drawImage(overlayImage, 2, 2, scaledWidth, scaledHeight, null); }
		else if(showFPS.equals("TopRight"))    { gc.getGraphics2D().drawImage(overlayImage, lcdWidth-scaledWidth-2, 2, scaledWidth, scaledHeight, null); }
		else if(showFPS.equals("BottomLeft"))  { gc.getGraphics2D().drawImage(overlayImage, 2, lcdHeight-scaledHeight-2, scaledWidth, scaledHeight, null); }
		else if(showFPS.equals("BottomRight")) { gc.getGraphics2D().drawImage(overlayImage, lcdWidth-scaledWidth-2, lcdHeight-scaledHeight-2, scaledWidth, scaledHeight, null); }
	}

	public void setShowFPS(String show) { showFPS = show; }
}
