package eu.isas.peptideshaker.gui.pride;

import eu.isas.peptideshaker.gui.pride.annotationdialogs.NewProtocolDialog;
import eu.isas.peptideshaker.gui.pride.annotationdialogs.NewReferenceGroupDialog;
import eu.isas.peptideshaker.gui.pride.annotationdialogs.NewContactGroupDialog;
import eu.isas.peptideshaker.gui.pride.annotationdialogs.NewSampleDialog;
import eu.isas.peptideshaker.gui.pride.annotationdialogs.NewInstrumentDialog;
import com.compomics.util.Util;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.ptm.PtmDialog;
import com.compomics.util.pride.prideobjects.Reference;
import com.compomics.util.pride.prideobjects.Contact;
import com.compomics.util.pride.prideobjects.Sample;
import com.compomics.util.pride.prideobjects.Instrument;
import com.compomics.util.pride.prideobjects.Protocol;
import com.compomics.util.pride.PtmToPrideMap;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.gui.renderers.ToolTipComboBoxRenderer;
import com.compomics.util.io.export.ExportWriter;
import com.compomics.util.preferences.LastSelectedFolder;
import com.compomics.util.pride.PrideObjectsFactory;
import com.compomics.util.pride.prideobjects.*;
import com.compomics.util.pride.validation.PrideXmlValidator;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.export.PrideXmlExport;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.gui.tabpanels.PtmPanel;
import java.awt.Color;
import java.awt.Toolkit;
import java.io.*;
import java.util.ArrayList;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * A dialog where the user can export the project to PRIDE XML.
 *
 * @author Harald Barsnes
 */
public class ProjectExportDialog extends javax.swing.JDialog {

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
     * The PTM to pride map.
     */
    private PrideObjectsFactory prideObjectsFactory = null;
    /**
     * If true, the created PRIDE XML file will be validated against the PRIDE
     * schema.
     */
    private boolean validatePrideXml = true;
    /**
     * The last selected folder.
     */
    private LastSelectedFolder lastSelectedFolder;

    /**
     * Create a new PrideExportDialog.
     *
     * @param peptideShakerGUI a reference to the main GUI frame
     * @param modal if the dialog is to be modal or not
     */
    public ProjectExportDialog(PeptideShakerGUI peptideShakerGUI, boolean modal) {
        super(peptideShakerGUI, modal);
        this.peptideShakerGUI = peptideShakerGUI;
        lastSelectedFolder = peptideShakerGUI.getLastSelectedFolder();

        // reset the pride object factory
        resetPrideObjectFactory();

        // update the ptm
        updatePtmMap();

        initComponents();

        // set gui properties
        setGuiProperties();

        // insert available contacts, instruments, protocols and samples
        insertReferenceOptions();
        insertContactOptions();
        insertSampleOptions();
        insertProtocolOptions();
        insertInstrumentOptions();

        // insert project data
        insertProjectData();

        // validate the input
        validateInput();

        setLocationRelativeTo(peptideShakerGUI);
        setVisible(true);
    }

    /**
     * Insert the reference options in the combo box.
     */
    private void insertReferenceOptions() {
        insertOptions(new ArrayList<String>(prideObjectsFactory.getReferenceGroups().keySet()),
                "--- Select a Reference Group ---", "   Create a New Reference Group...", referenceGroupsJComboBox);
    }

    /**
     * Insert the contact options in the combo box.
     */
    private void insertContactOptions() {
        insertOptions(new ArrayList<String>(prideObjectsFactory.getContactGroups().keySet()),
                "--- Select a Contact Group ---", "   Create a New Contact Group...", contactGroupsJComboBox);
    }

    /**
     * Insert the sample options in the combo box.
     */
    private void insertSampleOptions() {
        insertOptions(new ArrayList<String>(prideObjectsFactory.getSamples().keySet()),
                "--- Select a Sample Set ---", "   Create a New Sample Set...", sampleJComboBox);
    }

    /**
     * Insert the protocol options in the combo box.
     */
    private void insertProtocolOptions() {
        insertOptions(new ArrayList<String>(prideObjectsFactory.getProtocols().keySet()),
                "--- Select a Protocol ---", "   Create a New Protocol...", protocolJComboBox);
    }

    /**
     * Insert the instrument options in the combo box.
     */
    private void insertInstrumentOptions() {
        insertOptions(new ArrayList<String>(prideObjectsFactory.getInstruments().keySet()),
                "--- Select an Instrument ---", "   Create a New Instrument...", instrumentJComboBox);
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

        referenceGroupsJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        contactGroupsJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        sampleJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        protocolJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        instrumentJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
    }

    /**
     * Returns the last selected folder.
     *
     * @return the last selected folder
     */
    private String getLastSelectedFolder() {
        String result = null;
        if (lastSelectedFolder != null) {
            result = lastSelectedFolder.getLastSelectedFolder(ExportWriter.lastFolderKey);
            if (result == null) {
                result = lastSelectedFolder.getLastSelectedFolder();
            }
        }
        return result;
    }

    /**
     * Update the PTM map.
     */
    private void updatePtmMap() {

        ArrayList<String> missingMods = checkModifications();

        if (!missingMods.isEmpty()) {
            String report = "Unimod mapping is missing for the following modifications:\n";
            boolean first = true;
            for (String mod : missingMods) {
                if (first) {
                    first = false;
                } else {
                    report += ",\n";
                }
                report += mod;
            }
            report += ".";
            JOptionPane.showMessageDialog(peptideShakerGUI, report, "Missing Unimod Mapping(s)", JOptionPane.WARNING_MESSAGE);

            // have the user add the CV term mappings
            for (String modName : missingMods) {
                PTM currentPtm = PTMFactory.getInstance().getPTM(modName);
                new PtmDialog(this, currentPtm, false);
            }
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
        for (String modification : peptideShakerGUI.getIdentificationParameters().getSearchParameters().getPtmSettings().getAllModifications()) {
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

        // use the pride experiment title if set, or the project experiment reference if not 
        if (peptideShakerGUI.getProjectDetails().getPrideExperimentTitle() != null) {
            titleJTextField.setText(peptideShakerGUI.getProjectDetails().getPrideExperimentTitle());
        } else {
            titleJTextField.setText(peptideShakerGUI.getExperiment().getReference());
        }

        labelJTextField.setText(peptideShakerGUI.getProjectDetails().getPrideExperimentLabel());
        projectJTextField.setText(peptideShakerGUI.getProjectDetails().getPrideExperimentProjectTitle());
        descriptionJTextArea.setText(peptideShakerGUI.getProjectDetails().getPrideExperimentDescription());

        if (peptideShakerGUI.getProjectDetails().getPrideReferenceGroup() != null) {
            if (!prideObjectsFactory.getReferenceGroups().keySet().contains(peptideShakerGUI.getProjectDetails().getPrideReferenceGroup().getName())) {
                try {
                    // if the project has beem moved and the pride objects do not exist on the new machine we try to create them 
                    prideObjectsFactory.addReferenceGroup(peptideShakerGUI.getProjectDetails().getPrideReferenceGroup());
                    insertReferenceOptions();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            referenceGroupsJComboBox.setSelectedItem(peptideShakerGUI.getProjectDetails().getPrideReferenceGroup().getName());
        }
        if (peptideShakerGUI.getProjectDetails().getPrideContactGroup() != null) {
            if (!prideObjectsFactory.getContactGroups().keySet().contains(peptideShakerGUI.getProjectDetails().getPrideContactGroup().getName())) {
                try {
                    // if the project has beem moved and the pride objects do not exist on the new machine we try to create them 
                    prideObjectsFactory.addContactGroup(peptideShakerGUI.getProjectDetails().getPrideContactGroup());
                    insertContactOptions();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            contactGroupsJComboBox.setSelectedItem(peptideShakerGUI.getProjectDetails().getPrideContactGroup().getName());
        }
        if (peptideShakerGUI.getProjectDetails().getPrideSample() != null) {
            if (!prideObjectsFactory.getSamples().keySet().contains(peptideShakerGUI.getProjectDetails().getPrideSample().getName())) {
                try {
                    // if the project has beem moved and the pride objects do not exist on the new machine we try to create them 
                    prideObjectsFactory.addSample(peptideShakerGUI.getProjectDetails().getPrideSample());
                    insertSampleOptions();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            sampleJComboBox.setSelectedItem(peptideShakerGUI.getProjectDetails().getPrideSample().getName());
        }
        if (peptideShakerGUI.getProjectDetails().getPrideProtocol() != null) {
            if (!prideObjectsFactory.getProtocols().keySet().contains(peptideShakerGUI.getProjectDetails().getPrideProtocol().getName())) {
                try {
                    // if the project has beem moved and the pride objects do not exist on the new machine we try to create them 
                    prideObjectsFactory.addProtocol(peptideShakerGUI.getProjectDetails().getPrideProtocol());
                    insertProtocolOptions();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            protocolJComboBox.setSelectedItem(peptideShakerGUI.getProjectDetails().getPrideProtocol().getName());
        }
        if (peptideShakerGUI.getProjectDetails().getPrideProtocol() != null) {
            if (!prideObjectsFactory.getInstruments().keySet().contains(peptideShakerGUI.getProjectDetails().getPrideInstrument().getName())) {
                try {
                    // if the project has beem moved and the pride objects do not exist on the new machine we try to create them 
                    prideObjectsFactory.addInstrument(peptideShakerGUI.getProjectDetails().getPrideInstrument());
                    insertInstrumentOptions();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            instrumentJComboBox.setSelectedItem(peptideShakerGUI.getProjectDetails().getPrideInstrument().getName());
        }

        if (peptideShakerGUI.getProjectDetails().getPrideOutputFolder() != null
                && new File(peptideShakerGUI.getProjectDetails().getPrideOutputFolder()).exists()) {
            outputFolderJTextField.setText(peptideShakerGUI.getProjectDetails().getPrideOutputFolder());
        }
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

        for (String option : options) {
            comboboxTooltips.add(option);
        }

        comboboxTooltips.add(null);
        optionComboBox.setRenderer(new ToolTipComboBoxRenderer(comboboxTooltips, SwingConstants.CENTER));
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
        contactLabel = new javax.swing.JLabel();
        contactGroupsJComboBox = new javax.swing.JComboBox();
        editContactsJButton = new javax.swing.JButton();
        sampleLabel = new javax.swing.JLabel();
        sampleJComboBox = new javax.swing.JComboBox();
        editSampleJButton = new javax.swing.JButton();
        protocolLabel = new javax.swing.JLabel();
        protocolJComboBox = new javax.swing.JComboBox();
        editProtocolJButton = new javax.swing.JButton();
        instrumentLabel = new javax.swing.JLabel();
        instrumentJComboBox = new javax.swing.JComboBox();
        editInstrumentJButton = new javax.swing.JButton();
        referenceGroupsJComboBox = new javax.swing.JComboBox();
        editReferencesJButton = new javax.swing.JButton();
        convertJButton = new javax.swing.JButton();
        openDialogHelpJButton = new javax.swing.JButton();
        helpLabel = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        outputFolderLabel = new javax.swing.JLabel();
        outputFolderJTextField = new javax.swing.JTextField();
        browseOutputFolderJButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("PeptideShaker - Export");
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

        contactLabel.setForeground(new java.awt.Color(255, 0, 0));
        contactLabel.setText("Contact(s)*");
        contactLabel.setToolTipText("The contact person for the dataset");

        contactGroupsJComboBox.setMaximumRowCount(20);
        contactGroupsJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "--- Select ---", "Item 2", "Item 3", "Item 4" }));
        contactGroupsJComboBox.setToolTipText("The contact person for the PRIDE dataset");
        contactGroupsJComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contactGroupsJComboBoxActionPerformed(evt);
            }
        });

        editContactsJButton.setText("Edit");
        editContactsJButton.setToolTipText("Edit the selected contact");
        editContactsJButton.setEnabled(false);
        editContactsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editContactsJButtonActionPerformed(evt);
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

        referenceGroupsJComboBox.setMaximumRowCount(20);
        referenceGroupsJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "--- Select ---", "Item 2", "Item 3", "Item 4" }));
        referenceGroupsJComboBox.setToolTipText("Ther references for the PRIDE dataset");
        referenceGroupsJComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                referenceGroupsJComboBoxActionPerformed(evt);
            }
        });

        editReferencesJButton.setText("Edit");
        editReferencesJButton.setToolTipText("Edit the selected contact");
        editReferencesJButton.setEnabled(false);
        editReferencesJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editReferencesJButtonActionPerformed(evt);
            }
        });

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
                                .addComponent(titleJTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 462, Short.MAX_VALUE)
                                .addGap(18, 18, 18)
                                .addComponent(experimentLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(labelJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, experimentPropertiesPanelLayout.createSequentialGroup()
                                .addComponent(referenceGroupsJComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(editReferencesJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))))
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
                            .addComponent(contactGroupsJComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(editContactsJButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                    .addComponent(descriptionJScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(referenceGroupsJComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(editReferencesJButton)
                    .addComponent(referencesLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(contactLabel)
                    .addComponent(contactGroupsJComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(editContactsJButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sampleLabel)
                    .addComponent(sampleJComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(editSampleJButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(protocolLabel)
                    .addComponent(protocolJComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(editProtocolJButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(experimentPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(instrumentLabel)
                    .addComponent(instrumentJComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(editInstrumentJButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        experimentPropertiesPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {contactGroupsJComboBox, editContactsJButton});

        experimentPropertiesPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {editSampleJButton, sampleJComboBox});

        experimentPropertiesPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {editProtocolJButton, protocolJComboBox});

        experimentPropertiesPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {editInstrumentJButton, instrumentJComboBox});

        experimentPropertiesPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {editReferencesJButton, referenceGroupsJComboBox});

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

        outputFolderLabel.setForeground(new java.awt.Color(255, 0, 0));
        outputFolderLabel.setText("Folder*");
        outputFolderLabel.setToolTipText("The folder where the PRIDE XML file will be saved");

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
                .addComponent(outputFolderLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                    .addComponent(outputFolderLabel)
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
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(convertJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(15, 15, 15))
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
                    .addComponent(convertJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PrideExportDialog.html"),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PeptideShaker - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_openDialogHelpJButtonActionPerformed

    /**
     * Opens a file chooser where the user can select the output folder.
     *
     * @param evt
     */
    private void browseOutputFolderJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseOutputFolderJButtonActionPerformed
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        File selectedFolder = Util.getUserSelectedFolder(this, "Select Output Folder", getLastSelectedFolder(), "Output Folder", "Select", false);

        if (selectedFolder != null) {
            String path = selectedFolder.getAbsolutePath();
            lastSelectedFolder.setLastSelectedFolder(ExportWriter.lastFolderKey, path);
            outputFolderJTextField.setText(path);
        }

        validateInput();

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_browseOutputFolderJButtonActionPerformed

    /**
     * Enable/disable the edit contacts button and open the new contact group
     * dialog if the user selected to add a new contact.
     *
     * @param evt
     */
    private void contactGroupsJComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contactGroupsJComboBoxActionPerformed
        if (contactGroupsJComboBox.getSelectedIndex() == 0) {
            editContactsJButton.setEnabled(false);
        } else if (contactGroupsJComboBox.getSelectedIndex() == contactGroupsJComboBox.getItemCount() - 1) {
            editContactsJButton.setEnabled(false);
            contactGroupsJComboBox.setSelectedIndex(0);
            new NewContactGroupDialog(this, true);
        } else {
            editContactsJButton.setEnabled(true);
        }

        validateInput();
    }//GEN-LAST:event_contactGroupsJComboBoxActionPerformed

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
     * Enable/disable the edit protocol button and open the new protocol dialog
     * if the user selected to add a new protocol.
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
     * Opens a NewContactGroupDialog where the selected contact group can be
     * edited.
     *
     * @param evt
     */
    private void editContactsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editContactsJButtonActionPerformed

        // get the selected contact details
        String selectedContactGroup = (String) contactGroupsJComboBox.getSelectedItem();
        ContactGroup tempContactGroup = prideObjectsFactory.getContactGroups().get(selectedContactGroup);

        if (tempContactGroup == null) {
            tempContactGroup = new ContactGroup(new ArrayList<Contact>(), "");
        }

        new NewContactGroupDialog(this, true, tempContactGroup);
    }//GEN-LAST:event_editContactsJButtonActionPerformed

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
        String fileName = titleJTextField.getText().trim().replaceAll(" ", "_"); // @TODO: not sure why this is needed?
        File outputFile = new File(outputFolderJTextField.getText(), fileName + ".xml");

        if (outputFile.exists()) {
            int selection = JOptionPane.showConfirmDialog(this, "The file \'"
                    + outputFile.getAbsolutePath() + "\' already exists."
                    + "\nOverwrite file?",
                    "Overwrite?", JOptionPane.YES_NO_CANCEL_OPTION);

            if (selection != JOptionPane.YES_OPTION) {
                return;
            }
        }

        final String outputFileName = fileName;
        final ProjectExportDialog projectExportDialog = this; // needed due to threading issues

        progressDialog = new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Exporting PRIDE XML. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
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

                // get the references, if any
                String selectedReferenceGroup = (String) referenceGroupsJComboBox.getSelectedItem();
                ReferenceGroup referenceGroup = prideObjectsFactory.getReferenceGroups().get(selectedReferenceGroup);

                // get the selected contact details
                String selectedContactGroup = (String) contactGroupsJComboBox.getSelectedItem();
                ContactGroup contactGroup = prideObjectsFactory.getContactGroups().get(selectedContactGroup);

                // get the selected sample details
                String selectedSample = (String) sampleJComboBox.getSelectedItem();
                Sample sample = prideObjectsFactory.getSamples().get(selectedSample);

                // get the selected protcol details
                String selectedProtocol = (String) protocolJComboBox.getSelectedItem();
                Protocol protocol = prideObjectsFactory.getProtocols().get(selectedProtocol);

                // get the selected instrument details
                String selectedInstrument = (String) instrumentJComboBox.getSelectedItem();
                Instrument instrument = prideObjectsFactory.getInstruments().get(selectedInstrument);

                // save the inserted pride details with the project
                peptideShakerGUI.getProjectDetails().setPrideExperimentTitle(titleJTextField.getText());
                peptideShakerGUI.getProjectDetails().setPrideExperimentLabel(labelJTextField.getText());
                peptideShakerGUI.getProjectDetails().setPrideExperimentProjectTitle(projectJTextField.getText());
                peptideShakerGUI.getProjectDetails().setPrideExperimentDescription(descriptionJTextArea.getText());
                peptideShakerGUI.getProjectDetails().setPrideReferenceGroup(referenceGroup);
                peptideShakerGUI.getProjectDetails().setPrideContactGroup(contactGroup);
                peptideShakerGUI.getProjectDetails().setPrideSample(sample);
                peptideShakerGUI.getProjectDetails().setPrideProtocol(protocol);
                peptideShakerGUI.getProjectDetails().setPrideInstrument(instrument);
                peptideShakerGUI.getProjectDetails().setPrideOutputFolder(outputFolderJTextField.getText());
                peptideShakerGUI.setDataSaved(false); // @TODO: this might not always be true, e.g., if nothing has changed, but better than not saving at all

                boolean conversionCompleted;

                try {
                    PrideXmlExport prideExport = new PrideXmlExport(PeptideShaker.getVersion(), peptideShakerGUI.getIdentification(), peptideShakerGUI.getProjectDetails(),
                            peptideShakerGUI.getShotgunProtocol(), peptideShakerGUI.getIdentificationParameters(), peptideShakerGUI.getSpectrumCountingPreferences(),
                            peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getSpectrumAnnotator(),
                            selectedSample, selectedSample, selectedProtocol, selectedProtocol, referenceGroup, contactGroup, sample, protocol, instrument,
                            new File(outputFolderJTextField.getText()), outputFileName, progressDialog);

                    prideExport.createPrideXmlFile(progressDialog);

                    // validate the pride xml file
                    if (validatePrideXml && !projectExportDialog.progressCancelled()) {
                        progressDialog.setPrimaryProgressCounterIndeterminate(true);
                        progressDialog.setTitle("Validating PRIDE XML. Please Wait...");
                        PrideXmlValidator validator = new PrideXmlValidator();
                        conversionCompleted = validator.validate(new File(outputFolderJTextField.getText(), outputFileName + ".xml"));

                        // see if any errors were found, and display them to the user
                        if (!conversionCompleted) {
                            JOptionPane.showMessageDialog(null, validator.getErrorsAsString(), "PRIDE XML Errors", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        conversionCompleted = true;
                    }
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                    progressDialog.setRunCanceled();
                    progressDialog.dispose();
                    return;
                }

                // close the progress dialog
                boolean processCancelled = progressDialog.isRunCanceled();
                progressDialog.setRunFinished();

                // display a conversion complete message to the user
                if (conversionCompleted && !processCancelled) {

                    // create an empty label to put the message in
                    JLabel label = new JLabel();

                    // html content 
                    JEditorPane ep = new JEditorPane("text/html", "<html><body bgcolor=\"#" + Util.color2Hex(label.getBackground()) + "\">"
                            + "PRIDE XML file \'"
                            + new File(outputFolderJTextField.getText(), outputFileName + ".xml").getAbsolutePath() + "\' created.<br><br>"
                            + "Please see <a href=\"http://www.ebi.ac.uk/pride\">www.ebi.ac.uk/pride</a> for how to submit data to PRIDE.<br><br>"
                            + "We recommend checking the file in <a href=\"https://github.com/PRIDE-Toolsuite/pride-inspector\">PRIDE Inspector</a> before uploading."
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

                    JOptionPane.showMessageDialog(projectExportDialog, ep, "PRIDE XML File Created", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                }

                if (processCancelled) {
                    JOptionPane.showMessageDialog(peptideShakerGUI, "PRIDE XML conversion cancelled by the user.", "PRIDE XML Conversion Cancelled", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.start();
    }//GEN-LAST:event_convertJButtonActionPerformed

    /**
     * Enable/disable the edit references button and open the new reference
     * group dialog if the user selected to add a new reference.
     *
     * @param evt
     */
    private void referenceGroupsJComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_referenceGroupsJComboBoxActionPerformed
        if (referenceGroupsJComboBox.getSelectedIndex() == 0) {
            editReferencesJButton.setEnabled(false);
        } else if (referenceGroupsJComboBox.getSelectedIndex() == referenceGroupsJComboBox.getItemCount() - 1) {
            editReferencesJButton.setEnabled(false);
            referenceGroupsJComboBox.setSelectedIndex(0);
            new NewReferenceGroupDialog(this, true);
        } else {
            editReferencesJButton.setEnabled(true);
        }

        validateInput();
    }//GEN-LAST:event_referenceGroupsJComboBoxActionPerformed

    /**
     * Opens a NewReferenceGroupDialog where the selected reference group can be
     * edited.
     *
     * @param evt
     */
    private void editReferencesJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editReferencesJButtonActionPerformed
        // get the selected reference details
        String selectedReferenceGroup = (String) referenceGroupsJComboBox.getSelectedItem();
        ReferenceGroup tempReferenceGroup = prideObjectsFactory.getReferenceGroups().get(selectedReferenceGroup);

        if (tempReferenceGroup == null) {
            tempReferenceGroup = new ReferenceGroup(new ArrayList<Reference>(), "");
        }

        new NewReferenceGroupDialog(this, true, tempReferenceGroup);
    }//GEN-LAST:event_editReferencesJButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel backgroundJPanel;
    private javax.swing.JButton browseOutputFolderJButton;
    private javax.swing.JComboBox contactGroupsJComboBox;
    private javax.swing.JLabel contactLabel;
    private javax.swing.JButton convertJButton;
    private javax.swing.JScrollPane descriptionJScrollPane;
    private javax.swing.JTextArea descriptionJTextArea;
    private javax.swing.JLabel descriptionLabel;
    private javax.swing.JButton editContactsJButton;
    private javax.swing.JButton editInstrumentJButton;
    private javax.swing.JButton editProtocolJButton;
    private javax.swing.JButton editReferencesJButton;
    private javax.swing.JButton editSampleJButton;
    private javax.swing.JLabel experimentLabel;
    private javax.swing.JLabel experimentPropertiesLabel;
    private javax.swing.JPanel experimentPropertiesPanel;
    private javax.swing.JLabel helpLabel;
    private javax.swing.JComboBox instrumentJComboBox;
    private javax.swing.JLabel instrumentLabel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextField labelJTextField;
    private javax.swing.JButton openDialogHelpJButton;
    private javax.swing.JTextField outputFolderJTextField;
    private javax.swing.JLabel outputFolderLabel;
    private javax.swing.JTextField projectJTextField;
    private javax.swing.JLabel projectLabel;
    private javax.swing.JComboBox protocolJComboBox;
    private javax.swing.JLabel protocolLabel;
    private javax.swing.JComboBox referenceGroupsJComboBox;
    private javax.swing.JLabel referencesLabel;
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

        if ((contactGroupsJComboBox.getSelectedIndex() == 0 || contactGroupsJComboBox.getSelectedIndex() == contactGroupsJComboBox.getItemCount() - 1)
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
            outputFolderLabel.setForeground(Color.BLACK);
        } else {
            outputFolderLabel.setForeground(Color.RED);
        }

        if (contactGroupsJComboBox.getSelectedIndex() != 0) {
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
     * Sets the selected reference group.
     *
     * @param referenceGroup the reference group
     */
    public void setReferences(ReferenceGroup referenceGroup) {

        try {
            prideObjectsFactory.addReferenceGroup(referenceGroup);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }

        insertReferenceOptions();

        int selectedReferenceIndex = 0;

        for (int i = 0; i < referenceGroupsJComboBox.getItemCount(); i++) {
            if (((String) referenceGroupsJComboBox.getItemAt(i)).equalsIgnoreCase(referenceGroup.getName())) {
                selectedReferenceIndex = i;
            }
        }

        referenceGroupsJComboBox.setSelectedIndex(selectedReferenceIndex);
        referenceGroupsJComboBoxActionPerformed(null);
    }

    /**
     * Save the provided protocol to file and then select it in the list.
     *
     * @param protocol the protocol
     */
    public void setProtocol(Protocol protocol) {
        try {
            prideObjectsFactory.addProtocol(protocol);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }

        insertProtocolOptions();

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
     * Save the provided instrument to file and then select it in the list.
     *
     * @param instrument the instrument
     */
    public void setInstrument(Instrument instrument) {
        try {
            prideObjectsFactory.addInstrument(instrument);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }

        insertInstrumentOptions();

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
     * @param sample the sample
     */
    public void setSample(Sample sample) {
        try {
            prideObjectsFactory.addSample(sample);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }

        insertSampleOptions();

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
     * Sets the selected contact group.
     *
     * @param contactGroup the contact group
     */
    public void setContacts(ContactGroup contactGroup) {

        try {
            prideObjectsFactory.addContactGroup(contactGroup);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }

        insertContactOptions();

        int selectedContactIndex = 0;

        for (int i = 0; i < contactGroupsJComboBox.getItemCount(); i++) {
            if (((String) contactGroupsJComboBox.getItemAt(i)).equalsIgnoreCase(contactGroup.getName())) {
                selectedContactIndex = i;
            }
        }

        contactGroupsJComboBox.setSelectedIndex(selectedContactIndex);
        contactGroupsJComboBoxActionPerformed(null);
    }

    /**
     * Tries to extract the ontology from the given CV term. For example
     * BTO:0000763 returns BTO.
     *
     * @param cvTerm the CV term to extract the ontology from, e.g., BTO:0000763
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
     * @return true if the user has canceled the progress
     */
    public boolean progressCancelled() {
        return progressDialog.isRunCanceled();
    }

    /**
     * Resets the PRIDE object factory.
     */
    private void resetPrideObjectFactory() {
        try {
            prideObjectsFactory = PrideObjectsFactory.getInstance();
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
    }

    /**
     * Try to delete the given contact group.
     *
     * @param contactGroup the contact group to delete
     */
    public void deleteContactGroup(ContactGroup contactGroup) {
        prideObjectsFactory.deleteContactGroup(contactGroup);
        insertContactOptions();
        contactGroupsJComboBoxActionPerformed(null);
    }

    /**
     * Try to delete the given instrument.
     *
     * @param instrument the instrument to delete
     */
    public void deleteInstrument(Instrument instrument) {
        prideObjectsFactory.deleteInstrument(instrument);
        insertInstrumentOptions();
        instrumentJComboBoxActionPerformed(null);
    }

    /**
     * Try to delete the given sample.
     *
     * @param sample the sample to delete
     */
    public void deleteSample(Sample sample) {
        prideObjectsFactory.deleteSample(sample);
        insertSampleOptions();
        sampleJComboBoxActionPerformed(null);
    }

    /**
     * Try to delete the given protocol.
     *
     * @param protocol the protocol to delete
     */
    public void deleteProtocol(Protocol protocol) {
        prideObjectsFactory.deleteProtocol(protocol);
        insertProtocolOptions();
        protocolJComboBoxActionPerformed(null);
    }

    /**
     * Try to delete the given reference group.
     *
     * @param referenceGroup the reference group to delete
     */
    public void deleteReferenceGroup(ReferenceGroup referenceGroup) {
        prideObjectsFactory.deleteReferenceGroup(referenceGroup);
        insertReferenceOptions();
        referenceGroupsJComboBoxActionPerformed(null);
    }
}
