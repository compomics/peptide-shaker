package eu.isas.peptideshaker.gui;

import com.compomics.util.experiment.biology.aminoacids.AminoAcid;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import static com.compomics.util.experiment.personalization.ExperimentObject.NO_KEY;
import com.compomics.util.threading.SimpleSemaphore;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A Jump To panel for use in the menu bar in the main frame.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class JumpToPanel extends javax.swing.JPanel {

    /**
     * Enum of the types of data to jump to.
     */
    public enum JumpType {

        protein, peptide, psm
    }
    /**
     * The type of data to jump to in that panel.
     */
    private JumpType selectedJumpType = JumpType.protein;
    /**
     * Instance of the main GUI class.
     */
    private final PeptideShakerGUI peptideShakerGUI;
    /**
     * Items matching the criterion for each type. Keys are in an array:
     * protein, peptide, PSM.
     */
    private final EnumMap<JumpType, ArrayList<Object[]>> possibilities;
    /**
     * Currently selected item.
     */
    private final EnumMap<JumpType, Integer> currentSelection = new EnumMap<>(JumpType.class);
    /**
     * The text to display by default.
     */
    private final EnumMap<JumpType, String> lastInput = new EnumMap<>(JumpType.class);
    /**
     * The text to display by default.
     */
    private final EnumMap<JumpType, String> lastLabel = new EnumMap<>(JumpType.class);
    /**
     * The text to display by default.
     */
    private final EnumMap<JumpType, String> welcomeText;
    /**
     * The spectrum file inspected when jumping to spectra.
     */
    private String spectrumfile;
    /**
     * Counts the number of times the users has pressed a key on the keyboard in
     * the search field.
     */
    private boolean newInput = false;
    /**
     * Semaphore for the synchronization of threads.
     */
    private final SimpleSemaphore searchMutex = new SimpleSemaphore(1, true);
    /**
     * Splitter between spectrum file and spectrum title.
     */
    private final String SPECTRUM_FILENAME_SPLITTER = "_spectrum_file_name_splitter_";

    /**
     * Creates a new JumpToPanel.
     *
     * @param peptideShakerGUI the parent
     */
    public JumpToPanel(
            PeptideShakerGUI peptideShakerGUI
    ) {

        initComponents();

        this.peptideShakerGUI = peptideShakerGUI;

        welcomeText = new EnumMap<>(JumpType.class);
        welcomeText.put(JumpType.protein, "(protein, peptide, or spectrum)");
        welcomeText.put(JumpType.peptide, "(protein, peptide, or spectrum)");
        welcomeText.put(JumpType.psm, "(spectrum title, precursor m/z or RT)");
        inputTxt.setText(welcomeText.get(selectedJumpType));
        indexLabel.setText("");
        previousButton.setEnabled(false);
        nextButton.setEnabled(false);

        possibilities = new EnumMap<>(JumpType.class);

        for (JumpType jumpType : JumpType.values()) {

            possibilities.put(jumpType, new ArrayList<>());

        }
    }

    /**
     * Move the focus to the Jump To text field and select all the content.
     */
    public void selectTextField() {
        inputTxt.requestFocus();
        inputTxt.selectAll();
    }

    /**
     * Set the color for the hits.
     *
     * @param color the color
     */
    public void setColor(Color color) {
        indexLabel.setForeground(color);
    }

    /**
     * Updates the item selection in the selected tab.
     */
    public void updateSelectionInTab() {

        indexLabel.setForeground(Color.BLACK);
        int selectedIndex = currentSelection.get(selectedJumpType);
        ArrayList<Object[]> keys = possibilities.get(selectedJumpType);
        Object[] selection = keys.get(selectedIndex);

        String spectrumFile = null;
        String spectrumTitle = null;

        if (selection[2] instanceof String) {

            String psmKey = (String) selection[2];

            // parse file and title
            String[] elements = psmKey.split(SPECTRUM_FILENAME_SPLITTER);
            spectrumFile = elements[0];
            spectrumTitle = elements[1];
        }

        peptideShakerGUI.setSelectedItems(
                (Long) selection[0],
                (Long) selection[1],
                spectrumFile,
                spectrumTitle
        );

        peptideShakerGUI.updateSelectionInCurrentTab();

        String label = String.join(
                "",
                "(",
                Integer.toString(selectedIndex + 1),
                " of ",
                Integer.toString(keys.size()),
                ")"
        );

        indexLabel.setText(label);
        lastLabel.put(selectedJumpType, label);

    }

    /**
     * Returns a list of descriptions corresponding to every item matching the
     * search.
     *
     * @return a list of descriptions
     */
    public String[] getPossibilitiesDescriptions() {

        return possibilities.get(selectedJumpType).stream()
                .map(
                        keys -> getItemsDescription(keys)
                )
                .toArray(String[]::new);
    }

    /**
     * Returns the description of an item.
     *
     * @param key the key of the item
     *
     * @return the description of an item
     */
    private Object getItemsDescription(
            Object[] keys
    ) {

        StringBuilder sb = new StringBuilder();

        if ((Long) keys[0] != NO_KEY) {

            sb.append(getProteinDescription((Long) keys[0]));

        }
        if ((Long) keys[1] != NO_KEY) {

            if (sb.length() > 0) {

                sb.append(" | ");

            }

            sb.append(getPeptideDescription((Long) keys[1]));

        }
        if (keys[2] instanceof String) {

            if (sb.length() > 0) {

                sb.append(" | ");

            }

            sb.append(getSpectrumDescription((String) keys[2]));

        }

        return sb.toString();

    }

    /**
     * Returns the description of a protein.
     *
     * @param key the key of the protein
     *
     * @return the description of a protein
     */
    private String getProteinDescription(
            long key
    ) {

        Identification identification = peptideShakerGUI.getIdentification();
        ProteinDetailsProvider proteinDetailsProvider = peptideShakerGUI.getProteinDetailsProvider();

        ProteinMatch proteinMatch = identification.getProteinMatch(key);
        String mainMatch = proteinMatch.getLeadingAccession();
        String description = proteinDetailsProvider.getSimpleDescription(mainMatch);
        return String.join(
                " - ",
                String.join(
                        ",",
                        proteinMatch.getAccessions()
                ),
                description
        );
    }

    /**
     * Returns the description of a peptide.
     *
     * @param key the key of the peptide
     *
     * @return the description of a peptide
     */
    private String getPeptideDescription(long key) {

        Identification identification = peptideShakerGUI.getIdentification();

        PeptideMatch peptideMatch = identification.getPeptideMatch(key);
        return peptideShakerGUI.getDisplayFeaturesGenerator()
                .getTaggedPeptideSequence(
                        peptideMatch,
                        true,
                        true,
                        true
                );
    }

    /**
     * Returns the description of a spectrum.
     *
     * @param key the key of the spectrum
     *
     * @return the description of a spectrum
     */
    private String getSpectrumDescription(
            String key
    ) {

        String[] elements = key.split(SPECTRUM_FILENAME_SPLITTER);

        return String.join(
                "",
                elements[1],
                " (",
                elements[0],
                ")"
        );
    }

    /**
     * Returns the index of the selected item.
     *
     * @return the index of the selected item
     */
    public int getIndexOfSelectedItem() {

        return currentSelection.get(selectedJumpType);

    }

    /**
     * Sets the index of the selected item. Note: this does not update the
     * selection in tab and the GUI (see updateSelectionInTab()).
     *
     * @param itemIndex the item index
     */
    public void setSelectedItem(
            int itemIndex
    ) {

        currentSelection.put(selectedJumpType, itemIndex);

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        findJLabel = new javax.swing.JLabel();
        inputTxt = new javax.swing.JTextField();
        previousButton = new javax.swing.JButton();
        nextButton = new javax.swing.JButton();
        indexLabel = new javax.swing.JLabel();

        setOpaque(false);

        findJLabel.setText("Find");

        inputTxt.setForeground(new java.awt.Color(204, 204, 204));
        inputTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        inputTxt.setText("(peptide or protein)");
        inputTxt.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        inputTxt.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                inputTxtMouseReleased(evt);
            }
        });
        inputTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                inputTxtKeyReleased(evt);
            }
        });

        previousButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/previous_grey.png"))); // NOI18N
        previousButton.setToolTipText("Previous");
        previousButton.setBorder(null);
        previousButton.setBorderPainted(false);
        previousButton.setContentAreaFilled(false);
        previousButton.setIconTextGap(0);
        previousButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/previous.png"))); // NOI18N
        previousButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                previousButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                previousButtonMouseExited(evt);
            }
        });
        previousButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                previousButtonActionPerformed(evt);
            }
        });

        nextButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/next_grey.png"))); // NOI18N
        nextButton.setToolTipText("Next");
        nextButton.setBorderPainted(false);
        nextButton.setContentAreaFilled(false);
        nextButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/next.png"))); // NOI18N
        nextButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                nextButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                nextButtonMouseExited(evt);
            }
        });
        nextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextButtonActionPerformed(evt);
            }
        });

        indexLabel.setFont(indexLabel.getFont().deriveFont((indexLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        indexLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        indexLabel.setText(" ");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(findJLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(inputTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 205, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(previousButton, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(nextButton, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(indexLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {nextButton, previousButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(findJLabel)
                    .addComponent(inputTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(indexLabel)
                    .addComponent(previousButton, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(nextButton, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Update the jump to filter.
     *
     * @param evt the key event
     */
    private void inputTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_inputTxtKeyReleased

        indexLabel.setText("Typing...");

        final KeyEvent event = evt;

        newInput = true;

        new Thread("FindThread") {
            @Override
            public synchronized void run() {

                try {

                    searchMutex.acquire();
                    newInput = false;

                    try {
                        wait(500);
                    } catch (InterruptedException e) {
                        // Ignore
                    }

                    Identification identification = peptideShakerGUI.getIdentification();

                    if (identification != null && !newInput) {

                        indexLabel.setText("Searching...");

                        if (!inputTxt.getText().equalsIgnoreCase(welcomeText.get(selectedJumpType))) {

                            inputTxt.setForeground(Color.black);

                        } else {

                            inputTxt.setForeground(new Color(204, 204, 204));

                        }

                        if (event.getKeyCode() == KeyEvent.VK_UP && previousButton.isEnabled()) {

                            previousButtonActionPerformed(null);

                        } else if (event.getKeyCode() == KeyEvent.VK_DOWN && nextButton.isEnabled()) {

                            nextButtonActionPerformed(null);

                        } else {

                            possibilities.get(selectedJumpType).clear();
                            currentSelection.put(selectedJumpType, 0);

                            EnumMap<JumpType, Boolean> reinitializedMap = new EnumMap<>(JumpType.class);

                            for (JumpType jumpType : JumpType.values()) {

                                reinitializedMap.put(jumpType, jumpType == selectedJumpType);

                            }

                            String doubleString,
                                    inputLowerCase = inputTxt.getText().trim().toLowerCase(),
                                    inputUpperCase = inputLowerCase.toUpperCase();

                            if (!inputLowerCase.equals("")) {

                                peptideShakerGUI.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                                inputTxt.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

                                boolean proteinFound = false;
                                boolean peptidefound = false;

                                if (selectedJumpType == JumpType.protein || selectedJumpType == JumpType.peptide) {

                                    // See if the input is contained by a protein accession or description
                                    TreeSet<Long> proteinKeysFound = new TreeSet<>();
                                    TreeSet<Long> peptideKeysFound = new TreeSet<>();

                                    for (long proteinKey : peptideShakerGUI.getIdentificationFeaturesGenerator().getProcessedProteinKeys(null, peptideShakerGUI.getFilterParameters())) {

                                        if (newInput) {

                                            peptideShakerGUI.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                                            inputTxt.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                                            inputTxt.requestFocus();

                                            return;
                                        }

                                        ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);

                                        if (!proteinMatch.isDecoy()) {

                                            if (Arrays.stream(proteinMatch.getAccessions())
                                                    .map(accession -> accession.toLowerCase())
                                                    .anyMatch(accession -> accession.contains(inputLowerCase))
                                                    || Arrays.stream(proteinMatch.getAccessions())
                                                            .map(accession -> peptideShakerGUI.getProteinDetailsProvider().getDescription(accession))
                                                            .map(description -> description.toLowerCase())
                                                            .anyMatch(description -> description.contains(inputLowerCase))) {

                                                proteinFound = true;

                                                proteinKeysFound.add(proteinKey);

                                                for (long peptideKey : proteinMatch.getPeptideMatchesKeys()) {

                                                    peptideKeysFound.add(peptideKey);

                                                }
                                            }
                                        }
                                    }

                                    if (proteinFound) {

                                        lastInput.put(JumpType.protein, inputLowerCase);

                                        if (!reinitializedMap.get(JumpType.protein)) {

                                            possibilities.get(JumpType.protein).clear();
                                            currentSelection.put(JumpType.protein, 0);

                                        }

                                        for (long proteinKey : proteinKeysFound) {

                                            Object[] keys = new Object[3];
                                            Arrays.fill(keys, NO_KEY);
                                            keys[0] = proteinKey;

                                            possibilities.get(JumpType.protein).add(keys);

                                        }

                                        lastInput.put(JumpType.peptide, inputLowerCase);

                                        if (!reinitializedMap.get(JumpType.peptide)) {

                                            possibilities.get(JumpType.peptide).clear();
                                            currentSelection.put(JumpType.peptide, 0);

                                        }

                                        for (long peptideKey : peptideKeysFound) {

                                            Object[] keys = new Object[3];
                                            Arrays.fill(keys, NO_KEY);
                                            keys[1] = peptideKey;

                                            possibilities.get(JumpType.peptide).add(keys);

                                        }

                                    } else {

                                        // See if the input is contained by a peptide sequence or is a modification                                        
                                        boolean validPeptideSequence = inputUpperCase.chars()
                                                .allMatch(aa -> AminoAcid.isAa(aa));
                                        boolean possibleMod = peptideShakerGUI.getIdentificationParameters().getSearchParameters().getModificationParameters().getAllModifications().stream()
                                                .anyMatch(modName -> modName.toLowerCase().contains(inputLowerCase));

                                        if (validPeptideSequence || possibleMod) {

                                            String matchingInput = AminoAcid.getMatchingSequence(inputUpperCase, peptideShakerGUI.getIdentificationParameters().getSequenceMatchingParameters());

                                            TreeMap<Long, TreeSet<Long>> sequencesMatchesMap = new TreeMap<>();
                                            TreeMap<Long, TreeSet<Long>> modificationsMatchesMap = new TreeMap<>();

                                            for (long peptideKey : identification.getPeptideIdentification()) {

                                                if (newInput) {

                                                    peptideShakerGUI.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                                                    inputTxt.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                                                    inputTxt.requestFocus();

                                                    return;
                                                }

                                                PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);

                                                PSParameter psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);

                                                if (!psParameter.getHidden()) {

                                                    Peptide peptide = peptideMatch.getPeptide();
                                                    String matchingSequence = AminoAcid.getMatchingSequence(peptide.getSequence(), peptideShakerGUI.getIdentificationParameters().getSequenceMatchingParameters());

                                                    boolean sequenceMatch = validPeptideSequence && matchingSequence.contains(matchingInput);
                                                    boolean modMatch = possibleMod && Arrays.stream(peptideMatch.getPeptide().getVariableModifications())
                                                            .map(ModificationMatch::getModification)
                                                            .anyMatch(modName -> modName.contains(matchingInput));

                                                    if (sequenceMatch || modMatch) {

                                                        peptidefound = true;

                                                        TreeSet<Long> proteinKeys = identification.getProteinMatches(peptideKey);

                                                        for (long proteinKey : proteinKeys) {

                                                            ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);

                                                            if (!proteinMatch.isDecoy()) {

                                                                if (sequenceMatch) {

                                                                    TreeSet<Long> peptideKeys = sequencesMatchesMap.get(proteinKey);

                                                                    if (peptideKeys == null) {

                                                                        peptideKeys = new TreeSet<>();
                                                                        sequencesMatchesMap.put(proteinKey, peptideKeys);

                                                                    }

                                                                    peptideKeys.add(peptideKey);

                                                                }
                                                                if (modMatch) {

                                                                    TreeSet<Long> peptideKeys = modificationsMatchesMap.get(proteinKey);

                                                                    if (peptideKeys == null) {

                                                                        peptideKeys = new TreeSet<>();
                                                                        modificationsMatchesMap.put(proteinKey, peptideKeys);

                                                                    }

                                                                    peptideKeys.add(peptideKey);

                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            TreeMap<Long, TreeSet<Long>> itemsMap = !modificationsMatchesMap.isEmpty() ? modificationsMatchesMap : sequencesMatchesMap;

                                            lastInput.put(JumpType.protein, inputLowerCase);

                                            if (!reinitializedMap.get(JumpType.protein)) {

                                                possibilities.get(JumpType.protein).clear();
                                                currentSelection.put(JumpType.protein, 0);

                                            }

                                            for (Entry<Long, TreeSet<Long>> entry : itemsMap.entrySet()) {

                                                long proteinKey = entry.getKey();

                                                for (long peptideKey : entry.getValue()) {

                                                    Object[] keys = new Object[3];
                                                    Arrays.fill(keys, NO_KEY);
                                                    keys[0] = proteinKey;
                                                    keys[1] = peptideKey;

                                                    possibilities.get(JumpType.protein).add(keys);

                                                }
                                            }

                                            lastInput.put(JumpType.peptide, inputLowerCase);

                                            if (!reinitializedMap.get(JumpType.peptide)) {

                                                possibilities.get(JumpType.peptide).clear();
                                                currentSelection.put(JumpType.peptide, 0);

                                            }

                                            itemsMap.values().stream()
                                                    .flatMapToLong(
                                                            set -> set.stream().mapToLong(a -> a)
                                                    )
                                                    .distinct()
                                                    .sorted()
                                                    .forEach(
                                                            peptideKey -> {

                                                                Object[] keys = new Object[3];
                                                                Arrays.fill(keys, NO_KEY);
                                                                keys[1] = peptideKey;

                                                                possibilities.get(JumpType.peptide).add(keys);

                                                            }
                                                    );
                                        }
                                    }
                                }

                                if (selectedJumpType == JumpType.psm || !proteinFound && !peptidefound) {

                                    // See if the input is contained by a spectrum title or corresponds to a precursor mass or RT
                                    TreeSet<String> spectrumTitles = new TreeSet<>();

                                    SpectrumProvider spectrumProvider = peptideShakerGUI.getSpectrumProvider();

                                    String[] spectrumFiles = spectrumfile == null ? spectrumProvider.getFileNames() : new String[]{spectrumfile};

                                    for (String fileName : spectrumFiles) {
                                        for (String spectrumTitle : spectrumProvider.getSpectrumTitles(fileName)) {

                                            if (newInput) {

                                                peptideShakerGUI.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                                                inputTxt.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                                                inputTxt.requestFocus();

                                                return;
                                            }

                                            if (spectrumTitle.toLowerCase().contains(inputLowerCase)
                                                    || fileName.toLowerCase().contains(inputLowerCase)) {

                                                spectrumTitles.add(fileName + SPECTRUM_FILENAME_SPLITTER + spectrumTitle);

                                            } else {

                                                double precusorMz = spectrumProvider.getPrecursorMz(
                                                        fileName,
                                                        spectrumTitle
                                                );
                                                doubleString = Double.toString(precusorMz);

                                                if (doubleString.startsWith(inputLowerCase)) {

                                                    spectrumTitles.add(fileName + SPECTRUM_FILENAME_SPLITTER + spectrumTitle);

                                                } else {

                                                    double precusorRt = spectrumProvider.getPrecursorRt(
                                                            fileName,
                                                            spectrumTitle
                                                    );
                                                    doubleString = Double.toString(precusorRt);
                                                    if (doubleString.startsWith(inputLowerCase)) {

                                                        spectrumTitles.add(fileName + SPECTRUM_FILENAME_SPLITTER + spectrumTitle);

                                                    }
                                                }
                                            }
                                        }

                                        if (!spectrumTitles.isEmpty()) {

                                            for (JumpType jumpType : JumpType.values()) {

                                                ArrayList<Object[]> currentPossibilities = possibilities.get(jumpType);
                                                Object[] sample = currentPossibilities.isEmpty()
                                                        ? new Object[]{NO_KEY, NO_KEY, NO_KEY}
                                                        : currentPossibilities.get(0);

                                                if (jumpType == JumpType.psm || ((Long) sample[0] == NO_KEY && (Long) sample[1] == NO_KEY)) {

                                                    if (!reinitializedMap.get(jumpType)) {

                                                        possibilities.get(jumpType).clear();
                                                        currentSelection.put(jumpType, 0);

                                                    }

                                                    for (String spectrumTitle : spectrumTitles) {

                                                        Object[] keys = new Object[3];
                                                        Arrays.fill(keys, NO_KEY);
                                                        keys[2] = spectrumTitle;

                                                        possibilities.get(jumpType).add(keys);

                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (possibilities.get(selectedJumpType).size() > 0) {

                                    if (possibilities.get(selectedJumpType).size() > 1) {
                                        previousButton.setEnabled(true);
                                        nextButton.setEnabled(true);
                                    } else { // possibilities.size() == 1
                                        previousButton.setEnabled(false);
                                        nextButton.setEnabled(false);
                                    }

                                    updateSelectionInTab();
                                } else {
                                    previousButton.setEnabled(false);
                                    nextButton.setEnabled(false);

                                    if (!inputLowerCase.equalsIgnoreCase(welcomeText.get(selectedJumpType))) {
                                        indexLabel.setText("(no matches)");
                                    } else {
                                        indexLabel.setText("");
                                    }
                                }

                                peptideShakerGUI.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                                inputTxt.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                                inputTxt.requestFocus();

                            } else {

                                indexLabel.setText("");
                                previousButton.setEnabled(false);
                                nextButton.setEnabled(false);
                                inputTxt.setText(welcomeText.get(selectedJumpType));
                                inputTxt.selectAll();
                                inputTxt.requestFocus();

                            }
                        }

                        lastLabel.put(selectedJumpType, indexLabel.getText());

                    }
                } catch (Exception e) {

                    peptideShakerGUI.catchException(e);

                } finally {

                    searchMutex.release();

                }
            }
        }.start();
    }//GEN-LAST:event_inputTxtKeyReleased

    /**
     * Display the previous match in the list.
     *
     * @param evt the action event
     */
    private void previousButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_previousButtonActionPerformed
        if (currentSelection.get(selectedJumpType) == 0) {
            currentSelection.put(selectedJumpType, possibilities.get(selectedJumpType).size() - 1);
        } else {
            currentSelection.put(selectedJumpType, currentSelection.get(selectedJumpType) - 1);
        }

        updateSelectionInTab();

    }//GEN-LAST:event_previousButtonActionPerformed

    /**
     * Display the next match in the list.
     *
     * @param evt the action event
     */
    private void nextButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextButtonActionPerformed
        if (currentSelection.get(selectedJumpType) == possibilities.get(selectedJumpType).size() - 1) {
            currentSelection.put(selectedJumpType, 0);
        } else {
            currentSelection.put(selectedJumpType, currentSelection.get(selectedJumpType) + 1);
        }

        updateSelectionInTab();

    }//GEN-LAST:event_nextButtonActionPerformed

    /**
     * Select all text in the search field.
     *
     * @param evt the mouse event
     */
    private void inputTxtMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_inputTxtMouseReleased
        if (inputTxt.getText().equals(welcomeText.get(selectedJumpType))) {
            inputTxt.selectAll();
        }
    }//GEN-LAST:event_inputTxtMouseReleased

    /**
     * Change the icon to a hand icon.
     *
     * @param evt the mouse event
     */
    private void previousButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_previousButtonMouseEntered
        if (previousButton.isEnabled()) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }
    }//GEN-LAST:event_previousButtonMouseEntered

    /**
     * Change the icon back to the default icon.
     *
     * @param evt the mouse event
     */
    private void previousButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_previousButtonMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_previousButtonMouseExited

    /**
     * Change the icon back to the default icon.
     *
     * @param evt the mouse event
     */
    private void nextButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_nextButtonMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_nextButtonMouseExited

    /**
     * Change the icon to a hand icon.
     *
     * @param evt the mouse event
     */
    private void nextButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_nextButtonMouseEntered
        if (nextButton.isEnabled()) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }
    }//GEN-LAST:event_nextButtonMouseEntered
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel findJLabel;
    private javax.swing.JLabel indexLabel;
    private javax.swing.JTextField inputTxt;
    private javax.swing.JButton nextButton;
    private javax.swing.JButton previousButton;
    // End of variables declaration//GEN-END:variables

    @Override
    public void setEnabled(boolean enabled) {

        inputTxt.setEnabled(enabled);
        indexLabel.setEnabled(enabled);

        if (possibilities.size() > 0 && enabled) {
            previousButton.setEnabled(true);
            nextButton.setEnabled(true);
        } else {
            previousButton.setEnabled(false);
            nextButton.setEnabled(false);
        }
    }

    /**
     * Changes the type of jumpToPanel.
     *
     * @param jumpType the new type of jump to panel
     */
    public void setType(JumpType jumpType) {

        this.selectedJumpType = jumpType;

        if (lastInput.get(jumpType) != null && !lastInput.get(jumpType).equals("")) {

            inputTxt.setText(lastInput.get(jumpType));
            indexLabel.setText(lastLabel.get(jumpType));

        } else {

            inputTxt.setText(welcomeText.get(jumpType));
            indexLabel.setText("");

        }
    }

    /**
     * Sets the spectrum file inspected.
     *
     * @param spectrumFile the name of the spectrum file inspected
     */
    public void setSpectrumFile(String spectrumFile) {
        this.spectrumfile = spectrumFile;
    }
}
