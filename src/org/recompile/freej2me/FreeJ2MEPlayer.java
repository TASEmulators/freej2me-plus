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

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.decoders.NokiaOTTDecoder;

import org.recompile.mobile.PlatformPlayer;

public final class FreeJ2MEPlayer extends Dialog 
{
    private Label dropMessageLabel = new Label(">> DROP HERE <<", Label.CENTER);
    private Timer playbackTimer;
    private Label descLabel1 = new Label("Click the button below, or drag a file", Label.CENTER);
    private Label descLabel2 = new Label("onto this window to load J2ME media.", Label.CENTER);
    private Label fileNameLabel = new Label("Loaded Media File:");
    private Label fileTypeLabel = new Label("File Type: None");
    private Label playbackTicker = new Label("00:00 / 00:00", Label.CENTER);
    private ProgressBar progressBar;
    private Button[] UIButtons = new Button[6];
    private TextField fileNameField;
    private Player mediaPlayer;
    private boolean isPlaying = false;

    public FreeJ2MEPlayer(Frame parent) 
    {
        super(parent, "FreeJ2ME Media Player", true);
        if(Manager.toneSynth == null) { Manager.prepareMediaEngine(); }
        setupPlayerDialog();
    }

    private void setupPlayerDialog() 
    {
        dropMessageLabel.setFont(new Font("Dialog", Font.BOLD, 20));
        dropMessageLabel.setForeground(Color.WHITE);
        dropMessageLabel.setVisible(false);

        setBackground(FreeJ2ME.freeJ2MEBGColor);
        setForeground(Color.ORANGE);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        setSize(280, 280);
        setResizable(false);
        
        fileNameField = new TextField();
        fileNameField.setEditable(false);
        fileNameField.setEnabled(false);
        fileNameField.setFocusable(false);
        fileNameField.setBackground(FreeJ2ME.freeJ2MEBGColor);
        fileNameField.setForeground(Color.WHITE);
        progressBar = new ProgressBar();

        UIButtons[0] = new Button("Play");
        UIButtons[1] = new Button("Pause");
        UIButtons[2] = new Button("Stop");
        UIButtons[3] = new Button("- 5s");
        UIButtons[4] = new Button("+ 5s");
        UIButtons[5] = new Button("Load File...");

        for(int i = 0; i < UIButtons.length; i++) { UIButtons[i].setBackground(FreeJ2ME.freeJ2MEDragColor); }

        UIButtons[0].setPreferredSize(new Dimension(100, 30));

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 5; // Span across multiple columns
        add(dropMessageLabel, gbc); // Add the drop message label first
    
        add(descLabel1, gbc);
        gbc.gridy++;
        add(descLabel2, gbc);
        gbc.gridy++;
        add(UIButtons[5], gbc);
        gbc.gridy++;
        add(fileNameLabel, gbc);
        gbc.gridy++;
        add(fileNameField, gbc);
        gbc.gridy++;
        add(fileTypeLabel, gbc);

        gbc.gridy++;
        add(progressBar, gbc);
        gbc.gridy++;
        add(playbackTicker, gbc);
        gbc.gridy++;

        gbc.gridwidth = 1;
        add(UIButtons[3], gbc);
        gbc.gridx++;
        add(UIButtons[1], gbc);
        gbc.gridx++;
        add(UIButtons[0], gbc);
        gbc.gridx++;
        add(UIButtons[2], gbc);
        gbc.gridx++;
        add(UIButtons[4], gbc);

        gbc.gridx = 0;

        // Window close behavior
        addWindowListener(new WindowAdapter() 
        {
            public void windowClosing(WindowEvent we) 
            {
                stopMedia();
                dispose();
            }
        });

        UIButtons[5].addActionListener(new ActionListener() 
        {
            @Override
            public void actionPerformed(ActionEvent e) { openFile(""); }
        });
        UIButtons[3].addActionListener(new ActionListener() 
        {
            @Override
            public void actionPerformed(ActionEvent e) { seekMediaBack(); }
        });
        UIButtons[1].addActionListener(new ActionListener() 
        {
            @Override
            public void actionPerformed(ActionEvent e) { pauseMedia(); }
        });
        UIButtons[0].addActionListener(new ActionListener() 
        {
            @Override
            public void actionPerformed(ActionEvent e) { playMedia(); }
        });
        UIButtons[2].addActionListener(new ActionListener() 
        {
            @Override
            public void actionPerformed(ActionEvent e) { stopMedia(); }
        });
        UIButtons[4].addActionListener(new ActionListener() 
        {
            @Override
            public void actionPerformed(ActionEvent e) { seekMediaForward(); }
        });

        setDropTarget(new DropTarget(this, new DropTargetListener() 
        {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) 
            { 
                dtde.acceptDrag(DnDConstants.ACTION_COPY);
                setBackground(FreeJ2ME.freeJ2MEDragColor);
                toggleComponentsVisibility(false);
                dropMessageLabel.setVisible(true);
                revalidate();
                repaint();
            }

            @Override
            public void dragOver(DropTargetDragEvent dtde) { }

            @Override
            public void dropActionChanged(DropTargetDragEvent dtde) { }

            @Override
            public void dragExit(DropTargetEvent dte) 
            { 
                // Show other components
                toggleComponentsVisibility(true);
                dropMessageLabel.setVisible(false);
                setBackground(FreeJ2ME.freeJ2MEBGColor);
                revalidate();
                repaint();
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
                catch (Exception e) { System.out.println("Exception caught in Drag and Drop:" + e.getMessage()); }
                finally 
                {
                    dtde.dropComplete(true);
                    toggleComponentsVisibility(true);
                    dropMessageLabel.setVisible(false);
                    setBackground(FreeJ2ME.freeJ2MEBGColor);
                    revalidate();
                    repaint();
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
        descLabel1.setVisible(visible);
        descLabel2.setVisible(visible);

        for(int i = 0; i < UIButtons.length; i++) { UIButtons[i].setVisible(visible); }
    }

    private void startPlaybackTimer() 
    {
        playbackTimer = new Timer();
        playbackTimer.scheduleAtFixedRate(new TimerTask() 
        {
            @Override
            public void run()
            {
                if (isPlaying)
                {
                    playbackTicker.setVisible(true);
                    progressBar.setVisible(true);
                    final long currentTime = mediaPlayer.getMediaTime();
                    final long duration = mediaPlayer.getDuration();
                    if(currentTime >= duration) 
                    {
                        EventQueue.invokeLater(new Runnable() 
                        {
                            @Override
                            public void run() { updatePlaybackTicker(0, duration); }
                        });
                        pauseMedia();
                    }
                    else 
                    { 
                        EventQueue.invokeLater(new Runnable() 
                        {
                            @Override
                            public void run() { updatePlaybackTicker(currentTime, duration); }
                        });
                    }
                } 
                else 
                {
                    EventQueue.invokeLater(new Runnable() 
                    {
                        @Override
                        public void run() 
                        {
                            boolean isVisible = playbackTicker.isVisible();
                            playbackTicker.setVisible(!isVisible);
                            progressBar.setVisible(!isVisible);
                        }
                    });
                }
            }
        }, 0, 500);
    }

    // Method to update the playback time
    private void updatePlaybackTicker(long currentTime, long duration) 
    {
        String currentTimeStr = formatTime(currentTime);
        String durationStr = formatTime(duration);
        playbackTicker.setText(currentTimeStr + " / " + durationStr);

        int progress = (int) ((currentTime * 100) / (duration != 0 ? duration : 1));
        progressBar.setProgress(progress);
    }

    // Helper method to format time from microseconds to mm:ss
    private String formatTime(long microseconds) 
    {
        long seconds = microseconds / 1000000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void openFile(String filePath) 
    {
        if(mediaPlayer != null) 
        { 
            mediaPlayer.stop();
            mediaPlayer.close();
            mediaPlayer = null;
        }
        if(filePath == "") 
        {
            FileDialog fileDialog = new FileDialog(this, "Select a Media File", FileDialog.LOAD);
            fileDialog.setVisible(true);
            filePath = fileDialog.getDirectory() + fileDialog.getFile();
            fileNameField.setText(fileDialog.getFile());
            
            try 
            {   
                if(filePath.contains(".ota") || filePath.contains(".ott")) // Nokia's OTT has no real header to parse, we can only work with ott/ota as the file extension
                {
                    FileInputStream fileData = new FileInputStream(filePath);
                    byte[] toneData = new byte[fileData.available()];

                    fileData.read(toneData);
                    
                    mediaPlayer = Manager.createPlayer(new ByteArrayInputStream(NokiaOTTDecoder.convertToMidi(toneData) ), ""); // Let PlatformPlayer find out what type to prepare
                    fileTypeLabel.setText("File Type: audio/ott");
                }
                else {  mediaPlayer = Manager.createPlayer(new FileInputStream(filePath), ""); /* Let PlatformPlayer find out what type to prepare */ }
                
                mediaPlayer.realize();
                mediaPlayer.prefetch();

                fileTypeLabel.setText("File Type: " + ((PlatformPlayer) mediaPlayer).contentType);
                
                updatePlaybackTicker(0, mediaPlayer.getDuration());
                startPlaybackTimer();
            } 
            catch (Exception e) 
            {
                e.printStackTrace();
            }
        }
        else // Drag and Drop
        {
            fileNameField.setText(new File(filePath).getName());
        
            try 
            {
                mediaPlayer = null;
                if(filePath.endsWith(".ota") || filePath.endsWith(".ott")) {
                    FileInputStream fileData = new FileInputStream(filePath);
                    byte[] toneData = new byte[fileData.available()];

                    fileData.read(toneData);
                    
                    mediaPlayer = Manager.createPlayer(new ByteArrayInputStream(NokiaOTTDecoder.convertToMidi(toneData)), "");
                    fileTypeLabel.setText("File Type: audio/ott");
                } else {
                    mediaPlayer = Manager.createPlayer(new FileInputStream(filePath), "");
                }
                
                mediaPlayer.realize();
                mediaPlayer.prefetch();

                fileTypeLabel.setText("File Type: " + ((PlatformPlayer) mediaPlayer).contentType);
                updatePlaybackTicker(0, mediaPlayer.getDuration());
                startPlaybackTimer();
            } 
            catch (Exception e) 
            {
                e.printStackTrace();
            }
        }
    }

    private void playMedia() 
    {
        if (mediaPlayer != null && !isPlaying) 
        {
            mediaPlayer.start();
            isPlaying = true;
            if(playbackTimer == null) { startPlaybackTimer(); }
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
        if (mediaPlayer != null) 
        {
            pauseMedia();
            playbackTimer.cancel();
            playbackTimer = null;
            updatePlaybackTicker(0, 0);
            mediaPlayer.close();
            fileTypeLabel.setText("File Type: None");
            playbackTicker.setVisible(true);
            progressBar.setVisible(true);
        }
        mediaPlayer = null;
    }

    private void seekMediaBack() 
    {
        if (mediaPlayer != null) 
        {
            mediaPlayer.stop();
            mediaPlayer.setMediaTime(mediaPlayer.getMediaTime()-5000000);
            mediaPlayer.start();
        }
    }

    private void seekMediaForward() 
    {
        if (mediaPlayer != null) 
        {
            mediaPlayer.stop();
            mediaPlayer.setMediaTime(mediaPlayer.getMediaTime()+5000000);
            mediaPlayer.start();
        }
    }
}

class ProgressBar extends Panel 
{
    private int progress = 0;

    public ProgressBar() { }

    public void setProgress(int progress) 
    {
        this.progress = Math.max(0, Math.min(progress, 100));
        repaint();
    }

    @Override
    public void paint(Graphics g) 
    {
        synchronized (this)
        {
            // Background
            g.setColor(new Color(55, 55, 150));
            g.fillRect(0, 0, getWidth(), getHeight());

            // Progress Bar
            g.setColor(new Color(120, 120, 255));
            g.fillRect(0, 2, (int) ((getWidth() * progress) / 100.0), getHeight() - 4);
            
            // Border
            g.setColor(Color.WHITE);
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
        
    }
}