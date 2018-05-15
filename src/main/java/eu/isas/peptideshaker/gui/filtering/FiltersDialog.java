package eu.isas.peptideshaker.gui.filtering;

import com.compomics.util.experiment.filtering.Filter;
import com.compomics.util.experiment.identification.filtering.MatchFilter;
import com.compomics.util.experiment.identification.filtering.PeptideFilter;
import com.compomics.util.experiment.identification.filtering.ProteinFilter;
import com.compomics.util.experiment.identification.filtering.PsmFilter;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import no.uib.jsparklines.extra.NimbusCheckBoxRenderer;
import com.compomics.util.gui.error_handlers.HelpDialog;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;

/**
 * Displays the filters used for star/hide items.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class FiltersDialog extends javax.swing.JDialog {

    /**
     * The main GUI.
     */
    private final PeptideShakerGUI peptideShakerGUI;
    /**
     * The protein star filters.
     */
    private HashMap<String, ProteinFilter> proteinStarFilters;
    /**
     * The protein hide filters.
     */
    private HashMap<String, ProteinFilter> proteinHideFilters;
    /**
     * The peptide star filters.
     */
    private HashMap<String, PeptideFilter> peptideStarFilters;
    /**
     * The peptide hide filters.
     */
    private HashMap<String, PeptideFilter> peptideHideFilters;
    /**
     * The PSM star filters.
     */
    private HashMap<String, PsmFilter> psmStarFilters;
    /**
     * The PSM hide filters.
     */
    private HashMap<String, PsmFilter> psmHideFilters;

    /**
     * Creates a new FiltersDialog.
     *
     * @param peptideShakerGUI the PeptideShakerGUI parent
     */
    public FiltersDialog(PeptideShakerGUI peptideShakerGUI) {
        super(peptideShakerGUI, true);

        initComponents();

        setTableProperties();

        this.peptideShakerGUI = peptideShakerGUI;
        setLocationRelativeTo(peptideShakerGUI);
        updateMaps();
        fillTables();
    }

    /**
     * Set up the properties of the tables.
     */
    private void setTableProperties() {

        // set table properties
        starredProteinsTable.getTableHeader().setReorderingAllowed(false);
        hiddenProteinsTable.getTableHeader().setReorderingAllowed(false);
        starredPeptidesTable.getTableHeader().setReorderingAllowed(false);
        hiddenPeptidesTable.getTableHeader().setReorderingAllowed(false);
        starredPsmTable.getTableHeader().setReorderingAllowed(false);
        hiddenPsmTable.getTableHeader().setReorderingAllowed(false);

        starredProteinsTable.setAutoCreateRowSorter(true);
        hiddenProteinsTable.setAutoCreateRowSorter(true);
        starredPeptidesTable.setAutoCreateRowSorter(true);
        hiddenPeptidesTable.setAutoCreateRowSorter(true);
        starredPsmTable.setAutoCreateRowSorter(true);
        hiddenPsmTable.setAutoCreateRowSorter(true);

        // the index columns
        starredProteinsTable.getColumn("").setMaxWidth(50);
        hiddenProteinsTable.getColumn("").setMaxWidth(50);
        starredPeptidesTable.getColumn("").setMaxWidth(50);
        hiddenPeptidesTable.getColumn("").setMaxWidth(50);
        starredPsmTable.getColumn("").setMaxWidth(50);
        hiddenPsmTable.getColumn("").setMaxWidth(50);
        starredProteinsTable.getColumn("").setMinWidth(50);
        hiddenProteinsTable.getColumn("").setMinWidth(50);
        starredPeptidesTable.getColumn("").setMinWidth(50);
        hiddenPeptidesTable.getColumn("").setMinWidth(50);
        starredPsmTable.getColumn("").setMinWidth(50);
        hiddenPsmTable.getColumn("").setMinWidth(50);

        // the selected columns
        starredProteinsTable.getColumn(" ").setMaxWidth(30);
        hiddenProteinsTable.getColumn(" ").setMaxWidth(30);
        starredPeptidesTable.getColumn(" ").setMaxWidth(30);
        hiddenPeptidesTable.getColumn(" ").setMaxWidth(30);
        starredPsmTable.getColumn(" ").setMaxWidth(30);
        hiddenPsmTable.getColumn(" ").setMaxWidth(30);
        starredProteinsTable.getColumn(" ").setMinWidth(30);
        hiddenProteinsTable.getColumn(" ").setMinWidth(30);
        starredPeptidesTable.getColumn(" ").setMinWidth(30);
        hiddenPeptidesTable.getColumn(" ").setMinWidth(30);
        starredPsmTable.getColumn(" ").setMinWidth(30);
        hiddenPsmTable.getColumn(" ").setMinWidth(30);

        starredProteinsScrollPane.getViewport().setOpaque(false);
        hiddenProteinsScrollPane.getViewport().setOpaque(false);
        starredPeptidesScrollPane.getViewport().setOpaque(false);
        hiddenPeptidesScrollPane.getViewport().setOpaque(false);
        starredPsmsScrollPane.getViewport().setOpaque(false);
        hiddenPsmsScrollPane.getViewport().setOpaque(false);

        // cell renderer
        starredProteinsTable.getColumn(" ").setCellRenderer(new NimbusCheckBoxRenderer());
        hiddenProteinsTable.getColumn(" ").setCellRenderer(new NimbusCheckBoxRenderer());
        starredPeptidesTable.getColumn(" ").setCellRenderer(new NimbusCheckBoxRenderer());
        hiddenPeptidesTable.getColumn(" ").setCellRenderer(new NimbusCheckBoxRenderer());
        starredPsmTable.getColumn(" ").setCellRenderer(new NimbusCheckBoxRenderer());
        hiddenPsmTable.getColumn(" ").setCellRenderer(new NimbusCheckBoxRenderer());
    }

    /**
     * Update the maps.
     */
    private void updateMaps() {
        proteinStarFilters = new HashMap<>();
        proteinHideFilters = new HashMap<>();
        peptideStarFilters = new HashMap<>();
        peptideHideFilters = new HashMap<>();
        psmStarFilters = new HashMap<>();
        psmHideFilters = new HashMap<>();
        proteinStarFilters.putAll(peptideShakerGUI.getFilterParameters().getProteinStarFilters());
        proteinHideFilters.putAll(peptideShakerGUI.getFilterParameters().getProteinHideFilters());
        peptideStarFilters.putAll(peptideShakerGUI.getFilterParameters().getPeptideStarFilters());
        peptideHideFilters.putAll(peptideShakerGUI.getFilterParameters().getPeptideHideFilters());
        psmStarFilters.putAll(peptideShakerGUI.getFilterParameters().getPsmStarFilters());
        psmHideFilters.putAll(peptideShakerGUI.getFilterParameters().getPsmHideFilters());
    }

    /**
     * Empty the maps.
     */
    private void emptyTables() {
        while (hiddenProteinsTable.getRowCount() > 0) {
            ((DefaultTableModel) hiddenProteinsTable.getModel()).removeRow(0);
        }
        while (starredProteinsTable.getRowCount() > 0) {
            ((DefaultTableModel) starredProteinsTable.getModel()).removeRow(0);
        }
        while (hiddenPeptidesTable.getRowCount() > 0) {
            ((DefaultTableModel) hiddenPeptidesTable.getModel()).removeRow(0);
        }
        while (starredPeptidesTable.getRowCount() > 0) {
            ((DefaultTableModel) starredPeptidesTable.getModel()).removeRow(0);
        }
        while (hiddenPsmTable.getRowCount() > 0) {
            ((DefaultTableModel) hiddenPsmTable.getModel()).removeRow(0);
        }
        while (starredPsmTable.getRowCount() > 0) {
            ((DefaultTableModel) starredPsmTable.getModel()).removeRow(0);
        }
    }

    /**
     * Fill the tables.
     */
    private void fillTables() {
        for (MatchFilter matchFilter : proteinStarFilters.values()) {
            ((DefaultTableModel) starredProteinsTable.getModel()).addRow(new Object[]{
                starredProteinsTable.getRowCount() + 1,
                matchFilter.isActive(),
                matchFilter.getName(),
                matchFilter.getDescription()
            });
        }
        for (MatchFilter matchFilter : peptideStarFilters.values()) {
            ((DefaultTableModel) starredPeptidesTable.getModel()).addRow(new Object[]{
                starredPeptidesTable.getRowCount() + 1,
                matchFilter.isActive(),
                matchFilter.getName(),
                matchFilter.getDescription()
            });
        }
        for (MatchFilter matchFilter : psmStarFilters.values()) {
            ((DefaultTableModel) starredPsmTable.getModel()).addRow(new Object[]{
                starredPsmTable.getRowCount() + 1,
                matchFilter.isActive(),
                matchFilter.getName(),
                matchFilter.getDescription()
            });
        }
        for (MatchFilter matchFilter : proteinHideFilters.values()) {
            ((DefaultTableModel) hiddenProteinsTable.getModel()).addRow(new Object[]{
                hiddenProteinsTable.getRowCount() + 1,
                matchFilter.isActive(),
                matchFilter.getName(),
                matchFilter.getDescription()
            });
        }
        for (MatchFilter matchFilter : peptideHideFilters.values()) {
            ((DefaultTableModel) hiddenPeptidesTable.getModel()).addRow(new Object[]{
                hiddenPeptidesTable.getRowCount() + 1,
                matchFilter.isActive(),
                matchFilter.getName(),
                matchFilter.getDescription()
            });
        }
        for (MatchFilter matchFilter : psmHideFilters.values()) {
            ((DefaultTableModel) hiddenPsmTable.getModel()).addRow(new Object[]{
                hiddenPsmTable.getRowCount() + 1,
                matchFilter.isActive(),
                matchFilter.getName(),
                matchFilter.getDescription()
            });
        }
        setVisible(true);
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
        tabbedPane = new javax.swing.JTabbedPane();
        proteinsSplitPane = new javax.swing.JSplitPane();
        starredProteinsPanel = new javax.swing.JPanel();
        starredProteinsScrollPane = new javax.swing.JScrollPane();
        starredProteinsTable = new javax.swing.JTable();
        addStarredProtein = new javax.swing.JButton();
        editStarredProtein = new javax.swing.JButton();
        clearStarredProtein = new javax.swing.JButton();
        deleteStarredProtein = new javax.swing.JButton();
        hiddenProteinsPanel = new javax.swing.JPanel();
        hiddenProteinsScrollPane = new javax.swing.JScrollPane();
        hiddenProteinsTable = new javax.swing.JTable();
        addHiddenProtein = new javax.swing.JButton();
        editHiddenProtein = new javax.swing.JButton();
        clearHiddenProtein = new javax.swing.JButton();
        deleteHiddenProtein = new javax.swing.JButton();
        peptidesSplitPane = new javax.swing.JSplitPane();
        starredPeptidesPanel = new javax.swing.JPanel();
        starredPeptidesScrollPane = new javax.swing.JScrollPane();
        starredPeptidesTable = new javax.swing.JTable();
        addStarredPeptides = new javax.swing.JButton();
        editStarredPeptides = new javax.swing.JButton();
        clearStarredPeptides = new javax.swing.JButton();
        deleteStarredPeptides = new javax.swing.JButton();
        hiddenPeptidesPanel = new javax.swing.JPanel();
        hiddenPeptidesScrollPane = new javax.swing.JScrollPane();
        hiddenPeptidesTable = new javax.swing.JTable();
        addHiddenPeptides = new javax.swing.JButton();
        editHiddenPeptides = new javax.swing.JButton();
        clearHiddenPeptides = new javax.swing.JButton();
        deleteHiddenPeptides = new javax.swing.JButton();
        psmsSplitPane = new javax.swing.JSplitPane();
        starredPsmsPanel = new javax.swing.JPanel();
        starredPsmsScrollPane = new javax.swing.JScrollPane();
        starredPsmTable = new javax.swing.JTable();
        addStarredPsm = new javax.swing.JButton();
        editStarredPsm = new javax.swing.JButton();
        clearStarredPsm = new javax.swing.JButton();
        deleteStarredPsm = new javax.swing.JButton();
        hiddenPsmsPanel = new javax.swing.JPanel();
        hiddenPsmsScrollPane = new javax.swing.JScrollPane();
        hiddenPsmTable = new javax.swing.JTable();
        addHiddenPsm = new javax.swing.JButton();
        editHiddenPsm = new javax.swing.JButton();
        clearHiddenPsm = new javax.swing.JButton();
        deleteHiddenPsm = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        openDialogHelpJButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Filter Selection");

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        tabbedPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                tabbedPaneComponentResized(evt);
            }
        });

        proteinsSplitPane.setBorder(null);
        proteinsSplitPane.setDividerLocation(200);
        proteinsSplitPane.setDividerSize(-1);
        proteinsSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        proteinsSplitPane.setResizeWeight(0.5);
        proteinsSplitPane.setOpaque(false);

        starredProteinsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Starred Proteins"));
        starredProteinsPanel.setOpaque(false);
        starredProteinsPanel.setPreferredSize(new java.awt.Dimension(613, 195));

        starredProteinsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "", " ", "Name", "Description"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Boolean.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        starredProteinsTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        starredProteinsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                starredProteinsTableMouseReleased(evt);
            }
        });
        starredProteinsTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                starredProteinsTableKeyReleased(evt);
            }
        });
        starredProteinsScrollPane.setViewportView(starredProteinsTable);

        addStarredProtein.setText("Add");
        addStarredProtein.setToolTipText("Add a starred proteins filter");
        addStarredProtein.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addStarredProteinActionPerformed(evt);
            }
        });

        editStarredProtein.setText("Edit");
        editStarredProtein.setToolTipText("Edit selected filter");
        editStarredProtein.setEnabled(false);
        editStarredProtein.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editStarredProteinActionPerformed(evt);
            }
        });

        clearStarredProtein.setText("Clear");
        clearStarredProtein.setToolTipText("Remove all starred proteins filters");
        clearStarredProtein.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearStarredProteinActionPerformed(evt);
            }
        });

        deleteStarredProtein.setText("Delete");
        deleteStarredProtein.setToolTipText("Delete selected filter");
        deleteStarredProtein.setEnabled(false);
        deleteStarredProtein.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteStarredProteinActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout starredProteinsPanelLayout = new javax.swing.GroupLayout(starredProteinsPanel);
        starredProteinsPanel.setLayout(starredProteinsPanelLayout);
        starredProteinsPanelLayout.setHorizontalGroup(
            starredProteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(starredProteinsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(starredProteinsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 561, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(starredProteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(editStarredProtein, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(addStarredProtein, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(clearStarredProtein, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(deleteStarredProtein, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        starredProteinsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {addStarredProtein, clearStarredProtein, deleteStarredProtein, editStarredProtein});

        starredProteinsPanelLayout.setVerticalGroup(
            starredProteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(starredProteinsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(starredProteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(starredProteinsScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(starredProteinsPanelLayout.createSequentialGroup()
                        .addComponent(addStarredProtein)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(editStarredProtein)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deleteStarredProtein)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearStarredProtein)
                        .addGap(0, 45, Short.MAX_VALUE)))
                .addContainerGap())
        );

        proteinsSplitPane.setLeftComponent(starredProteinsPanel);

        hiddenProteinsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Hidden Proteins"));
        hiddenProteinsPanel.setOpaque(false);
        hiddenProteinsPanel.setPreferredSize(new java.awt.Dimension(613, 195));

        hiddenProteinsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "", " ", "Name", "Description"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Boolean.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        hiddenProteinsTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        hiddenProteinsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                hiddenProteinsTableMouseReleased(evt);
            }
        });
        hiddenProteinsTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                hiddenProteinsTableKeyReleased(evt);
            }
        });
        hiddenProteinsScrollPane.setViewportView(hiddenProteinsTable);

        addHiddenProtein.setText("Add");
        addHiddenProtein.setToolTipText("Add a hidden proteins filter");
        addHiddenProtein.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addHiddenProteinActionPerformed(evt);
            }
        });

        editHiddenProtein.setText("Edit");
        editHiddenProtein.setToolTipText("Edit selected filter");
        editHiddenProtein.setEnabled(false);
        editHiddenProtein.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editHiddenProteinActionPerformed(evt);
            }
        });

        clearHiddenProtein.setText("Clear");
        clearHiddenProtein.setToolTipText("Remove all hidden proteins filters");
        clearHiddenProtein.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearHiddenProteinActionPerformed(evt);
            }
        });

        deleteHiddenProtein.setText("Delete");
        deleteHiddenProtein.setToolTipText("Delete selected filter");
        deleteHiddenProtein.setEnabled(false);
        deleteHiddenProtein.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteHiddenProteinActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout hiddenProteinsPanelLayout = new javax.swing.GroupLayout(hiddenProteinsPanel);
        hiddenProteinsPanel.setLayout(hiddenProteinsPanelLayout);
        hiddenProteinsPanelLayout.setHorizontalGroup(
            hiddenProteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(hiddenProteinsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(hiddenProteinsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 561, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(hiddenProteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(editHiddenProtein, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(addHiddenProtein, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(deleteHiddenProtein, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(clearHiddenProtein, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );

        hiddenProteinsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {addHiddenProtein, clearHiddenProtein, deleteHiddenProtein, editHiddenProtein});

        hiddenProteinsPanelLayout.setVerticalGroup(
            hiddenProteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(hiddenProteinsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(hiddenProteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(hiddenProteinsScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(hiddenProteinsPanelLayout.createSequentialGroup()
                        .addComponent(addHiddenProtein)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(editHiddenProtein)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deleteHiddenProtein)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearHiddenProtein)
                        .addGap(0, 48, Short.MAX_VALUE)))
                .addContainerGap())
        );

        proteinsSplitPane.setRightComponent(hiddenProteinsPanel);

        tabbedPane.addTab("Proteins", proteinsSplitPane);

        peptidesSplitPane.setBorder(null);
        peptidesSplitPane.setDividerLocation(200);
        peptidesSplitPane.setDividerSize(-1);
        peptidesSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        peptidesSplitPane.setResizeWeight(0.5);
        peptidesSplitPane.setOpaque(false);

        starredPeptidesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Starred Peptides"));
        starredPeptidesPanel.setOpaque(false);
        starredPeptidesPanel.setPreferredSize(new java.awt.Dimension(613, 195));

        starredPeptidesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "", " ", "Name", "Description"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Boolean.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        starredPeptidesTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        starredPeptidesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                starredPeptidesTableMouseReleased(evt);
            }
        });
        starredPeptidesTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                starredPeptidesTableKeyReleased(evt);
            }
        });
        starredPeptidesScrollPane.setViewportView(starredPeptidesTable);

        addStarredPeptides.setText("Add");
        addStarredPeptides.setToolTipText("Add a starred peptides filter");
        addStarredPeptides.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addStarredPeptidesActionPerformed(evt);
            }
        });

        editStarredPeptides.setText("Edit");
        editStarredPeptides.setToolTipText("Edit selected filter");
        editStarredPeptides.setEnabled(false);
        editStarredPeptides.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editStarredPeptidesActionPerformed(evt);
            }
        });

        clearStarredPeptides.setText("Clear");
        clearStarredPeptides.setToolTipText("Remove all starred peptides filters");
        clearStarredPeptides.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearStarredPeptidesActionPerformed(evt);
            }
        });

        deleteStarredPeptides.setText("Delete");
        deleteStarredPeptides.setToolTipText("Delete selected filter");
        deleteStarredPeptides.setEnabled(false);
        deleteStarredPeptides.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteStarredPeptidesActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout starredPeptidesPanelLayout = new javax.swing.GroupLayout(starredPeptidesPanel);
        starredPeptidesPanel.setLayout(starredPeptidesPanelLayout);
        starredPeptidesPanelLayout.setHorizontalGroup(
            starredPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(starredPeptidesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(starredPeptidesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 561, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(starredPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(editStarredPeptides, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(addStarredPeptides, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, starredPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(deleteStarredPeptides, javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(clearStarredPeptides)))
                .addContainerGap())
        );

        starredPeptidesPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {addStarredPeptides, clearStarredPeptides, deleteStarredPeptides, editStarredPeptides});

        starredPeptidesPanelLayout.setVerticalGroup(
            starredPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(starredPeptidesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(starredPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(starredPeptidesPanelLayout.createSequentialGroup()
                        .addComponent(addStarredPeptides)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(editStarredPeptides)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deleteStarredPeptides)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearStarredPeptides)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(starredPeptidesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 155, Short.MAX_VALUE))
                .addContainerGap())
        );

        peptidesSplitPane.setLeftComponent(starredPeptidesPanel);

        hiddenPeptidesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Hidden Peptides"));
        hiddenPeptidesPanel.setOpaque(false);

        hiddenPeptidesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "", " ", "Name", "Description"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Boolean.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        hiddenPeptidesTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        hiddenPeptidesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                hiddenPeptidesTableMouseReleased(evt);
            }
        });
        hiddenPeptidesTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                hiddenPeptidesTableKeyReleased(evt);
            }
        });
        hiddenPeptidesScrollPane.setViewportView(hiddenPeptidesTable);

        addHiddenPeptides.setText("Add");
        addHiddenPeptides.setToolTipText("Add a hidden peptides filter");
        addHiddenPeptides.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addHiddenPeptidesActionPerformed(evt);
            }
        });

        editHiddenPeptides.setText("Edit");
        editHiddenPeptides.setToolTipText("Edit selected filter");
        editHiddenPeptides.setEnabled(false);
        editHiddenPeptides.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editHiddenPeptidesActionPerformed(evt);
            }
        });

        clearHiddenPeptides.setText("Clear");
        clearHiddenPeptides.setToolTipText("Remove all hidden peptides filters");
        clearHiddenPeptides.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearHiddenPeptidesActionPerformed(evt);
            }
        });

        deleteHiddenPeptides.setText("Delete");
        deleteHiddenPeptides.setToolTipText("Delete selected filter");
        deleteHiddenPeptides.setEnabled(false);
        deleteHiddenPeptides.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteHiddenPeptidesActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout hiddenPeptidesPanelLayout = new javax.swing.GroupLayout(hiddenPeptidesPanel);
        hiddenPeptidesPanel.setLayout(hiddenPeptidesPanelLayout);
        hiddenPeptidesPanelLayout.setHorizontalGroup(
            hiddenPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(hiddenPeptidesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(hiddenPeptidesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 561, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(hiddenPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, hiddenPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(editHiddenPeptides, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(addHiddenPeptides, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(clearHiddenPeptides, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(deleteHiddenPeptides, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );

        hiddenPeptidesPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {addHiddenPeptides, clearHiddenPeptides, deleteHiddenPeptides, editHiddenPeptides});

        hiddenPeptidesPanelLayout.setVerticalGroup(
            hiddenPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(hiddenPeptidesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(hiddenPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(hiddenPeptidesPanelLayout.createSequentialGroup()
                        .addComponent(addHiddenPeptides)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(editHiddenPeptides)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deleteHiddenPeptides)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearHiddenPeptides)
                        .addGap(0, 48, Short.MAX_VALUE))
                    .addComponent(hiddenPeptidesScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );

        peptidesSplitPane.setRightComponent(hiddenPeptidesPanel);

        tabbedPane.addTab("Peptides", peptidesSplitPane);

        psmsSplitPane.setBorder(null);
        psmsSplitPane.setDividerLocation(200);
        psmsSplitPane.setDividerSize(-1);
        psmsSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        psmsSplitPane.setResizeWeight(0.5);
        psmsSplitPane.setOpaque(false);

        starredPsmsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Starred PSMs"));
        starredPsmsPanel.setOpaque(false);
        starredPsmsPanel.setPreferredSize(new java.awt.Dimension(613, 195));

        starredPsmTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "", " ", "Name", "Description"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Boolean.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        starredPsmTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        starredPsmTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                starredPsmTableMouseReleased(evt);
            }
        });
        starredPsmTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                starredPsmTableKeyReleased(evt);
            }
        });
        starredPsmsScrollPane.setViewportView(starredPsmTable);

        addStarredPsm.setText("Add");
        addStarredPsm.setToolTipText("Add a starred PSMs filter");
        addStarredPsm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addStarredPsmActionPerformed(evt);
            }
        });

        editStarredPsm.setText("Edit");
        editStarredPsm.setToolTipText("Edit selected filter");
        editStarredPsm.setEnabled(false);
        editStarredPsm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editStarredPsmActionPerformed(evt);
            }
        });

        clearStarredPsm.setText("Clear");
        clearStarredPsm.setToolTipText("Remove all starred PSMs filters");
        clearStarredPsm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearStarredPsmActionPerformed(evt);
            }
        });

        deleteStarredPsm.setText("Delete");
        deleteStarredPsm.setToolTipText("Delete selected filter");
        deleteStarredPsm.setEnabled(false);
        deleteStarredPsm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteStarredPsmActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout starredPsmsPanelLayout = new javax.swing.GroupLayout(starredPsmsPanel);
        starredPsmsPanel.setLayout(starredPsmsPanelLayout);
        starredPsmsPanelLayout.setHorizontalGroup(
            starredPsmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(starredPsmsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(starredPsmsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 561, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(starredPsmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, starredPsmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(editStarredPsm, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(addStarredPsm, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(clearStarredPsm, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(deleteStarredPsm, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );

        starredPsmsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {addStarredPsm, clearStarredPsm, deleteStarredPsm, editStarredPsm});

        starredPsmsPanelLayout.setVerticalGroup(
            starredPsmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(starredPsmsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(starredPsmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(starredPsmsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 155, Short.MAX_VALUE)
                    .addGroup(starredPsmsPanelLayout.createSequentialGroup()
                        .addComponent(addStarredPsm)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(editStarredPsm)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deleteStarredPsm)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearStarredPsm)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        psmsSplitPane.setLeftComponent(starredPsmsPanel);

        hiddenPsmsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Hidden PSMs"));
        hiddenPsmsPanel.setOpaque(false);

        hiddenPsmTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "", " ", "Name", "Description"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Boolean.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        hiddenPsmTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        hiddenPsmTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                hiddenPsmTableMouseReleased(evt);
            }
        });
        hiddenPsmTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                hiddenPsmTableKeyReleased(evt);
            }
        });
        hiddenPsmsScrollPane.setViewportView(hiddenPsmTable);

        addHiddenPsm.setText("Add");
        addHiddenPsm.setToolTipText("Add a hidden PSMs filter");
        addHiddenPsm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addHiddenPsmActionPerformed(evt);
            }
        });

        editHiddenPsm.setText("Edit");
        editHiddenPsm.setToolTipText("Edit selected filter");
        editHiddenPsm.setEnabled(false);
        editHiddenPsm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editHiddenPsmActionPerformed(evt);
            }
        });

        clearHiddenPsm.setText("Clear");
        clearHiddenPsm.setToolTipText("Remove all hidden PSMs filters");
        clearHiddenPsm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearHiddenPsmActionPerformed(evt);
            }
        });

        deleteHiddenPsm.setText("Delete");
        deleteHiddenPsm.setToolTipText("Delete selected filter");
        deleteHiddenPsm.setEnabled(false);
        deleteHiddenPsm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteHiddenPsmActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout hiddenPsmsPanelLayout = new javax.swing.GroupLayout(hiddenPsmsPanel);
        hiddenPsmsPanel.setLayout(hiddenPsmsPanelLayout);
        hiddenPsmsPanelLayout.setHorizontalGroup(
            hiddenPsmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(hiddenPsmsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(hiddenPsmsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 561, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(hiddenPsmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, hiddenPsmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(editHiddenPsm, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(addHiddenPsm, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(clearHiddenPsm, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(deleteHiddenPsm, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );

        hiddenPsmsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {addHiddenPsm, clearHiddenPsm, deleteHiddenPsm, editHiddenPsm});

        hiddenPsmsPanelLayout.setVerticalGroup(
            hiddenPsmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(hiddenPsmsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(hiddenPsmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(hiddenPsmsPanelLayout.createSequentialGroup()
                        .addComponent(addHiddenPsm)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(editHiddenPsm)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deleteHiddenPsm)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearHiddenPsm)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(hiddenPsmsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 158, Short.MAX_VALUE))
                .addContainerGap())
        );

        psmsSplitPane.setRightComponent(hiddenPsmsPanel);

        tabbedPane.addTab("PSMs", psmsSplitPane);

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        okButton.setText("OK");
        okButton.setPreferredSize(new java.awt.Dimension(65, 23));
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        openDialogHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help.GIF"))); // NOI18N
        openDialogHelpJButton.setToolTipText("Help");
        openDialogHelpJButton.setBorder(null);
        openDialogHelpJButton.setBorderPainted(false);
        openDialogHelpJButton.setContentAreaFilled(false);
        openDialogHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                openDialogHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                openDialogHelpJButtonMouseExited(evt);
            }
        });
        openDialogHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openDialogHelpJButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(openDialogHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton))
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(tabbedPane)))
                .addContainerGap())
        );

        backgroundPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 430, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(openDialogHelpJButton)
                    .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cancelButton))
                .addContainerGap())
        );

        backgroundPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {cancelButton, okButton});

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
     * Close the dialog.
     *
     * @param evt the action event
     */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        this.dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    /**
     * Save the filter settings and update the filters.
     *
     * @param evt the action event
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        peptideShakerGUI.getFilterParameters().setProteinStarFilters(proteinStarFilters);
        peptideShakerGUI.getFilterParameters().setProteinHideFilters(proteinHideFilters);
        peptideShakerGUI.getFilterParameters().setPeptideStarFilters(peptideStarFilters);
        peptideShakerGUI.getFilterParameters().setPeptideHideFilters(peptideHideFilters);
        peptideShakerGUI.getFilterParameters().setPsmStarFilters(psmStarFilters);
        peptideShakerGUI.getFilterParameters().setPsmHideFilters(psmHideFilters);

        setVisible(false);

        peptideShakerGUI.resetSelectedItems();
        peptideShakerGUI.setUpdated(PeptideShakerGUI.OVER_VIEW_TAB_INDEX, false);
        peptideShakerGUI.setUpdated(PeptideShakerGUI.PROTEIN_FRACTIONS_TAB_INDEX, false);
        peptideShakerGUI.setUpdated(PeptideShakerGUI.MODIFICATIONS_TAB_INDEX, false);
        peptideShakerGUI.setUpdated(PeptideShakerGUI.STRUCTURES_TAB_INDEX, false);
        peptideShakerGUI.setUpdated(PeptideShakerGUI.GO_ANALYSIS_TAB_INDEX, false);
        peptideShakerGUI.setUpdated(PeptideShakerGUI.QC_PLOTS_TAB_INDEX, false);
        peptideShakerGUI.setUpdated(PeptideShakerGUI.PROTEIN_FRACTIONS_TAB_INDEX, false);

        peptideShakerGUI.getStarHider().starHide();
        peptideShakerGUI.updateTabbedPanes();

        dispose();
    }//GEN-LAST:event_okButtonActionPerformed

    /**
     * Update the starred proteins table.
     *
     * @param evt the key event
     */
    private void starredProteinsTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_starredProteinsTableKeyReleased
        int column = starredProteinsTable.getSelectedColumn();
        int row = starredProteinsTable.getSelectedRow();

        if (column == 2) {
            String newName = (String) starredProteinsTable.getValueAt(row, column);
            ArrayList<String> others = new ArrayList<>();
            for (int i = 0; i < starredProteinsTable.getRowCount(); i++) {
                if (i != row) {
                    others.add((String) starredProteinsTable.getValueAt(i, column));
                }
            }
            if (others.contains(newName)) {
                int outcome = JOptionPane.YES_OPTION;
                if (peptideShakerGUI.getFilterParameters().filterExists(newName)) {
                    outcome = JOptionPane.showConfirmDialog(this,
                            "Should protein filter " + newName + " be overwritten?", "Selected Name Already Exists",
                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                }
                if (outcome != JOptionPane.YES_OPTION) {
                    for (ProteinFilter proteinFilter : proteinStarFilters.values()) {
                        if (!others.contains(proteinFilter.getName())) {
                            starredProteinsTable.setValueAt(proteinFilter.getName(), row, column);
                        }
                    }
                    return;
                }
            }

            // has to be done like this on order to avoid a ConcurrentModificationException
            final Collection<ProteinFilter> values = proteinStarFilters.values();
            Iterator<ProteinFilter> iterator = values.iterator();

            while (iterator.hasNext()) {
                ProteinFilter proteinFilter = iterator.next();

                if (!others.contains(proteinFilter.getName())) {
                    String oldName = proteinFilter.getName();
                    proteinFilter.setName(newName);
                    proteinStarFilters.remove(oldName);
                    proteinStarFilters.put(newName, proteinFilter);
                }
            }
        }
        if (column == 3) {
            String name = (String) starredProteinsTable.getValueAt(row, 2);
            proteinStarFilters.get(name).setDescription((String) starredProteinsTable.getValueAt(row, column));
        }

        if (starredProteinsTable.isEditing()) {
            editStarredProtein.setEnabled(false);
            deleteStarredProtein.setEnabled(false);
        } else if (starredProteinsTable.getSelectedRow() != -1) {
            editStarredProtein.setEnabled(true);
            deleteStarredProtein.setEnabled(true);
        }
    }//GEN-LAST:event_starredProteinsTableKeyReleased

    /**
     * Update the starred proteins table.
     *
     * @param evt the mouse event
     */
    private void starredProteinsTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_starredProteinsTableMouseReleased

        int column = starredProteinsTable.getSelectedColumn();
        int row = starredProteinsTable.getSelectedRow();
        if (row != -1 && evt.getButton() == MouseEvent.BUTTON1) {
            String key = (String) starredProteinsTable.getValueAt(row, 2);
            MatchFilter matchFilter = proteinStarFilters.get(key);
            if (evt.getClickCount() == 1) {
                if (column == 1) {
                    matchFilter.setActive(!matchFilter.isActive());
                }
            }
        }

        if (starredProteinsTable.isEditing()) {
            editStarredProtein.setEnabled(false);
            deleteStarredProtein.setEnabled(false);
        } else if (starredProteinsTable.getSelectedRow() != -1) {
            editStarredProtein.setEnabled(true);
            deleteStarredProtein.setEnabled(true);
        }

    }//GEN-LAST:event_starredProteinsTableMouseReleased

    /**
     * Update the starred peptides table.
     *
     * @param evt the mouse event
     */
    private void starredPeptidesTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_starredPeptidesTableMouseReleased
        int column = starredPeptidesTable.getSelectedColumn();
        int row = starredPeptidesTable.getSelectedRow();
        if (row != -1 && evt.getButton() == MouseEvent.BUTTON1) {
            String key = (String) starredPeptidesTable.getValueAt(row, 2);
            MatchFilter matchFilter = peptideStarFilters.get(key);
            if (evt.getClickCount() == 1) {
                if (column == 1) {
                    matchFilter.setActive(!matchFilter.isActive());
                }
            } else if (evt.getClickCount() == 2) {
                if (column != 2) {
                    //@TODO edit matchFilter
                }
            }
        }

        if (starredPeptidesTable.isEditing()) {
            editStarredPeptides.setEnabled(false);
            deleteStarredPeptides.setEnabled(false);
        } else if (starredPeptidesTable.getSelectedRow() != -1) {
            editStarredPeptides.setEnabled(true);
            deleteStarredPeptides.setEnabled(true);
        }
    }//GEN-LAST:event_starredPeptidesTableMouseReleased

    /**
     * Update the starred PSMs table.
     *
     * @param evt the mouse event
     */
    private void starredPsmTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_starredPsmTableMouseReleased
        int column = starredPsmTable.getSelectedColumn();
        int row = starredPsmTable.getSelectedRow();
        if (row != -1 && evt.getButton() == MouseEvent.BUTTON1) {
            String key = (String) starredPsmTable.getValueAt(row, 2);
            MatchFilter matchFilter = psmStarFilters.get(key);
            if (evt.getClickCount() == 1) {
                if (column == 1) {
                    matchFilter.setActive(!matchFilter.isActive());
                }
            } else if (evt.getClickCount() == 2) {
                if (column != 2) {
                    //@TODO edit matchFilter
                }
            }
        }

        if (starredPsmTable.isEditing()) {
            editStarredPsm.setEnabled(false);
            deleteStarredPsm.setEnabled(false);
        } else if (starredPsmTable.getSelectedRow() != -1) {
            editStarredPsm.setEnabled(true);
            deleteStarredPsm.setEnabled(true);
        }
    }//GEN-LAST:event_starredPsmTableMouseReleased

    /**
     * Update the hidden proteins table.
     *
     * @param evt the mouse event
     */
    private void hiddenProteinsTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_hiddenProteinsTableMouseReleased
        int column = hiddenProteinsTable.getSelectedColumn();
        int row = hiddenProteinsTable.getSelectedRow();
        if (row != -1 && evt.getButton() == MouseEvent.BUTTON1) {
            String key = (String) hiddenProteinsTable.getValueAt(row, 2);
            MatchFilter matchFilter = proteinHideFilters.get(key);
            if (evt.getClickCount() == 1) {
                if (column == 1) {
                    matchFilter.setActive(!matchFilter.isActive());
                }
            } else if (evt.getClickCount() == 2) {
                if (column != 2) {
                    //@TODO edit matchFilter
                }
            }
        }

        if (hiddenProteinsTable.isEditing()) {
            editHiddenProtein.setEnabled(false);
            deleteHiddenProtein.setEnabled(false);
        } else if (hiddenProteinsTable.getSelectedRow() != -1) {
            editHiddenProtein.setEnabled(true);
            deleteHiddenProtein.setEnabled(true);
        }
    }//GEN-LAST:event_hiddenProteinsTableMouseReleased

    /**
     * Update the hidden peptides table.
     *
     * @param evt the mouse event
     */
    private void hiddenPeptidesTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_hiddenPeptidesTableMouseReleased
        int column = hiddenPeptidesTable.getSelectedColumn();
        int row = hiddenPeptidesTable.getSelectedRow();
        if (row != -1 && evt.getButton() == MouseEvent.BUTTON1) {
            String key = (String) hiddenPeptidesTable.getValueAt(row, 2);
            MatchFilter matchFilter = peptideHideFilters.get(key);
            if (evt.getClickCount() == 1) {
                if (column == 1) {
                    matchFilter.setActive(!matchFilter.isActive());
                }
            } else if (evt.getClickCount() == 2) {
                if (column != 2) {
                    //@TODO edit matchFilter
                }
            }
        }

        if (hiddenPeptidesTable.isEditing()) {
            editHiddenPeptides.setEnabled(false);
            deleteHiddenPeptides.setEnabled(false);
        } else if (hiddenPeptidesTable.getSelectedRow() != -1) {
            editHiddenPeptides.setEnabled(true);
            deleteHiddenPeptides.setEnabled(true);
        }
    }//GEN-LAST:event_hiddenPeptidesTableMouseReleased

    /**
     * Update the hidden PSMs table.
     *
     * @param evt the mouse event
     */
    private void hiddenPsmTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_hiddenPsmTableMouseReleased
        int column = hiddenPsmTable.getSelectedColumn();
        int row = hiddenPsmTable.getSelectedRow();
        if (row != -1 && evt.getButton() == MouseEvent.BUTTON1) {
            String key = (String) hiddenPsmTable.getValueAt(row, 2);
            MatchFilter matchFilter = psmHideFilters.get(key);
            if (evt.getClickCount() == 1) {
                if (column == 1) {
                    matchFilter.setActive(!matchFilter.isActive());
                }
            } else if (evt.getClickCount() == 2) {
                if (column != 2) {
                    //@TODO edit matchFilter
                }
            }
        }

        if (hiddenPsmTable.isEditing()) {
            editHiddenPsm.setEnabled(false);
            deleteHiddenPsm.setEnabled(false);
        } else if (hiddenPsmTable.getSelectedRow() != -1) {
            editHiddenPsm.setEnabled(true);
            deleteHiddenPsm.setEnabled(true);
        }
    }//GEN-LAST:event_hiddenPsmTableMouseReleased

    /**
     * Open the FindDialog to add/edit a filter.
     *
     * @param evt the action event
     */
    private void addStarredPsmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addStarredPsmActionPerformed
        PsmFilter newFilter = (PsmFilter) peptideShakerGUI.createPsmFilter();
        if (newFilter != null) {
            String filterName = newFilter.getName();
            if (psmStarFilters.containsKey(filterName)) {
                int value = JOptionPane.showConfirmDialog(this, "A filter named " + filterName + " already exists. Overwrite?", "Overwrite Filter?", JOptionPane.YES_NO_OPTION);
                if (value == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            psmStarFilters.put(filterName, newFilter);
            updateTables();

        }
    }//GEN-LAST:event_addStarredPsmActionPerformed

    /**
     * Open the FindDialog to add/edit a filter.
     *
     * @param evt the action event
     */
    private void addStarredProteinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addStarredProteinActionPerformed
        ProteinFilter newFilter = (ProteinFilter) peptideShakerGUI.createProteinFilter();
        if (newFilter != null) {
            String filterName = newFilter.getName();
            if (proteinStarFilters.containsKey(filterName)) {
                int value = JOptionPane.showConfirmDialog(this, "A filter named " + filterName + " already exists. Overwrite?", "Overwrite Filter?", JOptionPane.YES_NO_OPTION);
                if (value == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            proteinStarFilters.put(filterName, newFilter);
            updateTables();
        }
    }//GEN-LAST:event_addStarredProteinActionPerformed

    /**
     * Open the FindDialog to add/edit a filter.
     *
     * @param evt the action event
     */
    private void addHiddenProteinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addHiddenProteinActionPerformed
        ProteinFilter newFilter = (ProteinFilter) peptideShakerGUI.createProteinFilter();
        if (newFilter != null) {
            String filterName = newFilter.getName();
            if (proteinHideFilters.containsKey(filterName)) {
                int value = JOptionPane.showConfirmDialog(this, "A filter named " + filterName + " already exists. Overwrite?", "Overwrite Filter?", JOptionPane.YES_NO_OPTION);
                if (value == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            proteinHideFilters.put(filterName, newFilter);
            updateTables();
        }
    }//GEN-LAST:event_addHiddenProteinActionPerformed

    /**
     * Open the FindDialog to add/edit a filter.
     *
     * @param evt the action event
     */
    private void addStarredPeptidesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addStarredPeptidesActionPerformed
        PeptideFilter newFilter = (PeptideFilter) peptideShakerGUI.createPeptideFilter();
        if (newFilter != null) {
            String filterName = newFilter.getName();
            if (peptideStarFilters.containsKey(filterName)) {
                int value = JOptionPane.showConfirmDialog(this, "A filter named " + filterName + " already exists. Overwrite?", "Overwrite Filter?", JOptionPane.YES_NO_OPTION);
                if (value == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            peptideStarFilters.put(filterName, newFilter);
            updateTables();
        }
    }//GEN-LAST:event_addStarredPeptidesActionPerformed

    /**
     * Open the FindDialog to add/edit a filter.
     *
     * @param evt the action event
     */
    private void addHiddenPeptidesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addHiddenPeptidesActionPerformed
        PeptideFilter newFilter = (PeptideFilter) peptideShakerGUI.createPeptideFilter();
        if (newFilter != null) {
            String filterName = newFilter.getName();
            if (peptideHideFilters.containsKey(filterName)) {
                int value = JOptionPane.showConfirmDialog(this, "A filter named " + filterName + " already exists. Overwrite?", "Overwrite Filter?", JOptionPane.YES_NO_OPTION);
                if (value == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            peptideHideFilters.put(filterName, newFilter);
            updateTables();
        }
    }//GEN-LAST:event_addHiddenPeptidesActionPerformed

    /**
     * Open the FindDialog to add/edit a filter.
     *
     * @param evt the action event
     */
    private void addHiddenPsmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addHiddenPsmActionPerformed
        PsmFilter newFilter = (PsmFilter) peptideShakerGUI.createPsmFilter();
        if (newFilter != null) {
            String filterName = newFilter.getName();
            if (psmHideFilters.containsKey(filterName)) {
                int value = JOptionPane.showConfirmDialog(this, "A filter named " + filterName + " already exists. Overwrite?", "Overwrite Filter?", JOptionPane.YES_NO_OPTION);
                if (value == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            psmHideFilters.put(filterName, newFilter);
            updateTables();
        }
    }//GEN-LAST:event_addHiddenPsmActionPerformed

    /**
     * Clear the starred protein filters.
     *
     * @param evt the action event
     */
    private void clearStarredProteinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearStarredProteinActionPerformed
        proteinStarFilters.clear();
        updateTables();
        editStarredProtein.setEnabled(false);
        deleteStarredProtein.setEnabled(false);
    }//GEN-LAST:event_clearStarredProteinActionPerformed

    /**
     * Clear the hidden protein filters.
     *
     * @param evt the action event
     */
    private void clearHiddenProteinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearHiddenProteinActionPerformed
        proteinHideFilters.clear();
        updateTables();
        editHiddenProtein.setEnabled(false);
        deleteHiddenProtein.setEnabled(false);
    }//GEN-LAST:event_clearHiddenProteinActionPerformed

    /**
     * Clear the starred peptides filters.
     *
     * @param evt the action event
     */
    private void clearStarredPeptidesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearStarredPeptidesActionPerformed
        peptideStarFilters.clear();
        updateTables();
        editStarredPeptides.setEnabled(false);
        deleteStarredPeptides.setEnabled(false);
    }//GEN-LAST:event_clearStarredPeptidesActionPerformed

    /**
     * Clear the hidden peptides filters.
     *
     * @param evt the action event
     */
    private void clearHiddenPeptidesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearHiddenPeptidesActionPerformed
        peptideHideFilters.clear();
        updateTables();
        editHiddenPeptides.setEnabled(false);
        deleteHiddenPeptides.setEnabled(false);
    }//GEN-LAST:event_clearHiddenPeptidesActionPerformed

    /**
     * Clear the starred PSMs filters.
     *
     * @param evt the action event
     */
    private void clearStarredPsmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearStarredPsmActionPerformed
        psmStarFilters.clear();
        updateTables();
        editStarredPsm.setEnabled(false);
        deleteStarredPsm.setEnabled(false);
    }//GEN-LAST:event_clearStarredPsmActionPerformed

    /**
     * Clear the hidden PSMs filters.
     *
     * @param evt the action event
     */
    private void clearHiddenPsmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearHiddenPsmActionPerformed
        psmHideFilters.clear();
        updateTables();
        editHiddenPsm.setEnabled(false);
        deleteHiddenPsm.setEnabled(false);
    }//GEN-LAST:event_clearHiddenPsmActionPerformed

    /**
     * Edit a starred protein.
     *
     * @param evt the action event
     */
    private void editStarredProteinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editStarredProteinActionPerformed
        int row = starredProteinsTable.getSelectedRow();
        if (row >= 0) {
            String selectedFilterName = (String) starredProteinsTable.getValueAt(row, 2);
            ProteinFilter proteinFilter = proteinStarFilters.get(selectedFilterName);
            peptideShakerGUI.editFilter(proteinFilter);
            updateTables();
        }
    }//GEN-LAST:event_editStarredProteinActionPerformed

    /**
     * Edit a hidden protein.
     *
     * @param evt the action event
     */
    private void editHiddenProteinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editHiddenProteinActionPerformed
        int row = hiddenProteinsTable.getSelectedRow();
        if (row >= 0) {
            String selectedFilterName = (String) hiddenProteinsTable.getValueAt(row, 2);
            ProteinFilter proteinFilter = proteinHideFilters.get(selectedFilterName);
            peptideShakerGUI.editFilter(proteinFilter);
            updateTables();
        }
    }//GEN-LAST:event_editHiddenProteinActionPerformed

    /**
     * Edit a starred peptide.
     *
     * @param evt the action event
     */
    private void editStarredPeptidesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editStarredPeptidesActionPerformed
        int row = starredPeptidesTable.getSelectedRow();
        if (row >= 0) {
            String selectedFilterName = (String) starredPeptidesTable.getValueAt(row, 2);
            PeptideFilter peptideFilter = peptideStarFilters.get(selectedFilterName);
            peptideShakerGUI.editFilter(peptideFilter);
            updateTables();
        }
    }//GEN-LAST:event_editStarredPeptidesActionPerformed

    /**
     * Edit a hidden peptide.
     *
     * @param evt the action event
     */
    private void editHiddenPeptidesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editHiddenPeptidesActionPerformed
        int row = hiddenPeptidesTable.getSelectedRow();
        if (row >= 0) {
            String selectedFilterName = (String) hiddenPeptidesTable.getValueAt(row, 2);
            PeptideFilter peptideFilter = peptideHideFilters.get(selectedFilterName);
            peptideShakerGUI.editFilter(peptideFilter);
            updateTables();
        }
    }//GEN-LAST:event_editHiddenPeptidesActionPerformed

    /**
     * Edit a starred PSM.
     *
     * @param evt the action event
     */
    private void editStarredPsmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editStarredPsmActionPerformed
        int row = starredPsmTable.getSelectedRow();
        if (row >= 0) {
            String selectedFilterName = (String) starredPsmTable.getValueAt(row, 2);
            PsmFilter psmFilter = psmStarFilters.get(selectedFilterName);
            peptideShakerGUI.editFilter(psmFilter);
            updateTables();
        }
    }//GEN-LAST:event_editStarredPsmActionPerformed

    /**
     * Edit a hidden protein.
     *
     * @param evt the action event
     */
    private void editHiddenPsmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editHiddenPsmActionPerformed
        int row = hiddenPsmTable.getSelectedRow();
        if (row >= 0) {
            String selectedFilterName = (String) hiddenPsmTable.getValueAt(row, 2);
            PsmFilter psmFilter = psmHideFilters.get(selectedFilterName);
            peptideShakerGUI.editFilter(psmFilter);
            updateTables();
        }
    }//GEN-LAST:event_editHiddenPsmActionPerformed

    /**
     * Update the hidden proteins table.
     *
     * @param evt the key event
     */
    private void hiddenProteinsTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_hiddenProteinsTableKeyReleased

        int column = hiddenProteinsTable.getSelectedColumn();
        int row = hiddenProteinsTable.getSelectedRow();

        if (column == 2) {
            String newName = (String) hiddenProteinsTable.getValueAt(row, column);
            ArrayList<String> others = new ArrayList<>();
            for (int i = 0; i < hiddenProteinsTable.getRowCount(); i++) {
                if (i != row) {
                    others.add((String) hiddenProteinsTable.getValueAt(i, column));
                }
            }
            if (others.contains(newName)) {
                int outcome = JOptionPane.YES_OPTION;
                if (peptideShakerGUI.getFilterParameters().filterExists(newName)) {
                    outcome = JOptionPane.showConfirmDialog(this,
                            "Should protein filter " + newName + " be overwritten?", "Selected Name Already Exists",
                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                }
                if (outcome != JOptionPane.YES_OPTION) {
                    for (ProteinFilter proteinFilter : proteinHideFilters.values()) {
                        if (!others.contains(proteinFilter.getName())) {
                            hiddenProteinsTable.setValueAt(proteinFilter.getName(), row, column);
                        }
                    }
                    return;
                }
            }
            for (ProteinFilter proteinFilter : proteinHideFilters.values()) {
                if (!others.contains(proteinFilter.getName())) {
                    String oldName = proteinFilter.getName();
                    proteinFilter.setName(newName);
                    proteinHideFilters.remove(oldName);
                    proteinHideFilters.put(newName, proteinFilter);
                }
            }
        }
        if (column == 3) {
            String name = (String) hiddenProteinsTable.getValueAt(row, 2);
            proteinHideFilters.get(name).setDescription((String) hiddenProteinsTable.getValueAt(row, column));
        }

        if (hiddenProteinsTable.isEditing()) {
            editHiddenProtein.setEnabled(false);
            deleteHiddenProtein.setEnabled(false);
        } else if (hiddenProteinsTable.getSelectedRow() != -1) {
            editHiddenProtein.setEnabled(true);
            deleteHiddenProtein.setEnabled(true);
        }
    }//GEN-LAST:event_hiddenProteinsTableKeyReleased

    /**
     * Update the starred peptides table.
     *
     * @param evt the key event
     */
    private void starredPeptidesTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_starredPeptidesTableKeyReleased
        int column = starredPeptidesTable.getSelectedColumn();
        int row = starredPeptidesTable.getSelectedRow();

        if (column == 2) {
            String newName = (String) starredPeptidesTable.getValueAt(row, column);
            ArrayList<String> others = new ArrayList<>();
            for (int i = 0; i < starredPeptidesTable.getRowCount(); i++) {
                if (i != row) {
                    others.add((String) starredPeptidesTable.getValueAt(i, column));
                }
            }
            if (others.contains(newName)) {
                int outcome = JOptionPane.YES_OPTION;
                if (peptideShakerGUI.getFilterParameters().filterExists(newName)) {
                    outcome = JOptionPane.showConfirmDialog(this,
                            "Should peptide filter " + newName + " be overwritten?", "Selected Name Already Exists",
                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                }
                if (outcome != JOptionPane.YES_OPTION) {
                    for (PeptideFilter peptideFilter : peptideStarFilters.values()) {
                        if (!others.contains(peptideFilter.getName())) {
                            starredPeptidesTable.setValueAt(peptideFilter.getName(), row, column);
                        }
                    }
                    return;
                }
            }
            for (PeptideFilter peptideFilter : peptideStarFilters.values()) {
                if (!others.contains(peptideFilter.getName())) {
                    String oldName = peptideFilter.getName();
                    peptideFilter.setName(newName);
                    peptideStarFilters.remove(oldName);
                    peptideStarFilters.put(newName, peptideFilter);
                }
            }
        }
        if (column == 3) {
            String name = (String) starredPeptidesTable.getValueAt(row, 2);
            peptideStarFilters.get(name).setDescription((String) starredPeptidesTable.getValueAt(row, column));
        }

        if (starredPeptidesTable.isEditing()) {
            editStarredPeptides.setEnabled(false);
            deleteStarredPeptides.setEnabled(false);
        } else if (starredPeptidesTable.getSelectedRow() != -1) {
            editStarredPeptides.setEnabled(true);
            deleteStarredPeptides.setEnabled(true);
        }
    }//GEN-LAST:event_starredPeptidesTableKeyReleased

    /**
     * Update the hidden peptides table.
     *
     * @param evt the key event
     */
    private void hiddenPeptidesTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_hiddenPeptidesTableKeyReleased
        int column = hiddenPeptidesTable.getSelectedColumn();
        int row = hiddenPeptidesTable.getSelectedRow();

        if (column == 2) {
            String newName = (String) hiddenPeptidesTable.getValueAt(row, column);
            ArrayList<String> others = new ArrayList<>();
            for (int i = 0; i < hiddenPeptidesTable.getRowCount(); i++) {
                if (i != row) {
                    others.add((String) hiddenPeptidesTable.getValueAt(i, column));
                }
            }
            if (others.contains(newName)) {
                int outcome = JOptionPane.YES_OPTION;
                if (peptideShakerGUI.getFilterParameters().filterExists(newName)) {
                    outcome = JOptionPane.showConfirmDialog(this,
                            "Should peptide filter " + newName + " be overwritten?", "Selected Name Already Exists",
                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                }
                if (outcome != JOptionPane.YES_OPTION) {
                    for (PeptideFilter peptideFilter : peptideHideFilters.values()) {
                        if (!others.contains(peptideFilter.getName())) {
                            hiddenPeptidesTable.setValueAt(peptideFilter.getName(), row, column);
                        }
                    }
                    return;
                }
            }
            for (PeptideFilter peptideFilter : peptideHideFilters.values()) {
                if (!others.contains(peptideFilter.getName())) {
                    String oldName = peptideFilter.getName();
                    peptideFilter.setName(newName);
                    peptideHideFilters.remove(oldName);
                    peptideHideFilters.put(newName, peptideFilter);
                }
            }
        }
        if (column == 3) {
            String name = (String) hiddenPeptidesTable.getValueAt(row, 2);
            peptideHideFilters.get(name).setDescription((String) hiddenPeptidesTable.getValueAt(row, column));
        }

        if (hiddenPeptidesTable.isEditing()) {
            editHiddenPeptides.setEnabled(false);
            deleteHiddenPeptides.setEnabled(false);
        } else if (hiddenPeptidesTable.getSelectedRow() != -1) {
            editHiddenPeptides.setEnabled(true);
            deleteHiddenPeptides.setEnabled(true);
        }
    }//GEN-LAST:event_hiddenPeptidesTableKeyReleased

    /**
     * Update the starred PSMs table.
     *
     * @param evt the key event
     */
    private void starredPsmTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_starredPsmTableKeyReleased
        int column = starredPsmTable.getSelectedColumn();
        int row = starredPsmTable.getSelectedRow();

        if (column == 2) {
            String newName = (String) starredPsmTable.getValueAt(row, column);
            ArrayList<String> others = new ArrayList<>();
            for (int i = 0; i < starredPsmTable.getRowCount(); i++) {
                if (i != row) {
                    others.add((String) starredPsmTable.getValueAt(i, column));
                }
            }
            if (others.contains(newName)) {
                int outcome = JOptionPane.YES_OPTION;
                if (peptideShakerGUI.getFilterParameters().filterExists(newName)) {
                    outcome = JOptionPane.showConfirmDialog(this,
                            "Should psm filter " + newName + " be overwritten?", "Selected Name Already Exists",
                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                }
                if (outcome != JOptionPane.YES_OPTION) {
                    for (PsmFilter psmFilter : psmStarFilters.values()) {
                        if (!others.contains(psmFilter.getName())) {
                            starredPsmTable.setValueAt(psmFilter.getName(), row, column);
                        }
                    }
                    return;
                }
            }
            for (PsmFilter psmFilter : psmStarFilters.values()) {
                if (!others.contains(psmFilter.getName())) {
                    String oldName = psmFilter.getName();
                    psmFilter.setName(newName);
                    psmStarFilters.remove(oldName);
                    psmStarFilters.put(newName, psmFilter);
                }
            }
        }
        if (column == 3) {
            String name = (String) starredPsmTable.getValueAt(row, 2);
            psmStarFilters.get(name).setDescription((String) starredPsmTable.getValueAt(row, column));
        }

        if (starredPsmTable.isEditing()) {
            editStarredPsm.setEnabled(false);
            deleteStarredPsm.setEnabled(false);
        } else if (starredPsmTable.getSelectedRow() != -1) {
            editStarredPsm.setEnabled(true);
            deleteStarredPsm.setEnabled(true);
        }
    }//GEN-LAST:event_starredPsmTableKeyReleased

    /**
     * Update the hidden PSMs table.
     *
     * @param evt the key event
     */
    private void hiddenPsmTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_hiddenPsmTableKeyReleased
        int column = hiddenPsmTable.getSelectedColumn();
        int row = hiddenPsmTable.getSelectedRow();

        if (column == 2) {
            String newName = (String) hiddenPsmTable.getValueAt(row, column);
            ArrayList<String> others = new ArrayList<>();
            for (int i = 0; i < hiddenPsmTable.getRowCount(); i++) {
                if (i != row) {
                    others.add((String) hiddenPsmTable.getValueAt(i, column));
                }
            }
            if (others.contains(newName)) {
                int outcome = JOptionPane.YES_OPTION;
                if (peptideShakerGUI.getFilterParameters().filterExists(newName)) {
                    outcome = JOptionPane.showConfirmDialog(this,
                            "Should psm filter " + newName + " be overwritten?", "Selected Name Already Exists",
                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                }
                if (outcome != JOptionPane.YES_OPTION) {
                    for (PsmFilter psmFilter : psmHideFilters.values()) {
                        if (!others.contains(psmFilter.getName())) {
                            hiddenPsmTable.setValueAt(psmFilter.getName(), row, column);
                        }
                    }
                    return;
                }
            }
            for (PsmFilter psmFilter : psmHideFilters.values()) {
                if (!others.contains(psmFilter.getName())) {
                    String oldName = psmFilter.getName();
                    psmFilter.setName(newName);
                    psmHideFilters.remove(oldName);
                    psmHideFilters.put(newName, psmFilter);
                }
            }
        }
        if (column == 3) {
            String name = (String) hiddenPsmTable.getValueAt(row, 2);
            psmHideFilters.get(name).setDescription((String) hiddenPsmTable.getValueAt(row, column));
        }

        if (row != -1) {
            editHiddenPsm.setEnabled(true);
            deleteHiddenPsm.setEnabled(true);
        }

        if (hiddenPsmTable.isEditing()) {
            editHiddenPsm.setEnabled(false);
            deleteHiddenPsm.setEnabled(false);
        } else if (hiddenPsmTable.getSelectedRow() != -1) {
            editHiddenPsm.setEnabled(true);
            deleteHiddenPsm.setEnabled(true);
        }
    }//GEN-LAST:event_hiddenPsmTableKeyReleased

    /**
     * Change the cursor icon to a hand icon.
     *
     * @param evt the mouse event
     */
    private void openDialogHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_openDialogHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_openDialogHelpJButtonMouseEntered

    /**
     * Change the cursor icon to the default icon.
     *
     * @param evt the mouse event
     */
    private void openDialogHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_openDialogHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_openDialogHelpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt the action event
     */
    private void openDialogHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openDialogHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/FilterSelection.html"),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "Filter Selection - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_openDialogHelpJButtonActionPerformed

    /**
     * Make sure that the split pane dividers all have the same location.
     *
     * @param evt the component event
     */
    private void tabbedPaneComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_tabbedPaneComponentResized
        proteinsSplitPane.setDividerLocation(0.5);
        peptidesSplitPane.setDividerLocation(0.5);
        psmsSplitPane.setDividerLocation(0.5);
    }//GEN-LAST:event_tabbedPaneComponentResized

    /**
     * Delete the currently selected starred proteins filter.
     *
     * @param evt the action event
     */
    private void deleteStarredProteinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteStarredProteinActionPerformed
        int row = starredProteinsTable.getSelectedRow();
        if (row >= 0) {
            String selectedFilterName = (String) starredProteinsTable.getValueAt(row, 2);
            proteinStarFilters.remove(selectedFilterName);
            emptyTables();
            fillTables();
        }
    }//GEN-LAST:event_deleteStarredProteinActionPerformed

    /**
     * Delete the currently selected hidden proteins filter.
     *
     * @param evt the action event
     */
    private void deleteHiddenProteinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteHiddenProteinActionPerformed
        int row = hiddenProteinsTable.getSelectedRow();
        if (row >= 0) {
            String selectedFilterName = (String) hiddenProteinsTable.getValueAt(row, 2);
            proteinHideFilters.remove(selectedFilterName);
            emptyTables();
            fillTables();
        }
    }//GEN-LAST:event_deleteHiddenProteinActionPerformed

    /**
     * Delete the currently selected starred peptides filter.
     *
     * @param evt the action event
     */
    private void deleteStarredPeptidesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteStarredPeptidesActionPerformed
        int row = starredPeptidesTable.getSelectedRow();
        if (row >= 0) {
            String selectedFilterName = (String) starredPeptidesTable.getValueAt(row, 2);
            peptideStarFilters.remove(selectedFilterName);
            emptyTables();
            fillTables();
        }
    }//GEN-LAST:event_deleteStarredPeptidesActionPerformed

    /**
     * Delete the currently selected hidden peptides filter.
     *
     * @param evt the action event
     */
    private void deleteHiddenPeptidesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteHiddenPeptidesActionPerformed
        int row = hiddenPeptidesTable.getSelectedRow();
        if (row >= 0) {
            String selectedFilterName = (String) hiddenPeptidesTable.getValueAt(row, 2);
            peptideHideFilters.remove(selectedFilterName);
            emptyTables();
            fillTables();
        }
    }//GEN-LAST:event_deleteHiddenPeptidesActionPerformed

    /**
     * Delete the currently selected starred PSMs filter.
     *
     * @param evt the action event
     */
    private void deleteStarredPsmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteStarredPsmActionPerformed
        int row = starredPsmTable.getSelectedRow();
        if (row >= 0) {
            String selectedFilterName = (String) starredPsmTable.getValueAt(row, 2);
            psmStarFilters.remove(selectedFilterName);
            emptyTables();
            fillTables();
        }
    }//GEN-LAST:event_deleteStarredPsmActionPerformed

    /**
     * Delete the currently selected hidden PSMs filter.
     *
     * @param evt the action event
     */
    private void deleteHiddenPsmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteHiddenPsmActionPerformed
        int row = hiddenPsmTable.getSelectedRow();
        if (row >= 0) {
            String selectedFilterName = (String) hiddenPsmTable.getValueAt(row, 2);
            psmHideFilters.remove(selectedFilterName);
            emptyTables();
            fillTables();
        }
    }//GEN-LAST:event_deleteHiddenPsmActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addHiddenPeptides;
    private javax.swing.JButton addHiddenProtein;
    private javax.swing.JButton addHiddenPsm;
    private javax.swing.JButton addStarredPeptides;
    private javax.swing.JButton addStarredProtein;
    private javax.swing.JButton addStarredPsm;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton clearHiddenPeptides;
    private javax.swing.JButton clearHiddenProtein;
    private javax.swing.JButton clearHiddenPsm;
    private javax.swing.JButton clearStarredPeptides;
    private javax.swing.JButton clearStarredProtein;
    private javax.swing.JButton clearStarredPsm;
    private javax.swing.JButton deleteHiddenPeptides;
    private javax.swing.JButton deleteHiddenProtein;
    private javax.swing.JButton deleteHiddenPsm;
    private javax.swing.JButton deleteStarredPeptides;
    private javax.swing.JButton deleteStarredProtein;
    private javax.swing.JButton deleteStarredPsm;
    private javax.swing.JButton editHiddenPeptides;
    private javax.swing.JButton editHiddenProtein;
    private javax.swing.JButton editHiddenPsm;
    private javax.swing.JButton editStarredPeptides;
    private javax.swing.JButton editStarredProtein;
    private javax.swing.JButton editStarredPsm;
    private javax.swing.JPanel hiddenPeptidesPanel;
    private javax.swing.JScrollPane hiddenPeptidesScrollPane;
    private javax.swing.JTable hiddenPeptidesTable;
    private javax.swing.JPanel hiddenProteinsPanel;
    private javax.swing.JScrollPane hiddenProteinsScrollPane;
    private javax.swing.JTable hiddenProteinsTable;
    private javax.swing.JTable hiddenPsmTable;
    private javax.swing.JPanel hiddenPsmsPanel;
    private javax.swing.JScrollPane hiddenPsmsScrollPane;
    private javax.swing.JButton okButton;
    private javax.swing.JButton openDialogHelpJButton;
    private javax.swing.JSplitPane peptidesSplitPane;
    private javax.swing.JSplitPane proteinsSplitPane;
    private javax.swing.JSplitPane psmsSplitPane;
    private javax.swing.JPanel starredPeptidesPanel;
    private javax.swing.JScrollPane starredPeptidesScrollPane;
    private javax.swing.JTable starredPeptidesTable;
    private javax.swing.JPanel starredProteinsPanel;
    private javax.swing.JScrollPane starredProteinsScrollPane;
    private javax.swing.JTable starredProteinsTable;
    private javax.swing.JTable starredPsmTable;
    private javax.swing.JPanel starredPsmsPanel;
    private javax.swing.JScrollPane starredPsmsScrollPane;
    private javax.swing.JTabbedPane tabbedPane;
    // End of variables declaration//GEN-END:variables

    /**
     * Updates the tables with the filters.
     */
    public void updateTables() {
        emptyTables();
        fillTables();
    }
}
