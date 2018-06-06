package eu.isas.peptideshaker.gui.pride;

import com.compomics.software.dialogs.ProteoWizardSetupDialog;
import com.compomics.util.Util;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.io.biology.protein.FastaParameters;
import com.compomics.util.experiment.io.biology.protein.FastaSummary;
import com.compomics.util.experiment.mass_spectrometry.proteowizard.MsFormat;
import com.compomics.util.gui.JOptionEditorPane;
import com.compomics.util.gui.TableProperties;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.io.file.LastSelectedFolder;
import com.compomics.util.parameters.UtilitiesUserParameters;
import com.compomics.util.experiment.io.biology.protein.Header;
import com.compomics.util.experiment.io.biology.protein.ProteinDatabase;
import com.compomics.util.gui.parameters.identification.search.SequenceDbDetailsDialog;
import com.compomics.util.parameters.identification.search.SearchParameters;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import no.uib.jsparklines.extra.HtmlLinksRenderer;
import no.uib.jsparklines.extra.NimbusCheckBoxRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;

/**
 * Dialog for setting up the PRIDE Reshake.
 *
 * @author Harald Barsnes
 */
public class PrideReshakeSetupDialog extends javax.swing.JDialog {

    /**
     * The PrideReshakeGUI parent.
     */
    private final PrideReshakeGUI prideReShakeGUI;
    /**
     * The files table column header tooltips.
     */
    private ArrayList<String> filesTableToolTips;
    /**
     * A simple progress dialog.
     */
    private static ProgressDialogX progressDialog;
    /**
     * True if a file is currently being downloaded.
     */
    private boolean isFileBeingDownloaded = false;
    /**
     * The last selected folder.
     */
    private final LastSelectedFolder lastSelectedFolder;

    /**
     * Creates a new PrideReshakeSetupDialog.
     *
     * @param prideReShakeGUI the PrideReshakeGUI parent
     * @param modal if the dialog is to be modal or not
     */
    public PrideReshakeSetupDialog(PrideReshakeGUI prideReShakeGUI, boolean modal) {
        super(prideReShakeGUI, modal);
        lastSelectedFolder = prideReShakeGUI.getPeptideShakerGUI().getLastSelectedFolder();
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

        filesTableToolTips = new ArrayList<>();
        filesTableToolTips.add(null);
        filesTableToolTips.add("Assay Accession Numbers");
        filesTableToolTips.add("File Type");
        filesTableToolTips.add("File");
        filesTableToolTips.add("Download File");
        filesTableToolTips.add("File Size (MB)");
        filesTableToolTips.add("Selected");

        int fixedColumnWidth = 110;

        spectrumTable.getColumn("Assay").setMaxWidth(fixedColumnWidth);
        spectrumTable.getColumn("Assay").setMinWidth(fixedColumnWidth);
        spectrumTable.getColumn(" ").setMaxWidth(50);
        spectrumTable.getColumn(" ").setMinWidth(50);
        spectrumTable.getColumn("  ").setMaxWidth(30);
        spectrumTable.getColumn("  ").setMinWidth(30);
        spectrumTable.getColumn("Type").setMaxWidth(fixedColumnWidth);
        spectrumTable.getColumn("Type").setMinWidth(fixedColumnWidth);
        spectrumTable.getColumn("Download").setMaxWidth(fixedColumnWidth);
        spectrumTable.getColumn("Download").setMinWidth(fixedColumnWidth);
        spectrumTable.getColumn("Size (MB)").setMaxWidth(fixedColumnWidth);
        spectrumTable.getColumn("Size (MB)").setMinWidth(fixedColumnWidth);

        searchSettingsTable.getColumn("Assay").setMaxWidth(fixedColumnWidth);
        searchSettingsTable.getColumn("Assay").setMinWidth(fixedColumnWidth);
        searchSettingsTable.getColumn(" ").setMaxWidth(50);
        searchSettingsTable.getColumn(" ").setMinWidth(50);
        searchSettingsTable.getColumn("  ").setMaxWidth(30);
        searchSettingsTable.getColumn("  ").setMinWidth(30);
        searchSettingsTable.getColumn("Type").setMaxWidth(fixedColumnWidth);
        searchSettingsTable.getColumn("Type").setMinWidth(fixedColumnWidth);
        searchSettingsTable.getColumn("Download").setMaxWidth(fixedColumnWidth);
        searchSettingsTable.getColumn("Download").setMinWidth(fixedColumnWidth);
        searchSettingsTable.getColumn("Size (MB)").setMaxWidth(fixedColumnWidth);
        searchSettingsTable.getColumn("Size (MB)").setMinWidth(fixedColumnWidth);

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
        speciesJTextField.setText(prideReShakeGUI.getCurrentSpeciesList());

        if (prideReShakeGUI.getCurrentSpeciesList() == null
                || prideReShakeGUI.getCurrentSpeciesList().trim().isEmpty()) {
            downloadUniProtJLabel.setEnabled(false);
        }

        updateTables();
    }

    /**
     * Update the tables.
     */
    private void updateTables() {

        TableModel filesTableModel = prideReShakeGUI.getFilesTable().getRowSorter().getModel();

        double maxFileSize = 0.0;

        for (int i = 0; i < filesTableModel.getRowCount(); i++) {
            ((DefaultTableModel) spectrumTable.getModel()).addRow(new Object[]{
                spectrumTable.getRowCount() + 1,
                filesTableModel.getValueAt(i, 1),
                filesTableModel.getValueAt(i, 2),
                filesTableModel.getValueAt(i, 3),
                filesTableModel.getValueAt(i, 4),
                filesTableModel.getValueAt(i, 5),
                true
            });

            ((DefaultTableModel) searchSettingsTable.getModel()).addRow(new Object[]{
                searchSettingsTable.getRowCount() + 1,
                filesTableModel.getValueAt(i, 1),
                filesTableModel.getValueAt(i, 2),
                filesTableModel.getValueAt(i, 3),
                filesTableModel.getValueAt(i, 4),
                filesTableModel.getValueAt(i, 5),
                i == 0
            });

            double tempFileSize = (Double) filesTableModel.getValueAt(i, spectrumTable.getColumn("Size (MB)").getModelIndex());

            if (tempFileSize > maxFileSize) {
                maxFileSize = tempFileSize;
            }
        }

        // filter the spectrum table
        List<RowFilter<Object, Object>> spectrumFileFilters = new ArrayList<>();

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
        List<RowFilter<Object, Object>> searchSettingsFilesFilters = new ArrayList<>();

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

        // update the border titles
        ((TitledBorder) spectrumPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Spectrum Files (" + spectrumTable.getRowCount() + ")");
        spectrumPanel.repaint();
        ((TitledBorder) searchSettingsPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Search Settings (" + searchSettingsTable.getRowCount() + ")");
        searchSettingsPanel.repaint();

        // update the sparklines with the max values
        spectrumTable.getColumn("Size (MB)").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxFileSize, prideReShakeGUI.getPeptideShakerGUI().getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Size (MB)").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Size (MB)").getCellRenderer()).setLogScale(true);
        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Size (MB)").getCellRenderer()).setMinimumChartValue(1);

        searchSettingsTable.getColumn("Size (MB)").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxFileSize, prideReShakeGUI.getPeptideShakerGUI().getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) searchSettingsTable.getColumn("Size (MB)").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) searchSettingsTable.getColumn("Size (MB)").getCellRenderer()).setLogScale(true);
        ((JSparklinesBarChartTableCellRenderer) searchSettingsTable.getColumn("Size (MB)").getCellRenderer()).setMinimumChartValue(1);
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

                if (fileName.toLowerCase().endsWith(".pride.mgf.gz")
                        && fileName.toLowerCase().endsWith(".pride.mztab.gz")) { // @TODO: allow the pride files as soon as they can be downloaded
                    return false;
                }

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

        spectrumPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING +  "Spectrum Files"));
        spectrumPanel.setOpaque(false);

        spectrumLabel.setFont(spectrumLabel.getFont().deriveFont((spectrumLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        spectrumLabel.setText("Select the spectrum files to reanalyze: peak lists, raw files or PRIDE XML.");

        spectrumTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Assay", "Type", "File", "Download", "Size (MB)", "  "
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
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumTableScrollPane)
                .addContainerGap())
        );
        spectrumPanelLayout.setVerticalGroup(
            spectrumPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 174, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(spectrumPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(spectrumPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(deselectAllLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(selectAllLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(dataTypeSeparatorLabel))
                    .addComponent(spectrumLabel))
                .addContainerGap())
        );

        searchSettingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING +  "Search Settings"));
        searchSettingsPanel.setOpaque(false);

        searchSettingsLabel.setFont(searchSettingsLabel.getFont().deriveFont((searchSettingsLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        searchSettingsLabel.setText("Select the file to extract the search parameters from: mzIdentML or PRIDE XML.");

        searchSettingsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Assay", "Type", "File", "Download", "Size (MB)", "  "
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

        databasePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING +  "Sequence Database"));
        databasePanel.setOpaque(false);

        speciesLabel.setText("Species");

        speciesJTextField.setEditable(false);
        speciesJTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        speciesJTextField.setMargin(new java.awt.Insets(2, 4, 2, 2));

        downloadUniProtJLabel.setForeground(new java.awt.Color(0, 0, 255));
        downloadUniProtJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        downloadUniProtJLabel.setText("<html><u>UniProt</u></html>");
        downloadUniProtJLabel.setToolTipText("Click to Download UniProt Database");
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

        databaseSettingsLbl.setForeground(new java.awt.Color(0, 0, 255));
        databaseSettingsLbl.setText("<html><u>Database</u></html>");
        databaseSettingsLbl.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                databaseSettingsLblMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                databaseSettingsLblMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                databaseSettingsLblMouseExited(evt);
            }
        });

        databaseSettingsTxt.setEditable(false);
        databaseSettingsTxt.setMargin(new java.awt.Insets(2, 4, 2, 2));

        browseDatabaseSettingsButton.setText("Browse");
        browseDatabaseSettingsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseDatabaseSettingsButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout databasePanelLayout = new javax.swing.GroupLayout(databasePanel);
        databasePanel.setLayout(databasePanelLayout);
        databasePanelLayout.setHorizontalGroup(
            databasePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(databasePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(databasePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(databaseSettingsLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(speciesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(databasePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(speciesJTextField)
                    .addComponent(databaseSettingsTxt))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(databasePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(downloadUniProtJLabel)
                    .addComponent(browseDatabaseSettingsButton, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE))
                .addContainerGap())
        );
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
                    .addComponent(databaseSettingsLbl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(browseDatabaseSettingsButton)
                    .addComponent(databaseSettingsTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        workingFolderPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING +  "Output Folder"));
        workingFolderPanel.setOpaque(false);

        workingFolderLbl.setText("Folder");

        workingFolderTxt.setEditable(false);
        workingFolderTxt.setMargin(new java.awt.Insets(2, 4, 2, 2));

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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(workingFolderTxt)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(browseWorkingFolderButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
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
        reshakeButton.setText("  Start Reshaking!  ");
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
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 197, Short.MAX_VALUE)
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

        // check if we have any files that require proteowizard
        boolean msConvertRequired = false;
        for (int i = 0; i < spectrumTable.getRowCount(); i++) {
            if ((Boolean) spectrumTable.getValueAt(i, spectrumTable.getColumn("  ").getModelIndex())) {
                String fileName = (String) spectrumTable.getValueAt(i, spectrumTable.getColumn("File").getModelIndex());
                for (MsFormat format : MsFormat.values()) {
                    if (format != MsFormat.mgf && fileName.toLowerCase().endsWith(format.fileNameEnding)) {
                        msConvertRequired = true;
                        break;
                    }
                }
            }
        }

        // check if proteowizard is installed
        boolean proteoWizardFolderOk = true;
        if (msConvertRequired && prideReShakeGUI.getPeptideShakerGUI().getUtilitiesUserParameters().getProteoWizardPath() == null) {
            proteoWizardFolderOk = editProteoWizardInstallation();
            if (!proteoWizardFolderOk) {
                JOptionPane.showMessageDialog(this, "ProteoWizard folder not set. Currently supported spectrum formats are mgf and PRIDE XML.", "ProteoWizard Setup Error", JOptionPane.WARNING_MESSAGE);
            }
        }

        if (proteoWizardFolderOk) {

            progressDialog = new ProgressDialogX(prideReShakeGUI,
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                    true);
            progressDialog.setPrimaryProgressCounterIndeterminate(true);

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

                    ArrayList<String> selectedSpectrumFileLinks = new ArrayList<>();
                    ArrayList<String> selectedFileNames = new ArrayList<>();
                    String selectedSearchSettingsFileLink = null;
                    ArrayList<Integer> fileSizes = new ArrayList<>();

                    for (int i = 0; i < spectrumTable.getRowCount(); i++) {
                        if ((Boolean) spectrumTable.getValueAt(i, spectrumTable.getColumn("  ").getModelIndex())) {

                            String link = (String) spectrumTable.getValueAt(i, spectrumTable.getColumn("Download").getModelIndex());
                            link = link.substring(link.indexOf("\"") + 1);
                            link = link.substring(0, link.indexOf("\""));

                            boolean exists = Util.checkIfURLExists(link, prideReShakeGUI.getUserName(), prideReShakeGUI.getPassword());

                            if (!exists) {
                                if (link.endsWith(".gz")) {
                                    link = link.substring(0, link.length() - 3);
                                    exists = Util.checkIfURLExists(link, prideReShakeGUI.getUserName(), prideReShakeGUI.getPassword());
                                }
                            }

                            if (exists) {
                                // add the file name
                                String fileName = (String) spectrumTable.getValueAt(i, spectrumTable.getColumn("File").getModelIndex());
                                selectedFileNames.add(fileName);

                                // add the link to the file
                                selectedSpectrumFileLinks.add(link);

                                // add the file size
                                Double fileSizeInMB = (Double) spectrumTable.getValueAt(i, spectrumTable.getColumn("Size (MB)").getModelIndex());
                                int fileSizeInBytes;
                                if (fileSizeInMB != null) {
                                    fileSizeInBytes = new Double(fileSizeInMB * 1024 * 1024).intValue();
                                } else {
                                    fileSizeInBytes = -1;
                                }
                                fileSizes.add(fileSizeInBytes);
                            } else {
                                JOptionPane.showMessageDialog(PrideReshakeSetupDialog.this, JOptionEditorPane.getJOptionEditorPane(
                                        "PRIDE web service access error. Cannot open:<br>"
                                        + link + "<br>"
                                        + "Please contact the <a href=\"https://www.ebi.ac.uk/support/index.php?query=pride\">PRIDE team</a>."),
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

                            String selectedSearchSettingsFileName = (String) searchSettingsTable.getValueAt(i, searchSettingsTable.getColumn("File").getModelIndex());

                            if (!selectedSpectrumFileLinks.contains(link)) {

                                boolean exists = Util.checkIfURLExists(link, prideReShakeGUI.getUserName(), prideReShakeGUI.getPassword());

                                if (!exists) {
                                    if (link.endsWith(".gz")) {
                                        link = link.substring(0, link.length() - 3);
                                        if (!selectedSpectrumFileLinks.contains(link)) {
                                            exists = Util.checkIfURLExists(link, prideReShakeGUI.getUserName(), prideReShakeGUI.getPassword());
                                        } else {
                                            exists = true;
                                        }
                                    }
                                }

                                if (exists) {
                                    selectedSearchSettingsFileLink = link;
                                    selectedFileNames.add(selectedSearchSettingsFileName);
                                    Double fileSizeInMB = (Double) searchSettingsTable.getValueAt(i, searchSettingsTable.getColumn("Size (MB)").getModelIndex());
                                    int fileSizeInBytes;
                                    if (fileSizeInMB != null) {
                                        fileSizeInBytes = new Double(fileSizeInMB * 1024 * 1024).intValue();
                                    } else {
                                        fileSizeInBytes = -1;
                                    }
                                    fileSizes.add(fileSizeInBytes);
                                } else {
                                    JOptionPane.showMessageDialog(PrideReshakeSetupDialog.this, JOptionEditorPane.getJOptionEditorPane(
                                            "PRIDE web service access error. Cannot open:<br>"
                                            + link + "<br>"
                                            + "Please contact the <a href=\"https://www.ebi.ac.uk/support/index.php?query=pride\">PRIDE team</a>."),
                                            "PRIDE Access Error", JOptionPane.WARNING_MESSAGE);
                                    System.out.println("Not found: " + link + "!");
                                }
                            } else {
                                selectedSearchSettingsFileLink = link;
                            }
                        }
                    }

                    boolean download = true;

                    if (selectedSpectrumFileLinks.isEmpty()) {
                        download = false;
                    }

                    progressDialog.setRunFinished();

                    if (download) {
                        prideReShakeGUI.downloadPrideDatasets(workingFolderTxt.getText(), selectedSpectrumFileLinks, selectedFileNames,
                                selectedSearchSettingsFileLink, databaseSettingsTxt.getText(), speciesJTextField.getText(), fileSizes);
                    } else {
                        JOptionPane.showMessageDialog(PrideReshakeSetupDialog.this, "No spectrum files found. Reshake canceled.", "File Error", JOptionPane.WARNING_MESSAGE);
                    }
                }
            }.start();
        }
    }//GEN-LAST:event_reshakeButtonActionPerformed

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

        SearchParameters searchParameters = prideReShakeGUI.getPeptideShakerGUI().getIdentificationParameters().getSearchParameters();
        File fastaFile = searchParameters.getFastaFile();

        SequenceDbDetailsDialog sequenceDbDetailsDialog = new SequenceDbDetailsDialog(prideReShakeGUI, fastaFile, searchParameters.getFastaParameters(),
                prideReShakeGUI.getPeptideShakerGUI().getLastSelectedFolder(), true,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

        boolean success = sequenceDbDetailsDialog.selectDB(true);
        if (success) {
            
            sequenceDbDetailsDialog.setVisible(true);
            
            fastaFile = sequenceDbDetailsDialog.getSelectedFastaFile();
            searchParameters.setFastaFile(fastaFile);
            searchParameters.setFastaParameters(sequenceDbDetailsDialog.getFastaParameters());
 
        }

        lastSelectedFolder.setLastSelectedFolder(sequenceDbDetailsDialog.getLastSelectedFolder());

        if (fastaFile != null) {

            databaseSettingsTxt.setText(fastaFile.getAbsolutePath());
            checkFastaFile();

        }

        validateInput(false);
    }//GEN-LAST:event_browseDatabaseSettingsButtonActionPerformed

    /**
     * Open a file chooser where the user can select the working/output
     * directory.
     *
     * @param evt
     */
    private void browseWorkingFolderButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseWorkingFolderButtonActionPerformed
        File selectedFolder = Util.getUserSelectedFolder(this, "Select Working Folder", lastSelectedFolder.getLastSelectedFolder(), "Working Folder", "Select", false);

        if (selectedFolder != null) {
            lastSelectedFolder.setLastSelectedFolder(selectedFolder.getAbsolutePath());
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

        int row = searchSettingsTable.rowAtPoint(evt.getPoint());
        int column = searchSettingsTable.columnAtPoint(evt.getPoint());

        if (column == searchSettingsTable.getColumn("  ").getModelIndex() && row != -1) {
            if ((Boolean) searchSettingsTable.getValueAt(row, searchSettingsTable.getColumn("  ").getModelIndex())) {
                for (int i = 0; i < searchSettingsTable.getRowCount(); i++) {
                    searchSettingsTable.setValueAt(i == row, i, searchSettingsTable.getColumn("  ").getModelIndex());
                }
            }
        }

        validateInput(false);

        row = searchSettingsTable.getSelectedRow();
        column = searchSettingsTable.getSelectedColumn();

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
            Double fileSizeInMB = (Double) searchSettingsTable.getValueAt(row, searchSettingsTable.getColumn("Size (MB)").getModelIndex());
            final int fileSizeInBytes;
            if (fileSizeInMB != null) {
                fileSizeInBytes = new Double(fileSizeInMB * 1024 * 1024).intValue();
            } else {
                fileSizeInBytes = -1;
            }

            final String fileName = (String) spectrumTable.getValueAt(row, spectrumTable.getColumn("File").getModelIndex());
            final File downloadFolder = Util.getUserSelectedFolder(this, "Select Download Folder", lastSelectedFolder.getLastSelectedFolder(), "Download Folder", "Select", false);

            if (downloadFolder != null) {

                lastSelectedFolder.setLastSelectedFolder(downloadFolder.getAbsolutePath());

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
                            File downLoadLocation = new File(downloadFolder, fileName);
                            File savedFile = Util.saveUrl(downLoadLocation, link, fileSizeInBytes, prideReShakeGUI.getUserName(), prideReShakeGUI.getPassword(), progressDialog);

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
                final Double fileSizeInMB = (Double) spectrumTable.getValueAt(row, spectrumTable.getColumn("Size (MB)").getModelIndex());
                final int fileSizeInBytes;
                if (fileSizeInMB != null) {
                    fileSizeInBytes = new Double(fileSizeInMB * 1024 * 1024).intValue();
                } else {
                    fileSizeInBytes = -1;
                }

                final String fileName = (String) spectrumTable.getValueAt(row, spectrumTable.getColumn("File").getModelIndex());
                final File downloadFolder = Util.getUserSelectedFolder(this, "Select Download Folder", lastSelectedFolder.getLastSelectedFolder(), "Download Folder", "Select", false);

                if (downloadFolder != null) {

                    lastSelectedFolder.setLastSelectedFolder(downloadFolder.getAbsolutePath());

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
                                File downLoadLocation = new File(downloadFolder, fileName);
                                File savedFile = Util.saveUrl(downLoadLocation, link, fileSizeInBytes, prideReShakeGUI.getUserName(), prideReShakeGUI.getPassword(), progressDialog);

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
        BareBonesBrowserLaunch.openURL("https://compomics.github.io/projects/peptide-shaker.html");
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
        validateInput(false);
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
        validateInput(false);
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

    /**
     * Open the database help web page.
     *
     * @param evt
     */
    private void databaseSettingsLblMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_databaseSettingsLblMouseClicked
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        BareBonesBrowserLaunch.openURL("https://compomics.github.io/projects/searchgui/wiki/databasehelp.html");
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_databaseSettingsLblMouseClicked

    /**
     * Change the cursor to a hand icon.
     *
     * @param evt
     */
    private void databaseSettingsLblMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_databaseSettingsLblMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_databaseSettingsLblMouseEntered

    /**
     * Change the cursor back to the default icon.
     *
     * @param evt
     */
    private void databaseSettingsLblMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_databaseSettingsLblMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_databaseSettingsLblMouseExited

    /**
     * Open the UniProt download page for the given species.
     *
     * @param evt
     */
    private void downloadUniProtJLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_downloadUniProtJLabelMouseClicked
        if (downloadUniProtJLabel.isEnabled()) {

            String tempSpecies = speciesJTextField.getText().trim();

            String[] allSpecies = tempSpecies.split(", ");

            boolean combinedSearch = true;

            if (allSpecies.length > 1) {

                int option = JOptionPane.showConfirmDialog(this,
                        "You have multiple species. Combine into one UniProt search?\n"
                        + "Choosing 'No' will open one UniProt web page per species.",
                        "Multiple Species", JOptionPane.YES_NO_CANCEL_OPTION);

                if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) {
                    return;
                }

                combinedSearch = option == JOptionPane.YES_OPTION;
            }

            String link = "https://www.uniprot.org/uniprot/?query=";

            for (int i = 0; i < allSpecies.length; i++) {

                String species = allSpecies[i];

                // try to clean up the species field, e.g., Mus musculus (Mouse) to Mus musculus
                if (species.endsWith(")")) {
                    species = species.substring(0, species.indexOf("(")).trim();
                }

                species = species.replaceAll(" ", "%20");

                if (combinedSearch) {
                    if (i > 0) {
                        link += "+OR+";
                    }
                    link += "organism%3A%22" + species + "%22";
                } else {
                    link = "https://www.uniprot.org/uniprot/?query=organism%3A%22" + species + "%22&sort=score";
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                    BareBonesBrowserLaunch.openURL(link);
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (combinedSearch) {
                link += "&sort=score";
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                BareBonesBrowserLaunch.openURL(link);
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        }
    }//GEN-LAST:event_downloadUniProtJLabelMouseClicked

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
    private javax.swing.JLabel workingFolderLbl;
    private javax.swing.JPanel workingFolderPanel;
    private javax.swing.JTextField workingFolderTxt;
    // End of variables declaration//GEN-END:variables

    /**
     * Inspects the parameters validity.
     *
     * @param showMessage if true an error messages are shown to the users
     * @return a boolean indicating if the parameters are valid
     */
    public boolean validateInput(boolean showMessage) {

        boolean valid = true;

        // check the database
        if (databaseSettingsTxt.getText() == null || databaseSettingsTxt.getText().trim().isEmpty()) {
            if (showMessage && valid) {
                JOptionPane.showMessageDialog(this, "You need to specify a search database.", "Search Database Not Found", JOptionPane.WARNING_MESSAGE);
            }
            valid = false;
        } else {
            File test = new File(databaseSettingsTxt.getText().trim());
            if (!test.exists()) {
                if (showMessage && valid) {
                    JOptionPane.showMessageDialog(this, "The database file could not be found.", "Search Database Not Found", JOptionPane.WARNING_MESSAGE);
                }
                valid = false;
            }
        }

        // check the working folder
        if (workingFolderTxt.getText().trim().isEmpty()) {
            if (showMessage && valid) {
                JOptionPane.showMessageDialog(this, "You need to specify a working folder.", "Working Folder", JOptionPane.WARNING_MESSAGE);
            }
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
     * Checks whether the FASTA file loaded contains mainly UniProt concatenated
     * target decoy.
     */
    public void checkFastaFile() {
        
        try {
            
        SearchParameters searchParameters = prideReShakeGUI.getPeptideShakerGUI().getIdentificationParameters().getSearchParameters();
        FastaSummary fastaSummary = FastaSummary.getSummary(searchParameters.getFastaFile(), searchParameters.getFastaParameters(), progressDialog);
        
        if (!fastaSummary.databaseType.containsKey(ProteinDatabase.UniProt)) {
            
            showDataBaseHelpDialog();
 
        }
        if (!searchParameters.getFastaParameters().isTargetDecoy()) {
            
            JOptionPane.showMessageDialog(this, "PeptideShaker validation requires the use of a taget-decoy database.\n"
                    + "Some features will be limited if using other types of databases.\n\n"
                    + "Note that using Automatic Decoy Search in Mascot is not supported.\n\n"
                    + "See the PeptideShaker home page for details.",
                    "No Decoys Found",
                    JOptionPane.INFORMATION_MESSAGE);
        
        }
        
        } catch (IOException exception) {
            
            JOptionPane.showMessageDialog(this, "An error occurred while parsing the fasta file.",
                    "Fasta File Error",
                    JOptionPane.INFORMATION_MESSAGE);
            
        }
    }

    /**
     * Show a simple dialog saying that UniProt databases is recommended and
     * display a link to the Database Help web page.
     */
    private void showDataBaseHelpDialog() {
        JOptionPane.showMessageDialog(this, JOptionEditorPane.getJOptionEditorPane(
                "We strongly recommend the use of UniProt databases. Some<br>"
                + "features will be limited if using other databases.<br><br>"
                + "See <a href=\"https://compomics.github.io/projects/searchgui/wiki/databasehelp.html\">Database Help</a> for details."),
                "Database Information", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Opens a dialog allowing the edition of the ProteoWizard installation
     * folder.
     *
     * @return true of the installation is now set
     */
    public boolean editProteoWizardInstallation() {

        boolean canceled = false;

        try {
            ProteoWizardSetupDialog proteoWizardSetupDialog = new ProteoWizardSetupDialog(this, true);
            canceled = proteoWizardSetupDialog.isDialogCanceled();

            if (!canceled) {

                // reload the user preferences
                try {
                    prideReShakeGUI.getPeptideShakerGUI().setUtilitiesUserParameters(UtilitiesUserParameters.loadUserParameters());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "An error occurred when reading the user preferences.", "File Error", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return !canceled;
    }
}
