package eu.isas.peptideshaker.gui.pride;

import com.compomics.util.Util;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.pride.CvTerm;
import com.compomics.util.pride.prideobjects.Sample;
import java.awt.Color;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import no.uib.olsdialog.OLSDialog;
import no.uib.olsdialog.OLSInputable;

/**
 * A dialog for annotating samples.
 *
 * @author Harald Barsnes
 */
public class NewSampleDialog extends javax.swing.JDialog implements OLSInputable {

    /**
     * The table column header tooltips.
     */
    private Vector columnToolTips;
    /**
     * The PRIDE Export Dialog.
     */
    private PrideExportDialog prideExportDialog;
    /**
     * The NEWT taxonony root.
     */
    private String newtRoot = "NEWT UniProt Taxonomy Database [NEWT] / Root node of taxonomy";
    /**
     * Mapping of species names to species accession number.
     */
    private HashMap<String, String> speciesMap;
    /**
     * Mapping of tissue names to tissue accession number.
     */
    private HashMap<String, String> tissueMap;
    /**
     * Mapping of cell type names to cell type accession number.
     */
    private HashMap<String, String> cellTypeMap;
    /**
     * The species separator used in the species combobox.
     */
    private String speciesSeparator = "------------";
    /**
     * The last valid input for contact name
     */
    private String lastNameInput = "";

    /**
     * Creates a new NewSampleDialog.
     *
     * @param prideExportDialog
     * @param modal
     */
    public NewSampleDialog(PrideExportDialog prideExportDialog, boolean modal) {
        super(prideExportDialog, modal);
        this.prideExportDialog = prideExportDialog;

        initComponents();
        setTitle("New Sample");

        setUpTableAndComboBoxes();

        setLocationRelativeTo(prideExportDialog);
        setVisible(true);
    }

    /**
     * Creates a new NewSampleDialog.
     *
     * @param prideExportDialog
     * @param modal
     * @param sample
     */
    public NewSampleDialog(PrideExportDialog prideExportDialog, boolean modal, Sample sample) {
        super(prideExportDialog, modal);
        this.prideExportDialog = prideExportDialog;

        initComponents();
        setTitle("Edit Sample");

        sampleNameJTextField.setText(sample.getName());

        setUpTableAndComboBoxes();

        for (int i = 0; i < sample.getCvTerms().size(); i++) {

            if (speciesMap.containsKey(sample.getCvTerms().get(i).getName()) && speciesJComboBox.getSelectedIndex() == 0) {
                speciesJComboBox.setSelectedItem(sample.getCvTerms().get(i).getName());
            } else if (tissueMap.containsKey(sample.getCvTerms().get(i).getName()) && tissueJComboBox.getSelectedIndex() == 0) {
                tissueJComboBox.setSelectedItem(sample.getCvTerms().get(i).getName());
            } else if (cellTypeMap.containsKey(sample.getCvTerms().get(i).getName()) && cellTypeJComboBox.getSelectedIndex() == 0) {
                cellTypeJComboBox.setSelectedItem(sample.getCvTerms().get(i).getName());
            } else {
                ((DefaultTableModel) sampleCvTermsJTable.getModel()).addRow(new Object[]{
                            (i + 1),
                            sample.getCvTerms().get(i).getOntology(),
                            sample.getCvTerms().get(i).getAccession(),
                            sample.getCvTerms().get(i).getName(),
                            sample.getCvTerms().get(i).getValue()
                        });
            }
        }

        validateInput();

        setLocationRelativeTo(prideExportDialog);
        setVisible(true);
    }

    /**
     * Set up the table properties.
     */
    private void setUpTableAndComboBoxes() {
        sampleCvScrollPane.getViewport().setOpaque(false);
        sampleCvTermsJTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        sampleCvTermsJTable.getTableHeader().setReorderingAllowed(false);
        sampleCvTermsJTable.getColumn(" ").setMaxWidth(40);
        sampleCvTermsJTable.getColumn(" ").setMinWidth(40);

        speciesJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        tissueJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        cellTypeJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        columnToolTips = new Vector();
        columnToolTips.add(null);
        columnToolTips.add(null);
        columnToolTips.add(null);
        columnToolTips.add(null);
        columnToolTips.add(null);

        // insert the species from file
        speciesMap = new HashMap<String, String>();

        try {
            InputStream stream = getClass().getResource("/prideDefaults/species.txt").openStream();
            InputStreamReader streamReader = new InputStreamReader(stream);
            BufferedReader b = new BufferedReader(streamReader);
            Vector<String> species = new Vector<String>();
            species.add("--- Select ---");
            String line;

            while ((line = b.readLine()) != null) {
                String[] temp = line.split("\t");

                if (temp[0].equalsIgnoreCase("0000")) {
                    species.add(speciesSeparator);

                } else {
                    species.add(temp[1]);
                    speciesMap.put(temp[1], temp[0]);
                }
            }

            species.add("Other (Please Add Below)");
            speciesJComboBox.setModel(new DefaultComboBoxModel(species));
            speciesJComboBox.setSelectedIndex(0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // insert the tissue types from file
        tissueMap = new HashMap<String, String>();

        try {
            InputStream stream = getClass().getResource("/prideDefaults/tissue.txt").openStream();
            InputStreamReader streamReader = new InputStreamReader(stream);
            BufferedReader b = new BufferedReader(streamReader);
            Vector<String> tissue = new Vector<String>();
            tissue.add("--- Select ---");
            String line;

            while ((line = b.readLine()) != null) {
                String[] temp = line.split("\t");
                tissue.add(temp[1]);
                tissueMap.put(temp[1], temp[0]);
            }

            tissue.add("Other (Please Add Below)");
            tissueJComboBox.setModel(new DefaultComboBoxModel(tissue));
            tissueJComboBox.setSelectedIndex(0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // insert the cell types from file
        cellTypeMap = new HashMap<String, String>();

        try {
            InputStream stream = getClass().getResource("/prideDefaults/cell_type.txt").openStream();
            InputStreamReader streamReader = new InputStreamReader(stream);
            BufferedReader b = new BufferedReader(streamReader);
            Vector<String> cellType = new Vector<String>();
            cellType.add("--- Select ---");
            String line;

            while ((line = b.readLine()) != null) {
                String[] temp = line.split("\t");
                cellType.add(temp[1]);
                cellTypeMap.put(temp[1], temp[0]);
            }

            cellType.add("Other (Please Add Below)");
            cellTypeJComboBox.setModel(new DefaultComboBoxModel(cellType));
            cellTypeJComboBox.setSelectedIndex(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        popupJMenu = new javax.swing.JPopupMenu();
        editJMenuItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        moveUpJMenuItem = new javax.swing.JMenuItem();
        moveDownJMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        deleteSelectedRowJMenuItem = new javax.swing.JMenuItem();
        sampleDetailsPanel = new javax.swing.JPanel();
        preferredOntologiesLabel = new javax.swing.JLabel();
        sampleCvScrollPane = new javax.swing.JScrollPane();
        sampleCvTermsJTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) columnToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        sampleDetailsJButton = new javax.swing.JButton();
        nameLabel = new javax.swing.JLabel();
        sampleNameJTextField = new javax.swing.JTextField();
        speciesLabel = new javax.swing.JLabel();
        speciesJComboBox = new javax.swing.JComboBox();
        tissueLabel = new javax.swing.JLabel();
        tissueJComboBox = new javax.swing.JComboBox();
        cellTypeLabel = new javax.swing.JLabel();
        cellTypeJComboBox = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        okButton = new javax.swing.JButton();

        editJMenuItem.setMnemonic('E');
        editJMenuItem.setText("Edit");
        editJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editJMenuItemActionPerformed(evt);
            }
        });
        popupJMenu.add(editJMenuItem);
        popupJMenu.add(jSeparator3);

        moveUpJMenuItem.setMnemonic('U');
        moveUpJMenuItem.setText("Move Up");
        moveUpJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveUpJMenuItemActionPerformed(evt);
            }
        });
        popupJMenu.add(moveUpJMenuItem);

        moveDownJMenuItem.setMnemonic('D');
        moveDownJMenuItem.setText("Move Down");
        moveDownJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveDownJMenuItemActionPerformed(evt);
            }
        });
        popupJMenu.add(moveDownJMenuItem);
        popupJMenu.add(jSeparator4);

        deleteSelectedRowJMenuItem.setText("Delete");
        deleteSelectedRowJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteSelectedRowJMenuItemActionPerformed(evt);
            }
        });
        popupJMenu.add(deleteSelectedRowJMenuItem);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("New Sample");

        sampleDetailsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Sample Details"));

        preferredOntologiesLabel.setFont(preferredOntologiesLabel.getFont().deriveFont((preferredOntologiesLabel.getFont().getStyle() | java.awt.Font.ITALIC), preferredOntologiesLabel.getFont().getSize()-2));
        preferredOntologiesLabel.setText("NEWT (species), BTO (tissue), CL (cell type), GO (gene ontology) and DOID (disease state)");

        sampleCvTermsJTable.setFont(sampleCvTermsJTable.getFont());
        sampleCvTermsJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Ontology", "Accession", "Name", "Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.Object.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        sampleCvTermsJTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                sampleCvTermsJTableMouseClicked(evt);
            }
        });
        sampleCvTermsJTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                sampleCvTermsJTableKeyReleased(evt);
            }
        });
        sampleCvScrollPane.setViewportView(sampleCvTermsJTable);

        sampleDetailsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/ols_transparent.GIF"))); // NOI18N
        sampleDetailsJButton.setText("Add Sample Term");
        sampleDetailsJButton.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        sampleDetailsJButton.setPreferredSize(new java.awt.Dimension(159, 23));
        sampleDetailsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sampleDetailsJButtonActionPerformed(evt);
            }
        });

        nameLabel.setForeground(new java.awt.Color(255, 0, 0));
        nameLabel.setText("Name*");

        sampleNameJTextField.setMargin(new java.awt.Insets(2, 4, 2, 2));
        sampleNameJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                sampleNameJTextFieldKeyReleased(evt);
            }
        });

        speciesLabel.setForeground(new java.awt.Color(255, 0, 0));
        speciesLabel.setText("Species*");

        speciesJComboBox.setMaximumRowCount(20);
        speciesJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "--- Select ---" }));
        speciesJComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speciesJComboBoxActionPerformed(evt);
            }
        });

        tissueLabel.setText("Tissue");

        tissueJComboBox.setMaximumRowCount(20);
        tissueJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "--- Select ---" }));

        cellTypeLabel.setText("Cell Type");

        cellTypeJComboBox.setMaximumRowCount(20);
        cellTypeJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "--- Select ---" }));

        jLabel1.setText("Additional Annotation");

        javax.swing.GroupLayout sampleDetailsPanelLayout = new javax.swing.GroupLayout(sampleDetailsPanel);
        sampleDetailsPanel.setLayout(sampleDetailsPanelLayout);
        sampleDetailsPanelLayout.setHorizontalGroup(
            sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sampleDetailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sampleCvScrollPane)
                    .addGroup(sampleDetailsPanelLayout.createSequentialGroup()
                        .addGroup(sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cellTypeLabel)
                            .addComponent(tissueLabel)
                            .addComponent(speciesLabel)
                            .addComponent(nameLabel))
                        .addGap(18, 18, 18)
                        .addGroup(sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(sampleNameJTextField)
                            .addComponent(speciesJComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(tissueJComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cellTypeJComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, sampleDetailsPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(sampleDetailsJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 184, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, sampleDetailsPanelLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 97, Short.MAX_VALUE)
                        .addComponent(preferredOntologiesLabel)))
                .addContainerGap())
        );
        sampleDetailsPanelLayout.setVerticalGroup(
            sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sampleDetailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nameLabel)
                    .addComponent(sampleNameJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(speciesLabel)
                    .addComponent(speciesJComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tissueLabel)
                    .addComponent(tissueJComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cellTypeLabel)
                    .addComponent(cellTypeJComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(preferredOntologiesLabel)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sampleCvScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 176, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sampleDetailsJButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        okButton.setText("OK");
        okButton.setEnabled(false);
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sampleDetailsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(okButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sampleDetailsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(okButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Open the popup menu.
     *
     * @param evt
     */
    private void sampleCvTermsJTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sampleCvTermsJTableMouseClicked
        if (evt.getButton() == 3) {

            int row = sampleCvTermsJTable.rowAtPoint(evt.getPoint());
            int column = sampleCvTermsJTable.columnAtPoint(evt.getPoint());

            sampleCvTermsJTable.changeSelection(row, column, false, false);

            this.moveUpJMenuItem.setEnabled(true);
            this.moveDownJMenuItem.setEnabled(true);

            if (row == sampleCvTermsJTable.getRowCount() - 1) {
                this.moveDownJMenuItem.setEnabled(false);
            }

            if (row == 0) {
                this.moveUpJMenuItem.setEnabled(false);
            }

            popupJMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        } else if (evt.getButton() == 1 && evt.getClickCount() == 2) {
            editJMenuItemActionPerformed(null);
        }
    }//GEN-LAST:event_sampleCvTermsJTableMouseClicked

    /**
     * Delete the selected row.
     *
     * @param evt
     */
    private void sampleCvTermsJTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_sampleCvTermsJTableKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_DELETE) {
            deleteSelectedRowJMenuItemActionPerformed(null);
        }
    }//GEN-LAST:event_sampleCvTermsJTableKeyReleased

    /**
     * Open the OLS Dialog.
     *
     * @param evt
     */
    private void sampleDetailsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sampleDetailsJButtonActionPerformed
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new OLSDialog(prideExportDialog, this, true, "singleSample", newtRoot, null);
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_sampleDetailsJButtonActionPerformed

    /**
     * Edit the selected row using the OLS Dialog.
     *
     * @param evt
     */
    private void editJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editJMenuItemActionPerformed
        int selectedRow = sampleCvTermsJTable.getSelectedRow();

        String searchTerm = (String) sampleCvTermsJTable.getValueAt(selectedRow, 3);
        String ontology = (String) sampleCvTermsJTable.getValueAt(selectedRow, 1);
        ontology = PrideExportDialog.getOntologyFromCvTerm(ontology);

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        if (newtRoot.indexOf(ontology) != -1) {
            ontology = newtRoot;
        }

        new OLSDialog(prideExportDialog, this, true, "singleSample", ontology, selectedRow, searchTerm);
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_editJMenuItemActionPerformed

    /**
     * Move the current row up.
     *
     * @param evt
     */
    private void moveUpJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveUpJMenuItemActionPerformed
        int selectedRow = sampleCvTermsJTable.getSelectedRow();
        int selectedColumn = sampleCvTermsJTable.getSelectedColumn();

        Object[] tempRow = new Object[]{
            sampleCvTermsJTable.getValueAt(selectedRow - 1, 0),
            sampleCvTermsJTable.getValueAt(selectedRow - 1, 1),
            sampleCvTermsJTable.getValueAt(selectedRow - 1, 2)
        };

        ((DefaultTableModel) sampleCvTermsJTable.getModel()).removeRow(selectedRow - 1);
        ((DefaultTableModel) sampleCvTermsJTable.getModel()).insertRow(selectedRow, tempRow);

        sampleCvTermsJTable.changeSelection(selectedRow - 1, selectedColumn, false, false);

        fixTableIndices();
    }//GEN-LAST:event_moveUpJMenuItemActionPerformed

    /**
     * Move the current row down.
     *
     * @param evt
     */
    private void moveDownJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveDownJMenuItemActionPerformed
        int selectedRow = sampleCvTermsJTable.getSelectedRow();
        int selectedColumn = sampleCvTermsJTable.getSelectedColumn();

        Object[] tempRow = new Object[]{
            sampleCvTermsJTable.getValueAt(selectedRow + 1, 0),
            sampleCvTermsJTable.getValueAt(selectedRow + 1, 1),
            sampleCvTermsJTable.getValueAt(selectedRow + 1, 2)
        };

        ((DefaultTableModel) sampleCvTermsJTable.getModel()).removeRow(selectedRow + 1);
        ((DefaultTableModel) sampleCvTermsJTable.getModel()).insertRow(selectedRow, tempRow);

        sampleCvTermsJTable.changeSelection(selectedRow + 1, selectedColumn, false, false);

        fixTableIndices();
    }//GEN-LAST:event_moveDownJMenuItemActionPerformed

    /**
     * Delete the selected row.
     *
     * @param evt
     */
    private void deleteSelectedRowJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteSelectedRowJMenuItemActionPerformed

        int selectedRow = sampleCvTermsJTable.getSelectedRow();

        if (selectedRow != -1) {

            ((DefaultTableModel) sampleCvTermsJTable.getModel()).removeRow(selectedRow);
            fixTableIndices();
            validateInput();
        }
    }//GEN-LAST:event_deleteSelectedRowJMenuItemActionPerformed

    /**
     * Add the sample to the export dialog and close.
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed

        ArrayList<CvTerm> cvTerms = new ArrayList<CvTerm>();

        // add species
        if (speciesJComboBox.getSelectedIndex() > 0
                && speciesJComboBox.getSelectedIndex() < speciesJComboBox.getItemCount() - 1) {

            if (!((String) speciesJComboBox.getSelectedItem()).equalsIgnoreCase(speciesSeparator)) {
                cvTerms.add(new CvTerm(
                        "NEWT",
                        speciesMap.get((String) speciesJComboBox.getSelectedItem()),
                        (String) speciesJComboBox.getSelectedItem(),
                        null));
            }
        }

        // add tissue type
        if (tissueJComboBox.getSelectedIndex() > 0
                && tissueJComboBox.getSelectedIndex() < tissueJComboBox.getItemCount() - 1) {

            cvTerms.add(new CvTerm(
                    "BTO",
                    tissueMap.get((String) tissueJComboBox.getSelectedItem()),
                    (String) tissueJComboBox.getSelectedItem(),
                    null));
        }

        // add cell type
        if (cellTypeJComboBox.getSelectedIndex() > 0
                && cellTypeJComboBox.getSelectedIndex() < cellTypeJComboBox.getItemCount() - 1) {

            cvTerms.add(new CvTerm(
                    "CL",
                    cellTypeMap.get((String) cellTypeJComboBox.getSelectedItem()),
                    (String) cellTypeJComboBox.getSelectedItem(),
                    null));
        }
        
        // add additional cv terms
        for (int i = 0; i < sampleCvTermsJTable.getRowCount(); i++) {
            cvTerms.add(new CvTerm(
                    (String) sampleCvTermsJTable.getValueAt(i, 1),
                    (String) sampleCvTermsJTable.getValueAt(i, 2),
                    (String) sampleCvTermsJTable.getValueAt(i, 3),
                    (String) sampleCvTermsJTable.getValueAt(i, 4)));
        }

        prideExportDialog.setSample(new Sample(sampleNameJTextField.getText(), cvTerms));
        dispose();
    }//GEN-LAST:event_okButtonActionPerformed

    /**
     * Close without saving.
     *
     * @param evt
     */
    private void sampleNameJTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_sampleNameJTextFieldKeyReleased
        validateInput();
    }//GEN-LAST:event_sampleNameJTextFieldKeyReleased

    /**
     * Validate the input.
     *
     * @param evt
     */
    private void speciesJComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_speciesJComboBoxActionPerformed
        validateInput();
    }//GEN-LAST:event_speciesJComboBoxActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox cellTypeJComboBox;
    private javax.swing.JLabel cellTypeLabel;
    private javax.swing.JMenuItem deleteSelectedRowJMenuItem;
    private javax.swing.JMenuItem editJMenuItem;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JMenuItem moveDownJMenuItem;
    private javax.swing.JMenuItem moveUpJMenuItem;
    private javax.swing.JLabel nameLabel;
    private javax.swing.JButton okButton;
    private javax.swing.JPopupMenu popupJMenu;
    private javax.swing.JLabel preferredOntologiesLabel;
    private javax.swing.JScrollPane sampleCvScrollPane;
    private javax.swing.JTable sampleCvTermsJTable;
    private javax.swing.JButton sampleDetailsJButton;
    private javax.swing.JPanel sampleDetailsPanel;
    private javax.swing.JTextField sampleNameJTextField;
    private javax.swing.JComboBox speciesJComboBox;
    private javax.swing.JLabel speciesLabel;
    private javax.swing.JComboBox tissueJComboBox;
    private javax.swing.JLabel tissueLabel;
    // End of variables declaration//GEN-END:variables

    /**
     * Fixes the indices so that they are in accending order starting from one
     */
    private void fixTableIndices() {
        for (int row = 0; row < ((DefaultTableModel) sampleCvTermsJTable.getModel()).getRowCount(); row++) {
            ((DefaultTableModel) sampleCvTermsJTable.getModel()).setValueAt(new Integer(row + 1), row, 0);
        }
    }

    /**
     * Enables the OK button if a valid sample set is selected.
     */
    private void validateInput() {
        
        String input = sampleNameJTextField.getText();
        for (String forbiddenCharacter : Util.forbiddenCharacters) {
            if (input.contains(forbiddenCharacter)) {
                JOptionPane.showMessageDialog(null, "'" + forbiddenCharacter + "' is not allowed in sample name.",
                    "Forbidden character", JOptionPane.ERROR_MESSAGE);
                sampleNameJTextField.setText(lastNameInput);
                return;
            }
        }
        lastNameInput = input;
        
        boolean allValidated = true;

        // highlight the fields that have not been filled
        if (sampleNameJTextField.getText().length() > 0) {
            nameLabel.setForeground(Color.BLACK);
        } else {
            nameLabel.setForeground(Color.RED);
            allValidated = false;
        }

        if (speciesJComboBox.getSelectedIndex() > 0
                && speciesJComboBox.getSelectedIndex() < speciesJComboBox.getItemCount() - 1) {

            if (!((String) speciesJComboBox.getSelectedItem()).equalsIgnoreCase(speciesSeparator)) {
                speciesLabel.setForeground(Color.BLACK);
            } else {
                speciesLabel.setForeground(Color.RED);
                allValidated = false;
            }
        } else {
            speciesLabel.setForeground(Color.RED);
            allValidated = false;
        }

        okButton.setEnabled(allValidated);
    }

    @Override
    public void insertOLSResult(String field, String selectedValue, String accession, String ontologyShort,
            String ontologyLong, int modifiedRow, String mappedTerm, Map<String, String> metadata) {
        addSampleDetails(selectedValue, accession, ontologyShort, modifiedRow);
    }

    @Override
    public Window getWindow() {
        return (Window) this;
    }

    /**
     * Add a sample cv term to the table.
     *
     * @param name
     * @param accession
     * @param ontology
     * @param modifiedRow the row to modify, use -1 if adding a new row
     */
    public void addSampleDetails(String name, String accession, String ontology, int modifiedRow) {
        addSampleDetails(name, accession, ontology, null, modifiedRow);
    }

    /**
     * Add a sample cv term to the table.
     *
     * @param name
     * @param accession
     * @param ontology
     * @param value
     * @param modifiedRow the row to modify, use -1 if adding a new row
     */
    public void addSampleDetails(String name, String accession, String ontology, String value, int modifiedRow) {

        if (modifiedRow == -1) {

            ((DefaultTableModel) this.sampleCvTermsJTable.getModel()).addRow(
                    new Object[]{
                        new Integer(sampleCvTermsJTable.getRowCount() + 1),
                        ontology,
                        accession,
                        name,
                        value
                    });
        } else {
            sampleCvTermsJTable.setValueAt(ontology, modifiedRow, 1);
            sampleCvTermsJTable.setValueAt(accession, modifiedRow, 2);
            sampleCvTermsJTable.setValueAt(name, modifiedRow, 3);
            sampleCvTermsJTable.setValueAt(null, modifiedRow, 4);
        }

        validateInput();
    }
}