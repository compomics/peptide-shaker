package eu.isas.peptideshaker.gui.exportdialogs;

import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.identification.SearchParameters;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import java.util.ArrayList;
import java.util.Collections;

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

        text += "<h2>Methods</h2>";

        if (proteowizardCheckBox.isSelected()) {
            text += "The raw data was converted to mgf using ProteoWizard v[add version] [PMID:3471674]";
            if (!searchGuiCheckBox.isSelected()) {
                text += ". ";
            }
        }
        if (searchGuiCheckBox.isSelected()) {
            if (!proteowizardCheckBox.isSelected()) {
                text += "The data was ";
            } else {
                text += " and ";
            }

            if (databaseCheckBox.isSelected()) {
                text += "searched against [add database details]"; // @TODO: add database details
            } else {
                text += "searched";
            }

            text += " using SearchGUI v[add version] [PMID:21337703] ";

            if (searchEnginesCheckBox.isSelected()) {

                text += " (employing ";

                ArrayList<String> searchEngines = peptideShakerGUI.getProjectDetails().getSearchEnginesNames();
                Collections.sort(searchEngines);

                for (int i = 0; i < searchEngines.size(); i++) {
                    if (i > 0) {
                        if (i == searchEngines.size() - 1) {
                            text += " and ";
                        } else {
                            text += ",";
                        }
                    }
                    text += searchEngines.get(i);
                    text += " [" + convertSearchEngineToReference(searchEngines.get(i)) + "]";
                }

                text += ")";
            }

            if (searchParametersCheckBox.isSelected()) {

                SearchParameters searchParameters = peptideShakerGUI.getSearchParameters();

                text += " with the following search parameters: ";
                text += "enzyme: " + searchParameters.getEnzyme().getName();
                text += "; max missed cleavages: " + searchParameters.getnMissedCleavages();
                text += "; precursor accuracy: " + searchParameters.getPrecursorAccuracy();
                if (searchParameters.getPrecursorAccuracyType() == SearchParameters.MassAccuracyType.PPM) {
                    text += " ppm";
                } else {
                    text += " Da";
                }
                text += "; fragment ion accuracy: " + searchParameters.getFragmentIonAccuracy() + " Da";

                ArrayList<String> fixedPtms = searchParameters.getModificationProfile().getFixedModifications();
                text += getPtmsAsString(fixedPtms, true);

                ArrayList<String> variablePtms = searchParameters.getModificationProfile().getVariableModifications();
                text += getPtmsAsString(variablePtms, false);

                text += ". ";
            } else {
                text += ". ";
            }
        }

        text += "The search results were merged and processed in PeptideShaker v" + peptideShakerGUI.getVersion() + " [http://peptide-shaker.googlecode.com]"; // @TODO: add ref

        if (validationCheckBox.isSelected()) {
            text += " using [add FDR/FNR/Confidence levels] at the protein, peptide and PSM level, respectively.";

            text += "This resulted in ";

            try {
                text += peptideShakerGUI.getIdentificationFeaturesGenerator().getNValidatedProteins();
            } catch (Exception e) {
                e.printStackTrace();
                text += "[error]";
            }

            text += " validated proteins.";

        } else {
            text += ".";
        }

        text += "</html>";

        methodsSectionEditorPane.setText(text);
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
        proteowizardCheckBox = new javax.swing.JCheckBox();
        searchGuiCheckBox = new javax.swing.JCheckBox();
        searchEnginesCheckBox = new javax.swing.JCheckBox();
        searchParametersCheckBox = new javax.swing.JCheckBox();
        databaseCheckBox = new javax.swing.JCheckBox();
        validationCheckBox = new javax.swing.JCheckBox();
        outputPanel = new javax.swing.JPanel();
        outputScrollPane = new javax.swing.JScrollPane();
        methodsSectionEditorPane = new javax.swing.JEditorPane();

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

        settingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Settings"));
        settingsPanel.setOpaque(false);

        proteowizardCheckBox.setSelected(true);
        proteowizardCheckBox.setText("ProteoWizard");
        proteowizardCheckBox.setIconTextGap(15);
        proteowizardCheckBox.setInheritsPopupMenu(true);
        proteowizardCheckBox.setOpaque(false);
        proteowizardCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteowizardCheckBoxActionPerformed(evt);
            }
        });

        searchGuiCheckBox.setSelected(true);
        searchGuiCheckBox.setText("SearchGUI");
        searchGuiCheckBox.setIconTextGap(15);
        searchGuiCheckBox.setInheritsPopupMenu(true);
        searchGuiCheckBox.setOpaque(false);
        searchGuiCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchGuiCheckBoxActionPerformed(evt);
            }
        });

        searchEnginesCheckBox.setSelected(true);
        searchEnginesCheckBox.setText("Search Engines");
        searchEnginesCheckBox.setIconTextGap(15);
        searchEnginesCheckBox.setInheritsPopupMenu(true);
        searchEnginesCheckBox.setOpaque(false);
        searchEnginesCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchEnginesCheckBoxActionPerformed(evt);
            }
        });

        searchParametersCheckBox.setSelected(true);
        searchParametersCheckBox.setText("Search Parameters");
        searchParametersCheckBox.setIconTextGap(15);
        searchParametersCheckBox.setInheritsPopupMenu(true);
        searchParametersCheckBox.setMinimumSize(new java.awt.Dimension(300, 400));
        searchParametersCheckBox.setOpaque(false);
        searchParametersCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchParametersCheckBoxActionPerformed(evt);
            }
        });

        databaseCheckBox.setSelected(true);
        databaseCheckBox.setText("Database Details");
        databaseCheckBox.setIconTextGap(15);
        databaseCheckBox.setInheritsPopupMenu(true);
        databaseCheckBox.setOpaque(false);
        databaseCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                databaseCheckBoxActionPerformed(evt);
            }
        });

        validationCheckBox.setSelected(true);
        validationCheckBox.setText("Validation Details");
        validationCheckBox.setIconTextGap(15);
        validationCheckBox.setInheritsPopupMenu(true);
        validationCheckBox.setOpaque(false);
        validationCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                validationCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout settingsPanelLayout = new javax.swing.GroupLayout(settingsPanel);
        settingsPanel.setLayout(settingsPanelLayout);
        settingsPanelLayout.setHorizontalGroup(
            settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(proteowizardCheckBox)
                    .addComponent(searchGuiCheckBox)
                    .addComponent(searchEnginesCheckBox))
                .addGap(51, 51, 51)
                .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(searchParametersCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(databaseCheckBox)
                    .addComponent(validationCheckBox))
                .addContainerGap(170, Short.MAX_VALUE))
        );
        settingsPanelLayout.setVerticalGroup(
            settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(settingsPanelLayout.createSequentialGroup()
                        .addComponent(searchParametersCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(databaseCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(validationCheckBox))
                    .addGroup(settingsPanelLayout.createSequentialGroup()
                        .addComponent(proteowizardCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(searchGuiCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(searchEnginesCheckBox)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        outputPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Output"));
        outputPanel.setOpaque(false);

        methodsSectionEditorPane.setEditable(false);
        methodsSectionEditorPane.setContentType("text/html"); // NOI18N
        methodsSectionEditorPane.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
            public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {
                methodsSectionEditorPaneHyperlinkUpdate(evt);
            }
        });
        outputScrollPane.setViewportView(methodsSectionEditorPane);

        javax.swing.GroupLayout outputPanelLayout = new javax.swing.GroupLayout(outputPanel);
        outputPanel.setLayout(outputPanelLayout);
        outputPanelLayout.setHorizontalGroup(
            outputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(outputPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(outputScrollPane)
                .addContainerGap())
        );
        outputPanelLayout.setVerticalGroup(
            outputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(outputPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(outputScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 276, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(settingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(okButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton))
                    .addComponent(outputPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        backgroundPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(settingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(outputPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton)
                    .addComponent(okButton))
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

    /**
     * Update the methods section.
     *
     * @param evt
     */
    private void proteowizardCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteowizardCheckBoxActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_proteowizardCheckBoxActionPerformed

    /**
     * Update the methods section.
     *
     * @param evt
     */
    private void searchGuiCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchGuiCheckBoxActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_searchGuiCheckBoxActionPerformed

    /**
     * Update the methods section.
     *
     * @param evt
     */
    private void searchEnginesCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchEnginesCheckBoxActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_searchEnginesCheckBoxActionPerformed

    /**
     * Update the methods section.
     *
     * @param evt
     */
    private void searchParametersCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchParametersCheckBoxActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_searchParametersCheckBoxActionPerformed

    /**
     * Update the methods section.
     *
     * @param evt
     */
    private void databaseCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_databaseCheckBoxActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_databaseCheckBoxActionPerformed

    /**
     * Update the methods section.
     *
     * @param evt
     */
    private void validationCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_validationCheckBoxActionPerformed
        updateMethodsSection();
    }//GEN-LAST:event_validationCheckBoxActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JCheckBox databaseCheckBox;
    private javax.swing.JEditorPane methodsSectionEditorPane;
    private javax.swing.JButton okButton;
    private javax.swing.JPanel outputPanel;
    private javax.swing.JScrollPane outputScrollPane;
    private javax.swing.JCheckBox proteowizardCheckBox;
    private javax.swing.JCheckBox searchEnginesCheckBox;
    private javax.swing.JCheckBox searchGuiCheckBox;
    private javax.swing.JCheckBox searchParametersCheckBox;
    private javax.swing.JPanel settingsPanel;
    private javax.swing.JCheckBox validationCheckBox;
    // End of variables declaration//GEN-END:variables

    /**
     * Returns the PTMs as a string.
     *
     * @param ptms the PTM names to return
     * @param fixed if the PTMs are fixed or variable
     * @return the PTMs as a string
     */
    private String getPtmsAsString(ArrayList<String> ptms, boolean fixed) {

        String text = "";

        if (!ptms.isEmpty()) {
            if (fixed) {
                text += "; fixed modifications: ";
            } else {
                text += "; variable modifications: ";
            }
            for (int i = 0; i < ptms.size(); i++) {
                if (i == ptms.size() - 1 && ptms.size() > 1) {
                    text += " and ";
                }
                text += ptms.get(i);
                if (i < ptms.size() - 1) {
                    text += ", ";
                }
            }
        }

        return text;
    }

    /**
     * Returns the reference to be used for the given search engine.
     *
     * @param searchEngine
     * @return the reference for the given search engine
     */
    private String convertSearchEngineToReference(String searchEngine) {

        // @TODO: should be moved to the Advocate class?

        if (searchEngine.equalsIgnoreCase("OMSSA")) {
            return "PMID:15473683";
        } else if (searchEngine.equalsIgnoreCase("OMSSA")) {
            return "PMID:???";
        } else if (searchEngine.equalsIgnoreCase("X!Tandem")) {
            return "PMID:???";
        } else if (searchEngine.equalsIgnoreCase("MS-GF+")) {
            return "PMID:???";
        } else {
            return "PMID:???";
        }

        // @TODO: add more search engine references 
    }
}
