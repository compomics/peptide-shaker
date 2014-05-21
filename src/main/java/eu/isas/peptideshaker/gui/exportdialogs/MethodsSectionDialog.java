package eu.isas.peptideshaker.gui.exportdialogs;

import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.io.export.ExportFactory;
import com.compomics.util.io.export.ExportScheme;
import eu.isas.peptideshaker.export.PeptideShakerMethods;
import eu.isas.peptideshaker.export.PSExportFactory;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.JOptionPane;

/**
 * A dialog for drafting the methods section for a publication based on
 * PeptideShaker results.
 *
 * @author Harald Barsnes
 */
public class MethodsSectionDialog extends javax.swing.JDialog {

    /**
     * The main PeptideShaker frame.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * A simple progress dialog.
     */
    private static ProgressDialogX progressDialog;

    /**
     * Creates a new MethodsSectionDialog.
     *
     * @param peptideShakerGUI the main frame
     * @param modal
     */
    public MethodsSectionDialog(PeptideShakerGUI peptideShakerGUI, boolean modal) {
        super(peptideShakerGUI, modal);
        initComponents();
        this.peptideShakerGUI = peptideShakerGUI;
        updateMethodsSection();
        setLocationRelativeTo(peptideShakerGUI);
        setVisible(true);
    }

    /**
     * Generates the methods section according to the current user selections
     * and shows it in the GUI.
     */
    private void updateMethodsSection() {

        String text = "<html>";

        text += "<h2>Protein Identification</h2>";

        if (algorithmsCheck.isSelected()) {
            text+= PeptideShakerMethods.getSearchEnginesText(peptideShakerGUI.getProjectDetails());
        }
        if (searchGUICheck.isSelected()) {
            text+= PeptideShakerMethods.getSearchGUIText();
        }
        if (proteinDbCkeck.isSelected()) {
            text+= PeptideShakerMethods.getDatabaseText();
        }
        if (decoyCheck.isSelected()) {
            text+= PeptideShakerMethods.getDecoyType();
        }
        if (idParametersCheck.isSelected()) {
            text+= PeptideShakerMethods.getIdentificationSettings(peptideShakerGUI.getSearchParameters());
        }
        if (peptideShakerCheck.isSelected()) {
            text+= PeptideShakerMethods.getPeptideShaker();
        }
        if (validationCheck.isSelected()) {
            text+= PeptideShakerMethods.getValidation(peptideShakerGUI.getProcessingPreferences());
        }
        if (ptmLocalizationCheck.isSelected()) {
            text+= PeptideShakerMethods.getPtmScoring(peptideShakerGUI.getPtmScoringPreferences());
        }
        if (geneAnnotationCheck.isSelected()) {
            text+= PeptideShakerMethods.getGeneAnnoration();
        }
        if (proteinAbundanceIndexesCheck.isSelected()) {
            text+= PeptideShakerMethods.getSpectrumCounting(peptideShakerGUI.getSpectrumCountingPreferences());
        }
        if (pxCheck.isSelected()) {
            text+= PeptideShakerMethods.getProteomeXchage();
        }

        text += "</html>";

        methodsSectionEditorPane.setText(text);
    }
    
    /**
     * Lets the user select a file where to write the coa
     */
    private void writeCoa() {
        
        // get the file to send the output to
        final File selectedFile = peptideShakerGUI.getUserSelectedFile(".txt", "Text file (.txt)", "Export...", false);

        if (selectedFile != null) {
            progressDialog = new ProgressDialogX(this, peptideShakerGUI,
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                    true);
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
                        PSExportFactory.writeExport(exportScheme, selectedFile, peptideShakerGUI.getExperiment().getReference(),
                                peptideShakerGUI.getSample().getReference(), peptideShakerGUI.getReplicateNumber(),
                                peptideShakerGUI.getProjectDetails(), peptideShakerGUI.getIdentification(),
                                peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getSearchParameters(),
                                null, null, null, null, peptideShakerGUI.getDisplayPreferences().getnAASurroundingPeptides(),
                                peptideShakerGUI.getAnnotationPreferences(), peptideShakerGUI.getIdFilter(),
                                peptideShakerGUI.getPtmScoringPreferences(), peptideShakerGUI.getSpectrumCountingPreferences(), progressDialog);

                        boolean processCancelled = progressDialog.isRunCanceled();
                        progressDialog.setRunFinished();

                        if (!processCancelled) {
                            JOptionPane.showMessageDialog(peptideShakerGUI, "Data copied to file:\n" + filePath, "Data Exported.", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (FileNotFoundException e) {
                        progressDialog.setRunFinished();
                        JOptionPane.showMessageDialog(peptideShakerGUI,
                                "An error occurred while generating the output. Please make sure "
                                + "that the destination file is not opened by another application.", "Output Error.", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    } catch (Exception e) {
                        progressDialog.setRunFinished();
                        JOptionPane.showMessageDialog(peptideShakerGUI, "An error occurred while generating the output.", "Output Error.", JOptionPane.ERROR_MESSAGE);
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
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        settingsPanel = new javax.swing.JPanel();
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
        outputScrollPane = new javax.swing.JScrollPane();
        methodsSectionEditorPane = new javax.swing.JEditorPane();
        exportCoaLbl = new javax.swing.JLabel();
        introductionPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        introductionTxt = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Methods Section Draft");
        setMinimumSize(new java.awt.Dimension(450, 400));

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

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

        settingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Features"));
        settingsPanel.setOpaque(false);
        settingsPanel.setPreferredSize(new java.awt.Dimension(500, 523));

        algorithmsCheck.setSelected(true);
        algorithmsCheck.setText("Identification algorithms");
        algorithmsCheck.setOpaque(false);
        algorithmsCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                algorithmsCheckActionPerformed(evt);
            }
        });

        searchGUICheck.setSelected(true);
        searchGUICheck.setText("SearchGUI");
        searchGUICheck.setOpaque(false);
        searchGUICheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchGUICheckActionPerformed(evt);
            }
        });

        jLabel1.setText("Spectrum Identification Algorithms:");

        jLabel2.setText("Spectrum Identification Settings:");

        proteinDbCkeck.setSelected(true);
        proteinDbCkeck.setText("Protein database");
        proteinDbCkeck.setOpaque(false);
        proteinDbCkeck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinDbCkeckActionPerformed(evt);
            }
        });

        decoyCheck.setSelected(true);
        decoyCheck.setText("Decoy sequences generation");
        decoyCheck.setOpaque(false);
        decoyCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                decoyCheckActionPerformed(evt);
            }
        });

        idParametersCheck.setSelected(true);
        idParametersCheck.setText("Identification parameters");
        idParametersCheck.setOpaque(false);
        idParametersCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                idParametersCheckActionPerformed(evt);
            }
        });

        jLabel3.setText("Peptide and Protein Identification:");

        peptideShakerCheck.setSelected(true);
        peptideShakerCheck.setText("PeptideShaker");
        peptideShakerCheck.setOpaque(false);
        peptideShakerCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptideShakerCheckActionPerformed(evt);
            }
        });

        validationCheck.setSelected(true);
        validationCheck.setText("Statistical validation");
        validationCheck.setOpaque(false);
        validationCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                validationCheckActionPerformed(evt);
            }
        });

        ptmLocalizationCheck.setSelected(true);
        ptmLocalizationCheck.setText("PTM localization");
        ptmLocalizationCheck.setOpaque(false);
        ptmLocalizationCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ptmLocalizationCheckActionPerformed(evt);
            }
        });

        geneAnnotationCheck.setText("Gene annotation");
        geneAnnotationCheck.setOpaque(false);
        geneAnnotationCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                geneAnnotationCheckActionPerformed(evt);
            }
        });

        proteinAbundanceIndexesCheck.setText("Protein abundance index");
        proteinAbundanceIndexesCheck.setOpaque(false);
        proteinAbundanceIndexesCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinAbundanceIndexesCheckActionPerformed(evt);
            }
        });

        jLabel4.setText("Additional Features:");

        jLabel5.setText("Identification Repository:");

        pxCheck.setSelected(true);
        pxCheck.setText("ProteomeXchange");
        pxCheck.setOpaque(false);
        pxCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pxCheckActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout settingsPanelLayout = new javax.swing.GroupLayout(settingsPanel);
        settingsPanel.setLayout(settingsPanelLayout);
        settingsPanelLayout.setHorizontalGroup(
            settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5)
                    .addGroup(settingsPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(pxCheck)
                            .addComponent(peptideShakerCheck)
                            .addComponent(proteinDbCkeck)
                            .addComponent(algorithmsCheck)
                            .addComponent(searchGUICheck)
                            .addComponent(decoyCheck)
                            .addComponent(idParametersCheck)
                            .addComponent(validationCheck)
                            .addComponent(ptmLocalizationCheck)
                            .addComponent(geneAnnotationCheck)
                            .addComponent(proteinAbundanceIndexesCheck))))
                .addContainerGap(303, Short.MAX_VALUE))
        );
        settingsPanelLayout.setVerticalGroup(
            settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(algorithmsCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(searchGUICheck)
                .addGap(18, 18, 18)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(proteinDbCkeck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(decoyCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(idParametersCheck)
                .addGap(18, 18, 18)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(peptideShakerCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(validationCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(ptmLocalizationCheck)
                .addGap(18, 18, 18)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(geneAnnotationCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(proteinAbundanceIndexesCheck)
                .addGap(18, 18, 18)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(pxCheck)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        outputPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Output"));
        outputPanel.setOpaque(false);
        outputPanel.setPreferredSize(new java.awt.Dimension(500, 93));

        methodsSectionEditorPane.setEditable(false);
        methodsSectionEditorPane.setContentType("text/html"); // NOI18N
        methodsSectionEditorPane.setMaximumSize(new java.awt.Dimension(600, 2147483647));
        methodsSectionEditorPane.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
            public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {
                methodsSectionEditorPaneHyperlinkUpdate(evt);
            }
        });
        outputScrollPane.setViewportView(methodsSectionEditorPane);

        exportCoaLbl.setForeground(new java.awt.Color(0, 0, 204));
        exportCoaLbl.setText("Export Certificate of Analysis");
        exportCoaLbl.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportCoaLblMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportCoaLblMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                exportCoaLblMouseReleased(evt);
            }
        });

        javax.swing.GroupLayout outputPanelLayout = new javax.swing.GroupLayout(outputPanel);
        outputPanel.setLayout(outputPanelLayout);
        outputPanelLayout.setHorizontalGroup(
            outputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(outputPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(outputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(outputScrollPane)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, outputPanelLayout.createSequentialGroup()
                        .addGap(0, 328, Short.MAX_VALUE)
                        .addComponent(exportCoaLbl)))
                .addContainerGap())
        );
        outputPanelLayout.setVerticalGroup(
            outputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(outputPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(outputScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 453, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(exportCoaLbl)
                .addContainerGap())
        );

        introductionPanel.setOpaque(false);

        jScrollPane1.setBorder(null);
        jScrollPane1.setEnabled(false);
        jScrollPane1.setOpaque(false);

        introductionTxt.setEditable(false);
        introductionTxt.setBackground(new java.awt.Color(240, 240, 240));
        introductionTxt.setColumns(20);
        introductionTxt.setFont(new java.awt.Font("Tahoma", 0, 13)); // NOI18N
        introductionTxt.setLineWrap(true);
        introductionTxt.setRows(5);
        introductionTxt.setText("The Method section editor automatically generates a text listing the methods used for protein identification with SearchGUI and PeptideShaker. It can serve as a basis to write the Method section of Manuscripts.\n\n1- Select the relevant sections on the left panel.\n2- Copy the output in a text editor.\n3- Complete the missing sections marked in brackets. References are indicated by their Pubmed ID (PMID), paste it in pubmed to retrieve the original reference.\n4- Export the Certificate of Analysis and add it to the supplementary material of your manuscript and to the files uploaded in ProteomeXchange.\n\nNote: the section editor does not include the raw file to peak list conversion.");
        introductionTxt.setWrapStyleWord(true);
        introductionTxt.setMargin(new java.awt.Insets(10, 10, 10, 10));
        jScrollPane1.setViewportView(introductionTxt);

        javax.swing.GroupLayout introductionPanelLayout = new javax.swing.GroupLayout(introductionPanel);
        introductionPanel.setLayout(introductionPanelLayout);
        introductionPanelLayout.setHorizontalGroup(
            introductionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
        );
        introductionPanelLayout.setVerticalGroup(
            introductionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(introductionPanelLayout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 189, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(okButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton))
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(introductionPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, backgroundPanelLayout.createSequentialGroup()
                                .addComponent(settingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(outputPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        backgroundPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(introductionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(settingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(outputPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 523, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton)
                    .addComponent(okButton))
                .addGap(143, 143, 143))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 793, Short.MAX_VALUE)
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
     * Makes the links active.
     *
     * @param evt
     */
    private void methodsSectionEditorPaneHyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {//GEN-FIRST:event_methodsSectionEditorPaneHyperlinkUpdate
        if (evt.getEventType().toString().equalsIgnoreCase(
                javax.swing.event.HyperlinkEvent.EventType.ENTERED.toString())) {
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        } else if (evt.getEventType().toString().equalsIgnoreCase(
                javax.swing.event.HyperlinkEvent.EventType.EXITED.toString())) {
            setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        } else if (evt.getEventType().toString().equalsIgnoreCase(
                javax.swing.event.HyperlinkEvent.EventType.ACTIVATED.toString())) {
            if (evt.getDescription().startsWith("#")) {
                methodsSectionEditorPane.scrollToReference(evt.getDescription());
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                BareBonesBrowserLaunch.openURL(evt.getDescription());
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        }
    }//GEN-LAST:event_methodsSectionEditorPaneHyperlinkUpdate

    private void exportCoaLblMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportCoaLblMouseReleased
        writeCoa();
    }//GEN-LAST:event_exportCoaLblMouseReleased

    private void exportCoaLblMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportCoaLblMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportCoaLblMouseEntered

    private void exportCoaLblMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportCoaLblMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportCoaLblMouseExited

    private void algorithmsCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_algorithmsCheckActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_algorithmsCheckActionPerformed

    private void searchGUICheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchGUICheckActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_searchGUICheckActionPerformed

    private void proteinDbCkeckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinDbCkeckActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_proteinDbCkeckActionPerformed

    private void decoyCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_decoyCheckActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_decoyCheckActionPerformed

    private void idParametersCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_idParametersCheckActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_idParametersCheckActionPerformed

    private void peptideShakerCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptideShakerCheckActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_peptideShakerCheckActionPerformed

    private void validationCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_validationCheckActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_validationCheckActionPerformed

    private void ptmLocalizationCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ptmLocalizationCheckActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_ptmLocalizationCheckActionPerformed

    private void geneAnnotationCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_geneAnnotationCheckActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_geneAnnotationCheckActionPerformed

    private void proteinAbundanceIndexesCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinAbundanceIndexesCheckActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_proteinAbundanceIndexesCheckActionPerformed

    private void pxCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pxCheckActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_pxCheckActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox algorithmsCheck;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JCheckBox decoyCheck;
    private javax.swing.JLabel exportCoaLbl;
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
    private javax.swing.JEditorPane methodsSectionEditorPane;
    private javax.swing.JButton okButton;
    private javax.swing.JPanel outputPanel;
    private javax.swing.JScrollPane outputScrollPane;
    private javax.swing.JCheckBox peptideShakerCheck;
    private javax.swing.JCheckBox proteinAbundanceIndexesCheck;
    private javax.swing.JCheckBox proteinDbCkeck;
    private javax.swing.JCheckBox ptmLocalizationCheck;
    private javax.swing.JCheckBox pxCheck;
    private javax.swing.JCheckBox searchGUICheck;
    private javax.swing.JPanel settingsPanel;
    private javax.swing.JCheckBox validationCheck;
    // End of variables declaration//GEN-END:variables

}
