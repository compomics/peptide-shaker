package eu.isas.peptideshaker.gui;

import eu.isas.peptideshaker.filtering.MatchFilter;
import eu.isas.peptideshaker.filtering.PeptideFilter;
import eu.isas.peptideshaker.filtering.ProteinFilter;
import eu.isas.peptideshaker.filtering.PsmFilter;
import eu.isas.peptideshaker.gui.FindDialog.FilterType;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import no.uib.jsparklines.extra.NimbusCheckBoxRenderer;

/**
 * Displays the filters used for star/hide items.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class FiltersDialog extends javax.swing.JDialog {

    /**
     * The main gui.
     */
    private PeptideShakerGUI peptideShakerGUI;
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
     * The psm star filters.
     */
    private HashMap<String, PsmFilter> psmStarFilters;
    /**
     * The psm hide filters.
     */
    private HashMap<String, PsmFilter> psmHideFilters;

    /**
     * Creates a new FiltersDialog.
     *
     * @param peptideShakerGUI
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
        proteinStarFilters = new HashMap<String, ProteinFilter>();
        proteinHideFilters = new HashMap<String, ProteinFilter>();
        peptideStarFilters = new HashMap<String, PeptideFilter>();
        peptideHideFilters = new HashMap<String, PeptideFilter>();
        psmStarFilters = new HashMap<String, PsmFilter>();
        psmHideFilters = new HashMap<String, PsmFilter>();
        proteinStarFilters.putAll(peptideShakerGUI.getFilterPreferences().getProteinStarFilters());
        proteinHideFilters.putAll(peptideShakerGUI.getFilterPreferences().getProteinHideFilters());
        peptideStarFilters.putAll(peptideShakerGUI.getFilterPreferences().getPeptideStarFilters());
        peptideHideFilters.putAll(peptideShakerGUI.getFilterPreferences().getPeptideHideFilters());
        psmStarFilters.putAll(peptideShakerGUI.getFilterPreferences().getPsmStarFilters());
        psmHideFilters.putAll(peptideShakerGUI.getFilterPreferences().getPsmHideFilters());
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
        hiddenProteinsPanel = new javax.swing.JPanel();
        hiddenProteinsScrollPane = new javax.swing.JScrollPane();
        hiddenProteinsTable = new javax.swing.JTable();
        addHiddenProtein = new javax.swing.JButton();
        editHiddenProtein = new javax.swing.JButton();
        clearHiddenProtein = new javax.swing.JButton();
        peptidesSplitPane = new javax.swing.JSplitPane();
        starredPeptidesPanel = new javax.swing.JPanel();
        starredPeptidesScrollPane = new javax.swing.JScrollPane();
        starredPeptidesTable = new javax.swing.JTable();
        addStarredPeptides = new javax.swing.JButton();
        editStarredPeptides = new javax.swing.JButton();
        clearStarredPeptides = new javax.swing.JButton();
        hiddenPeptidesPanel = new javax.swing.JPanel();
        hiddenPeptidesScrollPane = new javax.swing.JScrollPane();
        hiddenPeptidesTable = new javax.swing.JTable();
        addHiddenPeptides = new javax.swing.JButton();
        editHiddenPeptides = new javax.swing.JButton();
        clearHiddenPeptides = new javax.swing.JButton();
        psmsSplitPane = new javax.swing.JSplitPane();
        starredPsmsPanel = new javax.swing.JPanel();
        starredPsmsScrollPane = new javax.swing.JScrollPane();
        starredPsmTable = new javax.swing.JTable();
        addStarredPsm = new javax.swing.JButton();
        editStarredPsm = new javax.swing.JButton();
        clearStarredPsm = new javax.swing.JButton();
        hiddenPsmsPanel = new javax.swing.JPanel();
        hiddenPsmsScrollPane = new javax.swing.JScrollPane();
        hiddenPsmTable = new javax.swing.JTable();
        addHiddenPsm = new javax.swing.JButton();
        editHiddenPsm = new javax.swing.JButton();
        clearHiddenPsm = new javax.swing.JButton();
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
        addStarredProtein.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addStarredProteinActionPerformed(evt);
            }
        });

        editStarredProtein.setText("Edit");
        editStarredProtein.setEnabled(false);
        editStarredProtein.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editStarredProteinActionPerformed(evt);
            }
        });

        clearStarredProtein.setText("Clear");
        clearStarredProtein.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearStarredProteinActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout starredProteinsPanelLayout = new javax.swing.GroupLayout(starredProteinsPanel);
        starredProteinsPanel.setLayout(starredProteinsPanelLayout);
        starredProteinsPanelLayout.setHorizontalGroup(
            starredProteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(starredProteinsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(starredProteinsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(starredProteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(editStarredProtein, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(addStarredProtein, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(clearStarredProtein, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        starredProteinsPanelLayout.setVerticalGroup(
            starredProteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(starredProteinsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(starredProteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(starredProteinsPanelLayout.createSequentialGroup()
                        .addComponent(addStarredProtein)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(editStarredProtein)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearStarredProtein))
                    .addComponent(starredProteinsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 151, Short.MAX_VALUE))
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
        addHiddenProtein.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addHiddenProteinActionPerformed(evt);
            }
        });

        editHiddenProtein.setText("Edit");
        editHiddenProtein.setEnabled(false);
        editHiddenProtein.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editHiddenProteinActionPerformed(evt);
            }
        });

        clearHiddenProtein.setText("Clear");
        clearHiddenProtein.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearHiddenProteinActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout hiddenProteinsPanelLayout = new javax.swing.GroupLayout(hiddenProteinsPanel);
        hiddenProteinsPanel.setLayout(hiddenProteinsPanelLayout);
        hiddenProteinsPanelLayout.setHorizontalGroup(
            hiddenProteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(hiddenProteinsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(hiddenProteinsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(hiddenProteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(editHiddenProtein, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(addHiddenProtein, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(clearHiddenProtein, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        hiddenProteinsPanelLayout.setVerticalGroup(
            hiddenProteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(hiddenProteinsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(hiddenProteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(hiddenProteinsPanelLayout.createSequentialGroup()
                        .addComponent(addHiddenProtein)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(editHiddenProtein)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearHiddenProtein))
                    .addComponent(hiddenProteinsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 154, Short.MAX_VALUE))
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
        addStarredPeptides.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addStarredPeptidesActionPerformed(evt);
            }
        });

        editStarredPeptides.setText("Edit");
        editStarredPeptides.setEnabled(false);
        editStarredPeptides.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editStarredPeptidesActionPerformed(evt);
            }
        });

        clearStarredPeptides.setText("Clear");
        clearStarredPeptides.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearStarredPeptidesActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout starredPeptidesPanelLayout = new javax.swing.GroupLayout(starredPeptidesPanel);
        starredPeptidesPanel.setLayout(starredPeptidesPanelLayout);
        starredPeptidesPanelLayout.setHorizontalGroup(
            starredPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(starredPeptidesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(starredPeptidesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(starredPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(editStarredPeptides, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(addStarredPeptides, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(clearStarredPeptides, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
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
                        .addComponent(clearStarredPeptides))
                    .addComponent(starredPeptidesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 151, Short.MAX_VALUE))
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
        addHiddenPeptides.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addHiddenPeptidesActionPerformed(evt);
            }
        });

        editHiddenPeptides.setText("Edit");
        editHiddenPeptides.setEnabled(false);
        editHiddenPeptides.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editHiddenPeptidesActionPerformed(evt);
            }
        });

        clearHiddenPeptides.setText("Clear");
        clearHiddenPeptides.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearHiddenPeptidesActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout hiddenPeptidesPanelLayout = new javax.swing.GroupLayout(hiddenPeptidesPanel);
        hiddenPeptidesPanel.setLayout(hiddenPeptidesPanelLayout);
        hiddenPeptidesPanelLayout.setHorizontalGroup(
            hiddenPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(hiddenPeptidesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(hiddenPeptidesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(hiddenPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(editHiddenPeptides, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(addHiddenPeptides, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(clearHiddenPeptides, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
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
                        .addComponent(clearHiddenPeptides))
                    .addComponent(hiddenPeptidesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 154, Short.MAX_VALUE))
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
        addStarredPsm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addStarredPsmActionPerformed(evt);
            }
        });

        editStarredPsm.setText("Edit");
        editStarredPsm.setEnabled(false);
        editStarredPsm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editStarredPsmActionPerformed(evt);
            }
        });

        clearStarredPsm.setText("Clear");
        clearStarredPsm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearStarredPsmActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout starredPsmsPanelLayout = new javax.swing.GroupLayout(starredPsmsPanel);
        starredPsmsPanel.setLayout(starredPsmsPanelLayout);
        starredPsmsPanelLayout.setHorizontalGroup(
            starredPsmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(starredPsmsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(starredPsmsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(starredPsmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(editStarredPsm, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(addStarredPsm, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(clearStarredPsm, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        starredPsmsPanelLayout.setVerticalGroup(
            starredPsmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(starredPsmsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(starredPsmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(starredPsmsPanelLayout.createSequentialGroup()
                        .addComponent(addStarredPsm)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(editStarredPsm)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearStarredPsm))
                    .addComponent(starredPsmsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 151, Short.MAX_VALUE))
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
        addHiddenPsm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addHiddenPsmActionPerformed(evt);
            }
        });

        editHiddenPsm.setText("Edit");
        editHiddenPsm.setEnabled(false);
        editHiddenPsm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editHiddenPsmActionPerformed(evt);
            }
        });

        clearHiddenPsm.setText("Clear");
        clearHiddenPsm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearHiddenPsmActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout hiddenPsmsPanelLayout = new javax.swing.GroupLayout(hiddenPsmsPanel);
        hiddenPsmsPanel.setLayout(hiddenPsmsPanelLayout);
        hiddenPsmsPanelLayout.setHorizontalGroup(
            hiddenPsmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(hiddenPsmsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(hiddenPsmsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(hiddenPsmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(editHiddenPsm, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(addHiddenPsm, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(clearHiddenPsm, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
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
                        .addComponent(clearHiddenPsm))
                    .addComponent(hiddenPsmsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 154, Short.MAX_VALUE))
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
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 375, Short.MAX_VALUE)
                        .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton))
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(tabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 544, Short.MAX_VALUE)))
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
     * @param evt
     */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        this.dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    /**
     * Save the filter settings and update the filters.
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed

        peptideShakerGUI.getFilterPreferences().setProteinStarFilters(proteinStarFilters);
        peptideShakerGUI.getFilterPreferences().setProteinHideFilters(proteinHideFilters);
        peptideShakerGUI.getFilterPreferences().setPeptideStarFilters(peptideStarFilters);
        peptideShakerGUI.getFilterPreferences().setPeptideHideFilters(peptideHideFilters);
        peptideShakerGUI.getFilterPreferences().setPsmStarFilters(psmStarFilters);
        peptideShakerGUI.getFilterPreferences().setPsmHideFilters(psmHideFilters);
        setVisible(false);
        peptideShakerGUI.getStarHider().starHide();

        peptideShakerGUI.setUpdated(PeptideShakerGUI.OVER_VIEW_TAB_INDEX, false);
        peptideShakerGUI.setUpdated(PeptideShakerGUI.PROTEIN_FRACTIONS_TAB_INDEX, false);
        peptideShakerGUI.setUpdated(PeptideShakerGUI.MODIFICATIONS_TAB_INDEX, false);
        peptideShakerGUI.setUpdated(PeptideShakerGUI.STRUCTURES_TAB_INDEX, false);
        peptideShakerGUI.setUpdated(PeptideShakerGUI.GO_ANALYSIS_TAB_INDEX, false);
        peptideShakerGUI.setUpdated(PeptideShakerGUI.QC_PLOTS_TAB_INDEX, false);
        peptideShakerGUI.setUpdated(PeptideShakerGUI.PROTEIN_FRACTIONS_TAB_INDEX, false);
        peptideShakerGUI.updateTabbedPanes();

        dispose();
    }//GEN-LAST:event_okButtonActionPerformed

    /**
     * Update the starred proteins table.
     *
     * @param evt
     */
    private void starredProteinsTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_starredProteinsTableKeyReleased
        int column = starredProteinsTable.getSelectedColumn();
        int row = starredProteinsTable.getSelectedRow();

        if (column == 2) {
            String newName = (String) starredProteinsTable.getValueAt(row, column);
            ArrayList<String> others = new ArrayList<String>();
            for (int i = 0; i < starredProteinsTable.getRowCount(); i++) {
                if (i != row) {
                    others.add((String) starredProteinsTable.getValueAt(i, column));
                }
            }
            if (others.contains(newName)) {
                int outcome = JOptionPane.YES_OPTION;
                if (peptideShakerGUI.getFilterPreferences().filterExists(newName)) {
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

            // has to be done lie this on order to avoid a ConcurrentModificationException
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
        } else if (starredProteinsTable.getSelectedRow() != -1) {
            editStarredProtein.setEnabled(true);
        }
    }//GEN-LAST:event_starredProteinsTableKeyReleased

    /**
     * Update the starred proteins table.
     *
     * @param evt
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
        } else if (starredProteinsTable.getSelectedRow() != -1) {
            editStarredProtein.setEnabled(true);
        }
    }//GEN-LAST:event_starredProteinsTableMouseReleased

    /**
     * Update the starred peptides table.
     *
     * @param evt
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
        } else if (starredPeptidesTable.getSelectedRow() != -1) {
            editStarredPeptides.setEnabled(true);
        }
    }//GEN-LAST:event_starredPeptidesTableMouseReleased

    /**
     * Update the starred psms table.
     *
     * @param evt
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
        } else if (starredPsmTable.getSelectedRow() != -1) {
            editStarredPsm.setEnabled(true);
        }
    }//GEN-LAST:event_starredPsmTableMouseReleased

    /**
     * Update the hidden proteins table.
     *
     * @param evt
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
        } else if (hiddenProteinsTable.getSelectedRow() != -1) {
            editHiddenProtein.setEnabled(true);
        }
    }//GEN-LAST:event_hiddenProteinsTableMouseReleased

    /**
     * Update the hidden peptides table.
     *
     * @param evt
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
        } else if (hiddenPeptidesTable.getSelectedRow() != -1) {
            editHiddenPeptides.setEnabled(true);
        }
    }//GEN-LAST:event_hiddenPeptidesTableMouseReleased

    /**
     * Update the hidden psms table.
     *
     * @param evt
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
        } else if (hiddenPsmTable.getSelectedRow() != -1) {
            editHiddenPsm.setEnabled(true);
        }
    }//GEN-LAST:event_hiddenPsmTableMouseReleased

    /**
     * Open the FindDialog to add/edit a filter.
     *
     * @param evt
     */
    private void addStarredPsmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addStarredPsmActionPerformed
        new FindDialog(peptideShakerGUI, this, 2, FilterType.STAR);
    }//GEN-LAST:event_addStarredPsmActionPerformed

    /**
     * Open the FindDialog to add/edit a filter.
     *
     * @param evt
     */
    private void addStarredProteinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addStarredProteinActionPerformed
        new FindDialog(peptideShakerGUI, this, 0, FilterType.STAR);
    }//GEN-LAST:event_addStarredProteinActionPerformed

    /**
     * Open the FindDialog to add/edit a filter.
     *
     * @param evt
     */
    private void addHiddenProteinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addHiddenProteinActionPerformed
        new FindDialog(peptideShakerGUI, this, 0, FilterType.HIDE);
    }//GEN-LAST:event_addHiddenProteinActionPerformed

    /**
     * Open the FindDialog to add/edit a filter.
     *
     * @param evt
     */
    private void addStarredPeptidesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addStarredPeptidesActionPerformed
        new FindDialog(peptideShakerGUI, this, 1, FilterType.STAR);
    }//GEN-LAST:event_addStarredPeptidesActionPerformed

    /**
     * Open the FindDialog to add/edit a filter.
     *
     * @param evt
     */
    private void addHiddenPeptidesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addHiddenPeptidesActionPerformed
        new FindDialog(peptideShakerGUI, this, 1, FilterType.HIDE);
    }//GEN-LAST:event_addHiddenPeptidesActionPerformed

    /**
     * Open the FindDialog to add/edit a filter.
     *
     * @param evt
     */
    private void addHiddenPsmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addHiddenPsmActionPerformed
        new FindDialog(peptideShakerGUI, this, 2, FilterType.HIDE);
    }//GEN-LAST:event_addHiddenPsmActionPerformed

    /**
     * Clear the starred protein filters.
     *
     * @param evt
     */
    private void clearStarredProteinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearStarredProteinActionPerformed
        proteinStarFilters.clear();
        ((DefaultTableModel) starredProteinsTable.getModel()).getDataVector().removeAllElements();
        ((DefaultTableModel) starredProteinsTable.getModel()).fireTableDataChanged();
        editStarredProtein.setEnabled(false);
    }//GEN-LAST:event_clearStarredProteinActionPerformed

    /**
     * Clear the hidden protein filters.
     *
     * @param evt
     */
    private void clearHiddenProteinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearHiddenProteinActionPerformed
        proteinHideFilters.clear();
        ((DefaultTableModel) hiddenProteinsTable.getModel()).getDataVector().removeAllElements();
        ((DefaultTableModel) hiddenProteinsTable.getModel()).fireTableDataChanged();
        editHiddenProtein.setEnabled(false);
    }//GEN-LAST:event_clearHiddenProteinActionPerformed

    /**
     * Clear the starred peptides filters.
     *
     * @param evt
     */
    private void clearStarredPeptidesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearStarredPeptidesActionPerformed
        peptideStarFilters.clear();
        ((DefaultTableModel) starredPeptidesTable.getModel()).getDataVector().removeAllElements();
        ((DefaultTableModel) starredPeptidesTable.getModel()).fireTableDataChanged();
        editStarredPeptides.setEnabled(false);
    }//GEN-LAST:event_clearStarredPeptidesActionPerformed

    /**
     * Clear the hidden peptides filters.
     *
     * @param evt
     */
    private void clearHiddenPeptidesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearHiddenPeptidesActionPerformed
        peptideHideFilters.clear();
        ((DefaultTableModel) hiddenPeptidesTable.getModel()).getDataVector().removeAllElements();
        ((DefaultTableModel) hiddenPeptidesTable.getModel()).fireTableDataChanged();
        editHiddenPeptides.setEnabled(false);
    }//GEN-LAST:event_clearHiddenPeptidesActionPerformed

    /**
     * Clear the starred psms filters.
     *
     * @param evt
     */
    private void clearStarredPsmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearStarredPsmActionPerformed
        psmStarFilters.clear();
        ((DefaultTableModel) starredPsmTable.getModel()).getDataVector().removeAllElements();
        ((DefaultTableModel) starredPsmTable.getModel()).fireTableDataChanged();
        editStarredPsm.setEnabled(false);
    }//GEN-LAST:event_clearStarredPsmActionPerformed

    /**
     * Clear the hidden psms filters.
     *
     * @param evt
     */
    private void clearHiddenPsmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearHiddenPsmActionPerformed
        psmHideFilters.clear();
        ((DefaultTableModel) hiddenPsmTable.getModel()).getDataVector().removeAllElements();
        ((DefaultTableModel) hiddenPsmTable.getModel()).fireTableDataChanged();
        editHiddenPsm.setEnabled(false);
    }//GEN-LAST:event_clearHiddenPsmActionPerformed

    /**
     * Edit a starred protein.
     *
     * @param evt
     */
    private void editStarredProteinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editStarredProteinActionPerformed
        int row = starredProteinsTable.getSelectedRow();
        if (row >= 0) {
            String selectedFilterName = (String) starredProteinsTable.getValueAt(row, 2);
            ProteinFilter proteinFilter = proteinStarFilters.get(selectedFilterName);
            new FindDialog(peptideShakerGUI, this, proteinFilter, null, null, FilterType.STAR);
        }
    }//GEN-LAST:event_editStarredProteinActionPerformed

    /**
     * Edit a hidden protein.
     *
     * @param evt
     */
    private void editHiddenProteinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editHiddenProteinActionPerformed
        int row = hiddenProteinsTable.getSelectedRow();
        if (row >= 0) {
            String selectedFilterName = (String) hiddenProteinsTable.getValueAt(row, 2);
            ProteinFilter proteinFilter = proteinHideFilters.get(selectedFilterName);
            new FindDialog(peptideShakerGUI, this, proteinFilter, null, null, FilterType.HIDE);
        }
    }//GEN-LAST:event_editHiddenProteinActionPerformed

    /**
     * Edit a starred peptide.
     *
     * @param evt
     */
    private void editStarredPeptidesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editStarredPeptidesActionPerformed
        int row = starredPeptidesTable.getSelectedRow();
        if (row >= 0) {
            String selectedFilterName = (String) starredPeptidesTable.getValueAt(row, 2);
            PeptideFilter peptideFilter = peptideStarFilters.get(selectedFilterName);
            new FindDialog(peptideShakerGUI, this, null, peptideFilter, null, FilterType.STAR);
        }
    }//GEN-LAST:event_editStarredPeptidesActionPerformed

    /**
     * Edit a hidden peptide.
     *
     * @param evt
     */
    private void editHiddenPeptidesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editHiddenPeptidesActionPerformed
        int row = hiddenPeptidesTable.getSelectedRow();
        if (row >= 0) {
            String selectedFilterName = (String) hiddenPeptidesTable.getValueAt(row, 2);
            PeptideFilter peptideFilter = peptideHideFilters.get(selectedFilterName);
            new FindDialog(peptideShakerGUI, this, null, peptideFilter, null, FilterType.HIDE);
        }
    }//GEN-LAST:event_editHiddenPeptidesActionPerformed

    /**
     * Edit a starred psm.
     *
     * @param evt
     */
    private void editStarredPsmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editStarredPsmActionPerformed
        int row = starredPsmTable.getSelectedRow();
        if (row >= 0) {
            String selectedFilterName = (String) starredPsmTable.getValueAt(row, 2);
            PsmFilter psmFilter = psmStarFilters.get(selectedFilterName);
            new FindDialog(peptideShakerGUI, this, null, null, psmFilter, FilterType.STAR);
        }
    }//GEN-LAST:event_editStarredPsmActionPerformed

    /**
     * Edit a hidden protein.
     *
     * @param evt
     */
    private void editHiddenPsmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editHiddenPsmActionPerformed
        int row = hiddenPsmTable.getSelectedRow();
        if (row >= 0) {
            String selectedFilterName = (String) hiddenPsmTable.getValueAt(row, 2);
            PsmFilter psmFilter = psmHideFilters.get(selectedFilterName);
            new FindDialog(peptideShakerGUI, this, null, null, psmFilter, FilterType.HIDE);
        }
    }//GEN-LAST:event_editHiddenPsmActionPerformed

    /**
     * Update the hidden proteins table.
     *
     * @param evt
     */
    private void hiddenProteinsTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_hiddenProteinsTableKeyReleased

        int column = hiddenProteinsTable.getSelectedColumn();
        int row = hiddenProteinsTable.getSelectedRow();

        if (column == 2) {
            String newName = (String) hiddenProteinsTable.getValueAt(row, column);
            ArrayList<String> others = new ArrayList<String>();
            for (int i = 0; i < hiddenProteinsTable.getRowCount(); i++) {
                if (i != row) {
                    others.add((String) hiddenProteinsTable.getValueAt(i, column));
                }
            }
            if (others.contains(newName)) {
                int outcome = JOptionPane.YES_OPTION;
                if (peptideShakerGUI.getFilterPreferences().filterExists(newName)) {
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
        } else if (hiddenProteinsTable.getSelectedRow() != -1) {
            editHiddenProtein.setEnabled(true);
        }
    }//GEN-LAST:event_hiddenProteinsTableKeyReleased

    /**
     * Update the starred peptides table.
     *
     * @param evt
     */
    private void starredPeptidesTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_starredPeptidesTableKeyReleased
        int column = starredPeptidesTable.getSelectedColumn();
        int row = starredPeptidesTable.getSelectedRow();

        if (column == 2) {
            String newName = (String) starredPeptidesTable.getValueAt(row, column);
            ArrayList<String> others = new ArrayList<String>();
            for (int i = 0; i < starredPeptidesTable.getRowCount(); i++) {
                if (i != row) {
                    others.add((String) starredPeptidesTable.getValueAt(i, column));
                }
            }
            if (others.contains(newName)) {
                int outcome = JOptionPane.YES_OPTION;
                if (peptideShakerGUI.getFilterPreferences().filterExists(newName)) {
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
        } else if (starredPeptidesTable.getSelectedRow() != -1) {
            editStarredPeptides.setEnabled(true);
        }
    }//GEN-LAST:event_starredPeptidesTableKeyReleased

    /**
     * Update the hidden peptides table.
     *
     * @param evt
     */
    private void hiddenPeptidesTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_hiddenPeptidesTableKeyReleased
        int column = hiddenPeptidesTable.getSelectedColumn();
        int row = hiddenPeptidesTable.getSelectedRow();

        if (column == 2) {
            String newName = (String) hiddenPeptidesTable.getValueAt(row, column);
            ArrayList<String> others = new ArrayList<String>();
            for (int i = 0; i < hiddenPeptidesTable.getRowCount(); i++) {
                if (i != row) {
                    others.add((String) hiddenPeptidesTable.getValueAt(i, column));
                }
            }
            if (others.contains(newName)) {
                int outcome = JOptionPane.YES_OPTION;
                if (peptideShakerGUI.getFilterPreferences().filterExists(newName)) {
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
        } else if (hiddenPeptidesTable.getSelectedRow() != -1) {
            editHiddenPeptides.setEnabled(true);
        }
    }//GEN-LAST:event_hiddenPeptidesTableKeyReleased

    /**
     * Update the starred psms table.
     *
     * @param evt
     */
    private void starredPsmTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_starredPsmTableKeyReleased
        int column = starredPsmTable.getSelectedColumn();
        int row = starredPsmTable.getSelectedRow();

        if (column == 2) {
            String newName = (String) starredPsmTable.getValueAt(row, column);
            ArrayList<String> others = new ArrayList<String>();
            for (int i = 0; i < starredPsmTable.getRowCount(); i++) {
                if (i != row) {
                    others.add((String) starredPsmTable.getValueAt(i, column));
                }
            }
            if (others.contains(newName)) {
                int outcome = JOptionPane.YES_OPTION;
                if (peptideShakerGUI.getFilterPreferences().filterExists(newName)) {
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
        } else if (starredPsmTable.getSelectedRow() != -1) {
            editStarredPsm.setEnabled(true);
        }
    }//GEN-LAST:event_starredPsmTableKeyReleased

    /**
     * Update the hidden psms table.
     *
     * @param evt
     */
    private void hiddenPsmTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_hiddenPsmTableKeyReleased
        int column = hiddenPsmTable.getSelectedColumn();
        int row = hiddenPsmTable.getSelectedRow();

        if (column == 2) {
            String newName = (String) hiddenPsmTable.getValueAt(row, column);
            ArrayList<String> others = new ArrayList<String>();
            for (int i = 0; i < hiddenPsmTable.getRowCount(); i++) {
                if (i != row) {
                    others.add((String) hiddenPsmTable.getValueAt(i, column));
                }
            }
            if (others.contains(newName)) {
                int outcome = JOptionPane.YES_OPTION;
                if (peptideShakerGUI.getFilterPreferences().filterExists(newName)) {
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
        }

        if (hiddenPsmTable.isEditing()) {
            editHiddenPsm.setEnabled(false);
        } else if (hiddenPsmTable.getSelectedRow() != -1) {
            editHiddenPsm.setEnabled(true);
        }
    }//GEN-LAST:event_hiddenPsmTableKeyReleased

    /**
     * Change the cursor icon to a hand icon.
     *
     * @param evt
     */
    private void openDialogHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_openDialogHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_openDialogHelpJButtonMouseEntered

    /**
     * Change the cursor icon to the default icon.
     *
     * @param evt
     */
    private void openDialogHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_openDialogHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_openDialogHelpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void openDialogHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openDialogHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/FilterSelection.html"));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_openDialogHelpJButtonActionPerformed

    /**
     * Make sure that the split pane dividers all have the same location.
     *
     * @param evt
     */
    private void tabbedPaneComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_tabbedPaneComponentResized
        proteinsSplitPane.setDividerLocation(0.5);
        peptidesSplitPane.setDividerLocation(0.5);
        psmsSplitPane.setDividerLocation(0.5);
    }//GEN-LAST:event_tabbedPaneComponentResized
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
    public void updateFilters() {
        emptyTables();
        updateMaps();
        fillTables();
    }
}
