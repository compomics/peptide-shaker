package eu.isas.peptideshaker.fileimport;

import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingDialog;
import java.awt.Frame;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;

/**
 * A dialog for selecting missing mgf files.
 *
 * @author Marc Vaudel
 */
public class MgfFilesNotFoundDialog extends javax.swing.JDialog {

    /**
     * Map of the missing mgf files indexed by ID file.
     */
    private HashMap<File, String> missingFiles;
    /**
     * Map of the new mgf files indexed by the new ones.
     */
    private HashMap<String, File> newFiles = new HashMap<String, File>();
    /**
     * The list of id files presenting a missing mgf file.
     */
    private ArrayList<File> idFiles;
    /**
     * The last selected folder.
     */
    private File lastSelectedFolder;
    /**
     * The waiting dialog.
     */
    private WaitingDialog waitingDialog;
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();

    /**
     * Creates a new MgfFilesNotFoundDialog.
     *
     * @param waitingDialog a reference to the waiting dialog
     * @param missingFiles the list of missing mgf files.
     */
    public MgfFilesNotFoundDialog(WaitingDialog waitingDialog, HashMap<File, String> missingFiles) {
        super(waitingDialog, true);

        this.waitingDialog = waitingDialog;
        setLocationRelativeTo(waitingDialog);
        this.missingFiles = missingFiles;

        idFiles = new ArrayList<File>(missingFiles.keySet());
        lastSelectedFolder = idFiles.get(0).getParentFile();

        initComponents();

        newFolderLoaded();
        fileTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileTable.getColumn(" ").setMaxWidth(30);

        setVisible(true);
    }

    /**
     * Table model for the file table.
     */
    private class FileTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return idFiles.size();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return " ";
                case 1:
                    return "Id File";
                case 2:
                    return "Expected File";
                case 3:
                    return "Spectrum File";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            try {
                switch (column) {
                    case 0:
                        return row + 1;
                    case 1:
                        return idFiles.get(row).getName();
                    case 2:
                        return missingFiles.get(idFiles.get(row));
                    case 3:
                        if (newFiles.containsKey(missingFiles.get(idFiles.get(row)))) {
                            return newFiles.get(missingFiles.get(idFiles.get(row))).getName();
                        }
                        return "";
                    default:
                        return "";
                }
            } catch (Exception e) {
                return "Error: " + e.getLocalizedMessage();
            }
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
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }

    /**
     * Loads files from the selected folder.
     */
    private void newFolderLoaded() {
        folderTxt.setText(lastSelectedFolder.getAbsolutePath());
        updateFileList();
    }

    /**
     * Updates the file list of the selected folder.
     */
    private void updateFileList() {
        File[] files = lastSelectedFolder.listFiles();

        ArrayList<String> fileNames = new ArrayList<String>();
        String fileName;
        boolean found;
        for (File file : files) {
            fileName = file.getName();
            found = false;
            if (fileName.endsWith(".mgf")) {
                for (File newFile : newFiles.values()) {
                    if (newFile.getName().equals(fileName)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    fileNames.add(fileName);
                }
            }
        }
        Collections.sort(fileNames);
        String[] fileNamesArray = new String[fileNames.size()];
        for (int i = 0; i < fileNames.size(); i++) {
            fileNamesArray[i] = fileNames.get(i);
        }
        fileList.setListData(fileNamesArray);
    }

    /**
     * Validates the input.
     *
     * @return a boolean indicating whether a spectrum file is given for every
     * id file
     */
    private boolean validateInput() {
        for (File idFile : missingFiles.keySet()) {
            if (!newFiles.keySet().contains(missingFiles.get(idFile))) {
                JOptionPane.showMessageDialog(null, "Please select the spectrum file corresponding to " + idFile.getName() + ".",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return true;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        fileTable = new javax.swing.JTable();
        addButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        fileList = new javax.swing.JList();
        jLabel2 = new javax.swing.JLabel();
        browseButton = new javax.swing.JButton();
        folderTxt = new javax.swing.JTextField();
        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        jLabel1.setText("Spectrum files needed for identification processing were missing. Select them manually:");

        fileTable.setModel(new FileTable());
        jScrollPane1.setViewportView(fileTable);

        addButton.setText("<<");
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        removeButton.setText(">>");
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonActionPerformed(evt);
            }
        });

        fileList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(fileList);

        jLabel2.setText("Folder:");

        browseButton.setText("Browse");
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        folderTxt.setEditable(false);

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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 437, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(addButton)
                            .addComponent(removeButton))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addGap(18, 18, 18)
                                .addComponent(folderTxt)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(browseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 327, Short.MAX_VALUE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(593, 593, 593)
                        .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(89, 89, 89)
                        .addComponent(addButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(removeButton))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel2)
                                    .addComponent(browseButton)
                                    .addComponent(folderTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jScrollPane2))
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 295, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton)
                    .addComponent(okButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Lets the user select the folder to find the missing files in.
     * 
     * @param evt 
     */
    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Change Folder");

        FileFilter filter = new FileFilter() {

            @Override
            public boolean accept(File myFile) {
                return myFile.getName().toLowerCase().endsWith("mgf")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "New Folder";
            }
        };

        fileChooser.setFileFilter(filter);
        int returnVal = fileChooser.showDialog(null, "Open");

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File mgfFolder = fileChooser.getSelectedFile();
            if (!mgfFolder.isDirectory()) {
                mgfFolder = mgfFolder.getParentFile();
            }
            lastSelectedFolder = mgfFolder;
            newFolderLoaded();
        }
    }//GEN-LAST:event_browseButtonActionPerformed

    /**
     * Cancels the selection, closes the dialog and then cancels the import.
     * 
     * @param evt 
     */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        waitingDialog.setRunCanceled();
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    /**
     * Saves the mgf files and closes the dialog.
     * 
     * @param evt 
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        if (validateInput()) {
            for (String idName : newFiles.keySet()) {
                spectrumFactory.addIdNameMapping(idName, newFiles.get(idName));
            }
            dispose();
        }
    }//GEN-LAST:event_okButtonActionPerformed

    /**
     * Add an mgf file.
     * 
     * @param evt 
     */
    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        int row = fileTable.getSelectedRow();
        if (row == -1) {
            for (int i = 0; i < fileTable.getRowCount(); i++) {
                if (!newFiles.containsKey(missingFiles.get(idFiles.get(i)))) {
                    row = i;
                    break;
                }
            }
        }
        String newFile = (String) fileList.getSelectedValue();
        String mgfFile = missingFiles.get(idFiles.get(row));
        newFiles.put(mgfFile, new File(lastSelectedFolder, newFile));
        DefaultTableModel dm = (DefaultTableModel) fileTable.getModel();
        dm.fireTableDataChanged();
        updateFileList();
    }//GEN-LAST:event_addButtonActionPerformed

    /**
     * Remove an mgf file from the list.
     * 
     * @param evt 
     */
    private void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeButtonActionPerformed
        int row = fileTable.getSelectedRow();
        if (row >= 0) {
            newFiles.remove(missingFiles.get(idFiles.get(row)));
            DefaultTableModel dm = (DefaultTableModel) fileTable.getModel();
            dm.fireTableDataChanged();
            updateFileList();
        }
    }//GEN-LAST:event_removeButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JButton browseButton;
    private javax.swing.JButton cancelButton;
    private javax.swing.JList fileList;
    private javax.swing.JTable fileTable;
    private javax.swing.JTextField folderTxt;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JButton okButton;
    private javax.swing.JButton removeButton;
    // End of variables declaration//GEN-END:variables
}
