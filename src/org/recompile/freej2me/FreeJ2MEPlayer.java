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
package org.recompile.freej2me;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.decoders.NokiaOTTDecoder;


import org.recompile.mobile.PlatformPlayer;

public final class FreeJ2MEPlayer extends JDialog
{
	private JLabel dropMessageLabel = new JLabel(">> DROP HERE <<", SwingConstants.CENTER);
	private Timer playbackTimer;
	private JLabel descLabel = new JLabel("<html><center>Click below, or drag a file onto this window to load J2ME media.</center></html>", SwingConstants.CENTER);
	private JLabel fileNameLabel = new JLabel("Loaded Media File:");
	private JLabel fileTypeLabel = new JLabel("File Type: None");
	private JLabel playbackTicker = new JLabel("00:00 / 00:00", SwingConstants.CENTER);

	private JProgressBar progressBar = new JProgressBar(0, 100);
	private JButton[] UIButtons = new JButton[6];
	private JTextField fileNameField = new JTextField();

	private Player mediaPlayer;
	private boolean isPlaying = false;

	public FreeJ2MEPlayer(JFrame parent)
	{
		super(parent, "FreeJ2ME Media Player", true);
		if(Manager.toneSynth == null) { Manager.prepareMediaEngine(); }
		setupPlayerDialog();
	}

	private void setupPlayerDialog()
	{
		Dimension fillSize = new Dimension(240, 250);
		dropMessageLabel.setPreferredSize(fillSize);
		dropMessageLabel.setMaximumSize(fillSize);
		dropMessageLabel.setFont(new Font("Dialog", Font.BOLD, 24));
		dropMessageLabel.setForeground(Color.BLACK);
		dropMessageLabel.setVisible(false);

		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		setSize(240, 250);
		setResizable(false);
		setLocationRelativeTo(getOwner());

		// Configure text field
		fileNameField.setEditable(false);
		fileNameField.setFocusable(false);
		fileNameField.setForeground(Color.BLACK);
		fileNameField.setMaximumSize(new Dimension(280, 24));

		// Configure buttons
		UIButtons[0] = new JButton("Play");
		UIButtons[1] = new JButton("Pause");
		UIButtons[2] = new JButton("Stop");
		UIButtons[3] = new JButton("-5s");
		UIButtons[4] = new JButton("+5s");
		UIButtons[5] = new JButton("Load File...");

		UIButtons[0].setForeground(Color.BLUE);
		UIButtons[1].setForeground(Color.MAGENTA);
		UIButtons[2].setForeground(Color.RED);

		for(int i = 0; i < UIButtons.length; i++) { FJGUI.flattenButton(UIButtons[i]);}

		Insets buttonMargin = new Insets(2, 4, 2, 4);
		for (int i = 0; i < UIButtons.length; i++)
		{
			UIButtons[i].setMargin(buttonMargin);
			UIButtons[i].setFocusPainted(false);
		}

		// Configure Progress Bar
		progressBar.setMaximumSize(new Dimension(280, 16));
		progressBar.setStringPainted(false);

		// Control Panel (Bottom row buttons)
		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
		controlPanel.setOpaque(false);
		controlPanel.add(UIButtons[3]); // -5s
		controlPanel.add(UIButtons[1]); // Pause
		controlPanel.add(UIButtons[0]); // Play
		controlPanel.add(UIButtons[2]); // Stop
		controlPanel.add(UIButtons[4]); // +5s

		// Set Component Alignments for BoxLayout
		descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		UIButtons[5].setAlignmentX(Component.CENTER_ALIGNMENT);
		fileNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		fileNameField.setAlignmentX(Component.CENTER_ALIGNMENT);
		fileTypeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
		playbackTicker.setAlignmentX(Component.CENTER_ALIGNMENT);
		controlPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		dropMessageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		add(Box.createVerticalStrut(8));
		add(dropMessageLabel);
		add(descLabel);
		add(Box.createVerticalStrut(6));
		add(UIButtons[5]);
		add(Box.createVerticalStrut(6));
		add(fileNameLabel);
		add(fileNameField);
		add(fileTypeLabel);
		add(Box.createVerticalStrut(8));
		add(progressBar);
		add(playbackTicker);
		add(Box.createVerticalStrut(8));
		add(controlPanel);

		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent we) { stopMedia(); }
		});

		UIButtons[5].addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { openFile(""); } });
		UIButtons[3].addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { seekMediaBack(); } });
		UIButtons[1].addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { pauseMedia(); } });
		UIButtons[0].addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { playMedia(); } });
		UIButtons[2].addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { stopMedia(); } });
		UIButtons[4].addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { seekMediaForward(); } });

		setDropTarget(new DropTarget(this, new DropTargetListener()
		{
			@Override
			public void dragEnter(DropTargetDragEvent dtde)
			{
				dtde.acceptDrag(DnDConstants.ACTION_COPY);
				toggleComponentsVisibility(false);
				dropMessageLabel.setVisible(true);
			}

			@Override public void dragOver(DropTargetDragEvent dtde) { }
			@Override public void dropActionChanged(DropTargetDragEvent dtde) { }

			@Override
			public void dragExit(DropTargetEvent dte)
			{
				toggleComponentsVisibility(true);
				dropMessageLabel.setVisible(false);
			}

			@Override
			@SuppressWarnings("unchecked")
			public void drop(DropTargetDropEvent dtde)
			{
				try
				{
					dtde.acceptDrop(DnDConstants.ACTION_COPY);
					Transferable transferable = dtde.getTransferable();
					if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
					{
						java.util.List<File> files = (java.util.List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
						if (!files.isEmpty()) { openFile(files.get(0).getAbsolutePath()); }
					}
				}
				catch (Exception e) { System.out.println("Exception caught in Drag and Drop: " + e.getMessage()); }
				finally
				{
					dtde.dropComplete(true);
					toggleComponentsVisibility(true);
					dropMessageLabel.setVisible(false);
				}
			}
		}));
	}

	private void toggleComponentsVisibility(boolean visible)
	{
		fileNameLabel.setVisible(visible);
		fileTypeLabel.setVisible(visible);
		playbackTicker.setVisible(visible);
		progressBar.setVisible(visible);
		fileNameField.setVisible(visible);
		descLabel.setVisible(visible);

		for (int i = 0; i < UIButtons.length; i++) { UIButtons[i].setVisible(visible); }
	}

	private void startPlaybackTimer()
	{
		if (playbackTimer != null) { playbackTimer.cancel(); }
		playbackTimer = new Timer();
		playbackTimer.scheduleAtFixedRate(new TimerTask()
		{
			@Override
			public void run()
			{
				if (isPlaying && mediaPlayer != null)
				{
					final long currentTime = mediaPlayer.getMediaTime();
					final long duration = mediaPlayer.getDuration();

					EventQueue.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							if (currentTime >= duration && duration > 0)
							{
								updatePlaybackTicker(0, duration);
								pauseMedia();
							}
							else { updatePlaybackTicker(currentTime, duration); }
						}
					});
				}
			}
		}, 0, 250);
	}

	private void updatePlaybackTicker(long currentTime, long duration)
	{
		playbackTicker.setText(formatTime(currentTime) + " / " + formatTime(duration));
		int progress = (duration > 0) ? (int) ((currentTime * 100) / duration) : 0;
		progressBar.setValue(progress);
	}

	private String formatTime(long microseconds)
	{
		long seconds = microseconds / 1000000;
		long minutes = seconds / 60;
		seconds = seconds % 60;
		return String.format("%02d:%02d", minutes, seconds);
	}

	private void openFile(String filePath)
	{
		if (mediaPlayer != null) { stopMedia(); }

		if (filePath.length() == 0)
		{
			FileDialog fileDialog = new FileDialog(this, "Select a Media File", FileDialog.LOAD);
			fileDialog.setVisible(true);
			if (fileDialog.getFile() == null) { return; } // User canceled file picker
			filePath = fileDialog.getDirectory() + fileDialog.getFile();
		}

		fileNameField.setText(new File(filePath).getName());

		try
		{
			if (filePath.endsWith(".ota") || filePath.endsWith(".ott")) {
				FileInputStream fileData = new FileInputStream(filePath);
				byte[] toneData = new byte[fileData.available()];
				fileData.read(toneData);
				fileData.close();

				mediaPlayer = Manager.createPlayer(new ByteArrayInputStream(NokiaOTTDecoder.convertToMidi(toneData)), "");
				fileTypeLabel.setText("File Type: audio/ott");
			}
			else
			{
				mediaPlayer = Manager.createPlayer(new FileInputStream(filePath), "");
			}

			mediaPlayer.realize();
			mediaPlayer.prefetch();

			if (mediaPlayer instanceof PlatformPlayer)
			{
				fileTypeLabel.setText("File Type: " + ((PlatformPlayer) mediaPlayer).contentType);
			}

			updatePlaybackTicker(0, mediaPlayer.getDuration());
			playMedia();
		}
		catch (Exception e) { e.printStackTrace(); }
	}

	private void playMedia()
	{
		if (mediaPlayer != null && !isPlaying)
		{
			mediaPlayer.start();
			isPlaying = true;
			startPlaybackTimer();
		}
	}

	private void pauseMedia()
	{
		if (mediaPlayer != null && isPlaying)
		{
			mediaPlayer.stop();
			isPlaying = false;
		}
	}

	private void stopMedia()
	{
		pauseMedia();
		if (playbackTimer != null)
		{
			playbackTimer.cancel();
			playbackTimer = null;
		}
		if (mediaPlayer != null)
		{
			mediaPlayer.close();
			mediaPlayer = null;
		}
		fileTypeLabel.setText("File Type: None");
		fileNameField.setText("");
		updatePlaybackTicker(0, 0);
	}

	private void seekMediaBack()
	{
		if (mediaPlayer != null)
		{
			long newTime = Math.max(0, mediaPlayer.getMediaTime() - 5000000);
			mediaPlayer.setMediaTime(newTime);
		}
	}

	private void seekMediaForward()
	{
		if (mediaPlayer != null)
		{
			long newTime = Math.min(mediaPlayer.getDuration(), mediaPlayer.getMediaTime() + 5000000);
			mediaPlayer.setMediaTime(newTime);
		}
	}
}
