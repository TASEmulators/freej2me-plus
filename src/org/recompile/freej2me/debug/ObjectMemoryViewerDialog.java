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
package org.recompile.freej2me.debug;

import java.awt.Frame;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformImage;
import org.recompile.mobile.PlatformPlayer;

public class ObjectMemoryViewerDialog extends JDialog
{
	private static final String[] SCALE_OPTIONS = {"25%", "50%", "75%", "100%", "200%", "300%", "400%"};
	private static final float[] SCALE_FACTORS = {0.25f, 0.50f, 0.75f, 1.00f, 2.00f, 3.00f, 4.00f};

	private final ImagePanel imagePanel;
	private final JLabel imageInfoLabel, playerInfoLabel, classInfoLabel, totalHeapLabel;
	private final JComboBox imgScaleBox;
	private final DefaultListModel playerListModel, classListModel;
	private final JList playerList, classList;
	private final JTabbedPane tabbedPane;
	private final TitledBorder imageBorder, playerBorder, classBorder;

	@SuppressWarnings("unchecked")
	public ObjectMemoryViewerDialog(Frame main)
	{
		super(main, "Object Memory Viewer", false);
		setSize(640, 480);
		setUndecorated(true);
		setLayout(new BorderLayout());

		// Created images (aside from FreeJ2ME-Plus' framebuffers and the like)
		imagePanel = new ImagePanel(new ImageSelectionListener()
		{
			public void onImageSelected(WeakReference<PlatformImage> img)
			{
				updateSelectedImageInfo(img);
			}
		});
		imageBorder = BorderFactory.createTitledBorder("Images (0)");
		JScrollPane imageScroll = new JScrollPane(imagePanel);
		imageScroll.getVerticalScrollBar().setUnitIncrement(24);
		imageScroll.getHorizontalScrollBar().setUnitIncrement(24);
		imageScroll.setBorder(imageBorder);

		imageInfoLabel = createInfoLabel();
		imgScaleBox = new JComboBox(SCALE_OPTIONS);
		imgScaleBox.setSelectedItem("100%");
		imgScaleBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				int idx = imgScaleBox.getSelectedIndex();
				if (idx >= 0 && idx < SCALE_FACTORS.length) { imagePanel.setScale(SCALE_FACTORS[idx]); }
			}
		});

		JPanel imageControlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
		imageControlsPanel.add(new JLabel("Scale:"));
		imageControlsPanel.add(imgScaleBox);

		JPanel imageBottomPanel = new JPanel(new BorderLayout());
		imageBottomPanel.add(imageInfoLabel, BorderLayout.WEST);
		imageBottomPanel.add(imageControlsPanel, BorderLayout.EAST);

		JPanel imagesTabPanel = createTabPanel(imageScroll, imageBottomPanel);

		// FreeJ2ME-Plus Players
		playerListModel = new DefaultListModel();
		playerList = new JList(playerListModel);
		playerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		playerBorder = BorderFactory.createTitledBorder("Players (0)");
		JScrollPane playerScroll = new JScrollPane(playerList);
		playerScroll.setBorder(playerBorder);

		playerInfoLabel = createInfoLabel();
		playerList.addListSelectionListener(new javax.swing.event.ListSelectionListener()
		{
			public void valueChanged(javax.swing.event.ListSelectionEvent e)
			{
				if (!e.getValueIsAdjusting())
				{
					updateSelectedPlayerInfo(Mobile.memPlayers);
				}
			}
		});

		JPanel playerTabPanel = createTabPanel(playerScroll, playerInfoLabel);

		// Loaded MIDlet classes
		classListModel = new DefaultListModel();
		classList = new JList(classListModel);
		classList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		classBorder = BorderFactory.createTitledBorder("Loaded MIDlet Classes (0)");
		JScrollPane classScroll = new JScrollPane(classList);
		classScroll.setBorder(classBorder);

		classInfoLabel = createInfoLabel();
		classList.addListSelectionListener(new javax.swing.event.ListSelectionListener()
		{
			public void valueChanged(javax.swing.event.ListSelectionEvent e)
			{
				if (!e.getValueIsAdjusting())
				{
					updateSelectedClassInfo(Mobile.memClasses);
				}
			}
		});

		JPanel classesTabPanel = createTabPanel(classScroll, classInfoLabel);

		tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Images (0)", imagesTabPanel);
		tabbedPane.addTab("Players (0)", playerTabPanel);
		tabbedPane.addTab("Classes (0)", classesTabPanel);
		add(tabbedPane, BorderLayout.CENTER);

		// --- Bottom Bar ---
		JPanel bottomBar = new JPanel(new BorderLayout());
		bottomBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

		totalHeapLabel = new JLabel("WAITING DATA...");
		JButton refreshButton = new JButton("Refresh");
		refreshButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) { refreshData(); }
		});

		bottomBar.add(totalHeapLabel, BorderLayout.WEST);
		bottomBar.add(refreshButton, BorderLayout.EAST);
		add(bottomBar, BorderLayout.SOUTH);
	}

	private static JLabel createInfoLabel()
	{
		JLabel label = new JLabel("Selected: None");
		label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
		return label;
	}

	private static JPanel createTabPanel(JComponent center, JComponent south)
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(center, BorderLayout.CENTER);
		panel.add(south, BorderLayout.SOUTH);
		return panel;
	}

	public void refreshData()
	{
		System.gc();
		ArrayList<WeakReference<PlatformImage>> images = Mobile.memImages;
		ArrayList<WeakReference<PlatformPlayer>> players = Mobile.memPlayers;
		ArrayList<WeakReference<Class<?>>> classes = Mobile.memClasses;

		int imageNum = 0;
		int playerNum = 0;
		int classNum = 0;

		synchronized (images)
		{
			imageNum = countRefs(images);
			imagePanel.setImages(images);
			updateSelectedImageInfo(imagePanel.getSelectedImage());

			synchronized (players)
			{
				playerNum = countRefs(players);
				updatePlayers(players);

				synchronized (classes)
				{
					classNum = countRefs(classes);
					updateClasses(classes);
					updateTotalHeapInfo(images, players, classes, classNum);
				}
			}
		}

		updateTab(0, imageBorder, "Images", imageNum);
		updateTab(1, playerBorder, "Players", playerNum);
		updateTab(2, classBorder, "Classes", classNum);
		classBorder.setTitle("Loaded MIDlet Classes (" + classNum + ")");
	}

	private void updateTab(int tabIndex, TitledBorder border, String label, int count)
	{
		String title = label + " (" + count + ")";
		border.setTitle(title);
		tabbedPane.setTitleAt(tabIndex, title);
	}

	static <T> T getReference(WeakReference<T> ref) { return (ref != null) ? ref.get() : null; }

	private static <T> int countRefs(ArrayList<WeakReference<T>> refs)
	{
		if (refs == null) { return 0; }
		
		int count = 0;
		for (WeakReference<T> ref : refs)
		{
			if (getReference(ref) != null) { count++; }
		}
		return count;
	}

	private static <T> WeakReference<T> getLiveRef(ArrayList<WeakReference<T>> refs, int targetIndex)
	{
		if (refs == null || targetIndex < 0) { return null; }
		
		int liveIdx = 0;
		for (WeakReference<T> ref : refs)
		{
			if (getReference(ref) != null)
			{
				if (liveIdx == targetIndex) { return ref; }
				liveIdx++;
			}
		}
		return null;
	}

	private void updateSelectedImageInfo(WeakReference<PlatformImage> imgRef)
	{
		PlatformImage img = getReference(imgRef);
		if (img == null)
		{
			imageInfoLabel.setText("Selected: None");
			return;
		}
		BufferedImage canvas = img.getCanvas();

		double heapKb = estimateImageHeapBytes(imgRef) / 1024.0;
		imageInfoLabel.setText("Selected: " + canvas.getWidth() + "x" + canvas.getHeight() +
			" | Format: " + getImageTypeName(canvas.getType()) +
			" | Heap: " + String.format("%.2f", heapKb) + " KB");
	}

	private void updateTotalHeapInfo(
		ArrayList<WeakReference<PlatformImage>> images,
		ArrayList<WeakReference<PlatformPlayer>> players,
		ArrayList<WeakReference<Class<?>>> classes,
		int loadedClassNum)
	{
		double imageKb = 0;
		double playerKb = 0;
		double classKb = 0;

		for (WeakReference<PlatformImage> imgRef : images)
		{
			imageKb += estimateImageHeapBytes(imgRef) / 1024.0;
		}
		for (WeakReference<PlatformPlayer> playerRef : players)
		{
			playerKb += estimatePlayerHeapBytes(playerRef) / 1024.0;
		}
		for (WeakReference<Class<?>> classRef : classes)
		{
			classKb += estimateClassHeapBytes(classRef) / 1024.0;
		}
		
		double totalKb = imageKb + playerKb + classKb;
		totalHeapLabel.setText("Total Usage: " + String.format("%.2f", totalKb) + "KB [" +
			String.format("%.2f", imageKb) + "KB Image | " +
			String.format("%.2f", playerKb) + "KB Sound | " +
			String.format("%.2f", classKb) + "KB Class]");
	}

	private long estimateImageHeapBytes(WeakReference<PlatformImage> imgRef)
	{
		PlatformImage img = getReference(imgRef);
		return (img != null) ? (long) img.getCanvas().getWidth() * img.getCanvas().getHeight() * 4L : 0L;
	}

	private long estimatePlayerHeapBytes(WeakReference<PlatformPlayer> playerRef)
	{
		PlatformPlayer player = getReference(playerRef);
		return (player != null) ? player.getDataSize() : 0L;
	}

	private long estimateClassHeapBytes(WeakReference<Class<?>> classRef)
	{
		Class<?> cls = getReference(classRef);
		return (cls != null) ? Mobile.getRecordedClassSize(cls) : 0L;
	}

	private static String getImageTypeName(int type)
	{
		// BufferedImage supports more than these, but show only those that
		// FreeJ2ME-Plus supports, or intends to support at some point.
		switch (type)
		{
			case BufferedImage.TYPE_INT_RGB: return "INT_RGB";
			case BufferedImage.TYPE_INT_ARGB: return "INT_ARGB";
			case BufferedImage.TYPE_USHORT_565_RGB: return "USHORT_565_RGB";
			case BufferedImage.TYPE_USHORT_555_RGB: return "USHORT_555_RGB";
			default: return "INVALID (" + type + ")";
		}
	}

	@SuppressWarnings("unchecked")
	private void updatePlayers(ArrayList<WeakReference<PlatformPlayer>> players)
	{
		int selectedIndex = playerList.getSelectedIndex();
		playerListModel.clear();
		if (players == null)
		{
			updateSelectedPlayerInfo(null);
			return;
		}

		for (WeakReference<PlatformPlayer> playerRef : players)
		{
			PlatformPlayer player = getReference(playerRef);
			if (player != null)
			{
				String contentType = "unknown";
				long curTime = 0;
				long duration = 0;
				try
				{
					contentType = player.getContentType();
					curTime = player.getMediaTime();
					duration = player.getDuration();
				}
				catch (IllegalStateException e) { }

				double heapKb = estimatePlayerHeapBytes(playerRef) / 1024.0;
				playerListModel.addElement(player.toString() + ": " + contentType + " -> " + player.getStateString() +
					" [" + formatTime(curTime) + " / " + formatTime(duration) + "]" +
					" (" + String.format("%.2f", heapKb) + " KB)");
			}
		}

		if (selectedIndex >= 0 && selectedIndex < playerListModel.getSize())
		{
			playerList.setSelectedIndex(selectedIndex);
		}
		updateSelectedPlayerInfo(players);
	}

	private void updateSelectedPlayerInfo(ArrayList<WeakReference<PlatformPlayer>> players)
	{
		WeakReference<PlatformPlayer> playerRef = getLiveRef(players, playerList.getSelectedIndex());
		PlatformPlayer player = getReference(playerRef);
		if (player == null)
		{
			playerInfoLabel.setText("Selected: None");
			return;
		}

		double heapKb = estimatePlayerHeapBytes(playerRef) / 1024.0;
		try
		{
			String contentType = player.getContentType();
			String state = player.getStateString();
			PlatformPlayer.volumeControl vc = (PlatformPlayer.volumeControl) player.getControl("VolumeControl");
			PlatformPlayer.tempoControl tc = (PlatformPlayer.tempoControl) player.getControl("TempoControl");

			String volumeStr = (vc != null) ? (vc.getLevel() + "%") : "N/A";
			String speedStr = (tc != null) ? ((tc.getRate() / 1000) + "%") : "N/A";
			String tempoStr = (tc != null) ? ((tc.getTempo() / 1000) + " BPM") : "N/A";

			playerInfoLabel.setText("Type: " + contentType + " | State: " + state +
				" | Volume: " + volumeStr + " | Speed: " + speedStr +
				" | Tempo: " + tempoStr + " | Heap: " + String.format("%.2f", heapKb) + " KB");
		}
		catch (IllegalStateException e)
		{
			playerInfoLabel.setText("Type: unknown | State: " + player.getStateString() + " | Heap: " + String.format("%.2f", heapKb) + " KB");
		}
	}

	@SuppressWarnings("unchecked")
	private void updateClasses(ArrayList<WeakReference<Class<?>>> classes)
	{
		int selectedIndex = classList.getSelectedIndex();
		classListModel.clear();
		if (classes == null)
		{
			updateSelectedClassInfo(null);
			return;
		}

		for (WeakReference<Class<?>> classRef : classes)
		{
			Class<?> cls = getReference(classRef);
			if (cls != null)
			{
				String pkgName = (cls.getPackage() != null && cls.getPackage().getName().length() > 0) ? cls.getPackage().getName() : "<root>";
				Class<?> sup = cls.getSuperclass();
				String superName = (sup != null) ? sup.getSimpleName() : "Object";
				double heapKb = estimateClassHeapBytes(classRef) / 1024.0;

				classListModel.addElement(cls.getName() + " [" + pkgName + "] -> " + superName +
					" (" + String.format("%.2f", heapKb) + " KB)");
			}
		}

		if (selectedIndex >= 0 && selectedIndex < classListModel.getSize())
		{
			classList.setSelectedIndex(selectedIndex);
		}
		updateSelectedClassInfo(classes);
	}

	private void updateSelectedClassInfo(ArrayList<WeakReference<Class<?>>> classes)
	{
		WeakReference<Class<?>> classRef = getLiveRef(classes, classList.getSelectedIndex());
		Class<?> cls = getReference(classRef);
		if (cls == null)
		{
			classInfoLabel.setText("Selected: None");
			return;
		}

		String pkgName = (cls.getPackage() != null && cls.getPackage().getName().length() > 0) ? cls.getPackage().getName() : "<root>";
		Class<?> sup = cls.getSuperclass();
		String superName = (sup != null) ? sup.getName() : "none";
		double heapKb = estimateClassHeapBytes(classRef) / 1024.0;

		classInfoLabel.setText("Class: " + cls.getName() + " | Package: " + pkgName +
			" | Super: " + superName + " | Heap: " + String.format("%.2f", heapKb) + " KB");
	}

	private static String formatTime(long microseconds)
	{
		long totalSeconds = microseconds / 1000000L;
		long minutes = totalSeconds / 60L;
		return String.format("%02d:%02d", minutes, totalSeconds % 60L);
	}
}
