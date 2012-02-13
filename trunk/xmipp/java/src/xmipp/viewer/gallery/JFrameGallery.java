/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * JFrameRotSpectra.java
 *
 * Created on 22-abr-2010, 12:35:07
 */
package xmipp.viewer.gallery;

import xmipp.viewer.ImageGallery;
import xmipp.viewer.ImageItem;
import xmipp.viewer.ImageItemRenderer;
import xmipp.viewer.MetadataGallery;
import xmipp.viewer.TestRenderer;
import xmipp.viewer.VolumeGallery;
import xmipp.viewer.gallery.models.GalleryRowHeaderModel;
import xmipp.viewer.gallery.models.MDTableModel;
import xmipp.viewer.gallery.models.GalleryTableColumnModel;
import xmipp.viewer.imageitems.tableitems.AbstractGalleryImageItem;
import xmipp.viewer.gallery.renderers.ImageRenderer;
import xmipp.viewer.gallery.renderers.RowHeaderRenderer;
import xmipp.viewer.windows.ImagesWindowFactory;
import xmipp.utils.DEBUG;
import xmipp.utils.Labels;
import xmipp.utils.Param;
import xmipp.viewer.gallery.models.AbstractXmippTableModel;
import xmipp.viewer.gallery.models.VolumeTableModel;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import xmipp.jni.Filename;
import xmipp.jni.ImageGeneric;
import xmipp.jni.MetaData;
import xmipp.ij.XmippImageConverter;
import xmipp.utils.Resources;

/**
 * 
 * @author Juanjo Vega
 */
public class JFrameGallery extends JFrame {
	private static final long serialVersionUID = -8957336972082018823L;

	private final static int DELAY_TO_UPDATE = 500;
	private static int update_counter = 0;
	private ImageGallery gallery;
	private GalleryTableColumnModel columnModel;
	private GalleryRowHeaderModel rowHeaderModel;
	private int previousSelectedRow, previousSelectedCol;
	private JList rowHeader;
	private ImageItemRenderer renderer;
	private Timer updateTimer = new Timer(true); // Timer for zoom.
	private TableUpdater tableUpdaterTask; // Associated task for zoom timer.
	private boolean isUpdating;
	// this flag will be used to avoid firing properties change events
	// when the change is from our code and not external user interaction
	private boolean internalChange = false;
	private boolean autoAdjustColumns = false;
	private JPopUpMenuGallery jpopUpMenuTable;
	private JMenuBarTable menu;
	Param parameters;
	JFileChooser fc = new JFileChooser();

	private javax.swing.JLabel jlZoom;
	private javax.swing.JLabel jlGoto;
	private javax.swing.JLabel jlRows;
	private javax.swing.JLabel jlColumns;
	private javax.swing.JToggleButton jcbAutoAdjustColumns;
	private javax.swing.JComboBox jcbMDLabels;
	private javax.swing.JCheckBox jcbShowLabels;
	private javax.swing.JCheckBox jcbSortByLabel;
	protected javax.swing.JPanel jpBottom;
	protected javax.swing.JSpinner jsColumns;
	protected javax.swing.JSpinner jsGoToImage;
	private javax.swing.JScrollPane jspContent;
	protected javax.swing.JSpinner jsRows;
	protected javax.swing.JSpinner jsZoom;
	// private javax.swing.JToggleButton jtbNormalize;
	// private javax.swing.JToggleButton jtbUseGeometry;
	private javax.swing.JTable table;
	private javax.swing.JToolBar toolBar;

	public enum RESLICE_MODE {

		TOP_Y, RIGHT_X
	}

	/** Constructors */
	public JFrameGallery(String filename, Param parameters) {
		super();		

		try {
			boolean volume = Filename.isVolume(filename);
			createGUI(volume ? new VolumeGallery(filename, parameters.zoom)
					: new MetadataGallery(filename, parameters.zoom), parameters);
			//createGUI(new MetadataGallery(filename, parameters.zoom), parameters);
		} catch (Exception e) {
			DEBUG.printException(e);
			IJ.error(e.getMessage());
		}
	}

	public JFrameGallery(String filenames[], Param parameters) {
		this(filenames, null, parameters);
	}

	public JFrameGallery(String filenames[], boolean enabled[], Param parameters) {
		super();
		//createGUI(new MDTableModel(filenames, enabled), parameters);
	}

	/**
	 * Function to create general GUI base on a TableModel. It will use helper
	 * functions to create different components of the GUI
	 */
	private void createGUI(ImageGallery tm, Param parameters) {
		gallery = tm;
		readParams(parameters);
		isUpdating = true; // avoid handling some changes events

		//FIXME: setTitle(gallery.getTitle());
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setMinimumSize(new Dimension(600, 400));
		setPreferredSize(new Dimension(800, 600));
		ImagesWindowFactory.setConvenientSize(this);

		// Get main pane and set layout
		Container container = getContentPane();
		container.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		// Create toolbar buttons
		createToolbar();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		container.add(toolBar, c);

		setInitialValues();
		// Create table
		createTable();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 1.0;
		c.weighty = 1.0;
		container.add(jspContent, c);

		jpopUpMenuTable = new JPopUpMenuGallery();

		// Stacks will be "auto-normalized".
		//FIXME: setNormalized(gallery.isVolume());

		// Geometry info is used if present, otherwise button is disabled.
		boolean containsGeometry = false;//FIXME: gallery.containsGeometryInfo();
		// tableModel.setUseGeometry(jtbUseGeometry.isSelected());
		menu.enableApplyGeo(containsGeometry);
		menu.selectApplyGeo(containsGeometry);
		// jtbUseGeometry.setSelected(containsGeometry);
		// jtbUseGeometry.setEnabled(containsGeometry);

		// updateTable();
		
		pack();
		isUpdating = false;
		// startUpdater();
		// Register some listeners
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowOpened(java.awt.event.WindowEvent evt) {
				formWindowOpened(evt);
			}
		});

		addComponentListener(new java.awt.event.ComponentAdapter() {
			public void componentResized(java.awt.event.ComponentEvent evt) {
				formComponentResized(evt);
			}
		});
		setVisible(true);
	}

	private void readParams(Param parameters) {
		this.parameters = parameters;
	}

	/**
	 * This function will set the values of colums and rows spinners depending
	 * on the value of the table model rows and columns
	 */
	private void updateColumnsRowsValues() {
//		jsColumns.setValue(table.getColumnCount());
//		jsRows.setValue(table.getRowCount());
	}

	private void setInitialValues() {
		boolean adjust = false;
		if (parameters.columns > 0)
			gallery.setColumns(parameters.columns);
		else if (parameters.rows > 0)
			gallery.setRows(parameters.rows);
		else {
			adjustColumns();
			adjust = true;
		}
		setAutoAdjustColumns(adjust);
//		updateColumnsRowsValues();
		//
		// DEBUG.printMessage(String.format("================ readParams: Table Model:\nsize: %d, cols: %d rows: %d",
		// tableModel.getSize(), tableModel.getColumnCount(),
		// tableModel.getRowCount()));
		//
		// if (tableModel.getSize() > 0) {

		// setAutoAdjustColumns(autoAdjustColumns);
		//
		// double scale = tableModel.getInitialZoomScale(
		// jspContent.getVisibleRect().width,
		// table.getIntercellSpacing().width);
		// //setZoom((int) (scale * 100));
		// setZoom(100);
		// }
		// else {
		// jsZoom.setEnabled(false);
		// jcbAutoAdjustColumns.setEnabled(autoAdjustColumns);
		// //jcbShowLabels.setEnabled(autoAdjustColumns);
		// jsRows.setEnabled(autoAdjustColumns);
		// jsColumns.setEnabled(autoAdjustColumns);
		// jsGoToImage.setEnabled(false);
		// }
	}

	private void createTable() {
		// Create scrollable area for data on the table
		jspContent = new javax.swing.JScrollPane();
		// tableModel.autoAdjustColumns(
		// // jsPanel.getVisibleRect().width - rowHeader.getWidth(),
		// // jsPanel.getViewportBorderBounds().width -
		// // rowHeader.getWidth(),
		// //jspContent.getViewport().getWidth()
		// 600,
		// table.getIntercellSpacing().width);
		//
		// jsRows.setValue(tableModel.getRowCount());
		// jsColumns.setValue(tableModel.getColumnCount());

		//setZoom(parameters.zoom);
		//FIXME:
//		if (gallery.containsGeometryInfo()) {
//			((MDTableModel) gallery).setUseGeometry(true);
//		}
		// Create row header for enumerate rows
		rowHeaderModel = new GalleryRowHeaderModel(gallery.getRowCount(), 1);
		rowHeader = new JList();
		rowHeader.setModel(rowHeaderModel);
		LookAndFeel.installColorsAndFont(rowHeader, "TableHeader.background",
				"TableHeader.foreground", "TableHeader.font");
		rowHeader.setCellRenderer(new RowHeaderRenderer());
		jspContent.setRowHeaderView(rowHeader);

		table = new JTable();
		// Create column model
    	columnModel = new GalleryTableColumnModel(gallery.getCellSize().width);
		table.setColumnModel(columnModel);
		table.setModel(gallery);
		//int h = 25;
		//table.setRowHeight(h);
		//rowHeader.setFixedCellHeight(h);
		jspContent.setViewportView(table);
		table.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
		renderer = gallery.getRenderer();
    	table.setDefaultRenderer(ImageItem.class, renderer);
    	
		
		// Create the menu for table
		menu = new JMenuBarTable();
		setJMenuBar(menu);

		// DEBUG.printMessage("WIDTH: " + jspContent.getVisibleRect().width);
		// DEBUG.printMessage("preferred: " + getPreferredSize().toString());
		gallery.addTableModelListener(new TableModelListener() {
			
			@Override
			public void tableChanged(TableModelEvent e) {
				updateTable();				
			}
		});
		
		table.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent evt) {
				tableMouseClicked(evt);
			}
		});
		
		updateTable();
		

		// int WIDTH = getPreferredSize().width;
		// double scale = tableModel.getInitialZoomScale(
		// //jspContent.getVisibleRect().width,
		// WIDTH,
		// table.getIntercellSpacing().width);
		// setZoom((int) (scale * 100));
	}// function createTable

	private void updateTable() {
		// if (table.isShowing()) {
		boolean updatingState = isUpdating;
		isUpdating = true;
		update_counter++;
		DEBUG.printMessage(" *** Updating table: " + update_counter);// System.currentTimeMillis()
																		// );
		DEBUG.printStackTrace();

		//FIXME:gallery.updateSort();

		if (gallery.getSize() > 0) {
//			 DEBUG.printMessage(String.format("updateTable: Table Model:\nsize: %d, cols: %d rows: %d",
//			 gallery.getSize(), gallery.getColumnCount(),
//			 gallery.getRowCount()));
			Dimension dimension = gallery.getCellSize();
			//renderer.setPreferredSize(dimension);

			// Adjusts rows size.
			table.setRowHeight(dimension.height);
			rowHeader.setFixedCellHeight(dimension.height);
			rowHeaderModel.setSize(gallery.getRowCount());
			// Adjusts columns width
			columnModel.setWidth(dimension.width);

			// If auto adjust columns is enabled, refresh!
			jsRows.setValue(gallery.getRowCount());
			jsColumns.setValue(gallery.getColumnCount());

			rowHeader.revalidate();
			rowHeader.repaint();
			//table.revalidate();
			// repaint();
		}

		isUpdating = updatingState;
		// }
	}// function updateTable

	private void startUpdater() {

		if (tableUpdaterTask != null)
			tableUpdaterTask.cancel();
		tableUpdaterTask = new TableUpdater();
		updateTimer.schedule(tableUpdaterTask, DELAY_TO_UPDATE);
	}// function startUpdater

	/** Adjust the columns depending on the current windows width and cell width
	 */
	private void adjustColumns() {
		int w = getSize().width;
		gallery.adjustColumn(w - 50);
		DEBUG.printMessage(String.format("==>> JFrameGallery.autoAdjust: width: %d",	w));
		//int rw = rowHeader.getWidth();
		// DEBUG.printStackTrace();
		//FIXME
//		gallery.autoAdjustColumns(
//		// jsPanel.getVisibleRect().width - rowHeader.getWidth(),
//		// jsPanel.getViewportBorderBounds().width -
//		// rowHeader.getWidth(),
//		// jspContent.getViewport().getWidth() - rowHeader.getWidth()
//				w - rw, table.getIntercellSpacing().width);
//		updateColumnsRowsValues();
	}

	public void setAutoAdjustColumns(boolean autoAdjustColumns) {
		this.autoAdjustColumns = autoAdjustColumns;
		jcbAutoAdjustColumns.setSelected(autoAdjustColumns);
		jsColumns.setEnabled(!autoAdjustColumns);
		jsRows.setEnabled(!autoAdjustColumns);
	}

	private void goToImage(int index) {
		//gallery.setSelected(index);

		int coords[] = gallery.getCoords(index);

		// Gets current selected cell bounds.
		Rectangle rect = table.getCellRect(coords[0], coords[1], true);

		// Ensures item is visible.
		Point pos = jspContent.getViewport().getViewPosition();
		rect.translate(-pos.x, -pos.y);
		jspContent.getViewport().scrollRectToVisible(rect);

		repaint();
	}

	private void avgImage() {
		//ImagesWindowFactory.captureFrame(ImageOperations.mean(tableModel.getAllItems()));
	}

	private void stdDevImage() {
		//ImagesWindowFactory.captureFrame(ImageOperations.std_deviation(tableModel.getAllItems()));
	}

	private void setNormalized(boolean normalize) {
		// jtbNormalize.setSelected(normalize);
		menu.selectNormalize(normalize);
		//gallery.setNormalized(normalize);
	}

	private void openAsStack() {
		//ImagesWindowFactory.openGalleryAsImagePlus(gallery);
	}

	private void openAs3D() {
		//ImagesWindowFactory.openGalleryAs3D(gallery);
	}

	private void saveAsMetadata(boolean all) {
		// Sets path and filename automatically.
//		String filename = gallery.getFilename() != null ? gallery
//				.getFilename() : "";
//		fc.setSelectedFile(new File(forceExtension(filename, ".xmd")));
//
//		if (fc.showSaveDialog(this) != JFileChooser.CANCEL_OPTION) {
//			boolean response = true;
//			if (fc.getSelectedFile().exists()) {
//				response = JOptionPane.showConfirmDialog(null,
//						Labels.MESSAGE_OVERWRITE_FILE,
//						Labels.MESSAGE_OVERWRITE_FILE_TITLE,
//						JOptionPane.OK_CANCEL_OPTION,
//						JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION;
//			}
//
//			if (response) {
//				String path = fc.getSelectedFile().getAbsolutePath();
//				if (gallery.saveAsMetadata(path, all)) {
//					JOptionPane.showMessageDialog(this,
//							Labels.MESSAGE_FILE_SAVED + path,
//							Labels.MESSAGE_FILE_SAVED_TITLE,
//							JOptionPane.INFORMATION_MESSAGE);
//				}
//			}
//		}
	}

	private void saveAsStack(boolean all) {
		// Sets path and filename automatically.
//		String filename = gallery.getFilename() != null ? gallery
//				.getFilename() : "";
//		fc.setSelectedFile(new File(forceExtension(filename, ".stk")));
//
//		if (fc.showSaveDialog(this) != JFileChooser.CANCEL_OPTION) {
//			boolean response = true;
//			if (fc.getSelectedFile().exists()) {
//				response = JOptionPane.showConfirmDialog(null,
//						Labels.MESSAGE_OVERWRITE_FILE,
//						Labels.MESSAGE_OVERWRITE_FILE_TITLE,
//						JOptionPane.OK_CANCEL_OPTION,
//						JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION;
//			}
//
//			if (response) {
//				String path = fc.getSelectedFile().getAbsolutePath();
//				if (gallery.saveAsStack(path, all)) {
//					JOptionPane.showMessageDialog(this,
//							Labels.MESSAGE_FILE_SAVED + path,
//							Labels.MESSAGE_FILE_SAVED_TITLE,
//							JOptionPane.INFORMATION_MESSAGE);
//				}
//			}
//		}
	}

	static String forceExtension(String filename, String ext) {
		int dot = filename.lastIndexOf(".");
		return filename.substring(0, dot) + ext;
	}

	public void pca() {
//		String filename = gallery.getFilename();
//
//		try {
//			MetaData md = new MetaData(filename);
//			ImageGeneric image = new ImageGeneric();
//
//			md.getPCAbasis(image);
//
//			ImagePlus imp = XmippImageConverter.convertToImageJ(image);
//			imp.setTitle("PCA: " + filename);
//			ImagesWindowFactory.captureFrame(imp);
//		} catch (Exception ex) {
//			DEBUG.printException(ex);
//		}
	}

	public void fsc() {
//		String filename = gallery.getFilename();
//
//		try {
//			MetaData md = new MetaData(filename);
//
//			ImagesWindowFactory.openFSCWindow(filename);
//		} catch (Exception ex) {
//			DEBUG.printException(ex);
//		}
	}

	private void reslice(RESLICE_MODE mode) throws Exception {
		String command = null;

		switch (mode) {
		case TOP_Y:
			command = "Top";
			break;
		case RIGHT_X:
			command = "Right";
			break;
		}

		// Get volume ImagePlus.
//		String filename = gallery.getFilename();
//		ImagePlus volume = XmippImageConverter.loadImage(filename);
//
//		// Reslice.
//		IJ.run(volume, "Reslice [/]...", "slice=1.000 start=" + command);
//		volume = WindowManager.getCurrentImage();
//		volume.getWindow().setVisible(false);
//
//		// Save temp file.
//		int index = filename.lastIndexOf(".");
//		String name = filename.substring(
//				filename.lastIndexOf(File.separator) + 1, index);
//		String ext = filename.substring(index);
//		File f = File.createTempFile(name + "_" + command, ext);
//		f.deleteOnExit();
//
//		XmippImageConverter.saveImage(volume, f.getAbsolutePath());
//		volume.close();
//
//		// Open as gallery.
//		ImagesWindowFactory.openFileAsGallery(f.getCanonicalPath());
	}

	/***
	 * Helper function to create toolbar toggle buttons
	 */
	protected JToggleButton createToggleButton(String icon, String text,
			ActionListener listener) {
		// Add toggle button to set/unset global normalization
		JToggleButton toggleBtn = new javax.swing.JToggleButton();
		toggleBtn.setFocusable(false);
		toggleBtn.setIcon(Resources.getIcon(icon));
		toggleBtn.setToolTipText(text);
		toggleBtn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
		toggleBtn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
		toggleBtn.addActionListener(listener);
		return toggleBtn;
	}

	/***
	 * Function to create the main toolbar
	 */
	protected void createToolbar() {
		// Create Main TOOLBAR
		toolBar = new javax.swing.JToolBar();
		toolBar.setRollover(true);
		toolBar.setLayout(new FlowLayout(FlowLayout.LEFT));
		// toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.X_AXIS));

		// Add toggle button to set/unset global normalization
		// jtbNormalize = createToggleButton(Resources.NORMALIZE,
		// Labels.MSG_NORMALIZE, new java.awt.event.ActionListener() {
		// public void actionPerformed(java.awt.event.ActionEvent evt) {
		// jtbNormalizeActionPerformed(evt);
		// }
		// });
		// toolBar.add(jtbNormalize);
		// // Add toggle button to set/unset apply geometry when read from
		// metadata
		// jtbUseGeometry = createToggleButton(Resources.APPLY_GEO,
		// Labels.MSG_APPLY_GEO, new java.awt.event.ActionListener() {
		// public void actionPerformed(java.awt.event.ActionEvent evt) {
		// jtbUseGeometryActionPerformed(evt);
		// }
		// });
		// toolBar.add(jtbUseGeometry);

		//toolBar.addSeparator();
		jlZoom = new javax.swing.JLabel();
		jsZoom = new javax.swing.JSpinner();
		jlZoom.setIcon(Resources.getIcon(Resources.ZOOM));
		jlZoom.setToolTipText(Labels.LABEL_ZOOM);
		toolBar.add(jlZoom);

		jsZoom.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(parameters.zoom),
				Integer.valueOf(1), null, Integer.valueOf(1)));
		jsZoom.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				jsZoomStateChanged(evt);
			}
		});
		
		toolBar.add(jsZoom);

		toolBar.addSeparator();

		// TODO: MOVE THIS TO MENU OPTION
		// jcbShowLabels = new javax.swing.JCheckBox();
		// jcbMDLabels = new javax.swing.JComboBox();
		// jcbShowLabels.setText(Labels.LABEL_SHOW_LABELS);
		// jcbShowLabels.addActionListener(new java.awt.event.ActionListener() {
		// public void actionPerformed(java.awt.event.ActionEvent evt) {
		// jcbShowLabelsActionPerformed(evt);
		// }
		// });
		// toolBar.add(jcbShowLabels);
		//
		// jcbMDLabels.addItemListener(new java.awt.event.ItemListener() {
		// public void itemStateChanged(java.awt.event.ItemEvent evt) {
		// jcbMDLabelsItemStateChanged(evt);
		// }
		// });
		// toolBar.add(jcbMDLabels);
		//
		// jcbSortByLabel.setText(Labels.LABEL_SORT_BY_LABEL);
		// jcbSortByLabel.addActionListener(new java.awt.event.ActionListener()
		// {
		// public void actionPerformed(java.awt.event.ActionEvent evt) {
		// jcbSortByLabelActionPerformed(evt);
		// }
		// });
		// Set comboBox items.
		// String labels[] = tableModel.getLabels();
		// for (int i = 0; i < labels.length; i++) {
		// jcbMDLabels.addItem(labels[i]);
		// }
		// toolBar.add(jcbSortByLabel);

		jlGoto = new javax.swing.JLabel();
		jsGoToImage = new javax.swing.JSpinner();
		jlGoto.setIcon(Resources.getIcon(Resources.GOTO));
		jlGoto.setToolTipText(Labels.LABEL_GOTO_ITEM);
		toolBar.add(jlGoto);

		jsGoToImage.setValue(1);
		jsGoToImage.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				jsGoToImageStateChanged(evt);
			}
		});
		toolBar.add(jsGoToImage);

		toolBar.addSeparator();

		jcbAutoAdjustColumns = createToggleButton(Resources.ADJUST_COLS,
				Labels.MSG_ADJUST_COLS, new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jcbAutoAdjustColumnsStateChanged(evt);
					}
				});
		jcbAutoAdjustColumns.setSelected(true);
		jlRows = new javax.swing.JLabel();
		jsRows = new javax.swing.JSpinner();
		jlColumns = new javax.swing.JLabel();
		jsColumns = new javax.swing.JSpinner();
		toolBar.add(jcbAutoAdjustColumns);

		jlColumns.setText(Labels.LABEL_COLUMNS);
		toolBar.add(jlColumns);

		jsColumns.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				jsColumnsStateChanged(evt);
			}
		});
		toolBar.add(jsColumns);

		jlRows.setText(Labels.LABEL_ROWS);
		toolBar.add(jlRows);

		jsRows.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				jsRowsStateChanged(evt);
			}
		});
		toolBar.add(jsRows);

		// Some settings of the spinners
		jsRows.setModel(new SpinnerNumberModel(1, 1, gallery.getSize(), 1));
		jsColumns
				.setModel(new SpinnerNumberModel(1, 1, gallery.getSize(), 1));
		jsGoToImage.setModel(new SpinnerNumberModel(1, 1, gallery.getSize(),
				1));

		int TEXTWIDTH = 4;
		((JSpinner.NumberEditor) jsZoom.getEditor()).getTextField().setColumns(
				TEXTWIDTH);
		((JSpinner.NumberEditor) jsGoToImage.getEditor()).getTextField()
				.setColumns(TEXTWIDTH);
		((JSpinner.NumberEditor) jsRows.getEditor()).getTextField().setColumns(
				TEXTWIDTH);
		((JSpinner.NumberEditor) jsColumns.getEditor()).getTextField()
				.setColumns(TEXTWIDTH);

		// Sets limits for spinners.
//		jsRows.setModel(new SpinnerNumberModel(1, 1, gallery.getSize(), 1));
//		jsColumns
//				.setModel(new SpinnerNumberModel(1, 1, gallery.getSize(), 1));
//		jsGoToImage.setModel(new SpinnerNumberModel(1, 1, gallery.getSize(),
//				1));

		//jsRows.setValue(gallery.getRowCount());
		//jsColumns.setValue(gallery.getColumnCount());
	}

	private void jsZoomStateChanged(javax.swing.event.ChangeEvent evt) {// GEN-FIRST:event_jsZoomStateChanged
		gallery.setZoom((Integer) jsZoom.getValue());
//		if (!isUpdating) {
//			setZoom();
//			if (autoAdjustColumns)
//				autoAdjustColumns();
//			updateTable();
//		}
	}// GEN-LAST:event_jsZoomStateChanged

	// Handle user request to display item label
	private void jcbShowLabelsActionPerformed(java.awt.event.ActionEvent evt) {
		gallery.setShowLabels(jcbShowLabels.isSelected());
		//updateTable();
	}// GEN-LAST:event_jcbShowLabelsActionPerformed

	private void formWindowOpened(java.awt.event.WindowEvent evt) {// GEN-FIRST:event_formWindowOpened
		// pack();
		// ImagesWindowFactory.setConvenientSize(this);
		// setInitialValues();
	}// GEN-LAST:event_formWindowOpened

	private void jsRowsStateChanged(javax.swing.event.ChangeEvent evt) {// GEN-FIRST:event_jsRowsStateChanged
		if (!isUpdating) {
			gallery.setRows((Integer) jsRows.getValue());
		}
	}

	private void jsColumnsStateChanged(javax.swing.event.ChangeEvent evt) {// GEN-FIRST:event_jsColumnsStateChanged
		if (!isUpdating) {
			gallery.setColumns((Integer) jsColumns.getValue());
		}
	}

	private void jsGoToImageStateChanged(javax.swing.event.ChangeEvent evt) {// GEN-FIRST:event_jsGoToImageStateChanged
		if (!isUpdating)
			goToImage((Integer) jsGoToImage.getValue() - 1);
	}

	private void formComponentResized(java.awt.event.ComponentEvent evt) {// GEN-FIRST:event_formComponentResized
//		Dimension dim = evt.getComponent().getSize();
//		DEBUG.printMessage(dim.toString());
//		DEBUG.printMessage(evt.getComponent().getName());
		if (!isUpdating && autoAdjustColumns) 
				adjustColumns();
	}

	private void tableMouseClicked(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_tableMouseClicked
		final Point p = evt.getPoint();
		int view_row = table.rowAtPoint(p);
		int view_col = table.columnAtPoint(p);

		if (evt.getButton() == MouseEvent.BUTTON1) { // Left click.
			if (evt.getClickCount() > 1) {
				Object item = table.getValueAt(view_row, view_col);

				if (item instanceof AbstractGalleryImageItem) {
					ImagesWindowFactory.captureFrame(((AbstractGalleryImageItem) item).getImagePlus());
				}
			} else {
				// Ctrl adds items to selection, otherwise previous ones are
				// removed.
				if (!evt.isControlDown()) {
					gallery.clearSelection();
				}

				if (evt.isShiftDown()) {
					gallery.touchRange(previousSelectedRow,
							previousSelectedCol, view_row, view_col);
				} else {
					gallery.touchItem(view_row, view_col);
				}
				isUpdating = true;
				jsGoToImage.setValue(gallery.getIndex(view_row, view_col) + 1);
				isUpdating = false;
				//table.repaint();
			}

			if (!evt.isShiftDown()) {
				previousSelectedRow = view_row;
				previousSelectedCol = view_col;
			}
		} else if (evt.getButton() == MouseEvent.BUTTON3) { // Right click.
			if (gallery.getSelectedCount() < 2) {
				gallery.clearSelection();
				gallery.touchItem(view_row, view_col);

				table.setRowSelectionInterval(view_row, view_row);
				table.setColumnSelectionInterval(view_col, view_col);

				//table.repaint();
			}

			final MouseEvent me = evt;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					jpopUpMenuTable.show(me.getComponent(), p);
				}
			});
		}
	}

	private void jtbNormalizeActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jtbNormalizeActionPerformed
	// setNormalized(jtbNormalize.isSelected());
	// updateTable();
	}// GEN-LAST:event_jtbNormalizeActionPerformed

	private void jcbMDLabelsItemStateChanged(java.awt.event.ItemEvent evt) {// GEN-FIRST:event_jcbMDLabelsItemStateChanged
		if (evt.getStateChange() == ItemEvent.DESELECTED) {
			//gallery.setSelectedLabel(jcbMDLabels.getSelectedIndex());
			updateTable();
		}
	}// GEN-LAST:event_jcbMDLabelsItemStateChanged

	private void jcbSortByLabelActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jcbSortByLabelActionPerformed
		//gallery.setSorting(jcbSortByLabel.isSelected());
		updateTable();
	}// GEN-LAST:event_jcbSortByLabelActionPerformed

	private void jtbUseGeometryActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jtbUseGeometryActionPerformed
	// if (tableModel.containsGeometryInfo()) {
	// ((MDTableModel) tableModel).setUseGeometry(jtbUseGeometry.isSelected());
	// updateTable();
	// }
	}// GEN-LAST:event_jtbUseGeometryActionPerformed

	private void jcbAutoAdjustColumnsStateChanged(ActionEvent evt) {// GEN-FIRST:event_jcbAutoAdjustColumnsStateChanged
		setAutoAdjustColumns(jcbAutoAdjustColumns.isSelected());
		if (autoAdjustColumns) {
			adjustColumns();
		}
	}// GEN-LAST:event_jcbAutoAdjustColumnsStateChanged
		// Variables declaration - do not modify//GEN-BEGIN:variables

	class TableUpdater extends TimerTask {

		@Override
		public void run() {
			DEBUG.printMessage("Starting table update...");
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			updateTable();
			tableUpdaterTask = null;
		}
	}

	class JMenuBarTable extends JMenuBar implements ActionListener {
		protected JMenu jmFile = new JMenu(Labels.LABEL_GALLERY_FILE);
		protected JMenu jmSave = new JMenu(Labels.LABEL_GALLERY_SAVE);
		protected JMenu jmDisplay = new JMenu("Display");

		protected JMenuItem jmiSaveAsMetadata = new JMenuItem(
				Labels.LABEL_GALLERY_SAVE_AS_METADATA);
		protected JMenuItem jmiSaveAsStack = new JMenuItem(
				Labels.LABEL_GALLERY_SAVE_AS_IMAGE);
		protected JMenuItem jmiSaveSelectionAsMetadata = new JMenuItem(
				Labels.LABEL_GALLERY_SAVE_SELECTION_AS_METADATA);
		protected JMenuItem jmiSaveSelectionAsStack = new JMenuItem(
				Labels.LABEL_GALLERY_SAVE_SELECTION_AS_IMAGE);
		protected JMenuItem jmiExit = new JMenuItem(Labels.LABEL_GALLERY_EXIT);
		protected JMenu jmStatistics = new JMenu(Labels.MENU_STATS);
		protected JMenuItem jmiAVG = new JMenuItem(Labels.BUTTON_MEAN);
		protected JMenuItem jmiSTDEV = new JMenuItem(
				Labels.BUTTON_STD_DEVIATION);
		protected JMenuItem jmiPCA = new JMenuItem(Labels.BUTTON_PCA);
		protected JMenuItem jmiFSC = new JMenuItem(Labels.BUTTON_FSC);
		protected JMenu jmOpenWith = new JMenu(Labels.MENU_OPEN_WITH);
		protected JMenuItem jmiOpenWithChimera = new JMenuItem(Labels.MENU_OPEN_WITH_CHIMERA, 
				Resources.getIcon("chimera.gif"));
		protected JMenuItem jmiOpenWithImageJ = new JMenuItem(Labels.MENU_OPEN_WITH_IJ,
				Resources.getIcon("ij.gif"));
//		protected JMenu jmReslice = new JMenu(Labels.LABEL_RESLICE);
//		protected JMenuItem jmiResliceTop = new JMenuItem(
//				Labels.LABEL_RESLICE_TOP);
//		protected JMenuItem jmiResliceRight = new JMenuItem(
//				Labels.LABEL_RESLICE_RIGHT);

		protected JCheckBoxMenuItem jmiNormalize = new JCheckBoxMenuItem(
				Labels.MSG_NORMALIZE);
		protected JCheckBoxMenuItem jmiApplyGeo = new JCheckBoxMenuItem(
				Labels.MSG_APPLY_GEO);
		protected JMenuItem jmiColumns = new JMenuItem("Columns ...", Resources.getIcon("columns.gif"));
		protected JCheckBoxMenuItem jmiShowLabel = new JCheckBoxMenuItem(
				Labels.MSG_SHOW_LABEL);
		protected JMenu jmReslice = new JMenu(Labels.LABEL_RESLICE);
		protected JRadioButtonMenuItem jmiAxisX = new JRadioButtonMenuItem("X");
		protected JRadioButtonMenuItem jmiAxisY = new JRadioButtonMenuItem("Y");
		protected JRadioButtonMenuItem jmiAxisZ = new JRadioButtonMenuItem("Z");

		public void enableNormalize(boolean value) {
			jmiNormalize.setEnabled(value);
		}

		public void selectNormalize(boolean value) {
			jmiNormalize.setSelected(value);
		}

		public void enableApplyGeo(boolean value) {
			jmiApplyGeo.setEnabled(value);
		}

		public void selectApplyGeo(boolean value) {
			jmiApplyGeo.setSelected(value);
		}
		
		public void enableShowLabel(boolean value){
			jmiShowLabel.setEnabled(value);
		}
		
		public void enableReslice(boolean value){
			jmReslice.setEnabled(value);
		}

		public void actionPerformed(ActionEvent e) {
			JMenuItem jmi = (JMenuItem) e.getSource();
			if (jmi == jmiNormalize){
				gallery.setNormalized(jmiNormalize.isSelected());
			}
//			else if (jmi == jmiApplyGeo) {
//				if (gallery.containsGeometryInfo()) {
//					((MDTableModel) gallery).setUseGeometry(jmiApplyGeo
//							.isSelected());
//					updateTable();
//				}
//			}
			else if (jmi == jmiShowLabel){
				gallery.setShowLabels(jmiShowLabel.isSelected());
			}
		}

		public JMenuBarTable() {
			super();

			// File menu
			jmSave.add(jmiSaveAsMetadata);
			jmSave.add(jmiSaveAsStack);
			jmSave.addSeparator();
			jmSave.add(jmiSaveSelectionAsMetadata);
			jmSave.add(jmiSaveSelectionAsStack);

			add(jmFile);
			jmFile.add(jmSave);
			jmFile.addSeparator();
			jmFile.add(jmiExit);

			jmiSaveAsMetadata.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					saveAsMetadata(true);
				}
			});

			jmiSaveSelectionAsMetadata.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					saveAsMetadata(false);
				}
			});

			jmiSaveAsStack.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					saveAsStack(true);
				}
			});

			jmiSaveSelectionAsStack.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					saveAsStack(false);
				}
			});

			jmiExit.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent ae) {
					System.exit(0);
				}
			});

			// Display menu
			add(jmDisplay);
			jmDisplay.add(jmiNormalize);
			jmDisplay.add(jmiShowLabel);
			jmDisplay.addSeparator();
			jmDisplay.add(jmiApplyGeo);
			jmDisplay.add(jmiColumns);
			jmDisplay.addSeparator();
			jmDisplay.add(jmReslice);
			jmReslice.add(jmiAxisX);
			jmReslice.add(jmiAxisY);
			jmReslice.add(jmiAxisZ);
			ButtonGroup group = new ButtonGroup();
			group.add(jmiAxisX);
			group.add(jmiAxisY);
			group.add(jmiAxisZ);			
			
			jmiNormalize.addActionListener(this);
			jmiApplyGeo.addActionListener(this);
			jmiShowLabel.addActionListener(this);

			// Statistics menu
			add(jmStatistics);
			jmStatistics.add(jmiAVG);
			jmStatistics.add(jmiSTDEV);
			jmStatistics.add(jmiPCA);
			jmStatistics.add(jmiFSC);

			jmiAVG.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					avgImage();
				}
			});

			jmiSTDEV.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					stdDevImage();
				}
			});

			jmiPCA.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					pca();
				}
			});

			jmiFSC.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					fsc();
				}
			});

			// Open with menu
			add(jmOpenWith);
			jmOpenWith.add(jmiOpenWithChimera);
			jmOpenWith.add(jmiOpenWithImageJ);
			//jmOpenWith.add(jmiOpenAsStack);

//			jmiOpenWithChimera.addActionListener(new ActionListener() {
//
//				public void actionPerformed(ActionEvent e) {
//					try {
//						Process p = new ProcessBuilder("chimera", "spider:"+gallery.getFilename()).start();
//					} catch (IOException e1) {
//						// TODO Auto-generated catch block
//						e1.printStackTrace();
//					}
//				}
//			});
//
//			jmiOpenAsMetadata.addActionListener(new ActionListener() {
//
//				public void actionPerformed(ActionEvent e) {
//					ImagesWindowFactory.openFileAsMetadata(tableModel
//							.getFilename());
//				}
//			});
//
//			jmiOpenAsStack.addActionListener(new ActionListener() {
//
//				public void actionPerformed(ActionEvent e) {
//					openAsStack();
//				}
//			});

//			// Reslice menu
//			add(jmReslice);
//			jmReslice.add(jmiResliceTop);
//			jmReslice.add(jmiResliceRight);
//
//			jmiResliceTop.addActionListener(new ActionListener() {
//
//				public void actionPerformed(ActionEvent e) {
//					try {
//						reslice(RESLICE_MODE.TOP_Y);
//					} catch (Exception ex) {
//						DEBUG.printException(ex);
//						IJ.error(ex.getMessage());
//					}
//				}
//			});
//
//			jmiResliceRight.addActionListener(new ActionListener() {
//
//				public void actionPerformed(ActionEvent e) {
//					try {
//						reslice(RESLICE_MODE.RIGHT_X);
//					} catch (Exception ex) {
//						DEBUG.printException(ex);
//						IJ.error(ex.getMessage());
//					}
//				}
//			});

			boolean isVolume = false;//gallery.isVolume();
			// boolean isStack = tableModel.isStack();
			boolean isMetaData = true;//gallery.isMetaData();

			jmStatistics.setEnabled(isMetaData);
			jmReslice.setEnabled(isVolume);
			//jmTopAxis.setEnabled(isVolume);
			jmiOpenWithChimera.setEnabled(isVolume);
			//jmiOpenAs3D.setEnabled(!isMetaData);
			//jmiOpenAsMetadata.setEnabled(!isVolume);

			// Volumes can't be saved as metadata.
			jmiSaveAsMetadata.setEnabled(!isVolume);
			jmiSaveSelectionAsMetadata.setEnabled(!isVolume);
		}
	}

	class JPopUpMenuGallery extends JPopupMenu {

		protected Point location;
		protected JMenuItem jmiEnable = new JMenuItem(
				Labels.LABEL_GALLERY_ENABLE);
		protected JMenuItem jmiDisable = new JMenuItem(
				Labels.LABEL_GALLERY_DISABLE);
		protected JMenuItem jmiEnableAll = new JMenuItem(
				Labels.LABEL_GALLERY_ENABLE_ALL);
		protected JMenuItem jmiDisableAll = new JMenuItem(
				Labels.LABEL_GALLERY_DISABLE_ALL);
		protected JMenuItem jmiEnableFrom = new JMenuItem(
				Labels.LABEL_GALLERY_ENABLE_FROM);
		protected JMenuItem jmiEnableTo = new JMenuItem(
				Labels.LABEL_GALLERY_ENABLE_TO);
		protected JMenuItem jmiDisableFrom = new JMenuItem(
				Labels.LABEL_GALLERY_DISABLE_FROM);
		protected JMenuItem jmiDisableTo = new JMenuItem(
				Labels.LABEL_GALLERY_DISABLE_TO);

		public JPopUpMenuGallery() {
			add(jmiEnable);
			add(jmiDisable);
			addSeparator();
			add(jmiEnableAll);
			add(jmiDisableAll);
			addSeparator();
			add(jmiEnableFrom);
			add(jmiEnableTo);
			addSeparator();
			add(jmiDisableFrom);
			add(jmiDisableTo);

			jmiEnable.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,
					InputEvent.CTRL_DOWN_MASK));
			jmiDisable.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D,
					InputEvent.CTRL_DOWN_MASK));
			jmiEnableAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,
					InputEvent.CTRL_DOWN_MASK));
			jmiDisableAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
					InputEvent.CTRL_DOWN_MASK));

			// jmiEnableFrom.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F,
			// InputEvent.SHIFT_DOWN_MASK));
			// jmiDisableFrom.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F,
			// InputEvent.CTRL_DOWN_MASK));
			// jmiEnableTo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T,
			// InputEvent.SHIFT_DOWN_MASK));
			// jmiDisableTo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T,
			// InputEvent.CTRL_DOWN_MASK));

			table.addKeyListener(new KeyListener() {

				public void keyTyped(KeyEvent e) {
				}

				public void keyPressed(KeyEvent e) {
				}

				public void keyReleased(KeyEvent e) {
//					if (e.isControlDown()) {
//						if (e.getKeyCode() == KeyEvent.VK_E) {
//							gallery.enableSelectedItems();
//						}
//						if (e.getKeyCode() == KeyEvent.VK_D) {
//							gallery.disableSelectedItems();
//						}
//						if (e.getKeyCode() == KeyEvent.VK_A) {
//							gallery.enableAllItems();
//						}
//						if (e.getKeyCode() == KeyEvent.VK_N) {
//							gallery.disableAllItems();
//						}
//					}
				}
			});

//			jmiEnable.addActionListener(new ActionListener() {
//
//				public void actionPerformed(ActionEvent e) {
//					gallery.enableSelectedItems();
//				}
//			});
//
//			jmiDisable.addActionListener(new ActionListener() {
//
//				public void actionPerformed(ActionEvent e) {
//					gallery.disableSelectedItems();
//				}
//			});
//
//			jmiEnableAll.addActionListener(new ActionListener() {
//
//				public void actionPerformed(ActionEvent e) {
//					gallery.enableAllItems();
//				}
//			});
//
//			jmiDisableAll.addActionListener(new ActionListener() {
//
//				public void actionPerformed(ActionEvent e) {
//					gallery.disableAllItems();
//				}
//			});
//
//			jmiEnableFrom.addActionListener(new ActionListener() {
//
//				public void actionPerformed(ActionEvent e) {
//					int row = table.rowAtPoint(location);
//					int col = table.columnAtPoint(location);
//
//					gallery.setEnabledFrom(row, col, true);
//				}
//			});
//
//			jmiEnableTo.addActionListener(new ActionListener() {
//
//				public void actionPerformed(ActionEvent e) {
//					int row = table.rowAtPoint(location);
//					int col = table.columnAtPoint(location);
//
//					gallery.setEnabledTo(row, col, true);
//				}
//			});
//
//			jmiDisableFrom.addActionListener(new ActionListener() {
//
//				public void actionPerformed(ActionEvent e) {
//					int row = table.rowAtPoint(location);
//					int col = table.columnAtPoint(location);
//
//					gallery.setEnabledFrom(row, col, false);
//				}
//			});
//
//			jmiDisableTo.addActionListener(new ActionListener() {
//
//				public void actionPerformed(ActionEvent e) {
//					int row = table.rowAtPoint(location);
//					int col = table.columnAtPoint(location);
//
//					gallery.setEnabledTo(row, col, false);
//				}
//			});
		}

		public void show(Component cmpnt, Point location) {
			this.location = location;

			// Update menu items status depending on item.
			int row = table.rowAtPoint(location);
			int col = table.columnAtPoint(location);
			boolean enabled = ((AbstractGalleryImageItem) table.getValueAt(row,
					col)).isEnabled();

			jmiDisable.setEnabled(enabled);
			jmiEnable.setEnabled(!enabled);

			show(cmpnt, location.x, location.y);
		}
	}

}
