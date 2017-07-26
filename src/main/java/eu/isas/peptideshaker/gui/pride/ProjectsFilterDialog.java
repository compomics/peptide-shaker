package eu.isas.peptideshaker.gui.pride;

import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.table.TableRowSorter;

/**
 * A dialog displaying various filters that can be applied to the PRIDE project
 * table.
 *
 * @author Harald Barsnes
 */
public class ProjectsFilterDialog extends javax.swing.JDialog {

    /**
     * The projects table.
     */
    private JTable projectsTable;
    /**
     * The PrideReshakeGUI parent frame.
     */
    private PrideReshakeGUI prideReShakeGUI;
    /**
     * Counts the number of times the users has pressed a key on the keyboard in
     * the search field.
     */
    private int keyPressedCounter = 0;
    /**
     * The time to wait between keys typed before updating the search.
     */
    private int waitingTime = 1000;

    /**
     * Creates a new ProjectsFilterDialog.
     *
     * @param prideReShakeGUI the PrideReshakeGUI parent frame
     * @param modal if the dialog is modal or not
     * @param currentFilterValues the current filter text values
     * @param assaysBiggerThan if assay filter is bigger than or smaller than
     * @param visible if true the dialog is made visible
     * @param species the available species
     * @param tissues the available tissues
     * @param instruments the available instruments
     * @param ptms the available PTMs
     * @param projectTags the available project tags
     */
    public ProjectsFilterDialog(PrideReshakeGUI prideReShakeGUI, boolean modal, String[] currentFilterValues, boolean assaysBiggerThan, boolean visible,
            ArrayList<String> species, ArrayList<String> tissues, ArrayList<String> instruments, ArrayList<String> ptms, ArrayList<String> projectTags) {
        super(prideReShakeGUI, modal);

        this.prideReShakeGUI = prideReShakeGUI;
        projectsTable = prideReShakeGUI.getProjectsTable();

        initComponents();

        setUpGUI();

        speciesComboBox.setModel(new DefaultComboBoxModel(species.toArray()));
        tissuesComboBox.setModel(new DefaultComboBoxModel(tissues.toArray()));
        instrumentsComboBox.setModel(new DefaultComboBoxModel(instruments.toArray()));
        ptmsComboBox.setModel(new DefaultComboBoxModel(ptms.toArray()));
        tagsComboBox.setModel(new DefaultComboBoxModel(projectTags.toArray()));

        // update the filter properties
        int filterCounter = 0;
        accessionJTextField.setText(currentFilterValues[filterCounter++]);
        titleJTextField.setText(currentFilterValues[filterCounter]);
        if (currentFilterValues[++filterCounter] != null) {
            tagsComboBox.setSelectedItem(currentFilterValues[filterCounter]);
        }
        if (currentFilterValues[++filterCounter] != null) {
            speciesComboBox.setSelectedItem(currentFilterValues[filterCounter]);
        }
        if (currentFilterValues[++filterCounter] != null) {
            tissuesComboBox.setSelectedItem(currentFilterValues[filterCounter]);
        }
        if (currentFilterValues[++filterCounter] != null) {
            ptmsComboBox.setSelectedItem(currentFilterValues[filterCounter]);
        }
        if (currentFilterValues[++filterCounter] != null) {
            instrumentsComboBox.setSelectedItem(currentFilterValues[filterCounter]);
        }
        assaysJTextField.setText(currentFilterValues[++filterCounter]);
        if (currentFilterValues[++filterCounter] != null) {
            typeComboBox.setSelectedIndex(Integer.parseInt(currentFilterValues[filterCounter]));
        }
        if (currentFilterValues[++filterCounter] != null) {
            categoryComboBox.setSelectedIndex(Integer.parseInt(currentFilterValues[filterCounter]));
        }

        if (assaysBiggerThan) {
            assaysComboBox.setSelectedIndex(0);
        } else {
            assaysComboBox.setSelectedIndex(1);
        }

        setLocationRelativeTo(prideReShakeGUI);
        setVisible(visible);
    }

    /**
     * Set up the GUI
     */
    private void setUpGUI() {

        // set the padding for the combo boxes
        JComponent c = (JComponent) speciesComboBox.getRenderer();
        c.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 4));
        speciesComboBox.setRenderer((ListCellRenderer) c);
        ((JTextField) speciesComboBox.getEditor().getEditorComponent()).setMargin(new Insets(2, 10, 2, 2));

        c = (JComponent) tissuesComboBox.getRenderer();
        c.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 4));
        tissuesComboBox.setRenderer((ListCellRenderer) c);
        ((JTextField) tissuesComboBox.getEditor().getEditorComponent()).setMargin(new Insets(2, 10, 2, 2));

        c = (JComponent) ptmsComboBox.getRenderer();
        c.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 4));
        ptmsComboBox.setRenderer((ListCellRenderer) c);
        ((JTextField) ptmsComboBox.getEditor().getEditorComponent()).setMargin(new Insets(2, 10, 2, 2));

        c = (JComponent) tagsComboBox.getRenderer();
        c.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 4));
        tagsComboBox.setRenderer((ListCellRenderer) c);
        ((JTextField) tagsComboBox.getEditor().getEditorComponent()).setMargin(new Insets(2, 10, 2, 2));

        c = (JComponent) instrumentsComboBox.getRenderer();
        c.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 4));
        instrumentsComboBox.setRenderer((ListCellRenderer) c);
        ((JTextField) instrumentsComboBox.getEditor().getEditorComponent()).setMargin(new Insets(2, 10, 2, 2));

        c = (JComponent) typeComboBox.getRenderer();
        c.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 4));
        typeComboBox.setRenderer((ListCellRenderer) c);
        ((JTextField) typeComboBox.getEditor().getEditorComponent()).setMargin(new Insets(2, 10, 2, 2));

        // add key listeners for the combo boxes
        speciesComboBox.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent event) {
                filterInputGiven();
            }
        });

        assaysComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        typeComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        categoryComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
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
        filterPanel = new javax.swing.JPanel();
        accessionLabel = new javax.swing.JLabel();
        accessionJTextField = new javax.swing.JTextField();
        titleLabel = new javax.swing.JLabel();
        titleJTextField = new javax.swing.JTextField();
        speciesLabel = new javax.swing.JLabel();
        tissueLabel = new javax.swing.JLabel();
        assaysLabel = new javax.swing.JLabel();
        assaysJTextField = new javax.swing.JTextField();
        ptmsLabel = new javax.swing.JLabel();
        instrumentLabel = new javax.swing.JLabel();
        typeLabel = new javax.swing.JLabel();
        typeComboBox = new javax.swing.JComboBox();
        speciesComboBox = new javax.swing.JComboBox();
        tissuesComboBox = new javax.swing.JComboBox();
        ptmsComboBox = new javax.swing.JComboBox();
        instrumentsComboBox = new javax.swing.JComboBox();
        assaysComboBox = new javax.swing.JComboBox();
        tagsLabel = new javax.swing.JLabel();
        tagsComboBox = new javax.swing.JComboBox();
        categoryLabel = new javax.swing.JLabel();
        categoryComboBox = new javax.swing.JComboBox();
        clearJButton = new javax.swing.JButton();
        okJButton = new javax.swing.JButton();
        projectSearchLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Find");
        setMinimumSize(new java.awt.Dimension(400, 350));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        filterPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Project Filters"));
        filterPanel.setOpaque(false);

        accessionLabel.setText("Accession");

        accessionJTextField.setText("test");
        accessionJTextField.setMargin(new java.awt.Insets(2, 10, 2, 2));
        accessionJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                accessionJTextFieldKeyReleased(evt);
            }
        });

        titleLabel.setText("Title");

        titleJTextField.setMargin(new java.awt.Insets(2, 10, 2, 2));
        titleJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                titleJTextFieldKeyReleased(evt);
            }
        });

        speciesLabel.setText("Species");

        tissueLabel.setText("Tissue");

        assaysLabel.setText("#Assays");

        assaysJTextField.setMargin(new java.awt.Insets(2, 10, 2, 2));
        assaysJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                assaysJTextFieldKeyReleased(evt);
            }
        });

        ptmsLabel.setText("PTMs");

        instrumentLabel.setText("Instrument");

        typeLabel.setText("Type");

        typeComboBox.setMaximumRowCount(20);
        typeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "COMPLETE", "PARTIAL", "PRIDE", "All" }));
        typeComboBox.setSelectedIndex(3);
        typeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                typeComboBoxActionPerformed(evt);
            }
        });

        speciesComboBox.setEditable(true);
        speciesComboBox.setMaximumRowCount(20);
        speciesComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speciesComboBoxActionPerformed(evt);
            }
        });

        tissuesComboBox.setEditable(true);
        tissuesComboBox.setMaximumRowCount(20);
        tissuesComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tissuesComboBoxActionPerformed(evt);
            }
        });

        ptmsComboBox.setEditable(true);
        ptmsComboBox.setMaximumRowCount(20);
        ptmsComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ptmsComboBoxActionPerformed(evt);
            }
        });

        instrumentsComboBox.setEditable(true);
        instrumentsComboBox.setMaximumRowCount(20);
        instrumentsComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                instrumentsComboBoxActionPerformed(evt);
            }
        });

        assaysComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { ">", "<" }));

        tagsLabel.setText("Tags");

        tagsComboBox.setEditable(true);
        tagsComboBox.setMaximumRowCount(20);
        tagsComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { " ", "COMPLETE", "PARTIAL", "PRIDE" }));
        tagsComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tagsComboBoxActionPerformed(evt);
            }
        });

        categoryLabel.setText("Confidence");

        categoryComboBox.setMaximumRowCount(20);
        categoryComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Not yet classified", "High confidence", "Good confidence", "Moderate confidence", "Low confidence", "All" }));
        categoryComboBox.setSelectedIndex(5);
        categoryComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                categoryComboBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout filterPanelLayout = new javax.swing.GroupLayout(filterPanel);
        filterPanel.setLayout(filterPanelLayout);
        filterPanelLayout.setHorizontalGroup(
            filterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(filterPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(filterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(filterPanelLayout.createSequentialGroup()
                        .addGroup(filterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(accessionLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(tissueLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(ptmsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(instrumentLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(titleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(speciesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(assaysLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(typeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(filterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(filterPanelLayout.createSequentialGroup()
                                .addGap(2, 2, 2)
                                .addComponent(assaysComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(assaysJTextField))
                            .addComponent(tissuesComboBox, javax.swing.GroupLayout.Alignment.CENTER, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(ptmsComboBox, javax.swing.GroupLayout.Alignment.CENTER, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(instrumentsComboBox, javax.swing.GroupLayout.Alignment.CENTER, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(accessionJTextField)
                            .addComponent(titleJTextField)
                            .addComponent(speciesComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(typeComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, filterPanelLayout.createSequentialGroup()
                        .addComponent(tagsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tagsComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(filterPanelLayout.createSequentialGroup()
                        .addComponent(categoryLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(categoryComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        filterPanelLayout.setVerticalGroup(
            filterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(filterPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(filterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(accessionJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(accessionLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(filterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(titleJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(titleLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(filterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tagsLabel)
                    .addComponent(tagsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(filterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(speciesLabel)
                    .addComponent(speciesComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(filterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tissueLabel)
                    .addComponent(tissuesComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(filterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ptmsLabel)
                    .addComponent(ptmsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(filterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(instrumentLabel)
                    .addComponent(instrumentsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(filterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(assaysLabel)
                    .addComponent(assaysComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(assaysJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(filterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(typeLabel)
                    .addComponent(typeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(filterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(categoryLabel)
                    .addComponent(categoryComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        filterPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {accessionJTextField, assaysComboBox, assaysJTextField, categoryComboBox, instrumentsComboBox, ptmsComboBox, speciesComboBox, tagsComboBox, tissuesComboBox, titleJTextField, typeComboBox});

        clearJButton.setText("Clear");
        clearJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearJButtonActionPerformed(evt);
            }
        });

        okJButton.setText("OK");
        okJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okJButtonActionPerformed(evt);
            }
        });

        projectSearchLabel.setText("<html><a href=\\\"dummy\\\">Free Text Project Search</a></html>");
        projectSearchLabel.setToolTipText("Open Free Text Project Search");
        projectSearchLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                projectSearchLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                projectSearchLabelMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                projectSearchLabelMouseReleased(evt);
            }
        });

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(filterPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(projectSearchLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 329, Short.MAX_VALUE)
                        .addComponent(okJButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearJButton)))
                .addContainerGap())
        );

        backgroundPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {clearJButton, okJButton});

        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(filterPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(clearJButton)
                    .addComponent(okJButton)
                    .addComponent(projectSearchLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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
     * Clears all filters.
     *
     * @param evt
     */
    private void clearJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearJButtonActionPerformed
        accessionJTextField.setText("");
        titleJTextField.setText("");
        tagsComboBox.setSelectedIndex(0);
        speciesComboBox.setSelectedIndex(0);
        tissuesComboBox.setSelectedIndex(0);
        ptmsComboBox.setSelectedIndex(0);
        instrumentsComboBox.setSelectedIndex(0);
        assaysJTextField.setText("");
        typeComboBox.setSelectedIndex(3);
        categoryComboBox.setSelectedIndex(5);
        filter();
    }//GEN-LAST:event_clearJButtonActionPerformed

    /**
     * Filters the projects table according to the current filter settings.
     *
     * @param evt
     */
    private void accessionJTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_accessionJTextFieldKeyReleased
        filterInputGiven();
    }//GEN-LAST:event_accessionJTextFieldKeyReleased

    /**
     * Filters the projects table according to the current filter settings.
     *
     * @param evt
     */
    private void assaysJTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_assaysJTextFieldKeyReleased
        filterInputGiven();
    }//GEN-LAST:event_assaysJTextFieldKeyReleased

    /**
     * Saves the filter settings and closes the dialog.
     *
     * @param evt
     */
    private void okJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okJButtonActionPerformed

        // store the current filter values
        String[] currentFilterValues = new String[10];
        currentFilterValues[0] = accessionJTextField.getText();
        currentFilterValues[1] = titleJTextField.getText();

        if (!((JTextField) tagsComboBox.getEditor().getEditorComponent()).getText().trim().isEmpty()) {
            currentFilterValues[2] = ((JTextField) tagsComboBox.getEditor().getEditorComponent()).getText().trim();
        }
        if (!((JTextField) speciesComboBox.getEditor().getEditorComponent()).getText().trim().isEmpty()) {
            currentFilterValues[3] = ((JTextField) speciesComboBox.getEditor().getEditorComponent()).getText().trim();
        }
        if (!((JTextField) tissuesComboBox.getEditor().getEditorComponent()).getText().trim().isEmpty()) {
            currentFilterValues[4] = ((JTextField) tissuesComboBox.getEditor().getEditorComponent()).getText().trim();
        }
        if (!((JTextField) ptmsComboBox.getEditor().getEditorComponent()).getText().trim().isEmpty()) {
            currentFilterValues[5] = ((JTextField) ptmsComboBox.getEditor().getEditorComponent()).getText().trim();
        }
        if (!((JTextField) instrumentsComboBox.getEditor().getEditorComponent()).getText().trim().isEmpty()) {
            currentFilterValues[6] = ((JTextField) instrumentsComboBox.getEditor().getEditorComponent()).getText().trim();
        }

        currentFilterValues[7] = assaysJTextField.getText();

        currentFilterValues[8] = "" + typeComboBox.getSelectedIndex();
        currentFilterValues[9] = "" + categoryComboBox.getSelectedIndex();

        prideReShakeGUI.setCurrentFilterValues(currentFilterValues, assaysComboBox.getSelectedIndex() == 0);

        // close the dialog
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_okJButtonActionPerformed

    /**
     * Closes the dialog.
     *
     * @param evt
     */
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        okJButtonActionPerformed(null);
    }//GEN-LAST:event_formWindowClosing

    /**
     * Filters the projects table according to the current filter settings.
     *
     * @param evt
     */
    private void titleJTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_titleJTextFieldKeyReleased
        filterInputGiven();
    }//GEN-LAST:event_titleJTextFieldKeyReleased

    /**
     * Filters the projects table according to the current filter settings.
     *
     * @param evt
     */
    private void typeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_typeComboBoxActionPerformed
        filter();
    }//GEN-LAST:event_typeComboBoxActionPerformed

    /**
     * Filters the projects table according to the current filter settings.
     *
     * @param evt
     */
    private void speciesComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_speciesComboBoxActionPerformed
        filter();
    }//GEN-LAST:event_speciesComboBoxActionPerformed

    /**
     * Filters the projects table according to the current filter settings.
     *
     * @param evt
     */
    private void tissuesComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tissuesComboBoxActionPerformed
        filter();
    }//GEN-LAST:event_tissuesComboBoxActionPerformed

    /**
     * Filters the projects table according to the current filter settings.
     *
     * @param evt
     */
    private void ptmsComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ptmsComboBoxActionPerformed
        filter();
    }//GEN-LAST:event_ptmsComboBoxActionPerformed

    /**
     * Filters the projects table according to the current filter settings.
     *
     * @param evt
     */
    private void instrumentsComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_instrumentsComboBoxActionPerformed
        filter();
    }//GEN-LAST:event_instrumentsComboBoxActionPerformed

    /**
     * Filters the projects table according to the current filter settings.
     *
     * @param evt
     */
    private void tagsComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tagsComboBoxActionPerformed
        filter();
    }//GEN-LAST:event_tagsComboBoxActionPerformed

    /**
     * Change the cursor into a hand cursor.
     *
     * @param evt
     */
    private void projectSearchLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_projectSearchLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_projectSearchLabelMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void projectSearchLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_projectSearchLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_projectSearchLabelMouseExited

    /**
     * Open the PRIDE web page search.
     *
     * @param evt
     */
    private void projectSearchLabelMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_projectSearchLabelMouseReleased
        new PrideFreeTextSearchDialog(this, true);
    }//GEN-LAST:event_projectSearchLabelMouseReleased

    /**
     * Filters the projects table according to the current filter settings.
     *
     * @param evt
     */
    private void categoryComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_categoryComboBoxActionPerformed
        filter();
    }//GEN-LAST:event_categoryComboBoxActionPerformed

    /**
     * Make sure that the user is finished typing before applying the filters.
     */
    private void filterInputGiven() {

        keyPressedCounter++;

        new Thread("FilterThread") {
            @Override
            public synchronized void run() {

                try {
                    wait(waitingTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    // see if the gui is to be updated or not
                    if (keyPressedCounter == 1) {
                        filter();

                        // gui updated, reset the counter
                        keyPressedCounter = 0;
                    } else {
                        // gui not updated, decrease the counter
                        keyPressedCounter--;
                    }
                } catch (Exception e) {
                    prideReShakeGUI.getPeptideShakerGUI().catchException(e);
                }
            }
        }.start();
    }

    /**
     * Filters the projects table according to the current filter settings.
     */
    public void filter() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        List<RowFilter<Object, Object>> filters = new ArrayList<>();

        // project accession filter
        String text = accessionJTextField.getText();

        if (text == null || text.length() == 0) {
            filters.add(RowFilter.regexFilter(".*"));
        } else {
            try {
                filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), projectsTable.getColumn("Accession").getModelIndex()));
            } catch (PatternSyntaxException pse) {
                //JOptionPane.showMessageDialog(this, "Bad regex pattern for accession!", "Filter Error", JOptionPane.ERROR_MESSAGE);
                //pse.printStackTrace();
            }
        }

        // project title filter
        text = titleJTextField.getText();

        if (text == null || text.length() == 0) {
            filters.add(RowFilter.regexFilter(".*"));
        } else {
            try {
                filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), projectsTable.getColumn("Title").getModelIndex()));
            } catch (PatternSyntaxException pse) {
                //JOptionPane.showMessageDialog(this, "Bad regex pattern for title!", "Filter Error", JOptionPane.ERROR_MESSAGE);
                //pse.printStackTrace();
            }
        }

        // project tags filter
        if (((JTextField) tagsComboBox.getEditor().getEditorComponent()).getText().trim().length() > 0) {

            text = ((JTextField) tagsComboBox.getEditor().getEditorComponent()).getText().trim();

            if (text == null || text.length() == 0) {
                filters.add(RowFilter.regexFilter(".*"));
            } else {
                try {
                    filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), projectsTable.getColumn("Tags").getModelIndex()));
                } catch (PatternSyntaxException pse) {
                    //JOptionPane.showMessageDialog(this, "Bad regex pattern for tags!", "Filter Error", JOptionPane.ERROR_MESSAGE);
                    //pse.printStackTrace();
                }
            }
        }

        // project type filter
        if (((JTextField) typeComboBox.getEditor().getEditorComponent()).getText().trim().length() > 0) {

            text = ((JTextField) typeComboBox.getEditor().getEditorComponent()).getText().trim();

            if (text == null || text.length() == 0) {
                filters.add(RowFilter.regexFilter(".*"));
            } else {
                try {
                    filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), projectsTable.getColumn("Type").getModelIndex()));
                } catch (PatternSyntaxException pse) {
                    //JOptionPane.showMessageDialog(this, "Bad regex pattern for type!", "Filter Error", JOptionPane.ERROR_MESSAGE);
                    //pse.printStackTrace();
                }
            }
        }

        // project category filter
        int selectedIndex = categoryComboBox.getSelectedIndex();

        if (selectedIndex == 5) {
            filters.add(RowFilter.regexFilter(".*"));
        } else {
            try {
                filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote("" + selectedIndex), projectsTable.getColumn("  ").getModelIndex()));
            } catch (PatternSyntaxException pse) {
                //JOptionPane.showMessageDialog(this, "Bad regex pattern for type!", "Filter Error", JOptionPane.ERROR_MESSAGE);
                //pse.printStackTrace();
            }
        }

        // number of assays filter
        if (assaysJTextField.getText().length() > 0) {

            try {
                Integer value = new Integer(assaysJTextField.getText());

                if (assaysComboBox.getSelectedIndex() == 0) {
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, value, projectsTable.getColumn("#Assays").getModelIndex()));
                } else {
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.BEFORE, value, projectsTable.getColumn("#Assays").getModelIndex()));
                }
            } catch (NumberFormatException e) {
                //JOptionPane.showMessageDialog(this, "Assay count has to be an integer!", "Filter Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // species filter
        if (((JTextField) speciesComboBox.getEditor().getEditorComponent()).getText().trim().length() > 0) {

            text = ((JTextField) speciesComboBox.getEditor().getEditorComponent()).getText().trim();

            if (text == null || text.length() == 0) {
                filters.add(RowFilter.regexFilter(".*"));
            } else {
                try {
                    filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), projectsTable.getColumn("Species").getModelIndex()));
                } catch (PatternSyntaxException pse) {
                    //JOptionPane.showMessageDialog(this, "Bad regex pattern for species!", "Filter Error", JOptionPane.ERROR_MESSAGE);
                    //pse.printStackTrace();
                }
            }
        }

        // tissues filter
        if (((JTextField) tissuesComboBox.getEditor().getEditorComponent()).getText().trim().length() > 0) {

            text = ((JTextField) tissuesComboBox.getEditor().getEditorComponent()).getText().trim();

            if (text == null || text.length() == 0) {
                filters.add(RowFilter.regexFilter(".*"));
            } else {
                try {
                    filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), projectsTable.getColumn("Tissues").getModelIndex()));
                } catch (PatternSyntaxException pse) {
                    //JOptionPane.showMessageDialog(this, "Bad regex pattern for tissues!", "Filter Error", JOptionPane.ERROR_MESSAGE);
                    //pse.printStackTrace();
                }
            }
        }

        // instruments filter
        if (((JTextField) instrumentsComboBox.getEditor().getEditorComponent()).getText().trim().length() > 0) {

            text = ((JTextField) instrumentsComboBox.getEditor().getEditorComponent()).getText().trim();

            if (text == null || text.length() == 0) {
                filters.add(RowFilter.regexFilter(".*"));
            } else {
                try {
                    filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), projectsTable.getColumn("Instruments").getModelIndex()));
                } catch (PatternSyntaxException pse) {
                    //JOptionPane.showMessageDialog(this, "Bad regex pattern for instruments!", "Filter Error", JOptionPane.ERROR_MESSAGE);
                    //pse.printStackTrace();
                }
            }
        }

        // ptm filter
        if (((JTextField) ptmsComboBox.getEditor().getEditorComponent()).getText().trim().length() > 0) {

            text = ((JTextField) ptmsComboBox.getEditor().getEditorComponent()).getText().trim();

            if (text == null || text.length() == 0) {
                filters.add(RowFilter.regexFilter(".*"));
            } else {
                try {
                    filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), projectsTable.getColumn("PTMs").getModelIndex()));
                } catch (PatternSyntaxException pse) {
                    //JOptionPane.showMessageDialog(this, "Bad regex pattern for PTMs!", "Filter Error", JOptionPane.ERROR_MESSAGE);
                    //pse.printStackTrace();
                }
            }
        }

        RowFilter<Object, Object> allFilters = RowFilter.andFilter(filters);

        if (projectsTable.getRowSorter() != null) {
            ((TableRowSorter) projectsTable.getRowSorter()).setRowFilter(allFilters);

            if (projectsTable.getRowCount() > 0) {
                projectsTable.setRowSelectionInterval(0, 0);
            }

            // see if any filters were actually added
            if (!accessionJTextField.getText().trim().isEmpty()
                    || !titleJTextField.getText().trim().isEmpty()
                    || !((JTextField) typeComboBox.getEditor().getEditorComponent()).getText().trim().isEmpty()
                    || !assaysJTextField.getText().trim().isEmpty()
                    || !((JTextField) speciesComboBox.getEditor().getEditorComponent()).getText().trim().isEmpty()
                    || !((JTextField) tissuesComboBox.getEditor().getEditorComponent()).getText().trim().isEmpty()
                    || !((JTextField) instrumentsComboBox.getEditor().getEditorComponent()).getText().trim().isEmpty()
                    || !((JTextField) ptmsComboBox.getEditor().getEditorComponent()).getText().trim().isEmpty()) {
                prideReShakeGUI.showProjectFilterRemovalOption(true);
            } else {
                prideReShakeGUI.showProjectFilterRemovalOption(false);
            }

            prideReShakeGUI.updateProjectTableSelection();
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField accessionJTextField;
    private javax.swing.JLabel accessionLabel;
    private javax.swing.JComboBox assaysComboBox;
    private javax.swing.JTextField assaysJTextField;
    private javax.swing.JLabel assaysLabel;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JComboBox categoryComboBox;
    private javax.swing.JLabel categoryLabel;
    private javax.swing.JButton clearJButton;
    private javax.swing.JPanel filterPanel;
    private javax.swing.JLabel instrumentLabel;
    private javax.swing.JComboBox instrumentsComboBox;
    private javax.swing.JButton okJButton;
    private javax.swing.JLabel projectSearchLabel;
    private javax.swing.JComboBox ptmsComboBox;
    private javax.swing.JLabel ptmsLabel;
    private javax.swing.JComboBox speciesComboBox;
    private javax.swing.JLabel speciesLabel;
    private javax.swing.JComboBox tagsComboBox;
    private javax.swing.JLabel tagsLabel;
    private javax.swing.JLabel tissueLabel;
    private javax.swing.JComboBox tissuesComboBox;
    private javax.swing.JTextField titleJTextField;
    private javax.swing.JLabel titleLabel;
    private javax.swing.JComboBox typeComboBox;
    private javax.swing.JLabel typeLabel;
    // End of variables declaration//GEN-END:variables
}
