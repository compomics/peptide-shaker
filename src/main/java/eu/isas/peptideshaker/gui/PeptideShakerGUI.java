package eu.isas.peptideshaker.gui;

import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.io.ExperimentIO;
import com.compomics.util.experiment.refinementparameters.MascotScore;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import com.jgoodies.looks.plastic.theme.SkyKrupp;
import eu.isas.peptideshaker.export.CsvExporter;
import eu.isas.peptideshaker.fdrestimation.PeptideSpecificMap;
import eu.isas.peptideshaker.fdrestimation.PsmSpecificMap;
import eu.isas.peptideshaker.fdrestimation.TargetDecoyMap;
import eu.isas.peptideshaker.myparameters.PSMaps;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.renderers.TrueFalseIconRenderer;
import eu.isas.peptideshaker.utils.Properties;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableModel;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;

/**
 * The main frame of the PeptideShaker.
 *
 * @author  Harald Barsnes
 * @author  Marc Vaudel
 */
public class PeptideShakerGUI extends javax.swing.JFrame implements ProgressDialogParent {

    /**
     * If set to true all messages will be sent to a log file.
     */
    private static boolean useLogFile = true;
    /**
     * The last folder opened by the user. Defaults to user.home.
     */
    private String lastSelectedFolder = "user.home";
    /**
     * The compomics experiment
     */
    private MsExperiment experiment;
    /**
     * The investigated sample
     */
    private Sample sample;
    /**
     * The replicate number
     */
    private int replicateNumber;
    /**
     * The specific target/decoy map at the psm level
     */
    private PsmSpecificMap psmMap;
    /**
     * The specific target/decoy map at the peptide level
     */
    private PeptideSpecificMap peptideMap;
    /**
     * The target/decoy map at the protein level
     */
    private TargetDecoyMap proteinMap;
    /**
     * The color used for the sparkline bar chart plots.
     */
    private Color sparklineColor = new Color(100, 100, 255);
    /**
     * Compomics experiment saver and opener
     */
    private ExperimentIO experimentIO = new ExperimentIO();
    /**
     * A simple progress dialog.
     */
    private static ProgressDialog progressDialog;
    /**
     * If set to true the progress stopped and the simple progress dialog
     * disposed.
     */
    private boolean cancelProgress = false;

    /**
     * The main method used to start PeptideShaker
     * 
     * @param args
     */
    public static void main(String[] args) {

        // update the look and feel after adding the panels
        setLookAndFeel();

        new PeptideShakerGUI();
    }

    /**
     * Creates a new PeptideShaker frame.
     */
    public PeptideShakerGUI() {

        // set up the ErrorLog
        setUpLogFile();

        initComponents();

        // set up the table column properties
        setColumnProperies();

        // disable the Quantification and PTM Analysis tabs for now
        resultsJTabbedPane.setEnabledAt(3, false);
        resultsJTabbedPane.setEnabledAt(4, false);

        setTitle(this.getTitle() + " " + new Properties().getVersion());

        setLocationRelativeTo(null);
        setVisible(true);

        // open the OpenDialog
        new OpenDialog(this, true);
    }

    /**
     * Set the properties for the columns in the results tables.
     */
    private void setColumnProperies() {

        proteinsJTable.getTableHeader().setReorderingAllowed(false);
        peptidesJTable.getTableHeader().setReorderingAllowed(false);
        spectraJTable.getTableHeader().setReorderingAllowed(false);
        quantificationJTable.getTableHeader().setReorderingAllowed(false);

        proteinsJTable.setAutoCreateRowSorter(true);
        peptidesJTable.setAutoCreateRowSorter(true);
        spectraJTable.setAutoCreateRowSorter(true);
        quantificationJTable.setAutoCreateRowSorter(true);

        proteinsJTable.getColumn(" ").setMaxWidth(70);
        peptidesJTable.getColumn(" ").setMaxWidth(70);
        spectraJTable.getColumn(" ").setMaxWidth(70);
        quantificationJTable.getColumn(" ").setMaxWidth(70);

        proteinsJTable.getColumn("Decoy").setMaxWidth(60);
        peptidesJTable.getColumn("Decoy").setMaxWidth(60);
        spectraJTable.getColumn("Decoy").setMaxWidth(60);

        proteinsJTable.getColumn("#Peptides").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        proteinsJTable.getColumn("#Spectra").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        proteinsJTable.getColumn("p-score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 1.0, sparklineColor));
        proteinsJTable.getColumn("PEP").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 1.0, sparklineColor));

        peptidesJTable.getColumn("#Spectra").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        peptidesJTable.getColumn("p-score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 1.0, sparklineColor));
        peptidesJTable.getColumn("PEP").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 1.0, sparklineColor));

        spectraJTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        spectraJTable.getColumn("Mass Error").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        spectraJTable.getColumn("Mascot Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        spectraJTable.getColumn("Mascot e-value").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        spectraJTable.getColumn("OMSSA e-value").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        spectraJTable.getColumn("X!Tandem e-value").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        spectraJTable.getColumn("p-score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 1.0, sparklineColor));
        spectraJTable.getColumn("PEP").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 1.0, sparklineColor));

        proteinsJTable.getColumn("Decoy").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")), null));
        peptidesJTable.getColumn("Decoy").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")), null));
        spectraJTable.getColumn("Decoy").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")), null));
    }

    /**
     * Returns the last selected folder.
     *
     * @return the last selected folder
     */
    public String getLastSelectedFolder() {
        return lastSelectedFolder;
    }

    /**
     * Set the last selected folder.
     *
     * @param lastSelectedFolder the folder to set
     */
    public void setLastSelectedFolder(String lastSelectedFolder) {
        this.lastSelectedFolder = lastSelectedFolder;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        resultsJTabbedPane = new javax.swing.JTabbedPane();
        proteinsJScrollPane = new javax.swing.JScrollPane();
        proteinsJTable = new javax.swing.JTable();
        peptidesJScrollPane = new javax.swing.JScrollPane();
        peptidesJTable = new javax.swing.JTable();
        spectraJScrollPane = new javax.swing.JScrollPane();
        spectraJTable = new javax.swing.JTable();
        quantificationJScrollPane = new javax.swing.JScrollPane();
        quantificationJTable = new javax.swing.JTable();
        ptmAnalysisScrollPane = new javax.swing.JScrollPane();
        ptmAnalysisJTable = new javax.swing.JTable();
        menuBar = new javax.swing.JMenuBar();
        fileJMenu = new javax.swing.JMenu();
        openJMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        saveMenuItem = new javax.swing.JMenuItem();
        exportMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        exitJMenuItem = new javax.swing.JMenuItem();
        viewJMenu = new javax.swing.JMenu();
        sparklinesJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        helpMenu = new javax.swing.JMenu();
        helpJMenuItem = new javax.swing.JMenuItem();
        aboutJMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("PeptideShaker");

        proteinsJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Protein", "#Peptides", "#Spectra", "p-score", "PEP", "Decoy"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        proteinsJScrollPane.setViewportView(proteinsJTable);

        resultsJTabbedPane.addTab("Proteins", proteinsJScrollPane);

        peptidesJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Protein(s)", "Sequence", "Variable Modification(s)", "#Spectra", "p-score", "PEP", "Decoy"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        peptidesJScrollPane.setViewportView(peptidesJTable);

        resultsJTabbedPane.addTab("Peptides", peptidesJScrollPane);

        spectraJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Protein(s)", "Sequence", "Variable Modification(s)", "Charge", "Spectrum", "Spectrum File", "Identification File(s)", "Mass Error", "Mascot Score", "Mascot e-value", "OMSSA e-value", "X!Tandem e-value", "p-score", "PEP", "Decoy"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        spectraJScrollPane.setViewportView(spectraJTable);

        resultsJTabbedPane.addTab("Spectra", spectraJScrollPane);

        quantificationJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Title 2", "Title 3", "Title 4"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        quantificationJScrollPane.setViewportView(quantificationJTable);

        resultsJTabbedPane.addTab("Quantification", quantificationJScrollPane);

        ptmAnalysisJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        ptmAnalysisScrollPane.setViewportView(ptmAnalysisJTable);

        resultsJTabbedPane.addTab("PTM Analysis", ptmAnalysisScrollPane);

        fileJMenu.setMnemonic('F');
        fileJMenu.setText("File");

        openJMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openJMenuItem.setMnemonic('O');
        openJMenuItem.setText("Open");
        openJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openJMenuItemActionPerformed(evt);
            }
        });
        fileJMenu.add(openJMenuItem);
        fileJMenu.add(jSeparator2);

        saveMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        saveMenuItem.setMnemonic('S');
        saveMenuItem.setText("Save As");
        saveMenuItem.setEnabled(false);
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });
        fileJMenu.add(saveMenuItem);

        exportMenuItem.setMnemonic('E');
        exportMenuItem.setText("Export");
        exportMenuItem.setEnabled(false);
        exportMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportMenuItemActionPerformed(evt);
            }
        });
        fileJMenu.add(exportMenuItem);
        fileJMenu.add(jSeparator1);

        exitJMenuItem.setMnemonic('x');
        exitJMenuItem.setText("Exit");
        exitJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitJMenuItemActionPerformed(evt);
            }
        });
        fileJMenu.add(exitJMenuItem);

        menuBar.add(fileJMenu);

        viewJMenu.setMnemonic('V');
        viewJMenu.setText("View");

        sparklinesJCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        sparklinesJCheckBoxMenuItem.setSelected(true);
        sparklinesJCheckBoxMenuItem.setText("JSparklines");
        sparklinesJCheckBoxMenuItem.setToolTipText("View sparklines or the underlying numbers");
        sparklinesJCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sparklinesJCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewJMenu.add(sparklinesJCheckBoxMenuItem);

        menuBar.add(viewJMenu);

        helpMenu.setMnemonic('H');
        helpMenu.setText("Help");

        helpJMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        helpJMenuItem.setMnemonic('H');
        helpJMenuItem.setText("Help");
        helpJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpJMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(helpJMenuItem);

        aboutJMenuItem.setMnemonic('A');
        aboutJMenuItem.setText("About");
        aboutJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutJMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutJMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(resultsJTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1080, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(resultsJTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 646, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Opens a dialog where the identification files to analyzed are selected.
     *
     * @param evt
     */
    private void openJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openJMenuItemActionPerformed
        new OpenDialog(this, true);
    }//GEN-LAST:event_openJMenuItemActionPerformed

    /**
     * Closes the PeptideShaker
     *
     * @param evt
     */
    private void exitJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitJMenuItemActionPerformed
        System.exit(0);
    }//GEN-LAST:event_exitJMenuItemActionPerformed

    /**
     * Updates the sparklines to show charts or numbers based on the current
     * selection of the menu item.
     *
     * @param evt
     */
    private void sparklinesJCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sparklinesJCheckBoxMenuItemActionPerformed
        ((JSparklinesBarChartTableCellRenderer) proteinsJTable.getColumn("#Peptides").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) proteinsJTable.getColumn("#Spectra").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) proteinsJTable.getColumn("p-score").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) proteinsJTable.getColumn("PEP").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());

        ((JSparklinesBarChartTableCellRenderer) peptidesJTable.getColumn("#Spectra").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) peptidesJTable.getColumn("p-score").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) peptidesJTable.getColumn("PEP").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());

        ((JSparklinesBarChartTableCellRenderer) spectraJTable.getColumn("Charge").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) spectraJTable.getColumn("Mass Error").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) spectraJTable.getColumn("Mascot Score").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) spectraJTable.getColumn("Mascot e-value").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) spectraJTable.getColumn("OMSSA e-value").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) spectraJTable.getColumn("X!Tandem e-value").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) spectraJTable.getColumn("p-score").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) spectraJTable.getColumn("PEP").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());

        proteinsJTable.revalidate();
        proteinsJTable.repaint();

        peptidesJTable.revalidate();
        peptidesJTable.repaint();

        spectraJTable.revalidate();
        spectraJTable.repaint();
    }//GEN-LAST:event_sparklinesJCheckBoxMenuItemActionPerformed

    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed

        final JFileChooser fileChooser = new JFileChooser(lastSelectedFolder);
        fileChooser.setDialogTitle("Save As...");
        fileChooser.setMultiSelectionEnabled(false);

        int returnVal = fileChooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {

            lastSelectedFolder = fileChooser.getCurrentDirectory().getPath();

            cancelProgress = false;

            progressDialog = new ProgressDialog(this, this, true);
            progressDialog.doNothingOnClose();

            final PeptideShakerGUI tempRef = this; // needed due to threading issues

            new Thread(new Runnable() {

                public void run() {
                    progressDialog.setIntermidiate(true);
                    progressDialog.setTitle("Saving. Please Wait...");
                    progressDialog.setVisible(true);
                }
            }, "ProgressDialog").start();

            new Thread("SaveThread") {

                @Override
                public void run() {

                    String selectedFile = fileChooser.getSelectedFile().getPath();

                    if (!selectedFile.endsWith(".cps")) {
                        selectedFile += ".cps";
                    }

                    // @TODO: add check for if a file is about to be overwritten

                    try {
                        experimentIO.save(new File(selectedFile), experiment);

                        progressDialog.setVisible(false);
                        progressDialog.dispose();

                        JOptionPane.showMessageDialog(tempRef, "Identifications were successfully saved.", "Save Successful", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e) {

                        progressDialog.setVisible(false);
                        progressDialog.dispose();

                        JOptionPane.showMessageDialog(tempRef, "Failed saving the file.", "Error", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }//GEN-LAST:event_saveMenuItemActionPerformed

    private void exportMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportMenuItemActionPerformed
        final CsvExporter exporter = new CsvExporter(experiment, sample, replicateNumber);
        final JFileChooser fileChooser = new JFileChooser(lastSelectedFolder);
        fileChooser.setDialogTitle("Select Result Folder");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

        int returnVal = fileChooser.showDialog(this, "Save");

        if (returnVal == JFileChooser.APPROVE_OPTION) {

            lastSelectedFolder = fileChooser.getCurrentDirectory().getPath();

            // @TODO: add check for if a file is about to be overwritten

            cancelProgress = false;
            final PeptideShakerGUI tempRef = this; // needed due to threading issues

            progressDialog = new ProgressDialog(this, this, true);
            progressDialog.doNothingOnClose();

            new Thread(new Runnable() {

                public void run() {
                    progressDialog.setIntermidiate(true);
                    progressDialog.setTitle("Exporting. Please Wait...");
                    progressDialog.setVisible(true);
                }
            }, "ProgressDialog").start();

            new Thread("ExportThread") {

                @Override
                public void run() {
                    boolean exported = exporter.exportResults(fileChooser.getSelectedFile());
                    progressDialog.setVisible(false);
                    progressDialog.dispose();

                    if (exported) {
                        JOptionPane.showMessageDialog(tempRef, "Identifications were successfully exported.", "Export Successful", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(tempRef, "Writing of spectrum file failed.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.start();
        }
    }//GEN-LAST:event_exportMenuItemActionPerformed

    /**
     * Opens the help dialog.
     * 
     * @param evt
     */
    private void helpJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpJMenuItemActionPerformed
        new HelpWindow(this, getClass().getResource("/helpFiles/PeptideShaker.html"));
    }//GEN-LAST:event_helpJMenuItemActionPerformed

    /**
     * Opens the About dialog.
     *
     * @param evt
     */
    private void aboutJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutJMenuItemActionPerformed
        new HelpWindow(this, getClass().getResource("/helpFiles/AboutPeptideShaker.html"));
    }//GEN-LAST:event_aboutJMenuItemActionPerformed

    /**
     * This method sets the information of the project when opened
     * 
     * @param experiment        the experiment conducted
     * @param sample            The sample analyzed
     * @param replicateNumber   The replicate number
     */
    public void setProject(MsExperiment experiment, Sample sample, int replicateNumber) {
        this.experiment = experiment;
        this.sample = sample;
        this.replicateNumber = replicateNumber;
    }

    /**
     * Displays the results in the result tables.
     */
    public void displayResults() {

        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);

        PSMaps psMaps = (PSMaps) identification.getUrParam(new PSMaps());
        psmMap = psMaps.getPsmSpecificMap();
        peptideMap = psMaps.getPeptideSpecificMap();
        proteinMap = psMaps.getProteinMap();

        emptyResultTables();

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        // set the FDR threshold to use
        double threshold = 0.01;    // @TODO: this number ought to not be hardcoded!!!
        psmMap.getResults(threshold);
        peptideMap.getResults(threshold);
        proteinMap.getResults(threshold);

        int indexCounter = 0;
        int maxPeptides = 1;
        int maxSpectra = 1;

        // add the proteins to the table
        for (ProteinMatch proteinMatch : identification.getProteinIdentification().values()) {

            PSParameter probabilities = new PSParameter();
            probabilities = (PSParameter) proteinMatch.getUrParam(probabilities);

            ((DefaultTableModel) proteinsJTable.getModel()).addRow(new Object[]{
                        ++indexCounter,
                        proteinMatch.getTheoreticProtein().getAccession(),
                        proteinMatch.getPeptideMatches().size(),
                        proteinMatch.getSpectrumCount(),
                        probabilities.getProteinProbabilityScore(),
                        probabilities.getProteinProbability(),
                        proteinMatch.isDecoy()
                    });

            if (maxPeptides < proteinMatch.getPeptideMatches().size()) {
                maxPeptides = proteinMatch.getPeptideMatches().size();
            }

            if (maxSpectra < proteinMatch.getSpectrumCount()) {
                maxSpectra = proteinMatch.getSpectrumCount();
            }
        }

        ((JSparklinesBarChartTableCellRenderer) proteinsJTable.getColumn("#Peptides").getCellRenderer()).setMaxValue(maxPeptides);
        ((JSparklinesBarChartTableCellRenderer) proteinsJTable.getColumn("#Spectra").getCellRenderer()).setMaxValue(maxSpectra);


        indexCounter = 0;
        maxSpectra = 1;

        // add the peptides to the table
        for (PeptideMatch peptideMatch : identification.getPeptideIdentification().values()) {

            String accessionNumbers = "";

            for (Protein protein : peptideMatch.getTheoreticPeptide().getParentProteins()) {
                accessionNumbers += protein.getAccession() + ", ";
            }

            accessionNumbers = accessionNumbers.substring(0, accessionNumbers.length() - 2);

            String modifications = "";

            for (ModificationMatch mod : peptideMatch.getTheoreticPeptide().getModificationMatches()) {
                if (mod.isVariable()) {
                    modifications += mod.getTheoreticPtm().getName() + ", ";
                }
            }

            if (modifications.length() > 0) {
                modifications = modifications.substring(0, modifications.length() - 2);
            } else {
                modifications = null;
            }

            PSParameter probabilities = new PSParameter();
            probabilities = (PSParameter) peptideMatch.getUrParam(probabilities);


            ((DefaultTableModel) peptidesJTable.getModel()).addRow(new Object[]{
                        ++indexCounter,
                        accessionNumbers,
                        peptideMatch.getTheoreticPeptide().getSequence(),
                        modifications,
                        peptideMatch.getSpectrumMatches().size(),
                        probabilities.getPeptideProbabilityScore(),
                        probabilities.getPeptideProbability(),
                        peptideMatch.isDecoy()
                    });

            if (maxSpectra < peptideMatch.getSpectrumMatches().size()) {
                maxSpectra = peptideMatch.getSpectrumMatches().size();
            }
        }

        ((JSparklinesBarChartTableCellRenderer) peptidesJTable.getColumn("#Spectra").getCellRenderer()).setMaxValue(maxSpectra);


        indexCounter = 0;
        int maxCharge = 0;
        double maxMassError = 0;
        double maxMascotScore = 0.0;
        double maxMascotEValue = 0.0;
        double maxOmssaEValue = 0.0;
        double maxXTandemEValue = 0.0;
        double maxPScore = 1;
        double maxPEP = 1;

        // add the spectra to the table
        for (SpectrumMatch spectrumMatch : identification.getSpectrumIdentification().values()) {

            Peptide bestAssumption = spectrumMatch.getBestAssumption().getPeptide();

            String accessionNumbers = "";

            for (Protein protein : bestAssumption.getParentProteins()) {
                accessionNumbers += protein.getAccession() + ", ";
            }

            accessionNumbers = accessionNumbers.substring(0, accessionNumbers.length() - 2);

            String modifications = "";

            for (ModificationMatch mod : bestAssumption.getModificationMatches()) {
                if (mod.isVariable()) {
                    modifications += mod.getTheoreticPtm().getName() + ", ";
                }
            }

            if (modifications.length() > 0) {
                modifications = modifications.substring(0, modifications.length() - 2);
            } else {
                modifications = null;
            }

            String assumptions = "";

            for (PeptideAssumption assumption : spectrumMatch.getAllAssumptions()) {
                if (assumption.getPeptide().isSameAs(bestAssumption)) {
                    assumptions += assumption.getFile() + ", ";
                }
            }

            if (assumptions.length() > 0) {
                assumptions = assumptions.substring(0, assumptions.length() - 2);
            } else {
                assumptions = null;
            }

            Double mascotScore = null;

            PeptideAssumption assumption = spectrumMatch.getFirstHit(Advocate.MASCOT);
            if (assumption != null) {
                if (assumption.getPeptide().isSameAs(bestAssumption)) {
                    MascotScore score = (MascotScore) assumption.getUrParam(new MascotScore(0));
                    mascotScore = score.getScore();
                }
            }

            Double mascotEValue = null;

            if (assumption != null) {
                if (assumption.getPeptide().isSameAs(bestAssumption)) {
                    mascotEValue = assumption.getEValue();
                }
            }

            Double omssaEValue = null;

            assumption = spectrumMatch.getFirstHit(Advocate.OMSSA);
            if (assumption != null) {
                if (assumption.getPeptide().isSameAs(bestAssumption)) {
                    omssaEValue = assumption.getEValue();
                }
            }

            Double xTandemEValue = null;

            assumption = spectrumMatch.getFirstHit(Advocate.XTANDEM);
            if (assumption != null) {
                if (assumption.getPeptide().isSameAs(bestAssumption)) {
                    xTandemEValue = assumption.getEValue();
                }
            }

            PSParameter probabilities = new PSParameter();
            probabilities = (PSParameter) spectrumMatch.getUrParam(probabilities);

            ((DefaultTableModel) spectraJTable.getModel()).addRow(new Object[]{
                        ++indexCounter,
                        accessionNumbers,
                        bestAssumption.getSequence(),
                        modifications,
                        spectrumMatch.getSpectrum().getPrecursor().getCharge().value, // @TODO: check if this is correct
                        spectrumMatch.getSpectrum().getSpectrumTitle(),
                        spectrumMatch.getSpectrum().getFileName(),
                        assumptions,
                        spectrumMatch.getBestAssumption().getDeltaMass(),
                        mascotScore,
                        mascotEValue,
                        omssaEValue,
                        xTandemEValue,
                        probabilities.getSpectrumProbabilityScore(),
                        probabilities.getSpectrumProbability(),
                        spectrumMatch.getBestAssumption().isDecoy()
                    });

            if (maxCharge < spectrumMatch.getSpectrum().getPrecursor().getCharge().value) {
                maxCharge = spectrumMatch.getSpectrum().getPrecursor().getCharge().value;
            }

            if (maxMassError < spectrumMatch.getBestAssumption().getDeltaMass()) {
                maxMassError = spectrumMatch.getBestAssumption().getDeltaMass();
            }

            if (mascotScore != null && maxMascotScore < mascotScore) {
                maxMascotScore = mascotScore;
            }

            if (mascotEValue != null && maxMascotEValue < mascotEValue) {
                maxMascotEValue = mascotEValue;
            }

            if (omssaEValue != null && maxOmssaEValue < omssaEValue) {
                maxOmssaEValue = omssaEValue;
            }

            if (xTandemEValue != null && maxXTandemEValue < xTandemEValue) {
                maxXTandemEValue = xTandemEValue;
            }
        }

        ((JSparklinesBarChartTableCellRenderer) spectraJTable.getColumn("Charge").getCellRenderer()).setMaxValue(maxCharge);
        ((JSparklinesBarChartTableCellRenderer) spectraJTable.getColumn("Mass Error").getCellRenderer()).setMaxValue(maxMassError);
        ((JSparklinesBarChartTableCellRenderer) spectraJTable.getColumn("Mascot Score").getCellRenderer()).setMaxValue(maxMascotScore);
        ((JSparklinesBarChartTableCellRenderer) spectraJTable.getColumn("Mascot e-value").getCellRenderer()).setMaxValue(maxMascotEValue);
        ((JSparklinesBarChartTableCellRenderer) spectraJTable.getColumn("OMSSA e-value").getCellRenderer()).setMaxValue(maxOmssaEValue);
        ((JSparklinesBarChartTableCellRenderer) spectraJTable.getColumn("X!Tandem e-value").getCellRenderer()).setMaxValue(maxXTandemEValue);
        ((JSparklinesBarChartTableCellRenderer) spectraJTable.getColumn("p-score").getCellRenderer()).setMaxValue(maxPScore);
        ((JSparklinesBarChartTableCellRenderer) spectraJTable.getColumn("PEP").getCellRenderer()).setMaxValue(maxPEP);

        // enable the save and export menu items
        saveMenuItem.setEnabled(true);
        exportMenuItem.setEnabled(true);

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Clear the result tables.
     */
    private void emptyResultTables() {
        while (proteinsJTable.getRowCount() > 0) {
            ((DefaultTableModel) proteinsJTable.getModel()).removeRow(0);
        }

        while (peptidesJTable.getRowCount() > 0) {
            ((DefaultTableModel) peptidesJTable.getModel()).removeRow(0);
        }

        while (spectraJTable.getRowCount() > 0) {
            ((DefaultTableModel) spectraJTable.getModel()).removeRow(0);
        }

        while (quantificationJTable.getRowCount() > 0) {
            ((DefaultTableModel) quantificationJTable.getModel()).removeRow(0);
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutJMenuItem;
    private javax.swing.JMenuItem exitJMenuItem;
    private javax.swing.JMenuItem exportMenuItem;
    private javax.swing.JMenu fileJMenu;
    private javax.swing.JMenuItem helpJMenuItem;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem openJMenuItem;
    private javax.swing.JScrollPane peptidesJScrollPane;
    private javax.swing.JTable peptidesJTable;
    private javax.swing.JScrollPane proteinsJScrollPane;
    private javax.swing.JTable proteinsJTable;
    private javax.swing.JTable ptmAnalysisJTable;
    private javax.swing.JScrollPane ptmAnalysisScrollPane;
    private javax.swing.JScrollPane quantificationJScrollPane;
    private javax.swing.JTable quantificationJTable;
    private javax.swing.JTabbedPane resultsJTabbedPane;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JCheckBoxMenuItem sparklinesJCheckBoxMenuItem;
    private javax.swing.JScrollPane spectraJScrollPane;
    private javax.swing.JTable spectraJTable;
    private javax.swing.JMenu viewJMenu;
    // End of variables declaration//GEN-END:variables

    /**
     * Check if a newer version of reporter is available.
     *
     * @param currentVersion the version number of the currently running reporter
     */
    private static void checkForNewVersion(String currentVersion) {

        try {
            boolean deprecatedOrDeleted = false;
            URL downloadPage = new URL(
                    "http://code.google.com/p/peptide-shaker/downloads/detail?name=PeptideShaker-"
                    + currentVersion + ".zip");
            int respons = ((java.net.HttpURLConnection) downloadPage.openConnection()).getResponseCode();

            // 404 means that the file no longer exists, which means that
            // the running version is no longer available for download,
            // which again means that a never version is available.
            if (respons == 404) {
                deprecatedOrDeleted = true;
            } else {

                // also need to check if the available running version has been
                // deprecated (but not deleted)
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(downloadPage.openStream()));

                String inputLine;

                while ((inputLine = in.readLine()) != null && !deprecatedOrDeleted) {
                    if (inputLine.lastIndexOf("Deprecated") != -1
                            && inputLine.lastIndexOf("Deprecated Downloads") == -1
                            && inputLine.lastIndexOf("Deprecated downloads") == -1) {
                        deprecatedOrDeleted = true;
                    }
                }

                in.close();
            }

            // informs the user about an updated version of the tool, unless the user
            // is running a beta version
            if (deprecatedOrDeleted && currentVersion.lastIndexOf("beta") == -1) {
                int option = JOptionPane.showConfirmDialog(null,
                        "A newer version of PeptideShaker is available.\n"
                        + "Do you want to upgrade?",
                        "Upgrade Available",
                        JOptionPane.YES_NO_CANCEL_OPTION);
                if (option == JOptionPane.YES_OPTION) {
                    BareBonesBrowserLaunch.openURL("http://peptide-shaker.googlecode.com/");
                    System.exit(0);
                } else if (option == JOptionPane.CANCEL_OPTION) {
                    System.exit(0);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set up the log file.
     */
    private void setUpLogFile() {
        if (useLogFile && !getJarFilePath().equalsIgnoreCase(".")) {
            try {
                String path = getJarFilePath() + "/conf/PeptideShakerLog.log";

                File file = new File(path);
                System.setOut(new java.io.PrintStream(new FileOutputStream(file, true)));
                System.setErr(new java.io.PrintStream(new FileOutputStream(file, true)));

                // creates a new log file if it does not exist
                if (!file.exists()) {
                    file.createNewFile();

                    FileWriter w = new FileWriter(file);
                    BufferedWriter bw = new BufferedWriter(w);

                    bw.close();
                    w.close();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        null, "An error occured when trying to create the PeptideShaker Log.",
                        "Error Creating Log File", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the path to the jar file.
     *
     * @return the path to the jar file
     */
    private String getJarFilePath() {
        String path = this.getClass().getResource("PeptideShakerGUI.class").getPath();

        if (path.lastIndexOf("/PeptideShaker-") != -1) {
            path = path.substring(5, path.lastIndexOf("/PeptideShaker-"));
            path = path.replace("%20", " ");
        } else {
            path = ".";
        }

        return path;
    }

    /**
     * Sets the look and feel of the PeptideShaker.
     * <p/>
     * Note that the GUI has been created with the following look and feel
     * in mind. If using a different look and feel you might need to tweak the GUI
     * to get the best appearance.
     */
    private static void setLookAndFeel() {

        try {
            PlasticLookAndFeel.setPlasticTheme(new SkyKrupp());
            UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            // ignore exception, i.e. use default look and feel
        }
    }

    @Override
    public void cancelProgress() {
        cancelProgress = true;
    }
}
