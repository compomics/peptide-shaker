package eu.isas.peptideshaker.gui.pride;

import com.compomics.util.Util;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.gui.JOptionEditorPane;
import com.compomics.util.gui.TableProperties;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.preferences.UtilitiesUserPreferences;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import no.uib.jsparklines.extra.HtmlLinksRenderer;
import no.uib.jsparklines.extra.NimbusCheckBoxRenderer;

/**
 * Dialog for setting up the PRIDE Reshake.
 *
 * @author Harald Barsnes
 */
public class PrideReshakeSetupDialog extends javax.swing.JDialog {

    /**
     * The PrideReShakeGUI parent.
     */
    private PrideReShakeGUIv2 prideReShakeGUI;
    /**
     * The files table column header tooltips.
     */
    private ArrayList<String> filesTableToolTips;
    /**
     * A simple progress dialog.
     */
    private static ProgressDialogX progressDialog;
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * True of a file is currently being downloaded.
     */
    private boolean isFileBeingDownloaded = false;

    /**
     * Creates a new PrideReshakeSetupDialog.
     *
     * @param prideReShakeGUI
     * @param modal
     */
    public PrideReshakeSetupDialog(PrideReShakeGUIv2 prideReShakeGUI, boolean modal) {
        super(prideReShakeGUI, modal);
        initComponents();
        this.prideReShakeGUI = prideReShakeGUI;
        setUpGUI();
        validateInput(false);
        setLocationRelativeTo(prideReShakeGUI);
        setVisible(true);
    }

    /**
     * Set up the GUI.
     */
    private void setUpGUI() {
        // make sure that the scroll panes are see-through
        spectrumTableScrollPane.getViewport().setOpaque(false);
        searchSettingsScrollPane.getViewport().setOpaque(false);

        spectrumTable.getTableHeader().setReorderingAllowed(false);
        searchSettingsTable.getTableHeader().setReorderingAllowed(false);

        spectrumTable.setAutoCreateRowSorter(true);
        searchSettingsTable.setAutoCreateRowSorter(true);

        filesTableToolTips = new ArrayList<String>();
        filesTableToolTips.add(null);
        filesTableToolTips.add("Assay Accession Numbers");
        filesTableToolTips.add("File Type");
        filesTableToolTips.add("File");
        filesTableToolTips.add("Download File");
        filesTableToolTips.add("File Size");
        filesTableToolTips.add("Selected");

        spectrumTable.getColumn("Assay").setMaxWidth(90);
        spectrumTable.getColumn("Assay").setMinWidth(90);
        spectrumTable.getColumn(" ").setMaxWidth(50);
        spectrumTable.getColumn(" ").setMinWidth(50);
        spectrumTable.getColumn("  ").setMaxWidth(30);
        spectrumTable.getColumn("  ").setMinWidth(30);
        spectrumTable.getColumn("Type").setMaxWidth(90);
        spectrumTable.getColumn("Type").setMinWidth(90);
        spectrumTable.getColumn("Download").setMaxWidth(90);
        spectrumTable.getColumn("Download").setMinWidth(90);
        spectrumTable.getColumn("Size").setMaxWidth(90);
        spectrumTable.getColumn("Size").setMinWidth(90);

        searchSettingsTable.getColumn("Assay").setMaxWidth(90);
        searchSettingsTable.getColumn("Assay").setMinWidth(90);
        searchSettingsTable.getColumn(" ").setMaxWidth(50);
        searchSettingsTable.getColumn(" ").setMinWidth(50);
        searchSettingsTable.getColumn("  ").setMaxWidth(30);
        searchSettingsTable.getColumn("  ").setMinWidth(30);
        searchSettingsTable.getColumn("Type").setMaxWidth(90);
        searchSettingsTable.getColumn("Type").setMinWidth(90);
        searchSettingsTable.getColumn("Download").setMaxWidth(90);
        searchSettingsTable.getColumn("Download").setMinWidth(90);
        searchSettingsTable.getColumn("Size").setMaxWidth(90);
        searchSettingsTable.getColumn("Size").setMinWidth(90);

        spectrumTable.getColumn("Assay").setCellRenderer(new HtmlLinksRenderer(TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));
        searchSettingsTable.getColumn("Assay").setCellRenderer(new HtmlLinksRenderer(TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));
        spectrumTable.getColumn("Download").setCellRenderer(new HtmlLinksRenderer(TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));
        searchSettingsTable.getColumn("Download").setCellRenderer(new HtmlLinksRenderer(TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));

        spectrumTable.getColumn("  ").setCellRenderer(new NimbusCheckBoxRenderer());
        searchSettingsTable.getColumn("  ").setCellRenderer(new NimbusCheckBoxRenderer());

        // correct the color for the upper right corner
        JPanel spectrumCorner = new JPanel();
        spectrumCorner.setBackground(spectrumTable.getTableHeader().getBackground());
        spectrumTableScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, spectrumCorner);
        JPanel settingsCorner = new JPanel();
        settingsCorner.setBackground(searchSettingsTable.getTableHeader().getBackground());
        searchSettingsScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, settingsCorner);

        // set the database
        speciesJTextField.setText(prideReShakeGUI.getCurrentDatabase());

        if (prideReShakeGUI.getCurrentDatabase() == null
                || prideReShakeGUI.getCurrentDatabase().trim().isEmpty()
                || prideReShakeGUI.getCurrentDatabase().indexOf(",") != -1) {
            downloadUniProtJLabel.setEnabled(false);
        }

        updateTables();
    }

    /**
     * Update the tables.
     */
    private void updateTables() {

        TableModel filesTableModel = prideReShakeGUI.getFilesTable().getRowSorter().getModel();

        for (int i = 0; i < filesTableModel.getRowCount(); i++) {
            ((DefaultTableModel) spectrumTable.getModel()).addRow(new Object[]{
                spectrumTable.getRowCount() + 1,
                filesTableModel.getValueAt(i, 1),
                filesTableModel.getValueAt(i, 2),
                filesTableModel.getValueAt(i, 3),
                filesTableModel.getValueAt(i, 4),
                filesTableModel.getValueAt(i, 5),
                false
            });

            ((DefaultTableModel) searchSettingsTable.getModel()).addRow(new Object[]{
                searchSettingsTable.getRowCount() + 1,
                filesTableModel.getValueAt(i, 1),
                filesTableModel.getValueAt(i, 2),
                filesTableModel.getValueAt(i, 3),
                filesTableModel.getValueAt(i, 4),
                filesTableModel.getValueAt(i, 5),
                false
            });
        }

        // filter the specturm table
        List<RowFilter<Object, Object>> spectrumFileFilters = new ArrayList<RowFilter<Object, Object>>();

        // reshakeble filter
        RowFilter<Object, Object> reshakeableFilter = new RowFilter<Object, Object>() {
            public boolean include(RowFilter.Entry<? extends Object, ? extends Object> entry) {
                return isFileReshakeable((String) entry.getValue(spectrumTable.getColumn("File").getModelIndex()),
                        (String) entry.getValue(spectrumTable.getColumn("Type").getModelIndex()));
            }
        };

        spectrumFileFilters.add(reshakeableFilter);

        RowFilter<Object, Object> allSpectrumFileFilters = RowFilter.andFilter(spectrumFileFilters);

        if (spectrumTable.getRowSorter() != null) {
            ((TableRowSorter) spectrumTable.getRowSorter()).setRowFilter(allSpectrumFileFilters);
        }

        // fix the index column
        for (int i = 0; i < spectrumTable.getRowCount(); i++) {
            spectrumTable.setValueAt(i + 1, i, 0);
        }

        // filter the search settings table
        List<RowFilter<Object, Object>> searchSettingsFilesFilters = new ArrayList<RowFilter<Object, Object>>();

        // reshakeble filter
        RowFilter<Object, Object> searchSettingsFilter = new RowFilter<Object, Object>() {
            public boolean include(RowFilter.Entry<? extends Object, ? extends Object> entry) {
                return isFileSearchSettingsExtractable((String) entry.getValue(searchSettingsTable.getColumn("File").getModelIndex()),
                        (String) entry.getValue(searchSettingsTable.getColumn("Type").getModelIndex()));
            }
        };

        searchSettingsFilesFilters.add(searchSettingsFilter);

        RowFilter<Object, Object> allSearchSettingsFilesFilters = RowFilter.andFilter(searchSettingsFilesFilters);

        if (searchSettingsTable.getRowSorter() != null) {
            ((TableRowSorter) searchSettingsTable.getRowSorter()).setRowFilter(allSearchSettingsFilesFilters);
        }

        // fix the index column
        for (int i = 0; i < searchSettingsTable.getRowCount(); i++) {
            searchSettingsTable.setValueAt(i + 1, i, 0);
        }
    }

    /**
     * Returns true if the file is reshakeable.
     *
     * @param fileName the file to check
     * @param fileType the file type
     * @return true if the file is reshakeable
     */
    private boolean isFileReshakeable(String fileName, String fileType) {

        boolean reshakeable = false;

        // check if the file is reshakeable
        if (prideReShakeGUI.getReshakeableFiles().containsKey(fileType)) {
            for (String fileEnding : prideReShakeGUI.getReshakeableFiles().get(fileType)) {
                if (fileName.toLowerCase().endsWith(fileEnding)) {
                    reshakeable = true;
                    break;
                }
            }
        }

        return reshakeable;
    }

    /**
     * Returns true if search settings can be extracted from the file.
     *
     * @param fileName the file to check
     * @param fileType the file type
     * @return true if search settings can be extracted from the file
     */
    private boolean isFileSearchSettingsExtractable(String fileName, String fileType) {

        boolean containsSearchSettings = false;

        // check if the file contains search settings
        if (prideReShakeGUI.getSearchSettingsFiles().containsKey(fileType)) {
            for (String fileEnding : prideReShakeGUI.getSearchSettingsFiles().get(fileType)) {
                if (fileName.toLowerCase().endsWith(fileEnding)) {
                    containsSearchSettings = true;
                    break;
                }
            }
        }

        return containsSearchSettings;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        backgroundPanel = new javax.swing.JPanel();
        spectrumPanel = new javax.swing.JPanel();
        spectrumLabel = new javax.swing.JLabel();
        spectrumTableScrollPane = new javax.swing.JScrollPane();
        spectrumTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        return (String) filesTableToolTips.get(realIndex);
                    }
                };
            }
        };
        selectAllLabel = new javax.swing.JLabel();
        dataTypeSeparatorLabel = new javax.swing.JLabel();
        deselectAllLabel = new javax.swing.JLabel();
        searchSettingsPanel = new javax.swing.JPanel();
        searchSettingsLabel = new javax.swing.JLabel();
        searchSettingsScrollPane = new javax.swing.JScrollPane();
        searchSettingsTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        return (String) filesTableToolTips.get(realIndex);
                    }
                };
            }
        };
        ;
        databasePanel = new javax.swing.JPanel();
        speciesLabel = new javax.swing.JLabel();
        speciesJTextField = new javax.swing.JTextField();
        downloadUniProtJLabel = new javax.swing.JLabel();
        databaseSettingsLbl = new javax.swing.JLabel();
        databaseSettingsTxt = new javax.swing.JTextField();
        browseDatabaseSettingsButton = new javax.swing.JButton();
        targetDecoySettingsButton = new javax.swing.JButton();
        workingFolderPanel = new javax.swing.JPanel();
        workingFolderLbl = new javax.swing.JLabel();
        workingFolderTxt = new javax.swing.JTextField();
        browseWorkingFolderButton = new javax.swing.JButton();
        reshakeButton = new javax.swing.JButton();
        aboutButton = new javax.swing.JButton();
        peptideShakerHomePageLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Reshake Settings");
        setMinimumSize(new java.awt.Dimension(780, 700));

        backgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        spectrumPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING +  "1) Spectrum Files"));
        spectrumPanel.setOpaque(false);

        spectrumLabel.setFont(spectrumLabel.getFont().deriveFont((spectrumLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        spectrumLabel.setText("Select the spectrum files to reanalyze. Supported formats: mgf and PRIDE XML.");

        spectrumTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Assay", "Type", "File", "Download", "Size", "  "
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Long.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        spectrumTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                spectrumTableMouseReleased(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                spectrumTableMouseExited(evt);
            }
        });
        spectrumTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                spectrumTableMouseMoved(evt);
            }
        });
        spectrumTableScrollPane.setViewportView(spectrumTable);

        selectAllLabel.setText("<html><a href=\"dummy\">Select All</a></html>  ");
        selectAllLabel.setToolTipText("Open the PeptideShaker web page");
        selectAllLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                selectAllLabelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                selectAllLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                selectAllLabelMouseExited(evt);
            }
        });

        dataTypeSeparatorLabel.setText("/");

        deselectAllLabel.setText("<html><a href=\"dummy\">Deselect All</a></html>\n\n");
        deselectAllLabel.setToolTipText("Open the PeptideShaker web page");
        deselectAllLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                deselectAllLabelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                deselectAllLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                deselectAllLabelMouseExited(evt);
            }
        });

        javax.swing.GroupLayout spectrumPanelLayout = new javax.swing.GroupLayout(spectrumPanel);
        spectrumPanel.setLayout(spectrumPanelLayout);
        spectrumPanelLayout.setHorizontalGroup(
            spectrumPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumPanelLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(spectrumLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(selectAllLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(dataTypeSeparatorLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(deselectAllLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(23, 23, 23))
            .addGroup(spectrumPanelLayout.createSequentialGroup()
                .addComponent(spectrumTableScrollPane)
                .addContainerGap())
        );
        spectrumPanelLayout.setVerticalGroup(
            spectrumPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 174, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(spectrumPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spectrumLabel)
                    .addGroup(spectrumPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(deselectAllLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(selectAllLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(dataTypeSeparatorLabel)))
                .addContainerGap())
        );

        searchSettingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING +  "2) Search Settings"));
        searchSettingsPanel.setOpaque(false);

        searchSettingsLabel.setFont(searchSettingsLabel.getFont().deriveFont((searchSettingsLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        searchSettingsLabel.setText("Select the file to extract the search parameters from. Supported formats: mzIdentML and PRIDE XML.");

        searchSettingsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Assay", "Type", "File", "Download", "Size", "  "
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Long.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        searchSettingsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                searchSettingsTableMouseReleased(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                searchSettingsTableMouseExited(evt);
            }
        });
        searchSettingsTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                searchSettingsTableMouseMoved(evt);
            }
        });
        searchSettingsScrollPane.setViewportView(searchSettingsTable);

        javax.swing.GroupLayout searchSettingsPanelLayout = new javax.swing.GroupLayout(searchSettingsPanel);
        searchSettingsPanel.setLayout(searchSettingsPanelLayout);
        searchSettingsPanelLayout.setHorizontalGroup(
            searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(searchSettingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(searchSettingsScrollPane)
                    .addGroup(searchSettingsPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(searchSettingsLabel)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        searchSettingsPanelLayout.setVerticalGroup(
            searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, searchSettingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(searchSettingsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 174, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(searchSettingsLabel)
                .addContainerGap())
        );

        databasePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING +  "3) Sequence Database"));
        databasePanel.setOpaque(false);

        speciesLabel.setText("Species");

        speciesJTextField.setEditable(false);
        speciesJTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        downloadUniProtJLabel.setForeground(new java.awt.Color(0, 0, 255));
        downloadUniProtJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        downloadUniProtJLabel.setText("<html><u>Download from UniProt</u></html>");
        downloadUniProtJLabel.setToolTipText("Download UniProt Database");
        downloadUniProtJLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                downloadUniProtJLabelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                downloadUniProtJLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                downloadUniProtJLabelMouseExited(evt);
            }
        });

        databaseSettingsLbl.setText("Database");

        databaseSettingsTxt.setEditable(false);

        browseDatabaseSettingsButton.setText("Browse");
        browseDatabaseSettingsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseDatabaseSettingsButtonActionPerformed(evt);
            }
        });

        targetDecoySettingsButton.setText("Decoy");
        targetDecoySettingsButton.setToolTipText("Generate a concatenated Target/Decoy database");
        targetDecoySettingsButton.setEnabled(false);
        targetDecoySettingsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                targetDecoySettingsButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout databasePanelLayout = new javax.swing.GroupLayout(databasePanel);
        databasePanel.setLayout(databasePanelLayout);
        databasePanelLayout.setHorizontalGroup(
            databasePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(databasePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(databasePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(databasePanelLayout.createSequentialGroup()
                        .addComponent(speciesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(speciesJTextField))
                    .addGroup(databasePanelLayout.createSequentialGroup()
                        .addComponent(databaseSettingsLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(databaseSettingsTxt)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(databasePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(databasePanelLayout.createSequentialGroup()
                        .addComponent(browseDatabaseSettingsButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(targetDecoySettingsButton, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(downloadUniProtJLabel))
                .addContainerGap())
        );

        databasePanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {browseDatabaseSettingsButton, targetDecoySettingsButton});

        databasePanelLayout.setVerticalGroup(
            databasePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, databasePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(databasePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(speciesLabel)
                    .addComponent(speciesJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(downloadUniProtJLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(databasePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(databaseSettingsLbl)
                    .addComponent(browseDatabaseSettingsButton)
                    .addComponent(databaseSettingsTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(targetDecoySettingsButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        workingFolderPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING +  "4) Working Folder"));
        workingFolderPanel.setOpaque(false);

        workingFolderLbl.setText("Folder");

        workingFolderTxt.setEditable(false);

        browseWorkingFolderButton.setText("Browse");
        browseWorkingFolderButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseWorkingFolderButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout workingFolderPanelLayout = new javax.swing.GroupLayout(workingFolderPanel);
        workingFolderPanel.setLayout(workingFolderPanelLayout);
        workingFolderPanelLayout.setHorizontalGroup(
            workingFolderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(workingFolderPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(workingFolderLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(workingFolderTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 569, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(browseWorkingFolderButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(92, 92, 92))
        );
        workingFolderPanelLayout.setVerticalGroup(
            workingFolderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(workingFolderPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(workingFolderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(workingFolderLbl)
                    .addComponent(browseWorkingFolderButton)
                    .addComponent(workingFolderTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        reshakeButton.setBackground(new java.awt.Color(0, 153, 0));
        reshakeButton.setFont(reshakeButton.getFont().deriveFont(reshakeButton.getFont().getStyle() | java.awt.Font.BOLD));
        reshakeButton.setForeground(new java.awt.Color(255, 255, 255));
        reshakeButton.setText("Start the Reshaking!");
        reshakeButton.setEnabled(false);
        reshakeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reshakeButtonActionPerformed(evt);
            }
        });

        aboutButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/peptide-shaker-medium-orange-shadow.png"))); // NOI18N
        aboutButton.setToolTipText("Open the PeptideShaker web page");
        aboutButton.setBorder(null);
        aboutButton.setBorderPainted(false);
        aboutButton.setContentAreaFilled(false);
        aboutButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                aboutButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                aboutButtonMouseExited(evt);
            }
        });
        aboutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutButtonActionPerformed(evt);
            }
        });

        peptideShakerHomePageLabel.setFont(peptideShakerHomePageLabel.getFont().deriveFont((peptideShakerHomePageLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        peptideShakerHomePageLabel.setText("Select the files to reanalyze, provide the search settings and click Start the Reshaking!");
        peptideShakerHomePageLabel.setToolTipText("Open the PeptideShaker web page");

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(databasePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(workingFolderPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(spectrumPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(searchSettingsPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(aboutButton)
                        .addGap(44, 44, 44)
                        .addComponent(peptideShakerHomePageLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(reshakeButton)))
                .addContainerGap())
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(searchSettingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(databasePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(workingFolderPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(aboutButton)
                    .addComponent(peptideShakerHomePageLabel)
                    .addComponent(reshakeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Start downloading and converting the files.
     *
     * @param evt
     */
    private void reshakeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reshakeButtonActionPerformed

        progressDialog = new ProgressDialogX(prideReShakeGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);

        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
        progressDialog.setTitle("Checking Files. Please Wait...");
        isFileBeingDownloaded = true;

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        new Thread("FileExistsThread") {
            @Override
            public void run() {

                ArrayList<String> selectedSpectrumFiles = new ArrayList<String>();
                String selectedSearchSettingsFile = null;

                for (int i = 0; i < spectrumTable.getRowCount(); i++) {
                    if ((Boolean) spectrumTable.getValueAt(i, spectrumTable.getColumn("  ").getModelIndex())) {

                        String link = (String) spectrumTable.getValueAt(i, spectrumTable.getColumn("Download").getModelIndex());
                        link = link.substring(link.indexOf("\"") + 1);
                        link = link.substring(0, link.indexOf("\""));

                        boolean exists = prideReShakeGUI.checkIfURLExists(link);

                        if (!exists) {
                            if (link.endsWith(".gz")) {
                                link = link.substring(0, link.length() - 3);
                                exists = prideReShakeGUI.checkIfURLExists(link);
                            }
                        }

                        if (exists) {
                            selectedSpectrumFiles.add(link);
                        } else {
                            JOptionPane.showMessageDialog(PrideReshakeSetupDialog.this, JOptionEditorPane.getJOptionEditorPane(
                                    "PRIDE web service access error. Cannot open:<br>"
                                    + link + "<br>"
                                    + "Please contact the <a href=\"http://www.ebi.ac.uk/pride/ws/archive/\">PRIDE web service developers</a>."),
                                    "PRIDE Access Error", JOptionPane.WARNING_MESSAGE);
                            System.out.println("Not found: " + link + "!");
                        }
                    }
                }

                for (int i = 0; i < searchSettingsTable.getRowCount(); i++) {
                    if ((Boolean) searchSettingsTable.getValueAt(i, searchSettingsTable.getColumn("  ").getModelIndex())) {

                        String link = (String) searchSettingsTable.getValueAt(i, searchSettingsTable.getColumn("Download").getModelIndex());
                        link = link.substring(link.indexOf("\"") + 1);
                        link = link.substring(0, link.indexOf("\""));

                        if (!selectedSpectrumFiles.contains(link)) {

                            boolean exists = prideReShakeGUI.checkIfURLExists(link);

                            if (!exists) {
                                if (link.endsWith(".gz")) {
                                    link = link.substring(0, link.length() - 3);
                                    if (!selectedSpectrumFiles.contains(link)) {
                                        exists = prideReShakeGUI.checkIfURLExists(link);
                                    } else {
                                        exists = true;
                                    }
                                }
                            }

                            if (exists) {
                                selectedSearchSettingsFile = link;
                            } else {
                                JOptionPane.showMessageDialog(PrideReshakeSetupDialog.this, JOptionEditorPane.getJOptionEditorPane(
                                        "PRIDE web service access error. Cannot open:<br>"
                                        + link + "<br>"
                                        + "Please contact the <a href=\"http://www.ebi.ac.uk/pride/ws/archive/\">PRIDE web service developers</a>."),
                                        "PRIDE Access Error", JOptionPane.WARNING_MESSAGE);
                                System.out.println("Not found: " + link + "!");
                            }
                        }
                    }
                }

                boolean download = true;

                if (selectedSpectrumFiles.isEmpty()) {
                    download = false;
                }

                progressDialog.setRunFinished();

                if (download) {

                    String selectedSpectrumFilesAsText = "";

                    for (String file : selectedSpectrumFiles) {
                        selectedSpectrumFilesAsText += new File(file).getName() + "\n";
                    }

                    String selectedSearchSettingsFileAsText;
                    if (selectedSearchSettingsFile != null) {
                        selectedSearchSettingsFileAsText = new File(selectedSearchSettingsFile).getName();
                    } else {
                        selectedSearchSettingsFileAsText = "(default search settings)";
                    }

                    JOptionPane.showMessageDialog(PrideReshakeSetupDialog.this, "Will reshake:\n"
                            + selectedSpectrumFilesAsText + "\n"
                            + "using search settings from:\n"
                            + selectedSearchSettingsFileAsText + ".",
                            "Reshake Settings", JOptionPane.INFORMATION_MESSAGE);

                    JOptionPane.showMessageDialog(null, "Reshake not yet reimplemented...");

                    // @TODO: reimplement me!!
                    //downloadPrideDatasets(selectedFiles);
                } else {
                    JOptionPane.showMessageDialog(PrideReshakeSetupDialog.this, "No spectrum files found. Reshake canceled.", "File Error", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.start();
    }//GEN-LAST:event_reshakeButtonActionPerformed

    /**
     * Open the UniProt download page for the given species.
     *
     * @param evt
     */
    private void downloadUniProtJLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_downloadUniProtJLabelMouseClicked
        if (downloadUniProtJLabel.isEnabled()) {

            String species = speciesJTextField.getText().trim();

            // try to clean up the species field, e.g., Mus musculus (Mouse) to Mus musculus
            if (species.endsWith(")") && species.lastIndexOf(",") == -1) {
                species = species.substring(0, species.indexOf("("));
            }

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
            BareBonesBrowserLaunch.openURL("http://www.uniprot.org/uniprot/?query=%28organism%3A%22" + species + "%22%29&sort=score");
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_downloadUniProtJLabelMouseClicked

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void downloadUniProtJLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_downloadUniProtJLabelMouseEntered
        if (downloadUniProtJLabel.isEnabled()) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }
    }//GEN-LAST:event_downloadUniProtJLabelMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void downloadUniProtJLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_downloadUniProtJLabelMouseExited
        if (downloadUniProtJLabel.isEnabled()) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_downloadUniProtJLabelMouseExited

    /**
     * Opens a file chooser where the user can select the database file.
     *
     * @param evt
     */
    private void browseDatabaseSettingsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseDatabaseSettingsButtonActionPerformed

        File startLocation = new File(prideReShakeGUI.getPeptideShakerGUI().getLastSelectedFolder());
        UtilitiesUserPreferences utilitiesUserPreferences = UtilitiesUserPreferences.loadUserPreferences();
        if (utilitiesUserPreferences.getDbFolder() != null && utilitiesUserPreferences.getDbFolder().exists()) {
            startLocation = utilitiesUserPreferences.getDbFolder();
        }

        // First check whether a file has already been selected.
        // If so, start from that file's parent.
        if (databaseSettingsTxt.getText() != null && new File(databaseSettingsTxt.getText()).exists()) {
            File temp = new File(databaseSettingsTxt.getText());
            startLocation = temp.getParentFile();
        }

        JFileChooser fc = new JFileChooser(startLocation);
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {
                return myFile.getName().toLowerCase().endsWith("fasta")
                        || myFile.getName().toLowerCase().endsWith("fas")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Supported formats: FASTA (.fasta or .fas)";
            }
        };

        fc.setFileFilter(filter);
        int result = fc.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();

            if (file.getName().indexOf(" ") != -1) {
                renameFastaFileName(file);
            } else {
                databaseSettingsTxt.setText(file.getAbsolutePath());
                databaseSettingsTxt.setText(file.getAbsolutePath());
            }

            prideReShakeGUI.getPeptideShakerGUI().setLastSelectedFolder(file.getAbsolutePath());
            targetDecoySettingsButton.setEnabled(true);

            // check if the database contains decoys
            if (!file.getAbsolutePath().endsWith(SequenceFactory.getTargetDecoyFileNameTag())) {

                int value = JOptionPane.showConfirmDialog(this,
                        "The selected FASTA file does not seem to contain decoy sequences.\n"
                        + "Decoys are required by PeptideShaker. Add decoys?", "Add Decoy Sequences?", JOptionPane.YES_NO_OPTION);

                if (value == JOptionPane.NO_OPTION) {
                    // do nothing
                } else if (value == JOptionPane.YES_OPTION) {
                    targetDecoySettingsButtonActionPerformed(null);
                }
            }

            validateInput(false);
        }
    }//GEN-LAST:event_browseDatabaseSettingsButtonActionPerformed

    /**
     * Generates a target-decoy database.
     *
     * @param evt
     */
    private void targetDecoySettingsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_targetDecoySettingsButtonActionPerformed
        generateTargetDecoyDatabase();
    }//GEN-LAST:event_targetDecoySettingsButtonActionPerformed

    /**
     * Open a file chooser where the user can select the working/output
     * directory.
     *
     * @param evt
     */
    private void browseWorkingFolderButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseWorkingFolderButtonActionPerformed
        File selectedFolder = Util.getUserSelectedFolder(this, "Select Working Folder", prideReShakeGUI.getPeptideShakerGUI().getLastSelectedFolder(), "Working Folder", "Select", false);

        if (selectedFolder != null) {
            prideReShakeGUI.getPeptideShakerGUI().setLastSelectedFolder(selectedFolder.getAbsolutePath());
            workingFolderTxt.setText(selectedFolder.getAbsolutePath());
        }

        validateInput(false);
    }//GEN-LAST:event_browseWorkingFolderButtonActionPerformed

    /**
     * Make sure that only one search settings file is selected.
     *
     * @param evt
     */
    private void searchSettingsTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_searchSettingsTableMouseReleased
        int rowSelectionCount = 0;

        for (int i = 0; i < searchSettingsTable.getRowCount(); i++) {
            if ((Boolean) searchSettingsTable.getValueAt(i, searchSettingsTable.getColumn("  ").getModelIndex())) {
                rowSelectionCount++;
            }
        }

        if (rowSelectionCount > 1) {
            JOptionPane.showMessageDialog(this, "You can only select one file to extract seach settings from.", "Search Settings Error", JOptionPane.WARNING_MESSAGE);
        }

        validateInput(false);

        if (evt != null) {

            int row = searchSettingsTable.getSelectedRow();
            int column = searchSettingsTable.getSelectedColumn();

            // open pride project link in web browser
            if (column == searchSettingsTable.getColumn("Assay").getModelIndex() && evt.getButton() == MouseEvent.BUTTON1
                    && ((String) searchSettingsTable.getValueAt(row, column)).lastIndexOf("<html>") != -1) {

                String link = (String) searchSettingsTable.getValueAt(row, column);
                link = link.substring(link.indexOf("\"") + 1);
                link = link.substring(0, link.indexOf("\""));

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                BareBonesBrowserLaunch.openURL(link);
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            } else if (column == searchSettingsTable.getColumn("Download").getModelIndex() && evt.getButton() == MouseEvent.BUTTON1
                    && ((String) searchSettingsTable.getValueAt(row, column)).lastIndexOf("<html>") != -1) {

                String tempLink = (String) searchSettingsTable.getValueAt(row, column);
                tempLink = tempLink.substring(tempLink.indexOf("\"") + 1);
                final String link = tempLink.substring(0, tempLink.indexOf("\""));
                final Double fileSize = (Double) searchSettingsTable.getValueAt(row, searchSettingsTable.getColumn("Size").getModelIndex());

                final File downloadFolder = Util.getUserSelectedFolder(this, "Select Download Folder", prideReShakeGUI.getPeptideShakerGUI().getLastSelectedFolder(), "Download Folder", "Select", false);

                if (downloadFolder != null) {

                    prideReShakeGUI.getPeptideShakerGUI().setLastSelectedFolder(downloadFolder.getAbsolutePath());

                    progressDialog = new ProgressDialogX(prideReShakeGUI,
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                            true);

                    progressDialog.setPrimaryProgressCounterIndeterminate(true);
                    progressDialog.setTitle("Downloading File. Please Wait...");

                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                progressDialog.setVisible(true);
                            } catch (IndexOutOfBoundsException e) {
                                // ignore
                            }
                        }
                    }, "ProgressDialog").start();

                    Thread thread = new Thread("DownloadThread") {
                        @Override
                        public void run() {
                            try {
                                File downLoadLocation = new File(downloadFolder, new File(link).getName());
                                File savedFile = prideReShakeGUI.saveUrl(downLoadLocation, link, fileSize, progressDialog);

                                boolean canceled = progressDialog.isRunCanceled();
                                progressDialog.setRunFinished();

                                if (!canceled) {
                                    JOptionPane.showMessageDialog(PrideReshakeSetupDialog.this, savedFile.getName() + " downloaded to\n"
                                            + savedFile + ".", "Download Complete", JOptionPane.INFORMATION_MESSAGE);
                                } else {
                                    if (downLoadLocation.exists()) {
                                        downLoadLocation.delete();
                                    }
                                }

                            } catch (MalformedURLException e) {
                                progressDialog.setRunFinished();
                                prideReShakeGUI.getPeptideShakerGUI().catchException(e);
                            } catch (IOException e) {
                                progressDialog.setRunFinished();
                                prideReShakeGUI.getPeptideShakerGUI().catchException(e);
                            }
                        }
                    };
                    thread.start();
                }
            }
        }
    }//GEN-LAST:event_searchSettingsTableMouseReleased

    /**
     * Changes the cursor into a hand cursor if the table cell contains an html
     * link.
     *
     * @param evt
     */
    private void spectrumTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumTableMouseMoved
        int row = spectrumTable.rowAtPoint(evt.getPoint());
        int column = spectrumTable.columnAtPoint(evt.getPoint());

        spectrumTable.setToolTipText(null);

        if (row != -1 && column != -1
                && (column == spectrumTable.getColumn("Assay").getModelIndex() || column == spectrumTable.getColumn("Download").getModelIndex())
                && spectrumTable.getValueAt(row, column) != null) {

            String tempValue = (String) spectrumTable.getValueAt(row, column);

            if (tempValue.lastIndexOf("<html>") != -1) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_spectrumTableMouseMoved

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void spectrumTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_spectrumTableMouseExited

    /**
     * Open the assay link.
     *
     * @param evt
     */
    private void spectrumTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumTableMouseReleased
        if (evt != null) {

            int row = spectrumTable.getSelectedRow();
            int column = spectrumTable.getSelectedColumn();

            // open pride project link in web browser
            if (column == spectrumTable.getColumn("Assay").getModelIndex() && evt.getButton() == MouseEvent.BUTTON1
                    && ((String) spectrumTable.getValueAt(row, column)).lastIndexOf("<html>") != -1) {

                String link = (String) spectrumTable.getValueAt(row, column);
                link = link.substring(link.indexOf("\"") + 1);
                link = link.substring(0, link.indexOf("\""));

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                BareBonesBrowserLaunch.openURL(link);
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            } else if (column == spectrumTable.getColumn("Download").getModelIndex() && evt.getButton() == MouseEvent.BUTTON1
                    && ((String) spectrumTable.getValueAt(row, column)).lastIndexOf("<html>") != -1) {

                String tempLink = (String) spectrumTable.getValueAt(row, column);
                tempLink = tempLink.substring(tempLink.indexOf("\"") + 1);
                final String link = tempLink.substring(0, tempLink.indexOf("\""));
                final Double fileSize = (Double) spectrumTable.getValueAt(row, spectrumTable.getColumn("Size").getModelIndex());

                final File downloadFolder = Util.getUserSelectedFolder(this, "Select Download Folder", prideReShakeGUI.getPeptideShakerGUI().getLastSelectedFolder(), "Download Folder", "Select", false);

                if (downloadFolder != null) {

                    prideReShakeGUI.getPeptideShakerGUI().setLastSelectedFolder(downloadFolder.getAbsolutePath());

                    progressDialog = new ProgressDialogX(prideReShakeGUI,
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                            true);

                    progressDialog.setPrimaryProgressCounterIndeterminate(true);
                    progressDialog.setTitle("Downloading File. Please Wait...");

                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                progressDialog.setVisible(true);
                            } catch (IndexOutOfBoundsException e) {
                                // ignore
                            }
                        }
                    }, "ProgressDialog").start();

                    Thread thread = new Thread("DownloadThread") {
                        @Override
                        public void run() {
                            try {
                                File downLoadLocation = new File(downloadFolder, new File(link).getName());
                                File savedFile = prideReShakeGUI.saveUrl(downLoadLocation, link, fileSize, progressDialog);

                                boolean canceled = progressDialog.isRunCanceled();
                                progressDialog.setRunFinished();

                                if (!canceled) {
                                    JOptionPane.showMessageDialog(PrideReshakeSetupDialog.this, savedFile.getName() + " downloaded to\n"
                                            + savedFile + ".", "Download Complete", JOptionPane.INFORMATION_MESSAGE);
                                } else {
                                    if (downLoadLocation.exists()) {
                                        downLoadLocation.delete();
                                    }
                                }

                            } catch (MalformedURLException e) {
                                progressDialog.setRunFinished();
                                prideReShakeGUI.getPeptideShakerGUI().catchException(e);
                            } catch (IOException e) {
                                progressDialog.setRunFinished();
                                prideReShakeGUI.getPeptideShakerGUI().catchException(e);
                            }
                        }
                    };
                    thread.start();
                }
            }

            validateInput(false);
        }
    }//GEN-LAST:event_spectrumTableMouseReleased

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void searchSettingsTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_searchSettingsTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_searchSettingsTableMouseExited

    /**
     * Changes the cursor into a hand cursor if the table cell contains an HTML
     * link.
     *
     * @param evt
     */
    private void searchSettingsTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_searchSettingsTableMouseMoved
        int row = searchSettingsTable.rowAtPoint(evt.getPoint());
        int column = searchSettingsTable.columnAtPoint(evt.getPoint());

        searchSettingsTable.setToolTipText(null);

        if (row != -1 && column != -1
                && (column == searchSettingsTable.getColumn("Assay").getModelIndex() || column == spectrumTable.getColumn("Download").getModelIndex())
                && searchSettingsTable.getValueAt(row, column) != null) {

            String tempValue = (String) searchSettingsTable.getValueAt(row, column);

            if (tempValue.lastIndexOf("<html>") != -1) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_searchSettingsTableMouseMoved

    /**
     * Open the PeptideShaker web page.
     *
     * @param evt
     */
    private void aboutButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutButtonActionPerformed
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        BareBonesBrowserLaunch.openURL("http://peptide-shaker.googlecode.com");
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_aboutButtonActionPerformed

    /**
     * Change the cursor to a hand icon.
     *
     * @param evt
     */
    private void aboutButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_aboutButtonMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_aboutButtonMouseEntered

    /**
     * Change the cursor back to the default icon.
     *
     * @param evt
     */
    private void aboutButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_aboutButtonMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_aboutButtonMouseExited

    /**
     * Select all the spectrum files.
     *
     * @param evt
     */
    private void selectAllLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_selectAllLabelMouseClicked
        for (int i = 0; i < spectrumTable.getRowCount(); i++) {
            spectrumTable.setValueAt(true, i, spectrumTable.getColumn("  ").getModelIndex());
        }
    }//GEN-LAST:event_selectAllLabelMouseClicked

    /**
     * Change the cursor to a hand icon.
     *
     * @param evt
     */
    private void selectAllLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_selectAllLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_selectAllLabelMouseEntered

    /**
     * Change the cursor back to the default icon.
     *
     * @param evt
     */
    private void selectAllLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_selectAllLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_selectAllLabelMouseExited

    /**
     * Deselect all the spectrum files.
     *
     * @param evt
     */
    private void deselectAllLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deselectAllLabelMouseClicked
        for (int i = 0; i < spectrumTable.getRowCount(); i++) {
            spectrumTable.setValueAt(false, i, spectrumTable.getColumn("  ").getModelIndex());
        }
    }//GEN-LAST:event_deselectAllLabelMouseClicked

    /**
     * Change the cursor to a hand icon.
     *
     * @param evt
     */
    private void deselectAllLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deselectAllLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_deselectAllLabelMouseEntered

    /**
     * Change the cursor back to the default icon.
     *
     * @param evt
     */
    private void deselectAllLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deselectAllLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_deselectAllLabelMouseExited

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton aboutButton;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton browseDatabaseSettingsButton;
    private javax.swing.JButton browseWorkingFolderButton;
    private javax.swing.JLabel dataTypeSeparatorLabel;
    private javax.swing.JPanel databasePanel;
    private javax.swing.JLabel databaseSettingsLbl;
    private javax.swing.JTextField databaseSettingsTxt;
    private javax.swing.JLabel deselectAllLabel;
    private javax.swing.JLabel downloadUniProtJLabel;
    private javax.swing.JLabel peptideShakerHomePageLabel;
    private javax.swing.JButton reshakeButton;
    private javax.swing.JLabel searchSettingsLabel;
    private javax.swing.JPanel searchSettingsPanel;
    private javax.swing.JScrollPane searchSettingsScrollPane;
    private javax.swing.JTable searchSettingsTable;
    private javax.swing.JLabel selectAllLabel;
    private javax.swing.JTextField speciesJTextField;
    private javax.swing.JLabel speciesLabel;
    private javax.swing.JLabel spectrumLabel;
    private javax.swing.JPanel spectrumPanel;
    private javax.swing.JTable spectrumTable;
    private javax.swing.JScrollPane spectrumTableScrollPane;
    private javax.swing.JButton targetDecoySettingsButton;
    private javax.swing.JLabel workingFolderLbl;
    private javax.swing.JPanel workingFolderPanel;
    private javax.swing.JTextField workingFolderTxt;
    // End of variables declaration//GEN-END:variables

    /**
     * Copies the content of the FASTA file to a new file and replaces any white
     * space in the file name with '_' instead.
     *
     * @param file
     */
    public void renameFastaFileName(File file) {

        // @TODO: this method should be merged with the identical method in SearchGUI...
        String tempName = file.getName();
        tempName = tempName.replaceAll(" ", "_");

        File renamedFile = new File(file.getParentFile().getAbsolutePath() + File.separator + tempName);

        boolean success = false;

        try {
            success = renamedFile.createNewFile();

            if (success) {

                FileReader r = new FileReader(file);
                BufferedReader br = new BufferedReader(r);

                FileWriter w = new FileWriter(renamedFile);
                BufferedWriter bw = new BufferedWriter(w);

                String line = br.readLine();

                while (line != null) {
                    bw.write(line + "\n");
                    line = br.readLine();
                }

                bw.close();
                w.close();
                br.close();
                r.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (success) {
            JOptionPane.showMessageDialog(this, "Your FASTA file name contained white space and has been renamed to:\n"
                    + file.getParentFile().getAbsolutePath() + File.separator + tempName, "Renamed File", JOptionPane.WARNING_MESSAGE);
            databaseSettingsTxt.setText(file.getParentFile().getAbsolutePath() + File.separator + tempName);
            targetDecoySettingsButton.setEnabled(true);
        } else {
            JOptionPane.showMessageDialog(this, "Your FASTA file name contains white space and has to been renamed.",
                    "Please Rename File", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Inspects the parameters validity.
     *
     * @param showMessage if true an error messages are shown to the users
     * @return a boolean indicating if the parameters are valid
     */
    public boolean validateInput(boolean showMessage) {

        boolean valid = true;
        databaseSettingsLbl.setForeground(Color.BLACK);
        databaseSettingsLbl.setToolTipText(null);
        workingFolderLbl.setForeground(Color.BLACK);
        workingFolderLbl.setToolTipText(null);

        // check the database
        if (databaseSettingsTxt.getText() == null || databaseSettingsTxt.getText().trim().isEmpty()) {
            if (showMessage && valid) {
                JOptionPane.showMessageDialog(this, "You need to specify a search database.", "Search Database Not Found", JOptionPane.WARNING_MESSAGE);
            }
            databaseSettingsLbl.setForeground(Color.RED);
            databaseSettingsLbl.setToolTipText("Please select a valid '.fasta' or '.fas' database file");
            valid = false;
        } else {
            File test = new File(databaseSettingsTxt.getText().trim());
            if (!test.exists()) {
                if (showMessage && valid) {
                    JOptionPane.showMessageDialog(this, "The database file could not be found.", "Search Database Not Found", JOptionPane.WARNING_MESSAGE);
                }
                databaseSettingsLbl.setForeground(Color.RED);
                databaseSettingsLbl.setToolTipText("Database file could not be found!");
                valid = false;
            }
        }

        // check the working folder
        if (workingFolderTxt.getText().trim().isEmpty()) {

            if (showMessage && valid) {
                JOptionPane.showMessageDialog(this, "You need to specify a working folder.", "Working Folder", JOptionPane.WARNING_MESSAGE);
            }
            workingFolderLbl.setForeground(Color.RED);
            workingFolderLbl.setToolTipText("Please select a working folder");
            valid = false;
        }

        // check if at least one spectrum file is selected
        int spectrumCounter = 0;
        for (int i = 0; i < spectrumTable.getRowCount() && spectrumCounter == 0; i++) {
            if ((Boolean) spectrumTable.getValueAt(i, spectrumTable.getColumn("  ").getModelIndex())) {
                spectrumCounter++;
            }
        }

        if (spectrumCounter == 0) {
            if (showMessage && valid) {
                JOptionPane.showMessageDialog(this, "You need to select at least one specturm file.", "Spectrum Files", JOptionPane.WARNING_MESSAGE);
            }
            valid = false;
        }

        // check that no more than one search settings file is selected
        int searchSettingsCount = 0;

        for (int i = 0; i < searchSettingsTable.getRowCount(); i++) {
            if ((Boolean) searchSettingsTable.getValueAt(i, searchSettingsTable.getColumn("  ").getModelIndex())) {
                searchSettingsCount++;
            }
        }

        if (searchSettingsCount > 1) {
            if (showMessage && valid) {
                JOptionPane.showMessageDialog(this, "You can only select one file to extract seach settings from.", "Search Settings Error", JOptionPane.WARNING_MESSAGE);
            }
            valid = false;
        }

        reshakeButton.setEnabled(valid);

        return valid;
    }

    /**
     * Adds a decoy database to the current FASTA file.
     */
    public void generateTargetDecoyDatabase() {

        // @TODO: this method should be merged with the identical method in SearchGUI...
        progressDialog = new ProgressDialogX(this, prideReShakeGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Creating Decoy. Please Wait...");

        final PrideReshakeSetupDialog finalRef = this;

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        new Thread("DecoyThread") {
            public void run() {

                String fastaInput = databaseSettingsTxt.getText().trim();
                try {
                    progressDialog.setTitle("Importing Database. Please Wait...");
                    progressDialog.setPrimaryProgressCounterIndeterminate(false);
                    sequenceFactory.loadFastaFile(new File(fastaInput), progressDialog);
                } catch (IOException e) {
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(finalRef,
                            "File " + fastaInput + " not found.",
                            "FASTA Import Error", JOptionPane.WARNING_MESSAGE);
                    e.printStackTrace();
                    return;
                } catch (ClassNotFoundException e) {
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(finalRef, JOptionEditorPane.getJOptionEditorPane("File index of " + fastaInput + " could not be imported.<br>"
                            + "Please <a href=\"http://code.google.com/p/peptide-shaker/issues/list\">contact the developers</a>."),
                            "FASTA Import Error", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                    return;
                } catch (StringIndexOutOfBoundsException e) {
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(finalRef,
                            e.getMessage(),
                            "FASTA Import Error", JOptionPane.WARNING_MESSAGE);
                    e.printStackTrace();
                    return;
                } catch (IllegalArgumentException e) {
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(finalRef,
                            e.getMessage(),
                            "FASTA Import Error", JOptionPane.WARNING_MESSAGE);
                    e.printStackTrace();
                    return;
                }

                if (sequenceFactory.concatenatedTargetDecoy() && !progressDialog.isRunCanceled()) {
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(finalRef,
                            "The database already contains decoy sequences.",
                            "FASTA File Already Decoy!", JOptionPane.WARNING_MESSAGE);
                    targetDecoySettingsButton.setEnabled(false);
                    return;
                }

                if (!progressDialog.isRunCanceled()) {

                    try {
                        String newFasta = fastaInput.substring(0, fastaInput.lastIndexOf("."));
                        newFasta += SequenceFactory.getTargetDecoyFileNameTag();
                        progressDialog.setTitle("Appending Decoy Sequences. Please Wait...");
                        sequenceFactory.appendDecoySequences(new File(newFasta), progressDialog);
                        databaseSettingsTxt.setText(newFasta);
                        targetDecoySettingsButton.setEnabled(false);
                    } catch (IllegalArgumentException e) {
                        progressDialog.setRunFinished();
                        JOptionPane.showMessageDialog(finalRef,
                                new String[]{"FASTA File Error.", fastaInput + " already contains decoy sequences."},
                                "FASTA File Error", JOptionPane.WARNING_MESSAGE);
                        targetDecoySettingsButton.setEnabled(false);
                        e.printStackTrace();
                        return;
                    } catch (OutOfMemoryError error) {
                        System.err.println("Ran out of memory!");
                        System.err.println("Memory given to the Java virtual machine: " + Runtime.getRuntime().maxMemory() + ".");
                        System.err.println("Memory used by the Java virtual machine: " + Runtime.getRuntime().totalMemory() + ".");
                        System.err.println("Free memory in the Java virtual machine: " + Runtime.getRuntime().freeMemory() + ".");
                        Runtime.getRuntime().gc();
                        progressDialog.setRunFinished();
                        JOptionPane.showMessageDialog(finalRef,
                                "PeptideShaker used up all the available memory and had to be stopped.\n"
                                + "Memory boundaries are changed in the the Welcome Dialog (Settings\n"
                                + "& Help > Settings > Java Memory Settings) or in the Edit menu (Edit\n"
                                + "Java Options).\n\n"
                                + "More help can be found at our website http://peptide-shaker.googlecode.com.",
                                "Out Of Memory Error",
                                JOptionPane.ERROR_MESSAGE);
                        System.out.println("Ran out of memory!");
                        error.printStackTrace();
                        return;
                    } catch (IOException e) {
                        progressDialog.setRunFinished();
                        JOptionPane.showMessageDialog(finalRef,
                                new String[]{"FASTA Import Error.", "File " + fastaInput + " not found."},
                                "FASTA Import Error", JOptionPane.WARNING_MESSAGE);
                        e.printStackTrace();
                        return;
                    } catch (InterruptedException e) {
                        progressDialog.setRunFinished();
                        JOptionPane.showMessageDialog(finalRef,
                                new String[]{"FASTA Import Error.", "File " + fastaInput + " could not be imported."},
                                "FASTA Import Error", JOptionPane.WARNING_MESSAGE);
                        e.printStackTrace();
                        return;
                    } catch (ClassNotFoundException e) {
                        progressDialog.setRunFinished();
                        JOptionPane.showMessageDialog(finalRef,
                                new String[]{"FASTA Import Error.", "File " + fastaInput + " could not be imported."},
                                "FASTA Import Error", JOptionPane.WARNING_MESSAGE);
                        e.printStackTrace();
                        return;
                    }
                }

                if (!progressDialog.isRunCanceled()) {
                    progressDialog.setRunFinished();
                    targetDecoySettingsButton.setEnabled(false);
                    JOptionPane.showMessageDialog(finalRef, "Concatenated decoy database created and selected.", "Decoy Created", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    progressDialog.setRunFinished();
                }
            }
        }.start();
    }
}
