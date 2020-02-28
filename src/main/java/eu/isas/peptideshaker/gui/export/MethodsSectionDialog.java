package eu.isas.peptideshaker.gui.export;

import com.compomics.util.gui.file_handling.FileAndFileFilter;
import com.compomics.util.experiment.io.biology.protein.FastaParameters;
import com.compomics.util.experiment.io.biology.protein.FastaSummary;
import com.compomics.util.gui.file_handling.FileChooserUtil;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.io.export.ExportFormat;
import com.compomics.util.io.export.ExportScheme;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.search.SearchParameters;
import eu.isas.peptideshaker.export.PeptideShakerMethods;
import eu.isas.peptideshaker.export.PSExportFactory;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * A dialog for drafting the methods section for a publication based on
 * PeptideShaker results.
 *
 * @author Harald Barsnes
 * @author Marc Vaudel
 */
public class MethodsSectionDialog extends javax.swing.JDialog {

    /**
     * The main PeptideShaker frame.
     */
    private final PeptideShakerGUI peptideShakerGUI;
    /**
     * A simple progress dialog.
     */
    private static ProgressDialogX progressDialog;

    /**
     * Creates a new MethodsSectionDialog.
     *
     * @param peptideShakerGUI the main frame
     * @param modal if the dialog is to be modal or not
     */
    public MethodsSectionDialog(PeptideShakerGUI peptideShakerGUI, boolean modal) {
        super(peptideShakerGUI, modal);
        initComponents();
        this.peptideShakerGUI = peptideShakerGUI;
        setLocationRelativeTo(peptideShakerGUI);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                updateMethodsSection();

            }
        });

        setVisible(true);
    }

    /**
     * Generates the methods section according to the current user selections
     * and shows it in the GUI.
     */
    private void updateMethodsSection() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
        waitingLabel.setVisible(true);
        IdentificationParameters identificationParameters = peptideShakerGUI.getIdentificationParameters();
        SearchParameters searchParameters = peptideShakerGUI.getIdentificationParameters().getSearchParameters();

        String text = "";
        String tab = "          ";

        text += "Protein Identification\n\n";

        if (algorithmsCheck.isSelected() || searchGUICheck.isSelected()) {
            text += tab;
        }

        if (algorithmsCheck.isSelected()) {
            text += PeptideShakerMethods.getSearchEnginesText(peptideShakerGUI.getProjectDetails());
        }
        if (searchGUICheck.isSelected()) {
            text += PeptideShakerMethods.getSearchGUIText();
        }

        if (algorithmsCheck.isSelected() || searchGUICheck.isSelected()) {
            text += System.lineSeparator() + tab;
        }

        if (proteinDbCkeck.isSelected()) {

            FastaParameters fastaParameters = identificationParameters.getFastaParameters();
            FastaSummary fastaSummary;

            try {

                fastaSummary = FastaSummary.getSummary(peptideShakerGUI.getProjectDetails().getFastaFile(), fastaParameters, progressDialog);

            } catch (IOException e) {

                // Skip the database details
                fastaSummary = null;

            }
            text += PeptideShakerMethods.getDatabaseText(fastaParameters, fastaSummary);

        }
        if (decoyCheck.isSelected()) {
            text += PeptideShakerMethods.getDecoyType();
        }
        if (idParametersCheck.isSelected()) {
            text += PeptideShakerMethods.getIdentificationSettings(searchParameters);
        }

        if (proteinDbCkeck.isSelected() || decoyCheck.isSelected() || idParametersCheck.isSelected()) {
            text += System.lineSeparator() + tab;
        }

        if (peptideShakerCheck.isSelected()) {
            text += PeptideShakerMethods.getPeptideShaker();
        }
        if (validationCheck.isSelected()) {
            text += PeptideShakerMethods.getValidation(peptideShakerGUI.getIdentificationParameters().getIdValidationParameters());
        }
        if (ptmLocalizationCheck.isSelected()) {
            text += PeptideShakerMethods.getPtmScoring(identificationParameters.getModificationLocalizationParameters());
        }

        if (peptideShakerCheck.isSelected() || validationCheck.isSelected() || ptmLocalizationCheck.isSelected()) {
            text += System.lineSeparator() + tab;
        }

        if (geneAnnotationCheck.isSelected()) {
            text += PeptideShakerMethods.getGeneAnnotation();
        }
        if (proteinAbundanceIndexesCheck.isSelected()) {
            text += PeptideShakerMethods.getSpectrumCounting(peptideShakerGUI.getSpectrumCountingParameters());
        }

        if (geneAnnotationCheck.isSelected() || proteinAbundanceIndexesCheck.isSelected()) {
            text += System.lineSeparator() + tab;
        }

        if (pxCheck.isSelected()) {
            text += PeptideShakerMethods.getProteomeXchange();
        }

        outputTextArea.setText(text);
        outputTextArea.setCaretPosition(0);

        waitingLabel.setVisible(false);
        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Lets the user select a file where to write the certificate of analysis.
     */
    private void writeCoa() {

        String textFileFilterDescription = "Tab separated text file (.txt)";
        String excelFileFilterDescription = "Excel Workbook (.xls)";
        String lastSelectedFolderPath = peptideShakerGUI.getLastSelectedFolder().getLastSelectedFolder();
        FileAndFileFilter selectedFileAndFilter = FileChooserUtil.getUserSelectedFile(
                this,
                new String[]{".txt", ".xls"},
                new String[]{textFileFilterDescription, excelFileFilterDescription},
                "Export Report",
                lastSelectedFolderPath,
                "certificate_of_analysis",
                false,
                true,
                false,
                0
        );

        if (selectedFileAndFilter != null) {

            final File selectedFile = selectedFileAndFilter.getFile();
            final ExportFormat exportFormat;
            if (selectedFileAndFilter.getFileFilter().getDescription().equalsIgnoreCase(textFileFilterDescription)) {
                exportFormat = ExportFormat.text;
            } else {
                exportFormat = ExportFormat.excel;
            }

            progressDialog = new ProgressDialogX(
                    this,
                    peptideShakerGUI,
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                    true
            );
            progressDialog.setTitle("Exporting Report. Please Wait...");

            final String filePath = selectedFile.getPath();

            new Thread(new Runnable() {
                public void run() {
                    try {
                        progressDialog.setVisible(true);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore
                    }
                }
            }, "ProgressDialog").start();

            new Thread("ExportThread") {
                @Override
                public void run() {

                    try {
                        String schemeName = "Certificate of Analysis"; //TODO: get this from the PSExportFactory
                        ExportScheme exportScheme = PSExportFactory.getInstance().getExportScheme(schemeName);
                        progressDialog.setTitle("Exporting. Please Wait...");
                        PSExportFactory.writeExport(
                                exportScheme,
                                selectedFile,
                                exportFormat,
                                false,
                                peptideShakerGUI.getProjectParameters().getProjectUniqueName(),
                                peptideShakerGUI.getProjectDetails(),
                                peptideShakerGUI.getIdentification(),
                                peptideShakerGUI.getIdentificationFeaturesGenerator(),
                                peptideShakerGUI.getGeneMaps(),
                                null,
                                null,
                                null,
                                peptideShakerGUI.getDisplayParameters().getnAASurroundingPeptides(),
                                peptideShakerGUI.getIdentificationParameters(),
                                peptideShakerGUI.getSequenceProvider(),
                                peptideShakerGUI.getProteinDetailsProvider(),
                                peptideShakerGUI.getSpectrumProvider(),
                                peptideShakerGUI.getSpectrumCountingParameters(),
                                progressDialog
                        );

                        boolean processCancelled = progressDialog.isRunCanceled();
                        progressDialog.setRunFinished();

                        if (!processCancelled) {
                            JOptionPane.showMessageDialog(
                                    peptideShakerGUI,
                                    "Data copied to file:\n" + filePath, "Data Exported.",
                                    JOptionPane.INFORMATION_MESSAGE
                            );
                        }
                    } catch (FileNotFoundException e) {

                        progressDialog.setRunFinished();
                        JOptionPane.showMessageDialog(
                                peptideShakerGUI,
                                "An error occurred while generating the output. Please make sure "
                                + "that the destination file is not opened by another application.",
                                "Output Error.",
                                JOptionPane.ERROR_MESSAGE
                        );
                        e.printStackTrace();
                    } catch (Exception e) {
                        progressDialog.setRunFinished();
                        JOptionPane.showMessageDialog(
                                peptideShakerGUI,
                                "An error occurred while generating the output.",
                                "Output Error.",
                                JOptionPane.ERROR_MESSAGE
                        );
                        e.printStackTrace();
                    }
                }
            }.start();
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

        backgroundPanel = new javax.swing.JPanel();
        introductionPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        introductionTxt = new javax.swing.JTextArea();
        featuresPanel = new javax.swing.JPanel();
        algorithmsCheck = new javax.swing.JCheckBox();
        searchGUICheck = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        proteinDbCkeck = new javax.swing.JCheckBox();
        decoyCheck = new javax.swing.JCheckBox();
        idParametersCheck = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        peptideShakerCheck = new javax.swing.JCheckBox();
        validationCheck = new javax.swing.JCheckBox();
        ptmLocalizationCheck = new javax.swing.JCheckBox();
        geneAnnotationCheck = new javax.swing.JCheckBox();
        proteinAbundanceIndexesCheck = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        pxCheck = new javax.swing.JCheckBox();
        outputPanel = new javax.swing.JPanel();
        exportCoaLbl = new javax.swing.JLabel();
        copyLbl = new javax.swing.JLabel();
        outputAreadScrollPane = new javax.swing.JScrollPane();
        outputTextArea = new javax.swing.JTextArea();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        waitingLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Methods Section Editor");
        setMinimumSize(new java.awt.Dimension(900, 700));

        backgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        introductionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Information"));
        introductionPanel.setOpaque(false);

        jScrollPane1.setBorder(null);
        jScrollPane1.setEnabled(false);
        jScrollPane1.setOpaque(false);

        introductionTxt.setEditable(false);
        introductionTxt.setColumns(20);
        introductionTxt.setFont(new java.awt.Font("Tahoma", 0, 13)); // NOI18N
        introductionTxt.setLineWrap(true);
        introductionTxt.setRows(5);
        introductionTxt.setText("Method Section Editor automatically drafts a methods section for protein identification with SearchGUI and PeptideShaker.\n\n1 - Select the wanted features in the left panel.\n2 - Copy the output to a text editor.\n3 - Complete the sections in brackets. (PubMed IDs are indicated for references, paste into PubMed to retrieve the complete reference)\n4 - Export the Certificate of Analysis and add it to your supplementary material and to the files uploaded to ProteomeXchange.\n\nNote: The section editor does not include the raw file to peak list conversion.");
        introductionTxt.setWrapStyleWord(true);
        introductionTxt.setMargin(new java.awt.Insets(10, 10, 10, 10));
        jScrollPane1.setViewportView(introductionTxt);

        javax.swing.GroupLayout introductionPanelLayout = new javax.swing.GroupLayout(introductionPanel);
        introductionPanel.setLayout(introductionPanelLayout);
        introductionPanelLayout.setHorizontalGroup(
            introductionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(introductionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 864, Short.MAX_VALUE)
                .addContainerGap())
        );
        introductionPanelLayout.setVerticalGroup(
            introductionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(introductionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 165, Short.MAX_VALUE)
                .addContainerGap())
        );

        featuresPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Features"));
        featuresPanel.setOpaque(false);
        featuresPanel.setPreferredSize(new java.awt.Dimension(500, 523));

        algorithmsCheck.setSelected(true);
        algorithmsCheck.setText("Identification Algorithms");
        algorithmsCheck.setIconTextGap(15);
        algorithmsCheck.setOpaque(false);
        algorithmsCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                algorithmsCheckActionPerformed(evt);
            }
        });

        searchGUICheck.setSelected(true);
        searchGUICheck.setText("SearchGUI");
        searchGUICheck.setIconTextGap(15);
        searchGUICheck.setOpaque(false);
        searchGUICheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchGUICheckActionPerformed(evt);
            }
        });

        jLabel1.setText("Spectrum Identification Algorithms");

        jLabel2.setText("Spectrum Identification Settings");

        proteinDbCkeck.setSelected(true);
        proteinDbCkeck.setText("Protein Database");
        proteinDbCkeck.setIconTextGap(15);
        proteinDbCkeck.setOpaque(false);
        proteinDbCkeck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinDbCkeckActionPerformed(evt);
            }
        });

        decoyCheck.setSelected(true);
        decoyCheck.setText("Decoy Sequences Generation");
        decoyCheck.setIconTextGap(15);
        decoyCheck.setOpaque(false);
        decoyCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                decoyCheckActionPerformed(evt);
            }
        });

        idParametersCheck.setSelected(true);
        idParametersCheck.setText("Identification Parameters");
        idParametersCheck.setIconTextGap(15);
        idParametersCheck.setOpaque(false);
        idParametersCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                idParametersCheckActionPerformed(evt);
            }
        });

        jLabel3.setText("Peptide and Protein Identification");

        peptideShakerCheck.setSelected(true);
        peptideShakerCheck.setText("PeptideShaker");
        peptideShakerCheck.setIconTextGap(15);
        peptideShakerCheck.setOpaque(false);
        peptideShakerCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptideShakerCheckActionPerformed(evt);
            }
        });

        validationCheck.setSelected(true);
        validationCheck.setText("Statistical Validation");
        validationCheck.setIconTextGap(15);
        validationCheck.setOpaque(false);
        validationCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                validationCheckActionPerformed(evt);
            }
        });

        ptmLocalizationCheck.setSelected(true);
        ptmLocalizationCheck.setText("PTM Localization");
        ptmLocalizationCheck.setIconTextGap(15);
        ptmLocalizationCheck.setOpaque(false);
        ptmLocalizationCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ptmLocalizationCheckActionPerformed(evt);
            }
        });

        geneAnnotationCheck.setText("Gene Annotation");
        geneAnnotationCheck.setEnabled(false);
        geneAnnotationCheck.setIconTextGap(15);
        geneAnnotationCheck.setOpaque(false);
        geneAnnotationCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                geneAnnotationCheckActionPerformed(evt);
            }
        });

        proteinAbundanceIndexesCheck.setText("Protein Abundance Index");
        proteinAbundanceIndexesCheck.setIconTextGap(15);
        proteinAbundanceIndexesCheck.setOpaque(false);
        proteinAbundanceIndexesCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinAbundanceIndexesCheckActionPerformed(evt);
            }
        });

        jLabel4.setText("Additional Features");

        jLabel5.setText("Identification Repository");

        pxCheck.setSelected(true);
        pxCheck.setText("ProteomeXchange");
        pxCheck.setIconTextGap(15);
        pxCheck.setOpaque(false);
        pxCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pxCheckActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout featuresPanelLayout = new javax.swing.GroupLayout(featuresPanel);
        featuresPanel.setLayout(featuresPanelLayout);
        featuresPanelLayout.setHorizontalGroup(
            featuresPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(featuresPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(featuresPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addGroup(featuresPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(featuresPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(peptideShakerCheck)
                            .addComponent(idParametersCheck)
                            .addComponent(validationCheck)
                            .addComponent(ptmLocalizationCheck)
                            .addGroup(featuresPanelLayout.createSequentialGroup()
                                .addGroup(featuresPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(proteinDbCkeck)
                                    .addComponent(algorithmsCheck)
                                    .addComponent(searchGUICheck))
                                .addGap(70, 70, 70)
                                .addGroup(featuresPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel4)
                                    .addComponent(jLabel5)
                                    .addGroup(featuresPanelLayout.createSequentialGroup()
                                        .addGap(10, 10, 10)
                                        .addGroup(featuresPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(pxCheck)
                                            .addComponent(geneAnnotationCheck)
                                            .addComponent(proteinAbundanceIndexesCheck)))))
                            .addComponent(decoyCheck))))
                .addGap(25, 25, 25))
        );
        featuresPanelLayout.setVerticalGroup(
            featuresPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(featuresPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(featuresPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel1)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(featuresPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(featuresPanelLayout.createSequentialGroup()
                        .addComponent(algorithmsCheck)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(searchGUICheck))
                    .addGroup(featuresPanelLayout.createSequentialGroup()
                        .addComponent(geneAnnotationCheck)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(proteinAbundanceIndexesCheck)))
                .addGap(21, 21, 21)
                .addGroup(featuresPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel2)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(featuresPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(proteinDbCkeck)
                    .addComponent(pxCheck))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(decoyCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(idParametersCheck)
                .addGap(24, 24, 24)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(peptideShakerCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(validationCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ptmLocalizationCheck)
                .addContainerGap(94, Short.MAX_VALUE))
        );

        outputPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Output"));
        outputPanel.setOpaque(false);
        outputPanel.setPreferredSize(new java.awt.Dimension(500, 93));

        exportCoaLbl.setText("<html><a href>Export Certificate of Analysis</a></html>");
        exportCoaLbl.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                exportCoaLblMouseReleased(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportCoaLblMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportCoaLblMouseExited(evt);
            }
        });

        copyLbl.setText("<html><a href>Copy to Clipboard</a></html>");
        copyLbl.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                copyLblMouseReleased(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                copyLblMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                copyLblMouseExited(evt);
            }
        });

        outputAreadScrollPane.setBorder(null);
        outputAreadScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        outputTextArea.setColumns(20);
        outputTextArea.setLineWrap(true);
        outputTextArea.setRows(5);
        outputTextArea.setTabSize(4);
        outputTextArea.setWrapStyleWord(true);
        outputAreadScrollPane.setViewportView(outputTextArea);

        javax.swing.GroupLayout outputPanelLayout = new javax.swing.GroupLayout(outputPanel);
        outputPanel.setLayout(outputPanelLayout);
        outputPanelLayout.setHorizontalGroup(
            outputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(outputPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(outputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(outputPanelLayout.createSequentialGroup()
                        .addComponent(copyLbl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(exportCoaLbl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(outputAreadScrollPane, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        outputPanelLayout.setVerticalGroup(
            outputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(outputPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(outputAreadScrollPane)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(outputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(exportCoaLbl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(copyLbl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        waitingLabel.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        waitingLabel.setText("Gathering the required information. Please wait...");

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(waitingLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(okButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton))
                    .addComponent(introductionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addComponent(featuresPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(outputPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 442, Short.MAX_VALUE)))
                .addContainerGap())
        );

        backgroundPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(introductionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(featuresPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE)
                    .addComponent(outputPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton)
                    .addComponent(okButton)
                    .addComponent(waitingLabel))
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
     * Close the dialog.
     *
     * @param evt
     */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    /**
     * Close the dialog.
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        dispose();
    }//GEN-LAST:event_okButtonActionPerformed

    /**
     * Write the certificate of analysis.
     *
     * @param evt
     */
    private void exportCoaLblMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportCoaLblMouseReleased
        writeCoa();
    }//GEN-LAST:event_exportCoaLblMouseReleased

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void exportCoaLblMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportCoaLblMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportCoaLblMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void exportCoaLblMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportCoaLblMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportCoaLblMouseExited

    /**
     * Update the methods draft.
     *
     * @param evt
     */
    private void algorithmsCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_algorithmsCheckActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_algorithmsCheckActionPerformed

    /**
     * Update the methods draft.
     *
     * @param evt
     */
    private void searchGUICheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchGUICheckActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_searchGUICheckActionPerformed

    /**
     * Update the methods draft.
     *
     * @param evt
     */
    private void proteinDbCkeckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinDbCkeckActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_proteinDbCkeckActionPerformed

    /**
     * Update the methods draft.
     *
     * @param evt
     */
    private void decoyCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_decoyCheckActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_decoyCheckActionPerformed

    /**
     * Update the methods draft.
     *
     * @param evt
     */
    private void idParametersCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_idParametersCheckActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_idParametersCheckActionPerformed

    /**
     * Update the methods draft.
     *
     * @param evt
     */
    private void peptideShakerCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptideShakerCheckActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_peptideShakerCheckActionPerformed

    /**
     * Update the methods draft.
     *
     * @param evt
     */
    private void validationCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_validationCheckActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_validationCheckActionPerformed

    /**
     * Update the methods draft.
     *
     * @param evt
     */
    private void ptmLocalizationCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ptmLocalizationCheckActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_ptmLocalizationCheckActionPerformed

    /**
     * Update the methods draft.
     *
     * @param evt
     */
    private void geneAnnotationCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_geneAnnotationCheckActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_geneAnnotationCheckActionPerformed

    /**
     * Update the methods draft.
     *
     * @param evt
     */
    private void proteinAbundanceIndexesCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinAbundanceIndexesCheckActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_proteinAbundanceIndexesCheckActionPerformed

    /**
     * Update the methods draft.
     *
     * @param evt
     */
    private void pxCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pxCheckActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_pxCheckActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void copyLblMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_copyLblMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_copyLblMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void copyLblMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_copyLblMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_copyLblMouseExited

    /**
     * Copies the methods section text to the system clipboard.
     *
     * @param evt
     */
    private void copyLblMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_copyLblMouseReleased
        Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection stringSelection = new StringSelection(outputTextArea.getText());
        clpbrd.setContents(stringSelection, null);
        JOptionPane.showMessageDialog(this, "Text copied to clipboard.", "Text Copied", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_copyLblMouseReleased

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox algorithmsCheck;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel copyLbl;
    private javax.swing.JCheckBox decoyCheck;
    private javax.swing.JLabel exportCoaLbl;
    private javax.swing.JPanel featuresPanel;
    private javax.swing.JCheckBox geneAnnotationCheck;
    private javax.swing.JCheckBox idParametersCheck;
    private javax.swing.JPanel introductionPanel;
    private javax.swing.JTextArea introductionTxt;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton okButton;
    private javax.swing.JScrollPane outputAreadScrollPane;
    private javax.swing.JPanel outputPanel;
    private javax.swing.JTextArea outputTextArea;
    private javax.swing.JCheckBox peptideShakerCheck;
    private javax.swing.JCheckBox proteinAbundanceIndexesCheck;
    private javax.swing.JCheckBox proteinDbCkeck;
    private javax.swing.JCheckBox ptmLocalizationCheck;
    private javax.swing.JCheckBox pxCheck;
    private javax.swing.JCheckBox searchGUICheck;
    private javax.swing.JCheckBox validationCheck;
    private javax.swing.JLabel waitingLabel;
    // End of variables declaration//GEN-END:variables
}
