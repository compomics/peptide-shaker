package eu.isas.peptideshaker.pride.gui;

import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import eu.isas.peptideshaker.gui.HelpDialog;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.pride.Contact;
import eu.isas.peptideshaker.pride.Instrument;
import eu.isas.peptideshaker.pride.Protocol;
import eu.isas.peptideshaker.pride.Reference;
import eu.isas.peptideshaker.pride.Sample;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 * A simple dialog where the user can select
 *
 * @author Harald Barsnes
 */
public class PrideExportDialog extends javax.swing.JDialog {

    /**
     * The PeptideShakerGUI main class.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The references table column tool tips.
     */
    private Vector referenceTableColumnToolTips;

    /**
     * Create a new PrideExportDialog.
     *
     * @param peptideShakerGUI a refereence to the main GUI frame
     * @param modal
     */
    public PrideExportDialog(PeptideShakerGUI peptideShakerGUI, boolean modal) {
        super(peptideShakerGUI, modal);
        this.peptideShakerGUI = peptideShakerGUI;
        initComponents();

        referenceTableColumnToolTips = new Vector();
        referenceTableColumnToolTips.add(null);
        referenceTableColumnToolTips.add("The reference tag");
        referenceTableColumnToolTips.add("PubMed ID");
        referenceTableColumnToolTips.add("Digital Object Identifier");

        referencesJScrollPane.getViewport().setOpaque(false);
        referencesJTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        referencesJTable.getTableHeader().setReorderingAllowed(false);
        referencesJTable.getColumn(" ").setMaxWidth(40);
        referencesJTable.getColumn(" ").setMinWidth(40);

        contactsJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        sampleJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        protocolJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        instrumentJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        // insert project data
        insertProjectData();

        // insert available contacts, instruments, protocols and samples
        insertOptions("conf/pride/contacts", ".con", "--- Select a Contact ---", "   Create a New Contact...", contactsJComboBox);
        insertOptions("conf/pride/samples", ".sam", "--- Select a Sample Set ---", "   Create a New Sample Set...", sampleJComboBox);
        insertOptions("conf/pride/protocols", ".pro", "--- Select a Protocol ---", "   Create a New Protocol...", protocolJComboBox);
        insertOptions("conf/pride/instruments", ".int", "--- Select an Instrument ---", "   Create a New Instrument...", instrumentJComboBox);

        setLocationRelativeTo(peptideShakerGUI);
        setVisible(true);
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
     * @param optionsPath the path to the option files, e.g.,
     * conf/pride/contacts
     * @param fileEnding the file ending, e.g., .con
     * @param selectText the text to use for the select item, e.g., --- Select a
     * Contact ---
     * @param insertNewText the text to use for the new item, e.g., Create a New
     * Contact...
     * @param optionComboBox the combo box to add the options to
     */
    private void insertOptions(String optionsPath, String fileEnding, String selectText, String insertNewText, JComboBox optionComboBox) {

        File optionFolder = new File(peptideShakerGUI.getJarFilePath(), optionsPath);

        if (!optionFolder.exists()) {
            optionFolder.mkdir();
        }

        File[] optionFiles = optionFolder.listFiles();
        Vector optionNames = new Vector();

        for (int i = 0; i < optionFiles.length; i++) {

            if (optionFiles[i].getAbsolutePath().endsWith(fileEnding)) {
                optionNames.add(optionFiles[i].getName().subSequence(0, optionFiles[i].getName().lastIndexOf(".")));
            }
        }

        java.util.Collections.sort(optionNames);

        Vector comboboxTooltips = new Vector();
        comboboxTooltips.add(null);

        for (int i = 0; i < optionNames.size(); i++) {
            comboboxTooltips.add(optionNames.get(i));
        }

        comboboxTooltips.add(null);
        optionComboBox.setRenderer(new MyComboBoxRenderer(comboboxTooltips, SwingConstants.CENTER));
        optionNames.insertElementAt(selectText, 0);
        optionNames.add(insertNewText);
        optionComboBox.setModel(new DefaultComboBoxModel(optionNames));
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
        jLabel1 = new javax.swing.JLabel();
        titleJTextField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        labelJTextField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        projectJTextField = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        descriptionJScrollPane = new javax.swing.JScrollPane();
        descriptionJTextArea = new javax.swing.JTextArea();
        jLabel10 = new javax.swing.JLabel();
        addReferencesJButton = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        contactsJComboBox = new javax.swing.JComboBox();
        editContactJButton = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        sampleJComboBox = new javax.swing.JComboBox();
        editSampleJButton = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        protocolJComboBox = new javax.swing.JComboBox();
        editProtocolJButton = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
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
        jLabel9 = new javax.swing.JLabel();
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

        jLabel1.setText("Title*");

        titleJTextField.setMargin(new java.awt.Insets(2, 4, 2, 2));
        titleJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                titleJTextFieldKeyReleased(evt);
            }
        });

        jLabel2.setText("Label*");

        labelJTextField.setToolTipText("Allows experiments to be grouped or organized under projects");
        labelJTextField.setMargin(new java.awt.Insets(2, 4, 2, 2));
        labelJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                labelJTextFieldKeyReleased(evt);
            }
        });

        jLabel3.setText("Project*");

        projectJTextField.setMargin(new java.awt.Insets(2, 4, 2, 2));
        projectJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                projectJTextFieldKeyReleased(evt);
            }
        });

        jLabel4.setText("Description*");

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

        jLabel10.setText("References");

        addReferencesJButton.setText("Add");
        addReferencesJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addReferencesJButtonActionPerformed(evt);
            }
        });

        jLabel5.setText("Contact*");

        contactsJComboBox.setMaximumRowCount(20);
        contactsJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "--- Select ---", "Item 2", "Item 3", "Item 4" }));
        contactsJComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contactsJComboBoxActionPerformed(evt);
            }
        });

        editContactJButton.setText("Edit");
        editContactJButton.setEnabled(false);
        editContactJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editContactJButtonActionPerformed(evt);
            }
        });

        jLabel6.setText("Sample*");

        sampleJComboBox.setMaximumRowCount(20);
        sampleJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "--- Select ---", "Item 2", "Item 3", "Item 4" }));
        sampleJComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sampleJComboBoxActionPerformed(evt);
            }
        });

        editSampleJButton.setText("Edit");
        editSampleJButton.setEnabled(false);
        editSampleJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editSampleJButtonActionPerformed(evt);
            }
        });

        jLabel7.setText("Protocol*");

        protocolJComboBox.setMaximumRowCount(20);
        protocolJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "--- Select ---", "Item 2", "Item 3", "Item 4" }));
        protocolJComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolJComboBoxActionPerformed(evt);
            }
        });

        editProtocolJButton.setText("Edit");
        editProtocolJButton.setEnabled(false);
        editProtocolJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editProtocolJButtonActionPerformed(evt);
            }
        });

        jLabel8.setText("Instrument*");

        instrumentJComboBox.setMaximumRowCount(20);
        instrumentJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "--- Select ---", "Item 2", "Item 3", "Item 4" }));
        instrumentJComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                instrumentJComboBoxActionPerformed(evt);
            }
        });

        editInstrumentJButton.setText("Edit");
        editInstrumentJButton.setEnabled(false);
        editInstrumentJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editInstrumentJButtonActionPerformed(evt);
            }
        });

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
                            .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(descriptionJScrollPane)
                            .addComponent(projectJTextField)
                            .addGroup(experimentPropertiesPanelLayout.createSequentialGroup()
                                .addComponent(titleJTextField)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(labelJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, experimentPropertiesPanelLayout.createSequentialGroup()
                                .addComponent(referencesJScrollPane)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(addReferencesJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, experimentPropertiesPanelLayout.createSequentialGroup()
                        .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE))
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
                    .addComponent(jLabel1)
                    .addComponent(titleJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(labelJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(projectJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel4)
                    .addComponent(descriptionJScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel10)
                    .addComponent(referencesJScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(addReferencesJButton))
                .addGap(18, 18, Short.MAX_VALUE)
                .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(contactsJComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(editContactJButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(sampleJComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(editSampleJButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(protocolJComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(editProtocolJButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
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
        convertJButton.setEnabled(false);

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
        helpLabel.setText("Insert the required information (*) and click Convert to convert the project to PRIDE XML.");

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Output Folder"));
        jPanel1.setOpaque(false);

        jLabel9.setText("Folder*");

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
                .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                    .addComponent(jLabel9)
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
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 231, Short.MAX_VALUE)
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

            //makes sure that only valid "moving options" are enabled
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

            for (int i = referencesJTable.getSelectedRows().length - 1; i >= 0; i--) {
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
        File contactsFolder = new File(peptideShakerGUI.getJarFilePath(), "conf/pride/contacts");
        Contact tempContact = new Contact(new File(contactsFolder, selectedContact + ".con"));

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
        File samplesFolder = new File(peptideShakerGUI.getJarFilePath(), "conf/pride/samples");
        Sample tempSample = new Sample(new File(samplesFolder, selectedSample + ".sam"));

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
        File protocolsFolder = new File(peptideShakerGUI.getJarFilePath(), "conf/pride/protocols");
        Protocol tempProtocol = new Protocol(new File(protocolsFolder, selectedProtocol + ".pro"));

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
        File instrumentFolder = new File(peptideShakerGUI.getJarFilePath(), "conf/pride/instruments");
        Instrument tempInstrument = new Instrument(new File(instrumentFolder, selectedInstrument + ".int"));

        new NewInstrumentDialog(this, true, tempInstrument);
    }//GEN-LAST:event_editInstrumentJButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addReferencesJButton;
    private javax.swing.JPanel backgroundJPanel;
    private javax.swing.JButton browseOutputFolderJButton;
    private javax.swing.JComboBox contactsJComboBox;
    private javax.swing.JButton convertJButton;
    private javax.swing.JScrollPane descriptionJScrollPane;
    private javax.swing.JTextArea descriptionJTextArea;
    private javax.swing.JButton editContactJButton;
    private javax.swing.JButton editInstrumentJButton;
    private javax.swing.JButton editProtocolJButton;
    private javax.swing.JButton editSampleJButton;
    private javax.swing.JPanel experimentPropertiesPanel;
    private javax.swing.JLabel helpLabel;
    private javax.swing.JComboBox instrumentJComboBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JTextField labelJTextField;
    private javax.swing.JButton openDialogHelpJButton;
    private javax.swing.JTextField outputFolderJTextField;
    private javax.swing.JTextField projectJTextField;
    private javax.swing.JComboBox protocolJComboBox;
    private javax.swing.JMenuItem refDeleteSelectedRowJMenuItem;
    private javax.swing.JMenuItem refEditJMenuItem;
    private javax.swing.JMenuItem refMoveDownJMenuItem;
    private javax.swing.JMenuItem refMoveUpJMenuItem;
    private javax.swing.JScrollPane referencesJScrollPane;
    private javax.swing.JTable referencesJTable;
    private javax.swing.JPopupMenu referencesPopupJMenu;
    private javax.swing.JComboBox sampleJComboBox;
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
    public void setProtocol (Protocol protocol) {
        
        File protocolsFolder = new File(peptideShakerGUI.getJarFilePath(), "conf/pride/protocols");
        File protocolsFile = new File(protocolsFolder, protocol.getName() + ".pro");
        
        try {
            protocol.saveAsFile(protocolsFile);

            insertOptions("conf/pride/protocols", ".pro", "--- Select a Protocol ---", "   Create a New Protocol...", protocolJComboBox);

            int selectedProtocolIndex = 0;

            for (int i = 0; i < protocolJComboBox.getItemCount(); i++) {
                if (((String) protocolJComboBox.getItemAt(i)).equalsIgnoreCase(protocol.getName())) {
                    selectedProtocolIndex = i;
                }
            }

            protocolJComboBox.setSelectedIndex(selectedProtocolIndex);

        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(
                    this, "The file " + protocolsFile.getAbsolutePath() + " could not be found.",
                    "File Not Found", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    this, "An error occured when trying to save the file " + protocolsFile.getAbsolutePath() + ".",
                    "File Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    
    /**
     * Save the provided Instrument to file and then select it in the list.
     * 
     * @param instrument 
     */
    public void setInstrument (Instrument instrument) {
        
        File instrumentsFolder = new File(peptideShakerGUI.getJarFilePath(), "conf/pride/instruments");
        File instrumentsFile = new File(instrumentsFolder, instrument.getName() + ".int");
        
        try {
            instrument.saveAsFile(instrumentsFile);

            insertOptions("conf/pride/instruments", ".int", "--- Select an Instrument ---", "   Create a New Instrument...", instrumentJComboBox);

            int selectedInstrumentIndex = 0;

            for (int i = 0; i < instrumentJComboBox.getItemCount(); i++) {
                if (((String) instrumentJComboBox.getItemAt(i)).equalsIgnoreCase(instrument.getName())) {
                    selectedInstrumentIndex = i;
                }
            }

            instrumentJComboBox.setSelectedIndex(selectedInstrumentIndex);

        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(
                    this, "The file " + instrumentsFile.getAbsolutePath() + " could not be found.",
                    "File Not Found", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    this, "An error occured when trying to save the file " + instrumentsFile.getAbsolutePath() + ".",
                    "File Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    
    /**
     * Save the provided sample to file and then select it in the list.
     * 
     * @param sample 
     */
    public void setSample (Sample sample) {
        
        File samplesFolder = new File(peptideShakerGUI.getJarFilePath(), "conf/pride/samples");
        File samplesFile = new File(samplesFolder, sample.getName() + ".sam");
        
        try {
            sample.saveAsFile(samplesFile);

            insertOptions("conf/pride/samples", ".sam", "--- Select a Sample ---", "   Create a New Sample...", sampleJComboBox);

            int selectedSampleIndex = 0;

            for (int i = 0; i < sampleJComboBox.getItemCount(); i++) {
                if (((String) sampleJComboBox.getItemAt(i)).equalsIgnoreCase(sample.getName())) {
                    selectedSampleIndex = i;
                }
            }

            sampleJComboBox.setSelectedIndex(selectedSampleIndex);

        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(
                    this, "The file " + samplesFile.getAbsolutePath() + " could not be found.",
                    "File Not Found", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    this, "An error occured when trying to save the file " + samplesFile.getAbsolutePath() + ".",
                    "File Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    /**
     * Sets the selected contact.
     *
     * @param contact
     */
    public void setContact(Contact contact) {

        File contactsFolder = new File(peptideShakerGUI.getJarFilePath(), "conf/pride/contacts");
        File contactFile = new File(contactsFolder, contact.getName() + ".con");
        
        try {
            contact.saveAsFile(contactFile);

            insertOptions("conf/pride/contacts", ".con", "--- Select a Contact ---", "   Create a New Contact...", contactsJComboBox);

            int selectedContactIndex = 0;

            for (int i = 0; i < contactsJComboBox.getItemCount(); i++) {
                if (((String) contactsJComboBox.getItemAt(i)).equalsIgnoreCase(contact.getName())) {
                    selectedContactIndex = i;
                }
            }

            contactsJComboBox.setSelectedIndex(selectedContactIndex);

        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(
                    this, "The file " + contactFile.getAbsolutePath() + " could not be found.",
                    "File Not Found", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    this, "An error occured when trying to save the file " + contactFile.getAbsolutePath() + ".",
                    "File Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
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
}
