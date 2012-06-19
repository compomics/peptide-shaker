package eu.isas.peptideshaker.gui.pride;

import com.compomics.util.Util;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.pride.prideobjects.Reference;
import com.compomics.util.pride.prideobjects.Contact;
import com.compomics.util.pride.prideobjects.Sample;
import com.compomics.util.pride.prideobjects.Instrument;
import com.compomics.util.pride.prideobjects.Protocol;
import com.compomics.util.pride.PtmToPrideMap;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.pride.PrideObjectsFactory;
import eu.isas.peptideshaker.export.PRIDEExport;
import eu.isas.peptideshaker.gui.HelpDialog;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.gui.preferencesdialogs.SearchPreferencesDialog;
import eu.isas.peptideshaker.gui.tabpanels.PtmPanel;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 * A simple dialog where the user can select
 *
 * @author Harald Barsnes
 */
public class PrideExportDialog extends javax.swing.JDialog {

    /**
     * A simple progress dialog.
     */
    private static ProgressDialogX progressDialog;
    /**
     * The PeptideShakerGUI main class.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The references table column tool tips.
     */
    private Vector referenceTableColumnToolTips;
    /**
     * The ptm to pride map.
     */
    private PrideObjectsFactory prideObjectsFactory = null;

    /**
     * Create a new PrideExportDialog.
     *
     * @param peptideShakerGUI a refereence to the main GUI frame
     * @param modal
     */
    public PrideExportDialog(PeptideShakerGUI peptideShakerGUI, boolean modal) {
        super(peptideShakerGUI, modal);
        this.peptideShakerGUI = peptideShakerGUI;

        // update the ptm
        updatePtmMap();

        initComponents();

        // set gui properties
        setGuiProperties();

        // insert project data
        insertProjectData();

        // insert references
//        for (Reference reference : Reference.getDefaultReferences()) {
//            ((DefaultTableModel) referencesJTable.getModel()).addRow(new Object[]{
//                        referencesJTable.getRowCount() + 1,
//                        reference.getReference(),
//                        reference.getPmid(),
//                        reference.getDoi()
//                    });
//        }

        // insert available contacts, instruments, protocols and samples
        insertOptions(new ArrayList<String>(prideObjectsFactory.getContacts().keySet()), "--- Select a Contact ---", "   Create a New Contact...", contactsJComboBox);
        insertOptions(new ArrayList<String>(prideObjectsFactory.getSamples().keySet()), "--- Select a Sample Set ---", "   Create a New Sample Set...", sampleJComboBox);
        insertOptions(new ArrayList<String>(prideObjectsFactory.getProtocols().keySet()), "--- Select a Protocol ---", "   Create a New Protocol...", protocolJComboBox);
        insertOptions(new ArrayList<String>(prideObjectsFactory.getInstruments().keySet()), "--- Select an Instrument ---", "   Create a New Instrument...", instrumentJComboBox);

        // validate the input
        validateInput();

        setLocationRelativeTo(peptideShakerGUI);
        setVisible(true);
    }

    /**
     * Set the GUI properties.
     */
    private void setGuiProperties() {
        referenceTableColumnToolTips = new Vector();
        referenceTableColumnToolTips.add(null);
        referenceTableColumnToolTips.add("The reference tag");
        referenceTableColumnToolTips.add("PubMed ID");
        referenceTableColumnToolTips.add("Digital Object Identifier");

        referencesJScrollPane.getViewport().setOpaque(false);
        referencesJTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        referencesJTable.getTableHeader().setReorderingAllowed(false);
        referencesJTable.getColumn(" ").setMaxWidth(30);
        referencesJTable.getColumn(" ").setMinWidth(30);

        contactsJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        sampleJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        protocolJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        instrumentJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
    }

    /**
     * Update the ptm map.
     */
    private void updatePtmMap() {
        try {
            prideObjectsFactory = PrideObjectsFactory.getInstance();
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            return;
        }
        peptideShakerGUI.loadPrideToPtmMap();
        ArrayList<String> missingMods = checkModifications();
        if (!missingMods.isEmpty()) {
            String report = "Pride CV term missing for the following modifications:\n";
            boolean first = true;
            for (String mod : missingMods) {
                if (first) {
                    first = false;
                } else {
                    report += ",\n";
                }
                report += mod;
            }
            report += ".\nPlease add a CV term by clicking on the corresponding case in the PTM table.";
            JOptionPane.showMessageDialog(peptideShakerGUI, report, "PTM CV Term(s) Missing.", JOptionPane.WARNING_MESSAGE);
            new SearchPreferencesDialog(peptideShakerGUI, true);
        }
    }

    /**
     * Verifies that all modifications have a PRIDE CV term.
     *
     * @return a boolean indicating that PTMs are configured correctly
     */
    private ArrayList<String> checkModifications() {
        ArrayList<String> missingTerm = new ArrayList<String>();
        PtmToPrideMap ptmToPrideMap = prideObjectsFactory.getPtmToPrideMap();
        for (String modification : peptideShakerGUI.getFoundModifications()) {
            if (!modification.equals(PtmPanel.NO_MODIFICATION) && ptmToPrideMap.getCVTerm(modification) == null) {
                missingTerm.add(modification);
            }
        }
        return missingTerm;
    }

    /**
     * Insert the available project data.
     */
    private void insertProjectData() {
        titleJTextField.setText(peptideShakerGUI.getExperiment().getReference());
    }

    /**
     * Insert the contact, sample, protocol and instrument options.
     *
     * @param optionsPath the path to the option files, e.g., contacts
     * @param fileEnding the file ending, e.g., .con
     * @param selectText the text to use for the select item, e.g., --- Select a
     * Contact ---
     * @param insertNewText the text to use for the new item, e.g., Create a New
     * Contact...
     * @param optionComboBox the combo box to add the options to
     */
    private void insertOptions(ArrayList<String> options, String selectText, String insertNewText, JComboBox optionComboBox) {

        java.util.Collections.sort(options);

        Vector comboboxTooltips = new Vector();
        comboboxTooltips.add(null);

        for (int i = 0; i < options.size(); i++) {
            comboboxTooltips.add(options.get(i));
        }

        comboboxTooltips.add(null);
        optionComboBox.setRenderer(new MyComboBoxRenderer(comboboxTooltips, SwingConstants.CENTER));
        options.add(0, selectText);
        options.add(insertNewText);
        optionComboBox.setModel(new DefaultComboBoxModel(options.toArray()));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        referencesPopupJMenu = new javax.swing.JPopupMenu();
        refEditJMenuItem = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JSeparator();
        refMoveUpJMenuItem = new javax.swing.JMenuItem();
        refMoveDownJMenuItem = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JSeparator();
        refDeleteSelectedRowJMenuItem = new javax.swing.JMenuItem();
        backgroundJPanel = new javax.swing.JPanel();
        experimentPropertiesPanel = new javax.swing.JPanel();
        experimentPropertiesLabel = new javax.swing.JLabel();
        titleJTextField = new javax.swing.JTextField();
        experimentLabel = new javax.swing.JLabel();
        labelJTextField = new javax.swing.JTextField();
        projectLabel = new javax.swing.JLabel();
        projectJTextField = new javax.swing.JTextField();
        descriptionLabel = new javax.swing.JLabel();
        descriptionJScrollPane = new javax.swing.JScrollPane();
        descriptionJTextArea = new javax.swing.JTextArea();
        referencesLabel = new javax.swing.JLabel();
        addReferencesJButton = new javax.swing.JButton();
        contactLabel = new javax.swing.JLabel();
        contactsJComboBox = new javax.swing.JComboBox();
        editContactJButton = new javax.swing.JButton();
        sampleLabel = new javax.swing.JLabel();
        sampleJComboBox = new javax.swing.JComboBox();
        editSampleJButton = new javax.swing.JButton();
        protocolLabel = new javax.swing.JLabel();
        protocolJComboBox = new javax.swing.JComboBox();
        editProtocolJButton = new javax.swing.JButton();
        instrumentLabel = new javax.swing.JLabel();
        instrumentJComboBox = new javax.swing.JComboBox();
        editInstrumentJButton = new javax.swing.JButton();
        referencesJScrollPane = new javax.swing.JScrollPane();
        referencesJTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) referenceTableColumnToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        convertJButton = new javax.swing.JButton();
        openDialogHelpJButton = new javax.swing.JButton();
        helpLabel = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        outpitFolderLabel = new javax.swing.JLabel();
        outputFolderJTextField = new javax.swing.JTextField();
        browseOutputFolderJButton = new javax.swing.JButton();

        refEditJMenuItem.setMnemonic('E');
        refEditJMenuItem.setText("Edit");
        refEditJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refEditJMenuItemActionPerformed(evt);
            }
        });
        referencesPopupJMenu.add(refEditJMenuItem);
        referencesPopupJMenu.add(jSeparator5);

        refMoveUpJMenuItem.setMnemonic('U');
        refMoveUpJMenuItem.setText("Move Up");
        refMoveUpJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refMoveUpJMenuItemActionPerformed(evt);
            }
        });
        referencesPopupJMenu.add(refMoveUpJMenuItem);

        refMoveDownJMenuItem.setMnemonic('D');
        refMoveDownJMenuItem.setText("Move Down");
        refMoveDownJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refMoveDownJMenuItemActionPerformed(evt);
            }
        });
        referencesPopupJMenu.add(refMoveDownJMenuItem);
        referencesPopupJMenu.add(jSeparator6);

        refDeleteSelectedRowJMenuItem.setMnemonic('L');
        refDeleteSelectedRowJMenuItem.setText("Delete");
        refDeleteSelectedRowJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refDeleteSelectedRowJMenuItemActionPerformed(evt);
            }
        });
        referencesPopupJMenu.add(refDeleteSelectedRowJMenuItem);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("PeptideShaker - PRIDE Export");
        setResizable(false);

        backgroundJPanel.setBackground(new java.awt.Color(230, 230, 230));

        experimentPropertiesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Experiment Properties"));
        experimentPropertiesPanel.setOpaque(false);

        experimentPropertiesLabel.setForeground(new java.awt.Color(255, 0, 0));
        experimentPropertiesLabel.setText("Title*");
        experimentPropertiesLabel.setToolTipText("The title of the project");

        titleJTextField.setToolTipText("The title of the project");
        titleJTextField.setMargin(new java.awt.Insets(2, 4, 2, 2));
        titleJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                titleJTextFieldKeyReleased(evt);
            }
        });

        experimentLabel.setForeground(new java.awt.Color(255, 0, 0));
        experimentLabel.setText("Label*");
        experimentLabel.setToolTipText("A (short) label for the project");

        labelJTextField.setToolTipText("A (short) label for the project");
        labelJTextField.setMargin(new java.awt.Insets(2, 4, 2, 2));
        labelJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                labelJTextFieldKeyReleased(evt);
            }
        });

        projectLabel.setForeground(new java.awt.Color(255, 0, 0));
        projectLabel.setText("Project*");
        projectLabel.setToolTipText("Allows experiments to be grouped or organized under a projects");

        projectJTextField.setToolTipText("Allows experiments to be grouped or organized under a projects");
        projectJTextField.setMargin(new java.awt.Insets(2, 4, 2, 2));
        projectJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                projectJTextFieldKeyReleased(evt);
            }
        });

        descriptionLabel.setForeground(new java.awt.Color(255, 0, 0));
        descriptionLabel.setText("Description*");
        descriptionLabel.setToolTipText("A general free-text description of the experiment");

        descriptionJTextArea.setColumns(10);
        descriptionJTextArea.setLineWrap(true);
        descriptionJTextArea.setRows(2);
        descriptionJTextArea.setToolTipText("A general free-text description of the experiment");
        descriptionJTextArea.setWrapStyleWord(true);
        descriptionJTextArea.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                descriptionJTextAreaKeyReleased(evt);
            }
        });
        descriptionJScrollPane.setViewportView(descriptionJTextArea);

        referencesLabel.setText("References");
        referencesLabel.setToolTipText("<html>\nReferences to publications (if any).<br>\nRight click in the table to edit.\n</html>");

        addReferencesJButton.setText("Add");
        addReferencesJButton.setToolTipText("Add a new reference.");
        addReferencesJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addReferencesJButtonActionPerformed(evt);
            }
        });

        contactLabel.setForeground(new java.awt.Color(255, 0, 0));
        contactLabel.setText("Contact*");
        contactLabel.setToolTipText("The contact person for the PRIDE dataset");

        contactsJComboBox.setMaximumRowCount(20);
        contactsJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "--- Select ---", "Item 2", "Item 3", "Item 4" }));
        contactsJComboBox.setToolTipText("The contact person for the PRIDE dataset");
        contactsJComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contactsJComboBoxActionPerformed(evt);
            }
        });

        editContactJButton.setText("Edit");
        editContactJButton.setToolTipText("Edit the selected contact");
        editContactJButton.setEnabled(false);
        editContactJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editContactJButtonActionPerformed(evt);
            }
        });

        sampleLabel.setForeground(new java.awt.Color(255, 0, 0));
        sampleLabel.setText("Sample*");
        sampleLabel.setToolTipText("Details about the sample used");

        sampleJComboBox.setMaximumRowCount(20);
        sampleJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "--- Select ---", "Item 2", "Item 3", "Item 4" }));
        sampleJComboBox.setToolTipText("Details about the sample used");
        sampleJComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sampleJComboBoxActionPerformed(evt);
            }
        });

        editSampleJButton.setText("Edit");
        editSampleJButton.setToolTipText("Edit the selected sample");
        editSampleJButton.setEnabled(false);
        editSampleJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editSampleJButtonActionPerformed(evt);
            }
        });

        protocolLabel.setForeground(new java.awt.Color(255, 0, 0));
        protocolLabel.setText("Protocol*");
        protocolLabel.setToolTipText("Details about the protocol used");

        protocolJComboBox.setMaximumRowCount(20);
        protocolJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "--- Select ---", "Item 2", "Item 3", "Item 4" }));
        protocolJComboBox.setToolTipText("Details about the protocol used");
        protocolJComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolJComboBoxActionPerformed(evt);
            }
        });

        editProtocolJButton.setText("Edit");
        editProtocolJButton.setToolTipText("Edit the selected protocol");
        editProtocolJButton.setEnabled(false);
        editProtocolJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editProtocolJButtonActionPerformed(evt);
            }
        });

        instrumentLabel.setForeground(new java.awt.Color(255, 0, 0));
        instrumentLabel.setText("Instrument*");
        instrumentLabel.setToolTipText("Details about the instrument used");

        instrumentJComboBox.setMaximumRowCount(20);
        instrumentJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "--- Select ---", "Item 2", "Item 3", "Item 4" }));
        instrumentJComboBox.setToolTipText("Details about the instrument used");
        instrumentJComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                instrumentJComboBoxActionPerformed(evt);
            }
        });

        editInstrumentJButton.setText("Edit");
        editInstrumentJButton.setToolTipText("Edit the selected instrument");
        editInstrumentJButton.setEnabled(false);
        editInstrumentJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editInstrumentJButtonActionPerformed(evt);
            }
        });

        referencesJScrollPane.setToolTipText("<html>\nReferences to publications (if any).<br>\nRight click in the table to edit.\n</html>");

        referencesJTable.setFont(referencesJTable.getFont());
        referencesJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Reference", "PMID", "DOI"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
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
        referencesJTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                referencesJTableMouseClicked(evt);
            }
        });
        referencesJTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                referencesJTableKeyReleased(evt);
            }
        });
        referencesJScrollPane.setViewportView(referencesJTable);

        javax.swing.GroupLayout experimentPropertiesPanelLayout = new javax.swing.GroupLayout(experimentPropertiesPanel);
        experimentPropertiesPanel.setLayout(experimentPropertiesPanelLayout);
        experimentPropertiesPanelLayout.setHorizontalGroup(
            experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(experimentPropertiesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(experimentPropertiesPanelLayout.createSequentialGroup()
                        .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(experimentPropertiesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(projectLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(descriptionLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(referencesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(descriptionJScrollPane)
                            .addComponent(projectJTextField)
                            .addGroup(experimentPropertiesPanelLayout.createSequentialGroup()
                                .addComponent(titleJTextField)
                                .addGap(18, 18, 18)
                                .addComponent(experimentLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(labelJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, experimentPropertiesPanelLayout.createSequentialGroup()
                                .addComponent(referencesJScrollPane)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(addReferencesJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, experimentPropertiesPanelLayout.createSequentialGroup()
                        .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(instrumentLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(contactLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sampleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(protocolLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(instrumentJComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(protocolJComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(sampleJComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(contactsJComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(editContactJButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(editSampleJButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(editProtocolJButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(editInstrumentJButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        experimentPropertiesPanelLayout.setVerticalGroup(
            experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(experimentPropertiesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(experimentPropertiesLabel)
                    .addComponent(titleJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(experimentLabel)
                    .addComponent(labelJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(projectLabel)
                    .addComponent(projectJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(descriptionLabel)
                    .addComponent(descriptionJScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(referencesLabel)
                    .addComponent(referencesJScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(addReferencesJButton))
                .addGap(18, 18, Short.MAX_VALUE)
                .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(contactLabel)
                    .addComponent(contactsJComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(editContactJButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sampleLabel)
                    .addComponent(sampleJComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(editSampleJButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(protocolLabel)
                    .addComponent(protocolJComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(editProtocolJButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(instrumentLabel)
                    .addComponent(instrumentJComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(editInstrumentJButton))
                .addContainerGap())
        );

        experimentPropertiesPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {contactsJComboBox, editContactJButton});

        experimentPropertiesPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {editSampleJButton, sampleJComboBox});

        experimentPropertiesPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {editProtocolJButton, protocolJComboBox});

        experimentPropertiesPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {editInstrumentJButton, instrumentJComboBox});

        convertJButton.setBackground(new java.awt.Color(0, 153, 0));
        convertJButton.setFont(convertJButton.getFont().deriveFont(convertJButton.getFont().getStyle() | java.awt.Font.BOLD));
        convertJButton.setForeground(new java.awt.Color(255, 255, 255));
        convertJButton.setText("Convert!");
        convertJButton.setToolTipText("Click here to start the conversion!");
        convertJButton.setEnabled(false);
        convertJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                convertJButtonActionPerformed(evt);
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

        helpLabel.setFont(helpLabel.getFont().deriveFont((helpLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        helpLabel.setText("Insert the required information (*) and click Convert to export the project to PRIDE XML.");

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Output Folder"));
        jPanel1.setOpaque(false);

        outpitFolderLabel.setForeground(new java.awt.Color(255, 0, 0));
        outpitFolderLabel.setText("Folder*");
        outpitFolderLabel.setToolTipText("The folder where the PRIDE XML file will be saved");

        outputFolderJTextField.setEditable(false);
        outputFolderJTextField.setToolTipText("The folder where the PRIDE XML file will be saved");
        outputFolderJTextField.setMargin(new java.awt.Insets(2, 4, 2, 2));

        browseOutputFolderJButton.setText("Browse");
        browseOutputFolderJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseOutputFolderJButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(outpitFolderLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(outputFolderJTextField)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(browseOutputFolderJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(outpitFolderLabel)
                    .addComponent(outputFolderJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(browseOutputFolderJButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout backgroundJPanelLayout = new javax.swing.GroupLayout(backgroundJPanel);
        backgroundJPanel.setLayout(backgroundJPanelLayout);
        backgroundJPanelLayout.setHorizontalGroup(
            backgroundJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(backgroundJPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(openDialogHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(helpLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 236, Short.MAX_VALUE)
                        .addComponent(convertJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(20, 20, 20))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(experimentPropertiesPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        backgroundJPanelLayout.setVerticalGroup(
            backgroundJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(experimentPropertiesPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(openDialogHelpJButton)
                    .addComponent(helpLabel)
                    .addComponent(convertJButton))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void openDialogHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_openDialogHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_openDialogHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
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
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PrideExportDialog.html"));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_openDialogHelpJButtonActionPerformed

    /**
     * Opens a file chooser where the user can select the output folder.
     *
     * @param evt
     */
    private void browseOutputFolderJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseOutputFolderJButtonActionPerformed
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        JFileChooser chooser = new JFileChooser(peptideShakerGUI.getLastSelectedFolder());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select The Output Folder");

        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String path = (chooser.getSelectedFile().getAbsoluteFile().getPath());
            peptideShakerGUI.setLastSelectedFolder(path);
            outputFolderJTextField.setText(path);
        }

        validateInput();

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_browseOutputFolderJButtonActionPerformed

    /**
     * Enable/disable the edit contacts button and open the new contact dialog
     * if the user selected to add a new contact.
     *
     * @param evt
     */
    private void contactsJComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contactsJComboBoxActionPerformed
        if (contactsJComboBox.getSelectedIndex() == 0) {
            editContactJButton.setEnabled(false);
        } else if (contactsJComboBox.getSelectedIndex() == contactsJComboBox.getItemCount() - 1) {
            editContactJButton.setEnabled(false);
            contactsJComboBox.setSelectedIndex(0);
            new NewContactDialog(this, true);
        } else {
            editContactJButton.setEnabled(true);
        }

        validateInput();
    }//GEN-LAST:event_contactsJComboBoxActionPerformed

    /**
     * Enable/disable the edit sample button and open the new sample dialog if
     * the user selected to add a new sample.
     *
     * @param evt
     */
    private void sampleJComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sampleJComboBoxActionPerformed
        if (sampleJComboBox.getSelectedIndex() == 0) {
            editSampleJButton.setEnabled(false);
        } else if (sampleJComboBox.getSelectedIndex() == sampleJComboBox.getItemCount() - 1) {
            editSampleJButton.setEnabled(false);
            sampleJComboBox.setSelectedIndex(0);
            new NewSampleDialog(this, true);
        } else {
            editSampleJButton.setEnabled(true);
        }

        validateInput();
    }//GEN-LAST:event_sampleJComboBoxActionPerformed

    /**
     * Enable/disable the edit protcol button and open the new protcol dialog if
     * the user selected to add a new protcol.
     *
     * @param evt
     */
    private void protocolJComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolJComboBoxActionPerformed
        if (protocolJComboBox.getSelectedIndex() == 0) {
            editProtocolJButton.setEnabled(false);
        } else if (protocolJComboBox.getSelectedIndex() == protocolJComboBox.getItemCount() - 1) {
            editProtocolJButton.setEnabled(false);
            protocolJComboBox.setSelectedIndex(0);
            new NewProtocolDialog(this, true);
        } else {
            editProtocolJButton.setEnabled(true);
        }

        validateInput();
    }//GEN-LAST:event_protocolJComboBoxActionPerformed

    /**
     * Enable/disable the edit instrument button and open the new instrument
     * dialog if the user selected to add a new instrument.
     *
     * @param evt
     */
    private void instrumentJComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_instrumentJComboBoxActionPerformed
        if (instrumentJComboBox.getSelectedIndex() == 0) {
            editInstrumentJButton.setEnabled(false);
        } else if (instrumentJComboBox.getSelectedIndex() == instrumentJComboBox.getItemCount() - 1) {
            editInstrumentJButton.setEnabled(false);
            instrumentJComboBox.setSelectedIndex(0);
            new NewInstrumentDialog(this, true);
        } else {
            editInstrumentJButton.setEnabled(true);
        }

        validateInput();
    }//GEN-LAST:event_instrumentJComboBoxActionPerformed

    /**
     * @see #validateInput()
     */
    private void titleJTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_titleJTextFieldKeyReleased
        validateInput();
    }//GEN-LAST:event_titleJTextFieldKeyReleased

    /**
     * @see #validateInput()
     */
    private void labelJTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_labelJTextFieldKeyReleased
        validateInput();
    }//GEN-LAST:event_labelJTextFieldKeyReleased

    /**
     * @see #validateInput()
     */
    private void projectJTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_projectJTextFieldKeyReleased
        validateInput();
    }//GEN-LAST:event_projectJTextFieldKeyReleased

    /**
     * @see #validateInput()
     */
    private void descriptionJTextAreaKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_descriptionJTextAreaKeyReleased
        validateInput();
    }//GEN-LAST:event_descriptionJTextAreaKeyReleased

    /**
     * Open a new NewReferenceDialog.
     *
     * @param evt
     */
    private void addReferencesJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addReferencesJButtonActionPerformed
        new NewReferenceDialog(this, true);
    }//GEN-LAST:event_addReferencesJButtonActionPerformed

    /**
     * Opens a NewReferenceDialog or a popup menu when right clicking.
     *
     * @param evt
     */
    private void referencesJTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_referencesJTableMouseClicked

        if (evt.getButton() == 1 && evt.getClickCount() == 2) {

            new NewReferenceDialog(this, true, new Reference(
                    (String) referencesJTable.getValueAt(referencesJTable.getSelectedRow(), 1),
                    (String) referencesJTable.getValueAt(referencesJTable.getSelectedRow(), 2),
                    (String) referencesJTable.getValueAt(referencesJTable.getSelectedRow(), 3)),
                    referencesJTable.getSelectedRow());

        } else if (evt.getButton() == 3) {

            int row = referencesJTable.rowAtPoint(evt.getPoint());
            int column = referencesJTable.columnAtPoint(evt.getPoint());
            int[] selectedRows = referencesJTable.getSelectedRows();

            boolean changeRow = true;

            for (int i = 0; i < selectedRows.length && changeRow; i++) {
                if (row == selectedRows[i]) {
                    changeRow = false;
                }
            }

            if (changeRow) {
                referencesJTable.changeSelection(row, column, false, false);
            }

            // makes sure that only valid "moving options" are enabled
            this.refMoveUpJMenuItem.setEnabled(true);
            this.refMoveDownJMenuItem.setEnabled(true);

            if (row == referencesJTable.getRowCount() - 1) {
                this.refMoveDownJMenuItem.setEnabled(false);
            }

            if (row == 0) {
                this.refMoveUpJMenuItem.setEnabled(false);
            }

            referencesPopupJMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_referencesJTableMouseClicked

    /**
     * Deletes the selected reference.
     *
     * @param evt
     */
    private void referencesJTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_referencesJTableKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_DELETE) {
            refDeleteSelectedRowJMenuItemActionPerformed(null);
        }
    }//GEN-LAST:event_referencesJTableKeyReleased

    /**
     * Opens a NewReferenceDialog.
     *
     * @param evt
     */
    private void refEditJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refEditJMenuItemActionPerformed
        new NewReferenceDialog(this, true, new Reference(
                (String) referencesJTable.getValueAt(referencesJTable.getSelectedRow(), 1),
                (String) referencesJTable.getValueAt(referencesJTable.getSelectedRow(), 2),
                (String) referencesJTable.getValueAt(referencesJTable.getSelectedRow(), 3)),
                referencesJTable.getSelectedRow());
    }//GEN-LAST:event_refEditJMenuItemActionPerformed

    /**
     * Moves the selected reference up in the list.
     *
     * @param evt
     */
    private void refMoveUpJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refMoveUpJMenuItemActionPerformed
        int selectedRow = referencesJTable.getSelectedRow();
        int selectedColumn = referencesJTable.getSelectedColumn();

        Object[] tempRow = new Object[]{
            referencesJTable.getValueAt(selectedRow - 1, 0),
            referencesJTable.getValueAt(selectedRow - 1, 1),
            referencesJTable.getValueAt(selectedRow - 1, 2),
            referencesJTable.getValueAt(selectedRow - 1, 3)
        };

        ((DefaultTableModel) referencesJTable.getModel()).removeRow(selectedRow - 1);
        ((DefaultTableModel) referencesJTable.getModel()).insertRow(selectedRow, tempRow);

        referencesJTable.changeSelection(selectedRow - 1, selectedColumn, false, false);

        fixReferenceIndices();
    }//GEN-LAST:event_refMoveUpJMenuItemActionPerformed

    /**
     * Moves the selected reference down in the list.
     *
     * @param evt
     */
    private void refMoveDownJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refMoveDownJMenuItemActionPerformed
        int selectedRow = referencesJTable.getSelectedRow();
        int selectedColumn = referencesJTable.getSelectedColumn();

        Object[] tempRow = new Object[]{
            referencesJTable.getValueAt(selectedRow + 1, 0),
            referencesJTable.getValueAt(selectedRow + 1, 1),
            referencesJTable.getValueAt(selectedRow + 1, 2),
            referencesJTable.getValueAt(selectedRow + 1, 3)
        };

        ((DefaultTableModel) referencesJTable.getModel()).removeRow(selectedRow + 1);
        ((DefaultTableModel) referencesJTable.getModel()).insertRow(selectedRow, tempRow);

        referencesJTable.changeSelection(selectedRow + 1, selectedColumn, false, false);

        fixReferenceIndices();
    }//GEN-LAST:event_refMoveDownJMenuItemActionPerformed

    /**
     * Deletes the selected reference.
     *
     * @param evt
     */
    private void refDeleteSelectedRowJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refDeleteSelectedRowJMenuItemActionPerformed
        if (referencesJTable.getSelectedRow() != -1) {

            int selectedRow = referencesJTable.getSelectedRow();
            int selectedColumn = referencesJTable.getSelectedColumn();

            int[] selectedRows = referencesJTable.getSelectedRows();

            for (int i = selectedRows.length - 1; i >= 0; i--) {
                ((DefaultTableModel) referencesJTable.getModel()).removeRow(selectedRows[i]);
            }

            referencesJTable.changeSelection(selectedRow, selectedColumn, false, false);
            referencesJTable.editingCanceled(null);

            fixReferenceIndices();
        }
    }//GEN-LAST:event_refDeleteSelectedRowJMenuItemActionPerformed

    /**
     * Opens a NewContactDialog where the selected contact can be edited.
     *
     * @param evt
     */
    private void editContactJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editContactJButtonActionPerformed

        // get the selected contact details
        String selectedContact = (String) contactsJComboBox.getSelectedItem();
        Contact tempContact = prideObjectsFactory.getContacts().get(selectedContact);
        if (tempContact == null) {
            tempContact = new Contact("New contact", "", "");
        }

        new NewContactDialog(this, true, tempContact);
    }//GEN-LAST:event_editContactJButtonActionPerformed

    /**
     * Edit the selected sample.
     *
     * @param evt
     */
    private void editSampleJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editSampleJButtonActionPerformed

        // get the selected sample details
        String selectedSample = (String) sampleJComboBox.getSelectedItem();
        Sample tempSample = prideObjectsFactory.getSamples().get(selectedSample);
        if (tempSample == null) {
            tempSample = new Sample("New Sample", null);
        }

        new NewSampleDialog(this, true, tempSample);
    }//GEN-LAST:event_editSampleJButtonActionPerformed

    /**
     * Edit the selected protocol.
     *
     * @param evt
     */
    private void editProtocolJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editProtocolJButtonActionPerformed
        // get the selected protcol details
        String selectedProtocol = (String) protocolJComboBox.getSelectedItem();
        Protocol tempProtocol = prideObjectsFactory.getProtocols().get(selectedProtocol);
        if (tempProtocol == null) {
            tempProtocol = new Protocol("New protocol", null);
        }

        new NewProtocolDialog(this, true, tempProtocol);
    }//GEN-LAST:event_editProtocolJButtonActionPerformed

    /**
     * Edit the selected instrument.
     *
     * @param evt
     */
    private void editInstrumentJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editInstrumentJButtonActionPerformed
        // get the selected instrument details
        String selectedInstrument = (String) instrumentJComboBox.getSelectedItem();
        Instrument tempInstrument = prideObjectsFactory.getInstruments().get(selectedInstrument);
        if (tempInstrument == null) {
            tempInstrument = new Instrument("New instrument", null, null, null);
        }

        new NewInstrumentDialog(this, true, tempInstrument);
    }//GEN-LAST:event_editInstrumentJButtonActionPerformed

    /**
     * Convert the project to a PRIDE XML file.
     *
     * @param evt
     */
    private void convertJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_convertJButtonActionPerformed

        // check if the xml file already exists
        String[] splittedName = titleJTextField.getText().split(" ");
        String fileName = "";
        for (String part : splittedName) {
            fileName += part;
        }
        File prideFile = new File(outputFolderJTextField.getText(), fileName + ".xml");
        if (prideFile.exists()) {
            int selection = JOptionPane.showConfirmDialog(this, "The file \'"
                    + prideFile.getAbsolutePath() + "\' already exists."
                    + "\nOverwrite file?",
                    "Overwrite?", JOptionPane.YES_NO_CANCEL_OPTION);

            if (selection != JOptionPane.YES_OPTION) {
                return;
            }
        }

        final String prideFileName = fileName;
        final PrideExportDialog prideExportDialog = this; // needed due to threading issues
        progressDialog = new ProgressDialogX(this, true);
        progressDialog.setIndeterminate(true);

        new Thread(new Runnable() {

            public void run() {
                progressDialog.setIndeterminate(true);
                progressDialog.setTitle("Exporting PRIDE XML. Please Wait...");
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        new Thread("ConvertThread") {

            @Override
            public void run() {

                // change the peptide shaker icon to a "waiting version"
                peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

                // get the references, if any
                ArrayList<Reference> references = new ArrayList<Reference>();

                for (int row = 0; row < ((DefaultTableModel) referencesJTable.getModel()).getRowCount(); row++) {
                    references.add(new Reference(
                            (String) referencesJTable.getValueAt(row, 1),
                            (String) referencesJTable.getValueAt(row, 2),
                            (String) referencesJTable.getValueAt(row, 3)));
                }

                // get the selected contact details
                String selectedContact = (String) contactsJComboBox.getSelectedItem();
                Contact contact = prideObjectsFactory.getContacts().get(selectedContact);

                // get the selected sample details
                String selectedSample = (String) sampleJComboBox.getSelectedItem();
                Sample sample = prideObjectsFactory.getSamples().get(selectedSample);

                // get the selected protcol details
                String selectedProtocol = (String) protocolJComboBox.getSelectedItem();
                Protocol protocol = prideObjectsFactory.getProtocols().get(selectedProtocol);

                // get the selected instrument details
                String selectedInstrument = (String) instrumentJComboBox.getSelectedItem();
                Instrument instrument = prideObjectsFactory.getInstruments().get(selectedInstrument);

                boolean conversionCompleted = false;

                try {
                    PRIDEExport prideExport = new PRIDEExport(peptideShakerGUI, prideExportDialog, titleJTextField.getText(),
                            labelJTextField.getText(), descriptionJTextArea.getText(), projectJTextField.getText(),
                            references, contact, sample, protocol, instrument, new File(outputFolderJTextField.getText()), prideFileName);
                    prideExport.createPrideXmlFile(progressDialog);
                    conversionCompleted = true;
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                }

                // close the progress dialog
                progressDialog.dispose();

                // return the peptide shaker icon to the standard version
                peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

                // display a conversion complete message to the user
                if (conversionCompleted && !progressDialog.isRunCanceled()) {

                    // create an empty label to put the message in
                    JLabel label = new JLabel();

                    // html content 
                    JEditorPane ep = new JEditorPane("text/html", "<html><body bgcolor=\"#" + Util.color2Hex(label.getBackground()) + "\">"
                            + "PRIDE XML file \'"
                            + new File(outputFolderJTextField.getText(), titleJTextField.getText() + ".xml").getAbsolutePath() + "\' created.<br><br>"
                            + "Please see <a href=\"http://www.ebi.ac.uk/pride\">www.ebi.ac.uk/pride</a> for how to submit data to PRIDE."
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

                    JOptionPane.showMessageDialog(prideExportDialog, ep, "PRIDE XML File Created", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                }
                
                if (progressDialog.isRunCanceled()) {
                    JOptionPane.showMessageDialog(peptideShakerGUI, "PRIDE XML conversion cancelled by the user.", "PRIDE XML Conversion Cancelled", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.start();
    }//GEN-LAST:event_convertJButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addReferencesJButton;
    private javax.swing.JPanel backgroundJPanel;
    private javax.swing.JButton browseOutputFolderJButton;
    private javax.swing.JLabel contactLabel;
    private javax.swing.JComboBox contactsJComboBox;
    private javax.swing.JButton convertJButton;
    private javax.swing.JScrollPane descriptionJScrollPane;
    private javax.swing.JTextArea descriptionJTextArea;
    private javax.swing.JLabel descriptionLabel;
    private javax.swing.JButton editContactJButton;
    private javax.swing.JButton editInstrumentJButton;
    private javax.swing.JButton editProtocolJButton;
    private javax.swing.JButton editSampleJButton;
    private javax.swing.JLabel experimentLabel;
    private javax.swing.JLabel experimentPropertiesLabel;
    private javax.swing.JPanel experimentPropertiesPanel;
    private javax.swing.JLabel helpLabel;
    private javax.swing.JComboBox instrumentJComboBox;
    private javax.swing.JLabel instrumentLabel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JTextField labelJTextField;
    private javax.swing.JButton openDialogHelpJButton;
    private javax.swing.JLabel outpitFolderLabel;
    private javax.swing.JTextField outputFolderJTextField;
    private javax.swing.JTextField projectJTextField;
    private javax.swing.JLabel projectLabel;
    private javax.swing.JComboBox protocolJComboBox;
    private javax.swing.JLabel protocolLabel;
    private javax.swing.JMenuItem refDeleteSelectedRowJMenuItem;
    private javax.swing.JMenuItem refEditJMenuItem;
    private javax.swing.JMenuItem refMoveDownJMenuItem;
    private javax.swing.JMenuItem refMoveUpJMenuItem;
    private javax.swing.JScrollPane referencesJScrollPane;
    private javax.swing.JTable referencesJTable;
    private javax.swing.JLabel referencesLabel;
    private javax.swing.JPopupMenu referencesPopupJMenu;
    private javax.swing.JComboBox sampleJComboBox;
    private javax.swing.JLabel sampleLabel;
    private javax.swing.JTextField titleJTextField;
    // End of variables declaration//GEN-END:variables

    /**
     * Validates that the required input has been inserted and enables or
     * disables the Convert button.
     */
    private void validateInput() {

        boolean inputValid = true;

        if (titleJTextField.getText().length() == 0
                || labelJTextField.getText().length() == 0
                || projectJTextField.getText().length() == 0
                || descriptionJTextArea.getText().length() == 0) {
            inputValid = false;
        }

        if ((contactsJComboBox.getSelectedIndex() == 0 || contactsJComboBox.getSelectedIndex() == contactsJComboBox.getItemCount() - 1)
                || (sampleJComboBox.getSelectedIndex() == 0 || sampleJComboBox.getSelectedIndex() == sampleJComboBox.getItemCount() - 1)
                || (protocolJComboBox.getSelectedIndex() == 0 || protocolJComboBox.getSelectedIndex() == protocolJComboBox.getItemCount() - 1)
                || (instrumentJComboBox.getSelectedIndex() == 0 || instrumentJComboBox.getSelectedIndex() == instrumentJComboBox.getItemCount() - 1)) {
            inputValid = false;
        }

        if (!new File(outputFolderJTextField.getText()).exists()) {
            inputValid = false;
        }

        convertJButton.setEnabled(inputValid);

        // highlight the fields that have not been filled
        if (titleJTextField.getText().length() > 0) {
            experimentPropertiesLabel.setForeground(Color.BLACK);
        } else {
            experimentPropertiesLabel.setForeground(Color.RED);
        }

        if (labelJTextField.getText().length() > 0) {
            experimentLabel.setForeground(Color.BLACK);
        } else {
            experimentLabel.setForeground(Color.RED);
        }

        if (projectJTextField.getText().length() > 0) {
            projectLabel.setForeground(Color.BLACK);
        } else {
            projectLabel.setForeground(Color.RED);
        }

        if (descriptionJTextArea.getText().length() > 0) {
            descriptionLabel.setForeground(Color.BLACK);
        } else {
            descriptionLabel.setForeground(Color.RED);
        }

        if (outputFolderJTextField.getText().length() > 0) {
            outpitFolderLabel.setForeground(Color.BLACK);
        } else {
            outpitFolderLabel.setForeground(Color.RED);
        }

        if (contactsJComboBox.getSelectedIndex() != 0) {
            contactLabel.setForeground(Color.BLACK);
        } else {
            contactLabel.setForeground(Color.RED);
        }

        if (sampleJComboBox.getSelectedIndex() != 0) {
            sampleLabel.setForeground(Color.BLACK);
        } else {
            sampleLabel.setForeground(Color.RED);
        }

        if (protocolJComboBox.getSelectedIndex() != 0) {
            protocolLabel.setForeground(Color.BLACK);
        } else {
            protocolLabel.setForeground(Color.RED);
        }

        if (instrumentJComboBox.getSelectedIndex() != 0) {
            instrumentLabel.setForeground(Color.BLACK);
        } else {
            instrumentLabel.setForeground(Color.RED);
        }
    }

    /**
     * Fixes the indices in the reference table so that they are in accending
     * order starting from one.
     */
    private void fixReferenceIndices() {
        for (int row = 0; row < ((DefaultTableModel) referencesJTable.getModel()).getRowCount(); row++) {
            ((DefaultTableModel) referencesJTable.getModel()).setValueAt(new Integer(row + 1), row, 0);
        }
    }

    /**
     * Updates the reference at the given row.
     *
     * @param reference
     * @param rowIndex
     */
    public void updateReference(Reference reference, int rowIndex) {
        referencesJTable.setValueAt(reference.getReference(), rowIndex, 1);
        referencesJTable.setValueAt(reference.getPmid(), rowIndex, 1);
        referencesJTable.setValueAt(reference.getDoi(), rowIndex, 1);
    }

    /**
     * Adds a new reference.
     *
     * @param reference
     */
    public void addReference(Reference reference) {

        try {
            prideObjectsFactory.addReference(reference);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
        ((DefaultTableModel) referencesJTable.getModel()).addRow(new Object[]{
                    referencesJTable.getRowCount() + 1,
                    reference.getReference(),
                    reference.getPmid(),
                    reference.getDoi()
                });
    }

    /**
     * Save the provided protocol to file and then select it in the list.
     *
     * @param protocol
     */
    public void setProtocol(Protocol protocol) {
        try {
            prideObjectsFactory.addProtocol(protocol);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }

        insertOptions(new ArrayList<String>(prideObjectsFactory.getProtocols().keySet()), "--- Select a Protocol ---", "   Create a New Protocol...", protocolJComboBox);

        int selectedProtocolIndex = 0;

        for (int i = 0; i < protocolJComboBox.getItemCount(); i++) {
            if (((String) protocolJComboBox.getItemAt(i)).equalsIgnoreCase(protocol.getName())) {
                selectedProtocolIndex = i;
            }
        }

        protocolJComboBox.setSelectedIndex(selectedProtocolIndex);

        protocolJComboBoxActionPerformed(null);
    }

    /**
     * Save the provided Instrument to file and then select it in the list.
     *
     * @param instrument
     */
    public void setInstrument(Instrument instrument) {
        try {
            prideObjectsFactory.addInstrument(instrument);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }

        insertOptions(new ArrayList<String>(prideObjectsFactory.getInstruments().keySet()), "--- Select an Instrument ---", "   Create a New Instrument...", instrumentJComboBox);

        int selectedInstrumentIndex = 0;

        for (int i = 0; i < instrumentJComboBox.getItemCount(); i++) {
            if (((String) instrumentJComboBox.getItemAt(i)).equalsIgnoreCase(instrument.getName())) {
                selectedInstrumentIndex = i;
            }
        }

        instrumentJComboBox.setSelectedIndex(selectedInstrumentIndex);


        instrumentJComboBoxActionPerformed(null);
    }

    /**
     * Save the provided sample to file and then select it in the list.
     *
     * @param sample
     */
    public void setSample(Sample sample) {
        try {
            prideObjectsFactory.addSample(sample);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }

        insertOptions(new ArrayList<String>(prideObjectsFactory.getSamples().keySet()), "--- Select a Sample ---", "   Create a New Sample...", sampleJComboBox);

        int selectedSampleIndex = 0;

        for (int i = 0; i < sampleJComboBox.getItemCount(); i++) {
            if (((String) sampleJComboBox.getItemAt(i)).equalsIgnoreCase(sample.getName())) {
                selectedSampleIndex = i;
            }
        }

        sampleJComboBox.setSelectedIndex(selectedSampleIndex);

        sampleJComboBoxActionPerformed(null);
    }

    /**
     * Sets the selected contact.
     *
     * @param contact
     */
    public void setContact(Contact contact) {
        try {
            prideObjectsFactory.addContact(contact);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
        insertOptions(new ArrayList<String>(prideObjectsFactory.getContacts().keySet()), "--- Select a Contact ---", "   Create a New Contact...", contactsJComboBox);

        int selectedContactIndex = 0;

        for (int i = 0; i < contactsJComboBox.getItemCount(); i++) {
            if (((String) contactsJComboBox.getItemAt(i)).equalsIgnoreCase(contact.getName())) {
                selectedContactIndex = i;
            }
        }

        contactsJComboBox.setSelectedIndex(selectedContactIndex);


        contactsJComboBoxActionPerformed(null);
    }

    /**
     * Tries to extract the ontology from the given cv term. For example
     * BTO:0000763 returns BTO.
     *
     * @param cvTerm the cv term to extract the ontology from, e.g., BTO:0000763
     * @return the extracted ontology
     */
    public static String getOntologyFromCvTerm(String cvTerm) {

        String ontology;

        if (cvTerm.lastIndexOf(":") != -1) {
            ontology = cvTerm.substring(0, cvTerm.lastIndexOf(":"));
        } else if (cvTerm.lastIndexOf("_") != -1) {
            ontology = cvTerm.substring(0, cvTerm.lastIndexOf("_"));
        } else {
            ontology = "NEWT";
        }

        return ontology;
    }
    
    /**
     * Returns true if the user has canceled the progress.
     * 
     * 
     * @return true if the user has canceled the progress
     */
    public boolean progressCancelled() {
        return progressDialog.isRunCanceled();
    }
}
