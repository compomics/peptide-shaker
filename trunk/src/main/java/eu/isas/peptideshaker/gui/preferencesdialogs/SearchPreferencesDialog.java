package eu.isas.peptideshaker.gui.preferencesdialogs;

import com.compomics.util.Util;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.io.identifications.IdentificationParametersReader;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.ptm.ModificationsDialog;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.gui.ptm.PtmDialog;
import com.compomics.util.gui.ptm.PtmDialogParent;
import com.compomics.util.preferences.ModificationProfile;
import com.compomics.util.pride.CvTerm;
import com.compomics.util.pride.PrideObjectsFactory;
import com.compomics.util.pride.PtmToPrideMap;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import no.uib.jsparklines.extra.HtmlLinksRenderer;
import no.uib.jsparklines.extra.NimbusCheckBoxRenderer;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import no.uib.jsparklines.renderers.JSparklinesColorTableCellRenderer;

/**
 * A dialog for displaying and editing the search preferences.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class SearchPreferencesDialog extends javax.swing.JDialog implements PtmDialogParent {

    /**
     * The tooltips for the expected variable mods.
     */
    private Vector<String> expectedVariableModsTableToolTips;
    /**
     * The tooltips for the available mods.
     */
    private Vector<String> availableModsTableToolTips;
    /**
     * The search parameters needed by PeptideShaker.
     */
    private SearchParameters searchParameters;
    /**
     * The enzyme factory.
     */
    private EnzymeFactory enzymeFactory = EnzymeFactory.getInstance();
    /**
     * The compomics PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The selected PTMs.
     */
    private ArrayList<String> modificationList = new ArrayList<String>();
    /**
     * Boolean indicating whether import-related data can be edited.
     */
    private boolean editable;
    /**
     * The PTM to pride map.
     */
    private PtmToPrideMap ptmToPrideMap;
    /**
     * The color for the HTML tags for selected rows.
     */
    private String selectedRowHtmlTagFontColor; // @TODO: this ought to be a compomics setting
    /**
     * The color for the HTML tags for not selected rows.
     */
    private String notSelectedRowHtmlTagFontColor; //@TODO: this ought to be a compomics setting
    /**
     * Boolean indicating whether the user pushed on cancel.
     */
    private boolean canceled = false;
    /*
     * The PeptideShakerGUI parent.
     */
    private PeptideShakerGUI peptideShakerGUI;

    /**
     * Creates a new search parameters dialog.
     *
     * @param peptideShakerGUI the parent frame
     * @param editable a boolean indicating whether the search parameters can be
     * edited
     * @param searchParameters the search parameters. If null default versions
     * will be used.
     * @param selectedRowHtmlTagFontColor @TODO: this ought to be a compomics
     * setting
     * @param notSelectedRowHtmlTagFontColor @TODO: this ought to be a compomics
     * setting
     * @param ptmToPrideMap the PTM to pride map
     */
    public SearchPreferencesDialog(PeptideShakerGUI peptideShakerGUI, boolean editable, SearchParameters searchParameters, 
            PtmToPrideMap ptmToPrideMap, String selectedRowHtmlTagFontColor, String notSelectedRowHtmlTagFontColor) {
        super(peptideShakerGUI, true);
        this.peptideShakerGUI = peptideShakerGUI;

        this.editable = editable;

        if (searchParameters == null) {
            this.searchParameters = new SearchParameters();
        } else {
            this.searchParameters = searchParameters;
        }

        this.ptmToPrideMap = ptmToPrideMap;
        this.selectedRowHtmlTagFontColor = selectedRowHtmlTagFontColor;
        this.notSelectedRowHtmlTagFontColor = notSelectedRowHtmlTagFontColor;

        initComponents();
        setUpGui();
        setLocationRelativeTo(peptideShakerGUI);
        setVisible(true);
    }

    /**
     * Set up the GUI.
     */
    private void setUpGui() {

        // set the cell renderers
        expectedModificationsTable.getColumn("  ").setCellRenderer(new JSparklinesColorTableCellRenderer());
        expectedModificationsTable.getColumn("PSI-MOD").setCellRenderer(new HtmlLinksRenderer(selectedRowHtmlTagFontColor, notSelectedRowHtmlTagFontColor));
        availableModificationsTable.getColumn("PSI-MOD").setCellRenderer(new HtmlLinksRenderer(selectedRowHtmlTagFontColor, notSelectedRowHtmlTagFontColor));
        expectedModificationsTable.getColumn("U.M.").setCellRenderer(new NimbusCheckBoxRenderer());
        availableModificationsTable.getColumn("U.M.").setCellRenderer(new NimbusCheckBoxRenderer());
        expectedModificationsTable.getColumn("U.M.").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/selected_green.png")),
                null,
                "User Modification", null));
        availableModificationsTable.getColumn("U.M.").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/selected_green.png")),
                null,
                "User Modification", null));

        // set table properties
        expectedModificationsTable.getTableHeader().setReorderingAllowed(false);
        availableModificationsTable.getTableHeader().setReorderingAllowed(false);

        availableModificationsTable.getColumn(" ").setMaxWidth(40);
        availableModificationsTable.getColumn(" ").setMinWidth(40);
        availableModificationsTable.getColumn("U.M.").setMaxWidth(40);
        availableModificationsTable.getColumn("U.M.").setMinWidth(40);

        expectedModificationsTable.getColumn(" ").setMaxWidth(40);
        expectedModificationsTable.getColumn(" ").setMinWidth(40);
        expectedModificationsTable.getColumn("  ").setMaxWidth(40);
        expectedModificationsTable.getColumn("  ").setMinWidth(40);
        expectedModificationsTable.getColumn("U.M.").setMaxWidth(40);
        expectedModificationsTable.getColumn("U.M.").setMinWidth(40);

        availableModificationsTable.getColumn("PSI-MOD").setMaxWidth(100);
        availableModificationsTable.getColumn("PSI-MOD").setMinWidth(100);
        expectedModificationsTable.getColumn("PSI-MOD").setMaxWidth(100);
        expectedModificationsTable.getColumn("PSI-MOD").setMinWidth(100);

        expectedVariableModsTableToolTips = new Vector<String>();
        expectedVariableModsTableToolTips.add(null);
        expectedVariableModsTableToolTips.add("Modification Color");
        expectedVariableModsTableToolTips.add("Modification Name");
        expectedVariableModsTableToolTips.add("Modification Family Name");
        expectedVariableModsTableToolTips.add("Modification Short Name");
        expectedVariableModsTableToolTips.add("User Defined Modification");
        expectedVariableModsTableToolTips.add("The PSI-MOD CV Term Mapping");

        availableModsTableToolTips = new Vector<String>();
        availableModsTableToolTips.add(null);
        availableModsTableToolTips.add("Modification Name");
        availableModsTableToolTips.add("User Defined Modification");
        availableModsTableToolTips.add("The PSI-MOD CV Term Mapping");

        // make sure that the scroll panes are see-through
        expectedModsScrollPane.getViewport().setOpaque(false);
        availableModsScrollPane.getViewport().setOpaque(false);

        modificationList = new ArrayList<String>(searchParameters.getModificationProfile().getAllNotFixedModifications());
        Collections.sort(modificationList);
        enzymesCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        ion1Cmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        ion2Cmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        precursorUnit.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        setScreenProps();
        updateModificationLists();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        expectedPtmPopupMenu = new javax.swing.JPopupMenu();
        removeExpectedPtmJMenuItem = new javax.swing.JMenuItem();
        editExpectedPtmJMenuItem = new javax.swing.JMenuItem();
        availablePtmPopupMenu = new javax.swing.JPopupMenu();
        addAvailablePtmJMenuItem = new javax.swing.JMenuItem();
        editAvailablePtmJMenuItem = new javax.swing.JMenuItem();
        backgroundPanel = new javax.swing.JPanel();
        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        enzymeAndFragmentIonsPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        fragmentIonAccuracyTxt = new javax.swing.JTextField();
        enzymesCmb = new javax.swing.JComboBox();
        jLabel5 = new javax.swing.JLabel();
        missedCleavagesTxt = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        ion1Cmb = new javax.swing.JComboBox();
        ion2Cmb = new javax.swing.JComboBox();
        jLabel9 = new javax.swing.JLabel();
        precursorAccuracy = new javax.swing.JTextField();
        precursorUnit = new javax.swing.JComboBox();
        modProfilePanel = new javax.swing.JPanel();
        addModifications = new javax.swing.JButton();
        removeModification = new javax.swing.JButton();
        expectedModsScrollPane = new javax.swing.JScrollPane();
        expectedModificationsTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) expectedVariableModsTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        expectedModsLabel = new javax.swing.JLabel();
        availableModsLabel = new javax.swing.JLabel();
        availableModsScrollPane = new javax.swing.JScrollPane();
        availableModificationsTable =         new JTable() {

            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {

                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) availableModsTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        editModificationsLabel = new javax.swing.JLabel();
        searchGuiParamsPanel = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        fileTxt = new javax.swing.JTextField();
        loadButton = new javax.swing.JButton();
        helpLineLabel = new javax.swing.JLabel();
        searchPreferencesHelpJButton = new javax.swing.JButton();

        removeExpectedPtmJMenuItem.setText("Remove Selected Modifications");
        removeExpectedPtmJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeExpectedPtmJMenuItemActionPerformed(evt);
            }
        });
        expectedPtmPopupMenu.add(removeExpectedPtmJMenuItem);

        editExpectedPtmJMenuItem.setText("Edit");
        editExpectedPtmJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editExpectedPtmJMenuItemActionPerformed(evt);
            }
        });
        expectedPtmPopupMenu.add(editExpectedPtmJMenuItem);

        addAvailablePtmJMenuItem.setText("Add Selected Modifications");
        addAvailablePtmJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addAvailablePtmJMenuItemActionPerformed(evt);
            }
        });
        availablePtmPopupMenu.add(addAvailablePtmJMenuItem);

        editAvailablePtmJMenuItem.setText("Edit");
        editAvailablePtmJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editAvailablePtmJMenuItemActionPerformed(evt);
            }
        });
        availablePtmPopupMenu.add(editAvailablePtmJMenuItem);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Search Parameters");
        setMinimumSize(new java.awt.Dimension(810, 650));

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        enzymeAndFragmentIonsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Enzyme and Fragment Ions"));
        enzymeAndFragmentIonsPanel.setOpaque(false);

        jLabel1.setText("MS/MS Tol. (Da)");
        jLabel1.setToolTipText("Fragment ion tolerance");

        fragmentIonAccuracyTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fragmentIonAccuracyTxt.setToolTipText("Fragment ion tolerance");

        enzymesCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        enzymesCmb.setToolTipText("Enzyme used");

        jLabel5.setText("Enzyme");
        jLabel5.setToolTipText("Enzyme used");

        missedCleavagesTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        missedCleavagesTxt.setText("1");
        missedCleavagesTxt.setToolTipText("Max number of missed cleavages");

        jLabel7.setText("Missed Cleavages");
        jLabel7.setToolTipText("Max number of missed cleavages");

        jLabel2.setText("Fragment Ion Types");

        ion1Cmb.setModel(new DefaultComboBoxModel(searchParameters.getForwardIons()));
        ion1Cmb.setToolTipText("Fragment ion types");

        ion2Cmb.setModel(new DefaultComboBoxModel(searchParameters.getRewindIons()));
        ion2Cmb.setToolTipText("Fragment ion types");

        jLabel9.setText("Prec. Tol.");
        jLabel9.setToolTipText("Precursor tolerance");

        precursorAccuracy.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        precursorAccuracy.setToolTipText("Precursor tolerance");

        precursorUnit.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "ppm", "Da" }));
        precursorUnit.setToolTipText("Precursor tolerance type");

        javax.swing.GroupLayout enzymeAndFragmentIonsPanelLayout = new javax.swing.GroupLayout(enzymeAndFragmentIonsPanel);
        enzymeAndFragmentIonsPanel.setLayout(enzymeAndFragmentIonsPanelLayout);
        enzymeAndFragmentIonsPanelLayout.setHorizontalGroup(
            enzymeAndFragmentIonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(enzymeAndFragmentIonsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(enzymeAndFragmentIonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addComponent(jLabel9))
                .addGap(18, 18, 18)
                .addGroup(enzymeAndFragmentIonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(enzymeAndFragmentIonsPanelLayout.createSequentialGroup()
                        .addComponent(precursorAccuracy, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(precursorUnit, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fragmentIonAccuracyTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(enzymesCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 308, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(59, 59, 59)
                .addGroup(enzymeAndFragmentIonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING))
                .addGap(18, 18, 18)
                .addGroup(enzymeAndFragmentIonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(missedCleavagesTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, enzymeAndFragmentIonsPanelLayout.createSequentialGroup()
                        .addComponent(ion1Cmb, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(ion2Cmb, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        enzymeAndFragmentIonsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {ion1Cmb, ion2Cmb});

        enzymeAndFragmentIonsPanelLayout.setVerticalGroup(
            enzymeAndFragmentIonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(enzymeAndFragmentIonsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(enzymeAndFragmentIonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(enzymesCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7)
                    .addComponent(missedCleavagesTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(enzymeAndFragmentIonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(precursorAccuracy, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(precursorUnit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(ion1Cmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ion2Cmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(fragmentIonAccuracyTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        modProfilePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Modification Profile"));
        modProfilePanel.setOpaque(false);

        addModifications.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/arrowUp_grey.png"))); // NOI18N
        addModifications.setText("Add");
        addModifications.setToolTipText("Add to list of expected modifications");
        addModifications.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        addModifications.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/arrowUp.png"))); // NOI18N
        addModifications.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addModificationsActionPerformed(evt);
            }
        });

        removeModification.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/arrowDown_grey.png"))); // NOI18N
        removeModification.setText("Remove");
        removeModification.setToolTipText("Remove from list of selected modifications");
        removeModification.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        removeModification.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/arrowDown.png"))); // NOI18N
        removeModification.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeModificationActionPerformed(evt);
            }
        });

        expectedModificationsTable.setModel(new ModificationTable());
        expectedModificationsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                expectedModificationsTableMouseClicked(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                expectedModificationsTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                expectedModificationsTableMouseReleased(evt);
            }
        });
        expectedModificationsTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                expectedModificationsTableMouseMoved(evt);
            }
        });
        expectedModsScrollPane.setViewportView(expectedModificationsTable);

        expectedModsLabel.setFont(expectedModsLabel.getFont().deriveFont((expectedModsLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        expectedModsLabel.setText("Expected Variable Modifications");

        availableModsLabel.setFont(availableModsLabel.getFont().deriveFont((availableModsLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        availableModsLabel.setText("Available Modifications");

        availableModificationsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Name", "U.M.", "PSI-MOD"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.Boolean.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        availableModificationsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                availableModificationsTableMouseClicked(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                availableModificationsTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                availableModificationsTableMouseReleased(evt);
            }
        });
        availableModificationsTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                availableModificationsTableMouseMoved(evt);
            }
        });
        availableModsScrollPane.setViewportView(availableModificationsTable);

        editModificationsLabel.setText("<html><a href>Edit</a></html>");
        editModificationsLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                editModificationsLabelMouseReleased(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                editModificationsLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                editModificationsLabelMouseExited(evt);
            }
        });

        javax.swing.GroupLayout modProfilePanelLayout = new javax.swing.GroupLayout(modProfilePanel);
        modProfilePanel.setLayout(modProfilePanelLayout);
        modProfilePanelLayout.setHorizontalGroup(
            modProfilePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(modProfilePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(modProfilePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(availableModsScrollPane)
                    .addComponent(expectedModsScrollPane)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, modProfilePanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(addModifications, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(removeModification, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(modProfilePanelLayout.createSequentialGroup()
                        .addGroup(modProfilePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(modProfilePanelLayout.createSequentialGroup()
                                .addComponent(availableModsLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(editModificationsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(expectedModsLabel))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        modProfilePanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {addModifications, removeModification});

        modProfilePanelLayout.setVerticalGroup(
            modProfilePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(modProfilePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(expectedModsLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(expectedModsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 124, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(modProfilePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(modProfilePanelLayout.createSequentialGroup()
                        .addGroup(modProfilePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(removeModification, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(addModifications, javax.swing.GroupLayout.PREFERRED_SIZE, 0, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(14, 14, 14))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, modProfilePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(availableModsLabel)
                        .addComponent(editModificationsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(availableModsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 124, Short.MAX_VALUE)
                .addContainerGap())
        );

        modProfilePanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {addModifications, removeModification});

        searchGuiParamsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("SearchGUI Parameters File"));
        searchGuiParamsPanel.setOpaque(false);

        jLabel4.setText("SearchGUI File");

        fileTxt.setEditable(false);

        loadButton.setText("Load");
        loadButton.setToolTipText("Load parameters from a SearchGUI parameters file");
        loadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout searchGuiParamsPanelLayout = new javax.swing.GroupLayout(searchGuiParamsPanel);
        searchGuiParamsPanel.setLayout(searchGuiParamsPanelLayout);
        searchGuiParamsPanelLayout.setHorizontalGroup(
            searchGuiParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(searchGuiParamsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(fileTxt)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(loadButton, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        searchGuiParamsPanelLayout.setVerticalGroup(
            searchGuiParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(searchGuiParamsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(searchGuiParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(fileTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(loadButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        helpLineLabel.setFont(helpLineLabel.getFont().deriveFont((helpLineLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        helpLineLabel.setText("Edit the search parameters and the modification profile and click OK to save.");

        searchPreferencesHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help.GIF"))); // NOI18N
        searchPreferencesHelpJButton.setToolTipText("Help");
        searchPreferencesHelpJButton.setBorder(null);
        searchPreferencesHelpJButton.setBorderPainted(false);
        searchPreferencesHelpJButton.setContentAreaFilled(false);
        searchPreferencesHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                searchPreferencesHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                searchPreferencesHelpJButtonMouseExited(evt);
            }
        });
        searchPreferencesHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchPreferencesHelpJButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(modProfilePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(searchPreferencesHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(helpLineLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(185, 185, 185)
                        .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton))
                    .addComponent(enzymeAndFragmentIonsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(searchGuiParamsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        backgroundPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(enzymeAndFragmentIonsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(modProfilePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(searchGuiParamsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(helpLineLabel)
                    .addComponent(searchPreferencesHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(okButton)
                    .addComponent(cancelButton))
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
     * Saves the settings and closes the dialog.
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        if (validateInput()) {
            //@TODO: the displayed data ought to be updated here if any change was made
            searchParameters.setFragmentIonAccuracy(new Double(fragmentIonAccuracyTxt.getText()));
            searchParameters.setnMissedCleavages(new Integer(missedCleavagesTxt.getText()));
            searchParameters.setEnzyme(enzymeFactory.getEnzyme((String) enzymesCmb.getSelectedItem()));
            searchParameters.setIonSearched1((String) ion1Cmb.getSelectedItem());
            searchParameters.setIonSearched2((String) ion2Cmb.getSelectedItem());

            if (((String) precursorUnit.getSelectedItem()).equalsIgnoreCase("ppm")) {
                searchParameters.setPrecursorAccuracyType(SearchParameters.PrecursorAccuracyType.PPM);
            } else { // Da
                searchParameters.setPrecursorAccuracyType(SearchParameters.PrecursorAccuracyType.DA);
            }

            searchParameters.setPrecursorAccuracy(new Double(precursorAccuracy.getText()));

            if (!searchParameters.getEnzyme().enzymeCleaves()) {

                // create an empty label to put the message in
                JLabel label = new JLabel();

                // html content 
                JEditorPane ep = new JEditorPane("text/html", "<html><body bgcolor=\"#" + Util.color2Hex(label.getBackground()) + "\">"
                        + "The cleavage site of the selected enzyme is not compatible with all PeptideShaker functionalities.<br><br>"
                        + "For more information on enzymes, contact us via:<br>"
                        + "<a href=\"http://groups.google.com/group/peptide-shaker\">http://groups.google.com/group/peptide-shaker</a>."
                        + "</body></html>");

                // handle link events 
                ep.addHyperlinkListener(new HyperlinkListener() {

                    @Override
                    public void hyperlinkUpdate(HyperlinkEvent e) {
                        if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                            BareBonesBrowserLaunch.openURL(e.getURL().toString());
                        }
                    }
                });

                ep.setBorder(null);
                ep.setEditable(false);

                JOptionPane.showMessageDialog(this, ep, "Enzyme Not Configured", JOptionPane.WARNING_MESSAGE);
            }

            this.dispose();
        }
    }//GEN-LAST:event_okButtonActionPerformed

    /**
     * Closes the dialog.
     *
     * @param evt
     */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        canceled = true;
        this.dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    /**
     * Adds a modification to the list.
     *
     * @param evt
     */
    private void addModificationsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addModificationsActionPerformed

        Double fragmentIonAccuracy = null;
        try {
            fragmentIonAccuracy = new Double(fragmentIonAccuracyTxt.getText().trim());
        } catch (Exception e) {
            
        }
        if (fragmentIonAccuracy == null) {
                JOptionPane.showMessageDialog(this, "Please set the fragment ion accuracy.", "Missing Fragment Accuracy", JOptionPane.WARNING_MESSAGE);
                return;
        }
        int[] selectedRows = availableModificationsTable.getSelectedRows();

        for (int i = selectedRows.length - 1; i >= 0; i--) {
            String name = (String) availableModificationsTable.getValueAt(selectedRows[i], 1);
            ModificationProfile modificationProfile = searchParameters.getModificationProfile();
            ArrayList<String> notFixedModifications = modificationProfile.getAllNotFixedModifications();
            if (notFixedModifications.contains(name)) {
                int choice = JOptionPane.showConfirmDialog(this,
                        new String[]{"The list of expected variable modifications already contains a modification named " + name + ".", "Shall it be replaced?"},
                        "Modification Name Conflict", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            PTM ptm = ptmFactory.getPTM(name);
            ArrayList<String> conflicts = new ArrayList<String>();
            for (String oldName : notFixedModifications) {
                PTM oldPTM = ptmFactory.getPTM(oldName);
                if (Math.abs(oldPTM.getMass() - ptm.getMass()) < fragmentIonAccuracy
                        && oldPTM.getPattern().isSameAs(ptm.getPattern())) {
                    conflicts.add(oldName);
                }
            }
            if (conflicts.size() == 1) {
                int choice = JOptionPane.showConfirmDialog(this,
                        new String[]{name + " will be impossible to distinguish from " + conflicts.get(0) + ".", "Shall it be replaced?"},
                        "Modification Name Conflict", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.NO_OPTION) {
                    return;
                } else {
                    modificationProfile.removeVariableModification(conflicts.get(0));
                    modificationProfile.removeRefinementModification(conflicts.get(0));
                }
            } else if (conflicts.size() > 1) {
                String report = name + " will be impossible to distinguish from ";
                Collections.sort(conflicts);
                for (int j = 0; j < conflicts.size(); j++) {
                    if (j == conflicts.size() - 1) {
                        report += " and ";
                    } else if (j > 0) {
                        report += ", ";
                    }
                    report += conflicts.get(j);
                }
                report += ".";
                int choice = JOptionPane.showConfirmDialog(this,
                        new String[]{report, "Shall they be replaced?"},
                        "Modification Name Conflict", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.NO_OPTION) {
                    return;
                } else {
                    for (String mod : conflicts) {
                        modificationProfile.removeVariableModification(mod);
                        modificationProfile.removeRefinementModification(mod);
                    }
                }
            }
            modificationProfile.addVariableModification(ptm);
            modificationList.add(name);
        }

        updateModificationLists();
    }//GEN-LAST:event_addModificationsActionPerformed

    /**
     * Removes a modification from the list.
     *
     * @param evt
     */
    private void removeModificationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeModificationActionPerformed

        ArrayList<String> toRemove = new ArrayList<String>();

        int[] selectedRows = expectedModificationsTable.getSelectedRows();
        for (int selectedRow : selectedRows) {
            toRemove.add((String) expectedModificationsTable.getValueAt(selectedRow, 2));
        }

        for (String name : toRemove) {
            modificationList.remove(name);
            searchParameters.getModificationProfile().removeVariableModification(name);
        }

        updateModificationLists();
    }//GEN-LAST:event_removeModificationActionPerformed

    /**
     * Loads the search preferences from a SearchGUI file.
     *
     * @param evt
     */
    private void loadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadButtonActionPerformed
       
        String currentPath = peptideShakerGUI.getLastSelectedFolder();

        if (new File(fileTxt.getText()).exists()) {
            currentPath = fileTxt.getText();
        }

        JFileChooser fc = new JFileChooser(currentPath);

        FileFilter filter = new FileFilter() {

            @Override
            public boolean accept(File myFile) {
                return myFile.getName().toLowerCase().endsWith("parameters") || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "(SearchGUI properties file) *.parameters";
            }
        };

        fc.setFileFilter(filter);

        int result = fc.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            loadSearchParameters(file);
            peptideShakerGUI.setLastSelectedFolder(file.getAbsolutePath());
        }
    }//GEN-LAST:event_loadButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void searchPreferencesHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_searchPreferencesHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_searchPreferencesHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void searchPreferencesHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_searchPreferencesHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_searchPreferencesHelpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void searchPreferencesHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchPreferencesHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(this, getClass().getResource("/helpFiles/SearchPreferencesDialog.html"),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PeptideShaker - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_searchPreferencesHelpJButtonActionPerformed

    /**
     * Changes the cursor to a hand cursor if over the color or PSI-MOD column.
     *
     * @param evt
     */
    private void expectedModificationsTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_expectedModificationsTableMouseMoved

        int row = expectedModificationsTable.rowAtPoint(evt.getPoint());
        int column = expectedModificationsTable.columnAtPoint(evt.getPoint());

        if (row != -1) {

            if (column == expectedModificationsTable.getColumn("  ").getModelIndex()) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else if (column == expectedModificationsTable.getColumn("PSI-MOD").getModelIndex() && expectedModificationsTable.getValueAt(row, column) != null) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        }
    }//GEN-LAST:event_expectedModificationsTableMouseMoved

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void expectedModificationsTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_expectedModificationsTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_expectedModificationsTableMouseExited

    /**
     * Opens a file chooser where the color for the PTM can be changed.
     *
     * @param evt
     */
    private void expectedModificationsTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_expectedModificationsTableMouseReleased
        int row = expectedModificationsTable.rowAtPoint(evt.getPoint());
        int column = expectedModificationsTable.columnAtPoint(evt.getPoint());

        if (row != -1) {
            int ptmIndex = expectedModificationsTable.convertRowIndexToModel(row);
            String modificationName = modificationList.get(ptmIndex);
            if (column == expectedModificationsTable.getColumn("  ").getModelIndex()) {
                Color newColor = JColorChooser.showDialog(this, "Pick a Color", (Color) expectedModificationsTable.getValueAt(ptmIndex, column));

                if (newColor != null) {
                    searchParameters.getModificationProfile().setColor(modificationName, newColor);
                    expectedModificationsTable.repaint();
                }
            } else if (column == expectedModificationsTable.getColumn("PSI-MOD").getModelIndex()) {
                // open protein link in web browser
                if (column == expectedModificationsTable.getColumn("PSI-MOD").getModelIndex() && evt.getButton() == MouseEvent.BUTTON1) {
                    if (((String) expectedModificationsTable.getValueAt(ptmIndex, column)).lastIndexOf("<html>") != -1) {
                        String link = (String) expectedModificationsTable.getValueAt(ptmIndex, column);
                        link = link.substring(link.indexOf("\"") + 1);
                        link = link.substring(0, link.indexOf("\""));

                        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                        BareBonesBrowserLaunch.openURL(link);
                        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                    } else {
                        boolean userMod = ptmFactory.isUserDefined(modificationList.get(ptmIndex))
                                && ptmFactory.isUserDefined(modificationName);

                        new PtmDialog(this, this, ptmToPrideMap, ptmFactory.getPTM(modificationName), userMod);
                    }
                }
            }
        }
    }//GEN-LAST:event_expectedModificationsTableMouseReleased

    /**
     * Changes the cursor to a hand cursor if over the color or PSI-MOD column.
     *
     * @param evt
     */
    private void availableModificationsTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_availableModificationsTableMouseMoved
        int row = availableModificationsTable.rowAtPoint(evt.getPoint());
        int column = availableModificationsTable.columnAtPoint(evt.getPoint());

        if (row != -1) {
            if (column == availableModificationsTable.getColumn("PSI-MOD").getModelIndex() && availableModificationsTable.getValueAt(row, column) != null) {

                String tempValue = (String) availableModificationsTable.getValueAt(row, column);

                if (tempValue.lastIndexOf("<html>") != -1) {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                } else {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }

            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        }
    }//GEN-LAST:event_availableModificationsTableMouseMoved

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void availableModificationsTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_availableModificationsTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_availableModificationsTableMouseExited

    /**
     * Opens a file chooser where the color for the PTM can be changed.
     *
     * @param evt
     */
    private void availableModificationsTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_availableModificationsTableMouseReleased
        int row = availableModificationsTable.rowAtPoint(evt.getPoint());
        int column = availableModificationsTable.columnAtPoint(evt.getPoint());

        if (row != -1) {
            int ptmIndex = availableModificationsTable.convertRowIndexToModel(row);

            if (column == availableModificationsTable.getColumn("PSI-MOD").getModelIndex()) {

                String modificationName = modificationList.get(ptmIndex);

                // open protein link in web browser
                if (column == availableModificationsTable.getColumn("PSI-MOD").getModelIndex() && evt.getButton() == MouseEvent.BUTTON1
                        && ((String) availableModificationsTable.getValueAt(row, column)).lastIndexOf("<html>") != -1) {
                    if (((String) availableModificationsTable.getValueAt(ptmIndex, column)).lastIndexOf("<html>") != -1) {
                        String link = (String) availableModificationsTable.getValueAt(ptmIndex, column);
                        link = link.substring(link.indexOf("\"") + 1);
                        link = link.substring(0, link.indexOf("\""));

                        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                        BareBonesBrowserLaunch.openURL(link);
                        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                    } else {
                        boolean userMod = ptmFactory.isUserDefined(modificationName);

                        new PtmDialog(this, this, ptmToPrideMap, ptmFactory.getPTM(modificationName), userMod);
                    }
                }
            }
        }
    }//GEN-LAST:event_availableModificationsTableMouseReleased

    /**
     * Show the edit ptm popup menu.
     *
     * @param evt
     */
    private void expectedModificationsTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_expectedModificationsTableMouseClicked
        if ((evt.getClickCount() == 1 && evt.getButton() == MouseEvent.BUTTON3 && expectedModificationsTable.rowAtPoint(evt.getPoint()) != -1)) {

            boolean rowAlreadySelected = false;

            int[] selectedRows = expectedModificationsTable.getSelectedRows();

            for (int i = 0; i < selectedRows.length && !rowAlreadySelected; i++) {
                if (selectedRows[i] == expectedModificationsTable.rowAtPoint(evt.getPoint())) {
                    rowAlreadySelected = true;
                }
            }

            if (!rowAlreadySelected) {
                expectedModificationsTable.setRowSelectionInterval(expectedModificationsTable.rowAtPoint(evt.getPoint()), expectedModificationsTable.rowAtPoint(evt.getPoint()));
            }

            expectedPtmPopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());

        } else if (evt.getClickCount() == 2 && evt.getButton() == MouseEvent.BUTTON1) {
            editExpectedPtmJMenuItemActionPerformed(null);
        }
    }//GEN-LAST:event_expectedModificationsTableMouseClicked

    /**
     * Open the edit ptm dialog.
     *
     * @param evt
     */
    private void editExpectedPtmJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editExpectedPtmJMenuItemActionPerformed
        int row = expectedModificationsTable.getSelectedRow();
        int ptmIndex = expectedModificationsTable.convertRowIndexToModel(row);

        String name = modificationList.get(ptmIndex);

        boolean userMod = ptmFactory.isUserDefined(name);

        new PtmDialog(this, this, ptmToPrideMap, ptmFactory.getPTM(name), userMod);
    }//GEN-LAST:event_editExpectedPtmJMenuItemActionPerformed

    /**
     * Show the edit ptm popup menu.
     *
     * @param evt
     */
    private void availableModificationsTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_availableModificationsTableMouseClicked
        if (evt.getClickCount() == 1 && evt.getButton() == MouseEvent.BUTTON3 && availableModificationsTable.rowAtPoint(evt.getPoint()) != -1) {

            boolean rowAlreadySelected = false;

            int[] selectedRows = availableModificationsTable.getSelectedRows();

            for (int i = 0; i < selectedRows.length && !rowAlreadySelected; i++) {
                if (selectedRows[i] == availableModificationsTable.rowAtPoint(evt.getPoint())) {
                    rowAlreadySelected = true;
                }
            }

            if (!rowAlreadySelected) {
                availableModificationsTable.setRowSelectionInterval(availableModificationsTable.rowAtPoint(evt.getPoint()), availableModificationsTable.rowAtPoint(evt.getPoint()));
            }

            availablePtmPopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());

        } else if (evt.getClickCount() == 2 && evt.getButton() == MouseEvent.BUTTON1) {
            editAvailablePtmJMenuItemActionPerformed(null);
        }
    }//GEN-LAST:event_availableModificationsTableMouseClicked

    /**
     * Open the edit ptm dialog.
     *
     * @param evt
     */
    private void editAvailablePtmJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editAvailablePtmJMenuItemActionPerformed
        int row = availableModificationsTable.getSelectedRow();

        if (row != -1) {
            String modName = (String) availableModificationsTable.getValueAt(row, availableModificationsTable.getColumn("Name").getModelIndex());
            boolean userMod = (Boolean) availableModificationsTable.getValueAt(row, availableModificationsTable.getColumn("U.M.").getModelIndex());
            new PtmDialog(this, this, ptmToPrideMap, ptmFactory.getPTM(modName), userMod);
        }
    }//GEN-LAST:event_editAvailablePtmJMenuItemActionPerformed

    /**
     * Remove selected ptms from the list of expected ptms.
     *
     * @param evt
     */
    private void removeExpectedPtmJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeExpectedPtmJMenuItemActionPerformed
        removeModificationActionPerformed(null);
    }//GEN-LAST:event_removeExpectedPtmJMenuItemActionPerformed

    /**
     * Add selected ptms to the list of expected ptms.
     *
     * @param evt
     */
    private void addAvailablePtmJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addAvailablePtmJMenuItemActionPerformed
        addModificationsActionPerformed(null);
    }//GEN-LAST:event_addAvailablePtmJMenuItemActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void editModificationsLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_editModificationsLabelMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_editModificationsLabelMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void editModificationsLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_editModificationsLabelMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_editModificationsLabelMouseExited

    /**
     * Open the modification overview dialog.
     * 
     * @param evt 
     */
    private void editModificationsLabelMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_editModificationsLabelMouseReleased
        new ModificationsDialog(peptideShakerGUI, this, true); 
    }//GEN-LAST:event_editModificationsLabelMouseReleased

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem addAvailablePtmJMenuItem;
    private javax.swing.JButton addModifications;
    private javax.swing.JTable availableModificationsTable;
    private javax.swing.JLabel availableModsLabel;
    private javax.swing.JScrollPane availableModsScrollPane;
    private javax.swing.JPopupMenu availablePtmPopupMenu;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JMenuItem editAvailablePtmJMenuItem;
    private javax.swing.JMenuItem editExpectedPtmJMenuItem;
    private javax.swing.JLabel editModificationsLabel;
    private javax.swing.JPanel enzymeAndFragmentIonsPanel;
    private javax.swing.JComboBox enzymesCmb;
    private javax.swing.JTable expectedModificationsTable;
    private javax.swing.JLabel expectedModsLabel;
    private javax.swing.JScrollPane expectedModsScrollPane;
    private javax.swing.JPopupMenu expectedPtmPopupMenu;
    private javax.swing.JTextField fileTxt;
    private javax.swing.JTextField fragmentIonAccuracyTxt;
    private javax.swing.JLabel helpLineLabel;
    private javax.swing.JComboBox ion1Cmb;
    private javax.swing.JComboBox ion2Cmb;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JButton loadButton;
    private javax.swing.JTextField missedCleavagesTxt;
    private javax.swing.JPanel modProfilePanel;
    private javax.swing.JButton okButton;
    private javax.swing.JTextField precursorAccuracy;
    private javax.swing.JComboBox precursorUnit;
    private javax.swing.JMenuItem removeExpectedPtmJMenuItem;
    private javax.swing.JButton removeModification;
    private javax.swing.JPanel searchGuiParamsPanel;
    private javax.swing.JButton searchPreferencesHelpJButton;
    // End of variables declaration//GEN-END:variables

    /**
     * Returns true if the input can be correctly imported.
     *
     * @return true if the input can be correctly imported
     */
    private boolean validateInput() {
        try {
            new Double(fragmentIonAccuracyTxt.getText());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please verify the input for fragment ion accuracy.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try {
            new Integer(missedCleavagesTxt.getText());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please verify the input for allowed missed cleavages.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try {
            new Double(precursorAccuracy.getText());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please verify the input for allowed missed cleavages.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    /**
     * This method takes the SearchParameters instance and reads the values for
     * the GUI components from it. Method inspired from SearchGUI, would be good
     * to have a unified panel.
     */
    public void setScreenProps() {

        ModificationProfile modificationProfile = searchParameters.getModificationProfile();

        modificationList = new ArrayList<String>();
        ArrayList<String> missing = new ArrayList<String>();

        for (String name : modificationProfile.getAllNotFixedModifications()) {
            if (!modificationList.contains(name)) {
                if (!ptmFactory.containsPTM(name)) {
                    missing.add(name);
                } else {
                    if (searchParameters.getModificationProfile().getColor(name) == null) {
                        searchParameters.getModificationProfile().setColor(name, Color.lightGray);
                    }
                    modificationList.add(name);
                }
            }
        }
        if (!missing.isEmpty()) {
            // Might happen with old parameters files
            if (missing.size() == 1) {
                JOptionPane.showMessageDialog(this, "The following modification is currently not recognized by PeptideShaker: "
                        + missing.get(0) + ".\nPlease import it in the modification panel.", "Modification Not Found", JOptionPane.WARNING_MESSAGE);
            } else {
                String output = "The following modifications are currently not recognized by PeptideShaker:\n";
                boolean first = true;
                for (String ptm : missing) {
                    if (first) {
                        first = false;
                    } else {
                        output += ", ";
                    }
                    output += ptm;
                }
                output += ".\nPlease import them in the modification panel.";
                JOptionPane.showMessageDialog(this, output, "Modification Not Found", JOptionPane.WARNING_MESSAGE);
            }
        }
        Collections.sort(modificationList);
        updateModificationLists();

        enzymesCmb.setModel(new DefaultComboBoxModel(loadEnzymes()));
        enzymesCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        if (searchParameters.getEnzyme() != null) {
            enzymesCmb.setSelectedItem(searchParameters.getEnzyme().getName());
        } else {
            enzymesCmb.setSelectedItem("Trypsin");
        }

        if (searchParameters.getFragmentIonAccuracy() != null) {
            fragmentIonAccuracyTxt.setText(searchParameters.getFragmentIonAccuracy() + "");
        }

        if (searchParameters.getnMissedCleavages() != null) {
            missedCleavagesTxt.setText(searchParameters.getnMissedCleavages() + "");
        }

        setIons();

        if (searchParameters.isPrecursorAccuracyTypePpm() != null) {
            if (searchParameters.isPrecursorAccuracyTypePpm()) {
                precursorUnit.setSelectedItem("ppm");
            } else {
                precursorUnit.setSelectedItem("Da");
            }
        }

        if (searchParameters.getPrecursorAccuracy() != null) {
            precursorAccuracy.setText(searchParameters.getPrecursorAccuracy() + "");
        }

        if (searchParameters.getParametersFile() != null) {

            // invoke later to give time for components to update
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {

                    fileTxt.setText(searchParameters.getParametersFile().getAbsolutePath());
                }
            });
        }
    }

    /**
     * Loads the implemented enzymes.
     *
     * @return the list of enzyme names
     */
    private String[] loadEnzymes() {

        ArrayList<String> tempEnzymes = new ArrayList<String>();

        for (int i = 0; i < enzymeFactory.getEnzymes().size(); i++) {
            tempEnzymes.add(enzymeFactory.getEnzymes().get(i).getName());
        }

        Collections.sort(tempEnzymes);

        String[] enzymes = new String[tempEnzymes.size()];

        for (int i = 0; i < tempEnzymes.size(); i++) {
            enzymes[i] = tempEnzymes.get(i);
        }

        return enzymes;
    }

    /**
     * Sets the selected ion types.
     */
    private void setIons() {
        if (searchParameters.getIonSearched1() != null) {
            if (searchParameters.getIonSearched1() == PeptideFragmentIon.A_ION) {
                ion1Cmb.setSelectedItem("a");
            } else if (searchParameters.getIonSearched1() == PeptideFragmentIon.B_ION) {
                ion1Cmb.setSelectedItem("b");
            } else if (searchParameters.getIonSearched1() == PeptideFragmentIon.C_ION) {
                ion1Cmb.setSelectedItem("c");
            } else if (searchParameters.getIonSearched1() == PeptideFragmentIon.X_ION) {
                ion1Cmb.setSelectedItem("x");
            } else if (searchParameters.getIonSearched1() == PeptideFragmentIon.Y_ION) {
                ion1Cmb.setSelectedItem("y");
            } else if (searchParameters.getIonSearched1() == PeptideFragmentIon.Z_ION) {
                ion1Cmb.setSelectedItem("z");
            }
        }

        if (searchParameters.getIonSearched2() != null) {
            if (searchParameters.getIonSearched2() == PeptideFragmentIon.A_ION) {
                ion2Cmb.setSelectedItem("a");
            } else if (searchParameters.getIonSearched2() == PeptideFragmentIon.B_ION) {
                ion2Cmb.setSelectedItem("b");
            } else if (searchParameters.getIonSearched2() == PeptideFragmentIon.C_ION) {
                ion2Cmb.setSelectedItem("c");
            } else if (searchParameters.getIonSearched2() == PeptideFragmentIon.X_ION) {
                ion2Cmb.setSelectedItem("x");
            } else if (searchParameters.getIonSearched2() == PeptideFragmentIon.Y_ION) {
                ion2Cmb.setSelectedItem("y");
            } else if (searchParameters.getIonSearched2() == PeptideFragmentIon.Z_ION) {
                ion2Cmb.setSelectedItem("z");
            }
        }
    }

    /**
     * Repaints the table.
     */
    private void repaintTable() {

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                expectedModificationsTable.revalidate();
                expectedModificationsTable.repaint();
            }
        });
    }

    /**
     * Loads the search parameters from a serialized object.
     *
     * @param file the file where the search parameters were saved
     */
    private void loadSearchParameters(File file) {
        try {
            searchParameters = SearchParameters.getIdentificationParameters(file);
            searchParameters.setParametersFile(file);
            PeptideShaker.loadModifications(searchParameters);
            setScreenProps();
            expectedModificationsTable.revalidate();
            expectedModificationsTable.repaint();
        } catch (Exception e) {
            try {
                // Old school format, overwrite old file
                Properties props = loadProperties(file);
                // We need the user mods file, try to see if it is along the search parameters or use the PeptideShaker version
                File userMods = new File(file.getParent(), "usermods.xml");
                if (!userMods.exists()) {
                    userMods = new File(peptideShakerGUI.getJarFilePath(), PeptideShaker.USER_MODIFICATIONS_FILE);
                }
                searchParameters = IdentificationParametersReader.getSearchParameters(props, userMods);
                setScreenProps();
                String fileName = file.getName();
                if (fileName.endsWith(".properties")) {
                    String newName = fileName.substring(0, fileName.lastIndexOf(".")) + ".parameters";
                    try {
                        file.delete();
                    } catch (Exception deleteException) {
                        deleteException.printStackTrace();
                    }
                    file = new File(file.getParentFile(), newName);
                }
                SearchParameters.saveIdentificationParameters(searchParameters, file);
            } catch (Exception saveException) {
                e.printStackTrace();
                saveException.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error occured while reading " + file + ". Please verify the search paramters.", "File error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * This method loads the necessary parameters for populating (part of) the
     * GUI from a properties file.
     *
     * @deprecated use SearchParameters instead
     * @param aFile File with the relevant properties file.
     * @return Properties with the loaded properties.
     */
    private Properties loadProperties(File aFile) {
        Properties screenProps = new Properties();
        try {
            FileInputStream fis = new FileInputStream(aFile);
            if (fis != null) {
                screenProps.load(fis);
                fis.close();
            } else {
                throw new IllegalArgumentException("Could not read the file you specified ('" + aFile.getAbsolutePath() + "').");
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            JOptionPane.showMessageDialog(this, new String[]{"Unable to read file: " + aFile.getName(), ioe.getMessage()}, "Error Reading File", JOptionPane.WARNING_MESSAGE);
        }
        return screenProps;
    }

    /**
     * Updates the modification list (bottom).
     */
    private void updateModificationLists() {

        ArrayList<String> allModifications = new ArrayList<String>();

        for (String name : ptmFactory.getPTMs()) {
            boolean found = false;

            for (String modification : modificationList) {
                if (modification.equals(name)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                allModifications.add(name);
            }
        }

        String[] allModificationsAsArray = new String[allModifications.size()];

        for (int i = 0; i < allModifications.size(); i++) {
            allModificationsAsArray[i] = allModifications.get(i);
        }

        Arrays.sort(allModificationsAsArray);

        DefaultTableModel dm = (DefaultTableModel) availableModificationsTable.getModel();
        dm.getDataVector().removeAllElements();
        dm.fireTableDataChanged();

        for (int i = 0; i < allModificationsAsArray.length; i++) {

            CvTerm cvTerm = null;
            String psiModMapping = null;
            if (ptmToPrideMap != null) {
                cvTerm = ptmToPrideMap.getCVTerm(allModificationsAsArray[i]);
            }
            if (cvTerm != null) {
                psiModMapping = getOlsAccessionLink(cvTerm.getAccession());
            } else {
                cvTerm = PtmToPrideMap.getDefaultCVTerm(allModificationsAsArray[i]);
            }

            if (cvTerm != null) {
                psiModMapping = getOlsAccessionLink(cvTerm.getAccession());
            }


            ((DefaultTableModel) availableModificationsTable.getModel()).addRow(new Object[]{
                        (i + 1),
                        allModificationsAsArray[i],
                        ptmFactory.isUserDefined(allModificationsAsArray[i]),
                        psiModMapping
                    });
        }

        addModifications.setEnabled(availableModificationsTable.getRowCount() > 0);
        removeModification.setEnabled(expectedModificationsTable.getRowCount() > 0);

        repaintTable();
    }

    /**
     * Table model for the modification table.
     */
    private class ModificationTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return modificationList.size();
        }

        @Override
        public int getColumnCount() {
            return 6;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 1:
                    return "  ";
                case 2:
                    return "Name";
                case 3:
                    return "Short";
                case 4:
                    return "U.M.";
                case 5:
                    return "PSI-MOD";
                default:
                    return " ";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            String modificationName = modificationList.get(row);
            switch (column) {
                case 0:
                    return row + 1;
                case 1:
                    return searchParameters.getModificationProfile().getColor(modificationName);
                case 2:
                    return modificationList.get(row);
                case 3:
                    return ptmFactory.getShortName(modificationName);
                case 4:
                    return ptmFactory.isUserDefined(modificationName);
                case 5:
                    CvTerm cvTerm = null;
                    if (ptmToPrideMap != null) {
                        cvTerm = ptmToPrideMap.getCVTerm(modificationName);
                    }
                    if (cvTerm == null) {
                        cvTerm = PtmToPrideMap.getDefaultCVTerm(modificationName);
                        if (cvTerm != null) {
                            ptmToPrideMap.putCVTerm(modificationName, cvTerm);
                        }
                    }
                    if (cvTerm != null) {
                        return getOlsAccessionLink(cvTerm.getAccession());
                    }
                    return "";
                default:
                    return "";
            }
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            try {
                String modificationName = modificationList.get(row);
                if (column == 3) {
                    ptmFactory.setShortName(modificationName, aValue.toString().trim());
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for " + modificationList.get(row) + " occurrence.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
            }
            repaintTable();
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            for (int i = 0; i < getRowCount(); i++) {
                if (getValueAt(i, columnIndex) != null) {
                    return getValueAt(i, columnIndex).getClass();
                }
            }
            return (new Double(0.0)).getClass();
        }

        @Override
        public boolean isCellEditable(int row, int column) {

            if (column == 1 || column == 3) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Returns the protein accession number as a web link to the given PSI-MOD
     * at http://www.ebi.ac.uk/ontology-lookup.
     *
     * @param modAccession the PSI-MOD accession number
     * @return the OLS web link
     */
    public String getOlsAccessionLink(String modAccession) {
        String accessionNumberWithLink = "<html><a href=\"http://www.ebi.ac.uk/ontology-lookup/?termId=" + modAccession + "\""
                + "\"><font color=\"" + notSelectedRowHtmlTagFontColor + "\">"
                + modAccession + "</font></a></html>";
        return accessionNumberWithLink;
    }

    /**
     * Updates the PTM to pride map.
     *
     * @throws FileNotFoundException exception thrown whenever the map was not
     * found in the user folder
     * @throws IOException exception thrown whenever an error occurred while
     * writing the map
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing the map
     */
    public void updatePtmToPrideMap() throws FileNotFoundException, IOException, ClassNotFoundException {
        PrideObjectsFactory prideObjectsFactory = PrideObjectsFactory.getInstance();
        prideObjectsFactory.setPtmToPrideMap(ptmToPrideMap);
    }

    /**
     * Indicates whether the user pushed on cancel.
     *
     * @return a boolean indicating whether the user pushed on cancel
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Returns the search parameters as set by the user.
     *
     * @return the search parameters as set by the user
     */
    public SearchParameters getSearchParameters() {
        return searchParameters;
    }

    @Override
    public void updateModifications() {
        updateModificationLists();
    }
}
