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

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
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
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import javax.microedition.m3g.Image2D;
import javax.microedition.m3g.Mesh;
import javax.microedition.m3g.SkinnedMesh;
import javax.microedition.m3g.Sprite3D;
import javax.microedition.m3g.Object3D;
import javax.microedition.m3g.World;

import com.mascotcapsule.micro3d.v3.Figure;
import com.mascotcapsule.micro3d.v3.Texture;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformImage;
import org.recompile.mobile.PlatformPlayer;

public class ObjectMemoryViewerDialog extends JDialog
{
	// TODO: Sorting doesn't really do anything yet.
	private static final int SORT_DEFAULT = 0;
	private static final int SORT_SIZE_INCR = 1;
	private static final int SORT_SIZE_DECR = 2;
	private static final int SORT_NAME_INCR = 3;
	private static final int SORT_NAME_DECR = 4;
	private static final String[] SORT_OPTIONS_LESS = {"Default", "Size ^", "Size v"};
	private static final String[] SORT_OPTIONS_ALL = {"Default", "Size ^", "Size v", "Name ^", "Name v"};
	
	private static final String[] SCALE_OPTIONS = {"25%", "50%", "75%", "100%", "200%", "300%", "400%"};
	private static final float[] SCALE_FACTORS = {0.25f, 0.50f, 0.75f, 1.00f, 2.00f, 3.00f, 4.00f};
	private static final String[] BG_OPTIONS = {"Black", "DarkGray", "Gray", "Blue", "Green", "Orange"};
	private static final int[] BG_VALUES = {0x000000, 0x444444, 0x888888, 0x8BADFF, 0x90D090, 0xFF9A67};
	private static final Color BUTTON_BGCOLOR = new Color(220, 220, 220);
	private static final Font DEBUG_TOGGLE_FONT = new Font("Dialog", Font.BOLD, 10);

	private final ImagePanel imagePanel, m3gImagePanel, mcv3ImagePanel;
	private final JLabel imageInfoLabel, playerInfoLabel, classInfoLabel, m3gInfoLabel, mcv3InfoLabel, totalHeapLabel;
	private final JComboBox imgScaleBox, m3gScaleBox, m3gBGBox, mcv3ScaleBox, mcv3BGBox;
	private final JComboBox imgSortBox, playerSortBox, classSortBox;
	private final DefaultListModel playerListModel, classListModel, m3gObjListModel, mcv3ObjListModel;
	private final JList playerList, classList, m3gObjList, mcv3ObjList;
	private final JTabbedPane tabbedPane;
	private final TitledBorder imageBorder, playerBorder, classBorder, m3gImageBorder, m3gObjBorder, mcv3ImageBorder, mcv3ObjBorder;
	private static boolean RenderableM3GFlag = false;
	private JToggleButton m3gShowOnlyRenderables, m3gDrawWireframe, m3gDrawUntextured, m3gDrawHiddenNodes, m3gMipmapping, m3gDisableCulling;
	private final M3G3DPreviewPanel m3gPreviewPanel;
	private final MCv33DPreviewPanel mcv3PreviewPanel;

	@SuppressWarnings("unchecked")
	public ObjectMemoryViewerDialog(Frame main)
	{
		super(main, "Object Memory Viewer", false);
		setSize(640, 480);
		setUndecorated(true);
		setLayout(new BorderLayout());

		// Created images (aside from emulator framebuffers and the like)
		imagePanel = new ImagePanel(new ImageSelectionListener()
		{
			public void onImageSelected(WeakReference<?> img) { updateSelectedImageInfo(img); }
		});
		imageBorder = BorderFactory.createTitledBorder("Images (0)");
		JScrollPane imageScroll = createScrollPane(imagePanel, imageBorder, 24);
		imageInfoLabel = createInfoLabel();
		imgScaleBox = createScaleBox(imagePanel);
		imgSortBox = createSortBox(true);

		JPanel imageControlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
		//imageControlsPanel.add(new JLabel("Sort by:"));
		//imageControlsPanel.add(imgSortBox);
		imageControlsPanel.add(new JLabel("Image Scale:"));
		imageControlsPanel.add(imgScaleBox);

		JPanel imageBottomPanel = new JPanel(new BorderLayout());
		imageBottomPanel.add(imageInfoLabel, BorderLayout.WEST);
		imageBottomPanel.add(imageControlsPanel, BorderLayout.EAST);
		JPanel imagesTabPanel = createTabPanel(imageScroll, imageBottomPanel);

		// Media Players
		playerListModel = new DefaultListModel();
		playerBorder = BorderFactory.createTitledBorder("Players (0)");
		playerList = createSelectionList(playerListModel, new Runnable()
		{
			public void run() { updateSelectedPlayerInfo(Mobile.memPlayers); }
		});
		JScrollPane playerScroll = createScrollPane(playerList, playerBorder, 0);
		playerInfoLabel = createInfoLabel();
		playerSortBox = createSortBox(true);
		
		JPanel playerControlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
		//playerControlsPanel.add(new JLabel("Sort by:"));
		//playerControlsPanel.add(playerSortBox);

		JPanel playerBottomPanel = new JPanel(new BorderLayout());
		playerBottomPanel.add(playerInfoLabel, BorderLayout.WEST);
		playerBottomPanel.add(playerControlsPanel, BorderLayout.EAST);
		JPanel playerTabPanel = createTabPanel(playerScroll, playerBottomPanel);

		// Loaded MIDlet classes
		classListModel = new DefaultListModel();
		classBorder = BorderFactory.createTitledBorder("Loaded MIDlet Classes (0)");
		classList = createSelectionList(classListModel, new Runnable()
		{
			public void run() { updateSelectedClassInfo(Mobile.memClasses); }
		});
		JScrollPane classScroll = createScrollPane(classList, classBorder, 0);
		classInfoLabel = createInfoLabel();
		classSortBox = createSortBox(false);
		
		JPanel classControlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
		//classControlsPanel.add(new JLabel("Sort by:"));
		//classControlsPanel.add(classSortBox);

		JPanel classBottomPanel = new JPanel(new BorderLayout());
		classBottomPanel.add(classInfoLabel, BorderLayout.WEST);
		classBottomPanel.add(classControlsPanel, BorderLayout.EAST);
		JPanel classesTabPanel = createTabPanel(classScroll, classBottomPanel);

		// M3G Panel
		m3gImagePanel = new ImagePanel(new ImageSelectionListener()
		{
			public void onImageSelected(WeakReference<?> img) { updateSelectedImageInfo(img); }
		});
		m3gImageBorder = BorderFactory.createTitledBorder("Image2Ds (0)");
		JScrollPane m3gImageScroll = createScrollPane(m3gImagePanel, m3gImageBorder, 24);

		m3gObjListModel = new DefaultListModel();
		m3gObjBorder = BorderFactory.createTitledBorder("Object3Ds (0)");
		m3gObjList = createSelectionList(m3gObjListModel, new Runnable()
		{
			public void run() { updateSelectedM3GObj(Mobile.memM3GObjects); }
		});
		JScrollPane m3gSceneObjectsScroll = createScrollPane(m3gObjList, m3gObjBorder, 0);

		JSplitPane m3gTopSplit = createFixedSplitPane(JSplitPane.HORIZONTAL_SPLIT, m3gImageScroll, m3gSceneObjectsScroll, 0.7);

		// Bottom area gets the 3D Preview
		m3gPreviewPanel = new M3G3DPreviewPanel();
		m3gPreviewPanel.setLayout(new BorderLayout());
		TitledBorder m3gPreviewTitle = BorderFactory.createTitledBorder("3D Preview");
		m3gPreviewTitle.setTitleColor(Color.WHITE);
		m3gPreviewPanel.setBorder(m3gPreviewTitle);

		m3gBGBox = new JComboBox(BG_OPTIONS);
		m3gBGBox.setSelectedItem("Black");
		m3gBGBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				int idx = m3gBGBox.getSelectedIndex();
				if (idx >= 0 && idx < BG_VALUES.length)
				{ 
					m3gPreviewPanel.setPreviewBackground(BG_VALUES[idx]);
					renderLastM3GObj();
				}
			}
		});

		m3gDrawWireframe = createJToggleButton("Wireframe Mode", new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m3gPreviewPanel.setWireframe(m3gDrawWireframe.isSelected());
				renderLastM3GObj();
			}
		});

		m3gDrawUntextured = createJToggleButton("Disable Textures", new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m3gPreviewPanel.setUntextured(m3gDrawUntextured.isSelected());
				renderLastM3GObj();
			}
		});

		m3gDrawHiddenNodes = createJToggleButton("Draw Hidden Nodes", new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m3gPreviewPanel.setDrawHidden(m3gDrawHiddenNodes.isSelected());
				renderLastM3GObj();
			}
		});
		m3gDrawHiddenNodes.setSelected(true);

		m3gMipmapping = createJToggleButton("Mipmapping", new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m3gPreviewPanel.setMipmapping(m3gMipmapping.isSelected());
				renderLastM3GObj();
			}
		});

		m3gDisableCulling = createJToggleButton("Disable Culling", new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m3gPreviewPanel.setCulling(m3gDisableCulling.isSelected());
				renderLastM3GObj();
			}
		});

		JPanel m3gPreviewBottomOverlay = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
		m3gPreviewBottomOverlay.setOpaque(false);
		m3gPreviewBottomOverlay.add(m3gBGBox);
		m3gPreviewBottomOverlay.add(m3gDrawWireframe);
		m3gPreviewBottomOverlay.add(m3gDrawUntextured);
		m3gPreviewBottomOverlay.add(m3gDrawHiddenNodes);
		m3gPreviewBottomOverlay.add(m3gMipmapping);
		m3gPreviewBottomOverlay.add(m3gDisableCulling);
		m3gPreviewPanel.add(m3gPreviewBottomOverlay, BorderLayout.SOUTH);

		JSplitPane m3gMainSplit = createFixedSplitPane(JSplitPane.VERTICAL_SPLIT, m3gTopSplit, m3gPreviewPanel, 0.4);

		m3gInfoLabel = createInfoLabel();
		m3gScaleBox = createScaleBox(m3gImagePanel);
		m3gPreviewPanel.setPreviewBackground(BG_VALUES[m3gBGBox.getSelectedIndex()]);

		m3gShowOnlyRenderables = createJToggleButton("Show Renderables", new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				RenderableM3GFlag = m3gShowOnlyRenderables.isSelected();
				refreshData();
			}
		});

		JPanel m3gControlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
		m3gControlsPanel.add(new JLabel("Image Scale:"));
		m3gControlsPanel.add(m3gScaleBox);
		m3gControlsPanel.add(m3gShowOnlyRenderables);

		JPanel m3gBottomPanel = new JPanel(new BorderLayout());
		m3gBottomPanel.add(m3gInfoLabel, BorderLayout.WEST);
		m3gBottomPanel.add(m3gControlsPanel, BorderLayout.EAST);

		JPanel m3gTabPanel = createTabPanel(m3gMainSplit, m3gBottomPanel);

		// MascotCapsuleV3 panel
		mcv3ImagePanel = new ImagePanel(new ImageSelectionListener()
		{
			public void onImageSelected(WeakReference<?> img) { updateSelectedImageInfo(img); }
		});
		mcv3ImageBorder = BorderFactory.createTitledBorder("Textures (0)");
		JScrollPane mcv3ImageScroll = createScrollPane(mcv3ImagePanel, mcv3ImageBorder, 24);

		mcv3ObjListModel = new DefaultListModel();
		mcv3ObjBorder = BorderFactory.createTitledBorder("Figures (0)");
		mcv3ObjList = createSelectionList(mcv3ObjListModel, new Runnable()
		{
			public void run() { updateSelectedMCv3Obj(Mobile.memMCv3Objects); }
		});
		JScrollPane mcv3SceneObjectsScroll = createScrollPane(mcv3ObjList, mcv3ObjBorder, 0);

		JSplitPane mcv3TopSplit = createFixedSplitPane(JSplitPane.HORIZONTAL_SPLIT, mcv3ImageScroll, mcv3SceneObjectsScroll, 0.7);
		
		mcv3PreviewPanel = new MCv33DPreviewPanel();
		mcv3PreviewPanel.setLayout(new BorderLayout());
		TitledBorder mcv3PreviewTitle = BorderFactory.createTitledBorder("3D Preview");
		mcv3PreviewTitle.setTitleColor(Color.WHITE);
		mcv3PreviewPanel.setBorder(mcv3PreviewTitle);

		mcv3BGBox = new JComboBox(BG_OPTIONS);
		mcv3BGBox.setSelectedItem("Black");
		mcv3BGBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				int idx = mcv3BGBox.getSelectedIndex();
				if (idx >= 0 && idx < BG_VALUES.length)
				{ 
					mcv3PreviewPanel.setPreviewBackground(BG_VALUES[idx]);
					renderLastM3GObj();
				}
			}
		});

		JPanel mcv3PreviewBottomOverlay = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
		mcv3PreviewBottomOverlay.setOpaque(false);
		mcv3PreviewBottomOverlay.add(mcv3BGBox);
		mcv3PreviewPanel.add(mcv3PreviewBottomOverlay, BorderLayout.SOUTH);

		JSplitPane mcv3MainSplit = createFixedSplitPane(JSplitPane.VERTICAL_SPLIT, mcv3TopSplit, mcv3PreviewPanel, 0.4);

		mcv3InfoLabel = createInfoLabel();
		mcv3ScaleBox = createScaleBox(mcv3ImagePanel);
		mcv3PreviewPanel.setPreviewBackground(BG_VALUES[mcv3BGBox.getSelectedIndex()]);

		JPanel mcv3ControlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
		mcv3ControlsPanel.add(new JLabel("Image Scale:"));
		mcv3ControlsPanel.add(mcv3ScaleBox);

		JPanel mcv3BottomPanel = new JPanel(new BorderLayout());
		mcv3BottomPanel.add(mcv3InfoLabel, BorderLayout.WEST);
		mcv3BottomPanel.add(mcv3ControlsPanel, BorderLayout.EAST);

		JPanel mcv3TabPanel = createTabPanel(mcv3MainSplit, mcv3BottomPanel);

		// Add everyone to the tabbed pane
		tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Images (0)", imagesTabPanel);
		tabbedPane.addTab("Players (0)", playerTabPanel);
		tabbedPane.addTab("Classes (0)", classesTabPanel);
		tabbedPane.addTab("M3G Data", m3gTabPanel);
		tabbedPane.addTab("MascotCapsuleV3 Data", mcv3TabPanel);
		add(tabbedPane, BorderLayout.CENTER);

		// Bottom bar, visible on all tabs.
		JPanel bottomBar = new JPanel(new BorderLayout());
		bottomBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

		totalHeapLabel = new JLabel("WAITING DATA...");
		JButton refreshButton = new JButton("Refresh");
		refreshButton.setMargin(new Insets(4, 4, 4, 4));
		refreshButton.setFont(new Font("Dialog", Font.BOLD, 12));
		refreshButton.setContentAreaFilled(false);
		refreshButton.setOpaque(true);
		refreshButton.setBackground(BUTTON_BGCOLOR);
		refreshButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) { refreshData(); }
		});

		bottomBar.add(totalHeapLabel, BorderLayout.WEST);
		bottomBar.add(refreshButton, BorderLayout.EAST);
		add(bottomBar, BorderLayout.SOUTH);
	}

	private static JToggleButton createJToggleButton(String text, ActionListener listener)
	{
		JToggleButton btn = new JToggleButton(text);
		btn.setMargin(new Insets(4, 0, 4, 0));
		btn.setFont(DEBUG_TOGGLE_FONT);
		btn.setOpaque(true);
		btn.setBackground(BUTTON_BGCOLOR);
		btn.addActionListener(listener);
		return btn;
	}

	private static JScrollPane createScrollPane(JComponent view, Border border, int wheelIncrement)
	{
		JScrollPane scroll = new JScrollPane(view);
		if (wheelIncrement > 0)
		{
			scroll.getVerticalScrollBar().setUnitIncrement(wheelIncrement);
			scroll.getHorizontalScrollBar().setUnitIncrement(wheelIncrement);
		}
		if (border != null) { scroll.setBorder(border); }
		return scroll;
	}

	private static JSplitPane createFixedSplitPane(int orientation, Component first, Component second, double resizeWeight)
	{
		JSplitPane split = new JSplitPane(orientation, first, second);
		split.setResizeWeight(resizeWeight);
		split.setContinuousLayout(true);
		split.setDividerSize(2);
		split.setEnabled(false);
		split.setDividerLocation(0.5);
		return split;
	}

	@SuppressWarnings("unchecked")
	private JComboBox createScaleBox(final ImagePanel targetPanel)
	{
		final JComboBox box = new JComboBox(SCALE_OPTIONS);
		box.setSelectedItem("100%");
		box.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				int idx = box.getSelectedIndex();
				if (idx >= 0 && idx < SCALE_FACTORS.length) { targetPanel.setScale(SCALE_FACTORS[idx]); }
			}
		});
		return box;
	}

	@SuppressWarnings("unchecked")
	private JComboBox createSortBox(final boolean noNameSort)
	{
		final JComboBox box = new JComboBox(noNameSort ? SORT_OPTIONS_LESS : SORT_OPTIONS_ALL);
		box.setSelectedItem("Default");
		box.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				int idx = box.getSelectedIndex();
				// TODO: Apply sort type here later?
			}
		});
		return box;
	}

	@SuppressWarnings("unchecked")
	private static JList createSelectionList(DefaultListModel model, final Runnable onSelectionChanged)
	{
		JList list = new JList(model);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addListSelectionListener(new javax.swing.event.ListSelectionListener()
		{
			public void valueChanged(javax.swing.event.ListSelectionEvent e)
			{
				if (!e.getValueIsAdjusting()) { onSelectionChanged.run(); }
			}
		});
		return list;
	}

	private void renderLastM3GObj()
	{
		synchronized (Mobile.memM3GObjects)
		{
			WeakReference<Object3D> objRef = getLiveRef(Mobile.memM3GObjects, m3gObjList.getSelectedIndex());
			m3gPreviewPanel.renderObject(getReference(objRef));
		}
	}

	private void renderLastMCv3Obj()
	{
		synchronized (Mobile.memMCv3Objects)
		{
			WeakReference<Figure> objRef = getLiveRef(Mobile.memMCv3Objects, mcv3ObjList.getSelectedIndex());
			mcv3PreviewPanel.renderObject(getReference(objRef));
		}
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
		ArrayList<WeakReference<Image2D>> m3gImages = Mobile.memM3GImages;
		ArrayList<WeakReference<Object3D>> m3gObjects = Mobile.memM3GObjects;
		ArrayList<WeakReference<Texture>> mcv3Images = Mobile.memMCv3Images;
		ArrayList<WeakReference<Figure>> mcv3Objects = Mobile.memMCv3Objects;

		int imageNum = 0;
		int playerNum = 0;
		int classNum = 0;
		int m3gImageNum = 0;
		int m3gObjNum = 0;
		int mcv3ImageNum = 0;
		int mcv3ObjNum = 0;

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
		
		synchronized (m3gImages)
		{
			m3gImageNum = countRefs(m3gImages);
			m3gImagePanel.setImage2Ds(m3gImages);
			updateSelectedImageInfo(m3gImagePanel.getSelectedImage());
		}

		synchronized (m3gObjects)
		{
			m3gObjNum = countRefs(m3gObjects);
			updateM3GObjs(m3gObjects);
		}

		synchronized (mcv3Images)
		{
			mcv3ImageNum = countRefs(mcv3Images);
			mcv3ImagePanel.setTextures(mcv3Images);
			updateSelectedImageInfo(mcv3ImagePanel.getSelectedImage());
		}

		synchronized (mcv3Objects)
		{
			mcv3ObjNum = countRefs(mcv3Objects);
			updateMCv3Objs(mcv3Objects);
		}

		updateTab(0, imageBorder, "Images", imageNum);
		updateTab(1, playerBorder, "Players", playerNum);
		updateTab(2, classBorder, "Classes", classNum);
		classBorder.setTitle("Loaded MIDlet Classes (" + classNum + ")");

		m3gImageBorder.setTitle("Image2Ds (" + m3gImageNum + ")");
		m3gObjBorder.setTitle("Object3Ds (" + m3gObjNum + ")");

		mcv3ImageBorder.setTitle("Textures (" + mcv3ImageNum + ")");
		mcv3ObjBorder.setTitle("Figures (" + mcv3ObjNum + ")");
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
			T obj = getReference(ref);
			if (obj != null)
			{
				if (RenderableM3GFlag && obj instanceof Object3D && !isRenderableM3G((Object3D) obj))
				{
					continue;
				}
				if (liveIdx == targetIndex) { return ref; }
				liveIdx++;
			}
		}
		return null;
	}

	private void updateSelectedImageInfo(WeakReference<?> imgRef)
	{
		Object ref = getReference(imgRef);
		if (ref == null)
		{
			imageInfoLabel.setText("Selected: None");
			m3gInfoLabel.setText("Selected: None");
			mcv3InfoLabel.setText("Selected: None");
			return;
		}

		double heapKb = estimateImageHeapBytes(imgRef) / 1024.0;

		if (ref instanceof PlatformImage)
		{
			PlatformImage img = (PlatformImage) ref;
			BufferedImage canvas = img.getCanvas();

			imageInfoLabel.setText("Size: " + canvas.getWidth() + "x" + canvas.getHeight() +
				" | Format: " + getImageTypeName(canvas.getType()) +
				" | Heap: " + String.format("%.2f", heapKb) + " KB");
		}
		else if (ref instanceof Image2D)
		{
			Image2D img2d = (Image2D) ref;
			m3gInfoLabel.setText("Size: " + img2d.getWidth() + "x" + img2d.getHeight() +
				" | Format: " + img2d.getFormatString() +
				" | Heap: " + String.format("%.2f", heapKb) + " KB");
		}
		else if (ref instanceof Texture)
		{
			Texture tex = (Texture) ref;
			mcv3InfoLabel.setText("Size: " + tex.getWidth() + "x" + tex.getHeight() + " | Heap: " + String.format("%.2f", heapKb) + " KB");
		}
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
		// TODO: M3G and MascotCapsule

		double totalKb = imageKb + playerKb + classKb;
		totalHeapLabel.setText("Total Usage: " + String.format("%.2f", totalKb) + "KB [" +
			String.format("%.2f", imageKb) + "KB Image | " +
			String.format("%.2f", playerKb) + "KB Sound | " +
			String.format("%.2f", classKb) + "KB Class]");
	}

	private long estimateImageHeapBytes(WeakReference<?> imgRef)
	{
		Object ref = getReference(imgRef);
		if (ref instanceof PlatformImage)
		{
			PlatformImage img = (PlatformImage) ref;
			BufferedImage canvas = img.getCanvas();
			return (canvas != null) ? (long) canvas.getWidth() * canvas.getHeight() * 4L : 0L;
		}
		if (ref instanceof Image2D)
		{
			Image2D img2d = (Image2D) ref;
			return (long) img2d.getWidth() * img2d.getHeight() * img2d.getBpp();
		}
		if (ref instanceof Texture)
		{
			Texture tex = (Texture) ref;
			return (long) tex.getWidth() * tex.getHeight() * 4;
		}
		return 0L;
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

			String volStr = (vc != null) ? (vc.getLevel() + "%") : "N/A";
			String rateStr = (tc != null) ? ((tc.getRate() / 1000) + "%") : "N/A";
			String bpmStr = (tc != null) ? ((tc.getTempo() / 1000) + "") : "N/A";

			playerInfoLabel.setText("Type: " + contentType + " | State: " + state +
				" | Vol.: " + volStr + " | Rate: " + rateStr + " | BPM: " + bpmStr);
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

		classInfoLabel.setText("Package: " + pkgName + " | Super: " + superName);
	}

	@SuppressWarnings("unchecked")
	private void updateM3GObjs(ArrayList<WeakReference<Object3D>> objects)
	{
		WeakReference<Object3D> prevRef = getLiveRef(objects, m3gObjList.getSelectedIndex());
		Object3D previouslySelected = getReference(prevRef);
	
		m3gObjListModel.clear();
		if (objects == null)
		{
			updateSelectedM3GObj(null);
			return;
		}
	
		int newSelectedIndex = -1;
		for (WeakReference<Object3D> objRef : objects)
		{
			Object3D obj = getReference(objRef);
			if (obj != null && (!RenderableM3GFlag || isRenderableM3G(obj)))
			{
				String objName = obj.toString();
				// TODO: Expand on this?
				m3gObjListModel.addElement(objName.substring(objName.lastIndexOf('.') + 1));
	
				if (previouslySelected != null && obj == previouslySelected && newSelectedIndex < 0)
				{
					newSelectedIndex = m3gObjListModel.getSize() - 1;
				}
			}
		}
	
		if (newSelectedIndex >= 0) { m3gObjList.setSelectedIndex(newSelectedIndex); }
		else { m3gObjList.clearSelection(); }
		updateSelectedM3GObj(objects);
	}

	@SuppressWarnings("unchecked")
	private void updateMCv3Objs(ArrayList<WeakReference<Figure>> objects)
	{
		WeakReference<Figure> prevRef = getLiveRef(objects, mcv3ObjList.getSelectedIndex());
		Figure previouslySelected = getReference(prevRef);
	
		mcv3ObjListModel.clear();
		if (objects == null)
		{
			updateSelectedMCv3Obj(null);
			return;
		}
	
		int newSelectedIndex = -1;
		for (WeakReference<Figure> objRef : objects)
		{
			Figure obj = getReference(objRef);
			if (obj != null)
			{
				String objName = obj.toString();
				// TODO: Expand on this?
				mcv3ObjListModel.addElement(objName.substring(objName.lastIndexOf('.') + 1));
	
				if (previouslySelected != null && obj == previouslySelected && newSelectedIndex < 0)
				{
					newSelectedIndex = mcv3ObjListModel.getSize() - 1;
				}
			}
		}
	
		if (newSelectedIndex >= 0) { mcv3ObjList.setSelectedIndex(newSelectedIndex); }
		else { mcv3ObjList.clearSelection(); }
		updateSelectedMCv3Obj(objects);
	}

	private static boolean isRenderableM3G(Object3D obj)
	{
		return (obj instanceof Mesh) || (obj instanceof SkinnedMesh)
			|| (obj instanceof Sprite3D) || (obj instanceof World);
	}

	private void updateSelectedM3GObj(ArrayList<WeakReference<Object3D>> objects)
	{
		WeakReference<Object3D> objRef = getLiveRef(objects, m3gObjList.getSelectedIndex());
		Object3D obj = getReference(objRef);
		if (obj == null)
		{
			m3gInfoLabel.setText("Selected: None");
			return;
		}

		// TODO: Expand on this?
		m3gInfoLabel.setText(obj.toString());
		m3gPreviewPanel.renderObject(obj);
	}

	private void updateSelectedMCv3Obj(ArrayList<WeakReference<Figure>> objects)
	{
		WeakReference<Figure> objRef = getLiveRef(objects, mcv3ObjList.getSelectedIndex());
		Figure obj = getReference(objRef);
		if (obj == null)
		{
			mcv3InfoLabel.setText("Selected: None");
			return;
		}

		// TODO: Expand on this?
		mcv3InfoLabel.setText(obj.toString());
		mcv3PreviewPanel.renderObject(obj);
	}

	private static String formatTime(long microseconds)
	{
		long totalSeconds = microseconds / 1000000L;
		long minutes = totalSeconds / 60L;
		return String.format("%02d:%02d", minutes, totalSeconds % 60L);
	}
}
