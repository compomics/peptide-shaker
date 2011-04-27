package eu.isas.peptideshaker.gui;

import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.SequenceDataBase;
import com.compomics.util.experiment.io.ExperimentIO;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumCollection;
import com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel;
import eu.isas.peptideshaker.export.CsvExporter;
import eu.isas.peptideshaker.gui.preferencesdialogs.AnnotationPreferencesDialog;
import eu.isas.peptideshaker.gui.preferencesdialogs.SearchPreferencesDialog;
import eu.isas.peptideshaker.gui.tabpanels.OverviewPanel;
import eu.isas.peptideshaker.gui.tabpanels.PtmPanel;
import eu.isas.peptideshaker.gui.tabpanels.StatsPanel;
import eu.isas.peptideshaker.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.preferences.IdentificationPreferences;
import eu.isas.peptideshaker.preferences.SearchParameters;
import java.awt.Color;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * The main frame of the PeptideShaker.
 *
 * @author  Harald Barsnes
 * @author  Marc Vaudel
 */
public class PeptideShakerGUI extends javax.swing.JFrame implements ProgressDialogParent {

    /**
     * Turns of the gradient painting for the bar charts.
     */
    static {
        XYBarRenderer.setDefaultBarPainter(new StandardXYBarPainter());
    }
    /**
     * If set to true all messages will be sent to a log file.
     */
    private static boolean useLogFile = true;
    /**
     * The last folder opened by the user. Defaults to user.home.
     */
    private String lastSelectedFolder = "user.home";
    /**
     * The xml file containing the enzymes
     */
    private static final String ENZYME_FILE = "conf/peptideshaker_enzymes.xml";
    /**
     * modification file
     */
    private final String MODIFICATIONS_FILE = "conf/peptideshaker_mods.xml";
    /**
     * user modification file
     */
    private final String USER_MODIFICATIONS_FILE = "conf/peptideshaker_usermods.xml";
    /**
     * The compomics PTM factory
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The compomics experiment
     */
    private MsExperiment experiment = null;
    /**
     * The investigated sample
     */
    private Sample sample;
    /**
     * The replicate number
     */
    private int replicateNumber;
    /**
     * The identification preferences
     */
    private IdentificationPreferences identificationPreferences;
    /**
     * The annotation preferences
     */
    private AnnotationPreferences annotationPreferences = new AnnotationPreferences();
    /**
     * The parameters of the search
     */
    private SearchParameters searchParameters = new SearchParameters();
    /**
     * The color used for the sparkline bar chart plots.
     */
    private Color sparklineColor = new Color(110, 196, 97);
    /**
     * Compomics experiment saver and opener
     */
    private ExperimentIO experimentIO = new ExperimentIO();
    /**
     * A simple progress dialog.
     */
    private static ProgressDialog progressDialog;
    /**
     * The identification to display
     */
    private Identification identification;
    /**
     * The overview panel
     */
    private OverviewPanel overviewPanel = new OverviewPanel(this);
    /**
     * The statistics panel
     */
    private StatsPanel statsPanel = new StatsPanel(this);
    /**
     * The PTM panel
     */
    private PtmPanel ptmPanel = new PtmPanel(this);
    /**
     * The sequence database used for identification
     */
    private SequenceDataBase sequenceDataBase;
    /**
     * The spectrum collection loaded
     */
    private SpectrumCollection spectrumCollection;

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

        // set the title
        this.setTitle(this.getTitle() + " - v" + getVersion() + " beta");

        // set the title of the frame and add the icon
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

        this.setExtendedState(MAXIMIZED_BOTH);

        setLocationRelativeTo(null);
        setVisible(true);

        loadEnzymes();
        loadModifications();
        setDefaultPreferences();

        overviewJPanel.removeAll();
        overviewJPanel.add(overviewPanel);
        overviewJPanel.revalidate();
        overviewJPanel.repaint();

        statsJPanel.removeAll();
        statsJPanel.add(statsPanel);
        statsJPanel.revalidate();
        statsJPanel.repaint();

        ptmJPanel.removeAll();
        ptmJPanel.add(ptmPanel);
        ptmJPanel.revalidate();
        ptmJPanel.repaint();

        // open the OpenDialog
        new OpenDialog(this, true);
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

    /**
     * Retrieves the version number set in the pom file.
     *
     * @return the version number of PeptideShaker
     */
    public String getVersion() {

        java.util.Properties p = new java.util.Properties();

        try {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("peptide-shaker.properties");
            p.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return p.getProperty("peptide-shaker.version");
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        gradientPanel = new javax.swing.JPanel();
        resultsJTabbedPane = new javax.swing.JTabbedPane();
        overviewJPanel = new javax.swing.JPanel();
        statsJPanel = new javax.swing.JPanel();
        ptmJPanel = new javax.swing.JPanel();
        menuBar = new javax.swing.JMenuBar();
        fileJMenu = new javax.swing.JMenu();
        openJMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        saveMenuItem = new javax.swing.JMenuItem();
        exportMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        exitJMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        searchParametersMenu = new javax.swing.JMenuItem();
        annotationPreferencesMenu = new javax.swing.JMenuItem();
        viewJMenu = new javax.swing.JMenu();
        sequenceFragmentationJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        sequenceCoverageJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        sparklinesJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        helpMenu = new javax.swing.JMenu();
        helpJMenuItem = new javax.swing.JMenuItem();
        aboutJMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("PeptideShaker");
        setBackground(new java.awt.Color(255, 255, 255));
        setMinimumSize(new java.awt.Dimension(1260, 800));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        gradientPanel.setBackground(new java.awt.Color(255, 255, 255));

        resultsJTabbedPane.setTabPlacement(javax.swing.JTabbedPane.RIGHT);

        overviewJPanel.setOpaque(false);
        overviewJPanel.setPreferredSize(new java.awt.Dimension(900, 800));
        overviewJPanel.setLayout(new javax.swing.BoxLayout(overviewJPanel, javax.swing.BoxLayout.LINE_AXIS));
        resultsJTabbedPane.addTab("Overview", overviewJPanel);

        statsJPanel.setLayout(new javax.swing.BoxLayout(statsJPanel, javax.swing.BoxLayout.LINE_AXIS));
        resultsJTabbedPane.addTab("Statistics", statsJPanel);

        ptmJPanel.setLayout(new javax.swing.BoxLayout(ptmJPanel, javax.swing.BoxLayout.LINE_AXIS));
        resultsJTabbedPane.addTab("PTMs", ptmJPanel);

        javax.swing.GroupLayout gradientPanelLayout = new javax.swing.GroupLayout(gradientPanel);
        gradientPanel.setLayout(gradientPanelLayout);
        gradientPanelLayout.setHorizontalGroup(
            gradientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1044, Short.MAX_VALUE)
            .addGroup(gradientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(resultsJTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1044, Short.MAX_VALUE))
        );
        gradientPanelLayout.setVerticalGroup(
            gradientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 747, Short.MAX_VALUE)
            .addGroup(gradientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(resultsJTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 747, Short.MAX_VALUE))
        );

        menuBar.setBackground(new java.awt.Color(255, 255, 255));

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
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });
        fileJMenu.add(saveMenuItem);

        exportMenuItem.setMnemonic('E');
        exportMenuItem.setText("Export");
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

        editMenu.setMnemonic('E');
        editMenu.setText("Edit");

        searchParametersMenu.setText("Search Parameters");
        searchParametersMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchParametersMenuActionPerformed(evt);
            }
        });
        editMenu.add(searchParametersMenu);

        annotationPreferencesMenu.setText("Spectrum Annotations");
        annotationPreferencesMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                annotationPreferencesMenuActionPerformed(evt);
            }
        });
        editMenu.add(annotationPreferencesMenu);

        menuBar.add(editMenu);

        viewJMenu.setMnemonic('V');
        viewJMenu.setText("View");

        sequenceFragmentationJCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        sequenceFragmentationJCheckBoxMenuItem.setMnemonic('S');
        sequenceFragmentationJCheckBoxMenuItem.setSelected(true);
        sequenceFragmentationJCheckBoxMenuItem.setText("Spectrum");
        viewJMenu.add(sequenceFragmentationJCheckBoxMenuItem);

        sequenceCoverageJCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        sequenceCoverageJCheckBoxMenuItem.setMnemonic('C');
        sequenceCoverageJCheckBoxMenuItem.setSelected(true);
        sequenceCoverageJCheckBoxMenuItem.setText("Sequence Coverage");
        sequenceCoverageJCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sequenceCoverageJCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewJMenu.add(sequenceCoverageJCheckBoxMenuItem);
        viewJMenu.add(jSeparator3);

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
            .addComponent(gradientPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(gradientPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Opens a dialog where the identification files to analyzed are selected.
     *
     * @param evt
     */
    private void openJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openJMenuItemActionPerformed

        // @TODO: the code below did not work and had to be removed. but should be fixed and put back in?

        //        if (experiment != null) {
//            new OpenDialog(this, true, experiment, sample, replicateNumber);
//        } else {
        new OpenDialog(this, true);
//        }
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
        overviewPanel.showSparkLines(!sparklinesJCheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_sparklinesJCheckBoxMenuItemActionPerformed

    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed

        final JFileChooser fileChooser = new JFileChooser(lastSelectedFolder);
        fileChooser.setDialogTitle("Save As...");
        fileChooser.setMultiSelectionEnabled(false);

        int returnVal = fileChooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {

            lastSelectedFolder = fileChooser.getCurrentDirectory().getPath();

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

                    File newFile = new File(selectedFile);
                    int outcome = JOptionPane.YES_OPTION;
                    if (newFile.exists()) {
                        outcome = JOptionPane.showConfirmDialog(progressDialog,
                                "Should " + selectedFile + " be overwritten?", "Selected File Already Exists",
                                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    }
                    if (outcome != JOptionPane.YES_OPTION) {
                        progressDialog.setVisible(false);
                        progressDialog.dispose();
                        return;
                    }

                    try {
                        experimentIO.save(newFile, experiment);

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
        final CsvExporter exporter = new CsvExporter(experiment, sample, replicateNumber, searchParameters.getEnzyme());
        final JFileChooser fileChooser = new JFileChooser(lastSelectedFolder);
        fileChooser.setDialogTitle("Select Result Folder");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

        int returnVal = fileChooser.showDialog(this, "Save");

        if (returnVal == JFileChooser.APPROVE_OPTION) {

            lastSelectedFolder = fileChooser.getCurrentDirectory().getPath();

            // @TODO: add check for if a file is about to be overwritten

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
     * Opens the Identification Preference dialog.
     *
     * @param evt
     */
    private void annotationPreferencesMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_annotationPreferencesMenuActionPerformed
        new AnnotationPreferencesDialog(this);
    }//GEN-LAST:event_annotationPreferencesMenuActionPerformed

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        overviewPanel.udateSeparators();
    }//GEN-LAST:event_formComponentResized

    private void sequenceCoverageJCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sequenceCoverageJCheckBoxMenuItemActionPerformed
        overviewPanel.setDisplayOptions(sequenceCoverageJCheckBoxMenuItem.isSelected(), sequenceFragmentationJCheckBoxMenuItem.isSelected());
        overviewPanel.udateSeparators();
}//GEN-LAST:event_sequenceCoverageJCheckBoxMenuItemActionPerformed

    private void sequenceFragmentationJCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sequenceFragmentationJCheckBoxMenuItemActionPerformed
        overviewPanel.setDisplayOptions(sequenceCoverageJCheckBoxMenuItem.isSelected(), sequenceFragmentationJCheckBoxMenuItem.isSelected());
        overviewPanel.udateSeparators();
    }//GEN-LAST:event_sequenceFragmentationJCheckBoxMenuItemActionPerformed

    private void searchParametersMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchParametersMenuActionPerformed
        new SearchPreferencesDialog(this);
    }//GEN-LAST:event_searchParametersMenuActionPerformed

    /**
     * Loads the enzymes from the enzyme file into the enzyme factory
     */
    private void loadEnzymes() {
        try {
            EnzymeFactory enzymeFactory = EnzymeFactory.getInstance();
            enzymeFactory.importEnzymes(new File(ENZYME_FILE));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Impossible to load enzyme file.", "Wrong enzyme file.", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * This method will display results in all panels
     */
    public void displayResults() {
        try {
            boolean displaySpectrum = true;
            boolean displaySequence = true;
            String tempSpectrumKey = spectrumCollection.getAllKeys(2).get(0);
            Spectrum tempSpectrum = spectrumCollection.getSpectrum(2, tempSpectrumKey);
            if (tempSpectrum.getPeakList() == null || tempSpectrum.getPeakList().isEmpty()) {
                displaySpectrum = false;
            }
            if (sequenceDataBase == null) {
                displaySequence = false;
            }
            sequenceCoverageJCheckBoxMenuItem.setSelected(displaySequence);
            sequenceFragmentationJCheckBoxMenuItem.setSelected(displaySpectrum);
            overviewPanel.setDisplayOptions(displaySequence, displaySpectrum);
            overviewPanel.udateSeparators();

            overviewPanel.displayResults();
            ptmPanel.displayResults();
            statsPanel.displayResults();


        } catch (MzMLUnmarshallerException e) {
            JOptionPane.showMessageDialog(this, "A problem occured while reading the mzML file.", "mzML problem", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "A problem occured while displaying results. Please send the log file to the developers.", "Display problem", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

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
        ProteomicAnalysis proteomicAnalysis = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber);
        identification = proteomicAnalysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        sequenceDataBase = proteomicAnalysis.getSequenceDataBase();
        spectrumCollection = proteomicAnalysis.getSpectrumCollection();
    }

    /**
     * Sets new identification preferences
     * @param identificationPreferences the new identification preferences
     */
    public void setIdentificationPreferences(IdentificationPreferences identificationPreferences) {
        this.identificationPreferences = identificationPreferences;
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutJMenuItem;
    private javax.swing.JMenuItem annotationPreferencesMenu;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem exitJMenuItem;
    private javax.swing.JMenuItem exportMenuItem;
    private javax.swing.JMenu fileJMenu;
    private javax.swing.JPanel gradientPanel;
    private javax.swing.JMenuItem helpJMenuItem;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem openJMenuItem;
    private javax.swing.JPanel overviewJPanel;
    private javax.swing.JPanel ptmJPanel;
    private javax.swing.JTabbedPane resultsJTabbedPane;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JMenuItem searchParametersMenu;
    private javax.swing.JCheckBoxMenuItem sequenceCoverageJCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem sequenceFragmentationJCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem sparklinesJCheckBoxMenuItem;
    private javax.swing.JPanel statsJPanel;
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
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            // ignore exception, i.e. use default look and feel
        }
    }

    /**
     * Set the default preferences.
     * TODO: Not sure that this ought to be hard coded
     */
    private void setDefaultPreferences() {
        identificationPreferences = new IdentificationPreferences(0.01, 0.01, 0.01, false, false);
        searchParameters = new SearchParameters();
        searchParameters.setEnzyme(EnzymeFactory.getInstance().getEnzyme("Trypsin"));
        searchParameters.setFragmentIonMZTolerance(0.5);
    }

    /**
     * @return the experiment
     */
    public MsExperiment getExperiment() {
        return experiment;
    }

    /**
     * @return the sample
     */
    public Sample getSample() {
        return sample;
    }

    /**
     * @return the replicateNumber
     */
    public int getReplicateNumber() {
        return replicateNumber;
    }

    /**
     * returns the identification displayed
     * @return  the identification displayed
     */
    public Identification getIdentification() {
        return identification;
    }

    /**
     * Returns the sequence database used for identification
     * @return the sequence database used for identification
     */
    public SequenceDataBase getSequenceDataBase() {
        return sequenceDataBase;
    }

    /**
     * Returns the spectrum collection loaded
     * @return the spectrum collection loaded
     */
    public SpectrumCollection getSpectrumCollection() {
        return spectrumCollection;
    }

    /**
     * Returns the annotation preferences as set by the user
     * @return the annotation preferences as set by the user
     */
    public AnnotationPreferences getAnnotationPreferences() {
        return annotationPreferences;
    }

    /**
     * Returns the displayed proteomicAnalysis
     * @return the displayed proteomicAnalysis
     */
    public ProteomicAnalysis getProteomicanalysis() {
        return experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber);
    }

    /**
     * Returns the search parameters
     * @return the search parameters
     */
    public SearchParameters getSearchParameters() {
        return searchParameters;
    }

    /**
     * updates the search parameters
     * @param searchParameters  the new search parameters
     */
    public void setSearchParameters(SearchParameters searchParameters) {
        this.searchParameters = searchParameters;
        statsPanel.estimatePossibilities();
    }

    /**
     * Updates the new annotation preferences
     * @param annotationPreferences the new annotation preferences
     */
    public void setAnnotationPreferences(AnnotationPreferences annotationPreferences) {
        this.annotationPreferences = annotationPreferences;
    }

    /**
     * Loads the modifications from the modification file
     */
    private void loadModifications() {
        try {
            ptmFactory.importModifications(new File(MODIFICATIONS_FILE));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "An error (" + e.getMessage() + ") occured when trying to load the modifications from " + MODIFICATIONS_FILE + ".",
                    "Configuration import Error", JOptionPane.ERROR_MESSAGE);
        }
        try {
            ptmFactory.importModifications(new File(USER_MODIFICATIONS_FILE));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "An error (" + e.getMessage() + ") occured when trying to load the modifications from " + USER_MODIFICATIONS_FILE + ".",
                    "Configuration import Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void cancelProgress() {
        // @TODO not sure how to implement this
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
