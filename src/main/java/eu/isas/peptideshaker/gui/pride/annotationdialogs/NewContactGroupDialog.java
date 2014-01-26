package eu.isas.peptideshaker.gui.pride.annotationdialogs;

import com.compomics.util.Util;
import com.compomics.util.pride.prideobjects.Contact;
import com.compomics.util.pride.prideobjects.ContactGroup;
import eu.isas.peptideshaker.gui.pride.ProjectExportDialog;
import java.awt.Color;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;

/**
 * A dialog for creating new contact groups and editing old ones.
 *
 * @author Harald Barsnes
 */
public class NewContactGroupDialog extends javax.swing.JDialog {

    /**
     * A reference to the PRIDE export dialog.
     */
    private ProjectExportDialog prideExportDialog;
    /**
     * The last valid input for contact name
     */
    private String lastNameInput = "";

    /**
     * Creates a new NewContactDialog.
     *
     * @param prideExportDialog
     * @param modal
     */
    public NewContactGroupDialog(ProjectExportDialog prideExportDialog, boolean modal) {
        super(prideExportDialog, modal);
        this.prideExportDialog = prideExportDialog;
        initComponents();
        setUpGUI();
        validateInput();
        setLocationRelativeTo(prideExportDialog);
        contactsJTableMouseReleased(null);
        setVisible(true);
    }

    /**
     * Creates a new NewContactDialog.
     *
     * @param prideExportDialog
     * @param modal
     * @param contactGroup
     */
    public NewContactGroupDialog(ProjectExportDialog prideExportDialog, boolean modal, ContactGroup contactGroup) {
        super(prideExportDialog, modal);
        this.prideExportDialog = prideExportDialog;
        initComponents();
        setUpGUI();

        groupNameTextField.setText(contactGroup.getName());

        for (int i = 0; i < contactGroup.getContacts().size(); i++) {
            ((DefaultTableModel) contactsJTable.getModel()).addRow(new Object[]{
                        contactsJTable.getRowCount() + 1,
                        contactGroup.getContacts().get(i).getName(),
                        contactGroup.getContacts().get(i).getEMail(),
                        contactGroup.getContacts().get(i).getInstitution()
                    });
        }

        validateInput();
        contactsJTableMouseReleased(null);
        setTitle("Edit Contacts");
        setLocationRelativeTo(prideExportDialog);
        setVisible(true);
    }
    
    /**
     * Set up the GUI.
     */
    private void setUpGUI() {
        contactsScrollPane.getViewport().setOpaque(false);
        contactsJTable.getTableHeader().setReorderingAllowed(false);

        // correct the color for the upper right corner
        JPanel proteinCorner = new JPanel();
        proteinCorner.setBackground(contactsJTable.getTableHeader().getBackground());
        contactsScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, proteinCorner);
        
        // the index column
        contactsJTable.getColumn(" ").setMaxWidth(50);
        contactsJTable.getColumn(" ").setMinWidth(50);
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
        backgroundPanel = new javax.swing.JPanel();
        contactPanel = new javax.swing.JPanel();
        contactsScrollPane = new javax.swing.JScrollPane();
        contactsJTable = new javax.swing.JTable();
        addButton = new javax.swing.JButton();
        groupNameLabel = new javax.swing.JLabel();
        groupNameTextField = new javax.swing.JTextField();
        deleteGroupButton = new javax.swing.JButton();
        okJButton = new javax.swing.JButton();
        groupNameNoteLabel = new javax.swing.JLabel();
        cancelJButton = new javax.swing.JButton();

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
        setTitle("New Contact Group");

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        contactPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Contacts"));
        contactPanel.setOpaque(false);

        contactsScrollPane.setOpaque(false);

        contactsJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Name", "E-mail", "Institute"
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
        contactsJTable.setOpaque(false);
        contactsJTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        contactsJTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                contactsJTableMouseClicked(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                contactsJTableMouseReleased(evt);
            }
        });
        contactsScrollPane.setViewportView(contactsJTable);

        addButton.setText("Add Contact");
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        groupNameLabel.setText("Group Name*");

        groupNameTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                groupNameTextFieldKeyReleased(evt);
            }
        });

        deleteGroupButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Error_3.png"))); // NOI18N
        deleteGroupButton.setToolTipText("Delete Contact Group");
        deleteGroupButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                deleteGroupButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                deleteGroupButtonMouseExited(evt);
            }
        });
        deleteGroupButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteGroupButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout contactPanelLayout = new javax.swing.GroupLayout(contactPanel);
        contactPanel.setLayout(contactPanelLayout);
        contactPanelLayout.setHorizontalGroup(
            contactPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(contactPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(contactPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(contactsScrollPane, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, contactPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(addButton))
                    .addGroup(contactPanelLayout.createSequentialGroup()
                        .addComponent(groupNameLabel)
                        .addGap(18, 18, 18)
                        .addComponent(groupNameTextField)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deleteGroupButton, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        contactPanelLayout.setVerticalGroup(
            contactPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(contactPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(contactPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(groupNameLabel)
                    .addComponent(groupNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(deleteGroupButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(contactsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 235, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(addButton)
                .addContainerGap())
        );

        contactPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {deleteGroupButton, groupNameTextField});

        okJButton.setText("OK");
        okJButton.setEnabled(false);
        okJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okJButtonActionPerformed(evt);
            }
        });

        groupNameNoteLabel.setFont(groupNameNoteLabel.getFont().deriveFont((groupNameNoteLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        groupNameNoteLabel.setText("Note that Group Name is only for internal reference and is not included in the PRIDE XML.");

        cancelJButton.setText("Cancel");
        cancelJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelJButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(contactPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(groupNameNoteLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 74, Short.MAX_VALUE)
                        .addComponent(okJButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelJButton)))
                .addContainerGap())
        );

        backgroundPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelJButton, okJButton});

        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(contactPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(okJButton)
                    .addComponent(groupNameNoteLabel)
                    .addComponent(cancelJButton))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Saves the contact and closes the dialog.
     *
     * @param evt
     */
    private void okJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okJButtonActionPerformed

        ArrayList<Contact> contacts = new ArrayList<Contact>();

        for (int i = 0; i < contactsJTable.getRowCount(); i++) {
            contacts.add(new Contact(
                    (String) contactsJTable.getValueAt(i, 1),
                    (String) contactsJTable.getValueAt(i, 2),
                    (String) contactsJTable.getValueAt(i, 3)));
        }

        prideExportDialog.setContacts(new ContactGroup(contacts, groupNameTextField.getText()));
        dispose();
    }//GEN-LAST:event_okJButtonActionPerformed

    /**
     * Enable/disable the delete button.
     *
     * @param evt
     */
    private void contactsJTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_contactsJTableMouseReleased
        int selectedRow = contactsJTable.getSelectedRow();
        deleteSelectedRowJMenuItem.setEnabled(selectedRow != -1);
    }//GEN-LAST:event_contactsJTableMouseReleased

    /**
     * Open the New Contact dialog.
     *
     * @param evt
     */
    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        new NewContactDialog(this, true);
    }//GEN-LAST:event_addButtonActionPerformed

    /**
     * Checks if all mandatory information is filled in. Enables or disables the
     * OK button.
     */
    private void groupNameTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_groupNameTextFieldKeyReleased
        validateInput();
    }//GEN-LAST:event_groupNameTextFieldKeyReleased

    private void editJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editJMenuItemActionPerformed
        int selectedRow = contactsJTable.getSelectedRow();

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new NewContactDialog(this, true, new Contact(
                    (String) contactsJTable.getValueAt(selectedRow, 1),
                    (String) contactsJTable.getValueAt(selectedRow, 2),
                    (String) contactsJTable.getValueAt(selectedRow, 3)), 
                    selectedRow);
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_editJMenuItemActionPerformed

    private void moveUpJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveUpJMenuItemActionPerformed
        int selectedRow = contactsJTable.getSelectedRow();
        int selectedColumn = contactsJTable.getSelectedColumn();

        Object[] tempRow = new Object[]{
            contactsJTable.getValueAt(selectedRow - 1, 0),
            contactsJTable.getValueAt(selectedRow - 1, 1),
            contactsJTable.getValueAt(selectedRow - 1, 2),
            contactsJTable.getValueAt(selectedRow - 1, 3)
        };

        ((DefaultTableModel) contactsJTable.getModel()).removeRow(selectedRow - 1);
        ((DefaultTableModel) contactsJTable.getModel()).insertRow(selectedRow, tempRow);

        contactsJTable.changeSelection(selectedRow - 1, selectedColumn, false, false);

        updateTableIndexes();
    }//GEN-LAST:event_moveUpJMenuItemActionPerformed

    private void moveDownJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveDownJMenuItemActionPerformed
        int selectedRow = contactsJTable.getSelectedRow();
        int selectedColumn = contactsJTable.getSelectedColumn();

        Object[] tempRow = new Object[]{
            contactsJTable.getValueAt(selectedRow + 1, 0),
            contactsJTable.getValueAt(selectedRow + 1, 1),
            contactsJTable.getValueAt(selectedRow + 1, 2),
            contactsJTable.getValueAt(selectedRow + 1, 3)
        };

        ((DefaultTableModel) contactsJTable.getModel()).removeRow(selectedRow + 1);
        ((DefaultTableModel) contactsJTable.getModel()).insertRow(selectedRow, tempRow);

        contactsJTable.changeSelection(selectedRow + 1, selectedColumn, false, false);

        updateTableIndexes();
    }//GEN-LAST:event_moveDownJMenuItemActionPerformed

    private void deleteSelectedRowJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteSelectedRowJMenuItemActionPerformed

        int selectedRow = contactsJTable.getSelectedRow();

        if (selectedRow != -1) {

            ((DefaultTableModel) contactsJTable.getModel()).removeRow(selectedRow);
            updateTableIndexes();
            validateInput();
        }
    }//GEN-LAST:event_deleteSelectedRowJMenuItemActionPerformed

    /**
     * Open the popup menu.
     *
     * @param evt
     */
    private void contactsJTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_contactsJTableMouseClicked
        if (evt.getButton() == 3) {

            int row = contactsJTable.rowAtPoint(evt.getPoint());
            int column = contactsJTable.columnAtPoint(evt.getPoint());

            contactsJTable.changeSelection(row, column, false, false);

            this.moveUpJMenuItem.setEnabled(true);
            this.moveDownJMenuItem.setEnabled(true);

            if (row == contactsJTable.getRowCount() - 1) {
                this.moveDownJMenuItem.setEnabled(false);
            }

            if (row == 0) {
                this.moveUpJMenuItem.setEnabled(false);
            }

            popupJMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        } else if (evt.getButton() == 1 && evt.getClickCount() == 2) {
            editJMenuItemActionPerformed(null);
        }
    }//GEN-LAST:event_contactsJTableMouseClicked

    /**
     * Delete the given group and close the dialog.
     * 
     * @param evt 
     */
    private void deleteGroupButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteGroupButtonActionPerformed
        dispose();
        prideExportDialog.deleteContactGroup(new ContactGroup(new ArrayList<Contact>(), groupNameTextField.getText()));
    }//GEN-LAST:event_deleteGroupButtonActionPerformed

    /**
     * Changes the cursor into a hand cursor.
     *
     * @param evt
     */
    private void deleteGroupButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deleteGroupButtonMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_deleteGroupButtonMouseEntered

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void deleteGroupButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deleteGroupButtonMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_deleteGroupButtonMouseExited

     /**
     * Close the dialog without saving.
     * 
     * @param evt 
     */
    private void cancelJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelJButtonActionPerformed
        dispose();
    }//GEN-LAST:event_cancelJButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton cancelJButton;
    private javax.swing.JPanel contactPanel;
    private javax.swing.JTable contactsJTable;
    private javax.swing.JScrollPane contactsScrollPane;
    private javax.swing.JButton deleteGroupButton;
    private javax.swing.JMenuItem deleteSelectedRowJMenuItem;
    private javax.swing.JMenuItem editJMenuItem;
    private javax.swing.JLabel groupNameLabel;
    private javax.swing.JLabel groupNameNoteLabel;
    private javax.swing.JTextField groupNameTextField;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JMenuItem moveDownJMenuItem;
    private javax.swing.JMenuItem moveUpJMenuItem;
    private javax.swing.JButton okJButton;
    private javax.swing.JPopupMenu popupJMenu;
    // End of variables declaration//GEN-END:variables

    /**
     * Checks if all mandatory information is filled in. Enables or disables the
     * OK button.
     */
    private void validateInput() {

        String input = groupNameTextField.getText();
        for (String forbiddenCharacter : Util.forbiddenCharacters) {
            if (input.contains(forbiddenCharacter)) {
                JOptionPane.showMessageDialog(null, "'" + forbiddenCharacter + "' is not allowed in group names.",
                        "Forbidden Character", JOptionPane.ERROR_MESSAGE);
                groupNameTextField.setText(lastNameInput);
                return;
            }
        }
        lastNameInput = input;

        if (groupNameTextField.getText().length() > 0
                && contactsJTable.getRowCount() > 0) {
            okJButton.setEnabled(true);
        } else {
            okJButton.setEnabled(false);
        }

        // highlight the fields that have not been filled
        if (groupNameTextField.getText().length() > 0) {
            groupNameLabel.setForeground(Color.BLACK);
        } else {
            groupNameLabel.setForeground(Color.RED);
        }
    }

    /**
     * Add a new conctact to the table.
     *
     * @param contact
     */
    public void insertContact(Contact contact) {
        ((DefaultTableModel) contactsJTable.getModel()).addRow(new Object[]{
                    contactsJTable.getRowCount() + 1,
                    contact.getName(),
                    contact.getEMail(),
                    contact.getInstitution()
                });
        validateInput();
    }
    
    /**
     * Add a new conctact to the table.
     *
     * @param contact
     * @param row the index of the row to edit 
     */
    public void editContact(Contact contact, int row) {
        contactsJTable.setValueAt(contact.getName(), row, 1);
        contactsJTable.setValueAt(contact.getEMail(), row, 2);
        contactsJTable.setValueAt(contact.getInstitution(), row, 3);
    }
    
    /**
     * Update the table indexes.
     */
    private void updateTableIndexes() {
        for (int i=0;i<contactsJTable.getRowCount(); i++) {
            contactsJTable.setValueAt(i+1, i, 0);
        }
    }
}